package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.TexturingStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

public class ReinforcementRenderType extends RenderType {
   private static final ResourceLocation REINFORCEMENT_ITEM_GLINT = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "textures/misc/enchanted_item_glint_typemoon.png"
   );
   private static final ResourceLocation KNIGHT_OF_OWNER_ITEM_GLINT = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "textures/misc/knight_of_owner_glint.png"
   );
   private static final ResourceLocation SKIN_TEXTURE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/models/armor/magic_circuit_skin.png");
   private static final ResourceLocation SKIN_TEXTURE_DANGER = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "textures/models/armor/magic_circuit_skin2.png"
   );
   private static final TexturingStateShard REINFORCEMENT_ITEM_GLINT_TEXTURING = new TexturingStateShard(
      "reinforcement_item_glint_texturing", () -> setupReinforcementGlintTexturing(20.0F, -45.0F, 520000L, 180000L), RenderSystem::resetTextureMatrix
   );
   private static final TexturingStateShard REINFORCEMENT_ENTITY_GLINT_TEXTURING = new TexturingStateShard(
      "reinforcement_entity_glint_texturing", () -> setupReinforcementGlintTexturing(0.16F, -45.0F, 620000L, 220000L), RenderSystem::resetTextureMatrix
   );
   private static final TexturingStateShard REINFORCEMENT_ITEM_GLINT_TEXTURING_3D = new TexturingStateShard(
      "reinforcement_item_glint_texturing_3d", () -> setupReinforcementGlintTexturing(0.8F, -45.0F, 520000L, 180000L), RenderSystem::resetTextureMatrix
   );
   private static final TexturingStateShard REINFORCEMENT_ITEM_GLINT_TEXTURING_BLOCK = new TexturingStateShard(
      "reinforcement_item_glint_texturing_block", () -> setupReinforcementGlintTexturing(10.0F, -45.0F, 520000L, 180000L), RenderSystem::resetTextureMatrix
   );
   private static final TexturingStateShard KNIGHT_OF_OWNER_STATIC_TEXTURING = new TexturingStateShard(
      "knight_of_owner_static_texturing", () -> setupStaticGlintTexturing(1.0F, -45.0F), RenderSystem::resetTextureMatrix
   );
   private static final TexturingStateShard KNIGHT_OF_OWNER_STATIC_TEXTURING_3D = new TexturingStateShard(
      "knight_of_owner_static_texturing_3d", () -> setupStaticGlintTexturing(0.8F, -45.0F), RenderSystem::resetTextureMatrix
   );
   private static final TexturingStateShard KNIGHT_OF_OWNER_STATIC_TEXTURING_BLOCK = new TexturingStateShard(
      "knight_of_owner_static_texturing_block", () -> setupStaticGlintTexturing(10.0F, -45.0F), RenderSystem::resetTextureMatrix
   );
   private static final RenderType REINFORCEMENT_GLINT_TRANSLUCENT = create(
      "reinforcement_glint_translucent",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING)
         .setOutputState(ITEM_ENTITY_TARGET)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_GLINT = create(
      "reinforcement_glint",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_GLINT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_ENTITY_GLINT = create(
      "reinforcement_entity_glint",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setOutputState(ITEM_ENTITY_TARGET)
         .setTexturingState(REINFORCEMENT_ENTITY_GLINT_TEXTURING)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_ENTITY_GLINT_DIRECT = create(
      "reinforcement_entity_glint_direct",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ENTITY_GLINT_TEXTURING)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_GLINT_DIRECT = create(
      "reinforcement_glint_direct",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_SKIN_EMISSIVE = create(
      "reinforcement_skin_emissive",
      DefaultVertexFormat.NEW_ENTITY,
      Mode.QUADS,
      256,
      true,
      false,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
         .setTextureState(new TextureStateShard(SKIN_TEXTURE, false, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setLightmapState(LIGHTMAP)
         .setOverlayState(OVERLAY)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_SKIN_EMISSIVE_DANGER = create(
      "reinforcement_skin_emissive_danger",
      DefaultVertexFormat.NEW_ENTITY,
      Mode.QUADS,
      256,
      true,
      false,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
         .setTextureState(new TextureStateShard(SKIN_TEXTURE_DANGER, false, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setLightmapState(LIGHTMAP)
         .setOverlayState(OVERLAY)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_GLINT_TRANSLUCENT_3D = create(
      "reinforcement_glint_translucent_3d",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_3D)
         .setOutputState(ITEM_ENTITY_TARGET)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_GLINT_3D = create(
      "reinforcement_glint_3d",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_GLINT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_3D)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_ENTITY_GLINT_3D = create(
      "reinforcement_entity_glint_3d",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setOutputState(ITEM_ENTITY_TARGET)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_3D)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_GLINT_DIRECT_3D = create(
      "reinforcement_glint_direct_3d",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_3D)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_ENTITY_GLINT_DIRECT_3D = create(
      "reinforcement_entity_glint_direct_3d",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_3D)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_GLINT_TRANSLUCENT_BLOCK = create(
      "reinforcement_glint_translucent_block",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_BLOCK)
         .setOutputState(ITEM_ENTITY_TARGET)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_GLINT_BLOCK = create(
      "reinforcement_glint_block",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_GLINT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_BLOCK)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_ENTITY_GLINT_BLOCK = create(
      "reinforcement_entity_glint_block",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setOutputState(ITEM_ENTITY_TARGET)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_BLOCK)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_GLINT_DIRECT_BLOCK = create(
      "reinforcement_glint_direct_block",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_BLOCK)
         .createCompositeState(false)
   );
   private static final RenderType REINFORCEMENT_ENTITY_GLINT_DIRECT_BLOCK = create(
      "reinforcement_entity_glint_direct_block",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      1536,
      CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
         .setTextureState(new TextureStateShard(REINFORCEMENT_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(ADDITIVE_TRANSPARENCY)
         .setTexturingState(REINFORCEMENT_ITEM_GLINT_TEXTURING_BLOCK)
         .createCompositeState(false)
   );
   private static final RenderType KNIGHT_OF_OWNER_GLINT_TRANSLUCENT = knightOfOwner("knight_of_owner_glint_translucent", RENDERTYPE_GLINT_TRANSLUCENT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING, true, false);
   private static final RenderType KNIGHT_OF_OWNER_GLINT = knightOfOwner("knight_of_owner_glint", RENDERTYPE_GLINT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING, false, false);
   private static final RenderType KNIGHT_OF_OWNER_ENTITY_GLINT = knightOfOwner("knight_of_owner_entity_glint", RENDERTYPE_ENTITY_GLINT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING, true, true);
   private static final RenderType KNIGHT_OF_OWNER_ENTITY_GLINT_DIRECT = knightOfOwner("knight_of_owner_entity_glint_direct", RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING, false, true);
   private static final RenderType KNIGHT_OF_OWNER_GLINT_DIRECT = knightOfOwner("knight_of_owner_glint_direct", RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING, false, false);
   private static final RenderType KNIGHT_OF_OWNER_GLINT_TRANSLUCENT_3D = knightOfOwner("knight_of_owner_glint_translucent_3d", RENDERTYPE_GLINT_TRANSLUCENT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_3D, true, false);
   private static final RenderType KNIGHT_OF_OWNER_GLINT_3D = knightOfOwner("knight_of_owner_glint_3d", RENDERTYPE_GLINT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_3D, false, false);
   private static final RenderType KNIGHT_OF_OWNER_ENTITY_GLINT_3D = knightOfOwner("knight_of_owner_entity_glint_3d", RENDERTYPE_ENTITY_GLINT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_3D, true, true);
   private static final RenderType KNIGHT_OF_OWNER_GLINT_DIRECT_3D = knightOfOwner("knight_of_owner_glint_direct_3d", RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_3D, false, false);
   private static final RenderType KNIGHT_OF_OWNER_ENTITY_GLINT_DIRECT_3D = knightOfOwner("knight_of_owner_entity_glint_direct_3d", RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_3D, false, true);
   private static final RenderType KNIGHT_OF_OWNER_GLINT_TRANSLUCENT_BLOCK = knightOfOwner("knight_of_owner_glint_translucent_block", RENDERTYPE_GLINT_TRANSLUCENT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_BLOCK, true, false);
   private static final RenderType KNIGHT_OF_OWNER_GLINT_BLOCK = knightOfOwner("knight_of_owner_glint_block", RENDERTYPE_GLINT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_BLOCK, false, false);
   private static final RenderType KNIGHT_OF_OWNER_ENTITY_GLINT_BLOCK = knightOfOwner("knight_of_owner_entity_glint_block", RENDERTYPE_ENTITY_GLINT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_BLOCK, true, true);
   private static final RenderType KNIGHT_OF_OWNER_GLINT_DIRECT_BLOCK = knightOfOwner("knight_of_owner_glint_direct_block", RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_BLOCK, false, false);
   private static final RenderType KNIGHT_OF_OWNER_ENTITY_GLINT_DIRECT_BLOCK = knightOfOwner("knight_of_owner_entity_glint_direct_block", RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER, KNIGHT_OF_OWNER_STATIC_TEXTURING_BLOCK, false, true);

   private static RenderType knightOfOwner(String name, ShaderStateShard shader, TexturingStateShard texturing, boolean itemTarget, boolean entityShader) {
      var builder = CompositeState.builder()
         .setShaderState(shader)
         .setTextureState(new TextureStateShard(KNIGHT_OF_OWNER_ITEM_GLINT, false, false))
         .setWriteMaskState(COLOR_WRITE)
         .setCullState(NO_CULL)
         .setDepthTestState(EQUAL_DEPTH_TEST)
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setTexturingState(texturing);
      if (itemTarget || entityShader) {
         builder.setOutputState(ITEM_ENTITY_TARGET);
      }
      return create(name, DefaultVertexFormat.POSITION_TEX, Mode.QUADS, 1536, builder.createCompositeState(false));
   }

   private ReinforcementRenderType(
      String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
   }

   public static RenderType glintTranslucent() {
      return REINFORCEMENT_GLINT_TRANSLUCENT;
   }

   public static RenderType glintTranslucent3d() {
      return REINFORCEMENT_GLINT_TRANSLUCENT_3D;
   }

   public static RenderType glint() {
      return REINFORCEMENT_GLINT;
   }

   public static RenderType glint3d() {
      return REINFORCEMENT_GLINT_3D;
   }

   public static RenderType entityGlint() {
      return REINFORCEMENT_ENTITY_GLINT;
   }

   public static RenderType entityGlint3d() {
      return REINFORCEMENT_ENTITY_GLINT_3D;
   }

   public static RenderType entityGlintDirect() {
      return REINFORCEMENT_ENTITY_GLINT_DIRECT;
   }

   public static RenderType entityGlintDirect3d() {
      return REINFORCEMENT_ENTITY_GLINT_DIRECT_3D;
   }

   public static RenderType glintDirect() {
      return REINFORCEMENT_GLINT_DIRECT;
   }

   public static RenderType glintDirect3d() {
      return REINFORCEMENT_GLINT_DIRECT_3D;
   }

   public static RenderType glintTranslucentBlock() {
      return REINFORCEMENT_GLINT_TRANSLUCENT_BLOCK;
   }

   public static RenderType glintBlock() {
      return REINFORCEMENT_GLINT_BLOCK;
   }

   public static RenderType entityGlintBlock() {
      return REINFORCEMENT_ENTITY_GLINT_BLOCK;
   }

   public static RenderType glintDirectBlock() {
      return REINFORCEMENT_GLINT_DIRECT_BLOCK;
   }

   public static RenderType entityGlintDirectBlock() {
      return REINFORCEMENT_ENTITY_GLINT_DIRECT_BLOCK;
   }

   public static RenderType knightOfOwnerGlintTranslucent() { return KNIGHT_OF_OWNER_GLINT_TRANSLUCENT; }
   public static RenderType knightOfOwnerGlint() { return KNIGHT_OF_OWNER_GLINT; }
   public static RenderType knightOfOwnerEntityGlint() { return KNIGHT_OF_OWNER_ENTITY_GLINT; }
   public static RenderType knightOfOwnerEntityGlintDirect() { return KNIGHT_OF_OWNER_ENTITY_GLINT_DIRECT; }
   public static RenderType knightOfOwnerGlintDirect() { return KNIGHT_OF_OWNER_GLINT_DIRECT; }
   public static RenderType knightOfOwnerGlintTranslucent3d() { return KNIGHT_OF_OWNER_GLINT_TRANSLUCENT_3D; }
   public static RenderType knightOfOwnerGlint3d() { return KNIGHT_OF_OWNER_GLINT_3D; }
   public static RenderType knightOfOwnerEntityGlint3d() { return KNIGHT_OF_OWNER_ENTITY_GLINT_3D; }
   public static RenderType knightOfOwnerGlintDirect3d() { return KNIGHT_OF_OWNER_GLINT_DIRECT_3D; }
   public static RenderType knightOfOwnerEntityGlintDirect3d() { return KNIGHT_OF_OWNER_ENTITY_GLINT_DIRECT_3D; }
   public static RenderType knightOfOwnerGlintTranslucentBlock() { return KNIGHT_OF_OWNER_GLINT_TRANSLUCENT_BLOCK; }
   public static RenderType knightOfOwnerGlintBlock() { return KNIGHT_OF_OWNER_GLINT_BLOCK; }
   public static RenderType knightOfOwnerEntityGlintBlock() { return KNIGHT_OF_OWNER_ENTITY_GLINT_BLOCK; }
   public static RenderType knightOfOwnerGlintDirectBlock() { return KNIGHT_OF_OWNER_GLINT_DIRECT_BLOCK; }
   public static RenderType knightOfOwnerEntityGlintDirectBlock() { return KNIGHT_OF_OWNER_ENTITY_GLINT_DIRECT_BLOCK; }

   public static RenderType getReinforcementFoilType(RenderType baseRenderType, boolean useItemGlint) {
      if (Minecraft.useShaderTransparency() && baseRenderType == Sheets.translucentItemSheet()) {
         return glintTranslucent();
      } else {
         return useItemGlint ? glint() : entityGlint();
      }
   }

   public static RenderType getReinforcementFoilDirectType(boolean useItemGlint) {
      return useItemGlint ? glintDirect() : entityGlintDirect();
   }

   public static RenderType getSkinRenderType(ReinforcementRenderType.ReinforcementPart part) {
      return REINFORCEMENT_SKIN_EMISSIVE;
   }

   public static RenderType getSkinRenderType(ReinforcementRenderType.ReinforcementPart part, LivingEntity entity) {
      return isLowHealth(entity) ? REINFORCEMENT_SKIN_EMISSIVE_DANGER : REINFORCEMENT_SKIN_EMISSIVE;
   }

   public static void addGlintTypes(Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> map) {
      addIfAbsent(map, glint());
      addIfAbsent(map, glintTranslucent());
      addIfAbsent(map, entityGlint());
      addIfAbsent(map, glintDirect());
      addIfAbsent(map, entityGlintDirect());
      addIfAbsent(map, glint3d());
      addIfAbsent(map, glintTranslucent3d());
      addIfAbsent(map, entityGlint3d());
      addIfAbsent(map, glintDirect3d());
      addIfAbsent(map, entityGlintDirect3d());
      addIfAbsent(map, glintBlock());
      addIfAbsent(map, glintTranslucentBlock());
      addIfAbsent(map, entityGlintBlock());
      addIfAbsent(map, glintDirectBlock());
      addIfAbsent(map, entityGlintDirectBlock());
      for (RenderType renderType : knightOfOwnerGlintTypes()) {
         addIfAbsent(map, renderType);
      }
   }

   public static RenderType[] glintTypes() {
      RenderType[] base = new RenderType[]{
         glint(),
         glintTranslucent(),
         entityGlint(),
         glintDirect(),
         entityGlintDirect(),
         glint3d(),
         glintTranslucent3d(),
         entityGlint3d(),
         glintDirect3d(),
         entityGlintDirect3d(),
         glintBlock(),
         glintTranslucentBlock(),
         entityGlintBlock(),
         glintDirectBlock(),
         entityGlintDirectBlock()
      };
      RenderType[] knight = knightOfOwnerGlintTypes();
      RenderType[] all = java.util.Arrays.copyOf(base, base.length + knight.length);
      System.arraycopy(knight, 0, all, base.length, knight.length);
      return all;
   }

   private static RenderType[] knightOfOwnerGlintTypes() {
      return new RenderType[]{
         knightOfOwnerGlint(),
         knightOfOwnerGlintTranslucent(),
         knightOfOwnerEntityGlint(),
         knightOfOwnerGlintDirect(),
         knightOfOwnerEntityGlintDirect(),
         knightOfOwnerGlint3d(),
         knightOfOwnerGlintTranslucent3d(),
         knightOfOwnerEntityGlint3d(),
         knightOfOwnerGlintDirect3d(),
         knightOfOwnerEntityGlintDirect3d(),
         knightOfOwnerGlintBlock(),
         knightOfOwnerGlintTranslucentBlock(),
         knightOfOwnerEntityGlintBlock(),
         knightOfOwnerGlintDirectBlock(),
         knightOfOwnerEntityGlintDirectBlock()
      };
   }

   private static void addIfAbsent(Object2ObjectLinkedOpenHashMap<RenderType, ByteBufferBuilder> map, RenderType renderType) {
      if (!map.containsKey(renderType)) {
         map.put(renderType, new ByteBufferBuilder(renderType.bufferSize()));
      }
   }

   private static boolean isLowHealth(LivingEntity entity) {
      if (entity == null) {
         return false;
      } else {
         float maxHealth = entity.getMaxHealth();
         return maxHealth <= 0.0F ? false : entity.getHealth() <= maxHealth * 0.25F;
      }
   }

   private static void setupReinforcementGlintTexturing(float scale, float angleDeg, long uPeriod, long vPeriod) {
      long time = (long)(Util.getMillis() * (Double)Minecraft.getInstance().options.glintSpeed().get() * 2.0);
      float u = (float)(time % uPeriod) / (float)uPeriod;
      float v = (float)(time % vPeriod) / (float)vPeriod;
      Matrix4f matrix = new Matrix4f().translation(-u, v, 0.0F).rotate(Axis.ZP.rotationDegrees(angleDeg)).scale(scale);
      RenderSystem.setTextureMatrix(matrix);
   }

   private static void setupStaticGlintTexturing(float scale, float angleDeg) {
      Matrix4f matrix = new Matrix4f().rotate(Axis.ZP.rotationDegrees(angleDeg)).scale(scale);
      RenderSystem.setTextureMatrix(matrix);
   }

   public static enum ReinforcementPart {
      HEAD,
      BODY,
      ARM,
      LEG;
   }
}
