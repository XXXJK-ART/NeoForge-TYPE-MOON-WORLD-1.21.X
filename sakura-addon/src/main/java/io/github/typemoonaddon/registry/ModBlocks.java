package io.github.typemoonaddon.registry;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.world.BlackMudBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TypeMoonAddon.MOD_ID);

    public static final DeferredBlock<BlackMudBlock> BLACK_MUD = BLOCKS.register(
        "black_mud",
        () -> new BlackMudBlock(
            ModFluids.BLACK_MUD.get(),
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

    private ModBlocks() {
    }
}
