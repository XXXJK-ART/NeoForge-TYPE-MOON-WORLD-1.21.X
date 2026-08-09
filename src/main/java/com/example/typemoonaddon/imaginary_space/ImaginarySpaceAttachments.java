package com.example.typemoonaddon.imaginary_space;

import com.example.typemoonaddon.TypeMoonAddon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ImaginarySpaceAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ImaginarySpaceData>> PLAYER_STATE =
            ATTACHMENTS.register("imaginary_space_state", () ->
                    AttachmentType.serializable(ImaginarySpaceData::new).copyOnDeath().build());

    private ImaginarySpaceAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
