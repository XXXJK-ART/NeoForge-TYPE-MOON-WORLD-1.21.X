package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.BasicInformationMenu;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.GemCarvingTableMenu;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicalattributesMenu;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicResearchTableMenu;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.MagicCopyingTableMenu;

public class TypeMoonWorldModMenus {
   public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, "typemoonworld");
   public static final DeferredHolder<MenuType<?>, MenuType<BasicInformationMenu>> BASIC_INFORMATION = REGISTRY.register(
      "basicinformation", () -> IMenuTypeExtension.create(BasicInformationMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MagicalattributesMenu>> MAGICAL_ATTRIBUTES = REGISTRY.register(
      "magicalattributes", () -> IMenuTypeExtension.create(MagicalattributesMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<GemCarvingTableMenu>> GEM_CARVING_TABLE = REGISTRY.register(
      "gem_carving_table", () -> IMenuTypeExtension.create(GemCarvingTableMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MagicResearchTableMenu>> MAGIC_RESEARCH_TABLE = REGISTRY.register(
      "magic_research_table", () -> IMenuTypeExtension.create(MagicResearchTableMenu::new)
   );
   public static final DeferredHolder<MenuType<?>, MenuType<MagicCopyingTableMenu>> MAGIC_COPYING_TABLE = REGISTRY.register(
      "magic_copying_table", () -> IMenuTypeExtension.create(MagicCopyingTableMenu::new)
   );
}
