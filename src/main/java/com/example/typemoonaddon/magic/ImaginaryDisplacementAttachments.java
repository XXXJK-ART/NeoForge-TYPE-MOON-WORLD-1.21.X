package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ImaginaryDisplacementAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ImaginaryDisplacementData>> PLAYER_STATE =
            ATTACHMENTS.register("imaginary_displacement_state", () ->
                    AttachmentType.serializable(ImaginaryDisplacementData::new).copyOnDeath().build());

    private ImaginaryDisplacementAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
