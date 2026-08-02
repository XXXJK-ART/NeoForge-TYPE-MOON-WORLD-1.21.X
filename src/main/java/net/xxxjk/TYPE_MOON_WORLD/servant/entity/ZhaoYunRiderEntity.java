package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import org.jetbrains.annotations.Nullable;

public final class ZhaoYunRiderEntity extends ServantEntity {
   public static final String SERVANT_KEY = "zhao_yun_rider";
   public static final String TAG_MOUNT = "ZhaoYunHakuryu";
   public static final String TAG_NP_UNTIL = "ZhaoYunChangbanpoUntil";
   public static final String TAG_NP_COOLDOWN = "ZhaoYunChangbanpoCooldown";
   private static final int NP_DURATION = 300;
   private static final int NP_COOLDOWN = 800;
   private static final float NP_DAMAGE = 100.0F;
   @Nullable private UUID mountUuid;
   private final Set<UUID> npContactTargets = new HashSet<>();

   public ZhaoYunRiderEntity(EntityType<? extends ZhaoYunRiderEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override public void tick() {
      super.tick();
      if (!(level() instanceof ServerLevel server)) return;
      ensureHakuryu(server);
      tickZhaoYunSkills(server);
      if (!isChangbanpoActive() && getTarget() != null && getTarget().isAlive()
         && distanceToSqr(getTarget()) > 9.0 && server.getGameTime() >= getPersistentData().getLong(TAG_NP_COOLDOWN)) {
         startChangbanpo();
      }
      if (isChangbanpoActive() && server.getGameTime() >= getPersistentData().getLong(TAG_NP_UNTIL)) {
         endChangbanpo();
      }
   }

   private void tickZhaoYunSkills(ServerLevel level) {
      long now = level.getGameTime();
      if (getHealth() <= getMaxHealth() * 0.5F) {
         if (!getPersistentData().getBoolean("ZhaoYunDragonGallActive")) {
            getPersistentData().putBoolean("ZhaoYunDragonGallActive", true);
            triggerNamedActionAnimation("dragon_gall");
            VFXServerEffects.spawnReplayable(level, "servant_zhao_yun_dragon_gall", this, 24.0F);
         }
         if (!hasEffect(MobEffects.DAMAGE_BOOST) || getEffect(MobEffects.DAMAGE_BOOST).getDuration() < 10) {
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, true, false));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, true, false));
         }
      } else {
         getPersistentData().putBoolean("ZhaoYunDragonGallActive", false);
      }

      LivingEntity master = getEntityMaster();
      if (master != null && master.getHealth() <= master.getMaxHealth() * 0.5F
         && getCurrentMp() >= 20.0 && now >= getPersistentData().getLong("ZhaoYunRescueCooldown")) {
         setCurrentMp(getCurrentMp() - 20.0);
         getPersistentData().putLong("ZhaoYunRescueCooldown", now + 400L);
         getPersistentData().putLong("ZhaoYunRescueUntil", now + 1200L);
         addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1, true, false));
         master.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1, true, false));
         triggerNamedActionAnimation("rescue");
         VFXServerEffects.spawnReplayable(level, "servant_zhao_yun_rescue", master, 32.0F);
      }

      if (getTarget() != null && getTarget().isAlive() && now >= getPersistentData().getLong("ZhaoYunSevenOutCooldown")) {
         int enemies = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0),
            entity -> entity != this && entity.isAlive() && !isAlliedTo(entity)).size();
         if (enemies >= 3) {
            getPersistentData().putLong("ZhaoYunSevenOutCooldown", now + 60L);
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, true, false));
            triggerNamedActionAnimation("seven_out");
            VFXServerEffects.spawnReplayable(level, "servant_zhao_yun_seven_out", this, 32.0F);
         }
      }
   }

   @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
                                       net.minecraft.world.level.ServerLevelAccessor level,
                                       net.minecraft.world.DifficultyInstance difficulty,
                                       net.minecraft.world.entity.MobSpawnType spawnType,
                                       @Nullable net.minecraft.world.entity.SpawnGroupData groupData) {
      net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(ModItems.YAJIAO_QIANG.get()));
      setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
      return result;
   }

   private void ensureHakuryu(ServerLevel level) {
      if (mountUuid != null && level.getEntity(mountUuid) instanceof ZhaoYunHakuryuEntity mount && mount.isAlive()) {
         if (!isPassenger() && !mount.getPassengers().contains(this)) startRiding(mount, true);
         return;
      }
      ZhaoYunHakuryuEntity mount = ModEntities.ZHAO_YUN_HAKURYU.get().create(level);
      if (mount == null) return;
      mount.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
      mount.setHealth(mount.getMaxHealth());
      if (!level.addFreshEntity(mount)) return;
      mountUuid = mount.getUUID();
      startRiding(mount, true);
   }

   @Nullable public ZhaoYunHakuryuEntity getHakuryu() {
      if (!(level() instanceof ServerLevel level) || mountUuid == null) return null;
      Entity entity = level.getEntity(mountUuid);
      return entity instanceof ZhaoYunHakuryuEntity mount ? mount : null;
   }

   public boolean isChangbanpoActive() {
      return getPersistentData().getLong(TAG_NP_UNTIL) > level().getGameTime();
   }

   public boolean startChangbanpo() {
      if (!(level() instanceof ServerLevel level) || isChangbanpoActive() || getCurrentMp() < 80.0
         || level.getGameTime() < getPersistentData().getLong(TAG_NP_COOLDOWN)) return false;
      setCurrentMp(getCurrentMp() - 80.0);
      npContactTargets.clear();
      getPersistentData().putLong(TAG_NP_UNTIL, level.getGameTime() + NP_DURATION);
      getPersistentData().putLong(TAG_NP_COOLDOWN, level.getGameTime() + NP_COOLDOWN);
      if (getHakuryu() != null) getHakuryu().setNpActive(true);
      triggerNamedActionAnimation("changbanpo_no_ikki_gake");
      VFXServerEffects.spawnOriented(level, "servant_zhao_yun_changbanpo", this.position(), getLookAngle(), 128.0);
      ServantVoiceHelper.tryPlayZhaoYunNp(this);
      return true;
   }

   public void endChangbanpo() {
      getPersistentData().remove(TAG_NP_UNTIL);
      npContactTargets.clear();
      if (getHakuryu() != null) getHakuryu().setNpActive(false);
   }

   public void tickChangbanpoMount(ZhaoYunHakuryuEntity mount) {
      if (!(level() instanceof ServerLevel level)) return;
      LivingEntity target = getTarget();
      Vec3 direction = target != null && target.isAlive()
         ? target.position().subtract(mount.position()) : getLookAngle();
      Vec3 flat = new Vec3(direction.x, 0.0, direction.z);
      if (flat.lengthSqr() < 1.0E-4) return;
      flat = flat.normalize();
      double speed = Math.min(1.8, mount.getAttributeValue(Attributes.MOVEMENT_SPEED) * 3.4);
      Vec3 next = mount.position().add(flat.scale(speed));
      BlockPos collision = BlockPos.containing(next.x, next.y + 0.75, next.z);
      if (level.getBlockState(collision).is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
         endChangbanpo();
         return;
      }
      destroyNonBedrock(level, mount.getBoundingBox().move(next.subtract(mount.position())));
      mount.setYRot((float)(Math.atan2(-flat.x, flat.z) * 180.0 / Math.PI));
      mount.move(net.minecraft.world.entity.MoverType.SELF, flat.scale(speed));
      mount.setDeltaMovement(flat.scale(speed));
      AABB hitBox = mount.getBoundingBox().inflate(1.2, 0.5, 1.2);
      Set<UUID> contactsThisTick = new HashSet<>();
      for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, hitBox,
         entity -> entity != this && entity != mount && entity.isAlive() && entity != getEntityMaster()
            && !isAlliedTo(entity))) {
         UUID victimId = victim.getUUID();
         contactsThisTick.add(victimId);
         if (!npContactTargets.contains(victimId)) {
            victim.hurt(damageSources().mobAttack(this), NP_DAMAGE);
         }
      }
      npContactTargets.retainAll(contactsThisTick);
      npContactTargets.addAll(contactsThisTick);
   }

   private static void destroyNonBedrock(ServerLevel level, AABB box) {
      BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
      BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
      for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
         if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
            level.removeBlock(pos, false);
         }
      }
   }

   @Override public boolean isAlliedTo(Entity other) {
      return super.isAlliedTo(other) || getEntityMaster() != null && (other == getEntityMaster() || getEntityMaster().isAlliedTo(other));
   }

   @Override public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (mountUuid != null) tag.putUUID(TAG_MOUNT, mountUuid);
      tag.putLong(TAG_NP_UNTIL, getPersistentData().getLong(TAG_NP_UNTIL));
      tag.putLong(TAG_NP_COOLDOWN, getPersistentData().getLong(TAG_NP_COOLDOWN));
   }

   @Override public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID(TAG_MOUNT)) mountUuid = tag.getUUID(TAG_MOUNT);
      getPersistentData().putLong(TAG_NP_UNTIL, tag.getLong(TAG_NP_UNTIL));
      getPersistentData().putLong(TAG_NP_COOLDOWN, tag.getLong(TAG_NP_COOLDOWN));
   }
}
