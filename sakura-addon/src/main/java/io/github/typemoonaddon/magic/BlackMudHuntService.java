package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowCommandMode;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.registry.ModAttachments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Owns the transient familiar encirclement, its black-shadow support, and shared mana pool. */
public final class BlackMudHuntService {
    private static final int MAX_FAMILIARS = 6;
    private static final int MAX_BLACK_SHADOWS = 6;
    private static final double TARGET_RANGE = 64.0D;
    private static final double ORBIT_ANGULAR_SPEED = 0.045D;
    private static final float GROWTH_STEP = 0.05F;
    private static final double MANA_PER_GROWTH_STEP = 1.0D;

    private static final Map<UUID, HuntState> HUNTS_BY_ID = new HashMap<>();
    private static final Map<UUID, UUID> HUNT_BY_FAMILIAR = new HashMap<>();

    public enum StartResult {
        STARTED,
        NO_TARGET,
        NO_FAMILIARS
    }

    public record HuntOrder(LivingEntity target, Vec3 destination) {
    }

    public static StartResult start(ServerPlayer owner) {
        LivingEntity target = rayTraceTarget(owner);
        if (target == null) {
            return StartResult.NO_TARGET;
        }

        List<ShadowFamiliarEntity> familiars = new ArrayList<>();
        for (Entity entity : owner.serverLevel().getAllEntities()) {
            if (entity instanceof ShadowFamiliarEntity familiar
                && owner.getUUID().equals(familiar.getOwnerId())
                && familiar.isAlive()
                && !familiar.isForming()
                && !HUNT_BY_FAMILIAR.containsKey(familiar.getUUID())) {
                familiars.add(familiar);
            }
        }
        familiars.sort(Comparator.comparingDouble(target::distanceToSqr));
        if (familiars.size() > MAX_FAMILIARS) {
            familiars = new ArrayList<>(familiars.subList(0, MAX_FAMILIARS));
        }

        List<BlackShadowEntity> blackShadows = new ArrayList<>();
        for (Entity entity : owner.serverLevel().getAllEntities()) {
            if (entity instanceof BlackShadowEntity shadow
                && owner.getUUID().equals(shadow.getOwnerId())
                && shadow.isAlive()
                && shadow.canAttack(target)) {
                blackShadows.add(shadow);
            }
        }
        blackShadows.sort(Comparator.comparingDouble(target::distanceToSqr));
        if (blackShadows.size() > MAX_BLACK_SHADOWS) {
            blackShadows = new ArrayList<>(blackShadows.subList(0, MAX_BLACK_SHADOWS));
        }
        if (familiars.isEmpty()
            && blackShadows.isEmpty()
            && !PollutionService.hasControlledCombatant(owner)) {
            return StartResult.NO_FAMILIARS;
        }

        float uniformSize = 0.0F;
        double sharedMana = 0.0D;
        List<UUID> familiarIds = new ArrayList<>(familiars.size());
        for (ShadowFamiliarEntity familiar : familiars) {
            familiarIds.add(familiar.getUUID());
            uniformSize += familiar.getSummonSize();
            sharedMana = addWithoutGameplayLimit(sharedMana, familiar.takeStoredGrowthMana());
        }
        if (!familiars.isEmpty()) {
            uniformSize /= familiars.size();
        }
        List<UUID> blackShadowIds = new ArrayList<>(blackShadows.size());
        for (BlackShadowEntity shadow : blackShadows) {
            blackShadowIds.add(shadow.getUUID());
        }

        HuntState state = new HuntState(
            UUID.randomUUID(),
            owner.getUUID(),
            owner.level().dimension(),
            target.getUUID(),
            familiarIds,
            blackShadowIds,
            sharedMana,
            uniformSize
        );
        HUNTS_BY_ID.put(state.huntId, state);
        for (ShadowFamiliarEntity familiar : familiars) {
            HUNT_BY_FAMILIAR.put(familiar.getUUID(), state.huntId);
            familiar.prepareForHunt(target, uniformSize);
        }

        var data = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
        data.setActiveShadowCommandMode(ShadowCommandMode.HUNT);
        owner.syncData(ModAttachments.IMAGINARY_SPACE.get());
        return StartResult.STARTED;
    }

