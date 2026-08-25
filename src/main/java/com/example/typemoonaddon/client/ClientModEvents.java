package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.model.BlackShadowModel;
import com.example.typemoonaddon.client.model.ShadowFamiliarModel;
import com.example.typemoonaddon.client.model.ShadowFamiliarOutlineModel;
import com.example.typemoonaddon.client.model.WingedWormModel;
import com.example.typemoonaddon.client.renderer.BlackMudCorruptionLayer;
import com.example.typemoonaddon.client.renderer.BlackShadowRenderer;
import com.example.typemoonaddon.client.renderer.CursedArmorLayer;
import com.example.typemoonaddon.client.renderer.GillesDeRaisArmorRenderer;
import com.example.typemoonaddon.client.renderer.GillesDeRaisRenderer;
import com.example.typemoonaddon.client.renderer.HugeSeaMonsterRenderer;
import com.example.typemoonaddon.client.renderer.ShadowFamiliarRenderer;
import com.example.typemoonaddon.client.renderer.ShadowPiercingRhoAiasRenderer;
import com.example.typemoonaddon.client.renderer.SeaMonsterRenderer;
import com.example.typemoonaddon.client.renderer.VoidRingRegaliaRenderer;
import com.example.typemoonaddon.client.renderer.WormRenderer;
import com.example.typemoonaddon.client.renderer.SummonedSpiritParticleRenderer;
import com.example.typemoonaddon.client.renderer.BoundaryMarkRenderer;
import com.example.typemoonaddon.client.BlackMudControlScreen;
import com.example.typemoonaddon.client.BlackMudSummonModeScreen;
import com.example.typemoonaddon.client.ImaginaryModeScreen;
import com.example.typemoonaddon.client.ShadowArtModeScreen;
import com.example.typemoonaddon.magic.BoundaryMagicIntegration;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.network.RequestDevourerSelectionPayload;
import com.example.typemoonaddon.registry.AddonEntities;
import com.example.typemoonaddon.registry.AddonFluids;
import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.shadowlogic.client.renderer.ShadowArtRibbonRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final KeyMapping OPEN_SPACE = new KeyMapping(
            "key.typemoonworld.open_space",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            "key.categories.typemoonworld"
    );

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SPACE);
    }

    @SubscribeEvent
    public static void registerEntityLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BlackShadowModel.LAYER_LOCATION, BlackShadowModel::createBodyLayer);
        event.registerLayerDefinition(ShadowFamiliarModel.LAYER_LOCATION, ShadowFamiliarModel::createBodyLayer);
        event.registerLayerDefinition(ShadowFamiliarOutlineModel.LAYER_LOCATION, ShadowFamiliarOutlineModel::createBodyLayer);
        event.registerLayerDefinition(WingedWormModel.LAYER_LOCATION, WingedWormModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AddonEntities.BLACK_SHADOW.get(), BlackShadowRenderer::new);
        event.registerEntityRenderer(AddonEntities.SHADOW_FAMILIAR.get(), ShadowFamiliarRenderer::new);
        event.registerEntityRenderer(AddonEntities.SHADOW_ART_RIBBON.get(), ShadowArtRibbonRenderer::new);
        event.registerEntityRenderer(AddonEntities.SHADOW_PIERCING_RHO_AIAS.get(), ShadowPiercingRhoAiasRenderer::new);
        event.registerEntityRenderer(AddonEntities.GILLES_DE_RAIS_CASTER.get(), GillesDeRaisRenderer::new);
        event.registerEntityRenderer(AddonEntities.GILLES_SEA_MONSTER.get(), SeaMonsterRenderer::new);
        event.registerEntityRenderer(AddonEntities.GILLES_SEA_MONSTER_SPIT.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(AddonEntities.GILLES_HUGE_SEA_MONSTER.get(), HugeSeaMonsterRenderer::new);
        event.registerEntityRenderer(AddonEntities.WORM.get(), WormRenderer::new);
        event.registerEntityRenderer(AddonEntities.WRAITH.get(), SummonedSpiritParticleRenderer::new);
        event.registerEntityRenderer(AddonEntities.EVIL_SPIRIT.get(), SummonedSpiritParticleRenderer::new);
        event.registerEntityRenderer(AddonEntities.EVIL_SPIRIT_SMALL.get(), SummonedSpiritParticleRenderer::new);
        event.registerEntityRenderer(AddonEntities.BOUNDARY_MARK.get(), BoundaryMarkRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private VoidRingRegaliaRenderer renderer;

            @Override
            public Model getGenericArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<?> original
            ) {
                VoidRingRegaliaRenderer currentRenderer = renderer();
                currentRenderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return currentRenderer;
            }

            private VoidRingRegaliaRenderer renderer() {
                if (renderer == null) {
                    renderer = new VoidRingRegaliaRenderer();
                }
                return renderer;
            }
        }, AddonItems.VOID_RING_REGALIA.get());
        event.registerItem(new IClientItemExtensions() {
            private GillesDeRaisArmorRenderer renderer;

            @Override
            public Model getGenericArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<?> original
            ) {
                if (renderer == null) {
                    renderer = new GillesDeRaisArmorRenderer();
                }
                renderer.prepForRender(
                        livingEntity,
                        itemStack,
                        equipmentSlot,
                        original,
                        null,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F
                );
                return renderer;
            }
        }, AddonItems.CURSED_ARMOR_RENDER.get());
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = TypeMoonAddon.id("block/black_mud_still");
            private static final ResourceLocation FLOW = TypeMoonAddon.id("block/black_mud_flow");
            private static final ResourceLocation OVERLAY = TypeMoonAddon.id("textures/misc/black_mud_overlay.png");

            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOW;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(net.minecraft.client.Minecraft minecraft) {
                return OVERLAY;
            }

            @Override
            public Vector3f modifyFogColor(
                    net.minecraft.client.Camera camera,
                    float partialTick,
                    net.minecraft.client.multiplayer.ClientLevel level,
                    int renderDistance,
                    float darkenWorldAmount,
                    Vector3f fluidFogColor
            ) {
                return new Vector3f(0.025F, 0.0F, 0.005F);
            }
        }, AddonFluids.BLACK_MUD_TYPE.get());

        for (var id : BoundaryMagicIntegration.boundaryMagicIds()) {
            TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).client().registerMagicOptions(id, context -> {
                net.minecraft.client.gui.screens.Screen parent = context instanceof net.minecraft.client.gui.screens.Screen screen
                        ? screen : null;
                net.minecraft.client.Minecraft.getInstance().setScreen(new BoundaryMagicOptionsScreen(parent));
            });
        }
        TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).client().registerMagicOptions(SakuraTypeMoonIntegration.IMAGINARY_STORAGE,
                context -> net.minecraft.client.Minecraft.getInstance().setScreen(new ImaginaryModeScreen()));
        TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).client().registerMagicOptions(SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION,
                context -> net.minecraft.client.Minecraft.getInstance().setScreen(new ImaginaryModeScreen()));
        TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).client().registerMagicOptions(SakuraTypeMoonIntegration.BLACK_MUD_CONTROL,
                context -> net.minecraft.client.Minecraft.getInstance().setScreen(new BlackMudControlScreen()));
        TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).client().registerMagicOptions(SakuraTypeMoonIntegration.SUMMON_BLACK_MUD,
                context -> net.minecraft.client.Minecraft.getInstance().setScreen(new BlackMudSummonModeScreen()));
        TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).client().registerMagicOptions(SakuraTypeMoonIntegration.SHADOW_ART,
                context -> net.minecraft.client.Minecraft.getInstance().setScreen(new ShadowArtModeScreen()));
        TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).client().registerMagicOptions(SakuraTypeMoonIntegration.HEROIC_SPIRIT_DEVOURER,
                context -> {
                    net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                    minecraft.setScreen(null);
                    PacketDistributor.sendToServer(RequestDevourerSelectionPayload.INSTANCE);
                });
    }

    @SubscribeEvent
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        for (var entityType : event.getEntityTypes()) {
            addCorruptionLayer(event.getRenderer(entityType));
        }
        for (var skin : event.getSkins()) {
            addCorruptionLayer(event.getSkin(skin));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addCorruptionLayer(EntityRenderer<?> renderer) {
        if (renderer instanceof LivingEntityRenderer livingRenderer) {
            livingRenderer.addLayer(new BlackMudCorruptionLayer(livingRenderer));
            if (livingRenderer.getModel() instanceof HumanoidModel) {
                livingRenderer.addLayer(new CursedArmorLayer(livingRenderer));
            }
        }
    }
}
