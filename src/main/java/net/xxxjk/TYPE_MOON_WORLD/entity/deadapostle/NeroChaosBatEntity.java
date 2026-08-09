package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.Level;

public class NeroChaosBatEntity extends Bat implements NeroChaosBeastLogic.NeroChaosBeastEntityMarker {
   public NeroChaosBatEntity(EntityType<? extends Bat> type, Level level) {
      super(type, level);
      setPersistenceRequired();
      setSilent(true);
      setResting(false);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 45.0)
         .add(Attributes.MOVEMENT_SPEED, 0.41)
         .add(Attributes.ATTACK_DAMAGE, 9.0)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   @Override
   protected void customServerAiStep() {
      setResting(false);
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
