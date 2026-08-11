package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import org.jetbrains.annotations.Nullable;

public final class HundredFacesHassanEntity extends ServantEntity {
   public static final String SERVANT_KEY = "hundred_faces_hassan";

   public enum PersonaMode {
      ASSASSINATION,
      STEALTH,
      TACTICS,
      POISON,
      SCOUT,
      ZAID
   }

   private static final EntityDataAccessor<Boolean> PRESENCE_CONCEALED = SynchedEntityData.defineId(
      HundredFacesHassanEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> PERSONA_MODE = SynchedEntityData.defineId(
      HundredFacesHassanEntity.class, EntityDataSerializers.INT);
   private static final String TAG_EXPOSED_UNTIL = "HundredFacesExposedUntil";
   private static final String TAG_RETREAT_UNTIL = "HundredFacesRetreatUntil";
   private static final ServantAnimations BUILT_IN_ANIMATIONS = new ServantAnimations(
      "animation.hundred_faces_hassan.standing",
      "animation.hundred_faces_hassan.walk",
      Map.of(
         "melee", "animation.hundred_faces_hassan.melee",
         "assassin_stab", "animation.hundred_faces_hassan.melee",
         "dirk_throw", "animation.hundred_faces_hassan.melee",
         "shadow_step", "animation.hundred_faces_hassan.zabaniya",
         "knife_feint", "animation.hundred_faces_hassan.melee",
         "shadow_lunge", "animation.hundred_faces_hassan.melee",
         "zabaniya", "animation.hundred_faces_hassan.zabaniya"
      )
   );

   public HundredFacesHassanEntity(EntityType<? extends HundredFacesHassanEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   protected ServantAnimations getAnimationSet() {
      return BUILT_IN_ANIMATIONS;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(PRESENCE_CONCEALED, false);
      builder.define(PERSONA_MODE, PersonaMode.STEALTH.ordinal());
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
      this.ensureDefaultNpcLoadout(false);
      this.ensureDirkLoadout();
      this.setPresenceConcealed(true);
      return result;
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
      long now = level.getGameTime();
      boolean exposed = now < this.getPersistentData().getLong(TAG_EXPOSED_UNTIL);
      boolean retreating = now < this.getPersistentData().getLong(TAG_RETREAT_UNTIL);
      boolean shouldConceal = !exposed && (this.getTarget() == null || this.distanceToSqr(this.getTarget()) > 9.0);
      this.setPresenceConcealed(shouldConceal);
      this.applyMovementSpeed(retreating ? HundredFacesHassanRules.MAIN_RETREAT_MOVEMENT_SPEED
         : shouldConceal ? HundredFacesHassanRules.MAIN_CONCEALED_MOVEMENT_SPEED
         : HundredFacesHassanRules.MAIN_MOVEMENT_SPEED);
      if (this.tickCount % 40 == 0) this.ensureDirkLoadout();
      if (retreating && this.tickCount % 10 == 0) HundredFacesHassanCombatHelper.cleanseHarmfulEffects(this);
      if (this.tickCount % 40 == 0) HundredFacesHassanCombatHelper.rescaleOwnedPersonas(this);
   }

   public void revealForCombat() {
      if (this.level().isClientSide()) return;
      this.getPersistentData().putLong(TAG_EXPOSED_UNTIL, this.level().getGameTime() + HundredFacesHassanRules.CONCEALMENT_EXPOSURE_TICKS);
      this.setPresenceConcealed(false);
   }

   public boolean isPresenceConcealed() {
      return this.entityData.get(PRESENCE_CONCEALED);
   }

   public PersonaMode getPersonaMode() {
      int ordinal = this.entityData.get(PERSONA_MODE);
      PersonaMode[] modes = PersonaMode.values();
      return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : PersonaMode.STEALTH;
   }

   public void setPersonaMode(PersonaMode mode) {
      this.entityData.set(PERSONA_MODE, mode == null ? PersonaMode.STEALTH.ordinal() : mode.ordinal());
   }

   public void startRetreat(long now) {
      this.getPersistentData().putLong(TAG_RETREAT_UNTIL, now + HundredFacesHassanRules.RETREAT_DURATION_TICKS);
      this.revealForCombat();
   }

   public boolean isRetreating() {
      return this.getPersistentData().getLong(TAG_RETREAT_UNTIL) > this.level().getGameTime();
   }

   public void triggerDirkThrowAnimation() {
      this.triggerNamedActionAnimation("dirk_throw");
      ServantVoiceHelper.tryPlayAttack(this);
   }

   public void triggerAssassinStabAnimation() {
      this.triggerNamedActionAnimation("assassin_stab");
      ServantVoiceHelper.tryPlayAttack(this);
   }

   public void triggerShadowStepAnimation() {
      this.triggerNamedActionAnimation("shadow_step");
   }

   private void ensureDirkLoadout() {
      ItemStack held = this.getMainHandItem();
      if (held.is(ModItems.DIRK_SMALL_KNIFE.get())) {
         if (held.getCount() < 30) held.setCount(30);
         return;
      }
      if (held.isEmpty()) {
         this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DIRK_SMALL_KNIFE.get(), 30));
      }
   }

