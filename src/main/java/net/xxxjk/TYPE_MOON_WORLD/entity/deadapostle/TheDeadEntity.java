package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class TheDeadEntity extends DeadApostleEntity {
   public TheDeadEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
   @Override protected boolean canSwim() { return false; }
   @Override protected boolean burnsInSun() { return true; }
}
