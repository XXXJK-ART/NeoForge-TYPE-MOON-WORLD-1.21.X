package net.xxxjk.TYPE_MOON_WORLD.servant.baobhan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.joml.Vector3f;
import net.minecraft.core.particles.DustParticleOptions;

public final class BaobhanSithCurseService {
   public static final String TAG_PREFIX = "TypeMoonBaobhanSith";
   public static final String TAG_MEDIUMS = TAG_PREFIX + "Mediums";
   public static final String TAG_CURSES = TAG_PREFIX + "Curses";
   public static final String TAG_PANEL_SELECTED = TAG_PREFIX + "PanelSelected";
   public static final String TAG_PANEL_LAST_TRIGGER = TAG_PREFIX + "PanelLastTrigger";
   public static final String TAG_CURSE_TARGETS = TAG_PREFIX + "CurseTargets";
   public static final String TAG_LAST_TICK = TAG_PREFIX + "LastTick";
   public static final String TAG_IMMUNE_UNTIL = "ImmuneUntil";
   public static final String TAG_BURST_UNTIL = "BurstUntil";
   public static final String TAG_CURSE_LAYERS = "CurseLayers";
   public static final String TAG_EXPIRES_AT = "ExpiresAt";
   public static final String TAG_STRENGTH_SCALE = "StrengthScale";
   public static final String MEDIUM_BLOOD = "Blood";
   public static final String MEDIUM_SKIN = "Skin";
   public static final String MEDIUM_HAIR = "Hair";
   public static final String MEDIUM_REMAINS = "Remains";
   public static final String CURSE_BLOOD = "BloodCurse";
   public static final String CURSE_SKIN = "SkinCurse";
   public static final String CURSE_HAIR = "HairCurse";
   public static final String CURSE_REMAINS = "RemainsCurse";
   public static final int MAX_MEDIUMS_PER_TYPE = 10;

