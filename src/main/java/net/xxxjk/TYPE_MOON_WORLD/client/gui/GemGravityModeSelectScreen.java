package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravity;
import net.xxxjk.TYPE_MOON_WORLD.network.GemGravitySelfCastMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GemGravityModeSelectScreen extends Screen {
    private final int handValue;
    private final List<Component> modes = new ArrayList<>();
    private final List<Integer> modeIds = new ArrayList<>();
    private int selectedIndex = 0;

    private static final ResourceLocation ICON_ULTRA_LIGHT = ResourceLocation.withDefaultNamespace("textures/mob_effect/jump_boost.png");
    private static final ResourceLocation ICON_LIGHT = ResourceLocation.withDefaultNamespace("textures/mob_effect/slow_falling.png");
    private static final ResourceLocation ICON_NORMAL = ResourceLocation.withDefaultNamespace("textures/mob_effect/glowing.png");
    private static final ResourceLocation ICON_HEAVY = ResourceLocation.withDefaultNamespace("textures/mob_effect/slowness.png");
    private static final ResourceLocation ICON_ULTRA_HEAVY = ResourceLocation.withDefaultNamespace("textures/mob_effect/mining_fatigue.png");

    public GemGravityModeSelectScreen(InteractionHand hand) {
        super(Component.translatable("gui.typemoonworld.gem.gravity_select.title"));
        this.handValue = hand == InteractionHand.OFF_HAND ? 1 : 0;
        addMode(MagicGravity.MODE_ULTRA_LIGHT, Component.translatable("gui.typemoonworld.mode.gravity.ultra_light"));
        addMode(MagicGravity.MODE_LIGHT, Component.translatable("gui.typemoonworld.mode.gravity.light"));
        addMode(MagicGravity.MODE_NORMAL, Component.translatable("gui.typemoonworld.mode.gravity.normal"));
        addMode(MagicGravity.MODE_HEAVY, Component.translatable("gui.typemoonworld.mode.gravity.heavy"));
        addMode(MagicGravity.MODE_ULTRA_HEAVY, Component.translatable("gui.typemoonworld.mode.gravity.ultra_heavy"));
    }

    private void addMode(int id, Component component) {
        modeIds.add(id);
        modes.add(component);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            selectedIndex = (selectedIndex - 1 + modes.size()) % modes.size();
        } else if (scrollY < 0) {
            selectedIndex = (selectedIndex + 1) % modes.size();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && updateSelectionAt(mouseX, mouseY)) {
            int mode = modeIds.get(selectedIndex);
            PacketDistributor.sendToServer(new GemGravitySelfCastMessage(this.handValue, 0, mode));
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int itemWidth = 50;
        int itemHeight = 50;
        int gap = 8;
        int totalWidth = (itemWidth * modes.size()) + (gap * (modes.size() - 1));
        int startX = (this.width - totalWidth) / 2;
        int centerY = this.height / 2;
        int startY = centerY - itemHeight / 2;

        int padding = 10;
        int bgX1 = startX - padding;
        int bgY1 = startY - padding - 20;
        int bgX2 = startX + totalWidth + padding;
        int bgY2 = startY + itemHeight + padding;

        guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, 0xB4000000);
        guiGraphics.renderOutline(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 0xFF888888);

        updateSelectionAt(mouseX, mouseY);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, bgY1 + 5, 0xFFFFFF);

        for (int i = 0; i < modes.size(); i++) {
            int x = startX + i * (itemWidth + gap);
            int y = startY;
            boolean selected = i == selectedIndex;

            int fillColor = selected ? 0x60FFFFFF : 0x40000000;
            int textColor = selected ? 0xFFFFFF55 : 0xFFAAAAAA;
            int borderColor = selected ? 0xFFFFFFFF : 0xFF555555;

            guiGraphics.fill(x, y, x + itemWidth, y + itemHeight, fillColor);
            guiGraphics.renderOutline(x, y, itemWidth, itemHeight, borderColor);

            ResourceLocation icon = getIconByMode(modeIds.get(i));
            if (icon != null) {
                int iconSize = 24;
                int iconX = x + (itemWidth - iconSize) / 2;
                int iconY = y + 4;
                RenderSystem.enableBlend();
                guiGraphics.setColor(0.54f, 0.49f, 1.0f, 0.75f);
                guiGraphics.blit(icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
            }

            guiGraphics.drawCenteredString(this.font, modes.get(i), x + itemWidth / 2, y + itemHeight - 10, textColor);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private ResourceLocation getIconByMode(int mode) {
        return switch (mode) {
            case MagicGravity.MODE_ULTRA_LIGHT -> ICON_ULTRA_LIGHT;
            case MagicGravity.MODE_LIGHT -> ICON_LIGHT;
            case MagicGravity.MODE_NORMAL -> ICON_NORMAL;
            case MagicGravity.MODE_HEAVY -> ICON_HEAVY;
            case MagicGravity.MODE_ULTRA_HEAVY -> ICON_ULTRA_HEAVY;
            default -> null;
        };
    }

    private boolean updateSelectionAt(double mouseX, double mouseY) {
        int itemWidth = 50;
        int itemHeight = 50;
        int gap = 8;
        int totalWidth = (itemWidth * modes.size()) + (gap * (modes.size() - 1));
        int startX = (this.width - totalWidth) / 2;
        int centerY = this.height / 2;
        int startY = centerY - itemHeight / 2;

        for (int i = 0; i < modes.size(); i++) {
            int x = startX + i * (itemWidth + gap);
            int y = startY;
            if (mouseX >= x && mouseX < x + itemWidth && mouseY >= y && mouseY < y + itemHeight) {
                selectedIndex = i;
                return true;
            }
        }
        return false;
    }
}
