package com.example.typemoonaddon.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Renders a dim vanilla End Portal sphere with a dense, bright star field in front. */
final class StarrySkyboxRenderer {
    private static final RenderType END_PORTAL_RENDER_TYPE = RenderType.endPortal();
    private static final RenderType PORTAL_DIMMER_RENDER_TYPE = RenderType.debugQuads();
    private static final RenderType STAR_RENDER_TYPE = RenderType.lines();
    private static final int LATITUDE_SEGMENTS = 20;
    private static final int LONGITUDE_SEGMENTS = 48;
    private static final int STAR_COUNT = 1_400;

    private StarrySkyboxRenderer() {
    }

    static void render(Minecraft minecraft, RenderLevelStageEvent event, float radius) {
        if (minecraft.level == null) {
            return;
        }

        float time = event.getRenderTick() + event.getPartialTick().getGameTimeDeltaTicks();
        Matrix4f modelView = event.getModelViewMatrix();
        BufferSource buffers = minecraft.renderBuffers().bufferSource();

        // The vanilla portal shader supplies the slow, almost-black animated backdrop.
        VertexConsumer portal = buffers.getBuffer(END_PORTAL_RENDER_TYPE);
        renderPortalSphere(portal, modelView, radius);
        buffers.endBatch(END_PORTAL_RENDER_TYPE);

        // A black veil keeps the portal texture at a very low brightness without changing the shader.
        VertexConsumer dimmer = buffers.getBuffer(PORTAL_DIMMER_RENDER_TYPE);
        renderPortalDimmer(dimmer, modelView, radius);
        buffers.endBatch(PORTAL_DIMMER_RENDER_TYPE);

        VertexConsumer stars = buffers.getBuffer(STAR_RENDER_TYPE);
        renderStars(stars, modelView, radius, time);
        renderShootingStars(stars, modelView, radius, time);
        buffers.endBatch(STAR_RENDER_TYPE);
    }

