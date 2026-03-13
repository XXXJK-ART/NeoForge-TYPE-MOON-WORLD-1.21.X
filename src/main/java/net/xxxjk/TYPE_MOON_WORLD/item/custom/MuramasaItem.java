package net.xxxjk.TYPE_MOON_WORLD.item.custom;

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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MuramasaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaSlashHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MuramasaItem extends SwordItem implements GeoItem, NoblePhantasmItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public MuramasaItem(Properties properties) {
      super(Tiers.NETHERITE, properties);
   }

   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private MuramasaRenderer renderer;

         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new MuramasaRenderer();
            }

            return this.renderer;
         }
      });
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<MuramasaItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("red"));
      return PlayState.CONTINUE;
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (player.isCrouching()) {
         player.startUsingItem(hand);
         return InteractionResultHolder.consume(player.getItemInHand(hand));
      } else {
         return super.use(level, player, hand);
      }
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseTicks) {
      if (livingEntity instanceof ServerPlayer player) {
         int useDuration = this.getUseDuration(stack, livingEntity) - remainingUseTicks;
         int currentCharge = useDuration;
         if (useDuration > 100) {
            currentCharge = 100;
         }

         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean isMaxCharge = currentCharge >= 100;
         if (!isMaxCharge && !(vars.player_mana >= 10.0)) {
            player.releaseUsingItem();
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         } else {
            if (!isMaxCharge) {
               vars.player_mana -= 10.0;
               vars.syncMana(player);
            }

            player.displayClientMessage(
               Component.translatable("message.typemoonworld.muramasa.charge", new Object[]{currentCharge}).withStyle(ChatFormatting.RED), true
            );
            level.playSound(
               null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.5F, 1.0F + currentCharge / 100.0F
            );
            if (level instanceof ServerLevel serverLevel) {
               double radius = 1.0 + currentCharge / 50.0;
               int particleCount = 2 + currentCharge / 10;

               for (int i = 0; i < particleCount; i++) {
                  double angle = (Math.PI * 2) * i / particleCount + level.getGameTime() * 0.1;
                  double px = player.getX() + radius * Math.cos(angle);
                  double pz = player.getZ() + radius * Math.sin(angle);
                  double py = player.getY() + level.random.nextDouble() * 2.0;
                  serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 0, 0.0, 0.05, 0.0, 1.0);
               }
            }

            if (currentCharge == 100 && useDuration == 100) {
               level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5F, 2.0F);
            }
         }
      }
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
      if (livingEntity instanceof Player player) {
         if (!level.isClientSide) {
            int useDuration = this.getUseDuration(stack, livingEntity) - timeCharged;
            int charge = useDuration;
            if (useDuration > 100) {
               charge = 100;
            }

            if (charge > 0) {
               if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                  MuramasaSlashHandler.initiate(
                     serverLevel, serverPlayer, charge, this.getMaxSlashDistance(), this.getMaxSlashWidth(), this.getMaxSlashHeight()
                  );
               }

               level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F + charge / 100.0F);
            }
         }
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player != null && player.isCrouching()) {
         return InteractionResult.PASS;
      } else {
         Level level = context.getLevel();
         if (!level.isClientSide) {
            double x = context.getClickedPos().getX();
            double y = context.getClickedPos().getY();
            double z = context.getClickedPos().getZ();
            level.explode(null, x, y, z, 1.0F, true, ExplosionInteraction.TNT);
            if (level instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.LAVA, x, y, z, 20, 0.5, 0.5, 0.5, 0.0);
               serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 20, 0.5, 0.5, 0.5, 0.0);
            }

            context.getItemInHand().hurtAndBreak(1, context.getPlayer(), LivingEntity.getSlotForHand(context.getHand()));
         }

         return InteractionResult.SUCCESS;
      }
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.hurtAndBreak(1, attacker, LivingEntity.getSlotForHand(attacker.getUsedItemHand()));
      target.igniteForSeconds(30.0F);
      Level level = attacker.level();
      if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.0, 0.0, 0.0, 0.0);
         serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.0, 0.0, 0.0, 0.0);
      }

      return true;
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      super.inventoryTick(stack, level, entity, slotId, isSelected);
      if (isSelected && entity instanceof LivingEntity livingEntity && !level.isClientSide) {
         livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20, 1, false, false));
         livingEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 0, false, false));
         livingEntity.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 20, 1, false, false));
      }
   }

   public int getMaxManaCost() {
      return 1000;
   }

   public double getManaCostPerTick() {
      return 10.0;
   }

   public int getMaxSlashDistance() {
      return 50;
   }

   public int getMaxSlashWidth() {
      return 5;
   }

   public int getMaxSlashHeight() {
      return 50;
   }
}
