package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class MatouSakuraMasterProfile {
    public static final ResourceLocation ID = TypeMoonAddon.id("matou_sakura");
    public static final ResourceLocation ALTER_ID = TypeMoonAddon.id("matou_sakura_alter");
    public static final ResourceLocation FHA_ID = TypeMoonAddon.id("matou_sakura_fha");
    private static final String PLAYER_PERSISTED = "PlayerPersisted";
    private static final String SNAPSHOT = "TypeMoonAddonMatouSakuraSnapshot";
    private static final String SNAPSHOT_ACTIVE = "TypeMoonAddonMatouSakuraSnapshotActive";
    private static final String FHA_CHEST_SNAPSHOT = "TypeMoonAddonMatouSakuraFhaChestSnapshot";
    private static final String FHA_CHEST_SNAPSHOT_ACTIVE = "TypeMoonAddonMatouSakuraFhaChestSnapshotActive";

    public static void initialize(ServerPlayer player, Variant variant) {
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        CompoundTag persisted = persistedData(player);
        if (!persisted.getBoolean(SNAPSHOT_ACTIVE)) {
            persisted.put(SNAPSHOT, data.serializeNBT(player.registryAccess()));
            persisted.putBoolean(SNAPSHOT_ACTIVE, true);
            savePersistedData(player, persisted);
        }
        data.deserializeNBT(player.registryAccess(), new CompoundTag());
        SakuraImaginaryStorageService.learnAndGrantAttribute(player);
        SakuraTypeMoonIntegration.grantMasterCardAttributes(player);
        data.unlock();
        if (variant != Variant.STAY_NIGHT) {
            data.assimilateCrestWorm();
            data.ascendGrailWorm();
        }
        if (variant == Variant.FHA) {
            snapshotFhaChest(player, persisted);
            data.increaseGrailErosion(1L);
            data.increaseGrailErosion(1L);
            data.increaseGrailErosion(1L);
            data.completePendingGrailErosion(Long.MAX_VALUE);
            VoidRingRegaliaService.equip(player);
        }
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
    }

    public static void restoreAddonState(ServerPlayer player) {
        CompoundTag persisted = persistedData(player);
        if (!persisted.getBoolean(SNAPSHOT_ACTIVE)) {
            return;
        }
        if (persisted.contains(SNAPSHOT, Tag.TAG_COMPOUND)) {
            player.getData(AddonAttachments.IMAGINARY_SPACE.get()).deserializeNBT(
                    player.registryAccess(),
                    persisted.getCompound(SNAPSHOT)
            );
            AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        }
        if (persisted.getBoolean(FHA_CHEST_SNAPSHOT_ACTIVE)) {
            restoreFhaChest(player, persisted);
        }
        persisted.remove(SNAPSHOT);
        persisted.remove(SNAPSHOT_ACTIVE);
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

    private static CompoundTag persistedData(ServerPlayer player) {
        return player.getPersistentData().getCompound(PLAYER_PERSISTED).copy();
    }

    private static void savePersistedData(ServerPlayer player, CompoundTag persisted) {
        player.getPersistentData().put(PLAYER_PERSISTED, persisted);
    }

    private static void snapshotFhaChest(ServerPlayer player, CompoundTag persisted) {
        if (persisted.getBoolean(FHA_CHEST_SNAPSHOT_ACTIVE)) {
            return;
        }
        CompoundTag chestSnapshot = new CompoundTag();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            chestSnapshot.put("Item", chest.save(player.registryAccess()));
        }
        persisted.put(FHA_CHEST_SNAPSHOT, chestSnapshot);
        persisted.putBoolean(FHA_CHEST_SNAPSHOT_ACTIVE, true);
        savePersistedData(player, persisted);
    }

    private static void restoreFhaChest(ServerPlayer player, CompoundTag persisted) {
        CompoundTag chestSnapshot = persisted.getCompound(FHA_CHEST_SNAPSHOT);
        ItemStack restored = chestSnapshot.contains("Item", Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(player.registryAccess(), chestSnapshot.getCompound("Item"))
                : ItemStack.EMPTY;
        player.setItemSlot(EquipmentSlot.CHEST, restored);
        persisted.remove(FHA_CHEST_SNAPSHOT);
        persisted.remove(FHA_CHEST_SNAPSHOT_ACTIVE);
        savePersistedData(player, persisted);
    }

    public enum Variant {
        STAY_NIGHT,
        ALTER,
        FHA
    }

    private MatouSakuraMasterProfile() {
    }
}
