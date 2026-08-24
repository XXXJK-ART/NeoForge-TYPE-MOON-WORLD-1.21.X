package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.engravedworm.EngravedWormMenu;
import com.example.typemoonaddon.storage.StorageMenu;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.WormWarehouseMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AddonMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> STORAGE =
            MENUS.register("storage", () -> IMenuTypeExtension.create(StorageMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<WormWarehouseMenu>> WORM_WAREHOUSE =
            MENUS.register("worm_warehouse", () -> IMenuTypeExtension.create(WormWarehouseMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<EngravedWormMenu>> ENGRAVED_WORMS =
            MENUS.register("engraved_worms", () -> IMenuTypeExtension.create(EngravedWormMenu::new));

    private AddonMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
