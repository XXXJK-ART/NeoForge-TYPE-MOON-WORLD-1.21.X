package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoyalCannonProjectileEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Caster Gilgamesh's slate focus. It never consumes player mana. */
public final class GilgameshSlateItem extends Item implements GeoItem {
   public static final int CROUCH_SHOT_COUNT = 5;
   public static final float MIN_SHOT_DAMAGE = 5.0F;
   public static final float MAX_SHOT_DAMAGE = 10.0F;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public GilgameshSlateItem(Properties properties) {
      super(properties.stacksTo(1));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
      if (level instanceof ServerLevel serverLevel) {
         Vec3 look = player.getLookAngle().normalize();
         int shotCount = player.isCrouching() ? CROUCH_SHOT_COUNT : 1;
         for (int i = 0; i < shotCount; i++) {
            Vec3 direction = look;
            if (shotCount > 1) {
               double spreadX = (player.getRandom().nextDouble() - 0.5) * 0.16;
               double spreadY = (player.getRandom().nextDouble() - 0.5) * 0.10;
               double spreadZ = (player.getRandom().nextDouble() - 0.5) * 0.16;
               direction = look.add(spreadX, spreadY, spreadZ).normalize();
            }
            Vec3 start = player.getEyePosition().add(direction.scale(0.7));
            float damage = MIN_SHOT_DAMAGE
               + player.getRandom().nextInt((int)(MAX_SHOT_DAMAGE - MIN_SHOT_DAMAGE) + 1);
            RoyalCannonProjectileEntity shot = new RoyalCannonProjectileEntity(
               serverLevel, player, start, direction, damage);
            serverLevel.addFreshEntity(shot);
            serverLevel.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z,
               4, 0.08, 0.08, 0.08, 0.04);
         }
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
   }

   @Override
   public void createGeoRenderer(java.util.function.Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private net.xxxjk.TYPE_MOON_WORLD.client.renderer.GilgameshSlateRenderer renderer;
         @Override
         public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (renderer == null) renderer = new net.xxxjk.TYPE_MOON_WORLD.client.renderer.GilgameshSlateRenderer();
            return renderer;
         }
      });
   }

   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
