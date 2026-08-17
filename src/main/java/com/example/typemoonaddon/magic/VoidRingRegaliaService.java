package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Creates and equips the FHA master profile's vanishing-cursed full-body regalia. */
public final class VoidRingRegaliaService {
    public static ItemStack create(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(AddonItems.VOID_RING_REGALIA.get());
        ensureVanishingCurse(stack, registries);
        return stack;
    }

    public static void ensureVanishingCurse(ItemStack stack, HolderLookup.Provider registries) {
        var vanishing = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE);
        if (EnchantmentHelper.getItemEnchantmentLevel(vanishing, stack) == 0) {
            stack.enchant(vanishing, 1);
        }
    }

    public static void equip(ServerPlayer player) {
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        data.markCursedArmorRemoved();
        CursedArmorService.sync(player);

        ItemStack equipped = player.getItemBySlot(EquipmentSlot.CHEST);
        if (equipped.is(AddonItems.VOID_RING_REGALIA.get())) {
            ensureVanishingCurse(equipped, player.registryAccess());
            return;
        }
        if (!equipped.isEmpty()) {
            ItemStack displaced = equipped.copy();
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            if (!player.getInventory().add(displaced)) {
                player.drop(displaced, false);
            }
        }
        player.setItemSlot(EquipmentSlot.CHEST, create(player.registryAccess()));
    }

    private VoidRingRegaliaService() {
    }
}
