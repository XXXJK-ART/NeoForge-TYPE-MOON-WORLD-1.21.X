package com.example.typemoonaddon.compat;

import com.example.typemoonaddon.TypeMoonAddon;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PlayerVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/** Keeps Type Moon World's contract state aligned with fully corrupted servants. */
public final class TypeMoonContractBridge {
    public static void transferTo(ServerPlayer controller, LivingEntity servant) {
        if (controller == null || servant == null || controller == servant) {
            return;
        }
        try {
            PlayerVariables masterData = variables(controller);
            ensureMasterActive(controller, masterData);
            releaseCurrentServant(controller, masterData, servant);

            if (servant instanceof ServantEntity entityServant) {
                transferEntityServant(controller, masterData, entityServant);
            } else if (servant instanceof ServerPlayer cardServant) {
                transferCardServant(controller, masterData, cardServant);
            }
        } catch (RuntimeException exception) {
            TypeMoonAddon.LOGGER.error(
                "Could not transfer the corrupted servant contract for {}",
                servant.getDisplayName().getString(),
                exception
            );
        }
    }

    @Nullable
    public static LivingEntity boundServant(ServerPlayer master) {
        if (master == null) {
            return null;
        }
        try {
            return MasterStateManager.getBoundServantEntity(master, variables(master));
        } catch (RuntimeException exception) {
            TypeMoonAddon.LOGGER.error("Could not resolve a command spell target", exception);
            return null;
        }
    }

    public static boolean isContractedTo(ServerPlayer master, LivingEntity servant) {
        if (master == null || servant == null || master == servant) {
            return false;
        }
        if (servant instanceof ServantEntity entityServant) {
            return master.getUUID().equals(entityServant.getMasterUuid());
        }
        if (servant instanceof ServerPlayer cardServant) {
            PlayerVariables servantData = variables(cardServant);
            return servantData.servant_card_transformed
                && master.getUUID().toString().equals(servantData.servant_card_master_uuid)
                && MasterServantLinkService.SERVANT_CONTRACT_CONTRACTED.equals(
                    servantData.servant_card_contract_state
                );
        }
        return false;
    }

    private static void transferEntityServant(
        ServerPlayer controller,
        PlayerVariables masterData,
        ServantEntity servant
    ) {
        UUID oldMasterId = servant.getMasterUuid();
        if (oldMasterId != null && !oldMasterId.equals(controller.getUUID())) {
            clearLoadedMasterReference(controller, oldMasterId, servant.getUUID());
        }
        servant.unbindMaster();
        servant.bindMaster(controller);
        masterData.master_servant_uuid = servant.getUUID().toString();
        MasterServantLinkService.establishEntityContract(controller, masterData, servant);
        masterData.syncPlayerVariables(controller);
    }

    private static void transferCardServant(
        ServerPlayer controller,
        PlayerVariables masterData,
        ServerPlayer servant
    ) {
        PlayerVariables servantData = variables(servant);
        if (!servantData.servant_card_transformed) {
            return;
        }

        UUID oldMasterId = parseUuid(servantData.servant_card_master_uuid);
        if (oldMasterId != null && !oldMasterId.equals(controller.getUUID())) {
            clearLoadedMasterReference(controller, oldMasterId, servant.getUUID());
        }
        clearServantSide(servant, servantData);

        masterData.master_servant_uuid = servant.getUUID().toString();
        servantData.servant_card_master_uuid = controller.getUUID().toString();
        MasterServantLinkService.establishContract(controller, masterData, servant, servantData);
        servantData.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_CONTRACTED;
        MasterServantLinkService.clearSurvival(servantData);
        servant.getPersistentData().remove("MasterServantIndependentActionState");
        masterData.syncPlayerVariables(controller);
        servantData.syncPlayerVariables(servant);
    }

    private static void ensureMasterActive(ServerPlayer controller, PlayerVariables data) {
        if (data.master_active) {
            return;
        }
        if (!MasterStateManager.activate(controller)) {
            // Corruption overrides the normal rule that rejects servant-card users as Masters.
            data.master_active = true;
            data.master_servant_uuid = "";
            data.master_servant_contract_id = "";
            data.master_command_spell_pose_active = false;
            data.syncPlayerVariables(controller);
        }
    }

    private static void releaseCurrentServant(
        ServerPlayer controller,
        PlayerVariables masterData,
        LivingEntity replacement
    ) {
        LivingEntity current = MasterStateManager.getBoundServantEntity(controller, masterData);
        if (current == replacement) {
            return;
        }
        if (current instanceof ServantEntity entityServant) {
            entityServant.unbindMaster();
        } else if (current instanceof ServerPlayer cardServant) {
            clearServantSide(cardServant, variables(cardServant));
        }
        masterData.master_servant_uuid = "";
        masterData.master_servant_contract_id = "";
        MasterServantLinkService.clearSnapshot(masterData);
        MasterServantLinkService.clearContractTags(controller);
        masterData.syncPlayerVariables(controller);
    }

    private static void clearLoadedMasterReference(
        ServerPlayer controller,
        UUID oldMasterId,
        UUID servantId
    ) {
        ServerPlayer oldMaster = controller.server.getPlayerList().getPlayer(oldMasterId);
        if (oldMaster == null) {
            return;
        }
        PlayerVariables oldMasterData = variables(oldMaster);
        if (servantId.toString().equals(oldMasterData.master_servant_uuid)) {
            oldMasterData.master_servant_uuid = "";
            oldMasterData.master_servant_contract_id = "";
            MasterServantLinkService.clearSnapshot(oldMasterData);
            MasterServantLinkService.clearContractTags(oldMaster);
            oldMasterData.syncPlayerVariables(oldMaster);
        }
    }

    private static void clearServantSide(ServerPlayer servant, PlayerVariables data) {
        data.servant_card_master_uuid = "";
        data.servant_card_contract_id = "";
        data.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_MASTERLESS;
        MasterServantLinkService.clearSurvival(data);
        MasterServantLinkService.clearContractTags(servant);
        data.syncPlayerVariables(servant);
    }

    private static PlayerVariables variables(ServerPlayer player) {
        return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
    }

    @Nullable
    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private TypeMoonContractBridge() {
    }
}
