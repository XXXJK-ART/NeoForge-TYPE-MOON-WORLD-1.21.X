package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.AirflowBladeEntity;
import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import com.example.typemoonaddon.entity.HugeSeaMonsterEntity;
import com.example.typemoonaddon.entity.SeaMonsterEntity;
import com.example.typemoonaddon.entity.SeaMonsterSpitEntity;
import com.example.typemoonaddon.entity.StorageVisualEntity;
import com.example.typemoonaddon.entity.WraithEntity;
import com.example.typemoonaddon.entity.EvilSpiritEntity;
import com.example.typemoonaddon.entity.BoundaryMarkEntity;
import com.example.typemoonaddon.worm.WormEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

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

    public static final DeferredHolder<EntityType<?>, EntityType<SeaMonsterSpitEntity>> GILLES_SEA_MONSTER_SPIT =
            ENTITIES.register("gilles_sea_monster_spit", () -> EntityType.Builder
                    .<SeaMonsterSpitEntity>of(SeaMonsterSpitEntity::new, MobCategory.MISC)
                    .sized(0.55F, 0.55F)
                    .clientTrackingRange(96)
                    .updateInterval(1)
                    .build("gilles_sea_monster_spit"));

    public static final DeferredHolder<EntityType<?>, EntityType<HugeSeaMonsterEntity>> GILLES_HUGE_SEA_MONSTER =
            ENTITIES.register("gilles_huge_sea_monster", () -> EntityType.Builder
                    .of(HugeSeaMonsterEntity::new, MobCategory.MONSTER)
                    .sized(36.0F, 64.0F)
                    .eyeHeight(32.0F)
                    .clientTrackingRange(32)
                    .build("gilles_huge_sea_monster"));

    public static final DeferredHolder<EntityType<?>, EntityType<WormEntity>> WORM =
            ENTITIES.register("worm", () -> EntityType.Builder
                    .of(WormEntity::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.25F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("worm"));

    public static final DeferredHolder<EntityType<?>, EntityType<WraithEntity>> WRAITH =
            ENTITIES.register("wraith", () -> EntityType.Builder
                    .of(WraithEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.9F)
                    .eyeHeight(0.65F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("wraith"));

    public static final DeferredHolder<EntityType<?>, EntityType<EvilSpiritEntity>> EVIL_SPIRIT =
            ENTITIES.register("evil_spirit", () -> EntityType.Builder
                    .of(EvilSpiritEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.0F)
                    .eyeHeight(1.5F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("evil_spirit"));

    public static final DeferredHolder<EntityType<?>, EntityType<EvilSpiritEntity>> EVIL_SPIRIT_SMALL =
            ENTITIES.register("evil_spirit_small", () -> EntityType.Builder
                    .of(EvilSpiritEntity::new, MobCategory.MONSTER)
                    .sized(0.65F, 0.95F)
                    .eyeHeight(0.7F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("evil_spirit_small"));

    public static final DeferredHolder<EntityType<?>, EntityType<BoundaryMarkEntity>> BOUNDARY_MARK =
            ENTITIES.register("boundary_mark", () -> EntityType.Builder
                    .<BoundaryMarkEntity>of(BoundaryMarkEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .build("boundary_mark"));

    private AddonEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GILLES_DE_RAIS_CASTER.get(), GillesDeRaisEntity.createAttributes().build());
        event.put(GILLES_SEA_MONSTER.get(), SeaMonsterEntity.createAttributes().build());
        event.put(GILLES_HUGE_SEA_MONSTER.get(), HugeSeaMonsterEntity.createAttributes().build());
        event.put(WORM.get(), WormEntity.createAttributes().build());
        event.put(WRAITH.get(), WraithEntity.createAttributes().build());
        event.put(EVIL_SPIRIT.get(), EvilSpiritEntity.createAttributes().build());
        event.put(EVIL_SPIRIT_SMALL.get(), EvilSpiritEntity.createSmallAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(WORM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AddonEntities::checkWildWormSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Mob> boolean checkWildWormSpawnRules(
            EntityType<T> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && pos.getY() < level.getSeaLevel() + 24
                && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
    }
}
