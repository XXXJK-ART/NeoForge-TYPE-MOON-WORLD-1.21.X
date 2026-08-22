package com.example.typemoonaddon.block.entity;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.block.WormWarehouseBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AddonBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WormWarehouseBlockEntity>> WORM_WAREHOUSE =
            BLOCK_ENTITIES.register("worm_warehouse", () -> BlockEntityType.Builder.of(WormWarehouseBlockEntity::new, com.example.typemoonaddon.registry.AddonBlocks.WORM_WAREHOUSE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ManaFurnaceBlockEntity>> MANA_FURNACE =
            BLOCK_ENTITIES.register("mana_furnace", () -> BlockEntityType.Builder.of(ManaFurnaceBlockEntity::new,
                    com.example.typemoonaddon.registry.AddonBlocks.MANA_FURNACE.get()).build(null));

    private AddonBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
