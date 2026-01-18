package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;

public class CarvedGemItem extends Item {
    private final GemType type;
    private final double manaAmount;

    public CarvedGemItem(Properties properties, GemType type, double manaAmount) {
        super(properties);
        this.type = type;
        this.manaAmount = manaAmount;
    }

    private Item fullGemItem() {
        return switch (type) {
            case EMERALD -> ModItems.CARVED_EMERALD_FULL.get();
            case RUBY -> ModItems.CARVED_RUBY_FULL.get();
            case SAPPHIRE -> ModItems.CARVED_SAPPHIRE_FULL.get();
            case TOPAZ -> ModItems.CARVED_TOPAZ_FULL.get();
            case WHITE_GEMSTONE -> ModItems.CARVED_WHITE_GEMSTONE_FULL.get();
        };
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.player_mana >= manaAmount) {
                vars.player_mana -= manaAmount;
                vars.syncPlayerVariables(player);
                if (stack.getCount() <= 1) {
                    ItemStack fullGem = new ItemStack(fullGemItem());
                    player.setItemInHand(hand, fullGem);
                    return InteractionResultHolder.sidedSuccess(fullGem, world.isClientSide);
                } else {
                    stack.shrink(1);
                    ItemStack fullGem = new ItemStack(fullGemItem());
                    if (!player.addItem(fullGem)) {
                        player.drop(fullGem, false);
                    }
                }
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("魔力不足"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }
}
