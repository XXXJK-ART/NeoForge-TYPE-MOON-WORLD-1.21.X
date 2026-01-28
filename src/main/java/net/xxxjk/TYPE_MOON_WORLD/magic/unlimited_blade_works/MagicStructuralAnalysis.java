package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.component.CustomData;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicStructuralAnalysis {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        ItemStack heldItem = player.getMainHandItem();
        ItemStack targetItem = ItemStack.EMPTY;
        
        // 1. Check Held Item
        if (!heldItem.isEmpty()) {
            targetItem = heldItem;
        } else {
            // 2. Raytrace for Block or Entity
            HitResult hitResult = rayTrace(player, 5.0); // 5 blocks range
            
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockState state = player.level().getBlockState(blockHit.getBlockPos());
                targetItem = state.getBlock().asItem().getDefaultInstance();
            } else if (hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hitResult;
                if (entityHit.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
                    targetItem = itemEntity.getItem();
                } else if (entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                    ItemStack mainHand = livingEntity.getMainHandItem();
                    ItemStack offHand = livingEntity.getOffhandItem();
                    if (!mainHand.isEmpty()) targetItem = mainHand;
                    else if (!offHand.isEmpty()) targetItem = offHand;
                }
            }
        }

        if (targetItem.isEmpty()) {
             player.displayClientMessage(Component.literal("未发现可解析目标"), true);
             return;
        }

        // Process Target Item
        analyzeItem(player, vars, targetItem);
    }

    private static void analyzeItem(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ItemStack target) {
        // Check if already projected (prevent analyzing projected items)
        if (target.has(DataComponents.CUSTOM_DATA)) {
             CompoundTag tag = target.get(DataComponents.CUSTOM_DATA).copyTag();
             if (tag.contains("is_projected")) {
                 player.displayClientMessage(Component.translatable(MagicConstants.MSG_PROJECTION_CANNOT_ANALYZE_PROJECTED), true);
                 return;
             }
        }
        
        // Clean Item (Remove container contents)
        ItemStack toSave = target.copy();
        toSave.setCount(1);
        
        // Strip Container Data (Shulker Box, Bundle, etc.)
        // In 1.21, container data is stored in DataComponents.CONTAINER or DataComponents.BUNDLE_CONTENTS
        if (toSave.has(DataComponents.CONTAINER)) {
            toSave.remove(DataComponents.CONTAINER);
        }
        if (toSave.has(DataComponents.BUNDLE_CONTENTS)) {
            toSave.remove(DataComponents.BUNDLE_CONTENTS);
        }
        // Also check for legacy BlockEntityTag if any custom data remains
        if (toSave.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag customTag = toSave.get(DataComponents.CUSTOM_DATA).copyTag();
            if (customTag.contains("BlockEntityTag")) {
                CompoundTag bet = customTag.getCompound("BlockEntityTag");
                if (bet.contains("Items")) {
                    bet.remove("Items"); // Remove items from block entity tag
                    if (bet.isEmpty()) {
                        customTag.remove("BlockEntityTag");
                    } else {
                        customTag.put("BlockEntityTag", bet);
                    }
                    if (customTag.isEmpty()) {
                        toSave.remove(DataComponents.CUSTOM_DATA);
                    } else {
                        toSave.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
                    }
                }
            }
        }

        // Check if already analyzed
        boolean known = false;
        for (ItemStack s : vars.analyzed_items) {
             if (ItemStack.isSameItemSameComponents(s, toSave)) {
                 known = true;
                 break;
             }
        }
        
        if (!known) {
            double cost = calculateCost(toSave, vars.player_magic_attributes_sword);
            
            // Proficiency Logic
            double successRate = 0.5 + (vars.proficiency_structural_analysis * 0.005); // Base 50% + 0.5% per level (max 100%)
            if (vars.player_magic_attributes_sword && toSave.getItem() instanceof net.minecraft.world.item.SwordItem) {
                successRate = 1.0; // Sword attribute makes sword analysis 100% success
            }
            if (successRate > 1.0) successRate = 1.0;

            boolean success = player.getRandom().nextDouble() < successRate;

            if (success) {
                if (ManaHelper.consumeManaOrHealth(player, cost)) {
                    vars.analyzed_items.add(toSave);
                    vars.proficiency_structural_analysis = Math.min(100, vars.proficiency_structural_analysis + 0.5);
                    vars.syncPlayerVariables(player);
                    player.displayClientMessage(Component.translatable(MagicConstants.MSG_PROJECTION_ANALYSIS_COMPLETE, (int)cost), true);
                }
            } else {
                double failCost = cost * 0.3; // 30% cost on fail
                if (ManaHelper.consumeManaOrHealth(player, failCost)) {
                    vars.proficiency_structural_analysis = Math.min(100, vars.proficiency_structural_analysis + 0.1);
                    vars.syncPlayerVariables(player);
                    player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.failed"), true);
                }
            }
        } else {
            player.displayClientMessage(Component.translatable(MagicConstants.MSG_PROJECTION_ALREADY_ANALYZED), true);
        }
    }

    private static HitResult rayTrace(ServerPlayer player, double range) {
        float partialTicks = 1.0F;
        HitResult blockHit = player.pick(range, partialTicks, false);
        Vec3 eyePos = player.getEyePosition(partialTicks);
        double distToBlock = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation().distanceTo(eyePos) : range;
        
        Vec3 lookDir = player.getViewVector(partialTicks);
        Vec3 endPos = eyePos.add(lookDir.x * range, lookDir.y * range, lookDir.z * range);
        AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(range)).inflate(1.0D);
        
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eyePos, endPos, searchBox, (e) -> !e.isSpectator() && e.isPickable(), distToBlock * distToBlock);
        
        if (entityHit != null) {
            return entityHit;
        }
        return blockHit;
    }

    private static double calculateCost(ItemStack stack, boolean hasSwordAttribute) {
        double baseCost = 10;
        Rarity rarity = stack.getRarity();
        if (rarity == Rarity.UNCOMMON) baseCost = 50;
        else if (rarity == Rarity.RARE) baseCost = 100;
        else if (rarity == Rarity.EPIC) baseCost = 300;
        
        // Multipliers
        // Enchantments
        int enchantCount = stack.getEnchantments().size();
        if (enchantCount > 0) {
            baseCost *= (1 + enchantCount * 0.2); 
        }
        
        // Durability
        if (stack.getMaxDamage() > 0) {
             baseCost *= (1 + stack.getMaxDamage() / 1000.0);
        }
        
        // Block Hardness
        if (stack.getItem() instanceof BlockItem blockItem) {
             BlockState state = blockItem.getBlock().defaultBlockState();
             float hardness = state.getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
             if (hardness > 0) {
                 baseCost += hardness * 5; 
             }
        }
        
        // Sword Attribute Discount / Cost Modification
        if (hasSwordAttribute) {
            boolean isSword = stack.getItem() instanceof net.minecraft.world.item.SwordItem;
            if (isSword) {
                baseCost *= 0.1; // 10% cost for swords
            } else {
                baseCost *= 0.5; // 50% cost for other items
            }
        }
        
        return baseCost;
    }
}
