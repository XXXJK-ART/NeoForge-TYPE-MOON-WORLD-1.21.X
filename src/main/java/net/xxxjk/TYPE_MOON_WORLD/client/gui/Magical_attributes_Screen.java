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
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minecraft.util.Mth;

@SuppressWarnings("null")
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
        
        updateFilteredMagics();
    }
    
    private void updateFilteredMagics() {
        // String query = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        String query = ""; // No search box
        filteredMagics = allMagics.stream().filter(entry -> {
            boolean matchCategory = "all".equals(filterCategory) || entry.category.equals(filterCategory);
            String name = Component.translatable(entry.nameKey).getString().toLowerCase();
            boolean matchSearch = query.isEmpty() || name.contains(query) || entry.id.contains(query);
            return matchCategory && matchSearch;
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
            renderMagicList(guiGraphics, mouseX, mouseY);
        }
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    private void renderMagicList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
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
            // Fix vertical alignment: shift up by one slot
            // Originally: listY + startOffsetY + row * (btnH + gapY)
            // If we shift up by one full slot (height + gap), we subtract (btnH + gapY)
            // But user said "misaligned by exactly one", maybe it means the visual list starts too low?
            // Or maybe the hitboxes are off?
            // "magical list is vertically misaligned by exactly one, clicking the previous icon selects the next one"
            // This implies the RENDER is correct but the CLICK detection is offset? 
            // Or Render is offset and click is correct?
            // If "clicking top one selects bottom one", then Click Area > Render Area.
            // If "clicking previous (top) selects next (bottom)", then Click Area is LOWER than Render Area.
            // Wait, "clicking the previous icon selects the next one" -> Click Item N triggers Item N+1?
            // No, "clicking the UPPER icon selects the LOWER icon" means click area is shifted UP relative to render?
            // Or Render is shifted DOWN?
            
            // Let's look at click logic vs render logic.
            // Render: btnY = listY + startOffsetY + row * (btnH + gapY)
            // Click: btnY = listY + row * 25 (where 25 is btnH + gapY approx)
            
            // In init(), GAP_Y = 10, BTN_H = 20. Total 30 per row.
            // In Render: row * (20 + 10) = row * 30.
            // In Click (old code): row * 25. -> This is the mismatch!
            
            // Let's sync Render and Click logic.
            int btnY = listY + startOffsetY + row * (btnH + gapY); 
            
            // Custom Button Render Logic
            boolean hovered = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
            
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
    }
    
    private void updateVisibility() {
        boolean visible = (this.pageMode == 1);
        if (this.filterButton != null) this.filterButton.visible = visible;
        
        if (imagebutton_magical_attributes != null) imagebutton_magical_attributes.visible = true;
        if (imagebutton_magical_properties != null) imagebutton_magical_properties.visible = true;
        if (imagebutton_basic_attributes != null) imagebutton_basic_attributes.visible = true;
    }

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
        
        // 1. Main Background (Gradient: Dark Blue/Purple to Black)
        guiGraphics.fillGradient(x, y, x + w, y + h, 0xF0101020, 0xF0050510);
        
        // 2. Cyber/Magic Border (Cyan/Neon Blue)
        int borderColor = 0xFF00FFFF;
        int borderWidth = 1;
        
        guiGraphics.fill(x - borderWidth, y - borderWidth, x + w + borderWidth, y, borderColor); // Top
        guiGraphics.fill(x - borderWidth, y + h, x + w + borderWidth, y + h + borderWidth, borderColor); // Bottom
        guiGraphics.fill(x - borderWidth, y, x, y + h, borderColor); // Left
        guiGraphics.fill(x + w, y, x + w + borderWidth, y + h, borderColor); // Right
        
        // 3. Header Separator
        guiGraphics.fill(x + 5, y + 25, x + w - 5, y + 26, 0x8000FFFF);
        
        // 4. Character Panel Background (Left side)
        guiGraphics.fill(x + 10, y + 35, x + 110, y + 150, 0x40000000); 
        guiGraphics.renderOutline(x + 10, y + 35, 100, 115, 0x6000FFFF);
        
        // 5. Enhanced Magic Circuit Decorations (PCB Style) - REDESIGNED
        // REMOVED all circuits and connecting lines as requested
        
        // (Empty)

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
            guiGraphics.drawString(this.font, Component.translatable(MagicConstants.GUI_MAGIC_EYES_AND_MODIFICATION), 125, 40, 0xFF00E0E0, false);
            guiGraphics.drawString(this.font, Component.translatable(MagicConstants.GUI_LOAD_MAGIC_EYES), 125, 60, 0xFFCCCCCC, false);
            guiGraphics.drawString(this.font, Component.translatable(MagicConstants.GUI_WIP), 125, 80, 0xFFAAAAAA, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable(MagicConstants.GUI_LEARNED_MAGIC), 125, 28, 0xFF00E0E0, false);
            // Label for filter
            // Button is at 230, 25. Text should be left of it.
            // "分类:"
            guiGraphics.drawString(this.font, "分类:", 200, 28, 0xFFAAAAAA, false);
        }
    }

        // Custom Neon Button Class
    class NeonButton extends Button {
        private final int hoverColor; // Add specific hover color support

        public NeonButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            this(x, y, width, height, message, onPress, 0xFF00FFFF); // Default Cyan
        }

        public NeonButton(int x, int y, int width, int height, Component message, OnPress onPress, int color) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.hoverColor = color;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int borderColor = isHoveredOrFocused() ? hoverColor : 0xFF00AAAA;
            int fillColor = isHoveredOrFocused() ? (hoverColor & 0x00FFFFFF) | 0x80000000 : 0x80000000;
            int textColor = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFAAAAAA;
            
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, fillColor);
            guiGraphics.renderOutline(getX(), getY(), width, height, borderColor);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, textColor);
        }
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
        this.filterButton = new NeonButton(this.leftPos + 230, this.topPos + 25, 40, 14, Component.literal("全部"), e -> {
            // Cycle filters: all -> jewel -> all
            if ("all".equals(filterCategory)) {
                filterCategory = "jewel";
                e.setMessage(Component.literal("宝石"));
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
            updateVisibility();
        });
        this.addRenderableWidget(imagebutton_magical_attributes);
        
        imagebutton_magical_properties = new NeonButton(tabX + (tabWidth + 2) * 2, tabY, tabWidth, tabHeight, Component.literal("魔术知识"), e -> {
            this.pageMode = 1;
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
