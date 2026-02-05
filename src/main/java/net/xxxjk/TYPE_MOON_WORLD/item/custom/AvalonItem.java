package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.AvalonRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import java.util.function.Consumer;

public class AvalonItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AvalonItem(Properties properties) {
        super(properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private AvalonRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new AvalonRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            // Logic from Avalon_every_timeProcedure: Regen VI and Absorption V
            // Duration 20 ticks (1s) refreshes constantly
            livingEntity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 5));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20, 4));
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
         if (!level.isClientSide) {
             // Active effect: Give Resistance and stronger Regen for a short time
             player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2)); // Regen III for 5s
             player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0)); // Resistance I for 5s
             player.getCooldowns().addCooldown(this, 200); // 10s cooldown
         }
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<AvalonItem> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("animation.model.new"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
