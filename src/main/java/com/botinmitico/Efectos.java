package com.botinmitico;

import org.jspecify.annotations.Nullable;

import com.botinmitico.mixin.FlechaAccessor;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;

/** Los afijos que no son atributos: vampírico y ardiente al pegar, certero y ardiente al disparar. */
public final class Efectos {
	private static final int TICKS_POR_SEGUNDO = 20;

	private Efectos() {
	}

	/**
	 * ENTITY_LOAD de una flecha: el arco con que se tiró queda en {@code getWeaponItem()}. Sube el daño base (como
	 * Poder: después se multiplica por la velocidad) y prende la flecha (como Llama). Sólo las flechas de jugadores.
	 */
	public static void alDisparar(Entity entidad, ServerLevel level) {
		if (!(entidad instanceof AbstractArrow flecha) || !(flecha.getOwner() instanceof ServerPlayer)) {
			return;
		}
		ObjetoConAfijos arma = ObjetoConAfijos.de(flecha.getWeaponItem());
		// La marca evita aplicarlo dos veces: ENTITY_LOAD también corre cuando la flecha vuelve a cargarse con su chunk
		if (arma == null || flecha.getAttached(BotinMitico.FLECHA_POTENCIADA) != null) {
			return;
		}
		flecha.setAttached(BotinMitico.FLECHA_POTENCIADA, true);
		double certero = arma.valor(AfijoDeBotin.CERTERO);
		if (certero > 0) {
			flecha.setBaseDamage(((FlechaAccessor) flecha).botinmitico$danioBase() * (1 + certero));
		}
		double fuego = arma.valor(AfijoDeBotin.ARDIENTE);
		if (fuego > 0) {
			// Una flecha prendida prende lo que toca, como con Llama
			flecha.igniteForSeconds((float) fuego);
		}
	}

	/**
	 * AFTER_DAMAGE. El arma es la de la mano si pegó cuerpo a cuerpo, o el arco de la flecha si fue a distancia: así el
	 * vampírico de un arco cura con las flechas, y no cuenta el arco que el jugador cambió de mano después de tirar.
	 */
	public static void alPegar(LivingEntity herido, DamageSource fuente, float danioBase, float danio, boolean bloqueado) {
		if (bloqueado || danio <= 0 || !(fuente.getEntity() instanceof ServerPlayer jugador) || jugador == herido) {
			return;
		}
		ObjetoConAfijos arma = ObjetoConAfijos.de(armaDelGolpe(jugador, fuente));
		if (arma == null) {
			return;
		}
		double robo = arma.valor(AfijoDeBotin.VAMPIRICO);
		if (robo > 0 && jugador.isAlive()) {
			jugador.heal((float) (danio * robo));
		}
		// A distancia, el fuego ya lo lleva la flecha prendida
		double fuego = arma.valor(AfijoDeBotin.ARDIENTE);
		if (fuego > 0 && fuente.getDirectEntity() == jugador && herido.isAlive()) {
			herido.igniteForSeconds((float) fuego);
			if (herido.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.FLAME, herido.getX(), herido.getY(0.5), herido.getZ(), 6, 0.3, 0.4, 0.3, 0.01);
			}
		}
	}

	private static @Nullable ItemStack armaDelGolpe(ServerPlayer jugador, DamageSource fuente) {
		if (fuente.getDirectEntity() == jugador) {
			return jugador.getMainHandItem();
		}
		if (fuente.getDirectEntity() instanceof AbstractArrow flecha) {
			return flecha.getWeaponItem();
		}
		return null;
	}
}
