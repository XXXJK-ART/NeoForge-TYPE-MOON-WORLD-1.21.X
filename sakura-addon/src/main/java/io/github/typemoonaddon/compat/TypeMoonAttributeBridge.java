package io.github.typemoonaddon.compat;

import io.github.typemoonaddon.TypeMoonAddon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import io.github.typemoonaddon.registry.ModAttachments;
import net.xxxjk.typemoonworld.api.MagicAttributes;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Public-API-only boundary for Type Moon World attributes and mana. */
public final class TypeMoonAttributeBridge {
    public static void verifyCompatibility() {
        if (!TypeMoonWorldApi.isCompatible(TypeMoonWorldApi.API_VERSION)) {
            throw new IllegalStateException("Incompatible Type Moon World API version " + TypeMoonWorldApi.API_VERSION);
        }
    }

    public static boolean grantImaginaryNumber(ServerPlayer player) {
        if (player == null) return false;
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        data.unlock();
        player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        return true;
    }

    public static boolean grantWater(ServerPlayer player) {
        return player != null;
    }

    public static boolean grantGrailWormPower(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        var mana = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(player);
        mana.add(Math.max(0.0D, mana.maximum() - mana.current()));
        return true;
    }

    public static boolean hasNativeImaginaryNumber(ServerPlayer player) {
        return player != null && TypeMoonWorldApi.magicAttributes(player).has(MagicAttributes.IMAGINARY_NUMBER);
    }

    public static void register() {
        TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).registerAttributeProvider(new net.xxxjk.typemoonworld.api.MagicAttributeProvider() {
            @Override
            public boolean has(LivingEntity entity, ResourceLocation attribute) {
                return entity != null && MagicAttributes.IMAGINARY_NUMBER.equals(attribute)
                    && entity.getData(ModAttachments.IMAGINARY_SPACE.get()).imaginaryAffinity();
            }

            @Override
            public Set<ResourceLocation> attributes(LivingEntity entity) {
                return has(entity, MagicAttributes.IMAGINARY_NUMBER) ? Set.of(MagicAttributes.IMAGINARY_NUMBER) : Set.of();
            }
        });
    }

    private TypeMoonAttributeBridge() {
    }
}
