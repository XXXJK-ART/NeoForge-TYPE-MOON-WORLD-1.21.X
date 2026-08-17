package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.magic.SakuraShadowArtService;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class SakuraShadowArtRibbonEntity extends Entity {
    public static final byte IDLE = 0;
    public static final byte ATTACK = 1;
    public static final byte DEFEND = 2;
    public static final byte PIERCED = 3;
    public static final byte LIFT = 4;
    public static final byte THROW = 5;
    public static final byte PINNED = 6;
    public static final byte RETRACT = 7;
    public static final byte REGROW = 8;
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(
            SakuraShadowArtRibbonEntity.class,
            EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Integer> OWNER_ENTITY = SynchedEntityData.defineId(
            SakuraShadowArtRibbonEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> INDEX = SynchedEntityData.defineId(
            SakuraShadowArtRibbonEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(
            SakuraShadowArtRibbonEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Byte> ACTION = SynchedEntityData.defineId(
            SakuraShadowArtRibbonEntity.class,
            EntityDataSerializers.BYTE
    );
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(
            SakuraShadowArtRibbonEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> ACTION_START = SynchedEntityData.defineId(
            SakuraShadowArtRibbonEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Long> ACTION_START_GAME_TIME = SynchedEntityData.defineId(
            SakuraShadowArtRibbonEntity.class,
            EntityDataSerializers.LONG
    );

    public SakuraShadowArtRibbonEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            SakuraShadowArtService.tickRibbon(this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || amount <= 0.0F || isRemoved()) {
            return false;
        }
        float nextHealth = Math.max(0.0F, ribbonHealth() - amount);
        entityData.set(HEALTH, nextHealth);
        if (nextHealth <= 0.0F) {
            SakuraShadowArtService.ribbonBroken(this);
        }
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
        builder.define(OWNER_ENTITY, -1);
        builder.define(INDEX, 0);
        builder.define(HEALTH, GameplayConfig.SHADOW_ART_RIBBON_HEALTH);
        builder.define(ACTION, IDLE);
        builder.define(TARGET, -1);
        builder.define(ACTION_START, 0);
        builder.define(ACTION_START_GAME_TIME, -1L);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(OWNER_ENTITY, tag.getInt("OwnerEntityId"));
        entityData.set(INDEX, Math.clamp(tag.getInt("RibbonIndex"), 0, GameplayConfig.SHADOW_ART_RIBBON_COUNT - 1));
        entityData.set(ACTION, tag.getByte("Action"));
        entityData.set(TARGET, tag.getInt("TargetEntityId"));
        entityData.set(ACTION_START, tag.getInt("ActionStart"));
        entityData.set(ACTION_START_GAME_TIME, tag.getLong("ActionStartGameTime"));
        entityData.set(HEALTH, tag.contains("ShellHealth") ? tag.getFloat("ShellHealth") : GameplayConfig.SHADOW_ART_RIBBON_HEALTH);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID ownerId = ownerId();
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("OwnerEntityId", ownerEntityId());
        tag.putInt("RibbonIndex", ribbonIndex());
        tag.putByte("Action", action());
        tag.putInt("TargetEntityId", targetEntityId());
        tag.putInt("ActionStart", actionStart());
        tag.putLong("ActionStartGameTime", actionStartGameTime());
        tag.putFloat("ShellHealth", ribbonHealth());
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public void initialize(LivingEntity owner, int ribbonIndex) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        entityData.set(OWNER_ENTITY, owner.getId());
        entityData.set(INDEX, Math.clamp(ribbonIndex, 0, GameplayConfig.SHADOW_ART_RIBBON_COUNT - 1));
        entityData.set(HEALTH, GameplayConfig.SHADOW_ART_RIBBON_HEALTH);
    }

    public void setAction(byte action, @Nullable Entity target) {
        int nextTarget = target == null ? -1 : target.getId();
        if (entityData.get(ACTION) == action && entityData.get(TARGET) == nextTarget) {
            return;
        }
        entityData.set(ACTION, action);
        entityData.set(TARGET, nextTarget);
        entityData.set(ACTION_START, tickCount);
        entityData.set(ACTION_START_GAME_TIME, level().getGameTime());
    }

    @Nullable
    public UUID ownerId() {
        return entityData.get(OWNER).orElse(null);
    }

    public int ownerEntityId() {
        return entityData.get(OWNER_ENTITY);
    }

    public void refreshOwnerEntity(Entity owner) {
        UUID ownerId = ownerId();
        if (ownerId != null && ownerId.equals(owner.getUUID())) {
            entityData.set(OWNER_ENTITY, owner.getId());
        }
    }

    public int ribbonIndex() {
        return entityData.get(INDEX);
    }

    public float ribbonHealth() {
        return entityData.get(HEALTH);
    }

    public byte action() {
        return entityData.get(ACTION);
    }

    public int targetEntityId() {
        return entityData.get(TARGET);
    }

    public int actionStart() {
        return entityData.get(ACTION_START);
    }

    public long actionStartGameTime() {
        return entityData.get(ACTION_START_GAME_TIME);
    }
}
