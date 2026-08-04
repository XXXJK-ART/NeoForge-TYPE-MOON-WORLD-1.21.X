package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RubyStaffRenderer;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RubyStaffItem extends SwordItem implements GeoItem {
   public static final int CHARGE_TICKS = 100;
   public static final int COOLDOWN_TICKS = 100;
   public static final float FIRE_DAMAGE = 30.0F;
   public static final float SHIELD_HP = 500.0F;
   private static final String TAG_MODE = "RubyStaffUseMode";
   private static final String TAG_SHIELD_ACTIVE = "RubyStaffShieldActive";
   private static final String TAG_SHIELD_HP = "RubyStaffShieldHp";
   private static final int MODE_FIRE = 0;
   private static final int MODE_SHIELD = 1;
   private static final double FIRE_RANGE = 8.0;
   private static final double FIRE_WIDTH_PER_BLOCK = 0.35;
   private static final DustParticleOptions FIRE_ORANGE = new DustParticleOptions(new Vector3f(1.0F, 0.28F, 0.03F), 1.35F);
   private static final DustParticleOptions FIRE_GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.10F), 1.2F);
   private static final DustParticleOptions SHIELD_BLUE = new DustParticleOptions(new Vector3f(0.50F, 0.86F, 1.0F), 1.25F);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public RubyStaffItem(Properties properties) {
      super(Tiers.DIAMOND, properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private RubyStaffRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new RubyStaffRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<RubyStaffItem> state) {
      state.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.ruby_staff.desc").withStyle(ChatFormatting.RED));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player.getCooldowns().isOnCooldown(this)) {
         return InteractionResultHolder.fail(stack);
      }
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         beginUse(serverPlayer, player.isCrouching() ? MODE_SHIELD : MODE_FIRE);
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
   public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
      if (!(living instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
         return;
      }
      int useTicks = this.getUseDuration(stack, living) - remainingUseDuration;
      if (useTicks % 5 == 0 || useTicks == CHARGE_TICKS) {
         spawnFireArray(serverLevel, player, Math.min(1.0F, useTicks / (float)CHARGE_TICKS), mode(player) == MODE_SHIELD);
      }
      if (mode(player) == MODE_SHIELD && useTicks >= CHARGE_TICKS) {
         activateShield(player, serverLevel);
      }
   }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
      if (!(living instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
         return;
      }
      int useTicks = this.getUseDuration(stack, living) - timeLeft;
      int mode = mode(player);
      boolean charged = useTicks >= CHARGE_TICKS;
      if (mode == MODE_FIRE && charged) {
         releaseFireCone(serverLevel, player);
         player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
      } else if (mode == MODE_SHIELD && charged) {
         player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
      }
      clearUseState(player);
   }

   public static boolean tryAbsorbShield(ServerPlayer player, LivingIncomingDamageEvent event) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_SHIELD_ACTIVE) || event.getAmount() <= 0.0F || !isHoldingActiveShield(player)) {
         return false;
      }
      if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      float hp = data.getFloat(TAG_SHIELD_HP);
      if (hp <= 0.0F) {
         clearUseState(player);
         return false;
      }
      float absorbed = Math.min(hp, event.getAmount());
      float remainingDamage = Math.max(0.0F, event.getAmount() - absorbed);
      float remainingShield = hp - absorbed;
      event.setAmount(remainingDamage);
      data.putFloat(TAG_SHIELD_HP, remainingShield);
      spawnShieldHit(player, remainingShield);
      if (remainingShield <= 0.0F) {
         clearUseState(player);
         player.stopUsingItem();
         player.getCooldowns().addCooldown(ModItems.RUBY_STAFF.get(), COOLDOWN_TICKS);
      }
      if (remainingDamage <= 0.0F) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return true;
      }
      return false;
   }

   public static void tickActiveShield(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_SHIELD_ACTIVE)) {
         return;
      }
      if (!player.isAlive() || !isHoldingActiveShield(player) || player.hasEffect(ModMobEffects.PETRIFIED)) {
         clearUseState(player);
         return;
      }
      if (player.level() instanceof ServerLevel level && player.tickCount % 4 == 0) {
         spawnShieldArray(level, player, data.getFloat(TAG_SHIELD_HP) / SHIELD_HP);
      }
   }

   public static boolean isRubyStaff(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ModItems.RUBY_STAFF.get());
   }

   private static void beginUse(ServerPlayer player, int mode) {
      CompoundTag data = player.getPersistentData();
      clearUseState(player);
      data.putInt(TAG_MODE, mode);
      if (player.level() instanceof ServerLevel level) {
         level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.65F, 0.95F);
      }
   }

   private static int mode(ServerPlayer player) {
      return player.getPersistentData().getInt(TAG_MODE);
   }

   private static void activateShield(ServerPlayer player, ServerLevel level) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_SHIELD_ACTIVE)) {
         data.putBoolean(TAG_SHIELD_ACTIVE, true);
         data.putFloat(TAG_SHIELD_HP, SHIELD_HP);
         level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.35F);
         spawnShieldArray(level, player, 1.0F);
      }
   }

   private static void clearUseState(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(TAG_MODE);
      data.remove(TAG_SHIELD_ACTIVE);
      data.remove(TAG_SHIELD_HP);
   }

   private static boolean isHoldingActiveShield(ServerPlayer player) {
      return player.isUsingItem() && isRubyStaff(player.getUseItem());
   }

   private static void releaseFireCone(ServerLevel level, ServerPlayer player) {
      Vec3 dir = player.getLookAngle().normalize();
      Vec3 start = player.getEyePosition().add(dir.scale(0.7));
      Set<Integer> damaged = new HashSet<>();
      level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.05F, 0.78F);
      level.playSound(null, player.blockPosition(), SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 0.9F, 1.1F);
      spawnFireArray(level, player, 1.0F, false);
      for (int i = 0; i <= 16; i++) {
         double along = FIRE_RANGE * i / 16.0;
         Vec3 center = start.add(dir.scale(along));
         double spread = 0.18 + along * FIRE_WIDTH_PER_BLOCK;
         level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 12, spread, spread * 0.45, spread, 0.08);
         level.sendParticles(FIRE_ORANGE, center.x, center.y, center.z, 8, spread * 0.8, spread * 0.35, spread * 0.8, 0.015);
         if (i % 3 == 0) {
            level.sendParticles(ParticleTypes.LAVA, center.x, center.y - 0.15, center.z, 2, spread * 0.35, 0.08, spread * 0.35, 0.0);
         }
      }
      AABB area = player.getBoundingBox().inflate(FIRE_RANGE, 3.0, FIRE_RANGE).move(dir.scale(FIRE_RANGE * 0.45));
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> isValidFireTarget(player, e))) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         Vec3 toTarget = center.subtract(start);
         double along = toTarget.dot(dir);
         if (along < -0.2 || along > FIRE_RANGE + 0.8) {
            continue;
         }
         double sideSqr = Math.max(0.0, toTarget.lengthSqr() - along * along);
         double allowed = 0.9 + along * FIRE_WIDTH_PER_BLOCK;
         if (sideSqr <= allowed * allowed && damaged.add(target.getId())) {
            target.hurt(player.damageSources().magic(), FIRE_DAMAGE);
            target.igniteForSeconds(4.0F);
            Vec3 push = dir.scale(0.32).add(0.0, 0.08, 0.0);
            target.push(push.x, push.y, push.z);
            target.hurtMarked = true;
         }
      }
   }

   private static boolean isValidFireTarget(ServerPlayer player, LivingEntity entity) {
      return entity != null && entity.isAlive() && entity != player && !player.isAlliedTo(entity)
         && !entity.isAlliedTo(player) && !EntityUtils.isImmunePlayerTarget(entity);
   }

   private static void spawnFireArray(ServerLevel level, LivingEntity caster, float progress, boolean shieldMode) {
      Vec3 forward = caster.getLookAngle().normalize();
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(up);
      if (right.lengthSqr() < 1.0E-4) {
         right = new Vec3(1.0, 0.0, 0.0);
      }
      right = right.normalize();
      Vec3 center = caster.getEyePosition().add(forward.scale(0.65)).subtract(0.0, 0.18, 0.0);
      float radius = 0.85F + progress * 1.15F;
      drawVerticalRing(level, center, right, up, radius, FIRE_ORANGE, 48);
      if (progress > 0.65F) {
         drawVerticalRing(level, center, right, up, radius * 0.68F, shieldMode ? SHIELD_BLUE : FIRE_GOLD, 32);
         drawDiamond(level, center, right, up, radius * 0.88F, shieldMode ? SHIELD_BLUE : ParticleTypes.END_ROD);
      }
      if (progress >= 1.0F) {
         level.sendParticles(shieldMode ? SHIELD_BLUE : ParticleTypes.FLAME, center.x, center.y, center.z, 24, 0.35, 0.35, 0.35, shieldMode ? 0.015 : 0.08);
      }
   }

   private static void spawnShieldArray(ServerLevel level, ServerPlayer player, float strength) {
      Vec3 forward = player.getLookAngle().normalize();
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 right = forward.cross(up);
      if (right.lengthSqr() < 1.0E-4) {
         right = new Vec3(1.0, 0.0, 0.0);
      }
      right = right.normalize();
      Vec3 center = player.getEyePosition().add(forward.scale(1.05)).subtract(0.0, 0.18, 0.0);
      float radius = 1.65F;
      drawVerticalRing(level, center, right, up, radius, SHIELD_BLUE, 48);
      drawVerticalRing(level, center, right, up, radius * 0.72F, FIRE_GOLD, 36);
      drawDiamond(level, center, right, up, radius * 0.92F, SHIELD_BLUE);
      if (strength <= 0.35F) {
         level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 5, 0.25, 0.25, 0.25, 0.01);
      }
   }

   private static void spawnShieldHit(ServerPlayer player, float remainingShield) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 center = player.getEyePosition().add(player.getLookAngle().normalize().scale(1.0)).subtract(0.0, 0.18, 0.0);
      level.sendParticles(SHIELD_BLUE, center.x, center.y, center.z, 28, 0.7, 0.7, 0.7, 0.05);
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 12, 0.45, 0.45, 0.45, 0.04);
      level.playSound(null, player.blockPosition(), remainingShield > 0.0F ? SoundEvents.SHIELD_BLOCK : SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9F, remainingShield > 0.0F ? 1.2F : 0.7F);
   }

   private static void drawVerticalRing(ServerLevel level, Vec3 center, Vec3 right, Vec3 up, float radius, ParticleOptions particle, int points) {
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         Vec3 point = center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
         level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static void drawDiamond(ServerLevel level, Vec3 center, Vec3 right, Vec3 up, float radius, ParticleOptions particle) {
      Vec3[] corners = new Vec3[]{
         center.add(up.scale(radius)),
         center.add(right.scale(radius)),
         center.add(up.scale(-radius)),
         center.add(right.scale(-radius))
      };
      for (int i = 0; i < corners.length; i++) {
         Vec3 from = corners[i];
         Vec3 to = corners[(i + 1) % corners.length];
         for (int step = 0; step <= 10; step++) {
            Vec3 point = from.lerp(to, step / 10.0);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }
}
