package com.botinmitico;

import java.util.Arrays;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jspecify.annotations.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/** {@code /botinmitico generar <nivel> [rareza] [tipo]} y {@code /botinmitico recargar}, los dos para ops. */
public final class ComandoBotin {
	private ComandoBotin() {
	}

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(BotinMitico.MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("generar")
						.then(Commands.argument("nivel", IntegerArgumentType.integer(1))
								.executes(ctx -> generar(ctx, null, null))
								.then(Commands.argument("rareza", StringArgumentType.word())
										.suggests((ctx, sugerencias) -> SharedSuggestionProvider.suggest(ids(Rareza.values()), sugerencias))
										.executes(ctx -> generar(ctx, StringArgumentType.getString(ctx, "rareza"), null))
										.then(Commands.argument("tipo", StringArgumentType.word())
												.suggests((ctx, sugerencias) -> SharedSuggestionProvider.suggest(ids(TipoDeObjeto.values()), sugerencias))
												.executes(ctx -> generar(ctx, StringArgumentType.getString(ctx, "rareza"), StringArgumentType.getString(ctx, "tipo")))))))
				.then(Commands.literal("recargar")
						.executes(ComandoBotin::recargar)));
	}

	private static int generar(CommandContext<CommandSourceStack> ctx, @Nullable String idRareza, @Nullable String idTipo) throws CommandSyntaxException {
		CommandSourceStack fuente = ctx.getSource();
		ServerPlayer jugador = fuente.getPlayerOrException();
		Configuracion config = BotinMitico.config();
		RandomSource random = jugador.getRandom();
		int nivel = IntegerArgumentType.getInteger(ctx, "nivel");

		Rareza rareza = idRareza == null ? GeneradorDeBotin.elegirRareza(config, nivel, random) : Rareza.porId(idRareza);
		if (rareza == null) {
			fuente.sendFailure(Component.literal("No existe la rareza " + idRareza + ". Opciones: " + String.join(", ", ids(Rareza.values()))));
			return 0;
		}
		TipoDeObjeto tipo = idTipo == null ? GeneradorDeBotin.elegirTipo(config, random) : TipoDeObjeto.porId(idTipo);
		if (tipo == null) {
			fuente.sendFailure(Component.literal("No existe el tipo " + idTipo + ". Opciones: " + String.join(", ", ids(TipoDeObjeto.values()))));
			return 0;
		}

		ItemStack objeto = GeneradorDeBotin.generar(config, nivel, rareza, tipo, random);
		Component nombre = objeto.getHoverName();
		if (!jugador.getInventory().add(objeto)) {
			jugador.spawnAtLocation(jugador.level(), objeto);
		}
		fuente.sendSuccess(() -> Component.literal("Generado: ").append(nombre), false);
		return 1;
	}

	private static int recargar(CommandContext<CommandSourceStack> ctx) {
		String error = BotinMitico.cargarConfiguracion();
		if (error != null) {
			ctx.getSource().sendFailure(Component.literal("No se pudo recargar config/botinmitico.json, sigue la anterior: " + error));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Config recargada"), true);
		return 1;
	}

	private static String[] ids(StringRepresentable[] valores) {
		return Arrays.stream(valores).map(StringRepresentable::getSerializedName).toArray(String[]::new);
	}
}
