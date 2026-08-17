package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.config.GameplayConfig;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

/** Imported Rho Aias sequence followed by a short functional shield phase. */
public final class SakuraShadowPiercingRhoAiasEntity extends RhoAiasEntity {
    private static final double BUD_HOLD_TICKS = 2.0417D * 20.0D;
    private static final double BUD_LAUNCH_TICKS = 0.25D * 20.0D;
    private static final double BUD_LAUNCH_DISTANCE = 2.0D;
    private static final double RIGHT_PALM_FORWARD_OFFSET = 10.0D / 16.0D;
    private static final double RIGHT_PALM_SIDE_OFFSET = 5.0D / 16.0D;
    private static final EntityDataAccessor<Float> FIXED_FACING_YAW = SynchedEntityData.defineId(
        SakuraShadowPiercingRhoAiasEntity.class,
        EntityDataSerializers.FLOAT
    );
    private static final RawAnimation SEQUENCE = RawAnimation.begin()
        .thenPlay("animation.shadow_piercing_rho_aias.sequence");
    @Nullable
    private Vec3 fixedPosition;

    public SakuraShadowPiercingRhoAiasEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void initialize(LivingEntity owner, @Nullable LivingEntity threat) {
        Vec3 direction = fixedDirection(owner, threat);
        float yaw = (float)(Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.entityData.set(FIXED_FACING_YAW, yaw);
        CompoundTag state = new CompoundTag();
        state.putUUID("Owner", owner.getUUID());
        state.putFloat("ShieldHp", 2_000.0F);
        state.putInt("Duration", GameplayConfig.EMIYA_SHADOW_RHO_AIAS_TOTAL_TICKS);
        state.putInt("Layers", 7);
        state.putFloat("FixedYaw", yaw);
        readAdditionalSaveData(state);

        Vec3 right = new Vec3(-direction.z, 0.0D, direction.x);
        Vec3 position = owner.position()
            .add(direction.scale(RIGHT_PALM_FORWARD_OFFSET + BUD_LAUNCH_DISTANCE))
            .add(right.scale(RIGHT_PALM_SIDE_OFFSET))
            .add(0.0D, owner.getBbHeight() * 0.55D, 0.0D);
        this.fixedPosition = position;
        setPos(position.x, position.y, position.z);
        setXRot(0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FIXED_FACING_YAW, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) {
            return;
        }
        float yaw = getFacingYaw();
        this.yRotO = yaw;
        setYRot(yaw);
        if (!level().isClientSide && this.fixedPosition != null) {
            setPos(this.fixedPosition.x, this.fixedPosition.y, this.fixedPosition.z);
        }
    }

    @Override
    public float getFacingYaw() {
        return this.entityData.get(FIXED_FACING_YAW);
    }

    @Override
    public Vec3 getFacingDirection() {
        double radians = (getFacingYaw() + 90.0F) * Mth.DEG_TO_RAD;
        return new Vec3(Math.cos(radians), 0.0D, Math.sin(radians)).normalize();
    }

    /** Keeps the forming bud at Archer's raised hand, then sends it exactly two blocks forward. */
    public Vec3 getBudRenderOffset(float partialTick) {
        double launchProgress = Mth.clamp(
            (this.tickCount + partialTick - BUD_HOLD_TICKS) / BUD_LAUNCH_TICKS,
            0.0D,
            1.0D
        );
        return getFacingDirection().scale(-BUD_LAUNCH_DISTANCE * (1.0D - launchProgress));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "sequence", 0, state -> state.setAndContinue(SEQUENCE)));
    }

    @Override
    public boolean protects(LivingEntity target) {
        return this.tickCount >= GameplayConfig.EMIYA_SHADOW_RHO_AIAS_SEQUENCE_TICKS
            && super.protects(target);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("SequenceFixedYaw", getFacingYaw());
        if (this.fixedPosition != null) {
            tag.putDouble("SequenceFixedX", this.fixedPosition.x);
            tag.putDouble("SequenceFixedY", this.fixedPosition.y);
            tag.putDouble("SequenceFixedZ", this.fixedPosition.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SequenceFixedYaw")) {
            this.entityData.set(FIXED_FACING_YAW, tag.getFloat("SequenceFixedYaw"));
        }
        if (tag.contains("SequenceFixedX") && tag.contains("SequenceFixedY") && tag.contains("SequenceFixedZ")) {
            this.fixedPosition = new Vec3(
                tag.getDouble("SequenceFixedX"),
                tag.getDouble("SequenceFixedY"),
                tag.getDouble("SequenceFixedZ")
            );
        }
    }

    private static Vec3 fixedDirection(LivingEntity owner, @Nullable LivingEntity threat) {
        Vec3 direction = threat == null ? owner.getLookAngle() : threat.position().subtract(owner.position());
        direction = new Vec3(direction.x, 0.0D, direction.z);
        if (direction.lengthSqr() < 1.0E-4D) {
            Vec3 look = owner.getLookAngle();
            direction = new Vec3(look.x, 0.0D, look.z);
        }
        return direction.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
    }
}
