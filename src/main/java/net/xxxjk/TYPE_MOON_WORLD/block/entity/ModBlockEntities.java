package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;

public class ModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "typemoonworld");
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MuramasaBlockEntity>> MURAMASA_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "redswordblock", () -> Builder.of(MuramasaBlockEntity::new, new Block[]{(Block)ModBlocks.MURAMASA_BLOCK.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UBWWeaponBlockEntity>> UBW_WEAPON_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "ubw_weapon_block_entity", () -> Builder.of(UBWWeaponBlockEntity::new, new Block[]{(Block)ModBlocks.UBW_WEAPON_BLOCK.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SwordBarrelBlockEntity>> SWORD_BARREL_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "sword_barrel_block_entity", () -> Builder.of(SwordBarrelBlockEntity::new, new Block[]{(Block)ModBlocks.SWORD_BARREL_BLOCK.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GemCarvingTableBlockEntity>> GEM_CARVING_TABLE_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "gem_carving_table_block_entity", () -> Builder.of(GemCarvingTableBlockEntity::new, new Block[]{(Block)ModBlocks.GEM_CARVING_TABLE.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagicResearchTableBlockEntity>> MAGIC_RESEARCH_TABLE = BLOCK_ENTITIES.register(
      "magic_research_table", () -> Builder.of(MagicResearchTableBlockEntity::new, ModBlocks.MAGIC_RESEARCH_TABLE.get()).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagicCopyingTableBlockEntity>> MAGIC_COPYING_TABLE = BLOCK_ENTITIES.register(
      "magic_copying_table", () -> Builder.of(MagicCopyingTableBlockEntity::new, ModBlocks.MAGIC_COPYING_TABLE.get()).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArtificialLeylineBlockEntity>> ARTIFICIAL_LEYLINE_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "artificial_leyline_block_entity", () -> Builder.of(ArtificialLeylineBlockEntity::new, new Block[]{(Block)ModBlocks.ARTIFICIAL_LEYLINE_BLOCK.get()}).build(null)
   );
   public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RuneBlockEntity>> RUNE_BLOCK_ENTITY = BLOCK_ENTITIES.register(
      "rune_block_entity", () -> Builder.of(RuneBlockEntity::new, ModBlocks.RUNE_INSCRIPTION.get()).build(null)
   );

   public static void register(IEventBus eventBus) {
      BLOCK_ENTITIES.register(eventBus);
   }
}
