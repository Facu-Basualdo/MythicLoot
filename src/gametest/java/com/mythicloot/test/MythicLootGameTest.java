package com.mythicloot.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;

import com.mythicloot.AfijoDeBotin;
import com.mythicloot.ArchivoDeConfiguracion;
import com.mythicloot.MythicLoot;
import com.mythicloot.Caida;
import com.mythicloot.Configuracion;
import com.mythicloot.EfectosDeJefe;
import com.mythicloot.GeneradorDeBotin;
import com.mythicloot.Idioma;
import com.mythicloot.NivelDelMob;
import com.mythicloot.ObjetoConAfijos;
import com.mythicloot.Rareza;
import com.mythicloot.TipoDeObjeto;
import com.mythicloot.Yunque;
import com.mythicloot.mixin.FlechaAccessor;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MythicLootGameTest {
	/** Sólo lee los archivos de idioma: no toca el mundo. */
	@GameTest
	public void textosEnLosDosIdiomas(GameTestHelper helper) {
		Set<String> ingles = Idioma.claves(Idioma.INGLES);
		Set<String> espanol = Idioma.claves(Idioma.ESPANOL);
		Set<String> soloIngles = new HashSet<>(ingles);
		soloIngles.removeAll(espanol);
		Set<String> soloEspanol = new HashSet<>(espanol);
		soloEspanol.removeAll(ingles);
		helper.assertTrue(ingles.size() > 20, Component.literal("Se leyó el archivo en inglés (" + ingles.size() + " claves)"));
		helper.assertTrue(soloIngles.isEmpty() && soloEspanol.isEmpty(),
				Component.literal("Los dos idiomas tienen las mismas claves (sólo en inglés: " + soloIngles + ", sólo en español: " + soloEspanol + ")"));
		for (AfijoDeBotin afijo : AfijoDeBotin.values()) {
			helper.assertTrue(ingles.contains("mythicloot.afijo." + afijo.id() + ".sufijo"), Component.literal("Falta el sufijo de " + afijo.id()));
		}
		for (Rareza rareza : Rareza.values()) {
			helper.assertTrue(ingles.contains("mythicloot.rareza." + rareza.getSerializedName()) && ingles.contains(rareza.claveDelAviso()),
					Component.literal("Falta el nombre o el aviso de " + rareza.getSerializedName()));
		}
		helper.assertTrue(Idioma.resolver("es_MX").equals(Idioma.ESPANOL) && Idioma.resolver("en_gb").equals(Idioma.INGLES), Component.literal("Variantes de idioma"));
		// %% es un porcentaje, igual que en las plantillas que arma el cliente
		String robo = Idioma.tr(Idioma.ESPANOL, "mythicloot.afijo.vampirico.descripcion", "5").getString();
		helper.assertValueEqual(robo, "Roba el 5 % del daño que hacés", "Reemplaza %s y %%");
		helper.assertTrue(Idioma.numero(Idioma.ESPANOL, 2.5).equals("2,5") && Idioma.numero(Idioma.INGLES, 2.5).equals("2.5"), Component.literal("Coma decimal en español"));
		helper.succeed();
	}

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
				if (tipo == TipoDeObjeto.CUERPO_A_CUERPO || tipo == TipoDeObjeto.TRIDENTE) {
					boolean danioDeVanilla = modificadores.modifiers().stream().anyMatch(m -> m.attribute().equals(Attributes.ATTACK_DAMAGE)
							&& !m.modifier().id().getNamespace().equals(MythicLoot.MOD_ID));
					helper.assertTrue(danioDeVanilla, Component.literal(que + ": el arma conserva su daño de vanilla"));
				}
				String nombre = stack.getHoverName().getString();
				helper.assertTrue(objeto.afijos().keySet().stream().anyMatch(a -> nombre.endsWith(a.sufijo().getString())), Component.literal(que + ": el nombre lleva un sufijo: " + nombre));
				Component primeraLinea = stack.get(DataComponents.LORE).lines().getFirst();
				helper.assertValueEqual(primeraLinea.getString(), Idioma.guardado("mythicloot.lore.rareza", rareza.nombre()).getString(), que + ": primera línea del lore");
				// Guardado como traducible: un cliente con el mod lo ve en su idioma, uno vanilla ve el respaldo
				helper.assertTrue(primeraLinea.getContents() instanceof TranslatableContents traducible && traducible.getKey().equals("mythicloot.lore.rareza")
						&& traducible.getFallback() != null, Component.literal(que + ": el lore es traducible con respaldo"));
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
		helper.assertTrue(bajo.getItem().toString().contains("copper"), Component.literal("En el nivel 5, de cobre: " + bajo.getItem()));
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

	@GameTest
	public void lanzasMazasTridentesEscudosYHerramientas(GameTestHelper helper) {
		Configuracion config = Configuracion.POR_DEFECTO;
		RandomSource random = RandomSource.create(5);
		boolean lanza = false;
		boolean maza = false;
		for (int i = 0; i < 200; i++) {
			ItemStack magica = GeneradorDeBotin.generar(config, 60, Rareza.MAGICA, TipoDeObjeto.CUERPO_A_CUERPO, random);
			helper.assertFalse(magica.is(Items.MACE), Component.literal("Una mágica nunca es maza"));
			lanza |= magica.is(Items.DIAMOND_SPEAR);
			maza |= GeneradorDeBotin.generar(config, 60, Rareza.EPICA, TipoDeObjeto.CUERPO_A_CUERPO, random).is(Items.MACE);
		}
		helper.assertTrue(lanza, Component.literal("En el nivel 60 salen lanzas de diamante"));
		helper.assertTrue(maza, Component.literal("Desde épica, también mazas"));

		ItemStack herramienta = GeneradorDeBotin.generar(config, 5, Rareza.MAGICA, TipoDeObjeto.HERRAMIENTA, random);
		helper.assertTrue(herramienta.is(Items.COPPER_PICKAXE) || herramienta.is(Items.COPPER_SHOVEL), Component.literal("En el nivel 5, pico o pala de cobre: " + herramienta.getItem()));
		helper.assertTrue(GeneradorDeBotin.generar(config, 5, Rareza.MAGICA, TipoDeObjeto.TRIDENTE, random).is(Items.TRIDENT), Component.literal("Tridente"));

		ItemStack escudo = GeneradorDeBotin.generar(config, 60, Rareza.LEGENDARIA, TipoDeObjeto.ESCUDO, random);
		helper.assertTrue(escudo.is(Items.SHIELD), Component.literal("Escudo"));
		helper.assertTrue(escudo.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().allMatch(m -> m.slot() == EquipmentSlotGroup.OFFHAND),
				Component.literal("Los afijos del escudo valen en la mano secundaria"));
		helper.succeed();
	}

	@GameTest
	public void elEscudoValeEnLaManoSecundaria(GameTestHelper helper) {
		Zombie conEscudo = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
		Zombie enLaPrincipal = helper.spawn(EntityTypes.ZOMBIE, DENTRO.east(2));
		double vida = conEscudo.getAttributeValue(Attributes.MAX_HEALTH);
		conEscudo.setItemSlot(EquipmentSlot.OFFHAND, conAfijo(new ItemStack(Items.SHIELD), AfijoDeBotin.ROBUSTO, 4));
		enLaPrincipal.setItemSlot(EquipmentSlot.MAINHAND, conAfijo(new ItemStack(Items.SHIELD), AfijoDeBotin.ROBUSTO, 4));
		helper.runAfterDelay(2, () -> {
			assertCerca(helper, conEscudo.getAttributeValue(Attributes.MAX_HEALTH), vida + 4, "Con el escudo en la mano secundaria");
			assertCerca(helper, enLaPrincipal.getAttributeValue(Attributes.MAX_HEALTH), vida, "En la principal, no");
			conEscudo.discard();
			enLaPrincipal.discard();
			helper.succeed();
		});
	}

	/** El tridente lanzado pega un 8 fijo: el certero entra por el mixin, y el fuego no lo pasa vanilla. */
	@GameTest
	public void tridenteLanzado(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = entrar(helper, "pescador");
		ItemStack tridente = new ItemStack(Items.TRIDENT);
		Map<AfijoDeBotin, Double> afijos = new EnumMap<>(AfijoDeBotin.class);
		afijos.put(AfijoDeBotin.CERTERO, 0.5);
		afijos.put(AfijoDeBotin.VAMPIRICO, 0.1);
		afijos.put(AfijoDeBotin.ARDIENTE, 2.0);
		GeneradorDeBotin.armar(tridente, new ObjetoConAfijos(50, Rareza.RARA, afijos), AfijoDeBotin.CERTERO);
		jugador.setHealth(10);

		IronGolem blanco = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, DENTRO);
		// Que el fuego no le saque vida: así se mide sólo el golpe
		blanco.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
		float vida = blanco.getHealth();
		ThrownTrident lanzado = new ThrownTrident(level, jugador, tridente);
		lanzado.setPos(blanco.position().add(0, 1, -1.5));
		lanzado.setDeltaMovement(0, 0, 1);
		level.addFreshEntity(lanzado);
		helper.succeedWhen(() -> {
			helper.assertTrue(blanco.getHealth() < vida, Component.literal("El tridente le pegó"));
			assertCerca(helper, vida - blanco.getHealth(), 12, "8 de vanilla y un 50 % más");
			assertCerca(helper, jugador.getHealth(), 11.2, "El vampírico cura el 10 % del golpe");
			helper.assertTrue(blanco.getRemainingFireTicks() > 0, Component.literal("El ardiente lo prende fuego"));
			lanzado.discard();
			blanco.discard();
			salir(helper, jugador);
		});
	}

	@GameTest
	public void elAfijoDeJefeVaAparte(GameTestHelper helper) {
		ItemStack stack = GeneradorDeBotin.generar(Configuracion.POR_DEFECTO, 60, Rareza.EPICA, TipoDeObjeto.ARMADURA, AfijoDeBotin.VACIO, RandomSource.create(9));
		ObjetoConAfijos objeto = ObjetoConAfijos.de(stack);
		helper.assertValueEqual(objeto.afijos().size(), 4, "Los 3 de la épica más el de jefe");
		helper.assertTrue(objeto.valor(AfijoDeBotin.VACIO) > 0, Component.literal("El de jefe vale algo"));
		helper.assertTrue(stack.getHoverName().getString().endsWith(AfijoDeBotin.VACIO.sufijo().getString()), Component.literal("Y da el nombre: " + stack.getHoverName().getString()));
		helper.assertTrue(stack.get(DataComponents.LORE).lines().stream().anyMatch(l -> TextColor.fromLegacyFormat(ChatFormatting.RED).equals(l.getStyle().getColor())),
				Component.literal("Su línea del lore va en rojo"));

		Map<AfijoDeBotin, Double> legendaria = new EnumMap<>(AfijoDeBotin.class);
		legendaria.put(AfijoDeBotin.AFILADO, 1.0);
		legendaria.put(AfijoDeBotin.FURIOSO, 0.1);
		legendaria.put(AfijoDeBotin.LARGO, 0.5);
		legendaria.put(AfijoDeBotin.VAMPIRICO, 0.05);
		ObjetoConAfijos unido = Yunque.juntar(new ObjetoConAfijos(50, Rareza.LEGENDARIA, legendaria), new ObjetoConAfijos(50, Rareza.MAGICA, Map.of(AfijoDeBotin.VERDUGO, 0.2)));
		helper.assertValueEqual(unido.afijos().size(), 5, "Una legendaria de 4 gana el de jefe: no cuenta para el tope");
		helper.assertValueEqual(unido.rareza(), Rareza.LEGENDARIA, "Sigue legendaria");

		ObjetoConAfijos dos = Yunque.juntar(new ObjetoConAfijos(50, Rareza.MAGICA, Map.of(AfijoDeBotin.AFILADO, 1.0, AfijoDeBotin.VERDUGO, 0.2)),
				new ObjetoConAfijos(50, Rareza.MAGICA, Map.of(AfijoDeBotin.FURIOSO, 0.1, AfijoDeBotin.VACIO, 0.1)));
		helper.assertValueEqual(dos.afijos().keySet(), Set.of(AfijoDeBotin.AFILADO, AfijoDeBotin.FURIOSO, AfijoDeBotin.VERDUGO), "Con dos de jefe distintos, queda el de la izquierda");
		helper.assertValueEqual(dos.rareza(), Rareza.RARA, "Y la rareza cuenta sólo los comunes");
		helper.succeed();
	}

	/** Cambia el modo Bloodmoon, que es global: todo pasa adentro del método y se restaura en el finally. */
	@GameTest
	public void sorteoDelAfijoDeJefe(GameTestHelper helper) {
		RandomSource random = RandomSource.create(13);
		Zombie zombi = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
		zombi.setAttached(AttachmentsDeBloodmoon.NIVEL, 50);
		Witch bruja = helper.spawn(EntityTypes.WITCH, DENTRO);
		bruja.setAttached(AttachmentsDeBloodmoon.NIVEL, 50);
		bruja.setAttached(AttachmentsDeBloodmoon.JEFE, true);
		boolean antes = NivelDelMob.conBloodmoon();
		NivelDelMob.usarBloodmoon(true);
		try {
			helper.assertTrue(Caida.afijoDeJefe(zombi, conAfijosDeJefe(1), random) == null, Component.literal("Un mob que no es jefe no trae"));
			zombi.setAttached(AttachmentsDeBloodmoon.JEFE, true);
			helper.assertValueEqual(Caida.afijoDeJefe(zombi, conAfijosDeJefe(1), random), AfijoDeBotin.FESTIN, "El jefe zombi trae el festín");
			helper.assertValueEqual(Caida.afijoDeJefe(bruja, conAfijosDeJefe(1), random), AfijoDeBotin.BREBAJE, "La jefa bruja, el brebaje");
			helper.assertTrue(Caida.afijoDeJefe(zombi, conAfijosDeJefe(0), random) == null, Component.literal("Con chance 0, ninguno"));
		} finally {
			NivelDelMob.usarBloodmoon(antes);
		}
		zombi.discard();
		bruja.discard();
		helper.succeed();
	}

	@GameTest
	public void festinYMarchitoAlPegarYAlMatar(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = entrar(helper, "glotona");
		jugador.setItemSlot(EquipmentSlot.HEAD, conAfijos(new ItemStack(Items.IRON_HELMET), Rareza.MAGICA, Map.of(AfijoDeBotin.FESTIN, 0.1)));
		jugador.setItemSlot(EquipmentSlot.MAINHAND, conAfijos(new ItemStack(Items.IRON_SWORD), Rareza.MAGICA, Map.of(AfijoDeBotin.MARCHITO, 3.0)));
		jugador.setHealth(10);

		Zombie presa = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
		presa.hurtServer(level, jugador.damageSources().playerAttack(jugador), 10_000);
		assertCerca(helper, jugador.getHealth(), 12, "Matar un zombi con un casco del Festín de 0,1 cura 2 (de 20)");
		contarObjetos(helper, presa);

		IronGolem blanco = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, DENTRO.east(2));
		blanco.hurtServer(level, jugador.damageSources().playerAttack(jugador), 5);
		helper.assertTrue(blanco.hasEffect(MobEffects.WITHER), Component.literal("La espada del Marchitamiento marchita"));
		helper.assertValueEqual(blanco.getEffect(MobEffects.WITHER).getAmplifier(), 1, "Wither II");

		// En la mochila no vale: tiene que estar equipado en su lugar
		jugador.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
		jugador.getInventory().add(conAfijos(new ItemStack(Items.IRON_HELMET), Rareza.MAGICA, Map.of(AfijoDeBotin.FESTIN, 0.1)));
		helper.assertValueEqual(EfectosDeJefe.valor(jugador, AfijoDeBotin.FESTIN), 0.0, "Un casco en el inventario no cuenta");
		jugador.setItemSlot(EquipmentSlot.MAINHAND, conAfijos(new ItemStack(Items.IRON_HELMET), Rareza.MAGICA, Map.of(AfijoDeBotin.FESTIN, 0.1)));
		helper.assertValueEqual(EfectosDeJefe.valor(jugador, AfijoDeBotin.FESTIN), 0.0, "Ni un casco en la mano");

		blanco.discard();
		salir(helper, jugador);
		helper.succeed();
	}

	/** Pasa por el mixin de getDamageAfterArmorAbsorb: un zombi con pechera de diamante, con y sin verdugo. */
	@GameTest
	public void verdugoIgnoraLaArmadura(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = entrar(helper, "hacha");
		Zombie comun = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
		Zombie verdugo = helper.spawn(EntityTypes.ZOMBIE, DENTRO.east(2));
		comun.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
		verdugo.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
		helper.runAfterDelay(2, () -> {
			jugador.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			comun.hurtServer(level, jugador.damageSources().playerAttack(jugador), 10);
			jugador.setItemSlot(EquipmentSlot.MAINHAND, conAfijos(new ItemStack(Items.IRON_SWORD), Rareza.MAGICA, Map.of(AfijoDeBotin.VERDUGO, 1.0)));
			verdugo.hurtServer(level, jugador.damageSources().playerAttack(jugador), 10);
			helper.assertTrue(comun.getHealth() > 10, Component.literal("Sin verdugo, la armadura baja el golpe: quedó con " + comun.getHealth()));
			assertCerca(helper, 20 - verdugo.getHealth(), 10, "Con verdugo 1, el golpe entra entero");
			comun.discard();
			verdugo.discard();
			salir(helper, jugador);
			helper.succeed();
		});
	}

	/** Los jugadores de prueba no reciben daño (su cliente nunca "cargó"): se llama a cada efecto directo. */
	@GameTest
	public void brutoLlamaradaBrebajeYVacio(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = entrar(helper, "tanque");
		Zombie atacante = helper.spawn(EntityTypes.ZOMBIE, DENTRO);
		jugador.setItemSlot(EquipmentSlot.HEAD, conAfijos(new ItemStack(Items.IRON_HELMET), Rareza.MAGICA, Map.of(AfijoDeBotin.BRUTO, 0.5)));
		jugador.setItemSlot(EquipmentSlot.CHEST, conAfijos(new ItemStack(Items.IRON_CHESTPLATE), Rareza.MAGICA, Map.of(AfijoDeBotin.LLAMARADA, 0.5, AfijoDeBotin.BREBAJE, 1.0)));
		jugador.setItemSlot(EquipmentSlot.OFFHAND, conAfijos(new ItemStack(Items.SHIELD), Rareza.MAGICA, Map.of(AfijoDeBotin.VACIO, 1.0)));

		assertCerca(helper, EfectosDeJefe.danioRecibido(jugador, jugador.damageSources().mobAttack(atacante), 10), 5, "Bruto 0,5: la mitad de un golpe cuerpo a cuerpo");
		assertCerca(helper, EfectosDeJefe.danioRecibido(jugador, jugador.damageSources().inFire(), 10), 5, "Llamarada 0,5: la mitad del fuego");
		assertCerca(helper, EfectosDeJefe.danioRecibido(jugador, jugador.damageSources().fall(), 10), 10, "Una caída, entera");

		jugador.setHealth(10);
		EfectosDeJefe.alPegar(jugador, jugador.damageSources().mobAttack(atacante), 2, 2, false);
		assertCerca(helper, jugador.getHealth(), 14, "Brebaje 1: toma curación instantánea (2 corazones)");
		helper.assertTrue(atacante.getRemainingFireTicks() > 0, Component.literal("La llamarada prende al que le pegó"));
		EfectosDeJefe.alPegar(jugador, jugador.damageSources().mobAttack(atacante), 2, 2, false);
		assertCerca(helper, jugador.getHealth(), 14, "Hasta que pase la espera, no toma otro");

		// Los jugadores de prueba entran en (0, 0, 0): para saltar necesita suelo cerca
		Vec3 suelo = helper.absoluteVec(Vec3.atBottomCenterOf(DENTRO));
		jugador.teleportTo(suelo.x, suelo.y, suelo.z);
		Vec3 antes = jugador.position();
		helper.assertTrue(EfectosDeJefe.esquivar(jugador, jugador.damageSources().fall(), 5), Component.literal("Una caída no se esquiva"));
		helper.assertFalse(EfectosDeJefe.esquivar(jugador, jugador.damageSources().mobAttack(atacante), 5), Component.literal("Vacío 1 esquiva el golpe de un mob"));
		helper.assertTrue(jugador.position().distanceTo(antes) > 0, Component.literal("Saltando a otro lugar"));
		helper.assertTrue(EfectosDeJefe.esquivar(jugador, jugador.damageSources().mobAttack(atacante), 5), Component.literal("Y hasta que pase la espera, no esquiva otro"));

		atacante.discard();
		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void lluviaDeFlechas(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer jugador = entrar(helper, "lluvia");
		ItemStack arco = conAfijos(new ItemStack(Items.BOW), Rareza.MAGICA, Map.of(AfijoDeBotin.LLUVIA, 1.0));
		jugador.setItemSlot(EquipmentSlot.MAINHAND, arco);
		Arrow flecha = new Arrow(level, jugador, new ItemStack(Items.ARROW), arco);
		flecha.setPos(helper.absoluteVec(Vec3.atCenterOf(DENTRO)));
		flecha.setDeltaMovement(0, 0, 1);
		level.addFreshEntity(flecha);
		List<Arrow> flechas = level.getEntitiesOfClass(Arrow.class, flecha.getBoundingBox().inflate(2));
		helper.assertValueEqual(flechas.size(), 2, "Lluvia 1: sale una flecha más");
		helper.assertTrue(flechas.stream().anyMatch(f -> f != flecha && f.pickup == AbstractArrow.Pickup.CREATIVE_ONLY), Component.literal("Y la copia no se puede juntar"));
		flechas.forEach(Arrow::discard);
		salir(helper, jugador);
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
		helper.assertTrue(resultado.getHoverName().getString().endsWith(AfijoDeBotin.AFILADO.sufijo().getString()), Component.literal("Se queda el nombre del de la izquierda"));
		helper.assertValueEqual(resultado.get(DataComponents.LORE).lines().getFirst().getString(),
				Idioma.guardado("mythicloot.lore.rareza", Rareza.RARA.nombre()).getString(), "El lore dice la rareza nueva");

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

			Path jefes = carpeta.resolve("jefes.json");
			Files.writeString(jefes, "{\"afijosDeJefe\": {\"porMob\": {\"minecraft:husk\": \"festin\", \"minecraft:zombie\": \"\"}}}");
			Configuracion conJefes = ArchivoDeConfiguracion.leer(jefes);
			helper.assertValueEqual(conJefes.afijoDeJefe("minecraft:husk"), AfijoDeBotin.FESTIN, "Se pueden sumar especies de jefe");
			helper.assertTrue(conJefes.afijoDeJefe("minecraft:zombie") == null, Component.literal("\"\" saca el afijo de una especie"));
			helper.assertValueEqual(conJefes.afijoDeJefe("minecraft:witch"), AfijoDeBotin.BREBAJE, "Las otras quedan por defecto");

			for (String invalido : List.of("{\"afijosDeJefe\": {\"porMob\": {\"minecraft:zombie\": \"afilado\"}}}", "{\"afijosDeJefe\": {\"chance\": 2}}", "{\"chanceMaxima\": 2}", "{\"rarezaDeJefe\": \"divina\"}", "{\"rarezaMinimaDeMaza\": \"divina\"}", "{\"valoresMaximos\": {\"certero\": 0}}")) {
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

	private static Configuracion conAfijosDeJefe(double chance) {
		Configuracion c = Configuracion.POR_DEFECTO;
		Configuracion.AfijosDeJefe a = c.afijosDeJefe();
		return new Configuracion(c.nivelMaximo(), c.bloquesPorNivel(), c.extraNether(), c.extraEnd(), c.chanceBase(), c.chancePorNivel(), c.chanceMaxima(),
				c.multiplicadorCampeon(), c.botinDeJefe(), c.rarezaDeJefe(),
				new Configuracion.AfijosDeJefe(chance, a.porMob(), a.nivelDeMarchito(), a.segundosDeBrebaje(), a.segundosEntreBrebajes(), a.segundosDeLlamarada(),
						a.segundosEntreTeletransportes(), a.distanciaDeTeletransporte()),
				c.rarezas(), c.pesosDeTipo(), c.nivelesPorMaterial(), c.netheritaSoloLegendaria(), c.rarezaMinimaDeMaza(), c.valoresMaximos(),
				c.costoPorAfijoUnido(), c.idioma());
	}

	private static ItemStack conAfijos(ItemStack stack, Rareza rareza, Map<AfijoDeBotin, Double> afijos) {
		return GeneradorDeBotin.armar(stack, new ObjetoConAfijos(50, rareza, afijos), null);
	}

	private static Configuracion conChance(double chance) {
		Configuracion c = Configuracion.POR_DEFECTO;
		return new Configuracion(c.nivelMaximo(), c.bloquesPorNivel(), c.extraNether(), c.extraEnd(), chance, 0, chance, c.multiplicadorCampeon(),
				c.botinDeJefe(), c.rarezaDeJefe(), c.afijosDeJefe(), c.rarezas(), c.pesosDeTipo(), c.nivelesPorMaterial(), c.netheritaSoloLegendaria(), c.rarezaMinimaDeMaza(), c.valoresMaximos(),
				c.costoPorAfijoUnido(), c.idioma());
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
