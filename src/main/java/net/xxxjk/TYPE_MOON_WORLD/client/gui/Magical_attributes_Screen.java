package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicClassification;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicWheelSlotEditMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Magical_attributes_Button_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.PageChangeMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicIndexMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicWheelMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Basic_information_back_player_self;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicalattributesMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Magical_attributes_Screen extends AbstractContainerScreen<MagicalattributesMenu> {
   private static final HashMap<String, Object> guistate = MagicalattributesMenu.guistate;
   private static final int MAGIC_SOURCE_SELF = 0;
   private static final int MAGIC_SOURCE_CREST = 1;
   private static final int LIST_X_OFFSET = 120;
   private static final int LIST_Y_OFFSET = 52;
   private static final int LIST_WIDTH = 132;
   private static final int LIST_HEIGHT = 124;
   private static final int CARD_WIDTH = 62;
   private static final int CARD_HEIGHT = 22;
   private static final int CARD_COLUMNS = 2;
   private static final int CARD_ROWS_VISIBLE = 4;
   private static final int CARD_GAP_X = 8;
   private static final int CARD_GAP_Y = 8;
   private static final int WHEEL_X_OFFSET = 272;
   private static final int WHEEL_Y_OFFSET = 60;
   private static final int WHEEL_SLOT_SIZE = 22;
   private static final int WHEEL_COLUMNS = 3;
   private static final int FILTER_LABEL_X = 120;
   private static final int FILTER_LABEL_Y = 34;
   private static final int FILTER_BUTTON_Y = 31;
   private static final int FILTER_BUTTON_MIN_WIDTH = 40;
   private static final int FILTER_BUTTON_MAX_WIDTH = 72;
   private static final int TOOLTIP_DELAY_MS = 1000;
   private static final int MAX_TOOLTIP_HEIGHT = 126;
   private static final int PRESET_DIALOG_WIDTH_DEFAULT = 232;
   private static final int PRESET_DIALOG_WIDTH_PROJECTION = 320;
   private static final int PRESET_DIALOG_MAX_HEIGHT_DEFAULT = 260;
   private static final int PRESET_DIALOG_MAX_HEIGHT_PROJECTION = 300;
   private static final int PRESET_DIALOG_OPTION_HEIGHT = 18;
   private static final Set<String> CREST_ALLOWED_JEWEL_MAGIC_IDS = Set.of(
      "jewel_random_shoot", "jewel_machine_gun", "jewel_magic_shoot", "jewel_magic_release"
   );
   private final Level world;
   private final int x;
   private final int y;
   private final int z;
   private final Player entity;
   private int pageMode;
   Button imagebutton_basic_attributes;
   Button imagebutton_magical_attributes;
   Button imagebutton_magical_properties;
   Button tabSelfKnowledge;
   Button tabCrestKnowledge;
   Button filterButton;
   final List<Button> wheelSwitchButtons = new ArrayList<>();
   final List<Magical_attributes_Screen.MagicEntry> baseMagicCatalog = new ArrayList<>();
   final Map<String, Magical_attributes_Screen.MagicEntry> magicCatalogById = new HashMap<>();
   List<Magical_attributes_Screen.MagicEntry> sourceMagics = new ArrayList<>();
   List<Magical_attributes_Screen.MagicEntry> filteredMagics = new ArrayList<>();
   int magicSourceTab = 0;
   String filterCategory = "all";
   float scrollOffs;
   boolean scrolling;
   int startIndex;
   float descScrollOffs;
   long hoverStartTime;
   Magical_attributes_Screen.MagicEntry lastHoveredEntry;
   boolean tooltipActive;
   Magical_attributes_Screen.MagicEntry draggingEntry;
   boolean draggingFromWheel;
   int draggingFromWheelSlot = -1;
   Magical_attributes_Screen.PresetDialogState presetDialogState;
   float presetDialogScrollOffs;

   public Magical_attributes_Screen(MagicalattributesMenu container, Inventory inventory, Component text) {
      super(container, inventory, text);
      this.world = container.world;
      this.x = container.x;
      this.y = container.y;
      this.z = container.z;
      this.entity = container.entity;
      this.pageMode = container.pageMode;
      this.imageWidth = 360;
      this.imageHeight = 200;
      this.initMagicCatalog();
      this.rebuildSourceMagics();
      this.updateFilteredMagics();
   }

   private TypeMoonWorldModVariables.PlayerVariables getVars() {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)this.entity
         .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.ensureMagicSystemInitialized();
      return vars;
   }

   private boolean hasUsableMagicCrest() {
      return this.getVars().hasValidImplantedCrest();
   }

   private void ensureCrestSourceAvailability() {
      if (this.magicSourceTab == 1 && !this.hasUsableMagicCrest()) {
         this.magicSourceTab = 0;
      }
   }

   private void initMagicCatalog() {
      this.addMagic("jewel_magic_shoot", "key.typemoonworld.magic.jewel_shoot.short", "jewel", -10837304);
      this.addMagic("jewel_magic_release", "key.typemoonworld.magic.jewel_release.short", "jewel", -4695462);
      this.addMagic("jewel_random_shoot", "key.typemoonworld.magic.jewel_random_shoot.short", "jewel", -5197648);
      this.addMagic("jewel_machine_gun", "key.typemoonworld.magic.jewel_machine_gun.short", "jewel,nordic", -8727320);
      this.addMagic("projection", "key.typemoonworld.magic.projection.short", "basic,unlimited_blade_works", -11549464);
      this.addMagic("structural_analysis", "key.typemoonworld.magic.structural_analysis.short", "basic,unlimited_blade_works", -11549464);
      this.addMagic("broken_phantasm", "key.typemoonworld.magic.broken_phantasm.short", "basic,unlimited_blade_works", -2193579);
      this.addMagic("unlimited_blade_works", "key.typemoonworld.magic.unlimited_blade_works.short", "unlimited_blade_works", -3125939);
      this.addMagic("sword_barrel_full_open", "key.typemoonworld.magic.sword_barrel_full_open.short", "unlimited_blade_works", -3125939);
      this.addMagic("reinforcement", "key.typemoonworld.magic.reinforcement.short", "basic,reinforcement", -12602534);
      this.addMagic("gravity_magic", "key.typemoonworld.magic.gravity_magic.short", "other", -7701249);
      this.addMagic("gander", "key.typemoonworld.magic.gander.short", "nordic", -5230544);
      this.addMagic("gandr_machine_gun", "key.typemoonworld.magic.gandr_machine_gun.short", "nordic", -3121056);
   }

   private void addMagic(String id, String nameKey, String category, int color) {
      Magical_attributes_Screen.MagicEntry entry = new Magical_attributes_Screen.MagicEntry(id, nameKey, category, color);
      this.baseMagicCatalog.add(entry);
      this.magicCatalogById.put(id, entry);
   }

   private static boolean hasLearnedMagic(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      return !"reinforcement".equals(magicId)
         ? vars.learned_magics.contains(magicId)
         : vars.learned_magics.contains("reinforcement")
            || vars.learned_magics.contains("reinforcement_self")
            || vars.learned_magics.contains("reinforcement_other")
            || vars.learned_magics.contains("reinforcement_item");
   }

   private void rebuildSourceMagics() {
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      this.ensureCrestSourceAvailability();
      List<Magical_attributes_Screen.MagicEntry> rebuilt = new ArrayList<>();
      if (this.magicSourceTab == 0) {
         for (Magical_attributes_Screen.MagicEntry base : this.baseMagicCatalog) {
            if (hasLearnedMagic(vars, base.id)) {
               rebuilt.add(base.copy());
            }
         }
      } else {
         boolean hasValidCrest = vars.hasValidImplantedCrest();

         for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry : vars.crest_entries) {
            if (crestEntry != null && crestEntry.magicId != null && !crestEntry.magicId.isEmpty()) {
               String displayMagicId = normalizeMagicIdForDisplay(crestEntry.magicId);
               Magical_attributes_Screen.MagicEntry basex = this.magicCatalogById.get(displayMagicId);
               if (!shouldHideCrestMagic(displayMagicId, basex)) {
                  Magical_attributes_Screen.MagicEntry entry = basex == null
                     ? new Magical_attributes_Screen.MagicEntry(
                        displayMagicId, "key.typemoonworld.magic." + displayMagicId + ".short", this.resolveFallbackCategory(displayMagicId), -1811878
                     )
                     : basex.copy();
                  entry.sourceType = "crest";
                  entry.crestEntryId = crestEntry.entryId;
                  entry.crestSourceKind = crestEntry.sourceKind == null ? "self" : crestEntry.sourceKind;
                  if ("plunder".equals(entry.crestSourceKind)) {
                     entry.presetPayload = crestEntry.presetPayload == null ? new CompoundTag() : crestEntry.presetPayload.copy();
                  } else {
                     entry.presetPayload = new CompoundTag();
                  }

                  entry.originName = crestEntry.originOwnerName == null ? "" : crestEntry.originOwnerName;
                  entry.active = hasValidCrest && crestEntry.active;
                  rebuilt.add(entry);
               }
            }
         }

         rebuilt.sort(Comparator.comparing(this::getMagicShortName));
      }

      this.sourceMagics = rebuilt;
   }

   private static boolean shouldHideCrestMagic(String magicId, Magical_attributes_Screen.MagicEntry base) {
      if (magicId != null && !magicId.isEmpty()) {
         boolean jewelFamily = magicId.startsWith("jewel_")
            || magicId.startsWith("ruby")
            || magicId.startsWith("sapphire")
            || magicId.startsWith("emerald")
            || magicId.startsWith("topaz")
            || magicId.startsWith("cyan")
            || base != null && base.category != null && base.category.contains("jewel");
         return jewelFamily && !CREST_ALLOWED_JEWEL_MAGIC_IDS.contains(magicId);
      } else {
         return false;
      }
   }

   private void updateFilteredMagics() {
      List<Magical_attributes_Screen.MagicEntry> next = new ArrayList<>();

      for (Magical_attributes_Screen.MagicEntry entry : this.sourceMagics) {
         if ("all".equals(this.filterCategory) || entry.category.contains(this.filterCategory)) {
            next.add(entry);
         }
      }

      if (next.isEmpty() && !"all".equals(this.filterCategory)) {
         this.filterCategory = "all";

         for (Magical_attributes_Screen.MagicEntry entryx : this.sourceMagics) {
            next.add(entryx);
         }

         if (this.filterButton != null) {
            this.filterButton.setMessage(this.getFilterButtonText(this.filterCategory));
         }
      }

      this.filteredMagics = next;
      int totalRows = (this.filteredMagics.size() + 2 - 1) / 2;
      int maxStartRow = Math.max(0, totalRows - 4);
      int currentStartRow = this.startIndex / 2;
      if (currentStartRow > maxStartRow) {
         currentStartRow = maxStartRow;
      }

      this.startIndex = currentStartRow * 2;
      this.scrollOffs = maxStartRow == 0 ? 0.0F : (float)currentStartRow / maxStartRow;
   }

   private String getCategoryLabelKey(String category) {
      if ("jewel".equals(category)) {
         return "gui.typemoonworld.category.jewel";
      } else if ("basic".equals(category)) {
         return "gui.typemoonworld.category.basic";
      } else if ("unlimited_blade_works".equals(category)) {
         return "gui.typemoonworld.category.ubw";
      } else if ("other".equals(category)) {
         return "gui.typemoonworld.category.other";
      } else {
         return "nordic".equals(category) ? "gui.typemoonworld.category.nordic" : "gui.typemoonworld.category.all";
      }
   }

   private int getFilterButtonWidth() {
      String[] categoryKeys = new String[]{
         "gui.typemoonworld.category.all",
         "gui.typemoonworld.category.jewel",
         "gui.typemoonworld.category.basic",
         "gui.typemoonworld.category.ubw",
         "gui.typemoonworld.category.other",
         "gui.typemoonworld.category.nordic"
      };
      int maxCategoryWidth = 0;

      for (String key : categoryKeys) {
         maxCategoryWidth = Math.max(maxCategoryWidth, this.font.width(Component.translatable(key)));
      }

      return Mth.clamp(maxCategoryWidth + 8, 40, 72);
   }

   private int getFilterButtonX() {
      int labelWidth = this.font.width(Component.translatable("gui.typemoonworld.category.label"));
      int desiredX = this.leftPos + 120 + labelWidth + 6;
      int buttonWidth = this.getFilterButtonWidth();
      int maxX = this.leftPos + this.imageWidth - buttonWidth - 6;
      return Math.min(desiredX, maxX);
   }

   private String clampTextToWidth(String text, int maxWidth) {
      if (this.font.width(text) <= maxWidth) {
         return text;
      } else {
         String ellipsis = "...";
         int ellipsisWidth = this.font.width(ellipsis);
         return ellipsisWidth >= maxWidth
            ? this.font.plainSubstrByWidth(text, maxWidth)
            : this.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ellipsis;
      }
   }

   private Component getFilterButtonText(String category) {
      String label = Component.translatable(this.getCategoryLabelKey(category)).getString();
      int availableWidth = this.getFilterButtonWidth() - 8;
      return Component.literal(this.clampTextToWidth(label, availableWidth));
   }

   private boolean isCategoryUnlocked(String category) {
      if ("all".equals(category)) {
         return true;
      } else if ("unlimited_blade_works".equals(category)) {
         return hasLearnedMagic(this.getVars(), "unlimited_blade_works");
      } else {
         for (Magical_attributes_Screen.MagicEntry entry : this.sourceMagics) {
            if (entry.category.contains(category)) {
               return true;
            }
         }

         return false;
      }
   }

   private String nextCategoryRaw(String current) {
      if ("all".equals(current)) {
         return "jewel";
      } else if ("jewel".equals(current)) {
         return "basic";
      } else if ("basic".equals(current)) {
         return "unlimited_blade_works";
      } else if ("unlimited_blade_works".equals(current)) {
         return "other";
      } else {
         return "other".equals(current) ? "nordic" : "all";
      }
   }

   private String getNextCategory(String current) {
      String next = this.nextCategoryRaw(current);

      for (int safety = 0; !"all".equals(next) && !this.isCategoryUnlocked(next) && safety < 8; safety++) {
         next = this.nextCategoryRaw(next);
      }

      return next;
   }

   public void init() {
      super.init();
      int tabY = this.topPos + 5;
      int tabWidth = 80;
      int tabHeight = 16;
      int tabX = this.leftPos + this.imageWidth - tabWidth * 3 - 10;
      this.imagebutton_basic_attributes = new NeonButton(
         tabX, tabY, tabWidth, tabHeight, Component.translatable("gui.typemoonworld.tab.basic_attributes"), e -> {
            PacketDistributor.sendToServer(new Magical_attributes_Button_Message(0, this.x, this.y, this.z), new CustomPacketPayload[0]);
            Magical_attributes_Button_Message.handleButtonAction(this.entity, 0, this.x, this.y, this.z);
         }
      );
      this.addRenderableWidget(this.imagebutton_basic_attributes);
      this.imagebutton_magical_attributes = new NeonButton(
         tabX + tabWidth + 2, tabY, tabWidth, tabHeight, Component.translatable("gui.typemoonworld.tab.body_modification"), e -> {
            this.pageMode = 0;
            ((MagicalattributesMenu)this.menu).setPage(0);
            PacketDistributor.sendToServer(new PageChangeMessage(0), new CustomPacketPayload[0]);
            this.updateVisibility();
         }
      );
      this.addRenderableWidget(this.imagebutton_magical_attributes);
      this.imagebutton_magical_properties = new NeonButton(
         tabX + (tabWidth + 2) * 2, tabY, tabWidth, tabHeight, Component.translatable("gui.typemoonworld.tab.magic_knowledge"), e -> {
            this.pageMode = 1;
            ((MagicalattributesMenu)this.menu).setPage(1);
            PacketDistributor.sendToServer(new PageChangeMessage(1), new CustomPacketPayload[0]);
            this.ensureCrestSourceAvailability();
            this.rebuildSourceMagics();
            this.updateFilteredMagics();
            this.updateVisibility();
         }
      );
      this.addRenderableWidget(this.imagebutton_magical_properties);
      int sourceTabWidth = 52;
      int sourceTabHeight = 14;
      int sourceTabGap = 4;
      int sourceTabsX = this.leftPos + this.imageWidth - sourceTabWidth * 2 - sourceTabGap - 10;
      int sourceTabsY = this.topPos + 30;
      this.tabSelfKnowledge = new NeonButton(
         sourceTabsX, sourceTabsY, sourceTabWidth, sourceTabHeight, Component.translatable("gui.typemoonworld.magic_knowledge.source.self"), e -> {
            this.magicSourceTab = 0;
            this.rebuildSourceMagics();
            this.updateFilteredMagics();
            this.updateSourceTabLabels();
         }, -11557889
      );
      this.addRenderableWidget(this.tabSelfKnowledge);
      this.tabCrestKnowledge = new NeonButton(
         sourceTabsX + sourceTabWidth + sourceTabGap,
         sourceTabsY,
         sourceTabWidth,
         sourceTabHeight,
         Component.translatable("gui.typemoonworld.magic_knowledge.source.crest"),
         e -> {
            if (!this.hasUsableMagicCrest()) {
               this.entity.displayClientMessage(Component.translatable("gui.typemoonworld.magic_knowledge.crest_inactive"), true);
            } else {
               this.magicSourceTab = 1;
               this.rebuildSourceMagics();
               this.updateFilteredMagics();
               this.updateSourceTabLabels();
            }
         },
         -1811878
      );
      this.addRenderableWidget(this.tabCrestKnowledge);
      int filterButtonWidth = this.getFilterButtonWidth();
      this.filterButton = new NeonButton(
         this.getFilterButtonX(), this.topPos + 31, filterButtonWidth, 14, this.getFilterButtonText(this.filterCategory), e -> {
            this.filterCategory = this.getNextCategory(this.filterCategory);
            this.rebuildSourceMagics();
            this.updateFilteredMagics();
            e.setMessage(this.getFilterButtonText(this.filterCategory));
         }
      );
      this.addRenderableWidget(this.filterButton);
      this.wheelSwitchButtons.clear();

      for (int i = 0; i < 10; i++) {
         int wheel = i;
         Button wheelBtn = new NeonButton(
            this.leftPos + 120 + i * 18, this.topPos + 182, 16, 14, Component.literal(String.valueOf(i)), e -> this.switchWheel(wheel)
         );
         this.addRenderableWidget(wheelBtn);
         this.wheelSwitchButtons.add(wheelBtn);
      }

      this.updateWheelButtonLabels();
      this.updateSourceTabLabels();
      this.updateVisibility();
   }

   protected void containerTick() {
      super.containerTick();
      this.ensureCrestSourceAvailability();
      if (this.tabCrestKnowledge != null) {
         this.tabCrestKnowledge.active = this.pageMode == 1 && this.hasUsableMagicCrest();
      }

      if (this.pageMode == 1 && this.minecraft != null && this.minecraft.level != null && this.minecraft.level.getGameTime() % 10L == 0L) {
         this.rebuildSourceMagics();
         this.updateFilteredMagics();
         this.updateWheelButtonLabels();
      }
   }

   private void updateVisibility() {
      boolean knowledgeVisible = this.pageMode == 1;
      boolean bodyVisible = this.pageMode == 0;
      if (this.filterButton != null) {
         this.filterButton.visible = knowledgeVisible;
      }

      if (this.tabSelfKnowledge != null) {
         this.tabSelfKnowledge.visible = knowledgeVisible;
      }

      if (this.tabCrestKnowledge != null) {
         this.tabCrestKnowledge.visible = knowledgeVisible;
      }

      if (this.tabCrestKnowledge != null) {
         this.tabCrestKnowledge.active = knowledgeVisible && this.hasUsableMagicCrest();
      }

      for (Button button : this.wheelSwitchButtons) {
         button.visible = knowledgeVisible;
      }

      if (this.imagebutton_magical_attributes != null) {
         this.imagebutton_magical_attributes.visible = true;
      }

      if (this.imagebutton_magical_properties != null) {
         this.imagebutton_magical_properties.visible = true;
      }

      if (this.imagebutton_basic_attributes != null) {
         this.imagebutton_basic_attributes.visible = true;
      }
   }

   private void switchWheel(int wheel) {
      if (wheel >= 0 && wheel < 10) {
         PacketDistributor.sendToServer(new SwitchMagicWheelMessage(wheel), new CustomPacketPayload[0]);
         TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
         vars.switchActiveWheel(wheel);
         this.updateWheelButtonLabels();
      }
   }

   private void updateWheelButtonLabels() {
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      int active = vars.active_wheel_index;

      for (int i = 0; i < this.wheelSwitchButtons.size(); i++) {
         this.wheelSwitchButtons.get(i).setMessage(Component.literal(i == active ? ">" + i : String.valueOf(i)));
      }
   }

   private void updateSourceTabLabels() {
      this.ensureCrestSourceAvailability();
      if (this.tabSelfKnowledge != null) {
         this.tabSelfKnowledge
            .setMessage(
               Component.translatable(
                  this.magicSourceTab == 0 ? "gui.typemoonworld.magic_knowledge.source.self.active" : "gui.typemoonworld.magic_knowledge.source.self"
               )
            );
      }

      if (this.tabCrestKnowledge != null) {
         this.tabCrestKnowledge
            .setMessage(
               Component.translatable(
                  this.magicSourceTab == 1 ? "gui.typemoonworld.magic_knowledge.source.crest.active" : "gui.typemoonworld.magic_knowledge.source.crest"
               )
            );
      }
   }

   public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
      super.render(guiGraphics, mouseX, mouseY, partialTicks);
      if (Basic_information_back_player_self.execute(this.entity) instanceof LivingEntity livingEntity) {
         this.renderEntityInInventoryFollowsAngle(
            guiGraphics,
            this.leftPos + 60,
            this.topPos + 170,
            (float)Math.atan((this.leftPos + 60 - mouseX) / 40.0),
            (float)Math.atan((this.topPos + 87 - mouseY) / 40.0),
            livingEntity
         );
      }

      if (this.pageMode == 1) {
         this.renderWheelSlots(guiGraphics, mouseX, mouseY);
         this.renderMagicList(guiGraphics, mouseX, mouseY);
         this.checkTooltipHover(mouseX, mouseY);
         if (this.tooltipActive && this.lastHoveredEntry != null && this.presetDialogState == null) {
            this.renderScrollableTooltip(guiGraphics, mouseX, mouseY);
         }

         if (this.draggingEntry != null && this.presetDialogState == null) {
            this.renderDraggingGhost(guiGraphics, mouseX, mouseY);
         }

         if (this.presetDialogState != null) {
            this.renderPresetDialog(guiGraphics, mouseX, mouseY);
         }
      }

      if (this.presetDialogState == null) {
         this.renderTooltip(guiGraphics, mouseX, mouseY);
      }
   }

   private void renderMagicList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      int listX = this.leftPos + 120;
      int listY = this.topPos + 52;
      int visibleItems = 8;

      for (int i = 0; i < visibleItems; i++) {
         int index = this.startIndex + i;
         if (index >= this.filteredMagics.size()) {
            break;
         }

         Magical_attributes_Screen.MagicEntry entry = this.filteredMagics.get(index);
         int col = i % 2;
         int row = i / 2;
         int cardX = listX + col * 70;
         int cardY = listY + row * 30;
         boolean hovered = mouseX >= cardX && mouseX < cardX + 62 && mouseY >= cardY && mouseY < cardY + 22;
         boolean disabled = !entry.active || isKnowledgeOnlyMagic(entry.id);
         boolean crest = "crest".equals(entry.sourceType);
         int borderColor = crest ? -1811878 : -11557889;
         int fillColor = crest ? 1613565716 : 1611669554;
         if (hovered) {
            fillColor = crest ? -1873795536 : -1875885984;
         }

         if (disabled) {
            borderColor = -9539986;
            fillColor = 1613968179;
         }

         guiGraphics.fill(cardX, cardY, cardX + 62, cardY + 22, fillColor);
         guiGraphics.renderOutline(cardX, cardY, 62, 22, borderColor);
         guiGraphics.fill(cardX + 2, cardY + 2, cardX + 6, cardY + 6, crest ? -1811878 : -11557889);
         String name = this.getMagicShortName(entry);
         int textColor = disabled ? -6645094 : -1;
         int textX = cardX + 4;
         int textY = cardY + 7;
         int textMaxWidth = 54;
         if (hovered && this.font.width(name) > textMaxWidth) {
            this.drawMarqueeText(guiGraphics, name, textX, textY, textMaxWidth, textColor);
         } else {
            String shortName = this.clampTextToWidth(name, textMaxWidth);
            guiGraphics.drawCenteredString(this.font, Component.literal(shortName), cardX + 31, textY, textColor);
         }
      }

      int totalRows = (this.filteredMagics.size() + 2 - 1) / 2;
      if (totalRows > 4) {
         int scrollBarX = listX + 132 + 4;
         int scrollBarHeight = 124;
         int barHeight = Math.max(24, (int)((float)(4 * scrollBarHeight) / totalRows));
         int barTop = listY + (int)(this.scrollOffs * (scrollBarHeight - barHeight));
         guiGraphics.fill(scrollBarX, listY, scrollBarX + 6, listY + scrollBarHeight, Integer.MIN_VALUE);
         guiGraphics.fill(scrollBarX, barTop, scrollBarX + 6, barTop + barHeight, -16711681);
      }
   }

   private void renderWheelSlots(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      int activeWheel = vars.active_wheel_index;
      int currentSlot = this.getCurrentSelectedRuntimeSlot(vars);

      for (int slot = 0; slot < 12; slot++) {
         int col = slot % 3;
         int row = slot / 3;
         int slotX = this.leftPos + 272 + col * 25;
         int slotY = this.topPos + 60 + row * 25;
         boolean hovered = mouseX >= slotX && mouseX < slotX + 22 && mouseY >= slotY && mouseY < slotY + 22;
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry wheelEntry = vars.getWheelSlotEntry(activeWheel, slot);
         boolean empty = wheelEntry == null || wheelEntry.isEmpty();
         boolean crest = !empty && "crest".equals(wheelEntry.sourceType);
         boolean castable = !empty && vars.isWheelSlotEntryCastable(wheelEntry);
         int fillColor = 1343229968;
         int borderColor = -13722952;
         if (empty) {
            borderColor = hovered ? -9120532 : -13722952;
         } else if (crest) {
            fillColor = castable ? -2144003552 : -2144128205;
            borderColor = castable ? -1811878 : -7829368;
         } else {
            fillColor = castable ? -2145371560 : -2144128205;
            borderColor = castable ? -11557889 : -7829368;
         }

         if (hovered) {
            fillColor |= 536870912;
         }

         guiGraphics.fill(slotX, slotY, slotX + 22, slotY + 22, fillColor);
         guiGraphics.renderOutline(slotX, slotY, 22, 22, borderColor);
         if (slot == currentSlot) {
            guiGraphics.renderOutline(slotX - 1, slotY - 1, 24, 24, -11174);
         }

         if (hovered && this.draggingEntry != null && this.presetDialogState == null) {
            int dragBorder = this.canPlaceDraggingEntry(vars, this.draggingEntry) ? -12255352 : -6645094;
            guiGraphics.renderOutline(slotX - 2, slotY - 2, 26, 26, dragBorder);
         }

         guiGraphics.drawString(this.font, String.valueOf(slot + 1), slotX + 1, slotY + 1, -5197648, false);
         if (!empty) {
            String name = wheelEntry.displayNameCache != null && !wheelEntry.displayNameCache.isEmpty()
               ? wheelEntry.displayNameCache
               : this.getFallbackMagicName(wheelEntry.magicId);
            String shortName = this.clampTextToWidth(name, 20);
            guiGraphics.drawCenteredString(this.font, Component.literal(shortName), slotX + 11, slotY + 12, -1);
         }
      }
   }

   private int getCurrentSelectedRuntimeSlot(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magic_runtime_slot_indices.size()
         ? vars.selected_magic_runtime_slot_indices.get(vars.current_magic_index)
         : -1;
   }

   private String getMagicShortName(Magical_attributes_Screen.MagicEntry entry) {
      String shortLabel = Component.translatable(entry.nameKey).getString();
      if (shortLabel.equals(entry.nameKey)) {
         String selectedKey = entry.nameKey.replace(".short", ".selected");
         shortLabel = Component.translatable(selectedKey).getString();
         if (shortLabel.equals(selectedKey)) {
            shortLabel = this.getFallbackMagicName(entry.id);
         }
      }

      return shortLabel;
   }

   private String getMagicFullName(Magical_attributes_Screen.MagicEntry entry) {
      String id = entry == null ? "" : entry.id;
      if (id != null && !id.isEmpty()) {
         String nameKey = "magic.typemoonworld." + id + ".name";
         String full = Component.translatable(nameKey).getString();
         if (!full.equals(nameKey)) {
            return full;
         }
      }

      String selectedKey = entry != null && entry.nameKey != null ? entry.nameKey.replace(".short", ".selected") : "";
      if (!selectedKey.isEmpty()) {
         String selected = Component.translatable(selectedKey).getString();
         if (!selected.equals(selectedKey)) {
            return selected;
         }
      }

      return entry == null ? "" : this.getMagicShortName(entry);
   }

   private Component getMagicCardName(Magical_attributes_Screen.MagicEntry entry) {
      return Component.literal(this.clampTextToWidth(this.getMagicShortName(entry), 54));
   }

   private void drawMarqueeText(GuiGraphics guiGraphics, String text, int x, int y, int width, int color) {
      int textWidth = this.font.width(text);
      if (textWidth <= width) {
         guiGraphics.drawString(this.font, text, x, y, color, false);
      } else {
         int gap = 16;
         float speedPixelsPerSecond = 28.0F;
         float cyclePixels = textWidth + 16;
         float cycleDurationMs = cyclePixels / 28.0F * 1000.0F;
         float t = (float)(System.currentTimeMillis() % (long)Math.max(1.0F, cycleDurationMs));
         float offset = t / Math.max(1.0F, cycleDurationMs) * cyclePixels;
         int drawX = x - (int)offset;
         double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
         int scX = (int)(x * guiScale);
         int scY = (int)(Minecraft.getInstance().getWindow().getHeight() - (y + 9 + 1) * guiScale);
         int scW = (int)(width * guiScale);
         int scH = (int)((9 + 1) * guiScale);
         RenderSystem.enableScissor(scX, scY, scW, scH);
         guiGraphics.drawString(this.font, text, drawX, y, color, false);
         guiGraphics.drawString(this.font, text, drawX + textWidth + 16, y, color, false);
         RenderSystem.disableScissor();
      }
   }

   private String getFallbackMagicName(String magicId) {
      if (magicId != null && !magicId.isEmpty()) {
         String byName = Component.translatable("magic.typemoonworld." + magicId + ".name").getString();
         return !byName.equals("magic.typemoonworld." + magicId + ".name") ? byName : magicId;
      } else {
         return "";
      }
   }

   private void renderDraggingGhost(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      String display = this.getMagicCardName(this.draggingEntry).getString();
      int w = Math.max(48, this.font.width(display) + 10);
      int h = 16;
      int x = mouseX + 8;
      int y = mouseY + 8;
      int border = "crest".equals(this.draggingEntry.sourceType) ? -1811878 : -11557889;
      int fill = "crest".equals(this.draggingEntry.sourceType) ? -1607462890 : -1609555912;
      guiGraphics.fill(x, y, x + w, y + h, fill);
      guiGraphics.renderOutline(x, y, w, h, border);
      guiGraphics.drawString(this.font, display, x + 5, y + 4, -1, false);
   }

   private void checkTooltipHover(int mouseX, int mouseY) {
      Magical_attributes_Screen.MagicEntry hovered = this.getMagicEntryAt(mouseX, mouseY);
      if (hovered == null) {
         hovered = this.getWheelMagicEntryAt(mouseX, mouseY);
      }

      if (hovered == null) {
         this.lastHoveredEntry = null;
         this.tooltipActive = false;
         this.hoverStartTime = 0L;
      } else if (!this.isSameMagicEntry(this.lastHoveredEntry, hovered)) {
         this.lastHoveredEntry = hovered;
         this.hoverStartTime = System.currentTimeMillis();
         this.descScrollOffs = 0.0F;
         this.tooltipActive = false;
      } else {
         this.tooltipActive = System.currentTimeMillis() - this.hoverStartTime >= 1000L;
      }
   }

   private boolean isSameMagicEntry(Magical_attributes_Screen.MagicEntry a, Magical_attributes_Screen.MagicEntry b) {
      if (a == b) {
         return true;
      } else {
         return a != null && b != null
            ? Objects.equals(a.id, b.id)
               && Objects.equals(a.sourceType, b.sourceType)
               && Objects.equals(a.crestEntryId, b.crestEntryId)
               && a.wheelSlotIndex == b.wheelSlotIndex
               && Objects.equals(a.presetPayload == null ? "" : a.presetPayload.toString(), b.presetPayload == null ? "" : b.presetPayload.toString())
            : false;
      }
   }

   private Magical_attributes_Screen.MagicEntry getMagicEntryAt(double mouseX, double mouseY) {
      int listX = this.leftPos + 120;
      int listY = this.topPos + 52;
      if (!(mouseX < listX) && !(mouseX >= listX + 132) && !(mouseY < listY) && !(mouseY >= listY + 124)) {
         int visibleItems = 8;

         for (int i = 0; i < visibleItems; i++) {
            int index = this.startIndex + i;
            if (index >= this.filteredMagics.size()) {
               break;
            }

            int col = i % 2;
            int row = i / 2;
            int cardX = listX + col * 70;
            int cardY = listY + row * 30;
            if (mouseX >= cardX && mouseX < cardX + 62 && mouseY >= cardY && mouseY < cardY + 22) {
               return this.filteredMagics.get(index);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private Magical_attributes_Screen.MagicEntry getWheelMagicEntryAt(double mouseX, double mouseY) {
      int slot = this.getWheelSlotAt(mouseX, mouseY);
      if (slot < 0) {
         return null;
      } else {
         TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry wheelEntry = vars.getWheelSlotEntry(vars.active_wheel_index, slot);
         if (wheelEntry != null && !wheelEntry.isEmpty()) {
            Magical_attributes_Screen.MagicEntry entry = this.createDragEntryFromWheel(wheelEntry);
            entry.wheelSlotIndex = slot;
            return entry;
         } else {
            return null;
         }
      }
   }

   private void renderScrollableTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      if (this.lastHoveredEntry != null) {
         List<Component> lines = this.getDescriptionLines(this.lastHoveredEntry);
         int tooltipWidth = 180;
         int contentWidth = tooltipWidth - 10;
         List<FormattedCharSequence> wrappedLines = this.buildWrappedTooltipLines(lines, contentWidth);
         int lineHeight = 9 + 1;
         int padding = 5;
         int contentHeight = wrappedLines.size() * lineHeight;
         int tooltipHeight = Math.min(contentHeight + padding * 2, 126);
         int tooltipX = mouseX + 12;
         int tooltipY = mouseY - 12;
         if (tooltipX + tooltipWidth > this.width) {
            tooltipX = mouseX - tooltipWidth - 12;
         }

         if (tooltipY + tooltipHeight > this.height) {
            tooltipY = this.height - tooltipHeight - 5;
         }

         if (tooltipY < 5) {
            tooltipY = 5;
         }

         guiGraphics.pose().pushPose();
         guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
         guiGraphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, -267386864);
         guiGraphics.renderOutline(tooltipX, tooltipY, tooltipWidth, tooltipHeight, -16733526);
         int viewHeight = tooltipHeight - padding * 2;
         boolean canScroll = contentHeight > viewHeight;
         if (canScroll) {
            int scrollBarX = tooltipX + tooltipWidth - 6;
            int scrollBarY = tooltipY + padding;
            int barHeight = Math.max(10, (int)((float)(viewHeight * viewHeight) / contentHeight));
            int barTop = scrollBarY + (int)(this.descScrollOffs * (viewHeight - barHeight));
            guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + viewHeight, Integer.MIN_VALUE);
            guiGraphics.fill(scrollBarX, barTop, scrollBarX + 4, barTop + barHeight, -1);
         }

         double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
         int scX = (int)((tooltipX + padding) * guiScale);
         int scY = (int)(Minecraft.getInstance().getWindow().getHeight() - (tooltipY + tooltipHeight - padding) * guiScale);
         int scW = (int)(contentWidth * guiScale);
         int scH = (int)(viewHeight * guiScale);
         RenderSystem.enableScissor(scX, scY, scW, scH);
         int startY = tooltipY + padding - (int)(this.descScrollOffs * Math.max(0, contentHeight - viewHeight));

         for (int i = 0; i < wrappedLines.size(); i++) {
            int y = startY + i * lineHeight;
            guiGraphics.drawString(this.font, wrappedLines.get(i), tooltipX + padding, y, -1, false);
         }

         RenderSystem.disableScissor();
         guiGraphics.pose().popPose();
      }
   }

   private List<FormattedCharSequence> buildWrappedTooltipLines(List<Component> lines, int maxWidth) {
      List<FormattedCharSequence> wrapped = new ArrayList<>();
      int width = Math.max(20, maxWidth);

      for (Component line : lines) {
         if (line != null && !line.getString().isEmpty()) {
            List<FormattedCharSequence> split = this.font.split(line, width);
            if (split.isEmpty()) {
               wrapped.add(FormattedCharSequence.EMPTY);
            } else {
               wrapped.addAll(split);
            }
         } else {
            wrapped.add(FormattedCharSequence.EMPTY);
         }
      }

      return wrapped;
   }

   private List<Component> getDescriptionLines(Magical_attributes_Screen.MagicEntry entry) {
      List<Component> lines = new ArrayList<>();
      lines.add(Component.literal(this.getMagicFullName(entry)).withStyle(Style.EMPTY.withColor(entry.color).withBold(true)));
      lines.add(Component.empty());
      if (entry.wheelSlotIndex >= 0) {
         lines.add(Component.literal("槽位 " + (entry.wheelSlotIndex + 1)).withStyle(ChatFormatting.AQUA));
         lines.add(Component.empty());
      }

      if ("crest".equals(entry.sourceType)) {
         String sourceTypeKey = "plunder".equals(entry.crestSourceKind)
            ? "gui.typemoonworld.magic_knowledge.crest_plunder"
            : "gui.typemoonworld.magic_knowledge.crest_self";
         lines.add(Component.translatable(sourceTypeKey).withStyle(ChatFormatting.RED));
         lines.add(
            Component.translatable(entry.active ? "gui.typemoonworld.magic_knowledge.crest_active" : "gui.typemoonworld.magic_knowledge.crest_inactive")
               .withStyle(entry.active ? ChatFormatting.GREEN : ChatFormatting.GRAY)
         );
         if (entry.originName != null && !entry.originName.isEmpty()) {
            lines.add(
               Component.translatable("gui.typemoonworld.magic_knowledge.crest_origin", new Object[]{entry.originName}).withStyle(ChatFormatting.DARK_RED)
            );
         }

         if ("plunder".equals(entry.crestSourceKind)) {
            lines.add(Component.translatable("gui.typemoonworld.magic_knowledge.crest_preset_locked").withStyle(ChatFormatting.DARK_RED));
         }

         lines.add(Component.empty());
      }

      this.appendPresetPayloadLines(lines, entry.id, entry.presetPayload);
      String descKey = "magic.typemoonworld." + entry.id + ".desc";
      this.appendDescriptionLines(lines, descKey, ChatFormatting.GRAY);
      double proficiency = this.getMagicProficiency(entry);
      if (proficiency >= 0.0) {
         lines.add(Component.empty());
         lines.add(Component.translatable("gui.typemoonworld.proficiency", new Object[]{String.format("%.1f%%", proficiency)}).withStyle(ChatFormatting.GOLD));
      }

      if (isKnowledgeOnlyMagic(entry.id)) {
         lines.add(Component.empty());
         lines.add(Component.translatable("gui.typemoonworld.magic.knowledge_only.note").withStyle(ChatFormatting.DARK_GRAY));
      }

      return lines;
   }

   private void appendPresetPayloadLines(List<Component> lines, String magicId, CompoundTag payload) {
      if (payload != null && !payload.isEmpty()) {
         lines.add(Component.translatable("gui.typemoonworld.magic_knowledge.preset_applied").withStyle(ChatFormatting.GOLD));
         if ("reinforcement".equals(magicId)) {
            int target = payload.contains("reinforcement_target") ? payload.getInt("reinforcement_target") : 0;
            int mode = payload.contains("reinforcement_mode") ? payload.getInt("reinforcement_mode") : 0;
            int level = payload.contains("reinforcement_level") ? payload.getInt("reinforcement_level") : 1;

            String targetName = switch (target) {
               case 1 -> Component.translatable("gui.typemoonworld.mode.other").getString();
               case 2 -> Component.translatable("gui.typemoonworld.mode.item").getString();
               case 3 -> Component.translatable("gui.typemoonworld.mode.cancel").getString();
               default -> Component.translatable("gui.typemoonworld.mode.self").getString();
            };
            lines.add(Component.literal(" - " + targetName).withStyle(ChatFormatting.YELLOW));
            if (target == 3) {
               String cancelName = switch (mode) {
                  case 1 -> Component.translatable("gui.typemoonworld.mode.cancel.other").getString();
                  case 2 -> Component.translatable("gui.typemoonworld.mode.cancel.item").getString();
                  default -> Component.translatable("gui.typemoonworld.mode.cancel.self").getString();
               };
               lines.add(Component.literal(" - " + cancelName).withStyle(ChatFormatting.YELLOW));
            } else {
               if (target != 2) {
                  String partName = switch (mode) {
                     case 1 -> Component.translatable("gui.typemoonworld.mode.hand").getString();
                     case 2 -> Component.translatable("gui.typemoonworld.mode.leg").getString();
                     case 3 -> Component.translatable("gui.typemoonworld.mode.eye").getString();
                     default -> Component.translatable("gui.typemoonworld.mode.body").getString();
                  };
                  lines.add(Component.literal(" - " + partName).withStyle(ChatFormatting.YELLOW));
               }

               lines.add(Component.translatable("gui.typemoonworld.mode.level", new Object[]{level}).withStyle(ChatFormatting.YELLOW));
            }
         } else if ("gravity_magic".equals(magicId)) {
            int target = payload.contains("gravity_target") ? payload.getInt("gravity_target") : 0;
            int mode = payload.contains("gravity_mode") ? payload.getInt("gravity_mode") : 0;
            String targetName = target == 0
               ? Component.translatable("gui.typemoonworld.mode.self").getString()
               : Component.translatable("gui.typemoonworld.mode.other").getString();

            String modeName = switch (mode) {
               case -2 -> Component.translatable("gui.typemoonworld.mode.gravity.ultra_light").getString();
               case -1 -> Component.translatable("gui.typemoonworld.mode.gravity.light").getString();
               default -> Component.translatable("gui.typemoonworld.mode.gravity.normal").getString();
               case 1 -> Component.translatable("gui.typemoonworld.mode.gravity.heavy").getString();
               case 2 -> Component.translatable("gui.typemoonworld.mode.gravity.ultra_heavy").getString();
            };
            lines.add(Component.literal(" - " + targetName + " / " + modeName).withStyle(ChatFormatting.YELLOW));
         } else if ("gandr_machine_gun".equals(magicId)) {
            int mode = payload.contains("gandr_machine_gun_mode") ? payload.getInt("gandr_machine_gun_mode") : 0;
            String modeName = mode == 1
               ? Component.translatable("gui.typemoonworld.mode.gandr_barrage").getString()
               : Component.translatable("gui.typemoonworld.mode.gandr_rapid").getString();
            lines.add(Component.literal(" - " + modeName).withStyle(ChatFormatting.YELLOW));
         } else {
            if ("projection".equals(magicId)) {
               if (payload.getBoolean("projection_lock_empty")) {
                  lines.add(Component.literal(" - 锁空").withStyle(ChatFormatting.YELLOW));
                  return;
               }

               if (payload.contains("projection_structure_id")) {
                  lines.add(Component.literal(" - 结构: " + payload.getString("projection_structure_id")).withStyle(ChatFormatting.YELLOW));
                  return;
               }

               if (payload.contains("projection_item", 10)) {
                  ItemStack.parse(this.entity.registryAccess(), payload.getCompound("projection_item"))
                     .ifPresentOrElse(
                        stack -> lines.add(Component.literal(" - 物品: " + stack.getHoverName().getString()).withStyle(ChatFormatting.YELLOW)),
                        () -> lines.add(Component.literal(" - 物品: ?").withStyle(ChatFormatting.YELLOW))
                     );
               }
            }
         }
      }
   }

   private void appendDescriptionLines(List<Component> lines, String baseKey, ChatFormatting style) {
      if (I18n.exists(baseKey)) {
         for (String line : I18n.get(baseKey, new Object[0]).split("\\n")) {
            lines.add(Component.literal(line).withStyle(style));
         }
      }

      for (int i = 0; i < 10; i++) {
         String subKey = baseKey + "." + i;
         if (!I18n.exists(subKey)) {
            if (i > 0) {
               break;
            }
         } else {
            if (I18n.exists(baseKey) || i > 0) {
               lines.add(Component.empty());
            }

            for (String line : I18n.get(subKey, new Object[0]).split("\\n")) {
               lines.add(Component.literal(line).withStyle(style));
            }
         }
      }
   }

   private double getMagicProficiency(Magical_attributes_Screen.MagicEntry entry) {
      return entry != null && "crest".equals(entry.sourceType) ? 100.0 : this.getMagicProficiency(entry == null ? "" : entry.id);
   }

   private double getMagicProficiency(String magicId) {
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      String normalized = normalizeMagicIdForDisplay(magicId);

      return switch (normalized) {
         case "structural_analysis" -> vars.proficiency_structural_analysis;
         case "projection" -> vars.proficiency_projection;
         case "unlimited_blade_works" -> vars.proficiency_unlimited_blade_works;
         case "sword_barrel_full_open" -> vars.proficiency_sword_barrel_full_open;
         case "jewel_magic_shoot", "jewel_random_shoot" -> vars.proficiency_jewel_magic_shoot;
         case "jewel_magic_release" -> vars.proficiency_jewel_magic_release;
         case "gravity_magic" -> vars.proficiency_gravity_magic;
         case "gander", "gandr_machine_gun" -> vars.proficiency_gander;
         case "reinforcement" -> vars.proficiency_reinforcement;
         default -> -1.0;
      };
   }

   private static String normalizeMagicIdForDisplay(String magicId) {
      return !"reinforcement_self".equals(magicId) && !"reinforcement_other".equals(magicId) && !"reinforcement_item".equals(magicId)
         ? magicId
         : "reinforcement";
   }

   protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int x = this.leftPos;
      int y = this.topPos;
      GuiUtils.renderBackground(guiGraphics, x, y, this.imageWidth, this.imageHeight);
      GuiUtils.renderTechFrame(guiGraphics, x + 10, y + 35, 100, 150, -16733526, -16711681);
      if (this.pageMode == 0) {
         int invX = x + 145;
         int invY = y + 80;
         int invW = 172;
         int invH = 85;
         GuiUtils.renderTechFrame(guiGraphics, invX, invY, invW, invH, -16733526, -16742145);
         int eyeSlotX = x + 193 - 1;
         int eyeSlotY = y + 51 - 1;
         int eyeSlotSize = 18;
         GuiUtils.renderTechFrame(guiGraphics, eyeSlotX, eyeSlotY, eyeSlotSize, eyeSlotSize, -65281, -65400);
         int crestSlotX = x + 223 - 1;
         int crestSlotY = y + 51 - 1;
         GuiUtils.renderTechFrame(guiGraphics, crestSlotX, crestSlotY, eyeSlotSize, eyeSlotSize, -1811878, -5227974);

         for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
               int slotX = x + 150 + j * 18 - 1;
               int slotY = y + 85 + i * 18 - 1;
               guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 553648127);
            }
         }

         for (int k = 0; k < 9; k++) {
            int slotX = x + 150 + k * 18 - 1;
            int slotY = y + 143 - 1;
            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 553648127);
         }
      } else {
         GuiUtils.renderTechFrame(guiGraphics, x + 120 - 6, y + 52 - 6, 148, 136, -13722952, -11549464);
      }

      RenderSystem.disableBlend();
   }

   protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
      if (this.pageMode == 0) {
         int eyeLabelX = 189;
         int crestLabelX = 219;
         guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.body_modification.slot.mystic_eyes"), eyeLabelX, 41, -32513, false);
         guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.body_modification.slot.magic_crest"), crestLabelX, 41, -1811878, false);
      } else {
         TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
         guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.screen.learned_magic"), 120, 24, -16719648, false);
         guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.category.label"), 120, 34, -5592406, false);
         guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.magic_knowledge.wheel_slots"), 272, 50, -1811878, false);
         guiGraphics.drawString(
            this.font, Component.translatable("gui.typemoonworld.magic_knowledge.wheel_page", new Object[]{vars.active_wheel_index}), 120, 170, -3355444, false
         );
         guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.magic_knowledge.drag_hint"), 120, 150, -7697782, false);
         guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.magic_knowledge.clear_hint"), 120, 160, -7697782, false);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.pageMode == 1) {
         if (this.presetDialogState != null) {
            return this.handlePresetDialogClick(mouseX, mouseY, button);
         }

         int listX = this.leftPos + 120;
         int listY = this.topPos + 52;
         int scrollBarX = listX + 132 + 4;
         if (mouseX >= scrollBarX && mouseX < scrollBarX + 6 && mouseY >= listY && mouseY < listY + 124) {
            this.scrolling = true;
            return true;
         }

         int targetSlot = this.getWheelSlotAt(mouseX, mouseY);
         if (button == 1 && targetSlot >= 0) {
            this.sendClearWheelSlot(targetSlot);
            return true;
         }

         if (button == 0 && targetSlot >= 0) {
            TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry wheelEntry = vars.getWheelSlotEntry(vars.active_wheel_index, targetSlot);
            if (wheelEntry != null && !wheelEntry.isEmpty()) {
               this.draggingEntry = this.createDragEntryFromWheel(wheelEntry);
               this.draggingFromWheel = true;
               this.draggingFromWheelSlot = targetSlot;
               return true;
            }
         }

         if (button == 0) {
            Magical_attributes_Screen.MagicEntry clickedEntry = this.getMagicEntryAt(mouseX, mouseY);
            if (clickedEntry != null) {
               if (!clickedEntry.active) {
                  this.entity.displayClientMessage(Component.translatable("gui.typemoonworld.magic_knowledge.crest_inactive"), true);
                  return true;
               }

               if (isKnowledgeOnlyMagic(clickedEntry.id)) {
                  this.entity.displayClientMessage(Component.translatable("message.typemoonworld.magic.knowledge_only"), true);
                  return true;
               }

               this.draggingEntry = clickedEntry.copy();
               this.draggingFromWheel = false;
               this.draggingFromWheelSlot = -1;
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.scrolling = false;
      }

      if (this.pageMode == 1 && button == 0 && this.draggingEntry != null) {
         int targetSlot = this.getWheelSlotAt(mouseX, mouseY);
         if (targetSlot >= 0) {
            if (this.draggingFromWheel) {
               if (this.draggingFromWheelSlot >= 0) {
                  if (this.draggingFromWheelSlot != targetSlot) {
                     this.sendSwapWheelSlot(this.draggingFromWheelSlot, targetSlot);
                  } else {
                     this.selectWheelSlotAsCurrent(targetSlot);
                  }
               }
            } else {
               CompoundTag payload = this.draggingEntry.presetPayload == null ? new CompoundTag() : this.draggingEntry.presetPayload.copy();
               if (this.shouldOpenPresetDialog(this.draggingEntry, payload)) {
                  this.openPresetDialog(this.draggingEntry.copy(), targetSlot);
               } else {
                  if ("crest".equals(this.draggingEntry.sourceType) && payload.isEmpty() && this.isMultiOptionMagic(this.draggingEntry.id)) {
                     if ("plunder".equals(this.draggingEntry.crestSourceKind)) {
                        payload = this.resolvePlunderLockedPayload(this.draggingEntry, payload);
                     } else {
                        payload = this.buildDefaultPresetPayloadFromCurrent(this.draggingEntry.id);
                     }
                  }

                  this.sendSetWheelSlot(targetSlot, this.draggingEntry, payload);
               }
            }
         }

         this.draggingEntry = null;
         this.draggingFromWheel = false;
         this.draggingFromWheelSlot = -1;
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   private void selectWheelSlotAsCurrent(int slot) {
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      int runtimeIndex = vars.selected_magic_runtime_slot_indices.indexOf(slot);
      if (runtimeIndex >= 0) {
         PacketDistributor.sendToServer(new SwitchMagicIndexMessage(runtimeIndex), new CustomPacketPayload[0]);
         vars.current_magic_index = runtimeIndex;
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.scrolling && this.pageMode == 1) {
         float d = (float)dragY / 124.0F;
         this.scrollOffs = Mth.clamp(this.scrollOffs + d, 0.0F, 1.0F);
         int totalRows = (this.filteredMagics.size() + 2 - 1) / 2;
         int maxStartRow = Math.max(0, totalRows - 4);
         int startRow = maxStartRow == 0 ? 0 : Math.round(this.scrollOffs * maxStartRow);
         this.startIndex = startRow * 2;
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
      if (this.pageMode == 1) {
         if (this.presetDialogState != null && this.scrollPresetDialog(mouseX, mouseY, deltaY)) {
            return true;
         }

         if (this.tooltipActive && this.lastHoveredEntry != null) {
            List<Component> lines = this.getDescriptionLines(this.lastHoveredEntry);
            int contentWidth = 170;
            int lineHeight = 9 + 1;
            int contentHeight = this.buildWrappedTooltipLines(lines, contentWidth).size() * lineHeight;
            int viewHeight = 116;
            if (contentHeight > viewHeight) {
               float step = 10.0F / (contentHeight - viewHeight);
               this.descScrollOffs = Mth.clamp(this.descScrollOffs - (float)deltaY * step * 3.0F, 0.0F, 1.0F);
               return true;
            }
         }

         int listX = this.leftPos + 120;
         int listY = this.topPos + 52;
         if (mouseX >= listX && mouseX < listX + 132 && mouseY >= listY && mouseY < listY + 124) {
            int totalRows = (this.filteredMagics.size() + 2 - 1) / 2;
            int maxStartRow = Math.max(0, totalRows - 4);
            if (maxStartRow > 0) {
               float step = 1.0F / maxStartRow;
               this.scrollOffs = Mth.clamp(this.scrollOffs - (float)deltaY * step, 0.0F, 1.0F);
               int startRow = Math.round(this.scrollOffs * maxStartRow);
               this.startIndex = startRow * 2;
               return true;
            }
         }
      }

      return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
   }

   public boolean keyPressed(int key, int b, int c) {
      if (key == 256) {
         if (this.presetDialogState != null) {
            this.presetDialogState = null;
            this.presetDialogScrollOffs = 0.0F;
            return true;
         } else {
            if (this.minecraft != null && this.minecraft.player != null) {
               this.minecraft.player.closeContainer();
            }

            return true;
         }
      } else {
         if (this.pageMode == 1) {
            int wheel = this.keyToWheelIndex(key);
            if (wheel >= 0) {
               this.switchWheel(wheel);
               return true;
            }
         }

         return super.keyPressed(key, b, c);
      }
   }

   private int keyToWheelIndex(int keyCode) {
      return switch (keyCode) {
         case 48, 320 -> 0;
         case 49, 321 -> 1;
         case 50, 322 -> 2;
         case 51, 323 -> 3;
         case 52, 324 -> 4;
         case 53, 325 -> 5;
         case 54, 326 -> 6;
         case 55, 327 -> 7;
         case 56, 328 -> 8;
         case 57, 329 -> 9;
         default -> -1;
      };
   }

   private int getWheelSlotAt(double mouseX, double mouseY) {
      for (int slot = 0; slot < 12; slot++) {
         int col = slot % 3;
         int row = slot / 3;
         int slotX = this.leftPos + 272 + col * 25;
         int slotY = this.topPos + 60 + row * 25;
         if (mouseX >= slotX && mouseX < slotX + 22 && mouseY >= slotY && mouseY < slotY + 22) {
            return slot;
         }
      }

      return -1;
   }

   private Magical_attributes_Screen.MagicEntry createDragEntryFromWheel(TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry wheelEntry) {
      Magical_attributes_Screen.MagicEntry base = this.magicCatalogById.get(wheelEntry.magicId);
      Magical_attributes_Screen.MagicEntry entry = base == null
         ? new Magical_attributes_Screen.MagicEntry(
            wheelEntry.magicId, "key.typemoonworld.magic." + wheelEntry.magicId + ".short", this.resolveFallbackCategory(wheelEntry.magicId), -11557889
         )
         : base.copy();
      entry.sourceType = wheelEntry.sourceType == null ? "self" : wheelEntry.sourceType;
      entry.crestEntryId = wheelEntry.crestEntryId == null ? "" : wheelEntry.crestEntryId;
      entry.presetPayload = wheelEntry.presetPayload == null ? new CompoundTag() : wheelEntry.presetPayload.copy();
      if ("crest".equals(entry.sourceType)) {
         TypeMoonWorldModVariables.PlayerVariables.CrestEntry crest = this.getVars().getCrestEntryById(entry.crestEntryId);
         if (crest != null) {
            entry.crestSourceKind = crest.sourceKind == null ? "self" : crest.sourceKind;
            entry.originName = crest.originOwnerName == null ? "" : crest.originOwnerName;
            entry.active = crest.active;
         }
      }

      return entry;
   }

   private String resolveFallbackCategory(String magicId) {
      if (magicId != null && !magicId.isEmpty()) {
         if (MagicClassification.getSchoolType(magicId) == MagicClassification.MagicSchoolType.NORDIC) {
            return "nordic";
         } else {
            return magicId.startsWith("jewel_") ? "jewel" : "other";
         }
      } else {
         return "other";
      }
   }

   private boolean canPlaceDraggingEntry(TypeMoonWorldModVariables.PlayerVariables vars, Magical_attributes_Screen.MagicEntry entry) {
      if (entry == null || entry.id == null || entry.id.isEmpty() || isKnowledgeOnlyMagic(entry.id)) {
         return false;
      } else {
         return !"crest".equals(entry.sourceType) ? hasLearnedMagic(vars, entry.id) : entry.active && vars.hasValidImplantedCrest();
      }
   }

   private void sendClearWheelSlot(int slot) {
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      int wheel = vars.active_wheel_index;
      PacketDistributor.sendToServer(new MagicWheelSlotEditMessage(1, wheel, slot, -1, "self", "", new CompoundTag(), "", ""), new CustomPacketPayload[0]);
      vars.clearWheelSlotEntry(wheel, slot);
      vars.rebuildSelectedMagicsFromActiveWheel();
   }

   private void sendSwapWheelSlot(int fromSlot, int toSlot) {
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      int wheel = vars.active_wheel_index;
      PacketDistributor.sendToServer(
         new MagicWheelSlotEditMessage(2, wheel, fromSlot, toSlot, "self", "", new CompoundTag(), "", ""), new CustomPacketPayload[0]
      );
      vars.swapWheelSlots(wheel, fromSlot, toSlot);
      vars.rebuildSelectedMagicsFromActiveWheel();
   }

   private void sendSetWheelSlot(int slot, Magical_attributes_Screen.MagicEntry entry, CompoundTag payload) {
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      int wheel = vars.active_wheel_index;
      CompoundTag finalPayload = payload == null ? new CompoundTag() : payload.copy();
      if ("crest".equals(entry.sourceType) && "plunder".equals(entry.crestSourceKind)) {
         finalPayload = this.resolvePlunderLockedPayload(entry, finalPayload);
      }

      String displayName = this.buildWheelDisplayName(entry, finalPayload);
      PacketDistributor.sendToServer(
         new MagicWheelSlotEditMessage(0, wheel, slot, -1, entry.sourceType, entry.id, finalPayload, entry.crestEntryId, displayName),
         new CustomPacketPayload[0]
      );
      TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry local = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(wheel, slot);
      local.sourceType = entry.sourceType;
      local.magicId = entry.id;
      local.presetPayload = finalPayload.copy();
      local.crestEntryId = entry.crestEntryId;
      local.displayNameCache = displayName;
      vars.setWheelSlotEntry(wheel, slot, local);
      vars.rebuildSelectedMagicsFromActiveWheel();
   }

   private String buildWheelDisplayName(Magical_attributes_Screen.MagicEntry entry, CompoundTag payload) {
      String base = this.getMagicCardName(entry).getString();
      if (payload == null || payload.isEmpty()) {
         return base;
      } else if ("reinforcement".equals(entry.id)) {
         int target = payload.contains("reinforcement_target") ? payload.getInt("reinforcement_target") : 0;

         String targetName = switch (target) {
            case 1 -> Component.translatable("gui.typemoonworld.overlay.reinforcement.target.other.short").getString();
            case 2 -> Component.translatable("gui.typemoonworld.overlay.reinforcement.target.item.short").getString();
            case 3 -> Component.translatable("gui.typemoonworld.overlay.reinforcement.target.cancel.short").getString();
            default -> Component.translatable("gui.typemoonworld.overlay.reinforcement.target.self.short").getString();
         };
         int level = payload.contains("reinforcement_level") ? payload.getInt("reinforcement_level") : 1;
         return base + " " + targetName + " L" + level;
      } else if ("gravity_magic".equals(entry.id)) {
         int target = payload.contains("gravity_target") ? payload.getInt("gravity_target") : 0;
         int mode = payload.contains("gravity_mode") ? payload.getInt("gravity_mode") : 0;
         String targetName = target == 0
            ? Component.translatable("gui.typemoonworld.overlay.gravity.target.self.short").getString()
            : Component.translatable("gui.typemoonworld.overlay.gravity.target.other.short").getString();

         String modeName = switch (mode) {
            case -2 -> Component.translatable("gui.typemoonworld.overlay.gravity.mode.ultra_light.short").getString();
            case -1 -> Component.translatable("gui.typemoonworld.overlay.gravity.mode.light.short").getString();
            default -> Component.translatable("gui.typemoonworld.overlay.gravity.mode.normal.short").getString();
            case 1 -> Component.translatable("gui.typemoonworld.overlay.gravity.mode.heavy.short").getString();
            case 2 -> Component.translatable("gui.typemoonworld.overlay.gravity.mode.ultra_heavy.short").getString();
         };
         return base + " " + targetName + "/" + modeName;
      } else if ("gandr_machine_gun".equals(entry.id)) {
         int mode = payload.contains("gandr_machine_gun_mode") ? payload.getInt("gandr_machine_gun_mode") : 0;
         String modeName = mode == 1
            ? Component.translatable("gui.typemoonworld.overlay.gandr.mode.barrage.short").getString()
            : Component.translatable("gui.typemoonworld.overlay.gandr.mode.rapid.short").getString();
         return base + " " + modeName;
      } else {
         if ("projection".equals(entry.id)) {
            if (payload.contains("projection_structure_id")) {
               return base + " " + Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.structure").getString();
            }

            if (payload.contains("projection_item", 10)) {
               return base + " " + Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.item").getString();
            }

            if (payload.getBoolean("projection_lock_empty")) {
               return base + " EMPTY";
            }
         }

         return base;
      }
   }

   private boolean isMultiOptionMagic(String magicId) {
      return "reinforcement".equals(magicId) || "gravity_magic".equals(magicId) || "gandr_machine_gun".equals(magicId) || "projection".equals(magicId);
   }

   private boolean shouldOpenPresetDialog(Magical_attributes_Screen.MagicEntry entry, CompoundTag payload) {
      if (!"crest".equals(entry.sourceType)) {
         return false;
      } else if ("plunder".equals(entry.crestSourceKind)) {
         return false;
      } else {
         return !this.isMultiOptionMagic(entry.id) ? false : payload == null || payload.isEmpty();
      }
   }

   private void openPresetDialog(Magical_attributes_Screen.MagicEntry entry, int targetSlot) {
      String stageId = this.getInitialPresetStage(entry.id);
      if (stageId == null) {
         this.sendSetWheelSlot(targetSlot, entry, this.buildDefaultPresetPayloadFromCurrent(entry.id));
      } else {
         this.openPresetDialogStage(entry, targetSlot, stageId, new CompoundTag());
      }
   }

   private String getInitialPresetStage(String magicId) {
      return switch (magicId) {
         case "reinforcement" -> "reinforcement_target";
         case "gravity_magic" -> "gravity_target";
         case "gandr_machine_gun" -> "gandr_mode";
         case "projection" -> "projection_source";
         default -> null;
      };
   }

   private void openPresetDialogStage(Magical_attributes_Screen.MagicEntry entry, int targetSlot, String stageId, CompoundTag draftPayload) {
      List<Magical_attributes_Screen.PresetOption> options = this.buildPresetStageOptions(entry, stageId, draftPayload);
      if (options.isEmpty()) {
         this.sendSetWheelSlot(targetSlot, entry, draftPayload);
      } else {
         Component title = Component.translatable("gui.typemoonworld.magic_knowledge.preset.title", new Object[]{this.getMagicCardName(entry).getString()});
         this.presetDialogState = new Magical_attributes_Screen.PresetDialogState(entry, targetSlot, stageId, draftPayload, title, options);
         this.presetDialogScrollOffs = 0.0F;
      }
   }

   private List<Magical_attributes_Screen.PresetOption> buildPresetStageOptions(
      Magical_attributes_Screen.MagicEntry entry, String stageId, CompoundTag draftPayload
   ) {
      List<Magical_attributes_Screen.PresetOption> options = new ArrayList<>();
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      String magicId = entry.id;
      if ("reinforcement".equals(magicId) && "reinforcement_target".equals(stageId)) {
         CompoundTag self = new CompoundTag();
         self.putInt("reinforcement_target", 0);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.self"), self, "reinforcement_mode", false));
         CompoundTag other = new CompoundTag();
         other.putInt("reinforcement_target", 1);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.other"), other, "reinforcement_mode", false));
         CompoundTag item = new CompoundTag();
         item.putInt("reinforcement_target", 2);
         item.putInt("reinforcement_mode", 0);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.item"), item, "reinforcement_level", false));
         CompoundTag cancel = new CompoundTag();
         cancel.putInt("reinforcement_target", 3);
         options.add(
            new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.cancel"), cancel, "reinforcement_cancel_type", false)
         );
         return options;
      } else if ("reinforcement".equals(magicId) && "reinforcement_mode".equals(stageId)) {
         CompoundTag body = new CompoundTag();
         body.putInt("reinforcement_mode", 0);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.body"), body, "reinforcement_level", false));
         CompoundTag hand = new CompoundTag();
         hand.putInt("reinforcement_mode", 1);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.hand"), hand, "reinforcement_level", false));
         CompoundTag leg = new CompoundTag();
         leg.putInt("reinforcement_mode", 2);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.leg"), leg, "reinforcement_level", false));
         CompoundTag eye = new CompoundTag();
         eye.putInt("reinforcement_mode", 3);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.eye"), eye, "reinforcement_level", false));
         return options;
      } else if ("reinforcement".equals(magicId) && "reinforcement_cancel_type".equals(stageId)) {
         CompoundTag cancelSelf = new CompoundTag();
         cancelSelf.putInt("reinforcement_mode", 0);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.cancel.self"), cancelSelf));
         CompoundTag cancelOther = new CompoundTag();
         cancelOther.putInt("reinforcement_mode", 1);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.cancel.other"), cancelOther));
         CompoundTag cancelItem = new CompoundTag();
         cancelItem.putInt("reinforcement_mode", 2);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.cancel.item"), cancelItem));
         return options;
      } else if ("reinforcement".equals(magicId) && "reinforcement_level".equals(stageId)) {
         int maxLevel = "crest".equals(entry.sourceType) ? 5 : Math.max(1, Math.min(5, 1 + (int)(vars.proficiency_reinforcement / 20.0)));

         for (int level = 1; level <= maxLevel; level++) {
            CompoundTag patch = new CompoundTag();
            patch.putInt("reinforcement_level", level);
            options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.level", new Object[]{level}), patch));
         }

         return options;
      } else if ("gravity_magic".equals(magicId) && "gravity_target".equals(stageId)) {
         CompoundTag self = new CompoundTag();
         self.putInt("gravity_target", 0);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.self"), self, "gravity_mode", false));
         CompoundTag other = new CompoundTag();
         other.putInt("gravity_target", 1);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.other"), other, "gravity_mode", false));
         return options;
      } else if ("gravity_magic".equals(magicId) && "gravity_mode".equals(stageId)) {
         int[] modes = new int[]{-2, -1, 0, 1, 2};
         String[] keys = new String[]{
            "gui.typemoonworld.mode.gravity.ultra_light",
            "gui.typemoonworld.mode.gravity.light",
            "gui.typemoonworld.mode.gravity.normal",
            "gui.typemoonworld.mode.gravity.heavy",
            "gui.typemoonworld.mode.gravity.ultra_heavy"
         };

         for (int i = 0; i < modes.length; i++) {
            CompoundTag patch = new CompoundTag();
            patch.putInt("gravity_mode", modes[i]);
            options.add(new Magical_attributes_Screen.PresetOption(Component.translatable(keys[i]), patch));
         }

         return options;
      } else if ("gandr_machine_gun".equals(magicId) && "gandr_mode".equals(stageId)) {
         CompoundTag rapid = new CompoundTag();
         rapid.putInt("gandr_machine_gun_mode", 0);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.gandr_rapid"), rapid));
         CompoundTag barrage = new CompoundTag();
         barrage.putInt("gandr_machine_gun_mode", 1);
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.mode.gandr_barrage"), barrage));
         return options;
      } else if ("projection".equals(magicId) && "projection_source".equals(stageId)) {
         options.addAll(this.buildProjectionPresetOptions(vars));
         return options;
      } else {
         options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.magic_knowledge.preset.default"), draftPayload));
         return options;
      }
   }

   private List<Magical_attributes_Screen.PresetOption> buildProjectionPresetOptions(TypeMoonWorldModVariables.PlayerVariables vars) {
      List<Magical_attributes_Screen.PresetOption> options = new ArrayList<>();
      HashSet<String> structureIds = new HashSet<>();

      for (TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure : vars.analyzed_structures) {
         if (structure != null && TypeMoonWorldModVariables.PlayerVariables.isTrustedStructure(structure)) {
            String id = structure.id == null ? "" : structure.id;
            if (!id.isEmpty() && structureIds.add(id)) {
               CompoundTag payload = new CompoundTag();
               payload.putString("projection_structure_id", id);
               String name = structure.name != null && !structure.name.isEmpty() ? structure.name : id;
               String prefix = Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.structure").getString();
               options.add(new Magical_attributes_Screen.PresetOption(Component.literal("[" + prefix + "] " + name), payload));
            }
         }
      }

      for (ItemStack stack : vars.analyzed_items) {
         if (stack != null && !stack.isEmpty()) {
            CompoundTag payload = new CompoundTag();
            payload.put("projection_item", stack.save(this.entity.registryAccess()));
            String prefix = Component.translatable("gui.typemoonworld.gem_carving_table.projection_mode.item").getString();
            options.add(new Magical_attributes_Screen.PresetOption(Component.literal("[" + prefix + "] " + stack.getHoverName().getString()), payload));
         }
      }

      CompoundTag fallback = this.buildDefaultPresetPayloadFromCurrent("projection");
      if (fallback.isEmpty()) {
         fallback.putBoolean("projection_lock_empty", true);
      }

      options.add(new Magical_attributes_Screen.PresetOption(Component.translatable("gui.typemoonworld.magic_knowledge.preset.projection.default"), fallback));
      return options;
   }

   private static CompoundTag mergePayload(CompoundTag base, CompoundTag patch) {
      CompoundTag merged = base == null ? new CompoundTag() : base.copy();
      if (patch != null && !patch.isEmpty()) {
         for (String key : patch.getAllKeys()) {
            merged.put(key, patch.get(key).copy());
         }

         return merged;
      } else {
         return merged;
      }
   }

   private CompoundTag resolvePlunderLockedPayload(Magical_attributes_Screen.MagicEntry entry, CompoundTag fallback) {
      if (entry != null && "crest".equals(entry.sourceType) && "plunder".equals(entry.crestSourceKind)) {
         TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry = this.getVars().getCrestEntryById(entry.crestEntryId);
         if (crestEntry != null && crestEntry.presetPayload != null && !crestEntry.presetPayload.isEmpty()) {
            return crestEntry.presetPayload.copy();
         } else if (entry.presetPayload != null && !entry.presetPayload.isEmpty()) {
            return entry.presetPayload.copy();
         } else {
            return fallback == null ? new CompoundTag() : fallback.copy();
         }
      } else {
         return fallback == null ? new CompoundTag() : fallback.copy();
      }
   }

   private CompoundTag buildDefaultPresetPayloadFromCurrent(String magicId) {
      TypeMoonWorldModVariables.PlayerVariables vars = this.getVars();
      if ("reinforcement".equals(magicId)) {
         return this.buildReinforcementPayload(
            Mth.clamp(vars.reinforcement_target, 0, 3), Mth.clamp(vars.reinforcement_mode, 0, 3), Mth.clamp(vars.reinforcement_level, 1, 5)
         );
      } else if ("gravity_magic".equals(magicId)) {
         return this.buildGravityPayload(Mth.clamp(vars.gravity_magic_target, 0, 1), Mth.clamp(vars.gravity_magic_mode, -2, 2));
      } else if (!"projection".equals(magicId)) {
         if ("gandr_machine_gun".equals(magicId)) {
            CompoundTag payload = new CompoundTag();
            payload.putInt("gandr_machine_gun_mode", Mth.clamp(vars.gandr_machine_gun_mode, 0, 1));
            return payload;
         } else {
            return new CompoundTag();
         }
      } else {
         CompoundTag payload = new CompoundTag();
         if (vars.projection_selected_structure_id != null && !vars.projection_selected_structure_id.isEmpty()) {
            payload.putString("projection_structure_id", vars.projection_selected_structure_id);
         } else if (vars.projection_selected_item != null && !vars.projection_selected_item.isEmpty()) {
            payload.put("projection_item", vars.projection_selected_item.save(this.entity.registryAccess()));
         }

         return TypeMoonWorldModVariables.PlayerVariables.normalizeProjectionPresetPayload(payload);
      }
   }

   private CompoundTag buildReinforcementPayload(int target, int mode, int level) {
      CompoundTag payload = new CompoundTag();
      payload.putInt("reinforcement_target", Mth.clamp(target, 0, 3));
      payload.putInt("reinforcement_mode", Mth.clamp(mode, 0, 3));
      payload.putInt("reinforcement_level", Mth.clamp(level, 1, 5));
      return payload;
   }

   private CompoundTag buildGravityPayload(int target, int mode) {
      CompoundTag payload = new CompoundTag();
      payload.putInt("gravity_target", Mth.clamp(target, 0, 1));
      payload.putInt("gravity_mode", Mth.clamp(mode, -2, 2));
      return payload;
   }

   private static boolean isProjectionPresetStage(Magical_attributes_Screen.PresetDialogState state) {
      return state != null && "projection".equals(state.entry.id) && "projection_source".equals(state.stageId);
   }

   private int getPresetDialogPanelWidth(Magical_attributes_Screen.PresetDialogState state) {
      return isProjectionPresetStage(state) ? 320 : 232;
   }

   private int getPresetDialogHeaderHeight(Magical_attributes_Screen.PresetDialogState state) {
      return isProjectionPresetStage(state) ? 36 : 28;
   }

   private int getPresetDialogFooterHeight() {
      return 22;
   }

   private int getPresetDialogPanelHeight(Magical_attributes_Screen.PresetDialogState state) {
      int header = this.getPresetDialogHeaderHeight(state);
      int footer = this.getPresetDialogFooterHeight();
      int desired = header + footer + state.options.size() * 18 + 10;
      int maxHeight = isProjectionPresetStage(state) ? 300 : 260;
      int allowed = Math.max(header + footer + 18 + 10, Math.min(maxHeight, this.height - 20));
      return Math.min(desired, allowed);
   }

   private int getPresetDialogContentScrollPixels(Magical_attributes_Screen.PresetDialogState state, int optionsAreaHeight) {
      int contentHeight = state.options.size() * 18;
      return Math.max(0, contentHeight - optionsAreaHeight);
   }

   private boolean scrollPresetDialog(double mouseX, double mouseY, double deltaY) {
      if (this.presetDialogState == null) {
         return false;
      } else {
         int panelWidth = this.getPresetDialogPanelWidth(this.presetDialogState);
         int panelHeight = this.getPresetDialogPanelHeight(this.presetDialogState);
         int panelX = (this.width - panelWidth) / 2;
         int panelY = (this.height - panelHeight) / 2;
         int headerHeight = this.getPresetDialogHeaderHeight(this.presetDialogState);
         int footerHeight = this.getPresetDialogFooterHeight();
         int optionsAreaX = panelX + 10;
         int optionsAreaY = panelY + headerHeight;
         int optionsAreaW = panelWidth - 20;
         int optionsAreaH = panelHeight - headerHeight - footerHeight;
         if (optionsAreaW > 0 && optionsAreaH > 0) {
            int scrollPixels = this.getPresetDialogContentScrollPixels(this.presetDialogState, optionsAreaH);
            if (scrollPixels <= 0) {
               return false;
            } else if (!(mouseX < optionsAreaX)
               && !(mouseX >= optionsAreaX + optionsAreaW)
               && !(mouseY < optionsAreaY)
               && !(mouseY >= optionsAreaY + optionsAreaH)) {
               float step = 18.0F / scrollPixels;
               this.presetDialogScrollOffs = Mth.clamp(this.presetDialogScrollOffs - (float)deltaY * step * 1.5F, 0.0F, 1.0F);
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   private void renderPresetDialog(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      if (this.presetDialogState != null) {
         int panelWidth = this.getPresetDialogPanelWidth(this.presetDialogState);
         int panelHeight = this.getPresetDialogPanelHeight(this.presetDialogState);
         int panelX = (this.width - panelWidth) / 2;
         int panelY = (this.height - panelHeight) / 2;
         int headerHeight = this.getPresetDialogHeaderHeight(this.presetDialogState);
         int footerHeight = this.getPresetDialogFooterHeight();
         int optionsAreaX = panelX + 10;
         int optionsAreaY = panelY + headerHeight;
         int optionsAreaW = panelWidth - 20;
         int optionsAreaH = panelHeight - headerHeight - footerHeight;
         int scrollPixels = this.getPresetDialogContentScrollPixels(this.presetDialogState, optionsAreaH);
         int startY = optionsAreaY - (int)(this.presetDialogScrollOffs * scrollPixels);
         guiGraphics.pose().pushPose();
         guiGraphics.pose().translate(0.0F, 0.0F, 500.0F);
         guiGraphics.fill(0, 0, this.width, this.height, -1342177280);
         guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, -535818216);
         guiGraphics.renderOutline(panelX, panelY, panelWidth, panelHeight, -1811878);
         guiGraphics.drawCenteredString(this.font, this.presetDialogState.title, panelX + panelWidth / 2, panelY + 8, -1);
         if (isProjectionPresetStage(this.presetDialogState)) {
            guiGraphics.drawCenteredString(
               this.font, Component.translatable("gui.typemoonworld.projection.title"), panelX + panelWidth / 2, panelY + 21, -5592406
            );
         }

         double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
         int scX = (int)(optionsAreaX * guiScale);
         int scY = (int)(Minecraft.getInstance().getWindow().getHeight() - (optionsAreaY + optionsAreaH) * guiScale);
         int scW = (int)(optionsAreaW * guiScale);
         int scH = (int)(optionsAreaH * guiScale);
         RenderSystem.enableScissor(scX, scY, scW, scH);

         for (int i = 0; i < this.presetDialogState.options.size(); i++) {
            int optionY = startY + i * 18;
            int optionW = optionsAreaW - (scrollPixels > 0 ? 6 : 0);
            int optionH = 16;
            if (optionY + optionH >= optionsAreaY && optionY <= optionsAreaY + optionsAreaH) {
               boolean hovered = mouseX >= optionsAreaX && mouseX < optionsAreaX + optionW && mouseY >= optionY && mouseY < optionY + optionH;
               guiGraphics.fill(optionsAreaX, optionY, optionsAreaX + optionW, optionY + optionH, hovered ? -1874190296 : 1882202148);
               guiGraphics.renderOutline(optionsAreaX, optionY, optionW, optionH, -1811878);
               Component label = this.presetDialogState.options.get(i).label;
               String clipped = this.clampTextToWidth(label.getString(), Math.max(20, optionW - 8));
               guiGraphics.drawString(this.font, clipped, optionsAreaX + 4, optionY + 4, -1, false);
            }
         }

         RenderSystem.disableScissor();
         if (scrollPixels > 0) {
            int scrollBarX = panelX + panelWidth - 14;
            int barHeight = Math.max(12, (int)((float)(optionsAreaH * optionsAreaH) / (this.presetDialogState.options.size() * 18)));
            int barTop = optionsAreaY + (int)(this.presetDialogScrollOffs * (optionsAreaH - barHeight));
            guiGraphics.fill(scrollBarX, optionsAreaY, scrollBarX + 4, optionsAreaY + optionsAreaH, Integer.MIN_VALUE);
            guiGraphics.fill(scrollBarX, barTop, scrollBarX + 4, barTop + barHeight, -1);
         }

         int cancelX = panelX + panelWidth - 62;
         int cancelY = panelY + panelHeight - 18;
         guiGraphics.fill(cancelX, cancelY, cancelX + 52, cancelY + 14, 1884311632);
         guiGraphics.renderOutline(cancelX, cancelY, 52, 14, -5592406);
         guiGraphics.drawCenteredString(this.font, Component.translatable("gui.cancel"), cancelX + 26, cancelY + 3, -1);
         guiGraphics.pose().popPose();
      }
   }

   private boolean handlePresetDialogClick(double mouseX, double mouseY, int button) {
      if (this.presetDialogState == null) {
         return false;
      } else {
         int panelWidth = this.getPresetDialogPanelWidth(this.presetDialogState);
         int panelHeight = this.getPresetDialogPanelHeight(this.presetDialogState);
         int panelX = (this.width - panelWidth) / 2;
         int panelY = (this.height - panelHeight) / 2;
         int headerHeight = this.getPresetDialogHeaderHeight(this.presetDialogState);
         int footerHeight = this.getPresetDialogFooterHeight();
         int optionsAreaX = panelX + 10;
         int optionsAreaY = panelY + headerHeight;
         int optionsAreaW = panelWidth - 20;
         int optionsAreaH = panelHeight - headerHeight - footerHeight;
         int scrollPixels = this.getPresetDialogContentScrollPixels(this.presetDialogState, optionsAreaH);
         int startY = optionsAreaY - (int)(this.presetDialogScrollOffs * scrollPixels);
         if (button != 0) {
            this.presetDialogState = null;
            this.presetDialogScrollOffs = 0.0F;
            return true;
         } else {
            for (int i = 0; i < this.presetDialogState.options.size(); i++) {
               int optionY = startY + i * 18;
               int optionW = optionsAreaW - (scrollPixels > 0 ? 6 : 0);
               int optionH = 16;
               if (optionY + optionH >= optionsAreaY
                  && optionY <= optionsAreaY + optionsAreaH
                  && mouseX >= optionsAreaX
                  && mouseX < optionsAreaX + optionW
                  && mouseY >= optionY
                  && mouseY < optionY + optionH) {
                  Magical_attributes_Screen.PresetOption option = this.presetDialogState.options.get(i);
                  CompoundTag merged = mergePayload(this.presetDialogState.draftPayload, option.payload);
                  if (!option.finish && option.nextStageId != null && !option.nextStageId.isEmpty()) {
                     Magical_attributes_Screen.MagicEntry entry = this.presetDialogState.entry;
                     int targetSlot = this.presetDialogState.targetSlot;
                     this.presetDialogState = null;
                     this.presetDialogScrollOffs = 0.0F;
                     this.openPresetDialogStage(entry, targetSlot, option.nextStageId, merged);
                     return true;
                  }

                  this.sendSetWheelSlot(this.presetDialogState.targetSlot, this.presetDialogState.entry, merged);
                  this.presetDialogState = null;
                  this.presetDialogScrollOffs = 0.0F;
                  return true;
               }
            }

            int cancelX = panelX + panelWidth - 62;
            int cancelY = panelY + panelHeight - 18;
            if (mouseX >= cancelX && mouseX < cancelX + 52 && mouseY >= cancelY && mouseY < cancelY + 14) {
               this.presetDialogState = null;
               this.presetDialogScrollOffs = 0.0F;
               return true;
            } else {
               if (mouseX < panelX || mouseX > panelX + panelWidth || mouseY < panelY || mouseY > panelY + panelHeight) {
                  this.presetDialogState = null;
                  this.presetDialogScrollOffs = 0.0F;
               }

               return true;
            }
         }
      }
   }

   private static boolean isKnowledgeOnlyMagic(String magicId) {
      return "jewel_magic_shoot".equals(magicId) || "jewel_magic_release".equals(magicId);
   }

   private void renderEntityInInventoryFollowsAngle(GuiGraphics guiGraphics, int x, int y, float angleXComponent, float angleYComponent, LivingEntity entity) {
      Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
      Quaternionf cameraOrientation = new Quaternionf().rotateX(angleYComponent * 20.0F * (float) (Math.PI / 180.0));
      pose.mul(cameraOrientation);
      float bodyRot = entity.yBodyRot;
      float yRot = entity.getYRot();
      float xRot = entity.getXRot();
      float yHeadRotO = entity.yHeadRotO;
      float yHeadRot = entity.yHeadRot;
      entity.yBodyRot = 180.0F + angleXComponent * 20.0F;
      entity.setYRot(180.0F + angleXComponent * 40.0F);
      entity.setXRot(-angleYComponent * 20.0F);
      entity.yHeadRot = entity.getYRot();
      entity.yHeadRotO = entity.getYRot();
      InventoryScreen.renderEntityInInventory(guiGraphics, x, y, 55.0F, new Vector3f(0.0F, 0.0F, 0.0F), pose, cameraOrientation, entity);
      entity.yBodyRot = bodyRot;
      entity.setYRot(yRot);
      entity.setXRot(xRot);
      entity.yHeadRotO = yHeadRotO;
      entity.yHeadRot = yHeadRot;
   }

   public Level getWorld() {
      return this.world;
   }

   static class MagicEntry {
      String id;
      String nameKey;
      String category;
      int color;
      String sourceType = "self";
      String crestEntryId = "";
      CompoundTag presetPayload = new CompoundTag();
      String crestSourceKind = "self";
      String originName = "";
      boolean active = true;
      int wheelSlotIndex = -1;

      MagicEntry(String id, String nameKey, String category, int color) {
         this.id = id;
         this.nameKey = nameKey;
         this.category = category;
         this.color = color;
      }

      Magical_attributes_Screen.MagicEntry copy() {
         Magical_attributes_Screen.MagicEntry copy = new Magical_attributes_Screen.MagicEntry(this.id, this.nameKey, this.category, this.color);
         copy.sourceType = this.sourceType;
         copy.crestEntryId = this.crestEntryId;
         copy.presetPayload = this.presetPayload == null ? new CompoundTag() : this.presetPayload.copy();
         copy.crestSourceKind = this.crestSourceKind;
         copy.originName = this.originName;
         copy.active = this.active;
         copy.wheelSlotIndex = this.wheelSlotIndex;
         return copy;
      }
   }

   static class PresetDialogState {
      final Magical_attributes_Screen.MagicEntry entry;
      final int targetSlot;
      final String stageId;
      final CompoundTag draftPayload;
      final Component title;
      final List<Magical_attributes_Screen.PresetOption> options;

      PresetDialogState(
         Magical_attributes_Screen.MagicEntry entry,
         int targetSlot,
         String stageId,
         CompoundTag draftPayload,
         Component title,
         List<Magical_attributes_Screen.PresetOption> options
      ) {
         this.entry = entry;
         this.targetSlot = targetSlot;
         this.stageId = stageId;
         this.draftPayload = draftPayload == null ? new CompoundTag() : draftPayload.copy();
         this.title = title;
         this.options = options;
      }
   }

   static class PresetOption {
      final Component label;
      final CompoundTag payload;
      final String nextStageId;
      final boolean finish;

      PresetOption(Component label, CompoundTag payload) {
         this(label, payload, null, true);
      }

      PresetOption(Component label, CompoundTag payload, String nextStageId, boolean finish) {
         this.label = label;
         this.payload = payload == null ? new CompoundTag() : payload.copy();
         this.nextStageId = nextStageId;
         this.finish = finish;
      }
   }
}
