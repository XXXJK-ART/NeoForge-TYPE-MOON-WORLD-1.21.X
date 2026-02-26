package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
public class FullManaCarvedGemItem extends Item {
    private final Supplier<Item> emptyGemSupplier;
    private final GemQuality quality;
    private final GemType type;

    public FullManaCarvedGemItem(Properties properties, Supplier<Item> emptyGemSupplier, GemQuality quality, GemType type) {
        super(properties);
        this.emptyGemSupplier = emptyGemSupplier;
        this.quality = quality;
        this.type = type;
    }

    public GemQuality getQuality() {
        return quality;
    }
    
    public GemType getType() {
        return type;
    }
    
    public Item getEmptyGemItem() {
        return emptyGemSupplier.get();
    }

    public double getManaAmount() {
        return quality.getCapacity(type);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            double manaAmount = quality.getCapacity(type);
            
            // Allow Overload: Removed check for max mana
            vars.player_mana += manaAmount;
            vars.syncMana(player);
            
            // Overload warning
            double current = vars.player_mana;
            double max = vars.player_max_mana;
            if (current > max) {
                 String color = "\u00A7e"; // Yellow
                 if (current > max * 1.25) {
                      color = "\u00A74"; // Dark Red
                 } else if (current > max * 1.2) {
                      color = "\u00A7c"; // Red
                 }
                 player.displayClientMessage(net.minecraft.network.chat.Component.literal(color + "警告：魔力过载！ (" + (int)current + "/" + (int)max + ")"), true);
            }
            
            if (stack.getCount() <= 1) {
                ItemStack emptyGem = new ItemStack(emptyGemSupplier.get());
                player.setItemInHand(hand, emptyGem);
                return InteractionResultHolder.sidedSuccess(emptyGem, world.isClientSide);
            } else {
                stack.shrink(1);
                ItemStack emptyGem = new ItemStack(emptyGemSupplier.get());
                if (!player.addItem(emptyGem)) {
                    player.drop(emptyGem, false);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }
}
