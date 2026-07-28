package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

public final class FanaticAssassinJinnEntity extends PathfinderMob {
   public static final int FORM_BEAST = 0;
   public static final int FORM_SERPENT = 1;
   private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(
      FanaticAssassinJinnEntity.class, EntityDataSerializers.INT);
   private UUID ownerUuid;
   private UUID targetUuid;
   private long expiresAt;
   private long nextAttackTick;

   public FanaticAssassinJinnEntity(EntityType<? extends FanaticAssassinJinnEntity> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, FanaticAssassinRules.JINN_MAX_HEALTH)
         .add(Attributes.ATTACK_DAMAGE, FanaticAssassinRules.JINN_DAMAGE)
         .add(Attributes.MOVEMENT_SPEED, 0.33)
         .add(Attributes.ARMOR, 4.0)
         .add(Attributes.FOLLOW_RANGE, 48.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FORM, FORM_BEAST);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
   }

   public void initialize(FanaticAssassinEntity owner, long expiresAt) {
      initialize(owner, owner.getTarget(), expiresAt);
   }

   public void initialize(LivingEntity owner, @Nullable LivingEntity target, long expiresAt) {
      this.ownerUuid = owner.getUUID();
      this.targetUuid = target == null ? null : target.getUUID();
      this.expiresAt = expiresAt;
      this.setHealth(FanaticAssassinRules.JINN_MAX_HEALTH);
      this.setTarget(target);
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (!(this.level() instanceof ServerLevel level)) return;
      LivingEntity owner = this.getOwner();
      if (!ownerStillValid(owner) || level.getGameTime() >= this.expiresAt) {
         level.sendParticles(ParticleTypes.ASH, this.getX(), this.getY() + 0.8, this.getZ(), 30, 0.8, 0.8, 0.8, 0.025);
         level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.8, this.getZ(), 16, 0.65, 0.75, 0.65, 0.018);
         this.discard();
         return;
      }

      LivingEntity target = resolveTarget(level, owner);
      if (target == null) {
         this.setTarget(null);
         if (this.distanceToSqr(owner) > 16.0) this.getNavigation().moveTo(owner, 1.05);
         return;
      }
      this.targetUuid = target.getUUID();
      this.setTarget(target);
      double distance = this.distanceTo(target);
      this.setForm(FanaticAssassinRules.jinnForm(distance));
      this.getLookControl().setLookAt(target, 45.0F, 45.0F);
      long now = level.getGameTime();
      if (this.getForm() == FORM_BEAST) {
         if (distance > 2.2) this.getNavigation().moveTo(target, 1.18);
         else if (now >= this.nextAttackTick) {
            this.nextAttackTick = now + 20L;
            target.invulnerableTime = 0;
            target.hurt(owner.damageSources().source(FanaticDamageTypes.JINN, this, owner), FanaticAssassinRules.JINN_DAMAGE);
            level.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + target.getBbHeight() * 0.5,
               target.getZ(), 12, 0.35, 0.4, 0.35, 0.04);
         }
      } else {
         if (distance > 12.0) this.getNavigation().moveTo(target, 1.12);
         else if (distance < 5.0) {
            Vec3 away = this.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-4) this.getNavigation().moveTo(
               this.getX() + away.normalize().x * 5.0, this.getY(), this.getZ() + away.normalize().z * 5.0, 1.0);
         }
         if (distance <= 16.0 && now >= this.nextAttackTick) {
            this.nextAttackTick = now + 40L;
            fireFogBolt(level, owner, target);
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel level && this.tickCount % 3 == 0) {
         FanaticAssassinCombatHelper.spawnJinnAura(level, this);
         level.sendParticles(ParticleTypes.ASH, this.getX(), this.getY() + 0.8, this.getZ(), 4, 0.55, 0.65, 0.55, 0.018);
      }
   }

   private void fireFogBolt(ServerLevel level, LivingEntity owner, LivingEntity target) {
      Vec3 from = this.position().add(0.0, 0.8, 0.0);
      Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 delta = to.subtract(from);
      for (int i = 0; i <= 12; i++) {
         Vec3 point = from.add(delta.scale(i / 12.0));
         level.sendParticles(ParticleTypes.SQUID_INK, point.x, point.y, point.z, 1, 0.06, 0.06, 0.06, 0.0);
         if ((i & 1) == 0) level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
      target.invulnerableTime = 0;
      target.hurt(owner.damageSources().source(FanaticDamageTypes.JINN, this, owner), FanaticAssassinRules.JINN_DAMAGE);
      target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true, true), owner);
   }

   public int getForm() {
      return this.entityData.get(FORM);
   }

   private void setForm(int form) {
      int normalized = form == FORM_SERPENT ? FORM_SERPENT : FORM_BEAST;
      if (this.entityData.get(FORM) == normalized) return;
      this.entityData.set(FORM, normalized);
      if (this.level() instanceof ServerLevel level) {
         FanaticAssassinCombatHelper.spawnJinnTransitionFx(level, this);
      }
   }

   @Nullable
   public LivingEntity getOwner() {
      if (this.ownerUuid == null || !(this.level() instanceof ServerLevel level)) return null;
      Entity entity = level.getEntity(this.ownerUuid);
      return entity instanceof LivingEntity owner ? owner : null;
   }

   @Nullable
   private LivingEntity resolveTarget(ServerLevel level, LivingEntity owner) {
      if (owner instanceof FanaticAssassinEntity fanatic) {
         LivingEntity target = fanatic.getTarget();
         if (EntityUtils.isValidCombatTarget(owner, target)) return target;
         return null;
      }
      if (this.targetUuid != null) {
         Entity entity = level.getEntity(this.targetUuid);
         if (entity instanceof LivingEntity living && EntityUtils.isValidCombatTarget(owner, living)) return living;
      }
      return level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(24.0),
         candidate -> EntityUtils.isValidCombatTarget(owner, candidate)).stream()
         .min(java.util.Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
   }

   private static boolean ownerStillValid(@Nullable LivingEntity owner) {
      if (owner == null || !owner.isAlive()) return false;
      if (!(owner instanceof ServerPlayer player)) return true;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "fanatic_assassin".equals(vars.servant_card_id);
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      LivingEntity owner = this.getOwner();
      return super.isAlliedTo(other) || owner != null && (other == owner || owner.isAlliedTo(other));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.ownerUuid != null) tag.putUUID("FanaticJinnOwner", this.ownerUuid);
      if (this.targetUuid != null) tag.putUUID("FanaticJinnTarget", this.targetUuid);
      tag.putLong("FanaticJinnExpires", this.expiresAt);
      tag.putLong("FanaticJinnNextAttack", this.nextAttackTick);
      tag.putInt("FanaticJinnForm", this.getForm());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.ownerUuid = tag.hasUUID("FanaticJinnOwner") ? tag.getUUID("FanaticJinnOwner") : null;
      this.targetUuid = tag.hasUUID("FanaticJinnTarget") ? tag.getUUID("FanaticJinnTarget") : null;
      this.expiresAt = tag.getLong("FanaticJinnExpires");
      this.nextAttackTick = tag.getLong("FanaticJinnNextAttack");
      this.entityData.set(FORM, tag.getInt("FanaticJinnForm") == FORM_SERPENT ? FORM_SERPENT : FORM_BEAST);
   }
}
