package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AddonItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TypeMoonAddon.MOD_ID);

    public static final DeferredItem<Item> MYSTIC_CODE_FRAGMENT = ITEMS.register(
            "mystic_code_fragment",
            () -> new Item(new Item.Properties())
    );

    private AddonItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
