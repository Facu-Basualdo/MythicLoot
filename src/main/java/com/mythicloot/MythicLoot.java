package com.mythicloot;

import java.io.IOException;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;

public class MythicLoot implements ModInitializer {
	public static final String MOD_ID = "mythicloot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Una flecha a la que ya se le aplicaron los afijos del arco. Persistente: el daño subido ya viaja en su NBT. */
	public static final AttachmentType<Boolean> FLECHA_POTENCIADA = AttachmentRegistry.createPersistent(id("flecha_potenciada"), Codec.BOOL);

	private static volatile Configuracion config = Configuracion.POR_DEFECTO;

	@Override
	public void onInitialize() {
		cargarConfiguracion();
		if (FabricLoader.getInstance().isModLoaded(NivelDelMob.BLOODMOON)) {
			NivelDelMob.usarBloodmoon(true);
			LOGGER.info("Bloodmoon & Bosses está cargado: el botín usa el nivel de cada mob");
		}

		ServerLivingEntityEvents.AFTER_DEATH.register(Caida::alMorir);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Efectos::alPegar);
		ServerEntityEvents.ENTITY_LOAD.register(Efectos::alDisparar);
		ServerLivingEntityEvents.AFTER_DEATH.register(EfectosDeJefe::alMorir);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(EfectosDeJefe::alPegar);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(EfectosDeJefe::esquivar);
		CommandRegistrationCallback.EVENT.register((dispatcher, contexto, seleccion) -> ComandoBotin.registrar(dispatcher));
	}

	public static Configuracion config() {
		return config;
	}

	/** Para los tests: cambia la config sin pasar por el archivo. */
	public static void usar(Configuracion nueva) {
		nueva.validar();
		config = nueva;
	}

	/** Si el archivo tiene un error se queda la configuración que había (al arrancar, la por defecto) y se devuelve el motivo. */
	public static @Nullable String cargarConfiguracion() {
		try {
			config = ArchivoDeConfiguracion.leer(ArchivoDeConfiguracion.ruta());
			return null;
		} catch (IOException | RuntimeException e) {
			LOGGER.error("No se pudo leer {}: {}", ArchivoDeConfiguracion.ruta(), e.getMessage());
			return e.getMessage();
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
