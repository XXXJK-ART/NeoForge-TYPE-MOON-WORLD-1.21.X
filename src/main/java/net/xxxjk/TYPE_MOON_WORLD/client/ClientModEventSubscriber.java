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
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.AvalonRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.BrokenPhantasmRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.EmiyaProjectionItemRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.particle.RuneSigilParticle;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GaeBulgRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.HecatesStaffRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MercurySwordRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MuramasaBlockRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MuramasaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RuleBreakerRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RubyStaffRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MuramasaSlashProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.NamelessChainDaggerRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.NamelessBowRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ThompsonContenderRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.TsumukariMuramasaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.UBWProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.UBWWeaponBlockEntityRenderer;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
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
      event.registerItem(new IClientItemExtensions() {
         private GaeBulgRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new GaeBulgRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.GAE_BULG.get()});
      event.registerItem(new IClientItemExtensions() {
         private RuleBreakerRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new RuleBreakerRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.RULE_BREAKER.get()});
      event.registerItem(new IClientItemExtensions() {
         private HecatesStaffRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new HecatesStaffRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.HECATES_STAFF.get()});
      event.registerItem(new IClientItemExtensions() {
         private RubyStaffRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new RubyStaffRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.RUBY_STAFF.get()});
      event.registerItem(new IClientItemExtensions() {
         private MercurySwordRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new MercurySwordRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.MERCURY_SWORD.get()});
      event.registerItem(new IClientItemExtensions() {
         private ThompsonContenderRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new ThompsonContenderRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.THOMPSON_CONTENDER.get()});
      event.registerItem(new IClientItemExtensions() {
         private NamelessChainDaggerRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new NamelessChainDaggerRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.NAMELESS_CHAIN_DAGGER.get()});
      event.registerItem(new IClientItemExtensions() {
         private EmiyaProjectionItemRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new EmiyaProjectionItemRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{
         ModItems.GAN_JIANG.get(),
         ModItems.MO_YE.get(),
         ModItems.GAN_JIANG_OVEREDGE.get(),
         ModItems.MO_YE_OVEREDGE.get(),
         ModItems.PSEUDO_SPIRAL_SWORD.get(),
         ModItems.CRIMSON_HOUND.get(),
         ModItems.EXCALIBUR_GALLATIN.get(),
         ModItems.RHO_AIAS.get(),
         ModItems.UBW_METAL_1.get(),
         ModItems.UBW_METAL_2.get(),
         ModItems.UBW_METAL_3.get()
      });
      event.registerItem(new IClientItemExtensions() {
         private NamelessBowRenderer renderer;

         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new NamelessBowRenderer();
            }

            return this.renderer;
         }
      }, new Item[]{ModItems.NAMELESS_BOW.get()});
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

   @SubscribeEvent
   public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(ModParticles.ANSUZ_RUNE.get(), sprite -> new RuneSigilParticle.Provider(sprite, 1.0F, 0.35F, 0.18F, 1.8F, 20));
      event.registerSpriteSet(ModParticles.LAGUZ_RUNE.get(), sprite -> new RuneSigilParticle.Provider(sprite, 0.35F, 0.75F, 1.0F, 1.75F, 22));
      event.registerSpriteSet(ModParticles.TIWAZ_RUNE.get(), sprite -> new RuneSigilParticle.Provider(sprite, 1.0F, 0.88F, 0.42F, 1.95F, 24));
      event.registerSpriteSet(ModParticles.ALGIZ_RUNE.get(), sprite -> new RuneSigilParticle.Provider(sprite, 0.78F, 0.55F, 1.0F, 1.95F, 24));
      event.registerSpriteSet(ModParticles.BERKANA_RUNE.get(), sprite -> new RuneSigilParticle.Provider(sprite, 0.42F, 1.0F, 0.48F, 1.95F, 24));
      event.registerSpriteSet(ModParticles.RUNE_BARRIER.get(), sprite -> new RuneSigilParticle.Provider(sprite, 0.30F, 0.72F, 1.0F, 0.55F, 14, 0.28F));
      event.registerSpriteSet(ModParticles.FEHU_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.URUZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.THURISAZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.RAIDHO_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.KENAZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.GEBO_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.WUNJO_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.HAGALAZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.NAUTHIZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.ISA_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.JERA_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.EIHWAZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.PERTHRO_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.SOWILO_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.BERKANO_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.EHWAZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.MANNAZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.INGWAZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.DAGAZ_RUNE.get(), sprite -> rune(sprite));
      event.registerSpriteSet(ModParticles.OTHALA_RUNE.get(), sprite -> rune(sprite));
   }

   private static RuneSigilParticle.Provider rune(net.minecraft.client.particle.SpriteSet sprite) {
      return new RuneSigilParticle.Provider(sprite, 1.0F, 0.3F, 0.15F, 1.8F, 20);
   }
}
