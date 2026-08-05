package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.xxxjk.TYPE_MOON_WORLD.effect.SuggestionEffect;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicProficiencyContestHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.typemoonworld.api.MagicComplexity;

public final class MagicSuggestion {
   public static final int COMMAND_CONFUSE = 0;
   public static final int COMMAND_STOP = 1;
   public static final int COMMAND_FOLLOW = 2;
   public static final int COMMAND_AWAY = 3;
   public static final int COMMAND_ATTACK = 4;

   private MagicSuggestion() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double proficiency = vars.isCurrentSelectionFromCrest("suggestion_magic") ? 100.0 : vars.proficiency_suggestion_magic;
      LivingEntity target = BasicMagecraftHelper.rayTarget(player, 16.0);
      if (target == null || target == player || EntityUtils.isImmunePlayerTarget(target)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.suggestion.no_target"), true);
         return false;
      }
      if (!ManaHelper.consumeOneTimeMagicCost(player, 10.0 + proficiency * 0.08)) {
         return false;
      }

      boolean success = applySuggestion(player, target, proficiency);
      if (success && !vars.isCurrentSelectionFromCrest("suggestion_magic")) {
         vars.proficiency_suggestion_magic = Math.min(100.0, vars.proficiency_suggestion_magic + 0.18);
      }
      player.displayClientMessage(
         Component.translatable(success ? "message.typemoonworld.magic.suggestion.success" : "message.typemoonworld.magic.suggestion.failed"),
         true
      );
      return success;
   }

   public static boolean castDirect(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      if (caster == null || target == null || !target.isAlive() || vars == null || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      return applySuggestion(caster, target, proficiency);
   }

   public static boolean applySuggestion(LivingEntity caster, LivingEntity target, double proficiency) {
      if (caster == null || target == null || !target.isAlive()) {
         return false;
      }
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      MagicProficiencyContestHelper.Result contest = MagicProficiencyContestHelper.contest(
         caster, target, "suggestion_magic", p, MagicComplexity.ONE_VERSE);
      if (contest == MagicProficiencyContestHelper.Result.COUNTERED) {
         applyCounterSuggestion(target, caster, p);
         return false;
      }
      if (contest == MagicProficiencyContestHelper.Result.RESISTED) {
         return false;
      }
      if (isServantTarget(target)) {
         return applyServantInterference(caster, target, p);
      }
      if (!canControlTarget(target)) {
         int blindTicks = MagicResistanceHelper.applyHarmfulMagicEffectResistance(
            target, p >= 50.0 ? 20 : 8, MagicComplexity.ONE_VERSE);
         if (blindTicks > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindTicks, 0, false, true, true));
         }
         return false;
      }
      if (caster.getRandom().nextDouble() > successChance(p)) {
         int confuseTicks = MagicResistanceHelper.applyHarmfulMagicEffectResistance(target, 50, MagicComplexity.ONE_VERSE);
         if (confuseTicks > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, confuseTicks, 0, false, true, true));
         }
         return false;
      }

      int command = commandForProficiency(p);
      int duration = MagicResistanceHelper.applyHarmfulMagicEffectResistance(target, durationTicks(p), MagicComplexity.ONE_VERSE);
      if (duration <= 0) {
         return false;
      }
      int amplifier = p >= 75.0 ? 3 : p >= 50.0 ? 2 : p >= 25.0 ? 1 : 0;
      target.addEffect(new MobEffectInstance(ModMobEffects.SUGGESTION, duration, amplifier, false, true, true));
      int confusionTicks = MagicResistanceHelper.applyHarmfulMagicEffectResistance(target, Math.min(duration, 160), MagicComplexity.ONE_VERSE);
      if (confusionTicks > 0) {
         target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, confusionTicks, Math.min(1, amplifier), false, true, true));
      }
      if (target instanceof ServerPlayer) {
         if (p >= 75.0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 9, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 9, false, true, true));
         } else if (p >= 50.0) {
            int reverseTicks = MagicResistanceHelper.applyHarmfulMagicEffectResistance(
               target, Math.min(duration, 120), MagicComplexity.ONE_VERSE);
            if (reverseTicks > 0) {
               target.addEffect(new MobEffectInstance(ModMobEffects.REVERSE_MOVEMENT, reverseTicks, 0, false, true, true));
            }
         } else {
            int blindTicks = MagicResistanceHelper.applyHarmfulMagicEffectResistance(
               target, Math.min(duration, 80), MagicComplexity.ONE_VERSE);
            if (blindTicks > 0) {
               target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindTicks, 0, false, true, true));
            }
         }
      } else if (target instanceof Mob mob) {
         mob.getPersistentData().putInt(SuggestionEffect.TAG_COMMAND, command);
         mob.getPersistentData().putUUID(SuggestionEffect.TAG_CASTER, caster.getUUID());
         mob.getNavigation().stop();
         mob.setTarget(command == COMMAND_STOP || command == COMMAND_FOLLOW || command == COMMAND_AWAY ? null : mob.getTarget());
      }

      if (target.level() instanceof ServerLevel level) {
         level.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 0.7F, 1.25F);
      }
      return true;
   }

   private static void applyCounterSuggestion(LivingEntity counterCaster, LivingEntity originalCaster, double originalProficiency) {
      if (counterCaster == null || originalCaster == null || !originalCaster.isAlive() || EntityUtils.isImmunePlayerTarget(originalCaster)) {
         return;
      }
      double counterP = MagicProficiencyContestHelper.getComparableMagicProficiency(counterCaster, "suggestion_magic") == null
         ? Math.min(100.0, originalProficiency + MagicProficiencyContestHelper.COUNTER_GAP + 1.0)
         : MagicProficiencyContestHelper.getComparableMagicProficiency(counterCaster, "suggestion_magic");
      int duration = MagicResistanceHelper.applyHarmfulMagicEffectResistance(
         originalCaster, Math.min(200, Math.max(60, durationTicks(counterP) / 4)), MagicComplexity.ONE_VERSE);
      if (duration <= 0) {
         return;
      }
      int amplifier = counterP >= 75.0 ? 2 : counterP >= 50.0 ? 1 : 0;
      originalCaster.addEffect(new MobEffectInstance(ModMobEffects.SUGGESTION, duration, amplifier, false, true, true));
      originalCaster.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.min(duration, 120), Math.min(1, amplifier), false, true, true));
      if (originalCaster instanceof ServerPlayer && counterP >= 50.0) {
         originalCaster.addEffect(new MobEffectInstance(ModMobEffects.REVERSE_MOVEMENT, Math.min(duration, 100), 0, false, true, true));
      } else if (originalCaster instanceof Mob mob) {
         mob.getPersistentData().putInt(SuggestionEffect.TAG_COMMAND, COMMAND_CONFUSE);
         mob.getPersistentData().putUUID(SuggestionEffect.TAG_CASTER, counterCaster.getUUID());
         mob.getNavigation().stop();
      }
      if (originalCaster.level() instanceof ServerLevel level) {
         level.playSound(null, originalCaster.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 0.65F, 0.75F);
      }
   }

   public static boolean isServantTarget(LivingEntity target) {
      return target instanceof ServantEntity || ServantIdentityHelper.definitionOf(target) != null;
   }

   public static boolean canControlTarget(LivingEntity target) {
      if (target instanceof ServerPlayer || target instanceof net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity) {
         return BasicMagecraftHelper.isLowManaTarget(target);
      }
      return !(target instanceof ServantEntity) && ServantIdentityHelper.definitionOf(target) == null;
   }

   private static boolean applyServantInterference(LivingEntity caster, LivingEntity target, double proficiency) {
      if (proficiency < 50.0) {
         return false;
      }
      int duration = MagicResistanceHelper.applyHarmfulMagicEffectResistance(target, 20, MagicComplexity.ONE_VERSE);
      if (duration <= 0) {
         return false;
      }
      target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0, false, true, true));
      if (proficiency >= 75.0 && caster.getRandom().nextFloat() < 0.08F) {
         int reverseTicks = MagicResistanceHelper.applyHarmfulMagicEffectResistance(target, 40, MagicComplexity.ONE_VERSE);
         if (reverseTicks > 0) {
            target.addEffect(new MobEffectInstance(ModMobEffects.REVERSE_MOVEMENT, reverseTicks, 0, false, true, true));
         }
      }
      return true;
   }

   private static int commandForProficiency(double proficiency) {
      if (proficiency >= 75.0) {
         return COMMAND_STOP;
      } else if (proficiency >= 50.0) {
         return COMMAND_ATTACK;
      } else if (proficiency >= 25.0) {
         return COMMAND_STOP + Mth.floor(Math.random() * 3.0);
      }
      return COMMAND_CONFUSE;
   }

   private static int durationTicks(double proficiency) {
      if (proficiency >= 75.0) {
         return 3600;
      } else if (proficiency >= 50.0) {
         return 1200;
      } else if (proficiency >= 25.0) {
         return 600;
      }
      return 200;
   }

   private static double successChance(double proficiency) {
      if (proficiency >= 75.0) {
         return 0.95;
      } else if (proficiency >= 50.0) {
         return 0.9;
      } else if (proficiency >= 25.0) {
         return 0.8;
      }
      return 0.6;
   }
}
