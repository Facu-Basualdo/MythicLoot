package com.mythicloot;

import com.mythicloot.mixin.FlechaAccessor;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * Los afijos de jefe. A diferencia de los comunes, que valen en el arma con que se pega, estos valen mientras el objeto
 * está equipado en su lugar (el arma o la herramienta en la mano principal, el escudo en la secundaria, la armadura
 * puesta), sea el tipo que sea: un casco "del Verdugo" también hace que tus golpes ignoren armadura. Si dos objetos
 * equipados traen el mismo, cuenta el más alto: no se apilan. Sólo para jugadores.
 */
public final class EfectosDeJefe {
	private static final int TICKS_POR_SEGUNDO = 20;
	// Los mismos intentos que la fruta de chorus: si en 16 no encuentra dónde caer, no salta
	private static final int INTENTOS_DE_TELETRANSPORTE = 16;

	public static final AttachmentType<Long> ULTIMO_BREBAJE = AttachmentRegistry.create(MythicLoot.id("ultimo_brebaje"));
	public static final AttachmentType<Long> ULTIMO_TELETRANSPORTE = AttachmentRegistry.create(MythicLoot.id("ultimo_teletransporte"));

	private EfectosDeJefe() {
	}

	/** Cuánto vale el afijo en lo que el jugador tiene equipado: el más alto, no la suma. */
	public static double valor(LivingEntity entidad, AfijoDeBotin afijo) {
		double valor = 0;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack stack = entidad.getItemBySlot(slot);
			ObjetoConAfijos objeto = ObjetoConAfijos.de(stack);
			if (objeto != null && slot == lugarDe(stack)) {
				valor = Math.max(valor, objeto.valor(afijo));
			}
		}
		return valor;
	}

	/** Dónde se usa: la armadura en su slot, el escudo en la mano secundaria y todo lo demás en la principal. */
	private static EquipmentSlot lugarDe(ItemStack stack) {
		Equippable equipable = stack.get(DataComponents.EQUIPPABLE);
		if (equipable != null && equipable.slot().isArmor()) {
			return equipable.slot();
		}
		return stack.has(DataComponents.BLOCKS_ATTACKS) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
	}

	/** AFTER_DEATH. Festín: al matar un enemigo, el que lo mató recupera una parte de su vida máxima. */
	public static void alMorir(LivingEntity muerto, DamageSource fuente) {
		if (!(muerto instanceof Enemy) || !(fuente.getEntity() instanceof ServerPlayer jugador) || !jugador.isAlive()) {
			return;
		}
		double festin = valor(jugador, AfijoDeBotin.FESTIN);
		if (festin > 0) {
			jugador.heal((float) (jugador.getMaxHealth() * festin));
		}
	}

	/**
	 * AFTER_DAMAGE. Del que pega: marchitamiento. Del que recibe: brebaje y la llamarada que prende al que le pegó
	 * cuerpo a cuerpo.
	 */
	public static void alPegar(LivingEntity herido, DamageSource fuente, float danioBase, float danio, boolean bloqueado) {
		if (bloqueado || danio <= 0 || !(herido.level() instanceof ServerLevel level)) {
			return;
		}
		Configuracion.AfijosDeJefe config = MythicLoot.config().afijosDeJefe();
		if (fuente.getEntity() instanceof ServerPlayer atacante && atacante != herido && herido.isAlive()) {
			double marchito = valor(atacante, AfijoDeBotin.MARCHITO);
			if (marchito > 0) {
				herido.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) Math.round(marchito * TICKS_POR_SEGUNDO), config.nivelDeMarchito() - 1), atacante);
			}
		}
		if (!(herido instanceof ServerPlayer jugador) || !jugador.isAlive()) {
			return;
		}
		if (fuente.getEntity() instanceof LivingEntity atacante && atacante != jugador && cuerpoACuerpo(fuente) && valor(jugador, AfijoDeBotin.LLAMARADA) > 0) {
			atacante.igniteForSeconds(config.segundosDeLlamarada());
		}
		double brebaje = valor(jugador, AfijoDeBotin.BREBAJE);
		if (brebaje > 0 && libre(jugador, ULTIMO_BREBAJE, config.segundosEntreBrebajes(), level) && jugador.getRandom().nextDouble() < brebaje) {
			tomarBrebaje(jugador, level, config);
		}
	}

	/**
	 * Como la bruja, toma el que necesita: resistencia al fuego si se está quemando, respiración si está bajo el agua, y
	 * si no, curación instantánea.
	 */
	private static void tomarBrebaje(ServerPlayer jugador, ServerLevel level, Configuracion.AfijosDeJefe config) {
		Holder<MobEffect> efecto = MobEffects.INSTANT_HEALTH;
		if (jugador.isOnFire() && !jugador.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			efecto = MobEffects.FIRE_RESISTANCE;
		} else if (jugador.isUnderWater() && !jugador.hasEffect(MobEffects.WATER_BREATHING)) {
			efecto = MobEffects.WATER_BREATHING;
		}
		if (efecto.value().isInstantaneous()) {
			// Un efecto instantáneo agregado con addEffect recién se aplica en el tick siguiente
			efecto.value().applyInstantaneousEffect(level, null, null, jugador, 0, 1.0);
		} else {
			jugador.addEffect(new MobEffectInstance(efecto, config.segundosDeBrebaje() * TICKS_POR_SEGUNDO));
		}
		jugador.setAttached(ULTIMO_BREBAJE, level.getGameTime());
		level.playSound(null, jugador.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
	}

	/**
	 * Mixin al principio de {@code LivingEntity.hurtServer}, como el daño de Hero's Forge: bruto (cuerpo a cuerpo) y
	 * llamarada (fuego y lava) bajan el golpe antes de la armadura.
	 */
	public static float danioRecibido(LivingEntity herido, DamageSource fuente, float danio) {
		if (danio <= 0 || !(herido instanceof ServerPlayer jugador)) {
			return danio;
		}
		if (cuerpoACuerpo(fuente)) {
			danio *= (float) (1 - Mth.clamp(valor(jugador, AfijoDeBotin.BRUTO), 0, 1));
		}
		if (fuente.is(DamageTypeTags.IS_FIRE)) {
			danio *= (float) (1 - Mth.clamp(valor(jugador, AfijoDeBotin.LLAMARADA), 0, 1));
		}
		return danio;
	}

	/** Mixin en {@code getDamageAfterArmorAbsorb}: verdugo hace que los golpes del jugador ignoren parte de la armadura. */
	public static float armaduraContra(DamageSource fuente, float armadura) {
		if (!(fuente.getEntity() instanceof ServerPlayer jugador)) {
			return armadura;
		}
		return armadura * (float) (1 - Mth.clamp(valor(jugador, AfijoDeBotin.VERDUGO), 0, 1));
	}

	/**
	 * ALLOW_DAMAGE. Vacío: una chance de esquivar el ataque de algo (un mob, una flecha, otro jugador) saltando como un
	 * enderman. Si no encuentra dónde caer, el golpe entra igual. La lava, las caídas y el vacío no se esquivan.
	 */
	public static boolean esquivar(LivingEntity herido, DamageSource fuente, float danio) {
		if (!(herido instanceof ServerPlayer jugador) || !(herido.level() instanceof ServerLevel level) || fuente.getEntity() == null
				|| fuente.getEntity() == jugador || fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}
		Configuracion.AfijosDeJefe config = MythicLoot.config().afijosDeJefe();
		double vacio = valor(jugador, AfijoDeBotin.VACIO);
		if (vacio <= 0 || !libre(jugador, ULTIMO_TELETRANSPORTE, config.segundosEntreTeletransportes(), level) || jugador.getRandom().nextDouble() >= vacio) {
			return true;
		}
		if (!teletransportar(jugador, level, config.distanciaDeTeletransporte())) {
			return true;
		}
		jugador.setAttached(ULTIMO_TELETRANSPORTE, level.getGameTime());
		return false;
	}

	/** Como la fruta de chorus: hasta 16 intentos a {@code distancia} bloques, sin caer en bloques donde no se puede. */
	private static boolean teletransportar(ServerPlayer jugador, ServerLevel level, int distancia) {
		RandomSource random = jugador.getRandom();
		Vec3 desde = jugador.position();
		for (int i = 0; i < INTENTOS_DE_TELETRANSPORTE; i++) {
			double x = jugador.getX() + (random.nextDouble() - 0.5) * 2 * distancia;
			double y = Mth.clamp(jugador.getY() + random.nextInt(2 * distancia) - distancia, level.getMinY(), level.getMinY() + level.getLogicalHeight() - 1);
			double z = jugador.getZ() + (random.nextDouble() - 0.5) * 2 * distancia;
			if (jugador.isPassenger()) {
				jugador.stopRiding();
			}
			if (jugador.randomTeleport(x, y, z, true, BlockTags.CONSUMABLE_DOES_NOT_TELEPORT_TO)) {
				level.gameEvent(GameEvent.TELEPORT, desde, GameEvent.Context.of(jugador));
				level.playSound(null, jugador.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
				jugador.resetFallDistance();
				return true;
			}
		}
		return false;
	}

	/**
	 * Lluvia: una chance de que la flecha salga con otra más, levemente desviada, con el mismo daño. La copia no se puede
	 * juntar (como las de Multidisparo) y lleva la marca de potenciada, así no vuelve a pasar por acá.
	 */
	public static void alDisparar(AbstractArrow flecha, ServerPlayer jugador, ServerLevel level) {
		double lluvia = valor(jugador, AfijoDeBotin.LLUVIA);
		if (lluvia <= 0 || jugador.getRandom().nextDouble() >= lluvia) {
			return;
		}
		Arrow copia = new Arrow(level, jugador, new ItemStack(Items.ARROW), flecha.getWeaponItem());
		copia.setAttached(MythicLoot.FLECHA_POTENCIADA, true);
		copia.setPos(flecha.position());
		RandomSource random = jugador.getRandom();
		Vec3 desvio = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(0.05 * flecha.getDeltaMovement().length());
		copia.setDeltaMovement(flecha.getDeltaMovement().add(desvio));
		copia.setBaseDamage(((FlechaAccessor) flecha).mythicloot$danioBase());
		copia.setCritArrow(flecha.isCritArrow());
		copia.setRemainingFireTicks(flecha.getRemainingFireTicks());
		copia.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
		level.addFreshEntity(copia);
	}

	/** Un golpe directo de un mob o un jugador: ni flechas, ni explosiones, ni magia. */
	private static boolean cuerpoACuerpo(DamageSource fuente) {
		return fuente.getEntity() instanceof LivingEntity && fuente.getDirectEntity() == fuente.getEntity()
				&& !fuente.is(DamageTypeTags.IS_PROJECTILE) && !fuente.is(DamageTypeTags.IS_EXPLOSION) && !fuente.is(DamageTypeTags.WITCH_RESISTANT_TO);
	}

	private static boolean libre(ServerPlayer jugador, AttachmentType<Long> ultimo, int segundos, ServerLevel level) {
		Long antes = jugador.getAttached(ultimo);
		return antes == null || level.getGameTime() - antes >= (long) segundos * TICKS_POR_SEGUNDO;
	}
}
