package com.example.typemoonaddon.andrephius;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.kimaris.KimarisService;
import com.example.typemoonaddon.magic.AndrephiusMagic;
import com.example.typemoonaddon.network.AndrephiusCastInputPayload;
import com.example.typemoonaddon.network.AddonSpellVisualPayload;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class AndrephiusService {
    public static final int MAX_CHARGE_TICKS = 200;
    public static final int MAX_FULL_CHARGE_HOLD_TICKS = 60;
    public static final double MAX_INJECTED_MANA = 600.0D;
    public static final double MANA_PER_TICK = 3.0D;
    public static final double MIN_EFFECTIVE_MANA = 40.0D;
    public static final double MIN_RADIUS = 8.0D;
    public static final double MAX_RADIUS = 50.0D;
    public static final float MIN_DAMAGE = 80.0F;
    public static final float MAX_DAMAGE = 600.0F;
    public static final int MAX_TARGETS = 64;

    public static final ResourceKey<DamageType> LIGHTNING_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "andrephius_lightning")
    );

    private static final int MANA_SYNC_INTERVAL_TICKS = 5;
    private static final int CHARGE_VFX_INTERVAL_TICKS = 10;
    private static final int LOCK_DELAY_TICKS = 8;
    private static final int STRIKE_BATCH_INTERVAL_TICKS = 2;
    private static final int STRIKES_PER_BATCH = 12;
    private static final int STRIKES_PER_TARGET = 8;
    // Eight independently damaging bolts retain the old per-bolt damage,
    // so the total damage is twice the previous four-bolt version.
    private static final float DAMAGE_PER_STRIKE_FRACTION = 2.0F / STRIKES_PER_TARGET;
    private static final double VFX_OBSERVER_RADIUS = 224.0D;

    private static final String CHARGE_EFFECT = "typemoonworld:andrephius_charge";
    private static final String SIGILLUM_CHARGE_EFFECT = "typemoonworld:andrephius_sigillum_charge";
    private static final String SIGILLUM_RELEASE_EFFECT = "typemoonworld:andrephius_sigillum_release";
    private static final String LOCK_EFFECT = "typemoonworld:andrephius_lock";
    private static final String TARGET_CLOUD_EFFECT = "typemoonworld:andrephius_target_cloud";
    private static final String STORM_EFFECT = "typemoonworld:andrephius_storm";
    private static final String STRIKE_EFFECT = "typemoonworld:andrephius_strike";
    private static final String AFTERMATH_EFFECT = "typemoonworld:andrephius_aftermath";

    private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();
    private static final Map<UUID, StormTask> STORMS = new HashMap<>();

    private AndrephiusService() {
    }

    public static void handleCastInput(ServerPlayer player, byte action) {
        if (KimarisService.isFrozen(player)) {
            cancelCharge(player);
            return;
        }
        switch (action) {
            case AndrephiusCastInputPayload.PRESS -> beginCharge(player);
            case AndrephiusCastInputPayload.RELEASE -> releaseCharge(player);
            case AndrephiusCastInputPayload.CANCEL -> cancelCharge(player);
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

        if (state.injectedMana >= MAX_INJECTED_MANA
                || state.chargedTicks >= MAX_CHARGE_TICKS) {
            state.injectedMana = MAX_INJECTED_MANA;
            state.fullChargeHoldTicks++;
            if (state.fullChargeHoldTicks % CHARGE_VFX_INTERVAL_TICKS == 0) {
                sendChargeVfx(player, state);
            }
            if (state.fullChargeHoldTicks >= MAX_FULL_CHARGE_HOLD_TICKS) {
                CHARGES.remove(player.getUUID());
                syncManaIfDirty(player, state);
                releaseStorm(player, state);
            }
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double rawMana = vars.player_mana;
        double currentMana = sanitizeMana(vars);
        if (!Double.isFinite(rawMana) || rawMana < 0.0D) {
            state.manaDirty = true;
        }
        double remaining = Math.max(0.0D, MAX_INJECTED_MANA - state.injectedMana);
        double expected = Math.min(MANA_PER_TICK, remaining);
        double injected = Math.min(expected, currentMana);
        if (injected > 0.0D) {
            vars.player_mana = Math.max(0.0D, currentMana - injected);
            state.injectedMana = Mth.clamp(
                    state.injectedMana + injected, 0.0D, MAX_INJECTED_MANA);
            state.chargedTicks++;
            state.manaDirty = true;
        }

        if (state.chargedTicks == 1 || state.chargedTicks % CHARGE_VFX_INTERVAL_TICKS == 0) {
            sendChargeVfx(player, state);
        }

        boolean reachedMaximum = state.injectedMana >= MAX_INJECTED_MANA
                || state.chargedTicks >= MAX_CHARGE_TICKS;
        boolean manaDepleted = injected < expected || vars.player_mana <= 0.0D;
        if (state.chargedTicks % MANA_SYNC_INTERVAL_TICKS == 0
                || reachedMaximum
                || manaDepleted) {
            syncManaIfDirty(player, state);
        }

        if (reachedMaximum && !state.maxChargeNotified) {
            state.injectedMana = MAX_INJECTED_MANA;
            state.maxChargeNotified = true;
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.andrephius.max_charge"), true);
        }

        if (manaDepleted && !reachedMaximum) {
            CHARGES.remove(player.getUUID());
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.andrephius.mana_depleted",
                    formatMana(state.injectedMana)), true);
            finishOrCancel(player, state);
        }
    }

    public static void tickStorms(MinecraftServer server) {
        Iterator<Map.Entry<UUID, StormTask>> iterator = STORMS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().tick(server)) {
                iterator.remove();
            }
        }
    }

    public static void stop(Player player, boolean cancelStorm) {
        if (player == null) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            cancelCharge(serverPlayer);
        } else {
            CHARGES.remove(player.getUUID());
        }
        if (cancelStorm) {
            STORMS.remove(player.getUUID());
            if (player instanceof ServerPlayer serverPlayer) {
                AddonSpellVisualPayload.clear(
                        serverPlayer, AddonSpellVisualPayload.ANDREPHIUS,
                        VFX_OBSERVER_RADIUS);
            }
        }
    }

    public static void clearAll(MinecraftServer server) {
        for (Map.Entry<UUID, ChargeState> entry : CHARGES.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                syncManaIfDirty(player, entry.getValue());
            }
        }
        CHARGES.clear();
        STORMS.clear();
    }

    private static void beginCharge(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (CHARGES.containsKey(playerId)
                || STORMS.containsKey(playerId)
                || !isValidCaster(player)) {
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (sanitizeMana(vars) <= 0.0D) {
            vars.syncMana(player);
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.andrephius.insufficient_mana"), true);
            return;
        }

        CHARGES.put(playerId, new ChargeState(player.serverLevel().dimension()));
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.andrephius.charging"), true);
    }

    private static void releaseCharge(ServerPlayer player) {
        ChargeState state = CHARGES.remove(player.getUUID());
        if (state == null) {
            return;
        }
        syncManaIfDirty(player, state);
        if (!isValidCaster(player) || !state.dimension.equals(player.serverLevel().dimension())) {
            return;
        }
        finishOrCancel(player, state);
    }

    private static void finishOrCancel(ServerPlayer player, ChargeState state) {
        if (state.injectedMana < MIN_EFFECTIVE_MANA) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.andrephius.undercharged",
                    formatMana(state.injectedMana)), true);
            return;
        }
        releaseStorm(player, state);
    }

    private static void cancelCharge(ServerPlayer player) {
        ChargeState state = CHARGES.remove(player.getUUID());
        if (state != null) {
            syncManaIfDirty(player, state);
            AddonSpellVisualPayload.clear(
                    player, AddonSpellVisualPayload.ANDREPHIUS, VFX_OBSERVER_RADIUS);
        }
    }

    private static void releaseStorm(ServerPlayer caster, ChargeState chargeState) {
        if (STORMS.containsKey(caster.getUUID()) || !caster.isAlive() || caster.isRemoved()) {
            return;
        }

        ServerLevel level = caster.serverLevel();
        AddonSpellVisualPayload.clear(
                caster, AddonSpellVisualPayload.ANDREPHIUS, VFX_OBSERVER_RADIUS);
        StormParameters parameters = StormParameters.fromMana(chargeState.injectedMana);
        Vec3 center = caster.getBoundingBox().getCenter();
        double radiusSquared = parameters.radius * parameters.radius;
        AABB searchArea = new AABB(center, center).inflate(parameters.radius);
        List<LockedTarget> targets = new ArrayList<>();
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                entity -> EntityUtils.isValidCombatTarget(caster, entity))) {
            Vec3 lockedPosition = target.getBoundingBox().getCenter();
            double distanceSquared = lockedPosition.distanceToSqr(center);
            if (distanceSquared <= radiusSquared) {
                targets.add(new LockedTarget(target.getUUID(), lockedPosition, distanceSquared));
            }
        }
        targets.sort(Comparator
                .comparingDouble(LockedTarget::distanceSquared)
                .thenComparing(target -> target.targetId().toString()));
        if (targets.size() > MAX_TARGETS) {
            targets = new ArrayList<>(targets.subList(0, MAX_TARGETS));
        }

        VFXServerEffects.spawn(level, STORM_EFFECT, center, VFX_OBSERVER_RADIUS);
        VFXServerEffects.spawn(level, SIGILLUM_RELEASE_EFFECT, caster, VFX_OBSERVER_RADIUS);
        AddonSpellVisualPayload.showAttached(
                level, AddonSpellVisualPayload.ANDREPHIUS,
                AddonSpellVisualPayload.SIGIL, caster.getUUID(), caster,
                (float) parameters.strength, 1.0F, 30, 1, VFX_OBSERVER_RADIUS);
        AddonSpellVisualPayload.showAt(
                level, AddonSpellVisualPayload.ANDREPHIUS,
                AddonSpellVisualPayload.LIGHTNING_STORM, caster.getUUID(), center,
                (float) Math.min(28.0D, parameters.radius), 22.0F,
                140, 0, VFX_OBSERVER_RADIUS);
        for (LockedTarget target : targets) {
            VFXServerEffects.spawn(level, LOCK_EFFECT, target.lockedPosition(), VFX_OBSERVER_RADIUS);
            Entity targetEntity = level.getEntity(target.targetId());
            if (targetEntity instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                VFXServerEffects.spawn(level, TARGET_CLOUD_EFFECT, livingTarget, VFX_OBSERVER_RADIUS);
                AddonSpellVisualPayload.showAttached(
                        level, AddonSpellVisualPayload.ANDREPHIUS,
                        AddonSpellVisualPayload.LIGHTNING_STORM, caster.getUUID(), livingTarget,
                        5.5F, 13.0F, 140, livingTarget.getId(), VFX_OBSERVER_RADIUS);
            }
        }
        caster.displayClientMessage(Component.translatable(
                "message.typemoonworld.andrephius.locked",
                targets.size(), formatMana(parameters.radius)), true);

        if (targets.isEmpty()) {
            VFXServerEffects.spawn(level, AFTERMATH_EFFECT, center, VFX_OBSERVER_RADIUS);
            AddonSpellVisualPayload.showAt(
                    level, AddonSpellVisualPayload.ANDREPHIUS,
                    AddonSpellVisualPayload.AFTERMATH, caster.getUUID(), center,
                    12.0F, 1.0F, 32, 0, VFX_OBSERVER_RADIUS);
            caster.displayClientMessage(Component.translatable(
                    "message.typemoonworld.andrephius.released", 0), true);
            return;
        }

        STORMS.put(caster.getUUID(), new StormTask(
                caster.getUUID(),
                level.dimension(),
                center,
                parameters,
                targets,
                level.getGameTime() + LOCK_DELAY_TICKS
        ));
    }

    private static void sendChargeVfx(ServerPlayer player, ChargeState state) {
        double charge = Mth.clamp(state.injectedMana / MAX_INJECTED_MANA, 0.0D, 1.0D);
        int pulses = Mth.clamp(1 + Mth.floor(charge * 2.0D), 1, 3);
        for (int pulse = 0; pulse < pulses; pulse++) {
            VFXServerEffects.spawn(
                    player.serverLevel(), CHARGE_EFFECT, player, VFX_OBSERVER_RADIUS);
        }
        VFXServerEffects.spawn(
                player.serverLevel(), SIGILLUM_CHARGE_EFFECT, player, VFX_OBSERVER_RADIUS);
        AddonSpellVisualPayload.showAttached(
                player.serverLevel(), AddonSpellVisualPayload.ANDREPHIUS,
                AddonSpellVisualPayload.SIGIL, player.getUUID(), player,
                (float) charge, 1.0F, CHARGE_VFX_INTERVAL_TICKS + 7,
                0, VFX_OBSERVER_RADIUS);
        player.serverLevel().sendParticles(
                ModParticles.ELEMENTAL_LIGHTNING.get(),
                player.getX(),
                player.getY() + player.getBbHeight() + 3.0D,
                player.getZ(),
                Mth.clamp(8 + Mth.floor(charge * 24.0D), 8, 32),
                Mth.lerp(charge, 1.2D, 4.5D),
                1.2D,
                Mth.lerp(charge, 1.2D, 4.5D),
                0.05D
        );
    }

    private static boolean isValidCaster(ServerPlayer player) {
        if (player == null
                || !player.isAlive()
                || player.isRemoved()
                || player.isSpectator()
                || player.hasDisconnected()
                || player.containerMenu != player.inventoryMenu
                || KimarisService.isFrozen(player)) {
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
                && PlayerMagicSelectionService.isCurrentSelection(
                        vars, AndrephiusMagic.MAGIC_ID);
    }

    private static double sanitizeMana(TypeMoonWorldModVariables.PlayerVariables vars) {
        double current = vars.player_mana;
        if (!Double.isFinite(current) || current < 0.0D) {
            vars.player_mana = 0.0D;
            return 0.0D;
        }
        return current;
    }

    private static void syncManaIfDirty(ServerPlayer player, ChargeState state) {
        if (!state.manaDirty) {
            return;
        }
        player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).syncMana(player);
        state.manaDirty = false;
    }

    private static String formatMana(double value) {
        double safe = Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
        long rounded = Math.round(safe);
        return Math.abs(safe - rounded) < 0.000001D
                ? Long.toString(rounded)
                : String.format(java.util.Locale.ROOT, "%.2f", safe);
    }

    private static final class ChargeState {
        private final ResourceKey<Level> dimension;
        private int chargedTicks;
        private int fullChargeHoldTicks;
        private double injectedMana;
        private boolean manaDirty;
        private boolean maxChargeNotified;

        private ChargeState(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }
    }

    private record LockedTarget(UUID targetId, Vec3 lockedPosition, double distanceSquared) {
    }

    private record StormParameters(double strength, double radius, float damage) {
        private static StormParameters fromMana(double mana) {
            double charge = Mth.clamp(mana / MAX_INJECTED_MANA, 0.0D, 1.0D);
            double strength = charge * charge * (3.0D - 2.0D * charge);
            return new StormParameters(
                    strength,
                    Mth.clamp(Mth.lerp(strength, MIN_RADIUS, MAX_RADIUS), MIN_RADIUS, MAX_RADIUS),
                    Mth.clamp(Mth.lerp((float) strength, MIN_DAMAGE, MAX_DAMAGE),
                            MIN_DAMAGE, MAX_DAMAGE)
            );
        }

        private float damageAt(double distanceSquared) {
            double distance = Math.sqrt(Math.max(0.0D, distanceSquared));
            double normalized = Mth.clamp(distance / Math.max(0.001D, radius), 0.0D, 1.0D);
            double multiplier = normalized <= 0.2D
                    ? 1.0D
                    : Mth.lerp((normalized - 0.2D) / 0.8D, 1.0D, 0.2D);
            return (float) Mth.clamp(damage * multiplier, 0.0D, MAX_DAMAGE);
        }
    }

    private static final class StormTask {
        private final UUID casterId;
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final StormParameters parameters;
        private final ArrayDeque<PendingStrike> pendingStrikes;
        private final Set<StrikeKey> processedStrikes = new HashSet<>();
        private final Set<UUID> successfullyStruckTargets = new HashSet<>();
        private long nextStrikeTick;

        private StormTask(
                UUID casterId,
                ResourceKey<Level> dimension,
                Vec3 center,
                StormParameters parameters,
                List<LockedTarget> targets,
                long nextStrikeTick
        ) {
            this.casterId = casterId;
            this.dimension = dimension;
            this.center = center;
            this.parameters = parameters;
            this.pendingStrikes = new ArrayDeque<>();
            for (int strikeIndex = 0; strikeIndex < STRIKES_PER_TARGET; strikeIndex++) {
                for (LockedTarget target : targets) {
                    this.pendingStrikes.addLast(new PendingStrike(target, strikeIndex));
                }
            }
            this.nextStrikeTick = nextStrikeTick;
        }

        private boolean tick(MinecraftServer server) {
            ServerLevel level = server.getLevel(dimension);
            ServerPlayer caster = server.getPlayerList().getPlayer(casterId);
            if (level == null
                    || caster == null
                    || !caster.isAlive()
                    || caster.isRemoved()
                    || caster.hasDisconnected()
                    || caster.serverLevel() != level) {
                return true;
            }

            long now = level.getGameTime();
            if (now < nextStrikeTick) {
                return false;
            }

            int processed = 0;
            while (processed < STRIKES_PER_BATCH && !pendingStrikes.isEmpty()) {
                PendingStrike pendingStrike = pendingStrikes.removeFirst();
                StrikeKey key = new StrikeKey(
                        pendingStrike.target().targetId(), pendingStrike.strikeIndex());
                if (processedStrikes.add(key)
                        && strike(caster, level, pendingStrike, parameters)) {
                    successfullyStruckTargets.add(pendingStrike.target().targetId());
                }
                processed++;
            }

            if (pendingStrikes.isEmpty()) {
                VFXServerEffects.spawn(level, AFTERMATH_EFFECT, center, VFX_OBSERVER_RADIUS);
                AddonSpellVisualPayload.showAt(
                        level, AddonSpellVisualPayload.ANDREPHIUS,
                        AddonSpellVisualPayload.AFTERMATH, casterId, center,
                        (float) parameters.radius, 1.0F, 36, 0, VFX_OBSERVER_RADIUS);
                caster.displayClientMessage(Component.translatable(
                        "message.typemoonworld.andrephius.released",
                        successfullyStruckTargets.size()), true);
                return true;
            }
            nextStrikeTick = now + STRIKE_BATCH_INTERVAL_TICKS;
            return false;
        }

        private static boolean strike(
                ServerPlayer caster,
                ServerLevel level,
                PendingStrike pendingStrike,
                StormParameters parameters
        ) {
            LockedTarget lockedTarget = pendingStrike.target();
            Entity entity = level.getEntity(lockedTarget.targetId());
            if (!(entity instanceof LivingEntity target)
                    || !target.isAlive()
                    || target.isRemoved()
                    || !EntityUtils.isValidCombatTarget(caster, target)) {
                return false;
            }

            Vec3 strikePosition = target.position();
            VFXServerEffects.spawn(level, STRIKE_EFFECT, strikePosition, VFX_OBSERVER_RADIUS);
            AddonSpellVisualPayload.showBetween(
                    level, AddonSpellVisualPayload.ANDREPHIUS,
                    AddonSpellVisualPayload.LIGHTNING_STRIKE, caster.getUUID(),
                    strikePosition, strikePosition.add(0.0D, 26.0D, 0.0D),
                    0.72F, 26.0F, 12,
                    target.getId() * STRIKES_PER_TARGET + pendingStrike.strikeIndex(),
                    VFX_OBSERVER_RADIUS);
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(strikePosition.x, strikePosition.y, strikePosition.z);
                lightning.setVisualOnly(true);
                level.addFreshEntity(lightning);
            }
            // Keep the vanilla lightning bolt and layer the custom sprite around its impact.
            level.sendParticles(
                    ModParticles.ELEMENTAL_LIGHTNING.get(),
                    strikePosition.x,
                    strikePosition.y + 1.0D,
                    strikePosition.z,
                    18,
                    0.55D,
                    1.35D,
                    0.55D,
                    0.035D
            );

            float damage = Mth.clamp(
                    parameters.damageAt(lockedTarget.distanceSquared()) * DAMAGE_PER_STRIKE_FRACTION,
                    0.0F,
                    MAX_DAMAGE
            );
            damage = MagicResistanceHelper.applyNoblePhantasmMagicResistance(target, damage);
            damage = HeraclesGodHandHelper.applyAntiHeraclesNoblePhantasmSpecialAttack(
                    target, damage);
            if (!Float.isFinite(damage) || damage <= 0.0F) {
                return false;
            }

            DamageSource source = caster.damageSources().source(LIGHTNING_DAMAGE, caster);
            target.invulnerableTime = 0;
            boolean hurt = target.hurt(source, damage);
            target.invulnerableTime = 0;
            if (hurt) {
                EntityUtils.triggerSwarmAnger(level, caster, target);
            }
            return hurt;
        }
    }

    private record PendingStrike(LockedTarget target, int strikeIndex) {
    }

    private record StrikeKey(UUID targetId, int strikeIndex) {
    }
}
