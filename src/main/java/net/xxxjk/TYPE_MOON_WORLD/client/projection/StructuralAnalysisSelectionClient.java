package net.xxxjk.TYPE_MOON_WORLD.client.projection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.event.ScreenEvent.Opening;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.StructuralSelectionConfirmScreen;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.StructuralSelectionNamingScreen;
import net.xxxjk.TYPE_MOON_WORLD.network.SaveStructuralSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(
   modid = "typemoonworld",
   value = {Dist.CLIENT}
)
public final class StructuralAnalysisSelectionClient {
   private static boolean active = false;
   private static StructuralAnalysisSelectionClient.SelectionStage stage = StructuralAnalysisSelectionClient.SelectionStage.LINE;
   private static Axis planeAxis = Axis.Y;
   private static BlockPos startPos = BlockPos.ZERO;
   private static BlockPos facePos = BlockPos.ZERO;
   private static BlockPos depthPos = BlockPos.ZERO;
   private static StructuralAnalysisSelectionClient.SavedSelection pendingSelection = null;
   private static StructuralAnalysisSelectionClient.SavedSelection lastSavedSelection = null;

   private StructuralAnalysisSelectionClient() {
   }

   public static boolean isActive() {
      return active;
   }

   public static boolean startSelectionFromCrosshair() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         BlockPos target = getCrosshairBlockPos(mc);
         if (target == null) {
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.no_target"), true);
            return false;
         } else {
            startPos = target;
            facePos = target;
            depthPos = target;
            planeAxis = getDominantAxis(mc.player.getViewVector(1.0F));
            stage = StructuralAnalysisSelectionClient.SelectionStage.LINE;
            active = true;
            pendingSelection = null;
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.step1"), true);
            return true;
         }
      } else {
         return false;
      }
   }

   public static void confirmWithCrosshair() {
      if (active) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            int maxSideLength = getMaxSideLength(mc);
            if (stage == StructuralAnalysisSelectionClient.SelectionStage.LINE) {
               facePos = getPreviewFacePos(mc, maxSideLength);
               stage = StructuralAnalysisSelectionClient.SelectionStage.VOLUME;
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.step2"), true);
            } else if (stage == StructuralAnalysisSelectionClient.SelectionStage.VOLUME) {
               depthPos = getPreviewDepthPos(mc, maxSideLength);
               stage = StructuralAnalysisSelectionClient.SelectionStage.CONFIRM;
               mc.setScreen(new StructuralSelectionConfirmScreen());
            } else {
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.confirm_pending"), true);
            }
         }
      }
   }

   public static void onSaveDecision(boolean save) {
      if (active && stage == StructuralAnalysisSelectionClient.SelectionStage.CONFIRM) {
         Minecraft mc = Minecraft.getInstance();
         if (save) {
            pendingSelection = getCurrentSelection();
            stage = StructuralAnalysisSelectionClient.SelectionStage.NAMING;
            if (mc.player != null) {
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structure.enter_name"), true);
            }

            mc.setScreen(new StructuralSelectionNamingScreen());
         } else {
            if (mc.player != null) {
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.discarded"), true);
            }

            cancelSelection();
         }
      }
   }

   public static boolean submitNamedSelection(String name) {
      if (active && stage == StructuralAnalysisSelectionClient.SelectionStage.NAMING) {
         StructuralAnalysisSelectionClient.SavedSelection selection = pendingSelection != null ? pendingSelection : getCurrentSelection();
         String trimmedName = name == null ? "" : name.trim();
         if (trimmedName.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structure.save_name_empty"), true);
            }

            return false;
         } else {
            PacketDistributor.sendToServer(
               new SaveStructuralSelectionMessage(
                  trimmedName,
                  selection.min().getX(),
                  selection.min().getY(),
                  selection.min().getZ(),
                  selection.max().getX(),
                  selection.max().getY(),
                  selection.max().getZ()
               ),
               new CustomPacketPayload[0]
            );
            lastSavedSelection = selection;
            cancelSelection();
            return true;
         }
      } else {
         return false;
      }
   }

   public static void backFromConfirmScreen() {
      if (active && stage == StructuralAnalysisSelectionClient.SelectionStage.CONFIRM) {
         stage = StructuralAnalysisSelectionClient.SelectionStage.VOLUME;
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.back_step2"), true);
         }
      }
   }

   public static void backFromNamingScreen() {
      if (active && stage == StructuralAnalysisSelectionClient.SelectionStage.NAMING) {
         stage = StructuralAnalysisSelectionClient.SelectionStage.CONFIRM;
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.back_confirm"), true);
         }

         mc.setScreen(new StructuralSelectionConfirmScreen());
      }
   }

   public static StructuralAnalysisSelectionClient.SavedSelection getLastSavedSelection() {
      return lastSavedSelection;
   }

   public static void rollbackOneStep() {
      if (active) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            if (stage == StructuralAnalysisSelectionClient.SelectionStage.CONFIRM) {
               stage = StructuralAnalysisSelectionClient.SelectionStage.VOLUME;
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.back_step2"), true);
            } else if (stage == StructuralAnalysisSelectionClient.SelectionStage.NAMING) {
               stage = StructuralAnalysisSelectionClient.SelectionStage.CONFIRM;
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.back_confirm"), true);
            } else if (stage == StructuralAnalysisSelectionClient.SelectionStage.VOLUME) {
               stage = StructuralAnalysisSelectionClient.SelectionStage.LINE;
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.back_step1"), true);
            } else {
               cancelSelection();
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.selection.cancelled"), true);
            }
         }
      }
   }

   public static void cancelSelection() {
      active = false;
      stage = StructuralAnalysisSelectionClient.SelectionStage.LINE;
      pendingSelection = null;
   }

   @SubscribeEvent
   public static void onScreenOpening(Opening event) {
      if (active) {
         if (event.getNewScreen() instanceof PauseScreen && !(event.getNewScreen() instanceof StructuralSelectionConfirmScreen)) {
            rollbackOneStep();
            event.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (active) {
         if (event.getStage() == Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
               int maxSideLength = getMaxSideLength(mc);
               PoseStack poseStack = event.getPoseStack();
               Vec3 cameraPos = event.getCamera().getPosition();
               BlockPos currentTarget = getViewTargetPos(mc, maxSideLength);
               poseStack.pushPose();
               poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
               BufferSource bufferSource = mc.renderBuffers().bufferSource();
               VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
               renderBlockOutline(poseStack, lineConsumer, startPos, 0.2F, 1.0F, 0.2F, 1.0F);
               if (stage == StructuralAnalysisSelectionClient.SelectionStage.LINE) {
                  BlockPos previewFacePos = getPreviewFacePos(mc, maxSideLength);
                  renderBlockOutline(poseStack, lineConsumer, previewFacePos, 0.2F, 0.85F, 1.0F, 1.0F);
                  renderLineBetweenBlocks(poseStack, lineConsumer, startPos, previewFacePos, 0.2F, 0.85F, 1.0F, 0.9F);
                  renderBlockOutline(poseStack, lineConsumer, buildFaceAabb(startPos, previewFacePos), 0.2F, 0.85F, 1.0F, 0.7F);
               } else if (stage == StructuralAnalysisSelectionClient.SelectionStage.VOLUME) {
                  renderBlockOutline(poseStack, lineConsumer, buildFaceAabb(startPos, facePos), 0.2F, 0.85F, 1.0F, 0.85F);
                  renderBlockOutline(
                     poseStack, lineConsumer, buildVolumeAabb(startPos, facePos, getPreviewDepthPos(mc, maxSideLength)), 1.0F, 0.35F, 0.9F, 0.9F
                  );
               } else if (stage == StructuralAnalysisSelectionClient.SelectionStage.CONFIRM || stage == StructuralAnalysisSelectionClient.SelectionStage.NAMING
                  )
                {
                  renderBlockOutline(poseStack, lineConsumer, buildVolumeAabb(startPos, facePos, depthPos), 1.0F, 0.8F, 0.2F, 1.0F);
               }

               bufferSource.endBatch(RenderType.lines());
               poseStack.popPose();
            }
         }
      }
   }

   private static Axis getDominantAxis(Vec3 look) {
      double ax = Math.abs(look.x);
      double ay = Math.abs(look.y);
      double az = Math.abs(look.z);
      if (ay >= ax && ay >= az) {
         return Axis.Y;
      } else {
         return ax >= az ? Axis.X : Axis.Z;
      }
   }

   private static int getMaxSideLength(Minecraft mc) {
      if (mc.player == null) {
         return 16;
      } else {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)mc.player
            .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.player_magic_attributes_sword ? 32 : 16;
      }
   }

   private static BlockPos getCrosshairBlockPos(Minecraft mc) {
      HitResult hit = mc.hitResult;
      return hit instanceof BlockHitResult blockHit && hit.getType() == Type.BLOCK ? blockHit.getBlockPos() : null;
   }

   private static BlockPos getViewTargetPos(Minecraft mc, int maxSideLength) {
      Vec3 viewPoint;
      if (mc.hitResult != null) {
         viewPoint = mc.hitResult.getLocation();
      } else {
         if (mc.player == null) {
            return startPos;
         }

         viewPoint = mc.player.getEyePosition(1.0F).add(mc.player.getViewVector(1.0F).scale(maxSideLength));
      }

      BlockPos raw = BlockPos.containing(viewPoint);
      return clampToMaxSide(raw, maxSideLength);
   }

   private static BlockPos getPreviewFacePos(Minecraft mc, int maxSideLength) {
      BlockPos view = getViewTargetPos(mc, maxSideLength);
      return projectToFacePlane(view);
   }

   private static BlockPos getPreviewDepthPos(Minecraft mc, int maxSideLength) {
      return getViewTargetPos(mc, maxSideLength);
   }

   private static BlockPos clampToMaxSide(BlockPos input, int maxSideLength) {
      int maxDelta = Math.max(0, maxSideLength - 1);
      int dx = Mth.clamp(input.getX() - startPos.getX(), -maxDelta, maxDelta);
      int dy = Mth.clamp(input.getY() - startPos.getY(), -maxDelta, maxDelta);
      int dz = Mth.clamp(input.getZ() - startPos.getZ(), -maxDelta, maxDelta);
      return new BlockPos(startPos.getX() + dx, startPos.getY() + dy, startPos.getZ() + dz);
   }

   private static BlockPos projectToFacePlane(BlockPos input) {
      if (planeAxis == Axis.X) {
         return new BlockPos(startPos.getX(), input.getY(), input.getZ());
      } else {
         return planeAxis == Axis.Y ? new BlockPos(input.getX(), startPos.getY(), input.getZ()) : new BlockPos(input.getX(), input.getY(), startPos.getZ());
      }
   }

   private static AABB buildFaceAabb(BlockPos a, BlockPos b) {
      int minX = Math.min(a.getX(), b.getX());
      int minY = Math.min(a.getY(), b.getY());
      int minZ = Math.min(a.getZ(), b.getZ());
      int maxX = Math.max(a.getX(), b.getX());
      int maxY = Math.max(a.getY(), b.getY());
      int maxZ = Math.max(a.getZ(), b.getZ());
      return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
   }

   private static AABB buildVolumeAabb(BlockPos start, BlockPos face, BlockPos depth) {
      int minX = Math.min(start.getX(), face.getX());
      int minY = Math.min(start.getY(), face.getY());
      int minZ = Math.min(start.getZ(), face.getZ());
      int maxX = Math.max(start.getX(), face.getX());
      int maxY = Math.max(start.getY(), face.getY());
      int maxZ = Math.max(start.getZ(), face.getZ());
      if (planeAxis == Axis.X) {
         minX = Math.min(start.getX(), depth.getX());
         maxX = Math.max(start.getX(), depth.getX());
      } else if (planeAxis == Axis.Y) {
         minY = Math.min(start.getY(), depth.getY());
         maxY = Math.max(start.getY(), depth.getY());
      } else {
         minZ = Math.min(start.getZ(), depth.getZ());
         maxZ = Math.max(start.getZ(), depth.getZ());
      }

      return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
   }

   private static StructuralAnalysisSelectionClient.SavedSelection getCurrentSelection() {
      AABB box = buildVolumeAabb(startPos, facePos, depthPos);
      BlockPos min = new BlockPos((int)Math.floor(box.minX), (int)Math.floor(box.minY), (int)Math.floor(box.minZ));
      BlockPos max = new BlockPos((int)Math.floor(box.maxX) - 1, (int)Math.floor(box.maxY) - 1, (int)Math.floor(box.maxZ) - 1);
      return new StructuralAnalysisSelectionClient.SavedSelection(min, max);
   }

   private static void renderLineBetweenBlocks(PoseStack poseStack, VertexConsumer consumer, BlockPos from, BlockPos to, float r, float g, float b, float a) {
      Vec3 c1 = Vec3.atCenterOf(from);
      Vec3 c2 = Vec3.atCenterOf(to);
      double minX = Math.min(c1.x, c2.x);
      double minY = Math.min(c1.y, c2.y);
      double minZ = Math.min(c1.z, c2.z);
      double maxX = Math.max(c1.x, c2.x);
      double maxY = Math.max(c1.y, c2.y);
      double maxZ = Math.max(c1.z, c2.z);
      if (maxX - minX < 0.02) {
         maxX = minX + 0.02;
      }

      if (maxY - minY < 0.02) {
         maxY = minY + 0.02;
      }

      if (maxZ - minZ < 0.02) {
         maxZ = minZ + 0.02;
      }

      AABB lineBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
      LevelRenderer.renderLineBox(poseStack, consumer, lineBox, r, g, b, a);
   }

   private static void renderBlockOutline(PoseStack poseStack, VertexConsumer consumer, BlockPos pos, float r, float g, float b, float a) {
      AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
      LevelRenderer.renderLineBox(poseStack, consumer, box, r, g, b, a);
   }

   private static void renderBlockOutline(PoseStack poseStack, VertexConsumer consumer, AABB box, float r, float g, float b, float a) {
      LevelRenderer.renderLineBox(poseStack, consumer, box, r, g, b, a);
   }

   public record SavedSelection(BlockPos min, BlockPos max) {
   }

   private static enum SelectionStage {
      LINE,
      VOLUME,
      CONFIRM,
      NAMING;
   }
}
