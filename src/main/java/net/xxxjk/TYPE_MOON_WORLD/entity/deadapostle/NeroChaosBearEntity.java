package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.level.Level;

public class NeroChaosBearEntity extends PolarBear implements NeroChaosBeastLogic.NeroChaosBeastEntityMarker {
   public NeroChaosBearEntity(EntityType<? extends PolarBear> type, Level level) {
      super(type, level);
      setPersistenceRequired();
      setSilent(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 150.0)
         .add(Attributes.MOVEMENT_SPEED, 0.27)
         .add(Attributes.ATTACK_DAMAGE, 34.0)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
   }

   @Override
   protected void customServerAiStep() {
      NeroChaosBeastLogic.tick(this);
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      return NeroChaosBeastLogic.isAllied(this, other) || super.isAlliedTo(other);
   }

   @Override
   public boolean removeWhenFarAway(double distance) {
      return false;
   }

   @Override
   public UUID neroChaosOwnerUuid() {
      return NeroChaosBeastLogic.ownerUuid(this);
   }
}
