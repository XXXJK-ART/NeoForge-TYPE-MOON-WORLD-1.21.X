package com.example.typemoonaddon.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class GillesPollutionZoneService {
    private static final List<Zone> ZONES = new ArrayList<>();

    private GillesPollutionZoneService() {
    }

    public static void add(ResourceKey<Level> dimension, Vec3 center, double radius, int lifetimeTicks, float damagePerSecond) {
        ZONES.add(new Zone(dimension, center, radius, lifetimeTicks, damagePerSecond));
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        Iterator<Zone> iterator = ZONES.iterator();
        while (iterator.hasNext()) {
            Zone zone = iterator.next();
            if (!zone.dimension.equals(level.dimension())) {
                continue;
            }
            zone.remainingTicks -= 20;
            if (zone.remainingTicks <= 0) {
                iterator.remove();
                continue;
            }
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(zone.center, zone.center).inflate(zone.radius),
                    living -> living.isAlive() && living.distanceToSqr(zone.center) <= zone.radius * zone.radius)) {
                living.hurt(living.damageSources().magic(), zone.damagePerSecond);
            }
            level.sendParticles(ParticleTypes.SQUID_INK, zone.center.x, zone.center.y + 0.2, zone.center.z,
                    24, zone.radius * 0.28, 0.15, zone.radius * 0.28, 0.02);
        }
    }

    private static final class Zone {
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final double radius;
        private final float damagePerSecond;
        private int remainingTicks;

        private Zone(ResourceKey<Level> dimension, Vec3 center, double radius, int lifetimeTicks, float damagePerSecond) {
            this.dimension = dimension;
            this.center = center;
            this.radius = radius;
            this.remainingTicks = lifetimeTicks;
            this.damagePerSecond = damagePerSecond;
        }
    }
}
