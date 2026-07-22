package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
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
      if (next > previous && canControl(target) && target.getRandom().nextDouble() < InfectionRules.controlChance(next)) {
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
      if (target.tickCount % 20 == Math.floorMod(target.getId(), 20)) {
         target.hurt(owner.damageSources().source(PaleRiderDamageTypes.INFECTION, owner), 10.0F);
         spread(serverLevel, target, owner, level);
      }
      if (target.getPersistentData().getBoolean(TAG_CONTROLLED) && target instanceof Mob mob) {
         LivingEntity ownerTarget = owner instanceof Mob ownerMob ? ownerMob.getTarget() : null;
         if (ownerTarget != null && ownerTarget.isAlive() && ownerTarget != mob && !ownerTarget.isAlliedTo(owner)) {
            mob.setTarget(ownerTarget);
         }
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

   private static boolean canControl(LivingEntity target) {
      return target instanceof Mob && !(target instanceof Player) && !(target instanceof ServantEntity) && !target.getType().is(Tags.EntityTypes.BOSSES);
   }

   private static void beginControl(Mob mob, LivingEntity owner) {
      CompoundTag data = mob.getPersistentData();
      data.putBoolean(TAG_CONTROLLED, true);
      data.putBoolean(TAG_PREVIOUS_NO_AI, mob.isNoAi());
      mob.setNoAi(false);
      mob.setTarget(owner instanceof Mob ownerMob ? ownerMob.getTarget() : null);
   }

   private static void endControl(LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      if (data.getBoolean(TAG_CONTROLLED) && target instanceof Mob mob) {
         mob.setNoAi(data.getBoolean(TAG_PREVIOUS_NO_AI));
         mob.setTarget(null);
      }
      data.remove(TAG_CONTROLLED);
      data.remove(TAG_PREVIOUS_NO_AI);
   }

   private static LivingEntity getOwner(ServerLevel level, LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      if (!data.hasUUID(TAG_OWNER)) {
         return null;
      }
      UUID id = data.getUUID(TAG_OWNER);
      return level.getEntity(id) instanceof LivingEntity living ? living : null;
   }
}
