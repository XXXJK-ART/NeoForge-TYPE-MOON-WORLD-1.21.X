package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MercurySwordRenderer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MercurySwordItem extends SwordItem implements GeoItem, NoblePhantasmItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public MercurySwordItem(Properties properties) {
      super(Tiers.NETHERITE, properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private MercurySwordRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new MercurySwordRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<MercurySwordItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.mercury_sword.desc").withStyle(ChatFormatting.AQUA));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         boolean advanced = player.isShiftKeyDown();
         double cost = advanced ? 100.0 : 50.0;
         TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.player_mana < cost) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
            return InteractionResultHolder.fail(stack);
         }
         vars.player_mana -= cost;
         vars.syncMana(serverPlayer);
         releaseMercuryBeam(serverPlayer, advanced ? 10.0 : 5.0, advanced ? 50.0F : 25.0F);
         serverPlayer.getCooldowns().addCooldown(this, advanced ? 32 : 18);
      }
      return InteractionResultHolder.consume(stack);
   }

   private static void releaseMercuryBeam(ServerPlayer player, double range, float damage) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.6));
      Vec3 dir = player.getLookAngle().normalize();
      Vec3 end = start.add(dir.scale(range));
      level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.35F);
      int steps = Math.max(6, (int)(range * 3.0));
      for (int i = 0; i <= steps; i++) {
         Vec3 pos = start.lerp(end, i / (double)steps);
         level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 2, 0.035, 0.035, 0.035, 0.0);
         if (i % 2 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.01);
         }
      }
      AABB box = new AABB(start, end).inflate(0.9);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && !player.isAlliedTo(e) && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         double along = center.subtract(start).dot(dir);
         if (along < -0.3 || along > range + 0.8) {
            continue;
         }
         Vec3 closest = start.add(dir.scale(Math.max(0.0, Math.min(range, along))));
         if (center.distanceToSqr(closest) <= 1.4) {
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().magic(), damage);
            target.invulnerableTime = 0;
         }
      }
   }
}
