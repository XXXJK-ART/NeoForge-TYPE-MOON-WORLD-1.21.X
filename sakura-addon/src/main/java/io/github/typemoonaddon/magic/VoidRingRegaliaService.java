package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModItems;
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
        ItemStack stack = new ItemStack(ModItems.VOID_RING_REGALIA.get());
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
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        data.markCursedArmorRemoved();
        CursedArmorService.sync(player);

        ItemStack equipped = player.getItemBySlot(EquipmentSlot.CHEST);
        if (equipped.is(ModItems.VOID_RING_REGALIA.get())) {
            ensureVanishingCurse(equipped, player.registryAccess());
            return;
        }
        player.setItemSlot(EquipmentSlot.CHEST, create(player.registryAccess()));
    }

    private VoidRingRegaliaService() {
    }
}
