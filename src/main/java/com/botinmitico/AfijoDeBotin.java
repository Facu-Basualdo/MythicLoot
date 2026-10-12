package com.botinmitico;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.DoubleFunction;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
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
	AFILADO("afilado", "del Filo", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO), Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE, null),
	// Se multiplica sobre la base más el arma, como la velocidad de ataque de Hero's Forge
	FURIOSO("furioso", "de la Furia", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO), Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, null),
	LARGO("largo", "del Alcance", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO), Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_VALUE, null),
	VAMPIRICO("vampirico", "del Vampiro", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO, TipoDeObjeto.A_DISTANCIA), null, null,
			valor -> "Roba el " + Texto.numero(valor * 100) + " % del daño que hacés"),
	ARDIENTE("ardiente", "de las Llamas", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO, TipoDeObjeto.A_DISTANCIA), null, null,
			valor -> "Prende fuego " + Texto.numero(valor) + (valor == 1 ? " segundo" : " segundos")),
	CERTERO("certero", "del Halcón", EnumSet.of(TipoDeObjeto.A_DISTANCIA), null, null,
			valor -> "+" + Texto.numero(valor * 100) + " % de daño de las flechas"),
	LIGERO("ligero", "del Viento", EnumSet.of(TipoDeObjeto.A_DISTANCIA, TipoDeObjeto.ARMADURA), Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, null),
	ROBUSTO("robusto", "del Gigante", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.MAX_HEALTH, AttributeModifier.Operation.ADD_VALUE, null),
	REFORZADO("reforzado", "de la Fortaleza", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.ARMOR, AttributeModifier.Operation.ADD_VALUE, null),
	TENAZ("tenaz", "del Coloso", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE, null),
	FIRME("firme", "de la Montaña", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE, null),
	AFORTUNADO("afortunado", "de la Suerte", EnumSet.of(TipoDeObjeto.ARMADURA), Attributes.LUCK, AttributeModifier.Operation.ADD_VALUE, null);

	private final String id;
	private final String sufijo;
	private final Set<TipoDeObjeto> tipos;
	private final @Nullable Holder<Attribute> atributo;
	private final AttributeModifier.@Nullable Operation operacion;
	private final @Nullable DoubleFunction<String> descripcion;

	AfijoDeBotin(String id, String sufijo, Set<TipoDeObjeto> tipos, @Nullable Holder<Attribute> atributo,
			AttributeModifier.@Nullable Operation operacion, @Nullable DoubleFunction<String> descripcion) {
		this.id = id;
		this.sufijo = sufijo;
		this.tipos = tipos;
		this.atributo = atributo;
		this.operacion = operacion;
		this.descripcion = descripcion;
	}

	public String id() {
		return id;
	}

	/** Lo que se agrega al nombre del objeto ("Espada de hierro del Vampiro"): sin adjetivos, no hay que concordar género. */
	public String sufijo() {
		return sufijo;
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
	public @Nullable String describir(double valor) {
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
