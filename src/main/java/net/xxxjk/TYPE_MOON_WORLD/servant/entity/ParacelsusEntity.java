package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;

public class ParacelsusEntity extends ServantEntity {
   public static final String SERVANT_KEY = "paracelsus";
   private static final EntityDataAccessor<Integer> COMBAT_PHASE = SynchedEntityData.defineId(ParacelsusEntity.class, EntityDataSerializers.INT);
   private static final String TAG_COMBAT_PHASE = "ParacelsusCombatPhase";

   public ParacelsusEntity(EntityType<ParacelsusEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(COMBAT_PHASE, 1);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         if (this.tickCount <= 2 && this.getMainHandItem().isEmpty()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PARACELSUS_SWORD.get()));
         }
         int phase = this.computeCombatPhase();
         if (phase != this.getCombatPhase()) {
            this.setCombatPhase(phase);
         }
         this.setNoGravity(false);
      }
   }

   @Override
   protected boolean useFloatingAnimation() {
      return false;
   }

   @Override
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      if (moving) {
         return animations.walkAnimation().orElseGet(() -> animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, true)));
      }
      if (this.getCombatPhase() >= 3) {
         return animations.actionAnimation("idle_cast").orElseGet(() ->
            animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, false))
         );
      }
      return animations.idleAnimation().orElse(super.getLoopAnimationOverride(animations, false));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt(TAG_COMBAT_PHASE, this.getCombatPhase());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setCombatPhase(tag.contains(TAG_COMBAT_PHASE) ? tag.getInt(TAG_COMBAT_PHASE) : this.computeCombatPhase());
   }

   @Override
   public double getMaxMp() {
      return Math.max(200.0, super.getMaxMp());
   }

   public int getCombatPhase() {
      return this.entityData.get(COMBAT_PHASE);
   }

   public void setCombatPhase(int phase) {
      this.entityData.set(COMBAT_PHASE, Math.max(1, Math.min(3, phase)));
   }

   public int computeCombatPhase() {
      float maxHealth = Math.max(1.0F, this.getMaxHealth());
      float ratio = this.getHealth() / maxHealth;
      if (ratio <= 0.333F) {
         return 3;
      }
      return ratio <= 0.666F ? 2 : 1;
   }
}
