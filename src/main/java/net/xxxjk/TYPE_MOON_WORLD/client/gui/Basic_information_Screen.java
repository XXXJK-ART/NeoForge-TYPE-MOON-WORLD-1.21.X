package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_Button_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Basic_information_back_player_self;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.BasicInformationMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Basic_information_Screen extends AbstractContainerScreen<BasicInformationMenu> {
   private static final HashMap<String, Object> guistate = BasicInformationMenu.guistate;
   private static final double STAT_EPSILON = 1.0E-4;
   private static final int STAT_ROW_HOVER_WIDTH = 280;
   private final Level world;
   private final int x;
   private final int y;
   private final int z;
   private final Player entity;
   private final List<Basic_information_Screen.StatTooltipArea> statTooltipAreas = new ArrayList<>();
   NeonButton imagebutton_basic_attributes;
   NeonButton imagebutton_magical_attributes;
   NeonButton imagebutton_magical_properties;

   public Basic_information_Screen(BasicInformationMenu container, Inventory inventory, Component text) {
      super(container, inventory, text);
      this.world = container.world;
      this.x = container.x;
      this.y = container.y;
      this.z = container.z;
      this.entity = container.entity;
      this.imageWidth = 420;
      this.imageHeight = 230;
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      super.render(guiGraphics, mouseX, mouseY, partialTicks);
      if (Basic_information_back_player_self.execute(this.entity) instanceof LivingEntity livingEntity) {
         this.renderEntityInInventoryFollowsAngle(
            guiGraphics,
            this.leftPos + 56,
            this.topPos + 214,
            (float)Math.atan((this.leftPos + 56 - mouseX) / 40.0),
            (float)Math.atan((this.topPos + 112 - mouseY) / 40.0),
            livingEntity
         );
      }

      this.renderTooltip(guiGraphics, mouseX, mouseY);
      this.renderStatModifierTooltip(guiGraphics, mouseX, mouseY);
   }

   protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int x = this.leftPos;
      int y = this.topPos;
      GuiUtils.renderArcaneBackground(guiGraphics, x, y, this.imageWidth, this.imageHeight);
      GuiUtils.renderArcanePanel(guiGraphics, x + 10, y + 36, 92, 184, GuiUtils.ARCANE_CYAN);
      GuiUtils.renderArcanePanel(guiGraphics, x + 112, y + 36, 298, 84, GuiUtils.ARCANE_CYAN);
      GuiUtils.renderArcanePanel(guiGraphics, x + 112, y + 128, 298, 92, GuiUtils.ARCANE_GOLD);
      RenderSystem.disableBlend();
   }

   protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
      int startX = 120;
      this.statTooltipAreas.clear();
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)this.entity
         .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.section.profile"), 18, 43, GuiUtils.ARCANE_TEXT_MUTED, false);
      GuiUtils.renderSectionHeader(guiGraphics, 18, 43, 76, GuiUtils.ARCANE_CYAN);
      guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.section.mana_overview"), startX, 43, GuiUtils.ARCANE_CYAN, false);
      GuiUtils.renderSectionHeader(guiGraphics, startX, 43, 282, GuiUtils.ARCANE_CYAN);
      double currentMana = vars.player_mana;
      double maxMana = vars.player_max_mana;
      double baseCurrentMana = Math.min(currentMana, maxMana);
      List<Basic_information_Screen.ModifierReason> currentManaReasons = new ArrayList<>();
      if (currentMana > maxMana + 1.0E-4) {
         currentManaReasons.add(
            new Basic_information_Screen.ModifierReason(Component.translatable("gui.typemoonworld.basic_info.mod.reason.mana_overload"), currentMana - maxMana)
         );
      }

      this.drawStatWithModifiers(
         guiGraphics,
         Component.translatable("gui.typemoonworld.basic_info.current_mana"),
         baseCurrentMana,
         currentManaReasons,
         startX,
         57,
         STAT_ROW_HOVER_WIDTH,
         0,
         "gui.typemoonworld.basic_info.unit.mana",
         true
      );
      this.drawStatWithModifiers(
         guiGraphics,
         Component.translatable("gui.typemoonworld.basic_info.max_mana"),
         maxMana,
         List.of(),
         startX,
         70,
         STAT_ROW_HOVER_WIDTH,
         0,
         "gui.typemoonworld.basic_info.unit.mana",
         true
      );
      double baseRegen = vars.player_mana_egenerated_every_moment;
      double regenMultiplier = vars.current_mana_regen_multiplier <= 0.0 ? 1.0 : vars.current_mana_regen_multiplier;
      List<Basic_information_Screen.ModifierReason> regenReasons = new ArrayList<>();
      double leylineDelta = baseRegen * regenMultiplier - baseRegen;
      if (Math.abs(leylineDelta) > 1.0E-4) {
         regenReasons.add(
            new Basic_information_Screen.ModifierReason(
               Component.translatable("gui.typemoonworld.basic_info.mod.reason.leyline", String.format(Locale.ROOT, "%.2fx", regenMultiplier)),
               leylineDelta
            )
         );
      }

      double baseIntervalTicks = vars.player_restore_magic_moment;
      List<Basic_information_Screen.ModifierReason> intervalReasons = new ArrayList<>();
      if (vars.is_magic_circuit_open) {
         double halvedTicks = baseIntervalTicks / 2.0;
         double halvedDeltaSec = (halvedTicks - baseIntervalTicks) / 20.0;
         if (Math.abs(halvedDeltaSec) > 1.0E-4) {
            intervalReasons.add(
               new Basic_information_Screen.ModifierReason(Component.translatable("gui.typemoonworld.basic_info.mod.reason.magic_circuit"), halvedDeltaSec)
            );
         }

         if (halvedTicks < 1.0) {
            double effectiveIntervalTicks = 1.0;
            double floorDeltaSec = (effectiveIntervalTicks - halvedTicks) / 20.0;
            if (Math.abs(floorDeltaSec) > 1.0E-4) {
               intervalReasons.add(
                  new Basic_information_Screen.ModifierReason(Component.translatable("gui.typemoonworld.basic_info.mod.reason.interval_floor"), floorDeltaSec)
               );
            }
         }
      }

      float manaRatio = maxMana <= 0.0 ? 0.0F : (float)(currentMana / maxMana);
      GuiUtils.renderProgressBar(guiGraphics, startX, 84, 282, 8, manaRatio, GuiUtils.ARCANE_CYAN);
      this.drawStatWithModifiers(
         guiGraphics,
         Component.translatable("gui.typemoonworld.basic_info.mana_regen"),
         baseRegen,
         regenReasons,
         startX,
         99,
         STAT_ROW_HOVER_WIDTH,
         1,
         "gui.typemoonworld.basic_info.unit.mana_per_cycle",
         true
      );
      this.drawStatWithModifiers(
         guiGraphics,
         Component.translatable("gui.typemoonworld.basic_info.regen_interval"),
         baseIntervalTicks / 20.0,
         intervalReasons,
         startX,
         108,
         STAT_ROW_HOVER_WIDTH,
         2,
         "gui.typemoonworld.basic_info.unit.second",
         false
      );
      guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.section.affinities"), startX, 135, GuiUtils.ARCANE_GOLD, false);
      GuiUtils.renderSectionHeader(guiGraphics, startX, 135, 282, GuiUtils.ARCANE_GOLD);
      Component baseLabel = Component.translatable("gui.typemoonworld.basic_info.base_attributes");
      guiGraphics.drawString(this.font, baseLabel, startX, 150, GuiUtils.ARCANE_TEXT_MUTED, false);
      Component baseAttr = this.buildBaseAttributes(vars);
      guiGraphics.fill(startX, 164, startX + 4, 168, GuiUtils.ARCANE_CYAN);
      guiGraphics.drawWordWrap(this.font, baseAttr, startX + 9, 161, 273, GuiUtils.ARCANE_TEXT);
      Component extraLabel = Component.translatable("gui.typemoonworld.basic_info.extra_attributes");
      guiGraphics.drawString(this.font, extraLabel, startX, 183, GuiUtils.ARCANE_TEXT_MUTED, false);
      Component extraAttr = this.buildExtraAttributes(vars);
      guiGraphics.fill(startX, 197, startX + 4, 201, GuiUtils.ARCANE_GOLD);
      guiGraphics.drawWordWrap(this.font, extraAttr, startX + 9, 194, 273, GuiUtils.ARCANE_TEXT);
   }

   private void drawStatWithModifiers(
      GuiGraphics gui,
      Component label,
      double baseValue,
      List<Basic_information_Screen.ModifierReason> reasons,
      int x,
      int y,
      int hoverWidth,
      int decimals,
      String unitKey,
      boolean positiveIsBeneficial
   ) {
      gui.drawString(this.font, label, x, y, -5592406, false);
      double totalModifier = reasons.stream().mapToDouble(Basic_information_Screen.ModifierReason::delta).sum();
      String unitText = Component.translatable(unitKey).getString();
      String baseText = this.formatNumber(baseValue, decimals);
      String mainText = baseText + " " + unitText;
      int valueX = x + Math.max(62, this.font.width(label) + 6);
      gui.drawString(this.font, mainText, valueX, y, -1, false);
      if (!(Math.abs(totalModifier) <= 1.0E-4)) {
         String modifierText = "(" + this.formatSignedNumber(totalModifier, decimals) + ")";
         boolean isBuff = positiveIsBeneficial ? totalModifier >= 0.0 : totalModifier <= 0.0;
         int modifierColor = isBuff ? -11141291 : -34953;
         int modifierX = valueX + this.font.width(mainText) + 4;
         gui.drawString(this.font, modifierText, modifierX, y, modifierColor, false);
         this.statTooltipAreas
            .add(
               new Basic_information_Screen.StatTooltipArea(
                  this.leftPos + x,
                  this.topPos + y - 1,
                  hoverWidth,
                  9 + 3,
                  this.buildModifierTooltip(label, baseValue, totalModifier, decimals, unitKey, reasons, positiveIsBeneficial)
               )
            );
      }
   }

   private List<Component> buildModifierTooltip(
      Component label,
      double baseValue,
      double totalModifier,
      int decimals,
      String unitKey,
      List<Basic_information_Screen.ModifierReason> reasons,
      boolean positiveIsBeneficial
   ) {
      List<Component> lines = new ArrayList<>();
      Component unit = Component.translatable(unitKey);
      String unitText = unit.getString();
      lines.add(label.copy().withStyle(ChatFormatting.AQUA));
      lines.add(
         Component.translatable("gui.typemoonworld.basic_info.tooltip.base", this.formatNumber(baseValue, decimals), unit)
            .withStyle(ChatFormatting.GRAY)
      );
      lines.add(
         Component.translatable("gui.typemoonworld.basic_info.tooltip.total_modifier", this.formatSignedNumber(totalModifier, decimals), unit)
            .withStyle((positiveIsBeneficial ? !(totalModifier >= 0.0) : !(totalModifier <= 0.0)) ? ChatFormatting.RED : ChatFormatting.GREEN)
      );
      lines.add(
         Component.translatable("gui.typemoonworld.basic_info.tooltip.effective", this.formatNumber(baseValue + totalModifier, decimals), unit)
            .withStyle(ChatFormatting.WHITE)
      );
      if (!reasons.isEmpty()) {
         lines.add(Component.empty());
         lines.add(Component.translatable("gui.typemoonworld.basic_info.tooltip.details").withStyle(ChatFormatting.GOLD));

         for (Basic_information_Screen.ModifierReason reason : reasons) {
            String detail = String.format(Locale.ROOT, "- %s (%s %s)", reason.label().getString(), this.formatSignedNumber(reason.delta(), decimals), unitText);
            lines.add(Component.literal(detail).withStyle(ChatFormatting.DARK_GRAY));
         }
      }

      return lines;
   }

   private void renderStatModifierTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      for (Basic_information_Screen.StatTooltipArea area : this.statTooltipAreas) {
         if (mouseX >= area.x() && mouseX < area.x() + area.width() && mouseY >= area.y() && mouseY < area.y() + area.height()) {
            this.renderSimpleTooltip(guiGraphics, area.lines(), mouseX, mouseY);
            return;
         }
      }
   }

   private void renderSimpleTooltip(GuiGraphics guiGraphics, List<Component> lines, int mouseX, int mouseY) {
      if (!lines.isEmpty()) {
         int padding = 5;
         int lineHeight = 10;
         int maxContentWidth = Math.min(260, this.width - padding * 2 - 12);
         List<FormattedCharSequence> wrappedLines = new ArrayList<>();
         for (Component line : lines) {
            if (line.getString().isEmpty()) {
               wrappedLines.add(FormattedCharSequence.EMPTY);
            } else {
               wrappedLines.addAll(this.font.split(line, maxContentWidth));
            }
         }

         int tooltipWidth = 0;
         for (FormattedCharSequence line : wrappedLines) {
            tooltipWidth = Math.max(tooltipWidth, this.font.width(line));
         }

         tooltipWidth += padding * 2;
         int tooltipHeight = wrappedLines.size() * lineHeight + padding * 2;
         int tooltipX = mouseX + 12;
         int tooltipY = mouseY - 12;
         if (tooltipX + tooltipWidth > this.width - 4) {
            tooltipX = this.width - tooltipWidth - 4;
         }

         if (tooltipX < 4) {
            tooltipX = 4;
         }

         if (tooltipY + tooltipHeight > this.height - 4) {
            tooltipY = this.height - tooltipHeight - 4;
         }

         if (tooltipY < 4) {
            tooltipY = 4;
         }

         guiGraphics.pose().pushPose();
         guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
         guiGraphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, GuiUtils.ARCANE_PANEL);
         guiGraphics.renderOutline(tooltipX, tooltipY, tooltipWidth, tooltipHeight, GuiUtils.ARCANE_CYAN);
         int textY = tooltipY + padding;

         for (FormattedCharSequence line : wrappedLines) {
            guiGraphics.drawString(this.font, line, tooltipX + padding, textY, -1, false);
            textY += lineHeight;
         }

         guiGraphics.pose().popPose();
      }
   }

   private String formatNumber(double value, int decimals) {
      return decimals <= 0 ? String.format(Locale.ROOT, "%d", Math.round(value)) : String.format(Locale.ROOT, "%1$." + decimals + "f", value);
   }

   private String formatSignedNumber(double value, int decimals) {
      return decimals <= 0 ? String.format(Locale.ROOT, "%+d", Math.round(value)) : String.format(Locale.ROOT, "%1$+." + decimals + "f", value);
   }

   public void init() {
      super.init();
      int btnY = this.topPos + 6;
      int btnWidth = 78;
      int btnHeight = 16;
      int startX = this.leftPos + this.imageWidth - btnWidth * 3 - 14;
      this.imagebutton_basic_attributes = new NeonButton(
         startX, btnY, btnWidth, btnHeight, Component.translatable("gui.typemoonworld.tab.basic_attributes"), e -> {}
      ).setArcaneStyle(true).setSelected(true);
      this.addRenderableWidget(this.imagebutton_basic_attributes);
      this.imagebutton_magical_attributes = new NeonButton(
         startX + btnWidth + 2, btnY, btnWidth, btnHeight, Component.translatable("gui.typemoonworld.tab.body_modification"), e -> {
            PacketDistributor.sendToServer(new Basic_information_Button_Message(0, this.x, this.y, this.z), new CustomPacketPayload[0]);
            Basic_information_Button_Message.handleButtonAction(this.entity, 0, this.x, this.y, this.z);
         }
      ).setArcaneStyle(true);
      this.addRenderableWidget(this.imagebutton_magical_attributes);
      this.imagebutton_magical_properties = new NeonButton(
         startX + (btnWidth + 2) * 2, btnY, btnWidth, btnHeight, Component.translatable("gui.typemoonworld.tab.magic_knowledge"), e -> {
            PacketDistributor.sendToServer(new Basic_information_Button_Message(1, this.x, this.y, this.z), new CustomPacketPayload[0]);
            Basic_information_Button_Message.handleButtonAction(this.entity, 1, this.x, this.y, this.z);
         }
      ).setArcaneStyle(true);
      this.addRenderableWidget(this.imagebutton_magical_properties);
   }

   private Component buildBaseAttributes(TypeMoonWorldModVariables.PlayerVariables vars) {
      MutableComponent builder = Component.empty();
      boolean has = false;
      if (vars.player_magic_attributes_earth) {
         builder.append(Component.translatable("attribute.typemoonworld.earth"));
         has = true;
      }

      if (vars.player_magic_attributes_water) {
         if (has) {
            builder.append(Component.literal(" / "));
         }

         builder.append(Component.translatable("attribute.typemoonworld.water"));
         has = true;
      }

      if (vars.player_magic_attributes_fire) {
         if (has) {
            builder.append(Component.literal(" / "));
         }

         builder.append(Component.translatable("attribute.typemoonworld.fire"));
         has = true;
      }

      if (vars.player_magic_attributes_wind) {
         if (has) {
            builder.append(Component.literal(" / "));
         }

         builder.append(Component.translatable("attribute.typemoonworld.wind"));
         has = true;
      }

      if (vars.player_magic_attributes_ether) {
         if (has) {
            builder.append(Component.literal(" / "));
         }

         builder.append(Component.translatable("attribute.typemoonworld.ether"));
         has = true;
      }

      if (!has) {
         builder.append(Component.literal("/"));
      }

      return builder;
   }

   private Component buildExtraAttributes(TypeMoonWorldModVariables.PlayerVariables vars) {
      MutableComponent builder = Component.empty();
      boolean has = false;
      if (vars.player_magic_attributes_none) {
         builder.append(Component.translatable("attribute.typemoonworld.none"));
         has = true;
      }

      if (vars.player_magic_attributes_imaginary_number) {
         if (has) {
            builder.append(Component.literal(" / "));
         }

         builder.append(Component.translatable("attribute.typemoonworld.imaginary"));
         has = true;
      }

      if (vars.player_magic_attributes_sword) {
         if (has) {
            builder.append(Component.literal(" / "));
         }

         builder.append(Component.translatable("attribute.typemoonworld.sword"));
         has = true;
      }

      if (!has) {
         builder.append(Component.literal("/"));
      }

      return builder;
   }

   private void renderEntityInInventoryFollowsAngle(GuiGraphics guiGraphics, int x, int y, float angleXComponent, float angleYComponent, LivingEntity entity) {
      Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
      Quaternionf cameraOrientation = new Quaternionf().rotateX(angleYComponent * 20.0F * (float) (Math.PI / 180.0));
      pose.mul(cameraOrientation);
      float f2 = entity.yBodyRot;
      float f3 = entity.getYRot();
      float f4 = entity.getXRot();
      float f5 = entity.yHeadRotO;
      float f6 = entity.yHeadRot;
      entity.yBodyRot = 180.0F + angleXComponent * 20.0F;
      entity.setYRot(180.0F + angleXComponent * 40.0F);
      entity.setXRot(-angleYComponent * 20.0F);
      entity.yHeadRot = entity.getYRot();
      entity.yHeadRotO = entity.getYRot();
      InventoryScreen.renderEntityInInventory(guiGraphics, x, y, 55.0F, new Vector3f(0.0F, 0.0F, 0.0F), pose, cameraOrientation, entity);
      entity.yBodyRot = f2;
      entity.setYRot(f3);
      entity.setXRot(f4);
      entity.yHeadRotO = f5;
      entity.yHeadRot = f6;
   }

   public Level getWorld() {
      return this.world;
   }

   private record ModifierReason(Component label, double delta) {
   }

   private record StatTooltipArea(int x, int y, int width, int height, List<Component> lines) {
   }
}
