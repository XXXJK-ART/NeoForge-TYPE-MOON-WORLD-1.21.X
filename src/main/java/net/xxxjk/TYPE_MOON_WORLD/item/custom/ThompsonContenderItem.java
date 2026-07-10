package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ThompsonContenderRenderer;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.FirearmPoseMessage;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ThompsonContenderItem extends Item implements GeoItem {
   public static final String CONTENDER_ARROW_TAG = "TypeMoonContenderArrow";
   public static final String ORIGIN_ARROW_TAG = "TypeMoonOriginBulletArrow";
   private static final int EMPTY = 0;
   private static final int NORMAL = 1;
   private static final int ORIGIN = 2;
   private static final Map<UUID, Integer> LOADED_ROUNDS = new ConcurrentHashMap<>();
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public ThompsonContenderItem(Properties properties) {
      super(properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private ThompsonContenderRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new ThompsonContenderRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<ThompsonContenderItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResultHolder.pass(stack);
      }
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         handleServerRightClick(serverPlayer, hand);
      }
      return InteractionResultHolder.consume(stack);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.thompson_contender.desc").withStyle(ChatFormatting.GRAY));
   }

   public static boolean handleServerRightClick(ServerPlayer player, InteractionHand hand) {
      ItemStack gun = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND || !gun.is(ModItems.THOMPSON_CONTENDER.get())) {
         return false;
      }
      if (player.getCooldowns().isOnCooldown(gun.getItem())) {
         return true;
      }

      int loaded = LOADED_ROUNDS.getOrDefault(player.getUUID(), EMPTY);
      if (loaded == EMPTY) {
         int loadedKind = tryLoad(player, hand);
         if (loadedKind == EMPTY) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.thompson_contender.no_ammo"), true);
            return false;
         }
         LOADED_ROUNDS.put(player.getUUID(), loadedKind);
         player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.8F, 1.1F);
         player.displayClientMessage(Component.translatable("message.typemoonworld.thompson_contender.loaded", bulletName(loadedKind)), true);
         return true;
      }

      LOADED_ROUNDS.remove(player.getUUID());
      fire(player, loaded == ORIGIN);
      player.getCooldowns().addCooldown(gun.getItem(), 14);
      player.displayClientMessage(Component.translatable("item.typemoonworld.thompson_contender.empty"), true);
      return true;
   }

   private static Component bulletName(int kind) {
      return switch (kind) {
         case NORMAL -> Component.translatable("item.typemoonworld.bullet");
         case ORIGIN -> Component.translatable("item.typemoonworld.origin_bullet");
         default -> Component.translatable("item.typemoonworld.thompson_contender.empty");
      };
   }

   private static int tryLoad(ServerPlayer player, InteractionHand gunHand) {
      InteractionHand otherHand = gunHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      ItemStack offhand = player.getItemInHand(otherHand);
      int offhandKind = kindOf(offhand);
      if (offhandKind != EMPTY) {
         offhand.shrink(1);
         player.getInventory().setChanged();
         return offhandKind;
      }

      Inventory inventory = player.getInventory();
      int normalSlot = findAmmoSlot(inventory, NORMAL);
      if (normalSlot >= 0) {
         inventory.getItem(normalSlot).shrink(1);
         inventory.setChanged();
         return NORMAL;
      }
      int originSlot = findAmmoSlot(inventory, ORIGIN);
      if (originSlot >= 0) {
         inventory.getItem(originSlot).shrink(1);
         inventory.setChanged();
         return ORIGIN;
      }
      return EMPTY;
   }

   private static int findAmmoSlot(Inventory inventory, int kind) {
      for (int i = 0; i < inventory.items.size(); i++) {
         if (kindOf(inventory.items.get(i)) == kind) {
            return i;
         }
      }
      return -1;
   }

   private static int kindOf(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return EMPTY;
      }
      if (stack.is(ModItems.ORIGIN_BULLET.get())) {
         return ORIGIN;
      }
      if (stack.is(ModItems.BULLET.get())) {
         return NORMAL;
      }
      return EMPTY;
   }

   private static void fire(ServerPlayer player, boolean origin) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }

      Vec3 look = player.getLookAngle().normalize();
      Vec3 spawn = player.getEyePosition().add(look.scale(0.65));
      OdaMatchlockBulletEntity bullet = new OdaMatchlockBulletEntity(level, player, null, 20.0F).setBulletKind(origin ? 2 : 3);
      bullet.getPersistentData().putBoolean(CONTENDER_ARROW_TAG, true);
      bullet.getPersistentData().putBoolean(ORIGIN_ARROW_TAG, origin);
      bullet.setPos(spawn.x, spawn.y - 0.05, spawn.z);
      bullet.shoot(look.x, look.y, look.z, 4.8F, 0.0F);
      level.addFreshEntity(bullet);

      level.sendParticles(ParticleTypes.FLASH, spawn.x, spawn.y, spawn.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(origin ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CRIT, spawn.x, spawn.y, spawn.z, origin ? 14 : 8, 0.08, 0.08, 0.08, 0.03);
      drawInitialTrail(level, spawn, look, origin);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, origin ? 1.1F : 0.95F, origin ? 1.35F : 1.65F);
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new FirearmPoseMessage(player.getUUID(), 12), new net.minecraft.network.protocol.common.custom.CustomPacketPayload[0]);
   }

   private static void drawInitialTrail(ServerLevel level, Vec3 start, Vec3 look, boolean origin) {
      for (int i = 1; i <= 16; i++) {
         Vec3 pos = start.add(look.scale(i * 0.7));
         level.sendParticles(origin ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0.0);
         if (i % 4 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 1, 0.025, 0.025, 0.025, 0.0);
         }
      }
   }
}
