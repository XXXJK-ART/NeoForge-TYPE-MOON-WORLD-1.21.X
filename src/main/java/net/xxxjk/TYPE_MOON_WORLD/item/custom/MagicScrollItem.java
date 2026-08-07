package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningService;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Arrays;

public class MagicScrollItem extends Item {
    private final List<String> magicsToLearn;
    private final double successRate;
    private final boolean learnAllAtOnce;
    private final String requiredMagic;

    public MagicScrollItem(Properties properties, double successRate, String requiredMagic, String... magics) {
        this(properties, successRate, false, requiredMagic, magics);
    }

    public MagicScrollItem(Properties properties, double successRate, boolean learnAllAtOnce, String requiredMagic, String... magics) {
        super(properties);
        this.successRate = successRate;
        this.learnAllAtOnce = learnAllAtOnce;
        this.requiredMagic = requiredMagic;
        this.magicsToLearn = Arrays.asList(magics);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        
        boolean reusableBook = isReusableBook();
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (reusableBook && player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
            TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Check Requirement
            if (requiredMagic != null && !requiredMagic.isEmpty()) {
                if (!MagicLearningStrategy.isLearned(vars, requiredMagic)) {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.requirement_not_met", 
                        Component.translatable("magic.typemoonworld." + requiredMagic + ".name")), true);
                    return InteractionResultHolder.fail(stack);
                }
            }
            
            java.util.List<String> unlearnedMagics = new java.util.ArrayList<>();
            for (String magic : magicsToLearn) {
                if (!vars.learned_magics.contains(magic)) {
                    unlearnedMagics.add(magic);
                }
            }
            
            if (unlearnedMagics.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.already_learned"), true);
                return InteractionResultHolder.fail(stack);
            }
            
            String magicToLearn = unlearnedMagics.get(0);
            if (!MagicLearningStrategy.materialAllowed(vars, magicToLearn)) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.magic.learning_restricted"), true);
                return InteractionResultHolder.fail(stack);
            }
            // Books are reusable and use the normal complexity/proficiency formula.
            if (MagicLearningService.learnFromMaterial(serverPlayer, magicToLearn, player.getRandom().nextDouble())) {
                if (learnAllAtOnce) {
                    for (String magicId : unlearnedMagics) {
                        MagicLearningService.grantFromMaterial(serverPlayer, magicId);
                    }
                }
                
                if (reusableBook) player.getCooldowns().addCooldown(this, 100);
                else stack.shrink(1);
                return InteractionResultHolder.consume(stack);
            } else {
                if (reusableBook) player.getCooldowns().addCooldown(this, 100);
                else stack.shrink(1);
                return InteractionResultHolder.consume(stack);
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }

    private boolean isReusableBook() {
        var key = BuiltInRegistries.ITEM.getKey(this);
        String path = key == null ? "" : key.getPath();
        return path.startsWith("magic_book_") || path.startsWith("magic_scroll_") && !path.endsWith("_broken");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        // Removed tooltips as requested
    }
}
