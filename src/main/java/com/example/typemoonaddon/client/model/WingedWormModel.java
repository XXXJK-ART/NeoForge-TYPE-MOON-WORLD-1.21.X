package com.example.typemoonaddon.client.model;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.worm.WormEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class WingedWormModel extends HierarchicalModel<WormEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            TypeMoonAddon.id("winged_worm"), "main");

    private final ModelPart root;
    private final ModelPart bone;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart frontLeg;
    private final ModelPart middleLeg;
    private final ModelPart backLeg;

    public WingedWormModel(ModelPart root) {
        this.root = root;
        this.bone = root.getChild("bone");
        this.rightWing = this.bone.getChild("right_wing");
        this.leftWing = this.bone.getChild("left_wing");
        this.frontLeg = this.bone.getChild("front_legs");
        this.middleLeg = this.bone.getChild("middle_legs");
        this.backLeg = this.bone.getChild("back_legs");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));
        bone.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F)
                        .texOffs(26, 7).addBox(0.0F, -1.0F, 5.0F, 0.0F, 1.0F, 2.0F)
                        .texOffs(2, 0).addBox(1.5F, -4.0F, -8.0F, 1.0F, 2.0F, 3.0F)
                        .texOffs(2, 3).addBox(-2.5F, -4.0F, -8.0F, 1.0F, 2.0F, 3.0F),
                PartPose.ZERO);
        CubeDeformation wingDeformation = new CubeDeformation(0.001F);
        bone.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 18).addBox(-9.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F, wingDeformation),
                PartPose.offsetAndRotation(-1.5F, -4.0F, -3.0F, 0.0F, -0.2618F, 0.0F));
        bone.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 18).mirror().addBox(0.0F, 0.0F, 0.0F, 9.0F, 0.0F, 6.0F, wingDeformation).mirror(false),
                PartPose.offsetAndRotation(1.5F, -4.0F, -3.0F, 0.0F, 0.2618F, 0.0F));
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().addBox("front_legs", -5.0F, 0.0F, 0.0F, 7, 2, 0, 26, 1),
                PartPose.offset(1.5F, 3.0F, -2.0F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().addBox("middle_legs", -5.0F, 0.0F, 0.0F, 7, 2, 0, 26, 3),
                PartPose.offset(1.5F, 3.0F, 0.0F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().addBox("back_legs", -5.0F, 0.0F, 0.0F, 7, 2, 0, 26, 5),
                PartPose.offset(1.5F, 3.0F, 2.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WormEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        boolean flying = !entity.onGround() || entity.getDeltaMovement().lengthSqr() > 1.0E-7D;
        float flap = Mth.cos(ageInTicks * 2.1F) * 0.32F;
        if (flying) {
            rightWing.yRot = flap;
            rightWing.zRot = Mth.cos(ageInTicks * 8.0F) * 0.28F;
            leftWing.yRot = -flap;
            leftWing.zRot = -rightWing.zRot;
            frontLeg.xRot = Mth.PI / 4.0F;
            middleLeg.xRot = Mth.PI / 4.0F;
            backLeg.xRot = Mth.PI / 4.0F;
            bone.y = 19.0F + Mth.cos(ageInTicks * 0.18F) * 0.7F;
        } else {
            float walk = Mth.sin(limbSwing * 0.8F) * Mth.clamp(limbSwingAmount, 0.0F, 1.0F) * 0.25F;
            frontLeg.xRot = walk;
            middleLeg.xRot = -walk;
            backLeg.xRot = walk;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, int packedColor) {
        root.render(poseStack, consumer, packedLight, packedOverlay, packedColor);
    }

    @Override
    public ModelPart root() {
        return root;
    }
}
