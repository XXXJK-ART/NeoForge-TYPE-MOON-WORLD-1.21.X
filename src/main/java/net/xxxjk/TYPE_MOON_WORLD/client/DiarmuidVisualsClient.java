package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.DiarmuidTrailRenderType;
import org.joml.Vector3f;

/** Client-only ribbons and sparks follow the rendered spear tips and each held weapon's color. */
@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT)
public final class DiarmuidVisualsClient {
   private static final Map<UUID, Swing> SWINGS = new HashMap<>();
   private static final DustParticleOptions RED_SPARK = new DustParticleOptions(new Vector3f(1,0.035F,0.045F),0.65F);
   private static final DustParticleOptions YELLOW_SPARK = new DustParticleOptions(new Vector3f(1,0.86F,0.035F),0.6F);
   private static net.minecraft.client.multiplayer.ClientLevel activeLevel;
   private record Point(Vec3 position, float age) { }
   private static final class Swing {
      final LivingEntity owner;
      final DiarmuidAction action;
      final long start;
      final EnumMap<InteractionHand, ArrayDeque<Point>> paths = new EnumMap<>(InteractionHand.class);
      final EnumMap<InteractionHand, Integer> lastSpark = new EnumMap<>(InteractionHand.class);
      final EnumMap<InteractionHand, Float> sampledAge = new EnumMap<>(InteractionHand.class);
      Swing(LivingEntity owner, DiarmuidAction action, long start) { this.owner=owner; this.action=action; this.start=start; }
   }
   private DiarmuidVisualsClient() { }

   private static void ensureLevel() {
      if (activeLevel != Minecraft.getInstance().level) { activeLevel=Minecraft.getInstance().level; SWINGS.clear(); }
   }
   public static void begin(LivingEntity owner, DiarmuidAction action, long start) {
      ensureLevel(); SWINGS.put(owner.getUUID(),new Swing(owner,action,start));
   }
   public static void stop(UUID id) { SWINGS.remove(id); }
   public static boolean isAnimating(UUID id) {
      ensureLevel();
      Swing swing=SWINGS.get(id);
      return swing!=null && activeLevel!=null && activeLevel.getGameTime()-swing.start<swing.action.duration();
   }
   public static int activeTrailCount(UUID id) {
      ensureLevel(); Swing swing=SWINGS.get(id);
      return swing == null ? 0 : (int)swing.paths.values().stream().filter(path -> path.size()>1).count();
   }

   @SubscribeEvent
   public static void render(RenderLevelStageEvent event) {
      if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
      ensureLevel();
      if (activeLevel==null) return;
      for (var entity : activeLevel.entitiesForRendering()) {
         if (entity instanceof DiarmuidUaDuibhneEntity npc) {
            var action=npc.getDiarmuidAction();
            float age=npc.getDiarmuidActionAge(0);
            if (action==null || age<0 || action!=DiarmuidAction.FIGURE_EIGHT && age>=action.duration()) continue;
            Swing previous=SWINGS.get(npc.getUUID());
            if (previous==null || previous.action!=action) begin(npc,action,activeLevel.getGameTime()-(long)age);
         }
      }
      SWINGS.values().removeIf(s -> !s.owner.isAlive() || s.owner.isRemoved() || s.owner.level()!=activeLevel
         || s.action!=DiarmuidAction.FIGURE_EIGHT && activeLevel.getGameTime()-s.start>=s.action.duration());
      var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
      Vec3 camera=event.getCamera().getPosition();
      float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
      for (Swing swing : SWINGS.values()) {
         float age=activeLevel.getGameTime()-swing.start+partial;
         if (swing.action==DiarmuidAction.FIGURE_EIGHT) age%=swing.action.duration();
         float yaw=Mth.rotLerp(partial,swing.owner.yBodyRotO,swing.owner.yBodyRot);
         for (var hand : InteractionHand.values()) {
            var stack=swing.owner.getItemInHand(hand);
            if (!(stack.getItem() instanceof DiarmuidSpearItem spear)) { swing.paths.remove(hand); continue; }
            // Hand swaps change the light immediately; animation labels never determine its color.
            boolean red=spear.spearType()==DiarmuidSpearItem.SpearType.GAE_DEARG;
            // A technique glows throughout windup, active motion and recovery. Single-hand basics
            // illuminate only the attacking hand; swapping weapons still swaps the actual hue.
            boolean single=swing.action==DiarmuidAction.RED_BASIC || swing.action==DiarmuidAction.YELLOW_BASIC || swing.action.dance();
            boolean left=hand==InteractionHand.MAIN_HAND ? swing.owner.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT
               : swing.owner.getMainArm()!=net.minecraft.world.entity.HumanoidArm.LEFT;
            boolean leftAction=swing.action==DiarmuidAction.YELLOW_BASIC || swing.action==DiarmuidAction.YELLOW_DANCE;
            boolean drawing=age>=0 && age<swing.action.duration() && (!single || left==leftAction);
            var path=swing.paths.computeIfAbsent(hand,ignored -> new ArrayDeque<>());
            float tailLife=swing.action==DiarmuidAction.FIGURE_EIGHT || swing.action.dance()?6.5F:5.0F;
            while (!path.isEmpty() && age-path.getFirst().age()>tailLife) path.removeFirst();
            if (drawing) {
               float last=swing.sampledAge.getOrDefault(hand,age-0.25F);
               // Dense sub-frame sampling keeps the shoulder loops and fast thrusts continuous.
               for (float frame=Math.max(last+0.04F,age-1.0F);frame<age;frame+=0.10F) samplePoint(swing,hand,frame,partial,yaw,path);
               samplePoint(swing,hand,age,partial,yaw,path);
               swing.sampledAge.put(hand,age);
               while (path.size()>160) path.removeFirst();
               if (!path.isEmpty()) {
                  int beat=(int)age;
                  if (swing.lastSpark.getOrDefault(hand,-1)!=beat) {
                     swing.lastSpark.put(hand,beat);
                     Vec3 tip=path.getLast().position();
                     for (int i=0;i<(swing.action==DiarmuidAction.FIGURE_EIGHT?3:2);i++) {
                        double spread=swing.action==DiarmuidAction.ROSE_DANCE?0.10:0.055;
                        activeLevel.addParticle(red?RED_SPARK:YELLOW_SPARK,
                           tip.x+(activeLevel.random.nextDouble()-0.5)*spread,
                           tip.y+(activeLevel.random.nextDouble()-0.5)*spread,
                           tip.z+(activeLevel.random.nextDouble()-0.5)*spread,0,0.005,0);
                     }
                  }
               }
            } else swing.sampledAge.remove(hand);
            if (path.size()>=2) ribbon(event.getPoseStack(),buffers,new ArrayList<>(path),camera,red,age,tailLife);
         }
      }
   }

