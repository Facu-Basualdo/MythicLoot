package com.mythicloot;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Cuántos afijos trae un objeto y cuánto rinden. El orden importa: de la más común a la mejor. */
public enum Rareza implements StringRepresentable {
	MAGICA("magica", ChatFormatting.BLUE, 1, 1.0),
	RARA("rara", ChatFormatting.YELLOW, 2, 1.0),
	EPICA("epica", ChatFormatting.LIGHT_PURPLE, 3, 1.1),
	LEGENDARIA("legendaria", ChatFormatting.GOLD, 4, 1.25);

	private final String id;
	private final ChatFormatting color;
	private final int afijos;
	private final double bonificacion;

	Rareza(String id, ChatFormatting color, int afijos, double bonificacion) {
		this.id = id;
		this.color = color;
		this.afijos = afijos;
		this.bonificacion = bonificacion;
	}

	@Override
	public String getSerializedName() {
		return id;
	}

	/** Para el lore ("Rareza: épica"): en español va en femenino, para concordar. */
	public Component nombre() {
		return Idioma.guardado("mythicloot.rareza." + id);
	}

	/** El aviso al soltarse ("¡Botín épico!"), con su propia clave: en español concuerda con "botín", en masculino. */
	public String claveDelAviso() {
		return "mythicloot.aviso." + id;
	}

	public ChatFormatting color() {
		return color;
	}

	public int afijos() {
		return afijos;
	}

	/** Multiplica el valor de cada afijo: una épica rinde un 10 % más que una rara del mismo nivel. */
	public double bonificacion() {
		return bonificacion;
	}

	/** La primera que trae esa cantidad de afijos; con más de los que trae la mejor, la mejor. */
	public static Rareza conAfijos(int cantidad) {
		for (Rareza rareza : values()) {
			if (rareza.afijos >= cantidad) {
				return rareza;
			}
		}
		return LEGENDARIA;
	}

	public static @Nullable Rareza porId(String id) {
		for (Rareza rareza : values()) {
			if (rareza.id.equals(id)) {
				return rareza;
			}
		}
		return null;
	}
}
