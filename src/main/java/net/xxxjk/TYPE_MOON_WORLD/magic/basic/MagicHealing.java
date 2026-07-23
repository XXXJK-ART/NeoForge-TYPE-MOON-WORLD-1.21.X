package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import org.joml.Vector3f;

public final class MagicHealing {
   private static final DustParticleOptions HEAL_DUST = new DustParticleOptions(new Vector3f(0.45F, 1.0F, 0.62F), 1.1F);

   private MagicHealing() {
   }

   public static void execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return;
      }

      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double proficiency = vars.isCurrentSelectionFromCrest("healing_magic") ? 100.0 : vars.proficiency_healing_magic;
      boolean selfTarget = vars.healing_magic_target == 0;
      LivingEntity target = selfTarget ? player : BasicMagecraftHelper.rayTarget(player, targetRange(proficiency));
      if (target == null || EntityUtils.isImmunePlayerTarget(target) || (!selfTarget && !canHealOther(player, target, proficiency))) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.healing.no_target"), true);
         return;
      }

      double cost = 12.0 + proficiency * 0.08;
      if (!ManaHelper.consumeManaOrHealth(player, cost)) {
         return;
      }

      int castTicks = castTicks(proficiency);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(8, castTicks), 3, false, false, true));
      int heal = healAmount(proficiency);
      if (target == player) {
         heal = Math.max(1, heal / 2);
      }
      if (hasCurseLikeWound(target)) {
         heal = Math.max(1, heal / 2);
      }

      target.heal(heal);
      net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService.cleanse(target, true);
      if (proficiency >= 25.0) {
         removeBleedingEffects(target);
      }

      spawnParticles(target);
      target.level().playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.35F);
      if (!vars.isCurrentSelectionFromCrest("healing_magic")) {
         vars.proficiency_healing_magic = Math.min(100.0, vars.proficiency_healing_magic + (selfTarget ? 0.12 : 0.2));
      }
   }

   public static boolean healDirect(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      if (caster == null || target == null || !target.isAlive()) {
         return false;
      }
      double clamped = BasicMagecraftHelper.clampProficiency(proficiency);
      int heal = healAmount(clamped);
      if (target == caster) {
         heal = Math.max(1, heal / 2);
      }
      if (hasCurseLikeWound(target)) {
         heal = Math.max(1, heal / 2);
      }
      target.heal(heal);
      net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService.cleanse(target, true);
      if (clamped >= 25.0) {
         removeBleedingEffects(target);
      }
      spawnParticles(target);
      target.level().playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.65F, 1.3F);
      return true;
   }

   public static int healAmount(double proficiency) {
      return (int)Math.floor(2.0 + BasicMagecraftHelper.clampProficiency(proficiency) / 100.0 * 23.0);
   }

   private static int castTicks(double proficiency) {
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      if (p >= 75.0) {
         return 12;
      } else if (p >= 50.0) {
         return 24;
      } else if (p >= 25.0) {
         return 40;
      }
      return 60;
   }

   private static double targetRange(double proficiency) {
      return proficiency >= 50.0 ? 8.0 : 3.0;
   }

   private static boolean canHealOther(ServerPlayer caster, LivingEntity target, double proficiency) {
      if (target == caster || !target.isAlive()) {
         return false;
      }
      if (proficiency < 50.0 && caster.distanceToSqr(target) > 3.0 * 3.0) {
         return false;
      }
      return caster.hasLineOfSight(target) || caster.distanceToSqr(target) <= 3.0 * 3.0;
   }

   private static boolean hasCurseLikeWound(LivingEntity target) {
      return target.hasEffect(MobEffects.WITHER) || target.hasEffect(MobEffects.POISON) || target.hasEffect(MobEffects.HARM);
   }

   private static void removeBleedingEffects(LivingEntity target) {
      List<Holder<MobEffect>> remove = new ArrayList<>();
      for (MobEffectInstance instance : target.getActiveEffects()) {
         MobEffect effect = instance.getEffect().value();
         if (effect.getCategory() == MobEffectCategory.HARMFUL) {
            ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            String path = key == null ? "" : key.getPath().toLowerCase();
            if (path.contains("bleed") || path.contains("bleeding")) {
               remove.add(instance.getEffect());
            }
         }
      }
      for (Holder<MobEffect> effect : remove) {
         target.removeEffect(effect);
      }
   }

   private static void spawnParticles(LivingEntity target) {
      if (target.level() instanceof ServerLevel level) {
         level.sendParticles(HEAL_DUST, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 22, 0.35, 0.35, 0.35, 0.0);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + target.getBbHeight() * 0.65, target.getZ(), 8, 0.28, 0.3, 0.28, 0.02);
      }
   }
}
