package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardKeybindConfig;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardSkillInputController;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import org.lwjgl.glfw.GLFW;

/** Per-servant local keybind editor, available only while a servant card is active. */
public class ServantCardKeybindScreen extends Screen {
   private static final int WINDOW_WIDTH = 386;
   private static final int WINDOW_HEIGHT = 292;
   private final String servantId;
   private final Button[] keyButtons = new Button[ServantCardKeybindConfig.SLOT_COUNT];
   private final Button[] holdButtons = new Button[ServantCardKeybindConfig.SLOT_COUNT];
   private int leftPos;
   private int topPos;
   private int listeningSlot = -1;

   public ServantCardKeybindScreen(String servantId) {
      super(Component.translatable("gui.typemoonworld.servant_card.keybinds"));
      this.servantId = servantId;
   }

   @Override
   protected void init() {
      this.leftPos = (this.width - Math.min(WINDOW_WIDTH, this.width - 12)) / 2;
      this.topPos = (this.height - Math.min(WINDOW_HEIGHT, this.height - 12)) / 2;
      int keyX = this.leftPos + 234;
      int holdX = this.leftPos + 310;
      for (int slot = 0; slot < ServantCardKeybindConfig.SLOT_COUNT; slot++) {
         int rowY = this.topPos + 39 + slot * 20;
         final int slotIndex = slot;
         this.keyButtons[slot] = this.addRenderableWidget(
            Button.builder(this.keyButtonLabel(slot), button -> {
               this.listeningSlot = slotIndex;
               this.refreshButtons();
            }).bounds(keyX, rowY, 70, 18).build()
         );
         this.holdButtons[slot] = this.addRenderableWidget(
            Button.builder(this.holdButtonLabel(slot), button -> {
               if (!ServantCardSkillInputController.isForcedHoldSkill(this.servantId, slotIndex)) {
                  return;
               }
               ServantCardKeybindConfig.setHoldSlot(this.servantId, slotIndex, true);
               this.refreshButtons();
            }).bounds(holdX, rowY, 60, 18).build()
         );
         this.holdButtons[slot].active = false;
      }
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.typemoonworld.servant_card.keybinds.reset"), button -> {
            ServantCardKeybindConfig.reset(this.servantId);
            this.listeningSlot = -1;
            this.refreshButtons();
         }).bounds(this.leftPos + 15, this.topPos + 255, 88, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), button -> this.onClose()).bounds(this.leftPos + 283, this.topPos + 255, 88, 20).build()
      );
   }

   @Override
   public void tick() {
      super.tick();
      Player player = Minecraft.getInstance().player;
      if (player == null) {
         this.onClose();
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !this.servantId.equals(vars.servant_card_id)) {
         this.onClose();
      }
   }

   @Override
   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      int windowWidth = Math.min(WINDOW_WIDTH, this.width - 12);
      int windowHeight = Math.min(WINDOW_HEIGHT, this.height - 12);
      GuiUtils.renderScreenBaseBackdrop(gui, this.width, this.height);
      GuiUtils.renderArcaneWindow(gui, this.leftPos, this.topPos, windowWidth, windowHeight, GuiUtils.ARCANE_CYAN);
      gui.drawCenteredString(this.font, this.title, this.leftPos + windowWidth / 2, this.topPos + 9, GuiUtils.ARCANE_TEXT);
      for (int slot = 0; slot < ServantCardKeybindConfig.SLOT_COUNT; slot++) {
         int rowY = this.topPos + 39 + slot * 20;
         boolean occupied = !ServantCardTransformManager.skillTranslationKey(this.servantId, slot, false).isBlank();
         GuiUtils.renderChoiceTile(gui, this.leftPos + 12, rowY - 1, windowWidth - 24, 19, occupied ? GuiUtils.ARCANE_CYAN : GuiUtils.ARCANE_BORDER, false, occupied);
         String translationKey = ServantCardTransformManager.skillTranslationKey(this.servantId, slot, false);
         Component name = occupied ? Component.translatable(translationKey) : Component.translatable("hud.typemoonworld.servant_card.none");
         String text = slot + "  " + this.font.plainSubstrByWidth(name.getString(), 185);
         gui.drawString(this.font, text, this.leftPos + 19, rowY + 5, occupied ? GuiUtils.ARCANE_TEXT : GuiUtils.ARCANE_TEXT_MUTED);
      }
      super.render(gui, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.listeningSlot >= 0) {
         if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.listeningSlot = -1;
         } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            ServantCardKeybindConfig.setKey(this.servantId, this.listeningSlot, InputConstants.UNKNOWN);
            this.listeningSlot = -1;
         } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
            if (!ServantCardKeybindConfig.isReservedKey(key)) {
               ServantCardKeybindConfig.setKey(this.servantId, this.listeningSlot, key);
               this.listeningSlot = -1;
            }
         }
         this.refreshButtons();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.listeningSlot >= 0) {
         ServantCardKeybindConfig.setKey(this.servantId, this.listeningSlot, InputConstants.Type.MOUSE.getOrCreate(button));
         this.listeningSlot = -1;
         this.refreshButtons();
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   private Component keyButtonLabel(int slot) {
      return this.listeningSlot == slot
         ? Component.translatable("gui.typemoonworld.servant_card.keybinds.listening")
         : Component.literal(ServantCardKeybindConfig.keyName(ServantCardKeybindConfig.keyFor(this.servantId, slot)));
   }

   private Component holdButtonLabel(int slot) {
      boolean hold = ServantCardSkillInputController.isHoldSkill(this.servantId, slot);
      return Component.translatable(hold ? "gui.typemoonworld.servant_card.keybinds.hold" : "gui.typemoonworld.servant_card.keybinds.tap");
   }

   private void refreshButtons() {
      for (int slot = 0; slot < ServantCardKeybindConfig.SLOT_COUNT; slot++) {
         if (this.keyButtons[slot] != null) {
            this.keyButtons[slot].setMessage(this.keyButtonLabel(slot));
         }
         if (this.holdButtons[slot] != null) {
            this.holdButtons[slot].setMessage(this.holdButtonLabel(slot));
         }
      }
   }
}
