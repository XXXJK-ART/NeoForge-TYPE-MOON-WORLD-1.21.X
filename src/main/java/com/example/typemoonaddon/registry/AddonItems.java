package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.item.CrestWormItem;
import com.example.typemoonaddon.item.EngravedWormItem;
import com.example.typemoonaddon.item.CursedArmorRenderItem;
import com.example.typemoonaddon.item.HolyGrailFragmentItem;
import com.example.typemoonaddon.item.ImaginaryPrimerItem;
import com.example.typemoonaddon.item.PrelatisSpellbookItem;
import com.example.typemoonaddon.item.SeaMonsterSpawnEggItem;
import com.example.typemoonaddon.item.WormItem;
import com.example.typemoonaddon.item.VoidRingRegaliaItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
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

    public static final DeferredItem<Item> MAGIC_BOOK_WORM_MAGIC = ITEMS.register(
            "magic_book_worm_magic",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D, null,
                    "worm_magic")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_WORM_MAGIC = ITEMS.register(
            "magic_page_worm_magic",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D, null,
                    "worm_magic")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_WORM_CONTROL = ITEMS.register(
            "magic_book_worm_control",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D,
                    "worm_magic", "worm_control")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_WORM_CONTROL = ITEMS.register(
            "magic_page_worm_control",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D,
                    "worm_magic", "worm_control")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_ENGRAVED_WORM_OPERATION = ITEMS.register(
            "magic_book_engraved_worm_operation",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.EPIC), 1.0D,
                    "worm_magic", "engraved_worm_operation")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_ENGRAVED_WORM_OPERATION = ITEMS.register(
            "magic_page_engraved_worm_operation",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.EPIC), 0.15D,
                    "worm_magic", "engraved_worm_operation")
    );

    public static final DeferredItem<Item> MAGIC_BOOK_BOUNDARY_ART = ITEMS.register(
            "magic_book_boundary_art",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D, null,
                    "boundary_art")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_BOUNDARY_ART = ITEMS.register(
            "magic_page_boundary_art",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D, null,
                    "boundary_art")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_SENSING_BOUNDARY = ITEMS.register(
            "magic_book_sensing_boundary",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D,
                    "boundary_art", "sensing_boundary")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_SENSING_BOUNDARY = ITEMS.register(
            "magic_page_sensing_boundary",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D,
                    "boundary_art", "sensing_boundary")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_WARNING_BOUNDARY = ITEMS.register(
            "magic_book_warning_boundary",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D,
                    "boundary_art", "warning_boundary")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_WARNING_BOUNDARY = ITEMS.register(
            "magic_page_warning_boundary",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D,
                    "boundary_art", "warning_boundary")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_DEFENSE_BOUNDARY = ITEMS.register(
            "magic_book_defense_boundary",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D,
                    "boundary_art", "defense_boundary")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_DEFENSE_BOUNDARY = ITEMS.register(
            "magic_page_defense_boundary",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D,
                    "boundary_art", "defense_boundary")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_SUGGESTION_BOUNDARY = ITEMS.register(
            "magic_book_suggestion_boundary",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D,
                    "boundary_art", "suggestion_boundary")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_SUGGESTION_BOUNDARY = ITEMS.register(
            "magic_page_suggestion_boundary",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D,
                    "boundary_art", "suggestion_boundary")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_ANTI_MAGIC_BOUNDARY = ITEMS.register(
            "magic_book_anti_magic_boundary",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D,
                    "boundary_art", "anti_magic_boundary")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_ANTI_MAGIC_BOUNDARY = ITEMS.register(
            "magic_page_anti_magic_boundary",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D,
                    "boundary_art", "anti_magic_boundary")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_GUARD_BOUNDARY = ITEMS.register(
            "magic_book_guard_boundary",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D,
                    "boundary_art", "guard_boundary")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_GUARD_BOUNDARY = ITEMS.register(
            "magic_page_guard_boundary",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D,
                    "boundary_art", "guard_boundary")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_INTERFERENCE_BOUNDARY = ITEMS.register(
            "magic_book_interference_boundary",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D,
                    "boundary_art", "interference_boundary")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_INTERFERENCE_BOUNDARY = ITEMS.register(
            "magic_page_interference_boundary",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D,
                    "boundary_art", "interference_boundary")
    );

    public static final DeferredItem<Item> MAGIC_BOOK_SPIRIT_SUMMONING = ITEMS.register(
            "magic_book_spirit_summoning",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D, null,
                    "spirit_summoning")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_SPIRIT_SUMMONING = ITEMS.register(
            "magic_page_spirit_summoning",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D, null,
                    "spirit_summoning")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_WRAITH_SERVITUDE = ITEMS.register(
            "magic_book_wraith_servitude",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.RARE), 1.0D,
                    "spirit_summoning", "wraith_servitude")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_WRAITH_SERVITUDE = ITEMS.register(
            "magic_page_wraith_servitude",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.RARE), 0.15D,
                    "spirit_summoning", "wraith_servitude")
    );
    public static final DeferredItem<Item> MAGIC_BOOK_EVIL_SPIRIT_SUMMONING = ITEMS.register(
            "magic_book_evil_spirit_summoning",
            () -> new MagicScrollItem(new Item.Properties().durability(20).rarity(Rarity.EPIC), 1.0D,
                    "spirit_summoning", "evil_spirit_summoning")
    );
    public static final DeferredItem<Item> MAGIC_PAGE_EVIL_SPIRIT_SUMMONING = ITEMS.register(
            "magic_page_evil_spirit_summoning",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3).rarity(Rarity.EPIC), 0.15D,
                    "spirit_summoning", "evil_spirit_summoning")
    );

    public static final DeferredItem<Item> CREST_WORM = ITEMS.register(
            "crest_worm",
            () -> new CrestWormItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))
    );

    public static final DeferredItem<Item> WORM = ITEMS.register(
            "worm",
            () -> new WormItem(new Item.Properties().rarity(Rarity.UNCOMMON))
    );

    public static final DeferredItem<Item> ENGRAVED_WORM = ITEMS.register(
            "engraved_worm",
            () -> new EngravedWormItem(new Item.Properties().rarity(Rarity.RARE))
    );

    public static final DeferredItem<Item> MANA_FURNACE = ITEMS.register(
            "mana_furnace",
            () -> new BlockItem(AddonBlocks.MANA_FURNACE.get(), new Item.Properties().rarity(Rarity.EPIC))
    );

    public static final DeferredItem<Item> HOLY_GRAIL_FRAGMENT = ITEMS.register(
            "holy_grail_fragment",
            () -> new HolyGrailFragmentItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))
    );

    public static final DeferredItem<PrelatisSpellbookItem> PRELATIS_SPELLBOOK = ITEMS.register(
            "prelatis_spellbook",
            () -> new PrelatisSpellbookItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())
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

    public static final DeferredItem<MasterCardItem> MASTER_CARD_MATOU_KARIYA = ITEMS.register(
            "master_card_matou_kariya",
            () -> new MasterCardItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(),
                    TypeMoonAddon.id("matou_kariya").toString()
            )
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
