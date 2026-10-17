package com.captain.servant.client;

import com.captain.servant.lancelot.KnightArmoryEntry;
import com.captain.servant.network.payload.KnightArmorySelectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class KnightArmoryScreen extends Screen {
    private final int unlockedMask;

    private KnightArmoryScreen(int unlockedMask) {
        super(Component.translatable("screen.fatesupport.knight_armory.title"));
        this.unlockedMask = unlockedMask;
    }

    public static void open(int unlockedMask) {
        Minecraft.getInstance().setScreen(new KnightArmoryScreen(unlockedMask));
    }

    @Override
    protected void init() {
        int visible = 0;
        for (KnightArmoryEntry entry : KnightArmoryEntry.values()) {
            if ((unlockedMask & entry.bit()) != 0) {
                visible++;
            }
        }
        int y = Math.max(44, height / 2 - visible * 13);
        for (KnightArmoryEntry entry : KnightArmoryEntry.values()) {
            if ((unlockedMask & entry.bit()) == 0) {
                continue;
            }
            Component name = Component.translatable("screen.fatesupport.knight_armory.entry." + entry.key());
            addRenderableWidget(Button.builder(name, button -> {
                PacketDistributor.sendToServer(new KnightArmorySelectPayload(entry.networkId()));
                onClose();
            }).bounds(width / 2 - 90, y, 180, 20).build());
            y += 26;
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);
        gui.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
