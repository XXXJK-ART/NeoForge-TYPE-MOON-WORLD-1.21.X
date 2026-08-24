package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.magic.BoundaryMagicIntegration;
import com.example.typemoonaddon.registry.AddonEntities;
import java.util.UUID;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persistent, non-block boundary inscription. One entity represents one placed inscription. */
public final class BoundaryMarkEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_TYPE =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Optional<UUID>> DATA_AUTHOR =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_BOUNDARY =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_SIDE =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POWER =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COMPLEXITY =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FACE =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CONTROLLER =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_REQUIRED =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REMAINING =
            SynchedEntityData.defineId(BoundaryMarkEntity.class, EntityDataSerializers.INT);

    private UUID authorId;
    private UUID boundaryId;
    private String boundaryType = "sensing_boundary";
    private int side = 10;
    private int power = 1;
    private int complexity = 10;
    private int requiredMarks = 1;
    private int remainingMarks = 1;
    private boolean occupied;
    private boolean active = true;
    private boolean controller;
    private String blacklist = "none";
    private UUID blacklistUuid;
    private BlockPos anchor = BlockPos.ZERO;
    private Direction face = Direction.UP;
    private float barrierHealth = 1000.0F;

    public BoundaryMarkEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public BoundaryMarkEntity(Level level, UUID authorId, UUID boundaryId, String boundaryType,
                              int side, int power, int complexity, int requiredMarks) {
        this(AddonEntities.BOUNDARY_MARK.get(), level);
        this.authorId = authorId;
        this.boundaryId = boundaryId;
        this.boundaryType = boundaryType;
        this.side = side;
        this.power = power;
        this.complexity = Math.max(1, Math.min(100, complexity));
        this.requiredMarks = Math.max(1, requiredMarks);
        this.remainingMarks = this.requiredMarks;
        setBoundaryType(boundaryType);
        setAuthorId(authorId);
        setBoundaryId(boundaryId);
        setSide(side);
        setPower(power);
        setComplexity(this.complexity);
        setRequiredMarks(this.requiredMarks);
        setRemainingMarks(this.remainingMarks);
    }

    public BoundaryMarkEntity(Level level, UUID authorId, UUID boundaryId, String boundaryType,
                              int side, int power, int requiredMarks) {
        this(level, authorId, boundaryId, boundaryType, side, power, 10, requiredMarks);
    }

    public static BoundaryMarkEntity unbound(Level level, UUID authorId, BlockPos anchor, Direction face) {
        BoundaryMarkEntity mark = new BoundaryMarkEntity(AddonEntities.BOUNDARY_MARK.get(), level);
        mark.authorId = authorId;
        mark.anchor = anchor.immutable();
        mark.face = face == null ? Direction.UP : face;
        mark.setBoundaryType("");
        mark.setAuthorId(authorId);
        mark.setBoundaryId(null);
        mark.setAnchor(anchor, mark.face);
        return mark;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isBound() && controller) {
            BoundaryMagicIntegration.tickMark(this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide()) {
            return true;
        }
        if (!(source.getEntity() instanceof Player)) {
            return false;
        }
        if (!isBound()) {
            active = false;
            discard();
            return true;
        }
        BoundaryMagicIntegration.markDestroyed(this);
        active = false;
        discard();
        return true;
    }

    public UUID getAuthorId() {
        return entityData.get(DATA_AUTHOR).orElse(authorId);
    }

    public UUID getBoundaryId() {
        return entityData.get(DATA_BOUNDARY).orElse(boundaryId);
    }

    public void setAuthorId(UUID value) {
        authorId = value;
        entityData.set(DATA_AUTHOR, Optional.ofNullable(value));
    }

    public void setBoundaryId(UUID value) {
        boundaryId = value;
        entityData.set(DATA_BOUNDARY, Optional.ofNullable(value));
    }

    public String getBoundaryType() {
        return entityData.get(DATA_TYPE);
    }

    public void setBoundaryType(String type) {
        boundaryType = type == null ? "" : type;
        entityData.set(DATA_TYPE, boundaryType);
    }

    public int getSide() {
        return entityData.get(DATA_SIDE);
    }

    public void setSide(int value) {
        side = Math.max(10, Math.min(100, value));
        entityData.set(DATA_SIDE, side);
    }

    public int getPower() {
        return entityData.get(DATA_POWER);
    }

    public void setPower(int value) {
        power = Math.max(1, Math.min(5, value));
        entityData.set(DATA_POWER, power);
    }

    public int getComplexity() {
        return entityData.get(DATA_COMPLEXITY);
    }

    public void setComplexity(int value) {
        complexity = Math.max(1, Math.min(100, value));
        entityData.set(DATA_COMPLEXITY, complexity);
    }

    public int getRequiredMarks() {
        return entityData.get(DATA_REQUIRED);
    }

    public int getRemainingMarks() {
        return entityData.get(DATA_REMAINING);
    }

    public double getRemainingRatio() {
        int required = Math.max(1, getRequiredMarks());
        return Math.max(0.0D, Math.min(1.0D, getRemainingMarks() / (double) required));
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setBlacklist(String blacklist) {
        setBlacklistMode(blacklist);
    }

    public String getBlacklist() {
        return blacklist;
    }

    public void setBlacklistMode(String value) {
        blacklist = value == null || value.isEmpty() ? "none" : value;
    }

    public UUID getBlacklistUuid() {
        return blacklistUuid;
    }

    public void setBlacklistUuid(UUID value) {
        blacklistUuid = value;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isBound() {
        return getBoundaryId() != null && !getBoundaryType().isEmpty();
    }

    public UUID getBoundaryIdOrNull() {
        return getBoundaryId();
    }

    public boolean isController() {
        return entityData.get(DATA_CONTROLLER);
    }

    public void setController(boolean value) {
        controller = value;
        entityData.set(DATA_CONTROLLER, value);
    }

    public void setRequiredMarks(int value) {
        requiredMarks = Math.max(1, value);
        entityData.set(DATA_REQUIRED, requiredMarks);
        if (remainingMarks > requiredMarks) {
            setRemainingMarks(requiredMarks);
        }
    }

    public void setRemainingMarks(int value) {
        remainingMarks = Math.max(0, Math.min(Math.max(1, getRequiredMarks()), value));
        entityData.set(DATA_REMAINING, remainingMarks);
    }

    public BlockPos getAnchor() {
        return anchor;
    }

    public Direction getFace() {
        return Direction.from3DDataValue(entityData.get(DATA_FACE));
    }

    public void setAnchor(BlockPos value, Direction direction) {
        anchor = value == null ? BlockPos.ZERO : value.immutable();
        face = direction == null ? Direction.UP : direction;
        entityData.set(DATA_FACE, face.get3DDataValue());
        Vec3 position = Vec3.atCenterOf(anchor).add(face.getStepX() * 0.505D,
                face.getStepY() * 0.505D, face.getStepZ() * 0.505D);
        setPos(position);
    }

    public void deactivate() {
        active = false;
        discard();
    }

    public float getBarrierHealth() {
        return barrierHealth;
    }

    public void setBarrierHealth(float value) {
        barrierHealth = Math.max(0.0F, Math.min(1000.0F, value));
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_TYPE, "");
        builder.define(DATA_AUTHOR, Optional.empty());
        builder.define(DATA_BOUNDARY, Optional.empty());
        builder.define(DATA_SIDE, 10);
        builder.define(DATA_POWER, 1);
        builder.define(DATA_COMPLEXITY, 10);
        builder.define(DATA_FACE, Direction.UP.get3DDataValue());
        builder.define(DATA_CONTROLLER, false);
        builder.define(DATA_REQUIRED, 1);
        builder.define(DATA_REMAINING, 1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        authorId = tag.hasUUID("Author") ? tag.getUUID("Author") : null;
        boundaryId = tag.hasUUID("Boundary") ? tag.getUUID("Boundary") : null;
        setAuthorId(authorId);
        setBoundaryId(boundaryId);
        boundaryType = tag.getString("Type");
        setBoundaryType(boundaryType);
        side = Math.max(10, Math.min(100, tag.getInt("Side")));
        setSide(side);
        power = Math.max(1, Math.min(5, tag.getInt("Power")));
        setPower(power);
        complexity = Math.max(1, Math.min(100, tag.contains("Complexity") ? tag.getInt("Complexity") : 10));
        setComplexity(complexity);
        setRequiredMarks(Math.max(1, tag.getInt("RequiredMarks")));
        setRemainingMarks(tag.getInt("RemainingMarks"));
        occupied = tag.getBoolean("Occupied");
        active = tag.getBoolean("Active");
        controller = tag.getBoolean("Controller");
        setController(controller);
        blacklist = tag.contains("Blacklist") ? tag.getString("Blacklist") : "none";
        blacklistUuid = tag.hasUUID("BlacklistUuid") ? tag.getUUID("BlacklistUuid") : null;
        anchor = BlockPos.of(tag.getLong("Anchor"));
        face = Direction.from3DDataValue(tag.getInt("Face"));
        setAnchor(anchor, face);
        barrierHealth = Math.max(0.0F, Math.min(1000.0F, tag.getFloat("BarrierHealth")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (authorId != null) tag.putUUID("Author", authorId);
        if (boundaryId != null) tag.putUUID("Boundary", boundaryId);
        tag.putString("Type", getBoundaryType());
        tag.putInt("Side", getSide());
        tag.putInt("Power", getPower());
        tag.putInt("Complexity", getComplexity());
        tag.putInt("RequiredMarks", getRequiredMarks());
        tag.putInt("RemainingMarks", getRemainingMarks());
        tag.putBoolean("Occupied", occupied);
        tag.putBoolean("Active", active);
        tag.putBoolean("Controller", isController());
        tag.putString("Blacklist", getBlacklist());
        if (blacklistUuid != null) {
            tag.putUUID("BlacklistUuid", blacklistUuid);
        }
        tag.putLong("Anchor", anchor.asLong());
        tag.putInt("Face", getFace().get3DDataValue());
        tag.putFloat("BarrierHealth", barrierHealth);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}
