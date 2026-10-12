package com.mythicloot.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mythicloot.MythicLoot;
import com.mythicloot.Yunque;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Se mete en {@code createResult} después de juntar durabilidad y encantamientos y antes del renombre (la primera
 * lectura de {@code itemName}). El costo es la variable que después decide si hay resultado: sumándole acá, dos
 * objetos sin daño ni encantamientos también se pueden juntar.
 *
 * <p>Sin tabla de variables locales en el jar, se ubican por orden: los {@code ItemStack} son el de la izquierda, el
 * resultado y el de la derecha; el primer {@code int} es el costo.
 */
@Mixin(AnvilMenu.class)
abstract class YunqueMixin {
	@Inject(method = "createResult", at = @At(value = "FIELD", target = "Lnet/minecraft/world/inventory/AnvilMenu;itemName:Ljava/lang/String;", opcode = Opcodes.GETFIELD, ordinal = 0))
	private void mythicloot$unirAfijos(CallbackInfo ci, @Local(ordinal = 1) ItemStack resultado, @Local(ordinal = 2) ItemStack derecha, @Local(ordinal = 0) LocalIntRef costo) {
		int ganados = Yunque.unir(resultado, derecha);
		if (ganados > 0) {
			costo.set(costo.get() + ganados * MythicLoot.config().costoPorAfijoUnido());
		}
	}
}
