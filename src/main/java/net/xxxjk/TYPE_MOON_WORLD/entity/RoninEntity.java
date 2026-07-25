package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;
import org.jetbrains.annotations.Nullable;

public class RoninEntity extends KendoApprenticeEntity {
   private static final String TAG_SECOND = "TypeMoonRoninSecondSchool";
   private static final String TAG_SECOND_SCHOOL = "TypeMoonRoninSecondSchoolId";
   public RoninEntity(EntityType<? extends KendoApprenticeEntity> type, Level level) { super(type, level); }
   @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
      KendoSchool primary = random.nextBoolean() ? KendoSchool.HOKUSHIN : KendoSchool.TENNEN;
      setSchool(primary);
      boolean hasSecond = random.nextBoolean();
      getPersistentData().putBoolean(TAG_SECOND, hasSecond);
      getPersistentData().putString(TAG_SECOND_SCHOOL, hasSecond ? (primary == KendoSchool.HOKUSHIN ? KendoSchool.TENNEN.id() : KendoSchool.HOKUSHIN.id()) : "");
      setProficiency(70 + random.nextInt(31));
      if (!hasCustomName()) { setCustomName(net.minecraft.network.chat.Component.literal(JapaneseNpcNameGenerator.ronin(random))); setCustomNameVisible(true); }
      return result;
   }
   public boolean hasSecondSchool() { return getPersistentData().getBoolean(TAG_SECOND); }
   public KendoSchool secondarySchool() {
      return "tennen_rishin_ryu".equals(getPersistentData().getString(TAG_SECOND_SCHOOL)) ? KendoSchool.TENNEN : KendoSchool.HOKUSHIN;
   }
   @Override public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean(TAG_SECOND, hasSecondSchool());
      tag.putString(TAG_SECOND_SCHOOL, getPersistentData().getString(TAG_SECOND_SCHOOL));
   }
   @Override public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      getPersistentData().putBoolean(TAG_SECOND, tag.getBoolean(TAG_SECOND));
      getPersistentData().putString(TAG_SECOND_SCHOOL, tag.getString(TAG_SECOND_SCHOOL));
   }
}
