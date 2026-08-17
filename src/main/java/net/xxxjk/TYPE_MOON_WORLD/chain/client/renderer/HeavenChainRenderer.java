package net.xxxjk.TYPE_MOON_WORLD.chain.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.client.model.HeavenChainModel;
import net.xxxjk.TYPE_MOON_WORLD.chain.config.ChainConfig;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.HeavenChainEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class HeavenChainRenderer extends GeoEntityRenderer<HeavenChainEntity> {
    private static final ResourceLocation TEXTURE = TYPE_MOON_WORLD.id("textures/entity/heaven_chain_head.png");
    private static final ResourceLocation TETHER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        "typemoonworld", "textures/entity/chains_of_heaven_silver.png"
    );

    public HeavenChainRenderer(EntityRendererProvider.Context context) {
        super(context, new HeavenChainModel());
        addRenderLayer(new GoldenHeadFlowLayer(this));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
        HeavenChainEntity chain,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        Entity owner = chain.clientOwner();
        if (owner instanceof LivingEntity living) {
            renderTexturedTether(chain, living, partialTick, poseStack, buffer, packedLight);
        }
        if (chain.hasEnumaFlow() && chain.state() != HeavenChainEntity.ChainState.BROKEN) {
            renderEnumaGateRings(chain, partialTick, poseStack, buffer);
        }
        super.render(chain, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderEnumaGateRings(
        HeavenChainEntity chain,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer
    ) {
        Vec3 chainPosition = chain.getPosition(partialTick);
        Matrix4f matrix = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        int ringSegments = chain.state() == HeavenChainEntity.ChainState.LATCHED ? 24 : 48;
        float time = chain.tickCount + partialTick;
        List<Vec3> gateOrigins = chain.enumaGateOrigins();
        VertexConsumer glow = buffer.getBuffer(RenderType.entityTranslucentEmissive(TETHER_TEXTURE));
        for (Vec3 gateOrigin : gateOrigins) {
            Vec3 center = gateOrigin.subtract(chainPosition).add(0.0D, 0.045D, 0.0D);
            renderRing(glow, matrix, pose, center, 1.48D, 0.88D, ringSegments, 255, 190, 18, 255);
        }

        VertexConsumer flow = buffer.getBuffer(
            RenderType.energySwirl(TETHER_TEXTURE, time * 0.018F % 1.0F, time * 0.011F % 1.0F)
        );
        for (Vec3 gateOrigin : gateOrigins) {
            Vec3 center = gateOrigin.subtract(chainPosition).add(0.0D, 0.045D, 0.0D);
            renderRing(flow, matrix, pose, center.add(0.0D, 0.008D, 0.0D),
                1.39D, 0.98D, ringSegments, 255, 238, 66, 255);
        }
    }

    private static void renderRing(
        VertexConsumer consumer,
        Matrix4f matrix,
        PoseStack.Pose pose,
        Vec3 center,
        double outerRadius,
        double innerRadius,
        int segments,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 down = new Vec3(0.0D, -1.0D, 0.0D);
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2.0D / segments;
            double a1 = (i + 1) * Math.PI * 2.0D / segments;
            Vec3 outer0 = center.add(Math.cos(a0) * outerRadius, 0.0D, Math.sin(a0) * outerRadius);
            Vec3 outer1 = center.add(Math.cos(a1) * outerRadius, 0.0D, Math.sin(a1) * outerRadius);
            Vec3 inner0 = center.add(Math.cos(a0) * innerRadius, 0.0D, Math.sin(a0) * innerRadius);
            Vec3 inner1 = center.add(Math.cos(a1) * innerRadius, 0.0D, Math.sin(a1) * innerRadius);
            float u0 = (float)i / segments * 4.0F;
            float u1 = (float)(i + 1) / segments * 4.0F;

            // Top and bottom faces keep the circle visible from either camera
            // elevation even when the active render type enables culling.
            addVertex(consumer, matrix, pose, outer0, u0, 1.0F, up, LightTexture.FULL_BRIGHT, red, green, blue, alpha);
            addVertex(consumer, matrix, pose, outer1, u1, 1.0F, up, LightTexture.FULL_BRIGHT, red, green, blue, alpha);
            addVertex(consumer, matrix, pose, inner1, u1, 0.0F, up, LightTexture.FULL_BRIGHT, red, green, blue, alpha);
            addVertex(consumer, matrix, pose, inner0, u0, 0.0F, up, LightTexture.FULL_BRIGHT, red, green, blue, alpha);

            addVertex(consumer, matrix, pose, outer0, u0, 1.0F, down, LightTexture.FULL_BRIGHT, red, green, blue, alpha);
            addVertex(consumer, matrix, pose, inner0, u0, 0.0F, down, LightTexture.FULL_BRIGHT, red, green, blue, alpha);
            addVertex(consumer, matrix, pose, inner1, u1, 0.0F, down, LightTexture.FULL_BRIGHT, red, green, blue, alpha);
            addVertex(consumer, matrix, pose, outer1, u1, 1.0F, down, LightTexture.FULL_BRIGHT, red, green, blue, alpha);
        }
    }

    private static void renderTexturedTether(
        HeavenChainEntity chain,
        LivingEntity owner,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        Vec3 chainPosition = chain.getPosition(partialTick);
        Matrix4f matrix = poseStack.last().pose();
        float yaw = net.minecraft.util.Mth.rotLerp(partialTick, chain.yRotO, chain.getYRot());
        float pitch = net.minecraft.util.Mth.lerp(partialTick, chain.xRotO, chain.getXRot());
        Vec3 headConnection = Vec3.directionFromRotation(pitch, yaw)
            .scale(-ChainConfig.CHAIN_HEAD_CONNECTION_OFFSET);
        List<Vec3> gateOrigins = chain.enumaGateOrigins();
        if (chain.isEnumaChain()
            && chain.state() == HeavenChainEntity.ChainState.LATCHED
            && gateOrigins.size() > 1) {
            renderMergedEnumaTethers(
                chain,
                chainPosition,
                headConnection,
                gateOrigins,
                partialTick,
                poseStack,
                buffer
            );
            return;
        }

        Vec3 tetherStart = chain.tetherOrigin(owner);
        List<Vec3> path = new ArrayList<>();
        path.add(tetherStart.subtract(chainPosition));
        chain.anchorPoints().stream().map(point -> point.subtract(chainPosition)).forEach(path::add);
        if (path.get(path.size() - 1).distanceToSqr(headConnection) > 1.0E-5D) {
            path.add(headConnection);
        }
        renderPath(
            buffer.getBuffer(RenderType.entityTranslucentEmissive(TETHER_TEXTURE)),
            matrix,
            poseStack.last(),
            path,
            chain.isEnumaChain() ? ChainConfig.ENUMA_RENDER_LINK_CAP : ChainConfig.RENDER_SEGMENT_CAP,
            255,
            255,
            255,
            255
        );
        if (chain.hasEnumaFlow()) {
            renderPath(
                buffer.getBuffer(RenderType.entityTranslucentEmissive(TETHER_TEXTURE)),
                matrix,
                poseStack.last(),
                path,
                ChainConfig.ENUMA_RENDER_LINK_CAP,
                255,
                206,
                42,
                128
            );
            float time = chain.tickCount + partialTick;
            RenderType flow = RenderType.energySwirl(
                TETHER_TEXTURE,
                time * 0.014F % 1.0F,
                time * 0.007F % 1.0F
            );
            renderPath(
                buffer.getBuffer(flow),
                matrix,
                poseStack.last(),
                path,
                ChainConfig.ENUMA_RENDER_LINK_CAP,
                255,
                220,
                72,
                255
            );
        }
    }

    private static void renderMergedEnumaTethers(
        HeavenChainEntity chain,
        Vec3 chainPosition,
        Vec3 headConnection,
        List<Vec3> gateOrigins,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer
    ) {
        long latchTick = chain.enumaLatchTick();
        double progress = latchTick <= 0L
            ? 1.0D
            : net.minecraft.util.Mth.clamp(
                (chain.level().getGameTime() - latchTick + partialTick) / ChainConfig.ENUMA_BIND_CONNECTION_TICKS,
                0.0D,
                1.0D
            );
        if (progress <= 0.0D) {
            return;
        }

        Matrix4f matrix = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        renderMergedEnumaLayer(
            buffer.getBuffer(RenderType.entityTranslucentEmissive(TETHER_TEXTURE)),
            matrix,
            pose,
            gateOrigins,
            chainPosition,
            headConnection,
            progress,
            255,
            255,
            255,
            255
        );
        renderMergedEnumaLayer(
            buffer.getBuffer(RenderType.entityTranslucentEmissive(TETHER_TEXTURE)),
            matrix,
            pose,
            gateOrigins,
            chainPosition,
            headConnection,
            progress,
            255,
            206,
            42,
            160
        );
        float time = chain.tickCount + partialTick;
        renderMergedEnumaLayer(
            buffer.getBuffer(RenderType.energySwirl(
                TETHER_TEXTURE,
                time * 0.014F % 1.0F,
                time * 0.007F % 1.0F
            )),
            matrix,
            pose,
            gateOrigins,
            chainPosition,
            headConnection,
            progress,
            255,
            220,
            72,
            255
        );
    }

    private static void renderMergedEnumaLayer(
        VertexConsumer consumer,
        Matrix4f matrix,
        PoseStack.Pose pose,
        List<Vec3> gateOrigins,
        Vec3 chainPosition,
        Vec3 headConnection,
        double progress,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        for (int gateIndex = 0; gateIndex < gateOrigins.size(); gateIndex++) {
            Vec3 start = gateOrigins.get(gateIndex).subtract(chainPosition).add(0.0D, 0.10D, 0.0D);
            Vec3 animatedEnd = start.lerp(headConnection, progress);
            renderLinkRun(
                consumer,
                matrix,
                pose,
                start,
                animatedEnd,
                gateIndex,
                ChainConfig.ENUMA_MERGED_LINKS_PER_TETHER,
                LightTexture.FULL_BRIGHT,
                red,
                green,
                blue,
                alpha
            );
        }
    }

    private static void renderPath(
        VertexConsumer consumer,
        Matrix4f matrix,
        PoseStack.Pose pose,
        List<Vec3> path,
        int linkCap,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        int renderedLinks = 0;
        for (int i = 1; i < path.size() && renderedLinks < linkCap; i++) {
            renderedLinks += renderLinkRun(
                consumer,
                matrix,
                pose,
                path.get(i - 1),
                path.get(i),
                renderedLinks,
                linkCap - renderedLinks,
                LightTexture.FULL_BRIGHT,
                red,
                green,
                blue,
                alpha
            );
        }
    }

    private static int renderLinkRun(
        VertexConsumer consumer,
        Matrix4f matrix,
        PoseStack.Pose pose,
        Vec3 from,
        Vec3 to,
        int firstLink,
        int limit,
        int packedLight,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        if (length < 0.05D || limit <= 0) {
            return 0;
        }
        Vec3 axis = span.scale(1.0D / length);
        Vec3 basisA = axis.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (basisA.lengthSqr() < 1.0E-5D) {
            basisA = axis.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        basisA = basisA.normalize();
        Vec3 basisB = axis.cross(basisA).normalize();
        int linkCount = Math.min(limit, Math.max(1, (int)Math.ceil(length / 0.26D)));
        for (int link = 0; link < linkCount; link++) {
            int globalLink = firstLink + link;
            double along = (link + 0.5D) / linkCount;
            Vec3 center = from.add(span.scale(along));
            Vec3 transverse = (globalLink & 1) == 0 ? basisA : basisB;
            renderLink(consumer, matrix, pose, center, axis, transverse, packedLight, red, green, blue, alpha);
        }
        return linkCount;
    }

    private static void renderLink(
        VertexConsumer consumer,
        Matrix4f matrix,
        PoseStack.Pose pose,
        Vec3 center,
        Vec3 axis,
        Vec3 transverse,
        int packedLight,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        Vec3 along = axis.scale(0.105D);
        Vec3 across = transverse.scale(0.045D);
        Vec3 normal = axis.cross(transverse).normalize();
        float minU = 1.0F / 16.0F;
        float maxU = 3.0F / 16.0F;
        float minV = 1.0F / 16.0F;
        float maxV = 2.0F / 16.0F;
        addVertex(consumer, matrix, pose, center.subtract(along).subtract(across), minU, maxV, normal, packedLight, red, green, blue, alpha);
        addVertex(consumer, matrix, pose, center.add(along).subtract(across), maxU, maxV, normal, packedLight, red, green, blue, alpha);
        addVertex(consumer, matrix, pose, center.add(along).add(across), maxU, minV, normal, packedLight, red, green, blue, alpha);
        addVertex(consumer, matrix, pose, center.subtract(along).add(across), minU, minV, normal, packedLight, red, green, blue, alpha);
    }

    private static void addVertex(
        VertexConsumer consumer,
        Matrix4f matrix,
        PoseStack.Pose pose,
        Vec3 point,
        float u,
        float v,
        Vec3 normal,
        int packedLight,
        int red,
        int green,
        int blue,
        int alpha
    ) {
        consumer.addVertex(matrix, (float)point.x, (float)point.y, (float)point.z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
    }

    @Override
    protected void applyRotations(
        HeavenChainEntity chain,
        PoseStack poseStack,
        float ageInTicks,
        float rotationYaw,
        float partialTick,
        float nativeScale
    ) {
        super.applyRotations(chain, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        float pitch = net.minecraft.util.Mth.lerp(partialTick, chain.xRotO, chain.getXRot());
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    @Override
    public ResourceLocation getTextureLocation(HeavenChainEntity entity) {
        return TEXTURE;
    }

    @Override
    public RenderType getRenderType(
        HeavenChainEntity entity,
        ResourceLocation texture,
        MultiBufferSource bufferSource,
        float partialTick
    ) {
        return RenderType.entityCutoutNoCull(texture);
    }
}

