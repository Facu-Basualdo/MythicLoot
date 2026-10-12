package com.mythicloot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class Texto {
	private Texto() {
	}

	/**
	 * Un número para guardar en un objeto, en el idioma de la config (el del texto de respaldo): 2.5 → "2,5" en español,
	 * 3.0 → "3". Va como texto ya armado porque el cliente no sabe formatear con coma.
	 */
	static String numero(double valor) {
		return Idioma.numero(MythicLoot.config().idioma(), valor);
	}

	/** Vanilla pone el lore en cursiva: sin el {@code withItalic(false)} se ve torcido. */
	static MutableComponent lore(Component texto, ChatFormatting color) {
		return texto.copy().withStyle(color).withStyle(estilo -> estilo.withItalic(false));
	}
}
