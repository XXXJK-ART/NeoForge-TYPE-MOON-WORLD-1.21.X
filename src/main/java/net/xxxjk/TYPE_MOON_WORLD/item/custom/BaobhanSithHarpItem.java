package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.BaobhanSithHarpRenderer;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashAimHelper;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class BaobhanSithHarpItem extends net.minecraft.world.item.Item implements GeoItem {
   private static final double PROJECTILE_SPEED = 3.2;
   private static final int FULL_CHARGE_TICKS = 20;
   private static final DustParticleOptions BLOOD_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.06F, 0.10F), 1.15F);
   private static final DustParticleOptions CURSE_DUST = new DustParticleOptions(new Vector3f(0.08F, 0.02F, 0.10F), 1.0F);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public BaobhanSithHarpItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResultHolder.pass(stack);
      }
      player.startUsingItem(hand);
      return InteractionResultHolder.consume(stack);
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
      int chargeTicks = this.getUseDuration(stack, living) - timeLeft;
      if (chargeTicks < FULL_CHARGE_TICKS) {
         return;
      }
      if (level instanceof ServerLevel serverLevel) {
         int chargeSeconds = Math.max(1, Math.min(5, chargeTicks / FULL_CHARGE_TICKS));
         fireCurseShot(serverLevel, living, PROJECTILE_SPEED, 1.0F, chargeSeconds);
      }
   }

   public static void fireCurseShot(ServerLevel level, LivingEntity shooter, double speed, float visualScale) {
      fireCurseShot(level, shooter, speed, visualScale, 1);
   }

   public static void fireCurseShot(ServerLevel level, LivingEntity shooter, double speed, float visualScale, int chargeSeconds) {
      Vec3 look = shooter.getLookAngle().normalize();
      Vec3 start = shooter.getEyePosition().add(look.scale(0.65));
      Vec3 direction = ArashAimHelper.autoAimDirection(shooter, start, look, speed);
      GanderProjectileEntity projectile = new GanderProjectileEntity(level, shooter);
      projectile.setPos(start.x, start.y - 0.12, start.z);
      projectile.setDeltaMovement(direction.scale(speed));
      projectile.setMagicSource("baobhan_sith_curse", 70.0);
      projectile.setChargeSeconds(chargeSeconds);
      projectile.setVisualScale(visualScale);
      level.addFreshEntity(projectile);
      spawnHarpMuzzleFx(level, start, direction);
      level.sendParticles(BLOOD_DUST, start.x, start.y, start.z, 10, 0.12, 0.12, 0.12, 0.0);
      level.sendParticles(CURSE_DUST, start.x, start.y, start.z, 8, 0.10, 0.10, 0.10, 0.0);
      level.playSound(null, shooter.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.85F, 1.55F);
   }

   private static void spawnHarpMuzzleFx(ServerLevel level, Vec3 start, Vec3 direction) {
      Vec3 forward = direction.normalize();
      Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
      if (side.lengthSqr() < 1.0E-5) {
         side = new Vec3(1.0, 0.0, 0.0);
      } else {
         side = side.normalize();
      }
      Vec3 up = side.cross(forward).normalize();
      for (int i = 0; i < 16; i++) {
         double angle = i * Math.PI * 2.0 / 16.0;
         Vec3 petal = start.add(side.scale(Math.cos(angle) * 0.24)).add(up.scale(Math.sin(angle) * 0.24));
         level.sendParticles(BLOOD_DUST, petal.x, petal.y, petal.z, 1, 0.015, 0.015, 0.015, 0.0);
      }
      for (int i = 0; i < 6; i++) {
         Vec3 trace = start.add(forward.scale(i * 0.22));
         level.sendParticles(CURSE_DUST, trace.x, trace.y, trace.z, 1, 0.035, 0.035, 0.035, 0.0);
      }
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private BaobhanSithHarpRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new BaobhanSithHarpRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
