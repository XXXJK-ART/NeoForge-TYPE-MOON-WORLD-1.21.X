package net.xxxjk.TYPE_MOON_WORLD.servant.artoria;

import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackOrigin;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroActionAnimationService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.CombatTerrainProtection;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.typemoonworld.api.event.TerrainImpactBlockEvent;

/** Identical accepted skill effects for the player card and NPC. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class ArtoriaAbilityService {
   public static final String COUNTER_UNTIL = "ArtoriaGuardCounterUntil";
   public static final String COUNTER_FACING_X = "ArtoriaGuardCounterFacingX";
   public static final String COUNTER_FACING_Z = "ArtoriaGuardCounterFacingZ";
   public static final String ACTION_LOCK_UNTIL = "ArtoriaSwordActionLockUntil";
   private static final String NPC_SKILL_READY_PREFIX = "ArtoriaSkillReady:";
   private ArtoriaAbilityService() { }
   public static boolean enemy(LivingEntity owner, LivingEntity target) {
      return target != null && owner != target && target.isAlive() && owner.level() == target.level()
         && !owner.isAlliedTo(target) && !target.isAlliedTo(owner) && !EntityUtils.isImmunePlayerTarget(target)
         && !ServantMasterProtection.isProtectedMaster(owner, target);
   }
   public static Vec3 horizontal(LivingEntity owner) {
      Vec3 look = owner.getLookAngle().multiply(1, 0, 1);
      return look.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, owner.getYRot()) : look.normalize();
   }
   public static LivingEntity comboTarget(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return null;
      Vec3 forward = horizontal(owner);
      return level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(16, 4, 16),
         t -> enemy(owner, t) && owner.distanceToSqr(t) <= 256 && owner.hasLineOfSight(t)
            && t.position().subtract(owner.position()).normalize().dot(forward) > 0.45)
         .stream().min(java.util.Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
   }
   public static boolean canWind(LivingEntity owner) {
      return ArtoriaStateService.isWindBarrierEnabled(owner)
         && !ArtoriaStateService.isWindDefenseActive(owner)
         && !ArtoriaStateService.busy(owner) && !ArtoriaPendragonCombatHelper.isExcaliburVisible(owner)
         && !actionLocked(owner)
         && owner.level().getGameTime() >= owner.getPersistentData().getLong(ArtoriaStateService.WIND_READY);
   }
   public static void reveal(LivingEntity owner, int ticks) {
      long until = owner.level().getGameTime() + ticks;
      owner.getPersistentData().putLong(ArtoriaPendragonCombatHelper.TAG_EXCALIBUR_VISIBLE_UNTIL, until);
      owner.getPersistentData().putLong(ArtoriaPendragonCombatHelper.TAG_WIND_REGATHER_UNTIL, until);
      if (owner instanceof ServerPlayer player) PlayerNoblePhantasmHelper.revealArtoriaWindVeiledExcalibur(player, ticks);
      if (owner instanceof ArtoriaPendragonEntity npc) ArtoriaPendragonCombatHelper.syncExcaliburVisibility(npc);
   }
   private static DamageSource source(LivingEntity owner, String skill, boolean magic) {
      DamageSource base = magic ? owner.damageSources().indirectMagic(owner, owner)
         : owner instanceof ServerPlayer p ? owner.damageSources().playerAttack(p) : owner.damageSources().mobAttack(owner);
      return new CombatDamageSource(base, AttackOrigin.skill(ArtoriaRules.ID, skill));
   }
   private static boolean hit(LivingEntity owner, LivingEntity target, float damage, String skill, boolean magic, boolean segment) {
      if (!enemy(owner, target)) return false;
      int previous = target.invulnerableTime;
      try {
         if (segment) target.invulnerableTime = 0;
         return target.hurt(source(owner, skill, magic), damage);
      } finally {
         if (segment) target.invulnerableTime = Math.max(previous, target.invulnerableTime);
      }
   }
   public static boolean isTargetedSwordSkill(String skill) {
      return ArtoriaRules.KNIGHT_TRIPLE_SLASH.equals(skill) || ArtoriaRules.INTERCEPT_THRUST.equals(skill)
         || ArtoriaRules.SPINNING_SLASH.equals(skill) || ArtoriaRules.UPWARD_SLASH.equals(skill)
         || ArtoriaRules.KNIGHT_HEAVY_SLASH.equals(skill);
   }
   public static LivingEntity nearestForwardTarget(LivingEntity owner, double range, double minimumDot) {
      return nearestInDirection(owner, range, minimumDot, horizontal(owner));
   }
   private static LivingEntity nearestInDirection(LivingEntity owner, double range, double minimumDot, Vec3 direction) {
      if (!(owner.level() instanceof ServerLevel level)) return null;
      Vec3 forward = direction.multiply(1, 0, 1).normalize();
      return level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(range, 2, range), target ->
         enemy(owner, target) && owner.distanceTo(target) <= range && owner.hasLineOfSight(target)
            && target.position().subtract(owner.position()).multiply(1, 0, 1).normalize().dot(forward) >= minimumDot)
         .stream().min(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
   }
   public static boolean canPerformSwordSkill(LivingEntity owner, String skill) {
      if (owner == null || !ArtoriaStateService.isArtoria(owner) || ArtoriaStateService.busy(owner)
         || actionLocked(owner)) return false;
      return switch (skill) {
         case ArtoriaRules.KNIGHT_TRIPLE_SLASH, ArtoriaRules.INTERCEPT_THRUST,
            ArtoriaRules.SPINNING_SLASH, ArtoriaRules.UPWARD_SLASH, ArtoriaRules.KNIGHT_HEAVY_SLASH -> true;
         case ArtoriaRules.GUARD_COUNTER -> true;
         default -> false;
      };
   }
   public static boolean performSwordSkill(LivingEntity owner, String skill) {
      if (!canPerformSwordSkill(owner, skill) || !(owner.level() instanceof ServerLevel level)) return false;
      return switch (skill) {
         case ArtoriaRules.KNIGHT_TRIPLE_SLASH -> startTripleSlash(owner, level);
         case ArtoriaRules.INTERCEPT_THRUST -> startInterceptThrust(owner, level);
         case ArtoriaRules.SPINNING_SLASH -> startSpinningSlash(owner, level);
         case ArtoriaRules.UPWARD_SLASH -> startUpwardSlash(owner, level);
         case ArtoriaRules.GUARD_COUNTER -> startGuardCounter(owner, level);
         case ArtoriaRules.KNIGHT_HEAVY_SLASH -> startHeavySlash(owner, level);
         default -> false;
      };
   }
   public static boolean tryNpcSkill(LivingEntity owner, String skill) {
      if (!(owner instanceof ArtoriaPendragonEntity npc) || !ArtoriaStateService.isArtoria(owner) || !(owner.level() instanceof ServerLevel)
         || ArtoriaStateService.busy(owner) || (!isTargetedSwordSkill(skill) && !ArtoriaRules.GUARD_COUNTER.equals(skill))) return false;
      long now = owner.level().getGameTime();
      String readyKey = NPC_SKILL_READY_PREFIX + skill;
      if (now < owner.getPersistentData().getLong(readyKey) || !canPerformSwordSkill(owner, skill)) return false;
      LivingEntity target = npc.getTarget();
      if (enemy(owner, target)) {
         npc.faceToward(target.position());
         npc.getNavigation().stop();
      }
      double mp = ArtoriaRules.skillMpCost(skill);
      if (!ArtoriaStateService.consume(owner, mp)) return false;
      if (!performSwordSkill(owner, skill)) {
         npc.setCurrentMp(Math.min(npc.getMaxMp(), npc.getCurrentMp() + mp));
         return false;
      }
      ServantVoiceHelper.tryPlayArtoriaSkill(npc);
      owner.getPersistentData().putLong(readyKey, now + ArtoriaRules.skillCooldown(skill));
      return true;
   }
   public static boolean actionLocked(LivingEntity owner) {
      return owner != null && owner.level().getGameTime() < owner.getPersistentData().getLong(ACTION_LOCK_UNTIL);
   }
   private static void lockAction(LivingEntity owner, int ticks) {
      if (owner instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().stop();
      owner.getPersistentData().putLong(ACTION_LOCK_UNTIL, owner.level().getGameTime() + ticks);
   }
   public static boolean tryNpcLightCannon(LivingEntity owner) {
      if (!(owner instanceof ArtoriaPendragonEntity npc) || !(owner.level() instanceof ServerLevel) || !ArtoriaStateService.isArtoria(owner)
         || ArtoriaStateService.busy(owner)) return false;
      long now = owner.level().getGameTime();
      String readyKey = NPC_SKILL_READY_PREFIX + ArtoriaRules.EXCALIBUR_SHORT_SKILL;
      if (now < owner.getPersistentData().getLong(readyKey)
         || now < owner.getPersistentData().getLong(ArtoriaStateService.NP_SHORT_READY)
         || npc.getCurrentMp() < ArtoriaRules.EXCALIBUR_SHORT_MP) return false;
      if (!ArtoriaRules.npcMayUseExcalibur(ServantCombatSystem.getPhase(npc), npc.getRandom().nextFloat())) return false;
      if (!ArtoriaStateService.consume(owner, ArtoriaRules.EXCALIBUR_SHORT_MP)) return false;
      NoblePhantasmCastService.noteNpcPayment(npc, ArtoriaRules.EXCALIBUR_SHORT_MP);
      owner.getPersistentData().putLong(readyKey, now + ArtoriaRules.EXCALIBUR_SHORT_COOLDOWN);
      owner.getPersistentData().putLong(ArtoriaStateService.NP_SHORT_READY, now + ArtoriaRules.EXCALIBUR_SHORT_COOLDOWN);
      ArtoriaExcaliburAnimationService.play(owner, ArtoriaExcaliburAction.SHORT_RELEASE);
      NoblePhantasmCastService.begin(owner, ArtoriaRules.EXCALIBUR_SHORT_SKILL, ArtoriaRules.EXCALIBUR_SHORT_COOLDOWN,
         ticks -> owner.getPersistentData().putLong(ArtoriaStateService.NP_SHORT_READY, owner.level().getGameTime() + ticks),
         () -> owner.getPersistentData().remove(ArtoriaStateService.NP_BEAM));
      ArtoriaStateService.autoReleaseWindBarrier(owner);
      TYPE_MOON_WORLD.queueServerWork(20, () -> {
         if (NoblePhantasmCastService.chanting(owner)) ArtoriaStateService.autoReleaseWindBarrier(owner);
      });
      npc.triggerChargeAnimation(ArtoriaRules.EXCALIBUR_SHORT_CHARGE_TICKS);
      NoblePhantasmCastService.deferEffect(owner, () -> spawnShortExcalibur(owner));
      return true;
   }

   private static void spawnShortExcalibur(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive()) return;
      Vec3 start = owner.position().add(0, owner.getBbHeight() * .66, 0).add(owner.getLookAngle().scale(1.2));
      var beam = new net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity(level, owner, start,
         ArtoriaRules.EXCALIBUR_SHORT_TICKS, 0, 1.0F, false, true);
      owner.getPersistentData().putUUID(ArtoriaStateService.NP_BEAM, beam.getUUID());
      level.addFreshEntity(beam);
      reveal(owner, ArtoriaRules.EXCALIBUR_SHORT_TICKS + ArtoriaRules.REVEAL_AFTER_NP);
      VFXServerEffects.spawn(level, "artoria_excalibur_beam", owner, ArtoriaRules.EXCALIBUR_SHORT_TICKS);
      ServantVoiceHelper.tryPlayArtoriaExcaliburRelease((ArtoriaPendragonEntity) owner);
      level.playSound(null, owner.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4F, 1.15F);
   }
   private static boolean startTripleSlash(LivingEntity owner, ServerLevel level) {
      lockAction(owner, 16);
      animate(owner, SasakiKojiroAction.VERTICAL_SLASH);
      skillStartFx(owner, level);
      TYPE_MOON_WORLD.queueServerWork(4, () -> strikeIfValid(owner, level,
         ArtoriaRules.TRIPLE_SLASH_FIRST_DAMAGE, ArtoriaRules.KNIGHT_TRIPLE_SLASH, true, 5.0));
      TYPE_MOON_WORLD.queueServerWork(8, () -> {
         if (validDelayed(owner, level)) animate(owner, SasakiKojiroAction.HORIZONTAL_SLASH);
          strikeIfValid(owner, level, ArtoriaRules.TRIPLE_SLASH_SECOND_DAMAGE, ArtoriaRules.KNIGHT_TRIPLE_SLASH, true, 5.0);
      });
      TYPE_MOON_WORLD.queueServerWork(12, () -> {
         if (validDelayed(owner, level)) animate(owner, SasakiKojiroAction.THRUST);
          strikeIfValid(owner, level, ArtoriaRules.TRIPLE_SLASH_FINAL_DAMAGE, ArtoriaRules.KNIGHT_TRIPLE_SLASH, true, 5.0);
      });
      return true;
   }
   private static void strikeIfValid(LivingEntity owner, ServerLevel level,
                                     float damage, String skill, boolean segment, double range) {
      if (!validDelayed(owner, level)) return;
      LivingEntity target = nearestForwardTarget(owner, range, 0.35);
      if (target == null) return;
      hit(owner, target, damage, skill, false, segment);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5,
         target.getZ(), 1, 0.2, 0.25, 0.2, 0.0);
   }
   private static boolean startInterceptThrust(LivingEntity owner, ServerLevel level) {
      lockAction(owner, 10);
      Vec3 forward = horizontal(owner);
      animate(owner, SasakiKojiroAction.THRUST);
      lunge(owner, forward, 1.2);
      skillStartFx(owner, level);
      TYPE_MOON_WORLD.queueServerWork(5, () -> strikeFixedDirection(owner, level, forward,
         ArtoriaRules.INTERCEPT_THRUST_DAMAGE, ArtoriaRules.INTERCEPT_THRUST, 5.8));
      return true;
   }
   private static void lunge(LivingEntity owner, Vec3 direction, double distance) {
      Vec3 horizontal = direction.multiply(1, 0, 1).normalize();
      double safeDistance = 0;
      for (double step = 0.15; step <= distance + 1.0E-6; step += 0.15) {
         AABB moved = owner.getBoundingBox().move(horizontal.scale(step));
         if (!owner.level().noCollision(owner, moved)) break;
         safeDistance = step;
      }
      if (safeDistance > 0) {
         owner.setDeltaMovement(owner.getDeltaMovement().add(horizontal.scale(Math.min(0.72, safeDistance * 0.6))));
         owner.hurtMarked = true;
      }
   }
   private static void strikeFixedDirection(LivingEntity owner, ServerLevel level, Vec3 direction,
                                            float damage, String skill, double range) {
      if (!validDelayed(owner, level)) return;
      LivingEntity target = nearestInDirection(owner, range, 0.35, direction);
      if (target == null) return;
      hit(owner, target, damage, skill, false, false);
      level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5,
         target.getZ(), 8, 0.25, 0.3, 0.25, 0.1);
   }
   private static boolean startSpinningSlash(LivingEntity owner, ServerLevel level) {
      lockAction(owner, 8);
      Vec3 forward = horizontal(owner);
      animate(owner, SasakiKojiroAction.SPINNING_SLASH);
      skillStartFx(owner, level);
      TYPE_MOON_WORLD.queueServerWork(4, () -> {
         if (!validDelayed(owner, level)) return;
         Set<Integer> hitIds = new HashSet<>();
         for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            owner.getBoundingBox().inflate(4.8, 2, 4.8), t -> enemy(owner, t))) {
            Vec3 offset = target.position().subtract(owner.position());
            if (owner.distanceTo(target) <= 4.8 && offset.multiply(1, 0, 1).normalize().dot(forward) >= 0.5
               && owner.hasLineOfSight(target) && hitIds.add(target.getId()))
               hit(owner, target, ArtoriaRules.SPINNING_SLASH_DAMAGE, ArtoriaRules.SPINNING_SLASH, false, false);
         }
      });
      return true;
   }
   private static boolean startUpwardSlash(LivingEntity owner, ServerLevel level) {
      lockAction(owner, 6);
      animate(owner, SasakiKojiroAction.UPWARD_SLASH);
      skillStartFx(owner, level);
      TYPE_MOON_WORLD.queueServerWork(3, () -> {
         if (!validDelayed(owner, level)) return;
         LivingEntity target = nearestForwardTarget(owner, 4.8, 0.35);
         if (target == null) return;
         if (hit(owner, target, ArtoriaRules.UPWARD_SLASH_DAMAGE, ArtoriaRules.UPWARD_SLASH, false, false)) {
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x, Math.max(0.22, motion.y), motion.z);
            target.hurtMarked = true;
         }
      });
      return true;
   }
   private static boolean startGuardCounter(LivingEntity owner, ServerLevel level) {
      Vec3 facing = horizontal(owner);
      owner.getPersistentData().putLong(COUNTER_UNTIL, level.getGameTime() + 6);
      owner.getPersistentData().putFloat(COUNTER_FACING_X, (float)facing.x);
      owner.getPersistentData().putFloat(COUNTER_FACING_Z, (float)facing.z);
      lockAction(owner, 6);
      animate(owner, SasakiKojiroAction.COUNTER_STANCE);
      level.playSound(null, owner.blockPosition(), SoundEvents.SHIELD_BLOCK, owner instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, 0.8F, 1.35F);
      return true;
   }
   private static boolean startHeavySlash(LivingEntity owner, ServerLevel level) {
      lockAction(owner, 9);
      Vec3 lockedDirection = horizontal(owner);
      animate(owner, SasakiKojiroAction.DOWNWARD_SLASH);
      skillStartFx(owner, level);
      TYPE_MOON_WORLD.queueServerWork(6, () -> strikeFixedDirection(owner, level, lockedDirection,
         ArtoriaRules.KNIGHT_HEAVY_SLASH_DAMAGE, ArtoriaRules.KNIGHT_HEAVY_SLASH, 5.2));
      return true;
   }
   private static void animate(LivingEntity owner, SasakiKojiroAction action) {
      SasakiKojiroActionAnimationService.play(owner, action);
      if (owner instanceof ServerPlayer player) player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
      else if (owner instanceof ArtoriaPendragonEntity artoria) artoria.triggerAttackSwing();
      if (action != SasakiKojiroAction.COUNTER_STANCE && owner.level() instanceof ServerLevel level) {
         spawnSwordTrail(owner, level);
      }
   }
   private static void spawnSwordTrail(LivingEntity owner, ServerLevel level) {
      String effect = ArtoriaPendragonCombatHelper.isManaBurstActive(owner)
         && !ArtoriaStateService.isWindBarrierActive(owner)
         && !ArtoriaStateService.isWindDefenseActive(owner)
         ? "artoria_sword_trail_gold" : "artoria_sword_trail_wind";
      VFXServerEffects.spawn(level, effect, owner, 72);
   }
   private static void skillStartFx(LivingEntity owner, ServerLevel level) {
      level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
         owner instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, 0.75F, 1.5F);
      level.sendParticles(ParticleTypes.CRIT, owner.getX(), owner.getY() + owner.getBbHeight() * 0.6,
         owner.getZ(), 5, 0.25, 0.25, 0.25, 0.02);
   }
   @SubscribeEvent
   public static void onCounterDamage(LivingIncomingDamageEvent event) {
      if (!(event.getEntity() instanceof LivingEntity defender) || defender.level().isClientSide()
         || !ArtoriaStateService.isArtoria(defender)) return;
      var data = defender.getPersistentData();
      long now = defender.level().getGameTime();
      if (now >= data.getLong(COUNTER_UNTIL)) { data.remove(COUNTER_UNTIL); return; }
      DamageSource incoming = event.getSource();
      Entity attackerEntity = incoming.getEntity();
      if (event.getAmount() <= 0 || !(attackerEntity instanceof LivingEntity attacker) || incoming.getDirectEntity() != attacker
         || incoming.is(DamageTypeTags.IS_PROJECTILE) || incoming.is(DamageTypeTags.IS_EXPLOSION)
         || incoming.is(DamageTypeTags.IS_FIRE) || incoming.is(DamageTypes.MAGIC) || incoming.is(DamageTypes.INDIRECT_MAGIC)
         || incoming.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !enemy(defender, attacker)) return;
      Vec3 facing = new Vec3(data.getFloat(COUNTER_FACING_X), 0, data.getFloat(COUNTER_FACING_Z)).normalize();
      Vec3 towardAttacker = attacker.position().subtract(defender.position()).multiply(1, 0, 1).normalize();
      if (facing.dot(towardAttacker) < 0.5) return;
      data.remove(COUNTER_UNTIL);
      event.setCanceled(true);
      hit(defender, attacker, ArtoriaRules.GUARD_COUNTER_DAMAGE, ArtoriaRules.GUARD_COUNTER, false, false);
      if (defender.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5,
            attacker.getZ(), 10, 0.25, 0.25, 0.25, 0.08);
         level.playSound(null, defender.blockPosition(), SoundEvents.SHIELD_BLOCK,
            defender instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.0F, 0.85F);
      }
   }
   public static boolean hammer(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return false;
      long now = level.getGameTime();
      if (now < owner.getPersistentData().getLong(ArtoriaStateService.WIND_HAMMER_READY)) return false;
      if (!canWind(owner)) {
         ArtoriaStateService.lockWindRecoveryIfVeilMissing(owner);
         return false;
      }
      Vec3 forward = horizontal(owner);
      owner.getPersistentData().putLong(ArtoriaStateService.WIND_HAMMER_READY,
         now + ArtoriaRules.WIND_HAMMER_COOLDOWN);
      lockAction(owner, ArtoriaRules.WIND_HAMMER_WINDUP_TICKS);
      owner.getPersistentData().putLong(ArtoriaStateService.WIND_READY,
         level.getGameTime() + ArtoriaRules.WIND_HAMMER_WINDUP_TICKS + ArtoriaRules.WIND_REGATHER_TICKS);
      // The raised-blade windup is already a visible Excalibur state; only the
      // barrier's regathering lock continues after the gust is released.
      reveal(owner, ArtoriaRules.WIND_HAMMER_WINDUP_TICKS + ArtoriaRules.WIND_REGATHER_TICKS);
      SasakiKojiroActionAnimationService.play(owner, SasakiKojiroAction.TSUBAME_WINDUP);
      VFXServerEffects.spawn(level, "artoria_wind_hammer_charge", owner, 96);
      level.playSound(null, owner.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.65F, 1.55F);
      owner.setDeltaMovement(owner.getDeltaMovement().multiply(.35, 1, .35));
      owner.hurtMarked = true;
      TYPE_MOON_WORLD.queueServerWork(ArtoriaRules.WIND_HAMMER_WINDUP_TICKS,
         () -> releaseHammer(owner, level, forward));
      return true;
   }

   private static void releaseHammer(LivingEntity owner, ServerLevel level, Vec3 forward) {
      // The cast itself has already started the shared refill timer. Requiring
      // canWind here would cancel every delayed hammer before its hit frame.
      if (!validDelayed(owner, level) || !ArtoriaStateService.isWindBarrierEnabled(owner)
         || ArtoriaStateService.isWindDefenseActive(owner)) return;
      Vec3 right = new Vec3(-forward.z, 0, forward.x);
      Vec3 start = owner.position().add(0, owner.getBbHeight() * 0.55, 0);
      reveal(owner, ArtoriaRules.WIND_REGATHER_TICKS);
      owner.getPersistentData().putLong(ArtoriaStateService.WIND_READY,
         owner.level().getGameTime() + ArtoriaRules.WIND_REGATHER_TICKS);
      animate(owner, SasakiKojiroAction.THRUST);
      Set<Integer> hits = new HashSet<>();
      boolean npc = owner instanceof ArtoriaPendragonEntity;
      boolean burst = ArtoriaPendragonCombatHelper.isManaBurstActive(owner);
      double range = npc ? ArtoriaRules.WIND_HAMMER_NPC_RANGE
         : burst ? ArtoriaRules.WIND_HAMMER_PLAYER_BURST_RANGE : ArtoriaRules.WIND_HAMMER_PLAYER_RANGE;
      double widthScale = npc ? ArtoriaRules.WIND_HAMMER_NPC_WIDTH_SCALE
         : burst ? ArtoriaRules.WIND_HAMMER_PLAYER_BURST_WIDTH_SCALE : ArtoriaRules.WIND_HAMMER_PLAYER_WIDTH_SCALE;
      float damage = ArtoriaRules.WIND_HAMMER_DAMAGE;
      int sample = 0;
      double step = npc ? 0.8 : 0.75;
      for (double distance = npc ? 1.5 : 1.2; distance <= range; distance += step, sample++) {
         Vec3 center = start.add(forward.scale(distance));
         double halfHeight = 0.7 + distance * (npc ? 0.32 : 0.28);
         double halfWidth = distance * widthScale;
         for (double side = -halfWidth; side <= halfWidth; side += 0.8) {
            double firstVertical = npc ? -0.7 : halfHeight * 0.18;
            double lastVertical = npc ? halfHeight : firstVertical;
            double verticalStep = npc ? 0.7 : 1.0;
            for (double vertical = firstVertical; vertical <= lastVertical; vertical += verticalStep) {
               Vec3 point = center.add(right.scale(side)).add(0.0, vertical, 0.0);
               double verticalRadius = npc ? 0.75 : 0.85 + distance * 0.04;
               AABB area = new AABB(point, point).inflate(0.9, verticalRadius, 0.9);
               for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, t -> enemy(owner, t))) {
                  if (owner.hasLineOfSight(target) && hits.add(target.getId())) {
                     damageWindHammer(owner, target, damage);
                     target.push(forward.x * 1.05, 0.34, forward.z * 1.05);
                     target.hurtMarked = true;
                  }
               }
            }
         }
         if (sample % 2 == 0) {
            for (int lane = -1; lane <= 1; lane++) {
               double curl = Math.sin(distance * 0.72 + lane * 1.8) * Math.min(0.32, distance * 0.025);
               Vec3 ribbon = center.add(right.scale(lane * (0.14 + distance * 0.055) + curl));
               level.sendParticles(ParticleTypes.GUST, ribbon.x, ribbon.y, ribbon.z, 1, 0.05, 0.05, 0.05, 0.0);
               level.sendParticles(ParticleTypes.CLOUD, ribbon.x, ribbon.y, ribbon.z, 1, 0.12, 0.10, 0.12, 0.025);
            }
         }
      }
      VFXServerEffects.spawnOriented(level, "artoria_wind_hammer", start.add(forward.scale(0.8)), forward, 128);
      level.playSound(null, owner.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1, 1.3F);
   }
   private static void damageWindHammer(LivingEntity owner, LivingEntity target, float damage) {
      target.getPersistentData().putLong(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_DAMAGE_BYPASS_UNTIL,
         target.level().getGameTime() + 2L);
      target.invulnerableTime = 0;
      DamageSource base = owner instanceof ServerPlayer player
         ? owner.damageSources().playerAttack(player) : owner.damageSources().magic();
      target.hurt(new CombatDamageSource(base, AttackOrigin.skill(ArtoriaRules.ID, "invisible_air_hammer")), damage);
      target.invulnerableTime = 0;
   }

   public static boolean windRelease(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return false;
      long now = level.getGameTime();
      if (now < owner.getPersistentData().getLong(ArtoriaStateService.WIND_RELEASE_READY)) return false;
      if (!canWind(owner)) {
         ArtoriaStateService.lockWindRecoveryIfVeilMissing(owner);
         return false;
      }
      VFXServerEffects.spawn(level, "artoria_wind_release", owner, 96);
      owner.getPersistentData().putLong(ArtoriaStateService.WIND_RELEASE_READY,
         now + ArtoriaRules.WIND_RELEASE_COOLDOWN);
      reveal(owner, ArtoriaRules.WIND_REGATHER_TICKS);
      owner.getPersistentData().putLong(ArtoriaStateService.WIND_READY,
         owner.level().getGameTime() + ArtoriaRules.WIND_REGATHER_TICKS);
      double radius = 8.0;
      boolean burst = ArtoriaPendragonCombatHelper.isManaBurstActive(owner);
      boolean npc = owner instanceof ArtoriaPendragonEntity;
      double verticalRange = 3.5 + (burst ? 1.5 : 0.0);
      float baseDamage = burst ? (npc ? 34.0F : 32.0F) : 22.0F;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
         owner.getBoundingBox().inflate(radius, verticalRange, radius), t -> enemy(owner, t))) {
         Vec3 away = target.position().subtract(owner.position()).multiply(1, 0, 1);
         double distance = away.length();
         if (distance > radius || !owner.hasLineOfSight(target)) continue;
         float amount = (float)(baseDamage * Math.clamp(1.0 - distance / (radius * 1.35), .35, 1.0));
         hit(owner, target, amount, "invisible_air_release", false, true);
         Vec3 dir = away.lengthSqr() > 1.0E-6 ? away.normalize() : horizontal(owner);
         double falloff = 1.0 - Math.min(1.0, distance / radius);
         double horizontalImpulse = 3.6 * (0.35 + 0.65 * falloff);
         double verticalImpulse = (burst ? 1.05 : 0.8) * (0.45 + 0.55 * falloff);
         target.push(dir.x * horizontalImpulse, verticalImpulse, dir.z * horizontalImpulse);
         target.hurtMarked = true;
      }
      for (double[] ring : new double[][]{{0.4, 8.0, 0.18, 14, 0xFFFFFF, 0.92F},
         {0.24, 6.56, 0.12, 18, 0xF7F7FF, 0.7F}, {0.15, 4.48, 0.09, 22, 0xFFFFFF, 0.48F}}) {
         level.addFreshEntity(new net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity(level,
            owner.getX(), owner.getY() + 1.0, owner.getZ(), (float)ring[0], (float)ring[1], (float)ring[2],
            (int)ring[3], (int)ring[4], (float)ring[5], 0.0F));
      }
      for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(radius + 1.5))) {
         if (!(projectile.getOwner() instanceof LivingEntity shooter) || !enemy(owner, shooter)) continue;
         Vec3 away = projectile.position().subtract(owner.position());
         if (away.lengthSqr() > (radius + 1.5) * (radius + 1.5)) continue;
         Vec3 outward = away.lengthSqr() > 1.0E-6 ? away.normalize() : horizontal(owner).scale(-1.0);
         projectile.setDeltaMovement(projectile.getDeltaMovement().add(outward.scale(2.6)).add(0, .24, 0));
         projectile.hurtMarked = true;
      }
      level.sendParticles(ParticleTypes.GUST, owner.getX(), owner.getY() + .6, owner.getZ(), 12, 1.4, 1.0, 1.4, .12);
      level.sendParticles(ParticleTypes.CLOUD, owner.getX(), owner.getY() + .6, owner.getZ(), 72, 2.4, 1.0, 2.4, .11);
      level.sendParticles(ParticleTypes.END_ROD, owner.getX(), owner.getY() + .9, owner.getZ(), 24, 1.2, 1.1, 1.2, .10);
      level.playSound(null, owner.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1, 1.45F);
      return true;
   }
   public static void combo(LivingEntity owner) {
      LivingEntity target = comboTarget(owner);
      if (target == null || !(owner.level() instanceof ServerLevel level)) return;
      spawnSwordTrail(owner, level);
      if (owner.distanceTo(target) > 5) {
         Vec3 dir = target.position().subtract(owner.position()).multiply(1, 0, 1).normalize();
         owner.setDeltaMovement(owner.getDeltaMovement().add(dir.scale(1.65)).add(0, .24, 0)); owner.hurtMarked = true;
         TYPE_MOON_WORLD.queueServerWork(5, () -> {
            if (validDelayed(owner, level) && enemy(owner, target) && owner.distanceToSqr(target) <= 30.25 && owner.hasLineOfSight(target))
               hit(owner, target, 32, "artoria_small_combo", false, true);
         });
      } else {
         arc(owner, 4.8, 28);
         TYPE_MOON_WORLD.queueServerWork(4, () -> {
            if (validDelayed(owner, level)) {
               spawnSwordTrail(owner, level);
               arc(owner, 5.5, 24);
            }
         });
      }
      owner.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
      level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, .95F, 1.3F);
   }
   private static boolean validDelayed(LivingEntity owner, ServerLevel level) {
      return owner.isAlive() && !owner.isRemoved() && owner.level() == level && ArtoriaStateService.isArtoria(owner)
         && !ArtoriaStateService.busy(owner) && (!(owner instanceof ServerPlayer p) || !p.hasDisconnected());
   }
   private static void arc(LivingEntity owner, double range, float amount) {
      Vec3 forward = horizontal(owner);
      for (LivingEntity target : owner.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(range), t -> enemy(owner, t))) {
         Vec3 offset = target.position().subtract(owner.position());
         if (offset.lengthSqr() <= range * range && offset.normalize().dot(forward) > 0.1 && owner.hasLineOfSight(target))
            hit(owner, target, amount, "artoria_small_combo", false, true);
      }
   }
   public static void lightCannon(LivingEntity owner) {
      if (!(owner.level() instanceof ServerLevel level)) return;
      Vec3 direction = owner.getLookAngle().normalize(), start = owner.getEyePosition();
      Vec3 max = start.add(direction.scale(32));
      Vec3 end = traceLightCannon(level, owner, start, direction, max);
      double length = start.distanceTo(end);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(2), t -> enemy(owner, t))) {
         Vec3 center = target.getBoundingBox().getCenter();
         double along = center.subtract(start).dot(direction);
         double radius = 1.6 + target.getBbWidth() * .5;
         if (along >= 0 && along <= length && owner.hasLineOfSight(target)
            && center.distanceToSqr(start.add(direction.scale(along))) <= radius * radius)
            hit(owner, target, ArtoriaRules.MANA_BURST_BEAM_DAMAGE, "mana_burst_beam", true, false);
      }
      Vec3 right = new Vec3(-direction.z, 0, direction.x);
      if (right.lengthSqr() < 1.0E-5) right = new Vec3(1, 0, 0);
      else right = right.normalize();
      Vec3 up = direction.cross(right).normalize();
      for (double t = 0; t <= length; t += 0.95) {
         double progress = length <= 1.0E-4 ? 1 : t / length;
         double width = 0.10 + Math.sin(progress * Math.PI) * 0.28;
         Vec3 center = start.add(direction.scale(t));
         for (int lane = -1; lane <= 1; lane++) {
            Vec3 p = center.add(right.scale(lane * width)).add(up.scale(lane == 0 ? 0 : 0.06 * Math.sin(t * 0.8 + lane)));
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.08, 0.08, 0.08, 0.018);
            if (lane == 0 && Math.floorMod((int)t, 3) == 0)
               level.sendParticles(ParticleTypes.GUST, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
            if (lane == 0 && Math.floorMod((int)t, 2) == 0)
               level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
         }
      }
      level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0, 0, 0, 0);
      level.sendParticles(ParticleTypes.GUST, end.x, end.y, end.z, 2, 0.18, 0.18, 0.18, 0.0);
      level.sendParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 12, 0.48, 0.35, 0.48, 0.035);
      level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 18, 0.55, 0.4, 0.55, 0.12);
      level.playSound(null, owner.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.25F, 1.55F);
   }
   private static Vec3 traceLightCannon(ServerLevel level, LivingEntity owner, Vec3 start, Vec3 direction, Vec3 max) {
      Vec3 rayStart = start;
      Vec3 end = max;
      int removed = 0;
      for (int attempt = 0; attempt < ArtoriaRules.MANA_BURST_BEAM_MAX_BLOCKS + 1; attempt++) {
         HitResult hit = level.clip(new ClipContext(rayStart, max, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
         if (hit.getType() == HitResult.Type.MISS) return max;
         end = hit.getLocation();
         if (!(hit instanceof BlockHitResult blockHit)) return end;
         BlockPos pos = blockHit.getBlockPos();
         BlockState state = level.getBlockState(pos);
         if (removed >= ArtoriaRules.MANA_BURST_BEAM_MAX_BLOCKS || !beamBlockEligible(level, pos, state)
            || !canBreakBeamBlock(level, owner, pos, state)) return end;
         if (!CombatTerrainProtection.destroyBlockWithinSurfaceDepth(level, pos, true, owner, 1)) return end;
         removed++;
         rayStart = hit.getLocation().add(direction.scale(0.03));
         if (removed >= ArtoriaRules.MANA_BURST_BEAM_MAX_BLOCKS) return rayStart;
      }
      return end;
   }
   private static boolean beamBlockEligible(ServerLevel level, BlockPos pos, BlockState state) {
      float hardness = state.getDestroySpeed(level, pos);
      return !state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty()
         && !state.is(Blocks.BEDROCK) && hardness >= 0 && hardness <= 0.6F
         && !state.is(BlockTags.BEDS)
         && !state.is(BlockTags.RAILS) && !state.is(BlockTags.BUTTONS) && !state.is(BlockTags.DOORS)
         && !state.is(BlockTags.TRAPDOORS) && !state.is(BlockTags.PRESSURE_PLATES) && !state.is(BlockTags.CAULDRONS)
         && !state.is(Blocks.REDSTONE_BLOCK) && !state.is(Blocks.REDSTONE_LAMP) && !state.is(Blocks.REDSTONE_TORCH)
         && !state.is(Blocks.REPEATER) && !state.is(Blocks.COMPARATOR) && !state.is(Blocks.LEVER)
         && !state.is(Blocks.OBSERVER) && !state.is(Blocks.PISTON) && !state.is(Blocks.STICKY_PISTON)
         && !state.is(Blocks.DISPENSER) && !state.is(Blocks.DROPPER) && !state.is(Blocks.TARGET);
   }
   private static boolean canBreakBeamBlock(ServerLevel level, LivingEntity owner, BlockPos pos, BlockState state) {
      boolean allowed = Config.terrainDestructionEnabled && (owner instanceof Player
         ? Config.playerTerrainDestructionEnabled
         : Config.npcTerrainDestructionEnabled && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING));
      if (!allowed || !CombatTerrainProtection.canRemoveWithinSurfaceDepth(level, pos, 1)) return false;
      if (NeoForge.EVENT_BUS.post(new TerrainImpactBlockEvent(level, owner, pos, state,
         TerrainImpactProfile.of(TerrainImpactProfile.Tier.CHIP), "ARTORIA_MANA_BURST_BEAM")).isCanceled()) return false;
      if (owner instanceof Player player) {
         return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
      }
      return true;
   }
}
