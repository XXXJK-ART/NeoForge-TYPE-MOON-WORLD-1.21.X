package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ExpandingRingEffectRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GravityFieldShellRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GanderOrbShaderRegistry;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GanderProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GemProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GravityShellRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MerlinRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MysticMagicianRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RyougiShikiRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.StoneManRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.SwordBarrelBlockEntityRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.SwordBarrelProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.TsumukariWaveProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.TsumukariLightColumnEffectRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.TypeMoonEffectShaders;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.UbwChantRippleRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ProjectionCircuitEffectRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.world.UBWDimensionEffects;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

@EventBusSubscriber(
   modid = "typemoonworld",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class TypeMoonWorldClientEvents {
   @SubscribeEvent
   public static void registerItemColors(Item event) {
      event.register(
         (stack, tintIndex) -> -6250336,
         new ItemLike[]{
            ModItems.CARVED_EMERALD_POOR.get(),
            ModItems.CARVED_EMERALD_POOR_FULL.get(),
            ModItems.CARVED_RUBY_POOR.get(),
            ModItems.CARVED_RUBY_POOR_FULL.get(),
            ModItems.CARVED_SAPPHIRE_POOR.get(),
            ModItems.CARVED_SAPPHIRE_POOR_FULL.get(),
            ModItems.CARVED_TOPAZ_POOR.get(),
            ModItems.CARVED_TOPAZ_POOR_FULL.get(),
            ModItems.CARVED_BLACK_SHARD_POOR.get(),
            ModItems.CARVED_BLACK_SHARD_POOR_FULL.get(),
            (ItemLike)ModBlocks.EMERALD_BLOCK_POOR.get(),
            (ItemLike)ModBlocks.RUBY_BLOCK_POOR.get(),
            (ItemLike)ModBlocks.SAPPHIRE_BLOCK_POOR.get(),
            (ItemLike)ModBlocks.TOPAZ_BLOCK_POOR.get(),
            (ItemLike)ModBlocks.WHITE_GEMSTONE_BLOCK_POOR.get()
         }
      );
      event.register(
         (stack, tintIndex) -> -2039584,
         new ItemLike[]{
            ModItems.CARVED_EMERALD.get(),
            ModItems.CARVED_EMERALD_FULL.get(),
            ModItems.CARVED_RUBY.get(),
            ModItems.CARVED_RUBY_FULL.get(),
            ModItems.CARVED_SAPPHIRE.get(),
            ModItems.CARVED_SAPPHIRE_FULL.get(),
            ModItems.CARVED_TOPAZ.get(),
            ModItems.CARVED_TOPAZ_FULL.get(),
            ModItems.CARVED_BLACK_SHARD.get(),
            ModItems.CARVED_BLACK_SHARD_FULL.get(),
            (ItemLike)ModBlocks.EMERALD_BLOCK.get(),
            (ItemLike)ModBlocks.RUBY_BLOCK.get(),
            (ItemLike)ModBlocks.SAPPHIRE_BLOCK.get(),
            (ItemLike)ModBlocks.TOPAZ_BLOCK.get(),
            (ItemLike)ModBlocks.WHITE_GEMSTONE_BLOCK.get()
         }
      );
      event.register(
         (stack, tintIndex) -> -1,
         new ItemLike[]{
            ModItems.CARVED_EMERALD_HIGH.get(),
            ModItems.CARVED_EMERALD_HIGH_FULL.get(),
            ModItems.CARVED_RUBY_HIGH.get(),
            ModItems.CARVED_RUBY_HIGH_FULL.get(),
            ModItems.CARVED_SAPPHIRE_HIGH.get(),
            ModItems.CARVED_SAPPHIRE_HIGH_FULL.get(),
            ModItems.CARVED_TOPAZ_HIGH.get(),
            ModItems.CARVED_TOPAZ_HIGH_FULL.get(),
            ModItems.CARVED_BLACK_SHARD_HIGH.get(),
            ModItems.CARVED_BLACK_SHARD_HIGH_FULL.get(),
            (ItemLike)ModBlocks.EMERALD_BLOCK_HIGH.get(),
            (ItemLike)ModBlocks.RUBY_BLOCK_HIGH.get(),
            (ItemLike)ModBlocks.SAPPHIRE_BLOCK_HIGH.get(),
            (ItemLike)ModBlocks.TOPAZ_BLOCK_HIGH.get(),
            (ItemLike)ModBlocks.WHITE_GEMSTONE_BLOCK_HIGH.get()
         }
      );
      event.register(
         (stack, tintIndex) -> -6250336,
         new ItemLike[]{ModItems.CARVED_WHITE_GEMSTONE_POOR.get(), ModItems.CARVED_WHITE_GEMSTONE_POOR_FULL.get()}
      );
      event.register(
         (stack, tintIndex) -> -2039584, new ItemLike[]{ModItems.CARVED_WHITE_GEMSTONE.get(), ModItems.CARVED_WHITE_GEMSTONE_FULL.get()}
      );
      event.register(
         (stack, tintIndex) -> -1,
         new ItemLike[]{ModItems.CARVED_WHITE_GEMSTONE_HIGH.get(), ModItems.CARVED_WHITE_GEMSTONE_HIGH_FULL.get()}
      );
   }

   @SubscribeEvent
   public static void registerBlockColors(Block event) {
      event.register(
         (state, world, pos, tintIndex) -> -6250336,
         new net.minecraft.world.level.block.Block[]{
            (net.minecraft.world.level.block.Block)ModBlocks.EMERALD_BLOCK_POOR.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.RUBY_BLOCK_POOR.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.SAPPHIRE_BLOCK_POOR.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.TOPAZ_BLOCK_POOR.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.WHITE_GEMSTONE_BLOCK_POOR.get()
         }
      );
      event.register(
         (state, world, pos, tintIndex) -> -2039584,
         new net.minecraft.world.level.block.Block[]{
            (net.minecraft.world.level.block.Block)ModBlocks.EMERALD_BLOCK.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.RUBY_BLOCK.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.SAPPHIRE_BLOCK.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.TOPAZ_BLOCK.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.WHITE_GEMSTONE_BLOCK.get()
         }
      );
      event.register(
         (state, world, pos, tintIndex) -> -1,
         new net.minecraft.world.level.block.Block[]{
            (net.minecraft.world.level.block.Block)ModBlocks.EMERALD_BLOCK_HIGH.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.RUBY_BLOCK_HIGH.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.SAPPHIRE_BLOCK_HIGH.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.TOPAZ_BLOCK_HIGH.get(),
            (net.minecraft.world.level.block.Block)ModBlocks.WHITE_GEMSTONE_BLOCK_HIGH.get()
         }
      );
   }

   @SubscribeEvent
   @SuppressWarnings("unchecked")
   public static void registerRenderers(RegisterRenderers event) {
      event.registerEntityRenderer(ModEntities.RYOUGI_SHIKI.get(), RyougiShikiRenderer::new);
      event.registerEntityRenderer(ModEntities.MERLIN.get(), MerlinRenderer::new);
      event.registerEntityRenderer(ModEntities.STONE_MAN.get(), StoneManRenderer::new);
      event.registerEntityRenderer(ModEntities.MYSTIC_MAGICIAN.get(), MysticMagicianRenderer::new);
      event.registerEntityRenderer(ModEntities.SWORD_BARREL_PROJECTILE.get(), SwordBarrelProjectileRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.SWORD_BARREL_BLOCK_ENTITY.get(), SwordBarrelBlockEntityRenderer::new);
      event.registerEntityRenderer(ModEntities.RUBY_PROJECTILE.get(), context -> new GemProjectileRenderer(context, 1.0F, 0.0F, 0.0F));
      event.registerEntityRenderer(ModEntities.SAPPHIRE_PROJECTILE.get(), context -> new GemProjectileRenderer(context, 0.0F, 0.0F, 1.0F));
      event.registerEntityRenderer(ModEntities.TOPAZ_PROJECTILE.get(), context -> new GemProjectileRenderer(context, 1.0F, 1.0F, 0.0F));
      event.registerEntityRenderer(ModEntities.GANDER_PROJECTILE.get(), GanderProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.CYAN_WIND_FIELD.get(), NoopRenderer::new);
      event.registerEntityRenderer(ModEntities.GRAVITY_SHELL_EFFECT.get(), GravityShellRenderer::new);
      event.registerEntityRenderer(ModEntities.GRAVITY_FIELD_SHELL_EFFECT.get(), GravityFieldShellRenderer::new);
      event.registerEntityRenderer(ModEntities.EXPANDING_RING_EFFECT.get(), ExpandingRingEffectRenderer::new);
      event.registerEntityRenderer(ModEntities.PROJECTION_CIRCUIT_EFFECT.get(), ProjectionCircuitEffectRenderer::new);
      event.registerEntityRenderer(ModEntities.UBW_CHANT_RIPPLE.get(), UbwChantRippleRenderer::new);
      event.registerEntityRenderer(ModEntities.TSUMUKARI_WAVE_PROJECTILE.get(), TsumukariWaveProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.TSUMUKARI_LIGHT_COLUMN_EFFECT.get(), TsumukariLightColumnEffectRenderer::new);
   }

   @SubscribeEvent
   public static void registerShaders(RegisterShadersEvent event) throws IOException {
      ShaderInstance shader = new ShaderInstance(
         event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gander_orb"), DefaultVertexFormat.POSITION_TEX_COLOR
      );
      event.registerShader(shader, GanderOrbShaderRegistry::setShader);
      event.registerShader(
         new ShaderInstance(
            event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gravity_shell"), DefaultVertexFormat.POSITION_TEX_COLOR
         ),
         TypeMoonEffectShaders::setGravityShell
      );
      event.registerShader(
         new ShaderInstance(
            event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "expanding_ring"), DefaultVertexFormat.POSITION_TEX_COLOR
         ),
         TypeMoonEffectShaders::setExpandingRing
      );
      event.registerShader(
         new ShaderInstance(
            event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ubw_analysis_ripple"), DefaultVertexFormat.POSITION_TEX_COLOR
         ),
         TypeMoonEffectShaders::setUbwAnalysisRipple
      );
   }

   @SubscribeEvent
   public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
      event.register(ResourceLocation.fromNamespaceAndPath("typemoonworld", "unlimited_blade_works"), new UBWDimensionEffects());
   }
}
