package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
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
import net.minecraft.client.gui.GuiGraphics;

import net.xxxjk.TYPE_MOON_WORLD.world.inventory.BasicInformationMenu;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class Basic_information_Screen extends AbstractContainerScreen<BasicInformationMenu> {
    private final static HashMap<String, Object> guistate = BasicInformationMenu.guistate;
    private final Level world;
    private final int x, y, z;
    private final Player entity;
    ImageButton imagebutton_basic_attributes;
    ImageButton imagebutton_magical_attributes;
    ImageButton imagebutton_magical_properties;

    public Basic_information_Screen(BasicInformationMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
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
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
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
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font,

                Back_mana.execute(entity), 71, 24, -13408513, false);
        guiGraphics.drawString(this.font,

                Back_max_mana.execute(entity), 71, 44, -13408513, false);
        guiGraphics.drawString(this.font,

                Back_player_mana_egenerated_every_moment.execute(entity), 71, 63, -13408513, false);
        guiGraphics.drawString(this.font,

                Back_player_restore_magic_moment.execute(entity), 71, 84, -13408513, false);
        TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        String baseAttributes = buildBaseAttributes(vars);
        String extraAttributes = buildExtraAttributes(vars);
        guiGraphics.drawString(this.font, baseAttributes, 71, 104, -13408513, false);
        guiGraphics.drawString(this.font, extraAttributes, 71, 124, -13408513, false);
    }

    @Override
    public void init() {
        super.init();
        imagebutton_basic_attributes = new ImageButton(this.leftPos + 4, this.topPos - 31, 32, 32,
                new WidgetSprites(ResourceLocation.parse("typemoonworld:textures/screens/basic_attributes.png"),
                        ResourceLocation.parse("typemoonworld:textures/screens/basic_attributes02.png")), e -> {
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
            PacketDistributor.sendToServer(new Basic_information_Button_Message(0, x, y, z));
            Basic_information_Button_Message.handleButtonAction(entity, 0, x, y, z);
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
            PacketDistributor.sendToServer(new Basic_information_Button_Message(1, x, y, z));
            Basic_information_Button_Message.handleButtonAction(entity, 1, x, y, z);
        }) {
            @Override
            public void renderWidget(@NotNull GuiGraphics guiGraphics, int x, int y, float partialTicks) {
                guiGraphics.blit(sprites.get(isActive(), isHoveredOrFocused()), getX(), getY(), 0, 0, width, height, width, height);
            }
        };
        guistate.put("button:imagebutton_magical_properties", imagebutton_magical_properties);
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
