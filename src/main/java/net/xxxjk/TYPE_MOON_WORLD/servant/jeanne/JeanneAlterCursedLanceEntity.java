package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/** Synchronized visible lance used by Jeanne Alter's cursed lance attack. */
public final class JeanneAlterCursedLanceEntity extends Entity {
   private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(
      JeanneAlterCursedLanceEntity.class, EntityDataSerializers.FLOAT);
   private UUID targetUuid;
   private Vec3 impact = Vec3.ZERO;
   private Vec3 targetOffset = Vec3.ZERO;
   private Vec3 startOffset = new Vec3(0.0D, 10.0D, 0.0D);
   private int hoverTicks = 16;
   private int travelTicks = 8;
   private int plantedTicks = 7;

   public JeanneAlterCursedLanceEntity(EntityType<? extends JeanneAlterCursedLanceEntity> type, Level level) {
      super(type, level);
      noPhysics = true;
      noCulling = true;
      setNoGravity(true);
   }

   private JeanneAlterCursedLanceEntity(ServerLevel level, LivingEntity target, Vec3 impact,
         Vec3 startOffset, int hoverTicks, int travelTicks, int plantedTicks, float scale) {
      this(ModEntities.JEANNE_ALTER_CURSED_LANCE.get(), level);
      targetUuid = target == null ? null : target.getUUID();
      this.impact = impact;
      if (target != null) targetOffset = impact.subtract(targetBase(target));
      this.startOffset = startOffset;
      this.hoverTicks = Math.max(0, hoverTicks);
      this.travelTicks = Math.max(1, travelTicks);
      this.plantedTicks = Math.max(1, plantedTicks);
      entityData.set(SCALE, Math.max(0.25F, scale));
      Vec3 start = impactPoint(level).add(startOffset);
      setLanceRotation(impactPoint(level).subtract(start));
      moveTo(start.x, start.y, start.z, getYRot(), getXRot());
   }

   public static void spawnFalling(ServerLevel level, LivingEntity target, Vec3 impact,
         Vec3 startOffset, int hoverTicks, int fallTicks, float scale) {
      if (level == null || impact == null) return;
      JeanneAlterCursedLanceEntity lance = new JeanneAlterCursedLanceEntity(level, target, impact,
         startOffset, hoverTicks, fallTicks, 7, scale);
      level.addFreshEntity(lance);
      VFXServerEffects.spawnReplayable(level, "jeanne_alter_cursed_lance_aura", lance,
         Math.max(0.5F, (hoverTicks + fallTicks + 7) / 20.0F));
   }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(SCALE, 1.0F); }
   public float modelScale() { return entityData.get(SCALE); }

   @Override public void tick() {
      super.tick();
      if (!(level() instanceof ServerLevel level)) return;
      Vec3 point = impactPoint(level), start = point.add(startOffset), current = point;
      if (tickCount <= hoverTicks) current = start;
      else if (tickCount <= hoverTicks + travelTicks) {
         double progress = Mth.clamp((tickCount - hoverTicks) / (double)travelTicks, 0.0D, 1.0D);
         current = start.lerp(point, progress * progress);
      }
      setPos(current.x, current.y, current.z);
      setDeltaMovement(Vec3.ZERO);
      if (tickCount >= hoverTicks + travelTicks + plantedTicks) discard();
   }

   private void setLanceRotation(Vec3 direction) {
      Vec3 normal = direction.lengthSqr() < 1.0E-6D ? new Vec3(0, -1, 0) : direction.normalize();
      setYRot((float)Math.toDegrees(Math.atan2(-normal.z, normal.x)));
      setXRot((float)Math.toDegrees(Math.atan2(normal.y, Math.sqrt(normal.x * normal.x + normal.z * normal.z))));
   }
   private Vec3 impactPoint(ServerLevel level) {
      if (targetUuid != null && level.getEntity(targetUuid) instanceof LivingEntity target && target.isAlive())
         impact = targetBase(target).add(targetOffset);
      return impact;
   }
   private static Vec3 targetBase(LivingEntity target) {
      Vec3 center = target.getBoundingBox().getCenter();
      return new Vec3(center.x, target.getY() + target.getBbHeight() * 0.42D, center.z);
   }
   @Override protected void addAdditionalSaveData(CompoundTag tag) {
      if (targetUuid != null) tag.putUUID("Target", targetUuid);
      tag.putDouble("ImpactX", impact.x); tag.putDouble("ImpactY", impact.y); tag.putDouble("ImpactZ", impact.z);
      tag.putDouble("OffsetX", targetOffset.x); tag.putDouble("OffsetY", targetOffset.y); tag.putDouble("OffsetZ", targetOffset.z);
      tag.putDouble("StartX", startOffset.x); tag.putDouble("StartY", startOffset.y); tag.putDouble("StartZ", startOffset.z);
      tag.putInt("Hover", hoverTicks); tag.putInt("Travel", travelTicks); tag.putInt("Planted", plantedTicks);
      tag.putFloat("Scale", modelScale());
   }
   @Override protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Target")) targetUuid = tag.getUUID("Target");
      impact = new Vec3(tag.getDouble("ImpactX"), tag.getDouble("ImpactY"), tag.getDouble("ImpactZ"));
      targetOffset = new Vec3(tag.getDouble("OffsetX"), tag.getDouble("OffsetY"), tag.getDouble("OffsetZ"));
      startOffset = new Vec3(tag.getDouble("StartX"), tag.getDouble("StartY"), tag.getDouble("StartZ"));
      hoverTicks = Math.max(0, tag.getInt("Hover")); travelTicks = Math.max(1, tag.getInt("Travel"));
      plantedTicks = Math.max(1, tag.getInt("Planted")); entityData.set(SCALE, Math.max(0.25F, tag.getFloat("Scale")));
      setLanceRotation(startOffset.scale(-1.0D));
   }
}
