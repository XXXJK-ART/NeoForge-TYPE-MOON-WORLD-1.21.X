package com.example.typemoonaddon.item;

import com.example.typemoonaddon.registry.AddonItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class AwakeningCatalystItems {
    public static boolean isCatalyst(Item item) {
        return item == AddonItems.IMAGINARY_PRIMER.get()
                || item == AddonItems.CREST_WORM.get()
                || item == AddonItems.HOLY_GRAIL_FRAGMENT.get();
    }

    public static boolean isAwakened(Player player) {
        return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).is_magus;
    }

    public static boolean requireAwakened(ServerPlayer player) {
        if (isAwakened(player)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.typemoonworld.awaken.hint"), true);
        return false;
    }

    private AwakeningCatalystItems() {
    }
}
