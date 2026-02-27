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
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class AvalonItem extends Item implements GeoItem, NoblePhantasmItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final long TICKS_FOR_SWORD_ATTRIBUTE = 24000; // 1 Game Day

    public AvalonItem(Properties properties) {
        super(properties);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private net.xxxjk.TYPE_MOON_WORLD.client.renderer.AvalonRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new net.xxxjk.TYPE_MOON_WORLD.client.renderer.AvalonRenderer();
                return this.renderer;
            }
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            // 1. Handle Sword Attribute Unlock logic
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (!vars.player_magic_attributes_sword) {
                CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = customData.copyTag();
                long time = tag.getLong("binding_time");
                
                // Increment time
                time++;
                tag.putLong("binding_time", time);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                
                if (time >= TICKS_FOR_SWORD_ATTRIBUTE) {
                    vars.player_magic_attributes_sword = true;
                    vars.syncPlayerVariables(player);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }

            // 2. Base Functionality (Requires Saber's Mana)
            if (hasSaberMana(player)) {
                // Logic from Avalon_every_timeProcedure: Regen VI and Absorption V
                // Duration 20 ticks (1s) refreshes constantly
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 5, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20, 4, false, false));
            }
        }
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
         if (!level.isClientSide) {
             if (hasSaberMana(player)) {
                 // Active effect: Give Resistance and stronger Regen for a short time
                 player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2)); // Regen III for 5s
                 player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0)); // Resistance I for 5s
                 player.getCooldowns().addCooldown(this, 200); // 10s cooldown
                 return InteractionResultHolder.success(player.getItemInHand(usedHand));
             } else {
                 player.displayClientMessage(Component.translatable("message.typemoonworld.avalon.no_saber_mana"), true);
                 return InteractionResultHolder.fail(player.getItemInHand(usedHand));
             }
         }
        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
    }

    private boolean hasSaberMana(Player player) {
        // TODO: Implement Saber's Mana check when available.
        // Currently disabled as requested by user ("currently not implemented in mod").
        // Return false to block usage until implemented.
        return false;
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.typemoonworld.avalon.desc").withStyle(net.minecraft.ChatFormatting.GOLD));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
