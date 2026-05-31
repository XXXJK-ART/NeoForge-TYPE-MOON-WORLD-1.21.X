package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;

public final class MedeaWorkshopHelper {
   public static final String TAG_INSIDE_WORKSHOP = "MedeaInsideWorkshop";
   public static final String TAG_WORKSHOP_TYPE = "MedeaWorkshopType";
   public static final String TAG_CENTER_X = "MedeaWorkshopCenterX";
   public static final String TAG_CENTER_Y = "MedeaWorkshopCenterY";
   public static final String TAG_CENTER_Z = "MedeaWorkshopCenterZ";
   public static final String TAG_RADIUS = "MedeaWorkshopRadius";
   public static final String TAG_MIN_X = "MedeaWorkshopMinX";
   public static final String TAG_MIN_Y = "MedeaWorkshopMinY";
   public static final String TAG_MIN_Z = "MedeaWorkshopMinZ";
   public static final String TAG_MAX_X = "MedeaWorkshopMaxX";
   public static final String TAG_MAX_Y = "MedeaWorkshopMaxY";
   public static final String TAG_MAX_Z = "MedeaWorkshopMaxZ";
   public static final String TAG_DRAGONFANG_STOCK = "MedeaDragonfangStock";
   public static final String TAG_MANA_CHARM_STOCK = "MedeaManaCharmStock";
   public static final String TAG_HEAL_CHARM_STOCK = "MedeaHealCharmStock";
   public static final String TAG_LAST_CRAFT_TICK = "MedeaLastCraftTick";
   public static final String TAG_LAST_SUMMON_TICK = "MedeaLastSummonTick";
   public static final String TAG_LAST_TELEPORT_TICK = "MedeaLastTeleportTick";
   public static final String TAG_WORKSHOP_DAMAGE_BUFF = "MedeaWorkshopDamageBuff";
   public static final String TAG_STARTING_STOCKS_READY = "MedeaStartingStocksReady";
   public static final String TAG_MAGIC_SUMMON = "magic_summon";
   public static final String TAG_MAGIC_SUMMON_OWNER = "magic_summon_owner";
   public static final int WORKSHOP_NONE = 0;
   public static final int WORKSHOP_SPHERE = 1;
   public static final int WORKSHOP_STRUCTURE = 2;
   public static final int SIMPLE_WORKSHOP_RADIUS = 15;
   public static final int MAX_DRAGONFANG_STOCK = 50;
   public static final int MAX_MANA_CHARM_STOCK = 10;
   public static final int MAX_HEAL_CHARM_STOCK = 10;
   public static final int TARGET_DRAGONFANG_STOCK = 20;
   public static final int TARGET_MANA_CHARM_STOCK = 3;
   public static final int TARGET_HEAL_CHARM_STOCK = 3;
   public static final int MAX_ACTIVE_DRAGONFANG = 20;
   private static final ResourceLocation WORKSHOP_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "medea_workshop_speed");
   private static final List<ResourceKey<Structure>> STRUCTURE_SEARCH_KEYS = List.of(
      BuiltinStructures.PILLAGER_OUTPOST,
      BuiltinStructures.JUNGLE_TEMPLE,
      BuiltinStructures.DESERT_PYRAMID,
      BuiltinStructures.IGLOO,
      BuiltinStructures.SWAMP_HUT,
      BuiltinStructures.VILLAGE_PLAINS,
      BuiltinStructures.VILLAGE_DESERT,
      BuiltinStructures.VILLAGE_SAVANNA,
      BuiltinStructures.VILLAGE_SNOWY,
      BuiltinStructures.VILLAGE_TAIGA
   );

   private MedeaWorkshopHelper() {
   }

   public static void tickEnvironment(MedeaEntity entity) {
      if (!(entity.level() instanceof ServerLevel serverLevel) || !entity.isAlive()) {
         return;
      }

      if (!hasWorkshop(entity)) {
         establishWorkshop(entity, serverLevel);
      }

      boolean inside = refreshWorkshopState(entity);
      applyWorkshopSpeedModifier(entity, inside);
      regenerateMana(entity, inside);
      recallFarDragonfangs(entity, serverLevel);

      long now = serverLevel.getGameTime();
      if (isOutOfCombat(entity, now)) {
         recallOwnedDragonfangs(entity, serverLevel);
         maintainOutOfCombatWorkshop(entity, serverLevel);
      }
   }

   public static boolean hasWorkshop(MedeaEntity entity) {
      return entity.getPersistentData().getInt(TAG_WORKSHOP_TYPE) != WORKSHOP_NONE;
   }

   public static boolean refreshWorkshopState(MedeaEntity entity) {
      boolean inside = isInsideWorkshop(entity, entity.position());
      entity.getPersistentData().putBoolean(TAG_INSIDE_WORKSHOP, inside);
      double maxMp = entity.getMaxMp();
      if (entity.getCurrentMp() > maxMp) {
         entity.setCurrentMp(maxMp);
      }
      return inside;
   }

   public static boolean isInsideWorkshop(MedeaEntity entity, Vec3 position) {
      int type = entity.getPersistentData().getInt(TAG_WORKSHOP_TYPE);
      if (type == WORKSHOP_NONE) {
         return false;
      }
      if (type == WORKSHOP_SPHERE) {
         Vec3 center = getWorkshopCenter(entity);
         double radius = entity.getPersistentData().getDouble(TAG_RADIUS);
         return center.distanceToSqr(position) <= radius * radius;
      }
      if (type == WORKSHOP_STRUCTURE) {
         AABB bounds = getWorkshopBounds(entity);
         return bounds != null && bounds.inflate(1.0).contains(position);
      }
      return false;
   }

   public static Vec3 getWorkshopCenter(MedeaEntity entity) {
      return new Vec3(
         entity.getPersistentData().getDouble(TAG_CENTER_X),
         entity.getPersistentData().getDouble(TAG_CENTER_Y),
         entity.getPersistentData().getDouble(TAG_CENTER_Z)
      );
   }

   public static AABB getWorkshopBounds(MedeaEntity entity) {
      if (entity.getPersistentData().getInt(TAG_WORKSHOP_TYPE) != WORKSHOP_STRUCTURE) {
         return null;
      }
      return new AABB(
         entity.getPersistentData().getInt(TAG_MIN_X),
         entity.getPersistentData().getInt(TAG_MIN_Y),
         entity.getPersistentData().getInt(TAG_MIN_Z),
         entity.getPersistentData().getInt(TAG_MAX_X) + 1.0,
         entity.getPersistentData().getInt(TAG_MAX_Y) + 3.0,
         entity.getPersistentData().getInt(TAG_MAX_Z) + 1.0
      );
   }

   public static float applyWorkshopDamageBonus(MedeaEntity entity, float baseDamage) {
      return isInsideWorkshop(entity, entity.position()) ? baseDamage * 1.3F : baseDamage;
   }

   public static double adjustedManaCost(MedeaEntity entity, double baseCost) {
      return isInsideWorkshop(entity, entity.position()) ? baseCost * 0.8 : baseCost;
   }

   public static boolean consumeMana(MedeaEntity entity, double baseCost) {
      double actualCost = adjustedManaCost(entity, baseCost);
      if (entity.getCurrentMp() + 1.0E-4 < actualCost) {
         return false;
      }
      entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - actualCost));
      return true;
   }

   public static boolean canTeleportNow(MedeaEntity entity, long now) {
      if (entity.isInsideWorkshop()) {
         return true;
      }
      long lastTeleport = entity.getPersistentData().getLong(TAG_LAST_TELEPORT_TICK);
      return now - lastTeleport >= 300L;
   }

   public static void markTeleportUsed(MedeaEntity entity, long now) {
      entity.getPersistentData().putLong(TAG_LAST_TELEPORT_TICK, now);
   }

   public static int getDragonfangStock(MedeaEntity entity) {
      return entity.getPersistentData().getInt(TAG_DRAGONFANG_STOCK);
   }

   public static void initializeStartingStocks(MedeaEntity entity) {
      if (entity.getPersistentData().getBoolean(TAG_STARTING_STOCKS_READY)) {
         return;
      }
      setDragonfangStock(entity, MAX_DRAGONFANG_STOCK / 2);
      setManaCharmStock(entity, MAX_MANA_CHARM_STOCK / 2);
      setHealCharmStock(entity, MAX_HEAL_CHARM_STOCK / 2);
      entity.getPersistentData().putBoolean(TAG_STARTING_STOCKS_READY, true);
   }

   public static void setDragonfangStock(MedeaEntity entity, int value) {
      entity.getPersistentData().putInt(TAG_DRAGONFANG_STOCK, Mth.clamp(value, 0, MAX_DRAGONFANG_STOCK));
   }

   public static int getManaCharmStock(MedeaEntity entity) {
      return entity.getPersistentData().getInt(TAG_MANA_CHARM_STOCK);
   }

   public static void setManaCharmStock(MedeaEntity entity, int value) {
      entity.getPersistentData().putInt(TAG_MANA_CHARM_STOCK, Mth.clamp(value, 0, MAX_MANA_CHARM_STOCK));
   }

   public static int getHealCharmStock(MedeaEntity entity) {
      return entity.getPersistentData().getInt(TAG_HEAL_CHARM_STOCK);
   }

   public static void setHealCharmStock(MedeaEntity entity, int value) {
      entity.getPersistentData().putInt(TAG_HEAL_CHARM_STOCK, Mth.clamp(value, 0, MAX_HEAL_CHARM_STOCK));
   }

   public static int countOwnedDragonfangs(ServerLevel level, UUID ownerUuid, Vec3 around) {
      AABB searchBox = new AABB(around, around).inflate(160.0);
      return level.getEntitiesOfClass(
         DragonfangSoldierEntity.class,
         searchBox,
         dragonfang -> dragonfang.isAlive() && ownerUuid.equals(dragonfang.getSummonerUuid())
      ).size();
   }

   public static boolean summonDragonfang(MedeaEntity entity, ServerLevel level) {
      if (getDragonfangStock(entity) <= 0) {
         return false;
      }
      int active = countOwnedDragonfangs(level, entity.getUUID(), entity.position());
      if (active >= MAX_ACTIVE_DRAGONFANG) {
         return false;
      }

      BlockPos spawnPos = findNearbySafePosition(level, entity.blockPosition(), 6);
      DragonfangSoldierEntity summon = net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.DRAGONFANG_SOLDIER.get().create(level);
      if (summon == null) {
         return false;
      }
      summon.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, entity.getYRot(), 0.0F);
      summon.setSummoner(entity);
      level.addFreshEntity(summon);
      setDragonfangStock(entity, getDragonfangStock(entity) - 1);
      spawnDragonfangSummonFx(level, spawnPos);
      return true;
   }

   public static void reclaimDragonfang(MedeaEntity owner, DragonfangSoldierEntity summon) {
      if (owner == null || summon == null || !summon.isAlive()) {
         return;
      }
      if (owner.level() == summon.level()) {
         setDragonfangStock(owner, getDragonfangStock(owner) + 1);
      }
      summon.discard();
   }

   public static BlockPos findTeleportPosition(MedeaEntity entity) {
      if (!(entity.level() instanceof ServerLevel serverLevel)) {
         return entity.blockPosition();
      }
      if (entity.getPersistentData().getInt(TAG_WORKSHOP_TYPE) == WORKSHOP_STRUCTURE) {
         AABB box = getWorkshopBounds(entity);
         if (box != null) {
            BlockPos center = BlockPos.containing(box.getCenter());
            return findNearbySafePosition(serverLevel, center, 8);
         }
      }
      return findNearbySafePosition(serverLevel, BlockPos.containing(getWorkshopCenter(entity)), SIMPLE_WORKSHOP_RADIUS);
   }

   public static BlockPos findSafeWorkshopEscapePosition(MedeaEntity entity, ServerLevel level, LivingEntity threat, double enemyClearRadius) {
      if (!hasWorkshop(entity)) {
         return findTeleportPosition(entity);
      }

      Vec3 current = entity.position();
      Vec3 threatPos = threat != null ? threat.position() : current;
      BlockPos best = null;
      double bestScore = Double.NEGATIVE_INFINITY;

      for (int i = 0; i < 32; i++) {
         BlockPos candidate = randomWorkshopCandidate(entity, level);
         if (candidate == null || !isSafeFeetPosition(level, candidate)) {
            continue;
         }
         Vec3 center = new Vec3(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
         if (!isInsideWorkshop(entity, center)) {
            continue;
         }

         AABB clearBox = new AABB(center, center).inflate(enemyClearRadius, 2.5, enemyClearRadius);
         boolean hasEnemy = !level.getEntitiesOfClass(
            LivingEntity.class,
            clearBox,
            enemy -> enemy != entity
               && enemy.isAlive()
               && !enemy.isAlliedTo(entity)
               && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(enemy)
         ).isEmpty();
         if (hasEnemy) {
            continue;
         }

         double score = center.distanceToSqr(threatPos) + center.distanceToSqr(current) * 0.35;
         if (score > bestScore) {
            bestScore = score;
            best = candidate;
         }
      }

      return best != null ? best : findTeleportPosition(entity);
   }

   private static void establishWorkshop(MedeaEntity entity, ServerLevel level) {
      Optional<WorkshopSite> structureSite = findNearbyStructureSite(level, entity.blockPosition(), 96);
      if (structureSite.isPresent()) {
         applyStructureWorkshop(entity, structureSite.get());
         return;
      }

      BlockPos highPoint = findHighestSafePoint(level, entity.blockPosition(), 32);
      if (highPoint != null && highPoint.getY() > 70) {
         applySphereWorkshop(entity, highPoint);
         return;
      }

      applySphereWorkshop(entity, entity.blockPosition());
   }

   private static Optional<WorkshopSite> findNearbyStructureSite(ServerLevel level, BlockPos origin, int radius) {
      var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
      StructureStart best = null;
      double bestDistance = Double.MAX_VALUE;

      for (ResourceKey<Structure> key : STRUCTURE_SEARCH_KEYS) {
         Structure structure = registry.getOrThrow(key).value();
         if (structure == null) {
            continue;
         }
         for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
               int x = origin.getX() + dx;
               int z = origin.getZ() + dz;
               int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
               BlockPos sample = new BlockPos(x, y, z);
               StructureStart start = level.structureManager().getStructureWithPieceAt(sample, structure);
               if (start == null || !start.isValid()) {
                  continue;
               }
               BoundingBox box = start.getBoundingBox();
               Vec3 center = new Vec3((box.minX() + box.maxX()) * 0.5, (box.minY() + box.maxY()) * 0.5, (box.minZ() + box.maxZ()) * 0.5);
               double distance = center.distanceToSqr(origin.getX(), origin.getY(), origin.getZ());
               if (distance < bestDistance) {
                  bestDistance = distance;
                  best = start;
               }
            }
         }
      }

      if (best == null || !best.isValid()) {
         return Optional.empty();
      }
      return Optional.of(new WorkshopSite(best.getBoundingBox()));
   }

   private static BlockPos findHighestSafePoint(ServerLevel level, BlockPos origin, int radius) {
      BlockPos best = null;
      int bestY = Integer.MIN_VALUE;
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (y > bestY && isSafeFeetPosition(level, pos)) {
               best = pos;
               bestY = y;
            }
         }
      }
      return best;
   }

   private static void applySphereWorkshop(MedeaEntity entity, BlockPos center) {
      entity.getPersistentData().putInt(TAG_WORKSHOP_TYPE, WORKSHOP_SPHERE);
      entity.getPersistentData().putDouble(TAG_CENTER_X, center.getX() + 0.5);
      entity.getPersistentData().putDouble(TAG_CENTER_Y, center.getY());
      entity.getPersistentData().putDouble(TAG_CENTER_Z, center.getZ() + 0.5);
      entity.getPersistentData().putDouble(TAG_RADIUS, SIMPLE_WORKSHOP_RADIUS);
   }

   private static void applyStructureWorkshop(MedeaEntity entity, WorkshopSite site) {
      BoundingBox box = site.bounds();
      entity.getPersistentData().putInt(TAG_WORKSHOP_TYPE, WORKSHOP_STRUCTURE);
      entity.getPersistentData().putInt(TAG_MIN_X, box.minX());
      entity.getPersistentData().putInt(TAG_MIN_Y, box.minY());
      entity.getPersistentData().putInt(TAG_MIN_Z, box.minZ());
      entity.getPersistentData().putInt(TAG_MAX_X, box.maxX());
      entity.getPersistentData().putInt(TAG_MAX_Y, box.maxY());
      entity.getPersistentData().putInt(TAG_MAX_Z, box.maxZ());
      entity.getPersistentData().putDouble(TAG_CENTER_X, (box.minX() + box.maxX()) * 0.5 + 0.5);
      entity.getPersistentData().putDouble(TAG_CENTER_Y, box.minY());
      entity.getPersistentData().putDouble(TAG_CENTER_Z, (box.minZ() + box.maxZ()) * 0.5 + 0.5);
      entity.getPersistentData().putDouble(TAG_RADIUS, SIMPLE_WORKSHOP_RADIUS);
   }

   private static BlockPos randomWorkshopCandidate(MedeaEntity entity, ServerLevel level) {
      int type = entity.getPersistentData().getInt(TAG_WORKSHOP_TYPE);
      if (type == WORKSHOP_STRUCTURE) {
         AABB bounds = getWorkshopBounds(entity);
         if (bounds == null) {
            return null;
         }
         int minX = Mth.floor(bounds.minX);
         int maxX = Mth.floor(bounds.maxX - 1.0);
         int minZ = Mth.floor(bounds.minZ);
         int maxZ = Mth.floor(bounds.maxZ - 1.0);
         int x = Mth.nextInt(level.random, minX, maxX);
         int z = Mth.nextInt(level.random, minZ, maxZ);
         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         return new BlockPos(x, y, z);
      }

      Vec3 center = getWorkshopCenter(entity);
      double radius = Math.max(5.0, entity.getPersistentData().getDouble(TAG_RADIUS) * 0.85);
      double angle = level.random.nextDouble() * Math.PI * 2.0;
      double dist = radius * Math.sqrt(level.random.nextDouble());
      int x = Mth.floor(center.x + Math.cos(angle) * dist);
      int z = Mth.floor(center.z + Math.sin(angle) * dist);
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      return new BlockPos(x, y, z);
   }

   private static void applyWorkshopSpeedModifier(MedeaEntity entity, boolean inside) {
      var speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed == null) {
         return;
      }
      if (inside) {
         if (speed.getModifier(WORKSHOP_SPEED_MODIFIER_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(WORKSHOP_SPEED_MODIFIER_ID, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      } else {
         speed.removeModifier(WORKSHOP_SPEED_MODIFIER_ID);
      }
   }

   private static void regenerateMana(MedeaEntity entity, boolean inside) {
      if (entity.tickCount % 10 != 0) {
         return;
      }
      double regen = inside ? 5.0 : 1.0;
      entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + regen));
   }

   private static void maintainOutOfCombatWorkshop(MedeaEntity entity, ServerLevel level) {
      long now = level.getGameTime();
      if (now - entity.getPersistentData().getLong(TAG_LAST_CRAFT_TICK) >= 20L) {
         if (entity.getCurrentMp() < 140.0) {
            return;
         }
         entity.getPersistentData().putLong(TAG_LAST_CRAFT_TICK, now);
         if (getDragonfangStock(entity) < TARGET_DRAGONFANG_STOCK && consumeMana(entity, 30.0)) {
            setDragonfangStock(entity, getDragonfangStock(entity) + 1);
         } else if (getManaCharmStock(entity) < TARGET_MANA_CHARM_STOCK && consumeMana(entity, 30.0)) {
            setManaCharmStock(entity, getManaCharmStock(entity) + 1);
         } else if (getHealCharmStock(entity) < TARGET_HEAL_CHARM_STOCK && consumeMana(entity, 30.0)) {
            setHealCharmStock(entity, getHealCharmStock(entity) + 1);
         }
      }

      if (entity.getCurrentMp() >= 160.0
         && getDragonfangStock(entity) > 0
         && now - entity.getPersistentData().getLong(TAG_LAST_SUMMON_TICK) >= 15L
         && hasHostileInRange(entity, 18.0)) {
         if (summonDragonfang(entity, level)) {
            entity.getPersistentData().putLong(TAG_LAST_SUMMON_TICK, now);
         }
      }
   }

   private static boolean isOutOfCombat(MedeaEntity entity, long now) {
      if (entity.getTarget() != null && entity.getTarget().isAlive()) {
         return false;
      }
      long lastCombat = entity.getPersistentData().getLong(MedeaCombatHelper.TAG_LAST_COMBAT_ACTIVITY_TICK);
      return lastCombat <= 0L || now - lastCombat > 80L;
   }

   private static void recallOwnedDragonfangs(MedeaEntity entity, ServerLevel level) {
      AABB searchBox = new AABB(entity.position(), entity.position()).inflate(192.0);
      int recalled = 0;
      for (DragonfangSoldierEntity dragonfang : level.getEntitiesOfClass(
         DragonfangSoldierEntity.class,
         searchBox,
         summon -> summon.isAlive() && entity.getUUID().equals(summon.getSummonerUuid())
      )) {
         dragonfang.discard();
         recalled++;
      }
      if (recalled > 0) {
         setDragonfangStock(entity, getDragonfangStock(entity) + recalled);
      }
   }

   private static void recallFarDragonfangs(MedeaEntity entity, ServerLevel level) {
      AABB searchBox = new AABB(entity.position(), entity.position()).inflate(256.0);
      int recalled = 0;
      for (DragonfangSoldierEntity dragonfang : level.getEntitiesOfClass(
         DragonfangSoldierEntity.class,
         searchBox,
         summon -> summon.isAlive() && entity.getUUID().equals(summon.getSummonerUuid()) && summon.distanceToSqr(entity) > 48.0 * 48.0
      )) {
         dragonfang.discard();
         recalled++;
      }
      if (recalled > 0) {
         setDragonfangStock(entity, getDragonfangStock(entity) + recalled);
      }
   }

   private static boolean isSafeFeetPosition(ServerLevel level, BlockPos feetPos) {
      BlockPos below = feetPos.below();
      return level.getBlockState(below).isSolidRender(level, below)
         && level.getBlockState(feetPos).getCollisionShape(level, feetPos).isEmpty()
         && level.getBlockState(feetPos.above()).getCollisionShape(level, feetPos.above()).isEmpty();
   }

   private static BlockPos findNearbySafePosition(ServerLevel level, BlockPos center, int radius) {
      List<BlockPos> candidates = new ArrayList<>();
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (isSafeFeetPosition(level, pos)) {
               candidates.add(pos);
            }
         }
      }
      if (candidates.isEmpty()) {
         return center;
      }
      return candidates.get(level.random.nextInt(candidates.size()));
   }

   private static boolean hasHostileInRange(MedeaEntity entity, double radius) {
      return entity.level().getEntitiesOfClass(
         LivingEntity.class,
         entity.getBoundingBox().inflate(radius, 5.0, radius),
         other -> other != entity
            && other.isAlive()
            && !other.isAlliedTo(entity)
            && !net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils.isImmunePlayerTarget(other)
      ).stream().findAny().isPresent();
   }

   private static void spawnDragonfangSummonFx(ServerLevel level, BlockPos pos) {
      Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 12, 0.28, 0.1, 0.28, 0.01);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, center.x, center.y + 0.2, center.z, 18, 0.4, 0.25, 0.4, 0.02);
   }

   private record WorkshopSite(BoundingBox bounds) {
   }
}
