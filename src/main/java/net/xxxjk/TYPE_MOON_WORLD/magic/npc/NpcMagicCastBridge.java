package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import com.example.typemoonaddon.airflow_blade.AirflowBladeService;
import com.example.typemoonaddon.detection.DetectionService;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService.CastStatus;
import com.example.typemoonaddon.magic.EntityDisplacementService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.LeffLaynorFlaurosEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TohsakaRinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TopazProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicAnalysisService;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.gravity.GemGravityFieldMagic;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicBinding;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicEarthElement;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicFireElement;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicHealing;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicMagicBullet;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicSuggestion;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicWaterElement;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicWindElement;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravityEffectHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.special.SpiritronCannonService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.AdvancedPassiveService;
import net.xxxjk.TYPE_MOON_WORLD.util.NightVisionEffectSource;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineService;

public final class NpcMagicCastBridge {
   private static final String TAG_MAGIC_INIT = "TypeMoonNpcMagicInitV3";
   private static final String TAG_LAST_DECISION_TICK = "TypeMoonNpcLastDecisionTick";
   private static final String TAG_LAST_COMBAT_TICK = "TypeMoonNpcLastCombatTick";
   private static final String TAG_NEXT_GLOBAL_CAST_TICK = "TypeMoonNpcNextGlobalCastTick";
   private static final String TAG_MAGIC_CD = "TypeMoonNpcMagicCd";
   private static final String TAG_CAST_LOCK_UNTIL = "TypeMoonNpcCastLockUntil";
   private static final String TAG_RESOURCE_USE = "TypeMoonNpcResourceUse";
   private static final String TAG_LAST_CAST_MAGIC = "TypeMoonNpcLastCastMagic";
   private static final String TAG_CAST_REPEAT_COUNT = "TypeMoonNpcCastRepeatCount";
   private static final String TAG_RETREAT_COUNTER_TICK = "TypeMoonNpcRetreatCounterTick";
   private static final String TAG_RETREAT_UTILITY_TICK = "TypeMoonNpcRetreatUtilityTick";
   private static final String TAG_THREAT_UTILITY_TICK = "TypeMoonNpcThreatUtilityTick";
   private static final String TAG_THREAT_CONTROL_TICK = "TypeMoonNpcThreatControlTick";
   private static final String TAG_THREAT_COUNTER_TICK = "TypeMoonNpcThreatCounterTick";
   private static final String TAG_ENV_UTILITY_TICK = "TypeMoonNpcEnvUtilityTick";
   private static final String TAG_ENV_MOVE_TICK = "TypeMoonNpcEnvMoveTick";
   private static final String TAG_FIRE_IGNITE_TICK = "TypeMoonNpcFireIgniteTick";
   private static final String TAG_FIRE_NEXT_WATER_SEARCH_TICK = "TypeMoonNpcFireNextWaterSearchTick";
   private static final String TAG_CAP_HAS_RANGED = "TypeMoonNpcCapHasRanged";
   private static final String TAG_CAP_HAS_MELEE_BURST = "TypeMoonNpcCapHasMeleeBurst";
   private static final String TAG_CAP_ONLY_MELEE = "TypeMoonNpcCapOnlyMelee";
   private static final String TAG_CAP_HAS_BUFF = "TypeMoonNpcCapHasBuff";
   private static final String TAG_CAP_HAS_CONTROL = "TypeMoonNpcCapHasControl";
   private static final String TAG_CAP_HAS_ANY = "TypeMoonNpcCapHasAny";
   private static final String TAG_CAP_CACHE_TICK = "TypeMoonNpcCapCacheTick";
   private static final String TAG_ENV_CACHE_TICK = "TypeMoonNpcEnvCacheTick";
   private static final String TAG_ENV_CACHE_ANY = "TypeMoonNpcEnvCacheAny";
   private static final String TAG_ENV_CACHE_SEVERE = "TypeMoonNpcEnvCacheSevere";
   private static final String TAG_ENV_CACHE_SCORE = "TypeMoonNpcEnvCacheScore";
   private static final String TAG_THREAT_CACHE_TICK = "TypeMoonNpcThreatCacheTick";
   private static final String TAG_THREAT_CACHE_TARGET = "TypeMoonNpcThreatCacheTarget";
   private static final String TAG_THREAT_CACHE_COUNT = "TypeMoonNpcThreatCacheCount";
   private static final String TAG_THREAT_CACHE_STRONG = "TypeMoonNpcThreatCacheStrong";
   private static final String TAG_THREAT_CACHE_LOW = "TypeMoonNpcThreatCacheLow";
   private static final String TAG_MIGRATION_CLEANED_V3 = "TypeMoonNpcMigrationCleanedV3";
   private static final String TAG_FIXED_LEVEL = "TypeMoonNpcFixedLevel";
   private static final String TAG_COMBAT_LEVEL_BONUS = "TypeMoonNpcCombatLevelBonus";
   private static final String TAG_REINF_BODY_CD = "TypeMoonNpcReinfBodyCd";
   private static final String TAG_REINF_HAND_CD = "TypeMoonNpcReinfHandCd";
   private static final String TAG_REINF_LEG_CD = "TypeMoonNpcReinfLegCd";
   private static final String TAG_REINF_EYE_CD = "TypeMoonNpcReinfEyeCd";
   private static final String TAG_MELEE_PUNCH_CD = "TypeMoonNpcMeleePunchCd";
   private static final String TAG_MELEE_KICK_CD = "TypeMoonNpcMeleeKickCd";
   private static final String TAG_MELEE_THROW_CD = "TypeMoonNpcMeleeThrowCd";
   private static final String TAG_MELEE_SLAM_CD = "TypeMoonNpcMeleeSlamCd";
   private static final String TAG_PENDING_MG_MAGIC = "TypeMoonNpcPendingMgMagic";
   private static final String TAG_PENDING_MG_TARGET = "TypeMoonNpcPendingMgTarget";
   private static final String TAG_PENDING_MG_NEXT_TICK = "TypeMoonNpcPendingMgNextTick";
   private static final String TAG_PENDING_MG_WAVES_LEFT = "TypeMoonNpcPendingMgWavesLeft";
   private static final String TAG_PENDING_MG_INTERVAL = "TypeMoonNpcPendingMgInterval";
   private static final String TAG_PENDING_MG_MODE = "TypeMoonNpcPendingMgMode";
   private static final String TAG_PENDING_MG_CHARGE = "TypeMoonNpcPendingMgCharge";
   private static final String TAG_PENDING_MG_SHOT_COUNT = "TypeMoonNpcPendingMgShotCount";
   private static final String TAG_PENDING_MG_BLOCKED_RETRIES = "TypeMoonNpcPendingMgBlockedRetries";
   private static final String TAG_JEWEL_ITEM_NEXT_TICK = "TypeMoonNpcJewelItemNextTick";
   private static final String TAG_ANALYSIS_TEMPORARY = "TypeMoonNpcAnalysisTemporary";
   private static final String TAG_ANALYSIS_EXPIRES = "TypeMoonNpcAnalysisExpires";
   private static final String TAG_ANALYSIS_ALREADY_KNOWN = "TypeMoonNpcAnalysisAlreadyKnown";
   private static final String TAG_ATTR_INIT = "TypeMoonNpcAttrInitV1";
   private static final String TAG_BASE_MAX_HEALTH = "TypeMoonNpcBaseMaxHealth";
   private static final String TAG_BASE_MOVE_SPEED = "TypeMoonNpcBaseMoveSpeed";
   private static final String TAG_BASE_ATTACK_DAMAGE = "TypeMoonNpcBaseAttackDamage";
   private static final String TAG_COMBAT_MOVE_SPEED = "TypeMoonNpcCombatMoveSpeed";
   private static final String NPC_JEWEL_ITEM_BASIC = "npc_jewel_item_basic";
   private static final String NPC_JEWEL_ITEM_ADVANCED = "npc_jewel_item_advanced";
   private static final String NPC_JEWEL_ITEM_ENGRAVED = "npc_jewel_item_engraved";
   private static final int DECISION_INTERVAL_TICKS = 5;
   private static final int CIRCUIT_CLOSE_DELAY_TICKS = 120;
   private static final int CAST_RANGE = 28;
   private static final int MAX_SELF_MAGICS = 8;
   private static final int MIN_SELF_MAGICS = 4;
   private static final int MIN_CREST_MAGICS = 1;
   private static final int MAX_CREST_MAGICS = 4;
   private static final double ZOMBIE_BASE_MAX_HEALTH = 20.0;
   private static final double ZOMBIE_BASE_MOVE_SPEED = 0.23;
   private static final double ZOMBIE_BASE_ATTACK_DAMAGE = 3.0;
   private static final double ATTRIBUTE_RANDOM_BASE_MIN_MULTIPLIER = 0.9;
   private static final double ATTRIBUTE_RANDOM_BASE_MAX_MULTIPLIER = 1.35;
   private static final double ATTRIBUTE_RANDOM_ELITE_MAX_MULTIPLIER = 2.0;
   private static final double COMBAT_MOVE_SPEED_MULTIPLIER = 1.45;
   private static final double HIGH_THREAT_HEALTH_RATIO = 0.65;
   private static final int HIGH_THREAT_ENEMY_COUNT = 3;
   private static final double HIGH_THREAT_POWER_RATIO = 1.18;
   private static final double THREAT_SCAN_RANGE = 20.0;
   private static final int ENV_SCAN_RADIUS = 4;
   private static final int FIRE_AUTO_EXTINGUISH_TICKS = 200;
   private static final int FIRE_WATER_SEARCH_INTERVAL_TICKS = 10;
   private static final int FIRE_WATER_SEARCH_HORIZONTAL_RADIUS = 12;
   private static final int FIRE_WATER_SEARCH_VERTICAL_RADIUS = 5;
   private static final int MAX_PENDING_MG_BLOCKED_RETRIES = 6;
   private static final double MAX_TARGET_DISTANCE_SQR = 576.0;
   private static final double MAX_TARGET_VERTICAL_GAP = 10.0;
   private static final double MAX_BLIND_VERTICAL_GAP = 4.5;
   private static final float[] RAPID_YAW = new float[]{0.0F, -1.2F, 1.2F};
   private static final float[] RAPID_PITCH = new float[]{0.0F, -0.8F, 0.8F};
   private static final double[] RAPID_SIDE = new double[]{0.0, -0.12, 0.12};
   private static final double[] RAPID_UP = new double[]{0.0, 0.06, -0.06};
   private static final double[] MANA_BURST_UPKEEP = new double[]{15.0, 25.0, 35.0, 45.0, 55.0};
   private static final double[] MANA_BURST_DIRECT_COST = new double[]{150.0, 250.0, 350.0, 450.0, 550.0};
   private static final float[] MANA_BURST_DIRECT_DAMAGE = new float[]{40.0F, 80.0F, 120.0F, 160.0F, 200.0F};
   private static final double[] MANA_BURST_DIRECT_RANGE = new double[]{20.0, 28.0, 36.0, 44.0, 52.0};
   private static final String[] LEFF_FIXED_MAGICS = new String[]{
      "aerial_stasis",
      "aerial_ascent",
      "magic_analysis",
      "airflow_blade",
      "suggestion_magic",
      "reinforcement",
      "detection",
      "imaginary_displacement",
      "imaginary_space",
      "spiritron_cannon"
   };
   private static final double RETREAT_HEALTH_RATIO = 0.28;
   private static final double RETREAT_DISTANCE = 12.0;
   private static final double RANGED_KEEP_MIN_DISTANCE = 7.0;
   private static final double RANGED_KEEP_MAX_DISTANCE = 14.0;
   private static final double MELEE_ENGAGE_DISTANCE = 4.2;
   private static final double RANGED_ONLY_RETREAT_DISTANCE = 5.0;
   private static final double ADVANCED_PREFERRED_MANA_RATIO = 0.42;
   private static final int LEVEL_MIN = 1;
   private static final int LEVEL_MAX = 5;
   private static final int COMBAT_BONUS_MIN = 0;
   private static final int COMBAT_BONUS_MAX = 2;
   private static final GemType[] NPC_BASIC_JEWEL_TYPES = new GemType[]{
      GemType.RUBY, GemType.SAPPHIRE, GemType.TOPAZ, GemType.CYAN, GemType.WHITE_GEMSTONE, GemType.BLACK_SHARD
   };
   private static final Set<String> VALID_MAGIC_IDS = Set.of(
      "gander",
      "gandr_machine_gun",
      "gravity_magic",
      "reinforcement",
      "jewel_random_shoot",
      "jewel_machine_gun",
      "healing_magic",
      "magic_bullet",
      "magic_analysis",
      "suggestion_magic",
      "binding_magic",
      "airflow_blade",
      "detection",
      "imaginary_displacement",
      "imaginary_space",
      "spiritron_cannon",
      "mana_burst",
      "fire_magic",
      "water_magic",
      "wind_magic",
      "earth_magic",
      "aerial_stasis",
      "aerial_ascent",
      "ruby_flame_sword",
      "sapphire_winter_frost",
      "emerald_winter_river",
      "topaz_reinforcement",
      "cyan_wind",
      "wraith_servitude",
      "evil_spirit_summoning",
      "worm_control",
      "engraved_worm_operation"
   );
   private static final NpcMagicCastBridge.BehaviorProfile[][] BEHAVIOR_PROFILE_MATRIX = new NpcMagicCastBridge.BehaviorProfile[][]{
      {
         new NpcMagicCastBridge.BehaviorProfile(0.43, 9.5, 17.5, 3.4, 2.0),
         new NpcMagicCastBridge.BehaviorProfile(0.34, 8.2, 15.5, 3.9, 2.3),
         new NpcMagicCastBridge.BehaviorProfile(0.27, 7.2, 13.8, 4.4, 2.6)
      },
      {
         new NpcMagicCastBridge.BehaviorProfile(0.4, 9.0, 16.8, 3.7, 2.1),
         new NpcMagicCastBridge.BehaviorProfile(0.31, 7.8, 14.6, 4.2, 2.4),
         new NpcMagicCastBridge.BehaviorProfile(0.24, 6.8, 13.0, 4.7, 2.8)
      },
      {
         new NpcMagicCastBridge.BehaviorProfile(0.37, 8.4, 16.0, 4.0, 2.3),
         new NpcMagicCastBridge.BehaviorProfile(0.29, 7.3, 14.0, 4.5, 2.6),
         new NpcMagicCastBridge.BehaviorProfile(0.2, 6.2, 12.2, 5.0, 3.0)
      }
   };

   private NpcMagicCastBridge() {
   }

