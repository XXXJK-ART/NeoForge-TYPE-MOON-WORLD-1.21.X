package com.example.typemoonaddon.airflow_blade;

import com.example.typemoonaddon.entity.AirflowBladeEntity;
import com.example.typemoonaddon.kimaris.KimarisService;
import com.example.typemoonaddon.magic.AirflowBladeMagic;
import com.example.typemoonaddon.network.AirflowBladeCastInputPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/** Server state, collision and damage implementation for Airflow Blade. */
public final class AirflowBladeService {
    public static final int MODE_BLADE = AirflowBladeEntity.BLADE_MODE;
    public static final int MODE_CANNON = AirflowBladeEntity.CANNON_MODE;

    public static final int MIN_CHARGE_TICKS = 8;
    public static final int MAX_CHARGE_TICKS = 40;
    public static final double BLADE_SPEED = 2.6D;
    public static final double BLADE_MAX_DISTANCE = 48.0D;
    public static final float BLADE_DAMAGE = 35.0F;
    public static final double CANNON_SPEED = 1.45D;
    public static final double CANNON_MAX_DISTANCE = 64.0D;
    public static final float CANNON_MIN_DAMAGE = 60.0F;
    public static final float CANNON_MAX_DAMAGE = 150.0F;
    public static final double CANNON_MIN_RADIUS = 4.0D;
    public static final double CANNON_MAX_RADIUS = 5.5D;
    public static final double BLADE_MANA_COST = 20.0D;
    public static final double CANNON_MIN_MANA_COST = 40.0D;
    public static final double CANNON_MAX_MANA_COST = 100.0D;
    public static final int BLADE_COOLDOWN_TICKS = 4;
    public static final int CANNON_COOLDOWN_TICKS = 20;

    private static final int CHARGE_VFX_INTERVAL_TICKS = 5;
    private static final int TRAIL_VFX_INTERVAL_TICKS = 2;
    private static final double BLADE_HIT_RADIUS = 0.65D;
    private static final double CANNON_HIT_RADIUS = 0.7D;
    private static final int MAX_EXPLOSION_TARGETS = 64;
    private static final double VFX_OBSERVER_RADIUS = 128.0D;
    private static final String CHARGE_EFFECT = "typemoonworld:airflow_blade_charge";
    private static final String BLADE_TRAIL_EFFECT = "typemoonworld:airflow_blade_trail";
    private static final String CANNON_TRAIL_EFFECT = "typemoonworld:airflow_cannon_trail";
    private static final String HIT_EFFECT = "typemoonworld:airflow_blade_hit";
    private static final String BURST_EFFECT = "typemoonworld:airflow_blade_burst";

    private static final Map<UUID, Integer> MODES = new HashMap<>();
    private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();
    private static final Map<UUID, AirflowBladeEntity> PROJECTILES = new HashMap<>();
    private static final Map<UUID, Long> LAST_MODE_SWITCH = new HashMap<>();

    private AirflowBladeService() {
    }

    public static void handleCastInput(ServerPlayer player, byte action) {
        if (player == null || KimarisService.isFrozen(player)) {
            if (player != null) {
                cancelCharge(player, false);
            }
            return;
        }
        switch (action) {
            case AirflowBladeCastInputPayload.PRESS -> {
                if (mode(player) == MODE_BLADE) {
                    castBlade(player);
                } else {
                    beginCharge(player);
                }
            }
            case AirflowBladeCastInputPayload.RELEASE -> releaseCharge(player);
            case AirflowBladeCastInputPayload.CANCEL -> cancelCharge(player, true);
            default -> {
            }
        }
    }

