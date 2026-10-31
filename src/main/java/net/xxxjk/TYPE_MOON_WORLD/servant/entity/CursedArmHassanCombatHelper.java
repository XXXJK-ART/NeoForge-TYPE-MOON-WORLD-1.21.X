package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantNavigationHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantNoblePhantasmResourceService;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.joml.Vector3f;

public final class CursedArmHassanCombatHelper {
   public static final String WIND_DODGE_CHARGES = "CursedArmWindDodgeCharges";
   public static final String WIND_DODGE_RESTORE_AT = "CursedArmWindDodgeRestoreAt";
   public static final int WIND_DODGE_MAX_CHARGES = 3;
   public static final int WIND_DODGE_RESTORE_TICKS = 400;
   private static final String LAST_DIRK_TICK = "CursedArmLastDirkTick";
   private static final String LAST_REPOSITION_TICK = "CursedArmLastRepositionTick";
   private static final String LAST_SHADOW_STEP_TICK = "CursedArmLastShadowStepTick";
   private static final String LAST_KNIFE_FEINT_TICK = "CursedArmLastKnifeFeintTick";
   private static final String LAST_SHADOW_LUNGE_TICK = "CursedArmLastShadowLungeTick";
   private static final String LAST_ZABANIYA_TICK = "CursedArmLastZabaniyaTick";
   private static final String ZABANIYA_POWER_SCALE = "CursedArmZabaniyaPowerScale";
   private static final String ZABANIYA_OVERDRAFT = "CursedArmZabaniyaOverdraft";
   public static final String ZABANIYA_WINDUP_UNTIL = "CursedArmZabaniyaWindupUntil";
   private static final String ZABANIYA_TARGET_ID = "CursedArmZabaniyaTargetId";
   private static final String LAST_STEALTH_TICK = "CursedArmLastStealthTick";
   private static final String CURSE_ATTACK_ID_TAG = "CursedArmCurseAttackApplied";
   private static final String CURSE_ARMOR_ID_TAG = "CursedArmCurseArmorApplied";
   private static final int DIRK_COOLDOWN = 40;
   private static final int QUICK_STAB_COOLDOWN = 30;
   private static final int REPOSITION_COOLDOWN = 100;
   private static final int SHADOW_STEP_COOLDOWN = 120;
   private static final int KNIFE_FEINT_COOLDOWN = 70;
   private static final int SHADOW_LUNGE_COOLDOWN = 85;
   private static final int ZABANIYA_COOLDOWN = 1400;
   private static final int ZABANIYA_WINDUP = 20;
   private static final float ZABANIYA_USE_CHANCE = 0.45F;
   private static final String LAST_PERSISTENT_STATE_TICK = "CursedArmLastPersistentStateTick";
   private static final net.minecraft.resources.ResourceLocation CURSE_ATTACK_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "cursed_arm_zabaniya_curse_attack");
   private static final net.minecraft.resources.ResourceLocation CURSE_ARMOR_ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "cursed_arm_zabaniya_curse_armor");
   private static final DustParticleOptions DARK_PARTICLE = new DustParticleOptions(new Vector3f(0.08F, 0.02F, 0.12F), 1.2F);

   private CursedArmHassanCombatHelper() {
   }

   public static void tick(CursedArmHassanEntity entity, ServantAiContext context) {
      long now = context.gameTick();
      tickPersistentState(entity);
      if (HassanZabaniyaService.isExtractedItem(entity.getMainHandItem())
         || HassanZabaniyaService.isExtractedItem(entity.getOffhandItem())) return;
      if (entity.getPersistentData().getLong(ZABANIYA_WINDUP_UNTIL) > now) {
         entity.getNavigation().stop();
         return;
      }

      LivingEntity target = context.target();
      if (target == null || target.isDeadOrDying() || EntityUtils.isImmunePlayerTarget(target)) {
         target = entity.getTarget();
      }
      if (target == null
         || target.isDeadOrDying()
         || EntityUtils.isImmunePlayerTarget(target)
         || isForbiddenTarget(target)
         || shouldAvoidPassiveFellowHassanTarget(entity, target)) {
         entity.setTarget(null);
         return;
      }

      entity.setTarget(target);
      entity.getLookControl().setLookAt(target, 30.0F, 30.0F);
      double distance = entity.distanceTo(target);

      if (tryBeginZabaniya(entity, target, distance, now)) {
         return;
      }
      if (distance >= 3.0 && (entity.getCurrentMp() < 8.0 || entity.getRandom().nextFloat() < 0.65F)
         && tryOrdinaryDirk(entity, target, distance, now)) return;
      if (tryShadowStepBackstab(entity, target, distance, now)) {
         return;
      }
      if (tryQuickStab(entity, target, distance, now)) {
         return;
      }
      if (tryShadowLunge(entity, target, distance, now)) {
         return;
      }
      if (tryKnifeFeint(entity, target, distance, now)) {
         return;
      }
      if (tryElbowStrike(entity, target, distance, now)) {
         return;
      }
      if (tryTripSweep(entity, target, distance, now)) {
         return;
      }
      if (tryThrowDirk(entity, target, distance, now)) {
         return;
      }
      if (tryOrdinaryDirk(entity, target, distance, now)) return;
      if (distance > 2.2) {
         boolean moving = ServantNavigationHelper.moveToTargetThrottled(
            entity,
            target,
            1.35,
            now,
            ServantNavigationHelper.SHORT_REPATH_INTERVAL,
            0.65,
            "HassanChasePath"
         );
         if ((!moving || shouldRepositionWhenBlocked(entity, target, distance)) && tryRepositionNearTarget(entity, target, distance, now)) {
            return;
         }
      } else if (!entity.isPerformingAction()) {
         entity.triggerAssassinStabAnimation();
         entity.doHurtTarget(target);
      }
   }

   public static void tickPersistentState(CursedArmHassanEntity entity) {
      if (entity == null || !entity.isAlive() || entity.level().isClientSide()) return;
      long now = entity.level().getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (data.contains(LAST_PERSISTENT_STATE_TICK) && data.getLong(LAST_PERSISTENT_STATE_TICK) == now) return;
      data.putLong(LAST_PERSISTENT_STATE_TICK, now);
      if (!data.contains(WIND_DODGE_CHARGES)) data.putInt(WIND_DODGE_CHARGES, WIND_DODGE_MAX_CHARGES);
      if (!data.contains(CursedArmHassanEntity.TAG_DIRK_AMMO)) {
         data.putInt(CursedArmHassanEntity.TAG_DIRK_AMMO, 16);
         data.putLong(CursedArmHassanEntity.TAG_DIRK_REPLENISH_TICK, now);
      }
      syncDirkStack(entity);
      tickStealth(entity, now);
      tickZabaniyaWindup(entity, now);
      tickCurseCleanup(entity);
   }

   public static void cancelZabaniyaWindup(CursedArmHassanEntity entity) {
      CompoundTag data = entity.getPersistentData();
      data.remove(ZABANIYA_WINDUP_UNTIL);
      data.remove(ZABANIYA_TARGET_ID);
      entity.setZabaniyaTargetId(0);
      entity.setNoBandages(false);
   }

   public static boolean tryDodge(CursedArmHassanEntity entity, DamageSource source) {
      return tryWindProtectionDodge(entity, source);
   }

   public static boolean tryWindProtectionDodge(LivingEntity entity, DamageSource source) {
      if (entity == null || source == null || !(source.getDirectEntity() instanceof Projectile)
         || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || source.is(DamageTypeTags.BYPASSES_RESISTANCE)
         || source.is(net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes.GUARANTEED_HITS)) return false;
      Vec3 incoming = source.getDirectEntity().position().subtract(entity.getEyePosition());
      if (incoming.lengthSqr() < 1.0E-4 || entity.getLookAngle().dot(incoming.normalize()) < 0.2) return false;
      CompoundTag data = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (data.getInt(WIND_DODGE_CHARGES) <= 0 && now >= data.getLong(WIND_DODGE_RESTORE_AT)) {
         data.putInt(WIND_DODGE_CHARGES, WIND_DODGE_MAX_CHARGES);
         data.remove(WIND_DODGE_RESTORE_AT);
      }
      int charges = data.getInt(WIND_DODGE_CHARGES);
      if (charges <= 0) return false;
      data.putInt(WIND_DODGE_CHARGES, charges - 1);
      if (charges == 1) data.putLong(WIND_DODGE_RESTORE_AT, now + WIND_DODGE_RESTORE_TICKS);
      spawnDodgeFx(entity);
      return true;
   }

   public static float applyWindProtectionReduction(LivingEntity entity, DamageSource source, float amount) {
      return amount > 0.0F && isWindDamage(source) ? amount * 0.5F : amount;
   }

   public static boolean isWindDamage(DamageSource source) {
      if (source instanceof net.xxxjk.TYPE_MOON_WORLD.servant.combat.CombatDamageSource combat
         && combat.origin() != null && combat.origin().skill() != null) {
         return "invisible_air_hammer".equals(combat.origin().skill());
      }
      return false;
   }

   public static void clearNonServantTargeting(CursedArmHassanEntity entity) {
      if (!entity.hasEffect(MobEffects.INVISIBILITY) || !(entity.level() instanceof ServerLevel level)) {
         return;
      }
      AABB box = entity.getBoundingBox().inflate(18.0);
      for (Mob mob : level.getEntitiesOfClass(Mob.class, box, mob -> mob.getTarget() == entity && !(mob instanceof ServantEntity))) {
         mob.setTarget(null);
      }
   }

   public static boolean refusesToHarm(ServantEntity entity, LivingEntity target) {
      return (entity instanceof CursedArmHassanEntity
         || entity instanceof HundredFacesHassanEntity
         || entity instanceof HundredFacesHassanPersonaEntity) && isForbiddenTarget(target);
   }

   public static boolean refusesToHarm(LivingEntity entity, LivingEntity target) {
      if (entity == null || target == null) return false;
      if (entity instanceof net.minecraft.server.level.ServerPlayer player
         && net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.isActiveCard(player)) {
         return isForbiddenTarget(target);
      }
      var definition = ServantIdentityHelper.definitionOf(entity);
      return definition != null && java.util.Set.of("cursed_arm_hassan", "hundred_faces_hassan").contains(definition.id())
         && isForbiddenTarget(target);
   }

   public static boolean shouldAvoidPassiveFellowHassanTarget(ServantEntity entity, LivingEntity target) {
      return entity instanceof CursedArmHassanEntity && isHassanIdentity(target);
   }

   public static boolean isProtectedPigKind(LivingEntity target) {
      if (target == null) {
         return false;
      }
      EntityType<?> type = target.getType();
      return type == EntityType.PIG
         || type == EntityType.PIGLIN
         || type == EntityType.PIGLIN_BRUTE
         || type == EntityType.ZOMBIFIED_PIGLIN
         || type == EntityType.HOGLIN
         || type == EntityType.ZOGLIN;
   }

   public static boolean isForbiddenTarget(LivingEntity target) {
      return isProtectedPigKind(target) || isHassanIdentity(target);
   }

   private static boolean isHassanIdentity(LivingEntity target) {
      if (target == null) return false;
      if (target instanceof CursedArmHassanEntity) return true;
      var definition = ServantIdentityHelper.definitionOf(target);
      return definition != null && java.util.Set.of("cursed_arm_hassan", "hundred_faces_hassan", "shadow_hassan", "fanatic_assassin")
         .contains(definition.id());
   }

   public static boolean isHumanoidInstantDeathTarget(LivingEntity target) {
      if (target == null || !target.isAlive() || EntityUtils.isImmunePlayerTarget(target) || isForbiddenTarget(target)) {
         return false;
      }
      if (target instanceof WitherBoss || target instanceof EnderDragon || target instanceof Warden) {
         return false;
      }
      if (target.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)
         || target.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD)
         || target instanceof Animal) {
         return false;
      }
      if (target instanceof Player || target instanceof AbstractVillager || target instanceof WanderingTrader || target instanceof AbstractIllager || target instanceof Witch) {
         return true;
      }
      if (ServantIdentityHelper.isServantLike(target)) {
         return ServantIdentityHelper.hasTrait(target, ServantTraitTag.HUMANOID)
            || ServantIdentityHelper.hasTrait(target, ServantTraitTag.LIVING_HUMAN);
      }
      if (target instanceof PathfinderMob && !(target instanceof Enemy)) {
         return false;
      }
      return target.getPersistentData().getBoolean("TypeMoonHumanoid")
         || target.getPersistentData().getBoolean("TypeMoonLivingHuman");
   }

   private static void tickStealth(CursedArmHassanEntity entity, long now) {
      LivingEntity target = entity.getTarget();
      if (!HassanPresenceService.enabled(entity) && target != null && target.isAlive() && entity.distanceToSqr(target) >= 36.0
         && entity.getCurrentMp() >= 18.0 && now >= entity.getPersistentData().getLong(LAST_STEALTH_TICK)
         && HassanPresenceService.set(entity, true)) {
         entity.setCurrentMp(entity.getCurrentMp() - 18.0);
         entity.getPersistentData().putLong(LAST_STEALTH_TICK, now + 120);
      }
      HassanPresenceService.tick(entity);
      clearNonServantTargeting(entity);
   }

   private static void tickZabaniyaWindup(CursedArmHassanEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      long until = data.getLong(ZABANIYA_WINDUP_UNTIL);
      if (until <= 0L || now < until) {
         if (until > 0L && entity.level().getEntity(data.getInt(ZABANIYA_TARGET_ID)) instanceof LivingEntity target && target.isAlive()) {
            if (EntityUtils.isImmunePlayerTarget(target)) {
               data.remove(ZABANIYA_WINDUP_UNTIL);
               data.remove(ZABANIYA_TARGET_ID);
               entity.setZabaniyaTargetId(0);
               entity.setNoBandages(false);
               entity.setTarget(null);
               return;
            }
            entity.getLookControl().setLookAt(target, 45.0F, 45.0F);
         }
         return;
      }
      data.remove(ZABANIYA_WINDUP_UNTIL);
      LivingEntity target = null;
      if (entity.level().getEntity(data.getInt(ZABANIYA_TARGET_ID)) instanceof LivingEntity living) {
         target = living;
      }
      data.remove(ZABANIYA_TARGET_ID);
      if (target == null || !target.isAlive() || entity.distanceTo(target) > entity.getZabaniyaRange() + 1.5
         || !entity.getSensing().hasLineOfSight(target)) {
         entity.setZabaniyaTargetId(0);
         entity.setNoBandages(false);
         return;
      }
      resolveZabaniya(entity, target);
      entity.setZabaniyaTargetId(0);
   }

   private static boolean tryBeginZabaniya(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      if (!HassanZabaniyaService.canExtract(target)) return false;
      CompoundTag data = entity.getPersistentData();
      ServantNoblePhantasmResourceService.CastDecision resource =
         ServantNoblePhantasmResourceService.evaluateNpcCast(entity, 80.0);
      if (!resource.allowed() || ServantNoblePhantasmResourceService.isOverdraftWeak(entity)
          || distance > entity.getZabaniyaRange()
          || data.contains(LAST_ZABANIYA_TICK) && now - data.getLong(LAST_ZABANIYA_TICK) < ZABANIYA_COOLDOWN
          || !entity.getSensing().hasLineOfSight(target)) {
         return false;
      }
      if (entity.getRandom().nextFloat() > ZABANIYA_USE_CHANCE) {
         data.putLong(LAST_ZABANIYA_TICK, now - ZABANIYA_COOLDOWN + 40L);
         return false;
      }
      data.putDouble(ZABANIYA_POWER_SCALE, resource.powerScale());
      data.putBoolean(ZABANIYA_OVERDRAFT, resource.overdraft());
      ServantNoblePhantasmResourceService.commitNpcCast(entity, resource);
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.begin(entity, "cursed_arm_hassan_zabaniya", ZABANIYA_COOLDOWN, ticks -> entity.getPersistentData().putLong(LAST_ZABANIYA_TICK, entity.level().getGameTime() - (ZABANIYA_COOLDOWN) + ticks), () -> { });
      net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.deferEffect(entity, () -> {
      data.putLong(LAST_ZABANIYA_TICK, entity.level().getGameTime());
      data.putLong(ZABANIYA_WINDUP_UNTIL, entity.level().getGameTime() + ZABANIYA_WINDUP);
      data.putInt(ZABANIYA_TARGET_ID, target.getId());
      entity.setZabaniyaTargetId(target.getId());
      entity.setNoBandages(true);
      entity.getPersistentData().putLong("CursedArmBandagesRestoreTick", entity.level().getGameTime() + ZABANIYA_WINDUP + 100L);
      entity.triggerZabaniyaAnimation();
      entity.getNavigation().stop();
      if (entity.level() instanceof ServerLevel level) {
         VFXServerEffects.spawn(level, "servant_cursed_arm_zabaniya_windup", entity, 96.0);
      }
      spawnZabaniyaWindupFx(entity, target);

      });
      return true;
   }

   private static boolean tryThrowDirk(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      if (isForbiddenTarget(target)) {
         entity.setTarget(null);
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      if (distance < 3.0 || distance > 18.0 || entity.getCurrentMp() < 8.0
         || data.contains(LAST_DIRK_TICK) && now - data.getLong(LAST_DIRK_TICK) < DIRK_COOLDOWN) {
         return false;
      }
      int ammo = data.getInt(CursedArmHassanEntity.TAG_DIRK_AMMO);
      if (ammo < 3) return false;
      data.putLong(LAST_DIRK_TICK, now);
      data.putInt(CursedArmHassanEntity.TAG_DIRK_AMMO, ammo - 3);
      syncDirkStack(entity);
      entity.setCurrentMp(entity.getCurrentMp() - 8.0);
      entity.triggerDirkThrowAnimation();
       Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
       for (int spread = -1; spread <= 1; spread++) {
          DirkProjectileEntity projectile = new DirkProjectileEntity(entity.level(), entity);
          projectile.setDamage(14.0F);
          projectile.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ());
          projectile.setMaxDistance(18.0);
          Vec3 direction = aim.subtract(projectile.position()).normalize().yRot((float)Math.toRadians(spread * 5.0));
          projectile.shoot(direction.x, direction.y + 0.03, direction.z, 2.2F, 0.0F);
          entity.level().addFreshEntity(projectile);
       }
      if (entity.level() instanceof ServerLevel level) {
         level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.75F, 1.45F);
      }
      return true;
   }

   private static boolean tryOrdinaryDirk(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      // Match the ordinary knife item's right-click behavior: the throw itself
      // does not require an entity line of sight; the projectile handles blocks.
      if (distance < 3 || distance > 18 || entity.isPerformingAction()
         || now < entity.getPersistentData().getLong("CursedArmOrdinaryDirkReady")) return false;
      CompoundTag data = entity.getPersistentData();
      int ammo = Math.max(0, Math.min(16, data.getInt(CursedArmHassanEntity.TAG_DIRK_AMMO)));
      if (ammo < 1) return false;
      entity.getPersistentData().putLong("CursedArmOrdinaryDirkReady", now + 16);
      data.putInt(CursedArmHassanEntity.TAG_DIRK_AMMO, ammo - 1);
      syncDirkStack(entity);
      entity.triggerDirkThrowAnimation();
      DirkProjectileEntity projectile = new DirkProjectileEntity(entity.level(), entity);
      projectile.setDamage(10.0F);
      projectile.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
      projectile.setMaxDistance(18.0);
      Vec3 aim = target.position().add(0, target.getBbHeight() * 0.55, 0).subtract(projectile.position());
      projectile.shoot(aim.x, aim.y, aim.z, 1.9F, 0.0F);
      entity.level().addFreshEntity(projectile);
      entity.level().playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.75F, 1.45F);
      return true;
   }

   private static void syncDirkStack(CursedArmHassanEntity entity) {
      CompoundTag data = entity.getPersistentData();
      int ammo = Math.max(0, Math.min(16, data.getInt(CursedArmHassanEntity.TAG_DIRK_AMMO)));
      ItemStack held = entity.getOffhandItem();
      if (HassanZabaniyaService.isExtractedItem(held) || HassanZabaniyaService.isExtractedItem(entity.getMainHandItem())) return;
      if (ammo == 0) {
         ammo = 16;
         data.putInt(CursedArmHassanEntity.TAG_DIRK_AMMO, ammo);
      }
      if (held.is(ModItems.DIRK_SMALL_KNIFE.get())) {
         held.setCount(1);
         held.setDamageValue(16 - ammo);
      } else if (held.isEmpty()) {
         ItemStack knife = new ItemStack(ModItems.DIRK_SMALL_KNIFE.get());
         knife.setDamageValue(16 - ammo);
         entity.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, knife);
      }
   }

   private static boolean shouldRepositionWhenBlocked(CursedArmHassanEntity entity, LivingEntity target, double distance) {
      if (distance <= 3.0 || distance > 20.0 || entity.isPerformingAction()) {
         return false;
      }
      if (!entity.getSensing().hasLineOfSight(target) && distance <= 12.0) {
         return true;
      }
      var path = entity.getNavigation().createPath(target, 0);
      return path == null || !path.canReach();
   }

   private static boolean tryRepositionNearTarget(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (now - data.getLong(LAST_REPOSITION_TICK) < REPOSITION_COOLDOWN || !(entity.level() instanceof ServerLevel level)) {
         return false;
      }

      Vec3 away = entity.position().subtract(target.position());
      Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(entity.getRandom().nextDouble() - 0.5, 0.0, entity.getRandom().nextDouble() - 0.5);
      }
      horizontal = horizontal.normalize();
      Vec3 side = new Vec3(-horizontal.z, 0.0, horizontal.x);
      Vec3[] candidates = new Vec3[]{
         target.position().add(horizontal.scale(1.65)),
         target.position().add(side.scale(1.75)),
         target.position().subtract(side.scale(1.75)),
         target.position().subtract(target.getLookAngle().normalize().scale(1.45))
      };

      for (Vec3 candidate : candidates) {
         BlockPos feet = findSafeTeleportFeet(level, BlockPos.containing(candidate.x, target.getY(), candidate.z));
         if (feet == null) {
            continue;
         }
         Vec3 destination = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
         if (!level.noCollision(entity, entity.getBoundingBox().move(destination.subtract(entity.position())))) {
            continue;
         }
         data.putLong(LAST_REPOSITION_TICK, now);
         spawnShadowStepFx(entity);
         entity.teleportTo(destination.x, destination.y, destination.z);
         entity.setDeltaMovement(Vec3.ZERO);
         entity.fallDistance = 0.0F;
         entity.getNavigation().stop();
         entity.setTarget(target);
         entity.faceToward(target.position());
         entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
         entity.triggerShadowStepAnimation();
         return true;
      }
      return false;
   }

   private static BlockPos findSafeTeleportFeet(ServerLevel level, BlockPos anchor) {
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

   private static boolean tryShadowStepBackstab(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      if (isForbiddenTarget(target)) {
         entity.setTarget(null);
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      if (distance < 3.0 || distance > 8.0 || entity.getCurrentMp() < 10.0 || now - data.getLong(LAST_SHADOW_STEP_TICK) < SHADOW_STEP_COOLDOWN) {
         return false;
      }
      if (!entity.getSensing().hasLineOfSight(target)) {
         return false;
      }

      if (!(entity.level() instanceof ServerLevel level)) return false;
      Vec3 behind = target.position().subtract(target.getLookAngle().normalize().scale(1.35));
      BlockPos feet = findSafeTeleportFeet(level, BlockPos.containing(behind.x, target.getY(), behind.z));
      if (feet == null) return false;
      Vec3 destination = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
      if (!level.noCollision(entity, entity.getBoundingBox().move(destination.subtract(entity.position())))) return false;
      Vec3 landingEye = destination.add(0.0, entity.getBbHeight() * 0.55, 0.0);
      if (level.clip(new net.minecraft.world.level.ClipContext(entity.getEyePosition(), landingEye,
         net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, entity)).getType()
         != net.minecraft.world.phys.HitResult.Type.MISS) return false;

      data.putLong(LAST_SHADOW_STEP_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 10.0);
      spawnShadowStepFx(entity);
      entity.teleportTo(destination.x, destination.y, destination.z);
      entity.setDeltaMovement(Vec3.ZERO);
      entity.fallDistance = 0.0F;
      entity.getNavigation().stop();
      entity.setTarget(target);
      entity.faceToward(target.position());
      entity.getLookControl().setLookAt(target, 60.0F, 60.0F);
      entity.triggerShadowStepAnimation();
      if (dealScaledKnifeDamage(entity, target, 1.65F)) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
      }
      spawnBackstabFx(entity, target);
      return true;
   }

   private static boolean tryShadowLunge(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      if (isForbiddenTarget(target)) {
         entity.setTarget(null);
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      if (distance < 2.0 || distance > 6.5 || entity.getCurrentMp() < 7.0 || now - data.getLong(LAST_SHADOW_LUNGE_TICK) < SHADOW_LUNGE_COOLDOWN) {
         return false;
      }
      if (!entity.getSensing().hasLineOfSight(target) || entity.isPerformingAction()) {
         return false;
      }

      Vec3 dir = target.position().subtract(entity.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return false;
      }
      horizontal = horizontal.normalize();
      data.putLong(LAST_SHADOW_LUNGE_TICK, now);
      entity.setCurrentMp(entity.getCurrentMp() - 7.0);
      entity.faceVector(horizontal);
      entity.triggerShadowStepAnimation();
      entity.setDeltaMovement(horizontal.x * 1.65, Math.max(entity.getDeltaMovement().y, 0.18), horizontal.z * 1.65);
      entity.hasImpulse = true;
      boolean hit = dealScaledKnifeDamage(entity, target, 1.05F);
      if (!hit) return true;
      target.push(horizontal.x * 0.85, 0.24, horizontal.z * 0.85);
      target.hurtMarked = true;
      spawnShadowStepFx(entity);
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
         level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 1.6F);
      }
      return true;
   }

   private static boolean tryKnifeFeint(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      if (isForbiddenTarget(target)) {
         entity.setTarget(null);
         return false;
      }
      CompoundTag data = entity.getPersistentData();
      if (distance < 2.5 || distance > 9.0 || entity.getCurrentMp() < 5.0 || now - data.getLong(LAST_KNIFE_FEINT_TICK) < KNIFE_FEINT_COOLDOWN) {
         return false;
      }
      if (!entity.getSensing().hasLineOfSight(target) || entity.isPerformingAction()) {
         return false;
      }
      int ammo = data.getInt(CursedArmHassanEntity.TAG_DIRK_AMMO);
      if (ammo < 1) return false;
      data.putLong(LAST_KNIFE_FEINT_TICK, now);
      data.putInt(CursedArmHassanEntity.TAG_DIRK_AMMO, ammo - 1);
      syncDirkStack(entity);
      entity.setCurrentMp(entity.getCurrentMp() - 5.0);
      entity.faceToward(target.position());
      entity.triggerDirkThrowAnimation();
      DirkProjectileEntity projectile = new DirkProjectileEntity(entity.level(), entity);
      projectile.setDamage(10.0F);
      projectile.setPos(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
      projectile.setMaxDistance(18.0);
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(projectile.position()).normalize();
      projectile.shoot(aim.x, aim.y, aim.z, 2.2F, 0.0F);
      entity.level().addFreshEntity(projectile);
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.62, target.getZ(), 10, 0.28, 0.22, 0.28, 0.08);
         level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.65F, 1.75F);
      }
      return true;
   }

   private static boolean tryQuickStab(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (isForbiddenTarget(target) || distance > 2.6 || entity.isPerformingAction()
         || now - data.getLong("CursedArmLastQuickStabTick") < QUICK_STAB_COOLDOWN) return false;
      data.putLong("CursedArmLastQuickStabTick", now);
      entity.triggerAssassinStabAnimation();
      dealScaledKnifeDamage(entity, target, 0.8F);
      return true;
   }

   private static boolean tryElbowStrike(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (isForbiddenTarget(target) || distance > 2.8 || entity.getCurrentMp() < 6.0
         || now - data.getLong("CursedArmLastElbowTick") < 80L || entity.isPerformingAction()) return false;
      data.putLong("CursedArmLastElbowTick", now);
      entity.setCurrentMp(entity.getCurrentMp() - 6.0);
      entity.triggerAssassinStabAnimation();
      if (dealScaledKnifeDamage(entity, target, 0.90F)) {
         target.getPersistentData().putLong(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.TAG_STUN_UNTIL, now + 8L);
      }
      return true;
   }

   private static boolean tryTripSweep(CursedArmHassanEntity entity, LivingEntity target, double distance, long now) {
      CompoundTag data = entity.getPersistentData();
      if (isForbiddenTarget(target) || distance > 2.5 || entity.getCurrentMp() < 8.0
         || now - data.getLong("CursedArmLastTripTick") < 160L || entity.isPerformingAction()) return false;
      data.putLong("CursedArmLastTripTick", now);
      entity.setCurrentMp(entity.getCurrentMp() - 8.0);
      entity.triggerAssassinStabAnimation();
      if (dealScaledKnifeDamage(entity, target, 0.65F)) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
      }
      return true;
   }

   private static boolean dealScaledKnifeDamage(CursedArmHassanEntity entity, LivingEntity target, float scale) {
      if (isForbiddenTarget(target)) {
         entity.setTarget(null);
         return false;
      }
      AttributeInstance attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
      float damage = (float)((attack != null ? attack.getValue() : 8.0) * scale);
      return target.hurt(entity.damageSources().mobAttack(entity), damage);
   }

   private static void resolveZabaniya(CursedArmHassanEntity entity, LivingEntity target) {
      if (EntityUtils.isImmunePlayerTarget(target)) {
         entity.setTarget(null);
         return;
      }
      if (isForbiddenTarget(target)) {
         entity.setTarget(null);
         return;
      }
      boolean extracted = HassanZabaniyaService.resolve(entity, target, 55.0F, true);
      spawnZabaniyaImpactFx(entity, target, extracted);
   }

   public static void applyZabaniyaCurse(LivingEntity target) {
      int duration = 160;
      AttributeInstance attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null && attack.getModifier(CURSE_ATTACK_ID) == null) {
         attack.addTransientModifier(new AttributeModifier(CURSE_ATTACK_ID, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         target.getPersistentData().putBoolean(CURSE_ATTACK_ID_TAG, true);
      }
      AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
      if (armor != null && armor.getModifier(CURSE_ARMOR_ID) == null) {
         armor.addTransientModifier(new AttributeModifier(CURSE_ARMOR_ID, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         target.getPersistentData().putBoolean(CURSE_ARMOR_ID_TAG, true);
      }
      long expiresAt = target.level().getGameTime() + duration;
      target.getPersistentData().putLong("CursedArmCurseUntil", expiresAt);
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
      TYPE_MOON_WORLD.queueServerWork(duration, () -> clearZabaniyaCurse(target, expiresAt));
   }

   private static void clearZabaniyaCurse(LivingEntity target, long expectedExpiry) {
      if (target == null || target.getPersistentData().getLong("CursedArmCurseUntil") != expectedExpiry) return;
      AttributeInstance attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) attack.removeModifier(CURSE_ATTACK_ID);
      AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
      if (armor != null) armor.removeModifier(CURSE_ARMOR_ID);
      target.getPersistentData().remove("CursedArmCurseUntil");
      target.getPersistentData().remove(CURSE_ATTACK_ID_TAG);
      target.getPersistentData().remove(CURSE_ARMOR_ID_TAG);
   }

   private static void tickCurseCleanup(CursedArmHassanEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || entity.tickCount % 20 != 0) {
         return;
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(32.0), e -> e.getPersistentData().getLong("CursedArmCurseUntil") > 0L)) {
         if (living.getPersistentData().getLong("CursedArmCurseUntil") > level.getGameTime()) {
            continue;
         }
         AttributeInstance attack = living.getAttribute(Attributes.ATTACK_DAMAGE);
         if (attack != null) {
            attack.removeModifier(CURSE_ATTACK_ID);
         }
         AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
         if (armor != null) {
            armor.removeModifier(CURSE_ARMOR_ID);
         }
         living.getPersistentData().remove("CursedArmCurseUntil");
         living.getPersistentData().remove(CURSE_ATTACK_ID_TAG);
         living.getPersistentData().remove(CURSE_ARMOR_ID_TAG);
      }
   }

   private static void spawnZabaniyaWindupFx(CursedArmHassanEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 start = entity.position().add(0.0, entity.getBbHeight() * 0.65, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      for (double t = 0.0; t <= 1.0; t += 0.12) {
         Vec3 pos = start.lerp(end, t);
         level.sendParticles(DARK_PARTICLE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
      level.playSound(null, entity.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.65F, 0.62F);
   }

   private static void spawnZabaniyaImpactFx(CursedArmHassanEntity entity, LivingEntity target, boolean killed) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_cursed_arm_zabaniya_impact", target.position(), 96.0);
      level.sendParticles(killed ? ParticleTypes.DRAGON_BREATH : ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), killed ? 36 : 20, 0.35, 0.45, 0.35, 0.04);
      level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.25, 0.25, 0.25, 0.08);
      level.playSound(null, target.blockPosition(), killed ? SoundEvents.WITHER_DEATH : SoundEvents.WITHER_HURT, SoundSource.HOSTILE, killed ? 0.9F : 0.7F, 1.25F);
   }

   private static void spawnDodgeFx(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 10, 0.18, 0.28, 0.18, 0.04);
      level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 8, 0.2, 0.25, 0.2, 0.03);
      level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.65F, 1.55F);
   }

   private static void spawnShadowStepFx(CursedArmHassanEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.45, entity.getZ(), 18, 0.25, 0.35, 0.25, 0.04);
      level.sendParticles(DARK_PARTICLE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 10, 0.2, 0.25, 0.2, 0.01);
      level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.75F, 1.35F);
   }

   private static void spawnBackstabFx(CursedArmHassanEntity entity, LivingEntity target) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 8, 0.18, 0.22, 0.18, 0.03);
      level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 0.8F, 1.25F);
   }
}
