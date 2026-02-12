package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class CarvedGemItem extends Item {
    private final GemQuality quality;
    private final Supplier<Item> fullGemSupplier;

    public CarvedGemItem(Properties properties, GemType type, GemQuality quality, Supplier<Item> fullGemSupplier) {
        super(properties);
        this.quality = quality;
        this.fullGemSupplier = fullGemSupplier;
    }

    private Item fullGemItem() {
        return fullGemSupplier.get();
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return false;
    }
    
    public GemQuality getQuality() {
        return quality;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            double manaAmount = quality.getCapacity();
            
            // Check if mana is sufficient
            if (vars.player_mana >= manaAmount) {
                vars.player_mana -= manaAmount;
                vars.syncMana(player);
                giveFullGem(player, hand, stack, world);
            } else {
                // Auto Health Conversion
                // Logic: 1 HP (0.5 heart) -> 10 Mana. (Based on Manually_deduct_health_to_restore_mana)
                double deficit = manaAmount - vars.player_mana;
                float healthCost = (float) Math.ceil(deficit / 10.0); // 1 HP per 10 Mana
                
                if (player.getHealth() > healthCost) {
                    player.setHealth(player.getHealth() - healthCost);
                    
                    // Consume all remaining mana
                    vars.player_mana = 0; // Technically it becomes negative then filled by HP, ending at exactly 0 relative to cost. 
                                          // Or simpler: we paid the mana cost using all mana + some health.
                    vars.syncMana(player);
                    
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u00A7c魔力不足！消耗了 " + (int)healthCost + " 点生命值来补充魔力。"), true);
                    
                    giveFullGem(player, hand, stack, world);
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("魔力与生命力均不足！"), true);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    private void giveFullGem(Player player, InteractionHand hand, ItemStack stack, Level world) {
        if (stack.getCount() <= 1) {
            ItemStack fullGem = new ItemStack(fullGemItem());
            player.setItemInHand(hand, fullGem);
        } else {
            stack.shrink(1);
            ItemStack fullGem = new ItemStack(fullGemItem());
            if (!player.addItem(fullGem)) {
                player.drop(fullGem, false);
            }
        }
    }
}
