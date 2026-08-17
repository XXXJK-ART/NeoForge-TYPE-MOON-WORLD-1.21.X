package com.example.typemoonaddon.client;

import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.network.SetShadowAttackPayload;
import com.example.typemoonaddon.network.SetShadowCommandModePayload;
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

public final class BlackMudControlScreen extends Screen {
    private static final int WINDOW_WIDTH = 220;
    private static final int WINDOW_HEIGHT = 220;
    private static final int BUTTON_WIDTH = 92;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ACCENT_COLOR = -13252912;
    private static final int DISMISS_COLOR = -2707875;
    private static final int DONE_COLOR = -11287157;

    private final Map<ImaginarySpaceData.ShadowCommandMode, NeonButton> modeButtons = new EnumMap<>(ImaginarySpaceData.ShadowCommandMode.class);
    private TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry;
    private ImaginarySpaceData.ShadowCommandMode selectedMode = ImaginarySpaceData.ShadowCommandMode.FREE;
    private ImaginarySpaceData.ShadowCommandMode highlightedMode = ImaginarySpaceData.ShadowCommandMode.FREE;
    private boolean attackAroundEnabled;

    public BlackMudControlScreen() {
        super(Component.translatable("screen.typemoonworld.black_mud_control.title"));
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
        selectedMode = data.selectedShadowCommandMode();
        if (selectedMode == ImaginarySpaceData.ShadowCommandMode.ATTACK_AROUND) {
            selectedMode = data.activeShadowCommandMode();
        }
        highlightedMode = selectedMode;
        attackAroundEnabled = data.shadowAttackAround();
        int left = width / 2 - 96;
        int top = height / 2 - 62;
        addModeButton(ImaginarySpaceData.ShadowCommandMode.FREE, left, top);
        addModeButton(ImaginarySpaceData.ShadowCommandMode.SPREAD, left + 100, top);
        addModeButton(ImaginarySpaceData.ShadowCommandMode.HOLD, left, top + 28);
        addModeButton(ImaginarySpaceData.ShadowCommandMode.GATHER, left + 100, top + 28);
        addModeButton(ImaginarySpaceData.ShadowCommandMode.ATTACK_AROUND, left, top + 56);
        addModeButton(ImaginarySpaceData.ShadowCommandMode.HUNT, left + 100, top + 56);
        addModeButton(ImaginarySpaceData.ShadowCommandMode.DISMISS, left + 50, top + 84);
        updateSelectionButtons();
        addRenderableWidget(new NeonButton(left, top + 116, 192, BUTTON_HEIGHT,
                Component.translatable("gui.done"), this::save, DONE_COLOR).setArcaneStyle(true));
    }

    private void addModeButton(ImaginarySpaceData.ShadowCommandMode mode, int x, int y) {
        Component label = mode == ImaginarySpaceData.ShadowCommandMode.ATTACK_AROUND
                ? Component.translatable("screen.typemoonworld.black_mud_control.attack_around_" + (attackAroundEnabled ? "enabled" : "disabled"))
                : Component.translatable("screen.typemoonworld.black_mud_control." + mode.serializedName());
        NeonButton button = new NeonButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, label, ignored -> select(mode),
                mode == ImaginarySpaceData.ShadowCommandMode.DISMISS ? DISMISS_COLOR : ACCENT_COLOR)
                .setArcaneStyle(true).setSelectedColor(GuiUtils.ARCANE_CYAN);
        modeButtons.put(mode, button);
        addRenderableWidget(button);
    }

    private void select(ImaginarySpaceData.ShadowCommandMode mode) {
        if (mode == ImaginarySpaceData.ShadowCommandMode.ATTACK_AROUND) {
            attackAroundEnabled = !attackAroundEnabled;
            highlightedMode = ImaginarySpaceData.ShadowCommandMode.ATTACK_AROUND;
            PacketDistributor.sendToServer(new SetShadowAttackPayload(attackAroundEnabled));
            NeonButton button = modeButtons.get(ImaginarySpaceData.ShadowCommandMode.ATTACK_AROUND);
            if (button != null) {
                button.setMessage(Component.translatable("screen.typemoonworld.black_mud_control.attack_around_" + (attackAroundEnabled ? "enabled" : "disabled")));
            }
            updateSelectionButtons();
            return;
        }
        selectedMode = mode;
        highlightedMode = mode;
        updateSelectionButtons();
    }

    private void updateSelectionButtons() {
        modeButtons.forEach((mode, button) -> button.setSelected(mode == highlightedMode));
    }

    private void save(Button ignored) {
        if (!validEntry()) {
            onClose();
            return;
        }
        CompoundTag preset = entry.presetPayload == null ? new CompoundTag() : entry.presetPayload.copy();
        preset.putString(SakuraTypeMoonIntegration.SHADOW_COMMAND_PRESET_KEY, selectedMode.serializedName());
        PacketDistributor.sendToServer(new SetShadowCommandModePayload(selectedMode.ordinal()));
        PacketDistributor.sendToServer(new MagicWheelSlotEditMessage(MagicWheelSlotEditMessage.ACTION_SET,
                entry.wheelIndex, entry.slotIndex, -1, entry.sourceType, entry.magicId, preset,
                entry.crestEntryId, entry.displayNameCache));
        onClose();
    }

    private boolean validEntry() {
        return entry != null && "self".equals(entry.sourceType)
                && ImaginaryModeScreen.matchesMagicId(entry.magicId, SakuraTypeMoonIntegration.BLACK_MUD_CONTROL.toString());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiUtils.renderScreenBackdrop(graphics, width, height);
        int windowX = width / 2 - WINDOW_WIDTH / 2;
        int windowY = height / 2 - 117;
        GuiUtils.renderArcaneWindow(graphics, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, ACCENT_COLOR);
        graphics.drawCenteredString(font, title, width / 2, windowY + 9, GuiUtils.ARCANE_TEXT);
        graphics.drawCenteredString(font, Component.translatable("screen.typemoonworld.black_mud_control.hint"),
                width / 2, windowY + 29, GuiUtils.ARCANE_TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
