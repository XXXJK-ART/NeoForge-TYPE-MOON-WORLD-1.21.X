package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicCopyMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicCopyingTableMenu;

/** Arcane framed copying table UI. */
public class MagicCopyingTableScreen extends AbstractContainerScreen<MagicCopyingTableMenu> {
   private final List<String> magics = new ArrayList<>();
   private int cursor;
   private NeonButton previousButton;
   private NeonButton nextButton;
   private NeonButton copyButton;
   private EditBox forceBox;

   public MagicCopyingTableScreen(MagicCopyingTableMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      imageWidth = 260;
      imageHeight = 228;
   }

   @Override
   protected void init() {
      super.init();
      magics.clear();
      var vars = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      magics.addAll(MagicLearningStrategy.displayMagicIds(vars.learned_magics).stream()
         .filter(MagicLearningStrategy::canCopy).sorted().toList());
      if (MagicLearningStrategy.isLearned(vars, "contract_magecraft")) {
         magics.add(MagicCopyingTableMenu.SELF_GEAS_COPY_ID);
      }
      cursor = 0;
      previousButton = arcaneButton(leftPos + 78, topPos + 42, 22, 18, Component.literal("<"), b -> cycle(-1), GuiUtils.ARCANE_CYAN);
      nextButton = arcaneButton(leftPos + 154, topPos + 42, 22, 18, Component.literal(">"), b -> cycle(1), GuiUtils.ARCANE_CYAN);
      copyButton = arcaneButton(leftPos + 184, topPos + 92, 62, 20,
         Component.translatable("gui.typemoonworld.magic_copying_table.copy"), b -> sendCopy(), GuiUtils.ARCANE_GOLD);
      forceBox = new EditBox(this.font, leftPos + 186, topPos + 66, 56, 18, Component.translatable("gui.typemoonworld.magic_copying_table.force"));
      forceBox.setMaxLength(3);
      forceBox.setFilter(value -> value.isEmpty() || value.matches("[0-9]{0,3}"));
      forceBox.setValue(String.valueOf(maxSelfGeasForce(vars)));
      addRenderableWidget(previousButton);
      addRenderableWidget(nextButton);
      addRenderableWidget(forceBox);
      addRenderableWidget(copyButton);
      updateButtons();
   }

   private NeonButton arcaneButton(int x, int y, int w, int h, Component text, Button.OnPress press, int accent) {
      return new NeonButton(x, y, w, h, text, press, accent).setArcaneStyle(true).setCompactStyle(true);
   }

   private void cycle(int delta) {
      if (!magics.isEmpty()) cursor = Math.floorMod(cursor + delta, magics.size());
      updateButtons();
   }

   private void sendCopy() {
      if (magics.isEmpty()) {
         return;
      }
      String id = magics.get(cursor);
      if (MagicCopyingTableMenu.SELF_GEAS_COPY_ID.equals(id)) {
         PacketDistributor.sendToServer(new MagicCopyMessage(id, readForce()));
      } else {
         PacketDistributor.sendToServer(new MagicCopyMessage(id));
      }
   }

   private void updateButtons() {
      boolean available = !magics.isEmpty();
      previousButton.active = available && magics.size() > 1;
      nextButton.active = previousButton.active;
      copyButton.active = available;
      if (forceBox != null) {
         boolean selfGeas = available && MagicCopyingTableMenu.SELF_GEAS_COPY_ID.equals(magics.get(cursor));
         forceBox.visible = selfGeas;
         forceBox.active = selfGeas;
      }
   }

