package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

public final class HundredFacesHassanPersonaEntity extends ServantEntity {
   private static final EntityDataAccessor<Boolean> PRESENCE_CONCEALED = SynchedEntityData.defineId(
      HundredFacesHassanPersonaEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> PERSONA_MODE = SynchedEntityData.defineId(
      HundredFacesHassanPersonaEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> VISUAL_HEIGHT = SynchedEntityData.defineId(
      HundredFacesHassanPersonaEntity.class, EntityDataSerializers.FLOAT);
   private static final String TAG_EXPOSED_UNTIL = "HundredFacesPersonaExposedUntil";
   private static final String TAG_MOVEMENT_SPEED = "HundredFacesPersonaMovementSpeed";
   private static final ServantAnimations BUILT_IN_ANIMATIONS = new ServantAnimations(
      "animation.hundred_faces_hassan.standing",
      "animation.hundred_faces_hassan.walk",
      Map.of(
         "melee", "animation.hundred_faces_hassan.melee",
         "assassin_stab", "animation.hundred_faces_hassan.melee",
         "dirk_throw", "animation.hundred_faces_hassan.melee",
         "shadow_step", "animation.hundred_faces_hassan.zabaniya",
         "shadow_lunge", "animation.hundred_faces_hassan.melee"
      )
   );

   @Nullable
   private UUID ownerUuid;
   @Nullable
   private UUID targetUuid;
   private long nextAttackTick;

   public HundredFacesHassanPersonaEntity(EntityType<? extends HundredFacesHassanPersonaEntity> type, Level level) {
      super(type, level, HundredFacesHassanEntity.SERVANT_KEY);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return net.minecraft.world.entity.PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, HundredFacesHassanRules.PERSONA_BASE_HEALTH)
         .add(Attributes.ATTACK_DAMAGE, HundredFacesHassanRules.PERSONA_ATTACK_DAMAGE)
         .add(Attributes.ATTACK_SPEED, 4.0)
         .add(Attributes.MOVEMENT_SPEED, HundredFacesHassanRules.PERSONA_MOVEMENT_SPEED)
         .add(Attributes.ARMOR, HundredFacesHassanRules.PERSONA_BASE_ARMOR)
         .add(Attributes.ARMOR_TOUGHNESS, 0.0)
         .add(Attributes.FOLLOW_RANGE, 48.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, HundredFacesHassanRules.PERSONA_KNOCKBACK_RESISTANCE)
         .add(Attributes.STEP_HEIGHT, 3.0);
   }

   @Override
   protected ServantAnimations getAnimationSet() {
      return BUILT_IN_ANIMATIONS;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(PRESENCE_CONCEALED, false);
      builder.define(PERSONA_MODE, HundredFacesHassanEntity.PersonaMode.SCOUT.ordinal());
      builder.define(VISUAL_HEIGHT, 0.9F);
   }

   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData spawnData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
      this.randomizePersonaHeight();
      this.applyPersonaAttributes(1, true);
      this.ensureDefaultNpcLoadout(false);
      this.ensureDirkLoadout();
      this.setPresenceConcealed(false);
      return result;
   }

   public void initialize(HundredFacesHassanEntity owner, @Nullable LivingEntity target,
                          HundredFacesHassanEntity.PersonaMode mode, int liveCount) {
      this.ownerUuid = owner.getUUID();
      this.targetUuid = target == null ? null : target.getUUID();
      this.entityData.set(PERSONA_MODE, mode == null ? HundredFacesHassanEntity.PersonaMode.SCOUT.ordinal() : mode.ordinal());
      this.randomizePersonaHeight();
      this.syncOwnerCombatAttributes(owner);
      this.setTarget(target);
      this.applyPersonaAttributes(liveCount, true);
      this.ensureDefaultNpcLoadout(false);
      this.ensureDirkLoadout();
      this.setPresenceConcealed(target != null);
   }

   public void initialize(ServerPlayer owner, @Nullable LivingEntity target,
                          HundredFacesHassanEntity.PersonaMode mode, int liveCount) {
      this.ownerUuid = owner.getUUID();
      this.targetUuid = target == null ? null : target.getUUID();
      this.entityData.set(PERSONA_MODE, mode == null ? HundredFacesHassanEntity.PersonaMode.SCOUT.ordinal() : mode.ordinal());
      this.randomizePersonaHeight();
      this.syncOwnerCombatAttributes(owner);
      this.setTarget(target);
      this.applyPersonaAttributes(liveCount, true);
      this.ensureDefaultNpcLoadout(false);
      this.ensureDirkLoadout();
      this.setPresenceConcealed(target != null);
   }

