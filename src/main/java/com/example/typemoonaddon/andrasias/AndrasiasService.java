package com.example.typemoonaddon.andrasias;

import com.example.typemoonaddon.magic.AndrasiasMagic;
import com.example.typemoonaddon.network.AndrasiasCastInputPayload;
import com.example.typemoonaddon.network.AddonSpellVisualPayload;
import com.example.typemoonaddon.kimaris.KimarisService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/** Server-authoritative two-stage impact and advancing ground rift. */
public final class AndrasiasService {
    public static final double TOTAL_DAMAGE_CAP = 300.0D;
    public static final float IMPACT_DAMAGE = 100.0F;
    public static final float RIFT_DAMAGE = 200.0F;
    public static final int MAX_CHARGE_TICKS = 100;
    public static final double MIN_RIFT_LENGTH = 100.0D;
    public static final double MAX_RIFT_LENGTH = 400.0D;
    public static final double MIN_RIFT_DEPTH = 100.0D;
    public static final double MAX_RIFT_DEPTH = 150.0D;
    public static final double MIN_RIFT_WIDTH = 5.0D;
    public static final double MAX_RIFT_WIDTH = 20.0D;
    /** Shared ten-block scale for the release impact and advancing rift. */
    public static final double ANDRASIAS_RADIUS = 10.0D;
    public static final double IMPACT_RADIUS = ANDRASIAS_RADIUS;
    /** Base terrain length; full charge interpolates this to MAX_RIFT_LENGTH. */
    public static final double RIFT_LENGTH = MIN_RIFT_LENGTH;
    public static final double RIFT_WIDTH = MIN_RIFT_WIDTH;
    /**
     * Vertical depth of the forward cylindrical cut. The cylinder is tangent to
     * the surface at its top and reaches this many blocks below the surface.
     */
    public static final double RIFT_DEPTH = MIN_RIFT_DEPTH;
    /** The circular cross-section uses the requested depth as its diameter. */
    public static final double RIFT_CYLINDER_RADIUS = RIFT_DEPTH * 0.5D;
    public static final int IMPACT_TO_RIFT_DELAY_TICKS = 8;
    public static final int RIFT_SEGMENT_INTERVAL_TICKS = 1;
    public static final int MAX_TERRAIN_BLOCKS = 1_200_000;
    public static final int MAX_BLOCKS_PER_TICK = 13_000;

    private static final double SEGMENT_LENGTH = 2.0D;
    private static final double IGALIMA_SAMPLE_STEP = 0.5D;
    private static final double IGALIMA_EDGE_RAMP = 0.18D;
    private static final double VFX_OBSERVER_RADIUS = 128.0D;
    private static final int CHARGE_VFX_INTERVAL_TICKS = 10;
    private static final String IMPACT_EFFECT = "typemoonworld:andrasias_impact";
    private static final String SIGIL_EFFECT = "typemoonworld:andrasias_sigil";
    private static final String RIFT_EFFECT = "typemoonworld:andrasias_ground_rift";
    private static final String AFTERMATH_EFFECT = "typemoonworld:andrasias_aftermath";

    private static final Map<UUID, CastState> ACTIVE = new HashMap<>();
    private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();

    private AndrasiasService() {
    }

    public static void handleCastInput(ServerPlayer player, byte action) {
        if (KimarisService.isFrozen(player)) {
            cancelCharge(player);
            return;
        }
        switch (action) {
            case AndrasiasCastInputPayload.PRESS -> beginCharge(player);
            case AndrasiasCastInputPayload.RELEASE -> releaseCharge(player);
            case AndrasiasCastInputPayload.CANCEL -> cancelCharge(player);
            default -> {
            }
        }
    }

    public static void tickCharge(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!isValidCaster(player) || !state.dimension.equals(player.serverLevel().dimension())) {
            cancelCharge(player);
            return;
        }

