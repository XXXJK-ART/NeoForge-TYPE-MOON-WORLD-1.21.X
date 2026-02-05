package net.xxxjk.TYPE_MOON_WORLD.block;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.RedswordBlock;

import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RedswordBlockItem;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(TYPE_MOON_WORLD.MOD_ID);

    public static final DeferredBlock<Block> REDSWORD_BLOCK = registerBlock("redswordblock",
            () -> new RedswordBlock(BlockBehaviour.Properties.of()));

    public static final DeferredBlock<Block> SPIRIT_VEIN_NODE = registerBlock("spirit_vein_node",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of().strength(3F).requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel((s) -> 5)));

    public static final DeferredBlock<Block> SPIRIT_VEIN_BLOCK = registerBlock("spirit_vein_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel((s) -> 10)));


    public static final DeferredBlock<Block> EMERALD_BLOCK = registerBlock("emerald_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> EMERALD_MINE = registerBlock("emerald_mine",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of().strength(3F).requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final DeferredBlock<Block> RUBY_BLOCK = registerBlock("ruby_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> RUBY_MINE = registerBlock("ruby_mine",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of().strength(3F).requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final DeferredBlock<Block> SAPPHIRE_BLOCK = registerBlock("sapphire_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> SAPPHIRE_MINE = registerBlock("sapphire_mine",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of().strength(3F).requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final DeferredBlock<Block> TOPAZ_BLOCK = registerBlock("topaz_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> TOPAZ_MINE = registerBlock("topaz_mine",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of().strength(3F).requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final DeferredBlock<Block> WHITE_GEMSTONE_BLOCK = registerBlock("white_gemstone_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final DeferredBlock<Block> WHITE_GEMSTONE_MINE = registerBlock("white_gemstone_mine",
            () -> new DropExperienceBlock(UniformInt.of(2, 4),
                    BlockBehaviour.Properties.of().strength(3F).requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final DeferredBlock<Block> GREEN_TRANSPARENT_BLOCK = registerBlock("green_transparent_block",
            () -> new HalfTransparentBlock(BlockBehaviour.Properties.of()
                    .strength(50f, 1200f).sound(SoundType.GLASS).noOcclusion()
                    .isViewBlocking((s, l, p) -> false).isValidSpawn((s, l, p, e) -> false)
                    .isSuffocating((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false)));

    public static final DeferredBlock<Block> UBW_WEAPON_BLOCK = registerBlock("ubw_weapon_block",
            () -> new net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock(BlockBehaviour.Properties.of()
                    .strength(0.1f).noOcclusion().noCollission().dynamicShape()));

    private static <T extends Block> DeferredBlock<T> registerBlock (String name, Supplier<T> block){
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    
    private static <T extends Block> DeferredBlock<T> registerBlockWithCustomItem(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new RedswordBlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }

    public static void register(IEventBus eventBus){
        BLOCKS.register(eventBus);
    }
}
