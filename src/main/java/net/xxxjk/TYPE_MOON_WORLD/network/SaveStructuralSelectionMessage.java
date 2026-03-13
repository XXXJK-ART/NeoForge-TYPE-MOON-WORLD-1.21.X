package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicStructuralAnalysis;
import org.jetbrains.annotations.NotNull;

public record SaveStructuralSelectionMessage(String structureName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) implements CustomPacketPayload {
   public static final Type<SaveStructuralSelectionMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "save_structural_selection"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SaveStructuralSelectionMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, message) -> {
         buffer.writeUtf(message.structureName, 64);
         buffer.writeInt(message.minX);
         buffer.writeInt(message.minY);
         buffer.writeInt(message.minZ);
         buffer.writeInt(message.maxX);
         buffer.writeInt(message.maxY);
         buffer.writeInt(message.maxZ);
      },
      buffer -> new SaveStructuralSelectionMessage(
         buffer.readUtf(64), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt()
      )
   );

   @NotNull
   public Type<SaveStructuralSelectionMessage> type() {
      return TYPE;
   }

   public static void handleData(SaveStructuralSelectionMessage message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(
               () -> {
                  if (context.player() instanceof ServerPlayer player) {
                     TypeMoonWorldModVariables.PlayerVariables var32 = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                        TypeMoonWorldModVariables.PLAYER_VARIABLES
                     );
                     String requestedName = message.structureName == null ? "" : message.structureName.trim();
                     if (requestedName.isEmpty()) {
                        player.displayClientMessage(Component.translatable("message.typemoonworld.structure.save_name_empty"), true);
                     } else {
                        if (requestedName.length() > 32) {
                           requestedName = requestedName.substring(0, 32);
                        }

                        int minX = Math.min(message.minX, message.maxX);
                        int minY = Math.min(message.minY, message.maxY);
                        int minZ = Math.min(message.minZ, message.maxZ);
                        int maxX = Math.max(message.minX, message.maxX);
                        int maxY = Math.max(message.minY, message.maxY);
                        int maxZ = Math.max(message.minZ, message.maxZ);
                        int sizeX = maxX - minX + 1;
                        int sizeY = maxY - minY + 1;
                        int sizeZ = maxZ - minZ + 1;
                        int maxSide = var32.player_magic_attributes_sword ? 32 : 16;
                        if (sizeX > 0 && sizeY > 0 && sizeZ > 0 && sizeX <= maxSide && sizeY <= maxSide && sizeZ <= maxSide) {
                           BlockPos min = new BlockPos(minX, minY, minZ);
                           BlockPos max = new BlockPos(maxX, maxY, maxZ);
                           List<TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock> blocks = new ArrayList<>();
                           ItemStack iconStack = ItemStack.EMPTY;
                           double totalCost = 0.0;

                           for (int x = min.getX(); x <= max.getX(); x++) {
                              for (int y = min.getY(); y <= max.getY(); y++) {
                                 for (int z = min.getZ(); z <= max.getZ(); z++) {
                                    BlockPos pos = new BlockPos(x, y, z);
                                    BlockState state = player.serverLevel().getBlockState(pos);
                                    if (!state.isAir()) {
                                       Item blockItem = state.getBlock().asItem();
                                       if (blockItem != Items.AIR) {
                                          ItemStack analyzedStack = blockItem.getDefaultInstance();
                                          analyzedStack.setCount(1);
                                          totalCost += MagicStructuralAnalysis.calculateStructureCost(analyzedStack, var32.player_magic_attributes_sword);
                                          String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                                          String blockStateProps = serializeBlockStateProperties(state);
                                          blocks.add(
                                             new TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock(
                                                x - min.getX(), y - min.getY(), z - min.getZ(), blockId, blockStateProps
                                             )
                                          );
                                          if (iconStack.isEmpty()) {
                                             iconStack = analyzedStack.copy();
                                          }
                                       }
                                    }
                                 }
                              }
                           }

                           if (blocks.isEmpty()) {
                              player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.no_target"), true);
                           } else if (blocks.size() > 8192) {
                              player.displayClientMessage(Component.translatable("message.typemoonworld.structure.invalid_range"), true);
                           } else {
                              int replacedIndex = -1;
                              int replacedBlockCount = 0;
                              String replacedStructureId = "";
                              int existingTotalBlocks = 0;

                              for (int i = 0; i < var32.analyzed_structures.size(); i++) {
                                 TypeMoonWorldModVariables.PlayerVariables.SavedStructure existing = var32.analyzed_structures.get(i);
                                 if (existing != null) {
                                    int existingBlocks = existing.blocks == null ? 0 : existing.blocks.size();
                                    existingTotalBlocks += existingBlocks;
                                    if (existing.name != null && existing.name.toLowerCase(Locale.ROOT).equals(requestedName.toLowerCase(Locale.ROOT))) {
                                       replacedIndex = i;
                                       replacedBlockCount = existingBlocks;
                                       replacedStructureId = existing.id == null ? "" : existing.id;
                                    }
                                 }
                              }

                              int predictedStructureCount = var32.analyzed_structures.size() + (replacedIndex == -1 ? 1 : 0);
                              int predictedTotalBlocks = existingTotalBlocks - replacedBlockCount + blocks.size();
                              if (predictedStructureCount > 48 || predictedTotalBlocks > 10000) {
                                 player.displayClientMessage(Component.translatable("message.typemoonworld.structure.invalid_range"), true);
                              } else if (!MagicStructuralAnalysis.consumeAnalysisManaOrFail(player, var32, totalCost)) {
                                 player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.failed"), true);
                              } else {
                                 TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = new TypeMoonWorldModVariables.PlayerVariables.SavedStructure();
                                 structure.id = UUID.randomUUID().toString();
                                 structure.name = requestedName;
                                 structure.source = "typemoonworld:analysis_v2";
                                 structure.sizeX = sizeX;
                                 structure.sizeY = sizeY;
                                 structure.sizeZ = sizeZ;
                                 structure.totalBlocks = blocks.size();
                                 structure.blocks.addAll(blocks);
                                 structure.icon = iconStack.isEmpty() ? new ItemStack(Items.STONE) : iconStack;
                                 if (replacedIndex >= 0) {
                                    var32.analyzed_structures.set(replacedIndex, structure);
                                 } else {
                                    var32.analyzed_structures.add(structure);
                                 }

                                 var32.projection_selected_structure_id = structure.id;
                                 var32.projection_selected_item = ItemStack.EMPTY;
                                 double progressGain = Math.max(0.2, Math.min(2.0, blocks.size() / 256.0));
                                 var32.proficiency_structural_analysis = Math.min(100.0, var32.proficiency_structural_analysis + progressGain);
                                 var32.syncMana(player);
                                 PacketDistributor.sendToPlayer(player, new TypeMoonWorldModVariables.ProficiencySyncMessage(var32), new CustomPacketPayload[0]);
                                 CompoundTag delta = new CompoundTag();
                                 delta.put("structure", structure.serializeNBT(player.registryAccess()));
                                 if (!replacedStructureId.isEmpty() && !replacedStructureId.equals(structure.id)) {
                                    delta.putString("replaced_structure_id", replacedStructureId);
                                 }

                                 delta.putString(
                                    "selected_structure_id", var32.projection_selected_structure_id == null ? "" : var32.projection_selected_structure_id
                                 );
                                 delta.putBoolean("clear_selected_item", true);
                                 var32.syncProjectionDelta(player, 0, delta);
                                 int roundedCost = (int)Math.ceil(totalCost);
                                 player.displayClientMessage(
                                    Component.translatable("message.typemoonworld.structure.saved", new Object[]{requestedName, roundedCost, 0}), true
                                 );
                              }
                           }
                        } else {
                           player.displayClientMessage(Component.translatable("message.typemoonworld.structure.invalid_range"), true);
                        }
                     }
                  }
               }
            )
            .exceptionally(e -> {
               TYPE_MOON_WORLD.LOGGER.error("Failed to save structural selection", e);
               if (context.player() != null) {
                  context.player().displayClientMessage(Component.translatable("message.typemoonworld.structure.invalid_range"), true);
               }

               return null;
            });
      }
   }

   private static String serializeBlockStateProperties(BlockState state) {
      if (state == null) {
         return "";
      } else {
         List<Property<?>> properties = new ArrayList<>(state.getProperties());
         properties.sort(Comparator.comparing(Property::getName));
         if (properties.isEmpty()) {
            return "";
         } else {
            StringBuilder sb = new StringBuilder();

            for (Property<?> property : properties) {
               if (sb.length() > 0) {
                  sb.append(',');
               }

               sb.append(property.getName()).append('=').append(getPropertyValueName(state, property));
            }

            return sb.toString();
         }
      }
   }

   private static String getPropertyValueName(BlockState state, Property<?> property) {
      Property rawProperty = (Property)property;
      return rawProperty.getName(state.getValue(rawProperty));
   }
}
