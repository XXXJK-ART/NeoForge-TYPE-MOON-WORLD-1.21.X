
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
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningService;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RandomMagicScrollItem extends Item {
    private final List<String> magicsToLearn;
    private final double successRate;
    private final String requiredMagic;

    public RandomMagicScrollItem(Properties properties, double successRate, String requiredMagic, String... magics) {
        super(properties);
        this.successRate = successRate;
        this.requiredMagic = requiredMagic;
        this.magicsToLearn = Arrays.asList(magics);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Check Requirement
            if (requiredMagic != null && !requiredMagic.isEmpty()) {
                if (!MagicLearningStrategy.isLearned(vars, requiredMagic)) {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.requirement_not_met", 
                        Component.translatable("magic.typemoonworld." + requiredMagic + ".name")), true);
                    return InteractionResultHolder.fail(stack);
                }
            }
            
            List<String> unlearnedMagics = new ArrayList<>();
            for (String magic : magicsToLearn) {
                if (!vars.learned_magics.contains(magic)) {
                    unlearnedMagics.add(magic);
                }
            }
            
            if (unlearnedMagics.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.already_learned"), true);
                return InteractionResultHolder.fail(stack);
            }
            
            // Shuffle to pick random
            Collections.shuffle(unlearnedMagics);
            String magicToLearn = unlearnedMagics.get(0);
            
            if (MagicLearningStrategy.materialAllowed(vars, magicToLearn)) {
                MagicLearningService.learnFromMaterial(serverPlayer, magicToLearn, player.getRandom().nextDouble());
            } else {
                player.displayClientMessage(Component.translatable("message.typemoonworld.magic.learning_restricted"), true);
            }
            // A fragment is consumed for every attempt, including failure.
            stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }
        
        return InteractionResultHolder.pass(stack);
    }
}
