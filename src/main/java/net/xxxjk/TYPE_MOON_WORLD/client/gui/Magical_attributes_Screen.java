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
    
    // UI Constants
    private static final int LIST_X_OFFSET = 130;
    private static final int LIST_Y_OFFSET = 50;
    private static final int LIST_WIDTH = 180;
    private static final int LIST_HEIGHT = 140;
    private static final int BTN_WIDTH = 80;
    private static final int BTN_HEIGHT = 20;
    private static final int GAP_X = 10;
    private static final int GAP_Y = 10;
    private static final int START_OFFSET_X = 5;
    private static final int START_OFFSET_Y = 10;
    private static final int VISIBLE_ROWS = 4; // Should match calculation
    private static final int COLUMNS = 2;
    
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
        allMagics.add(new MagicEntry("ruby_throw", MagicConstants.KEY_MAGIC_RUBY_THROW_SHORT, "jewel", 0xFFFF0000));
        allMagics.add(new MagicEntry("sapphire_throw", MagicConstants.KEY_MAGIC_SAPPHIRE_THROW_SHORT, "jewel", 0xFF0088FF));
        allMagics.add(new MagicEntry("emerald_use", MagicConstants.KEY_MAGIC_EMERALD_USE_SHORT, "jewel", 0xFF00FF00));
        allMagics.add(new MagicEntry("topaz_throw", MagicConstants.KEY_MAGIC_TOPAZ_THROW_SHORT, "jewel", 0xFFFFFF00));
        allMagics.add(new MagicEntry("ruby_flame_sword", MagicConstants.KEY_MAGIC_RUBY_FLAME_SWORD_SHORT, "jewel", 0xFFFF0000));
        allMagics.add(new MagicEntry("sapphire_winter_frost", MagicConstants.KEY_MAGIC_SAPPHIRE_WINTER_FROST_SHORT, "jewel", 0xFF0088FF));
        allMagics.add(new MagicEntry("emerald_winter_river", MagicConstants.KEY_MAGIC_EMERALD_WINTER_RIVER_SHORT, "jewel", 0xFF00FF00));
        allMagics.add(new MagicEntry("topaz_reinforcement", MagicConstants.KEY_MAGIC_TOPAZ_REINFORCEMENT_SHORT, "jewel", 0xFFFFFF00));
        allMagics.add(new MagicEntry("projection", MagicConstants.KEY_MAGIC_PROJECTION_SHORT, "unlimited_blade_works,basic", 0xFF00FFFF));
        allMagics.add(new MagicEntry("structural_analysis", MagicConstants.KEY_MAGIC_STRUCTURAL_ANALYSIS_SHORT, "unlimited_blade_works,basic", 0xFF00FFFF));
        allMagics.add(new MagicEntry("broken_phantasm", MagicConstants.KEY_MAGIC_BROKEN_PHANTASM_SHORT, "unlimited_blade_works,basic", 0xFFFF4000));
        allMagics.add(new MagicEntry("unlimited_blade_works", MagicConstants.KEY_MAGIC_UNLIMITED_BLADE_WORKS_SHORT, "unlimited_blade_works", 0xFFFF0000));
        allMagics.add(new MagicEntry("sword_barrel_full_open", MagicConstants.KEY_MAGIC_SWORD_BARREL_FULL_OPEN_SHORT, "unlimited_blade_works", 0xFFFF0000));
        
        updateFilteredMagics();
    }
    
    private void updateFilteredMagics() {
        // String query = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        String query = ""; // No search box
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        filteredMagics = allMagics.stream().filter(entry -> {
            boolean matchCategory = "all".equals(filterCategory) || entry.category.contains(filterCategory);
            String name = Component.translatable(entry.nameKey).getString().toLowerCase();
            boolean matchSearch = query.isEmpty() || name.contains(query) || entry.id.contains(query);
            
            // Check if learned
            boolean learned = vars.learned_magics.contains(entry.id);
            
            return matchCategory && matchSearch && learned;
        }).collect(Collectors.toList());
        
        // Reset scroll if needed
        if (startIndex >= filteredMagics.size()) {
            startIndex = 0;
            scrollOffs = 0;
        }
    }

    private static final ResourceLocation texture = ResourceLocation.parse("typemoonworld:textures/screens/basic_information.png");

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Hide player inventory when not on page 0
        if (this.pageMode != 0) {
            // We can't easily "hide" slots client-side visually without modifying the container
            // BUT, the container ALREADY hides slots via isActive().
            // So we just need to make sure we don't render their backgrounds/tooltips if for some reason they show up.
            // Actually, AbstractContainerScreen renders slots based on their visibility.
            // If container.slots.isActive() is false, they shouldn't be interactable.
            // However, AbstractContainerScreen.render() calls renderBg -> super.render -> renderSlots.
            // Slots that are not 'isActive' are usually moved off-screen by the container logic or just not processed for clicks.
            // But visually, standard slots might still be rendered if their x/y are within screen.
            // Wait, Slot.isActive() controls if it appears in the container's list for interaction? 
            // No, Slot.isActive() is a NeoForge/Forge extension or vanilla?
            // Vanilla Slot has 'isActive()'. AbstractContainerMenu.slots contains ALL slots.
            // AbstractContainerScreen.render() iterates all slots and calls renderSlot().
            // AbstractContainerScreen.renderSlot() checks 'if (slot.isActive())'.
            // So if our Menu logic is correct, they won't render.
            // Let's verify Menu logic.
        }
        
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (Basic_information_back_player_self.execute(entity) instanceof LivingEntity livingEntity) {
            // Reposition entity to align with Left Panel (Left Panel: x+10 to x+110)
            // Center X = leftPos + 10 + 50 = leftPos + 60
            // Bottom Y = topPos + 185 - 15 = topPos + 170 (approx feet pos)
            this.renderEntityInInventoryFollowsAngle(guiGraphics, this.leftPos + 60, this.topPos + 170, 0f
                    + (float) Math.atan((this.leftPos + 60 - mouseX) / 40.0), (float) Math.atan((this.topPos + 87 - mouseY) / 40.0), livingEntity);
        }
        
        if (this.pageMode == 1) {
            // Render Magic List
            MagicEntry hoveredEntry = renderMagicList(guiGraphics, mouseX, mouseY);
            if (hoveredEntry != null) {
                renderMagicTooltip(guiGraphics, hoveredEntry, mouseX, mouseY);
            }
        }
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    private MagicEntry renderMagicList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        MagicEntry hoveredEntry = null;
        
        int listX = this.leftPos + 130;
        int listY = this.topPos + 50;
        int listWidth = 180;
        int listHeight = 140;
        
        // Render List Background with a border
        guiGraphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0x40000000); // Semi-transparent bg
        guiGraphics.renderOutline(listX, listY, listWidth, listHeight, 0xFF00AAAA); // Cyan/Blue border
        
        // Render Buttons
        for (int i = 0; i < 8; i++) {
            int index = startIndex + i;
            if (index >= filteredMagics.size()) break;
            
            MagicEntry entry = filteredMagics.get(index);
            boolean isSelected = vars.selected_magics.contains(entry.id);
            
            int col = i % 2;
            int row = i / 2;
            
            // Adjust positions to be inside the list box with padding
            int btnW = 80;
            int btnH = 20;
            int gapX = 10;
            int gapY = 10;
            int startOffsetX = 5;
            int startOffsetY = 10;
            
            int btnX = listX + startOffsetX + col * (btnW + gapX); 
            
            // Let's sync Render and Click logic.
            int btnY = listY + startOffsetY + row * (btnH + gapY); 
            
            // Custom Button Render Logic
            boolean hovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
            if (hovered) {
                hoveredEntry = entry;
            }
            
            // Selection logic: Green if selected, else default
            int borderColor = isSelected ? 0xFF00FF00 : (hovered ? entry.color : 0xFF00AAAA);
            int fillColor = isSelected ? 0x80004000 : (hovered ? (entry.color & 0x00FFFFFF) | 0x80000000 : 0x80000000);
            int textColor = isSelected ? 0xFFFFFFFF : (hovered ? 0xFFFFFFFF : 0xFFAAAAAA);
            
            guiGraphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, fillColor);
            guiGraphics.renderOutline(btnX, btnY, btnW, btnH, borderColor);
            
            Component msg = Component.translatable(isSelected ? entry.nameKey.replace(".short", ".selected") : entry.nameKey);
            // Fallback if selected key not found or same as short
            if (isSelected && msg.getString().equals(entry.nameKey)) {
                 msg = Component.translatable(entry.nameKey); 
            }
            
            // Handle case where short key doesn't have .short suffix (though they should)
            if (!entry.nameKey.endsWith(".short") && isSelected) {
                // If we don't follow the convention, try appending .selected or use a map
                // For projection, we defined KEY_MAGIC_PROJECTION_SHORT = ...short
                // So it should be handled by replace above.
            }
            
            guiGraphics.drawCenteredString(this.font, msg, btnX + btnW / 2, btnY + (btnH - 8) / 2, textColor);
        }
        
        // Render Scrollbar
        int totalRows = (filteredMagics.size() + 1) / 2;
        int visibleRows = 4;
        if (totalRows > visibleRows) {
            int scrollBarX = listX + listWidth + 2; // Right of the list box
            int scrollBarY = listY;
            int scrollBarWidth = 6;
            int scrollBarHeight = listHeight;
            
            int barHeight = (int)((float)(visibleRows * scrollBarHeight) / totalRows);
            if (barHeight < 32) barHeight = 32;
            
            int barTop = scrollBarY + (int)(this.scrollOffs * (scrollBarHeight - barHeight));
            
            guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, scrollBarY + scrollBarHeight, 0x80000000); // Track
            guiGraphics.fill(scrollBarX, barTop, scrollBarX + scrollBarWidth, barTop + barHeight, 0xFF00FFFF); // Thumb
        }
        
        return hoveredEntry;
    }
    
    private void renderMagicTooltip(GuiGraphics guiGraphics, MagicEntry entry, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        // Title (Magic Name)
        tooltip.add(Component.translatable(entry.nameKey).withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(0xFF00FFFF).withBold(true)));
        
        // Main Description
        String descKey = "magic.typemoonworld." + entry.id + ".desc";
        String baseDescKey = descKey;
        
        // Handle Sword Attribute overrides
        if (vars.player_magic_attributes_sword) {
            String swordKey = descKey + ".sword";
            if (net.minecraft.client.resources.language.I18n.exists(swordKey)) {
                // Add Sword Text as Introduction (Flavor)
                String translated = net.minecraft.client.resources.language.I18n.get(swordKey);
                for (String line : translated.split("\n")) {
                    tooltip.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GOLD));
                }
                // Add a spacer
                tooltip.add(Component.empty());
            }
        }

        // Always render the main functional description
        if (net.minecraft.client.resources.language.I18n.exists(descKey)) {
            String translated = net.minecraft.client.resources.language.I18n.get(descKey);
            for (String line : translated.split("\n")) {
                tooltip.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
        
        // Proficiency Display
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
        } else if ("jewel".equals(entry.category)) {
            proficiency = vars.proficiency_jewel_magic;
            showProficiency = true;
        }
        
        if (showProficiency) {
            tooltip.add(Component.empty());
            String profText = String.format("%.1f%%", proficiency);
            tooltip.add(Component.translatable("gui.typemoonworld.proficiency", profText).withStyle(net.minecraft.ChatFormatting.GOLD));
        }

        // Multi-form Descriptions (desc.0, desc.1, etc.)
        for (int i = 0; i < 10; i++) { // Limit to 10 forms to avoid infinite loop
            String formKey = baseDescKey + "." + i;
            if (net.minecraft.client.resources.language.I18n.exists(formKey)) {
                if (i > 0 || net.minecraft.client.resources.language.I18n.exists(descKey)) {
                    tooltip.add(Component.empty()); // Spacer
                }
                String translated = net.minecraft.client.resources.language.I18n.get(formKey);
                for (String line : translated.split("\n")) {
                    tooltip.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            } else {
                // If 0 exists but 1 doesn't, stop. 
                // But maybe they skipped numbers? Unlikely. Assume sequential.
                if (i == 0 && !net.minecraft.client.resources.language.I18n.exists(baseDescKey)) {
                    // If no main desc and no form 0, maybe just stop?
                } else if (i > 0) {
                    break;
                }
            }
        }
        
        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }
    
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
            guiGraphics.drawString(this.font, Component.translatable(MagicConstants.GUI_LEARNED_MAGIC), 125, 28, 0xFF00E0E0, false);
            // Label for filter
            // Button is at 230, 25. Text should be left of it.
            // "分类:"
            guiGraphics.drawString(this.font, "分类:", 200, 28, 0xFFAAAAAA, false);
        }
    }

    // Custom Neon Button Class Removed - Use NeonButton.java
    
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
        this.filterButton = new NeonButton(this.leftPos + 230, this.topPos + 25, 40, 14, Component.literal("全部"), e -> {
            // Cycle filters: all -> jewel -> basic -> unlimited_blade_works -> all
            if ("all".equals(filterCategory)) {
                filterCategory = "jewel";
                e.setMessage(Component.literal("宝石"));
            } else if ("jewel".equals(filterCategory)) {
                filterCategory = "basic";
                e.setMessage(Component.literal("基础"));
            } else if ("basic".equals(filterCategory)) {
                filterCategory = "unlimited_blade_works";
                e.setMessage(Component.literal("无限剑制"));
            } else {
                filterCategory = "all";
                e.setMessage(Component.literal("全部"));
            }
            updateFilteredMagics();
            // Reset scroll on filter change
            this.startIndex = 0;
            this.scrollOffs = 0;
        });
        this.filterButton.visible = false;
        this.addRenderableWidget(this.filterButton);
        
        // --- Magic Buttons are now dynamic in render() ---
        
        // Tab Buttons
        imagebutton_basic_attributes = new NeonButton(tabX, tabY, tabWidth, tabHeight, Component.literal("基础属性"), e -> {
            PacketDistributor.sendToServer(new Magical_attributes_Button_Message(0, x, y, z));
            Magical_attributes_Button_Message.handleButtonAction(entity, 0, x, y, z);
        });
        this.addRenderableWidget(imagebutton_basic_attributes);
        
        imagebutton_magical_attributes = new NeonButton(tabX + tabWidth + 2, tabY, tabWidth, tabHeight, Component.literal("身体改造"), e -> {
            this.pageMode = 0;
            this.menu.setPage(0);
            PacketDistributor.sendToServer(new PageChangeMessage(0));
            updateVisibility();
        });
        this.addRenderableWidget(imagebutton_magical_attributes);
        
        imagebutton_magical_properties = new NeonButton(tabX + (tabWidth + 2) * 2, tabY, tabWidth, tabHeight, Component.literal("魔术知识"), e -> {
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
                    boolean isSelected = vars.selected_magics.contains(entry.id);
                    PacketDistributor.sendToServer(new SelectMagicMessage(entry.id, !isSelected));
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
