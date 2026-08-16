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
import net.xxxjk.TYPE_MOON_WORLD.entity.BucephalusEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GordiusWheelEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MacedonianSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoninEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ShinsengumiEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TohsakaRinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.LeffLaynorFlaurosEntity;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SenkoMuramasaEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ZhaoYunHakuryuEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.church.ChurchExecutorEntity;

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
      event.put(ModEntities.MYSTIC_MAGICIAN_GRAND.get(), MysticMagicianEntity.createAttributes().build());
      event.put(ModEntities.MYSTIC_MAGICIAN_BRAND.get(), MysticMagicianEntity.createAttributes().build());
      event.put(ModEntities.MYSTIC_MAGICIAN_PRIDE.get(), MysticMagicianEntity.createAttributes().build());
      event.put(ModEntities.MYSTIC_MAGICIAN_FES.get(), MysticMagicianEntity.createAttributes().build());
      event.put(ModEntities.MYSTIC_MAGICIAN_ADEPT.get(), MysticMagicianEntity.createAttributes().build());
      event.put(ModEntities.MYSTIC_MAGICIAN_UMNOS.get(), MysticMagicianEntity.createAttributes().build());
      event.put(ModEntities.MYSTIC_MAGICIAN_FRAME.get(), MysticMagicianEntity.createAttributes().build());
      event.put(ModEntities.THE_DEAD.get(), DeadApostleEntity.attributes(20.0, 3.0, 2.0, 0.23).build());
      event.put(ModEntities.GHOUL.get(), DeadApostleEntity.attributes(40.0, 6.0, 4.0, 0.24).build());
      event.put(ModEntities.LIVING_DEAD.get(), DeadApostleEntity.attributes(100.0, 18.0, 12.0, 0.32).build());
      event.put(ModEntities.NIGHT_KIN.get(), DeadApostleEntity.attributes(150.0, 24.0, 16.0, 0.36).build());
      event.put(ModEntities.NERO_CHAOS.get(), net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosEntity.createAttributes().build());
      event.put(ModEntities.NERO_CHAOS_HOUND.get(), net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosHoundEntity.createAttributes().build());
      event.put(ModEntities.NERO_CHAOS_SERPENT.get(), net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosSerpentEntity.createAttributes().build());
      event.put(ModEntities.NERO_CHAOS_STAG.get(), net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosStagEntity.createAttributes().build());
      event.put(ModEntities.NERO_CHAOS_BIRD.get(), net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosBirdEntity.createAttributes().build());
      event.put(ModEntities.NERO_CHAOS_BEAR.get(), net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosBearEntity.createAttributes().build());
      event.put(ModEntities.NERO_CHAOS_CAT.get(), net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosCatEntity.createAttributes().build());
      event.put(ModEntities.NERO_CHAOS_BAT.get(), net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NeroChaosBatEntity.createAttributes().build());
      event.put(ModEntities.CHURCH_EXECUTOR.get(), ChurchExecutorEntity.createAttributes().build());
      event.put(ModEntities.BAJIQUAN_MASTER.get(), BajiquanMasterEntity.createAttributes().build());
      event.put(ModEntities.BAJIQUAN_APPRENTICE.get(), BajiquanApprenticeEntity.createAttributes().build());
      event.put(ModEntities.MYSTERIOUS_SWORDSMAN.get(), MysteriousSwordsmanEntity.createAttributes().build());
      event.put(ModEntities.KENDO_MASTER.get(), KendoMasterEntity.createAttributes().build());
      event.put(ModEntities.KENDO_APPRENTICE.get(), KendoApprenticeEntity.createAttributes().build());
      event.put(ModEntities.RONIN.get(), KendoApprenticeEntity.createAttributes().build());
      event.put(ModEntities.SHINSENGUMI.get(), KendoApprenticeEntity.createAttributes().build());
      event.put(ModEntities.TOHSAKA_RIN.get(), TohsakaRinEntity.createAttributes().build());
      event.put(ModEntities.LEFF_LAYNOR_FLAUROS.get(), LeffLaynorFlaurosEntity.createAttributes().build());
      event.put(ModEntities.HERACLES.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.GENERIC_SERVANT.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.SASAKI_KOJIRO.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.CU_CHULAINN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.DIARMUID_UA_DUIBHNE.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.LANCELOT_BERSERKER.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.MEDEA.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.MEDUSA.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.CURSED_ARM_HASSAN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.EMIYA_ARCHER.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.SENKO_MURAMASA.get(), SenkoMuramasaEntity.createMuramasaAttributes().build());
      event.put(ModEntities.ARASH.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.ARTORIA_PENDRAGON.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.ODA_NOBUNAGA.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.USHIWAKAMARU_RIDER.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.ZHAO_YUN_RIDER.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.ISKANDAR.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.ZHAO_YUN_HAKURYU.get(), ZhaoYunHakuryuEntity.createAttributes().build());
      event.put(ModEntities.BUCEPHALUS.get(), BucephalusEntity.createAttributes().build());
      event.put(ModEntities.GORDIUS_WHEEL.get(), GordiusWheelEntity.createAttributes().build());
      event.put(ModEntities.ENKIDU.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.GILGAMESH.get(), GilgameshEntity.createAttributes().build());
      event.put(ModEntities.GILGAMESH_CASTER.get(), CasterGilgameshEntity.createAttributes().build());
      event.put(ModEntities.GAWAIN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.LI_SHUWEN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.PARACELSUS.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.PALE_RIDER.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.NIGHTINGALE.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.SHADOW_HASSAN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.SHADOW_HASSAN_DEATH_SHADOW.get(), Mob.createMobAttributes().build());
      event.put(ModEntities.FANATIC_ASSASSIN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.FANATIC_ASSASSIN_JINN.get(), net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinJinnEntity.createAttributes().build());
      event.put(ModEntities.HUNDRED_FACES_HASSAN.get(), ServantEntity.createAttributes().build());
      event.put(ModEntities.HUNDRED_FACES_HASSAN_PERSONA.get(), net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanPersonaEntity.createAttributes().build());
      event.put(ModEntities.RAT_SWARM.get(), net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity.createAttributes().build());
      event.put(ModEntities.PALE_RIDER_CROW.get(), net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity.createAttributes().build());
      event.put(ModEntities.SOUL_ECHO.get(), net.xxxjk.TYPE_MOON_WORLD.servant.entity.SoulEchoEntity.createAttributes().build());
      event.put(ModEntities.APOCALYPSE_HORSEMAN.get(), net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorsemanEntity.createAttributes().build());
      event.put(ModEntities.APOCALYPSE_HORSE.get(), net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorseEntity.createAttributes().build());
      event.put(ModEntities.MEDUSA_PEGASUS.get(), MedusaPegasusEntity.createAttributes().build());
      event.put(ModEntities.DRAGONFANG_SOLDIER.get(), DragonfangSoldierEntity.createAttributes().build());
      event.put(ModEntities.MACEDONIAN_SOLDIER.get(), MacedonianSoldierEntity.createAttributes().build());
   }

   @SubscribeEvent
   public static void registerTicketControllers(net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent event) {
      event.register(net.xxxjk.TYPE_MOON_WORLD.entity.ArashStellaControllerEntity.CHUNK_TICKETS);
   }

   @SubscribeEvent
   public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
      event.register(ModEntities.MYSTIC_MAGICIAN.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.MYSTIC_MAGICIAN_GRAND.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.MYSTIC_MAGICIAN_BRAND.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.MYSTIC_MAGICIAN_PRIDE.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.MYSTIC_MAGICIAN_FES.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.MYSTIC_MAGICIAN_ADEPT.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.MYSTIC_MAGICIAN_UMNOS.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.MYSTIC_MAGICIAN_FRAME.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.THE_DEAD.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkDeadApostleSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.GHOUL.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkDeadApostleSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.LIVING_DEAD.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkDeadApostleSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.NIGHT_KIN.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkDeadApostleSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.CHURCH_EXECUTOR.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkChurchSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
      event.register(ModEntities.RONIN.get(), SpawnPlacementTypes.ON_GROUND,
         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEventBusEvents::checkWildNpcSpawnRules,
         RegisterSpawnPlacementsEvent.Operation.REPLACE);
   }

   private static <T extends Mob> boolean checkWildNpcSpawnRules(
      EntityType<T> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
   ) {
      return level.getDifficulty() != Difficulty.PEACEFUL
         && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
   }

   private static <T extends Mob> boolean checkDeadApostleSpawnRules(
      EntityType<T> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
   ) {
      boolean city = level.getBiome(pos).is(net.minecraft.resources.ResourceKey.create(
         net.minecraft.core.registries.Registries.BIOME,
         net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld", "city")));
      return level.getDifficulty() != Difficulty.PEACEFUL
         && (type != ModEntities.NIGHT_KIN.get() || city || random.nextInt(4) == 0)
         && (MobSpawnType.ignoresLightRequirements(spawnType)
            || net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(level, pos, random))
         && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
   }

   private static <T extends Mob> boolean checkChurchSpawnRules(
      EntityType<T> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
   ) {
      return level.getDifficulty() != Difficulty.PEACEFUL
         && (MobSpawnType.ignoresLightRequirements(spawnType)
            || net.minecraft.world.entity.monster.Monster.isDarkEnoughToSpawn(level, pos, random))
         && Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
   }
}
