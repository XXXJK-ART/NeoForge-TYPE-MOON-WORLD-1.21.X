package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshCrossSlashEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UBWInstanceManager;

/** Gilgamesh-specific ranged AI and passive state. */
public final class GilgameshCombatHelper {
   private static final String NEXT_GATE = "GilgameshNextGate";
   private static final float GATE_PROJECTILE_DAMAGE_MULTIPLIER = 2.5F / 1.5F;
   private static final String[] GATE_WEAPONS = {"durandal", "gram", "vajra", "harpe", "fangtian_huaji", "pseudo_spiral_sword", "gae_bulg"};
   private enum GateFormation { SINGLE, FRONTAL, FLANKS, TARGET_RING, SKY_RAIN }
   private record GateAttack(int count, float damage, double mpCost, int cooldown) { }
   private static final String LAST_GATE = "GilgameshLastGate";
   private static final String LAST_CHAIN = "GilgameshLastChain";
   private static final String LAST_CHAIN_COUNTER = "GilgameshLastChainCounter";
   private static final String CHAIN_OWNER = "GilgameshChainsOwner";
   private static final String NEXT_BOUND_GATE = "GilgameshNextBoundGate";
   private static final String MELEE_UNTIL = "GilgameshMeleeUntil";
   private static final String LAST_MELEE = "GilgameshLastMelee";
   private static final String NEXT_MELEE_SWING = "GilgameshNextMeleeSwing";
   private static final String RETREAT_UNTIL = "GilgameshRetreatUntil";
   private static final String DAMAGE_WINDOW_START = "GilgameshDamageWindowStart";
   private static final String DAMAGE_WINDOW_TOTAL = "GilgameshDamageWindowTotal";
   private static final String NEXT_COMBAT_VOICE = "GilgameshNextCombatVoice";
   private static final String NEXT_COMBAT_VOICE_INDEX = "GilgameshNextCombatVoiceIndex";
   private static final String LAST_EA = "GilgameshLastEa";
   private static final String LAST_CROSS = "GilgameshLastCross";
   private static final long MAJOR_NP_SWITCH_LOCK_TICKS = 200L;
   private static final String EA_SUMMON_END = "GilgameshEaSummonEnd";
   private static final String EA_DRAW_END = "GilgameshEaDrawEnd";
   private static final String EA_SUMMON_TARGET = "GilgameshEaSummonTarget";
   public static final int EA_SUMMON_TICKS = 72;
   public static final int EA_DRAW_TICKS = 53;
   private static final String LAST_CHARISMA = "GilgameshLastCharisma";
   private static final String ENKIDU_BARRAGE_REMAINING = "GilgameshEnkiduBarrageRemaining";
   private static final String ENKIDU_BARRAGE_NEXT = "GilgameshEnkiduBarrageNext";
   private static final String ENKIDU_BARRAGE_COOLDOWN = "GilgameshEnkiduBarrageCooldown";
   private GilgameshCombatHelper() { }

