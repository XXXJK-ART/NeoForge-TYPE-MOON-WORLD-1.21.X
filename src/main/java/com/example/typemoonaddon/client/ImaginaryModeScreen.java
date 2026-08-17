package com.example.typemoonaddon.client;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.network.SetImaginaryModePayload;
import com.example.typemoonaddon.registry.AddonAttachments;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GuiUtils;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.NeonButton;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicWheelSlotEditMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ImaginaryModeScreen extends Screen {
    private static final int WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 124;
    private static final int BUTTON_WIDTH = 76;
    private static final int BUTTON_HEIGHT = 20;
    private static final int STORAGE_COLOR = -13252912;
    private static final int PROTECTION_COLOR = -2707875;
    private static final int DONE_COLOR = -11287157;

    private TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry;
    private ImaginarySpaceData.MagicMode selectedMode = ImaginarySpaceData.MagicMode.STORAGE;
    private NeonButton storageButton;
    private NeonButton protectionButton;

    public ImaginaryModeScreen() {
        super(Component.translatable("screen.typemoonworld.imaginary_mode.title"));
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        TypeMoonWorldModVariables.PlayerVariables variables = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        entry = variables.getCurrentRuntimeWheelEntry();
        if (!validEntry()) {
            onClose();
            return;
        }

        ImaginarySpaceData data = minecraft.player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        selectedMode = data.magicMode();
        int left = width / 2 - 80;
        int buttonY = height / 2 - 12;
        Component storageLabel = data.crestWormAssimilated()
                ? Component.translatable("screen.typemoonworld.imaginary_mode.absorption")
                : Component.translatable("screen.typemoonworld.imaginary_mode.storage");
        storageButton = new NeonButton(left, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, storageLabel,
                button -> select(ImaginarySpaceData.MagicMode.STORAGE), STORAGE_COLOR)
                .setArcaneStyle(true).setSelectedColor(GuiUtils.ARCANE_CYAN);
        storageButton.active = data.crestWormAssimilated() || !data.protectionActive();
        protectionButton = new NeonButton(left + 84, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("screen.typemoonworld.imaginary_mode.protection"),
                button -> select(ImaginarySpaceData.MagicMode.PROTECTION), PROTECTION_COLOR)
                .setArcaneStyle(true).setSelectedColor(GuiUtils.ARCANE_DANGER);
        updateSelectionButtons();
        addRenderableWidget(storageButton);
        addRenderableWidget(protectionButton);
        addRenderableWidget(new NeonButton(left, buttonY + 28, 160, BUTTON_HEIGHT,
                Component.translatable("gui.done"), this::save, DONE_COLOR).setArcaneStyle(true));
    }

    private void select(ImaginarySpaceData.MagicMode mode) {
        selectedMode = mode;
        updateSelectionButtons();
    }

    private void updateSelectionButtons() {
        if (storageButton != null) {
            storageButton.setSelected(selectedMode == ImaginarySpaceData.MagicMode.STORAGE);
        }
        if (protectionButton != null) {
            protectionButton.setSelected(selectedMode == ImaginarySpaceData.MagicMode.PROTECTION);
        }
    }

    private void save(Button ignored) {
        if (!validEntry()) {
            onClose();
            return;
        }
        CompoundTag preset = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload.copy();
        preset.putString(SakuraTypeMoonIntegration.MODE_PRESET_KEY, selectedMode.serializedName());
        PacketDistributor.sendToServer(new SetImaginaryModePayload(selectedMode.ordinal()));
        PacketDistributor.sendToServer(new MagicWheelSlotEditMessage(
                MagicWheelSlotEditMessage.ACTION_SET,
                entry.wheelIndex,
                entry.slotIndex,
                -1,
                entry.sourceType,
                entry.magicId,
                preset,
                entry.crestEntryId,
                entry.displayNameCache
        ));
        onClose();
    }

    private boolean validEntry() {
        if (entry == null || !"self".equals(entry.sourceType) || minecraft == null || minecraft.player == null) {
            return false;
        }
        boolean upgraded = minecraft.player.getData(AddonAttachments.IMAGINARY_SPACE.get()).crestWormAssimilated();
        String expected = (upgraded ? SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION : SakuraTypeMoonIntegration.IMAGINARY_STORAGE).toString();
        return matchesMagicId(entry.magicId, expected);
    }

    static boolean matchesMagicId(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        String expectedPath = expected.contains(":") ? expected.substring(expected.indexOf(':') + 1) : expected;
        return actual.equals(expected) || actual.equals(expectedPath);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiUtils.renderScreenBackdrop(graphics, width, height);
        int windowX = width / 2 - WINDOW_WIDTH / 2;
        int windowY = height / 2 - 70;
        GuiUtils.renderArcaneWindow(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, STORAGE_COLOR);
        graphics.drawCenteredString(font, title, width / 2, windowY + 9, GuiUtils.ARCANE_TEXT);
        graphics.drawCenteredString(font, Component.translatable("screen.typemoonworld.imaginary_mode.hint"),
                width / 2, height / 2 - 34, GuiUtils.ARCANE_TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
