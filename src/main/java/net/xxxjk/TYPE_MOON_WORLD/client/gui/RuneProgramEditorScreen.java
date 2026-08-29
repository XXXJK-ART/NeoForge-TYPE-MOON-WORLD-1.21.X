package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneDefinition;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RunePosition;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgram;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramCostService;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Twenty-slot rune editor with responsive controls and server-authoritative saving. */
public final class RuneProgramEditorScreen extends Screen {
   private static final int MAX_WIDTH = 700;
   private static final int MAX_HEIGHT = 340;
   private final Screen parent;
   private final RuneProgram program;
   private EditBox name;
   private int selectedSlot = -1;
   private RuneDefinition draggingRune;
   private int draggingSourceSlot = -1;

   public RuneProgramEditorScreen(Screen parent, RuneProgram program) {
      super(Component.translatable("gui.typemoonworld.rune.editor"));
      this.parent = parent;
      this.program = program == null ? new RuneProgram() : program.copy();
   }

   @Override
   protected void init() {
      Layout layout = layout();
      name = new EditBox(font, layout.nameX, layout.nameY, layout.nameWidth, 20,
         Component.translatable("gui.typemoonworld.rune.name"));
      name.setMaxLength(RuneProgram.MAX_NAME_LENGTH);
      name.setValue(program.displayName());
      addRenderableWidget(name);

      int buttonWidth = Math.max(72, Math.min(112, (layout.bounds.width - 36) / 3));
      int gap = 6;
      int totalWidth = buttonWidth * 3 + gap * 2;
      int buttonX = layout.bounds.x + (layout.bounds.width - totalWidth) / 2;
      int buttonY = layout.bounds.y + layout.bounds.height - 28;
      addRenderableWidget(actionButton(buttonX, buttonY, buttonWidth, Component.translatable("gui.cancel"), b -> onClose(), GuiUtils.ARCANE_BORDER));
      addRenderableWidget(actionButton(buttonX + buttonWidth + gap, buttonY, buttonWidth,
         Component.translatable("gui.typemoonworld.rune.clear"), b -> clearSelected(), GuiUtils.ARCANE_DANGER));
      addRenderableWidget(actionButton(buttonX + (buttonWidth + gap) * 2, buttonY, buttonWidth,
         Component.translatable("gui.done"), b -> save(), GuiUtils.ARCANE_VALID));
   }

   private NeonButton actionButton(int x, int y, int buttonWidth, Component label,
      net.minecraft.client.gui.components.Button.OnPress action, int color) {
      return new NeonButton(x, y, buttonWidth, 20, label, action, color).setArcaneStyle(true).setCompactStyle(true);
   }

   private void clearSelected() {
      if (selectedSlot < 0) {
         for (RunePosition position : RunePosition.values()) for (int i = 0; i < RuneProgram.BAND_SIZE; i++) program.setSlot(position, i, "");
      } else {
         program.setSlot(RunePosition.values()[selectedSlot / 5], selectedSlot % 5, "");
      }
      selectedSlot = -1;
   }

   private void save() {
      program.setDisplayName(name.getValue());
      // Release mode and medium parameters are configured in the next step.
      minecraft.setScreen(new RuneProgramConfigScreen(parent, program));
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(graphics, width, height);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics, mouseX, mouseY, partialTick);
      Layout layout = layout();
      GuiUtils.renderArcaneWindow(graphics, layout.bounds.x, layout.bounds.y,
         layout.bounds.width, layout.bounds.height, GuiUtils.ARCANE_CYAN);
      graphics.drawCenteredString(font, title, width / 2, layout.bounds.y + 9, GuiUtils.ARCANE_TEXT);
      graphics.drawCenteredString(font, Component.translatable("gui.typemoonworld.rune.drag_hint"),
         width / 2, layout.bounds.y + 21, GuiUtils.ARCANE_TEXT_MUTED);
      graphics.drawString(font, Component.translatable("gui.typemoonworld.rune.name"),
         layout.nameX, layout.nameY - 10, GuiUtils.ARCANE_TEXT, false);
      graphics.drawString(font, Component.translatable("gui.typemoonworld.rune.slots"),
         layout.slotPanelX + 8, layout.contentY - 14, GuiUtils.ARCANE_TEXT, false);
      graphics.drawString(font, Component.translatable("gui.typemoonworld.rune.palette"),
         layout.palettePanelX + 8, layout.contentY - 14, GuiUtils.ARCANE_TEXT, false);

