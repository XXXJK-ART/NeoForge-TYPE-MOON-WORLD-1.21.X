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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.HecatesStaffRenderer;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HecatesStaffItem extends SwordItem implements GeoItem, NoblePhantasmItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public HecatesStaffItem(Properties properties) {
      super(Tiers.DIAMOND, properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private HecatesStaffRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new HecatesStaffRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.hecates_staff.desc").withStyle(ChatFormatting.AQUA));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         if (PlayerNoblePhantasmHelper.consumeStrict(serverPlayer, 8.0)) {
            shootBasicBolt(serverPlayer);
            serverPlayer.getCooldowns().addCooldown(this, 12);
         }
      }
      return InteractionResultHolder.consume(stack);
   }

   private PlayState predicate(AnimationState<HecatesStaffItem> state) {
      state.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   private static void shootBasicBolt(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      MedeaMagicBoltEntity bolt = new MedeaMagicBoltEntity(level, player);
      bolt.setMode(MedeaMagicBoltEntity.Mode.BOLT);
      bolt.setMagicDamage(14.0F);
      Vec3 spawn = player.getEyePosition().add(player.getLookAngle().scale(0.8));
      bolt.setPos(spawn.x, spawn.y - 0.08, spawn.z);
      Vec3 look = player.getLookAngle();
      bolt.shoot(look.x, look.y, look.z, 2.7F, 0.02F);
      level.addFreshEntity(bolt);
      level.sendParticles(ParticleTypes.ENCHANT, spawn.x, spawn.y, spawn.z, 14, 0.14, 0.14, 0.14, 0.04);
      level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.65F, 1.65F);
   }
}
