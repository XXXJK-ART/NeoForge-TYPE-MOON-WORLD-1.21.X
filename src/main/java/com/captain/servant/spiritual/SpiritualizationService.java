package com.captain.servant.spiritual;

import com.captain.servant.TypeMoonServant;
import com.captain.servant.network.payload.SpiritualizationStatePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDefenseHandler;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardFlightController;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.magic.special.TimeAlterEventHandler;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

public final class SpiritualizationService {
    public static final int PHASE_NONE = 0;
    public static final int PHASE_ENTERING = 1;
    public static final int PHASE_ACTIVE = 2;
    public static final int PHASE_EXITING = 3;

    private static final int TRANSITION_TICKS = 60;
    private static final int EXIT_REVEAL_TICKS = 30;
    private static final int COOLDOWN_TICKS = 600;
    private static final float ACTIVE_DAMAGE_INTERRUPT_THRESHOLD = 20.0F;
    private static final double MP_COST = 5.0D;
    private static final double PASS_THROUGH_Y_OFFSET = 0.0D;

    private static final String PHASE_TAG = "FateSupportSpiritualizationPhase";
    private static final String PHASE_UNTIL_TAG = "FateSupportSpiritualizationPhaseUntil";
    private static final String EXIT_REVEAL_TAG = "FateSupportSpiritualizationExitReveal";
    private static final String COOLDOWN_UNTIL_TAG = "FateSupportSpiritualizationCooldownUntil";
    private static final String PREVIOUS_INVISIBLE_TAG = "FateSupportSpiritualizationPreviousInvisible";
    private static final String LOCKED_Y_TAG = "FateSupportSpiritualizationLockedY";

    private static final Set<UUID> ACTIVE_FIELDS = ConcurrentHashMap.newKeySet();

    public static boolean toggle(ServerPlayer player) {
        if (!isServantCardPlayer(player)) {
            player.displayClientMessage(Component.translatable("message.fatesupport.spiritualization.requires_servant_card"), true);
            return false;
        }

        CompoundTag data = player.getPersistentData();
        int phase = phase(player);
        if (phase == PHASE_ENTERING || phase == PHASE_EXITING) {
            return false;
        }
        if (phase == PHASE_ACTIVE) {
            beginExit(player);
            return true;
        }

        long now = player.level().getGameTime();
        long cooldownUntil = data.getLong(COOLDOWN_UNTIL_TAG);
        if (cooldownUntil > now) {
            player.displayClientMessage(
                    Component.translatable("message.fatesupport.spiritualization.cooldown", formatSeconds(cooldownUntil - now)),
                    true
            );
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables variables = variables(player);
        if (!ServantCardManaService.consume(player, variables, MP_COST)) {
            return false;
        }

        data.putInt(PHASE_TAG, PHASE_ENTERING);
        data.putLong(PHASE_UNTIL_TAG, now + TRANSITION_TICKS);
        data.remove(EXIT_REVEAL_TAG);
        sendState(player);
        player.displayClientMessage(Component.translatable("message.fatesupport.spiritualization.entering"), true);
        return true;
    }

    public static int phase(ServerPlayer player) {
        return player.getPersistentData().getInt(PHASE_TAG);
    }

    public static boolean isActive(ServerPlayer player) {
        return phase(player) == PHASE_ACTIVE;
    }

    public static boolean isRestricted(ServerPlayer player) {
        int phase = phase(player);
        return phase == PHASE_ACTIVE || phase == PHASE_EXITING;
    }

    public static boolean isRestricted(Player player) {
        return player instanceof ServerPlayer serverPlayer && isRestricted(serverPlayer);
    }

    public static boolean isSpiritualized(Entity entity) {
        return entity instanceof ServerPlayer player && isRestricted(player);
    }

    public static boolean shouldPassThrough(ServerPlayer player) {
        return player != null
                && isRestricted(player)
                && player.isShiftKeyDown()
                && player.onGround()
                && player.getDeltaMovement().y <= 0.0D
                && !ServantMasterCarryBridge.isCarryingMaster(player);
    }

    public static void clearOnDisconnect(ServerPlayer player) {
        stopWithoutCooldown(player);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer tracker
                && event.getTarget() instanceof ServerPlayer target
                && phase(target) != PHASE_NONE) {
            sendState(target, tracker);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearOnDisconnect(player);
        }
    }

