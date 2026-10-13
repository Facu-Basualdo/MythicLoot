package com.mythicloot;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

/**
 * Juntar dos objetos iguales en el yunque. Vanilla copia el de la izquierda y le suma la durabilidad y los
 * encantamientos del otro, pero los afijos del de la derecha se perdían: acá se suman al resultado.
 */
public final class Yunque {
	private Yunque() {
	}

	/**
	 * Le pone a {@code resultado} (la copia del de la izquierda que arma vanilla) los afijos de los dos. Devuelve cuántos
	 * afijos ganó o mejoró: con 0 no lo toca.
	 */
	public static int unir(ItemStack resultado, ItemStack derecha) {
		ObjetoConAfijos otro = ObjetoConAfijos.de(derecha);
		if (otro == null || !resultado.is(derecha.getItem())) {
			return 0;
		}
		ObjetoConAfijos propio = ObjetoConAfijos.de(resultado);
		ObjetoConAfijos unido = juntar(propio, otro);
		int ganados = 0;
		for (Map.Entry<AfijoDeBotin, Double> entrada : unido.afijos().entrySet()) {
			if (propio == null || entrada.getValue() > propio.valor(entrada.getKey())) {
				ganados++;
			}
		}
		if (ganados > 0) {
			GeneradorDeBotin.rearmar(resultado, unido);
		}
		return ganados;
	}

	/**
	 * Los afijos de los dos; si se repite uno, el valor más alto. Nunca más que los de la mejor rareza: si se pasa, se
	 * quedan los del de la izquierda y los primeros del otro. La rareza sube hasta la que trae esa cantidad de afijos, y
	 * el nivel es el más alto. El de jefe no cuenta para el tope ni para la rareza, pero hay uno solo: si los dos traen
	 * uno distinto, queda el del de la izquierda (si no, juntando objetos se apilarían los efectos de varios jefes).
	 */
	public static ObjetoConAfijos juntar(@Nullable ObjetoConAfijos izquierda, ObjetoConAfijos derecha) {
		Map<AfijoDeBotin, Double> afijos = new EnumMap<>(AfijoDeBotin.class);
		if (izquierda != null) {
			afijos.putAll(izquierda.afijos());
		}
		int tope = Rareza.LEGENDARIA.afijos();
		derecha.afijos().forEach((afijo, valor) -> {
			Double actual = afijos.get(afijo);
			if (actual != null) {
				afijos.put(afijo, Math.max(actual, valor));
			} else if (afijo.deJefe() ? afijos.keySet().stream().noneMatch(AfijoDeBotin::deJefe) : comunes(afijos) < tope) {
				afijos.put(afijo, valor);
			}
		});

		Rareza rareza = Rareza.conAfijos(comunes(afijos));
		int nivel = derecha.nivel();
		if (izquierda != null) {
			rareza = mejor(rareza, izquierda.rareza());
			nivel = Math.max(nivel, izquierda.nivel());
		}
		return new ObjetoConAfijos(nivel, mejor(rareza, derecha.rareza()), afijos);
	}

	private static int comunes(Map<AfijoDeBotin, Double> afijos) {
		return (int) afijos.keySet().stream().filter(afijo -> !afijo.deJefe()).count();
	}

	private static Rareza mejor(Rareza una, Rareza otra) {
		return una.compareTo(otra) >= 0 ? una : otra;
	}
}