      GuiUtils.renderArcanePanel(graphics, layout.slotPanelX, layout.contentY,
         layout.slotPanelWidth, layout.contentHeight, GuiUtils.ARCANE_GOLD);
      GuiUtils.renderArcanePanel(graphics, layout.palettePanelX, layout.contentY,
         layout.palettePanelWidth, layout.contentHeight, GuiUtils.ARCANE_CYAN);
      renderSlots(graphics, layout, mouseX, mouseY);
      renderPalette(graphics, layout, mouseX, mouseY);
      graphics.drawString(font, Component.translatable("gui.typemoonworld.rune.cost",
         String.format("%.0f", RuneProgramCostService.calculate(program))),
         layout.slotPanelX + 8, layout.contentY + layout.contentHeight - 15, GuiUtils.ARCANE_TEXT_MUTED, false);
      super.render(graphics, mouseX, mouseY, partialTick);
      if (draggingRune != null) {
         int icon = Math.max(16, Math.min(28, layout.paletteSize));
         GuiUtils.renderRuneIcon(graphics, draggingRune.icon(), mouseX - icon / 2, mouseY - icon / 2,
            icon, draggingRune.color(), true);
      } else {
         RuneDefinition hovered = hoveredRune(layout, mouseX, mouseY);
         if (hovered != null) renderRuneTooltip(graphics, hovered, mouseX, mouseY);
      }
   }

   private void renderSlots(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
      Component[] slotLabels = {
         Component.translatable("gui.typemoonworld.rune.slot.trigger"),
         Component.translatable("gui.typemoonworld.rune.slot.effect"),
         Component.translatable("gui.typemoonworld.rune.slot.modifier"),
         Component.translatable("gui.typemoonworld.rune.slot.terminal")
      };
      for (int band = 0; band < 4; band++) {
         int rowY = layout.slotY(band);
         int labelX = layout.slotPanelX + 8;
         int labelY = layout.compact ? rowY : rowY + (layout.slotSize - 8) / 2;
         String label = slotLabels[band].getString();
         graphics.drawString(font, layout.compact ? label.substring(0, Math.min(1, label.length())) : label,
            labelX, labelY, GuiUtils.ARCANE_GOLD, false);
         for (int slot = 0; slot < 5; slot++) {
            int index = band * 5 + slot;
            int slotX = layout.slotX(slot);
            int slotY = layout.compact ? rowY : rowY;
            String runeId = program.getSlot(RunePosition.values()[band], slot);
            RuneDefinition rune = RuneRegistry.get(runeId);
            boolean hovered = inside(mouseX, mouseY, slotX, slotY, layout.slotSize, layout.slotSize);
            GuiUtils.renderArcaneSlot(graphics, slotX, slotY, layout.slotSize,
               index == selectedSlot || hovered || (draggingRune != null && hovered) ? GuiUtils.ARCANE_CYAN : GuiUtils.ARCANE_BORDER, rune != null);
            if (rune != null) {
         int icon = Math.max(14, layout.slotSize - 5);
               GuiUtils.renderRuneIcon(graphics, rune.icon(), slotX + (layout.slotSize - icon) / 2,
                  slotY + (layout.slotSize - icon) / 2, icon, rune.color(), true);
            } else {
               graphics.drawCenteredString(font, String.valueOf(index + 1), slotX + layout.slotSize / 2,
                  slotY + (layout.slotSize - 8) / 2, GuiUtils.ARCANE_TEXT_MUTED);
            }
         }
      }
   }

   private void renderPalette(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
      TypeMoonWorldModVariables.PlayerVariables vars = minecraft.player == null
         ? null : minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      List<RuneDefinition> runes = RuneRegistry.all();
      for (int i = 0; i < runes.size(); i++) {
         RuneDefinition rune = runes.get(i);
         int runeX = layout.paletteX(i);
         int runeY = layout.paletteY(i);
         boolean learned = vars != null && vars.learned_runes.contains(rune.idPath());
         boolean hovered = inside(mouseX, mouseY, runeX, runeY, layout.paletteSize, layout.paletteSize);
            GuiUtils.renderArcaneSlot(graphics, runeX, runeY, layout.paletteSize, rune.color(), learned);
         if (hovered) graphics.renderOutline(runeX - 1, runeY - 1, layout.paletteSize + 2, layout.paletteSize + 2, rune.color());
               int icon = Math.max(14, layout.paletteSize - 5);
         GuiUtils.renderRuneIcon(graphics, rune.icon(), runeX + (layout.paletteSize - icon) / 2,
            runeY + (layout.paletteSize - icon) / 2, icon, rune.color(), hovered || learned);
         if (learned) graphics.renderOutline(runeX + 2, runeY + 2, layout.paletteSize - 4, layout.paletteSize - 4, 0xCCFFFFFF);
         else graphics.fill(runeX + 1, runeY + 1, runeX + layout.paletteSize - 1, runeY + layout.paletteSize - 1, 0x35080B0E);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      Layout layout = layout();
      for (int band = 0; band < 4; band++) {
         for (int slot = 0; slot < 5; slot++) {
         if (inside(mouseX, mouseY, layout.slotX(slot), layout.slotY(band), layout.slotSize, layout.slotSize)) {
            selectedSlot = band * 5 + slot;
            if (button == 0) {
               RuneDefinition rune = RuneRegistry.get(program.getSlot(RunePosition.values()[band], slot));
               if (rune != null) {
                  draggingRune = rune;
                  draggingSourceSlot = selectedSlot;
               }
            } else if (button == 1) {
               clearSelected();
            }
            return true;
         }
         }
      }

      TypeMoonWorldModVariables.PlayerVariables vars = minecraft.player == null
         ? null : minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      List<RuneDefinition> runes = RuneRegistry.all();
      for (int i = 0; i < runes.size(); i++) {
         RuneDefinition rune = runes.get(i);
         if (inside(mouseX, mouseY, layout.paletteX(i), layout.paletteY(i), layout.paletteSize, layout.paletteSize)) {
            if (button == 0 && vars != null && vars.learned_runes.contains(rune.idPath())) {
               draggingRune = rune;
               draggingSourceSlot = -1;
            }
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      return draggingRune != null && button == 0 || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && draggingRune != null) {
         Layout layout = layout();
         int target = slotAt(layout, mouseX, mouseY);
         if (target >= 0) {
            program.setSlot(RunePosition.values()[target / 5], target % 5, draggingRune.idPath());
            if (draggingSourceSlot >= 0 && draggingSourceSlot != target) {
               program.setSlot(RunePosition.values()[draggingSourceSlot / 5], draggingSourceSlot % 5, "");
            }
            selectedSlot = target;
         }
         draggingRune = null;
         draggingSourceSlot = -1;
         return true;
      }
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private int slotAt(Layout layout, double mouseX, double mouseY) {
      for (int band = 0; band < 4; band++) {
         for (int slot = 0; slot < 5; slot++) {
            if (inside(mouseX, mouseY, layout.slotX(slot), layout.slotY(band), layout.slotSize, layout.slotSize)) {
               return band * 5 + slot;
            }
         }
      }
      return -1;
   }

   private RuneDefinition hoveredRune(Layout layout, double mouseX, double mouseY) {
      for (int i = 0; i < RuneRegistry.all().size(); i++) {
         RuneDefinition rune = RuneRegistry.all().get(i);
         if (inside(mouseX, mouseY, layout.paletteX(i), layout.paletteY(i), layout.paletteSize, layout.paletteSize)) return rune;
      }
      for (int band = 0; band < 4; band++) {
         for (int slot = 0; slot < 5; slot++) {
            if (inside(mouseX, mouseY, layout.slotX(slot), layout.slotY(band), layout.slotSize, layout.slotSize)) {
               return RuneRegistry.get(program.getSlot(RunePosition.values()[band], slot));
            }
         }
      }
      return null;
   }

   private void renderRuneTooltip(GuiGraphics graphics, RuneDefinition rune, int mouseX, int mouseY) {
      List<Component> lines = new ArrayList<>();
      lines.add(Component.literal(rune.displayName()));
      for (RunePosition position : RunePosition.values()) {
         var spec = rune.effectSpec(position);
         String label = spec.isEmpty() ? semanticValue(rune.semantic(position)).getString() : spec.name() + "：" + spec.description();
         lines.add(Component.translatable("gui.typemoonworld.rune." + position.name().toLowerCase(java.util.Locale.ROOT), label));
      }
      graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
   }

   private Component semanticValue(String value) {
      String key = "gui.typemoonworld.rune.semantic." + value;
      Component translated = Component.translatable(key);
      return translated.getString().equals(key) ? Component.literal(value) : translated;
   }

   private Layout layout() {
      int panelWidth = Math.min(Math.max(310, width - 12), Math.min(MAX_WIDTH, width));
      int panelHeight = Math.min(Math.max(236, height - 12), Math.min(MAX_HEIGHT, height));
      Bounds bounds = new Bounds((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
      boolean compact = panelWidth < 590;
      // The footer remains fixed; only the editor content moves upward to use the freed space.
      int nameY = bounds.y + (compact ? 28 : 32);
      int nameWidth = compact ? Math.max(92, panelWidth - 24) : 220;
      int nameX = bounds.x + 12;
      int contentY = bounds.y + (compact ? 76 : 58);
      int contentHeight = bounds.y + bounds.height - 36 - contentY;
      int innerWidth = bounds.width - 24;
      int slotPanelWidth = compact ? (innerWidth - 8) / 2 : Math.min(320, innerWidth / 2);
      int palettePanelWidth = innerWidth - slotPanelWidth - 8;
      int slotPanelX = bounds.x + 12;
      int palettePanelX = slotPanelX + slotPanelWidth + 8;
      int slotLabelWidth = compact ? 18 : 70;
      int slotGap = compact ? 3 : 8;
      int slotSize = Math.max(17, Math.min(30, (slotPanelWidth - slotLabelWidth - 16 - slotGap * 4) / 5));
      int slotStartX = slotPanelX + slotLabelWidth + 8;
      int slotAreaHeight = Math.max(1, contentHeight - 20);
      int slotRowGap = Math.max(1, (slotAreaHeight - slotSize * 4) / 3);
      // Six narrow columns keep all 24 runes within the compact panel's height.
      int paletteColumns = 6;
      int paletteGap = compact ? 3 : 6;
      int paletteSize = Math.max(16, Math.min(30,
         (palettePanelWidth - 16 - paletteGap * (paletteColumns - 1)) / paletteColumns));
      int paletteStartX = palettePanelX + (palettePanelWidth - (paletteSize * paletteColumns + paletteGap * (paletteColumns - 1))) / 2;
      int paletteRows = (RuneRegistry.all().size() + paletteColumns - 1) / paletteColumns;
      int paletteRowGap = Math.max(1, Math.min(6, (contentHeight - 12 - paletteSize * paletteRows) / Math.max(1, paletteRows - 1)));
      return new Layout(bounds, compact, nameX, nameY, nameWidth, contentY, contentHeight,
         slotPanelX, slotPanelWidth, slotStartX, slotSize, slotGap, slotRowGap,
         palettePanelX, palettePanelWidth, paletteStartX, paletteColumns, paletteSize, paletteGap, paletteRowGap);
   }

   private static boolean inside(double mouseX, double mouseY, int x, int y, int areaWidth, int areaHeight) {
      return mouseX >= x && mouseX < x + areaWidth && mouseY >= y && mouseY < y + areaHeight;
   }

   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }

   private record Bounds(int x, int y, int width, int height) { }

   private record Layout(Bounds bounds, boolean compact, int nameX, int nameY, int nameWidth,
      int contentY, int contentHeight, int slotPanelX, int slotPanelWidth, int slotStartX,
      int slotSize, int slotGap, int slotRowGap, int palettePanelX, int palettePanelWidth,
      int paletteStartX, int paletteColumns, int paletteSize, int paletteGap, int paletteRowGap) {
      int slotX(int index) { return slotStartX + index * (slotSize + slotGap); }
      int slotY(int band) { return contentY + 7 + band * (slotSize + slotRowGap); }
      int paletteX(int index) { return paletteStartX + index % paletteColumns * (paletteSize + paletteGap); }
      int paletteY(int index) { return contentY + 7 + index / paletteColumns * (paletteSize + paletteRowGap); }
   }
}

final class RuneProgramValidationResultView {
   private RuneProgramValidationResultView() { }

   static void render(GuiGraphics graphics, net.minecraft.client.gui.Font font, RuneProgram program, int x, int y) {
      var result = program.validate();
      int color = result.valid() ? GuiUtils.ARCANE_VALID : GuiUtils.ARCANE_DANGER;
      Component status = result.valid() ? Component.translatable("gui.typemoonworld.rune.valid")
         : Component.translatable("gui.typemoonworld.rune.invalid");
      graphics.drawString(font, font.plainSubstrByWidth(status.getString(), 142), x, y, color, false);
      graphics.drawString(font, Component.translatable("gui.typemoonworld.rune.cost",
         String.format("%.0f", RuneProgramCostService.calculate(program))), x + 148, y, GuiUtils.ARCANE_TEXT_MUTED, false);
   }
}
