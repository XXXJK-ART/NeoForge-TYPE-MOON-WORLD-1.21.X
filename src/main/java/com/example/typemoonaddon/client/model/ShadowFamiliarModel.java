package com.example.typemoonaddon.client.model;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

public final class ShadowFamiliarModel extends HierarchicalModel<SakuraShadowFamiliarEntity> {
    private static final float PAPER_THICKNESS = 0.25F;
    private static final float PAPER_HALF_THICKNESS = PAPER_THICKNESS * 0.5F;

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        TypeMoonAddon.id("shadow_familiar"),
        "main"
    );

    private final ModelPart root;
    private final ModelPart familiar;
    private final ModelPart leftOuter;
    private final ModelPart rightOuter;
    private final ModelPart leftInner;
    private final ModelPart rightInner;
    private final List<ModelPart> cores;

    public ShadowFamiliarModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        this.familiar = root.getChild("familiar");
        this.leftOuter = this.familiar.getChild("left_outer");
        this.rightOuter = this.familiar.getChild("right_outer");
        this.leftInner = this.familiar.getChild("left_inner");
        this.rightInner = this.familiar.getChild("right_inner");
        this.cores = List.of(
            this.familiar.getChild("core_top"),
            this.familiar.getChild("core_upper_left"),
            this.familiar.getChild("core_upper_right"),
            this.familiar.getChild("core_center"),
            this.familiar.getChild("core_middle_left"),
            this.familiar.getChild("core_middle_right"),
            this.familiar.getChild("core_lower"),
            this.familiar.getChild("core_bottom")
        );
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition familiar = root.addOrReplaceChild("familiar", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        familiar.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0F, -16.0F, -PAPER_HALF_THICKNESS, 12.0F, 14.0F, PAPER_THICKNESS, CubeDeformation.NONE)
                .texOffs(0, 20)
                .addBox(-8.0F, -18.0F, -PAPER_HALF_THICKNESS, 16.0F, 7.0F, PAPER_THICKNESS, CubeDeformation.NONE)
                .texOffs(0, 33)
                .addBox(-4.0F, -5.0F, -PAPER_HALF_THICKNESS, 8.0F, 5.0F, PAPER_THICKNESS, CubeDeformation.NONE),
            PartPose.ZERO
        );

        PartDefinition leftOuter = familiar.addOrReplaceChild(
            "left_outer",
            CubeListBuilder.create().texOffs(28, 33).addBox(-2.0F, 0.0F, -PAPER_HALF_THICKNESS, 4.0F, 9.0F, PAPER_THICKNESS, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-7.0F, -17.0F, 0.0F, 0.0F, 0.0F, 0.48F)
        );
        leftOuter.addOrReplaceChild(
            "left_outer_tip",
            CubeListBuilder.create().texOffs(44, 33).addBox(-1.5F, 0.0F, -PAPER_HALF_THICKNESS, 3.0F, 8.5F, PAPER_THICKNESS, CubeDeformation.NONE),
            PartPose.offsetAndRotation(0.0F, 7.25F, 0.0F, 0.0F, 0.0F, 0.12F)
        );

        PartDefinition rightOuter = familiar.addOrReplaceChild(
            "right_outer",
            CubeListBuilder.create().texOffs(28, 33).mirror().addBox(-2.0F, 0.0F, -PAPER_HALF_THICKNESS, 4.0F, 9.0F, PAPER_THICKNESS, CubeDeformation.NONE).mirror(false),
            PartPose.offsetAndRotation(7.0F, -17.0F, 0.0F, 0.0F, 0.0F, -0.48F)
        );
        rightOuter.addOrReplaceChild(
            "right_outer_tip",
            CubeListBuilder.create().texOffs(44, 33).mirror().addBox(-1.5F, 0.0F, -PAPER_HALF_THICKNESS, 3.0F, 8.5F, PAPER_THICKNESS, CubeDeformation.NONE).mirror(false),
            PartPose.offsetAndRotation(0.0F, 7.25F, 0.0F, 0.0F, 0.0F, -0.12F)
        );

        familiar.addOrReplaceChild(
            "left_inner",
            CubeListBuilder.create().texOffs(28, 48).addBox(-1.5F, 0.0F, -PAPER_HALF_THICKNESS, 3.0F, 10.0F, PAPER_THICKNESS, CubeDeformation.NONE),
            PartPose.offsetAndRotation(-4.5F, -10.0F, 0.0F, 0.0F, 0.0F, 0.16F)
        );
        familiar.addOrReplaceChild(
            "right_inner",
            CubeListBuilder.create().texOffs(28, 48).mirror().addBox(-1.5F, 0.0F, -PAPER_HALF_THICKNESS, 3.0F, 10.0F, PAPER_THICKNESS, CubeDeformation.NONE).mirror(false),
            PartPose.offsetAndRotation(4.5F, -10.0F, 0.0F, 0.0F, 0.0F, -0.16F)
        );

        addCore(familiar, "core_top", 0.0F, -21.0F);
        addCore(familiar, "core_upper_left", -3.25F, -18.0F);
        addCore(familiar, "core_upper_right", 3.25F, -18.0F);
        addCore(familiar, "core_center", 0.0F, -15.5F);
        addCore(familiar, "core_middle_left", -3.5F, -13.0F);
        addCore(familiar, "core_middle_right", 3.5F, -13.0F);
        addCore(familiar, "core_lower", 0.0F, -10.5F);
        addCore(familiar, "core_bottom", 0.0F, -6.5F);

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
            false
        );
    }

    private static void addCore(PartDefinition parent, String name, float x, float y) {
        parent.addOrReplaceChild(
            name,
            CubeListBuilder.create().texOffs(52, 56).addBox(-1.0F, -1.0F, -PAPER_HALF_THICKNESS, 2.0F, 2.0F, PAPER_THICKNESS, CubeDeformation.NONE),
            PartPose.offset(x, y, -PAPER_THICKNESS)
        );
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
        this.leftInner.xRot -= walkCycle * 0.26F;
        this.rightInner.xRot += walkCycle * 0.26F;
        this.leftOuter.zRot += walkCycle * 0.05F;
        this.rightOuter.zRot += walkCycle * 0.05F;
        this.leftInner.zRot -= walkCycle * 0.04F;
        this.rightInner.zRot -= walkCycle * 0.04F;
        this.leftOuter.zRot += sway * 0.045F;
        this.rightOuter.zRot -= sway * 0.045F;
        this.leftInner.zRot -= sway * 0.03F;
        this.rightInner.zRot += sway * 0.03F;

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
            this.leftInner.zRot -= grasp * 0.32F;
            this.rightInner.zRot += grasp * 0.32F;
        } else if (attackPhase == SakuraShadowFamiliarEntity.ATTACK_PHASE_VOID_ABSORPTION) {
            float swell = entity.isVoidAbsorptionConnected()
                ? 0.72F + Mth.sin(ageInTicks * 0.28F) * 0.18F
                : Mth.sin(attackProgress * Mth.PI);
            this.familiar.xScale = 1.0F + swell * 0.18F;
            this.familiar.yScale = 1.0F + swell * 0.12F;
            this.familiar.zScale = 1.0F + swell * 0.18F;
            this.leftOuter.zRot -= swell * 0.34F;
            this.rightOuter.zRot += swell * 0.34F;
            this.leftInner.zRot -= swell * 0.22F;
            this.rightInner.zRot += swell * 0.22F;
        }

        for (int i = 0; i < this.cores.size(); i++) {
            float pulse = 0.94F + Mth.sin(ageInTicks * 0.18F + i * 0.7F) * 0.08F;
            if (attackPhase == SakuraShadowFamiliarEntity.ATTACK_PHASE_SHADOW_BINDING) {
                pulse += entity.isShadowBindingConnected()
                    ? 0.2F + Mth.sin(ageInTicks * 0.38F + i * 0.4F) * 0.08F
                    : attackProgress * 0.2F;
            } else if (attackPhase == SakuraShadowFamiliarEntity.ATTACK_PHASE_VOID_ABSORPTION) {
                float absorptionPulse = entity.isVoidAbsorptionConnected()
                    ? 0.24F + Mth.sin(ageInTicks * 0.4F + i * 0.35F) * 0.08F
                    : Mth.sin(attackProgress * Mth.PI) * 0.3F;
                pulse += absorptionPulse;
            }
            ModelPart core = this.cores.get(i);
            core.xScale = pulse;
            core.yScale = pulse;
            core.zScale = pulse;
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
