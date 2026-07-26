package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatPersonality;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatTemperament;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;
import org.jetbrains.annotations.Nullable;

public class RoninEntity extends KendoApprenticeEntity {
   private static final String TAG_SECOND = "TypeMoonRoninSecondSchool";
   private static final String TAG_SECOND_SCHOOL = "TypeMoonRoninSecondSchoolId";
   private static final EntityDataAccessor<Integer> PERSONALITY = SynchedEntityData.defineId(RoninEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> TEMPERAMENT = SynchedEntityData.defineId(RoninEntity.class, EntityDataSerializers.INT);
   public RoninEntity(EntityType<? extends KendoApprenticeEntity> type, Level level) { super(type, level); }
   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(PERSONALITY, NpcCombatPersonality.NEUTRAL.id());
      builder.define(TEMPERAMENT, NpcCombatTemperament.STEADY.id());
   }
   @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
      KendoSchool primary = random.nextBoolean() ? KendoSchool.HOKUSHIN : KendoSchool.TENNEN;
      setSchool(primary);
      boolean hasSecond = random.nextInt(100) < 5;
      getPersistentData().putBoolean(TAG_SECOND, hasSecond);
      getPersistentData().putString(TAG_SECOND_SCHOOL, hasSecond ? (primary == KendoSchool.HOKUSHIN ? KendoSchool.TENNEN.id() : KendoSchool.HOKUSHIN.id()) : "");
      setProficiency(70 + random.nextInt(31));
      setCombatPersonality(NpcCombatPersonality.random(random));
      setCombatTemperament(NpcCombatTemperament.random(random));
      if (!hasCustomName()) { setCustomName(net.minecraft.network.chat.Component.literal(JapaneseNpcNameGenerator.ronin(random))); setCustomNameVisible(true); }
      return result;
   }
   public boolean hasSecondSchool() { return getPersistentData().getBoolean(TAG_SECOND); }
   public NpcCombatPersonality getCombatPersonality() { return NpcCombatPersonality.fromId(entityData.get(PERSONALITY)); }
   public void setCombatPersonality(NpcCombatPersonality value) { entityData.set(PERSONALITY, (value == null ? NpcCombatPersonality.NEUTRAL : value).id()); }
   public NpcCombatTemperament getCombatTemperament() { return NpcCombatTemperament.fromId(entityData.get(TEMPERAMENT)); }
   public void setCombatTemperament(NpcCombatTemperament value) { entityData.set(TEMPERAMENT, (value == null ? NpcCombatTemperament.STEADY : value).id()); }
   @Override protected boolean canProactivelyTarget(LivingEntity target) {
      if (target == null || !target.isAlive()) return false;
      NpcCombatPersonality personality = getCombatPersonality();
      if (target instanceof Player player) return personality == NpcCombatPersonality.EVIL && !player.isCreative() && !player.isSpectator();
      if (personality == NpcCombatPersonality.EVIL) return true;
      return target instanceof Monster && personality == NpcCombatPersonality.GOOD
         || target instanceof Animal && personality == NpcCombatPersonality.EVIL;
   }
   @Override protected boolean allowsOffhandSword() { return true; }
   public KendoSchool secondarySchool() {
      return "tennen_rishin_ryu".equals(getPersistentData().getString(TAG_SECOND_SCHOOL)) ? KendoSchool.TENNEN : KendoSchool.HOKUSHIN;
   }
   @Override public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean(TAG_SECOND, hasSecondSchool());
      tag.putString(TAG_SECOND_SCHOOL, getPersistentData().getString(TAG_SECOND_SCHOOL));
      tag.putInt("NpcPersonality", getCombatPersonality().id());
      tag.putInt("NpcTemperament", getCombatTemperament().id());
   }
   @Override public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      getPersistentData().putBoolean(TAG_SECOND, tag.getBoolean(TAG_SECOND));
      getPersistentData().putString(TAG_SECOND_SCHOOL, tag.getString(TAG_SECOND_SCHOOL));
      setCombatPersonality(NpcCombatPersonality.fromId(tag.getInt("NpcPersonality")));
      setCombatTemperament(NpcCombatTemperament.fromId(tag.getInt("NpcTemperament")));
   }
}
