package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicModeSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.lwjgl.glfw.GLFW;

public class MagicModeSwitcherScreen extends Screen {
   private final List<Component> modes = new ArrayList<>();
   private final List<Integer> modeIds = new ArrayList<>();
   private static final ResourceLocation[] MODE_ICONS = new ResourceLocation[]{
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/gui/mode/mode_0.png"),
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/gui/mode/mode_1.png"),
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/gui/mode/mode_2.png"),
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/gui/mode/mode_3.png"),
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/gui/mode/mode_4.png")
   };
   private static final ResourceLocation[] JEWEL_ICONS = new ResourceLocation[]{
      ResourceLocation.parse("typemoonworld:textures/item/carved_ruby.png"),
      ResourceLocation.parse("typemoonworld:textures/item/carved_sapphire.png"),
      ResourceLocation.parse("typemoonworld:textures/item/carved_emerald.png"),
      ResourceLocation.parse("typemoonworld:textures/item/carved_topaz.png"),
      ResourceLocation.parse("typemoonworld:textures/item/carved_cyan_gemstone.png"),
      ResourceLocation.parse("typemoonworld:textures/item/carved_white_gemstone.png")
   };
   private int selectedIndex = 0;
   private boolean isClosing = false;
   private boolean isReinforcement = false;
   private boolean isGravity = false;
   private int reinforcementStage = 0;
   private int selectedTarget = 0;
   private int selectedBodyPart = 0;
   private int gravityStage = 0;

   public MagicModeSwitcherScreen(int currentMode) {
      super(Component.translatable("gui.typemoonworld.mode_switcher.title"));
      this.initModes(currentMode);
   }

   private void initModes(int currentMode) {
      this.modes.clear();
      this.modeIds.clear();
      this.isReinforcement = false;
      this.isGravity = false;
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         String currentMagic = "";
         if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
            currentMagic = vars.selected_magics.get(vars.current_magic_index);
         }

