package com.example.typemoonaddon.orias;

import com.example.typemoonaddon.magic.OriasMagic;
import com.example.typemoonaddon.network.OriasCastInputPayload;
import com.example.typemoonaddon.network.AddonSpellVisualPayload;
import com.example.typemoonaddon.kimaris.KimarisService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class OriasService {
    public static final int MIN_CHARGE_TICKS = 10;
    public static final int MAX_CHARGE_TICKS = 100;
    public static final double MANA_PER_TICK = 5.0D;
    public static final double MIN_EFFECTIVE_MANA = 20.0D;
    public static final double MAX_CHARGE_MANA = 500.0D;

    public static final float MIN_DAMAGE = 80.0F;
    public static final float MAX_DAMAGE = 600.0F;
    public static final double MIN_HORIZONTAL_RADIUS = 6.0D;
    public static final double MAX_HORIZONTAL_RADIUS = 48.0D;
    public static final double MIN_CORE_RADIUS = 2.0D;
    public static final double MAX_CORE_RADIUS = 10.0D;
    public static final double MIN_HEIGHT = 8.0D;
    public static final double MAX_HEIGHT = 80.0D;
    public static final double RELEASE_HEIGHT_BONUS = 40.0D;
    public static final int MIN_DURATION_TICKS = 30;
    public static final int MAX_DURATION_TICKS = 120;
    public static final int MAX_BLOCKS_PER_ERUPTION = 6000;
    public static final int MAX_BLOCKS_PER_TICK = 120;
    public static final double MAX_TARGET_RANGE = 256.0D;

    private static final int MANA_SYNC_INTERVAL_TICKS = 5;
    private static final int MAX_CHARGE_SUBTITLE_FADE_IN_TICKS = 5;
    private static final int MAX_CHARGE_SUBTITLE_STAY_TICKS = 50;
    private static final int MAX_CHARGE_SUBTITLE_FADE_OUT_TICKS = 10;
    private static final int MIN_TEMPORARY_LAVA_BLOCKS = 8;
    private static final int MAX_TEMPORARY_LAVA_BLOCKS = 320;
    private static final int MAX_ERUPTION_VFX_SEGMENTS = 7;
    private static final int MAX_GROUND_VFX_NODES = 8;
    private static final int MAX_AFTERMATH_VFX_NODES = 5;
    private static final double MIN_VFX_OBSERVER_RADIUS = 96.0D;
    private static final double MAX_VFX_OBSERVER_RADIUS = 280.0D;
    private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();
    private static final Set<UUID> PRESSED_PLAYERS = new HashSet<>();
    private static final List<TerrainTask> TERRAIN_TASKS = new ArrayList<>();

    private OriasService() {
    }

    public static void handleCastInput(ServerPlayer player, byte action) {
        if (KimarisService.isFrozen(player)) {
            cancelPlayer(player, true, false);
            return;
        }
        switch (action) {
            case OriasCastInputPayload.PRESS -> beginCharge(player);
            case OriasCastInputPayload.RELEASE -> releaseCharge(player);
            case OriasCastInputPayload.CANCEL -> cancelPlayer(player, true, false);
            default -> {
            }
        }
    }

    public static void tickCharge(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!isValidCaster(player, true) || !state.dimension.equals(player.serverLevel().dimension())) {
            cancelPlayer(player, true, false);
            return;
        }
        if (state.chargedTicks >= MAX_CHARGE_TICKS) {
            if (player.tickCount % 10 == 0) {
                sendCasterSigil(player, 1.0D);
            }
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double currentMana = finiteNonNegative(vars.player_mana);
        double injected = Math.min(MANA_PER_TICK, currentMana);
        if (injected <= 0.0D) {
            CHARGES.remove(player.getUUID());
            PRESSED_PLAYERS.remove(player.getUUID());
            finishCharge(player, state, true);
            return;
        }

        double previousChargedMana = state.chargedMana;
        vars.player_mana = Math.max(0.0D, currentMana - injected);
        state.chargedMana = Math.min(MAX_CHARGE_MANA, state.chargedMana + injected);
        state.chargedTicks++;

        if (previousChargedMana < MAX_CHARGE_MANA
                && state.chargedMana >= MAX_CHARGE_MANA) {
            showMaxChargeSubtitle(player);
        }

        boolean exhausted = injected < MANA_PER_TICK || vars.player_mana <= 0.0D;
        if (state.chargedTicks % MANA_SYNC_INTERVAL_TICKS == 0
                || state.chargedTicks >= MAX_CHARGE_TICKS
                || exhausted) {
            vars.syncMana(player);
        }
        if (state.chargedTicks % 10 == 0) {
            sendChargeParticles(player, state);
        }

        if (exhausted) {
            CHARGES.remove(player.getUUID());
            PRESSED_PLAYERS.remove(player.getUUID());
            finishCharge(player, state, true);
        }
    }

    public static void tickTerrainTasks(MinecraftServer server) {
        Iterator<TerrainTask> iterator = TERRAIN_TASKS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick(server)) {
                iterator.remove();
            }
        }
    }

    public static void cancelPlayer(Player player, boolean refundMana, boolean cancelTerrain) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        PRESSED_PLAYERS.remove(playerId);
        ChargeState state = CHARGES.remove(playerId);
        if (refundMana && state != null && player instanceof ServerPlayer serverPlayer) {
            refundMana(serverPlayer, state.chargedMana);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            AddonSpellVisualPayload.clear(
                    serverPlayer, AddonSpellVisualPayload.ORIAS, MAX_VFX_OBSERVER_RADIUS);
        }
        if (cancelTerrain && player.getServer() != null) {
            cancelTerrainTasks(player.getServer(), playerId);
        }
    }

    public static void handleChunkUnload(ServerLevel level, ChunkPos chunkPos) {
        Iterator<TerrainTask> iterator = TERRAIN_TASKS.iterator();
        while (iterator.hasNext()) {
            TerrainTask task = iterator.next();
            if (task.dimension.equals(level.dimension()) && task.touchesChunk(chunkPos)) {
                task.cleanup(level);
                iterator.remove();
            }
        }
    }

    public static void clearAll(MinecraftServer server) {
        for (Map.Entry<UUID, ChargeState> entry : CHARGES.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                refundMana(player, entry.getValue().chargedMana);
            }
        }
        CHARGES.clear();
        PRESSED_PLAYERS.clear();

        for (TerrainTask task : TERRAIN_TASKS) {
            ServerLevel level = server.getLevel(task.dimension);
            if (level != null) {
                task.cleanup(level);
            }
        }
        TERRAIN_TASKS.clear();
    }

    private static void beginCharge(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!isValidCaster(player, true)
                || PRESSED_PLAYERS.contains(playerId)
                || CHARGES.containsKey(playerId)) {
            return;
        }

        PRESSED_PLAYERS.add(playerId);
        CHARGES.put(playerId, new ChargeState(player.serverLevel().dimension()));
        player.displayClientMessage(Component.translatable("message.typemoonworld.orias.charging"), true);
    }

    private static void releaseCharge(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!PRESSED_PLAYERS.remove(playerId)) {
            return;
        }
        ChargeState state = CHARGES.remove(playerId);
        if (state == null) {
            return;
        }
        if (!isValidCaster(player, true) || !state.dimension.equals(player.serverLevel().dimension())) {
            refundMana(player, state.chargedMana);
            return;
        }
        finishCharge(player, state, false);
    }

    private static void finishCharge(ServerPlayer player, ChargeState state, boolean manaExhausted) {
        AddonSpellVisualPayload.clear(
                player, AddonSpellVisualPayload.ORIAS, MAX_VFX_OBSERVER_RADIUS);
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.syncMana(player);

        if (state.chargedTicks < MIN_CHARGE_TICKS || state.chargedMana < MIN_EFFECTIVE_MANA) {
            double refunded = refundMana(player, state.chargedMana);
            player.displayClientMessage(Component.translatable(
                    manaExhausted
                            ? "message.typemoonworld.orias.mana_insufficient_refunded"
                            : "message.typemoonworld.orias.undercharged",
                    formatMana(refunded)), true);
            return;
        }

        BlockPos ground = resolveGround(player);
        if (ground == null) {
            refundMana(player, state.chargedMana);
            player.displayClientMessage(Component.translatable("message.typemoonworld.orias.no_ground"), true);
            return;
        }

        EruptionParameters parameters = EruptionParameters.fromMana(state.chargedMana);
        boolean terrainAllowed = erupt(player, ground, parameters);
        player.displayClientMessage(Component.translatable(
                manaExhausted
                        ? "message.typemoonworld.orias.mana_insufficient_erupted"
                        : "message.typemoonworld.orias.erupted",
                formatMana(state.chargedMana)), true);
        if (!terrainAllowed) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.orias.terrain_disabled"), true);
        }
    }

    private static boolean erupt(
            ServerPlayer player,
            BlockPos ground,
            EruptionParameters parameters
    ) {
        ServerLevel level = player.serverLevel();
        Vec3 center = Vec3.atBottomCenterOf(ground.above());
        sendCasterSigil(player, parameters.strength());
        sendEruptionVfx(level, center, parameters);
        sendEruptionParticles(level, center, parameters);
        damageTargets(player, level, center, parameters);

        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        }

        TerrainTask task = createTerrainTask(player, level, ground, parameters);
        if (task != null) {
            TERRAIN_TASKS.add(task);
        }
        return true;
    }

    private static void damageTargets(
            ServerPlayer player,
            ServerLevel level,
            Vec3 center,
            EruptionParameters parameters
    ) {
        AABB area = new AABB(
                center.x - parameters.radius,
                center.y,
                center.z - parameters.radius,
                center.x + parameters.radius,
                center.y + parameters.height,
                center.z + parameters.radius
        );
        Set<UUID> damaged = new HashSet<>();
        DamageSource source = player.damageSources().source(DamageTypes.MAGIC, player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDistance > parameters.radius
                    || !damaged.add(target.getUUID())
                    || !EntityUtils.isValidCombatTarget(player, target)) {
                continue;
            }
            target.hurt(source, parameters.damageAt(horizontalDistance));
        }
    }

    private static TerrainTask createTerrainTask(
            ServerPlayer player,
            ServerLevel level,
            BlockPos center,
            EruptionParameters parameters
    ) {
        int scanRadius = Mth.ceil(parameters.radius);
        List<SurfaceOffset> offsets = new ArrayList<>();
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                double distanceSqr = dx * dx + dz * dz;
                if (distanceSqr <= parameters.radius * parameters.radius) {
                    offsets.add(new SurfaceOffset(dx, dz, distanceSqr));
                }
            }
        }
        offsets.sort(Comparator.comparingDouble(SurfaceOffset::distanceSqr));

        ArrayDeque<BlockAction> actions = new ArrayDeque<>();
        int lavaBudget = Mth.clamp(
                Mth.floor(Mth.lerp(parameters.strength,
                        MIN_TEMPORARY_LAVA_BLOCKS, MAX_TEMPORARY_LAVA_BLOCKS)),
                MIN_TEMPORARY_LAVA_BLOCKS,
                MAX_TEMPORARY_LAVA_BLOCKS
        );
        for (SurfaceOffset offset : offsets) {
            if (actions.size() >= MAX_BLOCKS_PER_ERUPTION) {
                break;
            }
            int x = center.getX() + offset.dx;
            int z = center.getZ() + offset.dz;
            BlockPos surface = findSurface(level, x, z, center.getY() + 3, center.getY() - 6);
            if (surface == null
                    || !level.getWorldBorder().isWithinBounds(surface)
                    || !level.mayInteract(player, surface)
                    || !canReplaceGround(level, surface)) {
                continue;
            }

            actions.addLast(new BlockAction(surface.immutable(), ActionType.NETHERRACK));
            BlockPos lavaPos = surface.above();
            if (lavaBudget > 0
                    && offset.distanceSqr >= 1.0D
                    && offset.distanceSqr <= parameters.coreRadius * parameters.coreRadius
                    && actions.size() < MAX_BLOCKS_PER_ERUPTION
                    && level.getBlockState(lavaPos).isAir()
                    && player.blockPosition().distSqr(lavaPos) > 2.0D) {
                actions.addLast(new BlockAction(lavaPos.immutable(), ActionType.TEMPORARY_LAVA));
                lavaBudget--;
            }
        }

        if (actions.isEmpty()) {
            return null;
        }
        long cleanupTick = level.getGameTime() + parameters.durationTicks;
        return new TerrainTask(player.getUUID(), level.dimension(), center.immutable(), actions, cleanupTick);
    }

    private static BlockPos resolveGround(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(MAX_TARGET_RANGE));
        HitResult hit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = blockHit.getBlockPos();
            BlockPos surface = findSurface(
                    level,
                    hitPos.getX(),
                    hitPos.getZ(),
                    level.getMaxBuildHeight() - 2,
                    level.getMinBuildHeight() + 1
            );
            if (isValidGroundCenter(level, surface)) {
                return surface;
            }
        }
        return null;
    }

    private static boolean isValidGroundCenter(ServerLevel level, BlockPos ground) {
        if (ground == null
                || !level.hasChunkAt(ground)
                || !level.getWorldBorder().isWithinBounds(ground)
                || ground.getY() <= level.getMinBuildHeight()
                || ground.getY() >= level.getMaxBuildHeight() - 1) {
            return false;
        }
        return true;
    }

    private static BlockPos findSurface(ServerLevel level, int x, int z, int startY, int minY) {
        int top = Mth.clamp(startY, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
        int bottom = Mth.clamp(minY, level.getMinBuildHeight() + 1, top);
        BlockPos column = new BlockPos(x, top, z);
        if (!level.hasChunkAt(column)) {
            return null;
        }
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()
                    && state.getFluidState().isEmpty()
                    && !state.getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private static boolean canReplaceGround(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return !state.hasBlockEntity()
                && (state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.STONE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE));
    }

    private static boolean isValidCaster(ServerPlayer player, boolean requireNoMenu) {
        if (player == null
                || !player.isAlive()
                || player.isRemoved()
                || player.isSpectator()
                || player.hasDisconnected()
                || requireNoMenu && player.containerMenu != player.inventoryMenu) {
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        vars.rebuildSelectedMagicsFromActiveWheel();
        PlayerMagicSelectionService.prepareCurrentSelection(player, vars);
        return vars.is_magus
                && vars.is_magic_circuit_open
                && vars.magic_cooldown <= 0.0D
                && PlayerMagicSelectionService.isCurrentSelection(vars, OriasMagic.MAGIC_ID);
    }

    private static void sendChargeParticles(ServerPlayer player, ChargeState state) {
        double strength = Mth.clamp(state.chargedMana / MAX_CHARGE_MANA, 0.0D, 1.0D);
        sendCasterSigil(player, strength);
        VFXServerEffects.spawn(
                player.serverLevel(),
                "typemoonworld:orias_charge",
                player,
                Mth.lerp(strength, MIN_VFX_OBSERVER_RADIUS, MAX_VFX_OBSERVER_RADIUS)
        );
        double ringRadius = Mth.lerp(strength, 0.8D, 5.5D);
        player.serverLevel().sendParticles(
                ParticleTypes.FLAME,
                player.getX(),
                player.getY() + 0.12D,
                player.getZ(),
                Mth.clamp(8 + Mth.floor(strength * 24.0D), 8, 32),
                ringRadius,
                0.08D,
                ringRadius,
                0.02D
        );
        player.serverLevel().sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(),
                player.getY() + 0.25D,
                player.getZ(),
                Mth.clamp(2 + Mth.floor(strength * 10.0D), 2, 12),
                ringRadius * 0.7D,
                0.25D,
                ringRadius * 0.7D,
                0.02D
        );
    }

    private static void sendCasterSigil(ServerPlayer player, double strength) {
        double observerRadius = Mth.lerp(
                Mth.clamp(strength, 0.0D, 1.0D),
                MIN_VFX_OBSERVER_RADIUS,
                MAX_VFX_OBSERVER_RADIUS
        );
        VFXServerEffects.spawn(
                player.serverLevel(),
                "typemoonworld:orias_sigil",
                player,
                observerRadius
        );
        AddonSpellVisualPayload.showAttached(
                player.serverLevel(), AddonSpellVisualPayload.ORIAS,
                AddonSpellVisualPayload.SIGIL, player.getUUID(), player,
                (float) Mth.clamp(strength, 0.0D, 1.0D), 1.0F,
                16, 0, observerRadius);
    }

    private static void showMaxChargeSubtitle(ServerPlayer player) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                MAX_CHARGE_SUBTITLE_FADE_IN_TICKS,
                MAX_CHARGE_SUBTITLE_STAY_TICKS,
                MAX_CHARGE_SUBTITLE_FADE_OUT_TICKS
        ));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(
                "message.typemoonworld.orias.max_charge_subtitle")));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
    }

    private static void sendEruptionVfx(ServerLevel level, Vec3 center, EruptionParameters parameters) {
        double observerRadius = Mth.lerp(
                parameters.strength,
                MIN_VFX_OBSERVER_RADIUS,
                MAX_VFX_OBSERVER_RADIUS
        );
        VFXServerEffects.spawn(level, "typemoonworld:orias_ground_warning", center, observerRadius);
        VFXServerEffects.spawn(level, "typemoonworld:orias_ground_impact", center, observerRadius);
        VFXServerEffects.spawn(level, "typemoonworld:orias_aftermath", center, observerRadius);
        UUID eruptionOwner = UUID.randomUUID();
        AddonSpellVisualPayload.showAt(
                level, AddonSpellVisualPayload.ORIAS, AddonSpellVisualPayload.GROUND_IMPACT,
                eruptionOwner, center, (float) parameters.radius,
                (float) parameters.coreRadius, 28, 0, observerRadius);
        AddonSpellVisualPayload.showAt(
                level, AddonSpellVisualPayload.ORIAS, AddonSpellVisualPayload.ERUPTION,
                eruptionOwner, center, (float) parameters.radius,
                (float) parameters.height, parameters.durationTicks, 0, observerRadius);
        AddonSpellVisualPayload.showAt(
                level, AddonSpellVisualPayload.ORIAS, AddonSpellVisualPayload.AFTERMATH,
                eruptionOwner, center, (float) parameters.radius,
                1.0F, 34, 0, observerRadius);

        int eruptionSegments = Mth.clamp(
                Mth.ceil(parameters.height / 12.0D),
                1,
                MAX_ERUPTION_VFX_SEGMENTS
        );
        for (int segment = 0; segment < eruptionSegments; segment++) {
            Vec3 segmentCenter = center.add(0.0D, segment * parameters.height / eruptionSegments, 0.0D);
            VFXServerEffects.spawn(level, "typemoonworld:orias_eruption", segmentCenter, observerRadius);
        }

        int groundNodes = Mth.clamp(
                Mth.floor(parameters.strength * MAX_GROUND_VFX_NODES),
                0,
                MAX_GROUND_VFX_NODES
        );
        spawnRadialVfx(level, "typemoonworld:orias_ground_warning", center,
                parameters.radius * 0.55D, groundNodes, observerRadius);
        spawnRadialVfx(level, "typemoonworld:orias_ground_impact", center,
                parameters.radius * 0.48D, groundNodes, observerRadius);

        int aftermathNodes = Mth.clamp(
                Mth.floor(parameters.strength * MAX_AFTERMATH_VFX_NODES),
                0,
                MAX_AFTERMATH_VFX_NODES
        );
        spawnRadialVfx(level, "typemoonworld:orias_aftermath", center,
                parameters.coreRadius * 0.75D, aftermathNodes, observerRadius);
    }

    private static void spawnRadialVfx(
            ServerLevel level,
            String effectId,
            Vec3 center,
            double radius,
            int count,
            double observerRadius
    ) {
        for (int index = 0; index < count; index++) {
            double angle = Math.PI * 2.0D * index / count;
            Vec3 origin = center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            VFXServerEffects.spawn(level, effectId, origin, observerRadius);
        }
    }

    private static void sendEruptionParticles(
            ServerLevel level,
            Vec3 center,
            EruptionParameters parameters
    ) {
        int lavaCount = Mth.clamp(24 + Mth.floor(parameters.strength * 72.0D), 24, 96);
        int flameCount = Mth.clamp(48 + Mth.floor(parameters.strength * 144.0D), 48, 192);
        int smokeCount = Mth.clamp(28 + Mth.floor(parameters.strength * 84.0D), 28, 112);
        int explosionCount = Mth.clamp(6 + Mth.floor(parameters.strength * 18.0D), 6, 24);
        level.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.15D, center.z,
                lavaCount, parameters.coreRadius * 0.75D, 0.35D, parameters.coreRadius * 0.75D, 0.05D);
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + parameters.height * 0.5D, center.z,
                flameCount, parameters.coreRadius * 0.95D, parameters.height * 0.48D,
                parameters.coreRadius * 0.95D, 0.12D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                center.x, center.y + parameters.height * 0.45D, center.z,
                smokeCount, parameters.coreRadius * 0.9D, parameters.height * 0.42D,
                parameters.coreRadius * 0.9D, 0.06D);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.25D, center.z,
                explosionCount, parameters.coreRadius * 1.2D, 0.45D,
                parameters.coreRadius * 1.2D, 0.0D);
    }

    private static double refundMana(ServerPlayer player, double amount) {
        if (amount <= 0.0D || !Double.isFinite(amount)) {
            return 0.0D;
        }
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double current = finiteNonNegative(vars.player_mana);
        double maximum = finiteNonNegative(vars.player_max_mana);
        double refunded = Math.min(amount, Math.max(0.0D, maximum - current));
        vars.player_mana = Math.min(maximum, current + refunded);
        vars.syncMana(player);
        return refunded;
    }

    private static void cancelTerrainTasks(MinecraftServer server, UUID playerId) {
        Iterator<TerrainTask> iterator = TERRAIN_TASKS.iterator();
        while (iterator.hasNext()) {
            TerrainTask task = iterator.next();
            if (!task.owner.equals(playerId)) {
                continue;
            }
            ServerLevel level = server.getLevel(task.dimension);
            if (level != null) {
                task.cleanup(level);
            }
            iterator.remove();
        }
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static String formatMana(double value) {
        double normalized = finiteNonNegative(value);
        long rounded = Math.round(normalized);
        return Math.abs(normalized - rounded) < 0.000001D
                ? Long.toString(rounded)
                : String.format(java.util.Locale.ROOT, "%.2f", normalized);
    }

    private static final class ChargeState {
        private final ResourceKey<Level> dimension;
        private int chargedTicks;
        private double chargedMana;

        private ChargeState(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }
    }

    private record EruptionParameters(
            double strength,
            float damage,
            double radius,
            double coreRadius,
            double height,
            int durationTicks
    ) {
        private static EruptionParameters fromMana(double mana) {
            double safeMana = Mth.clamp(finiteNonNegative(mana), MIN_EFFECTIVE_MANA, MAX_CHARGE_MANA);
            double charge = Mth.clamp((safeMana - MIN_EFFECTIVE_MANA) / 180.0D, 0.0D, 1.0D);
            double strength = charge * charge;
            return new EruptionParameters(
                    strength,
                    Mth.lerp((float) strength, MIN_DAMAGE, MAX_DAMAGE),
                    Mth.lerp(strength, MIN_HORIZONTAL_RADIUS, MAX_HORIZONTAL_RADIUS),
                    Mth.lerp(strength, MIN_CORE_RADIUS, MAX_CORE_RADIUS),
                    Mth.clamp(
                            Mth.lerp(strength, MIN_HEIGHT, MAX_HEIGHT)
                                    + RELEASE_HEIGHT_BONUS,
                            MIN_HEIGHT + RELEASE_HEIGHT_BONUS,
                            MAX_HEIGHT + RELEASE_HEIGHT_BONUS
                    ),
                    Mth.clamp(Mth.floor(Mth.lerp(strength, MIN_DURATION_TICKS, MAX_DURATION_TICKS)),
                            MIN_DURATION_TICKS, MAX_DURATION_TICKS)
            );
        }

        private float damageAt(double horizontalDistance) {
            double normalized = Mth.clamp(horizontalDistance / Math.max(0.001D, radius), 0.0D, 1.0D);
            double multiplier = normalized <= 0.2D
                    ? 1.0D
                    : Mth.lerp((float) ((normalized - 0.2D) / 0.8D), 1.0F, 0.2F);
            return (float) Math.max(0.0D, damage * multiplier);
        }
    }

    private record SurfaceOffset(int dx, int dz, double distanceSqr) {
    }

    private enum ActionType {
        NETHERRACK,
        TEMPORARY_LAVA
    }

    private record BlockAction(BlockPos pos, ActionType type) {
    }

    private static final class TerrainTask {
        private final UUID owner;
        private final ResourceKey<Level> dimension;
        private final BlockPos center;
        private final ArrayDeque<BlockAction> pending;
        private final Map<BlockPos, BlockState> temporaryLava = new LinkedHashMap<>();
        private final long cleanupTick;

        private TerrainTask(
                UUID owner,
                ResourceKey<Level> dimension,
                BlockPos center,
                ArrayDeque<BlockAction> pending,
                long cleanupTick
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.center = center;
            this.pending = pending;
            this.cleanupTick = cleanupTick;
        }

        private boolean tick(MinecraftServer server) {
            ServerLevel level = server.getLevel(dimension);
            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (level == null) {
                return true;
            }
            if (player == null || !player.isAlive() || !player.serverLevel().dimension().equals(dimension)) {
                cleanup(level);
                return true;
            }
            if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                cleanup(level);
                return true;
            }

            int processed = 0;
            while (processed < MAX_BLOCKS_PER_TICK && !pending.isEmpty()) {
                apply(level, player, pending.removeFirst());
                processed++;
            }
            if (level.getGameTime() >= cleanupTick && processed < MAX_BLOCKS_PER_TICK) {
                restoreTemporaryLava(level, MAX_BLOCKS_PER_TICK - processed);
            }
            return pending.isEmpty() && temporaryLava.isEmpty();
        }

        private void apply(ServerLevel level, ServerPlayer player, BlockAction action) {
            BlockPos pos = action.pos;
            if (!level.hasChunkAt(pos)
                    || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.mayInteract(player, pos)
                    || level.getBlockEntity(pos) != null) {
                return;
            }

            if (action.type == ActionType.NETHERRACK) {
                if (canReplaceGround(level, pos)) {
                    level.setBlock(pos, Blocks.NETHERRACK.defaultBlockState(), Block.UPDATE_ALL);
                }
                return;
            }

            BlockState current = level.getBlockState(pos);
            if (current.isAir() && level.getBlockEntity(pos) == null) {
                temporaryLava.put(pos.immutable(), current);
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        private void restoreTemporaryLava(ServerLevel level, int budget) {
            Iterator<Map.Entry<BlockPos, BlockState>> iterator = temporaryLava.entrySet().iterator();
            int restored = 0;
            while (iterator.hasNext() && restored < budget) {
                Map.Entry<BlockPos, BlockState> entry = iterator.next();
                BlockPos pos = entry.getKey();
                if (!level.hasChunkAt(pos)) {
                    continue;
                }
                if (level.getBlockState(pos).is(Blocks.LAVA)) {
                    level.setBlock(pos, entry.getValue(), Block.UPDATE_ALL);
                }
                iterator.remove();
                restored++;
            }
        }

        private boolean touchesChunk(ChunkPos chunkPos) {
            if (new ChunkPos(center).equals(chunkPos)) {
                return true;
            }
            for (BlockAction action : pending) {
                if (new ChunkPos(action.pos).equals(chunkPos)) {
                    return true;
                }
            }
            for (BlockPos pos : temporaryLava.keySet()) {
                if (new ChunkPos(pos).equals(chunkPos)) {
                    return true;
                }
            }
            return false;
        }

        private void cleanup(ServerLevel level) {
            for (Map.Entry<BlockPos, BlockState> entry : temporaryLava.entrySet()) {
                BlockPos pos = entry.getKey();
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.LAVA)) {
                    level.setBlock(pos, entry.getValue(), Block.UPDATE_ALL);
                }
            }
            temporaryLava.clear();
            pending.clear();
        }
    }
}
