package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.registry.AddonAttachments;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class MatouSakuraMasterProfile {
    public static final ResourceLocation ID = TypeMoonAddon.id("matou_sakura");
    public static final ResourceLocation ALTER_ID = TypeMoonAddon.id("matou_sakura_alter");
    public static final ResourceLocation FHA_ID = TypeMoonAddon.id("matou_sakura_fha");
    private static final String PLAYER_PERSISTED = "PlayerPersisted";
    private static final String SNAPSHOT = "TypeMoonAddonMatouSakuraSnapshot";
    private static final String SNAPSHOT_ACTIVE = "TypeMoonAddonMatouSakuraSnapshotActive";

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
        data.unlock();
        if (variant != Variant.STAY_NIGHT) {
            data.assimilateCrestWorm();
            data.ascendGrailWorm();
        }
        if (variant == Variant.FHA) {
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

    public enum Variant {
        STAY_NIGHT,
        ALTER,
        FHA
    }

    private MatouSakuraMasterProfile() {
    }
}
