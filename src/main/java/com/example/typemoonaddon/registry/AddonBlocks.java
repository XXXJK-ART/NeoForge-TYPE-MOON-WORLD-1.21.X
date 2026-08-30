package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.block.WormWarehouseBlock;
import com.example.typemoonaddon.block.ManaFurnaceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AddonBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TypeMoonAddon.MOD_ID);

    public static final DeferredBlock<WormWarehouseBlock> WORM_WAREHOUSE = BLOCKS.register(
            "worm_warehouse",
            () -> new WormWarehouseBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(-1.0F, 3600000.0F)
                            .noCollission()
                            .noOcclusion()
                            .noLootTable()
                            .pushReaction(PushReaction.BLOCK)
            )
    );

    public static final DeferredBlock<ManaFurnaceBlock> MANA_FURNACE = BLOCKS.register(
            "mana_furnace",
            () -> new ManaFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.STONE))
    );

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private AddonBlocks() {
    }
}
