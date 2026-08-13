package com.example.typemoonaddon.detection;

import com.example.typemoonaddon.kimaris.KimarisService;
import com.example.typemoonaddon.magic.DetectionMagic;
import com.example.typemoonaddon.network.AddonNetwork;
import com.example.typemoonaddon.network.DetectionEyeStatePayload;
import com.example.typemoonaddon.network.DetectionTargetSyncPayload;
import com.example.typemoonaddon.network.DetectionTargetSyncPayload.TargetMarker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

/** Server-authoritative toggle, spherical scan, and private target authorization. */
public final class DetectionService {
    public static final int DETECTION_RADIUS_CHUNKS = 6;
    public static final double DETECTION_RADIUS = DETECTION_RADIUS_CHUNKS * 16.0D;
    public static final double DETECTION_RADIUS_SQR = DETECTION_RADIUS * DETECTION_RADIUS;
    public static final int SCAN_INTERVAL_TICKS = 10;
    public static final int MAX_TARGETS = DetectionTargetSyncPayload.MAX_TARGETS;

    private static final int EYE_SYNC_INTERVAL_TICKS = 20;
    private static final int EYE_STATE_TTL_TICKS = 50;
    private static final double EYE_OBSERVER_RADIUS = 128.0D;

    private DetectionService() {
    }

