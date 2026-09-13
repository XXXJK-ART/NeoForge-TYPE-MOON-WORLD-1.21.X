package net.xxxjk.TYPE_MOON_WORLD.client.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.ColorableAgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.entity.SeaBeastEntity;

/** Vanilla wolf mesh, parameterized for the sea-beast entity. */
public final class SeaBeastWolfModel extends ColorableAgeableListModel<SeaBeastEntity> {
   private final ModelPart head;
   private final ModelPart realHead;
   private final ModelPart body;
   private final ModelPart rightHindLeg;
   private final ModelPart leftHindLeg;
   private final ModelPart rightFrontLeg;
   private final ModelPart leftFrontLeg;
   private final ModelPart tail;
   private final ModelPart realTail;
   private final ModelPart upperBody;

   public SeaBeastWolfModel(ModelPart root) {
      this.head = root.getChild("head");
      this.realHead = this.head.getChild("real_head");
      this.body = root.getChild("body");
      this.upperBody = root.getChild("upper_body");
      this.rightHindLeg = root.getChild("right_hind_leg");
      this.leftHindLeg = root.getChild("left_hind_leg");
      this.rightFrontLeg = root.getChild("right_front_leg");
      this.leftFrontLeg = root.getChild("left_front_leg");
      this.tail = root.getChild("tail");
      this.realTail = this.tail.getChild("real_tail");
   }

   /** Kept in sync with net.minecraft.client.model.WolfModel#createMeshDefinition. */
   public static MeshDefinition createMeshDefinition(CubeDeformation deformation) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-1.0F, 13.5F, -7.0F));
      head.addOrReplaceChild("real_head", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-2.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, deformation)
            .texOffs(16, 14).addBox(-2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, deformation)
            .texOffs(16, 14).addBox(2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, deformation)
            .texOffs(0, 10).addBox(-0.5F, -0.001F, -5.0F, 3.0F, 3.0F, 4.0F, deformation), PartPose.ZERO);
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 14)
            .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, deformation),
            PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
      root.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(21, 0)
            .addBox(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, deformation),
            PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
      CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, deformation);
      root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5F, 16.0F, 7.0F));
      root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(0.5F, 16.0F, 7.0F));
      root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5F, 16.0F, -4.0F));
      root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(0.5F, 16.0F, -4.0F));
      PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(),
            PartPose.offsetAndRotation(-1.0F, 12.0F, 8.0F, (float) (Math.PI / 5), 0.0F, 0.0F));
      tail.addOrReplaceChild("real_tail", CubeListBuilder.create().texOffs(9, 18)
            .addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, deformation), PartPose.ZERO);
      return mesh;
   }

   @Override
   protected Iterable<ModelPart> headParts() {
      return ImmutableList.of(head);
   }

   @Override
   protected Iterable<ModelPart> bodyParts() {
      return ImmutableList.of(body, rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg, tail, upperBody);
   }

   @Override
   public void prepareMobModel(SeaBeastEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
      tail.yRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
      body.setPos(0.0F, 14.0F, 2.0F);
      body.xRot = (float) (Math.PI / 2);
      upperBody.setPos(-1.0F, 14.0F, -3.0F);
      upperBody.xRot = body.xRot;
      tail.setPos(-1.0F, 12.0F, 8.0F);
      rightHindLeg.setPos(-2.5F, 16.0F, 7.0F);
      leftHindLeg.setPos(0.5F, 16.0F, 7.0F);
      rightFrontLeg.setPos(-2.5F, 16.0F, -4.0F);
      leftFrontLeg.setPos(0.5F, 16.0F, -4.0F);
      rightHindLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
      leftHindLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
      rightFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
      leftFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
      realHead.zRot = 0.0F;
      upperBody.zRot = 0.0F;
      body.zRot = 0.0F;
      realTail.zRot = 0.0F;
   }

   @Override
   public void setupAnim(SeaBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
         float netHeadYaw, float headPitch) {
      head.xRot = headPitch * ((float) Math.PI / 180.0F);
      head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
      tail.xRot = ageInTicks;
   }
}
