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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoNpcCombatController;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;
import net.xxxjk.TYPE_MOON_WORLD.martial.NpcActionPose;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import org.jetbrains.annotations.Nullable;

public class KendoApprenticeEntity extends HumanNpcEntity implements NpcActionPose {
   private static final String TAG_PROF = "TypeMoonKendoNpcProficiency";
   private static final String TAG_SCHOOL = "TypeMoonKendoNpcSchool";
   private static final String TAG_FEMALE = "Female";
   private static final String TAG_OFFHAND_ROLLED = "TypeMoonKendoOffhandRolled";
   private static final EntityDataAccessor<Boolean> FEMALE = SynchedEntityData.defineId(KendoApprenticeEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> ACTION_POSE = SynchedEntityData.defineId(KendoApprenticeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> ACTION_TICKS = SynchedEntityData.defineId(KendoApprenticeEntity.class, EntityDataSerializers.INT);
   public KendoApprenticeEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); setPersistenceRequired(); }
   @Override protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(1, KendoNpcCombatController.combatGoal(this, school(), this::proficiency));
      goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9));
      goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(5, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this));
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::canProactivelyTarget));
   }
   public static AttributeSupplier.Builder createAttributes() { return createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 2.0).add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.SCALE, NpcScaleHelper.DEFAULT_RANDOM_SCALE); }
   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(FEMALE, false); builder.define(ACTION_POSE, 0); builder.define(ACTION_TICKS, 0); }
   @Override public void tick() {
      super.tick();
      if (this.entityData.get(ACTION_TICKS) > 0) {
         this.entityData.set(ACTION_TICKS, this.entityData.get(ACTION_TICKS) - 1);
         if (this.entityData.get(ACTION_TICKS) <= 0) this.entityData.set(ACTION_POSE, 0);
      }
   }
   @Override protected void customServerAiStep() { super.customServerAiStep(); NpcScaleHelper.ensureRandomScale(this); ensureRandomName(); ensureSwordLoadout(); if (!getPersistentData().contains(TAG_PROF)) getPersistentData().putInt(TAG_PROF, 20 + random.nextInt(61)); }
   @Nullable @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) { SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data); setFemale(random.nextBoolean()); getPersistentData().putInt(TAG_PROF, 20 + random.nextInt(61)); ensureRandomName(); NpcScaleHelper.ensureRandomScale(this); ensureSwordLoadout(); return result; }
   public boolean isFemale() { return entityData.get(FEMALE); }
   public void setFemale(boolean value) { entityData.set(FEMALE, value); }
   public KendoSchool school() { return "tennen_rishin_ryu".equals(getPersistentData().getString(TAG_SCHOOL)) ? KendoSchool.TENNEN : KendoSchool.HOKUSHIN; }
   public void setSchool(KendoSchool school) { getPersistentData().putString(TAG_SCHOOL, school.id()); }
   public double proficiency() { return getPersistentData().getInt(TAG_PROF); }
   public void setProficiency(int value) { getPersistentData().putInt(TAG_PROF, value); }
   @Override public int getNpcActionPose() { return this.entityData.get(ACTION_POSE); }
   @Override public int getNpcActionPoseTicks() { return this.entityData.get(ACTION_TICKS); }
   @Override public void triggerNpcActionPose(int pose, int ticks) { this.entityData.set(ACTION_POSE, Math.max(0, pose)); this.entityData.set(ACTION_TICKS, Math.max(0, ticks)); }
   protected boolean canProactivelyTarget(LivingEntity target) { return target instanceof Monster; }
   public void ensureRandomName() { if (!level().isClientSide() && !hasCustomName()) { setCustomName(net.minecraft.network.chat.Component.literal(JapaneseNpcNameGenerator.apprentice(random, isFemale()))); setCustomNameVisible(true); } }
   protected boolean allowsOffhandSword() { return false; }
   protected void ensureSwordLoadout() {
      if (!isKendoSword(getMainHandItem())) {
         setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(randomSword()));
         setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      }
      if (allowsOffhandSword() && !getPersistentData().getBoolean(TAG_OFFHAND_ROLLED)) {
         getPersistentData().putBoolean(TAG_OFFHAND_ROLLED, true);
         if (getOffhandItem().isEmpty() && random.nextInt(100) < 5) {
            setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(randomSword()));
            setDropChance(EquipmentSlot.OFFHAND, 0.0F);
         }
      }
   }
   private Item randomSword() {
      return switch (random.nextInt(3)) {
         case 0 -> ModItems.WAKIZASHI.get();
         case 1 -> ModItems.KATANA.get();
         default -> ModItems.NODACHI.get();
      };
   }
   private static boolean isKendoSword(ItemStack stack) {
      return stack.is(ModItems.WAKIZASHI.get()) || stack.is(ModItems.KATANA.get()) || stack.is(ModItems.NODACHI.get());
   }
   @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean(TAG_FEMALE, isFemale()); }
   @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); setFemale(tag.getBoolean(TAG_FEMALE)); }
}
