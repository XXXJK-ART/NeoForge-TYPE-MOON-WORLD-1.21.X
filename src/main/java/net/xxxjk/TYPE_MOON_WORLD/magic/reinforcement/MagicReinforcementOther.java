package net.xxxjk.TYPE_MOON_WORLD.magic.reinforcement;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public class MagicReinforcementOther {
   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         Entity targetEntity = null;
         HitResult hitResult = EntityUtils.getRayTraceTarget(player, 10.0);
         if (hitResult.getType() == Type.ENTITY) {
            targetEntity = ((EntityHitResult)hitResult).getEntity();
         }

         if (targetEntity instanceof LivingEntity livingTarget) {
            int maxLevel = Math.min(5, 1 + (int)(vars.proficiency_reinforcement / 20.0));
            int requestLevel = vars.reinforcement_level == 0 ? 1 : vars.reinforcement_level;
            int level = Math.min(requestLevel, maxLevel);
            double cost = 30.0 * level;
            int duration = (600 + (int)(vars.proficiency_reinforcement * 10.0)) * level;
            String partKey = "";
            int amplifier = level - 1;
            MobEffectInstance effect = null;
            MobEffectInstance extraEffect = null;
            switch (vars.reinforcement_mode) {
               case 0:
                  effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE, duration, amplifier, false, false, true);
                  partKey = "body";
                  break;
               case 1:
                  effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH, duration, amplifier, false, false, true);
                  partKey = "arm";
                  break;
               case 2:
                  effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_OTHER_AGILITY, duration, amplifier, false, false, true);
                  partKey = "leg";
                  break;
               case 3:
                  effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_OTHER_SIGHT, duration, amplifier, false, false, true);
                  extraEffect = new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false);
                  partKey = "eye";
            }

            if (effect != null) {
               MobEffectInstance existing = livingTarget.getEffect(effect.getEffect());
               if (existing != null && existing.getAmplifier() >= effect.getAmplifier() && existing.getDuration() >= duration) {
                  player.displayClientMessage(
                     Component.translatable("message.typemoonworld.magic.reinforcement.item.already_better", new Object[]{existing.getAmplifier() + 1}), true
                  );
                  return;
               }
            }

            if (ManaHelper.consumeManaOrHealth(player, cost)) {
               if (effect != null) {
                  livingTarget.addEffect(effect);
               }

               if (extraEffect != null) {
                  livingTarget.addEffect(extraEffect);
               }

               TypeMoonWorldModVariables.ReinforcementData targetData = (TypeMoonWorldModVariables.ReinforcementData)livingTarget.getData(
                  TypeMoonWorldModVariables.REINFORCEMENT_DATA
               );
               targetData.casterUUID = player.getUUID();
               player.displayClientMessage(
                  Component.translatable("message.typemoonworld.magic.reinforcement.success", new Object[]{livingTarget.getDisplayName()}), true
               );
               player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement." + partKey), true);
               vars.proficiency_reinforcement = Math.min(100.0, vars.proficiency_reinforcement + 0.5);
               vars.syncPlayerVariables(player);
            }
         } else {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.reinforcement.no_target"), true);
         }
      }
   }
}
