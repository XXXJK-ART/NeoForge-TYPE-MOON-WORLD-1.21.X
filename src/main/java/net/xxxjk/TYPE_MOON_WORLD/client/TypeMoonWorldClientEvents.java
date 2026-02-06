package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.world.UBWDimensionEffects;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RyougiShikiRenderer;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TypeMoonWorldClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RYOUGI_SHIKI.get(), RyougiShikiRenderer::new);
    }

    @SubscribeEvent
    public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "unlimited_blade_works"), new UBWDimensionEffects());
    }
}
