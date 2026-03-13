package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TopazProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.MagicBrokenPhantasm;
import net.xxxjk.TYPE_MOON_WORLD.magic.nordic.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravityEffectHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicProjection;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineService;

public final class NpcMagicCastBridge {
   private static final String TAG_MAGIC_INIT = "TypeMoonNpcMagicInitV1";
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
   private static final String TAG_CAP_HAS_RANGED = "TypeMoonNpcCapHasRanged";
   private static final String TAG_CAP_HAS_MELEE_BURST = "TypeMoonNpcCapHasMeleeBurst";
   private static final String TAG_CAP_ONLY_MELEE = "TypeMoonNpcCapOnlyMelee";
   private static final String TAG_JEWEL_ITEM_NEXT_TICK = "TypeMoonNpcJewelItemNextTick";
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
   private static final float[] RAPID_YAW = new float[]{0.0F, -1.2F, 1.2F};
   private static final float[] RAPID_PITCH = new float[]{0.0F, -0.8F, 0.8F};
   private static final double[] RAPID_SIDE = new double[]{0.0, -0.12, 0.12};
   private static final double[] RAPID_UP = new double[]{0.0, 0.06, -0.06};
   private static final double RETREAT_HEALTH_RATIO = 0.28;
   private static final double RETREAT_DISTANCE = 12.0;
   private static final double RANGED_KEEP_MIN_DISTANCE = 7.0;
   private static final double RANGED_KEEP_MAX_DISTANCE = 14.0;
   private static final double MELEE_ENGAGE_DISTANCE = 4.2;
   private static final double RANGED_ONLY_RETREAT_DISTANCE = 5.0;
   private static final double ADVANCED_PREFERRED_MANA_RATIO = 0.42;
   private static final float NPC_PROJECTION_DUAL_WIELD_CHANCE = 0.12F;
   private static final GemType[] NPC_BASIC_JEWEL_TYPES = new GemType[]{
      GemType.RUBY, GemType.SAPPHIRE, GemType.TOPAZ, GemType.CYAN, GemType.WHITE_GEMSTONE, GemType.BLACK_SHARD
   };
   private static final Set<String> VALID_MAGIC_IDS = Set.of(
      "gander", "gandr_machine_gun", "gravity_magic", "reinforcement", "projection", "broken_phantasm", "jewel_random_shoot", "jewel_machine_gun"
   );

   private NpcMagicCastBridge() {
   }

