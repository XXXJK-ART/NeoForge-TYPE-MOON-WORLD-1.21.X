package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.world.SakuraBlackMudBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AddonBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TypeMoonAddon.MOD_ID);

    public static final DeferredBlock<SakuraBlackMudBlock> BLACK_MUD = BLOCKS.register(
            "black_mud",
            () -> new SakuraBlackMudBlock(
                    AddonFluids.BLACK_MUD.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .sound(SoundType.EMPTY)
            )
    );

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    private AddonBlocks() {
    }
}
