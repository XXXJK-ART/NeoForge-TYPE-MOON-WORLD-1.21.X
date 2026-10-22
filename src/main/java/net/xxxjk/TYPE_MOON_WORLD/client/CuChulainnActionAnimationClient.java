package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import org.joml.Vector3f;

/** PlayerAnimator owns both player playback and NPC pose/weapon sampling, as with Diarmuid. */
public final class CuChulainnActionAnimationClient {
   private static final Map<String,KeyframeAnimationPlayer> SAMPLES=new HashMap<>();
   private static PlayerModel<LivingEntity> tipModel;
   private CuChulainnActionAnimationClient() { }

   public static final class WeaponArmModifier extends AbstractModifier {
      private final LivingEntity owner;
      public WeaponArmModifier(LivingEntity owner){this.owner=owner;}
      @Override public boolean isActive(){return anim!=null && anim.isActive() && CuChulainnVisualClient.action(owner)!=null;}
      @Override public boolean canRemove(){return false;}
      @Override public void tick(){
         var action=CuChulainnVisualClient.action(owner);
         if(action==null || action.slot()!=10)super.tick();
      }
      @Override public Vec3f get3DTransform(String part,TransformType type,float partial,Vec3f base) {
         var action=CuChulainnVisualClient.action(owner);
         if(action==null)return base;
         return transform(owner,action,CuChulainnVisualClient.age(owner,action,partial),part,type,base);
      }
   }

   private static KeyframeAnimationPlayer sample(CuChulainnVisualClient.Action action,float age) {
      if(action.slot()==10)age=Math.min(age,8);
      String path=CuChulainnVisualClient.animation(action.slot());
      var clip=PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld",path));
      if(!(clip instanceof KeyframeAnimation data))return null;
      int tick=Math.max(0,Math.min(data.endTick,(int)age));
      var value=SAMPLES.get(path);
      if(value==null || value.getData()!=data || value.getTick()!=tick){value=new KeyframeAnimationPlayer(data,tick);SAMPLES.put(path,value);}
      value.setupAnim(Math.max(0,age-tick));return value;
   }

   private static Vec3f reflect(Vec3f value,TransformType type) {
      return type==TransformType.POSITION?new Vec3f(-value.getX(),value.getY(),value.getZ())
         :type==TransformType.ROTATION?new Vec3f(value.getX(),-value.getY(),-value.getZ()):value;
   }
   private static Vec3f transform(LivingEntity owner,CuChulainnVisualClient.Action action,float age,String part,TransformType type,Vec3f base) {
      var value=sample(action,age);if(value==null)return base;
      boolean mirror=action.left();String source=part;
      if(mirror && part.startsWith("right"))source="left"+part.substring(5);
      else if(mirror && part.startsWith("left"))source="right"+part.substring(4);
      var result=value.get3DTransform(source,type,0,mirror?reflect(base,type):base);
      return mirror?reflect(result,type):result;
   }
   private static void part(LivingEntity owner,CuChulainnVisualClient.Action action,float age,String name,ModelPart part) {
      Vec3f position=transform(owner,action,age,name,TransformType.POSITION,new Vec3f(part.x,part.y,part.z));
      Vec3f rotation=transform(owner,action,age,name,TransformType.ROTATION,new Vec3f(part.xRot,part.yRot,part.zRot));
      part.setPos(position.getX(),position.getY(),position.getZ());part.setRotation(rotation.getX(),rotation.getY(),rotation.getZ());
   }
   public static void pose(LivingEntity owner,HumanoidModel<?> model,CuChulainnVisualClient.Action action,float age) {
      part(owner,action,age,"torso",model.body);
      part(owner,action,age,"rightArm",model.rightArm);part(owner,action,age,"leftArm",model.leftArm);
      if(model instanceof PlayerModel<?> player){player.jacket.copyFrom(player.body);player.rightSleeve.copyFrom(player.rightArm);player.leftSleeve.copyFrom(player.leftArm);}
   }
   private static void grip(LivingEntity owner,HumanoidModel<?> model,HumanoidArm arm,CuChulainnVisualClient.Action action,float age,PoseStack poses) {
      model.translateToHand(arm,poses);poses.mulPose(Axis.XP.rotationDegrees(-90));poses.mulPose(Axis.YP.rotationDegrees(180));
      poses.translate(arm==HumanoidArm.LEFT?-.0625:.0625,.125,-.625);
      String name=arm==HumanoidArm.LEFT?"leftItem":"rightItem";
      var p=transform(owner,action,age,name,TransformType.POSITION,Vec3f.ZERO);
      var r=transform(owner,action,age,name,TransformType.ROTATION,Vec3f.ZERO);
      poses.translate(p.getX()/16,p.getY()/16,p.getZ()/16);
      poses.mulPose(Axis.ZP.rotation(r.getZ()));poses.mulPose(Axis.YP.rotation(r.getY()));poses.mulPose(Axis.XP.rotation(r.getX()));
   }
   public static boolean renderHeldItem(LivingEntity owner,HumanoidModel<?> model,HumanoidArm arm,ItemStack stack,ItemDisplayContext display,PoseStack poses,MultiBufferSource buffers,int light) {
      var action=CuChulainnVisualClient.action(owner);if(action==null || !stack.is(ModItems.GAE_BULG.get()))return false;
      float partial=Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
      poses.pushPose();grip(owner,model,arm,action,CuChulainnVisualClient.age(owner,action,partial),poses);
      Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(owner,stack,display,arm==HumanoidArm.LEFT,poses,buffers,light);
      poses.popPose();return true;
   }
   public static Vec3 spearTip(LivingEntity owner,CuChulainnVisualClient.Action action,float age) {
      HumanoidArm arm=action.left()?HumanoidArm.LEFT:HumanoidArm.RIGHT;
      var hand=arm==owner.getMainArm()?net.minecraft.world.InteractionHand.MAIN_HAND:net.minecraft.world.InteractionHand.OFF_HAND;
      var stack=owner.getItemInHand(hand);if(!stack.is(ModItems.GAE_BULG.get()))return null;
      var mc=Minecraft.getInstance();if(tipModel==null)tipModel=new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER),false);
      tipModel.body.resetPose();tipModel.rightArm.resetPose();tipModel.leftArm.resetPose();pose(owner,tipModel,action,age);
      PoseStack poses=new PoseStack();poses.scale(-1,-1,1);poses.translate(0,-1.501,0);grip(owner,tipModel,arm,action,age,poses);
      boolean left=arm==HumanoidArm.LEFT;
      mc.getItemRenderer().getModel(stack,owner.level(),owner,0).getTransforms().getTransform(left?ItemDisplayContext.THIRD_PERSON_LEFT_HAND:ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).apply(left,poses);
      poses.translate(0,.01,0);
      Vec3 endpoint=DiarmuidActionAnimationClient.spearMeshEndpoint(stack,(net.xxxjk.TYPE_MOON_WORLD.item.custom.GaeBulgItem)stack.getItem(),false);
      if(endpoint==null)return null;
      Vector3f point=poses.last().pose().transformPosition((float)endpoint.x,(float)endpoint.y,(float)endpoint.z,new Vector3f());
      return new Vec3(point.x,point.y,point.z);
   }
}
