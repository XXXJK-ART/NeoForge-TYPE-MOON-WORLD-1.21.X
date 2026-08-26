package io.github.typemoonaddon.client.model;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

public final class BlackShadowModel extends HierarchicalModel<BlackShadowEntity> {
    private static final int RADIAL_SEGMENTS = 24;
    private static final int CLOTH_PANEL_COUNT = 6;
    private static final int BODY_PANEL_SEGMENTS = RADIAL_SEGMENTS / CLOTH_PANEL_COUNT;
    private static final int CLOTH_PANEL_SEGMENTS = 6;
    private static final int DRAGGING_RING_COUNT = 33;
    private static final int DRAGGING_SPLIT_RING = 12;
    private static final int DRAGGING_GROUND_RING = 26;
    private static final int FLOATING_RING_COUNT = 25;
    private static final int FLOATING_SPLIT_RING = 8;
    private static final float OUTLINE_EXPANSION = 0.18F;
    private static final float MODEL_SCALE = 1.80F / 1.56F;
    private static final float MODEL_HEIGHT = 24.96F;
    private static final float TOP_Y = -MODEL_HEIGHT;
    private static final float CLOTH_U_MIN = 32.5F / 64.0F;
    private static final float CLOTH_U_MAX = 63.5F / 64.0F;

