package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
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

import net.minecraft.client.gui.components.Button;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;

@SuppressWarnings("null")
public class Magical_attributes_Screen extends AbstractContainerScreen<MagicalattributesMenu> {
    private final static HashMap<String, Object> guistate = MagicalattributesMenu.guistate;
    private final Level world;
    private final int x, y, z;
    private final Player entity;
    private int pageMode;
    ImageButton imagebutton_basic_attributes;
    ImageButton imagebutton_magical_attributes;
    ImageButton imagebutton_magical_properties;
    Button rubyThrowButton;
    Button sapphireThrowButton;
    Button emeraldUseButton;
    Button topazThrowButton;
    Button rubyFlameSwordButton;
    Button sapphireWinterRiverButton;
    Button emeraldShieldButton;
    Button topazReinforcementButton;

    public Magical_attributes_Screen(MagicalattributesMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.pageMode = container.pageMode;
        this.imageWidth = 300;
        this.imageHeight = 160;
    }

    private static final ResourceLocation texture = ResourceLocation.parse("typemoonworld:textures/screens/basic_information.png");

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (Basic_information_back_player_self.execute(entity) instanceof LivingEntity livingEntity) {
            this.renderEntityInInventoryFollowsAngle(guiGraphics, this.leftPos + 37, this.topPos + 136, 0f
                    + (float) Math.atan((this.leftPos + 37 - mouseX) / 40.0), (float) Math.atan((this.topPos + 87 - mouseY) / 40.0), livingEntity);
        }
        
        if (this.pageMode == 1) {
            updateButtonLabels();
        }
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    private void updateButtonLabels() {
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        if (rubyThrowButton != null) {
            boolean selected = vars.selected_magics.contains("ruby_throw");
            rubyThrowButton.setMessage(Component.translatable(selected ? MagicConstants.KEY_MAGIC_RUBY_THROW_SELECTED : MagicConstants.KEY_MAGIC_RUBY_THROW_SHORT));
        }
        
        if (sapphireThrowButton != null) {
            boolean selected = vars.selected_magics.contains("sapphire_throw");
            sapphireThrowButton.setMessage(Component.translatable(selected ? MagicConstants.KEY_MAGIC_SAPPHIRE_THROW_SELECTED : MagicConstants.KEY_MAGIC_SAPPHIRE_THROW_SHORT));
        }
        
        if (emeraldUseButton != null) {
            boolean selected = vars.selected_magics.contains("emerald_use");
            emeraldUseButton.setMessage(Component.translatable(selected ? MagicConstants.KEY_MAGIC_EMERALD_USE_SELECTED : MagicConstants.KEY_MAGIC_EMERALD_USE_SHORT));
        }
        
        if (topazThrowButton != null) {
            boolean selected = vars.selected_magics.contains("topaz_throw");
            topazThrowButton.setMessage(Component.translatable(selected ? MagicConstants.KEY_MAGIC_TOPAZ_THROW_SELECTED : MagicConstants.KEY_MAGIC_TOPAZ_THROW_SHORT));
        }

        if (rubyFlameSwordButton != null) {
            boolean selected = vars.selected_magics.contains("ruby_flame_sword");
            rubyFlameSwordButton.setMessage(Component.translatable(selected ? MagicConstants.KEY_MAGIC_RUBY_FLAME_SWORD_SELECTED : MagicConstants.KEY_MAGIC_RUBY_FLAME_SWORD_SHORT));
        }

        if (sapphireWinterRiverButton != null) {
            boolean selected = vars.selected_magics.contains("sapphire_winter_frost");
            sapphireWinterRiverButton.setMessage(Component.translatable(selected ? MagicConstants.KEY_MAGIC_SAPPHIRE_WINTER_FROST_SELECTED : MagicConstants.KEY_MAGIC_SAPPHIRE_WINTER_FROST_SHORT));
        }

        if (emeraldShieldButton != null) {
            boolean selected = vars.selected_magics.contains("emerald_winter_river");
            emeraldShieldButton.setMessage(Component.translatable(selected ? MagicConstants.KEY_MAGIC_EMERALD_WINTER_RIVER_SELECTED : MagicConstants.KEY_MAGIC_EMERALD_WINTER_RIVER_SHORT));
        }

        if (topazReinforcementButton != null) {
            boolean selected = vars.selected_magics.contains("topaz_reinforcement");
            topazReinforcementButton.setMessage(Component.translatable(selected ? MagicConstants.KEY_MAGIC_TOPAZ_REINFORCEMENT_SELECTED : MagicConstants.KEY_MAGIC_TOPAZ_REINFORCEMENT_SHORT));
        }
    }
    
