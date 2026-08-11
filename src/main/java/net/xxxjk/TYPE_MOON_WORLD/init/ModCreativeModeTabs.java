package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TYPE_MOON_WORLD.MOD_ID);

    public static final Supplier<CreativeModeTab> SERVANT_CARDS_TAB = CREATIVE_MODE_TAB.register("servant_cards_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.SERVANT_CARD_EMIYA_ARCHER.get()))
                    .title(Component.translatable("creativetab.typemoonworld.servant_cards"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.SERVANT_MASTER_CONTRACT);
                        output.accept(ModItems.COMMAND_SPELL);
                        output.accept(ModItems.SUPERVISOR_COMMAND_SPELL);
                        output.accept(ModItems.SINGLE_COMMAND_SPELL);
                        output.accept(ModItems.SERVANT_CARD_RELEASE);
                        output.accept(ModItems.CHALK);
                        output.accept(ModBlocks.SUMMONING_CIRCLE);
                        output.accept(ModItems.RANDOM_START_ATTRIBUTES);
                        output.accept(ModBlocks.ARTIFICIAL_LEYLINE_BLOCK);
                        output.accept(ModItems.MASTER_CARD_TOHSAKA_RIN);
                        output.accept(ModItems.MASTER_CARD_EMIYA_KIRITSUGU);
                        output.accept(ModItems.MASTER_CARD_EMIYA_SHIROU);
                        output.accept(ModItems.MASTER_CARD_KOTOMINE_KIREI);
                        output.accept(ModItems.MASTER_CARD_LUVIA);
                        output.accept(ModItems.MASTER_CARD_ELSA_SAIJO);
                        output.accept(ModItems.MASTER_CARD_WAVER);
                        output.accept(ModItems.MASTER_CARD_TOHSAKA_TOKIOMI);
                        output.accept(ModItems.SERVANT_CARD_EMIYA_ARCHER);
                        output.accept(ModItems.SERVANT_CARD_ARTORIA_PENDRAGON);
                        output.accept(ModItems.SERVANT_CARD_SASAKI_KOJIRO);
                        output.accept(ModItems.SERVANT_CARD_CU_CHULAINN);
                        output.accept(ModItems.SERVANT_CARD_MEDEA);
                        output.accept(ModItems.SERVANT_CARD_MEDUSA);
                        output.accept(ModItems.SERVANT_CARD_CURSED_ARM_HASSAN);
                        output.accept(ModItems.SERVANT_CARD_SHADOW_HASSAN);
                        output.accept(ModItems.SERVANT_CARD_HERACLES);
                        output.accept(ModItems.SERVANT_CARD_ODA_NOBUNAGA);
                        output.accept(ModItems.SERVANT_CARD_ENKIDU);
                        output.accept(ModItems.SERVANT_CARD_GILGAMESH);
                        output.accept(new ItemStack(ModItems.SERVANT_CARD_GILGAMESH_CASTER.get()));
                        output.accept(ModItems.SERVANT_CARD_GAWAIN);
                        output.accept(ModItems.SERVANT_CARD_PARACELSUS);
                        output.accept(ModItems.SERVANT_CARD_LI_SHUWEN);
                        output.accept(ModItems.SERVANT_CARD_PALE_RIDER);
                        output.accept(ModItems.SERVANT_CARD_HUNDRED_FACES_HASSAN);
                        output.accept(ModItems.SERVANT_CARD_USHIWAKAMARU_RIDER);
                        output.accept(ModItems.SERVANT_CARD_FANATIC_ASSASSIN);
                        output.accept(ModItems.SERVANT_CARD_ARASH);
                        output.accept(ModItems.SERVANT_CARD_NIGHTINGALE);
                        output.accept(ModItems.SERVANT_CARD_ZHAO_YUN_RIDER);
                        output.accept(ModItems.SERVANT_CARD_SENKO_MURAMASA);
                        output.accept(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE);
                        output.accept(ModItems.SERVANT_CARD_EMIYA_ARCHER_CHEST);
                        output.accept(ModItems.SERVANT_CARD_EMIYA_ARCHER_LEGS);
                        output.accept(ModItems.SERVANT_CARD_ARTORIA_PENDRAGON_HEAD);
                        output.accept(ModItems.SERVANT_CARD_ARTORIA_PENDRAGON_CHEST);
                        output.accept(ModItems.SERVANT_CARD_ARTORIA_PENDRAGON_LEGS);
                        output.accept(ModItems.SERVANT_CARD_SASAKI_KOJIRO_HEAD);
                        output.accept(ModItems.SERVANT_CARD_SASAKI_KOJIRO_CHEST);
                        output.accept(ModItems.SERVANT_CARD_SASAKI_KOJIRO_LEGS);
                        output.accept(ModItems.SERVANT_CARD_CU_CHULAINN_CHEST);
                        output.accept(ModItems.SERVANT_CARD_CU_CHULAINN_LEGS);
                        output.accept(ModItems.SERVANT_CARD_MEDEA_HEAD);
                        output.accept(ModItems.SERVANT_CARD_MEDEA_CHEST);
                        output.accept(ModItems.SERVANT_CARD_MEDUSA_HEAD);
                        output.accept(ModItems.SERVANT_CARD_MEDUSA_CHEST);
                        output.accept(ModItems.SERVANT_CARD_MEDUSA_LEGS);
                        output.accept(ModItems.SERVANT_CARD_CURSED_ARM_HASSAN_HEAD);
                        output.accept(ModItems.SHADOW_HASSAN_MASK);
                        output.accept(ModItems.SERVANT_CARD_CURSED_ARM_HASSAN_CHEST);
                        output.accept(ModItems.SERVANT_CARD_HERACLES_CHEST);
                        output.accept(ModItems.SERVANT_CARD_HERACLES_LEGS);
                        output.accept(ModItems.SERVANT_CARD_ODA_NOBUNAGA_CHEST);
                        output.accept(ModItems.SERVANT_CARD_ODA_NOBUNAGA_LEGS);
                        output.accept(ModItems.SERVANT_CARD_ODA_NOBUNAGA_HEAD);
                        output.accept(ModItems.SERVANT_CARD_ENKIDU_HEAD);
                        output.accept(ModItems.SERVANT_CARD_ENKIDU_CHEST);
                        output.accept(ModItems.SERVANT_CARD_ENKIDU_LEGS);
                        output.accept(ModItems.SERVANT_CARD_GILGAMESH_CHEST);
                        output.accept(ModItems.SERVANT_CARD_GILGAMESH_LEGS);
                        output.accept(new ItemStack(ModItems.SERVANT_CARD_GILGAMESH_CASTER_HEAD.get()));
                        output.accept(new ItemStack(ModItems.SERVANT_CARD_GILGAMESH_CASTER_CHEST.get()));
                        output.accept(new ItemStack(ModItems.SERVANT_CARD_GILGAMESH_CASTER_LEGS.get()));
                        output.accept(ModItems.SERVANT_CARD_GAWAIN_CHEST);
                        output.accept(ModItems.SERVANT_CARD_GAWAIN_LEGS);
                        output.accept(ModItems.SERVANT_CARD_PARACELSUS_HEAD);
                        output.accept(ModItems.SERVANT_CARD_PARACELSUS_CHEST);
                        output.accept(ModItems.SERVANT_CARD_PARACELSUS_LEGS);
                        output.accept(ModItems.SERVANT_CARD_LI_SHUWEN_CHEST);
                        output.accept(ModItems.SERVANT_CARD_LI_SHUWEN_LEGS);
                        output.accept(ModItems.SERVANT_CARD_LI_SHUWEN_HEAD);
                        output.accept(ModItems.SERVANT_CARD_USHIWAKAMARU_RIDER_HEAD);
                        output.accept(ModItems.SERVANT_CARD_USHIWAKAMARU_RIDER_CHEST);
                        output.accept(ModItems.SERVANT_CARD_USHIWAKAMARU_RIDER_LEGS);
                        output.accept(ModItems.SERVANT_CARD_FANATIC_ASSASSIN_HEAD);
                        output.accept(ModItems.SERVANT_CARD_FANATIC_ASSASSIN_CHEST);
                        output.accept(ModItems.SERVANT_CARD_HUNDRED_FACES_HASSAN_HEAD);
                        output.accept(ModItems.SERVANT_CARD_HUNDRED_FACES_HASSAN_CHEST);
                        output.accept(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE_CHEST);
                        output.accept(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE_LEGS);
                        output.accept(ModItems.SERVANT_CARD_DIARMUID_UA_DUIBHNE_FEET);
                        output.accept(ModItems.SERVANT_CARD_ARASH_CHEST);
                        output.accept(ModItems.SERVANT_CARD_ARASH_LEGS);
                        output.accept(ModItems.SERVANT_CARD_NIGHTINGALE_CHEST);
                        output.accept(ModItems.SERVANT_CARD_NIGHTINGALE_LEGS);
                        output.accept(ModItems.SERVANT_CARD_ZHAO_YUN_RIDER_HEAD);
                        output.accept(ModItems.SERVANT_CARD_ZHAO_YUN_RIDER_CHEST);
                        output.accept(ModItems.SERVANT_CARD_ZHAO_YUN_RIDER_LEGS);
                        output.accept(ModItems.SERVANT_CARD_SENKO_MURAMASA_CHEST);
                        output.accept(ModItems.SERVANT_CARD_SENKO_MURAMASA_LEGS);
                    }).build());

    public static final Supplier<CreativeModeTab> RELICS_TAB = CREATIVE_MODE_TAB.register("relics_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.RELIC_GOLDEN_FLEECE.get()))
                    .title(Component.translatable("creativetab.typemoonworld.relics"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RELIC_APOCALYPSE);
                        output.accept(ModItems.RELIC_APOCALYPSE_PAGE);
                        output.accept(ModItems.RELIC_FIRST_SNAKE_SKIN);
                        output.accept(ModItems.RELIC_VALKYRIE_ARROWHEAD);
                        output.accept(ModItems.RELIC_BRONZE_MIRROR);
                        output.accept(ModItems.RELIC_ROUND_TABLE_FRAGMENT);
                        output.accept(ModItems.RELIC_TSUBURA_SHIP_PLANK);
                        output.accept(ModItems.RELIC_BIZEN_TSUBA);
                        output.accept(ModItems.RELIC_OLD_MAN_MASK);
                        output.accept(ModItems.RELIC_BANDAGE);
                        output.accept(ModItems.RELIC_PHILOSOPHERS_STONE);
                        output.accept(ModItems.RELIC_GOLDEN_FLEECE);
                        output.accept(ModItems.RELIC_ATO_CRADLE);
                        output.accept(ModItems.SEA_BEAST_BONE);
                        output.accept(ModItems.ODA_MATCHLOCK_CATALYST);
                        output.accept(ModItems.BROKEN_BOWSTRING);
                        output.accept(ModBlocks.AGE_OF_GODS_DIRT);
                        output.accept(ModItems.GEM_NECKLACE);
                        output.accept(ModItems.AVALON);
                    }).build());

    public static final Supplier<CreativeModeTab> TYPE_MOON_WORLD_TAB = CREATIVE_MODE_TAB.register("type_moon_world_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MAGIC_FRAGMENTS.get()))
                    .title(Component.translatable("creativetab.typemoonworld.type_moon_world"))
                    //添加创造栏物品
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.MAGIC_FRAGMENTS);
                        output.accept(ModItems.RELIC_HAJIQUAN_MANUAL);
                        output.accept(ModItems.GANRYU_MANUAL);
                        output.accept(ModItems.HOKUSHIN_MANUAL);
                        output.accept(ModItems.TENNEN_MANUAL);
                        output.accept(ModItems.SPARRING_INVITATION);
                        output.accept(ModItems.HOLY_SHROUD);
                        output.accept(ModItems.CLAW_OF_CHAOS);
                        output.accept(ModItems.DRAGON_FANG);
                        output.accept(ModItems.DRAGONS_REVERSE_SCALE);
                        output.accept(ModItems.EVIL_BONE);
                        output.accept(ModItems.HOLY_GRAIL);
                        output.accept(ModItems.PHOENIX_FEATHER);
                        output.accept(ModItems.PROOF_OF_HERO);
                        output.accept(ModItems.QP);
                        output.accept(ModItems.REMNANTS_OF_MADNESS);
                        output.accept(ModItems.SEED_OF_YGGDRASIL);
                        output.accept(ModItems.VOIDS_DUST);
                        output.accept(ModItems.GEM_NECKLACE);
                        output.accept(ModItems.MYSTIC_MERCURY);
                        output.accept(net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticMercuryItem.emptyVariant(ModItems.MYSTIC_MERCURY.get()));

                        output.accept(ModItems.WAKIZASHI);
                        output.accept(ModItems.KATANA);
                        output.accept(ModItems.NODACHI);

                        output.accept(ModItems.MAGIC_SCROLL_BASIC_JEWEL);
                        output.accept(ModItems.MAGIC_SCROLL_BASIC_JEWEL_BROKEN);
                        output.accept(ModItems.MAGIC_SCROLL_ADVANCED_JEWEL);
                        output.accept(ModItems.MAGIC_SCROLL_ADVANCED_JEWEL_BROKEN);
                        output.accept(ModItems.MAGIC_SCROLL_MACHINE_GUN);
                        output.accept(ModItems.MAGIC_SCROLL_MACHINE_GUN_BROKEN);
                        output.accept(ModItems.MAGIC_SCROLL_PROJECTION);
                        output.accept(ModItems.MAGIC_SCROLL_PROJECTION_BROKEN);
                        output.accept(ModItems.MAGIC_SCROLL_GRAVITY);
                        output.accept(ModItems.MAGIC_SCROLL_GRAVITY_BROKEN);
                        output.accept(ModItems.MAGIC_SCROLL_GANDER);
                        output.accept(ModItems.MAGIC_SCROLL_GANDER_BROKEN);
                        output.accept(ModItems.MAGIC_SCROLL_BROKEN_PHANTASM);
                        output.accept(ModItems.MAGIC_SCROLL_BROKEN_PHANTASM_BROKEN);
                        output.accept(ModItems.UNKNOWN_MAGIC_PAGE);
                        output.accept(ModItems.MAGIC_WASTE_PAPER);
                        output.accept(ModItems.MAGIC_BOOK_PROJECTION);
                        output.accept(ModItems.MAGIC_PAGE_PROJECTION);
                        output.accept(ModItems.MAGIC_BOOK_STRUCTURAL_ANALYSIS);
                        output.accept(ModItems.MAGIC_PAGE_STRUCTURAL_ANALYSIS);
                        output.accept(ModItems.MAGIC_BOOK_MAGIC_ANALYSIS);
                        output.accept(ModItems.MAGIC_PAGE_MAGIC_ANALYSIS);
                        output.accept(ModItems.MAGIC_BOOK_REINFORCEMENT);
                        output.accept(ModItems.MAGIC_BOOK_MANA_BURST);
                        output.accept(ModItems.MAGIC_PAGE_REINFORCEMENT);
                        output.accept(ModItems.MAGIC_BOOK_HEALING);
                        output.accept(ModItems.MAGIC_PAGE_HEALING);
                        output.accept(ModItems.MAGIC_BOOK_MAGIC_BULLET);
                        output.accept(ModItems.MAGIC_PAGE_MAGIC_BULLET);
                        output.accept(ModItems.MAGIC_BOOK_SUGGESTION);
                        output.accept(ModItems.MAGIC_PAGE_SUGGESTION);
                        output.accept(ModItems.MAGIC_BOOK_BINDING);
                        output.accept(ModItems.MAGIC_PAGE_BINDING);
                        output.accept(ModItems.MAGIC_BOOK_FIRE);
                        output.accept(ModItems.MAGIC_PAGE_FIRE);
                        output.accept(ModItems.MAGIC_BOOK_WATER);
                        output.accept(ModItems.MAGIC_PAGE_WATER);
                        output.accept(ModItems.MAGIC_BOOK_WIND);
                        output.accept(ModItems.MAGIC_PAGE_WIND);
                        output.accept(ModItems.MAGIC_BOOK_EARTH);
                        output.accept(ModItems.MAGIC_PAGE_EARTH);
                        output.accept(ModItems.MAGIC_BOOK_TIME_ALTER);
                        output.accept(ModItems.MAGIC_PAGE_TIME_ALTER);
                        output.accept(ModItems.MAGIC_BOOK_SPIRITUAL_HEALING);
                        output.accept(ModItems.MAGIC_PAGE_SPIRITUAL_HEALING);
                        output.accept(ModItems.MAGIC_BOOK_BAPTISM_RITE);
                        output.accept(ModItems.MAGIC_PAGE_BAPTISM_RITE);
                        output.accept(ModItems.PARTITIONED_THOUGHT_FRAGMENT);
                        output.accept(ModItems.MAGIC_BOOK_BLACK_KEY_FIRE_ENGRAVING);
                        output.accept(ModItems.MAGIC_PAGE_BLACK_KEY_FIRE_ENGRAVING);
                        output.accept(ModItems.MAGIC_BOOK_STIGMA);
                        output.accept(ModItems.MAGIC_PAGE_STIGMA);
                        output.accept(ModItems.MAGIC_BOOK_ABSORPTION);
                        output.accept(ModItems.MAGIC_PAGE_ABSORPTION);
                        output.accept(ModItems.MAGIC_BOOK_AIRFLOW_BLADE);
                        output.accept(ModItems.MAGIC_PAGE_AIRFLOW_BLADE);
                        output.accept(ModItems.MAGIC_BOOK_ANDRASIAS);
                        output.accept(ModItems.MAGIC_PAGE_ANDRASIAS);
                        output.accept(ModItems.MAGIC_BOOK_ANDREPHIUS);
                        output.accept(ModItems.MAGIC_PAGE_ANDREPHIUS);
                        output.accept(ModItems.MAGIC_BOOK_ANTORES);
                        output.accept(ModItems.MAGIC_PAGE_ANTORES);
                        output.accept(ModItems.MAGIC_BOOK_DEMON_GOD_GAZE);
                        output.accept(ModItems.MAGIC_PAGE_DEMON_GOD_GAZE);
                        output.accept(ModItems.MAGIC_BOOK_DETECTION);
                        output.accept(ModItems.MAGIC_PAGE_DETECTION);
                        output.accept(ModItems.MAGIC_BOOK_IMAGINARY_DISPLACEMENT);
                        output.accept(ModItems.MAGIC_PAGE_IMAGINARY_DISPLACEMENT);
                        output.accept(ModItems.MAGIC_BOOK_IMAGINARY_DIVE);
                        output.accept(ModItems.MAGIC_PAGE_IMAGINARY_DIVE);
                        output.accept(ModItems.MAGIC_BOOK_IMAGINARY_SPACE);
                        output.accept(ModItems.MAGIC_PAGE_IMAGINARY_SPACE);
                        output.accept(ModItems.MAGIC_BOOK_KIMARIS);
                        output.accept(ModItems.MAGIC_PAGE_KIMARIS);
                        output.accept(ModItems.MAGIC_BOOK_NEGA_SUMMON);
                        output.accept(ModItems.MAGIC_PAGE_NEGA_SUMMON);
                        output.accept(ModItems.MAGIC_BOOK_ORIAS);
                        output.accept(ModItems.MAGIC_PAGE_ORIAS);
                        output.accept(ModItems.MAGIC_BOOK_STORAGE);
                        output.accept(ModItems.MAGIC_PAGE_STORAGE);
                        output.accept(ModItems.MAGIC_BOOK_STORM);
                        output.accept(ModItems.MAGIC_PAGE_STORM);
                        output.accept(ModItems.MAGIC_BOOK_ZAGAN);
                        output.accept(ModItems.MAGIC_PAGE_ZAGAN);

                        output.accept(ModItems.MYSTIC_EYES_OF_DEATH_PERCEPTION);
                        output.accept(ModItems.MYSTIC_EYES_OF_DEATH_PERCEPTION_NOBLE_COLOR);
                        
                        output.accept(ModItems.AVALON);
                        output.accept(ModItems.GILGAMESH_BAB_ILU);
                        output.accept(ModItems.GILGAMESH_EA);
                        output.accept(ModItems.GILGAMESH_DURANDAL);
                        output.accept(ModItems.GILGAMESH_GRAM);
                        output.accept(ModItems.GILGAMESH_HARPE);
                        output.accept(ModItems.GILGAMESH_VAJRA);
                        output.accept(ModItems.GILGAMESH_FANGTIAN_HUAJI);
                        output.accept(ModItems.GILGAMESH_SPIRAL_SWORD);
                        output.accept(ModItems.GILGAMESH_GAE_BULG);
                        output.accept(ModItems.MURAMASA);
                        output.accept(ModItems.TSUMUKARI_MURAMASA);
                        output.accept(ModItems.HESHIKIRI_HASEBE);
                        output.accept(ModItems.SPIDER_CUTTER);
                        output.accept(ModItems.TEMPLE_STONE_SWORD_AXE);
                        output.accept(ModItems.EXCALIBUR);
                        output.accept(ModItems.EXCALIBUR2);
                        output.accept(ModItems.BIZEN_NAGAMITSU);
                        output.accept(ModItems.GAE_BULG);
                        output.accept(ModItems.GAE_DEARG);
                        output.accept(ModItems.GAE_BUIDHE);
                        output.accept(ModItems.YAJIAO_QIANG);
                        output.accept(ModItems.RULE_BREAKER);
                        output.accept(ModItems.HECATES_STAFF);
                        output.accept(ModItems.RUBY_STAFF);
                        output.accept(ModItems.NAMELESS_CHAIN_DAGGER);
                        output.accept(ModItems.DIRK_SMALL_KNIFE);
                        output.accept(ModItems.GAN_JIANG);
                        output.accept(ModItems.MO_YE);
                        output.accept(ModItems.GAN_JIANG_OVEREDGE);
                        output.accept(ModItems.MO_YE_OVEREDGE);
                        output.accept(ModItems.NAMELESS_BOW);
                        output.accept(ModItems.ARASH_BOW);
                        output.accept(ModItems.GILGAMESH_SLATE);
                        output.accept(ModItems.PSEUDO_SPIRAL_SWORD);
                        output.accept(ModItems.CRIMSON_HOUND);
                        output.accept(ModItems.PARACELSUS_SWORD);
                        output.accept(ModItems.MERCURY_SWORD);
                        output.accept(ModItems.THOMPSON_CONTENDER);
                        output.accept(ModItems.NIGHTINGALE_GUN);
                        output.accept(ModItems.BLACK_KEY);
                        output.accept(ModItems.EXCALIBUR_GALLATIN);
                        output.accept(ModBlocks.MURAMASA_BLOCK);

                        output.accept(ModItems.CHISEL);
                        output.accept(ModItems.MAGIC_CREST);
                        output.accept(ModBlocks.GEM_CARVING_TABLE);
                        output.accept(ModBlocks.MAGIC_RESEARCH_TABLE);
                        output.accept(ModBlocks.MAGIC_COPYING_TABLE);
                        output.accept(ModItems.MANA_SURVEY_BASE);
                        output.accept(ModItems.MANA_SURVEY_POINTER);
                        output.accept(ModItems.MANA_SURVEY_COMPASS);
                        output.accept(ModItems.COPPER_MANA_SURVEY_BASE);
                        output.accept(ModItems.COPPER_MANA_SURVEY_POINTER);
                        output.accept(ModItems.COPPER_MANA_SURVEY_COMPASS);
                        output.accept(ModItems.LEYLINE_SURVEY_MAP);
                        output.accept(ModItems.BULLET);
                        output.accept(ModItems.ORIGIN_BULLET);
                        output.accept(ModItems.KIRITSUGU_BONE_POWDER);

                        output.accept(ModItems.RAW_EMERALD);
                        output.accept(ModItems.RAW_RUBY);
                        output.accept(ModItems.RAW_SAPPHIRE);
                        output.accept(ModItems.RAW_TOPAZ);
                        output.accept(ModItems.RAW_WHITE_GEMSTONE);
                        output.accept(ModItems.RAW_CYAN_GEMSTONE);

                        output.accept(ModItems.CARVED_EMERALD_POOR);
                        output.accept(ModItems.CARVED_EMERALD);
                        output.accept(ModItems.CARVED_EMERALD_HIGH);
                        
                        output.accept(ModItems.CARVED_RUBY_POOR);
                        output.accept(ModItems.CARVED_RUBY);
                        output.accept(ModItems.CARVED_RUBY_HIGH);
                        
                        output.accept(ModItems.CARVED_SAPPHIRE_POOR);
                        output.accept(ModItems.CARVED_SAPPHIRE);
                        output.accept(ModItems.CARVED_SAPPHIRE_HIGH);
                        
                        output.accept(ModItems.CARVED_TOPAZ_POOR);
                        output.accept(ModItems.CARVED_TOPAZ);
                        output.accept(ModItems.CARVED_TOPAZ_HIGH);
                        
                        output.accept(ModItems.CARVED_WHITE_GEMSTONE_POOR);
                        output.accept(ModItems.CARVED_WHITE_GEMSTONE);
                        output.accept(ModItems.CARVED_WHITE_GEMSTONE_HIGH);
                        
                        output.accept(ModItems.CARVED_CYAN_GEMSTONE_POOR);
                        output.accept(ModItems.CARVED_CYAN_GEMSTONE);
                        output.accept(ModItems.CARVED_CYAN_GEMSTONE_HIGH);

                        output.accept(ModItems.CARVED_BLACK_SHARD_POOR);
                        output.accept(ModItems.CARVED_BLACK_SHARD);
                        output.accept(ModItems.CARVED_BLACK_SHARD_HIGH);

                        output.accept(ModItems.CARVED_EMERALD_POOR_FULL);
                        output.accept(ModItems.CARVED_EMERALD_FULL);
                        output.accept(ModItems.CARVED_EMERALD_HIGH_FULL);
                        
                        output.accept(ModItems.CARVED_RUBY_POOR_FULL);
                        output.accept(ModItems.CARVED_RUBY_FULL);
                        output.accept(ModItems.CARVED_RUBY_HIGH_FULL);
                        
                        output.accept(ModItems.CARVED_SAPPHIRE_POOR_FULL);
                        output.accept(ModItems.CARVED_SAPPHIRE_FULL);
                        output.accept(ModItems.CARVED_SAPPHIRE_HIGH_FULL);
                        
                        output.accept(ModItems.CARVED_TOPAZ_POOR_FULL);
                        output.accept(ModItems.CARVED_TOPAZ_FULL);
                        output.accept(ModItems.CARVED_TOPAZ_HIGH_FULL);
                        
                        output.accept(ModItems.CARVED_WHITE_GEMSTONE_POOR_FULL);
                        output.accept(ModItems.CARVED_WHITE_GEMSTONE_FULL);
                        output.accept(ModItems.CARVED_WHITE_GEMSTONE_HIGH_FULL);
                        
                        output.accept(ModItems.CARVED_CYAN_GEMSTONE_POOR_FULL);
                        output.accept(ModItems.CARVED_CYAN_GEMSTONE_FULL);
                        output.accept(ModItems.CARVED_CYAN_GEMSTONE_HIGH_FULL);

                        output.accept(ModItems.CARVED_BLACK_SHARD_POOR_FULL);
                        output.accept(ModItems.CARVED_BLACK_SHARD_FULL);
                        output.accept(ModItems.CARVED_BLACK_SHARD_HIGH_FULL);

                        output.accept(ModBlocks.EMERALD_BLOCK_POOR);
                        output.accept(ModBlocks.EMERALD_BLOCK);
                        output.accept(ModBlocks.EMERALD_BLOCK_HIGH);
                        output.accept(ModBlocks.EMERALD_MINE);
                        
                        output.accept(ModBlocks.RUBY_BLOCK_POOR);
                        output.accept(ModBlocks.RUBY_BLOCK);
                        output.accept(ModBlocks.RUBY_BLOCK_HIGH);
                        output.accept(ModBlocks.RUBY_MINE);
                        
                        output.accept(ModBlocks.SAPPHIRE_BLOCK_POOR);
                        output.accept(ModBlocks.SAPPHIRE_BLOCK);
                        output.accept(ModBlocks.SAPPHIRE_BLOCK_HIGH);
                        output.accept(ModBlocks.SAPPHIRE_MINE);
                        
                        output.accept(ModBlocks.TOPAZ_BLOCK_POOR);
                        output.accept(ModBlocks.TOPAZ_BLOCK);
                        output.accept(ModBlocks.TOPAZ_BLOCK_HIGH);
                        output.accept(ModBlocks.TOPAZ_MINE);
                        
                        output.accept(ModBlocks.WHITE_GEMSTONE_BLOCK_POOR);
                        output.accept(ModBlocks.WHITE_GEMSTONE_BLOCK);
                        output.accept(ModBlocks.WHITE_GEMSTONE_BLOCK_HIGH);
                        output.accept(ModBlocks.WHITE_GEMSTONE_MINE);
                        
                        output.accept(ModBlocks.CYAN_GEMSTONE_BLOCK_POOR);
                        output.accept(ModBlocks.CYAN_GEMSTONE_BLOCK);
                        output.accept(ModBlocks.CYAN_GEMSTONE_BLOCK_HIGH);
                        output.accept(ModBlocks.CYAN_GEMSTONE_MINE);
                        
                        output.accept(ModBlocks.GREEN_TRANSPARENT_BLOCK);

                        output.accept(ModBlocks.SPIRIT_VEIN_BLOCK);
                        output.accept(ModBlocks.SPIRIT_VEIN_NODE);
                        output.accept(ModBlocks.ANCIENT_TEMPLE_STONE);
                        
                        output.accept(ModItems.RYOUGI_SHIKI_SPAWN_EGG);
                        output.accept(ModItems.MERLIN_SPAWN_EGG);
                        output.accept(ModItems.STONE_MAN_SPAWN_EGG);
                        output.accept(ModItems.MYSTIC_MAGICIAN_SPAWN_EGG);
                        output.accept(ModItems.MYSTIC_MAGICIAN_GRAND_SPAWN_EGG);
                        output.accept(ModItems.MYSTIC_MAGICIAN_BRAND_SPAWN_EGG);
                        output.accept(ModItems.MYSTIC_MAGICIAN_PRIDE_SPAWN_EGG);
                        output.accept(ModItems.MYSTIC_MAGICIAN_FES_SPAWN_EGG);
                        output.accept(ModItems.MYSTIC_MAGICIAN_ADEPT_SPAWN_EGG);
                        output.accept(ModItems.MYSTIC_MAGICIAN_UMNOS_SPAWN_EGG);
                        output.accept(ModItems.MYSTIC_MAGICIAN_FRAME_SPAWN_EGG);
                        output.accept(ModItems.THE_DEAD_SPAWN_EGG);
                        output.accept(ModItems.GHOUL_SPAWN_EGG);
                        output.accept(ModItems.LIVING_DEAD_SPAWN_EGG);
                        output.accept(ModItems.NIGHT_KIN_SPAWN_EGG);
                        output.accept(ModItems.NERO_CHAOS_SPAWN_EGG);
                        output.accept(ModItems.CHURCH_EXECUTOR_SPAWN_EGG);
                        output.accept(ModItems.BAJIQUAN_MASTER_SPAWN_EGG);
                        output.accept(ModItems.BAJIQUAN_APPRENTICE_SPAWN_EGG);
                        output.accept(ModItems.MYSTERIOUS_SWORDSMAN_SPAWN_EGG);
                        output.accept(ModItems.KENDO_MASTER_SPAWN_EGG);
                        output.accept(ModItems.KENDO_MASTER_TENNEN_SPAWN_EGG);
                        output.accept(ModItems.KENDO_APPRENTICE_SPAWN_EGG);
                        output.accept(ModItems.RONIN_SPAWN_EGG);
                        output.accept(ModItems.SHINSENGUMI_SPAWN_EGG);
                        output.accept(ModItems.TOHSAKA_RIN_SPAWN_EGG);
                        output.accept(ModItems.HERACLES_SPAWN_EGG);
                        output.accept(ModItems.SASAKI_KOJIRO_SPAWN_EGG);
                        output.accept(ModItems.CU_CHULAINN_SPAWN_EGG);
                        output.accept(ModItems.DIARMUID_UA_DUIBHNE_SPAWN_EGG);
                        output.accept(ModItems.MEDEA_SPAWN_EGG);
                        output.accept(ModItems.MEDUSA_SPAWN_EGG);
                        output.accept(ModItems.CURSED_ARM_HASSAN_SPAWN_EGG);
                        output.accept(ModItems.DRAGONFANG_SOLDIER_SPAWN_EGG);
                        output.accept(ModItems.EMIYA_ARCHER_SPAWN_EGG);
                        output.accept(ModItems.ARTORIA_PENDRAGON_SPAWN_EGG);
                        output.accept(ModItems.ODA_NOBUNAGA_SPAWN_EGG);
                        output.accept(ModItems.USHIWAKAMARU_RIDER_SPAWN_EGG);
                        output.accept(ModItems.ZHAO_YUN_RIDER_SPAWN_EGG);
                        output.accept(ModItems.ENKIDU_SPAWN_EGG);
                        output.accept(ModItems.GILGAMESH_SPAWN_EGG);
                        output.accept(ModItems.GILGAMESH_CASTER_SPAWN_EGG);
                        output.accept(ModItems.GAWAIN_SPAWN_EGG);
                        output.accept(ModItems.SENKO_MURAMASA_SPAWN_EGG);
                        output.accept(ModItems.LI_SHUWEN_SPAWN_EGG);
                        output.accept(ModItems.PARACELSUS_SPAWN_EGG);
                        output.accept(ModItems.PALE_RIDER_SPAWN_EGG);
                        output.accept(ModItems.NIGHTINGALE_SPAWN_EGG);
                        output.accept(ModItems.SHADOW_HASSAN_SPAWN_EGG);
                        output.accept(ModItems.FANATIC_ASSASSIN_SPAWN_EGG);
                        output.accept(ModItems.HUNDRED_FACES_HASSAN_SPAWN_EGG);
                        output.accept(ModItems.ARASH_SPAWN_EGG);
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
