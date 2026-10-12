package com.mythicloot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

// En 26.3 AbstractArrow tiene setBaseDamage pero no el getter: el certero multiplica lo que ya tenía la flecha
@Mixin(AbstractArrow.class)
public interface FlechaAccessor {
	@Accessor("baseDamage")
	double mythicloot$danioBase();
}
