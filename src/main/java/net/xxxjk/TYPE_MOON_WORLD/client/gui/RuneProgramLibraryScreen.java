package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgram;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramCostService;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramService;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicWheelSlotEditMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.RuneProgramMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Player-owned rune programs with a selectable list and explicit actions. */
public final class RuneProgramLibraryScreen extends Screen {
   private static final int ROW_HEIGHT = 22;
   private final Screen parent;
   private int selected = -1;
   private int scrollOffset;

   public RuneProgramLibraryScreen(Screen parent) {
      super(Component.translatable("gui.typemoonworld.rune.library"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      Bounds bounds = bounds();
      boolean compact = bounds.width < 520;
      int gap = 5;
      int buttonWidth = compact ? Math.max(68, (bounds.width - 24 - gap * 2) / 3)
         : Math.max(64, (bounds.width - 24 - gap * 5) / 6);
      int startX = bounds.x + 12;
      int startY = bounds.y + bounds.height - 12 - (compact ? 44 : 22);
      addActionButton(startX, startY, buttonWidth, Component.translatable("gui.back"), b -> onClose(), GuiUtils.ARCANE_BORDER);
      addActionButton(buttonX(startX, buttonWidth, gap, 1, compact), buttonY(startY, 1, compact), buttonWidth,
         Component.translatable("gui.typemoonworld.rune.new"), b -> minecraft.setScreen(new RuneProgramEditorScreen(this, null)), GuiUtils.ARCANE_CYAN);
      addActionButton(buttonX(startX, buttonWidth, gap, 2, compact), buttonY(startY, 2, compact), buttonWidth,
         Component.translatable("gui.typemoonworld.rune.edit"), b -> edit(), GuiUtils.ARCANE_CYAN);
      addActionButton(buttonX(startX, buttonWidth, gap, 3, compact), buttonY(startY, 3, compact), buttonWidth,
         Component.translatable("gui.typemoonworld.rune.copy"), b -> action(RuneProgramMessage.COPY), GuiUtils.ARCANE_GOLD);
      addActionButton(buttonX(startX, buttonWidth, gap, 4, compact), buttonY(startY, 4, compact), buttonWidth,
         Component.translatable("gui.typemoonworld.rune.delete"), b -> action(RuneProgramMessage.DELETE), GuiUtils.ARCANE_DANGER);
      addActionButton(buttonX(startX, buttonWidth, gap, 5, compact), buttonY(startY, 5, compact), buttonWidth,
         Component.translatable("gui.typemoonworld.rune.add_wheel"), b -> addToWheel(), GuiUtils.ARCANE_VALID);
   }

   private int buttonX(int startX, int buttonWidth, int gap, int index, boolean compact) {
      return startX + (index % (compact ? 3 : 6)) * (buttonWidth + gap);
   }

   private int buttonY(int startY, int index, boolean compact) {
      return startY + (compact ? index / 3 * 22 : 0);
   }

   private void addActionButton(int x, int y, int buttonWidth, Component label,
      net.minecraft.client.gui.components.Button.OnPress action, int color) {
      addRenderableWidget(new NeonButton(x, y, buttonWidth, 20, label, action, color).setArcaneStyle(true).setCompactStyle(true));
   }

   private RuneProgram selectedProgram() {
      if (minecraft == null || minecraft.player == null) return null;
      List<RuneProgram> programs = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).rune_programs;
      return selected >= 0 && selected < programs.size() ? programs.get(selected) : null;
   }

   private void edit() {
      RuneProgram program = selectedProgram();
      if (program != null) minecraft.setScreen(new RuneProgramConfigScreen(this, program));
   }

   private void action(int action) {
      RuneProgram program = selectedProgram();
      if (program == null) return;
      PacketDistributor.sendToServer(new RuneProgramMessage(action, null, program.uuid().toString()), new CustomPacketPayload[0]);
      if (action == RuneProgramMessage.DELETE) selected = -1;
   }

   private void addToWheel() {
      RuneProgram program = selectedProgram();
      if (program == null || minecraft.player == null) return;
      TypeMoonWorldModVariables.PlayerVariables vars = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      int slot = 0;
      for (int i = 0; i < 12; i++) {
         if (vars.getWheelSlotEntry(vars.active_wheel_index, i).isEmpty()) { slot = i; break; }
      }
      PacketDistributor.sendToServer(new MagicWheelSlotEditMessage(MagicWheelSlotEditMessage.ACTION_SET,
         vars.active_wheel_index, slot, -1, "rune_program", RuneProgramService.dynamicId(program.uuid()),
         new CompoundTag(), "", program.displayName()), new CustomPacketPayload[0]);
   }

   @Override
   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBackdrop(graphics, width, height);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      renderBackground(graphics, mouseX, mouseY, partialTick);
      Bounds bounds = bounds();
      ListArea area = listArea(bounds);
      GuiUtils.renderArcaneWindow(graphics, bounds.x, bounds.y, bounds.width, bounds.height, GuiUtils.ARCANE_GOLD);
      graphics.drawCenteredString(font, title, width / 2, bounds.y + 9, GuiUtils.ARCANE_TEXT);
      GuiUtils.renderArcanePanel(graphics, area.x, area.y, area.width, area.height, GuiUtils.ARCANE_GOLD);

      List<RuneProgram> programs = minecraft.player == null ? List.of()
         : minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).rune_programs;
      clampScroll(programs.size(), area.visibleRows);
      for (int row = 0; row < area.visibleRows; row++) {
         int index = scrollOffset + row;
         if (index >= programs.size()) break;
         RuneProgram program = programs.get(index);
         int rowY = area.y + 5 + row * ROW_HEIGHT;
         boolean hovered = inside(mouseX, mouseY, area.x + 5, rowY, area.width - 10, ROW_HEIGHT - 2);
         GuiUtils.renderChoiceTile(graphics, area.x + 5, rowY, area.width - 10, ROW_HEIGHT - 2,
            GuiUtils.ARCANE_GOLD, index == selected || hovered, true);
         String label = (index + 1) + ". " + program.displayName();
         graphics.drawString(font, font.plainSubstrByWidth(label, area.width - 30), area.x + 13, rowY + 6,
            index == selected ? GuiUtils.ARCANE_TEXT : 0xFFF3F8FB, false);
      }
      if (programs.size() > area.visibleRows) {
         float progress = scrollOffset / (float)Math.max(1, programs.size() - area.visibleRows);
         GuiUtils.renderScrollBar(graphics, area.x + area.width - 8, area.y + 5, area.height - 10,
            progress, area.visibleRows / (float)programs.size(), GuiUtils.ARCANE_GOLD);
      }
      renderDetails(graphics, bounds, area, selectedProgram());
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void renderDetails(GuiGraphics graphics, Bounds bounds, ListArea area, RuneProgram program) {
      if (area.compact) return;
      int detailX = area.x + area.width + 8;
      int detailWidth = bounds.x + bounds.width - 12 - detailX;
      GuiUtils.renderArcanePanel(graphics, detailX, area.y, detailWidth, area.height, GuiUtils.ARCANE_CYAN);
      if (program == null) {
         graphics.drawCenteredString(font, Component.translatable("gui.typemoonworld.rune.library"),
            detailX + detailWidth / 2, area.y + area.height / 2 - 4, GuiUtils.ARCANE_TEXT_MUTED);
         return;
      }
      graphics.drawString(font, font.plainSubstrByWidth(program.displayName(), detailWidth - 20), detailX + 10, area.y + 12, GuiUtils.ARCANE_TEXT, false);
      graphics.drawString(font, Component.translatable("gui.typemoonworld.rune.mode", modeLabel(program)),
         detailX + 10, area.y + 36, 0xFFEAF3F7, false);
      graphics.drawString(font, Component.translatable("gui.typemoonworld.rune.cost",
         String.format("%.0f", RuneProgramCostService.calculate(program))), detailX + 10, area.y + 54, 0xFFEAF3F7, false);
      RuneProgramValidationResultView.render(graphics, font, program, detailX + 10, area.y + 78);
   }

