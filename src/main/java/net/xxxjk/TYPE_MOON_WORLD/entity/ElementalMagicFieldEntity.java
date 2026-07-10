package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.magic.player.MercurySwordMagicAmplifier;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class ElementalMagicFieldEntity extends Entity {
   public static final int ELEMENT_FIRE = 0;
   public static final int ELEMENT_WATER = 1;
   public static final int ELEMENT_WIND = 2;
   public static final int ELEMENT_EARTH = 3;
   public static final int FORM_FIRE_WALL = 0;
   public static final int FORM_WATER_PRISON = 1;
   public static final int FORM_WATER_SCREEN = 2;
   public static final int FORM_WIND_TORNADO = 3;
   public static final int FORM_WIND_WALL = 4;
   public static final int FORM_EARTH_WALL = 5;
   public static final int FORM_EARTH_PRISON = 6;
   public static final int FORM_EARTH_QUAKE = 7;
   private static final EntityDataAccessor<Integer> ELEMENT = SynchedEntityData.defineId(ElementalMagicFieldEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(ElementalMagicFieldEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(ElementalMagicFieldEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(ElementalMagicFieldEntity.class, EntityDataSerializers.FLOAT);
   private int duration = 100;
   private float damagePerSecond = 0.0F;
   private LivingEntity owner;
   private UUID ownerUUID;
   private final List<ElementalMagicFieldEntity.BlockSnapshot> snapshots = new ArrayList<>();

   public ElementalMagicFieldEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public ElementalMagicFieldEntity(Level level, double x, double y, double z, int element, int form, float radius, float width, int duration, LivingEntity owner) {
      this(ModEntities.ELEMENTAL_MAGIC_FIELD.get(), level);
      this.setPos(x, y, z);
      this.entityData.set(ELEMENT, element);
      this.entityData.set(FORM, form);
      this.entityData.set(RADIUS, Math.max(0.5F, radius));
      this.entityData.set(WIDTH, Math.max(1.0F, width));
      this.duration = Math.max(5, duration);
      this.setOwner(owner);
   }

   public void setDamagePerSecond(float damagePerSecond) {
      this.damagePerSecond = Math.max(0.0F, damagePerSecond);
   }

   public void setOwner(LivingEntity owner) {
      this.owner = owner;
      this.ownerUUID = owner == null ? null : owner.getUUID();
   }

   public LivingEntity getOwner() {
      if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
         Entity entity = serverLevel.getEntity(this.ownerUUID);
         if (entity instanceof LivingEntity living) {
            this.owner = living;
         }
      }
      return this.owner;
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(ELEMENT, ELEMENT_FIRE);
      builder.define(FORM, FORM_FIRE_WALL);
      builder.define(RADIUS, 3.0F);
      builder.define(WIDTH, 5.0F);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(ELEMENT, tag.getInt("Element"));
      this.entityData.set(FORM, tag.getInt("Form"));
      this.entityData.set(RADIUS, tag.getFloat("Radius"));
      this.entityData.set(WIDTH, tag.getFloat("Width"));
      this.duration = tag.getInt("Duration");
      this.damagePerSecond = tag.getFloat("DamagePerSecond");
      if (tag.hasUUID("Owner")) {
         this.ownerUUID = tag.getUUID("Owner");
      }
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putInt("Element", this.entityData.get(ELEMENT));
      tag.putInt("Form", this.entityData.get(FORM));
      tag.putFloat("Radius", this.entityData.get(RADIUS));
      tag.putFloat("Width", this.entityData.get(WIDTH));
      tag.putInt("Duration", this.duration);
      tag.putFloat("DamagePerSecond", this.damagePerSecond);
      if (this.ownerUUID != null) {
         tag.putUUID("Owner", this.ownerUUID);
      }
   }

   public void tick() {
      super.tick();
      if (this.tickCount == 1 && !this.level().isClientSide) {
         placeTemporaryBlocks();
      }
      if (this.level().isClientSide) {
         spawnClientParticles();
         return;
      }
      if (this.tickCount >= this.duration) {
         restoreBlocks();
         this.discard();
         return;
      }
      applyServerEffects();
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide) {
         restoreBlocks();
      }
      super.remove(reason);
   }

   private void applyServerEffects() {
      int form = this.entityData.get(FORM);
      float radius = this.entityData.get(RADIUS);
      LivingEntity ownerEntity = getOwner();
      AABB box = this.getBoundingBox().inflate(radius, Math.max(2.0, radius), radius);
      if (form == FORM_WIND_WALL || form == FORM_WATER_SCREEN) {
         for (Projectile projectile : this.level().getEntitiesOfClass(Projectile.class, box)) {
            if (projectile.getOwner() != ownerEntity) {
               projectile.discard();
            }
         }
      }
      if (form == FORM_WIND_TORNADO) {
         for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, box)) {
            Vec3 away = item.position().subtract(this.position());
            if (away.lengthSqr() < 0.001) {
               away = new Vec3(this.random.nextDouble() - 0.5, 0.2, this.random.nextDouble() - 0.5);
            }
            item.setDeltaMovement(away.normalize().scale(0.35).add(0.0, 0.12, 0.0));
            item.hurtMarked = true;
         }
      }
      for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != ownerEntity && !EntityUtils.isImmunePlayerTarget(e))) {
         if (!isInsideField(living, form, radius)) {
            continue;
         }
         if (form == FORM_FIRE_WALL) {
            living.igniteForSeconds(2.0F);
            hurtEverySecond(ownerEntity, living, 4.0F);
         } else if (form == FORM_WATER_PRISON) {
            living.setDeltaMovement(Vec3.ZERO);
            living.hurtMarked = true;
            hurtEverySecond(ownerEntity, living, this.damagePerSecond);
         } else if (form == FORM_WIND_TORNADO) {
            living.fallDistance = 0.0F;
            living.setDeltaMovement(living.getDeltaMovement().add(0.0, 0.18, 0.0));
            living.hurtMarked = true;
            hurtEverySecond(ownerEntity, living, this.damagePerSecond);
         } else if (form == FORM_EARTH_PRISON) {
            living.setDeltaMovement(Vec3.ZERO);
            living.hurtMarked = true;
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5, false, true, true));
            hurtEverySecond(ownerEntity, living, this.damagePerSecond);
         } else if (form == FORM_EARTH_QUAKE) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
            if (this.random.nextFloat() < 0.08F) {
               living.setDeltaMovement(living.getDeltaMovement().add(0.0, -0.15, 0.0));
               living.hurtMarked = true;
            }
         }
      }
   }

   private void hurtEverySecond(LivingEntity ownerEntity, LivingEntity target, float amount) {
      if (amount <= 0.0F || this.tickCount % 20 != 0) {
         return;
      }
      float damage = ownerEntity == null ? amount : MercurySwordMagicAmplifier.amplifyDamage(ownerEntity, amount);
      DamageSource source = this.damageSources().magic();
      target.invulnerableTime = 0;
      target.hurt(source, damage);
      target.invulnerableTime = 0;
   }

   private boolean isInsideField(LivingEntity living, int form, float radius) {
      double dx = living.getX() - this.getX();
      double dz = living.getZ() - this.getZ();
      if (form == FORM_FIRE_WALL || form == FORM_WATER_SCREEN || form == FORM_WIND_WALL || form == FORM_EARTH_WALL) {
         return Math.abs(dx) <= this.entityData.get(WIDTH) * 0.5 + 0.6 && Math.abs(dz) <= 1.3;
      }
      return dx * dx + dz * dz <= radius * radius;
   }

   private void placeTemporaryBlocks() {
      int form = this.entityData.get(FORM);
      if (form == FORM_FIRE_WALL) {
         placeLine(Blocks.FIRE.defaultBlockState(), 5, 1, true);
      } else if (form == FORM_EARTH_WALL) {
         placeLine(Blocks.PACKED_MUD.defaultBlockState(), 2, 2, true);
      } else if (form == FORM_EARTH_PRISON) {
         placeRing(Blocks.COBBLESTONE.defaultBlockState(), 2, 3);
      } else if (form == FORM_WATER_SCREEN) {
         placeLine(Blocks.WATER.defaultBlockState(), 5, 2, true);
      }
   }

   private void placeLine(BlockState state, int width, int height, boolean restore) {
      BlockPos center = this.blockPosition();
      for (int x = -width / 2; x <= width / 2; x++) {
         for (int y = 0; y < height; y++) {
            tryPlace(center.offset(x, y, 0), state, restore);
         }
      }
   }

   private void placeRing(BlockState state, int radius, int height) {
      BlockPos center = this.blockPosition();
      for (int x = -radius; x <= radius; x++) {
         for (int z = -radius; z <= radius; z++) {
            boolean edge = Math.abs(x) == radius || Math.abs(z) == radius;
            if (!edge) {
               continue;
            }
            for (int y = 0; y < height; y++) {
               tryPlace(center.offset(x, y, z), state, true);
            }
         }
      }
   }

   private void tryPlace(BlockPos pos, BlockState state, boolean restore) {
      BlockState old = this.level().getBlockState(pos);
      if (!old.isAir() && !old.canBeReplaced()) {
         return;
      }
      if (restore) {
         this.snapshots.add(new ElementalMagicFieldEntity.BlockSnapshot(pos.immutable(), old));
      }
      this.level().setBlock(pos, state, 3);
   }

   private void restoreBlocks() {
      if (this.snapshots.isEmpty()) {
         return;
      }
      List<ElementalMagicFieldEntity.BlockSnapshot> copy = new ArrayList<>(this.snapshots);
      this.snapshots.clear();
      for (ElementalMagicFieldEntity.BlockSnapshot snapshot : copy) {
         BlockState current = this.level().getBlockState(snapshot.pos);
         if (current.is(Blocks.FIRE) || current.is(Blocks.PACKED_MUD) || current.is(Blocks.COBBLESTONE) || current.is(Blocks.WATER)) {
            this.level().setBlock(snapshot.pos, snapshot.state, 3);
         }
      }
   }

   private void spawnClientParticles() {
      int form = this.entityData.get(FORM);
      ParticleOptions particle = switch (this.entityData.get(ELEMENT)) {
         case ELEMENT_WATER -> ParticleTypes.SPLASH;
         case ELEMENT_WIND -> ParticleTypes.CLOUD;
         case ELEMENT_EARTH -> ParticleTypes.POOF;
         default -> ParticleTypes.FLAME;
      };
      float radius = this.entityData.get(RADIUS);
      int count = form == FORM_WIND_TORNADO ? 18 : 8;
      for (int i = 0; i < count; i++) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double r = form == FORM_FIRE_WALL || form == FORM_WATER_SCREEN || form == FORM_WIND_WALL || form == FORM_EARTH_WALL
            ? (this.random.nextDouble() - 0.5) * this.entityData.get(WIDTH)
            : this.random.nextDouble() * radius;
         double x = form == FORM_FIRE_WALL || form == FORM_WATER_SCREEN || form == FORM_WIND_WALL || form == FORM_EARTH_WALL
            ? this.getX() + r
            : this.getX() + Math.cos(angle) * r;
         double z = form == FORM_FIRE_WALL || form == FORM_WATER_SCREEN || form == FORM_WIND_WALL || form == FORM_EARTH_WALL
            ? this.getZ() + (this.random.nextDouble() - 0.5)
            : this.getZ() + Math.sin(angle) * r;
         double y = this.getY() + this.random.nextDouble() * (form == FORM_WIND_TORNADO ? 5.0 : 2.0);
         this.level().addParticle(particle, x, y, z, 0.0, 0.04, 0.0);
      }
   }

   private record BlockSnapshot(BlockPos pos, BlockState state) {
   }
}
