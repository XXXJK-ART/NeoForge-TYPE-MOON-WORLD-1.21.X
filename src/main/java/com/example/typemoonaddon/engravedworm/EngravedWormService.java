package com.example.typemoonaddon.engravedworm;

import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.worm.WormStackData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class EngravedWormService {
    public static final int PAGE_SIZE = 54;
    public static final int MINIMUM_GU_POWER = 51;
    public static final double HEALTH_PER_WORM = 10.0D;
    private static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("typemoonworld", "engraved_worm_health");

    private EngravedWormService() {
    }

    public static ItemStack createFromWorm(ServerPlayer player, ItemStack source) {
        if (player == null || source.isEmpty() || !source.is(AddonItems.WORM.get())) {
            return ItemStack.EMPTY;
        }
        int gu = WormStackData.gu(source);
        if (gu < MINIMUM_GU_POWER) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.engraved_worm.gu_too_low", MINIMUM_GU_POWER), true);
            return ItemStack.EMPTY;
        }
        UUID owner = WormStackData.owner(source);
        if (owner != null && !owner.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.engraved_worm.owner_only"), true);
            return ItemStack.EMPTY;
        }
        return EngravedWormData.create(AddonItems.ENGRAVED_WORM.get(), gu, player.getUUID());
    }

    public static boolean insert(ServerPlayer player, int slot, ItemStack stack) {
        if (player == null || slot < 0 || stack.isEmpty() || !stack.is(AddonItems.ENGRAVED_WORM.get())
                || EngravedWormData.owner(stack) == null) {
            return false;
        }
        EngravedWormInventoryData data = player.getData(EngravedWormAttachments.INVENTORY.get());
        if (!data.get(slot).isEmpty()) {
            return false;
        }
        data.set(slot, stack);
        recalculate(player);
        return true;
    }

    public static ItemStack remove(ServerPlayer player, int slot) {
        if (player == null || slot < 0) {
            return ItemStack.EMPTY;
        }
        EngravedWormInventoryData data = player.getData(EngravedWormAttachments.INVENTORY.get());
        ItemStack existing = data.get(slot);
        if (existing.isEmpty() || !EngravedWormData.isBoundTo(existing, player.getUUID())) {
            return ItemStack.EMPTY;
        }
        data.set(slot, ItemStack.EMPTY);
        recalculate(player);
        return existing;
    }

    public static boolean hasWormOwnedBy(ServerPlayer host, UUID owner) {
        if (host == null || owner == null) {
            return false;
        }
        EngravedWormInventoryData data = host.getData(EngravedWormAttachments.INVENTORY.get());
        for (int slot = 0; slot < data.size(); slot++) {
            if (EngravedWormData.isBoundTo(data.get(slot), owner)) {
                return true;
            }
        }
        return false;
    }

    public static int totalGuOwnedBy(ServerPlayer host, UUID owner) {
        if (host == null || owner == null) {
            return 0;
        }
        int total = 0;
        EngravedWormInventoryData data = host.getData(EngravedWormAttachments.INVENTORY.get());
        for (int slot = 0; slot < data.size(); slot++) {
            ItemStack stack = data.get(slot);
            if (EngravedWormData.isBoundTo(stack, owner)) {
                total += EngravedWormData.gu(stack);
            }
        }
        return total;
    }

    /** Removes every body worm belonging to {@code owner}; the caller chooses where the returned worms go. */
    public static List<ItemStack> removeOwnedBy(ServerPlayer host, UUID owner) {
        List<ItemStack> removed = new ArrayList<>();
        if (host == null || owner == null) {
            return removed;
        }
        EngravedWormInventoryData data = host.getData(EngravedWormAttachments.INVENTORY.get());
        for (int slot = data.size() - 1; slot >= 0; slot--) {
            ItemStack stack = data.get(slot);
            if (EngravedWormData.isBoundTo(stack, owner)) {
                removed.add(stack.copyWithCount(1));
                data.set(slot, ItemStack.EMPTY);
            }
        }
        if (!removed.isEmpty()) {
            recalculate(host);
        }
        return removed;
    }

    public static int maxPage(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, (Math.max(1, player.getData(EngravedWormAttachments.INVENTORY.get()).size()) - 1) / PAGE_SIZE);
    }

    public static void recalculate(ServerPlayer player) {
        if (player == null) {
            return;
        }
        EngravedWormInventoryData data = player.getData(EngravedWormAttachments.INVENTORY.get());
        int count = 0;
        double manaBonus = 0.0D;
        for (int slot = 0; slot < data.size(); slot++) {
            ItemStack stack = data.get(slot);
            if (!stack.isEmpty() && stack.is(AddonItems.ENGRAVED_WORM.get())) {
                count++;
                manaBonus += EngravedWormData.gu(stack) * 2.0D;
            }
        }

        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            AttributeModifier old = health.getModifier(HEALTH_MODIFIER_ID);
            double penalty = -HEALTH_PER_WORM * count;
            if (old != null && Double.compare(old.amount(), penalty) != 0) {
                health.removeModifier(old);
            }
            if (penalty != 0.0D && health.getModifier(HEALTH_MODIFIER_ID) == null) {
                health.addTransientModifier(new AttributeModifier(
                        HEALTH_MODIFIER_ID, penalty, AttributeModifier.Operation.ADD_VALUE));
            }
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double previousBonus = data.getAppliedManaBonus();
        double baseMaximum = Math.max(0.0D, vars.player_max_mana - previousBonus);
        vars.player_max_mana = baseMaximum + manaBonus;
        vars.player_mana = Math.min(Math.max(0.0D, vars.player_mana), vars.player_max_mana);
        data.setAppliedManaBonus(manaBonus);
        vars.syncMana(player);
    }

    public static void repairAfterLoad(ServerPlayer player) {
        recalculate(player);
    }
}
