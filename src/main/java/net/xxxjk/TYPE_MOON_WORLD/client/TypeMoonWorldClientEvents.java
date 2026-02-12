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
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.UBWProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GemProjectileRenderer;

import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TypeMoonWorldClientEvents {
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Poor Quality - Darker
        event.register((stack, tintIndex) -> 0xFFA0A0A0, 
                ModItems.CARVED_EMERALD_POOR.get(), ModItems.CARVED_EMERALD_POOR_FULL.get(),
                ModItems.CARVED_RUBY_POOR.get(), ModItems.CARVED_RUBY_POOR_FULL.get(),
                ModItems.CARVED_SAPPHIRE_POOR.get(), ModItems.CARVED_SAPPHIRE_POOR_FULL.get(),
                ModItems.CARVED_TOPAZ_POOR.get(), ModItems.CARVED_TOPAZ_POOR_FULL.get(),
                ModBlocks.EMERALD_BLOCK_POOR.get(), ModBlocks.RUBY_BLOCK_POOR.get(),
                ModBlocks.SAPPHIRE_BLOCK_POOR.get(), ModBlocks.TOPAZ_BLOCK_POOR.get(),
                ModBlocks.WHITE_GEMSTONE_BLOCK_POOR.get()
        );

        // Normal Quality - Slightly Darker
        event.register((stack, tintIndex) -> 0xFFE0E0E0, 
                ModItems.CARVED_EMERALD.get(), ModItems.CARVED_EMERALD_FULL.get(),
                ModItems.CARVED_RUBY.get(), ModItems.CARVED_RUBY_FULL.get(),
                ModItems.CARVED_SAPPHIRE.get(), ModItems.CARVED_SAPPHIRE_FULL.get(),
                ModItems.CARVED_TOPAZ.get(), ModItems.CARVED_TOPAZ_FULL.get(),
                ModBlocks.EMERALD_BLOCK.get(), ModBlocks.RUBY_BLOCK.get(),
                ModBlocks.SAPPHIRE_BLOCK.get(), ModBlocks.TOPAZ_BLOCK.get(),
                ModBlocks.WHITE_GEMSTONE_BLOCK.get()
        );

        // High Quality - Brightest (Original Texture)
        event.register((stack, tintIndex) -> 0xFFFFFFFF, 
                ModItems.CARVED_EMERALD_HIGH.get(), ModItems.CARVED_EMERALD_HIGH_FULL.get(),
                ModItems.CARVED_RUBY_HIGH.get(), ModItems.CARVED_RUBY_HIGH_FULL.get(),
                ModItems.CARVED_SAPPHIRE_HIGH.get(), ModItems.CARVED_SAPPHIRE_HIGH_FULL.get(),
                ModItems.CARVED_TOPAZ_HIGH.get(), ModItems.CARVED_TOPAZ_HIGH_FULL.get(),
                ModBlocks.EMERALD_BLOCK_HIGH.get(), ModBlocks.RUBY_BLOCK_HIGH.get(),
                ModBlocks.SAPPHIRE_BLOCK_HIGH.get(), ModBlocks.TOPAZ_BLOCK_HIGH.get(),
                ModBlocks.WHITE_GEMSTONE_BLOCK_HIGH.get()
        );
        
        // White Gemstone (Also apply colors)
        event.register((stack, tintIndex) -> 0xFFA0A0A0, 
                ModItems.CARVED_WHITE_GEMSTONE_POOR.get(), ModItems.CARVED_WHITE_GEMSTONE_POOR_FULL.get());
        event.register((stack, tintIndex) -> 0xFFE0E0E0, 
                ModItems.CARVED_WHITE_GEMSTONE.get(), ModItems.CARVED_WHITE_GEMSTONE_FULL.get());
        event.register((stack, tintIndex) -> 0xFFFFFFFF, 
                ModItems.CARVED_WHITE_GEMSTONE_HIGH.get(), ModItems.CARVED_WHITE_GEMSTONE_HIGH_FULL.get());
    }
    
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
         event.register((state, world, pos, tintIndex) -> 0xFFA0A0A0,
                 ModBlocks.EMERALD_BLOCK_POOR.get(), ModBlocks.RUBY_BLOCK_POOR.get(),
                 ModBlocks.SAPPHIRE_BLOCK_POOR.get(), ModBlocks.TOPAZ_BLOCK_POOR.get(),
                 ModBlocks.WHITE_GEMSTONE_BLOCK_POOR.get());
                 
         event.register((state, world, pos, tintIndex) -> 0xFFE0E0E0,
                 ModBlocks.EMERALD_BLOCK.get(), ModBlocks.RUBY_BLOCK.get(),
                 ModBlocks.SAPPHIRE_BLOCK.get(), ModBlocks.TOPAZ_BLOCK.get(),
                 ModBlocks.WHITE_GEMSTONE_BLOCK.get());
                 
         event.register((state, world, pos, tintIndex) -> 0xFFFFFFFF,
                 ModBlocks.EMERALD_BLOCK_HIGH.get(), ModBlocks.RUBY_BLOCK_HIGH.get(),
                 ModBlocks.SAPPHIRE_BLOCK_HIGH.get(), ModBlocks.TOPAZ_BLOCK_HIGH.get(),
                 ModBlocks.WHITE_GEMSTONE_BLOCK_HIGH.get());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RYOUGI_SHIKI.get(), RyougiShikiRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_BARREL_PROJECTILE.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.SwordBarrelProjectileRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SWORD_BARREL_BLOCK_ENTITY.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.SwordBarrelBlockEntityRenderer::new);
        
        event.registerEntityRenderer(ModEntities.RUBY_PROJECTILE.get(), context -> new GemProjectileRenderer<>(context, 1.0f, 0.0f, 0.0f));
        event.registerEntityRenderer(ModEntities.SAPPHIRE_PROJECTILE.get(), context -> new GemProjectileRenderer<>(context, 0.0f, 0.0f, 1.0f));
        event.registerEntityRenderer(ModEntities.TOPAZ_PROJECTILE.get(), context -> new GemProjectileRenderer<>(context, 1.0f, 1.0f, 0.0f));
        event.registerEntityRenderer(ModEntities.CYAN_WIND_FIELD.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "unlimited_blade_works"), new UBWDimensionEffects());
    }
}
