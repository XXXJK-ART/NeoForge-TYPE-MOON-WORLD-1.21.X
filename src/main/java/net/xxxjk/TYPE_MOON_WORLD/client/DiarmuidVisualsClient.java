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
import net.minecraft.resources.ResourceLocation;
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
import net.xxxjk.TYPE_MOON_WORLD.vfx.client.VFXMaterialRenderTypes;
import org.joml.Vector3f;

/** Client-only ribbons and sparks follow the rendered spear tips and each held weapon's color. */
@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT)
public final class DiarmuidVisualsClient {
   private static final Map<UUID, Swing> SWINGS = new HashMap<>();
   private static final ResourceLocation TRAIL = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/particle/sasaki_tsubame_blade_stretched.png");
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
            if (action==null || age<0 || age>=action.duration()) continue;
            long start=activeLevel.getGameTime()-(long)age;
            Swing previous=SWINGS.get(npc.getUUID());
            if (previous==null || previous.start!=start) begin(npc,action,start);
         }
      }
      SWINGS.values().removeIf(s -> !s.owner.isAlive() || s.owner.isRemoved() || s.owner.level()!=activeLevel
         || activeLevel.getGameTime()-s.start>=s.action.duration());
      var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
      Vec3 camera=event.getCamera().getPosition();
      float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
      for (Swing swing : SWINGS.values()) {
         float age=activeLevel.getGameTime()-swing.start+partial;
         float yaw=Mth.rotLerp(partial,swing.owner.yBodyRotO,swing.owner.yBodyRot);
         for (var hand : InteractionHand.values()) {
            var stack=swing.owner.getItemInHand(hand);
            if (!(stack.getItem() instanceof DiarmuidSpearItem spear)) { swing.paths.remove(hand); continue; }
            // Hand swaps change the light immediately; animation labels never determine its color.
            boolean red=spear.spearType()==DiarmuidSpearItem.SpearType.GAE_DEARG;
            boolean drawing=swing.action==DiarmuidAction.FIGURE_EIGHT && age>=2 && age<=22
               || swing.action==DiarmuidAction.ROSE_DANCE && age>=2 && age<=7
               || swing.action.hits().stream().anyMatch(hit -> hit.red()==red && age>=hit.tick()-2.5F && age<=hit.tick()+2.0F);
            var path=swing.paths.computeIfAbsent(hand,ignored -> new ArrayDeque<>());
            float tailLife=red?3.6F:2.6F;
            while (!path.isEmpty() && age-path.getFirst().age()>tailLife) path.removeFirst();
            if (drawing) {
               float last=swing.sampledAge.getOrDefault(hand,age-0.25F);
               // Dense sub-frame sampling keeps the shoulder loops and fast thrusts continuous.
               for (float frame=Math.max(last+0.08F,age-0.5F);frame<age;frame+=0.16F) samplePoint(swing,hand,frame,partial,yaw,path);
               samplePoint(swing,hand,age,partial,yaw,path);
               swing.sampledAge.put(hand,age);
               while (path.size()>(red?48:36)) path.removeFirst();
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
      RenderType type=VFXMaterialRenderTypes.get("vanilla",TRAIL,false);
      VertexConsumer vertices=buffers.getBuffer(type);
      // Colored bloom, saturated edge and narrow hot core all retain the spear's own hue.
      for (int layer=0;layer<3;layer++) {
         Vec3 previousSide=Vec3.ZERO;
         for (int i=1;i<path.size();i++) {
            Vec3 a=path.get(i-1).position().subtract(camera),b=path.get(i).position().subtract(camera);
            Vec3 tangent=b.subtract(a);
            if (tangent.lengthSqr()<1.0E-10) continue;
            Vec3 side=tangent.cross(a.scale(-1)).normalize();
            if (side.lengthSqr()<1.0E-8) side=tangent.cross(new Vec3(0,1,0)).normalize();
            if (side.dot(previousSide)<0) side=side.scale(-1);
            Vec3 before=previousSide.lengthSqr()<1.0E-8?side:previousSide;
            float taper=(float)Math.sqrt(i/(double)path.size());
            double width=(layer==0?(red?0.16:0.12):layer==1?(red?0.065:0.05):0.017)*taper;
            float alpha=(layer==0?0.22F:layer==1?0.65F:0.95F)*taper
               *(1-Mth.clamp((age-path.get(i).age())/tailLife,0,1));
            float green=red?(layer==2?0.28F:0.035F):(layer==2?0.98F:0.82F);
            float blue=red?(layer==2?0.22F:0.065F):(layer==2?0.26F:0.025F);
            vertex(poses,vertices,a.subtract(before.scale(width)),0,0,green,blue,alpha);
            vertex(poses,vertices,b.subtract(side.scale(width)),1,0,green,blue,alpha);
            vertex(poses,vertices,b.add(side.scale(width)),1,1,green,blue,alpha);
            vertex(poses,vertices,a.add(before.scale(width)),0,1,green,blue,alpha);
            previousSide=side;
         }
      }
      buffers.endBatch(type);
   }

   private static void vertex(PoseStack poses,VertexConsumer vertices,Vec3 point,float u,float v,float g,float b,float alpha) {
      vertices.addVertex(poses.last(),(float)point.x,(float)point.y,(float)point.z).setUv(u,v).setColor(1,g,b,alpha);
   }
}
