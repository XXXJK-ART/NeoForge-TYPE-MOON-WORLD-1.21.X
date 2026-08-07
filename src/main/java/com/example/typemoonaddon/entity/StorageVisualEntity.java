package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.registry.AddonEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * A transient client-rendered storage prism used while a creature is transferred.
 * It has no collision, AI, gameplay state, or persistence beyond the visual window.
 */
public final class StorageVisualEntity extends Entity {
    public static final int FORMING = 0;
    public static final int COLLAPSING = 1;

    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(StorageVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION =
            SynchedEntityData.defineId(StorageVisualEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> TARGET_ID =
            SynchedEntityData.defineId(StorageVisualEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public StorageVisualEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public StorageVisualEntity(Level level, Vec3 position, int durationTicks, UUID targetId) {
        this(AddonEntities.STORAGE_VISUAL.get(), level);
        setPos(position.x, position.y, position.z);
        setDuration(durationTicks);
        setTargetId(targetId);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, FORMING);
        builder.define(DURATION, 60);
        builder.define(TARGET_ID, Optional.empty());
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    public boolean isCollapsing() {
        return phase() == COLLAPSING;
    }

    public int duration() {
        return entityData.get(DURATION);
    }

    public Optional<UUID> targetId() {
        return entityData.get(TARGET_ID);
    }

    public void setTargetId(UUID targetId) {
        entityData.set(TARGET_ID, Optional.ofNullable(targetId));
    }

    public void startCollapse(int durationTicks) {
        entityData.set(PHASE, COLLAPSING);
        setDuration(durationTicks);
        tickCount = 0;
    }

    /** Matches the reference cube's twelve-tick formation animation. */
    public float formationScale(float partialTick) {
        return Mth.clamp((tickCount + partialTick) / 12.0F, 0.0F, 1.0F);
    }

    /** Matches the reference renderer's 4.5 degree-per-tick spin. */
    public float rotationDegrees(float partialTick) {
        return Mth.wrapDegrees((tickCount + partialTick) * 4.5F);
    }

    /** Shrinks the cube during the eleven-tick collapse window. */
    public float collapseScale(float partialTick) {
        return 1.0F - Mth.clamp((tickCount + partialTick) / (float)Math.max(1, duration()), 0.0F, 1.0F);
    }

    private void setDuration(int durationTicks) {
        entityData.set(DURATION, Mth.clamp(durationTicks, 1, 200));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > duration()) {
            discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(PHASE, tag.getInt("Phase"));
        setDuration(tag.getInt("Duration"));
        setTargetId(tag.hasUUID("Target") ? tag.getUUID("Target") : null);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Phase", phase());
        tag.putInt("Duration", duration());
        targetId().ifPresent(target -> tag.putUUID("Target", target));
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
