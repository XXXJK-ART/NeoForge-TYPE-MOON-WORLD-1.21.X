package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.api.TransformType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.YajiaoQiangItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

/** Uses the displayed hand, item transform and blade mesh for both the weapon and its light. */
public final class ZhaoYunSpearAnimationClient {
   private ZhaoYunSpearAnimationClient() { }

   public static boolean renderHeldItem(LivingEntity owner, HumanoidModel<?> model, HumanoidArm arm,
      ItemStack stack, ItemDisplayContext display, PoseStack poses, MultiBufferSource buffers, int light) {
      if (!stack.is(ModItems.YAJIAO_QIANG.get()) || arm != owner.getMainArm() || !owner.isAlive()) return false;
      var mc = Minecraft.getInstance();
      float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
      var active = ServantBaseAnimationClient.activeAction(owner, partial);
      if (active == null || !ServantBaseAction.isZhaoYunSpear(active.action())
         || owner instanceof Player player && ServantGuardPoseClient.isGuarding(player)
         || owner instanceof ServantEntity servant && (servant.getCombatPose() != 0 || servant.isPerformingAction())) return false;
      poses.pushPose();
      model.translateToHand(arm, poses);
      poses.mulPose(Axis.XP.rotationDegrees(-90));
      poses.mulPose(Axis.YP.rotationDegrees(180));
      boolean left = arm == HumanoidArm.LEFT;
      poses.translate(left ? -0.0625 : 0.0625, 0.125, -0.625);
      var position = ServantBaseAnimationClient.itemTransform(owner, "rightItem", TransformType.POSITION, partial);
      var rotation = ServantBaseAnimationClient.itemTransform(owner, "rightItem", TransformType.ROTATION, partial);
      poses.translate((left ? -position.getX() : position.getX()) / 16, position.getY() / 16, position.getZ() / 16);
      poses.mulPose(Axis.ZP.rotation(left ? -rotation.getZ() : rotation.getZ()));
      poses.mulPose(Axis.YP.rotation(left ? -rotation.getY() : rotation.getY()));
      poses.mulPose(Axis.XP.rotation(rotation.getX()));
      captureTip(owner, stack, display, left, poses, active, partial);
      mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(owner, stack, display, left, poses, buffers, light);
      poses.popPose();
      return true;
   }

   private static void captureTip(LivingEntity owner, ItemStack stack, ItemDisplayContext display, boolean left,
      PoseStack poses, ServantBaseAnimationClient.ActiveAction action, float partial) {
      var mc = Minecraft.getInstance();
      @SuppressWarnings("unchecked")
      var geo = (software.bernie.geckolib.model.GeoModel<YajiaoQiangItem>)RenderUtil.getGeoModelForItem(stack);
      if (geo == null) return;
      geo.getBakedModel(geo.getModelResource((YajiaoQiangItem)stack.getItem()));
      GeoBone blade = geo.getAnimationProcessor().getBone("ren");
      if (blade == null) return;
      Vector3f endpoint = new Vector3f();
      findBladeTip(new PoseStack(), blade, endpoint);
      poses.pushPose();
      mc.getItemRenderer().getModel(stack, owner.level(), owner, 0).getTransforms().getTransform(display).apply(left, poses);
      // GeoItemRenderer's centering, after ItemRenderer's (-0.5) translation.
      poses.translate(0, 0.01, 0);
      Vector3f point = poses.last().pose().transformPosition(endpoint, new Vector3f());
      poses.popPose();
      Vec3 world = new Vec3(point.x, point.y, point.z).add(mc.gameRenderer.getMainCamera().getPosition());
      ZhaoYunSpearTrailClient.capture(owner, action, world, partial);
   }

   private static void findBladeTip(PoseStack poses, GeoBone bone, Vector3f endpoint) {
      poses.pushPose();
      RenderUtil.prepMatrixForBone(poses, bone);
      for (var cube : bone.getCubes()) {
         poses.pushPose();
         RenderUtil.translateToPivotPoint(poses, cube);
         RenderUtil.rotateMatrixAroundCube(poses, cube);
         RenderUtil.translateAwayFromPivotPoint(poses, cube);
         for (var quad : cube.quads()) {
            if (quad == null) continue;
            for (var vertex : quad.vertices()) {
               Vector3f point = poses.last().pose().transformPosition(vertex.position(), new Vector3f());
               if (point.lengthSquared() > endpoint.lengthSquared()) endpoint.set(point);
            }
         }
         poses.popPose();
      }
      for (var child : bone.getChildBones()) findBladeTip(poses, child, endpoint);
      poses.popPose();
   }
}
