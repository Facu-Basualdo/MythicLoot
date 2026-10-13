package com.mythicloot.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mythicloot.EfectosDeJefe;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Los afijos de jefe que cambian un golpe: Fabric API tiene un evento para cancelar el daño, no para cambiarlo. */
@Mixin(LivingEntity.class)
abstract class DanioMixin {
	// Al principio, antes de la armadura y de los encantamientos, como el daño de Hero's Forge: bruto y llamarada
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float mythicloot$danioRecibido(float danio, @Local(argsOnly = true) DamageSource fuente) {
		return EfectosDeJefe.danioRecibido((LivingEntity) (Object) this, fuente, danio);
	}

	// Verdugo: la armadura del que recibe el golpe cuenta menos. La dureza no cambia
	@WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
	private float mythicloot$verdugo(LivingEntity herido, float danio, DamageSource fuente, float armadura, float dureza, Operation<Float> original) {
		return original.call(herido, danio, fuente, EfectosDeJefe.armaduraContra(fuente, armadura), dureza);
	}
}
