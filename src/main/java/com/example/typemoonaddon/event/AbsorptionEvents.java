package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.AbsorptionState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import org.joml.Vector3f;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class AbsorptionEvents {
    private static final DustParticleOptions ABSORPTION_DUST =
            new DustParticleOptions(new Vector3f(0.15F, 0.55F, 1.0F), 1.1F);

    private AbsorptionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !AbsorptionState.isActive(player)
                || !MagicResistanceHelper.isMagicDamage(event.getSource())) {
            return;
        }

        float damage = event.getAmount();
        if (damage > 0.0F) {
            TypeMoonWorldModVariables.PlayerVariables vars =
                    player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            vars.player_mana = Math.min(vars.player_max_mana, vars.player_mana + damage);
            vars.syncMana(player);
        }

        event.setAmount(0.0F);
        event.setCanceled(true);
        if (damage > 0.0F) {
            sendAbsorptionParticles(player);
        }
    }

    private static void sendAbsorptionParticles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(
                ABSORPTION_DUST,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5,
                player.getZ(),
                18,
                0.55,
                player.getBbHeight() * 0.5,
                0.55,
                0.035
        );
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        AbsorptionState.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AbsorptionState.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AbsorptionState.clear(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        AbsorptionState.clear(event.getOriginal());
        AbsorptionState.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        AbsorptionState.clear(event.getEntity());
    }
}
