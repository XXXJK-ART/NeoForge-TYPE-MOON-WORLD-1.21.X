package io.github.typemoonaddon.client;

import io.github.typemoonaddon.data.ImaginarySpaceData.MagicMode;
import io.github.typemoonaddon.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.typemoonworld.api.ClientExtensionRegistry;

/** Client boundary that intentionally uses no Type Moon World implementation classes. */
public final class TypeMoonClientModeBridge {
    public static boolean registerOptions(ClientExtensionRegistry registry) {
        return registry != null;
    }

    public static void tick(Minecraft minecraft) {
    }

    public static boolean shouldShowProtectionSuffix() {
        Player player = Minecraft.getInstance().player;
        return player != null
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).magicMode() == MagicMode.PROTECTION;
    }

    public static boolean isImaginaryStorageCastHeld() {
        return false;
    }

    public static boolean isShadowMaterializationCastHeld() {
        return false;
    }

    private TypeMoonClientModeBridge() {
    }
}
