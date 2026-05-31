package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicCrestItem;

public record ImplantMagicCrestMessage(int handIndex) implements CustomPacketPayload {
   public static final Type<ImplantMagicCrestMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "implant_magic_crest"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ImplantMagicCrestMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> buffer.writeInt(message.handIndex), buffer -> new ImplantMagicCrestMessage(buffer.readInt())
   );

   public Type<ImplantMagicCrestMessage> type() {
      return TYPE;
   }

   public static void handleData(ImplantMagicCrestMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  Player player = context.player();
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  vars.ensureMagicSystemInitialized();
                  ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                  ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
                  boolean mainValid = !mainHand.isEmpty() && mainHand.getItem() instanceof MagicCrestItem;
                  boolean offValid = !offHand.isEmpty() && offHand.getItem() instanceof MagicCrestItem;
                  if (mainValid || offValid) {
                     InteractionHand hand;
                     if (mainValid && offValid) {
                        hand = message.handIndex == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                     } else {
                        hand = mainValid ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                     }

                     ItemStack sourceStack = player.getItemInHand(hand);
                     List<TypeMoonWorldModVariables.PlayerVariables.CrestEntry> incomingEntries = TypeMoonWorldModVariables.PlayerVariables.readCrestEntriesFromStack(
                        sourceStack
                     );
                     ItemStack implantedStack = vars.magicCrestInventory.getStackInSlot(0);
                     if (implantedStack.isEmpty()) {
                        ItemStack implanted = sourceStack.copy();
                        implanted.setCount(1);
                        vars.magicCrestInventory.setStackInSlot(0, implanted);
                        sourceStack.shrink(1);
                     } else {
                        if (!(implantedStack.getItem() instanceof MagicCrestItem)) {
                           return;
                        }

                        sourceStack.shrink(1);
                     }

                     vars.mergeImplantedCrestEntries(incomingEntries);
                     vars.syncSelfCrestEntriesFromKnowledge();
                     vars.pruneInvalidCrestWheelReferences();
                     vars.rebuildSelectedMagicsFromActiveWheel();
                     ItemStack finalImplanted = vars.magicCrestInventory.getStackInSlot(0);
                     if (!finalImplanted.isEmpty()) {
                        TypeMoonWorldModVariables.PlayerVariables.writeCrestEntriesToStack(finalImplanted, vars.crest_entries);
                     }

                     vars.syncPlayerVariables(player);
                  }
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle ImplantMagicCrestMessage", e);
               return null;
            });
      }
   }
}
