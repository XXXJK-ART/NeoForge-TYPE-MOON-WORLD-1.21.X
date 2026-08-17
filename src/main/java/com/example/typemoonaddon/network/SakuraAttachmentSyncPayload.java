package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.CursedArmorViewData;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.data.PollutionData;
import com.example.typemoonaddon.data.SpiritualDamageData;
import com.example.typemoonaddon.registry.AddonAttachments;
import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SakuraAttachmentSyncPayload(
        int entityId,
        Kind kind,
        @Nullable ImaginarySpaceData imaginarySpace,
        @Nullable PollutionData pollution,
        @Nullable CursedArmorViewData cursedArmorView,
        @Nullable SpiritualDamageData spiritualDamage
) implements CustomPacketPayload {
    public static final Type<SakuraAttachmentSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "sakura_attachment_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SakuraAttachmentSyncPayload> STREAM_CODEC =
            StreamCodec.of(SakuraAttachmentSyncPayload::write, SakuraAttachmentSyncPayload::read);

    public static SakuraAttachmentSyncPayload imaginarySpace(int entityId, ImaginarySpaceData data) {
        return new SakuraAttachmentSyncPayload(entityId, Kind.IMAGINARY_SPACE, data, null, null, null);
    }

    public static SakuraAttachmentSyncPayload pollution(int entityId, PollutionData data) {
        return new SakuraAttachmentSyncPayload(entityId, Kind.POLLUTION, null, data, null, null);
    }

    public static SakuraAttachmentSyncPayload cursedArmorView(int entityId, CursedArmorViewData data) {
        return new SakuraAttachmentSyncPayload(entityId, Kind.CURSED_ARMOR_VIEW, null, null, data, null);
    }

    public static SakuraAttachmentSyncPayload spiritualDamage(int entityId, SpiritualDamageData data) {
        return new SakuraAttachmentSyncPayload(entityId, Kind.SPIRITUAL_DAMAGE, null, null, null, data);
    }

    @Override
    public @NotNull Type<SakuraAttachmentSyncPayload> type() {
        return TYPE;
    }

    public static void handle(SakuraAttachmentSyncPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            Entity entity = context.player().level().getEntity(message.entityId);
            if (entity == null && context.player().getId() == message.entityId) {
                entity = context.player();
            }
            if (entity == null) {
                return;
            }
            switch (message.kind) {
                case IMAGINARY_SPACE -> {
                    if (message.imaginarySpace != null) {
                        entity.setData(AddonAttachments.IMAGINARY_SPACE.get(), message.imaginarySpace);
                    }
                }
                case POLLUTION -> {
                    if (message.pollution != null) {
                        entity.setData(AddonAttachments.POLLUTION.get(), message.pollution);
                    }
                }
                case CURSED_ARMOR_VIEW -> {
                    if (message.cursedArmorView != null) {
                        entity.setData(AddonAttachments.CURSED_ARMOR_VIEW.get(), message.cursedArmorView);
                    }
                }
                case SPIRITUAL_DAMAGE -> {
                    if (message.spiritualDamage != null) {
                        entity.setData(AddonAttachments.SPIRITUAL_DAMAGE.get(), message.spiritualDamage);
                    }
                }
            }
        });
    }

    private static void write(RegistryFriendlyByteBuf buffer, SakuraAttachmentSyncPayload message) {
        buffer.writeVarInt(message.entityId);
        buffer.writeEnum(message.kind);
        switch (message.kind) {
            case IMAGINARY_SPACE -> ImaginarySpaceData.STREAM_CODEC.encode(buffer, message.imaginarySpace);
            case POLLUTION -> PollutionData.STREAM_CODEC.encode(buffer, message.pollution);
            case CURSED_ARMOR_VIEW -> CursedArmorViewData.STREAM_CODEC.encode(buffer, message.cursedArmorView);
            case SPIRITUAL_DAMAGE -> SpiritualDamageData.STREAM_CODEC.encode(buffer, message.spiritualDamage);
        }
    }

    private static SakuraAttachmentSyncPayload read(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        Kind kind = buffer.readEnum(Kind.class);
        return switch (kind) {
            case IMAGINARY_SPACE -> imaginarySpace(entityId, ImaginarySpaceData.STREAM_CODEC.decode(buffer));
            case POLLUTION -> pollution(entityId, PollutionData.STREAM_CODEC.decode(buffer));
            case CURSED_ARMOR_VIEW -> cursedArmorView(entityId, CursedArmorViewData.STREAM_CODEC.decode(buffer));
            case SPIRITUAL_DAMAGE -> spiritualDamage(entityId, SpiritualDamageData.STREAM_CODEC.decode(buffer));
        };
    }

    public enum Kind {
        IMAGINARY_SPACE,
        POLLUTION,
        CURSED_ARMOR_VIEW,
        SPIRITUAL_DAMAGE
    }
}
