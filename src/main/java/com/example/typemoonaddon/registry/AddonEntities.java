package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.AirflowBladeEntity;
import com.example.typemoonaddon.entity.StorageVisualEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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

    private AddonEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
