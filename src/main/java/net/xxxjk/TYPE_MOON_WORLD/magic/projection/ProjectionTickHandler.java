package net.xxxjk.TYPE_MOON_WORLD.magic.projection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;

import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;

import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class ProjectionTickHandler {

    // Thread-safe storage for active sessions
    private static final Map<GlobalPos, Long> projectedBlocks = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getLevel() instanceof Level level) {
             net.minecraft.resources.ResourceKey<Level> dim = level.dimension();
             projectedBlocks.keySet().removeIf(pos -> pos.dimension().equals(dim));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        
        Player player = event.getEntity();
        if (player.tickCount % 20 != 0) return; // Check every second
        
        long gameTime = player.level().getGameTime();
        
        // Check Inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            checkAndRemoveProjectedItem(stack, gameTime, player, i);
        }

        if (player.containerMenu != null) {
            if (player.containerMenu instanceof GrindstoneMenu) {
                boolean hasProjectedInput = false;
                if (player.containerMenu.slots.size() >= 3) {
                    ItemStack s0 = player.containerMenu.slots.get(0).getItem();
                    ItemStack s1 = player.containerMenu.slots.get(1).getItem();
                    hasProjectedInput = isProjectedOrInfinite(s0) || isProjectedOrInfinite(s1);
                    if (hasProjectedInput) {
                        ItemStack out = player.containerMenu.slots.get(2).getItem();
                        if (!out.isEmpty()) {
                            tagItemAsProjected(out, player);
                        }
                    }
                }
            } else if (player.containerMenu instanceof SmithingMenu) {
                boolean hasProjectedInput = false;
                if (player.containerMenu.slots.size() >= 4) {
                    ItemStack s0 = player.containerMenu.slots.get(0).getItem();
                    ItemStack s1 = player.containerMenu.slots.get(1).getItem();
                    ItemStack s2 = player.containerMenu.slots.get(2).getItem();
                    hasProjectedInput = isProjectedOrInfinite(s0) || isProjectedOrInfinite(s1) || isProjectedOrInfinite(s2);
                    if (hasProjectedInput) {
                        ItemStack out = player.containerMenu.slots.get(3).getItem();
                        if (!out.isEmpty()) {
                            tagItemAsProjected(out, player);
                        }
                    }
                }
            } else if (player.containerMenu instanceof StonecutterMenu) {
                boolean hasProjectedInput = false;
                if (player.containerMenu.slots.size() >= 2) {
                    ItemStack s0 = player.containerMenu.slots.get(0).getItem();
                    hasProjectedInput = isProjectedOrInfinite(s0);
                    if (hasProjectedInput) {
                        ItemStack out = player.containerMenu.slots.get(1).getItem();
                        if (!out.isEmpty()) {
                            tagItemAsProjected(out, player);
                        }
                    }
                }
            } else if (player.containerMenu instanceof AnvilMenu) {
                boolean hasProjectedInput = false;
                if (player.containerMenu.slots.size() >= 3) {
                    ItemStack s0 = player.containerMenu.slots.get(0).getItem();
                    ItemStack s1 = player.containerMenu.slots.get(1).getItem();
                    hasProjectedInput = isProjectedOrInfinite(s0) || isProjectedOrInfinite(s1);
                    if (hasProjectedInput) {
                        ItemStack out = player.containerMenu.slots.get(2).getItem();
                        if (!out.isEmpty()) {
                            tagItemAsProjected(out, player);
                        }
                    }
                }
            }
        }
    }
    
    private static void checkAndRemoveProjectedItem(ItemStack stack, long gameTime, Player player, int slotIndex) {
        if (stack.isEmpty()) return;
        if (!stack.has(DataComponents.CUSTOM_DATA)) return;
        
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return;
        CompoundTag tag = cd.copyTag();
        if (tag.contains("is_projected")) {
            if (tag.contains("is_infinite_projection")) return; // Skip removal for infinite projection
            
            long projTime = tag.getLong("projection_time");
            if (gameTime - projTime > 200) { // 10 seconds = 200 ticks
                player.getInventory().removeItem(stack);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack stack = player.getMainHandItem(); // Assuming placed with main hand or offhand. Event doesn't say which hand.
            // We should check both or assume main if it matches the block.
            // Actually, we can check if the placed block corresponds to a projected item?
            // But the item is consumed/decremented.
            // We can check the stack in hand BEFORE it was fully consumed?
            // Or check `event.getItemInHand()`.
            // EntityPlaceEvent doesn't have `getItemInHand()`.
            
            // We'll check active hands.
            boolean isProjected = false;
            for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                ItemStack s = player.getItemInHand(hand);
                if (s.has(DataComponents.CUSTOM_DATA)) {
                    CustomData cd = s.get(DataComponents.CUSTOM_DATA);
                    if (cd != null && cd.copyTag().contains("is_projected")) {
                        isProjected = true;
                        break;
                    }
                }
            }
            
            if (isProjected) {
                // We assume the placed block is the projected one.
                if (event.getLevel() instanceof Level level) {
                    // Check if it's infinite (we need to check the item stack again, we didn't save the infinite tag state above)
                    boolean isInfinite = false;
                    for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
                        ItemStack s = player.getItemInHand(hand);
                        if (s.has(DataComponents.CUSTOM_DATA)) {
                            CustomData cd = s.get(DataComponents.CUSTOM_DATA);
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
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getLevel() instanceof ServerLevel level) {
            GlobalPos pos = GlobalPos.of(level.dimension(), event.getPos());
            if (projectedBlocks.containsKey(pos)) {
                projectedBlocks.remove(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (itemEntity.level().isClientSide) return;
            
            ItemStack stack = itemEntity.getItem();
            if (stack.has(DataComponents.CUSTOM_DATA)) {
                CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
                if (cd != null) {
                    CompoundTag tag = cd.copyTag();
                    if (tag.contains("is_projected")) {
                        if (tag.contains("is_infinite_projection")) return; // Skip removal
                        
                        long projTime = tag.getLong("projection_time");
                        if (itemEntity.level().getGameTime() - projTime > 200) {
                            itemEntity.discard();
                            itemEntity.level().playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0f, 1.0f);
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
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        
        // Cleanup Map for this level
        Iterator<Map.Entry<GlobalPos, Long>> it = projectedBlocks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<GlobalPos, Long> entry = it.next();
            if (entry.getKey().dimension() == level.dimension()) {
                long placeTime = entry.getValue();
                if (level.getGameTime() - placeTime > 200) {
                    BlockPos pos = entry.getKey().pos();
                    // Check if the block is still the one we placed?
                    // Since we don't store the block state, we assume that if it wasn't broken (via BreakEvent), it's still there.
                    // However, we should check if it's AIR already to avoid particles on nothing.
                    if (!level.getBlockState(pos).isAir()) {
                        // Remove block (Set to Air)
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.5, 0.5, 0.5, 0.05);
                    }
                    it.remove();
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = event.getItemStack().get(DataComponents.CUSTOM_DATA);
            if (cd != null) {
                CompoundTag tag = cd.copyTag();
                if (tag.contains("is_projected")) {
                    if (tag.contains("is_infinite_projection")) {
                        event.getToolTip().add(net.minecraft.network.chat.Component.translatable(MagicConstants.MSG_PROJECTION_TOOLTIP_INFINITE).withStyle(net.minecraft.ChatFormatting.GOLD));
                    } else {
                        event.getToolTip().add(net.minecraft.network.chat.Component.translatable(MagicConstants.MSG_PROJECTION_TOOLTIP).withStyle(net.minecraft.ChatFormatting.AQUA));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(ItemCraftedEvent event) {
        boolean hasProjectedIngredient = false;
        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            ItemStack stack = event.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA)) {
                CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
                if (cd != null) {
                    CompoundTag tag = cd.copyTag();
                    if (tag.contains("is_projected") && !tag.contains("is_infinite_projection")) {
                        hasProjectedIngredient = true;
                        break;
                    }
                    if (tag.contains("is_infinite_projection")) {
                        hasProjectedIngredient = true;
                        break;
                    }
                }
            }
        }
        if (hasProjectedIngredient) {
            ItemStack result = event.getCrafting();
            if (!result.isEmpty()) {
                tagItemAsProjected(result, event.getEntity());
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        boolean hasProjectedInput = false;
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!left.isEmpty() && left.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = left.get(DataComponents.CUSTOM_DATA);
            if (cd != null) {
                CompoundTag tag = cd.copyTag();
                if (tag.contains("is_projected") || tag.contains("is_infinite_projection")) {
                    hasProjectedInput = true;
                }
            }
        }
        if (!hasProjectedInput && !right.isEmpty() && right.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = right.get(DataComponents.CUSTOM_DATA);
            if (cd != null) {
                CompoundTag tag = cd.copyTag();
                if (tag.contains("is_projected") || tag.contains("is_infinite_projection")) {
                    hasProjectedInput = true;
                }
            }
        }
        if (hasProjectedInput) {
            ItemStack out = event.getOutput();
            if (!out.isEmpty()) {
                tagItemAsProjected(out, event.getEntity());
            }
        }
    }

    private static void tagItemAsProjected(ItemStack stack, Player player) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("is_projected", true);
        tag.putLong("projection_time", player.level().getGameTime());
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        if (existing != null) {
            CompoundTag merged = existing.copyTag();
            merged.putBoolean("is_projected", true);
            merged.putLong("projection_time", player.level().getGameTime());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(merged));
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd != null) {
                CompoundTag cur = cd.copyTag();
                if (cur.contains("is_infinite_projection")) {
                    cur.remove("is_infinite_projection");
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(cur));
                }
            }
        }
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants.MSG_PROJECTION_TOOLTIP).withStyle(net.minecraft.ChatFormatting.AQUA), true);
    }

    private static boolean isProjectedOrInfinite(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!stack.has(DataComponents.CUSTOM_DATA)) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return false;
        CompoundTag tag = cd.copyTag();
        return tag.contains("is_projected") || tag.contains("is_infinite_projection");
    }
}
