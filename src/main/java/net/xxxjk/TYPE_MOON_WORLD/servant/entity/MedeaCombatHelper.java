package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantFlightCombatService;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class MedeaCombatHelper {
   private static final String TAG_LAST_BOLT_TICK = "MedeaLastBoltTick";
   private static final String TAG_LAST_LIGHTNING_TICK = "MedeaLastLightningTick";
   private static final String TAG_LAST_BEAM_TICK = "MedeaLastBeamTick";
   private static final String TAG_LAST_BARRIER_TICK = "MedeaLastBarrierTick";
   private static final String TAG_LAST_TELEPORT_TICK = "MedeaLastTeleportTick";
   private static final String TAG_LAST_FLEECE_TICK = "MedeaLastFleeceTick";
   private static final String TAG_LAST_RULE_BREAKER_TICK = "MedeaLastRuleBreakerTick";
   private static final String TAG_LAST_COMBAT_SUMMON_TICK = "MedeaLastCombatSummonTick";
   public static final String TAG_LAST_COMBAT_ACTIVITY_TICK = "MedeaLastCombatActivityTick";
   private static final String TAG_LAST_BLINK_VOLLEY_TICK = "MedeaLastBlinkVolleyTick";
   private static final String TAG_LAST_STRAFE_BARRAGE_TICK = "MedeaLastStrafeBarrageTick";
   private static final String TAG_LAST_BIND_TICK = "MedeaLastBindTick";
   private static final String TAG_BLOCKED_LINE_START_TICK = "MedeaBlockedLineStartTick";
   private static final String TAG_LAST_SUPER_BOLT_TICK = "MedeaLastSuperBoltTick";
   private static final String TAG_LAST_MINI_BEAM_TICK = "MedeaLastMiniBeamTick";
   private static final String TAG_LAST_FLAME_BURST_TICK = "MedeaLastFlameBurstTick";
   private static final String TAG_LAST_FROST_SHACKLE_TICK = "MedeaLastFrostShackleTick";
   private static final String TAG_LAST_REPULSION_NOVA_TICK = "MedeaLastRepulsionNovaTick";
   private static final String TAG_LAST_WORKSHOP_ESCAPE_TICK = "MedeaLastWorkshopEscapeTick";
   private static final String TAG_LAST_AERIAL_ESCAPE_TICK = "MedeaLastAerialEscapeTick";
   private static final String TAG_LAST_ORBIT_HEX_TICK = "MedeaLastOrbitHexTick";
   private static final String TAG_LAST_CROSSFIRE_TICK = "MedeaLastCrossfireTick";
   private static final String TAG_LAST_STARFALL_TICK = "MedeaLastStarfallTick";
   private static final String TAG_LAST_THUNDERSTORM_TICK = "MedeaLastThunderstormTick";
   private static final String TAG_LAST_MASS_DRAGONFANG_TICK = "MedeaLastMassDragonfangTick";
   private static final String TAG_UNDERGROUND_TARGET_TICK = "MedeaUndergroundTargetTick";
   private static final String TAG_LAST_SPELL_TYPE = "MedeaLastSpellType";
   private static final String TAG_SPELL_REPEAT_COUNT = "MedeaSpellRepeatCount";
   private static final String TAG_SPELL_WINDOW_TICK = "MedeaSpellWindowTick";
   private static final String TAG_SPELL_WINDOW_COUNT = "MedeaSpellWindowCount";
   private static final String TAG_SPELL_WINDOW_FIRST_TYPE = "MedeaSpellWindowFirstType";
   private static final double COST_BOLT = 0.0;
   private static final double COST_LIGHTNING = 30.0;
   private static final double COST_BEAM = 40.0;
   private static final double COST_BARRIER = 8.0;
   private static final double COST_TELEPORT = 8.0;
   private static final double COST_BLINK_VOLLEY = 35.0;
   private static final double COST_STRAFE_BARRAGE = 35.0;
   private static final double COST_HECATE_BIND = 35.0;
   private static final double COST_SUPER_BOLT = 45.0;
   private static final double COST_THUNDERSTORM = 50.0;
   private static final double COST_RULE_BREAKER = 10.0;
   private static final double COST_MINI_BEAM = 5.0;
   private static final double COST_FLAME_BURST = 6.0;
   private static final double COST_FROST_SHACKLE = 7.0;
   private static final double COST_REPULSION_NOVA = 5.0;
   private static final double COST_WORKSHOP_ESCAPE = 8.0;
   private static final double COST_RETURN_TO_WORKSHOP = 10.0;
   private static final double COST_ORBIT_HEX = 7.0;
   private static final double COST_CROSSFIRE = 8.0;
   private static final double COST_STARFALL = 9.0;
   private static final DustParticleOptions CIRCLE_PRIMARY = new DustParticleOptions(new Vector3f(0.45F, 0.65F, 1.0F), 1.2F);
   private static final DustParticleOptions CIRCLE_ACCENT = new DustParticleOptions(new Vector3f(0.72F, 0.28F, 1.0F), 1.0F);

   private MedeaCombatHelper() {
   }

   @FunctionalInterface
   private interface SpellAction {
      boolean cast();
   }

   private enum SpellType {
      RETURN_TO_WORKSHOP("return_to_workshop"),
      WORKSHOP_ESCAPE("workshop_escape"),
      AERIAL_ESCAPE("aerial_escape"),
      MINI_BEAM("mini_beam"),
      REPULSION_NOVA("repulsion_nova"),
      FROST_SHACKLE("frost_shackle"),
      BARRIER("barrier"),
      BLINK_VOLLEY("blink_volley"),
      TELEPORT("teleport"),
      RULE_BREAKER("rule_breaker"),
      LIGHTNING("lightning"),
      BOLT("bolt"),
      FLAME_BURST("flame_burst"),
      HECATE_BIND("hecate_bind"),
      STRAFE_BARRAGE("strafe_barrage"),
      ORBIT_HEX("orbit_hex"),
      CROSSFIRE("crossfire"),
      STARFALL("starfall"),
      THUNDERSTORM("thunderstorm"),
      BEAM("beam"),
      SUPER_BOLT("super_bolt");

      private final String id;

      SpellType(String id) {
         this.id = id;
      }

      public String id() {
         return this.id;
      }
   }

   public static void tick(MedeaEntity entity, ServantAiContext context) {
      LivingEntity target = context.target();
      if (target != null && ServantMasterTargeting.isContractMaster(entity, target)) {
         target = null;
         entity.setTarget(null);
      }
      if (target == null || target.isDeadOrDying()) {
         target = entity.getTarget();
      }
      if (target == null || target.isDeadOrDying()) {
         target = entity.getLastHurtByMob();
         if (target != null
            && target.isAlive()
            && !target.isAlliedTo(entity)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(target)
            && !ServantMasterTargeting.isContractMaster(entity, target)) {
            entity.setTarget(target);
         }
      }
      if (target == null || target.isDeadOrDying()) {
         target = findEmergencyTarget(entity);
         if (target != null) {
            entity.setTarget(target);
         }
      }
      if (target == null || target.isDeadOrDying()) {
         entity.setFlyingMode(false);
         entity.clearTemporaryFocusItem();
         return;
      }

      long now = context.gameTick();
      entity.getPersistentData().putLong(TAG_LAST_COMBAT_ACTIVITY_TICK, now);
      consumeUtilityItems(entity);
      maybeUseGoldenFleece(entity, now);
      maybeCombatSummon(entity, now);
      boolean insideWorkshop = entity.isInsideWorkshop();
      double healthRatio = entity.getHealth() / entity.getMaxHealth();

      double distance = entity.distanceTo(target);
      if (distance > 10.0) {
         entity.setFlyingMode(true);
      } else if (distance < 6.0) {
         entity.setFlyingMode(false);
      }
      maintainFlightHeight(entity, entity.isFlyingMode() ? target : null);

      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
      faceTargetWhileFlying(entity, target);
      boolean hasLineOfSight = entity.getSensing().hasLineOfSight(target);
      if (handleUndergroundTarget(entity, target, now, hasLineOfSight)) {
         return;
      }
      LivingEntity closeThreat = findCloseThreat(entity, target);
      final LivingEntity spellTarget = target;
      int nearbyEnemyCount = countNearbyHostiles(entity, target, 12.0);
      boolean preferLargeMagic = nearbyEnemyCount >= 10 || target.getHealth() > 100.0F || target.getMaxHealth() > 100.0F;

      if (preferLargeMagic) {
         maybeMassDragonfangRelease(entity, now, nearbyEnemyCount, target);
      }

      if (!insideWorkshop
         && healthRatio <= 0.32F
         && MedeaWorkshopHelper.canTeleportNow(entity, now)
         && tryCastSpell(entity, now, SpellType.RETURN_TO_WORKSHOP, () -> castReturnToWorkshop(entity, now))) {
         return;
      }
      if (entity.isPerformingAction() || entity.isRoaring() || entity.isSlamming()) {
         maintainCasterSpacing(entity, closeThreat != null ? closeThreat : target, now, distance);
         return;
      }
      if (closeThreat != null) {
         if (insideWorkshop
            && canWorkshopEmergencyTeleport(entity, now)
            && tryCastSpell(entity, now, SpellType.WORKSHOP_ESCAPE, () -> castWorkshopEmergencyTeleport(entity, closeThreat, now))) {
            return;
         }
         if (!insideWorkshop && canAerialEscape(entity, now) && tryCastSpell(entity, now, SpellType.AERIAL_ESCAPE, () -> castAerialEscape(entity, closeThreat, now))) {
            if (canCastMiniBeam(entity, now)) {
               tryCastSpell(entity, now, SpellType.MINI_BEAM, () -> castMiniBeam(entity, closeThreat, now));
            }
            return;
         }
         if (canCastRepulsionNova(entity, now) && tryCastSpell(entity, now, SpellType.REPULSION_NOVA, () -> castRepulsionNova(entity, now))) {
            tryFollowUpSpell(entity, closeThreat, now, hasLineOfSight, distance);
            return;
         }
         if (canCastFrostShackle(entity, now) && tryCastSpell(entity, now, SpellType.FROST_SHACKLE, () -> castFrostShackle(entity, closeThreat, now))) {
            tryFollowUpSpell(entity, closeThreat, now, hasLineOfSight, distance);
            return;
         }
      }
      if (maintainCasterSpacing(entity, closeThreat != null ? closeThreat : target, now, distance)) {
         return;
      }

      LineOfFireState lineOfFire = evaluateLineOfFire(entity, target);
      if (lineOfFire.blocked() && handleBlockedLineOfFire(entity, target, now, distance, lineOfFire)) {
         return;
      }
      if (!lineOfFire.blocked()) {
         clearBlockedLineState(entity);
      }

      if (distance <= 4.5) {
         if (!entity.isBarrierActive()
            && now - entity.getPersistentData().getLong(TAG_LAST_BARRIER_TICK) >= 200L
            && tryCastSpell(entity, now, SpellType.BARRIER, () -> castBarrier(entity, now))) {
            return;
         }
         if (shouldUseBlinkVolley(entity, target, distance, hasLineOfSight)
            && canCastBlinkVolley(entity, now)
            && tryCastSpell(entity, now, SpellType.BLINK_VOLLEY, () -> castBlinkVolley(entity, spellTarget, now))) {
            return;
         }
         if (shouldUseRetreatTeleport(entity, distance, hasLineOfSight)
            && MedeaWorkshopHelper.canTeleportNow(entity, now)
            && tryCastSpell(entity, now, SpellType.TELEPORT, () -> castTeleport(entity, spellTarget, now))) {
            return;
         }
         if (canCastRepulsionNova(entity, now) && tryCastSpell(entity, now, SpellType.REPULSION_NOVA, () -> castRepulsionNova(entity, now))) {
            tryFollowUpSpell(entity, target, now, hasLineOfSight, distance);
            return;
         }
         if (canCastFrostShackle(entity, now) && tryCastSpell(entity, now, SpellType.FROST_SHACKLE, () -> castFrostShackle(entity, spellTarget, now))) {
            tryFollowUpSpell(entity, target, now, hasLineOfSight, distance);
            return;
         }
         if (shouldUseRuleBreaker(entity, target, now)) {
            if (tryPrepareRuleBreakerStab(entity, target, now)) {
               return;
            }
            if (tryCastSpell(entity, now, SpellType.RULE_BREAKER, () -> castRuleBreaker(entity, spellTarget, now))) {
               return;
            }
         }
         if (preferLargeMagic && tryLargeMagicPriority(entity, spellTarget, now, distance, hasLineOfSight, nearbyEnemyCount)) {
            return;
         }
         if (canCastLightning(entity, now) && tryCastSpell(entity, now, SpellType.LIGHTNING, () -> castLightning(entity, spellTarget, now))) {
            return;
         }
         if (canCastBolt(entity, now) && tryCastSpell(entity, now, SpellType.BOLT, () -> castBolt(entity, spellTarget, now))) {
            return;
         }
         return;
      }

      if (distance <= 16.0) {
         if (preferLargeMagic && tryLargeMagicPriority(entity, spellTarget, now, distance, hasLineOfSight, nearbyEnemyCount)) {
            return;
         }
         if (tryShowyVarietyCast(entity, spellTarget, now, distance, hasLineOfSight)) {
            return;
         }
         if (entity.isFlyingMode() && canCastMiniBeam(entity, now) && tryCastSpell(entity, now, SpellType.MINI_BEAM, () -> castMiniBeam(entity, spellTarget, now))) {
            return;
         }
         if (canCastFlameBurst(entity, now) && shouldUseFlameBurst(distance) && tryCastSpell(entity, now, SpellType.FLAME_BURST, () -> castFlameBurst(entity, spellTarget, now))) {
            return;
         }
         if (canCastFrostShackle(entity, now)
            && shouldUseFrostShackle(target, distance, hasLineOfSight)
            && tryCastSpell(entity, now, SpellType.FROST_SHACKLE, () -> castFrostShackle(entity, spellTarget, now))) {
            tryFollowUpSpell(entity, target, now, hasLineOfSight, distance);
            return;
         }
         if (shouldUseHecateBind(target, distance, hasLineOfSight)
            && canCastHecateBind(entity, now)
            && tryCastSpell(entity, now, SpellType.HECATE_BIND, () -> castHecateBind(entity, spellTarget, now))) {
            tryFollowUpSpell(entity, target, now, hasLineOfSight, distance);
            return;
         }
         if (shouldUseStrafeBarrage(distance, hasLineOfSight)
            && canCastStrafeBarrage(entity, now)
            && tryCastSpell(entity, now, SpellType.STRAFE_BARRAGE, () -> castStrafeBarrage(entity, spellTarget, now))) {
            return;
         }
         if (shouldUseRuleBreaker(entity, target, now)) {
            if (tryPrepareRuleBreakerStab(entity, target, now)) {
               return;
            }
            if (tryCastSpell(entity, now, SpellType.RULE_BREAKER, () -> castRuleBreaker(entity, spellTarget, now))) {
               return;
            }
         }
         if (canCastLightning(entity, now) && tryCastSpell(entity, now, SpellType.LIGHTNING, () -> castLightning(entity, spellTarget, now))) {
            return;
         }
         if (distance >= 6.0 && canCastBeam(entity, now) && tryCastSpell(entity, now, SpellType.BEAM, () -> castBeamVolley(entity, spellTarget, now))) {
            return;
         }
         if (canCastBolt(entity, now) && tryCastSpell(entity, now, SpellType.BOLT, () -> castBolt(entity, spellTarget, now))) {
         }
         return;
      }

      if (preferLargeMagic && tryLargeMagicPriority(entity, spellTarget, now, distance, hasLineOfSight, nearbyEnemyCount)) {
         return;
      }
      if (tryShowyVarietyCast(entity, spellTarget, now, distance, hasLineOfSight)) {
         return;
      }
      if (entity.isFlyingMode() && canCastMiniBeam(entity, now) && tryCastSpell(entity, now, SpellType.MINI_BEAM, () -> castMiniBeam(entity, spellTarget, now))) {
        return;
      }
      if (canCastSuperBolt(entity, now) && shouldUseSuperBolt(distance, hasLineOfSight) && tryCastSpell(entity, now, SpellType.SUPER_BOLT, () -> castSuperBolt(entity, spellTarget, now))) {
        return;
      }
      if (canCastFlameBurst(entity, now) && distance >= 8.0 && tryCastSpell(entity, now, SpellType.FLAME_BURST, () -> castFlameBurst(entity, spellTarget, now))) {
        return;
      }
      if (canCastBeam(entity, now) && tryCastSpell(entity, now, SpellType.BEAM, () -> castBeamVolley(entity, spellTarget, now))) {
        return;
      }
      if (canCastBolt(entity, now) && tryCastSpell(entity, now, SpellType.BOLT, () -> castBolt(entity, spellTarget, now))) {
         return;
      }
   }

   public static void applyRuleBreakerHit(LivingEntity target, LivingEntity attacker) {
      if (target == null || !target.isAlive()) {
         return;
      }

      Collection<MobEffectInstance> activeEffects = List.copyOf(target.getActiveEffects());
      for (MobEffectInstance effect : activeEffects) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
            target.removeEffect(effect.getEffect());
         }
      }

      target.removeEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY);
      target.removeEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE);
      target.removeEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT);
      target.removeEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH);
      target.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY);
      target.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE);
      target.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_SIGHT);
      target.removeEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH);

      if (target instanceof ServantEntity servant) {
         servant.getPersistentData().remove(CuChulainnCombatHelper.PROTECTION_FROM_ARROWS_TAG);
         servant.getPersistentData().remove(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG);
         servant.getPersistentData().remove(CuChulainnCombatHelper.GAE_BOLG_WINDUP_UNTIL_TAG);
         CuChulainnCombatHelper.clearRune(servant);
      }

      if (target instanceof net.minecraft.server.level.ServerPlayer targetPlayer) {
         TypeMoonWorldModVariables.PlayerVariables targetVars = targetPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (targetVars.servant_card_transformed) {
            net.minecraft.server.level.ServerPlayer master = MasterStateManager.getMaster(targetPlayer, targetVars);
            if (master != null) {
               MasterServantLinkService.terminateContract(master, targetPlayer);
            }
         } else if (targetVars.master_active) {
            net.minecraft.server.level.ServerPlayer servant = MasterServantLinkService.getLinkedServant(targetPlayer, targetVars);
            if (servant != null) {
               MasterServantLinkService.terminateContract(targetPlayer, servant);
            }
         }
      }

      if (target.getPersistentData().getBoolean(MedeaWorkshopHelper.TAG_MAGIC_SUMMON)) {
         target.invulnerableTime = 0;
         target.hurt(attacker != null ? attacker.damageSources().magic() : target.damageSources().magic(), Float.MAX_VALUE);
         if (target.isAlive()) {
            target.discard();
         }
      }

      purgeOwnedSummons(target);
   }

   private static boolean canCastBolt(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_BOLT_TICK) >= 8L && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_BOLT);
   }

   private static boolean canCastLightning(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_LIGHTNING_TICK) >= 18L && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_LIGHTNING);
   }

   private static boolean canCastBeam(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_BEAM_TICK) >= 100L && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_BEAM);
   }

   private static boolean canCastBlinkVolley(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_BLINK_VOLLEY_TICK) >= 100L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_BLINK_VOLLEY);
   }

   private static boolean canCastStrafeBarrage(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_STRAFE_BARRAGE_TICK) >= 100L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_STRAFE_BARRAGE);
   }

   private static boolean canCastHecateBind(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_BIND_TICK) >= 100L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_HECATE_BIND);
   }

   private static boolean canCastSuperBolt(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_SUPER_BOLT_TICK) >= 100L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_SUPER_BOLT);
   }

   private static boolean canCastMiniBeam(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_MINI_BEAM_TICK) >= 12L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_MINI_BEAM);
   }

   private static boolean canCastFlameBurst(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_FLAME_BURST_TICK) >= 12L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_FLAME_BURST);
   }

   private static boolean canCastFrostShackle(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_FROST_SHACKLE_TICK) >= 14L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_FROST_SHACKLE);
   }

   private static boolean canCastRepulsionNova(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_REPULSION_NOVA_TICK) >= 10L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_REPULSION_NOVA);
   }

   private static boolean canWorkshopEmergencyTeleport(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_WORKSHOP_ESCAPE_TICK) >= 60L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_WORKSHOP_ESCAPE);
   }

   private static boolean canAerialEscape(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_AERIAL_ESCAPE_TICK) >= 24L;
   }

   private static boolean canCastOrbitHex(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_ORBIT_HEX_TICK) >= 14L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_ORBIT_HEX);
   }

   private static boolean canCastCrossfire(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_CROSSFIRE_TICK) >= 16L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_CROSSFIRE);
   }

   private static boolean canCastStarfall(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_STARFALL_TICK) >= 18L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_STARFALL);
   }

   private static boolean canCastThunderstorm(MedeaEntity entity, long now) {
      return now - entity.getPersistentData().getLong(TAG_LAST_THUNDERSTORM_TICK) >= 100L
         && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_THUNDERSTORM);
   }

   private static boolean castBolt(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return false;
      }
      Vec3 direction = getAimDirection(entity, target);
      int pattern = entity.getRandom().nextInt(4);
      if (pattern == 3 && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_BOLT)) {
         if (!MedeaWorkshopHelper.consumeMana(entity, COST_BOLT)) {
            return false;
         }
         entity.getPersistentData().putLong(TAG_LAST_BOLT_TICK, now);
         maybeTriggerBasicCast(entity);
         Vec3 side = sidewaysAxis(direction);
         Vec3 back = direction.scale(-1.0);
         Vec3 leftRear = entity.getEyePosition().add(side.scale(-1.05)).add(back.scale(0.9)).add(0.0, 0.12, 0.0);
         Vec3 rightRear = entity.getEyePosition().add(side.scale(1.05)).add(back.scale(0.9)).add(0.0, 0.12, 0.0);
         spawnMagicCircle(level, leftRear, direction, 0.52F);
         spawnMagicCircle(level, rightRear, direction, 0.52F);
         spawnBoltProjectile(level, entity, leftRear, target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(leftRear).normalize(), MedeaMagicBoltEntity.Mode.BOLT, MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 8.5F), 2.8F, 0.0F);
         spawnBoltProjectile(level, entity, rightRear, target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(rightRear).normalize(), MedeaMagicBoltEntity.Mode.BOLT, MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 8.5F), 2.8F, 0.0F);
         level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 10, 0.55, 0.25, 0.55, 0.03);
         level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.48F, 1.7F);
         return true;
      }
      if (pattern == 2 && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_BOLT)) {
         if (!MedeaWorkshopHelper.consumeMana(entity, COST_BOLT)) {
            return false;
         }
         entity.getPersistentData().putLong(TAG_LAST_BOLT_TICK, now);
         maybeTriggerBasicCast(entity);
         Vec3 spawnPos = entity.getEyePosition().add(direction.scale(0.75));
         spawnMagicCircle(level, spawnPos.subtract(direction.scale(0.65)), direction, 0.7F);
         for (int i = 0; i < 3; i++) {
            final int shot = i;
            TYPE_MOON_WORLD.queueServerWork(i * 2, () -> {
               if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
                  return;
               }
               LivingEntity resolvedTarget = target.isAlive() ? target : entity.getTarget();
               if (resolvedTarget == null || !resolvedTarget.isAlive()) {
                  return;
               }
               Vec3 burstDirection = getAimDirection(entity, resolvedTarget);
               Vec3 burstSpawn = entity.getEyePosition().add(burstDirection.scale(0.75));
               spawnBoltProjectile(
                  serverLevel,
                  entity,
                  burstSpawn,
                  burstDirection,
                  MedeaMagicBoltEntity.Mode.BOLT,
                  MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 10.0F),
                  3.0F,
                  0.015F + shot * 0.01F
               );
               serverLevel.sendParticles(ParticleTypes.END_ROD, burstSpawn.x, burstSpawn.y, burstSpawn.z, 4, 0.06, 0.06, 0.06, 0.02);
            });
         }
         level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.5F, 1.65F);
         return true;
      }

      if (pattern == 1 && entity.getCurrentMp() >= MedeaWorkshopHelper.adjustedManaCost(entity, COST_BOLT)) {
         if (!MedeaWorkshopHelper.consumeMana(entity, COST_BOLT)) {
            return false;
         }
         entity.getPersistentData().putLong(TAG_LAST_BOLT_TICK, now);
         maybeTriggerBasicCast(entity);
         Vec3 spawnPos = entity.getEyePosition().add(direction.scale(0.75));
         Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
         if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1.0, 0.0, 0.0);
         } else {
            side = side.normalize();
         }
         spawnMagicCircle(level, spawnPos.subtract(direction.scale(0.65)), direction, 0.72F);
         for (int i = -1; i <= 1; i += 2) {
            Vec3 offsetSpawn = spawnPos.add(side.scale(0.22 * i));
            Vec3 shotDirection = direction.add(side.scale(0.08 * i)).normalize();
            spawnBoltProjectile(level, entity, offsetSpawn, shotDirection, MedeaMagicBoltEntity.Mode.BOLT, MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 9.0F), 2.85F, 0.01F);
         }
         level.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 8, 0.18, 0.18, 0.18, 0.02);
         level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.45F, 1.58F);
         return true;
      }

      if (!MedeaWorkshopHelper.consumeMana(entity, COST_BOLT)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_BOLT_TICK, now);
      maybeTriggerBasicCast(entity);
      Vec3 spawnPos = entity.getEyePosition().add(direction.scale(0.75));
      spawnMagicCircle(level, spawnPos.subtract(direction.scale(0.65)), direction, 0.65F);
      spawnBoltProjectile(level, entity, spawnPos, direction, MedeaMagicBoltEntity.Mode.BOLT, MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 12.5F), 2.9F, 0.04F);
      level.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y, spawnPos.z, 5, 0.08, 0.08, 0.08, 0.02);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.4F, 1.5F);
      return true;
   }

   private static boolean castLightning(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_LIGHTNING)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_LIGHTNING_TICK, now);
      beginStaffCast(entity);
      maybeTriggerBasicCast(entity);
      ServantVoiceHelper.tryPlaySpell(entity);
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 direction = getAimDirection(entity, target);
      spawnMagicCircle(level, entity.position().add(0.0, entity.getBbHeight() * 0.45, 0.0), direction, 0.82F);
      for (int i = 0; i < 7; i++) {
         double y = center.y + 5.2 - i * 0.95;
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, y, center.z, 14, 0.18, 0.22, 0.18, 0.04);
         level.sendParticles(ParticleTypes.END_ROD, center.x, y, center.z, 6, 0.16, 0.2, 0.16, 0.03);
      }
      level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.3, center.z, 2, 0.15, 0.15, 0.15, 0.0);
      level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.25, center.z, 18, 0.45, 0.7, 0.45, 0.02);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 12, 0.3, 0.55, 0.3, 0.03);
      fractureTerrain(level, entity, center, 2.2, 4.8, 55.0F, 34);
      target.hurt(entity.damageSources().magic(), MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 35.0F));
      target.invulnerableTime = 0;
      level.playSound(null, BlockPos.containing(center), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 0.9F, 1.2F);
      queueStaffClear(entity, 14);
      return true;
   }

   private static boolean castThunderstorm(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_THUNDERSTORM)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_THUNDERSTORM_TICK, now);
      beginStaffCast(entity);
      entity.triggerRuneCastAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);
      List<LivingEntity> targets = level.getEntitiesOfClass(
         LivingEntity.class,
         target.getBoundingBox().inflate(10.0, 4.0, 10.0),
         other -> other != entity
            && other.isAlive()
            && !other.isAlliedTo(entity)
            && !ServantMasterTargeting.isContractMaster(entity, other)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(other)
      );
      if (!targets.contains(target)) {
         targets.add(0, target);
      }
      int strikes = 0;
      for (LivingEntity victim : targets) {
         if (strikes >= 5) {
            break;
         }
         Vec3 center = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y + 0.3, center.z, 18, 0.22, 0.35, 0.22, 0.04);
         level.sendParticles(ParticleTypes.FLASH, center.x, center.y + 0.45, center.z, 1, 0.0, 0.0, 0.0, 0.0);
         victim.invulnerableTime = 0;
         victim.hurt(entity.damageSources().magic(), MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 35.0F));
         victim.invulnerableTime = 0;
         fractureTerrain(level, entity, center, 1.9, 5.0, 60.0F, 24);
         spawnLightningBolts(level, center, 1);
         strikes++;
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 1.1F, 0.9F);
      queueStaffClear(entity, 18);
      return true;
   }

   private static boolean castBeamVolley(MedeaEntity entity, LivingEntity target, long now) {
      return castBeamVolley(entity, target, now, true, false);
   }

   private static boolean castBeamVolley(MedeaEntity entity, LivingEntity target, long now, boolean breakBlocks) {
      return castBeamVolley(entity, target, now, breakBlocks, false);
   }

   private static boolean castBeamVolley(MedeaEntity entity, LivingEntity target, long now, boolean breakBlocks, boolean forceSingleShot) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_BEAM)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_BEAM_TICK, now);
      entity.setTemporaryFocusItem(MedeaEntity.FocusItem.HECATES_STAFF);
      if (!entity.isSoftCombatActionActive()) {
         entity.triggerRuneCastAnimation();
      }
      ServantVoiceHelper.tryPlaySpell(entity);
      int shotCount = forceSingleShot ? 1 : entity.isInsideWorkshop() ? 5 : 3;
      float damage = MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 35.0F);

      for (int i = 0; i < shotCount; i++) {
         final int beamIndex = i;
         TYPE_MOON_WORLD.queueServerWork(i * 6 + 1, () -> {
            if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
               return;
            }
            LivingEntity resolvedTarget = target.isAlive() ? target : entity.getTarget();
            if (resolvedTarget == null || !resolvedTarget.isAlive()) {
               return;
            }
            Vec3 start = variedBeamOrigin(entity, resolvedTarget, beamIndex, shotCount);
            Vec3 end = resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.5, 0.0);
            Vec3 direction = end.subtract(start).normalize();
            spawnMagicCircle(serverLevel, start.subtract(direction.scale(0.75)), direction, 0.9F);
            MedeaBeamEffectEntity beam = new MedeaBeamEffectEntity(serverLevel, entity, start, end, damage, 10);
            beam.setBreakBlocks(breakBlocks);
            serverLevel.addFreshEntity(beam);
            serverLevel.playSound(
               null,
               BlockPos.containing(start),
               breakBlocks ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.BEACON_POWER_SELECT,
               SoundSource.HOSTILE,
               breakBlocks ? 0.95F : 0.65F,
               breakBlocks ? 0.85F : 1.4F
            );
         });
      }
      TYPE_MOON_WORLD.queueServerWork(shotCount * 6 + 10, () -> {
         if (entity.isAlive() && entity.getTemporaryFocusItem() == MedeaEntity.FocusItem.HECATES_STAFF) {
            entity.clearTemporaryFocusItem();
         }
      });
      return true;
   }

   private static boolean castBarrier(MedeaEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_BARRIER)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_BARRIER_TICK, now);
      beginStaffCast(entity);
      entity.setBarrierStrength(100.0F);
      spawnMagicCircle(level, entity.position().add(0.0, entity.getBbHeight() * 0.45, 0.0), entity.getLookAngle(), 1.1F);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(3.2),
         target -> target != entity && target.isAlive() && !target.isAlliedTo(entity)
            && !ServantMasterTargeting.isContractMaster(entity, target)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(target)
      )) {
         Vec3 push = living.position().subtract(entity.position()).normalize().scale(1.2);
         living.push(push.x, 0.35, push.z);
         living.hurtMarked = true;
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.8F, 1.2F);
      queueStaffClear(entity, 12);
      return true;
   }

   private static boolean castTeleport(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_TELEPORT)) {
         return false;
      }
      beginStaffCast(entity);
      BlockPos destination = MedeaWorkshopHelper.findTeleportPosition(entity);
      Vec3 from = entity.position();
      teleportEntity(entity, destination, now);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.6, from.z, 18, 0.3, 0.4, 0.3, 0.05);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 0.6, entity.getZ(), 18, 0.3, 0.4, 0.3, 0.05);
      level.playSound(null, destination, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.1F);
      queueStaffClear(entity, 10);
      return true;
   }

   private static boolean castBlinkVolley(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_BLINK_VOLLEY)) {
         return false;
      }

      Vec3 destination = findBlinkDestination(level, target);
      if (destination == null) {
         return false;
      }

      entity.getPersistentData().putLong(TAG_LAST_BLINK_VOLLEY_TICK, now);
      beginStaffCast(entity);
      entity.triggerTeleportAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);
      Vec3 from = entity.position();
      entity.teleportTo(destination.x, destination.y, destination.z);
      entity.setDeltaMovement(Vec3.ZERO);
      entity.faceToward(target.position());
      entity.fallDistance = 0.0F;
      entity.setFlyingMode(false);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.6, from.z, 18, 0.3, 0.4, 0.3, 0.05);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, destination.x, destination.y + 0.6, destination.z, 18, 0.3, 0.4, 0.3, 0.05);
      level.playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.9F, 1.25F);

      for (int i = 0; i < 3; i++) {
         final int index = i;
         TYPE_MOON_WORLD.queueServerWork(i * 3 + 1, () -> {
            if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
               return;
            }
            LivingEntity resolvedTarget = target.isAlive() ? target : entity.getTarget();
            if (resolvedTarget == null || !resolvedTarget.isAlive()) {
               return;
            }

            if (index == 0 && entity.distanceToSqr(resolvedTarget) <= 25.0) {
               resolvedTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true, true));
            }

            Vec3 direction = getAimDirection(entity, resolvedTarget);
            Vec3 spawnPos = entity.getEyePosition().add(direction.scale(0.7));
            spawnMagicCircle(serverLevel, spawnPos.subtract(direction.scale(0.6)), direction, 0.62F);
            spawnBoltProjectile(
               serverLevel,
               entity,
               spawnPos,
               direction,
               MedeaMagicBoltEntity.Mode.BOLT,
               MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 9.0F),
               2.75F,
               0.01F + index * 0.01F
            );
         });
      }

      queueStaffClear(entity, 14);
      return true;
   }

   private static boolean castStrafeBarrage(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_STRAFE_BARRAGE)) {
         return false;
      }

      Vec3 toTarget = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return false;
      }

      entity.getPersistentData().putLong(TAG_LAST_STRAFE_BARRAGE_TICK, now);
      beginStaffCast(entity);
      entity.triggerChargeAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);

      Vec3 forward = horizontal.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x).normalize().scale(entity.getRandom().nextBoolean() ? 1.0 : -1.0);
      Vec3 dash = side.add(forward.scale(-0.35)).normalize();
      entity.setFlyingMode(true);
      entity.faceVector(dash);
      entity.setDeltaMovement(dash.x * 0.95, Math.max(entity.getDeltaMovement().y, 0.22), dash.z * 0.95);
      entity.hasImpulse = true;

      Vec3 start = entity.getEyePosition();
      spawnMagicCircle(level, start.subtract(forward.scale(0.5)), forward, 0.72F);
      for (double step = 0.75; step <= 4.0; step += 0.85) {
         Vec3 trace = entity.position().add(dash.scale(step)).add(0.0, entity.getBbHeight() * 0.45, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, trace.x, trace.y, trace.z, 1, 0.05, 0.05, 0.05, 0.0);
         level.sendParticles(CIRCLE_PRIMARY, trace.x, trace.y, trace.z, 1, 0.02, 0.02, 0.02, 0.0);
      }

      for (int i = 0; i < 4; i++) {
         TYPE_MOON_WORLD.queueServerWork(i * 3 + 1, () -> {
            if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
               return;
            }
            LivingEntity resolvedTarget = target.isAlive() ? target : entity.getTarget();
            if (resolvedTarget == null || !resolvedTarget.isAlive()) {
               return;
            }
            Vec3 direction = getAimDirection(entity, resolvedTarget);
            Vec3 spawnPos = entity.getEyePosition().add(direction.scale(0.65));
            spawnBoltProjectile(
               serverLevel,
               entity,
               spawnPos,
               direction,
               MedeaMagicBoltEntity.Mode.BOLT,
               MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 9.0F),
               2.85F,
               0.03F
            );
         });
      }

      queueStaffClear(entity, 20);
      return true;
   }

   private static boolean castHecateBind(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_HECATE_BIND)) {
         return false;
      }

      entity.getPersistentData().putLong(TAG_LAST_BIND_TICK, now);
      beginStaffCast(entity);
      entity.triggerRuneCastAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);

      Vec3 anchor = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      Vec3 direction = anchor.subtract(entity.getEyePosition());
      if (direction.lengthSqr() < 1.0E-6) {
         direction = entity.getLookAngle();
      } else {
         direction = direction.normalize();
      }

      spawnMagicCircle(level, anchor, direction, 0.85F);
      level.sendParticles(ParticleTypes.ENCHANT, anchor.x, anchor.y, anchor.z, 18, 0.45, 0.65, 0.45, 0.02);
      level.sendParticles(ParticleTypes.END_ROD, anchor.x, anchor.y + 0.3, anchor.z, 12, 0.35, 0.5, 0.35, 0.03);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));

      TYPE_MOON_WORLD.queueServerWork(10, () -> {
         if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive() || !target.isAlive()) {
            return;
         }

         Vec3 current = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
         boolean trapped = current.distanceToSqr(anchor) <= 2.75 * 2.75;
         float damage = MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, trapped ? 24.0F : 15.0F);
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().magic(), damage);
         target.invulnerableTime = 0;
         if (trapped) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 12, 0, false, true, true));
         }
         serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, current.x, current.y, current.z, 16, 0.3, 0.55, 0.3, 0.03);
         serverLevel.sendParticles(CIRCLE_ACCENT, current.x, current.y + 0.2, current.z, 8, 0.18, 0.25, 0.18, 0.0);
         serverLevel.playSound(null, BlockPos.containing(current), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 0.9F, 1.35F);
      });

      queueStaffClear(entity, 18);
      return true;
   }

   private static boolean castSuperBolt(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_SUPER_BOLT)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_SUPER_BOLT_TICK, now);
      beginStaffCast(entity);
      entity.triggerChargeAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);
      Vec3 direction = getAimDirection(entity, target);
      Vec3 spawnPos = entity.getEyePosition().add(direction.scale(0.95));
      spawnMagicCircle(level, spawnPos.subtract(direction.scale(0.75)), direction, 1.2F);
      level.sendParticles(ParticleTypes.FLASH, spawnPos.x, spawnPos.y, spawnPos.z, 2, 0.08, 0.08, 0.08, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y, spawnPos.z, 24, 0.35, 0.35, 0.35, 0.03);
      level.sendParticles(CIRCLE_PRIMARY, spawnPos.x, spawnPos.y, spawnPos.z, 12, 0.18, 0.18, 0.18, 0.0);
      spawnBoltProjectile(level, entity, spawnPos, direction, MedeaMagicBoltEntity.Mode.SUPER_BOLT, MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 50.0F), 1.45F, 0.0F);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 0.72F);
      queueStaffClear(entity, 18);
      return true;
   }

   private static boolean castMiniBeam(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_MINI_BEAM)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_MINI_BEAM_TICK, now);
      beginStaffCast(entity);
      entity.triggerRuneCastAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);
      int shotCount = entity.isFlyingMode() ? 2 : 1;
      float damage = MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, entity.isFlyingMode() ? 22.0F : 19.0F);
      for (int i = 0; i < shotCount; i++) {
         final int idx = i;
         TYPE_MOON_WORLD.queueServerWork(i * 3 + 1, () -> {
            if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
               return;
            }
            LivingEntity resolvedTarget = target.isAlive() ? target : entity.getTarget();
            if (resolvedTarget == null || !resolvedTarget.isAlive()) {
               return;
            }
            Vec3 direction = getAimDirection(entity, resolvedTarget);
            Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
            if (side.lengthSqr() < 1.0E-4) {
               side = new Vec3(1.0, 0.0, 0.0);
            } else {
               side = side.normalize();
            }
            Vec3 start = entity.getEyePosition().add(direction.scale(0.65)).add(side.scale((idx == 0 ? -0.18 : 0.18)));
            Vec3 end = resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.45, 0.0);
            spawnMagicCircle(serverLevel, start.subtract(direction.scale(0.55)), direction, 0.6F);
            MedeaBeamEffectEntity beam = new MedeaBeamEffectEntity(serverLevel, entity, start, end, damage, 7);
            beam.setBreakBlocks(true);
            serverLevel.addFreshEntity(beam);
            serverLevel.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 8, 0.12, 0.12, 0.12, 0.03);
         });
      }
      queueStaffClear(entity, 14);
      return true;
   }

   private static boolean castFlameBurst(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_FLAME_BURST)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_FLAME_BURST_TICK, now);
      beginStaffCast(entity);
      maybeTriggerBasicCast(entity);
      ServantVoiceHelper.tryPlaySpell(entity);
      Vec3 direction = getAimDirection(entity, target);
      Vec3 spawnPos = entity.getEyePosition().add(direction.scale(0.72));
      Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(1.0, 0.0, 0.0);
      } else {
         side = side.normalize();
      }
      spawnMagicCircle(level, spawnPos.subtract(direction.scale(0.55)), direction, 0.82F);
      for (int i = -1; i <= 1; i++) {
         Vec3 shotDirection = direction.add(side.scale(0.09 * i)).normalize();
         Vec3 shotSpawn = spawnPos.add(side.scale(0.18 * i));
         spawnBoltProjectile(level, entity, shotSpawn, shotDirection, MedeaMagicBoltEntity.Mode.FIRE_BOLT, MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 16.0F), 2.45F, 0.0F);
      }
      Vec3 impact = target.position().add(0.0, 0.1, 0.0);
      level.sendParticles(ParticleTypes.FLAME, impact.x, impact.y + 0.6, impact.z, 24, 0.65, 0.45, 0.65, 0.04);
      level.sendParticles(ParticleTypes.LAVA, impact.x, impact.y + 0.4, impact.z, 10, 0.25, 0.18, 0.25, 0.0);
      level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y + 0.5, impact.z, 16, 0.55, 0.4, 0.55, 0.02);
      fractureTerrain(level, entity, impact, 2.35, 1.6, 45.0F, 36);
      level.playSound(null, entity.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.95F, 1.05F);
      queueStaffClear(entity, 12);
      return true;
   }

   private static boolean castFrostShackle(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_FROST_SHACKLE)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_FROST_SHACKLE_TICK, now);
      beginStaffCast(entity);
      entity.triggerRuneCastAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);
      Vec3 anchor = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      Vec3 direction = getAimDirection(entity, target);
      spawnMagicCircle(level, anchor, direction, 0.95F);
      level.sendParticles(ParticleTypes.SNOWFLAKE, anchor.x, anchor.y, anchor.z, 36, 0.55, 0.8, 0.55, 0.03);
      level.sendParticles(ParticleTypes.ITEM_SNOWBALL, anchor.x, anchor.y + 0.25, anchor.z, 20, 0.38, 0.55, 0.38, 0.01);
      level.sendParticles(CIRCLE_PRIMARY, anchor.x, anchor.y + 0.15, anchor.z, 10, 0.18, 0.18, 0.18, 0.0);
      fractureTerrain(level, entity, anchor, 1.75, 1.9, 38.0F, 22);
      target.setTicksFrozen(Math.max(target.getTicksFrozen(), 160));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, true));
      target.setDeltaMovement(target.getDeltaMovement().scale(0.25));
      TYPE_MOON_WORLD.queueServerWork(10, () -> {
         if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive() || !target.isAlive()) {
            return;
         }
         Vec3 current = target.position().add(0.0, target.getBbHeight() * 0.4, 0.0);
         target.invulnerableTime = 0;
         target.hurt(entity.damageSources().magic(), MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 14.0F));
         target.invulnerableTime = 0;
         serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, current.x, current.y, current.z, 18, 0.28, 0.4, 0.28, 0.02);
      });
      queueStaffClear(entity, 18);
      return true;
   }

   private static boolean castRepulsionNova(MedeaEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_REPULSION_NOVA)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_REPULSION_NOVA_TICK, now);
      beginStaffCast(entity);
      entity.setBarrierStrength(Math.max(entity.getBarrierStrength(), 45.0F));
      spawnMagicCircle(level, entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0), entity.getLookAngle(), 1.35F);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 30, 0.8, 0.55, 0.8, 0.03);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 18, 0.65, 0.35, 0.65, 0.02);
      level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.25, entity.getZ(), 20, 1.1, 0.15, 1.1, 0.06);
      fractureTerrain(level, entity, entity.position().add(0.0, 0.25, 0.0), 3.1, 0.9, 42.0F, 48);
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(4.0),
         other -> other != entity && other.isAlive() && !other.isAlliedTo(entity)
            && !ServantMasterTargeting.isContractMaster(entity, other)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(other)
      )) {
         Vec3 push = living.position().subtract(entity.position());
         if (push.lengthSqr() < 1.0E-4) {
            push = new Vec3(0.0, 0.0, 1.0);
         } else {
            push = push.normalize();
         }
         living.push(push.x * 1.4, 0.55, push.z * 1.4);
         living.hurt(entity.damageSources().magic(), MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 13.0F));
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, false, true, true));
         living.hurtMarked = true;
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.05F, 0.8F);
      queueStaffClear(entity, 10);
      return true;
   }

   private static boolean castOrbitHex(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_ORBIT_HEX)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_ORBIT_HEX_TICK, now);
      beginStaffCast(entity);
      entity.triggerRuneCastAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 direction = getAimDirection(entity, target);
      Vec3 side = sidewaysAxis(direction);
      Vec3 back = direction.scale(-1.0);
      Vec3[] spawns = new Vec3[]{
         entity.getEyePosition().add(side.scale(-1.15)).add(back.scale(0.95)).add(0.0, 0.18, 0.0),
         entity.getEyePosition().add(side.scale(1.15)).add(back.scale(0.95)).add(0.0, 0.18, 0.0),
         entity.getEyePosition().add(side.scale(-0.7)).add(back.scale(1.45)).add(0.0, 0.38, 0.0),
         entity.getEyePosition().add(side.scale(0.7)).add(back.scale(1.45)).add(0.0, 0.38, 0.0)
      };
      MedeaMagicBoltEntity.Mode[] modes = new MedeaMagicBoltEntity.Mode[]{
         MedeaMagicBoltEntity.Mode.BOLT,
         MedeaMagicBoltEntity.Mode.FIRE_BOLT,
         MedeaMagicBoltEntity.Mode.FROST_BOLT,
         MedeaMagicBoltEntity.Mode.BOLT
      };
      for (int i = 0; i < spawns.length; i++) {
         final int idx = i;
         TYPE_MOON_WORLD.queueServerWork(i * 2, () -> {
            if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
               return;
            }
            LivingEntity resolvedTarget = target.isAlive() ? target : entity.getTarget();
            if (resolvedTarget == null || !resolvedTarget.isAlive()) {
               return;
            }
            Vec3 spawn = spawns[idx];
            Vec3 aim = resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.5, 0.0);
            Vec3 shotDirection = aim.subtract(spawn).normalize();
            spawnMagicCircle(serverLevel, spawn, shotDirection, 0.56F);
            spawnBoltProjectile(
               serverLevel,
               entity,
               spawn,
               shotDirection,
               modes[idx],
               MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 13.0F + idx * 0.5F),
               2.7F,
               0.0F
            );
            serverLevel.sendParticles(ParticleTypes.END_ROD, spawn.x, spawn.y, spawn.z, 6, 0.1, 0.1, 0.1, 0.02);
         });
      }
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.62, entity.getZ(), 20, 0.65, 0.3, 0.65, 0.03);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.75F, 1.75F);
      queueStaffClear(entity, 18);
      return true;
   }

   private static boolean castCrossfire(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_CROSSFIRE)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_CROSSFIRE_TICK, now);
      beginStaffCast(entity);
      entity.triggerRuneCastAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      Vec3 direction = getAimDirection(entity, target);
      Vec3 side = sidewaysAxis(direction);
      Vec3 back = direction.scale(-1.0);
      Vec3[] starts = new Vec3[]{
         entity.getEyePosition().add(side.scale(-1.5)).add(back.scale(1.0)).add(0.0, 0.2, 0.0),
         entity.getEyePosition().add(side.scale(1.5)).add(back.scale(1.0)).add(0.0, 0.2, 0.0)
      };
      for (Vec3 start : starts) {
         Vec3 beamDir = end.subtract(start).normalize();
         spawnMagicCircle(level, start.subtract(beamDir.scale(0.35)), beamDir, 0.66F);
         MedeaBeamEffectEntity beam = new MedeaBeamEffectEntity(level, entity, start, end, MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 20.0F), 8);
         beam.setBreakBlocks(true);
         level.addFreshEntity(beam);
         level.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 10, 0.14, 0.14, 0.14, 0.03);
         level.sendParticles(CIRCLE_PRIMARY, start.x, start.y, start.z, 4, 0.08, 0.08, 0.08, 0.0);
      }
      level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.1, 0.1, 0.1, 0.0);
      fractureTerrain(level, entity, end, 1.75, 2.0, 50.0F, 24);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.85F, 1.55F);
      queueStaffClear(entity, 16);
      return true;
   }

   private static boolean castStarfall(MedeaEntity entity, LivingEntity target, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_STARFALL)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_STARFALL_TICK, now);
      beginStaffCast(entity);
      entity.triggerChargeAnimation();
      ServantVoiceHelper.tryPlaySpell(entity);
      Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
      for (int i = 0; i < 5; i++) {
         final int idx = i;
         TYPE_MOON_WORLD.queueServerWork(i * 2, () -> {
            if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
               return;
            }
            LivingEntity resolvedTarget = target.isAlive() ? target : entity.getTarget();
            if (resolvedTarget == null || !resolvedTarget.isAlive()) {
               return;
            }
            double angle = (Math.PI * 2.0 * idx) / 5.0;
            Vec3 spawn = resolvedTarget.position()
               .add(Math.cos(angle) * 2.0, resolvedTarget.getBbHeight() + 4.0 + (idx & 1) * 0.6, Math.sin(angle) * 2.0);
            Vec3 aim = resolvedTarget.position().add(0.0, resolvedTarget.getBbHeight() * 0.5, 0.0);
            Vec3 shotDirection = aim.subtract(spawn).normalize();
            spawnMagicCircle(serverLevel, spawn, shotDirection, 0.5F);
            spawnBoltProjectile(
               serverLevel,
               entity,
               spawn,
               shotDirection,
               idx == 2 ? MedeaMagicBoltEntity.Mode.FROST_BOLT : idx == 4 ? MedeaMagicBoltEntity.Mode.FIRE_BOLT : MedeaMagicBoltEntity.Mode.BOLT,
               MedeaWorkshopHelper.applyWorkshopDamageBonus(entity, 12.5F),
               2.55F,
               0.0F
            );
            serverLevel.sendParticles(ParticleTypes.END_ROD, spawn.x, spawn.y, spawn.z, 5, 0.08, 0.08, 0.08, 0.02);
         });
      }
      level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 1.0, center.z, 28, 1.2, 1.4, 1.2, 0.02);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z, 20, 1.0, 1.2, 1.0, 0.03);
      level.playSound(null, entity.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 0.8F, 1.45F);
      queueStaffClear(entity, 18);
      return true;
   }

   private static boolean castWorkshopEmergencyTeleport(MedeaEntity entity, LivingEntity threat, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_WORKSHOP_ESCAPE)) {
         return false;
      }
      beginStaffCast(entity);
      BlockPos destination = MedeaWorkshopHelper.findSafeWorkshopEscapePosition(entity, level, threat, 6.5);
      entity.getPersistentData().putLong(TAG_LAST_WORKSHOP_ESCAPE_TICK, now);
      Vec3 from = entity.position();
      teleportEntity(entity, destination, now);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.6, from.z, 28, 0.35, 0.45, 0.35, 0.06);
      level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.7, entity.getZ(), 20, 0.35, 0.45, 0.35, 0.03);
      level.sendParticles(CIRCLE_ACCENT, entity.getX(), entity.getY() + 0.55, entity.getZ(), 10, 0.18, 0.18, 0.18, 0.0);
      queueStaffClear(entity, 12);
      return true;
   }

   private static boolean castReturnToWorkshop(MedeaEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_RETURN_TO_WORKSHOP)) {
         return false;
      }
      beginStaffCast(entity);
      Vec3 from = entity.position();
      teleportEntity(entity, MedeaWorkshopHelper.findTeleportPosition(entity), now);
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, from.x, from.y + 0.7, from.z, 20, 0.35, 0.45, 0.35, 0.05);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), entity.getY() + 0.7, entity.getZ(), 26, 0.35, 0.45, 0.35, 0.05);
      level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.9F);
      queueStaffClear(entity, 12);
      return true;
   }

   private static boolean castAerialEscape(MedeaEntity entity, LivingEntity threat, long now) {
      entity.getPersistentData().putLong(TAG_LAST_AERIAL_ESCAPE_TICK, now);
      entity.setFlyingMode(true);
      Vec3 away = entity.position().subtract(threat.position());
      Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      } else {
         horizontal = horizontal.normalize();
      }
      double escapeY = ServantFlightHelper.clampFlyingY(entity, entity.getY() + 4.0, threat);
      double lift = ServantFlightHelper.verticalVelocityToward(entity.getY(), escapeY, 0.22, 0.08, 0.42, 0.26);
      Vec3 velocity = horizontal.scale(0.95).add(0.0, lift, 0.0);
      entity.faceVector(horizontal);
      entity.setDeltaMovement(velocity.x, Math.max(entity.getDeltaMovement().y, velocity.y), velocity.z);
      entity.hasImpulse = true;
      entity.getMoveControl().setWantedPosition(
         entity.getX() + horizontal.x * 6.0,
         escapeY,
         entity.getZ() + horizontal.z * 6.0,
         1.2
      );
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.2, entity.getZ(), 22, 0.55, 0.18, 0.55, 0.06);
         level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 14, 0.28, 0.38, 0.28, 0.03);
         level.sendParticles(CIRCLE_PRIMARY, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 8, 0.16, 0.16, 0.16, 0.0);
         level.playSound(null, entity.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 0.95F, 1.2F);
      }
      return true;
   }

   private static boolean castRuleBreaker(MedeaEntity entity, LivingEntity target, long now) {
      if (!shouldCommitRuleBreakerStab(entity, target)) {
         return false;
      }
      if (!(entity.level() instanceof ServerLevel level) || !MedeaWorkshopHelper.consumeMana(entity, COST_RULE_BREAKER)) {
         return false;
      }
      entity.getPersistentData().putLong(TAG_LAST_RULE_BREAKER_TICK, now);
      entity.setTemporaryFocusItem(MedeaEntity.FocusItem.RULE_BREAKER);
      ServantVoiceHelper.tryPlayRuleBreaker(entity);
      if (entity.distanceToSqr(target) > 9.0 && MedeaWorkshopHelper.canTeleportNow(entity, now)) {
         Vec3 stabPos = findRuleBreakerStabPosition(level, target);
         if (stabPos != null) {
            Vec3 from = entity.position();
            entity.teleportTo(stabPos.x, stabPos.y, stabPos.z);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.faceToward(target.position());
            entity.fallDistance = 0.0F;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.6, from.z, 14, 0.22, 0.3, 0.22, 0.04);
            level.sendParticles(ParticleTypes.END_ROD, stabPos.x, stabPos.y + 0.65, stabPos.z, 10, 0.2, 0.28, 0.2, 0.02);
            MedeaWorkshopHelper.markTeleportUsed(entity, now);
         }
      }
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 14, 0.22, 0.28, 0.22, 0.08);
      level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 18, 0.25, 0.35, 0.25, 0.03);
      applyRuleBreakerHit(target, entity);
      target.hurt(entity.damageSources().mobAttack(entity), 13.0F);
      target.invulnerableTime = 0;
      TYPE_MOON_WORLD.queueServerWork(18, () -> {
         if (entity.isAlive() && entity.getTemporaryFocusItem() == MedeaEntity.FocusItem.RULE_BREAKER) {
            entity.clearTemporaryFocusItem();
         }
      });
      return true;
   }

   private static boolean tryPrepareRuleBreakerStab(MedeaEntity entity, LivingEntity target, long now) {
      if (!ruleBreakerWouldBeEffective(target) || shouldCommitRuleBreakerStab(entity, target)) {
         return false;
      }
      if (canCastFrostShackle(entity, now) && tryCastSpell(entity, now, SpellType.FROST_SHACKLE, () -> castFrostShackle(entity, target, now))) {
         return true;
      }
      return canCastHecateBind(entity, now) && tryCastSpell(entity, now, SpellType.HECATE_BIND, () -> castHecateBind(entity, target, now));
   }

   private static boolean shouldUseRuleBreaker(MedeaEntity entity, LivingEntity target, long now) {
      if (now - entity.getPersistentData().getLong(TAG_LAST_RULE_BREAKER_TICK) < 200L) {
         return false;
      }
      if (entity.getCurrentMp() < MedeaWorkshopHelper.adjustedManaCost(entity, COST_RULE_BREAKER)) {
         return false;
      }
      return ruleBreakerWouldBeEffective(target) && entity.distanceToSqr(target) <= 16.0 * 16.0;
   }

   private static boolean shouldCommitRuleBreakerStab(MedeaEntity entity, LivingEntity target) {
      return target != null
         && target.isAlive()
         && ruleBreakerWouldBeEffective(target)
         && isTargetPinned(target)
         && entity.distanceToSqr(target) <= 16.0 * 16.0;
   }

   private static boolean ruleBreakerWouldBeEffective(LivingEntity target) {
      if (target == null || !target.isAlive()) {
         return false;
      }
      if (target.getPersistentData().getBoolean(MedeaWorkshopHelper.TAG_MAGIC_SUMMON)) {
         return true;
      }
      for (MobEffectInstance effect : target.getActiveEffects()) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
            return true;
         }
      }
      if (target instanceof ServantEntity servant) {
         return servant.getPersistentData().getBoolean(CuChulainnCombatHelper.PROTECTION_FROM_ARROWS_TAG)
            || servant.getPersistentData().contains(CuChulainnCombatHelper.ALGIZ_SHIELD_TAG)
            || servant.getPersistentData().getLong(CuChulainnCombatHelper.GAE_BOLG_WINDUP_UNTIL_TAG) > servant.level().getGameTime()
            || CuChulainnCombatHelper.getActiveRune(servant) != CuChulainnCombatHelper.RuneType.NONE;
      }
      return false;
   }

   private static boolean shouldUseBlinkVolley(MedeaEntity entity, LivingEntity target, double distance, boolean hasLineOfSight) {
      return distance <= 5.5 || !hasLineOfSight || target.hasEffect(MobEffects.MOVEMENT_SPEED);
   }

   private static boolean shouldUseRetreatTeleport(MedeaEntity entity, double distance, boolean hasLineOfSight) {
      return distance <= 3.0 && (entity.getHealth() < entity.getMaxHealth() * 0.6F || !hasLineOfSight);
   }

   private static boolean shouldUseStrafeBarrage(double distance, boolean hasLineOfSight) {
      return hasLineOfSight && distance >= 6.0 && distance <= 15.5;
   }

   private static boolean shouldUseHecateBind(LivingEntity target, double distance, boolean hasLineOfSight) {
      return hasLineOfSight && distance >= 5.0 && distance <= 14.0 && !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
   }

   private static boolean shouldUseFlameBurst(double distance) {
      return distance >= 4.0 && distance <= 14.0;
   }

   private static boolean shouldUseFrostShackle(LivingEntity target, double distance, boolean hasLineOfSight) {
      return hasLineOfSight && distance <= 10.0 && (target.getTicksFrozen() <= 0 || !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN));
   }

   private static boolean shouldUseSuperBolt(double distance, boolean hasLineOfSight) {
      return hasLineOfSight && distance >= 10.0;
   }

   private static boolean isTargetPinned(LivingEntity target) {
      if (target == null || !target.isAlive()) {
         return false;
      }
      MobEffectInstance slowness = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
      if (slowness != null && slowness.getAmplifier() >= 3) {
         return true;
      }
      return target.hasEffect(MobEffects.LEVITATION)
         || target.getTicksFrozen() >= 80
         || slowness != null && target.hasEffect(MobEffects.WEAKNESS);
   }

   private static void maybeUseGoldenFleece(MedeaEntity entity, long now) {
      if (entity.getHealth() >= entity.getMaxHealth() * 0.3F) {
         return;
      }
      if (now - entity.getPersistentData().getLong(TAG_LAST_FLEECE_TICK) < 400L) {
         return;
      }
      entity.getPersistentData().putLong(TAG_LAST_FLEECE_TICK, now);
      entity.setHealth(entity.getMaxHealth());
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 24, 0.4, 0.6, 0.4, 0.05);
         level.playSound(null, entity.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 1.1F);
      }
   }

   private static void consumeUtilityItems(MedeaEntity entity) {
      if (entity.getHealth() < entity.getMaxHealth() * 0.55F && MedeaWorkshopHelper.getHealCharmStock(entity) > 0) {
         MedeaWorkshopHelper.setHealCharmStock(entity, MedeaWorkshopHelper.getHealCharmStock(entity) - 1);
         entity.heal(40.0F);
         if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 18, 0.3, 0.45, 0.3, 0.04);
            level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.HOSTILE, 0.85F, 1.25F);
         }
      }
      if (entity.getCurrentMp() < entity.getMaxMp() * 0.35 && MedeaWorkshopHelper.getManaCharmStock(entity) > 0) {
         MedeaWorkshopHelper.setManaCharmStock(entity, MedeaWorkshopHelper.getManaCharmStock(entity) - 1);
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + 50.0));
         if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 20, 0.35, 0.45, 0.35, 0.03);
            level.sendParticles(CIRCLE_PRIMARY, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 10, 0.12, 0.12, 0.12, 0.0);
         }
      }
   }

   private static void maybeCombatSummon(MedeaEntity entity, long now) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      if (!hasHostileInRange(entity, 18.0)) {
         return;
      }
      if (now - entity.getPersistentData().getLong(TAG_LAST_COMBAT_SUMMON_TICK) < 80L) {
         return;
      }
      if (MedeaWorkshopHelper.getDragonfangStock(entity) <= 0) {
         return;
      }
      int active = MedeaWorkshopHelper.countOwnedDragonfangs(level, entity.getUUID(), entity.position());
      if (active >= 6) {
         return;
      }
      if (entity.getCurrentMp() < 120.0) {
         return;
      }
      if (MedeaWorkshopHelper.summonDragonfang(entity, level)) {
         entity.getPersistentData().putLong(TAG_LAST_COMBAT_SUMMON_TICK, now);
      }
   }

   private static void prepareSpellWindow(MedeaEntity entity, long now) {
      if (entity.getPersistentData().getLong(TAG_SPELL_WINDOW_TICK) != now) {
         entity.getPersistentData().putLong(TAG_SPELL_WINDOW_TICK, now);
         entity.getPersistentData().putInt(TAG_SPELL_WINDOW_COUNT, 0);
         entity.getPersistentData().putString(TAG_SPELL_WINDOW_FIRST_TYPE, "");
      }
   }

   private static boolean canCastSpellNow(MedeaEntity entity, long now, SpellType spell) {
      prepareSpellWindow(entity, now);
      int windowCount = entity.getPersistentData().getInt(TAG_SPELL_WINDOW_COUNT);
      if (windowCount >= 2) {
         return false;
      }
      String firstSpell = entity.getPersistentData().getString(TAG_SPELL_WINDOW_FIRST_TYPE);
      if (windowCount >= 1 && spell.id().equals(firstSpell)) {
         return false;
      }
      String lastSpell = entity.getPersistentData().getString(TAG_LAST_SPELL_TYPE);
      int repeat = entity.getPersistentData().getInt(TAG_SPELL_REPEAT_COUNT);
      return !spell.id().equals(lastSpell) || repeat < 3;
   }

   private static void recordSpellCast(MedeaEntity entity, long now, SpellType spell) {
      prepareSpellWindow(entity, now);
      int windowCount = entity.getPersistentData().getInt(TAG_SPELL_WINDOW_COUNT);
      if (windowCount == 0) {
         entity.getPersistentData().putString(TAG_SPELL_WINDOW_FIRST_TYPE, spell.id());
      }
      entity.getPersistentData().putInt(TAG_SPELL_WINDOW_COUNT, windowCount + 1);
      String lastSpell = entity.getPersistentData().getString(TAG_LAST_SPELL_TYPE);
      int repeat = entity.getPersistentData().getInt(TAG_SPELL_REPEAT_COUNT);
      if (spell.id().equals(lastSpell)) {
         entity.getPersistentData().putInt(TAG_SPELL_REPEAT_COUNT, repeat + 1);
      } else {
         entity.getPersistentData().putString(TAG_LAST_SPELL_TYPE, spell.id());
         entity.getPersistentData().putInt(TAG_SPELL_REPEAT_COUNT, 1);
      }
   }

   private static boolean tryCastSpell(MedeaEntity entity, long now, SpellType spell, SpellAction action) {
      if (!canCastSpellNow(entity, now, spell)) {
         return false;
      }
      if (!action.cast()) {
         return false;
      }
      recordSpellCast(entity, now, spell);
      return true;
   }

   private static void tryFollowUpSpell(MedeaEntity entity, LivingEntity target, long now, boolean hasLineOfSight, double distance) {
      if (target == null || !target.isAlive()) {
         return;
      }
      int offset = entity.getRandom().nextInt(5);
      for (int i = 0; i < 5; i++) {
         switch ((offset + i) % 5) {
            case 0 -> {
               if (entity.isFlyingMode() && canCastMiniBeam(entity, now) && tryCastSpell(entity, now, SpellType.MINI_BEAM, () -> castMiniBeam(entity, target, now))) {
                  return;
               }
            }
            case 1 -> {
               if (canCastOrbitHex(entity, now) && distance <= 16.0 && tryCastSpell(entity, now, SpellType.ORBIT_HEX, () -> castOrbitHex(entity, target, now))) {
                  return;
               }
            }
            case 2 -> {
               if (hasLineOfSight && distance <= 14.0 && canCastLightning(entity, now) && tryCastSpell(entity, now, SpellType.LIGHTNING, () -> castLightning(entity, target, now))) {
                  return;
               }
            }
            case 3 -> {
               if (hasLineOfSight && distance >= 4.0 && canCastCrossfire(entity, now) && tryCastSpell(entity, now, SpellType.CROSSFIRE, () -> castCrossfire(entity, target, now))) {
                  return;
               }
            }
            default -> {
               if (canCastBolt(entity, now) && tryCastSpell(entity, now, SpellType.BOLT, () -> castBolt(entity, target, now))) {
                  return;
               }
            }
         }
      }
   }

   private static boolean tryLargeMagicPriority(MedeaEntity entity, LivingEntity target, long now, double distance, boolean hasLineOfSight, int nearbyEnemyCount) {
      if (target == null || !target.isAlive()) {
         return false;
      }
      if (nearbyEnemyCount >= 3 && canCastThunderstorm(entity, now) && tryCastSpell(entity, now, SpellType.THUNDERSTORM, () -> castThunderstorm(entity, target, now))) {
         return true;
      }
      if (distance >= 6.0 && canCastBeam(entity, now) && tryCastSpell(entity, now, SpellType.BEAM, () -> castBeamVolley(entity, target, now))) {
         return true;
      }
      if (distance >= 8.0 && canCastSuperBolt(entity, now) && tryCastSpell(entity, now, SpellType.SUPER_BOLT, () -> castSuperBolt(entity, target, now))) {
         return true;
      }
      if (hasLineOfSight && canCastHecateBind(entity, now) && tryCastSpell(entity, now, SpellType.HECATE_BIND, () -> castHecateBind(entity, target, now))) {
         return true;
      }
      return hasLineOfSight && canCastStrafeBarrage(entity, now) && tryCastSpell(entity, now, SpellType.STRAFE_BARRAGE, () -> castStrafeBarrage(entity, target, now));
   }

   private static void maybeMassDragonfangRelease(MedeaEntity entity, long now, int nearbyEnemyCount, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      if (now - entity.getPersistentData().getLong(TAG_LAST_MASS_DRAGONFANG_TICK) < 140L) {
         return;
      }
      if (nearbyEnemyCount < 10 && (target == null || (target.getHealth() <= 100.0F && target.getMaxHealth() <= 100.0F))) {
         return;
      }
      int stock = MedeaWorkshopHelper.getDragonfangStock(entity);
      if (stock <= 0) {
         return;
      }
      int active = MedeaWorkshopHelper.countOwnedDragonfangs(level, entity.getUUID(), entity.position());
      int slots = Math.max(0, MedeaWorkshopHelper.MAX_ACTIVE_DRAGONFANG - active);
      if (slots <= 0) {
         return;
      }
      int summonCount = Math.min(stock, slots);
      int released = 0;
      for (int i = 0; i < summonCount; i++) {
         if (!MedeaWorkshopHelper.summonDragonfang(entity, level)) {
            break;
         }
         released++;
      }
      if (released > 0) {
         entity.getPersistentData().putLong(TAG_LAST_MASS_DRAGONFANG_TICK, now);
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + 0.25, entity.getZ(), 24 + released * 2, 1.2, 0.2, 1.2, 0.03);
         level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 0.45, entity.getZ(), 32 + released * 2, 1.4, 0.5, 1.4, 0.04);
         level.playSound(null, entity.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.0F, 0.85F);
      }
   }

   private static int countNearbyHostiles(MedeaEntity entity, LivingEntity target, double radius) {
      Vec3 center = target != null ? target.position() : entity.position();
      AABB box = new AABB(center, center).inflate(radius, 5.0, radius);
      return entity.level().getEntitiesOfClass(
         LivingEntity.class,
         box,
         other -> other != entity
            && other.isAlive()
            && !other.isAlliedTo(entity)
            && !ServantMasterTargeting.isContractMaster(entity, other)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(other)
      ).size();
   }

   private static boolean hasHostileInRange(MedeaEntity entity, double radius) {
      return entity.level().getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(radius, 5.0, radius),
         other -> other != entity
            && other.isAlive()
            && !other.isAlliedTo(entity)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(other)
      ).stream().findAny().isPresent();
   }

   private static boolean tryShowyVarietyCast(MedeaEntity entity, LivingEntity target, long now, double distance, boolean hasLineOfSight) {
      if (target == null || !target.isAlive()) {
         return false;
      }
      int offset = entity.getRandom().nextInt(8);
      for (int i = 0; i < 8; i++) {
         switch ((offset + i) % 8) {
            case 0 -> {
               if (distance >= 4.0 && distance <= 18.0 && canCastOrbitHex(entity, now) && tryCastSpell(entity, now, SpellType.ORBIT_HEX, () -> castOrbitHex(entity, target, now))) {
                  return true;
               }
            }
            case 1 -> {
               if (hasLineOfSight && distance >= 5.0 && distance <= 18.0 && canCastCrossfire(entity, now) && tryCastSpell(entity, now, SpellType.CROSSFIRE, () -> castCrossfire(entity, target, now))) {
                  return true;
               }
            }
            case 2 -> {
               if (distance >= 6.0 && canCastStarfall(entity, now) && tryCastSpell(entity, now, SpellType.STARFALL, () -> castStarfall(entity, target, now))) {
                  return true;
               }
            }
            case 3 -> {
               if (canCastFlameBurst(entity, now) && shouldUseFlameBurst(distance) && tryCastSpell(entity, now, SpellType.FLAME_BURST, () -> castFlameBurst(entity, target, now))) {
                  return true;
               }
            }
            case 4 -> {
               if (canCastFrostShackle(entity, now) && shouldUseFrostShackle(target, distance, hasLineOfSight) && tryCastSpell(entity, now, SpellType.FROST_SHACKLE, () -> castFrostShackle(entity, target, now))) {
                  tryFollowUpSpell(entity, target, now, hasLineOfSight, distance);
                  return true;
               }
            }
            case 5 -> {
               if (hasLineOfSight && canCastHecateBind(entity, now) && tryCastSpell(entity, now, SpellType.HECATE_BIND, () -> castHecateBind(entity, target, now))) {
                  tryFollowUpSpell(entity, target, now, hasLineOfSight, distance);
                  return true;
               }
            }
            case 6 -> {
               if (hasLineOfSight && shouldUseStrafeBarrage(distance, hasLineOfSight) && canCastStrafeBarrage(entity, now) && tryCastSpell(entity, now, SpellType.STRAFE_BARRAGE, () -> castStrafeBarrage(entity, target, now))) {
                  return true;
               }
            }
            default -> {
               if (hasLineOfSight && canCastLightning(entity, now) && tryCastSpell(entity, now, SpellType.LIGHTNING, () -> castLightning(entity, target, now))) {
                  return true;
               }
            }
         }
      }
      return false;
    }

   private static boolean maintainCasterSpacing(MedeaEntity entity, LivingEntity threat, long now, double distance) {
      if (threat == null || !threat.isAlive() || shouldCommitRuleBreakerStab(entity, threat)) {
         return false;
      }
      if (distance > 7.5) {
         return false;
      }
      entity.setFlyingMode(true);
      Vec3 away = entity.position().subtract(threat.position());
      Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      } else {
         horizontal = horizontal.normalize();
      }
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize().scale((entity.tickCount & 1) == 0 ? 1.0 : -1.0);
      Vec3 retreatTarget = entity.position().add(horizontal.scale(distance <= 4.5 ? 4.8 : 3.2)).add(side.scale(1.15)).add(0.0, distance <= 4.5 ? 1.6 : 0.55, 0.0);
      steerTowards(entity, retreatTarget);
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.2, entity.getZ(), 4, 0.15, 0.08, 0.15, 0.02);
      }
      return distance <= 5.8;
   }

   private static Vec3 getAimDirection(MedeaEntity entity, LivingEntity target) {
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(entity.getEyePosition());
      return aim.lengthSqr() < 1.0E-6 ? entity.getLookAngle() : aim.normalize();
   }

   private static Vec3 sidewaysAxis(Vec3 direction) {
      Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
      if (side.lengthSqr() < 1.0E-4) {
         return new Vec3(1.0, 0.0, 0.0);
      }
      return side.normalize();
   }

   private static void faceTargetWhileFlying(MedeaEntity entity, LivingEntity target) {
      if (!entity.isFlyingMode() || target == null || !target.isAlive()) {
         return;
      }
      Vec3 toTarget = target.position().subtract(entity.position());
      double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
      if (horizontal < 1.0E-4) {
         return;
      }

      float targetYaw = (float)(Mth.atan2(toTarget.z, toTarget.x) * 180.0F / Math.PI) - 90.0F;
      float targetPitch = (float)(-(Mth.atan2(toTarget.y, horizontal) * 180.0F / Math.PI));
      entity.setYRot(targetYaw);
      entity.yRotO = targetYaw;
      entity.setYHeadRot(targetYaw);
      entity.yHeadRotO = targetYaw;
      entity.yBodyRot = targetYaw;
      entity.yBodyRotO = targetYaw;
      entity.setXRot(Mth.clamp(targetPitch, -40.0F, 40.0F));
   }

   private static void maintainFlightHeight(MedeaEntity entity, LivingEntity target) {
      if (!entity.isFlyingMode()) {
         return;
      }
      ServantFlightCombatService.markControlled(entity, entity.level().getGameTime());
      double desiredY = ServantFlightHelper.desiredHoverY(entity, target);
      double yMotion = ServantFlightHelper.verticalVelocityToward(entity.getY(), desiredY, 0.12, 0.025, 0.18, 0.24);
      if (yMotion != 0.0) {
         Vec3 motion = entity.getDeltaMovement();
         entity.setDeltaMovement(motion.x, Mth.clamp(motion.y * 0.65 + yMotion, -0.24, 0.22), motion.z);
         entity.hasImpulse = true;
      }
   }

   private static LivingEntity findCloseThreat(MedeaEntity entity, LivingEntity target) {
      if (target != null && target.isAlive() && entity.distanceToSqr(target) <= 4.5 * 4.5) {
         return target;
      }
      LivingEntity hurtBy = entity.getLastHurtByMob();
      if (hurtBy != null && hurtBy.isAlive() && !hurtBy.isAlliedTo(entity)
         && !ServantMasterTargeting.isContractMaster(entity, hurtBy) && entity.distanceToSqr(hurtBy) <= 5.0 * 5.0) {
         return hurtBy;
      }
      return entity.level().getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(4.5),
         other -> other != entity
            && other.isAlive()
            && !other.isAlliedTo(entity)
            && !ServantMasterTargeting.isContractMaster(entity, other)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(other)
      ).stream().findFirst().orElse(null);
   }

   private static void maybeTriggerBasicCast(MedeaEntity entity) {
      if (!entity.isPerformingAction()) {
         entity.triggerBasicAttackAnimation();
      }
   }

   private static void spawnBoltProjectile(
      ServerLevel level, MedeaEntity entity, Vec3 spawnPos, Vec3 direction, MedeaMagicBoltEntity.Mode mode, float damage, float speed, float inaccuracy
   ) {
      MedeaMagicBoltEntity projectile = new MedeaMagicBoltEntity(level, entity);
      projectile.setMode(mode);
      projectile.setMagicDamage(damage);
      projectile.setPos(spawnPos);
      projectile.shoot(direction.x, direction.y, direction.z, speed, inaccuracy);
      level.addFreshEntity(projectile);
      VFXServerEffects.spawn(level, mode == MedeaMagicBoltEntity.Mode.SUPER_BOLT ? "medea_super_magic_orb" : "medea_magic_orb", spawnPos, 80.0);
   }

   private static void fractureTerrain(ServerLevel level, MedeaEntity entity, Vec3 center, double radius, double yScale, float maxHardness, int maxBroken) {
      BlockPos origin = BlockPos.containing(center);
      int blockRadius = Math.max(1, (int)Math.ceil(radius));
      int broken = 0;
      double radiusSqr = radius * radius;
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-blockRadius, -blockRadius, -blockRadius), origin.offset(blockRadius, blockRadius, blockRadius))) {
         if (broken >= maxBroken) {
            break;
         }
         double dx = pos.getX() + 0.5 - center.x;
         double dy = (pos.getY() + 0.5 - center.y) / Math.max(0.25, yScale);
         double dz = pos.getZ() + 0.5 - center.z;
         if (dx * dx + dy * dy + dz * dz > radiusSqr) {
            continue;
         }
         if (entity != null && pos.closerToCenterThan(entity.position(), 1.0)) {
            continue;
         }
         BlockState state = level.getBlockState(pos);
         float hardness = state.getDestroySpeed(level, pos);
         if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F || hardness >= maxHardness) {
            continue;
         }
         if (level.removeBlock(pos, false)) {
            broken++;
         }
      }
      if (broken > 0) {
         level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, Math.min(28, 6 + broken / 2), radius * 0.28, 0.2, radius * 0.28, 0.045);
      }
   }

   private static void beginStaffCast(MedeaEntity entity) {
      entity.setTemporaryFocusItem(MedeaEntity.FocusItem.HECATES_STAFF);
   }

   private static void queueStaffClear(MedeaEntity entity, int delay) {
      TYPE_MOON_WORLD.queueServerWork(delay, () -> {
         if (entity.isAlive() && entity.getTemporaryFocusItem() == MedeaEntity.FocusItem.HECATES_STAFF) {
            entity.clearTemporaryFocusItem();
         }
      });
   }

   private static void teleportEntity(MedeaEntity entity, BlockPos destination, long now) {
      entity.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
      entity.setDeltaMovement(Vec3.ZERO);
      entity.fallDistance = 0.0F;
      entity.setFlyingMode(false);
      MedeaWorkshopHelper.markTeleportUsed(entity, now);
   }

   private static void spawnLightningBolts(ServerLevel level, Vec3 center, int count) {
      for (int i = 0; i < count; i++) {
         LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
         if (lightning == null) {
            continue;
         }
         double angle = count <= 1 ? 0.0 : (Math.PI * 2.0 * i) / count;
         double radius = i == 0 ? 0.0 : 1.4;
         lightning.moveTo(center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius);
         lightning.setVisualOnly(i != 0);
         level.addFreshEntity(lightning);
      }
   }

   private static LivingEntity findEmergencyTarget(MedeaEntity entity) {
      return entity.level().getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(20.0),
         candidate -> candidate != entity
            && candidate.isAlive()
            && !candidate.isAlliedTo(entity)
            && !ServantMasterTargeting.isContractMaster(entity, candidate)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(candidate)
      ).stream().findFirst().orElse(null);
   }

   private static LineOfFireState evaluateLineOfFire(MedeaEntity entity, LivingEntity target) {
      Vec3 start = entity.getEyePosition();
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      HitResult hit = entity.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
      return new LineOfFireState(hit.getType() == HitResult.Type.BLOCK, end, hit.getLocation());
   }

   private static boolean handleBlockedLineOfFire(MedeaEntity entity, LivingEntity target, long now, double distance, LineOfFireState lineOfFire) {
      long blockedStart = entity.getPersistentData().getLong(TAG_BLOCKED_LINE_START_TICK);
      if (blockedStart <= 0L) {
         entity.getPersistentData().putLong(TAG_BLOCKED_LINE_START_TICK, now);
         blockedStart = now;
      }

      long blockedTicks = now - blockedStart;
      boolean repositioning = attemptObstacleReposition(entity, target, lineOfFire.targetPoint());
      if (blockedTicks >= 20L && canCastBeam(entity, now) && tryCastSpell(entity, now, SpellType.BEAM, () -> castBeamVolley(entity, target, now, true, true))) {
         entity.getNavigation().stop();
         clearBlockedLineState(entity);
         return true;
      }

      if (!entity.isFlyingMode()) {
         ServantNavigationHelper.moveToTargetThrottled(
            entity,
            target,
            1.15,
            now,
            ServantNavigationHelper.DEFAULT_REPATH_INTERVAL,
            0.8,
            "MedeaBlockedLinePath"
         );
      }
      return blockedTicks >= 2L || repositioning;
   }

   private static boolean handleUndergroundTarget(MedeaEntity entity, LivingEntity target, long now, boolean hasLineOfSight) {
      double verticalDrop = entity.getY() - target.getY();
      boolean undergroundTarget = !hasLineOfSight && verticalDrop >= 3.5;
      if (!undergroundTarget) {
         entity.getPersistentData().remove(TAG_UNDERGROUND_TARGET_TICK);
         return false;
      }

      long startTick = entity.getPersistentData().getLong(TAG_UNDERGROUND_TARGET_TICK);
      if (startTick <= 0L) {
         entity.getPersistentData().putLong(TAG_UNDERGROUND_TARGET_TICK, now);
         startTick = now;
      }

      if (canCastBeam(entity, now) && tryCastSpell(entity, now, SpellType.BEAM, () -> castBeamVolley(entity, target, now, true, true))) {
         return true;
      }
      if (MedeaWorkshopHelper.canTeleportNow(entity, now) && tryCastSpell(entity, now, SpellType.TELEPORT, () -> castTeleport(entity, target, now))) {
         return true;
      }
      if (now - startTick >= 80L) {
         entity.setTarget(null);
         entity.getNavigation().stop();
         entity.getPersistentData().remove(TAG_UNDERGROUND_TARGET_TICK);
         return true;
      }
      return false;
   }

   private static void clearBlockedLineState(MedeaEntity entity) {
      entity.getPersistentData().remove(TAG_BLOCKED_LINE_START_TICK);
   }

   private static boolean attemptObstacleReposition(MedeaEntity entity, LivingEntity target, Vec3 targetPoint) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return false;
      }

      Vec3 toTarget = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return false;
      }

      entity.setFlyingMode(true);
      entity.getNavigation().stop();
      Vec3 forward = horizontal.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x).normalize();
      Vec3[] offsets = new Vec3[]{
         new Vec3(0.0, 1.4, 0.0),
         new Vec3(0.0, -1.0, 0.0),
         side.scale(1.45).add(0.0, 0.85, 0.0),
         side.scale(-1.45).add(0.0, 0.85, 0.0),
         side.scale(1.85),
         side.scale(-1.85),
         forward.scale(1.2).add(side.scale(1.2)).add(0.0, 0.55, 0.0),
         forward.scale(1.2).add(side.scale(-1.2)).add(0.0, 0.55, 0.0)
      };

      Vec3 currentEye = entity.getEyePosition();
      Vec3 bestCandidate = null;
      for (Vec3 offset : offsets) {
         Vec3 candidate = entity.position().add(offset);
         if (!canOccupyPosition(level, entity, candidate)) {
            continue;
         }
         Vec3 candidateEye = currentEye.add(offset);
         if (isLineClear(level, entity, candidateEye, targetPoint)) {
            bestCandidate = candidate;
            break;
         }
      }

      if (bestCandidate == null) {
         Vec3 climb = chooseVerticalEscape(level, entity, currentEye, targetPoint);
         if (climb != null) {
            bestCandidate = entity.position().add(climb);
         }
      }

      if (bestCandidate == null) {
         return false;
      }

      bestCandidate = new Vec3(bestCandidate.x, ServantFlightHelper.clampFlyingY(entity, bestCandidate.y, target), bestCandidate.z);
      steerTowards(entity, bestCandidate);
      return true;
   }

   private static Vec3 chooseVerticalEscape(ServerLevel level, MedeaEntity entity, Vec3 currentEye, Vec3 targetPoint) {
      Vec3 upOffset = new Vec3(0.0, 1.2, 0.0);
      Vec3 downOffset = new Vec3(0.0, -0.9, 0.0);
      boolean canGoUp = canOccupyPosition(level, entity, entity.position().add(upOffset)) && isLineClear(level, entity, currentEye.add(upOffset), targetPoint);
      boolean canGoDown = canOccupyPosition(level, entity, entity.position().add(downOffset)) && isLineClear(level, entity, currentEye.add(downOffset), targetPoint);
      if (canGoUp) {
         return upOffset;
      }
      if (canGoDown) {
         return downOffset;
      }
      return canOccupyPosition(level, entity, entity.position().add(upOffset)) ? upOffset : null;
   }

   private static boolean canOccupyPosition(ServerLevel level, MedeaEntity entity, Vec3 candidate) {
      Vec3 move = candidate.subtract(entity.position());
      return level.noCollision(entity, entity.getBoundingBox().move(move));
   }

   private static boolean isLineClear(ServerLevel level, MedeaEntity entity, Vec3 start, Vec3 end) {
      HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
      return hit.getType() != HitResult.Type.BLOCK;
   }

   private static void steerTowards(MedeaEntity entity, Vec3 destination) {
      Vec3 delta = destination.subtract(entity.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      Vec3 horizontalMove = horizontal.lengthSqr() < 1.0E-4 ? Vec3.ZERO : horizontal.normalize().scale(0.35);
      double vertical = Math.max(-0.32, Math.min(0.32, delta.y * 0.45));
      Vec3 velocity = entity.getDeltaMovement().scale(0.4).add(horizontalMove.x, vertical, horizontalMove.z);
      entity.faceVector(velocity);
      entity.setDeltaMovement(velocity.x, velocity.y, velocity.z);
      entity.hasImpulse = true;
      entity.getMoveControl().setWantedPosition(destination.x, destination.y, destination.z, 1.05);
   }

   private static Vec3 findBlinkDestination(ServerLevel level, LivingEntity target) {
      Vec3 look = target.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      } else {
         horizontal = horizontal.normalize();
      }
      Vec3 right = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize();
      Vec3 base = target.position();
      Vec3[] candidates = new Vec3[]{
         base.subtract(horizontal.scale(2.6)).add(right.scale(1.2)),
         base.subtract(horizontal.scale(2.6)).subtract(right.scale(1.2)),
         base.add(right.scale(2.25)),
         base.subtract(right.scale(2.25)),
         base.subtract(horizontal.scale(2.1))
      };

      for (Vec3 candidate : candidates) {
         BlockPos feet = findSafeBlinkFeet(level, BlockPos.containing(candidate.x, target.getY(), candidate.z));
         if (feet != null) {
            return new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
         }
      }
      return null;
   }

   private static Vec3 findRuleBreakerStabPosition(ServerLevel level, LivingEntity target) {
      Vec3 look = target.getLookAngle();
      Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      } else {
         horizontal = horizontal.normalize();
      }
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize();
      Vec3 base = target.position();
      Vec3[] candidates = new Vec3[]{
         base.subtract(horizontal.scale(1.25)),
         base.subtract(horizontal.scale(0.8)).add(side.scale(0.95)),
         base.subtract(horizontal.scale(0.8)).add(side.scale(-0.95))
      };
      for (Vec3 candidate : candidates) {
         BlockPos feet = findSafeBlinkFeet(level, BlockPos.containing(candidate.x, target.getY(), candidate.z));
         if (feet != null) {
            return new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
         }
      }
      return null;
   }

   private static Vec3 variedBeamOrigin(MedeaEntity entity, LivingEntity target, int shotIndex, int shotCount) {
      Vec3 direction = getAimDirection(entity, target);
      Vec3 side = sidewaysAxis(direction);
      Vec3 back = direction.scale(-1.0);
      double vertical = 0.0;
      Vec3 offset;
      if (shotCount <= 1) {
         offset = back.scale(0.8);
      } else {
         int mode = shotIndex % 3;
         if (mode == 0) {
            offset = side.scale(-0.95).add(back.scale(0.45));
         } else if (mode == 1) {
            offset = side.scale(0.95).add(back.scale(0.45));
         } else {
            offset = back.scale(1.1);
            vertical = 0.22;
         }
      }
      return entity.getEyePosition().add(offset).add(0.0, vertical, 0.0);
   }

   private static BlockPos findSafeBlinkFeet(ServerLevel level, BlockPos anchor) {
      int x = anchor.getX();
      int z = anchor.getZ();
      int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      for (int y = Math.max(anchor.getY() - 2, surfaceY - 2); y <= surfaceY + 2; y++) {
         BlockPos feet = new BlockPos(x, y, z);
         if (isTeleportSpotClear(level, feet)) {
            return feet;
         }
      }
      return null;
   }

   private static boolean isTeleportSpotClear(ServerLevel level, BlockPos feet) {
      BlockPos below = feet.below();
      return level.getBlockState(below).isSolidRender(level, below)
         && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
         && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
   }

   private static void spawnMagicCircle(ServerLevel level, Vec3 center, Vec3 direction, float scale) {
      Vec3 facing = new Vec3(direction.x, 0.0, direction.z);
      if (facing.lengthSqr() < 1.0E-6) {
         facing = new Vec3(0.0, 0.0, 1.0);
      } else {
         facing = facing.normalize();
      }
      Vec3 right = new Vec3(-facing.z, 0.0, facing.x).normalize();
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      List<Vec3> vertices = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
         double angle = -Math.PI / 2.0 + i * (Math.PI * 2.0 / 5.0);
         vertices.add(center.add(right.scale(Math.cos(angle) * scale)).add(up.scale(Math.sin(angle) * scale)));
      }
      drawRing(level, center, right, up, scale * 1.1, 24, CIRCLE_PRIMARY);
      drawStar(level, vertices, CIRCLE_ACCENT);
   }

   private static void drawRing(ServerLevel level, Vec3 center, Vec3 right, Vec3 forward, double radius, int samples, DustParticleOptions particle) {
      for (int i = 0; i < samples; i++) {
         double angle = i * (Math.PI * 2.0 / samples);
         Vec3 pos = center.add(right.scale(Math.cos(angle) * radius)).add(forward.scale(Math.sin(angle) * radius));
         level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void drawStar(ServerLevel level, List<Vec3> vertices, DustParticleOptions particle) {
      int[][] edges = new int[][]{{0, 2}, {2, 4}, {4, 1}, {1, 3}, {3, 0}};
      for (int[] edge : edges) {
         Vec3 start = vertices.get(edge[0]);
         Vec3 end = vertices.get(edge[1]);
         for (double t = 0.0; t <= 1.0; t += 0.1) {
            Vec3 pos = start.lerp(end, t);
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void purgeOwnedSummons(LivingEntity target) {
      if (!(target.level() instanceof ServerLevel serverLevel)) {
         return;
      }
      String ownerId = target.getUUID().toString();
      AABB purgeBox = target.getBoundingBox().inflate(192.0);
      for (DragonfangSoldierEntity summon : serverLevel.getEntitiesOfClass(
         DragonfangSoldierEntity.class,
         purgeBox,
         dragonfang -> dragonfang.isAlive() && ownerId.equals(dragonfang.getPersistentData().getString(MedeaWorkshopHelper.TAG_MAGIC_SUMMON_OWNER))
      )) {
         summon.discard();
      }
   }

   private record LineOfFireState(boolean blocked, Vec3 targetPoint, Vec3 hitPoint) {
   }
}
