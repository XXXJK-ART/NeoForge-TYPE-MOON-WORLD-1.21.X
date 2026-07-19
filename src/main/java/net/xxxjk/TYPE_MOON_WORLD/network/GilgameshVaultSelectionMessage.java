package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardGilgameshSkills;
import org.jetbrains.annotations.NotNull;

public record GilgameshVaultSelectionMessage(int index) implements CustomPacketPayload {
   public static final Type<GilgameshVaultSelectionMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "gilgamesh_vault_selection"));
   public static final StreamCodec<RegistryFriendlyByteBuf, GilgameshVaultSelectionMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeVarInt(message.index), buffer -> new GilgameshVaultSelectionMessage(buffer.readVarInt()));
   @Override @NotNull public Type<GilgameshVaultSelectionMessage> type() { return TYPE; }
   public static void handleData(GilgameshVaultSelectionMessage message, IPayloadContext context) {
      context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) ServantCardGilgameshSkills.selectTreasure(player, message.index); });
   }
}
