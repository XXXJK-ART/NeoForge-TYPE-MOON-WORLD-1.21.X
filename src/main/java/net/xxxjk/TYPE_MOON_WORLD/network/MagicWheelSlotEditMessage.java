package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicClassification;

public record MagicWheelSlotEditMessage(
   int action,
   int wheelIndex,
   int slotIndex,
   int targetSlotIndex,
   String sourceType,
   String magicId,
   CompoundTag presetPayload,
   String crestEntryId,
   String displayName
) implements CustomPacketPayload {
   private static final int MAX_MAGIC_ID_LENGTH = 64;
   private static final int MAX_TEXT_LENGTH = 128;
   public static final int ACTION_SET = 0;
   public static final int ACTION_CLEAR = 1;
   public static final int ACTION_SWAP = 2;
   public static final Type<MagicWheelSlotEditMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "magic_wheel_slot_edit"));
   public static final StreamCodec<RegistryFriendlyByteBuf, MagicWheelSlotEditMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeInt(message.action);
         buffer.writeInt(message.wheelIndex);
         buffer.writeInt(message.slotIndex);
         buffer.writeInt(message.targetSlotIndex);
         buffer.writeUtf(message.sourceType == null ? "self" : message.sourceType, 128);
         buffer.writeUtf(message.magicId == null ? "" : message.magicId, 64);
         buffer.writeNbt(message.presetPayload == null ? new CompoundTag() : message.presetPayload);
         buffer.writeUtf(message.crestEntryId == null ? "" : message.crestEntryId, 128);
         buffer.writeUtf(message.displayName == null ? "" : message.displayName, 128);
      },
      buffer -> {
         int action = buffer.readInt();
         int wheelIndex = buffer.readInt();
         int slotIndex = buffer.readInt();
         int targetSlotIndex = buffer.readInt();
         String sourceType = buffer.readUtf(128);
         String magicId = buffer.readUtf(64);
         CompoundTag payload = buffer.readNbt();
         String crestEntryId = buffer.readUtf(128);
         String displayName = buffer.readUtf(128);
         return new MagicWheelSlotEditMessage(
            action, wheelIndex, slotIndex, targetSlotIndex, sourceType, magicId, payload == null ? new CompoundTag() : payload, crestEntryId, displayName
         );
      }
   );

   public Type<MagicWheelSlotEditMessage> type() {
      return TYPE;
   }

   public static void handleData(MagicWheelSlotEditMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  Player player = context.player();
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  vars.ensureMagicSystemInitialized();
                  if (message.wheelIndex >= 0 && message.wheelIndex < 10) {
                     if (message.slotIndex >= 0 && message.slotIndex < 12) {
                        switch (message.action) {
                           case 0:
                              handleSet(player, vars, message);
                              break;
                           case 1:
                              vars.clearWheelSlotEntry(message.wheelIndex, message.slotIndex);
                              break;
                           case 2:
                              if (message.targetSlotIndex < 0 || message.targetSlotIndex >= 12) {
                                 return;
                              }

                              vars.swapWheelSlots(message.wheelIndex, message.slotIndex, message.targetSlotIndex);
                              break;
                           default:
                              return;
                        }

                        vars.rebuildSelectedMagicsFromActiveWheel();
                        vars.syncPlayerVariables(player);
                     }
                  }
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle MagicWheelSlotEditMessage", e);
               return null;
            });
      }
   }

   private static void handleSet(Player player, TypeMoonWorldModVariables.PlayerVariables vars, MagicWheelSlotEditMessage message) {
      String magicId = message.magicId == null ? "" : message.magicId;
      if (MagicClassification.isKnownMagic(magicId)) {
         if (isKnowledgeOnlyMagic(magicId)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.knowledge_only"), true);
         } else {
            String sourceType = "crest".equals(message.sourceType) ? "crest" : "self";
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(
               message.wheelIndex, message.slotIndex
            );
            entry.sourceType = sourceType;
            entry.magicId = magicId;
            entry.presetPayload = message.presetPayload == null ? new CompoundTag() : message.presetPayload.copy();
            if ("projection".equals(magicId)) {
               entry.presetPayload = TypeMoonWorldModVariables.PlayerVariables.normalizeProjectionPresetPayload(entry.presetPayload);
            }

            entry.displayNameCache = message.displayName == null ? "" : message.displayName;
            if ("crest".equals(sourceType)) {
               if (!vars.hasValidImplantedCrest()) {
                  return;
               }

               TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry = vars.getCrestEntryById(message.crestEntryId);
               if (crestEntry == null || !crestEntry.active || !magicId.equals(crestEntry.magicId)) {
                  return;
               }

               entry.crestEntryId = crestEntry.entryId;
               if ("plunder".equals(crestEntry.sourceKind)) {
                  entry.presetPayload = crestEntry.presetPayload == null ? new CompoundTag() : crestEntry.presetPayload.copy();
               } else if (entry.presetPayload.isEmpty()) {
                  entry.presetPayload = crestEntry.presetPayload == null ? new CompoundTag() : crestEntry.presetPayload.copy();
               }

               if ("projection".equals(magicId)) {
                  entry.presetPayload = TypeMoonWorldModVariables.PlayerVariables.normalizeProjectionPresetPayload(entry.presetPayload);
               }
            } else {
               if (!hasLearnedMagic(vars, magicId)) {
                  return;
               }

               entry.crestEntryId = "";
            }

            vars.setWheelSlotEntry(message.wheelIndex, message.slotIndex, entry);
         }
      }
   }

   private static boolean hasLearnedMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      return !"reinforcement".equals(magicId)
         ? vars.learned_magics.contains(magicId)
         : vars.learned_magics.contains("reinforcement")
            || vars.learned_magics.contains("reinforcement_self")
            || vars.learned_magics.contains("reinforcement_other")
            || vars.learned_magics.contains("reinforcement_item");
   }

   private static boolean isKnowledgeOnlyMagic(String magicId) {
      return "jewel_magic_shoot".equals(magicId) || "jewel_magic_release".equals(magicId);
   }
}
