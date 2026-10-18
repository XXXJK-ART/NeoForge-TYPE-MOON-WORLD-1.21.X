package com.captain.servant.compat.superbwarfare;

import com.captain.servant.TypeMoonServant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import java.util.Set;

public final class SuperbWarfareLoader {
    public static final String VEHICLE_TAG = "FateSupportKnightVehicle";
    public static final String VEHICLE_OWNER_TAG = "FateSupportKnightVehicleOwner";
    public static final String VEHICLE_A10_TAG = "FateSupportKnightVehicleA10";
    public static final String VEHICLE_ORIGINAL_HEALTH_TAG = "FateSupportKnightVehicleOriginalHealth";
    private static final SuperbWarfareBridge BRIDGE = loadBridge();

    private SuperbWarfareLoader() {
    }

    public static void initialize() {
        // Accessing the bridge triggers optional integration registration.
    }

    private static SuperbWarfareBridge loadBridge() {
        if (!ModList.get().isLoaded("superbwarfare")) {
            return null;
        }
        try {
            Class<?> type = Class.forName(
                    "com.captain.servant.compat.superbwarfare.SuperbWarfareIntegration",
                    true,
                    SuperbWarfareLoader.class.getClassLoader()
            );
            SuperbWarfareBridge bridge = (SuperbWarfareBridge) type.getConstructor().newInstance();
            NeoForge.EVENT_BUS.register(bridge);
            TypeMoonServant.LOGGER.info("Superb Warfare integration enabled");
            return bridge;
        } catch (ReflectiveOperationException | LinkageError exception) {
            TypeMoonServant.LOGGER.error("Failed to initialize optional Superb Warfare integration", exception);
            return null;
        }
    }

    public static boolean isAvailable() {
        return BRIDGE != null;
    }

    public static boolean isGun(ItemStack stack) {
        return BRIDGE != null && BRIDGE.isGun(stack);
    }

    public static void initializeOwnerizedGun(ItemStack stack) {
        if (BRIDGE != null) {
            BRIDGE.initializeOwnerizedGun(stack);
        }
    }

    public static int beginVehicleOwnerization(ServerPlayer player) {
        return BRIDGE == null ? 0 : BRIDGE.beginVehicleOwnerization(player);
    }

    public static boolean summonA10(ServerPlayer player, boolean npcVariant) {
        return BRIDGE != null && BRIDGE.summonA10(player, npcVariant);
    }

    public static boolean hasActiveSummonedA10(ServerPlayer player) {
        return BRIDGE != null && BRIDGE.hasActiveSummonedA10(player);
    }

    public static int countOwnedVehicles(ServerPlayer player) {
        return BRIDGE == null ? 0 : BRIDGE.countOwnedVehicles(player);
    }

    public static boolean forceTypeMoonDamage(Entity entity, DamageSource source, float amount) {
        return BRIDGE != null && BRIDGE.forceTypeMoonDamage(entity, source, amount);
    }

    public static void damageVehiclesInBox(
            ServerLevel level, AABB box, Entity owner, DamageSource source, float amount
    ) {
        if (BRIDGE != null) {
            BRIDGE.damageVehiclesInBox(level, box, owner, source, amount);
        }
    }

    public static void damageVehiclesInAnnulus(
            ServerLevel level,
            Vec3 center,
            double outerRadius,
            double innerRadius,
            Entity owner,
            DamageSource source,
            float amount,
            Set<Integer> damagedEntities
    ) {
        if (BRIDGE != null) {
            BRIDGE.damageVehiclesInAnnulus(
                    level, center, outerRadius, innerRadius, owner, source, amount, damagedEntities
            );
        }
    }

    public static void damageVehiclesInForwardArc(
            ServerPlayer player, Vec3 direction, double range, float amount
    ) {
        if (BRIDGE != null) {
            BRIDGE.damageVehiclesInForwardArc(player, direction, range, amount);
        }
    }

    public static void damageVehiclesInBeam(
            ServerLevel level,
            Entity beam,
            LivingEntity owner,
            Vec3 start,
            Vec3 end,
            double width,
            double height,
            float amount
    ) {
        if (BRIDGE != null) {
            BRIDGE.damageVehiclesInBeam(level, beam, owner, start, end, width, height, amount);
        }
    }

    public static boolean shouldPrioritizeA10(Entity entity) {
        return BRIDGE != null && BRIDGE.shouldPrioritizeA10(entity);
    }

    public static boolean tryDeployNpcA10(LancelotBerserkerEntity npc, LivingEntity target) {
        return BRIDGE != null && BRIDGE.tryDeployNpcA10(npc, target);
    }

    public static boolean tryCounterNoblePhantasm(
            LancelotBerserkerEntity npc, ServantEntity attacker, LivingEntity target
    ) {
        return BRIDGE != null && BRIDGE.tryCounterNoblePhantasm(npc, attacker, target);
    }
}
