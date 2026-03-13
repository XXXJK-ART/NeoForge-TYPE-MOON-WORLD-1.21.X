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

   public static enum ReinforcementPart {
      HEAD,
      BODY,
      ARM,
      LEG;
   }
}
