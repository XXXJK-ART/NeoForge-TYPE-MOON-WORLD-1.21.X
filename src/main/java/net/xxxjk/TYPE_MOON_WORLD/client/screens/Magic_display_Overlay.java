package net.xxxjk.TYPE_MOON_WORLD.client.screens;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;

import net.xxxjk.TYPE_MOON_WORLD.procedures.Returns_the_current_mana;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber({Dist.CLIENT})
@SuppressWarnings("null")
public class Magic_display_Overlay {
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int h = event.getGuiGraphics().guiHeight();
        Player entity = Minecraft.getInstance().player;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (true) {
            event.getGuiGraphics().blit(ResourceLocation.parse("typemoonworld:textures/screens/mana.png"), 2, h - 10, 0, 0, 8, 8, 8, 8);

            event.getGuiGraphics().drawString(Minecraft.getInstance().font,

                    Returns_the_current_mana.execute(entity), 12, h - 9, -10040065, false);

            if (entity != null) {
                TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                if (vars.is_magic_circuit_open) {
                    String magicName = "无";
                    if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                        String magicId = vars.selected_magics.get(vars.current_magic_index);
                        if ("ruby_throw".equals(magicId)) {
                             magicName = "红宝石投掷";
                        } else if ("sapphire_throw".equals(magicId)) {
                             magicName = "蓝宝石投掷";
                        } else if ("emerald_use".equals(magicId)) {
                             magicName = "绿宝石展开";
                        } else if ("topaz_throw".equals(magicId)) {
                             magicName = "黄宝石投掷";
                        }
                    }
                    event.getGuiGraphics().drawString(Minecraft.getInstance().font, "当前魔术：" + magicName, 2, h - 22, -10040065, false);
                }
            }
        }
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}