    public static boolean toggle(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isRemoved() || player.isSpectator()) {
            return false;
        }
        DetectionData data = player.getData(DetectionAttachments.PLAYER_STATE);
        if (data.isActive()) {
            deactivate(player, data, true);
            return true;
        }
        if (!isDetectionSelected(player)) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        data.activate(level.dimension(), now);
        sendTargetSync(player, true, Map.of());
        scanAndSync(player, data, now);
        broadcastEyeState(player, true);
        data.scheduleNextEyeSync(now, EYE_SYNC_INTERVAL_TICKS);
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.detection.enabled"), true);
        return true;
    }

    public static void tick(ServerPlayer player) {
        DetectionData data = player.getData(DetectionAttachments.PLAYER_STATE);
        if (!data.isActive()) {
            return;
        }
        if (!isValidActiveCaster(player)
                || data.dimension() == null
                || !data.dimension().equals(player.serverLevel().dimension())) {
            deactivate(player, data, false);
            return;
        }

        long now = player.serverLevel().getGameTime();
        if (now >= data.nextScanTick()) {
            scanAndSync(player, data, now);
        }
        if (now >= data.nextEyeSyncTick()) {
            broadcastEyeState(player, true);
            data.scheduleNextEyeSync(now, EYE_SYNC_INTERVAL_TICKS);
        }
    }

    public static void stop(ServerPlayer player, boolean notify) {
        if (player == null) {
            return;
        }
        DetectionData data = player.getData(DetectionAttachments.PLAYER_STATE);
        if (data.isActive()) {
            deactivate(player, data, notify);
        } else {
            data.deactivate();
            sendTargetSync(player, false, Map.of());
        }
    }

    public static void resetOnLogin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.getData(DetectionAttachments.PLAYER_STATE).deactivate();
        sendTargetSync(player, false, Map.of());
        sendEyeStateTo(player, player, false);
    }

    public static void syncEyeTo(ServerPlayer observer, ServerPlayer caster) {
        if (observer == null || caster == null || observer.serverLevel() != caster.serverLevel()) {
            return;
        }
        DetectionData data = caster.getData(DetectionAttachments.PLAYER_STATE);
        if (data.isActive()) {
            sendEyeStateTo(observer, caster, true);
        }
    }

    public static void clearAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DetectionData data = player.getData(DetectionAttachments.PLAYER_STATE);
            if (data.isActive()) {
                data.deactivate();
                sendTargetSync(player, false, Map.of());
            }
        }
    }

    public static boolean castNpcDetection(LivingEntity caster, LivingEntity currentTarget, double proficiency) {
        if (caster == null
                || !caster.isAlive()
                || caster.isRemoved()
                || !(caster.level() instanceof ServerLevel level)
                || KimarisService.isFrozen(caster)
                || EntityUtils.isPetrified(caster)) {
            return false;
        }

        broadcastEyeState(caster, true);
        LivingEntity detected = findNpcDetectionTarget(caster, currentTarget, proficiency);
        if (detected != null && caster instanceof Mob mob) {
            mob.setTarget(detected);
            mob.getLookControl().setLookAt(detected, 45.0F, 45.0F);
        }
        return true;
    }

    private static LivingEntity findNpcDetectionTarget(
            LivingEntity caster,
            LivingEntity currentTarget,
            double proficiency
    ) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return null;
        }
        Vec3 center = caster.getBoundingBox().getCenter();
        double radius = Mth.clamp(24.0D + Mth.clamp(proficiency, 0.0D, 100.0D) * 0.72D, 24.0D, DETECTION_RADIUS);
        double radiusSqr = radius * radius;
        if (currentTarget != null
                && isNpcDetectable(caster, currentTarget, center, radiusSqr)
                && EntityUtils.isValidCombatTarget(caster, currentTarget)) {
            return currentTarget;
        }
        AABB query = new AABB(center, center).inflate(radius);
        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        query,
                        target -> isNpcDetectable(caster, target, center, radiusSqr)
                                && EntityUtils.isValidCombatTarget(caster, target))
                .stream()
                .min(Comparator.comparingDouble(target ->
                        target.getBoundingBox().getCenter().distanceToSqr(center)))
                .orElse(null);
    }

    private static boolean isNpcDetectable(
            LivingEntity caster,
            LivingEntity target,
            Vec3 center,
            double radiusSqr
    ) {
        return target != caster
                && target != null
                && target.isAlive()
                && !target.isRemoved()
                && !(target instanceof ArmorStand)
                && target.level() == caster.level()
                && target.getBoundingBox().getCenter().distanceToSqr(center) <= radiusSqr;
    }

    private static void scanAndSync(ServerPlayer player, DetectionData data, long now) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.getBoundingBox().getCenter();
        AABB query = new AABB(center, center).inflate(DETECTION_RADIUS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                query,
                target -> isDetectable(player, target, center));
        candidates.sort(Comparator.comparingDouble(
                target -> target.getBoundingBox().getCenter().distanceToSqr(center)));

        Map<Integer, Byte> discovered = new LinkedHashMap<>();
        for (LivingEntity target : candidates) {
            if (discovered.size() >= MAX_TARGETS) {
                break;
            }
            discovered.put(target.getId(), classify(player, target));
        }
        if (data.replaceTargets(discovered)) {
            sendTargetSync(player, true, discovered);
        }
        data.scheduleNextScan(now, SCAN_INTERVAL_TICKS);
    }

    private static boolean isDetectable(ServerPlayer player, LivingEntity target, Vec3 center) {
        return target != player
                && target.isAlive()
                && !target.isRemoved()
                && !(target instanceof ArmorStand)
                && target.level() == player.level()
                && target.getBoundingBox().getCenter().distanceToSqr(center) <= DETECTION_RADIUS_SQR;
    }

    private static byte classify(ServerPlayer player, LivingEntity target) {
        byte category;
        if (EntityUtils.isValidCombatTarget(player, target)) {
            category = DetectionTargetSyncPayload.CATEGORY_HOSTILE;
        } else if (player.isAlliedTo(target)
                || target.isAlliedTo(player)
                || target instanceof TamableAnimal tamable && tamable.isOwnedBy(player)) {
            category = DetectionTargetSyncPayload.CATEGORY_FRIENDLY;
        } else {
            category = DetectionTargetSyncPayload.CATEGORY_NEUTRAL;
        }
        if (target.isInvisible()) {
            category |= DetectionTargetSyncPayload.FLAG_INVISIBLE;
        }
        return category;
    }

    private static void deactivate(ServerPlayer player, DetectionData data, boolean notify) {
        data.deactivate();
        sendTargetSync(player, false, Map.of());
        broadcastEyeState(player, false);
        if (notify) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.detection.disabled"), true);
        }
    }

    private static void sendTargetSync(ServerPlayer player, boolean active, Map<Integer, Byte> targets) {
        List<TargetMarker> markers = new ArrayList<>(Math.min(MAX_TARGETS, targets.size()));
        for (Map.Entry<Integer, Byte> entry : targets.entrySet()) {
            if (markers.size() >= MAX_TARGETS) {
                break;
            }
            markers.add(new TargetMarker(entry.getKey(), entry.getValue()));
        }
        AddonNetwork.sendToPlayer(player, new DetectionTargetSyncPayload(active, markers));
    }

    private static void broadcastEyeState(ServerPlayer caster, boolean active) {
        broadcastEyeState((LivingEntity)caster, active);
    }

    private static void broadcastEyeState(LivingEntity caster, boolean active) {
        DetectionEyeStatePayload payload = new DetectionEyeStatePayload(
                caster.getId(), active, active ? EYE_STATE_TTL_TICKS : 0);
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        double radiusSqr = EYE_OBSERVER_RADIUS * EYE_OBSERVER_RADIUS;
        for (ServerPlayer observer : level.players()) {
            if (observer.distanceToSqr(caster) <= radiusSqr) {
                AddonNetwork.sendToPlayer(observer, payload);
            }
        }
    }

    private static void sendEyeStateTo(ServerPlayer observer, ServerPlayer caster, boolean active) {
        AddonNetwork.sendToPlayer(observer, new DetectionEyeStatePayload(
                caster.getId(), active, active ? EYE_STATE_TTL_TICKS : 0));
    }

    private static boolean isValidActiveCaster(ServerPlayer player) {
        return player != null
                && player.isAlive()
                && !player.isRemoved()
                && !player.isSpectator()
                && !player.hasDisconnected()
                && !KimarisService.isFrozen(player)
                && !EntityUtils.isPetrified(player)
                && !OriginBulletHelper.isSealed(player)
                && isDetectionSelected(player);
    }

    private static boolean isDetectionSelected(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        vars.rebuildSelectedMagicsFromActiveWheel();
        PlayerMagicSelectionService.prepareCurrentSelection(player, vars);
        return vars.is_magus
                && vars.is_magic_circuit_open
                && PlayerMagicSelectionService.isCurrentSelection(vars, DetectionMagic.MAGIC_ID);
    }
}