    public static void end(ServerPlayer owner) {
        finishAll(owner.getUUID(), owner.server, true, true);
    }

    @Nullable
    public static LivingEntity targetForOwner(ServerPlayer owner) {
        for (HuntState state : HUNTS_BY_ID.values()) {
            if (state.ownerId.equals(owner.getUUID())) {
                return living(owner.getServer().getLevel(state.dimension), state.targetId);
            }
        }
        return null;
    }

    @Nullable
    public static LivingEntity targetFor(BlackShadowEntity shadow) {
        for (HuntState state : HUNTS_BY_ID.values()) {
            if (state.ownerId.equals(shadow.getOwnerId())
                && state.blackShadowIds.contains(shadow.getUUID())
                && shadow.level() instanceof ServerLevel level
                && level.dimension().equals(state.dimension)) {
                return living(level, state.targetId);
            }
        }
        return null;
    }

    public static void tick(MinecraftServer server) {
        for (UUID huntId : List.copyOf(HUNTS_BY_ID.keySet())) {
            HuntState state = HUNTS_BY_ID.get(huntId);
            if (state == null) {
                continue;
            }
            UUID ownerId = state.ownerId;
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
            ServerLevel level = server.getLevel(state.dimension);
            LivingEntity target = living(level, state.targetId);
            if (owner == null
                || !owner.isAlive()
                || owner.isSpectator()
                || !owner.level().dimension().equals(state.dimension)
                || owner.getData(ModAttachments.IMAGINARY_SPACE.get()).activeShadowCommandMode()
                    != ShadowCommandMode.HUNT
                || target == null
                || !validTarget(owner, target)) {
                finish(huntId, server, true, owner != null);
                continue;
            }

            List<ShadowFamiliarEntity> members = members(level, state);
            int blackShadowMemberCount = pruneAndCountBlackShadows(level, state);
            if (members.isEmpty()
                && blackShadowMemberCount == 0
                && !PollutionService.hasControlledCombatant(owner)) {
                finish(huntId, server, true, true);
                continue;
            }
            for (int index = state.familiarIds.size() - 1; index >= 0; index--) {
                UUID familiarId = state.familiarIds.get(index);
                if (!containsFamiliar(members, familiarId)) {
                    state.familiarIds.remove(index);
                }
            }
            state.phase += ORBIT_ANGULAR_SPEED;
            if (state.phase >= Math.PI * 2.0D) {
                state.phase -= Math.PI * 2.0D;
            }
            if (!members.isEmpty()) {
                growTogether(state, members);
            }

            if (level.getGameTime() % 10L == 0L) {
                Vec3 center = target.getBoundingBox().getCenter();
                GrailParticleService.send(
                    level,
                    owner,
                    ParticleTypes.SQUID_INK,
                    center.x,
                    center.y,
                    center.z,
                    8,
                    target.getBbWidth() * 0.75D + 1.0D,
                    target.getBbHeight() * 0.4D + 0.5D,
                    target.getBbWidth() * 0.75D + 1.0D,
                    0.015D
                );
            }
        }
    }

    public static boolean depositMana(ShadowFamiliarEntity familiar, double amount) {
        if (amount <= 0.0D) {
            return false;
        }
        UUID huntId = HUNT_BY_FAMILIAR.get(familiar.getUUID());
        HuntState state = huntId == null ? null : HUNTS_BY_ID.get(huntId);
        if (state == null || !state.familiarIds.contains(familiar.getUUID())) {
            return false;
        }
        state.sharedMana = addWithoutGameplayLimit(state.sharedMana, amount);
        return true;
    }

    public static boolean isHunting(ShadowFamiliarEntity familiar) {
        UUID huntId = HUNT_BY_FAMILIAR.get(familiar.getUUID());
        HuntState state = huntId == null ? null : HUNTS_BY_ID.get(huntId);
        return state != null && state.familiarIds.contains(familiar.getUUID());
    }

    public static boolean isHuntTarget(ShadowFamiliarEntity familiar, @Nullable LivingEntity target) {
        HuntOrder order = orderFor(familiar);
        return order != null && order.target() == target;
    }

