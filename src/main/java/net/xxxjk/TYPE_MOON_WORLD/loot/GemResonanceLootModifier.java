package net.xxxjk.TYPE_MOON_WORLD.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.event.GemTerrainEventHandler;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.util.ModTags;

public class GemResonanceLootModifier extends LootModifier {
   public static final MapCodec<GemResonanceLootModifier> CODEC = RecordCodecBuilder.mapCodec(
      inst -> LootModifier.codecStart(inst).apply(inst, GemResonanceLootModifier::new)
   );

   public GemResonanceLootModifier(LootItemCondition[] conditions) {
      super(conditions);
   }

   protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      ServerLevel blockState = context.getLevel();
      if (blockState instanceof ServerLevel) {
         BlockState blockStatex = (BlockState)context.getParamOrNull(LootContextParams.BLOCK_STATE);
         if (blockStatex != null && blockStatex.is(ModTags.Blocks.GEM_ORES)) {
            Entity entity = (Entity)context.getParamOrNull(LootContextParams.THIS_ENTITY);
            if (entity instanceof ServerPlayer player) {
               if (!GemTerrainEventHandler.isResonanceActive(blockState, player.blockPosition())) {
                  return generatedLoot;
               } else if (context.getRandom().nextDouble() >= Config.gemBonusDropChance) {
                  return generatedLoot;
               } else {
                  ItemStack bonusDrop = this.getBonusDrop(blockStatex.getBlock());
                  if (bonusDrop.isEmpty()) {
                     return generatedLoot;
                  } else {
                     ItemStack brokenBlockAsItem = new ItemStack(blockStatex.getBlock().asItem());
                     ObjectListIterator var9 = generatedLoot.iterator();

                     while (var9.hasNext()) {
                        ItemStack loot = (ItemStack)var9.next();
                        if (ItemStack.isSameItemSameComponents(loot, brokenBlockAsItem)) {
                           return generatedLoot;
                        }
                     }

                     generatedLoot.add(bonusDrop);
                     return generatedLoot;
                  }
               }
            } else {
               return generatedLoot;
            }
         } else {
            return generatedLoot;
         }
      } else {
         return generatedLoot;
      }
   }

   private ItemStack getBonusDrop(Block block) {
      if (block == ModBlocks.EMERALD_MINE.get()) {
         return new ItemStack((ItemLike)ModItems.RAW_EMERALD.get());
      } else if (block == ModBlocks.RUBY_MINE.get()) {
         return new ItemStack((ItemLike)ModItems.RAW_RUBY.get());
      } else if (block == ModBlocks.SAPPHIRE_MINE.get()) {
         return new ItemStack((ItemLike)ModItems.RAW_SAPPHIRE.get());
      } else if (block == ModBlocks.TOPAZ_MINE.get()) {
         return new ItemStack((ItemLike)ModItems.RAW_TOPAZ.get());
      } else if (block == ModBlocks.WHITE_GEMSTONE_MINE.get()) {
         return new ItemStack((ItemLike)ModItems.RAW_WHITE_GEMSTONE.get());
      } else {
         return block == ModBlocks.CYAN_GEMSTONE_MINE.get() ? new ItemStack((ItemLike)ModItems.RAW_CYAN_GEMSTONE.get()) : ItemStack.EMPTY;
      }
   }

   public MapCodec<? extends IGlobalLootModifier> codec() {
      return CODEC;
   }
}
