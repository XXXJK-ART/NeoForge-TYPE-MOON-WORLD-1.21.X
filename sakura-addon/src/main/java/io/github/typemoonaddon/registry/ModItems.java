package io.github.typemoonaddon.registry;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.item.ImaginaryPrimerItem;
import io.github.typemoonaddon.item.CrestWormItem;
import io.github.typemoonaddon.item.HolyGrailFragmentItem;
import io.github.typemoonaddon.item.BlackShadowSpawnEggItem;
import io.github.typemoonaddon.item.CursedArmorRenderItem;
import io.github.typemoonaddon.item.VoidRingRegaliaItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TypeMoonAddon.MOD_ID);

    public static final DeferredItem<Item> IMAGINARY_PRIMER = ITEMS.registerItem(
        "imaginary_primer",
        ImaginaryPrimerItem::new,
        new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
    );

    public static final DeferredItem<Item> CREST_WORM = ITEMS.registerItem(
        "crest_worm",
        CrestWormItem::new,
        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)
    );

    public static final DeferredItem<Item> HOLY_GRAIL_FRAGMENT = ITEMS.registerItem(
        "holy_grail_fragment",
        HolyGrailFragmentItem::new,
        new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)
    );

    public static final DeferredItem<CursedArmorRenderItem> CURSED_ARMOR_RENDER = ITEMS.register(
        "cursed_armor_render",
        () -> new CursedArmorRenderItem(new Item.Properties().stacksTo(1).fireResistant())
    );

    public static final DeferredItem<VoidRingRegaliaItem> VOID_RING_REGALIA = ITEMS.register(
        "void_ring_regalia",
        () -> new VoidRingRegaliaItem(
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
        )
    );

    public static final DeferredItem<DeferredSpawnEggItem> SHADOW_FAMILIAR_SPAWN_EGG = ITEMS.register(
        "shadow_familiar_spawn_egg",
        () -> new DeferredSpawnEggItem(
            ModEntities.SHADOW_FAMILIAR,
            0x12051F,
            0xA74CFF,
            new Item.Properties()
        )
    );

    public static final DeferredItem<BlackShadowSpawnEggItem> BLACK_SHADOW_SPAWN_EGG = ITEMS.register(
        "black_shadow_spawn_egg",
        () -> new BlackShadowSpawnEggItem(
            ModEntities.BLACK_SHADOW,
            0x08050A,
            0x991C2C,
            new Item.Properties()
        )
    );

    private ModItems() {
    }
}
