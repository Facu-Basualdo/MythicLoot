package com.botinmitico;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/** Cuándo un mob suelta un objeto con afijos, y cuál. */
public final class Caida {
	private Caida() {
	}

	/** Lo que decide si un mob suelta algo: null si no le toca. Separado de {@link #alMorir} para poder probarlo. */
	public static @Nullable Rareza sortear(Mob mob, Configuracion config, RandomSource random) {
		Integer nivel = NivelDelMob.nivel(mob, config);
		if (nivel == null) {
			return null;
		}
		if (config.botinDeJefe() && NivelDelMob.esJefe(mob)) {
			Rareza minima = Rareza.porId(config.rarezaDeJefe());
			return GeneradorDeBotin.elegirRareza(config, nivel, minima == null ? Rareza.EPICA : minima, random);
		}
		double chance = config.chance(nivel) * (NivelDelMob.esCampeon(mob) ? config.multiplicadorCampeon() : 1);
		if (random.nextDouble() >= chance) {
			return null;
		}
		return GeneradorDeBotin.elegirRareza(config, nivel, random);
	}

	/**
	 * AFTER_DEATH. Sólo si lo mató un jugador (la misma memoria que usa vanilla para el loot "killed_by_player": vale
	 * también si lo remató la caída o el fuego después de un golpe) y con {@code mobDrops} prendido.
	 */
	public static void alMorir(LivingEntity muerto, DamageSource fuente) {
		if (!(muerto instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		// Con Bloodmoon decide él quién tiene nivel (también piglins y otros que no son Enemy); sin él, sólo los hostiles
		if (!NivelDelMob.conBloodmoon() && !(mob instanceof Enemy)) {
			return;
		}
		Player jugador = mob.getLastHurtByPlayer();
		if (jugador == null || !level.getGameRules().get(GameRules.MOB_DROPS)) {
			return;
		}
		Configuracion config = BotinMitico.config();
		RandomSource random = mob.getRandom();
		Rareza rareza = sortear(mob, config, random);
		if (rareza == null) {
			return;
		}
		Integer nivel = NivelDelMob.nivel(mob, config);
		ItemStack objeto = GeneradorDeBotin.generar(config, nivel == null ? 1 : nivel, rareza, GeneradorDeBotin.elegirTipo(config, random), random);
		ItemEntity soltado = mob.spawnAtLocation(level, objeto);
		if (soltado != null && rareza.compareTo(Rareza.EPICA) >= 0) {
			// El contorno se ve a través de las paredes: en una pelea con escoltas, un épico no se pierde entre el loot
			soltado.setGlowingTag(true);
			level.playSound(null, mob.blockPosition(), rareza == Rareza.LEGENDARIA ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP,
					SoundSource.PLAYERS, 1.0f, 1.2f);
			if (jugador instanceof ServerPlayer servidor) {
				servidor.sendSystemMessage(Component.literal("¡Botín " + rareza.masculino() + "!").withStyle(rareza.color()), true);
			}
		}
	}
}
