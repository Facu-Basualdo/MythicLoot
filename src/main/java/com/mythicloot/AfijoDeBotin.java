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
	AFILADO("afilado", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO, TipoDeObjeto.TRIDENTE), Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_VALUE, null),
	// Se multiplica sobre la base más el arma, como la velocidad de ataque de Hero's Forge
	FURIOSO("furioso", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO, TipoDeObjeto.TRIDENTE), Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, null),
	LARGO("largo", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO, TipoDeObjeto.TRIDENTE), Attributes.ENTITY_INTERACTION_RANGE, AttributeModifier.Operation.ADD_VALUE, null),
	VAMPIRICO("vampirico", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO, TipoDeObjeto.A_DISTANCIA, TipoDeObjeto.TRIDENTE), null, null,
			valor -> Idioma.guardado("mythicloot.afijo.vampirico.descripcion", Texto.numero(valor * 100))),
	ARDIENTE("ardiente", EnumSet.of(TipoDeObjeto.CUERPO_A_CUERPO, TipoDeObjeto.A_DISTANCIA, TipoDeObjeto.TRIDENTE), null, null,
			valor -> valor == 1 ? Idioma.guardado("mythicloot.afijo.ardiente.descripcion.uno")
					: Idioma.guardado("mythicloot.afijo.ardiente.descripcion.varios", Texto.numero(valor))),
	CERTERO("certero", EnumSet.of(TipoDeObjeto.A_DISTANCIA, TipoDeObjeto.TRIDENTE), null, null,
			valor -> Idioma.guardado("mythicloot.afijo.certero.descripcion", Texto.numero(valor * 100))),
	LIGERO("ligero", EnumSet.of(TipoDeObjeto.A_DISTANCIA, TipoDeObjeto.ARMADURA, TipoDeObjeto.HERRAMIENTA), Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, null),
	// Los de armadura también van en el escudo: valen con el escudo en la mano secundaria
	ROBUSTO("robusto", EnumSet.of(TipoDeObjeto.ARMADURA, TipoDeObjeto.ESCUDO), Attributes.MAX_HEALTH, AttributeModifier.Operation.ADD_VALUE, null),
	REFORZADO("reforzado", EnumSet.of(TipoDeObjeto.ARMADURA, TipoDeObjeto.ESCUDO), Attributes.ARMOR, AttributeModifier.Operation.ADD_VALUE, null),
	TENAZ("tenaz", EnumSet.of(TipoDeObjeto.ARMADURA, TipoDeObjeto.ESCUDO), Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADD_VALUE, null),
	FIRME("firme", EnumSet.of(TipoDeObjeto.ARMADURA, TipoDeObjeto.ESCUDO), Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE, null),
	AFORTUNADO("afortunado", EnumSet.of(TipoDeObjeto.ARMADURA, TipoDeObjeto.ESCUDO), Attributes.LUCK, AttributeModifier.Operation.ADD_VALUE, null),
	// Como Eficiencia: sólo cuenta con la herramienta que corresponde al bloque
	EFICIENTE("eficiente", EnumSet.of(TipoDeObjeto.HERRAMIENTA), Attributes.MINING_EFFICIENCY, AttributeModifier.Operation.ADD_VALUE, null),
	AMPLIO("amplio", EnumSet.of(TipoDeObjeto.HERRAMIENTA), Attributes.BLOCK_INTERACTION_RANGE, AttributeModifier.Operation.ADD_VALUE, null),
	// La base es 0,2: con 0,8 más mina bajo el agua como con Afinidad acuática
	SUBMARINO("submarino", EnumSet.of(TipoDeObjeto.HERRAMIENTA), Attributes.SUBMERGED_MINING_SPEED, AttributeModifier.Operation.ADD_VALUE, null),

	// De jefe: sólo los suelta el jefe de zona de Bloodmoon que corresponde (ver Configuracion.AfijosDeJefe), en
	// cualquier tipo de objeto, y valen mientras el objeto está equipado en su lugar (ver EfectosDeJefe)
	FESTIN("festin", valor -> Idioma.guardado("mythicloot.afijo.festin.descripcion", Texto.numero(valor * 100))),
	LLUVIA("lluvia", valor -> Idioma.guardado("mythicloot.afijo.lluvia.descripcion", Texto.numero(valor * 100))),
	VERDUGO("verdugo", valor -> Idioma.guardado("mythicloot.afijo.verdugo.descripcion", Texto.numero(valor * 100))),
	BREBAJE("brebaje", valor -> Idioma.guardado("mythicloot.afijo.brebaje.descripcion", Texto.numero(valor * 100))),
	MARCHITO("marchito", valor -> valor == 1 ? Idioma.guardado("mythicloot.afijo.marchito.descripcion.uno")
			: Idioma.guardado("mythicloot.afijo.marchito.descripcion.varios", Texto.numero(valor))),
	BRUTO("bruto", valor -> Idioma.guardado("mythicloot.afijo.bruto.descripcion", Texto.numero(valor * 100))),
	LLAMARADA("llamarada", valor -> Idioma.guardado("mythicloot.afijo.llamarada.descripcion", Texto.numero(valor * 100))),
	VACIO("vacio", valor -> Idioma.guardado("mythicloot.afijo.vacio.descripcion", Texto.numero(valor * 100)));

	private final String id;
	private final Set<TipoDeObjeto> tipos;
	private final @Nullable Holder<Attribute> atributo;
	private final AttributeModifier.@Nullable Operation operacion;
	private final @Nullable DoubleFunction<Component> descripcion;
	private final boolean deJefe;

	AfijoDeBotin(String id, Set<TipoDeObjeto> tipos, @Nullable Holder<Attribute> atributo,
			AttributeModifier.@Nullable Operation operacion, @Nullable DoubleFunction<Component> descripcion) {
		this.id = id;
		this.tipos = tipos;
		this.atributo = atributo;
		this.operacion = operacion;
		this.descripcion = descripcion;
		this.deJefe = false;
	}

	/** Uno de jefe: no sale en el sorteo de ningún tipo, no es un atributo y no ocupa lugar entre los de la rareza. */
	AfijoDeBotin(String id, DoubleFunction<Component> descripcion) {
		this.id = id;
		this.tipos = EnumSet.noneOf(TipoDeObjeto.class);
		this.atributo = null;
		this.operacion = null;
		this.descripcion = descripcion;
		this.deJefe = true;
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

	public boolean deJefe() {
		return deJefe;
	}

	/** Los que se miden en segundos enteros (el fuego, el marchitamiento); el resto son fracciones. */
	public boolean enSegundos() {
		return this == ARDIENTE || this == MARCHITO;
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
