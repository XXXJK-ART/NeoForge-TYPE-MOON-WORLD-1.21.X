package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import com.mojang.math.Axis;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityFieldShellEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class MedusaCombatHelper {
   public static final String TAG_LAST_COMBAT_ACTIVITY_TICK = "MedusaLastCombatActivityTick";
   private static final String TAG_EYES_RELEASE_UNTIL = "MedusaEyesReleaseUntil";
   private static final String TAG_LAST_CYBELE_TICK = "MedusaLastCybeleTick";
   private static final String TAG_CYBELE_PRESSURE_UNTIL = "MedusaCybelePressureUntil";
   private static final String TAG_LAST_MONSTER_STRENGTH_TICK = "MedusaLastMonsterStrengthTick";
   private static final String TAG_MONSTER_STRENGTH_UNTIL = "MedusaMonsterStrengthUntil";
   private static final String TAG_LAST_CHARM_TICK = "MedusaLastCharmTick";
   private static final String TAG_LAST_BLOODFORT_TICK = "MedusaLastBloodfortTick";
   private static final String TAG_LAST_BLOODFORT_NP_TICK = "MedusaLastBloodfortNpTick";
   private static final String TAG_BLOODFORT_UNTIL = "MedusaBloodfortUntil";
   private static final String TAG_BLOODFORT_RADIUS = "MedusaBloodfortRadius";
   private static final String TAG_BLOODFORT_X = "MedusaBloodfortX";
   private static final String TAG_BLOODFORT_Y = "MedusaBloodfortY";
   private static final String TAG_BLOODFORT_Z = "MedusaBloodfortZ";
   private static final String TAG_BLOODFORT_NP_ACTIVE = "MedusaBloodfortNpActive";
   private static final String TAG_LAST_BELLEROPHON_TICK = "MedusaLastBellerophonTick";
   private static final String TAG_BELLEROPHON_CHARGE_UNTIL = "MedusaBellerophonChargeUntil";
   private static final String TAG_BELLEROPHON_SHOCKWAVE_DONE = "MedusaBellerophonShockwaveDone";
   private static final String TAG_BELLEROPHON_LAUNCH_TICK = "MedusaBellerophonLaunchTick";
   private static final String TAG_BELLEROPHON_TARGET_X = "MedusaBellerophonTargetX";
   private static final String TAG_BELLEROPHON_TARGET_Y = "MedusaBellerophonTargetY";
   private static final String TAG_BELLEROPHON_TARGET_Z = "MedusaBellerophonTargetZ";
   private static final String TAG_BELLEROPHON_RELAUNCH_TICK = "MedusaBellerophonRelaunchTick";
   private static final String TAG_LAST_CHAIN_SNARE_TICK = "MedusaLastChainSnareTick";
   private static final String TAG_LAST_VIPER_RUSH_TICK = "MedusaLastViperRushTick";
   private static final String TAG_LAST_SERPENT_STEP_TICK = "MedusaLastSerpentStepTick";
   private static final String TAG_LAST_PREDATOR_LOOP_TICK = "MedusaLastPredatorLoopTick";
   private static final String TAG_LAST_FRENZY_TEAR_TICK = "MedusaLastFrenzyTearTick";
   private static final String TAG_LAST_BASIC_MAUL_TICK = "MedusaLastBasicMaulTick";
   private static final String TAG_CHARM_OWNER = "MedusaCharmOwner";
   private static final String TAG_CHARM_UNTIL = "MedusaCharmUntil";
   private static final String TAG_BELLEROPHON_HIT_UNTIL = "MedusaBellerophonHitUntil";
   private static final String TAG_PEGASUS_COLLISION_HIT_UNTIL = "MedusaPegasusCollisionHitUntil";
   private static final String TAG_LAST_ROOFTOP_REPOSITION_TICK = "MedusaLastRooftopRepositionTick";
   private static final String TAG_RAPID_ASSAULT_HIT_UNTIL = "MedusaRapidAssaultHitUntil";
   private static final int CYBELE_COOLDOWN = 400;
   private static final int MONSTER_STRENGTH_COOLDOWN = 100;
   private static final int MONSTER_STRENGTH_DURATION = 100;
   private static final int CHARM_COOLDOWN = 400;
   private static final int BLOODFORT_COOLDOWN = 1200;
   private static final int BLOODFORT_NP_COOLDOWN = 1200;
   private static final int BELLEROPHON_COOLDOWN = 900;
   private static final int CHAIN_SNARE_COOLDOWN = 80;
   private static final int CHARGE_WINDUP_TICKS = 20;
   private static final int CHARGE_TICKS = 18;
   private static final int RIDE_EXTENSION_TICKS = 400;
   private static final int VIPER_RUSH_COOLDOWN = 24;
   private static final int SERPENT_STEP_COOLDOWN = 18;
   private static final int PREDATOR_LOOP_COOLDOWN = 12;
   private static final int FRENZY_TEAR_COOLDOWN = 4;
   private static final int BASIC_MAUL_COOLDOWN = 2;
   private static final double BLOODFORT_RADIUS_VALUE = 25.0;
   private static final double BELLEROPHON_CHARGE_DISTANCE = 10.0;
   private static final float CYBELE_USE_CHANCE = 0.05F;
   private static final float CHAIN_SNARE_CATCH_CHANCE = 0.72F;
   private static final float CHAIN_SNARE_ESCAPE_CHANCE = 0.35F;
   private static final ResourceLocation MONSTER_STRENGTH_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "medusa_monster_strength_attack");
   private static final ResourceLocation COMBAT_RUSH_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "medusa_combat_rush_speed");
   private static final ResourceLocation RIDING_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "medusa_riding_speed");
   private static final ResourceLocation RIDING_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "medusa_riding_armor");
   private static final ResourceLocation CYBELE_PRESSURE_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "medusa_cybele_pressure_attack");
   private static final ResourceLocation CYBELE_PRESSURE_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "medusa_cybele_pressure_speed");
   private static final ResourceLocation CYBELE_PRESSURE_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "medusa_cybele_pressure_armor");
   private static final ResourceLocation CYBELE_PRESSURE_TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "medusa_cybele_pressure_toughness");
   private static final DustParticleOptions BLOODFORT_PARTICLE = new DustParticleOptions(new Vector3f(0.95F, 0.22F, 0.35F), 1.1F);
   private static final DustParticleOptions BLOODFORT_SIGIL_PARTICLE = new DustParticleOptions(new Vector3f(0.86F, 0.08F, 0.12F), 1.25F);
   private static final DustParticleOptions BLOODFORT_NODE_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.24F), 1.45F);
   private static final DustParticleOptions BLOODFORT_LINK_PARTICLE = new DustParticleOptions(new Vector3f(0.72F, 0.02F, 0.08F), 1.05F);
   private static final DustParticleOptions SUMMON_LIGHT_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.96F, 0.82F), 1.25F);
   private static final DustParticleOptions SUMMON_GOLD_PARTICLE = new DustParticleOptions(new Vector3f(0.98F, 0.84F, 0.32F), 1.2F);

   private MedusaCombatHelper() {
   }

   public static boolean tryDodge(MedusaEntity entity, DamageSource source) {
      if (!canNegateIncomingHit(entity, source)) {
         return false;
      }
      if (entity.getRandom().nextFloat() < 0.6F) {
         spawnDodgeFx(entity);
         return true;
      }
      return false;
   }

   public static boolean tryBlock(MedusaEntity entity, DamageSource source) {
      if (!canNegateIncomingHit(entity, source)) {
         return false;
      }
      if (entity.getRandom().nextFloat() < 0.3F) {
         spawnDefenseFx(entity);
         return true;
      }
      return false;
   }

   public static void tick(MedusaEntity entity, ServantAiContext context) {
      long now = context.gameTick();
      tickTimedStates(entity, now);
      tickBloodfort(entity, now);
      tickCharmAttraction(entity, now);

      LivingEntity target = context.target();
      if (target == null || target.isDeadOrDying() || EntityUtils.isImmunePlayerTarget(target)) {
         target = entity.getTarget();
      }
      if (target == null || target.isDeadOrDying() || EntityUtils.isImmunePlayerTarget(target)) {
         target = findEmergencyTarget(entity);
      }

      if (entity.isRidingPegasus() || entity.getPegasus() != null) {
         tickBellerophon(entity, target, now);
      }

      if (target == null || target.isDeadOrDying() || EntityUtils.isImmunePlayerTarget(target)) {
         entity.setTarget(null);
         updateCloseCombatBonuses(entity, false);
         handleIdleState(entity);
         return;
      }

      entity.setTarget(target);
      updateCloseCombatBonuses(entity, true);
      entity.setCrouchPose(false);
      entity.getPersistentData().putLong(TAG_LAST_COMBAT_ACTIVITY_TICK, now);
      tickRapidAssaultDamage(entity, now);

      double distance = entity.distanceTo(target);
      int nearbyEnemyCount = countNearbyHostiles(entity, 12.0);
      boolean highThreat = target.getMaxHealth() >= 120.0F
         || target.getHealth() / Math.max(1.0F, target.getMaxHealth()) >= 0.8F
         || nearbyEnemyCount >= 3
         || entity.getHealth() / Math.max(1.0F, entity.getMaxHealth()) <= 0.35F;

      if (highThreat && entity.isBlindfoldSealed()) {
         releaseEyes(entity, now, 100);
      }

      if (entity.getPersistentData().getLong(TAG_BELLEROPHON_LAUNCH_TICK) > now) {
         entity.getNavigation().stop();
         return;
      }

      if (entity.isPerformingAction() || entity.isRoaring() || entity.isSlamming()) {
         performCloseRangePressure(entity, target, distance, now);
         return;
      }

      if (shouldUseCybele(entity, target, distance, highThreat, now)) {
         castCybele(entity, target, now);
         return;
      }

      if (shouldUseBloodfortNp(entity, target, nearbyEnemyCount, highThreat, now)) {
         castBloodfort(entity, now, true);
         return;
      }

      if (shouldUseBloodfort(entity, target, nearbyEnemyCount, highThreat, now)) {
         castBloodfort(entity, now, false);
         return;
      }

      if (shouldUseCharm(entity, target, distance, now)) {
         castCharm(entity, target, now);
         return;
      }

      if (shouldUseMonsterStrength(entity, distance, now)) {
         castMonsterStrength(entity, now);
      }

      if (shouldUseViperRush(entity, target, distance, now)) {
         performViperRush(entity, target, now);
         return;
      }

      if (shouldUseSerpentStep(entity, target, distance, now)) {
         performSerpentStep(entity, target, now);
         return;
      }

      if (shouldUsePredatorLoop(entity, target, distance, now)) {
         performPredatorLoop(entity, target, now);
         return;
      }

      if (shouldUseFrenzyTear(entity, distance, now)) {
         performFrenzyTear(entity, target, now);
         return;
      }

      if (shouldUseBellerophon(entity, target, distance, nearbyEnemyCount, highThreat, now)) {
         beginBellerophon(entity, target, now);
         return;
      }

      if (shouldUseChainSnare(entity, target, distance, now)) {
         castChainSnare(entity, target, now);
         return;
      }

      performCloseRangePressure(entity, target, distance, now);
   }

   public static void requestRooftopReposition(MedusaEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel) || target == null || !target.isAlive()) {
         return;
      }
      long now = entity.level().getGameTime();
      if (now - entity.getPersistentData().getLong(TAG_LAST_ROOFTOP_REPOSITION_TICK) < 40L || !entity.onGround()) {
         return;
      }

      Vec3 offset = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return;
      }
      horizontal = horizontal.normalize();
      entity.faceVector(horizontal);
      entity.getPersistentData().putLong(TAG_LAST_ROOFTOP_REPOSITION_TICK, now);
      entity.jumpFromGround();
      Vec3 motion = entity.getDeltaMovement();
      entity.setDeltaMovement(motion.x + horizontal.x * 0.65, Math.max(motion.y + 0.4, 0.75), motion.z + horizontal.z * 0.65);
      entity.hasImpulse = true;
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.15, entity.getZ(), 8, 0.3, 0.08, 0.3, 0.02);
      }
   }

   public static boolean isBusy(MedusaEntity entity) {
      long now = entity.level().getGameTime();
      return entity.isRidingPegasus()
         || entity.getPersistentData().getLong(TAG_BELLEROPHON_LAUNCH_TICK) > now
         || entity.getPersistentData().getLong(TAG_BELLEROPHON_CHARGE_UNTIL) > now;
   }

   private static void tickTimedStates(MedusaEntity entity, long now) {
      if (entity.getPersistentData().getLong(TAG_EYES_RELEASE_UNTIL) <= now
         && entity.getPersistentData().getLong(TAG_BELLEROPHON_LAUNCH_TICK) <= now
         && !entity.isRidingPegasus()) {
         entity.setEyesReleased(false);
         entity.setBlindfoldSealed(true);
      }

      if (entity.getPersistentData().getLong(TAG_MONSTER_STRENGTH_UNTIL) <= now) {
         clearMonsterStrength(entity);
      }

      updateMountedBonuses(entity);
   }

   private static void handleIdleState(MedusaEntity entity) {
      clearTransientZoneTargets(entity);
      clearCombatRush(entity);
      entity.setCrouchPose(false);
      if (!entity.isRidingPegasus()) {
         entity.setEyesReleased(false);
         entity.setBlindfoldSealed(true);
      }
   }

   private static void releaseEyes(MedusaEntity entity, long now, int durationTicks) {
      entity.setEyesReleased(true);
      entity.setBlindfoldSealed(false);
      entity.getPersistentData().putLong(TAG_EYES_RELEASE_UNTIL, now + durationTicks);
      entity.triggerNamedActionAnimation("remove_blindfold");
   }

   private static void castCybele(MedusaEntity entity, LivingEntity target, long now) {
      entity.getPersistentData().putLong(TAG_LAST_CYBELE_TICK, now);
      entity.triggerUppercutAnimation();
      if (!entity.isEyesReleased()) {
         releaseEyes(entity, now, 80);
      }
      if (entity.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_medusa_cybele", target.position(), 96.0);
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.25, 0.35, 0.25, 0.01);
         level.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 10, 0.3, 0.45, 0.3, 0.01);
         level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.8F, 0.85F);
      }

      if (HeraclesGodHandHelper.isAdaptedToCybele(target)) {
         HeraclesGodHandHelper.applyAdaptedSlow(target, 100);
         return;
      }
      if (HeraclesGodHandHelper.hasGodHand(target)) {
         HeraclesGodHandHelper.consumeLifeForCybele(target);
         return;
      }

      MagicResistanceRank rank = MagicResistanceHelper.getMagicResistanceRank(target);
      boolean released = entity.isEyesReleased();
      if (!rank.isAtLeast(MagicResistanceRank.B)) {
         applyPetrified(target, released ? 140 : 120);
         return;
      }

      if (rank == MagicResistanceRank.B) {
         if (failsCybeleSave(entity, target, rank, released)) {
            applyPetrified(target, released ? 140 : 120);
         } else {
            applyVisualSuppression(target, released ? 120 : 100);
         }
         return;
      }

      if (failsCybeleSave(entity, target, rank, released)) {
         applyHeavyPressure(target, released ? 220 : 200);
      } else {
         applyVisualSuppression(target, released ? 100 : 80);
      }
   }

   private static void castMonsterStrength(MedusaEntity entity, long now) {
      if (entity.getCurrentMp() < 5.0) {
         return;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 5.0);
      entity.getPersistentData().putLong(TAG_LAST_MONSTER_STRENGTH_TICK, now);
      entity.getPersistentData().putLong(TAG_MONSTER_STRENGTH_UNTIL, now + MONSTER_STRENGTH_DURATION);
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null && attack.getModifier(MONSTER_STRENGTH_ATTACK_ID) == null) {
         attack.addTransientModifier(new AttributeModifier(MONSTER_STRENGTH_ATTACK_ID, 5.0, AttributeModifier.Operation.ADD_VALUE));
      }
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANTED_HIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 14, 0.3, 0.4, 0.3, 0.04);
      }
   }

   private static void clearMonsterStrength(MedusaEntity entity) {
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) {
         attack.removeModifier(MONSTER_STRENGTH_ATTACK_ID);
      }
   }

   private static void castCharm(MedusaEntity entity, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 10.0) {
         return;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 10.0);
      entity.getPersistentData().putLong(TAG_LAST_CHARM_TICK, now);
      target.getPersistentData().putString(TAG_CHARM_OWNER, entity.getUUID().toString());
      target.getPersistentData().putLong(TAG_CHARM_UNTIL, now + 100L);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true, true));
      if (target instanceof net.minecraft.world.entity.Mob mob) {
         mob.setTarget(null);
      }
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() * 0.75, target.getZ(), 6, 0.35, 0.25, 0.35, 0.02);
         level.playSound(null, target.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE, 0.8F, 0.75F);
      }
   }

   private static void castBloodfort(MedusaEntity entity, long now, boolean noblePhantasm) {
      double cost = noblePhantasm ? 40.0 : 20.0;
      if (entity.getCurrentMp() < cost) {
         return;
      }
      double radius = BLOODFORT_RADIUS_VALUE;
      if (!hasAbsorbableTargets(entity, radius)) {
         return;
      }

      entity.setCurrentMp(entity.getCurrentMp() - cost);
      if (noblePhantasm) {
         ServantCombatSystem.broadcastNoblePhantasmWindup(entity, null, 16, false);
      }
      if (noblePhantasm) {
         entity.getPersistentData().putLong(TAG_LAST_BLOODFORT_NP_TICK, now);
      } else {
         entity.getPersistentData().putLong(TAG_LAST_BLOODFORT_TICK, now);
      }
      entity.getPersistentData().putLong(TAG_BLOODFORT_UNTIL, now + (noblePhantasm ? 600L : 300L));
      entity.getPersistentData().putDouble(TAG_BLOODFORT_RADIUS, radius);
      entity.getPersistentData().putDouble(TAG_BLOODFORT_X, entity.getX());
      entity.getPersistentData().putDouble(TAG_BLOODFORT_Y, entity.getY() + 0.1);
      entity.getPersistentData().putDouble(TAG_BLOODFORT_Z, entity.getZ());
      entity.getPersistentData().putBoolean(TAG_BLOODFORT_NP_ACTIVE, noblePhantasm);
      if (entity.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_medusa_bloodfort", entity.position(), 128.0);
         startBloodfortSummonSequence(level, entity.position(), radius, noblePhantasm);
         level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.8F, 0.65F);
      }
   }

   private static void tickBloodfort(MedusaEntity entity, long now) {
      long until = entity.getPersistentData().getLong(TAG_BLOODFORT_UNTIL);
      if (until <= now) {
         entity.getPersistentData().remove(TAG_BLOODFORT_UNTIL);
         entity.getPersistentData().remove(TAG_BLOODFORT_RADIUS);
         entity.getPersistentData().remove(TAG_BLOODFORT_NP_ACTIVE);
         return;
      }

      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = new Vec3(
         entity.getPersistentData().getDouble(TAG_BLOODFORT_X),
         entity.getPersistentData().getDouble(TAG_BLOODFORT_Y),
         entity.getPersistentData().getDouble(TAG_BLOODFORT_Z)
      );
      double radius = entity.getPersistentData().getDouble(TAG_BLOODFORT_RADIUS);
      boolean noblePhantasm = entity.getPersistentData().getBoolean(TAG_BLOODFORT_NP_ACTIVE);

      if (now % 10L == 0L) {
         spawnBloodfortGroundSigil(level, center, radius, noblePhantasm);
      }
      if (now % 5L == 0L) {
         spawnBloodfortShell(level, center, radius);
      }
      if (now % 4L == 0L) {
         spawnBloodfortInteriorHaze(level, center, radius, noblePhantasm);
      }
      if (now % 20L == 0L) {
         spawnBloodfortRisingShell(level, center, radius, noblePhantasm);
      }
      if (now % 20L != 0L) {
         return;
      }

      float damage = noblePhantasm ? 24.0F + entity.getRandom().nextInt(7) : 15.0F;
      for (LivingEntity victim : level.getEntitiesOfClass(
         LivingEntity.class,
         new AABB(center, center).inflate(radius, radius, radius),
         target -> isBloodfortTarget(entity, center, radius, target)
      )) {
         applyBloodfortDebuffs(victim, noblePhantasm);
         drainBloodfortMana(victim, noblePhantasm);
         float before = victim.getHealth();
         victim.hurt(entity.damageSources().magic(), damage);
         float dealt = Math.max(0.0F, before - victim.getHealth());
         if (dealt > 0.0F) {
            entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + dealt));
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
            spawnBloodfortVictimAura(level, victim, dealt);
         }
      }
   }

   private static void castChainSnare(MedusaEntity entity, LivingEntity target, long now) {
      entity.getPersistentData().putLong(TAG_LAST_CHAIN_SNARE_TICK, now);
      entity.triggerHorizontalSwingAnimation();
      if (entity.level() instanceof ServerLevel level) {
         spawnChainLine(level, entity.getEyePosition(), target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
         level.playSound(null, entity.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 0.75F, 0.9F);
      }
      if (entity.getRandom().nextFloat() > CHAIN_SNARE_CATCH_CHANCE) {
         if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(
               ParticleTypes.CLOUD,
               target.getX(),
               target.getY() + target.getBbHeight() * 0.5,
               target.getZ(),
               6,
               0.25,
               0.25,
               0.25,
               0.03
            );
            level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 0.7F, 1.15F);
         }
         return;
      }
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true, true));
      Vec3 pull = entity.position().subtract(target.position());
      boolean medusaPullsSelf = entity.getRandom().nextFloat() < 0.45F
         || entity.distanceTo(target) > 4.4
         || target.getY() - entity.getY() > 1.0;
      if (pull.lengthSqr() > 1.0E-4) {
         pull = pull.normalize();
         if (medusaPullsSelf) {
            Vec3 rush = target.position().subtract(entity.position());
            Vec3 horizontalRush = new Vec3(rush.x, 0.0, rush.z);
            if (horizontalRush.lengthSqr() > 1.0E-4) {
               horizontalRush = horizontalRush.normalize();
               entity.faceVector(horizontalRush);
               entity.setDeltaMovement(horizontalRush.x * 1.0, Math.max(entity.getDeltaMovement().y, 0.12), horizontalRush.z * 1.0);
               entity.hasImpulse = true;
               entity.getNavigation().moveTo(target, 1.3);
               if (entity.level() instanceof ServerLevel level) {
                  spawnDashTrail(level, entity, horizontalRush, 3.0, ParticleTypes.CRIT);
               }
            }
         } else {
            target.push(pull.x * 0.85, 0.15, pull.z * 0.85);
            target.hurtMarked = true;
         }
      }
      target.hurt(entity.damageSources().mobAttack(entity), 6.0F);
      if (entity.getRandom().nextFloat() < CHAIN_SNARE_ESCAPE_CHANCE) {
         TYPE_MOON_WORLD.queueServerWork(16, () -> tryEscapeChainSnare(entity, target));
      }
   }

   private static void beginBellerophon(MedusaEntity entity, LivingEntity target, long now) {
      if (entity.getCurrentMp() < 60.0 || entity.getPersistentData().getLong(TAG_LAST_BELLEROPHON_TICK) + BELLEROPHON_COOLDOWN > now) {
         return;
      }
      if (!ServantCombatSystem.canUseNoblePhantasm(entity)) {
         return;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 60.0);
      entity.getPersistentData().putLong(TAG_LAST_BELLEROPHON_TICK, now);
      entity.getPersistentData().putLong(TAG_BELLEROPHON_LAUNCH_TICK, now + CHARGE_WINDUP_TICKS);
      ServantCombatSystem.broadcastNoblePhantasmWindup(entity, target, CHARGE_WINDUP_TICKS, false);
      entity.setCrouchPose(true);
      releaseEyes(entity, now, CHARGE_WINDUP_TICKS + CHARGE_TICKS + 80);
      ServantVoiceHelper.tryPlayBellerophon(entity);
      if (entity.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_medusa_bellerophon", entity, 128.0);
         startBellerophonSummonFx(level, entity, now);
      }
      TYPE_MOON_WORLD.queueServerWork(CHARGE_WINDUP_TICKS, () -> launchBellerophon(entity, target));
   }

   private static void launchBellerophon(MedusaEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
         return;
      }
      MedusaPegasusEntity pegasus = ModEntities.MEDUSA_PEGASUS.get().create(level);
      if (pegasus == null) {
         entity.setCrouchPose(false);
         return;
      }
      pegasus.moveTo(entity.getX(), entity.getY() + 0.2, entity.getZ(), entity.getYRot(), entity.getXRot());
      pegasus.setSummoner(entity);
      pegasus.setFlyingMode(true);
      level.addFreshEntity(pegasus);
      entity.startRiding(pegasus, true);
      entity.setPegasusUuid(pegasus.getUUID());
      entity.setCrouchPose(false);
      long now = level.getGameTime();
      Vec3 chargeTarget = computeInitialChargeTarget(entity, target);
      entity.getPersistentData().putDouble(TAG_BELLEROPHON_TARGET_X, chargeTarget.x);
      entity.getPersistentData().putDouble(TAG_BELLEROPHON_TARGET_Y, chargeTarget.y);
      entity.getPersistentData().putDouble(TAG_BELLEROPHON_TARGET_Z, chargeTarget.z);
      entity.getPersistentData().putLong(TAG_BELLEROPHON_CHARGE_UNTIL, now + CHARGE_TICKS);
      entity.setBellerophonRideUntil(now + CHARGE_TICKS + RIDE_EXTENSION_TICKS);
      entity.getPersistentData().putBoolean(TAG_BELLEROPHON_SHOCKWAVE_DONE, false);
      spawnPegasusArrivalFx(level, pegasus);
   }

   private static void tickBellerophon(MedusaEntity entity, LivingEntity target, long now) {
      MedusaPegasusEntity pegasus = entity.getPegasus();
      if (pegasus == null || !pegasus.isAlive()) {
         entity.clearPegasusReference();
         entity.stopRiding();
         return;
      }

      pegasus.setFlyingMode(true);
      Vec3 desired = computePegasusVelocity(entity, pegasus, target, now);
      orientChargeActors(entity, pegasus, desired);
      pegasus.setDeltaMovement(desired);
      pegasus.hasImpulse = true;
      breakRideBlocks(pegasus, desired);

      if (now < entity.getPersistentData().getLong(TAG_BELLEROPHON_CHARGE_UNTIL)) {
         applyChargeHits(entity, pegasus, now);
         return;
      }

      if (!entity.getPersistentData().getBoolean(TAG_BELLEROPHON_SHOCKWAVE_DONE)) {
         entity.getPersistentData().putBoolean(TAG_BELLEROPHON_SHOCKWAVE_DONE, true);
         emitShockwave(entity, pegasus);
      }

      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         target = findNearbyChargeTarget(entity, pegasus);
      }
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         pegasus.setFlyingMode(false);
         applyGroundCruiseMotion(entity, pegasus, now);
         if (now >= entity.getBellerophonRideUntil()) {
            endBellerophon(entity, pegasus);
         }
         return;
      }

      if (!pegasus.isFlyingMode() && now - entity.getPersistentData().getLong(TAG_BELLEROPHON_RELAUNCH_TICK) >= 30L) {
         restartBellerophonCharge(entity, pegasus, target, now);
      }
      pegasus.setFlyingMode(true);
      applyRideCollisionHits(entity, pegasus, now);

      if (now >= entity.getBellerophonRideUntil()) {
         endBellerophon(entity, pegasus);
      }
   }

   private static void endBellerophon(MedusaEntity entity, MedusaPegasusEntity pegasus) {
      entity.stopRiding();
      entity.clearPegasusReference();
      entity.getPersistentData().remove(TAG_BELLEROPHON_CHARGE_UNTIL);
      entity.getPersistentData().remove(TAG_BELLEROPHON_LAUNCH_TICK);
      entity.getPersistentData().remove(TAG_BELLEROPHON_SHOCKWAVE_DONE);
      entity.getPersistentData().remove(TAG_BELLEROPHON_TARGET_X);
      entity.getPersistentData().remove(TAG_BELLEROPHON_TARGET_Y);
      entity.getPersistentData().remove(TAG_BELLEROPHON_TARGET_Z);
      entity.getPersistentData().remove(TAG_BELLEROPHON_RELAUNCH_TICK);
      entity.setBellerophonRideUntil(0L);
      if (pegasus.isAlive()) {
         pegasus.discard();
      }
   }

   private static Vec3 computePegasusVelocity(MedusaEntity entity, MedusaPegasusEntity pegasus, LivingEntity target, long now) {
      Vec3 aim;
      double speed;
      if (now < entity.getPersistentData().getLong(TAG_BELLEROPHON_CHARGE_UNTIL)) {
         Vec3 chargeTarget = new Vec3(
            entity.getPersistentData().getDouble(TAG_BELLEROPHON_TARGET_X),
            entity.getPersistentData().getDouble(TAG_BELLEROPHON_TARGET_Y),
            entity.getPersistentData().getDouble(TAG_BELLEROPHON_TARGET_Z)
         );
         aim = chargeTarget.subtract(pegasus.position());
         speed = 1.15;
      } else {
         if (target == null || !target.isAlive()) {
            return Vec3.ZERO;
         }
         Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         Vec3 toTarget = targetCenter.subtract(pegasus.position());
         double distance = toTarget.length();
         if (distance < 1.0E-4) {
            toTarget = entity.getLookAngle();
            distance = toTarget.length();
         }
         Vec3 passPoint = targetCenter.add(toTarget.normalize().scale(Math.max(1.0, BELLEROPHON_CHARGE_DISTANCE - Math.min(distance, BELLEROPHON_CHARGE_DISTANCE))));
         aim = passPoint.subtract(pegasus.position());
         speed = 0.95;
      }
      if (aim.lengthSqr() < 1.0E-4) {
         aim = entity.getLookAngle();
      }
      aim = aim.normalize();
      return new Vec3(aim.x * speed, Math.max(-0.25, Math.min(0.35, aim.y * speed)), aim.z * speed);
   }

   private static void applyChargeHits(MedusaEntity entity, MedusaPegasusEntity pegasus, long now) {
      AABB hitBox = pegasus.getBoundingBox().inflate(1.5, 0.8, 1.5);
      for (LivingEntity victim : pegasus.level().getEntitiesOfClass(LivingEntity.class, hitBox, target -> isChargeVictim(entity, target, TAG_BELLEROPHON_HIT_UNTIL, now))) {
         victim.getPersistentData().putLong(TAG_BELLEROPHON_HIT_UNTIL, now + 20L);
         victim.hurt(entity.damageSources().generic(), 300.0F);
         pushAway(pegasus, victim, 1.2, 0.45);
         if (entity.isEyesReleased()) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, false, true, true));
         }
      }
   }

   private static void applyRideCollisionHits(MedusaEntity entity, MedusaPegasusEntity pegasus, long now) {
      AABB hitBox = pegasus.getBoundingBox().inflate(1.1, 0.8, 1.1);
      for (LivingEntity victim : pegasus.level().getEntitiesOfClass(LivingEntity.class, hitBox, target -> isChargeVictim(entity, target, TAG_PEGASUS_COLLISION_HIT_UNTIL, now))) {
         victim.getPersistentData().putLong(TAG_PEGASUS_COLLISION_HIT_UNTIL, now + 10L);
         victim.hurt(entity.damageSources().generic(), 50.0F);
         pushAway(pegasus, victim, 1.0, 0.3);
      }
   }

   private static void emitShockwave(MedusaEntity entity, MedusaPegasusEntity pegasus) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      AABB area = pegasus.getBoundingBox().inflate(5.0, 2.0, 5.0);
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, target -> target != entity && target != pegasus && target.isAlive() && !target.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(target))) {
         victim.hurt(entity.damageSources().generic(), 100.0F);
         pushAway(pegasus, victim, 1.2, 0.55);
      }
      level.sendParticles(ParticleTypes.EXPLOSION, pegasus.getX(), pegasus.getY() + 0.4, pegasus.getZ(), 6, 1.8, 0.4, 1.8, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, pegasus.getX(), pegasus.getY() + 0.2, pegasus.getZ(), 32, 2.3, 0.2, 2.3, 0.08);
      level.playSound(null, pegasus.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 0.8F);
   }

   private static void performCloseRangePressure(MedusaEntity entity, LivingEntity target, double distance, long now) {
      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
      if (distance > 3.4) {
         ServantNavigationHelper.moveToTargetThrottled(
            entity,
            target,
            1.05,
            now,
            ServantNavigationHelper.SHORT_REPATH_INTERVAL,
            0.65,
            "MedusaClosePressurePath"
         );
         return;
      }

      ServantNavigationHelper.stopIfMoving(entity);
      entity.getMoveControl().strafe(0.25F, entity.getRandom().nextBoolean() ? 0.9F : -0.9F);

      if (!entity.isPerformingAction() && now - entity.getPersistentData().getLong(TAG_LAST_BASIC_MAUL_TICK) >= BASIC_MAUL_COOLDOWN) {
         entity.getPersistentData().putLong(TAG_LAST_BASIC_MAUL_TICK, now);
         entity.triggerBasicAttackAnimation();
         target.invulnerableTime = 0;
         float maulDamage = entity.isEyesReleased() ? 5.5F : 4.0F;
         target.hurt(entity.damageSources().mobAttack(entity), maulDamage);
         target.invulnerableTime = 0;
         applyDivinityDamage(entity, target);
         if (entity.getPersistentData().getLong(TAG_MONSTER_STRENGTH_UNTIL) > now) {
            target.hurt(entity.damageSources().generic(), 2.5F);
         }
         pushAway(entity, target, 0.22, 0.04);
         if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(
               ParticleTypes.SWEEP_ATTACK,
               target.getX(),
               target.getY() + target.getBbHeight() * 0.42,
               target.getZ(),
               1,
               0.0,
               0.0,
               0.0,
               0.0
            );
         }
      } else if (entity.doHurtTarget(target)) {
         applyDivinityDamage(entity, target);
      }
   }

   private static boolean shouldUseCybele(MedusaEntity entity, LivingEntity target, double distance, boolean highThreat, long now) {
      return distance <= 5.5
         && entity.getSensing().hasLineOfSight(target)
         && highThreat
         && entity.getRandom().nextFloat() < CYBELE_USE_CHANCE
         && now - entity.getPersistentData().getLong(TAG_LAST_CYBELE_TICK) >= CYBELE_COOLDOWN;
   }

   private static boolean shouldUseMonsterStrength(MedusaEntity entity, double distance, long now) {
      return distance <= 4.5
         && entity.getCurrentMp() >= 5.0
         && now - entity.getPersistentData().getLong(TAG_LAST_MONSTER_STRENGTH_TICK) >= MONSTER_STRENGTH_COOLDOWN;
   }

   private static boolean shouldUseViperRush(MedusaEntity entity, LivingEntity target, double distance, long now) {
      return distance >= 3.5
         && distance <= 7.5
         && entity.getSensing().hasLineOfSight(target)
         && now - entity.getPersistentData().getLong(TAG_LAST_VIPER_RUSH_TICK) >= VIPER_RUSH_COOLDOWN
         && entity.getRandom().nextFloat() < (entity.isEyesReleased() ? 0.62F : 0.44F);
   }

   private static boolean shouldUseSerpentStep(MedusaEntity entity, LivingEntity target, double distance, long now) {
      return distance >= 2.0
         && distance <= 5.0
         && entity.getSensing().hasLineOfSight(target)
         && now - entity.getPersistentData().getLong(TAG_LAST_SERPENT_STEP_TICK) >= SERPENT_STEP_COOLDOWN
         && entity.getRandom().nextFloat() < (entity.isEyesReleased() ? 0.58F : 0.4F);
   }

   private static boolean shouldUsePredatorLoop(MedusaEntity entity, LivingEntity target, double distance, long now) {
      return distance <= 3.2
         && now - entity.getPersistentData().getLong(TAG_LAST_PREDATOR_LOOP_TICK) >= PREDATOR_LOOP_COOLDOWN
         && entity.getRandom().nextFloat() < (entity.isEyesReleased() ? 0.72F : 0.5F);
   }

   private static boolean shouldUseFrenzyTear(MedusaEntity entity, double distance, long now) {
      return distance <= 2.4
         && now - entity.getPersistentData().getLong(TAG_LAST_FRENZY_TEAR_TICK) >= FRENZY_TEAR_COOLDOWN;
   }

   private static boolean shouldUseCharm(MedusaEntity entity, LivingEntity target, double distance, long now) {
      return distance <= 5.5
         && distance >= 2.5
         && entity.getCurrentMp() >= 10.0
         && now - entity.getPersistentData().getLong(TAG_LAST_CHARM_TICK) >= CHARM_COOLDOWN
         && isCharmTarget(target);
   }

   private static boolean shouldUseBloodfort(MedusaEntity entity, LivingEntity target, int nearbyEnemyCount, boolean highThreat, long now) {
      return entity.distanceTo(target) <= BLOODFORT_RADIUS_VALUE
         && entity.getCurrentMp() >= 20.0
         && now - entity.getPersistentData().getLong(TAG_LAST_BLOODFORT_TICK) >= BLOODFORT_COOLDOWN
         && hasAbsorbableTargets(entity, BLOODFORT_RADIUS_VALUE)
         && (
            highThreat
               || nearbyEnemyCount >= 2
               || target instanceof ServantEntity
               || target.getMaxHealth() >= 140.0F
               || entity.getHealth() <= entity.getMaxHealth() * 0.45F
         );
   }

   private static boolean shouldUseBloodfortNp(MedusaEntity entity, LivingEntity target, int nearbyEnemyCount, boolean highThreat, long now) {
      return (
            nearbyEnemyCount >= 3
               || target instanceof ServantEntity && (highThreat || target.getMaxHealth() >= 180.0F)
               || entity.getHealth() <= entity.getMaxHealth() * 0.3F
         )
         && ServantCombatSystem.canUseNoblePhantasm(entity)
         && entity.getCurrentMp() >= 40.0
         && now - entity.getPersistentData().getLong(TAG_LAST_BLOODFORT_NP_TICK) >= BLOODFORT_NP_COOLDOWN
         && hasAbsorbableTargets(entity, BLOODFORT_RADIUS_VALUE);
   }

   private static boolean shouldUseBellerophon(MedusaEntity entity, LivingEntity target, double distance, int nearbyEnemyCount, boolean highThreat, long now) {
      return distance <= BELLEROPHON_CHARGE_DISTANCE + 0.5
         && entity.getCurrentMp() >= 60.0
         && ServantCombatSystem.canUseNoblePhantasm(entity)
         && now - entity.getPersistentData().getLong(TAG_LAST_BELLEROPHON_TICK) >= BELLEROPHON_COOLDOWN
         && (nearbyEnemyCount >= 3 || highThreat || target.hasEffect(ModMobEffects.PETRIFIED));
   }

   private static boolean shouldUseChainSnare(MedusaEntity entity, LivingEntity target, double distance, long now) {
      return distance >= 2.8
         && distance <= 7.0
         && entity.getSensing().hasLineOfSight(target)
         && now - entity.getPersistentData().getLong(TAG_LAST_CHAIN_SNARE_TICK) >= CHAIN_SNARE_COOLDOWN;
   }

   private static void tickCharmAttraction(MedusaEntity entity, long now) {
      String ownerId = entity.getUUID().toString();
      for (LivingEntity living : entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(20.0, 6.0, 20.0), target -> target.isAlive())) {
         if (!ownerId.equals(living.getPersistentData().getString(TAG_CHARM_OWNER))) {
            continue;
         }
         long until = living.getPersistentData().getLong(TAG_CHARM_UNTIL);
         if (until <= now) {
            clearCharm(living);
            continue;
         }
         if (living instanceof net.minecraft.world.entity.Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().moveTo(entity, 0.65);
         }
         Vec3 toward = entity.position().subtract(living.position());
         if (toward.lengthSqr() > 4.0) {
            toward = toward.normalize().scale(0.08);
            living.push(toward.x, 0.02, toward.z);
            living.hurtMarked = true;
         }
      }
   }

   private static void clearCharm(LivingEntity living) {
      living.getPersistentData().remove(TAG_CHARM_OWNER);
      living.getPersistentData().remove(TAG_CHARM_UNTIL);
   }

   private static void updateMountedBonuses(MedusaEntity entity) {
      boolean riding = entity.isRidingPegasus();
      applyMountedModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), RIDING_SPEED_ID, 0.5, riding);
      applyMountedModifier(entity.getAttribute(Attributes.ARMOR), RIDING_ARMOR_ID, 0.2, riding);
   }

   private static void applyMountedModifier(AttributeInstance attribute, ResourceLocation id, double amount, boolean active) {
      if (attribute == null) {
         return;
      }
      if (active) {
         if (attribute.getModifier(id) == null) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      } else {
         attribute.removeModifier(id);
      }
   }

   private static boolean hasAbsorbableTargets(MedusaEntity entity, double radius) {
      Vec3 center = entity.position().add(0.0, 0.1, 0.0);
      return !entity.level()
         .getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius, radius, radius), target -> isBloodfortTarget(entity, center, radius, target))
         .isEmpty();
   }

   private static boolean isAbsorbableTarget(MedusaEntity entity, LivingEntity target) {
      if (target == null || !target.isAlive() || target == entity || target.isAlliedTo(entity) || entity.isAlliedTo(target) || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      return true;
   }

   private static boolean isBloodfortTarget(MedusaEntity entity, Vec3 center, double radius, LivingEntity target) {
      return isAbsorbableTarget(entity, target) && isInsideBloodfortHemisphere(center, radius, target);
   }

   private static boolean isInsideBloodfortHemisphere(Vec3 center, double radius, LivingEntity target) {
      double sampleY = Mth.clamp(target.getY() + target.getBbHeight() * 0.35, center.y, center.y + radius);
      if (target.getY() + target.getBbHeight() < center.y - 0.25) {
         return false;
      }
      double dx = target.getX() - center.x;
      double dy = sampleY - center.y;
      double dz = target.getZ() - center.z;
      return dx * dx + dy * dy + dz * dz <= radius * radius;
   }

   private static void applyBloodfortDebuffs(LivingEntity victim, boolean noblePhantasm) {
      int duration = noblePhantasm ? 60 : 40;
      int slowness = noblePhantasm ? 2 : 1;
      int weakness = noblePhantasm ? 1 : 0;
      int fatigue = noblePhantasm ? 1 : 0;
      victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, slowness, false, true, true));
      victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, weakness, false, true, true));
      victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, fatigue, false, true, true));
   }

   private static void drainBloodfortMana(LivingEntity victim, boolean noblePhantasm) {
      double manaDrain = noblePhantasm ? 18.0 : 10.0;
      if (victim instanceof ServantEntity servant) {
         servant.setCurrentMp(Math.max(0.0, servant.getCurrentMp() - manaDrain));
         return;
      }
      if (victim instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.is_magus || vars.player_max_mana > 0.0) {
            vars.player_mana = Math.max(0.0, vars.player_mana - manaDrain);
            vars.syncMana(player);
         }
      }
   }

   private static boolean isCharmTarget(LivingEntity target) {
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target) || MagicResistanceHelper.hasMagicResistance(target)) {
         return false;
      }
      if (target instanceof ServantEntity servant) {
         return servant.getDefinition() != null && servant.getDefinition().traits().contains(ServantTraitTag.MALE);
      }
      return target instanceof Player
         || target instanceof AbstractVillager
         || target instanceof WanderingTrader
         || target instanceof AbstractIllager
         || target instanceof Witch;
   }

   private static boolean isChargeVictim(MedusaEntity entity, LivingEntity target, String cooldownTag, long now) {
      return target != entity
         && target.isAlive()
         && !target.isAlliedTo(entity)
         && !EntityUtils.isImmunePlayerTarget(target)
         && target.getPersistentData().getLong(cooldownTag) <= now;
   }

   private static int countNearbyHostiles(MedusaEntity entity, double radius) {
      return entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(radius), target -> target != entity && target.isAlive() && !target.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(target)).size();
   }

   private static LivingEntity findEmergencyTarget(MedusaEntity entity) {
      return entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(20.0), target -> target != entity && target.isAlive() && !target.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(target)).stream().findFirst().orElse(null);
   }

   private static void applyPetrified(LivingEntity target, int durationTicks) {
      target.addEffect(new MobEffectInstance(ModMobEffects.PETRIFIED, MagicResistanceHelper.applyDebuffResistance(target, durationTicks), 0, false, true, true));
   }

   private static void applyHeavyPressure(LivingEntity target, int durationTicks) {
      int duration = MagicResistanceHelper.applyDebuffResistance(target, durationTicks);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 2, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 2, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 1, false, true, true));
      applyCybelePressureModifier(target.getAttribute(Attributes.ATTACK_DAMAGE), CYBELE_PRESSURE_ATTACK_ID, -0.2);
      applyCybelePressureModifier(target.getAttribute(Attributes.MOVEMENT_SPEED), CYBELE_PRESSURE_SPEED_ID, -0.1);
      applyCybelePressureModifier(target.getAttribute(Attributes.ARMOR), CYBELE_PRESSURE_ARMOR_ID, -0.2);
      applyCybelePressureModifier(target.getAttribute(Attributes.ARMOR_TOUGHNESS), CYBELE_PRESSURE_TOUGHNESS_ID, -0.2);
      target.getPersistentData().putLong(TAG_CYBELE_PRESSURE_UNTIL, target.level().getGameTime() + duration);
      TYPE_MOON_WORLD.queueServerWork(duration, () -> clearCybelePressureIfExpired(target));
   }

   private static void applyVisualSuppression(LivingEntity target, int durationTicks) {
      int duration = MagicResistanceHelper.applyDebuffResistance(target, durationTicks);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1, false, true, true));
   }

   private static boolean failsCybeleSave(MedusaEntity entity, LivingEntity target, MagicResistanceRank rank, boolean released) {
      float failChance = switch (rank) {
         case A -> released ? 0.52F : 0.38F;
         case B -> released ? 0.86F : 0.72F;
         default -> 1.0F;
      };
      failChance -= MagicResistanceHelper.getDebuffResistance(target) * (rank == MagicResistanceRank.A ? 0.45F : 0.35F);
      return entity.getRandom().nextFloat() <= Mth.clamp(failChance, 0.15F, 0.95F);
   }

   private static void applyCybelePressureModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(id);
      attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
   }

   private static void clearCybelePressureIfExpired(LivingEntity target) {
      if (target == null || !target.isAlive()) {
         return;
      }
      if (target.getPersistentData().getLong(TAG_CYBELE_PRESSURE_UNTIL) > target.level().getGameTime()) {
         return;
      }
      removeModifier(target.getAttribute(Attributes.ATTACK_DAMAGE), CYBELE_PRESSURE_ATTACK_ID);
      removeModifier(target.getAttribute(Attributes.MOVEMENT_SPEED), CYBELE_PRESSURE_SPEED_ID);
      removeModifier(target.getAttribute(Attributes.ARMOR), CYBELE_PRESSURE_ARMOR_ID);
      removeModifier(target.getAttribute(Attributes.ARMOR_TOUGHNESS), CYBELE_PRESSURE_TOUGHNESS_ID);
      target.getPersistentData().remove(TAG_CYBELE_PRESSURE_UNTIL);
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }

   private static boolean canNegateIncomingHit(MedusaEntity entity, DamageSource source) {
      if (entity == null || source == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      if (source.getEntity() == null && source.getDirectEntity() == null) {
         return false;
      }
      return !source.is(DamageTypes.FELL_OUT_OF_WORLD)
         && !source.is(DamageTypes.GENERIC_KILL)
         && !source.is(DamageTypes.FALL)
         && !source.is(DamageTypes.DROWN)
         && !source.is(DamageTypes.FREEZE)
         && !source.is(DamageTypes.IN_FIRE)
         && !source.is(DamageTypes.ON_FIRE)
         && !source.is(DamageTypes.LAVA)
         && !source.is(DamageTypes.IN_WALL);
   }

   private static void applyDivinityDamage(MedusaEntity entity, LivingEntity target) {
      if (entity.getPersistentData().getBoolean("DivinityActive")) {
         float extra = entity.getPersistentData().getFloat("DivinityFlatDamage");
         if (extra > 0.0F) {
            target.hurt(entity.damageSources().magic(), extra);
         }
      }
   }

   private static void clearTransientZoneTargets(MedusaEntity entity) {
      if (entity.getPersistentData().getLong(TAG_BLOODFORT_UNTIL) <= entity.level().getGameTime()) {
         entity.getPersistentData().remove(TAG_BLOODFORT_NP_ACTIVE);
      }
   }

   private static void tryEscapeChainSnare(MedusaEntity entity, LivingEntity target) {
      if (target == null || !target.isAlive()) {
         return;
      }
      target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      target.removeEffect(MobEffects.WEAKNESS);
      Vec3 away = target.position().subtract(entity.position());
      if (away.lengthSqr() > 1.0E-4) {
         away = away.normalize();
         target.push(away.x * 0.6, 0.18, away.z * 0.6);
         target.hurtMarked = true;
      }
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(
            ParticleTypes.CLOUD,
            target.getX(),
            target.getY() + target.getBbHeight() * 0.5,
            target.getZ(),
            7,
            0.2,
            0.25,
            0.2,
            0.04
         );
         level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 0.75F, 1.0F);
      }
   }

   private static void performViperRush(MedusaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      entity.getPersistentData().putLong(TAG_LAST_VIPER_RUSH_TICK, now);
      entity.triggerUppercutAnimation();
      Vec3 dashDir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dashDir.x, 0.0, dashDir.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return;
      }
      horizontal = horizontal.normalize();
      entity.faceVector(horizontal);
      entity.setDeltaMovement(horizontal.x * 1.1, Math.max(entity.getDeltaMovement().y, 0.12), horizontal.z * 1.1);
      entity.hasImpulse = true;
      entity.getNavigation().moveTo(entity.getX() + horizontal.x * 5.5, entity.getY(), entity.getZ() + horizontal.z * 5.5, 1.35);

      AABB hitBox = entity.getBoundingBox().expandTowards(horizontal.scale(4.8)).inflate(1.0, 0.8, 1.0);
      for (LivingEntity victim : level.getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         victim.hurt(entity.damageSources().mobAttack(entity), entity.isEyesReleased() ? 12.0F : 9.0F);
         applyDivinityDamage(entity, victim);
         pushAway(entity, victim, 0.9, 0.2);
      }
      spawnDashTrail(level, entity, horizontal, 4.8, ParticleTypes.CRIT);
   }

   private static void performSerpentStep(MedusaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      entity.getPersistentData().putLong(TAG_LAST_SERPENT_STEP_TICK, now);
      entity.triggerHorizontalSwingAnimation();
      Vec3 toTarget = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return;
      }
      horizontal = horizontal.normalize();
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize().scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);
      Vec3 move = horizontal.scale(0.75).add(side.scale(0.9)).normalize();
      entity.faceVector(move);
      entity.setDeltaMovement(move.x * 0.95, Math.max(entity.getDeltaMovement().y, 0.1), move.z * 0.95);
      entity.hasImpulse = true;
      entity.getNavigation().moveTo(entity.getX() + move.x * 4.0, entity.getY(), entity.getZ() + move.z * 4.0, 1.28);

      AABB hitBox = entity.getBoundingBox().expandTowards(move.scale(3.8)).inflate(1.4, 0.8, 1.4);
      for (LivingEntity victim : level.getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         victim.hurt(entity.damageSources().mobAttack(entity), entity.isEyesReleased() ? 10.0F : 8.0F);
         applyDivinityDamage(entity, victim);
         pushAway(entity, victim, 0.65, 0.18);
      }
      spawnDashTrail(level, entity, move, 3.8, ParticleTypes.SWEEP_ATTACK);
   }

   private static void performPredatorLoop(MedusaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      entity.getPersistentData().putLong(TAG_LAST_PREDATOR_LOOP_TICK, now);
      entity.triggerBasicAttackAnimation();
      Vec3 around = entity.position().subtract(target.position());
      Vec3 horizontal = new Vec3(around.x, 0.0, around.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(1.0, 0.0, 0.0);
      } else {
         horizontal = horizontal.normalize();
      }
      Vec3 tangent = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize().scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);
      Vec3 move = tangent.add(horizontal.scale(0.2)).normalize();
      entity.faceVector(move);
      entity.setDeltaMovement(move.x * 0.88, Math.max(entity.getDeltaMovement().y, 0.08), move.z * 0.88);
      entity.hasImpulse = true;
      entity.getNavigation().stop();

      AABB hitBox = target.getBoundingBox().inflate(1.35, 0.75, 1.35);
      for (LivingEntity victim : level.getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         victim.hurt(entity.damageSources().mobAttack(entity), entity.isEyesReleased() ? 9.0F : 7.0F);
         applyDivinityDamage(entity, victim);
         if (entity.getPersistentData().getLong(TAG_MONSTER_STRENGTH_UNTIL) > now) {
            victim.hurt(entity.damageSources().generic(), 3.0F);
         }
      }
      level.sendParticles(
         ParticleTypes.CRIT,
         target.getX(),
         target.getY() + target.getBbHeight() * 0.45,
         target.getZ(),
         8,
         0.35,
         0.25,
         0.35,
         0.04
      );
   }

   private static void performFrenzyTear(MedusaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || target == null || !target.isAlive()) {
         return;
      }
      entity.getPersistentData().putLong(TAG_LAST_FRENZY_TEAR_TICK, now);
      entity.triggerBasicAttackAnimation();
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), entity.isEyesReleased() ? 6.0F : 4.5F);
      target.invulnerableTime = 0;
      applyDivinityDamage(entity, target);
      if (entity.getPersistentData().getLong(TAG_MONSTER_STRENGTH_UNTIL) > now) {
         target.hurt(entity.damageSources().generic(), 2.0F);
      }
      pushAway(entity, target, 0.28, 0.05);
      level.sendParticles(
         ParticleTypes.SWEEP_ATTACK,
         target.getX(),
         target.getY() + target.getBbHeight() * 0.42,
         target.getZ(),
         1,
         0.0,
         0.0,
         0.0,
         0.0
      );
      level.sendParticles(
         ParticleTypes.CRIT,
         target.getX(),
         target.getY() + target.getBbHeight() * 0.48,
         target.getZ(),
         5,
         0.14,
         0.14,
         0.14,
         0.03
      );
   }

   private static void spawnDashTrail(ServerLevel level, MedusaEntity entity, Vec3 dir, double length, net.minecraft.core.particles.ParticleOptions particle) {
      for (double t = 0.5; t <= length; t += 0.7) {
         Vec3 pos = entity.position().add(dir.scale(t)).add(0.0, entity.getBbHeight() * 0.45, 0.0);
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.0);
      }
   }

   private static void tickRapidAssaultDamage(MedusaEntity entity, long now) {
      Vec3 movement = entity.getDeltaMovement();
      double horizontalSpeed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
      if (horizontalSpeed < 0.15 || entity.isRidingPegasus()) {
         return;
      }

      AABB hitBox = entity.getBoundingBox().inflate(1.45, 0.6, 1.45);
      for (LivingEntity victim : entity.level().getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         target -> isChargeVictim(entity, target, TAG_RAPID_ASSAULT_HIT_UNTIL, now)
      )) {
         victim.getPersistentData().putLong(TAG_RAPID_ASSAULT_HIT_UNTIL, now + 6L);
         float slashDamage = entity.isEyesReleased() ? 8.0F : 6.0F;
         victim.hurt(entity.damageSources().mobAttack(entity), slashDamage);
         if (entity.getPersistentData().getLong(TAG_MONSTER_STRENGTH_UNTIL) > now) {
            victim.hurt(entity.damageSources().generic(), 4.0F);
         }
         applyDivinityDamage(entity, victim);
         pushAway(entity, victim, 0.45, 0.12);
         if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(
               ParticleTypes.SWEEP_ATTACK,
               victim.getX(),
               victim.getY() + victim.getBbHeight() * 0.45,
               victim.getZ(),
               1,
               0.0,
               0.0,
               0.0,
               0.0
            );
            level.sendParticles(
               ParticleTypes.CRIT,
               victim.getX(),
               victim.getY() + victim.getBbHeight() * 0.5,
               victim.getZ(),
               4,
               0.15,
               0.15,
               0.15,
               0.03
            );
         }
      }
   }

   private static void updateCloseCombatBonuses(MedusaEntity entity, boolean engaged) {
      if (entity.isRidingPegasus()) {
         clearCombatRush(entity);
         return;
      }
      AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (movement == null) {
         return;
      }
      if (engaged) {
         double rushAmount = entity.isEyesReleased() ? 0.34 : 0.24;
         AttributeModifier existing = movement.getModifier(COMBAT_RUSH_SPEED_ID);
         if (existing == null || Math.abs(existing.amount() - rushAmount) > 1.0E-6) {
            movement.removeModifier(COMBAT_RUSH_SPEED_ID);
            movement.addTransientModifier(new AttributeModifier(COMBAT_RUSH_SPEED_ID, rushAmount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      } else {
         movement.removeModifier(COMBAT_RUSH_SPEED_ID);
      }
   }

   private static void clearCombatRush(MedusaEntity entity) {
      AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (movement != null) {
         movement.removeModifier(COMBAT_RUSH_SPEED_ID);
      }
   }

   private static void spawnChainLine(ServerLevel level, Vec3 from, Vec3 to) {
      for (double t = 0.0; t <= 1.0; t += 0.1) {
         Vec3 pos = from.lerp(to, t);
         level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
   }

   private static void spawnBloodfortShell(ServerLevel level, Vec3 center, double radius) {
      int ringSamples = 72;
      for (int ring = 0; ring <= 4; ring++) {
         double normalized = ring / 4.0;
         double phi = normalized * (Math.PI / 2.0);
         double ringRadius = Math.sin(phi) * radius;
         double y = center.y + Math.cos(phi) * radius;
         for (int i = 0; i < ringSamples; i++) {
            double theta = (Math.PI * 2.0 * i) / ringSamples;
            double x = center.x + Math.cos(theta) * ringRadius;
            double z = center.z + Math.sin(theta) * ringRadius;
            level.sendParticles(BLOODFORT_PARTICLE, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            if ((i & 3) == 0) {
               level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
            }
            if ((i & 1) == 0) {
               level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.03, 0.02, 0.03, 0.0);
            }
            if ((i % 6) == 0) {
               level.sendParticles(BLOODFORT_LINK_PARTICLE, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
            }
         }
      }
   }

   private static void spawnBloodfortInteriorHaze(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm) {
      int hazeCount = noblePhantasm ? 90 : 48;
      double verticalSpread = noblePhantasm ? radius * 0.95 : radius * 0.8;
      for (int i = 0; i < hazeCount; i++) {
         double angle = level.random.nextDouble() * Math.PI * 2.0;
         double dist = Math.sqrt(level.random.nextDouble()) * radius * 0.92;
         double x = center.x + Math.cos(angle) * dist;
         double z = center.z + Math.sin(angle) * dist;
         double y = center.y + 0.3 + level.random.nextDouble() * verticalSpread;
         level.sendParticles(BLOODFORT_PARTICLE, x, y, z, 1, 0.06, 0.03, 0.06, 0.0);
         if ((i % 3) == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.04, 0.02, 0.04, 0.0);
         }
         if ((i % 5) == 0) {
            level.sendParticles(ParticleTypes.DRIPPING_LAVA, x, y, z, 1, 0.02, 0.01, 0.02, 0.0);
         }
      }
   }

   private static void spawnBloodfortCastBurst(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm) {
      int burstCount = noblePhantasm ? 120 : 70;
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 1.2, center.z, burstCount / 3, radius * 0.25, radius * 0.12, radius * 0.25, 0.06);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 1.0, center.z, burstCount / 2, radius * 0.22, 1.2, radius * 0.22, 0.04);
      level.sendParticles(BLOODFORT_PARTICLE, center.x, center.y + 1.4, center.z, burstCount, radius * 0.25, 1.4, radius * 0.25, 0.0);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.8, center.z, burstCount / 4, radius * 0.18, 0.8, radius * 0.18, 0.01);
   }

   private static void startBloodfortSummonSequence(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm) {
      spawnBloodfortCastBurst(level, center, radius, noblePhantasm);
      spawnBloodfortGroundSigil(level, center, radius, noblePhantasm);
      spawnBloodfortNodeClusters(level, center, radius, noblePhantasm);
      TYPE_MOON_WORLD.queueServerWork(5, () -> {
         if (level.isLoaded(BlockPos.containing(center.x, center.y, center.z))) {
            spawnBloodfortNodeLinks(level, center, radius, noblePhantasm);
         }
      });
      TYPE_MOON_WORLD.queueServerWork(10, () -> {
         if (level.isLoaded(BlockPos.containing(center.x, center.y, center.z))) {
            spawnBloodfortRisingShell(level, center, radius, noblePhantasm);
            spawnBloodfortVerticalPulse(level, center, radius, noblePhantasm);
         }
      });
      TYPE_MOON_WORLD.queueServerWork(16, () -> {
         if (level.isLoaded(BlockPos.containing(center.x, center.y, center.z))) {
            spawnBloodfortGroundSigil(level, center, radius, noblePhantasm);
            spawnBloodfortNodeLinks(level, center, radius, noblePhantasm);
         }
      });
   }

   private static void spawnBloodfortGroundSigil(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm) {
      double y = center.y + 0.04;
      spawnBloodfortRing(level, center, y, radius * 0.82, 90, BLOODFORT_SIGIL_PARTICLE);
      spawnBloodfortRing(level, center, y, radius * 0.58, 70, BLOODFORT_LINK_PARTICLE);
      spawnBloodfortRing(level, center, y, radius * 0.26, 42, BLOODFORT_NODE_PARTICLE);
      Vec3[] outerPentagram = createRegularPolygon(center, y, radius * 0.64, 5, -Math.PI / 2.0);
      for (int i = 0; i < outerPentagram.length; i++) {
         Vec3 from = outerPentagram[i];
         Vec3 to = outerPentagram[(i + 2) % outerPentagram.length];
         spawnBloodfortLine(level, from, to, BLOODFORT_LINK_PARTICLE, 0.42);
         spawnBloodfortLine(level, from, center.add(0.0, 0.04, 0.0), BLOODFORT_SIGIL_PARTICLE, 0.5);
      }
      if (noblePhantasm) {
         Vec3[] innerPentagon = createRegularPolygon(center, y, radius * 0.38, 5, Math.PI / 10.0);
         for (int i = 0; i < innerPentagon.length; i++) {
            spawnBloodfortLine(level, innerPentagon[i], innerPentagon[(i + 1) % innerPentagon.length], BLOODFORT_SIGIL_PARTICLE, 0.35);
         }
      }
      spawnBloodfortPeripheralSigils(level, center, radius, noblePhantasm);
   }

   private static void spawnBloodfortNodeClusters(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm) {
      Vec3[] majorNodes = createRegularPolygon(center, center.y + 0.06, radius * 0.74, 5, -Math.PI / 2.0);
      Vec3[] minorNodes = createRegularPolygon(center, center.y + 0.06, radius * 0.44, 5, Math.PI / 10.0);
      for (Vec3 node : majorNodes) {
         spawnBloodfortNodeCluster(level, node, 7, BLOODFORT_NODE_PARTICLE);
         level.sendParticles(ParticleTypes.FLAME, node.x, node.y + 0.1, node.z, 2, 0.06, 0.02, 0.06, 0.0);
      }
      for (Vec3 node : minorNodes) {
         spawnBloodfortNodeCluster(level, node, noblePhantasm ? 5 : 3, BLOODFORT_SIGIL_PARTICLE);
      }
   }

   private static void spawnBloodfortNodeLinks(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm) {
      Vec3[] majorNodes = createRegularPolygon(center, center.y + 0.06, radius * 0.74, 5, -Math.PI / 2.0);
      Vec3[] minorNodes = createRegularPolygon(center, center.y + 0.06, radius * 0.44, 5, Math.PI / 10.0);
      for (int i = 0; i < majorNodes.length; i++) {
         spawnBloodfortLine(level, majorNodes[i], majorNodes[(i + 1) % majorNodes.length], BLOODFORT_LINK_PARTICLE, 0.38);
         spawnBloodfortLine(level, majorNodes[i], minorNodes[i], BLOODFORT_NODE_PARTICLE, 0.34);
         spawnBloodfortLine(level, minorNodes[i], center.add(0.0, 0.06, 0.0), BLOODFORT_SIGIL_PARTICLE, 0.3);
      }
      if (noblePhantasm) {
         for (int i = 0; i < majorNodes.length; i++) {
            spawnBloodfortLine(level, majorNodes[i], minorNodes[(i + 2) % minorNodes.length], BLOODFORT_SIGIL_PARTICLE, 0.3);
         }
      }
      spawnBloodfortPeripheralLinks(level, center, radius, noblePhantasm, majorNodes);
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.1, center.z, 1, 0.05, 0.02, 0.05, 0.0);
   }

   private static void spawnBloodfortRisingShell(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm) {
      level.addFreshEntity(
         new GravityFieldShellEffectEntity(
            level,
            center.x,
            center.y + 0.02,
            center.z,
            (float)(radius * 1.02),
            (float)(radius * 0.82),
            noblePhantasm ? 0.72F : 0.58F,
            noblePhantasm ? 32 : 26,
            0.86F,
            0.06F,
            0.11F,
            noblePhantasm ? 14 : 10
         )
      );
   }

   private static void spawnBloodfortVerticalPulse(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm) {
      int shafts = noblePhantasm ? 12 : 8;
      for (int i = 0; i < shafts; i++) {
         double angle = (Math.PI * 2.0 * i) / shafts;
         double ringRadius = radius * 0.78;
         double baseX = center.x + Math.cos(angle) * ringRadius;
         double baseZ = center.z + Math.sin(angle) * ringRadius;
         for (double t = 0.0; t <= 1.0; t += 0.12) {
            double y = center.y + t * radius * 0.9;
            level.sendParticles(BLOODFORT_SIGIL_PARTICLE, baseX, y, baseZ, 1, 0.0, 0.0, 0.0, 0.0);
            if ((i & 1) == 0) {
               level.sendParticles(ParticleTypes.FLAME, baseX, y, baseZ, 1, 0.015, 0.02, 0.015, 0.0);
            }
         }
      }
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + radius * 0.45, center.z, noblePhantasm ? 36 : 20, radius * 0.22, radius * 0.3, radius * 0.22, 0.01);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + radius * 0.18, center.z, noblePhantasm ? 28 : 18, radius * 0.28, 0.16, radius * 0.28, 0.01);
   }

   private static void spawnBloodfortRing(ServerLevel level, Vec3 center, double y, double radius, int samples, DustParticleOptions particle) {
      for (int i = 0; i < samples; i++) {
         double theta = (Math.PI * 2.0 * i) / samples;
         double x = center.x + Math.cos(theta) * radius;
         double z = center.z + Math.sin(theta) * radius;
         level.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static Vec3[] createRegularPolygon(Vec3 center, double y, double radius, int sides, double angleOffset) {
      Vec3[] points = new Vec3[sides];
      for (int i = 0; i < sides; i++) {
         double angle = angleOffset + (Math.PI * 2.0 * i) / sides;
         points[i] = new Vec3(center.x + Math.cos(angle) * radius, y, center.z + Math.sin(angle) * radius);
      }
      return points;
   }

   private static void spawnBloodfortLine(ServerLevel level, Vec3 from, Vec3 to, DustParticleOptions particle, double spacing) {
      Vec3 delta = to.subtract(from);
      double distance = delta.length();
      if (distance < 1.0E-4) {
         return;
      }
      Vec3 step = delta.normalize().scale(spacing);
      for (double traveled = 0.0; traveled <= distance; traveled += spacing) {
         Vec3 pos = from.add(step.scale(traveled / spacing));
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0.0);
      }
   }

   private static void spawnBloodfortNodeCluster(ServerLevel level, Vec3 center, int count, DustParticleOptions particle) {
      level.sendParticles(particle, center.x, center.y, center.z, count, 0.08, 0.02, 0.08, 0.0);
      level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.02, center.z, Math.max(1, count / 3), 0.04, 0.02, 0.04, 0.0);
   }

   private static void spawnBloodfortPeripheralSigils(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm) {
      double y = center.y + 0.035;
      int sigilCount = noblePhantasm ? 10 : 7;
      double rotation = level.getGameTime() * (noblePhantasm ? 0.022 : 0.016);
      for (int i = 0; i < sigilCount; i++) {
         double angle = -Math.PI / 2.0 + (Math.PI * 2.0 * i) / sigilCount + (i % 2 == 0 ? 0.11 : -0.09) + rotation;
         double sigilRadius = radius * (0.84 + (i % 3) * 0.035);
         Vec3 sigilCenter = new Vec3(center.x + Math.cos(angle) * sigilRadius, y, center.z + Math.sin(angle) * sigilRadius);
         double miniRadius = radius * (noblePhantasm ? 0.12 : 0.095) * (i % 2 == 0 ? 1.0 : 0.82);
         spawnBloodfortRing(level, sigilCenter, y, miniRadius, 18, BLOODFORT_SIGIL_PARTICLE);
         spawnBloodfortRing(level, sigilCenter, y, miniRadius * 0.52, 10, BLOODFORT_LINK_PARTICLE);
         Vec3[] miniPentagram = createRegularPolygon(sigilCenter, y, miniRadius * 0.72, 5, angle);
         for (int v = 0; v < miniPentagram.length; v++) {
            spawnBloodfortLine(level, miniPentagram[v], miniPentagram[(v + 2) % miniPentagram.length], BLOODFORT_LINK_PARTICLE, 0.18);
         }
         spawnBloodfortRotatingSubArray(level, sigilCenter, miniRadius, angle, noblePhantasm);
         level.sendParticles(BLOODFORT_NODE_PARTICLE, sigilCenter.x, sigilCenter.y, sigilCenter.z, noblePhantasm ? 4 : 2, 0.05, 0.01, 0.05, 0.0);
      }
   }

   private static void spawnBloodfortPeripheralLinks(ServerLevel level, Vec3 center, double radius, boolean noblePhantasm, Vec3[] majorNodes) {
      double y = center.y + 0.06;
      int sigilCount = noblePhantasm ? 10 : 7;
      double rotation = level.getGameTime() * (noblePhantasm ? 0.022 : 0.016);
      Vec3[] peripheral = new Vec3[sigilCount];
      for (int i = 0; i < sigilCount; i++) {
         double angle = -Math.PI / 2.0 + (Math.PI * 2.0 * i) / sigilCount + (i % 2 == 0 ? 0.11 : -0.09) + rotation;
         double sigilRadius = radius * (0.84 + (i % 3) * 0.035);
         peripheral[i] = new Vec3(center.x + Math.cos(angle) * sigilRadius, y, center.z + Math.sin(angle) * sigilRadius);
      }
      for (int i = 0; i < peripheral.length; i++) {
         spawnBloodfortLine(level, peripheral[i], peripheral[(i + 1) % peripheral.length], BLOODFORT_LINK_PARTICLE, 0.24);
         Vec3 nearestMajor = majorNodes[i % majorNodes.length];
         spawnBloodfortLine(level, peripheral[i], nearestMajor, BLOODFORT_SIGIL_PARTICLE, 0.28);
         if (noblePhantasm && (i % 2 == 0)) {
            spawnBloodfortLine(level, peripheral[i], center.add(0.0, 0.06, 0.0), BLOODFORT_LINK_PARTICLE, 0.3);
         }
      }
      for (int i = 0; i < peripheral.length; i++) {
         spawnBloodfortLine(level, peripheral[i], peripheral[(i + 2) % peripheral.length], BLOODFORT_SIGIL_PARTICLE, 0.42);
         if (noblePhantasm || (i % 2 == 0)) {
            spawnBloodfortLine(level, peripheral[i], peripheral[(i + 3) % peripheral.length], BLOODFORT_LINK_PARTICLE, 0.5);
         }
      }
   }

   private static void spawnBloodfortRotatingSubArray(ServerLevel level, Vec3 sigilCenter, double miniRadius, double baseAngle, boolean noblePhantasm) {
      double subRadius = miniRadius * (noblePhantasm ? 1.38 : 1.24);
      double ringAngle = baseAngle + level.getGameTime() * (noblePhantasm ? -0.031 : -0.024);
      Vec3[] triad = createRegularPolygon(sigilCenter, sigilCenter.y, subRadius, 3, ringAngle);
      Vec3[] square = createRegularPolygon(sigilCenter, sigilCenter.y, subRadius * 0.64, 4, -ringAngle * 0.8);
      for (int i = 0; i < triad.length; i++) {
         spawnBloodfortLine(level, triad[i], triad[(i + 1) % triad.length], BLOODFORT_SIGIL_PARTICLE, 0.12);
         spawnBloodfortLine(level, triad[i], sigilCenter, BLOODFORT_LINK_PARTICLE, 0.11);
      }
      for (int i = 0; i < square.length; i++) {
         spawnBloodfortLine(level, square[i], square[(i + 1) % square.length], BLOODFORT_LINK_PARTICLE, 0.1);
      }
      for (Vec3 point : triad) {
         level.sendParticles(BLOODFORT_NODE_PARTICLE, point.x, point.y, point.z, 1, 0.015, 0.0, 0.015, 0.0);
      }
   }

   private static void spawnBloodfortVictimAura(ServerLevel level, LivingEntity victim, float dealt) {
      int bloodCount = Math.max(8, Math.min(22, Math.round(dealt / 2.0F)));
      level.sendParticles(BLOODFORT_PARTICLE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.55, victim.getZ(), bloodCount, 0.25, 0.35, 0.25, 0.0);
      level.sendParticles(ParticleTypes.DRIPPING_LAVA, victim.getX(), victim.getY() + victim.getBbHeight() * 0.4, victim.getZ(), Math.max(4, bloodCount / 3), 0.18, 0.22, 0.18, 0.0);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), Math.max(3, bloodCount / 4), 0.18, 0.2, 0.18, 0.0);
   }

   private static void pushAway(Entity source, LivingEntity victim, double horizontalStrength, double verticalStrength) {
      Vec3 push = victim.position().subtract(source.position());
      double horizontal = Math.sqrt(push.x * push.x + push.z * push.z);
      if (horizontal < 1.0E-4) {
         push = new Vec3(Mth.nextDouble(source.level().random, -1.0, 1.0), 0.0, Mth.nextDouble(source.level().random, -1.0, 1.0));
         horizontal = Math.sqrt(push.x * push.x + push.z * push.z);
      }
      victim.push(push.x / horizontal * horizontalStrength, verticalStrength, push.z / horizontal * horizontalStrength);
      victim.hurtMarked = true;
   }

   private static Vec3 computeInitialChargeTarget(MedusaEntity entity, LivingEntity target) {
      Vec3 origin = entity.position();
      Vec3 aim = target != null && target.isAlive()
         ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(origin)
         : entity.getLookAngle();
      if (aim.lengthSqr() < 1.0E-4) {
         aim = new Vec3(0.0, 0.0, 1.0);
      }
      aim = aim.normalize();
      return origin.add(aim.scale(BELLEROPHON_CHARGE_DISTANCE));
   }

   private static LivingEntity findNearbyChargeTarget(MedusaEntity entity, MedusaPegasusEntity pegasus) {
      AABB searchBox = pegasus.getBoundingBox().inflate(18.0, 4.0, 18.0);
      return pegasus.level().getEntitiesOfClass(
         LivingEntity.class,
         searchBox,
         target -> target != entity
            && target != pegasus
            && target.isAlive()
            && !target.isAlliedTo(entity)
            && !EntityUtils.isImmunePlayerTarget(target)
      ).stream().min((a, b) -> Double.compare(pegasus.distanceToSqr(a), pegasus.distanceToSqr(b))).orElse(null);
   }

   private static void restartBellerophonCharge(MedusaEntity entity, MedusaPegasusEntity pegasus, LivingEntity target, long now) {
      Vec3 chargeTarget = computeInitialChargeTarget(entity, target);
      entity.getPersistentData().putDouble(TAG_BELLEROPHON_TARGET_X, chargeTarget.x);
      entity.getPersistentData().putDouble(TAG_BELLEROPHON_TARGET_Y, chargeTarget.y);
      entity.getPersistentData().putDouble(TAG_BELLEROPHON_TARGET_Z, chargeTarget.z);
      entity.getPersistentData().putLong(TAG_BELLEROPHON_CHARGE_UNTIL, now + CHARGE_TICKS);
      entity.getPersistentData().putBoolean(TAG_BELLEROPHON_SHOCKWAVE_DONE, false);
      entity.getPersistentData().putLong(TAG_BELLEROPHON_RELAUNCH_TICK, now);
      pegasus.setFlyingMode(true);
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, pegasus.getX(), pegasus.getY() + 0.2, pegasus.getZ(), 12, 0.35, 0.12, 0.35, 0.04);
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pegasus.getX(), pegasus.getY() + 0.45, pegasus.getZ(), 6, 0.18, 0.12, 0.18, 0.01);
      }
   }

   private static void applyGroundCruiseMotion(MedusaEntity entity, MedusaPegasusEntity pegasus, long now) {
      float baseYaw = entity.getYRot() + (float)Math.sin(now * 0.08) * 18.0F;
      float yawRad = baseYaw * (float)(Math.PI / 180.0F);
      Vec3 walkDir = new Vec3(-Mth.sin(yawRad), 0.0, Mth.cos(yawRad));
      if (walkDir.lengthSqr() > 1.0E-4) {
         walkDir = walkDir.normalize();
         Vec3 groundMove = new Vec3(walkDir.x * 0.18, Math.min(pegasus.getDeltaMovement().y, 0.0), walkDir.z * 0.18);
         orientChargeActors(entity, pegasus, new Vec3(groundMove.x, 0.0, groundMove.z));
         pegasus.setDeltaMovement(groundMove);
         pegasus.hasImpulse = true;
      } else {
         pegasus.setDeltaMovement(Vec3.ZERO);
      }
   }

   private static void startBellerophonSummonFx(ServerLevel level, MedusaEntity entity, long now) {
      for (int i = 0; i < CHARGE_WINDUP_TICKS; i += 4) {
         final int step = i;
         TYPE_MOON_WORLD.queueServerWork(step, () -> {
            if (!entity.isAlive() || entity.level() != level) {
               return;
            }
            double radius = 0.8 + step * 0.03;
            for (int sample = 0; sample < 14; sample++) {
               double angle = (Math.PI * 2.0 * sample) / 14.0 + step * 0.08;
               double x = entity.getX() + Math.cos(angle) * radius;
               double z = entity.getZ() + Math.sin(angle) * radius;
               double y = entity.getY() + 0.15 + (sample % 3) * 0.25;
               level.sendParticles(SUMMON_LIGHT_PARTICLE, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
               level.sendParticles(SUMMON_GOLD_PARTICLE, x, y + 0.1, z, 1, 0.01, 0.01, 0.01, 0.0);
               if ((sample & 1) == 0) {
                  level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
               }
            }
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0, entity.getZ(), 8, 0.22, 0.45, 0.22, 0.02);
            level.sendParticles(ParticleTypes.GLOW, entity.getX(), entity.getY() + 1.1, entity.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
         });
      }
   }

   private static void spawnPegasusArrivalFx(ServerLevel level, MedusaPegasusEntity pegasus) {
      level.sendParticles(ParticleTypes.FLASH, pegasus.getX(), pegasus.getY() + 1.0, pegasus.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, pegasus.getX(), pegasus.getY() + 0.9, pegasus.getZ(), 10, 0.22, 0.35, 0.22, 0.015);
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pegasus.getX(), pegasus.getY() + 0.9, pegasus.getZ(), 8, 0.25, 0.35, 0.25, 0.015);
      level.sendParticles(SUMMON_LIGHT_PARTICLE, pegasus.getX(), pegasus.getY() + 0.8, pegasus.getZ(), 14, 0.28, 0.42, 0.28, 0.0);
      level.sendParticles(SUMMON_GOLD_PARTICLE, pegasus.getX(), pegasus.getY() + 0.8, pegasus.getZ(), 10, 0.28, 0.42, 0.28, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, pegasus.getX(), pegasus.getY() + 0.2, pegasus.getZ(), 12, 0.45, 0.12, 0.45, 0.03);
      level.playSound(null, pegasus.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.9F, 1.35F);
      level.playSound(null, pegasus.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE, 0.85F, 0.8F);
   }

   private static void orientChargeActors(MedusaEntity entity, MedusaPegasusEntity pegasus, Vec3 desired) {
      if (desired.lengthSqr() < 1.0E-4) {
         return;
      }
      double horizontal = Math.sqrt(desired.x * desired.x + desired.z * desired.z);
      float yaw = (float)(Mth.atan2(desired.z, desired.x) * 180.0F / Math.PI) - 90.0F;
      float pitch = (float)(-(Mth.atan2(desired.y, Math.max(horizontal, 1.0E-4)) * 180.0F / Math.PI));

      pegasus.setYRot(yaw);
      pegasus.yRotO = yaw;
      pegasus.setYHeadRot(yaw);
      pegasus.yHeadRotO = yaw;
      pegasus.yBodyRot = yaw;
      pegasus.yBodyRotO = yaw;
      pegasus.setXRot(Mth.clamp(pitch, -35.0F, 35.0F));
      pegasus.xRotO = pegasus.getXRot();

      entity.setYRot(yaw);
      entity.yRotO = yaw;
      entity.setYHeadRot(yaw);
      entity.yHeadRotO = yaw;
      entity.yBodyRot = yaw;
      entity.yBodyRotO = yaw;
      entity.setXRot(Mth.clamp(pitch, -20.0F, 20.0F));
      entity.xRotO = entity.getXRot();
   }

   private static void breakRideBlocks(MedusaPegasusEntity pegasus, Vec3 desired) {
      if (!(pegasus.level() instanceof ServerLevel level) || desired.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 forward = new Vec3(desired.x, 0.0, desired.z);
      if (forward.lengthSqr() < 1.0E-4) {
         return;
      }
      forward = forward.normalize();
      BlockPos base = pegasus.blockPosition();
      boolean charging = desired.horizontalDistanceSqr() > 1.0;
      int length = charging ? 7 : 4;
      int radius = charging ? 2 : 1;
      int height = charging ? 4 : 2;
      int broken = 0;
      int maxBroken = charging ? 80 : 24;
      for (int i = 0; i < length; i++) {
         BlockPos check = base.offset((int)Math.round(forward.x * (i + 1)), 0, (int)Math.round(forward.z * (i + 1)));
         for (BlockPos pos : BlockPos.betweenClosed(check.offset(-radius, -1, -radius), check.offset(radius, height, radius))) {
            if (destroyRideBlock(level, pos, charging) && ++broken >= maxBroken) {
               break;
            }
         }
         if (broken >= maxBroken) {
            break;
         }
      }
      if (charging && pegasus.tickCount % 3 == 0) {
         level.sendParticles(ParticleTypes.EXPLOSION, pegasus.getX(), pegasus.getY() + 0.4, pegasus.getZ(), 2, 0.35, 0.2, 0.35, 0.0);
         level.sendParticles(ParticleTypes.CLOUD, pegasus.getX(), pegasus.getY() + 0.2, pegasus.getZ(), 12, 0.9, 0.2, 0.9, 0.08);
      }
   }

   private static boolean destroyRideBlock(ServerLevel level, BlockPos pos, boolean charging) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (!state.isAir() && hardness >= 0.0F && hardness < (charging ? 75.0F : 30.0F) && !state.is(Blocks.BEDROCK)) {
         level.removeBlock(pos, false);
         return true;
      }
      return false;
   }

   private static void spawnDefenseFx(MedusaEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(
         ParticleTypes.SWEEP_ATTACK,
         entity.getX(),
         entity.getY() + entity.getBbHeight() * 0.6,
         entity.getZ(),
         2,
         0.0,
         0.0,
         0.0,
         0.0
      );
      level.sendParticles(
         ParticleTypes.CRIT,
         entity.getX(),
         entity.getY() + entity.getBbHeight() * 0.65,
         entity.getZ(),
         10,
         0.28,
         0.28,
         0.28,
         0.06
      );
      level.sendParticles(
         ParticleTypes.CLOUD,
         entity.getX(),
         entity.getY() + 0.25,
         entity.getZ(),
         6,
         0.2,
         0.1,
         0.2,
         0.02
      );
      level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.7F, 1.3F);
   }

   private static void spawnDodgeFx(MedusaEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(
         ParticleTypes.SWEEP_ATTACK,
         entity.getX(),
         entity.getY() + entity.getBbHeight() * 0.5,
         entity.getZ(),
         2,
         0.1,
         0.1,
         0.1,
         0.0
      );
      level.sendParticles(
         ParticleTypes.CLOUD,
         entity.getX(),
         entity.getY() + entity.getBbHeight() * 0.5,
         entity.getZ(),
         8,
         0.15,
         0.25,
         0.15,
         0.03
      );
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.9F, 1.25F);
   }
}
