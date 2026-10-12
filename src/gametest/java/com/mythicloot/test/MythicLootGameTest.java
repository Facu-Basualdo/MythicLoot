package com.mythicloot.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;

import com.mythicloot.AfijoDeBotin;
import com.mythicloot.ArchivoDeConfiguracion;
import com.mythicloot.MythicLoot;
import com.mythicloot.Caida;
import com.mythicloot.Configuracion;
import com.mythicloot.GeneradorDeBotin;
import com.mythicloot.NivelDelMob;
import com.mythicloot.ObjetoConAfijos;
import com.mythicloot.Rareza;
import com.mythicloot.TipoDeObjeto;
import com.mythicloot.mixin.FlechaAccessor;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MythicLootGameTest {
	private static final BlockPos DENTRO = new BlockPos(2, 1, 2);

	@GameTest
	public void generarCadaTipoYRareza(GameTestHelper helper) {
		Configuracion config = Configuracion.POR_DEFECTO;
		RandomSource random = RandomSource.create(7);
		for (TipoDeObjeto tipo : TipoDeObjeto.values()) {
			for (Rareza rareza : Rareza.values()) {
				ItemStack stack = GeneradorDeBotin.generar(config, 60, rareza, tipo, random);
				String que = tipo.getSerializedName() + " " + rareza.getSerializedName();
				ObjetoConAfijos objeto = ObjetoConAfijos.de(stack);
				helper.assertTrue(objeto != null, Component.literal(que + ": el objeto tiene sus datos"));
				helper.assertValueEqual(objeto.nivel(), 60, que + ": nivel");
				helper.assertValueEqual(objeto.rareza(), rareza, que + ": rareza");
				helper.assertValueEqual(objeto.afijos().size(), rareza.afijos(), que + ": un afijo por cada uno de la rareza");

				ItemAttributeModifiers modificadores = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
				ItemAttributeModifiers deVanilla = new ItemStack(stack.getItem()).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
				for (Map.Entry<AfijoDeBotin, Double> entrada : objeto.afijos().entrySet()) {
					AfijoDeBotin afijo = entrada.getKey();
					helper.assertTrue(afijo.sirvePara(tipo), Component.literal(que + ": " + afijo + " sirve para el tipo"));
					helper.assertTrue(entrada.getValue() > 0, Component.literal(que + ": " + afijo + " vale algo"));
					if (afijo.atributo() != null) {
						// Puede ir sumado al modificador de vanilla o aparte: lo que importa es el total
						assertCerca(helper, suma(modificadores, afijo) - suma(deVanilla, afijo), entrada.getValue(), que + ": " + afijo + " suma su valor en attribute_modifiers");
					}
				}
				if (tipo == TipoDeObjeto.CUERPO_A_CUERPO) {
					boolean danioDeVanilla = modificadores.modifiers().stream().anyMatch(m -> m.attribute().equals(Attributes.ATTACK_DAMAGE)
							&& !m.modifier().id().getNamespace().equals(MythicLoot.MOD_ID));
					helper.assertTrue(danioDeVanilla, Component.literal(que + ": el arma conserva su daño de vanilla"));
				}
				String nombre = stack.getHoverName().getString();
				helper.assertTrue(objeto.afijos().keySet().stream().anyMatch(a -> nombre.endsWith(a.sufijo())), Component.literal(que + ": el nombre lleva un sufijo: " + nombre));
				helper.assertValueEqual(stack.get(DataComponents.LORE).lines().getFirst().getString(), "Rareza: " + rareza.nombre(), que + ": primera línea del lore");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void valoresYMaterialesSegunElNivel(GameTestHelper helper) {
		assertCerca(helper, AfijoDeBotin.valor(6, 100, 100, 1, Rareza.RARA), 6, "En el nivel máximo y con la mejor tirada, el máximo");
		assertCerca(helper, AfijoDeBotin.valor(6, 100, 100, 1, Rareza.LEGENDARIA), 7.5, "La legendaria rinde un 25 % más");
		assertCerca(helper, AfijoDeBotin.valor(6, 0, 100, 0, Rareza.MAGICA), 6 * 0.25 * 0.75, "En el nivel 0 y con la peor tirada, el mínimo");
		assertCerca(helper, AfijoDeBotin.valor(6, 500, 100, 1, Rareza.RARA), 6, "Pasar el nivel máximo no da más");

		Configuracion config = Configuracion.POR_DEFECTO;
		RandomSource random = RandomSource.create(3);
		ItemStack bajo = GeneradorDeBotin.generar(config, 5, Rareza.MAGICA, TipoDeObjeto.CUERPO_A_CUERPO, random);
		helper.assertTrue(bajo.is(Items.COPPER_SWORD) || bajo.is(Items.COPPER_AXE), Component.literal("En el nivel 5, de cobre: " + bajo.getItem()));
		for (int i = 0; i < 10; i++) {
			ItemStack epica = GeneradorDeBotin.generar(config, 90, Rareza.EPICA, TipoDeObjeto.ARMADURA, random);
			helper.assertTrue(epica.getItem().toString().contains("diamond"), Component.literal("Una épica de nivel 90 es de diamante: " + epica.getItem()));
			ItemStack legendaria = GeneradorDeBotin.generar(config, 90, Rareza.LEGENDARIA, TipoDeObjeto.ARMADURA, random);
			helper.assertTrue(legendaria.getItem().toString().contains("netherite"), Component.literal("Una legendaria de nivel 90 es de netherita: " + legendaria.getItem()));
			helper.assertTrue(legendaria.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), Component.literal("Y brilla"));
		}

		for (int i = 0; i < 50; i++) {
			helper.assertValueEqual(GeneradorDeBotin.elegirRareza(config, 10, random), Rareza.MAGICA, "En el nivel 10 sólo sale mágica");
			helper.assertTrue(GeneradorDeBotin.elegirRareza(config, 10, Rareza.EPICA, random) == Rareza.EPICA,
					Component.literal("Un jefe de nivel 10 da la mínima aunque todavía no salga"));
			helper.assertTrue(GeneradorDeBotin.elegirRareza(config, 90, Rareza.EPICA, random).compareTo(Rareza.EPICA) >= 0,
					Component.literal("Un jefe nunca da menos que la mínima"));
		}
		helper.succeed();
	}

	/** Dos piezas con el mismo afijo tienen que sumar: si el id fuera el mismo, la segunda pisaría a la primera. */
	@GameTest
	public void dosPiezasDeArmaduraSuman(GameTestHelper helper) {
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
		double vida = zombie.getAttributeValue(Attributes.MAX_HEALTH);
		zombie.setItemSlot(EquipmentSlot.HEAD, conAfijo(new ItemStack(Items.IRON_HELMET), AfijoDeBotin.ROBUSTO, 4));
		zombie.setItemSlot(EquipmentSlot.FEET, conAfijo(new ItemStack(Items.IRON_BOOTS), AfijoDeBotin.ROBUSTO, 4));
		// La vida de la armadura la aplica el tick del mob (collectEquipmentChanges)
		helper.runAfterDelay(2, () -> {
			assertCerca(helper, zombie.getAttributeValue(Attributes.MAX_HEALTH), vida + 8, "Casco y botas \"del Gigante\" suman 4 + 4");
			zombie.discard();
			helper.succeed();
		});
	}

	/** "Peto de la Fortaleza" tiene que mostrar una sola línea de armadura, con la del peto y la del afijo sumadas. */
	@GameTest
	public void elAfijoSeSumaAlAtributoDelObjeto(GameTestHelper helper) {
		ItemStack peto = conAfijo(new ItemStack(Items.IRON_CHESTPLATE), AfijoDeBotin.REFORZADO, 0.5);
		double deVanilla = suma(new ItemStack(Items.IRON_CHESTPLATE).get(DataComponents.ATTRIBUTE_MODIFIERS), AfijoDeBotin.REFORZADO);
		List<ItemAttributeModifiers.Entry> armadura = peto.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream()
				.filter(m -> m.attribute().equals(Attributes.ARMOR)).toList();
		helper.assertValueEqual(armadura.size(), 1, "Un solo modificador de armadura");
		assertCerca(helper, armadura.getFirst().modifier().amount(), deVanilla + 0.5, "La del peto más la del afijo");
		helper.succeed();
	}

	@GameTest
	public void vampiricoYArdienteAlPegar(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = entrar(helper, "cazador");
		ItemStack espada = new ItemStack(Items.IRON_SWORD);
		Map<AfijoDeBotin, Double> afijos = new EnumMap<>(AfijoDeBotin.class);
		afijos.put(AfijoDeBotin.VAMPIRICO, 0.1);
		afijos.put(AfijoDeBotin.ARDIENTE, 3.0);
		GeneradorDeBotin.armar(espada, new ObjetoConAfijos(50, Rareza.RARA, afijos), AfijoDeBotin.VAMPIRICO);
		jugador.setItemSlot(EquipmentSlot.MAINHAND, espada);
		jugador.setHealth(10);

		IronGolem blanco = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, DENTRO);
		blanco.hurtServer(level, jugador.damageSources().playerAttack(jugador), 8);
		assertCerca(helper, jugador.getHealth(), 10.8, "El vampírico cura el 10 % del golpe");
		helper.assertTrue(blanco.getRemainingFireTicks() > 0, Component.literal("El ardiente lo prende fuego"));

		jugador.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		IronGolem otro = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, DENTRO.east(2));
		otro.hurtServer(level, jugador.damageSources().playerAttack(jugador), 8);
		assertCerca(helper, jugador.getHealth(), 10.8, "Una espada común no cura");
		helper.assertTrue(otro.getRemainingFireTicks() <= 0, Component.literal("Ni prende fuego"));

		blanco.discard();
		otro.discard();
		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void certeroYArdienteEnLasFlechas(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = entrar(helper, "arquero");
		ItemStack arco = new ItemStack(Items.BOW);
		Map<AfijoDeBotin, Double> afijos = new EnumMap<>(AfijoDeBotin.class);
		afijos.put(AfijoDeBotin.CERTERO, 0.5);
		afijos.put(AfijoDeBotin.ARDIENTE, 2.0);
		GeneradorDeBotin.armar(arco, new ObjetoConAfijos(50, Rareza.RARA, afijos), AfijoDeBotin.CERTERO);

		Arrow flecha = new Arrow(level, jugador, new ItemStack(Items.ARROW), arco);
		double base = ((FlechaAccessor) flecha).mythicloot$danioBase();
		flecha.setPos(helper.absoluteVec(Vec3.atCenterOf(DENTRO)));
		level.addFreshEntity(flecha);
		assertCerca(helper, ((FlechaAccessor) flecha).mythicloot$danioBase(), base * 1.5, "Al aparecer, el certero sube el daño base un 50 %");
		helper.assertTrue(flecha.isOnFire(), Component.literal("Y el ardiente la prende"));
		com.mythicloot.Efectos.alDisparar(flecha, level);
		assertCerca(helper, ((FlechaAccessor) flecha).mythicloot$danioBase(), base * 1.5, "Cargarla de nuevo no lo aplica dos veces");

		Arrow comun = new Arrow(level, jugador, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
		comun.setPos(helper.absoluteVec(Vec3.atCenterOf(DENTRO)));
		level.addFreshEntity(comun);
		assertCerca(helper, ((FlechaAccessor) comun).mythicloot$danioBase(), base, "Con un arco común, el de vanilla");

		flecha.discard();
		comun.discard();
		salir(helper, jugador);
		helper.succeed();
	}

	/** Vanilla deja sólo los afijos del de la izquierda: el mixin del yunque suma los del otro. */
	@GameTest
	public void juntarEnElYunque(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "herrero");
		int costo = Configuracion.POR_DEFECTO.costoPorAfijoUnido();
		double danioDeVanilla = suma(new ItemStack(Items.COPPER_SWORD).get(DataComponents.ATTRIBUTE_MODIFIERS), AfijoDeBotin.AFILADO);

		ItemStack filo = conAfijo(new ItemStack(Items.COPPER_SWORD), AfijoDeBotin.AFILADO, 1.5);
		ItemStack furia = conAfijo(new ItemStack(Items.COPPER_SWORD), AfijoDeBotin.FURIOSO, 0.05);
		AnvilMenu yunque = yunque(jugador, filo, furia);
		ItemStack resultado = yunque.getSlot(AnvilMenu.RESULT_SLOT).getItem();
		ObjetoConAfijos unido = ObjetoConAfijos.de(resultado);
		helper.assertTrue(unido != null, Component.literal("Dos espadas sin daño ni encantamientos igual dan resultado"));
		helper.assertValueEqual(unido.afijos(), Map.of(AfijoDeBotin.AFILADO, 1.5, AfijoDeBotin.FURIOSO, 0.05), "Tiene los afijos de las dos");
		helper.assertValueEqual(unido.rareza(), Rareza.RARA, "Con dos afijos pasa a rara");
		helper.assertValueEqual(yunque.getCost(), costo, "Cobra un afijo ganado");
		ItemAttributeModifiers modificadores = resultado.get(DataComponents.ATTRIBUTE_MODIFIERS);
		assertCerca(helper, suma(modificadores, AfijoDeBotin.AFILADO), danioDeVanilla + 1.5, "El filo no se suma dos veces");
		assertCerca(helper, suma(modificadores, AfijoDeBotin.FURIOSO), 0.05, "La furia está");
		helper.assertTrue(resultado.getHoverName().getString().endsWith(AfijoDeBotin.AFILADO.sufijo()), Component.literal("Se queda el nombre del de la izquierda"));
		helper.assertValueEqual(resultado.get(DataComponents.LORE).lines().getFirst().getString(), "Rareza: rara", "El lore dice la rareza nueva");

		ItemStack gastada = conAfijo(new ItemStack(Items.COPPER_SWORD), AfijoDeBotin.AFILADO, 1.5);
		gastada.setDamageValue(100);
		helper.assertValueEqual(yunque(jugador, gastada, furia).getCost(), 2 + costo, "Reparar cobra lo de vanilla más el afijo");

		helper.assertTrue(yunque(jugador, filo, conAfijo(new ItemStack(Items.COPPER_SWORD), AfijoDeBotin.AFILADO, 1.0)).getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
				Component.literal("Si no gana nada, como en vanilla: sin resultado"));
		ItemStack mejorado = yunque(jugador, filo, conAfijo(new ItemStack(Items.COPPER_SWORD), AfijoDeBotin.AFILADO, 2.0)).getSlot(AnvilMenu.RESULT_SLOT).getItem();
		helper.assertValueEqual(ObjetoConAfijos.de(mejorado).valor(AfijoDeBotin.AFILADO), 2.0, "Un afijo repetido se queda con el valor más alto");
		helper.assertValueEqual(ObjetoConAfijos.de(mejorado).rareza(), Rareza.MAGICA, "Y no sube la rareza");

		Map<AfijoDeBotin, Double> cuatro = new EnumMap<>(AfijoDeBotin.class);
		cuatro.put(AfijoDeBotin.ROBUSTO, 2.0);
		cuatro.put(AfijoDeBotin.REFORZADO, 1.0);
		cuatro.put(AfijoDeBotin.TENAZ, 1.0);
		cuatro.put(AfijoDeBotin.FIRME, 0.05);
		ItemStack legendario = GeneradorDeBotin.armar(new ItemStack(Items.IRON_HELMET), new ObjetoConAfijos(80, Rareza.LEGENDARIA, cuatro), AfijoDeBotin.ROBUSTO);
		ItemStack casco = yunque(jugador, legendario, conAfijo(new ItemStack(Items.IRON_HELMET), AfijoDeBotin.AFORTUNADO, 1.0)).getSlot(AnvilMenu.RESULT_SLOT).getItem();
		helper.assertTrue(casco.isEmpty(), Component.literal("Con cuatro afijos no entra otro, y sin nada que ganar no hay resultado"));
		ItemStack conMagico = conAfijo(new ItemStack(Items.IRON_HELMET), AfijoDeBotin.REFORZADO, 1.5);
		ItemStack mejorCasco = yunque(jugador, legendario, conMagico).getSlot(AnvilMenu.RESULT_SLOT).getItem();
		ObjetoConAfijos objetoCasco = ObjetoConAfijos.de(mejorCasco);
		helper.assertValueEqual(objetoCasco.afijos().size(), 4, "Sigue con cuatro");
		helper.assertValueEqual(objetoCasco.nivel(), 80, "El nivel más alto");
		double armaduraDeVanilla = suma(new ItemStack(Items.IRON_HELMET).get(DataComponents.ATTRIBUTE_MODIFIERS), AfijoDeBotin.REFORZADO);
		assertCerca(helper, suma(mejorCasco.get(DataComponents.ATTRIBUTE_MODIFIERS), AfijoDeBotin.REFORZADO), armaduraDeVanilla + 1.5, "La armadura del casco más la del afijo mejorado");

		helper.assertTrue(yunque(jugador, filo, conAfijo(new ItemStack(Items.COPPER_AXE), AfijoDeBotin.FURIOSO, 0.05)).getSlot(AnvilMenu.RESULT_SLOT).getItem().isEmpty(),
				Component.literal("Una espada y un hacha no se juntan, como en vanilla"));
		salir(helper, jugador);
		helper.succeed();
	}

	private static AnvilMenu yunque(ServerPlayer jugador, ItemStack izquierda, ItemStack derecha) {
		AnvilMenu yunque = new AnvilMenu(0, jugador.getInventory(), ContainerLevelAccess.NULL);
		// Cada set llama a createResult, como cuando el jugador pone los objetos
		yunque.getSlot(AnvilMenu.INPUT_SLOT).set(izquierda.copy());
		yunque.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(derecha.copy());
		return yunque;
	}

	/** Cambia el modo Bloodmoon, que es global: todo pasa adentro del método y se restaura en el finally. */
	@GameTest
	public void sorteoConYSinBloodmoon(GameTestHelper helper) {
		Configuracion seguro = conChance(1);
		Configuracion nunca = conChance(0);
		RandomSource random = RandomSource.create(11);
		Zombie conNivel = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
		conNivel.setAttached(AttachmentsDeBloodmoon.NIVEL, 50);
		Zombie sinNivel = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
		boolean antes = NivelDelMob.conBloodmoon();
		NivelDelMob.usarBloodmoon(true);
		try {
			helper.assertValueEqual(NivelDelMob.nivel(conNivel, seguro), 50, "Con Bloodmoon, el nivel del mob");
			helper.assertTrue(NivelDelMob.nivel(sinNivel, seguro) == null, Component.literal("Un mob sin nivel (spawner, huevo) no tiene"));
			helper.assertTrue(Caida.sortear(sinNivel, seguro, random) == null, Component.literal("Y no suelta nada aunque la chance sea 1"));
			helper.assertTrue(Caida.sortear(conNivel, seguro, random) != null, Component.literal("Con chance 1 y nivel, suelta"));
			helper.assertTrue(Caida.sortear(conNivel, nunca, random) == null, Component.literal("Con chance 0, no"));

			helper.assertFalse(NivelDelMob.esCampeon(conNivel), Component.literal("Sin afijo no es campeón"));
			conNivel.setAttached(AttachmentsDeBloodmoon.AFIJO, "vampiro");
			helper.assertTrue(NivelDelMob.esCampeon(conNivel), Component.literal("Con afijo es campeón"));

			conNivel.setAttached(AttachmentsDeBloodmoon.JEFE, true);
			for (int i = 0; i < 20; i++) {
				Rareza rareza = Caida.sortear(conNivel, nunca, random);
				helper.assertTrue(rareza != null && rareza.compareTo(Rareza.EPICA) >= 0, Component.literal("Un jefe suelta siempre, épica o mejor, aunque la chance sea 0"));
			}
		} finally {
			NivelDelMob.usarBloodmoon(antes);
		}
		NivelDelMob.usarBloodmoon(false);
		try {
			Integer nivel = NivelDelMob.nivel(sinNivel, seguro);
			helper.assertTrue(nivel != null && nivel >= 1, Component.literal("Sin Bloodmoon, el nivel sale de la distancia al spawn"));
			helper.assertFalse(NivelDelMob.esJefe(conNivel), Component.literal("Sin Bloodmoon no hay jefes, aunque el attachment esté"));
		} finally {
			NivelDelMob.usarBloodmoon(antes);
		}
		conNivel.discard();
		sinNivel.discard();
		helper.succeed();
	}

	/** Cambia la config global: se restaura en el finally, antes de que corra otro test. */
	@GameTest
	public void alMorirSueltaSiLoMatoUnJugador(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = entrar(helper, "verdugo");
		Configuracion antes = MythicLoot.config();
		MythicLoot.usar(conChance(1));
		try {
			Zombie muerto = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
			muerto.hurtServer(level, jugador.damageSources().playerAttack(jugador), 10_000);
			helper.assertValueEqual(contarObjetos(helper, muerto), 1, "Muerto por un jugador, suelta un objeto con afijos");

			Zombie sinJugador = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
			sinJugador.hurtServer(level, level.damageSources().genericKill(), 10_000);
			helper.assertValueEqual(contarObjetos(helper, sinJugador), 0, "Muerto sin jugador, no");

			IronGolem golem = helper.spawn(EntityTypes.IRON_GOLEM, DENTRO);
			golem.hurtServer(level, jugador.damageSources().playerAttack(jugador), 10_000);
			helper.assertValueEqual(contarObjetos(helper, golem), 0, "Sin Bloodmoon, un mob que no es hostil no suelta");
		} finally {
			MythicLoot.usar(antes);
		}
		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void archivoDeConfiguracion(GameTestHelper helper) {
		try {
			Path carpeta = Files.createTempDirectory("mythicloot");
			Path nuevo = carpeta.resolve("nuevo.json");
			helper.assertValueEqual(ArchivoDeConfiguracion.leer(nuevo), Configuracion.POR_DEFECTO, "Sin archivo, los valores por defecto");
			String escrito = Files.readString(nuevo);
			helper.assertTrue(escrito.contains("\"legendaria\"") && escrito.contains("\"vampirico\""), Component.literal("El archivo tiene las rarezas y los afijos"));

			Path parcial = carpeta.resolve("parcial.json");
			Files.writeString(parcial, "{\"chanceBase\": 0.5, \"valoresMaximos\": {\"afilado\": 9.0}, \"rarezas\": {\"epica\": {\"peso\": 0}}}");
			Configuracion leida = ArchivoDeConfiguracion.leer(parcial);
			assertCerca(helper, leida.chanceBase(), 0.5, "Se lee lo que está en el archivo");
			assertCerca(helper, leida.valorMaximo(AfijoDeBotin.AFILADO), 9, "También dentro de un mapa");
			assertCerca(helper, leida.valorMaximo(AfijoDeBotin.VAMPIRICO), Configuracion.POR_DEFECTO.valorMaximo(AfijoDeBotin.VAMPIRICO), "Lo que falta del mapa queda por defecto");
			helper.assertValueEqual(leida.rareza(Rareza.EPICA), new Configuracion.ChanceDeRareza(50, 0), "Y lo que falta dentro de una rareza");

			for (String invalido : List.of("{\"chanceMaxima\": 2}", "{\"rarezaDeJefe\": \"divina\"}", "{\"valoresMaximos\": {\"certero\": 0}}")) {
				Path archivo = carpeta.resolve("invalido.json");
				Files.writeString(archivo, invalido);
				boolean lanzo;
				try {
					ArchivoDeConfiguracion.leer(archivo);
					lanzo = false;
				} catch (RuntimeException e) {
					lanzo = true;
				}
				helper.assertTrue(lanzo, Component.literal("Se rechaza " + invalido));
				helper.assertValueEqual(Files.readString(archivo), invalido, "Y no se pisa el archivo");
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		helper.succeed();
	}

	private static ItemStack conAfijo(ItemStack stack, AfijoDeBotin afijo, double valor) {
		return GeneradorDeBotin.armar(stack, new ObjetoConAfijos(50, Rareza.MAGICA, Map.of(afijo, valor)), afijo);
	}

	/** Lo que suman los modificadores del atributo y la operación de ese afijo. */
	private static double suma(ItemAttributeModifiers modificadores, AfijoDeBotin afijo) {
		return modificadores.modifiers().stream()
				.filter(m -> m.attribute().equals(afijo.atributo()) && m.modifier().operation() == afijo.operacion())
				.mapToDouble(m -> m.modifier().amount()).sum();
	}

	private static Configuracion conChance(double chance) {
		Configuracion c = Configuracion.POR_DEFECTO;
		return new Configuracion(c.nivelMaximo(), c.bloquesPorNivel(), c.extraNether(), c.extraEnd(), chance, 0, chance, c.multiplicadorCampeon(),
				c.botinDeJefe(), c.rarezaDeJefe(), c.rarezas(), c.pesosDeTipo(), c.nivelesPorMaterial(), c.netheritaSoloLegendaria(), c.valoresMaximos(),
				c.costoPorAfijoUnido());
	}

	/** Cuenta y borra los objetos con afijos que soltó: todos los mobs del test mueren en el mismo lugar. */
	private static int contarObjetos(GameTestHelper helper, Zombie muerto) {
		return contar(helper, muerto.getBoundingBox().inflate(3));
	}

	private static int contarObjetos(GameTestHelper helper, IronGolem muerto) {
		return contar(helper, muerto.getBoundingBox().inflate(3));
	}

	private static int contar(GameTestHelper helper, AABB zona) {
		int cuantos = 0;
		for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, zona)) {
			if (ObjetoConAfijos.de(item.getItem()) != null) {
				cuantos++;
			}
			item.discard();
		}
		return cuantos;
	}

	private static void assertCerca(GameTestHelper helper, double actual, double esperado, String mensaje) {
		helper.assertTrue(Math.abs(actual - esperado) < 1e-4, Component.literal(mensaje + ": esperaba " + esperado + ", dio " + actual));
	}

	/** Un jugador en supervivencia: makeMockServerPlayerInLevel fuerza creativo. */
	private static ServerPlayer entrar(GameTestHelper helper, String nombre) {
		ServerLevel level = helper.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), nombre), false);
		ServerPlayer jugador = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		Connection conexion = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(conexion);
		level.getServer().getPlayerList().placeNewPlayer(conexion, jugador, cookie);
		jugador.setGameMode(GameType.SURVIVAL);
		return jugador;
	}

	private static void salir(GameTestHelper helper, ServerPlayer jugador) {
		helper.getLevel().getServer().getPlayerList().remove(jugador);
	}
}
