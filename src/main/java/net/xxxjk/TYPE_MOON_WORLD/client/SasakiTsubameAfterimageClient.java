package net.xxxjk.TYPE_MOON_WORLD.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ServantCardArmorRenderer;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;

/** The two extra humanoid rigs in the supplied animation, rendered without world entities or damage. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class SasakiTsubameAfterimageClient {
   private static final float SOURCE_ANIMATION_SECONDS = 0.625F;
   private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("typemoonworld",
      "animations/sasaki_tsubame_afterimages.animation.json");
   private static final Map<UUID, Cast> CASTS = new HashMap<>();
   private static final Map<EquipmentSlot, ServantCardArmorRenderer> ARMOR = new EnumMap<>(EquipmentSlot.class);
   private static PlayerModel<LivingEntity> model;
   private static JsonObject bones;
   private static boolean animationLoadFailed;
   private static net.minecraft.client.multiplayer.ClientLevel castLevel;

   private SasakiTsubameAfterimageClient() { }

   public static void begin(LivingEntity owner) {
      if (owner == null || !owner.level().isClientSide()) return;
      ensureLevel();
      CASTS.put(owner.getUUID(), new Cast(owner, owner.position(), owner.getYRot(), owner.level().getGameTime()));
   }

   public static int afterimageCount(UUID owner) {
      Cast cast = CASTS.get(owner);
      return cast != null && cast.owner.level() == castLevel && castLevel.getGameTime() - cast.started < 18L ? 2 : 0;
   }

   public static float animationTime(float ageTicks) {
      return Mth.clamp(ageTicks / 20.0F, 0.0F, SOURCE_ANIMATION_SECONDS);
   }

   public static void applyMainPose(net.minecraft.client.model.HumanoidModel<?> target, float seconds) {
      if (target == null || !ensureAnimationLoaded(Minecraft.getInstance())) return;
      float time = Mth.clamp(seconds, 0.0F, SOURCE_ANIMATION_SECONDS);
      apply(target.body, "Waist", time);
      apply(target.head, "Head", time);
      apply(target.rightArm, "Right Arm", time);
      apply(target.leftArm, "Left Arm", time);
      apply(target.rightLeg, "Right Leg", time);
      apply(target.leftLeg, "Left Leg", time);
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) { ensureLevel(); }

   private static void ensureLevel() {
      var level = Minecraft.getInstance().level;
      if (castLevel != level) { CASTS.clear(); castLevel = level; }
   }

   @SubscribeEvent
   public static void render(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
      ensureLevel();
      if (castLevel == null) return;
      Minecraft mc = Minecraft.getInstance();
      float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
      for (var entity : castLevel.entitiesForRendering()) {
         if (entity instanceof SasakiKojiroEntity sasaki && sasaki.getSasakiAction() == SasakiKojiroAction.TSUBAME_GAESHI
            && sasaki.isSasakiActionActive()) {
            long start = castLevel.getGameTime() - (long)sasaki.getSasakiActionAge(0.0F);
            Cast previous = CASTS.get(sasaki.getUUID());
            if (previous == null || previous.started != start) {
               CASTS.put(sasaki.getUUID(), new Cast(sasaki, sasaki.position(), sasaki.getYRot(), start));
            }
         }
      }
      CASTS.values().removeIf(cast -> cast.owner.level() != castLevel || !cast.owner.isAlive()
         || castLevel.getGameTime() - cast.started >= 18L);
      if (CASTS.isEmpty()) return;
      if (model == null) model = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      if (!ensureAnimationLoaded(mc)) return;
      PoseStack poses = event.getPoseStack();
      Vec3 camera = event.getCamera().getPosition();
      for (Cast cast : CASTS.values()) {
         float age = castLevel.getGameTime() - cast.started + partialTick;
         if (age < 0.0F || age >= 18.0F) continue;
         float seconds = animationTime(age);
         float alpha = 0.72F * (1.0F - Mth.clamp((age - 10.0F) / 8.0F, 0.0F, 1.0F));
         ResourceLocation skin = texture(cast.owner, mc);
         RenderType type = RenderType.entityTranslucent(skin);
         var buffers = mc.renderBuffers().bufferSource();
         for (int shadow = 2; shadow <= 3; shadow++) {
            float scale = cast.owner instanceof SasakiKojiroEntity ? 0.926F : 1.0F;
            pose(shadow, seconds);
            Vec3 offset = channel("bone" + shadow, "position", seconds);
            poses.pushPose();
            poses.translate(cast.origin.x - camera.x, cast.origin.y - camera.y, cast.origin.z - camera.z);
            poses.mulPose(Axis.YP.rotationDegrees(180.0F - cast.yaw));
            poses.translate(offset.x / 16.0, -offset.y / 16.0, offset.z / 16.0);
            poses.scale(-scale, -scale, scale);
            poses.translate(0.0F, -1.501F, 0.0F);
            int tint = FastColor.ARGB32.color((int)(alpha * 255.0F), 176, 215, 255);
            model.renderToBuffer(poses, buffers.getBuffer(type), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
            buffers.endBatch(type);
            renderArmor(cast.owner, mc, poses, partialTick, tint);
            if (!cast.owner.getMainHandItem().isEmpty()) {
               poses.pushPose();
               model.translateToHand(HumanoidArm.RIGHT, poses);
               poses.mulPose(Axis.XP.rotationDegrees(-90.0F));
               poses.mulPose(Axis.YP.rotationDegrees(180.0F));
               poses.translate(0.0625F, 0.125F, -0.625F);
               applyWeaponAnimation(poses, shadow == 2 ? "jian3" : "jian2", seconds);
               mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(cast.owner, cast.owner.getMainHandItem(),
                  ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, poses, buffers, LightTexture.FULL_BRIGHT);
               poses.popPose();
            }
            poses.popPose();
         }
         buffers.endBatch(type);
      }
   }

   private static ResourceLocation texture(LivingEntity owner, Minecraft mc) {
      if (owner instanceof AbstractClientPlayer player) return player.getSkin().texture();
      return mc.getEntityRenderDispatcher().getRenderer(owner).getTextureLocation(owner);
   }

   private static void renderArmor(LivingEntity owner, Minecraft mc, PoseStack poses, float partialTick, int tint) {
      var buffers = mc.renderBuffers().bufferSource();
      for (EquipmentSlot slot : java.util.List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
         var stack = owner.getItemBySlot(slot);
         if (!(stack.getItem() instanceof ServantCardArmorItem item)) continue;
         var armor = ARMOR.computeIfAbsent(slot, ignored -> new ServantCardArmorRenderer() {
            @Override
            public RenderType getRenderType(ServantCardArmorItem animatable, ResourceLocation texture,
                                            net.minecraft.client.renderer.MultiBufferSource source, float tick) {
               return RenderType.entityTranslucent(texture);
            }
         });
         armor.prepForRender(owner, stack, slot, model, buffers, partialTick, 0.0F, 0.0F, 0.0F, 0.0F);
         RenderType type = RenderType.entityTranslucent(armor.getTextureLocation(item));
         armor.renderToBuffer(poses, buffers.getBuffer(type), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
         buffers.endBatch(type);
      }
   }

   private static boolean ensureAnimationLoaded(Minecraft mc) {
      if (bones != null) return true;
      if (animationLoadFailed) return false;
      try (var reader = mc.getResourceManager().getResourceOrThrow(ANIMATION).openAsReader()) {
         bones = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations")
            .getAsJsonObject("yanfan").getAsJsonObject("bones");
         return true;
      } catch (Exception error) {
         animationLoadFailed = true;
         TYPE_MOON_WORLD.LOGGER.error("Could not load Tsubame source animation", error);
         return false;
      }
   }

   private static void pose(int shadow, float seconds) {
      model.young = false;
      model.riding = false;
      model.crouching = false;
      model.attackTime = 0.0F;
      model.swimAmount = 0.0F;
      for (ModelPart part : java.util.List.of(model.head, model.hat, model.body, model.rightArm, model.leftArm,
         model.rightLeg, model.leftLeg, model.jacket, model.rightSleeve, model.leftSleeve, model.rightPants, model.leftPants)) part.resetPose();
      model.setAllVisible(true);
      apply(model.body, "Waist" + shadow, seconds);
      apply(model.head, "Head" + shadow, seconds);
      apply(model.rightArm, "Right_Arm" + shadow, seconds);
      apply(model.leftArm, "Left_Arm" + shadow, seconds);
      apply(model.rightLeg, "Right_Leg" + shadow, seconds);
      apply(model.leftLeg, "Left_Leg" + shadow, seconds);
      model.hat.copyFrom(model.head);
      model.jacket.copyFrom(model.body);
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftSleeve.copyFrom(model.leftArm);
      model.rightPants.copyFrom(model.rightLeg);
      model.leftPants.copyFrom(model.leftLeg);
   }

   private static void apply(ModelPart part, String bone, float time) {
      Vec3 rotation = channel(bone, "rotation", time);
      part.xRot = Mth.wrapDegrees((float)rotation.x) * Mth.DEG_TO_RAD;
      part.yRot = Mth.wrapDegrees((float)rotation.y) * Mth.DEG_TO_RAD;
      part.zRot = Mth.wrapDegrees((float)rotation.z) * Mth.DEG_TO_RAD;
      Vec3 position = channel(bone, "position", time);
      part.x += (float)position.x;
      part.y -= (float)position.y;
      part.z += (float)position.z;
   }

   private static void applyWeaponAnimation(PoseStack poses, String bone, float time) {
      Vec3 currentPosition = channel(bone, "position", time);
      Vec3 initialPosition = channel(bone, "position", 0.0F);
      poses.translate((currentPosition.x - initialPosition.x) / 16.0,
         -(currentPosition.y - initialPosition.y) / 16.0,
         (currentPosition.z - initialPosition.z) / 16.0);
      Vec3 currentRotation = channel(bone, "rotation", time);
      Vec3 initialRotation = channel(bone, "rotation", 0.0F);
      poses.mulPose(Axis.ZP.rotationDegrees(Mth.wrapDegrees((float)(currentRotation.z - initialRotation.z))));
      poses.mulPose(Axis.YP.rotationDegrees(Mth.wrapDegrees((float)(currentRotation.y - initialRotation.y))));
      poses.mulPose(Axis.XP.rotationDegrees(Mth.wrapDegrees((float)(currentRotation.x - initialRotation.x))));
      Vec3 scale = channel(bone, "scale", time);
      float factor = scale.x > 0.0 ? (float)scale.x : 0.8F;
      poses.scale(factor, factor, factor);
   }

   private static Vec3 channel(String bone, String name, float time) {
      JsonObject data = bones.getAsJsonObject(bone);
      if (data == null || !data.has(name)) return Vec3.ZERO;
      JsonElement values = data.get(name);
      if (values.isJsonArray()) return vector(values);
      if (values.isJsonPrimitive()) {
         double value = values.getAsDouble();
         return new Vec3(value, value, value);
      }
      float before = -Float.MAX_VALUE, after = Float.MAX_VALUE;
      Vec3 previous = Vec3.ZERO, next = Vec3.ZERO;
      for (var entry : values.getAsJsonObject().entrySet()) {
         float key = Float.parseFloat(entry.getKey());
         if (key <= time && key > before) { before = key; previous = vector(entry.getValue()); }
         if (key >= time && key < after) { after = key; next = vector(entry.getValue()); }
      }
      if (before == -Float.MAX_VALUE) return next;
      if (after == Float.MAX_VALUE || after == before) return previous;
      return previous.lerp(next, (time - before) / (after - before));
   }

   private static Vec3 vector(JsonElement value) {
      var array = value.getAsJsonArray();
      return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
   }

   private record Cast(LivingEntity owner, Vec3 origin, float yaw, long started) { }
}
