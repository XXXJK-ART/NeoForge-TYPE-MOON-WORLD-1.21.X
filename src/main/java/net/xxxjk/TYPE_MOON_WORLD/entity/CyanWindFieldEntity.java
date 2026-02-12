package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import java.util.UUID;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public class CyanWindFieldEntity extends Entity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(CyanWindFieldEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
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
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.duration = tag.getInt("Duration");
        this.setRadius(tag.getFloat("Radius"));
        if (tag.hasUUID("Owner")) {
            this.ownerUUID = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Duration", this.duration);
        tag.putFloat("Radius", this.getRadius());
        if (this.ownerUUID != null) {
            tag.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    public void tick() {
        super.tick();
        float currentRadius = getRadius();
        
        if (this.level().isClientSide) {
             // Particles
             for (int i = 0; i < 5; i++) { // Increased particle count
                 double r = currentRadius * Math.sqrt(this.random.nextDouble()); // Uniform distribution in circle
                 double theta = this.random.nextDouble() * 2 * Math.PI;
                 double d0 = this.getX() + r * Math.cos(theta);
                 
                 // Height scales with radius (e.g., 0.8 * radius)
                 double heightScale = currentRadius * 0.8;
                 double d1 = this.getY() + (this.random.nextDouble() * heightScale); 
                 double d2 = this.getZ() + r * Math.sin(theta);
                 
                 // Rising cloud/smoke
                 // Use POOF or SQUID_INK for variety, or just CLOUD with upward velocity
                 // To simulate rising air: positive Y velocity
                 this.level().addParticle(ParticleTypes.CLOUD, d0, d1, d2, 0, 0.2 + (this.random.nextDouble() * 0.1), 0);
                 
                 // Add occasional "Gust" or "Sweep" to show force
                 if (this.random.nextFloat() < 0.3f) {
                     this.level().addParticle(ParticleTypes.SWEEP_ATTACK, d0, d1, d2, 0, 0.1, 0);
                 }
                 
                 // Add "Poof" for rising smoke effect
                 if (this.random.nextFloat() < 0.1f) {
                      this.level().addParticle(ParticleTypes.POOF, d0, d1, d2, 0, 0.3, 0);
                 }
             }
        } else {
            if (this.tickCount >= this.duration) {
                this.discard();
                return;
            }

            List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(currentRadius, currentRadius, currentRadius));
            LivingEntity ownerEntity = getOwner();
            
            for (LivingEntity entity : list) {
                // Cylindrical check
                double dx = entity.getX() - this.getX();
                double dz = entity.getZ() - this.getZ();
                if (dx*dx + dz*dz > currentRadius * currentRadius) continue;

                // Center radius scales with total radius (50% of total radius)
                double centerRadius = currentRadius * 0.5;
                boolean isCenter = (dx*dx + dz*dz) < centerRadius * centerRadius;
                
                // Reset fall distance to prevent damage
                entity.fallDistance = 0;

                if (entity == ownerEntity) {
                    // Player/Owner: Lift only
                    Vec3 motion = entity.getDeltaMovement();
                    if (motion.y < 0.5) {
                        entity.setDeltaMovement(motion.x, Math.min(motion.y + 0.1, 0.5), motion.z);
                        entity.hurtMarked = true;
                    }
                } else {
                    // Damage non-owner entities (1 damage every 10 ticks = 0.5 hearts/0.5s)
                    if (this.tickCount % 10 == 0) {
                        entity.hurt(this.damageSources().magic(), 1.0f);
                    }

                    if (isCenter) {
                        // Center: Lift high
                        Vec3 motion = entity.getDeltaMovement();
                        if (motion.y < 0.8) {
                            entity.setDeltaMovement(motion.x, Math.min(motion.y + 0.15, 0.8), motion.z);
                            entity.hurtMarked = true;
                        }
                    } else {
                        // Perimeter: Push away
                        Vec3 dir = new Vec3(dx, 0, dz).normalize();
                        if (dir.lengthSqr() < 0.001) {
                             dir = new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize();
                        }
                        
                        Vec3 motion = entity.getDeltaMovement();
                        // Push force
                        double force = 0.3;
                        entity.setDeltaMovement(motion.x + dir.x * force, motion.y + 0.1, motion.z + dir.z * force);
                        entity.hurtMarked = true;
                    }
                }
            }
        }
    }
}
