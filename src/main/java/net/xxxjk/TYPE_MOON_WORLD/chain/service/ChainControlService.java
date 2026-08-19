package net.xxxjk.TYPE_MOON_WORLD.chain.service;

import net.xxxjk.TYPE_MOON_WORLD.chain.compat.TypeMoonBridge;
import net.xxxjk.TYPE_MOON_WORLD.chain.config.ChainConfig;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.HeavenChainEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ChainControlService {
    private static final int RIGHT_CLICK_COOLDOWN_TICKS = 20;
    private static final Map<UUID, PressState> PRESSES = new HashMap<>();
    private static final Map<UUID, Long> RIGHT_CLICK_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SEEK_SCAN = new HashMap<>();

    public static void handlePlayerInput(ServerPlayer player, boolean held) {
        if (EnumaChainService.isCasting(player)) {
            PRESSES.remove(player.getUUID());
            return;
        }
        TypeMoonBridge.PlayerFormState form = TypeMoonBridge.inspectEnkiduPlayer(player);
        if (!form.eligible()) {
            PRESSES.remove(player.getUUID());
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (held) {
            UUID playerId = player.getUUID();
            if (!PRESSES.containsKey(playerId)) {
                if (isRightClickCoolingDown(player, now)) {
                    return;
                }
                PRESSES.put(playerId, new PressState(now, false));
                summonOrRetarget(player);
                markRightClickCooldown(player, now);
                player.displayClientMessage(Component.translatable("message.typemoonworld.summoned"), true);
            }
            return;
        }
        PRESSES.remove(player.getUUID());
    }

    public static void tickPlayerPresses(ServerLevel level) {
        long now = level.getGameTime();
        for (Map.Entry<UUID, PressState> entry : new ArrayList<>(PRESSES.entrySet())) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof ServerPlayer player) || !player.isAlive() || !TypeMoonBridge.isEnkiduPlayer(player)) {
                PRESSES.remove(entry.getKey());
                continue;
            }
            PressState press = entry.getValue();
            if (!press.retracted() && now - press.startedAt() >= ChainConfig.LONG_PRESS_TICKS) {
                retractAll(player);
                player.displayClientMessage(Component.translatable("message.typemoonworld.retracted"), true);
                PRESSES.put(entry.getKey(), new PressState(press.startedAt(), true));
            }
        }
    }

    public static void summonOrRetarget(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level) || !isEligibleOwner(owner)
            || EnumaChainService.isCasting(owner)) {
            return;
        }
        List<HeavenChainEntity> chains = ownedChains(level, owner.getUUID()).stream()
            .filter(chain -> !chain.isSkillChain())
            .toList();
        chains = new ArrayList<>(chains);
        List<LivingEntity> targets = findTargets(level, owner);
        int physicalChainCount = physicalChainCount(ChainConfig.RIGHT_CLICK_PHYSICAL_CHAIN_COUNT, ChainConfig.CHAIN_COUNT);
        Vec3 origin = HeavenChainEntity.ownerHand(owner);
        Vec3 look = owner.getLookAngle();
        List<List<LivingEntity>> assignments = assignTargetsByLocality(targets, origin, look, physicalChainCount);
        TypeMoonBridge.spawnGoldenGate(level, origin, look);
        while (chains.size() < physicalChainCount) {
            int index = chains.size();
            HeavenChainEntity chain = ModEntities.HEAVEN_CHAIN.get().create(level);
            if (chain == null) {
                break;
            }
            chain.initialize(owner, origin, assignments.get(index), spreadDirection(look, index, physicalChainCount));
            chain.setStackedChainCount(logicalStackCount(index, physicalChainCount, ChainConfig.CHAIN_COUNT));
            chains.add(chain);
            level.addFreshEntity(chain);
        }
        for (int i = 0; i < chains.size(); i++) {
            HeavenChainEntity chain = chains.get(i);
            if (i >= physicalChainCount) {
                chain.beginRetracting();
                continue;
            }
            chain.setStackedChainCountPreservingDamage(logicalStackCount(i, physicalChainCount, ChainConfig.CHAIN_COUNT));
            if (chain.tickCount > 0 || chain.ownerUuid() != null && chain.position().distanceToSqr(origin) > 1.0E-6D) {
                chain.setTargets(assignments.get(i));
            }
        }
    }

    public static void summonSkillBarrage(LivingEntity owner, LivingEntity explicitTarget) {
        if (!(owner.level() instanceof ServerLevel level) || !isEligibleSkillOwner(owner)
            || EnumaChainService.isCasting(owner)) {
            return;
        }
        List<LivingEntity> targets;
        if (explicitTarget != null && isEnemy(owner, explicitTarget)
            && owner.distanceToSqr(explicitTarget) <= ChainConfig.MAX_EXTENSION_SQR) {
            targets = List.of(explicitTarget);
        } else {
            targets = findTargets(level, owner).stream().limit(ChainConfig.SKILL_TARGET_CAP).toList();
        }
        for (LivingEntity target : targets) {
            spawnGatePlanes(level, owner, target);
        }
        if (!targets.isEmpty()) {
            level.playSound(
                null,
                owner.blockPosition(),
                SoundEvents.CHAIN_PLACE,
                owner instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE,
                1.35F,
                0.85F
            );
        }
    }

    private static void spawnGatePlanes(ServerLevel level, LivingEntity owner, LivingEntity target) {
        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        int gates = ChainConfig.SKILL_GATES_PER_PLANE;
        int logicalChains = gates * 2;
        int physicalChainCount = physicalChainCount(ChainConfig.SKILL_PHYSICAL_CHAINS_PER_TARGET, logicalChains);
        int spawnedChains = 0;
        for (int plane = 0; plane < 2; plane++) {
            double vertical = plane == 0
                ? ChainConfig.SKILL_GATE_VERTICAL_OFFSET
                : -ChainConfig.SKILL_GATE_VERTICAL_OFFSET;
            double phase = plane == 0 ? 0.0D : Math.PI / gates;
            for (int i = 0; i < gates; i++) {
                double angle = Math.PI * 2.0D * i / gates + phase;
                Vec3 gate = targetCenter.add(
                    Math.cos(angle) * ChainConfig.SKILL_GATE_RADIUS,
                    vertical,
                    Math.sin(angle) * ChainConfig.SKILL_GATE_RADIUS
                );
                Vec3 direction = targetCenter.subtract(gate).normalize();
                TypeMoonBridge.spawnGoldenGate(level, gate, direction);
                if (spawnedChains < physicalChainCount) {
                    HeavenChainEntity chain = ModEntities.HEAVEN_CHAIN.get().create(level);
                    if (chain == null) {
                        continue;
                    }
                    chain.initializeFromGate(
                        owner,
                        gate,
                        target,
                        direction,
                        ChainConfig.SKILL_GATE_LAUNCH_DELAY + i / 2
                    );
                    chain.setStackedChainCount(logicalStackCount(spawnedChains, physicalChainCount, logicalChains));
                    level.addFreshEntity(chain);
                    spawnedChains++;
                }
            }
        }
    }

    public static void tryRefreshTargets(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        long previous = LAST_SEEK_SCAN.getOrDefault(owner.getUUID(), Long.MIN_VALUE / 2L);
        if (now - previous < ChainConfig.OWNER_SCAN_INTERVAL) {
            return;
        }
        LAST_SEEK_SCAN.put(owner.getUUID(), now);
        List<HeavenChainEntity> seekers = ownedChains(level, owner.getUUID()).stream()
            .filter(HeavenChainEntity::needsTarget)
            .toList();
        if (seekers.isEmpty()) {
            return;
        }
        List<LivingEntity> targets = findTargets(level, owner).stream()
            .filter(target -> !BindingService.isBound(target.getUUID()))
            .toList();
        List<List<LivingEntity>> assignments = assignTargetsByLocality(
            targets,
            HeavenChainEntity.ownerHand(owner),
            owner.getLookAngle(),
            seekers.size()
        );
        for (int i = 0; i < seekers.size(); i++) {
            if (!assignments.get(i).isEmpty()) {
                seekers.get(i).setTargets(assignments.get(i));
            }
        }
    }

    public static void retractAll(LivingEntity owner) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return;
        }
        for (HeavenChainEntity chain : ownedChains(level, owner.getUUID())) {
            chain.beginRetracting();
        }
    }

    public static void cleanupOwner(ServerPlayer player) {
        PRESSES.remove(player.getUUID());
        RIGHT_CLICK_COOLDOWNS.remove(player.getUUID());
        LAST_SEEK_SCAN.remove(player.getUUID());
        retractAll(player);
        BindingService.releaseByOwner(player.serverLevel(), player.getUUID());
    }

    private static boolean isRightClickCoolingDown(ServerPlayer player, long now) {
        return !player.getPersistentData().getBoolean("TypeMoonNoCooldown")
            && RIGHT_CLICK_COOLDOWNS.getOrDefault(player.getUUID(), 0L) > now;
    }

    private static void markRightClickCooldown(ServerPlayer player, long now) {
        if (!player.getPersistentData().getBoolean("TypeMoonNoCooldown")) {
            RIGHT_CLICK_COOLDOWNS.put(player.getUUID(), now + RIGHT_CLICK_COOLDOWN_TICKS);
        }
    }

    private static List<LivingEntity> findTargets(ServerLevel level, LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        AABB area = owner.getBoundingBox().inflate(ChainConfig.TARGET_RADIUS);
        Map<UUID, LivingEntity> candidates = new HashMap<>();
        level.getEntitiesOfClass(LivingEntity.class, area, target -> isEnemy(owner, target))
            .forEach(target -> candidates.put(target.getUUID(), target));
        level.players().stream()
            .filter(target -> area.intersects(target.getBoundingBox()))
            .filter(target -> isEnemy(owner, target))
            .forEach(target -> candidates.put(target.getUUID(), target));
        List<LivingEntity> targets = new ArrayList<>(candidates.values());
        targets.removeIf(target -> !TargetingMath.isInsideForwardHemisphere(
            owner.getEyePosition(), look, target.getEyePosition(), ChainConfig.TARGET_RADIUS_SQR
        ));
        targets.sort(Comparator.<LivingEntity>comparingDouble(owner::distanceToSqr).thenComparing(Entity::getUUID));
        return targets;
    }

    private static List<List<LivingEntity>> assignTargetsByLocality(
        List<LivingEntity> targets,
        Vec3 origin,
        Vec3 look,
        int chainCount
    ) {
        List<List<LivingEntity>> assignments = new ArrayList<>(chainCount);
        for (int i = 0; i < chainCount; i++) {
            assignments.add(new ArrayList<>());
        }
        if (chainCount <= 0) {
            return assignments;
        }
        List<Vec3> lanes = new ArrayList<>(chainCount);
        for (int i = 0; i < chainCount; i++) {
            lanes.add(spreadDirection(look, i, chainCount));
        }
        for (LivingEntity target : targets) {
            Vec3 direction = target.getEyePosition().subtract(origin);
            int index = bestLocalityLane(direction, lanes, assignments);
            assignments.get(index).add(target);
        }
        for (int i = 0; i < assignments.size(); i++) {
            assignments.set(i, nearestPathOrder(origin, assignments.get(i)));
        }
        return assignments;
    }

    private static int bestLocalityLane(
        Vec3 direction,
        List<Vec3> lanes,
        List<List<LivingEntity>> assignments
    ) {
        if (lanes.size() == 1 || direction.lengthSqr() <= 1.0E-10D) {
            return 0;
        }
        Vec3 normalized = direction.normalize();
        int bestIndex = 0;
        double bestScore = -Double.MAX_VALUE;
        for (int i = 0; i < lanes.size(); i++) {
            double score = normalized.dot(lanes.get(i)) - assignments.get(i).size() * 0.025D;
            if (score > bestScore + 1.0E-8D
                || Math.abs(score - bestScore) <= 1.0E-8D
                && assignments.get(i).size() < assignments.get(bestIndex).size()) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static List<LivingEntity> nearestPathOrder(Vec3 origin, List<LivingEntity> targets) {
        if (targets.size() <= 1) {
            return targets;
        }
        List<LivingEntity> pending = new ArrayList<>(targets);
        List<LivingEntity> ordered = new ArrayList<>(targets.size());
        Vec3 cursor = origin;
        while (!pending.isEmpty()) {
            int bestIndex = 0;
            double bestDistance = Double.MAX_VALUE;
            for (int i = 0; i < pending.size(); i++) {
                LivingEntity target = pending.get(i);
                double distance = cursor.distanceToSqr(target.position());
                if (distance < bestDistance - 1.0E-8D
                    || Math.abs(distance - bestDistance) <= 1.0E-8D
                    && target.getUUID().compareTo(pending.get(bestIndex).getUUID()) < 0) {
                    bestDistance = distance;
                    bestIndex = i;
                }
            }
            LivingEntity next = pending.remove(bestIndex);
            ordered.add(next);
            cursor = next.position();
        }
        return ordered;
    }

    private static boolean isEnemy(LivingEntity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isAlliedTo(owner)) {
            return false;
        }
        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return owner.canAttack(target);
    }

    public static boolean isEligibleOwner(LivingEntity owner) {
        return owner instanceof ServerPlayer player ? TypeMoonBridge.isEnkiduPlayer(player) : TypeMoonBridge.isEnkiduNpc(owner);
    }

    public static boolean isEligibleSkillOwner(LivingEntity owner) {
        if (owner instanceof ServerPlayer player) {
            return TypeMoonBridge.isEnkiduPlayer(player) || TypeMoonBridge.isGilgameshPlayer(player);
        }
        return TypeMoonBridge.isEnkiduNpc(owner) || TypeMoonBridge.isGilgameshNpc(owner);
    }

    public static boolean isEligibleEnumaOwner(LivingEntity owner) {
        return owner instanceof ServerPlayer player
            ? TypeMoonBridge.isEnkiduPlayer(player)
            : TypeMoonBridge.isEnkiduNpc(owner);
    }

    public static boolean isEnemyForEnuma(LivingEntity owner, LivingEntity target) {
        return isEnemy(owner, target);
    }

    private static List<HeavenChainEntity> ownedChains(ServerLevel level, UUID ownerId) {
        Entity owner = level.getEntity(ownerId);
        if (owner == null) {
            return List.of();
        }
        return level.getEntitiesOfClass(
            HeavenChainEntity.class,
            owner.getBoundingBox().inflate(ChainConfig.MAX_EXTENSION + 10.0D),
            chain -> ownerId.equals(chain.ownerUuid()) && chain.isAlive()
        );
    }

    private static Vec3 spreadDirection(Vec3 look, int index, int count) {
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 1.0E-5D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        }
        right = right.normalize();
        Vec3 vertical = right.cross(look).normalize();
        double angle = (Math.PI * 2.0D * index) / Math.max(1, count);
        return look.add(right.scale(Math.cos(angle) * 0.16D)).add(vertical.scale(Math.sin(angle) * 0.16D)).normalize();
    }

    private static int physicalChainCount(int configuredPhysicalCount, int logicalChainCount) {
        return Math.max(1, Math.min(configuredPhysicalCount, logicalChainCount));
    }

    private static int logicalStackCount(int index, int physicalChainCount, int logicalChainCount) {
        int base = logicalChainCount / physicalChainCount;
        int remainder = logicalChainCount % physicalChainCount;
        return base + (index < remainder ? 1 : 0);
    }

    private record PressState(long startedAt, boolean retracted) {
    }

    private ChainControlService() {
    }
}

