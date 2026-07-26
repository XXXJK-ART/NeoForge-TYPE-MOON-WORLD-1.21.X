package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.SpawnGroupData;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;

public class ShinsengumiEntity extends KendoApprenticeEntity {
   public ShinsengumiEntity(EntityType<? extends KendoApprenticeEntity> type, Level level) { super(type, level); setSchool(KendoSchool.TENNEN); }
   @Override public void ensureRandomName() { if (!level().isClientSide() && !hasCustomName()) { setCustomName(net.minecraft.network.chat.Component.literal(JapaneseNpcNameGenerator.shinsengumi(random))); setCustomNameVisible(true); } }
   @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType type, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData data) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data); setSchool(KendoSchool.TENNEN); setProficiency(70 + random.nextInt(31)); ensureRandomName(); return result;
   }
}
