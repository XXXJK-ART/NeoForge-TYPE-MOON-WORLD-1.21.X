package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardKeybindConfig;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardSkillInputController;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import org.lwjgl.glfw.GLFW;

/** A local profile for the active servant, with global bindings as its defaults. */
public class ServantCardKeybindScreen extends Screen {
   private static final int ACCENT = 0xFF70DFEB;
   private static final int ROW_COUNT = ServantCardKeybindConfig.SLOT_COUNT;
   private final String servantId;
   private final NeonButton[] keyButtons = new NeonButton[ROW_COUNT];
   private int leftPos, topPos, windowWidth, windowHeight, rowHeight, visibleRows, firstRow;
   private int listeningSlot = -1;
   private Component feedback = Component.empty();
   private boolean feedbackError;

   public ServantCardKeybindScreen(String servantId) {
      super(text("gui.typemoonworld.servant_card.keybinds"));
      this.servantId = servantId;
   }

   @Override
   protected void init() {
      this.windowWidth = Math.min(528, this.width - 16);
      this.windowHeight = Math.min(418, this.height - 16);
      this.leftPos = (this.width - this.windowWidth) / 2;
      this.topPos = (this.height - this.windowHeight) / 2;
      this.rowHeight = this.windowHeight < 300 ? 24 : 29;
      this.visibleRows = Mth.clamp((this.windowHeight - 122) / this.rowHeight, 1, ROW_COUNT);
      this.firstRow = Mth.clamp(this.firstRow, 0, ROW_COUNT - this.visibleRows);
      this.buildWidgets();
   }

   private int listTop() { return this.topPos + 66; }
   private int keyX() { return this.leftPos + this.windowWidth - 103; }

   private void buildWidgets() {
      this.clearWidgets();
      java.util.Arrays.fill(this.keyButtons, null);
      for (int row = 0; row < this.visibleRows; row++) {
         int slot = this.firstRow + row;
         int y = this.listTop() + row * this.rowHeight;
         NeonButton button = new NeonButton(this.keyX(), y + 3, 82, this.rowHeight - 6, this.keyLabel(slot), pressed -> {
            this.listeningSlot = slot;
            this.feedback = text("gui.typemoonworld.servant_card.keybinds.capture_help");
            this.feedbackError = false;
            this.refreshButtons();
         }, ACCENT).setArcaneStyle(true).setCompactStyle(true);
         button.active = this.occupied(slot);
         button.setTooltip(Tooltip.create(text("gui.typemoonworld.servant_card.keybinds.edit_hint")));
         this.keyButtons[slot] = this.addRenderableWidget(button);
      }
      int footerY = this.topPos + this.windowHeight - 29;
      this.addRenderableWidget(new NeonButton(this.leftPos + 12, footerY, 98, 20,
         text("gui.typemoonworld.servant_card.keybinds.reset"), button -> {
            ServantCardKeybindConfig.reset(this.servantId);
            this.listeningSlot = -1;
            this.feedback = text("gui.typemoonworld.servant_card.keybinds.inherited");
            this.feedbackError = false;
            this.refreshButtons();
         }, ACCENT).setArcaneStyle(true));
      this.addRenderableWidget(new NeonButton(this.leftPos + this.windowWidth - 96, footerY, 84, 20,
         text("gui.done"), button -> this.onClose(), ACCENT).setArcaneStyle(true));
   }

