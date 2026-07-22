package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.neoforged.neoforge.common.Tags;

public final class PaleRiderInfectionService {
   public static final String TAG_LEVEL = "PaleRiderInfectionLevel";
   public static final String TAG_OWNER = "PaleRiderInfectionOwner";
   public static final String TAG_UNTIL = "PaleRiderInfectionUntil";
   public static final String TAG_IMMUNE_UNTIL = "PaleRiderInfectionImmuneUntil";
   public static final String TAG_CONTROLLED = "PaleRiderControlled";
   public static final String TAG_PREVIOUS_NO_AI = "PaleRiderPreviousNoAi";
   private static final String TAG_LAST_SERVICE_TICK = "PaleRiderInfectionLastServiceTick";
   private static final String TAG_NEXT_CONTROLLED_ATTACK = "PaleRiderControlledNextAttack";

   private PaleRiderInfectionService() {
   }

   public static boolean infect(LivingEntity target, LivingEntity owner, int addedLevels) {
      if (target == null || owner == null || !target.isAlive() || target == owner || target.isAlliedTo(owner)) {
         return false;
      }
      long now = target.level().getGameTime();
      CompoundTag data = target.getPersistentData();
      if (data.getLong(TAG_IMMUNE_UNTIL) > now) {
         return false;
      }
      int previous = getLevel(target);
      int next = Math.min(InfectionRules.MAX_LEVEL, previous + Math.max(1, addedLevels));
      data.putInt(TAG_LEVEL, next);
      data.putUUID(TAG_OWNER, owner.getUUID());
      data.putLong(TAG_UNTIL, now + InfectionRules.DURATION_TICKS);
      target.addEffect(new MobEffectInstance(ModMobEffects.PALE_RIDER_INFECTION, InfectionRules.DURATION_TICKS, next - 1, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, InfectionRules.DURATION_TICKS, 0, false, true, true));
      if (next > previous && canControl(target) && target.getRandom().nextDouble() < controlChance(target, next)) {
         beginControl((Mob)target, owner);
      }
      return next > previous;
   }

   public static void tick(LivingEntity target) {
      int level = getLevel(target);
      if (level <= 0 || !(target.level() instanceof ServerLevel serverLevel)) {
         return;
      }
      long now = serverLevel.getGameTime();
      if (target.getPersistentData().getLong(TAG_LAST_SERVICE_TICK) == now) {
         return;
      }
      target.getPersistentData().putLong(TAG_LAST_SERVICE_TICK, now);
      if (target.getPersistentData().getLong(TAG_UNTIL) <= now) {
         cleanse(target, false);
         return;
      }
      LivingEntity owner = getOwner(serverLevel, target);
      if (owner == null || !owner.isAlive()) {
         cleanse(target, false);
         return;
      }
      boolean controlled = isControlled(target);
      if (target.tickCount % 20 == Math.floorMod(target.getId(), 20)) {
         if (!controlled) {
            target.hurt(owner.damageSources().source(PaleRiderDamageTypes.INFECTION, owner), 10.0F);
         }
         spread(serverLevel, target, owner, level);
      }
      if (controlled && target instanceof Mob mob) {
         tickControlledMob(serverLevel, mob, owner, now);
      }
   }

   private static void spread(ServerLevel level, LivingEntity source, LivingEntity owner, int infectionLevel) {
      if (source.getRandom().nextDouble() >= InfectionRules.spreadChance(infectionLevel)) {
         return;
      }
      AABB area = source.getBoundingBox().inflate(5.0);
      level.getEntitiesOfClass(LivingEntity.class, area, target -> target != source && target != owner && target.isAlive() && !target.isAlliedTo(owner))
         .stream().min((left, right) -> Double.compare(left.distanceToSqr(source), right.distanceToSqr(source)))
         .ifPresent(target -> infect(target, owner, 1));
   }

   public static void cleanse(LivingEntity target, boolean grantImmunity) {
      CompoundTag data = target.getPersistentData();
      data.remove(TAG_LEVEL);
      data.remove(TAG_OWNER);
      data.remove(TAG_UNTIL);
      data.remove(TAG_LAST_SERVICE_TICK);
      target.removeEffect(ModMobEffects.PALE_RIDER_INFECTION);
      if (grantImmunity) {
         data.putLong(TAG_IMMUNE_UNTIL, target.level().getGameTime() + InfectionRules.IMMUNITY_TICKS);
      }
      endControl(target);
   }

   public static int getLevel(LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      if (data.getLong(TAG_UNTIL) <= target.level().getGameTime()) {
         return 0;
      }
      return Math.max(0, Math.min(InfectionRules.MAX_LEVEL, data.getInt(TAG_LEVEL)));
   }

   public static boolean isInfected(LivingEntity target) {
      return getLevel(target) > 0;
   }

   public static boolean isControlled(Entity entity) {
      return entity != null && entity.getPersistentData().getBoolean(TAG_CONTROLLED)
         && entity.getPersistentData().hasUUID(TAG_OWNER);
   }

   public static LivingEntity getCommandTarget(Mob mob) {
      if (!(mob.level() instanceof ServerLevel level)) return null;
      LivingEntity owner = getOwner(level, mob);
      if (!(owner instanceof Mob ownerMob)) return null;
      LivingEntity target = ownerMob.getTarget();
      return isValidCommandTarget(mob, owner, target) ? target : null;
   }