   public static void tick(GilgameshEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean("GilgameshPassivesInitialized")) {
         data.putBoolean("GilgameshPassivesInitialized", true);
         data.putFloat("MagicResistanceDamageReduction", 0.20F);
         data.putFloat("GilgameshCommandObedienceMin", 0.20F);
         data.putFloat("GilgameshCommandObedienceMax", 0.60F);
      }
      updateMonotonicPhase(entity);
      if (entity.tickCount % 20 == 0) {
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + Math.max(0.25, entity.getMaxMp() * 0.013)));
      }
      if (tickEaSummon(entity, level, now, data)) {
         endMeleeMode(entity);
         return;
      }
      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive()) {
         clearEaEquipment(entity);
         endMeleeMode(entity);
         updateFlight(entity, target, now);
         return;
      }
      lockFacing(entity, target);
      if (GilgameshDuelState.tickGilgamesh(entity, level, now)) {
         endMeleeMode(entity);
         return;
      }
      if (GilgameshEaBeamEntity.isEaActiveFor(entity)) {
         endMeleeMode(entity);
         entity.getNavigation().stop();
         return;
      }
      clearEaEquipment(entity);
      if (isBoundByGilgamesh(entity, target, now)) {
         endMeleeMode(entity);
         updateFlight(entity, target, now);
         if (tryBoundGatePursuit(entity, level, target, now, data)) return;
      }
      if (tickMeleeMode(entity, level, target, now, data)) return;
      updateFlight(entity, target, now);

      if (target instanceof EnkiduEntity enkidu && tryEnkiduScaleBarrage(entity, level, enkidu, now, data)) return;
      if (tryChains(entity, level, target, now, data)) return;
      if (tryCrossSlash(entity, level, target, now, data)) return;
      if (tryEa(entity, level, target, now, data)) return;
      if (now >= data.getLong(NEXT_GATE)) {
         ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
         boolean projection = isProjectionCounterTarget(target);
         GateAttack attack = projection ? new GateAttack(150 * 12, 22.0F, 14.4, 320) : chooseGateAttack(entity, target, phase);
         if (entity.getCurrentMp() >= attack.mpCost()) {
            fireGateVolley(entity, level, target, attack.count(), attack.damage());
            data.putLong(LAST_GATE, now);
            data.putLong(NEXT_GATE, now + attack.cooldown());
            entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - attack.mpCost()));
         } else {
            data.putLong(NEXT_GATE, now + 10L);
         }
      }
      if (now - data.getLong(LAST_CHARISMA) >= 700 && entity.getCurrentMp() >= 20.0 && entity.getRandom().nextFloat() < 0.08F) {
         useCharisma(entity, level, data, now);
      }
   }

   private static void updateMonotonicPhase(GilgameshEntity entity) {
      double ratio = entity.getHealth() / Math.max(1.0, entity.getMaxHealth());
      if (ratio <= 0.40) ServantCombatSystem.forcePhaseAtLeast(entity, ServantCombatPhase.DECISIVE);
      else if (ratio <= 0.60) ServantCombatSystem.forcePhaseAtLeast(entity, ServantCombatPhase.NORMAL);
   }

   public static boolean isFlying(GilgameshEntity entity) {
      return entity.isFlyingMode();
   }

   private static void updateFlight(GilgameshEntity entity, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (target == null || !target.isAlive() || ServantCombatSystem.cannotAct(entity)) {
         entity.setFlyingMode(false);
         return;
      }
      double distance = entity.distanceTo(target);
      entity.setFlyingMode(true);
      entity.getNavigation().stop(); entity.fallDistance = 0.0F;
      double desiredY = Math.max(target.getY() + 5.0, entity.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, entity.blockPosition()).getY() + 6.0);
      Vec3 away = entity.position().subtract(target.position()).multiply(1, 0, 1);
      if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
      away = away.normalize();
      boolean retreat = shouldRetreatFrom(entity, target, now);
      double radial = retreat
         ? distance < 20.0 ? 0.24 : distance > 26.0 ? -0.08 : 0.0
         : distance > 18.0 ? -0.12 : 0.0;
      Vec3 orbit = new Vec3(-away.z, 0, away.x).scale(retreat ? 0.06 : 0.10);
      double vertical = net.minecraft.util.Mth.clamp((desiredY - entity.getY()) * 0.08, -0.22, 0.22);
      entity.setDeltaMovement(entity.getDeltaMovement().scale(0.58).add(away.scale(radial)).add(orbit).add(0, vertical, 0));
   }

   private static void lockFacing(GilgameshEntity entity, LivingEntity target) {
      Vec3 targetPoint = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 direction = targetPoint.subtract(entity.position().add(0.0, entity.getEyeHeight(), 0.0));
      entity.getLookControl().setLookAt(targetPoint.x, targetPoint.y, targetPoint.z, 360.0F, 360.0F);
      entity.faceToward(targetPoint);
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      float pitch = (float)(-net.minecraft.util.Mth.atan2(direction.y, Math.max(1.0E-6, horizontal)) * 180.0 / Math.PI);
      pitch = net.minecraft.util.Mth.clamp(pitch, -90.0F, 90.0F);
      entity.setXRot(pitch);
      entity.xRotO = pitch;
   }

   private static boolean tryChains(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, CompoundTag data) {
      if (target == entity || target instanceof GilgameshEntity || target instanceof EnkiduEntity) return false;
      boolean divine = ServantIdentityHelper.hasTrait(target, ServantTraitTag.DIVINE)
         || ServantIdentityHelper.hasTrait(target, ServantTraitTag.CELESTIAL);
      if (entity.distanceTo(target) > 30 || now - data.getLong(LAST_CHAIN) < 300 || entity.getCurrentMp() < 24.0) return false;
      if (!divine && entity.distanceTo(target) > 5.0) return false;
      int duration = divine ? 200 : 100;
      bindWithChains(entity, level, target, now, duration, divine);
      entity.setCurrentMp(entity.getCurrentMp() - 24.0);
      data.putLong(LAST_CHAIN, now);
      return true;
   }

   private static void bindWithChains(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, int duration, boolean divine) {
      target.setDeltaMovement(Vec3.ZERO);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, divine ? 20 : 8, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, divine ? 20 : 8, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, divine ? 10 : 4, false, true, true));
      target.getPersistentData().putLong("ChainsOfHeavenBoundUntil", now + duration);
      target.getPersistentData().putBoolean("ChainsOfHeavenBlocksTeleport", true);
      target.getPersistentData().putUUID(CHAIN_OWNER, entity.getUUID());
      entity.getPersistentData().putLong(NEXT_BOUND_GATE, now + 12L);
      level.addFreshEntity(new ChainsOfHeavenBindingEntity(level, entity, target, duration, divine));
      long boundUntil = now + duration;
      TYPE_MOON_WORLD.queueServerWork(duration, () -> {
         if (target.getPersistentData().getLong("ChainsOfHeavenBoundUntil") <= boundUntil) {
            target.getPersistentData().remove("ChainsOfHeavenBlocksTeleport");
            if (target.getPersistentData().hasUUID(CHAIN_OWNER)
               && entity.getUUID().equals(target.getPersistentData().getUUID(CHAIN_OWNER))) {
               target.getPersistentData().remove(CHAIN_OWNER);
            }
         }
      });
      entity.triggerNamedActionAnimation("chain_of_heaven");
   }

   private static boolean tryBoundGatePursuit(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, CompoundTag data) {
      if (!isBoundByGilgamesh(entity, target, now)) return false;
      entity.getNavigation().stop();
      if (now < data.getLong(NEXT_BOUND_GATE)) return true;
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      int count = phase == ServantCombatPhase.DECISIVE ? 72 : 48;
      fireGateVolley(entity, level, target, count, phase == ServantCombatPhase.DECISIVE ? 24.0F : 21.0F, false);
      data.putLong(NEXT_BOUND_GATE, now + (phase == ServantCombatPhase.DECISIVE ? 24L : 32L));
      return true;
   }

   private static boolean isBoundByGilgamesh(GilgameshEntity entity, LivingEntity target, long now) {
      CompoundTag targetData = target.getPersistentData();
      return targetData.hasUUID(CHAIN_OWNER) && entity.getUUID().equals(targetData.getUUID(CHAIN_OWNER))
         && targetData.getLong("ChainsOfHeavenBoundUntil") > now;
   }

   public static void tryRetaliatoryChains(GilgameshEntity entity, DamageSource source) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()
         || !(source.getEntity() instanceof LivingEntity attacker) || !attacker.isAlive() || attacker == entity
         || attacker instanceof GilgameshEntity || attacker instanceof EnkiduEntity || attacker.isAlliedTo(entity)) return;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (entity.distanceTo(attacker) > 30.0 || entity.getCurrentMp() < 24.0
         || isBoundByGilgamesh(entity, attacker, now)
         || now - data.getLong(LAST_CHAIN_COUNTER) < 160L || now - data.getLong(LAST_CHAIN) < 80L) return;
      float chance = switch (ServantCombatSystem.getPhase(entity)) {
         case PROBING -> 0.20F;
         case NORMAL -> 0.25F;
         case DECISIVE -> 0.30F;
      };
      if (entity.getRandom().nextFloat() >= chance) return;
      boolean divine = ServantIdentityHelper.hasTrait(attacker, ServantTraitTag.DIVINE)
         || ServantIdentityHelper.hasTrait(attacker, ServantTraitTag.CELESTIAL);
      bindWithChains(entity, level, attacker, now, divine ? 200 : 100, divine);
      entity.setTarget(attacker);
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 24.0));
      data.putLong(LAST_CHAIN, now);
      data.putLong(LAST_CHAIN_COUNTER, now);
   }

   public static void noteIncomingThreat(GilgameshEntity entity, DamageSource source, float amount) {
      if (!(entity.level() instanceof ServerLevel level) || amount <= 0.0F
         || !(source.getEntity() instanceof LivingEntity attacker) || attacker == entity || attacker.isAlliedTo(entity)) return;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      long windowStart = data.getLong(DAMAGE_WINDOW_START);
      double accumulated = now - windowStart <= 40L ? data.getDouble(DAMAGE_WINDOW_TOTAL) + amount : amount;
      if (now - windowStart > 40L) data.putLong(DAMAGE_WINDOW_START, now);
      data.putDouble(DAMAGE_WINDOW_TOTAL, accumulated);
      double singleHitThreshold = Math.max(20.0, entity.getMaxHealth() * 0.15);
      double burstThreshold = Math.max(35.0, entity.getMaxHealth() * 0.30);
      if (amount >= singleHitThreshold || accumulated >= burstThreshold) {
         data.putLong(RETREAT_UNTIL, now + 80L);
         data.putLong(DAMAGE_WINDOW_START, now);
         data.putDouble(DAMAGE_WINDOW_TOTAL, 0.0);
         endMeleeMode(entity);
      }
   }

   private static boolean shouldRetreatFrom(GilgameshEntity entity, LivingEntity target, long now) {
      var definition = ServantIdentityHelper.definitionOf(target);
      return entity.getPersistentData().getLong(RETREAT_UNTIL) > now
         || definition != null && definition.classType() == ServantClassType.BERSERKER;
   }

   public static boolean isMeleeMode(GilgameshEntity entity) {
      return entity.level() != null && entity.getPersistentData().getLong(MELEE_UNTIL) > entity.level().getGameTime();
   }

   private static boolean tickMeleeMode(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, CompoundTag data) {
      boolean active = data.getLong(MELEE_UNTIL) > now;
      double horizontalDistance = entity.position().multiply(1.0, 0.0, 1.0).distanceTo(target.position().multiply(1.0, 0.0, 1.0));
      if (shouldRetreatFrom(entity, target, now)) {
         if (active) endMeleeMode(entity);
         return false;
      }
      if (!active && entity.tickCount % 20 == 0 && horizontalDistance <= 5.0
         && Math.abs(entity.getY() - target.getY()) <= 8.0 && now - data.getLong(LAST_MELEE) >= 300L) {
         float chance = ServantCombatSystem.getPhase(entity) == ServantCombatPhase.PROBING ? 0.16F : 0.24F;
         if (entity.getRandom().nextFloat() < chance && entity.getMainHandItem().isEmpty()) {
            data.putLong(MELEE_UNTIL, now + 80L);
            data.putLong(LAST_MELEE, now);
            data.putLong(NEXT_MELEE_SWING, now + 8L);
            entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GILGAMESH_DURANDAL.get()));
            entity.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            level.playSound(null, entity.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.HOSTILE, 0.9F, 1.7F);
            active = true;
         }
      }
      if (!active) return false;
      if (!target.isAlive() || horizontalDistance > 10.0 || now >= data.getLong(MELEE_UNTIL)) {
         endMeleeMode(entity);
         return false;
      }
      entity.setFlyingMode(false);
      entity.fallDistance = 0.0F;
      if (horizontalDistance > 2.8 || Math.abs(entity.getY() - target.getY()) > 1.8) {
         entity.getNavigation().moveTo(target, 1.25);
         if (!entity.onGround()) {
            Vec3 approach = target.position().subtract(entity.position());
            if (approach.lengthSqr() > 1.0E-4) {
               Vec3 motion = approach.normalize().scale(0.16);
               entity.setDeltaMovement(entity.getDeltaMovement().scale(0.72).add(motion.x, Math.min(-0.08, motion.y), motion.z));
            }
         }
      } else {
         entity.getNavigation().stop();
         if (now >= data.getLong(NEXT_MELEE_SWING)) {
            entity.triggerNamedActionAnimation("durandal_slash");
            entity.doHurtTarget(target);
            data.putLong(NEXT_MELEE_SWING, now + 12L);
         }
      }
      return true;
   }

   private static void endMeleeMode(GilgameshEntity entity) {
      CompoundTag data = entity.getPersistentData();
      data.remove(MELEE_UNTIL);
      data.remove(NEXT_MELEE_SWING);
      if (entity.getMainHandItem().is(ModItems.GILGAMESH_DURANDAL.get())) {
         entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }
   }

   private static boolean tryEa(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, CompoundTag data) {
      if (ServantCombatSystem.getPhase(entity) != ServantCombatPhase.DECISIVE
         || now - data.getLong(LAST_EA) < 1200
         || data.contains(LAST_CROSS) && now - data.getLong(LAST_CROSS) < MAJOR_NP_SWITCH_LOCK_TICKS
         || entity.getCurrentMp() < 200.0) return false;
      if (entity.tickCount % 20 != 0) return false;
      float chance = level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(18.0),
         living -> living != entity && living.isAlive() && !living.isAlliedTo(entity)).size() >= 2 ? 0.16F : 0.08F;
      if (entity.getRandom().nextFloat() > chance) return false;
      if (UBWInstanceManager.isUbwDimension(level)
         && (target instanceof EmiyaArcherEntity || target instanceof net.minecraft.server.level.ServerPlayer player
            && "emiya_archer".equals(player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_id))) {
         entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10, false, true, true));
         entity.getPersistentData().putLong("GilgameshEaInterruptedUntil", now + 40);
         data.putLong(LAST_EA, now);
         return true;
      }
      Vec3 look = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(entity.position().add(0, entity.getBbHeight() * 0.65, 0)).normalize();
      beginNpcEaSummon(entity, level, target, now, look);
      data.putLong(LAST_EA, now);
      return true;
   }

   public static boolean beginNpcEaSummon(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now) {
      if (entity == null || level == null || target == null || !entity.isAlive() || !target.isAlive()) return false;
      Vec3 look = target.position().add(0, target.getBbHeight() * 0.5, 0)
         .subtract(entity.position().add(0, entity.getBbHeight() * 0.65, 0)).normalize();
      beginNpcEaSummon(entity, level, target, now, look);
      return true;
   }

   private static void beginNpcEaSummon(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, Vec3 look) {
      CompoundTag data = entity.getPersistentData();
      endMeleeMode(entity);
      clearEaEquipment(entity);
      entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GILGAMESH_BAB_ILU.get()));
      entity.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      entity.triggerNamedActionAnimation("ea_unlock");
      // The inverted tree is a vertical world landmark; it must not inherit the
      // target's pitch/yaw and lean sideways during the unlock animation.
      VFXServerEffects.spawn(level, "gilgamesh_ea_tree", entity.position().add(0, entity.getBbHeight() * 0.65, 0), 192.0);
      level.playSound(null, entity.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.HOSTILE, 1.6F, 0.72F);
      data.putLong(EA_SUMMON_END, now + EA_SUMMON_TICKS);
      data.putUUID(EA_SUMMON_TARGET, target.getUUID());
   }

   private static boolean tickEaSummon(GilgameshEntity entity, ServerLevel level, long now, CompoundTag data) {
      if (!data.contains(EA_SUMMON_END) && !data.contains(EA_DRAW_END)) return false;
      LivingEntity target = null;
      if (data.hasUUID(EA_SUMMON_TARGET)) {
         net.minecraft.world.entity.Entity resolved = level.getEntity(data.getUUID(EA_SUMMON_TARGET));
         if (resolved instanceof LivingEntity living) target = living;
      }
      if (target == null || !target.isAlive() || target.level() != level) {
         clearEaSummon(entity, data);
         return true;
      }
      lockFacing(entity, target);
      entity.getNavigation().stop();
      entity.setDeltaMovement(Vec3.ZERO);
      if (data.contains(EA_SUMMON_END)) {
         if (now < data.getLong(EA_SUMMON_END)) return true;
         entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GILGAMESH_EA.get()));
         entity.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
         entity.triggerNamedActionAnimation("ea_release");
         level.playSound(null, entity.blockPosition(), ModSounds.GILGAMESH_VOICE_EA_DRAW.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
         level.playSound(null, entity.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1.5F, 0.62F);
         data.remove(EA_SUMMON_END);
         data.putLong(EA_DRAW_END, now + EA_DRAW_TICKS);
         return true;
      }
      if (now < data.getLong(EA_DRAW_END)) return true;
      Vec3 look = target.position().add(0, target.getBbHeight() * 0.5, 0)
         .subtract(entity.position().add(0, entity.getBbHeight() * 0.65, 0)).normalize();
      level.addFreshEntity(new GilgameshEaBeamEntity(level, entity, look, target));
      data.remove(EA_DRAW_END);
      data.remove(EA_SUMMON_TARGET);
      return true;
   }

   private static void clearEaSummon(GilgameshEntity entity, CompoundTag data) {
      data.remove(EA_SUMMON_END);
      data.remove(EA_DRAW_END);
      data.remove(EA_SUMMON_TARGET);
      clearEaEquipment(entity);
   }

   public static void clearEaEquipment(GilgameshEntity entity) {
      if (entity.getMainHandItem().is(ModItems.GILGAMESH_BAB_ILU.get()) || entity.getMainHandItem().is(ModItems.GILGAMESH_EA.get())) {
         entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }
      if (entity.getOffhandItem().is(ModItems.GILGAMESH_BAB_ILU.get()) || entity.getOffhandItem().is(ModItems.GILGAMESH_EA.get())) {
         entity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      }
   }

   private static boolean tryCrossSlash(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, CompoundTag data) {
      if (ServantCombatSystem.getPhase(entity) != ServantCombatPhase.DECISIVE
         || now - data.getLong(LAST_CROSS) < 1800
         || data.contains(LAST_EA) && now - data.getLong(LAST_EA) < MAJOR_NP_SWITCH_LOCK_TICKS
         || entity.tickCount % 40 != 0) return false;
      if (entity.getRandom().nextFloat() >= 0.05F) return false;
      Vec3 direction = target.position().subtract(entity.position()).multiply(1, 0, 1).normalize();
      data.putLong("GilgameshCrossSlashCopyUntil", now + 32L);
      data.putDouble("GilgameshCrossSlashDirX", direction.x); data.putDouble("GilgameshCrossSlashDirY", direction.y); data.putDouble("GilgameshCrossSlashDirZ", direction.z);
      entity.triggerNamedActionAnimation("igalima_slash");
      GilgameshCrossSlashEntity green = new GilgameshCrossSlashEntity(level, entity, GilgameshCrossSlashEntity.SlashType.IGALIMA, direction);
      if (target instanceof EmiyaArcherEntity emiya) green.addImmuneEntity(emiya);
      level.addFreshEntity(green);
      GilgameshCrossSlashEntity white = new GilgameshCrossSlashEntity(level, entity, GilgameshCrossSlashEntity.SlashType.SULSAGANA, direction);
      if (target instanceof EmiyaArcherEntity emiya) white.addImmuneEntity(emiya);
      level.addFreshEntity(white);
      if (target instanceof EmiyaArcherEntity emiya) {
         Vec3 counterDirection = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(emiya.position().add(0, emiya.getBbHeight() * 0.5, 0)).normalize();
         GilgameshCrossSlashEntity counterGreen = new GilgameshCrossSlashEntity(level, emiya, GilgameshCrossSlashEntity.SlashType.IGALIMA, counterDirection);
         counterGreen.addImmuneEntity(entity); counterGreen.addImmuneEntity(emiya); level.addFreshEntity(counterGreen);
         GilgameshCrossSlashEntity counterWhite = new GilgameshCrossSlashEntity(level, emiya, GilgameshCrossSlashEntity.SlashType.SULSAGANA, counterDirection);
         counterWhite.addImmuneEntity(entity); counterWhite.addImmuneEntity(emiya); level.addFreshEntity(counterWhite);
         VFXServerEffects.spawn(level, "servant_emiya_projection", emiya, 256.0);
      }
      VFXServerEffects.spawnOriented(level, "gilgamesh_cross_slash", entity.position(), direction, 256.0);
      data.putLong(LAST_CROSS, now);
      return true;
   }

   public static void fireGateVolley(GilgameshEntity entity, ServerLevel level, LivingEntity target, int count, float damage) {
      fireGateVolley(entity, level, target, count, damage, true);
   }

   private static void fireGateVolley(GilgameshEntity entity, ServerLevel level, LivingEntity target, int count, float damage, boolean allowProjectionCounter) {
      boolean projectionCounter = allowProjectionCounter && isProjectionCounterTarget(target);
      boolean ubw = projectionCounter && UBWInstanceManager.isUbwDimension(level);
      if (projectionCounter) {
         count = 150 * 12;
      }
      int waveSize = projectionCounter ? 150 : count >= 100 ? 20 : 16;
      int waves = projectionCounter ? 12 : (count + waveSize - 1) / waveSize;
      final int totalCount = count;
      GateFormation baseFormation = projectionCounter ? GateFormation.FRONTAL : chooseFormation(entity, target, count);
      triggerGateAnimation(entity, count);
      tryPlayCombatVoice(entity, level);
      for (int wave = 0; wave < waves; wave++) {
         int waveIndex = wave;
         TYPE_MOON_WORLD.queueServerWork(wave * (ubw ? 15 : 10), () -> {
            if (!entity.isAlive() || !target.isAlive() || entity.level() != level) return;
            int amount = Math.min(waveSize, totalCount - waveIndex * waveSize);
            Vec3 forward = target.position().add(0, target.getBbHeight() * 0.55, 0)
               .subtract(entity.position().add(0, entity.getBbHeight() * 0.55, 0)).normalize();
            GateFormation formation = projectionCounter ? GateFormation.FRONTAL : formationForWave(baseFormation, waveIndex, totalCount);
            int columns = gateColumns(amount);
            for (int i = 0; i < amount; i++) {
               int globalIndex = waveIndex * waveSize + i;
               Vec3 gate = gatePosition(entity, target, forward, i, amount, formation);
               Vec3 aim = target.position().add(0, target.getBbHeight() * 0.55, 0).subtract(gate).normalize();
               String weapon = weaponForSlot(target, globalIndex);
               float projectileDamage = projectionCounter ? damage : damage * GATE_PROJECTILE_DAMAGE_MULTIPLIER;
               GilgameshGateWeaponProjectileEntity projectile = new GilgameshGateWeaponProjectileEntity(level, entity, gate, aim, weapon, projectileDamage);
               projectile.setLaunchDelay(24 + Math.min(16, (i / columns) * 3));
               if (projectionCounter) {
                  projectile.setSourceStyle(0);
                  projectile.setDuelToken(entity.getUUID() + ":projection:" + waveIndex + ":" + i);
               } else {
                  projectile.setHomingTarget(target);
               }
               level.addFreshEntity(projectile);
               level.sendParticles(ParticleTypes.END_ROD, gate.x, gate.y, gate.z, 14, 0.35, 0.35, 0.35, 0.02);
            }
            level.playSound(null, entity.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.HOSTILE, 1.2F, 1.45F);
         });
         if (projectionCounter) {
            TYPE_MOON_WORLD.queueServerWork(wave * (ubw ? 5 : 10), () -> spawnProjectionCounterWave(level, entity, target, totalCount, waveSize, waveIndex));
         }
      }
   }

   private static void tryPlayCombatVoice(GilgameshEntity entity, ServerLevel level) {
      CompoundTag data = entity.getPersistentData();
      long now = level.getGameTime();
      if (now < data.getLong(NEXT_COMBAT_VOICE) || entity.getRandom().nextFloat() >= 0.55F) return;
      boolean fightingEnkidu = entity.getTarget() instanceof EnkiduEntity
         || data.getBoolean("GilEnkiduDuelActive");
      net.minecraft.sounds.SoundEvent voice;
      if (!fightingEnkidu && entity.getRandom().nextFloat() < 0.18F) {
         voice = ModSounds.GILGAMESH_VOICE_MONGREL.get();
      } else {
         int index = Math.floorMod(data.getInt(NEXT_COMBAT_VOICE_INDEX), 4);
         voice = switch (index) {
            case 0 -> ModSounds.GILGAMESH_VOICE_ATTACK_1.get();
            case 1 -> ModSounds.GILGAMESH_VOICE_ATTACK_2.get();
            case 2 -> ModSounds.GILGAMESH_VOICE_ATTACK_3.get();
            default -> ModSounds.GILGAMESH_VOICE_ATTACK_4.get();
         };
         data.putInt(NEXT_COMBAT_VOICE_INDEX, (index + 1) % 4);
      }
      level.playSound(null, entity.blockPosition(), voice, SoundSource.HOSTILE, 1.6F, 1.0F);
      data.putLong(NEXT_COMBAT_VOICE, now + 80L + entity.getRandom().nextInt(101));
   }

   private static GateAttack chooseGateAttack(GilgameshEntity entity, ServantCombatPhase phase) {
      float roll = entity.getRandom().nextFloat();
      if (phase == ServantCombatPhase.PROBING) {
         if (roll < 0.56F) return new GateAttack(1, 16.0F, 1.8, 18);
         if (roll < 0.84F) return new GateAttack(5, 17.0F, 4.5, 34);
         return new GateAttack(10, 18.0F, 7.0, 52);
      }
      if (phase == ServantCombatPhase.NORMAL) {
         if (roll < 0.18F) return new GateAttack(1, 17.0F, 1.8, 16);
         if (roll < 0.47F) return new GateAttack(5, 18.0F, 4.5, 30);
         if (roll < 0.80F) return new GateAttack(10, 19.0F, 7.0, 48);
         if (roll < 0.95F) return new GateAttack(24, 20.0F, 12.0, 78);
         return new GateAttack(48, 21.0F, 16.0, 104);
      }
      if (roll < 0.12F) return new GateAttack(1, 18.0F, 1.8, 14);
      if (roll < 0.31F) return new GateAttack(10, 20.0F, 7.0, 42);
      if (roll < 0.68F) return new GateAttack(24, 21.0F, 12.0, 68);
      if (roll < 0.91F) return new GateAttack(48, 22.0F, 16.0, 92);
      return new GateAttack(120, 24.0F, 22.0, 150);
   }

   private static GateAttack chooseGateAttack(GilgameshEntity entity, LivingEntity target, ServantCombatPhase phase) {
      if (!(target instanceof EnkiduEntity)) return chooseGateAttack(entity, phase);
      // Against Enkidu the large exchange is handled separately. If its roll
      // misses, keep small shots uncommon so the matchup still reads as a
      // deliberate treasury barrage rather than a stream of single darts.
      float roll = entity.getRandom().nextFloat();
      if (phase == ServantCombatPhase.PROBING) {
         if (roll < 0.10F) return new GateAttack(1, 16.0F, 1.8, 20);
         if (roll < 0.24F) return new GateAttack(5, 17.0F, 4.5, 36);
         if (roll < 0.48F) return new GateAttack(10, 18.0F, 7.0, 54);
         return new GateAttack(24, 20.0F, 12.0, 78);
      }
      if (phase == ServantCombatPhase.NORMAL) {
         if (roll < 0.08F) return new GateAttack(1, 17.0F, 1.8, 18);
         if (roll < 0.18F) return new GateAttack(5, 18.0F, 4.5, 34);
         if (roll < 0.36F) return new GateAttack(10, 19.0F, 7.0, 52);
         if (roll < 0.72F) return new GateAttack(24, 20.0F, 12.0, 80);
         return new GateAttack(48, 21.0F, 16.0, 108);
      }
      if (roll < 0.06F) return new GateAttack(1, 18.0F, 1.8, 16);
      if (roll < 0.15F) return new GateAttack(10, 20.0F, 7.0, 46);
      if (roll < 0.40F) return new GateAttack(24, 21.0F, 12.0, 72);
      if (roll < 0.72F) return new GateAttack(48, 22.0F, 16.0, 96);
      return new GateAttack(120, 24.0F, 22.0, 150);
   }

   private static boolean tryEnkiduScaleBarrage(GilgameshEntity entity, ServerLevel level, EnkiduEntity target, long now, CompoundTag data) {
      int remaining = data.getInt(ENKIDU_BARRAGE_REMAINING);
      if (remaining > 0) {
         if (now < data.getLong(ENKIDU_BARRAGE_NEXT)) return true;
         if (entity.getCurrentMp() < 14.4) {
            data.putInt(ENKIDU_BARRAGE_REMAINING, 0);
            data.putLong(NEXT_GATE, now + 20L);
            return false;
         }
         fireGateVolley(entity, level, target, 100, 22.0F);
         entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - 14.4));
         remaining--;
         data.putInt(ENKIDU_BARRAGE_REMAINING, remaining);
         data.putLong(ENKIDU_BARRAGE_NEXT, now + 52L);
         if (remaining == 0) {
            data.putLong(ENKIDU_BARRAGE_COOLDOWN, now + 600L);
            // Leave a short recovery gap before a smaller treasury shot.
            data.putLong(NEXT_GATE, now + 80L);
         }
         return true;
      }
      if (now < data.getLong(ENKIDU_BARRAGE_COOLDOWN)) return false;
      ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
      float chance = switch (phase) {
         case PROBING -> 0.62F;
         case NORMAL -> 0.82F;
         case DECISIVE -> 0.92F;
      };
      if (entity.getCurrentMp() < 14.4 || entity.getRandom().nextFloat() >= chance) return false;
      int rounds = 2 + entity.getRandom().nextInt(2);
      data.putInt(ENKIDU_BARRAGE_REMAINING, rounds);
      data.putLong(ENKIDU_BARRAGE_NEXT, now);
      return true;
   }

   private static void triggerGateAnimation(GilgameshEntity entity, int count) {
      if (count <= 1) entity.triggerNamedActionAnimation("gate_single");
      else if (count <= 10) entity.triggerNamedActionAnimation("gate_small");
      else if (count >= 150) entity.triggerNamedActionAnimation("gate_duel");
      else entity.triggerNamedActionAnimation("gate_large");
   }

   private static GateFormation chooseFormation(GilgameshEntity entity, LivingEntity target, int count) {
      if (count <= 1) return GateFormation.SINGLE;
      if (count < 12) return entity.getRandom().nextBoolean() ? GateFormation.FRONTAL : GateFormation.FLANKS;
      if (count < 48) return switch (entity.getRandom().nextInt(4)) {
         case 0 -> GateFormation.FRONTAL;
         case 1 -> GateFormation.FLANKS;
         case 2 -> GateFormation.TARGET_RING;
         default -> GateFormation.SKY_RAIN;
      };
      return switch (entity.getRandom().nextInt(4)) {
         case 0 -> GateFormation.FRONTAL;
         case 1 -> GateFormation.FLANKS;
         case 2 -> GateFormation.TARGET_RING;
         default -> GateFormation.SKY_RAIN;
      };
   }

   private static GateFormation formationForWave(GateFormation base, int wave, int totalCount) {
      if (totalCount <= 1) return GateFormation.SINGLE;
      if (base == GateFormation.FRONTAL || totalCount >= 24) {
         return switch (wave % 4) {
            case 1 -> GateFormation.FLANKS;
            case 2 -> GateFormation.SKY_RAIN;
            case 3 -> GateFormation.TARGET_RING;
            default -> base;
         };
      }
      return base;
   }

   private static Vec3 gatePosition(GilgameshEntity entity, LivingEntity target, Vec3 forward, int index, int amount, GateFormation formation) {
      Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() * 0.72, 0);
      Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.55, 0);
      Vec3 right = forward.cross(new Vec3(0, 1, 0));
      if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
      right = right.normalize();
      Vec3 up = right.cross(forward).normalize();
      return switch (formation) {
         case SINGLE -> entityCenter.add(right.scale(index == 0 ? 0.9 : -0.9)).add(up.scale(0.35));
         case FRONTAL -> arrangedGatePosition(entity.position().add(0, 2.5, 0), forward, index, amount);
         case FLANKS -> {
            double side = index % 2 == 0 ? 1.0 : -1.0;
            int row = index / 2;
            yield targetCenter.add(right.scale(side * (6.0 + row * 0.7))).add(up.scale((row % 4 - 1.5) * 1.15)).subtract(forward.scale(3.0));
         }
         case TARGET_RING -> {
            double angle = (Math.PI * 2.0 * index) / Math.max(1, amount);
            double radius = 7.0 + (index % 3) * 1.5;
            yield targetCenter.add(new Vec3(Math.cos(angle) * radius, (index % 4 - 1.5) * 1.4, Math.sin(angle) * radius));
         }
         case SKY_RAIN -> {
            double angle = (Math.PI * 2.0 * index) / Math.max(1, amount);
            double radius = 4.0 + (index % 4) * 1.25;
            yield targetCenter.add(new Vec3(Math.cos(angle) * radius, 11.0 + (index % 3) * 1.2, Math.sin(angle) * radius));
         }
      };
   }

   private static boolean isProjectionCounterTarget(LivingEntity target) {
      if (target instanceof EmiyaArcherEntity) return true;
      if (target instanceof net.minecraft.server.level.ServerPlayer player) {
         var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "emiya_archer".equals(vars.servant_card_id);
      }
      return false;
   }

   private static void spawnProjectionCounterWave(ServerLevel level, GilgameshEntity gil, LivingEntity emiya, int count, int waveSize, int waveIndex) {
      if (!gil.isAlive() || !emiya.isAlive() || gil.level() != level) return;
      int amount = Math.min(waveSize, count - waveIndex * waveSize);
      Vec3 center = emiya.position().add(0, 3.0, 0);
      Vec3 forward = gil.position().add(0, gil.getBbHeight() * 0.55, 0).subtract(center).normalize();
      int columns = gateColumns(amount);
      for (int i=0;i<amount;i++) {
         Vec3 gate=arrangedGatePosition(center,forward,i,amount);
         Vec3 aim=gil.position().add(0,gil.getBbHeight()*0.55,0).subtract(gate).normalize();
         String weapon = weaponForSlot(emiya, waveIndex * waveSize + i);
         GilgameshGateWeaponProjectileEntity counter=new GilgameshGateWeaponProjectileEntity(level,emiya,gate,aim,weapon,0);
         counter.setLaunchDelay(24 + Math.min(16, (i / columns) * 3));
         counter.setSourceStyle(1); counter.setDuelToken(gil.getUUID()+":projection:"+waveIndex+":"+i); level.addFreshEntity(counter);
         VFXServerEffects.spawn(level,"servant_emiya_projection",gate,160.0);
      }
   }

   private static int gateColumns(int amount) {
      if (amount >= 100) return 25;
      if (amount >= 18) return 9;
      return Math.max(4, Math.min(8, amount));
   }

   private static Vec3 arrangedGatePosition(Vec3 center, Vec3 forward, int index, int amount) {
      if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0, 0, 1);
      forward = forward.normalize();
      Vec3 right = forward.cross(new Vec3(0, 1, 0));
      if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
      right = right.normalize();
      Vec3 up = right.cross(forward).normalize();
      int columns = gateColumns(amount);
      int row = index / columns;
      int column = index % columns;
      int rowCount = Math.min(columns, amount - row * columns);
      double horizontal = (column - (rowCount - 1) * 0.5) * (amount >= 100 ? 0.78 : 1.35);
      double vertical = row * (amount >= 100 ? 0.9 : 1.25);
      double depth = 1.8 + row * 0.22 + Math.abs(column - (rowCount - 1) * 0.5) * 0.035;
      return center.add(right.scale(horizontal)).add(up.scale(vertical)).subtract(forward.scale(depth));
   }

   private static String weaponForSlot(LivingEntity target, int index) {
      String preferred = preferredWeapon(target);
      int preferredIndex = java.util.Arrays.asList(GATE_WEAPONS).indexOf(preferred);
      int cycle = Math.floorMod(index, GATE_WEAPONS.length);
      if (cycle == 0) return preferred;
      int offset = Math.floorMod(preferredIndex + cycle, GATE_WEAPONS.length);
      return GATE_WEAPONS[offset];
   }

   private static String preferredWeapon(LivingEntity target) {
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON)) return "gram";
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.UNDEAD)) return "harpe";
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.MECHANICAL) || !target.onGround()) return "vajra";
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.GIANT)) return "fangtian_huaji";
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.SABER)) return "durandal";
      return GATE_WEAPONS[Math.floorMod(target.getId(), GATE_WEAPONS.length)];
   }

   private static void useCharisma(GilgameshEntity entity, ServerLevel level, CompoundTag data, long now) {
      AABB area = entity.getBoundingBox().inflate(20.0);
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, area, e -> e == entity || e.isAlliedTo(entity))) {
         ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0, false, true, true));
      }
      entity.setCurrentMp(entity.getCurrentMp() - 20.0);
      data.putLong(LAST_CHARISMA, now);
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0, entity.getZ(), 30, 2.5, 1.2, 2.5, 0.05);
   }
}