    @Nullable
    public static HuntOrder orderFor(ShadowFamiliarEntity familiar) {
        UUID huntId = HUNT_BY_FAMILIAR.get(familiar.getUUID());
        HuntState state = huntId == null ? null : HUNTS_BY_ID.get(huntId);
        if (state == null || !(familiar.level() instanceof ServerLevel level)
            || !level.dimension().equals(state.dimension)) {
            return null;
        }
        int slot = state.familiarIds.indexOf(familiar.getUUID());
        LivingEntity target = living(level, state.targetId);
        if (slot < 0 || target == null) {
            return null;
        }

        int count = state.familiarIds.size();
        double angle = state.phase + slot * Math.PI * 2.0D / count;
        double radius = target.getBbWidth() * 0.5D + familiar.getBbWidth() * 0.5D + 1.75D;
        double verticalAmplitude = Math.max(0.8D, target.getBbHeight() * 0.35D);
        double desiredY = target.getBoundingBox().getCenter().y
            + Math.sin(angle * 2.0D) * verticalAmplitude
            - familiar.getBbHeight() * 0.5D;
        desiredY = Math.max(target.getY() + 0.15D, desiredY);
        Vec3 destination = new Vec3(
            target.getX() + Math.cos(angle) * radius,
            desiredY,
            target.getZ() + Math.sin(angle) * radius
        );
        return new HuntOrder(target, destination);
    }

    public static void playerLoggedOut(ServerPlayer player) {
        finishAll(player.getUUID(), player.server, true, false);
    }

    public static void playerChangedDimension(ServerPlayer player) {
        finishAll(player.getUUID(), player.server, true, true);
    }

    public static void serverStopping(MinecraftServer server) {
        for (UUID huntId : List.copyOf(HUNTS_BY_ID.keySet())) {
            finish(huntId, server, true, false);
        }
        HUNTS_BY_ID.clear();
        HUNT_BY_FAMILIAR.clear();
    }

    private static void growTogether(HuntState state, List<ShadowFamiliarEntity> members) {
        float possibleGrowth = (float)(state.sharedMana / members.size()
            / MANA_PER_GROWTH_STEP * GROWTH_STEP);
        float growth = Math.min(
            GROWTH_STEP,
            Math.min(ShadowFamiliarEntity.MAX_SUMMON_SIZE - state.uniformSize, possibleGrowth)
        );
        if (growth > 0.0F) {
            double manaUsed = growth / GROWTH_STEP * MANA_PER_GROWTH_STEP * members.size();
            state.sharedMana = Math.max(0.0D, state.sharedMana - manaUsed);
            state.uniformSize += growth;
        }
        for (ShadowFamiliarEntity familiar : members) {
            familiar.setSummonSize(state.uniformSize);
        }
    }

    private static List<ShadowFamiliarEntity> members(@Nullable ServerLevel level, HuntState state) {
        List<ShadowFamiliarEntity> members = new ArrayList<>();
        if (level == null) {
            return members;
        }
        for (UUID familiarId : state.familiarIds) {
            Entity entity = level.getEntity(familiarId);
            if (entity instanceof ShadowFamiliarEntity familiar
                && familiar.isAlive()
                && state.ownerId.equals(familiar.getOwnerId())) {
                members.add(familiar);
            } else {
                HUNT_BY_FAMILIAR.remove(familiarId, state.huntId);
            }
        }
        return members;
    }

    private static int pruneAndCountBlackShadows(@Nullable ServerLevel level, HuntState state) {
        if (level == null) {
            state.blackShadowIds.clear();
            return 0;
        }
        int count = 0;
        for (int index = state.blackShadowIds.size() - 1; index >= 0; index--) {
            UUID shadowId = state.blackShadowIds.get(index);
            Entity entity = level.getEntity(shadowId);
            if (entity instanceof BlackShadowEntity shadow
                && shadow.isAlive()
                && state.ownerId.equals(shadow.getOwnerId())) {
                count++;
            } else {
                state.blackShadowIds.remove(index);
            }
        }
        return count;
    }

