package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TopazProjectileEntity;

import java.util.ArrayList;
import java.util.List;

public class GemProjectileRenderer<T extends ThrowableItemProjectile> extends EntityRenderer<T> {
    private final ItemRenderer itemRenderer;
    private final float scale;
    private final boolean fullBright;
    private final float r, g, b;
    private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/end_gateway_beam.png");

    public GemProjectileRenderer(EntityRendererProvider.Context context, float r, float g, float b) {
        this(context, 1.0F, true, r, g, b);
    }

    public GemProjectileRenderer(EntityRendererProvider.Context context, float scale, boolean fullBright, float r, float g, float b) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.scale = scale;
        this.fullBright = fullBright;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(this.scale, this.scale, this.scale);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();

        renderTrail(entity, partialTicks, poseStack, buffer);

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderTrail(T entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        List<Vec3> trace;
        if (entity instanceof RubyProjectileEntity ruby) trace = ruby.tracePos;
        else if (entity instanceof SapphireProjectileEntity sapphire) trace = sapphire.tracePos;
        else if (entity instanceof TopazProjectileEntity topaz) trace = topaz.tracePos;
        else return;

        if (trace.size() < 2) return;

        // Use a glowing translucent render type
        // Use beacon_beam instead of custom texture to verify visibility
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png")));
        
        Vec3 currentPos = entity.getPosition(partialTicks);
        Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();

        // Create a full list of control points including current interpolated pos
        List<Vec3> points = new ArrayList<>(trace);
        // Only render if we have enough points for spline
        if (points.size() < 2) return;
        
        points.add(currentPos);
        
        poseStack.pushPose();
        // Translate to the entity's current position to render relative to it
        // This puts us in World Space (relative to camera)
        poseStack.translate(-currentPos.x, -currentPos.y, -currentPos.z);
        
        PoseStack.Pose pose = poseStack.last();
        float baseWidth = 0.3f; // Increased width as requested
        int samplesPerSegment = 5; // Higher quality spline
        // Dynamic texture repeat based on trail length to maintain consistent density
        // 16.0f points per repeat (approx 1 repeat per 0.8 seconds of trail)
        float textureRepeat = Math.max(1.0f, points.size() / 16.0f);

        for (int i = 0; i < points.size() - 1; i++) {
            // Catmull-Rom Control Points
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);
            
            // Extrapolate endpoints
            Vec3 p0 = i > 0 ? points.get(i - 1) : p1.subtract(p2.subtract(p1));
            Vec3 p3 = i < points.size() - 2 ? points.get(i + 2) : p2.add(p2.subtract(p1));

            for (int j = 0; j < samplesPerSegment; j++) {
                float t1 = (float) j / samplesPerSegment;
                float t2 = (float) (j + 1) / samplesPerSegment;

                Vec3 start = catmullRom(t1, p0, p1, p2, p3);
                Vec3 end = catmullRom(t2, p0, p1, p2, p3);

                // Global progress for width/alpha (0.0 = tail, 1.0 = head)
                float totalPoints = (points.size() - 1) * samplesPerSegment;
                float globalIndex1 = i * samplesPerSegment + j;
                float globalIndex2 = globalIndex1 + 1;
                
                float progress1 = globalIndex1 / totalPoints;
                float progress2 = globalIndex2 / totalPoints;

                float width1 = getWidth(baseWidth, progress1);
                float width2 = getWidth(baseWidth, progress2);
                float alpha1 = getAlpha(progress1);
                float alpha2 = getAlpha(progress2);
                
                // UV mapping: Tile based on progress
                // U goes from 0 to textureRepeat
                float u1 = progress1 * textureRepeat;
                float u2 = progress2 * textureRepeat;

                drawBillboardSegment(pose, vertexConsumer, start, end, camPos, width1, width2, alpha1, alpha2, u1, u2);
            }
        }
        
        poseStack.popPose();
    }
    
    // Spline Interpolation Function
    private Vec3 catmullRom(float t, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        float t2 = t * t;
        float t3 = t2 * t;
        
        double x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        double y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        double z = 0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);
        
        return new Vec3(x, y, z);
    }

    // Width Function: Parabolic-like taper
    private float getWidth(float base, float progress) {
        return base * (0.2f + 0.8f * (float)Math.sqrt(progress));
    }

    // Alpha Function: Quadratic fade
    private float getAlpha(float progress) {
        return progress * progress;
    }

    private void drawBillboardSegment(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 camPos, float width1, float width2, float alpha1, float alpha2, float u1, float u2) {
        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 0.000001) return;
        
        Vec3 viewDir = start.subtract(camPos);
        Vec3 right = dir.cross(viewDir).normalize();
        
        Vec3 offset1 = right.scale(width1 * 0.5);
        Vec3 offset2 = right.scale(width2 * 0.5);

        // V1 --- V3
        // |      |
        // V0 --- V2

        // Tail (start)
        Vec3 v0 = start.subtract(offset1);
        Vec3 v1 = start.add(offset1);
        
        // Head (end)
        Vec3 v2 = end.subtract(offset2);
        Vec3 v3 = end.add(offset2);

        // Draw Quad
        // U coordinates: Tiled based on input
        // V coordinates: 0 to 1 across the width
        
        vertex(pose, consumer, v0, r, g, b, alpha1, u1, 0);
        vertex(pose, consumer, v2, r, g, b, alpha2, u2, 0);
        vertex(pose, consumer, v3, r, g, b, alpha2, u2, 1);
        vertex(pose, consumer, v1, r, g, b, alpha1, u1, 1);
    }

    private void vertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 pos, float r, float g, float b, float a, float u, float v) {
        consumer.addVertex(pose, (float)pos.x, (float)pos.y, (float)pos.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880) // Full bright
                .setNormal(pose, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TRAIL_TEXTURE;
    }
}