        state.chargedTicks = Math.min(MAX_CHARGE_TICKS, state.chargedTicks + 1);
        if (state.chargedTicks == 1 || state.chargedTicks % CHARGE_VFX_INTERVAL_TICKS == 0) {
            VFXServerEffects.spawn(player.serverLevel(), SIGIL_EFFECT, player, VFX_OBSERVER_RADIUS);
            AddonSpellVisualPayload.showAttached(
                    player.serverLevel(), AddonSpellVisualPayload.ANDRASIAS,
                    AddonSpellVisualPayload.SIGIL, player.getUUID(), player,
                    (float) chargePower(state.chargedTicks), 1.0F,
                    CHARGE_VFX_INTERVAL_TICKS + 7, 0, VFX_OBSERVER_RADIUS);
        }
        if (state.chargedTicks >= MAX_CHARGE_TICKS && !state.maxChargeNotified) {
            state.maxChargeNotified = true;
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.andrasias.max_charge"), true);
        }
    }

    public static boolean cast(ServerPlayer caster) {
        return cast(caster, MAX_CHARGE_TICKS);
    }

    private static boolean cast(ServerPlayer caster, int chargedTicks) {
        if (!isValidCaster(caster)
                || ACTIVE.containsKey(caster.getUUID())
                || CHARGES.containsKey(caster.getUUID())) {
            return false;
        }

        ServerLevel level = caster.serverLevel();
        double power = chargePower(chargedTicks);
        double riftLength = Mth.lerp(power, MIN_RIFT_LENGTH, MAX_RIFT_LENGTH);
        double riftDepth = Mth.lerp(power, MIN_RIFT_DEPTH, MAX_RIFT_DEPTH);
        double riftWidth = Mth.lerp(power, MIN_RIFT_WIDTH, MAX_RIFT_WIDTH);
        Vec3 direction = horizontalDirection(caster.getViewVector(1.0F));
        Vec3 ground = resolveGround(level, caster.position().add(direction.scale(2.0D)));
        if (ground == null) {
            caster.displayClientMessage(Component.translatable(
                    "message.typemoonworld.andrasias.no_ground"), true);
            return false;
        }

        CastState state = new CastState(
                UUID.randomUUID(),
                caster.getUUID(),
                level.dimension(),
                ground,
                direction,
                level.getGameTime() + IMPACT_TO_RIFT_DELAY_TICKS,
                riftLength,
                riftDepth,
                riftWidth
        );
        applyImpact(caster, level, state);
        AddonSpellVisualPayload.clear(
                caster, AddonSpellVisualPayload.ANDRASIAS, VFX_OBSERVER_RADIUS);
        VFXServerEffects.spawn(level, IMPACT_EFFECT, ground, VFX_OBSERVER_RADIUS);
        VFXServerEffects.spawn(level, SIGIL_EFFECT, caster, VFX_OBSERVER_RADIUS);
        AddonSpellVisualPayload.showAttached(
                level, AddonSpellVisualPayload.ANDRASIAS,
                AddonSpellVisualPayload.SIGIL, caster.getUUID(), caster,
                (float) power, 1.0F, 28, 1, VFX_OBSERVER_RADIUS);
        AddonSpellVisualPayload.showAt(
                level, AddonSpellVisualPayload.ANDRASIAS,
                AddonSpellVisualPayload.GROUND_IMPACT, caster.getUUID(), ground,
                (float) IMPACT_RADIUS, (float) riftDepth, 30, 0, VFX_OBSERVER_RADIUS);
        sendImpactParticles(level, ground);
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            caster.displayClientMessage(Component.translatable(
                    "message.typemoonworld.andrasias.terrain_disabled"), true);
        }
        ACTIVE.put(caster.getUUID(), state);
        caster.displayClientMessage(Component.translatable(
                "message.typemoonworld.andrasias.released"), true);
        return true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, CastState>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            CastState state = iterator.next().getValue();
            ServerPlayer caster = server.getPlayerList().getPlayer(state.casterId);
            ServerLevel level = server.getLevel(state.dimension);
            if (!isActiveCaster(caster)
                    || level == null
                    || caster.serverLevel() != level) {
                iterator.remove();
                continue;
            }

            long now = level.getGameTime();
            if (now >= state.nextRiftTick && state.nextSegment < state.maxRiftSegments) {
                advanceRift(caster, level, state);
                state.nextRiftTick = now + RIFT_SEGMENT_INTERVAL_TICKS;
            }
            if (state.nextSegment >= state.maxRiftSegments) {
                VFXServerEffects.spawn(level, AFTERMATH_EFFECT, state.origin, VFX_OBSERVER_RADIUS);
                AddonSpellVisualPayload.showAt(
                        level, AddonSpellVisualPayload.ANDRASIAS,
                        AddonSpellVisualPayload.AFTERMATH, state.casterId, state.origin,
                        (float) state.riftWidth, (float) state.riftDepth,
                        36, 0, VFX_OBSERVER_RADIUS);
                level.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.POOF,
                        state.origin.x, state.origin.y + 0.1D, state.origin.z,
                        18, 2.5D, 0.35D, 2.5D, 0.04D);
                iterator.remove();
                caster.displayClientMessage(Component.translatable(
                        "message.typemoonworld.andrasias.ended"), true);
            }
        }
    }

    public static void stop(net.minecraft.world.entity.Entity entity) {
        if (entity != null) {
            ACTIVE.remove(entity.getUUID());
            CHARGES.remove(entity.getUUID());
            if (entity instanceof ServerPlayer player) {
                AddonSpellVisualPayload.clear(
                        player, AddonSpellVisualPayload.ANDRASIAS, VFX_OBSERVER_RADIUS);
            }
        }
    }

    public static void clearAll() {
        ACTIVE.clear();
        CHARGES.clear();
    }

    private static void beginCharge(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (CHARGES.containsKey(playerId)
                || ACTIVE.containsKey(playerId)
                || !isValidCaster(player)) {
            return;
        }
        CHARGES.put(playerId, new ChargeState(player.serverLevel().dimension()));
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.andrasias.charging"), true);
    }

    private static void releaseCharge(ServerPlayer player) {
        ChargeState state = CHARGES.remove(player.getUUID());
        if (state == null) {
            return;
        }
        if (!isValidCaster(player) || !state.dimension.equals(player.serverLevel().dimension())) {
            return;
        }
        cast(player, state.chargedTicks);
    }

    private static void cancelCharge(ServerPlayer player) {
        if (player != null) {
            CHARGES.remove(player.getUUID());
            AddonSpellVisualPayload.clear(
                    player, AddonSpellVisualPayload.ANDRASIAS, VFX_OBSERVER_RADIUS);
        }
    }

    private static double chargePower(int chargedTicks) {
        double charge = Mth.clamp(chargedTicks / (double) MAX_CHARGE_TICKS, 0.0D, 1.0D);
        return charge * charge * (3.0D - 2.0D * charge);
    }

    private static void applyImpact(ServerPlayer caster, ServerLevel level, CastState state) {
        AABB area = new AABB(state.origin, state.origin).inflate(IMPACT_RADIUS);
        DamageSource source = caster.damageSources().source(DamageTypes.MAGIC, caster);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != caster
                        && EntityUtils.isValidCombatTarget(caster, entity)
                        && entity.getBoundingBox().getCenter().distanceToSqr(state.origin)
                        <= IMPACT_RADIUS * IMPACT_RADIUS)) {
            if (state.impactTargets.add(target.getUUID())) {
                dealStageDamage(target, source, state, IMPACT_DAMAGE);
                target.push(
                        state.direction.x * 0.55D,
                        0.18D,
                        state.direction.z * 0.55D);
                target.hurtMarked = true;
            }
        }
    }

    private static void advanceRift(ServerPlayer caster, ServerLevel level, CastState state) {
        int segment = state.nextSegment++;
        double distance = Math.min(state.riftLength, (segment + 1) * SEGMENT_LENGTH);
        Vec3 point = resolveGround(level, state.origin.add(state.direction.scale(distance)));
        if (point == null) {
            return;
        }
        state.lastRiftPoint = point;
        VFXServerEffects.spawn(level, RIFT_EFFECT, point, VFX_OBSERVER_RADIUS);
        AddonSpellVisualPayload.showBetween(
                level, AddonSpellVisualPayload.ANDRASIAS,
                AddonSpellVisualPayload.GROUND_RIFT, state.casterId,
                point, point.add(state.direction.scale(SEGMENT_LENGTH)),
                (float) state.riftWidth, (float) state.riftDepth,
                30, segment, VFX_OBSERVER_RADIUS);
        sendRiftParticles(level, point);
        applyRiftDamage(caster, level, state, point);
        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            double startDistance = segment * SEGMENT_LENGTH;
            processIgalimaTerrainSegment(level, state, startDistance, distance);
        }
    }

    private static void processIgalimaTerrainSegment(
            ServerLevel level,
            CastState state,
            double startDistance,
            double endDistance
    ) {
        if (state.terrainRemoved >= MAX_TERRAIN_BLOCKS) {
            return;
        }
        Vec3 right = new Vec3(-state.direction.z, 0.0D, state.direction.x);
        int checked = 0;
        terrain:
        for (double distance = startDistance;
                distance < endDistance && checked < MAX_BLOCKS_PER_TICK;
                distance += IGALIMA_SAMPLE_STEP) {
            Vec3 center = state.origin.add(state.direction.scale(distance));
            double widthFactor = igalimaWidthFactor(distance / state.riftLength);
            int currentWidth = Math.max(1, (int) (state.riftWidth * widthFactor));
            int halfWidth = currentWidth / 2;
            int depth = Math.max(1, (int) Math.ceil(state.riftDepth));
            for (int widthOffset = -halfWidth; widthOffset <= halfWidth; widthOffset++) {
                Vec3 horizontalOffset = right.scale(widthOffset);
                for (int depthOffset = 0; depthOffset < depth; depthOffset++) {
                    if (++checked > MAX_BLOCKS_PER_TICK
                            || state.terrainRemoved >= MAX_TERRAIN_BLOCKS) {
                        break terrain;
                    }
                    BlockPos pos = BlockPos.containing(
                            center.add(horizontalOffset).add(0.0D, -depthOffset, 0.0D));
                    if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) {
                        continue;
                    }
                    BlockState blockState = level.getBlockState(pos);
                    float hardness = blockState.getDestroySpeed(level, pos);
                    boolean fluid = !level.getFluidState(pos).isEmpty();
                    if ((!blockState.isAir()
                            && !blockState.is(Blocks.BEDROCK)
                            && hardness >= 0.0F)
                            || fluid) {
                        if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) {
                            state.terrainRemoved++;
                        }
                    }
                }
            }
        }
    }

    private static double igalimaWidthFactor(double progress) {
        double edge = Mth.clamp(progress, 0.0D, 1.0D);
        edge = Math.min(edge, 1.0D - edge);
        double ramp = Mth.clamp(edge / IGALIMA_EDGE_RAMP, 0.0D, 1.0D);
        ramp = ramp * ramp * (3.0D - 2.0D * ramp);
        return 0.16D + 0.84D * ramp;
    }

    private static void applyRiftDamage(
            ServerPlayer caster,
            ServerLevel level,
            CastState state,
            Vec3 point
    ) {
        double halfWidth = RIFT_WIDTH * 0.5D + 1.0D;
        AABB area = new AABB(point, point).inflate(halfWidth, 2.5D, halfWidth);
        DamageSource source = caster.damageSources().source(DamageTypes.MAGIC, caster);
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != caster
                        && EntityUtils.isValidCombatTarget(caster, entity)
                        && !state.riftTargets.contains(entity.getUUID()))) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            if (Math.abs(targetCenter.y - point.y) > 3.0D
                    || horizontalDistanceSqr(targetCenter, point) > halfWidth * halfWidth) {
                continue;
            }
            if (state.riftTargets.add(target.getUUID())) {
                dealStageDamage(target, source, state, RIFT_DAMAGE);
                target.push(0.0D, 0.28D, 0.0D);
                target.hurtMarked = true;
            }
        }
    }

    private static void dealStageDamage(
            LivingEntity target,
            DamageSource source,
            CastState state,
            float requested
    ) {
        if (!target.isAlive()) {
            return;
        }
        float already = state.damageByTarget.getOrDefault(target.getUUID(), 0.0F);
        float amount = (float) Math.min(
                Math.max(0.0D, TOTAL_DAMAGE_CAP - already),
                Math.max(0.0F, requested));
        if (amount <= 0.0F) {
            return;
        }
        if (target.hurt(source, amount)) {
            state.damageByTarget.put(target.getUUID(),
                    (float) Math.min(TOTAL_DAMAGE_CAP, already + amount));
        }
    }

    private static void sendImpactParticles(ServerLevel level, Vec3 point) {
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                point.x, point.y + 0.3D, point.z,
                2, 1.0D, 0.25D, 1.0D, 0.0D);
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.CLOUD,
                point.x, point.y + 0.15D, point.z,
                20, IMPACT_RADIUS * 0.45D, 0.12D, IMPACT_RADIUS * 0.45D, 0.08D);
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.FLAME,
                point.x, point.y + 0.45D, point.z,
                16, IMPACT_RADIUS * 0.55D, 0.7D, IMPACT_RADIUS * 0.55D, 0.08D);
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.LAVA,
                point.x, point.y + 0.35D, point.z,
                8, IMPACT_RADIUS * 0.45D, 0.4D, IMPACT_RADIUS * 0.45D, 0.04D);
    }

    private static void sendRiftParticles(ServerLevel level, Vec3 point) {
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                point.x, point.y + 0.2D, point.z,
                4, RIFT_WIDTH * 0.35D, 0.15D, RIFT_WIDTH * 0.35D, 0.025D);
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.CRIT,
                point.x, point.y + 0.35D, point.z,
                6, RIFT_WIDTH * 0.4D, 0.2D, RIFT_WIDTH * 0.4D, 0.08D);
    }

    private static Vec3 resolveGround(ServerLevel level, Vec3 point) {
        int x = Mth.floor(point.x);
        int z = Mth.floor(point.z);
        BlockPos column = new BlockPos(x, level.getMinBuildHeight(), z);
        if (!level.hasChunkAt(column)) {
            return null;
        }
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (top <= level.getMinBuildHeight()) {
            return null;
        }
        BlockState ground = level.getBlockState(new BlockPos(x, top, z));
        if (!ground.getFluidState().isEmpty()) {
            return null;
        }
        return new Vec3(x + 0.5D, top + 1.0D, z + 0.5D);
    }

    private static Vec3 horizontalDirection(Vec3 direction) {
        Vec3 flat = new Vec3(direction.x, 0.0D, direction.z);
        return flat.lengthSqr() > 1.0E-6D ? flat.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return dx * dx + dz * dz;
    }

    private static boolean isValidCaster(ServerPlayer player) {
        return isValidCaster(player, true);
    }

    private static boolean isActiveCaster(ServerPlayer player) {
        return isValidCaster(player, false);
    }

    private static boolean isValidCaster(ServerPlayer player, boolean requireReady) {
        if (player == null
                || !player.isAlive()
                || player.isRemoved()
                || player.isSpectator()
                || player.hasDisconnected()) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        vars.rebuildSelectedMagicsFromActiveWheel();
        PlayerMagicSelectionService.prepareCurrentSelection(player, vars);
        return vars.is_magus
                && vars.is_magic_circuit_open
                && (!requireReady || vars.magic_cooldown <= 0.0D)
                && PlayerMagicSelectionService.isCurrentSelection(vars, AndrasiasMagic.MAGIC_ID);
    }

    private static final class CastState {
        private final UUID castId;
        private final UUID casterId;
        private final ResourceKey<Level> dimension;
        private final Vec3 origin;
        private final Vec3 direction;
        private final long riftStartTick;
        private final double riftLength;
        private final double riftDepth;
        private final double riftWidth;
        private final int maxRiftSegments;
        private final Set<UUID> impactTargets = new HashSet<>();
        private final Set<UUID> riftTargets = new HashSet<>();
        private final Map<UUID, Float> damageByTarget = new HashMap<>();
        private int nextSegment;
        private long nextRiftTick;
        private Vec3 lastRiftPoint;
        private int terrainRemoved;

        private CastState(
                UUID castId,
                UUID casterId,
                ResourceKey<Level> dimension,
                Vec3 origin,
                Vec3 direction,
                long riftStartTick,
                double riftLength,
                double riftDepth,
                double riftWidth
        ) {
            this.castId = castId;
            this.casterId = casterId;
            this.dimension = dimension;
            this.origin = origin;
            this.direction = direction;
            this.riftStartTick = riftStartTick;
            this.riftLength = riftLength;
            this.riftDepth = riftDepth;
            this.riftWidth = riftWidth;
            this.maxRiftSegments = Math.max(1, (int) Math.ceil(riftLength / SEGMENT_LENGTH));
            this.nextRiftTick = riftStartTick;
        }
    }

    private static final class ChargeState {
        private final ResourceKey<Level> dimension;
        private int chargedTicks;
        private boolean maxChargeNotified;

        private ChargeState(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }
    }
}
