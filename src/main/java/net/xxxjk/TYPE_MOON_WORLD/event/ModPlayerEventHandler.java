package net.xxxjk.TYPE_MOON_WORLD.event;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class ModPlayerEventHandler {

    private static boolean isModItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && TYPE_MOON_WORLD.MOD_ID.equals(id.getNamespace());
    }

    private static boolean checkMagus(Player player) {
        if (player.level().isClientSide()) return true; // Client side check handled by server sync or just allowed for now
        // But interactions are server authoritative mostly.
        // Let's check server side.
        
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.is_magus) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_magus_interaction"), true);
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        
        if (isModItem(event.getItemStack())) {
            if (!checkMagus(event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        
        if (isModItem(event.getItemStack())) {
            if (!checkMagus(event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }
    
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        
        if (isModItem(event.getItemStack())) {
            if (!checkMagus(event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // Usually left click is attack or break.
        // Some items have left click actions?
        // Let's restrict it if it's a mod item, just in case (e.g. magical tools).
        if (event.getLevel().isClientSide()) return;
        
        if (isModItem(event.getItemStack())) {
            if (!checkMagus(event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }
}
