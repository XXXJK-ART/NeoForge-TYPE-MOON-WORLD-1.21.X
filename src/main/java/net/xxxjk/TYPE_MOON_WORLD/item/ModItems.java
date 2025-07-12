package net.xxxjk.TYPE_MOON_WORLD.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TYPE_MOON_WORLD.MOD_ID);

    public static final DeferredItem<Item> MAGIC_FRAGMENTS = ITEMS.register("magicfragments",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HOLY_SHROUD = ITEMS.register("holyshroud",
            () -> new Item(new Item.Properties()));



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
