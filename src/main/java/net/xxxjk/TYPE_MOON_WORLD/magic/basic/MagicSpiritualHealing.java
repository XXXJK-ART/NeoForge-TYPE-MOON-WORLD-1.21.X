package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import org.joml.Vector3f;

public final class MagicSpiritualHealing {
   public static final String MAGIC_ID = "spiritual_healing";
   private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(new Vector3f(0.92F, 0.96F, 1.0F), 1.15F);
   private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.36F), 1.25F);

   private MagicSpiritualHealing() {
   }

   public static boolean execute(net.minecraft.world.entity.Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      boolean crestCast = vars.isCurrentSelectionFromCrest(MAGIC_ID);
      double proficiency = crestCast ? 100.0 : vars.proficiency_spiritual_healing;
      if (proficiency < 25.0) {
         return senseSpirits(player, vars, crestCast);
      }

      LivingEntity target = BasicMagecraftHelper.rayTarget(player, 12.0);
      if (target == null || EntityUtils.isImmunePlayerTarget(target) || !canAffect(target)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.spiritual_healing.no_target"), true);
         return false;
      }

      double cost = Math.max(8.0, 15.0 - BasicMagecraftHelper.clampProficiency(proficiency) * 0.04);
      if (!ManaHelper.consumeManaOrHealth(player, cost)) {
         return false;
      }

      int castTicks = castTicks(proficiency);
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(8, castTicks), 2, false, false, true));
      int heal = healAmount(proficiency);
      if (!(target instanceof ServantEntity)) {
         heal = Math.max(2, heal / 2);
      }
      target.heal(heal);
      net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService.cleanse(target, true);
      int removed = cleanseSpiritualEffects(target, proficiency);
      spawnHealingParticles(target, proficiency);
      target.level().playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.65F);
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.spiritual_healing.cast", target.getDisplayName(), heal, removed), true);
      if (!crestCast) {
         net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.add(vars, "spiritual_healing", 0.18);
         vars.syncProficiency(player);
      }
      return true;
   }

   public static boolean healDirect(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      if (caster == null || target == null || !target.isAlive()) {
         return false;
      }
      double clamped = BasicMagecraftHelper.clampProficiency(proficiency);
      int heal = healAmount(clamped);
      if (!(target instanceof ServantEntity)) {
         heal = Math.max(2, heal / 2);
      }
      target.heal(heal);
      net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService.cleanse(target, true);
      cleanseSpiritualEffects(target, clamped);
      spawnHealingParticles(target, clamped);
      return true;
   }

   private static boolean senseSpirits(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, boolean crestCast) {
      List<LivingEntity> spirits = player.level().getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(10.0),
         target -> target != player && target.isAlive() && canAffect(target)
      );
      if (!ManaHelper.consumeManaOrHealth(player, 5.0)) {
         return false;
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(WHITE_DUST, player.getX(), player.getY() + player.getBbHeight() * 0.65, player.getZ(), 18, 0.65, 0.45, 0.65, 0.0);
         for (LivingEntity spirit : spirits) {
            level.sendParticles(ParticleTypes.END_ROD, spirit.getX(), spirit.getY() + spirit.getBbHeight() * 0.65, spirit.getZ(), 4, 0.2, 0.35, 0.2, 0.0);
         }
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.spiritual_healing.sense", spirits.size()), true);
      if (!crestCast) {
         net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.add(vars, "spiritual_healing", 0.08);
         vars.syncProficiency(player);
      }
      return true;
   }

   public static boolean canAffect(LivingEntity target) {
      return target instanceof ServantEntity || isUndeadLike(target) || hasSpiritualPollution(target) || target instanceof Player;
   }

   public static int healAmount(double proficiency) {
      return (int)Math.floor(5.0 + BasicMagecraftHelper.clampProficiency(proficiency) / 100.0 * 75.0);
   }

   private static int castTicks(double proficiency) {
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      return (int)Math.round(60.0 - p / 100.0 * 40.0);
   }

   private static boolean hasSpiritualPollution(LivingEntity target) {
      for (MobEffectInstance instance : target.getActiveEffects()) {
         MobEffect effect = instance.getEffect().value();
         if (effect.getCategory() == MobEffectCategory.HARMFUL && isSpiritualEffect(effect)) {
            return true;
         }
      }
      return false;
   }

   private static int cleanseSpiritualEffects(LivingEntity target, double proficiency) {
      List<Holder<MobEffect>> remove = new ArrayList<>();
      int max = proficiency >= 75.0 ? 8 : proficiency >= 50.0 ? 5 : 2;
      for (MobEffectInstance instance : target.getActiveEffects()) {
         MobEffect effect = instance.getEffect().value();
         if (effect.getCategory() == MobEffectCategory.HARMFUL && isSpiritualEffect(effect)) {
            remove.add(instance.getEffect());
            if (remove.size() >= max) {
               break;
            }
         }
      }
      for (Holder<MobEffect> effect : remove) {
         target.removeEffect(effect);
      }
      return remove.size();
   }

   private static boolean isSpiritualEffect(MobEffect effect) {
      if (effect == MobEffects.CONFUSION.value()
         || effect == MobEffects.BLINDNESS.value()
         || effect == MobEffects.DARKNESS.value()
         || effect == MobEffects.WEAKNESS.value()
         || effect == MobEffects.MOVEMENT_SLOWDOWN.value()
         || effect == MobEffects.WITHER.value()
         || effect == MobEffects.POISON.value()) {
         return true;
      }
      ResourceLocation key = BuiltInRegistries.MOB_EFFECT.getKey(effect);
      String path = key == null ? "" : key.getPath().toLowerCase();
      return path.contains("curse") || path.contains("fear") || path.contains("charm") || path.contains("madness") || path.contains("pollution");
   }

   private static void spawnHealingParticles(LivingEntity target, double proficiency) {
      if (target.level() instanceof ServerLevel level) {
         DustParticleOptions dust = proficiency >= 50.0 ? GOLD_DUST : WHITE_DUST;
         level.sendParticles(dust, target.getX(), target.getY() + target.getBbHeight() * 0.58, target.getZ(), proficiency >= 75.0 ? 46 : 28, 0.4, 0.55, 0.4, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.68, target.getZ(), 10, 0.28, 0.45, 0.28, 0.02);
      }
   }

   private static boolean isUndeadLike(LivingEntity target) {
      return target instanceof Zombie
         || target instanceof Skeleton
         || target instanceof Stray
         || target instanceof Husk
         || target instanceof Drowned
         || target instanceof WitherSkeleton
         || target instanceof WitherBoss
         || target instanceof ZombifiedPiglin;
   }
}