   public static boolean arePaleRiderAllies(Entity first, Entity second) {
      if (first == null || second == null) return false;
      PaleRiderEntity firstOwner = getPaleRiderController(first);
      if (firstOwner != null && (second == firstOwner || firstOwner.isAlliedTo(second) || second.isAlliedTo(firstOwner))) {
         return true;
      }
      PaleRiderEntity secondOwner = getPaleRiderController(second);
      return secondOwner != null && (first == secondOwner || secondOwner.isAlliedTo(first) || first.isAlliedTo(secondOwner));
   }

   private static boolean canControl(LivingEntity target) {
      return target instanceof Mob && !(target instanceof Player) && !(target instanceof ServantEntity) && !target.getType().is(Tags.EntityTypes.BOSSES);
   }

   private static double controlChance(LivingEntity target, int level) {
      net.minecraft.resources.ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
      boolean vanilla = typeId != null && "minecraft".equals(typeId.getNamespace());
      return InfectionRules.ordinaryControlChance(level, vanilla);
   }

   private static void beginControl(Mob mob, LivingEntity owner) {
      CompoundTag data = mob.getPersistentData();
      if (!data.getBoolean(TAG_CONTROLLED)) {
         data.putBoolean(TAG_PREVIOUS_NO_AI, mob.isNoAi());
      }
      data.putBoolean(TAG_CONTROLLED, true);
      mob.setNoAi(false);
      if (owner instanceof Mob ownerMob && ownerMob.getTarget() == mob) {
         ownerMob.setTarget(null);
      }
      mob.setTarget(owner instanceof Mob ownerMob ? ownerMob.getTarget() : null);
   }

   private static void tickControlledMob(ServerLevel level, Mob mob, LivingEntity owner, long now) {
      if (mob.tickCount % 5 == Math.floorMod(mob.getId(), 5)) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
            mob.getX(), mob.getY() + mob.getBbHeight() * 0.55, mob.getZ(), 2, mob.getBbWidth() * 0.35, mob.getBbHeight() * 0.35, mob.getBbWidth() * 0.35, 0.005);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.ASH,
            mob.getX(), mob.getY() + mob.getBbHeight() * 0.65, mob.getZ(), 3, mob.getBbWidth() * 0.4, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.4, 0.01);
      }

      LivingEntity commandTarget = owner instanceof Mob ownerMob ? ownerMob.getTarget() : null;
      if (!isValidCommandTarget(mob, owner, commandTarget)) {
         mob.setTarget(null);
         mob.setAggressive(false);
         Vec3 gatheringPosition = gatheringPosition(mob, owner);
         if (mob.distanceToSqr(gatheringPosition) > 2.25) {
            mob.getNavigation().moveTo(gatheringPosition.x, gatheringPosition.y, gatheringPosition.z, 1.05);
         } else {
            mob.getNavigation().stop();
         }
         return;
      }

      mob.setTarget(commandTarget);
      mob.setAggressive(true);
      mob.getLookControl().setLookAt(commandTarget, 30.0F, 30.0F);
      mob.getNavigation().moveTo(commandTarget, 1.0);
      double reach = mob.getBbWidth() + commandTarget.getBbWidth() + 1.25;
      if (mob.distanceToSqr(commandTarget) <= reach * reach
         && mob.getSensing().hasLineOfSight(commandTarget)
         && now >= mob.getPersistentData().getLong(TAG_NEXT_CONTROLLED_ATTACK)) {
         mob.getPersistentData().putLong(TAG_NEXT_CONTROLLED_ATTACK, now + 20L);
         mob.swing(InteractionHand.MAIN_HAND, true);
         if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.doHurtTarget(commandTarget);
         } else {
            commandTarget.hurt(mob.damageSources().mobAttack(mob), 2.0F);
         }
      }
   }

   private static Vec3 gatheringPosition(Mob mob, LivingEntity owner) {
      int hash = mob.getUUID().hashCode() & Integer.MAX_VALUE;
      double angle = Math.toRadians(hash % 360);
      double radius = 2.5 + (hash / 360 % 3) * 1.25;
      return owner.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
   }

   private static boolean isValidCommandTarget(Mob mob, LivingEntity owner, LivingEntity target) {
      return target != null && target != mob && target.isAlive() && !arePaleRiderAllies(owner, target);
   }

   private static void endControl(LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      boolean wasControlled = data.getBoolean(TAG_CONTROLLED);
      boolean previousNoAi = data.getBoolean(TAG_PREVIOUS_NO_AI);
      data.remove(TAG_CONTROLLED);
      data.remove(TAG_PREVIOUS_NO_AI);
      data.remove(TAG_NEXT_CONTROLLED_ATTACK);
      if (wasControlled && target instanceof Mob mob) {
         mob.setNoAi(previousNoAi);
         mob.setTarget(null);
      }
   }

   public static LivingEntity getOwner(ServerLevel level, LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      if (!data.hasUUID(TAG_OWNER)) {
         return null;
      }
      UUID id = data.getUUID(TAG_OWNER);
      return level.getEntity(id) instanceof LivingEntity living ? living : null;
   }

   private static PaleRiderEntity getPaleRiderController(Entity entity) {
      if (entity instanceof PaleRiderEntity rider) return rider;
      if (entity instanceof OwnedPaleRiderMob owned) return owned.getPaleRiderOwner();
      if (isControlled(entity) && entity.level() instanceof ServerLevel level
         && level.getEntity(entity.getPersistentData().getUUID(TAG_OWNER)) instanceof PaleRiderEntity rider) {
         return rider;
      }
      return null;
   }
}
