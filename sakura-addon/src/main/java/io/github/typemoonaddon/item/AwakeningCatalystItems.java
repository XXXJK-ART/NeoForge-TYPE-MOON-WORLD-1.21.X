package io.github.typemoonaddon.item;

import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Public-API-only progression check for addon catalysts. */
public final class AwakeningCatalystItems {
    public static boolean isCatalyst(Item item) {
        return item == ModItems.IMAGINARY_PRIMER.get()
            || item == ModItems.CREST_WORM.get()
            || item == ModItems.HOLY_GRAIL_FRAGMENT.get()
            || item == ModItems.SHADOW_FAMILIAR_SPAWN_EGG.get()
            || item == ModItems.BLACK_SHADOW_SPAWN_EGG.get();
    }

    public static boolean isAwakened(Player player) {
        return player.getData(ModAttachments.IMAGINARY_SPACE.get()).learned()
            || !TypeMoonWorldApi.magicAttributes(player).attributes().isEmpty();
    }

    public static boolean requireAwakened(ServerPlayer player) {
        if (isAwakened(player)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.typemoonaddon.awaken.hint"), true);
        return false;
    }

    private AwakeningCatalystItems() {
    }
}
