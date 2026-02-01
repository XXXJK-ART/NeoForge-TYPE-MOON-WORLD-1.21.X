package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.components.Button;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_Button_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.procedures.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Quaternionf;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.xxxjk.TYPE_MOON_WORLD.world.inventory.BasicInformationMenu;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

@SuppressWarnings({"null", "unused"})
public class Basic_information_Screen extends AbstractContainerScreen<BasicInformationMenu> {
    private final static HashMap<String, Object> guistate = BasicInformationMenu.guistate;
    private final Level world;
    private final int x, y, z;
    private final Player entity;
    Button imagebutton_basic_attributes;
    Button imagebutton_magical_attributes;
    Button imagebutton_magical_properties;

    public Basic_information_Screen(BasicInformationMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 360;
        this.imageHeight = 200;
    }

    private static final ResourceLocation texture = ResourceLocation.parse("typemoonworld:textures/screens/basic_information.png");

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (Basic_information_back_player_self.execute(entity) instanceof LivingEntity livingEntity) {
            // Reposition entity to align with new larger UI (Left side)
            // Left Panel is roughly from x+10 to x+110
            // Center of that is x+60
            // Top is y+35, height 150 -> bottom y+185.
            // Entity feet at y+170
            this.renderEntityInInventoryFollowsAngle(guiGraphics, this.leftPos + 60, this.topPos + 170, 0f
                    + (float) Math.atan((this.leftPos + 60 - mouseX) / 40.0), (float) Math.atan((this.topPos + 87 - mouseY) / 40.0), livingEntity);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
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
        
        GuiUtils.renderBackground(guiGraphics, x, y, w, h);
        
        // 4. Character Panel Background (Left side)
        // guiGraphics.fill(x + 10, y + 35, x + 110, y + 185, 0x40000000); 
        // guiGraphics.renderOutline(x + 10, y + 35, 100, 150, 0x6000FFFF);
        GuiUtils.renderTechFrame(guiGraphics, x + 10, y + 35, 100, 150, 0xFF00AAAA, 0xFF00FFFF);
        
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title (Removed Basic Information text)
        // guiGraphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, 8, 0xFF00FFFF);

        // Right Column Stats (Starting x = 120)
        int startX = 125;
        int startY = 40;
        int spacing = 15;
        
        // Helper to draw label: value
        drawStat(guiGraphics, "当前魔力:", Back_mana.execute(entity), startX, startY);
        drawStat(guiGraphics, "魔力上限:", Back_max_mana.execute(entity).replace("魔力值上限：", ""), startX, startY + spacing);
        drawStat(guiGraphics, "魔力回复:", Back_player_mana_egenerated_every_moment.execute(entity), startX, startY + spacing * 2);
        drawStat(guiGraphics, "回复间隔:", Back_player_restore_magic_moment.execute(entity), startX, startY + spacing * 3);
        
        // Attributes Section
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        guiGraphics.drawString(this.font, "基础属性:", startX, startY + spacing * 5, 0xFF00E0E0, false);
        String baseAttr = buildBaseAttributes(vars).replace("基础属性：", "");
        guiGraphics.drawWordWrap(this.font, Component.literal(baseAttr), startX, startY + spacing * 6, 160, 0xFFCCCCCC);
        
        guiGraphics.drawString(this.font, "扩展属性:", startX, startY + spacing * 8, 0xFF00E0E0, false);
        String extraAttr = buildExtraAttributes(vars).replace("扩展属性：", "");
        guiGraphics.drawWordWrap(this.font, Component.literal(extraAttr), startX, startY + spacing * 9, 160, 0xFFCCCCCC);
    }
    
    private void drawStat(GuiGraphics gui, String label, String value, int x, int y) {
        gui.drawString(this.font, label, x, y, 0xFFAAAAAA, false);
        gui.drawString(this.font, value, x + 60, y, 0xFFFFFFFF, false);
    }

    // Custom Neon Button Class Removed - Use NeonButton.java
    
    @Override
    public void init() {
        super.init();
        
        // Reposition buttons to top right or aligned with new layout
        // Let's place them as tabs at the top right of the panel
        // Panel starts at x+10, y+35.
        // Let's put buttons above the header line (y+25).
        
        int btnY = this.topPos + 5;
        int btnWidth = 80;
        int btnHeight = 16;
        int startX = this.leftPos + this.imageWidth - (btnWidth * 3) - 10;
        
        imagebutton_basic_attributes = new NeonButton(startX, btnY, btnWidth, btnHeight, Component.literal("基础属性"), e -> {
            // Already here
        });
        // Highlight active tab
        // imagebutton_basic_attributes.active = false; 
        this.addRenderableWidget(imagebutton_basic_attributes);

        imagebutton_magical_attributes = new NeonButton(startX + btnWidth + 2, btnY, btnWidth, btnHeight, Component.literal("身体改造"), e -> {
            PacketDistributor.sendToServer(new Basic_information_Button_Message(0, x, y, z));
            Basic_information_Button_Message.handleButtonAction(entity, 0, x, y, z);
        });
        this.addRenderableWidget(imagebutton_magical_attributes);

        imagebutton_magical_properties = new NeonButton(startX + (btnWidth + 2) * 2, btnY, btnWidth, btnHeight, Component.literal("魔术知识"), e -> {
            PacketDistributor.sendToServer(new Basic_information_Button_Message(1, x, y, z));
            Basic_information_Button_Message.handleButtonAction(entity, 1, x, y, z);
        });
        this.addRenderableWidget(imagebutton_magical_properties);
    }

    private String buildBaseAttributes(TypeMoonWorldModVariables.PlayerVariables vars) {
        StringBuilder builder = new StringBuilder("基础属性：");
        boolean has = false;
        if (vars.player_magic_attributes_earth) {
            builder.append("地");
            has = true;
        }
        if (vars.player_magic_attributes_water) {
            if (has) {
                builder.append("·");
            }
            builder.append("水");
            has = true;
        }
        if (vars.player_magic_attributes_fire) {
            if (has) {
                builder.append("·");
            }
            builder.append("火");
            has = true;
        }
        if (vars.player_magic_attributes_wind) {
            if (has) {
                builder.append("·");
            }
            builder.append("风");
            has = true;
        }
        if (vars.player_magic_attributes_ether) {
            if (has) {
                builder.append("·");
            }
            builder.append("以太");
            has = true;
        }
        if (!has) {
            builder.append("/");
        }
        return builder.toString();
    }

    private String buildExtraAttributes(TypeMoonWorldModVariables.PlayerVariables vars) {
        StringBuilder builder = new StringBuilder("扩展属性：");
        boolean has = false;
        if (vars.player_magic_attributes_none) {
            builder.append("无属性");
            has = true;
        }
        if (vars.player_magic_attributes_imaginary_number) {
            if (has) {
                builder.append("·");
            }
            builder.append("虚数");
            has = true;
        }
        if (vars.player_magic_attributes_sword) {
            if (has) {
                builder.append("·");
            }
            builder.append("剑");
            has = true;
        }
        if (!has) {
            builder.append("/");
        }
        return builder.toString();
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
