package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;

public class MagicJewelRelease {
    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (MagicJewelShoot.tryUseHeldFullGem(player)) return;
        player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_ANY_GEM_NEED), true);
    }
}
