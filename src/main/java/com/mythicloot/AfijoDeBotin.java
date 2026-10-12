package com.mythicloot;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.DoubleFunction;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Un rasgo de un objeto. Los que tienen atributo los aplica vanilla al equiparlo (y su tooltip los muestra solo); los
 * que no, los aplica {@link Efectos} al pegar o al disparar, y se describen en el lore.
 *
 * <p>El id es para siempre: va en el {@code custom_data} de cada objeto que ya se soltó.
 */
public enum AfijoDeBotin {
	AFILADO("afilado", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO), Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE, null),
	// Se multiplica sobre la base más el arma, como la velocidad de ataque de Hero's Forge
	FURIOSO("furioso", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO), Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, null),
	LARGO("largo", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO), Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_VALUE, null),
	VAMPIRICO("vampirico", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO, TipoDeObjeto.A_DISTANCIA), null, null,
			valor -> Idioma.guardado("mythicloot.afijo.vampirico.descripcion", Texto.numero(valor * 100))),
	ARDIENTE("ardiente", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO, TipoDeObjeto.A_DISTANCIA), null, null,
			valor -> valor == 1 ? Idioma.guardado("mythicloot.afijo.ardiente.descripcion.uno")
					: Idioma.guardado("mythicloot.afijo.ardiente.descripcion.varios", Texto.numero(valor))),
	CERTERO("certero", EnumSet.of(TipoDeObjeto.A_DISTANCIA), null, null,
			valor -> Idioma.guardado("mythicloot.afijo.certero.descripcion", Texto.numero(valor * 100))),
	LIGERO("ligero", EnumSet.of(TipoDeObjeto.A_DISTANCIA, TipoDeObjeto.ARMADURA), Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, null),
	ROBUSTO("robusto", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.MAX_HEALTH, AttributeModifier.Operation.ADD_VALUE, null),
	REFORZADO("reforzado", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.ARMOR, AttributeModifier.Operation.ADD_VALUE, null),
	TENAZ("tenaz", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE, null),
	FIRME("firme", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE, null),
	AFORTUNADO("afortunado", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.LUCK, AttributeModifier.Operation.ADD_VALUE, null);

	private final String id;
	private final Set<TipoDeObjeto> tipos;
	private final @Nullable Holder<Attribute> atributo;
	private final AttributeModifier.@Nullable Operation operacion;
	private final @Nullable DoubleFunction<Component> descripcion;

	AfijoDeBotin(String id, Set<TipoDeObjeto> tipos, @Nullable Holder<Attribute> atributo,
			AttributeModifier.@Nullable Operation operacion, @Nullable DoubleFunction<Component> descripcion) {
		this.id = id;
		this.tipos = tipos;
		this.atributo = atributo;
		this.operacion = operacion;
		this.descripcion = descripcion;
	}

	public String id() {
		return id;
	}

	/**
	 * Lo que se agrega al nombre del objeto ("Espada de hierro del Vampiro", "Iron Sword of the Vampire"): sin adjetivos,
	 * no hay que concordar género, y va al final en los dos idiomas.
	 */
	public Component sufijo() {
		return Idioma.guardado("mythicloot.afijo." + id + ".sufijo");
	}

	public boolean sirvePara(TipoDeObjeto tipo) {
		return tipos.contains(tipo);
	}

	/** Null si no es un atributo: entonces lo aplica {@link Efectos} y va en el lore. */
	public @Nullable Holder<Attribute> atributo() {
		return atributo;
	}

	public AttributeModifier.@Nullable Operation operacion() {
		return operacion;
	}

	/** La línea del lore de los que no son atributos; null para los que ya muestra el tooltip de vanilla. */
	public @Nullable Component describir(double valor) {
		return descripcion == null ? null : descripcion.apply(valor);
	}

	/**
	 * Cuánto vale el afijo con ese nivel: arranca en un cuarto del máximo y llega entero en el nivel máximo. El azar
	 * saca entre el 75 % y el 100 % de eso, y la rareza lo multiplica.
	 */
	public static double valor(double maximo, int nivel, int nivelMaximo, double azar, Rareza rareza) {
		double escala = 0.25 + 0.75 * Math.min(nivel, nivelMaximo) / nivelMaximo;
		return maximo * escala * (0.75 + 0.25 * azar) * rareza.bonificacion();
	}

	public static @Nullable AfijoDeBotin porId(String id) {
		for (AfijoDeBotin afijo : values()) {
			if (afijo.id.equals(id)) {
				return afijo;
			}
		}
		return null;
	}
}
