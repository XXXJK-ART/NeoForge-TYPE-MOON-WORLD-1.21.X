package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.example.typemoonaddon.registry.AddonAttachments;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SakuraBlackMudHuntService {
    private static final int MAX_FAMILIARS = 6;
    private static final int MAX_BLACK_SHADOWS = 6;
    private static final double TARGET_RANGE = 64.0D;
    private static final double ORBIT_ANGULAR_SPEED = 0.045D;
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
        LivingEntity target = SakuraMagicTargeting.rayTraceLiving(owner, TARGET_RANGE);
        if (target == null) {
            return StartResult.NO_TARGET;
        }
        List<SakuraShadowFamiliarEntity> familiars = new ArrayList<>();
        List<SakuraBlackShadowEntity> blackShadows = new ArrayList<>();
        for (Entity entity : owner.serverLevel().getAllEntities()) {
            if (entity instanceof SakuraShadowFamiliarEntity familiar
                    && owner.getUUID().equals(familiar.getOwnerId())
                    && familiar.isAlive()
                    && !familiar.isForming()
                    && !HUNT_BY_FAMILIAR.containsKey(familiar.getUUID())) {
                familiars.add(familiar);
            } else if (entity instanceof SakuraBlackShadowEntity shadow
                    && owner.getUUID().equals(shadow.getOwnerId())
                    && shadow.isAlive()
                    && shadow.canAttack(target)) {
                blackShadows.add(shadow);
            }
        }
        familiars.sort(Comparator.comparingDouble(target::distanceToSqr));
        blackShadows.sort(Comparator.comparingDouble(target::distanceToSqr));
        if (familiars.size() > MAX_FAMILIARS) {
            familiars = new ArrayList<>(familiars.subList(0, MAX_FAMILIARS));
        }
        if (blackShadows.size() > MAX_BLACK_SHADOWS) {
            blackShadows = new ArrayList<>(blackShadows.subList(0, MAX_BLACK_SHADOWS));
        }
        if (familiars.isEmpty() && blackShadows.isEmpty()) {
            return StartResult.NO_FAMILIARS;
        }
        float uniformSize = 0.0F;
        double sharedMana = 0.0D;
        List<UUID> familiarIds = new ArrayList<>(familiars.size());
        for (SakuraShadowFamiliarEntity familiar : familiars) {
            familiarIds.add(familiar.getUUID());
            uniformSize += familiar.getSummonSize();
            sharedMana += familiar.takeStoredGrowthMana();
        }
        if (!familiars.isEmpty()) {
            uniformSize /= familiars.size();
        }
        List<UUID> blackShadowIds = blackShadows.stream().map(Entity::getUUID).toList();
        HuntState state = new HuntState(UUID.randomUUID(), owner.getUUID(), owner.level().dimension(), target.getUUID(), familiarIds, new ArrayList<>(blackShadowIds), sharedMana, uniformSize);
        HUNTS_BY_ID.put(state.huntId, state);
        for (SakuraShadowFamiliarEntity familiar : familiars) {
            HUNT_BY_FAMILIAR.put(familiar.getUUID(), state.huntId);
            familiar.prepareForHunt(target, uniformSize);
        }
        owner.getData(AddonAttachments.IMAGINARY_SPACE.get()).setActiveShadowCommandMode(ImaginarySpaceData.ShadowCommandMode.HUNT);
        AddonAttachments.sync(owner, AddonAttachments.IMAGINARY_SPACE);
        return StartResult.STARTED;
    }

    public static void end(ServerPlayer owner) {
        finishAll(owner.getUUID(), owner.server, true, true);
    }

    @Nullable
    public static LivingEntity targetFor(SakuraBlackShadowEntity shadow) {
        for (HuntState state : HUNTS_BY_ID.values()) {
            if (state.ownerId.equals(shadow.getOwnerId()) && state.blackShadowIds.contains(shadow.getUUID()) && shadow.level() instanceof ServerLevel level && level.dimension().equals(state.dimension)) {
                return living(level, state.targetId);
            }
        }
        return null;
    }

    @Nullable
    public static LivingEntity targetForOwner(ServerPlayer owner) {
        for (HuntState state : HUNTS_BY_ID.values()) {
            if (state.ownerId.equals(owner.getUUID())) {
                return living(owner.server.getLevel(state.dimension), state.targetId);
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
            ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerId);
            ServerLevel level = server.getLevel(state.dimension);
            LivingEntity target = living(level, state.targetId);
            if (owner == null || level == null || target == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level || !SakuraMagicTargeting.validHostileTarget(owner, target)) {
                finish(huntId, server, true, owner != null);
                continue;
            }
            List<SakuraShadowFamiliarEntity> members = members(level, state);
            int shadows = pruneAndCountBlackShadows(level, state);
            if (members.isEmpty() && shadows == 0) {
                finish(huntId, server, true, true);
                continue;
            }
            state.phase = (state.phase + ORBIT_ANGULAR_SPEED) % (Math.PI * 2.0D);
            if (level.getGameTime() % 10L == 0L) {
                Vec3 center = target.getBoundingBox().getCenter();
                SakuraParticleService.send(level, ParticleTypes.SQUID_INK, center.x, center.y, center.z, 8, target.getBbWidth() * 0.75D + 1.0D, target.getBbHeight() * 0.4D + 0.5D, target.getBbWidth() * 0.75D + 1.0D, 0.015D);
            }
        }
    }

    @Nullable
    public static HuntOrder orderFor(SakuraShadowFamiliarEntity familiar) {
        UUID huntId = HUNT_BY_FAMILIAR.get(familiar.getUUID());
        HuntState state = huntId == null ? null : HUNTS_BY_ID.get(huntId);
        if (state == null || !(familiar.level() instanceof ServerLevel level) || !level.dimension().equals(state.dimension)) {
            return null;
        }
        int slot = state.familiarIds.indexOf(familiar.getUUID());
        LivingEntity target = living(level, state.targetId);
        if (slot < 0 || target == null) {
            return null;
        }
        int count = state.familiarIds.size();
        double angle = state.phase + slot * Math.PI * 2.0D / Math.max(1, count);
        double radius = target.getBbWidth() * 0.5D + familiar.getBbWidth() * 0.5D + 1.75D;
        Vec3 destination = new Vec3(
                target.getX() + Math.cos(angle) * radius,
                Math.max(target.getY() + 0.15D, target.getBoundingBox().getCenter().y + Math.sin(angle * 2.0D) * 0.9D - familiar.getBbHeight() * 0.5D),
                target.getZ() + Math.sin(angle) * radius
        );
        return new HuntOrder(target, destination);
    }

    public static void playerUnavailable(ServerPlayer player) {
        finishAll(player.getUUID(), player.server, true, false);
    }

    public static void serverStopping(MinecraftServer server) {
        for (UUID huntId : List.copyOf(HUNTS_BY_ID.keySet())) {
            finish(huntId, server, true, false);
        }
        HUNTS_BY_ID.clear();
        HUNT_BY_FAMILIAR.clear();
    }

    private static List<SakuraShadowFamiliarEntity> members(ServerLevel level, HuntState state) {
        List<SakuraShadowFamiliarEntity> members = new ArrayList<>();
        for (UUID familiarId : List.copyOf(state.familiarIds)) {
            Entity entity = level.getEntity(familiarId);
            if (entity instanceof SakuraShadowFamiliarEntity familiar && familiar.isAlive() && state.ownerId.equals(familiar.getOwnerId())) {
                members.add(familiar);
            } else {
                HUNT_BY_FAMILIAR.remove(familiarId, state.huntId);
                state.familiarIds.remove(familiarId);
            }
        }
        return members;
    }

    private static int pruneAndCountBlackShadows(ServerLevel level, HuntState state) {
        int count = 0;
        for (UUID id : List.copyOf(state.blackShadowIds)) {
            Entity entity = level.getEntity(id);
            if (entity instanceof SakuraBlackShadowEntity shadow && shadow.isAlive() && state.ownerId.equals(shadow.getOwnerId())) {
                count++;
            } else {
                state.blackShadowIds.remove(id);
            }
        }
        return count;
    }

    private static void finish(UUID huntId, MinecraftServer server, boolean resetMode, boolean syncOwner) {
        HuntState state = HUNTS_BY_ID.remove(huntId);
        if (state == null) {
            return;
        }
        ServerLevel level = server.getLevel(state.dimension);
        List<SakuraShadowFamiliarEntity> members = level == null ? List.of() : members(level, state);
        double share = members.isEmpty() ? 0.0D : state.sharedMana / members.size();
        for (UUID familiarId : state.familiarIds) {
            HUNT_BY_FAMILIAR.remove(familiarId, huntId);
        }
        for (SakuraShadowFamiliarEntity familiar : members) {
            familiar.addStoredGrowthMana(share);
            familiar.finishHunt();
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerId);
        if (resetMode && owner != null) {
            var data = owner.getData(AddonAttachments.IMAGINARY_SPACE.get());
            if (data.setActiveShadowCommandMode(ImaginarySpaceData.ShadowCommandMode.FREE) && syncOwner) {
                AddonAttachments.sync(owner, AddonAttachments.IMAGINARY_SPACE);
            }
        }
    }

    private static void finishAll(UUID ownerId, MinecraftServer server, boolean resetMode, boolean syncOwner) {
        HUNTS_BY_ID.values().stream().filter(state -> state.ownerId.equals(ownerId)).map(state -> state.huntId).toList().forEach(id -> finish(id, server, false, false));
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (resetMode && owner != null) {
            var data = owner.getData(AddonAttachments.IMAGINARY_SPACE.get());
            if (data.setActiveShadowCommandMode(ImaginarySpaceData.ShadowCommandMode.FREE) && syncOwner) {
                AddonAttachments.sync(owner, AddonAttachments.IMAGINARY_SPACE);
            }
        }
    }

    @Nullable
    private static LivingEntity living(@Nullable ServerLevel level, UUID id) {
        Entity entity = level == null ? null : level.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static final class HuntState {
        private final UUID huntId;
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private final UUID targetId;
        private final List<UUID> familiarIds;
        private final List<UUID> blackShadowIds;
        private final double sharedMana;
        private double phase;

        private HuntState(UUID huntId, UUID ownerId, ResourceKey<Level> dimension, UUID targetId, List<UUID> familiarIds, List<UUID> blackShadowIds, double sharedMana, float uniformSize) {
            this.huntId = huntId;
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.targetId = targetId;
            this.familiarIds = familiarIds;
            this.blackShadowIds = blackShadowIds;
            this.sharedMana = sharedMana + uniformSize * 0.0D;
        }
    }

    private SakuraBlackMudHuntService() {
    }
}
