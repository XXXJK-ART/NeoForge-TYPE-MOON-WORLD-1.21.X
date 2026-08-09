package com.example.typemoonaddon.storage;

import com.example.typemoonaddon.TypeMoonAddon;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class StorageAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TypeMoonAddon.MOD_ID);

    public static final Supplier<AttachmentType<StorageData>> PLAYER_STORAGE = ATTACHMENTS.register(
            "player_storage",
            () -> AttachmentType.serializable(StorageData::new).copyOnDeath().build()
    );

    private StorageAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