   public void applyPersonaAttributes(int liveCount, boolean resetHealth) {
      setBase(Attributes.MAX_HEALTH, HundredFacesHassanRules.personaHealthForCount(liveCount));
      setBase(Attributes.ARMOR, HundredFacesHassanRules.personaArmorForCount(liveCount));
      setBase(Attributes.ATTACK_DAMAGE, HundredFacesHassanRules.personaAttackDamageForCount(liveCount));
      setBase(Attributes.MOVEMENT_SPEED, this.personaMovementSpeed());
      setBase(Attributes.KNOCKBACK_RESISTANCE, HundredFacesHassanRules.PERSONA_KNOCKBACK_RESISTANCE);
      if (this.getCurrentMp() > this.getMaxMp()) this.setCurrentMp(this.getMaxMp());
      if (resetHealth || this.getHealth() > this.getMaxHealth()) this.setHealth(this.getMaxHealth());
   }

   @Override
   public void tick() {
      super.tick();
      if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
      HundredFacesHassanEntity owner = this.getOwnerEntity(level);
      ServerPlayer playerOwner = this.getOwnerPlayer(level);
      if ((owner == null || !owner.isAlive()) && playerOwner == null) {
         this.discard();
         return;
      }
      long now = level.getGameTime();
      boolean exposed = now < this.getPersistentData().getLong(TAG_EXPOSED_UNTIL);
      boolean hasTarget = this.getTarget() != null && this.getTarget().isAlive();
      this.setPresenceConcealed(!exposed && hasTarget && this.distanceToSqr(this.getTarget()) > 4.0);
      if (this.tickCount % 40 == 0) this.ensureDirkLoadout();
   }