    private static boolean containsFamiliar(List<ShadowFamiliarEntity> familiars, UUID id) {
        for (ShadowFamiliarEntity familiar : familiars) {
            if (familiar.getUUID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static void finish(
        UUID huntId,
        MinecraftServer server,
        boolean resetMode,
        boolean syncOwner
    ) {
        HuntState state = HUNTS_BY_ID.remove(huntId);
        if (state == null) {
            return;
        }
        UUID ownerId = state.ownerId;
        ServerLevel level = server.getLevel(state.dimension);
        List<ShadowFamiliarEntity> members = members(level, state);
        double share = members.isEmpty() ? 0.0D : state.sharedMana / members.size();
        for (UUID familiarId : state.familiarIds) {
            HUNT_BY_FAMILIAR.remove(familiarId, huntId);
        }
        for (ShadowFamiliarEntity familiar : members) {
            familiar.addStoredGrowthMana(share);
            familiar.finishHunt();
        }

        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (resetMode && owner != null && !hasHunt(ownerId)) {
            var data = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
            if (data.setActiveShadowCommandMode(ShadowCommandMode.FREE) && syncOwner) {
                owner.syncData(ModAttachments.IMAGINARY_SPACE.get());
            }
        }
    }

    private static void finishAll(
        UUID ownerId,
        MinecraftServer server,
        boolean resetMode,
        boolean syncOwner
    ) {
        List<UUID> huntIds = HUNTS_BY_ID.values().stream()
            .filter(state -> state.ownerId.equals(ownerId))
            .map(state -> state.huntId)
            .toList();
        for (UUID huntId : huntIds) {
            finish(huntId, server, false, false);
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (resetMode && owner != null) {
            var data = owner.getData(ModAttachments.IMAGINARY_SPACE.get());
            if (data.setActiveShadowCommandMode(ShadowCommandMode.FREE) && syncOwner) {
                owner.syncData(ModAttachments.IMAGINARY_SPACE.get());
            }
        }
    }

    private static boolean hasHunt(UUID ownerId) {
        return HUNTS_BY_ID.values().stream().anyMatch(state -> state.ownerId.equals(ownerId));
    }

    @Nullable
    private static LivingEntity rayTraceTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 intendedEnd = start.add(player.getLookAngle().scale(TARGET_RANGE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(
            start,
            intendedEnd,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? intendedEnd : blockHit.getLocation();
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        LivingEntity closest = null;
        double closestDistance = start.distanceToSqr(end);
        for (LivingEntity candidate : player.level().getEntitiesOfClass(
            LivingEntity.class,
            search,
            entity -> validTarget(player, entity)
        )) {
            AABB bounds = candidate.getBoundingBox().inflate(Math.max(0.3F, candidate.getPickRadius()));
            var intersection = bounds.clip(start, end);
            if (intersection.isPresent()) {
                double distance = start.distanceToSqr(intersection.get());
                if (distance <= closestDistance) {
                    closest = candidate;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    private static boolean validTarget(ServerPlayer owner, LivingEntity target) {
        if (target == owner
            || !target.isAlive()
            || !target.isAttackable()
            || target.isInvulnerable()
            || owner.isAlliedTo(target)
            || target.isAlliedTo(owner)) {
            return false;
        }
        return !(target instanceof Player player) || !player.isCreative() && !player.isSpectator();
    }

    @Nullable
    private static LivingEntity living(@Nullable ServerLevel level, UUID id) {
        Entity entity = level == null ? null : level.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static double addWithoutGameplayLimit(double current, double added) {
        if (!Double.isFinite(added) || added <= 0.0D) {
            return current;
        }
        return current > Double.MAX_VALUE - added ? Double.MAX_VALUE : current + added;
    }

    private static final class HuntState {
        private final UUID huntId;
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final UUID targetId;
        private final List<UUID> familiarIds;
        private final List<UUID> blackShadowIds;
        private double sharedMana;
        private float uniformSize;
        private double phase;

        private HuntState(
            UUID huntId,
            UUID ownerId,
            ResourceKey<Level> dimension,
            UUID targetId,
            List<UUID> familiarIds,
            List<UUID> blackShadowIds,
            double sharedMana,
            float uniformSize
        ) {
            this.huntId = huntId;
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.targetId = targetId;
            this.familiarIds = familiarIds;
            this.blackShadowIds = blackShadowIds;
            this.sharedMana = sharedMana;
            this.uniformSize = uniformSize;
        }
    }

    private BlackMudHuntService() {
    }
}
