package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoNpcCombatController;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;
import org.jetbrains.annotations.Nullable;

public class KendoApprenticeEntity extends PathfinderMob {
   private static final String TAG_PROF = "TypeMoonKendoNpcProficiency";
   private static final String TAG_SCHOOL = "TypeMoonKendoNpcSchool";
   private static final String TAG_FEMALE = "Female";
   private boolean female;
   public KendoApprenticeEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); setPersistenceRequired(); }
   @Override protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(1, KendoNpcCombatController.combatGoal(this, school(), this::proficiency));
      goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9));
      goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(5, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this));
   }
   public static AttributeSupplier.Builder createAttributes() { return createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 2.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.SCALE, NpcScaleHelper.DEFAULT_RANDOM_SCALE); }
   @Override protected void customServerAiStep() { super.customServerAiStep(); NpcScaleHelper.ensureRandomScale(this); ensureRandomName(); if (!getPersistentData().contains(TAG_PROF)) getPersistentData().putInt(TAG_PROF, 20 + random.nextInt(61)); }
   @Nullable @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) { SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data); female = random.nextBoolean(); getPersistentData().putInt(TAG_PROF, 20 + random.nextInt(61)); ensureRandomName(); NpcScaleHelper.ensureRandomScale(this); return result; }
   public boolean isFemale() { return female; }
   public void setFemale(boolean value) { female = value; }
   public KendoSchool school() { return "tennen_rishin_ryu".equals(getPersistentData().getString(TAG_SCHOOL)) ? KendoSchool.TENNEN : KendoSchool.HOKUSHIN; }
   public void setSchool(KendoSchool school) { getPersistentData().putString(TAG_SCHOOL, school.id()); }
   public double proficiency() { return getPersistentData().getInt(TAG_PROF); }
   public void setProficiency(int value) { getPersistentData().putInt(TAG_PROF, value); }
   public void ensureRandomName() { if (!level().isClientSide() && !hasCustomName()) { setCustomName(net.minecraft.network.chat.Component.literal(JapaneseNpcNameGenerator.apprentice(random, female))); setCustomNameVisible(true); } }
   @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean(TAG_FEMALE, female); }
   @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); female = tag.getBoolean(TAG_FEMALE); }
}
