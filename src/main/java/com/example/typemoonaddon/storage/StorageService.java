package com.example.typemoonaddon.storage;

import com.example.typemoonaddon.magic.StorageMagic;
import com.example.typemoonaddon.network.AddonNetwork;
import com.example.typemoonaddon.network.StorageDataSyncPayload;
import com.example.typemoonaddon.kimaris.KimarisService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StorageService {
    public static final int LONG_PRESS_TICKS = 10;
    public static final int REPEAT_INTERVAL_TICKS = 10;
    private static final int MODE_SWITCH_DEBOUNCE_TICKS = 4;
    public static final double TRANSFER_AMOUNT = 100.0D;

    private static final Map<UUID, CastState> CAST_STATES = new HashMap<>();
    private static final Set<UUID> PRESSED_PLAYERS = new HashSet<>();
    private static final Map<UUID, Long> LAST_MODE_SWITCH_TICKS = new HashMap<>();

    private StorageService() {
    }

    public static void handleCastInput(ServerPlayer player, boolean pressed) {
        if (KimarisService.isFrozen(player)) {
            clearCastState(player);
            return;
        }
        if (pressed) {
            if (!PRESSED_PLAYERS.add(player.getUUID())) {
                return;
            }
            handlePress(player);
        } else {
            if (!PRESSED_PLAYERS.remove(player.getUUID())) {
                return;
            }
            handleRelease(player);
        }
    }

    public static void switchMode(ServerPlayer player) {
        if (KimarisService.isFrozen(player)) {
            clearCastState(player);
            return;
        }
        if (!isValidStorageCaster(player, true)) {
            stopCastState(player);
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        UUID playerId = player.getUUID();
        Long lastSwitchTick = LAST_MODE_SWITCH_TICKS.get(playerId);
        if (lastSwitchTick != null && gameTime - lastSwitchTick < MODE_SWITCH_DEBOUNCE_TICKS) {
            return;
        }
        LAST_MODE_SWITCH_TICKS.put(playerId, gameTime);

        stopCastState(player);
        StorageData data = player.getData(StorageAttachments.PLAYER_STORAGE);
        int mode = data.cycleMode();
        syncData(player, data);
        player.displayClientMessage(Component.translatable(mode == StorageData.MANA_MODE
                ? "message.typemoonworld.storage.mode.mana"
                : "message.typemoonworld.storage.mode.items"), true);
    }

    public static void tick(ServerPlayer player) {
        CastState state = CAST_STATES.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (!isValidStorageCaster(player, true)) {
            stopCastState(player);
            return;
        }

        StorageData data = player.getData(StorageAttachments.PLAYER_STORAGE);
        if (data.getMode() != StorageData.MANA_MODE) {
            stopCastState(player);
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        if (gameTime < state.nextDepositTick) {
            return;
        }

        if (!depositMana(player, data)) {
            stopCastState(player);
            return;
        }

        state.longPressTriggered = true;
        state.nextDepositTick = gameTime + REPEAT_INTERVAL_TICKS;
    }

    public static void clearCastState(Player player) {
        if (player != null) {
            UUID playerId = player.getUUID();
            CAST_STATES.remove(playerId);
            PRESSED_PLAYERS.remove(playerId);
            LAST_MODE_SWITCH_TICKS.remove(playerId);
        }
    }

    public static void clearAllCastStates() {
        CAST_STATES.clear();
        PRESSED_PLAYERS.clear();
        LAST_MODE_SWITCH_TICKS.clear();
    }

    public static void syncData(ServerPlayer player) {
        syncData(player, player.getData(StorageAttachments.PLAYER_STORAGE));
    }

    private static void handlePress(ServerPlayer player) {
        if (!isValidStorageCaster(player, true)) {
            stopCastState(player);
            return;
        }

        UUID playerId = player.getUUID();
        StorageData data = player.getData(StorageAttachments.PLAYER_STORAGE);
        if (data.getMode() == StorageData.ITEM_MODE) {
            CAST_STATES.put(playerId, CastState.itemMode());
            openStorageMenu(player);
            return;
        }

        long now = player.serverLevel().getGameTime();
        CAST_STATES.put(playerId, CastState.manaMode(now + LONG_PRESS_TICKS));
    }

    private static void handleRelease(ServerPlayer player) {
        CastState state = CAST_STATES.remove(player.getUUID());
        if (state == null || state.itemModeHandled || state.longPressTriggered) {
            return;
        }

        if (!isValidStorageCaster(player, false)) {
            return;
        }

        StorageData data = player.getData(StorageAttachments.PLAYER_STORAGE);
        if (data.getMode() == StorageData.MANA_MODE) {
            withdrawMana(player, data);
        }
    }

    private static boolean depositMana(ServerPlayer player, StorageData data) {
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double playerMana = finiteNonNegative(vars.player_mana);
        double storedMana = data.getStoredMana();
        double freeCapacity = Math.max(0.0D, StorageData.MANA_CAPACITY - storedMana);
        double amount = Math.min(TRANSFER_AMOUNT, Math.min(playerMana, freeCapacity));
        if (amount <= 0.0D) {
            player.displayClientMessage(Component.translatable(freeCapacity <= 0.0D
                    ? "message.typemoonworld.storage.mana.storage_full"
                    : "message.typemoonworld.storage.mana.player_empty"), true);
            return false;
        }

        vars.player_mana = Math.max(0.0D, playerMana - amount);
        data.setStoredMana(storedMana + amount);
        vars.syncMana(player);
        syncData(player, data);
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.storage.mana.deposited",
                formatMana(amount),
                formatMana(data.getStoredMana()),
                formatMana(StorageData.MANA_CAPACITY)
        ), true);
        return true;
    }

    private static void withdrawMana(ServerPlayer player, StorageData data) {
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double playerMana = finiteNonNegative(vars.player_mana);
        double maxMana = finiteNonNegative(vars.player_max_mana);
        double storedMana = data.getStoredMana();
        double missingMana = Math.max(0.0D, maxMana - playerMana);
        double amount = Math.min(TRANSFER_AMOUNT, Math.min(storedMana, missingMana));
        if (amount <= 0.0D) {
            player.displayClientMessage(Component.translatable(storedMana <= 0.0D
                    ? "message.typemoonworld.storage.mana.storage_empty"
                    : "message.typemoonworld.storage.mana.player_full"), true);
            return;
        }

        data.setStoredMana(storedMana - amount);
        vars.player_mana = Math.min(maxMana, playerMana + amount);
        vars.syncMana(player);
        syncData(player, data);
        player.displayClientMessage(Component.translatable(
                "message.typemoonworld.storage.mana.withdrawn",
                formatMana(amount),
                formatMana(data.getStoredMana()),
                formatMana(StorageData.MANA_CAPACITY)
        ), true);
    }

    public static void openStorageMenu(ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu) {
            stopCastState(player);
            return;
        }

        player.openMenu(new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("menu.typemoonworld.storage");
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(
                    int containerId,
                    @NotNull Inventory playerInventory,
                    @NotNull Player menuPlayer
            ) {
                return menuPlayer == player ? new StorageMenu(containerId, playerInventory) : null;
            }
        });
    }

    private static boolean isValidStorageCaster(ServerPlayer player, boolean requireNoMenu) {
        if (player == null || !player.isAlive() || player.isRemoved() || player.isSpectator() || player.hasDisconnected()) {
            return false;
        }
        if (requireNoMenu && player.containerMenu != player.inventoryMenu) {
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        vars.rebuildSelectedMagicsFromActiveWheel();
        PlayerMagicSelectionService.prepareCurrentSelection(player, vars);
        return vars.is_magus
                && vars.player_magic_attributes_imaginary_number
                && vars.is_magic_circuit_open
                && vars.magic_cooldown <= 0.0D
                && PlayerMagicSelectionService.isCurrentSelection(vars, StorageMagic.MAGIC_ID);
    }

    private static void stopCastState(Player player) {
        if (player != null) {
            CAST_STATES.remove(player.getUUID());
        }
    }

    private static void syncData(ServerPlayer player, StorageData data) {
        AddonNetwork.sendToPlayer(player, new StorageDataSyncPayload(data));
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static String formatMana(double value) {
        double normalized = finiteNonNegative(value);
        long rounded = Math.round(normalized);
        return Math.abs(normalized - rounded) < 0.000001D ? Long.toString(rounded) : String.format(java.util.Locale.ROOT, "%.2f", normalized);
    }

    private static final class CastState {
        private long nextDepositTick;
        private boolean longPressTriggered;
        private final boolean itemModeHandled;

        private CastState(long nextDepositTick, boolean itemModeHandled) {
            this.nextDepositTick = nextDepositTick;
            this.itemModeHandled = itemModeHandled;
        }

        private static CastState manaMode(long firstDepositTick) {
            return new CastState(firstDepositTick, false);
        }

        private static CastState itemMode() {
            return new CastState(Long.MAX_VALUE, true);
        }
    }
}
