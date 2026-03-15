package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectProjectionItemMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectProjectionStructureMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

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
   private float scrollOffs = 0.0F;
   private int startIndex = 0;
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

   protected void init() {
      super.init();
      this.leftPos = (this.width - this.imageWidth) / 2;
      this.topPos = (this.height - this.imageHeight) / 2;
      this.initFilterButtons();
      this.updateFilteredItems();
   }

   public void tick() {
      super.tick();
      if (this.isStructureFilter()) {
         this.updateFilteredItems();
      }

      this.checkStructureDeleteLongPress();
   }

   private void initFilterButtons() {
      int btnY = this.topPos + 5;
      int btnH = 20;
      int startX = this.leftPos + LIST_X_OFFSET;
      int gap = 2;
      String[] filters = new String[]{"all", "structure", "noble_phantasm", "combat", "tools", "building", "misc", "special"};
      String[] labels = new String[]{
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
         if (width < 40) {
            width = 40;
         }

         NeonButton btn = new NeonButton(currentX, btnY, width, btnH, Component.translatable(labelKey), b -> {
            this.currentFilter = filter;
            this.scrollOffs = 0.0F;
            this.startIndex = 0;
            this.updateFilteredItems();
         }, -16711681);
         this.addRenderableWidget(btn);
         this.filterButtons.add(btn);
         currentX += width + gap;
      }
   }

   private void updateFilteredItems() {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)this.player
         .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (this.isStructureFilter()) {
         this.filteredStructures = vars.analyzed_structures
            .stream()
            .filter(TypeMoonWorldModVariables.PlayerVariables::isTrustedStructure)
            .map(TypeMoonWorldModVariables.PlayerVariables.SavedStructure::copy)
            .collect(Collectors.toList());
         this.filteredItems = new ArrayList<>();
         this.clampScrollState();
      } else {
         List<ItemStack> allItems = vars.analyzed_items;
         this.filteredItems = allItems.stream()
            .filter(
               stack -> {
                  if ("all".equals(this.currentFilter)) {
                     return true;
                  } else if ("noble_phantasm".equals(this.currentFilter)) {
                     return stack.getItem() instanceof NoblePhantasmItem;
                  } else if ("special".equals(this.currentFilter)) {
                     boolean isEnchanted = stack.isEnchanted();
                     boolean isRenamed = stack.has(DataComponents.CUSTOM_NAME);
                     return isEnchanted || isRenamed;
                  } else if ("combat".equals(this.currentFilter)) {
                     return stack.is(ItemTags.SWORDS)
                        || stack.is(ItemTags.AXES)
                        || stack.getItem() instanceof BowItem
                        || stack.getItem() instanceof CrossbowItem
                        || stack.getItem() instanceof ShieldItem
                        || stack.getItem() instanceof ArmorItem
                        || stack.getItem() instanceof TridentItem;
                  } else if ("tools".equals(this.currentFilter)) {
                     return stack.is(ItemTags.PICKAXES)
                        || stack.is(ItemTags.SHOVELS)
                        || stack.is(ItemTags.HOES)
                        || stack.getItem() instanceof ShearsItem
                        || stack.getItem() instanceof FlintAndSteelItem
                        || stack.getItem() instanceof FishingRodItem;
                  } else if ("building".equals(this.currentFilter)) {
                     return stack.getItem() instanceof BlockItem;
                  } else if (!"misc".equals(this.currentFilter)) {
                     return true;
                  } else {
                     boolean isCombat = stack.is(ItemTags.SWORDS)
                        || stack.is(ItemTags.AXES)
                        || stack.getItem() instanceof BowItem
                        || stack.getItem() instanceof CrossbowItem
                        || stack.getItem() instanceof ShieldItem
                        || stack.getItem() instanceof ArmorItem;
                     boolean isTool = stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES);
                     boolean isBlock = stack.getItem() instanceof BlockItem;
                     boolean isNP = stack.getItem() instanceof NoblePhantasmItem;
                     return !isCombat && !isTool && !isBlock && !isNP;
                  }
               }
            )
            .collect(Collectors.toList());
         this.filteredStructures = new ArrayList<>();
         this.clampScrollState();
      }
   }

   public void refreshDataAfterStructureChange() {
      this.updateFilteredItems();
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.lastMouseX = mouseX;
      this.lastMouseY = mouseY;
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      int x = this.leftPos;
      int y = this.topPos;
      int w = this.imageWidth;
      int h = this.imageHeight;
      GuiUtils.renderBackground(guiGraphics, x, y, w, h);
      int listX = this.leftPos + LIST_X_OFFSET;
      int listY = this.topPos + LIST_Y_OFFSET;
      int listW = w - 20;
      int listH = h - 40 - 10;
      GuiUtils.renderTechFrame(guiGraphics, listX - 2, listY - 2, listW + 4, listH + 4, -16733526, -16711681);
      int totalVisible = COLS * ROWS;
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)this.player
         .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

      for (int i = 0; i < totalVisible; i++) {
         int index = this.startIndex + i;
         if (index >= this.getCurrentEntryCount()) {
            break;
         }

         int col = i % COLS;
         int row = i / COLS;
         int slotX = listX + col * SLOT_SIZE;
         int slotY = listY + row * SLOT_SIZE;
         if (this.isStructureFilter()) {
            TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = this.filteredStructures.get(index);
            ItemStack icon = structure.icon.isEmpty() ? new ItemStack(Items.STONE) : structure.icon.copy();
            icon.setCount(1);
            boolean isSelected = structure.id != null && structure.id.equals(vars.projection_selected_structure_id);
            if (isSelected) {
               guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, -2147418368);
            }

            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
               guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, -2130706433);
               guiGraphics.renderTooltip(this.font, Component.literal(structure.name), mouseX, mouseY);
            }

            guiGraphics.renderItem(icon, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, icon, slotX + 1, slotY + 1);
         } else {
            ItemStack stack = this.filteredItems.get(index);
            ItemStack displayStack = stack.copy();
            displayStack.setCount(1);
            boolean isSelectedx = ItemStack.isSameItemSameComponents(stack, vars.projection_selected_item);
            if (isSelectedx) {
               guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, -2147418368);
            }

            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
               guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, -2130706433);
               guiGraphics.renderTooltip(this.font, displayStack, mouseX, mouseY);
            }

            guiGraphics.renderItem(displayStack, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, displayStack, slotX + 1, slotY + 1);
         }
      }

      this.renderScrollBar(guiGraphics, listY, ROWS * SLOT_SIZE, x + w - 8);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int listX = this.leftPos + LIST_X_OFFSET;
      int listY = this.topPos + LIST_Y_OFFSET;
      int totalVisible = COLS * ROWS;

      for (int i = 0; i < totalVisible; i++) {
         int index = this.startIndex + i;
         if (index >= this.getCurrentEntryCount()) {
            break;
         }

         int col = i % COLS;
         int row = i / COLS;
         int slotX = listX + col * SLOT_SIZE;
         int slotY = listY + row * SLOT_SIZE;
         if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
            if (this.isStructureFilter()) {
               TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = this.filteredStructures.get(index);
               if (structure.id != null && !structure.id.isEmpty()) {
                  PacketDistributor.sendToServer(new SelectProjectionStructureMessage(structure.id), new CustomPacketPayload[0]);
                  this.playClickSound();
                  this.startStructureLongPressTracking(structure);
                  return true;
               }

               return false;
            }

            ItemStack clickedItem = this.filteredItems.get(index);
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)this.player
               .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            int fullIndex = -1;

            for (int k = 0; k < vars.analyzed_items.size(); k++) {
               if (ItemStack.isSameItemSameComponents(vars.analyzed_items.get(k), clickedItem)) {
                  fullIndex = k;
                  break;
               }
            }

            if (fullIndex != -1) {
               PacketDistributor.sendToServer(new SelectProjectionItemMessage(fullIndex), new CustomPacketPayload[0]);
               this.playClickSound();
               return true;
            }
         }
      }

      this.clearStructureLongPressState();
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.clearStructureLongPressState();
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      int totalRows = (this.getCurrentEntryCount() + COLS - 1) / COLS;
      if (totalRows > ROWS) {
         float scrollStep = 1.0F / (totalRows - ROWS);
         this.scrollOffs = Mth.clamp(this.scrollOffs - (float)deltaY * scrollStep, 0.0F, 1.0F);
         int maxStartRow = totalRows - ROWS;
         int startRow = Math.round(this.scrollOffs * maxStartRow);
         this.startIndex = startRow * COLS;
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
   }

   private boolean isStructureFilter() {
      return "structure".equals(this.currentFilter);
   }

   private int getCurrentEntryCount() {
      return this.isStructureFilter() ? this.filteredStructures.size() : this.filteredItems.size();
   }

   private void clampScrollState() {
      int totalRows = (this.getCurrentEntryCount() + COLS - 1) / COLS;
      if (totalRows <= ROWS) {
         this.scrollOffs = 0.0F;
         this.startIndex = 0;
      } else {
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         int maxStartRow = totalRows - ROWS;
         int startRow = Math.round(this.scrollOffs * maxStartRow);
         this.startIndex = Mth.clamp(startRow * COLS, 0, Math.max(0, this.getCurrentEntryCount() - 1));
      }
   }

   private void renderScrollBar(GuiGraphics guiGraphics, int scrollBarY, int scrollBarH, int scrollBarX) {
      int totalRows = (this.getCurrentEntryCount() + COLS - 1) / COLS;
      if (totalRows > ROWS) {
         int barHeight = (int)((float)(ROWS * scrollBarH) / totalRows);
         if (barHeight < 20) {
            barHeight = 20;
         }

         int barTop = scrollBarY + (int)(this.scrollOffs * (scrollBarH - barHeight));
         guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarH, Integer.MIN_VALUE);
         guiGraphics.fill(scrollBarX, barTop, scrollBarX + 4, barTop + barHeight, -16711681);
      }
   }

   private void playClickSound() {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
      if (!this.isStructureFilter()) {
         this.clearStructureLongPressState();
      } else if (this.holdingStructureId != null && !this.holdDialogOpened && this.minecraft != null) {
         long window = this.minecraft.getWindow().getWindow();
         boolean leftDown = GLFW.glfwGetMouseButton(window, 0) == 1;
         if (!leftDown) {
            this.clearStructureLongPressState();
         } else if (System.currentTimeMillis() - this.holdStartMs >= STRUCTURE_DELETE_HOLD_MS) {
            String hoveredId = this.getHoveredStructureId(this.lastMouseX, this.lastMouseY);
            if (this.holdingStructureId.equals(hoveredId)) {
               this.holdDialogOpened = true;
               this.minecraft.setScreen(new StructureDeleteConfirmScreen(this, this.holdingStructureId, this.holdingStructureName));
            } else {
               this.clearStructureLongPressState();
            }
         }
      }
   }

   private String getHoveredStructureId(double mouseX, double mouseY) {
      int listX = this.leftPos + LIST_X_OFFSET;
      int listY = this.topPos + LIST_Y_OFFSET;
      int totalVisible = COLS * ROWS;

      for (int i = 0; i < totalVisible; i++) {
         int index = this.startIndex + i;
         if (index >= this.filteredStructures.size()) {
            break;
         }

         int col = i % COLS;
         int row = i / COLS;
         int slotX = listX + col * SLOT_SIZE;
         int slotY = listY + row * SLOT_SIZE;
         if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
            return this.filteredStructures.get(index).id;
         }
      }

      return null;
   }
}
