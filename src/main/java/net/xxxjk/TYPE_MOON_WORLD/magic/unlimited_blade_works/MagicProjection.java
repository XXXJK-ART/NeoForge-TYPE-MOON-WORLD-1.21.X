package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenProjectionGuiMessage;
import net.minecraft.nbt.CompoundTag;

import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicProjection {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        // Check for Shift Key (Sneaking)
        if (player.isShiftKeyDown()) {
            PacketDistributor.sendToPlayer(player, new OpenProjectionGuiMessage());
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        ItemStack offhandItem = player.getOffhandItem();
        
        // Projection Logic
        boolean canProject = false;
        net.minecraft.world.InteractionHand handToUse = null;

        if (heldItem.isEmpty()) {
            canProject = true;
            handToUse = net.minecraft.world.InteractionHand.MAIN_HAND;
        } else if (offhandItem.isEmpty()) {
            canProject = true;
            handToUse = net.minecraft.world.InteractionHand.OFF_HAND;
        }

        if (canProject) {
            ItemStack target = vars.projection_selected_item;
            if (target.isEmpty()) {
                player.displayClientMessage(Component.translatable(MagicConstants.MSG_PROJECTION_NO_TARGET), true);
                return;
            }
            
            double cost = calculateCost(target, vars.player_magic_attributes_sword, vars.proficiency_projection);
            if (ManaHelper.consumeManaOrHealth(player, cost)) {
                
                // Proficiency Increase
                vars.proficiency_projection = Math.min(100, vars.proficiency_projection + 0.2);
                vars.syncPlayerVariables(player);
                
                // TODO: Special Effect for Sword Attribute
                if (vars.player_magic_attributes_sword) {
                     // Placeholder for different effects when player has sword attribute
                }
                
                ItemStack projected = target.copy();
                projected.setCount(1);
                
                // Set Durability
                if (projected.isDamageableItem()) {
                    if (vars.player_magic_attributes_sword) {
                        boolean isSword = projected.getItem() instanceof net.minecraft.world.item.SwordItem;
                        int maxDmg = projected.getMaxDamage();
                        if (isSword) {
                            // 2/3 Durability remaining (damage = 1/3)
                            // damageValue is "damage taken". So set to 1/3 of max.
                            projected.setDamageValue((int)(maxDmg * 0.333));
                        } else {
                            // 1/10 Durability remaining (damage = 9/10)
                            projected.setDamageValue((int)(maxDmg * 0.9));
                        }
                    } else {
                        // Default: 1 Durability remaining
                        projected.setDamageValue(projected.getMaxDamage() - 1);
                    }
                }
                
                // Add Tag
                CompoundTag tag = new CompoundTag();
                tag.putBoolean("is_projected", true);
                if (vars.player_magic_attributes_sword) {
                    tag.putBoolean("is_infinite_projection", true);
                }
                tag.putLong("projection_time", player.level().getGameTime());
                
                // Merge with existing custom data if any
                CustomData existingData = projected.get(DataComponents.CUSTOM_DATA);
                if (existingData != null) {
                    CompoundTag existingTag = existingData.copyTag();
                    existingTag.merge(tag);
                    projected.set(DataComponents.CUSTOM_DATA, CustomData.of(existingTag));
                } else {
                    projected.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
                
                // Add Glint
                projected.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                
                player.setItemInHand(handToUse, projected);
                vars.syncPlayerVariables(player);
                player.displayClientMessage(Component.literal("Trace On"), true);
            }
        } else {
            // If both hands are not empty
            player.displayClientMessage(Component.translatable(MagicConstants.MSG_PROJECTION_HANDS_FULL), true);
        }
    }
    
    private static double calculateCost(ItemStack stack, boolean hasSwordAttribute, double proficiency) {
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
        
        // Proficiency Discount (up to 50% reduction)
        // 0 proficiency = 0% reduction
        // 100 proficiency = 50% reduction
        // cost = cost * (1 - (proficiency * 0.005))
        baseCost *= (1.0 - (proficiency * 0.005));
        
        return baseCost;
    }
}
