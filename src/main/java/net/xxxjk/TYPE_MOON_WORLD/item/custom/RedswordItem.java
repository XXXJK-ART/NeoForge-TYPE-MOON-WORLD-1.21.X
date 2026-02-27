
package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.UseAnim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaSlashHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class RedswordItem extends SwordItem implements GeoItem, NoblePhantasmItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RedswordItem(Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private net.xxxjk.TYPE_MOON_WORLD.client.renderer.RedswordRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new net.xxxjk.TYPE_MOON_WORLD.client.renderer.RedswordRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<RedswordItem> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("red"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isCrouching()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
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
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseTicks) {
        if (!(livingEntity instanceof ServerPlayer player)) return;
        
        int useDuration = getUseDuration(stack, livingEntity) - remainingUseTicks;
        
        // Growth: 1 point every tick (0.05s).
        // Max charge 100 reached in 100 ticks (5 seconds).
        int currentCharge = useDuration;
        if (currentCharge > 100) currentCharge = 100;
        
        // Mana Cost: 10 per point (every tick), but stop at 100%
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean isMaxCharge = currentCharge >= 100;
        
        if (isMaxCharge || vars.player_mana >= 5) {
            if (!isMaxCharge) {
                vars.player_mana -= 5;
                vars.syncMana(player);
            }
            
            // Feedback
            player.displayClientMessage(
                    Component.translatable("message.typemoonworld.redsword.charge", currentCharge)
                            .withStyle(ChatFormatting.RED),
                    true
            );
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.5f, 1.0f + (currentCharge / 100.0f));
            
            // Surrounding Flame Particles
            if (level instanceof ServerLevel serverLevel) {
                double radius = 1.0 + (currentCharge / 50.0); // Radius grows with charge
                int particleCount = 2 + (currentCharge / 10); // More particles with charge
                
                for (int i = 0; i < particleCount; i++) {
                    double angle = (2 * Math.PI * i / particleCount) + (level.getGameTime() * 0.1); // Rotating
                    double px = player.getX() + radius * Math.cos(angle);
                    double pz = player.getZ() + radius * Math.sin(angle);
                    double py = player.getY() + (level.random.nextDouble() * 2.0); // Random height around player
                    
                    serverLevel.sendParticles(ParticleTypes.FLAME, px, py, pz, 0, 0, 0.05, 0, 1.0);
                }
            }

            if (currentCharge == 100 && useDuration == 100) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5f, 2.0f);
            }
        } else {
            // Out of mana, release immediately
            player.releaseUsingItem();
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!(livingEntity instanceof Player player)) return;
        if (level.isClientSide) return;
        
        int useDuration = getUseDuration(stack, livingEntity) - timeCharged;
        int charge = useDuration; // 1 tick = 1 charge
        if (charge > 100) charge = 100;
        
        if (charge > 0) {
            // Use Handler instead of Entity
            if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                MuramasaSlashHandler.initiate(serverLevel, serverPlayer, charge, getMaxSlashDistance(), getMaxSlashWidth(), getMaxSlashHeight());
            }
            
            // SFX
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 0.5f + (charge / 100.0f));
        }
    }

    // Logic from procedures
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isCrouching()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide) {
            double x = context.getClickedPos().getX();
            double y = context.getClickedPos().getY();
            double z = context.getClickedPos().getZ();
            
            level.explode(null, x, y, z, 1, true, Level.ExplosionInteraction.TNT);
            
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LAVA, x, y, z, 20, 0.5, 0.5, 0.5, 0);
                serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 20, 0.5, 0.5, 0.5, 0);
            }
            
            context.getItemInHand().hurtAndBreak(1, context.getPlayer(), LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, LivingEntity.getSlotForHand(attacker.getUsedItemHand()));
        
        target.igniteForSeconds(30);
        Level level = attacker.level();
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
             serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 1, target.getZ(), 20, 0, 0, 0, 0);
             serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1, target.getZ(), 20, 0, 0, 0, 0);
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (isSelected && entity instanceof LivingEntity livingEntity && !level.isClientSide) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20, 1, false, false));
            // Muramasa Debuffs: Hunger (Energy Drain) and Unluck (Cursed Karma)
            livingEntity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20, 0, false, false));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 20, 1, false, false));
        }
    }

    public int getMaxManaCost() {
        return 500;
    }

    public double getManaCostPerTick() {
        return 5.0;
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
