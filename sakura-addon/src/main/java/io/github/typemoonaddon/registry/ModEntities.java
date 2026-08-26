package io.github.typemoonaddon.registry;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import io.github.typemoonaddon.entity.ShadowPiercingRhoAiasEntity;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.shadowlogic.entity.ShadowArtRibbonEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(
        Registries.ENTITY_TYPE,
        TypeMoonAddon.MOD_ID
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ShadowFamiliarEntity>> SHADOW_FAMILIAR = ENTITY_TYPES.register(
        "shadow_familiar",
        () -> EntityType.Builder.of(ShadowFamiliarEntity::new, MobCategory.CREATURE)
            .sized(1.0F, 1.85F)
            .eyeHeight(1.30F)
            .clientTrackingRange(8)
            .build(TypeMoonAddon.id("shadow_familiar").toString())
    );

    public static final DeferredHolder<EntityType<?>, EntityType<BlackShadowEntity>> BLACK_SHADOW = ENTITY_TYPES.register(
        "black_shadow",
        () -> EntityType.Builder.of(BlackShadowEntity::new, MobCategory.MONSTER)
            .sized(0.60F, 1.80F)
            .eyeHeight(1.62F)
            .clientTrackingRange(8)
            .build(TypeMoonAddon.id("black_shadow").toString())
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ShadowArtRibbonEntity>> SHADOW_ART_RIBBON = ENTITY_TYPES.register(
        "shadow_art_ribbon",
        () -> EntityType.Builder.<ShadowArtRibbonEntity>of(ShadowArtRibbonEntity::new, MobCategory.MISC)
            .sized(0.75F, 0.75F)
            .clientTrackingRange(64)
            .updateInterval(2)
            .build(TypeMoonAddon.id("shadow_art_ribbon").toString())
    );

    public static final DeferredHolder<EntityType<?>, EntityType<ShadowPiercingRhoAiasEntity>> SHADOW_PIERCING_RHO_AIAS =
        ENTITY_TYPES.register(
            "shadow_piercing_rho_aias",
            () -> EntityType.Builder.<ShadowPiercingRhoAiasEntity>of(
                    ShadowPiercingRhoAiasEntity::new,
                    MobCategory.MISC
                )
                .sized(0.1F, 0.1F)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(TypeMoonAddon.id("shadow_piercing_rho_aias").toString())
        );

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SHADOW_FAMILIAR.get(), ShadowFamiliarEntity.createAttributes().build());
        event.put(BLACK_SHADOW.get(), BlackShadowEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
