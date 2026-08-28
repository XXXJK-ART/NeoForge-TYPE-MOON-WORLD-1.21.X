package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneDefinition;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RunePosition;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Rune catalogue with a responsive grid and a semantic detail pane. */
public final class RuneKnowledgeScreen extends Screen {
   private static final int MAX_WIDTH = 620;
   private static final int MAX_HEIGHT = 300;
   private final Screen parent;
   private int selected;

   public RuneKnowledgeScreen(Screen parent) {
      super(Component.translatable("gui.typemoonworld.rune.knowledge"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      Bounds bounds = bounds();
      int buttonY = bounds.y + bounds.height - 28;
      int gap = 6;
      int buttonWidth = Math.max(72, Math.min(104, (bounds.width - 32 - gap * 2) / 3));
      int totalWidth = buttonWidth * 3 + gap * 2;
      int buttonX = bounds.x + (bounds.width - totalWidth) / 2;
      addRenderableWidget(arcaneButton(buttonX, buttonY, buttonWidth,
         Component.translatable("gui.back"), button -> onClose(), GuiUtils.ARCANE_BORDER));
      addRenderableWidget(arcaneButton(buttonX + buttonWidth + gap, buttonY, buttonWidth,
         Component.translatable("gui.typemoonworld.rune.library"),
         button -> minecraft.setScreen(new RuneProgramLibraryScreen(this)), GuiUtils.ARCANE_GOLD));
      addRenderableWidget(arcaneButton(buttonX + (buttonWidth + gap) * 2, buttonY, buttonWidth,
         Component.translatable("gui.typemoonworld.rune.editor"),
         button -> minecraft.setScreen(new RuneProgramEditorScreen(this, null)), GuiUtils.ARCANE_CYAN));
   }

   private NeonButton arcaneButton(int x, int y, int buttonWidth, Component label,
      net.minecraft.client.gui.components.Button.OnPress action, int color) {
      return new NeonButton(x, y, buttonWidth, 20, label, action, color).setArcaneStyle(true).setCompactStyle(true);
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(graphics, width, height);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics, mouseX, mouseY, partialTick);
      Bounds bounds = bounds();
      Grid grid = grid(bounds);
      GuiUtils.renderArcaneWindow(graphics, bounds.x, bounds.y, bounds.width, bounds.height, GuiUtils.ARCANE_CYAN);
      graphics.drawCenteredString(font, title, width / 2, bounds.y + 9, GuiUtils.ARCANE_TEXT);

      GuiUtils.renderArcanePanel(graphics, grid.x - 6, grid.y - 6, grid.panelWidth, grid.panelHeight, GuiUtils.ARCANE_CYAN);
      TypeMoonWorldModVariables.PlayerVariables vars = minecraft.player == null
         ? null : minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      List<RuneDefinition> runes = RuneRegistry.all();
      for (int i = 0; i < runes.size(); i++) {
         RuneDefinition rune = runes.get(i);
         int tileX = grid.tileX(i);
         int tileY = grid.tileY(i);
         boolean learned = vars != null && vars.learned_runes.contains(rune.idPath());
         boolean hovered = inside(mouseX, mouseY, tileX, tileY, grid.tileSize, grid.tileSize);
         GuiUtils.renderChoiceTile(graphics, tileX, tileY, grid.tileSize, grid.tileSize,
            rune.color(), i == selected || hovered, learned);
         int iconSize = Math.max(18, grid.tileSize - 6);
         int iconX = tileX + (grid.tileSize - iconSize) / 2;
         int iconY = tileY + (grid.tileSize - iconSize) / 2;
         GuiUtils.renderRuneIcon(graphics, rune.icon(), iconX, iconY, iconSize, rune.color(), learned || hovered);
         if (learned) graphics.renderOutline(tileX + 2, tileY + 2, grid.tileSize - 4, grid.tileSize - 4, 0xCCFFFFFF);
         else graphics.fill(tileX + 1, tileY + 1, tileX + grid.tileSize - 1, tileY + grid.tileSize - 1, 0x35080B0E);
      }

      RuneDefinition rune = runes.get(Math.max(0, Math.min(selected, runes.size() - 1)));
      renderDetails(graphics, bounds, grid, rune, vars != null && vars.learned_runes.contains(rune.idPath()));
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void renderDetails(GuiGraphics graphics, Bounds bounds, Grid grid, RuneDefinition rune, boolean learned) {
      int detailX = grid.compact ? bounds.x + 12 : grid.x + grid.panelWidth + 8;
      int detailY = grid.compact ? grid.y + grid.panelHeight + 5 : grid.y - 6;
      int detailWidth = grid.compact ? bounds.width - 24 : bounds.x + bounds.width - 12 - detailX;
      int detailHeight = bounds.y + bounds.height - 36 - detailY;
      GuiUtils.renderArcanePanel(graphics, detailX, detailY, detailWidth, detailHeight, rune.color());
      int textX = detailX + 10;
      int textY = detailY + 8;
      graphics.drawString(font, Component.literal(rune.displayName()), textX, textY,
         learned ? 0xFFFFFFFF : 0xFFE5EEF2, false);
      if (grid.compact) {
         String summary = semanticValue(rune.semantic(RunePosition.TRIGGER)).getString() + " / "
            + semanticValue(rune.semantic(RunePosition.EFFECT)).getString() + " / "
            + semanticValue(rune.semantic(RunePosition.MODIFIER)).getString() + " / "
            + semanticValue(rune.semantic(RunePosition.TERMINAL)).getString();
         graphics.drawString(font, font.plainSubstrByWidth(summary, detailWidth - 20), textX, textY + 15,
            learned ? 0xFFFFFFFF : 0xFFE5EEF2, false);
         return;
      }
      drawSemantic(graphics, textX, textY + 24, detailWidth - 20, "gui.typemoonworld.rune.trigger", rune.semantic(RunePosition.TRIGGER));
      drawSemantic(graphics, textX, textY + 44, detailWidth - 20, "gui.typemoonworld.rune.effect", rune.semantic(RunePosition.EFFECT));
      drawSemantic(graphics, textX, textY + 64, detailWidth - 20, "gui.typemoonworld.rune.modifier", rune.semantic(RunePosition.MODIFIER));
      drawSemantic(graphics, textX, textY + 84, detailWidth - 20, "gui.typemoonworld.rune.terminal", rune.semantic(RunePosition.TERMINAL));
   }

   private void drawSemantic(GuiGraphics graphics, int x, int y, int maxWidth, String key, String value) {
      graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(key, semanticValue(value)).getString(), maxWidth),
         x, y, GuiUtils.ARCANE_TEXT, false);
   }

