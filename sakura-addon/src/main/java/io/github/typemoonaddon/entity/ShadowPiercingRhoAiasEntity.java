package io.github.typemoonaddon.entity;

import io.github.typemoonaddon.config.GameplayConfig;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Addon-owned Rho Aias visual and shield state; it never subclasses the main mod. */
public final class ShadowPiercingRhoAiasEntity extends Entity implements GeoEntity {
    private static final double BUD_HOLD_TICKS = 2.0417D * 20.0D;
    private static final double BUD_LAUNCH_TICKS = 0.25D * 20.0D;
    private static final double BUD_LAUNCH_DISTANCE = 2.0D;
    private static final double RIGHT_PALM_FORWARD_OFFSET = 10.0D / 16.0D;
    private static final double RIGHT_PALM_SIDE_OFFSET = 5.0D / 16.0D;
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(
        ShadowPiercingRhoAiasEntity.class,
        EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Float> SHIELD_HP = SynchedEntityData.defineId(
        ShadowPiercingRhoAiasEntity.class,
        EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> FIXED_FACING_YAW = SynchedEntityData.defineId(
        ShadowPiercingRhoAiasEntity.class,
        EntityDataSerializers.FLOAT
    );
    private static final RawAnimation SEQUENCE = RawAnimation.begin()
        .thenPlay("animation.shadow_piercing_rho_aias.sequence");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private Vec3 fixedPosition;

    public ShadowPiercingRhoAiasEntity(EntityType<?> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
    }

    public void initialize(LivingEntity owner, @Nullable LivingEntity threat) {
        Vec3 direction = fixedDirection(owner, threat);
        float yaw = (float)(Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        entityData.set(SHIELD_HP, 2_000.0F);
        entityData.set(FIXED_FACING_YAW, yaw);
        Vec3 right = new Vec3(-direction.z, 0.0D, direction.x);
        fixedPosition = owner.position()
            .add(direction.scale(RIGHT_PALM_FORWARD_OFFSET + BUD_LAUNCH_DISTANCE))
            .add(right.scale(RIGHT_PALM_SIDE_OFFSET))
            .add(0.0D, owner.getBbHeight() * 0.55D, 0.0D);
        setPos(fixedPosition.x, fixedPosition.y, fixedPosition.z);
    }

    private static Vec3 fixedDirection(LivingEntity owner, @Nullable LivingEntity threat) {
        Vec3 direction = threat == null
            ? owner.getLookAngle()
            : threat.getBoundingBox().getCenter().subtract(owner.getEyePosition());
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        return horizontal.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
        builder.define(SHIELD_HP, 2_000.0F);
        builder.define(FIXED_FACING_YAW, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        float yaw = getFacingYaw();
        yRotO = yaw;
        setYRot(yaw);
        if (!level().isClientSide && fixedPosition != null) {
            setPos(fixedPosition.x, fixedPosition.y, fixedPosition.z);
        }
        if (!level().isClientSide && tickCount >= GameplayConfig.EMIYA_SHADOW_RHO_AIAS_TOTAL_TICKS) {
            discard();
        }
    }

    public float getFacingYaw() {
        return entityData.get(FIXED_FACING_YAW);
    }

    public Vec3 getFacingDirection() {
        double radians = (getFacingYaw() + 90.0F) * Mth.DEG_TO_RAD;
        return new Vec3(Math.cos(radians), 0.0D, Math.sin(radians)).normalize();
    }

    public Vec3 getBudRenderOffset(float partialTick) {
        double launchProgress = Mth.clamp(
            (tickCount + partialTick - BUD_HOLD_TICKS) / BUD_LAUNCH_TICKS,
            0.0D,
            1.0D
        );
        return getFacingDirection().scale(-BUD_LAUNCH_DISTANCE * (1.0D - launchProgress));
    }

    public boolean protects(LivingEntity target) {
        return tickCount >= GameplayConfig.EMIYA_SHADOW_RHO_AIAS_SEQUENCE_TICKS
            && target != null
            && target.distanceToSqr(this) <= 36.0D;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F || entityData.get(SHIELD_HP) <= 0.0F) {
            return false;
        }
        float remaining = Math.max(0.0F, entityData.get(SHIELD_HP) - amount);
        entityData.set(SHIELD_HP, remaining);
        if (remaining <= 0.0F) {
            discard();
        }
        return true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        entityData.get(OWNER).ifPresent(owner -> tag.putUUID("Owner", owner));
        tag.putFloat("ShieldHp", entityData.get(SHIELD_HP));
        tag.putFloat("SequenceFixedYaw", getFacingYaw());
        if (fixedPosition != null) {
            tag.putDouble("SequenceFixedX", fixedPosition.x);
            tag.putDouble("SequenceFixedY", fixedPosition.y);
            tag.putDouble("SequenceFixedZ", fixedPosition.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(SHIELD_HP, tag.contains("ShieldHp") ? tag.getFloat("ShieldHp") : 2_000.0F);
        entityData.set(FIXED_FACING_YAW, tag.getFloat("SequenceFixedYaw"));
        if (tag.contains("SequenceFixedX")) {
            fixedPosition = new Vec3(
                tag.getDouble("SequenceFixedX"),
                tag.getDouble("SequenceFixedY"),
                tag.getDouble("SequenceFixedZ")
            );
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "sequence", 0, state -> state.setAndContinue(SEQUENCE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
