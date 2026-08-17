package com.example.typemoonaddon.client;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.network.SetBlackMudSummonModePayload;
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

public final class BlackMudSummonModeScreen extends Screen {
    private static final int WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 124;
    private static final int BUTTON_WIDTH = 76;
    private static final int BUTTON_HEIGHT = 20;
    private static final int RELEASE_COLOR = -13252912;
    private static final int DISMISS_COLOR = -2707875;
    private static final int DONE_COLOR = -11287157;

    private TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry;
    private ImaginarySpaceData.BlackMudSummonMode selectedMode = ImaginarySpaceData.BlackMudSummonMode.RELEASE;
    private NeonButton releaseButton;
    private NeonButton dismissButton;

    public BlackMudSummonModeScreen() {
        super(Component.translatable("screen.typemoonworld.summon_black_mud.title"));
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
        selectedMode = minecraft.player.getData(AddonAttachments.IMAGINARY_SPACE.get()).blackMudSummonMode();
        int left = width / 2 - 80;
        int buttonY = height / 2 - 12;
        releaseButton = new NeonButton(left, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("screen.typemoonworld.summon_black_mud.release"),
                button -> select(ImaginarySpaceData.BlackMudSummonMode.RELEASE), RELEASE_COLOR)
                .setArcaneStyle(true).setSelectedColor(GuiUtils.ARCANE_CYAN);
        dismissButton = new NeonButton(left + 84, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("screen.typemoonworld.summon_black_mud.dismiss"),
                button -> select(ImaginarySpaceData.BlackMudSummonMode.DISMISS), DISMISS_COLOR)
                .setArcaneStyle(true).setSelectedColor(GuiUtils.ARCANE_DANGER);
        updateSelectionButtons();
        addRenderableWidget(releaseButton);
        addRenderableWidget(dismissButton);
        addRenderableWidget(new NeonButton(left, buttonY + 28, 160, BUTTON_HEIGHT,
                Component.translatable("gui.done"), this::save, DONE_COLOR).setArcaneStyle(true));
    }

    private void select(ImaginarySpaceData.BlackMudSummonMode mode) {
        selectedMode = mode;
        updateSelectionButtons();
    }

    private void updateSelectionButtons() {
        if (releaseButton != null) {
            releaseButton.setSelected(selectedMode == ImaginarySpaceData.BlackMudSummonMode.RELEASE);
        }
        if (dismissButton != null) {
            dismissButton.setSelected(selectedMode == ImaginarySpaceData.BlackMudSummonMode.DISMISS);
        }
    }

    private void save(Button ignored) {
        if (!validEntry()) {
            onClose();
            return;
        }
        CompoundTag preset = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload.copy();
        preset.putString(SakuraTypeMoonIntegration.BLACK_MUD_SUMMON_PRESET_KEY, selectedMode.serializedName());
        PacketDistributor.sendToServer(new SetBlackMudSummonModePayload(selectedMode.ordinal()));
        PacketDistributor.sendToServer(new MagicWheelSlotEditMessage(MagicWheelSlotEditMessage.ACTION_SET,
                entry.wheelIndex, entry.slotIndex, -1, entry.sourceType, entry.magicId, preset,
                entry.crestEntryId, entry.displayNameCache));
        onClose();
    }

    private boolean validEntry() {
        return entry != null && "self".equals(entry.sourceType)
                && ImaginaryModeScreen.matchesMagicId(entry.magicId, SakuraTypeMoonIntegration.SUMMON_BLACK_MUD.toString());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiUtils.renderScreenBackdrop(graphics, width, height);
        int windowX = width / 2 - WINDOW_WIDTH / 2;
        int windowY = height / 2 - 70;
        GuiUtils.renderArcaneWindow(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, RELEASE_COLOR);
        graphics.drawCenteredString(font, title, width / 2, windowY + 9, GuiUtils.ARCANE_TEXT);
        graphics.drawCenteredString(font, Component.translatable("screen.typemoonworld.summon_black_mud.hint"),
                width / 2, height / 2 - 34, GuiUtils.ARCANE_TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
