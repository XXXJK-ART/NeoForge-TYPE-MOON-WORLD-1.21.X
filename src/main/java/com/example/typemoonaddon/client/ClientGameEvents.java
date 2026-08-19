package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.network.OpenSpacePayload;
import com.example.typemoonaddon.network.BlockAbsorptionHoldPayload;
import com.example.typemoonaddon.network.ShadowMaterializationHoldPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.network.chat.Component;
import com.example.typemoonaddon.magic.SakuraPollutionService;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.client.renderer.CursedArmorLayer;
import com.example.typemoonaddon.client.renderer.PlayerArmorOcclusion;
import com.example.typemoonaddon.data.ImaginarySpaceData.CursedArmorState;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private static boolean blockCastHeld;
    private static int blockHoldHeartbeatTicks;
    private static boolean materializationCastHeld;
    private static int materializationHeartbeatTicks;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        EmiyaShadowWeaponOcclusion.restoreOutstanding();
        ClientEffects.tick(minecraft);
        ClientNightShadowCamera.tick(minecraft);
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        TypeMoonClientModeBridge.tick(minecraft);
        tickBlockCastHeartbeat();
        tickMaterializationHeartbeat();

        while (ClientModEvents.OPEN_SPACE.consumeClick()) {
            PacketDistributor.sendToServer(OpenSpacePayload.INSTANCE);
        }
    }

    private static void tickMaterializationHeartbeat() {
        boolean held = TypeMoonClientModeBridge.isShadowMaterializationCastHeld();
        materializationHeartbeatTicks++;
        if (held != materializationCastHeld || (held && materializationHeartbeatTicks >= 2)) {
            materializationCastHeld = held;
            materializationHeartbeatTicks = 0;
            PacketDistributor.sendToServer(new ShadowMaterializationHoldPayload(held));
        }
    }

    private static void tickBlockCastHeartbeat() {
        boolean held = TypeMoonClientModeBridge.isImaginaryStorageCastHeld();
        blockHoldHeartbeatTicks++;
        if (held != blockCastHeld || (held && blockHoldHeartbeatTicks >= 2)) {
            blockCastHeld = held;
            blockHoldHeartbeatTicks = 0;
            PacketDistributor.sendToServer(new BlockAbsorptionHoldPayload(held));
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        float scale = ClientEffects.deathDissolutionScale(event.getEntity().getId(), event.getPartialTick());
        if (scale < 1.0F) {
            // Living models are positioned from their feet, so the top contracts first.
            event.getPoseStack().scale(1.0F, scale, 1.0F);
        }
        if (event.getRenderer().getModel() instanceof PlayerModel<?>) {
            EmiyaShadowPiercingAnimation.prepare(
                event.getEntity(),
                event.getPartialTick()
            );
            EmiyaShadowWeaponOcclusion.apply(event.getEntity(), event.getPartialTick());
        } else {
            EmiyaShadowPiercingAnimation.clearPrepared();
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        EmiyaShadowWeaponOcclusion.restore(event.getEntity());
        ClientEffects.renderShadowBinding(
            event.getEntity(),
            event.getPoseStack(),
            event.getMultiBufferSource(),
            event.getPartialTick()
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!event.isCanceled()) {
            PlayerArmorOcclusion.apply(event.getEntity(), event.getRenderer());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        PlayerArmorOcclusion.restore(event.getEntity());
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(event.getPlayer());
        if (renderer instanceof PlayerRenderer playerRenderer) {
            CursedArmorLayer.renderFirstPersonArm(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight(),
                event.getPlayer(),
                event.getArm(),
                playerRenderer.getModel(),
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)
            );
            if (event.getPlayer().getData(AddonAttachments.CURSED_ARMOR_VIEW.get()).state() == CursedArmorState.ACTIVE
                && CursedArmorLayer.coversFirstPersonArm(event.getArm())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        ClientEffects.renderMagicOutputShockwaves(event);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        var imaginarySpace = minecraft.player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        boolean showErosion = imaginarySpace.grailWormAscended();
        LivingEntity shown = SakuraPollutionService.isPolluted(minecraft.player) ? minecraft.player : null;
        LivingEntity spiritualShown = minecraft.player.getData(AddonAttachments.SPIRITUAL_DAMAGE.get()).percent() > 0 ? minecraft.player : null;
        if (shown == null && minecraft.hitResult instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity living
            && SakuraPollutionService.isPolluted(living)) {
            shown = living;
        }
        if (minecraft.hitResult instanceof EntityHitResult entityHit
            && entityHit.getEntity() instanceof LivingEntity living
            && living.getData(AddonAttachments.SPIRITUAL_DAMAGE.get()).percent() > 0) {
            spiritualShown = living;
        }
        if (shown == null && spiritualShown == null && !showErosion) {
            return;
        }

        int width = 124;
        int height = 7;
        int x = (event.getGuiGraphics().guiWidth() - width) / 2;
        if (showErosion) {
            float erosion = imaginarySpace.grailErosionProgress();
            renderProgressBar(
                event,
                minecraft,
                x,
                event.getGuiGraphics().guiHeight() - 76,
                width,
                height,
                erosion,
                0xFF8A244F,
                Component.translatable("gui.typemoonworld.grail_erosion", Math.round(erosion * 100.0F))
            );
        }
        if (shown != null) {
            float progress = SakuraPollutionService.progress(shown);
            renderProgressBar(
                event,
                minecraft,
                x,
                event.getGuiGraphics().guiHeight() - 58,
                width,
                height,
                progress,
                0xFFE0202A,
                Component.translatable("gui.typemoonworld.pollution", Math.round(progress * 100.0F))
            );
        }
        if (spiritualShown != null) {
            var spiritual = spiritualShown.getData(AddonAttachments.SPIRITUAL_DAMAGE.get());
            float percent = spiritual.percent();
            renderProgressBar(event, minecraft, x, event.getGuiGraphics().guiHeight() - 40, width, height, percent / 100.0F, 0xFF7A102B, Component.translatable("gui.typemoonworld.spiritual_damage", Math.round(percent)));
            float collapse = spiritual.collapseProgress(spiritualShown.level().getGameTime());
            if (collapse > 0.0F) {
                renderProgressBar(
                    event,
                    minecraft,
                    x,
                    event.getGuiGraphics().guiHeight() - 22,
                    width,
                    height,
                    collapse,
                    0xFFD52020,
                    Component.translatable("gui.typemoonworld.spiritual_collapse", Math.round(collapse * 100.0F))
                );
            }
        }
    }

    private static void renderProgressBar(
        RenderGuiEvent.Post event,
        Minecraft minecraft,
        int x,
        int y,
        int width,
        int height,
        float progress,
        int color,
        Component label
    ) {
        int filled = Math.round((width - 2) * progress);
        event.getGuiGraphics().fill(x, y, x + width, y + height, 0xCC080808);
        event.getGuiGraphics().fill(x + 1, y + 1, x + 1 + filled, y + height - 1, color);
        event.getGuiGraphics().drawString(
            minecraft.font,
            label,
            (event.getGuiGraphics().guiWidth() - minecraft.font.width(label)) / 2,
            y - 11,
            0xFFF0E8E8,
            true
        );
    }

    private ClientGameEvents() {
    }
}
