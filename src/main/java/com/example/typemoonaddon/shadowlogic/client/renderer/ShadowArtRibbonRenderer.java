package com.example.typemoonaddon.shadowlogic.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.entity.SakuraShadowArtRibbonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Renders the source Blockbench plane as a continuous, smooth ribbon along synchronized action curves. */
public final class ShadowArtRibbonRenderer extends EntityRenderer<SakuraShadowArtRibbonEntity> {
    private static final ResourceLocation RIBBON_TEXTURE = TypeMoonAddon.id("textures/entity/shadow_art_ribbon.png");
    private static final int MIN_CURVE_SEGMENTS = 12;
    private static final int MAX_CURVE_SEGMENTS = 256;

    public ShadowArtRibbonRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override
    public void render(
        SakuraShadowArtRibbonEntity ribbon,
        float yaw,
        float partialTick,
        PoseStack pose,
        MultiBufferSource buffers,
        int light
    ) {
        Entity owner = owner(ribbon);
        if (owner == null) return;

        Vec3 ownerOffset = owner.getPosition(partialTick).subtract(ribbon.getPosition(partialTick));
        int index = ribbon.ribbonIndex();
        double rootAngle = Math.PI * 2.0D * index / GameplayConfig.SHADOW_ART_RIBBON_COUNT;
        Vec3 root = ownerOffset.add(
            Math.cos(rootAngle) * GameplayConfig.SHADOW_ART_ROOT_RADIUS,
            0.12D,
            Math.sin(rootAngle) * GameplayConfig.SHADOW_ART_ROOT_RADIUS
        );
        Vec3 tip = Vec3.ZERO;
        Curve curve = actionCurve(ribbon, root, tip, partialTick);
        VertexConsumer consumer = ShadowRibbonPlaneGeometry.consumer(buffers);
        byte palette = owner.getUUID().getLeastSignificantBits() % 3L == 0L ? (byte) 1 : 0;
        float animation = (ribbon.tickCount + partialTick) * 0.025F;
        int surface = ShadowRibbonRenderStyle.surface(palette, animation);
        int curveSegments = Mth.clamp(
            (int) Math.ceil(curveLength(curve) / ShadowRibbonPlaneGeometry.MODEL_LENGTH),
            MIN_CURVE_SEGMENTS,
            MAX_CURVE_SEGMENTS
        );
        Vec3 previousLeft = null;
        Vec3 previousRight = null;
        for (int segment = 0; segment <= curveSegments; segment++) {
            double t = segment / (double) curveSegments;
            Vec3 center = bezier(curve.start(), curve.controlA(), curve.controlB(), curve.end(), t);
            Vec3 tangent = bezierTangent(curve.start(), curve.controlA(), curve.controlB(), curve.end(), t);
            Vec3 facing = center.subtract(ownerOffset);
            if (facing.lengthSqr() < 1.0E-4D) facing = new Vec3(Math.cos(rootAngle), 0, Math.sin(rootAngle));
            Vec3 widthAxis = tangent.cross(facing.normalize());
            if (widthAxis.lengthSqr() < 1.0E-4D) widthAxis = tangent.cross(new Vec3(0, 1, 0));
            if (widthAxis.lengthSqr() < 1.0E-4D) widthAxis = new Vec3(1, 0, 0);
            widthAxis = widthAxis.normalize();

            double width = sourceModelWidth(ribbon.action(), t);
            Vec3 left = center.add(widthAxis.scale(width * 0.5D));
            Vec3 right = center.subtract(widthAxis.scale(width * 0.5D));
            if (previousLeft != null) {
                ShadowRibbonPlaneGeometry.renderSegment(
                    consumer, pose.last(), previousLeft, previousRight, right, left,
                    surface
                );
            }
            previousLeft = left;
            previousRight = right;
        }
        super.render(ribbon, yaw, partialTick, pose, buffers, light);
    }

