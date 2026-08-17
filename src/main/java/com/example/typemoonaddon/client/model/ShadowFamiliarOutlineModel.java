package com.example.typemoonaddon.client.model;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

public final class ShadowFamiliarOutlineModel extends HierarchicalModel<SakuraShadowFamiliarEntity> {
    private static final float PAPER_THICKNESS = 0.25F;
    private static final float PAPER_HALF_THICKNESS = PAPER_THICKNESS * 0.5F;

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        TypeMoonAddon.id("shadow_familiar"),
        "white_outline"
    );

    // Reversed boxes expand by this amount; connection faces are omitted from the hull entirely.
    private static final float OUTLINE_EXPANSION = 0.18F;
    private static final Set<Direction> SIDE_FACES = facesWithout(Direction.UP, Direction.DOWN);
    private static final Set<Direction> TOP_AND_SIDE_FACES = facesWithout(Direction.DOWN);
    private static final Set<Direction> BOTTOM_AND_SIDE_FACES = facesWithout(Direction.UP);

    private final ModelPart root;
    private final ModelPart familiar;
    private final ModelPart leftOuter;
    private final ModelPart rightOuter;

    public ShadowFamiliarOutlineModel(ModelPart root) {
        super(RenderType::entityCutout);
        this.root = root;
        this.familiar = root.getChild("familiar");
        this.leftOuter = this.familiar.getChild("left_outer");
        this.rightOuter = this.familiar.getChild("right_outer");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition familiar = root.addOrReplaceChild("familiar", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        CubeListBuilder bodyOutline = CubeListBuilder.create();
        addReversedBox(bodyOutline, 0, 0, 6.0F, -2.0F, -12.0F, -14.0F, SIDE_FACES);
        addReversedBox(bodyOutline, 0, 20, 8.0F, -11.0F, -16.0F, -7.0F, SIDE_FACES);
        addReversedBox(bodyOutline, 0, 33, 4.0F, 0.0F, -8.0F, -5.0F, BOTTOM_AND_SIDE_FACES);
        familiar.addOrReplaceChild(
            "body",
            bodyOutline,
            PartPose.ZERO
        );

        PartDefinition leftOuter = familiar.addOrReplaceChild(
            "left_outer",
            addReversedBox(CubeListBuilder.create(), 28, 33, 2.0F, 9.0F, -4.0F, -9.0F, SIDE_FACES),
            PartPose.offsetAndRotation(-7.0F, -17.0F, 0.0F, 0.0F, 0.0F, 0.48F)
        );
        leftOuter.addOrReplaceChild(
            "left_outer_tip",
            addReversedBox(CubeListBuilder.create(), 44, 33, 1.5F, 8.5F, -3.0F, -8.5F, BOTTOM_AND_SIDE_FACES),
            PartPose.offsetAndRotation(0.0F, 7.25F, 0.0F, 0.0F, 0.0F, 0.12F)
        );

        PartDefinition rightOuter = familiar.addOrReplaceChild(
            "right_outer",
            addReversedBox(CubeListBuilder.create(), 28, 33, 2.0F, 9.0F, -4.0F, -9.0F, SIDE_FACES),
            PartPose.offsetAndRotation(7.0F, -17.0F, 0.0F, 0.0F, 0.0F, -0.48F)
        );
        rightOuter.addOrReplaceChild(
            "right_outer_tip",
            addReversedBox(CubeListBuilder.create(), 44, 33, 1.5F, 8.5F, -3.0F, -8.5F, BOTTOM_AND_SIDE_FACES),
            PartPose.offsetAndRotation(0.0F, 7.25F, 0.0F, 0.0F, 0.0F, -0.12F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(
        PoseStack poseStack,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor
    ) {
        this.root.render(poseStack, consumer, packedLight, packedOverlay, packedColor);
        ShadowFamiliarHeadGeometry.render(
            poseStack,
            consumer,
            packedLight,
            packedOverlay,
            packedColor,
            this.root,
            this.familiar,
            true
        );
    }

    private static CubeListBuilder addReversedBox(
        CubeListBuilder builder,
        int textureX,
        int textureY,
        float x,
        float y,
        float sizeX,
        float sizeY,
        Set<Direction> visibleFaces
    ) {
        float expansion = OUTLINE_EXPANSION;
        return builder.texOffs(textureX, textureY).addBox(
            x + expansion,
            y + expansion,
            PAPER_HALF_THICKNESS + expansion,
            sizeX - expansion * 2.0F,
            sizeY - expansion * 2.0F,
            -PAPER_THICKNESS - expansion * 2.0F,
            visibleFaces
        );
    }

    private static Set<Direction> facesWithout(Direction... hiddenFaces) {
        EnumSet<Direction> faces = EnumSet.allOf(Direction.class);
        for (Direction hiddenFace : hiddenFaces) {
            faces.remove(hiddenFace);
        }
        return faces;
    }

    @Override
    public void setupAnim(SakuraShadowFamiliarEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        if (entity.deathTime > 0) {
            this.familiar.y = 24.0F;
            return;
        }
        float walkAmount = Mth.clamp(limbSwingAmount * 1.5F, 0.0F, 1.0F);
        float walkCycle = Mth.sin(limbSwing * 0.9F) * walkAmount;
        float stepLift = Mth.abs(Mth.cos(limbSwing * 0.9F)) * walkAmount;
        float sway = Mth.sin(ageInTicks * 0.09F);
        this.familiar.y = 24.0F - stepLift * 0.35F;
        this.familiar.zRot = Mth.sin(limbSwing * 0.45F) * walkAmount * 0.035F;
        this.leftOuter.xRot += walkCycle * 0.34F;
        this.rightOuter.xRot -= walkCycle * 0.34F;
        this.leftOuter.zRot += walkCycle * 0.05F;
        this.rightOuter.zRot += walkCycle * 0.05F;
        this.leftOuter.zRot += sway * 0.045F;
        this.rightOuter.zRot -= sway * 0.045F;

        byte attackPhase = entity.getAttackPhase();
        float attackProgress = entity.getAttackPhaseProgress(ageInTicks);
        if (attackPhase == SakuraShadowFamiliarEntity.ATTACK_PHASE_SHADOW_BINDING) {
            float grasp = entity.isShadowBindingConnected()
                ? 0.8F + Mth.sin(ageInTicks * 0.32F) * 0.12F
                : Mth.sin(attackProgress * Mth.PI);
            this.familiar.xScale = 1.0F + grasp * 0.12F;
            this.familiar.yScale = 1.0F + grasp * 0.08F;
            this.leftOuter.zRot -= grasp * 0.48F;
            this.rightOuter.zRot += grasp * 0.48F;
        } else if (attackPhase == SakuraShadowFamiliarEntity.ATTACK_PHASE_VOID_ABSORPTION) {
            float swell = entity.isVoidAbsorptionConnected()
                ? 0.72F + Mth.sin(ageInTicks * 0.28F) * 0.18F
                : Mth.sin(attackProgress * Mth.PI);
            this.familiar.xScale = 1.0F + swell * 0.18F;
            this.familiar.yScale = 1.0F + swell * 0.12F;
            this.familiar.zScale = 1.0F + swell * 0.18F;
            this.leftOuter.zRot -= swell * 0.34F;
            this.rightOuter.zRot += swell * 0.34F;
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
