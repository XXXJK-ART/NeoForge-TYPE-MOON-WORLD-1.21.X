package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotBerserkerCombatAi;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;
import org.jetbrains.annotations.Nullable;

public class LancelotBerserkerEntity extends ServantEntity {
   public static final String SERVANT_KEY = "lancelot_berserker";

   public LancelotBerserkerEntity(EntityType<? extends LancelotBerserkerEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
      this.ensureLancelotLoadout();
      return result;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         this.ensureLancelotLoadout();
         if (this.level() instanceof ServerLevel level) {
            LancelotCombatHelper.tick(this, level);
         }
         ServantSprintCollisionHelper.tickNpcSprintCollision(this);
      }
   }

   @Override
   protected void customServerAiStep() {
      if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) {
         super.customServerAiStep();
         return;
      }
      super.customServerAiStep();
      LancelotBerserkerCombatAi.tick(this, level);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (!(target instanceof LivingEntity living) || living == this || !living.isAlive()
         || this.isAlliedTo(living) || living.isAlliedTo(this)
         || net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection.isProtectedMaster(this, living)) {
         return false;
      }
      boolean hit = super.doHurtTarget(target);
      if (hit) {
         LancelotCombatHelper.applyWeaponHit(this, living, this.getMainHandItem());
         ServantVoiceHelper.tryPlayAttack(this);
      }
      return hit;
   }

   @Override
   public void die(DamageSource cause) {
      LancelotCombatHelper.stopAroundight(this, false);
      super.die(cause);
   }

   @Override
   public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean(LancelotCombatHelper.AROUNDIGHT_MODE_TAG, this.getPersistentData().getBoolean(LancelotCombatHelper.AROUNDIGHT_MODE_TAG));
      tag.putLong(LancelotCombatHelper.AROUNDIGHT_DRAIN_TICK_TAG, this.getPersistentData().getLong(LancelotCombatHelper.AROUNDIGHT_DRAIN_TICK_TAG));
      tag.putLong(LancelotCombatHelper.AROUNDIGHT_COOLDOWN_UNTIL_TAG, this.getPersistentData().getLong(LancelotCombatHelper.AROUNDIGHT_COOLDOWN_UNTIL_TAG));
      tag.putLong(LancelotCombatHelper.UNABLE_UNTIL_TAG, this.getPersistentData().getLong(LancelotCombatHelper.UNABLE_UNTIL_TAG));
   }

   @Override
   public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.getPersistentData().putBoolean(LancelotCombatHelper.AROUNDIGHT_MODE_TAG, tag.getBoolean(LancelotCombatHelper.AROUNDIGHT_MODE_TAG));
      this.getPersistentData().putLong(LancelotCombatHelper.AROUNDIGHT_DRAIN_TICK_TAG, tag.getLong(LancelotCombatHelper.AROUNDIGHT_DRAIN_TICK_TAG));
      this.getPersistentData().putLong(LancelotCombatHelper.AROUNDIGHT_COOLDOWN_UNTIL_TAG, tag.getLong(LancelotCombatHelper.AROUNDIGHT_COOLDOWN_UNTIL_TAG));
      this.getPersistentData().putLong(LancelotCombatHelper.UNABLE_UNTIL_TAG, tag.getLong(LancelotCombatHelper.UNABLE_UNTIL_TAG));
      this.ensureLancelotLoadout();
   }

   private void ensureLancelotLoadout() {
      if (this.getMainHandItem().isEmpty()) {
         this.setItemInHand(InteractionHand.MAIN_HAND,
            LancelotCombatHelper.knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), this));
      }
      this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SERVANT_CARD_LANCELOT_BERSERKER_HEAD.get()));
      this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.SERVANT_CARD_LANCELOT_BERSERKER_CHEST.get()));
      this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.SERVANT_CARD_LANCELOT_BERSERKER_LEGS.get()));
      this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
      this.setDropChance(EquipmentSlot.HEAD, 0.0F);
      this.setDropChance(EquipmentSlot.CHEST, 0.0F);
      this.setDropChance(EquipmentSlot.LEGS, 0.0F);
      this.setDropChance(EquipmentSlot.FEET, 0.0F);
   }
}
