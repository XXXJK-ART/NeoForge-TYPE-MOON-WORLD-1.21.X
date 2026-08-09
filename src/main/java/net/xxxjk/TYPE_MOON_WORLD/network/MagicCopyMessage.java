package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicCopyingTableMenu;
import org.jetbrains.annotations.NotNull;

public record MagicCopyMessage(String magicId) implements CustomPacketPayload {
   public static final Type<MagicCopyMessage> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld","magic_copy"));
   public static final StreamCodec<RegistryFriendlyByteBuf,MagicCopyMessage> STREAM_CODEC=StreamCodec.of((b,m)->b.writeUtf(m.magicId==null?"":m.magicId,64),b->new MagicCopyMessage(b.readUtf(64)));
   @Override @NotNull public Type<MagicCopyMessage> type(){return TYPE;}
   public static void handleData(MagicCopyMessage m,IPayloadContext c){if(c.flow()==PacketFlow.SERVERBOUND)c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p&&p.containerMenu instanceof MagicCopyingTableMenu menu&&m.magicId.matches("[a-z0-9_]+"))menu.tryCopy(p,m.magicId);});}
}
