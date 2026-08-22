package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.worm.WormCaptureService;
import com.example.typemoonaddon.worm.WormStackData;
import com.example.typemoonaddon.magic.WormMagicIntegration;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class WormEvents {
    private WormEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!player.getMainHandItem().isEmpty() || event.getHand() != InteractionHand.MAIN_HAND || !canCapture(player)) {
            return;
        }
        if (WormCaptureService.canCapture(player, target)) {
            var stack = WormCaptureService.capture(player, target);
            if (!stack.isEmpty()) {
                if (!player.addItem(stack)) {
                    player.drop(stack, false);
                }
                event.setCancellationResult(InteractionResult.CONSUME);
                event.setCanceled(true);
            }
        } else if (WormCaptureService.typeOf(target) != null) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.worm.capture_need_control"), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (!WormMagicIntegration.isKnown(player, WormMagicIntegration.WORM_MAGIC)
                    && !WormMagicIntegration.isKnown(player, WormMagicIntegration.WORM_CONTROL)) {
                return;
            }
            if (player.tickCount % 20 == 0) {
                bindOwnedWorms(player);
            }
        }
    }

    private static boolean canCapture(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        return WormMagicIntegration.isKnown(player, WormMagicIntegration.WORM_MAGIC)
                || WormMagicIntegration.isKnown(player, WormMagicIntegration.WORM_CONTROL)
                || vars.hasLearnedSelfMagic("worm_magic")
                || vars.hasLearnedSelfMagic("worm_control")
                || vars.hasLearnedSelfMagic("binding_magic")
                || vars.hasLearnedSelfMagic("suggestion_magic");
    }

    private static void bindOwnedWorms(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (vars.magicCrestInventory == null) {
            return;
        }
        for (int slot = 0; slot < vars.magicCrestInventory.getSlots(); slot++) {
            var stack = vars.magicCrestInventory.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.is(AddonItems.WORM.get())) {
                WormStackData.set(stack, WormStackData.type(stack), WormStackData.gu(stack), player.getUUID());
            }
        }
    }
}
