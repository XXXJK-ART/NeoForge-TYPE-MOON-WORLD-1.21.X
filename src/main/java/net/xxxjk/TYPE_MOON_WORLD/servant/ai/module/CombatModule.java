package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantEngagementService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantCombatTempoService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTargetingService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.NightingaleEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ParacelsusServantSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantNoblePhantasmExecutor;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantLifecycleContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatMotionService;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSpecialization;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.CombatDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.registry.ServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;

import java.util.List;

public final class CombatModule implements ServantAiModule {
   private static final double SLAM_RADIUS = 4.0;
   private static final double SLAM_DAMAGE_MULTIPLIER = 1.5;
   private static final int BASIC_ATTACK_COOLDOWN = 12;
   private static final int ASSASSIN_BASIC_ATTACK_COOLDOWN = 6;
   private static final int ROAR_COOLDOWN = 300;
   private static final int SLAM_COOLDOWN = 120;
   private static final int SWEEP_COOLDOWN = 100;
   private static final int CHARGE_COOLDOWN = 160;
   private static final int SLASH_COOLDOWN = 60;
   private static final int TELEPORT_COOLDOWN = 120;
   private static final int STOMP_COOLDOWN = 80;
   private static final int UPPERCUT_COOLDOWN = 50;
   private static final int H_SWING_COOLDOWN = 50;
   private static final int CU_LUNGING_THRUST_COOLDOWN = 55;
   private static final int CU_DRIVING_SLASH_COOLDOWN = 65;
   private static final int CU_SWEEPING_ADVANCE_COOLDOWN = 70;
   private static final int EARTH_REND_COOLDOWN = 90;
   private static final int SHOULDER_CHECK_COOLDOWN = 70;
   private static final int RUNE_BURST_COOLDOWN = 95;
   private static final int SPEAR_VAULT_COOLDOWN = 75;
   private static final int AFTERIMAGE_SLASH_COOLDOWN = 65;
   private static final int IAIJUTSU_STEP_COOLDOWN = 80;
   private static final int BLOCK_BREAK_COOLDOWN = 15;
   private static final int UNDERGROUND_TARGET_TIMEOUT = 80;
   private static final int COMBAT_PATH_RECALC_INTERVAL = 8;
   private static final int SURROUNDED_SCAN_INTERVAL = 5;
   private static final int COMBAT_LOS_INTERVAL = 3;
   private static final int CU_RECAST_MP_COST = 25;
   private static final int CU_RUNE_MP_COST = 20;
   private static final ResourceLocation VALOR_RES = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "frenzy_atk_boost");
   private static final ResourceLocation FRENZY_SPEED_RES = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "frenzy_speed_boost");
   private static final int PARACELSUS_CANNON_SUMMON_COOLDOWN = 240;
   private static final int PARACELSUS_MAGIC_AI_INTERVAL = 50;
   private static final String TAG_PARACELSUS_LAST_AI_MAGIC = "ParacelsusLastAiMagicTick";

   private boolean destroyBlockWithCombatFx(ServerLevel level, BlockPos pos, BlockState state, boolean heavyFx) {
      float hardness = state.getDestroySpeed(level, pos);
      if (state.isAir() || hardness < 0.0F || state.is(Blocks.BEDROCK)) {
         return false;
      }
      if (!level.removeBlock(pos, false)) {
         return false;
      }
      return true;
   }

   private boolean isInIrregularBreakShape(ServerLevel level, BlockPos pos, BlockPos center, double radius, double yScale, double edgeNoise, double keepChance) {
      double dx = pos.getX() - center.getX();
      double dy = (pos.getY() - center.getY()) / Math.max(0.25, yScale);
      double dz = pos.getZ() - center.getZ();
      double normalized = (dx * dx + dy * dy + dz * dz) / Math.max(0.25, radius * radius);
      double noise = blockNoise(level, pos);
      double edge = 1.0 + (noise - 0.5) * edgeNoise;
      return normalized <= edge && (normalized <= 0.45 || noise <= keepChance);
   }

   private double blockNoise(ServerLevel level, BlockPos pos) {
      long seed = pos.asLong() ^ (level.getGameTime() * 341873128712L);
      seed ^= seed >>> 33;
      seed *= 0xff51afd7ed558ccdL;
      seed ^= seed >>> 33;
      seed *= 0xc4ceb9fe1a85ec53L;
      seed ^= seed >>> 33;
      return (double)(seed & 0xFFFFFFL) / (double)0x1000000;
   }

   private double terrainBreakScale(ServantEntity entity) {
      ServantParams params = entity.getDefinition() != null ? entity.getDefinition().parameters() : null;
      if (params == null) {
         return 1.0;
      }
      int strength = params.strengthPlus() ? params.strength().plusCoefficient() : params.strength().coefficient();
      return Math.max(0.65, Math.min(1.8, Math.sqrt(strength / 30.0)));
   }

   private int scaledBreakLimit(int base, double scale) {
      return Math.max(1, (int)Math.round(base * scale * scale));
   }

   private void pushWithImpactCrater(ServerLevel level, ServantEntity attacker, LivingEntity target, Vec3 horizontal, double horizontalPower, double verticalPower, double craterRadius, int maxBroken) {
      Vec3 dir = new Vec3(horizontal.x, 0.0, horizontal.z);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = target.position().subtract(attacker.position()).multiply(1.0, 0.0, 1.0);
      }
      dir = dir.lengthSqr() < 1.0E-4 ? attacker.getLookAngle().multiply(1.0, 0.0, 1.0) : dir.normalize();
      long now = level.getGameTime();
      CompoundTag data = attacker.getPersistentData();
      long lastControl = data.getLong("CombatControlLastTick");
      if (lastControl <= 0L || now - lastControl > 120L) {
         data.putLong("CombatControlStartTick", now);
      }
      data.putLong("CombatControlLastTick", now);
      long combatAge = now - data.getLong("CombatControlStartTick");
      boolean strongMove = horizontalPower >= 1.6 || verticalPower >= 0.65;
      double terrainScale = terrainBreakScale(attacker);
      boolean canKnockback = combatAge >= 45L
         && now - data.getLong("CombatLastKnockbackTick") >= (strongMove ? 64L : 84L);
      boolean canLaunch = canKnockback
         && strongMove
         && combatAge >= 90L
         && now - data.getLong("CombatLastLaunchTick") >= 145L;
      if (!canKnockback) {
         level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.25, target.getZ(), 6, 0.22, 0.12, 0.22, 0.035);
         return;
      }
      data.putLong("CombatLastKnockbackTick", now);
      if (canLaunch) {
         data.putLong("CombatLastLaunchTick", now);
      }
      double yPower = canLaunch ? Math.max(0.35, verticalPower) : Math.min(0.08, verticalPower * 0.12);
      TerrainImpactProfile.Tier tier = strongMove
         ? TerrainImpactProfile.Tier.HEAVY : canLaunch ? TerrainImpactProfile.Tier.MEDIUM : TerrainImpactProfile.Tier.SMALL;
      ServantCombatMotionService.launch(attacker, target, dir, horizontalPower, yPower, tier, canLaunch ? 26 : 12);
      level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.25, target.getZ(), 16, 0.35, 0.18, 0.35, 0.08);
   }

   private void breakKnockbackPath(ServerLevel level, Vec3 start, Vec3 dir, double distance, boolean heavyFx, double terrainScale) {
      int radius = Math.max(1, (int)Math.ceil((heavyFx ? 2 : 1) * terrainScale));
      int maxBroken = scaledBreakLimit(heavyFx ? 22 : 12, terrainScale);
      int broken = 0;
      for (double step = 0.75; step <= distance && broken < maxBroken; step += 0.75) {
         BlockPos center = BlockPos.containing(start.add(dir.scale(step)));
         for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 2, radius))) {
            if (!isInIrregularBreakShape(level, pos, center, radius + 0.45, 1.1, 0.8, heavyFx ? 0.68 : 0.55)) {
               continue;
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (!state.isAir() && hardness >= 0.0F && hardness < (heavyFx ? 45.0F : 25.0F) && destroyBlockWithCombatFx(level, pos, state, heavyFx)) {
               broken++;
               if (broken >= maxBroken) {
                  break;
               }
            }
         }
      }
   }

   private void scheduleImpactCrater(LivingEntity target, double radius, int maxBroken, boolean heavyFx) {
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(10, () -> {
         if (!target.isAlive() || !(target.level() instanceof ServerLevel level)) {
            return;
         }
         BlockPos center = target.blockPosition().below();
         int r = Math.max(1, (int)Math.ceil(radius));
         int broken = 0;
         double radiusSqr = radius * radius;
         for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
            if (broken >= maxBroken) {
               break;
            }
            if (!isInIrregularBreakShape(level, pos, center, radius, 0.75, 0.55, heavyFx ? 0.74 : 0.64)) {
               continue;
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness >= 0.0F && hardness < (heavyFx ? 70.0F : 45.0F) && destroyBlockWithCombatFx(level, pos, state, heavyFx)) {
               broken++;
            }
         }
         if (broken > 0) {
            Vec3 impact = Vec3.atCenterOf(center).add(0.0, 0.4, 0.0);
            level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, heavyFx ? 5 : 2, radius * 0.35, 0.15, radius * 0.35, 0.0);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, impact.x, impact.y, impact.z, heavyFx ? 42 : 24, radius * 0.55, 0.28, radius * 0.55, 0.045);
            level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, heavyFx ? 1.4F : 0.9F, heavyFx ? 0.55F : 0.75F);
         }
      });
   }

   private boolean maybeCastParacelsusElementalMagic(ParacelsusEntity entity, LivingEntity target, boolean hasLineOfSight, ServantAiContext context) {
      long now = context.gameTick();
      if (entity.getCurrentMp() < 7.0 || !hasLineOfSight
         || ParacelsusServantSkills.isNoblePhantasmChanting(entity, now)
         || !ParacelsusServantSkills.combatActionReady(entity, now)
         || now - entity.getPersistentData().getLong(TAG_PARACELSUS_LAST_AI_MAGIC)
            < (entity.getPersistentData().getLong("ParacelsusHighSpeedChantingUntil") > now ? 20 : PARACELSUS_MAGIC_AI_INTERVAL)) {
         return false;
      }
      if (!EntityUtils.isValidCombatTarget(entity, target)) {
         return false;
      }
      String[] actions = entity.getCurrentMp() > entity.getMaxMp() * 0.55
         ? new String[]{"fire_magic_a_cast", "water_magic_a_cast", "earth_magic_a_cast", "wind_magic_a_cast", "fire_magic_b_cast", "water_magic_b_cast", "earth_magic_b_cast", "wind_magic_b_cast"}
         : new String[]{"water_magic_b_cast", "earth_magic_b_cast", "wind_magic_a_cast", "fire_magic_b_cast", "water_magic_a_cast", "earth_magic_a_cast", "wind_magic_b_cast", "fire_magic_a_cast"};
      // High-speed chanting shortens the next decision interval, but it never
      // permits several damaging actions in the same tick.
      int casts = 1;
      boolean castAny = false;
      int decisionInterval = entity.getPersistentData().getLong("ParacelsusHighSpeedChantingUntil") > now
         ? 20 : PARACELSUS_MAGIC_AI_INTERVAL;
      int start = (int)((now / decisionInterval) % actions.length);
      for (int i = 0; i < casts; i++) {
         if (entity.getCurrentMp() < 7.0) {
            break;
         }
         String action = actions[(start + i) % actions.length];
         ServantExecutionResult result = ServantAddonRegistry.executeCombatAction(
            new ServantCombatActionContext(entity, target, context, context.definition(), action, entity.distanceTo(target), hasLineOfSight, now)
         );
         if (result.handled() && result.success()) {
            castAny = true;
            ParacelsusServantSkills.markCombatAction(entity, now, 20L);
            break;
         }
      }
      if (castAny) {
         entity.getPersistentData().putLong(TAG_PARACELSUS_LAST_AI_MAGIC, now);
      }
      return castAny;
   }

   private boolean maybeSummonParacelsusCannon(ParacelsusEntity entity, LivingEntity target, boolean hasLineOfSight, ServantAiContext context) {
      long now = context.gameTick();
      if (entity.getCurrentMp() < 18.0
         || !hasLineOfSight
         || ParacelsusServantSkills.isNoblePhantasmChanting(entity, now)
         || !ParacelsusServantSkills.combatActionReady(entity, now)
         || now % 80 != 0
         || !EntityUtils.isValidCombatTarget(entity, target)
         || now - entity.getPersistentData().getLong("ParacelsusLastElementalSpiritSummon") < PARACELSUS_CANNON_SUMMON_COOLDOWN) {
         return false;
      }
      if (entity.distanceTo(target) > 22.0) {
         return false;
      }
      ServantExecutionResult result = ServantAddonRegistry.executeCombatAction(
         new ServantCombatActionContext(entity, target, context, context.definition(), "elemental_spirit", entity.distanceTo(target), hasLineOfSight, now)
      );
      boolean success = result.handled() && result.success();
      if (success) ParacelsusServantSkills.markCombatAction(entity, now, 20L);
      return success;
   }

   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      LivingEntity engagementTarget = context.target();
      if (engagementTarget != null && engagementTarget.isAlive()) {
         ServantNavigationHelper.tryMeleeClosingBurst(entity, engagementTarget, context.gameTick());
      }
      if (entity instanceof ArashEntity arash) {
         ArashCombatHelper.tick(arash, context);
         return;
      }
      // Nightingale owns a strict ranged/melee alternation in her entity tick.
      if (entity instanceof NightingaleEntity) {
         return;
      }
      LivingEntity sharedTarget = context.target();
      if (sharedTarget != null && ServantCombatSystem.skillsSuppressed(entity)) {
         entity.getLookControl().setLookAt(sharedTarget, 30.0F, 30.0F);
         if (entity.distanceTo(sharedTarget) <= 2.7 && !entity.isPerformingAction()) {
            entity.triggerAttackSwing();
            entity.doHurtTarget(sharedTarget);
         } else {
            moveToTargetThrottled(entity, sharedTarget, 1.1, (int)entity.level().getGameTime(), 0.8);
         }
         return;
      }
      if (entity instanceof EmiyaArcherEntity emiya) {
         EmiyaArcherCombatHelper.tick(emiya);
         return;
      }
      if (entity instanceof EnkiduEntity enkidu) {
         EnkiduCombatHelper.tick(enkidu);
         return;
      }
      if (entity instanceof GilgameshEntity gilgamesh) {
         GilgameshCombatHelper.tick(gilgamesh);
         return;
      }
      if (entity instanceof OdaNobunagaEntity oda) {
         OdaNobunagaCombatHelper.tick(oda);
         return;
      }
      if (entity instanceof PaleRiderEntity paleRider) {
         PaleRiderCombatHelper.tick(paleRider);
         return;
      }
      if (entity instanceof UshiwakamaruRiderEntity ushiwakamaru && !ushiwakamaru.isClone()) {
         UshiwakamaruCombatHelper.tick(ushiwakamaru);
         return;
      }
      if (entity instanceof FanaticAssassinEntity fanatic) {
         FanaticAssassinCombatHelper.tick(fanatic, context);
         return;
      }
      if (entity instanceof ShadowHassanEntity shadowHassan) {
         ShadowHassanCombatHelper.tick(shadowHassan, context);
         return;
      }
      if (entity instanceof LiShuwenEntity liShuwen) {
         LiShuwenCombatHelper.tick(liShuwen, context);
         return;
      }
      if (entity instanceof ParacelsusEntity paracelsus) {
         if (sharedTarget != null && (!sharedTarget.isAlive() || !EntityUtils.isValidCombatTarget(entity, sharedTarget))) {
            entity.setTarget(null);
            sharedTarget = null;
         }
         boolean hasLineOfSight = sharedTarget != null && entity.getSensing().hasLineOfSight(sharedTarget);
         ServantExecutionResult addonTick = ServantAddonRegistry.runLifecycleHandlers(
            new ServantLifecycleContext(entity, sharedTarget, context, context.definition(), context.gameTick())
         );
         if (addonTick.handled()) {
            return;
         }
         if (sharedTarget != null && sharedTarget.isAlive()) {
            double distance = entity.distanceTo(sharedTarget);
            int phase = paracelsus.getCombatPhase();
            entity.getLookControl().setLookAt(sharedTarget, 35.0F, 35.0F);
            ServantEngagementService.maintainRangedPosition(
               entity,
               sharedTarget,
               context.gameTick(),
               6.5,
               10.0,
               14.0,
               1.0,
               "ParacelsusRangedPosition"
            );
            if (sharedTarget != null && sharedTarget.isAlive()) {
               if (maybeCastParacelsusElementalMagic(paracelsus, sharedTarget, hasLineOfSight, context)) {
                  return;
               }
               if (maybeSummonParacelsusCannon(paracelsus, sharedTarget, hasLineOfSight, context)) {
                  return;
               }
            }
            if ((paracelsus.getHealth() <= paracelsus.getMaxHealth() * 0.5 || paracelsus.getCurrentMp() <= paracelsus.getMaxMp() * 0.3)
               && context.definition().specialization().hasCombatAction("philosopher_stone")
               && !ParacelsusServantSkills.isNoblePhantasmChanting(paracelsus, context.gameTick())
               && ParacelsusServantSkills.combatActionReady(paracelsus, context.gameTick())
               && context.gameTick() % 80 == 0) {
               ServantExecutionResult result = ServantAddonRegistry.executeCombatAction(
                  new ServantCombatActionContext(
                     entity, sharedTarget, context, context.definition(), "philosopher_stone", distance, hasLineOfSight, context.gameTick()
                  )
               );
               if (result.handled()) {
                  if (result.success()) ParacelsusServantSkills.markCombatAction(paracelsus, context.gameTick(), 20L);
                  return;
               }
            }
            if (phase >= 3
               && paracelsus.getHealth() <= paracelsus.getMaxHealth() / 3.0F
               && !ParacelsusServantSkills.isNoblePhantasmChanting(paracelsus, context.gameTick())
               && paracelsus.getCurrentMp() >= 150.0
               && ParacelsusServantSkills.combatActionReady(paracelsus, context.gameTick())
               && hasLineOfSight
               && EntityUtils.isValidCombatTarget(paracelsus, sharedTarget)
               && distance <= 28.0
               && context.gameTick() - paracelsus.getPersistentData().getLong("ParacelsusLastNpTick") >= 900L) {
               ServantExecutionResult npResult = ServantNoblePhantasmExecutor.activateNp(
                  paracelsus,
                  sharedTarget,
                  java.util.Objects.requireNonNullElseGet(
                     net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDataRegistry.get(context.definition().noblePhantasmId()),
                     () -> new ServantNoblePhantasmDefinition(
                     context.definition().noblePhantasmId(),
                     "",
                     "",
                     ServantNoblePhantasmDefinition.NpType.ARMY,
                     "A+",
                     150,
                     true,
                     3,
                     2.0,
                     30.0,
                     java.util.List.of(),
                     java.util.List.of(),
                     java.util.List.of()
                  )),
                  1
               );
               if (npResult.success()) ParacelsusServantSkills.markCombatAction(paracelsus, context.gameTick(), 20L);
               return;
            }
            if (distance <= 4.0 && entity.isPerformingAction()) {
               return;
            }
            if (distance <= 4.5 && hasLineOfSight && ParacelsusServantSkills.combatActionReady(paracelsus, context.gameTick())
               && !ParacelsusServantSkills.isNoblePhantasmChanting(paracelsus, context.gameTick())
               && context.gameTick() % 34 == 0) {
               entity.triggerAttackSwing();
               if (entity.doHurtTarget(sharedTarget)) ParacelsusServantSkills.markCombatAction(paracelsus, context.gameTick(), 20L);
               return;
            }
            if (context.definition().specialization().hasCombatAction("philosopher_stone")
               && paracelsus.getHealth() <= paracelsus.getMaxHealth() * 0.5
               && paracelsus.getPersistentData().getInt("ParacelsusPhilosopherStoneCount") > 0
               && !ParacelsusServantSkills.isNoblePhantasmChanting(paracelsus, context.gameTick())
               && ParacelsusServantSkills.combatActionReady(paracelsus, context.gameTick())
               && context.gameTick() % 100 == 0) {
               ServantExecutionResult result = ServantAddonRegistry.executeCombatAction(
                  new ServantCombatActionContext(
                     entity, sharedTarget, context, context.definition(), "philosopher_stone", distance, hasLineOfSight, context.gameTick()
                  )
               );
               if (result.success()) ParacelsusServantSkills.markCombatAction(paracelsus, context.gameTick(), 20L);
            }
         }
         return;
      }
      if (sharedTarget != null && ServantCombatSystem.tryRunComboAction(entity, sharedTarget)) {
         return;
      }
      if (sharedTarget != null && !sharedTarget.isDeadOrDying() && !EntityUtils.isImmunePlayerTarget(sharedTarget)) {
         ServantExecutionResult addonTick = ServantAddonRegistry.runLifecycleHandlers(
            new ServantLifecycleContext(entity, sharedTarget, context, context.definition(), context.gameTick())
         );
         if (addonTick.handled()) {
            return;
         }
      }
      if (entity instanceof MedeaEntity medea) {
         MedeaCombatHelper.tick(medea, context);
         return;
      }
      if (entity instanceof MedusaEntity medusa) {
         MedusaCombatHelper.tick(medusa, context);
         return;
      }
      if (entity instanceof CursedArmHassanEntity cursedArm) {
         CursedArmHassanCombatHelper.tick(cursedArm, context);
         return;
      }
      if (entity instanceof ArtoriaPendragonEntity artoria && ArtoriaPendragonCombatHelper.tick(artoria, context)) {
         return;
      }
      if (entity instanceof GawainEntity gawain && GawainCombatHelper.tick(gawain, context)) {
         return;
      }
      LivingEntity target = context.target();
      if (target == null || target.isDeadOrDying()) {
         entity.setTarget(null);
         return;
      }
      if (EntityUtils.isImmunePlayerTarget(target)) {
         entity.setTarget(null);
         return;
      }

      SasakiKojiroCombatHelper.markCombat(entity);
      CuChulainnCombatHelper.markCombat(entity);

      CompoundTag data = entity.getPersistentData();
      double healthRatio = entity.getHealth() / entity.getMaxHealth();
      CombatDisposition combatStyle = entity.getCombatDisposition();
      var behaviorProfile = context.behaviorProfile();
      int tick = (int) context.gameTick();
      ServantSpecialization specialization = context.definition().specialization();
      boolean canBreakForwardBlocks = specialization.hasCombatAction("break_forward_blocks");
      boolean canRoar = specialization.hasCombatAction("roar");
      boolean canLowHealthRoar = specialization.hasCombatAction("low_health_roar");
      boolean canJumpAttack = specialization.hasCombatAction("jump_attack");
      boolean canCharge = specialization.hasCombatAction("charge");
      boolean canSweep = specialization.hasCombatAction("sweep");
      boolean canSlash = specialization.hasCombatAction("slash");
      boolean canTeleportBehind = specialization.hasCombatAction("teleport_behind");
      boolean canStomp = specialization.hasCombatAction("stomp");
      boolean canUppercut = specialization.hasCombatAction("uppercut");
      boolean canHorizontalSwing = specialization.hasCombatAction("horizontal_swing");
      boolean canGroundSlam = specialization.hasCombatAction("slam");
      boolean canAssassinCombo = specialization.hasCombatAction("combo");
      boolean canTsurigameshi = specialization.hasCombatAction("tsurigameshi");
      boolean canRuneCast = specialization.hasCombatAction("rune_cast");
      boolean canGaeBolg = specialization.hasCombatAction("gae_bolg");
      boolean canGaeBolgArmy = specialization.hasCombatAction("gae_bolg_army");
      boolean canRecastStance = specialization.hasCombatAction("recast_stance");
      boolean canLungingThrust = specialization.hasCombatAction("lunging_thrust");
      boolean canDrivingSlash = specialization.hasCombatAction("driving_slash");
      boolean canSweepingAdvance = specialization.hasCombatAction("sweeping_advance");
      boolean canEarthRend = specialization.hasCombatAction("earth_rend");
      boolean canShoulderCheck = specialization.hasCombatAction("shoulder_check");
      boolean canRuneBurst = specialization.hasCombatAction("rune_burst");
      boolean canSpearVault = specialization.hasCombatAction("spear_vault");
      boolean canAfterimageSlash = specialization.hasCombatAction("afterimage_slash");
      boolean canIaijutsuStep = specialization.hasCombatAction("iaijutsu_step");
      double retreatThreshold = Math.max(0.08, behaviorProfile.applyCombatDisposition(combatStyle));
      double aggressionRange = Math.max(16.0, behaviorProfile.aggressionRange());
      if (CuChulainnCombatHelper.isLaguzActive(entity)) {
         aggressionRange *= 2.0;
      }
      if (EmiyaArcherEntity.SERVANT_KEY.equals(entity.getServantId())) {
         aggressionRange *= entity.getPersistentData().getBoolean("ClairvoyanceActive") ? 4.0 : 2.0;
      }
      double attackCommitDistance = Math.max(3.0, behaviorProfile.attackCommitDistance());
      double skillChanceScale = Math.max(0.75, Math.min(1.35, behaviorProfile.skillUsageFrequency() / 0.65));
      boolean hasLineOfSight = hasLineOfSightCached(entity, target, data, tick);
      boolean gaeBolgWindingUp = CuChulainnCombatHelper.isGaeBolgWindingUp(entity);

      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);

      if (entity.distanceToSqr(target) > aggressionRange * aggressionRange * 1.5
         && !ServantTargetingService.canRetain(entity, target, context.gameTick())) {
         entity.setTarget(null);
         entity.getNavigation().stop();
         return;
      }

      // 非狂化型：低血量撤退
      if (handleUndergroundTarget(entity, target, data, tick, canBreakForwardBlocks, canTeleportBehind, hasLineOfSight)) {
         return;
      }

      ServantExecutionResult addonAction = ServantAddonRegistry.runFirstHandledCombatAction(
         specialization.combatActions(),
         actionId -> new ServantCombatActionContext(
            entity,
            target,
            context,
            context.definition(),
            actionId,
            entity.distanceTo(target),
            hasLineOfSight,
            context.gameTick()
         )
      );
      if (addonAction.handled()) {
         return;
      }

      data.remove("RetreatStartTick");
      if (!(entity instanceof ArtoriaPendragonEntity) && combatStyle != CombatDisposition.FRENZIED
         && healthRatio < retreatThreshold && !data.getBoolean("LastStandActive")) {
         data.putBoolean("LastStandActive", true);
         entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200, 0, false, true));
         entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true));
      }

      // 动画中冻结
      if (entity.isRoaring() || entity.isSlamming() || entity.isHardCombatActionActive()) {
         return;
      }

      if (!gaeBolgWindingUp && canRecastStance && healthRatio < 0.2 && CuChulainnCombatHelper.canUseRecastStance(entity)
            && entity.getCurrentMp() >= CU_RECAST_MP_COST) {
         performRecastStance(entity);
         return;
      }

      double distance = entity.distanceTo(target);
      if (ServantCombatDisposition.isRelentlessAdvance(entity) && canBreakForwardBlocks) {
         tryBreakCollisionWall(entity, target, data, tick, entity instanceof HeraclesEntity, hasLineOfSight);
      }

      // ——— 0. 被方块挡住：挥砍砸开前方路径（仅Berserker） ———
      if (canBreakForwardBlocks && (ServantCombatDisposition.isRelentlessAdvance(entity)
         || ServantCombatTempoService.disconnectedTicks(entity, tick) >= ServantCombatTempoService.BREAKOUT_ESCALATION_TICKS)
         && (entity.getNavigation().isInProgress() || distance > 3.5)) {
         int lastBreak = data.getInt("LastBlockBreakTick");
         if (tick - lastBreak >= BLOCK_BREAK_COOLDOWN) {
            boolean stuck = isBlockedForward(entity, target);
            if (stuck) {
               data.putInt("LastBlockBreakTick", tick);
               breakForwardBlocks(entity);
            }
         }
      }

      // ——— 1. 首次吼叫（推开 + 视觉）（Berserker专属） ———
      if (canRoar && !data.getBoolean("HasRoared")) {
         data.putBoolean("HasRoared", true);
         data.putInt("LastRoarTick", tick);
         entity.triggerRoarAnimation();
         performRoar(entity);
         return;
      }

      // ——— 2. 低血量吼叫（<50%）（Berserker专属）———
      if (canLowHealthRoar && healthRatio < 0.5 && !data.getBoolean("LowHealthRoared")) {
         if (tick - data.getInt("LastRoarTick") >= ROAR_COOLDOWN) {
            data.putBoolean("LowHealthRoared", true);
            data.putInt("LastRoarTick", tick);
            entity.triggerRoarAnimation();
            performRoar(entity);
            
         }
      }

      // ——— 3. 跳跃攻击：目标在上方 3-8格（Berserker专属） ———
      double dy = target.getY() - entity.getY();
      double distSqr = entity.distanceToSqr(target);
      if (canJumpAttack && dy > 3.0 && dy < 8.0 && distSqr < 100.0 && entity.onGround()
            && passesSkillChance(entity, 25, skillChanceScale)) {
         entity.triggerJumpAttackAnimation();
         // 垂直 + 水平冲向目标
         entity.getNavigation().stop();
         entity.jumpFromGround();
         Vec3 dir = target.position().subtract(entity.position());
         Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
         if (horizontal.lengthSqr() > 1.0E-4) {
            horizontal = horizontal.normalize().scale(0.6);
            entity.faceVector(horizontal);
         } else {
            horizontal = Vec3.ZERO;
         }
         Vec3 motion = entity.getDeltaMovement();
         entity.setDeltaMovement(motion.x + horizontal.x, Math.max(motion.y + 0.55, 1.25), motion.z + horizontal.z);
         entity.hasImpulse = true;
         // 着陆时触发AOE（模拟着陆砸地）
         performGroundSlam(entity);
         return;
      }

      // ——— 4. 冲刺攻击：距离 6-15格（Berserker专属） ———
      if (canCharge && distance >= 6.0 && distance <= 15.0) {
         int lastCharge = data.getInt("LastChargeTick");
         if (tick - lastCharge >= CHARGE_COOLDOWN && passesSkillChance(entity, 30, skillChanceScale)) {
            data.putInt("LastChargeTick", tick);
            entity.triggerChargeAnimation();
            // 面朝目标快速冲刺
            Vec3 chargeDir = target.position().subtract(entity.position()).normalize().scale(2.05);
            entity.faceVector(chargeDir);
            entity.setDeltaMovement(chargeDir.x, entity.getDeltaMovement().y, chargeDir.z);
            // 冲刺粒子尾迹
            if (entity.level() instanceof ServerLevel sl) {
               sl.sendParticles(ParticleTypes.CLOUD,
                  entity.getX(), entity.getY() + 0.5, entity.getZ(),
                  25, 0.3, 0.3, 0.3, 0.15);
            }
            // 冲刺路径上破坏方块（模拟冲锋）
            BlockPos entityPos = entity.blockPosition();
            int chargeBroken = 0;
            for (int i = 0; i < 8 && chargeBroken < 48; i++) {
               BlockPos center = entityPos.relative(entity.getDirection(), i);
               for (BlockPos breakPos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 2, 1))) {
                  BlockState state = entity.level().getBlockState(breakPos);
                  float hardness = state.getDestroySpeed(entity.level(), breakPos);
                  if (!state.isAir() && hardness >= 0 && hardness < 80
                        && !state.is(Blocks.BEDROCK)) {
                     if (entity.level() instanceof ServerLevel sl && destroyBlockWithCombatFx(sl, breakPos, state, true)) {
                        chargeBroken++;
                     }
                     if (chargeBroken >= 48) {
                        break;
                     }
                  }
               }
            }
            // 冲刺结束后对目标造成伤害
            if (distance <= 3.5) {
               entity.doHurtTarget(target);
               entity.triggerAttackSwing();
               applyDivinityDamage(entity, target, data);
               applyMadEnhancementDamage(entity, target, data);
            }
            
         }
      }

      // ——— 5. 横扫攻击：近距离 + 周围2+敌人 ———
      if (canSweep && distance <= 4.0) {
         int lastSweep = data.getInt("LastSweepTick");
         if (tick - lastSweep >= SWEEP_COOLDOWN && passesSkillChance(entity, 35, skillChanceScale)) {
            AABB sweepBox = entity.getBoundingBox().inflate(3.0);
            List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
               LivingEntity.class, sweepBox,
               e -> e != entity && e.isAlive() && !e.isAlliedTo(entity));
            if (nearby.size() >= 2) {
               data.putInt("LastSweepTick", tick);
               entity.triggerSweepAnimation();
               performSweep(entity);
               return;
            }
         }
      }

      // ——— 6. 斩击：近距离 + 周围1-2敌人 ———
      if (canSlash && distance <= 4.0) {
         int lastSlash = data.getInt("LastSlashTick");
         if (tick - lastSlash >= SLASH_COOLDOWN && passesSkillChance(entity, 40, skillChanceScale)) {
            AABB slashBox = entity.getBoundingBox().inflate(3.0);
            List<LivingEntity> nearSlash = entity.level().getEntitiesOfClass(
               LivingEntity.class, slashBox,
               e -> e != entity && e.isAlive() && !e.isAlliedTo(entity));
            if (!nearSlash.isEmpty() && nearSlash.size() <= 2) {
               data.putInt("LastSlashTick", tick);
               entity.triggerSlashAnimation();
               performSlash(entity, target);
               return;
            }
         }
      }

      // ——— 7. 瞬移到敌人身后：距离 4-12格 ———
      if (canTeleportBehind && distance >= 4.0 && distance <= 12.0) {
         int lastTeleport = data.getInt("LastTeleportTick");
         if (tick - lastTeleport >= TELEPORT_COOLDOWN && (!hasLineOfSight || passesSkillChance(entity, 25, skillChanceScale))) {
            data.putInt("LastTeleportTick", tick);
            entity.triggerTeleportAnimation();
            performTeleportBehind(entity, target);
            return;
         }
      }

      // ——— 8. 跺脚：近距离（Berserker专属） ———
      if (canStomp && distance <= 3.0) {
         int lastStomp = data.getInt("LastStompTick");
         if (tick - lastStomp >= STOMP_COOLDOWN && passesSkillChance(entity, 30, skillChanceScale)) {
            data.putInt("LastStompTick", tick);
            entity.triggerStompAnimation();
            performStomp(entity);
            return;
         }
      }

      // ——— 9. 砸地：近距离 + CD（Berserker专属） ———
      if (canGroundSlam && distance <= SLAM_RADIUS && !entity.isRoaring() && !entity.isSlamming()) {
         int lastSlam = data.getInt("LastSlamTick");
         if (tick - lastSlam >= SLAM_COOLDOWN && passesSkillChance(entity, 30, skillChanceScale)) {
            data.putInt("LastSlamTick", tick);
            entity.triggerGroundSlam();
            performGroundSlam(entity);
            return;
         }
      }

      if (canEarthRend && distance <= 7.5) {
         int lastEarthRend = data.getInt("LastEarthRendTick");
         if (tick - lastEarthRend >= EARTH_REND_COOLDOWN && passesSkillChance(entity, 34, skillChanceScale)) {
            data.putInt("LastEarthRendTick", tick);
            entity.triggerGroundSlam();
            performEarthRend(entity, target);
            return;
         }
      }

      if (canShoulderCheck && distance >= 3.0 && distance <= 9.0 && hasLineOfSight) {
         int lastShoulder = data.getInt("LastShoulderCheckTick");
         if (tick - lastShoulder >= SHOULDER_CHECK_COOLDOWN && passesSkillChance(entity, 38, skillChanceScale)) {
            data.putInt("LastShoulderCheckTick", tick);
            entity.triggerChargeAnimation();
            performShoulderCheck(entity, target);
            return;
         }
      }

      // ——— 燕返（Assassin专属）：目标HP<40%，100固定真伤 + 概率斩杀 ———
      if (canTsurigameshi && distance < 4.0 && !SasakiKojiroCombatHelper.isBladeBroken(entity)) {
         int lastTsurigameshi = data.getInt("LastTsurigameshiTick");
         if (tick - lastTsurigameshi >= 600 && entity.getCurrentMp() >= 30) {
            data.putInt("LastTsurigameshiTick", tick);
            entity.setCurrentMp(entity.getCurrentMp() - 30);
            entity.triggerTsurigameshiAnimation();
            performTsurigameshi(entity, target);
            return;
         }
      }

      // ——— Assassin连击：近距离快速3连击 ———
      if (canAssassinCombo && distance < 3.0) {
         int lastCombo = data.getInt("LastComboTick");
         if (tick - lastCombo >= 20 && passesSkillChance(entity, 50, skillChanceScale * 1.2)) {
            data.putInt("LastComboTick", tick);
            performAssassinCombo(entity, target);
            return;
         }
      }

      // ——— 10. 上勾拳：近距离随机 ———
      if (!gaeBolgWindingUp && canRuneCast && entity.getCurrentMp() >= CU_RUNE_MP_COST && CuChulainnCombatHelper.canCastRune(entity)) {
         if (tryUseCuRune(entity, target, healthRatio, distance, hasLineOfSight)) {
            return;
         }
      }

      if (!gaeBolgWindingUp
         && canGaeBolgArmy
         && entity.getCurrentMp() > 0.0
         && ServantCombatSystem.canUseNoblePhantasm(entity)
         && CuChulainnCombatHelper.canUseArmyGaeBolg(entity)) {
         AABB armyBox = entity.getBoundingBox().inflate(8.0);
         int groupSize = entity.level().getEntitiesOfClass(
            LivingEntity.class, armyBox, e -> e != entity && e.isAlive() && !e.isAlliedTo(entity)
         ).size();
         boolean emergencyArmy = healthRatio <= 0.1 && target != null && target.isAlive();
         boolean desperate = healthRatio <= 0.5;
         boolean favorableWindow = distance >= 5.0 || groupSize >= 2 || desperate;
         int armyChance = groupSize >= 3 ? 95 : 70;
         double armyScale = desperate ? skillChanceScale * 1.25 : skillChanceScale;
         if (emergencyArmy || favorableWindow && passesSkillChance(entity, armyChance, armyScale)) {
            performGaeBolgArmy(entity, target);
            return;
         }
      }

      if (!gaeBolgWindingUp
         && canGaeBolg
         && entity.getCurrentMp() >= 10
         && ServantCombatSystem.canUseNoblePhantasm(entity)
         && CuChulainnCombatHelper.canUseSingleGaeBolg(entity)) {
         if (distance <= 12.0 && (distance <= 3.0 || passesSkillChance(entity, 28, skillChanceScale))) {
            performGaeBolg(entity, target, distance <= 3.0);
            return;
         }
      }

      if (CuChulainnCombatHelper.isCuChulainn(entity) && canLungingThrust && hasLineOfSight && distance >= 3.0 && distance <= 7.5) {
         int lastThrust = data.getInt("CuLastLungingThrustTick");
         if (tick - lastThrust >= CU_LUNGING_THRUST_COOLDOWN && passesSkillChance(entity, 42, skillChanceScale)) {
            data.putInt("CuLastLungingThrustTick", tick);
            performCuLungingThrust(entity, target);
            return;
         }
      }

      if (CuChulainnCombatHelper.isCuChulainn(entity) && canDrivingSlash && hasLineOfSight && distance >= 2.5 && distance <= 6.5) {
         int lastDrive = data.getInt("CuLastDrivingSlashTick");
         if (tick - lastDrive >= CU_DRIVING_SLASH_COOLDOWN && passesSkillChance(entity, 38, skillChanceScale)) {
            data.putInt("CuLastDrivingSlashTick", tick);
            performCuDrivingSlash(entity, target);
            return;
         }
      }

      if (CuChulainnCombatHelper.isCuChulainn(entity) && canSweepingAdvance && hasLineOfSight && distance >= 2.5 && distance <= 5.5) {
         int lastAdvance = data.getInt("CuLastSweepingAdvanceTick");
         if (tick - lastAdvance >= CU_SWEEPING_ADVANCE_COOLDOWN && passesSkillChance(entity, 36, skillChanceScale)) {
            data.putInt("CuLastSweepingAdvanceTick", tick);
            performCuSweepingAdvance(entity, target);
            return;
         }
      }

      if (canRuneBurst && hasLineOfSight && distance <= 8.0 && entity.getCurrentMp() >= 8.0) {
         int lastRuneBurst = data.getInt("LastRuneBurstTick");
         if (tick - lastRuneBurst >= RUNE_BURST_COOLDOWN && passesSkillChance(entity, 32, skillChanceScale)) {
            data.putInt("LastRuneBurstTick", tick);
            entity.setCurrentMp(entity.getCurrentMp() - 8.0);
            entity.triggerRuneCastAnimation();
            performRuneBurst(entity, target);
            return;
         }
      }

      if (canSpearVault && hasLineOfSight && distance >= 2.5 && distance <= 8.5) {
         int lastVault = data.getInt("LastSpearVaultTick");
         if (tick - lastVault >= SPEAR_VAULT_COOLDOWN && passesSkillChance(entity, 36, skillChanceScale)) {
            data.putInt("LastSpearVaultTick", tick);
            entity.triggerSweepAnimation();
            performSpearVault(entity, target);
            return;
         }
      }

      if (canAfterimageSlash && distance <= 5.0) {
         int lastAfterimage = data.getInt("LastAfterimageSlashTick");
         if (tick - lastAfterimage >= AFTERIMAGE_SLASH_COOLDOWN && passesSkillChance(entity, 40, skillChanceScale)) {
            data.putInt("LastAfterimageSlashTick", tick);
            entity.triggerSlashAnimation();
            performAfterimageSlash(entity, target);
            return;
         }
      }

      if (canIaijutsuStep && distance >= 2.0 && distance <= 8.0 && hasLineOfSight) {
         int lastIai = data.getInt("LastIaijutsuStepTick");
         if (tick - lastIai >= IAIJUTSU_STEP_COOLDOWN && passesSkillChance(entity, 34, skillChanceScale)) {
            data.putInt("LastIaijutsuStepTick", tick);
            entity.triggerHorizontalSwingAnimation();
            performIaijutsuStep(entity, target);
            return;
         }
      }

      if (canUppercut && distance <= 3.5) {
         int lastUppercut = data.getInt("LastUppercutTick");
         if (tick - lastUppercut >= UPPERCUT_COOLDOWN && passesSkillChance(entity, 35, skillChanceScale)) {
            data.putInt("LastUppercutTick", tick);
            entity.triggerUppercutAnimation();
            performUppercut(entity, target);
            return;
         }
      }

      // ——— 11. 横挥：近距离随机 ———
      if (canHorizontalSwing && distance <= 3.5) {
         int lastHSwing = data.getInt("LastHSwingTick");
         if (tick - lastHSwing >= H_SWING_COOLDOWN && passesSkillChance(entity, 35, skillChanceScale)) {
            data.putInt("LastHSwingTick", tick);
            entity.triggerHorizontalSwingAnimation();
            performHorizontalSwing(entity, target);
            return;
         }
      }

      // ——— 7. 接近 + 近战 ———
      
      if (!hasLineOfSight) {
         moveToTargetThrottled(entity, target, distance > 8.0 ? 1.3 : 1.1, tick, 0.85);
      } else if (distance > Math.max(3.5, attackCommitDistance)) {
         double speed = distance > 10.0 ? 1.35 : (distance > 6.0 ? 1.2 : 1.0);
         if (combatStyle == CombatDisposition.CAUTIOUS && distance < attackCommitDistance + 1.5) {
            speed = 0.9;
         }
         moveToTargetThrottled(entity, target, speed, tick, 0.85);
      } else {
         int basicAttackCooldown = canAssassinCombo ? ASSASSIN_BASIC_ATTACK_COOLDOWN : BASIC_ATTACK_COOLDOWN;
         int lastBasicAttack = data.getInt("LastBasicAttackTick");
         if (distance > 2.4 || tick - lastBasicAttack < basicAttackCooldown) {
            performCombatFootwork(entity, target, combatStyle, distance);
            return;
         }
         data.putInt("LastBasicAttackTick", tick);
         entity.doHurtTarget(target);
         entity.triggerAttackSwing();
         applyDivinityDamage(entity, target, data);
         applyMadEnhancementDamage(entity, target, data);
         // 近战攻击粒子
         if (entity.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
               target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
               1, 0.0, 0.0, 0.0, 0.0);
            sl.sendParticles(ParticleTypes.CRIT,
               target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
               5, 0.3, 0.3, 0.3, 0.15);
         }
      }

      // ——— 8. 狂暴：低血量永久 ATK+5 + 移速+0.05 ———
      if (healthRatio < 0.3 && combatStyle == CombatDisposition.FRENZIED) {
         AttributeInstance atk = entity.getAttribute(Attributes.ATTACK_DAMAGE);
         if (atk != null && atk.getModifier(VALOR_RES) == null) {
            atk.addPermanentModifier(new AttributeModifier(
               VALOR_RES, 5.0, AttributeModifier.Operation.ADD_VALUE));
         }
         AttributeInstance spd = entity.getAttribute(Attributes.MOVEMENT_SPEED);
         if (spd != null && spd.getModifier(FRENZY_SPEED_RES) == null) {
            spd.addPermanentModifier(new AttributeModifier(
               FRENZY_SPEED_RES, 0.05, AttributeModifier.Operation.ADD_VALUE));
         }
      }
   }

   /**
    * 神性 A：每次近战额外施加 25 点魔法伤害
    */
   private boolean passesSkillChance(ServantEntity entity, int basePercent, double scale) {
      int effectivePercent = (int)Math.round(basePercent * scale);
      effectivePercent = Math.max(5, Math.min(95, effectivePercent));
      return entity.getRandom().nextInt(100) < effectivePercent;
   }

   private int getNearbyEnemyCountCached(ServantEntity entity, CompoundTag data, int tick) {
      int lastScan = data.getInt("CombatSurroundedScanTick");
      if (lastScan > 0 && tick - lastScan < SURROUNDED_SCAN_INTERVAL) {
         return data.getInt("CombatSurroundedEnemyCount");
      }
      AABB dangerZone = entity.getBoundingBox().inflate(3.0);
      int enemyCount = entity.level().getEntitiesOfClass(
         LivingEntity.class, dangerZone,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity)
            && !ServantMasterTargeting.isContractMaster(entity, e)
      ).size();
      data.putInt("CombatSurroundedScanTick", tick);
      data.putInt("CombatSurroundedEnemyCount", enemyCount);
      return enemyCount;
   }

   private boolean hasLineOfSightCached(ServantEntity entity, LivingEntity target, CompoundTag data, int tick) {
      boolean sameTarget = data.hasUUID("CombatLosTarget") && data.getUUID("CombatLosTarget").equals(target.getUUID());
      int lastCheck = data.getInt("CombatLosTick");
      if (sameTarget && lastCheck > 0 && tick - lastCheck < COMBAT_LOS_INTERVAL) {
         return data.getBoolean("CombatHasLos");
      }
      boolean hasLineOfSight = entity.getSensing().hasLineOfSight(target);
      data.putUUID("CombatLosTarget", target.getUUID());
      data.putInt("CombatLosTick", tick);
      data.putBoolean("CombatHasLos", hasLineOfSight);
      return hasLineOfSight;
   }

   private boolean handleUndergroundTarget(
      ServantEntity entity, LivingEntity target, CompoundTag data, int tick, boolean canBreakForwardBlocks, boolean canTeleportBehind, boolean hasLineOfSight
   ) {
      double verticalDrop = entity.getY() - target.getY();
      boolean undergroundTarget = !hasLineOfSight && verticalDrop >= 3.5;
      if (!undergroundTarget) {
         data.remove("UndergroundTargetStartTick");
         return false;
      }

      int startTick = data.getInt("UndergroundTargetStartTick");
      if (startTick == 0) {
         data.putInt("UndergroundTargetStartTick", tick);
         startTick = tick;
      }

      if (canBreakForwardBlocks) {
         int lastBreak = data.getInt("LastBlockBreakTick");
         if (tick - lastBreak >= BLOCK_BREAK_COOLDOWN) {
            data.putInt("LastBlockBreakTick", tick);
            breakTowardUndergroundTarget(entity, target);
         }
         moveToTargetThrottled(entity, target, 1.15, tick, 0.8);
         return true;
      }

      if (canTeleportBehind) {
         int lastTeleport = data.getInt("LastTeleportTick");
         if (tick - lastTeleport >= TELEPORT_COOLDOWN && teleportNearUndergroundTarget(entity, target)) {
            data.putInt("LastTeleportTick", tick);
            entity.triggerTeleportAnimation();
            return true;
         }
      }

      if (tick - startTick >= UNDERGROUND_TARGET_TIMEOUT) {
         data.remove("UndergroundTargetStartTick");
         // Keep the target and let the tempo supervisor choose a new approach
         // or breakout route instead of turning an occlusion into disengagement.
         ServantCombatTempoService.enforceLegacy(entity, target, tick);
         return true;
      }

      moveToTargetThrottled(entity, target, 1.0, tick, 0.8);
      return true;
   }

   private void moveToTargetThrottled(ServantEntity entity, LivingEntity target, double speed, int tick, double minTargetMoveSqr) {
      ServantNavigationHelper.moveToTargetThrottled(
         entity,
         target,
         speed,
         tick,
         COMBAT_PATH_RECALC_INTERVAL,
         minTargetMoveSqr,
         "CombatPath"
      );
   }

   private void breakTowardUndergroundTarget(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel serverLevel)) {
         return;
      }
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      for (double step = 0.0; step <= 1.0; step += 0.08) {
         Vec3 sample = start.lerp(end, step);
         BlockPos center = BlockPos.containing(sample);
         for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState state = serverLevel.getBlockState(pos);
            float hardness = state.getDestroySpeed(serverLevel, pos);
            if (!state.isAir() && hardness >= 0.0F && hardness < 50.0F && !state.is(Blocks.BEDROCK)) {
               serverLevel.removeBlock(pos, false);
            }
         }
      }
      serverLevel.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.4, entity.getZ(), 12, 0.5, 0.2, 0.5, 0.05);
   }

   private boolean teleportNearUndergroundTarget(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel serverLevel)) {
         return false;
      }
      Vec3 look = target.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      } else {
         horizontal = horizontal.normalize();
      }
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize();
      Vec3[] candidates = new Vec3[]{
         target.position().subtract(horizontal.scale(1.5)),
         target.position().add(side.scale(1.2)),
         target.position().subtract(side.scale(1.2))
      };

      for (Vec3 candidate : candidates) {
         BlockPos feet = findSafeTeleportFeet(serverLevel, BlockPos.containing(candidate.x, target.getY(), candidate.z));
         if (feet != null) {
            entity.teleportTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.faceToward(target.position());
            entity.fallDistance = 0.0F;
            return true;
         }
      }
      return false;
   }

   private BlockPos findSafeTeleportFeet(ServerLevel level, BlockPos anchor) {
      for (int dy = -2; dy <= 3; dy++) {
         BlockPos feet = anchor.offset(0, dy, 0);
         BlockPos below = feet.below();
         if (level.getBlockState(below).isSolidRender(level, below)
            && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
            return feet;
         }
      }
      return null;
   }

   private void performRetreatFootwork(ServantEntity entity, LivingEntity target, CombatDisposition combatStyle) {
      Vec3 away = entity.position().subtract(target.position());
      if (away.lengthSqr() < 1.0E-4) {
         away = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
      }

      Vec3 retreatPos = entity.position().add(away.normalize().scale(combatStyle == CombatDisposition.CAUTIOUS ? 6.0 : 4.0));
      ServantNavigationHelper.moveToPositionThrottled(
         entity,
         retreatPos,
         combatStyle == CombatDisposition.CAUTIOUS ? 1.25 : 1.05,
         entity.level().getGameTime(),
         ServantNavigationHelper.SHORT_REPATH_INTERVAL,
         1.0,
         "CombatRetreatPath"
      );
      if (entity.getRandom().nextInt(4) == 0) {
         float side = entity.getRandom().nextBoolean() ? 0.5F : -0.5F;
         entity.getMoveControl().strafe(-0.6F, side);
      }
      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
   }

   private void performCombatFootwork(ServantEntity entity, LivingEntity target, CombatDisposition combatStyle, double distance) {
      ServantNavigationHelper.stopIfMoving(entity);
      // Heracles and Gawain are committed melee pursuers.  At the edge of
      // melee range they should keep their line and face the opponent instead
      // of entering the generic random strafe loop (which reads as spinning).
      if ((entity instanceof HeraclesEntity || entity instanceof GawainEntity) && distance > 2.8) {
         entity.getLookControl().setLookAt(target, 45.0F, 45.0F);
         moveToTargetThrottled(entity, target, 1.15, (int)entity.level().getGameTime(), 0.15);
         return;
      }
      if (ServantCombatTempoService.inMeleePressure(entity, entity.level().getGameTime())) {
         entity.getMoveControl().strafe(distance > 2.5 ? 0.42F : 0.16F,
            entity.getRandom().nextBoolean() ? 0.14F : -0.14F);
         entity.getLookControl().setLookAt(target, 40.0F, 35.0F);
         return;
      }
      float side = entity.getRandom().nextBoolean() ? 0.45F : -0.45F;
      float forward = switch (combatStyle) {
         case CAUTIOUS -> distance < 2.4 ? -0.35F : -0.10F;
         case FRENZIED -> 0.35F;
         default -> distance > 2.7 ? 0.10F : 0.0F;
      };
      float sideScale = combatStyle == CombatDisposition.FRENZIED ? 0.20F : side;
      entity.getMoveControl().strafe(forward, sideScale);
      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
   }

   private void applyDivinityDamage(ServantEntity entity, LivingEntity target, CompoundTag data) {
      if (data.getBoolean("DivinityActive")) {
         float divDmg = data.getFloat("DivinityFlatDamage");
         if (divDmg > 0) {
            target.hurt(entity.damageSources().magic(), divDmg);
         }
      }
   }

   /**
    * 狂化 B：额外造成基础攻击力 15% 的伤害
    */
   private void applyMadEnhancementDamage(ServantEntity entity, LivingEntity target, CompoundTag data) {
      if (data.getBoolean("MadEnhancementActive")) {
         float bonusRatio = data.getFloat("MadEnhancementDamageBonus");
         if (bonusRatio > 0) {
            AttributeInstance atk = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) {
               float extraDmg = (float) (atk.getValue() * bonusRatio);
               target.hurt(entity.damageSources().mobAttack(entity), extraDmg);
            }
         }
      }
   }

   /**
    * 斩击：对前方单体造成高倍率伤害
    */
   private void performSlash(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double slashDamage = baseAtk * 1.8;
      DamageSource src = entity.damageSources().mobAttack(entity);

      // 朝目标方向进行180度扇形检测
      Vec3 look = entity.getLookAngle();
      Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
      AABB slashBox = entity.getBoundingBox().inflate(4.5).move(look.scale(2.0));
      List<LivingEntity> near = sl.getEntitiesOfClass(
         LivingEntity.class, slashBox,
         e -> e != entity && e.isAlive());
      for (LivingEntity le : near) {
         le.hurt(src, (float) slashDamage);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double kb = Math.max(0.35, 1.8 * (1.0 - dist / 4.5));
            pushWithImpactCrater(sl, entity, le, new Vec3(dx / dist, 0.0, dz / dist), kb, 0.38, 1.8, 10);
         }
      }

      // 斩击轨迹粒子
      int slashBroken = 0;
      double terrainScale = terrainBreakScale(entity);
      int slashLimit = scaledBreakLimit(36, terrainScale);
      for (double r = 0.5; r <= 5.0 * terrainScale && slashBroken < slashLimit; r += 0.5) {
         for (double theta = -Math.PI / 2; theta <= Math.PI / 2; theta += Math.PI / 6) {
            double x = look.x * Math.cos(theta) - look.z * Math.sin(theta);
            double z = look.x * Math.sin(theta) + look.z * Math.cos(theta);
            Vec3 offset = new Vec3(x, 0, z).normalize().scale(r);
            BlockPos pos = BlockPos.containing(center.add(offset));
            if (blockNoise(sl, pos) > 0.82 && r > 1.5) {
               continue;
            }
            BlockState state = sl.getBlockState(pos);
            if (!state.isAir() && (state.getDestroySpeed(sl, pos) >= 0
                  || state.getFluidState().isSource())) {
               if (destroyBlockWithCombatFx(sl, pos, state, true)) {
                  slashBroken++;
               }
               if (slashBroken >= slashLimit) {
                  break;
               }
            }
         }
      }

      sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
         entity.getX() + look.x * 2.0,
         entity.getY() + entity.getBbHeight() * 0.6,
         entity.getZ() + look.z * 2.0,
         5, 0.0, 0.0, 0.0, 0.0);
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         20, 0.45, 0.4, 0.45, 0.22);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         entity.getX() + look.x * 2.2, entity.getY() + 0.25, entity.getZ() + look.z * 2.2,
         32, 1.2, 0.25, 1.2, 0.05);
   }

   /**
    * 瞬移到敌人身后：短距离传送 + 立即攻击
    */
   private void performTeleportBehind(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;

      // 计算目标背后位置
      Vec3 targetLook = target.getLookAngle();
      Vec3 behindPos = target.position().add(targetLook.scale(-2.0));

      // 确保位置安全（不卡在方块里）
      BlockPos safePos = BlockPos.containing(behindPos);
      if (!entity.level().getBlockState(safePos).canBeReplaced()) {
         safePos = safePos.above();
      }
      if (!entity.level().getBlockState(safePos).canBeReplaced()) {
         safePos = entity.blockPosition(); // 回到原位
      }

      // 瞬移
      entity.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);

      // 到达粒子
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         entity.getX(), entity.getY() + 0.5, entity.getZ(),
         15, 0.3, 0.5, 0.3, 0.05);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         entity.getX(), entity.getY() + 1.0, entity.getZ(),
         8, 0.2, 0.3, 0.2, 0.03);

      // 立即攻击
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double tpDamage = baseAtk * 1.5;
      DamageSource src = entity.damageSources().mobAttack(entity);
      target.hurt(src, (float) tpDamage);
      Vec3 away = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      pushWithImpactCrater(sl, entity, target, away, 1.45, 0.36, 1.8, 10);

      // 攻击粒子
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         8, 0.3, 0.3, 0.3, 0.15);
      sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
         SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 0.7F);
   }

   /**
    * 跺脚：小范围AOE伤害 + 击飞 + 地面碎裂
    */
   private void performStomp(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      double stompDamage = entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2;
      DamageSource src = entity.damageSources().mobAttack(entity);
      double stompRadius = 4.0;
      AABB box = entity.getBoundingBox().inflate(stompRadius);
      List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != entity && e.isAlive());
      for (LivingEntity le : nearby) {
         le.hurt(src, (float)stompDamage);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0.0) {
            double kb = Math.max(0.45, 1.8 * (1.0 - dist / stompRadius));
            pushWithImpactCrater(sl, entity, le, new Vec3(dx / dist, 0.0, dz / dist), kb, 0.72, 2.2, 16);
         }
      }

      BlockPos center = entity.blockPosition();
      double terrainScale = terrainBreakScale(entity);
      int stompRadiusBlocks = Math.max(1, (int)Math.ceil(3.0 * terrainScale));
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-stompRadiusBlocks, -1, -stompRadiusBlocks), center.offset(stompRadiusBlocks, 1, stompRadiusBlocks))) {
         if (!isInIrregularBreakShape(sl, pos, center, 3.45 * terrainScale, 0.8, 0.75, 0.68)) {
            continue;
         }
         BlockState state = sl.getBlockState(pos);
         float hardness = state.getDestroySpeed(sl, pos);
         if (!state.isAir() && hardness >= 0.0F && hardness < 30.0F && !state.is(Blocks.BEDROCK)) {
            destroyBlockWithCombatFx(sl, pos, state, true);
         }
      }
      for (int i = 0; i < 16; i++) {
         double angle = (Math.PI * 2.0) * i / 16.0;
         BlockPos ringPos = center.offset((int)Math.round(Math.cos(angle) * 4.0 * terrainScale), 0, (int)Math.round(Math.sin(angle) * 4.0 * terrainScale));
         if (blockNoise(sl, ringPos) > 0.72) {
            ringPos = ringPos.offset((int)Math.round(Math.cos(angle) * blockNoise(sl, ringPos.above())), 0, (int)Math.round(Math.sin(angle) * blockNoise(sl, ringPos.below())));
         }
         BlockState state = sl.getBlockState(ringPos);
         float hardness = state.getDestroySpeed(sl, ringPos);
         if (!state.isAir() && hardness >= 0.0F && hardness < 30.0F && !state.is(Blocks.BEDROCK)) {
            destroyBlockWithCombatFx(sl, ringPos, state, true);
         }
      }

      sl.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.1, entity.getZ(), 48, 1.8, 0.35, 1.8, 0.22);
      sl.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 0.3, entity.getZ(), 7, 1.4, 0.4, 1.4, 0.0);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX(), entity.getY() + 0.25, entity.getZ(), 56, 2.0, 0.35, 2.0, 0.055);
   }
   /**
    * Earth rend.
    */
   private void performEarthRend(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      Vec3 dir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      horizontal = horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
      entity.faceVector(horizontal);
      double baseAtk = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
      DamageSource src = entity.damageSources().mobAttack(entity);
      Vec3 origin = entity.position();
      int broken = 0;
      double terrainScale = terrainBreakScale(entity);
      int maxBroken = scaledBreakLimit(96, terrainScale);
      for (double step = 1.0; step <= 11.0 * terrainScale && broken < maxBroken; step += 1.0) {
         Vec3 centerVec = origin.add(horizontal.scale(step));
         BlockPos center = BlockPos.containing(centerVec);
         AABB hitBox = new AABB(center).inflate(1.45 + step * 0.16, 1.4, 1.45 + step * 0.16);
         for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && e.isAlive() && !e.isAlliedTo(entity))) {
            victim.hurt(src, (float)(baseAtk * 0.75));
            pushWithImpactCrater(sl, entity, victim, horizontal, 1.95, 0.82, 2.4, 18);
         }
         int pathRadius = Math.max(1, (int)Math.ceil(2.0 * terrainScale));
         for (BlockPos pos : BlockPos.betweenClosed(center.offset(-pathRadius, -1, -pathRadius), center.offset(pathRadius, 1, pathRadius))) {
            if (!isInIrregularBreakShape(sl, pos, center, 2.35 * terrainScale, 0.75, 0.8, 0.72)) {
               continue;
            }
            BlockState state = sl.getBlockState(pos);
            float hardness = state.getDestroySpeed(sl, pos);
            if (!state.isAir() && hardness >= 0.0F && hardness < 75.0F && !state.is(Blocks.BEDROCK)) {
               if (destroyBlockWithCombatFx(sl, pos, state, true)) {
                  broken++;
               }
               if (broken >= maxBroken) {
                  break;
               }
            }
         }
         sl.sendParticles(ParticleTypes.CLOUD, centerVec.x, entity.getY() + 0.2, centerVec.z, 16, 0.8, 0.2, 0.8, 0.06);
         sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, centerVec.x, entity.getY() + 0.35, centerVec.z, 12, 0.75, 0.25, 0.75, 0.035);
      }
      sl.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.55F);
   }

   private void performShoulderCheck(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      Vec3 dir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) return;
      horizontal = horizontal.normalize();
      entity.faceVector(horizontal);
      entity.setDeltaMovement(horizontal.x * 2.35, Math.max(entity.getDeltaMovement().y, 0.16), horizontal.z * 2.35);
      entity.hasImpulse = true;
      double baseAtk = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
      AABB hitBox = entity.getBoundingBox().expandTowards(horizontal.scale(5.5)).inflate(1.2, 0.8, 1.2);
      for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class, hitBox, e -> e != entity && e.isAlive() && !e.isAlliedTo(entity))) {
         victim.hurt(entity.damageSources().mobAttack(entity), (float)(baseAtk * 0.95));
         pushWithImpactCrater(sl, entity, victim, horizontal, 2.45, 0.48, 2.0, 14);
      }
      sl.sendParticles(ParticleTypes.EXPLOSION, entity.getX() + horizontal.x * 2.0, entity.getY() + 0.7, entity.getZ() + horizontal.z * 2.0, 6, 0.55, 0.25, 0.55, 0.0);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX() + horizontal.x * 2.0, entity.getY() + 0.35, entity.getZ() + horizontal.z * 2.0, 34, 0.9, 0.25, 0.9, 0.06);
   }

   private void performRuneBurst(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.faceToward(target.position());
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      double baseAtk = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
      AABB box = target.getBoundingBox().inflate(2.0);
      for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != entity && e.isAlive() && !e.isAlliedTo(entity))) {
         victim.hurt(entity.damageSources().magic(), (float)(baseAtk * 0.65 + 4.0));
         Vec3 push = victim.position().subtract(center);
         Vec3 horizontal = new Vec3(push.x, 0.0, push.z);
         if (horizontal.lengthSqr() > 1.0E-4) {
            horizontal = horizontal.normalize();
            pushWithImpactCrater(sl, entity, victim, horizontal, 1.15, 0.32, 1.6, 8);
         }
      }
      sl.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 56, 1.8, 0.8, 1.8, 0.05);
      sl.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 28, 1.2, 0.55, 1.2, 0.02);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y - 0.25, center.z, 22, 1.4, 0.18, 1.4, 0.035);
      sl.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.0F, 0.8F);
   }

   private void performSpearVault(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      Vec3 dir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) return;
      horizontal = horizontal.normalize();
      entity.faceVector(horizontal);
      entity.jumpFromGround();
      entity.setDeltaMovement(horizontal.x * 1.35, Math.max(entity.getDeltaMovement().y, 0.78), horizontal.z * 1.35);
      entity.hasImpulse = true;
      target.hurt(entity.damageSources().mobAttack(entity), (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85));
      pushWithImpactCrater(sl, entity, target, horizontal, 1.85, 0.72, 2.1, 14);
      sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 4, 0.0, 0.0, 0.0, 0.0);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.getX(), target.getY() + 0.2, target.getZ(), 24, 0.7, 0.2, 0.7, 0.05);
      sl.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.35F);
   }

   private void performAfterimageSlash(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.faceToward(target.position());
      double baseAtk = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
      for (int i = 0; i < 3; i++) {
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().mobAttack(entity), (float)(baseAtk * 0.42));
         double side = (i - 1) * 0.45;
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX() + side, target.getY() + target.getBbHeight() * (0.35 + i * 0.15), target.getZ() - side, 1, 0.0, 0.0, 0.0, 0.0);
      }
      target.push((target.getX() - entity.getX()) * 0.16, 0.25, (target.getZ() - entity.getZ()) * 0.16);
      target.hurtMarked = true;
      sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 1.25F);
   }

   private void performIaijutsuStep(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      Vec3 dir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) return;
      horizontal = horizontal.normalize();
      Vec3 destination = target.position().subtract(horizontal.scale(1.2));
      entity.teleportTo(destination.x, target.getY(), destination.z);
      entity.faceVector(horizontal);
      entity.setDeltaMovement(Vec3.ZERO);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1));
      target.push(horizontal.x * 0.8, 0.18, horizontal.z * 0.8);
      target.hurtMarked = true;
      sl.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.4, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.03);
      sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
      sl.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 1.1F);
   }

   /**
    * 判断前方是否被可破坏方块阻挡
    */
   private boolean isBlockedForward(ServantEntity entity, LivingEntity target) {
      Vec3 toTarget = target.position().subtract(entity.position());
      Vec3 dir = toTarget.normalize();
      BlockPos eyePos = entity.blockPosition().above();
      // 检查前方1-2格、视线高度和上方一格
      for (int i = 1; i <= 2; i++) {
         BlockPos check = eyePos.offset((int) Math.round(dir.x * i), 0, (int) Math.round(dir.z * i));
         BlockState state = entity.level().getBlockState(check);
         BlockState stateAbove = entity.level().getBlockState(check.above());
         float hard = state.getDestroySpeed(entity.level(), check);
         // 当前格或上方格被实心方块挡住，且可破坏
         if ((!state.isAir() && hard >= 0 && hard < 50 && !state.is(Blocks.BEDROCK))
               || (!stateAbove.isAir() && stateAbove.getDestroySpeed(entity.level(), check.above()) >= 0
               && stateAbove.getDestroySpeed(entity.level(), check.above()) < 50
               && !stateAbove.is(Blocks.BEDROCK))) {
            return true;
         }
      }
      return false;
   }

   private boolean tryBreakCollisionWall(ServantEntity entity, LivingEntity target, CompoundTag data, int tick, boolean heavy, boolean hasLineOfSight) {
      if (!(entity.level() instanceof ServerLevel sl) || target == null || !target.isAlive()) {
         return false;
      }
      if (tick - data.getInt("LastCombatWallBreakTick") < (heavy ? 6 : 10)) {
         return false;
      }
      boolean pressingWall = entity.horizontalCollision || entity.getNavigation().isInProgress() && !hasLineOfSight;
      if (!pressingWall) {
         return false;
      }
      Vec3 dir = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (dir.lengthSqr() < 1.0E-4) {
         dir = entity.getLookAngle().multiply(1.0, 0.0, 1.0);
      }
      if (dir.lengthSqr() < 1.0E-4) {
         return false;
      }
      dir = dir.normalize();
      Vec3 center = entity.position().add(dir.scale(heavy ? 2.2 : 1.6))
         .add(0.0, entity.getBbHeight() * 0.5, 0.0);
      boolean queued = TerrainImpactService.impact(sl, entity, center,
         TerrainImpactProfile.of(heavy ? TerrainImpactProfile.Tier.HEAVY : TerrainImpactProfile.Tier.MEDIUM),
         TerrainImpactService.Shape.AIR_SPHERE);
      if (!queued) return false;
      data.putInt("LastCombatWallBreakTick", tick);
      Vec3 fx = entity.position().add(dir.scale(1.4)).add(0.0, entity.getBbHeight() * 0.42, 0.0);
      sl.sendParticles(ParticleTypes.CLOUD, fx.x, fx.y, fx.z, heavy ? 14 : 8, 0.25, 0.2, 0.25, 0.055);
      sl.playSound(null, entity.blockPosition(), heavy ? SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR : SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, heavy ? 0.9F : 0.75F, heavy ? 0.65F : 0.85F);
      return true;
   }

   /**
    * 燕返（Tsurigameshi）：必杀一击特效增强版
    */
   private void performTsurigameshi(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      if (SasakiKojiroCombatHelper.isSasakiKojiro(entity)) {
         float hitChance = SasakiKojiroCombatHelper.getTsurigameshiHitChance(entity);
         if (hitChance <= 0.0F) {
            return;
         }

         if (entity.getRandom().nextFloat() > hitChance) {
            // 未命中：少量挥空粒子 + 弱攻击音效
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
               target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
               4, 0.25, 0.25, 0.25, 0.02);
            sl.sendParticles(ParticleTypes.CLOUD,
               target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
               8, 0.2, 0.2, 0.2, 0.04);
            sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
               SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.HOSTILE, 1.0F, 1.15F);
            return;
         }

         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().mobAttack(entity), 300.0F);
         target.invulnerableTime = 0;

         // === 燕返命中：多重斩击轨迹 + 灵子散逸 ===
         Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
         Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
         Vec3 slashDir = targetPos.subtract(entityPos).normalize();

         // 1. 弧形斩击轨迹粒子（从下往上三道弧线）
         for (int arc = 0; arc < 3; arc++) {
            double arcOffset = (arc - 1) * 0.4;
            for (double t = 0.0; t <= 1.0; t += 0.1) {
               Vec3 pos = entityPos.lerp(targetPos, t);
               double wave = Math.sin(t * Math.PI) * 0.3 * (arc + 1);
               Vec3 perp = new Vec3(-slashDir.z, 0, slashDir.x);
               pos = pos.add(perp.scale(wave + arcOffset));
               sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                  pos.x, pos.y, pos.z,
                  2, 0.0, 0.0, 0.0, 0.0);
            }
         }

         // 2. 目标处爆发：灵魂火焰 + 末影碎裂
         sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            target.getX(), target.getY() + target.getBbHeight() * 0.3, target.getZ(),
            25, 0.5, 0.6, 0.5, 0.08);
         sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            15, 0.4, 0.5, 0.4, 0.05);
         sl.sendParticles(ParticleTypes.SOUL,
            target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
            12, 0.3, 0.4, 0.3, 0.04);

         // 3. 斩击闪光（中心扩散）
         sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            8, 0.3, 0.3, 0.3, 0.15);

         // 4. 实体侧烟雾残留（瞬移残影感）
         sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
            entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
            20, 0.3, 0.5, 0.3, 0.04);

         // 5. 击飞效果
         double dx = target.getX() - entity.getX();
         double dz = target.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            target.push(dx / dist * 0.8, 0.4, dz / dist * 0.8);
            target.hurtMarked = true;
         }

         // 6. 音效叠加
         sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.8F, 0.5F);
         sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.ENDER_EYE_DEATH, SoundSource.HOSTILE, 1.2F, 0.7F);
         sl.playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.5F, 0.6F);
         return;
      }

      // 100点固定真伤
      target.hurt(entity.damageSources().generic(), 100.0F);

      double hpRatio = target.getHealth() / target.getMaxHealth();
      net.minecraft.util.RandomSource random = entity.getRandom();

      // 即死判定（HP<10%，30%概率）
      if (hpRatio < 0.10 && random.nextInt(100) < 30) {
         target.setHealth(0);
         target.die(entity.damageSources().mobAttack(entity));
      }
      // 半血斩杀判定（HP<30%，50%概率，额外造成剩余HP一半的伤害）
      else if (hpRatio < 0.30 && random.nextInt(100) < 50) {
         float bonusDmg = target.getHealth() / 2.0F;
         target.hurt(entity.damageSources().generic(), bonusDmg);
      }

      // 通用燕返特效
      Vec3 entityPos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
      Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
      Vec3 slashDir = targetPos.subtract(entityPos).normalize();

      // 弧形斩击轨迹
      for (double t = 0.0; t <= 1.0; t += 0.15) {
         Vec3 pos = entityPos.lerp(targetPos, t);
         double wave = Math.sin(t * Math.PI) * 0.2;
         Vec3 perp = new Vec3(-slashDir.z, 0, slashDir.x);
         pos = pos.add(perp.scale(wave));
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            pos.x, pos.y, pos.z,
            1, 0.0, 0.0, 0.0, 0.0);
      }

      sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
         target.getX(), target.getY() + target.getBbHeight() * 0.3, target.getZ(),
         15, 0.4, 0.5, 0.4, 0.06);
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
         20, 0.4, 0.4, 0.4, 0.2);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
         15, 0.3, 0.3, 0.3, 0.08);

      sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
         SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.5F, 0.6F);
      sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
         SoundEvents.ENDER_EYE_DEATH, SoundSource.HOSTILE, 1.0F, 0.8F);
   }

   /**
    * Assassin连击：快速3连击
    */
   private void performAssassinCombo(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;

      entity.triggerAttackSwing();

      for (int i = 0; i < 3; i++) {
         final int idx = i;
         net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(idx * 4, () -> {
            if (target.isAlive() && entity.isAlive()) {
               entity.doHurtTarget(target);
            }
         });
      }

      // 连击粒子
      sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         3, 0.3, 0.3, 0.3, 0.1);
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
         8, 0.3, 0.3, 0.3, 0.15);
   }

   /**
    * 砸开前方1-3格的可破坏方块 + 挥砍粒子音效
    */
   private void breakForwardBlocks(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      LivingEntity target = entity.getTarget();
      if (target == null) return;
      Vec3 dir = target.position().subtract(entity.position()).normalize();
      entity.faceVector(dir);
      BlockPos center = entity.blockPosition().above();
      int broken = 0;
      // 前方3格、中心±2格宽度；限制单次破坏数量，避免战斗 tick 里过量改方块。
      for (int d = 1; d <= 3 && broken < 12; d++) {
         for (int w = -2; w <= 2; w++) {
            Vec3 perp = new Vec3(-dir.z, 0, dir.x).scale(w);
            BlockPos pos = center.offset(
               (int) Math.round(dir.x * d + perp.x),
               0,
               (int) Math.round(dir.z * d + perp.z));
            BlockState state = sl.getBlockState(pos);
            float hard = state.getDestroySpeed(sl, pos);
            if (!state.isAir() && hard >= 0 && hard < 70 && !state.is(Blocks.BEDROCK)) {
               sl.removeBlock(pos, false);
               broken++;
               if (broken >= 12) {
                  break;
               }
            }
         }
      }
      if (broken > 0) {
         // 挥砍粒子
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            entity.getX() + dir.x * 2.0,
            entity.getY() + entity.getBbHeight() * 0.5,
            entity.getZ() + dir.z * 2.0,
            2, 0.0, 0.0, 0.0, 0.0);
         sl.sendParticles(ParticleTypes.CLOUD,
            entity.getX() + dir.x * 1.5,
            entity.getY() + entity.getBbHeight() * 0.5,
            entity.getZ() + dir.z * 1.5,
            8, 0.2, 0.2, 0.2, 0.08);
         sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.8F);
      }
   }

   /**
    * 上勾拳：向右上举起，往左下方挥去，单体高伤害
    */
   private void performUppercut(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double dmg = baseAtk * 1.4;
      DamageSource src = entity.damageSources().mobAttack(entity);
      target.hurt(src, (float) dmg);
      Vec3 uppercutDir = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      pushWithImpactCrater(sl, entity, target, uppercutDir, 1.35, 1.05, 2.0, 14);
      sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
         target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
         2, 0.0, 0.0, 0.0, 0.0);
      sl.sendParticles(ParticleTypes.CRIT,
         target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(),
         8, 0.3, 0.4, 0.3, 0.15);
   }

   /**
    * 横挥：右臂横在胸前向右挥，近距离扇形伤害
    */
   private void performHorizontalSwing(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double dmg = baseAtk * 1.2;
      DamageSource src = entity.damageSources().mobAttack(entity);
      Vec3 look = entity.getLookAngle();
      AABB swingBox = entity.getBoundingBox().inflate(2.5).move(look.scale(1.0));
      List<LivingEntity> near = sl.getEntitiesOfClass(
         LivingEntity.class, swingBox,
         e -> e != entity && e.isAlive());
      for (LivingEntity le : near) {
         le.hurt(src, (float) dmg);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double kb = Math.max(0.3, 1.25 * (1.0 - dist / 3.6));
            pushWithImpactCrater(sl, entity, le, new Vec3(dx / dist, 0.0, dz / dist), kb, 0.32, 1.5, 8);
         }
      }
      sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
         entity.getX() + look.x * 2.0,
         entity.getY() + entity.getBbHeight() * 0.55,
         entity.getZ() + look.z * 2.0,
         4, 0.0, 0.0, 0.0, 0.0);
      sl.sendParticles(ParticleTypes.CLOUD,
         entity.getX() + look.x * 1.5,
         entity.getY() + entity.getBbHeight() * 0.5,
         entity.getZ() + look.z * 1.5,
         18, 0.6, 0.25, 0.6, 0.06);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         entity.getX() + look.x * 1.8,
         entity.getY() + 0.25,
         entity.getZ() + look.z * 1.8,
         12, 0.55, 0.2, 0.55, 0.035);
   }

   private boolean tryUseCuRune(ServantEntity entity, LivingEntity target, double healthRatio, double distance, boolean hasLineOfSight) {
      boolean algizActive = CuChulainnCombatHelper.hasRune(entity, CuChulainnCombatHelper.RuneType.ALGIZ);
      boolean ansuzActive = CuChulainnCombatHelper.hasRune(entity, CuChulainnCombatHelper.RuneType.ANSUZ);
      boolean tiwazActive = CuChulainnCombatHelper.hasRune(entity, CuChulainnCombatHelper.RuneType.TIWAZ);
      boolean laguzActive = CuChulainnCombatHelper.hasRune(entity, CuChulainnCombatHelper.RuneType.LAGUZ);
      boolean berkanaActive = CuChulainnCombatHelper.hasRune(entity, CuChulainnCombatHelper.RuneType.BERKANA);

      if (healthRatio < 0.5 && entity.getHealth() < entity.getMaxHealth() * 0.9F && !berkanaActive) {
         performBerkanaRune(entity);
         return true;
      }

      if (healthRatio < 0.65 && entity.getPersistentData().getFloat(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG) <= 0.0F
            && !algizActive) {
         performAlgizRune(entity);
         return true;
      }

      if (distance > 7.0 && hasLineOfSight && target != null && !ansuzActive) {
         performAnsuzRune(entity, target);
         return true;
      }

      if (distance <= 4.5 && !tiwazActive) {
         performTiwazRune(entity);
         return true;
      }

      if ((!hasLineOfSight || (!laguzActive && distance > 5.5))
            && !laguzActive) {
         performLaguzRune(entity);
         return true;
      }

      return false;
   }

   private void performRecastStance(ServantEntity entity) {
      entity.setCurrentMp(entity.getCurrentMp() - CU_RECAST_MP_COST);
      entity.triggerRuneCastAnimation();
      entity.getPersistentData().putBoolean("BattleContinuationRecoveryActive", true);
      entity.getPersistentData().remove("BattleContinuationLastHealTick");
      java.util.List<net.minecraft.world.effect.MobEffectInstance> active = java.util.List.copyOf(entity.getActiveEffects());
      for (net.minecraft.world.effect.MobEffectInstance effect : active) {
         if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            entity.removeEffect(effect.getEffect());
         }
      }
      CuChulainnCombatHelper.markRecastUsed(entity);
      if (entity.level() instanceof ServerLevel sl) {
         spawnRuneParticles(sl, entity, ModParticles.TIWAZ_RUNE.get(), 18, 0.45, 0.02);
         sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
            18, 0.4, 0.6, 0.4, 0.05);
      }
   }

   private void performLaguzRune(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - CU_RUNE_MP_COST);
      entity.triggerRuneCastAnimation();
      CuChulainnCombatHelper.markRuneCast(entity);
      CuChulainnCombatHelper.setRune(entity, CuChulainnCombatHelper.RuneType.LAGUZ, 200);
      spawnRuneParticles(sl, entity, ModParticles.LAGUZ_RUNE.get(), 16, 0.65, 0.01);
      sl.sendParticles(ParticleTypes.ENCHANT,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
         20, 0.6, 0.8, 0.6, 0.04);
   }

   private void performBerkanaRune(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - CU_RUNE_MP_COST);
      entity.triggerRuneCastAnimation();
      CuChulainnCombatHelper.markRuneCast(entity);
      CuChulainnCombatHelper.setRune(entity, CuChulainnCombatHelper.RuneType.BERKANA, 180);
      java.util.List<net.minecraft.world.effect.MobEffectInstance> active = java.util.List.copyOf(entity.getActiveEffects());
      for (net.minecraft.world.effect.MobEffectInstance effect : active) {
         if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
            entity.removeEffect(effect.getEffect());
         }
      }
      spawnRuneParticles(sl, entity, ModParticles.BERKANA_RUNE.get(), 16, 0.5, 0.01);
      sl.sendParticles(ParticleTypes.HEART,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.7, entity.getZ(),
         10, 0.4, 0.55, 0.4, 0.02);
      sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
         12, 0.45, 0.7, 0.45, 0.03);
   }

   private void performTiwazRune(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - CU_RUNE_MP_COST);
      entity.triggerRuneCastAnimation();
      CuChulainnCombatHelper.markRuneCast(entity);
      CuChulainnCombatHelper.setRune(entity, CuChulainnCombatHelper.RuneType.TIWAZ, 300);
      spawnRuneParticles(sl, entity, ModParticles.TIWAZ_RUNE.get(), 18, 0.45, 0.02);
      sl.sendParticles(ParticleTypes.END_ROD,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
         14, 0.45, 0.7, 0.45, 0.03);
   }

   private void performAlgizRune(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - CU_RUNE_MP_COST);
      entity.triggerRuneCastAnimation();
      CuChulainnCombatHelper.markRuneCast(entity);
      CuChulainnCombatHelper.setRune(entity, CuChulainnCombatHelper.RuneType.ALGIZ, 160);
      spawnRuneParticles(sl, entity, ModParticles.ALGIZ_RUNE.get(), 16, 0.55, 0.01);
      sl.sendParticles(ParticleTypes.WAX_ON,
         entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(),
         18, 0.5, 0.8, 0.5, 0.03);
   }

   private void performAnsuzRune(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - CU_RUNE_MP_COST);
      entity.triggerRuneCastAnimation();
      CuChulainnCombatHelper.markRuneCast(entity);
      CuChulainnCombatHelper.setRune(entity, CuChulainnCombatHelper.RuneType.ANSUZ, 40);
      spawnRuneParticles(sl, entity, ModParticles.ANSUZ_RUNE.get(), 18, 0.45, 0.02);
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.65, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      spawnAnsuzTrace(sl, start, end);
      AABB groupBox = new AABB(end, end).inflate(4.0);
      int nearbyEnemies = sl.getEntitiesOfClass(
         LivingEntity.class,
         groupBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      ).size();
      int form = nearbyEnemies >= 3 ? 1 + entity.getRandom().nextInt(2) : entity.getRandom().nextInt(3);
      switch (form) {
         case 1 -> releaseAnsuzFlamePillars(entity, target, end);
         case 2 -> releaseAnsuzFireline(entity, target, start, end);
         default -> releaseAnsuzFireburst(entity, target, end);
      }
   }

   private void releaseAnsuzFireburst(ServantEntity entity, LivingEntity target, Vec3 fallbackEnd) {
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(8, () -> {
         if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
            return;
         }
         Vec3 impact = target.isAlive()
            ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
            : fallbackEnd;
         AABB fireBox = new AABB(impact, impact).inflate(2.5);
         for (LivingEntity living : serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            fireBox,
            e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
         )) {
            living.hurt(entity.damageSources().magic(), 80.0F);
            living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 100));
         }
         serverLevel.sendParticles(ParticleTypes.FLAME, impact.x, impact.y, impact.z, 24, 0.8, 0.5, 0.8, 0.05);
         serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, impact.x, impact.y, impact.z, 16, 0.7, 0.35, 0.7, 0.03);
         serverLevel.playSound(null, impact.x, impact.y, impact.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.1F, 0.8F);
      });
   }

   private void releaseAnsuzFlamePillars(ServantEntity entity, LivingEntity target, Vec3 fallbackEnd) {
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(8, () -> {
         if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
            return;
         }

         Vec3 impact = target.isAlive()
            ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
            : fallbackEnd;
         java.util.List<Vec3> pillars = java.util.List.of(
            impact,
            impact.add(2.2, 0.0, 0.0),
            impact.add(-2.2, 0.0, 0.0),
            impact.add(0.0, 0.0, 2.2)
         );
         java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
         for (Vec3 pillar : pillars) {
            serverLevel.sendParticles(ParticleTypes.FLAME, pillar.x, pillar.y + 1.2, pillar.z, 20, 0.35, 1.2, 0.35, 0.02);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pillar.x, pillar.y + 1.1, pillar.z, 10, 0.3, 1.0, 0.3, 0.02);
            AABB pillarBox = new AABB(pillar, pillar).inflate(1.4, 2.2, 1.4);
            for (LivingEntity living : serverLevel.getEntitiesOfClass(
               LivingEntity.class,
               pillarBox,
               e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
            )) {
               if (hit.add(living)) {
                  living.hurt(entity.damageSources().magic(), 70.0F);
                  living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 120));
               }
            }
         }
         serverLevel.playSound(null, impact.x, impact.y, impact.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.15F, 0.85F);
      });
   }

   private void releaseAnsuzFireline(ServantEntity entity, LivingEntity target, Vec3 start, Vec3 fallbackEnd) {
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(6, () -> {
         if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
            return;
         }

         Vec3 end = target.isAlive()
            ? target.position().add(0.0, target.getBbHeight() * 0.5, 0.0)
            : fallbackEnd;
         java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
         for (double t = 0.15; t <= 1.0; t += 0.17) {
            Vec3 pos = start.lerp(end, t);
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 6, 0.2, 0.25, 0.2, 0.01);
            serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 8, 0.25, 0.25, 0.25, 0.02);
            AABB segmentBox = new AABB(pos, pos).inflate(1.35);
            for (LivingEntity living : serverLevel.getEntitiesOfClass(
               LivingEntity.class,
               segmentBox,
               e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
            )) {
               if (hit.add(living)) {
                  living.hurt(entity.damageSources().magic(), 60.0F);
                  living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 80));
               }
            }
         }
         serverLevel.playSound(null, end.x, end.y, end.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
      });
   }

   private void spawnAnsuzTrace(ServerLevel level, Vec3 start, Vec3 end) {
      for (double t = 0.0; t <= 1.0; t += 0.1) {
         Vec3 pos = start.lerp(end, t);
         level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.0);
      }
   }

   private void performGaeBolg(ServantEntity entity, LivingEntity target, boolean meleeMode) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      entity.setCurrentMp(entity.getCurrentMp() - 10.0);
      CuChulainnCombatHelper.markSingleGaeBolg(entity);
      CuChulainnCombatHelper.startGaeBolgWindup(entity, CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS);
      ServantCombatSystem.broadcastNoblePhantasmWindup(entity, target, CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS, true);
      entity.triggerGaeBolgThrowAnimation(CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS);
      Vec3 fallbackAim = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS, () -> {
         releaseSingleGaeBolg(entity, target, meleeMode, fallbackAim);
      });
   }

   private void performGaeBolgArmy(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      double availableMp = Math.max(0.0, entity.getCurrentMp());
      double maxMp = Math.max(1.0, entity.getMaxMp());
      float damageScale = (float)Math.min(1.0, availableMp / maxMp);
      float armyDamage = 500.0F;
      entity.setCurrentMp(0.0);
      CuChulainnCombatHelper.markArmyGaeBolg(entity);
      CuChulainnCombatHelper.startGaeBolgWindup(entity, CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS);
      ServantCombatSystem.broadcastNoblePhantasmWindup(entity, target, CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS, true);
      entity.triggerGaeBolgThrowAnimation(CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS);
      entity.jumpFromGround();
      Vec3 launch = entity.getDeltaMovement();
      entity.setDeltaMovement(launch.x, Math.max(launch.y, 0.78), launch.z);
      entity.hasImpulse = true;
      Vec3 fallbackAim = target.position().add(0.0, target.getBbHeight() * 0.3, 0.0);
      net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.queueServerWork(CuChulainnCombatHelper.GAE_BOLG_WINDUP_TICKS, () -> {
         releaseArmyGaeBolg(entity, target, fallbackAim, armyDamage);
      });
   }

   private void releaseSingleGaeBolg(ServantEntity entity, LivingEntity target, boolean preferMelee, Vec3 fallbackAim) {
      CuChulainnCombatHelper.clearGaeBolgWindup(entity);
      if (!(entity.level() instanceof ServerLevel sl) || !entity.isAlive()) {
         return;
      }

      LivingEntity resolvedTarget = target != null && target.isAlive() ? target : null;
      if (resolvedTarget == null) {
         return;
      }

      if (preferMelee && entity.distanceTo(resolvedTarget) <= 3.5) {
         if (ArtoriaPendragonCombatHelper.tryNegateCertainHitOrDeath(resolvedTarget, "gae_bolg")) {
            sl.sendParticles(ParticleTypes.END_ROD,
               resolvedTarget.getX(), resolvedTarget.getY() + resolvedTarget.getBbHeight() * 0.6, resolvedTarget.getZ(),
               18, 0.3, 0.3, 0.3, 0.03);
            return;
         }
         boolean deathThorn = resolvedTarget.isAlive()
            && !(resolvedTarget instanceof EmiyaArcherEntity)
            && !(resolvedTarget instanceof EnkiduEntity)
            && entity.getRandom().nextFloat() < CuChulainnCombatHelper.getDeathThornChance(resolvedTarget);
         if (!tryConsumeGodHandLife(entity, resolvedTarget, 250.0F, deathThorn)) {
            applyFixedNoArmorDamage(entity, resolvedTarget, 250.0F);
            if (resolvedTarget.isAlive() && deathThorn) {
               applyDeathThorn(entity, resolvedTarget);
            }
         }
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            resolvedTarget.getX(), resolvedTarget.getY() + resolvedTarget.getBbHeight() * 0.5, resolvedTarget.getZ(),
            4, 0.0, 0.0, 0.0, 0.0);
         sl.sendParticles(ParticleTypes.CRIT,
            resolvedTarget.getX(), resolvedTarget.getY() + resolvedTarget.getBbHeight() * 0.55, resolvedTarget.getZ(),
            16, 0.25, 0.25, 0.25, 0.12);
         return;
      }

      GaeBulgProjectileEntity projectile = new GaeBulgProjectileEntity(sl, entity);
      projectile.setMode(GaeBulgProjectileEntity.Mode.SINGLE);
      projectile.setTrackedTarget(resolvedTarget);
      projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ());
      Vec3 aim = resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.45, 0.0);
      if (fallbackAim != null && !resolvedTarget.isAlive()) {
         aim = fallbackAim;
      }
      Vec3 toTarget = aim.subtract(projectile.position()).normalize();
      projectile.shoot(toTarget.x, toTarget.y + 0.06, toTarget.z, 2.4F, 0.0F);
      sl.addFreshEntity(projectile);
   }

   private void releaseArmyGaeBolg(ServantEntity entity, LivingEntity target, Vec3 fallbackAim, float armyDamage) {
      CuChulainnCombatHelper.clearGaeBolgWindup(entity);
      if (!(entity.level() instanceof ServerLevel sl) || !entity.isAlive()) {
         return;
      }

      LivingEntity resolvedTarget = target != null && target.isAlive() ? target : null;
      if (resolvedTarget == null && fallbackAim == null) {
         return;
      }

      GaeBulgArmyProjectileEntity projectile = new GaeBulgArmyProjectileEntity(sl, entity);
      projectile.setArmyDamage(armyDamage);
      projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ());
      Vec3 aim = resolvedTarget != null
         ? resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.3, 0.0)
         : fallbackAim;
      Vec3 toTarget = aim.subtract(projectile.position()).normalize();
      projectile.shoot(toTarget.x, toTarget.y + 0.14, toTarget.z, 2.0F, 0.0F);
      sl.addFreshEntity(projectile);
   }

   private void performCuLungingThrust(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      Vec3 dir = horizontalDashDirection(entity, target);
      if (dir == null) return;

      entity.triggerUppercutAnimation();
      launchDash(entity, dir, 1.4);
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      float damage = (float)(baseAtk * 1.45);
      AABB hitBox = entity.getBoundingBox().expandTowards(dir.scale(4.8)).inflate(0.9, 0.8, 0.9);
      for (LivingEntity living : sl.getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         living.hurt(entity.damageSources().mobAttack(entity), damage);
         living.push(dir.x * 1.15, 0.35, dir.z * 1.15);
         living.hurtMarked = true;
      }
      spawnDashTrail(sl, entity, dir, 4.8, ParticleTypes.CRIT);
   }

   private void performCuDrivingSlash(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      Vec3 dir = horizontalDashDirection(entity, target);
      if (dir == null) return;

      entity.triggerSlashAnimation();
      launchDash(entity, dir, 1.25);
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      float damage = (float)(baseAtk * 1.55);
      AABB hitBox = entity.getBoundingBox().expandTowards(dir.scale(4.4)).inflate(1.1, 0.9, 1.1);
      for (LivingEntity living : sl.getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         living.hurt(entity.damageSources().mobAttack(entity), damage);
         living.push(dir.x * 0.95, 0.25, dir.z * 0.95);
         living.hurtMarked = true;
      }
      spawnDashTrail(sl, entity, dir, 4.4, ParticleTypes.SWEEP_ATTACK);
   }

   private void performCuSweepingAdvance(ServantEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      Vec3 dir = horizontalDashDirection(entity, target);
      if (dir == null) return;

      entity.triggerHorizontalSwingAnimation();
      launchDash(entity, dir, 1.15);
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      float damage = (float)(baseAtk * 1.25);
      AABB hitBox = entity.getBoundingBox().expandTowards(dir.scale(3.8)).inflate(1.8, 1.0, 1.8);
      for (LivingEntity living : sl.getEntitiesOfClass(
         LivingEntity.class,
         hitBox,
         e -> e != entity && e.isAlive() && !e.isAlliedTo(entity) && !EntityUtils.isImmunePlayerTarget(e)
      )) {
         living.hurt(entity.damageSources().mobAttack(entity), damage);
         living.push(dir.x * 0.8, 0.22, dir.z * 0.8);
         living.hurtMarked = true;
      }
      spawnDashTrail(sl, entity, dir, 3.8, ParticleTypes.CLOUD);
   }

   private void applyFixedNoArmorDamage(ServantEntity attacker, LivingEntity target, float damage) {
      float before = target.getHealth();
      target.invulnerableTime = 0;
      target.hurt(attacker.damageSources().mobAttack(attacker), damage);
      target.invulnerableTime = 0;
      float desired = Math.max(0.0F, before - damage);
      if (target.getHealth() > desired && target.getHealth() <= before) {
         target.setHealth(desired);
      }
   }

   private boolean tryConsumeGodHandLife(ServantEntity attacker, LivingEntity target, float incomingDamage, boolean deathThorn) {
      CompoundTag targetData = target.getPersistentData();
      if (targetData.getBoolean("CausalSevered") || !targetData.getBoolean("GodHandActive")) {
         return false;
      }

      int livesLeft = targetData.getInt("GodHandLives");
      if (livesLeft <= 0) {
         return false;
      }

      boolean lethalByDamage = target.getHealth() <= incomingDamage;
      if (!lethalByDamage && !deathThorn) {
         return false;
      }

      target.setHealth(target.getMaxHealth());
      targetData.putInt("GodHandLives", livesLeft - 1);
      if (attacker.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            target.getX(), target.getY() + 1.0, target.getZ(),
            30, 0.6, 0.6, 0.6, 0.15);
         sl.sendParticles(ParticleTypes.POOF,
            target.getX(), target.getY() + 0.5, target.getZ(),
            20, 0.5, 0.5, 0.5, 0.1);
         sl.playSound(null, target.blockPosition(),
            SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 0.8F);
      }
      return true;
   }

   private void applyDeathThorn(ServantEntity attacker, LivingEntity target) {
      float lethalDamage = Math.max(target.getMaxHealth() * 2.0F, 500.0F);
      target.invulnerableTime = 0;
      target.hurt(attacker.damageSources().mobAttack(attacker), lethalDamage);
      target.invulnerableTime = 0;
      if (target.isAlive()) {
         target.setHealth(0.0F);
         target.die(attacker.damageSources().genericKill());
      }
   }

   private Vec3 horizontalDashDirection(ServantEntity entity, LivingEntity target) {
      Vec3 dir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return null;
      }
      return horizontal.normalize();
   }

   private void launchDash(ServantEntity entity, Vec3 dir, double speed) {
      entity.setDeltaMovement(dir.x * speed, Math.max(entity.getDeltaMovement().y, 0.12), dir.z * speed);
      entity.hasImpulse = true;
      entity.getNavigation().moveTo(entity.getX() + dir.x * 4.5, entity.getY(), entity.getZ() + dir.z * 4.5, 1.45);
   }

   private void spawnDashTrail(ServerLevel level, ServantEntity entity, Vec3 dir, double length, net.minecraft.core.particles.ParticleOptions particle) {
      for (double t = 0.5; t <= length; t += 0.8) {
         Vec3 pos = entity.position().add(dir.scale(t)).add(0.0, entity.getBbHeight() * 0.45, 0.0);
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
      }
   }

   private void spawnRuneParticles(ServerLevel level, ServantEntity entity, net.minecraft.core.particles.SimpleParticleType particle, int count, double spread, double speed) {
      level.sendParticles(
         particle,
         entity.getX(),
         entity.getY() + entity.getBbHeight() * 0.72,
         entity.getZ(),
         1,
         0.0,
         0.0,
         0.0,
         0.0
      );
   }

   private void performRoar(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel)) return;
      AABB box = entity.getBoundingBox().inflate(6.0);
      List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
         LivingEntity.class, box, e -> e != entity && e.isAlive());
      for (LivingEntity le : nearby) {
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double strength = 0.8 * (1.0 - dist / 6.0);
            le.push(dx / dist * strength, 0.3, dz / dist * strength);
            le.hurtMarked = true;
         }
      }
   }

   private void performGroundSlam(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      AttributeInstance atkAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      double baseAtk = atkAttr != null ? atkAttr.getValue() : 5.0;
      double slamDamage = baseAtk * SLAM_DAMAGE_MULTIPLIER;
      DamageSource src = entity.damageSources().mobAttack(entity);
      boolean heavySlam = entity.getDefinition() != null && entity.getDefinition().classType() == ServantClassType.BERSERKER;
      double terrainScale = terrainBreakScale(entity);
      double slamRadius = (heavySlam ? SLAM_RADIUS + 3.0 : SLAM_RADIUS + 1.25) * terrainScale;
      AABB box = entity.getBoundingBox().inflate(slamRadius);
      List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
         LivingEntity.class, box, e -> e != entity && e.isAlive());
      for (LivingEntity le : nearby) {
         le.hurt(src, (float) slamDamage);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0) {
            double kb = Math.max(0.55, (heavySlam ? 3.2 : 2.1) * (1.0 - dist / slamRadius));
            pushWithImpactCrater(sl, entity, le, new Vec3(dx / dist, 0.0, dz / dist), kb, heavySlam ? 0.95 : 0.68, heavySlam ? 2.8 : 2.2, heavySlam ? 24 : 16);
         }
      }
      sl.sendParticles(ParticleTypes.EXPLOSION,
         entity.getX(), entity.getY() + 0.5, entity.getZ(),
         heavySlam ? 10 : 7, 2.6, 0.65, 2.6, 0.0);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
         entity.getX(), entity.getY() + 0.25, entity.getZ(),
         heavySlam ? 90 : 58, slamRadius * 0.35, 0.45, slamRadius * 0.35, 0.06);
      // triggerGroundSlam queues the crater through the shared terrain service.
   }

   /**
    * 横扫攻击：扇形范围伤害 + 地形破坏
    */
   /**
    * 横扫攻击：扇形范围伤害 + 地形破坏
    */
   private void performSweep(ServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel sl)) return;
      Vec3 look = entity.getLookAngle();
      Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
      double sweepDamage = entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2;
      DamageSource src = entity.damageSources().mobAttack(entity);

      int sweepBroken = 0;
      double terrainScale = terrainBreakScale(entity);
      int sweepLimit = scaledBreakLimit(40, terrainScale);
      for (double r = 0.5; r <= 5.0 * terrainScale && sweepBroken < sweepLimit; r += 0.5) {
         for (double theta = -Math.PI / 2; theta <= Math.PI / 2; theta += Math.PI / 8) {
            double x = look.x * Math.cos(theta) - look.z * Math.sin(theta);
            double z = look.x * Math.sin(theta) + look.z * Math.cos(theta);
            Vec3 offset = new Vec3(x, 0, z).normalize().scale(r);
            BlockPos pos = BlockPos.containing(center.add(offset));
            if (blockNoise(sl, pos) > 0.80 && r > 1.5) {
               continue;
            }
            BlockState state = sl.getBlockState(pos);
            if (!state.isAir() && (state.getDestroySpeed(sl, pos) >= 0.0F || state.getFluidState().isSource())) {
               if (destroyBlockWithCombatFx(sl, pos, state, true)) {
                  sweepBroken++;
               }
               if (sweepBroken >= sweepLimit) {
                  break;
               }
            }
         }
      }

      AABB sweepBox = entity.getBoundingBox().inflate(4.5).move(look.scale(2.0));
      List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, sweepBox, e -> e != entity && e.isAlive());
      for (LivingEntity le : nearby) {
         le.hurt(src, (float)sweepDamage);
         double dx = le.getX() - entity.getX();
         double dz = le.getZ() - entity.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist > 0.0) {
            double kb = Math.max(0.35, 1.55 * (1.0 - dist / 4.5));
            pushWithImpactCrater(sl, entity, le, new Vec3(dx / dist, 0.0, dz / dist), kb, 0.45, 1.8, 12);
         }
      }

      sl.sendParticles(ParticleTypes.SWEEP_ATTACK, entity.getX() + look.x * 1.5, entity.getY() + entity.getBbHeight() * 0.5, entity.getZ() + look.z * 1.5, 6, 0.0, 0.0, 0.0, 0.0);
      sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, entity.getX() + look.x * 2.2, entity.getY() + 0.25, entity.getZ() + look.z * 2.2, 38, 1.35, 0.28, 1.35, 0.055);
   }
}
