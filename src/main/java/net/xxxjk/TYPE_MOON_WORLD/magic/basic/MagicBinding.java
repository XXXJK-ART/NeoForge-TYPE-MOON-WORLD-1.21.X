package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.effect.BindingEffect;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicProficiencyContestHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.typemoonworld.api.MagicComplexity;

public final class MagicBinding {
   private MagicBinding() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double proficiency = vars.isCurrentSelectionFromCrest("binding_magic") ? 100.0 : vars.proficiency_binding_magic;
      LivingEntity target = BasicMagecraftHelper.rayTarget(player, 14.0);
      if (target == null || target == player || EntityUtils.isImmunePlayerTarget(target)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.binding.no_target"), true);
         return false;
      }
      if (!ManaHelper.consumeOneTimeMagicCost(player, 12.0 + proficiency * 0.08)) {
         return false;
      }

      boolean success = applyBinding(player, target, proficiency);
      if (success && !vars.isCurrentSelectionFromCrest("binding_magic")) {
         net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.add(vars, "binding_magic", 0.18);
      }
      return success;
   }

   public static boolean castDirect(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      if (caster == null || target == null || !target.isAlive() || vars == null || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      return applyBinding(caster, target, proficiency);
   }

   public static boolean applyBinding(LivingEntity caster, LivingEntity target, double proficiency) {
      return applyBinding(caster, target, proficiency, living -> true);
   }

   public static boolean applyBinding(LivingEntity caster, LivingEntity target, double proficiency,
                                      Predicate<LivingEntity> targetFilter) {
      if (caster == null || target == null || !target.isAlive() || targetFilter == null || !targetFilter.test(target)) {
         return false;
      }
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      int amplifier = p >= 25.0 ? 1 : 0;
      int duration = durationTicks(p);
      boolean any = false;
      if (p >= 70.0 && caster.level() instanceof ServerLevel level) {
         double radius = areaRadius(p);
         AABB box = target.getBoundingBox().inflate(radius, 2.0, radius);
         List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
            e -> e != caster && e.isAlive() && !EntityUtils.isImmunePlayerTarget(e) && targetFilter.test(e));
         for (LivingEntity living : targets) {
            any |= applySingle(caster, living, duration, amplifier, MagicComplexity.TWO_VERSE, p);
         }
      } else {
         any = applySingle(caster, target, duration, amplifier, MagicComplexity.ONE_VERSE, p);
      }
      if (any) {
         caster.level().playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.85F, p >= 75.0 ? 0.75F : 1.0F);
      }
      return any;
   }

   public static boolean applyArea(LivingEntity caster, Vec3 center, double proficiency, double radius) {
      if (!(caster.level() instanceof ServerLevel level)) {
         return false;
      }
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      int amplifier = p >= 25.0 ? 1 : 0;
      int duration = durationTicks(p);
      AABB box = new AABB(center, center).inflate(Math.max(0.5, radius), 2.0, Math.max(0.5, radius));
      List<LivingEntity> targets = level.getEntitiesOfClass(
         LivingEntity.class, box, target -> target != caster && target.isAlive()
            && target.position().distanceToSqr(center) <= radius * radius && !EntityUtils.isImmunePlayerTarget(target)
      );
      boolean any = false;
      for (LivingEntity target : targets) {
         any |= applySingle(caster, target, duration, amplifier, MagicComplexity.TWO_VERSE, p);
      }
      if (any) {
         level.playSound(null, BlockPos.containing(center), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.85F, p >= 75.0 ? 0.75F : 1.0F);
      }
      return any;
   }

   public static int durationTicks(double proficiency) {
      return (int)Math.round((1.0 + BasicMagecraftHelper.clampProficiency(proficiency) / 100.0 * 5.0) * 20.0);
   }

   public static double areaRadius(double proficiency) {
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      if (p < 70.0) {
         return 0.0;
      }
      return 3.0 + (p - 70.0) / 30.0 * 2.0;
   }

   private static boolean applySingle(
      LivingEntity caster, LivingEntity target, int baseDuration, int amplifier, MagicComplexity complexity, double proficiency
   ) {
      MagicProficiencyContestHelper.Result contest = MagicProficiencyContestHelper.contest(
         caster, target, "binding_magic", proficiency, complexity);
      if (contest == MagicProficiencyContestHelper.Result.COUNTERED) {
         applyCounterBinding(target, caster, proficiency);
         return false;
      }
      if (contest == MagicProficiencyContestHelper.Result.RESISTED) {
         return false;
      }
      int duration = MagicResistanceHelper.applyHarmfulMagicEffectResistance(target, baseDuration, complexity);
      if (duration <= 0) {
         return false;
      }
      target.getPersistentData().putBoolean(BindingEffect.TAG_FULL_BIND, amplifier > 0);
      target.addEffect(new MobEffectInstance(ModMobEffects.BINDING, duration, amplifier, false, true, true));
      return true;
   }

   private static void applyCounterBinding(LivingEntity counterCaster, LivingEntity originalCaster, double originalProficiency) {
      if (counterCaster == null || originalCaster == null || !originalCaster.isAlive() || EntityUtils.isImmunePlayerTarget(originalCaster)) {
         return;
      }
      Double targetProficiency = MagicProficiencyContestHelper.getComparableMagicProficiency(counterCaster, "binding_magic");
      double counterP = targetProficiency == null
         ? Math.min(100.0, originalProficiency + MagicProficiencyContestHelper.COUNTER_GAP + 1.0)
         : targetProficiency;
      int duration = MagicResistanceHelper.applyHarmfulMagicEffectResistance(
         originalCaster, Math.min(120, Math.max(30, durationTicks(counterP) * 2)), MagicComplexity.ONE_VERSE);
      if (duration <= 0) {
         return;
      }
      originalCaster.getPersistentData().putBoolean(BindingEffect.TAG_FULL_BIND, counterP >= 50.0);
      originalCaster.addEffect(new MobEffectInstance(ModMobEffects.BINDING, duration, counterP >= 50.0 ? 1 : 0, false, true, true));
   }
}
