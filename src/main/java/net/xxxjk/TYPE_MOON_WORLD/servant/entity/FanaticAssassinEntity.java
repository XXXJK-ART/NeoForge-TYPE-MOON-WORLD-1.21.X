package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import org.jetbrains.annotations.Nullable;

public final class FanaticAssassinEntity extends ServantEntity {
   public static final String SERVANT_KEY = "fanatic_assassin";
   public static final int TECHNIQUE_NONE = 0;
   public static final int TECHNIQUE_HEARTBEAT = 1;
   public static final int TECHNIQUE_MARROW = 2;
   public static final int TECHNIQUE_HAIR = 3;
   public static final int TECHNIQUE_TEMPERATURE = 4;
   public static final int TECHNIQUE_NERVES = 5;
   public static final int TECHNIQUE_COMPUTER = 6;
   public static final int TECHNIQUE_TOXIN = 7;
   public static final int TECHNIQUE_JINN = 8;
   private static final EntityDataAccessor<Integer> ACTIVE_TECHNIQUE = SynchedEntityData.defineId(
      FanaticAssassinEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Long> TECHNIQUE_UNTIL = SynchedEntityData.defineId(
      FanaticAssassinEntity.class, EntityDataSerializers.LONG);
   private static final ResourceLocation CRYSTAL_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "fanatic_crystal_armor");
   private static final String LAST_REVEAL_TICK = "FanaticLastRevealTick";
   private static final ServantAnimations BUILT_IN_ANIMATIONS = new ServantAnimations(
      "animation.fanatic_assassin.standing",
      "animation.fanatic_assassin.walk",
      Map.ofEntries(
         Map.entry("melee", "animation.fanatic_assassin.melee"),
         Map.entry("heartbeat", "animation.fanatic_assassin.heartbeat"),
         Map.entry("marrow", "animation.fanatic_assassin.marrow"),
         Map.entry("hair", "animation.fanatic_assassin.hair"),
         Map.entry("temperature", "animation.fanatic_assassin.temperature"),
         Map.entry("nerves", "animation.fanatic_assassin.nerves"),
         Map.entry("computer", "animation.fanatic_assassin.computer"),
         Map.entry("toxin", "animation.fanatic_assassin.toxin"),
         Map.entry("jinn", "animation.fanatic_assassin.jinn")
      )
   );

   public FanaticAssassinEntity(EntityType<? extends FanaticAssassinEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }

