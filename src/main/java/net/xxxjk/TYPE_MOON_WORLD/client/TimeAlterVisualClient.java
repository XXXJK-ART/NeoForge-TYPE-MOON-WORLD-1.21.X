package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.special.MagicTimeAlter;
import net.xxxjk.TYPE_MOON_WORLD.magic.special.TimeAlterRateMath;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class TimeAlterVisualClient {
   private static final int HISTORY_DELAY_TICKS = 2;
   private static final int AFTERIMAGE_LIFETIME_TICKS = 40;
   private static final int AFTERIMAGE_INTERVAL_TICKS = 1;
   private static final double MOVEMENT_EPSILON_SQR = 0.0025;
   private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
   private static final Map<UUID, ActiveState> ACTIVE = new HashMap<>();
   private static final Map<UUID, ArrayDeque<PoseSample>> HISTORY = new HashMap<>();
   private static final Map<UUID, Double> CLIENT_ITEM_USE_PROGRESS = new HashMap<>();
   private static final List<Afterimage> AFTERIMAGES = new ArrayList<>();
   private static PlayerModel<AbstractClientPlayer> ghostModel;
   private static long clientTick;

   private TimeAlterVisualClient() {
   }

   public static void applyState(UUID playerId, boolean active, int mode, int remainingTicks, double actionRate) {
      if (playerId == null) {
         return;
      }
      if (!active || remainingTicks <= 0) {
         ACTIVE.remove(playerId);
         HISTORY.remove(playerId);
         CLIENT_ITEM_USE_PROGRESS.remove(playerId);
         return;
      }
      ACTIVE.put(
         playerId,
         new ActiveState(mode, TimeAlterRateMath.effectiveActionRate(mode, actionRate), clientTick + Math.max(1, remainingTicks))
      );
      HISTORY.remove(playerId);
   }

   public static double getEffectiveActionRate(UUID playerId) {
      ActiveState state = ACTIVE.get(playerId);
      return state != null && state.expiresAtTick > clientTick ? state.actionRate : 1.0;
   }

   @SubscribeEvent
   public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
      if (event.getEntity() instanceof AbstractClientPlayer player) {
         CLIENT_ITEM_USE_PROGRESS.remove(player.getUUID());
      }
   }

   @SubscribeEvent
   public static void onItemUseTick(LivingEntityUseItemEvent.Tick event) {
      if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
         return;
      }
      double actionRate = getEffectiveActionRate(player.getUUID());
      if (Math.abs(actionRate - 1.0) <= 1.0E-6) {
         CLIENT_ITEM_USE_PROGRESS.remove(player.getUUID());
         return;
      }
      TimeAlterRateMath.UseAdvance advance = TimeAlterRateMath.advanceItemUse(
         CLIENT_ITEM_USE_PROGRESS.getOrDefault(player.getUUID(), 0.0), actionRate
      );
      CLIENT_ITEM_USE_PROGRESS.put(player.getUUID(), advance.remainingProgress());
      event.setDuration(TimeAlterRateMath.durationBeforeVanillaDecrement(event.getDuration(), advance.elapsedUseTicks()));
   }

   @SubscribeEvent
   public static void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
      if (event.getEntity() instanceof AbstractClientPlayer player) {
         CLIENT_ITEM_USE_PROGRESS.remove(player.getUUID());
      }
   }

   @SubscribeEvent
   public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
      if (event.getEntity() instanceof AbstractClientPlayer player) {
         CLIENT_ITEM_USE_PROGRESS.remove(player.getUUID());
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      Minecraft minecraft = Minecraft.getInstance();
      clientTick++;
      AFTERIMAGES.removeIf(afterimage -> --afterimage.remainingTicks <= 0);
      if (minecraft.level == null) {
         ACTIVE.clear();
         HISTORY.clear();
         CLIENT_ITEM_USE_PROGRESS.clear();
         AFTERIMAGES.clear();
         return;
      }

      Iterator<Map.Entry<UUID, ActiveState>> iterator = ACTIVE.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<UUID, ActiveState> entry = iterator.next();
         if (entry.getValue().expiresAtTick <= clientTick) {
            HISTORY.remove(entry.getKey());
            CLIENT_ITEM_USE_PROGRESS.remove(entry.getKey());
            iterator.remove();
            continue;
         }
         if (entry.getValue().mode != MagicTimeAlter.MODE_ACCEL) {
            HISTORY.remove(entry.getKey());
            continue;
         }

         AbstractClientPlayer player = findPlayer(minecraft, entry.getKey());
         if (player == null || !player.isAlive()) {
            HISTORY.remove(entry.getKey());
            continue;
         }
         PoseSample current = capture(player, minecraft);
         if (current == null) {
            continue;
         }
         ArrayDeque<PoseSample> history = HISTORY.computeIfAbsent(entry.getKey(), ignored -> new ArrayDeque<>());
         history.addLast(current);
         while (history.size() > HISTORY_DELAY_TICKS + 1) {
            history.removeFirst();
         }
         if (history.size() == HISTORY_DELAY_TICKS + 1
            && clientTick % AFTERIMAGE_INTERVAL_TICKS == 0
            && current.distanceToSqr(history.peekFirst()) >= MOVEMENT_EPSILON_SQR) {
            AFTERIMAGES.add(new Afterimage(history.peekFirst(), AFTERIMAGE_LIFETIME_TICKS));
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevel(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || AFTERIMAGES.isEmpty()) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.player == null) {
         return;
      }
      PlayerModel<AbstractClientPlayer> model = getGhostModel(minecraft);
      if (model == null) {
         return;
      }

      PoseStack poseStack = event.getPoseStack();
      var camera = event.getCamera().getPosition();
      BufferSource source = minecraft.renderBuffers().bufferSource();
      RenderType renderType = RenderType.entityTranslucent(WHITE_TEXTURE);
      VertexConsumer consumer = source.getBuffer(renderType);
      int color = FastColor.ARGB32.color(96, 255, 18, 18);

      poseStack.pushPose();
      poseStack.translate(-camera.x, -camera.y, -camera.z);
      for (Afterimage afterimage : AFTERIMAGES) {
         PoseSample sample = afterimage.sample;
         sample.pose.apply(model);
         showBaseModelOnly(model);
         poseStack.pushPose();
         poseStack.translate(sample.x, sample.y, sample.z);
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - sample.bodyYaw));
         poseStack.scale(-1.0F, -1.0F, 1.0F);
         poseStack.translate(0.0F, -1.501F, 0.0F);
         model.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, 0, color);
         poseStack.popPose();
      }
      poseStack.popPose();
      source.endBatch(renderType);
   }

   @SubscribeEvent
   public static void onRenderGui(RenderGuiEvent.Pre event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null || !isActive(minecraft.player.getUUID())) {
         return;
      }
      GuiGraphics graphics = event.getGuiGraphics();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0x38FF1010);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
   }

   private static boolean isActive(UUID playerId) {
      ActiveState state = ACTIVE.get(playerId);
      return state != null && state.expiresAtTick > clientTick;
   }

   private static AbstractClientPlayer findPlayer(Minecraft minecraft, UUID playerId) {
      if (minecraft.level == null) {
         return null;
      }
      for (AbstractClientPlayer player : minecraft.level.players()) {
         if (playerId.equals(player.getUUID())) {
            return player;
         }
      }
      return null;
   }

   private static PoseSample capture(AbstractClientPlayer player, Minecraft minecraft) {
      PlayerModel<AbstractClientPlayer> model = getGhostModel(minecraft);
      if (model == null) {
         return null;
      }
      if (minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
         PlayerModel<AbstractClientPlayer> sourceModel = renderer.getModel();
         model.leftArmPose = sourceModel.leftArmPose;
         model.rightArmPose = sourceModel.rightArmPose;
      }
      model.attackTime = player.getAttackAnim(1.0F);
      model.riding = player.isPassenger();
      model.young = false;
      model.crouching = player.isCrouching();
      model.swimAmount = player.getSwimAmount(1.0F);
      float limbAmount = Math.min(player.walkAnimation.speed(1.0F), 1.0F);
      float limbSwing = player.walkAnimation.position(1.0F);
      float bodyYaw = Mth.rotLerp(1.0F, player.yBodyRotO, player.yBodyRot);
      float headYaw = Mth.wrapDegrees(Mth.rotLerp(1.0F, player.yHeadRotO, player.yHeadRot) - bodyYaw);
      float pitch = Mth.lerp(1.0F, player.xRotO, player.getXRot());
      model.prepareMobModel(player, limbSwing, limbAmount, 1.0F);
      model.setupAnim(player, limbSwing, limbAmount, player.tickCount + 1.0F, headYaw, pitch);
      return new PoseSample(player.getX(), player.getY(), player.getZ(), bodyYaw, HumanoidPose.capture(model));
   }

   private static PlayerModel<AbstractClientPlayer> getGhostModel(Minecraft minecraft) {
      if (ghostModel == null) {
         ghostModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      }
      return ghostModel;
   }

   private static void showBaseModelOnly(PlayerModel<?> model) {
      model.setAllVisible(false);
      model.head.visible = true;
      model.body.visible = true;
      model.rightArm.visible = true;
      model.leftArm.visible = true;
      model.rightLeg.visible = true;
      model.leftLeg.visible = true;
   }

   private record ActiveState(int mode, double actionRate, long expiresAtTick) {
   }

   private record PoseSample(double x, double y, double z, float bodyYaw, HumanoidPose pose) {
      private double distanceToSqr(PoseSample other) {
         if (other == null) {
            return 0.0;
         }
         double dx = this.x - other.x;
         double dy = this.y - other.y;
         double dz = this.z - other.z;
         return dx * dx + dy * dy + dz * dz;
      }
   }

   private static final class Afterimage {
      private final PoseSample sample;
      private int remainingTicks;

      private Afterimage(PoseSample sample, int remainingTicks) {
         this.sample = sample;
         this.remainingTicks = remainingTicks;
      }
   }

   private record HumanoidPose(
      PartTransform head,
      PartTransform body,
      PartTransform rightArm,
      PartTransform leftArm,
      PartTransform rightLeg,
      PartTransform leftLeg
   ) {
      private static HumanoidPose capture(PlayerModel<?> model) {
         return new HumanoidPose(
            PartTransform.capture(model.head),
            PartTransform.capture(model.body),
            PartTransform.capture(model.rightArm),
            PartTransform.capture(model.leftArm),
            PartTransform.capture(model.rightLeg),
            PartTransform.capture(model.leftLeg)
         );
      }

      private void apply(PlayerModel<?> model) {
         this.head.apply(model.head);
         this.body.apply(model.body);
         this.rightArm.apply(model.rightArm);
         this.leftArm.apply(model.leftArm);
         this.rightLeg.apply(model.rightLeg);
         this.leftLeg.apply(model.leftLeg);
      }
   }

   private record PartTransform(float x, float y, float z, float xRot, float yRot, float zRot, float xScale, float yScale, float zScale) {
      private static PartTransform capture(ModelPart part) {
         return new PartTransform(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale);
      }

      private void apply(ModelPart part) {
         part.x = this.x;
         part.y = this.y;
         part.z = this.z;
         part.xRot = this.xRot;
         part.yRot = this.yRot;
         part.zRot = this.zRot;
         part.xScale = this.xScale;
         part.yScale = this.yScale;
         part.zScale = this.zScale;
      }
   }
}
