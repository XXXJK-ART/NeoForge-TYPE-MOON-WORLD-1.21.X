package com.example.typemoonaddon.client;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.network.SetShadowArtModePayload;
import com.example.typemoonaddon.registry.AddonAttachments;
import java.util.EnumMap;
import java.util.Map;
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

public final class ShadowArtModeScreen extends Screen {
    private static final int WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 148;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ACCENT_COLOR = -13252912;
    private static final int DONE_COLOR = -11287157;

    private final Map<ImaginarySpaceData.ShadowArtMode, NeonButton> modeButtons = new EnumMap<>(ImaginarySpaceData.ShadowArtMode.class);
    private TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry;
    private ImaginarySpaceData.ShadowArtMode selectedMode = ImaginarySpaceData.ShadowArtMode.BALANCED;

    public ShadowArtModeScreen() {
        super(Component.translatable("screen.typemoonworld.shadow_art.title"));
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
        selectedMode = minecraft.player.getData(AddonAttachments.IMAGINARY_SPACE.get()).shadowArtMode();
        int left = width / 2 - BUTTON_WIDTH / 2;
        int top = height / 2 - 44;
        for (ImaginarySpaceData.ShadowArtMode mode : ImaginarySpaceData.ShadowArtMode.values()) {
            addModeButton(mode, left, top);
            top += 25;
        }
        updateSelectionButtons();
        addRenderableWidget(new NeonButton(left, top + 4, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("gui.done"), this::save, DONE_COLOR).setArcaneStyle(true));
    }

    private void addModeButton(ImaginarySpaceData.ShadowArtMode mode, int x, int y) {
        NeonButton button = new NeonButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("screen.typemoonworld.shadow_art." + mode.serializedName()),
                ignored -> select(mode), ACCENT_COLOR).setArcaneStyle(true).setSelectedColor(GuiUtils.ARCANE_CYAN);
        modeButtons.put(mode, button);
        addRenderableWidget(button);
    }

    private void select(ImaginarySpaceData.ShadowArtMode mode) {
        selectedMode = mode;
        updateSelectionButtons();
    }

    private void updateSelectionButtons() {
        modeButtons.forEach((mode, button) -> button.setSelected(mode == selectedMode));
    }

    private void save(Button ignored) {
        if (!validEntry()) {
            onClose();
            return;
        }
        CompoundTag preset = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload.copy();
        preset.putString(SakuraTypeMoonIntegration.SHADOW_ART_MODE_PRESET_KEY, selectedMode.serializedName());
        PacketDistributor.sendToServer(new SetShadowArtModePayload(selectedMode.ordinal()));
        PacketDistributor.sendToServer(new MagicWheelSlotEditMessage(MagicWheelSlotEditMessage.ACTION_SET,
                entry.wheelIndex, entry.slotIndex, -1, entry.sourceType, entry.magicId, preset,
                entry.crestEntryId, entry.displayNameCache));
        onClose();
    }

    private boolean validEntry() {
        return entry != null && "self".equals(entry.sourceType)
                && ImaginaryModeScreen.matchesMagicId(entry.magicId, SakuraTypeMoonIntegration.SHADOW_ART.toString());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiUtils.renderScreenBackdrop(graphics, width, height);
        int windowX = width / 2 - WINDOW_WIDTH / 2;
        int windowY = height / 2 - 76;
        GuiUtils.renderArcaneWindow(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, ACCENT_COLOR);
        graphics.drawCenteredString(font, title, width / 2, windowY + 9, GuiUtils.ARCANE_TEXT);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
