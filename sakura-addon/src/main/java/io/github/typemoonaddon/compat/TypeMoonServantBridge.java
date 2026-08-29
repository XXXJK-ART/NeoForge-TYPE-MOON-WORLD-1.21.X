package io.github.typemoonaddon.compat;

import io.github.typemoonaddon.TypeMoonAddon;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Public-API and data-tag based servant classification. */
public final class TypeMoonServantBridge {
    private static final TagKey<EntityType<?>> SERVANTS = TagKey.create(
        Registries.ENTITY_TYPE,
        TypeMoonAddon.id("servants")
    );

    public static void verifyCompatibility() {
        if (!TypeMoonWorldApi.isCompatible(TypeMoonWorldApi.API_VERSION)) {
            throw new IllegalStateException("Incompatible Type Moon World API version " + TypeMoonWorldApi.API_VERSION);
        }
    }

    public static boolean isServantLike(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return TypeMoonWorldApi.servantForm(player).transformed();
        }
        if (entity == null) {
            return false;
        }
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return entity.getType().is(SERVANTS) || "typemoonworld".equals(typeId.getNamespace());
    }

    public static boolean isServantOrCardUser(LivingEntity entity) {
        return isServantLike(entity);
    }

    public static boolean isActualServant(LivingEntity entity) {
        return entity != null && !(entity instanceof ServerPlayer) && isServantLike(entity);
    }

    @Nullable
    public static String servantId(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            ResourceLocation id = TypeMoonWorldApi.servantForm(player).servantId();
            return id == null ? null : id.getPath().toLowerCase(Locale.ROOT);
        }
        if (!isServantLike(entity)) {
            return null;
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().toLowerCase(Locale.ROOT);
    }

    private TypeMoonServantBridge() {
    }
}
