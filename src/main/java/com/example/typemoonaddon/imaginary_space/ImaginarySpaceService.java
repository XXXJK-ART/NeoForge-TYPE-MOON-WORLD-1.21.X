package com.example.typemoonaddon.imaginary_space;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.StorageVisualEntity;
import com.example.typemoonaddon.magic.ImaginarySpaceMagic;
import com.example.typemoonaddon.network.ImaginarySpaceStatePayload;
import com.example.typemoonaddon.registry.AddonSounds;
import java.security.SecureRandom;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.example.typemoonaddon.network.AddonNetwork;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/** Server-authoritative teleportation, instance generation, virtual state, and cleanup. */
public final class ImaginarySpaceService {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_space"));
    public static final ResourceKey<DamageType> WORLD_CORRECTION_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_space_correction"));
    /** Vanilla damage source used when an entity crosses the level world border. */
    public static final ResourceKey<DamageType> WORLD_BORDER_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.withDefaultNamespace("world_border"));

    public static final double TARGET_RADIUS = 25.0D;
    public static final double TARGET_RADIUS_SQUARED = TARGET_RADIUS * TARGET_RADIUS;
    public static final double MAX_DEPTH = ImaginarySpaceData.MAX_DEPTH;
    public static final double SELF_MANA_COST = 50.0D;
    public static final double CREATURE_MANA_COST = 100.0D;
    public static final int EXISTENCE_INTERVAL_TICKS = 20;
    public static final int TIME_OFFSET_INTERVAL_TICKS = 200;
    public static final int OUTER_GOD_DELAY_TICKS = 40;
    /** Generated content in each active instance is replaced every three minutes. */
    public static final int CONTENT_REFRESH_INTERVAL_TICKS = 3 * 60 * 20;
    /** Re-entry margin used when a player reaches the dimension's horizontal or vertical boundary. */
    private static final double WORLD_BOUNDARY_MARGIN = 1.0D;
    /** Boundary randomization includes depth zero, which exits to the main world. */
    private static final int RANDOM_BOUNDARY_MIN_DEPTH = 0;

    private static final int BASE_BLOCK_COUNT = 512;
    private static final int BASE_ITEM_COUNT = 128;
    private static final int BASE_MOB_COUNT = 24;
    private static final double MOB_GENERATION_MIN_DEPTH = 5.0D;
    /** Independent high-but-not-guaranteed rolls for the two named visitors. */
    private static final double NAO_MUSHROOM_CHANCE = 0.70D;
    private static final double CAT_ARC_CHANCE = 0.70D;
    private static final ResourceLocation MOOSHROOM_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "mooshroom");
    private static final ResourceLocation OCELOT_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "ocelot");

    private static final double INSTANCE_MIN_SEPARATION = 256.0D;
    private static final int INSTANCE_CELL_SIZE = 512;
    private static final int INSTANCE_CELL_LIMIT = 20_000;
    private static final double INSTANCE_Y = 128.0D;
    private static final int INSTANCE_TICKET_LEVEL = 3;
    private static final int MAX_SPAWN_RETRIES = 40;
    private static final int MAX_SAFE_RETURN_RADIUS = 8;
    private static final double WORLD_CORRECTION_BASE_DAMAGE = 4.0D;
    private static final double WORLD_CORRECTION_SCALE = 2.0D;
    private static final double WORLD_CORRECTION_MAX_DAMAGE = 100.0D;
    private static final double VFX_OBSERVER_RADIUS = 160.0D;
    /** Creature transport follows the reference storage-box lock window (60 ticks). */
    private static final int CREATURE_STORAGE_DURATION_TICKS = 60;
    /** The collapse renderer is 0.55 seconds long; transfer happens after it finishes. */
    private static final int CREATURE_STORAGE_COLLAPSE_TICKS = 11;
    /** Only the three-by-three chunk window around the owner is populated. */
    private static final int GENERATION_WINDOW_RADIUS_CHUNKS = 1;
    /** Generated blocks are sampled apart so the scene reads as scattered fragments. */
    private static final double MIN_RANDOM_BLOCK_SEPARATION = 1.35D;
    /** Water-like movement tuning for players inside the unbounded imaginary space. */
    private static final double SWIM_HORIZONTAL_DAMPING = 0.80D;
    private static final double SWIM_VERTICAL_DAMPING = 0.80D;
    private static final double SWIM_VERTICAL_ACCELERATION = 0.06D;
    private static final double SWIM_MAX_VERTICAL_SPEED = 0.30D;
    private static final double GENERATION_CENTER_REFRESH_DISTANCE_SQR = 8.0D * 8.0D;
    /** Movement bonus while the player holds the sprint key to enter swim mode. */
    private static final double SWIM_SPEED_MULTIPLIER = 0.65D;
    private static final ResourceLocation SWIM_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_swim_speed");
    private static final int MOVEMENT_INPUT_TIMEOUT_TICKS = 8;
    private static final String INSTANCE_TAG = "TypeMoonAddonImaginarySpaceInstance";
    private static final String TRANSIENT_TAG = "TypeMoonAddonImaginarySpaceTransient";
    private static final String ENTER_EFFECT = "typemoonworld:imaginary_space_enter";
    private static final String EXIT_EFFECT = "typemoonworld:imaginary_space_exit";
    private static final String AMBIENT_EFFECT = "typemoonworld:imaginary_space_ambient";
    private static final String ANOMALY_EFFECT = "typemoonworld:imaginary_space_anomaly";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Map<UUID, InstanceState> INSTANCES = new LinkedHashMap<>();
    private static final Map<UUID, UUID> PLAYER_INSTANCES = new HashMap<>();
    private static final Map<UUID, PendingCreatureTransfer> PENDING_CREATURE_TRANSFERS =
            new LinkedHashMap<>();
    /** Edge-trigger latch: one random depth shift per boundary contact. */
    private static final Set<UUID> BOUNDARY_CONTACT_LATCHED = new HashSet<>();
    private static final Map<UUID, VerticalMovementInput> VERTICAL_MOVEMENT_INPUTS = new HashMap<>();
    private static final Set<UUID> CONTROLLED_TRANSITIONS = new HashSet<>();
    private static final Map<String, Integer> SPAWN_FAILURES = new HashMap<>();
    private static PoolSnapshot pools = PoolSnapshot.empty();
    private static int poolConfigSignature = Integer.MIN_VALUE;
    private static boolean daylightOriginal;
    private static boolean daylightSnapshotPresent;
    private static final Set<UUID> DAYLIGHT_OVERRIDES = new HashSet<>();

    private ImaginarySpaceService() {
    }

    public static double manaCost(ServerPlayer player) {
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        return data.mode() == ImaginarySpaceData.CREATURE_MODE
                ? CREATURE_MANA_COST
                : SELF_MANA_COST;
    }

    public static CastOutcome cast(ServerPlayer caster) {
        if (!isValidCaster(caster)) {
            return CastOutcome.rejected();
        }
        ServerLevel imaginaryLevel = caster.getServer().getLevel(DIMENSION);
        if (imaginaryLevel == null) {
            return CastOutcome.dimensionUnavailable();
        }

        ImaginarySpaceData data = caster.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (isImaginarySpace(caster.serverLevel())) {
            return data.mode() == ImaginarySpaceData.CREATURE_MODE
                    ? exitNearbyPlayers(caster)
                    : exitSelf(caster);
        }
        return data.mode() == ImaginarySpaceData.CREATURE_MODE
                ? enterNearbyCreatures(caster, imaginaryLevel)
                : enterSelf(caster, imaginaryLevel);
    }

    public static void switchMode(ServerPlayer player) {
        if (!isValidCaster(player)) {
            return;
        }
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        vars.rebuildSelectedMagicsFromActiveWheel();
        PlayerMagicSelectionService.prepareCurrentSelection(player, vars);
        if (!PlayerMagicSelectionService.isCurrentSelection(vars, ImaginarySpaceMagic.MAGIC_ID)) {
            return;
        }

        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        int mode = data.cycleMode();
        syncData(player, data);
        player.displayClientMessage(Component.translatable(mode == ImaginarySpaceData.CREATURE_MODE
                ? "message.typemoonworld.imaginary_space.mode.creatures"
                : "message.typemoonworld.imaginary_space.mode.self"), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.PLAYERS, 0.55F, mode == ImaginarySpaceData.CREATURE_MODE ? 1.28F : 0.92F);
    }

    /** Applies a validated depth change through the same rebuild path used by the world itself. */
    public static DepthChangeResult adjustDepth(ServerPlayer player, double requestedDepth) {
        if (player == null || !isImaginarySpace(player.serverLevel())) {
            return DepthChangeResult.NOT_IN_SPACE;
        }
        if (!Double.isFinite(requestedDepth)
                || requestedDepth < 0.0D
                || requestedDepth > MAX_DEPTH) {
            return DepthChangeResult.INVALID;
        }

        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (!data.active() || data.instanceId() == null) {
            return DepthChangeResult.REJECTED;
        }
        double depth = Math.max(0.0D, Math.min(MAX_DEPTH, requestedDepth));
        if (Double.compare(data.depth(), depth) == 0) {
            return DepthChangeResult.UNCHANGED;
        }

        if (depth <= 0.0D) {
            // Reuse the established return, correction, invulnerability and cleanup path.
            data.setDepth(depth);
            return floatToRespawn(player, data)
                    ? DepthChangeResult.CHANGED
                    : DepthChangeResult.REJECTED;
        }

        InstanceState state = INSTANCES.get(data.instanceId());
        if (state == null
                || !state.instanceId.equals(data.instanceId())
                || !state.ownerId.equals(player.getUUID())) {
            return DepthChangeResult.REJECTED;
        }
        data.setDepth(depth);
        rebuildScene(player, data, state);
        syncData(player, data);
        return DepthChangeResult.CHANGED;
    }

    /** Compatibility entry point for existing callers; new controls should use adjustDepth. */
    public static boolean setDepth(ServerPlayer player, double depth) {
        return adjustDepth(player, depth) == DepthChangeResult.CHANGED;
    }

    /** Accepts only the local player's clamped vertical intent while in an active instance. */
    public static void handleMovementInput(ServerPlayer player, int verticalInput) {
        if (player == null || !isImaginarySpace(player.serverLevel())) {
            return;
        }
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (!data.active() || !isValidCaster(player)) {
            VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
            return;
        }
        int input = Math.max(-1, Math.min(1, verticalInput));
        VERTICAL_MOVEMENT_INPUTS.put(player.getUUID(), new VerticalMovementInput(
                input, player.serverLevel().getGameTime()));
    }

    /** Applies water-like damping and a bounded jump/sneak vertical impulse. */
    public static void applySwimmingMovement(Player player, int verticalInput) {
        if (player == null || !player.isAlive() || player.isRemoved()) {
            return;
        }
        Vec3 movement = player.getDeltaMovement();
        if (!isFinite(movement)) {
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        int input = Math.max(-1, Math.min(1, verticalInput));
        double x = movement.x * SWIM_HORIZONTAL_DAMPING;
        double y = movement.y * SWIM_VERTICAL_DAMPING
                + input * SWIM_VERTICAL_ACCELERATION;
        double z = movement.z * SWIM_HORIZONTAL_DAMPING;
        y = Math.max(-SWIM_MAX_VERTICAL_SPEED, Math.min(SWIM_MAX_VERTICAL_SPEED, y));
        player.setDeltaMovement(x, y, z);
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (!isImaginarySpace(player.serverLevel())) {
            VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
            clearSwimmingState(player);
            if (data.active() && !CONTROLLED_TRANSITIONS.contains(player.getUUID())) {
                cleanupDetachedSession(player, data, true);
            }
            return;
        }
        if (!data.active()) {
            VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
            clearSwimmingState(player);
            floatUntrackedPlayer(player);
            return;
        }

        ensureRuntimeInstance(player, data);
        player.setNoGravity(true);
        if (handleWorldBoundary(player, data)) {
            return;
        }
        updateSwimmingState(player, player.isSprinting());
        player.fallDistance = 0.0F;
        VerticalMovementInput input = VERTICAL_MOVEMENT_INPUTS.get(player.getUUID());
        long gameTime = player.serverLevel().getGameTime();
        int verticalInput = input != null
                && gameTime - input.lastUpdateTick <= MOVEMENT_INPUT_TIMEOUT_TICKS
                ? input.direction
                : 0;
        if (input != null && verticalInput == 0 &&
                gameTime - input.lastUpdateTick > MOVEMENT_INPUT_TIMEOUT_TICKS) {
            VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
        }
        applySwimmingMovement(player, verticalInput);
        data.tickSession();
        data.tickInvulnerability();

        if (data.depth() <= 0.0D && data.sessionTicks() >= 1) {
            floatToRespawn(player, data);
            return;
        }
        if (data.outerGodCountdown() >= 0 && data.tickOuterGodCountdown()) {
            killForOuterGod(player, data);
            return;
        }
        if (data.depth() >= 100.0D && data.outerGodCountdown() < 0) {
            startOuterGodEvent(player, data);
        }

        if (data.advanceExistenceTicker(EXISTENCE_INTERVAL_TICKS)) {
            settleExistence(player, data);
            if (!player.isAlive() || !data.active()) {
                return;
            }
        }
        if (data.advanceTimeTicker(TIME_OFFSET_INTERVAL_TICKS)) {
            settleTimeOffset(player, data);
            maybeTriggerLayerEvent(player, data);
        }

        if (player.serverLevel().getGameTime() % 20L == 0L) {
            syncData(player, data);
        }
    }

    public static void tickServer(MinecraftServer server) {
        if (server == null) {
            return;
        }
        processPendingCreatureTransfers(server);
        ensurePools(server);
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null || INSTANCES.isEmpty()) {
            return;
        }
        int perInstanceBudget = ImaginarySpaceConfig.TASKS_PER_TICK.get();
        for (InstanceState state : List.copyOf(INSTANCES.values())) {
            ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerId);
            if (owner == null || !isImaginarySpace(owner.serverLevel())) {
                continue;
            }
            ImaginarySpaceData data = owner.getData(ImaginarySpaceAttachments.PLAYER_STATE);
            if (!data.active() || !state.instanceId.equals(data.instanceId())) {
                continue;
            }
            ChunkPos currentChunk = owner.chunkPosition();
            if (!currentChunk.equals(state.windowCenterChunk)) {
                updateChunkWindow(owner, data, state, currentChunk);
            } else if (shouldRefreshGenerationCenter(owner, state)) {
                updateChunkWindow(owner, data, state, currentChunk, true);
            }
            if (state.advanceRefreshTicker(CONTENT_REFRESH_INTERVAL_TICKS)) {
                rebuildScene(owner, data, state);
            }
            processSpawnQueue(level, state, perInstanceBudget);
        }
    }

    public static void onLogin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
        BOUNDARY_CONTACT_LATCHED.remove(player.getUUID());
        ensurePools(player.getServer());
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (isImaginarySpace(player.serverLevel())) {
            if (data.active()) {
                exitToRecordedReturn(player, data, true, "message.typemoonworld.imaginary_space.recovered");
            } else {
                floatUntrackedPlayer(player);
            }
            return;
        }
        if (data.active()) {
            cleanupDetachedSession(player, data, false);
        }
        syncData(player, data);
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        cancelPendingTransfersFor(player.getServer(), player.getUUID());
        clearSwimmingState(player);
        VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
        BOUNDARY_CONTACT_LATCHED.remove(player.getUUID());
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (!data.active()) {
            return;
        }
        InstanceState state = INSTANCES.remove(data.instanceId());
        PLAYER_INSTANCES.remove(player.getUUID());
        ServerLevel imaginaryLevel = player.getServer().getLevel(DIMENSION);
        if (state != null && imaginaryLevel != null) {
            clearGeneratedContent(imaginaryLevel, state);
        }
        releaseHighDepthOverride(player, data);
        player.setNoGravity(data.previousNoGravity());
        player.getPersistentData().remove(INSTANCE_TAG);
        data.clearSession();
    }

    public static void onDeath(ServerPlayer player) {
        if (player == null) {
            return;
        }
        cancelPendingTransfersFor(player.getServer(), player.getUUID());
        clearSwimmingState(player);
        VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
        BOUNDARY_CONTACT_LATCHED.remove(player.getUUID());
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (!data.active()) {
            return;
        }
        cleanupDetachedSession(player, data, false);
    }

    public static void onChangedDimension(
            ServerPlayer player,
            ResourceKey<Level> from,
            ResourceKey<Level> to
    ) {
        if (player == null) {
            return;
        }
        // Controlled transitions also end the previous boundary contact.
        BOUNDARY_CONTACT_LATCHED.remove(player.getUUID());
        if (CONTROLLED_TRANSITIONS.contains(player.getUUID())) {
            return;
        }
        cancelPendingTransfersFor(player.getServer(), player.getUUID());
        clearSwimmingState(player);
        VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
        BOUNDARY_CONTACT_LATCHED.remove(player.getUUID());
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (DIMENSION.equals(from) && data.active()) {
            double correction = data.timeOffset();
            cleanupDetachedSession(player, data, true);
            applyWorldCorrection(player, correction);
        } else if (DIMENSION.equals(to) && !data.active()) {
            floatUntrackedPlayer(player);
        }
    }

    public static boolean isEntryInvulnerable(ServerPlayer player) {
        if (player == null || !isImaginarySpace(player.serverLevel())) {
            return false;
        }
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        return data.active() && data.invulnerabilityTicks() > 0;
    }

    public static boolean allowOuterGodEffect(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        return data.active() && data.depth() >= 100.0D && data.outerGodCountdown() >= 0;
    }

    /** Learning Imaginary Dive grants immunity to hazards produced by this dimension itself. */
    public static boolean hasImaginaryDiveImmunity(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        return vars.hasLearnedSelfMagic(com.example.typemoonaddon.magic.ImaginaryDiveMagic.MAGIC_ID);
    }

    /** Void and world-border damage are disabled inside the unbounded imaginary-space play area. */
    public static boolean isBoundaryDamage(DamageSource source) {
        return source != null
                && (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(WORLD_BORDER_DAMAGE));
    }

    public static boolean isTransientVictim(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBoolean(TRANSIENT_TAG);
    }

    public static void stopServer(MinecraftServer server) {
        if (server != null) {
            ServerLevel level = server.getLevel(DIMENSION);
            if (level != null) {
                for (InstanceState state : List.copyOf(INSTANCES.values())) {
                    clearGeneratedContent(level, state);
                }
            }
            restoreDaylightRule(server);
        }
        if (!SPAWN_FAILURES.isEmpty()) {
            TypeMoonAddon.LOGGER.info("Imaginary-space skipped entries by reason: {}", SPAWN_FAILURES);
        }
        INSTANCES.clear();
        PLAYER_INSTANCES.clear();
        discardPendingStorageVisuals(server);
        PENDING_CREATURE_TRANSFERS.clear();
        VERTICAL_MOVEMENT_INPUTS.clear();
        BOUNDARY_CONTACT_LATCHED.clear();
        CONTROLLED_TRANSITIONS.clear();
        DAYLIGHT_OVERRIDES.clear();
    }

    private static CastOutcome enterSelf(ServerPlayer caster, ServerLevel imaginaryLevel) {
        return enterPlayer(caster, imaginaryLevel)
                ? CastOutcome.success(1)
                : CastOutcome.rejected();
    }

    private static CastOutcome enterNearbyCreatures(ServerPlayer caster, ServerLevel imaginaryLevel) {
        Vec3 center = caster.getBoundingBox().getCenter();
        AABB query = new AABB(center, center).inflate(TARGET_RADIUS);
        List<LivingEntity> targets = caster.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                query,
                target -> isTeleportCandidate(caster, target, center, imaginaryLevel));
        targets.sort(Comparator
                .comparingDouble((LivingEntity target) ->
                        target.getBoundingBox().getCenter().distanceToSqr(center))
                .thenComparingInt(LivingEntity::getId));
        if (targets.isEmpty()) {
            return CastOutcome.noTarget();
        }

        int affected = 0;
        for (LivingEntity target : targets) {
            if (beginCreatureTransfer(caster, target, imaginaryLevel)) {
                affected++;
            }
        }
        return affected > 0 ? CastOutcome.success(affected) : CastOutcome.noTarget();
    }

    /** Starts one independent storage window; the actual dimension change is deferred. */
    private static boolean beginCreatureTransfer(
            ServerPlayer caster,
            LivingEntity target,
            ServerLevel imaginaryLevel
    ) {
        if (caster == null || target == null || imaginaryLevel == null
                || PENDING_CREATURE_TRANSFERS.containsKey(target.getUUID())
                || !(target.level() instanceof ServerLevel source)
                || !target.isAlive()
                || target.isRemoved()
                || !target.canChangeDimensions(source, imaginaryLevel)) {
            return false;
        }

        Vec3 anchor = target.position();
        StorageVisualEntity visual = new StorageVisualEntity(
                source,
                anchor.add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                CREATURE_STORAGE_DURATION_TICKS,
                target.getUUID());
        if (!source.addFreshEntity(visual)) {
            return false;
        }
        PendingCreatureTransfer pending = new PendingCreatureTransfer(
                caster.getUUID(),
                target.getUUID(),
                source.dimension(),
                anchor,
                source.getGameTime() + CREATURE_STORAGE_DURATION_TICKS,
                -1L,
                visual.getUUID());
        PENDING_CREATURE_TRANSFERS.put(target.getUUID(), pending);
        source.playSound(null, target.blockPosition(), AddonSounds.IMAGINARY_STORAGE_START.get(),
                SoundSource.PLAYERS, 1.25F, 1.0F);
        holdCreatureInStorage(target, anchor);
        return true;
    }

    /** Resolves and advances all pending targets on the server tick. */
    private static void processPendingCreatureTransfers(MinecraftServer server) {
        if (PENDING_CREATURE_TRANSFERS.isEmpty()) {
            return;
        }
        ServerLevel imaginaryLevel = server.getLevel(DIMENSION);
        if (imaginaryLevel == null) {
            discardPendingStorageVisuals(server);
            PENDING_CREATURE_TRANSFERS.clear();
            return;
        }

        Iterator<Map.Entry<UUID, PendingCreatureTransfer>> iterator =
                PENDING_CREATURE_TRANSFERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingCreatureTransfer> entry = iterator.next();
            PendingCreatureTransfer pending = entry.getValue();
            ServerPlayer caster = server.getPlayerList().getPlayer(pending.casterId());
            ServerLevel source = server.getLevel(pending.sourceDimension());
            LivingEntity target = source == null
                    ? null
                    : source.getEntity(pending.targetId()) instanceof LivingEntity living ? living : null;

            if (!isPendingTransferValid(caster, source, target, pending)) {
                discardStorageVisual(source, pending.visualId());
                iterator.remove();
                continue;
            }

            holdCreatureInStorage(target, pending.anchor());
            long gameTime = source.getGameTime();
            StorageVisualEntity visual = findStorageVisual(source, pending.visualId());
            if (visual == null && (gameTime & 1L) == 0L) {
                emitStorageFallbackParticles(source, pending.anchor(), pending.collapseAt() >= 0L);
            }
            if (pending.collapseAt() < 0L) {
                if (gameTime < pending.completeAt()) {
                    continue;
                }

                if (visual != null) {
                    visual.startCollapse(CREATURE_STORAGE_COLLAPSE_TICKS);
                }
                source.playSound(null, BlockPos.containing(pending.anchor()), AddonSounds.IMAGINARY_STORAGE_END.get(),
                        SoundSource.PLAYERS, 1.35F, 1.0F);
                entry.setValue(pending.startCollapse(gameTime + CREATURE_STORAGE_COLLAPSE_TICKS));
                continue;
            }
            if (gameTime < pending.collapseAt()) {
                continue;
            }

            boolean transferred;
            if (target instanceof ServerPlayer player) {
                transferred = enterPlayer(player, imaginaryLevel);
            } else {
                transferred = teleportAndKill(caster, target, imaginaryLevel);
            }
            if (transferred) {
                // The target dimension change is submitted first; remove the prism
                // only after the server has accepted the transfer in this same tick.
                discardStorageVisual(source, pending.visualId());
                iterator.remove();
            }
        }
    }

    /**
     * Keeps the storage silhouette visible even if a client has not yet resolved the
     * bound entity for the data-driven VFX packet. This is only a visual fallback.
     */
    private static void emitStorageFallbackParticles(
            ServerLevel source,
            Vec3 anchor,
            boolean collapsing
    ) {
        if (source == null || anchor == null || !isFinite(anchor)) {
            return;
        }
        if (collapsing) {
            source.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    anchor.x, anchor.y + 0.9D, anchor.z,
                    10, 0.9D, 1.0D, 0.9D, 0.06D);
            source.sendParticles(ParticleTypes.END_ROD,
                    anchor.x, anchor.y + 0.9D, anchor.z,
                    3, 0.65D, 0.85D, 0.65D, 0.015D);
            return;
        }

        source.sendParticles(ParticleTypes.PORTAL,
                anchor.x, anchor.y + 0.9D, anchor.z,
                8, 0.75D, 1.0D, 0.75D, 0.08D);
        source.sendParticles(ParticleTypes.END_ROD,
                anchor.x, anchor.y + 0.9D, anchor.z,
                4, 0.8D, 1.1D, 0.8D, 0.02D);
        double half = 1.5D;
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    source.sendParticles(ParticleTypes.END_ROD,
                            anchor.x + x * half,
                            anchor.y + 0.9D + y * half,
                            anchor.z + z * half,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private static boolean isPendingTransferValid(
            ServerPlayer caster,
            ServerLevel source,
            LivingEntity target,
            PendingCreatureTransfer pending
    ) {
        return caster != null
                && isValidCaster(caster)
                && source != null
                && caster.serverLevel() == source
                && target != null
                && target.level() == source
                && target.isAlive()
                && !target.isRemoved()
                && target.getUUID().equals(pending.targetId())
                && target.canChangeDimensions(source, caster.getServer().getLevel(DIMENSION));
    }

    private static StorageVisualEntity findStorageVisual(ServerLevel source, UUID visualId) {
        if (source == null || visualId == null) {
            return null;
        }
        Entity entity = source.getEntity(visualId);
        return entity instanceof StorageVisualEntity visual && !visual.isRemoved() ? visual : null;
    }

    private static void discardStorageVisual(ServerLevel source, UUID visualId) {
        StorageVisualEntity visual = findStorageVisual(source, visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    /** Keeps the target at the captured position without changing its persistent AI state. */
    private static void holdCreatureInStorage(LivingEntity target, Vec3 anchor) {
        if (target == null || anchor == null || !isFinite(anchor)) {
            return;
        }
        target.teleportTo(anchor.x, anchor.y, anchor.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        target.hurtMarked = true;
    }

    private static void cancelPendingTransfersFor(MinecraftServer server, UUID entityId) {
        if (entityId == null || PENDING_CREATURE_TRANSFERS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, PendingCreatureTransfer>> iterator =
                PENDING_CREATURE_TRANSFERS.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingCreatureTransfer pending = iterator.next().getValue();
            if (entityId.equals(pending.casterId()) || entityId.equals(pending.targetId())) {
                discardStorageVisual(
                        server == null ? null : server.getLevel(pending.sourceDimension()),
                        pending.visualId());
                iterator.remove();
            }
        }
    }

    /** Removes all local storage visuals before a server/level-wide queue reset. */
    private static void discardPendingStorageVisuals(MinecraftServer server) {
        if (PENDING_CREATURE_TRANSFERS.isEmpty()) {
            return;
        }
        for (PendingCreatureTransfer pending : PENDING_CREATURE_TRANSFERS.values()) {
            discardStorageVisual(
                    server == null ? null : server.getLevel(pending.sourceDimension()),
                    pending.visualId());
        }
    }

    private static CastOutcome exitSelf(ServerPlayer caster) {
        ImaginarySpaceData data = caster.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (!data.active()) {
            return CastOutcome.rejected();
        }
        return exitToRecordedReturn(caster, data, true,
                "message.typemoonworld.imaginary_space.exited")
                ? CastOutcome.success(1)
                : CastOutcome.rejected();
    }

    private static CastOutcome exitNearbyPlayers(ServerPlayer caster) {
        ImaginarySpaceData casterData = caster.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (!casterData.active()) {
            return CastOutcome.rejected();
        }
        Vec3 center = caster.getBoundingBox().getCenter();
        AABB query = new AABB(center, center).inflate(TARGET_RADIUS);
        List<ServerPlayer> targets = caster.serverLevel().getEntitiesOfClass(
                ServerPlayer.class,
                query,
                target -> target != caster
                        && target.isAlive()
                        && !target.isSpectator()
                        && target.getBoundingBox().getCenter().distanceToSqr(center) <= TARGET_RADIUS_SQUARED
                        && sameInstance(casterData, target));
        targets.sort(Comparator
                .comparingDouble((ServerPlayer target) ->
                        target.getBoundingBox().getCenter().distanceToSqr(center))
                .thenComparingInt(ServerPlayer::getId));
        if (targets.isEmpty()) {
            return CastOutcome.noTarget();
        }
        int affected = 0;
        for (ServerPlayer target : targets) {
            ImaginarySpaceData targetData = target.getData(ImaginarySpaceAttachments.PLAYER_STATE);
            if (exitToRecordedReturn(target, targetData, true,
                    "message.typemoonworld.imaginary_space.exited")) {
                affected++;
            }
        }
        return affected > 0 ? CastOutcome.success(affected) : CastOutcome.noTarget();
    }

    private static boolean enterPlayer(ServerPlayer player, ServerLevel imaginaryLevel) {
        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (data.active() || isImaginarySpace(player.serverLevel())
                || !player.canChangeDimensions(player.serverLevel(), imaginaryLevel)) {
            return false;
        }
        ServerLevel source = player.serverLevel();
        Vec3 sourcePos = player.position();
        Vec3 safeReturn = findSafeReturn(source, player, sourcePos, sourcePos);
        Vec3 destination = allocateInstanceCenter();
        UUID instanceId = UUID.randomUUID();
        ChunkPos entryChunk = new ChunkPos(BlockPos.containing(destination));
        data.beginSession(
                instanceId,
                source.dimension().location().toString(),
                sourcePos.x,
                sourcePos.y,
                sourcePos.z,
                player.getYRot(),
                player.getXRot(),
                safeReturn.x,
                safeReturn.y,
                safeReturn.z,
                destination.x,
                destination.y,
                destination.z,
                entryChunk.x,
                entryChunk.z,
                player.isNoGravity());

        InstanceState state = new InstanceState(instanceId, player.getUUID(), destination, entryChunk);
        INSTANCES.put(instanceId, state);
        PLAYER_INSTANCES.put(player.getUUID(), instanceId);
        player.getPersistentData().putUUID(INSTANCE_TAG, instanceId);
        VFXServerEffects.spawn(source, ENTER_EFFECT, player.getBoundingBox().getCenter(), VFX_OBSERVER_RADIUS);
        source.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS, 0.7F, 0.72F);

        try {
            CONTROLLED_TRANSITIONS.add(player.getUUID());
            player.teleportTo(imaginaryLevel, destination.x, destination.y, destination.z,
                    player.getYRot(), player.getXRot());
        } catch (RuntimeException exception) {
            TypeMoonAddon.LOGGER.error("Failed to enter imaginary space for {}", player.getGameProfile().getName(), exception);
            INSTANCES.remove(instanceId);
            PLAYER_INSTANCES.remove(player.getUUID());
            player.getPersistentData().remove(INSTANCE_TAG);
            data.clearSession();
            return false;
        } finally {
            CONTROLLED_TRANSITIONS.remove(player.getUUID());
        }
        if (!isImaginarySpace(player.serverLevel())) {
            INSTANCES.remove(instanceId);
            PLAYER_INSTANCES.remove(player.getUUID());
            player.getPersistentData().remove(INSTANCE_TAG);
            data.clearSession();
            return false;
        }

        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        VFXServerEffects.spawn(imaginaryLevel, ENTER_EFFECT, destination, VFX_OBSERVER_RADIUS);
        VFXServerEffects.spawn(imaginaryLevel, AMBIENT_EFFECT, destination, VFX_OBSERVER_RADIUS);
        imaginaryLevel.playSound(null, player.blockPosition(), SoundEvents.PORTAL_AMBIENT,
                SoundSource.AMBIENT, 0.8F, 0.58F);
        rebuildScene(player, data, state, true);
        syncData(player, data);
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.imaginary_space.entered"), true);
        return true;
    }

    private static boolean teleportAndKill(
            ServerPlayer caster,
            LivingEntity target,
            ServerLevel imaginaryLevel
    ) {
        if (EntityUtils.isImmunePlayerTarget(target)) {
            return false;
        }
        if (!target.canChangeDimensions(target.level(), imaginaryLevel)) {
            return false;
        }
        Vec3 destination = allocateInstanceCenter();
        VFXServerEffects.spawn(caster.serverLevel(), ENTER_EFFECT,
                target.getBoundingBox().getCenter(), VFX_OBSERVER_RADIUS);
        try {
            target.getPersistentData().putBoolean(TRANSIENT_TAG, true);
            Entity moved = target.changeDimension(new DimensionTransition(
                    imaginaryLevel,
                    destination,
                    Vec3.ZERO,
                    target.getYRot(),
                    target.getXRot(),
                    DimensionTransition.DO_NOTHING));
            if (!(moved instanceof LivingEntity living)) {
                return false;
            }
            living.getPersistentData().putBoolean(TRANSIENT_TAG, true);
            living.setNoGravity(true);
            living.setInvulnerable(false);
            living.invulnerableTime = 0;
            VFXServerEffects.spawn(imaginaryLevel, EXIT_EFFECT,
                    living.getBoundingBox().getCenter(), VFX_OBSERVER_RADIUS);
            DamageSource source = caster.damageSources().source(DamageTypes.GENERIC_KILL, caster);
            living.hurt(source, Float.MAX_VALUE);
            if (living.isAlive()) {
                living.setHealth(0.0F);
                living.die(source);
            }
            return true;
        } catch (RuntimeException exception) {
            TypeMoonAddon.LOGGER.warn("Imaginary-space transfer rejected entity {}", target.getType(), exception);
            target.getPersistentData().remove(TRANSIENT_TAG);
            return false;
        }
    }

    private static boolean exitToRecordedReturn(
            ServerPlayer player,
            ImaginarySpaceData data,
            boolean applyCorrection,
            String messageKey
    ) {
        MinecraftServer server = player.getServer();
        ServerLevel returnLevel = resolveReturnLevel(server, data.returnDimension());
        Vec3 requested = new Vec3(data.returnX(), data.returnY(), data.returnZ());
        Vec3 fallback = new Vec3(data.safeX(), data.safeY(), data.safeZ());
        Vec3 destination = findSafeReturn(returnLevel, player, requested, fallback);
        double correction = data.timeOffset();
        boolean result = teleportOut(
                player,
                data,
                returnLevel,
                destination,
                data.returnYaw(),
                data.returnPitch());
        if (result && applyCorrection) {
            applyWorldCorrection(player, correction);
        }
        if (result && messageKey != null) {
            player.displayClientMessage(Component.translatable(messageKey), true);
        }
        return result;
    }

    private static boolean floatToRespawn(ServerPlayer player, ImaginarySpaceData data) {
        DimensionTransition transition = player.findRespawnPositionAndUseSpawnBlock(
                false, DimensionTransition.DO_NOTHING);
        double correction = data.timeOffset();
        boolean result = teleportOut(player, data, transition.newLevel(), transition.pos(),
                transition.yRot(), transition.xRot());
        if (result) {
            applyWorldCorrection(player, correction);
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_space.floated"), true);
        }
        return result;
    }

    private static void floatUntrackedPlayer(ServerPlayer player) {
        if (player == null || CONTROLLED_TRANSITIONS.contains(player.getUUID())) {
            return;
        }
        clearSwimmingState(player);
        DimensionTransition transition = player.findRespawnPositionAndUseSpawnBlock(
                false, DimensionTransition.DO_NOTHING);
        try {
            CONTROLLED_TRANSITIONS.add(player.getUUID());
            player.setNoGravity(false);
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(transition.newLevel(), transition.pos().x, transition.pos().y,
                    transition.pos().z, transition.yRot(), transition.xRot());
        } finally {
            CONTROLLED_TRANSITIONS.remove(player.getUUID());
        }
    }

    private static boolean teleportOut(
            ServerPlayer player,
            ImaginarySpaceData data,
            ServerLevel destinationLevel,
            Vec3 destination,
            float yaw,
            float pitch
    ) {
        if (destinationLevel == null || !isFinite(destination)) {
            return false;
        }
        ServerLevel source = player.serverLevel();
        UUID instanceId = data.instanceId();
        InstanceState state = instanceId == null ? null : INSTANCES.remove(instanceId);
        PLAYER_INSTANCES.remove(player.getUUID());
        if (state != null && isImaginarySpace(source)) {
            clearGeneratedContent(source, state);
        }
        releaseHighDepthOverride(player, data);
        VFXServerEffects.spawn(source, EXIT_EFFECT, player.getBoundingBox().getCenter(), VFX_OBSERVER_RADIUS);
        clearSwimmingState(player);
        player.setNoGravity(data.previousNoGravity());
        VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        try {
            CONTROLLED_TRANSITIONS.add(player.getUUID());
            player.teleportTo(destinationLevel, destination.x, destination.y, destination.z, yaw, pitch);
        } catch (RuntimeException exception) {
            TypeMoonAddon.LOGGER.error("Failed to leave imaginary space for {}", player.getGameProfile().getName(), exception);
            return false;
        } finally {
            CONTROLLED_TRANSITIONS.remove(player.getUUID());
        }

        player.getPersistentData().remove(INSTANCE_TAG);
        data.clearSession();
        syncData(player, data);
        VFXServerEffects.spawn(destinationLevel, EXIT_EFFECT,
                player.getBoundingBox().getCenter(), VFX_OBSERVER_RADIUS);
        destinationLevel.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS, 0.72F, 1.18F);
        return true;
    }

    private static void cleanupDetachedSession(
            ServerPlayer player,
            ImaginarySpaceData data,
            boolean sync
    ) {
        UUID instanceId = data.instanceId();
        InstanceState state = instanceId == null ? null : INSTANCES.remove(instanceId);
        PLAYER_INSTANCES.remove(player.getUUID());
        ServerLevel imaginaryLevel = player.getServer().getLevel(DIMENSION);
        if (state != null && imaginaryLevel != null) {
            clearGeneratedContent(imaginaryLevel, state);
        }
        releaseHighDepthOverride(player, data);
        clearSwimmingState(player);
        player.setNoGravity(data.previousNoGravity());
        VERTICAL_MOVEMENT_INPUTS.remove(player.getUUID());
        player.getPersistentData().remove(INSTANCE_TAG);
        data.clearSession();
        if (sync) {
            syncData(player, data);
        }
    }

    private static void ensureRuntimeInstance(ServerPlayer player, ImaginarySpaceData data) {
        UUID instanceId = data.instanceId();
        if (instanceId == null || INSTANCES.containsKey(instanceId)) {
            return;
        }
        Vec3 center = new Vec3(data.centerX(), data.centerY(), data.centerZ());
        ChunkPos chunk = new ChunkPos(data.entryChunkX(), data.entryChunkZ());
        InstanceState state = new InstanceState(instanceId, player.getUUID(), center, chunk);
        INSTANCES.put(instanceId, state);
        PLAYER_INSTANCES.put(player.getUUID(), instanceId);
        rebuildScene(player, data, state);
    }

    private static void rebuildScene(
            ServerPlayer player,
            ImaginarySpaceData data,
            InstanceState state
    ) {
        rebuildScene(player, data, state, false);
    }

    private static void rebuildScene(
            ServerPlayer player,
            ImaginarySpaceData data,
            InstanceState state,
            boolean fillSphereImmediately
    ) {
        ensurePools(player.getServer());
        clearGeneratedContent(player.serverLevel(), state);
        state.windowCenterChunk = player.chunkPosition();
        state.generationCenter = player.position();
        state.generatedChunks.clear();
        state.generatedChunks.addAll(windowAround(state.windowCenterChunk));
        keepInstanceLoaded(player.serverLevel(), state);
        int generation = data.nextGeneration();
        state.generation = generation;
        state.random = newRandom();
        state.refreshTicks = 0;
        data.markContentsGenerated();

        double depth = data.depth();
        state.depth = depth;
        // Depth 100 is reserved for the outer-god event and deliberately has no generated content.
        int blockCount = scaledCount(ImaginarySpaceConfig.MAX_BLOCKS.get(), BASE_BLOCK_COUNT, depth);
        int itemCount = scaledCount(ImaginarySpaceConfig.MAX_ITEMS.get(), BASE_ITEM_COUNT, depth);
        int mobCount = scaledMobCount(ImaginarySpaceConfig.MAX_MOBS.get(), depth);
        if (fillSphereImmediately && depth < MAX_DEPTH) {
            fillImmediateRandomSphere(player.serverLevel(), state);
            blockCount = 0;
        }
        queueContentForChunks(state, generation, state.generatedChunks,
                blockCount, itemCount, mobCount);
        queueNamedVisitors(state, generation);

        if (depth < 80.0D && data.highDepthOverride()) {
            releaseHighDepthOverride(player, data);
        } else if (depth >= 80.0D && !data.highDepthOverride()) {
            double probability = Math.min(1.0D, Math.max(0.0D, (depth - 79.0D) / 40.0D));
            if (state.random.nextDouble() < probability) {
                acquireHighDepthOverride(player, data, state.instanceId);
            }
        }
        if (depth >= 100.0D) {
            startOuterGodEvent(player, data);
        }
    }

    /**
     * Moves a player away from the vanilla/build-height boundary and changes the
     * current depth through the normal server-side scene rebuild path.
     */
    private static boolean handleWorldBoundary(ServerPlayer player, ImaginarySpaceData data) {
        if (player == null || data == null || !data.active()
                || CONTROLLED_TRANSITIONS.contains(player.getUUID())) {
            return false;
        }

        UUID playerId = player.getUUID();
        if (!isAtWorldBoundary(player)) {
            // Re-arm only after the player has actually left the boundary zone.
            BOUNDARY_CONTACT_LATCHED.remove(playerId);
            return false;
        }
        if (BOUNDARY_CONTACT_LATCHED.contains(playerId)) {
            // This contact was already handled. Continue normal movement so the
            // player can leave the boundary and re-arm the next contact.
            return false;
        }

        InstanceState state = INSTANCES.get(data.instanceId());
        if (state == null || !state.ownerId.equals(playerId)) {
            return false;
        }

        int targetDepth = randomBoundaryDepth(data.depth());
        if (targetDepth == 0) {
            // Depth zero is the surface/exit outcome, not an in-instance rebuild.
            // Reuse the established safe respawn and cleanup path so the player is
            // returned directly to the recorded main-world spawn position.
            data.setDepth(0.0D);
            floatToRespawn(player, data);
            return true;
        }

        ServerLevel level = player.serverLevel();
        var border = level.getWorldBorder();
        double targetX = clampToBoundary(state.center.x, border.getMinX(), border.getMaxX());
        double targetZ = clampToBoundary(state.center.z, border.getMinZ(), border.getMaxZ());
        double targetY = Math.max(level.getMinBuildHeight() + 2.0D,
                Math.min(level.getMaxBuildHeight() - player.getBbHeight() - 2.0D, state.center.y));

        try {
            CONTROLLED_TRANSITIONS.add(playerId);
            player.teleportTo(level, targetX, targetY, targetZ, player.getYRot(), player.getXRot());
        } catch (RuntimeException exception) {
            TypeMoonAddon.LOGGER.warn("Failed to re-enter Imaginary Space after boundary contact for {}",
                    player.getGameProfile().getName(), exception);
            return true;
        } finally {
            CONTROLLED_TRANSITIONS.remove(playerId);
        }

        // Latch immediately after a successful teleport so duplicate tick/event
        // delivery cannot perform another random depth transfer at the same contact.
        BOUNDARY_CONTACT_LATCHED.add(playerId);
        if (targetDepth < (int) MAX_DEPTH && data.outerGodCountdown() >= 0) {
            data.clearOuterGodCountdown();
        }
        data.setDepth(targetDepth);
        rebuildScene(player, data, state);
        syncData(player, data);
        VFXServerEffects.spawn(level, ANOMALY_EFFECT, player.getBoundingBox().getCenter(), VFX_OBSERVER_RADIUS);
        level.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS, 0.8F, 0.72F);
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.imaginary_space.boundary_depth", targetDepth), true);
        return true;
    }

    private static boolean isAtWorldBoundary(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!isImaginarySpace(level)) {
            return false;
        }
        var border = level.getWorldBorder();
        double x = player.getX();
        double z = player.getZ();
        boolean horizontal = !border.isWithinBounds(player.blockPosition())
                || x <= border.getMinX() + WORLD_BOUNDARY_MARGIN
                || x >= border.getMaxX() - WORLD_BOUNDARY_MARGIN
                || z <= border.getMinZ() + WORLD_BOUNDARY_MARGIN
                || z >= border.getMaxZ() - WORLD_BOUNDARY_MARGIN;
        double feet = player.getY();
        double head = feet + player.getBbHeight();
        boolean vertical = feet <= level.getMinBuildHeight() + WORLD_BOUNDARY_MARGIN
                || head >= level.getMaxBuildHeight() - WORLD_BOUNDARY_MARGIN;
        return horizontal || vertical;
    }

    private static int randomBoundaryDepth(double currentDepth) {
        int maxDepth = Math.max(RANDOM_BOUNDARY_MIN_DEPTH, (int) MAX_DEPTH);
        int current = (int) Math.round(currentDepth);
        int selected = RANDOM_BOUNDARY_MIN_DEPTH
                + newRandom().nextInt(maxDepth - RANDOM_BOUNDARY_MIN_DEPTH + 1);
        for (int attempt = 0; attempt < 8 && selected == current; attempt++) {
            selected = RANDOM_BOUNDARY_MIN_DEPTH
                    + newRandom().nextInt(maxDepth - RANDOM_BOUNDARY_MIN_DEPTH + 1);
        }
        if (selected == current) {
            selected = current < maxDepth ? current + 1 : maxDepth - 1;
        }
        return Math.max(RANDOM_BOUNDARY_MIN_DEPTH, Math.min(maxDepth, selected));
    }

    private static double clampToBoundary(double value, double minimum, double maximum) {
        double lower = minimum + WORLD_BOUNDARY_MARGIN + 1.0D;
        double upper = maximum - WORLD_BOUNDARY_MARGIN - 1.0D;
        if (lower > upper) {
            return (minimum + maximum) * 0.5D;
        }
        return Math.max(lower, Math.min(upper, value));
    }

    /**
     * Replaces only the chunks that left the moving three-by-three window and fills the
     * chunks that entered it. The instance origin remains fixed for separation purposes;
     * generated content follows the owner's current chunk instead.
     */
    private static void updateChunkWindow(
            ServerPlayer player,
            ImaginarySpaceData data,
            InstanceState state,
            ChunkPos newCenter
    ) {
        updateChunkWindow(player, data, state, newCenter, false);
    }

    private static void updateChunkWindow(
            ServerPlayer player,
            ImaginarySpaceData data,
            InstanceState state,
            ChunkPos newCenter,
            boolean refillExistingWindow
    ) {
        Set<ChunkPos> oldWindow = new HashSet<>(state.generatedChunks);
        Set<ChunkPos> newWindow = windowAround(newCenter);
        Set<ChunkPos> leaving = new HashSet<>(oldWindow);
        leaving.removeAll(newWindow);
        Set<ChunkPos> entering = new HashSet<>(newWindow);
        entering.removeAll(oldWindow);

        state.windowCenterChunk = newCenter;
        state.generationCenter = player.position();
        if (refillExistingWindow && leaving.isEmpty()) {
            clearGeneratedContentInChunks(player.serverLevel(), state, newWindow);
            state.queue.removeIf(task -> newWindow.contains(task.chunk));
            state.reservedBlockPositions.removeIf(pos -> newWindow.contains(new ChunkPos(pos)));
        }
        if (!leaving.isEmpty()) {
            clearGeneratedContentInChunks(player.serverLevel(), state, leaving);
            for (ChunkPos chunk : leaving) {
                removeInstanceTicket(player.serverLevel(), state, chunk);
            }
        }

        clearGeneratedBlocksOutsideSphere(player.serverLevel(), state);
        state.queue.removeIf(task -> leaving.contains(task.chunk)
                || (task.kind == SpawnKind.BLOCK
                && !withinSphere(state.generationCenter, task.position)));
        state.reservedBlockPositions.removeIf(pos ->
                !withinSphere(state.generationCenter, Vec3.atCenterOf(pos)));
        state.generatedChunks.clear();
        state.generatedChunks.addAll(newWindow);
        for (ChunkPos chunk : entering) {
            addInstanceTicket(player.serverLevel(), state, chunk);
        }

        if ((!entering.isEmpty() || refillExistingWindow) && data.depth() < MAX_DEPTH) {
            state.random = newRandom();
            state.depth = data.depth();
            Set<ChunkPos> refillChunks = refillExistingWindow ? Set.copyOf(state.generatedChunks) : entering;
            queueContentForChunks(state, state.generation, refillChunks,
                    scaledCount(ImaginarySpaceConfig.MAX_BLOCKS.get(), BASE_BLOCK_COUNT, state.depth),
                    scaledCount(ImaginarySpaceConfig.MAX_ITEMS.get(), BASE_ITEM_COUNT, state.depth),
                    scaledMobCount(ImaginarySpaceConfig.MAX_MOBS.get(), state.depth));
            data.markContentsGenerated();
        }
    }

    private static boolean shouldRefreshGenerationCenter(ServerPlayer player, InstanceState state) {
        return player != null
                && state != null
                && isFinite(state.generationCenter)
                && player.position().distanceToSqr(state.generationCenter) >= GENERATION_CENTER_REFRESH_DISTANCE_SQR;
    }

    private static Set<ChunkPos> windowAround(ChunkPos center) {
        Set<ChunkPos> window = new HashSet<>();
        for (int dx = -GENERATION_WINDOW_RADIUS_CHUNKS;
             dx <= GENERATION_WINDOW_RADIUS_CHUNKS; dx++) {
            for (int dz = -GENERATION_WINDOW_RADIUS_CHUNKS;
                 dz <= GENERATION_WINDOW_RADIUS_CHUNKS; dz++) {
                window.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
        return window;
    }

    private static void queueContentForChunks(
            InstanceState state,
            int generation,
            Set<ChunkPos> chunks,
            int blockCount,
            int itemCount,
            int mobCount
    ) {
        if (chunks.isEmpty()) {
            return;
        }
        List<ChunkPos> orderedChunks = new ArrayList<>(state.generatedChunks);
        orderedChunks.sort(Comparator.comparingInt((ChunkPos chunk) -> chunk.x)
                .thenComparingInt(chunk -> chunk.z));
        Map<String, Integer> blockNamespaces = new HashMap<>();
        Map<String, Integer> itemNamespaces = new HashMap<>();
        Map<String, Integer> entityNamespaces = new HashMap<>();
        int namespaceLimit = ImaginarySpaceConfig.MAX_PER_NAMESPACE.get();

        for (int chunkIndex = 0; chunkIndex < orderedChunks.size(); chunkIndex++) {
            ChunkPos chunk = orderedChunks.get(chunkIndex);
            if (!chunks.contains(chunk)) {
                continue;
            }
            for (int i = 0; i < quotaForChunk(blockCount, chunkIndex, orderedChunks.size()); i++) {
                ResourceLocation id = chooseCapped(pools.blocks, state.random,
                        blockNamespaces, namespaceLimit);
                Vec3 position = id == null ? null : sampleSeparatedBlockPosition(state, chunk);
                if (id != null && position != null) {
                    state.queue.addLast(new SpawnTask(SpawnKind.BLOCK, id, position,
                            chunk, generation));
                }
            }
            for (int i = 0; i < quotaForChunk(itemCount, chunkIndex, orderedChunks.size()); i++) {
                ResourceLocation id = chooseCapped(pools.items, state.random,
                        itemNamespaces, namespaceLimit);
                if (id != null) {
                    state.queue.addLast(new SpawnTask(SpawnKind.ITEM, id,
                            sampleChunkPosition(state, chunk), chunk, generation));
                }
            }
            for (int i = 0; i < quotaForChunk(mobCount, chunkIndex, orderedChunks.size()); i++) {
                ResourceLocation id = chooseCapped(pools.entities, state.random,
                        entityNamespaces, namespaceLimit);
                if (id != null) {
                    state.queue.addLast(new SpawnTask(SpawnKind.MOB, id,
                            sampleChunkPosition(state, chunk), chunk, generation));
                }
            }
        }
    }

    private static int quotaForChunk(int total, int chunkIndex, int activeChunkCount) {
        if (total <= 0 || activeChunkCount <= 0) {
            return 0;
        }
        int base = total / activeChunkCount;
        int remainder = total % activeChunkCount;
        return base + (chunkIndex < remainder ? 1 : 0);
    }

    private static void queueNamedVisitors(InstanceState state, int generation) {
        if (state.depth >= MAX_DEPTH) {
            return;
        }
        if (state.random.nextDouble() < NAO_MUSHROOM_CHANCE) {
            queueNamedVisitor(state, generation, MOOSHROOM_ID,
                    Component.translatable("entity.typemoonworld.imaginary_space.nao_mushroom"));
        }
        if (state.random.nextDouble() < CAT_ARC_CHANCE) {
            queueNamedVisitor(state, generation, OCELOT_ID,
                    Component.translatable("entity.typemoonworld.imaginary_space.cat_arc"));
        }
    }

    private static void queueNamedVisitor(
            InstanceState state,
            int generation,
            ResourceLocation entityId,
            Component name
    ) {
        for (int attempt = 0; attempt < 96; attempt++) {
            Vec3 position = sampleSphere(state.generationCenter, state.random);
            ChunkPos chunk = new ChunkPos(BlockPos.containing(position));
            if (!state.generatedChunks.contains(chunk)) {
                continue;
            }
            state.queue.addLast(new SpawnTask(SpawnKind.MOB, entityId, position,
                    chunk, generation, name));
            return;
        }
    }

    /** Fills the entry area once with individually randomized, safe block states. */
    private static void fillImmediateRandomSphere(ServerLevel level, InstanceState state) {
        if (level == null || pools.blocks.isEmpty() || !isFinite(state.generationCenter)) {
            return;
        }
        int minX = (int)Math.floor(state.generationCenter.x - TARGET_RADIUS);
        int maxX = (int)Math.ceil(state.generationCenter.x + TARGET_RADIUS);
        int minY = (int)Math.floor(state.generationCenter.y - TARGET_RADIUS);
        int maxY = (int)Math.ceil(state.generationCenter.y + TARGET_RADIUS);
        int minZ = (int)Math.floor(state.generationCenter.z - TARGET_RADIUS);
        int maxZ = (int)Math.ceil(state.generationCenter.z + TARGET_RADIUS);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Vec3 blockCenter = Vec3.atCenterOf(pos);
                    // Entry fill is a lower hemisphere: the player's entry position is
                    // the upper cutting plane, so no blocks are placed above the player.
                    if (!state.generatedChunks.contains(new ChunkPos(pos))
                            || !withinSphere(state.generationCenter, blockCenter)
                            || blockCenter.y > state.generationCenter.y
                            || intersectsPlayer(level, pos)) {
                        continue;
                    }
                    BlockState original = level.getBlockState(pos);
                    if (!original.isAir()) {
                        continue;
                    }
                    ResourceLocation id = pools.blocks.get(state.random.nextInt(pools.blocks.size()));
                    Block block = BuiltInRegistries.BLOCK.get(id);
                    if (block == null || block == Blocks.AIR) {
                        continue;
                    }
                    BlockState placedState = controlledBlockState(level, block,
                            Vec3.atCenterOf(pos), state);
                    if (!isSafeGeneratedBlock(level, pos, placedState)) {
                        continue;
                    }
                    if (level.setBlock(pos, placedState,
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
                        BlockPos storedPos = pos.immutable();
                        state.blocks.put(storedPos, new PlacedBlock(original, placedState));
                        state.reservedBlockPositions.add(storedPos);
                    }
                }
            }
        }
    }

    private static int scaledCount(int configuredMaximum, int baseCount, double depth) {
        if (!Double.isFinite(depth) || depth < 0.0D || depth >= MAX_DEPTH) {
            return 0;
        }
        int maximum = Math.max(0, configuredMaximum);
        int base = Math.min(maximum, Math.max(0, baseCount));
        double progress = Math.max(0.0D, Math.min(1.0D, depth / MAX_DEPTH));
        return Math.min(maximum, base + (int)Math.floor((maximum - base) * progress));
    }

    private static int scaledMobCount(int configuredMaximum, double depth) {
        if (!Double.isFinite(depth) || depth < MOB_GENERATION_MIN_DEPTH || depth >= MAX_DEPTH) {
            return 0;
        }
        int maximum = Math.max(0, configuredMaximum);
        int base = Math.min(maximum, BASE_MOB_COUNT);
        double progress = Math.max(0.0D, Math.min(1.0D,
                (depth - MOB_GENERATION_MIN_DEPTH) / (MAX_DEPTH - MOB_GENERATION_MIN_DEPTH)));
        return Math.min(maximum, base + (int)Math.floor((maximum - base) * progress));
    }

    private static void processSpawnQueue(
            ServerLevel level,
            InstanceState state,
            int budget
    ) {
        int processed = 0;
        while (processed < Math.max(1, budget) && !state.queue.isEmpty()) {
            SpawnTask task = state.queue.removeFirst();
            processed++;
            if (task.generation != state.generation) {
                continue;
            }
            if (!state.generatedChunks.contains(task.chunk)) {
                continue;
            }
            BlockPos chunkProbe = BlockPos.containing(task.position);
            if (!level.hasChunkAt(chunkProbe)) {
                if (++task.attempts < MAX_SPAWN_RETRIES) {
                    state.queue.addLast(task);
                } else {
                    recordFailure(task.id, "unloaded_chunk");
                }
                continue;
            }
            try {
                switch (task.kind) {
                    case BLOCK -> spawnBlock(level, state, task);
                    case ITEM -> spawnItem(level, state, task);
                    case MOB -> spawnMob(level, state, task);
                }
            } catch (Throwable throwable) {
                recordFailure(task.id, throwable.getClass().getSimpleName());
                TypeMoonAddon.LOGGER.warn(
                        "Skipping imaginary-space {} entry {} from namespace {}",
                        task.kind,
                        task.id,
                        task.id == null ? "unknown" : task.id.getNamespace(),
                        throwable);
            }
        }
    }

    private static void spawnBlock(ServerLevel level, InstanceState state, SpawnTask task) {
        Block block = BuiltInRegistries.BLOCK.get(task.id);
        if (block == null || block == Blocks.AIR) {
            recordFailure(task.id, "missing_block");
            return;
        }
        BlockState placedState = controlledBlockState(level, block, task.position, state);
        BlockPos pos = BlockPos.containing(task.position);
        if (!state.generatedChunks.contains(new ChunkPos(pos))
                || !withinSphere(state.generationCenter, Vec3.atCenterOf(pos))
                || !isSafeGeneratedBlock(level, pos, placedState)
                || intersectsPlayer(level, pos)) {
            recordFailure(task.id, "unsafe_block_position");
            return;
        }
        BlockState original = level.getBlockState(pos);
        if (!original.isAir()) {
            recordFailure(task.id, "occupied_block_position");
            return;
        }
        if (level.setBlock(pos, placedState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
            state.blocks.put(pos.immutable(), new PlacedBlock(original, placedState));
        }
    }

    private static void spawnItem(ServerLevel level, InstanceState state, SpawnTask task) {
        Item item = BuiltInRegistries.ITEM.get(task.id);
        if (item == null || item == Items.AIR || !isFinite(task.position)
                || !state.generatedChunks.contains(task.chunk)) {
            recordFailure(task.id, "missing_item");
            return;
        }
        ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) {
            recordFailure(task.id, "empty_item_stack");
            return;
        }
        if (state.depth >= 20.0D && state.random.nextDouble() < 0.35D) {
            stack.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("item.typemoonworld.imaginary_space.anomaly"));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        ItemEntity entity = new ItemEntity(
                level,
                task.position.x,
                task.position.y,
                task.position.z,
                stack,
                state.random.nextDouble() * 0.08D - 0.04D,
                state.random.nextDouble() * 0.06D - 0.03D,
                state.random.nextDouble() * 0.08D - 0.04D);
        entity.setNoGravity(true);
        markInstanceEntity(entity, state.instanceId);
        if (level.addFreshEntity(entity)) {
            state.entities.add(entity.getUUID());
        } else {
            recordFailure(task.id, "item_add_rejected");
        }
    }

    private static void spawnMob(ServerLevel level, InstanceState state, SpawnTask task) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(task.id);
        if (type == null || type == EntityType.PLAYER || !type.canSerialize() || !type.canSummon()) {
            recordFailure(task.id, "unsummonable_entity_type");
            return;
        }
        Entity created = type.create(level);
        if (!(created instanceof Mob mob) || created instanceof ArmorStand) {
            if (created != null) {
                created.discard();
            }
            recordFailure(task.id, "not_safe_living_mob");
            return;
        }
        mob.moveTo(task.position.x, task.position.y, task.position.z,
                state.random.nextFloat() * 360.0F, 0.0F);
        if (!state.generatedChunks.contains(task.chunk)
                || !level.noCollision(mob, mob.getBoundingBox())) {
            mob.discard();
            recordFailure(task.id, "entity_collision");
            return;
        }
        mob.setNoGravity(true);
        mob.setDeltaMovement(Vec3.ZERO);
        if (state.depth >= 20.0D && state.random.nextDouble() < 0.35D) {
            mob.setCustomName(Component.translatable(
                    "entity.typemoonworld.imaginary_space.anomaly", mob.getDisplayName()));
            mob.setPersistenceRequired();
        }
        if (task.customName != null) {
            mob.setCustomName(task.customName);
            mob.setPersistenceRequired();
        }
        markInstanceEntity(mob, state.instanceId);
        if (level.addFreshEntity(mob)) {
            state.entities.add(mob.getUUID());
        } else {
            mob.discard();
            recordFailure(task.id, "entity_add_rejected");
        }
    }

    private static void clearGeneratedContent(ServerLevel level, InstanceState state) {
        state.queue.clear();
        for (UUID entityId : List.copyOf(state.entities)) {
            Entity entity = level.getEntity(entityId);
            if (entity != null && !(entity instanceof Player)
                    && state.instanceId.equals(readInstanceId(entity))) {
                entity.discard();
            }
        }
        state.entities.clear();
        for (Map.Entry<BlockPos, PlacedBlock> entry : List.copyOf(state.blocks.entrySet())) {
            BlockPos pos = entry.getKey();
            PlacedBlock placed = entry.getValue();
            if (level.hasChunkAt(pos) && level.getBlockState(pos).equals(placed.placedState)) {
                level.setBlock(pos, placed.originalState, Block.UPDATE_ALL);
            }
        }
        state.blocks.clear();
        state.reservedBlockPositions.clear();
        state.generatedChunks.clear();
        releaseInstanceTicket(level, state);
    }

    private static void clearGeneratedContentInChunks(
            ServerLevel level,
            InstanceState state,
            Set<ChunkPos> chunks
    ) {
        for (UUID entityId : List.copyOf(state.entities)) {
            Entity entity = level.getEntity(entityId);
            if (entity != null
                    && !(entity instanceof Player)
                    && state.instanceId.equals(readInstanceId(entity))
                    && chunks.contains(new ChunkPos(entity.blockPosition()))) {
                entity.discard();
                state.entities.remove(entityId);
            }
        }
        for (Map.Entry<BlockPos, PlacedBlock> entry : List.copyOf(state.blocks.entrySet())) {
            BlockPos pos = entry.getKey();
            if (!chunks.contains(new ChunkPos(pos))) {
                continue;
            }
            PlacedBlock placed = entry.getValue();
            if (level.hasChunkAt(pos) && level.getBlockState(pos).equals(placed.placedState)) {
                level.setBlock(pos, placed.originalState, Block.UPDATE_ALL);
            }
            state.blocks.remove(pos);
        }
    }

    private static void clearGeneratedBlocksOutsideSphere(ServerLevel level, InstanceState state) {
        for (Map.Entry<BlockPos, PlacedBlock> entry : List.copyOf(state.blocks.entrySet())) {
            BlockPos pos = entry.getKey();
            if (withinSphere(state.generationCenter, Vec3.atCenterOf(pos))) {
                continue;
            }
            PlacedBlock placed = entry.getValue();
            if (level.hasChunkAt(pos) && level.getBlockState(pos).equals(placed.placedState)) {
                level.setBlock(pos, placed.originalState, Block.UPDATE_ALL);
            }
            state.blocks.remove(pos);
        }
    }

    private static void keepInstanceLoaded(ServerLevel level, InstanceState state) {
        if (level == null || state == null) {
            return;
        }
        for (ChunkPos chunk : state.generatedChunks) {
            addInstanceTicket(level, state, chunk);
        }
    }

    private static void releaseInstanceTicket(ServerLevel level, InstanceState state) {
        if (level == null || state == null) {
            return;
        }
        for (ChunkPos chunk : Set.copyOf(state.ticketChunks)) {
            removeInstanceTicket(level, state, chunk);
        }
        state.ticketChunks.clear();
        state.ticketActive = false;
    }

    private static void addInstanceTicket(ServerLevel level, InstanceState state, ChunkPos chunk) {
        if (level == null || state == null || chunk == null || state.ticketChunks.contains(chunk)) {
            return;
        }
        level.getChunkSource().addRegionTicket(
                TicketType.PLAYER, chunk, INSTANCE_TICKET_LEVEL, chunk);
        state.ticketChunks.add(chunk);
        state.ticketActive = true;
    }

    private static void removeInstanceTicket(ServerLevel level, InstanceState state, ChunkPos chunk) {
        if (level == null || state == null || chunk == null || !state.ticketChunks.remove(chunk)) {
            return;
        }
        level.getChunkSource().removeRegionTicket(
                TicketType.PLAYER, chunk, INSTANCE_TICKET_LEVEL, chunk);
        state.ticketActive = !state.ticketChunks.isEmpty();
    }

    private static void settleExistence(ServerPlayer player, ImaginarySpaceData data) {
        if (hasImaginaryDiveImmunity(player)) {
            syncData(player, data);
            return;
        }
        data.setExistence(data.existence() - data.depth() / 10.0D);
        double probability = Math.max(0.0D, Math.min(1.0D,
                (100.0D - data.existence()) / 100.0D));
        syncData(player, data);
        if (newRandom().nextDouble() < probability) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.imaginary_space.dissolved"), false);
            killForDissolution(player, data);
        }
    }

    private static void settleTimeOffset(ServerPlayer player, ImaginarySpaceData data) {
        int multiplier = newRandom().nextInt(21) - 10;
        data.addTimeOffset(data.depth() * multiplier);
        syncData(player, data);
    }

    private static void maybeTriggerLayerEvent(ServerPlayer player, ImaginarySpaceData data) {
        double depth = data.depth();
        if (depth < 30.0D) {
            return;
        }
        double probability = Math.min(0.85D, Math.max(0.0D, (depth - 29.0D) / 140.0D));
        if (newRandom().nextDouble() >= probability) {
            return;
        }
        VFXServerEffects.spawn(player.serverLevel(), ANOMALY_EFFECT,
                player.getBoundingBox().getCenter(), VFX_OBSERVER_RADIUS);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                SoundSource.AMBIENT, 0.55F, 0.45F + newRandom().nextFloat() * 0.35F);
        if (depth >= 50.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, true));
        }
    }

    private static void startOuterGodEvent(ServerPlayer player, ImaginarySpaceData data) {
        if (EntityUtils.isImmunePlayerTarget(player)) {
            return;
        }
        data.startOuterGodCountdown(OUTER_GOD_DELAY_TICKS);
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.imaginary_space.outer_god")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.OBFUSCATED), false);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, OUTER_GOD_DELAY_TICKS + 20, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, OUTER_GOD_DELAY_TICKS + 20, 9, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, OUTER_GOD_DELAY_TICKS + 20, 9, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, OUTER_GOD_DELAY_TICKS + 20, 4, false, false, true));
        VFXServerEffects.spawn(player.serverLevel(), ANOMALY_EFFECT,
                player.getBoundingBox().getCenter(), VFX_OBSERVER_RADIUS);
    }

    private static void killForOuterGod(ServerPlayer player, ImaginarySpaceData data) {
        killWithGenericSource(player, data);
    }

    private static void killForDissolution(ServerPlayer player, ImaginarySpaceData data) {
        if (hasImaginaryDiveImmunity(player)) {
            return;
        }
        killWithGenericSource(player, data);
    }

    private static void killWithGenericSource(ServerPlayer player, ImaginarySpaceData data) {
        if (EntityUtils.isImmunePlayerTarget(player)) {
            data.clearOuterGodCountdown();
            return;
        }
        cleanupDetachedSession(player, data, false);
        player.setInvulnerable(false);
        player.invulnerableTime = 0;
        DamageSource source = player.damageSources().source(DamageTypes.GENERIC_KILL);
        player.hurt(source, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.setHealth(0.0F);
            player.die(source);
        }
    }

    private static void applyWorldCorrection(ServerPlayer player, double timeOffset) {
        if (hasImaginaryDiveImmunity(player)) {
            return;
        }
        double absolute = Math.abs(Double.isFinite(timeOffset) ? timeOffset : 0.0D);
        if (absolute <= 0.0D || player == null || !player.isAlive()) {
            return;
        }
        double damage = Math.min(
                WORLD_CORRECTION_MAX_DAMAGE,
                WORLD_CORRECTION_BASE_DAMAGE + Math.sqrt(absolute) * WORLD_CORRECTION_SCALE);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().source(WORLD_CORRECTION_DAMAGE), (float)damage);
    }

    private static void acquireHighDepthOverride(
            ServerPlayer player,
            ImaginarySpaceData data,
            UUID instanceId
    ) {
        MinecraftServer server = player.getServer();
        if (!daylightSnapshotPresent) {
            daylightOriginal = server.overworld().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
            daylightSnapshotPresent = true;
        }
        DAYLIGHT_OVERRIDES.add(instanceId);
        server.overworld().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        int selected = player.getInventory().selected;
        data.markHighDepthOverride(selected);
        player.getInventory().selected = (selected + 4) % 9;
    }

    private static void releaseHighDepthOverride(ServerPlayer player, ImaginarySpaceData data) {
        if (!data.highDepthOverride()) {
            return;
        }
        if (data.previousSelectedSlot() >= 0) {
            player.getInventory().selected = data.previousSelectedSlot();
        }
        UUID instanceId = data.instanceId();
        if (instanceId != null) {
            DAYLIGHT_OVERRIDES.remove(instanceId);
        }
        data.clearHighDepthOverride();
        if (DAYLIGHT_OVERRIDES.isEmpty()) {
            restoreDaylightRule(player.getServer());
        }
    }

    private static void restoreDaylightRule(MinecraftServer server) {
        if (server != null && daylightSnapshotPresent) {
            server.overworld().getGameRules().getRule(GameRules.RULE_DAYLIGHT)
                    .set(daylightOriginal, server);
        }
        daylightSnapshotPresent = false;
        DAYLIGHT_OVERRIDES.clear();
    }

    private static boolean isTeleportCandidate(
            ServerPlayer caster,
            LivingEntity target,
            Vec3 center,
            ServerLevel destination
    ) {
        return target != caster
                && target.isAlive()
                && !target.isRemoved()
                && !(target instanceof ArmorStand)
                && (!(target instanceof ServerPlayer player) || !player.isSpectator())
                && target.level() == caster.level()
                && target.getBoundingBox().getCenter().distanceToSqr(center) <= TARGET_RADIUS_SQUARED
                && !target.isPassenger()
                && !target.isVehicle()
                && target.canChangeDimensions(target.level(), destination)
                && (!(target instanceof ServerPlayer player)
                        || !player.getData(ImaginarySpaceAttachments.PLAYER_STATE).active());
    }

    private static boolean sameInstance(ImaginarySpaceData casterData, ServerPlayer target) {
        ImaginarySpaceData targetData = target.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        return targetData.active()
                && casterData.instanceId() != null
                && casterData.instanceId().equals(targetData.instanceId());
    }

    private static void ensurePools(MinecraftServer server) {
        int signature = ImaginarySpaceConfig.signature();
        if (!pools.items.isEmpty() && signature == poolConfigSignature) {
            return;
        }
        poolConfigSignature = signature;
        List<ResourceLocation> items = new ArrayList<>();
        List<ResourceLocation> blocks = new ArrayList<>();
        List<ResourceLocation> entities = new ArrayList<>();
        Map<String, Integer> itemNamespaces = new HashMap<>();
        Map<String, Integer> blockNamespaces = new HashMap<>();
        Map<String, Integer> entityNamespaces = new HashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (item == Items.AIR || !ImaginarySpaceConfig.allows(id)) {
                continue;
            }
            try {
                ItemStack stack = new ItemStack(item);
                if (stack.isEmpty()) {
                    recordFailure(id, "empty_default_stack");
                    continue;
                }
                items.add(id);
                itemNamespaces.merge(id.getNamespace(), 1, Integer::sum);
            } catch (Throwable throwable) {
                recordFailure(id, "item_pool_" + throwable.getClass().getSimpleName());
                TypeMoonAddon.LOGGER.warn(
                        "Skipping imaginary-space item pool entry {} from namespace {}",
                        id, id == null ? "unknown" : id.getNamespace(), throwable);
            }
        }
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            try {
                if (isSafePoolBlock(block) && ImaginarySpaceConfig.allows(id)) {
                    blocks.add(id);
                    blockNamespaces.merge(id.getNamespace(), 1, Integer::sum);
                }
            } catch (Throwable throwable) {
                recordFailure(id, "block_pool_" + throwable.getClass().getSimpleName());
                TypeMoonAddon.LOGGER.warn(
                        "Skipping imaginary-space block pool entry {} from namespace {}",
                        id, id == null ? "unknown" : id.getNamespace(), throwable);
            }
        }
        ServerLevel probeLevel = server.overworld();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (type == EntityType.PLAYER || !type.canSerialize() || !type.canSummon()
                    || !ImaginarySpaceConfig.allows(id)) {
                continue;
            }
            Entity probe = null;
            try {
                probe = type.create(probeLevel);
                if (!(probe instanceof Mob) || probe instanceof ArmorStand) {
                    recordFailure(id, "not_safe_living_mob");
                    continue;
                }
                entities.add(id);
                entityNamespaces.merge(id.getNamespace(), 1, Integer::sum);
            } catch (Throwable throwable) {
                recordFailure(id, "entity_pool_" + throwable.getClass().getSimpleName());
                TypeMoonAddon.LOGGER.warn(
                        "Skipping imaginary-space entity pool entry {} from namespace {}",
                        id, id == null ? "unknown" : id.getNamespace(), throwable);
            } finally {
                if (probe != null) {
                    probe.discard();
                }
            }
        }
        pools = new PoolSnapshot(List.copyOf(items), List.copyOf(blocks), List.copyOf(entities));
        TypeMoonAddon.LOGGER.info(
                "Imaginary-space runtime pools: items={}, blocks={}, entity candidates={}",
                items.size(), blocks.size(), entities.size());
        TypeMoonAddon.LOGGER.info("Imaginary-space item candidates by namespace: {}", itemNamespaces);
        TypeMoonAddon.LOGGER.info("Imaginary-space block candidates by namespace: {}", blockNamespaces);
        TypeMoonAddon.LOGGER.info("Imaginary-space entity candidates by namespace: {}", entityNamespaces);
        TypeMoonAddon.LOGGER.info("Imaginary-space skipped entries by reason: {}",
                skippedEntryReasonCounts());
    }

    private static boolean isSafePoolBlock(Block block) {
        if (block == null || block == Blocks.AIR || block instanceof FallingBlock) {
            return false;
        }
        BlockState state = block.defaultBlockState();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id != null
                && ImaginarySpaceConfig.allows(id)
                && !state.hasBlockEntity()
                && state.getFluidState().isEmpty();
    }

    private static boolean isSafeGeneratedBlock(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {
        return state != null
                && !state.isAir()
                && !state.hasBlockEntity()
                && state.getFluidState().isEmpty()
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static BlockState controlledBlockState(
            ServerLevel level,
            Block block,
            Vec3 sampledPosition,
            InstanceState state
    ) {
        BlockPos pos = BlockPos.containing(sampledPosition);
        BlockState fallback = block.defaultBlockState();
        if (state.depth < 20.0D || state.random.nextDouble() >= 0.35D) {
            return fallback;
        }
        List<BlockState> candidates = block.getStateDefinition().getPossibleStates().stream()
                .filter(candidate -> isSafeGeneratedBlock(level, pos, candidate))
                .toList();
        return candidates.isEmpty()
                ? fallback
                : candidates.get(state.random.nextInt(candidates.size()));
    }

    private static boolean intersectsPlayer(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos);
        return !level.getEntitiesOfClass(Player.class, box.inflate(0.05D)).isEmpty();
    }

    private static ResourceLocation chooseCapped(
            List<ResourceLocation> pool,
            RandomSource random,
            Map<String, Integer> namespaceCounts,
            int namespaceLimit
    ) {
        if (pool.isEmpty()) {
            return null;
        }
        for (int attempt = 0; attempt < 32; attempt++) {
            ResourceLocation id = pool.get(random.nextInt(pool.size()));
            int count = namespaceCounts.getOrDefault(id.getNamespace(), 0);
            if (count < namespaceLimit) {
                namespaceCounts.put(id.getNamespace(), count + 1);
                return id;
            }
        }
        return null;
    }

    private static Vec3 allocateInstanceCenter() {
        RandomSource random = newRandom();
        for (int attempt = 0; attempt < 128; attempt++) {
            int cellX = random.nextInt(INSTANCE_CELL_LIMIT * 2 + 1) - INSTANCE_CELL_LIMIT;
            int cellZ = random.nextInt(INSTANCE_CELL_LIMIT * 2 + 1) - INSTANCE_CELL_LIMIT;
            Vec3 candidate = new Vec3(
                    cellX * (double)INSTANCE_CELL_SIZE + 0.5D,
                    INSTANCE_Y,
                    cellZ * (double)INSTANCE_CELL_SIZE + 0.5D);
            boolean collision = INSTANCES.values().stream().anyMatch(state ->
                    horizontalDistanceSquared(state.center, candidate)
                            < INSTANCE_MIN_SEPARATION * INSTANCE_MIN_SEPARATION);
            if (!collision) {
                return candidate;
            }
        }
        long fallback = Math.abs(SECURE_RANDOM.nextLong() % 10_000L);
        return new Vec3(fallback * INSTANCE_CELL_SIZE + 0.5D, INSTANCE_Y,
                -fallback * INSTANCE_CELL_SIZE + 0.5D);
    }

    private static Vec3 sampleSphere(Vec3 center, RandomSource random) {
        double radius = TARGET_RADIUS * Math.cbrt(random.nextDouble());
        double yDirection = random.nextDouble() * 2.0D - 1.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - yDirection * yDirection));
        double angle = random.nextDouble() * Math.PI * 2.0D;
        return center.add(
                radius * horizontal * Math.cos(angle),
                radius * yDirection,
                radius * horizontal * Math.sin(angle));
    }

    private static Vec3 sampleChunkPosition(InstanceState state, ChunkPos chunk) {
        double x = chunk.x * 16.0D + 0.5D + state.random.nextDouble() * 15.0D;
        double z = chunk.z * 16.0D + 0.5D + state.random.nextDouble() * 15.0D;
        double yOffset = state.random.nextDouble() * TARGET_RADIUS * 2.0D - TARGET_RADIUS;
        double y = Math.max(8.0D, Math.min(375.0D,
                state.generationCenter.y + yOffset));
        return new Vec3(x, y, z);
    }

    private static Vec3 sampleSeparatedBlockPosition(InstanceState state, ChunkPos chunk) {
        int attempts = 96;
        for (int attempt = 0; attempt < attempts; attempt++) {
            Vec3 candidate = sampleSphere(state.generationCenter, state.random);
            BlockPos pos = BlockPos.containing(candidate);
            if (!chunk.equals(new ChunkPos(pos))) {
                continue;
            }
            if (hasNearbyReservedBlock(state, pos)) {
                continue;
            }
            state.reservedBlockPositions.add(pos.immutable());
            return Vec3.atCenterOf(pos);
        }
        return null;
    }

    private static boolean hasNearbyReservedBlock(InstanceState state, BlockPos candidate) {
        int radius = (int)Math.ceil(MIN_RANDOM_BLOCK_SEPARATION);
        double maximum = MIN_RANDOM_BLOCK_SEPARATION * MIN_RANDOM_BLOCK_SEPARATION;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > maximum) {
                        continue;
                    }
                    if (state.reservedBlockPositions.contains(
                            candidate.offset(dx, dy, dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Vec3 findSafeReturn(
            ServerLevel level,
            ServerPlayer player,
            Vec3 requested,
            Vec3 fallback
    ) {
        if (isSafeReturnPosition(level, requested)) {
            return requested;
        }
        if (isSafeReturnPosition(level, fallback)) {
            TypeMoonAddon.LOGGER.warn("Using recorded safe return for {} instead of blocked position {}",
                    player.getGameProfile().getName(), requested);
            return fallback;
        }
        BlockPos origin = BlockPos.containing(requested);
        ChunkPos originalChunk = new ChunkPos(origin);
        for (int radius = 0; radius <= MAX_SAFE_RETURN_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    if (!new ChunkPos(x >> 4, z >> 4).equals(originalChunk)) {
                        continue;
                    }
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    Vec3 candidate = new Vec3(x + 0.5D, y, z + 0.5D);
                    if (isSafeReturnPosition(level, candidate)) {
                        TypeMoonAddon.LOGGER.warn(
                                "Adjusted imaginary-space return for {} from {} to {}",
                                player.getGameProfile().getName(), requested, candidate);
                        return candidate;
                    }
                }
            }
        }
        Vec3 spawn = Vec3.atBottomCenterOf(level.getSharedSpawnPos()).add(0.0D, 1.0D, 0.0D);
        TypeMoonAddon.LOGGER.warn("Falling back to world spawn for imaginary-space return of {}",
                player.getGameProfile().getName());
        return spawn;
    }

    private static boolean isSafeReturnPosition(ServerLevel level, Vec3 position) {
        if (level == null || !isFinite(position)) {
            return false;
        }
        BlockPos feet = BlockPos.containing(position);
        if (!level.hasChunkAt(feet)) {
            level.getChunk(feet);
        }
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(feet.above());
        BlockState floorState = level.getBlockState(feet.below());
        return feetState.getCollisionShape(level, feet).isEmpty()
                && headState.getCollisionShape(level, feet.above()).isEmpty()
                && feetState.getFluidState().isEmpty()
                && headState.getFluidState().isEmpty()
                && floorState.isFaceSturdy(level, feet.below(), Direction.UP);
    }

    private static ServerLevel resolveReturnLevel(MinecraftServer server, String dimensionId) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        if (id != null) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (level != null && !DIMENSION.equals(level.dimension())) {
                return level;
            }
        }
        return server.overworld();
    }

    private static void markInstanceEntity(Entity entity, UUID instanceId) {
        entity.getPersistentData().putUUID(INSTANCE_TAG, instanceId);
    }

    private static UUID readInstanceId(Entity entity) {
        return entity.getPersistentData().hasUUID(INSTANCE_TAG)
                ? entity.getPersistentData().getUUID(INSTANCE_TAG)
                : null;
    }

    private static void recordFailure(ResourceLocation id, String reason) {
        String key = (id == null ? "unknown" : id.toString()) + "|" + reason;
        SPAWN_FAILURES.merge(key, 1, Integer::sum);
    }

    private static Map<String, Integer> skippedEntryReasonCounts() {
        Map<String, Integer> counts = new java.util.TreeMap<>();
        for (Map.Entry<String, Integer> entry : SPAWN_FAILURES.entrySet()) {
            String key = entry.getKey();
            int separator = key.lastIndexOf('|');
            String reason = separator >= 0 && separator + 1 < key.length()
                    ? key.substring(separator + 1)
                    : "unknown";
            counts.merge(reason, entry.getValue(), Integer::sum);
        }
        return counts;
    }

    private static void syncData(ServerPlayer player, ImaginarySpaceData data) {
        AddonNetwork.sendToPlayer(player, new ImaginarySpaceStatePayload(data));
    }

    private static RandomSource newRandom() {
        return RandomSource.create(SECURE_RANDOM.nextLong());
    }

    private static boolean isValidCaster(ServerPlayer player) {
        return player != null
                && player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator()
                && !player.hasDisconnected();
    }

    private static boolean isImaginarySpace(ServerLevel level) {
        return level != null && DIMENSION.equals(level.dimension());
    }

    private static boolean withinSphere(Vec3 center, Vec3 position) {
        return isFinite(center)
                && isFinite(position)
                && center.distanceToSqr(position) <= TARGET_RADIUS_SQUARED;
    }

    private static boolean isFinite(Vec3 vector) {
        return vector != null
                && Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
    }

    private static void updateSwimmingState(Player player, boolean swimming) {
        if (player == null) {
            return;
        }
        player.setSwimming(swimming);
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }
        if (swimming) {
            if (!movementSpeed.hasModifier(SWIM_SPEED_MODIFIER_ID)) {
                movementSpeed.addTransientModifier(new AttributeModifier(
                        SWIM_SPEED_MODIFIER_ID,
                        SWIM_SPEED_MULTIPLIER,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        } else {
            movementSpeed.removeModifier(SWIM_SPEED_MODIFIER_ID);
        }
    }

    private static void clearSwimmingState(Player player) {
        if (player == null) {
            return;
        }
        player.setSwimming(false);
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(SWIM_SPEED_MODIFIER_ID);
        }
    }

    private static double horizontalDistanceSquared(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return dx * dx + dz * dz;
    }

    public enum CastStatus {
        SUCCESS,
        NO_TARGET,
        DIMENSION_UNAVAILABLE,
        REJECTED
    }

    public record CastOutcome(CastStatus status, int affectedTargets) {
        private static CastOutcome success(int affectedTargets) {
            return new CastOutcome(CastStatus.SUCCESS, Math.max(0, affectedTargets));
        }

        private static CastOutcome noTarget() {
            return new CastOutcome(CastStatus.NO_TARGET, 0);
        }

        private static CastOutcome dimensionUnavailable() {
            return new CastOutcome(CastStatus.DIMENSION_UNAVAILABLE, 0);
        }

        private static CastOutcome rejected() {
            return new CastOutcome(CastStatus.REJECTED, 0);
        }
    }

    public enum DepthChangeResult {
        CHANGED,
        UNCHANGED,
        INVALID,
        NOT_IN_SPACE,
        REJECTED
    }

    private record VerticalMovementInput(int direction, long lastUpdateTick) {
    }

    private record PendingCreatureTransfer(
            UUID casterId,
            UUID targetId,
            ResourceKey<Level> sourceDimension,
            Vec3 anchor,
            long completeAt,
            long collapseAt,
            UUID visualId
    ) {
        private PendingCreatureTransfer startCollapse(long transferAt) {
            return new PendingCreatureTransfer(
                    casterId, targetId, sourceDimension, anchor, completeAt, transferAt, visualId);
        }
    }

    private enum SpawnKind {
        BLOCK,
        ITEM,
        MOB
    }

    private static final class SpawnTask {
        private final SpawnKind kind;
        private final ResourceLocation id;
        private final Vec3 position;
        private final ChunkPos chunk;
        private final int generation;
        private final Component customName;
        private int attempts;

        private SpawnTask(
                SpawnKind kind,
                ResourceLocation id,
                Vec3 position,
                ChunkPos chunk,
                int generation
        ) {
            this(kind, id, position, chunk, generation, null);
        }

        private SpawnTask(
                SpawnKind kind,
                ResourceLocation id,
                Vec3 position,
                ChunkPos chunk,
                int generation,
                Component customName
        ) {
            this.kind = kind;
            this.id = id;
            this.position = position;
            this.chunk = chunk;
            this.generation = generation;
            this.customName = customName;
        }
    }

    private static final class InstanceState {
        private final UUID instanceId;
        private final UUID ownerId;
        private final Vec3 center;
        private final ChunkPos entryChunk;
        private ChunkPos windowCenterChunk;
        private Vec3 generationCenter;
        private final Set<ChunkPos> generatedChunks = new HashSet<>();
        private final Set<ChunkPos> ticketChunks = new HashSet<>();
        private final ArrayDeque<SpawnTask> queue = new ArrayDeque<>();
        private final Set<UUID> entities = new HashSet<>();
        private final Map<BlockPos, PlacedBlock> blocks = new LinkedHashMap<>();
        private final Set<BlockPos> reservedBlockPositions = new HashSet<>();
        private int generation;
        private int refreshTicks;
        private double depth;
        private RandomSource random = newRandom();
        private boolean ticketActive;

        private InstanceState(UUID instanceId, UUID ownerId, Vec3 center, ChunkPos entryChunk) {
            this.instanceId = instanceId;
            this.ownerId = ownerId;
            this.center = center;
            this.entryChunk = entryChunk;
            this.windowCenterChunk = entryChunk;
            this.generationCenter = center;
        }

        private boolean advanceRefreshTicker(int interval) {
            refreshTicks++;
            if (refreshTicks < Math.max(1, interval)) {
                return false;
            }
            refreshTicks = 0;
            return true;
        }
    }

    private record PlacedBlock(BlockState originalState, BlockState placedState) {
    }

    private record PoolSnapshot(
            List<ResourceLocation> items,
            List<ResourceLocation> blocks,
            List<ResourceLocation> entities
    ) {
        private static PoolSnapshot empty() {
            return new PoolSnapshot(List.of(), List.of(), List.of());
        }
    }
}
