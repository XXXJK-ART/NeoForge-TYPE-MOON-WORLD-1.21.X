package net.xxxjk.TYPE_MOON_WORLD.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.Magic_fragmentsItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TYPE_MOON_WORLD.MOD_ID);

    public static final DeferredItem<Item> MAGIC_FRAGMENTS = ITEMS.register("magic_fragments",
            () -> new Magic_fragmentsItem(new Item.Properties()));

    public static final DeferredItem<Item> HOLY_SHROUD = ITEMS.register("holy_shroud",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> CHISEL = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties().durability(100)));

    public static final DeferredItem<Item> CARVED_EMERALD = ITEMS.register("carved_emerald",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CARVED_RUBY = ITEMS.register("carved_ruby",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CARVED_SAPPHIRE = ITEMS.register("carved_sapphire",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CARVED_TOPAZ = ITEMS.register("carved_topaz",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE = ITEMS.register("carved_white_gemstone",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RAW_EMERALD = ITEMS.register("raw_emerald",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_RUBY = ITEMS.register("raw_ruby",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_SAPPHIRE = ITEMS.register("raw_sapphire",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_TOPAZ = ITEMS.register("raw_topaz",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_WHITE_GEMSTONE = ITEMS.register("raw_white_gemstone",
            () -> new Item(new Item.Properties()));





    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}