package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicResearchTableMenu;
import org.jetbrains.annotations.NotNull;

public record MagicResearchMessage(String magicId) implements CustomPacketPayload {
   public static final Type<MagicResearchMessage> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld","magic_research"));
   public static final StreamCodec<RegistryFriendlyByteBuf,MagicResearchMessage> STREAM_CODEC=StreamCodec.of((b,m)->b.writeUtf(m.magicId==null?"":m.magicId,64),b->new MagicResearchMessage(b.readUtf(64)));
   @Override @NotNull public Type<MagicResearchMessage> type(){return TYPE;}
   public static void handleData(MagicResearchMessage m,IPayloadContext c){if(c.flow()==PacketFlow.SERVERBOUND)c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p&&p.containerMenu instanceof MagicResearchTableMenu menu&&m.magicId.matches("[a-z0-9_]+"))menu.tryResearch(p,m.magicId);});}
}
