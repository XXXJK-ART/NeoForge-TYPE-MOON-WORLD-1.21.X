package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraBlackMudHuntService;
import com.example.typemoonaddon.registry.AddonAttachments;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SakuraShadowFamiliarEntity extends Monster {
    public static final float MIN_SUMMON_SIZE = 1.0F;
    public static final float MAX_SUMMON_SIZE = 6.0F;
    public static final byte ATTACK_PHASE_IDLE = 0;
    public static final byte ATTACK_PHASE_SHADOW_BINDING = 1;
    public static final byte ATTACK_PHASE_VOID_ABSORPTION = 4;
    public static final int SHADOW_BINDING_CAST_TICKS = 20;
    public static final int VOID_ABSORPTION_CAST_TICKS = 30;
    private static final double BINDING_RANGE = 50.0D;
    private static final EntityDataAccessor<Boolean> DATA_FORMING = SynchedEntityData.defineId(
            SakuraShadowFamiliarEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Byte> DATA_ATTACK_PHASE = SynchedEntityData.defineId(
            SakuraShadowFamiliarEntity.class,
            EntityDataSerializers.BYTE
    );
    private static final EntityDataAccessor<Integer> DATA_ATTACK_PHASE_START = SynchedEntityData.defineId(
            SakuraShadowFamiliarEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Boolean> DATA_VOID_ABSORPTION_CONNECTED = SynchedEntityData.defineId(
            SakuraShadowFamiliarEntity.class,
            EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_SHADOW_BINDING_CONNECTED = SynchedEntityData.defineId(
            SakuraShadowFamiliarEntity.class,
            EntityDataSerializers.BOOLEAN
    );

    @Nullable
    private UUID ownerId;
    private int ownerDismissalGeneration;
    private float summonSize = MIN_SUMMON_SIZE;
    private boolean forming;
    private double storedGrowthMana;

    public SakuraShadowFamiliarEntity(EntityType<? extends SakuraShadowFamiliarEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FORMING, false);
        builder.define(DATA_ATTACK_PHASE, ATTACK_PHASE_IDLE);
        builder.define(DATA_ATTACK_PHASE_START, 0);
        builder.define(DATA_VOID_ABSORPTION_CONNECTED, false);
        builder.define(DATA_SHADOW_BINDING_CONNECTED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level) || isForming()) {
            return;
        }
        ServerPlayer owner = owner(level);
        if (owner == null || owner.isSpectator() || owner.isDeadOrDying()) {
            discard();
            return;
        }
        ImaginarySpaceData data = owner.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (ownerDismissalGeneration != data.shadowDismissalGeneration()) {
            discard();
            return;
        }
        SakuraBlackMudHuntService.HuntOrder huntOrder = SakuraBlackMudHuntService.orderFor(this);
        if (huntOrder != null) {
            moveToward(huntOrder.destination(), 1.15D);
            setTarget(huntOrder.target());
            return;
        }
        if (data.shadowAttackAround()) {
            LivingEntity nearby = level.getEntitiesOfClass(
                            LivingEntity.class,
                            getBoundingBox().inflate(8.0D),
                            target -> target != owner && target.isAlive() && !target.isAlliedTo(owner) && !owner.isAlliedTo(target)
                    )
                    .stream()
                    .min(java.util.Comparator.comparingDouble(this::distanceToSqr))
                    .orElse(null);
            if (nearby != null) {
                setTarget(nearby);
            }
        }
        switch (data.activeShadowCommandMode()) {
            case GATHER -> moveToward(owner.position(), 1.0D);
            case HOLD -> getNavigation().stop();
            case SPREAD -> moveToward(owner.position().add(spreadOffset()), 0.7D);
            default -> {
                if (distanceToSqr(owner) > 30.0D * 30.0D) {
                    moveToward(owner.position(), 0.85D);
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putInt("OwnerDismissalGeneration", ownerDismissalGeneration);
        tag.putFloat("SummonSize", summonSize);
        tag.putBoolean("Forming", forming);
        tag.putDouble("StoredGrowthMana", storedGrowthMana);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerDismissalGeneration = tag.getInt("OwnerDismissalGeneration");
        summonSize = Math.clamp(tag.getFloat("SummonSize"), MIN_SUMMON_SIZE, MAX_SUMMON_SIZE);
        setForming(tag.getBoolean("Forming"));
        storedGrowthMana = Math.max(0.0D, tag.getDouble("StoredGrowthMana"));
        refreshDimensions();
    }

    public void onCommandModeApplied() {
        getNavigation().stop();
        setTarget(null);
    }

    public boolean isWithinShadowBindingRange(LivingEntity target) {
        return target != null && target.level() == level() && distanceToSqr(target) <= BINDING_RANGE * BINDING_RANGE;
    }

    public void prepareForHunt(LivingEntity target, float uniformSize) {
        setSummonSize(uniformSize);
        setTarget(target);
    }

    public void finishHunt() {
        setTarget(null);
        getNavigation().stop();
    }

    public double takeStoredGrowthMana() {
        double mana = storedGrowthMana;
        storedGrowthMana = 0.0D;
        return mana;
    }

    public void addStoredGrowthMana(double amount) {
        if (amount > 0.0D && Double.isFinite(amount)) {
            storedGrowthMana += amount;
        }
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (ownerId == null) {
            return null;
        }
        if (level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(ownerId);
            return entity instanceof LivingEntity living ? living : level.getServer().getPlayerList().getPlayer(ownerId);
        }
        return level().getPlayerByUUID(ownerId);
    }

    public int getOwnerDismissalGeneration() {
        return ownerDismissalGeneration;
    }

    public void setOwnerDismissalGeneration(int ownerDismissalGeneration) {
        this.ownerDismissalGeneration = ownerDismissalGeneration;
    }

    public float getSummonSize() {
        return summonSize;
    }

    public void setSummonSize(float summonSize) {
        this.summonSize = Math.clamp(summonSize, MIN_SUMMON_SIZE, MAX_SUMMON_SIZE);
        refreshDimensions();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(14.0D + this.summonSize * 8.0D);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1.0D + this.summonSize * 1.4D);
    }

    public boolean isForming() {
        return entityData.get(DATA_FORMING);
    }

    public void setForming(boolean forming) {
        this.forming = forming;
        entityData.set(DATA_FORMING, forming);
        if (forming) {
            setAttackPhase(ATTACK_PHASE_IDLE);
        }
    }

    public byte getAttackPhase() {
        return entityData.get(DATA_ATTACK_PHASE);
    }

    public float getAttackPhaseProgress(float ageInTicks) {
        int duration = switch (getAttackPhase()) {
            case ATTACK_PHASE_SHADOW_BINDING -> SHADOW_BINDING_CAST_TICKS;
            case ATTACK_PHASE_VOID_ABSORPTION -> VOID_ABSORPTION_CAST_TICKS;
            default -> 1;
        };
        return Mth.clamp((ageInTicks - entityData.get(DATA_ATTACK_PHASE_START)) / duration, 0.0F, 1.0F);
    }

    public boolean isVoidAbsorptionConnected() {
        return entityData.get(DATA_VOID_ABSORPTION_CONNECTED);
    }

    public boolean isShadowBindingConnected() {
        return entityData.get(DATA_SHADOW_BINDING_CONNECTED);
    }

    private void setAttackPhase(byte attackPhase) {
        if (entityData.get(DATA_ATTACK_PHASE) == attackPhase) {
            return;
        }
        entityData.set(DATA_ATTACK_PHASE, attackPhase);
        entityData.set(DATA_ATTACK_PHASE_START, tickCount);
    }

    @Nullable
    private ServerPlayer owner(ServerLevel level) {
        return ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
    }

    private Vec3 spreadOffset() {
        double angle = (getUUID().getLeastSignificantBits() & 1023L) / 1024.0D * Math.PI * 2.0D;
        return new Vec3(Math.cos(angle) * 10.0D, 0.0D, Math.sin(angle) * 10.0D);
    }

    private void moveToward(Vec3 destination, double speed) {
        getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
    }
}
