package com.mythicloot;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;

/**
 * Los números de balance. Se lee de {@code config/mythicloot.json} mezclado sobre {@link #POR_DEFECTO} (ver
 * {@link ArchivoDeConfiguracion}). Los mapas usan los ids de {@link Rareza}, {@link TipoDeObjeto} y {@link AfijoDeBotin}.
 *
 * @param bloquesPorNivel sin Bloodmoon & Bosses, cada cuántos bloques del spawn sube el nivel del botín
 * @param chanceBase chance de soltar un objeto en el nivel 0; se suma chancePorNivel por nivel, hasta chanceMaxima
 * @param multiplicadorCampeon por cuánto se multiplica la chance si el mob es un campeón de Bloodmoon & Bosses
 * @param rarezaDeJefe la rareza mínima del objeto que suelta siempre un jefe de zona
 * @param afijosDeJefe el afijo que puede traer el objeto de cada especie de jefe, y los números de sus efectos
 * @param nivelesPorMaterial cada cuántos niveles mejora el material (cobre, hierro, diamante…)
 * @param rarezaMinimaDeMaza desde qué rareza un arma cuerpo a cuerpo puede salir maza
 * @param valoresMaximos lo que vale cada afijo en el nivel máximo, antes del azar y la rareza
 * @param costoPorAfijoUnido cuántos niveles de experiencia suma al yunque cada afijo que gana o mejora un objeto al
 *        juntarlo con otro
 * @param idioma el de los objetos que se generan (para quien no tiene el mod en el cliente) y el de la consola; en el
 *        chat cada jugador ve el de su cliente (ver {@link Idioma})
 */
