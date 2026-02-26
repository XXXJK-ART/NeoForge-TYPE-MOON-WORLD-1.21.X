
package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RandomMagicScrollItem extends Item {
    private final List<String> magicsToLearn;
    private final double successRate;

    public RandomMagicScrollItem(Properties properties, double successRate, String... magics) {
        super(properties);
        this.successRate = successRate;
        this.magicsToLearn = Arrays.asList(magics);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Filter out already learned magics
            List<String> unlearned = new ArrayList<>();
            for (String magic : magicsToLearn) {
                if (!vars.learned_magics.contains(magic)) {
                    unlearned.add(magic);
                }
            }
            
            if (unlearned.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.already_learned"), true);
                return InteractionResultHolder.fail(stack);
            }
            
            // Shuffle to pick random
            Collections.shuffle(unlearned);
            String magicToLearn = unlearned.get(0);
            
            // Attempt to learn
            if (player.getRandom().nextDouble() < successRate) {
                vars.learned_magics.add(magicToLearn);
                vars.syncPlayerVariables(player);
                
                player.displayClientMessage(Component.translatable("message.typemoonworld.magic.learned", Component.translatable("magic.typemoonworld." + magicToLearn + ".name")), true);
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                
                // Damage Item (Reduce Durability)
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return InteractionResultHolder.consume(stack);
            } else {
                player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.learn_failed"), true);
                player.playNotifySound(SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                
                // Damage Item (Reduce Durability)
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return InteractionResultHolder.consume(stack);
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }
}
