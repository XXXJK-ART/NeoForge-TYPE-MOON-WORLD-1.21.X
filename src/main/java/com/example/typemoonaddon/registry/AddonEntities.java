package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.AirflowBladeEntity;
import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import com.example.typemoonaddon.entity.HugeSeaMonsterEntity;
import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import com.example.typemoonaddon.entity.SakuraShadowArtRibbonEntity;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.example.typemoonaddon.entity.SakuraShadowPiercingRhoAiasEntity;
import com.example.typemoonaddon.entity.SeaMonsterEntity;
import com.example.typemoonaddon.entity.SakuraVisualEntity;
import com.example.typemoonaddon.entity.StorageVisualEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

public final class AddonEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<StorageVisualEntity>> STORAGE_VISUAL =
            ENTITIES.register("imaginary_storage_visual", () -> EntityType.Builder
                    .<StorageVisualEntity>of(StorageVisualEntity::new, MobCategory.MISC)
                    .sized(3.2F, 3.2F)
                    .clientTrackingRange(160)
                    .updateInterval(1)
                    .build("imaginary_storage_visual"));

    public static final DeferredHolder<EntityType<?>, EntityType<AirflowBladeEntity>> AIRFLOW_BLADE =
            ENTITIES.register("airflow_blade_projectile", () -> EntityType.Builder
                    .<AirflowBladeEntity>of(AirflowBladeEntity::new, MobCategory.MISC)
                    .sized(0.45F, 0.45F)
                    .clientTrackingRange(96)
                    .updateInterval(1)
                    .build("airflow_blade_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SakuraShadowFamiliarEntity>> SHADOW_FAMILIAR =
            ENTITIES.register("shadow_familiar", () -> EntityType.Builder
                    .of(SakuraShadowFamiliarEntity::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.85F)
                    .eyeHeight(1.30F)
                    .clientTrackingRange(8)
                    .build("shadow_familiar"));

    public static final DeferredHolder<EntityType<?>, EntityType<SakuraBlackShadowEntity>> BLACK_SHADOW =
            ENTITIES.register("black_shadow", () -> EntityType.Builder
                    .of(SakuraBlackShadowEntity::new, MobCategory.MONSTER)
                    .sized(0.60F, 1.80F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(8)
                    .build("black_shadow"));

    public static final DeferredHolder<EntityType<?>, EntityType<SakuraShadowArtRibbonEntity>> SHADOW_ART_RIBBON =
            ENTITIES.register("shadow_art_ribbon", () -> EntityType.Builder
                    .<SakuraShadowArtRibbonEntity>of(SakuraShadowArtRibbonEntity::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build("shadow_art_ribbon"));

    public static final DeferredHolder<EntityType<?>, EntityType<SakuraShadowPiercingRhoAiasEntity>> SHADOW_PIERCING_RHO_AIAS =
            ENTITIES.register("shadow_piercing_rho_aias", () -> EntityType.Builder
                    .<SakuraShadowPiercingRhoAiasEntity>of(SakuraShadowPiercingRhoAiasEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("shadow_piercing_rho_aias"));

    public static final DeferredHolder<EntityType<?>, EntityType<GillesDeRaisEntity>> GILLES_DE_RAIS_CASTER =
            ENTITIES.register("gilles_de_rais_caster", () -> EntityType.Builder
                    .of(GillesDeRaisEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.86F)
                    .eyeHeight(1.68F)
                    .clientTrackingRange(12)
                    .build("gilles_de_rais_caster"));

    public static final DeferredHolder<EntityType<?>, EntityType<SeaMonsterEntity>> GILLES_SEA_MONSTER =
            ENTITIES.register("gilles_sea_monster", () -> EntityType.Builder
                    .of(SeaMonsterEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.85F)
                    .eyeHeight(1.0F)
                    .clientTrackingRange(12)
                    .build("gilles_sea_monster"));

    public static final DeferredHolder<EntityType<?>, EntityType<HugeSeaMonsterEntity>> GILLES_HUGE_SEA_MONSTER =
            ENTITIES.register("gilles_huge_sea_monster", () -> EntityType.Builder
                    .of(HugeSeaMonsterEntity::new, MobCategory.MONSTER)
                    .sized(30.0F, 55.5F)
                    .eyeHeight(28.0F)
                    .clientTrackingRange(32)
                    .build("gilles_huge_sea_monster"));

    private AddonEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SHADOW_FAMILIAR.get(), SakuraShadowFamiliarEntity.createAttributes().build());
        event.put(BLACK_SHADOW.get(), SakuraBlackShadowEntity.createAttributes().build());
        event.put(GILLES_DE_RAIS_CASTER.get(), GillesDeRaisEntity.createAttributes().build());
        event.put(GILLES_SEA_MONSTER.get(), SeaMonsterEntity.createAttributes().build());
        event.put(GILLES_HUGE_SEA_MONSTER.get(), HugeSeaMonsterEntity.createAttributes().build());
    }
}
