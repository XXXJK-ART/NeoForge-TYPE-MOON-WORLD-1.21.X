package net.xxxjk.TYPE_MOON_WORLD.magic.church;

import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.BasicMagecraftHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import org.joml.Vector3f;

@EventBusSubscriber(modid = "typemoonworld")
public final class BaptismRiteEventHandler {
   private static final String TAG_UNTIL = "TypeMoonBaptismRiteUntil";
   private static final String TAG_STARTED = "TypeMoonBaptismRiteStarted";
   private static final String TAG_TARGET = "TypeMoonBaptismRiteTarget";
   private static final String TAG_PROFICIENCY = "TypeMoonBaptismRiteProficiency";
   private static final String TAG_LAST_LINE = "TypeMoonBaptismRiteLastLine";
   private static final ResourceKey<DamageType> BAPTISM_DAMAGE = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("typemoonworld", "baptism_rite")
   );
   private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.78F, 0.22F), 1.35F);
   private static final String[] CHANT_LINES = new String[]{
      "message.typemoonworld.magic.baptism_rite.chant.0",
      "message.typemoonworld.magic.baptism_rite.chant.1",
      "message.typemoonworld.magic.baptism_rite.chant.2",
      "message.typemoonworld.magic.baptism_rite.chant.3",
      "message.typemoonworld.magic.baptism_rite.chant.4",
      "message.typemoonworld.magic.baptism_rite.chant.5",
      "message.typemoonworld.magic.baptism_rite.chant.6",
      "message.typemoonworld.magic.baptism_rite.chant.7"
   };

   private BaptismRiteEventHandler() {
   }

   public static boolean isChanting(ServerPlayer player) {
      return player != null && player.getPersistentData().getLong(TAG_UNTIL) > player.level().getGameTime();
   }

   public static void interrupt(ServerPlayer player) {
      if (!isChanting(player)) return;
      clear(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.baptism_rite.interrupted"), true);
   }

   public static void start(ServerPlayer player, UUID targetId, double proficiency, int chantTicks) {
      long now = player.level().getGameTime();
      player.getPersistentData().putLong(TAG_STARTED, now);
      player.getPersistentData().putLong(TAG_UNTIL, now + (chantTicks <= 0 ? 0 : Math.max(20, chantTicks)));
      player.getPersistentData().putString(TAG_TARGET, targetId == null ? "" : targetId.toString());
      player.getPersistentData().putDouble(TAG_PROFICIENCY, BasicMagecraftHelper.clampProficiency(proficiency));
      player.getPersistentData().putInt(TAG_LAST_LINE, -1);
      if (chantTicks <= 0) {
         finish(player);
      }
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
         return;
      }
      long until = player.getPersistentData().getLong(TAG_UNTIL);
      if (until <= 0L) {
         return;
      }
      long now = player.level().getGameTime();
      if (now >= until || !player.isAlive()) {
         finish(player);
         return;
      }
      long started = player.getPersistentData().getLong(TAG_STARTED);
      int duration = (int)Math.max(1L, until - started);
      int elapsed = (int)Math.max(0L, now - started);
      int lineIndex = Math.min(CHANT_LINES.length - 1, elapsed * CHANT_LINES.length / duration);
      if (lineIndex != player.getPersistentData().getInt(TAG_LAST_LINE)) {
         player.getPersistentData().putInt(TAG_LAST_LINE, lineIndex);
         player.displayClientMessage(Component.translatable(CHANT_LINES[lineIndex]), true);
      }
      if (player.tickCount % 4 == 0 && player.level() instanceof ServerLevel level) {
         level.sendParticles(GOLD, player.getX(), player.getY() + player.getBbHeight() * 0.72, player.getZ(), 5, 0.45, 0.5, 0.45, 0.0);
      }
   }

   private static void finish(ServerPlayer player) {
      double proficiency = player.getPersistentData().getDouble(TAG_PROFICIENCY);
      LivingEntity target = resolveTarget(player);
      clear(player);
      if (target == null || !target.isAlive() || !MagicBaptismRite.isValidRiteTarget(target)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.baptism_rite.lost_target"), true);
         return;
      }

      float damage = applyRite(player, target, proficiency);
      if (damage <= 0.0F) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.baptism_rite.lost_target"), true);
         return;
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.baptism_rite.finish", target.getDisplayName(), String.format("%.1f", damage)), true);
   }

   public static float applyRite(LivingEntity caster, LivingEntity target, double proficiency) {
      if (caster == null || target == null || !target.isAlive() || !MagicBaptismRite.isValidRiteTarget(target)) return 0.0F;
      double clamped = BasicMagecraftHelper.clampProficiency(proficiency);
      float damage = (float)(30.0 + clamped / 100.0 * 170.0);
      double chance = 0.30 + BasicMagecraftHelper.clampProficiency(proficiency) / 100.0 * 0.55;
      if (isUndeadLike(target)) {
         damage *= proficiency >= 75.0 ? 2.5F : 2.0F;
      } else if (!(target instanceof ServantEntity)) {
         damage *= 1.5F;
         chance = Math.min(0.95, chance + 0.10);
      }
      MagicResistanceRank rank = MagicResistanceHelper.getMagicResistanceRank(target);
      switch (rank) {
         case C -> {
            damage *= 0.8F;
            chance -= 0.10;
         }
         case B -> {
            damage *= 0.6F;
            chance -= 0.25;
         }
         case A -> {
            damage *= 0.4F;
            chance -= 0.40;
         }
         default -> {
         }
      }
      chance = Math.max(0.0, Math.min(0.95, chance));
      cleanse(target, proficiency);
      net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderInfectionService.cleanse(target, true);
      target.invulnerableTime = 0;
      target.hurt(caster.damageSources().source(BAPTISM_DAMAGE, caster), damage);
      target.invulnerableTime = 0;
      boolean sublimated = target.isAlive() && !(target instanceof ServerPlayer) && caster.getRandom().nextDouble() < chance;
      if (sublimated) {
         target.hurt(caster.damageSources().source(BAPTISM_DAMAGE, caster), Math.max(target.getMaxHealth() * 2.0F, damage));
      }
      spawnFinishParticles(caster, target, proficiency);
      SoundSource soundSource = caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE;
      target.level().playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE, soundSource, 0.9F, 1.45F);
      return damage;
   }

   private static LivingEntity resolveTarget(ServerPlayer player) {
      String raw = player.getPersistentData().getString(TAG_TARGET);
      if (player.level() instanceof ServerLevel level && raw != null && !raw.isBlank()) {
         try {
            Entity entity = level.getEntity(UUID.fromString(raw));
            if (entity instanceof LivingEntity living) {
               return living;
            }
         } catch (IllegalArgumentException ignored) {
         }
      }
      return BasicMagecraftHelper.rayTarget(player, 24.0);
   }

   private static void clear(ServerPlayer player) {
      player.getPersistentData().remove(TAG_UNTIL);
      player.getPersistentData().remove(TAG_STARTED);
      player.getPersistentData().remove(TAG_TARGET);
      player.getPersistentData().remove(TAG_PROFICIENCY);
      player.getPersistentData().remove(TAG_LAST_LINE);
   }

   private static void cleanse(LivingEntity target, double proficiency) {
      int max = proficiency >= 75.0 ? 8 : proficiency >= 50.0 ? 5 : 2;
      int removed = 0;
      for (MobEffectInstance effect : new java.util.ArrayList<>(target.getActiveEffects())) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            target.removeEffect(effect.getEffect());
            if (++removed >= max) {
               return;
            }
         }
      }
   }

   private static void spawnFinishParticles(LivingEntity caster, LivingEntity target, double proficiency) {
      if (caster.level() instanceof ServerLevel level) {
         level.sendParticles(GOLD, target.getX(), target.getY() + target.getBbHeight() * 0.65, target.getZ(), proficiency >= 75.0 ? 80 : 48, 0.55, 0.8, 0.55, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.75, target.getZ(), 24, 0.45, 0.75, 0.45, 0.04);
      }
   }

   private static boolean isUndeadLike(LivingEntity target) {
      return target.getType().is(EntityTypeTags.UNDEAD)
         || target instanceof Zombie
         || target instanceof Skeleton
         || target instanceof Stray
         || target instanceof Husk
         || target instanceof Drowned
         || target instanceof WitherSkeleton
         || target instanceof WitherBoss
         || target instanceof ZombifiedPiglin;
   }
}