    private static Curve actionCurve(SakuraShadowArtRibbonEntity ribbon, Vec3 root, Vec3 tip, float partialTick) {
        int index = ribbon.ribbonIndex();
        double seed = index * 1.731D + ribbon.getId() * 0.017D;
        double length = Math.max(0.1D, root.distanceTo(tip));
        Vec3 direction = tip.subtract(root).normalize();
        Vec3 lateral = new Vec3(-direction.z, 0, direction.x);
        if (lateral.lengthSqr() < 1.0E-4D) lateral = new Vec3(1, 0, 0);
        lateral = lateral.normalize();
        double age = ribbon.tickCount + partialTick;

        return switch (ribbon.action()) {
            case SakuraShadowArtRibbonEntity.ATTACK -> {
                double snap = Mth.clamp((age - ribbon.actionStart()) / GameplayConfig.SHADOW_ART_STRIKE_TICKS, 0.0D, 1.0D);
                yield new Curve(
                    root,
                    root.add(lateral.scale(Math.sin(seed) * Math.min(1.4D, length * 0.12D))).add(0, 1.8D - snap, 0),
                    tip.subtract(direction.scale(Math.min(2.5D, length * 0.18D))).add(lateral.scale(Math.cos(seed) * 0.35D)),
                    tip
                );
            }
            case SakuraShadowArtRibbonEntity.LIFT -> new Curve(
                root,
                root.add(lateral.scale((index % 2 == 0 ? 1 : -1) * Math.min(2.4D, length * 0.2D))).add(0, 2.2D, 0),
                tip.subtract(direction.scale(Math.min(2.0D, length * 0.15D))).add(0, -0.65D, 0),
                tip
            );
            case SakuraShadowArtRibbonEntity.THROW -> {
                double sweep = Mth.clamp((age - ribbon.actionStart()) / GameplayConfig.SHADOW_ART_THROW_TICKS, 0.0D, 1.0D);
                double side = index % 2 == 0 ? 1.0D : -1.0D;
                yield new Curve(
                    root,
                    root.add(lateral.scale(side * (2.6D + sweep * 1.5D))).add(0, 2.6D + Math.sin(sweep * Math.PI) * 1.6D, 0),
                    tip.subtract(direction.scale(Math.min(2.8D, length * 0.2D))).add(lateral.scale(-side * 0.8D)).add(0, 1.2D, 0),
                    tip
                );
            }
            case SakuraShadowArtRibbonEntity.PINNED, SakuraShadowArtRibbonEntity.PIERCED -> {
                double side = index % 2 == 0 ? 1.0D : -1.0D;
                double level = (index % 5 - 2) * 0.32D;
                yield new Curve(
                    root,
                    root.add(lateral.scale(side * Math.min(3.0D, length * 0.24D))).add(0, 2.0D + level, 0),
                    tip.subtract(direction.scale(Math.min(2.2D, length * 0.16D))).add(lateral.scale(-side * 0.65D)).add(0, level, 0),
                    tip
                );
            }
            case SakuraShadowArtRibbonEntity.DEFEND -> new Curve(
                root,
                root.add(lateral.scale(Math.sin(seed) * 1.8D)).add(0, 2.8D, 0),
                tip.subtract(direction.scale(Math.min(1.5D, length * 0.15D))).add(0, 1.2D, 0),
                tip
            );
            case SakuraShadowArtRibbonEntity.RETRACT -> new Curve(
                root,
                root.add(lateral.scale(Math.sin(seed + age * 0.2D) * 0.55D)).add(0, 0.5D, 0),
                tip.add(0, 0.2D, 0),
                tip
            );
            default -> {
                double slowA = Math.sin(age * (0.021D + index * 0.0007D) + seed);
                double slowB = Math.sin(age * 0.009D + seed * 2.3D);
                double slowC = Math.cos(age * 0.013D + seed * 0.61D);
                yield new Curve(
                    root,
                    root.add(lateral.scale((slowA * 1.5D + slowB * 0.8D) * Math.min(1.0D, length * 0.2D))).add(0, 1.2D + slowC * 0.65D, 0),
                    tip.subtract(direction.scale(Math.min(2.0D, length * 0.2D))).add(lateral.scale((slowB - slowC) * 0.6D)).add(0, 0.45D + slowA * 0.35D, 0),
                    tip
                );
            }
        };
    }

    private static double sourceModelWidth(byte action, double progress) {
        double base = ShadowRibbonPlaneGeometry.MODEL_WIDTH;
        if (action == SakuraShadowArtRibbonEntity.ATTACK && progress > 0.82D) return base * (1.0D - (progress - 0.82D) * 2.1D);
        return base * (0.96D + Math.sin(progress * Math.PI) * 0.12D);
    }

    private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double inverse = 1.0D - t;
        return p0.scale(inverse * inverse * inverse)
            .add(p1.scale(3.0D * inverse * inverse * t))
            .add(p2.scale(3.0D * inverse * t * t))
            .add(p3.scale(t * t * t));
    }

    private static Vec3 bezierTangent(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double inverse = 1.0D - t;
        Vec3 tangent = p1.subtract(p0).scale(3.0D * inverse * inverse)
            .add(p2.subtract(p1).scale(6.0D * inverse * t))
            .add(p3.subtract(p2).scale(3.0D * t * t));
        return tangent.lengthSqr() < 1.0E-5D ? p3.subtract(p0).normalize() : tangent.normalize();
    }

    private static double curveLength(Curve curve) {
        Vec3 previous = curve.start();
        double length = 0.0D;
        for (int sample = 1; sample <= 24; sample++) {
            Vec3 point = bezier(curve.start(), curve.controlA(), curve.controlB(), curve.end(), sample / 24.0D);
            length += previous.distanceTo(point);
            previous = point;
        }
        return length;
    }

    private static Entity owner(SakuraShadowArtRibbonEntity ribbon) {
        Entity owner = ribbon.ownerEntityId() < 0 ? null : ribbon.level().getEntity(ribbon.ownerEntityId());
        return owner != null && ribbon.ownerId() != null && ribbon.ownerId().equals(owner.getUUID()) ? owner : null;
    }

    @Override
    public ResourceLocation getTextureLocation(SakuraShadowArtRibbonEntity entity) {
        return RIBBON_TEXTURE;
    }

    private record Curve(Vec3 start, Vec3 controlA, Vec3 controlB, Vec3 end) {}
}
