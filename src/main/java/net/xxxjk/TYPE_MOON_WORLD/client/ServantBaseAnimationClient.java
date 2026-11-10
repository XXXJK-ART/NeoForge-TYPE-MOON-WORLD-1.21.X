package net.xxxjk.TYPE_MOON_WORLD.client;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

/** Shared fallback motions for transformed players and humanoid servant NPCs. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ServantBaseAnimationClient {
   public static final int SWORD = 0;
   public static final int FIST = 1;
   public static final int SMALL_LEFT = 2;
   public static final int SMALL_RIGHT = 3;
   public static final int LARGE_FORWARD = 4;
   public static final int LARGE_BACK = 5;
   public static final int LARGE_LEFT = 6;
   public static final int LARGE_RIGHT = 7;
   public static final int LARGE_FORWARD_RIGHT = 8;
   private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_base_action");
   private static final String[] SWORD_IDS = {"servant_base_sword_huijian", "servant_base_sword_huijian1"};
   private static final String[] FIST_IDS = {"servant_base_fist_qingji1", "servant_base_fist_qingji2", "servant_base_fist_lianji"};
   private static final String[] DODGE_IDS = {"", "", "servant_base_dodge_small_shanbi1", "servant_base_dodge_small_shanbi2",
      "servant_base_dodge_large_forward", "servant_base_dodge_large_back", "servant_base_dodge_large_left", "servant_base_dodge_large_right",
      "servant_base_dodge_large_forward_right"};
   private static final Map<UUID, Playback> PLAYBACK = new HashMap<>();
   private static final Map<String, KeyframeAnimationPlayer> SAMPLES = new HashMap<>();
   private static ClientLevel activeLevel;

   private ServantBaseAnimationClient() { }

   @SubscribeEvent public static void register(FMLClientSetupEvent event) {
      event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 990,
         player -> ServantActionAnimationLayer.create()));
   }

   public static void play(UUID id, int action) {
      ClientLevel level = level();
      if (id == null || level == null || action < 0 || action >= DODGE_IDS.length) return;
      Playback previous = PLAYBACK.get(id);
      int variant = previous == null ? 0 : previous.variant + 1;
      String animation = animationId(action, variant);
      if (animation == null || !(PlayerAnimationRegistry.getAnimation(resource(animation)) instanceof KeyframeAnimation clip)) return;
      PLAYBACK.put(id, new Playback(action, level.getGameTime(), variant));
      for (AbstractClientPlayer player : level.players()) if (id.equals(player.getUUID())) {
         if (PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER) instanceof ModifierLayer<?> raw) {
            @SuppressWarnings("unchecked") ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)raw;
            layer.setAnimation(new KeyframeAnimationPlayer(clip));
         }
         break;
      }
   }

   public static void clear(UUID id) { PLAYBACK.remove(id); }

   public static boolean applyNpcPose(LivingEntity owner, HumanoidModel<?> model, float partialTick) {
      if (owner == null || model == null) return false;
      ClientLevel level = level(); if (level == null) return false;
      Playback playback = PLAYBACK.get(owner.getUUID()); if (playback == null) return false;
      float age = level.getGameTime() - playback.start + partialTick;
      String name = animationId(playback.action, playback.variant);
      if (age < 0 || !(PlayerAnimationRegistry.getAnimation(resource(name)) instanceof KeyframeAnimation clip)
         || age >= clip.endTick) { PLAYBACK.remove(owner.getUUID()); return false; }
      int tick = (int)age;
      KeyframeAnimationPlayer sample = sample(name, tick, age - tick);
      if (sample == null) return false;
      apply(model, sample); return true;
   }

   private static KeyframeAnimationPlayer sample(String name, int tick, float partialTick) {
      ResourceLocation id = resource(name);
      if (!(PlayerAnimationRegistry.getAnimation(id) instanceof KeyframeAnimation animation)) return null;
      String key = id.toString(); KeyframeAnimationPlayer player = SAMPLES.get(key);
      if (player == null || player.getData() != animation || player.getTick() != tick) {
         player = new KeyframeAnimationPlayer(animation, tick); SAMPLES.put(key, player);
      }
      player.setupAnim(partialTick);
      return player;
   }

   private static void apply(HumanoidModel<?> model, KeyframeAnimationPlayer sample) {
      apply(sample,"head",model.head); apply(sample,"torso",model.body); apply(sample,"rightArm",model.rightArm);
      apply(sample,"leftArm",model.leftArm); apply(sample,"rightLeg",model.rightLeg); apply(sample,"leftLeg",model.leftLeg);
      model.hat.copyFrom(model.head);
      if (model instanceof PlayerModel<?> p) { p.jacket.copyFrom(p.body); p.rightSleeve.copyFrom(p.rightArm); p.leftSleeve.copyFrom(p.leftArm); p.rightPants.copyFrom(p.rightLeg); p.leftPants.copyFrom(p.leftLeg); }
   }

   private static void apply(KeyframeAnimationPlayer sample, String name, ModelPart part) {
      if (sample.getData().getPart(name) == null) return;
      Vec3f pos=sample.get3DTransform(name,TransformType.POSITION,0,new Vec3f(part.x,part.y,part.z));
      Vec3f rot=sample.get3DTransform(name,TransformType.ROTATION,0,Vec3f.ZERO);
      part.setPos(pos.getX(),pos.getY(),pos.getZ()); part.setRotation(rot.getX(),rot.getY(),rot.getZ());
   }

   private static String animationId(int action, int variant) {
      if (action == SWORD) return SWORD_IDS[variant & 1];
      if (action == FIST) return FIST_IDS[Math.floorMod(variant, FIST_IDS.length)];
      return DODGE_IDS[action];
   }
   private static ResourceLocation resource(String path) { return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,path); }
   private static ClientLevel level() {
      ClientLevel current=Minecraft.getInstance().level;
      if(current!=activeLevel){PLAYBACK.clear();SAMPLES.clear();activeLevel=current;}
      return current;
   }
   private record Playback(int action,long start,int variant) { }
}
