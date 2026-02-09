package net.xxxjk.TYPE_MOON_WORLD.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.*;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEventSubscriber {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {

    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private AvalonRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new AvalonRenderer();
                return this.renderer;
            }
        }, ModItems.AVALON.get());

        event.registerItem(new IClientItemExtensions() {
            private RedswordRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new RedswordRenderer();
                return this.renderer;
            }
        }, ModItems.REDSWORD.get());

        event.registerItem(new IClientItemExtensions() {
            private TsumukariMuramasaRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new TsumukariMuramasaRenderer();
                return this.renderer;
            }
        }, ModItems.TSUMUKARI_MURAMASA.get());
    }
    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RUBY_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SAPPHIRE_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.TOPAZ_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.BROKEN_PHANTASM_PROJECTILE.get(), BrokenPhantasmRenderer::new);
        
        event.registerEntityRenderer(ModEntities.UBW_PROJECTILE.get(), UBWProjectileRenderer::new);
        
        event.registerBlockEntityRenderer(ModBlockEntities.REDSWORD_BLOCK_ENTITY.get(), context -> new RedswordBlockRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.UBW_WEAPON_BLOCK_ENTITY.get(), UBWWeaponBlockEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MURAMASA_SLASH.get(), MuramasaSlashProjectileRenderer::new);
    }
}