   private static final DustParticleOptions BLOOD_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.04F, 0.08F), 1.1F);
   private static final DustParticleOptions CURSE_DUST = new DustParticleOptions(new Vector3f(0.09F, 0.02F, 0.11F), 1.0F);
   private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(new Vector3f(0.44F, 0.06F, 0.62F), 0.95F);

   private BaobhanSithCurseService() {
   }

   public static DamageSource curseDamageSource(LivingEntity owner) {
      return owner.damageSources().source(BaobhanSithDamageTypes.CURSE, owner);
   }

   public static DamageSource curseDamageSource(LivingEntity owner, net.minecraft.world.entity.Entity direct) {
      return owner.damageSources().source(BaobhanSithDamageTypes.CURSE, direct, owner);
   }

   public static void addMedium(LivingEntity owner, LivingEntity target, String medium, int count) {
      if (owner == null || target == null || medium == null || count <= 0) {
         return;
      }
      CompoundTag root = owner.getPersistentData().getCompound(TAG_MEDIUMS);
      CompoundTag targetTag = root.getCompound(target.getUUID().toString());
      targetTag.putInt(normalizeMedium(medium), Math.min(MAX_MEDIUMS_PER_TYPE, targetTag.getInt(normalizeMedium(medium)) + count));
      root.put(target.getUUID().toString(), targetTag);
      owner.getPersistentData().put(TAG_MEDIUMS, root);
   }

   public static boolean consumeMedium(LivingEntity owner, LivingEntity target, String medium, int count) {
      if (owner == null || target == null || medium == null || count <= 0) {
         return false;
      }
      String key = normalizeMedium(medium);
      CompoundTag root = owner.getPersistentData().getCompound(TAG_MEDIUMS);
      CompoundTag targetTag = root.getCompound(target.getUUID().toString());
      int current = targetTag.getInt(key);
      if (current < count) {
         return false;
      }
      if (current == count) {
         targetTag.remove(key);
      } else {
         targetTag.putInt(key, current - count);
      }
      root.put(target.getUUID().toString(), targetTag);
      owner.getPersistentData().put(TAG_MEDIUMS, root);
      return true;
   }

   public static void consumeAllMediums(LivingEntity owner, LivingEntity target) {
      if (owner == null || target == null) {
         return;
      }
      CompoundTag root = owner.getPersistentData().getCompound(TAG_MEDIUMS);
      root.remove(target.getUUID().toString());
      owner.getPersistentData().put(TAG_MEDIUMS, root);
   }

   public static int mediumCount(LivingEntity owner, LivingEntity target, String medium) {
      if (owner == null || target == null || medium == null) {
         return 0;
      }
      return owner.getPersistentData().getCompound(TAG_MEDIUMS)
         .getCompound(target.getUUID().toString())
         .getInt(normalizeMedium(medium));
   }

   public static int totalMediumCount(LivingEntity owner, LivingEntity target) {
      if (owner == null || target == null) {
         return 0;
      }
      return totalMediumCount(owner.getPersistentData().getCompound(TAG_MEDIUMS).getCompound(target.getUUID().toString()));
   }

   public static int totalMediumCount(CompoundTag targetTag) {
      if (targetTag == null) {
         return 0;
      }
      return targetTag.getInt(MEDIUM_BLOOD) + targetTag.getInt(MEDIUM_SKIN)
         + targetTag.getInt(MEDIUM_HAIR) + targetTag.getInt(MEDIUM_REMAINS);
   }

   public static boolean applyCurse(LivingEntity owner, LivingEntity target, String curse, int amount, int durationTicks, float strengthScale) {
      if (owner == null || target == null || curse == null || amount <= 0 || !target.isAlive()) {
         return false;
      }
      long now = owner.level().getGameTime();
      CompoundTag curseTag = targetCurses(owner, target);
      if (now < curseTag.getLong(TAG_IMMUNE_UNTIL)) {
         return false;
      }
      String key = normalizeCurse(curse);
      trackTarget(owner, target);
      trackCurseTarget(owner, target);
      curseTag.putInt(key, Math.min(5, curseTag.getInt(key) + amount));
      curseTag.putInt(TAG_CURSE_LAYERS, Math.min(20, curseTag.getInt(TAG_CURSE_LAYERS) + amount));
      curseTag.putFloat(TAG_STRENGTH_SCALE, Math.max(0.2F, Math.min(1.0F, strengthScale)));
      curseTag.putLong(TAG_EXPIRES_AT, Math.max(curseTag.getLong(TAG_EXPIRES_AT), now + Math.max(20, durationTicks)));
      if (curseTag.getInt(TAG_CURSE_LAYERS) >= 5 && now >= curseTag.getLong(TAG_BURST_UNTIL)) {
         curseTag.putLong(TAG_BURST_UNTIL, now + 200L);
      }
      saveTargetCurses(owner, target, curseTag);
      if (target.level() instanceof ServerLevel level) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.52, 0.0);
         level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, 12, 0.36, 0.34, 0.36, 0.0);
         level.sendParticles(CURSE_DUST, center.x, center.y, center.z, 10, 0.30, 0.28, 0.30, 0.0);
         spawnCurseSigil(level, center, curseTag.getInt(TAG_CURSE_LAYERS));
      }
      return true;
   }

   public static int totalCurseLayers(LivingEntity owner, LivingEntity target) {
      if (owner == null || target == null) {
         return 0;
      }
      return readTargetCurses(owner, target).getInt(TAG_CURSE_LAYERS);
   }

   public static int curseCount(LivingEntity owner, LivingEntity target, String curse) {
      if (owner == null || target == null || curse == null) {
         return 0;
      }
      return readTargetCurses(owner, target).getInt(normalizeCurse(curse));
   }

   public static void tickOwnerCurses(ServerPlayer owner) {
      tickTrackedCurses(owner);
   }

   public static void tickTrackedCurses(LivingEntity owner) {
      if (owner == null || !(owner.level() instanceof ServerLevel)) {
         return;
      }
      long now = owner.level().getGameTime();
      if (now - owner.getPersistentData().getLong(TAG_LAST_TICK + "Owner") < 20L) {
         return;
      }
      owner.getPersistentData().putLong(TAG_LAST_TICK + "Owner", now);
      for (UUID uuid : curseTargetUuids(owner)) {
          LivingEntity target = findCursedTarget(owner, uuid);
          if (target != null) {
             tickTargetCurses(owner, target, now);
          }
      }
   }

   public static void tickTargetCurses(LivingEntity owner, LivingEntity target, long now) {
      CompoundTag curseTag = readTargetCurses(owner, target);
      if (curseTag.isEmpty() || now - curseTag.getLong(TAG_LAST_TICK) < 20L) {
         return;
      }
      if (curseTag.getLong(TAG_EXPIRES_AT) > 0L && now > curseTag.getLong(TAG_EXPIRES_AT)) {
         clearCurse(owner, target, now, 0L);
         return;
      }
      curseTag.putLong(TAG_LAST_TICK, now);
      boolean burst = now < curseTag.getLong(TAG_BURST_UNTIL);
      float scale = Math.max(0.2F, curseTag.getFloat(TAG_STRENGTH_SCALE));
      float damage = (curseTag.getInt(CURSE_BLOOD) * 5.0F + curseTag.getInt(CURSE_REMAINS) * 15.0F) * scale;
      if (curseTag.getInt(CURSE_BLOOD) > 0) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, Math.min(4, curseTag.getInt(CURSE_BLOOD) - 1), false, true, true));
      }
      if (curseTag.getInt(CURSE_SKIN) > 0) {
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, Math.min(4, curseTag.getInt(CURSE_SKIN) - 1), false, true, true));
      }
      if (curseTag.getInt(CURSE_HAIR) > 0) {
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 8, false, true, true));
         target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, true, true));
      }
      if (burst) {
         damage *= 2.0F;
         if (target.level() instanceof ServerLevel level) {
            Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
            spawnBurst(level, center, 1.12, 20);
            spawnThornSpiral(level, center, 1.0, target.getBbHeight() * 0.95, 22);
         }
      } else if (curseTag.getLong(TAG_BURST_UNTIL) > 0L && now >= curseTag.getLong(TAG_BURST_UNTIL)) {
         clearCurse(owner, target, now, 100L);
         return;
      }
      if (damage > 0.0F) {
         boolean wasAlive = target.isAlive();
         target.invulnerableTime = 0;
         target.hurt(curseDamageSource(owner), damage);
         target.invulnerableTime = 0;
         if (wasAlive && !target.isAlive()) {
            addMedium(owner, target, MEDIUM_REMAINS, 1);
         }
      }
      saveTargetCurses(owner, target, curseTag);
   }

   public static float hurtWithCurse(LivingEntity owner, LivingEntity target, float damage) {
      if (owner == null || target == null || damage <= 0.0F) {
         return 0.0F;
      }
      float before = target.getHealth();
      target.invulnerableTime = 0;
      target.hurt(curseDamageSource(owner), damage);
      target.invulnerableTime = 0;
      if (before > 0.0F && !target.isAlive()) {
         addMedium(owner, target, MEDIUM_REMAINS, 1);
      }
      return Math.min(before, damage);
   }

   public static float drainLife(LivingEntity owner, LivingEntity target, float amount) {
      float dealt = hurtWithCurse(owner, target, amount);
      owner.heal(amount);
      return dealt;
   }

   public static void clearCurse(LivingEntity owner, LivingEntity target, long now, long immuneTicks) {
      CompoundTag curseTag = readTargetCurses(owner, target);
      for (String key : List.copyOf(curseTag.getAllKeys())) {
         curseTag.remove(key);
      }
      if (immuneTicks > 0L) {
         curseTag.putLong(TAG_IMMUNE_UNTIL, now + immuneTicks);
      }
      saveTargetCurses(owner, target, curseTag);
   }

   public static void triggerBurst(LivingEntity owner, LivingEntity target) {
      trackCurseTarget(owner, target);
      CompoundTag curseTag = targetCurses(owner, target);
      curseTag.putLong(TAG_BURST_UNTIL, owner.level().getGameTime() + 200L);
      saveTargetCurses(owner, target, curseTag);
   }

   public static String preferredMissingMedium(LivingEntity owner, LivingEntity target) {
      CompoundTag mediumTag = owner.getPersistentData().getCompound(TAG_MEDIUMS).getCompound(target.getUUID().toString());
      for (String medium : List.of(MEDIUM_SKIN, MEDIUM_HAIR, MEDIUM_BLOOD)) {
         if (mediumTag.getInt(medium) <= 0) {
            return medium;
         }
      }
      return MEDIUM_SKIN;
   }

   public static List<Snapshot> snapshots(ServerPlayer owner) {
      if (owner == null || owner.server == null) {
         return List.of();
      }
      CompoundTag mediumRoot = owner.getPersistentData().getCompound(TAG_MEDIUMS);
      List<Snapshot> result = new ArrayList<>();
      for (UUID uuid : curseTargetUuids(owner)) {
         String key = uuid.toString();
         LivingEntity target = findCursedTarget(owner, uuid);
         if (target == null) {
            continue;
         }
         CompoundTag mediumTag = mediumRoot.getCompound(key);
         CompoundTag curseTag = readTargetCurses(owner, target);
         int totalMediums = totalMediumCount(mediumTag);
         int layers = curseTag.getInt(TAG_CURSE_LAYERS);
         if (totalMediums <= 0 && layers <= 0) {
            continue;
         }
         result.add(new Snapshot(
            target.getId(),
            target.getUUID(),
            target.getDisplayName().getString(),
            target.level().dimension().location().toString(),
            target.blockPosition().getX(),
            target.blockPosition().getY(),
            target.blockPosition().getZ(),
            Math.max(0.0F, target.getHealth()),
            target.getMaxHealth(),
            layers,
            mediumTag.getInt(MEDIUM_BLOOD),
            mediumTag.getInt(MEDIUM_SKIN),
            mediumTag.getInt(MEDIUM_HAIR),
            mediumTag.getInt(MEDIUM_REMAINS),
            curseTag.getInt(CURSE_BLOOD),
            curseTag.getInt(CURSE_SKIN),
            curseTag.getInt(CURSE_HAIR),
            curseTag.getInt(CURSE_REMAINS)
         ));
      }
      return result;
   }

   private static Set<UUID> curseTargetUuids(LivingEntity owner) {
      Set<UUID> result = new LinkedHashSet<>();
      if (owner == null) {
         return result;
      }
      CompoundTag mediumRoot = owner.getPersistentData().getCompound(TAG_MEDIUMS);
      for (String key : mediumRoot.getAllKeys()) {
         UUID uuid = parseUuid(key);
         if (uuid != null) {
            result.add(uuid);
         }
      }
      CompoundTag curseRoot = owner.getPersistentData().getCompound(TAG_CURSE_TARGETS);
      for (String key : curseRoot.getAllKeys()) {
         UUID uuid = parseUuid(key);
         if (uuid != null) {
            result.add(uuid);
         }
      }
      return result;
   }

   public static LivingEntity findCursedTarget(ServerPlayer owner, UUID uuid) {
      return findCursedTarget((LivingEntity) owner, uuid);
   }

   public static LivingEntity findCursedTarget(LivingEntity owner, UUID uuid) {
      if (owner == null || uuid == null || !(owner.level() instanceof ServerLevel ownerLevel)
         || ownerLevel.getServer() == null) {
          return null;
      }
      for (ServerLevel level : ownerLevel.getServer().getAllLevels()) {
         if (level.getEntity(uuid) instanceof LivingEntity living && living.isAlive()) {
            return living;
         }
      }
      return null;
   }

   public static void setSelectedTarget(ServerPlayer owner, UUID uuid) {
      if (owner != null && uuid != null) {
         owner.getPersistentData().putUUID(TAG_PANEL_SELECTED, uuid);
      }
   }

   public static LivingEntity selectedTarget(ServerPlayer owner) {
      if (owner == null || !owner.getPersistentData().hasUUID(TAG_PANEL_SELECTED)) {
         return null;
      }
      return findCursedTarget(owner, owner.getPersistentData().getUUID(TAG_PANEL_SELECTED));
   }

   public static void clearPanelState(ServerPlayer owner) {
      if (owner != null) {
         owner.getPersistentData().remove(TAG_PANEL_SELECTED);
         owner.getPersistentData().remove(TAG_PANEL_LAST_TRIGGER);
      }
   }

   public static boolean isSuppressed(LivingEntity entity) {
      return entity != null && entity.getPersistentData().getLong("TypeMoonCombatSuppressedUntil") > entity.level().getGameTime();
   }

   public static String curseForMedium(String medium) {
      return switch (normalizeMedium(medium)) {
         case MEDIUM_SKIN -> CURSE_SKIN;
         case MEDIUM_HAIR -> CURSE_HAIR;
         case MEDIUM_REMAINS -> CURSE_REMAINS;
         default -> CURSE_BLOOD;
      };
   }

   public static String normalizeMedium(String medium) {
      if (medium == null) {
         return MEDIUM_BLOOD;
      }
      return switch (medium.toLowerCase(Locale.ROOT)) {
         case "skin", "皮肤" -> MEDIUM_SKIN;
         case "hair", "头发" -> MEDIUM_HAIR;
         case "remains", "残骸" -> MEDIUM_REMAINS;
         default -> MEDIUM_BLOOD;
      };
   }

   public static String normalizeCurse(String curse) {
      if (curse == null) {
         return CURSE_BLOOD;
      }
      return switch (curse) {
         case CURSE_SKIN, "skin", "Skin" -> CURSE_SKIN;
         case CURSE_HAIR, "hair", "Hair" -> CURSE_HAIR;
         case CURSE_REMAINS, "remains", "Remains" -> CURSE_REMAINS;
         default -> CURSE_BLOOD;
      };
   }

   public static boolean isServant(LivingEntity entity) {
      return entity instanceof ServantEntity;
   }

   private static CompoundTag targetCurses(LivingEntity owner, LivingEntity target) {
      CompoundTag root = target.getPersistentData().getCompound(TAG_CURSES);
      CompoundTag ownerTag = root.getCompound(owner.getUUID().toString());
      root.put(owner.getUUID().toString(), ownerTag);
      target.getPersistentData().put(TAG_CURSES, root);
      return ownerTag;
   }

   private static void trackTarget(LivingEntity owner, LivingEntity target) {
      CompoundTag root = owner.getPersistentData().getCompound(TAG_MEDIUMS);
      String key = target.getUUID().toString();
      if (!root.contains(key)) {
         root.put(key, new CompoundTag());
         owner.getPersistentData().put(TAG_MEDIUMS, root);
      }
   }

   private static void trackCurseTarget(LivingEntity owner, LivingEntity target) {
      CompoundTag root = owner.getPersistentData().getCompound(TAG_CURSE_TARGETS);
      root.put(target.getUUID().toString(), new CompoundTag());
      owner.getPersistentData().put(TAG_CURSE_TARGETS, root);
   }

   private static CompoundTag readTargetCurses(LivingEntity owner, LivingEntity target) {
      return target.getPersistentData().getCompound(TAG_CURSES).getCompound(owner.getUUID().toString());
   }

   private static void saveTargetCurses(LivingEntity owner, LivingEntity target, CompoundTag ownerTag) {
      CompoundTag root = target.getPersistentData().getCompound(TAG_CURSES);
      root.put(owner.getUUID().toString(), ownerTag);
      target.getPersistentData().put(TAG_CURSES, root);
   }

   private static UUID parseUuid(String key) {
      try {
         return UUID.fromString(key);
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   private static void spawnCurseSigil(ServerLevel level, Vec3 center, int layers) {
      double radius = 0.35 + Math.min(5, layers) * 0.09;
      int points = 18 + Math.min(5, layers) * 4;
      for (int i = 0; i < points; i++) {
         double angle = i * Math.PI * 2.0 / points;
         Vec3 pos = center.add(Math.cos(angle) * radius, -center.y + Math.floor(center.y) + 0.08, Math.sin(angle) * radius);
         level.sendParticles(i % 2 == 0 ? BLOOD_DUST : PURPLE_DUST, pos.x, pos.y, pos.z, 1, 0.01, 0.01, 0.01, 0.0);
      }
   }

   private static void spawnBurst(ServerLevel level, Vec3 center, double radius, int count) {
      level.sendParticles(BLOOD_DUST, center.x, center.y, center.z, count, radius, radius * 0.45, radius, 0.0);
      level.sendParticles(CURSE_DUST, center.x, center.y, center.z, count, radius * 0.85, radius * 0.35, radius * 0.85, 0.0);
   }

   private static void spawnThornSpiral(ServerLevel level, Vec3 center, double radius, double height, int points) {
      for (int i = 0; i < points; i++) {
         double t = i / Math.max(1.0, points - 1.0);
         double angle = t * Math.PI * 5.0;
         Vec3 pos = center.add(Math.cos(angle) * radius * (1.0 - t * 0.25), height * (t - 0.5), Math.sin(angle) * radius * (1.0 - t * 0.25));
         level.sendParticles(i % 3 == 0 ? CURSE_DUST : BLOOD_DUST, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
   }

   public record Snapshot(
      int entityId,
      UUID uuid,
      String name,
      String dimension,
      int x,
      int y,
      int z,
      float health,
      float maxHealth,
      int layers,
      int blood,
      int skin,
      int hair,
      int remains,
      int bloodCurse,
      int skinCurse,
      int hairCurse,
      int remainsCurse
   ) {
   }
}
