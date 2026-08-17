package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BaobhanSithHarpItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.baobhan.BaobhanSithDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantLifecycleContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantNoblePhantasmContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class BaobhanSithServantSkills {
   public static final String SERVANT_ID = "baobhan_sith";
   public static final String PROVIDER_ID = "typemoonworld_core";
   public static final String ACTION_CURSE_SHOT = "baobhan_sith_curse_shot";
   public static final String ACTION_BLOOD_SPIKE = "baobhan_sith_blood_spike";
   public static final String ACTION_BLOOD_THORNS = "baobhan_sith_blood_thorns";
   public static final String ACTION_CURSE_VOLLEY = "baobhan_sith_curse_volley";
   public static final String ACTION_FINGERTIP_DANCE = "baobhan_sith_fingertip_dance";
   public static final String ACTION_NIGHT_FEAST = "baobhan_sith_night_feast";
   public static final String ACTION_GRIMALKIN = "baobhan_sith_grimalkin";
   public static final String ACTION_BLESSED_SUCCESSOR = "baobhan_sith_blessed_successor";
   public static final String ACTION_FAIRY_VAMPIRISM = "baobhan_sith_fairy_vampirism";
   public static final String NP_FETCH_FAILNAUGHT = "fetch_failnaught";
   private static final String TAG_PREFIX = "TypeMoonBaobhanSith";
   private static final String TAG_MEDIUMS = TAG_PREFIX + "Mediums";
   private static final String TAG_CURSES = TAG_PREFIX + "Curses";
   private static final String TAG_LAST_TICK = TAG_PREFIX + "LastTick";
   private static final String TAG_MANA_STACKS = TAG_PREFIX + "ManaStacks";
   private static final String TAG_GRIMALKIN_UNTIL = TAG_PREFIX + "GrimalkinUntil";
   private static final String TAG_BLESSED_SUCCESSOR_UNTIL = TAG_PREFIX + "BlessedSuccessorUntil";
   private static final String TAG_LAST_CURSE_SHOT = TAG_PREFIX + "LastCurseShot";
   private static final String TAG_LAST_FINGERTIP = TAG_PREFIX + "LastFingertipDance";
   private static final String TAG_LAST_NIGHT_FEAST = TAG_PREFIX + "LastNightFeast";
   private static final String TAG_LAST_GRIMALKIN = TAG_PREFIX + "LastGrimalkin";
   private static final String TAG_LAST_BLESSED = TAG_PREFIX + "LastBlessedSuccessor";
   private static final String TAG_LAST_FAIRY_VAMPIRISM = TAG_PREFIX + "LastFairyVampirism";
   private static final String TAG_LAST_FETCH_FAILNAUGHT = TAG_PREFIX + "LastFetchFailnaught";
   private static final String TAG_LAST_AI_DECISION = TAG_PREFIX + "LastAiDecision";
   private static final String TAG_LAST_TACTICAL_MOVE = TAG_PREFIX + "LastTacticalMove";
   private static final String TAG_ORBIT_DIRECTION = TAG_PREFIX + "OrbitDirection";
   private static final String TAG_NEXT_ORBIT_SWITCH = TAG_PREFIX + "NextOrbitSwitch";
   private static final String TAG_LAST_HOOF_FX = TAG_PREFIX + "LastHoofFx";
   private static final String TAG_LAST_BLOOD_SPIKE = TAG_PREFIX + "LastBloodSpike";
   private static final String TAG_LAST_BLOOD_THORNS = TAG_PREFIX + "LastBloodThorns";
   private static final String TAG_LAST_CURSE_VOLLEY = TAG_PREFIX + "LastCurseVolley";
   private static final String TAG_IMMUNE_UNTIL = "ImmuneUntil";
   private static final String TAG_BURST_UNTIL = "BurstUntil";
   private static final String TAG_CURSE_LAYERS = "CurseLayers";
   private static final String MEDIUM_BLOOD = "Blood";
   private static final String MEDIUM_SKIN = "Skin";
   private static final String MEDIUM_HAIR = "Hair";
   private static final String MEDIUM_REMAINS = "Remains";
   private static final String CURSE_BLOOD = "BloodCurse";
   private static final String CURSE_SKIN = "SkinCurse";
   private static final String CURSE_REMAINS = "RemainsCurse";
   private static final int MAX_MEDIUMS_PER_TYPE = 10;
   private static final DustParticleOptions BLOOD_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.06F, 0.10F), 1.15F);
   private static final DustParticleOptions CURSE_DUST = new DustParticleOptions(new Vector3f(0.08F, 0.02F, 0.10F), 1.0F);
   private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(new Vector3f(0.45F, 0.08F, 0.62F), 1.0F);
   private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.74F, 0.18F), 1.05F);
   private static final DustParticleOptions PALE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.92F, 0.86F), 0.9F);

   private BaobhanSithServantSkills() {
   }

   public static void registerCombatActions(IServantAddonRegistry registry) {
      registry.registerCombatAction(ACTION_CURSE_SHOT, BaobhanSithServantSkills::castCurseShot, PROVIDER_ID);
      registry.registerCombatAction(ACTION_BLOOD_SPIKE, BaobhanSithServantSkills::castBloodSpike, PROVIDER_ID);
      registry.registerCombatAction(ACTION_BLOOD_THORNS, BaobhanSithServantSkills::castBloodThorns, PROVIDER_ID);
      registry.registerCombatAction(ACTION_CURSE_VOLLEY, BaobhanSithServantSkills::castCurseVolley, PROVIDER_ID);
      registry.registerCombatAction(ACTION_FINGERTIP_DANCE, BaobhanSithServantSkills::castFingertipDance, PROVIDER_ID);
      registry.registerCombatAction(ACTION_NIGHT_FEAST, BaobhanSithServantSkills::castNightFeast, PROVIDER_ID);
      registry.registerCombatAction(ACTION_GRIMALKIN, BaobhanSithServantSkills::castGrimalkin, PROVIDER_ID);
      registry.registerCombatAction(ACTION_BLESSED_SUCCESSOR, BaobhanSithServantSkills::castBlessedSuccessor, PROVIDER_ID);
      registry.registerCombatAction(ACTION_FAIRY_VAMPIRISM, BaobhanSithServantSkills::castFairyVampirism, PROVIDER_ID);
      registry.registerCombatAction(NP_FETCH_FAILNAUGHT, BaobhanSithServantSkills::castFetchFailnaughtAction, PROVIDER_ID);
      registry.registerNoblePhantasm(NP_FETCH_FAILNAUGHT, BaobhanSithServantSkills::castFetchFailnaught, PROVIDER_ID);
      registry.registerLifecycleHandler("baobhan_sith_lifecycle", BaobhanSithServantSkills::tickBaobhanSith, PROVIDER_ID);
   }

   public static void onCurseProjectileHit(Entity owner, LivingEntity target) {
      if (owner instanceof LivingEntity livingOwner && target != null && target.isAlive()) {
         addMedium(livingOwner, target, MEDIUM_BLOOD, 1);
         applyCurse(livingOwner, target, CURSE_BLOOD, 1, 240, isSunlit(livingOwner) ? 0.5F : 1.0F);
      }
   }

   private static ServantExecutionResult tickBaobhanSith(ServantLifecycleContext context) {
      if (!(context.entity() instanceof ServantEntity servant) || !isBaobhanSith(servant)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      long now = context.gameTick();
      CompoundTag data = servant.getPersistentData();

      if (servant.level() instanceof ServerLevel level) {
         if (isSunlit(servant) && now - data.getLong(TAG_LAST_TICK + "Sun") >= 20L) {
            data.putLong(TAG_LAST_TICK + "Sun", now);
            servant.hurt(servant.damageSources().magic(), 5.0F);
            spawnBurstParticles(level, servant.position().add(0.0, servant.getBbHeight() * 0.5, 0.0), 0.8, 12);
         }
         if (now - data.getLong(TAG_LAST_TICK + "Mana") >= 20L) {
            data.putLong(TAG_LAST_TICK + "Mana", now);
            int stacks = Math.min(10, data.getInt(TAG_MANA_STACKS));
            if (stacks > 0) {
               servant.setCurrentMp(Math.min(servant.getMaxMp(), servant.getCurrentMp() + stacks * 0.5));
            }
         }
      }

      LivingEntity target = context.target();
      if (target != null && target.isAlive()) {
         tickTargetCurses(servant, target, now);
         ServantExecutionResult aiResult = runCurseWeaverAi(context, servant, target, now);
         if (aiResult.handled()) {
            return aiResult;
         }
      }
      return ServantExecutionResult.NOT_HANDLED;
   }

   private static ServantExecutionResult runCurseWeaverAi(
      ServantLifecycleContext context,
      ServantEntity servant,
      LivingEntity target,
      long now
   ) {
      if (!(servant.level() instanceof ServerLevel level) || !EntityUtils.isValidCombatTarget(servant, target)) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      boolean lineOfSight = servant.getSensing().hasLineOfSight(target);
      double distance = servant.distanceTo(target);
      maintainCurseWeaverSpacing(level, servant, target, distance, lineOfSight, now);
      tickGrimalkinHoofTrail(level, servant, now);

      if (ServantCombatSystem.cannotAct(servant) || ServantCombatSystem.skillsSuppressed(servant)
         || servant.isPerformingAction() || now - servant.getPersistentData().getLong(TAG_LAST_AI_DECISION) < 10L) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      servant.getPersistentData().putLong(TAG_LAST_AI_DECISION, now);

      int layers = totalCurseLayers(servant, target);
      int mediums = totalMediumCount(servant, target);
      boolean pressured = distance <= 7.0 || countNearbyEnemies(level, servant, 6.5) >= 2 || isSunlit(servant);

      ServantExecutionResult result = ServantExecutionResult.NOT_HANDLED;
      if (mediums > 0 && layers >= 5 && servant.getCurrentMp() >= 50.0 && distance <= 64.0) {
         result = castFetchFailnaughtAction(actionContext(context, target, NP_FETCH_FAILNAUGHT, distance, lineOfSight));
      } else if (servant.getHealth() <= servant.getMaxHealth() * 0.48F && distance <= 4.5) {
         result = castNightFeast(actionContext(context, target, ACTION_NIGHT_FEAST, distance, lineOfSight));
         if (!result.handled()) {
            result = castFairyVampirism(actionContext(context, target, ACTION_FAIRY_VAMPIRISM, distance, lineOfSight));
         }
      } else if (pressured && distance <= 9.5 && layers < 5 && servant.getCurrentMp() >= 12.0) {
         result = castBloodThorns(actionContext(context, target, ACTION_BLOOD_THORNS, distance, lineOfSight));
      } else if (pressured && servant.getCurrentMp() >= 15.0) {
         result = castGrimalkin(actionContext(context, target, ACTION_GRIMALKIN, distance, lineOfSight));
      } else if (target instanceof ServantEntity && distance <= 20.0 && servant.getCurrentMp() >= 25.0
         && (layers >= 3 || countNearbyEnemies(level, servant, 20.0) >= 2)) {
         result = castBlessedSuccessor(actionContext(context, target, ACTION_BLESSED_SUCCESSOR, distance, lineOfSight));
      } else if (distance <= 4.0 && layers < 4) {
         result = castFingertipDance(actionContext(context, target, ACTION_FINGERTIP_DANCE, distance, lineOfSight));
      } else if (distance <= 4.0 && layers >= 2) {
         result = castFairyVampirism(actionContext(context, target, ACTION_FAIRY_VAMPIRISM, distance, lineOfSight));
      } else if (lineOfSight && distance >= 10.0 && distance <= 34.0 && layers <= 3 && servant.getCurrentMp() >= 14.0) {
         result = castCurseVolley(actionContext(context, target, ACTION_CURSE_VOLLEY, distance, true));
      } else if (lineOfSight && distance >= 4.0 && distance <= 18.0 && layers < 5 && servant.getCurrentMp() >= 6.0) {
         result = castBloodSpike(actionContext(context, target, ACTION_BLOOD_SPIKE, distance, true));
      } else if (lineOfSight && distance >= 6.0 && distance <= 42.0 && layers < 5) {
         result = castCurseShot(actionContext(context, target, ACTION_CURSE_SHOT, distance, true));
      } else if (lineOfSight && distance >= 10.0 && distance <= 42.0 && servant.getCurrentMp() > servant.getMaxMp() * 0.7) {
         result = castCurseShot(actionContext(context, target, ACTION_CURSE_SHOT, distance, true));
      }

      return result.handled() ? result : ServantExecutionResult.NOT_HANDLED;
   }

   private static ServantCombatActionContext actionContext(
      ServantLifecycleContext context,
      LivingEntity target,
      String actionId,
      double distance,
      boolean lineOfSight
   ) {
      return new ServantCombatActionContext(
         context.entity(),
         target,
         context.aiContext(),
         context.definition(),
         actionId,
         distance,
         lineOfSight,
         context.gameTick()
      );
   }

   private static void maintainCurseWeaverSpacing(
      ServerLevel level,
      ServantEntity servant,
      LivingEntity target,
      double distance,
      boolean lineOfSight,
      long now
   ) {
      CompoundTag data = servant.getPersistentData();
      int orbit = data.getInt(TAG_ORBIT_DIRECTION);
      if (orbit == 0) {
         orbit = servant.getRandom().nextBoolean() ? 1 : -1;
         data.putInt(TAG_ORBIT_DIRECTION, orbit);
         data.putLong(TAG_NEXT_ORBIT_SWITCH, now + 80L + servant.getRandom().nextInt(61));
      } else if (now >= data.getLong(TAG_NEXT_ORBIT_SWITCH)) {
         orbit = -orbit;
         data.putInt(TAG_ORBIT_DIRECTION, orbit);
         data.putLong(TAG_NEXT_ORBIT_SWITCH, now + 80L + servant.getRandom().nextInt(81));
      }

      servant.getLookControl().setLookAt(target, 45.0F, 45.0F);
      if (now - data.getLong(TAG_LAST_TACTICAL_MOVE) < 14L) {
         if (distance >= 13.0 && distance <= 28.0 && lineOfSight) {
            servant.getMoveControl().strafe(0.08F, orbit * 0.28F);
         }
         return;
      }
      data.putLong(TAG_LAST_TACTICAL_MOVE, now);

      Vec3 away = servant.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
      if (away.lengthSqr() < 1.0E-5) {
         away = servant.getLookAngle().multiply(-1.0, 0.0, -1.0);
      }
      away = away.lengthSqr() < 1.0E-5 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
      Vec3 side = new Vec3(-away.z, 0.0, away.x).scale(orbit);

      Vec3 destination = null;
      double speed = data.getLong(TAG_GRIMALKIN_UNTIL) > now ? 1.38 : 1.08;
      if (distance < 10.0 || isSunlit(servant)) {
         destination = target.position().add(away.scale(isSunlit(servant) ? 24.0 : 20.0)).add(side.scale(5.0));
         if (servant.onGround()) {
            servant.jumpFromGround();
         }
         Vec3 motion = servant.getDeltaMovement();
         servant.setDeltaMovement(away.x * 0.38 + side.x * 0.16, Math.max(motion.y, 0.18), away.z * 0.38 + side.z * 0.16);
         servant.hurtMarked = true;
      } else if (distance > 30.0 || !lineOfSight) {
         destination = target.position().add(away.scale(22.0)).add(side.scale(lineOfSight ? 3.5 : 8.0));
         speed = lineOfSight ? speed : 1.22;
      } else if (distance >= 13.0 && distance <= 28.0) {
         destination = servant.position().add(side.scale(6.5)).add(away.scale(distance < 18.0 ? 2.0 : -1.0));
      }

      if (destination != null) {
         if (!servant.getNavigation().moveTo(destination.x, servant.getY(), destination.z, speed)) {
            servant.getMoveControl().strafe(distance < 12.0 ? -0.18F : 0.05F, orbit * 0.34F);
         }
      } else {
         servant.getNavigation().stop();
      }
   }

   private static ServantExecutionResult castCurseShot(ServantCombatActionContext context) {
      ServantEntity servant = validatedServant(context, 4.0, 42.0, 4.0);
      if (servant == null || !context.hasLineOfSight()) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      CompoundTag data = servant.getPersistentData();
      if (!ready(data, TAG_LAST_CURSE_SHOT, context.gameTick(), 34L)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      data.putLong(TAG_LAST_CURSE_SHOT, context.gameTick());
      servant.setCurrentMp(servant.getCurrentMp() - 4.0);
      servant.faceToward(context.target().position());
      servant.triggerAttackSwing();
      if (servant.level() instanceof ServerLevel level) {
         spawnRoseMuzzleFx(level, servant);
         BaobhanSithHarpItem.fireCurseShot(level, servant, 3.15, 1.05F);
      }
      ServantVoiceHelper.tryPlayAttack(servant);
      return ServantExecutionResult.SUCCESS.withMpCost(4.0);
   }

   private static ServantExecutionResult castBloodSpike(ServantCombatActionContext context) {
      ServantEntity servant = validatedServant(context, 3.0, 18.0, 6.0);
      if (servant == null || !context.hasLineOfSight()
         || !ready(servant.getPersistentData(), TAG_LAST_BLOOD_SPIKE, context.gameTick(), 120L)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      servant.getPersistentData().putLong(TAG_LAST_BLOOD_SPIKE, context.gameTick());
      servant.setCurrentMp(servant.getCurrentMp() - 6.0);
      servant.faceToward(target.position());
      servant.triggerRuneCastAnimation(12);
      target.invulnerableTime = 0;
      target.hurt(curseDamageSource(servant), 22.0F);
      target.invulnerableTime = 0;
      addMedium(servant, target, MEDIUM_BLOOD, 1);
      if (servant.getRandom().nextFloat() < 0.45F) {
         addMedium(servant, target, MEDIUM_SKIN, 1);
      }
      applyCurse(servant, target, CURSE_BLOOD, 1, 260, isSunlit(servant) ? 0.5F : 1.0F);
      if (servant.level() instanceof ServerLevel level) {
         spawnBloodSpikeFx(level, target.position(), target.getBbHeight());
      }
      ServantVoiceHelper.tryPlayAttack(servant);
      return ServantExecutionResult.SUCCESS.withMpCost(6.0);
   }

   private static ServantExecutionResult castBloodThorns(ServantCombatActionContext context) {
      ServantEntity servant = validatedServant(context, 2.0, 22.0, 12.0);
      if (servant == null || !ready(servant.getPersistentData(), TAG_LAST_BLOOD_THORNS, context.gameTick(), 220L)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity primary = context.target();
      servant.getPersistentData().putLong(TAG_LAST_BLOOD_THORNS, context.gameTick());
      servant.setCurrentMp(servant.getCurrentMp() - 12.0);
      servant.faceToward(primary.position());
      servant.triggerRuneCastAnimation(18);
      if (servant.level() instanceof ServerLevel level) {
         Vec3 center = primary.position();
         AABB box = primary.getBoundingBox().inflate(3.4, 1.0, 3.4);
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
            other -> other != servant && other.isAlive() && EntityUtils.isValidCombatTarget(servant, other))) {
            target.invulnerableTime = 0;
            target.hurt(curseDamageSource(servant), target == primary ? 18.0F : 14.0F);
            target.invulnerableTime = 0;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 5, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, true, true));
            addMedium(servant, target, MEDIUM_SKIN, 1);
            applyCurse(servant, target, CURSE_SKIN, 1, 300, isSunlit(servant) ? 0.5F : 1.0F);
         }
         spawnBloodThornsFx(level, center, 3.4);
      }
      ServantVoiceHelper.tryPlayAttack(servant);
      return ServantExecutionResult.SUCCESS.withMpCost(12.0);
   }

   private static ServantExecutionResult castCurseVolley(ServantCombatActionContext context) {
      ServantEntity servant = validatedServant(context, 10.0, 38.0, 14.0);
      if (servant == null || !context.hasLineOfSight()
         || !ready(servant.getPersistentData(), TAG_LAST_CURSE_VOLLEY, context.gameTick(), 180L)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      servant.getPersistentData().putLong(TAG_LAST_CURSE_VOLLEY, context.gameTick());
      servant.setCurrentMp(servant.getCurrentMp() - 14.0);
      servant.faceToward(target.position());
      servant.triggerRuneCastAnimation(20);
      if (servant.level() instanceof ServerLevel level) {
         spawnCurseVolleyBackFx(level, servant, 5);
         fireBackCurseVolley(level, servant, target);
      }
      ServantVoiceHelper.tryPlayAttack(servant);
      return ServantExecutionResult.SUCCESS.withMpCost(14.0);
   }

   private static ServantExecutionResult castFingertipDance(ServantCombatActionContext context) {
      ServantEntity servant = validatedServant(context, 0.0, 4.0, 10.0);
      if (servant == null || !ready(servant.getPersistentData(), TAG_LAST_FINGERTIP, context.gameTick(), 300L)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      servant.getPersistentData().putLong(TAG_LAST_FINGERTIP, context.gameTick());
      servant.setCurrentMp(servant.getCurrentMp() - 10.0);
      servant.faceToward(target.position());
      servant.triggerSlashAnimation();
      for (int i = 0; i < 3; i++) {
         target.invulnerableTime = 0;
         target.hurt(servant.damageSources().mobAttack(servant), 20.0F);
         addMedium(servant, target, MEDIUM_BLOOD, 1);
         if (servant.getRandom().nextFloat() < 0.5F) {
            addMedium(servant, target, preferredMissingMedium(servant, target));
            applyCurse(servant, target, CURSE_SKIN, 1, 300, isSunlit(servant) ? 0.5F : 1.0F);
         }
      }
      spawnSlashParticles(servant, target);
      ServantVoiceHelper.tryPlayAttack(servant);
      return ServantExecutionResult.SUCCESS.withMpCost(10.0);
   }

   private static ServantExecutionResult castNightFeast(ServantCombatActionContext context) {
      ServantEntity servant = validatedServant(context, 0.0, 4.0, 15.0);
      if (servant == null || servant.getHealth() > servant.getMaxHealth() * 0.65F
         || !ready(servant.getPersistentData(), TAG_LAST_NIGHT_FEAST, context.gameTick(), 400L)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      int layers = totalCurseLayers(servant, target);
      float drain = 80.0F + layers * 20.0F;
      servant.getPersistentData().putLong(TAG_LAST_NIGHT_FEAST, context.gameTick());
      servant.setCurrentMp(servant.getCurrentMp() - 15.0);
      drainLife(servant, target, drain);
      addMedium(servant, target, MEDIUM_BLOOD, 1);
      ServantVoiceHelper.tryPlayAttack(servant);
      return ServantExecutionResult.SUCCESS.withMpCost(15.0);
   }

   private static ServantExecutionResult castGrimalkin(ServantCombatActionContext context) {
      ServantEntity servant = validatedServant(context, 0.0, 28.0, 15.0);
      if (servant == null || !ready(servant.getPersistentData(), TAG_LAST_GRIMALKIN, context.gameTick(), 600L)
         || servant.getHealth() > servant.getMaxHealth() * 0.9F && context.distance() > 22.0 && !isSunlit(servant)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      servant.getPersistentData().putLong(TAG_LAST_GRIMALKIN, context.gameTick());
      servant.getPersistentData().putLong(TAG_GRIMALKIN_UNTIL, context.gameTick() + 300L);
      servant.setCurrentMp(servant.getCurrentMp() - 15.0);
      servant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 2, false, true, true));
      servant.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 2, false, true, true));
      servant.triggerChargeAnimation();
      playSelfBuffFx(servant, 1.6);
      return ServantExecutionResult.SUCCESS.withMpCost(15.0);
   }

   private static ServantExecutionResult castBlessedSuccessor(ServantCombatActionContext context) {
      ServantEntity servant = validatedServant(context, 0.0, 20.0, 25.0);
      if (servant == null || !ready(servant.getPersistentData(), TAG_LAST_BLESSED, context.gameTick(), 700L)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      servant.getPersistentData().putLong(TAG_LAST_BLESSED, context.gameTick());
      servant.getPersistentData().putLong(TAG_BLESSED_SUCCESSOR_UNTIL, context.gameTick() + 300L);
      servant.setCurrentMp(servant.getCurrentMp() - 25.0);
      servant.triggerRuneCastAnimation(24);
      int sealed = sealEnemies(servant, context.gameTick());
      if (sealed <= 0 && context.target() instanceof ServantEntity targetServant && !targetServant.isAlliedTo(servant)) {
         targetServant.getPersistentData().putLong("TypeMoonCombatSuppressedUntil", context.gameTick() + 200L);
         sealed = 1;
      }
      servant.setCurrentMp(Math.min(servant.getMaxMp(), servant.getCurrentMp() + sealed * 5.0));
      playDominionFx(servant, 3.5);
      return ServantExecutionResult.SUCCESS.withMpCost(25.0);
   }

   private static ServantExecutionResult castFairyVampirism(ServantCombatActionContext context) {
      ServantEntity servant = validatedServant(context, 0.0, 4.0, 10.0);
      if (servant == null || !ready(servant.getPersistentData(), TAG_LAST_FAIRY_VAMPIRISM, context.gameTick(), 500L)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      servant.getPersistentData().putLong(TAG_LAST_FAIRY_VAMPIRISM, context.gameTick());
      servant.setCurrentMp(servant.getCurrentMp() - 10.0);
      drainLife(servant, target, 60.0F);
      if (target instanceof ServantEntity targetServant) {
         double drained = Math.min(20.0, targetServant.getCurrentMp());
         targetServant.setCurrentMp(Math.max(0.0, targetServant.getCurrentMp() - drained));
         servant.setCurrentMp(Math.min(servant.getMaxMp(), servant.getCurrentMp() + drained));
      } else {
         servant.setCurrentMp(Math.min(servant.getMaxMp(), servant.getCurrentMp() + 20.0));
      }
      applyCurse(servant, target, CURSE_BLOOD, 1, 200, isSunlit(servant) ? 0.5F : 1.0F);
      if (totalCurseLayers(servant, target) >= 5 && servant.getRandom().nextFloat() < 0.2F) {
         servant.hurt(servant.damageSources().magic(), 50.0F);
      }
      spawnDrainParticles(servant, target);
      ServantVoiceHelper.tryPlayAttack(servant);
      return ServantExecutionResult.SUCCESS.withMpCost(10.0);
   }

   private static ServantExecutionResult castFetchFailnaughtAction(ServantCombatActionContext context) {
      if (!(context.caster() instanceof ServantEntity servant) || !isBaobhanSith(servant)
         || context.target() == null || !context.target().isAlive() || context.distance() > 64.0
         || servant.getHealth() > servant.getMaxHealth() * 0.7F && totalCurseLayers(servant, context.target()) < 5
         || !ready(servant.getPersistentData(), TAG_LAST_FETCH_FAILNAUGHT, context.gameTick(), 600L)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      ServantNoblePhantasmDefinition np = ServantNoblePhantasmDataRegistry.get(NP_FETCH_FAILNAUGHT);
      if (np == null) {
         return ServantExecutionResult.FAILED;
      }
      ServantExecutionResult result = ServantNoblePhantasmExecutor.activateNp(servant, context.target(), np, 1);
      if (result.success()) {
         servant.getPersistentData().putLong(TAG_LAST_FETCH_FAILNAUGHT, context.gameTick());
      }
      return result;
   }

   private static ServantExecutionResult castFetchFailnaught(ServantNoblePhantasmContext context) {
      ServantEntity servant = context.caster();
      LivingEntity target = context.target();
      if (servant == null || !isBaobhanSith(servant) || target == null || !target.isAlive()
         || context.currentMp() < context.noblePhantasmDefinition().mpCost()) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      int mediumCount = Math.min(10, totalMediumCount(servant, target));
      if (mediumCount <= 0) {
         return ServantExecutionResult.FAILED;
      }
      servant.faceToward(target.position());
      servant.triggerRuneCastAnimation(60);
      ServantVoiceHelper.tryPlayBaobhanSithNp(servant);
      boolean canBurst = totalCurseLayers(servant, target) >= 5;
      float damage = mediumCount * 50.0F;
      if (canBurst) {
         damage *= 2.0F;
      }
      boolean wasAlive = target.isAlive();
      target.invulnerableTime = 0;
      target.hurt(curseDamageSource(servant), damage);
      target.invulnerableTime = 0;
      consumeAllMediums(servant, target);
      if (canBurst) {
         triggerBurst(servant, target);
      }
      if (servant.level() instanceof ServerLevel level) {
         spawnFetchFailnaughtFx(level, servant, target);
      }
      if (wasAlive && !target.isAlive()) {
         addMedium(servant, target, MEDIUM_REMAINS, 1);
         servant.heal(100.0F);
      }
      return ServantExecutionResult.SUCCESS.withMpCost(context.noblePhantasmDefinition().mpCost());
   }

   private static ServantEntity validatedServant(ServantCombatActionContext context, double minDistance, double maxDistance, double mpCost) {
      if (!(context.caster() instanceof ServantEntity servant) || !isBaobhanSith(servant)) {
         return null;
      }
      LivingEntity target = context.target();
      if (target == null || !target.isAlive() || context.distance() < minDistance || context.distance() > maxDistance
         || servant.getCurrentMp() < mpCost || ServantCombatSystem.cannotAct(servant)
         || ServantCombatSystem.skillsSuppressed(servant) || servant.isPerformingAction()
         || !EntityUtils.isValidCombatTarget(servant, target)) {
         return null;
      }
      return servant;
   }

   private static boolean ready(CompoundTag data, String tag, long now, long cooldownTicks) {
      return now - data.getLong(tag) >= cooldownTicks;
   }

   private static boolean isBaobhanSith(ServantEntity servant) {
      if (servant == null) {
         return false;
      }
      String id = servant.getServantId();
      int separator = id == null ? -1 : id.indexOf(':');
      if (separator >= 0) {
         id = id.substring(separator + 1);
      }
      return SERVANT_ID.equals(id);
   }

   private static boolean isSunlit(LivingEntity entity) {
      return entity != null && entity.level() instanceof ServerLevel level
         && level.isDay() && level.canSeeSky(entity.blockPosition()) && !level.isRaining();
   }

   private static CompoundTag ownerMediums(LivingEntity owner, LivingEntity target) {
      CompoundTag root = owner.getPersistentData().getCompound(TAG_MEDIUMS);
      CompoundTag targetTag = root.getCompound(target.getUUID().toString());
      root.put(target.getUUID().toString(), targetTag);
      owner.getPersistentData().put(TAG_MEDIUMS, root);
      return targetTag;
   }

   private static CompoundTag targetCurses(LivingEntity owner, LivingEntity target) {
      CompoundTag root = target.getPersistentData().getCompound(TAG_CURSES);
      CompoundTag ownerTag = root.getCompound(owner.getUUID().toString());
      root.put(owner.getUUID().toString(), ownerTag);
      target.getPersistentData().put(TAG_CURSES, root);
      return ownerTag;
   }

   private static void saveTargetCurses(LivingEntity owner, LivingEntity target, CompoundTag ownerTag) {
      CompoundTag root = target.getPersistentData().getCompound(TAG_CURSES);
      root.put(owner.getUUID().toString(), ownerTag);
      target.getPersistentData().put(TAG_CURSES, root);
   }

   private static void addMedium(LivingEntity owner, LivingEntity target, String medium) {
      addMedium(owner, target, medium, 1);
   }

   private static void addMedium(LivingEntity owner, LivingEntity target, String medium, int count) {
      CompoundTag mediumTag = ownerMediums(owner, target);
      mediumTag.putInt(medium, Math.min(MAX_MEDIUMS_PER_TYPE, mediumTag.getInt(medium) + Math.max(1, count)));
      CompoundTag root = owner.getPersistentData().getCompound(TAG_MEDIUMS);
      root.put(target.getUUID().toString(), mediumTag);
      owner.getPersistentData().put(TAG_MEDIUMS, root);
   }

   private static String preferredMissingMedium(LivingEntity owner, LivingEntity target) {
      CompoundTag mediumTag = ownerMediums(owner, target);
      for (String medium : List.of(MEDIUM_SKIN, MEDIUM_HAIR, MEDIUM_BLOOD)) {
         if (mediumTag.getInt(medium) <= 0) {
            return medium;
         }
      }
      return MEDIUM_SKIN;
   }

   private static void applyCurse(LivingEntity owner, LivingEntity target, String curse, int amount, int durationTicks, float strengthScale) {
      long now = owner.level().getGameTime();
      CompoundTag curseTag = targetCurses(owner, target);
      if (now < curseTag.getLong(TAG_IMMUNE_UNTIL)) {
         return;
      }
      curseTag.putInt(curse, Math.min(5, curseTag.getInt(curse) + Math.max(1, amount)));
      curseTag.putInt(TAG_CURSE_LAYERS, Math.min(20, curseTag.getInt(TAG_CURSE_LAYERS) + Math.max(1, amount)));
      curseTag.putFloat("StrengthScale", Math.max(0.2F, Math.min(1.0F, strengthScale)));
      curseTag.putLong("ExpiresAt", Math.max(curseTag.getLong("ExpiresAt"), now + durationTicks));
      if (curseTag.getInt(TAG_CURSE_LAYERS) >= 5 && now >= curseTag.getLong(TAG_BURST_UNTIL)) {
         curseTag.putLong(TAG_BURST_UNTIL, now + 200L);
      }
      saveTargetCurses(owner, target, curseTag);
      if (owner instanceof ServantEntity servant) {
         CompoundTag data = servant.getPersistentData();
         data.putInt(TAG_MANA_STACKS, Math.min(10, data.getInt(TAG_MANA_STACKS) + 1));
      }
      if (target.level() instanceof ServerLevel level) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, 12, 0.35, 0.35, 0.35, 0.0);
         level.sendParticles(CURSE_DUST, center.x, center.y, center.z, 10, 0.32, 0.32, 0.32, 0.0);
         spawnCurseSigil(level, center, curseTag.getInt(TAG_CURSE_LAYERS));
      }
   }

   private static int totalCurseLayers(LivingEntity owner, LivingEntity target) {
      return targetCurses(owner, target).getInt(TAG_CURSE_LAYERS);
   }

   private static int totalMediumCount(LivingEntity owner, LivingEntity target) {
      CompoundTag mediumTag = ownerMediums(owner, target);
      return mediumTag.getInt(MEDIUM_BLOOD) + mediumTag.getInt(MEDIUM_SKIN)
         + mediumTag.getInt(MEDIUM_HAIR) + mediumTag.getInt(MEDIUM_REMAINS);
   }

   private static void consumeAllMediums(LivingEntity owner, LivingEntity target) {
      CompoundTag root = owner.getPersistentData().getCompound(TAG_MEDIUMS);
      root.remove(target.getUUID().toString());
      owner.getPersistentData().put(TAG_MEDIUMS, root);
   }

   private static void triggerBurst(LivingEntity owner, LivingEntity target) {
      CompoundTag curseTag = targetCurses(owner, target);
      curseTag.putLong(TAG_BURST_UNTIL, owner.level().getGameTime() + 200L);
      saveTargetCurses(owner, target, curseTag);
   }

   private static void tickTargetCurses(LivingEntity owner, LivingEntity target, long now) {
      CompoundTag curseTag = targetCurses(owner, target);
      if (curseTag.isEmpty() || now - curseTag.getLong(TAG_LAST_TICK) < 20L) {
         return;
      }
      if (curseTag.getLong("ExpiresAt") > 0L && now > curseTag.getLong("ExpiresAt")) {
         clearCurse(owner, target, curseTag, now, 0L);
         return;
      }
      curseTag.putLong(TAG_LAST_TICK, now);
      boolean burst = now < curseTag.getLong(TAG_BURST_UNTIL);
      float scale = curseTag.getFloat("StrengthScale");
      float damage = (curseTag.getInt(CURSE_BLOOD) * 5.0F + curseTag.getInt(CURSE_REMAINS) * 15.0F) * Math.max(0.2F, scale);
      if (curseTag.getInt(CURSE_SKIN) > 0) {
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, Math.min(4, curseTag.getInt(CURSE_SKIN) - 1), false, true, true));
      }
      if (curseTag.getInt(CURSE_BLOOD) > 0) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, Math.min(4, curseTag.getInt(CURSE_BLOOD) - 1), false, true, true));
      }
      if (burst) {
         damage *= 2.0F;
         if (target.level() instanceof ServerLevel level) {
            Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
            spawnBurstParticles(level, center, 1.15, 18);
            spawnThornSpiral(level, center, 1.0, target.getBbHeight() * 0.95, 18);
         }
      } else if (curseTag.getLong(TAG_BURST_UNTIL) > 0L && now >= curseTag.getLong(TAG_BURST_UNTIL)) {
         clearCurse(owner, target, curseTag, now, 100L);
         return;
      }
      if (damage > 0.0F) {
         target.invulnerableTime = 0;
         target.hurt(curseDamageSource(owner), damage);
         target.invulnerableTime = 0;
         if (!target.isAlive()) {
            addMedium(owner, target, MEDIUM_REMAINS, 1);
         }
      }
      saveTargetCurses(owner, target, curseTag);
   }

   private static void clearCurse(LivingEntity owner, LivingEntity target, CompoundTag curseTag, long now, long immuneTicks) {
      for (String key : List.copyOf(curseTag.getAllKeys())) {
         curseTag.remove(key);
      }
      if (immuneTicks > 0L) {
         curseTag.putLong(TAG_IMMUNE_UNTIL, now + immuneTicks);
      }
      saveTargetCurses(owner, target, curseTag);
   }

   private static void drainLife(ServantEntity servant, LivingEntity target, float amount) {
      target.invulnerableTime = 0;
      target.hurt(curseDamageSource(servant), amount);
      target.invulnerableTime = 0;
      servant.heal(amount);
      spawnDrainParticles(servant, target);
   }

   private static DamageSource curseDamageSource(LivingEntity owner) {
      return owner.damageSources().source(BaobhanSithDamageTypes.CURSE, owner);
   }

   private static int sealEnemies(ServantEntity servant, long now) {
      if (!(servant.level() instanceof ServerLevel level)) {
         return 0;
      }
      int count = 0;
      for (ServantEntity target : level.getEntitiesOfClass(ServantEntity.class, servant.getBoundingBox().inflate(20.0),
         other -> other != servant && other.isAlive() && !other.isAlliedTo(servant))) {
         target.getPersistentData().putLong("TypeMoonCombatSuppressedUntil", now + 200L);
         count++;
      }
      return count;
   }

   private static int countNearbyEnemies(ServerLevel level, ServantEntity servant, double radius) {
      AABB box = servant.getBoundingBox().inflate(radius);
      return level.getEntitiesOfClass(LivingEntity.class, box,
         other -> other != servant && other.isAlive() && EntityUtils.isValidCombatTarget(servant, other)).size();
   }

   private static void tickGrimalkinHoofTrail(ServerLevel level, ServantEntity servant, long now) {
      CompoundTag data = servant.getPersistentData();
      if (data.getLong(TAG_GRIMALKIN_UNTIL) <= now || now - data.getLong(TAG_LAST_HOOF_FX) < 6L) {
         return;
      }
      data.putLong(TAG_LAST_HOOF_FX, now);
      Vec3 center = servant.position().add(0.0, 0.08, 0.0);
      double yaw = Math.toRadians(servant.getYRot());
      Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).scale(0.18);
      spawnHoofPrint(level, center.add(right));
      spawnHoofPrint(level, center.subtract(right));
   }

   private static void fireBackCurseVolley(ServerLevel level, ServantEntity servant, LivingEntity target) {
      Vec3 toTarget = target.getEyePosition().subtract(servant.getEyePosition());
      Vec3 forward = toTarget.lengthSqr() < 1.0E-5 ? servant.getLookAngle() : toTarget.normalize();
      Vec3 flatForward = new Vec3(forward.x, 0.0, forward.z);
      if (flatForward.lengthSqr() < 1.0E-5) {
         flatForward = new Vec3(0.0, 0.0, 1.0);
      } else {
         flatForward = flatForward.normalize();
      }
      Vec3 side = new Vec3(-flatForward.z, 0.0, flatForward.x).normalize();
      Vec3 base = servant.position()
         .add(0.0, servant.getBbHeight() * 0.78, 0.0)
         .subtract(flatForward.scale(0.95));
      for (int i = 0; i < 5; i++) {
         double spread = (i - 2) * 0.44;
         double lift = 0.18 + Math.sin(i * Math.PI / 4.0) * 0.26;
         Vec3 start = base.add(side.scale(spread)).add(0.0, lift, 0.0);
         Vec3 aim = target.getEyePosition().add(side.scale((i - 2) * 0.18)).subtract(start);
         Vec3 direction = aim.lengthSqr() < 1.0E-5 ? forward : aim.normalize();
         fireOffsetCurseShot(level, servant, start, direction, 3.05 + i * 0.06, 0.82F, 1);
      }
      level.playSound(null, servant.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.4F);
   }

   private static void fireOffsetCurseShot(
      ServerLevel level,
      LivingEntity shooter,
      Vec3 start,
      Vec3 direction,
      double speed,
      float visualScale,
      int chargeSeconds
   ) {
      GanderProjectileEntity projectile = new GanderProjectileEntity(level, shooter);
      projectile.setPos(start.x, start.y, start.z);
      projectile.setDeltaMovement(direction.normalize().scale(speed));
      projectile.setMagicSource("baobhan_sith_curse", 70.0);
      projectile.setChargeSeconds(chargeSeconds);
      projectile.setVisualScale(visualScale);
      level.addFreshEntity(projectile);
      level.sendParticles(BLOOD_DUST, start.x, start.y, start.z, 7, 0.08, 0.08, 0.08, 0.0);
      level.sendParticles(CURSE_DUST, start.x, start.y, start.z, 5, 0.06, 0.06, 0.06, 0.0);
   }

   private static void spawnSlashParticles(ServantEntity servant, LivingEntity target) {
      if (servant.level() instanceof ServerLevel level) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 3, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, 22, 0.45, 0.35, 0.45, 0.0);
         spawnThornSpiral(level, target.position().add(0.0, 0.1, 0.0), 0.85, target.getBbHeight(), 24);
         spawnRoseBloom(level, center, 0.9, 28);
         level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.25F);
      }
   }

   private static void spawnDrainParticles(ServantEntity servant, LivingEntity target) {
      if (servant.level() instanceof ServerLevel level) {
         Vec3 from = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         Vec3 to = servant.position().add(0.0, servant.getBbHeight() * 0.65, 0.0);
         for (int i = 0; i < 8; i++) {
            Vec3 pos = from.lerp(to, i / 7.0);
            level.sendParticles(BLOOD_DUST, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.0);
            if (i % 2 == 0) {
               level.sendParticles(CURSE_DUST, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.0);
            }
         }
         spawnRoseBloom(level, target.position().add(0.0, target.getBbHeight() * 0.55, 0.0), 0.7, 18);
         level.playSound(null, servant.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.HOSTILE, 0.9F, 0.75F);
      }
   }

   private static void playSelfBuffFx(ServantEntity servant, double radius) {
      if (servant.level() instanceof ServerLevel level) {
         Vec3 center = servant.position().add(0.0, 0.15, 0.0);
         level.sendParticles(CURSE_DUST, center.x, center.y, center.z, 28, radius, 0.08, radius, 0.0);
         level.sendParticles(ParticleTypes.WITCH, center.x, center.y + 0.5, center.z, 16, 0.45, 0.65, 0.45, 0.02);
         spawnRing(level, PURPLE_DUST, center, 0.8, 22);
         spawnRing(level, PURPLE_DUST, center, 1.45, 32);
         spawnCatTailArc(level, servant.position().add(0.0, servant.getBbHeight() * 0.55, 0.0), radius);
         level.playSound(null, servant.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.3F);
      }
   }

   private static void playDominionFx(ServantEntity servant, double radius) {
      if (servant.level() instanceof ServerLevel level) {
         Vec3 center = servant.position().add(0.0, 0.2, 0.0);
         level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, 64, radius, 0.1, radius, 0.0);
         level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.8, center.z, 80, radius * 0.5, 0.8, radius * 0.5, 0.05);
         spawnRing(level, BLOOD_DUST, center, 2.0, 40);
         spawnRing(level, GOLD_DUST, center.add(0.0, 0.05, 0.0), 3.2, 56);
         spawnCrownGlyph(level, servant.position().add(0.0, servant.getBbHeight() + 0.35, 0.0), 1.25);
         level.playSound(null, servant.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.2F, 0.82F);
      }
   }

   private static void spawnBurstParticles(ServerLevel level, Vec3 center, double radius, int count) {
      level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, count, radius, radius * 0.45, radius, 0.0);
      level.sendParticles(CURSE_DUST, center.x, center.y, center.z, count, radius * 0.85, radius * 0.35, radius * 0.85, 0.0);
   }

   private static void spawnRoseMuzzleFx(ServerLevel level, LivingEntity shooter) {
      Vec3 look = shooter.getLookAngle().normalize();
      Vec3 center = shooter.getEyePosition().add(look.scale(0.72));
      spawnRing(level, BLOOD_DUST, center, 0.28, 12);
      for (int i = 0; i < 10; i++) {
         double angle = i * Math.PI * 2.0 / 10.0 + shooter.tickCount * 0.12;
         Vec3 petal = center.add(Math.cos(angle) * 0.18, Math.sin(angle * 2.0) * 0.04, Math.sin(angle) * 0.18);
         level.sendParticles(BLOOD_DUST, petal.x, petal.y, petal.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
      level.sendParticles(CURSE_DUST, center.x, center.y, center.z, 8, 0.16, 0.12, 0.16, 0.0);
   }

   private static void spawnBloodSpikeFx(ServerLevel level, Vec3 base, double targetHeight) {
      Vec3 root = base.add(0.0, 0.08, 0.0);
      double height = Math.max(1.1, targetHeight * 0.9);
      spawnRing(level, BLOOD_DUST, root, 0.42, 18);
      for (int i = 0; i < 26; i++) {
         double t = i / 25.0;
         double twist = t * Math.PI * 3.5;
         double radius = (1.0 - t) * 0.28;
         Vec3 pos = root.add(Math.cos(twist) * radius, height * t, Math.sin(twist) * radius);
         level.sendParticles(BLOOD_DUST, pos.x, pos.y, pos.z, 2, 0.025, 0.025, 0.025, 0.0);
         if (i % 3 == 0) {
            level.sendParticles(CURSE_DUST, pos.x, pos.y, pos.z, 1, 0.018, 0.018, 0.018, 0.0);
         }
      }
      level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, root.x, root.y + height * 0.75, root.z, 4, 0.14, 0.18, 0.14, 0.02);
      level.playSound(null, BlockPos.containing(root), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.0F, 0.58F);
   }

   private static void spawnBloodThornsFx(ServerLevel level, Vec3 center, double radius) {
      Vec3 root = center.add(0.0, 0.08, 0.0);
      spawnRing(level, CURSE_DUST, root, radius * 0.45, 24);
      spawnRing(level, BLOOD_DUST, root.add(0.0, 0.04, 0.0), radius, 44);
      for (int thorn = 0; thorn < 8; thorn++) {
         double angle = thorn * Math.PI * 2.0 / 8.0;
         Vec3 thornBase = root.add(Math.cos(angle) * radius * 0.58, 0.0, Math.sin(angle) * radius * 0.58);
         spawnThornSpiral(level, thornBase, 0.28 + (thorn % 2) * 0.08, 1.0 + (thorn % 3) * 0.22, 16);
      }
      for (int i = 0; i < 36; i++) {
         double angle = i * Math.PI * 2.0 / 36.0;
         double curl = Math.sin(angle * 4.0) * 0.32;
         Vec3 pos = root.add(Math.cos(angle) * (radius * 0.78 + curl), 0.2 + Math.sin(angle * 2.0) * 0.12,
            Math.sin(angle) * (radius * 0.78 - curl));
         level.sendParticles(BLOOD_DUST, pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.0);
      }
      level.playSound(null, BlockPos.containing(root), SoundEvents.GROWING_PLANT_CROP, SoundSource.HOSTILE, 1.1F, 0.62F);
   }

   private static void spawnCurseVolleyBackFx(ServerLevel level, ServantEntity servant, int bolts) {
      Vec3 look = servant.getLookAngle();
      Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
      if (flatLook.lengthSqr() < 1.0E-5) {
         flatLook = new Vec3(0.0, 0.0, 1.0);
      } else {
         flatLook = flatLook.normalize();
      }
      Vec3 side = new Vec3(-flatLook.z, 0.0, flatLook.x).normalize();
      Vec3 center = servant.position()
         .add(0.0, servant.getBbHeight() * 0.82, 0.0)
         .subtract(flatLook.scale(0.9));
      spawnRing(level, BLOOD_DUST, center, 0.68, 24);
      spawnRing(level, PURPLE_DUST, center.add(0.0, 0.05, 0.0), 0.92, 30);
      for (int i = 0; i < bolts; i++) {
         double spread = (i - (bolts - 1) * 0.5) * 0.44;
         double lift = 0.18 + Math.sin(i * Math.PI / Math.max(1, bolts - 1)) * 0.26;
         Vec3 orb = center.add(side.scale(spread)).add(0.0, lift, 0.0);
         spawnRing(level, BLOOD_DUST, orb, 0.16, 8);
         level.sendParticles(CURSE_DUST, orb.x, orb.y, orb.z, 6, 0.04, 0.04, 0.04, 0.0);
      }
   }

   private static void spawnCurseSigil(ServerLevel level, Vec3 center, int layers) {
      double radius = 0.45 + Math.min(5, layers) * 0.08;
      spawnRing(level, CURSE_DUST, center, radius, 18 + Math.min(5, layers) * 4);
      if (layers >= 5) {
         spawnRing(level, BLOOD_DUST, center.add(0.0, 0.05, 0.0), radius + 0.22, 32);
      }
   }

   private static void spawnRing(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
      for (int i = 0; i < points; i++) {
         double angle = i * Math.PI * 2.0 / points;
         double x = center.x + Math.cos(angle) * radius;
         double z = center.z + Math.sin(angle) * radius;
         level.sendParticles(particle, x, center.y, z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void spawnThornSpiral(ServerLevel level, Vec3 base, double radius, double height, int points) {
      for (int i = 0; i < points; i++) {
         double t = i / (double)Math.max(1, points - 1);
         double angle = t * Math.PI * 4.0;
         double r = radius * (0.35 + t * 0.65);
         Vec3 pos = base.add(Math.cos(angle) * r, height * t, Math.sin(angle) * r);
         level.sendParticles(CURSE_DUST, pos.x, pos.y, pos.z, 1, 0.025, 0.025, 0.025, 0.0);
         if (i % 3 == 0) {
            Vec3 thorn = pos.add(Math.cos(angle + Math.PI * 0.5) * 0.18, 0.02, Math.sin(angle + Math.PI * 0.5) * 0.18);
            level.sendParticles(BLOOD_DUST, thorn.x, thorn.y, thorn.z, 1, 0.015, 0.015, 0.015, 0.0);
         }
      }
   }

   private static void spawnRoseBloom(ServerLevel level, Vec3 center, double radius, int petals) {
      for (int i = 0; i < petals; i++) {
         double angle = i * Math.PI * 2.0 / petals;
         double wave = Math.sin(angle * 3.0) * 0.18;
         Vec3 pos = center.add(Math.cos(angle) * (radius + wave), Math.sin(i * 0.7) * 0.16, Math.sin(angle) * (radius - wave));
         level.sendParticles(BLOOD_DUST, pos.x, pos.y, pos.z, 1, 0.04, 0.03, 0.04, 0.0);
      }
      level.sendParticles(PURPLE_DUST, center.x, center.y, center.z, 10, radius * 0.28, radius * 0.18, radius * 0.28, 0.0);
   }

   private static void spawnCatTailArc(ServerLevel level, Vec3 center, double radius) {
      for (int tail = 0; tail < 3; tail++) {
         double offset = tail * Math.PI * 2.0 / 3.0;
         for (int i = 0; i < 16; i++) {
            double t = i / 15.0;
            double angle = offset + t * Math.PI * 1.25;
            Vec3 pos = center.add(Math.cos(angle) * radius * (0.45 + t * 0.55), Math.sin(t * Math.PI) * 0.75, Math.sin(angle) * radius * (0.45 + t * 0.55));
            level.sendParticles(PURPLE_DUST, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
         }
      }
   }

   private static void spawnCrownGlyph(ServerLevel level, Vec3 center, double radius) {
      spawnRing(level, GOLD_DUST, center, radius, 28);
      for (int i = 0; i < 7; i++) {
         double angle = i * Math.PI * 2.0 / 7.0;
         Vec3 tip = center.add(Math.cos(angle) * radius, 0.36, Math.sin(angle) * radius);
         level.sendParticles(GOLD_DUST, tip.x, tip.y, tip.z, 2, 0.03, 0.08, 0.03, 0.0);
         level.sendParticles(BLOOD_DUST, tip.x, tip.y - 0.18, tip.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
   }

   private static void spawnHoofPrint(ServerLevel level, Vec3 center) {
      level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, 2, 0.045, 0.0, 0.025, 0.0);
      level.sendParticles(CURSE_DUST, center.x, center.y + 0.02, center.z, 1, 0.035, 0.0, 0.02, 0.0);
   }

   private static void spawnNightCurtain(ServerLevel level, Vec3 center) {
      for (int ring = 0; ring < 3; ring++) {
         double radius = 3.0 + ring * 2.8;
         for (int i = 0; i < 42; i++) {
            double angle = i * Math.PI * 2.0 / 42.0;
            Vec3 pos = center.add(Math.cos(angle) * radius, 0.25 + ring * 0.18, Math.sin(angle) * radius);
            level.sendParticles(PURPLE_DUST, pos.x, pos.y, pos.z, 1, 0.02, 0.10, 0.02, 0.0);
         }
      }
   }

   private static void spawnBloodMoon(ServerLevel level, Vec3 center) {
      spawnRing(level, BLOOD_DUST, center, 0.85, 30);
      level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, 32, 0.45, 0.06, 0.45, 0.0);
      level.sendParticles(PALE_DUST, center.x, center.y, center.z, 8, 0.28, 0.03, 0.28, 0.0);
   }

   private static void spawnFragmentConvergence(ServerLevel level, Vec3 from, Vec3 to) {
      Vec3 delta = to.subtract(from);
      for (int strand = 0; strand < 4; strand++) {
         double phase = strand * Math.PI * 0.5;
         for (int i = 0; i <= 16; i++) {
            double t = i / 16.0;
            Vec3 pos = from.add(delta.scale(t));
            double curl = Math.sin(t * Math.PI * 3.0 + phase) * 0.28;
            Vec3 curled = pos.add(Math.cos(phase) * curl, Math.sin(t * Math.PI) * 0.35, Math.sin(phase) * curl);
            level.sendParticles(BLOOD_DUST, curled.x, curled.y, curled.z, 1, 0.02, 0.02, 0.02, 0.0);
         }
      }
   }

   private static void spawnFetchDouble(ServerLevel level, Vec3 center, double targetHeight) {
      double height = Math.max(1.2, targetHeight);
      for (int i = 0; i < 28; i++) {
         double t = i / 27.0;
         double bodyRadius = 0.18 + Math.sin(t * Math.PI) * 0.24;
         double angle = i * Math.PI * 0.75;
         Vec3 pos = center.add(Math.cos(angle) * bodyRadius, (t - 0.45) * height, Math.sin(angle) * bodyRadius);
         level.sendParticles(BLOOD_DUST, pos.x, pos.y, pos.z, 2, 0.035, 0.035, 0.035, 0.0);
         if (i % 4 == 0) {
            level.sendParticles(CURSE_DUST, pos.x, pos.y, pos.z, 2, 0.04, 0.04, 0.04, 0.0);
         }
      }
      level.sendParticles(PALE_DUST, center.x, center.y + height * 0.25, center.z, 8, 0.18, 0.28, 0.18, 0.0);
   }

   private static void spawnFetchFailnaughtFx(ServerLevel level, ServantEntity servant, LivingEntity target) {
      Vec3 caster = servant.position().add(0.0, servant.getBbHeight() * 0.65, 0.0);
      Vec3 victim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 doublePos = caster.add(victim.subtract(caster).normalize().scale(Math.min(3.2, caster.distanceTo(victim) * 0.45)));
      spawnNightCurtain(level, servant.position());
      spawnBloodMoon(level, servant.position().add(0.0, 5.2, 0.0));
      spawnFragmentConvergence(level, victim, caster);
      spawnFetchDouble(level, doublePos, target.getBbHeight());
      spawnThornSpiral(level, doublePos.subtract(0.0, target.getBbHeight() * 0.35, 0.0), 1.1, target.getBbHeight() * 1.15, 32);
      Vec3 delta = victim.subtract(caster);
      for (int i = 0; i <= 24; i++) {
         Vec3 pos = caster.add(delta.scale(i / 24.0));
         level.sendParticles(BLOOD_DUST, pos.x, pos.y, pos.z, 3, 0.08, 0.08, 0.08, 0.0);
         if (i % 3 == 0) {
            level.sendParticles(CURSE_DUST, pos.x, pos.y, pos.z, 2, 0.12, 0.12, 0.12, 0.0);
         }
      }
      Vec3 doubleDelta = doublePos.subtract(caster);
      for (int i = 0; i <= 18; i++) {
         Vec3 pos = caster.add(doubleDelta.scale(i / 18.0));
         level.sendParticles(PALE_DUST, pos.x, pos.y, pos.z, 2, 0.04, 0.04, 0.04, 0.0);
      }
      spawnRoseBloom(level, doublePos, 1.35, 48);
      spawnBurstParticles(level, victim, 1.8, 80);
      spawnRing(level, BLOOD_DUST, victim, 1.2, 36);
      spawnRing(level, PURPLE_DUST, victim, 2.2, 48);
      level.sendParticles(ParticleTypes.FLASH, victim.x, victim.y, victim.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 0.6F);
   }
}
