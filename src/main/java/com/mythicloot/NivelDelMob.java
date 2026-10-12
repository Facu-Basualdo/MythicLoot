package com.mythicloot;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentRegistryImpl;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * El nivel del botín de un mob. Con Bloodmoon & Bosses es el nivel del mob; sin él, el de la distancia al spawn, con
 * la misma cuenta que usaría Bloodmoon por defecto (sin días ni campeones).
 *
 * <p>No se depende de las clases de Bloodmoon: se leen sus attachments por id. Esos ids no pueden cambiar nunca (están
 * guardados en el NBT de los mobs de todos los mundos), así que son un contrato más firme que un nombre de clase.
 */
public final class NivelDelMob {
	public static final String BLOODMOON = "bloodmoonandbosses";
	private static final Identifier NIVEL = Identifier.fromNamespaceAndPath(BLOODMOON, "nivel");
	private static final Identifier JEFE = Identifier.fromNamespaceAndPath(BLOODMOON, "jefe");
	private static final Identifier AFIJO = Identifier.fromNamespaceAndPath(BLOODMOON, "afijo");

	// Lo decide MythicLoot al iniciar (si está Bloodmoon); los tests lo fuerzan
	private static volatile boolean conBloodmoon;

	private NivelDelMob() {
	}

	public static void usarBloodmoon(boolean usar) {
		conBloodmoon = usar;
	}

	public static boolean conBloodmoon() {
		return conBloodmoon;
	}

	/**
	 * Null si el mob no tiene nivel: con Bloodmoon, los de spawners, huevos y comandos no lo tienen a propósito (para
	 * que las granjas no cambien), y entonces tampoco sueltan botín con afijos.
	 */
	public static @Nullable Integer nivel(Mob mob, Configuracion config) {
		if (conBloodmoon) {
			return leer(mob, NIVEL) instanceof Integer nivel ? nivel : null;
		}
		if (!(mob.level() instanceof ServerLevel level)) {
			return null;
		}
		return porDistancia(level, mob.blockPosition(), config);
	}

	public static boolean esJefe(Mob mob) {
		return conBloodmoon && Boolean.TRUE.equals(leer(mob, JEFE));
	}

	/** Un campeón de Bloodmoon: tiene afijo (por defecto, todos los campeones). Los jefes también, pero van aparte. */
	public static boolean esCampeon(Mob mob) {
		return conBloodmoon && leer(mob, AFIJO) != null;
	}

	public static int porDistancia(ServerLevel level, BlockPos pos, Configuracion config) {
		BlockPos spawn = level.getServer().getRespawnData().pos();
		double dx;
		double dz;
		int extra = 0;
		if (level.dimension() == Level.NETHER) {
			dx = pos.getX() * 8.0 - spawn.getX();
			dz = pos.getZ() * 8.0 - spawn.getZ();
			extra = config.extraNether();
		} else if (level.dimension() == Level.END) {
			dx = pos.getX();
			dz = pos.getZ();
			extra = config.extraEnd();
		} else {
			dx = pos.getX() - spawn.getX();
			dz = pos.getZ() - spawn.getZ();
		}
		int nivel = 1 + (int) Math.floor(Math.sqrt(dx * dx + dz * dz) / config.bloquesPorNivel()) + extra;
		return Mth.clamp(nivel, 1, config.nivelMaximo());
	}

	/**
	 * Se busca cada vez y no al iniciar: el orden en que Fabric inicializa los mods no está garantizado, y Bloodmoon
	 * podría registrar sus attachments después que este mod. La búsqueda es un get de un mapa.
	 */
	private static @Nullable Object leer(Mob mob, Identifier id) {
		AttachmentType<?> tipo = AttachmentRegistryImpl.get(id);
		return tipo == null ? null : mob.getAttached(tipo);
	}
}
