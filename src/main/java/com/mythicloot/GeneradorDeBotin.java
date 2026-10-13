package com.mythicloot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.Equippable;

/** Arma un objeto con afijos: elige la rareza, el objeto base según el nivel y los afijos, y lo deja listo para soltar. */
public final class GeneradorDeBotin {
	// Por material, de peor a mejor: cobre, hierro (cadena en armadura), hierro, diamante, netherita
	private static final Item[][] CUERPO_A_CUERPO = {
			{Items.COPPER_SWORD, Items.COPPER_AXE, Items.COPPER_SPEAR},
			{Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_SPEAR},
			{Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_SPEAR},
			{Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_SPEAR},
			{Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_SPEAR}};
	private static final Item[][] ARMADURA = {
			{Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS},
			{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
			{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
			{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
			{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}};
	private static final Item[][] HERRAMIENTA = {
			{Items.COPPER_PICKAXE, Items.COPPER_SHOVEL},
			{Items.IRON_PICKAXE, Items.IRON_SHOVEL},
			{Items.IRON_PICKAXE, Items.IRON_SHOVEL},
			{Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL},
			{Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL}};
	private static final Item[] A_DISTANCIA = {Items.BOW, Items.BOW, Items.CROSSBOW};
	private static final int DIAMANTE = 3;

	private GeneradorDeBotin() {
	}

	/** Entre las rarezas que ya pueden salir con ese nivel, por peso. */
	public static Rareza elegirRareza(Configuracion config, int nivel, RandomSource random) {
		return elegirRareza(config, nivel, Rareza.MAGICA, random);
	}

	/**
	 * Como la otra, pero nunca peor que {@code minima} (el jefe de zona). Si con ese nivel todavía no sale ninguna tan
	 * buena (un jefe de nivel 30 con mínima épica, que arranca en el 50), da la mínima igual: el jefe la asegura.
	 */
	public static Rareza elegirRareza(Configuracion config, int nivel, Rareza minima, RandomSource random) {
		Map<Rareza, Double> posibles = new LinkedHashMap<>();
		double total = 0;
		for (Rareza rareza : Rareza.values()) {
			Configuracion.ChanceDeRareza chance = config.rareza(rareza);
			if (rareza.compareTo(minima) >= 0 && nivel >= chance.nivelMinimo() && chance.peso() > 0) {
				posibles.put(rareza, chance.peso());
				total += chance.peso();
			}
		}
		if (posibles.isEmpty()) {
			return minima;
		}
		double tirada = random.nextDouble() * total;
		for (Map.Entry<Rareza, Double> entrada : posibles.entrySet()) {
			tirada -= entrada.getValue();
			if (tirada < 0) {
				return entrada.getKey();
			}
		}
		return posibles.keySet().iterator().next();
	}

	public static TipoDeObjeto elegirTipo(Configuracion config, RandomSource random) {
		double total = 0;
		for (TipoDeObjeto tipo : TipoDeObjeto.values()) {
			total += config.pesoDe(tipo);
		}
		double tirada = random.nextDouble() * total;
		for (TipoDeObjeto tipo : TipoDeObjeto.values()) {
			tirada -= config.pesoDe(tipo);
			if (tirada < 0) {
				return tipo;
			}
		}
		return TipoDeObjeto.CUERPO_A_CUERPO;
	}

	/** Un objeto al azar de esa rareza y ese tipo, con los afijos ya puestos. */
	public static ItemStack generar(Configuracion config, int nivel, Rareza rareza, TipoDeObjeto tipo, RandomSource random) {
		return generar(config, nivel, rareza, tipo, null, random);
	}

	/**
	 * Como la otra, más un afijo de jefe ({@code deJefe}, o null): va aparte de los de la rareza y le da el nombre al
	 * objeto ("Espada de diamante del Verdugo").
	 */
	public static ItemStack generar(Configuracion config, int nivel, Rareza rareza, TipoDeObjeto tipo, @Nullable AfijoDeBotin deJefe, RandomSource random) {
		int nivelDelObjeto = Math.clamp(nivel, 1, config.nivelMaximo());
		ItemStack stack = new ItemStack(base(config, nivelDelObjeto, rareza, tipo, random));

		List<AfijoDeBotin> candidatos = new ArrayList<>();
		for (AfijoDeBotin afijo : AfijoDeBotin.values()) {
			if (afijo.sirvePara(tipo)) {
				candidatos.add(afijo);
			}
		}
		Map<AfijoDeBotin, Double> afijos = new EnumMap<>(AfijoDeBotin.class);
		AfijoDeBotin principal = null;
		for (int i = 0; i < rareza.afijos() && !candidatos.isEmpty(); i++) {
			AfijoDeBotin afijo = candidatos.remove(random.nextInt(candidatos.size()));
			afijos.put(afijo, redondear(afijo, AfijoDeBotin.valor(config.valorMaximo(afijo), nivelDelObjeto, config.nivelMaximo(), random.nextDouble(), rareza)));
			if (principal == null) {
				principal = afijo;
			}
		}
		if (deJefe != null) {
			afijos.put(deJefe, redondear(deJefe, AfijoDeBotin.valor(config.valorMaximo(deJefe), nivelDelObjeto, config.nivelMaximo(), random.nextDouble(), rareza)));
			principal = deJefe;
		}

		return armar(stack, new ObjetoConAfijos(nivelDelObjeto, rareza, afijos), principal);
	}

	/**
	 * Le pone a un objeto base los afijos ya elegidos: atributos, datos, nombre y lore. {@code principal} es el que da
	 * el nombre ("del Vampiro"); null deja el nombre de vanilla.
	 */
	public static ItemStack armar(ItemStack stack, ObjetoConAfijos objeto, @Nullable AfijoDeBotin principal) {
		// En ITEM_NAME y no en CUSTOM_NAME: no sale en cursiva ni como "renombrado", y un yunque lo puede renombrar
		MutableComponent nombre = Component.empty().append(stack.getHoverName());
		if (principal != null) {
			nombre.append(" ").append(principal.sufijo());
		}
		stack.set(DataComponents.ITEM_NAME, nombre.withStyle(objeto.rareza().color()));
		ponerAfijos(stack, objeto);
		return stack;
	}

	/**
	 * Le cambia los afijos a un objeto que ya tenía (el resultado del yunque): los atributos vuelven a los del objeto
	 * de vanilla antes de sumar los nuevos, para no sumar dos veces los viejos. El nombre se queda, con el color de la
	 * rareza nueva.
	 */
	public static void rearmar(ItemStack stack, ObjetoConAfijos objeto) {
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, stack.getPrototype().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
		Component nombre = stack.get(DataComponents.ITEM_NAME);
		if (nombre != null) {
			stack.set(DataComponents.ITEM_NAME, nombre.copy().withStyle(objeto.rareza().color()));
		}
		ponerAfijos(stack, objeto);
	}

	private static void ponerAfijos(ItemStack stack, ObjetoConAfijos objeto) {
		Rareza rareza = objeto.rareza();
		ponerAtributos(stack, objeto);
		objeto.guardarEn(stack);

		List<Component> lore = new ArrayList<>();
		lore.add(Texto.lore(Idioma.guardado("mythicloot.lore.rareza", rareza.nombre()), rareza.color()));
		lore.add(Texto.lore(Idioma.guardado("mythicloot.lore.nivel", objeto.nivel()), ChatFormatting.GRAY));
		objeto.afijos().forEach((afijo, valor) -> {
			Component linea = afijo.describir(valor);
			if (linea != null) {
				// Azul, como las líneas de atributos que agrega vanilla abajo; el de jefe en rojo, para que se distinga
				lore.add(Texto.lore(linea, afijo.deJefe() ? ChatFormatting.RED : ChatFormatting.BLUE));
			}
		});
		stack.set(DataComponents.LORE, new ItemLore(lore));
		if (rareza == Rareza.LEGENDARIA) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
	}

	/**
	 * Suma los afijos que son atributos a los que el objeto ya trae de vanilla (el daño de la espada, la armadura del
	 * peto): si se pusiera un componente nuevo, la espada perdería su daño. Los que agrega {@link #sumar} llevan el grupo
	 * de slots en el id porque dos piezas con el mismo id en el mismo atributo se pisan: la segunda reemplaza a la
	 * primera y no suman.
	 */
	private static void ponerAtributos(ItemStack stack, ObjetoConAfijos objeto) {
		EquipmentSlotGroup grupo = EquipmentSlotGroup.MAINHAND;
		Equippable equipable = stack.get(DataComponents.EQUIPPABLE);
		if (equipable != null && equipable.slot().isArmor()) {
			grupo = EquipmentSlotGroup.bySlot(equipable.slot());
		} else if (stack.has(DataComponents.BLOCKS_ATTACKS)) {
			// El escudo se usa en la mano secundaria: con la principal sus afijos no valdrían nunca
			grupo = EquipmentSlotGroup.OFFHAND;
		}
		ItemAttributeModifiers modificadores = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		for (Map.Entry<AfijoDeBotin, Double> entrada : objeto.afijos().entrySet()) {
			AfijoDeBotin afijo = entrada.getKey();
			if (afijo.atributo() != null) {
				modificadores = sumar(modificadores, afijo, entrada.getValue(), grupo);
			}
		}
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modificadores);
	}

	/**
	 * Si el objeto ya trae un modificador del mismo atributo, operación y slots (la armadura del peto, el daño de la
	 * espada), le suma el afijo: el tooltip muestra "Armadura: +4.5" y no dos líneas de armadura. Si no, agrega uno.
	 */
	private static ItemAttributeModifiers sumar(ItemAttributeModifiers modificadores, AfijoDeBotin afijo, double valor, EquipmentSlotGroup grupo) {
		List<ItemAttributeModifiers.Entry> entradas = new ArrayList<>(modificadores.modifiers());
		for (int i = 0; i < entradas.size(); i++) {
			ItemAttributeModifiers.Entry entrada = entradas.get(i);
			AttributeModifier existente = entrada.modifier();
			if (entrada.attribute().equals(afijo.atributo()) && entrada.slot() == grupo && existente.operation() == afijo.operacion()) {
				AttributeModifier sumado = new AttributeModifier(existente.id(), existente.amount() + valor, existente.operation());
				entradas.set(i, new ItemAttributeModifiers.Entry(entrada.attribute(), sumado, entrada.slot(), entrada.display()));
				return new ItemAttributeModifiers(entradas);
			}
		}
		AttributeModifier modificador = new AttributeModifier(MythicLoot.id(afijo.id() + "/" + grupo.getSerializedName()), valor, afijo.operacion());
		return modificadores.withModifierAdded(afijo.atributo(), modificador, grupo);
	}

	private static Item base(Configuracion config, int nivel, Rareza rareza, TipoDeObjeto tipo, RandomSource random) {
		int material = Math.min(nivel / config.nivelesPorMaterial(), CUERPO_A_CUERPO.length - 1);
		// La netherita se consigue con esfuerzo en vanilla: como botín común sería regalarla
		if (material > DIAMANTE && config.netheritaSoloLegendaria() && rareza != Rareza.LEGENDARIA) {
			material = DIAMANTE;
		}
		return switch (tipo) {
			case CUERPO_A_CUERPO -> cuerpoACuerpo(config, CUERPO_A_CUERPO[material], rareza, random);
			case ARMADURA -> ARMADURA[material][random.nextInt(ARMADURA[material].length)];
			case A_DISTANCIA -> A_DISTANCIA[random.nextInt(A_DISTANCIA.length)];
			case HERRAMIENTA -> HERRAMIENTA[material][random.nextInt(HERRAMIENTA[material].length)];
			case TRIDENTE -> Items.TRIDENT;
			case ESCUDO -> Items.SHIELD;
		};
	}

	/**
	 * La maza no tiene materiales y en vanilla sale de un cofre de las cámaras de prueba: con la rareza mínima
	 * ({@code rarezaMinimaDeMaza}) es una opción más, con la misma chance que la espada, el hacha o la lanza.
	 */
	private static Item cuerpoACuerpo(Configuracion config, Item[] opciones, Rareza rareza, RandomSource random) {
		boolean maza = rareza.compareTo(config.minimaDeMaza()) >= 0;
		int elegido = random.nextInt(opciones.length + (maza ? 1 : 0));
		return elegido < opciones.length ? opciones[elegido] : Items.MACE;
	}

	/** El fuego y el marchitamiento en segundos enteros; el resto con tres decimales, que alcanza para un porcentaje con un decimal. */
	private static double redondear(AfijoDeBotin afijo, double valor) {
		if (afijo.enSegundos()) {
			return Math.max(1, Math.round(valor));
		}
		return Math.round(valor * 1000) / 1000.0;
	}
}
