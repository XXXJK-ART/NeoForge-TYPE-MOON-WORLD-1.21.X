package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TYPE_MOON_WORLD.MOD_ID);

    public static final Supplier<CreativeModeTab> TYPE_MOON_WORLD_TAB = CREATIVE_MODE_TAB.register("type_moon_world_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MAGIC_FRAGMENTS.get()))
                    .title(Component.translatable("creativetab.typemoonworld.type_moon_world"))
                    //添加创造栏物品
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.MAGIC_FRAGMENTS);
                        output.accept(ModItems.HOLY_SHROUD);

                        output.accept(ModItems.CHISEL);

                        output.accept(ModItems.RAW_EMERALD);
                        output.accept(ModItems.RAW_RUBY);
                        output.accept(ModItems.RAW_SAPPHIRE);
                        output.accept(ModItems.RAW_TOPAZ);
                        output.accept(ModItems.RAW_WHITE_GEMSTONE);

                        output.accept(ModItems.CARVED_EMERALD);
                        output.accept(ModItems.CARVED_RUBY);
                        output.accept(ModItems.CARVED_SAPPHIRE);
                        output.accept(ModItems.CARVED_TOPAZ);
                        output.accept(ModItems.CARVED_WHITE_GEMSTONE);

                        output.accept(ModItems.CARVED_EMERALD_FULL);
                        output.accept(ModItems.CARVED_RUBY_FULL);
                        output.accept(ModItems.CARVED_SAPPHIRE_FULL);
                        output.accept(ModItems.CARVED_TOPAZ_FULL);
                        output.accept(ModItems.CARVED_WHITE_GEMSTONE_FULL);

                        output.accept(ModBlocks.EMERALD_BLOCK);
                        output.accept(ModBlocks.EMERALD_MINE);
                        output.accept(ModBlocks.RUBY_BLOCK);
                        output.accept(ModBlocks.RUBY_MINE);
                        output.accept(ModBlocks.SAPPHIRE_BLOCK);
                        output.accept(ModBlocks.SAPPHIRE_MINE);
                        output.accept(ModBlocks.TOPAZ_BLOCK);
                        output.accept(ModBlocks.TOPAZ_MINE);
                        output.accept(ModBlocks.WHITE_GEMSTONE_BLOCK);
                        output.accept(ModBlocks.WHITE_GEMSTONE_MINE);
                        output.accept(ModBlocks.GREEN_TRANSPARENT_BLOCK);

                        output.accept(ModBlocks.SPIRIT_VEIN_BLOCK);
                        output.accept(ModBlocks.SPIRIT_VEIN_NODE);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
