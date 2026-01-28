package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Arrays;

public class MagicScrollItem extends Item {
    private final List<String> magicsToLearn;
    private final double successRate;

    public MagicScrollItem(Properties properties, double successRate, String... magics) {
        super(properties.durability(10)); // 10 Durability
        this.successRate = successRate;
        this.magicsToLearn = Arrays.asList(magics);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Check if already learned ALL magics in this scroll
            boolean allLearned = true;
            for (String magic : magicsToLearn) {
                if (!vars.learned_magics.contains(magic)) {
                    allLearned = false;
                    break;
                }
            }
            
            if (allLearned) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.already_learned"), true);
                return InteractionResultHolder.fail(stack);
            }
            
            // Attempt to learn
            if (player.getRandom().nextDouble() < successRate) {
                // Success
                boolean learnedAny = false;
                for (String magic : magicsToLearn) {
                    if (!vars.learned_magics.contains(magic)) {
                        vars.learned_magics.add(magic);
                        learnedAny = true;
                    }
                }
                
                if (learnedAny) {
                    vars.syncPlayerVariables(player);
                    player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.learn_success"), true);
                    player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                    
                    // Consume scroll on success
                    stack.shrink(1);
                    return InteractionResultHolder.consume(stack);
                }
            } else {
                // Failure
                player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.learn_failed"), true);
                player.playNotifySound(SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                
                // Damage Item (Reduce Durability)
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return InteractionResultHolder.consume(stack);
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        // Removed tooltips as requested
    }
}
