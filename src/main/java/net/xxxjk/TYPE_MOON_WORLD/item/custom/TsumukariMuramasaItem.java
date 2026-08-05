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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MuramasaRenderer;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TsumukariWaveProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaSlashHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TsumukariMuramasaItem extends SwordItem implements GeoItem, NoblePhantasmItem {
   private static final double CARD_MURAMASA_TOTAL_MANA_COST = 1000.0;
   private static final int CARD_MURAMASA_MAX_CHARGE_TICKS = 30;
   private static final int SPECIAL_CHARGE_PERCENT = 60;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public TsumukariMuramasaItem(Properties properties) {
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

   private PlayState predicate(AnimationState<TsumukariMuramasaItem> event) {
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

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
      tooltipComponents.add(Component.translatable("item.typemoonworld.tsumukari_muramasa.desc").withStyle(ChatFormatting.GOLD));
      tooltipComponents.add(
         Component.translatable("item.typemoonworld.tsumukari_muramasa.warning").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD})
      );
   }

   public int getMaxManaCost() {
      return 2000;
   }

   public double getManaCostPerTick() {
      return 20.0;
   }

   public int getMaxSlashDistance() {
      return 300;
   }

   public int getMaxSlashWidth() {
      return 10;
   }

   public int getMaxSlashHeight() {
      return 100;
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if (isSelected && entity instanceof LivingEntity livingEntity && !level.isClientSide) {
         livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20, 1, false, false));
      }
   }

   public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseTicks) {
      if (livingEntity instanceof ServerPlayer player) {
         int useDuration = this.getUseDuration(stack, livingEntity) - remainingUseTicks;
         if (isMuramasaCard(player)) {
            tickCardCharge(level, player, useDuration);
            return;
         }
         int currentCharge = useDuration;
         if (useDuration > 100) {
            currentCharge = 100;
         }

         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         boolean paid = useDuration > 100
            || consumePlayerMana(player, vars, getManaCostPerTick());
         if (!paid) {
            player.releaseUsingItem();
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
         } else {
            Component chargeText = Component.translatable("message.typemoonworld.tsumukari_muramasa.charge", currentCharge)
               .withStyle(currentCharge >= SPECIAL_CHARGE_PERCENT ? ChatFormatting.DARK_RED : ChatFormatting.RED);
            player.displayClientMessage(chargeText, true);
            level.playSound(
               null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.5F, 1.0F + currentCharge / 100.0F
            );
            if (level instanceof ServerLevel serverLevel) {
               double radius = 1.0 + currentCharge / 50.0;
               int particleCount = 2 + currentCharge / 10;

               for (int i = 0; i < particleCount; i++) {
                  double angle = (Math.PI * 2) * i / particleCount + level.getGameTime() * 0.2;
                  double heightOffset = level.getGameTime() % 20L / 10.0;
                  double px = player.getX() + radius * Math.cos(angle);
                  double pz = player.getZ() + radius * Math.sin(angle);
                  double py = player.getY() + heightOffset + level.random.nextDouble() * 0.5;
                  if (currentCharge >= SPECIAL_CHARGE_PERCENT) {
                     serverLevel.sendParticles(ParticleTypes.LAVA, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
                     serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, 0.0, 0.0, 0.0, 0.05);
                  } else {
                     serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 1, 0.0, 0.05, 0.0, 0.05);
                  }
               }
            }

            if (currentCharge == 100 && useDuration == 100) {
               level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5F, 2.0F);
            }
         }
      }
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
      super.releaseUsing(stack, level, livingEntity, timeCharged);
      if (!level.isClientSide && livingEntity instanceof Player player) {
         int useDuration = this.getUseDuration(stack, livingEntity) - timeCharged;
         if (player instanceof ServerPlayer serverPlayer && isMuramasaCard(serverPlayer)) {
            releaseCardCharge(serverPlayer, level, useDuration);
            return;
         }
         int charge = useDuration;
         if (useDuration > 100) {
            charge = 100;
         }

         if (charge > 0) {
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
               int color = MagicCircuitColorHelper.ensureColor(serverPlayer);
               spawnReleaseAirwaves(serverLevel, serverPlayer, charge, color);
               MuramasaSlashHandler.initiateTsumukari(
                  serverLevel, serverPlayer, charge, this.getMaxSlashDistance(), this.getMaxSlashWidth(), this.getMaxSlashHeight()
               );
               TsumukariWaveProjectileEntity projectile = new TsumukariWaveProjectileEntity(serverLevel, serverPlayer, charge, color);
               serverLevel.addFreshEntity(projectile);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F + charge / 100.0F);
         }

         if (charge >= SPECIAL_CHARGE_PERCENT && player instanceof ServerPlayer serverPlayer && !hasDivinity(serverPlayer)) {
            level.explode(null, player.getX(), player.getY(), player.getZ(), 10.0F, true, ExplosionInteraction.TNT);
            forceTsumukariDeath(serverPlayer, level);
         }
      }
   }

   private static void tickCardCharge(Level level, ServerPlayer player, int useDuration) {
      int charge = Math.max(0, Math.min(CARD_MURAMASA_MAX_CHARGE_TICKS, useDuration));
      int percent = Math.round(charge * 100.0F / CARD_MURAMASA_MAX_CHARGE_TICKS);
      if (percent < SPECIAL_CHARGE_PERCENT) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         double costPerTick = CARD_MURAMASA_TOTAL_MANA_COST / CARD_MURAMASA_MAX_CHARGE_TICKS;
         if (!ServantCardManaService.consumeSilently(player, vars, costPerTick)) {
            player.releaseUsingItem();
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
            return;
         }
      }
      player.displayClientMessage(
         Component.translatable("message.typemoonworld.tsumukari_muramasa.charge", percent)
            .withStyle(percent >= SPECIAL_CHARGE_PERCENT ? ChatFormatting.DARK_RED : ChatFormatting.RED),
         true
      );
      level.playSound(
         null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE,
         SoundSource.PLAYERS, 0.5F, 1.0F + percent / 100.0F
      );
      if (level instanceof ServerLevel serverLevel) {
         double radius = 1.0 + percent / 50.0;
         int particleCount = 2 + percent / 10;
         for (int i = 0; i < particleCount; i++) {
            double angle = Math.PI * 2.0 * i / particleCount + level.getGameTime() * 0.2;
            double px = player.getX() + radius * Math.cos(angle);
            double pz = player.getZ() + radius * Math.sin(angle);
            double py = player.getY() + 0.6 + level.random.nextDouble() * 1.2;
            serverLevel.sendParticles(
               percent >= SPECIAL_CHARGE_PERCENT ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
               px, py, pz, 1, 0.0, 0.04, 0.0, 0.05
            );
         }
      }
      if (charge == 30 && useDuration == 30) {
         level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5F, 2.0F);
      }
   }

   private static void releaseCardCharge(ServerPlayer player, Level level, int useDuration) {
      int charge = Math.max(0, Math.min(CARD_MURAMASA_MAX_CHARGE_TICKS, useDuration));
      if (charge <= 0) {
         return;
      }
      int percent = Math.round(charge * 100.0F / CARD_MURAMASA_MAX_CHARGE_TICKS);
      boolean delayedDissolution = percent >= SPECIAL_CHARGE_PERCENT && !hasDivinity(player);

      if (level instanceof ServerLevel serverLevel) {
         int color = MagicCircuitColorHelper.ensureColor(player);
         level.playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.SENKO_MURAMASA_VOICE_TSUMUKARI.get(), SoundSource.VOICE, 1.0F, 1.0F);
         spawnReleaseAirwaves(serverLevel, player, percent, color);
         MuramasaSlashHandler.initiateTsumukari(serverLevel, player, percent, 300, 10, 100);
         serverLevel.addFreshEntity(new TsumukariWaveProjectileEntity(serverLevel, player, percent, color));
      }
      level.playSound(null, player.getX(), player.getY(), player.getZ(),
         SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F + percent / 100.0F);
      if (delayedDissolution) {
         forceTsumukariDeath(player, level);
      }
   }

   private static void forceTsumukariDeath(ServerPlayer player, Level level) {
      if (!(level instanceof ServerLevel serverLevel) || player == null || !player.isAlive()) {
         return;
      }
      serverLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0, player.getZ(),
         4, 0.6, 0.8, 0.6, 0.0);
      serverLevel.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.8, player.getZ(),
         40, 1.0, 0.8, 1.0, 0.04);
      if (!player.isCreative()) {
         player.kill();
      }
   }

   private static boolean isMuramasaCard(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars =
         player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "senko_muramasa".equals(vars.servant_card_id);
   }

   private static boolean consumePlayerMana(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, double amount) {
      if (amount <= 0.0) return true;
      if (vars.player_mana + 1.0E-6 < amount) return false;
      vars.player_mana -= amount;
      vars.syncMana(player);
      return true;
   }

   private static boolean hasDivinity(ServerPlayer player) {
      return ServantIdentityHelper.hasTrait(player, ServantTraitTag.DIVINE)
         || ServantIdentityHelper.hasTrait(player, ServantTraitTag.CELESTIAL);
   }

   private static void spawnReleaseAirwaves(ServerLevel level, ServerPlayer player, int charge, int color) {
      float maxRadius = 2.8F + charge * 0.06F;
      level.addFreshEntity(new ExpandingRingEffectEntity(level, player.getX(), player.getY() + 0.15, player.getZ(), 0.35F, maxRadius, 0.14F, 12, color, 0.72F, 0.05F));
      level.addFreshEntity(
         new ExpandingRingEffectEntity(level, player.getX(), player.getY() + 0.55, player.getZ(), 0.25F, maxRadius * 0.82F, 0.11F, 16, color, 0.54F, 0.045F)
      );
      level.addFreshEntity(
         new ExpandingRingEffectEntity(level, player.getX(), player.getY() + 0.95, player.getZ(), 0.18F, maxRadius * 0.65F, 0.09F, 20, color, 0.38F, 0.04F)
      );
   }
}
