package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.Magical_attributes_Button_Message;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Basic_information_back_player_self;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicalattributesMenu;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.HashMap;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PageChangeMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minecraft.util.Mth;

@SuppressWarnings({"null", "unused"})
public class Magical_attributes_Screen extends AbstractContainerScreen<MagicalattributesMenu> {
    private final static HashMap<String, Object> guistate = MagicalattributesMenu.guistate;
    private final Level world;
    private final int x, y, z;
    private final Player entity;
    private int pageMode;
    Button imagebutton_basic_attributes;
    Button imagebutton_magical_attributes;
    Button imagebutton_magical_properties;
    
    // Search and Filter
    // Removed SearchBox
    Button filterButton; // Simple button to cycle filters
    String filterCategory = "all"; // all, jewel, etc.
    List<MagicEntry> allMagics = new ArrayList<>();
    List<MagicEntry> filteredMagics = new ArrayList<>();
    
    // Scroll
    float scrollOffs;
    boolean scrolling;
    int startIndex;
    
    // Description Scroll
    float descScrollOffs;
    boolean descScrolling;
    
    // Tooltip State
    private long hoverStartTime = 0;
    private MagicEntry lastHoveredEntry = null;
    private boolean tooltipActive = false;
    
    // UI Constants
    private static final int LIST_X_OFFSET = 130;
    private static final int LIST_Y_OFFSET = 50;
    private static final int LIST_WIDTH = 180;
    private static final int LIST_HEIGHT = 140;
    
    // New Layout Constants
    private static final int PLAYER_VIEW_WIDTH = 80;
    private static final int LIST_VIEW_X = 85;
    private static final int LIST_VIEW_WIDTH = 100;
    private static final int DESC_VIEW_X = 190;
    private static final int DESC_VIEW_WIDTH = 160;
    private static final int VIEW_HEIGHT = 140;
    
    private static final int BTN_WIDTH = 80;
    private static final int BTN_HEIGHT = 20;
    private static final int GAP_X = 10;
    private static final int GAP_Y = 10;
    private static final int START_OFFSET_X = 5;
    private static final int START_OFFSET_Y = 10;
    private static final int VISIBLE_ROWS = 4; // Should match calculation
    private static final int COLUMNS = 2;
    private static final int FILTER_LABEL_X = 200;
    private static final int FILTER_LABEL_Y = 28;
    private static final int FILTER_BUTTON_Y = 25;
    private static final int FILTER_BUTTON_MIN_WIDTH = 44;
    private static final int FILTER_BUTTON_MAX_WIDTH = 72;
    
    // Magic Entry Class
    static class MagicEntry {
        String id;
        String nameKey;
        String category; // jewel, other
        int color;
        
        public MagicEntry(String id, String nameKey, String category, int color) {
            this.id = id;
            this.nameKey = nameKey;
            this.category = category;
            this.color = color;
        }
    }
    
    // Current Selection
    private MagicEntry selectedEntry = null;
    
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
        
