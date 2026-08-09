package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.AddonSpellClientState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record AntoresBeamVisualPayload(List<BeamPath> beams) implements CustomPacketPayload {
    private static final int MAX_BEAMS = 6;
    public static final Type<AntoresBeamVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "antores_beam_visual")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, AntoresBeamVisualPayload> STREAM_CODEC =
            StreamCodec.of(AntoresBeamVisualPayload::encode, AntoresBeamVisualPayload::decode);

    public AntoresBeamVisualPayload {
        beams = beams == null
                ? List.of()
                : List.copyOf(beams.subList(0, Math.min(MAX_BEAMS, beams.size())));
    }

    @Override
    public @NotNull Type<AntoresBeamVisualPayload> type() {
        return TYPE;
    }

    public static void handle(AntoresBeamVisualPayload message, IPayloadContext context) {
        if (context.flow() != PacketFlow.CLIENTBOUND) {
            return;
        }
        context.enqueueWork(() -> AddonSpellClientState.applyAntores(message.beams));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, AntoresBeamVisualPayload message) {
        buffer.writeVarInt(message.beams.size());
        for (BeamPath beam : message.beams) {
            buffer.writeDouble(beam.startX);
            buffer.writeDouble(beam.startY);
            buffer.writeDouble(beam.startZ);
            buffer.writeDouble(beam.endX);
            buffer.writeDouble(beam.endY);
            buffer.writeDouble(beam.endZ);
        }
    }

    private static AntoresBeamVisualPayload decode(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_BEAMS) {
            throw new IllegalArgumentException("Invalid Antores beam count: " + count);
        }
        List<BeamPath> beams = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            beams.add(new BeamPath(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble()
            ));
        }
        return new AntoresBeamVisualPayload(beams);
    }

    public record BeamPath(
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ
    ) {
        public static BeamPath between(Vec3 start, Vec3 end) {
            return new BeamPath(start.x, start.y, start.z, end.x, end.y, end.z);
        }

        public Vec3 start() {
            return new Vec3(startX, startY, startZ);
        }

        public Vec3 end() {
            return new Vec3(endX, endY, endZ);
        }
    }
}
