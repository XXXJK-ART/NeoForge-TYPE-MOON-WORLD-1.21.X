package net.xxxjk.TYPE_MOON_WORLD.client.gui;

import net.xxxjk.TYPE_MOON_WORLD.world.inventory.BasicInformationMenu;
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

import net.xxxjk.TYPE_MOON_WORLD.procedures.Back_mana;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Back_max_mana;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Back_player_mana_egenerated_every_moment;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Back_player_restore_magic_moment;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Basic_information_back_player_self;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class Basic_information_Screen extends AbstractContainerScreen<BasicInformationMenu> {
    private final static HashMap<String, Object> guitarist = BasicInformationMenu.guitarist;
    private final Level world;
    private final int x, y, z;
    private final Player entity;

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
            this.renderEntityInInventoryFollowsAngle(guiGraphics, this.leftPos + 37, this.topPos + 136, 0f +
                    (float) Math.atan((this.leftPos + 37 - mouseX) / 40.0), (float) Math.atan((this.topPos + 87 - mouseY) / 40.0), livingEntity);
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
    }

    @Override
    public void init() {
        super.init();
    }

    private void renderEntityInInventoryFollowsAngle(GuiGraphics guiGraphics, int x, int y, float angleXComponent, float angleYComponent, LivingEntity entity) {
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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
