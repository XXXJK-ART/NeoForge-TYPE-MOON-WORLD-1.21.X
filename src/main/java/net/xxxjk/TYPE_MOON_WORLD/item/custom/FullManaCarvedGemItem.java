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

public class FullManaCarvedGemItem extends Item {
    private final Supplier<Item> emptyGemSupplier;
    private final double manaAmount;

    public FullManaCarvedGemItem(Properties properties, Supplier<Item> emptyGemSupplier, double manaAmount) {
        super(properties);
        this.emptyGemSupplier = emptyGemSupplier;
        this.manaAmount = manaAmount;
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
            if (vars.player_mana < vars.player_max_mana) {
                double canReceive = vars.player_max_mana - vars.player_mana;
                double restore = Math.min(canReceive, manaAmount);
                vars.player_mana += restore;
                vars.syncPlayerVariables(player);
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
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("魔力已满"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }
}
