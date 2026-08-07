package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.registry.AddonEntities;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative moving visual used by both Airflow Blade modes. */
public final class AirflowBladeEntity extends Entity {
    public static final int BLADE_MODE = 0;
    public static final int CANNON_MODE = 1;

    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(AirflowBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CHARGE =
            SynchedEntityData.defineId(AirflowBladeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(AirflowBladeEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private Vec3 direction = Vec3.ZERO;
    private double speed;
    private double maxDistance;
    private double travelled;
    private float damage;
    private double explosionRadius;

    public AirflowBladeEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public AirflowBladeEntity(Level level) {
        this(AddonEntities.AIRFLOW_BLADE.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MODE, BLADE_MODE);
        builder.define(CHARGE, 0.0F);
        builder.define(OWNER_ID, Optional.empty());
    }

    public void configure(
            int mode,
            UUID ownerId,
            Vec3 direction,
            double speed,
            double maxDistance,
            float damage,
            double explosionRadius,
            float charge
    ) {
        entityData.set(MODE, mode == CANNON_MODE ? CANNON_MODE : BLADE_MODE);
        entityData.set(CHARGE, Mth.clamp(charge, 0.0F, 1.0F));
        entityData.set(OWNER_ID, Optional.ofNullable(ownerId));
        this.direction = direction.lengthSqr() > 1.0E-8D ? direction.normalize() : Vec3.ZERO;
        this.speed = Math.max(0.05D, speed);
        this.maxDistance = Math.max(1.0D, maxDistance);
        this.damage = Math.max(0.0F, damage);
        this.explosionRadius = Math.max(0.0D, explosionRadius);
        this.travelled = 0.0D;
        setDeltaMovement(this.direction.scale(this.speed));
        setYRot((float) (Mth.atan2(this.direction.z, this.direction.x) * 180.0D / Math.PI) - 90.0F);
        setXRot((float) (-(Mth.atan2(this.direction.y,
                Math.sqrt(this.direction.x * this.direction.x + this.direction.z * this.direction.z))
                * 180.0D / Math.PI)));
    }

    public int mode() {
        return entityData.get(MODE);
    }

    public float charge() {
        return entityData.get(CHARGE);
    }

    public Optional<UUID> ownerId() {
        return entityData.get(OWNER_ID);
    }

    public Vec3 direction() {
        return direction;
    }

    public double speed() {
        return speed;
    }

    public double maxDistance() {
        return maxDistance;
    }

    public double travelled() {
        return travelled;
    }

    public void addTravelled(double distance) {
        travelled += Math.max(0.0D, distance);
    }

    public float damage() {
        return damage;
    }

    public double explosionRadius() {
        return explosionRadius;
    }

    /** Movement and collision are driven by AirflowBladeService on the server. */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            setDeltaMovement(direction.scale(speed));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(MODE, Mth.clamp(tag.getInt("Mode"), BLADE_MODE, CANNON_MODE));
        entityData.set(CHARGE, Mth.clamp(tag.getFloat("Charge"), 0.0F, 1.0F));
        if (tag.hasUUID("Owner")) {
            entityData.set(OWNER_ID, Optional.of(tag.getUUID("Owner")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Mode", mode());
        tag.putFloat("Charge", charge());
        ownerId().ifPresent(owner -> tag.putUUID("Owner", owner));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}
