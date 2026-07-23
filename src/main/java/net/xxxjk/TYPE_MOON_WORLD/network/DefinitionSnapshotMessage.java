package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.typemoonworld.api.DefinitionSnapshot;
import net.xxxjk.typemoonworld.api.DefinitionSnapshotStore;

public record DefinitionSnapshotMessage(long revision, String payload) implements CustomPacketPayload {
   public static final Type<DefinitionSnapshotMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "definition_snapshot"));
   public static final StreamCodec<FriendlyByteBuf, DefinitionSnapshotMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, value) -> { buffer.writeLong(value.revision); buffer.writeUtf(value.payload, 1_500_000); },
      buffer -> new DefinitionSnapshotMessage(buffer.readLong(), buffer.readUtf(1_500_000)));
   @Override public Type<DefinitionSnapshotMessage> type() { return TYPE; }
   public static void handleData(DefinitionSnapshotMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.CLIENTBOUND) return;
      context.enqueueWork(() -> {
         if (FMLEnvironment.dist != Dist.CLIENT) return;
         Map<String, String> sections = new LinkedHashMap<>();
         // Payload is a bounded JSON object. Keep it opaque here so client-only API users can parse it safely.
         String payload = message.payload == null ? "{}" : message.payload;
         sections.put("all", payload);
         try {
            var root = JsonParser.parseString(payload);
            if (root.isJsonObject()) root.getAsJsonObject().entrySet().forEach(entry -> sections.put(entry.getKey(), entry.getValue().toString()));
         } catch (RuntimeException ignored) { }
         DefinitionSnapshotStore.replace(new DefinitionSnapshot(message.revision, sections));
      });
   }
}
