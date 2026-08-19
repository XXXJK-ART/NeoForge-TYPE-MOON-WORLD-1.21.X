package com.example.typemoonaddon.entity;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class GillesPollutionZoneService {
    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final int PARTICLE_INTERVAL_TICKS = 40;
    private static final List<Zone> ZONES = new ArrayList<>();

    private GillesPollutionZoneService() {
    }

    public static void add(ResourceKey<Level> dimension, Vec3 center, double radius, int lifetimeTicks, float damagePerSecond) {
        add(dimension, center, radius, lifetimeTicks, damagePerSecond, null);
    }

    public static void add(ResourceKey<Level> dimension, Vec3 center, double radius, int lifetimeTicks,
                           float damagePerSecond, UUID sourceUuid) {
        add(dimension, center, radius, lifetimeTicks, damagePerSecond, sourceUuid, null);
    }

    public static void add(ResourceKey<Level> dimension, Vec3 center, double radius, int lifetimeTicks,
                           float damagePerSecond, UUID sourceUuid, UUID masterUuid) {
        ZONES.add(new Zone(dimension, center, radius, lifetimeTicks, damagePerSecond, sourceUuid, masterUuid));
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % DAMAGE_INTERVAL_TICKS != 0L) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Zone> iterator = ZONES.iterator();
        while (iterator.hasNext()) {
            Zone zone = iterator.next();
            if (!zone.dimension.equals(level.dimension())) {
                continue;
            }
            zone.remainingTicks -= DAMAGE_INTERVAL_TICKS;
            if (zone.remainingTicks <= 0) {
                iterator.remove();
                continue;
            }
            if (!level.hasNearbyAlivePlayer(zone.center.x, zone.center.y, zone.center.z, zone.radius + 32.0)) {
                continue;
            }
            Entity source = zone.resolveSource(level);
            Entity master = zone.resolveMaster(level);
            AABB box = zone.boundingBox();
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                    box,
                    living -> living.isAlive()
                            && living.distanceToSqr(zone.center) <= zone.radius * zone.radius
                            && !zone.isFriendly(source, master, living))) {
                living.hurt(living.damageSources().magic(), zone.damagePerSecond);
            }
            if (now % PARTICLE_INTERVAL_TICKS == 0L) {
                level.sendParticles(ParticleTypes.SQUID_INK, zone.center.x, zone.center.y + 0.2, zone.center.z,
                        10, zone.radius * 0.22, 0.12, zone.radius * 0.22, 0.02);
            }
        }
    }

    private static final class Zone {
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final double radius;
        private final float damagePerSecond;
        private final UUID sourceUuid;
        private final UUID masterUuid;
        private Entity cachedSource;
        private Entity cachedMaster;
        private long cachedResolveTick = Long.MIN_VALUE;
        private int remainingTicks;

        private Zone(ResourceKey<Level> dimension, Vec3 center, double radius, int lifetimeTicks,
                     float damagePerSecond, UUID sourceUuid, UUID masterUuid) {
            this.dimension = dimension;
            this.center = center;
            this.radius = radius;
            this.remainingTicks = lifetimeTicks;
            this.damagePerSecond = damagePerSecond;
            this.sourceUuid = sourceUuid;
            this.masterUuid = masterUuid;
        }

        private Entity resolveSource(ServerLevel level) {
            this.refreshResolveCache(level);
            return this.cachedSource;
        }

        private Entity resolveMaster(ServerLevel level) {
            this.refreshResolveCache(level);
            return this.cachedMaster;
        }

        private AABB boundingBox() {
            return new AABB(this.center.x - this.radius, this.center.y - this.radius, this.center.z - this.radius,
                    this.center.x + this.radius, this.center.y + this.radius, this.center.z + this.radius);
        }

        private void refreshResolveCache(ServerLevel level) {
            long now = level.getGameTime();
            if (this.cachedResolveTick == now) {
                return;
            }
            this.cachedResolveTick = now;
            this.cachedSource = this.sourceUuid == null ? null : level.getEntity(this.sourceUuid);
            this.cachedMaster = this.masterUuid == null ? null : level.getEntity(this.masterUuid);
        }

        private boolean isFriendly(Entity source, Entity master, LivingEntity living) {
            if (master != null && (living == master || master.isAlliedTo(living))) {
                return true;
            }
            if (source == null) {
                return false;
            }
            if (living == source || source.isAlliedTo(living)) {
                return true;
            }
            if (source instanceof GillesDeRaisEntity gilles) {
                ServerPlayer gillesMaster = gilles.getEntityMaster();
                return living == gillesMaster || gillesMaster != null && gillesMaster.isAlliedTo(living);
            }
            if (source instanceof HugeSeaMonsterEntity hugeSeaMonster) {
                return hugeSeaMonster.isFriendlyTo(living);
            }
            return false;
        }
    }
}
