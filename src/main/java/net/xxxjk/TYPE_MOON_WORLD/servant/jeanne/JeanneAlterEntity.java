package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.jetbrains.annotations.Nullable;

/** Built-in Avenger Jeanne d'Arc Alter. */
public final class JeanneAlterEntity extends ServantEntity {
   public static final String SERVANT_KEY = "jeanne_alter";

   public JeanneAlterEntity(EntityType<? extends JeanneAlterEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (this.level() instanceof ServerLevel level && this.isAlive()) {
         JeanneAlterClosePressureAi.tick(this, level);
      }
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData groupData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      this.ensureDefaultNpcLoadout(false);
      JeanneAlterVoice.summon(this);
      return result;
   }

   @Override
   public void ensureDefaultNpcLoadout(boolean forceClientSync) {
      super.ensureDefaultNpcLoadout(forceClientSync);
      ensureWeapon(InteractionHand.MAIN_HAND, new ItemStack(ModItems.JEANNE_ALTER_SWORD.get()), forceClientSync);
      ensureWeapon(InteractionHand.OFF_HAND, new ItemStack(ModItems.JEANNE_ALTER_FLAG.get()), forceClientSync);
   }

   private void ensureWeapon(InteractionHand hand, ItemStack expected, boolean forceClientSync) {
      ItemStack current = this.getItemInHand(hand);
      if (!current.is(expected.getItem())) {
         this.setItemInHand(hand, expected);
      } else if (forceClientSync) {
         this.setItemInHand(hand, ItemStack.EMPTY);
         this.setItemInHand(hand, current.copy());
      }
      this.setDropChance(hand == InteractionHand.MAIN_HAND
         ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
         : net.minecraft.world.entity.EquipmentSlot.OFFHAND, 0.0F);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof LivingEntity living) {
         living.igniteForSeconds(4.0F);
      }
      return hit;
   }

   @Override
   protected net.minecraft.sounds.SoundEvent getDeathSound() {
      return null;
   }
}
