package net.xxxjk.TYPE_MOON_WORLD.loot;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.common.loot.*;

/** Adds chest rewards; replaces the one archaeology draw rather than creating a second brush result. */
public final class SurvivalLootModifier extends LootModifier {
   public static final MapCodec<SurvivalLootModifier> CODEC=RecordCodecBuilder.mapCodec(i->codecStart(i).and(i.group(
      BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m->m.item),
      Codec.floatRange(0,1).fieldOf("chance").forGetter(m->m.chance),
      Codec.BOOL.optionalFieldOf("archaeology",false).forGetter(m->m.archaeology),
      Codec.BOOL.optionalFieldOf("zombie_village",false).forGetter(m->m.zombieVillage)
   )).apply(i,SurvivalLootModifier::new));
   private final Item item;private final float chance;private final boolean archaeology,zombieVillage;
   public SurvivalLootModifier(LootItemCondition[] conditions,Item item,float chance,boolean archaeology,boolean zombieVillage){super(conditions);this.item=item;this.chance=chance;this.archaeology=archaeology;this.zombieVillage=zombieVillage;}
   @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
      if(zombieVillage){
         var origin=context.getParamOrNull(LootContextParams.ORIGIN);if(origin==null)return loot;
         var start=context.getLevel().structureManager().getStructureWithPieceAt(BlockPos.containing(origin),StructureTags.VILLAGE);
         if(!start.isValid())return loot;
         var serialization=StructurePieceSerializationContext.fromLevel(context.getLevel());
         boolean zombie=start.getPieces().stream().anyMatch(p->p.createTag(serialization).getCompound("pool_element").getString("location").contains("/zombie/"));
         if(!zombie)return loot;
      }
      if(context.getRandom().nextFloat()<chance){if(archaeology)loot.clear();loot.add(new ItemStack(item));}return loot;
   }
   @Override public MapCodec<? extends IGlobalLootModifier> codec(){return CODEC;}
}