   @Override
   public void tick() {
      var player = Minecraft.getInstance().player;
      if (player == null) {
         this.onClose();
         return;
      }
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !this.servantId.equals(vars.servant_card_id)) this.onClose();
   }

   @Override
   public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick) { }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      int right = this.leftPos + this.windowWidth;
      int bottom = this.topPos + this.windowHeight;
      gui.fill(0, 0, this.width, this.height, 0x66070E18);
      gui.fill(this.leftPos - 2, this.topPos - 2, right + 2, bottom + 2, 0xD509111C);
      gui.fill(this.leftPos, this.topPos, right, bottom, 0xF0152332);
      gui.renderOutline(this.leftPos, this.topPos, this.windowWidth, this.windowHeight, 0xFF466575);
      gui.fill(this.leftPos, this.topPos, right, this.topPos + 2, ACCENT);
      gui.fillGradient(this.leftPos + 1, this.topPos + 2, right - 1, this.topPos + 47, 0xFF233C50, 0xFF152332);
      gui.drawString(this.font, this.title, this.leftPos + 13, this.topPos + 12, 0xFFF1FCFF, false);
      Component servant = text("item.typemoonworld.servant_card_" + this.servantId);
      gui.drawString(this.font, this.fit(servant, this.windowWidth - 28), this.leftPos + 13, this.topPos + 29, ACCENT, false);
      gui.drawString(this.font, text("gui.typemoonworld.servant_card.keybinds.skill"), this.leftPos + 35, this.topPos + 53, 0xFFADC4D0, false);
      gui.drawCenteredString(this.font, text("gui.typemoonworld.servant_card.keybinds.input"), this.keyX() - 32, this.topPos + 53, 0xFFADC4D0);
      gui.drawCenteredString(this.font, text("gui.typemoonworld.servant_card.keybinds.binding"), this.keyX() + 41, this.topPos + 53, 0xFFADC4D0);
      Component hoveredName = null;
      for (int row = 0; row < this.visibleRows; row++) {
         int slot = this.firstRow + row;
         int y = this.listTop() + row * this.rowHeight;
         boolean occupied = this.occupied(slot);
         boolean selected = this.listeningSlot == slot;
         boolean hovered = mouseX >= this.leftPos + 10 && mouseX < right - 12 && mouseY >= y && mouseY < y + this.rowHeight;
         gui.fill(this.leftPos + 10, y, right - 12, y + this.rowHeight - 2,
            selected ? 0xFF2A4F60 : hovered ? 0xFF263E50 : row % 2 == 0 ? 0xFF1C3041 : 0xFF192A3A);
         if (selected) gui.fill(this.leftPos + 10, y, this.leftPos + 12, y + this.rowHeight - 2, ACCENT);
         int textY = y + (this.rowHeight - 10) / 2;
         gui.drawString(this.font, String.valueOf(slot), this.leftPos + 20, textY, occupied ? ACCENT : 0xFF6B8292, false);
         Component name = occupied ? text(ServantCardTransformManager.skillTranslationKey(this.servantId, slot, false)) : text("hud.typemoonworld.servant_card.none");
         int nameWidth = this.keyX() - this.leftPos - 97;
         gui.drawString(this.font, this.fit(name, nameWidth), this.leftPos + 35, textY, occupied ? 0xFFE8F4FA : 0xFF79909E, false);
         if (occupied) {
            boolean hold = ServantCardSkillInputController.isHoldSkill(this.servantId, slot);
            gui.drawCenteredString(this.font, text(hold ? "gui.typemoonworld.servant_card.keybinds.hold" : "gui.typemoonworld.servant_card.keybinds.tap"),
               this.keyX() - 32, textY, hold ? 0xFFF1CA83 : 0xFFA6C5D6);
         }
         if (hovered && mouseX < this.keyX() - 59 && this.font.width(name) > nameWidth) hoveredName = name;
      }
      if (this.visibleRows < ROW_COUNT) {
         GuiUtils.renderScrollBar(gui, right - 8, this.listTop(), this.visibleRows * this.rowHeight - 2,
            this.firstRow / (float)(ROW_COUNT - this.visibleRows), this.visibleRows / (float)ROW_COUNT, ACCENT);
      }
      Component help = this.feedback.getString().isEmpty()
         ? text("gui.typemoonworld.servant_card.keybinds.select_hint", TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_SELECT.getTranslatedKeyMessage(), TypeMoonWorldModKeyMappings.SERVANT_CARD_CAST.getTranslatedKeyMessage())
         : this.feedback;
      gui.drawString(this.font, this.fit(help, this.windowWidth - 24), this.leftPos + 12, bottom - 46, this.feedbackError ? 0xFFFF9B91 : 0xFFB3CBD7, false);
      super.render(gui, mouseX, mouseY, partialTick);
      if (hoveredName != null) gui.renderTooltip(this.font, hoveredName, mouseX, mouseY);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.listeningSlot < 0) return super.keyPressed(keyCode, scanCode, modifiers);
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
         this.listeningSlot = -1;
         this.feedback = Component.empty();
         this.refreshButtons();
      } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
         this.assign(InputConstants.UNKNOWN);
      } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
         this.assign(InputConstants.getKey(keyCode, scanCode));
      }
      return true;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.listeningSlot < 0) return super.mouseClicked(mouseX, mouseY, button);
      this.assign(InputConstants.Type.MOUSE.getOrCreate(button));
      return true;
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.listeningSlot >= 0) return true;
      int next = Mth.clamp(this.firstRow - (int)Math.signum(scrollY), 0, ROW_COUNT - this.visibleRows);
      if (next != this.firstRow) {
         this.firstRow = next;
         this.buildWidgets();
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   private void assign(InputConstants.Key key) {
      if (ServantCardKeybindConfig.isReservedKey(key)) {
         this.feedback = text("gui.typemoonworld.servant_card.keybinds.reserved", key.getDisplayName());
         this.feedbackError = true;
      } else {
         ServantCardKeybindConfig.setKey(this.servantId, this.listeningSlot, key);
         this.listeningSlot = -1;
         this.feedback = text("gui.typemoonworld.servant_card.keybinds.saved");
         this.feedbackError = false;
      }
      this.refreshButtons();
   }

   private boolean occupied(int slot) {
      return !ServantCardTransformManager.skillTranslationKey(this.servantId, slot, false).isBlank();
   }

   private Component keyLabel(int slot) {
      return this.listeningSlot == slot ? text("gui.typemoonworld.servant_card.keybinds.listening")
         : Component.literal(ServantCardKeybindConfig.keyName(ServantCardKeybindConfig.keyFor(this.servantId, slot)));
   }

   private void refreshButtons() {
      for (int slot = 0; slot < ROW_COUNT; slot++) {
         if (this.keyButtons[slot] != null) this.keyButtons[slot].setMessage(this.keyLabel(slot));
         if (this.keyButtons[slot] != null) this.keyButtons[slot].setSelected(this.listeningSlot == slot).setSelectedColor(ACCENT);
      }
   }

   private String fit(Component value, int maxWidth) {
      if (this.font.width(value) <= maxWidth) return value.getString();
      return this.font.plainSubstrByWidth(value.getString(), Math.max(1, maxWidth - this.font.width("…"))) + "…";
   }

   private static Component text(String key, Object... args) { return Component.translatable(key, args); }
}