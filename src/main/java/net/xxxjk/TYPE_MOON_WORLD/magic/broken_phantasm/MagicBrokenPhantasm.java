package net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.CustomData;

public class MagicBrokenPhantasm {
    private static void executeMode(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        ItemStack heldItem = player.getMainHandItem();
        boolean isMainHand = true;

        // Check Main Hand first
        if (!isValidBPItem(heldItem)) {
            ItemStack offhandItem = player.getOffhandItem();
            if (isValidBPItem(offhandItem)) {
                heldItem = offhandItem;
                isMainHand = false;
            } else {
                // No valid item found. Attempt auto-projection if hands are free.
                if (heldItem.isEmpty() || offhandItem.isEmpty()) {
                    net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicProjection.execute(player);
                } else {
                    player.displayClientMessage(Component.literal("手中没有投影物品或宝具"), true);
                }
                return;
            }
        }

        // Calculate Cost (for explosion power)
        double cost = calculateCost(heldItem, false);
        
        // Calculate Explosion Power
        // TNT has power 4.0
        // Cost 10 (Common) -> 0.5
        // Cost 50 (Uncommon) -> 2.5
        // Cost 100 (Rare) -> 5.0
        // Cost 300 (Epic) -> 15.0
        float explosionPower = (float)(cost / 20.0);
        
        // Cap explosion power to avoid server crashes (e.g. max 50)
        // NOTE: We pass the raw power now, and let the entity handle capping for effects.
        // if (explosionPower > 50.0f) explosionPower = 50.0f;

        // Spawn Projectile
        BrokenPhantasmProjectileEntity projectile = new BrokenPhantasmProjectileEntity(player.level(), player, heldItem.copy());
        projectile.setItem(heldItem.copy());
        projectile.setExplosionPower(explosionPower);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F); // Speed increased from 1.5F to 3.0F
        player.level().addFreshEntity(projectile);

        // Consume Item
        heldItem.shrink(1);
        
        player.displayClientMessage(Component.literal("投影崩坏！(威力: " + String.format("%.1f", explosionPower) + ")"), true);
    }

    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        executeMode(player, vars);
    }

    private static boolean isValidBPItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 1. Projected items can always be BP'd
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                if (tag.contains("is_projected")) return true;
            }
        }
        // 2. Noble Phantasms (except Avalon) can be BP'd
        if (stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem && 
            !(stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem)) {
            return true;
        }
        return false;
    }

    private static boolean isProjectedItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                return tag.contains("is_projected");
            }
        }
        return false;
    }

    public static double calculateCost(ItemStack stack, boolean hasSwordAttribute) {
        double baseCost = 10;
        
        // Noble Phantasms have a fixed base cost of 500
        if (stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem) {
            baseCost = 500;
        } else {
            Rarity rarity = stack.getRarity();
            if (rarity == Rarity.UNCOMMON) baseCost = 50;
            else if (rarity == Rarity.RARE) baseCost = 100;
            else if (rarity == Rarity.EPIC) baseCost = 300;
        }
        
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
        
        // Attack Damage Calculation
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double damage = modifiers.compute(0.0, EquipmentSlot.MAINHAND); 
        if (damage > 0) {
            // Add damage to cost (e.g., 1 damage = 5 mana)
            baseCost += damage * 5;
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