         if ("sword_barrel_full_open".equals(currentMagic)) {
            this.addMode(0, Component.translatable("gui.typemoonworld.mode.aoe"));
            this.addMode(1, Component.translatable("gui.typemoonworld.mode.aiming"));
            this.addMode(2, Component.translatable("gui.typemoonworld.mode.focus"));
            this.addMode(3, Component.translatable("gui.typemoonworld.mode.broken_phantasm"));
            this.addMode(4, Component.translatable("gui.typemoonworld.mode.clear"));
         } else if ("jewel_magic_shoot".equals(currentMagic)) {
            if (vars.learned_magics.contains("jewel_magic_shoot")) {
               this.addMode(0, Component.translatable("gui.typemoonworld.mode.ruby").withStyle(ChatFormatting.RED));
               this.addMode(1, Component.translatable("gui.typemoonworld.mode.sapphire").withStyle(ChatFormatting.BLUE));
               this.addMode(2, Component.translatable("gui.typemoonworld.mode.emerald").withStyle(ChatFormatting.GREEN));
               this.addMode(3, Component.translatable("gui.typemoonworld.mode.topaz").withStyle(ChatFormatting.YELLOW));
               this.addMode(4, Component.translatable("gui.typemoonworld.mode.cyan").withStyle(ChatFormatting.AQUA));
               this.addMode(5, Component.translatable("gui.typemoonworld.mode.random").withStyle(ChatFormatting.WHITE));
            }
         } else if ("jewel_machine_gun".equals(currentMagic)) {
            this.addMode(0, Component.translatable("gui.typemoonworld.mode.auto"));
         } else if ("jewel_magic_release".equals(currentMagic)) {
            if (vars.learned_magics.contains("jewel_magic_release")) {
               this.addMode(0, Component.translatable("gui.typemoonworld.mode.sapphire").withStyle(ChatFormatting.BLUE));
               this.addMode(1, Component.translatable("gui.typemoonworld.mode.emerald").withStyle(ChatFormatting.GREEN));
               this.addMode(2, Component.translatable("gui.typemoonworld.mode.topaz").withStyle(ChatFormatting.YELLOW));
               this.addMode(3, Component.translatable("gui.typemoonworld.mode.cyan").withStyle(ChatFormatting.AQUA));
            }
         } else if ("reinforcement".equals(currentMagic)
            || "reinforcement_self".equals(currentMagic)
            || "reinforcement_other".equals(currentMagic)
            || "reinforcement_item".equals(currentMagic)) {
            this.isReinforcement = true;
            if (this.reinforcementStage == 0) {
               this.addMode(0, Component.translatable("gui.typemoonworld.mode.self"));
               this.addMode(1, Component.translatable("gui.typemoonworld.mode.other"));
               this.addMode(2, Component.translatable("gui.typemoonworld.mode.item"));
               this.addMode(3, Component.translatable("gui.typemoonworld.mode.cancel"));
            } else if (this.reinforcementStage == 1) {
               this.addMode(0, Component.translatable("gui.typemoonworld.mode.body"));
               this.addMode(1, Component.translatable("gui.typemoonworld.mode.hand"));
               this.addMode(2, Component.translatable("gui.typemoonworld.mode.leg"));
               this.addMode(3, Component.translatable("gui.typemoonworld.mode.eye"));
            } else if (this.reinforcementStage == 2) {
               for (int i = 1; i <= 5; i++) {
                  this.addMode(i, Component.translatable("gui.typemoonworld.mode.level", i));
               }
            } else if (this.reinforcementStage == 3) {
               this.addMode(0, Component.translatable("gui.typemoonworld.mode.cancel.self"));
               this.addMode(1, Component.translatable("gui.typemoonworld.mode.cancel.other"));
               this.addMode(2, Component.translatable("gui.typemoonworld.mode.cancel.item"));
            }
         } else if ("gravity_magic".equals(currentMagic)) {
            this.isGravity = true;
            if (this.gravityStage == 0) {
               this.addMode(0, Component.translatable("gui.typemoonworld.mode.self"));
               this.addMode(1, Component.translatable("gui.typemoonworld.mode.other"));
            } else {
               this.addMode(-2, Component.translatable("gui.typemoonworld.mode.gravity.ultra_light"));
               this.addMode(-1, Component.translatable("gui.typemoonworld.mode.gravity.light"));
               this.addMode(0, Component.translatable("gui.typemoonworld.mode.gravity.normal"));
               this.addMode(1, Component.translatable("gui.typemoonworld.mode.gravity.heavy"));
               this.addMode(2, Component.translatable("gui.typemoonworld.mode.gravity.ultra_heavy"));
            }
         }
      }

      if (this.modes.isEmpty()) {
         this.addMode(0, Component.translatable("gui.typemoonworld.mode.none"));
      }

      this.selectedIndex = 0;
      if (this.isGravity && this.gravityStage == 1) {
         for (int i = 0; i < this.modeIds.size(); i++) {
            if (this.modeIds.get(i) == currentMode) {
               this.selectedIndex = i;
               break;
            }
         }
      } else if (!this.isReinforcement && !this.isGravity) {
         for (int ix = 0; ix < this.modeIds.size(); ix++) {
            if (this.modeIds.get(ix) == currentMode) {
               this.selectedIndex = ix;
               break;
            }
         }
      }
   }

   private void addMode(int id, Component component) {
      this.modeIds.add(id);
      this.modes.add(component);
   }

   public void init() {
      super.init();
      Minecraft mc = this.minecraft;
      if (mc != null) {
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      super.tick();
      if (this.minecraft != null) {
         long window = this.minecraft.getWindow().getWindow();
         KeyMapping keyMapping = TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH;
         Type type = keyMapping.getKey().getType();
         int code = keyMapping.getKey().getValue();
         boolean isDown = false;
         if (type == Type.KEYSYM) {
            isDown = GLFW.glfwGetKey(window, code) == 1;
         } else if (type == Type.MOUSE) {
            isDown = GLFW.glfwGetMouseButton(window, code) == 1;
         }

         if (!isDown
            && !this.isClosing
            && (!this.isReinforcement || this.reinforcementStage != 1 && this.reinforcementStage != 2 && this.reinforcementStage != 3)
            && (!this.isGravity || this.gravityStage != 1)) {
            this.closeAndSelect();
         }
      }
   }

   private void closeAndSelect() {
      Player player = Minecraft.getInstance().player;
      if (player == null) {
         this.isClosing = true;
         this.onClose();
      } else if (this.isReinforcement) {
         int selectedId = 0;
         if (this.selectedIndex >= 0 && this.selectedIndex < this.modeIds.size()) {
            selectedId = this.modeIds.get(this.selectedIndex);
         }

         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (this.reinforcementStage == 0) {
            if (selectedId == 3) {
               PacketDistributor.sendToServer(new MagicModeSwitchMessage(2, 3), new CustomPacketPayload[0]);
               this.reinforcementStage = 3;
               this.initModes(0);
               this.selectedIndex = 0;
               return;
            }

            PacketDistributor.sendToServer(new MagicModeSwitchMessage(2, selectedId), new CustomPacketPayload[0]);
            this.selectedTarget = selectedId;
            if (selectedId == 2) {
               this.reinforcementStage = 2;
               this.initModes(0);
               this.selectedIndex = 0;
            } else {
               this.reinforcementStage = 1;
               this.initModes(0);
               this.selectedIndex = 0;
            }
         } else if (this.reinforcementStage == 1) {
            PacketDistributor.sendToServer(new MagicModeSwitchMessage(3, selectedId), new CustomPacketPayload[0]);
            this.selectedBodyPart = selectedId;
            this.reinforcementStage = 2;
            this.initModes(0);
            this.selectedIndex = 0;
         } else if (this.reinforcementStage == 2) {
            int maxLevel = Math.min(5, 1 + (int)(vars.proficiency_reinforcement / 20.0));
            if (selectedId > maxLevel) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.reinforcement.level_too_high", maxLevel), true);
               return;
            }

            PacketDistributor.sendToServer(new MagicModeSwitchMessage(4, selectedId), new CustomPacketPayload[0]);
            this.isClosing = true;
            this.onClose();
         } else if (this.reinforcementStage == 3) {
            PacketDistributor.sendToServer(new MagicModeSwitchMessage(3, selectedId), new CustomPacketPayload[0]);
            this.isClosing = true;
            this.onClose();
         }
      } else if (this.isGravity) {
         int selectedIdx = 0;
         if (this.selectedIndex >= 0 && this.selectedIndex < this.modeIds.size()) {
            selectedIdx = this.modeIds.get(this.selectedIndex);
         }

         if (this.gravityStage == 0) {
            PacketDistributor.sendToServer(new MagicModeSwitchMessage(6, selectedIdx), new CustomPacketPayload[0]);
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            this.gravityStage = 1;
            this.initModes(vars.gravity_magic_mode);
         } else {
            PacketDistributor.sendToServer(new MagicModeSwitchMessage(7, selectedIdx), new CustomPacketPayload[0]);
            this.isClosing = true;
            this.onClose();
         }
      } else {
         this.isClosing = true;
         int targetMode = 0;
         if (this.selectedIndex >= 0 && this.selectedIndex < this.modeIds.size()) {
            targetMode = this.modeIds.get(this.selectedIndex);
         }

         PacketDistributor.sendToServer(new MagicModeSwitchMessage(0, targetMode), new CustomPacketPayload[0]);
         this.onClose();
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (scrollY > 0.0) {
         this.selectedIndex = (this.selectedIndex - 1 + this.modes.size()) % this.modes.size();
      } else if (scrollY < 0.0) {
         this.selectedIndex = (this.selectedIndex + 1) % this.modes.size();
      }

      return true;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.updateSelectionAt(mouseX, mouseY)) {
         this.closeAndSelect();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int itemWidth = 50;
      int itemHeight = 50;
      int gap = 8;
      int totalWidth = itemWidth * this.modes.size() + gap * (this.modes.size() - 1);
      int startX = (this.width - totalWidth) / 2;
      int centerY = this.height / 2;
      int startY = centerY - itemHeight / 2;
      int padding = 10;
      int bgX1 = startX - padding;
      int bgY1 = startY - padding - 20;
      int bgX2 = startX + totalWidth + padding;
      int bgY2 = startY + itemHeight + padding;
      guiGraphics.fill(bgX1, bgY1, bgX2, bgY2, -1275068416);
      guiGraphics.renderOutline(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, -7829368);
      this.updateSelectionAt(mouseX, mouseY);
      boolean showIcons = true;
      boolean isSwordBarrel = false;
      boolean isJewelMagic = false;
      boolean isReinforcement = false;
      boolean isGravity = false;
      boolean isJewelRelease = false;
      String currentMagic = "";
      if (!this.modes.isEmpty()) {
         Player player = Minecraft.getInstance().player;
         if (player != null) {
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
               currentMagic = vars.selected_magics.get(vars.current_magic_index);
            }

            if ("sword_barrel_full_open".equals(currentMagic)) {
               isSwordBarrel = true;
            } else if (currentMagic.startsWith("jewel_magic") || "jewel_machine_gun".equals(currentMagic)) {
               isJewelMagic = true;
               isJewelRelease = "jewel_magic_release".equals(currentMagic);
            } else if ("reinforcement".equals(currentMagic) || currentMagic.startsWith("reinforcement_")) {
               isReinforcement = true;
            } else if ("gravity_magic".equals(currentMagic)) {
               isGravity = true;
            } else {
               showIcons = false;
            }
         }
      }

      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, bgY1 + 5, 16777215);

      for (int i = 0; i < this.modes.size(); i++) {
         int x = startX + i * (itemWidth + gap);
         boolean isSelected = i == this.selectedIndex;
         int fillColor = isSelected ? 1627389951 : 1073741824;
         int textColor = isSelected ? -171 : -5592406;
         if (isReinforcement && this.reinforcementStage == 2) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
               TypeMoonWorldModVariables.PlayerVariables varsx = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               int maxLevel = Math.min(5, 1 + (int)(varsx.proficiency_reinforcement / 20.0));
               int level = i < this.modeIds.size() ? this.modeIds.get(i) : i + 1;
               if (level > maxLevel) {
                  fillColor = 1616205141;
                  textColor = -11184811;
               }
            }
         }

         int modeId = i < this.modeIds.size() ? this.modeIds.get(i) : -1;
         if (isSwordBarrel && modeId == 3 && this.minecraft != null && this.minecraft.player != null) {
            TypeMoonWorldModVariables.PlayerVariables varsx = (TypeMoonWorldModVariables.PlayerVariables)this.minecraft
               .player
               .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isBP = varsx.ubw_broken_phantasm_enabled;
            if (isBP) {
               fillColor = 1627324416;
               textColor = -43691;
            }
         }

         guiGraphics.fill(x, startY, x + itemWidth, startY + itemHeight, fillColor);
         int borderColor = isSelected ? -1 : -11184811;
         guiGraphics.renderOutline(x, startY, itemWidth, itemHeight, borderColor);
         if (showIcons) {
            RenderSystem.enableBlend();
            int iconSize = 24;
            int iconX = x + (itemWidth - iconSize) / 2;
            int iconY = startY + 4;
            if (isSwordBarrel && modeId >= 0 && modeId < MODE_ICONS.length) {
               guiGraphics.blit(MODE_ICONS[modeId], iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
            } else if (isReinforcement) {
               ResourceLocation iconLoc = null;
               if (this.reinforcementStage == 0) {
                  switch (modeId) {
                     case 0:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/resistance.png");
                        break;
                     case 1:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png");
                        break;
                     case 2:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/item/iron_sword.png");
                        break;
                     case 3:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/item/barrier.png");
                  }
               } else if (this.reinforcementStage == 3) {
                  switch (modeId) {
                     case 0:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/resistance.png");
                        break;
                     case 1:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png");
                        break;
                     case 2:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/item/iron_sword.png");
                  }
               } else if (this.reinforcementStage == 1) {
                  switch (modeId) {
                     case 0:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/resistance.png");
                        break;
                     case 1:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/strength.png");
                        break;
                     case 2:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/speed.png");
                        break;
                     case 3:
                        iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/night_vision.png");
                  }
               } else if (this.reinforcementStage == 2) {
                  if (this.selectedTarget == 2) {
                     iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/haste.png");
                  } else {
                     switch (this.selectedBodyPart) {
                        case 0:
                           iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/resistance.png");
                           break;
                        case 1:
                           iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/strength.png");
                           break;
                        case 2:
                           iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/speed.png");
                           break;
                        case 3:
                           iconLoc = ResourceLocation.withDefaultNamespace("textures/mob_effect/night_vision.png");
                     }
                  }
               }

               if (iconLoc != null) {
                  guiGraphics.setColor(0.0F, 1.0F, 1.0F, 0.5F);
                  guiGraphics.blit(iconLoc, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
                  guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
               }
            } else if (isGravity) {
               ResourceLocation iconLocx = null;
               if (this.gravityStage == 0) {
                  iconLocx = modeId == 0
                     ? ResourceLocation.withDefaultNamespace("textures/mob_effect/resistance.png")
                     : ResourceLocation.withDefaultNamespace("textures/item/totem_of_undying.png");
               } else {
                  iconLocx = switch (modeId) {
                     case -2 -> ResourceLocation.withDefaultNamespace("textures/mob_effect/jump_boost.png");
                     case -1 -> ResourceLocation.withDefaultNamespace("textures/mob_effect/slow_falling.png");
                     case 0 -> ResourceLocation.withDefaultNamespace("textures/mob_effect/glowing.png");
                     case 1 -> ResourceLocation.withDefaultNamespace("textures/mob_effect/slowness.png");
                     case 2 -> ResourceLocation.withDefaultNamespace("textures/mob_effect/mining_fatigue.png");
                     default -> null;
                  };
               }

               if (iconLocx != null) {
                  guiGraphics.setColor(0.54F, 0.49F, 1.0F, 0.75F);
                  guiGraphics.blit(iconLocx, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
                  guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
               }
            } else if (isJewelMagic && modeId >= 0) {
               if ("jewel_machine_gun".equals(currentMagic)) {
                  int smallSize = 12;
                  int cx = iconX + (iconSize - smallSize) / 2;
                  int cy = iconY + (iconSize - smallSize) / 2;
                  guiGraphics.blit(JEWEL_ICONS[5], cx, cy, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
                  guiGraphics.blit(JEWEL_ICONS[0], cx - 6, cy - 6, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
                  guiGraphics.blit(JEWEL_ICONS[1], cx + 6, cy - 6, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
                  guiGraphics.blit(JEWEL_ICONS[2], cx - 6, cy + 6, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
                  guiGraphics.blit(JEWEL_ICONS[3], cx + 6, cy + 6, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
               } else if (isJewelRelease) {
                  int releaseIconIndex = switch (modeId) {
                     case 0 -> 1;
                     case 1 -> 2;
                     case 2 -> 3;
                     case 3 -> 4;
                     default -> -1;
                  };
                  if (releaseIconIndex >= 0 && releaseIconIndex < JEWEL_ICONS.length) {
                     guiGraphics.blit(JEWEL_ICONS[releaseIconIndex], iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
                  }
               } else if (modeId != 5 && modeId < JEWEL_ICONS.length) {
                  guiGraphics.blit(JEWEL_ICONS[modeId], iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
               } else {
                  int smallSize = 12;
                  int cx = iconX + (iconSize - smallSize) / 2;
                  int cy = iconY + (iconSize - smallSize) / 2;
                  guiGraphics.blit(JEWEL_ICONS[5], cx, cy, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
                  guiGraphics.blit(JEWEL_ICONS[0], cx - 6, cy - 6, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
                  guiGraphics.blit(JEWEL_ICONS[1], cx + 6, cy - 6, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
                  guiGraphics.blit(JEWEL_ICONS[2], cx - 6, cy + 6, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
                  guiGraphics.blit(JEWEL_ICONS[3], cx + 6, cy + 6, 0.0F, 0.0F, smallSize, smallSize, smallSize, smallSize);
               }
            }

            RenderSystem.disableBlend();
         }

         Component text = this.modes.get(i);
         String textStr = text.getString();
         if (textStr.contains("\n")) {
            String[] lines = textStr.split("\n");
            guiGraphics.drawCenteredString(this.font, lines[0], x + itemWidth / 2, startY + itemHeight - 18, textColor);
            guiGraphics.drawCenteredString(this.font, lines[1], x + itemWidth / 2, startY + itemHeight - 9, textColor);
         } else {
            int textY = startY + itemHeight - 10;
            if (!showIcons) {
               textY = startY + (itemHeight - 8) / 2;
            }

            guiGraphics.drawCenteredString(this.font, text, x + itemWidth / 2, textY, textColor);
         }
      }
   }

   private boolean updateSelectionAt(double mouseX, double mouseY) {
      int itemWidth = 50;
      int itemHeight = 50;
      int gap = 8;
      int totalWidth = itemWidth * this.modes.size() + gap * (this.modes.size() - 1);
      int startX = (this.width - totalWidth) / 2;
      int centerY = this.height / 2;
      int startY = centerY - itemHeight / 2;

      for (int i = 0; i < this.modes.size(); i++) {
         int x = startX + i * (itemWidth + gap);
         if (mouseX >= x && mouseX < x + itemWidth && mouseY >= startY && mouseY < startY + itemHeight) {
            this.selectedIndex = i;
            return true;
         }
      }

      return false;
   }
}
