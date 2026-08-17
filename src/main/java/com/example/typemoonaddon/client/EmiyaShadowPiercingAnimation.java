package com.example.typemoonaddon.client;

import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import com.example.typemoonaddon.entity.SakuraShadowArtRibbonEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Applies the interrupted UBW chant pose to a pinned Archer model. */
public final class EmiyaShadowPiercingAnimation {
    private static final ResourceLocation EMIYA_ARCHER = ResourceLocation.fromNamespaceAndPath(
        "typemoonworld",
        "emiya_archer"
    );
    private static final double SEARCH_PADDING = 4.0D;
    private static final float HEAD_BASE_X = 0.97709F;
    private static final float HEAD_BASE_Y = -0.02725F;
    private static final float HEAD_BASE_Z = 0.52199F;
    private static final float LEFT_ARM_BASE_X = 0.5F;
    private static final float RIGHT_LEG_BASE_Z = 2.0F;
    private static final float LEFT_LEG_BASE_Z = -2.0F;
    private static final double RHO_AIAS_SOURCE_DURATION_SECONDS = 2.0D;
    private static final ThreadLocal<PreparedPose> PREPARED = new ThreadLocal<>();

    // Keyframes mirror blockbench/emiya_shadow_rho_aias.animation.json.
    private static final Keyframes RHO_RIGHT_ARM = keyframes(
        new double[]{0.0D, 0.25D, 0.5D, 0.75D, 1.0D, 1.25D},
        new float[]{-67.5F, -80.0F, -85.0F, -88.0F, -90.0F, -90.0F}
    );

    public static void prepare(LivingEntity entity, float partialTick) {
        PREPARED.remove();
        AnimationPose pose = resolvePose(entity, partialTick);
        if (pose == null) {
            return;
        }
        PREPARED.set(new PreparedPose(entity.getUUID(), pose));
    }

    public static void clearPrepared() {
        PREPARED.remove();
    }

    public static void applyPreparedModel(LivingEntity entity, PlayerModel<?> model) {
        PreparedPose prepared = PREPARED.get();
        PREPARED.remove();
        if (prepared == null || !prepared.entityId().equals(entity.getUUID())) {
            return;
        }
        AnimationPose pose = prepared.pose();
        setRotation(model.head, pose.headX() + HEAD_BASE_X, HEAD_BASE_Y, HEAD_BASE_Z);
        setRotation(model.body, 0.0F, 0.0F, 0.0F);
        setRotation(model.rightArm, pose.rightArmX(), 0.0F, 0.0F);
        setRotation(model.leftArm, pose.leftArmX() + LEFT_ARM_BASE_X, 0.0F, 0.0F);
        setRotation(model.rightLeg, pose.rightLegX(), 0.0F, RIGHT_LEG_BASE_Z);
        setRotation(model.leftLeg, pose.leftLegX(), 0.0F, LEFT_LEG_BASE_Z);
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    public static boolean isPerformanceActive(LivingEntity entity, float partialTick) {
        double tick = performanceTick(entity, partialTick);
        return tick >= 0.0D && tick < totalPerformanceTicks();
    }

    @Nullable
    private static AnimationPose resolvePose(LivingEntity entity, float partialTick) {
        if (!EMIYA_ARCHER.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
            return null;
        }
        double animationTick = performanceTick(entity, partialTick);
        if (animationTick < 0.0D) {
            return null;
        }
        if (animationTick < totalPerformanceTicks()) {
            double chantTick = Math.min(animationTick, GameplayConfig.EMIYA_SHADOW_CHANT_TICKS);
            return rhoAiasPose(chantTick / 20.0D);
        }
        return null;
    }

    private static double performanceTick(LivingEntity entity, float partialTick) {
        if (!EMIYA_ARCHER.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
            return -1.0D;
        }
        long pinnedStart = pinnedStartGameTime(entity);
        if (pinnedStart < 0L) {
            return -1.0D;
        }
        return entity.level().getGameTime() + partialTick - pinnedStart
            - GameplayConfig.EMIYA_SHADOW_PIN_ANIMATION_DELAY_TICKS;
    }

    private static int totalPerformanceTicks() {
        return GameplayConfig.EMIYA_SHADOW_CHANT_TICKS
            + GameplayConfig.EMIYA_SHADOW_RHO_AIAS_SEQUENCE_TICKS;
    }

    private static long pinnedStartGameTime(LivingEntity target) {
        Map<UUID, Integer> countsByOwner = new HashMap<>();
        Map<UUID, Long> startsByOwner = new HashMap<>();
        for (SakuraShadowArtRibbonEntity ribbon : target.level().getEntitiesOfClass(
            SakuraShadowArtRibbonEntity.class,
            target.getBoundingBox().inflate(SEARCH_PADDING),
            ribbon -> ribbon.action() == SakuraShadowArtRibbonEntity.PINNED
                && ribbon.targetEntityId() == target.getId()
        )) {
            Entity owner = ribbon.ownerEntityId() < 0 ? null : target.level().getEntity(ribbon.ownerEntityId());
            if (!(owner instanceof SakuraBlackShadowEntity)
                || ribbon.ownerId() == null
                || !ribbon.ownerId().equals(owner.getUUID())
                || ribbon.actionStartGameTime() < 0L) {
                continue;
            }
            UUID ownerId = ribbon.ownerId();
            countsByOwner.merge(ownerId, 1, Integer::sum);
            startsByOwner.merge(ownerId, ribbon.actionStartGameTime(), Math::max);
        }
        long selectedStart = -1L;
        for (Map.Entry<UUID, Integer> entry : countsByOwner.entrySet()) {
            if (entry.getValue() >= GameplayConfig.SHADOW_ART_HOLD_RIBBON_COUNT) {
                selectedStart = Math.max(selectedStart, startsByOwner.getOrDefault(entry.getKey(), -1L));
            }
        }
        return selectedStart;
    }

    private static AnimationPose rhoAiasPose(double seconds) {
        double durationSeconds = GameplayConfig.EMIYA_SHADOW_CHANT_TICKS / 20.0D;
        double sourceSeconds = seconds * RHO_AIAS_SOURCE_DURATION_SECONDS / durationSeconds;
        return new AnimationPose(
            0.0F,
            RHO_RIGHT_ARM.sample(sourceSeconds),
            0.0F,
            0.0F,
            0.0F
        );
    }

    private static void setRotation(ModelPart part, float xDegrees, float yDegrees, float zDegrees) {
        part.xRot = (float)Math.toRadians(xDegrees);
        part.yRot = (float)Math.toRadians(yDegrees);
        part.zRot = (float)Math.toRadians(zDegrees);
    }

    private static Keyframes keyframes(double[] times, float[] values) {
        return new Keyframes(times, values);
    }

    private record Keyframes(double[] times, float[] values) {
        private float sample(double time) {
            if (time <= times[0]) {
                return values[0];
            }
            for (int index = 1; index < times.length; index++) {
                if (time <= times[index]) {
                    float progress = (float)((time - times[index - 1]) / (times[index] - times[index - 1]));
                    return Mth.lerp(progress, values[index - 1], values[index]);
                }
            }
            return values[values.length - 1];
        }
    }

    private record AnimationPose(
        float headX,
        float rightArmX,
        float leftArmX,
        float rightLegX,
        float leftLegX
    ) {
    }

    private record PreparedPose(UUID entityId, AnimationPose pose) {
    }

    private EmiyaShadowPiercingAnimation() {
    }
}
