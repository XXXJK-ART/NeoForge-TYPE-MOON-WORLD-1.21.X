package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.item.CrestWormItem;
import com.example.typemoonaddon.item.CursedArmorRenderItem;
import com.example.typemoonaddon.item.HolyGrailFragmentItem;
import com.example.typemoonaddon.item.ImaginaryPrimerItem;
import com.example.typemoonaddon.item.SeaMonsterSpawnEggItem;
import com.example.typemoonaddon.item.VoidRingRegaliaItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MasterCardItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomMagicScrollItem;

public final class AddonItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TypeMoonAddon.MOD_ID);

    public static final DeferredItem<Item> MYSTIC_CODE_FRAGMENT = ITEMS.register(
            "mystic_code_fragment",
            () -> new Item(new Item.Properties())
    );

    public static final DeferredItem<Item> IMAGINARY_PRIMER = ITEMS.register(
            "imaginary_primer",
            () -> new ImaginaryPrimerItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE))
    );

    public static final DeferredItem<Item> MAGIC_BOOK_IMAGINARY_STORAGE = ITEMS.register(
            "magic_book_imaginary_storage",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D, (String) null,
                    TypeMoonAddon.id("imaginary_absorption").toString())
    );

    public static final DeferredItem<Item> MAGIC_PAGE_IMAGINARY_STORAGE = ITEMS.register(
            "magic_page_imaginary_storage",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.5D, (String) null,
                    TypeMoonAddon.id("imaginary_absorption").toString())
    );

    public static final DeferredItem<Item> MAGIC_BOOK_IMAGINARY_ABSORPTION = ITEMS.register(
            "magic_book_imaginary_absorption",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.EPIC), 1.0D, (String) null,
                    TypeMoonAddon.id("imaginary_absorption_evolved").toString())
    );

    public static final DeferredItem<Item> MAGIC_PAGE_IMAGINARY_ABSORPTION = ITEMS.register(
            "magic_page_imaginary_absorption",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.EPIC), 0.5D, (String) null,
                    TypeMoonAddon.id("imaginary_absorption_evolved").toString())
    );

    public static final DeferredItem<Item> CREST_WORM = ITEMS.register(
            "crest_worm",
            () -> new CrestWormItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))
    );

    public static final DeferredItem<Item> HOLY_GRAIL_FRAGMENT = ITEMS.register(
            "holy_grail_fragment",
            () -> new HolyGrailFragmentItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))
    );

    public static final DeferredItem<Item> PRELATIS_SPELLBOOK = ITEMS.register(
            "prelatis_spellbook",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())
    );

    public static final DeferredItem<CursedArmorRenderItem> CURSED_ARMOR_RENDER = ITEMS.register(
            "cursed_armor_render",
            () -> new CursedArmorRenderItem(new Item.Properties().stacksTo(1).fireResistant())
    );

    public static final DeferredItem<VoidRingRegaliaItem> VOID_RING_REGALIA = ITEMS.register(
            "void_ring_regalia",
            () -> new VoidRingRegaliaItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())
    );

    public static final DeferredItem<MasterCardItem> MASTER_CARD_MATOU_SAKURA = ITEMS.register(
            "master_card_matou_sakura",
            () -> new MasterCardItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(),
                    TypeMoonAddon.id("matou_sakura").toString()
            )
    );

    public static final DeferredItem<MasterCardItem> MASTER_CARD_MATOU_SAKURA_ALTER = ITEMS.register(
            "master_card_matou_sakura_alter",
            () -> new MasterCardItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(),
                    TypeMoonAddon.id("matou_sakura_alter").toString()
            )
    );

    public static final DeferredItem<MasterCardItem> MASTER_CARD_MATOU_SAKURA_FHA = ITEMS.register(
            "master_card_matou_sakura_fha",
            () -> new MasterCardItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(),
                    TypeMoonAddon.id("matou_sakura_fha").toString()
            )
    );

    public static final DeferredItem<DeferredSpawnEggItem> SHADOW_FAMILIAR_SPAWN_EGG = ITEMS.register(
            "shadow_familiar_spawn_egg",
            () -> new DeferredSpawnEggItem(AddonEntities.SHADOW_FAMILIAR, 0x12051F, 0xA74CFF, new Item.Properties())
    );

    public static final DeferredItem<DeferredSpawnEggItem> BLACK_SHADOW_SPAWN_EGG = ITEMS.register(
            "black_shadow_spawn_egg",
            () -> new DeferredSpawnEggItem(AddonEntities.BLACK_SHADOW, 0x08050A, 0x991C2C, new Item.Properties())
    );

    public static final DeferredItem<DeferredSpawnEggItem> GILLES_DE_RAIS_CASTER_SPAWN_EGG = ITEMS.register(
            "gilles_de_rais_caster_spawn_egg",
            () -> new DeferredSpawnEggItem(AddonEntities.GILLES_DE_RAIS_CASTER, 0x20202A, 0x8B1238, new Item.Properties())
    );

    public static final DeferredItem<DeferredSpawnEggItem> GILLES_SEA_MONSTER_SPAWN_EGG = ITEMS.register(
            "gilles_sea_monster_spawn_egg",
            () -> new SeaMonsterSpawnEggItem(AddonEntities.GILLES_SEA_MONSTER, 0x143B48, 0x6E1B64, new Item.Properties(), false)
    );

    public static final DeferredItem<DeferredSpawnEggItem> GILLES_LARGE_SEA_MONSTER_SPAWN_EGG = ITEMS.register(
            "gilles_large_sea_monster_spawn_egg",
            () -> new SeaMonsterSpawnEggItem(AddonEntities.GILLES_SEA_MONSTER, 0x10303A, 0xB22FA0, new Item.Properties(), true)
    );

    public static final DeferredItem<DeferredSpawnEggItem> GILLES_HUGE_SEA_MONSTER_SPAWN_EGG = ITEMS.register(
            "gilles_huge_sea_monster_spawn_egg",
            () -> new DeferredSpawnEggItem(AddonEntities.GILLES_HUGE_SEA_MONSTER, 0x071B20, 0x2FA6A5, new Item.Properties())
    );

    private AddonItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
