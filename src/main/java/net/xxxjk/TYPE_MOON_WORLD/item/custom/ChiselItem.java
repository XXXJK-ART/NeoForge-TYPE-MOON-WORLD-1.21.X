package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import org.jetbrains.annotations.NotNull;
import java.util.Random;

import net.minecraft.world.item.Items;

public class ChiselItem extends Item {
    private final Random random = new Random();

    public ChiselItem(Properties properties) {
        super(properties);
    }
    
    // ... (keep crafting methods) ...

    @Override
    public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(ItemStack itemstack) {
        ItemStack retrieval = new ItemStack(this);
        retrieval.setDamageValue(itemstack.getDamageValue() + 1);
        return retrieval.getDamageValue() >= retrieval.getMaxDamage() ? ItemStack.EMPTY : retrieval;
    }

    @Override
    public boolean isRepairable(@NotNull ItemStack itemstack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack chiselStack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            ItemStack offhandStack = player.getOffhandItem();
            if (offhandStack.isEmpty()) {
                return InteractionResultHolder.pass(chiselStack);
            }
            
            Item rawItem = offhandStack.getItem();
            ItemStack resultGem = ItemStack.EMPTY;
            int count = 1; // Default count
            
            // Raw Gems Logic
            if (rawItem == ModItems.RAW_EMERALD.get()) {
                resultGem = getRandomQualityGem(ModItems.CARVED_EMERALD_POOR.get(), ModItems.CARVED_EMERALD.get(), ModItems.CARVED_EMERALD_HIGH.get());
            } else if (rawItem == ModItems.RAW_RUBY.get()) {
                resultGem = getRandomQualityGem(ModItems.CARVED_RUBY_POOR.get(), ModItems.CARVED_RUBY.get(), ModItems.CARVED_RUBY_HIGH.get());
            } else if (rawItem == ModItems.RAW_SAPPHIRE.get()) {
                resultGem = getRandomQualityGem(ModItems.CARVED_SAPPHIRE_POOR.get(), ModItems.CARVED_SAPPHIRE.get(), ModItems.CARVED_SAPPHIRE_HIGH.get());
            } else if (rawItem == ModItems.RAW_TOPAZ.get()) {
                resultGem = getRandomQualityGem(ModItems.CARVED_TOPAZ_POOR.get(), ModItems.CARVED_TOPAZ.get(), ModItems.CARVED_TOPAZ_HIGH.get());
            } else if (rawItem == ModItems.RAW_WHITE_GEMSTONE.get()) {
                resultGem = getRandomQualityGem(ModItems.CARVED_WHITE_GEMSTONE_POOR.get(), ModItems.CARVED_WHITE_GEMSTONE.get(), ModItems.CARVED_WHITE_GEMSTONE_HIGH.get());
            } else if (rawItem == ModItems.RAW_CYAN_GEMSTONE.get()) {
                resultGem = getRandomQualityGem(ModItems.CARVED_CYAN_GEMSTONE_POOR.get(), ModItems.CARVED_CYAN_GEMSTONE.get(), ModItems.CARVED_CYAN_GEMSTONE_HIGH.get());
            }
            // Vanilla Mapping
            // Diamond -> White Gemstone
            else if (rawItem == Items.DIAMOND) {
                resultGem = getRandomQualityGem(ModItems.CARVED_WHITE_GEMSTONE_POOR.get(), ModItems.CARVED_WHITE_GEMSTONE.get(), ModItems.CARVED_WHITE_GEMSTONE_HIGH.get());
            }
            // Emerald -> Green Gemstone
            else if (rawItem == Items.EMERALD) {
                resultGem = getRandomQualityGem(ModItems.CARVED_EMERALD_POOR.get(), ModItems.CARVED_EMERALD.get(), ModItems.CARVED_EMERALD_HIGH.get());
            }
            // Redstone Block -> 9x Red Gemstone
            else if (rawItem == Items.REDSTONE_BLOCK) {
                resultGem = getRandomQualityGem(ModItems.CARVED_RUBY_POOR.get(), ModItems.CARVED_RUBY.get(), ModItems.CARVED_RUBY_HIGH.get());
                resultGem.setCount(9);
            }
            // Glowstone Dust -> Yellow Gemstone
            else if (rawItem == Items.GLOWSTONE_DUST) {
                resultGem = getRandomQualityGem(ModItems.CARVED_TOPAZ_POOR.get(), ModItems.CARVED_TOPAZ.get(), ModItems.CARVED_TOPAZ_HIGH.get());
            }
            // Lapis Lazuli -> Cyan Gemstone or Sapphire (50/50)
            else if (rawItem == Items.LAPIS_LAZULI) {
                if (random.nextBoolean()) {
                    resultGem = getRandomQualityGem(ModItems.CARVED_SAPPHIRE_POOR.get(), ModItems.CARVED_SAPPHIRE.get(), ModItems.CARVED_SAPPHIRE_HIGH.get());
                } else {
                    resultGem = getRandomQualityGem(ModItems.CARVED_CYAN_GEMSTONE_POOR.get(), ModItems.CARVED_CYAN_GEMSTONE.get(), ModItems.CARVED_CYAN_GEMSTONE_HIGH.get());
                }
            }
            
            if (!resultGem.isEmpty()) {
                // Success
                offhandStack.shrink(1);
                
                // Damage Chisel
                chiselStack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                
                // Give Item
                if (!player.getInventory().add(resultGem)) {
                    player.drop(resultGem, false);
                }
                
                // Sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.PLAYERS, 1.0f, 1.0f);
                
                // Add 0.1s cooldown (2 ticks)
                player.getCooldowns().addCooldown(this, 2);
                
                return InteractionResultHolder.success(chiselStack);
            }
        }
        
        return InteractionResultHolder.pass(chiselStack);
    }
    
    private ItemStack getRandomQualityGem(Item poor, Item normal, Item high) {
        // Probabilities: 10% Poor, 60% Normal, 30% High
        int roll = random.nextInt(100);
        if (roll < 10) {
            return new ItemStack(poor);
        } else if (roll < 70) {
            return new ItemStack(normal);
        } else {
            return new ItemStack(high);
        }
    }
}
