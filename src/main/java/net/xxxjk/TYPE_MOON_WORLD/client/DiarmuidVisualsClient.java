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
   private static final Map<UUID, java.util.EnumMap<InteractionHand, Swing>> HAND_SWINGS = new HashMap<>();
   private static final Map<UUID, EnumMap<InteractionHand, Integer>> BASIC_SEQUENCE = new HashMap<>();
   public record PreviousPose(DiarmuidAction action, String path, float age) { }
   private static final DustParticleOptions RED_SPARK = new DustParticleOptions(new Vector3f(1,0.035F,0.045F),0.65F);
   private static final DustParticleOptions YELLOW_SPARK = new DustParticleOptions(new Vector3f(1,0.86F,0.035F),0.6F);
   private static net.minecraft.client.multiplayer.ClientLevel activeLevel;
   private record Point(Vec3 position, float age) { }
   private static final class Swing {
      final LivingEntity owner;
      final DiarmuidAction action;
      final long start;
      final String animationPath;
      final EnumMap<InteractionHand, PreviousPose> previous = new EnumMap<>(InteractionHand.class);
      final EnumMap<InteractionHand, ArrayDeque<Point>> paths = new EnumMap<>(InteractionHand.class);
      final EnumMap<InteractionHand, Integer> lastSpark = new EnumMap<>(InteractionHand.class);
      final EnumMap<InteractionHand, Float> sampledAge = new EnumMap<>(InteractionHand.class);
      Swing(LivingEntity owner, DiarmuidAction action, long start, InteractionHand selected) {
         this.owner=owner; this.action=action; this.start=start;
         for (InteractionHand hand : InteractionHand.values()) {
            var hands = HAND_SWINGS.get(owner.getUUID());
            Swing old = hands == null ? null : hands.get(hand);
            if (old == null) old = SWINGS.get(owner.getUUID());
            if (old != null && start - old.start < old.action.visualDuration())
               previous.put(hand, new PreviousPose(old.action, old.animationPath, start - old.start));
         }
         String path = action.animationPath();
         if (action == DiarmuidAction.RED_BASIC || action == DiarmuidAction.YELLOW_BASIC) {
            InteractionHand hand = selected;
            if (hand == null) for (InteractionHand candidate : InteractionHand.values())
               if (owner.getItemInHand(candidate).getItem() instanceof DiarmuidSpearItem spear
                  && (spear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG) == (action == DiarmuidAction.RED_BASIC)) hand = candidate;
            if (hand == null) hand = InteractionHand.MAIN_HAND;
            var counts = BASIC_SEQUENCE.computeIfAbsent(owner.getUUID(), ignored -> new EnumMap<>(InteractionHand.class));
            int variant = counts.getOrDefault(hand, 0);
            counts.put(hand, (variant + 1) % 4);
            path += new String[]{"", "_slash_left", "_slash_right", "_rising_cut"}[variant];
         }
         animationPath = path;
      }
   }
   private DiarmuidVisualsClient() { }

   private static void ensureLevel() {
      if (activeLevel != Minecraft.getInstance().level) { activeLevel=Minecraft.getInstance().level; SWINGS.clear(); HAND_SWINGS.clear(); BASIC_SEQUENCE.clear(); }
   }
   public static void begin(LivingEntity owner, DiarmuidAction action, long start) {
      if (action == DiarmuidAction.DOUBLE_SPEAR_STANCE) return;
      ensureLevel();
      Swing next = new Swing(owner, action, start, null);
      HAND_SWINGS.remove(owner.getUUID());
      SWINGS.put(owner.getUUID(), next);
   }
   public static void stop(UUID id) { SWINGS.remove(id); HAND_SWINGS.remove(id); }
   public static void beginHand(LivingEntity owner, DiarmuidAction action, InteractionHand hand, long start) {
      ensureLevel();
      HAND_SWINGS.computeIfAbsent(owner.getUUID(), ignored -> new java.util.EnumMap<>(InteractionHand.class))
         .put(hand, new Swing(owner, action, start, hand));
   }
   public static void stopHand(UUID id, InteractionHand hand) {
      var hands = HAND_SWINGS.get(id);
      if (hands == null) return;
      hands.remove(hand);
      if (hands.isEmpty()) HAND_SWINGS.remove(id);
   }
   public static DiarmuidAction handAction(UUID id, InteractionHand hand) {
      ensureLevel();
      Swing swing = HAND_SWINGS.getOrDefault(id, new java.util.EnumMap<>(InteractionHand.class)).get(hand);
      return swing == null || activeLevel == null || activeLevel.getGameTime() - swing.start >= swing.action.visualDuration() ? null : swing.action;
   }
   private static Swing swingFor(UUID id, DiarmuidAction action, InteractionHand hand) {
      ensureLevel();
      var hands = HAND_SWINGS.get(id);
      Swing swing = hands == null ? null : hands.get(hand);
      if (swing == null || swing.action != action) swing = SWINGS.get(id);
      return swing != null && swing.action == action && activeLevel != null
         && activeLevel.getGameTime() - swing.start < action.visualDuration() ? swing : null;
   }
   public static String animationPath(UUID id, DiarmuidAction action, InteractionHand hand) {
      Swing swing = swingFor(id, action, hand);
      return swing == null ? action.animationPath() : swing.animationPath;
   }
   public static PreviousPose previousPose(UUID id, DiarmuidAction action, InteractionHand hand) {
      Swing swing = swingFor(id, action, hand);
      return swing == null ? null : swing.previous.get(hand);
   }
   public static DiarmuidAction wholeBodyAction(UUID id) {
      return isWholeBodyAnimating(id) ? SWINGS.get(id).action : null;
   }
   public static float wholeBodyActionAge(UUID id, float partial) {
      return isWholeBodyAnimating(id) ? activeLevel.getGameTime() - SWINGS.get(id).start + partial : -1;
   }
   public static float handActionAge(UUID id, InteractionHand hand, float partial) {
      ensureLevel();
      Swing swing = HAND_SWINGS.getOrDefault(id, new java.util.EnumMap<>(InteractionHand.class)).get(hand);
      return swing == null || activeLevel == null ? -1 : activeLevel.getGameTime() - swing.start + partial;
   }
   public static boolean isAnimating(UUID id) {
      ensureLevel();
      Swing swing=SWINGS.get(id);
      if (swing != null && activeLevel!=null && activeLevel.getGameTime()-swing.start<swing.action.visualDuration()) return true;
      var hands = HAND_SWINGS.get(id);
      return hands != null && activeLevel != null && hands.values().stream().anyMatch(hand -> activeLevel.getGameTime()-hand.start<hand.action.visualDuration());
   }
   public static boolean isWholeBodyAnimating(UUID id) {
      ensureLevel();
      Swing swing = SWINGS.get(id);
      return swing != null && swing.action != DiarmuidAction.DOUBLE_SPEAR_STANCE && activeLevel != null && activeLevel.getGameTime()-swing.start<swing.action.visualDuration();
   }
   public static int activeTrailCount(UUID id) {
      ensureLevel(); Swing swing=SWINGS.get(id);
      int whole = swing == null ? 0 : (int)swing.paths.values().stream().filter(path -> path.size()>1).count();
      var hands = HAND_SWINGS.get(id);
      return whole + (hands == null ? 0 : hands.values().stream().mapToInt(s -> (int)s.paths.values().stream().filter(path -> path.size()>1).count()).sum());
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
            if (action==null || action==DiarmuidAction.DOUBLE_SPEAR_STANCE || age<0 || action!=DiarmuidAction.FIGURE_EIGHT && age>=action.duration()) continue;
            Swing previous=SWINGS.get(npc.getUUID());
            if (previous==null || previous.action!=action || Math.abs(previous.start - activeLevel.getGameTime()+(long)age) > 1) begin(npc,action,activeLevel.getGameTime()-(long)age);
         }
      }
      SWINGS.values().removeIf(s -> !s.owner.isAlive() || s.owner.isRemoved() || s.owner.level()!=activeLevel
         || s.action!=DiarmuidAction.FIGURE_EIGHT && activeLevel.getGameTime()-s.start>=s.action.visualDuration());
      for (var entity : activeLevel.entitiesForRendering()) {
         if (!(entity instanceof DiarmuidUaDuibhneEntity npc)) continue;
         for (InteractionHand hand : InteractionHand.values()) {
            DiarmuidAction action = npc.getDiarmuidAction(hand);
            float age = npc.getDiarmuidActionAge(hand, 0);
            if (action != null && age >= 0 && age < action.duration()) {
               var hands = HAND_SWINGS.get(npc.getUUID());
               Swing previous = hands == null ? null : hands.get(hand);
               if (previous == null || previous.action != action || Math.abs(previous.start - activeLevel.getGameTime()+(long)age) > 1) beginHand(npc, action, hand, activeLevel.getGameTime()-(long)age);
            }
         }
      }
      HAND_SWINGS.values().forEach(hands -> hands.values().removeIf(s -> !s.owner.isAlive() || s.owner.isRemoved() || s.owner.level()!=activeLevel
         || activeLevel.getGameTime()-s.start>=s.action.visualDuration()));
      HAND_SWINGS.values().removeIf(java.util.Map::isEmpty);
      var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
      Vec3 camera=event.getCamera().getPosition();
      float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
      for (Swing swing : SWINGS.values()) {
         renderSwing(swing, null, event, buffers, camera, partial);
      }
      for (var hands : HAND_SWINGS.values()) for (var entry : hands.entrySet())
         renderSwing(entry.getValue(),entry.getKey(),event,buffers,camera,partial);
   }

   private static void renderSwing(Swing swing, InteractionHand activeHand, RenderLevelStageEvent event, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
         float age=activeLevel.getGameTime()-swing.start+partial;
         if (swing.action==DiarmuidAction.FIGURE_EIGHT) age%=swing.action.visualDuration();
         float yaw=Mth.rotLerp(partial,swing.owner.yBodyRotO,swing.owner.yBodyRot);
         for (var hand : InteractionHand.values()) {
            if (activeHand != null && hand != activeHand) continue;
            var stack=swing.owner.getItemInHand(hand);
            if (!(stack.getItem() instanceof DiarmuidSpearItem spear)) { swing.paths.remove(hand); continue; }
            // Hand swaps change the light immediately; animation labels never determine its color.
            boolean red=spear.spearType()==DiarmuidSpearItem.SpearType.GAE_DEARG;
            // A technique glows throughout windup, active motion and recovery. Single-hand basics
            // illuminate only the attacking hand; swapping weapons still swaps the actual hue.
            boolean single=swing.action==DiarmuidAction.RED_BASIC || swing.action==DiarmuidAction.YELLOW_BASIC || swing.action.dance();
            boolean redAction=swing.action==DiarmuidAction.RED_BASIC || swing.action==DiarmuidAction.RED_DANCE;
            boolean drawing=age>=0 && age<swing.action.visualDuration() && (!single || activeHand != null || red==redAction);
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
