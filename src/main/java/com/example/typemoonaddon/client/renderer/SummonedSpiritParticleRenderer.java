package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.entity.EvilSpiritEntity;
import com.example.typemoonaddon.entity.SummonedSpiritEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Renders summoned spirits as drifting clusters of soul particles instead of mob models. */
public final class SummonedSpiritParticleRenderer<T extends SummonedSpiritEntity> extends EntityRenderer<T> {
    private final Map<Integer, Integer> lastParticleTicks = new HashMap<>();

    public SummonedSpiritParticleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        if (!(entity.level() instanceof ClientLevel level)) {
            return;
        }
        Integer previousTick = this.lastParticleTicks.put(entity.getId(), entity.tickCount);
        if (previousTick != null && previousTick == entity.tickCount) {
            return;
        }
        if (this.lastParticleTicks.size() > 256) {
            this.lastParticleTicks.clear();
        }

        RandomSource random = entity.getRandom();
        boolean smallEvilSpirit = entity instanceof EvilSpiritEntity evilSpirit && evilSpirit.isSmall();
        double scale = entity instanceof EvilSpiritEntity ? (smallEvilSpirit ? 0.8D : 1.45D) : 0.9D;
        int particleCount = entity instanceof EvilSpiritEntity ? (smallEvilSpirit ? 10 : 18) : 8;
        double phase = entity.tickCount * 0.12D;
        for (int index = 0; index < particleCount; index++) {
            double angle = phase + index * Math.PI * 2.0D / particleCount;
            double radius = scale * (0.18D + random.nextDouble() * 0.26D);
            double x = entity.getX() + Math.cos(angle) * radius;
            double y = entity.getY() + scale * 0.25D + (index % 6) * scale * 0.18D
                    + random.nextDouble() * scale * 0.14D;
            double z = entity.getZ() + Math.sin(angle) * radius;
            double particleType = random.nextDouble();
            var type = particleType < 0.72D ? ParticleTypes.SOUL : ParticleTypes.SOUL_FIRE_FLAME;
            double driftScale = 0.018D * scale;
            double driftX = -Math.sin(angle) * driftScale;
            double driftY = (0.012D + random.nextDouble() * 0.018D) * scale;
            double driftZ = Math.cos(angle) * driftScale;
            level.addParticle(type, x, y, z, driftX, driftY, driftZ);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return ResourceLocation.withDefaultNamespace("textures/particle/soul.png");
    }
}
