package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectProjectionItemMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectProjectionStructureMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"null", "unused", "unchecked"})
public class ProjectionPresetScreen extends Screen {
    private static final int LIST_X_OFFSET = 10;
    private static final int LIST_Y_OFFSET = 40;
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 18;
    private static final int ROWS = 8;
    private static final long STRUCTURE_DELETE_HOLD_MS = 700L;

    private final Player player;
    private int leftPos;
    private int topPos;
    private int imageWidth = 360;
    private int imageHeight = 200;
    private float scrollOffs = 0;
    private int startIndex = 0;
    private boolean scrolling;

    private String currentFilter = "all";
    private final List<Button> filterButtons = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();
    private List<TypeMoonWorldModVariables.PlayerVariables.SavedStructure> filteredStructures = new ArrayList<>();

    private double lastMouseX;
    private double lastMouseY;
    private String holdingStructureId = null;
    private String holdingStructureName = "";
    private long holdStartMs = 0L;
    private boolean holdDialogOpened = false;

    public ProjectionPresetScreen(Player player) {
        super(Component.translatable("gui.typemoonworld.projection.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        initFilterButtons();
        updateFilteredItems();
    }

    @Override
    public void tick() {
        super.tick();
        if (isStructureFilter()) {
            updateFilteredItems();
        }
        checkStructureDeleteLongPress();
    }

    private void initFilterButtons() {
        int btnY = topPos + 5;
        int btnH = 20;
        int startX = leftPos + 10;
        int gap = 2;

        String[] filters = {"all", "structure", "noble_phantasm", "combat", "tools", "building", "misc", "special"};
        String[] labels = {
                "gui.typemoonworld.projection.filter.all",
                "gui.typemoonworld.projection.filter.structure",
                "gui.typemoonworld.projection.filter.noble_phantasm",
                "gui.typemoonworld.projection.filter.combat",
                "gui.typemoonworld.projection.filter.tools",
                "gui.typemoonworld.projection.filter.building",
                "gui.typemoonworld.projection.filter.misc",
                "gui.typemoonworld.projection.filter.special"
        };

        int currentX = startX;
        for (int i = 0; i < filters.length; i++) {
            String filter = filters[i];
            String labelKey = labels[i];
            int width = Minecraft.getInstance().font.width(Component.translatable(labelKey)) + 10;
            if (width < 40) width = 40;

            NeonButton btn = new NeonButton(currentX, btnY, width, btnH, Component.translatable(labelKey), b -> {
                this.currentFilter = filter;
                this.scrollOffs = 0;
                this.startIndex = 0;
                updateFilteredItems();
            }, 0xFF00FFFF);

            this.addRenderableWidget(btn);
            filterButtons.add(btn);
            currentX += width + gap;
        }
    }

    private void updateFilteredItems() {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        if (isStructureFilter()) {
            filteredStructures = vars.analyzed_structures.stream()
                    .filter(TypeMoonWorldModVariables.PlayerVariables::isTrustedStructure)
                    .map(TypeMoonWorldModVariables.PlayerVariables.SavedStructure::copy)
                    .collect(Collectors.toList());
            filteredItems = new ArrayList<>();
            clampScrollState();
            return;
        }

        List<ItemStack> allItems = vars.analyzed_items;
        filteredItems = allItems.stream().filter(stack -> {
            if ("all".equals(currentFilter)) return true;

            if ("noble_phantasm".equals(currentFilter)) {
                return stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
            }

            if ("special".equals(currentFilter)) {
                boolean isEnchanted = stack.isEnchanted();
                boolean isRenamed = stack.has(DataComponents.CUSTOM_NAME);
                return isEnchanted || isRenamed;
            }

            if ("combat".equals(currentFilter)) {
                return stack.is(ItemTags.SWORDS) ||
                        stack.is(ItemTags.AXES) ||
                        stack.getItem() instanceof net.minecraft.world.item.BowItem ||
                        stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
                        stack.getItem() instanceof net.minecraft.world.item.ShieldItem ||
                        stack.getItem() instanceof net.minecraft.world.item.ArmorItem ||
                        stack.getItem() instanceof net.minecraft.world.item.TridentItem;
            }

            if ("tools".equals(currentFilter)) {
                return stack.is(ItemTags.PICKAXES) ||
                        stack.is(ItemTags.SHOVELS) ||
                        stack.is(ItemTags.HOES) ||
                        stack.getItem() instanceof net.minecraft.world.item.ShearsItem ||
                        stack.getItem() instanceof net.minecraft.world.item.FlintAndSteelItem ||
                        stack.getItem() instanceof net.minecraft.world.item.FishingRodItem;
            }

            if ("building".equals(currentFilter)) {
                return stack.getItem() instanceof net.minecraft.world.item.BlockItem;
            }

            if ("misc".equals(currentFilter)) {
                boolean isCombat = stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) ||
                        stack.getItem() instanceof net.minecraft.world.item.BowItem ||
                        stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
                        stack.getItem() instanceof net.minecraft.world.item.ShieldItem ||
                        stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
                boolean isTool = stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES);
                boolean isBlock = stack.getItem() instanceof net.minecraft.world.item.BlockItem;
                boolean isNP = stack.getItem() instanceof net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
                return !isCombat && !isTool && !isBlock && !isNP;
            }

            return true;
        }).collect(Collectors.toList());
        filteredStructures = new ArrayList<>();
        clampScrollState();
    }

