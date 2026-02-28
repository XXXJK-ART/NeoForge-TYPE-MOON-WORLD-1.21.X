package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.network.GemCarvingEngraveMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.GemCarvingTableMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GemCarvingTableScreen extends AbstractContainerScreen<GemCarvingTableMenu> {
    private static final int PANEL_X = 0;
    private static final int PANEL_Y = 0;
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 224;

    private static final int SECTION_GEM_X = 8;
    private static final int SECTION_GEM_Y = 18;
    private static final int SECTION_GEM_W = 56;
    private static final int SECTION_GEM_H = 84;

    private static final int SECTION_MAGIC_X = 70;
    private static final int SECTION_MAGIC_Y = 18;
    private static final int SECTION_MAGIC_W = 124;
    private static final int SECTION_MAGIC_H = 98;

    private static final int SECTION_CTRL_X = 196;
    private static final int SECTION_CTRL_Y = 18;
    private static final int SECTION_CTRL_W = 114;
    private static final int SECTION_CTRL_H = 174;

    private static final int SECTION_INV_X = 8;
    private static final int SECTION_INV_Y = 118;
    private static final int SECTION_INV_W = 172;
    private static final int SECTION_INV_H = 96;
    private static final int ENGRAVE_BUTTON_H = 18;
    private static final int ENGRAVE_BUTTON_BOTTOM_MARGIN = 6;

    private static final int SLOT_GEM_X = 25;
    private static final int SLOT_GEM_Y = 33;
    private static final int SLOT_TOOL_X = 25;
    private static final int SLOT_TOOL_Y = 64;
    private static final int SLOT_SIZE = 18;

    private static final int COLOR_BORDER_DARK = 0xFF1D1D1D;
    private static final int COLOR_BORDER_LIGHT = 0xFF8C8C8C;
    private static final int COLOR_PANEL_OUTER = 0xFF555555;
    private static final int COLOR_PANEL_INNER = 0xFF3F3F3F;
    private static final int COLOR_SECTION_OUTER = 0xFF616161;
    private static final int COLOR_SECTION_INNER = 0xFF4A4A4A;
    private static final int COLOR_SLOT_INNER = 0xFF2C2C2C;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_TEXT_HINT = 0xFFC6C6C6;
    private static final int COLOR_TEXT_WARN = 0xFFFF8080;
    private static final int COLOR_TEXT_OK = 0xFF98E698;

    private record MagicButtonEntry(String magicId, Button button) {
    }

    private final List<MagicButtonEntry> magicButtons = new ArrayList<>();
    private Button engraveButton;
    private Button reinforcementPartButton;
    private Button reinforcementLevelDownButton;
    private Button reinforcementLevelUpButton;
    private Button projectionModeButton;
    private Button projectionTargetPrevButton;
    private Button projectionTargetNextButton;

    private String selectedMagicId;
    private int reinforcementPart = 0;
    private int reinforcementLevel = 1;
    private int projectionMode = GemCarvingTableMenu.PROJECTION_MODE_ITEM;
    private final List<Integer> projectionItemCandidates = new ArrayList<>();
    private final List<String> projectionStructureCandidates = new ArrayList<>();
    private int projectionItemCursor = 0;
    private int projectionStructureCursor = 0;

    public GemCarvingTableScreen(GemCarvingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 320;
        this.imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        this.magicButtons.clear();

        List<String> availableMagics = getAvailableMagics();
        int buttonX = this.leftPos + SECTION_MAGIC_X + 3;
        int buttonY = this.topPos + SECTION_MAGIC_Y + 14;
        int buttonWidth = SECTION_MAGIC_W - 6;
        int buttonHeight = 18;

        for (String magicId : availableMagics) {
            Button button = vanillaButton(buttonX, buttonY, buttonWidth, buttonHeight, Component.empty(), btn -> {
                this.selectedMagicId = magicId;
                updateUiState();
            });
            this.addRenderableWidget(button);
            this.magicButtons.add(new MagicButtonEntry(magicId, button));
            buttonY += 20;
        }

        this.selectedMagicId = availableMagics.isEmpty() ? null : availableMagics.get(0);

        int ctrlButtonX = this.leftPos + SECTION_CTRL_X + 4;
        int ctrlButtonW = SECTION_CTRL_W - 8;
        this.reinforcementPartButton = vanillaButton(ctrlButtonX, this.topPos + SECTION_CTRL_Y + 106, ctrlButtonW, 18, Component.empty(), btn -> {
            this.reinforcementPart = (this.reinforcementPart + 1) % 4;
            updateUiState();
        });
        this.addRenderableWidget(this.reinforcementPartButton);

        int levelRowY = this.topPos + SECTION_CTRL_Y + 128;
        this.reinforcementLevelDownButton = vanillaButton(ctrlButtonX, levelRowY, 20, 18, Component.literal("-"), btn -> {
            this.reinforcementLevel = Math.max(1, this.reinforcementLevel - 1);
            updateUiState();
        });
        this.addRenderableWidget(this.reinforcementLevelDownButton);

        this.reinforcementLevelUpButton = vanillaButton(ctrlButtonX + ctrlButtonW - 20, levelRowY, 20, 18, Component.literal("+"), btn -> {
            this.reinforcementLevel = Math.min(5, this.reinforcementLevel + 1);
            updateUiState();
        });
        this.addRenderableWidget(this.reinforcementLevelUpButton);

        this.projectionModeButton = vanillaButton(ctrlButtonX, this.topPos + SECTION_CTRL_Y + 84, ctrlButtonW, 18, Component.empty(), btn -> {
            this.projectionMode = (this.projectionMode == GemCarvingTableMenu.PROJECTION_MODE_ITEM)
                    ? GemCarvingTableMenu.PROJECTION_MODE_STRUCTURE
                    : GemCarvingTableMenu.PROJECTION_MODE_ITEM;
            updateUiState();
        });
        this.addRenderableWidget(this.projectionModeButton);

        int projectionNavY = this.topPos + SECTION_CTRL_Y + 152;
        this.projectionTargetPrevButton = vanillaButton(ctrlButtonX, projectionNavY, 20, 18, Component.literal("<"), btn -> {
            cycleProjectionTarget(-1);
            updateUiState();
        });
        this.addRenderableWidget(this.projectionTargetPrevButton);

        this.projectionTargetNextButton = vanillaButton(ctrlButtonX + ctrlButtonW - 20, projectionNavY, 20, 18, Component.literal(">"), btn -> {
            cycleProjectionTarget(1);
            updateUiState();
        });
        this.addRenderableWidget(this.projectionTargetNextButton);

        int engraveButtonY = this.topPos + PANEL_H - ENGRAVE_BUTTON_H - ENGRAVE_BUTTON_BOTTOM_MARGIN;
        this.engraveButton = vanillaButton(ctrlButtonX, engraveButtonY, ctrlButtonW, ENGRAVE_BUTTON_H,
                Component.translatable("gui.typemoonworld.gem_carving_table.engrave"), btn -> {
                    if (this.selectedMagicId != null) {
                        PacketDistributor.sendToServer(new GemCarvingEngraveMessage(
                                this.selectedMagicId,
                                this.reinforcementPart,
                                this.reinforcementLevel,
                                this.projectionMode,
                                getSelectedProjectionItemIndex(),
                                getSelectedProjectionStructureId()
                        ));
                    }
                });
        this.addRenderableWidget(this.engraveButton);

        rebuildProjectionCandidates();
        updateUiState();
    }

    private Button vanillaButton(int x, int y, int width, int height, Component text, Button.OnPress onPress) {
        return Button.builder(text, onPress).bounds(x, y, width, height).build();
    }

    private void updateUiState() {
        rebuildProjectionCandidates();

        for (MagicButtonEntry entry : this.magicButtons) {
            boolean selected = entry.magicId.equals(this.selectedMagicId);
            int maxTextWidth = Math.max(8, entry.button.getWidth() - 8);
            entry.button.setMessage(getMagicButtonLabel(entry.magicId, selected, maxTextWidth));
        }

        boolean reinforcementSelected = "reinforcement".equals(this.selectedMagicId);
        boolean projectionSelected = "projection".equals(this.selectedMagicId);

        setWidgetState(this.reinforcementPartButton, reinforcementSelected, true);
        setWidgetState(this.reinforcementLevelDownButton, reinforcementSelected, true);
        setWidgetState(this.reinforcementLevelUpButton, reinforcementSelected, true);
        setWidgetState(this.projectionModeButton, projectionSelected, true);

        boolean hasMultipleTargets = false;
        if (projectionSelected) {
            hasMultipleTargets = this.projectionMode == GemCarvingTableMenu.PROJECTION_MODE_STRUCTURE
                    ? this.projectionStructureCandidates.size() > 1
                    : this.projectionItemCandidates.size() > 1;
        }
        setWidgetState(this.projectionTargetPrevButton, projectionSelected, hasMultipleTargets);
        setWidgetState(this.projectionTargetNextButton, projectionSelected, hasMultipleTargets);

        this.reinforcementPartButton.setMessage(Component.translatable(
                "gui.typemoonworld.gem_carving_table.reinforcement_part",
                GemEngravingService.getReinforcementPartName(this.reinforcementPart)
        ));
        this.projectionModeButton.setMessage(Component.translatable(
                "gui.typemoonworld.gem_carving_table.projection_mode",
                this.projectionMode == GemCarvingTableMenu.PROJECTION_MODE_STRUCTURE
                        ? Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.structure")
                        : Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.item")
        ));
    }

    private void setWidgetState(Button button, boolean visible, boolean activeWhenVisible) {
        button.visible = visible;
        button.active = visible && activeWhenVisible;
    }

    private List<String> getAvailableMagics() {
        List<String> magics = new ArrayList<>();
        if (this.minecraft == null || this.minecraft.player == null) {
            return magics;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = this.minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (hasLearnedMagic(vars, "projection")) {
            magics.add("projection");
        }
        if (hasLearnedMagic(vars, "reinforcement")) {
            magics.add("reinforcement");
        }
        if (hasLearnedMagic(vars, "gravity_magic")) {
            magics.add("gravity_magic");
        }
        return magics;
    }

    private static boolean hasLearnedMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
        if ("reinforcement".equals(magicId)) {
            return vars.learned_magics.contains("reinforcement")
                    || vars.learned_magics.contains("reinforcement_self")
                    || vars.learned_magics.contains("reinforcement_other")
                    || vars.learned_magics.contains("reinforcement_item");
        }
        return vars.learned_magics.contains(magicId);
    }

    private Component getMagicButtonLabel(String magicId, boolean selected, int maxWidth) {
        String name = GemEngravingService.getMagicName(magicId).getString();
        String prefix = selected ? "> " : "";
        String text = prefix + name;
        if (this.font.width(text) <= maxWidth) {
            return Component.literal(text);
        }
        String clipped = this.font.plainSubstrByWidth(text, Math.max(2, maxWidth - this.font.width("..."))) + "...";
        return Component.literal(clipped);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        drawPanel(guiGraphics, x + PANEL_X, y + PANEL_Y, PANEL_W, PANEL_H);
        drawSection(guiGraphics, x + SECTION_GEM_X, y + SECTION_GEM_Y, SECTION_GEM_W, SECTION_GEM_H);
        drawSection(guiGraphics, x + SECTION_MAGIC_X, y + SECTION_MAGIC_Y, SECTION_MAGIC_W, SECTION_MAGIC_H);
        drawSection(guiGraphics, x + SECTION_CTRL_X, y + SECTION_CTRL_Y, SECTION_CTRL_W, SECTION_CTRL_H);
        drawSection(guiGraphics, x + SECTION_INV_X, y + SECTION_INV_Y, SECTION_INV_W, SECTION_INV_H);

        guiGraphics.fill(x + SLOT_GEM_X, y + SLOT_GEM_Y, x + SLOT_GEM_X + SLOT_SIZE, y + SLOT_GEM_Y + SLOT_SIZE, COLOR_SLOT_INNER);
        guiGraphics.fill(x + SLOT_TOOL_X, y + SLOT_TOOL_Y, x + SLOT_TOOL_X + SLOT_SIZE, y + SLOT_TOOL_Y + SLOT_SIZE, COLOR_SLOT_INNER);
        guiGraphics.renderOutline(x + SLOT_GEM_X, y + SLOT_GEM_Y, SLOT_SIZE, SLOT_SIZE, COLOR_BORDER_LIGHT);
        guiGraphics.renderOutline(x + SLOT_TOOL_X, y + SLOT_TOOL_Y, SLOT_SIZE, SLOT_SIZE, COLOR_BORDER_LIGHT);
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        guiGraphics.fill(x, y, x + w, y + h, COLOR_BORDER_DARK);
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, COLOR_PANEL_OUTER);
        guiGraphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, COLOR_PANEL_INNER);
        guiGraphics.fill(x + 2, y + 2, x + w - 2, y + 3, COLOR_BORDER_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + 3, y + h - 2, COLOR_BORDER_LIGHT);
    }

    private void drawSection(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        guiGraphics.fill(x, y, x + w, y + h, COLOR_BORDER_DARK);
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, COLOR_SECTION_OUTER);
        guiGraphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, COLOR_SECTION_INNER);
        guiGraphics.fill(x + 2, y + 2, x + w - 2, y + 3, COLOR_BORDER_LIGHT);
        guiGraphics.fill(x + 2, y + 2, x + 3, y + h - 2, COLOR_BORDER_LIGHT);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = Component.translatable("gui.typemoonworld.gem_carving_table.title");
        int titleX = (this.imageWidth - this.font.width(title)) / 2;
        guiGraphics.drawString(this.font, title, titleX, 6, COLOR_TEXT, false);

        guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.gem_carving_table.gem_slot"), SECTION_GEM_X + 3, SECTION_GEM_Y + 4, COLOR_TEXT_HINT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.gem_carving_table.tool_slot"), SECTION_GEM_X + 3, SECTION_GEM_Y + 35, COLOR_TEXT_HINT, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.gem_carving_table.magic_list"), SECTION_MAGIC_X + 3, SECTION_MAGIC_Y + 4, COLOR_TEXT_HINT, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, SECTION_INV_X + 3, SECTION_INV_Y - 2, COLOR_TEXT_HINT, false);

        if ("reinforcement".equals(this.selectedMagicId)) {
            Component lvl = Component.translatable("gui.typemoonworld.gem_carving_table.reinforcement_level", this.reinforcementLevel);
            int textX = SECTION_CTRL_X + 26 + ((SECTION_CTRL_W - 52) - this.font.width(lvl)) / 2;
            guiGraphics.drawString(this.font, lvl, textX, SECTION_CTRL_Y + 133, COLOR_TEXT, false);
        } else if ("projection".equals(this.selectedMagicId)) {
            guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.gem_carving_table.projection_target"), SECTION_CTRL_X + 4, SECTION_CTRL_Y + 130, COLOR_TEXT, false);
        }

        if (this.selectedMagicId != null && this.minecraft != null && this.minecraft.player != null) {
            GemCarvingTableMenu.EngravePreview preview = this.menu.getPreview(
                    this.minecraft.player,
                    this.selectedMagicId,
                    this.reinforcementPart,
                    this.reinforcementLevel,
                    this.projectionMode,
                    getSelectedProjectionItemIndex(),
                    getSelectedProjectionStructureId()
            );
            boolean hasTool = hasValidToolInput();

            if (preview.valid() && hasTool) {
                this.engraveButton.active = true;
                guiGraphics.drawString(this.font,
                        Component.translatable("gui.typemoonworld.gem_carving_table.success_chance", preview.chance() + "%"),
                        SECTION_CTRL_X + 4, SECTION_CTRL_Y + 28, COLOR_TEXT_OK, false);
                guiGraphics.drawString(this.font,
                        Component.translatable("gui.typemoonworld.gem_carving_table.mana_usage",
                                (int) Math.ceil(preview.requiredMana()),
                                preview.capacity()),
                        SECTION_CTRL_X + 4, SECTION_CTRL_Y + 40, COLOR_TEXT, false);

                if ("projection".equals(this.selectedMagicId)) {
                    String target = preview.projectionMode() == GemCarvingTableMenu.PROJECTION_MODE_STRUCTURE
                            ? preview.projectionStructureName()
                            : preview.projectionItemTemplate().getHoverName().getString();
                    if (target == null || target.isEmpty()) {
                        target = "-";
                    }
                    String clamped = this.font.plainSubstrByWidth(target, SECTION_CTRL_W - 8);
                    guiGraphics.drawString(this.font, Component.literal(clamped), SECTION_CTRL_X + 4, SECTION_CTRL_Y + 142, COLOR_TEXT_HINT, false);
                }
            } else {
                this.engraveButton.active = false;
                guiGraphics.drawString(this.font,
                        Component.translatable("gui.typemoonworld.gem_carving_table.success_chance_invalid"),
                        SECTION_CTRL_X + 4, SECTION_CTRL_Y + 28, COLOR_TEXT_WARN, false);
                Component errorText = hasTool
                        ? Component.translatable(preview.errorKey(), preview.errorArgs())
                        : Component.translatable("message.typemoonworld.gem.engrave.need_tool");
                guiGraphics.drawWordWrap(this.font, errorText, SECTION_CTRL_X + 4, SECTION_CTRL_Y + 40, SECTION_CTRL_W - 8, COLOR_TEXT_WARN);
            }
        } else if (this.engraveButton != null) {
            this.engraveButton.active = false;
        }
    }

    private boolean hasValidToolInput() {
        ItemStack toolStack = this.menu.getSlot(GemCarvingTableMenu.SLOT_TOOL).getItem();
        return !toolStack.isEmpty() && toolStack.getItem() instanceof ChiselItem;
    }

    private void rebuildProjectionCandidates() {
        if (this.minecraft == null || this.minecraft.player == null) {
            this.projectionItemCandidates.clear();
            this.projectionStructureCandidates.clear();
            this.projectionItemCursor = 0;
            this.projectionStructureCursor = 0;
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = this.minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        this.projectionItemCandidates.clear();
        for (int i = 0; i < vars.analyzed_items.size(); i++) {
            ItemStack stack = vars.analyzed_items.get(i);
            if (!stack.isEmpty()) {
                this.projectionItemCandidates.add(i);
            }
        }

        this.projectionStructureCandidates.clear();
        for (TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure : vars.analyzed_structures) {
            if (TypeMoonWorldModVariables.PlayerVariables.isTrustedStructure(structure)) {
                this.projectionStructureCandidates.add(structure.id);
            }
        }

        if (this.projectionItemCandidates.isEmpty()) {
            this.projectionItemCursor = 0;
        } else {
            this.projectionItemCursor = Math.floorMod(this.projectionItemCursor, this.projectionItemCandidates.size());
        }

        if (this.projectionStructureCandidates.isEmpty()) {
            this.projectionStructureCursor = 0;
        } else {
            this.projectionStructureCursor = Math.floorMod(this.projectionStructureCursor, this.projectionStructureCandidates.size());
        }
    }

    private void cycleProjectionTarget(int delta) {
        if (this.projectionMode == GemCarvingTableMenu.PROJECTION_MODE_STRUCTURE) {
            if (this.projectionStructureCandidates.isEmpty()) {
                return;
            }
            this.projectionStructureCursor = Math.floorMod(
                    this.projectionStructureCursor + delta,
                    this.projectionStructureCandidates.size()
            );
            return;
        }

        if (this.projectionItemCandidates.isEmpty()) {
            return;
        }
        this.projectionItemCursor = Math.floorMod(
                this.projectionItemCursor + delta,
                this.projectionItemCandidates.size()
        );
    }

    private int getSelectedProjectionItemIndex() {
        if (this.projectionItemCandidates.isEmpty()) {
            return -1;
        }
        int cursor = Math.floorMod(this.projectionItemCursor, this.projectionItemCandidates.size());
        return this.projectionItemCandidates.get(cursor);
    }

    private String getSelectedProjectionStructureId() {
        if (this.projectionStructureCandidates.isEmpty()) {
            return "";
        }
        int cursor = Math.floorMod(this.projectionStructureCursor, this.projectionStructureCandidates.size());
        return this.projectionStructureCandidates.get(cursor);
    }
}
