
package net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class MagicReinforcementEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // Check all items in inventory for reinforcement expiry
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA)) {
                checkReinforcementExpiry(player, stack);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getDirectEntity() instanceof Player player && !player.level().isClientSide()) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.has(DataComponents.CUSTOM_DATA)) {
                net.minecraft.nbt.CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
                if (tag.getBoolean("ReinforcementTemporary")) {
                    int hits = tag.getInt("ReinforcementHitsLeft");
                    hits--;
                    if (hits <= 0) {
                        // Destroy item
                        stack.shrink(1);
                        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.destroyed"), true);
                    } else {
                        tag.putInt("ReinforcementHitsLeft", hits);
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.hits_left", hits), true);
                    }
                }
            }
        }
    }

    private static void checkReinforcementExpiry(Player player, ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        if (tag.getBoolean("Reinforced")) {
            long startTime = tag.getLong("ReinforcementTime");
            int expiry = tag.getInt("ReinforcementExpiry");
            long currentTime = player.level().getGameTime();

            if (currentTime - startTime >= expiry) {
                removeReinforcement(player, stack, tag);
            }
        }
    }

    public static void removeReinforcement(Player player, ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
        // Remove Enchantments if any
        if (tag.contains("ReinforcedEnchantment")) {
            String enchantmentId = tag.getString("ReinforcedEnchantment");
            int levelToRemove = tag.getInt("ReinforcedEnchantmentLevel");
            
            player.registryAccess().registry(net.minecraft.core.registries.Registries.ENCHANTMENT).ifPresent(registry -> {
                net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.parse(enchantmentId);
                registry.getHolder(rl).ifPresent(holder -> {
                    EnchantmentHelper.updateEnchantments(stack, mutable -> {
                        int currentLevel = mutable.getLevel(holder);
                        if (currentLevel <= levelToRemove) {
                            mutable.set(holder, 0); // Remove it
                        } else {
                            // If current level is higher (maybe player enchanted it manually?), we keep it or reduce it?
                            // The instruction says "寮哄寲娑堝け", so we should at least remove the part we added.
                            mutable.set(holder, currentLevel - levelToRemove);
                        }
                    });
                });
            });
        }

        // Remove Attribute Modifiers
        if (stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
            net.minecraft.world.item.component.ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            net.minecraft.world.item.component.ItemAttributeModifiers.Builder builder = net.minecraft.world.item.component.ItemAttributeModifiers.builder();
            
            boolean found = false;
            for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (!entry.modifier().id().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, "reinforcement_item_damage"))) {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                } else {
                    found = true;
                }
            }
            if (found) {
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
            }
        }

        // Clear Metadata
        boolean injectedGlint = tag.getBoolean("ReinforcementInjectedGlint");
        tag.remove("Reinforced");
        tag.remove("ReinforcedLevel");
        tag.remove("ReinforcementTime");
        tag.remove("ReinforcementExpiry");
        tag.remove("ReinforcementTemporary");
        tag.remove("ReinforcementHitsLeft");
        tag.remove("ReinforcedEnchantment");
        tag.remove("ReinforcedEnchantmentLevel");
        tag.remove("ReinforcementInjectedGlint");
        tag.remove("CasterUUID");

        if (injectedGlint && !shouldKeepForcedGlint(stack, tag)) {
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
        
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.item.expired"), true);
    }

    private static boolean shouldKeepForcedGlint(ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
        if (tag.contains("is_projected") || tag.contains("is_infinite_projection")) {
            return true;
        }
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return enchantments.size() > 0;
    }
}
