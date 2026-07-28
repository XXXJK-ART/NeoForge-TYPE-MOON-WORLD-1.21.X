package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.NightingaleGunRenderer;
import net.xxxjk.TYPE_MOON_WORLD.entity.NightingaleBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.FirearmPoseMessage;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardNightingaleSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleSupportService;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NightingaleGunItem extends Item implements GeoItem {
   private static final Map<UUID, Boolean> LOADED = new ConcurrentHashMap<>();
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public NightingaleGunItem(Properties properties) {
      super(properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private NightingaleGunRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (renderer == null) renderer = new NightingaleGunRenderer();
            return renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0,
         state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.nightingale_gun.idle"))));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return cache;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) handleUse(serverPlayer);
      return InteractionResultHolder.consume(stack);
   }

   private static void handleUse(ServerPlayer player) {
      ItemStack gun = player.getMainHandItem();
      if (!gun.is(ModItems.NIGHTINGALE_GUN.get()) || player.getCooldowns().isOnCooldown(gun.getItem())
         || ServantCardNightingaleSkills.isCasting(player)) return;
      if (!LOADED.getOrDefault(player.getUUID(), false)) {
         if (requiresAmmunition(player) && !consumeBullet(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.nightingale_gun.no_ammo"), true);
            return;
         }
         LOADED.put(player.getUUID(), true);
         player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.8F, 1.15F);
         player.displayClientMessage(Component.translatable("message.typemoonworld.nightingale_gun.loaded"), true);
         return;
      }
      LOADED.remove(player.getUUID());
      fire(player, player.getLookAngle(), 4.8F);
      ServantCardVoiceHelper.tryPlayAttack(player);
      player.getCooldowns().addCooldown(gun.getItem(),
         NightingaleSupportService.adjustActionTicks(player, 14, player.level().getGameTime()));
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new FirearmPoseMessage(player.getUUID(), 12));
   }

   public static boolean requiresAmmunition(Player player) {
      return player != null && !NightingaleSupportService.isNightingaleSource(player);
   }

   private static boolean consumeBullet(ServerPlayer player) {
      if (player.getAbilities().instabuild) return true;
      Inventory inventory = player.getInventory();
      for (int slot = 0; slot < inventory.items.size(); slot++) {
         ItemStack stack = inventory.items.get(slot);
         if (stack.is(ModItems.BULLET.get())) {
            stack.shrink(1);
            inventory.setChanged();
            return true;
         }
      }
      return false;
   }

   public static void fire(LivingEntity shooter, Vec3 direction, float velocity) {
      if (!(shooter.level() instanceof ServerLevel level)) return;
      Vec3 look = direction.normalize();
      Vec3 spawn = shooter.getEyePosition().add(look.scale(0.65));
      NightingaleBulletEntity bullet = new NightingaleBulletEntity(level, shooter);
      bullet.setPos(spawn.x, spawn.y - 0.05, spawn.z);
      bullet.shoot(look.x, look.y, look.z, velocity, 0.0F);
      level.addFreshEntity(bullet);
      level.sendParticles(ParticleTypes.FLASH, spawn.x, spawn.y, spawn.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.CRIT, spawn.x, spawn.y, spawn.z, 10, 0.08, 0.08, 0.08, 0.03);
      level.sendParticles(ParticleTypes.SMOKE, spawn.x, spawn.y, spawn.z, 5, 0.05, 0.05, 0.05, 0.02);
      level.playSound(null, shooter.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 1.45F);
   }
}