        initMagicList();
    }
    
    private void initMagicList() {
        allMagics.clear();
        // Jewel Magics
        allMagics.add(new MagicEntry("jewel_magic_shoot", MagicConstants.KEY_MAGIC_JEWEL_SHOOT_SHORT, "jewel", 0xFF00FFFF));
        allMagics.add(new MagicEntry("jewel_magic_release", MagicConstants.KEY_MAGIC_JEWEL_RELEASE_SHORT, "jewel", 0xFFFF4000));
        
        allMagics.add(new MagicEntry("projection", MagicConstants.KEY_MAGIC_PROJECTION_SHORT, "unlimited_blade_works,basic", 0xFF00FFFF));
        allMagics.add(new MagicEntry("structural_analysis", MagicConstants.KEY_MAGIC_STRUCTURAL_ANALYSIS_SHORT, "unlimited_blade_works,basic", 0xFF00FFFF));
        allMagics.add(new MagicEntry("broken_phantasm", MagicConstants.KEY_MAGIC_BROKEN_PHANTASM_SHORT, "unlimited_blade_works,basic", 0xFFFF4000));
        allMagics.add(new MagicEntry("unlimited_blade_works", MagicConstants.KEY_MAGIC_UNLIMITED_BLADE_WORKS_SHORT, "unlimited_blade_works", 0xFFFF0000));
        allMagics.add(new MagicEntry("sword_barrel_full_open", MagicConstants.KEY_MAGIC_SWORD_BARREL_FULL_OPEN_SHORT, "unlimited_blade_works", 0xFFFF0000));

        // Reinforcement - Now part of "basic" category
        allMagics.add(new MagicEntry("reinforcement", MagicConstants.KEY_MAGIC_REINFORCEMENT_SHORT, "basic,reinforcement", 0xFF00AA00));
        // Other Magic
        allMagics.add(new MagicEntry("gravity_magic", MagicConstants.KEY_MAGIC_GRAVITY_SHORT, "other", 0xFF8A7CFF));
        
        updateFilteredMagics();
        
        // Auto-select first learned magic
        if (!filteredMagics.isEmpty()) {
            selectedEntry = filteredMagics.get(0);
        }
    }
    
    private void updateFilteredMagics() {
        // String query = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        String query = ""; // No search box
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

        if ("unlimited_blade_works".equals(filterCategory) && !vars.learned_magics.contains("unlimited_blade_works")) {
            filterCategory = "all";
        }
        
        filteredMagics = allMagics.stream().filter(entry -> {
            boolean matchCategory = "all".equals(filterCategory) || entry.category.contains(filterCategory);
            String name = Component.translatable(entry.nameKey).getString().toLowerCase();
            boolean matchSearch = query.isEmpty() || name.contains(query) || entry.id.contains(query);
            
            // Check if learned
            boolean learned = vars.learned_magics.contains(entry.id);
            
            // FORCE Check for Virtual/Category Magics (Ignore if parent ID is learned, require sub-skills)
            if ("reinforcement".equals(entry.id)) {
                learned = vars.learned_magics.contains("reinforcement")
                        || vars.learned_magics.contains("reinforcement_self")
                        || vars.learned_magics.contains("reinforcement_other")
                        || vars.learned_magics.contains("reinforcement_item");
            } else if ("jewel_magic_shoot".equals(entry.id)) {
                learned = vars.learned_magics.contains("jewel_magic_shoot");
            } else if ("jewel_magic_release".equals(entry.id)) {
                learned = vars.learned_magics.contains("jewel_magic_release");
            }
            
            return matchCategory && matchSearch && learned;
        }).collect(Collectors.toList());
        
        // Reset scroll if needed
        if (startIndex >= filteredMagics.size()) {
            startIndex = 0;
            scrollOffs = 0;
        }
    }

    private static final ResourceLocation texture = ResourceLocation.parse("typemoonworld:textures/screens/basic_information.png");

    // Helper method to get category label from ID
    private String getCategoryLabelKey(String category) {
        if ("jewel".equals(category)) return "gui.typemoonworld.category.jewel";
        if ("basic".equals(category)) return "gui.typemoonworld.category.basic";
        if ("unlimited_blade_works".equals(category)) return "gui.typemoonworld.category.ubw";
        if ("other".equals(category)) return "gui.typemoonworld.category.other";
        return "gui.typemoonworld.category.all";
    }

    private int getFilterButtonWidth() {
        String[] categoryKeys = {
                "gui.typemoonworld.category.all",
                "gui.typemoonworld.category.jewel",
                "gui.typemoonworld.category.basic",
                "gui.typemoonworld.category.ubw",
                "gui.typemoonworld.category.other"
        };
        int maxCategoryWidth = 0;
        for (String key : categoryKeys) {
            maxCategoryWidth = Math.max(maxCategoryWidth, this.font.width(Component.translatable(key)));
        }
        return Mth.clamp(maxCategoryWidth + 8, FILTER_BUTTON_MIN_WIDTH, FILTER_BUTTON_MAX_WIDTH);
    }

    private int getFilterButtonX() {
        int labelWidth = this.font.width(Component.translatable("gui.typemoonworld.category.label"));
        int desiredX = this.leftPos + FILTER_LABEL_X + labelWidth + 6;
        int buttonWidth = getFilterButtonWidth();
        int maxX = this.leftPos + this.imageWidth - buttonWidth - 6;
        return Math.min(desiredX, maxX);
    }

    private String clampTextToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return this.font.plainSubstrByWidth(text, maxWidth);
        }
        return this.font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ellipsis;
    }

    private Component getMagicListButtonText(MagicEntry entry, int availableWidth) {
        String shortLabel = Component.translatable(entry.nameKey).getString();
        if (shortLabel.equals(entry.nameKey)) {
            String fullKey = entry.nameKey.replace(".short", ".selected");
            shortLabel = Component.translatable(fullKey).getString();
            if (shortLabel.equals(fullKey)) {
                shortLabel = entry.id;
            }
        }
        return Component.literal(clampTextToWidth(shortLabel, availableWidth));
    }

    private Component getFilterButtonText(String category) {
        String label = Component.translatable(getCategoryLabelKey(category)).getString();
        int availableWidth = getFilterButtonWidth() - 8;
        return Component.literal(clampTextToWidth(label, availableWidth));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Hide player inventory when not on page 0
        if (this.pageMode != 0) {
            // ... (comments unchanged)
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (Basic_information_back_player_self.execute(entity) instanceof LivingEntity livingEntity) {
            // Reposition entity to align with Left Panel (Left Panel: 0-80)
            // Center X = leftPos + 60 (Center of 10-110 frame)
            // Bottom Y = topPos + 170 (approx feet pos)
            this.renderEntityInInventoryFollowsAngle(guiGraphics, this.leftPos + 60, this.topPos + 170, 0f
                    + (float) Math.atan((this.leftPos + 60 - mouseX) / 40.0), (float) Math.atan((this.topPos + 87 - mouseY) / 40.0), livingEntity);
        }
        
        if (this.pageMode == 1) {
            // Update filter button text based on current category
            if (filterButton != null) {
                filterButton.visible = true;
                filterButton.setMessage(getFilterButtonText(filterCategory));
            }
            
            // Render Magic List (Center-Left)
            renderMagicList(guiGraphics, mouseX, mouseY);
            
            // Tooltip Logic
            checkTooltipHover(mouseX, mouseY);
            if (tooltipActive && lastHoveredEntry != null) {
                renderScrollableTooltip(guiGraphics, mouseX, mouseY);
            }
        } else {
            if (filterButton != null) filterButton.visible = false;
        }
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    private void renderMagicList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        int listX = this.leftPos + LIST_X_OFFSET;
        int listY = this.topPos + LIST_Y_OFFSET;
        int listWidth = LIST_WIDTH;
        int listHeight = LIST_HEIGHT;
        
        // Render List Background
        // guiGraphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0x40000000); // Optional bg
        
        // Render Buttons (Grid)
        int btnW = BTN_WIDTH;
        int btnH = BTN_HEIGHT;
        int gapX = GAP_X;
        int gapY = GAP_Y;
        int startOffsetX = START_OFFSET_X;
        int startOffsetY = START_OFFSET_Y;
        
        // Visible items: 2 columns x 4 rows = 8
        int visibleItems = 8;
        
        for (int i = 0; i < visibleItems; i++) {
            int index = startIndex + i;
            if (index >= filteredMagics.size()) break;
            
            MagicEntry entry = filteredMagics.get(index);
            boolean isSelected = vars.selected_magics.contains(entry.id);
            if (!isSelected && "reinforcement".equals(entry.id)) {
                isSelected = vars.selected_magics.contains("reinforcement_self") || 
                             vars.selected_magics.contains("reinforcement_other") || 
                             vars.selected_magics.contains("reinforcement_item");
            }
            
            int col = i % 2;
            int row = i / 2;
            
            int btnX = listX + startOffsetX + col * (btnW + gapX);
            int btnY = listY + startOffsetY + row * (btnH + gapY);
            
            // Hover logic
            boolean hovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
            
            // Colors
            int borderColor = isSelected ? 0xFF00FF00 : (hovered ? entry.color : 0xFF00AAAA); 
            int fillColor = isSelected ? 0x80005500 : (hovered ? (entry.color & 0x00FFFFFF) | 0x80000000 : 0x80000000);
            int textColor = isSelected ? 0xFFFFFFFF : (hovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            
            guiGraphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, fillColor);
            guiGraphics.renderOutline(btnX, btnY, btnW, btnH, borderColor);
            
            Component msg = getMagicListButtonText(entry, btnW - 8);
            guiGraphics.drawCenteredString(this.font, msg, btnX + btnW / 2, btnY + (btnH - 8) / 2, textColor);
        }
        
        // Render Scrollbar
        int totalRows = (filteredMagics.size() + 1) / 2;
        int visibleRows = 4;
        
        if (totalRows > visibleRows) {
            int scrollBarX = listX + listWidth + 5; // Outside the list area
            int scrollBarY = listY;
            int scrollBarWidth = 6;
            int scrollBarHeight = listHeight;
            
            int barHeight = (int)((float)(visibleRows * scrollBarHeight) / totalRows);
            if (barHeight < 32) barHeight = 32;
            
            int barTop = scrollBarY + (int)(this.scrollOffs * (scrollBarHeight - barHeight));
            
            guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, scrollBarY + scrollBarHeight, 0x80000000); // Track
            guiGraphics.fill(scrollBarX, barTop, scrollBarX + scrollBarWidth, barTop + barHeight, 0xFF00FFFF); // Thumb
        }
    }

    private void renderScrollableTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (lastHoveredEntry == null) return;
        
        List<Component> lines = getDescriptionLines(lastHoveredEntry);
        
        // Tooltip Dimensions
        int tooltipWidth = 160;
        int lineHeight = 10;
        int padding = 5;
        int contentHeight = lines.size() * lineHeight;
        
        // Max height
        int maxTooltipHeight = 120;
        int tooltipHeight = Math.min(contentHeight + padding * 2, maxTooltipHeight);
        
        // Position near mouse, but clamped to screen
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 12;
        
        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = mouseX - tooltipWidth - 12;
        }
        if (tooltipY + tooltipHeight > this.height) {
            tooltipY = this.height - tooltipHeight - 5;
        }
        
        // Render Background
        // Use a high Z-offset to render on top
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400);
        
        guiGraphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xF0100010);
        guiGraphics.renderOutline(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 0xFF00AAAA); // Border
        
        // Scrollbar if needed
        int viewHeight = tooltipHeight - padding * 2;
        boolean canScroll = contentHeight > viewHeight;
        
        if (canScroll) {
            int scrollBarX = tooltipX + tooltipWidth - 6;
            int scrollBarY = tooltipY + padding;
            int scrollBarHeight = viewHeight;
            
            int barHeight = (int)((float)(viewHeight * scrollBarHeight) / contentHeight);
            if (barHeight < 10) barHeight = 10;
            
            int barTop = scrollBarY + (int)(this.descScrollOffs * (scrollBarHeight - barHeight));
            
            guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight, 0x80000000);
            guiGraphics.fill(scrollBarX, barTop, scrollBarX + 4, barTop + barHeight, 0xFFFFFFFF);
        }
        
        // Scissor Test for Text
        int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        // Adjust for GUI scale and window height (OpenGL coordinates start from bottom-left)
        int scX = (int)((tooltipX + padding) * scale);
        int scY = (int)((Minecraft.getInstance().getWindow().getHeight()) - (tooltipY + tooltipHeight - padding) * scale);
        int scW = (int)((tooltipWidth - 10) * scale);
        int scH = (int)(viewHeight * scale);
        
        RenderSystem.enableScissor(scX, scY, scW, scH);
                                   
        int startY = tooltipY + padding - (int)(this.descScrollOffs * (Math.max(0, contentHeight - viewHeight)));
        
        for (int i = 0; i < lines.size(); i++) {
            int y = startY + i * lineHeight;
            // Draw if visible (basic check)
            // if (y + lineHeight > tooltipY && y < tooltipY + tooltipHeight)
            guiGraphics.drawString(this.font, lines.get(i), tooltipX + padding, y, 0xFFFFFFFF, false);
        }
        
        RenderSystem.disableScissor();
        guiGraphics.pose().popPose();
    }

    private void checkTooltipHover(int mouseX, int mouseY) {
        int listX = this.leftPos + LIST_X_OFFSET;
        int listY = this.topPos + LIST_Y_OFFSET;
        
        if (mouseX >= listX && mouseX < listX + LIST_WIDTH && mouseY >= listY && mouseY < listY + LIST_HEIGHT) {
            int btnW = BTN_WIDTH;
            int btnH = BTN_HEIGHT;
            int gapX = GAP_X;
            int gapY = GAP_Y;
            int startOffsetX = START_OFFSET_X;
            int startOffsetY = START_OFFSET_Y;
            
            int visibleItems = 8;
            MagicEntry currentHover = null;
            
            for (int i = 0; i < visibleItems; i++) {
                int index = startIndex + i;
                if (index >= filteredMagics.size()) break;
                
                int col = i % 2;
                int row = i / 2;
                
                int btnX = listX + startOffsetX + col * (btnW + gapX);
                int btnY = listY + startOffsetY + row * (btnH + gapY);
                
                if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    currentHover = filteredMagics.get(index);
                    break;
                }
            }
            
            if (currentHover != null) {
                if (currentHover != lastHoveredEntry) {
                    lastHoveredEntry = currentHover;
                    hoverStartTime = System.currentTimeMillis();
                    descScrollOffs = 0;
                    tooltipActive = false;
                } else {
                    if (System.currentTimeMillis() - hoverStartTime > 500) {
                        tooltipActive = true;
                    }
                }
                return;
            }
        }
        
        lastHoveredEntry = null;
        tooltipActive = false;
        hoverStartTime = 0;
    }
    
    private List<Component> getDescriptionLines(MagicEntry entry) {
        List<Component> lines = new ArrayList<>();
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        // Title
        lines.add(Component.translatable(entry.nameKey).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFF00FFFF).withBold(true)));
        lines.add(Component.empty());
        
        String descKey = "magic.typemoonworld." + entry.id + ".desc";
        
        // Logic copied from previous renderMagicTooltip
        if (entry.id.startsWith("jewel_magic")) {
             if (net.minecraft.client.resources.language.I18n.exists(descKey)) {
                String translated = net.minecraft.client.resources.language.I18n.get(descKey);
                for (String line : translated.split("\n")) {
                    lines.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                lines.add(Component.empty());
             }

             int maxModes = entry.id.equals("jewel_magic_shoot") ? 6 : 4;
             
             for (int i = 0; i < maxModes; i++) {
                 String modeName = "";
                 String subSkillId = "";
                 int color = 0xFFFFFFFF;
                 
                 if (entry.id.equals("jewel_magic_shoot")) {
                     switch(i) {
                         case 0: modeName = "Ruby"; subSkillId = "ruby_throw"; color = 0xFFFF5555; break;
                         case 1: modeName = "Sapphire"; subSkillId = "sapphire_throw"; color = 0xFF5555FF; break;
                         case 2: modeName = "Emerald"; subSkillId = "emerald_use"; color = 0xFF55FF55; break;
                         case 3: modeName = "Topaz"; subSkillId = "topaz_throw"; color = 0xFFFFFF55; break;
                         case 4: modeName = "Cyan"; subSkillId = "cyan_throw"; color = 0xFF00FFFF; break;
                         case 5: modeName = "Random"; subSkillId = "jewel_magic_shoot"; color = 0xFFFFFFFF; break;
                     }
                 } else { // release
                     switch(i) {
                         case 0: modeName = "Sapphire"; subSkillId = "sapphire_winter_frost"; color = 0xFF5555FF; break;
                         case 1: modeName = "Emerald"; subSkillId = "emerald_winter_river"; color = 0xFF55FF55; break;
                         case 2: modeName = "Topaz"; subSkillId = "topaz_reinforcement"; color = 0xFFFFFF55; break;
                         case 3: modeName = "Cyan"; subSkillId = "cyan_wind"; color = 0xFF00FFFF; break;
                     }
                 }
                 
                 if (vars.learned_magics.contains(subSkillId)) {
                     String specificKey = "magic.typemoonworld." + entry.id + "." + modeName.toLowerCase() + ".desc";
                     if (net.minecraft.client.resources.language.I18n.exists(specificKey)) {
                         lines.add(Component.literal("- " + modeName + " -").withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(color)));
                         String translated = net.minecraft.client.resources.language.I18n.get(specificKey);
                         for (String line : translated.split("\n")) {
                             lines.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
                         }
                         lines.add(Component.empty());
                     }
                 }
             }
        } else {
            String baseDescKey = descKey;
            if (vars.player_magic_attributes_sword) {
                String swordKey = descKey + ".sword";
                if (net.minecraft.client.resources.language.I18n.exists(swordKey)) {
                    String translated = net.minecraft.client.resources.language.I18n.get(swordKey);
                    for (String line : translated.split("\n")) {
                        lines.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GOLD));
                    }
                    lines.add(Component.empty());
                }
            }
            if (net.minecraft.client.resources.language.I18n.exists(descKey)) {
                String translated = net.minecraft.client.resources.language.I18n.get(descKey);
                for (String line : translated.split("\n")) {
                    lines.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
            for (int i = 0; i < 10; i++) {
                String formKey = baseDescKey + "." + i;
                if (net.minecraft.client.resources.language.I18n.exists(formKey)) {
                    if (i > 0 || net.minecraft.client.resources.language.I18n.exists(descKey)) {
                        lines.add(Component.empty());
                    }
                    String translated = net.minecraft.client.resources.language.I18n.get(formKey);
                    for (String line : translated.split("\n")) {
                        lines.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                } else {
                    if (i > 0) break;
                }
            }
        }
        
        // Proficiency
        double proficiency = 0;
        boolean showProficiency = false;
        
        if ("structural_analysis".equals(entry.id)) {
            proficiency = vars.proficiency_structural_analysis;
            showProficiency = true;
        } else if ("projection".equals(entry.id)) {
            proficiency = vars.proficiency_projection;
            showProficiency = true;
        } else if ("unlimited_blade_works".equals(entry.id)) {
            proficiency = vars.proficiency_unlimited_blade_works;
            showProficiency = true;
        } else if ("sword_barrel_full_open".equals(entry.id)) {
            proficiency = vars.proficiency_sword_barrel_full_open;
            showProficiency = true;
        } else if ("jewel_magic_shoot".equals(entry.id)) {
            proficiency = vars.proficiency_jewel_magic_shoot;
            showProficiency = true;
        } else if ("jewel_magic_release".equals(entry.id)) {
            proficiency = vars.proficiency_jewel_magic_release;
            showProficiency = true;
        } else if ("gravity_magic".equals(entry.id)) {
            proficiency = vars.proficiency_gravity_magic;
            showProficiency = true;
        } else if ("reinforcement".equals(entry.id) || "reinforcement_self".equals(entry.id) || "reinforcement_other".equals(entry.id) || "reinforcement_item".equals(entry.id)) {
            proficiency = vars.proficiency_reinforcement;
            showProficiency = true;
        }
        
        if (showProficiency) {
            lines.add(Component.empty());
            String profText = String.format("%.1f%%", proficiency);
            lines.add(Component.translatable("gui.typemoonworld.proficiency", profText).withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        
        return lines;
    }

    // Removed old renderMagicList and renderMagicTooltip methods that returned MagicEntry or void
    // Now renderMagicList is void and draws buttons.
    // renderDescriptionPanel handles the text.

    // Mouse Input methods moved to end of file to match "Old Version" structure

    
    // Remove old renderMagicList signature if it conflicts (it was returning MagicEntry)
    // The previous code had: private MagicEntry renderMagicList(GuiGraphics guiGraphics, int mouseX, int mouseY)
    // We changed it to void.

    
    private void updateVisibility() {
        boolean visible = (this.pageMode == 1);
        if (this.filterButton != null) this.filterButton.visible = visible;
        
        if (imagebutton_magical_attributes != null) imagebutton_magical_attributes.visible = true;
        if (imagebutton_magical_properties != null) imagebutton_magical_properties.visible = true;
        if (imagebutton_basic_attributes != null) imagebutton_basic_attributes.visible = true;
    }

    // Removed local renderTechFrame, using GuiUtils
    
    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // --- Modern Custom Background ---
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        
        GuiUtils.renderBackground(guiGraphics, x, y, w, h);
        
        // 4. Character Panel Background (Left side)
        // guiGraphics.fill(x + 10, y + 35, x + 110, y + 150, 0x40000000); 
        // guiGraphics.renderOutline(x + 10, y + 35, 100, 115, 0x6000FFFF);
        GuiUtils.renderTechFrame(guiGraphics, x + 10, y + 35, 100, 150, 0xFF00AAAA, 0xFF00FFFF);
        
        // 5. Enhanced Magic Circuit Decorations (PCB Style) - REDESIGNED
        // REMOVED all circuits and connecting lines as requested
        
        // (Empty)

        // 6. Inventory Background for Page 0
        if (pageMode == 0) {
            // Inventory Panel Background
            int invX = x + 145;
            int invY = y + 80;
            int invW = 172;
            int invH = 85;
            
            // guiGraphics.fill(invX, invY, invX + invW, invY + invH, 0x40000000); // Semi-transparent bg
            // guiGraphics.renderOutline(invX, invY, invW, invH, 0xFF00AAAA); // Border
            GuiUtils.renderTechFrame(guiGraphics, invX, invY, invW, invH, 0xFF00AAAA, 0xFF0088FF);
            
            // Mystic Eyes Slot Background
            int eyeSlotX = x + 195; // 200 - 5 (padding for 28x28 box around 16x16 slot)
            int eyeSlotY = y + 35; // 40 - 5
            int eyeSlotSize = 26;
            
            // guiGraphics.fill(eyeSlotX, eyeSlotY, eyeSlotX + eyeSlotSize, eyeSlotY + eyeSlotSize, 0x60000000);
            // guiGraphics.renderOutline(eyeSlotX, eyeSlotY, eyeSlotSize, eyeSlotSize, 0xFFFF00FF); // Purple/Pink highlight
            GuiUtils.renderTechFrame(guiGraphics, eyeSlotX, eyeSlotY, eyeSlotSize, eyeSlotSize, 0xFFFF00FF, 0xFFFF0088);
            
            // Draw connection line
            guiGraphics.fill(x + 110 + 2, y + 60, eyeSlotX - 2, y + 61, 0xFF00FFFF);
            
            // Draw Inventory Slot Grids (Visual Guide)
            // Inventory (3x9) at 150, 85
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 9; ++j) {
                    int slotX = x + 150 + j * 18 - 1;
                    int slotY = y + 85 + i * 18 - 1;
                    guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0x20FFFFFF);
                }
            }
            
            // Hotbar (1x9) at 150, 143
            for (int k = 0; k < 9; ++k) {
                int slotX = x + 150 + k * 18 - 1;
                int slotY = y + 143 - 1;
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0x20FFFFFF);
            }
        }

        RenderSystem.disableBlend();
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            if (this.minecraft != null) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.closeContainer();
                }
            }
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title (Removed text)
        // guiGraphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, 8, 0xFF00FFFF);

        // Content
        if (pageMode == 0) {
            // No text
            // Add symbol or icon for Mystic Eyes?
            // Let's add a small label "EYES" above the slot if it's not too cluttered.
            // But user asked to clear text. So maybe just keep it graphical.
            // Maybe draw a small eye symbol?
            // For now, keep it clean as requested previously.
        } else {
            guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.screen.learned_magic"), 125, 28, 0xFF00E0E0, false);
            guiGraphics.drawString(this.font, Component.translatable("gui.typemoonworld.category.label"), FILTER_LABEL_X, FILTER_LABEL_Y, 0xFFAAAAAA, false);
        }
    }

    // Custom Neon Button Class Removed - Use NeonButton.java
    
    // Helper to check if a category is unlocked (has at least one learned magic)
    private boolean isCategoryUnlocked(String category, TypeMoonWorldModVariables.PlayerVariables vars) {
        if ("all".equals(category)) return true;
        if ("unlimited_blade_works".equals(category)) {
            return vars.learned_magics.contains("unlimited_blade_works");
        }
        
        return allMagics.stream().anyMatch(entry -> {
            boolean matchCategory = entry.category.contains(category);
            if (!matchCategory) return false;
            
            boolean learned = vars.learned_magics.contains(entry.id);
            if (!learned && "reinforcement".equals(entry.id)) {
                learned = vars.learned_magics.contains("reinforcement")
                        || vars.learned_magics.contains("reinforcement_self")
                        || vars.learned_magics.contains("reinforcement_other")
                        || vars.learned_magics.contains("reinforcement_item");
            }
            return learned;
        });
    }

    private String nextCategoryRaw(String current) {
        // Order: all -> jewel -> basic -> unlimited_blade_works -> other -> all
        if ("all".equals(current)) return "jewel";
        if ("jewel".equals(current)) return "basic";
        if ("basic".equals(current)) return "unlimited_blade_works";
        if ("unlimited_blade_works".equals(current)) return "other";
        return "all";
    }

    private String getNextCategory(String current, TypeMoonWorldModVariables.PlayerVariables vars) {
        String next = nextCategoryRaw(current);

        // If next is not unlocked (and not "all"), skip it
        int safety = 0;
        while (!"all".equals(next) && !isCategoryUnlocked(next, vars) && safety < 6) {
            next = nextCategoryRaw(next);
            safety++;
        }
        return next;
    }

    @Override
    public void init() {
        super.init();
        
        int tabY = this.topPos + 5;
        int tabWidth = 80;
        int tabHeight = 16;
        int tabX = this.leftPos + this.imageWidth - (tabWidth * 3) - 10;
        
        // --- Search Box Removed ---

        // --- Filter Button ---
        // Positioned where search box was roughly, aligned with "分类:" label
        // "分类:" text will be drawn at x=200, y=28 (aligned with title)
        // Button should be after that.
        // Let's place label at x=200. Width ~25. Button at x=230.
        int filterButtonWidth = getFilterButtonWidth();
        this.filterButton = new NeonButton(getFilterButtonX(), this.topPos + FILTER_BUTTON_Y, filterButtonWidth, 14, getFilterButtonText("all"), e -> {
            TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            
            // Cycle filters logic with unlock check
            // Order: all -> jewel -> basic -> unlimited_blade_works -> all
            // Skip if not unlocked
            
            String nextCategory = getNextCategory(filterCategory, vars);
            filterCategory = nextCategory;
            
            e.setMessage(getFilterButtonText(filterCategory));
            updateFilteredMagics();
            // Reset scroll on filter change
            startIndex = 0;
            scrollOffs = 0;
        });

        // Add filter button
        this.addRenderableWidget(filterButton);
        
        // --- Magic Buttons are now dynamic in render() ---
        
        // Tab Buttons
        imagebutton_basic_attributes = new NeonButton(tabX, tabY, tabWidth, tabHeight, Component.translatable("gui.typemoonworld.tab.basic_attributes"), e -> {
            PacketDistributor.sendToServer(new Magical_attributes_Button_Message(0, x, y, z));
            Magical_attributes_Button_Message.handleButtonAction(entity, 0, x, y, z);
        });
        this.addRenderableWidget(imagebutton_basic_attributes);
        
        imagebutton_magical_attributes = new NeonButton(tabX + tabWidth + 2, tabY, tabWidth, tabHeight, Component.translatable("gui.typemoonworld.tab.body_modification"), e -> {
            this.pageMode = 0;
            this.menu.setPage(0);
            PacketDistributor.sendToServer(new PageChangeMessage(0));
            updateVisibility();
        });
        this.addRenderableWidget(imagebutton_magical_attributes);
        
        imagebutton_magical_properties = new NeonButton(tabX + (tabWidth + 2) * 2, tabY, tabWidth, tabHeight, Component.translatable("gui.typemoonworld.tab.magic_knowledge"), e -> {
            this.pageMode = 1;
            this.menu.setPage(1);
            PacketDistributor.sendToServer(new PageChangeMessage(1));
            updateVisibility();
        });
        this.addRenderableWidget(imagebutton_magical_properties);
        
        updateVisibility();
    }

    // Removed duplicate updateVisibility() method at the end of the file.
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.pageMode == 1) {
            // Handle Scrollbar click
            int listX = this.leftPos + 130;
            int listY = this.topPos + 50;
            int listWidth = 180;
            int listHeight = 140;
            int scrollBarX = listX + listWidth + 5;
            int scrollBarWidth = 6;
            
            if (mouseX >= scrollBarX && mouseX < scrollBarX + scrollBarWidth &&
                mouseY >= listY && mouseY < listY + listHeight) {
                this.scrolling = true;
                return true;
            }
            
            // Handle Magic Button Clicks
            // 2 columns, 4 rows visible = 8 items
            for (int i = 0; i < 8; i++) {
                int index = startIndex + i;
                if (index >= filteredMagics.size()) break;
                
                int col = i % 2;
                int row = i / 2;
                
                // Match Render Logic Exactly
                int btnW = BTN_WIDTH;
                int btnH = BTN_HEIGHT;
                int gapX = GAP_X;
                int gapY = GAP_Y;
                int startOffsetX = START_OFFSET_X;
                int startOffsetY = START_OFFSET_Y;
                
                int btnX = listX + startOffsetX + col * (btnW + gapX); 
                int btnY = listY + startOffsetY + row * (btnH + gapY); 
                
                if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    MagicEntry entry = filteredMagics.get(index);
                    TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                    
                    String magicToToggle = entry.id;
                    
                    boolean isSelected = vars.selected_magics.contains(magicToToggle);
                    PacketDistributor.sendToServer(new SelectMagicMessage(magicToToggle, !isSelected));
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.scrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling && this.pageMode == 1) {
            int listHeight = 140;
            int scrollBarHeight = listHeight;
            
            // Calculate new scroll offset
            float d = (float)dragY / (float)scrollBarHeight;
            this.scrollOffs = Mth.clamp(this.scrollOffs + d, 0.0F, 1.0F);
            
            // Update startIndex
            int totalRows = (filteredMagics.size() + 1) / 2;
            int visibleRows = 4;
            if (totalRows > visibleRows) {
                int maxStartRow = totalRows - visibleRows;
                int startRow = (int)(this.scrollOffs * maxStartRow);
                this.startIndex = startRow * 2;
            } else {
                this.startIndex = 0;
            }
            
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.pageMode == 1) {
            // Check Tooltip Scroll
            if (tooltipActive && lastHoveredEntry != null) {
                 List<Component> lines = getDescriptionLines(lastHoveredEntry);
                 int contentHeight = lines.size() * 10;
                 int viewHeight = 110; 
                 
                 if (contentHeight > viewHeight) {
                     float step = 10.0f / (contentHeight - viewHeight);
                     this.descScrollOffs = Mth.clamp(this.descScrollOffs - (float)deltaY * step * 3, 0.0F, 1.0F);
                     return true;
                 }
            }

            // Check if mouse is over the list area
            int listX = this.leftPos + LIST_X_OFFSET;
            int listY = this.topPos + LIST_Y_OFFSET;
            int listW = LIST_WIDTH;
            int listH = LIST_HEIGHT;
            
            if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                 int totalRows = (filteredMagics.size() + 1) / 2;
                 int visibleRows = 4;
                 if (totalRows > visibleRows) {
                     float scrollStep = 1.0f / (totalRows - visibleRows);
                     // Invert direction: scrolling down (negative delta) should increase offset
                     this.scrollOffs = Mth.clamp(this.scrollOffs - (float)deltaY * scrollStep, 0.0F, 1.0F);
                     
                     int maxStartRow = totalRows - visibleRows;
                     int startRow = Math.round(this.scrollOffs * maxStartRow); 
                     this.startIndex = startRow * 2;
                     return true;
                 }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void updateButtonLabels() {
        // No longer used, handled in render
    }

    private void renderEntityInInventoryFollowsAngle(GuiGraphics guiGraphics, int x, int y,
                                                     float angleXComponent, float angleYComponent, LivingEntity entity) {
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraOrientation = new Quaternionf().rotateX(angleYComponent * 20 * ((float) Math.PI / 180F));
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
        InventoryScreen.renderEntityInInventory(guiGraphics, x, y, 55, new Vector3f(0, 0, 0), pose, cameraOrientation, entity);
        entity.yBodyRot = f2;
        entity.setYRot(f3);
        entity.setXRot(f4);
        entity.yHeadRotO = f5;
        entity.yHeadRot = f6;
    }

    public Level getWorld() {
        return world;
    }
}
