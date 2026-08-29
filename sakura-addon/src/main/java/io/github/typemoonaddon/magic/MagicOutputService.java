package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.network.MagicOutputShockwavePayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/** Server-authoritative blast and expanding, entity-occluded shockwave for corrupted magic output. */
public final class MagicOutputService {
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        TypeMoonAddon.id("magic_output")
    );
    private static final DustParticleOptions SHOCKWAVE_PARTICLE = new DustParticleOptions(
        new Vector3f(0.18F, 0.62F, 1.0F),
        1.25F
    );
    private static final Map<ResourceKey<Level>, List<ActiveShockwave>> ACTIVE_SHOCKWAVES = new HashMap<>();

    public static void release(
        BlackShadowEntity source,
        ServerPlayer owner,
        Vec3 center
    ) {
        if (!(source.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource damageSource = damageSource(level, source, owner);
        damageInitialBlast(level, source, owner, center, damageSource);

        ExplosionDamageCalculator terrainOnlyExplosion = new ExplosionDamageCalculator() {
            @Override
            public Optional<Float> getBlockExplosionResistance(
                Explosion explosion,
                BlockGetter blockGetter,
                BlockPos position,
                BlockState state,
                FluidState fluidState
            ) {
                if (Vec3.atCenterOf(position).distanceToSqr(center)
                    > GameplayConfig.MAGIC_OUTPUT_RADIUS * GameplayConfig.MAGIC_OUTPUT_RADIUS) {
                    return Optional.of(Float.MAX_VALUE);
                }
                return super.getBlockExplosionResistance(explosion, blockGetter, position, state, fluidState);
            }

            @Override
            public boolean shouldBlockExplode(
                Explosion explosion,
                BlockGetter blockGetter,
                BlockPos position,
                BlockState state,
                float power
            ) {
                return Vec3.atCenterOf(position).distanceToSqr(center)
                    <= GameplayConfig.MAGIC_OUTPUT_RADIUS * GameplayConfig.MAGIC_OUTPUT_RADIUS;
            }

            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return false;
            }
        };
        level.explode(
            source,
            damageSource,
            terrainOnlyExplosion,
            center,
            GameplayConfig.MAGIC_OUTPUT_TERRAIN_EXPLOSION_POWER,
            false,
            Level.ExplosionInteraction.BLOCK
        );
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 6, 0.4D, 0.4D, 0.4D, 0.0D);
        sendShockwave(level, owner, center);
        level.playSound(
            null,
            BlockPos.containing(center),
            SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.HOSTILE,
            4.0F,
            0.45F
        );
        ACTIVE_SHOCKWAVES.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>()).add(
            new ActiveShockwave(source, owner, center)
        );
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<ResourceKey<Level>, List<ActiveShockwave>>> levels = ACTIVE_SHOCKWAVES.entrySet().iterator();
        while (levels.hasNext()) {
            Map.Entry<ResourceKey<Level>, List<ActiveShockwave>> entry = levels.next();
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                levels.remove();
                continue;
            }
            entry.getValue().removeIf(shockwave -> shockwave.tick(level));
            if (entry.getValue().isEmpty()) {
                levels.remove();
            }
        }
    }

    public static boolean isDamage(DamageSource source) {
        return source != null && source.is(DAMAGE_TYPE);
    }

    public static void serverStopping() {
        ACTIVE_SHOCKWAVES.clear();
    }

    private static void damageInitialBlast(
        ServerLevel level,
        BlackShadowEntity source,
        ServerPlayer owner,
        Vec3 center,
        DamageSource damageSource
    ) {
        double radius = GameplayConfig.MAGIC_OUTPUT_RADIUS;
        for (LivingEntity victim : level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(center, center).inflate(radius),
            victim -> canDamage(source, owner, victim)
        )) {
            double distance = victim.getBoundingBox().getCenter().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float damage = Mth.lerp(
                (float)(distance / radius),
                GameplayConfig.MAGIC_OUTPUT_CENTER_DAMAGE,
                GameplayConfig.MAGIC_OUTPUT_EDGE_DAMAGE
            );
            victim.hurt(damageSource, damage);
        }
    }

    private static void sendShockwave(ServerLevel level, ServerPlayer owner, Vec3 center) {
        MagicOutputShockwavePayload payload = new MagicOutputShockwavePayload(
            center.x,
            center.y,
            center.z,
            (float)GameplayConfig.MAGIC_OUTPUT_RADIUS,
            GameplayConfig.MAGIC_OUTPUT_SHOCKWAVE_TICKS
        );
        PacketDistributor.sendToPlayer(owner, payload);
        PacketDistributor.sendToPlayersNear(
            level,
            owner,
            center.x,
            center.y,
            center.z,
            192.0D,
            payload
        );
    }

    private static DamageSource damageSource(ServerLevel level, BlackShadowEntity source, ServerPlayer owner) {
        return new DamageSource(
            level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE),
            source,
            owner
        );
    }

    private static boolean canDamage(BlackShadowEntity source, ServerPlayer owner, LivingEntity victim) {
        return victim.isAlive()
            && victim != source
            && victim != owner
            && !source.isAlliedTo(victim)
            && !(victim instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    private static final class ActiveShockwave {
        private final BlackShadowEntity source;
        private final ServerPlayer owner;
        private final Vec3 center;
        private final Set<UUID> hitEntities = new HashSet<>();
        private int elapsedTicks;

        private ActiveShockwave(BlackShadowEntity source, ServerPlayer owner, Vec3 center) {
            this.source = source;
            this.owner = owner;
            this.center = center;
        }

        private boolean tick(ServerLevel level) {
            if (++this.elapsedTicks > GameplayConfig.MAGIC_OUTPUT_SHOCKWAVE_TICKS) {
                return true;
            }
            double previousRadius = GameplayConfig.MAGIC_OUTPUT_RADIUS
                * (this.elapsedTicks - 1) / GameplayConfig.MAGIC_OUTPUT_SHOCKWAVE_TICKS;
            double radius = GameplayConfig.MAGIC_OUTPUT_RADIUS
                * this.elapsedTicks / GameplayConfig.MAGIC_OUTPUT_SHOCKWAVE_TICKS;
            spawnParticles(level, radius);
            damageEntities(level, previousRadius, radius);
            destroyBlocks(level, previousRadius, radius);
            return false;
        }

        private void spawnParticles(ServerLevel level, double radius) {
            int samples = 16;
            double phase = this.elapsedTicks * 0.37D;
            for (int index = 0; index < samples; index++) {
                double y = 1.0D - 2.0D * (index + 0.5D) / samples;
                double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
                double angle = index * 2.399963229728653D + phase;
                level.sendParticles(
                    SHOCKWAVE_PARTICLE,
                    this.center.x + Math.cos(angle) * horizontal * radius,
                    this.center.y + y * radius,
                    this.center.z + Math.sin(angle) * horizontal * radius,
                    1,
                    0.02D,
                    0.02D,
                    0.02D,
                    0.01D
                );
            }
        }

        private void damageEntities(ServerLevel level, double previousRadius, double radius) {
            List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(this.center, this.center).inflate(radius + 1.5D),
                LivingEntity::isAlive
            );
            candidates.sort(Comparator.comparingDouble(entity -> entity.getBoundingBox().getCenter().distanceToSqr(this.center)));
            DamageSource damageSource = damageSource(level, this.source, this.owner);
            for (LivingEntity victim : candidates) {
                if (this.hitEntities.contains(victim.getUUID()) || !canDamage(this.source, this.owner, victim)) {
                    continue;
                }
                Vec3 victimCenter = victim.getBoundingBox().getCenter();
                double distance = victimCenter.distanceTo(this.center);
                double reach = Math.max(0.35D, victim.getBbWidth() * 0.5D);
                if (distance + reach < previousRadius || distance - reach > radius) {
                    continue;
                }
                if (isBlockedByEntity(candidates, victim, victimCenter, distance)) {
                    this.hitEntities.add(victim.getUUID());
                    continue;
                }
                victim.hurt(damageSource, GameplayConfig.MAGIC_OUTPUT_SHOCKWAVE_DAMAGE);
                this.hitEntities.add(victim.getUUID());
            }
        }

        private boolean isBlockedByEntity(
            List<LivingEntity> candidates,
            LivingEntity victim,
            Vec3 victimCenter,
            double victimDistance
        ) {
            for (LivingEntity blocker : candidates) {
                if (blocker == victim || blocker == this.source || blocker == this.owner) {
                    continue;
                }
                double blockerDistance = blocker.getBoundingBox().getCenter().distanceTo(this.center);
                if (blockerDistance >= victimDistance - 0.1D) {
                    break;
                }
                if (blocker.getBoundingBox().inflate(0.15D).clip(this.center, victimCenter).isPresent()) {
                    return true;
                }
            }
            return false;
        }

        private void destroyBlocks(ServerLevel level, double previousRadius, double radius) {
            int extent = Mth.ceil(radius + 1.0D);
            BlockPos centerPos = BlockPos.containing(this.center);
            int budget = GameplayConfig.MAGIC_OUTPUT_SHOCKWAVE_BLOCK_BUDGET;
            for (BlockPos candidate : BlockPos.betweenClosed(
                centerPos.offset(-extent, -extent, -extent),
                centerPos.offset(extent, extent, extent)
            )) {
                double distance = Vec3.atCenterOf(candidate).distanceTo(this.center);
                if (distance < Math.max(0.0D, previousRadius - 0.8D) || distance > radius + 0.8D) {
                    continue;
                }
                var state = level.getBlockState(candidate);
                float hardness = state.getDestroySpeed(level, candidate);
                if (state.isAir() || state.hasBlockEntity() || hardness < 0.0F || hardness > 50.0F) {
                    continue;
                }
                level.destroyBlock(candidate, false, this.owner);
                if (--budget <= 0) {
                    return;
                }
            }
        }
    }

    private MagicOutputService() {
    }
}