public record Configuracion(
		int nivelMaximo,
		int bloquesPorNivel,
		int extraNether,
		int extraEnd,
		double chanceBase,
		double chancePorNivel,
		double chanceMaxima,
		double multiplicadorCampeon,
		boolean botinDeJefe,
		String rarezaDeJefe,
		AfijosDeJefe afijosDeJefe,
		Map<String, ChanceDeRareza> rarezas,
		Map<String, Double> pesosDeTipo,
		int nivelesPorMaterial,
		boolean netheritaSoloLegendaria,
		String rarezaMinimaDeMaza,
		Map<String, Double> valoresMaximos,
		int costoPorAfijoUnido,
		String idioma) {

	/*
	 * Pensados junto con Bloodmoon & Bosses y Hero's Forge, que llegan al nivel 100. Con 2 % + 0,08 % por nivel, un
	 * mob de nivel 50 suelta un objeto una vez cada 16 y uno de nivel 100 una cada 10. Las rarezas arrancan en los
	 * mismos niveles que el botín raro de Bloodmoon (20, 50 y 80). Al máximo, un arma suma el doble que Filo V (más con la
	 * rareza), y una armadura completa "del Gigante" unos 8 corazones: menos que los 15 de Hero's Forge, que se pagan.
	 */
	public static final Configuracion POR_DEFECTO = new Configuracion(
			100,
			100,
			20,
			40,
			0.02,
			0.0008,
			0.10,
			2.0,
			true,
			Rareza.EPICA.getSerializedName(),
			// Con un jefe cada hora y media de juego, más o menos, uno de cada cuatro trae el afijo de su especie
			new AfijosDeJefe(0.25, afijosPorMob(), 2, 10, 20, 4, 5, 8),
			ordenado(Map.of(
					Rareza.MAGICA.getSerializedName(), new ChanceDeRareza(1, 60),
					Rareza.RARA.getSerializedName(), new ChanceDeRareza(20, 30),
					Rareza.EPICA.getSerializedName(), new ChanceDeRareza(50, 12),
					Rareza.LEGENDARIA.getSerializedName(), new ChanceDeRareza(80, 4)), Rareza.values()),
			ordenado(Map.of(
					TipoDeObjeto.CUERPO_A_CUERPO.getSerializedName(), 40.0,
					TipoDeObjeto.A_DISTANCIA.getSerializedName(), 20.0,
					TipoDeObjeto.ARMADURA.getSerializedName(), 40.0,
					TipoDeObjeto.TRIDENTE.getSerializedName(), 5.0,
					TipoDeObjeto.ESCUDO.getSerializedName(), 8.0,
					TipoDeObjeto.HERRAMIENTA.getSerializedName(), 12.0), TipoDeObjeto.values()),
			20,
			true,
			Rareza.EPICA.getSerializedName(),
			valoresPorDefecto(),
			// Llevar una mágica a legendaria juntando objetos cuesta 12 niveles, más lo que cobra vanilla por reparar
			4,
			Idioma.INGLES);

	private static Map<String, Double> valoresPorDefecto() {
		Map<String, Double> valores = new LinkedHashMap<>();
		valores.put(AfijoDeBotin.AFILADO.id(), 6.0);
		valores.put(AfijoDeBotin.FURIOSO.id(), 0.2);
		valores.put(AfijoDeBotin.LARGO.id(), 1.0);
		valores.put(AfijoDeBotin.VAMPIRICO.id(), 0.08);
		// En segundos
		valores.put(AfijoDeBotin.ARDIENTE.id(), 5.0);
		valores.put(AfijoDeBotin.CERTERO.id(), 0.4);
		valores.put(AfijoDeBotin.LIGERO.id(), 0.06);
		// Por pieza: las cuatro suman 16 de vida (8 corazones)
		valores.put(AfijoDeBotin.ROBUSTO.id(), 4.0);
		valores.put(AfijoDeBotin.REFORZADO.id(), 2.0);
		valores.put(AfijoDeBotin.TENAZ.id(), 2.0);
		valores.put(AfijoDeBotin.FIRME.id(), 0.1);
		valores.put(AfijoDeBotin.AFORTUNADO.id(), 1.0);
		// Eficiencia III suma 10 y la IV, 17
		valores.put(AfijoDeBotin.EFICIENTE.id(), 12.0);
		valores.put(AfijoDeBotin.AMPLIO.id(), 1.5);
		valores.put(AfijoDeBotin.SUBMARINO.id(), 0.5);
		// De jefe. Los porcentajes, como fracción de la vida máxima, de la armadura del otro, del daño o de chance
		valores.put(AfijoDeBotin.FESTIN.id(), 0.1);
		valores.put(AfijoDeBotin.LLUVIA.id(), 0.3);
		valores.put(AfijoDeBotin.VERDUGO.id(), 0.3);
		valores.put(AfijoDeBotin.BREBAJE.id(), 0.3);
		// En segundos
		valores.put(AfijoDeBotin.MARCHITO.id(), 4.0);
		valores.put(AfijoDeBotin.BRUTO.id(), 0.2);
		valores.put(AfijoDeBotin.LLAMARADA.id(), 0.5);
		valores.put(AfijoDeBotin.VACIO.id(), 0.2);
		return valores;
	}

	/** Las especies de los tags jefe_de_zona de Bloodmoon, cada una con el afijo que imita lo que hace. */
	private static Map<String, String> afijosPorMob() {
		Map<String, String> afijos = new LinkedHashMap<>();
		afijos.put("minecraft:zombie", AfijoDeBotin.FESTIN.id());
		afijos.put("minecraft:skeleton", AfijoDeBotin.LLUVIA.id());
		afijos.put("minecraft:vindicator", AfijoDeBotin.VERDUGO.id());
		afijos.put("minecraft:witch", AfijoDeBotin.BREBAJE.id());
		afijos.put("minecraft:wither_skeleton", AfijoDeBotin.MARCHITO.id());
		afijos.put("minecraft:piglin_brute", AfijoDeBotin.BRUTO.id());
		afijos.put("minecraft:blaze", AfijoDeBotin.LLAMARADA.id());
		afijos.put("minecraft:enderman", AfijoDeBotin.VACIO.id());
		return afijos;
	}

	/** En el orden del enum: así el archivo sale de la más común a la mejor y no en el orden de un HashMap. */
	private static <V> Map<String, V> ordenado(Map<String, V> mapa, Enum<?>[] orden) {
		Map<String, V> ordenado = new LinkedHashMap<>();
		for (Enum<?> valor : orden) {
			String id = valor instanceof net.minecraft.util.StringRepresentable conId ? conId.getSerializedName() : valor.name();
			ordenado.put(id, mapa.get(id));
		}
		return ordenado;
	}

	public double chance(int nivel) {
		return Math.min(chanceBase + chancePorNivel * nivel, chanceMaxima);
	}

	public ChanceDeRareza rareza(Rareza rareza) {
		return rarezas.get(rareza.getSerializedName());
	}

	public double pesoDe(TipoDeObjeto tipo) {
		return pesosDeTipo.get(tipo.getSerializedName());
	}

	/** El afijo de esa especie de jefe ("minecraft:zombie"), o null si no tiene. */
	public @Nullable AfijoDeBotin afijoDeJefe(String idDeMob) {
		String id = afijosDeJefe.porMob().get(idDeMob);
		return id == null ? null : AfijoDeBotin.porId(id);
	}

	public Rareza minimaDeMaza() {
		Rareza rareza = Rareza.porId(rarezaMinimaDeMaza);
		return rareza == null ? Rareza.EPICA : rareza;
	}

	public double valorMaximo(AfijoDeBotin afijo) {
		return valoresMaximos.get(afijo.id());
	}

	/** Lo que haría fallar más tarde, con un error menos claro: mejor rechazar el archivo. */
	public void validar() {
		exigir(nivelMaximo >= 1, "nivelMaximo tiene que ser 1 o más");
		exigir(bloquesPorNivel >= 1, "bloquesPorNivel tiene que ser 1 o más");
		exigir(chanceBase >= 0 && chancePorNivel >= 0 && chanceMaxima >= 0 && chanceMaxima <= 1, "las chances van de 0 a 1");
		exigir(multiplicadorCampeon >= 0, "multiplicadorCampeon no puede ser negativo");
		exigir(Rareza.porId(rarezaDeJefe) != null, "rarezaDeJefe tiene que ser magica, rara, epica o legendaria");
		exigir(afijosDeJefe != null && afijosDeJefe.chance() >= 0 && afijosDeJefe.chance() <= 1, "afijosDeJefe.chance va de 0 a 1");
		afijosDeJefe.porMob().forEach((mob, id) -> {
			exigir(Identifier.tryParse(mob) != null, "afijosDeJefe.porMob: \"" + mob + "\" no es un id de entidad");
			AfijoDeBotin afijo = AfijoDeBotin.porId(id);
			exigir(id.isEmpty() || afijo != null && afijo.deJefe(), "afijosDeJefe.porMob." + mob + " tiene que ser un afijo de jefe o \"\" (ninguno)");
		});
		exigir(afijosDeJefe.nivelDeMarchito() >= 1 && afijosDeJefe.segundosDeBrebaje() >= 1 && afijosDeJefe.segundosEntreBrebajes() >= 0
				&& afijosDeJefe.segundosDeLlamarada() >= 1 && afijosDeJefe.segundosEntreTeletransportes() >= 0 && afijosDeJefe.distanciaDeTeletransporte() >= 1,
				"los números de afijosDeJefe tienen que ser 1 o más (las esperas, 0 o más)");
		exigir(nivelesPorMaterial >= 1, "nivelesPorMaterial tiene que ser 1 o más");
		exigir(Rareza.porId(rarezaMinimaDeMaza) != null, "rarezaMinimaDeMaza tiene que ser magica, rara, epica o legendaria");
		for (Rareza rareza : Rareza.values()) {
			ChanceDeRareza chance = rareza(rareza);
			exigir(chance != null, "falta la rareza " + rareza.getSerializedName());
			exigir(chance.peso() >= 0 && chance.nivelMinimo() >= 0, "el peso y el nivelMinimo de " + rareza.getSerializedName() + " no pueden ser negativos");
		}
		exigir(rareza(Rareza.MAGICA).nivelMinimo() <= 1 && rareza(Rareza.MAGICA).peso() > 0,
				"la rareza magica tiene que poder salir desde el nivel 1, o un mob de nivel bajo no tendría qué soltar");
		double pesos = 0;
		for (TipoDeObjeto tipo : TipoDeObjeto.values()) {
			Double peso = pesosDeTipo.get(tipo.getSerializedName());
			exigir(peso != null && peso >= 0, "el peso de " + tipo.getSerializedName() + " falta o es negativo");
			pesos += peso;
		}
		exigir(pesos > 0, "algún tipo de objeto tiene que tener peso");
		for (AfijoDeBotin afijo : AfijoDeBotin.values()) {
			Double valor = valoresMaximos.get(afijo.id());
			exigir(valor != null && valor > 0, "el valor máximo de " + afijo.id() + " falta o no es mayor que 0");
		}
		exigir(costoPorAfijoUnido >= 0, "costoPorAfijoUnido no puede ser negativo");
		exigir(Idioma.DISPONIBLES.contains(idioma), "idioma tiene que ser " + String.join(" o ", Idioma.DISPONIBLES));
	}

	private static void exigir(boolean condicion, String mensaje) {
		if (!condicion) {
			throw new IllegalArgumentException(mensaje);
		}
	}

	/**
	 * Los afijos de los jefes de zona de Bloodmoon (ver {@link EfectosDeJefe}).
	 *
	 * @param chance de que el objeto que suelta un jefe traiga el afijo de su especie
	 * @param porMob el afijo de cada especie, por id de entidad; {@code ""} es ninguno. Se pueden sumar especies (las
	 *        que un datapack agregue a los tags de jefes de Bloodmoon)
	 * @param nivelDeMarchito el nivel del Wither que dan los golpes "del Marchitamiento" (2 = II)
	 * @param segundosDeBrebaje lo que dura el brebaje que no es instantáneo (resistencia al fuego, respiración)
	 * @param segundosEntreBrebajes la espera entre un brebaje y el siguiente
	 * @param segundosDeLlamarada cuánto prende "de la Llamarada" al que te pega cuerpo a cuerpo
	 * @param distanciaDeTeletransporte hasta cuántos bloques salta "del Vacío" (como la fruta de chorus, que salta 8)
	 */
	public record AfijosDeJefe(double chance, Map<String, String> porMob, int nivelDeMarchito, int segundosDeBrebaje, int segundosEntreBrebajes,
			int segundosDeLlamarada, int segundosEntreTeletransportes, int distanciaDeTeletransporte) {
	}

	/** Desde qué nivel puede salir una rareza y cuánto pesa en el sorteo contra las otras que ya pueden salir. */
	public record ChanceDeRareza(int nivelMinimo, double peso) {
	}
}
