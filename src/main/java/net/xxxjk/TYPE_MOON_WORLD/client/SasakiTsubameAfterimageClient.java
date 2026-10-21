package net.xxxjk.TYPE_MOON_WORLD.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
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
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

/** The two extra humanoid rigs in the supplied animation, rendered without world entities or damage. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class SasakiTsubameAfterimageClient {
   private static final float SOURCE_ANIMATION_SECONDS = 0.625F;
   private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("typemoonworld",
      "animations/sasaki_tsubame_afterimages.animation.json");
   private static final Map<UUID, Cast> CASTS = new HashMap<>();
   private static final Map<EquipmentSlot, ServantCardArmorRenderer> ARMOR = new EnumMap<>(EquipmentSlot.class);
   private static PlayerModel<LivingEntity> model;
   private static PlayerModel<LivingEntity> trailModel;
   private static final Map<Item, Vec3> BLADE_TIPS = new HashMap<>();
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

   private static Vec3 heldItemOrigin(HumanoidModel<?> target, HumanoidArm arm) {
      PoseStack grip = new PoseStack();
      target.translateToHand(arm, grip);
      grip.mulPose(Axis.XP.rotationDegrees(-90.0F));
      grip.mulPose(Axis.YP.rotationDegrees(180.0F));
      boolean left = arm == HumanoidArm.LEFT;
      grip.translate(left ? -0.0625F : 0.0625F, 0.125F, -0.625F);
      Vector3f position = grip.last().pose().getTranslation(new Vector3f());
      return new Vec3(position.x, position.y, position.z);
   }

   private static void heldItemPose(PoseStack poses, HumanoidModel<?> target, HumanoidArm arm, float age) {
      Vec3 grip = heldItemOrigin(target, arm);
      poses.translate(grip.x, grip.y, grip.z);
      float lift = -32.0F * (1.0F - Mth.clamp(age / 2.5F, 0.0F, 1.0F));
      poses.mulPose(Axis.XP.rotationDegrees(-90.0F + lift));
      poses.mulPose(Axis.YP.rotationDegrees(180.0F));
   }

   /** Samples the same grip, item display transform and baked geometry as the rendered sword. */
   public static Vec3 bladeTip(LivingEntity owner, int blade, float age) {
      Minecraft mc = Minecraft.getInstance();
      if (!ensureAnimationLoaded(mc) || owner.getMainHandItem().isEmpty()) return Vec3.ZERO;
      if (trailModel == null) trailModel = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      float seconds = animationTime(age);
      if (blade == 1) applyMainPose(trailModel, seconds);
      else pose(trailModel, blade, seconds);
      PoseStack sample = new PoseStack();
      Vec3 offset = blade == 1 ? Vec3.ZERO : shadowOffset(blade, seconds);
      sample.translate(offset.x, offset.y, offset.z);
      float scale = owner instanceof SasakiKojiroEntity ? 0.926F : 1.0F;
      sample.scale(-scale, -scale, scale);
      sample.translate(0.0F, -1.501F, 0.0F);
      HumanoidArm arm = owner.getMainArm();
      heldItemPose(sample, trailModel, arm, age);
      ItemStack stack = owner.getMainHandItem();
      boolean left = arm == HumanoidArm.LEFT;
      ItemDisplayContext display = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
      var baked = mc.getItemRenderer().getModel(stack, owner.level(), owner, 0);
      baked.getTransforms().getTransform(display).apply(left, sample);
      Vec3 localTip = heldItemBladeTip(stack);
      sample.translate(-0.5F, -0.5F, -0.5F);
      if (RenderUtil.getGeoModelForItem(stack) != null) sample.translate(0.5F, 0.51F, 0.5F);
      Vector3f tip = sample.last().pose().transformPosition((float)localTip.x, (float)localTip.y, (float)localTip.z, new Vector3f());
      return new Vec3(tip.x, tip.y, tip.z);
   }

   public static Vec3 heldItemBladeTip(ItemStack stack) {
      return stack == null || stack.isEmpty() ? Vec3.ZERO
         : BLADE_TIPS.computeIfAbsent(stack.getItem(), ignored -> modelBladeTip(stack));
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static Vec3 modelBladeTip(ItemStack stack) {
      software.bernie.geckolib.model.GeoModel geo = RenderUtil.getGeoModelForItem(stack);
      if (geo == null) return new Vec3(0.94, 0.94, 0.5);
      // These swords have an idle-only geometry; the farthest baked vertex is their blade tip.
      var baked = geo.getBakedModel(geo.getModelResource((software.bernie.geckolib.animatable.GeoAnimatable)stack.getItem()));
      Vector3f tip = new Vector3f();
      PoseStack geometry = new PoseStack();
      for (GeoBone bone : baked.topLevelBones()) findBladeTip(geometry, bone, tip);
      return new Vec3(tip.x, tip.y, tip.z);
   }

   private static void findBladeTip(PoseStack geometry, GeoBone bone, Vector3f tip) {
      geometry.pushPose();
      RenderUtil.prepMatrixForBone(geometry, bone);
      for (var cube : bone.getCubes()) {
         geometry.pushPose();
         RenderUtil.translateToPivotPoint(geometry, cube);
         RenderUtil.rotateMatrixAroundCube(geometry, cube);
         RenderUtil.translateAwayFromPivotPoint(geometry, cube);
         for (var quad : cube.quads()) {
            if (quad == null) continue;
            for (var vertex : quad.vertices()) {
               Vector3f point = geometry.last().pose().transformPosition(vertex.position(), new Vector3f());
               if (point.lengthSquared() > tip.lengthSquared()) tip.set(point);
            }
         }
         geometry.popPose();
      }
      for (GeoBone child : bone.getChildBones()) findBladeTip(geometry, child, tip);
      geometry.popPose();
   }

   /** The three imported swords seed three authored forward curves instead of replaying their downward pose arc. */
   public static List<Vec3> bladeTrail(LivingEntity owner, int blade, float age) {
      Vec3 bladeStart = bladeTip(owner, blade, 0.0F);
      return SasakiTsubameSlashRenderer.trailPath(blade, age, bladeStart);
   }

   public static void applyMainPose(net.minecraft.client.model.HumanoidModel<?> target, float seconds) {
      if (target == null || !ensureAnimationLoaded(Minecraft.getInstance())) return;
      float time = Mth.clamp(seconds, 0.0F, SOURCE_ANIMATION_SECONDS);
      for (ModelPart part : java.util.List.of(target.body, target.head, target.rightArm, target.leftArm,
         target.rightLeg, target.leftLeg)) part.resetPose();
      apply(target.body, "Waist", time);
      apply(target.head, "Head", time);
      apply(target.rightArm, "Right Arm", time);
      apply(target.leftArm, "Left Arm", time);
      apply(target.rightLeg, "Right Leg", time);
      apply(target.leftLeg, "Left Leg", time);
      faceForward(target);
      if (target instanceof PlayerModel<?> playerModel) copyClothes(playerModel);
   }

   public static boolean applyOwnerPose(LivingEntity owner, HumanoidModel<?> target, float partialTick) {
      float age = castAge(owner, partialTick);
      if (age < 0.0F) return false;
      applyMainPose(target, animationTime(age));
      return true;
   }

   public static float facingYaw(LivingEntity owner, float fallback) {
      return castAge(owner, 0.0F) >= 0.0F ? CASTS.get(owner.getUUID()).yaw : fallback;
   }

   private static float castAge(LivingEntity owner, float partialTick) {
      ensureLevel();
      if (castLevel == null || owner.level() != castLevel || !owner.isAlive()) return -1.0F;
      Cast cast = CASTS.get(owner.getUUID());
      if (cast == null) return -1.0F;
      float age = castLevel.getGameTime() - cast.started + partialTick;
      return age >= 0.0F && age < 18.0F ? age : -1.0F;
   }

   /** Keep the animated grip position, but aim the blade in the common cast frame. */
   public static boolean renderHeldItem(LivingEntity owner, HumanoidModel<?> target, HumanoidArm arm,
                                        ItemStack stack, ItemDisplayContext display, PoseStack poses,
                                        MultiBufferSource buffers, int light) {
      float age = castAge(owner, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
      if (age < 0.0F || stack.isEmpty()) return false;
      boolean left = arm == HumanoidArm.LEFT;
      poses.pushPose();
      heldItemPose(poses, target, arm, age);
      Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
         .renderItem(owner, stack, display, left, poses, buffers, light);
      poses.popPose();
      return true;
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
         HumanoidArm arm = cast.owner.getMainArm();
         ItemStack heldItem = cast.owner.getMainHandItem();
         if (!heldItem.isEmpty()) {
            poses.pushPose();
            poses.translate(cast.origin.x - camera.x, cast.origin.y - camera.y, cast.origin.z - camera.z);
            poses.mulPose(Axis.YP.rotationDegrees(180.0F - cast.yaw));
            Vec3 localCamera = camera.subtract(cast.origin).yRot((cast.yaw - 180.0F) * Mth.DEG_TO_RAD);
            for (int blade = 1; blade <= 3; blade++) {
               SasakiTsubameSlashRenderer.render(poses, buffers, age, bladeTrail(cast.owner, blade, age), localCamera);
            }
            poses.popPose();
         }
         for (int shadow = 2; shadow <= 3; shadow++) {
            float scale = cast.owner instanceof SasakiKojiroEntity ? 0.926F : 1.0F;
            pose(model, shadow, seconds);
            Vec3 offset = shadowOffset(shadow, seconds);
            poses.pushPose();
            poses.translate(cast.origin.x - camera.x, cast.origin.y - camera.y, cast.origin.z - camera.z);
            poses.mulPose(Axis.YP.rotationDegrees(180.0F - cast.yaw));
            poses.translate(offset.x, offset.y, offset.z);
            poses.scale(-scale, -scale, scale);
            poses.translate(0.0F, -1.501F, 0.0F);
            int tint = FastColor.ARGB32.color((int)(alpha * 255.0F), 176, 215, 255);
            model.renderToBuffer(poses, buffers.getBuffer(type), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
            buffers.endBatch(type);
            renderArmor(cast.owner, mc, poses, partialTick, tint);
            if (!heldItem.isEmpty()) {
               renderHeldItem(cast.owner, model, arm, heldItem,
                  arm == HumanoidArm.LEFT ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                  poses, buffers, LightTexture.FULL_BRIGHT);
            }
            poses.popPose();
         }
         buffers.endBatch(type);
      }
   }

   public static Vec3 shadowOffset(int shadow, float seconds) {
      if (!ensureAnimationLoaded(Minecraft.getInstance())) return Vec3.ZERO;
      // Source rigs are staged apart in Blockbench; each cast starts at the caster.
      Vec3 delta = channel("bone" + shadow, "position", seconds)
         .subtract(channel("bone" + shadow, "position", 0.0F));
      return new Vec3(delta.x / 16.0, -delta.y / 16.0, delta.z / 16.0);
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

   private static void pose(PlayerModel<LivingEntity> model, int shadow, float seconds) {
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
      faceForward(model);
      copyClothes(model);
   }

   private static void faceForward(HumanoidModel<?> target) {
      target.body.yRot = 0.0F;
      target.head.yRot = 0.0F;
      target.hat.copyFrom(target.head);
   }

   private static void copyClothes(PlayerModel<?> target) {
      target.hat.copyFrom(target.head);
      target.jacket.copyFrom(target.body);
      target.rightSleeve.copyFrom(target.rightArm);
      target.leftSleeve.copyFrom(target.leftArm);
      target.rightPants.copyFrom(target.rightLeg);
      target.leftPants.copyFrom(target.leftLeg);
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
