package net.xxxjk.TYPE_MOON_WORLD.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.entity.HumanNpcEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.leyline.LeylineService;

/**
 * The recoverable failure and exposure rules for an ordinary magus.
 *
 * <p>The state is kept in the player's persistent data instead of adding a
 * second attachment.  This keeps old worlds readable while still making the
 * values survive death/respawn and server restarts.</p>
 */
public final class MagecraftRiskService {
   public static final String DISORDER_TAG = "TypeMoonCircuitDisorder";
   public static final String DISRUPTION_TICKS_TAG = "TypeMoonCircuitDisruptionTicks";
   public static final String EXPOSURE_COOLDOWN_TAG = "TypeMoonMagecraftExposureCooldown";
   public static final String PURSUER_OWNER_TAG = "TypeMoonMagecraftPursuerOwner";
   public static final double MAX_DISORDER = 100.0D;
   private static final double DISORDER_CLOSE_THRESHOLD = 80.0D;
   private static final int DISRUPTION_DURATION_TICKS = 200;
   private static final int EXPOSURE_COOLDOWN_TICKS = 1200;
   private static final double OVERLOAD_DEATH_RATIO = 1.50D;
   private static final ResourceKey<Biome> CITY_BIOME = ResourceKey.create(
      net.minecraft.core.registries.Registries.BIOME,
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "city")
   );

   private MagecraftRiskService() {
   }

   public static double disorder(ServerPlayer player) {
      return player == null ? 0.0D : clamp(player.getPersistentData().getDouble(DISORDER_TAG), 0.0D, MAX_DISORDER);
   }

   public static int disruptionTicks(ServerPlayer player) {
      return player == null ? 0 : Math.max(0, player.getPersistentData().getInt(DISRUPTION_TICKS_TAG));
   }

   public static boolean isDisrupted(ServerPlayer player) {
      return disruptionTicks(player) > 0;
   }

   public static double castingEfficiency(ServerPlayer player) {
      // At full disorder a spell costs at most 50% more.  The penalty is
      // noticeable, but a single failed experiment never bricks a save.
      return 1.0D + disorder(player) * 0.005D;
   }

   public static void addDisorder(ServerPlayer player, double amount) {
      if (player == null || amount <= 0.0D || !isOrdinaryMagus(player)) return;
      double next = clamp(disorder(player) + amount, 0.0D, MAX_DISORDER);
      player.getPersistentData().putDouble(DISORDER_TAG, next);
      if (next >= DISORDER_CLOSE_THRESHOLD && !isDisrupted(player)) {
         closeCircuit(player);
      }
   }

   public static void reduceDisorder(ServerPlayer player, double amount) {
      if (player == null || amount <= 0.0D) return;
      player.getPersistentData().putDouble(DISORDER_TAG, clamp(disorder(player) - amount, 0.0D, MAX_DISORDER));
   }

   public static void onInsufficientMana(ServerPlayer player) {
      addDisorder(player, 1.5D);
   }

   public static void onMagicFailure(ServerPlayer player, String magicId) {
      if (player == null) return;
      int complexity = MagicLearningStrategy.complexity(magicId);
      addDisorder(player, 1.0D + complexity * 0.045D);
   }

   public static void onResearchFailure(ServerPlayer player, String magicId) {
      if (player == null) return;
      addDisorder(player, 2.0D + MagicLearningStrategy.complexity(magicId) * 0.06D);
   }

   public static void onGemEngravingFailure(ServerPlayer player, int failedAttempts) {
      if (player == null || failedAttempts <= 0) return;
      addDisorder(player, Math.min(25.0D, failedAttempts * 2.5D));
   }

   /** Records a successful cast and applies the local, short-lived exposure rule. */
   public static void recordCast(ServerPlayer player, String magicId, double manaCost, boolean success) {
      if (player == null || !success || !isOrdinaryMagus(player) || !isPubliclyObservable(magicId, manaCost)) return;
      ServerLevel level = player.serverLevel();
      long now = level.getGameTime();
      if (player.getPersistentData().getLong(EXPOSURE_COOLDOWN_TAG) > now) return;

      AABB area = player.getBoundingBox().inflate(48.0D, 24.0D, 48.0D);
      boolean city = level.getBiome(player.blockPosition()).is(CITY_BIOME);
      boolean humanActivity = !level.getEntitiesOfClass(LivingEntity.class, area,
         entity -> entity != player && isHumanObserver(entity)).isEmpty();
      if (!city && !humanActivity) return;

      double chance = clamp(0.12D + MagicLearningStrategy.complexity(magicId) * 0.006D + Math.max(0.0D, manaCost) * 0.002D, 0.12D, 0.70D);
      if (level.random.nextDouble() >= chance) return;

      player.getPersistentData().putLong(EXPOSURE_COOLDOWN_TAG, now + EXPOSURE_COOLDOWN_TICKS);
      int pursuers = MagicLearningStrategy.complexity(magicId) >= 80 || manaCost >= 80.0D ? 2 : 1;
      int spawned = 0;
      for (int i = 0; i < pursuers; i++) {
         MysticMagicianEntity pursuer = createPursuer(level, player, i);
         if (pursuer != null) {
            level.addFreshEntity(pursuer);
            spawned++;
         }
      }
      if (spawned > 0) {
         player.displayClientMessage(Component.literal("附近的魔术师注意到了这次施法，追杀者已经出现。"), true);
      }
   }

   private static MysticMagicianEntity createPursuer(ServerLevel level, ServerPlayer target, int index) {
      MysticMagicianEntity pursuer = ModEntities.MYSTIC_MAGICIAN_ADEPT.get().create(level);
      if (pursuer == null) return null;
      double angle = level.random.nextDouble() * Math.PI * 2.0D + index * Math.PI;
      double distance = 12.0D + level.random.nextDouble() * 8.0D;
      BlockPos pos = BlockPos.containing(target.getX() + Math.cos(angle) * distance, target.getY(), target.getZ() + Math.sin(angle) * distance);
      pursuer.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, target.getYRot() + 180.0F, 0.0F);
      pursuer.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
      pursuer.setTarget(target);
      pursuer.getPersistentData().putString(PURSUER_OWNER_TAG, target.getUUID().toString());
      return pursuer;
   }

   private static boolean isHumanObserver(LivingEntity entity) {
      return entity instanceof Villager
         || entity instanceof WanderingTrader
         || entity instanceof HumanNpcEntity
         || entity instanceof ServerPlayer;
   }

   private static boolean isPubliclyObservable(String magicId, double manaCost) {
      if (magicId == null) return manaCost >= 35.0D;
      String id = MagicLearningStrategy.normalizeDisplayId(magicId);
      if (manaCost >= 35.0D) return true;
      return switch (id) {
         case "flame_array", "azure_water_array", "gale_wind_array", "rock_earth_array",
            "storm", "spiritron_cannon", "jewel_magic_release", "jewel_machine_gun",
            "ruby_flame_sword", "imaginary_space", "nega_summon" -> true;
         default -> false;
      };
   }

   /** Called once per server tick by the existing player tick coordinator. */
   public static void tick(ServerPlayer player) {
      if (player == null || !isOrdinaryMagus(player)) return;
      CompoundTagAccess data = new CompoundTagAccess(player);
      int disruption = data.disruptionTicks();
      if (disruption > 0) {
         data.setDisruptionTicks(Math.max(0, disruption - 1));
         if (disruption == 1) {
            player.displayClientMessage(Component.literal("魔术回路紊乱已缓解，可以重新施法。"), true);
         }
      }

      if (player.tickCount % 20 != 0) return;
      double disorder = disorder(player);
      if (disorder > 0.0D) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         double recovery = player.isSleeping() ? 2.0D : (!vars.is_magic_circuit_open ? 1.0D : 0.25D);
         if (player.level() instanceof ServerLevel level && LeylineService.getConcentration(level, player.blockPosition()) >= 70) {
            recovery += 1.0D;
         }
         reduceDisorder(player, recovery);
      }
      if (disorder(player) >= DISORDER_CLOSE_THRESHOLD && !isDisrupted(player)) closeCircuit(player);
      tickOverload(player);
   }

   private static void closeCircuit(ServerPlayer player) {
      player.getPersistentData().putInt(DISRUPTION_TICKS_TAG, DISRUPTION_DURATION_TICKS);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.is_magic_circuit_open = false;
      vars.magic_circuit_open_timer = 0.0D;
      vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.MAGIC);
      player.displayClientMessage(Component.literal("魔术回路紊乱过高，回路已暂时关闭。"), true);
   }

   private static void tickOverload(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double max = vars.magicState().maxMana();
      double mana = vars.magicState().mana();
      if (max <= 0.0D || mana <= max) return;
      if (mana > max * OVERLOAD_DEATH_RATIO) {
         player.displayClientMessage(Component.literal("魔力超过回路承受上限，回路崩溃。"), true);
         player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
         return;
      }
      double overflow = mana - max;
      double drain = Math.max(1.0D, overflow * 0.10D);
      vars.magicState().setMana(Math.max(max, mana - drain));
      float damage = (float)Math.min(4.0D, Math.max(0.5D, overflow / max * 2.0D));
      player.hurt(player.damageSources().magic(), damage);
      vars.syncMana(player);
   }

   private static boolean isOrdinaryMagus(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.is_magus && !vars.servantCardState().transformed();
   }

   private static double clamp(double value, double min, double max) {
      return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
   }

   /** Small adapter avoids exposing the player's persistent tag implementation in callers. */
   private static final class CompoundTagAccess {
      private final ServerPlayer player;
      private CompoundTagAccess(ServerPlayer player) { this.player = player; }
      private int disruptionTicks() { return Math.max(0, player.getPersistentData().getInt(DISRUPTION_TICKS_TAG)); }
      private void setDisruptionTicks(int value) { player.getPersistentData().putInt(DISRUPTION_TICKS_TAG, Math.max(0, value)); }
   }
}
