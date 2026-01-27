package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectProjectionItemMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectionPresetScreen extends Screen {
    private final Player player;
    private int leftPos, topPos;
    // Same size as Magical_attributes_Screen: 360x200
    private int imageWidth = 360;
    private int imageHeight = 200;
    private float scrollOffs = 0;
    private int startIndex = 0;
    private boolean scrolling;
    
    // Filters
    private String currentFilter = "all"; // all, building, combat, tools, special, etc.
    private List<Button> filterButtons = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();
    
    // UI Constants
    private static final int LIST_X_OFFSET = 10;
    private static final int LIST_Y_OFFSET = 40;
    private static final int SLOT_SIZE = 18;
    private static final int COLS = 18; // 340 / 18 ≈ 18
    private static final int ROWS = 8;  // 150 / 18 ≈ 8
    
    public ProjectionPresetScreen(Player player) {
        super(Component.literal("Projection Preset"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        initFilterButtons();
        updateFilteredItems();
    }
    
    private void initFilterButtons() {
        int btnY = topPos + 5;
        int btnH = 20;
        int startX = leftPos + 10;
        int gap = 2;
        
        // Categories mapping to CreativeTabs somewhat
        // Building, Colored, Natural, Functional, Redstone, Tools, Combat, Food, Ingredients, Spawn Eggs?
        // Let's simplify: Building(Blocks), Combat(Weapons/Armor), Tools, Misc(Items), Special(Enchanted/Renamed)
        
        String[] filters = {"all", "building", "combat", "tools", "misc", "special"};
        String[] labels = {"gui.typemoonworld.projection.filter.all", "gui.typemoonworld.projection.filter.building", 
                           "gui.typemoonworld.projection.filter.combat", "gui.typemoonworld.projection.filter.tools",
                           "gui.typemoonworld.projection.filter.misc", "gui.typemoonworld.projection.filter.special"};
        
        int currentX = startX;
        for (int i = 0; i < filters.length; i++) {
            String filter = filters[i];
            String labelKey = labels[i];
            int width = Minecraft.getInstance().font.width(Component.translatable(labelKey)) + 10;
            if (width < 40) width = 40;
            
            Button btn = Button.builder(Component.translatable(labelKey), b -> {
                this.currentFilter = filter;
                this.scrollOffs = 0;
                this.startIndex = 0;
                updateFilteredItems();
            }).bounds(currentX, btnY, width, btnH).build();
            
            this.addRenderableWidget(btn);
            filterButtons.add(btn);
            currentX += width + gap;
        }
    }
    
    private void updateFilteredItems() {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        List<ItemStack> allItems = vars.analyzed_items;
        
        filteredItems = allItems.stream().filter(stack -> {
            if ("all".equals(currentFilter)) return true;
            
            if ("special".equals(currentFilter)) {
                // Enchanted or Renamed
                boolean isEnchanted = stack.isEnchanted();
                boolean isRenamed = stack.has(DataComponents.CUSTOM_NAME);
                return isEnchanted || isRenamed;
            }
            
            // Check creative tabs or item tags for other categories
            // This is a rough approximation
            if ("combat".equals(currentFilter)) {
                // Swords, Bows, Armor
                return stack.getItem() instanceof net.minecraft.world.item.SwordItem || 
                       stack.getItem() instanceof net.minecraft.world.item.AxeItem ||
                       stack.getItem() instanceof net.minecraft.world.item.BowItem ||
                       stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
                       stack.getItem() instanceof net.minecraft.world.item.ShieldItem ||
                       stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
            }
            
            if ("tools".equals(currentFilter)) {
                return stack.getItem() instanceof net.minecraft.world.item.DiggerItem || // Pickaxe, Shovel, Axe, Hoe
                       stack.getItem() instanceof net.minecraft.world.item.ShearsItem ||
                       stack.getItem() instanceof net.minecraft.world.item.FlintAndSteelItem ||
                       stack.getItem() instanceof net.minecraft.world.item.FishingRodItem;
            }
            
            if ("building".equals(currentFilter)) {
                return stack.getItem() instanceof net.minecraft.world.item.BlockItem;
            }
            
            if ("misc".equals(currentFilter)) {
                // Everything else that is not caught above
                // Or maybe just things that are NOT blocks, tools, combat
                 boolean isCombat = stack.getItem() instanceof net.minecraft.world.item.SwordItem || 
                        stack.getItem() instanceof net.minecraft.world.item.AxeItem ||
                        stack.getItem() instanceof net.minecraft.world.item.BowItem ||
                       stack.getItem() instanceof net.minecraft.world.item.CrossbowItem ||
                       stack.getItem() instanceof net.minecraft.world.item.ShieldItem ||
                       stack.getItem() instanceof net.minecraft.world.item.ArmorItem;
                boolean isTool = stack.getItem() instanceof net.minecraft.world.item.DiggerItem;
                boolean isBlock = stack.getItem() instanceof net.minecraft.world.item.BlockItem;
                return !isCombat && !isTool && !isBlock;
            }
            
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Background
        // Modern gradient style matching Magical_attributes_Screen
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;
        
        guiGraphics.fillGradient(x, y, x + w, y + h, 0xF0101020, 0xF0050510);
        int borderColor = 0xFF00FFFF;
        guiGraphics.renderOutline(x, y, w, h, borderColor);
        
        // Header Separator
        guiGraphics.fill(x + 5, y + 30, x + w - 5, y + 31, 0x8000FFFF);

        // Items List Area
        int listX = leftPos + LIST_X_OFFSET;
        int listY = topPos + LIST_Y_OFFSET;
        
        int totalVisible = COLS * ROWS;

        for (int i = 0; i < totalVisible; i++) {
            int index = startIndex + i;
            if (index >= filteredItems.size()) break;

            ItemStack stack = filteredItems.get(index);
            // Ensure count is 1 for display
            if (stack.getCount() != 1) {
                stack.setCount(1);
            }
            
            int col = i % COLS;
            int row = i / COLS;
            int slotX = listX + col * SLOT_SIZE;
            int slotY = listY + row * SLOT_SIZE;

            // Highlight selected
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isSelected = ItemStack.isSameItemSameComponents(stack, vars.projection_selected_item);
            
            if (isSelected) {
                guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x8000FF00);
            }
            
            // Hover
            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                 guiGraphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0x80FFFFFF);
                 guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }

            guiGraphics.renderItem(stack, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, stack, slotX + 1, slotY + 1);
        }
        
        // Scrollbar (Simple indicator)
        int totalRows = (filteredItems.size() + COLS - 1) / COLS;
        if (totalRows > ROWS) {
            int scrollBarX = x + w - 8;
            int scrollBarY = listY;
            int scrollBarH = ROWS * SLOT_SIZE;
            
            int barHeight = (int)((float)(ROWS * scrollBarH) / totalRows);
            if (barHeight < 20) barHeight = 20;
            
            int barTop = scrollBarY + (int)(this.scrollOffs * (scrollBarH - barHeight));
            
            guiGraphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarH, 0x80000000);
            guiGraphics.fill(scrollBarX, barTop, scrollBarX + 4, barTop + barHeight, 0xFF00FFFF);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = leftPos + LIST_X_OFFSET;
        int listY = topPos + LIST_Y_OFFSET;
        int totalVisible = COLS * ROWS;

        for (int i = 0; i < totalVisible; i++) {
            int index = startIndex + i;
            if (index >= filteredItems.size()) break;

            int col = i % COLS;
            int row = i / COLS;
            int slotX = listX + col * SLOT_SIZE;
            int slotY = listY + row * SLOT_SIZE;

            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
                // Find original index in analyzed_items to send correct index?
                // Wait, message sends index. If we filter, index in filtered list != index in full list.
                // We need to send the index in the FULL list.
                ItemStack clickedItem = filteredItems.get(index);
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                int fullIndex = -1;
                for(int k=0; k<vars.analyzed_items.size(); k++) {
                    // Object identity might not work if copied, use exact match
                    if (ItemStack.isSameItemSameComponents(vars.analyzed_items.get(k), clickedItem)) {
                        fullIndex = k;
                        break;
                    }
                }
                
                if (fullIndex != -1) {
                    PacketDistributor.sendToServer(new SelectProjectionItemMessage(fullIndex));
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int totalRows = (filteredItems.size() + COLS - 1) / COLS;
        if (totalRows > ROWS) {
            float scrollStep = 1.0f / (totalRows - ROWS);
            this.scrollOffs = Mth.clamp(this.scrollOffs - (float)deltaY * scrollStep, 0.0F, 1.0F);
            
            int maxStartRow = totalRows - ROWS;
            int startRow = Math.round(this.scrollOffs * maxStartRow);
            this.startIndex = startRow * COLS;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
