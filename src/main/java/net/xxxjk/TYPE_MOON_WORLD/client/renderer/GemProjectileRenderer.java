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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.core.BlockPos;
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
    private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/end_gateway_beam.png");

    public GemProjectileRenderer(EntityRendererProvider.Context context, float r, float g, float b) {
        this(context, 1.0F, true, r, g, b);
    }

    public GemProjectileRenderer(EntityRendererProvider.Context context, float scale, boolean fullBright, float r, float g, float b) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.scale = scale;
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
        Level level = entity.level();
        List<Vec3> trace;
        if (entity instanceof RubyProjectileEntity ruby) trace = ruby.tracePos;
        else if (entity instanceof SapphireProjectileEntity sapphire) trace = sapphire.tracePos;
        else if (entity instanceof TopazProjectileEntity topaz) trace = topaz.tracePos;
        else return;

        if (trace.size() < 2) return;

        if (entity instanceof RubyProjectileEntity ruby && ruby.getGemType() == 99) {
            NineLivesSlashRenderer.renderVisualSlash(entity, partialTicks, poseStack, buffer);
            return;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png")));
        
        // Determine color based on entity type
        float r = 1.0f, g = 1.0f, b = 1.0f;
        
        if (entity instanceof RubyProjectileEntity ruby) {
             int type = ruby.getGemType();
             if (type == 0) { // Ruby - Red
                 r = 1.0f; g = 0.2f; b = 0.2f;
             } else if (type == 1) { // Sapphire - Blue
                 r = 0.2f; g = 0.2f; b = 1.0f;
             } else if (type == 2) { // Emerald - Green
                 r = 0.2f; g = 1.0f; b = 0.2f;
             } else if (type == 3) { // Topaz - Yellow
                 r = 1.0f; g = 1.0f; b = 0.2f;
             } else if (type == 4) { // Cyan
                 r = 0.0f; g = 1.0f; b = 1.0f;
             } else if (type == 5) { // White
                 r = 1.0f; g = 1.0f; b = 1.0f;
             } else if (type == 6) { // Black
                 r = 0.15f; g = 0.15f; b = 0.15f;
             } else if (type == 99) { // Visual Slash (Nine Lives)
                 r = 1.0f; g = 1.0f; b = 1.0f;
             } else {
                 r = 1.0f; g = 1.0f; b = 1.0f;
             }
        } else if (entity instanceof SapphireProjectileEntity) {
             r = 0.2f; g = 0.2f; b = 1.0f;
        } else if (entity instanceof TopazProjectileEntity) {
             r = 1.0f; g = 1.0f; b = 0.2f;
        }

        Vec3 currentPos = entity.getPosition(partialTicks);

        Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();

        // Create a full list of control points including current interpolated pos
        List<Vec3> points = new ArrayList<>(trace);
        // Only render if we have enough points for spline
        if (points.size() < 2) return;
        
        points.add(currentPos);
        
        poseStack.pushPose();
        // Render relative to the entity's current position to avoid precision issues
        // The poseStack is already translated to (EntityPos - CameraPos)
        // We want to draw points P. 
        // If we use world coordinates P_world, we need to translate by (-EntityPos_world) to get P_local.
        // P_local = P_world - EntityPos_world
        // Then vertex = P_local
        
        // Note: Do NOT translate poseStack by -currentPos here, as poseStack is already relative to camera.
        // Instead, we will subtract currentPos from the points manually.
        
        PoseStack.Pose pose = poseStack.last();
        float baseWidth = 0.3f;  
        int samplesPerSegment = 5; 
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

                // Calculate local positions relative to the entity
            Vec3 startLocal = start.subtract(currentPos);
            Vec3 endLocal = end.subtract(currentPos);

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

                float r1 = r, g1 = g, b1 = b;
                float r2 = r, g2 = g, b2 = b;

                // Random mode logic removed to prevent rainbow. We want specific color of the gem.
                // The GemType is now correctly set to the ACTUAL gem thrown (e.g. Red for Ruby).
                // So 'r', 'g', 'b' variables are already set correctly above in renderTrail.
                // However, for pure WHITE_GEMSTONE (type 4), we want NO special effects (White trail).
                // If isRandom is true (meaning Type 4 White Gemstone), it will be white.
                // If MagicRubyThrow sets type 0 (Ruby) even in Random mode, then isRandom is false, and it uses Red.
                
                // Wait, if MagicRubyThrow logic sets type to 0/1/2/3, then isRandom will be FALSE.
                // So we get colored trails.
                // If it sets type to 4 (White Gemstone), isRandom is TRUE.
                // And for type 4 we want NO special effects (White trail).
                // So we actually DON'T need any rainbow logic here.
                
                // Removing the rainbow block I just added? Or did I misunderstand "Random throw white gem can also throw"?
                // "Random throw white gem can also throw" -> MagicRubyThrow logic updated to include White Gemstone.
                // "Random throw... what gem throws what color trail" -> Done by MagicRubyThrow setting correct ID.
                // "No effects" -> Probably means no particle effects? That's in RubyProjectileEntity.tick().
                
                // So here in Renderer, we just render the color.
                // If type is 4 (White), r/g/b is 1.0 (White).
                // If type is 0 (Ruby), r/g/b is Red.
                // So we should NOT add rainbow override.

                drawBillboardSegment(level, pose, vertexConsumer, startLocal, endLocal, camPos.subtract(currentPos), width1, width2, alpha1, alpha2, u1, u2, r1, g1, b1, r2, g2, b2);
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

    private float getAlpha(float progress) {
        return 0.2f + 0.3f * progress * progress;
    }

    private float getVisualWidth(float base, float progress) {
        float s = Mth.sin((float)Math.PI * progress);
        return base * (0.4f + 0.6f * s);
    }

    private float getVisualAlpha(float progress) {
        float s = Mth.sin((float)Math.PI * progress);
        return 0.6f + 0.4f * s;
    }

    private void drawBillboardSegment(Level level, PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 viewOffset, float width1, float width2, float alpha1, float alpha2, float u1, float u2, float r1, float g1, float b1, float r2, float g2, float b2) {
        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 1.0E-6) return;
        
        // viewOffset is (CameraPos - EntityPos) technically, but we passed (CameraPos - EntityPos) which is -RelativePos?
        // Wait, we passed: camPos.subtract(currentPos)
        // start is Local (Point - EntityPos)
        // We need viewDir = Point - CameraPos
        // Point - CameraPos = (Point - EntityPos) - (CameraPos - EntityPos) = start - viewOffset
        
        Vec3 viewDir = start.subtract(viewOffset);
        
        // Prevent NaN if viewDir and dir are parallel
        Vec3 cross = dir.cross(viewDir);
        if (cross.lengthSqr() < 1.0E-6) {
             // Fallback: use UP vector or just skip
             return;
        }
        Vec3 right = cross.normalize();
        
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
        
        int lightStart = LightTexture.FULL_BRIGHT;
        int lightEnd = LightTexture.FULL_BRIGHT;
        
        vertex(pose, consumer, v0, r1, g1, b1, alpha1, u1, 0, lightStart);
        vertex(pose, consumer, v2, r2, g2, b2, alpha2, u2, 0, lightEnd);
        vertex(pose, consumer, v3, r2, g2, b2, alpha2, u2, 1, lightEnd);
        vertex(pose, consumer, v1, r1, g1, b1, alpha1, u1, 1, lightStart);
    }

    private void vertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 pos, float r, float g, float b, float a, float u, float v, int packedLight) {
        consumer.addVertex(pose, (float)pos.x, (float)pos.y, (float)pos.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0, 1, 0);
    }
    
    private int packedLight(Level level, Vec3 pos) {
        BlockPos bp = BlockPos.containing(pos);
        int block = level.getBrightness(LightLayer.BLOCK, bp);
        int sky = level.getBrightness(LightLayer.SKY, bp);
        return LightTexture.pack(block, sky);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TRAIL_TEXTURE;
    }
}
