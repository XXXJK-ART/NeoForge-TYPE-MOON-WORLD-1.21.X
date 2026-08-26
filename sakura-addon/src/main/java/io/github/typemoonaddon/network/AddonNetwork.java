package io.github.typemoonaddon.network;

import io.github.typemoonaddon.client.ClientPayloadBridge;
import io.github.typemoonaddon.data.ImaginarySpaceData.MagicMode;
import io.github.typemoonaddon.data.ImaginarySpaceData.BlackMudSummonMode;
import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowArtMode;
import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowCommandMode;
import io.github.typemoonaddon.magic.BlackMudControlService;
import io.github.typemoonaddon.magic.ImaginaryStorageService;
import io.github.typemoonaddon.magic.PollutionService;
import io.github.typemoonaddon.magic.TypeMoonIntegration;
import io.github.typemoonaddon.magic.ShadowMaterializationService;
import io.github.typemoonaddon.magic.ShadowTransferService;
import io.github.typemoonaddon.magic.SummonBlackMudService;
import io.github.typemoonaddon.menu.ImaginarySpaceMenu;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.shadowlogic.magic.ShadowArtService;
import io.github.typemoonaddon.shadowlogic.network.SetShadowArtModePayload;
import io.github.typemoonaddon.shadowlogic.network.ShadowBindingEffectPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class AddonNetwork {
    private static final Map<UUID, Long> LAST_OPEN_REQUEST = new HashMap<>();
    private static final Map<UUID, Long> LAST_MODE_REQUEST = new HashMap<>();
    private static final Map<UUID, Long> LAST_SHADOW_COMMAND_REQUEST = new HashMap<>();
    private static final Map<UUID, Long> LAST_BLOCK_HOLD_REQUEST = new HashMap<>();
    private static final Map<UUID, Long> LAST_MATERIALIZATION_HOLD_REQUEST = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRANSFER_SELECTION = new HashMap<>();
    private static final Map<UUID, Long> LAST_BLACK_MUD_MODE_REQUEST = new HashMap<>();
    private static final Map<UUID, Long> LAST_DEVOURER_REQUEST = new HashMap<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(OpenSpacePayload.TYPE, OpenSpacePayload.STREAM_CODEC, AddonNetwork::handleOpenSpace);
        registrar.playToServer(
            SetShadowArtModePayload.TYPE, SetShadowArtModePayload.STREAM_CODEC, AddonNetwork::handleSetShadowArtMode
        );
        registrar.playToServer(
            SetImaginaryModePayload.TYPE,
            SetImaginaryModePayload.STREAM_CODEC,
            AddonNetwork::handleSetImaginaryMode
        );
        registrar.playToServer(
            SetShadowCommandModePayload.TYPE,
            SetShadowCommandModePayload.STREAM_CODEC,
            AddonNetwork::handleSetShadowCommandMode
        );
        registrar.playToServer(
            SetShadowAttackPayload.TYPE,
            SetShadowAttackPayload.STREAM_CODEC,
            AddonNetwork::handleSetShadowAttack
        );
        registrar.playToServer(
            BlockAbsorptionHoldPayload.TYPE,
            BlockAbsorptionHoldPayload.STREAM_CODEC,
            AddonNetwork::handleBlockAbsorptionHold
        );
        registrar.playToServer(
            ShadowMaterializationHoldPayload.TYPE,
            ShadowMaterializationHoldPayload.STREAM_CODEC,
            AddonNetwork::handleShadowMaterializationHold
        );
        registrar.playToClient(EffectPayload.TYPE, EffectPayload.STREAM_CODEC, AddonNetwork::handleEffect);
        registrar.playToClient(DeathFadePayload.TYPE, DeathFadePayload.STREAM_CODEC, AddonNetwork::handleDeathFade);
        registrar.playToClient(
            VoidAbsorptionLinkPayload.TYPE,
            VoidAbsorptionLinkPayload.STREAM_CODEC,
            AddonNetwork::handleVoidAbsorptionLink
        );
        registrar.playToClient(
            ShadowBindingEffectPayload.TYPE,
            ShadowBindingEffectPayload.STREAM_CODEC,
            AddonNetwork::handleShadowBindingEffect
        );
        registrar.playToClient(
            MagicOutputShockwavePayload.TYPE,
            MagicOutputShockwavePayload.STREAM_CODEC,
            AddonNetwork::handleMagicOutputShockwave
        );
        registrar.playToClient(
            OpenShadowTransferPayload.TYPE,
            OpenShadowTransferPayload.STREAM_CODEC,
            AddonNetwork::handleOpenShadowTransfer
        );
        registrar.playToServer(
            SelectShadowTransferPayload.TYPE,
            SelectShadowTransferPayload.STREAM_CODEC,
            AddonNetwork::handleSelectShadowTransfer
        );
        registrar.playToServer(
            SetBlackMudSummonModePayload.TYPE,
            SetBlackMudSummonModePayload.STREAM_CODEC,
            AddonNetwork::handleSetBlackMudSummonMode
        );
        registrar.playToServer(
            RequestDevourerSelectionPayload.TYPE,
            RequestDevourerSelectionPayload.STREAM_CODEC,
            AddonNetwork::handleRequestDevourerSelection
        );
        registrar.playToClient(
            OpenDevourerSelectionPayload.TYPE,
            OpenDevourerSelectionPayload.STREAM_CODEC,
            AddonNetwork::handleOpenDevourerSelection
        );
        registrar.playToClient(
            NightShadowCameraPayload.TYPE,
            NightShadowCameraPayload.STREAM_CODEC,
            AddonNetwork::handleNightShadowCamera
        );
        registrar.playToServer(
            SetDevourerSelectionPayload.TYPE,
            SetDevourerSelectionPayload.STREAM_CODEC,
            AddonNetwork::handleSetDevourerSelection
        );
    }

    private static void handleSetImaginaryMode(SetImaginaryModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
                return;
            }
            if (payload.modeId() < 0 || payload.modeId() >= MagicMode.values().length) {
                return;
            }

            long now = player.serverLevel().getGameTime();
            long previous = LAST_MODE_REQUEST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
            if (now - previous < 2L) {
                return;
            }
            LAST_MODE_REQUEST.put(player.getUUID(), now);
            ImaginaryStorageService.selectMode(player, MagicMode.values()[payload.modeId()]);
        });
    }

    private static void handleSetShadowArtMode(SetShadowArtModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || payload.modeId() < 0 || payload.modeId() >= ShadowArtMode.values().length) return;
            ShadowArtService.selectMode(player, ShadowArtMode.values()[payload.modeId()]);
        });
    }

    private static void handleSetShadowCommandMode(SetShadowCommandModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
                return;
            }
            if (payload.modeId() < 0 || payload.modeId() >= ShadowCommandMode.values().length) {
                return;
            }

            long now = player.serverLevel().getGameTime();
            long previous = LAST_SHADOW_COMMAND_REQUEST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
            if (now - previous < 2L) {
                return;
            }
            LAST_SHADOW_COMMAND_REQUEST.put(player.getUUID(), now);
            BlackMudControlService.selectMode(player, ShadowCommandMode.values()[payload.modeId()]);
        });
    }

    private static void handleSetShadowAttack(SetShadowAttackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()) {
                BlackMudControlService.setAttackAround(player, payload.enabled());
            }
        });
    }

    private static void handleOpenSpace(OpenSpacePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
                return;
            }
            long now = player.serverLevel().getGameTime();
            long previous = LAST_OPEN_REQUEST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
            if (now - previous < 5L) {
                return;
            }
            LAST_OPEN_REQUEST.put(player.getUUID(), now);
            if (!TypeMoonIntegration.isLearned(player)) {
                player.displayClientMessage(Component.translatable("message.typemoonaddon.not_learned"), true);
                return;
            }
            player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new ImaginarySpaceMenu(containerId, inventory, player),
                Component.translatable("menu.typemoonaddon.imaginary_space")
            ));
        });
    }

    private static void handleBlockAbsorptionHold(BlockAbsorptionHoldPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
                return;
            }
            long now = player.serverLevel().getGameTime();
            long previous = LAST_BLOCK_HOLD_REQUEST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
            if (payload.held() && now <= previous) {
                return;
            }
            LAST_BLOCK_HOLD_REQUEST.put(player.getUUID(), now);
            ImaginaryStorageService.updateBlockCastHeld(player, payload.held());
        });
    }

    private static void handleShadowMaterializationHold(ShadowMaterializationHoldPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
                return;
            }
            long now = player.serverLevel().getGameTime();
            long previous = LAST_MATERIALIZATION_HOLD_REQUEST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
            if (payload.held() && now <= previous) {
                return;
            }
            LAST_MATERIALIZATION_HOLD_REQUEST.put(player.getUUID(), now);
            ShadowMaterializationService.updateHeld(player, payload.held());
        });
    }

    private static void handleEffect(EffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadBridge.accept(payload));
    }

    private static void handleDeathFade(DeathFadePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadBridge.accept(payload));
    }

    private static void handleVoidAbsorptionLink(VoidAbsorptionLinkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadBridge.accept(payload));
    }

    private static void handleShadowBindingEffect(ShadowBindingEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadBridge.accept(payload));
    }

    private static void handleMagicOutputShockwave(MagicOutputShockwavePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadBridge.accept(payload));
    }

    private static void handleOpenShadowTransfer(OpenShadowTransferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadBridge.accept(payload));
    }

    private static void handleOpenDevourerSelection(OpenDevourerSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadBridge.accept(payload));
    }

    private static void handleNightShadowCamera(NightShadowCameraPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloadBridge.accept(payload));
    }

    private static void handleSelectShadowTransfer(SelectShadowTransferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
                return;
            }
            long now = player.serverLevel().getGameTime();
            long previous = LAST_TRANSFER_SELECTION.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
            if (now - previous < 4L) {
                return;
            }
            LAST_TRANSFER_SELECTION.put(player.getUUID(), now);
            ShadowTransferService.begin(player, payload.familiarId());
        });
    }

    private static void handleSetBlackMudSummonMode(SetBlackMudSummonModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
                return;
            }
            if (payload.modeId() < 0 || payload.modeId() >= BlackMudSummonMode.values().length) {
                return;
            }
            long now = player.serverLevel().getGameTime();
            long previous = LAST_BLACK_MUD_MODE_REQUEST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
            if (now - previous < 2L) {
                return;
            }
            LAST_BLACK_MUD_MODE_REQUEST.put(player.getUUID(), now);
            SummonBlackMudService.selectMode(player, BlackMudSummonMode.values()[payload.modeId()]);
        });
    }

    private static void handleRequestDevourerSelection(
        RequestDevourerSelectionPayload payload,
        IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.isAlive()
                || player.isSpectator()
                || !TypeMoonIntegration.isHeroicSpiritDevourerLearned(player)) {
                PacketDistributor.sendToPlayer(player, new OpenDevourerSelectionPayload(java.util.List.of()));
                return;
            }
            long now = player.serverLevel().getGameTime();
            long previous = LAST_DEVOURER_REQUEST.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
            if (now - previous < 5L) {
                PollutionService.openDevourerSelection(player);
                return;
            }
            LAST_DEVOURER_REQUEST.put(player.getUUID(), now);
            PollutionService.openDevourerSelection(player);
        });
    }

    private static void handleSetDevourerSelection(
        SetDevourerSelectionPayload payload,
        IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                || !player.isAlive()
                || player.isSpectator()
                || !TypeMoonIntegration.isHeroicSpiritDevourerLearned(player)) {
                return;
            }
            var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
            if (data.setSelectedCorruptedServants(new HashSet<>(payload.rosterIds()))) {
                player.syncData(ModAttachments.IMAGINARY_SPACE.get());
            }
            player.displayClientMessage(Component.translatable(
                "message.typemoonaddon.heroic_spirit_devourer.selection_saved",
                data.selectedCorruptedServants().size()
            ), true);
        });
    }

    public static void playerLoggedOut(ServerPlayer player) {
        LAST_OPEN_REQUEST.remove(player.getUUID());
        LAST_MODE_REQUEST.remove(player.getUUID());
        LAST_SHADOW_COMMAND_REQUEST.remove(player.getUUID());
        LAST_BLOCK_HOLD_REQUEST.remove(player.getUUID());
        LAST_MATERIALIZATION_HOLD_REQUEST.remove(player.getUUID());
        LAST_TRANSFER_SELECTION.remove(player.getUUID());
        LAST_BLACK_MUD_MODE_REQUEST.remove(player.getUUID());
        LAST_DEVOURER_REQUEST.remove(player.getUUID());
    }

    public static void serverStopping() {
        LAST_OPEN_REQUEST.clear();
        LAST_MODE_REQUEST.clear();
        LAST_SHADOW_COMMAND_REQUEST.clear();
        LAST_BLOCK_HOLD_REQUEST.clear();
        LAST_MATERIALIZATION_HOLD_REQUEST.clear();
        LAST_TRANSFER_SELECTION.clear();
        LAST_BLACK_MUD_MODE_REQUEST.clear();
        LAST_DEVOURER_REQUEST.clear();
    }

    private AddonNetwork() {
    }
}
