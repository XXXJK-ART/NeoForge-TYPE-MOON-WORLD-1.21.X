package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

import java.util.List;

public class MuramasaSlashProjectileEntity extends Projectile {
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(MuramasaSlashProjectileEntity.class, EntityDataSerializers.INT);

    private int maxAge = 100;

    public MuramasaSlashProjectileEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
    }

    public MuramasaSlashProjectileEntity(Level level, LivingEntity shooter, int charge) {
        super(ModEntities.MURAMASA_SLASH.get(), level);
        this.setOwner(shooter);
        this.setCharge(charge);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.5, shooter.getZ());
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 1.5F, 0.0F); // Speed 1.5
        
        // Lifetime scales with charge? Or fixed range?
        // "Cuts the mountain" -> Needs to travel far.
        this.maxAge = 100 + charge * 2; // 100 to 300 ticks (5-15 seconds)
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGE, 0);
    }

    public void setCharge(int charge) {
        this.entityData.set(CHARGE, charge);
    }

    public int getCharge() {
        return this.entityData.get(CHARGE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Visuals
            int charge = getCharge();
            double size = 1.0 + (charge / 20.0);
            
            // Particles along the slash vertical line
            for (int i = 0; i < size * 3; i++) {
                double yOffset = (this.random.nextDouble() - 0.5) * size * 2;
                this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + yOffset, this.getZ(), 0, 0, 0);
                if (charge >= 50) {
                     this.level().addParticle(ParticleTypes.LAVA, this.getX(), this.getY() + yOffset, this.getZ(), 0, 0, 0);
                }
            }
        } else {
            // Server Logic
            if (this.tickCount > maxAge) {
                this.discard();
                return;
            }

            // Move
            Vec3 motion = this.getDeltaMovement();
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            
            // Block Destruction
            processBlockDestruction();
            
            // Entity Damage
            processEntityDamage();
        }
    }

    private void processBlockDestruction() {
        if (this.level().isClientSide) return;

        int charge = getCharge();
        // Height: 2 blocks base + charge. Max 2 + 100 = 102 blocks.
        int height = 2 + charge;
        // Width: 3 blocks base + charge/10. Max 3 + 10 = 13 blocks.
        int width = 3 + (charge / 10);

        BlockPos center = this.blockPosition();
        Vec3 motion = this.getDeltaMovement().normalize();
        
        // Calculate perpendicular vector for width
        Vec3 right = new Vec3(-motion.z, 0, motion.x).normalize();
        
        for (int h = -1; h < height; h++) {
            for (int w = -width/2; w <= width/2; w++) {
                BlockPos target = center.offset(
                    (int)(right.x * w),
                    h,
                    (int)(right.z * w)
                );
                
                BlockState state = this.level().getBlockState(target);
                if (!state.isAir() && state.getDestroySpeed(this.level(), target) >= 0 && state.getDestroySpeed(this.level(), target) < 50.0f) { // Don't break bedrock/obsidian? Maybe allow obsidian at high charge.
                    // Destroy
                    this.level().destroyBlock(target, true);
                }
            }
        }
    }

    private void processEntityDamage() {
        int charge = getCharge();
        // Damage: Base 10 + charge. Max 110.
        float damage = 10.0f + charge;
        double radius = 1.5 + (charge / 10.0);
        double height = 2.0 + charge;
        
        AABB box = this.getBoundingBox().inflate(radius, height, radius);
        List<Entity> entities = this.level().getEntities(this, box, e -> e instanceof LivingEntity && e != this.getOwner());
        
        for (Entity e : entities) {
            e.hurt(this.damageSources().mobProjectile(this, (LivingEntity)this.getOwner()), damage);
            e.igniteForSeconds(5);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Charge")) {
            this.setCharge(tag.getInt("Charge"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Charge", this.getCharge());
    }
}