   public static void onSpawnInitialized(MysticMagicianEntity npc) {
      if (npc != null && !npc.level().isClientSide()) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)npc.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         initializeIfNeeded(npc, vars);
      }
   }

   public static boolean shouldUseMeleeGoal(MysticMagicianEntity npc) {
      if (npc == null) {
         return true;
      } else {
         CompoundTag data = npc.getPersistentData();
         boolean hasRanged = data.getBoolean("TypeMoonNpcCapHasRanged");
         boolean hasMeleeBurst = data.getBoolean("TypeMoonNpcCapHasMeleeBurst");
         boolean onlyMelee = data.getBoolean("TypeMoonNpcCapOnlyMelee");
         LivingEntity target = npc.getTarget();
         if (target != null && target.isAlive()) {
            double distanceSqr = npc.distanceToSqr(target);
            if (onlyMelee) {
               return distanceSqr <= 100.0;
            } else if (hasRanged && hasMeleeBurst) {
               return distanceSqr <= 17.64;
            } else {
               return hasRanged ? distanceSqr <= 7.839999999999999 : distanceSqr <= 20.25;
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
         long gameTime = npc.level().getGameTime();
         tickProjectedHandItemExpiry(npc, gameTime);
         tickManaRecovery(npc, vars);
         tickLocalCooldown(vars);
         NpcMagicCastBridge.MagicCapabilityProfile capabilities = analyzeMagicCapabilities(vars);
         syncCapabilityFlags(npc, capabilities);
         NpcMagicCastBridge.EnvironmentHazardProfile environment = analyzeEnvironmentalHazard(npc);
         if (environment.anyHazard()) {
            boolean envConsumed = handleEnvironmentalHazard(npc, vars, gameTime, environment, capabilities);
            if (envConsumed) {
               return;
            }
         }

         LivingEntity target = npc.getTarget();
         boolean validTarget = isValidCombatTarget(npc, target);
         long lastCombatTick = npc.getPersistentData().getLong("TypeMoonNpcLastCombatTick");
         boolean combatMoveState = validTarget || gameTime - lastCombatTick <= 40L;
         applyCombatMovementState(npc, combatMoveState);
         if (validTarget) {
            npc.getPersistentData().putLong("TypeMoonNpcLastCombatTick", gameTime);
            vars.is_magic_circuit_open = true;
            vars.magic_circuit_open_timer = 0.0;
            NpcMagicCastBridge.ThreatProfile threat = analyzeThreat(npc, target);
            maintainPreferredCombatDistance(npc, target, capabilities, threat);
            if (shouldRetreat(npc, target)) {
               handleRetreat(npc, target, vars, gameTime);
            } else {
               double distance = Math.sqrt(npc.distanceToSqr(target));
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
                     long lastDecision = npc.getPersistentData().getLong("TypeMoonNpcLastDecisionTick");
                     if (gameTime - lastDecision >= 5L) {
                        npc.getPersistentData().putLong("TypeMoonNpcLastDecisionTick", gameTime);
                        if (gameTime >= npc.getPersistentData().getLong("TypeMoonNpcNextGlobalCastTick")) {
                           if (!(vars.player_mana <= 1.0)) {
                              if (!tryUseJewelItemSkill(npc, target, vars, gameTime, threat)) {
                                 TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slot = chooseNextMagic(npc, vars, target, gameTime, capabilities);
                                 if (slot == null || slot.magicId == null || slot.magicId.isEmpty()) {
                                    handleNoMagicFallback(npc, target, vars, gameTime, threat, capabilities);
                                 } else if (isResourceMagicAvailable(npc, slot.magicId, gameTime)) {
                                    if (gameTime >= getMagicCooldownUntil(npc, slot.magicId)) {
                                       boolean crestCast = "crest".equals(slot.sourceType);
                                       double effectiveProficiency = getEffectiveProficiency(vars, slot.magicId, crestCast);
                                       boolean castSuccess = castMagic(npc, target, vars, slot, effectiveProficiency, gameTime);
                                       if (castSuccess) {
                                          int gcd = getGlobalCooldownAfterCast(slot.magicId, slot.presetPayload);
                                          vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
                                          npc.getPersistentData().putLong("TypeMoonNpcNextGlobalCastTick", gameTime + gcd);
                                          setMagicCooldown(npc, slot.magicId, gameTime + getPerMagicCooldown(slot.magicId, slot.presetPayload));
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
            maybeCloseCircuit(npc, vars, gameTime);
         }
      }
   }

   public static void cleanup(MysticMagicianEntity npc) {
      if (npc != null) {
         CompoundTag data = npc.getPersistentData();
         data.remove("TypeMoonNpcLastDecisionTick");
         data.remove("TypeMoonNpcLastCombatTick");
         data.remove("TypeMoonNpcNextGlobalCastTick");
         data.remove("TypeMoonNpcMagicCd");
         data.remove("TypeMoonNpcCastLockUntil");
         data.remove("TypeMoonNpcResourceUse");
         data.remove("TypeMoonNpcLastCastMagic");
         data.remove("TypeMoonNpcCastRepeatCount");
         data.remove("TypeMoonNpcRetreatCounterTick");
         data.remove("TypeMoonNpcRetreatUtilityTick");
         data.remove("TypeMoonNpcThreatUtilityTick");
         data.remove("TypeMoonNpcThreatControlTick");
         data.remove("TypeMoonNpcThreatCounterTick");
         data.remove("TypeMoonNpcEnvUtilityTick");
         data.remove("TypeMoonNpcEnvMoveTick");
         data.remove("TypeMoonNpcCapHasRanged");
         data.remove("TypeMoonNpcCapHasMeleeBurst");
         data.remove("TypeMoonNpcCapOnlyMelee");
         data.remove("TypeMoonNpcJewelItemNextTick");
      }
   }

   private static void initializeIfNeeded(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = npc.getPersistentData();
      ensureCombatAttributesInitialized(npc, data, npc.getRandom());
      if (!data.getBoolean("TypeMoonNpcMagicInitV1")) {
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
         randomizeBaseStats(vars, random);
         randomizeMagicAttributes(vars, random);
         seedSelfKnowledge(vars, random);
         seedCrestKnowledge(npc, vars, random);
         buildWheelEntries(npc, vars, random);
         vars.rebuildSelectedMagicsFromActiveWheel();
         vars.current_magic_index = 0;
         vars.is_magus = true;
         vars.is_magic_circuit_open = false;
         vars.magic_circuit_open_timer = 0.0;
         vars.magic_cooldown = 0.0;
         npc.setCombatStyle(NpcCombatStyle.fromAttributes(vars));
         data.putBoolean("TypeMoonNpcMagicInitV1", true);
         data.putLong("TypeMoonNpcLastDecisionTick", 0L);
         data.putLong("TypeMoonNpcLastCombatTick", npc.level().getGameTime());
         data.putLong("TypeMoonNpcNextGlobalCastTick", 0L);
         data.putLong("TypeMoonNpcCastLockUntil", 0L);
         data.putLong("TypeMoonNpcRetreatCounterTick", 0L);
         data.putLong("TypeMoonNpcRetreatUtilityTick", 0L);
         data.putLong("TypeMoonNpcThreatUtilityTick", 0L);
         data.putLong("TypeMoonNpcThreatControlTick", 0L);
         data.putLong("TypeMoonNpcThreatCounterTick", 0L);
         data.putLong("TypeMoonNpcEnvUtilityTick", 0L);
         data.putLong("TypeMoonNpcEnvMoveTick", 0L);
      }
   }

   private static void ensureCombatAttributesInitialized(MysticMagicianEntity npc, CompoundTag data, RandomSource random) {
      if (npc != null && data != null) {
         boolean initialized = data.getBoolean("TypeMoonNpcAttrInitV1");
         double baseHealth = data.contains("TypeMoonNpcBaseMaxHealth") ? data.getDouble("TypeMoonNpcBaseMaxHealth") : randomZombieComparableStat(random, 20.0);
         double baseSpeed = data.contains("TypeMoonNpcBaseMoveSpeed") ? data.getDouble("TypeMoonNpcBaseMoveSpeed") : randomZombieComparableStat(random, 0.23);
         double baseAttack = data.contains("TypeMoonNpcBaseAttackDamage")
            ? data.getDouble("TypeMoonNpcBaseAttackDamage")
            : randomZombieComparableStat(random, 3.0);
         double combatSpeed = data.contains("TypeMoonNpcCombatMoveSpeed") ? data.getDouble("TypeMoonNpcCombatMoveSpeed") : round3(baseSpeed * 1.45);
         baseHealth = clampZombieComparableStat(baseHealth, 20.0);
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
            data.putDouble("TypeMoonNpcBaseMaxHealth", baseHealth);
            data.putDouble("TypeMoonNpcBaseMoveSpeed", baseSpeed);
            data.putDouble("TypeMoonNpcBaseAttackDamage", baseAttack);
            data.putDouble("TypeMoonNpcCombatMoveSpeed", combatSpeed);
            data.putBoolean("TypeMoonNpcAttrInitV1", true);
            if (healthAttr != null) {
               npc.setHealth((float)healthAttr.getBaseValue());
            }
         } else if (healthAttr != null) {
            data.putDouble("TypeMoonNpcBaseMaxHealth", baseHealth);
            data.putDouble("TypeMoonNpcBaseMoveSpeed", baseSpeed);
            data.putDouble("TypeMoonNpcBaseAttackDamage", baseAttack);
            data.putDouble("TypeMoonNpcCombatMoveSpeed", combatSpeed);
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
         double baseSpeed = data.contains("TypeMoonNpcBaseMoveSpeed") ? data.getDouble("TypeMoonNpcBaseMoveSpeed") : 0.23;
         double combatSpeed = data.contains("TypeMoonNpcCombatMoveSpeed") ? data.getDouble("TypeMoonNpcCombatMoveSpeed") : round3(baseSpeed * 1.45);
         double targetSpeed = inCombat ? combatSpeed : baseSpeed;
         AttributeInstance speedAttr = npc.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speedAttr != null) {
            if (Math.abs(speedAttr.getBaseValue() - targetSpeed) > 1.0E-4) {
               speedAttr.setBaseValue(targetSpeed);
            }
         }
      }
   }

   private static void randomizeBaseStats(TypeMoonWorldModVariables.PlayerVariables vars, RandomSource random) {
      vars.is_magus = true;
      vars.player_max_mana = round1(100.0 + random.nextDouble() * 900.0);
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

   private static void seedSelfKnowledge(TypeMoonWorldModVariables.PlayerVariables vars, RandomSource random) {
      List<String> pool = new ArrayList<>();

      for (String id : NpcMagicFilterService.candidateMagicPool()) {
         if (NpcMagicFilterService.isMagicAllowedForNpc(id)) {
            pool.add(id);
         }
      }

      if (!pool.isEmpty()) {
         Collections.shuffle(pool, new Random(random.nextLong()));
         int max = Math.min(8, pool.size());
         int selfCount = Mth.nextInt(random, 4, Math.max(4, max));

         for (int i = 0; i < selfCount && i < pool.size(); i++) {
            String magicId = pool.get(i);
            vars.learned_magics.add(magicId);
            ensurePrerequisites(vars, magicId);
            seedSelfProficiency(vars, magicId, random);
         }

         if (!vars.learned_magics.contains("gander")) {
            vars.learned_magics.add("gander");
            vars.proficiency_gander = Math.max(vars.proficiency_gander, 35.0);
         }
      }
   }

   private static void seedCrestKnowledge(MysticMagicianEntity npc, TypeMoonWorldModVariables.PlayerVariables vars, RandomSource random) {
      boolean hasCrest = random.nextBoolean();
      if (!hasCrest) {
         vars.magicCrestInventory.setStackInSlot(0, ItemStack.EMPTY);
      } else {
         vars.magicCrestInventory.setStackInSlot(0, new ItemStack((ItemLike)ModItems.MAGIC_CREST.get()));
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

      for (String learned : vars.learned_magics) {
         if (VALID_MAGIC_IDS.contains(learned) && !selfAdded.contains(learned)) {
            CompoundTag payload = NpcMagicFilterService.sanitizePresetForNpc(
               learned, NpcMagicFilterService.buildRandomPresetForMagic(learned, lookup, random), lookup, random
            );
            if (!NpcMagicFilterService.isPresetValidForNpc(learned, payload, lookup)) {
               payload = new CompoundTag();
            }

            seeds.add(new NpcMagicCastBridge.SlotSeed("self", learned, payload, ""));
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

      Collections.shuffle(seeds, new Random(random.nextLong()));
      int slots = Math.min(12, seeds.size());

      for (int i = 0; i < slots; i++) {
         NpcMagicCastBridge.SlotSeed seed = seeds.get(i);
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

   private static void seedSelfProficiency(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, RandomSource random) {
      double p = 15.0 + random.nextDouble() * 75.0;
      switch (magicId) {
         case "gander":
         case "gandr_machine_gun":
            vars.proficiency_gander = Math.max(vars.proficiency_gander, p);
            break;
         case "gravity_magic":
            vars.proficiency_gravity_magic = Math.max(vars.proficiency_gravity_magic, p);
            break;
         case "reinforcement":
            vars.proficiency_reinforcement = Math.max(vars.proficiency_reinforcement, p);
            break;
         case "projection":
         case "broken_phantasm":
            vars.proficiency_projection = Math.max(vars.proficiency_projection, p);
            break;
         case "jewel_random_shoot":
            vars.proficiency_jewel_magic_shoot = Math.max(vars.proficiency_jewel_magic_shoot, p);
            break;
         case "jewel_machine_gun":
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
               break;
            case "broken_phantasm":
               ensureLearned(vars, "projection");
         }
      }
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
      long lastCombat = npc.getPersistentData().getLong("TypeMoonNpcLastCombatTick");
      if (vars.is_magic_circuit_open && gameTime - lastCombat >= 120L) {
         vars.is_magic_circuit_open = false;
         vars.magic_circuit_open_timer = 0.0;
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

   private static boolean isValidCombatTarget(MysticMagicianEntity npc, LivingEntity target) {
      if (target != null && target.isAlive() && !target.isRemoved()) {
         if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
         } else {
            return target == npc ? false : npc.distanceToSqr(target) <= 784.0;
         }
      } else {
         return false;
      }
   }

   private static boolean shouldRetreat(MysticMagicianEntity npc, LivingEntity target) {
      if (npc != null && target != null && target.isAlive()) {
         double maxHealth = Math.max(1.0, (double)npc.getMaxHealth());
         double ratio = npc.getHealth() / maxHealth;
         return ratio <= 0.28;
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
         boolean hasBuff = false;
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
                     hasBuff = true;
                     break;
                  case "gravity_magic":
                     hasControl = true;
                     break;
                  case "gander":
                  case "gandr_machine_gun":
                  case "jewel_random_shoot":
                  case "jewel_machine_gun":
                     hasRanged = true;
                     break;
                  case "projection":
                  case "broken_phantasm":
                     hasMeleeBurst = true;
               }
            }
         }

         return new NpcMagicCastBridge.MagicCapabilityProfile(hasBuff, hasControl, hasRanged, hasMeleeBurst, hasAnyCastable);
      }
   }

   private static void syncCapabilityFlags(MysticMagicianEntity npc, NpcMagicCastBridge.MagicCapabilityProfile profile) {
      if (npc != null && profile != null) {
         CompoundTag data = npc.getPersistentData();
         boolean onlyMelee = !profile.hasRanged() && profile.hasMeleeBurst();
         data.putBoolean("TypeMoonNpcCapHasRanged", profile.hasRanged());
         data.putBoolean("TypeMoonNpcCapHasMeleeBurst", profile.hasMeleeBurst());
         data.putBoolean("TypeMoonNpcCapOnlyMelee", onlyMelee);
      }
   }

   private static void maintainPreferredCombatDistance(
      MysticMagicianEntity npc, LivingEntity target, NpcMagicCastBridge.MagicCapabilityProfile capabilities, NpcMagicCastBridge.ThreatProfile threat
   ) {
      if (npc != null && target != null && target.isAlive() && capabilities != null) {
         double distance = Math.sqrt(npc.distanceToSqr(target));
         if (shouldForceMeleeEngage(capabilities, distance)) {
            npc.getNavigation().moveTo(target, 1.14);
            npc.lookAt(target, 45.0F, 45.0F);
         } else if (!capabilities.hasRanged()) {
            if (capabilities.hasMeleeBurst()) {
               if (distance > 4.2) {
                  npc.getNavigation().moveTo(target, 1.1);
               }

               npc.lookAt(target, 35.0F, 35.0F);
            }
         } else {
            double min = 7.0;
            double max = 14.0;
            if (threat != null && threat.requiresDefensiveTactics()) {
               min += 3.0;
               max += 4.0;
            } else if (npc.getCombatStyle() == NpcCombatStyle.RANGED_BURST) {
               min++;
               max += 2.0;
            }

            if (distance < min) {
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
      if (vars != null && magicId != null && !magicId.isEmpty()) {
         for (int slot = 0; slot < 12; slot++) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
            if (entry != null && !entry.isEmpty() && magicId.equals(entry.magicId) && vars.isWheelSlotEntryCastable(entry)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
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
         if (gameTime >= data.getLong("TypeMoonNpcEnvMoveTick")) {
            Vec3 safe = findNearestSafeStandPos(npc, 4);
            if (safe != null) {
               double speed = hazard.severe() ? 1.3 : 1.08;
               npc.getNavigation().moveTo(safe.x, safe.y, safe.z, speed);
               data.putLong("TypeMoonNpcEnvMoveTick", gameTime + (hazard.severe() ? 6L : 10L));
            }
         }

         if (gameTime >= getCastLockUntil(npc) && !(vars.player_mana <= 1.0)) {
            if (gameTime >= data.getLong("TypeMoonNpcEnvUtilityTick")) {
               if (capabilities.hasBuff() && hasCastableMagic(vars, "reinforcement") && gameTime >= getMagicCooldownUntil(npc, "reinforcement")) {
                  CompoundTag payload = new CompoundTag();
                  payload.putInt("reinforcement_mode", hazard.severe() ? 0 : 2);
                  payload.putInt("reinforcement_level", hazard.severe() ? 3 : 2);
                  if (castReinforcement(npc, vars, payload, Math.max(65.0, vars.proficiency_reinforcement))) {
                     setMagicCooldown(npc, "reinforcement", gameTime + 120L);
                     setCastLockUntil(npc, gameTime + 10L);
                     data.putLong("TypeMoonNpcEnvUtilityTick", gameTime + 30L);
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
                  data.putLong("TypeMoonNpcEnvUtilityTick", gameTime + 24L);
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
            && hasCastableMagic(vars, "gravity_magic")
            && gameTime >= getMagicCooldownUntil(npc, "gravity_magic")
            && distanceSqr <= 196.0) {
            CompoundTag gravityPayload = new CompoundTag();
            gravityPayload.putInt("gravity_target", 1);
            gravityPayload.putInt("gravity_mode", underPressure ? 2 : 1);
            if (castGravity(npc, target, vars, gravityPayload, Math.max(55.0, vars.proficiency_gravity_magic))) {
               int gcd = 18;
               vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
               npc.getPersistentData().putLong("TypeMoonNpcNextGlobalCastTick", gameTime + gcd);
               setMagicCooldown(npc, "gravity_magic", gameTime + 50L);
               setCastLockUntil(npc, gameTime + 10L);
               return;
            }
         }

         if (!capabilities.hasRanged() || !tryFallbackRangedFire(npc, target, vars, gameTime, threat, false)) {
            if (capabilities.hasBuff() && hasCastableMagic(vars, "reinforcement") && gameTime >= getMagicCooldownUntil(npc, "reinforcement")) {
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
               maintainPreferredCombatDistance(npc, target, capabilities, threat);
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
         if (hasCastableMagic(vars, "gander")
            && gameTime >= getMagicCooldownUntil(npc, "gander")
            && castGander(npc, target, vars, new CompoundTag(), Math.max(highThreat ? 70.0 : 50.0, vars.proficiency_gander))) {
            applyPostCastCooldown(npc, vars, "gander", new CompoundTag(), gameTime, highThreat ? 10 : 8);
            return true;
         } else {
            if (hasCastableMagic(vars, "gandr_machine_gun") && gameTime >= getMagicCooldownUntil(npc, "gandr_machine_gun")) {
               CompoundTag payload = new CompoundTag();
               payload.putInt("gandr_machine_gun_mode", highThreat && threat.enemyCount() >= 3 ? 1 : 0);
               if (castGandrMachineGun(npc, target, vars, payload, Math.max(highThreat ? 70.0 : 45.0, vars.proficiency_gander))) {
                  applyPostCastCooldown(npc, vars, "gandr_machine_gun", payload, gameTime, highThreat ? 12 : 8);
                  return true;
               }
            }

            if (hasCastableMagic(vars, "jewel_random_shoot")
               && gameTime >= getMagicCooldownUntil(npc, "jewel_random_shoot")
               && castJewelRandomShoot(npc, target, vars, vars.proficiency_jewel_magic_shoot)) {
               applyPostCastCooldown(npc, vars, "jewel_random_shoot", new CompoundTag(), gameTime, 8);
               return true;
            } else if (hasCastableMagic(vars, "jewel_machine_gun")
               && gameTime >= getMagicCooldownUntil(npc, "jewel_machine_gun")
               && castJewelMachineGun(npc, target, vars, vars.proficiency_jewel_magic_release)) {
               applyPostCastCooldown(npc, vars, "jewel_machine_gun", new CompoundTag(), gameTime, 8);
               return true;
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
      int gcd = getGlobalCooldownAfterCast(magicId, payload);
      vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
      npc.getPersistentData().putLong("TypeMoonNpcNextGlobalCastTick", gameTime + gcd);
      setMagicCooldown(npc, magicId, gameTime + getPerMagicCooldown(magicId, payload));
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
         if (gameTime < npc.getPersistentData().getLong("TypeMoonNpcJewelItemNextTick")) {
            return false;
         } else {
            double distanceSqr = npc.distanceToSqr(target);
            if (!(distanceSqr < 16.0) && !(distanceSqr > 576.0)) {
               int triggerChance = threat != null && threat.requiresDefensiveTactics() ? 42 : 26;
               if (npc.getRandom().nextInt(100) >= triggerChance) {
                  return false;
               } else {
                  String usedSkillId = null;
                  double manaRatio = vars.player_max_mana <= 0.0 ? 0.0 : Mth.clamp(vars.player_mana / vars.player_max_mana, 0.0, 1.0);
                  boolean highThreat = threat != null && threat.requiresDefensiveTactics();
                  if ((highThreat || manaRatio >= 0.45)
                     && isResourceMagicAvailable(npc, "npc_jewel_item_engraved", gameTime)
                     && castJewelItemEngraved(npc, target, vars, highThreat)) {
                     usedSkillId = "npc_jewel_item_engraved";
                  }

                  if (usedSkillId == null
                     && (highThreat || manaRatio >= 0.55)
                     && isResourceMagicAvailable(npc, "npc_jewel_item_advanced", gameTime)
                     && castJewelItemAdvanced(npc, target, vars, highThreat)) {
                     usedSkillId = "npc_jewel_item_advanced";
                  }

                  if (usedSkillId == null && isResourceMagicAvailable(npc, "npc_jewel_item_basic", gameTime) && castJewelItemBasic(npc, target, vars)) {
                     usedSkillId = "npc_jewel_item_basic";
                  }

                  if (usedSkillId == null) {
                     return false;
                  } else {
                     int gcd = switch (usedSkillId) {
                        case "npc_jewel_item_engraved" -> 16;
                        case "npc_jewel_item_advanced" -> 14;
                        default -> 10;
                     };

                     int perCd = switch (usedSkillId) {
                        case "npc_jewel_item_engraved" -> 1900;
                        case "npc_jewel_item_advanced" -> 1400;
                        default -> 900;
                     };
                     vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
                     npc.getPersistentData().putLong("TypeMoonNpcNextGlobalCastTick", gameTime + gcd);
                     setMagicCooldown(npc, usedSkillId, gameTime + perCd);
                     recordResourceMagicUse(npc, usedSkillId, gameTime);
                     recordMagicCast(npc, usedSkillId);
                     setCastLockUntil(npc, gameTime + gcd);
                     npc.getPersistentData().putLong("TypeMoonNpcJewelItemNextTick", gameTime + 30L);
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
      if (!consumeMana(vars, 26.0)) {
         return false;
      } else {
         GemType type = NPC_BASIC_JEWEL_TYPES[caster.getRandom().nextInt(NPC_BASIC_JEWEL_TYPES.length)];
         return spawnNpcGemProjectile(caster, target, type, 2.8, 0.1F, false, false, false);
      }
   }

   private static boolean castJewelItemAdvanced(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, boolean highThreat
   ) {
      if (!consumeMana(vars, 70.0)) {
         return false;
      } else {
         int mode;
         if (highThreat && caster.getHealth() / Math.max(1.0, (double)caster.getMaxHealth()) < 0.6) {
            mode = 0;
         } else {
            mode = caster.getRandom().nextInt(3);
         }

         if (mode == 0) {
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 360, 1, false, true, true));
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 360, 1, false, true, true));
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 320, 0, false, true, true));
            return true;
         } else {
            return mode == 1
               ? spawnNpcGemProjectile(caster, target, GemType.CYAN, 2.8, 0.08F, false, false, true)
               : spawnNpcGemProjectile(caster, target, GemType.SAPPHIRE, 2.6, 0.1F, false, false, false);
         }
      }
   }

   private static boolean castJewelItemEngraved(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, boolean highThreat
   ) {
      if (vars == null) {
         return false;
      } else {
         List<Integer> choices = new ArrayList<>();
         choices.add(0);
         if (hasCastableMagic(vars, "gravity_magic")) {
            choices.add(1);
         }

         if (hasCastableMagic(vars, "reinforcement")) {
            choices.add(2);
         }

         if (hasCastableMagic(vars, "projection")) {
            choices.add(3);
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
               CompoundTag payload = new CompoundTag();
               ItemStack item = NpcMagicFilterService.chooseRandomCombatProjectionItem(caster.getRandom());
               if (!item.isEmpty()) {
                  payload.put("projection_item", item.save(caster.registryAccess()));
               }

               return castProjection(caster, vars, payload, Math.max(80.0, vars.proficiency_projection), true);
            default:
               return castGander(caster, target, vars, new CompoundTag(), Math.max(80.0, vars.proficiency_gander));
         }
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
         if (gameTime >= npc.getPersistentData().getLong("TypeMoonNpcThreatUtilityTick") && tryPreBuffForHighThreat(npc, vars, gameTime, threat, capabilities)) {
            npc.getPersistentData().putLong("TypeMoonNpcThreatUtilityTick", gameTime + 35L);
            return true;
         } else if (capabilities.hasControl()
            && gameTime >= npc.getPersistentData().getLong("TypeMoonNpcThreatControlTick")
            && tryHighThreatControl(npc, target, vars, gameTime, threat)) {
            npc.getPersistentData().putLong("TypeMoonNpcThreatControlTick", gameTime + 26L);
            return true;
         } else if (capabilities.hasRanged()
            && gameTime >= npc.getPersistentData().getLong("TypeMoonNpcThreatCounterTick")
            && tryHighThreatCounterFire(npc, target, vars, gameTime, threat)) {
            npc.getPersistentData().putLong("TypeMoonNpcThreatCounterTick", gameTime + 20L);
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
         boolean hasDefense = npc.hasEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE) || npc.hasEffect(MobEffects.DAMAGE_RESISTANCE);
         boolean hasAgility = npc.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY) || npc.hasEffect(MobEffects.MOVEMENT_SPEED);
         CompoundTag payload = new CompoundTag();
         payload.putInt("reinforcement_mode", threat.strongTarget() ? 0 : 2);
         payload.putInt("reinforcement_level", threat.enemyCount() >= 3 ? 3 : 2);
         if (threat.strongTarget() && !hasDefense && castReinforcement(npc, vars, payload, Math.max(70.0, vars.proficiency_reinforcement))) {
            setMagicCooldown(npc, "reinforcement", gameTime + 120L);
            setCastLockUntil(npc, gameTime + 12L);
            return true;
         } else {
            if (threat.outnumberedOrPressured() && !hasAgility) {
               payload.putInt("reinforcement_mode", 2);
               if (castReinforcement(npc, vars, payload, Math.max(70.0, vars.proficiency_reinforcement))) {
                  setMagicCooldown(npc, "reinforcement", gameTime + 120L);
                  setCastLockUntil(npc, gameTime + 12L);
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
      } else if (gameTime < getMagicCooldownUntil(npc, "gravity_magic")) {
         return false;
      } else {
         CompoundTag gravityPayload = new CompoundTag();
         gravityPayload.putInt("gravity_target", 1);
         gravityPayload.putInt("gravity_mode", threat.strongTarget() ? 2 : 1);
         if (!castGravity(npc, target, vars, gravityPayload, Math.max(65.0, vars.proficiency_gravity_magic))) {
            return false;
         } else {
            int gcd = 18;
            vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
            npc.getPersistentData().putLong("TypeMoonNpcNextGlobalCastTick", gameTime + gcd);
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
      steerAwayFromTarget(npc, target, 16.0, 1.42);
      if (!npc.hasEffect(MobEffects.MOVEMENT_SPEED)) {
         npc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, true, true));
      }

      long lockUntil = getCastLockUntil(npc);
      if (gameTime >= lockUntil && !(vars.player_mana <= 1.0)) {
         if (gameTime >= npc.getPersistentData().getLong("TypeMoonNpcRetreatUtilityTick")) {
            boolean usedUtility = tryRetreatRecoveryOrEscapeMagic(npc, target, vars, gameTime);
            if (usedUtility) {
               npc.getPersistentData().putLong("TypeMoonNpcRetreatUtilityTick", gameTime + 30L);
               return;
            }
         }

         if (gameTime >= npc.getPersistentData().getLong("TypeMoonNpcRetreatCounterTick") && npc.distanceToSqr(target) <= 484.0) {
            CompoundTag payload = new CompoundTag();
            boolean cast = castGander(npc, target, vars, payload, Math.max(40.0, vars.proficiency_gander));
            if (cast) {
               int gcd = 14;
               vars.magic_cooldown = Math.max(vars.magic_cooldown, (double)gcd);
               npc.getPersistentData().putLong("TypeMoonNpcNextGlobalCastTick", gameTime + gcd);
               setMagicCooldown(npc, "gander", gameTime + 26L);
               setCastLockUntil(npc, gameTime + 10L);
               npc.getPersistentData().putLong("TypeMoonNpcRetreatCounterTick", gameTime + 22L);
            }
         }
      }
   }

   private static boolean tryRetreatRecoveryOrEscapeMagic(
      MysticMagicianEntity npc, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, long gameTime
   ) {
      double maxHealth = Math.max(1.0, (double)npc.getMaxHealth());
      double ratio = npc.getHealth() / maxHealth;
      if (ratio <= 0.55 && !npc.hasEffect(MobEffects.REGENERATION) && consumeMana(vars, 25.0)) {
         npc.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true));
         npc.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false, true));
         setCastLockUntil(npc, gameTime + 10L);
         return true;
      } else if (!npc.hasEffect(MobEffects.MOVEMENT_SPEED) && consumeMana(vars, 18.0)) {
         npc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, true, true));
         npc.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_AGILITY, 120, 0, false, false, true));
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
      return npc.getPersistentData().getLong("TypeMoonNpcCastLockUntil");
   }

   private static void setCastLockUntil(MysticMagicianEntity npc, long until) {
      npc.getPersistentData().putLong("TypeMoonNpcCastLockUntil", until);
   }

   private static boolean isResourceMagic(String magicId) {
      return "projection".equals(magicId)
         || "broken_phantasm".equals(magicId)
         || "jewel_random_shoot".equals(magicId)
         || "jewel_machine_gun".equals(magicId)
         || "npc_jewel_item_basic".equals(magicId)
         || "npc_jewel_item_advanced".equals(magicId)
         || "npc_jewel_item_engraved".equals(magicId);
   }

   private static int getResourceWindowTicks(String magicId) {
      return switch (magicId) {
         case "projection" -> 900;
         case "broken_phantasm" -> 1200;
         case "jewel_random_shoot" -> 600;
         case "jewel_machine_gun" -> 1000;
         case "npc_jewel_item_basic" -> 1800;
         case "npc_jewel_item_advanced" -> 3000;
         case "npc_jewel_item_engraved" -> 3600;
         default -> 0;
      };
   }

   private static int getResourceUseLimit(String magicId) {
      return switch (magicId) {
         case "projection" -> 2;
         case "broken_phantasm" -> 1;
         case "jewel_random_shoot" -> 6;
         case "jewel_machine_gun" -> 2;
         case "npc_jewel_item_basic" -> 3;
         case "npc_jewel_item_advanced" -> 2;
         case "npc_jewel_item_engraved" -> 1;
         default -> Integer.MAX_VALUE;
      };
   }

   private static int getResourceExhaustionCooldown(String magicId) {
      return switch (magicId) {
         case "projection" -> 2400;
         case "broken_phantasm" -> 3600;
         case "jewel_random_shoot" -> 1800;
         case "jewel_machine_gun" -> 2400;
         case "npc_jewel_item_basic" -> 2400;
         case "npc_jewel_item_advanced" -> 4400;
         case "npc_jewel_item_engraved" -> 5200;
         default -> 0;
      };
   }

   private static boolean isResourceMagicAvailable(MysticMagicianEntity npc, String magicId, long gameTime) {
      if (!isResourceMagic(magicId)) {
         return true;
      } else if (gameTime < getMagicCooldownUntil(npc, magicId)) {
         return false;
      } else {
         CompoundTag all = npc.getPersistentData().getCompound("TypeMoonNpcResourceUse");
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
         CompoundTag all = root.getCompound("TypeMoonNpcResourceUse");
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
         root.put("TypeMoonNpcResourceUse", all);
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
               if (entry.presetPayload == null) {
                  entry.presetPayload = new CompoundTag();
               } else {
                  entry.presetPayload = NpcMagicFilterService.sanitizePresetForNpc(entry.magicId, entry.presetPayload, lookup, npc.getRandom());
               }

               boolean crestCast = "crest".equals(entry.sourceType);
               double proficiency = getEffectiveProficiency(vars, entry.magicId, crestCast);
               if (canCastWithMana(vars, entry.magicId, entry.presetPayload, proficiency)
                  && gameTime >= getMagicCooldownUntil(npc, entry.magicId)
                  && isResourceMagicAvailable(npc, entry.magicId, gameTime)
                  && (!forceMeleeEngage || !isPureRangedMagic(entry.magicId))) {
                  double weight = computeMagicWeight(style, entry.magicId, entry.presetPayload, distance, manaRatio);
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
            choices.sort(Comparator.comparingDouble(NpcMagicCastBridge.Choice::weight).reversed());
            return weightedPick(choices, npc.getRandom()).entry();
         }
      }
   }

   private static void applyAdvancedAndVarietyWeighting(MysticMagicianEntity npc, List<NpcMagicCastBridge.Choice> choices, double manaRatio) {
      if (npc != null && choices != null && !choices.isEmpty()) {
         boolean hasGandrMachineGun = hasMagicChoice(choices, "gandr_machine_gun");
         boolean hasGander = hasMagicChoice(choices, "gander");
         boolean hasJewelMachineGun = hasMagicChoice(choices, "jewel_machine_gun");
         boolean hasJewelRandom = hasMagicChoice(choices, "jewel_random_shoot");
         boolean hasBrokenPhantasm = hasMagicChoice(choices, "broken_phantasm");
         boolean hasProjection = hasMagicChoice(choices, "projection");
         String lastMagic = npc.getPersistentData().getString("TypeMoonNpcLastCastMagic");
         int repeat = npc.getPersistentData().getInt("TypeMoonNpcCastRepeatCount");

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

            if (hasBrokenPhantasm && hasProjection && manaRatio >= 0.55) {
               if ("broken_phantasm".equals(magicId)) {
                  w *= 1.35;
               } else if ("projection".equals(magicId)) {
                  w *= 0.78;
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

   private static double computeMagicWeight(NpcCombatStyle style, String magicId, CompoundTag payload, double distance, double manaRatio) {
      double weight = 1.0;
      switch (style) {
         case CLOSE_PRESSURE:
            if ("reinforcement".equals(magicId)) {
               weight += 2.8;
            }

            if ("projection".equals(magicId) || "broken_phantasm".equals(magicId)) {
               weight += 2.2;
            }

            if ("gander".equals(magicId)) {
               weight++;
            }
            break;
         case RANGED_BURST:
            if ("gandr_machine_gun".equals(magicId) || "jewel_machine_gun".equals(magicId)) {
               weight += 3.0;
            }

            if ("jewel_random_shoot".equals(magicId)) {
               weight++;
            }

            if ("gander".equals(magicId)) {
               weight++;
            }
            break;
         case CONTROL_DRAIN:
            if ("gravity_magic".equals(magicId)) {
               weight += 3.2;
            }

            if ("gander".equals(magicId)) {
               weight++;
            }

            if ("reinforcement".equals(magicId)) {
               weight += 0.8;
            }
            break;
         case BALANCED:
            if ("gravity_magic".equals(magicId) || "gander".equals(magicId)) {
               weight++;
            }
      }

      if (distance < 5.0) {
         if ("projection".equals(magicId) || "reinforcement".equals(magicId) || "broken_phantasm".equals(magicId)) {
            weight++;
         }

         if ("gandr_machine_gun".equals(magicId) || "jewel_machine_gun".equals(magicId)) {
            weight *= 0.65;
         }
      } else if (distance > 14.0) {
         if ("projection".equals(magicId) || "broken_phantasm".equals(magicId) || "reinforcement".equals(magicId)) {
            weight *= 0.45;
         }

         if ("gandr_machine_gun".equals(magicId) || "jewel_machine_gun".equals(magicId) || "gander".equals(magicId)) {
            weight += 0.8;
         }
      }

      if (manaRatio < 0.3) {
         if ("gandr_machine_gun".equals(magicId) || "jewel_machine_gun".equals(magicId) || "broken_phantasm".equals(magicId)) {
            weight *= 0.2;
         } else if ("gander".equals(magicId) || "jewel_random_shoot".equals(magicId) || "gravity_magic".equals(magicId)) {
            weight *= 0.75;
         }
      }

      if ("gravity_magic".equals(magicId) && payload.contains("gravity_target") && payload.getInt("gravity_target") == 0 && distance < 6.0) {
         weight += 0.5;
      }

      return weight;
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
            case "gander", "gandr_machine_gun", "jewel_random_shoot", "jewel_machine_gun" -> true;
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
         case "gandr_machine_gun" -> isBarrageMode(payload) ? getBarrageShotCount(proficiency) * 20.0 : 60.0;
         case "gravity_magic" -> 20.0;
         case "reinforcement" -> 20.0 * Mth.clamp(payload.getInt("reinforcement_level"), 1, 5);
         case "projection" -> 45.0;
         case "broken_phantasm" -> 70.0;
         case "jewel_random_shoot" -> 30.0;
         case "jewel_machine_gun" -> 90.0;
         default -> 40.0;
      };
   }

   private static boolean castMagic(
      MysticMagicianEntity caster,
      LivingEntity target,
      TypeMoonWorldModVariables.PlayerVariables vars,
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slot,
      double effectiveProficiency,
      long gameTime
   ) {
      boolean crestCast = "crest".equals(slot.sourceType);
      String var9 = slot.magicId;

      return switch (var9) {
         case "gander" -> castGander(caster, target, vars, slot.presetPayload, effectiveProficiency);
         case "gandr_machine_gun" -> castGandrMachineGun(caster, target, vars, slot.presetPayload, effectiveProficiency);
         case "gravity_magic" -> castGravity(caster, target, vars, slot.presetPayload, effectiveProficiency);
         case "reinforcement" -> castReinforcement(caster, vars, slot.presetPayload, effectiveProficiency);
         case "projection" -> castProjection(caster, vars, slot.presetPayload, effectiveProficiency, crestCast);
         case "broken_phantasm" -> castBrokenPhantasm(caster, target, vars, slot.presetPayload, effectiveProficiency, crestCast);
         case "jewel_random_shoot" -> castJewelRandomShoot(caster, target, vars, effectiveProficiency);
         case "jewel_machine_gun" -> castJewelMachineGun(caster, target, vars, effectiveProficiency);
         default -> false;
      };
   }

   private static int getGlobalCooldownAfterCast(String magicId, CompoundTag payload) {
      return switch (magicId) {
         case "gander" -> 16;
         case "gandr_machine_gun" -> isBarrageMode(payload) ? 24 : 10;
         case "gravity_magic" -> 24;
         case "reinforcement" -> 30;
         case "projection" -> 20;
         case "broken_phantasm" -> 26;
         case "jewel_random_shoot" -> 10;
         case "jewel_machine_gun" -> 8;
         default -> 12;
      };
   }

   private static int getPerMagicCooldown(String magicId, CompoundTag payload) {
      return switch (magicId) {
         case "gander" -> 18;
         case "gandr_machine_gun" -> isBarrageMode(payload) ? 36 : 16;
         case "gravity_magic" -> 60;
         case "reinforcement" -> 120;
         case "projection" -> 80;
         case "broken_phantasm" -> 120;
         case "jewel_random_shoot" -> 10;
         case "jewel_machine_gun" -> 14;
         default -> 20;
      };
   }

   private static boolean castGander(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      int chargeSeconds = getGanderChargeSeconds(proficiency);
      double cost = chargeSeconds * 5.0;
      if (!consumeMana(vars, cost)) {
         return false;
      } else {
         markCastingPose(caster, 12);
         Vec3 direction = getAimDirection(caster, target, 3.8, 0.0);
         faceCasterToDirection(caster, direction);
         Vec3 spawnPos = EntityUtils.getRightHandCastAnchor(caster).add(direction.scale(0.1));
         GanderProjectileEntity projectile = new GanderProjectileEntity(caster.level(), caster);
         projectile.setNoGravity(true);
         projectile.setChargeSeconds(chargeSeconds);
         projectile.setVisualScale(MagicGander.getVisualScaleForChargeSeconds(chargeSeconds));
         projectile.setItem(new ItemStack((ItemLike)ModItems.GANDER.get()));
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

   private static boolean castGandrMachineGun(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      boolean barrage = isBarrageMode(payload);
      if (barrage) {
         int shotCount = getBarrageShotCount(proficiency);
         double cost = shotCount * 20.0;
         if (!consumeMana(vars, cost)) {
            return false;
         } else {
            markCastingPose(caster, 16);
            int chargeSeconds = getGanderChargeSeconds(proficiency);
            fireNpcBarrage(caster, target, shotCount, chargeSeconds);
            caster.level()
               .playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 0.65F, 1.2F);
            return true;
         }
      } else {
         double cost = 60.0;
         if (!consumeMana(vars, cost)) {
            return false;
         } else {
            markCastingPose(caster, 12);
            int chargeSeconds = getGanderChargeSeconds(proficiency);
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
               projectile.setVisualScale(MagicGander.getVisualScaleForChargeSeconds(chargeSeconds));
               projectile.setItem(new ItemStack((ItemLike)ModItems.GANDER.get()));
               Vec3 spawn = hand.add(forward.scale(0.08)).add(right.scale(RAPID_SIDE[i])).add(up.scale(RAPID_UP[i]));
               projectile.setPos(spawn);
               Vec3 spreadDir = applyDirectionalSpread(forward, right, up, RAPID_YAW[i], RAPID_PITCH[i]);
               projectile.shoot(spreadDir.x, spreadDir.y, spreadDir.z, 3.8F, 0.1F);
               level.addFreshEntity(projectile);
            }

            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.55F, 1.12F);
            return true;
         }
      }
   }

   private static void fireNpcBarrage(MysticMagicianEntity caster, LivingEntity target, int shotCount, int chargeSeconds) {
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
         projectile.setVisualScale(MagicGander.getVisualScaleForChargeSeconds(chargeSeconds));
         projectile.setItem(new ItemStack((ItemLike)ModItems.GANDER.get()));
         projectile.setPos(spawn);
         projectile.shoot(dir.x, dir.y, dir.z, 3.5F + random.nextFloat() * 0.7F, 0.12F);
         level.addFreshEntity(projectile);
      }
   }

   private static boolean castGravity(
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
         } else {
            int duration = getGravityDurationTicks(proficiency);
            MagicGravityEffectHandler.applyGravityState(actualTarget, mode, caster.level().getGameTime() + duration);
         }

         return true;
      }
   }

   private static boolean castReinforcement(
      MysticMagicianEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency
   ) {
      int mode = payload.contains("reinforcement_mode") ? Mth.clamp(payload.getInt("reinforcement_mode"), 0, 3) : 0;
      int level = payload.contains("reinforcement_level") ? Mth.clamp(payload.getInt("reinforcement_level"), 1, 5) : 1;
      double cost = 20.0 * level;
      if (!consumeMana(vars, cost)) {
         return false;
      } else {
         int duration = (600 + (int)(proficiency * 10.0)) * level;
         int amplifier = level - 1;
         switch (mode) {
            case 0:
               caster.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_DEFENSE, duration, amplifier, false, false, true));
               break;
            case 1:
               caster.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_STRENGTH, duration, amplifier, false, false, true));
               break;
            case 2:
               caster.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_AGILITY, duration, amplifier, false, false, true));
               break;
            case 3:
               caster.addEffect(new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_SIGHT, duration, amplifier, false, false, true));
               caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false));
         }

         return true;
      }
   }

   private static boolean castProjection(
      MysticMagicianEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag payload, double proficiency, boolean crestCast
   ) {
      Provider lookup = caster.registryAccess();
      ItemStack target = readItem(payload, "projection_item", lookup);
      if (!NpcMagicFilterService.isCombatProjectionItem(target)) {
         target = NpcMagicFilterService.chooseRandomCombatProjectionItem(caster.getRandom());
      }

      if (!target.isEmpty() && !(target.getItem() instanceof AvalonItem)) {
         boolean hasSwordAttribute = vars.player_magic_attributes_sword && !crestCast;
         double cost = MagicProjection.calculateCost(target, hasSwordAttribute, proficiency);
         if (!consumeMana(vars, cost)) {
            return false;
         } else {
            ItemStack projected = target.copy();
            projected.setCount(1);
            projected.remove(DataComponents.CONTAINER);
            projected.remove(DataComponents.BUNDLE_CONTENTS);
            projected.remove(DataComponents.BLOCK_ENTITY_DATA);
            if (projected.isDamageableItem()) {
               projected.setDamageValue(projected.getMaxDamage() - 1);
            }

            CompoundTag tag = ((CustomData)projected.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            tag.putBoolean("is_projected", true);
            tag.putLong("projection_time", caster.level().getGameTime());
            projected.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            projected.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            boolean canDualWield = caster.getMainHandItem().isEmpty() && caster.getOffhandItem().isEmpty() && caster.getRandom().nextFloat() < 0.12F;
            EquipmentSlot primarySlot;
            if (caster.getMainHandItem().isEmpty()) {
               primarySlot = EquipmentSlot.MAINHAND;
            } else if (caster.getOffhandItem().isEmpty()) {
               primarySlot = EquipmentSlot.OFFHAND;
            } else {
               primarySlot = caster.getMainArm() == HumanoidArm.RIGHT ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            }

            caster.setItemSlot(primarySlot, projected.copy());
            if (canDualWield) {
               EquipmentSlot secondary = primarySlot == EquipmentSlot.MAINHAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
               if (caster.getItemBySlot(secondary).isEmpty()) {
                  caster.setItemSlot(secondary, projected.copy());
               }
            }

            vars.projection_selected_item = target.copy();
            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean castBrokenPhantasm(
      MysticMagicianEntity caster,
      LivingEntity target,
      TypeMoonWorldModVariables.PlayerVariables vars,
      CompoundTag payload,
      double proficiency,
      boolean crestCast
   ) {
      ItemStack source = findValidBrokenPhantasmItem(caster);
      if (source.isEmpty()) {
         boolean projected = castProjection(caster, vars, payload, proficiency, crestCast);
         if (!projected) {
            return false;
         }

         source = findValidBrokenPhantasmItem(caster);
         if (source.isEmpty()) {
            return false;
         }
      }

      double manaCost = Math.min(220.0, Math.max(40.0, MagicBrokenPhantasm.calculateCost(source, false) * 0.2));
      if (!consumeMana(vars, manaCost)) {
         return false;
      } else {
         float power = (float)(MagicBrokenPhantasm.calculateCost(source, false) / 20.0);
         BrokenPhantasmProjectileEntity projectile = new BrokenPhantasmProjectileEntity(caster.level(), caster, source.copy());
         projectile.setExplosionPower(power);
         markCastingPose(caster, 10);
         Vec3 direction = getAimDirection(caster, target, 3.0, 0.04);
         faceCasterToDirection(caster, direction);
         projectile.shoot(direction.x, direction.y, direction.z, 3.0F, 1.0F);
         caster.level().addFreshEntity(projectile);
         source.shrink(1);
         return true;
      }
   }

   private static boolean castJewelRandomShoot(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (!consumeMana(vars, 30.0)) {
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

   private static boolean castJewelMachineGun(
      MysticMagicianEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency
   ) {
      if (!consumeMana(vars, 90.0)) {
         return false;
      } else {
         markCastingPose(caster, 12);
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

   private static ItemStack findValidBrokenPhantasmItem(MysticMagicianEntity caster) {
      ItemStack main = caster.getMainHandItem();
      if (isValidBrokenPhantasmItem(main)) {
         return main;
      } else {
         ItemStack off = caster.getOffhandItem();
         return isValidBrokenPhantasmItem(off) ? off : ItemStack.EMPTY;
      }
   }

   private static boolean isValidBrokenPhantasmItem(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (stack.getItem() instanceof NoblePhantasmItem && !(stack.getItem() instanceof AvalonItem)) {
         return true;
      } else {
         CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (customData == null) {
            return false;
         } else {
            CompoundTag tag = customData.copyTag();
            return tag.getBoolean("is_projected");
         }
      }
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

   private static int getGravityDurationTicks(double proficiency) {
      double t = Mth.clamp(proficiency, 0.0, 100.0) / 100.0;
      return 600 + (int)Math.round(3000.0 * t);
   }

   private static boolean isBarrageMode(CompoundTag payload) {
      return payload != null && payload.contains("gandr_machine_gun_mode") && Mth.clamp(payload.getInt("gandr_machine_gun_mode"), 0, 1) == 1;
   }

   private static long getMagicCooldownUntil(MysticMagicianEntity npc, String magicId) {
      CompoundTag all = npc.getPersistentData().getCompound("TypeMoonNpcMagicCd");
      return all.contains(magicId) ? all.getLong(magicId) : 0L;
   }

   private static void setMagicCooldown(MysticMagicianEntity npc, String magicId, long until) {
      CompoundTag all = npc.getPersistentData().getCompound("TypeMoonNpcMagicCd");
      all.putLong(magicId, until);
      npc.getPersistentData().put("TypeMoonNpcMagicCd", all);
   }

   private static void recordMagicCast(MysticMagicianEntity npc, String magicId) {
      if (npc != null && magicId != null && !magicId.isEmpty()) {
         CompoundTag data = npc.getPersistentData();
         String last = data.getString("TypeMoonNpcLastCastMagic");
         if (magicId.equals(last)) {
            int repeat = data.getInt("TypeMoonNpcCastRepeatCount");
            data.putInt("TypeMoonNpcCastRepeatCount", Math.min(8, repeat + 1));
         } else {
            data.putString("TypeMoonNpcLastCastMagic", magicId);
            data.putInt("TypeMoonNpcCastRepeatCount", 1);
         }
      }
   }

   private static double getEffectiveProficiency(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, boolean crestCast) {
      if (crestCast) {
         return 100.0;
      } else {
         return switch (magicId) {
            case "gander", "gandr_machine_gun" -> vars.proficiency_gander;
            case "gravity_magic" -> vars.proficiency_gravity_magic;
            case "reinforcement" -> vars.proficiency_reinforcement;
            case "projection", "broken_phantasm" -> vars.proficiency_projection;
            case "jewel_random_shoot" -> vars.proficiency_jewel_magic_shoot;
            case "jewel_machine_gun" -> vars.proficiency_jewel_magic_release;
            default -> 0.0;
         };
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
}
