package io.github.typemoonaddon.client;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.client.model.BlackShadowModel;
import io.github.typemoonaddon.client.model.ShadowFamiliarModel;
import io.github.typemoonaddon.client.model.ShadowFamiliarOutlineModel;
import io.github.typemoonaddon.client.renderer.BlackShadowRenderer;
import io.github.typemoonaddon.client.renderer.BlackMudCorruptionLayer;
import io.github.typemoonaddon.client.renderer.CursedArmorLayer;
import io.github.typemoonaddon.client.renderer.ShadowFamiliarRenderer;
import io.github.typemoonaddon.client.renderer.ShadowPiercingRhoAiasRenderer;
import io.github.typemoonaddon.shadowlogic.client.renderer.ShadowArtRibbonRenderer;
import io.github.typemoonaddon.registry.ModEntities;
import io.github.typemoonaddon.registry.ModFluids;
import io.github.typemoonaddon.registry.ModMenus;
import io.github.typemoonaddon.registry.ModItems;
import io.github.typemoonaddon.client.renderer.VoidRingRegaliaRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public final class ClientModEvents {
    public static final KeyMapping OPEN_SPACE = new KeyMapping(
        "key.typemoonaddon.open_space",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_I,
        "key.categories.typemoonaddon"
    );
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientModEvents::registerKeys);
        modEventBus.addListener(ClientModEvents::registerScreens);
        modEventBus.addListener(ClientModEvents::registerEntityLayers);
        modEventBus.addListener(ClientModEvents::registerEntityRenderers);
        modEventBus.addListener(ClientModEvents::registerClientExtensions);
        modEventBus.addListener(ClientModEvents::addEntityLayers);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SPACE);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.IMAGINARY_SPACE.get(), ImaginarySpaceScreen::new);
    }

    private static void registerEntityLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BlackShadowModel.LAYER_LOCATION, BlackShadowModel::createBodyLayer);
        event.registerLayerDefinition(ShadowFamiliarModel.LAYER_LOCATION, ShadowFamiliarModel::createBodyLayer);
        event.registerLayerDefinition(ShadowFamiliarOutlineModel.LAYER_LOCATION, ShadowFamiliarOutlineModel::createBodyLayer);
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BLACK_SHADOW.get(), BlackShadowRenderer::new);
        event.registerEntityRenderer(ModEntities.SHADOW_FAMILIAR.get(), ShadowFamiliarRenderer::new);
        event.registerEntityRenderer(ModEntities.SHADOW_ART_RIBBON.get(), ShadowArtRibbonRenderer::new);
        event.registerEntityRenderer(ModEntities.SHADOW_PIERCING_RHO_AIAS.get(), ShadowPiercingRhoAiasRenderer::new);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
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
        }, ModItems.VOID_RING_REGALIA.get());
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
        }, ModFluids.BLACK_MUD_TYPE.get());
    }

    private static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
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

    private ClientModEvents() {
    }
}
