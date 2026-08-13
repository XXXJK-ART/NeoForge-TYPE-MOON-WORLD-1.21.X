package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.CrimsonHoundProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaArrowOrbProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaThrownWeaponEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.PseudoSpiralSwordProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWInterceptorSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwSkyGearEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.UBWBrokenPhantasmExplosion;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicStructuralAnalysis;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UBWInstanceManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantEngagementService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

public final class EmiyaArcherCombatHelper {
   public static final String LAST_SPIRAL_TICK = "EmiyaLastSpiralTick";
   public static final String LAST_CRIMSON_TICK = "EmiyaLastCrimsonTick";
   public static final String LAST_RHO_AIAS_TICK = "EmiyaLastRhoAiasTick";
   public static final String LAST_BROKEN_PHANTASM_TICK = "EmiyaLastBrokenPhantasmTick";
   public static final String LAST_HRUNTING_STYLE_TICK = "EmiyaLastHruntingStyleTick";
   public static final String LAST_PROJECTION_VOLLEY_TICK = "EmiyaLastProjectionVolleyTick";
   public static final String LAST_MIND_EYE_STEP_TICK = "EmiyaLastMindEyeStepTick";
   public static final String LAST_TWIN_FLURRY_TICK = "EmiyaLastTwinFlurryTick";
   public static final String LAST_TWIN_UPPERCUT_TICK = "EmiyaLastTwinUppercutTick";
   public static final String LAST_TWIN_REPEL_TICK = "EmiyaLastTwinRepelTick";
   public static final String LAST_CHASING_THRUST_TICK = "EmiyaLastChasingThrustTick";
   public static final String LAST_PROJECTION_IMPACT_TICK = "EmiyaLastProjectionImpactTick";
   public static final String LAST_OVEREDGE_CLEAVE_TICK = "EmiyaLastOveredgeCleaveTick";
   public static final String LAST_BLADE_RUPTURE_TICK = "EmiyaLastBladeRuptureTick";
   public static final String LAST_AERIAL_PURSUIT_TICK = "EmiyaLastAerialPursuitTick";
   public static final String LAST_REINFORCED_SLAM_TICK = "EmiyaLastReinforcedSlamTick";
   public static final String LAST_SPHERICAL_PROJECTION_TICK = "EmiyaLastSphericalProjectionTick";
   public static final String LAST_AUTO_COUNTER_TICK = "EmiyaLastAutoCounterTick";
   public static final String AUTO_COUNTER_UNTIL = "EmiyaAutoCounterUntil";
   public static final String SPHERICAL_ROUNDS = "EmiyaSphericalProjectionRounds";
   public static final String SPHERICAL_NEXT_TICK = "EmiyaSphericalProjectionNextTick";
   public static final String SPHERICAL_TARGET = "EmiyaSphericalProjectionTarget";
   private static final String AUTO_COUNTER_CLAIMED = "EmiyaAutoCounterClaimed";
   public static final String LAST_REINFORCEMENT_TICK = "EmiyaLastReinforcementTick";
   public static final String LAST_UBW_TICK = "EmiyaLastUbwTick";
   public static final String PROJECTED_EXPIRES_TICK = "EmiyaProjectedExpiresTick";
   public static final String PROJECTED_PAIR = "EmiyaProjectedPair";
   public static final String UBW_CHANT_END_TICK = "EmiyaUbwChantEndTick";
   public static final String UBW_CHANT_TARGET_ID = "EmiyaUbwChantTargetId";
   public static final String UBW_ACTIVE_UNTIL = "EmiyaUbwActiveUntil";
   public static final String UBW_MIN_UNTIL = "EmiyaUbwMinUntil";
   public static final String UBW_NEXT_RAIN = "EmiyaUbwNextRain";
   public static final String UBW_NEXT_INTERCEPT = "EmiyaUbwNextIntercept";
   public static final String UBW_NEXT_TERRAIN = "EmiyaUbwNextTerrain";
   public static final String UBW_NEXT_BLADE_LIFT = "EmiyaUbwNextBladeLift";
   public static final String UBW_NEXT_CRIMSON_HOUND = "EmiyaUbwNextCrimsonHound";
   public static final String UBW_CENTER_X = "EmiyaUbwCenterX";
   public static final String UBW_CENTER_Y = "EmiyaUbwCenterY";
   public static final String UBW_CENTER_Z = "EmiyaUbwCenterZ";
   public static final String UBW_GEARS_SPAWNED = "EmiyaUbwGearsSpawned";
   public static final String COMBAT_MODE = "EmiyaCombatMode";
   public static final String ANALYZED_WEAPON_EXPIRES_TICK = "EmiyaAnalyzedWeaponExpiresTick";
   public static final String LAST_IRON_SWORD_SHOT_TICK = "EmiyaLastIronSwordShotTick";
   public static final String LAST_FOCUSED_SWORD_BARREL_TICK = "EmiyaLastFocusedSwordBarrelTick";
   public static final String LAST_CROSS_BLADE_TICK = "EmiyaLastCrossBladeTick";
   public static final String LAST_ORB_BURST_TICK = "EmiyaLastOrbBurstTick";
   public static final String LAST_CLAIRVOYANCE_SPIRAL_TICK = "EmiyaLastClairvoyanceSpiralTick";
   public static final String RANGED_STANDOFF_START_TICK = "EmiyaRangedStandoffStartTick";
   public static final String LAST_PROBING_RANGED_SPECIAL_TICK = "EmiyaLastProbingRangedSpecialTick";
   public static final String LAST_ANALYSIS_TICK = "EmiyaLastAnalysisTick";
   public static final String ANALYZED_WEAPON_STACK = "EmiyaAnalyzedWeaponStack";
   public static final String ANALYZED_WEAPON_BUFF_UNTIL = "EmiyaAnalyzedWeaponBuffUntil";
   public static final String BORROWED_NP_USED_UNTIL = "EmiyaBorrowedNoblePhantasmUsedUntil";
   public static final String UBW_RETURN_DIMENSION = "EmiyaUbwReturnDimension";
   public static final String UBW_RETURN_X = "EmiyaUbwReturnX";
   public static final String UBW_RETURN_Y = "EmiyaUbwReturnY";
   public static final String UBW_RETURN_Z = "EmiyaUbwReturnZ";
   public static final String UBW_LOCKED_TARGET = "EmiyaUbwLockedTarget";
   public static final String UBW_RELOCK_TARGET = "EmiyaUbwRelockTarget";
   public static final String UBW_RELOCK_UNTIL = "EmiyaUbwRelockUntil";
   public static final String UBW_TARGET_OWNER = "EmiyaUbwTargetOwner";
   public static final String UBW_TARGET_RETURN_DIMENSION = "EmiyaUbwTargetReturnDimension";
   public static final String UBW_TARGET_RETURN_X = "EmiyaUbwTargetReturnX";
   public static final String UBW_TARGET_RETURN_Y = "EmiyaUbwTargetReturnY";
   public static final String UBW_TARGET_RETURN_Z = "EmiyaUbwTargetReturnZ";
   public static final String UBW_TARGET_PRIMARY = "EmiyaUbwTargetPrimary";
   public static final String UBW_OFFSCREEN_DUEL = "EmiyaUbwOffscreenDuel";
   public static final String UBW_OFFSCREEN_PREVIOUS_INVISIBLE = "EmiyaUbwOffscreenPrevInvisible";
   public static final String UBW_OFFSCREEN_PREVIOUS_INVULNERABLE = "EmiyaUbwOffscreenPrevInvulnerable";
   public static final String UBW_OFFSCREEN_PREVIOUS_NO_AI = "EmiyaUbwOffscreenPrevNoAi";
   private static final String LAST_PERSISTENT_STATE_TICK = "EmiyaLastPersistentStateTick";
   public static final int SPIRAL_COOLDOWN = 18 * 20;
   public static final int CRIMSON_COOLDOWN = 16 * 20;
   public static final int RHO_AIAS_COOLDOWN = 15 * 20;
   public static final int RHO_AIAS_HARD_COOLDOWN = 15 * 20;
   public static final int BROKEN_PHANTASM_COOLDOWN = 12 * 20;
   public static final int REINFORCEMENT_COOLDOWN = 9 * 20;
   public static final int UBW_COOLDOWN = 60 * 20;
   public static final int UBW_CHANT_TICKS = 8 * 20;
   public static final int PROJECTION_VOLLEY_COOLDOWN = 3 * 20;
   public static final int IRON_SWORD_SHOT_COOLDOWN = 30;
   public static final int IRON_SWORD_ASSIST_COOLDOWN = 90;
   public static final int FOCUSED_SWORD_BARREL_COOLDOWN = 8 * 20;
   public static final int CROSS_BLADE_COOLDOWN = 5 * 20;
   public static final int ORB_BURST_COOLDOWN = 4 * 20;
   public static final int CLAIRVOYANCE_SPIRAL_COOLDOWN = 12 * 20;
   public static final int RANGED_STANDOFF_MIN_TICKS = 8 * 20;
   public static final int PROBING_RANGED_SPECIAL_COOLDOWN = 10 * 20;
   public static final int ANALYSIS_COOLDOWN = 8 * 20;
   public static final int MIND_EYE_STEP_COOLDOWN = 4 * 20;
   public static final int TWIN_FLURRY_COOLDOWN = 3 * 20;
   public static final int TWIN_UPPERCUT_COOLDOWN = 4 * 20;
   public static final int TWIN_REPEL_COOLDOWN = 3 * 20;
   public static final int CHASING_THRUST_COOLDOWN = 4 * 20;
   public static final int PROJECTION_IMPACT_COOLDOWN = 6 * 20;
   public static final int OVEREDGE_CLEAVE_COOLDOWN = 5 * 20;
   public static final int BLADE_RUPTURE_COOLDOWN = 7 * 20;
   public static final int AERIAL_PURSUIT_COOLDOWN = 5 * 20;
   public static final int REINFORCED_SLAM_COOLDOWN = 4 * 20;
   public static final int BORROWED_NP_PROJECTION_TICKS = 5 * 20;
   private static final double BORROWED_GALLATIN_RANGE = 100.0;
   private static final double BORROWED_GALLATIN_HALF_ANGLE_COS = Math.cos(Math.toRadians(35.0));
   private static final String MODE_MELEE = "melee";
   private static final String MODE_RANGED = "ranged";
   private static final double TWIN_SWORD_COST = 8.0;
   private static final double OVEREDGE_PAIR_COST = 15.0;
   private static final int UBW_MIN_DURATION_TICKS = 15 * 20;
   private static final int UBW_CHANT_SURFACE_SPREAD_DELAY = 3 * 20;
   private static final int UBW_CHANT_SURFACE_RADIUS = 14;
   private static final int UBW_RELOCK_TICKS = 3;
   private static final int UBW_CRIMSON_HOUND_INTERVAL = 10 * 20;
   private static final int UBW_TERRAIN_RADIUS = 16;
   private static final double UBW_PULL_RADIUS = 32.0;
   private static final Map<UUID, Map<BlockPos, BlockBackup>> EMIYA_UBW_BLOCKS = new HashMap<>();
   private static final Map<UUID, Map<BlockPos, BlockBackup>> EMIYA_UBW_CHANT_BLOCKS = new HashMap<>();

   private EmiyaArcherCombatHelper() {
   }

