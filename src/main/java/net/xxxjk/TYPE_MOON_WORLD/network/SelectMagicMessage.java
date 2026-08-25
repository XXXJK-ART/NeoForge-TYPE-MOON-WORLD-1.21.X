package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.Set;
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
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;

public record SelectMagicMessage(String magicId, boolean add) implements CustomPacketPayload {
   private static final int MAX_MAGIC_ID_LENGTH = 64;
   private static final Set<String> KNOWLEDGE_ONLY_MAGIC_IDS = Set.of("jewel_magic_shoot", "jewel_magic_release");
   public static final Type<SelectMagicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "select_magic"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SelectMagicMessage> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
      buffer.writeUtf(message.magicId == null ? "" : message.magicId, 64);
      buffer.writeBoolean(message.add);
   }, buffer -> new SelectMagicMessage(buffer.readUtf(64), buffer.readBoolean()));

   public Type<SelectMagicMessage> type() {
      return TYPE;
   }

   public static void handleData(SelectMagicMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  Player player = context.player();
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  String requestedMagicId = message.magicId == null ? "" : message.magicId;
                  String magicId = PlayerMagicSelectionService.canonicalRuntimeMagicId(requestedMagicId);
                  if (isValidMagicId(magicId)) {
                     if (message.add && (KNOWLEDGE_ONLY_MAGIC_IDS.contains(magicId) || MagicDefinitionRegistry.isKnowledgeOnly(magicId))) {
                        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.knowledge_only"), true);
                     } else {
                        boolean isLearned = vars.learned_magics.contains(magicId);
                        if (!isLearned && "reinforcement".equals(magicId)) {
                           isLearned = vars.learned_magics.contains("reinforcement_self")
                              || vars.learned_magics.contains("reinforcement_other")
                              || vars.learned_magics.contains("reinforcement_item");
                        }

                        vars.ensureMagicSystemInitialized();
                        int activeWheel = vars.active_wheel_index;
                        if (message.add) {
                           if (!isLearned) {
                              return;
                           }

                           for (int slot = 0; slot < 12; slot++) {
                              TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry existing = vars.getWheelSlotEntry(activeWheel, slot);
                              if (existing != null && !existing.isEmpty() && "self".equals(existing.sourceType) && magicId.equals(existing.magicId)) {
                                 vars.rebuildSelectedMagicsFromActiveWheel();
                                 vars.syncRuntimeSelection(player);
                                 return;
                              }
                           }

                           for (int slotx = 0; slotx < 12; slotx++) {
                              TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry existing = vars.getWheelSlotEntry(activeWheel, slotx);
                              if (existing == null || existing.isEmpty()) {
                                 TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(
                                    activeWheel, slotx
                                 );
                                 entry.sourceType = "self";
                                 entry.magicId = requestedMagicId;
                                 vars.setWheelSlotEntry(activeWheel, slotx, entry);
                                 break;
                              }
                           }
                        } else {
                           for (int slotxx = 0; slotxx < 12; slotxx++) {
                              TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry existing = vars.getWheelSlotEntry(activeWheel, slotxx);
                              if (existing != null && !existing.isEmpty() && "self".equals(existing.sourceType) && magicId.equals(existing.magicId)) {
                                 vars.clearWheelSlotEntry(activeWheel, slotxx);
                              }
                           }
                        }

                        vars.rebuildSelectedMagicsFromActiveWheel();
                        vars.syncRuntimeSelection(player);
                     }
                  }
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to handle SelectMagicMessage", e);
               return null;
            });
      }
   }

   private static boolean isValidMagicId(String magicId) {
      return magicId != null && !magicId.isEmpty() && magicId.length() <= 64
         && (magicId.matches("[a-z0-9_]+") || net.minecraft.resources.ResourceLocation.tryParse(magicId) != null)
         && MagicClassification.isKnownMagic(magicId) && MagicDefinitionRegistry.isWheelSelectable(magicId);
   }
}