    private void updateVisibility() {
        boolean visible = (this.pageMode == 1);
        if (rubyThrowButton != null) rubyThrowButton.visible = visible;
        if (sapphireThrowButton != null) sapphireThrowButton.visible = visible;
        if (emeraldUseButton != null) emeraldUseButton.visible = visible;
        if (topazThrowButton != null) topazThrowButton.visible = visible;
        if (rubyFlameSwordButton != null) rubyFlameSwordButton.visible = visible;
        if (sapphireWinterRiverButton != null) sapphireWinterRiverButton.visible = visible;
        if (emeraldShieldButton != null) emeraldShieldButton.visible = visible;
        if (topazReinforcementButton != null) topazReinforcementButton.visible = visible;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
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
        if (pageMode == 0) {
            guiGraphics.drawString(this.font, Component.translatable(MagicConstants.GUI_MAGIC_EYES_AND_MODIFICATION), 71, 24, MagicConstants.UI_COLOR_TEXT, false);
            guiGraphics.drawString(this.font, Component.translatable(MagicConstants.GUI_LOAD_MAGIC_EYES), 71, 44, MagicConstants.UI_COLOR_TEXT, false);
            guiGraphics.drawString(this.font, Component.translatable(MagicConstants.GUI_WIP), 71, 63, MagicConstants.UI_COLOR_TEXT, false);
        } else {
            guiGraphics.drawString(this.font, Component.translatable(MagicConstants.GUI_LEARNED_MAGIC), 71, 24, MagicConstants.UI_COLOR_TEXT, false);
        }
    }

