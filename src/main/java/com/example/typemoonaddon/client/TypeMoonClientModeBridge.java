package com.example.typemoonaddon.client;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.registry.AddonAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class TypeMoonClientModeBridge {
    public static void tick(Minecraft minecraft) {
    }

    public static boolean isShadowMaterializationCastHeld() {
        return false;
    }

    public static boolean isImaginaryStorageCastHeld() {
        return false;
    }

    public static boolean shouldShowProtectionSuffix() {
        Player player = Minecraft.getInstance().player;
        return player != null
                && isCurrentImaginaryStorage(player)
                && player.getData(AddonAttachments.IMAGINARY_SPACE.get()).magicMode() == ImaginarySpaceData.MagicMode.PROTECTION;
    }

    private static boolean isCurrentImaginaryStorage(Player player) {
        TypeMoonWorldModVariables.PlayerVariables variables = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = variables.getCurrentRuntimeWheelEntry();
        if (entry == null) {
            return false;
        }
        ResourceLocation expected = player.getData(AddonAttachments.IMAGINARY_SPACE.get()).crestWormAssimilated()
                ? SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION
                : SakuraTypeMoonIntegration.IMAGINARY_STORAGE;
        return matchesMagicId(entry.magicId, expected);
    }

    private static boolean matchesMagicId(String actualId, ResourceLocation expectedId) {
        return actualId != null && expectedId != null
                && (expectedId.toString().equals(actualId) || expectedId.getPath().equals(actualId));
    }

    private TypeMoonClientModeBridge() {
    }
}
