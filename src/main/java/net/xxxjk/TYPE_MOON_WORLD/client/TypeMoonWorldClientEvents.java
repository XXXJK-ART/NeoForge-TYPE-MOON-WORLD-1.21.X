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
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ExpandingRingEffectRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ArtoriaExcaliburBeamRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ArtoriaPendragonRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GravityFieldShellRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GanderOrbShaderRegistry;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GanderProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GaeBulgProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GawainRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GilgameshRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GilgameshGateWeaponRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GilgameshCrossSlashRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GilgameshEaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GemProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.GravityShellRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.CuChulainnRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.CursedArmHassanRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.DirkProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.DragonfangSoldierRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.EmiyaArcherRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ArashRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.EnkiduRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ChainsOfHeavenBindingRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ContenderBulletRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.BlackKeyProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.HeraclesRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MerlinRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MedeaBeamEffectRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ElementalMagicProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MagicBulletProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MedeaMagicBoltRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MedeaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MedusaPegasusRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MedusaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MysticMagicianRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.LiShuwenRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ParacelsusRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RatSwarmRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.PaleRiderCrowRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ShadowHassanRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ShadowHassanDeathShadowRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.FanaticAssassinRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.SoulEchoRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ApocalypseHorseRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ConceptSwordRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ParacelsusSpiritCannonRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.OdaMatchlockGunRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.OdaMatchlockBulletRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.OdaNobunagaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RyougiShikiRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RedSkeletonHajunRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.SasakiKojiroRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.StoneManRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.SwordBarrelBlockEntityRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.SwordBarrelProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.TsumukariWaveProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.TsumukariLightColumnEffectRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.TypeMoonEffectShaders;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.UBWInterceptorSwordRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.UbwChantRippleRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ProjectionCircuitEffectRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ProjectedItemProjectileRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RhoAiasEntityRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.VFXTriggerRenderer;
import net.xxxjk.TYPE_MOON_WORLD.client.world.HajunDimensionEffects;
import net.xxxjk.TYPE_MOON_WORLD.client.world.UBWDimensionEffects;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.UbwSkyGearEntityRenderer;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.magic.data.MagicDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantSkillDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiDefinitionLoader;

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
   public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener(new ServantDefinitionLoader());
      event.registerReloadListener(new ServantCardDefinitionLoader());
      event.registerReloadListener(new MagicDefinitionLoader());
      event.registerReloadListener(new ServantSkillDefinitionLoader());
      event.registerReloadListener(new ServantNoblePhantasmDefinitionLoader());
      event.registerReloadListener(new ServantAiDefinitionLoader());
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
      event.registerEntityRenderer(ModEntities.VFX_TRIGGER.get(), VFXTriggerRenderer::new);
      event.registerEntityRenderer(ModEntities.RYOUGI_SHIKI.get(), RyougiShikiRenderer::new);
      event.registerEntityRenderer(ModEntities.MERLIN.get(), MerlinRenderer::new);
      event.registerEntityRenderer(ModEntities.STONE_MAN.get(), StoneManRenderer::new);
      event.registerEntityRenderer(ModEntities.MYSTIC_MAGICIAN.get(), MysticMagicianRenderer::new);
      event.registerEntityRenderer(ModEntities.THE_DEAD.get(), context -> new net.xxxjk.TYPE_MOON_WORLD.client.renderer.DeadApostleRenderer<>(context, "the_dead"));
      event.registerEntityRenderer(ModEntities.GHOUL.get(), context -> new net.xxxjk.TYPE_MOON_WORLD.client.renderer.DeadApostleRenderer<>(context, "ghoul"));
      event.registerEntityRenderer(ModEntities.LIVING_DEAD.get(), context -> new net.xxxjk.TYPE_MOON_WORLD.client.renderer.DeadApostleRenderer<>(context, "living_dead"));
      event.registerEntityRenderer(ModEntities.NIGHT_KIN.get(), context -> new net.xxxjk.TYPE_MOON_WORLD.client.renderer.DeadApostleRenderer<>(context, "night_kin"));
      event.registerEntityRenderer(ModEntities.CHURCH_EXECUTOR.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.ChurchExecutorRenderer::new);
      event.registerEntityRenderer(ModEntities.BAJIQUAN_MASTER.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.BajiquanMasterRenderer::new);
      event.registerEntityRenderer(ModEntities.BAJIQUAN_APPRENTICE.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.BajiquanApprenticeRenderer::new);
      event.registerEntityRenderer(ModEntities.MYSTERIOUS_SWORDSMAN.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.MysteriousSwordsmanRenderer::new);
      event.registerEntityRenderer(ModEntities.KENDO_MASTER.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.KendoMasterRenderer::new);
      event.registerEntityRenderer(ModEntities.KENDO_APPRENTICE.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.KendoApprenticeRenderer::new);
      event.registerEntityRenderer(ModEntities.RONIN.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.RoninRenderer::new);
      event.registerEntityRenderer(ModEntities.SHINSENGUMI.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.ShinsengumiRenderer::new);
      event.registerEntityRenderer(ModEntities.TOHSAKA_RIN.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.TohsakaRinRenderer::new);
      event.registerEntityRenderer(ModEntities.HERACLES.get(), HeraclesRenderer::new);
      event.registerEntityRenderer(ModEntities.GENERIC_SERVANT.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.GenericServantRenderer::new);
      event.registerEntityRenderer(ModEntities.SASAKI_KOJIRO.get(), SasakiKojiroRenderer::new);
      event.registerEntityRenderer(ModEntities.CU_CHULAINN.get(), CuChulainnRenderer::new);
      event.registerEntityRenderer(ModEntities.MEDEA.get(), MedeaRenderer::new);
      event.registerEntityRenderer(ModEntities.MEDUSA.get(), MedusaRenderer::new);
      event.registerEntityRenderer(ModEntities.CURSED_ARM_HASSAN.get(), CursedArmHassanRenderer::new);
      event.registerEntityRenderer(ModEntities.EMIYA_ARCHER.get(), EmiyaArcherRenderer::new);
      event.registerEntityRenderer(ModEntities.ARASH.get(), ArashRenderer::new);
      event.registerEntityRenderer(ModEntities.ARASH_PARTICLE_ARROW.get(), NoopRenderer::new);
      event.registerEntityRenderer(ModEntities.ARASH_STELLA_CONTROLLER.get(), NoopRenderer::new);
      event.registerEntityRenderer(ModEntities.ARTORIA_PENDRAGON.get(), ArtoriaPendragonRenderer::new);
      event.registerEntityRenderer(ModEntities.ODA_NOBUNAGA.get(), OdaNobunagaRenderer::new);
      event.registerEntityRenderer(ModEntities.USHIWAKAMARU_RIDER.get(), net.xxxjk.TYPE_MOON_WORLD.client.renderer.UshiwakamaruRiderRenderer::new);
      event.registerEntityRenderer(ModEntities.ENKIDU.get(), EnkiduRenderer::new);
      event.registerEntityRenderer(ModEntities.GILGAMESH.get(), GilgameshRenderer::new);
      event.registerEntityRenderer(ModEntities.GAWAIN.get(), GawainRenderer::new);
      event.registerEntityRenderer(ModEntities.LI_SHUWEN.get(), LiShuwenRenderer::new);
      event.registerEntityRenderer(ModEntities.PARACELSUS.get(), ParacelsusRenderer::new);
      event.registerEntityRenderer(ModEntities.PALE_RIDER.get(), NoopRenderer::new);
      event.registerEntityRenderer(ModEntities.SHADOW_HASSAN.get(), ShadowHassanRenderer::new);
      event.registerEntityRenderer(ModEntities.SHADOW_HASSAN_DEATH_SHADOW.get(), ShadowHassanDeathShadowRenderer::new);
      event.registerEntityRenderer(ModEntities.FANATIC_ASSASSIN.get(), FanaticAssassinRenderer::new);
      event.registerEntityRenderer(ModEntities.FANATIC_ASSASSIN_JINN.get(), NoopRenderer::new);
      event.registerEntityRenderer(ModEntities.RAT_SWARM.get(), RatSwarmRenderer::new);
      event.registerEntityRenderer(ModEntities.PALE_RIDER_CROW.get(), PaleRiderCrowRenderer::new);
      event.registerEntityRenderer(ModEntities.SOUL_ECHO.get(), SoulEchoRenderer::new);
      event.registerEntityRenderer(ModEntities.APOCALYPSE_HORSEMAN.get(), NoopRenderer::new);
      event.registerEntityRenderer(ModEntities.APOCALYPSE_HORSE.get(), ApocalypseHorseRenderer::new);
      event.registerEntityRenderer(ModEntities.CONCEPT_SWORD.get(), ConceptSwordRenderer::new);
      event.registerEntityRenderer(ModEntities.PARACELSUS_SPIRIT_CANNON.get(), ParacelsusSpiritCannonRenderer::new);
      event.registerEntityRenderer(ModEntities.ODA_MATCHLOCK_GUN.get(), OdaMatchlockGunRenderer::new);
      event.registerEntityRenderer(ModEntities.ODA_MATCHLOCK_BULLET.get(), OdaMatchlockBulletRenderer::new);
      event.registerEntityRenderer(ModEntities.CONTENDER_BULLET.get(), ContenderBulletRenderer::new);
      event.registerEntityRenderer(ModEntities.BLACK_KEY_PROJECTILE.get(), BlackKeyProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.RED_SKELETON_HAJUN.get(), RedSkeletonHajunRenderer::new);
      event.registerEntityRenderer(ModEntities.CHAINS_OF_HEAVEN_BINDING.get(), ChainsOfHeavenBindingRenderer::new);
      event.registerEntityRenderer(ModEntities.MEDUSA_PEGASUS.get(), MedusaPegasusRenderer::new);
      event.registerEntityRenderer(ModEntities.DRAGONFANG_SOLDIER.get(), DragonfangSoldierRenderer::new);
      event.registerEntityRenderer(ModEntities.SWORD_BARREL_PROJECTILE.get(), SwordBarrelProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.UBW_INTERCEPTOR_SWORD.get(), UBWInterceptorSwordRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.SWORD_BARREL_BLOCK_ENTITY.get(), SwordBarrelBlockEntityRenderer::new);
      event.registerEntityRenderer(ModEntities.RUBY_PROJECTILE.get(), context -> new GemProjectileRenderer(context, 1.0F, 0.0F, 0.0F));
      event.registerEntityRenderer(ModEntities.SAPPHIRE_PROJECTILE.get(), context -> new GemProjectileRenderer(context, 0.0F, 0.0F, 1.0F));
      event.registerEntityRenderer(ModEntities.TOPAZ_PROJECTILE.get(), context -> new GemProjectileRenderer(context, 1.0F, 1.0F, 0.0F));
      event.registerEntityRenderer(ModEntities.GANDER_PROJECTILE.get(), GanderProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.MAGIC_BULLET_PROJECTILE.get(), MagicBulletProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.ELEMENTAL_MAGIC_PROJECTILE.get(), ElementalMagicProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.ELEMENTAL_MAGIC_FIELD.get(), NoopRenderer::new);
      event.registerEntityRenderer(ModEntities.MEDEA_MAGIC_BOLT.get(), MedeaMagicBoltRenderer::new);
      event.registerEntityRenderer(ModEntities.MEDEA_BEAM_EFFECT.get(), MedeaBeamEffectRenderer::new);
      event.registerEntityRenderer(ModEntities.ARTORIA_EXCALIBUR_BEAM.get(), ArtoriaExcaliburBeamRenderer::new);
      event.registerEntityRenderer(ModEntities.GILGAMESH_GATE_PROJECTILE.get(), GilgameshGateWeaponRenderer::new);
      event.registerEntityRenderer(ModEntities.GILGAMESH_CROSS_SLASH.get(), GilgameshCrossSlashRenderer::new);
      event.registerEntityRenderer(ModEntities.GILGAMESH_EA_BEAM.get(), GilgameshEaRenderer::new);
      event.registerEntityRenderer(ModEntities.CYAN_WIND_FIELD.get(), NoopRenderer::new);
      event.registerEntityRenderer(ModEntities.GRAVITY_SHELL_EFFECT.get(), GravityShellRenderer::new);
      event.registerEntityRenderer(ModEntities.GRAVITY_FIELD_SHELL_EFFECT.get(), GravityFieldShellRenderer::new);
      event.registerEntityRenderer(ModEntities.EXPANDING_RING_EFFECT.get(), ExpandingRingEffectRenderer::new);
      event.registerEntityRenderer(ModEntities.PROJECTION_CIRCUIT_EFFECT.get(), ProjectionCircuitEffectRenderer::new);
      event.registerEntityRenderer(ModEntities.UBW_CHANT_RIPPLE.get(), UbwChantRippleRenderer::new);
      event.registerEntityRenderer(ModEntities.TSUMUKARI_WAVE_PROJECTILE.get(), TsumukariWaveProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.TSUMUKARI_LIGHT_COLUMN_EFFECT.get(), TsumukariLightColumnEffectRenderer::new);
      event.registerEntityRenderer(ModEntities.GAE_BULG_PROJECTILE.get(), GaeBulgProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.GAE_BULG_ARMY_PROJECTILE.get(), GaeBulgProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.DIRK_PROJECTILE.get(), DirkProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.EMIYA_THROWN_WEAPON.get(), ProjectedItemProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.ENKIDU_EARTH_WEAPON.get(), ProjectedItemProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.EMIYA_ARROW_ORB.get(), NoopRenderer::new);
      event.registerEntityRenderer(ModEntities.CRIMSON_HOUND_PROJECTILE.get(), ProjectedItemProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.PSEUDO_SPIRAL_SWORD_PROJECTILE.get(), ProjectedItemProjectileRenderer::new);
      event.registerEntityRenderer(ModEntities.RHO_AIAS_SHIELD.get(), RhoAiasEntityRenderer::new);
      event.registerEntityRenderer(ModEntities.UBW_SKY_GEAR.get(), UbwSkyGearEntityRenderer::new);
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
      event.register(ResourceLocation.fromNamespaceAndPath("typemoonworld", "unlimited_blade_works_emiya"), new UBWDimensionEffects());
      event.register(ResourceLocation.fromNamespaceAndPath("typemoonworld", "dairokuten_maou_hajun"), new HajunDimensionEffects());
   }
}
