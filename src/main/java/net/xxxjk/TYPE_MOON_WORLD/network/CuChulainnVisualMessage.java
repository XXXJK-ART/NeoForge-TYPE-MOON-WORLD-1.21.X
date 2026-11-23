package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CuChulainnVisualMessage(UUID owner,int action,int ticks,Vec3 center,boolean flash,long start,boolean left) implements CustomPacketPayload {
   public static final Type<CuChulainnVisualMessage> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld","cu_visual"));
   public static final StreamCodec<RegistryFriendlyByteBuf,CuChulainnVisualMessage> STREAM_CODEC=StreamCodec.of(
      (b,m)->{b.writeUUID(m.owner);b.writeInt(m.action);b.writeInt(m.ticks);b.writeDouble(m.center.x);b.writeDouble(m.center.y);b.writeDouble(m.center.z);b.writeBoolean(m.flash);b.writeLong(m.start);b.writeBoolean(m.left);},
      b->new CuChulainnVisualMessage(b.readUUID(),b.readInt(),b.readInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readBoolean(),b.readLong(),b.readBoolean()));
   @Override public Type<CuChulainnVisualMessage> type(){return TYPE;}
   public static void handle(CuChulainnVisualMessage m,IPayloadContext context) {
      if(context.flow()!=net.minecraft.network.protocol.PacketFlow.CLIENTBOUND)return;
      context.enqueueWork(()->{
         if(net.neoforged.fml.loading.FMLEnvironment.dist==net.neoforged.api.distmarker.Dist.CLIENT)
            net.xxxjk.TYPE_MOON_WORLD.client.CuChulainnVisualClient.receive(m);
      });
   }
}
