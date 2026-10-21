package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.EnumMap;
import java.util.UUID;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;
import org.joml.Vector3f;

@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DiarmuidActionAnimationClient {
   private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "diarmuid_action");
   private static final EnumMap<DiarmuidAction, KeyframeAnimationPlayer> SAMPLES = new EnumMap<>(DiarmuidAction.class);
   private static PlayerModel<LivingEntity> tipModel;
   private DiarmuidActionAnimationClient() { }

   @SubscribeEvent
   public static void register(FMLClientSetupEvent event) {
      event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000, player -> ServantActionAnimationLayer.create()));
   }

   public static void play(UUID ownerId, int actionId) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || ownerId == null) return;
      DiarmuidAction action = DiarmuidAction.byNetworkId(actionId);
      for (var entity : mc.level.entitiesForRendering()) {
         if (entity instanceof LivingEntity owner && ownerId.equals(owner.getUUID())) {
            if (action == null) DiarmuidVisualsClient.stop(ownerId);
            else DiarmuidVisualsClient.begin(owner, action, mc.level.getGameTime());
            break;
         }
      }
      for (AbstractClientPlayer player : mc.level.players()) {
         if (!ownerId.equals(player.getUUID()) || !(PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof ModifierLayer<?> raw)) continue;
         @SuppressWarnings("unchecked") ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)raw;
         if (action == null) { layer.setAnimation(null); return; }
         var animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld", action.animationPath()));
         if (animation instanceof KeyframeAnimation keyframes) layer.setAnimation(new KeyframeAnimationPlayer(keyframes));
         return;
      }
   }

   private static KeyframeAnimationPlayer sample(DiarmuidAction action, float age) {
      if (action == null) return null;
      var animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld", action.animationPath()));
      if (!(animation instanceof KeyframeAnimation data)) return null;
      if (action == DiarmuidAction.FIGURE_EIGHT) age = age % action.duration();
      int tick = Math.max(0, (int)age);
      var sample = SAMPLES.get(action);
      if (sample == null || sample.getData() != data || sample.getTick() != tick) {
         sample = new KeyframeAnimationPlayer(data, tick);
         SAMPLES.put(action, sample);
      }
      sample.setupAnim(age - tick);
      return sample;
   }

   public static void applyImportedPose(HumanoidModel<?> model, DiarmuidAction action, float age) {
      var sample = sample(action, age);
      if (sample == null) return;
      apply(sample, "head", model.head);
      apply(sample, "torso", model.body);
      apply(sample, "rightArm", model.rightArm);
      apply(sample, "leftArm", model.leftArm);
      apply(sample, "rightLeg", model.rightLeg);
      apply(sample, "leftLeg", model.leftLeg);
      Vec3f root = sample.get3DTransform("body", TransformType.POSITION, 0, Vec3f.ZERO);
      for (var part : new ModelPart[]{model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg}) {
         part.x += root.getX(); part.y += root.getY(); part.z += root.getZ();
      }
      model.hat.copyFrom(model.head);
      if (model instanceof PlayerModel<?> player) {
         player.jacket.copyFrom(player.body);
         player.rightSleeve.copyFrom(player.rightArm); player.leftSleeve.copyFrom(player.leftArm);
         player.rightPants.copyFrom(player.rightLeg); player.leftPants.copyFrom(player.leftLeg);
      }
   }

   private static void apply(KeyframeAnimationPlayer sample, String name, ModelPart part) {
      part.resetPose();
      Vec3f position = sample.get3DTransform(name, TransformType.POSITION, 0, new Vec3f(part.x, part.y, part.z));
      Vec3f rotation = sample.get3DTransform(name, TransformType.ROTATION, 0, Vec3f.ZERO);
      part.setPos(position.getX(), position.getY(), position.getZ());
      part.setRotation(rotation.getX(), rotation.getY(), rotation.getZ());
   }

   private static boolean stanceIdle(LivingEntity owner) {
      if (DiarmuidVisualsClient.isAnimating(owner.getUUID())) return false;
      if (owner instanceof DiarmuidUaDuibhneEntity npc) return npc.isDoubleSpearStance();
      if (owner instanceof AbstractClientPlayer player) {
         var vars=player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "diarmuid_ua_duibhne".equals(vars.servant_card_id) && vars.servant_card_action_mode==1;
      }
      return false;
   }

   public static void applyIdlePose(LivingEntity owner, HumanoidModel<?> model) {
      if (!stanceIdle(owner)) return;
      var ready=sample(DiarmuidAction.DOUBLE_SPEAR_STANCE,6);
      if (ready==null) return;
      apply(ready,"rightArm",model.rightArm);
      apply(ready,"leftArm",model.leftArm);
      if (model instanceof PlayerModel<?> player) {
         player.rightSleeve.copyFrom(player.rightArm); player.leftSleeve.copyFrom(player.leftArm);
      }
   }

   private static void grip(HumanoidModel<?> model, HumanoidArm arm, KeyframeAnimationPlayer sample, PoseStack poses) {
      model.translateToHand(arm, poses);
      poses.mulPose(Axis.XP.rotationDegrees(-90));
      poses.mulPose(Axis.YP.rotationDegrees(180));
      poses.translate(arm == HumanoidArm.LEFT ? -0.0625 : 0.0625, 0.125, -0.625);
      String track = arm == HumanoidArm.LEFT ? "leftItem" : "rightItem";
      Vec3f position = sample.get3DTransform(track, TransformType.POSITION, 0, Vec3f.ZERO);
      Vec3f rotation = sample.get3DTransform(track, TransformType.ROTATION, 0, Vec3f.ZERO);
      poses.translate(position.getX() / 16, position.getY() / 16, position.getZ() / 16);
      poses.mulPose(Axis.ZP.rotation(rotation.getZ()));
      poses.mulPose(Axis.YP.rotation(rotation.getY()));
      poses.mulPose(Axis.XP.rotation(rotation.getX()));
   }

   public static boolean renderImportedHeldItem(LivingEntity owner, HumanoidModel<?> model, HumanoidArm arm,
      ItemStack stack, ItemDisplayContext display, PoseStack poses, MultiBufferSource buffers, int light) {
      if (stack.isEmpty()) return false;
      if (stanceIdle(owner)) return renderHeldItem(owner,model,DiarmuidAction.DOUBLE_SPEAR_STANCE,6,arm,stack,display,poses,buffers,light);
      if (!(owner instanceof DiarmuidUaDuibhneEntity npc)) return false;
      DiarmuidAction action = npc.getDiarmuidAction();
      float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
      float age = npc.getDiarmuidActionAge(partial);
      if (action == null || age < 0 || age >= action.duration()) return false;
      return renderHeldItem(owner, model, action, age, arm, stack, display, poses, buffers, light);
   }

   public static boolean renderHeldItem(LivingEntity owner, HumanoidModel<?> model, DiarmuidAction action, float age,
      HumanoidArm arm, ItemStack stack, ItemDisplayContext display, PoseStack poses, MultiBufferSource buffers, int light) {
      var sample = sample(action, age);
      if (sample == null || stack.isEmpty()) return false;
      poses.pushPose();
      grip(model, arm, sample, poses);
      Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(owner, stack, display, arm == HumanoidArm.LEFT, poses, buffers, light);
      poses.popPose();
      return true;
   }

   /** The trail samples the same model, grip, item display transform and shortened geometry. */
   public static Vec3 spearTip(LivingEntity owner, DiarmuidAction action, float age, InteractionHand hand) {
      ItemStack stack = owner.getItemInHand(hand);
      if (!(stack.getItem() instanceof DiarmuidSpearItem spear)) return null;
      var sample = sample(action, age);
      if (sample == null) return null;
      Minecraft mc = Minecraft.getInstance();
      if (tipModel == null) tipModel = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      applyImportedPose(tipModel, action, age);
      HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? owner.getMainArm() : owner.getMainArm().getOpposite();
      boolean left = arm == HumanoidArm.LEFT;
      ItemDisplayContext display = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
      PoseStack poses = new PoseStack();
      poses.scale(-1, -1, 1);
      poses.translate(0, -1.501, 0);
      grip(tipModel, arm, sample, poses);
      mc.getItemRenderer().getModel(stack, owner.level(), owner, 0).getTransforms().getTransform(display).apply(left, poses);
      poses.translate(-0.5, -0.5, -0.5);
      poses.translate(0.5, 0.51, 0.5);
      Vec3 meshTip = spearMeshTip(stack, spear);
      if (meshTip == null) return null;
      Vector3f tip = poses.last().pose().transformPosition((float)meshTip.x, (float)meshTip.y, (float)meshTip.z, new Vector3f());
      return new Vec3(tip.x, tip.y, tip.z);
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static Vec3 spearMeshTip(ItemStack stack, DiarmuidSpearItem spear) {
      software.bernie.geckolib.model.GeoModel geo = software.bernie.geckolib.util.RenderUtil.getGeoModelForItem(stack);
      if (geo == null) return null;
      var baked = geo.getBakedModel(geo.getModelResource(spear));
      var root = geo.getAnimationProcessor().getBone("bone");
      if (root != null) root.setScaleZ(spear.spearType() == DiarmuidSpearItem.SpearType.GAE_BUIDHE ? (float)DiarmuidAction.YELLOW_MODEL_LENGTH : 1);
      Vector3f tip = new Vector3f(0, 0, -Float.MAX_VALUE);
      PoseStack geometry = new PoseStack();
      for (software.bernie.geckolib.cache.object.GeoBone bone : baked.topLevelBones()) findSpearTip(geometry, bone, tip);
      return Float.isFinite(tip.z) && tip.z > -Float.MAX_VALUE ? new Vec3(tip.x, tip.y, tip.z) : null;
   }

   private static void findSpearTip(PoseStack geometry, software.bernie.geckolib.cache.object.GeoBone bone, Vector3f tip) {
      geometry.pushPose();
      software.bernie.geckolib.util.RenderUtil.prepMatrixForBone(geometry, bone);
      for (var cube : bone.getCubes()) {
         geometry.pushPose();
         software.bernie.geckolib.util.RenderUtil.translateToPivotPoint(geometry, cube);
         software.bernie.geckolib.util.RenderUtil.rotateMatrixAroundCube(geometry, cube);
         software.bernie.geckolib.util.RenderUtil.translateAwayFromPivotPoint(geometry, cube);
         for (var quad : cube.quads()) {
            if (quad == null) continue;
            for (var vertex : quad.vertices()) {
               Vector3f point = geometry.last().pose().transformPosition(vertex.position(), new Vector3f());
               if (point.z > tip.z) tip.set(point);
            }
         }
         geometry.popPose();
      }
      for (var child : bone.getChildBones()) findSpearTip(geometry, child, tip);
      geometry.popPose();
   }
}
