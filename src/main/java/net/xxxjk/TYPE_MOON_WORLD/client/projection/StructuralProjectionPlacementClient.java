package net.xxxjk.TYPE_MOON_WORLD.client.projection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.StartStructureProjectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(
   modid = "typemoonworld",
   value = {Dist.CLIENT}
)
public final class StructuralProjectionPlacementClient {
   private static final int MAX_RENDERED_BLOCK_OUTLINES = 2200;
   private static StructuralProjectionPlacementClient.Stage stage = StructuralProjectionPlacementClient.Stage.IDLE;
   private static String structureId = "";
   private static BlockPos anchorPos = BlockPos.ZERO;
   private static int rotationIndex = 0;

   private StructuralProjectionPlacementClient() {
   }

   public static boolean isActive() {
      return stage != StructuralProjectionPlacementClient.Stage.IDLE;
   }

   public static boolean isLocked() {
      return stage == StructuralProjectionPlacementClient.Stage.LOCKED;
   }

   public static void startPreview() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)mc.player
            .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (isProjectionWithStructureSelected(vars)) {
            if (vars.getStructureById(vars.projection_selected_structure_id) != null) {
               structureId = vars.projection_selected_structure_id;
               anchorPos = getCrosshairPlacementPos(mc);
               rotationIndex = 0;
               stage = StructuralProjectionPlacementClient.Stage.PREVIEW;
               mc.player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.preview_start"), true);
            }
         }
      }
   }

   public static void confirmPlacement() {
      if (stage == StructuralProjectionPlacementClient.Stage.PREVIEW) {
         stage = StructuralProjectionPlacementClient.Stage.LOCKED;
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.preview_lock"), true);
         }
      }
   }

   public static void startProjection() {
      if (stage == StructuralProjectionPlacementClient.Stage.LOCKED && !structureId.isEmpty()) {
         PacketDistributor.sendToServer(
            new StartStructureProjectionMessage(structureId, anchorPos.getX(), anchorPos.getY(), anchorPos.getZ(), rotationIndex), new CustomPacketPayload[0]
         );
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.build_begin"), true);
         }

         cancel();
      }
   }

   public static boolean handleScroll(double deltaY) {
      if ((stage == StructuralProjectionPlacementClient.Stage.PREVIEW || stage == StructuralProjectionPlacementClient.Stage.LOCKED) && deltaY != 0.0) {
         rotationIndex = Math.floorMod(rotationIndex + (deltaY > 0.0 ? 1 : -1), 4);
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.rotation", new Object[]{rotationIndex}), true);
         }

         return true;
      } else {
         return false;
      }
   }

   public static void cancel() {
      stage = StructuralProjectionPlacementClient.Stage.IDLE;
      structureId = "";
      anchorPos = BlockPos.ZERO;
      rotationIndex = 0;
   }

   public static void cancelIfInvalid(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (stage != StructuralProjectionPlacementClient.Stage.IDLE) {
         if (!isProjectionWithStructureSelected(vars)) {
            cancel();
         } else if (vars.getStructureById(vars.projection_selected_structure_id) == null) {
            cancel();
         } else {
            if (!structureId.equals(vars.projection_selected_structure_id)) {
               cancel();
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (stage != StructuralProjectionPlacementClient.Stage.IDLE) {
         if (event.getStage() == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)mc.player
                  .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
               TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = vars.getStructureById(structureId);
               if (structure == null) {
                  cancel();
               } else {
                  if (stage == StructuralProjectionPlacementClient.Stage.PREVIEW) {
                     anchorPos = getCrosshairPlacementPos(mc);
                  }

                  PoseStack poseStack = event.getPoseStack();
                  Vec3 cameraPos = event.getCamera().getPosition();
                  poseStack.pushPose();
                  poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                  BufferSource bufferSource = mc.renderBuffers().bufferSource();
                  VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
                  int[] size = getRotatedSize(structure.sizeX, structure.sizeZ, rotationIndex);
                  AABB bounds = new AABB(
                     anchorPos.getX(),
                     anchorPos.getY(),
                     anchorPos.getZ(),
                     anchorPos.getX() + size[0],
                     anchorPos.getY() + structure.sizeY,
                     anchorPos.getZ() + size[1]
                  );
                  float r = stage == StructuralProjectionPlacementClient.Stage.LOCKED ? 1.0F : 0.2F;
                  float g = stage == StructuralProjectionPlacementClient.Stage.LOCKED ? 0.65F : 0.9F;
                  float b = stage == StructuralProjectionPlacementClient.Stage.LOCKED ? 0.2F : 1.0F;
                  LevelRenderer.renderLineBox(poseStack, lineConsumer, bounds, r, g, b, 0.95F);
                  List<TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock> blocks = structure.blocks;
                  int step = Math.max(1, blocks.size() / 2200);

                  for (int i = 0; i < blocks.size(); i += step) {
                     TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock block = blocks.get(i);
                     int[] rotated = rotateXZ(block.x, block.z, structure.sizeX, structure.sizeZ, rotationIndex);
                     int worldX = anchorPos.getX() + rotated[0];
                     int worldY = anchorPos.getY() + block.y;
                     int worldZ = anchorPos.getZ() + rotated[1];
                     AABB box = new AABB(worldX, worldY, worldZ, worldX + 1, worldY + 1, worldZ + 1);
                     LevelRenderer.renderLineBox(poseStack, lineConsumer, box, r, g, b, 0.45F);
                  }

                  bufferSource.endBatch(RenderType.lines());
                  poseStack.popPose();
               }
            }
         }
      }
   }

   private static boolean isProjectionWithStructureSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.is_magus || !vars.is_magic_circuit_open) {
         return false;
      } else if (vars.selected_magics.isEmpty()) {
         return false;
      } else {
         int index = vars.current_magic_index;
         if (index < 0 || index >= vars.selected_magics.size()) {
            return false;
         } else {
            return !"projection".equals(vars.selected_magics.get(index))
               ? false
               : vars.projection_selected_structure_id != null && !vars.projection_selected_structure_id.isEmpty();
         }
      }
   }

   private static BlockPos getCrosshairPlacementPos(Minecraft mc) {
      HitResult hitResult = mc.hitResult;
      if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == Type.BLOCK) {
         return blockHit.getBlockPos().relative(blockHit.getDirection());
      } else if (mc.player != null) {
         Vec3 eye = mc.player.getEyePosition(1.0F);
         Vec3 look = mc.player.getViewVector(1.0F);
         return BlockPos.containing(eye.add(look.scale(5.0)));
      } else {
         return BlockPos.ZERO;
      }
   }

   private static int[] getRotatedSize(int sizeX, int sizeZ, int rotationIndex) {
      int normalized = Math.floorMod(rotationIndex, 4);
      return normalized != 1 && normalized != 3 ? new int[]{sizeX, sizeZ} : new int[]{sizeZ, sizeX};
   }

   private static int[] rotateXZ(int x, int z, int sizeX, int sizeZ, int rotationIndex) {
      int normalized = Math.floorMod(rotationIndex, 4);

      return switch (normalized) {
         case 1 -> new int[]{z, sizeX - 1 - x};
         case 2 -> new int[]{sizeX - 1 - x, sizeZ - 1 - z};
         case 3 -> new int[]{sizeZ - 1 - z, x};
         default -> new int[]{x, z};
      };
   }

   private static enum Stage {
      IDLE,
      PREVIEW,
      LOCKED;
   }
}
