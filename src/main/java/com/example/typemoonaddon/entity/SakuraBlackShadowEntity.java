package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.magic.SakuraBlackMudHuntService;
import com.example.typemoonaddon.magic.SakuraSummonBlackMudService;
import com.example.typemoonaddon.config.GameplayConfig;
import java.util.Arrays;
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

public class SakuraBlackShadowEntity extends Monster {
    public static final byte ACTION_IDLE = 0;
    public static final byte ACTION_WALK = 1;
    public static final byte ACTION_ATTACK = 2;
    public static final int RIBBON_COUNT = 10;
    public static final int ATTACK_TOTAL_TICKS = 22;
    public static final int MAGIC_OUTPUT_CHARGE_TICKS = GameplayConfig.MAGIC_OUTPUT_CHARGE_TICKS;
    public static final float MAGIC_OUTPUT_ORB_START_RADIUS = GameplayConfig.MAGIC_OUTPUT_ORB_START_RADIUS;
    public static final float MAGIC_OUTPUT_ORB_END_RADIUS = GameplayConfig.MAGIC_OUTPUT_ORB_END_RADIUS;
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(
            SakuraBlackShadowEntity.class,
            EntityDataSerializers.BYTE
    );
    private static final EntityDataAccessor<Integer> DATA_ACTION_START = SynchedEntityData.defineId(
            SakuraBlackShadowEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> DATA_MAGIC_OUTPUT_CHARGE = SynchedEntityData.defineId(
            SakuraBlackShadowEntity.class,
            EntityDataSerializers.INT
    );

    @Nullable
    private UUID ownerId;
    private final int[] shadowArtRecoveryTicks = new int[RIBBON_COUNT];
    private double actionMana = 200.0D;

    public SakuraBlackShadowEntity(EntityType<? extends SakuraBlackShadowEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACTION_IDLE);
        builder.define(DATA_ACTION_START, 0);
        builder.define(DATA_MAGIC_OUTPUT_CHARGE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            syncRenderAction();
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = owner(level);
        if (owner == null || owner.isSpectator() || owner.isDeadOrDying()) {
            discard();
            return;
        }
        LivingEntity huntTarget = SakuraBlackMudHuntService.targetFor(this);
        if (huntTarget != null) {
            setTarget(huntTarget);
            getNavigation().moveTo(huntTarget, 1.0D);
        } else if (getTarget() == null || !canAttack(getTarget())) {
            LivingEntity nearby = level.getEntitiesOfClass(
                            LivingEntity.class,
                            getBoundingBox().inflate(18.0D),
                            target -> target != owner && canAttack(target)
                    )
                    .stream()
                    .min(java.util.Comparator.comparingDouble(this::distanceToSqr))
                    .orElse(null);
            setTarget(nearby);
        }
    }

    public byte getAction() {
        return entityData.get(DATA_ACTION);
    }

    public float getAttackProgress(float ageInTicks) {
        if (getAction() != ACTION_ATTACK) {
            return 0.0F;
        }
        return Mth.clamp((ageInTicks - entityData.get(DATA_ACTION_START)) / ATTACK_TOTAL_TICKS, 0.0F, 1.0F);
    }

    public int getMagicOutputChargeTicks() {
        return entityData.get(DATA_MAGIC_OUTPUT_CHARGE);
    }

    public float magicOutputOrbRadius(float partialTick) {
        int chargeTicks = getMagicOutputChargeTicks();
        if (chargeTicks <= 0) {
            return 0.0F;
        }
        float elapsed = MAGIC_OUTPUT_CHARGE_TICKS - chargeTicks + partialTick;
        float progress = Mth.clamp(elapsed / MAGIC_OUTPUT_CHARGE_TICKS, 0.0F, 1.0F);
        return Mth.lerp(progress, MAGIC_OUTPUT_ORB_START_RADIUS, MAGIC_OUTPUT_ORB_END_RADIUS);
    }

    private void setAction(byte action) {
        if (entityData.get(DATA_ACTION) == action) {
            return;
        }
        entityData.set(DATA_ACTION, action);
        entityData.set(DATA_ACTION_START, tickCount);
    }

    private void syncRenderAction() {
        if (swinging || swingTime > 0) {
            setAction(ACTION_ATTACK);
            return;
        }
        Vec3 movement = getDeltaMovement();
        setAction(movement.horizontalDistanceSqr() > 1.0E-5D || !getNavigation().isDone() ? ACTION_WALK : ACTION_IDLE);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) {
            SakuraSummonBlackMudService.blackShadowRemoved(this);
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putIntArray("ShadowArtRecoveryTicks", shadowArtRecoveryTicks);
        tag.putDouble("ActionMana", actionMana);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        Arrays.fill(shadowArtRecoveryTicks, 0);
        int[] savedTicks = tag.getIntArray("ShadowArtRecoveryTicks");
        for (int index = 0; index < shadowArtRecoveryTicks.length && index < savedTicks.length; index++) {
            shadowArtRecoveryTicks[index] = Math.max(0, savedTicks[index]);
        }
        actionMana = Math.max(0.0D, tag.getDouble("ActionMana"));
    }

    public void onCommandModeApplied() {
        getNavigation().stop();
        setTarget(null);
    }

    public boolean canAttack(@Nullable LivingEntity target) {
        return target != null
                && target != this
                && target.isAlive()
                && !target.isRemoved()
                && target.level() == level()
                && !target.isAlliedTo(this)
                && !(target instanceof ServerPlayer player && (player.isCreative() || player.isSpectator()));
    }

    public void dismiss(ServerPlayer player) {
        if (player != null && player.getUUID().equals(ownerId)) {
            discard();
        }
    }

    public void rewardOwnerManaFromDamage(double amount) {
        if (amount > 0.0D && Double.isFinite(amount)) {
            actionMana = Math.min(actionMana + amount, 1_000.0D);
        }
    }

    public boolean tryConsumeActionMana(double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        if (actionMana < amount) {
            return false;
        }
        actionMana -= amount;
        return true;
    }

    public int shadowArtRecoveryTicks(int index) {
        return shadowArtRecoveryTicks[Math.clamp(index, 0, shadowArtRecoveryTicks.length - 1)];
    }

    public void breakShadowArtRibbon(int index) {
        shadowArtRecoveryTicks[Math.clamp(index, 0, shadowArtRecoveryTicks.length - 1)] = 30 * 20;
    }

    public boolean tickShadowArtRecovery(int index) {
        index = Math.clamp(index, 0, shadowArtRecoveryTicks.length - 1);
        if (shadowArtRecoveryTicks[index] <= 0) {
            return false;
        }
        return --shadowArtRecoveryTicks[index] == 0;
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
            if (entity instanceof LivingEntity living) {
                return living;
            }
            return level.getServer().getPlayerList().getPlayer(ownerId);
        }
        return level().getPlayerByUUID(ownerId);
    }

    @Nullable
    private ServerPlayer owner(ServerLevel level) {
        return ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
    }

    public void moveNear(Vec3 destination) {
        getNavigation().moveTo(destination.x, destination.y, destination.z, 0.9D);
    }
}
