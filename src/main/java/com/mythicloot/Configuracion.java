package com.mythicloot;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Los números de balance. Se lee de {@code config/mythicloot.json} mezclado sobre {@link #POR_DEFECTO} (ver
 * {@link ArchivoDeConfiguracion}). Los mapas usan los ids de {@link Rareza}, {@link TipoDeObjeto} y {@link AfijoDeBotin}.
 *
 * @param bloquesPorNivel sin Bloodmoon & Bosses, cada cuántos bloques del spawn sube el nivel del botín
 * @param chanceBase chance de soltar un objeto en el nivel 0; se suma chancePorNivel por nivel, hasta chanceMaxima
 * @param multiplicadorCampeon por cuánto se multiplica la chance si el mob es un campeón de Bloodmoon & Bosses
 * @param rarezaDeJefe la rareza mínima del objeto que suelta siempre un jefe de zona
 * @param nivelesPorMaterial cada cuántos niveles mejora el material (cobre, hierro, diamante…)
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
		Map<String, ChanceDeRareza> rarezas,
		Map<String, Double> pesosDeTipo,
		int nivelesPorMaterial,
		boolean netheritaSoloLegendaria,
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
			ordenado(Map.of(
					Rareza.MAGICA.getSerializedName(), new ChanceDeRareza(1, 60),
					Rareza.RARA.getSerializedName(), new ChanceDeRareza(20, 30),
					Rareza.EPICA.getSerializedName(), new ChanceDeRareza(50, 12),
					Rareza.LEGENDARIA.getSerializedName(), new ChanceDeRareza(80, 4)), Rareza.values()),
			ordenado(Map.of(
					TipoDeObjeto.CUERPO_A_CUERPO.getSerializedName(), 40.0,
					TipoDeObjeto.A_DISTANCIA.getSerializedName(), 20.0,
					TipoDeObjeto.ARMADURA.getSerializedName(), 40.0), TipoDeObjeto.values()),
			20,
			true,
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
		return valores;
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
		exigir(nivelesPorMaterial >= 1, "nivelesPorMaterial tiene que ser 1 o más");
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

	/** Desde qué nivel puede salir una rareza y cuánto pesa en el sorteo contra las otras que ya pueden salir. */
	public record ChanceDeRareza(int nivelMinimo, double peso) {
	}
}
