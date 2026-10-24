package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaExcaliburAction;

/** Client-only Saber NP pose: compress the blade in front, lift it, then hold the release through the beam. */
public final class ArtoriaExcaliburAnimationClient {
   private static final ResourceLocation PLAYER_ANIMATION_LAYER = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "sasaki_kojiro_action");
   private static final Map<UUID, Playback> PLAYBACKS = new HashMap<>();
   private static final Map<ArtoriaExcaliburAction, KeyframeAnimationPlayer> SAMPLES = new java.util.EnumMap<>(ArtoriaExcaliburAction.class);
   private static net.minecraft.client.multiplayer.ClientLevel activeLevel;

   private ArtoriaExcaliburAnimationClient() { }

   public static void play(UUID entityId, int actionId, long startTick) {
      var level = Minecraft.getInstance().level;
      if (level == null || entityId == null) return;
      if (activeLevel != level) { PLAYBACKS.clear(); activeLevel = level; }
      ArtoriaExcaliburAction action = ArtoriaExcaliburAction.byId(actionId);
      if (action == null || action == ArtoriaExcaliburAction.CANCEL) {
         PLAYBACKS.remove(entityId);
         for (AbstractClientPlayer player : level.players()) {
            if (entityId.equals(player.getUUID())
               && PlayerAnimationAccess.getPlayerAssociatedData(player).get(PLAYER_ANIMATION_LAYER) instanceof ModifierLayer<?> layer)
               layer.setAnimation(null);
         }
         return;
      }
      PLAYBACKS.put(entityId, new Playback(action, startTick));
      for (AbstractClientPlayer player : level.players()) {
         if (!entityId.equals(player.getUUID())) continue;
         if (PlayerAnimationAccess.getPlayerAssociatedData(player).get(PLAYER_ANIMATION_LAYER) instanceof ModifierLayer<?> raw
            && PlayerAnimationRegistry.getAnimation(animationId(action)) instanceof KeyframeAnimation animation) {
            @SuppressWarnings("unchecked") ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)raw;
            layer.setAnimation(new KeyframeAnimationPlayer(animation));
         }
         return;
      }
   }

   public static void applyPose(LivingEntity owner, HumanoidModel<?> model, float partialTick) {
      var level = Minecraft.getInstance().level;
      if (level == null || owner == null) return;
      if (activeLevel != level) { PLAYBACKS.clear(); activeLevel = level; }
      Playback playback = PLAYBACKS.get(owner.getUUID());
      if (playback == null) return;
      float age = level.getGameTime() - playback.startTick + partialTick;
      if (age < 0 || age >= playback.action.duration()) { PLAYBACKS.remove(owner.getUUID()); return; }
      KeyframeAnimationPlayer sample = sample(playback.action, age);
      if (sample != null) applySample(model, sample);
   }

   public static boolean renderHeldItem(LivingEntity owner, HumanoidModel<?> model, HumanoidArm arm, ItemStack stack,
                                        ItemDisplayContext display, PoseStack poses, MultiBufferSource buffers, int light) {
      if (owner == null || stack.isEmpty() || owner instanceof AbstractClientPlayer) return false;
      var level = Minecraft.getInstance().level;
      Playback playback = PLAYBACKS.get(owner.getUUID());
      if (level == null || playback == null) return false;
      float age = level.getGameTime() - playback.startTick + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
      KeyframeAnimationPlayer sample = sample(playback.action, age);
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

   private static KeyframeAnimationPlayer sample(ArtoriaExcaliburAction action, float age) {
      if (age < 0 || age >= action.duration()) return null;
      if (!(PlayerAnimationRegistry.getAnimation(animationId(action)) instanceof KeyframeAnimation animation)) return null;
      int tick = Math.max(0, (int)age);
      KeyframeAnimationPlayer player = SAMPLES.get(action);
      if (player == null || player.getData() != animation || player.getTick() != tick) {
         player = new KeyframeAnimationPlayer(animation, tick);
         SAMPLES.put(action, player);
      }
      player.setupAnim(age - tick);
      return player;
   }

   private static ResourceLocation animationId(ArtoriaExcaliburAction action) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", action.animation());
   }

   private static void applySample(HumanoidModel<?> model, KeyframeAnimationPlayer sample) {
      apply(sample, "head", model.head);
      apply(sample, "torso", model.body);
      apply(sample, "rightArm", model.rightArm);
      apply(sample, "leftArm", model.leftArm);
      apply(sample, "rightLeg", model.rightLeg);
      apply(sample, "leftLeg", model.leftLeg);
      model.hat.copyFrom(model.head);
      if (model instanceof net.minecraft.client.model.PlayerModel<?> player) {
         player.jacket.copyFrom(player.body);
         player.rightSleeve.copyFrom(player.rightArm);
         player.leftSleeve.copyFrom(player.leftArm);
         player.rightPants.copyFrom(player.rightLeg);
         player.leftPants.copyFrom(player.leftLeg);
      }
   }

   private static void apply(KeyframeAnimationPlayer sample, String name, ModelPart part) {
      part.resetPose();
      Vec3f position = sample.get3DTransform(name, TransformType.POSITION, 0.0F, new Vec3f(part.x, part.y, part.z));
      Vec3f rotation = sample.get3DTransform(name, TransformType.ROTATION, 0.0F, Vec3f.ZERO);
      part.setPos(position.getX(), position.getY(), position.getZ());
      part.setRotation(rotation.getX(), rotation.getY(), rotation.getZ());
   }

   private record Playback(ArtoriaExcaliburAction action, long startTick) { }
}
