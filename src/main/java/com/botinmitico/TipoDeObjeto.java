package com.botinmitico;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.StringRepresentable;

/** Qué clase de objeto es: decide qué afijos le tocan y dónde valen sus atributos. */
public enum TipoDeObjeto implements StringRepresentable {
	CUERPO_A_CUERPO("cuerpo_a_cuerpo"),
	A_DISTANCIA("a_distancia"),
	ARMADURA("armadura");

	private final String id;

	TipoDeObjeto(String id) {
		this.id = id;
	}

	@Override
	public String getSerializedName() {
		return id;
	}

	public static @Nullable TipoDeObjeto porId(String id) {
		for (TipoDeObjeto tipo : values()) {
			if (tipo.id.equals(id)) {
				return tipo;
			}
		}
		return null;
	}
}
