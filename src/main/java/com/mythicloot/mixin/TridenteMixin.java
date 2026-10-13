package com.mythicloot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.mythicloot.Efectos;

import net.minecraft.world.entity.projectile.arrow.ThrownTrident;

/**
 * El tridente lanzado pega siempre 8 (más Empalamiento) y no usa el daño base de la flecha: subirle el daño base, como
 * a una flecha, no cambiaría nada. Acá se le suma el certero.
 */
@Mixin(ThrownTrident.class)
abstract class TridenteMixin {
	@ModifyConstant(method = "onHitEntity", constant = @Constant(floatValue = 8.0f))
	private float mythicloot$certero(float danio) {
		return Efectos.danioDelTridente((ThrownTrident) (Object) this, danio);
	}
}
