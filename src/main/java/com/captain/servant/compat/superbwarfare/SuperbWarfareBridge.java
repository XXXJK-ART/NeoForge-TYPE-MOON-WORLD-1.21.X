package com.captain.servant.compat.superbwarfare;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import java.util.Set;

interface SuperbWarfareBridge {
    boolean isGun(ItemStack stack);

    void initializeOwnerizedGun(ItemStack stack);

    int beginVehicleOwnerization(ServerPlayer player);

    boolean summonA10(ServerPlayer player, boolean npcVariant);

    boolean hasActiveSummonedA10(ServerPlayer player);

    int countOwnedVehicles(ServerPlayer player);

    boolean forceTypeMoonDamage(Entity entity, DamageSource source, float amount);

    void damageVehiclesInBox(ServerLevel level, AABB box, Entity owner, DamageSource source, float amount);

    void damageVehiclesInAnnulus(
            ServerLevel level,
            Vec3 center,
            double outerRadius,
            double innerRadius,
            Entity owner,
            DamageSource source,
            float amount,
            Set<Integer> damagedEntities
    );

    void damageVehiclesInForwardArc(
            ServerPlayer player,
            Vec3 direction,
            double range,
            float amount
    );

    void damageVehiclesInBeam(
            ServerLevel level,
            Entity beam,
            LivingEntity owner,
            Vec3 start,
            Vec3 end,
            double width,
            double height,
            float amount
    );

    boolean shouldPrioritizeA10(Entity entity);

    boolean tryDeployNpcA10(LancelotBerserkerEntity npc, LivingEntity target);

    boolean tryCounterNoblePhantasm(LancelotBerserkerEntity npc, ServantEntity attacker, LivingEntity target);
}
