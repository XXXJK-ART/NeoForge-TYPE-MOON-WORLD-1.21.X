package net.xxxjk.TYPE_MOON_WORLD.client.projection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.StartStructureProjectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

import java.util.List;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class StructuralProjectionPlacementClient {
    private enum Stage {
        IDLE,
        PREVIEW,
        LOCKED
    }

    private static final int MAX_RENDERED_BLOCK_OUTLINES = 2200;

    private static Stage stage = Stage.IDLE;
    private static String structureId = "";
    private static BlockPos anchorPos = BlockPos.ZERO;
    private static int rotationIndex = 0;

    private StructuralProjectionPlacementClient() {
    }

    public static boolean isActive() {
        return stage != Stage.IDLE;
    }

    public static boolean isLocked() {
        return stage == Stage.LOCKED;
    }

    public static void startPreview() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        TypeMoonWorldModVariables.PlayerVariables vars = mc.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!isProjectionWithStructureSelected(vars)) return;
        if (vars.getStructureById(vars.projection_selected_structure_id) == null) return;

        structureId = vars.projection_selected_structure_id;
        anchorPos = getCrosshairPlacementPos(mc);
        rotationIndex = 0;
        stage = Stage.PREVIEW;
        mc.player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.preview_start"), true);
    }

    public static void confirmPlacement() {
        if (stage != Stage.PREVIEW) return;
        stage = Stage.LOCKED;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.preview_lock"), true);
        }
    }

    public static void startProjection() {
        if (stage != Stage.LOCKED || structureId.isEmpty()) return;
        PacketDistributor.sendToServer(new StartStructureProjectionMessage(
                structureId,
                anchorPos.getX(),
                anchorPos.getY(),
                anchorPos.getZ(),
                rotationIndex
        ));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.build_begin"), true);
        }
        cancel();
    }

    public static boolean handleScroll(double deltaY) {
        if ((stage != Stage.PREVIEW && stage != Stage.LOCKED) || deltaY == 0) return false;
        rotationIndex = Math.floorMod(rotationIndex + (deltaY > 0 ? 1 : -1), 4);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("message.typemoonworld.projection.structure.rotation", rotationIndex), true);
        }
        return true;
    }

    public static void cancel() {
        stage = Stage.IDLE;
        structureId = "";
        anchorPos = BlockPos.ZERO;
        rotationIndex = 0;
    }

    public static void cancelIfInvalid(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (stage == Stage.IDLE) return;
        if (!isProjectionWithStructureSelected(vars)) {
            cancel();
            return;
        }
        if (vars.getStructureById(vars.projection_selected_structure_id) == null) {
            cancel();
            return;
        }
        if (!structureId.equals(vars.projection_selected_structure_id)) {
            cancel();
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (stage == Stage.IDLE) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        TypeMoonWorldModVariables.PlayerVariables vars = mc.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = vars.getStructureById(structureId);
        if (structure == null) {
            cancel();
            return;
        }

        if (stage == Stage.PREVIEW) {
            anchorPos = getCrosshairPlacementPos(mc);
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
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
        float r = stage == Stage.LOCKED ? 1.00F : 0.20F;
        float g = stage == Stage.LOCKED ? 0.65F : 0.90F;
        float b = stage == Stage.LOCKED ? 0.20F : 1.00F;
        LevelRenderer.renderLineBox(poseStack, lineConsumer, bounds, r, g, b, 0.95F);

        List<TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock> blocks = structure.blocks;
        int step = Math.max(1, blocks.size() / MAX_RENDERED_BLOCK_OUTLINES);
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

    private static boolean isProjectionWithStructureSelected(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (!vars.is_magus || !vars.is_magic_circuit_open) return false;
        if (vars.selected_magics.isEmpty()) return false;
        int index = vars.current_magic_index;
        if (index < 0 || index >= vars.selected_magics.size()) return false;
        if (!"projection".equals(vars.selected_magics.get(index))) return false;
        return vars.projection_selected_structure_id != null && !vars.projection_selected_structure_id.isEmpty();
    }

    private static BlockPos getCrosshairPlacementPos(Minecraft mc) {
        HitResult hitResult = mc.hitResult;
        if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK) {
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        }
        if (mc.player != null) {
            Vec3 eye = mc.player.getEyePosition(1.0F);
            Vec3 look = mc.player.getViewVector(1.0F);
            return BlockPos.containing(eye.add(look.scale(5.0)));
        }
        return BlockPos.ZERO;
    }

    private static int[] getRotatedSize(int sizeX, int sizeZ, int rotationIndex) {
        int normalized = Math.floorMod(rotationIndex, 4);
        if (normalized == 1 || normalized == 3) {
            return new int[]{sizeZ, sizeX};
        }
        return new int[]{sizeX, sizeZ};
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
}