   @Override
   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      int x = leftPos;
      int y = topPos;
      GuiUtils.renderArcaneWindow(g, x, y, imageWidth, imageHeight, GuiUtils.ARCANE_CYAN);
      GuiUtils.renderArcanePanel(g, x + 8, y + 32, 56, 82, GuiUtils.ARCANE_CYAN);
      GuiUtils.renderArcanePanel(g, x + 70, y + 32, 104, 82, GuiUtils.ARCANE_GOLD);
      GuiUtils.renderArcanePanel(g, x + 180, y + 32, 70, 82, GuiUtils.ARCANE_VALID);
      GuiUtils.renderArcanePanel(g, x + 8, y + 120, 224, 100, GuiUtils.ARCANE_CYAN);
      GuiUtils.renderArcaneSlot(g, x + 25, y + 43, 18, GuiUtils.ARCANE_CYAN, true);
      GuiUtils.renderArcaneSlot(g, x + 25, y + 74, 18, GuiUtils.ARCANE_GOLD, true);
      for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
         GuiUtils.renderArcaneSlot(g, x + 12 + col * 18, y + 135 + row * 18, 18, GuiUtils.ARCANE_BORDER, false);
      }
      for (int col = 0; col < 9; col++) GuiUtils.renderArcaneSlot(g, x + 12 + col * 18, y + 193, 18, GuiUtils.ARCANE_BORDER, false);
   }

   @Override
   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      g.drawCenteredString(font, Component.translatable("gui.typemoonworld.magic_copying_table.title"), imageWidth / 2, 8, GuiUtils.ARCANE_TEXT);
      g.drawString(font, Component.translatable("gui.typemoonworld.magic_copying_table.paper"), 12, 36, GuiUtils.ARCANE_TEXT_MUTED, false);
      g.drawString(font, Component.translatable("gui.typemoonworld.magic_copying_table.magic"), 78, 36, GuiUtils.ARCANE_TEXT_MUTED, false);
      g.drawString(font, Component.translatable("gui.typemoonworld.magic_copying_table.status"), 186, 36, GuiUtils.ARCANE_TEXT_MUTED, false);
      if (!magics.isEmpty()) {
         String id = magics.get(cursor);
         var vars = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean selfGeas = MagicCopyingTableMenu.SELF_GEAS_COPY_ID.equals(id);
         double proficiency = MagicProficiencyService.get(vars, selfGeas ? "contract_magecraft" : id);
         int magicColor = MagicUiColors.colorFor(id, false);
         g.drawCenteredString(font, displayName(id), 122, 58, magicColor);
         g.drawCenteredString(font, Component.literal(String.format("%.1f%%", proficiency)), 122, 72, GuiUtils.ARCANE_TEXT_MUTED);
         GuiUtils.renderProgressBar(g, 80, 84, 84, 6, (float)proficiency / 100.0F, magicColor);
         if (selfGeas) {
            g.drawString(font, Component.translatable("gui.typemoonworld.magic_copying_table.force"), 186, 58, magicColor, false);
            g.drawString(font, Component.literal("Max " + maxSelfGeasForce(vars)), 186, 68, GuiUtils.ARCANE_TEXT_MUTED, false);
         } else {
            g.drawString(font, Component.literal("P " + (int)Math.ceil(proficiency)), 186, 58, magicColor, false);
            g.drawString(font, Component.translatable("gui.typemoonworld.magic_copying_table.status.ready"), 186, 72, GuiUtils.ARCANE_TEXT_MUTED, false);
         }
      } else {
         g.drawCenteredString(font, Component.translatable("gui.typemoonworld.magic_copying_table.empty"), 122, 68, GuiUtils.ARCANE_TEXT_MUTED);
      }
      g.drawString(font, playerInventoryTitle, 12, 124, GuiUtils.ARCANE_TEXT_MUTED, false);
   }

   private static Component displayName(String magicId) {
      if (MagicCopyingTableMenu.SELF_GEAS_COPY_ID.equals(magicId)) {
         return Component.translatable("item.typemoonworld.self_geas_scroll");
      }
      return Component.translatable("magic.typemoonworld." + magicId + ".name");
   }

   private int readForce() {
      try {
         int value = Integer.parseInt(forceBox == null || forceBox.getValue().isEmpty() ? "0" : forceBox.getValue());
         var vars = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return Math.max(0, Math.min(maxSelfGeasForce(vars), value));
      } catch (NumberFormatException ignored) {
         return 0;
      }
   }

   private static int maxSelfGeasForce(TypeMoonWorldModVariables.PlayerVariables vars) {
      return Math.max(0, Math.min(100, (int)Math.floor(MagicProficiencyService.get(vars, "contract_magecraft"))));
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      renderBackground(g, mouseX, mouseY, partialTick);
      super.render(g, mouseX, mouseY, partialTick);
      renderSlotMarkers(g);
      renderTooltip(g, mouseX, mouseY);
   }

   private void renderSlotMarkers(GuiGraphics g) {
      for (int i = 0; i < menu.slots.size(); i++) {
         int accent = i == 0 ? GuiUtils.ARCANE_CYAN : i == 1 ? GuiUtils.ARCANE_GOLD : GuiUtils.ARCANE_TEXT_MUTED;
         GuiUtils.renderArcaneSlotMarker(g, leftPos + menu.slots.get(i).x - 1, topPos + menu.slots.get(i).y - 1, 18, accent, i < 2);
      }
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      GuiUtils.renderScreenBaseBackdrop(g, width, height);
   }
}
