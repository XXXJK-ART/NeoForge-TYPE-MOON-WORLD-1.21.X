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
import org.jetbrains.annotations.Nullable;

/** Shared PlayerAnimator playback for servant-card players and humanoid servant NPCs. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ServantDefenseAnimationClient {
   public static final int ACTIVE = 1;
   public static final int PASSIVE = 2;
   private static final int ACTIVE_END_TICK = 13;
   private static final int PASSIVE_END_TICK = 7;
   private static final ResourceLocation PLAYER_ANIMATION_LAYER = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "servant_defense");
   private static final ResourceLocation ACTIVE_ANIMATION = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "servant_defense_active");
   private static final ResourceLocation PASSIVE_ANIMATION = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "servant_defense_passive");
   private static final Map<UUID, GuardState> STATES = new HashMap<>();
   private static final Map<String, KeyframeAnimationPlayer> SAMPLES = new HashMap<>();
   private static ClientLevel activeLevel;

   private ServantDefenseAnimationClient() { }

   @SubscribeEvent
   public static void registerPlayerAnimationLayer(FMLClientSetupEvent event) {
      event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
         PLAYER_ANIMATION_LAYER, 1010, player -> ServantActionAnimationLayer.create()));
   }

   public static void applyGuarding(UUID entityId, boolean guarding) {
      ClientLevel level = level();
      if (level == null || entityId == null) return;
      GuardState state = STATES.computeIfAbsent(entityId, ignored -> new GuardState());
      long now = level.getGameTime();
      if (guarding) {
         if (state.activeStart < 0L || now - state.activeStart >= ACTIVE_END_TICK) {
            state.activeStart = now;
            playPlayer(entityId, ACTIVE_ANIMATION);
         }
      } else {
         state.activeStart = -1L;
         if (state.passiveStart >= 0L && level.getGameTime() - state.passiveStart < PASSIVE_END_TICK) {
            playPlayer(entityId, PASSIVE_ANIMATION);
         } else {
            clearPlayer(entityId);
         }
         removeIfEmpty(entityId, state);
      }
   }

   public static void triggerPassive(UUID entityId) {
      ClientLevel level = level();
      if (level == null || entityId == null) return;
      GuardState state = STATES.computeIfAbsent(entityId, ignored -> new GuardState());
      state.passiveStart = level.getGameTime();
      playPlayer(entityId, PASSIVE_ANIMATION);
   }

   /** Applies the imported animation to a humanoid NPC model. */
   public static void applyImportedPose(LivingEntity owner, HumanoidModel<?> model, float partialTick) {
      if (owner == null || model == null) return;
      GuardState state = state(owner.getUUID());
      long now = level() == null ? 0L : level().getGameTime();
      float age;
      KeyframeAnimationPlayer sample;
      if (state != null && state.passiveStart >= 0L && now - state.passiveStart < PASSIVE_END_TICK) {
         age = now - state.passiveStart + partialTick;
         sample = sample(PASSIVE_ANIMATION, age, PASSIVE_END_TICK);
      } else {
         if (state != null && state.passiveStart >= 0L) state.passiveStart = -1L;
         if (state != null) removeIfEmpty(owner.getUUID(), state);
         return;
      }
      if (sample != null) applySample(model, sample);
   }

   /** Applies the same sampled pose to a player mixin, including first-person guard rendering. */
   public static boolean applyPose(LivingEntity owner, HumanoidModel<?> model, float partialTick) {
      if (owner == null || model == null) return false;
      GuardState state = state(owner.getUUID());
      ClientLevel level = level();
      if (state == null || level == null) return false;
      long now = level.getGameTime();
      KeyframeAnimationPlayer sample = null;
      if (state.passiveStart >= 0L) {
         long elapsed = now - state.passiveStart;
         if (elapsed < PASSIVE_END_TICK) {
            sample = sample(PASSIVE_ANIMATION, elapsed + partialTick, PASSIVE_END_TICK);
         } else {
            state.passiveStart = -1L;
         }
      }
      if (sample == null) {
         removeIfEmpty(owner.getUUID(), state);
         return false;
      }
      applySample(model, sample);
      return true;
   }

   @Nullable
   private static GuardState state(UUID entityId) {
      level();
      return STATES.get(entityId);
   }

   @Nullable
   private static KeyframeAnimationPlayer sample(ResourceLocation id, float age, int endTick) {
      if (!(PlayerAnimationRegistry.getAnimation(id) instanceof KeyframeAnimation animation)) return null;
      int tick = Math.max(0, Math.min(endTick, (int)age));
      String key = id.toString();
      KeyframeAnimationPlayer player = SAMPLES.get(key);
      if (player == null || player.getData() != animation || player.getTick() != tick) {
         player = new KeyframeAnimationPlayer(animation, tick);
         SAMPLES.put(key, player);
      }
      player.setupAnim(Math.max(0.0F, age - tick));
      return player;
   }

   private static void applySample(HumanoidModel<?> model, KeyframeAnimationPlayer sample) {
      model.head.resetPose();
      model.body.resetPose();
      model.rightArm.resetPose();
      model.leftArm.resetPose();
      model.rightLeg.resetPose();
      model.leftLeg.resetPose();
      apply(sample, "head", model.head);
      apply(sample, "torso", model.body);
      apply(sample, "rightArm", model.rightArm);
      apply(sample, "leftArm", model.leftArm);
      apply(sample, "rightLeg", model.rightLeg);
      apply(sample, "leftLeg", model.leftLeg);
      model.hat.copyFrom(model.head);
      if (model instanceof PlayerModel<?> player) {
         player.jacket.copyFrom(player.body);
         player.rightSleeve.copyFrom(player.rightArm);
         player.leftSleeve.copyFrom(player.leftArm);
         player.rightPants.copyFrom(player.rightLeg);
         player.leftPants.copyFrom(player.leftLeg);
      }
   }

   private static void apply(KeyframeAnimationPlayer sample, String name, ModelPart part) {
      if (sample.getData().getPart(name) == null) return;
      Vec3f position = sample.get3DTransform(name, TransformType.POSITION, 0.0F,
         new Vec3f(part.x, part.y, part.z));
      Vec3f rotation = sample.get3DTransform(name, TransformType.ROTATION, 0.0F,
         new Vec3f(0.0F, 0.0F, 0.0F));
      part.setPos(position.getX(), position.getY(), position.getZ());
      part.setRotation(rotation.getX(), rotation.getY(), rotation.getZ());
   }

   private static void playPlayer(UUID entityId, ResourceLocation animationId) {
      ClientLevel level = level();
      if (level == null) return;
      for (AbstractClientPlayer player : level.players()) {
         if (!entityId.equals(player.getUUID())) continue;
         if (!(PlayerAnimationAccess.getPlayerAssociatedData(player).get(PLAYER_ANIMATION_LAYER)
            instanceof ModifierLayer<?> rawLayer)) return;
         if (!(PlayerAnimationRegistry.getAnimation(animationId) instanceof KeyframeAnimation animation)) return;
         @SuppressWarnings("unchecked")
         ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)rawLayer;
         layer.setAnimation(new KeyframeAnimationPlayer(animation));
         return;
      }
   }

   private static void clearPlayer(UUID entityId) {
      ClientLevel level = level();
      if (level == null) return;
      for (AbstractClientPlayer player : level.players()) {
         if (!entityId.equals(player.getUUID())) continue;
         if (PlayerAnimationAccess.getPlayerAssociatedData(player).get(PLAYER_ANIMATION_LAYER)
            instanceof ModifierLayer<?> rawLayer) {
            @SuppressWarnings("unchecked")
            ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)rawLayer;
            layer.setAnimation(null);
         }
         return;
      }
   }

   private static ClientLevel level() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != activeLevel) {
         STATES.clear();
         SAMPLES.clear();
         activeLevel = level;
      }
      return level;
   }

   private static void removeIfEmpty(UUID entityId, GuardState state) {
      if (state.activeStart < 0L && state.passiveStart < 0L) STATES.remove(entityId);
   }

   private static final class GuardState {
      private long activeStart = -1L;
      private long passiveStart = -1L;
   }
}
