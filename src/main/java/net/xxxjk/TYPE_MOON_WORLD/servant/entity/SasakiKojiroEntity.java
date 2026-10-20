package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroActionAnimationService;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService;

public class SasakiKojiroEntity extends ServantEntity {
   public static final String SERVANT_KEY = "sasaki_kojiro";
   private static final EntityDataAccessor<Integer> SASAKI_ACTION = SynchedEntityData.defineId(
      SasakiKojiroEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Long> SASAKI_ACTION_START = SynchedEntityData.defineId(
      SasakiKojiroEntity.class, EntityDataSerializers.LONG);

   public SasakiKojiroEntity(EntityType<SasakiKojiroEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SASAKI_ACTION, 0);
      builder.define(SASAKI_ACTION_START, 0L);
   }

   @Override
   public void tick() {
      super.tick();
      SasakiKojiroCombatService.tick(this);
   }

   public void triggerSasakiAction(SasakiKojiroAction action) {
      if (action == null) return;
      this.entityData.set(SASAKI_ACTION, action.networkId());
      this.entityData.set(SASAKI_ACTION_START, this.level().getGameTime());
   }

   public SasakiKojiroAction getSasakiAction() {
      return SasakiKojiroAction.byNetworkId(this.entityData.get(SASAKI_ACTION));
   }

   public float getSasakiActionAge(float partialTick) {
      return (float)(this.level().getGameTime() - this.entityData.get(SASAKI_ACTION_START)) + partialTick;
   }

   public boolean isSasakiActionActive() {
      SasakiKojiroAction action = getSasakiAction();
      float age = getSasakiActionAge(0.0F);
      return action != null && age >= 0.0F && age < action.durationTicks();
   }

   @Override
   public boolean hasActionAnimation(String key) {
      if ("uppercut".equals(key) || "horizontal_swing".equals(key) || "tsurigameshi".equals(key)) return true;
      return super.hasActionAnimation(key);
   }

   @Override
   public void triggerUppercutAnimation() {
      super.triggerUppercutAnimation();
      SasakiKojiroActionAnimationService.play(this, SasakiKojiroAction.UPWARD_SLASH);
   }

   @Override
   public void triggerHorizontalSwingAnimation() {
      super.triggerHorizontalSwingAnimation();
      SasakiKojiroActionAnimationService.play(this, SasakiKojiroAction.HORIZONTAL_SLASH);
   }

   @Override
   public void triggerAttackSwing() {
      if (SasakiKojiroCombatService.isConcealed(this)) SasakiKojiroCombatService.setConcealed(this, false);
      super.triggerAttackSwing();
   }

   @Override
   public void triggerTsurigameshiAnimation() {
      SasakiKojiroCombatService.playTsubamePresentation(this);
   }
}
