package com.mythicloot;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Lo que el mod guarda en el objeto, dentro de {@code custom_data} bajo la clave {@code mythicloot}:
 * {@code {nivel: 57, rareza: "epica", afijos: {vampirico: 0.05, certero: 0.3}}}. Es un componente de vanilla: viaja
 * con el objeto a cofres, al yunque y a otros jugadores sin que el cliente necesite el mod.
 *
 * <p>Los atributos también quedan en {@code attribute_modifiers}, que es lo que usa vanilla; acá están todos los
 * afijos, para los que no son atributos y para saber qué es cada objeto.
 */
public record ObjetoConAfijos(int nivel, Rareza rareza, Map<AfijoDeBotin, Double> afijos) {
	static final String CLAVE = MythicLoot.MOD_ID;

	public ObjetoConAfijos {
		EnumMap<AfijoDeBotin, Double> copia = new EnumMap<>(AfijoDeBotin.class);
		copia.putAll(afijos);
		afijos = Collections.unmodifiableMap(copia);
	}

	public double valor(AfijoDeBotin afijo) {
		return afijos.getOrDefault(afijo, 0.0);
	}

	/** Null si el objeto no es del mod (o si alguien le borró la clave). Un afijo que ya no existe se ignora. */
	public static @Nullable ObjetoConAfijos de(@Nullable ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		CustomData datos = stack.get(DataComponents.CUSTOM_DATA);
		if (datos == null) {
			return null;
		}
		CompoundTag raiz = datos.copyTag();
		if (!raiz.contains(CLAVE)) {
			return null;
		}
		CompoundTag propio = raiz.getCompoundOrEmpty(CLAVE);
		Rareza rareza = Rareza.porId(propio.getStringOr("rareza", ""));
		if (rareza == null) {
			return null;
		}
		Map<AfijoDeBotin, Double> afijos = new EnumMap<>(AfijoDeBotin.class);
		CompoundTag guardados = propio.getCompoundOrEmpty("afijos");
		for (String id : guardados.keySet()) {
			AfijoDeBotin afijo = AfijoDeBotin.porId(id);
			if (afijo != null) {
				afijos.put(afijo, guardados.getDoubleOr(id, 0));
			}
		}
		return new ObjetoConAfijos(propio.getIntOr("nivel", 1), rareza, afijos);
	}

	/** Lo guarda sin pisar otras claves de {@code custom_data} (las de otro mod o de un datapack). */
	public void guardarEn(ItemStack stack) {
		CompoundTag propio = new CompoundTag();
		propio.putInt("nivel", nivel);
		propio.putString("rareza", rareza.getSerializedName());
		CompoundTag guardados = new CompoundTag();
		afijos.forEach((afijo, valor) -> guardados.putDouble(afijo.id(), valor));
		propio.put("afijos", guardados);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, raiz -> raiz.put(CLAVE, propio));
	}
}
