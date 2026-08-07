package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.ImaginaryDisplacementAttachments;
import com.example.typemoonaddon.magic.ImaginaryDisplacementData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class ImaginaryDisplacementEvents {
    private static final int REFLECT_PARTICLE_COUNT = 32;

    public static final ResourceKey<DamageType> REFLECT_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_displacement_reflect"));

    private ImaginaryDisplacementEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }

        ImaginaryDisplacementData data = player.getData(ImaginaryDisplacementAttachments.PLAYER_STATE);
        if (data.tick()) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "message.typemoonworld.magic.imaginary_displacement.ended"), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DamageSource source = event.getSource();
        if (source == null || source.is(REFLECT_DAMAGE)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.GENERIC_KILL)
                || isEnvironmentDamage(source)) {
            return;
        }

        ImaginaryDisplacementData data = player.getData(ImaginaryDisplacementAttachments.PLAYER_STATE);
        if (!data.isActive() || !isEligibleDamage(source)) {
            return;
        }

        float incomingDamage = event.getAmount();
        if (!Float.isFinite(incomingDamage) || incomingDamage <= 0.0F) {
            return;
        }

        event.setAmount(0.0F);
        event.setCanceled(true);

        LivingEntity attacker = resolveAttacker(source);
        if (attacker != null && attacker.isAlive() && attacker != player) {
            boolean reflected = attacker.hurt(
                    player.damageSources().source(REFLECT_DAMAGE, player), incomingDamage);
            if (reflected && data.tryStartReflectEffect(player.serverLevel().getGameTime())) {
                sendReflectParticles(player);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(ImaginaryDisplacementAttachments.PLAYER_STATE).clearActive();
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            original.getData(ImaginaryDisplacementAttachments.PLAYER_STATE).clearActive();
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(ImaginaryDisplacementAttachments.PLAYER_STATE).clearActive();
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(ImaginaryDisplacementAttachments.PLAYER_STATE).clearActive();
        }
    }

    private static boolean isEligibleDamage(DamageSource source) {
        if (MagicResistanceHelper.isMagicDamage(source)) {
            return true;
        }

        Entity causing = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (causing instanceof LivingEntity living && living.isAlive()
                && (source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypeTags.IS_PROJECTILE))) {
            return true;
        }

        return direct instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity owner
                && owner.isAlive();
    }

    private static LivingEntity resolveAttacker(DamageSource source) {
        Entity causing = source.getEntity();
        if (causing instanceof LivingEntity living) {
            return living;
        }
        if (causing instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }

        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        return direct instanceof LivingEntity living ? living : null;
    }

    private static void sendReflectParticles(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(
                ParticleTypes.PORTAL,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5D,
                player.getZ(),
                REFLECT_PARTICLE_COUNT,
                0.8D,
                player.getBbHeight() * 0.5D,
                0.8D,
                0.08D
        );
    }

    private static boolean isEnvironmentDamage(DamageSource source) {
        return source.is(DamageTypes.FALL)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FREEZE)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.HOT_FLOOR)
                || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypes.CRAMMING)
                || source.is(vanillaDamageType("world_border"))
                || source.is(vanillaDamageType("starve"));
    }

    private static ResourceKey<DamageType> vanillaDamageType(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.withDefaultNamespace(id));
    }
}