   private void setPresenceConcealed(boolean concealed) {
      boolean changed = this.entityData.get(PRESENCE_CONCEALED) != concealed;
      if (changed) this.entityData.set(PRESENCE_CONCEALED, concealed);
      this.setInvisible(concealed);
      this.setSilent(concealed);
      this.setCustomNameVisible(!concealed);
      if (concealed) {
         MobEffectInstance current = this.getEffect(MobEffects.INVISIBILITY);
         if (current == null || current.getDuration() <= 10) {
            this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
         }
      } else if (this.hasEffect(MobEffects.INVISIBILITY)) {
         this.removeEffect(MobEffects.INVISIBILITY);
      }
   }

   private void applyMovementSpeed(double speed) {
      AttributeInstance attribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attribute != null && Math.abs(attribute.getBaseValue() - speed) > 1.0E-5) {
         attribute.setBaseValue(speed);
      }
   }

   @Override
   public void setTarget(@Nullable LivingEntity target) {
      LivingEntity previous = this.getTarget();
      super.setTarget(target);
      if (!this.level().isClientSide() && target != null && target != previous && this.distanceToSqr(target) <= 36.0) {
         this.revealForCombat();
      }
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      this.revealForCombat();
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof LivingEntity living && this.getPersonaMode() == PersonaMode.POISON) {
         HundredFacesHassanCombatHelper.applyWeakPoison(this, living);
      }
      return hit;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      boolean hurt = super.hurt(source, amount);
      if (hurt) this.revealForCombat();
      return hurt;
   }

   @Override
   public void die(DamageSource cause) {
      HundredFacesHassanCombatHelper.discardOwnedPersonas(this);
      super.die(cause);
   }

   @Override
   public boolean isInvisibleTo(net.minecraft.world.entity.player.Player player) {
      return this.isPresenceConcealed() || super.isInvisibleTo(player);
   }

   @Override
   public boolean isCustomNameVisible() {
      return !this.isPresenceConcealed() && super.isCustomNameVisible();
   }

   @Override
   public boolean isCurrentlyGlowing() {
      return !this.isPresenceConcealed() && super.isCurrentlyGlowing();
   }

   @Override
   public boolean displayFireAnimation() {
      return !this.isPresenceConcealed() && super.displayFireAnimation();
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("HundredFacesConcealed", this.isPresenceConcealed());
      tag.putInt("HundredFacesPersonaMode", this.entityData.get(PERSONA_MODE));
      tag.putLong(TAG_EXPOSED_UNTIL, this.getPersistentData().getLong(TAG_EXPOSED_UNTIL));
      tag.putLong(TAG_RETREAT_UNTIL, this.getPersistentData().getLong(TAG_RETREAT_UNTIL));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(PERSONA_MODE, tag.getInt("HundredFacesPersonaMode"));
      this.getPersistentData().putLong(TAG_EXPOSED_UNTIL, tag.getLong(TAG_EXPOSED_UNTIL));
      this.getPersistentData().putLong(TAG_RETREAT_UNTIL, tag.getLong(TAG_RETREAT_UNTIL));
      this.setPresenceConcealed(tag.contains("HundredFacesConcealed") && tag.getBoolean("HundredFacesConcealed"));
      this.ensureDefaultNpcLoadout(false);
      this.ensureDirkLoadout();
   }
}
