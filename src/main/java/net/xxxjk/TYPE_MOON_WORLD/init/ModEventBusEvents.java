package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.StoneManEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TohsakaRinEntity;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

@EventBusSubscriber(
   modid = "typemoonworld",
   bus = Bus.MOD
)
public class ModEventBusEvents {
   @SubscribeEvent
   public static void registerAttributes(EntityAttributeCreationEvent event) {
      event.put(ModEntities.RYOUGI_SHIKI.get(), RyougiShikiEntity.createAttributes().build());
      event.put(ModEntities.MERLIN.get(), MerlinEntity.createAttributes().build());
      event.put(ModEntities.STONE_MAN.get(), StoneManEntity.createAttributes().build());
      event.put(ModEntities.MYSTIC_MAGICIAN.get(), MysticMagicianEntity.createAttributes().build());
      event.put(ModEntities.BAJIQUAN_MASTER.get(), BajiquanMasterEntity.createAttributes().build());
      event.put(ModEntities.BAJIQUAN_APPRENTICE.get(), BajiquanApprenticeEntity.createAttributes().build());
      event.put(ModEntities.TOHSAKA_RIN.get(), TohsakaRinEntity.createAttributes().build());
      event.put(ModEntities.HERACLES.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.SASAKI_KOJIRO.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.CU_CHULAINN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.MEDEA.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.MEDUSA.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.CURSED_ARM_HASSAN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.EMIYA_ARCHER.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.ARTORIA_PENDRAGON.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.ODA_NOBUNAGA.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.ENKIDU.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.GILGAMESH.get(), GilgameshEntity.createAttributes().build());
      event.put(ModEntities.GAWAIN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.LI_SHUWEN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.PARACELSUS.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.MEDUSA_PEGASUS.get(), MedusaPegasusEntity.createAttributes().build());
      event.put(ModEntities.DRAGONFANG_SOLDIER.get(), DragonfangSoldierEntity.createAttributes().build());
   }

   @SubscribeEvent
   public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
      event.register(ModEntities.MYSTIC_MAGICIAN.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.BAJIQUAN_APPRENTICE.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
   }

   private static <T extends Mob> boolean checkWildNpcSpawnRules(
      EntityType<T> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
   ) {
      return level.getDifficulty() != Difficulty.PEACEFUL
         && (MobSpawnType.ignoresLightRequirements(spawnType) || Monster.isDarkEnoughToSpawn(level, pos, random))
         && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
   }
}
