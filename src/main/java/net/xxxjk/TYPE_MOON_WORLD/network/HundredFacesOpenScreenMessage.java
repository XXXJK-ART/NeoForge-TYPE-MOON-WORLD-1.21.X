package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;

public record HundredFacesOpenScreenMessage(int kind, List<Target> targets) implements CustomPacketPayload {
   public static final Type<HundredFacesOpenScreenMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "hundred_faces_open_screen"));
   public static final StreamCodec<RegistryFriendlyByteBuf, HundredFacesOpenScreenMessage> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
      buf.writeVarInt(msg.kind);
      buf.writeVarInt(msg.targets.size());
      for (Target target : msg.targets) {
         buf.writeVarInt(target.entityId);
         buf.writeInt(target.x);
         buf.writeInt(target.y);
         buf.writeInt(target.z);
         buf.writeUtf(target.name, 64);
         buf.writeFloat(target.health);
         buf.writeFloat(target.maxHealth);
         buf.writeFloat(target.armor);
         buf.writeUtf(target.mode, 24);
         buf.writeVarInt(target.command);
         buf.writeBoolean(target.attackEnabled);
      }
   }, buf -> {
      int kind = buf.readVarInt();
      int count = Math.min(128, buf.readVarInt());
      List<Target> targets = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
         targets.add(new Target(buf.readVarInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf(64),
            buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readUtf(24), buf.readVarInt(), buf.readBoolean()));
      }
      return new HundredFacesOpenScreenMessage(kind, targets);
   });

   @Override
   public Type<HundredFacesOpenScreenMessage> type() {
      return TYPE;
   }

   public static void handleData(HundredFacesOpenScreenMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> ClientPacketHandler.openHundredFacesScreen(msg.kind, msg.targets));
   }

   public record Target(int entityId, int x, int y, int z, String name, float health, float maxHealth, float armor,
                        String mode, int command, boolean attackEnabled) {
   }
}
