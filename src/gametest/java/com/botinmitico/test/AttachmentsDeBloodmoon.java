package com.botinmitico.test;

import com.mojang.serialization.Codec;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.resources.Identifier;

/**
 * Los attachments de Bloodmoon & Bosses que lee {@code NivelDelMob}, con los mismos ids y codecs. Bloodmoon no está en
 * los tests: así se prueba la lectura sin depender de su jar.
 */
public class AttachmentsDeBloodmoon implements ModInitializer {
	public static final AttachmentType<Integer> NIVEL = AttachmentRegistry.createPersistent(id("nivel"), Codec.INT);
	public static final AttachmentType<Boolean> JEFE = AttachmentRegistry.createPersistent(id("jefe"), Codec.BOOL);
	public static final AttachmentType<String> AFIJO = AttachmentRegistry.createPersistent(id("afijo"), Codec.STRING);

	@Override
	public void onInitialize() {
		// Los campos estáticos ya se registraron al cargar la clase
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("mobspoderosos", path);
	}
}
