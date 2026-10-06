package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.GrailOpenPayload;

public final class GrailClientScreens {
    private GrailClientScreens() {}

    public static void open(InteractionHand hand) {
        Minecraft.getInstance().setScreen(hand == null ? new WishScreen() : new FirstScreen());
    }

    private static final class FirstScreen extends Screen {
        FirstScreen() { super(Component.translatable("screen.typemoonworld.grail.first")); }
        @Override protected void init() {
            int x = width / 2 - 100;
            addRenderableWidget(Button.builder(Component.translatable("screen.typemoonworld.grail.third_magic"), b ->
                    PacketDistributor.sendToServer(new GrailOpenPayload(GrailOpenPayload.ACTION_THIRD_MAGIC))).bounds(x, height / 2 - 32, 200, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.typemoonworld.grail.wish"), b ->
                    PacketDistributor.sendToServer(new GrailOpenPayload(GrailOpenPayload.ACTION_WISH_OPEN))).bounds(x, height / 2 + 2, 200, 20).build());
        }
        @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, title, width / 2, height / 2 - 70, 0xFFFFFF);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
        @Override public boolean isPauseScreen() { return false; }
    }

    private static final class WishScreen extends Screen {
        private EditBox input;
        WishScreen() { super(Component.translatable("screen.typemoonworld.grail.wish")); }
        @Override protected void init() {
            input = new EditBox(font, width / 2 - 140, height / 2 - 18, 280, 20, Component.translatable("screen.typemoonworld.grail.input"));
            input.setMaxLength(256);
            addRenderableWidget(input);
            addRenderableWidget(Button.builder(Component.translatable("screen.typemoonworld.grail.wish"), b -> {
                PacketDistributor.sendToServer(new GrailOpenPayload(GrailOpenPayload.ACTION_WISH_EXECUTE, input.getValue()));
                onClose();
            }).bounds(width / 2 - 100, height / 2 + 16, 200, 20).build());
            setInitialFocus(input);
        }
        @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, title, width / 2, height / 2 - 50, 0xFFFFFF);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
