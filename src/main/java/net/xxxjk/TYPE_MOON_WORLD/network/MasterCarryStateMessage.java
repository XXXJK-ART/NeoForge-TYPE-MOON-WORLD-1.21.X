package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService;

/** Visual state is explicit: observers do not receive the owners' complete private variables. */
public record MasterCarryStateMessage(UUID servant, UUID master, boolean carrying) implements CustomPacketPayload {
   public static final Type<MasterCarryStateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "master_carry_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MasterCarryStateMessage> STREAM_CODEC = StreamCodec.of(
      (buf, msg) -> { buf.writeUUID(msg.servant); buf.writeUUID(msg.master); buf.writeBoolean(msg.carrying); },
      buf -> new MasterCarryStateMessage(buf.readUUID(), buf.readUUID(), buf.readBoolean()));
   @Override public Type<MasterCarryStateMessage> type() { return TYPE; }
   public static void handle(MasterCarryStateMessage message, IPayloadContext context) {
      context.enqueueWork(() -> ServantMasterCarryService.applyClientPair(message.servant, message.master, message.carrying));
   }
}
