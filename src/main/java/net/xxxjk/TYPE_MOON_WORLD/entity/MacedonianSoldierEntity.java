package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.IskandarEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.StatRank;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MacedonianSoldierEntity extends PathfinderMob implements GeoEntity {
   private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(MacedonianSoldierEntity.class, EntityDataSerializers.INT);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   @Nullable private UUID iskandarUuid;
   private int poolIndex = -1;
   private long actionStartTick;

   public MacedonianSoldierEntity(EntityType<? extends MacedonianSoldierEntity> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, StatRank.E.toMaxHealth())
         .add(Attributes.ATTACK_DAMAGE, StatRank.E.toAttackDamage())
         .add(Attributes.MOVEMENT_SPEED, StatRank.E.toMovementSpeed())
         .add(Attributes.ARMOR, 3.0)
         .add(Attributes.ATTACK_SPEED, 4.0)
         .add(Attributes.FOLLOW_RANGE, 64.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.45);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(RANK, StatRank.E.ordinal());
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
      this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
   }

   public void initializeForIonioiHetairoi(IskandarEntity iskandar, StatRank rank, int poolIndex) {
      this.iskandarUuid = iskandar.getUUID();
      this.poolIndex = poolIndex;
      this.actionStartTick = iskandar.level().getGameTime() + IskandarEntity.IONIOI_FORMATION_DELAY_TICKS;
      this.setSoldierRank(rank);
      this.equipPhalanxGear();
      this.getPersistentData().putBoolean("IonioiHetairoiSoldier", true);
      this.getPersistentData().putUUID("IonioiHetairoiOwner", iskandar.getUUID());
      this.setTarget(iskandar.getTarget());
   }

   public StatRank getSoldierRank() {
      int id = this.entityData.get(RANK);
      StatRank[] values = StatRank.values();
      return id >= 0 && id < values.length ? values[id] : StatRank.E;
   }

   public int getPoolIndex() {
      return this.poolIndex;
   }

   private void setSoldierRank(StatRank rank) {
      StatRank resolved = rank == null ? StatRank.E : rank;
      this.entityData.set(RANK, resolved.ordinal());
      ServantParams params = new ServantParams(resolved, false, resolved, false, resolved, false, resolved, false, resolved, false);
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(params.maxHealth());
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(params.attackDamage());
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(params.movementSpeed());
      this.getAttribute(Attributes.ARMOR).setBaseValue(params.armor());
      this.setHealth(this.getMaxHealth());
   }

   private void equipPhalanxGear() {
      this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MACEDONIAN_SPEAR.get()));
      this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.MACEDONIAN_ROUND_SHIELD.get()));
      this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
   }

   @Override
   protected void customServerAiStep() {
      boolean tactical = net.xxxjk.TYPE_MOON_WORLD.combat.ai.NpcTacticalController.tick(this);
      if (!tactical) {
         super.customServerAiStep();
      }
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }
      if (this.iskandarUuid == null) {
         return;
      }
      IskandarEntity owner = this.getIskandar(level);
      if (owner == null || !owner.isAlive() || !owner.isIonioiHetairoiActive()) {
         this.discard();
         return;
      }
      if (level.getGameTime() < this.actionStartTick) {
         this.setTarget(null);
         this.getNavigation().stop();
         this.setDeltaMovement(Vec3.ZERO);
         return;
      }
      LivingEntity target = owner.getTarget();
      if (!isValidTarget(target)) {
         target = this.findThreatNear(owner);
      }
      if (isValidTarget(target)) {
         this.setTarget(target);
         if (this.tickCount > 40) {
            Vec3 slot = net.xxxjk.TYPE_MOON_WORLD.combat.ai.MinionCoordinationService.surroundPoint(owner, this, target, 3.1);
            this.getNavigation().moveTo(slot.x, slot.y, slot.z, 1.18);
         }
      } else {
         this.setTarget(null);
         if (this.distanceToSqr(owner) > 12.0 * 12.0) {
            this.getNavigation().moveTo(owner, 1.12);
         }
      }
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean success = super.doHurtTarget(target);
      if (success) {
         this.swing(InteractionHand.MAIN_HAND);
         this.triggerAnim("action_controller", "melee_attack");
      }
      return success;
   }

   @Override
   public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
      if (isFriendly(source.getEntity()) || isFriendly(source.getDirectEntity())) {
         return false;
      }
      return super.hurt(source, amount);
   }

   @Override
   public void die(net.minecraft.world.damagesource.DamageSource source) {
      if (this.level() instanceof ServerLevel level) {
         IskandarEntity iskandar = this.getIskandar(level);
         if (iskandar != null) {
            iskandar.onMacedonianSoldierDeath(this);
         }
      }
      super.die(source);
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      return this.isFriendly(other);
   }

   private boolean isFriendly(@Nullable Entity entity) {
      if (entity == null) {
         return false;
      }
      if (this.iskandarUuid != null && this.iskandarUuid.equals(entity.getUUID())) {
         return true;
      }
      if (entity instanceof MacedonianSoldierEntity soldier) {
         return this.iskandarUuid != null && this.iskandarUuid.equals(soldier.iskandarUuid);
      }
      if (this.level() instanceof ServerLevel level) {
         IskandarEntity iskandar = this.getIskandar(level);
         return iskandar != null && iskandar.isAlliedTo(entity);
      }
      return false;
   }

   private boolean isValidTarget(@Nullable LivingEntity target) {
      return target != null && target.isAlive() && target != this && !this.isAlliedTo(target) && !EntityUtils.isImmunePlayerTarget(target);
   }

   @Nullable
   private LivingEntity findThreatNear(IskandarEntity owner) {
      AABB area = owner.getBoundingBox().inflate(18.0);
      return this.level().getEntitiesOfClass(LivingEntity.class, area, this::isValidTarget).stream().findFirst().orElse(null);
   }

   @Nullable
   private IskandarEntity getIskandar(ServerLevel level) {
      if (this.iskandarUuid == null) {
         return null;
      }
      Entity entity = level.getEntity(this.iskandarUuid);
      return entity instanceof IskandarEntity iskandar ? iskandar : null;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.iskandarUuid != null) {
         tag.putUUID("IonioiHetairoiOwner", this.iskandarUuid);
      }
      tag.putInt("IonioiHetairoiPoolIndex", this.poolIndex);
      tag.putString("IonioiHetairoiRank", this.getSoldierRank().name());
      tag.putLong("IonioiHetairoiActionStartTick", this.actionStartTick);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID("IonioiHetairoiOwner")) {
         this.iskandarUuid = tag.getUUID("IonioiHetairoiOwner");
      }
      this.poolIndex = tag.getInt("IonioiHetairoiPoolIndex");
      this.actionStartTick = tag.getLong("IonioiHetairoiActionStartTick");
      if (this.actionStartTick <= 0L && this.iskandarUuid != null && this.level() instanceof ServerLevel level) {
         this.actionStartTick = level.getGameTime() + IskandarEntity.IONIOI_FORMATION_DELAY_TICKS;
      }
      this.setSoldierRank(StatRank.fromKey(tag.getString("IonioiHetairoiRank")));
      this.equipPhalanxGear();
   }

   public ItemStack getOffhandDisplayItem() {
      return this.getOffhandItem();
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(RawAnimation.begin().thenLoop(
         event.isMoving() ? "animation.dragonfang.walk" : "animation.dragonfang.idle"))));
      AnimationController<MacedonianSoldierEntity> action = new AnimationController<>(this, "action_controller", 0, event -> PlayState.STOP);
      action.triggerableAnim("melee_attack", RawAnimation.begin().thenPlay("animation.dragonfang.melee_attack"));
      controllers.add(action);
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
