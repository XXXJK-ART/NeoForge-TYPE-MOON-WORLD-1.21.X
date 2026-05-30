package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaWorkshopHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DragonfangSoldierEntity extends PathfinderMob implements GeoEntity, RangedAttackMob {
   private static final EntityDataAccessor<Boolean> HAS_OWNER = SynchedEntityData.defineId(DragonfangSoldierEntity.class, EntityDataSerializers.BOOLEAN);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.1, true) {
      @Override
      public boolean canUse() {
         return !DragonfangSoldierEntity.this.isBowLoadout() && super.canUse();
      }

      @Override
      public boolean canContinueToUse() {
         return !DragonfangSoldierEntity.this.isBowLoadout() && super.canContinueToUse();
      }
   };
   private final RangedBowAttackGoal<DragonfangSoldierEntity> bowGoal = new RangedBowAttackGoal<>(this, 1.0, 20, 15.0F) {
      @Override
      public boolean canUse() {
         return DragonfangSoldierEntity.this.isBowLoadout() && super.canUse();
      }

      @Override
      public boolean canContinueToUse() {
         return DragonfangSoldierEntity.this.isBowLoadout() && super.canContinueToUse();
      }
   };
   @Nullable
   private UUID summonerUuid;

   public DragonfangSoldierEntity(EntityType<? extends DragonfangSoldierEntity> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 40.0)
         .add(Attributes.ATTACK_DAMAGE, 8.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.ARMOR, 2.0)
         .add(Attributes.FOLLOW_RANGE, 32.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(HAS_OWNER, false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, this.meleeGoal);
      this.goalSelector.addGoal(1, this.bowGoal);
      this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
      SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
      this.assignRandomLoadout();
      return data;
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      LivingEntity owner = this.getSummoner();
      if (owner == null || !owner.isAlive()) {
         this.discard();
         return;
      }

      this.getPersistentData().putBoolean(MedeaWorkshopHelper.TAG_MAGIC_SUMMON, true);
      this.getPersistentData().putString(MedeaWorkshopHelper.TAG_MAGIC_SUMMON_OWNER, owner.getUUID().toString());

      LivingEntity preferredTarget = owner.getLastHurtByMob();
      if (!isValidHostile(preferredTarget)) {
         preferredTarget = owner instanceof net.minecraft.world.entity.Mob mob ? mob.getTarget() : null;
      }
      if (!isValidHostile(preferredTarget) && owner.getHealth() < owner.getMaxHealth() * 0.3F) {
         preferredTarget = this.findThreatNearOwner(owner);
      }
      if (isValidHostile(preferredTarget)) {
         this.setTarget(preferredTarget);
      } else if (this.getTarget() != null && !isValidHostile(this.getTarget())) {
         this.setTarget(null);
      }

      if (this.getTarget() == null && this.distanceToSqr(owner) > 16.0) {
         this.getNavigation().moveTo(owner, 1.1);
      }
   }

   @Nullable
   public UUID getSummonerUuid() {
      return this.summonerUuid;
   }

   public void setSummoner(MedeaEntity summoner) {
      this.summonerUuid = summoner.getUUID();
      this.entityData.set(HAS_OWNER, true);
      this.getPersistentData().putBoolean(MedeaWorkshopHelper.TAG_MAGIC_SUMMON, true);
      this.getPersistentData().putString(MedeaWorkshopHelper.TAG_MAGIC_SUMMON_OWNER, this.summonerUuid.toString());
      if (this.getMainHandItem().isEmpty()) {
         this.assignRandomLoadout();
      }
   }

   @Nullable
   public LivingEntity getSummoner() {
      if (this.summonerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
         return null;
      }
      Entity entity = serverLevel.getEntity(this.summonerUuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) {
         return true;
      }
      LivingEntity owner = this.getSummoner();
      if (owner == null) {
         return false;
      }
      if (other == owner || owner.isAlliedTo(other)) {
         return true;
      }
      if (other instanceof DragonfangSoldierEntity dragonfang && this.summonerUuid != null) {
         return this.summonerUuid.equals(dragonfang.summonerUuid);
      }
      return other instanceof MedeaEntity medea && this.summonerUuid != null && this.summonerUuid.equals(medea.getUUID());
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.summonerUuid != null) {
         tag.putUUID("DragonfangSummoner", this.summonerUuid);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID("DragonfangSummoner")) {
         this.summonerUuid = tag.getUUID("DragonfangSummoner");
         this.entityData.set(HAS_OWNER, true);
      }
      if (this.getMainHandItem().isEmpty()) {
         this.assignRandomLoadout();
      }
   }

   @Override
   public void performRangedAttack(LivingEntity target, float velocity) {
      ItemStack projectileStack = this.getProjectile(this.getMainHandItem());
      if (projectileStack.isEmpty()) {
         projectileStack = new ItemStack(Items.ARROW);
      }
      ItemStack bow = this.getMainHandItem();
      AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectileStack, velocity, bow);
      double dx = target.getX() - this.getX();
      double dz = target.getZ() - this.getZ();
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      double dy = target.getY(0.3333333333333333) - arrow.getY() + horizontal * 0.2;
      arrow.shoot(dx, dy, dz, 1.6F, 10.0F - this.level().getDifficulty().getId() * 2.0F);
      this.level().addFreshEntity(arrow);
      this.playSound(net.minecraft.sounds.SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
   }

   @Override
   public boolean canFireProjectileWeapon(ProjectileWeaponItem projectileWeaponItem) {
      return projectileWeaponItem instanceof BowItem;
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(RawAnimation.begin().thenLoop("animation"))));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private boolean isValidHostile(@Nullable LivingEntity target) {
      return target != null && target.isAlive() && !target.isAlliedTo(this) && !EntityUtils.isImmunePlayerTarget(target);
   }

   @Nullable
   private LivingEntity findThreatNearOwner(LivingEntity owner) {
      AABB guardBox = owner.getBoundingBox().inflate(6.0);
      return this.level().getEntitiesOfClass(
         LivingEntity.class,
         guardBox,
         candidate -> candidate != this && candidate.isAlive() && !candidate.isAlliedTo(this) && !EntityUtils.isImmunePlayerTarget(candidate)
      ).stream().findFirst().orElse(null);
   }

   public ItemStack getOffhandDisplayItem() {
      return this.getOffhandItem();
   }

   private boolean isBowLoadout() {
      return this.getMainHandItem().getItem() instanceof BowItem;
   }

   private void assignRandomLoadout() {
      int roll = this.getRandom().nextInt(3);
      if (roll == 0) {
         this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
         this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      } else if (roll == 1) {
         this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
         this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(Items.STONE_SWORD));
      } else {
         this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
         this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      }
   }
}
