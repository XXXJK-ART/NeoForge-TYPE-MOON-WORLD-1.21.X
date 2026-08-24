package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.engravedworm.EngravedWormAttachments;
import com.example.typemoonaddon.engravedworm.EngravedWormData;
import com.example.typemoonaddon.engravedworm.EngravedWormInventoryData;
import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.worm.WormStackData;
import com.example.typemoonaddon.worm.WormType;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.typemoonworld.api.MasterProfileContext;

/** State and starter inventory for Matou Kariya's worm-based master card. */
public final class MatouKariyaMasterProfile {
    public static final ResourceLocation ID = TypeMoonAddon.id("matou_kariya");
    private static final String PLAYER_PERSISTED = "PlayerPersisted";
    private static final String SNAPSHOT = "TypeMoonAddonMatouKariyaEngravedWormSnapshot";
    private static final UUID ZOUKEN_UUID = UUID.nameUUIDFromBytes("typemoonworld:matou_zouken".getBytes(StandardCharsets.UTF_8));

    private MatouKariyaMasterProfile() {
    }

    public static void initialize(MasterProfileContext context) {
        ServerPlayer player = context.player();
        snapshot(player);
        player.getData(EngravedWormAttachments.INVENTORY.get()).clear();
        com.example.typemoonaddon.engravedworm.EngravedWormService.recalculate(player);
        context.magicKnowledge().learn(WormMagicIntegration.WORM_MAGIC);
        context.magicKnowledge().learn(WormMagicIntegration.WORM_CONTROL);
        context.magicKnowledge().learn(WormMagicIntegration.ENGRAVED_WORM_OPERATION);
        context.magicKnowledge().setProficiency(WormMagicIntegration.WORM_MAGIC, 45.0D);
        context.magicKnowledge().setProficiency(WormMagicIntegration.WORM_CONTROL, 35.0D);
        context.magicKnowledge().setProficiency(WormMagicIntegration.ENGRAVED_WORM_OPERATION, 25.0D);
        SakuraTypeMoonIntegration.grantWaterAttribute(player);
        giveStarterWorms(player);
    }

    public static void snapshot(ServerPlayer player) {
        EngravedWormInventoryData data = player.getData(EngravedWormAttachments.INVENTORY.get());
        CompoundTag persisted = player.getPersistentData().getCompound(PLAYER_PERSISTED).copy();
        persisted.put(SNAPSHOT, data.serializeNBT(player.registryAccess()));
        player.getPersistentData().put(PLAYER_PERSISTED, persisted);
    }

    public static void restore(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(PLAYER_PERSISTED).copy();
        if (!persisted.contains(SNAPSHOT, Tag.TAG_COMPOUND)) {
            return;
        }
        EngravedWormInventoryData data = player.getData(EngravedWormAttachments.INVENTORY.get());
        data.deserializeNBT(player.registryAccess(), persisted.getCompound(SNAPSHOT));
        player.getPersistentData().put(PLAYER_PERSISTED, without(persisted, SNAPSHOT));
        com.example.typemoonaddon.engravedworm.EngravedWormService.recalculate(player);
    }

    public static boolean is(String masterId) {
        return ID.toString().equals(masterId) || "matou_kariya".equals(masterId);
    }

    private static CompoundTag without(CompoundTag source, String key) {
        source.remove(key);
        return source;
    }

    private static void giveStarterWorms(ServerPlayer player) {
        give(player, EngravedWormData.create(AddonItems.ENGRAVED_WORM.get(), 50, ZOUKEN_UUID, "间桐脏砚"));
        giveWormStack(player, WormType.WINGED, 10);
        giveWormStack(player, WormType.WINGED, 10);
        giveWormStack(player, WormType.WINGED, 20);
        giveWormStack(player, WormType.WINGED, 20);
        giveWormStack(player, WormType.WINGED, 30);
        giveWormStack(player, WormType.DETECTION, 15);
        giveWormStack(player, WormType.DETECTION, 20);
        giveWormStack(player, WormType.FIREPROOF, 25);
    }

    private static void giveWormStack(ServerPlayer player, WormType type, int gu) {
        ItemStack stack = WormStackData.create(AddonItems.WORM.get(), type, gu, player.getUUID());
        stack.setCount(16);
        give(player, stack);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