   private static void samplePoint(Swing swing,InteractionHand hand,float frame,float partial,float yaw,ArrayDeque<Point> path) {
      Vec3 tip=DiarmuidActionAnimationClient.spearTip(swing.owner,swing.action,frame,hand);
      if (tip==null) return;
      Vec3 point=swing.owner.getPosition(partial).add(tip.yRot((180-yaw)*Mth.DEG_TO_RAD));
      if (path.isEmpty() || path.getLast().position().distanceToSqr(point)>0.00008) path.addLast(new Point(point,frame));
   }

   private static void ribbon(PoseStack poses,MultiBufferSource.BufferSource buffers,List<Point> path,Vec3 camera,
                              boolean red,float age,float tailLife) {
      RenderType type=DiarmuidTrailRenderType.trail();
      VertexConsumer vertices=buffers.getBuffer(type);
      // Eight transverse gradient strips: transparent colored edge -> saturated band -> hot core.
      // Both cross-section and tail age interpolate per vertex, without a reused blade texture.
      for (int layer=0;layer<8;layer++) {
         Vec3 previousSide=Vec3.ZERO;
         for (int i=1;i<path.size();i++) {
            Vec3 a=path.get(i-1).position().subtract(camera),b=path.get(i).position().subtract(camera);
            Vec3 tangent=b.subtract(a);
            if (tangent.lengthSqr()<1.0E-10) continue;
            Vec3 side=tangent.cross(a.scale(-1)).normalize();
            if (side.lengthSqr()<1.0E-8) side=tangent.cross(new Vec3(0,1,0)).normalize();
            if (side.dot(previousSide)<0) side=side.scale(-1);
            Vec3 before=previousSide.lengthSqr()<1.0E-8?side:previousSide;
            float lifeA=1-Mth.clamp((age-path.get(i-1).age())/tailLife,0,1);
            float lifeB=1-Mth.clamp((age-path.get(i).age())/tailLife,0,1);
            double widthA=(red?0.40:0.34)*Math.sqrt(lifeA);
            double widthB=(red?0.40:0.34)*Math.sqrt(lifeB);
            float edgeA=-1+layer*0.25F,edgeB=edgeA+0.25F;
            gradientVertex(poses,vertices,a.add(before.scale(widthA*edgeA)),edgeA,lifeA,red);
            gradientVertex(poses,vertices,b.add(side.scale(widthB*edgeA)),edgeA,lifeB,red);
            gradientVertex(poses,vertices,b.add(side.scale(widthB*edgeB)),edgeB,lifeB,red);
            gradientVertex(poses,vertices,a.add(before.scale(widthA*edgeB)),edgeB,lifeA,red);
            previousSide=side;
         }
      }
      buffers.endBatch(type);
   }

   private static void gradientVertex(PoseStack poses,VertexConsumer vertices,Vec3 point,float edge,float life,boolean red) {
      float center=1-Math.abs(edge);
      float hot=(float)Math.pow(center,5)*life;
      float green=red?Mth.lerp(hot,0.008F,0.16F):Mth.lerp(hot,0.72F,1.0F);
      float blue=red?Mth.lerp(hot,0.018F,0.07F):Mth.lerp(hot,0.002F,0.06F);
      float alpha=(float)Math.pow(center,1.4)*life*0.80F;
      vertices.addVertex(poses.last(),(float)point.x,(float)point.y,(float)point.z).setColor(1,green,blue,alpha);
   }
}
