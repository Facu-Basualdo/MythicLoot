package com.mythicloot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class Texto {
	private Texto() {
	}

	/** Con coma decimal y sin ceros de más: 2.5 → "2,5", 3.0 → "3". */
	static String numero(double valor) {
		double redondeado = Math.round(valor * 100) / 100.0;
		if (redondeado == Math.rint(redondeado)) {
			return Long.toString((long) redondeado);
		}
		return Double.toString(redondeado).replace('.', ',');
	}

	/** Vanilla pone el lore en cursiva: sin el {@code withItalic(false)} se ve torcido. */
	static MutableComponent lore(String texto, ChatFormatting color) {
		return Component.literal(texto).withStyle(color).withStyle(estilo -> estilo.withItalic(false));
	}
}
