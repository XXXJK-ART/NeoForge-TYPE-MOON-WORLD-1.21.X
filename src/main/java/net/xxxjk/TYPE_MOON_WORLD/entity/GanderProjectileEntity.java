package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

@SuppressWarnings("null")
public class GanderProjectileEntity extends ThrowableItemProjectile {
    private static final float BASE_HIT_DAMAGE = 1.5F;
    private static final int MAX_CHARGE_SECONDS = 5;
    private static final int BREAK_BLOCK_COUNT = 5;
    private static final int PROJECTILE_MAX_LIFETIME_TICKS = 160; // 8 seconds
    private static final int PREVIEW_MAX_LIFETIME_TICKS = 600;    // 30 seconds safety cap
    private static final double PROJECTILE_MAX_OWNER_DISTANCE_SQR = 192.0D * 192.0D;
    private static final double PREVIEW_MAX_ANCHOR_DISTANCE_SQR = 16.0D * 16.0D;
    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.0F);
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.12F), 1.1F);
    private static final EntityDataAccessor<Boolean> CHARGING_PREVIEW =
            SynchedEntityData.defineId(GanderProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> VISUAL_SCALE =
            SynchedEntityData.defineId(GanderProjectileEntity.class, EntityDataSerializers.FLOAT);
    private int chargeSeconds = 1;

    public GanderProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public GanderProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.GANDER_PROJECTILE.get(), shooter, level);
    }

    public GanderProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.GANDER_PROJECTILE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GANDER.get();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING_PREVIEW, false);
        builder.define(VISUAL_SCALE, 1.0F);
    }

    public void setChargeSeconds(int value) {
        this.chargeSeconds = Math.max(1, Math.min(MAX_CHARGE_SECONDS, value));
    }

    public void setChargingPreview(boolean chargingPreview) {
        this.entityData.set(CHARGING_PREVIEW, chargingPreview);
    }

    public boolean isChargingPreview() {
        return this.entityData.get(CHARGING_PREVIEW);
    }

    public void setVisualScale(float scale) {
        this.entityData.set(VISUAL_SCALE, Math.max(0.01F, scale));
    }

    public float getVisualScale() {
        return this.entityData.get(VISUAL_SCALE);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && shouldDiscardForCleanup()) {
            this.discard();
            return;
        }
        if (this.isChargingPreview()) {
            this.setNoGravity(true);
            this.noPhysics = true;
            this.setDeltaMovement(Vec3.ZERO);
        }
        super.tick();
        if (this.isChargingPreview() && this.getOwner() instanceof LivingEntity owner) {
            Vec3 anchor = MagicGander.getChargeAnchor(owner);
            this.setPos(anchor);
            // Prevent interpolation / net lerp drift while the player rotates quickly.
            this.xo = anchor.x;
            this.yo = anchor.y;
            this.zo = anchor.z;
        } else if (!this.isChargingPreview()) {
            spawnTrailParticles();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (this.isChargingPreview()) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    public boolean isOwnedBy(Entity entity) {
        return entity != null && this.ownedBy(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.isChargingPreview()) {
            return;
        }
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        if (EntityUtils.isImmunePlayerTarget(target)) {
            this.discard();
            return;
        }

        if (target instanceof LivingEntity livingTarget) {
            int amplifier = Math.max(0, this.chargeSeconds - 1);
            int duration = 80 + (this.chargeSeconds * 40);
            float curseDamage = 2.0F + this.chargeSeconds;

            // Add a small physical hit before curse damage so the spell is not "curse-only".
            livingTarget.hurt(this.damageSources().thrown(this, this.getOwner()), BASE_HIT_DAMAGE);
            livingTarget.hurt(this.damageSources().magic(), curseDamage);
            livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier, false, true, true));
            livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amplifier, false, true, true));
            livingTarget.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.max(40, duration / 2), amplifier, false, true, true));
        }
        spawnImpactParticles(result.getLocation());
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.isChargingPreview()) {
            return;
        }
        if (this.isRemoved()) {
            return;
        }
        if (this.level().isClientSide) {
            return;
        }
        if (result instanceof BlockHitResult blockHitResult && canBreakImpactBlocks()) {
            breakBlocksFromImpact(blockHitResult);
        }
        spawnImpactParticles(result.getLocation());
        this.discard();
    }

    private void spawnTrailParticles() {
        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();
        if (speed < 1.0E-4D) {
            return;
        }
        Vec3 back = motion.normalize().scale(-0.15D);
        Vec3 base = this.position().add(back);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(BLACK_DUST, base.x, base.y, base.z, 2, 0.04D, 0.04D, 0.04D, 0.0D);
            serverLevel.sendParticles(RED_DUST, base.x, base.y, base.z, 2, 0.03D, 0.03D, 0.03D, 0.0D);
            return;
        }
        this.level().addParticle(BLACK_DUST, base.x, base.y, base.z, 0.0D, 0.0D, 0.0D);
        this.level().addParticle(RED_DUST, base.x, base.y, base.z, 0.0D, 0.0D, 0.0D);
    }

    private void spawnImpactParticles(Vec3 impactPos) {
        double x = impactPos.x;
        double y = impactPos.y;
        double z = impactPos.z;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(BLACK_DUST, x, y, z, 14, 0.22D, 0.16D, 0.22D, 0.0D);
            serverLevel.sendParticles(RED_DUST, x, y, z, 10, 0.18D, 0.14D, 0.18D, 0.0D);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, x, y, z, 8, 0.16D, 0.12D, 0.16D, 0.01D);
            return;
        }
        for (int i = 0; i < 12; i++) {
            this.level().addParticle(BLACK_DUST,
                    x + ((this.random.nextDouble() - 0.5D) * 0.32D),
                    y + ((this.random.nextDouble() - 0.5D) * 0.24D),
                    z + ((this.random.nextDouble() - 0.5D) * 0.32D),
                    0.0D, 0.01D, 0.0D);
        }
        for (int i = 0; i < 8; i++) {
            this.level().addParticle(RED_DUST,
                    x + ((this.random.nextDouble() - 0.5D) * 0.24D),
                    y + ((this.random.nextDouble() - 0.5D) * 0.2D),
                    z + ((this.random.nextDouble() - 0.5D) * 0.24D),
                    0.0D, 0.01D, 0.0D);
        }
    }

    private boolean canBreakImpactBlocks() {
        // Terrain break is enabled only for fully charged (5s) Gander shots.
        return this.chargeSeconds >= MAX_CHARGE_SECONDS;
    }

    private void breakBlocksFromImpact(BlockHitResult hitResult) {
        Vec3 motion = this.getDeltaMovement();
        Direction forward = motion.lengthSqr() > 1.0E-6D
                ? Direction.getNearest(motion.x, motion.y, motion.z)
                : hitResult.getDirection().getOpposite();
        Entity breaker = this.getOwner() != null ? this.getOwner() : this;
        BlockPos start = hitResult.getBlockPos();

        for (int i = 0; i < BREAK_BLOCK_COUNT; i++) {
            BlockPos target = start.relative(forward, i);
            BlockState state = this.level().getBlockState(target);
            if (state.isAir()) {
                continue;
            }
            if (state.getDestroySpeed(this.level(), target) < 0.0F) {
                continue;
            }
            this.level().destroyBlock(target, false, breaker);
        }
    }

    private boolean shouldDiscardForCleanup() {
        Entity owner = this.getOwner();
        if (this.isChargingPreview()) {
            if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive() || livingOwner.isRemoved() || livingOwner.level() != this.level()) {
                return true;
            }
            if (this.tickCount > PREVIEW_MAX_LIFETIME_TICKS) {
                return true;
            }
            Vec3 anchor = MagicGander.getChargeAnchor(livingOwner);
            return this.position().distanceToSqr(anchor) > PREVIEW_MAX_ANCHOR_DISTANCE_SQR;
        }

        if (this.tickCount > PROJECTILE_MAX_LIFETIME_TICKS) {
            return true;
        }
        if (owner != null) {
            if (!owner.isAlive() || owner.isRemoved() || owner.level() != this.level()) {
                return true;
            }
            if (this.distanceToSqr(owner) > PROJECTILE_MAX_OWNER_DISTANCE_SQR) {
                return true;
            }
        }
        if (this.getY() < (this.level().getMinBuildHeight() - 16.0D) || this.getY() > (this.level().getMaxBuildHeight() + 32.0D)) {
            return true;
        }
        return !this.level().getWorldBorder().isWithinBounds(this.blockPosition());
    }
}
