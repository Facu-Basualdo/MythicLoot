package com.botinmitico;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

/** Cuántos afijos trae un objeto y cuánto rinden. El orden importa: de la más común a la mejor. */
public enum Rareza implements StringRepresentable {
	MAGICA("magica", "mágica", "mágico", ChatFormatting.BLUE, 1, 1.0),
	RARA("rara", "rara", "raro", ChatFormatting.YELLOW, 2, 1.0),
	EPICA("epica", "épica", "épico", ChatFormatting.LIGHT_PURPLE, 3, 1.1),
	LEGENDARIA("legendaria", "legendaria", "legendario", ChatFormatting.GOLD, 4, 1.25);

	private final String id;
	private final String nombre;
	private final String masculino;
	private final ChatFormatting color;
	private final int afijos;
	private final double bonificacion;

	Rareza(String id, String nombre, String masculino, ChatFormatting color, int afijos, double bonificacion) {
		this.id = id;
		this.nombre = nombre;
		this.masculino = masculino;
		this.color = color;
		this.afijos = afijos;
		this.bonificacion = bonificacion;
	}

	@Override
	public String getSerializedName() {
		return id;
	}

	/** En femenino: concuerda con "Rareza: épica", que es como se muestra. */
	public String nombre() {
		return nombre;
	}

	/** Para "¡Botín épico!". */
	public String masculino() {
		return masculino;
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

	public static @Nullable Rareza porId(String id) {
		for (Rareza rareza : values()) {
			if (rareza.id.equals(id)) {
				return rareza;
			}
		}
		return null;
	}
}