   private Component semanticValue(String value) {
      String key = "gui.typemoonworld.rune.semantic." + value;
      Component translated = Component.translatable(key);
      return translated.getString().equals(key) ? Component.literal(value) : translated;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      Grid grid = grid(bounds());
      List<RuneDefinition> runes = RuneRegistry.all();
      for (int i = 0; i < runes.size(); i++) {
         if (inside(mouseX, mouseY, grid.tileX(i), grid.tileY(i), grid.tileSize, grid.tileSize)) {
            selected = i;
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private Bounds bounds() {
      int panelWidth = Math.min(Math.max(300, width - 12), Math.min(MAX_WIDTH, width));
      int panelHeight = Math.min(Math.max(218, height - 12), Math.min(MAX_HEIGHT, height));
      return new Bounds((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
   }

   private Grid grid(Bounds bounds) {
      boolean compact = bounds.width < 500;
      int columns = 8;
      int gap = compact ? 3 : 5;
      int availableWidth = compact ? bounds.width - 24 : Math.min(300, bounds.width - 230);
      int tileSize = Math.max(22, Math.min(32, (availableWidth - gap * (columns - 1)) / columns));
      int rows = (RuneRegistry.all().size() + columns - 1) / columns;
      int panelWidth = tileSize * columns + gap * (columns - 1) + 12;
      int panelHeight = tileSize * rows + gap * (rows - 1) + 12;
      return new Grid(bounds.x + 12, bounds.y + 40, columns, gap, tileSize, panelWidth, panelHeight, compact);
   }

   private static boolean inside(double mouseX, double mouseY, int x, int y, int areaWidth, int areaHeight) {
      return mouseX >= x && mouseX < x + areaWidth && mouseY >= y && mouseY < y + areaHeight;
   }

   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }

   private record Bounds(int x, int y, int width, int height) { }
   private record Grid(int x, int y, int columns, int gap, int tileSize, int panelWidth, int panelHeight, boolean compact) {
      int tileX(int index) { return x + index % columns * (tileSize + gap); }
      int tileY(int index) { return y + index / columns * (tileSize + gap); }
   }
}
