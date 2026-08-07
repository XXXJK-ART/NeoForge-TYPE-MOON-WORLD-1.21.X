package com.example.typemoonaddon.kimaris;

import com.example.typemoonaddon.TypeMoonAddon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class KimarisAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<KimarisFrozenData>> FROZEN_STATE =
            ATTACHMENTS.register("kimaris_frozen_state", () ->
                    AttachmentType.serializable(KimarisFrozenData::new).build());

    private KimarisAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
