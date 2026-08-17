package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.AddonSpellClientState;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Compact, server-authored cues for the shared deterministic spell renderer. */
public record AddonSpellVisualPayload(
        byte action,
        byte spell,
        byte stage,
        UUID ownerId,
        int entityId,
        Vec3 origin,
        Vec3 target,
        float scale,
        float secondaryScale,
        int durationTicks,
        int variant
) implements CustomPacketPayload {
    public static final byte SHOW = 0;
    public static final byte CLEAR_SPELL = 1;

    public static final byte STORM = 0;
    public static final byte ORIAS = 1;
    public static final byte KIMARIS = 2;
    public static final byte ANDREPHIUS = 3;
    public static final byte ANTORES = 4;
    public static final byte ZAGAN = 5;
    public static final byte ANDRASIAS = 6;
    public static final byte DETECTION = 7;
    public static final byte NEGA_SUMMON = 8;

    public static final byte SIGIL = 0;
    public static final byte TORNADO = 1;
    public static final byte ERUPTION = 2;
    public static final byte ICE_SPHERE = 3;
    public static final byte LIGHTNING_STORM = 4;
    public static final byte LIGHTNING_STRIKE = 5;
    public static final byte BEAM = 6;
    public static final byte WATER_FIELD = 7;
    public static final byte WATER_PRISON = 8;
    public static final byte GROUND_IMPACT = 9;
    public static final byte GROUND_RIFT = 10;
    public static final byte AFTERMATH = 11;
    public static final byte NEGA_LOCK = 12;
    public static final byte NEGA_COLLAPSE = 13;

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    private static final int MAX_DURATION_TICKS = 600;
    private static final float MAX_SCALE = 512.0F;

    public static final Type<AddonSpellVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "addon_spell_visual")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AddonSpellVisualPayload> STREAM_CODEC =
            StreamCodec.of(AddonSpellVisualPayload::encode, AddonSpellVisualPayload::decode);

    public AddonSpellVisualPayload {
        ownerId = ownerId == null ? EMPTY_UUID : ownerId;
        origin = finite(origin) ? origin : Vec3.ZERO;
        target = finite(target) ? target : origin;
        scale = finiteScale(scale);
        secondaryScale = finiteScale(secondaryScale);
        durationTicks = Mth.clamp(durationTicks, 1, MAX_DURATION_TICKS);
    }

    @Override
    public @NotNull Type<AddonSpellVisualPayload> type() {
        return TYPE;
    }

    public static void handle(AddonSpellVisualPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> AddonSpellClientState.apply(message));
    }

    public static void showAttached(
            ServerLevel level,
            byte spell,
            byte stage,
            UUID ownerId,
            Entity entity,
            float scale,
            float secondaryScale,
            int durationTicks,
            int variant,
            double observerRadius
    ) {
        if (level == null || entity == null) {
            return;
        }
        Vec3 center = entity.getBoundingBox().getCenter();
        send(level, center, observerRadius, new AddonSpellVisualPayload(
                SHOW, spell, stage, ownerId, entity.getId(), center, center,
                scale, secondaryScale, durationTicks, variant));
    }

    public static void showAt(
            ServerLevel level,
            byte spell,
            byte stage,
            UUID ownerId,
            Vec3 origin,
            float scale,
            float secondaryScale,
            int durationTicks,
            int variant,
            double observerRadius
    ) {
        if (level == null || !finite(origin)) {
            return;
        }
        send(level, origin, observerRadius, new AddonSpellVisualPayload(
                SHOW, spell, stage, ownerId, -1, origin, origin,
                scale, secondaryScale, durationTicks, variant));
    }

    public static void showBetween(
            ServerLevel level,
            byte spell,
            byte stage,
            UUID ownerId,
            Vec3 origin,
            Vec3 target,
            float scale,
            float secondaryScale,
            int durationTicks,
            int variant,
            double observerRadius
    ) {
        if (level == null || !finite(origin) || !finite(target)) {
            return;
        }
        send(level, origin, observerRadius, new AddonSpellVisualPayload(
                SHOW, spell, stage, ownerId, -1, origin, target,
                scale, secondaryScale, durationTicks, variant));
    }

    public static void clear(ServerPlayer player, byte spell, double observerRadius) {
        if (player == null) {
            return;
        }
        Vec3 center = player.getBoundingBox().getCenter();
        send(player.serverLevel(), center, observerRadius, new AddonSpellVisualPayload(
                CLEAR_SPELL, spell, SIGIL, player.getUUID(), player.getId(), center, center,
                1.0F, 1.0F, 1, 0));
    }

    private static void send(
            ServerLevel level,
            Vec3 center,
            double observerRadius,
            AddonSpellVisualPayload payload
    ) {
        double radius = Mth.clamp(
                Double.isFinite(observerRadius) ? observerRadius : 128.0D,
                16.0D,
                320.0D
        );
        AddonNetwork.sendNear(level, center.x, center.y, center.z, radius, payload);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, AddonSpellVisualPayload message) {
        buffer.writeByte(message.action);
        buffer.writeByte(message.spell);
        buffer.writeByte(message.stage);
        buffer.writeUUID(message.ownerId);
        buffer.writeVarInt(message.entityId + 1);
        writeVec3(buffer, message.origin);
        writeVec3(buffer, message.target);
        buffer.writeFloat(message.scale);
        buffer.writeFloat(message.secondaryScale);
        buffer.writeVarInt(message.durationTicks);
        buffer.writeVarInt(message.variant);
    }

    private static AddonSpellVisualPayload decode(RegistryFriendlyByteBuf buffer) {
        byte action = buffer.readByte();
        if (action != SHOW && action != CLEAR_SPELL) {
            throw new IllegalArgumentException("Invalid addon spell visual action: " + action);
        }
        byte spell = buffer.readByte();
        byte stage = buffer.readByte();
        UUID ownerId = buffer.readUUID();
        int entityId = buffer.readVarInt() - 1;
        Vec3 origin = readVec3(buffer);
        Vec3 target = readVec3(buffer);
        float scale = buffer.readFloat();
        float secondaryScale = buffer.readFloat();
        int duration = buffer.readVarInt();
        int variant = buffer.readVarInt();
        if (spell < STORM || spell > NEGA_SUMMON || stage < SIGIL || stage > NEGA_COLLAPSE) {
            throw new IllegalArgumentException("Invalid addon spell visual kind");
        }
        return new AddonSpellVisualPayload(
                action, spell, stage, ownerId, entityId, origin, target,
                scale, secondaryScale, duration, variant);
    }

    private static void writeVec3(RegistryFriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 readVec3(RegistryFriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static float finiteScale(float value) {
        return Float.isFinite(value) ? Mth.clamp(value, 0.05F, MAX_SCALE) : 1.0F;
    }

    private static boolean finite(Vec3 value) {
        return value != null
                && Double.isFinite(value.x)
                && Double.isFinite(value.y)
                && Double.isFinite(value.z);
    }
}
