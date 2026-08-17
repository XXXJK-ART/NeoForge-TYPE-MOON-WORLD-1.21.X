package net.xxxjk.TYPE_MOON_WORLD.chain.service;

import net.xxxjk.TYPE_MOON_WORLD.chain.compat.TypeMoonBridge;
import net.xxxjk.TYPE_MOON_WORLD.chain.config.ChainConfig;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.HeavenChainBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.HeavenChainEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class BindingService {
    private static final double POSITION_CORRECTION_EPSILON_SQR = 1.0E-6D;
    private static final Map<BindingKey, Binding> BINDINGS = new HashMap<>();
    private static final Map<UUID, Set<BindingKey>> BINDINGS_BY_CHAIN = new HashMap<>();
    private static final Map<UUID, TargetState> TARGET_STATES = new HashMap<>();

    public static void bind(ServerLevel level, LivingEntity owner, LivingEntity target, HeavenChainEntity chain) {
        bind(level, owner, target, chain, 0);
    }

    public static void bind(
        ServerLevel level,
        LivingEntity owner,
        LivingEntity target,
        HeavenChainEntity chain,
        int minimumTicks
    ) {
        long minimumBoundUntil = level.getGameTime() + Math.max(0, minimumTicks);
        for (LivingEntity boundTarget : mountedBindingTargets(owner, target)) {
            bindTargetWithSingleChain(level, owner, boundTarget, chain, minimumBoundUntil);
        }
    }

    public static void bindAll(
        ServerLevel level,
        LivingEntity owner,
        LivingEntity target,
        List<HeavenChainEntity> chains,
        int minimumTicks
    ) {
        if (chains.isEmpty() || !target.isAlive()) {
            return;
        }
        long minimumBoundUntil = level.getGameTime() + Math.max(0, minimumTicks);
        for (LivingEntity boundTarget : mountedBindingTargets(owner, target)) {
            bindTargetWithChains(level, owner, boundTarget, chains, minimumBoundUntil);
        }
    }

    private static void bindTargetWithSingleChain(
        ServerLevel level,
        LivingEntity owner,
        LivingEntity target,
        HeavenChainEntity chain,
        long minimumBoundUntil
    ) {
        TargetState state = TARGET_STATES.computeIfAbsent(target.getUUID(), ignored -> captureTargetState(target));
        Vec3 pathAnchor = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        int stackIndex = bindingsForTarget(level.dimension(), target.getUUID()).size();
        if (!bindSingle(level, owner, target, chain, minimumBoundUntil, pathAnchor, stackIndex)) {
            return;
        }
        holdTarget(target, state);
        applyEffects(target);
    }

    private static void bindTargetWithChains(
        ServerLevel level,
        LivingEntity owner,
        LivingEntity target,
        List<HeavenChainEntity> chains,
        long minimumBoundUntil
    ) {
        TargetState state = TARGET_STATES.computeIfAbsent(target.getUUID(), ignored -> captureTargetState(target));
        Vec3 pathAnchor = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        int stackIndex = bindingsForTarget(level.dimension(), target.getUUID()).size();
        boolean addedAny = false;
        for (HeavenChainEntity chain : chains) {
            if (bindSingle(level, owner, target, chain, minimumBoundUntil, pathAnchor, stackIndex)) {
                stackIndex++;
                addedAny = true;
            }
        }
        if (addedAny) {
            holdTarget(target, state);
            applyEffects(target);
        }
    }

    private static List<LivingEntity> mountedBindingTargets(LivingEntity owner, LivingEntity target) {
        List<LivingEntity> result = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        Entity root = target.getRootVehicle();
        if (root instanceof LivingEntity livingRoot && livingRoot.isAlive() && livingRoot.level() == target.level()) {
            addMountedTarget(owner, livingRoot, result, seen);
            addLivingPassengers(owner, livingRoot, result, seen);
            return result;
        }
        addMountedTarget(owner, target, result, seen);
        addLivingPassengers(owner, target, result, seen);
        return result;
    }

    private static void addLivingPassengers(
        LivingEntity owner,
        Entity vehicle,
        List<LivingEntity> result,
        Set<UUID> seen
    ) {
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof LivingEntity living && living.isAlive() && living.level() == vehicle.level()) {
                addMountedTarget(owner, living, result, seen);
            }
            addLivingPassengers(owner, passenger, result, seen);
        }
    }

    private static void addMountedTarget(
        LivingEntity owner,
        LivingEntity target,
        List<LivingEntity> result,
        Set<UUID> seen
    ) {
        if (target != owner && seen.add(target.getUUID())) {
            result.add(target);
        }
    }

    private static boolean bindSingle(
        ServerLevel level,
        LivingEntity owner,
        LivingEntity target,
        HeavenChainEntity chain,
        long minimumBoundUntil,
        Vec3 pathAnchor,
        int stackIndex
    ) {
        BindingKey key = new BindingKey(level.dimension(), target.getUUID(), chain.getUUID());
        Binding existing = BINDINGS.get(key);
        if (existing != null) {
            if (minimumBoundUntil > existing.minimumBoundUntil()) {
                BINDINGS.put(key, existing.withMinimumBoundUntil(minimumBoundUntil));
            }
            return false;
        }
        HeavenChainBindingEntity visual = ModEntities.HEAVEN_CHAIN_BINDING.get().create(level);
        UUID visualId = visual == null ? null : visual.getUUID();
        BINDINGS.put(key, new Binding(
            key,
            owner.getUUID(),
            pathAnchor,
            level.getGameTime(),
            minimumBoundUntil,
            visualId
        ));
        indexBinding(key);
        chain.addAnchor(pathAnchor);

        if (visual != null) {
            visual.initialize(chain, target, stackIndex, minimumBoundUntil);
            level.addFreshEntity(visual);
        }
        return true;
    }

    public static void ensureBinding(
        ServerLevel level,
        HeavenChainEntity chain,
        LivingEntity target,
        HeavenChainBindingEntity visual
    ) {
        BindingKey key = new BindingKey(level.dimension(), target.getUUID(), chain.getUUID());
        Binding existing = BINDINGS.get(key);
        if (existing != null) {
            if (existing.visualId() == null) {
                BINDINGS.put(key, existing.withVisual(visual.getUUID()));
            } else if (!existing.visualId().equals(visual.getUUID())) {
                visual.dissolve();
            }
            return;
        }

        TargetState state = TARGET_STATES.computeIfAbsent(target.getUUID(), ignored -> captureTargetState(target));
        Vec3 pathAnchor = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        BINDINGS.put(key, new Binding(
            key,
            chain.ownerUuid(),
            pathAnchor,
            level.getGameTime(),
            Math.max(level.getGameTime(), visual.minimumBoundUntil()),
            visual.getUUID()
        ));
        indexBinding(key);
        chain.addAnchor(pathAnchor);
        holdTarget(target, state);
    }

    public static void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        Set<UUID> maintainedTargets = new HashSet<>();
        for (Binding binding : new ArrayList<>(BINDINGS.values())) {
            if (!binding.key().dimension().equals(level.dimension()) || !BINDINGS.containsKey(binding.key())) {
                continue;
            }
            Entity targetEntity = level.getEntity(binding.key().targetId());
            Entity ownerEntity = binding.ownerId() == null ? null : level.getEntity(binding.ownerId());
            Entity chainEntity = level.getEntity(binding.key().chainId());
            if (!(targetEntity instanceof LivingEntity target) || !target.isAlive()
                || !(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()
                || !(chainEntity instanceof HeavenChainEntity chain) || !chain.isAlive()) {
                release(level, binding);
                continue;
            }
            TargetState state = TARGET_STATES.computeIfAbsent(target.getUUID(), ignored -> captureTargetState(target));
            if (gameTime >= binding.minimumBoundUntil()
                && owner.position().distanceToSqr(state.anchor()) > ChainConfig.MAX_EXTENSION_SQR) {
                chain.breakChain();
                continue;
            }
            if (!maintainedTargets.add(target.getUUID())) {
                continue;
            }
            holdTarget(target, state);
            prioritizeBindingTarget(level, target);
            if (gameTime >= state.nextDamageAt()) {
                state.setNextDamageAt(gameTime + ChainConfig.DAMAGE_INTERVAL_TICKS);
                target.hurt(damageSource(owner, target), ChainConfig.DAMAGE_PER_INTERVAL);
            }
            applyEffects(target);
        }
    }

    public static float scaleDamageFromBoundTarget(LivingEntity boundTarget, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker != boundTarget && direct != boundTarget) {
            return amount;
        }
        int divinity = TypeMoonBridge.divinityLevel(boundTarget);
        return TargetingMath.applyDivinityOutputSuppression(amount, divinity);
    }

    public static boolean hasBindings(UUID chainId) {
        Set<BindingKey> keys = BINDINGS_BY_CHAIN.get(chainId);
        return keys != null && !keys.isEmpty();
    }

    public static boolean hasProtectedBindings(UUID chainId, long gameTime) {
        Set<BindingKey> keys = BINDINGS_BY_CHAIN.get(chainId);
        if (keys == null) {
            return false;
        }
        return keys.stream()
            .map(BINDINGS::get)
            .filter(binding -> binding != null)
            .anyMatch(binding -> gameTime < binding.minimumBoundUntil());
    }

    public static long minimumBoundUntil(UUID chainId) {
        Set<BindingKey> keys = BINDINGS_BY_CHAIN.get(chainId);
        if (keys == null) {
            return 0L;
        }
        return keys.stream()
            .map(BINDINGS::get)
            .filter(binding -> binding != null)
            .mapToLong(Binding::minimumBoundUntil)
            .max()
            .orElse(0L);
    }

    public static boolean isBound(UUID targetId) {
        return BINDINGS.keySet().stream().anyMatch(key -> key.targetId().equals(targetId));
    }

    public static boolean isBoundByChain(UUID targetId, UUID chainId) {
        return BINDINGS.keySet().stream()
            .anyMatch(key -> key.targetId().equals(targetId) && key.chainId().equals(chainId));
    }

    public static void releaseByChain(ServerLevel level, UUID chainId) {
        Set<BindingKey> keys = BINDINGS_BY_CHAIN.get(chainId);
        if (keys == null) {
            return;
        }
        for (BindingKey key : new ArrayList<>(keys)) {
            Binding binding = BINDINGS.get(key);
            if (binding != null && binding.key().dimension().equals(level.dimension())) {
                release(level, binding);
            }
        }
    }

    public static void releaseByOwner(ServerLevel level, UUID ownerId) {
        for (Binding binding : new ArrayList<>(BINDINGS.values())) {
            if (binding.key().dimension().equals(level.dimension()) && ownerId.equals(binding.ownerId())) {
                release(level, binding);
            }
        }
    }

    public static void clearLevel(ServerLevel level) {
        for (Binding binding : new ArrayList<>(BINDINGS.values())) {
            if (binding.key().dimension().equals(level.dimension())) {
                release(level, binding);
            }
        }
    }

    private static void release(ServerLevel level, Binding binding) {
        if (!BINDINGS.remove(binding.key(), binding)) {
            return;
        }
        unindexBinding(binding.key());
        Entity chain = level.getEntity(binding.key().chainId());
        if (chain instanceof HeavenChainEntity heavenChain) {
            heavenChain.removeAnchor(binding.pathAnchor());
        }
        Entity visual = binding.visualId() == null ? null : level.getEntity(binding.visualId());
        if (visual instanceof HeavenChainBindingEntity bindingVisual) {
            bindingVisual.dissolve();
        }

        UUID targetId = binding.key().targetId();
        if (bindingsForTarget(level.dimension(), targetId).isEmpty()) {
            TargetState state = TARGET_STATES.remove(targetId);
            Entity target = level.getEntity(targetId);
            if (target instanceof LivingEntity living && state != null) {
                restoreTarget(level, living, state, binding.ownerId());
            }
        }
    }

    private static TargetState captureTargetState(LivingEntity target) {
        Boolean oldNoAi = target instanceof Mob mob ? mob.isNoAi() : null;
        UUID oldTarget = target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null;
        return new TargetState(target.position(), oldNoAi, oldTarget, target.level().getGameTime() + ChainConfig.DAMAGE_INTERVAL_TICKS);
    }

    private static void holdTarget(LivingEntity target, TargetState state) {
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            if (!Boolean.TRUE.equals(state.oldNoAi())) {
                mob.setNoAi(false);
            }
        }
        boolean wasPassenger = target.isPassenger();
        if (wasPassenger) {
            target.stopRiding();
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        Vec3 anchor = state.anchor();
        boolean positionDrifted = target.position().distanceToSqr(anchor) > POSITION_CORRECTION_EPSILON_SQR;
        if (target instanceof ServerPlayer player && (wasPassenger || positionDrifted)) {
            player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
        } else {
            target.setPos(anchor.x, anchor.y, anchor.z);
        }
        target.hurtMarked = true;
    }

    private static void prioritizeBindingTarget(ServerLevel level, LivingEntity target) {
        if (!(target instanceof Mob mob) || mob.isNoAi()) {
            return;
        }
        HeavenChainBindingEntity nearest = bindingsForTarget(level.dimension(), target.getUUID()).stream()
            .map(Binding::visualId)
            .filter(id -> id != null)
            .map(level::getEntity)
            .filter(HeavenChainBindingEntity.class::isInstance)
            .map(HeavenChainBindingEntity.class::cast)
            .filter(Entity::isAlive)
            .min(Comparator.comparingDouble(target::distanceToSqr))
            .orElse(null);
        if (nearest != null && mob.getTarget() != nearest) {
            mob.setTarget(nearest);
        }
    }

    private static void restoreTarget(
        ServerLevel level,
        LivingEntity target,
        TargetState state,
        @Nullable UUID releasingOwnerId
    ) {
        if (!(target instanceof Mob mob)) {
            return;
        }
        if (state.oldNoAi() != null) {
            mob.setNoAi(state.oldNoAi());
        }
        Entity releasingOwner = releasingOwnerId == null ? null : level.getEntity(releasingOwnerId);
        if (!mob.isNoAi() && releasingOwner instanceof LivingEntity owner && owner.isAlive()
            && owner != target && !target.isAlliedTo(owner)) {
            mob.setLastHurtByMob(owner);
            mob.setTarget(owner);
            return;
        }
        Entity oldTarget = state.oldTargetId() == null ? null : level.getEntity(state.oldTargetId());
        mob.setTarget(oldTarget instanceof LivingEntity living && living.isAlive() ? living : null);
    }

    private static List<Binding> bindingsForTarget(ResourceKey<Level> dimension, UUID targetId) {
        return BINDINGS.values().stream()
            .filter(binding -> binding.key().dimension().equals(dimension) && binding.key().targetId().equals(targetId))
            .toList();
    }

    private static void indexBinding(BindingKey key) {
        BINDINGS_BY_CHAIN.computeIfAbsent(key.chainId(), ignored -> new HashSet<>()).add(key);
    }

    private static void unindexBinding(BindingKey key) {
        Set<BindingKey> keys = BINDINGS_BY_CHAIN.get(key.chainId());
        if (keys == null) {
            return;
        }
        keys.remove(key);
        if (keys.isEmpty()) {
            BINDINGS_BY_CHAIN.remove(key.chainId());
        }
    }

    private static DamageSource damageSource(LivingEntity owner, LivingEntity target) {
        if (owner instanceof ServerPlayer player) {
            return target.damageSources().playerAttack(player);
        }
        if (owner instanceof Mob mob) {
            return target.damageSources().mobAttack(mob);
        }
        return target.damageSources().generic();
    }

    private static void applyEffects(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 255, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1, false, true));
    }

    private record BindingKey(ResourceKey<Level> dimension, UUID targetId, UUID chainId) {
    }

    private record Binding(
        BindingKey key,
        @Nullable UUID ownerId,
        Vec3 pathAnchor,
        long boundAt,
        long minimumBoundUntil,
        @Nullable UUID visualId
    ) {
        private Binding withVisual(UUID newVisualId) {
            return new Binding(key, ownerId, pathAnchor, boundAt, minimumBoundUntil, newVisualId);
        }

        private Binding withMinimumBoundUntil(long value) {
            return new Binding(key, ownerId, pathAnchor, boundAt, value, visualId);
        }
    }

    private static final class TargetState {
        private final Vec3 anchor;
        private final Boolean oldNoAi;
        private final UUID oldTargetId;
        private long nextDamageAt;

        private TargetState(Vec3 anchor, Boolean oldNoAi, UUID oldTargetId, long nextDamageAt) {
            this.anchor = anchor;
            this.oldNoAi = oldNoAi;
            this.oldTargetId = oldTargetId;
            this.nextDamageAt = nextDamageAt;
        }

        private Vec3 anchor() {
            return anchor;
        }

        private Boolean oldNoAi() {
            return oldNoAi;
        }

        private UUID oldTargetId() {
            return oldTargetId;
        }

        private long nextDamageAt() {
            return nextDamageAt;
        }

        private void setNextDamageAt(long value) {
            nextDamageAt = value;
        }
    }

    private BindingService() {
    }
}

