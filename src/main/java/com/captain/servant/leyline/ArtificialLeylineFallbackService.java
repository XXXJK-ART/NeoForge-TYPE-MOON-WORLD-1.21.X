package com.captain.servant.leyline;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ArtificialLeylineBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;

import java.util.UUID;

/** Keeps a servant card alive through a valid artificial leyline after its primary master is lost. */
public final class ArtificialLeylineFallbackService {
    private static final String PRIMARY_CONTRACT_TAG = "FateSupportArtificialLeylinePrimaryContract";
    private static final double MANA_DRAIN_PER_SECOND = 5.0D;
    private static final float EMPTY_MANA_DAMAGE_PER_SECOND = 5.0F;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TypeMoonWorldModVariables.PlayerVariables variables = variables(player);
            if (!variables.servant_card_transformed) {
                player.getPersistentData().remove(PRIMARY_CONTRACT_TAG);
                continue;
            }

            if (!isBlank(variables.servant_card_master_uuid) && isValidFallbackBlock(player, variables)) {
                player.getPersistentData().putBoolean(PRIMARY_CONTRACT_TAG, true);
            }
        }
    }

    public static void onMasterLost(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables masterVariables) {
        if (master == null || masterVariables == null) {
            return;
        }

        ServerPlayer servant = playerByUuid(master.getServer(), parseUuid(masterVariables.master_servant_uuid));
        if (master.isAlive() && master.getHealth() > 0.0F) {
            if (servant != null) {
                servant.getPersistentData().remove(PRIMARY_CONTRACT_TAG);
            }
            return;
        }

        if (servant != null && isValidFallbackBlock(servant, variables(servant))) {
            servant.getPersistentData().putBoolean(PRIMARY_CONTRACT_TAG, true);
        } else if (servant != null) {
            servant.getPersistentData().remove(PRIMARY_CONTRACT_TAG);
        }
    }

    /** Returns true when the original master-loss tick must be suppressed. */
    public static boolean handleServantLinkTick(
            ServerPlayer servant,
            TypeMoonWorldModVariables.PlayerVariables servantVariables
    ) {
        if (servant == null || servantVariables == null || !servantVariables.servant_card_transformed
                || !servant.getPersistentData().getBoolean(PRIMARY_CONTRACT_TAG)) {
            return false;
        }

        if (hasLivingPrimaryMaster(servant, servantVariables)) {
            return false;
        }

        // The base mod must own the independent-action countdown. The leyline only takes over afterwards.
        if (MasterServantLinkService.SURVIVAL_INDEPENDENT.equals(servantVariables.master_servant_survival_state)) {
            return false;
        }

        if (!isValidFallbackBlock(servant, servantVariables)) {
            servant.getPersistentData().remove(PRIMARY_CONTRACT_TAG);
            MasterServantLinkService.startMasterLossSurvival(servant, servantVariables);
            servantVariables.syncPlayerVariables(servant);
            return false;
        }

        maintainFallbackState(servant, servantVariables);
        return true;
    }

    /** Prevents the original artificial-leyline regeneration while the block is acting as fallback master. */
    public static boolean handleManaTick(
            ServerPlayer servant,
            TypeMoonWorldModVariables.PlayerVariables servantVariables
    ) {
        if (!isFallbackActive(servant, servantVariables)
                || MasterServantLinkService.SURVIVAL_INDEPENDENT.equals(servantVariables.master_servant_survival_state)) {
            return false;
        }

        maintainFallbackState(servant, servantVariables);
        if (servantVariables.servant_card_max_mana <= 0.0D) {
            servantVariables.servant_card_max_mana = ServantCardManaService.maxManaFor(servantVariables.servant_card_id);
        }
        boolean wasEmpty = servantVariables.servant_card_mana <= 0.0D;
        servantVariables.servant_card_mana = Math.max(0.0D, servantVariables.servant_card_mana - MANA_DRAIN_PER_SECOND / 20.0D);
        servantVariables.servant_card_mana_regen = -MANA_DRAIN_PER_SECOND;

        if (wasEmpty && servant.tickCount % 20 == 0) {
            applyTrueDamage(servant, EMPTY_MANA_DAMAGE_PER_SECOND);
        }

        if (servant.tickCount % 20 == 0) {
            servantVariables.syncMana(servant);
            servantVariables.syncPlayerVariables(servant);
        }
        return true;
    }

    private static boolean isFallbackActive(
            ServerPlayer servant,
            TypeMoonWorldModVariables.PlayerVariables servantVariables
    ) {
        return servant != null
                && servantVariables != null
                && servantVariables.servant_card_transformed
                && servant.getPersistentData().getBoolean(PRIMARY_CONTRACT_TAG)
                && !hasLivingPrimaryMaster(servant, servantVariables)
                && isValidFallbackBlock(servant, servantVariables);
    }

    private static void maintainFallbackState(
            ServerPlayer servant,
            TypeMoonWorldModVariables.PlayerVariables servantVariables
    ) {
        servantVariables.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_CONTRACTED;
        servantVariables.master_servant_link_state = MasterServantLinkService.STATE_NORMAL;
        servantVariables.master_servant_link_decay = 1.0D;
        MasterServantLinkService.clearSurvival(servantVariables);
    }

    private static boolean hasLivingPrimaryMaster(
            ServerPlayer servant,
            TypeMoonWorldModVariables.PlayerVariables servantVariables
    ) {
        UUID masterUuid = parseUuid(servantVariables.servant_card_master_uuid);
        if (masterUuid == null || servant.getServer() == null) {
            return false;
        }

        ServerPlayer master = playerByUuid(servant.getServer(), masterUuid);
        if (master == null || !master.isAlive() || master.getHealth() <= 0.0F) {
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables masterVariables = variables(master);
        return masterVariables.master_active
                && servant.getUUID().toString().equals(masterVariables.master_servant_uuid);
    }

    private static boolean isValidFallbackBlock(
            ServerPlayer servant,
            TypeMoonWorldModVariables.PlayerVariables servantVariables
    ) {
        if (servant == null || servantVariables == null || !servantVariables.servant_card_artificial_leyline_bonus_active
                || isBlank(servantVariables.servant_card_artificial_leyline_dimension)) {
            return false;
        }

        ServerLevel level = servant.serverLevel();
        if (!level.dimension().location().toString().equals(servantVariables.servant_card_artificial_leyline_dimension)) {
            return false;
        }

        BlockPos blockPos = new BlockPos(
                servantVariables.servant_card_artificial_leyline_x,
                servantVariables.servant_card_artificial_leyline_y,
                servantVariables.servant_card_artificial_leyline_z
        );
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        return blockEntity instanceof ArtificialLeylineBlockEntity leyline
                && leyline.isOwnedBy(servant.getUUID());
    }

    private static TypeMoonWorldModVariables.PlayerVariables variables(ServerPlayer player) {
        return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
    }

    private static void applyTrueDamage(ServerPlayer servant, float amount) {
        float healthAfterDamage = servant.getHealth() - amount;
        servant.setHealth(Math.max(0.0F, healthAfterDamage));
        if (healthAfterDamage <= 0.0F && servant.isAlive()) {
            servant.kill();
        }
    }

    private static ServerPlayer playerByUuid(MinecraftServer server, UUID uuid) {
        return server == null || uuid == null ? null : server.getPlayerList().getPlayer(uuid);
    }

    private static UUID parseUuid(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
