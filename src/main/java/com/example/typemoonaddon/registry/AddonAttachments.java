package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.CursedArmorViewData;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.data.PollutionData;
import com.example.typemoonaddon.data.SpiritualDamageData;
import com.example.typemoonaddon.network.AddonNetwork;
import com.example.typemoonaddon.network.SakuraAttachmentSyncPayload;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class AddonAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ImaginarySpaceData>> IMAGINARY_SPACE =
            ATTACHMENTS.register("imaginary_space", () -> AttachmentType.serializable(ImaginarySpaceData::new)
                    .copyOnDeath()
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PollutionData>> POLLUTION =
            ATTACHMENTS.register("pollution", () -> AttachmentType.serializable(PollutionData::new)
                    .copyOnDeath()
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CursedArmorViewData>> CURSED_ARMOR_VIEW =
            ATTACHMENTS.register("cursed_armor_view", () -> AttachmentType.builder(CursedArmorViewData::new)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SpiritualDamageData>> SPIRITUAL_DAMAGE =
            ATTACHMENTS.register("spiritual_damage", () -> AttachmentType.serializable(SpiritualDamageData::new)
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    public static void sync(@Nullable Object holder, Supplier<? extends AttachmentType<?>> type) {
        if (!(holder instanceof Entity entity) || type == null) {
            return;
        }
        AttachmentType<?> attachmentType = type.get();
        if (attachmentType == IMAGINARY_SPACE.get() && entity instanceof ServerPlayer player) {
            AddonNetwork.sendToPlayer(player, SakuraAttachmentSyncPayload.imaginarySpace(
                    player.getId(),
                    player.getData(IMAGINARY_SPACE.get())));
        } else if (attachmentType == POLLUTION.get()) {
            sendToLevel(entity, SakuraAttachmentSyncPayload.pollution(
                    entity.getId(),
                    entity.getData(POLLUTION.get())));
        } else if (attachmentType == CURSED_ARMOR_VIEW.get()) {
            sendToLevel(entity, SakuraAttachmentSyncPayload.cursedArmorView(
                    entity.getId(),
                    entity.getData(CURSED_ARMOR_VIEW.get())));
        } else if (attachmentType == SPIRITUAL_DAMAGE.get()) {
            sendToLevel(entity, SakuraAttachmentSyncPayload.spiritualDamage(
                    entity.getId(),
                    entity.getData(SPIRITUAL_DAMAGE.get())));
        }
    }

    private static void sendToLevel(Entity entity, SakuraAttachmentSyncPayload payload) {
        if (entity.level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.players()) {
                AddonNetwork.sendToPlayer(player, payload);
            }
        }
    }

    private AddonAttachments() {
    }
}
