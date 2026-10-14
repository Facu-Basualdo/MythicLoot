package com.mythicloot.cliente;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mythicloot.AfijoDeBotin;
import com.mythicloot.ArchivoDeConfiguracion;
import com.mythicloot.Idioma;
import com.mythicloot.MythicLoot;
import com.mythicloot.Rareza;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

/**
 * {@code config/mythicloot.json} desde Mod Menu. Con un mundo abierto en esta instancia se aplica como
 * {@code /mythicloot recargar}: lo que ya se soltó no cambia, lo que se suelte desde ahora sí.
 */
public final class PantallaDeConfiguracionDeMythicLoot extends PantallaDeConfiguracion {
	public PantallaDeConfiguracionDeMythicLoot(@Nullable Screen anterior) {
		super(Idioma.tr(idioma(), "mythicloot.config.pantalla.titulo"), anterior, ArchivoDeConfiguracion.ruta());
	}

	private static String idioma() {
		return Idioma.resolver(Minecraft.getInstance().getLanguageManager().getSelected());
	}

	@Override
	protected @Nullable MutableComponent traducir(String clave, Object... args) {
		String idioma = idioma();
		return Idioma.claves(idioma).contains(clave) ? Idioma.tr(idioma, clave, args) : null;
	}

	@Override
	protected String prefijo() {
		return "mythicloot.config";
	}

	@Override
	protected List<Component> recargar(@Nullable MinecraftServer servidor) {
		String error = MythicLoot.cargarConfiguracion();
		return error == null ? List.of() : List.of(Component.literal(error));
	}

	@Override
	protected List<String> opciones(String ruta) {
		if (ruta.equals("idioma")) {
			return Idioma.DISPONIBLES;
		}
		if (ruta.equals("rarezaDeJefe") || ruta.equals("rarezaMinimaDeMaza")) {
			return Arrays.stream(Rareza.values()).map(Rareza::getSerializedName).toList();
		}
		if (ruta.startsWith("afijosDeJefe.porMob.")) {
			// "" es ninguno: esa especie de jefe suelta su botín sin afijo propio
			List<String> afijos = new ArrayList<>(List.of(""));
			Arrays.stream(AfijoDeBotin.values()).filter(AfijoDeBotin::deJefe).map(AfijoDeBotin::id).forEach(afijos::add);
			return afijos;
		}
		return List.of();
	}

	/** Las rarezas y los afijos con el mismo nombre que en los botones de opciones, no con su id. */
	@Override
	protected Component nombre(String ruta, String clave) {
		if (ruta.equals("rarezas." + clave) || ruta.equals("valoresMaximos." + clave)) {
			Component traducido = traducir(prefijo() + ".valor." + clave);
			if (traducido != null) {
				return traducido;
			}
		}
		return super.nombre(ruta, clave);
	}
}
