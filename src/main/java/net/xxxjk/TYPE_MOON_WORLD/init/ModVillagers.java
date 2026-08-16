package net.xxxjk.TYPE_MOON_WORLD.init;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;

public final class ModVillagers {
   public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, TYPE_MOON_WORLD.MOD_ID);
   public static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, TYPE_MOON_WORLD.MOD_ID);

   public static final ResourceKey<PoiType> MAGICIAN_POI_KEY = ResourceKey.create(
      Registries.POINT_OF_INTEREST_TYPE,
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "magician")
   );

   public static final DeferredHolder<PoiType, PoiType> MAGICIAN_POI = POI_TYPES.register(
      "magician",
      () -> new PoiType(blockStates(ModBlocks.MAGIC_RESEARCH_TABLE.get()), 1, 1)
   );

   public static final DeferredHolder<VillagerProfession, VillagerProfession> MAGICIAN = PROFESSIONS.register(
      "magician",
      () -> new VillagerProfession(
         "magician",
         holder -> holder.is(MAGICIAN_POI_KEY),
         holder -> holder.is(MAGICIAN_POI_KEY),
         ImmutableSet.of(),
         ImmutableSet.of(),
         SoundEvents.VILLAGER_WORK_LIBRARIAN
      )
   );

   private ModVillagers() {
   }

   private static Set<BlockState> blockStates(Block block) {
      return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
   }

   public static void register(IEventBus eventBus) {
      POI_TYPES.register(eventBus);
      PROFESSIONS.register(eventBus);
   }
}
