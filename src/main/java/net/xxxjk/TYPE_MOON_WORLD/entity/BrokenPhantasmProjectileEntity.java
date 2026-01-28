package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

import java.util.ArrayList;
import java.util.List;

public class BrokenPhantasmProjectileEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Float> EXPLOSION_POWER = SynchedEntityData.defineId(BrokenPhantasmProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_EXPLODING = SynchedEntityData.defineId(BrokenPhantasmProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    
    private int lifeTime = 0;
    private int explosionTick = 0;
    private double currentRadius = 0;
    private BlockPos explosionCenter;
    private float maxRadius;
    private List<Entity> damagedEntities = new ArrayList<>();

    public BrokenPhantasmProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public BrokenPhantasmProjectileEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(ModEntities.BROKEN_PHANTASM_PROJECTILE.get(), shooter, level);
        this.setItem(stack);
    }

    public BrokenPhantasmProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.BROKEN_PHANTASM_PROJECTILE.get(), x, y, z, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EXPLOSION_POWER, 2.0f);
        builder.define(IS_EXPLODING, false);
    }

    public void setExplosionPower(float power) {
        this.entityData.set(EXPLOSION_POWER, power);
    }

    public float getExplosionPower() {
        return this.entityData.get(EXPLOSION_POWER);
    }

    public boolean isExploding() {
        return this.entityData.get(IS_EXPLODING);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_SWORD; // Fallback
    }

    @Override
    public void tick() {
        if (isExploding()) {
            if (!this.level().isClientSide) {
                processExplosion();
            } else {
                // Client side effects during explosion
                spawnExplosionParticles();
            }
        } else {
            super.tick();
            if (this.level().isClientSide) {
                // Particles while flying
                this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            } else {
                lifeTime++;
                if (lifeTime >= 200) { // 10 seconds (20 ticks * 10)
                    startExplosion();
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        // Stop movement logic
        if (!isExploding()) {
             // super.onHit(result); // Don't call super onHit as it might discard entity
             if (!this.level().isClientSide) {
                 startExplosion();
             }
        }
    }
    
    // Override to prevent despawn on impact if we want to keep it alive for explosion
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    private void startExplosion() {
        if (isExploding()) return;
        this.entityData.set(IS_EXPLODING, true);
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.explosionCenter = this.blockPosition();
        this.maxRadius = getExplosionPower(); // Radius = Power
        this.currentRadius = 0;
        this.explosionTick = 0;
        
        // Initial big sound
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F);

        // Instant damage for small explosions (Power <= 5)
        if (this.maxRadius <= 5) {
             // Increase range for small explosions: Base 2.0 + Radius * 1.5
             double damageRadius = 2.0 + this.maxRadius * 1.5;
             AABB damageBox = new AABB(explosionCenter).inflate(damageRadius);
             List<Entity> entities = this.level().getEntities(this, damageBox);
             DamageSource damageSource = this.damageSources().explosion(this, this.getOwner());
             
             for (Entity e : entities) {
                 if (e instanceof LivingEntity) {
                     // Increase damage for small explosions: Base 5 + Power * 3
                     float damage = 5.0f + getExplosionPower() * 3.0f;
                     e.hurt(damageSource, damage);
                     damagedEntities.add(e);
                 }
             }
        }
    }

    private void processExplosion() {
        if (currentRadius > maxRadius * 1.2) { // Allow effect/damage to go slightly beyond block break radius
            this.discard();
            return;
        }

        double step = Math.max(0.5, maxRadius / 20.0); // Expansion speed
        double nextRadius = currentRadius + step;
        
        // 1. Remove blocks (Layer by layer)
        // Only break blocks up to maxRadius
        if (maxRadius > 0 && currentRadius < maxRadius) {
            int rInt = (int)Math.ceil(nextRadius);
            // Optimization: limit rInt to maxRadius
            if (rInt > maxRadius) rInt = (int)Math.ceil(maxRadius);
            
            for (int x = -rInt; x <= rInt; x++) {
                for (int y = -rInt; y <= rInt; y++) {
                    for (int z = -rInt; z <= rInt; z++) {
                        double distSqr = x*x + y*y + z*z;
                        // Check if block is in the current shell (between currentRadius and nextRadius)
                        // And also ensure it is within maxRadius
                        if (distSqr <= nextRadius*nextRadius && distSqr > currentRadius*currentRadius && distSqr <= maxRadius*maxRadius) {
                            BlockPos pos = explosionCenter.offset(x, y, z);
                            BlockState state = this.level().getBlockState(pos);
                            if (!state.isAir() && state.getBlock().getExplosionResistance() < 1200) {
                                this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                                // Chance to spawn particles at block pos
                                if (this.level().random.nextInt(10) == 0) {
                                    ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, pos.getX(), pos.getY(), pos.getZ(), 1, 0.5, 0.5, 0.5, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 2. Damage Entities (Slightly larger radius than block breaking)
        // Damage radius is faster/larger
        // Only apply expanding wave damage if power > 5
        if (getExplosionPower() > 5) {
            double damageRadius = nextRadius * 1.2; 
            AABB damageBox = new AABB(explosionCenter).inflate(damageRadius);
            List<Entity> entities = this.level().getEntities(this, damageBox);
            DamageSource damageSource = this.damageSources().explosion(this, this.getOwner());
            
            for (Entity e : entities) {
                if (!damagedEntities.contains(e)) {
                    if (e instanceof LivingEntity) {
                        double dist = e.distanceToSqr(this.getX(), this.getY(), this.getZ());
                        // Only damage if the "wave" has reached them
                        if (dist <= damageRadius * damageRadius) {
                            float damage = getExplosionPower() * 2.0f; 
                            e.hurt(damageSource, damage);
                            damagedEntities.add(e);
                        }
                    }
                }
            }
        }
        
        // 3. Effects (Synced with expansion)
        if (explosionTick % 5 == 0) {
             this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, (1.0F + (this.level().random.nextFloat() - this.level().random.nextFloat()) * 0.2F) * 0.7F);
        }
        
        // Spawn more particles at the edge of the wave
        if (this.level() instanceof ServerLevel serverLevel) {
            int particleCount = (int)(nextRadius * 2); // More particles as radius grows
            for (int i = 0; i < particleCount; i++) {
                double theta = this.level().random.nextDouble() * Math.PI * 2;
                double phi = this.level().random.nextDouble() * Math.PI;
                // Place particles on the surface of the current sphere
                double x = nextRadius * Math.sin(phi) * Math.cos(theta);
                double y = nextRadius * Math.sin(phi) * Math.sin(theta);
                double z = nextRadius * Math.cos(phi);
                
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 
                    this.getX() + x, this.getY() + y, this.getZ() + z, 
                    1, 0, 0, 0, 0);
            }
        }

        currentRadius = nextRadius;
        explosionTick++;
    }

    private void spawnExplosionParticles() {
        if (this.level().random.nextInt(3) == 0) {
            double r = currentRadius;
            double theta = this.level().random.nextDouble() * Math.PI * 2;
            double phi = this.level().random.nextDouble() * Math.PI;
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.sin(phi) * Math.sin(theta);
            double z = r * Math.cos(phi);
            this.level().addParticle(ParticleTypes.EXPLOSION_EMITTER, 
                this.getX() + x, this.getY() + y, this.getZ() + z, 
                0, 0, 0);
        }
    }
}
