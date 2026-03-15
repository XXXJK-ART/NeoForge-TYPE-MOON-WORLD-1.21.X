package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.AvalonRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.BrokenPhantasmRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MuramasaBlockRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MuramasaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MuramasaSlashProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.TsumukariMuramasaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.UBWProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.UBWWeaponBlockEntityRenderer;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ManaSurveyCompassItem;

@EventBusSubscriber(
   modid = "typemoonworld",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class ClientModEventSubscriber {
   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> {
         registerManaSurveyCompassAngle(ModItems.MANA_SURVEY_COMPASS.get());
         registerManaSurveyCompassAngle(ModItems.COPPER_MANA_SURVEY_COMPASS.get());
      });
   }

   private static void registerManaSurveyCompassAngle(Item item) {
      CompassItemPropertyFunction baseCompassProperty = new CompassItemPropertyFunction(
         (level, stack, entity) -> ManaSurveyCompassItem.getStoredTarget(level, stack)
      );
      ItemProperties.register(item, ResourceLocation.withDefaultNamespace("angle"), (stack, level, livingEntity, seed) -> {
         float baseAngle = baseCompassProperty.unclampedCall(stack, level, livingEntity, seed);
         float corrected = baseAngle + 0.5F;
         return corrected >= 1.0F ? corrected - 1.0F : corrected;
      });
   }

   @SubscribeEvent
   public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
      event.registerItem(new IClientItemExtensions() {
         private AvalonRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new AvalonRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.AVALON.get()});
      event.registerItem(new IClientItemExtensions() {
         private MuramasaRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new MuramasaRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.MURAMASA.get()});
      event.registerItem(new IClientItemExtensions() {
         private TsumukariMuramasaRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new TsumukariMuramasaRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.TSUMUKARI_MURAMASA.get()});
      event.registerMobEffect(
         new IClientMobEffectExtensions() {
            private final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/mob_effect/nine_lives.jpg");

            public boolean renderInventoryIcon(
               MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset
            ) {
               guiGraphics.blit(this.ICON, x - 1, y + 6, 0.0F, 0.0F, 18, 18, 18, 18);
               return true;
            }

            public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics, int x, int y, float z, float alpha) {
               guiGraphics.blit(this.ICON, x + 3, y + 2, 0.0F, 0.0F, 18, 18, 18, 18);
               return true;
            }
         },
         new MobEffect[]{(MobEffect)ModMobEffects.NINE_LIVES.get()}
      );
   }

   @SubscribeEvent
   public static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(ModEntities.BROKEN_PHANTASM_PROJECTILE.get(), BrokenPhantasmRenderer::new);
      event.registerEntityRenderer(ModEntities.UBW_PROJECTILE.get(), UBWProjectileRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.MURAMASA_BLOCK_ENTITY.get(), context -> new MuramasaBlockRenderer());
      event.registerBlockEntityRenderer(ModBlockEntities.UBW_WEAPON_BLOCK_ENTITY.get(), UBWWeaponBlockEntityRenderer::new);
      event.registerEntityRenderer(ModEntities.MURAMASA_SLASH.get(), MuramasaSlashProjectileRenderer::new);
   }
}