   @Override
   protected void customServerAiStep() {
      if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) return;
      HundredFacesHassanEntity owner = this.getOwnerEntity(level);
      ServerPlayer playerOwner = this.getOwnerPlayer(level);
      if ((owner == null || !owner.isAlive()) && playerOwner == null) {
         this.discard();
         return;
      }
      LivingEntity ownerLiving = owner != null && owner.isAlive() ? owner : playerOwner;
      int command = playerOwner == null ? ServantCardHundredFacesHassanSkills.COMMAND_FREE : ServantCardHundredFacesHassanSkills.personaCommand(this);
      boolean activeAttack = playerOwner == null || ServantCardHundredFacesHassanSkills.hasActiveAttack(this);
      LivingEntity target = this.resolveLocalTarget(level, ownerLiving, activeAttack);
      if (target == null) {
         this.setTarget(null);
         if (playerOwner != null) {
            if (command == ServantCardHundredFacesHassanSkills.COMMAND_HOLD) {
               this.getNavigation().stop();
            } else if (command == ServantCardHundredFacesHassanSkills.COMMAND_FOLLOW && this.distanceToSqr(playerOwner) > 36.0) {
               this.getNavigation().moveTo(playerOwner, 1.12);
            } else if (command == ServantCardHundredFacesHassanSkills.COMMAND_FREE && this.distanceToSqr(playerOwner) > 400.0) {
               this.getNavigation().moveTo(playerOwner, 1.02);
            }
         } else if (owner != null && this.distanceToSqr(owner) > 144.0) {
            this.getNavigation().moveTo(owner, 1.08);
         }
         return;
      }
      this.setTarget(target);
      this.getLookControl().setLookAt(target, 45.0F, 45.0F);
      double distance = this.distanceTo(target);
      if (HundredFacesHassanCombatHelper.tryPersonaCombatSkill(this, target, distance, level.getGameTime())) {
         return;
      }
      if (distance > 2.1) {
         this.getNavigation().moveTo(target, 1.18);
      } else if (level.getGameTime() >= this.nextAttackTick) {
         this.nextAttackTick = level.getGameTime() + 18L;
         this.triggerAssassinStabAnimation();
         this.doHurtTarget(target);
      }
   }

   @Nullable
   private LivingEntity resolveLocalTarget(ServerLevel level, LivingEntity owner, boolean activeAttack) {
      LivingEntity current = this.getTarget();
      if (activeAttack && isValidTarget(owner, current)) return current;
      if (!activeAttack && current != null && this.getLastHurtByMob() == current && isValidTarget(owner, current)) return current;
      if (this.targetUuid != null) {
         Entity entity = level.getEntity(this.targetUuid);
         if (activeAttack && entity instanceof LivingEntity living && isValidTarget(owner, living)) return living;
      }
      if (!activeAttack && isValidTarget(owner, this.getLastHurtByMob())) return this.getLastHurtByMob();
      if (this.tickCount % HundredFacesHassanRules.TARGET_SCAN_INTERVAL_TICKS != 0) return null;
      if (!activeAttack) return null;
      AABB area = this.getBoundingBox().inflate(20.0);
      return level.getEntitiesOfClass(LivingEntity.class, area, candidate -> isValidTarget(owner, candidate)).stream()
         .min(Comparator.comparingDouble(this::distanceToSqr))
         .orElse(null);
   }

   private boolean isValidTarget(LivingEntity owner, @Nullable LivingEntity candidate) {
      if (candidate == null || candidate == this || candidate == owner || !candidate.isAlive()
         || candidate instanceof HundredFacesHassanPersonaEntity
         || EntityUtils.isImmunePlayerTarget(candidate)
         || owner.isAlliedTo(candidate) || candidate.isAlliedTo(owner)
         || ServantMasterTargeting.isContractMaster(owner, candidate)) return false;
      return EntityUtils.isValidCombatTarget(owner, candidate);
   }

   @Nullable
   public HundredFacesHassanEntity getOwnerEntity(ServerLevel level) {
      if (this.ownerUuid == null) return null;
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof HundredFacesHassanEntity owner ? owner : null;
   }

   @Nullable
   public ServerPlayer getOwnerPlayer(ServerLevel level) {
      if (this.ownerUuid == null || level.getServer() == null) return null;
      return level.getServer().getPlayerList().getPlayer(this.ownerUuid);
   }

   @Nullable
   public UUID getOwnerUuid() {
      return this.ownerUuid;
   }

   public void setOwnerUuid(@Nullable UUID ownerUuid) {
      this.ownerUuid = ownerUuid;
   }

   public HundredFacesHassanEntity.PersonaMode getPersonaMode() {
      int ordinal = this.entityData.get(PERSONA_MODE);
      HundredFacesHassanEntity.PersonaMode[] modes = HundredFacesHassanEntity.PersonaMode.values();
      return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : HundredFacesHassanEntity.PersonaMode.SCOUT;
   }

   public boolean isPresenceConcealed() {
      return this.entityData.get(PRESENCE_CONCEALED);
   }

   public void revealForCombat() {
      if (this.level().isClientSide()) return;
      this.getPersistentData().putLong(TAG_EXPOSED_UNTIL, this.level().getGameTime() + HundredFacesHassanRules.CONCEALMENT_EXPOSURE_TICKS);
      this.setPresenceConcealed(false);
   }

   public void triggerDirkThrowAnimation() {
      this.triggerNamedActionAnimation("dirk_throw");
   }

   public void triggerAssassinStabAnimation() {
      this.triggerNamedActionAnimation("assassin_stab");
   }

   public void triggerShadowStepAnimation() {
      this.triggerNamedActionAnimation("shadow_step");
   }

   public float getPersonaVisualHeight() {
      return HundredFacesHassanRules.clampVisualHeight(this.entityData.get(VISUAL_HEIGHT));
   }

   public float getVisualScale() {
      return HundredFacesHassanRules.visualScaleForHeight(this.getPersonaVisualHeight());
   }

   @Override
   protected EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
      float height = this.getPersonaVisualHeight();
      return EntityDimensions.fixed(
         HundredFacesHassanRules.widthForHeight(height),
         HundredFacesHassanRules.collisionHeightForVisualHeight(height)
      );
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (VISUAL_HEIGHT.equals(key)) {
         this.refreshDimensions();
      }
   }

   private void randomizePersonaHeight() {
      int step = this.getRandom().nextInt(11);
      this.setPersonaVisualHeight(HundredFacesHassanRules.PERSONA_MIN_VISUAL_HEIGHT + step * 0.02F);
   }

   private void setPersonaVisualHeight(float height) {
      this.entityData.set(VISUAL_HEIGHT, HundredFacesHassanRules.clampVisualHeight(height));
      this.refreshDimensions();
   }

   public void syncOwnerCombatAttributes(LivingEntity owner) {
      if (owner == null) return;
      this.getPersistentData().putDouble(TAG_MOVEMENT_SPEED,
         Math.max(0.0, owner.getAttributeValue(Attributes.MOVEMENT_SPEED)));
   }

   private double personaMovementSpeed() {
      CompoundTag data = this.getPersistentData();
      return data.contains(TAG_MOVEMENT_SPEED)
         ? data.getDouble(TAG_MOVEMENT_SPEED) : HundredFacesHassanRules.PERSONA_MOVEMENT_SPEED;
   }

   @Override
   public double getMaxMp() {
      return HundredFacesHassanRules.PERSONA_E_RANK_PARAMS.manaPool();
   }

   @Override
   public double getCritRate() {
      return HundredFacesHassanRules.PERSONA_E_RANK_PARAMS.critRatePercent();
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
      this.setInvisible(false);
      this.setSilent(concealed);
      this.setCustomNameVisible(!concealed);
      if (this.hasEffect(MobEffects.INVISIBILITY)) {
         this.removeEffect(MobEffects.INVISIBILITY);
      }
   }

   private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
      AttributeInstance instance = this.getAttribute(attribute);
      if (instance != null && Math.abs(instance.getBaseValue() - value) > 1.0E-5) instance.setBaseValue(value);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      this.revealForCombat();
      boolean hit = super.doHurtTarget(target);
      if (hit && target instanceof LivingEntity living && this.getPersonaMode() == HundredFacesHassanEntity.PersonaMode.POISON) {
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
   public void die(DamageSource source) {
      ServerPlayer playerOwner = this.level() instanceof ServerLevel level ? this.getOwnerPlayer(level) : null;
      super.die(source);
      if (playerOwner != null) {
         ServantCardHundredFacesHassanSkills.notifyPersonaDeath(this, playerOwner);
      }
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) return true;
      if (this.ownerUuid != null && this.ownerUuid.equals(other.getUUID())) return true;
      if (other instanceof HundredFacesHassanPersonaEntity persona
         && this.ownerUuid != null && this.ownerUuid.equals(persona.ownerUuid)) return true;
      if (this.level() instanceof ServerLevel level) {
         HundredFacesHassanEntity owner = this.getOwnerEntity(level);
         if (owner != null && owner.isAlliedTo(other)) return true;
         ServerPlayer playerOwner = this.getOwnerPlayer(level);
         return playerOwner != null && (playerOwner.isAlliedTo(other) || other.isAlliedTo(playerOwner));
      }
      return false;
   }

   @Override
   public boolean isInvisibleTo(net.minecraft.world.entity.player.Player player) {
      return super.isInvisibleTo(player);
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
      if (this.ownerUuid != null) tag.putUUID("HundredFacesOwner", this.ownerUuid);
      if (this.targetUuid != null) tag.putUUID("HundredFacesTarget", this.targetUuid);
      tag.putLong("HundredFacesNextAttack", this.nextAttackTick);
      tag.putInt("HundredFacesMode", this.entityData.get(PERSONA_MODE));
      tag.putBoolean("HundredFacesPersonaConcealed", this.isPresenceConcealed());
      tag.putFloat("HundredFacesPersonaVisualHeight", this.getPersonaVisualHeight());
      tag.putLong(TAG_EXPOSED_UNTIL, this.getPersistentData().getLong(TAG_EXPOSED_UNTIL));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.ownerUuid = tag.hasUUID("HundredFacesOwner") ? tag.getUUID("HundredFacesOwner") : null;
      this.targetUuid = tag.hasUUID("HundredFacesTarget") ? tag.getUUID("HundredFacesTarget") : null;
      this.nextAttackTick = tag.getLong("HundredFacesNextAttack");
      this.entityData.set(PERSONA_MODE, tag.getInt("HundredFacesMode"));
      this.setPersonaVisualHeight(tag.contains("HundredFacesPersonaVisualHeight")
         ? tag.getFloat("HundredFacesPersonaVisualHeight") : 0.9F);
      this.getPersistentData().putLong(TAG_EXPOSED_UNTIL, tag.getLong(TAG_EXPOSED_UNTIL));
      this.setPresenceConcealed(tag.contains("HundredFacesPersonaConcealed") && tag.getBoolean("HundredFacesPersonaConcealed"));
      this.applyPersonaAttributes(1, false);
      this.ensureDefaultNpcLoadout(false);
      this.ensureDirkLoadout();
   }
}