    static void tickPlayer(ServerPlayer player) {
        if (!isServantCardPlayer(player)) {
            if (phase(player) != PHASE_NONE) {
                stopWithoutCooldown(player);
            }
            return;
        }

        int phase = phase(player);
        if (phase == PHASE_NONE) {
            player.getPersistentData().remove(LOCKED_Y_TAG);
            return;
        }

        if (phase == PHASE_ENTERING) {
            maintainEnteringParticles(player);
            if (player.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)) {
                revealAndStartCooldown(player, "message.fatesupport.spiritualization.revealed");
                return;
            }
            if (player.level().getGameTime() >= player.getPersistentData().getLong(PHASE_UNTIL_TAG)) {
                enterActive(player);
            }
        } else if (phase == PHASE_ACTIVE) {
            maintainActive(player);
            if (player.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)) {
                revealAndStartCooldown(player, "message.fatesupport.spiritualization.revealed");
            } else if (!player.isInvisible()) {
                // Other effects may refresh entity visibility. Only glowing is allowed to reveal this state.
                player.setInvisible(true);
                sendState(player);
            }
        } else if (phase == PHASE_EXITING) {
            maintainExitingParticles(player);
            long now = player.level().getGameTime();
            CompoundTag data = player.getPersistentData();
            if (player.isInvisible() && now >= data.getLong(EXIT_REVEAL_TAG)) {
                restoreVisibility(player);
                sendState(player);
            }
            if (now >= data.getLong(PHASE_UNTIL_TAG)) {
                finishExit(player);
            }
        }

        if (isRestricted(player)) {
            maintainRestrictions(player);
        }
    }

    private static void enterActive(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.putInt(PHASE_TAG, PHASE_ACTIVE);
        data.remove(PHASE_UNTIL_TAG);
        data.remove(EXIT_REVEAL_TAG);
        data.putBoolean(PREVIOUS_INVISIBLE_TAG, player.isInvisible());
        clearActiveServantSkills(player);
        player.setInvisible(true);
        ACTIVE_FIELDS.add(player.getUUID());
        playBeaconSound(player);
        sendState(player);
        player.displayClientMessage(Component.translatable("message.fatesupport.spiritualization.active"), true);
    }

    private static void beginExit(ServerPlayer player) {
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        data.putInt(PHASE_TAG, PHASE_EXITING);
        data.putLong(PHASE_UNTIL_TAG, now + TRANSITION_TICKS);
        data.putLong(EXIT_REVEAL_TAG, now + EXIT_REVEAL_TICKS);
        sendState(player);
        player.displayClientMessage(Component.translatable("message.fatesupport.spiritualization.exiting"), true);
    }

    private static void finishExit(ServerPlayer player) {
        restoreVisibility(player);
        player.getPersistentData().remove(PREVIOUS_INVISIBLE_TAG);
        ACTIVE_FIELDS.remove(player.getUUID());
        player.getPersistentData().putInt(PHASE_TAG, PHASE_NONE);
        player.getPersistentData().putLong(COOLDOWN_UNTIL_TAG, player.level().getGameTime() + COOLDOWN_TICKS);
        player.getPersistentData().remove(PHASE_UNTIL_TAG);
        player.getPersistentData().remove(EXIT_REVEAL_TAG);
        player.getPersistentData().remove(LOCKED_Y_TAG);
        player.noPhysics = player.isSpectator();
        playBeaconSound(player);
        sendState(player);
        player.displayClientMessage(Component.translatable("message.fatesupport.spiritualization.finished"), true);
    }

    private static void revealAndStartCooldown(ServerPlayer player, String messageKey) {
        restoreVisibility(player);
        player.getPersistentData().remove(PREVIOUS_INVISIBLE_TAG);
        ACTIVE_FIELDS.remove(player.getUUID());
        CompoundTag data = player.getPersistentData();
        data.putInt(PHASE_TAG, PHASE_NONE);
        data.putLong(COOLDOWN_UNTIL_TAG, player.level().getGameTime() + COOLDOWN_TICKS);
        data.remove(PHASE_UNTIL_TAG);
        data.remove(EXIT_REVEAL_TAG);
        data.remove(LOCKED_Y_TAG);
        player.noPhysics = player.isSpectator();
        sendState(player);
        player.displayClientMessage(Component.translatable(messageKey), true);
    }

    private static void stopWithoutCooldown(ServerPlayer player) {
        if (phase(player) == PHASE_NONE && !ACTIVE_FIELDS.contains(player.getUUID())
                && !player.getPersistentData().contains(PREVIOUS_INVISIBLE_TAG)) {
            return;
        }
        restoreVisibility(player);
        player.getPersistentData().remove(PREVIOUS_INVISIBLE_TAG);
        ACTIVE_FIELDS.remove(player.getUUID());
        CompoundTag data = player.getPersistentData();
        data.putInt(PHASE_TAG, PHASE_NONE);
        data.remove(PHASE_UNTIL_TAG);
        data.remove(EXIT_REVEAL_TAG);
        data.remove(LOCKED_Y_TAG);
        player.noPhysics = player.isSpectator();
        sendState(player);
    }

    private static void restoreVisibility(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        player.setInvisible(data.getBoolean(PREVIOUS_INVISIBLE_TAG)
                || player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY));
    }

    private static void maintainEnteringParticles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double progress = 1.0D - Math.max(0L, player.getPersistentData().getLong(PHASE_UNTIL_TAG) - level.getGameTime())
                / (double) TRANSITION_TICKS;
        for (int i = 0; i < 8; i++) {
            double angle = (level.getGameTime() * 0.18D) + i * Math.PI / 4.0D;
            double radius = 0.1D + progress * 0.9D;
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + 0.9D + (i % 3 - 1) * 0.25D;
            double z = player.getZ() + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    private static void maintainActive(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || level.getGameTime() % 2L != 0L) {
            return;
        }
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI * 2.0D / 16.0D + level.getGameTime() * 0.04D;
            double radius = 0.72D + (i % 2) * 0.12D;
            double y = player.getY() + 0.15D + (i % 4) * 0.48D;
            level.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX() + Math.cos(angle) * radius,
                    y,
                    player.getZ() + Math.sin(angle) * radius,
                    1,
                    0,
                    0.02D,
                    0,
                    0
            );
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX() + Math.cos(angle + 0.18D) * (radius + 0.08D),
                    y + 0.08D,
                    player.getZ() + Math.sin(angle + 0.18D) * (radius + 0.08D),
                    1,
                    0,
                    0.01D,
                    0,
                    0
            );
        }
    }

    private static void maintainExitingParticles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        double progress = Math.min(1.0D, Math.max(0L, data.getLong(PHASE_UNTIL_TAG) - level.getGameTime())
                / (double) TRANSITION_TICKS);
        for (int i = 0; i < 8; i++) {
            double angle = (level.getGameTime() * 0.18D) + i * Math.PI / 4.0D;
            double radius = 0.1D + progress * 0.9D;
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.9D + (i % 3 - 1) * 0.25D,
                    player.getZ() + Math.sin(angle) * radius,
                    1,
                    -Math.cos(angle) * 0.03D,
                    0,
                    -Math.sin(angle) * 0.03D,
                    0
            );
        }
    }

    private static void maintainRestrictions(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables variables = variables(player);
        if (variables.servant_card_flying) {
            ServantCardFlightController.stop(player, variables, true);
            variables.servant_card_flying = false;
            variables.syncServantCardRuntime(player);
        }

        boolean canPassThrough = shouldPassThrough(player);
        CompoundTag data = player.getPersistentData();
        if (canPassThrough) {
            if (!data.contains(LOCKED_Y_TAG)) {
                data.putDouble(LOCKED_Y_TAG, player.getY());
            }
            player.noPhysics = true;
            player.setPos(player.getX(), data.getDouble(LOCKED_Y_TAG) + PASS_THROUGH_Y_OFFSET, player.getZ());
            player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
        } else {
            player.noPhysics = false;
            data.remove(LOCKED_Y_TAG);
        }
    }

    private static void clearActiveServantSkills(ServerPlayer player) {
        net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService.clear(player);
        TypeMoonWorldModVariables.PlayerVariables variables = variables(player);
        ServantCardDefenseHandler.clear(player);
        if (variables.servant_card_flying) {
            ServantCardFlightController.stop(player, variables, true);
        }
        variables.servant_card_flying = false;
        variables.servant_card_medusa_mystic_eyes_active = false;
        variables.syncServantCardRuntime(player);
        if (TimeAlterEventHandler.isActive(player)) {
            TimeAlterEventHandler.release(player);
        }
        player.stopUsingItem();

        CompoundTag data = player.getPersistentData();
        List<String> keys = new ArrayList<>(data.getAllKeys());
        for (String key : keys) {
            if (key.startsWith("ServantCard")
                    && (key.endsWith("Active") || key.endsWith("Until") || key.contains("ActiveUntil"))) {
                data.remove(key);
            }
        }
        data.remove("GilgameshDivineShieldActive");
        data.remove("GilgameshDivineShieldUntil");
        data.remove("GilgameshDivineShieldHp");
        data.remove("ArtoriaManaBurstUntil");
        data.remove("ArtoriaInvisibleAirActive");
        data.remove("ArtoriaInvisibleAirActiveUntil");
    }

    private static boolean isServantCardPlayer(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isSpectator()
                && TypeMoonWorldApi.servantForm(player).transformed();
    }

    private static TypeMoonWorldModVariables.PlayerVariables variables(ServerPlayer player) {
        return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
    }

    private static void sendState(ServerPlayer player) {
        sendState(player, null);
    }

    private static void sendState(ServerPlayer player, ServerPlayer receiver) {
        CompoundTag data = player.getPersistentData();
        long remaining = Math.max(0L, data.getLong(COOLDOWN_UNTIL_TAG) - player.level().getGameTime());
        boolean restricted = isRestricted(player);
        SpiritualizationStatePayload payload = new SpiritualizationStatePayload(
                player.getUUID(),
                phase(player),
                player.isInvisible() && (phase(player) == PHASE_ACTIVE || phase(player) == PHASE_EXITING),
                restricted,
                (int) Math.min(Integer.MAX_VALUE, remaining)
        );
        if (receiver == null) {
            // Mock players and connections that are closing have no negotiated payload channels.
            if (player.connection.hasChannel(SpiritualizationStatePayload.TYPE.id())) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload);
            }
        } else if (receiver.connection.hasChannel(SpiritualizationStatePayload.TYPE.id())) {
            PacketDistributor.sendToPlayer(receiver, payload);
        }
    }

    private static void playBeaconSound(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.31F,
                1.0F
        );
    }

    private static String formatSeconds(long ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            if (event.getSource().getEntity() instanceof ServerPlayer attacker && isRestricted(attacker)) {
                event.setCanceled(true);
            }
            return;
        }

        int phase = phase(victim);
        if (phase == PHASE_ENTERING && event.getAmount() > 0.0F) {
            revealAndStartCooldown(victim, "message.fatesupport.spiritualization.interrupted");
        }
        if (phase == PHASE_ACTIVE && event.getAmount() > ACTIVE_DAMAGE_INTERRUPT_THRESHOLD) {
            revealAndStartCooldown(victim, "message.fatesupport.spiritualization.interrupted");
        }
        if (isRestricted(victim) && event.getSource().is(DamageTypes.IN_WALL)) {
            event.setCanceled(true);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && isRestricted(attacker)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        if (isRestricted(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isRestricted(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isRestricted(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isRestricted(event.getEntity()) && !ServantMasterCarryBridge.isCarryPair(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (isRestricted(event.getEntity())) {
            event.setNewSpeed(0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBreak(BlockEvent.BreakEvent event) {
        if (isRestricted(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    public static boolean isCarryingMaster(ServerPlayer player) {
        return ServantMasterCarryBridge.isCarryingMaster(player);
    }

    private static final class ServantMasterCarryBridge {
        private static boolean isCarryPair(Player player, Entity target) {
            return net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService.isCarryPair(player, target);
        }

        private static boolean isCarryingMaster(Player player) {
            return net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService.isCarryingMaster(player);
        }
    }
}
