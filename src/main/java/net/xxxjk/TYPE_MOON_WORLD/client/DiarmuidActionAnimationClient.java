package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.api.layered.modifier.FirstPersonModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DiarmuidSpearItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAction;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;
import org.joml.Vector3f;

@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DiarmuidActionAnimationClient {
   private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "diarmuid_action");
   private static final ResourceLocation HAND_LAYER_LEFT = ResourceLocation.fromNamespaceAndPath("typemoonworld", "diarmuid_left_hand");
   private static final ResourceLocation HAND_LAYER_RIGHT = ResourceLocation.fromNamespaceAndPath("typemoonworld", "diarmuid_right_hand");
   private static final Map<String, KeyframeAnimationPlayer> SAMPLES = new HashMap<>();
   private static PlayerModel<LivingEntity> tipModel;
   private DiarmuidActionAnimationClient() { }

   @SubscribeEvent
   public static void register(FMLClientSetupEvent event) {
      event.enqueueWork(() -> {
         PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000, player -> createLayer(player, null));
         PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(HAND_LAYER_LEFT, 1001, player -> createLayer(player, HumanoidArm.LEFT));
         PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(HAND_LAYER_RIGHT, 1002, player -> createLayer(player, HumanoidArm.RIGHT));
      });
   }

   private static ModifierLayer<KeyframeAnimationPlayer> createLayer(AbstractClientPlayer owner, HumanoidArm arm) {
      ModifierLayer<KeyframeAnimationPlayer> layer = new ModifierLayer<>();
      layer.addModifierLast(new FirstPersonModifier() {
         @Override public dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode getFirstPersonMode(float tickDelta) {
            return getAnim() != null && getAnim().isActive()
               ? dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.THIRD_PERSON_MODEL
               : dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.DISABLED;
         }
      });
      layer.addModifierLast(new WeaponArmModifier(owner, arm));
      return layer;
   }

   private static final class WeaponArmModifier extends AbstractModifier {
      private final AbstractClientPlayer owner;
      private final HumanoidArm selected;
      private WeaponArmModifier(AbstractClientPlayer owner, HumanoidArm selected) { this.owner=owner; this.selected=selected; }
      @Override public boolean isActive() { return anim != null && anim.isActive(); }
      @Override public Vec3f get3DTransform(String part, TransformType type, float tickDelta, Vec3f value0) {
         if (anim == null) return value0;
         HumanoidArm arm = part.startsWith("right") ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
         boolean handPart = part.endsWith("Arm") || part.endsWith("Item");
         if (!handPart) {
            if (selected != null) return value0;
            boolean mirror = "torso".equals(part) && redOnLeft(owner);
            Vec3f result = anim.get3DTransform(part, type, tickDelta, mirror ? reflect(value0, type) : value0);
            return mirror ? reflect(result, type) : result;
         }
         if (selected != null && selected != arm) return value0;
         DiarmuidAction action = selected == null ? DiarmuidVisualsClient.wholeBodyAction(owner.getUUID())
            : DiarmuidVisualsClient.handAction(owner.getUUID(), handFor(owner, arm));
         float age = selected == null ? DiarmuidVisualsClient.wholeBodyActionAge(owner.getUUID(), tickDelta)
            : handActionAge(owner, handFor(owner, arm), tickDelta);
         if (action == null) return value0;
         var sampled = sample(DiarmuidVisualsClient.animationPath(owner.getUUID(), action, handFor(owner, arm)), action, age);
         return sampled == null ? value0 : mappedTransform(owner, sampled, action, age, arm, part.endsWith("Item"), type, value0);
      }
      @Override public void setupAnim(float tickDelta) { if (anim != null) anim.setupAnim(tickDelta); }
      @Override public boolean canRemove() { return false; }
      @Override public dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode getFirstPersonMode(float tickDelta) {
         return isActive() ? dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.THIRD_PERSON_MODEL
            : dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode.DISABLED;
      }
   }

   public static void play(UUID ownerId, int actionId) {
      play(ownerId, actionId, -1);
   }

   public static void play(UUID ownerId, int actionId, int handId) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || ownerId == null) return;
      if (actionId == net.xxxjk.TYPE_MOON_WORLD.network.DiarmuidActionAnimationMessage.FINISH) return;
      DiarmuidAction action = DiarmuidAction.byNetworkId(actionId);
      InteractionHand hand = handId >= 0 && handId < InteractionHand.values().length ? InteractionHand.values()[handId] : null;
      for (var entity : mc.level.entitiesForRendering()) {
         if (entity instanceof LivingEntity owner && ownerId.equals(owner.getUUID())) {
            if (hand != null && owner.getItemInHand(hand).getItem() instanceof DiarmuidSpearItem spear) {
               boolean red = spear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG;
               if (action == DiarmuidAction.RED_BASIC || action == DiarmuidAction.YELLOW_BASIC)
                  action = red ? DiarmuidAction.RED_BASIC : DiarmuidAction.YELLOW_BASIC;
               else if (action == DiarmuidAction.RED_DANCE || action == DiarmuidAction.YELLOW_DANCE)
                  action = red ? DiarmuidAction.RED_DANCE : DiarmuidAction.YELLOW_DANCE;
            }
            if (hand == null) {
               if (action == null || action == DiarmuidAction.DOUBLE_SPEAR_STANCE) DiarmuidVisualsClient.stop(ownerId);
               else DiarmuidVisualsClient.begin(owner, action, mc.level.getGameTime());
            } else if (action == null) DiarmuidVisualsClient.stopHand(ownerId, hand);
            else DiarmuidVisualsClient.beginHand(owner, action, hand, mc.level.getGameTime());
            break;
         }
      }
      for (AbstractClientPlayer player : mc.level.players()) {
         if (!ownerId.equals(player.getUUID())) continue;
         var data = PlayerAnimationAccess.getPlayerAssociatedData(player);
         Object raw = data.get(LAYER);
         if (hand == null && !(raw instanceof ModifierLayer<?>)) return;
         if (hand == null) {
            @SuppressWarnings("unchecked") ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)raw;
            if (action == null || action == DiarmuidAction.DOUBLE_SPEAR_STANCE) {
               layer.setAnimation(null);
               for (ResourceLocation handLayerId : new ResourceLocation[]{HAND_LAYER_LEFT, HAND_LAYER_RIGHT})
                  if (data.get(handLayerId) instanceof ModifierLayer<?> handLayer) handLayer.setAnimation(null);
               return;
            }
            var animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld",
               DiarmuidVisualsClient.animationPath(ownerId, action, InteractionHand.MAIN_HAND)));
            for (ResourceLocation handLayerId : new ResourceLocation[]{HAND_LAYER_LEFT, HAND_LAYER_RIGHT})
               if (data.get(handLayerId) instanceof ModifierLayer<?> handLayer) handLayer.setAnimation(null);
            if (animation instanceof KeyframeAnimation keyframes) layer.setAnimation(new KeyframeAnimationPlayer(keyframes));
         } else {
            var first = data.get(HAND_LAYER_LEFT);
            var second = data.get(HAND_LAYER_RIGHT);
            Object selected = armFor(player, hand) == HumanoidArm.LEFT ? first : second;
            if (selected instanceof ModifierLayer<?> rawHand) {
               @SuppressWarnings("unchecked") ModifierLayer<KeyframeAnimationPlayer> handLayer = (ModifierLayer<KeyframeAnimationPlayer>)rawHand;
               if (action == null) handLayer.setAnimation(null);
               else {
                  var animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld",
                     DiarmuidVisualsClient.animationPath(ownerId, action, hand)));
                  if (animation instanceof KeyframeAnimation keyframes) handLayer.setAnimation(new KeyframeAnimationPlayer(keyframes));
               }
            }
         }
         return;
      }
   }

   private static KeyframeAnimationPlayer sample(DiarmuidAction action, float age) {
      if (action == null) return null;
      return sample(action.animationPath(), action, age);
   }

   private static KeyframeAnimationPlayer sample(String path, DiarmuidAction action, float age) {
      var animation = PlayerAnimationRegistry.getAnimation(ResourceLocation.fromNamespaceAndPath("typemoonworld", path));
      if (!(animation instanceof KeyframeAnimation data)) return null;
      if (action == DiarmuidAction.FIGURE_EIGHT) age = age % action.duration();
      int tick = Math.max(0, (int)age);
      var sample = SAMPLES.get(path);
      if (sample == null || sample.getData() != data || sample.getTick() != tick) {
         sample = new KeyframeAnimationPlayer(data, tick);
         SAMPLES.put(path, sample);
      }
      sample.setupAnim(age - tick);
      return sample;
   }

   public static void applyImportedPose(HumanoidModel<?> model, DiarmuidAction action, float age) {
      var sample = sample(action, age);
      if (sample == null) return;
      apply(sample, "head", model.head);
      apply(sample, "torso", model.body);
      apply(sample, "rightArm", model.rightArm);
      apply(sample, "leftArm", model.leftArm);
      apply(sample, "rightLeg", model.rightLeg);
      apply(sample, "leftLeg", model.leftLeg);
      Vec3f root = sample.get3DTransform("body", TransformType.POSITION, 0, Vec3f.ZERO);
      for (var part : new ModelPart[]{model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg}) {
         part.x += root.getX(); part.y += root.getY(); part.z += root.getZ();
      }
      model.hat.copyFrom(model.head);
      if (model instanceof PlayerModel<?> player) {
         player.jacket.copyFrom(player.body);
         player.rightSleeve.copyFrom(player.rightArm); player.leftSleeve.copyFrom(player.leftArm);
         player.rightPants.copyFrom(player.rightLeg); player.leftPants.copyFrom(player.leftLeg);
      }
   }

   public static void applyBodyTransform(PoseStack poses, DiarmuidAction action, float age) {
      var sample = sample(action, age);
      if (sample == null) return;
      Vec3f rotation = sample.get3DTransform("body", TransformType.ROTATION, 0, Vec3f.ZERO);
      // Match PlayerAnimator's hip pivot for NPCs and actual spear-tip sampling.
      poses.translate(0, 0.7, 0);
      poses.mulPose(Axis.ZP.rotation(rotation.getZ()));
      poses.mulPose(Axis.YP.rotation(rotation.getY()));
      poses.mulPose(Axis.XP.rotation(rotation.getX()));
      poses.translate(0, -0.7, 0);
   }

   private static void apply(KeyframeAnimationPlayer sample, String name, ModelPart part) {
      var track = sample.getData().getPart(name);
      if (track == null || !track.isEnabled()) return;
      Vec3f position = sample.get3DTransform(name, TransformType.POSITION, 0, new Vec3f(part.x, part.y, part.z));
      Vec3f rotation = sample.get3DTransform(name, TransformType.ROTATION, 0, new Vec3f(part.xRot, part.yRot, part.zRot));
      part.setPos(position.getX(), position.getY(), position.getZ());
      part.setRotation(rotation.getX(), rotation.getY(), rotation.getZ());
   }

   private static HumanoidArm armFor(LivingEntity owner, InteractionHand hand) {
      return hand == InteractionHand.MAIN_HAND ? owner.getMainArm() : owner.getMainArm().getOpposite();
   }

   private static InteractionHand handFor(LivingEntity owner, HumanoidArm arm) {
      return arm == owner.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
   }

   private static boolean redOnLeft(LivingEntity owner) {
      return owner.getItemInHand(handFor(owner, HumanoidArm.LEFT)).getItem() instanceof DiarmuidSpearItem spear
         && spear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG;
   }

   private static Vec3f reflect(Vec3f value, TransformType type) {
      if (type == TransformType.POSITION) return new Vec3f(-value.getX(), value.getY(), value.getZ());
      if (type == TransformType.ROTATION) return new Vec3f(value.getX(), -value.getY(), -value.getZ());
      return value;
   }

   private static String sourceTrack(LivingEntity owner, DiarmuidAction action, HumanoidArm arm, boolean item) {
      boolean right = arm == HumanoidArm.RIGHT;
      if (action == DiarmuidAction.RED_BASIC || action == DiarmuidAction.RED_DANCE) right = true;
      else if (action == DiarmuidAction.YELLOW_BASIC || action == DiarmuidAction.YELLOW_DANCE) right = false;
      else if (owner.getItemInHand(handFor(owner, arm)).getItem() instanceof DiarmuidSpearItem spear)
         right = spear.spearType() == DiarmuidSpearItem.SpearType.GAE_DEARG;
      return (right ? "right" : "left") + (item ? "Item" : "Arm");
   }

   private static Vec3f mappedTransform(LivingEntity owner, KeyframeAnimationPlayer sample, DiarmuidAction action,
      float age, HumanoidArm arm, boolean item, TransformType type, Vec3f vanilla) {
      String source = sourceTrack(owner, action, arm, item);
      if (action != DiarmuidAction.DOUBLE_SPEAR_STANCE) {
         var actual = sample(DiarmuidVisualsClient.animationPath(owner.getUUID(), action, handFor(owner, arm)), action, age);
         if (actual != null) sample = actual;
      }
      boolean mirror = source.startsWith("right") != (arm == HumanoidArm.RIGHT);
      Vec3f result = sample.get3DTransform(source, type, 0, mirror ? reflect(vanilla, type) : vanilla);
      result = mirror ? reflect(result, type) : result;
      return blendStance(owner, action, age, arm, item, type, result);
   }

   private static float smooth(float value) {
      value = Math.max(0, Math.min(1, value));
      return value * value * (3 - 2 * value);
   }

   private static Vec3f blendStance(LivingEntity owner, DiarmuidAction action, float age, HumanoidArm arm,
      boolean item, TransformType type, Vec3f value) {
      if (action == null || action == DiarmuidAction.DOUBLE_SPEAR_STANCE || !owner.onGround() || !stanceEnabled(owner)
         || type != TransformType.POSITION && type != TransformType.ROTATION) return value;
      float incoming = 1 - smooth(age);
      float outgoing = smooth((age - action.visualDuration() + 2) / 2);
      float weight = Math.max(incoming, outgoing);
      if (weight <= 0) return value;
      var stance = sample(DiarmuidAction.DOUBLE_SPEAR_STANCE, 6);
      if (stance == null) return value;
      Vec3f target = mappedTransform(owner, stance, DiarmuidAction.DOUBLE_SPEAR_STANCE, 6, arm, item, type, value);
      var previous = DiarmuidVisualsClient.previousPose(owner.getUUID(), action, handFor(owner, arm));
      if (incoming > 0 && previous != null) {
         var old = sample(previous.path(), previous.action(), previous.age());
         if (old != null) {
            String source = sourceTrack(owner, previous.action(), arm, item);
            boolean mirror = source.startsWith("right") != (arm == HumanoidArm.RIGHT);
            Vec3f oldValue = old.get3DTransform(source, type, 0, mirror ? reflect(value, type) : value);
            target = mirror ? reflect(oldValue, type) : oldValue;
            target = interpolate(target, mappedTransform(owner, stance, DiarmuidAction.DOUBLE_SPEAR_STANCE, 6, arm, item, type, target),
               smooth((previous.age() - previous.action().visualDuration() + 2) / 2), type);
         }
      }
      return interpolate(value, target, weight, type);
   }

   private static Vec3f interpolate(Vec3f from, Vec3f to, float weight, TransformType type) {
      float x = to.getX() - from.getX(), y = to.getY() - from.getY(), z = to.getZ() - from.getZ();
      if (type == TransformType.ROTATION) {
         x = net.minecraft.util.Mth.wrapDegrees(x * 180F / (float)Math.PI) * (float)Math.PI / 180F;
         y = net.minecraft.util.Mth.wrapDegrees(y * 180F / (float)Math.PI) * (float)Math.PI / 180F;
         z = net.minecraft.util.Mth.wrapDegrees(z * 180F / (float)Math.PI) * (float)Math.PI / 180F;
      }
      return new Vec3f(from.getX() + x * weight, from.getY() + y * weight, from.getZ() + z * weight);
   }

   private static void applyArm(LivingEntity owner, HumanoidModel<?> model, KeyframeAnimationPlayer sample,
      DiarmuidAction action, float age, HumanoidArm arm) {
      ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
      Vec3f position = mappedTransform(owner, sample, action, age, arm, false, TransformType.POSITION, new Vec3f(part.x, part.y, part.z));
      Vec3f rotation = mappedTransform(owner, sample, action, age, arm, false, TransformType.ROTATION, new Vec3f(part.xRot, part.yRot, part.zRot));
      part.setPos(position.getX(), position.getY(), position.getZ());
      part.setRotation(rotation.getX(), rotation.getY(), rotation.getZ());
      if (model instanceof PlayerModel<?> player) {
         if (arm == HumanoidArm.RIGHT) player.rightSleeve.copyFrom(part);
         else player.leftSleeve.copyFrom(part);
      }
   }

   public static void applyImportedPose(LivingEntity owner, HumanoidModel<?> model, DiarmuidAction action, float age) {
      if (action == DiarmuidAction.DOUBLE_SPEAR_STANCE) { applyIdlePose(owner, model); return; }
      applyImportedPose(model, action, age);
      var sample = sample(action, age);
      if (sample == null) return;
      if (redOnLeft(owner)) {
         model.body.x = -model.body.x; model.body.yRot = -model.body.yRot; model.body.zRot = -model.body.zRot;
         if (model instanceof PlayerModel<?> player) player.jacket.copyFrom(player.body);
      }
      for (HumanoidArm arm : HumanoidArm.values()) applyArm(owner, model, sample, action, age, arm);
   }

   public static boolean stanceIdle(LivingEntity owner) {
      if (!owner.onGround() || DiarmuidVisualsClient.isWholeBodyAnimating(owner.getUUID())) return false;
      if (owner instanceof DiarmuidUaDuibhneEntity npc && npc.getDiarmuidAction() != null
         && npc.getDiarmuidAction() != DiarmuidAction.DOUBLE_SPEAR_STANCE
         && npc.getDiarmuidActionAge(0) < npc.getDiarmuidAction().duration()) return false;
      return stanceEnabled(owner);
   }

   private static boolean stanceEnabled(LivingEntity owner) {
      if (owner instanceof DiarmuidUaDuibhneEntity npc) return npc.isDoubleSpearStance();
      if (owner instanceof AbstractClientPlayer player) {
         var vars=player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "diarmuid_ua_duibhne".equals(vars.servant_card_id) && vars.servant_card_action_mode==1;
      }
      return false;
   }

   public static void applyIdlePose(LivingEntity owner, HumanoidModel<?> model) {
      if (!stanceIdle(owner)) return;
      var ready=sample(DiarmuidAction.DOUBLE_SPEAR_STANCE,6);
      if (ready==null) return;
      for (HumanoidArm arm : HumanoidArm.values())
         if (!hasHandAction(owner, handFor(owner, arm))) applyArm(owner, model, ready, DiarmuidAction.DOUBLE_SPEAR_STANCE, 6, arm);
   }

   public static boolean hasHandAction(LivingEntity owner, InteractionHand hand) {
      return DiarmuidVisualsClient.handAction(owner.getUUID(), hand) != null
         || owner instanceof DiarmuidUaDuibhneEntity npc && npc.getDiarmuidAction(hand) != null;
   }

   private static float handActionAge(LivingEntity owner, InteractionHand hand, float partialTick) {
      if (DiarmuidVisualsClient.handAction(owner.getUUID(), hand) != null)
         return DiarmuidVisualsClient.handActionAge(owner.getUUID(), hand, partialTick);
      return owner instanceof DiarmuidUaDuibhneEntity npc ? npc.getDiarmuidActionAge(hand, partialTick)
         : DiarmuidVisualsClient.handActionAge(owner.getUUID(), hand, partialTick);
   }

   public static void applyHandPose(LivingEntity owner, HumanoidModel<?> model, float partialTick) {
      for (InteractionHand hand : InteractionHand.values()) {
         DiarmuidAction action = DiarmuidVisualsClient.handAction(owner.getUUID(), hand);
         if (action == null && owner instanceof DiarmuidUaDuibhneEntity npc) action = npc.getDiarmuidAction(hand);
         if (action == null) continue;
         var handSample = sample(action, handActionAge(owner, hand, partialTick));
         if (handSample == null) continue;
         applyArm(owner, model, handSample, action, handActionAge(owner, hand, partialTick), armFor(owner, hand));
      }
   }

   private static void grip(LivingEntity owner, HumanoidModel<?> model, HumanoidArm arm,
      DiarmuidAction action, float age, KeyframeAnimationPlayer sample, PoseStack poses) {
      model.translateToHand(arm, poses);
      poses.mulPose(Axis.XP.rotationDegrees(-90));
      poses.mulPose(Axis.YP.rotationDegrees(180));
      poses.translate(arm == HumanoidArm.LEFT ? -0.0625 : 0.0625, 0.125, -0.625);
      Vec3f position = mappedTransform(owner, sample, action, age, arm, true, TransformType.POSITION, Vec3f.ZERO);
      Vec3f rotation = mappedTransform(owner, sample, action, age, arm, true, TransformType.ROTATION, Vec3f.ZERO);
      poses.translate(position.getX() / 16, position.getY() / 16, position.getZ() / 16);
      poses.mulPose(Axis.ZP.rotation(rotation.getZ()));
      poses.mulPose(Axis.YP.rotation(rotation.getY()));
      poses.mulPose(Axis.XP.rotation(rotation.getX()));
   }

   public static boolean renderImportedHeldItem(LivingEntity owner, HumanoidModel<?> model, HumanoidArm arm,
      ItemStack stack, ItemDisplayContext display, PoseStack poses, MultiBufferSource buffers, int light) {
      if (stack.isEmpty()) return false;
      InteractionHand hand = arm == owner.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
      float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
      if (hasHandAction(owner, hand)) {
         DiarmuidAction handAction = DiarmuidVisualsClient.handAction(owner.getUUID(), hand);
         if (handAction == null && owner instanceof DiarmuidUaDuibhneEntity npc) handAction = npc.getDiarmuidAction(hand);
         return renderHeldItem(owner, model, handAction, handActionAge(owner, hand, partial), arm, stack, display, poses, buffers, light);
      }
      if (stanceIdle(owner)) return renderHeldItem(owner,model,DiarmuidAction.DOUBLE_SPEAR_STANCE,6,arm,stack,display,poses,buffers,light);
      DiarmuidAction playerAction = DiarmuidVisualsClient.wholeBodyAction(owner.getUUID());
      if (playerAction != null)
         return renderHeldItem(owner, model, playerAction, DiarmuidVisualsClient.wholeBodyActionAge(owner.getUUID(), partial), arm, stack, display, poses, buffers, light);
      if (!(owner instanceof DiarmuidUaDuibhneEntity npc)) return false;
      DiarmuidAction action = npc.getDiarmuidAction();
      float age = npc.getDiarmuidActionAge(partial);
      if (action == null || action == DiarmuidAction.DOUBLE_SPEAR_STANCE || age < 0 || age >= action.duration()) return false;
      return renderHeldItem(owner, model, action, age, arm, stack, display, poses, buffers, light);
   }

   public static boolean renderHeldItem(LivingEntity owner, HumanoidModel<?> model, DiarmuidAction action, float age,
      HumanoidArm arm, ItemStack stack, ItemDisplayContext display, PoseStack poses, MultiBufferSource buffers, int light) {
      var sample = sample(action, age);
      if (sample == null || stack.isEmpty()) return false;
      poses.pushPose();
      grip(owner, model, arm, action, age, sample, poses);
      Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(owner, stack, display, arm == HumanoidArm.LEFT, poses, buffers, light);
      poses.popPose();
      return true;
   }

   /** The trail samples the same model, grip, item display transform and shortened geometry. */
   public static Vec3 spearTip(LivingEntity owner, DiarmuidAction action, float age, InteractionHand hand) {
      return spearEndpoint(owner, action, age, hand, true);
   }

   public static Vec3 spearTail(LivingEntity owner, DiarmuidAction action, float age, InteractionHand hand) {
      return spearEndpoint(owner, action, age, hand, false);
   }

   private static Vec3 spearEndpoint(LivingEntity owner, DiarmuidAction action, float age, InteractionHand hand, boolean pointed) {
      ItemStack stack = owner.getItemInHand(hand);
      if (!(stack.getItem() instanceof DiarmuidSpearItem spear)) return null;
      var sample = sample(action, age);
      if (sample == null) return null;
      Minecraft mc = Minecraft.getInstance();
      if (tipModel == null) tipModel = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
      tipModel.body.resetPose(); tipModel.rightArm.resetPose(); tipModel.leftArm.resetPose();
      HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? owner.getMainArm() : owner.getMainArm().getOpposite();
      DiarmuidAction activeHandAction = DiarmuidVisualsClient.handAction(owner.getUUID(), hand);
      if (activeHandAction == action) {
         applyArm(owner, tipModel, sample, action, age, arm);
      } else if (action == DiarmuidAction.DOUBLE_SPEAR_STANCE) {
         for (HumanoidArm side : HumanoidArm.values()) applyArm(owner, tipModel, sample, action, age, side);
      } else applyImportedPose(owner, tipModel, action, age);
      boolean left = arm == HumanoidArm.LEFT;
      ItemDisplayContext display = left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
      PoseStack poses = new PoseStack();
      if (activeHandAction != action) applyBodyTransform(poses, action, age);
      poses.scale(-1, -1, 1);
      poses.translate(0, -1.501, 0);
      grip(owner, tipModel, arm, action, age, sample, poses);
      mc.getItemRenderer().getModel(stack, owner.level(), owner, 0).getTransforms().getTransform(display).apply(left, poses);
      poses.translate(-0.5, -0.5, -0.5);
      poses.translate(0.5, 0.51, 0.5);
      Vec3 meshTip = spearMeshEndpoint(stack, spear, pointed);
      if (meshTip == null) return null;
      Vector3f tip = poses.last().pose().transformPosition((float)meshTip.x, (float)meshTip.y, (float)meshTip.z, new Vector3f());
      return new Vec3(tip.x, tip.y, tip.z);
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   public static Vec3 spearMeshEndpoint(ItemStack stack, software.bernie.geckolib.animatable.GeoItem spear, boolean pointed) {
      software.bernie.geckolib.model.GeoModel geo = software.bernie.geckolib.util.RenderUtil.getGeoModelForItem(stack);
      if (geo == null) return null;
      var baked = geo.getBakedModel(geo.getModelResource(spear));
      var root = geo.getAnimationProcessor().getBone("bone");
      if (root != null && spear instanceof DiarmuidSpearItem diarmuid) root.setScaleZ(diarmuid.spearType() == DiarmuidSpearItem.SpearType.GAE_BUIDHE ? (float)DiarmuidAction.YELLOW_MODEL_LENGTH : 1);
      Vector3f tip = new Vector3f(0, 0, pointed ? -Float.MAX_VALUE : Float.MAX_VALUE);
      PoseStack geometry = new PoseStack();
      for (software.bernie.geckolib.cache.object.GeoBone bone : baked.topLevelBones()) findSpearEndpoint(geometry, bone, tip, pointed);
      return Float.isFinite(tip.z) && Math.abs(tip.z) < Float.MAX_VALUE ? new Vec3(tip.x, tip.y, tip.z) : null;
   }

   private static void findSpearEndpoint(PoseStack geometry, software.bernie.geckolib.cache.object.GeoBone bone, Vector3f tip, boolean pointed) {
      geometry.pushPose();
      software.bernie.geckolib.util.RenderUtil.prepMatrixForBone(geometry, bone);
      for (var cube : bone.getCubes()) {
         geometry.pushPose();
         software.bernie.geckolib.util.RenderUtil.translateToPivotPoint(geometry, cube);
         software.bernie.geckolib.util.RenderUtil.rotateMatrixAroundCube(geometry, cube);
         software.bernie.geckolib.util.RenderUtil.translateAwayFromPivotPoint(geometry, cube);
         for (var quad : cube.quads()) {
            if (quad == null) continue;
            for (var vertex : quad.vertices()) {
               Vector3f point = geometry.last().pose().transformPosition(vertex.position(), new Vector3f());
               if (pointed ? point.z > tip.z : point.z < tip.z) tip.set(point);
            }
         }
         geometry.popPose();
      }
      for (var child : bone.getChildBones()) findSpearEndpoint(geometry, child, tip, pointed);
      geometry.popPose();
   }
}
