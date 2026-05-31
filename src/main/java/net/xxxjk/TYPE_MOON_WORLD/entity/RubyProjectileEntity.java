package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.gravity.GemGravityFieldMagic;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class RubyProjectileEntity extends ThrowableItemProjectile {
   public final List<Vec3> tracePos = new LinkedList<>();
   private static final EntityDataAccessor<Integer> GEM_TYPE = SynchedEntityData.defineId(RubyProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> VISUAL_SCALE = SynchedEntityData.defineId(RubyProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> VISUAL_END_X = SynchedEntityData.defineId(RubyProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> VISUAL_END_Y = SynchedEntityData.defineId(RubyProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> VISUAL_END_Z = SynchedEntityData.defineId(RubyProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> HAS_VISUAL_END = SynchedEntityData.defineId(RubyProjectileEntity.class, EntityDataSerializers.BOOLEAN);

   public RubyProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData(Builder builder) {
      super.defineSynchedData(builder);
      builder.define(GEM_TYPE, 0);
      builder.define(VISUAL_SCALE, 1.0F);
      builder.define(VISUAL_END_X, 0.0F);
      builder.define(VISUAL_END_Y, 0.0F);
      builder.define(VISUAL_END_Z, 0.0F);
      builder.define(HAS_VISUAL_END, false);
   }

   public void setVisualScale(float scale) {
      this.entityData.set(VISUAL_SCALE, scale);
   }

   public float getVisualScale() {
      return (Float)this.entityData.get(VISUAL_SCALE);
   }

   public void setVisualEnd(Vec3 end) {
      this.entityData.set(VISUAL_END_X, (float)end.x);
      this.entityData.set(VISUAL_END_Y, (float)end.y);
      this.entityData.set(VISUAL_END_Z, (float)end.z);
      this.entityData.set(HAS_VISUAL_END, true);
   }

   public Vec3 getVisualEnd() {
      return !this.entityData.get(HAS_VISUAL_END)
         ? null
         : new Vec3(
            ((Float)this.entityData.get(VISUAL_END_X)).floatValue(),
            ((Float)this.entityData.get(VISUAL_END_Y)).floatValue(),
            ((Float)this.entityData.get(VISUAL_END_Z)).floatValue()
         );
   }

   public void setGemType(int type) {
      this.entityData.set(GEM_TYPE, type);
   }

   public int getGemType() {
      return (Integer)this.entityData.get(GEM_TYPE);
   }

   public RubyProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.RUBY_PROJECTILE.get(), shooter, level);
   }

   public RubyProjectileEntity(Level level, double x, double y, double z) {
      super(ModEntities.RUBY_PROJECTILE.get(), x, y, z, level);
   }

   protected Item getDefaultItem() {
      return ModItems.CARVED_RUBY_FULL.get();
   }

   protected boolean canHitEntity(Entity entity) {
      if (this.getGemType() == 99) {
         return false;
      } else {
         return entity != null && entity == this.getOwner() ? false : super.canHitEntity(entity);
      }
   }

   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!this.level().isClientSide) {
         if (this.getGemType() == 99) {
            this.discard();
            return;
         }

         boolean isRandomMode = false;
         boolean isMachineGunMode = false;
         ItemStack stack = this.getItem();
         CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         CompoundTag customTag = null;
         if (customData != null) {
            customTag = customData.copyTag();
            if (customTag.getBoolean("IsRandomMode")) {
               isRandomMode = true;
            }

            if (customTag.getBoolean("IsMachineGunMode")) {
               isMachineGunMode = true;
            }
         }

         if (GemGravityFieldMagic.tryHandleProjectileImpact(this, stack)) {
            this.discard();
            return;
         }

         if (isMachineGunMode) {
            if (result instanceof EntityHitResult entityResult) {
               Entity target = entityResult.getEntity();
               if (target == this.getOwner()) {
                  this.discard();
                  return;
               }

               if (target instanceof LivingEntity livingTarget) {
                  livingTarget.invulnerableTime = 0;
                  float damage = 6.0F;
                  if (stack.getItem() instanceof FullManaCarvedGemItem gemItem) {
                     damage *= gemItem.getQuality().getEffectMultiplier();
                  } else if (stack.getItem() instanceof CarvedGemItem gemItem) {
                     damage *= gemItem.getQuality().getEffectMultiplier();
                  }

                  target.hurt(this.damageSources().magic(), damage);
                  livingTarget.invulnerableTime = 0;
                  if (this.level().random.nextFloat() < 0.3F) {
                     int effectId = this.level().random.nextInt(4);
                     switch (effectId) {
                        case 0:
                           livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                           break;
                        case 1:
                           livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                           break;
                        case 2:
                           livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                           break;
                        case 3:
                           livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
                     }
                  }
               }
            }

            if (this.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
            }

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.NEUTRAL, 0.5F, 1.5F);
            this.discard();
            return;
         }

         if (this.getGemType() == 4 && !isRandomMode) {
            float cyanMultiplier = 1.0F;
            if (stack.getItem() instanceof FullManaCarvedGemItem gemItem) {
               cyanMultiplier = gemItem.getQuality().getEffectMultiplier();
            }

            if (customTag != null && customTag.getBoolean("IsCyanTornado") && result.getType() == Type.BLOCK) {
               float radius = 4.0F * cyanMultiplier;
               if (customTag.contains("CyanRadius")) {
                  radius = customTag.getFloat("CyanRadius");
               }

               int duration = 100;
               if (customTag.contains("CyanDuration")) {
                  duration = customTag.getInt("CyanDuration");
               }

               CyanWindFieldEntity wind = new CyanWindFieldEntity(
                  this.level(), this.getX(), this.getY(), this.getZ(), radius, duration, this.getOwner() instanceof LivingEntity l ? l : null
               );
               wind.setTornadoMode(true);
               this.level().addFreshEntity(wind);
               this.discard();
               return;
            }

            if (customTag != null && customTag.contains("ExplosionPowerMultiplier")) {
               cyanMultiplier *= customTag.getFloat("ExplosionPowerMultiplier");
            }

            CyanWindFieldEntity wind = new CyanWindFieldEntity(
               this.level(), this.getX(), this.getY(), this.getZ(), 4.0F * cyanMultiplier, 100, this.getOwner() instanceof LivingEntity l ? l : null
            );
            this.level().addFreshEntity(wind);
            this.discard();
            return;
         }

         float multiplier = 1.0F;
         if (stack.getItem() instanceof FullManaCarvedGemItem gemItem) {
            multiplier = gemItem.getQuality().getEffectMultiplier();
         }

         if (customTag != null && customTag.contains("ExplosionPowerMultiplier")) {
            multiplier *= customTag.getFloat("ExplosionPowerMultiplier");
         }

         float radiusx = 5.0F * multiplier;
         if (isRandomMode) {
            int gemType = this.getGemType();
            float manaScale = 1.0F;
            if (this.getOwner() instanceof Player p) {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)p.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
               double maxMana = Math.max(vars.player_max_mana, 1.0);
               double manaRatio = Math.max(0.0, Math.min(vars.player_mana / maxMana, 1.0));
               manaScale = (float)(0.8 + 1.2 * manaRatio);
            }

            this.explodeWithoutAffectingOwner(radiusx, false, ExplosionInteraction.NONE);
            if (this.level() instanceof ServerLevel serverLevel) {
               int particleCount = Math.max(6, (int)(8.0F + radiusx * 2.0F));
               serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
               serverLevel.sendParticles(
                  ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), particleCount, radiusx * 0.4, radiusx * 0.3, radiusx * 0.4, 0.02
               );
            }
         } else {
            this.explodeWithoutAffectingOwner(radiusx, true, ExplosionInteraction.TNT);
         }

         if (this.getOwner() instanceof LivingEntity owner) {
            for (LivingEntity t : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radiusx))) {
               if (t != owner) {
                  EntityUtils.triggerSwarmAnger(this.level(), owner, t);
               }
            }
         }

         if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 20, 1.0, 1.0, 1.0, 0.5);
            serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 30, 1.5, 1.5, 1.5, 0.1);
         }

         this.discard();
      }
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (HAS_VISUAL_END.equals(key) && this.level().isClientSide && (Boolean)this.entityData.get(HAS_VISUAL_END)) {
         this.tracePos.clear();
         Vec3 start = this.position();
         Vec3 end = this.getVisualEnd();
         if (end != null) {
            int steps = 20;

            for (int i = 0; i <= steps; i++) {
               this.tracePos.add(start.lerp(end, (float)i / steps));
            }
         }
      }
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return this.getGemType() == 99 ? distance < 65536.0 : super.shouldRenderAtSqrDistance(distance);
   }

   public AABB getBoundingBoxForCulling() {
      if (this.getGemType() == 99 && (Boolean)this.entityData.get(HAS_VISUAL_END)) {
         Vec3 end = this.getVisualEnd();
         if (end != null) {
            return this.getBoundingBox().minmax(new AABB(end, end).inflate(1.0));
         }
      }

      return super.getBoundingBoxForCulling();
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.getGemType() == 99 && this.tickCount > 40) {
         this.discard();
      } else {
         if (this.level().isClientSide) {
            boolean isRandomMode = false;
            ItemStack stack = this.getItem();
            CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.copyTag().getBoolean("IsRandomMode")) {
               isRandomMode = true;
            }

            int type = this.getGemType();
            if (type == 0) {
               this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 1) {
               this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 2) {
               this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 3) {
               this.level().addParticle(ParticleTypes.WAX_ON, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 4) {
               this.level().addParticle(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 5) {
               this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else if (type == 6) {
               this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            } else {
               this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }

            Vec3 pos = this.position();
            if (this.getGemType() == 99 && (Boolean)this.entityData.get(HAS_VISUAL_END)) {
               this.tracePos.clear();
               Vec3 start = this.position();
               Vec3 end = this.getVisualEnd();
               int steps = 20;

               for (int i = 0; i <= steps; i++) {
                  this.tracePos.add(start.lerp(end, (float)i / steps));
               }
            } else {
               boolean addPos = true;
               if (!this.tracePos.isEmpty()) {
                  Vec3 last = this.tracePos.get(this.tracePos.size() - 1);
                  if (last.distanceToSqr(pos) < 0.05) {
                     addPos = false;
                  }
               }

               if (addPos) {
                  this.tracePos.add(pos);
                  if (this.tracePos.size() > 560) {
                     this.tracePos.remove(0);
                  }
               }
            }
         }

         if (this.tickCount > 200 && !this.level().isClientSide) {
            float multiplier = 1.0F;
            ItemStack stackx = this.getItem();
            if (stackx.getItem() instanceof FullManaCarvedGemItem gemItem) {
               multiplier = gemItem.getQuality().getEffectMultiplier();
            }

            CustomData customDatax = (CustomData)stackx.get(DataComponents.CUSTOM_DATA);
            if (customDatax != null) {
               CompoundTag tag = customDatax.copyTag();
               if (tag.contains("ExplosionPowerMultiplier")) {
                  multiplier *= tag.getFloat("ExplosionPowerMultiplier");
               }
            }

            float radius = 5.0F * multiplier;
            this.explodeWithoutAffectingOwner(radius, true, ExplosionInteraction.TNT);
            if (this.getOwner() instanceof LivingEntity owner) {
               for (LivingEntity t : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius))) {
                  if (t != owner) {
                     EntityUtils.triggerSwarmAnger(this.level(), owner, t);
                  }
               }
            }

            this.discard();
         }
      }
   }

   private void explodeWithoutAffectingOwner(float radius, boolean fire, ExplosionInteraction interaction) {
      Entity owner = this.getOwner();
      Vec3 ownerVelocity = null;
      Integer ownerFireTicks = null;
      if (owner instanceof LivingEntity livingOwner) {
         ownerVelocity = livingOwner.getDeltaMovement();
         ownerFireTicks = livingOwner.getRemainingFireTicks();
      }

      this.level().explode(this, this.getX(), this.getY(), this.getZ(), radius, fire, interaction);
      if (owner instanceof LivingEntity livingOwner && ownerVelocity != null) {
         livingOwner.setDeltaMovement(ownerVelocity);
         livingOwner.hurtMarked = true;
         if (ownerFireTicks != null) {
            livingOwner.setRemainingFireTicks(ownerFireTicks);
         }
      }
   }
}
