package net.xxxjk.TYPE_MOON_WORLD.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType.ReinforcementPart;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ModClientArmEvents {

    private static final int EMISSIVE_LIGHT = 15728880;
    private static final int SEMI_TRANSPARENT_WHITE = 0xCCFFFFFF;

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        // 第一人称手臂改由 PlayerRendererMixin.renderHand 精确叠加，避免事件路径下的姿态/面显示问题。
    }
}
