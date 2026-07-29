package net.xxxjk.TYPE_MOON_WORLD.servant.concealment;

import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanCombatService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanRules;
import org.joml.Vector3f;

/** Shared server-side controller for servant concealment and visibility leakage. */
public final class ServantConcealment {
   private static final String TAG_MANAGED_SILENCE = "TypeMoonConcealmentManagedSilence";
   private static final String TAG_PREVIOUS_SILENCE = "TypeMoonConcealmentPreviousSilence";
   private static final String TAG_NEXT_PARTICLE_TICK = "TypeMoonConcealmentNextParticleTick";
   private static final String TAG_MANAGED_INVISIBLE = "TypeMoonConcealmentManagedInvisible";
   private static final DustParticleOptions LEAK_PARTICLE = new DustParticleOptions(
      new Vector3f(0.018F, 0.018F, 0.024F), 0.32F);

   private ServantConcealment() {
   }

   public static boolean isFullyConcealed(LivingEntity entity) {
      if (entity instanceof ShadowHassanEntity hassan) return hassan.isPresenceConcealed();
      if (entity instanceof ServantEntity) {
         return entity.hasEffect(MobEffects.INVISIBILITY)
            || entity.isInvisible() && !entity.getPersistentData().getBoolean(TAG_MANAGED_INVISIBLE);
      }
      if (!(entity instanceof Player player)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (BajiquanCombatService.isCircleRealmActive(player)) return true;
      if (vars.servant_card_transformed && "shadow_hassan".equals(vars.servant_card_id)) {
         return player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
            && net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardShadowHassanSkills.isConcealed(serverPlayer);
      }
      return player.hasEffect(MobEffects.INVISIBILITY)
         || player.isInvisible() && !player.getPersistentData().getBoolean(TAG_MANAGED_INVISIBLE);
   }

   public static void tick(LivingEntity entity) {
      boolean concealed = isFullyConcealed(entity);
      manageVanillaInvisibility(entity, concealed);
      ConcealmentStateSync.update(entity, concealed);
      manageSilence(entity, concealed);
      if (!concealed) {
         entity.getPersistentData().remove(TAG_NEXT_PARTICLE_TICK);
         return;
      }

      tickLeakParticles(entity);

      if (entity.tickCount % 5 == 0 && entity.level() instanceof ServerLevel level) {
         for (Mob mob : level.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(48.0),
            mob -> mob != entity && mob.getTarget() == entity)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
         }
      }
      if (entity.tickCount % 20 == 0) suppressRevealingEffects(entity);
   }

   public static void suppressRevealingEffects(LivingEntity entity) {
      entity.setGlowingTag(false);
      if (entity.level().isClientSide()) return;
      for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
         if (effect.getEffect().is(ShadowHassanRules.REVEALING_EFFECTS)) entity.removeEffect(effect.getEffect());
      }
   }

   public static ConcealmentRank rankOf(LivingEntity entity) {
      if (isParticleExempt(entity)) return ConcealmentRank.EX;
      ServantDefinition definition = ServantIdentityHelper.definitionOf(entity);
      ConcealmentRank best = ConcealmentRank.NONE;
      if (definition != null) {
         for (String skillId : definition.skillIds()) {
            best = ConcealmentRank.stronger(best, ConcealmentRank.fromSkillId(skillId));
         }
      }
      return best == ConcealmentRank.NONE ? ConcealmentRank.E : best;
   }

   private static void tickLeakParticles(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      ConcealmentRank rank = rankOf(entity);
      if (rank.isPerfect() || rank.maxParticles() <= 0) {
         entity.getPersistentData().remove(TAG_NEXT_PARTICLE_TICK);
         return;
      }

      long now = level.getGameTime();
      if (!entity.getPersistentData().contains(TAG_NEXT_PARTICLE_TICK)) {
         scheduleNextLeak(entity, rank, now);
         return;
      }
      if (now < entity.getPersistentData().getLong(TAG_NEXT_PARTICLE_TICK)) return;

      int count = entity.getRandom().nextIntBetweenInclusive(1, rank.maxParticles());
      double horizontalSpread = Math.max(0.16, entity.getBbWidth() * 0.32);
      level.sendParticles(LEAK_PARTICLE,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.52, entity.getZ(), count,
         horizontalSpread, Math.max(0.24, entity.getBbHeight() * 0.30), horizontalSpread, 0.004);
      scheduleNextLeak(entity, rank, now);
   }

   private static void scheduleNextLeak(LivingEntity entity, ConcealmentRank rank, long now) {
      int delay = entity.getRandom().nextIntBetweenInclusive(rank.minimumDelay(), rank.maximumDelay());
      entity.getPersistentData().putLong(TAG_NEXT_PARTICLE_TICK, now + delay);
   }

   private static boolean isParticleExempt(LivingEntity entity) {
      if (entity instanceof ShadowHassanEntity || entity instanceof LiShuwenEntity) return true;
      if (entity instanceof Player player && BajiquanCombatService.isCircleRealmActive(player)) return true;
      ServantDefinition definition = ServantIdentityHelper.definitionOf(entity);
      if (definition != null && ("shadow_hassan".equals(definition.id()) || "li_shuwen".equals(definition.id()))) {
         return true;
      }
      if (entity instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return "shadow_hassan".equals(vars.servant_card_id) || "li_shuwen".equals(vars.servant_card_id);
      }
      return false;
   }

   private static void manageSilence(LivingEntity entity, boolean concealed) {
      if (entity instanceof ShadowHassanEntity) return;
      var data = entity.getPersistentData();
      if (concealed) {
         if (!data.getBoolean(TAG_MANAGED_SILENCE)) {
            data.putBoolean(TAG_MANAGED_SILENCE, true);
            data.putBoolean(TAG_PREVIOUS_SILENCE, entity.isSilent());
         }
         entity.setSilent(true);
      } else if (data.getBoolean(TAG_MANAGED_SILENCE)) {
         entity.setSilent(data.getBoolean(TAG_PREVIOUS_SILENCE));
         data.remove(TAG_MANAGED_SILENCE);
         data.remove(TAG_PREVIOUS_SILENCE);
      }
   }

   private static void manageVanillaInvisibility(LivingEntity entity, boolean concealed) {
      var data = entity.getPersistentData();
      if (concealed) {
         if (!entity.isInvisible()) {
            data.putBoolean(TAG_MANAGED_INVISIBLE, true);
            entity.setInvisible(true);
         }
      } else if (data.getBoolean(TAG_MANAGED_INVISIBLE)) {
         entity.setInvisible(false);
         data.remove(TAG_MANAGED_INVISIBLE);
      }
   }
}