   private static Component modeLabel(RuneProgram program) {
      String key = "gui.typemoonworld.rune.mode." + program.releaseMode().name().toLowerCase(java.util.Locale.ROOT);
      Component translated = Component.translatable(key);
      return translated.getString().equals(key) ? Component.literal(program.releaseMode().name()) : translated;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      ListArea area = listArea(bounds());
      if (inside(mouseX, mouseY, area.x + 5, area.y + 5, area.width - 10, area.visibleRows * ROW_HEIGHT)) {
         int row = (int)(mouseY - area.y - 5) / ROW_HEIGHT;
         int candidate = scrollOffset + row;
         if (minecraft.player != null && candidate < minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).rune_programs.size()) {
            selected = candidate;
            if (button == 1) edit();
         }
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      ListArea area = listArea(bounds());
      if (!inside(mouseX, mouseY, area.x, area.y, area.width, area.height) || minecraft.player == null) {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
      int size = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).rune_programs.size();
      scrollOffset = Math.max(0, Math.min(Math.max(0, size - area.visibleRows), scrollOffset - (int)Math.signum(scrollY)));
      return true;
   }

   private Bounds bounds() {
      int panelWidth = Math.min(Math.max(300, width - 12), Math.min(620, width));
      int panelHeight = Math.min(Math.max(220, height - 12), Math.min(300, height));
      return new Bounds((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
   }

   private ListArea listArea(Bounds bounds) {
      boolean compact = bounds.width < 520;
      int footerHeight = compact ? 56 : 34;
      int areaY = bounds.y + 36;
      int areaHeight = bounds.height - footerHeight - 42;
      int areaWidth = compact ? bounds.width - 24 : Math.max(220, bounds.width * 3 / 5);
      return new ListArea(bounds.x + 12, areaY, areaWidth, areaHeight,
         Math.max(1, (areaHeight - 10) / ROW_HEIGHT), compact);
   }

   private void clampScroll(int size, int visibleRows) {
      scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, size - visibleRows)));
   }

   private static boolean inside(double mouseX, double mouseY, int x, int y, int areaWidth, int areaHeight) {
      return mouseX >= x && mouseX < x + areaWidth && mouseY >= y && mouseY < y + areaHeight;
   }

   @Override public void onClose() { minecraft.setScreen(parent); }
   @Override public boolean isPauseScreen() { return false; }

   private record Bounds(int x, int y, int width, int height) { }
   private record ListArea(int x, int y, int width, int height, int visibleRows, boolean compact) { }
}