   public static void onSpawnInitialized(MysticMagicianEntity npc) {
      if (npc != null && !npc.level().isClientSide()) {
         if (npc instanceof LeffLaynorFlaurosEntity) {
            configureLeff(npc);
            return;
         }
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)npc.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         initializeIfNeeded(npc, vars);
      }
   }

   public static boolean shouldUseMeleeGoal(MysticMagicianEntity npc) {
      if (npc == null) {
         return true;
      } else {
         CompoundTag data = npc.getPersistentData();
         boolean hasRanged = data.getBoolean(TAG_CAP_HAS_RANGED);
         boolean hasMeleeBurst = data.getBoolean(TAG_CAP_HAS_MELEE_BURST);
         boolean onlyMelee = data.getBoolean(TAG_CAP_ONLY_MELEE);
         NpcCombatTemperament temperament = npc.getCombatTemperament();
         LivingEntity target = npc.getTarget();
         if (target != null && target.isAlive()) {
            double distanceSqr = npc.distanceToSqr(target);
            if (onlyMelee) {
               return distanceSqr <= 121.0;
            } else if (hasRanged && hasMeleeBurst) {
               return distanceSqr <= switch (temperament) {
                  case TIMID -> 20.25;
                  case BOLD -> 42.25;
                  default -> 30.25;
               };
            } else if (hasRanged) {
               // Ranged-only (typically no reinforcement): avoid committing to melee unless target is very close.
               return distanceSqr <= switch (temperament) {
                  case TIMID -> 2.25;
                  case BOLD -> 6.25;
                  default -> 4.0;
               };
            } else {
               return distanceSqr <= 25.0;
            }
         } else {
            return !hasRanged;
         }
      }
   }

   public static void tickServer(MysticMagicianEntity npc) {
      if (npc != null && !npc.level().isClientSide()) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)npc.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         initializeIfNeeded(npc, vars);
         if (OriginBulletHelper.isNpcSealed(npc)) {
            vars.is_magic_circuit_open = false;
            vars.magic_circuit_open_timer = 0.0;
            vars.player_mana = 0.0;
            tickLocalCooldown(vars);
            clearPendingMachineGun(npc);
            return;
         }
         long gameTime = npc.level().getGameTime();
         handleOnFireEmergency(npc, gameTime);
         tickProjectedHandItemExpiry(npc, gameTime);
         tickManaRecovery(npc, vars);
         tickLocalCooldown(vars);
         cleanupTemporaryAnalyzedMagics(npc, vars, gameTime, false);
         NpcMagicCastBridge.MagicCapabilityProfile capabilities = getCachedMagicCapabilities(npc, vars, gameTime);
         syncCapabilityFlags(npc, capabilities);
         NpcMagicCastBridge.EnvironmentHazardProfile environment = getCachedEnvironmentalHazard(npc, gameTime);
         if (environment.anyHazard()) {
            boolean envConsumed = handleEnvironmentalHazard(npc, vars, gameTime, environment, capabilities);
            if (envConsumed) {
               return;
            }
         }

         LivingEntity target = npc.getTarget();
         boolean validTarget = isValidCombatTarget(npc, target);
         long lastCombatTick = npc.getPersistentData().getLong(TAG_LAST_COMBAT_TICK);
         boolean combatMoveState = validTarget || gameTime - lastCombatTick <= 40L;
         applyCombatMovementState(npc, combatMoveState);
         if (validTarget) {
            npc.getPersistentData().putLong(TAG_LAST_COMBAT_TICK, gameTime);
            vars.is_magic_circuit_open = true;
            vars.magic_circuit_open_timer = 0.0;
            NpcMagicCastBridge.ThreatProfile threat = getCachedThreat(npc, target, gameTime);
            NpcMagicCastBridge.BehaviorProfile behavior = buildBehaviorProfile(npc.getCombatPersonality(), npc.getCombatTemperament());
            updateCombatLevelBonus(npc, target, threat);
            maintainPreferredCombatDistance(npc, target, capabilities, threat, behavior);

            if (trySelfGravityStabilize(npc, vars, gameTime, false)) {
               return;
            }

            // Reinforcement is treated as a parallel support cast and should not block offensive casting flow.
            tryAdaptiveReinforcement(npc, target, vars, gameTime, threat, capabilities, behavior);

            // A target already in striking range must get a chance to retaliate before retreat logic.
            // This keeps damaged NPCs from permanently circling the player without ever swinging.
            double distance = Math.sqrt(npc.distanceToSqr(target));
            if (gameTime >= getCastLockUntil(npc) && tryMeleeSkillCombo(npc, target, vars, gameTime, capabilities, behavior)) {
               return;
            }

            if (shouldRetreat(npc, target, threat, behavior)) {
               handleRetreat(npc, target, vars, gameTime);
            } else {
               if (processPendingMachineGun(npc, target, vars, gameTime)) {
                  return;
               }

               if (shouldForceRangedRetreat(capabilities, distance)) {
                  steerAwayFromTarget(npc, target, 14.0, 1.3);
               } else {
                  if (threat.requiresDefensiveTactics()) {
                     boolean consumedCast = handleHighThreatTactics(npc, target, vars, gameTime, threat, capabilities);
                     if (consumedCast) {
                        return;
                     }
                  }

                  if (gameTime >= getCastLockUntil(npc)) {
                     long lastDecision = npc.getPersistentData().getLong(TAG_LAST_DECISION_TICK);
                     if (gameTime - lastDecision >= 5L) {
                        npc.getPersistentData().putLong(TAG_LAST_DECISION_TICK, gameTime);
                        if (gameTime >= npc.getPersistentData().getLong(TAG_NEXT_GLOBAL_CAST_TICK)) {
                           if (!(vars.player_mana <= 1.0)) {
                              if (!tryEmergencyHealingMagic(npc, vars, gameTime) && !tryUseJewelItemSkill(npc, target, vars, gameTime, threat)) {
                                 TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slot = chooseNextMagic(npc, vars, target, gameTime, capabilities);
                                 if (slot == null || slot.magicId == null || slot.magicId.isEmpty()) {
                                    handleNoMagicFallback(npc, target, vars, gameTime, threat, capabilities);
                                 } else if (isResourceMagicAvailable(npc, slot.magicId, gameTime)) {
                                    if (gameTime >= getMagicCooldownUntil(npc, slot.magicId)) {
                                       boolean crestCast = "crest".equals(slot.sourceType);
                                       double effectiveProficiency = getEffectiveProficiency(npc, vars, slot.magicId, crestCast);
                                       boolean castSuccess = NpcMagicExecutionService.castMagic(npc, target, vars, slot, effectiveProficiency, gameTime);
                                       if (castSuccess) {
                                          int gcd = NpcMagicExecutionService.getGlobalCooldownAfterCast(slot.magicId, slot.presetPayload);
                                          vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
                                          npc.getPersistentData().putLong(TAG_NEXT_GLOBAL_CAST_TICK, gameTime + gcd);
                                          setMagicCooldown(npc, slot.magicId, gameTime + NpcMagicExecutionService.getPerMagicCooldown(slot.magicId, slot.presetPayload));
                                          recordResourceMagicUse(npc, slot.magicId, gameTime);
                                          recordMagicCast(npc, slot.magicId);
                                          setCastLockUntil(npc, gameTime + Math.max(6, gcd));
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         } else {
            if (target != null && !validTarget) {
               npc.setTarget(null);
            }

            npc.getPersistentData().putInt(TAG_COMBAT_LEVEL_BONUS, 0);
            clearPendingMachineGun(npc);
            cleanupTemporaryAnalyzedMagics(npc, vars, gameTime, true);
            maybeCloseCircuit(npc, vars, gameTime);
         }
      }
   }

   public static void configureTohsakaRin(MysticMagicianEntity npc) {
      if (npc == null || npc.level().isClientSide()) return;
      TypeMoonWorldModVariables.PlayerVariables vars = npc.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ensureMagicSystemInitialized();
      vars.clearAllWheelSlots();
      vars.learned_magics.clear();
      vars.crest_entries.clear();
      vars.player_max_mana = 1000.0;
      vars.player_mana = 1000.0;
      vars.player_mana_egenerated_every_moment = 8.0;
      vars.player_restore_magic_moment = 4.0;
      vars.is_magus = true;
      vars.player_magic_attributes_earth = true;
      vars.player_magic_attributes_water = true;
      vars.player_magic_attributes_fire = true;
      vars.player_magic_attributes_wind = true;
      vars.player_magic_attributes_ether = true;
      vars.player_magic_attributes_none = false;
      vars.player_magic_attributes_imaginary_number = false;
      vars.player_magic_attributes_sword = false;
      vars.bajiquan_learned = true;
      vars.bajiquan_proficiency = 60.0;
      vars.bajiquan_tiger_unlocked = false;
      if (!vars.learned_magics.contains("bajiquan")) vars.learned_magics.add("bajiquan");
      vars.proficiency_gander = 80.0;
      vars.proficiency_gravity_magic = 70.0;
      vars.proficiency_reinforcement = 65.0;
      vars.proficiency_jewel_magic_shoot = 85.0;
      vars.proficiency_jewel_magic_release = 85.0;
      setNpcKnownMagic(vars, "jewel_magic_shoot", 85.0);
      setNpcKnownMagic(vars, "jewel_magic_release", 85.0);
      setNpcKnownMagic(vars, "jewel_random_shoot", 85.0);
      setNpcKnownMagic(vars, "jewel_machine_gun", 85.0);
      setNpcKnownMagic(vars, "aerial_stasis", 100.0);
      setNpcKnownMagic(vars, "aerial_ascent", 100.0);
      setNpcKnownMagic(vars, "suggestion_magic", 60.0);
      setNpcKnownMagic(vars, "healing_magic", 60.0);
      setNpcKnownMagic(vars, "magic_analysis", 78.0);
      setNpcKnownMagic(vars, "detection", 90.0);
      grantTohsakaRinMagicCrest(npc, vars);
      String[] magics = new String[]{
         "gander",
         "gandr_machine_gun",
         "gravity_magic",
         "reinforcement",
         "jewel_random_shoot",
         "jewel_machine_gun",
         "aerial_ascent",
         "aerial_stasis",
         "suggestion_magic",
         "healing_magic",
         "detection",
         "magic_analysis"
      };
      for (int i = 0; i < magics.length; i++) {
         String magic = magics[i];
         if (!vars.learned_magics.contains(magic)) vars.learned_magics.add(magic);
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(0, i);
         entry.magicId = magic;
         entry.sourceType = "self";
         if ("gandr_machine_gun".equals(magic)) entry.presetPayload.putInt("gandr_machine_gun_mode", 1);
         if ("gravity_magic".equals(magic)) { entry.presetPayload.putInt("gravity_target", 1); entry.presetPayload.putInt("gravity_mode", 2); }
         if ("reinforcement".equals(magic)) { entry.presetPayload.putInt("reinforcement_target", 0); entry.presetPayload.putInt("reinforcement_mode", 0); entry.presetPayload.putInt("reinforcement_level", 4); }
         vars.setWheelSlotEntry(0, i, entry);
      }
      vars.rebuildSelectedMagicsFromActiveWheel();
      CompoundTag data = npc.getPersistentData();
      data.putBoolean(TAG_MAGIC_INIT, true);
      data.putBoolean(TAG_ATTR_INIT, true);
      data.putInt(TAG_FIXED_LEVEL, 5);
      data.putDouble(TAG_BASE_MAX_HEALTH, TohsakaRinEntity.MAX_HEALTH);
      data.putDouble(TAG_BASE_MOVE_SPEED, 0.27);
      data.putDouble(TAG_COMBAT_MOVE_SPEED, 0.39);
      data.putDouble(TAG_BASE_ATTACK_DAMAGE, 5.0);
      data.putInt(NPC_JEWEL_ITEM_BASIC, 64);
      data.putInt(NPC_JEWEL_ITEM_ADVANCED, 32);
      data.putInt(NPC_JEWEL_ITEM_ENGRAVED, 16);
      if (npc.getAttribute(Attributes.MAX_HEALTH) != null) npc.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TohsakaRinEntity.MAX_HEALTH);
      if (npc.getAttribute(Attributes.MOVEMENT_SPEED) != null) npc.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.27);
      if (npc.getAttribute(Attributes.ATTACK_DAMAGE) != null) npc.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0);
      npc.setHealth((float)TohsakaRinEntity.MAX_HEALTH);
      syncCapabilityFlags(npc, analyzeMagicCapabilities(vars));
   }

   public static void configureLeff(MysticMagicianEntity npc) {
      if (npc == null || npc.level().isClientSide()) return;
      TypeMoonWorldModVariables.PlayerVariables vars = npc.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      applyFixedLeffProfile(npc, vars, true);
   }

   public static void ensureLeffProfile(MysticMagicianEntity npc) {
      if (npc == null || npc.level().isClientSide()) return;
      TypeMoonWorldModVariables.PlayerVariables vars = npc.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      applyFixedLeffProfile(npc, vars, false);
   }

   private static void applyFixedLeffProfile(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, boolean refillMana) {
      if (npc == null || vars == null) return;
      vars.ensureMagicSystemInitialized();
      vars.clearAllWheelSlots();
      vars.learned_magics.clear();
      vars.magic_proficiencies.clear();
      vars.crest_entries.clear();
      vars.crest_practice_count.clear();
      vars.selected_magics.clear();
      vars.selected_magic_runtime_slot_indices.clear();
      vars.selected_magic_display_names.clear();
      vars.current_magic_index = 0;
      vars.active_wheel_index = 0;
      vars.player_max_mana = 800.0;
      vars.player_mana = refillMana ? 800.0 : Mth.clamp(vars.player_mana, 0.0, 800.0);
      vars.player_mana_egenerated_every_moment = 5.0;
      vars.player_restore_magic_moment = 10.0;
      vars.is_magus = true;
      vars.player_magic_attributes_earth = false;
      vars.player_magic_attributes_water = false;
      vars.player_magic_attributes_fire = false;
      vars.player_magic_attributes_wind = true;
      vars.player_magic_attributes_ether = false;
      vars.player_magic_attributes_none = false;
      vars.player_magic_attributes_imaginary_number = true;
      vars.player_magic_attributes_sword = false;
      vars.bajiquan_learned = false;
      vars.bajiquan_proficiency = 0.0;
      vars.bajiquan_tiger_unlocked = false;
      vars.ganryu_learned = false;
      vars.ganryu_proficiency = 0.0;
      vars.ganryu_tsubame_unlocked = false;
      vars.hokushin_learned = false;
      vars.hokushin_proficiency = 0.0;
      vars.hokushin_master_defeated = false;
      vars.tennen_learned = false;
      vars.tennen_proficiency = 0.0;
      vars.tennen_master_defeated = false;
      for (String magicId : LEFF_FIXED_MAGICS) {
         setNpcFixedMagic(vars, magicId, leffFixedProficiency(magicId));
      }
      for (int i = 0; i < LEFF_FIXED_MAGICS.length; i++) {
         installLeffWheelMagic(vars, i, LEFF_FIXED_MAGICS[i]);
      }
      vars.rebuildSelectedMagicsFromActiveWheel();
      CompoundTag data = npc.getPersistentData();
      data.putBoolean(TAG_MAGIC_INIT, true);
      data.putBoolean(TAG_ATTR_INIT, true);
      data.putInt(TAG_FIXED_LEVEL, 5);
      data.putDouble(TAG_BASE_MAX_HEALTH, LeffLaynorFlaurosEntity.MAX_HEALTH);
      data.putDouble(TAG_BASE_MOVE_SPEED, 0.28);
      data.putDouble(TAG_COMBAT_MOVE_SPEED, 0.42);
      data.putDouble(TAG_BASE_ATTACK_DAMAGE, 5.0);
      data.putInt(NPC_JEWEL_ITEM_BASIC, 0);
      data.putInt(NPC_JEWEL_ITEM_ADVANCED, 0);
      data.putInt(NPC_JEWEL_ITEM_ENGRAVED, 0);
      if (npc.getAttribute(Attributes.MAX_HEALTH) != null) npc.getAttribute(Attributes.MAX_HEALTH).setBaseValue(LeffLaynorFlaurosEntity.MAX_HEALTH);
      if (npc.getAttribute(Attributes.MOVEMENT_SPEED) != null) npc.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.28);
      if (npc.getAttribute(Attributes.ATTACK_DAMAGE) != null) npc.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0);
      npc.setMartialMask(0);
      npc.setHasThompson(false);
      npc.setRangedWeaponMode(false);
      grantLeffMagicCrest(npc, vars);
      syncCapabilityFlags(npc, analyzeMagicCapabilities(vars));
   }

   /** Keeps the fixed NPC profile intact for Rin entities loaded from older saves. */
   public static void ensureTohsakaRinBajiquan(MysticMagicianEntity npc) {
      if (npc == null || npc.level().isClientSide()) return;
      TypeMoonWorldModVariables.PlayerVariables vars = npc.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.bajiquan_learned = true;
      vars.bajiquan_proficiency = 60.0;
      vars.bajiquan_tiger_unlocked = false;
      if (!vars.learned_magics.contains("bajiquan")) vars.learned_magics.add("bajiquan");
      setNpcKnownMagic(vars, "aerial_stasis", 100.0);
      setNpcKnownMagic(vars, "aerial_ascent", 100.0);
      setNpcKnownMagic(vars, "suggestion_magic", 60.0);
      setNpcKnownMagic(vars, "healing_magic", 60.0);
      ensureTohsakaRinWheelMagic(vars, "aerial_stasis");
      ensureTohsakaRinWheelMagic(vars, "aerial_ascent");
      ensureTohsakaRinWheelMagic(vars, "suggestion_magic");
      ensureTohsakaRinWheelMagic(vars, "healing_magic");
      grantTohsakaRinMagicCrest(npc, vars);
   }

   private static void setNpcKnownMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, double proficiency) {
      if (vars == null || magicId == null || magicId.isEmpty()) return;
      if (!vars.learned_magics.contains(magicId)) vars.learned_magics.add(magicId);
      if (MagicProficiencyService.get(vars, magicId) < proficiency) {
         MagicProficiencyService.set(vars, magicId, proficiency);
      }
   }

   private static void setNpcFixedMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, double proficiency) {
      if (vars == null || magicId == null || magicId.isEmpty()) return;
      if (!vars.learned_magics.contains(magicId)) vars.learned_magics.add(magicId);
      MagicProficiencyService.set(vars, magicId, proficiency);
   }

   private static double leffFixedProficiency(String magicId) {
      return switch (magicId) {
         case "aerial_stasis", "aerial_ascent" -> 100.0;
         case "magic_analysis", "detection", "spiritron_cannon" -> 90.0;
         case "airflow_blade", "imaginary_displacement", "imaginary_space" -> 85.0;
         case "suggestion_magic", "reinforcement" -> 70.0;
         default -> 0.0;
      };
   }

   private static void installLeffWheelMagic(TypeMoonWorldModVariables.PlayerVariables vars, int slot, String magicId) {
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(0, slot);
      entry.magicId = magicId;
      entry.sourceType = "self";
      if ("reinforcement".equals(magicId)) {
         entry.presetPayload.putInt("reinforcement_target", 0);
         entry.presetPayload.putInt("reinforcement_mode", 0);
         entry.presetPayload.putInt("reinforcement_level", 4);
      }
      vars.setWheelSlotEntry(0, slot, entry);
   }

   private static void ensureTohsakaRinWheelMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (vars == null || magicId == null || magicId.isEmpty()) return;
      vars.ensureMagicSystemInitialized();
      for (int slot = 0; slot < 12; slot++) {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry existing = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         if (existing != null && magicId.equals(existing.magicId)) return;
      }
      for (int slot = 0; slot < 12; slot++) {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry existing = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         if (existing == null || existing.isEmpty()) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry =
               new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(vars.active_wheel_index, slot);
            entry.magicId = magicId;
            entry.sourceType = "self";
            vars.setWheelSlotEntry(vars.active_wheel_index, slot, entry);
            vars.rebuildSelectedMagicsFromActiveWheel();
            return;
         }
      }
   }

   private static void grantTohsakaRinMagicCrest(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (npc == null || vars == null) return;
      vars.ensureMagicSystemInitialized();
      boolean changed = false;
      if (vars.magicCrestInventory.getStackInSlot(0).isEmpty()) {
         vars.magicCrestInventory.setStackInSlot(0, new ItemStack(ModItems.MAGIC_CREST.get()));
         changed = true;
      }
      changed |= addTohsakaRinCrestEntry(npc, vars, "aerial_stasis");
      changed |= addTohsakaRinCrestEntry(npc, vars, "aerial_ascent");
      changed |= addTohsakaRinCrestEntry(npc, vars, "suggestion_magic");
      changed |= addTohsakaRinCrestEntry(npc, vars, "reinforcement");
      changed |= addTohsakaRinCrestEntry(npc, vars, "healing_magic");
      if (changed) {
         TypeMoonWorldModVariables.PlayerVariables.writeCrestEntriesToStack(vars.magicCrestInventory.getStackInSlot(0), vars.crest_entries);
      }
   }

   private static boolean addTohsakaRinCrestEntry(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry existing : vars.crest_entries) {
         if (existing != null && magicId.equals(existing.magicId) && "tohsaka_rin".equals(existing.originOwnerType)) return false;
      }
      TypeMoonWorldModVariables.PlayerVariables.CrestEntry entry = new TypeMoonWorldModVariables.PlayerVariables.CrestEntry();
      entry.entryId = UUID.randomUUID().toString();
      entry.magicId = magicId;
      entry.presetPayload = new CompoundTag();
      entry.sourceKind = "plunder";
      entry.originOwnerUuid = npc.getUUID().toString();
      entry.originOwnerType = "tohsaka_rin";
      entry.originOwnerName = npc.getName().getString();
      entry.active = true;
      vars.crest_entries.add(entry);
      return true;
   }

   private static void grantLeffMagicCrest(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (npc == null || vars == null) return;
      vars.ensureMagicSystemInitialized();
      boolean changed = false;
      if (vars.magicCrestInventory.getStackInSlot(0).isEmpty()) {
         vars.magicCrestInventory.setStackInSlot(0, new ItemStack(ModItems.MAGIC_CREST.get()));
         changed = true;
      }
      changed |= addLeffCrestEntry(npc, vars, "aerial_stasis");
      changed |= addLeffCrestEntry(npc, vars, "aerial_ascent");
      changed |= addLeffCrestEntry(npc, vars, "magic_analysis");
      changed |= addLeffCrestEntry(npc, vars, "airflow_blade");
      changed |= addLeffCrestEntry(npc, vars, "suggestion_magic");
      changed |= addLeffCrestEntry(npc, vars, "reinforcement");
      changed |= addLeffCrestEntry(npc, vars, "detection");
      changed |= addLeffCrestEntry(npc, vars, "imaginary_displacement");
      changed |= addLeffCrestEntry(npc, vars, "imaginary_space");
      changed |= addLeffCrestEntry(npc, vars, "spiritron_cannon");
      if (changed) {
         TypeMoonWorldModVariables.PlayerVariables.writeCrestEntriesToStack(vars.magicCrestInventory.getStackInSlot(0), vars.crest_entries);
      }
   }

   private static boolean addLeffCrestEntry(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry existing : vars.crest_entries) {
         if (existing != null && magicId.equals(existing.magicId) && "leff_laynor_flauros".equals(existing.originOwnerType)) return false;
      }
      TypeMoonWorldModVariables.PlayerVariables.CrestEntry entry = new TypeMoonWorldModVariables.PlayerVariables.CrestEntry();
      entry.entryId = "leff_laynor_flauros:" + magicId;
      entry.magicId = magicId;
      entry.presetPayload = new CompoundTag();
      entry.sourceKind = "plunder";
      entry.originOwnerUuid = npc.getUUID().toString();
      entry.originOwnerType = "leff_laynor_flauros";
      entry.originOwnerName = npc.getName().getString();
      entry.active = true;
      vars.crest_entries.add(entry);
      return true;
   }

   public static void cleanup(MysticMagicianEntity npc) {
      if (npc != null) {
         clearNpcCombatSelfBuffs(npc);
         CompoundTag data = npc.getPersistentData();
         data.remove(TAG_LAST_DECISION_TICK);
         data.remove(TAG_LAST_COMBAT_TICK);
         data.remove(TAG_NEXT_GLOBAL_CAST_TICK);
         data.remove(TAG_MAGIC_CD);
         data.remove(TAG_CAST_LOCK_UNTIL);
         data.remove(TAG_RESOURCE_USE);
         data.remove(TAG_LAST_CAST_MAGIC);
         data.remove(TAG_CAST_REPEAT_COUNT);
         data.remove(TAG_RETREAT_COUNTER_TICK);
         data.remove(TAG_RETREAT_UTILITY_TICK);
         data.remove(TAG_THREAT_UTILITY_TICK);
         data.remove(TAG_THREAT_CONTROL_TICK);
         data.remove(TAG_THREAT_COUNTER_TICK);
         data.remove(TAG_ENV_UTILITY_TICK);
         data.remove(TAG_ENV_MOVE_TICK);
         data.remove(TAG_CAP_HAS_RANGED);
         data.remove(TAG_CAP_HAS_MELEE_BURST);
         data.remove(TAG_CAP_ONLY_MELEE);
         data.remove(TAG_REINF_BODY_CD);
         data.remove(TAG_REINF_HAND_CD);
         data.remove(TAG_REINF_LEG_CD);
         data.remove(TAG_REINF_EYE_CD);
         data.remove(TAG_MELEE_PUNCH_CD);
         data.remove(TAG_MELEE_KICK_CD);
         data.remove(TAG_MELEE_THROW_CD);
         data.remove(TAG_MELEE_SLAM_CD);
         data.remove(TAG_PENDING_MG_MAGIC);
         data.remove(TAG_PENDING_MG_TARGET);
         data.remove(TAG_PENDING_MG_NEXT_TICK);
         data.remove(TAG_PENDING_MG_WAVES_LEFT);
         data.remove(TAG_PENDING_MG_INTERVAL);
         data.remove(TAG_PENDING_MG_MODE);
         data.remove(TAG_PENDING_MG_CHARGE);
         data.remove(TAG_PENDING_MG_SHOT_COUNT);
         data.remove(TAG_PENDING_MG_BLOCKED_RETRIES);
         data.remove(TAG_JEWEL_ITEM_NEXT_TICK);
      }
   }

   public static boolean grantTemporaryAnalyzedMagic(
      MysticMagicianEntity npc,
      String magicId,
      double analysisProficiency,
      long gameTime
   ) {
      if (npc == null || npc.level().isClientSide() || magicId == null || magicId.isEmpty()) {
         return false;
      }
      if ("magic_analysis".equals(magicId)
         || !NpcMagicFilterService.isMagicAllowedForNpc(magicId)
         || !MagicLearningStrategy.learningRequirementsMet(npc.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES), magicId)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = npc.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ensureMagicSystemInitialized();
      boolean alreadyKnown = vars.learned_magics.contains(magicId);
      if (!alreadyKnown) {
         vars.learned_magics.add(magicId);
      }
      double complexity = MagicLearningStrategy.complexity(magicId);
      double temporaryProficiency = Mth.clamp(28.0 + analysisProficiency * 0.55 - complexity * 0.12, 12.0, 88.0);
      MagicProficiencyService.set(vars, magicId, Math.max(MagicProficiencyService.get(vars, magicId), temporaryProficiency));

      int slot = findTemporaryAnalysisSlot(vars, magicId);
      if (slot < 0) {
         slot = findEmptyWheelSlot(vars);
      }
      if (slot < 0) {
         slot = findReplaceableWheelSlot(vars);
      }
      if (slot < 0) {
         return false;
      }

      CompoundTag payload = NpcMagicFilterService.sanitizePresetForNpc(
         magicId,
         NpcMagicFilterService.buildRandomPresetForMagic(magicId, npc.registryAccess(), npc.getRandom()),
         npc.registryAccess(),
         npc.getRandom()
      );
      payload.putBoolean(TAG_ANALYSIS_TEMPORARY, true);
      payload.putLong(TAG_ANALYSIS_EXPIRES, gameTime + temporaryAnalysisDurationTicks(analysisProficiency, complexity));
      payload.putBoolean(TAG_ANALYSIS_ALREADY_KNOWN, alreadyKnown);

      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry =
         new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(vars.active_wheel_index, slot);
      entry.magicId = magicId;
      entry.sourceType = "analysis";
      entry.presetPayload = payload;
      entry.displayNameCache = magicId + " [analysis]";
      vars.setWheelSlotEntry(vars.active_wheel_index, slot, entry);
      vars.rebuildSelectedMagicsFromActiveWheel();
      npc.getPersistentData().remove(TAG_CAP_CACHE_TICK);
      syncCapabilityFlags(npc, analyzeMagicCapabilities(vars));
      return true;
   }

   private static int findTemporaryAnalysisSlot(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      for (int slot = 0; slot < 12; slot++) {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         if (entry != null && magicId.equals(entry.magicId) && isTemporaryAnalysisEntry(entry)) {
            return slot;
         }
      }
      return -1;
   }

   private static int findEmptyWheelSlot(TypeMoonWorldModVariables.PlayerVariables vars) {
      for (int slot = 0; slot < 12; slot++) {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         if (entry == null || entry.isEmpty()) {
            return slot;
         }
      }
      return -1;
   }

   private static int findReplaceableWheelSlot(TypeMoonWorldModVariables.PlayerVariables vars) {
      for (int slot = 11; slot >= 0; slot--) {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         if (entry != null && isTemporaryAnalysisEntry(entry)) {
            return slot;
         }
      }
      return -1;
   }

   private static long temporaryAnalysisDurationTicks(double analysisProficiency, double complexity) {
      return (long)Mth.clamp(240.0 + analysisProficiency * 4.0 - complexity * 1.5, 120.0, 600.0);
   }

   private static boolean isTemporaryAnalysisEntry(TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry) {
      return entry != null
         && entry.presetPayload != null
         && "analysis".equals(entry.sourceType)
         && entry.presetPayload.getBoolean(TAG_ANALYSIS_TEMPORARY);
   }

   private static void cleanupTemporaryAnalyzedMagics(
      MysticMagicianEntity npc,
      TypeMoonWorldModVariables.PlayerVariables vars,
      long gameTime,
      boolean force
   ) {
      if (npc == null || vars == null) {
         return;
      }
      boolean changed = false;
      Set<String> removable = new HashSet<>();
      for (int slot = 0; slot < 12; slot++) {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         if (!isTemporaryAnalysisEntry(entry)) {
            continue;
         }
         long expires = entry.presetPayload.getLong(TAG_ANALYSIS_EXPIRES);
         if (force || expires <= gameTime) {
            if (!entry.presetPayload.getBoolean(TAG_ANALYSIS_ALREADY_KNOWN) && entry.magicId != null && !entry.magicId.isEmpty()) {
               removable.add(entry.magicId);
            }
            vars.setWheelSlotEntry(vars.active_wheel_index, slot, new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(vars.active_wheel_index, slot));
            changed = true;
         }
      }
      if (changed) {
         for (String magicId : removable) {
            if (!hasWheelMagic(vars, magicId)) {
               vars.learned_magics.remove(magicId);
            }
         }
         vars.rebuildSelectedMagicsFromActiveWheel();
         npc.getPersistentData().remove(TAG_CAP_CACHE_TICK);
         syncCapabilityFlags(npc, analyzeMagicCapabilities(vars));
      }
      if (force) {
         vars.magic_analysis_active = false;
      }
   }

   private static boolean hasWheelMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      for (int slot = 0; slot < 12; slot++) {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         if (entry != null && magicId.equals(entry.magicId)) {
            return true;
         }
      }
      return false;
   }

   private static void initializeIfNeeded(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (npc instanceof LeffLaynorFlaurosEntity) {
         ensureLeffProfile(npc);
         return;
      }
      CompoundTag data = npc.getPersistentData();
      ensureCombatAttributesInitialized(npc, data, npc.getRandom());
      if (!data.getBoolean(TAG_MIGRATION_CLEANED_V3)) {
         boolean legacy = data.getBoolean("TypeMoonNpcMagicInitV1");
         int purged = purgeDeprecatedProjectionChain(vars, data);
         data.putBoolean(TAG_MIGRATION_CLEANED_V3, true);
         if (legacy || purged > 0) {
            TYPE_MOON_WORLD.LOGGER.info(
               "Migrated Mystic Magician {} to V3. Purged projection-chain entries={}",
               npc.getUUID(),
               purged
            );
         }
      }

      if (!data.getBoolean(TAG_MAGIC_INIT)) {
         RandomSource random = npc.getRandom();
         vars.ensureMagicSystemInitialized();
         vars.clearAllWheelSlots();
         vars.learned_magics.clear();
         vars.crest_entries.clear();
         vars.crest_practice_count.clear();
         vars.selected_magics.clear();
         vars.selected_magic_runtime_slot_indices.clear();
         vars.selected_magic_display_names.clear();
         vars.current_magic_index = 0;
         vars.active_wheel_index = 0;
         int fixedLevel = Mth.clamp(data.contains(TAG_FIXED_LEVEL) ? data.getInt(TAG_FIXED_LEVEL) : Mth.nextInt(random, LEVEL_MIN, LEVEL_MAX), LEVEL_MIN, LEVEL_MAX);
         data.putInt(TAG_FIXED_LEVEL, fixedLevel);
         data.putInt(TAG_COMBAT_LEVEL_BONUS, 0);
         MysticMagicianRank rank = npc.getMagicianRank();
         randomizeBaseStats(vars, random, rank);
         randomizeMagicAttributes(vars, random);
         seedSelfKnowledge(npc, vars, random, fixedLevel, rank);
         seedCrestKnowledge(npc, vars, random);
         buildWheelEntries(npc, vars, random);
         vars.rebuildSelectedMagicsFromActiveWheel();
         vars.current_magic_index = 0;
         vars.is_magus = true;
         vars.is_magic_circuit_open = false;
         vars.magic_circuit_open_timer = 0.0;
         vars.magic_cooldown = 0.0;
         npc.setCombatStyle(NpcCombatStyle.fromAttributes(vars));
         data.putBoolean(TAG_MAGIC_INIT, true);
         data.putLong(TAG_LAST_DECISION_TICK, 0L);
         data.putLong(TAG_LAST_COMBAT_TICK, npc.level().getGameTime());
         data.putLong(TAG_NEXT_GLOBAL_CAST_TICK, 0L);
         data.putLong(TAG_CAST_LOCK_UNTIL, 0L);
         data.putLong(TAG_RETREAT_COUNTER_TICK, 0L);
         data.putLong(TAG_RETREAT_UTILITY_TICK, 0L);
         data.putLong(TAG_THREAT_UTILITY_TICK, 0L);
         data.putLong(TAG_THREAT_CONTROL_TICK, 0L);
         data.putLong(TAG_THREAT_COUNTER_TICK, 0L);
         data.putLong(TAG_ENV_UTILITY_TICK, 0L);
         data.putLong(TAG_ENV_MOVE_TICK, 0L);
      }
   }

   private static void ensureCombatAttributesInitialized(MysticMagicianEntity npc, CompoundTag data, RandomSource random) {
      if (npc != null && data != null) {
         boolean initialized = data.getBoolean(TAG_ATTR_INIT);
         double baseHealth = data.contains(TAG_BASE_MAX_HEALTH) ? data.getDouble(TAG_BASE_MAX_HEALTH) : randomZombieComparableStat(random, 20.0);
         double baseSpeed = data.contains(TAG_BASE_MOVE_SPEED) ? data.getDouble(TAG_BASE_MOVE_SPEED) : randomZombieComparableStat(random, 0.23);
         double baseAttack = data.contains(TAG_BASE_ATTACK_DAMAGE)
            ? data.getDouble(TAG_BASE_ATTACK_DAMAGE)
            : randomZombieComparableStat(random, 3.0);
         double combatSpeed = data.contains(TAG_COMBAT_MOVE_SPEED) ? data.getDouble(TAG_COMBAT_MOVE_SPEED) : round3(baseSpeed * 1.45);
         baseHealth = npc instanceof TohsakaRinEntity
            ? TohsakaRinEntity.MAX_HEALTH
            : clampZombieComparableStat(baseHealth, 20.0);
         baseSpeed = clampZombieComparableStat(baseSpeed, 0.23);
         baseAttack = clampZombieComparableStat(baseAttack, 3.0);
         combatSpeed = clampCombatMoveSpeed(combatSpeed, baseSpeed);
         AttributeInstance healthAttr = npc.getAttribute(Attributes.MAX_HEALTH);
         if (healthAttr != null) {
            healthAttr.setBaseValue(baseHealth);
         }

         AttributeInstance speedAttr = npc.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speedAttr != null) {
            speedAttr.setBaseValue(baseSpeed);
         }

         AttributeInstance attackAttr = npc.getAttribute(Attributes.ATTACK_DAMAGE);
         if (attackAttr != null) {
            attackAttr.setBaseValue(baseAttack);
         }

         if (!initialized) {
            data.putDouble(TAG_BASE_MAX_HEALTH, baseHealth);
            data.putDouble(TAG_BASE_MOVE_SPEED, baseSpeed);
            data.putDouble(TAG_BASE_ATTACK_DAMAGE, baseAttack);
            data.putDouble(TAG_COMBAT_MOVE_SPEED, combatSpeed);
            data.putBoolean(TAG_ATTR_INIT, true);
            if (healthAttr != null) {
               npc.setHealth((float)healthAttr.getBaseValue());
            }
         } else if (healthAttr != null) {
            data.putDouble(TAG_BASE_MAX_HEALTH, baseHealth);
            data.putDouble(TAG_BASE_MOVE_SPEED, baseSpeed);
            data.putDouble(TAG_BASE_ATTACK_DAMAGE, baseAttack);
            data.putDouble(TAG_COMBAT_MOVE_SPEED, combatSpeed);
            float clamped = Mth.clamp(npc.getHealth(), 1.0F, (float)healthAttr.getBaseValue());
            if (Math.abs(clamped - npc.getHealth()) > 0.01F) {
               npc.setHealth(clamped);
            }
         }
      }
   }

   private static void applyCombatMovementState(MysticMagicianEntity npc, boolean inCombat) {
      if (npc != null) {
         CompoundTag data = npc.getPersistentData();
         double baseSpeed = data.contains(TAG_BASE_MOVE_SPEED) ? data.getDouble(TAG_BASE_MOVE_SPEED) : 0.23;
         double combatSpeed = data.contains(TAG_COMBAT_MOVE_SPEED) ? data.getDouble(TAG_COMBAT_MOVE_SPEED) : round3(baseSpeed * 1.45);
         double targetSpeed = inCombat ? combatSpeed : baseSpeed;
         AttributeInstance speedAttr = npc.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speedAttr != null) {
            if (Math.abs(speedAttr.getBaseValue() - targetSpeed) > 1.0E-4) {
               speedAttr.setBaseValue(targetSpeed);
            }
         }
      }
   }

   private static void randomizeBaseStats(
      TypeMoonWorldModVariables.PlayerVariables vars, RandomSource random, MysticMagicianRank rank
   ) {
      vars.is_magus = true;
      MysticMagicianRank resolvedRank = rank == null ? MysticMagicianRank.ADEPT : rank;
      vars.player_max_mana = round1(resolvedRank.rollMana(random));
      vars.player_mana = vars.player_max_mana;
      vars.player_mana_egenerated_every_moment = round1(1.0 + random.nextDouble() * 9.0);
      vars.player_restore_magic_moment = round1(1.0 + random.nextDouble() * 9.0);
      vars.current_mana_regen_multiplier = 1.0;
   }

   private static void randomizeMagicAttributes(TypeMoonWorldModVariables.PlayerVariables vars, RandomSource random) {
      vars.player_magic_attributes_earth = false;
      vars.player_magic_attributes_water = false;
      vars.player_magic_attributes_fire = false;
      vars.player_magic_attributes_wind = false;
      vars.player_magic_attributes_ether = false;
      vars.player_magic_attributes_none = false;
      vars.player_magic_attributes_imaginary_number = false;
      vars.player_magic_attributes_sword = false;
      boolean special = false;
      if (random.nextInt(100) < 10) {
         if (random.nextBoolean()) {
            vars.player_magic_attributes_none = true;
         } else {
            vars.player_magic_attributes_imaginary_number = true;
         }

         special = true;
      }

      if (!special) {
         int roll = random.nextInt(100);
         int count = 1;
         if (roll >= 99) {
            count = 5;
         } else if (roll >= 95) {
            count = 4;
         } else if (roll >= 80) {
            count = 3;
         } else if (roll >= 50) {
            count = 2;
         }

         List<Integer> attrs = new ArrayList<>(List.of(0, 1, 2, 3, 4));
         Collections.shuffle(attrs, new Random(random.nextLong()));

         for (int i = 0; i < count; i++) {
            switch (attrs.get(i)) {
               case 0:
                  vars.player_magic_attributes_earth = true;
                  break;
               case 1:
                  vars.player_magic_attributes_water = true;
                  break;
               case 2:
                  vars.player_magic_attributes_fire = true;
                  break;
               case 3:
                  vars.player_magic_attributes_wind = true;
                  break;
               case 4:
                  vars.player_magic_attributes_ether = true;
            }
         }
      }
   }

   private static void seedSelfKnowledge(
      MysticMagicianEntity npc,
      TypeMoonWorldModVariables.PlayerVariables vars,
      RandomSource random,
      int fixedLevel,
      MysticMagicianRank rank
   ) {
      MysticMagicianRank resolvedRank = rank == null ? MysticMagicianRank.ADEPT : rank;
      List<String> pool = new ArrayList<>();

      for (String id : NpcMagicFilterService.candidateMagicPool()) {
         if (NpcMagicFilterService.isMagicAllowedForNpc(id)) {
            pool.add(id);
         }
      }

      if (!pool.isEmpty()) {
         List<String> orderedPool = prioritizeMagicPool(pool, resolvedRank, npc.getBrandColor(), random);
         int selfCount = Math.min(resolvedRank.rollMagicCount(random), orderedPool.size());
         boolean reinforcementPicked = false;
         boolean reinforcementInPool = orderedPool.remove("reinforcement");
         // 90% have reinforcement; 10% get ranged/control compensation and avoid melee preference.
         if (reinforcementInPool && random.nextFloat() < (resolvedRank.prefersAdvancedMagic() ? 0.75F : 0.9F)) {
            vars.learned_magics.add("reinforcement");
            seedSelfProficiency(vars, "reinforcement", random, resolvedRank, fixedLevel, false);
            reinforcementPicked = true;
         }

         int specialtyCount = resolvedRank == MysticMagicianRank.FES ? Mth.nextInt(random, 1, 2) : 0;
         int selectedCount = 0;
         for (String magicId : orderedPool) {
            if (selectedCount >= selfCount) {
               break;
            }
            if ("reinforcement".equals(magicId) || vars.learned_magics.contains(magicId)) {
               continue;
            }
            boolean specialty = specialtyCount > 0 && isFesSpecialty(magicId);
            vars.learned_magics.add(magicId);
            ensurePrerequisites(vars, magicId);
            seedSelfProficiency(vars, magicId, random, resolvedRank, fixedLevel, specialty);
            if (specialty) {
               specialtyCount--;
            }
            selectedCount++;
         }

         if (!reinforcementPicked) {
            String[] compensation = resolvedRank.prefersAdvancedMagic()
               ? new String[]{"gandr_machine_gun", "jewel_machine_gun", "healing_magic", "gravity_magic", "suggestion_magic", "binding_magic", "gander"}
               : new String[]{"gander", "magic_bullet", "fire_magic", "water_magic", "reinforcement"};
            int compensationCount = resolvedRank == MysticMagicianRank.FRAME || resolvedRank == MysticMagicianRank.UMNOS ? 1 : 2;
            int added = 0;

            for (String extra : compensation) {
               if (NpcMagicFilterService.isMagicAllowedForNpc(extra) && !vars.learned_magics.contains(extra)) {
                  vars.learned_magics.add(extra);
                  ensurePrerequisites(vars, extra);
                  seedSelfProficiency(vars, extra, random, resolvedRank, fixedLevel, false);
                  added++;
                  if (added >= compensationCount) {
                     break;
                  }
               }
            }
         }

         if (!vars.learned_magics.contains("gander")) {
            vars.learned_magics.add("gander");
            vars.proficiency_gander = Math.max(vars.proficiency_gander, resolvedRank == MysticMagicianRank.FRAME ? 20.0 : 35.0);
         }
      }
   }

   private static List<String> prioritizeMagicPool(
      List<String> source,
      MysticMagicianRank rank,
      MysticMagicianRank.BrandColor brandColor,
      RandomSource random
   ) {
      List<String> shuffled = new ArrayList<>(source);
      Collections.shuffle(shuffled, new Random(random.nextLong()));
      if (!rank.prefersAdvancedMagic()) {
         return shuffled;
      }

      List<String> preferred = new ArrayList<>();
      String[] advanced = new String[]{
         "gandr_machine_gun", "jewel_machine_gun", "jewel_random_shoot",
         "healing_magic", "gravity_magic", "binding_magic", "suggestion_magic",
         "mana_burst", "reinforcement"
      };
      for (String id : advanced) {
         if (shuffled.remove(id)) {
            preferred.add(id);
         }
      }
      if (rank == MysticMagicianRank.BRAND) {
         String brandPreferred = switch (brandColor == null ? MysticMagicianRank.BrandColor.RED : brandColor) {
            case RED -> "fire_magic";
            case BLUE -> "water_magic";
            case YELLOW -> "earth_magic";
            case ORANGE -> "jewel_random_shoot";
            case PURPLE -> "suggestion_magic";
            case GREEN -> "healing_magic";
            case BLACK -> "gravity_magic";
         };
         if (shuffled.remove(brandPreferred)) {
            preferred.add(0, brandPreferred);
         }
      }
      preferred.addAll(shuffled);
      return preferred;
   }

   private static boolean isFesSpecialty(String magicId) {
      return switch (magicId) {
         case "gandr_machine_gun", "jewel_machine_gun", "healing_magic",
            "gravity_magic", "binding_magic", "suggestion_magic",
            "ruby_flame_sword", "sapphire_winter_frost", "emerald_winter_river",
            "topaz_reinforcement", "cyan_wind", "mana_burst" -> true;
         default -> false;
      };
   }

   private static void seedCrestKnowledge(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, RandomSource random) {
      boolean hasCrest = random.nextBoolean();
      if (!hasCrest) {
         vars.magicCrestInventory.setStackInSlot(0, ItemStack.EMPTY);
      } else {
         vars.magicCrestInventory.setStackInSlot(0, new ItemStack(ModItems.MAGIC_CREST.get()));
         int crestCount = Mth.nextInt(random, 1, 4);
         Provider lookup = npc.registryAccess();
         List<String> pool = new ArrayList<>();

         for (String id : NpcMagicFilterService.candidateMagicPool()) {
            if (NpcMagicFilterService.isMagicAllowedForNpc(id)) {
               pool.add(id);
            }
         }

         if (!pool.isEmpty()) {
            for (int i = 0; i < crestCount; i++) {
               String magicId = pool.get(random.nextInt(pool.size()));
               CompoundTag payload = NpcMagicFilterService.buildRandomPresetForMagic(magicId, lookup, random);
               payload = NpcMagicFilterService.sanitizePresetForNpc(magicId, payload, lookup, random);
               if (NpcMagicFilterService.isPresetValidForNpc(magicId, payload, lookup)) {
                  TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry = new TypeMoonWorldModVariables.PlayerVariables.CrestEntry();
                  crestEntry.entryId = UUID.randomUUID().toString();
                  crestEntry.magicId = magicId;
                  crestEntry.presetPayload = payload.copy();
                  crestEntry.sourceKind = "plunder";
                  crestEntry.originOwnerUuid = npc.getUUID().toString();
                  crestEntry.originOwnerType = "npc";
                  crestEntry.originOwnerName = npc.getName().getString();
                  crestEntry.active = true;
                  vars.crest_entries.add(crestEntry);
                  ensurePrerequisites(vars, magicId);
               }
            }
         }
      }
   }

   private static void buildWheelEntries(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, RandomSource random) {
      vars.clearAllWheelSlots();
      vars.active_wheel_index = 0;
      Provider lookup = npc.registryAccess();
      List<NpcMagicCastBridge.SlotSeed> seeds = new ArrayList<>();
      Set<String> selfAdded = new HashSet<>();
      int fixedLevel = getFixedLevel(npc);

      for (String learned : vars.learned_magics) {
         if (VALID_MAGIC_IDS.contains(learned) && !selfAdded.contains(learned)) {
            if ("reinforcement".equals(learned)) {
               for (int mode = 0; mode < 4; mode++) {
                  CompoundTag payload = new CompoundTag();
                  payload.putInt("reinforcement_target", 0);
                  payload.putInt("reinforcement_mode", mode);
                  payload.putInt("reinforcement_level", Mth.clamp(fixedLevel + (mode == 0 ? 1 : 0), 1, 5));
                  seeds.add(new NpcMagicCastBridge.SlotSeed("self", learned, payload, ""));
               }
            } else {
               CompoundTag payload = NpcMagicFilterService.sanitizePresetForNpc(
                  learned, NpcMagicFilterService.buildRandomPresetForMagic(learned, lookup, random), lookup, random
               );
               if (!NpcMagicFilterService.isPresetValidForNpc(learned, payload, lookup)) {
                  payload = new CompoundTag();
               }

               seeds.add(new NpcMagicCastBridge.SlotSeed("self", learned, payload, ""));
            }

            selfAdded.add(learned);
         }
      }

      for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry : vars.crest_entries) {
         if (crestEntry != null && crestEntry.active && VALID_MAGIC_IDS.contains(crestEntry.magicId)) {
            CompoundTag payload = NpcMagicFilterService.sanitizePresetForNpc(crestEntry.magicId, crestEntry.presetPayload, lookup, random);
            if (NpcMagicFilterService.isPresetValidForNpc(crestEntry.magicId, payload, lookup)) {
               crestEntry.presetPayload = payload.copy();
               seeds.add(new NpcMagicCastBridge.SlotSeed("crest", crestEntry.magicId, payload, crestEntry.entryId));
            }
         }
      }

      if (seeds.isEmpty()) {
         seeds.add(new NpcMagicCastBridge.SlotSeed("self", "gander", new CompoundTag(), ""));
         if (!vars.learned_magics.contains("gander")) {
            vars.learned_magics.add("gander");
         }
      }

      List<NpcMagicCastBridge.SlotSeed> ordered = new ArrayList<>();
      List<NpcMagicCastBridge.SlotSeed> remainder = new ArrayList<>();
      for (NpcMagicCastBridge.SlotSeed seed : seeds) {
         if ("reinforcement".equals(seed.magicId())) {
            ordered.add(seed);
         } else {
            remainder.add(seed);
         }
      }

      Collections.shuffle(remainder, new Random(random.nextLong()));
      ordered.addAll(remainder);
      int slots = Math.min(12, ordered.size());

      for (int i = 0; i < slots; i++) {
         NpcMagicCastBridge.SlotSeed seed = ordered.get(i);
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(0, i);
         entry.sourceType = seed.sourceType();
         entry.magicId = seed.magicId();
         entry.presetPayload = seed.payload().copy();
         entry.crestEntryId = seed.crestEntryId();
         entry.displayNameCache = seed.sourceType().equals("crest") ? seed.magicId() + " [crest]" : seed.magicId();
         vars.setWheelSlotEntry(0, i, entry);
      }

      vars.switchActiveWheel(0);
      vars.rebuildSelectedMagicsFromActiveWheel();
   }

   private static void seedSelfProficiency(
      TypeMoonWorldModVariables.PlayerVariables vars,
      String magicId,
      RandomSource random,
      MysticMagicianRank rank,
      int fixedLevel,
      boolean specialty
   ) {
      MysticMagicianRank resolvedRank = rank == null ? MysticMagicianRank.ADEPT : rank;
      double min = specialty ? resolvedRank.minSpecialtyProficiency() : resolvedRank.minProficiency();
      double max = specialty ? resolvedRank.maxSpecialtyProficiency() : resolvedRank.maxProficiency();
      double p = min + random.nextDouble() * Math.max(1.0, max - min);
      switch (magicId) {
         case "gander":
         case "gandr_machine_gun":
            vars.proficiency_gander = Math.max(vars.proficiency_gander, p);
            break;
         case "gravity_magic":
            vars.proficiency_gravity_magic = Math.max(vars.proficiency_gravity_magic, p);
            break;
         case "reinforcement":
            double reinforcementFloor = Math.max(40.0, min);
            vars.proficiency_reinforcement = Math.max(vars.proficiency_reinforcement, Math.max(p, reinforcementFloor));
            break;
         case "jewel_random_shoot":
            vars.proficiency_jewel_magic_shoot = Math.max(vars.proficiency_jewel_magic_shoot, p);
            break;
         case "healing_magic":
            vars.proficiency_healing_magic = Math.max(vars.proficiency_healing_magic, p);
            break;
         case "magic_analysis":
            MagicProficiencyService.set(vars, magicId, Math.max(MagicProficiencyService.get(vars, magicId), p));
            break;
         case "magic_bullet":
            vars.proficiency_magic_bullet = Math.max(vars.proficiency_magic_bullet, p);
            break;
         case "suggestion_magic":
            vars.proficiency_suggestion_magic = Math.max(vars.proficiency_suggestion_magic, p);
            break;
         case "binding_magic":
            vars.proficiency_binding_magic = Math.max(vars.proficiency_binding_magic, p);
            break;
         case "fire_magic":
            vars.proficiency_fire_magic = Math.max(vars.proficiency_fire_magic, p);
            break;
         case "water_magic":
            vars.proficiency_water_magic = Math.max(vars.proficiency_water_magic, p);
            break;
         case "wind_magic":
            vars.proficiency_wind_magic = Math.max(vars.proficiency_wind_magic, p);
            break;
         case "earth_magic":
            vars.proficiency_earth_magic = Math.max(vars.proficiency_earth_magic, p);
            break;
         case "airflow_blade":
         case "detection":
         case "mana_burst":
            MagicProficiencyService.set(vars, magicId, Math.max(MagicProficiencyService.get(vars, magicId), p));
            break;
         case "jewel_machine_gun":
         case "ruby_flame_sword":
         case "sapphire_winter_frost":
         case "emerald_winter_river":
         case "topaz_reinforcement":
         case "cyan_wind":
            vars.proficiency_jewel_magic_release = Math.max(vars.proficiency_jewel_magic_release, p);
      }
   }

   private static void ensurePrerequisites(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (magicId != null && !magicId.isEmpty()) {
         switch (magicId) {
            case "gandr_machine_gun":
               ensureLearned(vars, "gander");
               break;
            case "jewel_machine_gun":
               ensureLearned(vars, "gander");
               ensureLearned(vars, "jewel_magic_shoot");
               ensureLearned(vars, "jewel_magic_release");
               ensureLearned(vars, "jewel_random_shoot");
               break;
            case "ruby_flame_sword":
               ensureAdvancedJewelPrerequisites(vars, "ruby_throw");
               break;
            case "sapphire_winter_frost":
               ensureAdvancedJewelPrerequisites(vars, "sapphire_throw");
               break;
            case "emerald_winter_river":
               ensureAdvancedJewelPrerequisites(vars, "emerald_use");
               break;
            case "topaz_reinforcement":
               ensureAdvancedJewelPrerequisites(vars, "topaz_throw");
               break;
            case "cyan_wind":
               ensureAdvancedJewelPrerequisites(vars, "cyan_throw");
               break;
            case "worm_control":
            case "engraved_worm_operation":
               ensureLearned(vars, "worm_magic");
               break;
            case "wraith_servitude":
            case "evil_spirit_summoning":
               ensureLearned(vars, "spirit_summoning");
               break;
         }
      }
   }

   private static void ensureAdvancedJewelPrerequisites(TypeMoonWorldModVariables.PlayerVariables vars, String baseMagicId) {
      ensureLearned(vars, baseMagicId);
      ensureLearned(vars, "jewel_magic_shoot");
      ensureLearned(vars, "jewel_magic_release");
      ensureLearned(vars, "jewel_random_shoot");
   }

   private static void ensureLearned(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (magicId != null && !magicId.isEmpty()) {
         if (!vars.learned_magics.contains(magicId)) {
            vars.learned_magics.add(magicId);
         }
      }
   }

   private static void tickManaRecovery(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (npc.level() instanceof ServerLevel serverLevel) {
         double var9 = LeylineService.getRegenMultiplier(serverLevel, npc.blockPosition());
         vars.current_mana_regen_multiplier = var9;
         double interval = Math.max(1.0, vars.player_restore_magic_moment);
         if (vars.is_magic_circuit_open) {
            interval = Math.max(1.0, interval / 2.0);
         }

         double regenPerTick = vars.player_mana_egenerated_every_moment * var9 / interval;
         if (vars.player_mana < vars.player_max_mana) {
            vars.player_mana = Math.min(vars.player_max_mana, vars.player_mana + regenPerTick);
         }
      }
   }

   private static void tickLocalCooldown(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.magic_cooldown > 0.0) {
         vars.magic_cooldown = Math.max(0.0, vars.magic_cooldown - 1.0);
      }
   }

   private static void maybeCloseCircuit(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime) {
      long lastCombat = npc.getPersistentData().getLong(TAG_LAST_COMBAT_TICK);
      if (vars.is_magic_circuit_open && gameTime - lastCombat >= 120L) {
         vars.is_magic_circuit_open = false;
         vars.magic_circuit_open_timer = 0.0;
         vars.magic_analysis_active = false;
      }

      if (gameTime - lastCombat >= 120L) {
         clearNpcCombatSelfBuffs(npc);
      }
   }

   private static void clearNpcCombatSelfBuffs(MysticMagicianEntity npc) {
      if (npc != null) {
         npc.removeEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE);
         npc.removeEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH);
         npc.removeEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY);
         npc.removeEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT);
         NightVisionEffectSource.clearHiddenIfTagged(npc, NightVisionEffectSource.NPC_REINFORCEMENT_SIGHT, 600);
         npc.removeEffect(MobEffects.DAMAGE_BOOST);
         npc.removeEffect(MobEffects.MOVEMENT_SPEED);
         npc.removeEffect(MobEffects.DAMAGE_RESISTANCE);
         npc.removeEffect(MobEffects.REGENERATION);
         npc.removeEffect(MobEffects.JUMP);
         npc.removeEffect(MobEffects.SLOW_FALLING);
         MagicGravityEffectHandler.clearGravityState(npc);
         TypeMoonWorldModVariables.ReinforcementData data = (TypeMoonWorldModVariables.ReinforcementData)npc.getData(
            TypeMoonWorldModVariables.REINFORCEMENT_DATA
         );
         if (data != null && npc.getUUID().equals(data.casterUUID)) {
            data.casterUUID = null;
         }
      }
   }

   private static void tickProjectedHandItemExpiry(MysticMagicianEntity npc, long gameTime) {
      if (npc != null && !npc.level().isClientSide()) {
         if (npc.tickCount % 20 == 0) {
            expireProjectedHandItem(npc, EquipmentSlot.MAINHAND, gameTime);
            expireProjectedHandItem(npc, EquipmentSlot.OFFHAND, gameTime);
         }
      }
   }

   private static void expireProjectedHandItem(MysticMagicianEntity npc, EquipmentSlot slot, long gameTime) {
      ItemStack stack = npc.getItemBySlot(slot);
      if (!stack.isEmpty()) {
         CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("is_projected") && tag.getBoolean("is_projected")) {
               if (!tag.contains("is_infinite_projection") || !tag.getBoolean("is_infinite_projection")) {
                  if (tag.contains("projection_time")) {
                     long projectionTime = tag.getLong("projection_time");
                     if (gameTime - projectionTime > 200L) {
                        npc.setItemSlot(slot, ItemStack.EMPTY);
                        npc.level().playSound(null, npc.getX(), npc.getY(), npc.getZ(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 0.65F, 1.0F);
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean isNpcTargetCandidate(MysticMagicianEntity npc, LivingEntity target) {
      if (npc == null || target == null || !target.isAlive() || target.isRemoved() || target == npc) {
         return false;
      } else if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
         return false;
      } else {
         double verticalGap = Math.abs(target.getY() - npc.getY());
         if (verticalGap > MAX_TARGET_VERTICAL_GAP) {
            return false;
         } else {
            return !(verticalGap > MAX_BLIND_VERTICAL_GAP) || npc.hasLineOfSight(target);
         }
      }
   }

   private static boolean isValidCombatTarget(MysticMagicianEntity npc, LivingEntity target) {
      return isNpcTargetCandidate(npc, target) && npc.distanceToSqr(target) <= MAX_TARGET_DISTANCE_SQR;
   }

   private static boolean shouldRetreat(
      MysticMagicianEntity npc, LivingEntity target, NpcMagicCastBridge.ThreatProfile threat, NpcMagicCastBridge.BehaviorProfile behavior
   ) {
      if (npc != null && target != null && target.isAlive()) {
         double maxHealth = Math.max(1.0, (double)npc.getMaxHealth());
         double ratio = npc.getHealth() / maxHealth;
         double threshold = behavior == null ? RETREAT_HEALTH_RATIO : behavior.retreatHealthRatio();
         if (threat != null) {
            if (threat.strongTarget()) {
               threshold += 0.07;
            }

            if (threat.enemyCount() >= 3) {
               threshold += 0.08;
            }
         }

         threshold = Mth.clamp(threshold, 0.16, 0.58);
         return ratio <= threshold;
      } else {
         return false;
      }
   }

   private static NpcMagicCastBridge.ThreatProfile analyzeThreat(MysticMagicianEntity npc, LivingEntity target) {
      if (npc != null && target != null && target.isAlive()) {
         int enemyCount = countThreatEnemies(npc, target);
         double selfPower = estimateEntityPower(npc);
         double targetPower = estimateEntityPower(target);
         boolean strongTarget = targetPower >= selfPower * 1.18;
         double healthRatio = npc.getHealth() / Math.max(1.0, (double)npc.getMaxHealth());
         boolean outnumbered = enemyCount >= 3;
         boolean lowWhilePressured = enemyCount >= 2 && healthRatio <= 0.65;
         return new NpcMagicCastBridge.ThreatProfile(enemyCount, strongTarget, lowWhilePressured);
      } else {
         return NpcMagicCastBridge.ThreatProfile.NONE;
      }
   }

   private static NpcMagicCastBridge.ThreatProfile getCachedThreat(MysticMagicianEntity npc, LivingEntity target, long gameTime) {
      if (npc == null || target == null || !target.isAlive()) {
         return NpcMagicCastBridge.ThreatProfile.NONE;
      }
      CompoundTag data = npc.getPersistentData();
      String targetId = target.getUUID().toString();
      if (data.contains(TAG_THREAT_CACHE_TICK)
         && gameTime - data.getLong(TAG_THREAT_CACHE_TICK) < 5L
         && targetId.equals(data.getString(TAG_THREAT_CACHE_TARGET))) {
         return new NpcMagicCastBridge.ThreatProfile(
            data.getInt(TAG_THREAT_CACHE_COUNT),
            data.getBoolean(TAG_THREAT_CACHE_STRONG),
            data.getBoolean(TAG_THREAT_CACHE_LOW)
         );
      }
      NpcMagicCastBridge.ThreatProfile profile = analyzeThreat(npc, target);
      data.putLong(TAG_THREAT_CACHE_TICK, gameTime);
      data.putString(TAG_THREAT_CACHE_TARGET, targetId);
      data.putInt(TAG_THREAT_CACHE_COUNT, profile.enemyCount());
      data.putBoolean(TAG_THREAT_CACHE_STRONG, profile.strongTarget());
      data.putBoolean(TAG_THREAT_CACHE_LOW, profile.lowWhilePressured());
      return profile;
   }

   private static int countThreatEnemies(MysticMagicianEntity npc, LivingEntity currentTarget) {
      int count = 0;
      Set<UUID> seen = new HashSet<>();
      if (currentTarget != null && currentTarget.isAlive()) {
         UUID id = currentTarget.getUUID();
         seen.add(id);
         count++;
      }

      for (Mob mob : npc.level().getEntitiesOfClass(Mob.class, npc.getBoundingBox().inflate(20.0), m -> m != npc && m.isAlive() && m.getTarget() == npc)) {
         UUID id = mob.getUUID();
         if (seen.add(id)) {
            count++;
         }
      }

      return count;
   }

   private static double estimateEntityPower(LivingEntity entity) {
      if (entity == null) {
         return 1.0;
      } else {
         double maxHealth = Math.max(1.0, (double)entity.getMaxHealth());
         double attack = readAttributeValue(entity, Attributes.ATTACK_DAMAGE, 1.0);
         double armor = readAttributeValue(entity, Attributes.ARMOR, 0.0);
         double toughness = readAttributeValue(entity, Attributes.ARMOR_TOUGHNESS, 0.0);
         return maxHealth * 0.8 + attack * 6.0 + armor * 2.0 + toughness * 2.5;
      }
   }

   private static double readAttributeValue(LivingEntity entity, Holder<Attribute> attribute, double fallback) {
      AttributeInstance instance = entity.getAttribute(attribute);
      return instance == null ? fallback : instance.getValue();
   }

   private static NpcMagicCastBridge.MagicCapabilityProfile analyzeMagicCapabilities(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return NpcMagicCastBridge.MagicCapabilityProfile.NONE;
      } else {
         boolean hasBuff = vars.learned_magics.contains("reinforcement");
         boolean hasControl = false;
         boolean hasRanged = false;
         boolean hasMeleeBurst = false;
         boolean hasAnyCastable = false;

         for (int slot = 0; slot < 12; slot++) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
            if (entry != null && !entry.isEmpty() && entry.magicId != null && !entry.magicId.isEmpty() && vars.isWheelSlotEntryCastable(entry)) {
               hasAnyCastable = true;
               String var8 = entry.magicId;
               switch (var8) {
                  case "reinforcement":
                  case "healing_magic":
                  case "topaz_reinforcement":
                  case "mana_burst":
                     hasBuff = true;
                     hasMeleeBurst = true;
                     if ("mana_burst".equals(var8)
                        && entry.presetPayload != null
                        && entry.presetPayload.contains("mana_burst_mode")
                        && Mth.clamp(entry.presetPayload.getInt("mana_burst_mode"), 0, 2) == 2) {
                        hasRanged = true;
                     }
                     break;
                  case "gravity_magic":
                  case "binding_magic":
                  case "suggestion_magic":
                  case "worm_control":
                  case "engraved_worm_operation":
                  case "wraith_servitude":
                  case "evil_spirit_summoning":
                  case "water_magic":
                  case "wind_magic":
                  case "earth_magic":
                  case "detection":
                  case "magic_analysis":
                  case "imaginary_displacement":
                  case "imaginary_space":
                  case "sapphire_winter_frost":
                  case "emerald_winter_river":
                     hasControl = true;
                     if (!"detection".equals(var8) && !"magic_analysis".equals(var8)) {
                        hasRanged = true;
                     }
                     break;
                  case "gander":
                  case "magic_bullet":
                  case "airflow_blade":
                  case "fire_magic":
                  case "gandr_machine_gun":
                  case "jewel_random_shoot":
                  case "jewel_machine_gun":
                  case "ruby_flame_sword":
                  case "cyan_wind":
                  case "spiritron_cannon":
                     hasRanged = true;
                     break;
               }
            }
         }

         return new NpcMagicCastBridge.MagicCapabilityProfile(hasBuff, hasControl, hasRanged, hasMeleeBurst, hasAnyCastable);
      }
   }

   private static NpcMagicCastBridge.MagicCapabilityProfile getCachedMagicCapabilities(
      MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime
   ) {
      if (npc == null || vars == null) {
         return NpcMagicCastBridge.MagicCapabilityProfile.NONE;
      }
      CompoundTag data = npc.getPersistentData();
      if (data.contains(TAG_CAP_CACHE_TICK) && gameTime - data.getLong(TAG_CAP_CACHE_TICK) < 20L) {
         return new NpcMagicCastBridge.MagicCapabilityProfile(
            data.getBoolean(TAG_CAP_HAS_BUFF),
            data.getBoolean(TAG_CAP_HAS_CONTROL),
            data.getBoolean(TAG_CAP_HAS_RANGED),
            data.getBoolean(TAG_CAP_HAS_MELEE_BURST),
            data.getBoolean(TAG_CAP_HAS_ANY)
         );
      }
      NpcMagicCastBridge.MagicCapabilityProfile profile = analyzeMagicCapabilities(vars);
      data.putLong(TAG_CAP_CACHE_TICK, gameTime);
      return profile;
   }

   private static void syncCapabilityFlags(MysticMagicianEntity npc, NpcMagicCastBridge.MagicCapabilityProfile profile) {
      if (npc != null && profile != null) {
         CompoundTag data = npc.getPersistentData();
         boolean onlyMelee = !profile.hasRanged() && profile.hasMeleeBurst();
         data.putBoolean(TAG_CAP_HAS_RANGED, profile.hasRanged());
         data.putBoolean(TAG_CAP_HAS_MELEE_BURST, profile.hasMeleeBurst());
         data.putBoolean(TAG_CAP_ONLY_MELEE, onlyMelee);
         data.putBoolean(TAG_CAP_HAS_BUFF, profile.hasBuff());
         data.putBoolean(TAG_CAP_HAS_CONTROL, profile.hasControl());
         data.putBoolean(TAG_CAP_HAS_ANY, profile.hasAnyCastable());
      }
   }

   private static void maintainPreferredCombatDistance(
      MysticMagicianEntity npc,
      LivingEntity target,
      NpcMagicCastBridge.MagicCapabilityProfile capabilities,
      NpcMagicCastBridge.ThreatProfile threat,
      NpcMagicCastBridge.BehaviorProfile behavior
   ) {
      if (npc != null && target != null && target.isAlive() && capabilities != null) {
         double distance = Math.sqrt(npc.distanceToSqr(target));
         if (shouldForceMeleeEngage(capabilities, distance) || (capabilities.hasMeleeBurst() && shouldUseMeleeGoal(npc))) {
            if (distance > 2.75 && (npc.getNavigation().isDone() || npc.tickCount % 5 == 0)) {
               npc.getNavigation().moveTo(target, 1.24);
            } else if (distance <= 2.75 && npc.tickCount % 8 == 0) {
               circleMeleeTarget(npc, target, distance);
            }
            npc.getLookControl().setLookAt(target, 60.0F, 50.0F);
         } else if (!capabilities.hasRanged()) {
            if (capabilities.hasMeleeBurst()) {
               if (distance > (behavior == null ? MELEE_ENGAGE_DISTANCE : behavior.meleeEngageDistance())) {
                  npc.getNavigation().moveTo(target, 1.1);
               }

               npc.lookAt(target, 35.0F, 35.0F);
            }
         } else {
            double min = behavior == null ? RANGED_KEEP_MIN_DISTANCE : behavior.rangedKeepMinDistance();
            double max = behavior == null ? RANGED_KEEP_MAX_DISTANCE : behavior.rangedKeepMaxDistance();
            if (threat != null && threat.requiresDefensiveTactics()) {
               min += 3.0;
               max += 4.0;
            } else if (npc.getCombatStyle() == NpcCombatStyle.RANGED_BURST) {
               min++;
               max += 2.0;
            }

            if (!hasGenericClearRangedPath(npc, target)) {
               repositionForClearShot(npc, target, Math.max(min + 1.0, 8.0), 1.08);
            } else if (distance < min) {
               steerAwayFromTarget(npc, target, 7.0, 1.2);
            } else if (distance > max) {
               npc.getNavigation().moveTo(target, 1.05);
               npc.lookAt(target, 35.0F, 35.0F);
            } else {
               if (npc.tickCount % 18 == 0) {
                  Vec3 forward = getAimDirection(npc, target, 3.6, 0.0);
                  Vec3 right = getRightVector(forward);
                  double side = npc.getRandom().nextBoolean() ? 1.8 : -1.8;
                  Vec3 lateral = npc.position().add(right.scale(side));
                  npc.getNavigation().moveTo(lateral.x, lateral.y, lateral.z, 1.0);
               }

               npc.lookAt(target, 45.0F, 45.0F);
            }
         }
      }
   }

   private static boolean hasCastableMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      return NpcMagicExecutionService.hasCastableMagic(vars, magicId);
   }

   private static NpcMagicCastBridge.EnvironmentHazardProfile analyzeEnvironmentalHazard(MysticMagicianEntity npc) {
      if (npc == null) {
         return NpcMagicCastBridge.EnvironmentHazardProfile.NONE;
      } else {
         Level level = npc.level();
         BlockPos feet = npc.blockPosition();
         BlockPos below = feet.below();
         BlockPos head = feet.above();
         int hazardScore = 0;
         boolean severe = npc.isInLava() || npc.isOnFire();
         BlockPos[] checks = new BlockPos[]{feet, below, head, feet.north(), feet.south(), feet.west(), feet.east()};

         for (BlockPos pos : checks) {
            if (isSevereHazardBlock(level, pos)) {
               hazardScore += 2;
               severe = true;
            } else if (isMinorHazardBlock(level, pos)) {
               hazardScore++;
            }
         }

         return new NpcMagicCastBridge.EnvironmentHazardProfile(hazardScore > 0, severe, hazardScore);
      }
   }

   private static NpcMagicCastBridge.EnvironmentHazardProfile getCachedEnvironmentalHazard(MysticMagicianEntity npc, long gameTime) {
      if (npc == null) {
         return NpcMagicCastBridge.EnvironmentHazardProfile.NONE;
      }
      CompoundTag data = npc.getPersistentData();
      if (data.contains(TAG_ENV_CACHE_TICK) && gameTime - data.getLong(TAG_ENV_CACHE_TICK) < 10L) {
         return new NpcMagicCastBridge.EnvironmentHazardProfile(
            data.getBoolean(TAG_ENV_CACHE_ANY),
            data.getBoolean(TAG_ENV_CACHE_SEVERE),
            data.getInt(TAG_ENV_CACHE_SCORE)
         );
      }
      NpcMagicCastBridge.EnvironmentHazardProfile profile = analyzeEnvironmentalHazard(npc);
      data.putLong(TAG_ENV_CACHE_TICK, gameTime);
      data.putBoolean(TAG_ENV_CACHE_ANY, profile.anyHazard());
      data.putBoolean(TAG_ENV_CACHE_SEVERE, profile.severe());
      data.putInt(TAG_ENV_CACHE_SCORE, profile.hazardScore());
      return profile;
   }

   private static boolean handleEnvironmentalHazard(
      MysticMagicianEntity npc,
      TypeMoonWorldModVariables.PlayerVariables vars,
      long gameTime,
      NpcMagicCastBridge.EnvironmentHazardProfile hazard,
      NpcMagicCastBridge.MagicCapabilityProfile capabilities
   ) {
      if (!hazard.anyHazard()) {
         return false;
      } else {
         CompoundTag data = npc.getPersistentData();
         if (gameTime >= data.getLong(TAG_ENV_MOVE_TICK)) {
            Vec3 safe = findNearestSafeStandPos(npc, 4);
            if (safe != null) {
               double speed = hazard.severe() ? 1.3 : 1.08;
               npc.getNavigation().moveTo(safe.x, safe.y, safe.z, speed);
               data.putLong(TAG_ENV_MOVE_TICK, gameTime + (hazard.severe() ? 6L : 10L));
            }
         }

         if (gameTime >= getCastLockUntil(npc) && !(vars.player_mana <= 1.0)) {
            if (gameTime >= data.getLong(TAG_ENV_UTILITY_TICK)) {
               if (capabilities.hasBuff() && NpcMagicExecutionService.hasCastableMagic(vars, "reinforcement") && gameTime >= getMagicCooldownUntil(npc, "reinforcement")) {
                  CompoundTag payload = new CompoundTag();
                  payload.putInt("reinforcement_mode", hazard.severe() ? 0 : 2);
                  payload.putInt("reinforcement_level", hazard.severe() ? 3 : 2);
                  if (castReinforcement(npc, vars, payload, Math.max(65.0, vars.proficiency_reinforcement))) {
                     setMagicCooldown(npc, "reinforcement", gameTime + 120L);
                     setCastLockUntil(npc, gameTime + 10L);
                     data.putLong(TAG_ENV_UTILITY_TICK, gameTime + 30L);
                     return true;
                  }
               }

               if (consumeMana(vars, hazard.severe() ? 20.0 : 12.0)) {
                  if (!npc.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                     npc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, hazard.severe() ? 1 : 0, false, true, true));
                  }

                  if (hazard.severe() && !npc.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                     npc.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, true, true));
                  }

                  if (hazard.severe() && !npc.hasEffect(MobEffects.REGENERATION)) {
                     npc.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true, true));
                  }

                  setCastLockUntil(npc, gameTime + 8L);
                  data.putLong(TAG_ENV_UTILITY_TICK, gameTime + 24L);
                  return true;
               }
            }

            return hazard.severe() && hazard.hazardScore() >= 3;
         } else {
            return hazard.severe() && hazard.hazardScore() >= 3;
         }
      }
   }

   private static Vec3 findNearestSafeStandPos(MysticMagicianEntity npc, int radius) {
      if (npc == null) {
         return null;
      } else {
         Level level = npc.level();
         BlockPos origin = npc.blockPosition();
         BlockPos best = null;
         double bestDist = Double.MAX_VALUE;

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               BlockPos ground = origin.offset(dx, -1, dz);
               BlockPos feet = ground.above();
               BlockPos head = feet.above();
               if (isSafeGround(level, ground)
                  && isPassable(level, feet)
                  && isPassable(level, head)
                  && !isHazardousBlock(level, ground)
                  && !isHazardousBlock(level, feet)
                  && !isHazardousBlock(level, head)) {
                  double dist = origin.distSqr(feet);
                  if (dist < bestDist) {
                     bestDist = dist;
                     best = feet;
                  }
               }
            }
         }

         return best == null ? null : new Vec3(best.getX() + 0.5, best.getY(), best.getZ() + 0.5);
      }
   }

   private static boolean isSafeGround(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return !state.getCollisionShape(level, pos).isEmpty() && !isHazardousBlock(level, pos);
   }

   private static boolean isPassable(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.getCollisionShape(level, pos).isEmpty() && !state.getFluidState().is(FluidTags.LAVA);
   }

   private static boolean isHazardousBlock(Level level, BlockPos pos) {
      return isSevereHazardBlock(level, pos) || isMinorHazardBlock(level, pos);
   }

   private static boolean isSevereHazardBlock(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.getFluidState().is(FluidTags.LAVA)) {
         return true;
      } else {
         return !state.is(Blocks.FIRE) && !state.is(Blocks.SOUL_FIRE)
            ? state.getBlock() instanceof CampfireBlock && state.hasProperty(CampfireBlock.LIT) && (Boolean)state.getValue(CampfireBlock.LIT)
            : true;
      }
   }

   private static boolean isMinorHazardBlock(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(Blocks.MAGMA_BLOCK)
         || state.is(Blocks.CACTUS)
         || state.is(Blocks.SWEET_BERRY_BUSH)
         || state.is(Blocks.WITHER_ROSE)
         || state.is(Blocks.POWDER_SNOW);
   }

   private static void handleNoMagicFallback(
      MysticMagicianEntity npc,
      LivingEntity target,
      TypeMoonWorldModVariables.PlayerVariables vars,
      long gameTime,
      NpcMagicCastBridge.ThreatProfile threat,
      NpcMagicCastBridge.MagicCapabilityProfile capabilities
   ) {
      if (npc != null && target != null && target.isAlive()) {
         double distanceSqr = npc.distanceToSqr(target);
         double distance = Math.sqrt(distanceSqr);
         boolean forceMeleeEngage = shouldForceMeleeEngage(capabilities, distance);
         boolean underPressure = threat.requiresDefensiveTactics() || !forceMeleeEngage && distanceSqr < 64.0;
         if (capabilities.hasControl()
            && NpcMagicExecutionService.hasCastableMagic(vars, "gravity_magic")
            && gameTime >= getMagicCooldownUntil(npc, "gravity_magic")
            && distanceSqr <= 196.0) {
            CompoundTag gravityPayload = new CompoundTag();
            gravityPayload.putInt("gravity_target", 1);
            gravityPayload.putInt("gravity_mode", underPressure ? 2 : 1);
            if (castGravity(npc, target, vars, gravityPayload, Math.max(55.0, vars.proficiency_gravity_magic))) {
               int gcd = 18;
               vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
               npc.getPersistentData().putLong(TAG_NEXT_GLOBAL_CAST_TICK, gameTime + gcd);
               setMagicCooldown(npc, "gravity_magic", gameTime + 50L);
               setCastLockUntil(npc, gameTime + 10L);
               return;
            }
         }

         if (!capabilities.hasRanged() || !tryFallbackRangedFire(npc, target, vars, gameTime, threat, false)) {
            if (capabilities.hasBuff() && NpcMagicExecutionService.hasCastableMagic(vars, "reinforcement") && gameTime >= getMagicCooldownUntil(npc, "reinforcement")) {
               CompoundTag payload = new CompoundTag();
               payload.putInt("reinforcement_mode", underPressure ? 2 : 0);
               payload.putInt("reinforcement_level", 2);
               if (castReinforcement(npc, vars, payload, Math.max(60.0, vars.proficiency_reinforcement))) {
                  setMagicCooldown(npc, "reinforcement", gameTime + 120L);
                  setCastLockUntil(npc, gameTime + 10L);
                  return;
               }
            }

            if (forceMeleeEngage) {
               npc.getNavigation().moveTo(target, 1.12);
               npc.lookAt(target, 42.0F, 42.0F);
            } else if (underPressure || !capabilities.hasAnyOffense()) {
               steerAwayFromTarget(npc, target);
               if (!npc.hasEffect(MobEffects.MOVEMENT_SPEED) && consumeMana(vars, 12.0)) {
                  npc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 0, false, true, true));
                  setCastLockUntil(npc, gameTime + 6L);
               }
            } else if (capabilities.hasRanged()) {
               maintainPreferredCombatDistance(
                  npc,
                  target,
                  capabilities,
                  threat,
                  buildBehaviorProfile(npc.getCombatPersonality(), npc.getCombatTemperament())
               );
            } else {
               npc.getNavigation().moveTo(target, 1.1);
            }
         }
      }
   }

   private static boolean tryFallbackRangedFire(
      MysticMagicianEntity npc,
      LivingEntity target,
      TypeMoonWorldModVariables.PlayerVariables vars,
      long gameTime,
      NpcMagicCastBridge.ThreatProfile threat,
      boolean highThreat
   ) {
      if (npc != null && target != null && target.isAlive()) {
         if (!hasGenericClearRangedPath(npc, target)) {
            repositionForClearShot(npc, target, 9.0, highThreat ? 1.16 : 1.08);
            return false;
         } else if (NpcMagicExecutionService.hasCastableMagic(vars, "magic_bullet")
            && gameTime >= getMagicCooldownUntil(npc, "magic_bullet")
            && castMagicBullet(npc, target, vars, Math.max(highThreat ? 65.0 : 45.0, vars.proficiency_magic_bullet))) {
            applyPostCastCooldown(npc, vars, "magic_bullet", new CompoundTag(), gameTime, highThreat ? 8 : 6);
            return true;
         } else if (NpcMagicExecutionService.hasCastableMagic(vars, "gander")
            && gameTime >= getMagicCooldownUntil(npc, "gander")
            && castGander(npc, target, vars, new CompoundTag(), Math.max(highThreat ? 70.0 : 50.0, vars.proficiency_gander))) {
            applyPostCastCooldown(npc, vars, "gander", new CompoundTag(), gameTime, highThreat ? 10 : 8);
            return true;
         } else {
            if (NpcMagicExecutionService.hasCastableMagic(vars, "gandr_machine_gun") && gameTime >= getMagicCooldownUntil(npc, "gandr_machine_gun")) {
               CompoundTag payload = new CompoundTag();
               payload.putInt("gandr_machine_gun_mode", highThreat && threat.enemyCount() >= 3 ? 1 : 0);
               if (castGandrMachineGun(npc, target, vars, payload, Math.max(highThreat ? 70.0 : 45.0, vars.proficiency_gander))) {
                  applyPostCastCooldown(npc, vars, "gandr_machine_gun", payload, gameTime, highThreat ? 12 : 8);
                  return true;
               }
            }

            if (NpcMagicExecutionService.hasCastableMagic(vars, "jewel_random_shoot")
               && gameTime >= getMagicCooldownUntil(npc, "jewel_random_shoot")
               && castJewelRandomShoot(npc, target, vars, vars.proficiency_jewel_magic_shoot)) {
               applyPostCastCooldown(npc, vars, "jewel_random_shoot", new CompoundTag(), gameTime, 8);
               return true;
            } else if (NpcMagicExecutionService.hasCastableMagic(vars, "jewel_machine_gun")
               && gameTime >= getMagicCooldownUntil(npc, "jewel_machine_gun")) {
               CompoundTag jewelPayload = buildCombatJewelMachineGunPayload(npc, target, threat);
               if (castJewelMachineGun(npc, target, vars, jewelPayload, vars.proficiency_jewel_magic_release)) {
                  applyPostCastCooldown(npc, vars, "jewel_machine_gun", jewelPayload, gameTime, 8);
                  return true;
               }
               return tryUseJewelItemSkill(npc, target, vars, gameTime, threat);
            } else {
               return tryUseJewelItemSkill(npc, target, vars, gameTime, threat);
            }
         }
      } else {
         return false;
      }
   }

   private static void applyPostCastCooldown(
      MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, String magicId, CompoundTag payload, long gameTime, int minCastLock
   ) {
      double chantMultiplier = AdvancedPassiveService.chantMultiplier(vars);
      int gcd = Math.max(1, (int)Math.round(NpcMagicExecutionService.getGlobalCooldownAfterCast(magicId, payload) * chantMultiplier));
      vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
      npc.getPersistentData().putLong(TAG_NEXT_GLOBAL_CAST_TICK, gameTime + gcd);
      long perMagicCooldown = Math.max(1L, Math.round(NpcMagicExecutionService.getPerMagicCooldown(magicId, payload) * chantMultiplier));
      setMagicCooldown(npc, magicId, gameTime + perMagicCooldown);
      setCastLockUntil(npc, gameTime + Math.max(minCastLock, gcd / 2));
      recordResourceMagicUse(npc, magicId, gameTime);
      recordMagicCast(npc, magicId);
   }

   private static boolean tryUseJewelItemSkill(
      MysticMagicianEntity npc, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime, NpcMagicCastBridge.ThreatProfile threat
   ) {
      if (npc == null || target == null || !target.isAlive() || vars == null) {
         return false;
      } else if (!hasJewelItemAccess(vars)) {
         return false;
      } else if (!(vars.player_mana < 18.0) && gameTime >= getCastLockUntil(npc)) {
         if (gameTime < npc.getPersistentData().getLong(TAG_JEWEL_ITEM_NEXT_TICK)) {
            return false;
         } else {
            double distanceSqr = npc.distanceToSqr(target);
            if (!(distanceSqr < 16.0) && !(distanceSqr > 576.0)) {
               boolean highThreat = threat != null && threat.requiresDefensiveTactics();
               boolean closeControlWindow = distanceSqr <= 144.0 && npc.hasLineOfSight(target);
               int triggerChance = highThreat ? 38 : closeControlWindow ? 30 : 24;
               if (npc.getRandom().nextInt(100) >= triggerChance) {
                  return false;
               } else {
                  String usedSkillId = null;
                  double manaRatio = vars.player_max_mana <= 0.0 ? 0.0 : Mth.clamp(vars.player_mana / vars.player_max_mana, 0.0, 1.0);
                  int advancedChance = highThreat ? 16 : 10;
                  if (closeControlWindow) {
                     advancedChance += 18;
                  } else if (distanceSqr <= 256.0) {
                     advancedChance += 8;
                  }

                  if (manaRatio >= 0.75) {
                     advancedChance += 6;
                  }

                  advancedChance = Mth.clamp(advancedChance, 10, 42);
                  int engravedChance = highThreat ? 9 : 4;
                  if ((highThreat || manaRatio >= 0.45)
                     && isResourceMagicAvailable(npc, NPC_JEWEL_ITEM_ENGRAVED, gameTime)
                     && npc.getRandom().nextInt(100) < engravedChance
                     && castJewelItemEngraved(npc, target, vars, highThreat)) {
                     usedSkillId = NPC_JEWEL_ITEM_ENGRAVED;
                  }

                  if (usedSkillId == null
                     && (highThreat || manaRatio >= 0.55)
                     && isResourceMagicAvailable(npc, NPC_JEWEL_ITEM_ADVANCED, gameTime)
                     && npc.getRandom().nextInt(100) < advancedChance
                     && castJewelItemAdvanced(npc, target, vars, highThreat)) {
                     usedSkillId = NPC_JEWEL_ITEM_ADVANCED;
                  }

                  if (usedSkillId == null && isResourceMagicAvailable(npc, NPC_JEWEL_ITEM_BASIC, gameTime) && castJewelItemBasic(npc, target, vars)) {
                     usedSkillId = NPC_JEWEL_ITEM_BASIC;
                  }

                  if (usedSkillId == null) {
                     return false;
                  } else {
                     int gcd = switch (usedSkillId) {
                        case NPC_JEWEL_ITEM_ENGRAVED -> 16;
                        case NPC_JEWEL_ITEM_ADVANCED -> 14;
                        default -> 10;
                     };

                     int perCd = switch (usedSkillId) {
                        case NPC_JEWEL_ITEM_ENGRAVED -> 3600;
                        case NPC_JEWEL_ITEM_ADVANCED -> 1800;
                        default -> 1200;
                     };
                     vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
                     npc.getPersistentData().putLong(TAG_NEXT_GLOBAL_CAST_TICK, gameTime + gcd);
                     setMagicCooldown(npc, usedSkillId, gameTime + perCd);
                     recordResourceMagicUse(npc, usedSkillId, gameTime);
                     recordMagicCast(npc, usedSkillId);
                     setCastLockUntil(npc, gameTime + gcd);
                     npc.getPersistentData().putLong(TAG_JEWEL_ITEM_NEXT_TICK, gameTime + (usedSkillId.equals(NPC_JEWEL_ITEM_ADVANCED) ? 28L : 40L));
                     return true;
                  }
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean hasJewelItemAccess(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) {
         return false;
      } else {
         boolean learnedBasic = vars.learned_magics.contains("jewel_magic_shoot")
            || vars.learned_magics.contains("jewel_random_shoot")
            || vars.learned_magics.contains("jewel_machine_gun");
         boolean learnedAdvanced = vars.learned_magics.contains("jewel_magic_release") || vars.learned_magics.contains("jewel_machine_gun");
         return learnedBasic || learnedAdvanced;
      }
   }

   private static boolean castJewelItemBasic(MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars) {
      GemType type = NPC_BASIC_JEWEL_TYPES[caster.getRandom().nextInt(NPC_BASIC_JEWEL_TYPES.length)];
      if (type != GemType.WHITE_GEMSTONE && !hasProjectilePath(caster, target, 2.8, 0.04)) {
         repositionForClearShot(caster, target, 8.5, 1.08);
         return false;
      } else if (!consumeMana(vars, 26.0)) {
         return false;
      } else {
         return type == GemType.WHITE_GEMSTONE ? castNpcWhiteShockwave(caster, target, vars) : spawnNpcGemProjectile(caster, target, type, 2.8, 0.1F, false, false, false);
      }
   }

   private static boolean castJewelItemAdvanced(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, boolean highThreat
   ) {
      int mode = selectJewelItemAdvancedMode(caster, target, highThreat);
      if (mode == 1 && !hasProjectilePath(caster, target, 2.8, 0.04)) {
         mode = caster.hasLineOfSight(target) ? 2 : 0;
      }

      if (mode == 2 && !caster.hasLineOfSight(target)) {
         mode = hasProjectilePath(caster, target, 2.8, 0.04) ? 1 : 0;
      }

      if ((mode == 1 && !hasProjectilePath(caster, target, 2.8, 0.04)) || (mode == 2 && !caster.hasLineOfSight(target))) {
         repositionForClearShot(caster, target, 8.5, 1.08);
         return false;
      } else if (!consumeMana(vars, 70.0)) {
         return false;
      } else {
         if (mode == 0) {
            markCastingPose(caster, 12);
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 360, 1, false, true, true));
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 360, 1, false, true, true));
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 320, 0, false, true, true));
            if (caster.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.5, 0.7, 0.5, 0.03);
            }
            return true;
         } else {
            return mode == 1 ? spawnNpcGemProjectile(caster, target, GemType.CYAN, 2.8, 0.08F, false, false, true) : castNpcAdvancedWinterFrost(caster, target, vars);
         }
      }
   }

   private static int selectJewelItemAdvancedMode(MysticMagicianEntity caster, LivingEntity target, boolean highThreat) {
      if (caster == null || target == null || !target.isAlive()) {
         return 0;
      } else {
         double distance = Math.sqrt(caster.distanceToSqr(target));
         boolean lineOfSight = caster.hasLineOfSight(target);
         double healthRatio = caster.getHealth() / Math.max(1.0, (double)caster.getMaxHealth());
         int buffWeight = 16;
         int cyanWeight = 22;
         int frostWeight = 26;
         if (highThreat) {
            frostWeight += 14;
            buffWeight += 8;
         }

         if (healthRatio < 0.45) {
            buffWeight += 20;
         }

         if (distance <= 9.0) {
            frostWeight += 34;
            cyanWeight = Math.max(8, cyanWeight - 6);
         } else if (distance <= 13.0) {
            frostWeight += 14;
            cyanWeight += 8;
         } else {
            cyanWeight += 24;
         }

         if (!lineOfSight) {
            frostWeight = Math.max(6, frostWeight - 28);
            cyanWeight += 10;
            buffWeight += 6;
         }

         int total = buffWeight + cyanWeight + frostWeight;
         int roll = caster.getRandom().nextInt(Math.max(1, total));
         if (roll < frostWeight) {
            return 2;
         } else {
            roll -= frostWeight;
            return roll < cyanWeight ? 1 : 0;
         }
      }
   }

   private static boolean castNpcWhiteShockwave(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars
   ) {
      if (caster == null || vars == null) {
         return false;
      } else {
         markCastingPose(caster, 10);
         double proficiency = Mth.clamp(getEffectiveProficiency(caster, vars, "jewel_random_shoot", false), 0.0, 100.0);
         double powerNormalized = Mth.clamp(0.35 + proficiency / 100.0 * 0.65, 0.35, 1.0);
         double radius = 4.5 + 5.0 * powerNormalized;
         float baseDamage = (float)(1.2 + 2.8 * powerNormalized);
         double baseHorizontalKnockback = 0.9 + 2.1 * powerNormalized;
         double baseVerticalKnockback = 0.2 + 0.45 * powerNormalized;
         if (caster.level() instanceof ServerLevel serverLevel) {
            spawnNpcWhiteShockwaveParticles(serverLevel, caster, radius, powerNormalized);
         }

         for (LivingEntity nearby : caster.level().getEntitiesOfClass(
            LivingEntity.class,
            caster.getBoundingBox().inflate(radius, 3.0 + powerNormalized, radius),
            e -> e != null && e.isAlive() && isValidCombatTarget(caster, e)
         )) {
            double dx = nearby.getX() - caster.getX();
            double dz = nearby.getZ() - caster.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < 1.0E-4) {
               double angle = caster.getRandom().nextDouble() * (Math.PI * 2);
               dx = Math.cos(angle);
               dz = Math.sin(angle);
               distance = 1.0;
            }

            double nx = dx / distance;
            double nz = dz / distance;
            double falloff = 1.0 - Math.min(1.0, distance / radius);
            float damage = (float)Math.max(0.75, baseDamage * (0.5 + 0.5 * falloff));
            nearby.hurt(caster.damageSources().indirectMagic(caster, caster), damage);
            double horizontal = baseHorizontalKnockback * (0.35 + 0.65 * falloff);
            double vertical = baseVerticalKnockback * (0.45 + 0.55 * falloff);
            nearby.push(nx * horizontal, vertical, nz * horizontal);
            nearby.fallDistance = 0.0F;
            EntityUtils.triggerSwarmAnger(caster.level(), caster, nearby);
            if (nearby instanceof Mob mob) {
               mob.setTarget(caster);
            }
         }

         caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.45F, 1.25F);
         return true;
      }
   }

   private static void spawnNpcWhiteShockwaveParticles(ServerLevel level, LivingEntity caster, double radius, double powerNormalized) {
      double cx = caster.getX();
      double cy = caster.getY() + 1.0;
      double cz = caster.getZ();
      int burstLevel = (int)Math.round(1.0 + powerNormalized * 2.0);
      level.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 4 + 3 * burstLevel, 0.2, 0.15, 0.2, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.06, cz, 8 + 6 * burstLevel, 0.25, 0.12, 0.25, 0.03);
      level.addFreshEntity(new ExpandingRingEffectEntity(level, cx, cy, cz, 0.32F, (float)radius, 0.16F, 12, 0xFFFFFF, 0.76F, 0.0F));
      level.addFreshEntity(new ExpandingRingEffectEntity(level, cx, cy + 0.06, cz, 0.18F, (float)(radius * 0.65), 0.1F, 18, 0xF2F2FF, 0.46F, 0.01F));
      spawnNpcWhiteAirwaves(level, cx, cy, cz, radius, powerNormalized, burstLevel);
   }

   private static void spawnNpcWhiteAirwaves(ServerLevel level, double cx, double cy, double cz, double radius, double powerNormalized, int burstLevel) {
      int extraRings = 3 + burstLevel * 2;

      for (int i = 0; i < extraRings; i++) {
         float startRadius = 0.08F + level.random.nextFloat() * 0.22F;
         float endRadius = (float)(radius * (0.34 + level.random.nextDouble() * 0.44));
         float thickness = 0.06F + level.random.nextFloat() * 0.07F;
         int duration = 10 + level.random.nextInt(10);
         int color = level.random.nextFloat() < 0.35F ? 0xF4F6FF : 0xFFFFFF;
         float alpha = 0.22F + level.random.nextFloat() * 0.28F;
         float upward = level.random.nextFloat() * 0.03F;
         float tilt = level.random.nextFloat() * (10.0F + (float)powerNormalized * 34.0F);
         float yaw = level.random.nextFloat() * 360.0F;
         double ox = (level.random.nextDouble() - 0.5) * radius * 0.15;
         double oy = (level.random.nextDouble() - 0.5) * 0.25;
         double oz = (level.random.nextDouble() - 0.5) * radius * 0.15;
         level.addFreshEntity(
            new ExpandingRingEffectEntity(level, cx + ox, cy + oy, cz + oz, startRadius, endRadius, thickness, duration, color, alpha, upward, tilt, yaw)
         );
      }
   }

   private static boolean castNpcAdvancedWinterFrost(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null) {
         return false;
      } else {
         markCastingPose(caster, 14);
         double proficiency = Mth.clamp(getEffectiveProficiency(caster, vars, "sapphire_winter_frost", false), 0.0, 100.0);
         int radius = Mth.clamp(8 + (int)Math.floor(proficiency / 25.0), 8, 12);
         int duration = 140 + (int)Math.round(proficiency * 1.8);
         applyNpcWinterFrostField(caster, target, radius, duration, 4, 3, 1);
         return true;
      }
   }

   private static boolean castJewelItemEngraved(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, boolean highThreat
   ) {
      if (vars == null) {
         return false;
      } else {
         if (!hasNpcEngravingAccess(vars)) {
            return false;
         }

         List<Integer> choices = new ArrayList<>();
         choices.add(0);
         if (hasNpcEngravedGravityAccess(vars)) {
            choices.add(1);
            choices.add(3);
         }

         if (vars.hasLearnedSelfMagic("reinforcement")) {
            choices.add(2);
         }

         int pick = choices.get(caster.getRandom().nextInt(choices.size()));
         switch (pick) {
            case 1: {
               CompoundTag payload = new CompoundTag();
               payload.putInt("gravity_target", 1);
               payload.putInt("gravity_mode", highThreat ? 2 : 1);
               return castGravity(caster, target, vars, payload, Math.max(80.0, vars.proficiency_gravity_magic));
            }
            case 2: {
               CompoundTag payload = new CompoundTag();
               payload.putInt("reinforcement_mode", highThreat ? 0 : 2);
               payload.putInt("reinforcement_level", highThreat ? 3 : 2);
               return castReinforcement(caster, vars, payload, Math.max(80.0, vars.proficiency_reinforcement));
            }
            case 3:
               return castJewelItemBlackShardGravity(caster, target, vars, highThreat);
            default:
               return castGander(caster, target, vars, new CompoundTag(), Math.max(80.0, vars.proficiency_gander));
         }
      }
   }

   private static boolean castJewelItemBlackShardGravity(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, boolean highThreat
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null) {
         return false;
      } else if (!hasNpcEngravedGravityAccess(vars)) {
         return false;
      } else if (!hasProjectilePath(caster, target, 1.5, 0.06)) {
         repositionForClearShot(caster, target, 9.0, 1.08);
         return false;
      } else if (!consumeMana(vars, highThreat ? 32.0 : 26.0)) {
         return false;
      } else {
         double proficiency = Math.max(vars.proficiency_gravity_magic, vars.proficiency_jewel_magic_release);
         double virtualManaAmount = 100.0 + proficiency * 0.8;
         ItemStack projectileGem = GemGravityFieldMagic.createBlackShardGravityProjectileStack(
            new ItemStack(ModItems.getNormalizedFullCarvedGem(GemType.BLACK_SHARD)), proficiency, virtualManaAmount, 1.0F
         );
         if (projectileGem.isEmpty()) {
            return false;
         } else {
            markCastingPose(caster, 10);
            Vec3 direction = getAimDirection(caster, target, 1.5, 0.06);
            faceCasterToDirection(caster, direction);
            Vec3 spawnPos = EntityUtils.getRightHandCastAnchor(caster).add(direction.scale(0.12));
            RubyProjectileEntity projectile = new RubyProjectileEntity(caster.level(), caster);
            projectile.setGemType(toGemTypeId(GemType.BLACK_SHARD));
            projectile.setItem(projectileGem);
            projectile.setPos(spawnPos);
            projectile.shoot(direction.x, direction.y, direction.z, 1.5F, 0.35F);
            caster.level().addFreshEntity(projectile);
            caster.level().playSound(null, caster.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.HOSTILE, 0.8F, 0.75F);
            return true;
         }
      }
   }

   private static boolean hasNpcEngravingAccess(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars != null && vars.learned_magics.contains("jewel_magic_release");
   }

   private static boolean hasNpcEngravedGravityAccess(TypeMoonWorldModVariables.PlayerVariables vars) {
      return hasNpcEngravingAccess(vars) && vars.hasLearnedSelfMagic("gravity_magic");
   }

   private static void handleOnFireEmergency(MysticMagicianEntity npc, long gameTime) {
      if (npc == null) {
         return;
      }

      CompoundTag data = npc.getPersistentData();
      if (!npc.isOnFire()) {
         data.remove(TAG_FIRE_IGNITE_TICK);
         data.remove(TAG_FIRE_NEXT_WATER_SEARCH_TICK);
         return;
      }

      long igniteTick = data.contains(TAG_FIRE_IGNITE_TICK) ? data.getLong(TAG_FIRE_IGNITE_TICK) : gameTime;
      if (!data.contains(TAG_FIRE_IGNITE_TICK)) {
         data.putLong(TAG_FIRE_IGNITE_TICK, gameTime);
      }

      if (gameTime - igniteTick >= FIRE_AUTO_EXTINGUISH_TICKS) {
         npc.clearFire();
         data.remove(TAG_FIRE_IGNITE_TICK);
         data.remove(TAG_FIRE_NEXT_WATER_SEARCH_TICK);
         return;
      }

      if (gameTime >= data.getLong(TAG_FIRE_NEXT_WATER_SEARCH_TICK)) {
         BlockPos waterPos = findNearbyWaterPos(npc, FIRE_WATER_SEARCH_HORIZONTAL_RADIUS, FIRE_WATER_SEARCH_VERTICAL_RADIUS);
         if (waterPos != null) {
            npc.getNavigation().moveTo(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5, 1.35);
         }

         data.putLong(TAG_FIRE_NEXT_WATER_SEARCH_TICK, gameTime + FIRE_WATER_SEARCH_INTERVAL_TICKS);
      }
   }

   private static BlockPos findNearbyWaterPos(MysticMagicianEntity npc, int horizontalRadius, int verticalRadius) {
      if (npc == null) {
         return null;
      }

      Level level = npc.level();
      BlockPos center = npc.blockPosition();
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;

      for (BlockPos pos : BlockPos.betweenClosed(
         center.offset(-horizontalRadius, -verticalRadius, -horizontalRadius), center.offset(horizontalRadius, verticalRadius, horizontalRadius)
      )) {
         if (level.getFluidState(pos).is(FluidTags.WATER)) {
            double dist = center.distSqr(pos);
            if (dist < bestDist) {
               bestDist = dist;
               best = pos.immutable();
            }
         }
      }

      return best;
   }

   private static int purgeDeprecatedProjectionChain(TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag data) {
      int removed = 0;
      if (vars != null) {
         removed += vars.learned_magics.removeIf(m -> "projection".equals(m) || "broken_phantasm".equals(m)) ? 1 : 0;
         removed += vars.crest_entries.removeIf(e -> e != null && ("projection".equals(e.magicId) || "broken_phantasm".equals(e.magicId)) ? true : false)
            ? 1
            : 0;

         for (int wheel = 0; wheel < 10; wheel++) {
            for (int slot = 0; slot < 12; slot++) {
               TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(wheel, slot);
               if (entry != null && ("projection".equals(entry.magicId) || "broken_phantasm".equals(entry.magicId))) {
                  vars.setWheelSlotEntry(wheel, slot, new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(wheel, slot));
                  removed++;
               }
            }
         }

         vars.rebuildSelectedMagicsFromActiveWheel();
      }

      if (data != null) {
         if (data.contains(TAG_MAGIC_CD, 10)) {
            CompoundTag cd = data.getCompound(TAG_MAGIC_CD);
            if (cd.contains("projection")) {
               cd.remove("projection");
               removed++;
            }

            if (cd.contains("broken_phantasm")) {
               cd.remove("broken_phantasm");
               removed++;
            }

            data.put(TAG_MAGIC_CD, cd);
         }

         if (data.contains(TAG_RESOURCE_USE, 10)) {
            CompoundTag rs = data.getCompound(TAG_RESOURCE_USE);
            if (rs.contains("projection")) {
               rs.remove("projection");
               removed++;
            }

            if (rs.contains("broken_phantasm")) {
               rs.remove("broken_phantasm");
               removed++;
            }

            data.put(TAG_RESOURCE_USE, rs);
         }
      }

      return removed;
   }

   private static int getFixedLevel(MysticMagicianEntity npc) {
      if (npc == null) {
         return LEVEL_MIN;
      } else {
         CompoundTag data = npc.getPersistentData();
         int level = Mth.clamp(data.contains(TAG_FIXED_LEVEL) ? data.getInt(TAG_FIXED_LEVEL) : LEVEL_MIN, LEVEL_MIN, LEVEL_MAX);
         data.putInt(TAG_FIXED_LEVEL, level);
         return level;
      }
   }

   private static int getCombatLevelBonus(MysticMagicianEntity npc) {
      if (npc == null) {
         return 0;
      } else {
         CompoundTag data = npc.getPersistentData();
         int bonus = Mth.clamp(data.contains(TAG_COMBAT_LEVEL_BONUS) ? data.getInt(TAG_COMBAT_LEVEL_BONUS) : 0, COMBAT_BONUS_MIN, COMBAT_BONUS_MAX);
         data.putInt(TAG_COMBAT_LEVEL_BONUS, bonus);
         return bonus;
      }
   }

   private static void updateCombatLevelBonus(MysticMagicianEntity npc, LivingEntity target, NpcMagicCastBridge.ThreatProfile threat) {
      if (npc != null && target != null && target.isAlive()) {
         int bonus = 0;
         double healthRatio = npc.getHealth() / Math.max(1.0, (double)npc.getMaxHealth());
         if (threat != null && (threat.strongTarget() || threat.enemyCount() >= 3)) {
            bonus++;
         }

         if (healthRatio <= 0.7 || npc.distanceToSqr(target) <= 49.0) {
            bonus++;
         }

         npc.getPersistentData().putInt(TAG_COMBAT_LEVEL_BONUS, Mth.clamp(bonus, COMBAT_BONUS_MIN, COMBAT_BONUS_MAX));
      }
   }

   private static boolean tryAdaptiveReinforcement(
      MysticMagicianEntity npc,
      LivingEntity target,
      TypeMoonWorldModVariables.PlayerVariables vars,
      long gameTime,
      NpcMagicCastBridge.ThreatProfile threat,
      NpcMagicCastBridge.MagicCapabilityProfile capabilities,
      NpcMagicCastBridge.BehaviorProfile behavior
   ) {
      if (npc == null || target == null || vars == null || !target.isAlive()) {
         return false;
      } else if (capabilities == null || !capabilities.hasBuff() || !NpcMagicExecutionService.hasCastableMagic(vars, "reinforcement")) {
         return false;
      } else if (gameTime < getMagicCooldownUntil(npc, "reinforcement")) {
         return false;
      } else {
         CompoundTag data = npc.getPersistentData();
         int level = Mth.clamp(getFixedLevel(npc) + getCombatLevelBonus(npc), 1, 5);
         double healthRatio = npc.getHealth() / Math.max(1.0, (double)npc.getMaxHealth());
         double distance = Math.sqrt(npc.distanceToSqr(target));
         boolean highThreat = threat != null && threat.requiresDefensiveTactics();
         boolean lostSight = !npc.hasLineOfSight(target);
         boolean retreatOrChase = distance < behavior.rangedKeepMinDistance() || distance > behavior.rangedKeepMaxDistance();
         int mode = -1;
         String cdKey = "";
         if ((healthRatio <= 0.56 || highThreat) && gameTime >= data.getLong(TAG_REINF_BODY_CD)) {
            mode = 0;
            cdKey = TAG_REINF_BODY_CD;
         } else if ((distance <= behavior.meleeSkillTriggerDistance() + 0.4 || shouldForceMeleeEngage(capabilities, distance))
            && gameTime >= data.getLong(TAG_REINF_HAND_CD)) {
            mode = 1;
            cdKey = TAG_REINF_HAND_CD;
         } else if (retreatOrChase && gameTime >= data.getLong(TAG_REINF_LEG_CD)) {
            mode = 2;
            cdKey = TAG_REINF_LEG_CD;
         } else if ((distance >= 12.0 || lostSight || threat.enemyCount() >= 2) && gameTime >= data.getLong(TAG_REINF_EYE_CD)) {
            mode = 3;
            cdKey = TAG_REINF_EYE_CD;
         }

         if (mode < 0 && !hasAnyReinforcementEffect(npc)) {
            if (gameTime >= data.getLong(TAG_REINF_BODY_CD) && (highThreat || healthRatio <= 0.75)) {
               mode = 0;
               cdKey = TAG_REINF_BODY_CD;
            } else if (gameTime >= data.getLong(TAG_REINF_LEG_CD) && retreatOrChase) {
               mode = 2;
               cdKey = TAG_REINF_LEG_CD;
            } else if (gameTime >= data.getLong(TAG_REINF_EYE_CD)) {
               mode = 3;
               cdKey = TAG_REINF_EYE_CD;
            }
         }

         if (mode < 0) {
            return false;
         } else {
            CompoundTag payload = new CompoundTag();
            payload.putInt("reinforcement_mode", mode);
            payload.putInt("reinforcement_level", level);
            if (!castReinforcement(npc, vars, payload, Math.max(72.0, vars.proficiency_reinforcement))) {
               return false;
            } else {
               // Support-cast cooldown only: do not consume global cast slot/cast-lock.
               setMagicCooldown(npc, "reinforcement", gameTime + NpcMagicExecutionService.getPerMagicCooldown("reinforcement", payload));
               recordResourceMagicUse(npc, "reinforcement", gameTime);
               data.putLong(cdKey, gameTime + 80L + mode * 15L);
               return true;
            }
         }
      }
   }

   private static boolean hasAnyReinforcementEffect(MysticMagicianEntity npc) {
      if (npc == null) {
         return false;
      }

      return npc.hasEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE)
         || npc.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE)
         || npc.hasEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH)
         || npc.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH)
         || npc.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY)
         || npc.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY)
         || npc.hasEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT)
         || npc.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_SIGHT);
   }

   private static boolean trySelfGravityStabilize(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime, boolean retreatContext) {
      if (npc == null || vars == null) {
         return false;
      } else if (!NpcMagicExecutionService.hasCastableMagic(vars, "gravity_magic")) {
         return false;
      } else if (gameTime < getCastLockUntil(npc) || gameTime < getMagicCooldownUntil(npc, "gravity_magic")) {
         return false;
      } else {
         boolean fallingRisk = npc.fallDistance >= 3.5F || !npc.onGround() && npc.getDeltaMovement().y < -0.35;
         if (!retreatContext && !fallingRisk) {
            return false;
         } else {
            CompoundTag payload = new CompoundTag();
            payload.putInt("gravity_target", 0);
            payload.putInt("gravity_mode", -1);
            if (!castGravity(npc, npc.getTarget(), vars, payload, Math.max(60.0, vars.proficiency_gravity_magic))) {
               return false;
            } else {
               int cd = retreatContext ? 45 : 30;
               setMagicCooldown(npc, "gravity_magic", gameTime + cd);
               setCastLockUntil(npc, gameTime + 8L);
               return true;
            }
         }
      }
   }

   private static void clearPendingMachineGun(MysticMagicianEntity npc) {
      if (npc != null) {
         CompoundTag data = npc.getPersistentData();
         data.remove(TAG_PENDING_MG_MAGIC);
         data.remove(TAG_PENDING_MG_TARGET);
         data.remove(TAG_PENDING_MG_NEXT_TICK);
         data.remove(TAG_PENDING_MG_WAVES_LEFT);
         data.remove(TAG_PENDING_MG_INTERVAL);
         data.remove(TAG_PENDING_MG_MODE);
         data.remove(TAG_PENDING_MG_CHARGE);
         data.remove(TAG_PENDING_MG_SHOT_COUNT);
         data.remove(TAG_PENDING_MG_BLOCKED_RETRIES);
      }
   }

   private static void startPendingMachineGun(
      MysticMagicianEntity npc, LivingEntity target, String magicId, int wavesLeft, int interval, int mode, int chargeOrPower, int shotCount
   ) {
      CompoundTag data = npc.getPersistentData();
      data.putString(TAG_PENDING_MG_MAGIC, magicId);
      data.putString(TAG_PENDING_MG_TARGET, target == null ? "" : target.getUUID().toString());
      data.putLong(TAG_PENDING_MG_NEXT_TICK, npc.level().getGameTime() + interval);
      data.putInt(TAG_PENDING_MG_WAVES_LEFT, Math.max(0, wavesLeft));
      data.putInt(TAG_PENDING_MG_INTERVAL, Math.max(2, interval));
      data.putInt(TAG_PENDING_MG_MODE, mode);
      data.putInt(TAG_PENDING_MG_CHARGE, chargeOrPower);
      data.putInt(TAG_PENDING_MG_SHOT_COUNT, Math.max(1, shotCount));
      data.putInt(TAG_PENDING_MG_BLOCKED_RETRIES, 0);
   }

   private static boolean processPendingMachineGun(
      MysticMagicianEntity npc, LivingEntity currentTarget, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime
   ) {
      CompoundTag data = npc.getPersistentData();
      if (!data.contains(TAG_PENDING_MG_MAGIC)) {
         return false;
      } else {
         String magicId = data.getString(TAG_PENDING_MG_MAGIC);
         if (magicId == null || magicId.isEmpty()) {
            clearPendingMachineGun(npc);
            return false;
         } else if (gameTime < data.getLong(TAG_PENDING_MG_NEXT_TICK)) {
            return true;
         } else {
            LivingEntity target = currentTarget;
            if ((target == null || !target.isAlive()) && data.contains(TAG_PENDING_MG_TARGET)) {
               try {
                  UUID id = UUID.fromString(data.getString(TAG_PENDING_MG_TARGET));
                  Entity maybe = ((ServerLevel)npc.level()).getEntity(id);
                  if (maybe instanceof LivingEntity living && living.isAlive()) {
                     target = living;
                  }
               } catch (Exception ignored) {
               }
            }

            if (target == null || !target.isAlive()) {
               clearPendingMachineGun(npc);
               return false;
            } else {
               if (!hasPendingMachineGunPath(npc, target, magicId, data.getInt(TAG_PENDING_MG_MODE))) {
                  int blockedRetries = data.getInt(TAG_PENDING_MG_BLOCKED_RETRIES) + 1;
                  if (blockedRetries >= MAX_PENDING_MG_BLOCKED_RETRIES) {
                     clearPendingMachineGun(npc);
                     return false;
                  }

                  data.putInt(TAG_PENDING_MG_BLOCKED_RETRIES, blockedRetries);
                  data.putLong(TAG_PENDING_MG_NEXT_TICK, gameTime + Math.max(4, data.getInt(TAG_PENDING_MG_INTERVAL)));
                  repositionForClearShot(npc, target, 9.0, 1.1);
                  return true;
               }

               data.putInt(TAG_PENDING_MG_BLOCKED_RETRIES, 0);
               int left = data.getInt(TAG_PENDING_MG_WAVES_LEFT);
               if (left <= 0) {
                  clearPendingMachineGun(npc);
                  return false;
               } else {
                  int mode = data.getInt(TAG_PENDING_MG_MODE);
                  int charge = Math.max(1, data.getInt(TAG_PENDING_MG_CHARGE));
                  int shotCount = Math.max(1, data.getInt(TAG_PENDING_MG_SHOT_COUNT));
                  if ("gandr_machine_gun".equals(magicId)) {
                     markCastingPose(npc, 8);
                     double proficiency = vars == null ? 0.0 : vars.proficiency_gander;
                     if (mode == 1) {
                        fireNpcBarrage(npc, target, shotCount, charge, proficiency);
                     } else {
                        fireGandrRapidWave(npc, target, charge, proficiency);
                     }
                  } else if ("jewel_machine_gun".equals(magicId)) {
                     markCastingPose(npc, 8);
                     fireJewelMachineGunWave(npc, target, vars, mode == 1);
                  }

                  data.putInt(TAG_PENDING_MG_WAVES_LEFT, left - 1);
                  data.putLong(TAG_PENDING_MG_NEXT_TICK, gameTime + Math.max(2, data.getInt(TAG_PENDING_MG_INTERVAL)));
                  if (left - 1 <= 0) {
                     clearPendingMachineGun(npc);
                  }

                  setCastLockUntil(npc, gameTime + 4L);
                  return true;
               }
            }
         }
      }
   }

   private static NpcMagicCastBridge.BehaviorProfile buildBehaviorProfile(
      NpcCombatPersonality personality, NpcCombatTemperament temperament
   ) {
      int p = switch (personality == null ? NpcCombatPersonality.NEUTRAL : personality) {
         case GOOD -> 0;
         case NEUTRAL -> 1;
         case EVIL -> 2;
      };
      int t = switch (temperament == null ? NpcCombatTemperament.STEADY : temperament) {
         case TIMID -> 0;
         case STEADY -> 1;
         case BOLD -> 2;
      };
      return BEHAVIOR_PROFILE_MATRIX[p][t];
   }

   private static boolean tryMeleeSkillCombo(
      MysticMagicianEntity npc,
      LivingEntity target,
      TypeMoonWorldModVariables.PlayerVariables vars,
      long gameTime,
      NpcMagicCastBridge.MagicCapabilityProfile capabilities,
      NpcMagicCastBridge.BehaviorProfile behavior
   ) {
      if (npc == null || target == null || !target.isAlive()) {
         return false;
      } else {
         double distance = Math.sqrt(npc.distanceToSqr(target));
         double trigger = behavior == null ? 2.4 : behavior.meleeSkillTriggerDistance();
         double skillReach = Math.max(3.25, trigger + 0.8);
         if (distance > skillReach || Math.abs(target.getY() - npc.getY()) > 2.5 || (!npc.hasLineOfSight(target) && distance > 2.15)) {
            return false;
         } else {
            CompoundTag data = npc.getPersistentData();
            boolean rangedOnly = capabilities.hasRanged() && !capabilities.hasMeleeBurst();
            if (rangedOnly) {
               if (distance <= 3.0 && gameTime >= data.getLong(TAG_MELEE_KICK_CD) && performWhipKick(npc, target, gameTime)) {
                  data.putLong(TAG_MELEE_KICK_CD, gameTime + 20L);
                  return true;
               }

               if (distance <= 2.7 && gameTime >= data.getLong(TAG_MELEE_PUNCH_CD) && performPunch(npc, target, gameTime)) {
                  data.putLong(TAG_MELEE_PUNCH_CD, gameTime + 10L);
                  return true;
               }

               return false;
            } else {
               if (distance <= 2.75 && gameTime >= data.getLong(TAG_MELEE_THROW_CD) && npc.getRandom().nextFloat() < 0.42F && performUpperThrow(npc, target, gameTime)) {
                  data.putLong(TAG_MELEE_THROW_CD, gameTime + 52L);
                  if (gameTime >= data.getLong(TAG_MELEE_SLAM_CD) && performSlam(npc, target, vars, gameTime, 2.6)) {
                     data.putLong(TAG_MELEE_SLAM_CD, gameTime + 78L);
                  }

                  return true;
               } else if (distance <= 3.25 && gameTime >= data.getLong(TAG_MELEE_KICK_CD) && performWhipKick(npc, target, gameTime)) {
                  data.putLong(TAG_MELEE_KICK_CD, gameTime + 20L);
                  return true;
               } else if (distance <= 2.8 && gameTime >= data.getLong(TAG_MELEE_PUNCH_CD) && performPunch(npc, target, gameTime)) {
                  data.putLong(TAG_MELEE_PUNCH_CD, gameTime + 10L);
                  return true;
               } else if (distance <= 3.0 && gameTime >= data.getLong(TAG_MELEE_SLAM_CD) && npc.getRandom().nextFloat() < 0.28F) {
                  if (performSlam(npc, target, vars, gameTime, 3.2)) {
                     data.putLong(TAG_MELEE_SLAM_CD, gameTime + 80L);
                     return true;
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            }
         }
      }
   }

   private static boolean performPunch(MysticMagicianEntity npc, LivingEntity target, long gameTime) {
      if (npc == null || target == null || !target.isAlive()) {
         return false;
      } else {
         faceCasterToDirection(npc, target.position().subtract(npc.position()).normalize());
         markMeleePose(npc, MysticMagicianEntity.MELEE_POSE_PUNCH, 8);
         float damage = computeMartialDamage(npc, 1.25, 1.0, 3.0, 14.0);
         if (!hurtWithNpcMelee(npc, target, damage)) {
            return false;
         } else {
            target.knockback(0.4, npc.getX() - target.getX(), npc.getZ() - target.getZ());
            setCastLockUntil(npc, gameTime + 8L);
            return true;
         }
      }
   }

   private static boolean performWhipKick(MysticMagicianEntity npc, LivingEntity target, long gameTime) {
      if (npc == null || target == null || !target.isAlive()) {
         return false;
      } else {
         faceCasterToDirection(npc, target.position().subtract(npc.position()).normalize());
         markMeleePose(npc, MysticMagicianEntity.MELEE_POSE_WHIP_KICK, 10);
         float damage = computeMartialDamage(npc, 1.45, 1.4, 4.0, 16.0);
         if (!hurtWithNpcMelee(npc, target, damage)) {
            return false;
         } else {
            target.knockback(1.05, npc.getX() - target.getX(), npc.getZ() - target.getZ());
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
            setCastLockUntil(npc, gameTime + 10L);
            return true;
         }
      }
   }

   private static boolean performUpperThrow(MysticMagicianEntity npc, LivingEntity target, long gameTime) {
      if (npc == null || target == null || !target.isAlive()) {
         return false;
      } else {
         faceCasterToDirection(npc, target.position().subtract(npc.position()).normalize());
         markMeleePose(npc, MysticMagicianEntity.MELEE_POSE_UPPER_THROW, 12);
         float damage = computeMartialDamage(npc, 1.3, 1.8, 4.0, 15.0);
         if (!hurtWithNpcMelee(npc, target, damage)) {
            return false;
         } else {
            Vec3 push = target.getDeltaMovement().add(0.0, 0.8, 0.0);
            target.setDeltaMovement(push.x, push.y, push.z);
            target.hurtMarked = true;
            setCastLockUntil(npc, gameTime + 11L);
            return true;
         }
      }
   }

   private static boolean performSlam(
      MysticMagicianEntity npc, LivingEntity primaryTarget, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime, double radius
   ) {
      if (npc == null || primaryTarget == null || !primaryTarget.isAlive()) {
         return false;
      } else if (!consumeMana(vars, 8.0)) {
         return false;
      } else {
         faceCasterToDirection(npc, primaryTarget.position().subtract(npc.position()).normalize());
         markMeleePose(npc, MysticMagicianEntity.MELEE_POSE_SLAM, 12);
         float baseDamage = computeMartialDamage(npc, 1.1, 2.2, 4.0, 14.0);
         boolean hit = false;

         for (LivingEntity nearby : npc.level()
            .getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(radius, 1.5, radius), e -> e != npc && e.isAlive())) {
            if (hurtWithNpcMelee(npc, nearby, baseDamage)) {
               nearby.knockback(0.6, npc.getX() - nearby.getX(), npc.getZ() - nearby.getZ());
               nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 28, 0, false, true, true));
               hit = true;
            }
         }

         if (hit) {
            setCastLockUntil(npc, gameTime + 12L);
         }

         return hit;
      }
   }

   private static boolean hurtWithNpcMelee(MysticMagicianEntity npc, LivingEntity target, float damage) {
      target.invulnerableTime = 0;
      boolean hit = target.hurt(npc.damageSources().mobAttack(npc), damage);
      target.invulnerableTime = 0;
      return hit;
   }

   private static float computeMartialDamage(MysticMagicianEntity npc, double multiplier, double flat, double min, double max) {
      if (npc == null) {
         return (float)min;
      } else {
         double damage = readAttributeValue(npc, Attributes.ATTACK_DAMAGE, 3.0) * multiplier + flat;
         MobEffectInstance selfStrength = npc.getEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH);
         if (selfStrength != null) {
            damage += (selfStrength.getAmplifier() + 1) * 1.8;
         }

         MobEffectInstance otherStrength = npc.getEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);
         if (otherStrength != null) {
            damage += (otherStrength.getAmplifier() + 1) * 1.2;
         }

         MobEffectInstance damageBoost = npc.getEffect(MobEffects.DAMAGE_BOOST);
         if (damageBoost != null) {
            damage += (damageBoost.getAmplifier() + 1) * 3.0;
         }

         MobEffectInstance agility = npc.getEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY);
         if (agility != null) {
            damage *= 1.0 + (agility.getAmplifier() + 1) * 0.07;
         }

         MobEffectInstance sight = npc.getEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT);
         if (sight != null) {
            damage *= 1.0 + (sight.getAmplifier() + 1) * 0.03;
         }

         MobEffectInstance defense = npc.getEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE);
         if (defense != null) {
            damage *= 1.0 + (defense.getAmplifier() + 1) * 0.02;
         }

         MobEffectInstance weakness = npc.getEffect(MobEffects.WEAKNESS);
         if (weakness != null) {
            damage -= (weakness.getAmplifier() + 1) * 4.0;
         }

         return (float)Mth.clamp(damage, min, max);
      }
   }

   private static boolean spawnNpcGemProjectile(
      MysticMagicianEntity caster,
      LivingEntity target,
      GemType type,
      double speed,
      float inaccuracy,
      boolean randomMode,
      boolean machineGunMode,
      boolean cyanTornado
   ) {
      if (caster != null && type != null) {
         markCastingPose(caster, 10);
         Vec3 direction = getAimDirection(caster, target, speed, 0.04);
         faceCasterToDirection(caster, direction);
         Vec3 spawnPos = EntityUtils.getRightHandCastAnchor(caster).add(direction.scale(0.1));
         Level level = caster.level();
         if (type == GemType.SAPPHIRE) {
            SapphireProjectileEntity projectile = new SapphireProjectileEntity(level, caster);
            projectile.setItem(new ItemStack(ModItems.getNormalizedFullCarvedGem(GemType.SAPPHIRE)));
            projectile.setPos(spawnPos);
            projectile.shoot(direction.x, direction.y, direction.z, (float)speed, inaccuracy);
            level.addFreshEntity(projectile);
            return true;
         } else if (type == GemType.TOPAZ) {
            TopazProjectileEntity projectile = new TopazProjectileEntity(level, caster);
            projectile.setItem(new ItemStack(ModItems.getNormalizedFullCarvedGem(GemType.TOPAZ)));
            projectile.setPos(spawnPos);
            projectile.shoot(direction.x, direction.y, direction.z, (float)speed, inaccuracy);
            level.addFreshEntity(projectile);
            return true;
         } else {
            ItemStack gem = new ItemStack(ModItems.getNormalizedFullCarvedGem(type));
            int gemTypeId = toGemTypeId(type);
            CompoundTag custom = ((CustomData)gem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            if (randomMode) {
               custom.putBoolean("IsRandomMode", true);
            }

            if (machineGunMode) {
               custom.putBoolean("IsMachineGunMode", true);
            }

            if (cyanTornado && type == GemType.CYAN) {
               custom.putBoolean("IsCyanTornado", true);
               custom.putFloat("CyanRadius", 4.5F);
               custom.putInt("CyanDuration", 160);
            }

            gem.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
            RubyProjectileEntity projectile = new RubyProjectileEntity(level, caster);
            projectile.setGemType(gemTypeId);
            projectile.setItem(gem);
            projectile.setPos(spawnPos);
            projectile.shoot(direction.x, direction.y, direction.z, (float)speed, inaccuracy);
            level.addFreshEntity(projectile);
            return true;
         }
      } else {
         return false;
      }
   }

   private static void fireRubyFlameVolley(MysticMagicianEntity caster, LivingEntity target, double proficiency) {
      if (caster != null && target != null && target.isAlive()) {
         Vec3 forward = getAimDirection(caster, target, 2.9, 0.04);
         faceCasterToDirection(caster, forward);
         Vec3 right = getRightVector(forward);
         Vec3 up = right.cross(forward).normalize();
         Vec3 hand = EntityUtils.getRightHandCastAnchor(caster);
         float inaccuracy = proficiency >= 70.0 ? 0.05F : 0.08F;

         for (int i = 0; i < 3; i++) {
            ItemStack gem = new ItemStack(ModItems.getNormalizedFullCarvedGem(GemType.RUBY));
            RubyProjectileEntity projectile = new RubyProjectileEntity(caster.level(), caster);
            projectile.setGemType(toGemTypeId(GemType.RUBY));
            projectile.setItem(gem);
            Vec3 spawn = hand.add(forward.scale(0.08)).add(right.scale(RAPID_SIDE[i] * 0.8)).add(up.scale(RAPID_UP[i] * 0.75));
            projectile.setPos(spawn);
            Vec3 spreadDir = applyDirectionalSpread(forward, right, up, RAPID_YAW[i] * 1.2F, RAPID_PITCH[i]);
            projectile.shoot(spreadDir.x, spreadDir.y, spreadDir.z, 2.9F, inaccuracy);
            caster.level().addFreshEntity(projectile);
         }
      }
   }

   private static void spawnNpcCyanWindProjectile(MysticMagicianEntity caster, LivingEntity target, double proficiency) {
      if (caster != null && target != null && target.isAlive()) {
         Vec3 direction = getAimDirection(caster, target, 2.8, 0.04);
         faceCasterToDirection(caster, direction);
         Vec3 spawnPos = EntityUtils.getRightHandCastAnchor(caster).add(direction.scale(0.1));
         float radius = 4.5F + (float)(Mth.clamp(proficiency, 0.0, 100.0) / 100.0) * 2.0F;
         int duration = 150 + (int)Math.round(Mth.clamp(proficiency, 0.0, 100.0) * 0.9);
         ItemStack gem = new ItemStack(ModItems.getNormalizedFullCarvedGem(GemType.CYAN));
         CompoundTag custom = ((CustomData)gem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         custom.putBoolean("IsCyanTornado", true);
         custom.putFloat("CyanRadius", radius);
         custom.putInt("CyanDuration", duration);
         gem.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
         RubyProjectileEntity projectile = new RubyProjectileEntity(caster.level(), caster);
         projectile.setGemType(toGemTypeId(GemType.CYAN));
         projectile.setItem(gem);
         projectile.setPos(spawnPos);
         projectile.shoot(direction.x, direction.y, direction.z, 2.8F, proficiency >= 70.0 ? 0.05F : 0.08F);
         caster.level().addFreshEntity(projectile);
      }
   }

   private static void applyNpcWinterFrostField(
      MysticMagicianEntity caster, LivingEntity target, int radius, int duration, int primarySlownessAmplifier, int secondarySlownessAmplifier, int weaknessAmplifier
   ) {
      if (caster != null && target != null && target.isAlive()) {
         Level level = caster.level();
         BlockPos center = target.blockPosition();
         RandomSource random = level.getRandom();
         Map<Integer, List<NpcMagicCastBridge.WinterFrostRestoreData>> restoreBuckets = new HashMap<>();
         boolean airborneCast = !caster.onGround() && !caster.isInWater() && !caster.isInLava() && !caster.isFallFlying();
         int lowerDepth = airborneCast ? radius : 2;
         if (level instanceof ServerLevel serverLevel) {
            double ySpread = airborneCast ? radius * 0.45 : radius * 0.35;
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 140, radius * 0.5, ySpread, radius * 0.5, 0.05);
            serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY() + 0.8, target.getZ(), 72, radius * 0.12, airborneCast ? 1.1 : 0.8, radius * 0.12, 0.03);
            serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.4, target.getZ(), 36, radius * 0.4, airborneCast ? 0.7 : 0.45, radius * 0.4, 0.01);
         }

         int radiusSq = radius * radius;
         int baseDelay = Math.max(100, duration - 10);
         for (int x = -radius; x <= radius; x++) {
            for (int y = -lowerDepth; y <= radius; y++) {
               for (int z = -radius; z <= radius; z++) {
                  if (x * x + y * y + z * z <= radiusSq) {
                     BlockPos pos = center.offset(x, y, z);
                     if (!level.getWorldBorder().isWithinBounds(pos) || isWinterFrostProtectedPos(caster, pos)) {
                        continue;
                     }

                     BlockState state = level.getBlockState(pos);
                     if (!state.isAir() && !state.hasBlockEntity() && !(state.getDestroySpeed(level, pos) < 0.0F)) {
                        BlockState iceState = randomWinterFrostIceState(random);
                        level.setBlock(pos, iceState, 2);
                        addWinterFrostRestore(restoreBuckets, pos, state, iceState, baseDelay + random.nextInt(31) - 15);
                     }
                  }
               }
            }
         }

         AABB area = new AABB(center).inflate(radius);
         for (LivingEntity nearby : level.getEntitiesOfClass(
            LivingEntity.class, area, e -> e != null && e.isAlive() && (e == target || isValidCombatTarget(caster, e))
         )) {
            int slownessAmplifier = nearby == target ? primarySlownessAmplifier : secondarySlownessAmplifier;
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, slownessAmplifier, false, true, true));
            nearby.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.max(90, duration / 2), weaknessAmplifier, false, true, true));
            nearby.setDeltaMovement(nearby.getDeltaMovement().multiply(0.08, 0.9, 0.08));
            nearby.fallDistance = 0.0F;
            EntityUtils.triggerSwarmAnger(level, caster, nearby);
            if (nearby instanceof Mob mob) {
               mob.setTarget(caster);
            }

            placeTemporaryWinterFrostTargetCage(level, nearby, Math.max(90, duration - 20), caster, restoreBuckets);
         }

         scheduleWinterFrostRestore(level, restoreBuckets);
         level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.45F, 0.78F);
      }
   }

   private static void placeTemporaryWinterFrostTargetCage(
      Level level,
      LivingEntity trapped,
      int baseDelay,
      LivingEntity protectedCaster,
      Map<Integer, List<NpcMagicCastBridge.WinterFrostRestoreData>> restoreBuckets
   ) {
      if (level != null && trapped != null && trapped.isAlive()) {
         RandomSource random = level.getRandom();
         AABB targetBox = trapped.getBoundingBox();
         AABB cageBox = targetBox.inflate(1.0);
         int minX = Mth.floor(cageBox.minX);
         int maxX = Mth.ceil(cageBox.maxX);
         int minY = Mth.floor(cageBox.minY);
         int maxY = Mth.ceil(cageBox.maxY);
         int minZ = Mth.floor(cageBox.minZ);
         int maxZ = Mth.ceil(cageBox.maxZ);

         for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
               for (int z = minZ; z < maxZ; z++) {
                  BlockPos pos = new BlockPos(x, y, z);
                  AABB blockBox = new AABB(pos);
                  if (!blockBox.intersects(targetBox)) {
                     BlockState current = level.getBlockState(pos);
                     if (level.getWorldBorder().isWithinBounds(pos) && current.isAir() && !isWinterFrostProtectedPos(protectedCaster, pos)) {
                        BlockState iceState = randomWinterFrostIceState(random);
                        replaceBlockWithoutDrops(level, pos, iceState);
                        addWinterFrostRestore(restoreBuckets, pos, Blocks.AIR.defaultBlockState(), iceState, Math.max(80, baseDelay + random.nextInt(21) - 10));
                     }
                  }
               }
            }
         }
      }
   }

   private static void addWinterFrostRestore(
      Map<Integer, List<NpcMagicCastBridge.WinterFrostRestoreData>> restoreBuckets,
      BlockPos pos,
      BlockState originalState,
      BlockState placedState,
      int delay
   ) {
      int clampedDelay = Math.max(60, delay);
      restoreBuckets.computeIfAbsent(clampedDelay, key -> new ArrayList<>())
         .add(new NpcMagicCastBridge.WinterFrostRestoreData(pos.immutable(), originalState, placedState));
   }

   private static void scheduleWinterFrostRestore(Level level, Map<Integer, List<NpcMagicCastBridge.WinterFrostRestoreData>> restoreBuckets) {
      for (Map.Entry<Integer, List<NpcMagicCastBridge.WinterFrostRestoreData>> entry : restoreBuckets.entrySet()) {
         int delay = entry.getKey();
         List<NpcMagicCastBridge.WinterFrostRestoreData> dataList = entry.getValue();
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            for (NpcMagicCastBridge.WinterFrostRestoreData data : dataList) {
               BlockState currentState = level.getBlockState(data.pos());
               if (currentState.getBlock() == data.placedState().getBlock()) {
                  boolean exposed = false;

                  for (Direction dir : Direction.values()) {
                     if (level.getBlockState(data.pos().relative(dir)).isAir()) {
                        exposed = true;
                        break;
                     }
                  }

                  level.setBlock(data.pos(), data.originalState(), 2);
               }
            }
         });
      }
   }

   private static boolean isWinterFrostProtectedPos(LivingEntity caster, BlockPos pos) {
      if (caster == null || pos == null) {
         return false;
      } else {
         AABB safeBox = caster.getBoundingBox().inflate(0.8, 1.15, 0.8);
         if (new AABB(pos).intersects(safeBox)) {
            return true;
         } else {
            BlockPos casterPos = caster.blockPosition();
            int dx = Math.abs(pos.getX() - casterPos.getX());
            int dy = pos.getY() - casterPos.getY();
            int dz = Math.abs(pos.getZ() - casterPos.getZ());
            return dx <= 1 && dz <= 1 && dy >= -1 && dy <= 2;
         }
      }
   }

   private static BlockState randomWinterFrostIceState(RandomSource random) {
      int roll = random.nextInt(100);
      if (roll < 60) {
         return Blocks.ICE.defaultBlockState();
      } else {
         return roll < 90 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
      }
   }

   private static void placeEmeraldWinterRiverCage(Level level, BlockPos base, int radius, int height) {
      if (level != null && base != null) {
         BlockState barrier = ModBlocks.GREEN_TRANSPARENT_BLOCK.get().defaultBlockState();
         RandomSource random = level.getRandom();

         for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= height; y++) {
               for (int z = -radius; z <= radius; z++) {
                  BlockPos pos = base.offset(x, y, z);
                  boolean isBorder = Math.abs(x) == radius || Math.abs(z) == radius || y == 0 || y == height;
                  if (isBorder) {
                     BlockState current = level.getBlockState(pos);
                     if (level.getWorldBorder().isWithinBounds(pos)) {
                        if (current.is(ModBlocks.GREEN_TRANSPARENT_BLOCK.get())) {
                           level.scheduleTick(pos, ModBlocks.GREEN_TRANSPARENT_BLOCK.get(), 160 + random.nextInt(41));
                        } else if (current.getDestroySpeed(level, pos) >= 0.0F && !current.is(Blocks.BEDROCK)) {
                           replaceBlockWithoutDrops(level, pos, barrier);
                        }
                     }
                  } else {
                     BlockState innerState = level.getBlockState(pos);
                     if (!innerState.isAir()) {
                        replaceBlockWithoutDrops(level, pos, Blocks.AIR.defaultBlockState());
                     }
                  }
               }
            }
         }
      }
   }

   private static void replaceBlockWithoutDrops(Level level, BlockPos pos, BlockState targetState) {
      if (level.getBlockEntity(pos) != null) {
         level.removeBlockEntity(pos);
      }

      level.setBlock(pos, targetState, 3);
   }

   private static void pushEntitiesOutOfEmeraldBarrier(Level level, MysticMagicianEntity caster, BlockPos center, int radius, int height) {
      if (level != null && caster != null && center != null) {
         BlockPos minPos = center.offset(-radius, 0, -radius);
         BlockPos maxPos = center.offset(radius, height, radius);
         AABB box = new AABB(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX() + 1, maxPos.getY() + 1, maxPos.getZ() + 1).inflate(1.0);
         Vec3 centerPos = center.offset(0, radius, 0).getCenter();

         for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive())) {
            Vec3 direction = entity.position().subtract(centerPos);
            if (direction.lengthSqr() < 0.01) {
               direction = new Vec3(1.0, 0.0, 0.0);
            }

            direction = direction.normalize();
            Vec3 targetPos = centerPos.add(direction.scale(radius * 1.5 + 2.0));
            entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            entity.setDeltaMovement(direction.x * 0.35, Math.max(0.12, entity.getDeltaMovement().y), direction.z * 0.35);
            entity.fallDistance = 0.0F;
         }
      }
   }

   private static boolean handleHighThreatTactics(
      MysticMagicianEntity npc,
      LivingEntity target,
      TypeMoonWorldModVariables.PlayerVariables vars,
      long gameTime,
      NpcMagicCastBridge.ThreatProfile threat,
      NpcMagicCastBridge.MagicCapabilityProfile capabilities
   ) {
      steerAwayFromTarget(npc, target);
      if (gameTime >= getCastLockUntil(npc) && !(vars.player_mana <= 1.0)) {
         if (gameTime >= npc.getPersistentData().getLong(TAG_THREAT_UTILITY_TICK) && tryPreBuffForHighThreat(npc, vars, gameTime, threat, capabilities)) {
            npc.getPersistentData().putLong(TAG_THREAT_UTILITY_TICK, gameTime + 35L);
         }

         if (capabilities.hasControl()
            && gameTime >= npc.getPersistentData().getLong(TAG_THREAT_CONTROL_TICK)
            && tryHighThreatControl(npc, target, vars, gameTime, threat)) {
            npc.getPersistentData().putLong(TAG_THREAT_CONTROL_TICK, gameTime + 26L);
            return true;
         } else if (capabilities.hasRanged()
            && gameTime >= npc.getPersistentData().getLong(TAG_THREAT_COUNTER_TICK)
            && tryHighThreatCounterFire(npc, target, vars, gameTime, threat)) {
            npc.getPersistentData().putLong(TAG_THREAT_COUNTER_TICK, gameTime + 20L);
            return true;
         } else if (!capabilities.hasAnyOffense()) {
            steerAwayFromTarget(npc, target);
            if (!npc.hasEffect(MobEffects.MOVEMENT_SPEED) && consumeMana(vars, 14.0)) {
               npc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, true, true));
               setCastLockUntil(npc, gameTime + 8L);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean tryPreBuffForHighThreat(
      MysticMagicianEntity npc,
      TypeMoonWorldModVariables.PlayerVariables vars,
      long gameTime,
      NpcMagicCastBridge.ThreatProfile threat,
      NpcMagicCastBridge.MagicCapabilityProfile capabilities
   ) {
      if (!capabilities.hasBuff()) {
         return false;
      } else {
         boolean hasDefense = npc.hasEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE) || npc.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE);
         boolean hasAgility = npc.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY) || npc.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY);
         CompoundTag payload = new CompoundTag();
         payload.putInt("reinforcement_mode", threat.strongTarget() ? 0 : 2);
         payload.putInt("reinforcement_level", threat.enemyCount() >= 3 ? 3 : 2);
         if (threat.strongTarget() && !hasDefense && castReinforcement(npc, vars, payload, Math.max(70.0, vars.proficiency_reinforcement))) {
            setMagicCooldown(npc, "reinforcement", gameTime + 120L);
            return true;
         } else {
            if (threat.outnumberedOrPressured() && !hasAgility) {
               payload.putInt("reinforcement_mode", 2);
               if (castReinforcement(npc, vars, payload, Math.max(70.0, vars.proficiency_reinforcement))) {
                  setMagicCooldown(npc, "reinforcement", gameTime + 120L);
                  return true;
               }
            }

            return false;
         }
      }
   }

   private static boolean tryHighThreatControl(
      MysticMagicianEntity npc, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime, NpcMagicCastBridge.ThreatProfile threat
   ) {
      if (target == null || !target.isAlive() || npc.distanceToSqr(target) > 196.0) {
         return false;
      } else {
         if (NpcMagicExecutionService.hasCastableMagic(vars, "binding_magic")
            && gameTime >= getMagicCooldownUntil(npc, "binding_magic")
            && castBindingMagic(npc, target, vars, Math.max(threat.enemyCount() >= 2 ? 75.0 : 55.0, vars.proficiency_binding_magic))) {
            applyPostCastCooldown(npc, vars, "binding_magic", new CompoundTag(), gameTime, 10);
            return true;
         }
         if (NpcMagicExecutionService.hasCastableMagic(vars, "suggestion_magic")
            && gameTime >= getMagicCooldownUntil(npc, "suggestion_magic")
            && !MagicSuggestion.isServantTarget(target)
            && castSuggestionMagic(npc, target, vars, Math.max(50.0, vars.proficiency_suggestion_magic))) {
            applyPostCastCooldown(npc, vars, "suggestion_magic", new CompoundTag(), gameTime, 10);
            return true;
         }
         if (gameTime < getMagicCooldownUntil(npc, "gravity_magic")) {
            return false;
         }
         CompoundTag gravityPayload = new CompoundTag();
         gravityPayload.putInt("gravity_target", 1);
         gravityPayload.putInt("gravity_mode", threat.strongTarget() ? 2 : 1);
         if (!castGravity(npc, target, vars, gravityPayload, Math.max(65.0, vars.proficiency_gravity_magic))) {
            return false;
         } else {
            int gcd = 18;
            vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
            npc.getPersistentData().putLong(TAG_NEXT_GLOBAL_CAST_TICK, gameTime + gcd);
            setMagicCooldown(npc, "gravity_magic", gameTime + 55L);
            setCastLockUntil(npc, gameTime + 12L);
            return true;
         }
      }
   }

   private static boolean tryHighThreatCounterFire(
      MysticMagicianEntity npc, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime, NpcMagicCastBridge.ThreatProfile threat
   ) {
      if (target != null && target.isAlive()) {
         return npc.distanceToSqr(target) < 36.0 && threat.enemyCount() >= 2 ? false : tryFallbackRangedFire(npc, target, vars, gameTime, threat, true);
      } else {
         return false;
      }
   }

   private static void handleRetreat(MysticMagicianEntity npc, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime) {
      clearPendingMachineGun(npc);
      steerAwayFromTarget(npc, target, 16.0, 1.42);
      if (!npc.hasEffect(MobEffects.MOVEMENT_SPEED)) {
         npc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, true, true));
      }

      long lockUntil = getCastLockUntil(npc);
      if (gameTime >= lockUntil && !(vars.player_mana <= 1.0)) {
         if (trySelfGravityStabilize(npc, vars, gameTime, true)) {
            npc.getPersistentData().putLong(TAG_RETREAT_UTILITY_TICK, gameTime + 16L);
            return;
         }

         if (gameTime >= npc.getPersistentData().getLong(TAG_RETREAT_UTILITY_TICK)) {
            boolean usedUtility = tryRetreatRecoveryOrEscapeMagic(npc, target, vars, gameTime);
            if (usedUtility) {
               npc.getPersistentData().putLong(TAG_RETREAT_UTILITY_TICK, gameTime + 30L);
               return;
            }
         }

         if (gameTime >= npc.getPersistentData().getLong(TAG_RETREAT_COUNTER_TICK) && npc.distanceToSqr(target) <= 484.0) {
            CompoundTag payload = new CompoundTag();
            boolean cast = castGander(npc, target, vars, payload, Math.max(40.0, vars.proficiency_gander));
            if (cast) {
               int gcd = 14;
               vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
               npc.getPersistentData().putLong(TAG_NEXT_GLOBAL_CAST_TICK, gameTime + gcd);
               setMagicCooldown(npc, "gander", gameTime + 26L);
               setCastLockUntil(npc, gameTime + 10L);
               npc.getPersistentData().putLong(TAG_RETREAT_COUNTER_TICK, gameTime + 22L);
            }
         }
      }
   }

   private static boolean tryRetreatRecoveryOrEscapeMagic(
      MysticMagicianEntity npc, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime
   ) {
      if (target != null && target.isAlive() && npc.distanceToSqr(target) <= 144.0
         && hasCastableMagic(vars, "entity_displacement")
         && gameTime >= getMagicCooldownUntil(npc, "entity_displacement")
         && EntityDisplacementService.swapNpc(npc, target)) {
         applyPostCastCooldown(npc, vars, "entity_displacement", new CompoundTag(), gameTime, 12);
         return true;
      }

      double maxHealth = Math.max(1.0, (double)npc.getMaxHealth());
      double ratio = npc.getHealth() / maxHealth;
      if (ratio <= 0.55 && !npc.hasEffect(MobEffects.REGENERATION) && consumeMana(vars, 25.0)) {
         npc.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true));
         npc.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false, true));
         setCastLockUntil(npc, gameTime + 10L);
         return true;
      } else if (!npc.hasEffect(MobEffects.MOVEMENT_SPEED) && consumeMana(vars, 18.0)) {
         npc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, true, true));
         setCastLockUntil(npc, gameTime + 10L);
         return true;
      } else {
         if (target != null && target.isAlive() && npc.distanceToSqr(target) < 100.0) {
            CompoundTag gravityPayload = new CompoundTag();
            gravityPayload.putInt("gravity_target", 1);
            gravityPayload.putInt("gravity_mode", 1);
            boolean cast = castGravity(npc, target, vars, gravityPayload, Math.max(50.0, vars.proficiency_gravity_magic));
            if (cast) {
               setMagicCooldown(npc, "gravity_magic", gameTime + 40L);
               setCastLockUntil(npc, gameTime + 10L);
               return true;
            }
         }

         return false;
      }
   }

   private static void steerAwayFromTarget(MysticMagicianEntity npc, LivingEntity target) {
      steerAwayFromTarget(npc, target, 12.0, 1.22);
   }

   private static void steerAwayFromTarget(MysticMagicianEntity npc, LivingEntity target, double distance, double speed) {
      if (npc != null && target != null) {
         Vec3 away = npc.position().subtract(target.position());
         if (away.lengthSqr() < 1.0E-6) {
            away = npc.getLookAngle().scale(-1.0);
         }

         away = away.normalize();
         Vec3 retreat = npc.position().add(away.scale(Math.max(2.0, distance)));
         npc.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, Math.max(0.6, speed));
         npc.lookAt(target, 30.0F, 30.0F);
      }
   }

   private static long getCastLockUntil(MysticMagicianEntity npc) {
      return npc.getPersistentData().getLong(TAG_CAST_LOCK_UNTIL);
   }

   private static void setCastLockUntil(MysticMagicianEntity npc, long until) {
      npc.getPersistentData().putLong(TAG_CAST_LOCK_UNTIL, until);
   }

   private static boolean isResourceMagic(String magicId) {
      return "jewel_random_shoot".equals(magicId)
         || "jewel_machine_gun".equals(magicId)
         || isAdvancedJewelMagic(magicId)
         || NPC_JEWEL_ITEM_BASIC.equals(magicId)
         || NPC_JEWEL_ITEM_ADVANCED.equals(magicId)
         || NPC_JEWEL_ITEM_ENGRAVED.equals(magicId);
   }

   private static boolean isAdvancedJewelMagic(String magicId) {
      if (magicId != null && !magicId.isEmpty()) {
         return switch (magicId) {
            case "ruby_flame_sword", "sapphire_winter_frost", "emerald_winter_river", "topaz_reinforcement", "cyan_wind" -> true;
            default -> false;
         };
      } else {
         return false;
      }
   }

   private static int getResourceWindowTicks(String magicId) {
      return switch (magicId) {
         case "jewel_random_shoot" -> 600;
         case "jewel_machine_gun" -> 1400;
         case "ruby_flame_sword", "cyan_wind" -> 2200;
         case "topaz_reinforcement" -> 2400;
         case "sapphire_winter_frost", "emerald_winter_river" -> 2800;
         case NPC_JEWEL_ITEM_BASIC -> 1800;
         case NPC_JEWEL_ITEM_ADVANCED -> 2400;
         case NPC_JEWEL_ITEM_ENGRAVED -> 3600;
         default -> 0;
      };
   }

   private static int getResourceUseLimit(String magicId) {
      return switch (magicId) {
         case "jewel_random_shoot" -> 6;
         case "jewel_machine_gun" -> 1;
         case "ruby_flame_sword",
            "sapphire_winter_frost",
            "emerald_winter_river",
            "topaz_reinforcement",
            "cyan_wind" -> 1;
         case NPC_JEWEL_ITEM_BASIC -> 3;
         case NPC_JEWEL_ITEM_ADVANCED -> 3;
         case NPC_JEWEL_ITEM_ENGRAVED -> 1;
         default -> Integer.MAX_VALUE;
      };
   }

   private static int getResourceExhaustionCooldown(String magicId) {
      return switch (magicId) {
         case "jewel_random_shoot" -> 1800;
         case "jewel_machine_gun" -> 3200;
         case "ruby_flame_sword", "cyan_wind" -> 3600;
         case "topaz_reinforcement" -> 3800;
         case "sapphire_winter_frost", "emerald_winter_river" -> 4200;
         case NPC_JEWEL_ITEM_BASIC -> 2400;
         case NPC_JEWEL_ITEM_ADVANCED -> 3200;
         case NPC_JEWEL_ITEM_ENGRAVED -> 5200;
         default -> 0;
      };
   }

   private static boolean isResourceMagicAvailable(MysticMagicianEntity npc, String magicId, long gameTime) {
      if (!isResourceMagic(magicId)) {
         return true;
      } else if (gameTime < getMagicCooldownUntil(npc, magicId)) {
         return false;
      } else {
         CompoundTag all = npc.getPersistentData().getCompound(TAG_RESOURCE_USE);
         CompoundTag per = all.contains(magicId, 10) ? all.getCompound(magicId) : new CompoundTag();
         long windowStart = per.contains("window_start") ? per.getLong("window_start") : gameTime;
         int used = per.contains("used") ? per.getInt("used") : 0;
         int window = getResourceWindowTicks(magicId);
         int limit = getResourceUseLimit(magicId);
         if (window > 0 && limit > 0) {
            return gameTime - windowStart >= window ? true : used < limit;
         } else {
            return true;
         }
      }
   }

   private static void recordResourceMagicUse(MysticMagicianEntity npc, String magicId, long gameTime) {
      if (isResourceMagic(magicId)) {
         CompoundTag root = npc.getPersistentData();
         CompoundTag all = root.getCompound(TAG_RESOURCE_USE);
         CompoundTag per = all.contains(magicId, 10) ? all.getCompound(magicId) : new CompoundTag();
         int window = getResourceWindowTicks(magicId);
         int limit = getResourceUseLimit(magicId);
         long start = per.contains("window_start") ? per.getLong("window_start") : gameTime;
         int used = per.contains("used") ? per.getInt("used") : 0;
         if (gameTime - start >= window) {
            start = gameTime;
            used = 0;
         }

         used++;
         per.putLong("window_start", start);
         per.putInt("used", used);
         all.put(magicId, per);
         root.put(TAG_RESOURCE_USE, all);
         if (used >= limit) {
            int longCd = getResourceExhaustionCooldown(magicId);
            setMagicCooldown(npc, magicId, Math.max(getMagicCooldownUntil(npc, magicId), gameTime + longCd));
         }
      }
   }

   private static TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry chooseNextMagic(
      MysticMagicianEntity npc,
      TypeMoonWorldModVariables.PlayerVariables vars,
      LivingEntity target,
      long gameTime,
      NpcMagicCastBridge.MagicCapabilityProfile capabilities
   ) {
      List<NpcMagicCastBridge.Choice> choices = new ArrayList<>();
      NpcCombatStyle style = npc.getCombatStyle();
      double distance = Math.sqrt(npc.distanceToSqr(target));
      boolean forceMeleeEngage = shouldForceMeleeEngage(capabilities, distance);
      boolean forceRangedRetreat = shouldForceRangedRetreat(capabilities, distance);
      if (forceRangedRetreat) {
         return null;
      } else {
         double manaRatio = vars.player_max_mana <= 0.0 ? 0.0 : Mth.clamp(vars.player_mana / vars.player_max_mana, 0.0, 1.0);
         Provider lookup = npc.registryAccess();

         for (int slot = 0; slot < 12; slot++) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
            if (entry != null && !entry.isEmpty() && VALID_MAGIC_IDS.contains(entry.magicId) && vars.isWheelSlotEntryCastable(entry)) {
               if ("magic_analysis".equals(entry.magicId) && MagicAnalysisService.isActive(npc, vars)) {
                  continue;
               }
               if (entry.presetPayload == null) {
                  entry.presetPayload = new CompoundTag();
               } else {
                  entry.presetPayload = NpcMagicFilterService.sanitizePresetForNpc(entry.magicId, entry.presetPayload, lookup, npc.getRandom());
               }

               boolean crestCast = "crest".equals(entry.sourceType);
               double proficiency = getEffectiveProficiency(npc, vars, entry.magicId, crestCast);
               if (canCastWithMana(vars, entry.magicId, entry.presetPayload, proficiency)
                  && gameTime >= getMagicCooldownUntil(npc, entry.magicId)
                  && isResourceMagicAvailable(npc, entry.magicId, gameTime)
                  && (!forceMeleeEngage || !isPureRangedMagic(entry.magicId))) {
                  double weight = computeMagicWeight(style, entry.magicId, entry.presetPayload, target, distance, manaRatio);
                  if (!(weight <= 0.0)) {
                     choices.add(new NpcMagicCastBridge.Choice(entry, slot, weight));
                  }
               }
            }
         }

         if (choices.isEmpty()) {
            return null;
         } else {
            applyAdvancedAndVarietyWeighting(npc, choices, manaRatio);
            applyLeffPersonalityWeighting(npc, choices, distance, manaRatio);
            choices.sort(Comparator.comparingDouble(NpcMagicCastBridge.Choice::weight).reversed());
            return weightedPick(choices, npc.getRandom()).entry();
         }
      }
   }

   private static void applyLeffPersonalityWeighting(MysticMagicianEntity npc, List<NpcMagicCastBridge.Choice> choices, double distance, double manaRatio) {
      if (!(npc instanceof LeffLaynorFlaurosEntity) || choices == null || choices.isEmpty()) {
         return;
      }
      int personality = Mth.clamp(npc.getPersistentData().getInt(LeffLaynorFlaurosEntity.TAG_PERSONALITY), 0, 2);
      for (int i = 0; i < choices.size(); i++) {
         NpcMagicCastBridge.Choice c = choices.get(i);
         String magicId = c.entry().magicId;
         double weight = c.weight();
         if ("imaginary_displacement".equals(magicId)) {
            CompoundTag data = npc.getPersistentData();
            boolean ready = data.getInt(LeffLaynorFlaurosEntity.TAG_IMAGINARY_DISPLACEMENT_ACTIVE) <= 0
               && data.getInt(LeffLaynorFlaurosEntity.TAG_IMAGINARY_DISPLACEMENT_COOLDOWN) <= 0;
            weight *= ready && manaRatio >= 0.12 ? 2.4 : 0.25;
         }
         if (personality == 0) {
            if ("imaginary_displacement".equals(magicId)) weight *= 3.4;
            if ("magic_analysis".equals(magicId)) weight *= 2.2;
            if ("reinforcement".equals(magicId)) weight *= 1.75;
            if ("spiritron_cannon".equals(magicId)) weight *= 0.28;
         } else if (personality == 1) {
            if ("airflow_blade".equals(magicId)) weight *= 2.2;
            if ("suggestion_magic".equals(magicId)) weight *= 1.9;
            if ("reinforcement".equals(magicId)) weight *= 1.5;
            if ("detection".equals(magicId)) weight *= 1.8;
            if ("spiritron_cannon".equals(magicId)) weight *= 0.45;
         } else {
            if ("spiritron_cannon".equals(magicId)) {
               weight *= distance >= 12.0 && manaRatio >= 0.72 ? 4.0 : 0.16;
            }
            if ("aerial_ascent".equals(magicId) || "aerial_stasis".equals(magicId)) weight *= 1.45;
            if ("airflow_blade".equals(magicId)) weight *= distance >= 7.0 ? 1.35 : 0.75;
         }
         choices.set(i, new NpcMagicCastBridge.Choice(c.entry(), c.slot(), Math.max(0.01, weight)));
      }
   }

   private static void applyAdvancedAndVarietyWeighting(MysticMagicianEntity npc, List<NpcMagicCastBridge.Choice> choices, double manaRatio) {
      if (npc != null && choices != null && !choices.isEmpty()) {
         boolean hasGandrMachineGun = hasMagicChoice(choices, "gandr_machine_gun");
         boolean hasGander = hasMagicChoice(choices, "gander");
         boolean hasJewelMachineGun = hasMagicChoice(choices, "jewel_machine_gun");
         boolean hasJewelRandom = hasMagicChoice(choices, "jewel_random_shoot");
         String lastMagic = npc.getPersistentData().getString(TAG_LAST_CAST_MAGIC);
         int repeat = npc.getPersistentData().getInt(TAG_CAST_REPEAT_COUNT);

         for (int i = 0; i < choices.size(); i++) {
            NpcMagicCastBridge.Choice c = choices.get(i);
            String magicId = c.entry().magicId;
            double w = c.weight();
            if (hasGandrMachineGun && hasGander && manaRatio >= 0.42) {
               if ("gandr_machine_gun".equals(magicId)) {
                  w *= 1.85;
               } else if ("gander".equals(magicId)) {
                  w *= 0.55;
               }
            }

            if (hasJewelMachineGun && hasJewelRandom && manaRatio >= 0.5) {
               if ("jewel_machine_gun".equals(magicId)) {
                  w *= 1.6;
               } else if ("jewel_random_shoot".equals(magicId)) {
                  w *= 0.7;
               }
            }

            if (choices.size() > 1 && magicId != null && !magicId.isEmpty()) {
               if (magicId.equals(lastMagic)) {
                  double penalty = switch (Math.min(4, Math.max(1, repeat))) {
                     case 1 -> 0.55;
                     case 2 -> 0.34;
                     case 3 -> 0.22;
                     default -> 0.14;
                  };
                  w *= penalty;
               } else {
                  w *= 1.08;
               }

               if ("gandr_machine_gun".equals(lastMagic) && "gander".equals(magicId)) {
                  w *= 1.22;
               } else if ("gander".equals(lastMagic) && "gandr_machine_gun".equals(magicId) && manaRatio >= 0.42) {
                  w *= 1.28;
               } else if ("jewel_machine_gun".equals(lastMagic) && "jewel_random_shoot".equals(magicId)) {
                  w *= 1.18;
               } else if ("jewel_random_shoot".equals(lastMagic) && "jewel_machine_gun".equals(magicId) && manaRatio >= 0.5) {
                  w *= 1.2;
               }
            }

            choices.set(i, new NpcMagicCastBridge.Choice(c.entry(), c.slot(), Math.max(0.01, w)));
         }
      }
   }

   private static boolean hasMagicChoice(List<NpcMagicCastBridge.Choice> choices, String magicId) {
      if (choices != null && magicId != null && !magicId.isEmpty()) {
         for (NpcMagicCastBridge.Choice choice : choices) {
            if (choice != null && choice.entry() != null && magicId.equals(choice.entry().magicId)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static NpcMagicCastBridge.Choice weightedPick(List<NpcMagicCastBridge.Choice> choices, RandomSource random) {
      double total = 0.0;

      for (NpcMagicCastBridge.Choice choice : choices) {
         total += choice.weight();
      }

      if (total <= 0.0) {
         return choices.get(0);
      } else {
         double roll = random.nextDouble() * total;
         double running = 0.0;

         for (NpcMagicCastBridge.Choice choice : choices) {
            running += choice.weight();
            if (roll <= running) {
               return choice;
            }
         }

         return choices.get(choices.size() - 1);
      }
   }

   private static double computeMagicWeight(
      NpcCombatStyle style, String magicId, CompoundTag payload, LivingEntity target, double distance, double manaRatio
   ) {
      double weight = 1.0;
      switch (style) {
         case CLOSE_PRESSURE:
            if ("reinforcement".equals(magicId) || "topaz_reinforcement".equals(magicId) || "mana_burst".equals(magicId)) {
               weight += 2.8;
            }

            if ("gravity_magic".equals(magicId) || "binding_magic".equals(magicId)) {
               weight += 1.5;
            }

            if ("gander".equals(magicId)
               || "magic_bullet".equals(magicId)
               || "fire_magic".equals(magicId)
               || "spiritual_healing".equals(magicId)
               || "earth_magic".equals(magicId)
               || "emerald_winter_river".equals(magicId)) {
               weight++;
            }
            break;
         case RANGED_BURST:
            if ("gandr_machine_gun".equals(magicId) || "jewel_machine_gun".equals(magicId)) {
               weight += 3.0;
            }

            if ("spiritron_cannon".equals(magicId)) {
               weight += distance >= 10.0 ? 2.4 : -1.2;
            }

            if ("jewel_random_shoot".equals(magicId) || "magic_bullet".equals(magicId) || "fire_magic".equals(magicId) || isManaBurstDirect(payload, magicId)) {
               weight++;
            }

            if ("gander".equals(magicId) || "water_magic".equals(magicId) || "wind_magic".equals(magicId) || "ruby_flame_sword".equals(magicId) || "cyan_wind".equals(magicId)) {
               weight++;
            }
            break;
         case CONTROL_DRAIN:
            if ("gravity_magic".equals(magicId) || "binding_magic".equals(magicId)) {
               weight += 3.2;
            }

            if ("imaginary_displacement".equals(magicId)) {
               weight += 2.8;
            } else if ("imaginary_space".equals(magicId)) {
               weight += distance <= 18.0 ? 1.5 : -0.6;
            }

            if ("suggestion_magic".equals(magicId)
               || "water_magic".equals(magicId)
               || "wind_magic".equals(magicId)
               || "earth_magic".equals(magicId)
               || "sapphire_winter_frost".equals(magicId)
               || "emerald_winter_river".equals(magicId)) {
               weight += 1.4;
            }

            if ("gander".equals(magicId)) {
               weight++;
            }

            if ("reinforcement".equals(magicId) || "topaz_reinforcement".equals(magicId) || "mana_burst".equals(magicId)) {
               weight += 0.8;
            }
            break;
         case BALANCED:
            if ("gravity_magic".equals(magicId)
               || "binding_magic".equals(magicId)
               || "magic_bullet".equals(magicId)
               || "fire_magic".equals(magicId)
               || "water_magic".equals(magicId)
               || "wind_magic".equals(magicId)
               || "earth_magic".equals(magicId)
               || "gander".equals(magicId)
               || "ruby_flame_sword".equals(magicId)
               || "sapphire_winter_frost".equals(magicId)
               || "emerald_winter_river".equals(magicId)
               || "cyan_wind".equals(magicId)
               || "imaginary_space".equals(magicId)
               || "spiritron_cannon".equals(magicId)) {
               weight++;
            }
      }

      if (isAdvancedJewelMagic(magicId)) {
         weight *= 0.22;
      }

      if (distance < 5.0) {
         if ("reinforcement".equals(magicId)
            || "healing_magic".equals(magicId)
            || "mana_burst".equals(magicId)
            || "gravity_magic".equals(magicId)
            || "binding_magic".equals(magicId)
            || "water_magic".equals(magicId)
            || "wind_magic".equals(magicId)
            || "earth_magic".equals(magicId)
            || "topaz_reinforcement".equals(magicId)) {
            weight++;
         }

         if ("gandr_machine_gun".equals(magicId)
            || "jewel_machine_gun".equals(magicId)
            || "ruby_flame_sword".equals(magicId)
            || "cyan_wind".equals(magicId)
            || "spiritron_cannon".equals(magicId)) {
            weight *= 0.65;
         }
      } else if (distance > 14.0) {
         if ("reinforcement".equals(magicId) || "healing_magic".equals(magicId) || "topaz_reinforcement".equals(magicId) || !isManaBurstDirect(payload, magicId) && "mana_burst".equals(magicId)) {
            weight *= 0.45;
         }

         if ("gandr_machine_gun".equals(magicId)
            || "jewel_machine_gun".equals(magicId)
            || "gander".equals(magicId)
            || "magic_bullet".equals(magicId)
            || isManaBurstDirect(payload, magicId)
            || "fire_magic".equals(magicId)
            || "water_magic".equals(magicId)
            || "wind_magic".equals(magicId)
            || "ruby_flame_sword".equals(magicId)
            || "cyan_wind".equals(magicId)
            || "spiritron_cannon".equals(magicId)) {
            weight += 0.8;
         }
      }

      if (manaRatio < 0.3) {
         if ("gandr_machine_gun".equals(magicId) || "jewel_machine_gun".equals(magicId) || isAdvancedJewelMagic(magicId) || "spiritron_cannon".equals(magicId)) {
            weight *= 0.2;
         } else if ("gander".equals(magicId) || "jewel_random_shoot".equals(magicId) || "gravity_magic".equals(magicId)) {
            weight *= 0.75;
         }
      }

      if ("ruby_flame_sword".equals(magicId) && distance >= 6.0 && distance <= 14.0 && manaRatio >= 0.55) {
         weight *= 1.35;
      }

      if ("sapphire_winter_frost".equals(magicId) && distance <= 9.0 && manaRatio >= 0.55) {
         weight *= 1.42;
      }

      if ("emerald_winter_river".equals(magicId) && distance <= 8.0 && manaRatio >= 0.6) {
         weight *= 1.45;
      }

      if ("topaz_reinforcement".equals(magicId) && distance <= 6.0 && manaRatio >= 0.45) {
         weight *= 1.6;
      }

      if ("cyan_wind".equals(magicId) && distance >= 7.0 && distance <= 16.0 && manaRatio >= 0.55) {
         weight *= 1.32;
      }

      if ("healing_magic".equals(magicId)) {
         weight += manaRatio >= 0.25 ? 0.6 : -0.8;
      } else if ("mana_burst".equals(magicId)) {
         int mode = manaBurstMode(payload);
         if (mode == 2) {
            weight += distance >= 5.0 && distance <= 18.0 ? 1.5 : -0.45;
            if (manaRatio < 0.55) weight *= 0.35;
         } else {
            weight += distance <= 6.0 ? 1.7 : 0.35;
            if (manaRatio < 0.35) weight *= 0.55;
         }
      } else if ("spiritual_healing".equals(magicId)) {
         weight += isSpiritLikeTarget(target) ? 1.8 : 0.35;
      } else if ("magic_analysis".equals(magicId)) {
         weight += isLikelyMageTarget(target) ? 2.0 : 0.45;
         if (manaRatio < 0.22) {
            weight *= 0.45;
         }
      } else if ("binding_magic".equals(magicId) && distance <= 8.0) {
         weight += 1.6;
      } else if ("suggestion_magic".equals(magicId) && distance >= 4.0 && distance <= 12.0) {
         weight += 0.8;
      } else if ("imaginary_space".equals(magicId) && distance <= 14.0 && manaRatio >= 0.35) {
         weight += 1.2;
      } else if ("spiritron_cannon".equals(magicId)) {
         weight += distance >= 10.0 && distance <= 32.0 && manaRatio >= 0.68 ? 2.0 : -0.9;
      }

      if (isElementalMagic(magicId)) {
         if (distance >= 7.0 && distance <= 24.0) {
            weight += "fire_magic".equals(magicId) ? 1.1 : 0.7;
         }
         if (payload != null && payload.contains("element_mode") && payload.getInt("element_mode") == 1) {
            weight += distance <= 8.0 ? 1.2 : -0.25;
         }
      }

      if ("gravity_magic".equals(magicId) && payload.contains("gravity_target") && payload.getInt("gravity_target") == 0 && distance < 6.0) {
         weight += 0.5;
      }

      if ("jewel_machine_gun".equals(magicId) && payload != null && payload.contains("jewel_machine_gun_mode")) {
         int mode = Mth.clamp(payload.getInt("jewel_machine_gun_mode"), 0, 1);
         if (mode == 1 && distance < 7.0) {
            weight += 1.2;
         } else if (mode == 1) {
            weight *= 0.8;
         }
      }

      return weight;
   }

   private static boolean isLikelyMageTarget(LivingEntity target) {
      if (target == null) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = target.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars != null && (vars.is_magus || vars.player_max_mana >= 100.0 || !vars.learned_magics.isEmpty());
   }

   private static boolean isSpiritLikeTarget(LivingEntity target) {
      if (target == null) {
         return false;
      }
      return target instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity
         || target instanceof net.minecraft.world.entity.monster.Zombie
         || target instanceof net.minecraft.world.entity.monster.Skeleton
         || target instanceof net.minecraft.world.entity.monster.Stray
         || target instanceof net.minecraft.world.entity.monster.Husk
         || target instanceof net.minecraft.world.entity.monster.Drowned
         || target instanceof net.minecraft.world.entity.monster.WitherSkeleton
         || target instanceof net.minecraft.world.entity.boss.wither.WitherBoss
         || target instanceof net.minecraft.world.entity.monster.ZombifiedPiglin;
   }

   private static boolean shouldForceMeleeEngage(NpcMagicCastBridge.MagicCapabilityProfile capabilities, double distance) {
      return capabilities == null ? false : capabilities.hasMeleeBurst() && distance <= 4.2;
   }

   private static boolean shouldForceRangedRetreat(NpcMagicCastBridge.MagicCapabilityProfile capabilities, double distance) {
      return capabilities == null ? false : capabilities.hasRanged() && !capabilities.hasMeleeBurst() && distance <= 5.0;
   }

   private static boolean isPureRangedMagic(String magicId) {
      if (magicId != null && !magicId.isEmpty()) {
         return switch (magicId) {
            case "gander",
               "gandr_machine_gun",
               "airflow_blade",
               "jewel_random_shoot",
               "jewel_machine_gun",
               "magic_bullet",
               "suggestion_magic",
               "fire_magic",
               "water_magic",
               "wind_magic",
               "imaginary_space",
               "spiritron_cannon",
               "ruby_flame_sword",
               "sapphire_winter_frost",
               "emerald_winter_river",
               "cyan_wind" -> true;
            default -> false;
         };
      } else {
         return false;
      }
   }

   private static boolean canCastWithMana(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, CompoundTag payload, double proficiency) {
      return vars.player_mana >= estimateManaCost(magicId, payload, proficiency);
   }

   private static double estimateManaCost(String magicId, CompoundTag payload, double proficiency) {
      return switch (magicId) {
         case "gander" -> getGanderChargeSeconds(proficiency) * 5.0;
         case "gandr_machine_gun" -> isBarrageMode(payload)
            ? getGandrBarrageWaveCount(proficiency) * Math.max(4, Math.min(8, getBarrageShotCount(proficiency) / 2)) * 20.0
            : getGandrRapidWaveCount(proficiency) * 3.0 * 20.0;
         case "gravity_magic" -> 20.0;
         case "reinforcement" -> 20.0 * Mth.clamp(payload.getInt("reinforcement_level"), 1, 5);
         case "mana_burst" -> estimateManaBurstCost(payload, proficiency);
         case "jewel_random_shoot" -> 30.0;
         case "healing_magic" -> 12.0 + proficiency * 0.08;
         case "spiritual_healing" -> 15.0;
         case "magic_analysis" -> 8.0;
         case "magic_bullet" -> 8.0 + proficiency * 0.06;
         case "airflow_blade" -> AirflowBladeService.BLADE_MANA_COST;
         case "detection" -> 6.0;
         case "imaginary_displacement" -> 70.0 + Math.max(0.0, 100.0 - proficiency) * 0.25;
         case "imaginary_space" -> 100.0;
         case "spiritron_cannon" -> SpiritronCannonService.MANA_COST;
         case "suggestion_magic" -> 10.0 + proficiency * 0.08;
         case "binding_magic" -> 12.0 + proficiency * 0.08;
         case "fire_magic" -> 10.0 + proficiency * 0.12;
         case "water_magic", "earth_magic" -> 9.0 + proficiency * 0.10;
         case "wind_magic" -> 8.0 + proficiency * 0.09;
         case "jewel_machine_gun" -> {
            boolean defensive = payload != null && payload.contains("jewel_machine_gun_mode") && Mth.clamp(payload.getInt("jewel_machine_gun_mode"), 0, 1) == 1;
            int waves = getJewelMachineGunWaveCount(proficiency, defensive);
            yield defensive ? 45.0 + waves * 18.0 : 60.0 + waves * 22.0;
         }
         case "ruby_flame_sword" -> 72.0;
         case "sapphire_winter_frost" -> 88.0;
         case "emerald_winter_river" -> 92.0;
         case "topaz_reinforcement" -> 76.0;
         case "cyan_wind" -> 80.0;
         default -> 40.0;
      };
   }

   private static double estimateManaBurstCost(CompoundTag payload, double proficiency) {
      int mode = manaBurstMode(payload);
      int level = manaBurstLevel(payload, proficiency);
      return mode == 2 ? MANA_BURST_DIRECT_COST[level - 1] : MANA_BURST_UPKEEP[level - 1];
   }

   private static boolean castMagic(
      MysticMagicianEntity caster,
      LivingEntity target,
      TypeMoonWorldModVariables.PlayerVariables vars,
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slot,
      double effectiveProficiency,
      long gameTime
   ) {
      return NpcMagicExecutionService.castMagic(caster, target, vars, slot, effectiveProficiency, gameTime);
   }

   private static int getGlobalCooldownAfterCast(String magicId, CompoundTag payload) {
      return NpcMagicExecutionService.getGlobalCooldownAfterCast(magicId, payload);
   }

   private static int getPerMagicCooldown(String magicId, CompoundTag payload) {
      return NpcMagicExecutionService.getPerMagicCooldown(magicId, payload);
   }

   private static boolean tryEmergencyHealingMagic(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime) {
      if (npc == null || vars == null) {
         return false;
      }
      double ratio = npc.getHealth() / Math.max(1.0F, npc.getMaxHealth());
      if (ratio > 0.55) {
         return false;
      }
      boolean casted = false;
      String cooldownMagicId = "healing_magic";
      if (NpcMagicExecutionService.hasCastableMagic(vars, "healing_magic") && gameTime >= getMagicCooldownUntil(npc, "healing_magic")) {
         casted = castHealingMagic(npc, npc, vars, Math.max(45.0, vars.proficiency_healing_magic));
      }
      if (!casted && NpcMagicExecutionService.hasCastableMagic(vars, "spiritual_healing") && gameTime >= getMagicCooldownUntil(npc, "spiritual_healing")) {
         casted = castSpiritualHealing(npc, npc, vars, Math.max(45.0, vars.proficiency_spiritual_healing));
         cooldownMagicId = "spiritual_healing";
      }
      if (!casted) {
         return false;
      }
      applyPostCastCooldown(npc, vars, cooldownMagicId, new CompoundTag(), gameTime, 10);
      return true;
   }

   static boolean castHealingMagic(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || vars == null) {
         return false;
      }
      LivingEntity healTarget = caster;
      double cost = estimateManaCost("healing_magic", new CompoundTag(), proficiency);
      if (!consumeMana(vars, cost)) {
         return false;
      }
      markCastingPose(caster, 12);
      return MagicHealing.healDirect(caster, healTarget, vars, proficiency);
   }

   static boolean castSpiritronCannon(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || vars == null || target == null || !target.isAlive()) {
         return false;
      }
      if (!consumeMana(vars, SpiritronCannonService.MANA_COST)) {
         return false;
      }
      markCastingPose(caster, SpiritronCannonService.WINDUP_TICKS + 10);
      return SpiritronCannonService.cast(caster, vars, target, false);
   }

   static boolean castLeffImaginaryDisplacement(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (!(caster instanceof LeffLaynorFlaurosEntity leff) || vars == null) {
         return false;
      }
      CompoundTag data = leff.getPersistentData();
      if (data.getInt(LeffLaynorFlaurosEntity.TAG_IMAGINARY_DISPLACEMENT_ACTIVE) > 0
         || data.getInt(LeffLaynorFlaurosEntity.TAG_IMAGINARY_DISPLACEMENT_COOLDOWN) > 0) {
         return false;
      }
      double cost = 70.0 + Math.max(0.0, 100.0 - proficiency) * 0.25;
      if (!consumeMana(vars, cost)) {
         return false;
      }
      int duration = Mth.clamp(50 + (int)Math.round(proficiency * 0.45), 50, 95);
      data.putInt(LeffLaynorFlaurosEntity.TAG_IMAGINARY_DISPLACEMENT_ACTIVE, duration);
      data.putInt(LeffLaynorFlaurosEntity.TAG_IMAGINARY_DISPLACEMENT_COOLDOWN, 260);
      markCastingPose(leff, 14);
      if (leff.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.PORTAL, leff.getX(), leff.getY() + leff.getBbHeight() * 0.55, leff.getZ(), 54, 0.8, 0.9, 0.8, 0.16);
         serverLevel.playSound(null, leff.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.9F, 0.72F);
      }
      return true;
   }

   static boolean castLeffImaginarySpace(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (!(caster instanceof LeffLaynorFlaurosEntity) || vars == null || target == null || !target.isAlive()) {
         return false;
      }
      if (!consumeMana(vars, 100.0)) {
         return false;
      }
      markCastingPose(caster, 20);
      var outcome = ImaginarySpaceService.castNpcCreature(caster, target);
      if (outcome.status() != CastStatus.SUCCESS) {
         vars.player_mana = Math.min(vars.player_max_mana, vars.player_mana + 100.0);
         return false;
      }
      MagicProficiencyService.add(vars, "imaginary_space", 0.2);
      return true;
   }

   static boolean castSpiritualHealing(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || vars == null) {
         return false;
      }
      LivingEntity healTarget = target == null ? caster : target;
      double cost = estimateManaCost("spiritual_healing", new CompoundTag(), proficiency);
      if (!consumeMana(vars, cost)) {
         return false;
      }
      markCastingPose(caster, 12);
      return net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicSpiritualHealing.healDirect(caster, healTarget, vars, proficiency);
   }

   static boolean castMagicAnalysis(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || vars == null || MagicAnalysisService.isActive(caster, vars)) {
         return false;
      } else if (!consumeMana(vars, estimateManaCost("magic_analysis", new CompoundTag(), proficiency))) {
         return false;
      }
      vars.magic_analysis_active = true;
      vars.is_magic_circuit_open = true;
      markCastingPose(caster, 8);
      if (target != null && target.isAlive()) {
         caster.lookAt(target, 42.0F, 42.0F);
      }
      return true;
   }

   static boolean castMagicBullet(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null) {
         return false;
      } else if (!hasProjectilePath(caster, target, MagicMagicBullet.speed(proficiency), 0.02)) {
         repositionForClearShot(caster, target, 9.0, 1.08);
         return false;
      } else if (!consumeMana(vars, estimateManaCost("magic_bullet", new CompoundTag(), proficiency))) {
         return false;
      }
      markCastingPose(caster, 8);
      return MagicMagicBullet.castDirect(caster, target, vars, proficiency);
   }

   static boolean castAirflowBlade(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null) {
         return false;
      } else if (!hasProjectilePath(caster, target, AirflowBladeService.BLADE_SPEED, 0.0)) {
         repositionForClearShot(caster, target, 11.0, 1.12);
         return false;
      } else if (!consumeMana(vars, estimateManaCost("airflow_blade", new CompoundTag(), proficiency))) {
         return false;
      }
      markCastingPose(caster, 10);
      Vec3 direction = getAimDirection(caster, target, AirflowBladeService.BLADE_SPEED, 0.0);
      faceCasterToDirection(caster, direction);
      return AirflowBladeService.castNpcBlade(caster, target, proficiency);
   }

   static boolean castDetection(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || vars == null) {
         return false;
      } else if (!consumeMana(vars, estimateManaCost("detection", new CompoundTag(), proficiency))) {
         return false;
      }
      markCastingPose(caster, 8);
      if (target != null && target.isAlive()) {
         caster.lookAt(target, 45.0F, 45.0F);
      }
      return DetectionService.castNpcDetection(caster, target, proficiency);
   }

   static boolean castSuggestionMagic(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null || caster.distanceToSqr(target) > 256.0) {
         return false;
      } else if (MagicSuggestion.isServantTarget(target)) {
         return false;
      } else if (!MagicSuggestion.canControlTarget(target)) {
         return false;
      } else if (!consumeMana(vars, estimateManaCost("suggestion_magic", new CompoundTag(), proficiency))) {
         return false;
      }
      markCastingPose(caster, 12);
      caster.lookAt(target, 38.0F, 38.0F);
      return MagicSuggestion.castDirect(caster, target, vars, proficiency);
   }

   static boolean castBindingMagic(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null || caster.distanceToSqr(target) > 256.0) {
         return false;
      } else if (!consumeMana(vars, estimateManaCost("binding_magic", new CompoundTag(), proficiency))) {
         return false;
      }
      markCastingPose(caster, proficiency >= 75.0 ? 14 : 10);
      caster.lookAt(target, 38.0F, 38.0F);
      return MagicBinding.castDirect(caster, target, vars, proficiency);
   }

   static boolean castFireMagic(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      return castElementalMagic(caster, target, vars, payload, proficiency, "fire_magic");
   }

   static boolean castWaterMagic(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      return castElementalMagic(caster, target, vars, payload, proficiency, "water_magic");
   }

   static boolean castWindMagic(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      return castElementalMagic(caster, target, vars, payload, proficiency, "wind_magic");
   }

   static boolean castEarthMagic(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      return castElementalMagic(caster, target, vars, payload, proficiency, "earth_magic");
   }

   private static boolean castElementalMagic(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency, String magicId
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null) {
         return false;
      }
      boolean utility = shouldUseElementUtility(caster, target, payload, proficiency, magicId);
      if (!utility && !hasProjectilePath(caster, target, elementalProjectileSpeed(magicId, proficiency), 0.0)) {
         repositionForClearShot(caster, target, 9.0, 1.08);
         return false;
      } else if (!consumeMana(vars, estimateManaCost(magicId, payload == null ? new CompoundTag() : payload, proficiency))) {
         return false;
      }
      markCastingPose(caster, utility ? 14 : 10);
      caster.lookAt(target, 38.0F, 38.0F);
      return switch (magicId) {
         case "fire_magic" -> MagicFireElement.castDirect(caster, target, vars, proficiency, utility);
         case "water_magic" -> MagicWaterElement.castDirect(caster, target, vars, proficiency, utility);
         case "wind_magic" -> MagicWindElement.castDirect(caster, target, vars, proficiency, utility);
         case "earth_magic" -> MagicEarthElement.castDirect(caster, target, vars, proficiency, utility);
         default -> false;
      };
   }

   private static boolean shouldUseElementUtility(MysticMagicianEntity caster, LivingEntity target, CompoundTag payload, double proficiency, String magicId) {
      if (proficiency < ("fire_magic".equals(magicId) ? 50.0 : 25.0)) {
         return false;
      }
      double distance = caster.distanceTo(target);
      boolean presetUtility = payload != null && payload.contains("element_mode") && Mth.clamp(payload.getInt("element_mode"), 0, 1) == 1;
      if (presetUtility && distance <= 12.0) {
         return true;
      }
      return distance <= 7.0 && caster.getRandom().nextFloat() < 0.35F;
   }

   private static double elementalProjectileSpeed(String magicId, double proficiency) {
      double p = Mth.clamp(proficiency, 0.0, 100.0);
      if ("fire_magic".equals(magicId)) {
         return p >= 75.0 ? 4.2 : p >= 50.0 ? 3.2 : p >= 25.0 ? 2.5 : 1.5;
      } else if ("water_magic".equals(magicId)) {
         return p >= 75.0 ? 4.4 : p >= 25.0 ? 2.6 : 1.4;
      } else if ("wind_magic".equals(magicId)) {
         return p >= 75.0 ? 4.8 : p >= 25.0 ? 3.0 : 1.2;
      }
      return p >= 50.0 ? 2.5 : p >= 25.0 ? 2.0 : 1.4;
   }

   static boolean castGander(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      int chargeSeconds = getGanderChargeSeconds(proficiency);
      double cost = chargeSeconds * 5.0;
      if (!hasProjectilePath(caster, target, 3.8, 0.0)) {
         repositionForClearShot(caster, target, 8.5, 1.08);
         return false;
      } else if (!consumeMana(vars, cost)) {
         return false;
      } else {
         markCastingPose(caster, 12);
         Vec3 direction = getAimDirection(caster, target, 3.8, 0.0);
         faceCasterToDirection(caster, direction);
         Vec3 spawnPos = EntityUtils.getRightHandCastAnchor(caster).add(direction.scale(0.1));
         GanderProjectileEntity projectile = new GanderProjectileEntity(caster.level(), caster);
         projectile.setNoGravity(true);
         projectile.setChargeSeconds(chargeSeconds);
         projectile.setMagicSource("gander", proficiency);
         projectile.setVisualScale(MagicGander.getVisualScaleForChargeSeconds(chargeSeconds));
         projectile.setItem(new ItemStack(ModItems.GANDER.get()));
         projectile.setPos(spawnPos);
         projectile.shoot(direction.x, direction.y, direction.z, 3.8F, 0.08F);
         caster.level().addFreshEntity(projectile);
         caster.level()
            .playSound(
               null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 0.6F, 1.05F + chargeSeconds * 0.05F
            );
         return true;
      }
   }

   static boolean castGandrMachineGun(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      boolean barrage = isBarrageMode(payload);
      if (!barrage && target != null && target.isAlive() && caster.distanceToSqr(target) > 49.0 && vars.player_mana > 220.0 && caster.getRandom().nextFloat() < 0.35F) {
         barrage = true;
      }

      if (!hasProjectilePath(caster, target, barrage ? 3.5 : 3.8, 0.0)) {
         repositionForClearShot(caster, target, 9.0, 1.08);
         return false;
      } else if (barrage) {
         int waveCount = getGandrBarrageWaveCount(proficiency);
         int shotCountPerWave = Math.max(4, Math.min(8, getBarrageShotCount(proficiency) / 2));
         int totalShots = waveCount * shotCountPerWave;
         double cost = totalShots * 20.0;
         if (!consumeMana(vars, cost)) {
            return false;
         } else {
            markCastingPose(caster, 16);
            int chargeSeconds = getGanderChargeSeconds(proficiency);
            fireNpcBarrage(caster, target, shotCountPerWave, chargeSeconds, proficiency);
            startPendingMachineGun(
               caster, target, "gandr_machine_gun", waveCount - 1, 4, 1, chargeSeconds, shotCountPerWave
            );
            caster.level()
               .playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 0.65F, 1.2F);
            return true;
         }
      } else {
         int waveCount = getGandrRapidWaveCount(proficiency);
         int totalShots = waveCount * 3;
         double cost = totalShots * 20.0;
         if (!consumeMana(vars, cost)) {
            return false;
         } else {
            markCastingPose(caster, 12);
            int chargeSeconds = getGanderChargeSeconds(proficiency);
            fireGandrRapidWave(caster, target, chargeSeconds, proficiency);
            startPendingMachineGun(
               caster, target, "gandr_machine_gun", waveCount - 1, 3, 0, chargeSeconds, 3
            );
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.55F, 1.12F);
            return true;
         }
      }
   }

   private static void fireNpcBarrage(MysticMagicianEntity caster, LivingEntity target, int shotCount, int chargeSeconds, double proficiency) {
      Level level = caster.level();
      Vec3 forward = getAimDirection(caster, target, 3.5, 0.0);
      faceCasterToDirection(caster, forward);
      Vec3 right = getRightVector(forward);
      Vec3 up = right.cross(forward).normalize();
      Vec3 center = caster.getEyePosition().add(forward.scale(-2.0)).add(up.scale(0.75));
      Vec3 targetPoint = getPredictedAimPoint(caster, target, 3.5, 0.0);
      RandomSource random = caster.getRandom();
      int columns = Math.max(5, Math.min(8 + random.nextInt(3), shotCount));
      int rows = (int)Math.ceil((double)shotCount / columns);
      int[] order = shuffledOrder(shotCount, random);

      for (int i = 0; i < shotCount; i++) {
         int slot = order[i];
         int row = slot / columns;
         int col = slot % columns;
         double sx = (col - (columns - 1) * 0.5) * 0.48 + (random.nextDouble() - 0.5) * 0.24;
         double sy = ((rows - 1) * 0.5 - row) * 0.42 + (random.nextDouble() - 0.5) * 0.28;
         double sz = ((rows - 1) * 0.5 - row) * 0.36 + (random.nextDouble() - 0.5) * 0.52;
         Vec3 spawn = center.add(right.scale(sx)).add(up.scale(sy)).add(forward.scale(sz));
         Vec3 aimJitter = right.scale((random.nextDouble() - 0.5) * 0.7).add(up.scale((random.nextDouble() - 0.5) * 0.45));
         Vec3 dir = targetPoint.add(aimJitter).subtract(spawn).normalize();
         GanderProjectileEntity projectile = new GanderProjectileEntity(level, caster);
         projectile.setNoGravity(true);
         projectile.setChargeSeconds(chargeSeconds);
         projectile.setMagicSource("gandr_machine_gun", proficiency);
         projectile.setVisualScale(MagicGander.getVisualScaleForChargeSeconds(chargeSeconds));
         projectile.setItem(new ItemStack(ModItems.GANDER.get()));
         projectile.setPos(spawn);
         projectile.shoot(dir.x, dir.y, dir.z, 3.5F + random.nextFloat() * 0.7F, 0.12F);
         level.addFreshEntity(projectile);
      }
   }

   private static void fireGandrRapidWave(MysticMagicianEntity caster, LivingEntity target, int chargeSeconds, double proficiency) {
      Level level = caster.level();
      Vec3 forward = getAimDirection(caster, target, 3.8, 0.0);
      faceCasterToDirection(caster, forward);
      Vec3 right = getRightVector(forward);
      Vec3 up = right.cross(forward).normalize();
      Vec3 hand = EntityUtils.getRightHandCastAnchor(caster);

      for (int i = 0; i < 3; i++) {
         GanderProjectileEntity projectile = new GanderProjectileEntity(level, caster);
         projectile.setNoGravity(true);
         projectile.setChargeSeconds(chargeSeconds);
         projectile.setMagicSource("gandr_machine_gun", proficiency);
         projectile.setVisualScale(MagicGander.getVisualScaleForChargeSeconds(chargeSeconds));
         projectile.setItem(new ItemStack(ModItems.GANDER.get()));
         Vec3 spawn = hand.add(forward.scale(0.08)).add(right.scale(RAPID_SIDE[i])).add(up.scale(RAPID_UP[i]));
         projectile.setPos(spawn);
         Vec3 spreadDir = applyDirectionalSpread(forward, right, up, RAPID_YAW[i], RAPID_PITCH[i]);
         projectile.shoot(spreadDir.x, spreadDir.y, spreadDir.z, 3.8F, 0.1F);
         level.addFreshEntity(projectile);
      }
   }

   private static void fireJewelMachineGunWave(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, boolean defensiveMode
   ) {
      if (defensiveMode) {
         if (target != null && target.isAlive()) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 0, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, true, true));
         }

         caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, true, true));
         caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, true, true));
         spawnNpcGemProjectile(caster, target, GemType.TOPAZ, 2.7, 0.06F, false, true, false);
         spawnNpcGemProjectile(caster, target, GemType.EMERALD, 2.6, 0.06F, false, true, false);
      } else {
         Vec3 forward = getAimDirection(caster, target, 2.8, 0.05);
         faceCasterToDirection(caster, forward);
         Vec3 right = getRightVector(forward);
         Vec3 up = right.cross(forward).normalize();
         Vec3 hand = EntityUtils.getRightHandCastAnchor(caster);
         RandomSource random = caster.getRandom();

         for (int i = 0; i < 3; i++) {
            GemType type = GemType.values()[random.nextInt(GemType.values().length)];
            ItemStack gem = new ItemStack(ModItems.getNormalizedFullCarvedGem(type));
            int gemTypeId = toGemTypeId(type);
            CompoundTag custom = ((CustomData)gem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            custom.putBoolean("IsMachineGunMode", true);
            gem.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
            RubyProjectileEntity projectile = new RubyProjectileEntity(caster.level(), caster);
            projectile.setGemType(gemTypeId);
            projectile.setItem(gem);
            Vec3 spawn = hand.add(forward.scale(0.08)).add(right.scale(RAPID_SIDE[i])).add(up.scale(RAPID_UP[i]));
            projectile.setPos(spawn);
            Vec3 spreadDir = applyDirectionalSpread(forward, right, up, RAPID_YAW[i], RAPID_PITCH[i]);
            projectile.shoot(spreadDir.x, spreadDir.y, spreadDir.z, 2.8F, 0.08F);
            caster.level().addFreshEntity(projectile);
         }
      }
   }

   private static CompoundTag buildCombatJewelMachineGunPayload(
      MysticMagicianEntity caster, LivingEntity target, NpcMagicCastBridge.ThreatProfile threat
   ) {
      CompoundTag payload = new CompoundTag();
      boolean defensive = false;
      if (caster != null && target != null && target.isAlive()) {
         double healthRatio = caster.getHealth() / Math.max(1.0, (double)caster.getMaxHealth());
         defensive = caster.distanceToSqr(target) <= 16.0 || healthRatio <= 0.62 || threat != null && threat.requiresDefensiveTactics();
      }

      payload.putInt("jewel_machine_gun_mode", defensive ? 1 : 0);
      return payload;
   }

   private static boolean isJewelDefenseMode(CompoundTag payload, MysticMagicianEntity caster, LivingEntity target) {
      if (payload != null && payload.contains("jewel_machine_gun_mode")) {
         return Mth.clamp(payload.getInt("jewel_machine_gun_mode"), 0, 1) == 1;
      } else if (caster != null && target != null && target.isAlive()) {
         return caster.distanceToSqr(target) <= 16.0 || caster.getHealth() / Math.max(1.0, (double)caster.getMaxHealth()) <= 0.62;
      } else {
         return false;
      }
   }

   private static boolean isElementalMagic(String magicId) {
      return "fire_magic".equals(magicId) || "water_magic".equals(magicId) || "wind_magic".equals(magicId) || "earth_magic".equals(magicId);
   }

   static boolean castGravity(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      int targetMode = payload.contains("gravity_target") ? Mth.clamp(payload.getInt("gravity_target"), 0, 1) : 1;
      int mode = payload.contains("gravity_mode") ? Mth.clamp(payload.getInt("gravity_mode"), -2, 2) : 1;
      LivingEntity actualTarget = (LivingEntity)(targetMode == 0 ? caster : target);
      if (actualTarget == null || !actualTarget.isAlive()) {
         return false;
      } else if (!consumeMana(vars, 20.0)) {
         return false;
      } else {
         if (mode == 0) {
            MagicGravityEffectHandler.clearGravityState(actualTarget);
            MagicGravityEffectHandler.playGravityNormalizeFx(caster, actualTarget);
         } else {
            int duration = getGravityDurationTicks(proficiency);
            MagicGravityEffectHandler.applyGravityState(actualTarget, mode, caster.level().getGameTime() + duration, caster);
            MagicGravityEffectHandler.playGravityCastFx(caster, actualTarget, mode);
         }

         return true;
      }
   }

   static boolean castReinforcement(
      MysticMagicianEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      int mode = payload.contains("reinforcement_mode") ? Mth.clamp(payload.getInt("reinforcement_mode"), 0, 3) : 0;
      int requestedLevel = payload.contains("reinforcement_level") ? Mth.clamp(payload.getInt("reinforcement_level"), 1, 5) : 1;
      int manaAffordableLevel = Mth.clamp((int)Math.floor(vars.player_mana / 20.0), 0, 5);
      int level = Math.min(requestedLevel, manaAffordableLevel);
      if (level <= 0) {
         return false;
      }

      int duration = (600 + (int)(proficiency * 10.0)) * level;
      int amplifier = level - 1;
      MobEffectInstance effect = null;
      MobEffectInstance extraEffect = null;
      switch (mode) {
         case 0:
            effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_DEFENSE, duration, amplifier, false, false, true);
            break;
         case 1:
            effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_STRENGTH, duration, amplifier, false, false, true);
            break;
         case 2:
            effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_AGILITY, duration, amplifier, false, false, true);
            break;
         case 3:
            effect = new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_SIGHT, duration, amplifier, false, false, true);
            extraEffect = new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false);
      }

      if (effect == null) {
         return false;
      }

      MobEffectInstance existing = caster.getEffect(effect.getEffect());
      if (existing != null && existing.getAmplifier() >= effect.getAmplifier() && existing.getDuration() >= effect.getDuration()) {
         return false;
      }

      double cost = 20.0 * level;
      if (!consumeMana(vars, cost)) {
         return false;
      }

      caster.addEffect(effect);
      if (extraEffect != null) {
         caster.addEffect(extraEffect);
         NightVisionEffectSource.mark(caster, NightVisionEffectSource.NPC_REINFORCEMENT_SIGHT);
      }

      TypeMoonWorldModVariables.ReinforcementData data = (TypeMoonWorldModVariables.ReinforcementData)caster.getData(
         TypeMoonWorldModVariables.REINFORCEMENT_DATA
      );
      data.casterUUID = caster.getUUID();
      return true;
   }

   static boolean castManaBurst(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      if (caster == null || vars == null) {
         return false;
      }
      int mode = manaBurstMode(payload);
      int level = manaBurstLevel(payload, proficiency);
      if (mode == 2) {
         return castManaBurstDirect(caster, target, vars, level);
      }
      return castManaBurstBuff(caster, target, vars, mode, level, proficiency);
   }

   private static boolean castManaBurstBuff(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, int mode, int level, double proficiency
   ) {
      int duration = 180 + (int)Math.round(Mth.clamp(proficiency, 0.0, 100.0) * 2.4);
      MobEffectInstance existingStrength = caster.getEffect(MobEffects.DAMAGE_BOOST);
      if (existingStrength != null && existingStrength.getAmplifier() >= Math.max(0, level - 2) && existingStrength.getDuration() >= duration / 2) {
         return false;
      }
      if (!consumeMana(vars, MANA_BURST_UPKEEP[level - 1])) {
         return false;
      }
      markCastingPose(caster, 12);
      if (target != null && target.isAlive()) {
         caster.lookAt(target, 40.0F, 40.0F);
      }
      caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, Math.max(0, level - 2), false, true, true));
      if (mode == 1) {
         caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, Math.max(0, Math.min(3, level - 1)), false, true, true));
         caster.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, Math.max(0, Math.min(3, level - 1)), false, true, true));
         caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, Math.max(80, duration / 2), 0, false, true, true));
      }
      if (caster.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.END_ROD, caster.getX(), caster.getY() + 1.0, caster.getZ(), 28, 0.55, 0.9, 0.55, 0.08);
         serverLevel.sendParticles(ParticleTypes.WITCH, caster.getX(), caster.getY() + 1.0, caster.getZ(), 18, 0.45, 0.75, 0.45, 0.04);
      }
      caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.75F, 1.2F + level * 0.05F);
      return true;
   }

   private static boolean castManaBurstDirect(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, int level
   ) {
      if (target == null || !target.isAlive()) {
         return false;
      }
      double range = MANA_BURST_DIRECT_RANGE[level - 1];
      if (caster.distanceToSqr(target) > range * range || !caster.hasLineOfSight(target)) {
         repositionForClearShot(caster, target, Math.min(14.0, Math.max(8.0, range * 0.35)), 1.1);
         return false;
      }
      if (!consumeMana(vars, MANA_BURST_DIRECT_COST[level - 1])) {
         return false;
      }
      markCastingPose(caster, 14);
      Vec3 eye = caster.getEyePosition();
      Vec3 look = getAimDirection(caster, target, 3.8, 0.0).normalize();
      faceCasterToDirection(caster, look);
      Vec3 end = eye.add(look.scale(range));
      if (caster.level() instanceof ServerLevel serverLevel) {
         Set<Integer> hit = new HashSet<>();
         for (LivingEntity victim : serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(eye, end).inflate(1.35),
            e -> e.isAlive() && e != caster && !EntityUtils.isImmunePlayerTarget(e))) {
            Vec3 center = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
            Vec3 rel = center.subtract(eye);
            double along = rel.dot(look);
            if (along < 0.0 || along > range || !hit.add(victim.getId())) {
               continue;
            }
            double side = rel.subtract(look.scale(along)).length();
            if (side <= 1.15) {
               victim.invulnerableTime = 0;
               victim.hurt(caster.damageSources().magic(), MANA_BURST_DIRECT_DAMAGE[level - 1]);
               victim.invulnerableTime = 0;
            }
         }
         for (double d = 1.0; d <= range; d += 1.5) {
            Vec3 p = eye.add(look.scale(d));
            serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 4, 0.15, 0.15, 0.15, 0.02);
         }
      }
      caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.9F, 1.55F);
      return true;
   }

   private static int manaBurstMode(CompoundTag payload) {
      return payload != null && payload.contains("mana_burst_mode") ? Mth.clamp(payload.getInt("mana_burst_mode"), 0, 2) : 1;
   }

   private static int manaBurstLevel(CompoundTag payload, double proficiency) {
      int requested = payload != null && payload.contains("mana_burst_level") ? Mth.clamp(payload.getInt("mana_burst_level"), 1, 5) : 1;
      int unlocked = Mth.clamp(1 + (int)Math.floor(Mth.clamp(proficiency, 0.0, 100.0) / 20.0), 1, 5);
      return Math.min(requested, unlocked);
   }

   private static boolean isManaBurstDirect(CompoundTag payload, String magicId) {
      return "mana_burst".equals(magicId) && manaBurstMode(payload) == 2;
   }

   static boolean castRubyFlameSword(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null) {
         return false;
      } else if (!hasProjectilePath(caster, target, 2.9, 0.04)) {
         repositionForClearShot(caster, target, 8.5, 1.08);
         return false;
      } else if (!consumeMana(vars, 72.0)) {
         return false;
      } else {
         markCastingPose(caster, 14);
         fireRubyFlameVolley(caster, target, proficiency);
         caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.65F, 0.95F);
         return true;
      }
   }

   static boolean castSapphireWinterFrost(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null) {
         return false;
      } else if (!caster.hasLineOfSight(target)) {
         repositionForClearShot(caster, target, 7.5, 1.05);
         return false;
      } else if (!consumeMana(vars, 88.0)) {
         return false;
      } else {
         markCastingPose(caster, 14);
         int radius = Mth.clamp(8 + (int)Math.floor(Mth.clamp(proficiency, 0.0, 100.0) / 25.0), 8, 12);
         int duration = 100 + (int)Math.round(Mth.clamp(proficiency, 0.0, 100.0) * 2.2);
         applyNpcWinterFrostField(caster, target, radius, duration, 4, 2, 0);
         return true;
      }
   }

   static boolean castEmeraldWinterRiver(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || vars == null) {
         return false;
      } else if (!consumeMana(vars, 92.0)) {
         return false;
      } else {
         markCastingPose(caster, 16);
         int radius = proficiency >= 75.0 ? 2 : 1;
         int height = 2 * radius;
         BlockPos center = caster.blockPosition();
         boolean airborneCast = !caster.onGround() && !caster.isInWater() && !caster.isInLava() && !caster.isFallFlying();
         if (airborneCast) {
            radius = Math.max(2, radius);
            height = 2 * radius;
            Vec3 pos = caster.position();
            center = new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y) - radius + 1, Mth.floor(pos.z));
         } else if (caster.isInWater() || caster.isInLava()) {
            center = center.below(radius);
         }

         pushEntitiesOutOfEmeraldBarrier(caster.level(), caster, center, radius, height);
         placeEmeraldWinterRiverCage(caster.level(), center, radius, height);
         if (airborneCast) {
            Vec3 lockPos = center.offset(0, radius, 0).getCenter();
            double targetY = lockPos.y - caster.getBbHeight() * 0.5;
            caster.teleportTo(lockPos.x, targetY, lockPos.z);
            caster.setDeltaMovement(Vec3.ZERO);
            caster.fallDistance = 0.0F;
         }

         if (caster.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.getX(), center.getY() + 1.0, center.getZ(), 50, radius + 0.8, 1.2, radius + 0.8, 0.08);
         }

         return true;
      }
   }

   static boolean castTopazReinforcement(MysticMagicianEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      if (caster == null || vars == null) {
         return false;
      } else {
         int duration = 240 + (int)Math.round(Mth.clamp(proficiency, 0.0, 100.0) * 2.4);
         MobEffectInstance existingStrength = caster.getEffect(MobEffects.DAMAGE_BOOST);
         if (existingStrength != null && existingStrength.getAmplifier() >= 1 && existingStrength.getDuration() >= duration / 2) {
            return false;
         } else if (!consumeMana(vars, 76.0)) {
            return false;
         } else {
            markCastingPose(caster, 12);
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1, false, true, true));
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, true, true));
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.max(180, duration - 40), 0, false, true, true));
            caster.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 1, false, true, true));
            if (caster.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, caster.getX(), caster.getY() + 1.0, caster.getZ(), 28, 0.5, 0.9, 0.5, 0.08);
               serverLevel.sendParticles(ParticleTypes.WAX_ON, caster.getX(), caster.getY() + 1.0, caster.getZ(), 18, 0.5, 0.9, 0.5, 0.06);
            }

            return true;
         }
      }
   }

   static boolean castCyanWind(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (caster == null || target == null || !target.isAlive() || vars == null) {
         return false;
      } else if (!hasProjectilePath(caster, target, 2.8, 0.04)) {
         repositionForClearShot(caster, target, 8.5, 1.08);
         return false;
      } else if (!consumeMana(vars, 80.0)) {
         return false;
      } else {
         markCastingPose(caster, 12);
         spawnNpcCyanWindProjectile(caster, target, proficiency);
         caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.HOSTILE, 0.7F, 0.9F);
         return true;
      }
   }

   static boolean castJewelRandomShoot(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (!hasProjectilePath(caster, target, 2.8, 0.04)) {
         repositionForClearShot(caster, target, 8.5, 1.08);
         return false;
      } else if (!consumeMana(vars, 30.0)) {
         return false;
      } else {
         markCastingPose(caster, 10);
         RandomSource random = caster.getRandom();
         GemType[] types = GemType.values();
         GemType type = types[random.nextInt(types.length)];
         ItemStack gem = new ItemStack(ModItems.getNormalizedFullCarvedGem(type));
         int gemTypeId = toGemTypeId(type);
         float multiplier = type == GemType.RUBY ? 1.0F : 0.5F;
         CompoundTag custom = ((CustomData)gem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
         custom.putBoolean("IsRandomMode", true);
         custom.putFloat("ExplosionPowerMultiplier", multiplier);
         gem.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
         Vec3 direction = getAimDirection(caster, target, 2.8, 0.05);
         faceCasterToDirection(caster, direction);
         Vec3 spawnPos = EntityUtils.getRightHandCastAnchor(caster).add(direction.scale(0.1));
         RubyProjectileEntity projectile = new RubyProjectileEntity(caster.level(), caster);
         projectile.setGemType(gemTypeId);
         projectile.setItem(gem);
         projectile.setPos(spawnPos);
         projectile.shoot(direction.x, direction.y, direction.z, 2.8F, 0.12F);
         caster.level().addFreshEntity(projectile);
         return true;
      }
   }

   static boolean castJewelMachineGun(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      boolean defensiveMode = isJewelDefenseMode(payload, caster, target);
      int waves = getJewelMachineGunWaveCount(proficiency, defensiveMode);
      double cost = defensiveMode ? 45.0 + waves * 18.0 : 60.0 + waves * 22.0;
      if (!hasProjectilePath(caster, target, defensiveMode ? 2.7 : 2.8, 0.04)) {
         repositionForClearShot(caster, target, defensiveMode ? 7.0 : 8.5, 1.08);
         return false;
      } else if (!consumeMana(vars, cost)) {
         return false;
      } else {
         markCastingPose(caster, 12);
         fireJewelMachineGunWave(caster, target, vars, defensiveMode);
         startPendingMachineGun(caster, target, "jewel_machine_gun", waves - 1, defensiveMode ? 5 : 4, defensiveMode ? 1 : 0, 0, 3);
         return true;
      }
   }

   private static int toGemTypeId(GemType type) {
      return switch (type) {
         case RUBY -> 0;
         case SAPPHIRE -> 1;
         case EMERALD -> 2;
         case TOPAZ -> 3;
         case CYAN -> 4;
         case WHITE_GEMSTONE -> 5;
         case BLACK_SHARD -> 6;
      };
   }

   private static Vec3 getAimDirection(LivingEntity caster, LivingEntity target) {
      return getAimDirection(caster, target, 3.0, 0.0);
   }

   private static Vec3 getAimDirection(LivingEntity caster, LivingEntity target, double projectileSpeed, double gravityPerTick) {
      Vec3 predicted = getPredictedAimPoint(caster, target, projectileSpeed, gravityPerTick);
      if (predicted != null) {
         Vec3 toward = predicted.subtract(caster.getEyePosition());
         if (toward.lengthSqr() > 1.0E-6) {
            return toward.normalize();
         }
      }

      Vec3 look = caster.getLookAngle();
      return look.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
   }

   private static Vec3 getPredictedAimPoint(LivingEntity caster, LivingEntity target, double projectileSpeed, double gravityPerTick) {
      if (caster != null && target != null && target.isAlive()) {
         Vec3 casterEye = caster.getEyePosition();
         Vec3 targetEye = target.getEyePosition();
         Vec3 relative = targetEye.subtract(casterEye);
         double speed = Math.max(0.1, projectileSpeed);
         double travelTime = Mth.clamp(relative.length() / speed, 0.0, 1.75);
         Vec3 leadPos = targetEye.add(target.getDeltaMovement().scale(travelTime * 0.95));
         if (gravityPerTick > 0.0) {
            leadPos = leadPos.add(0.0, 0.5 * gravityPerTick * travelTime * travelTime, 0.0);
         }

         return leadPos;
      } else {
         return null;
      }
   }

   private static boolean hasGenericClearRangedPath(MysticMagicianEntity caster, LivingEntity target) {
      return hasProjectilePath(caster, target, 3.0, 0.04);
   }

   private static boolean hasPendingMachineGunPath(MysticMagicianEntity caster, LivingEntity target, String magicId, int mode) {
      if ("gandr_machine_gun".equals(magicId)) {
         return hasProjectilePath(caster, target, mode == 1 ? 3.5 : 3.8, 0.0);
      } else {
         return !"jewel_machine_gun".equals(magicId) || hasProjectilePath(caster, target, mode == 1 ? 2.7 : 2.8, 0.04);
      }
   }

   private static boolean hasProjectilePath(LivingEntity caster, LivingEntity target, double projectileSpeed, double gravityPerTick) {
      if (!(caster instanceof MysticMagicianEntity npc) || !isValidCombatTarget(npc, target)) {
         return false;
      } else {
         Vec3 start = EntityUtils.getRightHandCastAnchor(caster);
         Vec3 targetPoint = getPredictedAimPoint(caster, target, projectileSpeed, gravityPerTick);
         if (targetPoint == null) {
            targetPoint = target.getEyePosition();
         }

         HitResult hit = caster.level().clip(new ClipContext(start, targetPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
         return hit.getType() != HitResult.Type.BLOCK || !(hit.getLocation().distanceToSqr(start) + 0.0625 < targetPoint.distanceToSqr(start));
      }
   }

   private static void repositionForClearShot(MysticMagicianEntity npc, LivingEntity target, double preferredDistance, double speed) {
      if (npc != null && target != null && target.isAlive()) {
         Vec3 toTarget = target.position().subtract(npc.position());
         Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
         if (horizontal.lengthSqr() < 1.0E-6) {
            npc.getNavigation().moveTo(target, Math.max(0.7, speed));
         } else {
            Vec3 forward = horizontal.normalize();
            Vec3 right = getRightVector(forward);
            double side = npc.getRandom().nextBoolean() ? 2.6 : -2.6;
            Vec3 anchor = target.position().subtract(forward.scale(Math.max(4.5, preferredDistance))).add(right.scale(side));
            npc.getNavigation().moveTo(anchor.x, target.getY(), anchor.z, Math.max(0.7, speed));
         }

         npc.lookAt(target, 45.0F, 45.0F);
      }
   }

   private static Vec3 applyDirectionalSpread(Vec3 forward, Vec3 right, Vec3 up, float yawDegrees, float pitchDegrees) {
      double yawOffset = Math.tan(Math.toRadians(yawDegrees));
      double pitchOffset = Math.tan(Math.toRadians(pitchDegrees));
      Vec3 dir = forward.add(right.scale(yawOffset)).add(up.scale(-pitchOffset));
      return dir.lengthSqr() < 1.0E-6 ? forward : dir.normalize();
   }

   private static void faceCasterToDirection(MysticMagicianEntity caster, Vec3 direction) {
      if (caster != null && direction != null && !(direction.lengthSqr() < 1.0E-6)) {
         double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
         if (!(horizontal < 1.0E-6)) {
            float yaw = (float)(Mth.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F;
            float pitch = (float)(-(Mth.atan2(direction.y, horizontal) * (180.0 / Math.PI)));
            caster.setYRot(yaw);
            caster.setYHeadRot(yaw);
            caster.setYBodyRot(yaw);
            caster.setXRot(Mth.clamp(pitch, -89.9F, 89.9F));
         }
      }
   }

   private static void markCastingPose(MysticMagicianEntity caster, int ticks) {
      if (caster != null) {
         caster.triggerCastingPose(ticks);
      }
   }

   private static void markMeleePose(MysticMagicianEntity caster, int pose, int ticks) {
      if (caster != null) {
         caster.swing(InteractionHand.MAIN_HAND, true);
         caster.triggerMeleeSkillPose(pose, ticks);
      }
   }

   private static void circleMeleeTarget(MysticMagicianEntity npc, LivingEntity target, double distance) {
      Vec3 forward = target.position().subtract(npc.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 1.0E-5) return;
      Vec3 side = getRightVector(forward.normalize()).scale(npc.getRandom().nextBoolean() ? 1.35 : -1.35);
      Vec3 anchor = npc.position().add(side);
      if (distance < 1.65) anchor = anchor.subtract(forward.normalize().scale(0.65));
      npc.getNavigation().moveTo(anchor.x, npc.getY(), anchor.z, 1.12);
   }

   private static Vec3 getRightVector(Vec3 forward) {
      Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
      return right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
   }

   private static int[] shuffledOrder(int size, RandomSource random) {
      int[] order = new int[size];
      int i = 0;

      while (i < size) {
         order[i] = i++;
      }

      for (int ix = size - 1; ix > 0; ix--) {
         int j = random.nextInt(ix + 1);
         int tmp = order[ix];
         order[ix] = order[j];
         order[j] = tmp;
      }

      return order;
   }

   private static boolean consumeMana(TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      amount *= AdvancedPassiveService.manaMultiplier(vars);
      if (vars.player_mana < amount) {
         return false;
      } else {
         vars.player_mana = Math.max(0.0, vars.player_mana - amount);
         return true;
      }
   }

   private static int getGanderChargeSeconds(double proficiency) {
      int seconds = 1 + (int)Math.floor(Mth.clamp(proficiency, 0.0, 100.0) / 25.0);
      return Mth.clamp(seconds, 1, proficiency >= 50.0 ? 5 : 4);
   }

   private static int getBarrageShotCount(double proficiency) {
      double t = Mth.clamp(proficiency, 0.0, 100.0) / 100.0;
      return (int)Math.round(12.0 + 3.0 * t);
   }

   private static int getGandrRapidWaveCount(double proficiency) {
      return Mth.clamp(2 + (int)Math.floor(Mth.clamp(proficiency, 0.0, 100.0) / 35.0), 2, 5);
   }

   private static int getGandrBarrageWaveCount(double proficiency) {
      return Mth.clamp(2 + (int)Math.floor(Mth.clamp(proficiency, 0.0, 100.0) / 50.0), 2, 4);
   }

   private static int getJewelMachineGunWaveCount(double proficiency, boolean defensiveMode) {
      int base = defensiveMode ? 3 : 4;
      int extra = (int)Math.floor(Mth.clamp(proficiency, 0.0, 100.0) / 40.0);
      return Mth.clamp(base + extra, 3, defensiveMode ? 5 : 6);
   }

   private static int getGravityDurationTicks(double proficiency) {
      double t = Mth.clamp(proficiency, 0.0, 100.0) / 100.0;
      return 600 + (int)Math.round(3000.0 * t);
   }

   static boolean isBarrageMode(CompoundTag payload) {
      return payload != null && payload.contains("gandr_machine_gun_mode") && Mth.clamp(payload.getInt("gandr_machine_gun_mode"), 0, 1) == 1;
   }

   private static long getMagicCooldownUntil(MysticMagicianEntity npc, String magicId) {
      CompoundTag all = npc.getPersistentData().getCompound(TAG_MAGIC_CD);
      return all.contains(magicId) ? all.getLong(magicId) : 0L;
   }

   private static void setMagicCooldown(MysticMagicianEntity npc, String magicId, long until) {
      CompoundTag all = npc.getPersistentData().getCompound(TAG_MAGIC_CD);
      all.putLong(magicId, until);
      npc.getPersistentData().put(TAG_MAGIC_CD, all);
   }

   private static void recordMagicCast(MysticMagicianEntity npc, String magicId) {
      if (npc != null && magicId != null && !magicId.isEmpty()) {
         CompoundTag data = npc.getPersistentData();
         String last = data.getString(TAG_LAST_CAST_MAGIC);
         if (magicId.equals(last)) {
            int repeat = data.getInt(TAG_CAST_REPEAT_COUNT);
            data.putInt(TAG_CAST_REPEAT_COUNT, Math.min(8, repeat + 1));
         } else {
            data.putString(TAG_LAST_CAST_MAGIC, magicId);
            data.putInt(TAG_CAST_REPEAT_COUNT, 1);
         }
      }
   }

   private static double getEffectiveProficiency(
      MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, String magicId, boolean crestCast
   ) {
      if (crestCast) {
         return 100.0;
      } else {
         double base = switch (magicId) {
            case "gander", "gandr_machine_gun" -> vars.proficiency_gander;
            case "gravity_magic" -> vars.proficiency_gravity_magic;
            case "reinforcement" -> vars.proficiency_reinforcement;
            case "healing_magic" -> vars.proficiency_healing_magic;
            case "magic_analysis" -> MagicProficiencyService.get(vars, magicId);
            case "magic_bullet" -> vars.proficiency_magic_bullet;
            case "airflow_blade", "detection", "mana_burst" -> MagicProficiencyService.get(vars, magicId);
            case "suggestion_magic" -> vars.proficiency_suggestion_magic;
            case "binding_magic" -> vars.proficiency_binding_magic;
            case "fire_magic" -> vars.proficiency_fire_magic;
            case "water_magic" -> vars.proficiency_water_magic;
            case "wind_magic" -> vars.proficiency_wind_magic;
            case "earth_magic" -> vars.proficiency_earth_magic;
            case "aerial_stasis", "aerial_ascent" -> MagicProficiencyService.get(vars, magicId);
            case "jewel_random_shoot" -> vars.proficiency_jewel_magic_shoot;
            case "jewel_machine_gun",
               "ruby_flame_sword",
               "sapphire_winter_frost",
               "emerald_winter_river",
               "topaz_reinforcement",
               "cyan_wind" -> vars.proficiency_jewel_magic_release;
            default -> MagicProficiencyService.get(vars, magicId);
         };
         int fixedLevel = getFixedLevel(npc);
         int combatBonus = getCombatLevelBonus(npc);
         double levelBonus = (fixedLevel - 1) * 7.0 + combatBonus * 5.0;
         if ("reinforcement".equals(magicId) && fixedLevel >= 3) {
            levelBonus += 6.0;
         }

         return Mth.clamp(base + levelBonus, 0.0, 100.0);
      }
   }

   private static ItemStack readItem(CompoundTag payload, String key, Provider lookup) {
      return payload != null && key != null && !key.isEmpty() && payload.contains(key, 10)
         ? ItemStack.parse(lookup, payload.getCompound(key)).orElse(ItemStack.EMPTY)
         : ItemStack.EMPTY;
   }

   private static double round1(double value) {
      return Math.round(value * 10.0) / 10.0;
   }

   private static double round3(double value) {
      return Math.round(value * 1000.0) / 1000.0;
   }

   private static double clampZombieComparableStat(double value, double zombieBase) {
      double min = zombieBase * 0.9;
      double max = zombieBase * 2.0;
      return round3(Mth.clamp(value, min, max));
   }

   private static double clampCombatMoveSpeed(double combatSpeed, double baseSpeed) {
      double min = baseSpeed * 1.18;
      double target = baseSpeed * 1.45;
      double max = 0.46;
      double desired = Math.max(combatSpeed, target);
      return round3(Mth.clamp(desired, min, max));
   }

   private static double randomZombieComparableStat(RandomSource random, double zombieBase) {
      if (random == null) {
         return round3(zombieBase);
      } else {
         double baseRatio = 0.9 + random.nextDouble() * 0.45000000000000007;
         if (random.nextInt(100) < 10) {
            baseRatio += 0.2 + random.nextDouble() * 0.25;
         }

         if (random.nextInt(100) < 3) {
            baseRatio += 0.15 + random.nextDouble() * 0.25;
         }

         double clampedRatio = Mth.clamp(baseRatio, 0.9, 2.0);
         return round3(zombieBase * clampedRatio);
      }
   }

   private record Choice(TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry, int slot, double weight) {
   }

   private record EnvironmentHazardProfile(boolean anyHazard, boolean severe, int hazardScore) {
      private static final NpcMagicCastBridge.EnvironmentHazardProfile NONE = new NpcMagicCastBridge.EnvironmentHazardProfile(false, false, 0);
   }

   private record BehaviorProfile(
      double retreatHealthRatio,
      double rangedKeepMinDistance,
      double rangedKeepMaxDistance,
      double meleeEngageDistance,
      double meleeSkillTriggerDistance
   ) {
   }

   private record MagicCapabilityProfile(boolean hasBuff, boolean hasControl, boolean hasRanged, boolean hasMeleeBurst, boolean hasAnyCastable) {
      private static final NpcMagicCastBridge.MagicCapabilityProfile NONE = new NpcMagicCastBridge.MagicCapabilityProfile(false, false, false, false, false);

      boolean hasAnyOffense() {
         return this.hasRanged || this.hasMeleeBurst || this.hasControl;
      }
   }

   private record SlotSeed(String sourceType, String magicId, CompoundTag payload, String crestEntryId) {
   }

   private record ThreatProfile(int enemyCount, boolean strongTarget, boolean lowWhilePressured) {
      private static final NpcMagicCastBridge.ThreatProfile NONE = new NpcMagicCastBridge.ThreatProfile(0, false, false);

      boolean outnumberedOrPressured() {
         return this.enemyCount >= 3 || this.lowWhilePressured;
      }

      boolean requiresDefensiveTactics() {
         return this.strongTarget || this.outnumberedOrPressured();
      }
   }

   private record WinterFrostRestoreData(BlockPos pos, BlockState originalState, BlockState placedState) {
   }
}
