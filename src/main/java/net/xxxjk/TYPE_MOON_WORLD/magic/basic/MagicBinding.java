package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.effect.BindingEffect;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

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
         vars.proficiency_binding_magic = Math.min(100.0, vars.proficiency_binding_magic + 0.18);
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
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      int amplifier = p >= 25.0 ? 1 : 0;
      int duration = durationTicks(p);
      boolean any = false;
      if (p >= 70.0 && caster.level() instanceof ServerLevel level) {
         double radius = areaRadius(p);
         AABB box = target.getBoundingBox().inflate(radius, 2.0, radius);
         List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive() && !EntityUtils.isImmunePlayerTarget(e));
         for (LivingEntity living : targets) {
            applySingle(living, duration, amplifier);
            any = true;
         }
      } else {
         applySingle(target, duration, amplifier);
         any = true;
      }
      if (any) {
         caster.level().playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.85F, p >= 75.0 ? 0.75F : 1.0F);
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

   private static void applySingle(LivingEntity target, int baseDuration, int amplifier) {
      int duration = applyResistanceDuration(target, baseDuration);
      if (duration <= 0) {
         return;
      }
      target.getPersistentData().putBoolean(BindingEffect.TAG_FULL_BIND, amplifier > 0);
      target.addEffect(new MobEffectInstance(ModMobEffects.BINDING, duration, amplifier, false, true, true));
   }

   private static int applyResistanceDuration(LivingEntity target, int duration) {
      MagicResistanceRank rank = MagicResistanceHelper.getMagicResistanceRank(target);
      if (rank.isAtLeast(MagicResistanceRank.A)) {
         return Math.min(duration, 10);
      } else if (rank.isAtLeast(MagicResistanceRank.B)) {
         return Math.max(1, duration / 3);
      } else if (rank.isAtLeast(MagicResistanceRank.C)) {
         return Math.max(1, duration / 2);
      }
      return MagicResistanceHelper.applyDebuffResistance(target, duration);
   }
}
