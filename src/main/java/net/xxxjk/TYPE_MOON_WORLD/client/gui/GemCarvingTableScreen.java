package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.network.GemCarvingEngraveMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.GemCarvingTableMenu;
import org.jetbrains.annotations.NotNull;

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
   private static final int COLOR_BORDER_DARK = -14869219;
   private static final int COLOR_BORDER_LIGHT = -7566196;
   private static final int COLOR_PANEL_OUTER = -11184811;
   private static final int COLOR_PANEL_INNER = -12632257;
   private static final int COLOR_SECTION_OUTER = -10395295;
   private static final int COLOR_SECTION_INNER = -11908534;
   private static final int COLOR_SLOT_INNER = -13882324;
   private static final int COLOR_TEXT = -2039584;
   private static final int COLOR_TEXT_HINT = -3750202;
   private static final int COLOR_TEXT_WARN = -32640;
   private static final int COLOR_TEXT_OK = -6756712;
   private final List<GemCarvingTableScreen.MagicButtonEntry> magicButtons = new ArrayList<>();
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
   private int projectionMode = 0;
   private final List<Integer> projectionItemCandidates = new ArrayList<>();
   private final List<String> projectionStructureCandidates = new ArrayList<>();
   private int projectionItemCursor = 0;
   private int projectionStructureCursor = 0;

   public GemCarvingTableScreen(GemCarvingTableMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = PANEL_W;
      this.imageHeight = PANEL_H;
   }

   protected void init() {
      super.init();
      this.magicButtons.clear();
      List<String> availableMagics = this.getAvailableMagics();
      int buttonX = this.leftPos + SECTION_MAGIC_X + 3;
      int buttonY = this.topPos + SECTION_MAGIC_Y + 14;
      int buttonWidth = SECTION_MAGIC_W - 6;
      int buttonHeight = ENGRAVE_BUTTON_H;

      for (String magicId : availableMagics) {
         Button button = this.vanillaButton(buttonX, buttonY, buttonWidth, buttonHeight, Component.empty(), btn -> {
            this.selectedMagicId = magicId;
            this.updateUiState();
         });
         this.addRenderableWidget(button);
         this.magicButtons.add(new GemCarvingTableScreen.MagicButtonEntry(magicId, button));
         buttonY += 20;
      }

      this.selectedMagicId = availableMagics.isEmpty() ? null : availableMagics.get(0);
       int ctrlButtonX = this.leftPos + SECTION_CTRL_X + 4;
       int ctrlButtonW = SECTION_CTRL_W - 8;
       this.reinforcementPartButton = this.vanillaButton(ctrlButtonX, this.topPos + SECTION_CTRL_Y + 106, ctrlButtonW, ENGRAVE_BUTTON_H, Component.empty(), btn -> {
          this.reinforcementPart = (this.reinforcementPart + 1) % 4;
          this.updateUiState();
       });
       this.addRenderableWidget(this.reinforcementPartButton);
       int levelRowY = this.topPos + SECTION_CTRL_Y + 128;
       this.reinforcementLevelDownButton = this.vanillaButton(ctrlButtonX, levelRowY, 20, ENGRAVE_BUTTON_H, Component.literal("-"), btn -> {
          this.reinforcementLevel = Math.max(1, this.reinforcementLevel - 1);
          this.updateUiState();
       });
       this.addRenderableWidget(this.reinforcementLevelDownButton);
       this.reinforcementLevelUpButton = this.vanillaButton(ctrlButtonX + ctrlButtonW - 20, levelRowY, 20, ENGRAVE_BUTTON_H, Component.literal("+"), btn -> {
          this.reinforcementLevel = Math.min(5, this.reinforcementLevel + 1);
          this.updateUiState();
       });
       this.addRenderableWidget(this.reinforcementLevelUpButton);
       this.projectionModeButton = this.vanillaButton(ctrlButtonX, this.topPos + SECTION_CTRL_Y + 84, ctrlButtonW, ENGRAVE_BUTTON_H, Component.empty(), btn -> {
          this.projectionMode = this.projectionMode == 0 ? 1 : 0;
          this.updateUiState();
       });
       this.addRenderableWidget(this.projectionModeButton);
       int projectionNavY = this.topPos + SECTION_CTRL_Y + 152;
       this.projectionTargetPrevButton = this.vanillaButton(ctrlButtonX, projectionNavY, 20, ENGRAVE_BUTTON_H, Component.literal("<"), btn -> {
          this.cycleProjectionTarget(-1);
          this.updateUiState();
       });
       this.addRenderableWidget(this.projectionTargetPrevButton);
       this.projectionTargetNextButton = this.vanillaButton(ctrlButtonX + ctrlButtonW - 20, projectionNavY, 20, ENGRAVE_BUTTON_H, Component.literal(">"), btn -> {
          this.cycleProjectionTarget(1);
          this.updateUiState();
       });
       this.addRenderableWidget(this.projectionTargetNextButton);
       int engraveButtonY = this.topPos + PANEL_H - ENGRAVE_BUTTON_H - ENGRAVE_BUTTON_BOTTOM_MARGIN;
       this.engraveButton = this.vanillaButton(
          ctrlButtonX,
          engraveButtonY,
          ctrlButtonW,
          ENGRAVE_BUTTON_H,
          Component.translatable("gui.typemoonworld.gem_carving_table.engrave"),
         btn -> {
            if (this.selectedMagicId != null) {
               PacketDistributor.sendToServer(
                  new GemCarvingEngraveMessage(
                     this.selectedMagicId,
                     this.reinforcementPart,
                     this.reinforcementLevel,
                     this.projectionMode,
                     this.getSelectedProjectionItemIndex(),
                     this.getSelectedProjectionStructureId()
                  ),
                  new CustomPacketPayload[0]
               );
            }
         }
      );
      this.addRenderableWidget(this.engraveButton);
      this.rebuildProjectionCandidates();
      this.updateUiState();
   }

   private Button vanillaButton(int x, int y, int width, int height, Component text, OnPress onPress) {
      return Button.builder(text, onPress).bounds(x, y, width, height).build();
   }

   private void updateUiState() {
      this.rebuildProjectionCandidates();

      for (GemCarvingTableScreen.MagicButtonEntry entry : this.magicButtons) {
         boolean selected = entry.magicId.equals(this.selectedMagicId);
         int maxTextWidth = Math.max(8, entry.button.getWidth() - 8);
         entry.button.setMessage(this.getMagicButtonLabel(entry.magicId, selected, maxTextWidth));
      }

      boolean reinforcementSelected = "reinforcement".equals(this.selectedMagicId);
      boolean projectionSelected = "projection".equals(this.selectedMagicId);
      this.setWidgetState(this.reinforcementPartButton, reinforcementSelected, true);
      this.setWidgetState(this.reinforcementLevelDownButton, reinforcementSelected, true);
      this.setWidgetState(this.reinforcementLevelUpButton, reinforcementSelected, true);
      this.setWidgetState(this.projectionModeButton, projectionSelected, true);
      boolean hasMultipleTargets = false;
      if (projectionSelected) {
         hasMultipleTargets = this.projectionMode == 1 ? this.projectionStructureCandidates.size() > 1 : this.projectionItemCandidates.size() > 1;
      }

      this.setWidgetState(this.projectionTargetPrevButton, projectionSelected, hasMultipleTargets);
      this.setWidgetState(this.projectionTargetNextButton, projectionSelected, hasMultipleTargets);
      this.reinforcementPartButton
         .setMessage(
            Component.translatable(
               "gui.typemoonworld.gem_carving_table.reinforcement_part", GemEngravingService.getReinforcementPartName(this.reinforcementPart)
            )
         );
      this.projectionModeButton
         .setMessage(
            Component.translatable(
               "gui.typemoonworld.gem_carving_table.projection_mode",
               
                  this.projectionMode == 1
                     ? Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.structure")
                     : Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.item")
               
            )
         );
   }

   private void setWidgetState(Button button, boolean visible, boolean activeWhenVisible) {
      button.visible = visible;
      button.active = visible && activeWhenVisible;
   }

   private List<String> getAvailableMagics() {
      List<String> magics = new ArrayList<>();
      if (this.minecraft != null && this.minecraft.player != null) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)this.minecraft
            .player
            .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (hasLearnedMagic(vars, "projection")) {
            magics.add("projection");
         }

         if (hasLearnedMagic(vars, "reinforcement")) {
            magics.add("reinforcement");
         }

         if (hasLearnedMagic(vars, "gravity_magic")) {
            magics.add("gravity_magic");
         }

         if (hasLearnedMagic(vars, "gander")) {
            magics.add("gander");
         }

         return magics;
      } else {
         return magics;
      }
   }

   private static boolean hasLearnedMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      return !"reinforcement".equals(magicId)
         ? vars.learned_magics.contains(magicId)
         : vars.learned_magics.contains("reinforcement")
            || vars.learned_magics.contains("reinforcement_self")
            || vars.learned_magics.contains("reinforcement_other")
            || vars.learned_magics.contains("reinforcement_item");
   }

   private Component getMagicButtonLabel(String magicId, boolean selected, int maxWidth) {
      String name = GemEngravingService.getMagicName(magicId).getString();
      String prefix = selected ? "> " : "";
      String text = prefix + name;
      if (this.font.width(text) <= maxWidth) {
         return Component.literal(text);
      } else {
         String clipped = this.font.plainSubstrByWidth(text, Math.max(2, maxWidth - this.font.width("..."))) + "...";
         return Component.literal(clipped);
      }
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
      super.render(guiGraphics, mouseX, mouseY, partialTicks);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }

   public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
   }

   protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      this.drawPanel(guiGraphics, x + PANEL_X, y + PANEL_Y, PANEL_W, PANEL_H);
      this.drawSection(guiGraphics, x + SECTION_GEM_X, y + SECTION_GEM_Y, SECTION_GEM_W, SECTION_GEM_H);
      this.drawSection(guiGraphics, x + SECTION_MAGIC_X, y + SECTION_MAGIC_Y, SECTION_MAGIC_W, SECTION_MAGIC_H);
      this.drawSection(guiGraphics, x + SECTION_CTRL_X, y + SECTION_CTRL_Y, SECTION_CTRL_W, SECTION_CTRL_H);
      this.drawSection(guiGraphics, x + SECTION_INV_X, y + SECTION_INV_Y, SECTION_INV_W, SECTION_INV_H);
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

   protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
      Component title = Component.translatable("gui.typemoonworld.gem_carving_table.title");
      int titleX = (this.imageWidth - this.font.width(title)) / 2;
      guiGraphics.drawString(this.font, title, titleX, 6, COLOR_TEXT, false);
      guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.gem_carving_table.gem_slot"), 11, 22, COLOR_TEXT_HINT, false);
      guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.gem_carving_table.tool_slot"), 11, 53, COLOR_TEXT_HINT, false);
      guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.gem_carving_table.magic_list"), 73, 22, COLOR_TEXT_HINT, false);
      guiGraphics.drawString(this.font, this.playerInventoryTitle, 11, 116, COLOR_TEXT_HINT, false);
      if ("reinforcement".equals(this.selectedMagicId)) {
         Component lvl = Component.translatable("gui.typemoonworld.gem_carving_table.reinforcement_level", this.reinforcementLevel);
         int textX = SECTION_CTRL_X + 26 + (62 - this.font.width(lvl)) / 2;
         guiGraphics.drawString(this.font, lvl, textX, 151, COLOR_TEXT, false);
      } else if ("projection".equals(this.selectedMagicId)) {
         guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.gem_carving_table.projection_target"), 200, 148, COLOR_TEXT, false);
      }

      if (this.selectedMagicId != null && this.minecraft != null && this.minecraft.player != null) {
         GemCarvingTableMenu.EngravePreview preview = ((GemCarvingTableMenu)this.menu)
            .getPreview(
               this.minecraft.player,
               this.selectedMagicId,
               this.reinforcementPart,
               this.reinforcementLevel,
               this.projectionMode,
               this.getSelectedProjectionItemIndex(),
               this.getSelectedProjectionStructureId()
            );
         boolean hasTool = this.hasValidToolInput();
         if (preview.valid() && hasTool) {
            this.engraveButton.active = true;
            guiGraphics.drawString(
               this.font,
               Component.translatable("gui.typemoonworld.gem_carving_table.success_chance", preview.chance() + "%"),
               200,
               46,
               COLOR_TEXT_OK,
               false
            );
            guiGraphics.drawString(
               this.font,
               Component.translatable(
                  "gui.typemoonworld.gem_carving_table.mana_usage", (int)Math.ceil(preview.requiredMana()), preview.capacity()
               ),
               200,
               58,
               COLOR_TEXT,
               false
            );
            if ("projection".equals(this.selectedMagicId)) {
               String target = preview.projectionMode() == 1 ? preview.projectionStructureName() : preview.projectionItemTemplate().getHoverName().getString();
               if (target == null || target.isEmpty()) {
                  target = "-";
               }

               String clamped = this.font.plainSubstrByWidth(target, 106);
               guiGraphics.drawString(this.font, Component.literal(clamped), 200, 160, COLOR_TEXT_HINT, false);
            }
         } else {
            this.engraveButton.active = false;
            guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.gem_carving_table.success_chance_invalid"), 200, 46, COLOR_TEXT_WARN, false);
            Component errorText = hasTool
               ? Component.translatable(preview.errorKey(), preview.errorArgs())
               : Component.translatable("message.typemoonworld.gem.engrave.need_tool");
            guiGraphics.drawWordWrap(this.font, errorText, 200, 58, 106, COLOR_TEXT_WARN);
         }
      } else if (this.engraveButton != null) {
         this.engraveButton.active = false;
      }
   }

   private boolean hasValidToolInput() {
      ItemStack toolStack = ((GemCarvingTableMenu)this.menu).getSlot(1).getItem();
      return !toolStack.isEmpty() && toolStack.getItem() instanceof ChiselItem;
   }

   private void rebuildProjectionCandidates() {
      if (this.minecraft != null && this.minecraft.player != null) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)this.minecraft
            .player
            .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
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
      } else {
         this.projectionItemCandidates.clear();
         this.projectionStructureCandidates.clear();
         this.projectionItemCursor = 0;
         this.projectionStructureCursor = 0;
      }
   }

   private void cycleProjectionTarget(int delta) {
      if (this.projectionMode == 1) {
         if (!this.projectionStructureCandidates.isEmpty()) {
            this.projectionStructureCursor = Math.floorMod(this.projectionStructureCursor + delta, this.projectionStructureCandidates.size());
         }
      } else if (!this.projectionItemCandidates.isEmpty()) {
         this.projectionItemCursor = Math.floorMod(this.projectionItemCursor + delta, this.projectionItemCandidates.size());
      }
   }

   private int getSelectedProjectionItemIndex() {
      if (this.projectionItemCandidates.isEmpty()) {
         return -1;
      } else {
         int cursor = Math.floorMod(this.projectionItemCursor, this.projectionItemCandidates.size());
         return this.projectionItemCandidates.get(cursor);
      }
   }

   private String getSelectedProjectionStructureId() {
      if (this.projectionStructureCandidates.isEmpty()) {
         return "";
      } else {
         int cursor = Math.floorMod(this.projectionStructureCursor, this.projectionStructureCandidates.size());
         return this.projectionStructureCandidates.get(cursor);
      }
   }

   private record MagicButtonEntry(String magicId, Button button) {
   }
}
