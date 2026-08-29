package io.github.typemoonaddon.registry;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.menu.ImaginarySpaceMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ImaginarySpaceMenu>> IMAGINARY_SPACE = MENUS.register(
        "imaginary_space",
        () -> new MenuType<>(ImaginarySpaceMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    private ModMenus() {
    }
}
