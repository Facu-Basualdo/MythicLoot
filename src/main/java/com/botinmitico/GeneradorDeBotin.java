package com.botinmitico;

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
			{Items.COPPER_SWORD, Items.COPPER_AXE},
			{Items.IRON_SWORD, Items.IRON_AXE},
			{Items.IRON_SWORD, Items.IRON_AXE},
			{Items.DIAMOND_SWORD, Items.DIAMOND_AXE},
			{Items.NETHERITE_SWORD, Items.NETHERITE_AXE}};
	private static final Item[][] ARMADURA = {
			{Items.COPPER_HELMET, Items.COPPER_CHESTPLATE, Items.COPPER_LEGGINGS, Items.COPPER_BOOTS},
			{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
			{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
			{Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
			{Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS}};
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

		return armar(stack, tipo, new ObjetoConAfijos(nivelDelObjeto, rareza, afijos), principal);
	}

	/**
	 * Le pone a un objeto base los afijos ya elegidos: atributos, datos, nombre y lore. {@code principal} es el que da
	 * el nombre ("del Vampiro"); null deja el nombre de vanilla.
	 */
	public static ItemStack armar(ItemStack stack, TipoDeObjeto tipo, ObjetoConAfijos objeto, @Nullable AfijoDeBotin principal) {
		Rareza rareza = objeto.rareza();
		ponerAtributos(stack, tipo, objeto);
		objeto.guardarEn(stack);

		// En ITEM_NAME y no en CUSTOM_NAME: no sale en cursiva ni como "renombrado", y un yunque lo puede renombrar
		MutableComponent nombre = Component.empty().append(stack.getHoverName());
		if (principal != null) {
			nombre.append(" " + principal.sufijo());
		}
		stack.set(DataComponents.ITEM_NAME, nombre.withStyle(rareza.color()));

		List<Component> lore = new ArrayList<>();
		lore.add(Texto.lore("Rareza: " + rareza.nombre(), rareza.color()));
		lore.add(Texto.lore("Nivel " + objeto.nivel(), ChatFormatting.GRAY));
		objeto.afijos().forEach((afijo, valor) -> {
			String linea = afijo.describir(valor);
			if (linea != null) {
				// Azul, como las líneas de atributos que agrega vanilla abajo
				lore.add(Texto.lore(linea, ChatFormatting.BLUE));
			}
		});
		stack.set(DataComponents.LORE, new ItemLore(lore));
		if (rareza == Rareza.LEGENDARIA) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		return stack;
	}

	/**
	 * Suma los afijos que son atributos a los que el objeto ya trae de vanilla (el daño de la espada, la armadura del
	 * peto): si se pusiera un componente nuevo, la espada perdería su daño. El id lleva el grupo de slots porque dos
	 * piezas con el mismo id en el mismo atributo se pisan: la segunda reemplaza a la primera y no suman.
	 */
	private static void ponerAtributos(ItemStack stack, TipoDeObjeto tipo, ObjetoConAfijos objeto) {
		EquipmentSlotGroup grupo = EquipmentSlotGroup.MAINHAND;
		Equippable equipable = stack.get(DataComponents.EQUIPPABLE);
		if (tipo == TipoDeObjeto.ARMADURA && equipable != null) {
			grupo = EquipmentSlotGroup.bySlot(equipable.slot());
		}
		ItemAttributeModifiers modificadores = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		for (Map.Entry<AfijoDeBotin, Double> entrada : objeto.afijos().entrySet()) {
			AfijoDeBotin afijo = entrada.getKey();
			if (afijo.atributo() != null) {
				AttributeModifier modificador = new AttributeModifier(BotinMitico.id(afijo.id() + "/" + grupo.getSerializedName()), entrada.getValue(), afijo.operacion());
				modificadores = modificadores.withModifierAdded(afijo.atributo(), modificador, grupo);
			}
		}
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modificadores);
	}

	private static Item base(Configuracion config, int nivel, Rareza rareza, TipoDeObjeto tipo, RandomSource random) {
		int material = Math.min(nivel / config.nivelesPorMaterial(), CUERPO_A_CUERPO.length - 1);
		// La netherita se consigue con esfuerzo en vanilla: como botín común sería regalarla
		if (material > DIAMANTE && config.netheritaSoloLegendaria() && rareza != Rareza.LEGENDARIA) {
			material = DIAMANTE;
		}
		return switch (tipo) {
			case CUERPO_A_CUERPO -> CUERPO_A_CUERPO[material][random.nextInt(CUERPO_A_CUERPO[material].length)];
			case ARMADURA -> ARMADURA[material][random.nextInt(ARMADURA[material].length)];
			case A_DISTANCIA -> A_DISTANCIA[random.nextInt(A_DISTANCIA.length)];
		};
	}

	/** El fuego en segundos enteros; el resto con tres decimales, que alcanza para un porcentaje con un decimal. */
	private static double redondear(AfijoDeBotin afijo, double valor) {
		if (afijo == AfijoDeBotin.ARDIENTE) {
			return Math.max(1, Math.round(valor));
		}
		return Math.round(valor * 1000) / 1000.0;
	}
}