    public static void switchMode(ServerPlayer player) {
        if (!isValidCaster(player, false)) {
            cancelCharge(player, false);
            return;
        }
        long now = player.serverLevel().getGameTime();
        Long last = LAST_MODE_SWITCH.get(player.getUUID());
        if (last != null && now - last < 4L) {
            return;
        }
        LAST_MODE_SWITCH.put(player.getUUID(), now);
        cancelCharge(player, false);
        int next = mode(player) == MODE_BLADE ? MODE_CANNON : MODE_BLADE;
        MODES.put(player.getUUID(), next);
        player.displayClientMessage(Component.translatable(next == MODE_BLADE
                ? "message.typemoonworld.airflow_blade.mode.blade"
                : "message.typemoonworld.airflow_blade.mode.cannon"), true);
    }

    public static void tickCharge(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!isValidCaster(player, false)
                || mode(player) != MODE_CANNON
                || !state.dimension.equals(player.serverLevel().dimension())) {
            cancelCharge(player, false);
            return;
        }
        long elapsed = Math.max(0L, player.serverLevel().getGameTime() - state.startTick);
        state.chargedTicks = (int) Math.min(MAX_CHARGE_TICKS, elapsed);
        if (state.chargedTicks == 1 || state.chargedTicks % CHARGE_VFX_INTERVAL_TICKS == 0) {
            float charge = chargePower(state.chargedTicks);
            Vec3 chargeCenter = player.getEyePosition(1.0F)
                    .add(state.direction.scale(1.15D))
                    .add(0.0D, -0.42D, 0.0D);
            VFXServerEffects.spawnOriented(
                    player.serverLevel(), CHARGE_EFFECT, chargeCenter,
                    state.direction, VFX_OBSERVER_RADIUS);
            if (charge >= 0.999F && !state.maxNotified) {
                state.maxNotified = true;
                player.displayClientMessage(Component.translatable(
                        "message.typemoonworld.airflow_blade.max_charge"), true);
            }
        }
    }

    public static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, AirflowBladeEntity> entry
                : new ArrayList<>(PROJECTILES.entrySet())) {
            AirflowBladeEntity projectile = entry.getValue();
            if (projectile == null || projectile.isRemoved()
                    || !(projectile.level() instanceof ServerLevel level)) {
                PROJECTILES.remove(entry.getKey(), projectile);
                continue;
            }
            ServerPlayer owner = projectile.ownerId()
                    .map(id -> server.getPlayerList().getPlayer(id)).orElse(null);
            if (!isActiveOwner(owner, level)) {
                projectile.discard();
                PROJECTILES.remove(entry.getKey(), projectile);
                continue;
            }
            if (tickProjectile(owner, projectile, level)) {
                PROJECTILES.remove(entry.getKey(), projectile);
            }
        }
    }

    public static void stop(Entity player) {
        if (player == null) {
            return;
        }
        cancelCharge(player instanceof ServerPlayer serverPlayer ? serverPlayer : null, false);
        removeProjectiles(player.getUUID());
        MODES.remove(player.getUUID());
        LAST_MODE_SWITCH.remove(player.getUUID());
    }

    public static void clearAll() {
        List<AirflowBladeEntity> projectiles = new ArrayList<>(PROJECTILES.values());
        PROJECTILES.clear();
        for (AirflowBladeEntity projectile : projectiles) {
            if (projectile != null && !projectile.isRemoved()) {
                projectile.discard();
            }
        }
        CHARGES.clear();
        MODES.clear();
        LAST_MODE_SWITCH.clear();
    }

    public static void handleEntityLeave(Entity entity) {
        if (entity instanceof AirflowBladeEntity projectile) {
            PROJECTILES.remove(projectile.getUUID());
        }
    }

    public static void handleChunkUnload(ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        for (Map.Entry<UUID, AirflowBladeEntity> entry
                : new ArrayList<>(PROJECTILES.entrySet())) {
            AirflowBladeEntity projectile = entry.getValue();
            if (projectile != null && projectile.level() == level
                    && new net.minecraft.world.level.ChunkPos(projectile.blockPosition()).equals(chunk)) {
                PROJECTILES.remove(entry.getKey(), projectile);
                projectile.discard();
            }
        }
    }

    private static void beginCharge(ServerPlayer player) {
        if (CHARGES.containsKey(player.getUUID()) || !isValidCaster(player, true)) {
            return;
        }
        if (mana(player) < CANNON_MIN_MANA_COST) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.airflow_blade.insufficient_mana"), true);
            return;
        }
        Vec3 direction = lookDirection(player);
        CHARGES.put(player.getUUID(), new ChargeState(
                player.serverLevel().dimension(), player.serverLevel().getGameTime(), direction));
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.airflow_blade.charging"), true);
    }

    private static void releaseCharge(ServerPlayer player) {
        ChargeState state = CHARGES.remove(player.getUUID());
        if (state == null) {
            return;
        }
        if (!isValidCaster(player, true)
                || !state.dimension.equals(player.serverLevel().dimension())) {
            return;
        }
        long elapsed = Math.max(0L, player.serverLevel().getGameTime() - state.startTick);
        int ticks = (int) Math.min(MAX_CHARGE_TICKS, elapsed);
        if (ticks < MIN_CHARGE_TICKS) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.airflow_blade.undercharged"), true);
            return;
        }
        float charge = chargePower(ticks);
        double manaCost = Mth.lerp(charge, CANNON_MIN_MANA_COST, CANNON_MAX_MANA_COST);
        if (!consumeMana(player, manaCost)) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.airflow_blade.insufficient_mana"), true);
            return;
        }
        setCooldown(player, CANNON_COOLDOWN_TICKS);
        spawnProjectile(player, MODE_CANNON, state.direction, charge);
    }

    private static void cancelCharge(ServerPlayer player, boolean notify) {
        if (player == null) {
            return;
        }
        if (CHARGES.remove(player.getUUID()) != null && notify) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.airflow_blade.cancelled"), true);
        }
    }

    private static void castBlade(ServerPlayer player) {
        if (!isValidCaster(player, true) || !consumeMana(player, BLADE_MANA_COST)) {
            if (isValidCaster(player, false)) {
                player.displayClientMessage(Component.translatable(
                        "message.typemoonworld.airflow_blade.insufficient_mana"), true);
            }
            return;
        }
        setCooldown(player, BLADE_COOLDOWN_TICKS);
        spawnProjectile(player, MODE_BLADE, lookDirection(player), 0.0F);
    }

    private static void spawnProjectile(ServerPlayer owner, int mode, Vec3 direction, float charge) {
        ServerLevel level = owner.serverLevel();
        AirflowBladeEntity projectile = new AirflowBladeEntity(level);
        Vec3 start = owner.getEyePosition(1.0F).add(direction.scale(0.65D));
        projectile.setPos(start.x, start.y, start.z);
        float damage = mode == MODE_BLADE
                ? BLADE_DAMAGE
                : Mth.lerp(charge, CANNON_MIN_DAMAGE, CANNON_MAX_DAMAGE);
        double radius = mode == MODE_BLADE
                ? 0.0D
                : Mth.lerp(charge, CANNON_MIN_RADIUS, CANNON_MAX_RADIUS);
        projectile.configure(
                mode,
                owner.getUUID(),
                direction,
                mode == MODE_BLADE ? BLADE_SPEED : CANNON_SPEED,
                mode == MODE_BLADE ? BLADE_MAX_DISTANCE : CANNON_MAX_DISTANCE,
                damage,
                radius,
                charge);
        if (level.addFreshEntity(projectile)) {
            PROJECTILES.put(projectile.getUUID(), projectile);
            VFXServerEffects.spawnOriented(
                    level,
                    mode == MODE_BLADE ? BLADE_TRAIL_EFFECT : CANNON_TRAIL_EFFECT,
                    start,
                    direction,
                    VFX_OBSERVER_RADIUS);
        }
    }

    private static boolean tickProjectile(
            ServerPlayer owner,
            AirflowBladeEntity projectile,
            ServerLevel level
    ) {
        Vec3 direction = projectile.direction();
        if (direction.lengthSqr() < 1.0E-8D) {
            projectile.discard();
            return true;
        }
        Vec3 current = projectile.position();
        Vec3 next = current.add(direction.scale(projectile.speed()));
        BlockHitResult blockHit = level.clip(new ClipContext(
                current, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.POSITIVE_INFINITY
                : current.distanceTo(blockHit.getLocation());

        double hitRadius = projectile.mode() == MODE_BLADE
                ? BLADE_HIT_RADIUS : CANNON_HIT_RADIUS;
        AABB sweep = projectile.getBoundingBox()
                .expandTowards(next.subtract(current)).inflate(hitRadius);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                sweep,
                target -> EntityUtils.isValidCombatTarget(owner, target));
        targets.sort(Comparator.comparingDouble(target ->
                current.distanceToSqr(target.getBoundingBox().getCenter())));

        LivingEntity entityHit = targets.isEmpty() ? null : targets.get(0);
        if (entityHit != null
                && current.distanceTo(entityHit.getBoundingBox().getCenter()) <= blockDistance + hitRadius) {
            Vec3 impact = entityHit.getBoundingBox().getCenter();
            projectile.setPos(impact.x, impact.y, impact.z);
            if (projectile.mode() == MODE_CANNON) {
                explode(owner, level, impact, projectile);
            } else {
                damageSingle(owner, entityHit, projectile.damage());
                VFXServerEffects.spawn(level, HIT_EFFECT, impact, VFX_OBSERVER_RADIUS);
                level.sendParticles(ParticleTypes.GUST, impact.x, impact.y, impact.z,
                        5, 0.25D, 0.25D, 0.25D, 0.12D);
            }
            projectile.discard();
            return true;
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            Vec3 impact = blockHit.getLocation();
            projectile.setPos(impact.x, impact.y, impact.z);
            if (projectile.mode() == MODE_CANNON) {
                explode(owner, level, impact, projectile);
            } else {
                VFXServerEffects.spawn(level, HIT_EFFECT, impact, VFX_OBSERVER_RADIUS);
            }
            projectile.discard();
            return true;
        }

        projectile.setPos(next.x, next.y, next.z);
        projectile.addTravelled(current.distanceTo(next));
        if (projectile.tickCount % TRAIL_VFX_INTERVAL_TICKS == 0) {
            String trailEffect = projectile.mode() == MODE_BLADE
                    ? BLADE_TRAIL_EFFECT : CANNON_TRAIL_EFFECT;
            VFXServerEffects.spawnOriented(level, trailEffect,
                    projectile.position(), direction, VFX_OBSERVER_RADIUS);
        }
        if (projectile.travelled() >= projectile.maxDistance()) {
            if (projectile.mode() == MODE_CANNON) {
                explode(owner, level, projectile.position(), projectile);
            }
            projectile.discard();
            return true;
        }
        return false;
    }

    private static void explode(
            ServerPlayer owner,
            ServerLevel level,
            Vec3 center,
            AirflowBladeEntity projectile
    ) {
        double radius = projectile.explosionRadius();
        VFXServerEffects.spawn(level, BURST_EFFECT, center, VFX_OBSERVER_RADIUS);
        level.sendParticles(ParticleTypes.GUST, center.x, center.y, center.z,
                18, radius * 0.35D, radius * 0.35D, radius * 0.35D, 0.16D);
        AABB box = new AABB(center, center).inflate(radius);
        Set<UUID> damaged = new HashSet<>();
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                target -> EntityUtils.isValidCombatTarget(owner, target)
                        && center.distanceToSqr(target.getBoundingBox().getCenter()) <= radius * radius);
        targets.sort(Comparator.comparingDouble(target ->
                center.distanceToSqr(target.getBoundingBox().getCenter())));
        DamageSource source = owner.damageSources().source(DamageTypes.MAGIC, owner);
        int limit = Math.min(MAX_EXPLOSION_TARGETS, targets.size());
        for (int index = 0; index < limit; index++) {
            LivingEntity target = targets.get(index);
            if (!damaged.add(target.getUUID())) {
                continue;
            }
            int previousInvulnerability = target.invulnerableTime;
            target.invulnerableTime = 0;
            if (!target.hurt(source, projectile.damage())) {
                target.invulnerableTime = previousInvulnerability;
            }
            EntityUtils.triggerSwarmAnger(level, owner, target);
        }
    }

    private static void damageSingle(ServerPlayer owner, LivingEntity target, float damage) {
        DamageSource source = owner.damageSources().source(DamageTypes.MAGIC, owner);
        int previousInvulnerability = target.invulnerableTime;
        target.invulnerableTime = 0;
        if (!target.hurt(source, damage)) {
            target.invulnerableTime = previousInvulnerability;
        }
        EntityUtils.triggerSwarmAnger(owner.serverLevel(), owner, target);
    }

    private static boolean isValidCaster(ServerPlayer player, boolean requireCooldown) {
        if (player == null || !player.isAlive() || player.isRemoved() || player.isSpectator()
                || KimarisService.isFrozen(player)) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        return vars.is_magus
                && vars.is_magic_circuit_open
                && PlayerMagicSelectionService.isCurrentSelection(vars, AirflowBladeMagic.MAGIC_ID)
                && (!requireCooldown || (Double.isFinite(vars.magic_cooldown)
                && vars.magic_cooldown <= 0.0D));
    }

    private static boolean isActiveOwner(ServerPlayer owner, ServerLevel level) {
        return owner != null && isValidCaster(owner, false)
                && owner.serverLevel() == level;
    }

    private static Vec3 lookDirection(ServerPlayer player) {
        Vec3 direction = player.getViewVector(1.0F);
        return direction.lengthSqr() > 1.0E-8D ? direction.normalize() : Vec3.ZERO;
    }

    private static int mode(ServerPlayer player) {
        return MODES.getOrDefault(player.getUUID(), MODE_BLADE);
    }

    private static double mana(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double value = vars.player_mana;
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static boolean consumeMana(ServerPlayer player, double amount) {
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double current = mana(player);
        if (!Double.isFinite(amount) || amount < 0.0D || current < amount) {
            vars.syncMana(player);
            return false;
        }
        vars.player_mana = Math.max(0.0D, current - amount);
        vars.syncMana(player);
        return true;
    }

    private static void setCooldown(ServerPlayer player, int ticks) {
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.magic_cooldown = Math.max(vars.magic_cooldown, ticks);
    }

    private static float chargePower(int chargedTicks) {
        double normalized = Mth.clamp(
                (chargedTicks - MIN_CHARGE_TICKS) / (double) (MAX_CHARGE_TICKS - MIN_CHARGE_TICKS),
                0.0D, 1.0D);
        return (float) (normalized * normalized * (3.0D - 2.0D * normalized));
    }

    private static void removeProjectiles(UUID ownerId) {
        for (Map.Entry<UUID, AirflowBladeEntity> entry
                : new ArrayList<>(PROJECTILES.entrySet())) {
            AirflowBladeEntity projectile = entry.getValue();
            if (projectile != null && projectile.ownerId().map(ownerId::equals).orElse(false)) {
                PROJECTILES.remove(entry.getKey(), projectile);
                projectile.discard();
            }
        }
    }

    private static final class ChargeState {
        private final ResourceKey<Level> dimension;
        private final long startTick;
        private final Vec3 direction;
        private int chargedTicks;
        private boolean maxNotified;

        private ChargeState(ResourceKey<Level> dimension, long startTick, Vec3 direction) {
            this.dimension = dimension;
            this.startTick = startTick;
            this.direction = direction;
        }
    }
}
