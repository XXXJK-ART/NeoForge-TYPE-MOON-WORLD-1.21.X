package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;

@SuppressWarnings("null")
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TYPE_MOON_WORLD.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MuramasaBlockEntity>> MURAMASA_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("redswordblock", () ->
                    BlockEntityType.Builder.of(MuramasaBlockEntity::new, ModBlocks.MURAMASA_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UBWWeaponBlockEntity>> UBW_WEAPON_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("ubw_weapon_block_entity", () ->
                    BlockEntityType.Builder.of(UBWWeaponBlockEntity::new, ModBlocks.UBW_WEAPON_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordBarrelBlockEntity>> SWORD_BARREL_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("sword_barrel_block_entity", () ->
                    BlockEntityType.Builder.of(net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordBarrelBlockEntity::new, ModBlocks.SWORD_BARREL_BLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GemCarvingTableBlockEntity>> GEM_CARVING_TABLE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("gem_carving_table_block_entity", () ->
                    BlockEntityType.Builder.of(GemCarvingTableBlockEntity::new, ModBlocks.GEM_CARVING_TABLE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
