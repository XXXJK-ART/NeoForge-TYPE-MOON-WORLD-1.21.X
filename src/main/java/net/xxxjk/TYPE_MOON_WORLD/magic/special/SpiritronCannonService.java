package net.xxxjk.TYPE_MOON_WORLD.magic.special;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.SpiritronCannonBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class SpiritronCannonService {
   public static final String MAGIC_ID = "spiritron_cannon";
   public static final double MANA_COST = 500.0;
   public static final int WINDUP_TICKS = SpiritronCannonBeamEntity.WINDUP_TICKS;
   private static final String TAG_LOCK_ACTIVE = "TypeMoonSpiritronCannonLock";
   private static final String TAG_LOCK_TICKS = "TypeMoonSpiritronCannonLockTicks";
   private static final String TAG_LOCK_X = "TypeMoonSpiritronCannonLockX";
   private static final String TAG_LOCK_Y = "TypeMoonSpiritronCannonLockY";
   private static final String TAG_LOCK_Z = "TypeMoonSpiritronCannonLockZ";
   private static final String TAG_LOCK_YAW = "TypeMoonSpiritronCannonLockYaw";
   private static final String TAG_LOCK_PITCH = "TypeMoonSpiritronCannonLockPitch";

   private SpiritronCannonService() {
   }

   public static boolean cast(LivingEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, LivingEntity target, boolean consumeMana) {
      if (caster == null || vars == null || !(caster.level() instanceof ServerLevel level) || !caster.isAlive()) {
         return false;
      }
      if (consumeMana) {
         if (vars.player_mana < MANA_COST) {
            return false;
         }
         vars.player_mana = Math.max(0.0, vars.player_mana - MANA_COST);
      }
      Vec3 direction = aimDirection(caster, target);
      faceCasterToDirection(caster, direction);
      Vec3 start = caster.position().add(0.0, caster.getBbHeight() * 0.66, 0.0).add(direction.scale(1.2));
      SpiritronCannonBeamEntity beam = new SpiritronCannonBeamEntity(level, caster, start, direction);
      level.addFreshEntity(beam);
      beginStationaryChant(caster);
      MagicProficiencyService.add(vars, MAGIC_ID, 0.25);
      return true;
   }

   @SubscribeEvent
   public static void onEntityTick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
         return;
      }
      tickStationaryChant(living);
   }

   private static Vec3 aimDirection(LivingEntity caster, LivingEntity target) {
      if (target != null && target.isAlive() && target.level() == caster.level()) {
         Vec3 start = caster.getEyePosition();
         Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         Vec3 direction = end.subtract(start);
         if (direction.lengthSqr() > 1.0E-4) {
            return direction.normalize();
         }
      }
      Vec3 look = caster.getLookAngle();
      return look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
   }

   private static void faceCasterToDirection(LivingEntity caster, Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() > 1.0E-4) {
         float yaw = (float)(Mth.atan2(horizontal.z, horizontal.x) * 180.0F / Math.PI) - 90.0F;
         caster.setYRot(yaw);
         caster.yBodyRot = yaw;
         caster.yHeadRot = yaw;
      }
      float pitch = (float)(-(Mth.atan2(direction.y, horizontal.length()) * 180.0F / Math.PI));
      caster.setXRot(Mth.clamp(pitch, -80.0F, 80.0F));
   }

   private static void beginStationaryChant(LivingEntity caster) {
      CompoundTag data = caster.getPersistentData();
      data.putBoolean(TAG_LOCK_ACTIVE, true);
      data.putInt(TAG_LOCK_TICKS, SpiritronCannonBeamEntity.DURATION_TICKS + 2);
      data.putDouble(TAG_LOCK_X, caster.getX());
      data.putDouble(TAG_LOCK_Y, caster.getY());
      data.putDouble(TAG_LOCK_Z, caster.getZ());
      data.putFloat(TAG_LOCK_YAW, caster.getYRot());
      data.putFloat(TAG_LOCK_PITCH, caster.getXRot());
      caster.setDeltaMovement(Vec3.ZERO);
      caster.hurtMarked = true;
   }

   private static void tickStationaryChant(LivingEntity caster) {
      CompoundTag data = caster.getPersistentData();
      if (!data.getBoolean(TAG_LOCK_ACTIVE)) {
         return;
      }
      int ticks = data.getInt(TAG_LOCK_TICKS);
      if (!caster.isAlive() || ticks <= 0) {
         clearStationaryChant(caster);
         return;
      }
      caster.setDeltaMovement(Vec3.ZERO);
      caster.setPos(data.getDouble(TAG_LOCK_X), data.getDouble(TAG_LOCK_Y), data.getDouble(TAG_LOCK_Z));
      caster.setYRot(data.getFloat(TAG_LOCK_YAW));
      caster.yBodyRot = data.getFloat(TAG_LOCK_YAW);
      caster.yHeadRot = data.getFloat(TAG_LOCK_YAW);
      caster.setXRot(data.getFloat(TAG_LOCK_PITCH));
      caster.hurtMarked = true;
      data.putInt(TAG_LOCK_TICKS, ticks - 1);
   }

   private static void clearStationaryChant(LivingEntity caster) {
      CompoundTag data = caster.getPersistentData();
      data.remove(TAG_LOCK_ACTIVE);
      data.remove(TAG_LOCK_TICKS);
      data.remove(TAG_LOCK_X);
      data.remove(TAG_LOCK_Y);
      data.remove(TAG_LOCK_Z);
      data.remove(TAG_LOCK_YAW);
      data.remove(TAG_LOCK_PITCH);
   }
}
