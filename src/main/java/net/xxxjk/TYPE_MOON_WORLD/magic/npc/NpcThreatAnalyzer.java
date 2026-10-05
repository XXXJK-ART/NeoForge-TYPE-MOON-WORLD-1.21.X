package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

/** Threat scanning and short-lived per-NPC threat cache for magic AI. */
public final class NpcThreatAnalyzer {
   public static final String CACHE_TICK = "TypeMoonNpcThreatCacheTick";
   public static final String CACHE_TARGET = "TypeMoonNpcThreatCacheTarget";
   public static final String CACHE_COUNT = "TypeMoonNpcThreatCacheCount";
   public static final String CACHE_STRONG = "TypeMoonNpcThreatCacheStrong";
   public static final String CACHE_LOW = "TypeMoonNpcThreatCacheLow";

   public record Profile(int enemyCount, boolean strongTarget, boolean lowWhilePressured) {
      public static final Profile NONE = new Profile(0, false, false);
   }

   private NpcThreatAnalyzer() {
   }

   public static Profile cached(MysticMagicianEntity npc, LivingEntity target, long gameTime) {
      if (npc == null || target == null || !target.isAlive()) {
         return Profile.NONE;
      }
      CompoundTag data = npc.getPersistentData();
      String targetId = target.getUUID().toString();
      if (data.contains(CACHE_TICK)
         && gameTime - data.getLong(CACHE_TICK) < 5L
         && targetId.equals(data.getString(CACHE_TARGET))) {
         return new Profile(data.getInt(CACHE_COUNT), data.getBoolean(CACHE_STRONG), data.getBoolean(CACHE_LOW));
      }
      Profile profile = analyze(npc, target);
      data.putLong(CACHE_TICK, gameTime);
      data.putString(CACHE_TARGET, targetId);
      data.putInt(CACHE_COUNT, profile.enemyCount());
      data.putBoolean(CACHE_STRONG, profile.strongTarget());
      data.putBoolean(CACHE_LOW, profile.lowWhilePressured());
      return profile;
   }

   public static Profile analyze(MysticMagicianEntity npc, LivingEntity target) {
      if (npc == null || target == null || !target.isAlive()) {
         return Profile.NONE;
      }
      int enemyCount = countThreatEnemies(npc, target);
      double selfPower = estimateEntityPower(npc);
      double targetPower = estimateEntityPower(target);
      boolean strongTarget = targetPower >= selfPower * 1.18D;
      double healthRatio = npc.getHealth() / Math.max(1.0D, npc.getMaxHealth());
      boolean lowWhilePressured = enemyCount >= 2 && healthRatio <= 0.65D;
      return new Profile(enemyCount, strongTarget, lowWhilePressured);
   }

   private static int countThreatEnemies(MysticMagicianEntity npc, LivingEntity currentTarget) {
      int count = 0;
      Set<UUID> seen = new HashSet<>();
      if (currentTarget != null && currentTarget.isAlive()) {
         seen.add(currentTarget.getUUID());
         count++;
      }
      for (net.minecraft.world.entity.Mob mob : NpcNearbyEntityIndex.mobs(npc, 20.0D)) {
         if (mob != npc && mob.isAlive() && mob.getTarget() == npc) {
         if (seen.add(mob.getUUID())) {
            count++;
         }
         }
      }
      return count;
   }

   private static double estimateEntityPower(LivingEntity entity) {
      if (entity == null) {
         return 1.0D;
      }
      return Math.max(1.0D, entity.getMaxHealth()) * 0.8D
         + readAttributeValue(entity, Attributes.ATTACK_DAMAGE, 1.0D) * 6.0D
         + readAttributeValue(entity, Attributes.ARMOR, 0.0D) * 2.0D
         + readAttributeValue(entity, Attributes.ARMOR_TOUGHNESS, 0.0D) * 2.5D;
   }

   private static double readAttributeValue(LivingEntity entity, net.minecraft.core.Holder<Attribute> attribute, double fallback) {
      AttributeInstance instance = entity.getAttribute(attribute);
      return instance == null ? fallback : instance.getValue();
   }
}
