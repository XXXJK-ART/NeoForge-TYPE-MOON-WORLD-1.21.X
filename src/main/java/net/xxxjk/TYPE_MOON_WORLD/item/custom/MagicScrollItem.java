package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.Nullable;

public class MagicScrollItem extends Item {
   private final List<String> magicsToLearn;
   private final double successRate;
   private final boolean learnAllAtOnce;
   private final List<String> requiredMagics;

   public MagicScrollItem(Properties properties, double successRate, String requiredMagic, String... magics) {
      this(properties, successRate, false, requiredMagic, magics);
   }

   public MagicScrollItem(Properties properties, double successRate, boolean learnAllAtOnce, String requiredMagic, String... magics) {
      this(properties, successRate, learnAllAtOnce, requiredMagic == null ? null : new String[]{requiredMagic}, magics);
   }

   public MagicScrollItem(Properties properties, double successRate, boolean learnAllAtOnce, String[] requiredMagics, String... magics) {
      super(properties);
      this.successRate = successRate;
      this.learnAllAtOnce = learnAllAtOnce;
      List<String> requirementList = new ArrayList<>();
      if (requiredMagics != null) {
         for (String requiredMagic : requiredMagics) {
            if (requiredMagic != null && !requiredMagic.isEmpty()) {
               requirementList.add(requiredMagic);
            }
         }
      }

      this.requiredMagics = requirementList;
      this.magicsToLearn = Arrays.asList(magics);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      ItemStack stack = player.getItemInHand(usedHand);
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)serverPlayer.getData(
            TypeMoonWorldModVariables.PLAYER_VARIABLES
         );
         if (!this.requiredMagics.isEmpty()) {
            for (String requiredMagic : this.requiredMagics) {
               if (!vars.learned_magics.contains(requiredMagic)) {
                  player.displayClientMessage(
                     Component.translatable(
                        "message.typemoonworld.scroll.requirement_not_met",
                        new Object[]{Component.translatable("magic.typemoonworld." + requiredMagic + ".name")}
                     ),
                     true
                  );
                  return InteractionResultHolder.fail(stack);
               }
            }
         }

         List<String> unlearnedMagics = new ArrayList<>();

         for (String magic : this.magicsToLearn) {
            if (!vars.learned_magics.contains(magic)) {
               unlearnedMagics.add(magic);
            }
         }

         if (unlearnedMagics.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.already_learned"), true);
            return InteractionResultHolder.fail(stack);
         } else if (!(player.getRandom().nextDouble() < this.successRate)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.scroll.learn_failed"), true);
            player.playNotifySound(SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            return InteractionResultHolder.consume(stack);
         } else {
            if (this.learnAllAtOnce) {
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
            if (this.learnAllAtOnce && unlearnedMagics.size() > 1) {
               for (String magicIdx : unlearnedMagics) {
                  player.displayClientMessage(
                     Component.translatable(
                        "message.typemoonworld.magic.learned", new Object[]{Component.translatable("magic.typemoonworld." + magicIdx + ".name")}
                     ),
                     true
                  );
               }
            } else {
               String learnedMagic = unlearnedMagics.get(0);
               player.displayClientMessage(
                  Component.translatable(
                     "message.typemoonworld.magic.learned", new Object[]{Component.translatable("magic.typemoonworld." + learnedMagic + ".name")}
                  ),
                  true
               );
            }

            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            return InteractionResultHolder.consume(stack);
         }
      } else {
         return InteractionResultHolder.pass(stack);
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
   }
}
