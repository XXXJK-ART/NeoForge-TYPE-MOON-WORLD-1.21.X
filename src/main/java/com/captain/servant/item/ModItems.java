package com.captain.servant.item;

import com.captain.servant.TypeMoonServant;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TypeMoonServant.MOD_ID);

    public static final DeferredItem<Item> MESSAGE_STONE = ITEMS.register(
            "message_stone",
            () -> new MessageStoneItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
