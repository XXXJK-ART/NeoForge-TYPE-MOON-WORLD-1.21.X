package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import java.util.UUID;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public class CyanWindFieldEntity extends Entity {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(CyanWindFieldEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> TORNADO_MODE = SynchedEntityData.defineId(CyanWindFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private int duration = 100; // 5 seconds
    private LivingEntity owner;
    private UUID ownerUUID;

    public CyanWindFieldEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public CyanWindFieldEntity(Level level, double x, double y, double z, float radius, int duration, LivingEntity owner) {
        this(ModEntities.CYAN_WIND_FIELD.get(), level);
        this.setPos(x, y, z);
        this.setRadius(radius);
        this.duration = duration;
        this.setOwner(owner);
    }

    public void setRadius(float radius) {
        this.entityData.set(RADIUS, radius);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }
    
    public void setTornadoMode(boolean tornado) {
        this.entityData.set(TORNADO_MODE, tornado);
    }
    
    public boolean isTornadoMode() {
        return this.entityData.get(TORNADO_MODE);
    }
    
    public void setOwner(LivingEntity owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }

    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity)entity;
            }
        }
        return this.owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 4.0f);
        builder.define(TORNADO_MODE, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.duration = tag.getInt("Duration");
        this.setRadius(tag.getFloat("Radius"));
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
        if (tag.contains("TornadoMode")) {
            this.entityData.set(TORNADO_MODE, tag.getBoolean("TornadoMode"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Duration", this.duration);
        tag.putFloat("Radius", this.getRadius());
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
        tag.putBoolean("TornadoMode", this.isTornadoMode());
    }

    @Override
    public void tick() {
        super.tick();
        float currentRadius = getRadius();
        
        if (this.level().isClientSide) {
             boolean tornado = this.isTornadoMode();
             if (tornado) {
                 double height = currentRadius * 1.2;
                 int arms = 3;
                 int segments = 24;
                 for (int arm = 0; arm < arms; arm++) {
                     double armPhase = (2.0 * Math.PI * arm) / arms;
                     for (int i = 0; i < segments; i++) {
                         double t = (double)i / (double)(segments - 1);
                         double baseRadius = 0.2 + 0.6 * t + 0.15 * t * t;
                         double wobble = 0.05 * Math.sin(t * 6.0 * Math.PI + this.tickCount * 0.1 + armPhase);
                         double radius = currentRadius * Math.max(0.1, baseRadius + wobble);
                         double angle = this.tickCount * 0.3 + armPhase + t * 6.0 * Math.PI;
                         double x = this.getX() + radius * Math.cos(angle);
                         double z = this.getZ() + radius * Math.sin(angle);
                         double y = this.getY() + t * height;
                         double vx = -Math.sin(angle) * 0.25;
                         double vz = Math.cos(angle) * 0.25;
                         double vy = 0.16 + this.random.nextDouble() * 0.06;
                         this.level().addParticle(ParticleTypes.CLOUD, x, y, z, vx, vy, vz);
                         if (this.random.nextFloat() < 0.3f) {
                             this.level().addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, vx * 0.5, vy * 0.5, vz * 0.5);
                         }
                     }
                 }
                 int wisps = 14;
                 for (int i = 0; i < wisps; i++) {
                     double t = this.random.nextDouble();
                     double baseRadius = 0.25 + 0.5 * t + 0.2 * t * t;
                     double wobble = 0.07 * Math.sin(t * 5.0 * Math.PI + this.tickCount * 0.12 + i);
                     double radius = currentRadius * Math.max(0.1, baseRadius + wobble);
                     double angle = this.tickCount * 0.35 + this.random.nextDouble() * 2.0 * Math.PI;
                     double x = this.getX() + radius * Math.cos(angle);
                     double z = this.getZ() + radius * Math.sin(angle);
                     double y = this.getY() + t * height;
                     double vx = -Math.sin(angle) * 0.22;
                     double vz = Math.cos(angle) * 0.22;
                     double vy = 0.18 + this.random.nextDouble() * 0.05;
                     this.level().addParticle(ParticleTypes.POOF, x, y, z, vx, vy, vz);
                 }
             } else {
                 for (int i = 0; i < 5; i++) {
                     double r = currentRadius * Math.sqrt(this.random.nextDouble());
                     double theta = this.random.nextDouble() * 2 * Math.PI;
                     double d0 = this.getX() + r * Math.cos(theta);
                     double heightScale = currentRadius * 0.8;
                     double d1 = this.getY() + (this.random.nextDouble() * heightScale);
                     double d2 = this.getZ() + r * Math.sin(theta);
                     this.level().addParticle(ParticleTypes.CLOUD, d0, d1, d2, 0, 0.2 + (this.random.nextDouble() * 0.1), 0);
                     if (this.random.nextFloat() < 0.3f) {
                         this.level().addParticle(ParticleTypes.SWEEP_ATTACK, d0, d1, d2, 0, 0.1, 0);
                     }
                     if (this.random.nextFloat() < 0.1f) {
                          this.level().addParticle(ParticleTypes.POOF, d0, d1, d2, 0, 0.3, 0);
                     }
                 }
             }
        } else {
            if (this.tickCount >= this.duration) {
                this.discard();
                return;
            }

            List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(currentRadius, currentRadius, currentRadius));
            LivingEntity ownerEntity = getOwner();
            boolean tornado = this.isTornadoMode();
            
            for (LivingEntity entity : list) {
                if (entity instanceof Player player && player.isCreative()) continue;
                double dx = entity.getX() - this.getX();
                double dz = entity.getZ() - this.getZ();
                if (dx * dx + dz * dz > currentRadius * currentRadius) continue;
                entity.fallDistance = 0;

                if (entity == ownerEntity) {
                    Vec3 motion = entity.getDeltaMovement();
                    if (motion.y < 0.5) {
                        entity.setDeltaMovement(motion.x, Math.min(motion.y + 0.1, 0.5), motion.z);
                        entity.hurtMarked = true;
                    }
                } else {
                    if (tornado) {
                        if (this.tickCount % 10 == 0) {
                            entity.hurt(this.damageSources().magic(), 1.5f);
                        }
                        double distSqr = dx * dx + dz * dz;
                        Vec3 dir;
                        if (distSqr < 0.001) {
                            dir = new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize();
                        } else {
                            double dist = Math.sqrt(distSqr);
                            dir = new Vec3(-dx / dist, 0, -dz / dist);
                        }
                        Vec3 tangent = new Vec3(-dir.z, 0, dir.x);
                        Vec3 motion = entity.getDeltaMovement();
                        double inwardForce = 0.25;
                        double swirlForce = 0.2;
                        double upForce = (dx * dx + dz * dz) < (currentRadius * currentRadius * 0.25) ? 0.2 : 0.1;
                        double vx = motion.x + dir.x * inwardForce + tangent.x * swirlForce;
                        double vz = motion.z + dir.z * inwardForce + tangent.z * swirlForce;
                        double vy = Math.min(motion.y + upForce, 1.0);
                        entity.setDeltaMovement(vx, vy, vz);
                        entity.hurtMarked = true;
                    } else {
                        if (this.tickCount % 10 == 0) {
                            entity.hurt(this.damageSources().magic(), 1.0f);
                        }
                        double centerRadius = currentRadius * 0.5;
                        boolean isCenter = (dx * dx + dz * dz) < centerRadius * centerRadius;
                        if (isCenter) {
                            Vec3 motion = entity.getDeltaMovement();
                            if (motion.y < 0.8) {
                                entity.setDeltaMovement(motion.x, Math.min(motion.y + 0.15, 0.8), motion.z);
                                entity.hurtMarked = true;
                            }
                        } else {
                            Vec3 dir = new Vec3(dx, 0, dz).normalize();
                            if (dir.lengthSqr() < 0.001) {
                                dir = new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize();
                            }
                            Vec3 motion = entity.getDeltaMovement();
                            double force = 0.3;
                            entity.setDeltaMovement(motion.x + dir.x * force, motion.y + 0.1, motion.z + dir.z * force);
                            entity.hurtMarked = true;
                        }
                    }
                }
            }

            List<Projectile> projectileList = this.level().getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(currentRadius, currentRadius, currentRadius));
            for (Projectile projectile : projectileList) {
                if (projectile.isRemoved()) continue;
                double dx = projectile.getX() - this.getX();
                double dz = projectile.getZ() - this.getZ();
                if (dx * dx + dz * dz > currentRadius * currentRadius) continue;

                Vec3 motion = projectile.getDeltaMovement();
                if (tornado) {
                    double distSqr = dx * dx + dz * dz;
                    Vec3 dir;
                    if (distSqr < 0.001) {
                        dir = new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize();
                    } else {
                        double dist = Math.sqrt(distSqr);
                        dir = new Vec3(-dx / dist, 0, -dz / dist);
                    }
                    Vec3 tangent = new Vec3(-dir.z, 0, dir.x);
                    double inwardForce = 0.15;
                    double swirlForce = 0.25;
                    double upForce = 0.05;
                    double vx = motion.x + dir.x * inwardForce + tangent.x * swirlForce;
                    double vz = motion.z + dir.z * inwardForce + tangent.z * swirlForce;
                    double vy = motion.y + upForce;
                    projectile.setDeltaMovement(vx, vy, vz);
                } else {
                    Vec3 dir = new Vec3(dx, 0, dz).normalize();
                    if (dir.lengthSqr() < 0.001) {
                        dir = new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize();
                    }
                    double force = 0.2;
                    double vy = motion.y + 0.02;
                    projectile.setDeltaMovement(motion.x + dir.x * force, vy, motion.z + dir.z * force);
                }
            }
        }
    }
}
