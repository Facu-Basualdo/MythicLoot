package com.mythicloot.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import com.mythicloot.ArchivoDeConfiguracion;
import com.mythicloot.MythicLoot;
import com.mythicloot.cliente.PantallaDeConfiguracionDeMythicLoot;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;

/**
 * La pantalla de configuración de Mod Menu, abierta directamente (Mod Menu no está en el entorno de prueba): un
 * número y una opción (la rareza de los jefes) se guardan y llegan a la config del servidor integrado, y un valor que
 * {@code validar()} rechaza deja el archivo como estaba. Deja capturas en {@code build/run/clientGameTest/screenshots}.
 */
public class ConfiguracionClienteTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// Los campos se buscan por su nombre en español: el cliente de prueba arranca en inglés
		context.runOnClient(cliente -> cliente.getLanguageManager().setSelected("es_ar"));
		try (TestSingleplayerContext mundo = context.worldBuilder().create()) {
			mundo.getConnection().waitForChunksRender();
			context.getInput().setCursorPos(0, 0);

			context.setScreen(() -> new PantallaDeConfiguracionDeMythicLoot(null));
			context.waitForScreen(PantallaDeConfiguracionDeMythicLoot.class);
			context.waitTicks(2);
			context.takeScreenshot("configuracion");
			// Más abajo: los afijos de cada especie de jefe, con el nombre del mob de vanilla y un botón de opciones
			context.getInput().setCursorPos(427, 240);
			context.getInput().scroll(-30);
			context.getInput().setCursorPos(0, 0);
			context.waitTicks(2);
			context.takeScreenshot("configuracion-afijos-de-jefe");
			context.runOnClient(cliente -> campo(cliente, "Probabilidad base").setValue("0,03"));
			// La única opción que arranca en épica es la rareza de los jefes (la de la maza está más abajo): pasa a legendaria
			context.runOnClient(cliente -> widgets(cliente, CycleButton.class).stream()
					.filter(boton -> "epica".equals(boton.getValue()))
					.findFirst()
					.orElseThrow()
					.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0)));
			context.clickScreenButton("Guardar");
			context.waitFor(cliente -> cliente.gui.screen() == null);
			double chance = mundo.getServer().computeOnServer(servidor -> MythicLoot.config().chanceBase());
			String rareza = mundo.getServer().computeOnServer(servidor -> MythicLoot.config().rarezaDeJefe());
			if (chance != 0.03 || !rareza.equals("legendaria")) {
				throw new AssertionError("Guardar tenía que dejar chanceBase en 0.03 y rarezaDeJefe en legendaria, quedaron " + chance + " y " + rareza);
			}

			// validar() exige que el máximo vaya de 0 a 1: no se guarda, y el archivo y la config siguen como antes
			context.setScreen(() -> new PantallaDeConfiguracionDeMythicLoot(null));
			context.waitForScreen(PantallaDeConfiguracionDeMythicLoot.class);
			context.runOnClient(cliente -> campo(cliente, "Probabilidad máxima").setValue("5"));
			context.clickScreenButton("Guardar");
			context.waitTicks(10);
			if (!context.computeOnClient(cliente -> cliente.gui.screen() instanceof PantallaDeConfiguracionDeMythicLoot)) {
				throw new AssertionError("Con un valor rechazado la pantalla tenía que quedar abierta, con el aviso");
			}
			context.takeScreenshot("configuracion-rechazada");
			String archivo = leer();
			if (!archivo.contains("\"chanceMaxima\": 0.1") || !archivo.contains("\"chanceBase\": 0.03")) {
				throw new AssertionError("El archivo tenía que quedar como estaba antes del valor rechazado:\n" + archivo);
			}
			context.setScreen(() -> null);
		}
	}

	private static String leer() {
		try {
			return Files.readString(ArchivoDeConfiguracion.ruta(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static EditBox campo(Minecraft cliente, String nombre) {
		return widgets(cliente, EditBox.class).stream()
				.filter(caja -> caja.getMessage().getString().equals(nombre))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No está el campo " + nombre));
	}

	/** Los de la lista están adentro del contenedor con scroll, no en la pantalla directamente. */
	private static <T> List<T> widgets(Minecraft cliente, Class<T> tipo) {
		List<T> encontrados = new ArrayList<>();
		for (GuiEventListener hijo : cliente.gui.screen().children()) {
			recorrer(hijo, tipo, encontrados);
		}
		return encontrados;
	}

	private static <T> void recorrer(GuiEventListener elemento, Class<T> tipo, List<T> encontrados) {
		if (elemento instanceof AbstractWidget && tipo.isInstance(elemento)) {
			encontrados.add(tipo.cast(elemento));
		}
		if (elemento instanceof ContainerEventHandler contenedor) {
			for (GuiEventListener hijo : contenedor.children()) {
				recorrer(hijo, tipo, encontrados);
			}
		}
	}
}