    // A single rotational profile keeps the silhouette identical from every horizontal angle.
    private static final float[] PROFILE_Y = {
        0.0F, -1.7F, -4.3F, -7.8F, -12.1F, -17.3F,
        -21.1F, -21.8F, -22.7F, -23.5F, -24.2F, -24.7F, -24.9F
    };
    private static final float[] PROFILE_RADIUS = {
        3.72F, 3.52F, 3.43F, 3.42F, 3.44F, 3.47F,
        3.49F, 3.46F, 3.30F, 2.91F, 2.25F, 1.45F, 0.65F
    };

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        TypeMoonAddon.id("black_shadow"),
        "main"
    );

    private final ModelPart root;
    private final ModelPart shadow;
    private float clothWave;
    private float clothMovement;

    public BlackShadowModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.shadow = root.getChild("black_shadow");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "black_shadow",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(
        BlackShadowEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float movement = entity.getAction() == BlackShadowEntity.ACTION_WALK
            ? Mth.clamp(limbSwingAmount * 1.5F, 0.0F, 1.0F)
            : 0.0F;
        float drift = Mth.sin(ageInTicks * 0.075F);
        float step = Mth.sin(limbSwing * 0.7F) * movement;
        this.shadow.y = 24.0F
            - Mth.abs(Mth.cos(limbSwing * 0.7F)) * movement * 0.28F
            + drift * 0.1F;
        this.shadow.zRot = step * 0.018F + drift * 0.008F;
        this.clothWave = limbSwing * 0.9F + ageInTicks * 0.04F;
        this.clothMovement = Mth.clamp(movement * 2.0F, 0.0F, 1.0F);

        float attackProgress = entity.getAttackProgress(ageInTicks);
        if (attackProgress > 0.0F) {
            float attackMotion = Mth.sin(attackProgress * Mth.PI);
            this.shadow.xRot = attackMotion * 0.13F;
            this.shadow.z -= attackMotion * 0.75F;
        }
    }

    @Override
    public void renderToBuffer(
        PoseStack poseStack,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        this.renderGeometry(poseStack, consumer, packedLight, packedOverlay, packedColor, false);
    }

    public void renderOutlineToBuffer(
        PoseStack poseStack,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        this.renderGeometry(poseStack, consumer, packedLight, packedOverlay, packedColor, true);
    }

    private void renderGeometry(
        PoseStack poseStack,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor,
        boolean outline
    ) {
        poseStack.pushPose();
        this.shadow.translateAndRotate(poseStack);
        poseStack.scale(MODEL_SCALE / 16.0F, MODEL_SCALE / 16.0F, MODEL_SCALE / 16.0F);
        PoseStack.Pose pose = poseStack.last();

        for (int ring = 0; ring < PROFILE_Y.length - 1; ring++) {
            float lowerY = outlineY(PROFILE_Y[ring], outline);
            float upperY = outlineY(PROFILE_Y[ring + 1], outline);
            float lowerRadius = PROFILE_RADIUS[ring] + outlineExpansion(outline);
            float upperRadius = PROFILE_RADIUS[ring + 1] + outlineExpansion(outline);
            float lowerSlope = profileSlope(ring);
            float upperSlope = profileSlope(ring + 1);
            float lowerV = 1.0F + lowerY / MODEL_HEIGHT;
            float upperV = 1.0F + upperY / MODEL_HEIGHT;

            for (int side = 0; side < RADIAL_SEGMENTS; side++) {
                float side0 = side / (float) RADIAL_SEGMENTS;
                float side1 = (side + 1) / (float) RADIAL_SEGMENTS;
                int panelSection = Math.floorMod(side + BODY_PANEL_SEGMENTS / 2, BODY_PANEL_SEGMENTS);
                float section0 = panelSection / (float) BODY_PANEL_SEGMENTS;
                float section1 = (panelSection + 1) / (float) BODY_PANEL_SEGMENTS;
                float u0 = Mth.lerp(section0, CLOTH_U_MIN, CLOTH_U_MAX);
                float u1 = Mth.lerp(section1, CLOTH_U_MIN, CLOTH_U_MAX);
                float angle0 = side0 * Mth.TWO_PI;
                float angle1 = side1 * Mth.TWO_PI;
                addSurfaceQuad(
                    pose,
                    consumer,
                    lowerY,
                    upperY,
                    lowerRadius,
                    upperRadius,
                    lowerSlope,
                    upperSlope,
                    angle0,
                    angle1,
                    u0,
                    u1,
                    lowerV,
                    upperV,
                    packedLight,
                    packedOverlay,
                    packedColor,
                    outline
                );
            }
        }

        addRoundedTop(pose, consumer, packedLight, packedOverlay, packedColor, outline);
        addClothCover(
            pose, consumer, packedLight, packedOverlay, packedColor,
            true, 0.72F, 0.07F, 1.35F, Mth.PI / CLOTH_PANEL_COUNT, outline
        );
        addClothCover(
            pose, consumer, packedLight, packedOverlay, packedColor,
            false, 1.0F, 0.14F, 0.0F, 0.0F, outline
        );
        poseStack.popPose();
    }

    private static void addSurfaceQuad(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float lowerY,
        float upperY,
        float lowerRadius,
        float upperRadius,
        float lowerSlope,
        float upperSlope,
        float angle0,
        float angle1,
        float u0,
        float u1,
        float lowerV,
        float upperV,
        int packedLight,
        int packedOverlay,
        int packedColor,
        boolean reversed
    ) {
        float lowerNormalScale = Mth.invSqrt(1.0F + lowerSlope * lowerSlope);
        float upperNormalScale = Mth.invSqrt(1.0F + upperSlope * upperSlope);
        float cos0 = Mth.cos(angle0);
        float sin0 = Mth.sin(angle0);
        float cos1 = Mth.cos(angle1);
        float sin1 = Mth.sin(angle1);

        float normalDirection = reversed ? -1.0F : 1.0F;
        if (reversed) {
            surfaceVertex(pose, consumer, upperRadius, upperY, cos0, sin0, upperSlope, upperNormalScale, u0, upperV, normalDirection, packedLight, packedOverlay, packedColor);
            surfaceVertex(pose, consumer, upperRadius, upperY, cos1, sin1, upperSlope, upperNormalScale, u1, upperV, normalDirection, packedLight, packedOverlay, packedColor);
            surfaceVertex(pose, consumer, lowerRadius, lowerY, cos1, sin1, lowerSlope, lowerNormalScale, u1, lowerV, normalDirection, packedLight, packedOverlay, packedColor);
            surfaceVertex(pose, consumer, lowerRadius, lowerY, cos0, sin0, lowerSlope, lowerNormalScale, u0, lowerV, normalDirection, packedLight, packedOverlay, packedColor);
            return;
        }
        surfaceVertex(pose, consumer, lowerRadius, lowerY, cos0, sin0, lowerSlope, lowerNormalScale, u0, lowerV, normalDirection, packedLight, packedOverlay, packedColor);
        surfaceVertex(pose, consumer, lowerRadius, lowerY, cos1, sin1, lowerSlope, lowerNormalScale, u1, lowerV, normalDirection, packedLight, packedOverlay, packedColor);
        surfaceVertex(pose, consumer, upperRadius, upperY, cos1, sin1, upperSlope, upperNormalScale, u1, upperV, normalDirection, packedLight, packedOverlay, packedColor);
        surfaceVertex(pose, consumer, upperRadius, upperY, cos0, sin0, upperSlope, upperNormalScale, u0, upperV, normalDirection, packedLight, packedOverlay, packedColor);
    }

    private static void surfaceVertex(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float radius,
        float y,
        float cos,
        float sin,
        float slope,
        float normalScale,
        float u,
        float v,
        float normalDirection,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        vertex(
            pose, consumer,
            radius * cos, y, radius * sin,
            cos * normalScale * normalDirection,
            -slope * normalScale * normalDirection,
            sin * normalScale * normalDirection,
            u, v, packedLight, packedOverlay, packedColor
        );
    }

    private static void addRoundedTop(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor,
        boolean reversed
    ) {
        int lastRing = PROFILE_Y.length - 1;
        float ringY = outlineY(PROFILE_Y[lastRing], reversed);
        float radius = PROFILE_RADIUS[lastRing] + outlineExpansion(reversed);
        float slope = profileSlope(lastRing);
        float normalScale = Mth.invSqrt(1.0F + slope * slope);
        float ringV = 1.0F + ringY / MODEL_HEIGHT;

        for (int side = 0; side < RADIAL_SEGMENTS; side++) {
            float side0 = side / (float) RADIAL_SEGMENTS;
            float side1 = (side + 1) / (float) RADIAL_SEGMENTS;
            int panelSection = Math.floorMod(side + BODY_PANEL_SEGMENTS / 2, BODY_PANEL_SEGMENTS);
            float section0 = panelSection / (float) BODY_PANEL_SEGMENTS;
            float section1 = (panelSection + 1) / (float) BODY_PANEL_SEGMENTS;
            float u0 = Mth.lerp(section0, CLOTH_U_MIN, CLOTH_U_MAX);
            float u1 = Mth.lerp(section1, CLOTH_U_MIN, CLOTH_U_MAX);
            float angle0 = side0 * Mth.TWO_PI;
            float angle1 = side1 * Mth.TWO_PI;
            float cos0 = Mth.cos(angle0);
            float sin0 = Mth.sin(angle0);
            float cos1 = Mth.cos(angle1);
            float sin1 = Mth.sin(angle1);

            float normalDirection = reversed ? -1.0F : 1.0F;
            float topY = reversed ? TOP_Y - OUTLINE_EXPANSION : TOP_Y;
            if (reversed) {
                topVertex(pose, consumer, topY, (u0 + u1) * 0.5F, normalDirection, packedLight, packedOverlay, packedColor);
                surfaceVertex(pose, consumer, radius, ringY, cos1, sin1, slope, normalScale, u1, ringV, normalDirection, packedLight, packedOverlay, packedColor);
                surfaceVertex(pose, consumer, radius, ringY, cos0, sin0, slope, normalScale, u0, ringV, normalDirection, packedLight, packedOverlay, packedColor);
                topVertex(pose, consumer, topY, (u0 + u1) * 0.5F, normalDirection, packedLight, packedOverlay, packedColor);
                continue;
            }
            surfaceVertex(pose, consumer, radius, ringY, cos0, sin0, slope, normalScale, u0, ringV, normalDirection, packedLight, packedOverlay, packedColor);
            surfaceVertex(pose, consumer, radius, ringY, cos1, sin1, slope, normalScale, u1, ringV, normalDirection, packedLight, packedOverlay, packedColor);
            topVertex(pose, consumer, topY, (u0 + u1) * 0.5F, normalDirection, packedLight, packedOverlay, packedColor);
            topVertex(pose, consumer, topY, (u0 + u1) * 0.5F, normalDirection, packedLight, packedOverlay, packedColor);
        }
    }

    private static void topVertex(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float y,
        float u,
        float normalDirection,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        vertex(
            pose, consumer, 0.0F, y, 0.0F,
            0.0F, -normalDirection, 0.0F,
            u, 0.0F, packedLight, packedOverlay, packedColor
        );
    }

    private void addClothCover(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor,
        boolean dragging,
        float layerScale,
        float baseLift,
        float phaseOffset,
        float baseAngleOffset,
        boolean reversed
    ) {
        float fullHalfWidth = Mth.PI / CLOTH_PANEL_COUNT;
        int ringCount = dragging ? DRAGGING_RING_COUNT : FLOATING_RING_COUNT;

        for (int panel = 0; panel < CLOTH_PANEL_COUNT; panel++) {
            float baseCenterAngle = panel * Mth.TWO_PI / CLOTH_PANEL_COUNT;

            for (int ring = 0; ring < ringCount - 1; ring++) {
                float upperDown = clothDown(dragging, ring);
                float lowerDown = clothDown(dragging, ring + 1);
                float upperBaseY = -MODEL_HEIGHT * (1.0F - upperDown);
                float lowerBaseY = -MODEL_HEIGHT * (1.0F - lowerDown);
                float upperY = outlineY(Math.min(
                    0.0F,
                    upperBaseY + clothFlutterY(dragging, panel, ring, layerScale, phaseOffset)
                ), reversed);
                float lowerY = outlineY(Math.min(
                    0.0F,
                    lowerBaseY + clothFlutterY(dragging, panel, ring + 1, layerScale, phaseOffset)
                ), reversed);
                float upperRadius = radiusAt(upperBaseY)
                    + clothLift(dragging, ring, layerScale, baseLift)
                    + clothFlutterLift(dragging, panel, ring, layerScale, phaseOffset)
                    + outlineExpansion(reversed);
                float lowerRadius = radiusAt(lowerBaseY)
                    + clothLift(dragging, ring + 1, layerScale, baseLift)
                    + clothFlutterLift(dragging, panel, ring + 1, layerScale, phaseOffset)
                    + outlineExpansion(reversed);
                float upperHalfWidth = fullHalfWidth * clothWidth(dragging, ring);
                float lowerHalfWidth = fullHalfWidth * clothWidth(dragging, ring + 1);
                float upperCenterAngle = baseCenterAngle
                    + baseAngleOffset * clothLayerOffsetProgress(dragging, ring)
                    + clothTwist(dragging, panel, ring)
                    + clothFlutterTwist(dragging, panel, ring, layerScale, phaseOffset);
                float lowerCenterAngle = baseCenterAngle
                    + baseAngleOffset * clothLayerOffsetProgress(dragging, ring + 1)
                    + clothTwist(dragging, panel, ring + 1)
                    + clothFlutterTwist(dragging, panel, ring + 1, layerScale, phaseOffset);
                float upperV = ring / (float)(ringCount - 1);
                float lowerV = (ring + 1) / (float)(ringCount - 1);

                for (int section = 0; section < CLOTH_PANEL_SEGMENTS; section++) {
                    float section0 = section / (float) CLOTH_PANEL_SEGMENTS;
                    float section1 = (section + 1) / (float) CLOTH_PANEL_SEGMENTS;
                    float upperAngle0 = upperCenterAngle + Mth.lerp(section0, -upperHalfWidth, upperHalfWidth);
                    float upperAngle1 = upperCenterAngle + Mth.lerp(section1, -upperHalfWidth, upperHalfWidth);
                    float lowerAngle0 = lowerCenterAngle + Mth.lerp(section0, -lowerHalfWidth, lowerHalfWidth);
                    float lowerAngle1 = lowerCenterAngle + Mth.lerp(section1, -lowerHalfWidth, lowerHalfWidth);
                    float u0 = Mth.lerp(section0, CLOTH_U_MIN, CLOTH_U_MAX);
                    float u1 = Mth.lerp(section1, CLOTH_U_MIN, CLOTH_U_MAX);

                    addClothQuad(
                        pose, consumer,
                        upperY, lowerY,
                        upperRadius, lowerRadius,
                        upperAngle0, upperAngle1,
                        lowerAngle0, lowerAngle1,
                        u0, u1, upperV, lowerV,
                        packedLight, packedOverlay, packedColor, reversed
                    );
                }
            }
        }
    }

    private static void addClothQuad(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float upperY,
        float lowerY,
        float upperRadius,
        float lowerRadius,
        float upperAngle0,
        float upperAngle1,
        float lowerAngle0,
        float lowerAngle1,
        float u0,
        float u1,
        float upperV,
        float lowerV,
        int packedLight,
        int packedOverlay,
        int packedColor,
        boolean reversed
    ) {
        if (reversed) {
            clothVertex(pose, consumer, upperRadius, upperAngle0, upperY, u0, upperV, -1.0F, packedLight, packedOverlay, packedColor);
            clothVertex(pose, consumer, upperRadius, upperAngle1, upperY, u1, upperV, -1.0F, packedLight, packedOverlay, packedColor);
            clothVertex(pose, consumer, lowerRadius, lowerAngle1, lowerY, u1, lowerV, -1.0F, packedLight, packedOverlay, packedColor);
            clothVertex(pose, consumer, lowerRadius, lowerAngle0, lowerY, u0, lowerV, -1.0F, packedLight, packedOverlay, packedColor);
            return;
        }
        clothVertex(pose, consumer, lowerRadius, lowerAngle0, lowerY, u0, lowerV, packedLight, packedOverlay, packedColor);
        clothVertex(pose, consumer, lowerRadius, lowerAngle1, lowerY, u1, lowerV, packedLight, packedOverlay, packedColor);
        clothVertex(pose, consumer, upperRadius, upperAngle1, upperY, u1, upperV, packedLight, packedOverlay, packedColor);
        clothVertex(pose, consumer, upperRadius, upperAngle0, upperY, u0, upperV, packedLight, packedOverlay, packedColor);
    }

    private static void clothVertex(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float radius,
        float angle,
        float y,
        float u,
        float v,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        clothVertex(pose, consumer, radius, angle, y, u, v, 1.0F, packedLight, packedOverlay, packedColor);
    }

    private static void clothVertex(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float radius,
        float angle,
        float y,
        float u,
        float v,
        float normalDirection,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        vertex(
            pose, consumer,
            radius * cos, y, radius * sin,
            cos * normalDirection, 0.0F, sin * normalDirection,
            u, v, packedLight, packedOverlay, packedColor
        );
    }

    private static float outlineExpansion(boolean outline) {
        return outline ? OUTLINE_EXPANSION : 0.0F;
    }

    private static float outlineY(float y, boolean outline) {
        if (!outline) {
            return y;
        }
        return y + Mth.lerp(-y / MODEL_HEIGHT, OUTLINE_EXPANSION, -OUTLINE_EXPANSION);
    }

    private static float clothDown(boolean dragging, int ring) {
        int splitRing = dragging ? DRAGGING_SPLIT_RING : FLOATING_SPLIT_RING;
        if (ring <= splitRing) {
            float upperProgress = ring / (float) splitRing;
            return Mth.lerp(upperProgress, 0.01F, dragging ? 0.5F : 1.0F / 3.0F);
        }
        if (!dragging) {
            float curveProgress = floatingCurveProgress(ring);
            return cubicBezier(curveProgress, 1.0F / 3.0F, 0.49F, 0.75F, 0.75F);
        }
        if (ring <= DRAGGING_GROUND_RING) {
            float curveProgress = draggingCurveProgress(ring);
            return cubicBezier(curveProgress, 0.5F, 0.72F, 1.0F, 1.0F);
        }
        return 1.0F;
    }

    private static float clothWidth(boolean dragging, int ring) {
        int splitRing = dragging ? DRAGGING_SPLIT_RING : FLOATING_SPLIT_RING;
        if (ring <= splitRing) {
            return 1.0F;
        }
        if (!dragging) {
            return Mth.lerp(smoothStep(floatingCurveProgress(ring)), 1.0F, 0.58F);
        }
        if (ring <= DRAGGING_GROUND_RING) {
            float splitProgress = smoothStep(draggingCurveProgress(ring));
            return Mth.lerp(splitProgress, 1.0F, 0.62F);
        }
        return Mth.lerp(smoothStep(draggingTrailProgress(ring)), 0.62F, 0.44F);
    }

    private static float clothLift(boolean dragging, int ring, float layerScale, float baseLift) {
        int splitRing = dragging ? DRAGGING_SPLIT_RING : FLOATING_SPLIT_RING;
        if (ring <= splitRing) {
            return baseLift;
        }
        if (!dragging) {
            return cubicBezier(floatingCurveProgress(ring), baseLift, baseLift, 3.80F, 7.50F);
        }
        float spread;
        if (ring <= DRAGGING_GROUND_RING) {
            spread = cubicBezier(draggingCurveProgress(ring), 0.0F, 0.0F, 3.80F, 6.86F);
        } else {
            spread = Mth.lerp(smoothStep(draggingTrailProgress(ring)), 6.86F, 11.86F);
        }
        return baseLift + spread * layerScale;
    }

    private static float clothTwist(boolean dragging, int panel, int ring) {
        int splitRing = dragging ? DRAGGING_SPLIT_RING : FLOATING_SPLIT_RING;
        if (ring <= splitRing) {
            return 0.0F;
        }
        float progress = clothMotionProgress(dragging, ring);
        float direction = (panel & 2) == 0 ? 1.0F : -1.0F;
        if (!dragging) {
            return direction * 0.22F * Mth.sin(progress * Mth.PI);
        }
        float curveTwist = direction * 0.18F * Mth.sin(Math.min(progress, 1.0F) * Mth.PI * 0.7F);
        if (ring <= DRAGGING_GROUND_RING) {
            return curveTwist;
        }
        return curveTwist + direction * 0.12F * smoothStep(draggingTrailProgress(ring));
    }

    private float clothFlutterLift(
        boolean dragging,
        int panel,
        int ring,
        float layerScale,
        float phaseOffset
    ) {
        int splitRing = dragging ? DRAGGING_SPLIT_RING : FLOATING_SPLIT_RING;
        if (ring <= splitRing || this.clothMovement <= 0.001F) {
            return 0.0F;
        }
        float progress = clothMotionProgress(dragging, ring);
        float primary = Mth.sin(
            this.clothWave * (1.25F + panel * 0.06F) + panel * 2.31F + progress * 1.9F + phaseOffset
        );
        float secondary = Mth.sin(
            this.clothWave * (2.05F + panel * 0.04F) + panel * 0.83F - progress * 2.7F + phaseOffset * 0.7F
        );
        return (primary * 0.42F + secondary * 0.18F) * progress * this.clothMovement * layerScale;
    }

    private float clothFlutterTwist(
        boolean dragging,
        int panel,
        int ring,
        float layerScale,
        float phaseOffset
    ) {
        int splitRing = dragging ? DRAGGING_SPLIT_RING : FLOATING_SPLIT_RING;
        if (ring <= splitRing || this.clothMovement <= 0.001F) {
            return 0.0F;
        }
        float progress = clothMotionProgress(dragging, ring);
        float primary = Mth.sin(
            this.clothWave * (1.12F + panel * 0.05F) + panel * 1.91F + progress * 2.5F + phaseOffset
        );
        float secondary = Mth.cos(
            this.clothWave * (0.73F + panel * 0.03F) + panel * 2.77F - progress * 1.8F + phaseOffset * 0.8F
        );
        return (primary * 0.065F + secondary * 0.03F) * progress * this.clothMovement * layerScale;
    }

    private float clothFlutterY(
        boolean dragging,
        int panel,
        int ring,
        float layerScale,
        float phaseOffset
    ) {
        int splitRing = dragging ? DRAGGING_SPLIT_RING : FLOATING_SPLIT_RING;
        if (ring <= splitRing
            || dragging && ring >= DRAGGING_GROUND_RING
            || this.clothMovement <= 0.001F) {
            return 0.0F;
        }
        float progress = clothMotionProgress(dragging, ring);
        float primary = Mth.sin(
            this.clothWave * (1.52F + panel * 0.05F) + panel * 1.41F + progress * 2.2F + phaseOffset
        );
        float secondary = Mth.cos(
            this.clothWave * (0.91F + panel * 0.04F) + panel * 2.63F + progress * 3.0F + phaseOffset * 0.6F
        );
        return (primary * 0.28F + secondary * 0.10F) * progress * this.clothMovement * layerScale;
    }

    private static float draggingCurveProgress(int ring) {
        return (ring - DRAGGING_SPLIT_RING) / (float)(DRAGGING_GROUND_RING - DRAGGING_SPLIT_RING);
    }

    private static float draggingTrailProgress(int ring) {
        return (ring - DRAGGING_GROUND_RING) / (float)(DRAGGING_RING_COUNT - 1 - DRAGGING_GROUND_RING);
    }

    private static float floatingCurveProgress(int ring) {
        return (ring - FLOATING_SPLIT_RING) / (float)(FLOATING_RING_COUNT - 1 - FLOATING_SPLIT_RING);
    }

    private static float clothMotionProgress(boolean dragging, int ring) {
        if (!dragging) {
            return smoothStep(floatingCurveProgress(ring));
        }
        if (ring <= DRAGGING_GROUND_RING) {
            return smoothStep(draggingCurveProgress(ring));
        }
        return 1.0F;
    }

    private static float clothLayerOffsetProgress(boolean dragging, int ring) {
        int splitRing = dragging ? DRAGGING_SPLIT_RING : FLOATING_SPLIT_RING;
        if (ring <= splitRing) {
            return 0.0F;
        }
        return clothMotionProgress(dragging, ring);
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float cubicBezier(float value, float start, float control1, float control2, float end) {
        float inverse = 1.0F - value;
        return inverse * inverse * inverse * start
            + 3.0F * inverse * inverse * value * control1
            + 3.0F * inverse * value * value * control2
            + value * value * value * end;
    }

    private static float radiusAt(float y) {
        for (int ring = 0; ring < PROFILE_Y.length - 1; ring++) {
            float lowerY = PROFILE_Y[ring];
            float upperY = PROFILE_Y[ring + 1];
            if (y <= lowerY && y >= upperY) {
                float progress = (y - lowerY) / (upperY - lowerY);
                return Mth.lerp(progress, PROFILE_RADIUS[ring], PROFILE_RADIUS[ring + 1]);
            }
        }
        return PROFILE_RADIUS[PROFILE_RADIUS.length - 1];
    }

    private static float profileSlope(int ring) {
        int previous = Math.max(0, ring - 1);
        int next = Math.min(PROFILE_Y.length - 1, ring + 1);
        return (PROFILE_RADIUS[next] - PROFILE_RADIUS[previous])
            / (PROFILE_Y[next] - PROFILE_Y[previous]);
    }

    private static void vertex(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        float x,
        float y,
        float z,
        float normalX,
        float normalY,
        float normalZ,
        float u,
        float v,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        consumer.addVertex(pose, x, y, z)
            .setColor(packedColor)
            .setUv(u, v)
            .setOverlay(packedOverlay)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
