package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Applies and restores the addon's temporary state for the Matou Sakura Master Card. */
public final class MatouSakuraMasterProfile {
    public static final ResourceLocation ID = TypeMoonAddon.id("matou_sakura");
    public static final ResourceLocation ALTER_ID = TypeMoonAddon.id("matou_sakura_alter");
    public static final ResourceLocation FHA_ID = TypeMoonAddon.id("matou_sakura_fha");

    private static final String PLAYER_PERSISTED = "PlayerPersisted";
    private static final String SNAPSHOT = "TypeMoonAddonMatouSakuraSnapshot";
    private static final String SNAPSHOT_ACTIVE = "TypeMoonAddonMatouSakuraSnapshotActive";
    private static final String FHA_CHEST_SNAPSHOT = "TypeMoonAddonMatouSakuraFhaChestSnapshot";

    public static void initialize(ServerPlayer player, Variant variant) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        CompoundTag persisted = persistedData(player);
        if (!persisted.getBoolean(SNAPSHOT_ACTIVE)) {
            persisted.put(SNAPSHOT, data.serializeNBT(player.registryAccess()));
            persisted.putBoolean(SNAPSHOT_ACTIVE, true);
            savePersistedData(player, persisted);
        }

        clearTransientState(player);
        data.deserializeNBT(player.registryAccess(), new CompoundTag());
        boolean initialized = TypeMoonIntegration.learnAndGrantAttribute(player);
        data.unlock();
        initialized &= TypeMoonIntegration.assimilateCrestWorm(player);
        if (variant != Variant.STAY_NIGHT) {
            RuleBreakerDispelService.capturePreGrailState(player);
            initialized &= TypeMoonIntegration.performHolyGrailRitual(player);
            initialized &= TypeMoonIntegration.ensureGrailWormPower(player);
        }
        if (variant == Variant.FHA) {
            snapshotFhaChest(player, persisted);
            initialized &= completeErosion(data);
            initialized &= TypeMoonIntegration.ensureGrailWormPower(player);
            initialized &= GrailErosionService.ensureCrestWormExpelled(player);
            initialized &= RuleBreakerDispelService.dispelSilently(player);
            VoidRingRegaliaService.equip(player);
        }
        player.syncData(ModAttachments.IMAGINARY_SPACE.get());

        if (!initialized) {
            TypeMoonAddon.LOGGER.error(
                "Could not initialize the {} Matou Sakura Master Card for {}",
                variant,
                player.getGameProfile().getName()
            );
        }
    }

    public static void restoreAddonState(ServerPlayer player) {
        CompoundTag persisted = persistedData(player);
        if (!persisted.getBoolean(SNAPSHOT_ACTIVE)) {
            return;
        }

        clearTransientState(player);
        if (persisted.contains(SNAPSHOT, Tag.TAG_COMPOUND)) {
            player.getData(ModAttachments.IMAGINARY_SPACE.get()).deserializeNBT(
                player.registryAccess(),
                persisted.getCompound(SNAPSHOT)
            );
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        }
        persisted.remove(SNAPSHOT);
        persisted.remove(SNAPSHOT_ACTIVE);
        if (persisted.contains(FHA_CHEST_SNAPSHOT, Tag.TAG_COMPOUND)) {
            net.minecraft.world.item.ItemStack.parse(player.registryAccess(), persisted.getCompound(FHA_CHEST_SNAPSHOT))
                .ifPresent(stack -> player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, stack));
        } else if (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(ModItems.VOID_RING_REGALIA.get())) {
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        }
        persisted.remove(FHA_CHEST_SNAPSHOT);
        savePersistedData(player, persisted);
    }

    public static Variant variant(String masterId) {
        if (ALTER_ID.toString().equals(masterId)) {
            return Variant.ALTER;
        }
        if (FHA_ID.toString().equals(masterId)) {
            return Variant.FHA;
        }
        return ID.toString().equals(masterId) ? Variant.STAY_NIGHT : null;
    }

    private static boolean completeErosion(io.github.typemoonaddon.data.ImaginarySpaceData data) {
        boolean advanced = data.increaseGrailErosion(1L)
            && data.increaseGrailErosion(1L)
            && data.increaseGrailErosion(1L);
        return advanced && data.completePendingGrailErosion(Long.MAX_VALUE);
    }

    private static void clearTransientState(ServerPlayer player) {
        BlackShadowNightService.playerUnavailable(player);
        ImaginaryStorageService.cancelChannelsForUpgrade(player);
        ImaginaryShadowService.stopForUpgrade(player);
        ShadowMaterializationService.playerLoggedOut(player);
        ShadowTransferService.playerLoggedOut(player);
        SummonBlackMudService.playerDied(player);
        BlackMudControlService.dismissForDispel(player);
        HolyGrailService.dispel(player);
    }

    private static CompoundTag persistedData(ServerPlayer player) {
        return player.getPersistentData().getCompound(PLAYER_PERSISTED).copy();
    }

    private static void savePersistedData(ServerPlayer player, CompoundTag persisted) {
        player.getPersistentData().put(PLAYER_PERSISTED, persisted);
    }

    private static void snapshotFhaChest(ServerPlayer player, CompoundTag persisted) {
        if (persisted.contains(FHA_CHEST_SNAPSHOT, Tag.TAG_COMPOUND)) return;
        var chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!chest.isEmpty() && !chest.is(ModItems.VOID_RING_REGALIA.get())) {
            persisted.put(FHA_CHEST_SNAPSHOT, chest.save(player.registryAccess()));
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
            savePersistedData(player, persisted);
        }
    }

    private MatouSakuraMasterProfile() {
    }

    public enum Variant {
        STAY_NIGHT,
        ALTER,
        FHA
    }
}
