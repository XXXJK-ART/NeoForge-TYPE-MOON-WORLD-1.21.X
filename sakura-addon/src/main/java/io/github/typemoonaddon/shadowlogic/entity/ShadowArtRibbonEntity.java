package io.github.typemoonaddon.shadowlogic.entity;

import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.shadowlogic.magic.ShadowArtService;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Independently damageable server-authoritative tip and state anchor for one ribbon. */
public final class ShadowArtRibbonEntity extends Entity {
    public static final byte IDLE = 0;
    public static final byte ATTACK = 1;
    public static final byte PIERCED = 2;
    public static final byte DEFEND = 3;
    public static final byte RETRACT = 4;
    public static final byte REGROW = 5;
    public static final byte LIFT = 6;
    public static final byte THROW = 7;
    public static final byte PINNED = 8;
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ShadowArtRibbonEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> OWNER_ENTITY = SynchedEntityData.defineId(ShadowArtRibbonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> INDEX = SynchedEntityData.defineId(ShadowArtRibbonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(ShadowArtRibbonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> ACTION = SynchedEntityData.defineId(ShadowArtRibbonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(ShadowArtRibbonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_START = SynchedEntityData.defineId(ShadowArtRibbonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_START_GAME_TIME = SynchedEntityData.defineId(ShadowArtRibbonEntity.class, EntityDataSerializers.LONG);

    public ShadowArtRibbonEntity(EntityType<? extends ShadowArtRibbonEntity> type, Level level) {
        super(type, level); setNoGravity(true); noPhysics = false;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty()); builder.define(OWNER_ENTITY, -1); builder.define(INDEX, 0); builder.define(HEALTH, GameplayConfig.SHADOW_ART_RIBBON_HEALTH);
        builder.define(ACTION, IDLE); builder.define(TARGET, -1); builder.define(ACTION_START, 0); builder.define(ACTION_START_GAME_TIME, -1L);
    }
    public void initialize(Entity owner, int index) { entityData.set(OWNER, Optional.of(owner.getUUID())); entityData.set(OWNER_ENTITY, owner.getId()); entityData.set(INDEX, Math.clamp(index, 0, 9)); entityData.set(HEALTH, GameplayConfig.SHADOW_ART_RIBBON_HEALTH); }
    @Nullable public UUID ownerId() { return entityData.get(OWNER).orElse(null); }
    public int ownerEntityId() { return entityData.get(OWNER_ENTITY); }
    public void refreshOwnerEntity(Entity owner) {
        if (ownerId() != null && ownerId().equals(owner.getUUID())) entityData.set(OWNER_ENTITY, owner.getId());
    }
    public int ribbonIndex() { return entityData.get(INDEX); }
    public float ribbonHealth() { return entityData.get(HEALTH); }
    public byte action() { return entityData.get(ACTION); }
    public int actionStart() { return entityData.get(ACTION_START); }
    public long actionStartGameTime() { return entityData.get(ACTION_START_GAME_TIME); }
    public int targetEntityId() { return entityData.get(TARGET); }
    public void setAction(byte action, @Nullable Entity target) {
        if (entityData.get(ACTION) != action || entityData.get(TARGET) != (target == null ? -1 : target.getId())) {
            entityData.set(ACTION, action); entityData.set(TARGET, target == null ? -1 : target.getId()); entityData.set(ACTION_START, tickCount);
            entityData.set(ACTION_START_GAME_TIME, level().getGameTime());
        }
    }
    @Override public void tick() { super.tick(); if (!level().isClientSide()) ShadowArtService.tickRibbon(this); }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || amount <= 0 || source.getEntity() != null && ownerId() != null && ownerId().equals(source.getEntity().getUUID())) return false;
        float next = Math.max(0, ribbonHealth() - amount); entityData.set(HEALTH, next);
        if (next <= 0) ShadowArtService.ribbonBroken(this);
        else if (!ShadowArtService.isHoldingAction(action())) setAction(DEFEND, source.getDirectEntity());
        return true;
    }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isAttackable() { return true; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(OWNER_ENTITY, -1);
        entityData.set(INDEX, Math.clamp(tag.getInt("Index"), 0, 9)); entityData.set(HEALTH, Math.clamp(tag.getFloat("RibbonHealth"), 0, GameplayConfig.SHADOW_ART_RIBBON_HEALTH));
        entityData.set(ACTION, tag.getByte("Action"));
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        UUID owner = ownerId(); if (owner != null) tag.putUUID("Owner", owner); tag.putInt("Index", ribbonIndex()); tag.putFloat("RibbonHealth", ribbonHealth()); tag.putByte("Action", action());
    }
}
