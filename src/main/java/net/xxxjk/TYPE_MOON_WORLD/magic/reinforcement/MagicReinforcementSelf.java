package net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicReinforcementSelf {
   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         int maxLevel = Math.min(5, 1 + (int)(vars.proficiency_reinforcement / 20.0));
         int requestLevel = vars.reinforcement_level == 0 ? 1 : vars.reinforcement_level;
         int level = Math.min(requestLevel, maxLevel);
         double cost = 20.0 * level;
         int duration = (600 + (int)(vars.proficiency_reinforcement * 10.0)) * level;
         int amplifier = level - 1;
         MobEffectInstance effect = null;
         MobEffectInstance extraEffect = null;
         switch (vars.reinforcement_mode) {
            case 0:
               effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_DEFENSE, duration, amplifier, false, false, true);
               break;
            case 1:
               effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_STRENGTH, duration, amplifier, false, false, true);
               break;
            case 2:
               effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_AGILITY, duration, amplifier, false, false, true);
               break;
            case 3:
               effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_SIGHT, duration, amplifier, false, false, true);
               extraEffect = new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false);
         }

         if (effect != null) {
            MobEffectInstance existing = player.getEffect(effect.getEffect());
            if (existing != null && existing.getAmplifier() >= effect.getAmplifier() && existing.getDuration() >= duration) {
               player.displayClientMessage(
                  Component.translatable("message.typemoonworld.magic.reinforcement.item.already_better", existing.getAmplifier() + 1), true
               );
               return;
            }
         }

         if (ManaHelper.consumeManaOrHealth(player, cost)) {
            if (effect != null) {
               player.addEffect(effect);
            }

            if (extraEffect != null) {
               player.addEffect(extraEffect);
            }

            TypeMoonWorldModVariables.ReinforcementData data = (TypeMoonWorldModVariables.ReinforcementData)player.getData(
               TypeMoonWorldModVariables.REINFORCEMENT_DATA
            );
            data.casterUUID = player.getUUID();
            switch (vars.reinforcement_mode) {
               case 0:
                  player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.body"), true);
                  break;
               case 1:
                  player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.arm"), true);
                  break;
               case 2:
                  player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.leg"), true);
                  break;
               case 3:
                  player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.eye"), true);
            }

            vars.proficiency_reinforcement = Math.min(100.0, vars.proficiency_reinforcement + 0.5);
            vars.syncPlayerVariables(player);
         }
      }
   }
}
