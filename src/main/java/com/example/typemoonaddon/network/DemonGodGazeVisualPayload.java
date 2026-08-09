package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.DemonGodGazeClientState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** Compact authoritative timeline used by the deterministic gaze renderer. */
public record DemonGodGazeVisualPayload(
        byte action,
        UUID castId,
        List<TargetLayout> targets,
        int targetEntityId,
        int circleIndex,
        Vec3 shotStart,
        Vec3 shotEnd
) implements CustomPacketPayload {
    public static final byte START = 0;
    public static final byte FIRE = 1;
    public static final byte CLEAR = 2;
    private static final int MAX_TARGETS = 11;
    private static final int CIRCLES_PER_TARGET = 11;

    public static final Type<DemonGodGazeVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "demon_god_gaze_visual")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, DemonGodGazeVisualPayload> STREAM_CODEC =
            StreamCodec.of(DemonGodGazeVisualPayload::encode, DemonGodGazeVisualPayload::decode);

    public DemonGodGazeVisualPayload {
        castId = castId == null ? new UUID(0L, 0L) : castId;
        targets = targets == null
                ? List.of()
                : List.copyOf(targets.subList(0, Math.min(MAX_TARGETS, targets.size())));
        shotStart = shotStart == null ? Vec3.ZERO : shotStart;
        shotEnd = shotEnd == null ? Vec3.ZERO : shotEnd;
    }

    public static DemonGodGazeVisualPayload start(UUID castId, List<TargetLayout> targets) {
        return new DemonGodGazeVisualPayload(START, castId, targets, -1, -1, Vec3.ZERO, Vec3.ZERO);
    }

    public static DemonGodGazeVisualPayload fire(
            UUID castId,
            int targetEntityId,
            int circleIndex,
            Vec3 start,
            Vec3 end
    ) {
        return new DemonGodGazeVisualPayload(
                FIRE, castId, List.of(), targetEntityId, circleIndex, start, end);
    }

    public static DemonGodGazeVisualPayload clear(UUID castId) {
        return new DemonGodGazeVisualPayload(CLEAR, castId, List.of(), -1, -1, Vec3.ZERO, Vec3.ZERO);
    }

    @Override
    public @NotNull Type<DemonGodGazeVisualPayload> type() {
        return TYPE;
    }

    public static void handle(DemonGodGazeVisualPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> DemonGodGazeClientState.apply(message));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, DemonGodGazeVisualPayload message) {
        buffer.writeByte(message.action);
        buffer.writeUUID(message.castId);
        if (message.action == START) {
            buffer.writeVarInt(message.targets.size());
            for (TargetLayout target : message.targets) {
                buffer.writeVarInt(Math.max(0, target.entityId));
                writeVec3(buffer, target.anchor);
                buffer.writeVarInt(target.offsets.size());
                for (Vec3 offset : target.offsets) {
                    buffer.writeFloat((float) offset.x);
                    buffer.writeFloat((float) offset.y);
                    buffer.writeFloat((float) offset.z);
                }
            }
        } else if (message.action == FIRE) {
            buffer.writeVarInt(Math.max(0, message.targetEntityId));
            buffer.writeVarInt(Math.max(0, message.circleIndex));
            writeVec3(buffer, message.shotStart);
            writeVec3(buffer, message.shotEnd);
        }
    }

    private static DemonGodGazeVisualPayload decode(RegistryFriendlyByteBuf buffer) {
        byte action = buffer.readByte();
        UUID castId = buffer.readUUID();
        if (action == START) {
            int targetCount = checkedCount(buffer.readVarInt(), MAX_TARGETS, "target");
            List<TargetLayout> targets = new ArrayList<>(targetCount);
            for (int targetIndex = 0; targetIndex < targetCount; targetIndex++) {
                int entityId = buffer.readVarInt();
                Vec3 anchor = readVec3(buffer);
                int offsetCount = checkedCount(buffer.readVarInt(), CIRCLES_PER_TARGET, "circle");
                if (offsetCount != CIRCLES_PER_TARGET) {
                    throw new IllegalArgumentException("Demon God Gaze requires eleven circle offsets");
                }
                List<Vec3> offsets = new ArrayList<>(offsetCount);
                for (int offsetIndex = 0; offsetIndex < offsetCount; offsetIndex++) {
                    offsets.add(new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));
                }
                targets.add(new TargetLayout(entityId, anchor, offsets));
            }
            return start(castId, targets);
        }
        if (action == FIRE) {
            return fire(castId, buffer.readVarInt(), buffer.readVarInt(), readVec3(buffer), readVec3(buffer));
        }
        if (action != CLEAR) {
            throw new IllegalArgumentException("Invalid Demon God Gaze visual action: " + action);
        }
        return clear(castId);
    }

    private static int checkedCount(int count, int maximum, String label) {
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid Demon God Gaze " + label + " count: " + count);
        }
        return count;
    }

    private static void writeVec3(RegistryFriendlyByteBuf buffer, Vec3 value) {
        buffer.writeDouble(value.x);
        buffer.writeDouble(value.y);
        buffer.writeDouble(value.z);
    }

    private static Vec3 readVec3(RegistryFriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public record TargetLayout(int entityId, Vec3 anchor, List<Vec3> offsets) {
        public TargetLayout {
            anchor = anchor == null ? Vec3.ZERO : anchor;
            offsets = offsets == null
                    ? List.of()
                    : List.copyOf(offsets.subList(0, Math.min(CIRCLES_PER_TARGET, offsets.size())));
        }
    }
}