    public void refreshDataAfterStructureChange() {
        updateFilteredItems();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        GuiUtils.renderBackground(guiGraphics, x, y, w, h);

        int listX = leftPos + LIST_X_OFFSET;
        int listY = topPos + LIST_Y_OFFSET;
        int listW = w - LIST_X_OFFSET * 2;
        int listH = h - LIST_Y_OFFSET - 10;

        GuiUtils.renderTechFrame(guiGraphics, listX - 2, listY - 2, listW + 4, listH + 4, 0xFF00AAAA, 0xFF00FFFF);

        int totalVisible = COLS * ROWS;
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        for (int i = 0; i < totalVisible; i++) {
            int index = startIndex + i;
            if (index >= getCurrentEntryCount()) break;

            int col = i % COLS;
            int row = i / COLS;
            int slotX = listX + col * SLOT_SIZE;
            int slotY = listY + row * SLOT_SIZE;

            if (isStructureFilter()) {
                TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = filteredStructures.get(index);
                ItemStack icon = structure.icon.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.STONE) : structure.icon.copy();
                icon.setCount(1);

                boolean isSelected = structure.id != null && structure.id.equals(vars.projection_selected_structure_id);
                if (isSelected) {
                    guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x8000FF00);
                }

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x80FFFFFF);
                    guiGraphics.renderTooltip(this.font, Component.literal(structure.name), mouseX, mouseY);
                }

                guiGraphics.renderItem(icon, slotX + 1, slotY + 1);
                guiGraphics.renderItemDecorations(this.font, icon, slotX + 1, slotY + 1);
            } else {
                ItemStack stack = filteredItems.get(index);
                ItemStack displayStack = stack.copy();
                displayStack.setCount(1);

                boolean isSelected = ItemStack.isSameItemSameComponents(stack, vars.projection_selected_item);
                if (isSelected) {
                    guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x8000FF00);
                }

                if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                    guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x80FFFFFF);
                    guiGraphics.renderTooltip(this.font, displayStack, mouseX, mouseY);
                }

                guiGraphics.renderItem(displayStack, slotX + 1, slotY + 1);
                guiGraphics.renderItemDecorations(this.font, displayStack, slotX + 1, slotY + 1);
            }
        }

        renderScrollBar(guiGraphics, listY, ROWS * SLOT_SIZE, x + w - 8);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = leftPos + LIST_X_OFFSET;
        int listY = topPos + LIST_Y_OFFSET;
        int totalVisible = COLS * ROWS;

        for (int i = 0; i < totalVisible; i++) {
            int index = startIndex + i;
            if (index >= getCurrentEntryCount()) break;

            int col = i % COLS;
            int row = i / COLS;
            int slotX = listX + col * SLOT_SIZE;
            int slotY = listY + row * SLOT_SIZE;

            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                if (isStructureFilter()) {
                    TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = filteredStructures.get(index);
                    if (structure.id != null && !structure.id.isEmpty()) {
                        PacketDistributor.sendToServer(new SelectProjectionStructureMessage(structure.id));
                        playClickSound();
                        startStructureLongPressTracking(structure);
                        return true;
                    }
                    return false;
                }

                ItemStack clickedItem = filteredItems.get(index);
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                int fullIndex = -1;
                for (int k = 0; k < vars.analyzed_items.size(); k++) {
                    if (ItemStack.isSameItemSameComponents(vars.analyzed_items.get(k), clickedItem)) {
                        fullIndex = k;
                        break;
                    }
                }

                if (fullIndex != -1) {
                    PacketDistributor.sendToServer(new SelectProjectionItemMessage(fullIndex));
                    playClickSound();
                    return true;
                }
            }
        }

        clearStructureLongPressState();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            clearStructureLongPressState();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int totalRows = (getCurrentEntryCount() + COLS - 1) / COLS;
        if (totalRows > ROWS) {
            float scrollStep = 1.0f / (totalRows - ROWS);
            this.scrollOffs = Mth.clamp(this.scrollOffs - (float) deltaY * scrollStep, 0.0F, 1.0F);

            int maxStartRow = totalRows - ROWS;
            int startRow = Math.round(this.scrollOffs * maxStartRow);
            this.startIndex = startRow * COLS;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Keep selection UI without vanilla blur/menu background.
    }

    private boolean isStructureFilter() {
        return "structure".equals(currentFilter);
    }

    private int getCurrentEntryCount() {
        return isStructureFilter() ? filteredStructures.size() : filteredItems.size();
    }

    private void clampScrollState() {
        int totalRows = (getCurrentEntryCount() + COLS - 1) / COLS;
        if (totalRows <= ROWS) {
            this.scrollOffs = 0.0F;
            this.startIndex = 0;
            return;
        }
        this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
        int maxStartRow = totalRows - ROWS;
        int startRow = Math.round(this.scrollOffs * maxStartRow);
        this.startIndex = Mth.clamp(startRow * COLS, 0, Math.max(0, getCurrentEntryCount() - 1));
    }

    private void renderScrollBar(GuiGraphics guiGraphics, int scrollBarY, int scrollBarH, int scrollBarX) {
        int totalRows = (getCurrentEntryCount() + COLS - 1) / COLS;
        if (totalRows <= ROWS) {
            return;
        }
        int barHeight = (int) ((float) (ROWS * scrollBarH) / totalRows);
        if (barHeight < 20) barHeight = 20;
        int barTop = scrollBarY + (int) (this.scrollOffs * (scrollBarH - barHeight));

        guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarH, 0x80000000);
        guiGraphics.fill(scrollBarX, barTop, scrollBarX + 4, barTop + barHeight, 0xFF00FFFF);
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK,
                        1.0F
                )
        );
    }

    private void startStructureLongPressTracking(TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure) {
        this.holdingStructureId = structure.id;
        this.holdingStructureName = structure.name == null ? "" : structure.name;
        this.holdStartMs = System.currentTimeMillis();
        this.holdDialogOpened = false;
    }

    private void clearStructureLongPressState() {
        this.holdingStructureId = null;
        this.holdingStructureName = "";
        this.holdStartMs = 0L;
        this.holdDialogOpened = false;
    }

    private void checkStructureDeleteLongPress() {
        if (!isStructureFilter()) {
            clearStructureLongPressState();
            return;
        }
        if (holdingStructureId == null || holdDialogOpened || this.minecraft == null) {
            return;
        }

        long window = this.minecraft.getWindow().getWindow();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!leftDown) {
            clearStructureLongPressState();
            return;
        }

        if (System.currentTimeMillis() - holdStartMs < STRUCTURE_DELETE_HOLD_MS) {
            return;
        }

        String hoveredId = getHoveredStructureId(lastMouseX, lastMouseY);
        if (holdingStructureId.equals(hoveredId)) {
            holdDialogOpened = true;
            this.minecraft.setScreen(new StructureDeleteConfirmScreen(this, holdingStructureId, holdingStructureName));
        } else {
            clearStructureLongPressState();
        }
    }

    private String getHoveredStructureId(double mouseX, double mouseY) {
        int listX = leftPos + LIST_X_OFFSET;
        int listY = topPos + LIST_Y_OFFSET;
        int totalVisible = COLS * ROWS;

        for (int i = 0; i < totalVisible; i++) {
            int index = startIndex + i;
            if (index >= filteredStructures.size()) break;

            int col = i % COLS;
            int row = i / COLS;
            int slotX = listX + col * SLOT_SIZE;
            int slotY = listY + row * SLOT_SIZE;

            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                return filteredStructures.get(index).id;
            }
        }
        return null;
    }
}
