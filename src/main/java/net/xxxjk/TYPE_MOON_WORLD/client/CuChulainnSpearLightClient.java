package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.DiarmuidTrailRenderType;
import org.joml.Vector3f;

/** The imported hand/item matrices also define the light's position. */
@EventBusSubscriber(modid="typemoonworld",value=Dist.CLIENT)
public final class CuChulainnSpearLightClient {
   private record Point(Vec3 position,float age) { }
   private static final class Path {
      final ArrayDeque<Point> points=new ArrayDeque<>();
      float sampled=-1;int spark=-1;
   }
   private static final Map<UUID,Path> PATHS=new HashMap<>();
   private static final DustParticleOptions RED=new DustParticleOptions(new Vector3f(1,.015F,.045F),.65F);
   private CuChulainnSpearLightClient() { }
   public static void begin(UUID owner){PATHS.remove(owner);}
   public static void clear(){PATHS.clear();}
   @SubscribeEvent public static void render(RenderLevelStageEvent event) {
      if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
      var mc=Minecraft.getInstance();if(mc.level==null){clear();return;}
      var buffers=mc.renderBuffers().bufferSource();var type=DiarmuidTrailRenderType.trail();
      Vec3 camera=event.getCamera().getPosition();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
      var alive=new java.util.HashSet<UUID>();
      for(var entity:mc.level.entitiesForRendering()) {
         if(!(entity instanceof LivingEntity owner))continue;
         var action=CuChulainnVisualClient.action(owner);
         if(action==null || action.slot()!=8 && action.slot()!=9 && action.slot()!=-1 && action.slot()!=10 && action.slot()!=11)continue;
         alive.add(owner.getUUID());float age=CuChulainnVisualClient.age(owner,action,partial);
         var path=PATHS.computeIfAbsent(owner.getUUID(),ignored->new Path());
         float tail=action.slot()==8?4.5F:3;
         while(!path.points.isEmpty() && age-path.points.getFirst().age()>tail)path.points.removeFirst();
         float first=path.sampled<0?Math.max(0,age-.5F):Math.max(path.sampled+.06F,age-1);
         for(float frame=first;frame<=age;frame+=.10F)sample(owner,action,frame,partial,path);
         sample(owner,action,age,partial,path);path.sampled=age;
         while(path.points.size()>100)path.points.removeFirst();
         if(!path.points.isEmpty() && path.spark!=(int)age) {
            path.spark=(int)age;Vec3 tip=path.points.getLast().position();mc.level.addParticle(RED,tip.x,tip.y,tip.z,0,.004,0);
         }
         if(path.points.size()>=2)ribbon(event.getPoseStack(),buffers.getBuffer(type),new ArrayList<>(path.points),camera,age,tail,action.slot()==8?.24F:.15F);
      }
      PATHS.keySet().retainAll(alive);buffers.endBatch(type);
   }
   private static void sample(LivingEntity owner,CuChulainnVisualClient.Action action,float age,float partial,Path path) {
      Vec3 tip=CuChulainnActionAnimationClient.spearTip(owner,action,age);if(tip==null)return;
      float yaw=Mth.rotLerp(partial,owner.yBodyRotO,owner.yBodyRot);
      Vec3 point=owner.getPosition(partial).add(tip.yRot((180-yaw)*Mth.DEG_TO_RAD));
      if(path.points.isEmpty() || path.points.getLast().position().distanceToSqr(point)>.00002)path.points.addLast(new Point(point,age));
   }
   private static void ribbon(PoseStack poses,VertexConsumer vertices,java.util.List<Point> path,Vec3 camera,float age,float tail,float width) {
      for(int strip=0;strip<8;strip++)for(int i=1;i<path.size();i++) {
         Vec3 a=path.get(i-1).position().subtract(camera),b=path.get(i).position().subtract(camera),axis=b.subtract(a);
         if(axis.lengthSqr()<1e-10)continue;
         Vec3 side=axis.cross(a.scale(-1)).normalize();if(side.lengthSqr()<1e-8)side=axis.cross(new Vec3(0,1,0)).normalize();
         float lifeA=1-Mth.clamp((age-path.get(i-1).age())/tail,0,1),lifeB=1-Mth.clamp((age-path.get(i).age())/tail,0,1);
         float lo=-1+strip*.25F,hi=lo+.25F;
         vertex(poses,vertices,a.add(side.scale(width*lo)),lo,lifeA);vertex(poses,vertices,b.add(side.scale(width*lo)),lo,lifeB);
         vertex(poses,vertices,b.add(side.scale(width*hi)),hi,lifeB);vertex(poses,vertices,a.add(side.scale(width*hi)),hi,lifeA);
      }
   }
   private static void vertex(PoseStack poses,VertexConsumer vertices,Vec3 point,float edge,float life) {
      float core=1-Math.abs(edge),hot=(float)Math.pow(core,6);
      vertices.addVertex(poses.last(),(float)point.x,(float)point.y,(float)point.z).setColor(1,.015F+hot*.34F,.04F+hot*.18F,(float)Math.pow(core,1.4)*life*.9F);
   }
}
