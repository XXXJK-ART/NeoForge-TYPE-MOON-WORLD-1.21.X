package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.client.ClientPacketHandler;

public record BaobhanSithCurseOpenScreenMessage(List<Target> targets) implements CustomPacketPayload {
   public static final Type<BaobhanSithCurseOpenScreenMessage> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "baobhan_sith_curse_open_screen"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BaobhanSithCurseOpenScreenMessage> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
      buf.writeVarInt(msg.targets.size());
      for (Target target : msg.targets) {
         buf.writeVarInt(target.entityId);
         buf.writeUUID(target.uuid);
         buf.writeUtf(target.name, 96);
         buf.writeUtf(target.dimension, 96);
         buf.writeInt(target.x);
         buf.writeInt(target.y);
         buf.writeInt(target.z);
         buf.writeFloat(target.health);
         buf.writeFloat(target.maxHealth);
         buf.writeVarInt(target.layers);
         buf.writeVarInt(target.blood);
         buf.writeVarInt(target.skin);
         buf.writeVarInt(target.hair);
         buf.writeVarInt(target.remains);
         buf.writeVarInt(target.bloodCurse);
         buf.writeVarInt(target.skinCurse);
         buf.writeVarInt(target.hairCurse);
         buf.writeVarInt(target.remainsCurse);
      }
   }, buf -> {
      int count = Math.min(256, buf.readVarInt());
      List<Target> targets = new ArrayList<>();
      for (int i = 0; i < count; i++) {
         targets.add(new Target(
            buf.readVarInt(),
            buf.readUUID(),
            buf.readUtf(96),
            buf.readUtf(96),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readFloat(),
            buf.readFloat(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt()
         ));
      }
      return new BaobhanSithCurseOpenScreenMessage(targets);
   });

   @Override
   public Type<BaobhanSithCurseOpenScreenMessage> type() {
      return TYPE;
   }

   public static void handleData(BaobhanSithCurseOpenScreenMessage msg, IPayloadContext ctx) {
      ctx.enqueueWork(() -> ClientPacketHandler.openBaobhanSithCurseScreen(msg.targets));
   }

   public record Target(
      int entityId,
      UUID uuid,
      String name,
      String dimension,
      int x,
      int y,
      int z,
      float health,
      float maxHealth,
      int layers,
      int blood,
      int skin,
      int hair,
      int remains,
      int bloodCurse,
      int skinCurse,
      int hairCurse,
      int remainsCurse
   ) {
   }
}