   @Override
   protected ServantAnimations getAnimationSet() {
      return BUILT_IN_ANIMATIONS;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ACTIVE_TECHNIQUE, TECHNIQUE_NONE);
      builder.define(TECHNIQUE_UNTIL, 0L);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
      this.setPresenceConcealed(false);
      return result;
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
      long now = level.getGameTime();
      if (this.getTechniqueUntil() > 0L && now >= this.getTechniqueUntil()) {
         this.setActiveTechnique(TECHNIQUE_NONE, 0L);
      }
      this.tickCrystalArmor(now);
      this.tickTechniqueExpirations(level, now);
      this.tickConcealment(now);
      if (this.tickCount % FanaticAssassinRules.MENTAL_CLEANSE_INTERVAL == 0) this.purgeMentalEffects();
      FanaticAssassinCombatHelper.spawnSustainedTechniqueEffects(this, level);
   }

   private void tickConcealment(long now) {
      boolean combat = this.getTarget() != null && this.getTarget().isAlive();
      if (combat) {
         this.getPersistentData().putLong("FanaticLastCombatTick", now);
         // Presence concealment is an approach tool, not a combat idle state.
         // Once a target is acquired the NPC must remain visible and keep its
         // attack loop running instead of re-concealing every 30 ticks.
         this.setPresenceConcealed(false);
      } else {
         // Keep an idle NPC visible so its owner can find and command it.
         this.setPresenceConcealed(false);
      }
   }

   private void tickCrystalArmor(long now) {
      AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
      if (armor == null) return;
      long until = this.getPersistentData().getLong("FanaticTemperatureUntil");
      AttributeModifier current = armor.getModifier(CRYSTAL_ARMOR_ID);
      if (until > now) {
         if (current == null) armor.addTransientModifier(new AttributeModifier(
            CRYSTAL_ARMOR_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      } else {
         if (until > 0L) this.getPersistentData().remove("FanaticTemperatureUntil");
         if (current != null) {
            armor.removeModifier(CRYSTAL_ARMOR_ID);
            FanaticAssassinCombatHelper.spawnTemperatureRelease((ServerLevel)this.level(), this);
         }
      }
   }

   private void tickTechniqueExpirations(ServerLevel level, long now) {
      long toxinUntil = this.getPersistentData().getLong("FanaticToxinUntil");
      if (toxinUntil > 0L && now >= toxinUntil) {
         this.getPersistentData().remove("FanaticToxinUntil");
         FanaticAssassinCombatHelper.spawnToxinRelease(level, this);
      }
      long nervesUntil = this.getPersistentData().getLong("FanaticNervesUntil");
      if (nervesUntil > 0L && now >= nervesUntil) {
         this.getPersistentData().remove("FanaticNervesUntil");
      }
   }

   public void activateCrystalArmor(long until) {
      this.getPersistentData().putLong("FanaticTemperatureUntil", until);
      this.tickCrystalArmor(this.level().getGameTime());
   }

   public boolean isCrystalArmorActive() {
      return this.getPersistentData().getLong("FanaticTemperatureUntil") > this.level().getGameTime();
   }

   public boolean isNervesActive() {
      return this.getPersistentData().getLong("FanaticNervesUntil") > this.level().getGameTime();
   }

   public boolean isToxinStanceActive() {
      return this.getPersistentData().getLong("FanaticToxinUntil") > this.level().getGameTime();
   }

   public void revealForCombat() {
      this.getPersistentData().putLong("FanaticLastCombatTick", this.level().getGameTime());
      this.getPersistentData().putLong(LAST_REVEAL_TICK, this.level().getGameTime());
      this.setPresenceConcealed(false);
   }

   public boolean isPresenceConcealed() {
      return this.isInvisible();
   }

   private void setPresenceConcealed(boolean concealed) {
      this.setInvisible(concealed);
      this.setCustomNameVisible(!concealed);
   }

   @Override
   public void setTarget(@Nullable net.minecraft.world.entity.LivingEntity target) {
      net.minecraft.world.entity.LivingEntity previous = this.getTarget();
      super.setTarget(target);
      if (!this.level().isClientSide() && target != null && target != previous) {
         this.revealForCombat();
         ServantVoiceHelper.tryPlayFanaticEncounter(this);
      }
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      this.revealForCombat();
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof net.minecraft.world.entity.LivingEntity living) {
         ServantVoiceHelper.tryPlayAttack(this);
         if (this.isToxinStanceActive()) FanaticAssassinCombatHelper.applyToxin(this, living);
      }
      return hit;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (source.is(FanaticDamageTypes.MENTAL_ATTACKS)
         && FanaticAssassinRules.shouldCancelMentalAttack(this.getRandom().nextFloat())) return false;
      boolean hurt = super.hurt(source, amount);
      if (hurt) this.revealForCombat();
      return hurt;
   }

   public void purgeMentalEffects() {
      for (MobEffectInstance effect : List.copyOf(this.getActiveEffects())) {
         if (effect.getEffect().is(FanaticAssassinCombatHelper.MENTAL_EFFECTS)) this.removeEffect(effect.getEffect());
      }
   }

   @Override
   public void die(DamageSource source) {
      this.removeCrystalArmor();
      ServantVoiceHelper.tryPlayFail(this);
      FanaticAssassinCombatHelper.discardOwnedJinn(this);
      super.die(source);
   }

   private void removeCrystalArmor() {
      AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
      if (armor != null) armor.removeModifier(CRYSTAL_ARMOR_ID);
   }

   public int getActiveTechnique() {
      return this.entityData.get(ACTIVE_TECHNIQUE);
   }

   public long getTechniqueUntil() {
      return this.entityData.get(TECHNIQUE_UNTIL);
   }

   public void setActiveTechnique(int technique, long until) {
      this.entityData.set(ACTIVE_TECHNIQUE, technique);
      this.entityData.set(TECHNIQUE_UNTIL, until);
   }

   @Override
   public boolean isInvisibleTo(Player player) {
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
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      return moving ? animations.walkAnimation().orElseGet(() -> animations.idleAnimation().orElse(null))
         : animations.idleAnimation().orElse(null);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("FanaticTechnique", this.getActiveTechnique());
      tag.putLong("FanaticTechniqueUntil", this.getTechniqueUntil());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setActiveTechnique(tag.getInt("FanaticTechnique"), tag.getLong("FanaticTechniqueUntil"));
   }
}
