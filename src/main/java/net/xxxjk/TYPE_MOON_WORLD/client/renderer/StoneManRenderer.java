package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.xxxjk.TYPE_MOON_WORLD.client.model.StoneManModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.StoneManEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class StoneManRenderer extends GeoEntityRenderer<StoneManEntity> {
   private static final ResourceLocation FALLBACK_BODY_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png");
   private static final Map<StoneManRenderer.TextureVariantKey, ResourceLocation> TILED_TEXTURE_CACHE = new HashMap<>();
   private static final int TILE_REPEAT = 16;
   private static final int MAX_TILED_SIZE = 1024;
   private static final int FACE_VARIANT_COUNT = 16;
   private static final int TOP_BAND_PERCENT = 18;
   private static final int BOTTOM_BAND_PERCENT = 18;
   private static final int TOP_FACE_BIAS_PERCENT = 65;
   private static final int BOTTOM_FACE_BIAS_PERCENT = 65;
   private static final float TOP_LIGHT_BOOST = 1.1F;
   private static final float BOTTOM_DARKEN = 0.2F;
   private static final float EDGE_DARKEN = 0.12F;
   private static final float EMBOSS_STRENGTH = 0.35F;
   private static final float EMBOSS_MIN_FACTOR = 0.78F;
   private static final float EMBOSS_MAX_FACTOR = 1.22F;
   private static final float CAVITY_AO_STRENGTH = 1.15F;
   private static final float RIDGE_LIGHT_STRENGTH = 0.8F;
   private static final float MICRO_CONTRAST_STRENGTH = 0.42F;
   private static final float CAVITY_MAX_DARKEN = 0.2F;
   private static final float RIDGE_MAX_LIGHTEN = 0.14F;
   private static final float MICRO_EDGE_MAX_LIGHTEN = 0.1F;
   private static final float SHOULDER_CENTER_V = 0.26F;
   private static final float SHOULDER_HALF_WIDTH = 0.22F;
   private static final float CHEST_CENTER_V = 0.48F;
   private static final float CHEST_HALF_WIDTH = 0.2F;
   private static final float SHOULDER_ROUGH_DARKEN = 0.16F;
   private static final float CHEST_CENTER_BRIGHTEN = 0.11F;
   private static final float REGION_NOISE_STRENGTH = 0.07F;

   public StoneManRenderer(Context renderManager) {
      super(renderManager, new StoneManModel());
      this.withScale(1.2F);
      this.addRenderLayer(new StoneManRenderer.StoneBodyTextureLayer(this));
      this.addRenderLayer(new StoneManRenderer.TntFuseFlashLayer(this));
   }

   private static StoneManRenderer.BodyRenderData resolveBodyRenderData(StoneManEntity animatable) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft != null && minecraft.getBlockRenderer() != null) {
         BlockState mimicState = animatable.getMimicBlockState();
         StoneManRenderer.TextureSet textureSet = resolveTextureSet(minecraft, mimicState);
         int variant = Math.floorMod(animatable.getUUID().hashCode(), FACE_VARIANT_COUNT);
         ResourceLocation texture = getOrCreateTiledTexture(textureSet, variant);
         int tint = resolveBodyTint(minecraft, mimicState, animatable);
         return new StoneManRenderer.BodyRenderData(texture, tint);
      } else {
         return new StoneManRenderer.BodyRenderData(FALLBACK_BODY_TEXTURE, -1);
      }
   }

   private static StoneManRenderer.TextureSet resolveTextureSet(Minecraft minecraft, BlockState state) {
      TextureAtlasSprite particleSprite = minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
      ResourceLocation particleTexture = particleSprite == null ? FALLBACK_BODY_TEXTURE : toTexturePath(particleSprite.contents().name());
      if (!resourceExists(minecraft, particleTexture)) {
         particleTexture = FALLBACK_BODY_TEXTURE;
      }

      BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
      ResourceLocation northRaw = findDirectionTexture(minecraft, model, state, Direction.NORTH);
      ResourceLocation southRaw = findDirectionTexture(minecraft, model, state, Direction.SOUTH);
      ResourceLocation eastRaw = findDirectionTexture(minecraft, model, state, Direction.EAST);
      ResourceLocation westRaw = findDirectionTexture(minecraft, model, state, Direction.WEST);
      ResourceLocation upRaw = findDirectionTexture(minecraft, model, state, Direction.UP);
      ResourceLocation downRaw = findDirectionTexture(minecraft, model, state, Direction.DOWN);
      ResourceLocation sideFallback = firstAvailableTexture(minecraft, northRaw, southRaw, eastRaw, westRaw, particleTexture, FALLBACK_BODY_TEXTURE);
      ResourceLocation up = firstAvailableTexture(minecraft, upRaw, sideFallback, particleTexture, FALLBACK_BODY_TEXTURE);
      ResourceLocation down = firstAvailableTexture(minecraft, downRaw, sideFallback, particleTexture, FALLBACK_BODY_TEXTURE);
      ResourceLocation north = firstAvailableTexture(minecraft, northRaw, sideFallback, up, down, FALLBACK_BODY_TEXTURE);
      ResourceLocation south = firstAvailableTexture(minecraft, southRaw, sideFallback, up, down, FALLBACK_BODY_TEXTURE);
      ResourceLocation east = firstAvailableTexture(minecraft, eastRaw, sideFallback, up, down, FALLBACK_BODY_TEXTURE);
      ResourceLocation west = firstAvailableTexture(minecraft, westRaw, sideFallback, up, down, FALLBACK_BODY_TEXTURE);
      return new StoneManRenderer.TextureSet(up, down, north, south, west, east);
   }

   private static int resolveBodyTint(Minecraft minecraft, BlockState state, StoneManEntity entity) {
      int tintIndex = findTintIndex(minecraft, state);
      if (tintIndex < 0) {
         return -1;
      } else {
         int rgb = minecraft.getBlockColors().getColor(state, entity.level(), entity.blockPosition(), tintIndex);
         return rgb == -1 ? -1 : 0xFF000000 | rgb & 16777215;
      }
   }

   private static int findTintIndex(Minecraft minecraft, BlockState state) {
      BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);

      for (Direction direction : Direction.values()) {
         int tint = firstTintIndex(model.getQuads(state, direction, RandomSource.create(42L)));
         if (tint >= 0) {
            return tint;
         }
      }

      return firstTintIndex(model.getQuads(state, null, RandomSource.create(42L)));
   }

   private static int firstTintIndex(List<BakedQuad> quads) {
      for (BakedQuad quad : quads) {
         if (quad.isTinted()) {
            return quad.getTintIndex();
         }
      }

      return -1;
   }

   @Nullable
   private static ResourceLocation findDirectionTexture(Minecraft minecraft, BakedModel model, BlockState state, Direction direction) {
      ResourceLocation fromFaceQuads = firstTextureFromQuads(model.getQuads(state, direction, RandomSource.create(42L)));
      if (fromFaceQuads != null && resourceExists(minecraft, fromFaceQuads)) {
         return fromFaceQuads;
      } else {
         for (BakedQuad quad : model.getQuads(state, null, RandomSource.create(42L))) {
            if (quad.getDirection() == direction) {
               ResourceLocation candidate = toTexturePath(quad.getSprite().contents().name());
               if (resourceExists(minecraft, candidate)) {
                  return candidate;
               }
            }
         }

         return null;
      }
   }

   @Nullable
   private static ResourceLocation firstTextureFromQuads(List<BakedQuad> quads) {
      for (BakedQuad quad : quads) {
         TextureAtlasSprite sprite = quad.getSprite();
         if (sprite != null) {
            return toTexturePath(sprite.contents().name());
         }
      }

      return null;
   }

   private static ResourceLocation firstAvailableTexture(Minecraft minecraft, @Nullable ResourceLocation... candidates) {
      for (ResourceLocation candidate : candidates) {
         if (candidate != null && resourceExists(minecraft, candidate)) {
            return candidate;
         }
      }

      return FALLBACK_BODY_TEXTURE;
   }

   private static boolean resourceExists(Minecraft minecraft, ResourceLocation texture) {
      return minecraft.getResourceManager().getResource(texture).isPresent();
   }

   private static ResourceLocation toTexturePath(ResourceLocation spriteName) {
      String path = spriteName.getPath();
      if (path.startsWith("textures/")) {
         return path.endsWith(".png") ? spriteName : ResourceLocation.fromNamespaceAndPath(spriteName.getNamespace(), path + ".png");
      } else {
         return ResourceLocation.fromNamespaceAndPath(spriteName.getNamespace(), "textures/" + path + ".png");
      }
   }

   private static ResourceLocation getOrCreateTiledTexture(StoneManRenderer.TextureSet textureSet, int variant) {
      StoneManRenderer.TextureVariantKey key = new StoneManRenderer.TextureVariantKey(textureSet, variant);
      ResourceLocation cached = TILED_TEXTURE_CACHE.get(key);
      if (cached != null) {
         return cached;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft == null) {
            return FALLBACK_BODY_TEXTURE;
         } else {
            Set<NativeImage> toClose = Collections.newSetFromMap(new IdentityHashMap<>());
            NativeImage upImage = null;
            NativeImage downImage = null;
            NativeImage northImage = null;
            NativeImage southImage = null;
            NativeImage westImage = null;
            NativeImage eastImage = null;

            ResourceLocation var42;
            try {
               upImage = readAndTrackImage(minecraft, textureSet.up(), toClose);
               downImage = readAndTrackImage(minecraft, textureSet.down(), toClose);
               northImage = readAndTrackImage(minecraft, textureSet.north(), toClose);
               southImage = readAndTrackImage(minecraft, textureSet.south(), toClose);
               westImage = readAndTrackImage(minecraft, textureSet.west(), toClose);
               eastImage = readAndTrackImage(minecraft, textureSet.east(), toClose);
               NativeImage fallback = firstNonNullImage(northImage, southImage, westImage, eastImage, upImage, downImage);
               if (fallback == null) {
                  fallback = readAndTrackImage(minecraft, FALLBACK_BODY_TEXTURE, toClose);
               }

               if (fallback != null) {
                  if (upImage == null) {
                     upImage = fallback;
                  }

                  if (downImage == null) {
                     downImage = fallback;
                  }

                  if (northImage == null) {
                     northImage = fallback;
                  }

                  if (southImage == null) {
                     southImage = fallback;
                  }

                  if (westImage == null) {
                     westImage = fallback;
                  }

                  if (eastImage == null) {
                     eastImage = fallback;
                  }

                  int tileWidth = Math.max(
                     1,
                     Math.max(
                        Math.max(upImage.getWidth(), downImage.getWidth()),
                        Math.max(Math.max(northImage.getWidth(), southImage.getWidth()), Math.max(westImage.getWidth(), eastImage.getWidth()))
                     )
                  );
                  int tileHeight = Math.max(
                     1,
                     Math.max(
                        Math.max(upImage.getHeight(), downImage.getHeight()),
                        Math.max(Math.max(northImage.getHeight(), southImage.getHeight()), Math.max(westImage.getHeight(), eastImage.getHeight()))
                     )
                  );
                  int outWidth = Math.min(MAX_TILED_SIZE, tileWidth * TILE_REPEAT);
                  int outHeight = Math.min(MAX_TILED_SIZE, tileHeight * TILE_REPEAT);
                  NativeImage mixed = new NativeImage(outWidth, outHeight, false);

                  for (int y = 0; y < outHeight; y++) {
                     int yPercent = (int)(y * 100L / Math.max(1, outHeight - 1));

                     for (int x = 0; x < outWidth; x++) {
                        int cellX = x / tileWidth;
                        int cellY = y / tileHeight;
                        NativeImage source = pickRandomFaceImage(
                           upImage, downImage, northImage, southImage, westImage, eastImage, cellX, cellY, yPercent, variant
                        );
                        int srcWidth = Math.max(1, source.getWidth());
                        int srcHeight = Math.max(1, source.getHeight());
                        int srcX = x % srcWidth;
                        int srcY = y % srcHeight;
                        int color = source.getPixelRGBA(srcX, srcY);
                        color = applyEmboss(color, source, srcX, srcY, srcWidth, srcHeight);
                        color = applyMicroSurfaceDepth(color, source, srcX, srcY, srcWidth, srcHeight);
                        color = applyBodyRegionMaterialProfile(color, x, y, outWidth, outHeight, variant);
                        color = applyPseudoShading(color, x, y, outWidth, outHeight);
                        mixed.setPixelRGBA(x, y, color);
                     }
                  }

                  DynamicTexture dynamicTexture = new DynamicTexture(mixed);
                  dynamicTexture.setFilter(false, false);
                  ResourceLocation dynamicLocation = minecraft.getTextureManager()
                     .register("stone_man_tile_" + textureSet.cacheId() + "_v" + variant, dynamicTexture);
                  TILED_TEXTURE_CACHE.put(key, dynamicLocation);
                  return dynamicLocation;
               }

               var42 = FALLBACK_BODY_TEXTURE;
            } catch (IOException var34) {
               return FALLBACK_BODY_TEXTURE;
            } finally {
               for (NativeImage image : toClose) {
                  image.close();
               }
            }

            return var42;
         }
      }
   }

   @Nullable
   private static NativeImage readTextureImage(Minecraft minecraft, ResourceLocation texture) throws IOException {
      Optional<Resource> resourceOpt = minecraft.getResourceManager().getResource(texture);
      if (resourceOpt.isEmpty()) {
         return null;
      } else {
         Resource resource = resourceOpt.get();

         NativeImage var5;
         try (InputStream stream = resource.open()) {
            var5 = NativeImage.read(stream);
         }

         return var5;
      }
   }

   @Nullable
   private static NativeImage readAndTrackImage(Minecraft minecraft, ResourceLocation texture, Set<NativeImage> collector) throws IOException {
      NativeImage image = readTextureImage(minecraft, texture);
      if (image != null) {
         collector.add(image);
      }

      return image;
   }

   @Nullable
   private static NativeImage firstNonNullImage(@Nullable NativeImage... images) {
      for (NativeImage image : images) {
         if (image != null) {
            return image;
         }
      }

      return null;
   }

   private static NativeImage pickRandomFaceImage(
      NativeImage upImage,
      NativeImage downImage,
      NativeImage northImage,
      NativeImage southImage,
      NativeImage westImage,
      NativeImage eastImage,
      int cellX,
      int cellY,
      int yPercent,
      int variant
   ) {
      int hash = hashCell(cellX, cellY, variant);
      int selector = Math.floorMod(hash, 100);
      if (yPercent <= TOP_BAND_PERCENT && selector < TOP_FACE_BIAS_PERCENT) {
         return upImage;
      } else if (yPercent >= (100 - BOTTOM_BAND_PERCENT) && selector < BOTTOM_FACE_BIAS_PERCENT) {
         return downImage;
      } else {
         int face = Math.floorMod(hash >>> 8, 6);

         return switch (face) {
            case 0 -> northImage;
            case 1 -> southImage;
            case 2 -> westImage;
            case 3 -> eastImage;
            case 4 -> upImage;
            default -> downImage;
         };
      }
   }

   private static int hashCell(int x, int y, int variant) {
      int h = x * 522133279 ^ y * 73244475 ^ variant * -1640531527;
      h ^= h >>> 16;
      h *= 668265261;
      return h ^ h >>> 15;
   }

   private static int applyPseudoShading(int argb, int x, int y, int width, int height) {
      int a = argb >>> 24 & 0xFF;
      if (a == 0) {
         return argb;
      } else {
         float u = width <= 1 ? 0.5F : (float)x / (width - 1);
         float v = height <= 1 ? 0.5F : (float)y / (height - 1);
         float verticalFactor = TOP_LIGHT_BOOST - v * BOTTOM_DARKEN;
         float edgeDistance = Math.abs(u - 0.5F) * 2.0F;
         float edgeFactor = 1.0F - edgeDistance * EDGE_DARKEN;
         float lightFactor = clamp(verticalFactor * edgeFactor, 0.7F, 1.2F);
         int r = argb >>> 16 & 0xFF;
         int g = argb >>> 8 & 0xFF;
         int b = argb & 0xFF;
         r = clampColor((int)(r * lightFactor));
         g = clampColor((int)(g * lightFactor));
         b = clampColor((int)(b * lightFactor));
         return a << 24 | r << 16 | g << 8 | b;
      }
   }

   private static int applyEmboss(int argb, NativeImage source, int srcX, int srcY, int srcWidth, int srcHeight) {
      int a = argb >>> 24 & 0xFF;
      if (a == 0) {
         return argb;
      } else {
         int x0 = wrap(srcX - 1, srcWidth);
         int y0 = wrap(srcY - 1, srcHeight);
         int x1 = wrap(srcX + 1, srcWidth);
         int y1 = wrap(srcY + 1, srcHeight);
         int c0 = source.getPixelRGBA(x0, y0);
         int c1 = source.getPixelRGBA(x1, y1);
         float gradient = luminance(c1) - luminance(c0);
         float factor = clamp(1.0F + gradient * EMBOSS_STRENGTH, EMBOSS_MIN_FACTOR, EMBOSS_MAX_FACTOR);
         int r = argb >>> 16 & 0xFF;
         int g = argb >>> 8 & 0xFF;
         int b = argb & 0xFF;
         r = clampColor((int)(r * factor));
         g = clampColor((int)(g * factor));
         b = clampColor((int)(b * factor));
         return a << 24 | r << 16 | g << 8 | b;
      }
   }

   private static int applyMicroSurfaceDepth(int argb, NativeImage source, int srcX, int srcY, int srcWidth, int srcHeight) {
      int a = argb >>> 24 & 0xFF;
      if (a == 0) {
         return argb;
      } else {
         int x0 = wrap(srcX - 1, srcWidth);
         int y0 = wrap(srcY - 1, srcHeight);
         int x1 = wrap(srcX + 1, srcWidth);
         int y1 = wrap(srcY + 1, srcHeight);
         float center = luminance(source.getPixelRGBA(srcX, srcY));
         float left = luminance(source.getPixelRGBA(x0, srcY));
         float right = luminance(source.getPixelRGBA(x1, srcY));
         float up = luminance(source.getPixelRGBA(srcX, y0));
         float down = luminance(source.getPixelRGBA(srcX, y1));
         float dx = right - left;
         float dy = down - up;
         float gradient = Mth.sqrt(dx * dx + dy * dy);
         float laplacian = (left + right + up + down) * 0.25F - center;
         float cavity = clamp(laplacian * CAVITY_AO_STRENGTH, 0.0F, CAVITY_MAX_DARKEN);
         float ridge = clamp(-laplacian * RIDGE_LIGHT_STRENGTH, 0.0F, RIDGE_MAX_LIGHTEN);
         float microEdge = clamp(gradient * MICRO_CONTRAST_STRENGTH, 0.0F, MICRO_EDGE_MAX_LIGHTEN);
         float factor = clamp(1.0F - cavity + ridge + microEdge, 0.66F, 1.26F);
         int r = argb >>> 16 & 0xFF;
         int g = argb >>> 8 & 0xFF;
         int b = argb & 0xFF;
         r = clampColor((int)(r * factor));
         g = clampColor((int)(g * factor));
         b = clampColor((int)(b * factor));
         return a << 24 | r << 16 | g << 8 | b;
      }
   }

   private static int applyBodyRegionMaterialProfile(int argb, int x, int y, int width, int height, int variant) {
      int a = argb >>> 24 & 0xFF;
      if (a == 0) {
         return argb;
      } else {
         float u = width <= 1 ? 0.5F : (float)x / (width - 1);
         float v = height <= 1 ? 0.5F : (float)y / (height - 1);
         float centerBias = clamp(1.0F - Math.abs(u - 0.5F) * 2.0F, 0.0F, 1.0F);
         float shoulderMask = bandMask(v, SHOULDER_CENTER_V, SHOULDER_HALF_WIDTH);
         float chestMask = bandMask(v, CHEST_CENTER_V, CHEST_HALF_WIDTH);
         float macroNoise = hashToUnit(hashCell(x / 6, y / 6, variant ^ 1515870810)) * 2.0F - 1.0F;
         float microNoise = hashToUnit(hashCell(x, y, variant ^ 668265261)) * 2.0F - 1.0F;
         float shoulderRoughDarken = shoulderMask * (SHOULDER_ROUGH_DARKEN + Math.abs(macroNoise) * 0.05F);
         float chestBrighten = chestMask * centerBias * CHEST_CENTER_BRIGHTEN;
         float noiseFactor = (macroNoise * 0.65F + microNoise * 0.35F) * REGION_NOISE_STRENGTH * (0.35F + shoulderMask * 0.65F);
         float factor = clamp(1.0F - shoulderRoughDarken + chestBrighten + noiseFactor, 0.64F, 1.24F);
         int r = argb >>> 16 & 0xFF;
         int g = argb >>> 8 & 0xFF;
         int b = argb & 0xFF;
         float coolShadow = shoulderMask * 0.06F;
         float warmCore = chestMask * centerBias * 0.03F;
         r = clampColor((int)(r * (factor + warmCore)));
         g = clampColor((int)(g * factor));
         b = clampColor((int)(b * (factor - coolShadow)));
         return a << 24 | r << 16 | g << 8 | b;
      }
   }

   private static float luminance(int argb) {
      float r = (argb >>> 16 & 0xFF) / 255.0F;
      float g = (argb >>> 8 & 0xFF) / 255.0F;
      float b = (argb & 0xFF) / 255.0F;
      return 0.299F * r + 0.587F * g + 0.114F * b;
   }

   private static float bandMask(float value, float center, float halfWidth) {
      float delta = Math.abs(value - center);
      if (delta >= halfWidth) {
         return 0.0F;
      } else {
         float t = 1.0F - delta / halfWidth;
         return t * t * (3.0F - 2.0F * t);
      }
   }

   private static float hashToUnit(int hash) {
      return (hash & 2147483647) / 2.1474836E9F;
   }

   private static int wrap(int value, int size) {
      int result = value % size;
      return result < 0 ? result + size : result;
   }

   private static int clampColor(int value) {
      return Math.max(0, Math.min(255, value));
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private record BodyRenderData(ResourceLocation texture, int tint) {
   }

   private static class StoneBodyTextureLayer extends GeoRenderLayer<StoneManEntity> {
      public StoneBodyTextureLayer(GeoEntityRenderer<StoneManEntity> renderer) {
         super(renderer);
      }

      public void render(
         PoseStack poseStack,
         StoneManEntity animatable,
         BakedGeoModel bakedModel,
         @Nullable RenderType renderType,
         MultiBufferSource bufferSource,
         @Nullable VertexConsumer buffer,
         float partialTick,
         int packedLight,
         int packedOverlay
      ) {
         StoneManRenderer.BodyRenderData renderData = StoneManRenderer.resolveBodyRenderData(animatable);
         ResourceLocation bodyTexture = renderData.texture();
         RenderType bodyRenderType = RenderType.entityCutoutNoCull(bodyTexture);
         VertexConsumer bodyBuffer = bufferSource.getBuffer(bodyRenderType);
         Optional<GeoBone> faceBoneOptional = this.getGeoModel().getBone("face");
         boolean previousHidden = false;
         boolean hasFaceBone = faceBoneOptional.isPresent();
         if (hasFaceBone) {
            GeoBone faceBone = faceBoneOptional.get();
            previousHidden = faceBone.isHidden();
            faceBone.setHidden(true);
         }

         this.getRenderer()
            .reRender(bakedModel, poseStack, bufferSource, animatable, bodyRenderType, bodyBuffer, partialTick, packedLight, packedOverlay, renderData.tint());
         if (hasFaceBone) {
            faceBoneOptional.get().setHidden(previousHidden);
         }
      }
   }

   private record TextureSet(
      ResourceLocation up, ResourceLocation down, ResourceLocation north, ResourceLocation south, ResourceLocation west, ResourceLocation east
   ) {
      private String cacheId() {
         return this.up.toDebugFileName()
            + "_"
            + this.down.toDebugFileName()
            + "_"
            + this.north.toDebugFileName()
            + "_"
            + this.south.toDebugFileName()
            + "_"
            + this.west.toDebugFileName()
            + "_"
            + this.east.toDebugFileName();
      }
   }

   private record TextureVariantKey(StoneManRenderer.TextureSet set, int variant) {
   }

   private static class TntFuseFlashLayer extends GeoRenderLayer<StoneManEntity> {
      public TntFuseFlashLayer(GeoEntityRenderer<StoneManEntity> renderer) {
         super(renderer);
      }

      public void render(
         PoseStack poseStack,
         StoneManEntity animatable,
         BakedGeoModel bakedModel,
         @Nullable RenderType renderType,
         MultiBufferSource bufferSource,
         @Nullable VertexConsumer buffer,
         float partialTick,
         int packedLight,
         int packedOverlay
      ) {
         float alpha = animatable.getTntFlashAlpha(partialTick);
         if (!(alpha <= 0.001F)) {
            StoneManRenderer.BodyRenderData renderData = StoneManRenderer.resolveBodyRenderData(animatable);
            RenderType flashType = RenderType.entityTranslucent(renderData.texture());
            VertexConsumer flashBuffer = bufferSource.getBuffer(flashType);
            int flashColor = Mth.clamp((int)(alpha * 255.0F), 0, 255) << 24 | 16777215;
            Optional<GeoBone> faceBoneOptional = this.getGeoModel().getBone("face");
            boolean previousHidden = false;
            boolean hasFaceBone = faceBoneOptional.isPresent();
            if (hasFaceBone) {
               GeoBone faceBone = faceBoneOptional.get();
               previousHidden = faceBone.isHidden();
               faceBone.setHidden(true);
            }

            this.getRenderer()
               .reRender(
                  bakedModel,
                  poseStack,
                  bufferSource,
                  animatable,
                  flashType,
                  flashBuffer,
                  partialTick,
                  15728880,
                  OverlayTexture.pack(OverlayTexture.u(1.0F), false),
                  flashColor
               );
            if (hasFaceBone) {
               faceBoneOptional.get().setHidden(previousHidden);
            }
         }
      }
   }
}
