package com.example.typemoonaddon.engravedworm;

import com.example.typemoonaddon.TypeMoonAddon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class EngravedWormAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EngravedWormInventoryData>> INVENTORY =
            ATTACHMENTS.register("engraved_worm_inventory", () ->
                    AttachmentType.serializable(EngravedWormInventoryData::new).copyOnDeath().build());

    private EngravedWormAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
