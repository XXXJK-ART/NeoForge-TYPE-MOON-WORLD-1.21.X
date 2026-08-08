package net.xxxjk.TYPE_MOON_WORLD.magic.projection;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class ProjectionTickHandler {
   private static final Map<GlobalPos, Long> projectedBlocks = new ConcurrentHashMap<>();

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      if (!event.getLevel().isClientSide()) {
         if (event.getLevel() instanceof Level level) {
            ResourceKey<Level> dim = level.dimension();
            projectedBlocks.keySet().removeIf(pos -> pos.dimension().equals(dim));
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(Post event) {
      if (!event.getEntity().level().isClientSide) {
         Player player = event.getEntity();
         if (player.tickCount % 20 == 0) {
            long gameTime = player.level().getGameTime();

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
               ItemStack stack = player.getInventory().getItem(i);
               checkAndRemoveProjectedItem(stack, gameTime, player, i);
            }

            if (player.containerMenu != null) {
               if (player.containerMenu instanceof GrindstoneMenu) {
                  boolean hasProjectedInput = false;
                  if (player.containerMenu.slots.size() >= 3) {
                     ItemStack s0 = ((Slot)player.containerMenu.slots.get(0)).getItem();
                     ItemStack s1 = ((Slot)player.containerMenu.slots.get(1)).getItem();
                     hasProjectedInput = isProjectedOrInfinite(s0) || isProjectedOrInfinite(s1);
                     if (hasProjectedInput) {
                        ItemStack out = ((Slot)player.containerMenu.slots.get(2)).getItem();
                        if (!out.isEmpty()) {
                           tagItemAsProjected(out, player, isInfiniteProjection(s0) || isInfiniteProjection(s1));
                        }
                     }
                  }
               } else if (player.containerMenu instanceof SmithingMenu) {
                  boolean hasProjectedInput = false;
                  if (player.containerMenu.slots.size() >= 4) {
                     ItemStack s0 = ((Slot)player.containerMenu.slots.get(0)).getItem();
                     ItemStack s1 = ((Slot)player.containerMenu.slots.get(1)).getItem();
                     ItemStack s2 = ((Slot)player.containerMenu.slots.get(2)).getItem();
                     hasProjectedInput = isProjectedOrInfinite(s0) || isProjectedOrInfinite(s1) || isProjectedOrInfinite(s2);
                     if (hasProjectedInput) {
                        ItemStack out = ((Slot)player.containerMenu.slots.get(3)).getItem();
                        if (!out.isEmpty()) {
                           tagItemAsProjected(out, player, isInfiniteProjection(s0) || isInfiniteProjection(s1) || isInfiniteProjection(s2));
                        }
                     }
                  }
               } else if (player.containerMenu instanceof StonecutterMenu) {
                  boolean hasProjectedInput = false;
                  if (player.containerMenu.slots.size() >= 2) {
                     ItemStack s0 = ((Slot)player.containerMenu.slots.get(0)).getItem();
                     hasProjectedInput = isProjectedOrInfinite(s0);
                     if (hasProjectedInput) {
                        ItemStack out = ((Slot)player.containerMenu.slots.get(1)).getItem();
                        if (!out.isEmpty()) {
                           tagItemAsProjected(out, player, isInfiniteProjection(s0));
                        }
                     }
                  }
               } else if (player.containerMenu instanceof AnvilMenu) {
                  boolean hasProjectedInput = false;
                  if (player.containerMenu.slots.size() >= 3) {
                     ItemStack s0 = ((Slot)player.containerMenu.slots.get(0)).getItem();
                     ItemStack s1 = ((Slot)player.containerMenu.slots.get(1)).getItem();
                     hasProjectedInput = isProjectedOrInfinite(s0) || isProjectedOrInfinite(s1);
                     if (hasProjectedInput) {
                        ItemStack out = ((Slot)player.containerMenu.slots.get(2)).getItem();
                        if (!out.isEmpty()) {
                           tagItemAsProjected(out, player, isInfiniteProjection(s0) || isInfiniteProjection(s1));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void checkAndRemoveProjectedItem(ItemStack stack, long gameTime, Player player, int slotIndex) {
      if (!stack.isEmpty()) {
         if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
            if (cd != null) {
               CompoundTag tag = cd.copyTag();
               if (tag.contains("is_projected")) {
                  if (tag.contains("is_infinite_projection")) {
                     return;
                  }

                  long projTime = tag.getLong("projection_time");
                  if (gameTime - projTime > 200L) {
                     player.getInventory().removeItem(stack);
                     player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onBlockPlace(EntityPlaceEvent event) {
      if (event.getEntity() instanceof Player player) {
         ItemStack stack = player.getMainHandItem();
         boolean isProjected = false;

         for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (s.has(DataComponents.CUSTOM_DATA)) {
               CustomData cd = (CustomData)s.get(DataComponents.CUSTOM_DATA);
               if (cd != null && cd.copyTag().contains("is_projected")) {
                  isProjected = true;
                  break;
               }
            }
         }

         if (isProjected && event.getLevel() instanceof Level level) {
            boolean isInfinite = false;

            for (InteractionHand handx : InteractionHand.values()) {
               ItemStack s = player.getItemInHand(handx);
               if (s.has(DataComponents.CUSTOM_DATA)) {
                  CustomData cd = (CustomData)s.get(DataComponents.CUSTOM_DATA);
                  if (cd != null) {
                     CompoundTag tag = cd.copyTag();
                     if (tag.contains("is_projected") && tag.contains("is_infinite_projection")) {
                        isInfinite = true;
                        break;
                     }
                  }
               }
            }

            if (!isInfinite) {
               GlobalPos pos = GlobalPos.of(level.dimension(), event.getPos());
               projectedBlocks.put(pos, level.getGameTime());
               if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                  ProjectionEffectHelper.spawnBlockPlace(serverLevel, event.getPos());
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onBlockBreak(BreakEvent event) {
      if (!event.getLevel().isClientSide()) {
         if (event.getLevel() instanceof ServerLevel level) {
            GlobalPos pos = GlobalPos.of(level.dimension(), event.getPos());
            if (projectedBlocks.remove(pos) != null) {
               ProjectionEffectHelper.spawnBlockBreak(level, event.getPos());
               event.setCanceled(true);
               level.setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 2);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
      if (event.getEntity() instanceof ItemEntity itemEntity) {
         if (itemEntity.level().isClientSide) {
            return;
         }
         if (itemEntity.tickCount % 10 != 0) {
            return;
         }

         ItemStack stack = itemEntity.getItem();
         if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
            if (cd != null) {
               CompoundTag tag = cd.copyTag();
               if (tag.contains("is_projected")) {
                  if (tag.contains("is_infinite_projection")) {
                     return;
                  }

                  long projTime = tag.getLong("projection_time");
                  if (itemEntity.level().getGameTime() - projTime > 200L) {
                     itemEntity.discard();
                     itemEntity.level()
                        .playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                     if (itemEntity.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.POOF, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
      if (!event.getLevel().isClientSide) {
         ServerLevel level = (ServerLevel)event.getLevel();
         if (level.getGameTime() % 20 != 0) {
            return;
         }
         Iterator<Entry<GlobalPos, Long>> it = projectedBlocks.entrySet().iterator();

         while (it.hasNext()) {
            Entry<GlobalPos, Long> entry = it.next();
            if (entry.getKey().dimension().equals(level.dimension())) {
               long placeTime = entry.getValue();
               if (level.getGameTime() - placeTime > 200L) {
                  BlockPos pos = entry.getKey().pos();
                  if (!level.getBlockState(pos).isAir()) {
                     ProjectionEffectHelper.spawnBlockBreak(level, pos);
                     level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                     level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                     level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.05);
                  }

                  it.remove();
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onItemTooltip(ItemTooltipEvent event) {
      if (event.getItemStack().has(DataComponents.CUSTOM_DATA)) {
         CustomData cd = (CustomData)event.getItemStack().get(DataComponents.CUSTOM_DATA);
         if (cd != null) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains("is_projected")) {
               if (tag.contains("is_infinite_projection")) {
                  event.getToolTip().add(Component.translatable("message.typemoonworld.projection.tooltip.infinite").withStyle(ChatFormatting.GOLD));
               } else {
                  event.getToolTip().add(Component.translatable("message.typemoonworld.projection.tooltip").withStyle(ChatFormatting.AQUA));
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onItemCrafted(ItemCraftedEvent event) {
      List<ItemStack> inputs = new ArrayList<>();
      for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
         ItemStack stack = event.getInventory().getItem(i);
         if (ProjectionDataHelper.isProjected(stack)) inputs.add(stack);
      }
      if (!inputs.isEmpty()) {
         ItemStack result = event.getCrafting();
         ProjectionDataHelper.inheritFinite(result, inputs, event.getEntity().level().getGameTime());
      }
   }

   @SubscribeEvent
   public static void onAnvilRepair(AnvilRepairEvent event) {
      ItemStack left = event.getLeft();
      ItemStack right = event.getRight();
      ProjectionDataHelper.inheritFinite(event.getOutput(), List.of(left, right), event.getEntity().level().getGameTime());
   }

   private static void tagItemAsProjected(ItemStack stack, Player player, boolean infiniteInput) {
      CompoundTag tag = new CompoundTag();
      tag.putBoolean("is_projected", true);
      long existingTime = ProjectionDataHelper.projectionTime(stack);
      tag.putLong("projection_time", existingTime == Long.MIN_VALUE ? player.level().getGameTime() : existingTime);
      if (infiniteInput) tag.putBoolean("is_infinite_projection", true);
      CustomData existing = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (existing != null) {
         CompoundTag merged = existing.copyTag();
         merged.putBoolean("is_projected", true);
         merged.putLong("projection_time", existingTime == Long.MIN_VALUE ? player.level().getGameTime() : existingTime);
         if (infiniteInput) merged.putBoolean("is_infinite_projection", true);
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(merged));
      } else {
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }

      if (stack.has(DataComponents.CUSTOM_DATA)) {
         CustomData cd = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (cd != null) {
            CompoundTag cur = cd.copyTag();
            if (cur.contains("is_infinite_projection") && !infiniteInput) {
               cur.remove("is_infinite_projection");
               stack.set(DataComponents.CUSTOM_DATA, CustomData.of(cur));
            }
         }
      }

      stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      player.displayClientMessage(Component.translatable("message.typemoonworld.projection.tooltip").withStyle(ChatFormatting.AQUA), true);
   }

   private static boolean isInfiniteProjection(ItemStack stack) {
      return ProjectionDataHelper.isProjected(stack) && ProjectionDataHelper.isInfinite(stack);
   }

   private static boolean isProjectedOrInfinite(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else if (!stack.has(DataComponents.CUSTOM_DATA)) {
         return false;
      } else {
         CustomData cd = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (cd == null) {
            return false;
         } else {
            CompoundTag tag = cd.copyTag();
            return tag.contains("is_projected") || tag.contains("is_infinite_projection");
         }
      }
   }
}
