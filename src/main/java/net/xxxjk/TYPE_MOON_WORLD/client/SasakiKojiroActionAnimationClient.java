package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.UUID;
import java.util.EnumMap;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;
import org.joml.Vector3f;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SasakiKojiroActionAnimationClient {
   private static final ResourceLocation PLAYER_ANIMATION_LAYER = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "sasaki_kojiro_action");
   private static final EnumMap<SasakiKojiroAction, KeyframeAnimationPlayer> SAMPLES = new EnumMap<>(SasakiKojiroAction.class);
   private static PlayerModel<LivingEntity> bladeTipModel;

   private SasakiKojiroActionAnimationClient() { }

   @SubscribeEvent
   public static void registerPlayerAnimationLayer(FMLClientSetupEvent event) {
      event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
         PLAYER_ANIMATION_LAYER, 1000, player -> ServantActionAnimationLayer.create()));
   }

   public static void play(UUID playerId, int actionId) {
      Minecraft minecraft = Minecraft.getInstance();
      SasakiKojiroAction action = SasakiKojiroAction.byNetworkId(actionId);
      if (minecraft.level == null || playerId == null || action == null) return;
      for (var entity : minecraft.level.entitiesForRendering()) {
         if (entity instanceof net.minecraft.world.entity.LivingEntity owner && playerId.equals(owner.getUUID())) {
            SasakiSwordTrailClient.begin(owner, action);
            break;
         }
      }
      if (action == SasakiKojiroAction.TSUBAME_GAESHI) {
         for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity owner && playerId.equals(owner.getUUID())) {
               SasakiTsubameAfterimageClient.begin(owner);
               break;
            }
         }
      }
      for (AbstractClientPlayer player : minecraft.level.players()) {
         if (playerId.equals(player.getUUID())) {
            play(player, action);
            return;
         }
      }
   }

   private static void play(AbstractClientPlayer player, SasakiKojiroAction action) {
      if (!(PlayerAnimationAccess.getPlayerAssociatedData(player).get(PLAYER_ANIMATION_LAYER) instanceof ModifierLayer<?> rawLayer)) return;
      ResourceLocation animationId = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, action.playerAnimationPath());
      if (!(PlayerAnimationRegistry.getAnimation(animationId) instanceof KeyframeAnimation animation)) return;
      @SuppressWarnings("unchecked")
      ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)rawLayer;
      if (action == SasakiKojiroAction.TSUBAME_GAESHI) {
         // The owner and both afterimages now sample the same source pose directly.
         layer.setAnimation(null);
         return;
      }
      layer.setAnimation(new KeyframeAnimationPlayer(animation));
   }

   /** NPCs sample the same imported PlayerAnimator data as transformed players. */
   public static void applyImportedPose(HumanoidModel<?> model, SasakiKojiroAction action, float age) {
      KeyframeAnimationPlayer sample = sample(action, age);
      if (sample == null) return;
      applySample(model, sample);
   }

   private static void applySample(HumanoidModel<?> model, KeyframeAnimationPlayer sample) {
      apply(sample, "head", model.head);
      apply(sample, "torso", model.body);
      apply(sample, "rightArm", model.rightArm);
      apply(sample, "leftArm", model.leftArm);
      apply(sample, "rightLeg", model.rightLeg);
      apply(sample, "leftLeg", model.leftLeg);
      model.hat.copyFrom(model.head);
      if (model instanceof PlayerModel<?> player) {
         player.jacket.copyFrom(player.body);
         player.rightSleeve.copyFrom(player.rightArm);
         player.leftSleeve.copyFrom(player.leftArm);
         player.rightPants.copyFrom(player.rightLeg);
         player.leftPants.copyFrom(player.leftLeg);
      }
   }

   /** Samples the held sword's rendered tip in the owner's model space for the active swing trail. */
   public static Vec3 bladeTip(LivingEntity owner, SasakiKojiroAction action, float age) {
      if (owner == null || owner.getMainHandItem().isEmpty()) return null;
      Minecraft mc = Minecraft.getInstance();
      KeyframeAnimationPlayer sample = sample(action, age);
      if (sample == null) return null;
      if (bladeTipModel == null) bladeTipModel = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      PlayerModel<LivingEntity> model = bladeTipModel;
      model.young = false;
      model.riding = false;
      model.crouching = false;
      model.attackTime = 0.0F;
      model.swimAmount = 0.0F;
      applySample(model, sample);

      ItemStack stack = owner.getMainHandItem();
      HumanoidArm arm = owner.getMainArm();
      boolean left = arm == HumanoidArm.LEFT;
      ItemDisplayContext display = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
      Vec3f position = sample.get3DTransform(left ? "leftItem" : "rightItem", TransformType.POSITION, 0.0F, Vec3f.ZERO);
      Vec3f rotation = sample.get3DTransform(left ? "leftItem" : "rightItem", TransformType.ROTATION, 0.0F, Vec3f.ZERO);
      PoseStack poses = new PoseStack();
      float scale = owner instanceof SasakiKojiroEntity ? 0.926F : 1.0F;
      poses.scale(-scale, -scale, scale);
      poses.translate(0.0F, -1.501F, 0.0F);
      model.translateToHand(arm, poses);
      poses.mulPose(Axis.XP.rotationDegrees(-90.0F));
      poses.mulPose(Axis.YP.rotationDegrees(180.0F));
      poses.translate(left ? -0.0625F : 0.0625F, 0.125F, -0.625F);
      poses.translate(position.getX() / 16.0F, position.getY() / 16.0F, position.getZ() / 16.0F);
      poses.mulPose(Axis.ZP.rotation(rotation.getZ()));
      poses.mulPose(Axis.YP.rotation(rotation.getY()));
      poses.mulPose(Axis.XP.rotation(rotation.getX()));
      var itemModel = mc.getItemRenderer().getModel(stack, owner.level(), owner, 0);
      itemModel.getTransforms().getTransform(display).apply(left, poses);
      poses.translate(-0.5F, -0.5F, -0.5F);
      if (software.bernie.geckolib.util.RenderUtil.getGeoModelForItem(stack) != null) poses.translate(0.5F, 0.51F, 0.5F);
      Vec3 localTip = SasakiTsubameAfterimageClient.heldItemBladeTip(stack);
      Vector3f tip = poses.last().pose().transformPosition((float)localTip.x, (float)localTip.y, (float)localTip.z, new Vector3f());
      return new Vec3(tip.x, tip.y, tip.z);
   }

   private static KeyframeAnimationPlayer sample(SasakiKojiroAction action, float age) {
      var id = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, action.playerAnimationPath());
      if (!(PlayerAnimationRegistry.getAnimation(id) instanceof KeyframeAnimation animation)) return null;
      int tick = Math.max(0, (int)age);
      KeyframeAnimationPlayer sample = SAMPLES.get(action);
      if (sample == null || sample.getData() != animation || sample.getTick() != tick) {
         sample = new KeyframeAnimationPlayer(animation, tick);
         SAMPLES.put(action, sample);
      }
      sample.setupAnim(age - tick);
      return sample;
   }

   /** PlayerAnimator already applies item tracks for players; mirror that path for the humanoid NPC. */
   public static boolean renderImportedHeldItem(LivingEntity owner, HumanoidModel<?> model, HumanoidArm arm,
                                                ItemStack stack, ItemDisplayContext display, PoseStack poses,
                                                MultiBufferSource buffers, int light) {
      if (!(owner instanceof SasakiKojiroEntity npc) || !npc.isSasakiActionActive() || stack.isEmpty()) return false;
      SasakiKojiroAction action = npc.getSasakiAction();
      if (action == SasakiKojiroAction.TSUBAME_GAESHI) return false;
      float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
      KeyframeAnimationPlayer sample = sample(action, npc.getSasakiActionAge(partial));
      if (sample == null) return false;
      String part = arm == HumanoidArm.LEFT ? "leftItem" : "rightItem";
      Vec3f position = sample.get3DTransform(part, TransformType.POSITION, 0.0F, Vec3f.ZERO);
      Vec3f rotation = sample.get3DTransform(part, TransformType.ROTATION, 0.0F, Vec3f.ZERO);
      poses.pushPose();
      model.translateToHand(arm, poses);
      poses.mulPose(Axis.XP.rotationDegrees(-90.0F));
      poses.mulPose(Axis.YP.rotationDegrees(180.0F));
      poses.translate(arm == HumanoidArm.LEFT ? -0.0625F : 0.0625F, 0.125F, -0.625F);
      poses.translate(position.getX() / 16.0F, position.getY() / 16.0F, position.getZ() / 16.0F);
      poses.mulPose(Axis.ZP.rotation(rotation.getZ()));
      poses.mulPose(Axis.YP.rotation(rotation.getY()));
      poses.mulPose(Axis.XP.rotation(rotation.getX()));
      Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
         .renderItem(owner, stack, display, arm == HumanoidArm.LEFT, poses, buffers, light);
      poses.popPose();
      return true;
   }

   private static void apply(KeyframeAnimationPlayer sample, String name, ModelPart part) {
      part.resetPose();
      Vec3f position = sample.get3DTransform(name, TransformType.POSITION, 0.0F, new Vec3f(part.x, part.y, part.z));
      Vec3f rotation = sample.get3DTransform(name, TransformType.ROTATION, 0.0F, new Vec3f(0.0F, 0.0F, 0.0F));
      part.setPos(position.getX(), position.getY(), position.getZ());
      part.setRotation(rotation.getX(), rotation.getY(), rotation.getZ());
   }
}
