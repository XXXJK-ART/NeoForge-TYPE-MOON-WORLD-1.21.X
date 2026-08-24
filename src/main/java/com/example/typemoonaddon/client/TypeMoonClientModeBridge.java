package com.example.typemoonaddon.client;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.registry.AddonAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class TypeMoonClientModeBridge {
    public static void tick(Minecraft minecraft) {
    }

    public static boolean isShadowMaterializationCastHeld() {
        Player player = Minecraft.getInstance().player;
        return player != null
                && Minecraft.getInstance().screen == null
                && TypeMoonWorldModKeyMappings.CAST_MAGIC.isDown()
                && isCurrentMagic(player, SakuraTypeMoonIntegration.SHADOW_MATERIALIZATION);
    }

    public static boolean isImaginaryStorageCastHeld() {
        Player player = Minecraft.getInstance().player;
        return player != null
                && Minecraft.getInstance().screen == null
                && TypeMoonWorldModKeyMappings.CAST_MAGIC.isDown()
                && isCurrentImaginaryStorage(player);
    }

    public static boolean shouldShowProtectionSuffix() {
        Player player = Minecraft.getInstance().player;
        return player != null
                && isCurrentImaginaryStorage(player)
                && player.getData(AddonAttachments.IMAGINARY_SPACE.get()).magicMode() == ImaginarySpaceData.MagicMode.PROTECTION;
    }

    private static boolean isCurrentImaginaryStorage(Player player) {
        return isCurrentMagic(player, player.getData(AddonAttachments.IMAGINARY_SPACE.get()).crestWormAssimilated()
                ? SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION
                : SakuraTypeMoonIntegration.IMAGINARY_STORAGE);
    }

    private static boolean isCurrentMagic(Player player, ResourceLocation expected) {
        TypeMoonWorldModVariables.PlayerVariables variables = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        String current = PlayerMagicSelectionService.getCurrentMagicId(variables);
        return matchesMagicId(current, expected);
    }

    private static boolean matchesMagicId(String actualId, ResourceLocation expectedId) {
        return actualId != null && expectedId != null
                && (expectedId.toString().equals(actualId) || expectedId.getPath().equals(actualId));
    }

    private TypeMoonClientModeBridge() {
    }
}
