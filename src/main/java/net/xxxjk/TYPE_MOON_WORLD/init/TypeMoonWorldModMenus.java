package net.xxxjk.TYPE_MOON_WORLD.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.registries.Registries;

import net.xxxjk.TYPE_MOON_WORLD.world.inventory.BasicInformationMenu;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.GemCarvingTableMenu;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicalattributesMenu;

public class TypeMoonWorldModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, TYPE_MOON_WORLD.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<BasicInformationMenu>> BASIC_INFORMATION
            = REGISTRY.register("basicinformation",
            () -> IMenuTypeExtension.create(BasicInformationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MagicalattributesMenu>> MAGICAL_ATTRIBUTES
            = REGISTRY.register("magicalattributes",
            () -> IMenuTypeExtension.create(MagicalattributesMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GemCarvingTableMenu>> GEM_CARVING_TABLE
            = REGISTRY.register("gem_carving_table",
            () -> IMenuTypeExtension.create(GemCarvingTableMenu::new));

}