    private static void renderPortalSphere(VertexConsumer consumer, Matrix4f modelView, float radius) {
        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            double theta0 = Math.PI * latitude / LATITUDE_SEGMENTS;
            double theta1 = Math.PI * (latitude + 1) / LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                double phi0 = Math.PI * 2.0D * longitude / LONGITUDE_SEGMENTS;
                double phi1 = Math.PI * 2.0D * (longitude + 1) / LONGITUDE_SEGMENTS;
                portalQuad(consumer, modelView,
                        spherePoint(theta0, phi0, radius),
                        spherePoint(theta0, phi1, radius),
                        spherePoint(theta1, phi1, radius),
                        spherePoint(theta1, phi0, radius));
            }
        }
    }

    private static void renderPortalDimmer(VertexConsumer consumer, Matrix4f modelView, float radius) {
        int dimmer = rgba(0, 0, 3, 238);
        float overlayRadius = radius * 0.998F;
        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            double theta0 = Math.PI * latitude / LATITUDE_SEGMENTS;
            double theta1 = Math.PI * (latitude + 1) / LATITUDE_SEGMENTS;
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                double phi0 = Math.PI * 2.0D * longitude / LONGITUDE_SEGMENTS;
                double phi1 = Math.PI * 2.0D * (longitude + 1) / LONGITUDE_SEGMENTS;
                float[] p0 = spherePoint(theta0, phi0, overlayRadius);
                float[] p1 = spherePoint(theta0, phi1, overlayRadius);
                float[] p2 = spherePoint(theta1, phi1, overlayRadius);
                float[] p3 = spherePoint(theta1, phi0, overlayRadius);
                colorQuad(consumer, modelView, p0, p1, p2, p3, dimmer);
            }
        }
    }

    private static void renderStars(VertexConsumer consumer, Matrix4f modelView, float radius, float time) {
        float starRadius = radius * 0.968F;
        for (int index = 0; index < STAR_COUNT; index++) {
            double seed = index * 31.719D + 17.37D;
            float[] direction = randomDirection(seed);
            float galacticBand = Math.abs(direction[0] * 0.22F + direction[1] * 0.83F - direction[2] * 0.51F);
            boolean denseBand = galacticBand < 0.24F;
            float density = denseBand ? 0.94F : 0.70F;
            if (fract(Math.sin(seed * 2.17D) * 61421.37D) > density) {
                continue;
            }

            float size = (denseBand ? 0.052F : 0.042F) + (index % 7) * 0.013F;
            float twinkle = (float) Math.sin(time * (0.007D + (index % 7) * 0.0015D) + seed);
            int alpha = clamp((int) (178.0F + twinkle * 58.0F + (denseBand ? 18.0F : 0.0F)), 105, 255);
            int color = starColor(index, alpha);
            float[] tangent = perpendicular(direction, 0.37F + index * 0.17F);
            float[] bitangent = cross(direction, tangent);
            float x = direction[0] * starRadius;
            float y = direction[1] * starRadius;
            float z = direction[2] * starRadius;
            line(consumer, modelView,
                    x - tangent[0] * size, y - tangent[1] * size, z - tangent[2] * size,
                    x + tangent[0] * size, y + tangent[1] * size, z + tangent[2] * size, color);
            if (index % 3 == 0) {
                line(consumer, modelView,
                        x - bitangent[0] * size * 0.82F, y - bitangent[1] * size * 0.82F,
                        z - bitangent[2] * size * 0.82F,
                        x + bitangent[0] * size * 0.82F, y + bitangent[1] * size * 0.82F,
                        z + bitangent[2] * size * 0.82F,
                        rgba(Math.min(255, ((color >>> 24) & 255) + 16),
                                Math.min(255, ((color >>> 16) & 255) + 16),
                                Math.min(255, ((color >>> 8) & 255) + 16),
                                Math.max(80, alpha - 10)));
            }
        }
    }

    private static void renderShootingStars(VertexConsumer consumer, Matrix4f modelView, float radius, float time) {
        int cycle = (int) Math.floor(time / 120.0F);
        float phase = (time - cycle * 120.0F) / 20.0F;
        if (phase < 0.0F || phase > 1.0F) {
            return;
        }
        float[] direction = randomDirection(cycle * 4.17D + 2.2D);
        float[] tangent = perpendicular(direction, cycle * 0.31D);
        float[] headDirection = normalize(new float[]{direction[0] + tangent[0] * (phase - 0.5F) * 0.55F,
                direction[1] + tangent[1] * (phase - 0.5F) * 0.55F,
                direction[2] + tangent[2] * (phase - 0.5F) * 0.55F});
        float[] tailDirection = normalize(new float[]{direction[0] + tangent[0] * (phase - 0.62F) * 0.55F,
                direction[1] + tangent[1] * (phase - 0.62F) * 0.55F,
                direction[2] + tangent[2] * (phase - 0.62F) * 0.55F});
        int alpha = clamp((int) (245.0F * (1.0F - Math.abs(phase - 0.5F) * 1.65F)), 0, 245);
        line(consumer, modelView,
                tailDirection[0] * radius * 0.95F, tailDirection[1] * radius * 0.95F, tailDirection[2] * radius * 0.95F,
                headDirection[0] * radius * 0.95F, headDirection[1] * radius * 0.95F, headDirection[2] * radius * 0.95F,
                rgba(242, 248, 255, alpha));
    }

    private static float[] spherePoint(double theta, double phi, float radius) {
        float sinTheta = (float) Math.sin(theta);
        return new float[]{
                (float) (Math.cos(phi) * sinTheta * radius),
                (float) (Math.cos(theta) * radius),
                (float) (Math.sin(phi) * sinTheta * radius)
        };
    }

    private static float[] randomDirection(double seed) {
        double u = fract(Math.sin(seed * 1.37D) * 43758.5453D) * 2.0D - 1.0D;
        double phi = fract(Math.sin(seed * 2.71D) * 24634.6345D) * Math.PI * 2.0D;
        double scale = Math.sqrt(Math.max(0.0D, 1.0D - u * u));
        return new float[]{(float) (Math.cos(phi) * scale), (float) u, (float) (Math.sin(phi) * scale)};
    }

    private static float[] perpendicular(float[] direction, double seed) {
        float[] reference = Math.abs(direction[1]) < 0.85F
                ? new float[]{0.0F, 1.0F, 0.0F}
                : new float[]{1.0F, 0.0F, 0.0F};
        float[] tangent = normalize(cross(reference, direction));
        float angle = (float) (fract(seed * 0.137D) * Math.PI * 2.0D);
        float[] bitangent = cross(direction, tangent);
        return normalize(new float[]{tangent[0] * (float) Math.cos(angle) + bitangent[0] * (float) Math.sin(angle),
                tangent[1] * (float) Math.cos(angle) + bitangent[1] * (float) Math.sin(angle),
                tangent[2] * (float) Math.cos(angle) + bitangent[2] * (float) Math.sin(angle)});
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static float[] normalize(float[] value) {
        float length = (float) Math.sqrt(value[0] * value[0] + value[1] * value[1] + value[2] * value[2]);
        if (length < 0.0001F) {
            return new float[]{0.0F, 1.0F, 0.0F};
        }
        return new float[]{value[0] / length, value[1] / length, value[2] / length};
    }

    private static void portalQuad(VertexConsumer consumer, Matrix4f modelView,
                                   float[] p0, float[] p1, float[] p2, float[] p3) {
        consumer.addVertex(modelView, p0[0], p0[1], p0[2]);
        consumer.addVertex(modelView, p1[0], p1[1], p1[2]);
        consumer.addVertex(modelView, p2[0], p2[1], p2[2]);
        consumer.addVertex(modelView, p3[0], p3[1], p3[2]);
    }

    private static void colorQuad(VertexConsumer consumer, Matrix4f modelView,
                                  float[] p0, float[] p1, float[] p2, float[] p3, int color) {
        colorVertex(consumer, modelView, p0[0], p0[1], p0[2], color);
        colorVertex(consumer, modelView, p1[0], p1[1], p1[2], color);
        colorVertex(consumer, modelView, p2[0], p2[1], p2[2], color);
        colorVertex(consumer, modelView, p3[0], p3[1], p3[2], color);
    }

    private static void colorVertex(VertexConsumer consumer, Matrix4f modelView,
                                    float x, float y, float z, int color) {
        consumer.addVertex(modelView, x, y, z).setColor(
                ((color >>> 24) & 255) / 255.0F,
                ((color >>> 16) & 255) / 255.0F,
                ((color >>> 8) & 255) / 255.0F,
                (color & 255) / 255.0F);
    }

    private static void line(VertexConsumer consumer, Matrix4f modelView,
                             float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        float red = ((color >>> 24) & 255) / 255.0F;
        float green = ((color >>> 16) & 255) / 255.0F;
        float blue = ((color >>> 8) & 255) / 255.0F;
        float alpha = (color & 255) / 255.0F;
        consumer.addVertex(modelView, x1, y1, z1).setColor(red, green, blue, alpha)
                .setNormal(0.0F, 1.0F, 0.0F);
        consumer.addVertex(modelView, x2, y2, z2).setColor(red, green, blue, alpha)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static int starColor(int index, int alpha) {
        return switch (index % 8) {
            case 0, 1, 2 -> rgba(245, 249, 255, alpha);
            case 3 -> rgba(255, 250, 224, alpha);
            case 4 -> rgba(205, 229, 255, alpha);
            case 5 -> rgba(224, 215, 255, alpha);
            case 6 -> rgba(255, 231, 193, alpha);
            default -> rgba(235, 239, 255, alpha);
        };
    }

    private static int rgba(int red, int green, int blue, int alpha) {
        return (red & 255) << 24 | (green & 255) << 16 | (blue & 255) << 8 | (alpha & 255);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }
}