    @Override
    public void init() {
        super.init();
        
        // 2x2 Grid Layout
        // Column 1: x = 71, Column 2: x = 141 (71 + 60 + 10)
        // Row 1: y = 40, Row 2: y = 60
        
        int col1 = this.leftPos + 71;
        int col2 = this.leftPos + 141;
        int row1 = this.topPos + 40;
        int row2 = this.topPos + 60;
        int btnWidth = 60;
        int btnHeight = 16;
        
        rubyThrowButton = Button.builder(Component.translatable(MagicConstants.KEY_MAGIC_RUBY_THROW), e -> {
            TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isSelected = vars.selected_magics.contains("ruby_throw");
            PacketDistributor.sendToServer(new SelectMagicMessage("ruby_throw", !isSelected));
        }).bounds(col1, row1, btnWidth, btnHeight).build();
        rubyThrowButton.visible = false;
        this.addRenderableWidget(rubyThrowButton);

        sapphireThrowButton = Button.builder(Component.translatable(MagicConstants.KEY_MAGIC_SAPPHIRE_THROW), e -> {
            TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isSelected = vars.selected_magics.contains("sapphire_throw");
            PacketDistributor.sendToServer(new SelectMagicMessage("sapphire_throw", !isSelected));
        }).bounds(col2, row1, btnWidth, btnHeight).build(); 
        sapphireThrowButton.visible = false;
        this.addRenderableWidget(sapphireThrowButton);

        emeraldUseButton = Button.builder(Component.translatable(MagicConstants.KEY_MAGIC_EMERALD_USE), e -> {
            TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isSelected = vars.selected_magics.contains("emerald_use");
            PacketDistributor.sendToServer(new SelectMagicMessage("emerald_use", !isSelected));
        }).bounds(col1, row2, btnWidth, btnHeight).build(); 
        emeraldUseButton.visible = false;
        this.addRenderableWidget(emeraldUseButton);

        topazThrowButton = Button.builder(Component.translatable(MagicConstants.KEY_MAGIC_TOPAZ_THROW), e -> {
            TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isSelected = vars.selected_magics.contains("topaz_throw");
            PacketDistributor.sendToServer(new SelectMagicMessage("topaz_throw", !isSelected));
        }).bounds(col2, row2, btnWidth, btnHeight).build(); 
        topazThrowButton.visible = false;
        this.addRenderableWidget(topazThrowButton);

        // Row 3: y = 80, Row 4: y = 100
        int row3 = this.topPos + 80;
        int row4 = this.topPos + 100;

        rubyFlameSwordButton = Button.builder(Component.translatable(MagicConstants.KEY_MAGIC_RUBY_FLAME_SWORD), e -> {
            TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isSelected = vars.selected_magics.contains("ruby_flame_sword");
            PacketDistributor.sendToServer(new SelectMagicMessage("ruby_flame_sword", !isSelected));
        }).bounds(col1, row3, btnWidth, btnHeight).build();
        rubyFlameSwordButton.visible = false;
        this.addRenderableWidget(rubyFlameSwordButton);

        sapphireWinterRiverButton = Button.builder(Component.translatable(MagicConstants.KEY_MAGIC_SAPPHIRE_WINTER_FROST), e -> {
            TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isSelected = vars.selected_magics.contains("sapphire_winter_frost");
            PacketDistributor.sendToServer(new SelectMagicMessage("sapphire_winter_frost", !isSelected));
        }).bounds(col2, row3, btnWidth, btnHeight).build();
        sapphireWinterRiverButton.visible = false;
        this.addRenderableWidget(sapphireWinterRiverButton);

        emeraldShieldButton = Button.builder(Component.translatable(MagicConstants.KEY_MAGIC_EMERALD_WINTER_RIVER), e -> {
            TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isSelected = vars.selected_magics.contains("emerald_winter_river");
            PacketDistributor.sendToServer(new SelectMagicMessage("emerald_winter_river", !isSelected));
        }).bounds(col1, row4, btnWidth, btnHeight).build();
        emeraldShieldButton.visible = false;
        this.addRenderableWidget(emeraldShieldButton);

        topazReinforcementButton = Button.builder(Component.translatable(MagicConstants.KEY_MAGIC_TOPAZ_REINFORCEMENT), e -> {
            TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean isSelected = vars.selected_magics.contains("topaz_reinforcement");
            PacketDistributor.sendToServer(new SelectMagicMessage("topaz_reinforcement", !isSelected));
        }).bounds(col2, row4, btnWidth, btnHeight).build();
        topazReinforcementButton.visible = false;
        this.addRenderableWidget(topazReinforcementButton);

        imagebutton_basic_attributes = new ImageButton(this.leftPos + 4, this.topPos - 31, 32, 32,
                new WidgetSprites(ResourceLocation.parse("typemoonworld:textures/screens/basic_attributes.png"),
                        ResourceLocation.parse("typemoonworld:textures/screens/basic_attributes02.png")), e -> {
            PacketDistributor.sendToServer(new Magical_attributes_Button_Message(0, x, y, z));
            Magical_attributes_Button_Message.handleButtonAction(entity, 0, x, y, z);
        }) {
            @Override
            public void renderWidget(@NotNull GuiGraphics guiGraphics, int x, int y, float partialTicks) {
                guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
            }
        };
        guistate.put("button:imagebutton_basic_attributes", imagebutton_basic_attributes);
        this.addRenderableWidget(imagebutton_basic_attributes);
        
        imagebutton_magical_attributes = new ImageButton(this.leftPos + 38, this.topPos - 31, 32, 32,
                new WidgetSprites(ResourceLocation.parse("typemoonworld:textures/screens/magical_attributes.png"),
                        ResourceLocation.parse("typemoonworld:textures/screens/magical_attributes02.png")), e -> {
            this.pageMode = 0;
            updateVisibility();
        }) {
            @Override
            public void renderWidget(@NotNull GuiGraphics guiGraphics, int x, int y, float partialTicks) {
                guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
            }
        };
        guistate.put("button:imagebutton_magical_attributes", imagebutton_magical_attributes);
        this.addRenderableWidget(imagebutton_magical_attributes);
        
        imagebutton_magical_properties = new ImageButton(this.leftPos + 72, this.topPos - 31, 32, 32,
                new WidgetSprites(ResourceLocation.parse("typemoonworld:textures/screens/magical_properties.png"),
                        ResourceLocation.parse("typemoonworld:textures/screens/magical_properties02.png")), e -> {
            this.pageMode = 1;
            updateVisibility();
        }) {
            @Override
            public void renderWidget(@NotNull GuiGraphics guiGraphics, int x, int y, float partialTicks) {
                guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
            }
        };
        guistate.put("button:imagebutton_magical_properties", imagebutton_magical_properties);
        this.addRenderableWidget(imagebutton_magical_properties);
        
        // Initial update
        updateVisibility();
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
