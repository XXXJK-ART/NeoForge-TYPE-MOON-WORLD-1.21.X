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
    private final boolean learnAllAtOnce;
    private final List<String> requiredMagics;

    public MagicScrollItem(Properties properties, double successRate, String requiredMagic, String... magics) {
        this(properties, successRate, false, requiredMagic == null ? new String[0] : new String[] { requiredMagic }, magics);
    }

    public MagicScrollItem(Properties properties, double successRate, boolean learnAllAtOnce, String requiredMagic, String... magics) {
        this(properties, successRate, learnAllAtOnce, requiredMagic == null ? new String[0] : new String[] { requiredMagic }, magics);
    }

    public MagicScrollItem(Properties properties, double successRate, boolean learnAllAtOnce, String[] requiredMagics, String... magics) {
        super(properties);
        this.successRate = successRate;
        this.learnAllAtOnce = learnAllAtOnce;
        this.requiredMagics = Arrays.asList(requiredMagics == null ? new String[0] : requiredMagics);
        this.magicsToLearn = Arrays.asList(magics);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Check requirements
            for (String requiredMagic : requiredMagics) {
                if (requiredMagic == null || requiredMagic.isEmpty()) {
                    continue;
                }
                if (!vars.learned_magics.contains(requiredMagic)) {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.requirement_not_met", 
                        Component.translatable("magic.typemoonworld." + requiredMagic + ".name")), true);
                    return InteractionResultHolder.fail(stack);
                }
            }

            for (String magic : magicsToLearn) {
                if (vars.learned_magics.contains(magic)) {
                    continue;
                }
                if (!meetsExtraLearningRequirement(serverPlayer, vars, magic)) {
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
            
            // Attempt to learn
            if (player.getRandom().nextDouble() < successRate) {
                if (learnAllAtOnce) {
                    for (String magicId : unlearnedMagics) {
                        if (!vars.learned_magics.contains(magicId)) {
                            vars.learned_magics.add(magicId);
                        }
                    }
                } else {
                    String magicToLearn = unlearnedMagics.get(0);
                    vars.learned_magics.add(magicToLearn);
                }
                vars.syncPlayerVariables(player);

                if (learnAllAtOnce && unlearnedMagics.size() > 1) {
                    for (String magicId : unlearnedMagics) {
                        player.displayClientMessage(
                                Component.translatable("message.typemoonworld.magic.learned", Component.translatable("magic.typemoonworld." + magicId + ".name")),
                                true
                        );
                    }
                } else {
                    String learnedMagic = unlearnedMagics.get(0);
                    player.displayClientMessage(
                            Component.translatable("message.typemoonworld.magic.learned", Component.translatable("magic.typemoonworld." + learnedMagic + ".name")),
                            true
                    );
                }
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                
                // Damage Item (Reduce Durability) instead of shrinking
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return InteractionResultHolder.consume(stack);
            } else {
                // Failed to learn
                player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.learn_failed"), true);
                player.playNotifySound(SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                
                // Damage Item (Reduce Durability)
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                return InteractionResultHolder.consume(stack);
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }

    private static boolean meetsExtraLearningRequirement(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
        if ("gandr_machine_gun".equals(magicId) && vars.proficiency_gander < 50.0D) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.typemoonworld.magic.gandr_machine_gun.learn_requirement",
                            50
                    ),
                    true
            );
            return false;
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        // Removed tooltips as requested
    }
}
