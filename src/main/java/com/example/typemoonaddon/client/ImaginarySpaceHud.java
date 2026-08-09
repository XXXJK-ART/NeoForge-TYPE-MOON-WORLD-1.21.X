package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceData;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class ImaginarySpaceHud {
    private ImaginarySpaceHud() {
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        ImaginarySpaceData data = minecraft.player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        if (!data.active()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 8;
        int y = 8;
        graphics.fill(x - 3, y - 3, x + 152, y + 34, 0x78090718);
        graphics.drawString(minecraft.font,
                Component.translatable("hud.typemoonworld.imaginary_space.existence",
                        format(data.existence())), x, y, 0xFFD4A8FF, false);
        graphics.drawString(minecraft.font,
                Component.translatable("hud.typemoonworld.imaginary_space.depth",
                        format(data.depth())), x, y + 11, 0xFF9F7BDB, false);
        graphics.drawString(minecraft.font,
                Component.translatable("hud.typemoonworld.imaginary_space.time_offset",
                        format(data.timeOffset())), x, y + 22, 0xFFB7A4D8, false);
    }

    private static String format(double value) {
        double safe = Double.isFinite(value) ? value : 0.0D;
        return String.format(Locale.ROOT, "%.1f", safe);
    }
}
