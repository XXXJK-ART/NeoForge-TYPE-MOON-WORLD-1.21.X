package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanNpcCombatController;
import org.jetbrains.annotations.Nullable;

public class BajiquanApprenticeEntity extends PathfinderMob {
   private static final EntityDataAccessor<Boolean> FEMALE = SynchedEntityData.defineId(BajiquanApprenticeEntity.class, EntityDataSerializers.BOOLEAN);
   private static final String TAG_PROFICIENCY = "TypeMoonBajiquanNpcProficiency";

   public BajiquanApprenticeEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

   @Override protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
      this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.27)
         .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.ARMOR, 2.0).add(Attributes.FOLLOW_RANGE, 24.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.12);
   }

   @Override protected void customServerAiStep() {
      super.customServerAiStep();
      NpcScaleHelper.ensureRandomScale(this);
      if (!this.getPersistentData().contains(TAG_PROFICIENCY)) this.getPersistentData().putInt(TAG_PROFICIENCY, 10 + this.random.nextInt(41));
      BajiquanNpcCombatController.tick(this, this.getPersistentData().getInt(TAG_PROFICIENCY), false);
   }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FEMALE, false);
   }
   public boolean isFemale() { return this.entityData.get(FEMALE); }
   public void setFemale(boolean value) { this.entityData.set(FEMALE, value); }

   @Nullable @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
      NpcScaleHelper.ensureRandomScale(this);
      this.setFemale(this.random.nextBoolean());
      this.getPersistentData().putInt(TAG_PROFICIENCY, 10 + this.random.nextInt(41));
      if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION) {
         this.setPersistenceRequired();
      }
      return result;
   }

   @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("Female", this.isFemale()); }
   @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); this.setFemale(tag.getBoolean("Female")); }
}
