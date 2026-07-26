package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class NightKinEntity extends DeadApostleEntity {
   private static final String SPECIALIZED = "NightKinSpecialized";
   private static final String CURRENT_STATS = "NightKinStatsV3";
   public NightKinEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
   @Override protected int sunlightDebuffDelay() { return 600; }
   @Override protected boolean receivesGeneratedName() { return true; }

   @Override
   protected void applyStageSpecialization() {
      if (getPersistentData().getBoolean(SPECIALIZED)) return;
      boolean speed = random.nextBoolean();
      boolean resistance = random.nextBoolean();
      boolean attack = random.nextBoolean();
      if (!speed && !resistance && !attack) {
         int forced = random.nextInt(3);
         speed = forced == 0;
         resistance = forced == 1;
         attack = forced == 2;
      }
      if (speed) getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * 1.30);
      if (resistance) {
         getAttribute(Attributes.ARMOR).setBaseValue(22.0);
         getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.25);
      }
      if (attack) getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(24.0 * 1.40);
      getPersistentData().putBoolean("NightKinSpeed", speed);
      getPersistentData().putBoolean("NightKinResistance", resistance);
      getPersistentData().putBoolean("NightKinAttack", attack);
      getPersistentData().putBoolean(SPECIALIZED, true);
   }

   @Override
   protected void ensureCurrentStageAttributes() {
      double speed = getPersistentData().getBoolean("NightKinSpeed") ? 0.36 * 1.30 : 0.36;
      migrateStageAttributes(CURRENT_STATS, 150.0, speed);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (!getPersistentData().getBoolean(SPECIALIZED)) applyStageSpecialization();
   }
}
