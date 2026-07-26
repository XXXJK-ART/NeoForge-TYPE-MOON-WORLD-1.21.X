package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class LivingDeadEntity extends DeadApostleEntity {
   private static final String CURRENT_STATS = "LivingDeadStatsV3";
   public LivingDeadEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
   @Override protected int sunlightDebuffDelay() { return 1; }
   @Override protected boolean receivesGeneratedName() { return true; }
   @Override protected void ensureCurrentStageAttributes() { migrateStageAttributes(CURRENT_STATS, 100.0, 0.32); }
}