   public static void tick(EmiyaArcherEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      if (!entity.isAlive() || entity.isSpiritualDissolving()) {
         cleanupUbw(entity, level);
         return;
      }

      long now = level.getGameTime();
      tickPersistentState(entity);
      if (entity.getPersistentData().getBoolean(UBW_OFFSCREEN_DUEL)) {
         entity.getNavigation().stop();
         if (now % 10L == 0L) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 0.35, entity.getZ(), 4, 0.25, 0.08, 0.25, 0.01);
         }
         return;
      }
      tickNpcAutoCounter(entity, level, now);

      LivingEntity target = entity.getTarget();
      if ((target == null || !target.isAlive()) && entity.getPersistentData().hasUUID(UBW_LOCKED_TARGET)) {
         target = findLivingByUuid(level, entity.getPersistentData().getUUID(UBW_LOCKED_TARGET));
         if (target != null && target.isAlive()) {
            forceCombatTarget(entity, target, level);
         }
      }
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         if (entity.getPersistentData().getLong(UBW_CHANT_END_TICK) > 0L) {
            restoreUbwChantTerrain(entity, level, Integer.MAX_VALUE);
            entity.getPersistentData().remove(UBW_CHANT_END_TICK);
            entity.getPersistentData().remove(UBW_CHANT_TARGET_ID);
         }
         if (tryClairvoyanceSpiralShot(entity, level, now)) {
            return;
         }
         clearProjection(entity, true);
         return;
      }

      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      double distance = entity.distanceTo(target);
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      boolean normalOrDecisive = phase.id() >= ServantCombatPhase.NORMAL.id();
      boolean decisive = phase == ServantCombatPhase.DECISIVE;
      RhoAiasEntity ownedShield = findOwnedRhoAias(entity, level);
      boolean probingMeleeProbe = phase == ServantCombatPhase.PROBING && ownedShield == null && distance <= 7.0 && entity.getRandom().nextInt(100) < 35;
      boolean forceMeleeRange = (distance <= 5.2 || probingMeleeProbe) && entity.getPersistentData().getLong(UBW_CHANT_END_TICK) <= 0L;
      boolean rangedMode = !forceMeleeRange && (ownedShield != null || shouldUseRangedMode(entity, target, distance, now));
      setCombatMode(entity, rangedMode ? MODE_RANGED : MODE_MELEE);
      updateRangedStandoff(entity, target, rangedMode, distance, now);

      if (tickUbwChant(entity, level, target, now)) {
         return;
      }
      if (tickNpcSphericalProjection(entity, level, now)) {
         return;
      }

      maybeCastReinforcement(entity, level, target, now, phase);

      if (shouldCastUbw(entity, target, now, phase)) {
         beginUbwChant(entity, level, target, now);
         return;
      }

      if (phase == ServantCombatPhase.PROBING
         && distance <= 6.0
         && canUse(now, entity.getPersistentData().getLong(LAST_HRUNTING_STYLE_TICK), phasedCooldown(240, phase))
         && entity.getCurrentMp() >= 30.0
         && entity.getRandom().nextInt(100) < 12) {
         executeKanshouBakuyaTriple(entity, level, target, now);
         return;
      }

      if (rangedMode) {
         equipBow(entity);
         RhoAiasEntity activeShield = ownedShield != null && ownedShield.isAlive() ? ownedShield : findOwnedRhoAias(entity, level);
         if (activeShield != null) {
            stayBehindRhoAias(entity, activeShield, target);
            if (tryDetonateRhoAias(entity, level, activeShield, target, now, phase)) {
               return;
            }
         }
          if (tryProbingRangedStandoffSpecial(entity, level, target, now, phase, distance)) {
            return;
         }
         if (normalOrDecisive && tryStartNpcAutoCounter(entity, level, target, now, phase)) {
            return;
         }
         if (normalOrDecisive && distance >= 6.0 && tryStartNpcSphericalProjection(entity, level, target, now, phase)) {
            return;
         }
         if (canUse(now, entity.getPersistentData().getLong(LAST_IRON_SWORD_SHOT_TICK), IRON_SWORD_SHOT_COOLDOWN)) {
            shootIronSword(entity, level, target, now);
            return;
         }
         if (normalOrDecisive && shouldUseCrimsonHound(entity, target, now, phase) && canUse(now, entity.getPersistentData().getLong(LAST_CRIMSON_TICK), phasedCooldown(CRIMSON_COOLDOWN, phase)) && entity.getCurrentMp() >= 20.0) {
            castCrimsonHound(entity, level, target, now);
            return;
         }
         if (decisive && shouldUsePseudoSpiralSword(entity, target, now, phase) && canUse(now, entity.getPersistentData().getLong(LAST_SPIRAL_TICK), phasedCooldown(SPIRAL_COOLDOWN, phase)) && entity.getCurrentMp() >= 25.0) {
            castPseudoSpiralSword(entity, level, target, now);
            return;
         }
         if (normalOrDecisive
            && distance >= 7.0
            && canUse(now, entity.getPersistentData().getLong(LAST_FOCUSED_SWORD_BARREL_TICK), phasedCooldown(FOCUSED_SWORD_BARREL_COOLDOWN, phase))
            && entity.getCurrentMp() >= 18.0
            && (decisive || entity.getRandom().nextInt(100) < phaseChance(28, phase))) {
            castFocusedSwordBarrel(entity, level, target, now, phase);
            return;
         }
         if (normalOrDecisive
            && distance >= 5.0
            && canUse(now, entity.getPersistentData().getLong(LAST_CROSS_BLADE_TICK), phasedCooldown(CROSS_BLADE_COOLDOWN, phase))
            && entity.getCurrentMp() >= 10.0
            && entity.getRandom().nextInt(100) < phaseChance(26, phase)) {
            castCrossBladePincer(entity, level, target, now, phase);
            return;
         }
         if (canUse(now, entity.getPersistentData().getLong(LAST_ORB_BURST_TICK), phasedCooldown(ORB_BURST_COOLDOWN, phase))
            && entity.getRandom().nextInt(100) < phaseChance(18, phase)) {
            castOrbBurst(entity, level, target, now);
            return;
         }
         if (normalOrDecisive && canUse(now, entity.getPersistentData().getLong(LAST_PROJECTION_VOLLEY_TICK), phasedCooldown(PROJECTION_VOLLEY_COOLDOWN, phase)) && entity.getCurrentMp() >= 12.0) {
            castProjectionVolley(entity, level, target, now);
            return;
         }
         if (activeShield != null) {
            stayBehindRhoAias(entity, activeShield, target);
         } else {
            ServantEngagementService.maintainRangedPosition(
               entity,
               target,
               now,
               10.0,
               14.0,
               18.0,
               1.15,
               "EmiyaRangedPosition"
            );
         }
         maybeShield(entity, level, target, now, phase);
         return;
      }

      if (equipMeleeWeapon(entity, level, target, now)) {
         return;
      }
      if (distance > 7.0 && canUse(now, entity.getPersistentData().getLong(LAST_IRON_SWORD_SHOT_TICK), IRON_SWORD_ASSIST_COOLDOWN)) {
         shootIronSword(entity, level, target, now);
         return;
      }
      if (distance >= 3.2 && distance <= 7.0
         && canUse(now, entity.getPersistentData().getLong(LAST_CHASING_THRUST_TICK), phasedCooldown(CHASING_THRUST_COOLDOWN, phase))
         && entity.getCurrentMp() >= 7.0) {
         performChasingThrust(entity, level, target, now);
         return;
      }
      if (distance <= 3.2 && canUse(now, entity.getPersistentData().getLong(LAST_MIND_EYE_STEP_TICK), phasedCooldown(MIND_EYE_STEP_COOLDOWN, phase)) && entity.getCurrentMp() >= 8.0 && (phase == ServantCombatPhase.PROBING || entity.getRandom().nextInt(100) < 35)) {
         performMindEyeStep(entity, level, target, now);
         return;
      }
      if (hasAnalyzedWeaponProjectionBuff(entity, now)
         && distance <= 5.8
         && canUse(now, entity.getPersistentData().getLong(LAST_REINFORCED_SLAM_TICK), phasedCooldown(REINFORCED_SLAM_COOLDOWN, phase))
         && entity.getCurrentMp() >= 12.0
         && entity.getRandom().nextInt(100) < phaseChance(42, phase)) {
         performReinforcedProjectionSlam(entity, level, target, now);
         return;
      }
      if (decisive
         && distance <= 3.5
         && canUse(now, entity.getPersistentData().getLong(LAST_BROKEN_PHANTASM_TICK), phasedCooldown(BROKEN_PHANTASM_COOLDOWN, phase))
         && entity.getHealth() < entity.getMaxHealth() * 0.4F
         && entity.getRandom().nextInt(100) < 30) {
         triggerBrokenPhantasm(entity, level, now, 0.2F);
         return;
      }
      if (distance <= 3.8
         && normalOrDecisive
         && canUse(now, entity.getPersistentData().getLong(LAST_TWIN_UPPERCUT_TICK), phasedCooldown(TWIN_UPPERCUT_COOLDOWN, phase))
         && entity.getCurrentMp() >= 6.0
         && entity.getRandom().nextInt(100) < phaseChance(36, phase)) {
         performTwinUppercut(entity, level, target, now);
         return;
      }
      if (distance <= 4.4
         && normalOrDecisive
         && canUse(now, entity.getPersistentData().getLong(LAST_AERIAL_PURSUIT_TICK), phasedCooldown(AERIAL_PURSUIT_COOLDOWN, phase))
         && entity.getCurrentMp() >= 10.0
         && entity.getRandom().nextInt(100) < phaseChance(28, phase)) {
         performAerialPursuit(entity, level, target, now);
         return;
      }
      if (distance <= 4.2
         && normalOrDecisive
         && canUse(now, entity.getPersistentData().getLong(LAST_PROJECTION_IMPACT_TICK), phasedCooldown(PROJECTION_IMPACT_COOLDOWN, phase))
         && entity.getCurrentMp() >= 10.0
         && (nearbyEnemyCount(entity, level, 4.5) >= 2 || entity.getRandom().nextInt(100) < phaseChance(22, phase))) {
         performProjectionImpact(entity, level, target, now);
         return;
      }
      if (distance <= 4.8
         && normalOrDecisive
         && canUse(now, entity.getPersistentData().getLong(LAST_OVEREDGE_CLEAVE_TICK), phasedCooldown(OVEREDGE_CLEAVE_COOLDOWN, phase))
         && entity.getCurrentMp() >= 14.0
         && (nearbyEnemyCount(entity, level, 4.8) >= 2 || entity.getRandom().nextInt(100) < phaseChance(34, phase))) {
         performOveredgeCleave(entity, level, target, now);
         return;
      }
      if (distance <= 3.6
         && decisive
         && canUse(now, entity.getPersistentData().getLong(LAST_BLADE_RUPTURE_TICK), phasedCooldown(BLADE_RUPTURE_COOLDOWN, phase))
         && entity.getCurrentMp() >= 16.0
         && (nearbyEnemyCount(entity, level, 4.0) >= 2 || entity.getRandom().nextInt(100) < phaseChance(24, phase))) {
         performBladeRupture(entity, level, target, now);
         return;
      }
      if (distance <= 4.0
         && canUse(now, entity.getPersistentData().getLong(LAST_TWIN_REPEL_TICK), phasedCooldown(TWIN_REPEL_COOLDOWN, phase))
         && entity.getCurrentMp() >= 6.0
         && (nearbyEnemyCount(entity, level, 3.2) >= 2 || entity.getRandom().nextInt(100) < phaseChance(32, phase))) {
         performTwinRepel(entity, level, target, now);
         return;
      }
      if (distance <= 5.0 && canUse(now, entity.getPersistentData().getLong(LAST_TWIN_FLURRY_TICK), phasedCooldown(TWIN_FLURRY_COOLDOWN, phase)) && entity.getCurrentMp() >= 12.0) {
         performTwinSwordFlurry(entity, level, target, now);
         return;
      }
      if (normalOrDecisive && distance <= 6.5 && canUse(now, entity.getPersistentData().getLong(LAST_HRUNTING_STYLE_TICK), phasedCooldown(240, phase)) && entity.getCurrentMp() >= 30.0) {
         executeKanshouBakuyaTriple(entity, level, target, now);
         return;
      }
      if (normalOrDecisive && distance <= 8.0 && canCastRhoAias(entity, now, phasedCooldown(RHO_AIAS_COOLDOWN, phase)) && entity.getCurrentMp() >= 35.0 && target.getLastHurtByMob() != null) {
         castRhoAias(entity, level, target, now);
      }
   }

   public static void tickPersistentState(EmiyaArcherEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      if (!entity.isAlive() || entity.isSpiritualDissolving()) {
         cleanupUbw(entity, level);
         return;
      }
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (data.contains(LAST_PERSISTENT_STATE_TICK) && data.getLong(LAST_PERSISTENT_STATE_TICK) == now) return;
      data.putLong(LAST_PERSISTENT_STATE_TICK, now);
      if (data.getBoolean(UBW_OFFSCREEN_DUEL)) return;
      tickUbw(entity, level, now);
      expireProjection(entity, now);
      tickUbwTargetRelock(entity, level, now);
   }

   public static void markProjectionExpiry(ServantEntity entity, long expiresAt, boolean pair) {
      entity.getPersistentData().putLong(PROJECTED_EXPIRES_TICK, expiresAt);
      entity.getPersistentData().putBoolean(PROJECTED_PAIR, pair);
   }

   public static void clearProjection(ServantEntity entity) {
      clearProjection(entity, false);
   }

   public static void cleanupUbw(EmiyaArcherEntity entity) {
      if (entity.level() instanceof ServerLevel level) {
         cleanupUbw(entity, level);
      }
   }

   public static boolean forceCastRhoAiasAgainstNoblePhantasm(EmiyaArcherEntity entity, LivingEntity threat, long now) {
      if (entity == null || threat == null || !entity.isAlive() || !threat.isAlive() || !(entity.level() instanceof ServerLevel level)) {
         return false;
      }
      RhoAiasEntity shield = findOwnedRhoAias(entity, level);
      if (shield == null || !shield.isAlive()) {
         if (!canCastRhoAias(entity, now, RHO_AIAS_HARD_COOLDOWN)) {
            return false;
         }
         castRhoAias(entity, level, threat, now, false);
         shield = findOwnedRhoAias(entity, level);
      } else {
         entity.getPersistentData().putLong(LAST_RHO_AIAS_TICK, now);
         entity.faceToward(threat.position().add(0.0, threat.getBbHeight() * 0.5, 0.0));
      }
      if (shield != null && shield.isAlive()) {
         setCombatMode(entity, MODE_RANGED);
         stayBehindRhoAias(entity, shield, threat);
      }
      return true;
   }

   private static void cleanupUbw(EmiyaArcherEntity entity, ServerLevel level) {
      if (UBWInstanceManager.isUbwDimension(level)) {
         returnFromUbw(entity, level);
         return;
      }
      restoreUbwChantTerrain(entity, level, Integer.MAX_VALUE);
      restoreUbwTerrain(entity, level, Integer.MAX_VALUE);
      clearUbwState(entity);
   }

   private static void clearProjection(ServantEntity entity, boolean refundMana) {
      boolean hadProjectedPair = entity.getPersistentData().getBoolean(PROJECTED_PAIR);
      if (!entity.getMainHandItem().isEmpty()) {
         entity.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      }
      if (!entity.getOffhandItem().isEmpty()) {
         entity.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
      }
      if (refundMana && hadProjectedPair) {
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 4.0));
      }
      entity.getPersistentData().remove(PROJECTED_EXPIRES_TICK);
      entity.getPersistentData().remove(PROJECTED_PAIR);
      entity.getPersistentData().remove(ANALYZED_WEAPON_EXPIRES_TICK);
      entity.getPersistentData().remove(ANALYZED_WEAPON_STACK);
      entity.getPersistentData().remove(BORROWED_NP_USED_UNTIL);
   }

   private static void expireProjection(EmiyaArcherEntity entity, long now) {
      long expiresAt = entity.getPersistentData().getLong(PROJECTED_EXPIRES_TICK);
      if (expiresAt > 0L && now >= expiresAt) {
         clearProjection(entity);
         if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(), 8, 0.2, 0.25, 0.2, 0.01);
         }
      }
   }

   private static boolean shouldCastUbw(EmiyaArcherEntity entity, LivingEntity target, long now, ServantCombatPhase phase) {
      if (entity.getPersistentData().getLong(UBW_CHANT_END_TICK) > now) {
         return false;
      }
      if (entity.getPersistentData().getLong(UBW_ACTIVE_UNTIL) > now) {
         return false;
      }
      if (!canUse(now, entity.getPersistentData().getLong(LAST_UBW_TICK), UBW_COOLDOWN)) {
         return false;
      }
      if (entity.getCurrentMp() < 120.0) {
         return false;
      }
      if (entity.level() instanceof ServerLevel level && UBWInstanceManager.isDimensionOccupied(level.getServer(), ModDimensions.EMIYA_UBW_KEY, entity.getUUID())) {
         return false;
      }
      if (phase == ServantCombatPhase.PROBING) {
         return false;
      }
      if (entity.getHealth() > entity.getMaxHealth() * 0.85F && entity.distanceTo(target) < 12.0) {
         return false;
      }
      List<LivingEntity> threats = entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(12.0),
         living -> living != entity && living.isAlive() && !living.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(living));
      return phase == ServantCombatPhase.DECISIVE && (threats.size() >= 2 || target.getHealth() > 90.0F)
         || threats.size() >= 3
         || target.getHealth() > 140.0F;
   }

   private static void maybeCastReinforcement(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      if (target == null || !target.isAlive() || entity.getCurrentMp() < 12.0) {
         return;
      }
      if (!canUse(now, entity.getPersistentData().getLong(LAST_REINFORCEMENT_TICK), phasedCooldown(REINFORCEMENT_COOLDOWN, phase))) {
         return;
      }
      int chance = phase == ServantCombatPhase.DECISIVE ? 34 : phase == ServantCombatPhase.NORMAL ? 24 : 12;
      if (entity.getHealth() <= entity.getMaxHealth() * 0.45F) {
         chance += 10;
      }
      if (entity.getRandom().nextInt(100) >= chance) {
         return;
      }
      entity.getPersistentData().putLong(LAST_REINFORCEMENT_TICK, now);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 12.0));
      int duration = phase == ServantCombatPhase.DECISIVE ? 120 : 90;
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false, true));
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(), 16, 0.28, 0.35, 0.28, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.6F, 1.35F);
   }

   private static void beginUbwChant(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_UBW_TICK, now);
      entity.getPersistentData().putLong(UBW_CHANT_END_TICK, now + UBW_CHANT_TICKS);
      entity.getPersistentData().putInt(UBW_CHANT_TARGET_ID, target.getId());
      entity.setCurrentMp(entity.getCurrentMp() - 100.0);
      ServantVoiceHelper.tryPlayEmiyaUbw(entity);
      entity.triggerNamedActionAnimation("ubw_chant");
      entity.getNavigation().stop();
      equipBow(entity);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 0.65F);
   }

   private static boolean tickUbwChant(EmiyaArcherEntity entity, ServerLevel level, LivingEntity fallbackTarget, long now) {
      long chantEnd = entity.getPersistentData().getLong(UBW_CHANT_END_TICK);
      if (chantEnd <= 0L) {
         return false;
      }

      if (fallbackTarget != null && fallbackTarget.isAlive()) {
         entity.faceToward(fallbackTarget.position().add(0.0, fallbackTarget.getBbHeight() * 0.5, 0.0));
         RhoAiasEntity shield = findOwnedRhoAias(entity, level);
         if (shield != null) {
            stayBehindRhoAias(entity, shield, fallbackTarget);
         } else if (canCastRhoAias(entity, now, RHO_AIAS_HARD_COOLDOWN) && entity.getCurrentMp() >= 35.0) {
            castRhoAias(entity, level, fallbackTarget, now);
         } else {
            moveDuringUbwChant(entity, fallbackTarget);
         }
      }
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 0, false, false, true));
      if (now % 5L == 0L) {
         double progress = 1.0 - Math.max(0.0, chantEnd - now) / (double)UBW_CHANT_TICKS;
         double radius = 1.8 + progress * 5.5;
         for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * (i / 8.0) + now * 0.08;
            level.sendParticles(
               ParticleTypes.FLAME,
               entity.getX() + Math.cos(angle) * radius,
               entity.getY() + 0.08,
               entity.getZ() + Math.sin(angle) * radius,
               1,
               0.04,
               0.02,
               0.04,
               0.0
            );
         }
         level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.75, entity.getZ(), 10, 0.45, 0.5, 0.45, 0.02);
      }
      if (now % 12L == 0L) {
         spawnUbwChantFallingSwords(entity, level, fallbackTarget);
      }
      if (now % 4L == 0L) {
         spreadUbwChantSurfaceFromCaster(entity, level, now, 16);
      }
      if (now < chantEnd) {
         return true;
      }

      int targetId = entity.getPersistentData().getInt(UBW_CHANT_TARGET_ID);
      Entity resolved = targetId > 0 ? level.getEntity(targetId) : null;
      LivingEntity target = resolved instanceof LivingEntity living && living.isAlive() ? living : fallbackTarget;
      entity.getPersistentData().remove(UBW_CHANT_END_TICK);
      entity.getPersistentData().remove(UBW_CHANT_TARGET_ID);
      restoreUbwChantTerrain(entity, level, Integer.MAX_VALUE);
      if (target != null && target.isAlive() && entity.isAlive()) {
         activateUbw(entity, level, target, now);
      }
      return true;
   }

   private static void moveDuringUbwChant(EmiyaArcherEntity entity, LivingEntity target) {
      double distance = entity.distanceTo(target);
      if (distance > 12.0) {
         ServantNavigationHelper.moveToTargetThrottled(
            entity,
            target,
            1.0,
            entity.level().getGameTime(),
            ServantNavigationHelper.DEFAULT_REPATH_INTERVAL,
            1.0,
            "EmiyaUbwChantChasePath"
         );
         return;
      }
      if (distance < 5.0) {
         Vec3 away = entity.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
         if (away.lengthSqr() > 1.0E-4) {
            Vec3 retreat = entity.position().add(away.normalize().scale(3.0));
            ServantNavigationHelper.moveToPositionThrottled(
               entity,
               retreat,
               1.0,
               entity.level().getGameTime(),
               ServantNavigationHelper.SHORT_REPATH_INTERVAL,
               1.0,
               "EmiyaUbwChantRetreatPath"
            );
            return;
         }
      }
      ServantNavigationHelper.stopIfMoving(entity);
   }

   private static void activateUbw(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      recordUbwLockedTarget(entity, target);
      List<LivingEntity> pulledTargets = collectUbwTargets(entity, level, target);
      if (!pulledTargets.isEmpty() && pulledTargets.stream().noneMatch(ServerPlayer.class::isInstance)) {
         startOffscreenUbwDuel(entity, level, pulledTargets.get(0), now);
         return;
      }

      entity.getPersistentData().putString(UBW_RETURN_DIMENSION, level.dimension().location().toString());
      entity.getPersistentData().putDouble(UBW_RETURN_X, entity.getX());
      entity.getPersistentData().putDouble(UBW_RETURN_Y, entity.getY());
      entity.getPersistentData().putDouble(UBW_RETURN_Z, entity.getZ());
      UUID lockedTargetId = entity.getPersistentData().hasUUID(UBW_LOCKED_TARGET) ? entity.getPersistentData().getUUID(UBW_LOCKED_TARGET) : null;
      entity.triggerChargeAnimation();
      BlockPos sourceCenter = entity.blockPosition();
      level.playSound(null, sourceCenter, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2F, 0.65F);
      for (int i = 0; i < 36; i++) {
         double angle = Math.PI * 2.0 * i / 36.0;
         double radius = 5.0 + (i % 5);
         double px = entity.getX() + Math.cos(angle) * radius;
         double pz = entity.getZ() + Math.sin(angle) * radius;
         level.sendParticles(ParticleTypes.FLAME, px, entity.getY() + 0.1, pz, 3, 0.15, 0.03, 0.15, 0.01);
         level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, entity.getY() + 0.15, pz, 2, 0.08, 0.02, 0.08, 0.0);
      }

      ServerLevel ubwLevel = UBWInstanceManager.getOrCreateFreshInstance(level.getServer(), entity);
      if (ubwLevel == null) {
         clearUbwState(entity);
         return;
      }

      Vec3 randomEntry = UBWInstanceManager.randomEntryPosition(entity.getRandom());
      double entryX = randomEntry.x;
      double entryZ = randomEntry.z;
      double entryY = findSafeSpawnY(ubwLevel, (int)entryX, (int)entryZ);
      Vec3 entryPos = new Vec3(entryX, entryY, entryZ);
      BlockPos entryBlock = BlockPos.containing(entryPos);
      applyUbwState(entity, now, entryBlock);

      LivingEntity ubwTarget = moveTargetsIntoUbw(entity, level, ubwLevel, pulledTargets, target, entryPos);
      Entity moved = entity.changeDimension(new DimensionTransition(
         ubwLevel,
         entryPos,
         Vec3.ZERO,
         entity.getYRot(),
         entity.getXRot(),
         DimensionTransition.DO_NOTHING
      ));
      if (moved instanceof EmiyaArcherEntity archer) {
         applyUbwState(archer, now, entryBlock);
         archer.getPersistentData().putString(UBW_RETURN_DIMENSION, level.dimension().location().toString());
         archer.getPersistentData().putDouble(UBW_RETURN_X, entity.getPersistentData().getDouble(UBW_RETURN_X));
         archer.getPersistentData().putDouble(UBW_RETURN_Y, entity.getPersistentData().getDouble(UBW_RETURN_Y));
         archer.getPersistentData().putDouble(UBW_RETURN_Z, entity.getPersistentData().getDouble(UBW_RETURN_Z));
         if (lockedTargetId != null) {
            archer.getPersistentData().putUUID(UBW_LOCKED_TARGET, lockedTargetId);
         }
         LivingEntity lockedInUbw = lockedTargetId != null ? findLivingByUuid(ubwLevel, lockedTargetId) : null;
         LivingEntity targetInUbw = lockedInUbw != null && lockedInUbw.isAlive() ? lockedInUbw : ubwTarget;
         if (targetInUbw != null && targetInUbw.isAlive()) {
            forceCombatTarget(archer, targetInUbw, ubwLevel);
            scheduleUbwTargetRelock(archer, targetInUbw.getUUID(), ubwLevel.getGameTime());
         } else if (lockedTargetId != null) {
            scheduleUbwTargetRelock(archer, lockedTargetId, ubwLevel.getGameTime());
         }
         ubwLevel.playSound(null, entryBlock, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2F, 0.65F);
         ubwLevel.sendParticles(ParticleTypes.FLAME, entryPos.x, entryPos.y + 0.2, entryPos.z, 48, 4.0, 0.12, 4.0, 0.03);
         spawnUbwGears(archer, ubwLevel);
      }
      UBWInstanceManager.keepInstanceTicking(entity.getUUID(), ubwLevel, entryBlock);
   }

   private static List<LivingEntity> collectUbwTargets(EmiyaArcherEntity owner, ServerLevel source, LivingEntity primary) {
      List<LivingEntity> targets = new ArrayList<>();
      if (isInUbwPullHemisphere(owner, primary, source)) {
         targets.add(primary);
      }
      for (LivingEntity living : source.getEntitiesOfClass(
         LivingEntity.class,
         owner.getBoundingBox().inflate(UBW_PULL_RADIUS),
         living -> living != primary
            && isInUbwPullHemisphere(owner, living, source)
      )) {
         targets.add(living);
      }
      return targets;
   }

   private static boolean isInUbwPullHemisphere(EmiyaArcherEntity owner, LivingEntity living, ServerLevel source) {
      if (living == null || !living.isAlive() || living == owner || living.level() != source) {
         return false;
      }
      Vec3 rel = living.position().subtract(owner.position());
      return rel.y >= -2.0 && rel.lengthSqr() <= UBW_PULL_RADIUS * UBW_PULL_RADIUS;
   }

   private static void applyUbwState(EmiyaArcherEntity entity, long now, BlockPos center) {
      entity.getPersistentData().putLong(UBW_ACTIVE_UNTIL, now + 20L * 20L);
      entity.getPersistentData().putLong(UBW_MIN_UNTIL, now + UBW_MIN_DURATION_TICKS);
      entity.getPersistentData().putLong(UBW_NEXT_RAIN, now + 20L);
      entity.getPersistentData().putLong(UBW_NEXT_INTERCEPT, now + 8L);
      entity.getPersistentData().putLong(UBW_NEXT_TERRAIN, now + 1L);
      entity.getPersistentData().putLong(UBW_NEXT_BLADE_LIFT, now + 18L);
      entity.getPersistentData().putLong(UBW_NEXT_CRIMSON_HOUND, now + UBW_CRIMSON_HOUND_INTERVAL);
      entity.getPersistentData().putInt(UBW_CENTER_X, center.getX());
      entity.getPersistentData().putInt(UBW_CENTER_Y, center.getY());
      entity.getPersistentData().putInt(UBW_CENTER_Z, center.getZ());
      entity.getPersistentData().putBoolean(UBW_GEARS_SPAWNED, false);
   }

   private static void clearUbwState(EmiyaArcherEntity entity) {
      entity.getPersistentData().remove(UBW_ACTIVE_UNTIL);
      entity.getPersistentData().remove(UBW_MIN_UNTIL);
      entity.getPersistentData().remove(UBW_CHANT_END_TICK);
      entity.getPersistentData().remove(UBW_CHANT_TARGET_ID);
      entity.getPersistentData().remove(UBW_NEXT_RAIN);
      entity.getPersistentData().remove(UBW_NEXT_INTERCEPT);
      entity.getPersistentData().remove(UBW_NEXT_TERRAIN);
      entity.getPersistentData().remove(UBW_NEXT_BLADE_LIFT);
      entity.getPersistentData().remove(UBW_NEXT_CRIMSON_HOUND);
      entity.getPersistentData().remove(UBW_CENTER_X);
      entity.getPersistentData().remove(UBW_CENTER_Y);
      entity.getPersistentData().remove(UBW_CENTER_Z);
      entity.getPersistentData().remove(UBW_GEARS_SPAWNED);
      entity.getPersistentData().remove(UBW_RETURN_DIMENSION);
      entity.getPersistentData().remove(UBW_RETURN_X);
      entity.getPersistentData().remove(UBW_RETURN_Y);
      entity.getPersistentData().remove(UBW_RETURN_Z);
      entity.getPersistentData().remove(UBW_LOCKED_TARGET);
      entity.getPersistentData().remove(UBW_RELOCK_TARGET);
      entity.getPersistentData().remove(UBW_RELOCK_UNTIL);
      EMIYA_UBW_CHANT_BLOCKS.remove(entity.getUUID());
      clearOffscreenDuelState(entity);
   }

   private static void recordUbwLockedTarget(EmiyaArcherEntity entity, LivingEntity target) {
      if (target != null && target.isAlive()) {
         entity.getPersistentData().putUUID(UBW_LOCKED_TARGET, target.getUUID());
      }
   }

   private static void startOffscreenUbwDuel(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (target == null || !target.isAlive() || target instanceof ServerPlayer) {
         return;
      }
      int duration = 100 + entity.getRandom().nextInt(101);
      Vec3 center = entity.position().add(target.position()).scale(0.5);
      entity.triggerNamedActionAnimation("ubw_chant");
      level.playSound(null, BlockPos.containing(center), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.15F, 0.58F);
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.2, center.z, 44, 2.2, 0.25, 2.2, 0.04);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.25, center.z, 32, 1.2, 0.18, 1.2, 0.03);
      hideForOffscreenDuel(entity);
      hideForOffscreenDuel(target);
      entity.setTarget(null);
      if (target instanceof Mob mobTarget) {
         mobTarget.setTarget(null);
      }

      double archerScore = duelScore(entity) + entity.getRandom().nextDouble() * 55.0;
      double targetScore = duelScore(target) + target.getRandom().nextDouble() * 55.0;
      int outcome = archerScore > targetScore + 16.0 ? 1 : (targetScore > archerScore + 16.0 ? -1 : 0);
      TYPE_MOON_WORLD.queueServerWork(duration, () -> finishOffscreenUbwDuel(entity, target, outcome));
   }

   private static void hideForOffscreenDuel(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.putBoolean(UBW_OFFSCREEN_DUEL, true);
      data.putBoolean(UBW_OFFSCREEN_PREVIOUS_INVISIBLE, living.isInvisible());
      data.putBoolean(UBW_OFFSCREEN_PREVIOUS_INVULNERABLE, living.isInvulnerable());
      if (living instanceof Mob mob) {
         data.putBoolean(UBW_OFFSCREEN_PREVIOUS_NO_AI, mob.isNoAi());
         mob.setNoAi(true);
         mob.getNavigation().stop();
      }
      living.setInvisible(true);
      living.setInvulnerable(true);
      living.setDeltaMovement(Vec3.ZERO);
      living.hurtMarked = true;
   }

   private static void finishOffscreenUbwDuel(EmiyaArcherEntity entity, LivingEntity target, int outcome) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = entity.position().add(target.position()).scale(0.5);
      restoreFromOffscreenDuel(entity);
      restoreFromOffscreenDuel(target);
      if (!entity.isAlive() || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.2, center.z, 34, 1.8, 0.2, 1.8, 0.035);
      level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.8, entity.getZ(), 16, 0.35, 0.35, 0.35, 0.04);
      level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + 0.8, target.getZ(), 16, 0.35, 0.35, 0.35, 0.04);
      level.playSound(null, BlockPos.containing(center), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.75F);

      if (outcome > 0) {
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().mobAttack(entity), Math.max(target.getMaxHealth() + 1.0F, 80.0F));
         target.invulnerableTime = 0;
         entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false, true));
      } else if (outcome < 0) {
         entity.invulnerableTime = 0;
         entity.hurt(target.damageSources().mobAttack(target), Math.max(entity.getMaxHealth() + 1.0F, 80.0F));
         entity.invulnerableTime = 0;
         target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false, true));
      } else {
         entity.setHealth(Math.max(1.0F, entity.getHealth() * 0.55F));
         target.setHealth(Math.max(1.0F, target.getHealth() * 0.55F));
         entity.setTarget(target);
         if (target instanceof Mob mobTarget) {
            mobTarget.setTarget(entity);
         }
      }
   }

   private static void restoreFromOffscreenDuel(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      if (!data.getBoolean(UBW_OFFSCREEN_DUEL)) {
         return;
      }
      living.setInvisible(data.getBoolean(UBW_OFFSCREEN_PREVIOUS_INVISIBLE));
      living.setInvulnerable(data.getBoolean(UBW_OFFSCREEN_PREVIOUS_INVULNERABLE));
      if (living instanceof Mob mob) {
         mob.setNoAi(data.getBoolean(UBW_OFFSCREEN_PREVIOUS_NO_AI));
      }
      clearOffscreenDuelState(living);
   }

   private static void clearOffscreenDuelState(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.remove(UBW_OFFSCREEN_DUEL);
      data.remove(UBW_OFFSCREEN_PREVIOUS_INVISIBLE);
      data.remove(UBW_OFFSCREEN_PREVIOUS_INVULNERABLE);
      data.remove(UBW_OFFSCREEN_PREVIOUS_NO_AI);
   }

   private static double duelScore(LivingEntity living) {
      double score = living.getHealth() / Math.max(1.0F, living.getMaxHealth()) * 35.0;
      score += living.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.4;
      score += living.getAttributeValue(Attributes.ARMOR) * 0.75;
      score += living.getAttributeValue(Attributes.MOVEMENT_SPEED) * 55.0;
      if (living instanceof ServantEntity servant && servant.getDefinition() != null) {
         ServantParams params = servant.getDefinition().parameters();
         score += effectiveRank(params.strength(), params.strengthPlus()) * 0.45;
         score += effectiveRank(params.agility(), params.agilityPlus()) * 0.38;
         score += effectiveRank(params.magic(), params.magicPlus()) * 0.28;
         score += effectiveRank(params.luck(), params.luckPlus()) * 0.18;
      }
      return score;
   }

   private static int effectiveRank(net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank rank, boolean plus) {
      return plus ? rank.plusCoefficient() : rank.coefficient();
   }

   private static LivingEntity moveTargetIntoUbw(EmiyaArcherEntity owner, ServerLevel source, ServerLevel ubwLevel, LivingEntity primary, Vec3 entryPos) {
      return moveTargetsIntoUbw(owner, source, ubwLevel, collectUbwTargets(owner, source, primary), primary, entryPos);
   }

   private static LivingEntity moveTargetsIntoUbw(EmiyaArcherEntity owner, ServerLevel source, ServerLevel ubwLevel, List<LivingEntity> targets, LivingEntity primary, Vec3 entryPos) {
      LivingEntity movedPrimary = moveLivingIntoUbw(owner, source, ubwLevel, primary, entryPos, true);
      for (LivingEntity living : targets) {
         if (living == primary) {
            continue;
         }
         moveLivingIntoUbw(owner, source, ubwLevel, living, entryPos, false);
      }
      return movedPrimary;
   }

   private static LivingEntity moveLivingIntoUbw(EmiyaArcherEntity owner, ServerLevel source, ServerLevel ubwLevel, LivingEntity living, Vec3 entryPos, boolean primaryTarget) {
      if (living == null || !living.isAlive() || living == owner || living.level() != source) {
         return null;
      }
      double relX = Mth.clamp(living.getX() - owner.getX(), -18.0, 18.0);
      double relZ = Mth.clamp(living.getZ() - owner.getZ(), -18.0, 18.0);
      double targetX = entryPos.x + relX;
      double targetZ = entryPos.z + relZ;
      double targetY = findSafeSpawnY(ubwLevel, Mth.floor(targetX), Mth.floor(targetZ));
      double returnX = living.getX();
      double returnY = living.getY();
      double returnZ = living.getZ();
      markPulledTarget(owner, living, source, returnX, returnY, returnZ, primaryTarget);
      Entity moved = living.changeDimension(new DimensionTransition(
         ubwLevel,
         new Vec3(targetX, targetY, targetZ),
         Vec3.ZERO,
         living.getYRot(),
         living.getXRot(),
         DimensionTransition.DO_NOTHING
      ));
      if (moved instanceof LivingEntity movedLiving) {
         markPulledTarget(owner, movedLiving, source, returnX, returnY, returnZ, primaryTarget);
         return movedLiving;
      }
      return null;
   }

   private static void markPulledTarget(EmiyaArcherEntity owner, LivingEntity target, ServerLevel source, double returnX, double returnY, double returnZ, boolean primaryTarget) {
      target.getPersistentData().putUUID(UBW_TARGET_OWNER, owner.getUUID());
      target.getPersistentData().putString(UBW_TARGET_RETURN_DIMENSION, source.dimension().location().toString());
      target.getPersistentData().putDouble(UBW_TARGET_RETURN_X, returnX);
      target.getPersistentData().putDouble(UBW_TARGET_RETURN_Y, returnY);
      target.getPersistentData().putDouble(UBW_TARGET_RETURN_Z, returnZ);
      if (primaryTarget) {
         target.getPersistentData().putBoolean(UBW_TARGET_PRIMARY, true);
      } else {
         target.getPersistentData().remove(UBW_TARGET_PRIMARY);
      }
   }

   private static int findSafeSpawnY(ServerLevel level, int x, int z) {
      MutableBlockPos pos = new MutableBlockPos(x, level.getMaxBuildHeight() - 2, z);
      for (int y = level.getMaxBuildHeight() - 2; y > level.getMinBuildHeight(); y--) {
         pos.set(x, y, z);
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.isFaceSturdy(level, pos, Direction.UP) && level.getBlockState(pos.above()).isAir()) {
            return y + 1;
         }
      }
      return Mth.clamp(64, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
   }

   private static void tickUbw(EmiyaArcherEntity entity, ServerLevel level, long now) {
      long activeUntil = entity.getPersistentData().getLong(UBW_ACTIVE_UNTIL);
      long minUntil = entity.getPersistentData().getLong(UBW_MIN_UNTIL);
      boolean inUbwDimension = UBWInstanceManager.isUbwDimension(level);
      if (activeUntil <= now) {
         if (inUbwDimension) {
            if (!entity.isAlive()
               || now >= minUntil && (entity.getCurrentMp() <= 0.0 || !hasActiveUbwEnemy(entity, level))) {
               returnFromUbw(entity, level);
               return;
            }
            entity.getPersistentData().putLong(UBW_ACTIVE_UNTIL, now + 20L);
         } else {
            restoreUbwTerrain(entity, level, 80);
            return;
         }
      }

      boolean minElapsed = minUntil <= 0L || now >= minUntil;
      if (inUbwDimension && minElapsed && !hasActiveUbwEnemy(entity, level)) {
         returnFromUbw(entity, level);
         return;
      }
      if (!entity.getPersistentData().getBoolean(UBW_GEARS_SPAWNED)) {
         spawnUbwGears(entity, level);
      }
      if (now % 20L == 0L) {
         double currentMp = entity.getCurrentMp();
         if (minElapsed) {
            currentMp -= 5.0;
         }
         entity.setCurrentMp(Math.max(0.0, currentMp));
         if (minElapsed && entity.getCurrentMp() <= 0.0) {
            entity.setCurrentMp(0.0);
            if (inUbwDimension) {
               returnFromUbw(entity, level);
            } else {
               entity.getPersistentData().remove(UBW_ACTIVE_UNTIL);
               restoreUbwTerrain(entity, level, 160);
            }
            return;
         }
         if (entity.getCurrentMp() < 20.0) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(), 2, 0.4, 0.4, 0.4, 0.0);
         }
      }

      long nextRain = entity.getPersistentData().getLong(UBW_NEXT_RAIN);
      if (now >= nextRain) {
         LivingEntity target = entity.getTarget();
         if (target != null && target.isAlive()) {
            rainSwords(entity, level, target);
            if ((now / 16L) % 3L == 0L) {
               rainGreatSwords(entity, level, target);
            }
            if (target.getHealth() <= target.getMaxHealth() * 0.35F || target.getHealth() <= 40.0F) {
               gatherSwordsOnLowHealth(entity, level, target);
            }
         }
         entity.getPersistentData().putLong(UBW_NEXT_RAIN, now + 10L);
      }

      if (now >= entity.getPersistentData().getLong(UBW_NEXT_BLADE_LIFT)) {
         LivingEntity target = entity.getTarget();
         if (target != null && target.isAlive()) {
            liftUbwBlockSwords(entity, level, target, 12);
         }
         entity.getPersistentData().putLong(UBW_NEXT_BLADE_LIFT, now + 18L);
      }

      if (now >= entity.getPersistentData().getLong(UBW_NEXT_CRIMSON_HOUND)) {
         launchUbwCrimsonHounds(entity, level);
         entity.getPersistentData().putLong(UBW_NEXT_CRIMSON_HOUND, now + UBW_CRIMSON_HOUND_INTERVAL);
      }

      if (now >= entity.getPersistentData().getLong(UBW_NEXT_INTERCEPT)) {
         interceptHostileProjectiles(entity, level);
         entity.getPersistentData().putLong(UBW_NEXT_INTERCEPT, now + 8L);
      }

      if (now >= entity.getPersistentData().getLong(UBW_NEXT_TERRAIN)) {
         spreadUbwTerrain(entity, level, 18);
         entity.getPersistentData().putLong(UBW_NEXT_TERRAIN, now + 5L);
      }
   }

   private static boolean hasActiveUbwEnemy(EmiyaArcherEntity entity, ServerLevel level) {
      UUID ownerId = entity.getUUID();
      for (Entity candidate : level.getEntities().getAll()) {
         if (candidate instanceof LivingEntity living
            && living.isAlive()
            && living != entity
            && isPulledBy(ownerId, living)
            && !living.isAlliedTo(entity)
            && !EntityUtils.isImmunePlayerTarget(living)) {
            return true;
         }
      }

      LivingEntity target = entity.getTarget();
      return target != null
         && target.isAlive()
         && target.level() == level
         && target != entity
         && !target.isAlliedTo(entity)
         && !EntityUtils.isImmunePlayerTarget(target);
   }

   private static void launchUbwCrimsonHounds(EmiyaArcherEntity entity, ServerLevel level) {
      List<LivingEntity> targets = activeUbwEnemies(entity, level);
      if (targets.isEmpty()) {
         return;
      }
      for (LivingEntity target : targets) {
         spawnUbwCrimsonHound(entity, level, target);
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.9F, 0.65F);
   }

   private static List<LivingEntity> activeUbwEnemies(EmiyaArcherEntity entity, ServerLevel level) {
      List<LivingEntity> targets = new ArrayList<>();
      UUID ownerId = entity.getUUID();
      for (Entity candidate : level.getEntities().getAll()) {
         if (candidate instanceof LivingEntity living
            && living.isAlive()
            && living != entity
            && isPulledBy(ownerId, living)
            && !living.isAlliedTo(entity)
            && !EntityUtils.isImmunePlayerTarget(living)) {
            targets.add(living);
         }
      }
      LivingEntity currentTarget = entity.getTarget();
      if (currentTarget != null
         && currentTarget.isAlive()
         && currentTarget.level() == level
         && currentTarget != entity
         && !currentTarget.isAlliedTo(entity)
         && !EntityUtils.isImmunePlayerTarget(currentTarget)
         && !targets.contains(currentTarget)) {
         targets.add(currentTarget);
      }
      return targets;
   }

   private static void spawnUbwCrimsonHound(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target) {
      double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
      double radius = 7.0 + entity.getRandom().nextDouble() * 9.0;
      Vec3 spawn = target.position().add(Math.cos(angle) * radius, 5.0 + entity.getRandom().nextDouble() * 6.0, Math.sin(angle) * radius);
      CrimsonHoundProjectileEntity projectile = new CrimsonHoundProjectileEntity(level, entity);
      projectile.setNoGravity(true);
      projectile.setPos(spawn.x, spawn.y, spawn.z);
      projectile.setTrackedTarget(target);
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0).subtract(spawn).normalize();
      projectile.setDeltaMovement(aim.scale(2.8));
      level.addFreshEntity(projectile);
      level.sendParticles(ParticleTypes.FLAME, spawn.x, spawn.y, spawn.z, 10, 0.18, 0.18, 0.18, 0.04);
   }

   private static void returnFromUbw(EmiyaArcherEntity entity, ServerLevel level) {
      UUID ownerId = entity.getUUID();
      CompoundTag data = entity.getPersistentData();
      UUID lockedTargetId = data.hasUUID(UBW_LOCKED_TARGET) ? data.getUUID(UBW_LOCKED_TARGET) : null;
      ServerLevel returnLevel = resolveDimensionOrOverworld(level, data.getString(UBW_RETURN_DIMENSION));
      Vec3 returnPos = new Vec3(
         data.getDouble(UBW_RETURN_X),
         data.getDouble(UBW_RETURN_Y),
         data.getDouble(UBW_RETURN_Z)
      );
      UUID primaryReturnedId = returnPulledTargets(ownerId, level, returnLevel);
      clearUbwState(entity);
      if (entity.isAlive()) {
         Entity moved = entity.changeDimension(new DimensionTransition(
            returnLevel,
            returnPos,
            Vec3.ZERO,
            entity.getYRot(),
            entity.getXRot(),
            DimensionTransition.DO_NOTHING
         ));
         if (moved instanceof EmiyaArcherEntity returned) {
            clearUbwState(returned);
            returned.getNavigation().stop();
            UUID relockTargetId = lockedTargetId != null ? lockedTargetId : primaryReturnedId;
            restoreReturnedCombatTargets(returned, returnLevel, ownerId, relockTargetId);
            scheduleUbwTargetRelock(returned, relockTargetId, returnLevel.getGameTime());
         }
      }
      UBWInstanceManager.scheduleDeleteInstance(level.getServer(), ownerId);
   }

   /** Breaks an NPC-owned UBW when an anti-world noble phantasm is fired. */
   public static boolean breakUbwForEa(EmiyaArcherEntity entity) {
      if (entity == null || !(entity.level() instanceof ServerLevel level)
         || !UBWInstanceManager.isUbwDimension(level)) {
         return false;
      }
      returnFromUbw(entity, level);
      return true;
   }

   private static UUID returnPulledTargets(UUID ownerId, ServerLevel sourceLevel, ServerLevel fallbackReturnLevel) {
      List<LivingEntity> toReturn = new java.util.ArrayList<>();
      for (Entity candidate : sourceLevel.getEntities().getAll()) {
         if (candidate instanceof LivingEntity living && living.isAlive() && isPulledBy(ownerId, living)) {
            toReturn.add(living);
         }
      }
      UUID primaryReturnedId = null;
      for (LivingEntity living : toReturn) {
         CompoundTag data = living.getPersistentData();
         boolean primaryTarget = data.getBoolean(UBW_TARGET_PRIMARY);
         ServerLevel returnLevel = resolveDimensionOrFallback(sourceLevel, data.getString(UBW_TARGET_RETURN_DIMENSION), fallbackReturnLevel);
         Vec3 returnPos = new Vec3(
            data.getDouble(UBW_TARGET_RETURN_X),
            data.getDouble(UBW_TARGET_RETURN_Y),
            data.getDouble(UBW_TARGET_RETURN_Z)
         );
         clearPulledTarget(living);
         Entity moved = living.changeDimension(new DimensionTransition(
            returnLevel,
            returnPos,
            Vec3.ZERO,
            living.getYRot(),
            living.getXRot(),
            DimensionTransition.DO_NOTHING
         ));
         if (moved instanceof LivingEntity movedLiving) {
            if (primaryTarget) {
               primaryReturnedId = movedLiving.getUUID();
            }
            clearPulledTarget(movedLiving);
         } else if (primaryTarget) {
            primaryReturnedId = living.getUUID();
         }
      }
      return primaryReturnedId;
   }

   private static void restoreReturnedCombatTargets(EmiyaArcherEntity archer, ServerLevel level, UUID ownerId, UUID lockedTargetId) {
      LivingEntity closest = findReturnedPrimaryTarget(archer, level, lockedTargetId);
      double closestDistance = Double.MAX_VALUE;
      if (closest == null) {
         AABB area = archer.getBoundingBox().inflate(32.0);
         for (LivingEntity living : level.getEntitiesOfClass(
            LivingEntity.class,
            area,
            e -> e != archer && e.isAlive() && !e.isAlliedTo(archer) && !EntityUtils.isImmunePlayerTarget(e)
         )) {
            double distance = living.distanceToSqr(archer);
            if (distance < closestDistance) {
               closest = living;
               closestDistance = distance;
            }
         }
      }
      if (closest != null) {
         forceCombatTarget(archer, closest, level);
         level.sendParticles(ParticleTypes.ANGRY_VILLAGER, archer.getX(), archer.getY() + archer.getBbHeight(), archer.getZ(), 3, 0.25, 0.25, 0.25, 0.0);
      }
   }

   private static LivingEntity findReturnedPrimaryTarget(EmiyaArcherEntity archer, ServerLevel level, UUID primaryReturnedId) {
      if (primaryReturnedId == null) {
         return null;
      }
      Entity entity = level.getEntity(primaryReturnedId);
      if (entity instanceof LivingEntity living && living.isAlive() && living != archer) {
         return living;
      }
      return null;
   }

   private static LivingEntity findLivingByUuid(ServerLevel level, UUID id) {
      if (id == null) {
         return null;
      }
      Entity entity = level.getEntity(id);
      return entity instanceof LivingEntity living ? living : null;
   }

   private static void scheduleUbwTargetRelock(EmiyaArcherEntity archer, UUID targetId, long now) {
      if (targetId == null) {
         return;
      }
      archer.getPersistentData().putUUID(UBW_RELOCK_TARGET, targetId);
      archer.getPersistentData().putLong(UBW_RELOCK_UNTIL, now + UBW_RELOCK_TICKS);
      archer.getPersistentData().putUUID(UBW_LOCKED_TARGET, targetId);
   }

   private static void tickUbwTargetRelock(EmiyaArcherEntity archer, ServerLevel level, long now) {
      CompoundTag data = archer.getPersistentData();
      if (!data.hasUUID(UBW_RELOCK_TARGET)) {
         return;
      }
      long until = data.getLong(UBW_RELOCK_UNTIL);
      if (until > 0L && now > until) {
         data.remove(UBW_RELOCK_TARGET);
         data.remove(UBW_RELOCK_UNTIL);
         return;
      }

      LivingEntity target = findLivingByUuid(level, data.getUUID(UBW_RELOCK_TARGET));
      if (target != null && target.isAlive() && target.level() == level) {
         forceCombatTarget(archer, target, level);
      }
   }

   private static void forceCombatTarget(EmiyaArcherEntity archer, LivingEntity target, ServerLevel level) {
      if (target == null || !target.isAlive() || target == archer) {
         return;
      }
      archer.setTarget(target);
      if (target instanceof Mob mob) {
         mob.setTarget(archer);
      }
      archer.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      archer.getPersistentData().putLong("TypeMoonCombatLastCombatTick", level.getGameTime());
      recordUbwLockedTarget(archer, target);
   }

   private static boolean isPulledBy(UUID ownerId, LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      return data.hasUUID(UBW_TARGET_OWNER) && ownerId.equals(data.getUUID(UBW_TARGET_OWNER));
   }

   private static void clearPulledTarget(LivingEntity living) {
      living.getPersistentData().remove(UBW_TARGET_OWNER);
      living.getPersistentData().remove(UBW_TARGET_RETURN_DIMENSION);
      living.getPersistentData().remove(UBW_TARGET_RETURN_X);
      living.getPersistentData().remove(UBW_TARGET_RETURN_Y);
      living.getPersistentData().remove(UBW_TARGET_RETURN_Z);
      living.getPersistentData().remove(UBW_TARGET_PRIMARY);
   }

   private static ServerLevel resolveDimensionOrOverworld(ServerLevel currentLevel, String dimensionId) {
      return resolveDimensionOrFallback(currentLevel, dimensionId, currentLevel.getServer().overworld());
   }

   private static ServerLevel resolveDimensionOrFallback(ServerLevel currentLevel, String dimensionId, ServerLevel fallback) {
      if (dimensionId != null && !dimensionId.isBlank()) {
         ResourceLocation location = ResourceLocation.tryParse(dimensionId);
         if (location != null) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
            ServerLevel level = currentLevel.getServer().getLevel(key);
            if (level != null) {
               return level;
            }
         }
      }
      return fallback;
   }

   private static void rainSwords(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target) {
      for (int i = 0; i < 12; i++) {
         double sx = target.getX() + (entity.getRandom().nextDouble() - 0.5) * 11.0;
         double sz = target.getZ() + (entity.getRandom().nextDouble() - 0.5) * 11.0;
         double sy = target.getY() + 10.0 + entity.getRandom().nextDouble() * 8.0;
         ItemStack stack = new ItemStack(Items.IRON_SWORD);
         UBWProjectileEntity sword = new UBWProjectileEntity(level, entity, stack);
         sword.setPos(sx, sy, sz);
         Vec3 dir = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0).subtract(sx, sy, sz).normalize();
         sword.setDeltaMovement(dir.scale(2.65 + entity.getRandom().nextDouble() * 0.25));
         level.addFreshEntity(sword);
      }
      level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 28, 3.2, 1.0, 3.2, 0.08);
   }

   private static void rainGreatSwords(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target) {
      for (int i = 0; i < 10; i++) {
         double angle = Math.PI * 2.0 * i / 10.0 + entity.getRandom().nextDouble() * 0.35;
         double radius = 4.0 + entity.getRandom().nextDouble() * 9.0;
         double sx = target.getX() + Math.cos(angle) * radius;
         double sz = target.getZ() + Math.sin(angle) * radius;
         double sy = target.getY() + 18.0 + entity.getRandom().nextDouble() * 10.0;
         ItemStack stack = new ItemStack(i % 3 == 0 ? Items.NETHERITE_SWORD : Items.DIAMOND_SWORD);
         UBWProjectileEntity sword = new UBWProjectileEntity(level, entity, stack);
         sword.setPos(sx, sy, sz);
         Vec3 aim = target.position().add((entity.getRandom().nextDouble() - 0.5) * 2.0, target.getBbHeight() * 0.5, (entity.getRandom().nextDouble() - 0.5) * 2.0);
         Vec3 dir = aim.subtract(sword.position()).normalize();
         sword.setDeltaMovement(dir.scale(3.15));
         level.addFreshEntity(sword);
      }
      level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + target.getBbHeight(), target.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 36, 2.8, 1.0, 2.8, 0.14);
      level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.75F, 0.62F);
   }

   private static void spawnUbwChantFallingSwords(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target) {
      Vec3 center = target != null && target.isAlive() ? target.position() : entity.position().add(entity.getLookAngle().scale(7.0));
      int count = target != null && target.isAlive() ? 6 : 3;
      for (int i = 0; i < count; i++) {
         double angle = level.random.nextDouble() * Math.PI * 2.0;
         double radius = 2.5 + level.random.nextDouble() * 10.0;
         double sx = center.x + Math.cos(angle) * radius;
         double sz = center.z + Math.sin(angle) * radius;
         double sy = center.y + 11.0 + level.random.nextDouble() * 7.0;
         ItemStack stack = new ItemStack(Items.IRON_SWORD);
         UBWProjectileEntity sword = new UBWProjectileEntity(level, entity, stack);
         sword.setStainUbwTerrainOnImpact(true);
         sword.setPos(sx, sy, sz);
         Vec3 aim = center.add((level.random.nextDouble() - 0.5) * 3.5, 0.0, (level.random.nextDouble() - 0.5) * 3.5);
         Vec3 dir = aim.subtract(sword.position()).normalize();
         sword.setDeltaMovement(dir.scale(2.35));
         level.addFreshEntity(sword);
      }
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.35, center.z, 12, 2.0, 0.25, 2.0, 0.04);
   }

   private static void liftUbwBlockSwords(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, int maxLifted) {
      BlockPos center = target.blockPosition();
      int lifted = 0;
      int radius = 15;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 6, radius))) {
         if (lifted >= maxLifted) {
            break;
         }
         BlockState state = level.getBlockState(pos);
         if (!state.is(ModBlocks.UBW_WEAPON_BLOCK.get())) {
            continue;
         }
         if (pos.distSqr(center) > radius * radius) {
            continue;
         }
         ItemStack stack = new ItemStack(Items.IRON_SWORD);
         if (level.getBlockEntity(pos) instanceof UBWWeaponBlockEntity tile && !tile.getStoredItem().isEmpty()) {
            stack = tile.getStoredItem().copy();
         }
         level.removeBlock(pos, false);
         Vec3 spawn = Vec3.atCenterOf(pos).add(0.0, 0.75 + entity.getRandom().nextDouble() * 1.2, 0.0);
         UBWProjectileEntity sword = new UBWProjectileEntity(level, entity, stack);
         sword.setPos(spawn.x, spawn.y, spawn.z);
         Vec3 aim = target.position().add(0.0, target.getBbHeight() * (0.35 + entity.getRandom().nextDouble() * 0.35), 0.0);
         Vec3 dir = aim.subtract(spawn).normalize();
         sword.setDeltaMovement(dir.scale(2.7 + entity.getRandom().nextDouble() * 0.55));
         level.addFreshEntity(sword);
         lifted++;
         level.sendParticles(ParticleTypes.CRIT, spawn.x, spawn.y, spawn.z, 5, 0.18, 0.22, 0.18, 0.06);
      }
      if (lifted > 0) {
         level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), lifted * 4, 2.8, 0.8, 2.8, 0.12);
         level.playSound(null, target.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE, 0.9F, 1.45F);
      }
   }

   private static void spawnUbwGears(EmiyaArcherEntity entity, ServerLevel level) {
      entity.getPersistentData().putBoolean(UBW_GEARS_SPAWNED, true);
      int gearCount = 9;
      for (int i = 0; i < gearCount; i++) {
         int variant = entity.getRandom().nextInt(3);
         float scale = 8.0F + entity.getRandom().nextFloat() * 8.0F;
         double angle = Math.PI * 2.0 * i / gearCount + (entity.getRandom().nextDouble() - 0.5) * 0.18;
         double distance = 50.0 + entity.getRandom().nextDouble() * 45.0;
         double x = entity.getX() + Math.cos(angle) * distance;
         double z = entity.getZ() + Math.sin(angle) * distance;
         double cloudY = Mth.clamp(158.0, level.getMinBuildHeight() + 42.0, level.getMaxBuildHeight() - 32.0);
         double y = cloudY + (entity.getRandom().nextDouble() - 0.5) * 10.0;
         UbwSkyGearEntity gear = new UbwSkyGearEntity(level, x, y, z, variant, scale, 20 * 22);
         level.addFreshEntity(gear);
      }
   }

   private static boolean shouldUseRangedMode(EmiyaArcherEntity entity, LivingEntity target, double distance, long now) {
      String currentMode = entity.getPersistentData().getString(COMBAT_MODE);
      long activeUntil = entity.getPersistentData().getLong(UBW_ACTIVE_UNTIL);
      if (activeUntil > now && distance > 4.5) {
         return true;
      }
      if (distance >= 8.5) {
         return true;
      }
      if (MODE_RANGED.equals(currentMode) && distance > 5.5) {
         return true;
      }
      if (entity.getHealth() < entity.getMaxHealth() * 0.35F && distance > 4.0) {
         return true;
      }
      return false;
   }

   private static void updateRangedStandoff(EmiyaArcherEntity entity, LivingEntity target, boolean rangedMode, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (!rangedMode || target == null || !target.isAlive() || distance < 12.0 || distance > 42.0 || !entity.getSensing().hasLineOfSight(target)) {
         data.remove(RANGED_STANDOFF_START_TICK);
         return;
      }

      boolean targetEngagedWithEmiya = target.getLastHurtByMob() == entity
         || entity.getLastHurtByMob() == target
         || target instanceof Mob mob && mob.getTarget() == entity;
      if (!targetEngagedWithEmiya && !looksLikeRangedOpponent(target)) {
         data.remove(RANGED_STANDOFF_START_TICK);
         return;
      }

      if (data.getLong(RANGED_STANDOFF_START_TICK) <= 0L) {
         data.putLong(RANGED_STANDOFF_START_TICK, now);
      }
   }

   private static boolean looksLikeRangedOpponent(LivingEntity target) {
      return target.getMainHandItem().is(Items.BOW)
         || target.getMainHandItem().is(Items.CROSSBOW)
         || target.getMainHandItem().is(Items.TRIDENT)
         || target.getOffhandItem().is(Items.BOW)
         || target.getOffhandItem().is(Items.CROSSBOW)
         || target.getOffhandItem().is(Items.TRIDENT);
   }

   private static boolean tryProbingRangedStandoffSpecial(
      EmiyaArcherEntity entity,
      ServerLevel level,
      LivingEntity target,
      long now,
      ServantCombatPhase phase,
      double distance
   ) {
      if (phase != ServantCombatPhase.PROBING) {
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      long standoffStart = data.getLong(RANGED_STANDOFF_START_TICK);
      if (standoffStart <= 0L || now - standoffStart < RANGED_STANDOFF_MIN_TICKS) {
         return false;
      }
      if (!canUse(now, data.getLong(LAST_PROBING_RANGED_SPECIAL_TICK), PROBING_RANGED_SPECIAL_COOLDOWN)) {
         return false;
      }
      if (entity.getPersistentData().getLong(UBW_ACTIVE_UNTIL) > now || entity.getCurrentMp() < 20.0) {
         return false;
      }
      int chance = distance >= 18.0 ? 9 : 5;
      if (target.getHealth() <= target.getMaxHealth() * 0.35F || target.getArmorValue() >= 12 || target.getMaxHealth() >= 90.0F) {
         chance += 4;
      }
      if (entity.getRandom().nextInt(100) >= chance) {
         return false;
      }

      boolean canSpiral = entity.getCurrentMp() >= 25.0
         && canUse(now, data.getLong(LAST_SPIRAL_TICK), phasedCooldown(SPIRAL_COOLDOWN, phase))
         && shouldUsePseudoSpiralSword(entity, target, now, phase);
      boolean canCrimson = canUse(now, data.getLong(LAST_CRIMSON_TICK), phasedCooldown(CRIMSON_COOLDOWN, phase))
         && shouldUseCrimsonHound(entity, target, now, phase);
      if (!canSpiral && !canCrimson) {
         return false;
      }

      data.putLong(LAST_PROBING_RANGED_SPECIAL_TICK, now);
      if (canSpiral && (!canCrimson || entity.getRandom().nextInt(100) < 42)) {
         castPseudoSpiralSword(entity, level, target, now);
      } else {
         castCrimsonHound(entity, level, target, now);
      }
      return true;
   }

   private static boolean shouldUseCrimsonHound(EmiyaArcherEntity entity, LivingEntity target, long now, ServantCombatPhase phase) {
      if (target == null || !target.isAlive()) {
         return false;
      }
      if (entity.getPersistentData().getLong(UBW_ACTIVE_UNTIL) > now) {
         return false;
      }
      if (target.getHealth() <= target.getMaxHealth() * 0.35F || target.getHealth() <= 35.0F) {
         return entity.getRandom().nextInt(100) < (phase == ServantCombatPhase.DECISIVE ? 40 : 26);
      }
      return entity.distanceTo(target) >= 14.0 && entity.getRandom().nextInt(100) < (phase == ServantCombatPhase.DECISIVE ? 16 : 9);
   }

   private static boolean shouldUsePseudoSpiralSword(EmiyaArcherEntity entity, LivingEntity target, long now, ServantCombatPhase phase) {
      if (target == null || !target.isAlive()) {
         return false;
      }
      if (entity.getPersistentData().getLong(UBW_ACTIVE_UNTIL) > now) {
         return false;
      }
      boolean sturdyTarget = target.getMaxHealth() >= 90.0F || target.getArmorValue() >= 12;
      boolean lowHealthFinisher = target.getHealth() <= target.getMaxHealth() * 0.28F;
      if (!sturdyTarget && !lowHealthFinisher) {
         return false;
      }
      return entity.getRandom().nextInt(100) < (phase == ServantCombatPhase.DECISIVE ? 24 : 12);
   }

   private static boolean tryClairvoyanceSpiralShot(EmiyaArcherEntity entity, ServerLevel level, long now) {
      if (!canUse(now, entity.getPersistentData().getLong(LAST_CLAIRVOYANCE_SPIRAL_TICK), CLAIRVOYANCE_SPIRAL_COOLDOWN)) {
         return false;
      }
      if (!canUse(now, entity.getPersistentData().getLong(LAST_SPIRAL_TICK), phasedCooldown(SPIRAL_COOLDOWN, ServantCombatPhase.DECISIVE))) {
         return false;
      }
      if (entity.getCurrentMp() < 30.0) {
         return false;
      }
      LivingEntity target = findClairvoyanceTarget(entity, level);
      if (target == null) {
         return false;
      }
      entity.getPersistentData().putLong(LAST_CLAIRVOYANCE_SPIRAL_TICK, now);
      entity.setTarget(target);
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      castPseudoSpiralSword(entity, level, target, now);
      level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight(), entity.getZ(), 36, 0.5, 0.5, 0.5, 0.08);
      level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.85, entity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.85F, 1.65F);
      return true;
   }

   private static LivingEntity findClairvoyanceTarget(EmiyaArcherEntity entity, ServerLevel level) {
      double baseRange = Math.max(48.0, entity.getAttributeValue(Attributes.FOLLOW_RANGE));
      double range = baseRange * 2.0;
      LivingEntity best = null;
      double bestScore = Double.MAX_VALUE;
      AABB area = entity.getBoundingBox().inflate(range);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         area,
         e -> e != entity && e.isAlive() && isClairvoyanceSniperTarget(entity, e)
      )) {
         double dist = entity.distanceToSqr(living);
         if (dist > range * range || dist <= baseRange * baseRange) {
            continue;
         }
         double score = dist;
         if (living instanceof ServantEntity) {
            score *= 0.45;
         }
         if (living.getLastHurtByMob() == entity) {
            score *= 0.6;
         }
         if (score < bestScore) {
            best = living;
            bestScore = score;
         }
      }
      return best;
   }

   private static boolean isClairvoyanceSniperTarget(EmiyaArcherEntity entity, LivingEntity target) {
      if (target.isAlliedTo(entity) || EntityUtils.isImmunePlayerTarget(target)) {
         return false;
      }
      if (target instanceof ServantEntity || target instanceof Monster || target instanceof ServerPlayer) {
         return true;
      }
      if (entity.getTarget() == target || target.getLastHurtByMob() == entity || entity.getLastHurtByMob() == target) {
         return true;
      }
      return target instanceof Mob mob && mob.getTarget() == entity;
   }

   private static void setCombatMode(EmiyaArcherEntity entity, String mode) {
      entity.getPersistentData().putString(COMBAT_MODE, mode);
   }

   private static void equipBow(EmiyaArcherEntity entity) {
      equipBow(entity, ItemStack.EMPTY);
   }

   private static void equipBow(EmiyaArcherEntity entity, ItemStack offhand) {
      entity.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.NAMELESS_BOW.get()));
      entity.setItemInHand(InteractionHand.OFF_HAND, offhand == null ? ItemStack.EMPTY : offhand);
      entity.getPersistentData().remove(PROJECTED_EXPIRES_TICK);
      entity.getPersistentData().remove(PROJECTED_PAIR);
   }

   private static boolean equipMeleeWeapon(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(ANALYZED_WEAPON_EXPIRES_TICK) > 0L && now >= data.getLong(ANALYZED_WEAPON_EXPIRES_TICK)) {
         data.remove(ANALYZED_WEAPON_EXPIRES_TICK);
         data.remove(ANALYZED_WEAPON_STACK);
         clearProjection(entity);
      }

      ItemStack analyzed = readAnalyzedWeapon(entity);
      if (!analyzed.isEmpty() && now < data.getLong(ANALYZED_WEAPON_EXPIRES_TICK)) {
         if (!ItemStack.matches(entity.getMainHandItem(), analyzed)) {
            entity.setItemInHand(InteractionHand.MAIN_HAND, analyzed.copy());
            entity.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            markProjectionExpiry(entity, data.getLong(ANALYZED_WEAPON_EXPIRES_TICK), false);
         }
         return false;
      }

      if (canUse(now, data.getLong(LAST_ANALYSIS_TICK), ANALYSIS_COOLDOWN)) {
         ItemStack enemyWeapon = findEnemyMeleeWeapon(target);
         if (!enemyWeapon.isEmpty()) {
            data.putLong(LAST_ANALYSIS_TICK, now);
            if (!shouldProjectEnemyWeapon(entity, now)) {
               return false;
            }
            ItemStack copy = enemyWeapon.copy();
            copy.setCount(1);
            if (isDivineOrSupremeWeapon(copy)) {
               return false;
            }
            addProjectedEnemyAttackPower(copy, target);
            data.putLong(ANALYZED_WEAPON_EXPIRES_TICK, now + 100L);
            data.put(ANALYZED_WEAPON_STACK, copy.save(entity.registryAccess()));
            entity.setItemInHand(InteractionHand.MAIN_HAND, copy);
            entity.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            markProjectionExpiry(entity, now + 100L, false);
            applyAnalyzedWeaponProjectionBuffs(entity, target);
            ServantVoiceHelper.tryPlayProjection(entity);
            entity.triggerNamedActionAnimation("projection");
            if (entity.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.7, entity.getZ(), 12, 0.25, 0.25, 0.25, 0.02);
            }
            return false;
         }
      }

         if (!isKanshouBakuyaPair(entity)) {
            projectTwinSwords(entity, false, now);
         }
         if (entity.getMainHandItem().is(ModItems.NAMELESS_BOW.get()) || !isMeleeWeapon(entity.getMainHandItem())) {
            applyProjectedTwinSwords(entity, false, now);
         }

      if (shouldUseBorrowedNoblePhantasm(entity, target, now)) {
         return tryBorrowEnemyNoblePhantasm(entity, level, target, now);
      }
      return false;
   }

   private static ItemStack readAnalyzedWeapon(EmiyaArcherEntity entity) {
      CompoundTag data = entity.getPersistentData();
      if (!data.contains(ANALYZED_WEAPON_STACK)) {
         return ItemStack.EMPTY;
      }
      return ItemStack.parse(entity.registryAccess(), data.getCompound(ANALYZED_WEAPON_STACK)).orElse(ItemStack.EMPTY);
   }

   private static boolean shouldProjectEnemyWeapon(EmiyaArcherEntity entity, long now) {
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      int chance = phase == ServantCombatPhase.DECISIVE ? 50 : 35;
      if (entity.getHealth() <= entity.getMaxHealth() * 0.35F) {
         chance += 10;
      }
      if (entity.getPersistentData().getLong(UBW_ACTIVE_UNTIL) > now) {
         chance += 10;
      }
      return entity.getRandom().nextInt(100) < Math.min(65, chance);
   }

   private static boolean shouldUseBorrowedNoblePhantasm(EmiyaArcherEntity entity, LivingEntity target, long now) {
      if (entity == null || target == null || !target.isAlive()) {
         return false;
      }
      if (ServantCombatSystem.getPhase(entity).id() < ServantCombatPhase.DECISIVE.id()) {
         return false;
      }
      if (entity.getPersistentData().getLong(UBW_ACTIVE_UNTIL) > now) {
         return false;
      }
      if (entity.getPersistentData().getLong(BORROWED_NP_USED_UNTIL) > now) {
         return false;
      }
      if (!canUse(now, entity.getPersistentData().getLong(LAST_PROJECTION_VOLLEY_TICK), PROJECTION_VOLLEY_COOLDOWN)) {
         return false;
      }
      return entity.getRandom().nextInt(100) < 6;
   }

   private static boolean tryBorrowEnemyNoblePhantasm(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (target instanceof EmiyaArcherEntity) {
         return false;
      }
      ItemStack borrowed = findBorrowableEnemyWeapon(target);
      if (borrowed.isEmpty()) {
         return false;
      }
      if (borrowed.is(ModItems.EXCALIBUR.get()) || borrowed.is(ModItems.EXCALIBUR_GALLATIN.get()) || borrowed.is(ModItems.TSUMUKARI_MURAMASA.get())) {
         if (entity.getHealth() <= 1.0F) {
            return false;
         }
      }
      if (!hasBorrowedNoblePhantasmAction(borrowed)) {
         return false;
      }
      markBorrowedNoblePhantasmUsed(entity, borrowed, target, now);
      if (borrowed.is(ModItems.EXCALIBUR.get())) {
         performBorrowedExcalibur(entity, level, target, now);
         return true;
      }
      if (borrowed.is(ModItems.EXCALIBUR_GALLATIN.get())) {
         performBorrowedGallatin(entity, level, target, now);
         return true;
      }
      if (borrowed.is(ModItems.TEMPLE_STONE_SWORD_AXE.get())) {
         performBorrowedNineLives(entity, level, target, now);
         return true;
      }
      if (borrowed.is(ModItems.BIZEN_NAGAMITSU.get())) {
         performBorrowedTsubame(entity, level, target, now);
         return true;
      }
      if (borrowed.is(ModItems.NAMELESS_CHAIN_DAGGER.get())) {
         performBorrowedMedusaDagger(entity, level, target, now);
         return true;
      }
      if (borrowed.is(ModItems.RULE_BREAKER.get())) {
         performBorrowedRuleBreaker(entity, level, target, now);
         return true;
      }
      if (borrowed.is(ModItems.GAE_BULG.get())) {
         performBorrowedGaeBulg(entity, level, target, now);
         return true;
      }
      return false;
   }

   private static boolean hasBorrowedNoblePhantasmAction(ItemStack stack) {
      return stack.is(ModItems.EXCALIBUR.get())
         || stack.is(ModItems.EXCALIBUR_GALLATIN.get())
         || stack.is(ModItems.TEMPLE_STONE_SWORD_AXE.get())
         || stack.is(ModItems.BIZEN_NAGAMITSU.get())
         || stack.is(ModItems.NAMELESS_CHAIN_DAGGER.get())
         || stack.is(ModItems.RULE_BREAKER.get())
         || stack.is(ModItems.GAE_BULG.get());
   }

   private static void markBorrowedNoblePhantasmUsed(EmiyaArcherEntity entity, ItemStack borrowed, LivingEntity sourceWielder, long now) {
      entity.getPersistentData().putLong(LAST_PROJECTION_VOLLEY_TICK, now);
      entity.getPersistentData().putLong(BORROWED_NP_USED_UNTIL, now + BORROWED_NP_PROJECTION_TICKS);
      ItemStack projected = borrowed.copy();
      projected.setCount(1);
      addProjectedEnemyAttackPower(projected, sourceWielder);
      entity.setItemInHand(InteractionHand.MAIN_HAND, projected);
      entity.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      markProjectionExpiry(entity, now + BORROWED_NP_PROJECTION_TICKS, false);
      applyAnalyzedWeaponProjectionBuffs(entity, sourceWielder);
   }

   private static ItemStack findBorrowableEnemyWeapon(LivingEntity target) {
      if (target == null) {
         return ItemStack.EMPTY;
      }
      ItemStack main = target.getMainHandItem();
      if (isBorrowableNoblePhantasm(main)) {
         return main;
      }
      ItemStack off = target.getOffhandItem();
      return isBorrowableNoblePhantasm(off) ? off : ItemStack.EMPTY;
   }

   private static boolean isBorrowableNoblePhantasm(ItemStack stack) {
      return !stack.isEmpty()
         && (stack.is(ModItems.EXCALIBUR.get())
            || stack.is(ModItems.EXCALIBUR_GALLATIN.get())
            || stack.is(ModItems.TSUMUKARI_MURAMASA.get())
            || stack.is(ModItems.TEMPLE_STONE_SWORD_AXE.get())
            || stack.is(ModItems.RULE_BREAKER.get())
            || stack.is(ModItems.NAMELESS_CHAIN_DAGGER.get())
            || stack.is(ModItems.GAE_BULG.get())
            || stack.is(ModItems.BIZEN_NAGAMITSU.get()));
   }

   private static boolean isDivineOrSupremeWeapon(ItemStack stack) {
      return MagicStructuralAnalysis.isProjectionBanned(stack);
   }

   private static void performBorrowedExcalibur(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 50.0));
      entity.triggerNamedActionAnimation("projection");
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.66, 0.0).add(ArtoriaPendragonCombatHelper.excaliburLook(entity).scale(1.2));
      ArtoriaExcaliburBeamEntity beam = new ArtoriaExcaliburBeamEntity(level, entity, start, 60);
      level.addFreshEntity(beam);
      if (entity.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 40, 0.55, 0.55, 0.55, 0.05);
      }
      TYPE_MOON_WORLD.queueServerWork(64, () -> {
         if (entity.isAlive() && entity.level() instanceof ServerLevel) {
            entity.invulnerableTime = 0;
            entity.hurt(entity.damageSources().magic(), entity.getMaxHealth() + 500.0F);
         }
      });
   }

   private static void performBorrowedGallatin(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 55.0));
      entity.triggerNamedActionAnimation("projection");
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      Vec3 look = horizontalDirectionTo(entity, target);
      VFXServerEffects.spawnReplayable(level, "servant_gawain_gallatin", entity, 3.0F);
      level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.68F);
      level.playSound(null, entity.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.25F, 0.82F);
      performBorrowedGallatinCone(entity, level, look, borrowedGallatinDamage(level, target));
      TYPE_MOON_WORLD.queueServerWork(64, () -> {
         if (entity.isAlive() && entity.level() instanceof ServerLevel) {
            entity.invulnerableTime = 0;
            entity.hurt(entity.damageSources().magic(), entity.getMaxHealth() + 500.0F);
         }
      });
   }

   private static void performBorrowedGallatinCone(EmiyaArcherEntity entity, ServerLevel level, Vec3 look, float damage) {
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      Set<Integer> hit = new HashSet<>();
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(BORROWED_GALLATIN_RANGE + 3.0),
         e -> e.isAlive() && e != entity && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.45, 0.0).subtract(origin);
         Vec3 horizontal = new Vec3(to.x, 0.0, to.z);
         double distance = horizontal.length();
         if (distance > BORROWED_GALLATIN_RANGE || distance < 0.2) {
            continue;
         }
         Vec3 dir = horizontal.normalize();
         if (dir.dot(look) < BORROWED_GALLATIN_HALF_ANGLE_COS || !hit.add(living.getId())) {
            continue;
         }
         applyBorrowedGallatinFixedDamage(entity, living, damage);
         living.igniteForSeconds(5.0F);
         living.push(look.x * 5.0, 0.32, look.z * 5.0);
         living.hurtMarked = true;
      }
      spawnBorrowedGallatinReleaseParticles(level, origin, look);
      breakBorrowedGallatinPath(level, origin, look);
   }

   private static void applyBorrowedGallatinFixedDamage(EmiyaArcherEntity entity, LivingEntity target, float damage) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }
      float before = target.getHealth();
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);
      target.invulnerableTime = 0;
      if (target.getPersistentData().getBoolean("GodHandActive")) {
         return;
      }
      float desired = Math.max(0.0F, before - damage);
      if (target.getHealth() > desired && target.getHealth() <= before) {
         target.setHealth(desired);
      }
   }

   private static void spawnBorrowedGallatinReleaseParticles(ServerLevel level, Vec3 origin, Vec3 look) {
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      for (double dist = 1.0; dist <= BORROWED_GALLATIN_RANGE; dist += 3.0) {
         double halfWidth = Math.min(20.0, dist * 0.7);
         Vec3 center = origin.add(look.scale(dist));
         level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 8, halfWidth * 0.3, 0.3, halfWidth * 0.3, 0.08);
         if (((int)dist) % 6 == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y - 0.2, center.z, 1, halfWidth * 0.16, 0.08, halfWidth * 0.16, 0.0);
         }
         if (((int)dist) % 9 == 0) {
            Vec3 edge = center.add(right.scale(level.random.nextBoolean() ? halfWidth : -halfWidth));
            level.sendParticles(ParticleTypes.FLAME, edge.x, edge.y, edge.z, 5, 0.2, 0.35, 0.2, 0.08);
         }
      }
   }

   private static void breakBorrowedGallatinPath(ServerLevel level, Vec3 origin, Vec3 look) {
      int broken = 0;
      int limit = 220;
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      for (double dist = 2.0; dist <= BORROWED_GALLATIN_RANGE && broken < limit; dist += 2.0) {
         double halfWidth = Math.min(20.0, dist * 0.7);
         for (double side = -halfWidth; side <= halfWidth && broken < limit; side += 2.0) {
            BlockPos center = BlockPos.containing(origin.add(look.scale(dist)).add(right.scale(side)));
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(0, -1, 0), center.offset(0, 2, 0))) {
               BlockState state = level.getBlockState(pos);
               float hardness = state.getDestroySpeed(level, pos);
               if (!state.isAir()
                  && hardness >= 0.0F
                  && hardness < 55.0F
                  && !state.is(Blocks.BEDROCK)
                  && state.getExplosionResistance(level, pos, null) < 1200.0F
                  && level.removeBlock(pos, false)) {
                  broken++;
                  if (broken >= limit) {
                     break;
                  }
               }
            }
         }
      }
   }

   private static boolean isUnderGallatinSun(ServerLevel level, BlockPos pos) {
      long dayTime = level.getDayTime() % 24000L;
      return level.dimensionType().hasSkyLight()
         && dayTime >= 0L && dayTime < 12000L
         && !level.isRaining()
         && !level.isThundering()
         && level.canSeeSky(pos.above());
   }

   private static float borrowedGallatinDamage(ServerLevel level, LivingEntity sourceWielder) {
      if (sourceWielder instanceof GawainEntity gawain && GawainCombatHelper.hasSunBlessing(gawain)) {
         return 3000.0F;
      }
      return isUnderGallatinSun(level, sourceWielder.blockPosition()) ? 3000.0F : 1000.0F;
   }

   private static void performBorrowedNineLives(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 28.0));
      entity.triggerNamedActionAnimation("projection");
      // Borrowed noble phantasms stay visually projected, but do not play the original owner's voice line.
      Vec3[] lastCenter = new Vec3[]{target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)};
      for (int i = 1; i <= 9; i++) {
         final int step = i;
         TYPE_MOON_WORLD.queueServerWork(step * 2, () -> {
            if (!entity.isAlive() || !(entity.level() instanceof ServerLevel sl)) {
               return;
            }
            Vec3 center = target.isAlive() ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0) : lastCenter[0];
            lastCenter[0] = center;
            Vec3 viewDir = target.position().subtract(entity.position()).normalize();
            if (viewDir.lengthSqr() < 1.0E-4) {
               viewDir = entity.getLookAngle();
            }
            Vec3 right = viewDir.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
            if (right.lengthSqr() < 1.0E-4) {
               right = new Vec3(1.0, 0.0, 0.0);
            }
            Vec3 planeUp = right.cross(viewDir).normalize();
            double angle = sl.random.nextDouble() * Math.PI * 2.0;
            double span = Math.max(target.getBbWidth(), target.getBbHeight()) * 1.3 + 3.0;
            Vec3 slashDir = right.scale(Math.cos(angle)).add(planeUp.scale(Math.sin(angle))).normalize();
            Vec3 start = center.add(slashDir.scale(-span));
            Vec3 end = center.add(slashDir.scale(span));
            RubyProjectileEntity slash = new RubyProjectileEntity(sl, start.x, start.y, start.z);
            slash.setItem(ItemStack.EMPTY);
            slash.setGemType(99);
            slash.setVisualScale(Math.max(0.5F, Math.max(target.getBbWidth(), target.getBbHeight()) / 1.8F));
            slash.setVisualEnd(end);
            slash.setNoGravity(true);
            slash.setDeltaMovement(Vec3.ZERO);
            sl.addFreshEntity(slash);
            if (target.isAlive()) {
               target.invulnerableTime = 0;
               target.hurt(entity.damageSources().mobAttack(entity), 84.0F);
               target.invulnerableTime = 0;
            }
            sl.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 8, 0.45, 0.45, 0.45, 0.35);
            sl.playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.9F, 1.35F);
         });
      }
   }

   private static void performBorrowedTsubame(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 20.0));
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 2, false, true, true));
      entity.triggerNamedActionAnimation("projection");
      entity.triggerSlashAnimation();
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), 300.0F);
      target.invulnerableTime = 0;
      Vec3 from = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
      Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 slashDir = to.subtract(from);
      if (slashDir.lengthSqr() < 1.0E-4) {
         slashDir = entity.getLookAngle();
      }
      slashDir = slashDir.normalize();
      Vec3 perp = new Vec3(-slashDir.z, 0.0, slashDir.x);
      for (int arc = 0; arc < 3; arc++) {
         double arcOffset = (arc - 1) * 0.4;
         for (double t = 0.0; t <= 1.0; t += 0.1) {
            Vec3 pos = from.lerp(to, t);
            double wave = Math.sin(t * Math.PI) * 0.3 * (arc + 1);
            pos = pos.add(perp.scale(wave + arcOffset));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 2, 0.0, 0.0, 0.0, 0.0);
         }
      }
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.3, target.getZ(), 25, 0.5, 0.6, 0.5, 0.08);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
      level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 12, 0.3, 0.4, 0.3, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.8F, 0.5F);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5F, 0.6F);
   }

   private static void performBorrowedMedusaDagger(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.triggerNamedActionAnimation("projection");
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.7, 0.0);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), 22.0F);
      target.invulnerableTime = 0;
      level.sendParticles(ParticleTypes.CRIT, origin.x, origin.y, origin.z, 18, 0.2, 0.2, 0.2, 0.05);
      Vec3 pull = origin.subtract(aim).normalize().scale(1.6);
      target.setDeltaMovement(pull.x, Math.max(0.15, pull.y + 0.1), pull.z);
      target.hurtMarked = true;
   }

   private static void performBorrowedRuleBreaker(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.triggerNamedActionAnimation("projection");
      MedeaCombatHelper.applyRuleBreakerHit(target, entity);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().magic(), 16.0F);
   }

   private static void performBorrowedGaeBulg(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.triggerNamedActionAnimation("projection");
      if (entity.distanceToSqr(target) > 64.0 && entity.getRandom().nextInt(100) < 38) {
         GaeBulgArmyProjectileEntity projectile = new GaeBulgArmyProjectileEntity(level, entity);
         projectile.setArmyDamage(500.0F);
         projectile.setTrackedTarget(target);
         projectile.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
         Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0);
         Vec3 dir = aim.subtract(projectile.position()).normalize();
         projectile.shoot(dir.x, dir.y + 0.08, dir.z, 2.25F, 0.0F);
         level.addFreshEntity(projectile);
         return;
      }
      GaeBulgProjectileEntity projectile = new GaeBulgProjectileEntity(level, entity);
      projectile.setMode(GaeBulgProjectileEntity.Mode.SINGLE);
      projectile.setTrackedTarget(target);
      projectile.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0);
      Vec3 dir = aim.subtract(projectile.position()).normalize();
      projectile.shoot(dir.x, dir.y + 0.12, dir.z, 2.6F, 0.0F);
      level.addFreshEntity(projectile);
   }

   private static ItemStack findEnemyMeleeWeapon(LivingEntity target) {
      if (target == null) {
         return ItemStack.EMPTY;
      }
      ItemStack main = target.getMainHandItem();
      if (isMeleeWeapon(main)) {
         return main;
      }
      ItemStack off = target.getOffhandItem();
      return isMeleeWeapon(off) ? off : ItemStack.EMPTY;
   }

   private static void addProjectedEnemyAttackPower(ItemStack projectedWeapon, LivingEntity target) {
      if (projectedWeapon.isEmpty() || target == null) {
         return;
      }
      double enemyAttack = Math.max(0.0, target.getAttributeValue(Attributes.ATTACK_DAMAGE));
      if (enemyAttack <= 0.0) {
         return;
      }

      ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
      ItemAttributeModifiers existing = projectedWeapon.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      for (ItemAttributeModifiers.Entry entry : existing.modifiers()) {
         builder.add(entry.attribute(), entry.modifier(), entry.slot());
      }
      builder.add(
         Attributes.ATTACK_DAMAGE,
         new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "emiya_projection_enemy_attack"),
            enemyAttack,
            Operation.ADD_VALUE
         ),
         EquipmentSlotGroup.MAINHAND
      );
      projectedWeapon.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
   }

   private static void applyAnalyzedWeaponProjectionBuffs(EmiyaArcherEntity entity, LivingEntity target) {
      int speedLevel = 1;
      int resistanceLevel = 1;
      int strengthLevel = 1;
      if (target instanceof ServantEntity servant && servant.getDefinition() != null) {
         ServantParams params = servant.getDefinition().parameters();
         if (params != null) {
            speedLevel = rankEffectLevel(params.agility(), params.agilityPlus());
            resistanceLevel = rankEffectLevel(params.endurance(), params.endurancePlus());
            strengthLevel = rankEffectLevel(params.strength(), params.strengthPlus());
            if (servant instanceof GawainEntity gawain && GawainCombatHelper.hasSunBlessing(gawain)) {
               speedLevel = amplifiedProjectionLevel(speedLevel, 3.0);
               resistanceLevel = amplifiedProjectionLevel(resistanceLevel, 3.0);
               strengthLevel = amplifiedProjectionLevel(strengthLevel, 3.0);
            }
         }
      }

      int duration = 5 * 20;
      entity.getPersistentData().putLong(ANALYZED_WEAPON_BUFF_UNTIL, entity.level().getGameTime() + duration);
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, speedLevel - 1, false, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resistanceLevel - 1, false, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, strengthLevel - 1, false, false, true));
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(), 18, 0.35, 0.45, 0.35, 0.08);
         level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 12, 0.25, 0.3, 0.25, 0.05);
      }
   }

   private static boolean hasAnalyzedWeaponProjectionBuff(EmiyaArcherEntity entity, long now) {
      return entity.getPersistentData().getLong(ANALYZED_WEAPON_BUFF_UNTIL) > now;
   }

   private static int rankEffectLevel(StatRank rank, boolean plus) {
      int level = switch (rank == null ? StatRank.E : rank) {
         case E -> 1;
         case D -> 2;
         case C -> 3;
         case B -> 4;
         case A -> 5;
      };
      return plus ? Math.min(5, level + 1) : level;
   }

   private static int amplifiedProjectionLevel(int baseLevel, double multiplier) {
      return Mth.clamp((int)Math.ceil(baseLevel * multiplier), 1, 5);
   }

   private static boolean isMeleeWeapon(ItemStack stack) {
      return !stack.isEmpty() && !stack.is(ModItems.NAMELESS_BOW.get()) && stack.getItem() instanceof SwordItem;
   }

   private static boolean isKanshouBakuyaPair(EmiyaArcherEntity entity) {
      return entity.getMainHandItem().is(ModItems.GAN_JIANG.get()) && entity.getOffhandItem().is(ModItems.MO_YE.get())
         || entity.getMainHandItem().is(ModItems.GAN_JIANG_OVEREDGE.get()) && entity.getOffhandItem().is(ModItems.MO_YE_OVEREDGE.get());
   }

   private static void projectTwinSwords(EmiyaArcherEntity entity, boolean overedge, long now) {
      ItemStack expectedMain = new ItemStack(overedge ? ModItems.GAN_JIANG_OVEREDGE.get() : ModItems.GAN_JIANG.get());
      ItemStack expectedOff = new ItemStack(overedge ? ModItems.MO_YE_OVEREDGE.get() : ModItems.MO_YE.get());
      long expiresAt = entity.getPersistentData().getLong(PROJECTED_EXPIRES_TICK);
      if (entity.getMainHandItem().is(expectedMain.getItem())
         && entity.getOffhandItem().is(expectedOff.getItem())
         && expiresAt > now + 20L) {
         return;
      }
      double cost = overedge ? OVEREDGE_PAIR_COST : TWIN_SWORD_COST;
      if (entity.getCurrentMp() < cost) {
         return;
      }
      entity.setCurrentMp(entity.getCurrentMp() - cost);
      applyProjectedTwinSwords(entity, overedge, now);
   }

   private static void applyProjectedTwinSwords(EmiyaArcherEntity entity, boolean overedge, long now) {
      ItemStack main = new ItemStack(overedge ? ModItems.GAN_JIANG_OVEREDGE.get() : ModItems.GAN_JIANG.get());
      ItemStack off = new ItemStack(overedge ? ModItems.MO_YE_OVEREDGE.get() : ModItems.MO_YE.get());
      entity.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, main);
      entity.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, off);
      markProjectionExpiry(entity, now + 100L, true);
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 0, false, false, true));
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.75, entity.getZ(), 12, 0.2, 0.25, 0.2, 0.02);
      }
   }

   private static void castCrimsonHound(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_CRIMSON_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 20.0);
      ServantVoiceHelper.tryPlayProjection(entity);
      equipBow(entity, new ItemStack(ModItems.CRIMSON_HOUND.get()));
      entity.triggerNamedActionAnimation("bow_shot");
      CrimsonHoundProjectileEntity projectile = new CrimsonHoundProjectileEntity(level, entity);
      projectile.setNoGravity(true);
      projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.72, entity.getZ());
      projectile.setTrackedTarget(target);
      Vec3 dir = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0).subtract(projectile.position()).normalize();
      projectile.setDeltaMovement(dir.scale(2.65));
      level.addFreshEntity(projectile);
      spawnProjectionCastFx(level, entity, target, ParticleTypes.FLAME, 40);
      level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 0.7F);
   }

   private static void castPseudoSpiralSword(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_SPIRAL_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 25.0);
      ServantVoiceHelper.tryPlayEmiyaSpiral(entity);
      equipBow(entity, new ItemStack(ModItems.PSEUDO_SPIRAL_SWORD.get()));
      entity.triggerNamedActionAnimation("bow_shot");
      PseudoSpiralSwordProjectileEntity projectile = new PseudoSpiralSwordProjectileEntity(level, entity);
      projectile.setNoGravity(true);
      projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.72, entity.getZ());
      projectile.setTrackedTarget(target);
      Vec3 dir = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0).subtract(projectile.position()).normalize();
      projectile.setDeltaMovement(dir.scale(3.15));
      level.addFreshEntity(projectile);
      spawnProjectionCastFx(level, entity, target, ParticleTypes.END_ROD, 56);
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 1.0F, 1.35F);
   }

   private static void castProjectionVolley(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_PROJECTION_VOLLEY_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 12.0);
      entity.triggerNamedActionAnimation("projection");
      ServantVoiceHelper.tryPlayProjection(entity);
      for (int i = 0; i < 3; i++) {
         final int step = i;
         TYPE_MOON_WORLD.queueServerWork(step * 3 + 1, () -> {
            if (!entity.isAlive() || !target.isAlive() || !(entity.level() instanceof ServerLevel sl)) {
               return;
            }
            ItemStack stack = new ItemStack(Items.IRON_SWORD);
            EmiyaArrowOrbProjectileEntity orb = new EmiyaArrowOrbProjectileEntity(sl, entity, stack);
            orb.setDirectDamage(20.0F);
            Vec3 side = sideVector(entity, target).scale((step - 1) * 1.4);
            Vec3 spawn = entity.position().add(0.0, entity.getBbHeight() * 0.78, 0.0).add(side);
            orb.setPos(spawn.x, spawn.y, spawn.z);
            Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
            orb.setDeltaMovement(straightShotVelocity(orb.position(), aim, 2.45));
            sl.addFreshEntity(orb);
            sl.sendParticles(ParticleTypes.ENCHANT, spawn.x, spawn.y, spawn.z, 8, 0.1, 0.1, 0.1, 0.02);
         });
      }
   }

   private static void castFocusedSwordBarrel(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      entity.getPersistentData().putLong(LAST_FOCUSED_SWORD_BARREL_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 18.0);
      entity.triggerNamedActionAnimation("projection");
      ServantVoiceHelper.tryPlayProjection(entity);
      int count = phase == ServantCombatPhase.DECISIVE ? 16 : 11;
      Vec3 horizontalLook = horizontalDirectionTo(entity, target);
      Vec3 right = sideVector(entity, target);
      Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.82 + 2.2, 0.0).add(horizontalLook.scale(-2.0));
      double lineWidth = 14.0;
      double spacing = count <= 1 ? 0.0 : lineWidth / (count - 1);
      double startOffset = -lineWidth / 2.0;
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);

      for (int i = 0; i < count; i++) {
         double localRight = startOffset + spacing * i + (entity.getRandom().nextDouble() - 0.5) * 0.35;
         double localUp = (entity.getRandom().nextDouble() - 0.5) * 2.4;
         Vec3 spawn = center.add(right.scale(localRight)).add(0.0, localUp, 0.0);
         SwordBarrelProjectileEntity projectile = new SwordBarrelProjectileEntity(level, entity, randomProjectedWeapon(entity, false));
         projectile.setPos(spawn.x, spawn.y, spawn.z);
         projectile.setTargetEntity(target.getId());
         projectile.setHover(12 + entity.getRandom().nextInt(8), aim);
         projectile.setMode2Tracking(true);
         projectile.setHoverOffset(new Vec3(localRight, localUp + 2.2, -2.0));
         projectile.setSpawnPhase(8);
         Vec3 dir = aim.subtract(spawn).normalize();
         projectile.setXRot((float)Math.toDegrees(Math.asin(-dir.y)));
         projectile.setYRot((float)Math.toDegrees(Math.atan2(-dir.x, dir.z)));
         level.addFreshEntity(projectile);
         level.sendParticles(ParticleTypes.ENCHANT, spawn.x, spawn.y, spawn.z, 4, 0.12, 0.12, 0.12, 0.04);
      }

      level.playSound(null, entity.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 0.65F, 1.55F);
   }

   private static void castCrossBladePincer(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      entity.getPersistentData().putLong(LAST_CROSS_BLADE_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 10.0);
      entity.triggerNamedActionAnimation("projection");
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 side = sideVector(entity, target);
      Vec3 forward = horizontalDirectionTo(entity, target);
      int count = phase == ServantCombatPhase.DECISIVE ? 6 : 4;
      for (int i = 0; i < count; i++) {
         double sideSign = (i & 1) == 0 ? 1.0 : -1.0;
         double height = 1.1 + (i / 2) * 0.55;
         Vec3 spawn = center.add(side.scale(sideSign * (5.2 + i * 0.28))).add(forward.scale(-2.0 + i * 0.35)).add(0.0, height, 0.0);
         EmiyaThrownWeaponEntity blade = new EmiyaThrownWeaponEntity(level, entity, randomProjectedWeapon(entity, i >= count - 2 && phase == ServantCombatPhase.DECISIVE));
         blade.setPos(spawn.x, spawn.y, spawn.z);
         blade.setFixedDamage(18.0F + i * 2.0F);
         blade.setBreakLowHardnessBlocks(true);
         blade.setNoGravity(true);
         blade.setDeltaMovement(straightShotVelocity(spawn, center, 2.45 + i * 0.1));
         level.addFreshEntity(blade);
         level.sendParticles(ParticleTypes.CRIT, spawn.x, spawn.y, spawn.z, 5, 0.12, 0.12, 0.12, 0.04);
      }

      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.75F, 1.25F);
   }

   private static void castOrbBurst(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_ORB_BURST_TICK, now);
      entity.triggerNamedActionAnimation("bow_shot");
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.46, 0.0);
      Vec3 side = sideVector(entity, target);
      for (int i = 0; i < 3; i++) {
         Vec3 spawn = entity.position().add(0.0, entity.getBbHeight() * 0.72 + i * 0.18, 0.0).add(side.scale((i - 1) * 0.45));
         EmiyaArrowOrbProjectileEntity orb = new EmiyaArrowOrbProjectileEntity(level, entity, new ItemStack(Items.ARROW));
         orb.setPos(spawn.x, spawn.y, spawn.z);
         orb.setDirectDamage(12.0F + i * 2.0F);
         orb.setDeltaMovement(straightShotVelocity(spawn, aim, 2.75 + i * 0.18));
         level.addFreshEntity(orb);
         level.sendParticles(ParticleTypes.END_ROD, spawn.x, spawn.y, spawn.z, 6, 0.08, 0.08, 0.08, 0.04);
      }

      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.55F, 1.8F);
   }

   private static ItemStack randomProjectedWeapon(EmiyaArcherEntity entity, boolean overedge) {
      int pick = entity.getRandom().nextInt(overedge ? 5 : 6);
      return switch (pick) {
         case 0 -> new ItemStack(overedge ? ModItems.GAN_JIANG_OVEREDGE.get() : ModItems.GAN_JIANG.get());
         case 1 -> new ItemStack(overedge ? ModItems.MO_YE_OVEREDGE.get() : ModItems.MO_YE.get());
         case 2 -> new ItemStack(ModItems.PSEUDO_SPIRAL_SWORD.get());
         default -> new ItemStack(Items.IRON_SWORD);
      };
   }

   private static void shootIronSword(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_IRON_SWORD_SHOT_TICK, now);
      entity.triggerNamedActionAnimation("bow_shot");
      ItemStack stack = new ItemStack(Items.ARROW);
      EmiyaArrowOrbProjectileEntity orb = new EmiyaArrowOrbProjectileEntity(level, entity, stack);
      orb.setDirectDamage(18.0F + entity.getRandom().nextFloat() * 4.0F);
      Vec3 side = sideVector(entity, target).scale(entity.getRandom().nextBoolean() ? 0.45 : -0.45);
      Vec3 spawn = entity.position().add(0.0, entity.getBbHeight() * 0.72, 0.0).add(side);
      orb.setPos(spawn.x, spawn.y, spawn.z);
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      orb.setDeltaMovement(straightShotVelocity(orb.position(), aim, 2.35));
      level.addFreshEntity(orb);
      level.sendParticles(ParticleTypes.ENCHANT, spawn.x, spawn.y, spawn.z, 8, 0.1, 0.1, 0.1, 0.02);
      level.sendParticles(ParticleTypes.END_ROD, spawn.x, spawn.y, spawn.z, 5, 0.08, 0.08, 0.08, 0.04);
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.55F, 1.45F);
   }

   private static Vec3 straightShotVelocity(Vec3 start, Vec3 target, double speed) {
      Vec3 direction = target.subtract(start);
      if (direction.lengthSqr() < 1.0E-4) {
         return new Vec3(0.0, 0.0, speed);
      }

      return direction.normalize().scale(speed);
   }

   private static Vec3 horizontalDirectionTo(EmiyaArcherEntity entity, LivingEntity target) {
      Vec3 forward = target.position().subtract(entity.position());
      if (forward.horizontalDistanceSqr() >= 1.0E-4) {
         return new Vec3(forward.x, 0.0, forward.z).normalize();
      }
      Vec3 look = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (look.lengthSqr() >= 1.0E-4) {
         return look.normalize();
      }
      return new Vec3(0.0, 0.0, 1.0);
   }

   private static void spawnProjectionCastFx(ServerLevel level, EmiyaArcherEntity entity, LivingEntity target, net.minecraft.core.particles.ParticleOptions particle, int count) {
      VFXServerEffects.spawn(level, "servant_emiya_projection", entity, 96.0);
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.72, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      Vec3 delta = end.subtract(start);
      level.sendParticles(ParticleTypes.ENCHANT, start.x, start.y, start.z, count / 2, 0.32, 0.28, 0.32, 0.08);
      level.sendParticles(particle, start.x, start.y, start.z, count, 0.38, 0.28, 0.38, 0.1);
      int steps = 8;
      for (int i = 1; i <= steps; i++) {
         double t = i / (double)steps;
         Vec3 p = start.add(delta.scale(t));
         level.sendParticles(particle, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.02);
      }
   }

   private static Vec3 arcingSwordVelocity(Vec3 spawn, Vec3 aim, double horizontalSpeed, double arcBonus) {
      Vec3 delta = aim.subtract(spawn);
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return new Vec3(0.0, 0.35 + arcBonus, 0.0);
      }
      double horizontalDistance = horizontal.length();
      double ticks = Mth.clamp(horizontalDistance / Math.max(0.1, horizontalSpeed), 4.0, 22.0);
      double gravity = 0.03;
      double yVelocity = delta.y / ticks + gravity * ticks * 0.5 + arcBonus;
      yVelocity = Mth.clamp(yVelocity, -0.05, 0.85);
      Vec3 flat = horizontal.normalize().scale(horizontalSpeed);
      return new Vec3(flat.x, yVelocity, flat.z);
   }

   private static void performMindEyeStep(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_MIND_EYE_STEP_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 8.0);
      entity.triggerNamedActionAnimation("mind_eye_step");
      Vec3 away = entity.position().subtract(target.position());
      if (away.horizontalDistanceSqr() < 1.0E-4) {
         away = entity.getLookAngle().scale(-1.0);
      }
      Vec3 side = sideVector(entity, target).scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);
      Vec3 dir = new Vec3(away.x, 0.0, away.z).normalize().scale(0.75).add(side.scale(0.85)).normalize();
      Vec3 destination = entity.position().add(dir.scale(4.0));
      entity.teleportTo(destination.x, destination.y, destination.z);
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 35, 0, false, false, true));
      entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 1, false, false, true));
      level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 18, 0.35, 0.45, 0.35, 0.06);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.HOSTILE, 0.8F, 1.35F);
   }

   private static void performTwinUppercut(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_TWIN_UPPERCUT_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 6.0);
      applyProjectedTwinSwords(entity, false, now);
      entity.triggerUppercutAnimation();
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      AABB hitBox = entity.getBoundingBox().inflate(2.2).expandTowards(entity.getLookAngle().scale(1.3));
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().mobAttack(entity), 23.0F);
         living.invulnerableTime = 0;
         Vec3 push = horizontalDirection(entity, living).scale(0.28);
         living.setDeltaMovement(living.getDeltaMovement().x + push.x, 0.92, living.getDeltaMovement().z + push.z);
         living.hurtMarked = true;
      }
      Vec3 fx = entity.position().add(entity.getLookAngle().scale(1.1)).add(0.0, entity.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CRIT, fx.x, fx.y + 0.25, fx.z, 12, 0.25, 0.35, 0.25, 0.08);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.85F, 1.25F);
   }

   private static void performTwinRepel(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_TWIN_REPEL_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 6.0);
      applyProjectedTwinSwords(entity, false, now);
      entity.triggerHorizontalSwingAnimation();
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
      AABB hitBox = entity.getBoundingBox().inflate(3.2).expandTowards(entity.getLookAngle().scale(1.2));
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().mobAttack(entity), 19.0F);
         living.invulnerableTime = 0;
         Vec3 push = horizontalDirection(entity, living);
         living.push(push.x * 1.45, 0.24, push.z * 1.45);
         living.hurtMarked = true;
      }
      Vec3 fx = entity.position().add(entity.getLookAngle().scale(1.35)).add(0.0, entity.getBbHeight() * 0.5, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, fx.x, fx.y - 0.25, fx.z, 14, 0.45, 0.14, 0.45, 0.05);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.95F, 0.92F);
   }

   private static void performAerialPursuit(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_AERIAL_PURSUIT_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 10.0);
      applyProjectedTwinSwords(entity, false, now);
      entity.triggerUppercutAnimation();
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), 18.0F);
      target.invulnerableTime = 0;
      Vec3 launch = horizontalDirection(entity, target);
      target.setDeltaMovement(target.getDeltaMovement().x + launch.x * 0.22, 1.05, target.getDeltaMovement().z + launch.z * 0.22);
      target.hurtMarked = true;
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 18, 0.28, 0.45, 0.28, 0.1);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 1.05F);

      TYPE_MOON_WORLD.queueServerWork(7, () -> {
         if (!entity.isAlive() || !target.isAlive() || !(entity.level() instanceof ServerLevel sl) || entity.level() != target.level()) {
            return;
         }
         Vec3 dir = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
         Vec3 horizontal = dir.lengthSqr() < 1.0E-4 ? entity.getLookAngle().multiply(1.0, 0.0, 1.0) : dir.normalize();
         if (horizontal.lengthSqr() > 1.0E-4) {
            Vec3 destination = target.position().subtract(horizontal.normalize().scale(1.25));
            Vec3 move = destination.subtract(entity.position());
            if (sl.noCollision(entity, entity.getBoundingBox().move(move))) {
               sl.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 12, 0.22, 0.28, 0.22, 0.05);
               entity.teleportTo(destination.x, target.getY(), destination.z);
               entity.fallDistance = 0.0F;
            }
         }
         entity.triggerSlashAnimation();
         entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().mobAttack(entity), 28.0F);
         target.invulnerableTime = 0;
         Vec3 push = horizontalDirection(entity, target);
         target.push(push.x * 0.95, -0.12, push.z * 0.95);
         target.hurtMarked = true;
         AABB shock = entity.getBoundingBox().inflate(2.8, 1.4, 2.8);
         for (LivingEntity living : sl.getEntitiesOfClass(
            LivingEntity.class,
            shock,
            e -> e != entity && e != target && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
         )) {
            Vec3 sidePush = horizontalDirection(entity, living);
            living.invulnerableTime = 0;
            living.hurt(entity.damageSources().mobAttack(entity), 12.0F);
            living.invulnerableTime = 0;
            living.push(sidePush.x * 0.8, 0.25, sidePush.z * 0.8);
            living.hurtMarked = true;
         }
         Vec3 fx = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, 3, 0.0, 0.0, 0.0, 0.0);
         sl.sendParticles(ParticleTypes.CRIT, fx.x, fx.y, fx.z, 24, 0.55, 0.35, 0.55, 0.12);
         sl.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.22F);
      });
   }

   private static void performChasingThrust(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_CHASING_THRUST_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 7.0);
      applyProjectedTwinSwords(entity, false, now);
      entity.triggerSlashAnimation();
      Vec3 dir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return;
      }
      horizontal = horizontal.normalize();
      Vec3 destination = target.position().subtract(horizontal.scale(1.35));
      Vec3 move = destination.subtract(entity.position());
      if (level.noCollision(entity, entity.getBoundingBox().move(move))) {
         level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 14, 0.25, 0.3, 0.25, 0.05);
         entity.teleportTo(destination.x, target.getY(), destination.z);
         entity.fallDistance = 0.0F;
      } else {
         entity.setDeltaMovement(horizontal.x * 1.45, Math.max(entity.getDeltaMovement().y, 0.12), horizontal.z * 1.45);
         entity.hasImpulse = true;
      }
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), 25.0F);
      target.invulnerableTime = 0;
      target.push(horizontal.x * 0.85, 0.3, horizontal.z * 0.85);
      target.hurtMarked = true;
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 12, 0.25, 0.25, 0.25, 0.12);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.9F, 1.35F);
   }

   private static void performProjectionImpact(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_PROJECTION_IMPACT_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 10.0);
      entity.triggerNamedActionAnimation("projection");
      ServantVoiceHelper.tryPlayProjection(entity);
      Vec3 center = target.position();
      for (int i = 0; i < 5; i++) {
         double angle = Math.PI * 2.0 * i / 5.0;
         double radius = i == 0 ? 0.0 : 1.7;
         Vec3 spawn = center.add(Math.cos(angle) * radius, 4.8 + i * 0.18, Math.sin(angle) * radius);
         UBWProjectileEntity sword = new UBWProjectileEntity(level, entity, new ItemStack(Items.IRON_SWORD));
         sword.setPos(spawn.x, spawn.y, spawn.z);
         sword.setDeltaMovement(0.0, -2.35, 0.0);
         level.addFreshEntity(sword);
      }
      TYPE_MOON_WORLD.queueServerWork(6, () -> {
         if (!entity.isAlive() || !(entity.level() instanceof ServerLevel sl)) {
            return;
         }
         Vec3 impact = target.isAlive() ? target.position() : center;
         resolveProjectionImpact(entity, sl, impact);
      });
   }

   private static void performReinforcedProjectionSlam(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_REINFORCED_SLAM_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 12.0);
      entity.triggerGroundSlam();
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
      Vec3 forward = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
      Vec3 center = entity.position().add(forward.scale(1.75));
      double radius = 5.2;
      AABB box = new AABB(center, center).inflate(radius, 2.0, radius);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         box,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         double dist = Math.sqrt(living.distanceToSqr(center.x, center.y, center.z));
         if (dist > radius) {
            continue;
         }
         float damage = (float)Math.max(16.0, 34.0 * (1.0 - dist / (radius * 1.35)));
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().mobAttack(entity), damage);
         living.invulnerableTime = 0;
         Vec3 push = living.position().subtract(center).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() < 1.0E-4) {
            push = forward;
         }
         push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : push.normalize();
         double power = Math.max(0.7, 1.7 * (1.0 - dist / (radius * 1.15)));
         living.push(push.x * power, 0.55, push.z * power);
         living.hurtMarked = true;
      }
      breakReinforcedSlamBlocks(level, center);
      level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.12, center.z, 5, 1.0, 0.18, 1.0, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.12, center.z, 44, 1.75, 0.35, 1.75, 0.08);
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.55, center.z, 34, 1.2, 0.45, 1.2, 0.16);
      level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.75, entity.getZ(), 16, 0.3, 0.35, 0.3, 0.08);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.95F, 0.72F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.75F, 1.1F);
   }

   private static void performOveredgeCleave(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_OVEREDGE_CLEAVE_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 14.0);
      applyProjectedTwinSwords(entity, true, now);
      entity.triggerSweepAnimation();
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.48, 0.0));
      AABB hitBox = entity.getBoundingBox().inflate(3.1, 1.4, 3.1).expandTowards(entity.getLookAngle().scale(2.0));
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().mobAttack(entity), 24.0F);
         living.invulnerableTime = 0;
         Vec3 push = horizontalDirection(entity, living);
         living.push(push.x * 1.25, 0.28, push.z * 1.25);
         living.hurtMarked = true;
      }
      breakOveredgeCleaveBlocks(entity, level);
      Vec3 fx = entity.position().add(entity.getLookAngle().scale(1.8)).add(0.0, entity.getBbHeight() * 0.55, 0.0);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, fx.x, fx.y, fx.z, 4, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CRIT, fx.x, fx.y, fx.z, 22, 0.7, 0.35, 0.7, 0.1);
      level.sendParticles(ParticleTypes.CLOUD, fx.x, fx.y - 0.45, fx.z, 18, 0.9, 0.18, 0.9, 0.06);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.05F, 0.75F);
      level.playSound(null, entity.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.45F, 1.45F);
   }

   private static void performBladeRupture(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_BLADE_RUPTURE_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 16.0);
      applyProjectedTwinSwords(entity, false, now);
      entity.triggerGroundSlam();
      entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.45, 0.0));
      Vec3 center = entity.position().add(entity.getLookAngle().multiply(1.7, 0.0, 1.7));
      AABB box = new AABB(center, center).inflate(3.6, 1.5, 3.6);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         box,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         double dist = Math.sqrt(living.distanceToSqr(center.x, center.y, center.z));
         if (dist > 3.8) {
            continue;
         }
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), 20.0F);
         living.invulnerableTime = 0;
         Vec3 push = living.position().subtract(center).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() < 1.0E-4) {
            push = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
         }
         push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : push.normalize();
         living.push(push.x * 1.05, 0.42, push.z * 1.05);
         living.hurtMarked = true;
      }
      breakBladeRuptureBlocks(level, center);
      level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.1, center.z, 3, 0.65, 0.12, 0.65, 0.0);
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.45, center.z, 28, 1.3, 0.5, 1.3, 0.12);
      level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.1, center.z, 34, 1.25, 0.28, 1.25, 0.08);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.7F, 1.2F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.HOSTILE, 0.65F, 1.35F);
   }

   private static void resolveProjectionImpact(EmiyaArcherEntity entity, ServerLevel level, Vec3 center) {
      AABB box = new AABB(center, center).inflate(3.0, 1.6, 3.0);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         box,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         double dist = Math.sqrt(living.distanceToSqr(center.x, center.y, center.z));
         if (dist > 3.25) {
            continue;
         }
         living.invulnerableTime = 0;
         living.hurt(entity.damageSources().magic(), 17.0F);
         living.invulnerableTime = 0;
         Vec3 push = living.position().subtract(center).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() < 1.0E-4) {
            push = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
         }
         push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : push.normalize();
         living.push(push.x * 0.9, 0.35, push.z * 0.9);
         living.hurtMarked = true;
      }
      breakProjectionImpactBlocks(level, center);
      level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.15, center.z, 4, 0.75, 0.18, 0.75, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 0.1, center.z, 28, 1.0, 0.25, 1.0, 0.06);
      level.sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.6, center.z, 18, 1.0, 0.45, 1.0, 0.08);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8F, 1.28F);
   }

   private static void breakProjectionImpactBlocks(ServerLevel level, Vec3 center) {
      BlockPos origin = BlockPos.containing(center);
      int broken = 0;
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 1, 2))) {
         if (broken >= 18) {
            return;
         }
         double distSqr = pos.distToCenterSqr(center.x, center.y, center.z);
         if (distSqr > 7.0) {
            continue;
         }
         BlockState state = level.getBlockState(pos);
         float hardness = state.getDestroySpeed(level, pos);
         if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F || hardness > 12.0F) {
            continue;
         }
         level.removeBlock(pos, false);
         broken++;
         if ((broken & 1) == 0) {
            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.18, 0.18, 0.18, 0.02);
         }
      }
   }

   private static void breakReinforcedSlamBlocks(ServerLevel level, Vec3 center) {
      BlockPos origin = BlockPos.containing(center);
      int broken = 0;
      int radius = 4;
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -1, -radius), origin.offset(radius, 2, radius))) {
         if (broken >= 44) {
            return;
         }
         double distSqr = pos.distToCenterSqr(center.x, center.y, center.z);
         if (distSqr > 16.5) {
            continue;
         }
         if (destroyMeleeBreakableBlock(level, pos, null, 12.0F)) {
            broken++;
            if ((broken & 1) == 0) {
               level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.22, 0.18, 0.22, 0.04);
            }
         }
      }
   }

   private static void breakOveredgeCleaveBlocks(EmiyaArcherEntity entity, ServerLevel level) {
      Vec3 forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-4) {
         return;
      }
      forward = forward.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      int broken = 0;
      BlockPos origin = entity.blockPosition();
      for (double depth = 0.8; depth <= 4.0; depth += 0.75) {
         int halfWidth = depth > 2.2 ? 2 : 1;
         for (int width = -halfWidth; width <= halfWidth; width++) {
            for (int dy = 0; dy <= 2; dy++) {
               if (broken >= 26) {
                  return;
               }
               Vec3 sample = entity.position().add(forward.scale(depth)).add(side.scale(width * 0.75)).add(0.0, dy - 0.2, 0.0);
               BlockPos pos = BlockPos.containing(sample);
               if (destroyMeleeBreakableBlock(level, pos, entity, 11.0F)) {
                  broken++;
               }
            }
         }
      }
      for (int i = 0; i < Math.min(8, broken); i++) {
         Vec3 sample = entity.position().add(forward.scale(1.0 + i * 0.35)).add(0.0, 0.25, 0.0);
         level.sendParticles(ParticleTypes.CLOUD, sample.x, sample.y, sample.z, 2, 0.18, 0.12, 0.18, 0.04);
      }
   }

   private static void breakBladeRuptureBlocks(ServerLevel level, Vec3 center) {
      BlockPos origin = BlockPos.containing(center);
      int broken = 0;
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -1, -3), origin.offset(3, 1, 3))) {
         if (broken >= 32) {
            return;
         }
         double distSqr = pos.distToCenterSqr(center.x, center.y, center.z);
         if (distSqr > 10.5) {
            continue;
         }
         if (destroyMeleeBreakableBlock(level, pos, null, 9.0F)) {
            broken++;
            if ((broken & 1) == 0) {
               level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.18, 0.16, 0.18, 0.04);
            }
         }
      }
   }

   private static boolean destroyMeleeBreakableBlock(ServerLevel level, BlockPos pos, Entity breaker, float maxHardness) {
      BlockState state = level.getBlockState(pos);
      float hardness = state.getDestroySpeed(level, pos);
      if (state.isAir()
         || state.hasBlockEntity()
         || state.is(Blocks.BEDROCK)
         || hardness < 0.0F
         || hardness > maxHardness) {
         return false;
      }
      return level.destroyBlock(pos, false, breaker);
   }

   private static int nearbyEnemyCount(EmiyaArcherEntity entity, ServerLevel level, double radius) {
      return level.getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(radius),
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      ).size();
   }

   private static Vec3 horizontalDirection(LivingEntity from, LivingEntity to) {
      Vec3 dir = to.position().subtract(from.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = from.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      return dir.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
   }

   private static void performTwinSwordFlurry(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_TWIN_FLURRY_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 12.0);
      applyProjectedTwinSwords(entity, false, now);
      entity.triggerNamedActionAnimation("twin_flurry");
      for (int i = 0; i < 4; i++) {
         final int step = i;
         TYPE_MOON_WORLD.queueServerWork(step * 3 + 1, () -> {
            if (!entity.isAlive() || !target.isAlive() || !(entity.level() instanceof ServerLevel sl)) {
               return;
            }
            entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
            float damage = 12.0F + step * 2.0F;
            AABB hitBox = entity.getBoundingBox().inflate(2.2).expandTowards(entity.getLookAngle().scale(1.5));
            for (LivingEntity living : sl.getEntitiesOfClass(LivingEntity.class, hitBox,
               e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e))) {
               living.invulnerableTime = 0;
               living.hurt(entity.damageSources().mobAttack(entity), damage);
               living.invulnerableTime = 0;
            }
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, entity.getX() + entity.getLookAngle().x * 1.2, entity.getY() + entity.getBbHeight() * 0.55, entity.getZ() + entity.getLookAngle().z * 1.2, 2, 0.0, 0.0, 0.0, 0.0);
            sl.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.75F, 1.1F + step * 0.06F);
         });
      }
   }

   private static void executeKanshouBakuyaTriple(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      entity.getPersistentData().putLong(LAST_HRUNTING_STYLE_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 30.0);
      applyProjectedTwinSwords(entity, true, now);
      ServantVoiceHelper.tryPlayEmiyaTwinThrow(entity);
      entity.triggerSweepAnimation();
      for (int i = 0; i < 5; i++) {
         final int step = i;
         TYPE_MOON_WORLD.queueServerWork(i * 5 + 1, () -> {
            if (!entity.isAlive() || !target.isAlive() || !(entity.level() instanceof ServerLevel sl)) {
               return;
            }
            boolean finalRush = step >= 4;
            boolean ganJiang = (step & 1) == 0;
            ItemStack thrownStack = finalRush
               ? new ItemStack(ganJiang ? ModItems.GAN_JIANG_OVEREDGE.get() : ModItems.MO_YE_OVEREDGE.get())
               : new ItemStack(ganJiang ? ModItems.GAN_JIANG.get() : ModItems.MO_YE.get());
            EmiyaThrownWeaponEntity thrown = new EmiyaThrownWeaponEntity(sl, entity, thrownStack);
            Vec3 sideUnit = sideVector(entity, target);
            double sideOffset = finalRush ? (ganJiang ? 0.9 : -0.9) : (ganJiang ? 1.8 : -1.8);
            Vec3 side = sideUnit.scale(sideOffset);
            Vec3 spawn = entity.position().add(0.0, entity.getBbHeight() * 0.72, 0.0).add(side);
            thrown.setPos(spawn.x, spawn.y, spawn.z);
            thrown.setFixedDamage(finalRush ? 50.0F : 30.0F + entity.getRandom().nextFloat() * 10.0F);
            thrown.setBreakLowHardnessBlocks(true);
            thrown.setNoGravity(true);
            Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
            Vec3 pull = finalRush ? Vec3.ZERO : side.scale(-0.75);
            Vec3 dir = aim.subtract(thrown.position()).add(pull).normalize();
            Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
            if (horizontal.lengthSqr() < 1.0E-4) {
               horizontal = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
            }
            thrown.setArcingFlight(horizontal, sideOffset, finalRush ? 5.5 : 7.5);
            thrown.shoot(dir.x, dir.y + (finalRush ? 0.02 : 0.09), dir.z, finalRush ? 2.8F : 2.15F, 0.0F);
            sl.addFreshEntity(thrown);
            if (finalRush) {
               entity.triggerSlashAnimation();
               Vec3 dash = aim.subtract(entity.position());
               if (dash.lengthSqr() > 1.0E-4) {
                  Vec3 next = entity.position().add(dash.normalize().scale(Math.min(4.0, Math.sqrt(dash.lengthSqr()) - 1.0)));
                  entity.teleportTo(next.x, next.y, next.z);
                  entity.faceToward(aim);
               }
            }
         });
      }
   }

   private static Vec3 sideVector(EmiyaArcherEntity entity, LivingEntity target) {
      Vec3 forward = target.position().subtract(entity.position());
      if (forward.horizontalDistanceSqr() < 1.0E-4) {
         return new Vec3(1.0, 0.0, 0.0);
      }
      Vec3 horizontal = new Vec3(forward.x, 0.0, forward.z).normalize();
      return new Vec3(-horizontal.z, 0.0, horizontal.x);
   }

   private static void castRhoAias(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now) {
      castRhoAias(entity, level, target, now, true);
   }

   private static void castRhoAias(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now, boolean requireFullCost) {
      entity.getPersistentData().putLong(LAST_RHO_AIAS_TICK, now);
      double cost = requireFullCost ? 35.0 : Math.min(35.0, Math.max(0.0, entity.getCurrentMp()));
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - cost));
      clearProjection(entity);
      entity.triggerNamedActionAnimation("rho_aias");
      ServantVoiceHelper.tryPlayEmiyaRhoAias(entity);
      VFXServerEffects.spawn(level, "servant_emiya_rho_aias", entity, 96.0);
      RhoAiasEntity shield = new RhoAiasEntity(level, entity, target);
      level.addFreshEntity(shield);
   }

   private static boolean canCastRhoAias(EmiyaArcherEntity entity, long now, int cooldown) {
      return canUse(now, entity.getPersistentData().getLong(LAST_RHO_AIAS_TICK), cooldown)
         && entity.getPersistentData().getLong(LAST_RHO_AIAS_TICK) + cooldown <= now;
   }

   private static RhoAiasEntity findOwnedRhoAias(EmiyaArcherEntity entity, ServerLevel level) {
      RhoAiasEntity closest = null;
      double closestDistance = Double.MAX_VALUE;
      for (RhoAiasEntity shield : level.getEntitiesOfClass(
         RhoAiasEntity.class,
         entity.getBoundingBox().inflate(8.0),
         shield -> shield.isAlive() && shield.getOwnerEntity() == entity
      )) {
         double distance = shield.distanceToSqr(entity);
         if (distance < closestDistance) {
            closest = shield;
            closestDistance = distance;
         }
      }
      return closest;
   }

   private static void stayBehindRhoAias(EmiyaArcherEntity entity, RhoAiasEntity shield, LivingEntity target) {
      if (shield == null || !shield.isAlive()) {
         return;
      }
      Vec3 cover = shield.getCoverPosition(entity.getY());
      double distanceSqr = entity.distanceToSqr(cover.x, cover.y, cover.z);
      if (distanceSqr > 0.55) {
         entity.getNavigation().moveTo(cover.x, cover.y, cover.z, 1.0);
      } else {
         entity.getNavigation().stop();
      }
      if (target != null && target.isAlive()) {
         entity.faceToward(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
      } else {
         Vec3 ahead = shield.position().add(shield.getFacingDirection().scale(4.0));
         entity.faceToward(ahead);
      }
   }

   private static boolean tryDetonateRhoAias(EmiyaArcherEntity entity, ServerLevel level, RhoAiasEntity shield, LivingEntity target, long now, ServantCombatPhase phase) {
      if (shield == null || !shield.isAlive() || target == null || !target.isAlive()) {
         return false;
      }
      if (phase != ServantCombatPhase.DECISIVE && entity.getHealth() > entity.getMaxHealth() * 0.3F) {
         return false;
      }
      if (target.distanceToSqr(shield) > 5.2 * 5.2 || entity.distanceToSqr(shield) < 4.2 * 4.2) {
         return false;
      }
      if (entity.getRandom().nextInt(100) > 8) {
         return false;
      }
      shield.detonate();
      entity.getPersistentData().putLong(LAST_RHO_AIAS_TICK, now);
      level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.7, entity.getZ(), 24, 0.45, 0.45, 0.45, 0.1);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.0F, 0.85F);
      return true;
   }

   private static void maybeShield(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      if ((phase == ServantCombatPhase.DECISIVE || entity.getHealth() < entity.getMaxHealth() * 0.55F)
         && canCastRhoAias(entity, now, RHO_AIAS_HARD_COOLDOWN)
         && entity.getCurrentMp() >= 35.0) {
         castRhoAias(entity, level, target, now);
      }
   }

   private static void triggerBrokenPhantasm(EmiyaArcherEntity entity, ServerLevel level, long now, float selfRatio) {
      entity.getPersistentData().putLong(LAST_BROKEN_PHANTASM_TICK, now);
      entity.triggerSlashAnimation();
      ItemStack stack = entity.getMainHandItem().copy();
      if (!stack.isEmpty()) {
         UBWBrokenPhantasmExplosion.explode(level, entity, entity, stack, entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0));
      }
      clearProjection(entity);
   }

   private static void kiteBack(EmiyaArcherEntity entity, LivingEntity target, double distance) {
      Vec3 away = entity.position().subtract(target.position());
      if (away.lengthSqr() < 1.0E-4) {
         return;
      }
      Vec3 retreat = entity.position().add(away.normalize().scale(distance));
      entity.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.2);
   }

   private static boolean canUse(long now, long lastUse, int cooldown) {
      return lastUse <= 0L || now - lastUse >= cooldown;
   }

   private static int phasedCooldown(int baseCooldown, ServantCombatPhase phase) {
      if (phase == ServantCombatPhase.DECISIVE) {
         return Math.max(18, baseCooldown * 2 / 3);
      }
      if (phase == ServantCombatPhase.NORMAL) {
         return Math.max(20, baseCooldown * 5 / 6);
      }
      return baseCooldown;
   }

   private static int phaseChance(int baseChance, ServantCombatPhase phase) {
      if (phase == ServantCombatPhase.DECISIVE) {
         return Math.min(92, baseChance + 22);
      }
      if (phase == ServantCombatPhase.NORMAL) {
         return Math.min(88, baseChance + 10);
      }
      return baseChance;
   }

   private static void gatherSwordsOnLowHealth(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target) {
      for (int i = 0; i < 8; i++) {
         double angle = Math.PI * 2.0 * i / 8.0;
         double radius = 9.0 + entity.getRandom().nextDouble() * 5.0;
         double sx = target.getX() + Math.cos(angle) * radius;
         double sz = target.getZ() + Math.sin(angle) * radius;
         double sy = target.getY() + 3.5 + entity.getRandom().nextDouble() * 3.0;
         ItemStack stack = new ItemStack(Items.IRON_SWORD);
         UBWProjectileEntity sword = new UBWProjectileEntity(level, entity, stack);
         sword.setPos(sx, sy, sz);
         Vec3 dir = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(sword.position()).normalize();
         sword.setDeltaMovement(dir.scale(2.9));
         level.addFreshEntity(sword);
      }
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 24, 1.2, 1.0, 1.2, 0.12);
   }

   private static void interceptHostileProjectiles(EmiyaArcherEntity entity, ServerLevel level) {
      AABB area = entity.getBoundingBox().inflate(18.0);
      for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area, projectile -> projectile.isAlive() && shouldIntercept(entity, projectile))) {
         Vec3 spawn = projectile.position().add(projectile.getDeltaMovement().scale(-2.0)).add(0.0, 1.0 + entity.getRandom().nextDouble(), 0.0);
         level.addFreshEntity(new UBWInterceptorSwordEntity(level, projectile, entity.getUUID(), spawn));
      }
   }

   private static boolean shouldIntercept(EmiyaArcherEntity entity, Projectile projectile) {
      Entity owner = projectile.getOwner();
      return !(projectile instanceof UBWProjectileEntity)
         && !(projectile instanceof UBWInterceptorSwordEntity)
         && owner != entity
         && (owner == null || !owner.isAlliedTo(entity));
   }

   public static void markIronSwordImpactTerrain(ServerLevel level, Entity owner, BlockPos hitPos) {
      if (owner instanceof EmiyaArcherEntity archer && archer.getPersistentData().getLong(UBW_CHANT_END_TICK) > level.getGameTime()) {
         spreadUbwChantTerrainAtImpact(archer, level, hitPos);
      }
   }

   private static void spreadUbwChantTerrainAtImpact(EmiyaArcherEntity entity, ServerLevel level, BlockPos hitPos) {
      for (int ring = 0; ring <= 4; ring++) {
         final int radius = ring;
         TYPE_MOON_WORLD.queueServerWork(ring * 3, () -> {
            if (!entity.isAlive() || !(entity.level() instanceof ServerLevel currentLevel) || currentLevel != level) {
               return;
            }
            if (entity.getPersistentData().getLong(UBW_CHANT_END_TICK) <= level.getGameTime()) {
               return;
            }
            stainUbwChantRing(entity, level, hitPos, radius);
         });
      }
   }

   private static void stainUbwChantRing(EmiyaArcherEntity entity, ServerLevel level, BlockPos hitPos, int radius) {
      Map<BlockPos, BlockBackup> backups = EMIYA_UBW_CHANT_BLOCKS.computeIfAbsent(entity.getUUID(), key -> new HashMap<>());
      int maxChanged = radius == 0 ? 1 : 10 + radius * 4;
      int changed = 0;
      int minX = hitPos.getX() - radius;
      int maxX = hitPos.getX() + radius;
      int minZ = hitPos.getZ() - radius;
      int maxZ = hitPos.getZ() + radius;
      for (BlockPos pos : BlockPos.betweenClosed(minX, hitPos.getY() - 3, minZ, maxX, hitPos.getY() + 2, maxZ)) {
         if (changed >= maxChanged) {
            return;
         }
         int dx = Math.abs(pos.getX() - hitPos.getX());
         int dz = Math.abs(pos.getZ() - hitPos.getZ());
         if (Math.max(dx, dz) != radius || dx * dx + dz * dz > radius * radius + radius) {
            continue;
         }
         if (radius > 0 && level.random.nextInt(100) < 18) {
            continue;
         }
         BlockPos surface = findSurface(level, pos);
         if (surface == null || backups.containsKey(surface)) {
            continue;
         }
         BlockState current = level.getBlockState(surface);
         if (!canReplaceWithUbwSandstone(current)) {
            continue;
         }
         backups.put(surface.immutable(), new BlockBackup(current, saveBlockEntity(level, surface)));
         level.setBlock(surface, redSandstoneState(level), 3);
         changed++;
         level.sendParticles(ParticleTypes.FLAME, surface.getX() + 0.5, surface.getY() + 1.05, surface.getZ() + 0.5, 1, 0.1, 0.03, 0.1, 0.01);
      }
   }

   private static void spreadUbwChantSurfaceFromCaster(EmiyaArcherEntity entity, ServerLevel level, long now, int budget) {
      long chantAge = Math.max(0L, now - entity.getPersistentData().getLong(LAST_UBW_TICK));
      if (chantAge < UBW_CHANT_SURFACE_SPREAD_DELAY) {
         return;
      }

      int radius = Mth.clamp(2 + (int)((chantAge - UBW_CHANT_SURFACE_SPREAD_DELAY) / 4L), 2, UBW_CHANT_SURFACE_RADIUS);
      BlockPos center = entity.blockPosition();
      Map<BlockPos, BlockBackup> backups = EMIYA_UBW_CHANT_BLOCKS.computeIfAbsent(entity.getUUID(), key -> new HashMap<>());
      int changed = 0;
      int attempts = budget * 4;
      for (int i = 0; i < attempts && changed < budget; i++) {
         double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = Math.sqrt(entity.getRandom().nextDouble()) * radius;
         BlockPos sample = center.offset(Mth.floor(Math.cos(angle) * distance), 0, Mth.floor(Math.sin(angle) * distance));
         BlockPos surface = findSurface(level, sample);
         if (surface == null || backups.containsKey(surface)) {
            continue;
         }

         BlockState current = level.getBlockState(surface);
         if (!canReplaceWithUbwSandstone(current)) {
            continue;
         }

         backups.put(surface.immutable(), new BlockBackup(current, saveBlockEntity(level, surface)));
         level.setBlock(surface, redSandstoneState(level), 3);
         changed++;
         if (entity.getRandom().nextInt(2) == 0) {
            level.sendParticles(ParticleTypes.FLAME, surface.getX() + 0.5, surface.getY() + 1.05, surface.getZ() + 0.5, 1, 0.1, 0.03, 0.1, 0.01);
         }
      }
   }

   private static boolean canReplaceWithUbwSandstone(BlockState state) {
      return !state.isAir()
         && !state.hasBlockEntity()
         && !state.is(Blocks.BEDROCK)
         && !state.is(Blocks.RED_SANDSTONE)
         && !state.is(Blocks.SMOOTH_RED_SANDSTONE);
   }

   private static BlockState redSandstoneState(ServerLevel level) {
      return level.random.nextInt(5) == 0
         ? Blocks.SMOOTH_RED_SANDSTONE.defaultBlockState()
         : Blocks.RED_SANDSTONE.defaultBlockState();
   }

   private static void spreadUbwTerrain(EmiyaArcherEntity entity, ServerLevel level, int budget) {
      BlockPos center = ubwCenter(entity);
      long age = Math.max(0L, level.getGameTime() - entity.getPersistentData().getLong(LAST_UBW_TICK));
      int radius = Mth.clamp(3 + (int)(age / 6L), 3, UBW_TERRAIN_RADIUS);
      Map<BlockPos, BlockBackup> backups = EMIYA_UBW_BLOCKS.computeIfAbsent(entity.getUUID(), key -> new HashMap<>());
      for (int i = 0; i < budget; i++) {
         double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = Math.sqrt(entity.getRandom().nextDouble()) * radius;
         BlockPos sample = center.offset(Mth.floor(Math.cos(angle) * distance), 0, Mth.floor(Math.sin(angle) * distance));
         BlockPos surface = findSurface(level, sample);
         if (surface == null || backups.containsKey(surface)) {
            continue;
         }
         BlockState current = level.getBlockState(surface);
         if (!canReplaceWithUbwSandstone(current)) {
            continue;
         }
         BlockState replacement = redSandstoneState(level);
         backups.put(surface.immutable(), new BlockBackup(current, saveBlockEntity(level, surface)));
         level.setBlock(surface, replacement, 3);
         if (entity.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.FLAME, surface.getX() + 0.5, surface.getY() + 1.05, surface.getZ() + 0.5, 1, 0.1, 0.03, 0.1, 0.01);
         }
      }
   }

   private static BlockPos ubwCenter(EmiyaArcherEntity entity) {
      return new BlockPos(
         entity.getPersistentData().getInt(UBW_CENTER_X),
         entity.getPersistentData().getInt(UBW_CENTER_Y),
         entity.getPersistentData().getInt(UBW_CENTER_Z)
      );
   }

   private static BlockPos findSurface(ServerLevel level, BlockPos sample) {
      int startY = Mth.clamp(sample.getY() + 4, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
      int minY = Math.max(level.getMinBuildHeight() + 1, sample.getY() - 8);
      for (int y = startY; y >= minY; y--) {
         BlockPos pos = new BlockPos(sample.getX(), y, sample.getZ());
         if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
            return pos;
         }
      }
      return null;
   }

   private static CompoundTag saveBlockEntity(ServerLevel level, BlockPos pos) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      return blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());
   }

   private static void restoreUbwTerrain(EmiyaArcherEntity entity, ServerLevel level, int budget) {
      Map<BlockPos, BlockBackup> backups = EMIYA_UBW_BLOCKS.get(entity.getUUID());
      if (backups == null || backups.isEmpty()) {
         return;
      }
      Iterator<Map.Entry<BlockPos, BlockBackup>> iterator = backups.entrySet().iterator();
      int restored = 0;
      while (iterator.hasNext() && restored++ < budget) {
         Map.Entry<BlockPos, BlockBackup> entry = iterator.next();
         BlockPos pos = entry.getKey();
         BlockBackup backup = entry.getValue();
         level.setBlock(pos, backup.state(), 3);
         if (backup.blockEntityNbt() != null && level.getBlockEntity(pos) instanceof BlockEntity blockEntity) {
            blockEntity.loadWithComponents(backup.blockEntityNbt(), level.registryAccess());
            blockEntity.setChanged();
         }
         iterator.remove();
      }
      if (backups.isEmpty()) {
         EMIYA_UBW_BLOCKS.remove(entity.getUUID());
         entity.getPersistentData().remove(UBW_ACTIVE_UNTIL);
      }
   }

   private static void restoreUbwChantTerrain(EmiyaArcherEntity entity, ServerLevel level, int budget) {
      Map<BlockPos, BlockBackup> backups = EMIYA_UBW_CHANT_BLOCKS.get(entity.getUUID());
      if (backups == null || backups.isEmpty()) {
         return;
      }
      Iterator<Map.Entry<BlockPos, BlockBackup>> iterator = backups.entrySet().iterator();
      int restored = 0;
      while (iterator.hasNext() && restored++ < budget) {
         Map.Entry<BlockPos, BlockBackup> entry = iterator.next();
         BlockPos pos = entry.getKey();
         BlockBackup backup = entry.getValue();
         level.setBlock(pos, backup.state(), 3);
         if (backup.blockEntityNbt() != null && level.getBlockEntity(pos) instanceof BlockEntity blockEntity) {
            blockEntity.loadWithComponents(backup.blockEntityNbt(), level.registryAccess());
            blockEntity.setChanged();
         }
         iterator.remove();
      }
      if (backups.isEmpty()) {
         EMIYA_UBW_CHANT_BLOCKS.remove(entity.getUUID());
      }
   }

   private static boolean tryStartNpcSphericalProjection(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      if (data.getInt(SPHERICAL_ROUNDS) > 0
         || !canUse(now, data.getLong(LAST_SPHERICAL_PROJECTION_TICK), phasedCooldown(12 * 20, phase))
         || entity.getCurrentMp() < 45.0
         || (phase != ServantCombatPhase.DECISIVE && entity.getRandom().nextInt(100) >= phaseChance(22, phase))) {
         return false;
      }
      data.putLong(LAST_SPHERICAL_PROJECTION_TICK, now);
      data.putInt(SPHERICAL_ROUNDS, 10);
      data.putLong(SPHERICAL_NEXT_TICK, now);
      data.putUUID(SPHERICAL_TARGET, target.getUUID());
      entity.setCurrentMp(entity.getCurrentMp() - 45.0);
      entity.triggerNamedActionAnimation("projection");
      level.playSound(null, entity.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 0.8F, 1.3F);
      return true;
   }

   private static boolean tickNpcSphericalProjection(EmiyaArcherEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      int rounds = data.getInt(SPHERICAL_ROUNDS);
      if (rounds <= 0 || now < data.getLong(SPHERICAL_NEXT_TICK)) return false;
      Entity targetEntity = data.hasUUID(SPHERICAL_TARGET) ? level.getEntity(data.getUUID(SPHERICAL_TARGET)) : null;
      if (!(targetEntity instanceof LivingEntity target) || !target.isAlive() || entity.distanceToSqr(target) > 4096.0) {
         data.remove(SPHERICAL_ROUNDS);
         data.remove(SPHERICAL_NEXT_TICK);
         data.remove(SPHERICAL_TARGET);
         return false;
      }
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      double radius = Math.max(3.2, target.getBbWidth() * 2.0 + 2.2);
      for (int i = 0; i < 20; i++) {
         double phi = entity.getRandom().nextDouble() * Math.PI * 2.0;
         double u = entity.getRandom().nextDouble() * 2.0 - 1.0;
         double theta = Math.acos(u);
         Vec3 spawn = center.add(radius * Math.sin(theta) * Math.cos(phi), radius * Math.sin(theta) * Math.sin(phi), radius * Math.cos(theta));
         SwordBarrelProjectileEntity sword = new SwordBarrelProjectileEntity(level, entity, new ItemStack(Items.IRON_SWORD));
         sword.setPos(spawn.x, spawn.y, spawn.z);
         sword.setOwner(entity);
         sword.setTargetEntity(target.getId());
         sword.setHover(15, center);
         sword.setMode1Tracking(true);
         level.addFreshEntity(sword);
      }
      rounds--;
      if (rounds <= 0) {
         data.remove(SPHERICAL_ROUNDS);
         data.remove(SPHERICAL_NEXT_TICK);
         data.remove(SPHERICAL_TARGET);
      } else {
         data.putInt(SPHERICAL_ROUNDS, rounds);
         data.putLong(SPHERICAL_NEXT_TICK, now + 10L);
      }
      return true;
   }

   private static boolean tryStartNpcAutoCounter(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target, long now, ServantCombatPhase phase) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(AUTO_COUNTER_UNTIL) >= now
         || !canUse(now, data.getLong(LAST_AUTO_COUNTER_TICK), phasedCooldown(30 * 20, phase))
         || entity.getCurrentMp() < 60.0
         || (!hasHostileProjectile(entity, level) && (phase != ServantCombatPhase.DECISIVE && entity.getRandom().nextInt(100) >= phaseChance(12, phase)))) {
         return false;
      }
      data.putLong(LAST_AUTO_COUNTER_TICK, now);
      data.putLong(AUTO_COUNTER_UNTIL, now + 300L);
      entity.setCurrentMp(entity.getCurrentMp() - 60.0);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 28, 1.0, 0.8, 1.0, 0.06);
      level.playSound(null, entity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 0.8F, 1.35F);
      return true;
   }

   private static void tickNpcAutoCounter(EmiyaArcherEntity entity, ServerLevel level, long now) {
      CompoundTag data = entity.getPersistentData();
      if (data.getLong(AUTO_COUNTER_UNTIL) < now) return;
      if (now % 4L == 0L) {
         int intercepted = 0;
         for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, entity.getBoundingBox().inflate(20.0), p ->
            p.isAlive() && p.getOwner() != entity && !(p instanceof UBWProjectileEntity) && !(p instanceof UBWInterceptorSwordEntity)
               && !p.getPersistentData().getBoolean(AUTO_COUNTER_CLAIMED))) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living && entity.isAlliedTo(living)) continue;
            projectile.getPersistentData().putBoolean(AUTO_COUNTER_CLAIMED, true);
            Vec3 spawn = entity.position().add((entity.getRandom().nextDouble() - 0.5) * 3.0, 2.0 + entity.getRandom().nextDouble() * 2.0,
               (entity.getRandom().nextDouble() - 0.5) * 3.0);
            level.addFreshEntity(new UBWInterceptorSwordEntity(level, projectile, entity.getUUID(), spawn));
            if (++intercepted >= 6) break;
         }
      }
      if (now % 10L == 0L) {
         LivingEntity target = entity.getTarget();
         if (target != null && target.isAlive() && !entity.isAlliedTo(target)) {
            spawnNpcTrackingSword(entity, level, target);
         }
      }
   }

   private static boolean hasHostileProjectile(EmiyaArcherEntity entity, ServerLevel level) {
      return !level.getEntitiesOfClass(Projectile.class, entity.getBoundingBox().inflate(20.0), p -> {
         if (!p.isAlive() || p.getOwner() == entity || p instanceof UBWProjectileEntity || p instanceof UBWInterceptorSwordEntity) return false;
         Entity owner = p.getOwner();
         return !(owner instanceof LivingEntity living) || !entity.isAlliedTo(living);
      }).isEmpty();
   }

   private static void spawnNpcTrackingSword(EmiyaArcherEntity entity, ServerLevel level, LivingEntity target) {
      double angle = entity.getRandom().nextDouble() * Math.PI * 2.0;
      double radius = 4.0 + entity.getRandom().nextDouble() * 8.0;
      Vec3 spawn = target.position().add(Math.cos(angle) * radius, 4.0 + entity.getRandom().nextDouble() * 4.0, Math.sin(angle) * radius);
      UBWProjectileEntity sword = new UBWProjectileEntity(level, entity, new ItemStack(Items.IRON_SWORD));
      sword.setPos(spawn.x, spawn.y, spawn.z);
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(spawn).normalize();
      sword.setDeltaMovement(aim.scale(2.4));
      level.addFreshEntity(sword);
   }

   private record BlockBackup(BlockState state, CompoundTag blockEntityNbt) {
   }
}
