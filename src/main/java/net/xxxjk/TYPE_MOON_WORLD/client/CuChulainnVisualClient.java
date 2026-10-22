package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.*;
import com.mojang.blaze3d.vertex.*;
import dev.kosmx.playerAnim.api.layered.*;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.CuChulainnVisualMessage;

@EventBusSubscriber(modid="typemoonworld",value=Dist.CLIENT)
public final class CuChulainnVisualClient {
   private static final ResourceLocation LAYER=ResourceLocation.fromNamespaceAndPath("typemoonworld","cu_action");
   public record Action(int slot,long start,long end,boolean left) { }
   private record Barrier(Vec3 center,long end,long flash) { }
   private static final Map<UUID,Action> ACTIONS=new HashMap<>();
   private static final Map<UUID,Barrier> BARRIERS=new HashMap<>();
   private static net.minecraft.client.multiplayer.ClientLevel activeLevel;
   private CuChulainnVisualClient() { }
   @EventBusSubscriber(modid="typemoonworld",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
   public static final class Setup {
      @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER,1003,p->{
         ModifierLayer<KeyframeAnimationPlayer> layer=new ModifierLayer<>();
         layer.addModifierLast(new FirstPersonModifier(){
            @Override public dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode getFirstPersonMode(float tick){return getAnim()!=null&&getAnim().isActive()?dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.THIRD_PERSON_MODEL:dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.DISABLED;}
         });layer.addModifierLast(new CuChulainnActionAnimationClient.WeaponArmModifier(p));return layer;
      }));}
   }
   public static String animation(int slot) {return slot==8?"cu_sweep":slot==9?"cu_flurry":slot==11?"cu_throw":slot==10?"cu_raise":slot==-1?"cu_thrust":"cu_rune";}
   public static void receive(CuChulainnVisualMessage message) {
      Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;long now=mc.level.getGameTime();
      ensureLevel();
      if(message.action()==-100){stop(message.owner());return;}
      if(message.action()==100) {
         if(message.ticks()<=0)BARRIERS.remove(message.owner());else BARRIERS.put(message.owner(),new Barrier(message.center(),now+message.ticks(),message.flash()?now+5:0));return;
      }
      Action previous=ACTIONS.get(message.owner());
      if(message.action()==10 && previous!=null && previous.slot==10 && previous.end>now){
         ACTIONS.put(message.owner(),new Action(10,previous.start,message.start()+message.ticks(),message.left()));return;
      }
      ACTIONS.put(message.owner(),new Action(message.action(),message.start(),message.start()+message.ticks(),message.left()));
      CuChulainnSpearLightClient.begin(message.owner());
      for(var player:mc.level.players())if(player.getUUID().equals(message.owner())) {
         var raw=PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
         var clip=PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld",animation(message.action())));
         if(raw instanceof ModifierLayer<?> layer && clip instanceof KeyframeAnimation animation) {
            @SuppressWarnings("unchecked") var typed=(ModifierLayer<KeyframeAnimationPlayer>)layer;
            typed.setAnimation(new KeyframeAnimationPlayer(animation,(int)Math.clamp(now-message.start(),0,animation.endTick)));
         }
      }
   }
   public static void pose(LivingEntity owner,HumanoidModel<?> model,float partial) {
      Action action=action(owner);if(action!=null)CuChulainnActionAnimationClient.pose(owner,model,action,age(owner,action,partial));
   }
   private static void ensureLevel() {
      if(activeLevel!=Minecraft.getInstance().level){activeLevel=Minecraft.getInstance().level;ACTIONS.clear();BARRIERS.clear();CuChulainnSpearLightClient.clear();}
   }
   public static Action action(LivingEntity owner) {
      ensureLevel();Action a=ACTIONS.get(owner.getUUID());
      if(a!=null && (owner.isRemoved() || owner.level().getGameTime()>=a.end)) {ACTIONS.remove(owner.getUUID());return null;}
      return a;
   }
   public static float age(LivingEntity owner,Action action,float partial){return owner.level().getGameTime()-action.start+partial;}
   public static void stop(UUID owner) {
      ACTIONS.remove(owner);BARRIERS.remove(owner);
      var mc=Minecraft.getInstance();if(mc.level!=null)for(var p:mc.level.players())if(p.getUUID().equals(owner)){
         var layer=PlayerAnimationAccess.getPlayerAssociatedData(p).get(LAYER);if(layer instanceof ModifierLayer<?> animation)animation.setAnimation(null);
      }
   }
   @SubscribeEvent public static void render(RenderLevelStageEvent event) {
      if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;
      Minecraft mc=Minecraft.getInstance();if(mc.level==null){BARRIERS.clear();ACTIONS.clear();return;}
      long now=mc.level.getGameTime();BARRIERS.values().removeIf(b->now>=b.end);
      var poses=event.getPoseStack();Vec3 camera=event.getCamera().getPosition();
      var buffers=mc.renderBuffers().bufferSource();var vertices=buffers.getBuffer(RenderType.debugQuads());
      for(Barrier barrier:BARRIERS.values()) {
         poses.pushPose();poses.translate(barrier.center.x-camera.x,barrier.center.y-camera.y,barrier.center.z-camera.z);
         float alpha=barrier.flash>now?.26F:.065F;
         float s=net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnRules.BARRIER_HALF_WIDTH,h=net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnRules.BARRIER_HEIGHT;
         float[][] p={{-s,0,-s},{s,0,-s},{s,0,s},{-s,0,s},{-s,h,-s},{s,h,-s},{s,h,s},{-s,h,s}};
         int[][] faces={{0,3,2,1},{4,5,6,7},{0,1,5,4},{1,2,6,5},{2,3,7,6},{3,0,4,7}};
         for(int[] face:faces){for(int i:face)vertex(vertices,poses,p[i],alpha);for(int j=3;j>=0;j--)vertex(vertices,poses,p[face[j]],alpha);}
         poses.popPose();
      }
      buffers.endBatch(RenderType.debugQuads());
   }
   private static void vertex(VertexConsumer v,PoseStack poses,float[] p,float alpha){v.addVertex(poses.last(),p[0],p[1],p[2]).setColor(.45F,.95F,.92F,alpha);}
   @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){ACTIONS.clear();BARRIERS.clear();CuChulainnSpearLightClient.clear();}
}
