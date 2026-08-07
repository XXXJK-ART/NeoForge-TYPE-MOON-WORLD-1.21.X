package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceData;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService;
import com.example.typemoonaddon.network.ImaginarySpaceMovementPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends the local vertical swim intent and predicts the same movement client-side. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class ImaginarySpaceMovementClientInput {
    private static final int HEARTBEAT_TICKS = 2;
    private static int lastSentInput;
    private static int heartbeatTicks;
    private static boolean initialized;

    private ImaginarySpaceMovementClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            initialized = false;
            heartbeatTicks = 0;
            return;
        }

        ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
        boolean active = ImaginarySpaceService.DIMENSION.equals(player.level().dimension())
                && data.active()
                && player.isAlive()
                && !player.isSpectator()
                && minecraft.screen == null;
        int verticalInput = active ? readVerticalInput(minecraft) : 0;

        if (!initialized || verticalInput != lastSentInput || ++heartbeatTicks >= HEARTBEAT_TICKS) {
            PacketDistributor.sendToServer(new ImaginarySpaceMovementPayload((byte) verticalInput));
            initialized = true;
            lastSentInput = verticalInput;
            heartbeatTicks = 0;
        }

        if (active) {
            // The sprint key is the swim-mode toggle, matching vanilla water controls.
            player.setSwimming(player.isSprinting());
            ImaginarySpaceService.applySwimmingMovement(player, verticalInput);
        } else {
            player.setSwimming(false);
        }
    }

    private static int readVerticalInput(Minecraft minecraft) {
        boolean ascend = minecraft.options.keyJump.isDown();
        boolean descend = minecraft.options.keyShift.isDown();
        if (ascend == descend) {
            return 0;
        }
        return ascend ? 1 : -1;
    }
}
