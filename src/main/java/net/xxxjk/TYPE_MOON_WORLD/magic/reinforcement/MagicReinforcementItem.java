package net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class MagicReinforcementItem {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.no_item"), true);
            return;
        }

        // Determine if reinforceable (Now includes almost anything)
        boolean isStandardReinforceable = heldItem.isDamageableItem() || heldItem.getItem() instanceof TieredItem || heldItem.getItem() instanceof ArmorItem || heldItem.getItem() instanceof BowItem;
        
        // Calculate Max Level based on Proficiency (1 level per 20 proficiency, max 5)
        int maxLevel = Math.min(5, 1 + (int)(vars.proficiency_reinforcement / 20));
        int requestLevel = vars.reinforcement_level == 0 ? 1 : vars.reinforcement_level;
        int level = Math.min(requestLevel, maxLevel);

        // Pre-check if anything would change to avoid wasting mana
        boolean wouldChange = false;
        if (!isStandardReinforceable) {
            wouldChange = true; // Non-standard items always get temporary stats
        } else {
            if (heldItem.isDamaged()) wouldChange = true;
            
            Registry<Enchantment> registry = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> enchantmentToApply = getEnchantmentForItem(heldItem, registry);
            if (enchantmentToApply != null) {
                int currentLevel = EnchantmentHelper.getEnchantmentsForCrafting(heldItem).getLevel(enchantmentToApply);
                if (level > currentLevel) wouldChange = true;
            }
        }

        if (!wouldChange) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.already_better"), true);
            return;
        }

        // Failure Chance Logic
        // Old: (100 - proficiency)% failure rate
        // New: Significantly reduced failure rate.
        // Base success rate starts high and scales with proficiency.
        // Let's say: 
        // Lvl 1: 90% base success
        // Lvl 5: 50% base success (harder to reinforce high levels)
        // Proficiency adds up to 40% success rate.
        
        // Example:
        // Lvl 1, Prof 0: 90% Success
        // Lvl 5, Prof 0: 50% Success
        // Lvl 5, Prof 100: 90% Success
        
        double baseSuccess = 1.0 - (level * 0.1); // 0.9 to 0.5
        double proficiencyBonus = (vars.proficiency_reinforcement / 100.0) * 0.4; // 0.0 to 0.4
        double successChance = Math.min(1.0, baseSuccess + proficiencyBonus);
        
        // Ensure at least 10% chance to fail at max level/min proficiency if we want difficulty,
        // but user complained it's too high.
        // Let's make it even easier:
        // Proficiency 0 -> 20% fail chance at level 1?
        // User said "failure probability is too high".
        // Previous logic: Prof 0 = 100% fail. Prof 50 = 50% fail.
        // That was indeed very harsh.
        
        // Revised Logic:
        // Fail Chance = (5 * Level) - (Proficiency / 2)
        // Lvl 1, Prof 0: 5% Fail
        // Lvl 1, Prof 100: 0% Fail
        // Lvl 5, Prof 0: 25% Fail
        // Lvl 5, Prof 50: 0% Fail
        
        double failChancePercent = (5.0 * level) - (vars.proficiency_reinforcement / 2.0);
        if (failChancePercent < 0) failChancePercent = 0;
        
        // Cap fail chance at 50% just in case
        failChancePercent = Math.min(50.0, failChancePercent);
        
        if (Math.random() * 100.0 < failChancePercent) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.failed"), true);
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            
            // Still increase proficiency on failure (learning from mistakes)
            // Maybe less than success? Or same?
            // Let's give small amount (0.2)
            vars.proficiency_reinforcement = Math.min(100.0, vars.proficiency_reinforcement + 0.2);
            vars.syncPlayerVariables(player);
            
            return; // Fail before mana consumption
        }

        // Mana Cost
        double cost = 50.0 * level;
        if (!ManaHelper.consumeManaOrHealth(player, cost)) {
            return;
        }

        boolean didSomething = false;

        // Apply Custom Reinforcement Data (60s expiry)
        net.minecraft.nbt.CompoundTag tag = heldItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("Reinforced", true);
        tag.putInt("ReinforcedLevel", level);
        tag.putLong("ReinforcementTime", player.level().getGameTime());
        tag.putInt("ReinforcementExpiry", 600); // 30 seconds (20 ticks/s)
        tag.putUUID("CasterUUID", player.getUUID());
        
        if (!isStandardReinforceable) {
            // Non-weapon/tool reinforcement (e.g. stick, paper, etc.)
            
            // Damage Scaling:
            // Level 1: 4.0 (Wooden Sword)
            // Level 2: 5.0 (Stone Sword)
            // Level 3: 6.0 (Iron Sword)
            // Level 4: 7.0 (Diamond Sword - 1) - Wait, Diamond is 7.
            // Level 5: 7.0 (Diamond Sword) or 8.0 (Netherite)?
            // User request:
            // Level 1 -> Wood (4.0)
            // ...
            // Level 5 -> Diamond (7.0)
            
            // Base Hand Damage is 1.0. 
            // Wooden Sword adds +3 (Total 4).
            // Stone Sword adds +4 (Total 5).
            // Iron Sword adds +5 (Total 6).
            // Diamond Sword adds +6 (Total 7).
            // Netherite Sword adds +7 (Total 8).
            
            // So we need to ADD: 3.0 + (level - 1) * 0.75? 
            // Let's just map it manually for precision.
            double addedDamage = 3.0; // Level 1 (Wood)
            if (level == 2) addedDamage = 4.0; // Stone
            else if (level == 3) addedDamage = 5.0; // Iron
            else if (level == 4) addedDamage = 5.5; // Between Iron and Diamond
            else if (level >= 5) addedDamage = 6.0; // Diamond
            
            net.minecraft.world.item.component.ItemAttributeModifiers.Builder builder = net.minecraft.world.item.component.ItemAttributeModifiers.builder();
            // Copy existing modifiers if any
            net.minecraft.world.item.component.ItemAttributeModifiers existingModifiers = heldItem.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (existingModifiers != null) {
                for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry entry : existingModifiers.modifiers()) {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                }
            }
            
            // Add temporary attack damage
            builder.add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, "reinforcement_item_damage"),
                    addedDamage, 
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                ), 
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND
            );
            
            heldItem.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
            
            tag.putBoolean("ReinforcementTemporary", true);
            tag.putInt("ReinforcementHitsLeft", 3); // Fixed 3 hits for non-weapon items
            didSomething = true;
        } else {
            // Standard Item Reinforcement (Enchantment based)
            // Apply Efficiency/Sharpness/Protection based on item type
            Registry<Enchantment> registry = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> enchantmentToApply = getEnchantmentForItem(heldItem, registry);
            
            if (enchantmentToApply != null) {
                // Store original level if needed? Or just overwrite?
                // Instruction says "temporary", so we should probably revert it later.
                // For now, let's just apply it and rely on expiry to remove it.
                // But EnchantmentHelper.updateEnchantments modifies the stack permanently unless we track it.
                
                int currentLevel = EnchantmentHelper.getEnchantmentsForCrafting(heldItem).getLevel(enchantmentToApply);
                if (level > currentLevel) {
                    tag.putString("ReinforcedEnchantment", enchantmentToApply.unwrapKey().get().location().toString());
                    tag.putInt("ReinforcedEnchantmentLevel", level - currentLevel); // Store how much we added
                    
                    EnchantmentHelper.updateEnchantments(heldItem, mutable -> {
                        mutable.set(enchantmentToApply, level);
                    });
                    didSomething = true;
                }
                
                // Repair item slightly
                if (heldItem.isDamaged()) {
                    heldItem.setDamageValue(Math.max(0, heldItem.getDamageValue() - (level * 50)));
                    didSomething = true;
                }
            }
        }
        
        heldItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        if (didSomething) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.temporary_success"), true);
            
            // Play Magic Sound (Enchanting Table / Beacon) instead of Anvil
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            
            // Particle Effects
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
            }
        } else {
            // Should be caught by wouldChange check, but just in case
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.no_effect"), true);
        }
        
        // Increase Proficiency
        vars.proficiency_reinforcement = Math.min(100.0, vars.proficiency_reinforcement + 0.5);
        vars.syncPlayerVariables(player);
    }

    private static Holder<Enchantment> getEnchantmentForItem(ItemStack stack, Registry<Enchantment> registry) {
        if (stack.getItem() instanceof SwordItem) {
            return registry.getHolderOrThrow(Enchantments.SHARPNESS);
        } else if (stack.getItem() instanceof ArmorItem) {
            return registry.getHolderOrThrow(Enchantments.PROTECTION);
        } else if (stack.getItem() instanceof DiggerItem) { // Tools
            return registry.getHolderOrThrow(Enchantments.EFFICIENCY);
        } else if (stack.getItem() instanceof BowItem) {
            return registry.getHolderOrThrow(Enchantments.POWER);
        } else if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) {
            return registry.getHolderOrThrow(Enchantments.QUICK_CHARGE); // Crossbow: Quick Charge
        } else if (stack.getItem() instanceof net.minecraft.world.item.TridentItem) {
            return registry.getHolderOrThrow(Enchantments.IMPALING); // Trident: Impaling
        } else if (stack.getItem() instanceof net.minecraft.world.item.MaceItem) {
            return registry.getHolderOrThrow(Enchantments.DENSITY); // Mace (Hammer): Density
        } else if (stack.getItem() instanceof net.minecraft.world.item.ShieldItem) {
            return registry.getHolderOrThrow(Enchantments.UNBREAKING); // Shield: Unbreaking
        }
        return null;
    }
}
