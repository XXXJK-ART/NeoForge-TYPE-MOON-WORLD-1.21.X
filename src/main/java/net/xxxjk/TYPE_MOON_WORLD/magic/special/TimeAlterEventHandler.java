package net.xxxjk.TYPE_MOON_WORLD.magic.special;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import org.joml.Vector3f;

@EventBusSubscriber(modid = "typemoonworld")
public final class TimeAlterEventHandler {
   private static final String TAG_ACTIVE_UNTIL = "TypeMoonTimeAlterActiveUntil";
   private static final String TAG_MODE = "TypeMoonTimeAlterMode";
   private static final String TAG_MULTIPLIER = "TypeMoonTimeAlterMultiplier";
   private static final String TAG_COOLDOWN_UNTIL = "TypeMoonTimeAlterCooldownUntil";
   private static final String TAG_STRAIN = "TypeMoonTimeAlterStrain";
   private static final String TAG_STRAIN_UNTIL = "TypeMoonTimeAlterStrainUntil";
   private static final int STRAIN_RECOVERY_TICKS = 1200;
   private static final ResourceLocation SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_speed");
   private static final ResourceLocation ATTACK_SPEED_MODIFIER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_attack_speed");
   private static final ResourceLocation JUMP_MODIFIER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_jump");
   private static final ResourceKey<DamageType> BACKLASH_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("typemoonworld", "time_alter_backlash"));
   private static final DustParticleOptions ACCEL_DUST = new DustParticleOptions(new Vector3f(0.28F, 0.78F, 1.0F), 1.0F);
   private static final DustParticleOptions STAGNATE_DUST = new DustParticleOptions(new Vector3f(0.62F, 0.78F, 0.92F), 0.9F);

   private TimeAlterEventHandler() {
   }

   public static boolean isActive(ServerPlayer player) {
      return player != null && player.getPersistentData().getLong(TAG_ACTIVE_UNTIL) > player.level().getGameTime();
   }

   public static boolean canStart(ServerPlayer player, int mode, double multiplier) {
      long now = player.level().getGameTime();
      long cooldownUntil = player.getPersistentData().getLong(TAG_COOLDOWN_UNTIL);
      if (cooldownUntil > now) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.time_alter.cooldown", String.format("%.1f", (cooldownUntil - now) / 20.0)), true);
         return false;
      }
      if (player.getPersistentData().getLong(TAG_STRAIN_UNTIL) <= now) {
         player.getPersistentData().remove(TAG_STRAIN);
         player.getPersistentData().remove(TAG_STRAIN_UNTIL);
      }
      return true;
   }

   public static void release(ServerPlayer player) {
      if (player != null) {
         finish(player, player.level().getGameTime());
      }
   }

   public static void start(ServerPlayer player, int mode, double multiplier, int durationTicks) {
      long now = player.level().getGameTime();
      player.getPersistentData().putLong(TAG_ACTIVE_UNTIL, now + Math.max(1, durationTicks));
      player.getPersistentData().putInt(TAG_MODE, mode);
      player.getPersistentData().putDouble(TAG_MULTIPLIER, multiplier);
      applyModifiers(player, mode, multiplier);
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
         return;
      }
      long activeUntil = player.getPersistentData().getLong(TAG_ACTIVE_UNTIL);
      if (activeUntil <= 0L) {
         removeModifiers(player);
         return;
      }

      long now = player.level().getGameTime();
      if (activeUntil <= now || !player.isAlive()) {
         finish(player, now);
         return;
      }

      if (player.tickCount % 4 == 0) {
         applyModifiers(player, player.getPersistentData().getInt(TAG_MODE), player.getPersistentData().getDouble(TAG_MULTIPLIER));
      }
      if (player.tickCount % 3 == 0 && player.level() instanceof ServerLevel level) {
         int mode = player.getPersistentData().getInt(TAG_MODE);
         level.sendParticles(mode == MagicTimeAlter.MODE_ACCEL ? ACCEL_DUST : STAGNATE_DUST,
            player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(),
            mode == MagicTimeAlter.MODE_ACCEL ? 3 : 2, 0.28, 0.36, 0.28, 0.0);
      }
   }

   private static void finish(ServerPlayer player, long now) {
      int mode = player.getPersistentData().getInt(TAG_MODE);
      double multiplier = player.getPersistentData().getDouble(TAG_MULTIPLIER);
      removeModifiers(player);
      player.getPersistentData().remove(TAG_ACTIVE_UNTIL);
      player.getPersistentData().remove(TAG_MODE);
      player.getPersistentData().remove(TAG_MULTIPLIER);

      double cooldownSeconds = mode == MagicTimeAlter.MODE_ACCEL
         ? 10.0 + multiplier * 5.0
         : 15.0 + (1.0 / Math.max(0.05, multiplier)) * 5.0;
      player.getPersistentData().putLong(TAG_COOLDOWN_UNTIL, now + Math.round(cooldownSeconds * 20.0));

      float damage = (float)(multiplier * 3.0 + 2.0 * Math.pow(Math.max(0.0, multiplier - 1.0), 2.0));
      if (hasAvalon(player)) {
         damage *= 0.5F;
      }
      int strain = player.getPersistentData().getInt(TAG_STRAIN);
      damage *= 1.0F + strain * 0.5F;
      player.getPersistentData().putInt(TAG_STRAIN, Math.min(6, strain + 1));
      player.getPersistentData().putLong(TAG_STRAIN_UNTIL, now + STRAIN_RECOVERY_TICKS);
      player.invulnerableTime = 0;
      player.hurt(player.damageSources().source(BACKLASH_DAMAGE), damage);
      player.invulnerableTime = 0;

      player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.7F, 0.8F);
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.time_alter.release", String.format("%.1f", damage)), true);
   }

   private static void applyModifiers(ServerPlayer player, int mode, double multiplier) {
      double speedBonus;
      double attackBonus;
      double jumpBonus;
      if (mode == MagicTimeAlter.MODE_ACCEL) {
         speedBonus = Math.max(0.0, multiplier - 1.0);
         attackBonus = Math.max(0.0, multiplier - 1.0);
         jumpBonus = Math.max(0.0, Math.min(multiplier - 1.0, 0.5));
      } else {
         speedBonus = Math.max(-0.9, multiplier - 1.0);
         attackBonus = Math.max(-0.9, multiplier - 1.0);
         jumpBonus = Math.max(-0.75, multiplier - 1.0);
      }
      updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER, speedBonus);
      updateModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER, attackBonus);
      updateModifier(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_MODIFIER, jumpBonus);
   }

   private static void removeModifiers(ServerPlayer player) {
      removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER);
      removeModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_MODIFIER);
      removeModifier(player.getAttribute(Attributes.JUMP_STRENGTH), JUMP_MODIFIER);
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(id);
      if (Math.abs(amount) > 1.0E-6) {
         attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }

   private static boolean hasAvalon(ServerPlayer player) {
      for (ItemStack stack : player.getInventory().items) {
         if (stack.is(ModItems.AVALON.get())) {
            return true;
         }
      }
      return player.getMainHandItem().is(ModItems.AVALON.get()) || player.getOffhandItem().is(ModItems.AVALON.get());
   }
}
