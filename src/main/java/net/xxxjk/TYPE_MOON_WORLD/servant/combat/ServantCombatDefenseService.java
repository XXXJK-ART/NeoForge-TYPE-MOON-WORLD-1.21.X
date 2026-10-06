package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardDefenseStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardActionTimeline;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;

/** Server-authoritative active dodge and guard state shared by cards and NPCs. */
public final class ServantCombatDefenseService {
   public static final String TAG_GUARDING = "TypeMoonDefenseGuarding";
   public static final String TAG_GUARD_START = "TypeMoonDefenseGuardStart";
   public static final String TAG_LAST_RESULT = "TypeMoonDefenseLastResult";
   public static final String TAG_RESULT_UNTIL = "TypeMoonDefenseResultUntil";
   public static final String TAG_DODGE_COOLDOWN = "TypeMoonDefenseDodgeCooldown";
   public static final String TAG_GUARD_BROKEN = "TypeMoonDefenseGuardBroken";
   public static final String TAG_LAST_SYNC = "TypeMoonDefenseLastSync";
   public static final String TAG_LAST_DODGE = "TypeMoonDefenseLastDodge";
   public static final String TAG_LAST_GUARD_PACKET = "TypeMoonDefenseLastGuardPacket";
   public static final String TAG_STAMINA = "TypeMoonCombatStamina";
   public static final String TAG_POISE = "TypeMoonCombatPoise";
   public static final String RESULT_DODGE = "dodge";
   public static final String RESULT_GUARD = "guard";
   public static final String RESULT_PARRY = "parry";
   public static final String RESULT_BREAK = "break";
   public static final String RESULT_FAIL = "fail";

   private ServantCombatDefenseService() { }

   public static void tick(LivingEntity entity) {
      if (entity == null || entity.level().isClientSide()) return;
      CompoundTag data = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (data.getLong(TAG_GUARD_BROKEN) <= now) data.remove(TAG_GUARD_BROKEN);
      if (!entity.isAlive() || entity instanceof ServerPlayer player && !isCardActive(player)) {
         data.remove(TAG_GUARDING);
      }
      if (entity instanceof ServerPlayer && data.getBoolean(TAG_GUARDING)
         && now - data.getLong(TAG_LAST_GUARD_PACKET) > 10L) {
         data.remove(TAG_GUARDING);
      }
      if (entity instanceof ServerPlayer player && isCardActive(player)
         && (entity.tickCount % 5 == 0 || data.getLong(TAG_LAST_SYNC) != now)) {
         sync(player, false);
      }
   }

   public static boolean requestDodge(LivingEntity entity, float forward, float strafe) {
      if (entity == null || entity.level().isClientSide() || !entity.isAlive() || !canAct(entity)) return false;
      CompoundTag data = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (now < data.getLong(TAG_DODGE_COOLDOWN) || BindingService.isBound(entity.getUUID())) {
         setResult(entity, RESULT_FAIL, now + 8L);
         return false;
      }
      ServantParams params = params(entity);
      if (params == null) return false;
      double cost = ServantCombatFormulas.dodgeMpCost(params);
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!isCardActive(player) || !ServantCardManaService.consumeSilently(player, vars, cost)) return false;
      } else if (entity instanceof ServantEntity servant && servant.getCurrentMp() < cost) {
         return false;
      } else if (entity instanceof ServantEntity servant) {
         servant.setCurrentMp(servant.getCurrentMp() - cost);
      }
      int cooldown = ServantCombatFormulas.dodgeCooldownTicks(params);
      int invulnerability = ServantCombatFormulas.dodgeInvulnerabilityTicks(params);
      data.putLong(TAG_DODGE_COOLDOWN, now + cooldown);
      data.putLong(TAG_LAST_DODGE, now);
      data.putLong(ServantCombatSystem.TAG_INVULN_UNTIL, now + invulnerability);
      if (entity instanceof ServerPlayer) data.putLong("ServantCardCombatInvulnUntil", now + invulnerability);
      data.putBoolean(TAG_GUARDING, false);
      Vec3 direction = dodgeDirection(entity, forward, strafe);
      double distance = 0.95 + ServantCombatFormulas.agilityStep(params) * 0.08;
      entity.setDeltaMovement(direction.x * distance, Math.max(entity.getDeltaMovement().y, 0.08), direction.z * distance);
      entity.hurtMarked = true;
      setResult(entity, RESULT_DODGE, now + 12L);
      fx(entity, ParticleTypes.CLOUD, SoundEvents.PLAYER_ATTACK_SWEEP, 1.35F);
      if (entity instanceof ServerPlayer player && player.connection != null) sync(player, true);
      return true;
   }

   public static boolean setGuarding(LivingEntity entity, boolean active) {
      if (entity == null || entity.level().isClientSide()) return false;
      CompoundTag data = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (!active) {
         data.remove(TAG_GUARDING);
         if (entity instanceof ServerPlayer player) sync(player, true);
         return true;
      }
      if (!canAct(entity) || now < data.getLong(TAG_GUARD_BROKEN) || !hasGuardResource(entity)) {
         setResult(entity, RESULT_FAIL, now + 8L);
         return false;
      }
      if (!data.getBoolean(TAG_GUARDING)) data.putLong(TAG_GUARD_START, now);
      data.putBoolean(TAG_GUARDING, true);
      data.putLong(TAG_LAST_GUARD_PACKET, now);
      if (entity instanceof ServerPlayer player) sync(player, true);
      return true;
   }

   /** Returns null when no active guard applies, zero for a full block, or reduced damage. */
   public static Float applyGuardHit(LivingEntity defender, DamageSource source, float amount) {
      if (defender == null || source == null || amount <= 0.0F || !defender.getPersistentData().getBoolean(TAG_GUARDING)
         || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_ARMOR)
         || source.is(FanaticDamageTypes.GUARANTEED_HITS) || source.is(FanaticDamageTypes.BYPASSES_DEFENSES)) return null;
      if (source.getEntity() == defender || source.getDirectEntity() == defender) return null;
      long now = defender.level().getGameTime();
      CompoundTag data = defender.getPersistentData();
      if (now < data.getLong(TAG_GUARD_BROKEN) || !hasGuardResource(defender)) {
         data.remove(TAG_GUARDING);
         return null;
      }
      ServantParams params = params(defender);
      if (params == null) return null;
      double cost = ServantCombatFormulas.blockStaminaCost(params);
      double stamina = stamina(defender);
      boolean parry = now - data.getLong(TAG_GUARD_START) <= ServantCombatFormulas.parryWindowTicks(params);
      double actualCost = parry ? ServantCombatFormulas.parryStaminaCost(params) : cost;
      stamina = Math.max(0.0, stamina - actualCost);
      setStamina(defender, stamina);
      data.putLong(TAG_GUARD_START, now - 100L);
      if (stamina <= 0.0) {
         data.remove(TAG_GUARDING);
         data.putLong(TAG_GUARD_BROKEN, now + ServantCombatFormulas.guardBreakTicks(params));
         setResult(defender, RESULT_BREAK, now + 20L);
         fx(defender, ParticleTypes.CRIT, SoundEvents.SHIELD_BREAK, 0.9F);
         if (defender instanceof ServerPlayer player) sync(player, true);
         return amount;
      }
      if (parry && !source.is(DamageTypeTags.IS_EXPLOSION)) {
         setResult(defender, RESULT_PARRY, now + 16L);
         if (source.getEntity() instanceof LivingEntity attacker) {
            attacker.hurtMarked = true;
            if (attacker instanceof ServantEntity) {
               attacker.getPersistentData().putLong(ServantCombatSystem.TAG_STUN_UNTIL, now + 10L);
            }
         }
         fx(defender, ParticleTypes.CRIT, SoundEvents.SHIELD_BLOCK, 1.65F);
         if (defender instanceof ServerPlayer player) sync(player, true);
         return 0.0F;
      }
      setResult(defender, RESULT_GUARD, now + 12L);
      fx(defender, ParticleTypes.ENCHANT, SoundEvents.SHIELD_BLOCK, 1.1F);
      if (defender instanceof ServerPlayer player) sync(player, true);
      float reduced = amount * (float)(1.0 - ServantCombatFormulas.blockReduction(params));
      return source.is(DamageTypeTags.IS_EXPLOSION) ? Math.max(reduced, amount * 0.5F) : reduced;
   }

   public static boolean isGuarding(LivingEntity entity) {
      return entity != null && entity.getPersistentData().getBoolean(TAG_GUARDING);
   }

   public static void sync(ServerPlayer player, boolean force) {
      if (player == null || player.level().isClientSide()) return;
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (!force && now - data.getLong(TAG_LAST_SYNC) < 5L) return;
      data.putLong(TAG_LAST_SYNC, now);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ServantParams p = params(player);
      double maxStamina = p == null ? 0.0 : ServantCombatFormulas.staminaMax(p);
      double maxPoise = p == null ? 0.0 : ServantCombatFormulas.poiseMax(p);
      ModNetwork.sendToPlayer(player, new ServantCardDefenseStateMessage(stamina(player), maxStamina, poise(player), maxPoise,
         isGuarding(player), data.getLong(TAG_DODGE_COOLDOWN), data.getLong(TAG_GUARD_BROKEN),
         data.getString(TAG_LAST_RESULT), data.getLong(TAG_RESULT_UNTIL)));
   }

   public static void clear(LivingEntity entity) {
      if (entity == null) return;
      CompoundTag data = entity.getPersistentData();
      data.remove(TAG_GUARDING); data.remove(TAG_GUARD_START); data.remove(TAG_LAST_GUARD_PACKET);
      data.remove(TAG_LAST_RESULT); data.remove(TAG_RESULT_UNTIL);
      data.remove("ServantCardCombatGuardBrokenUntil");
      if (entity instanceof ServerPlayer player) sync(player, true);
   }

   private static boolean canAct(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         return isCardActive(player) && !player.isDeadOrDying() && !player.isPassenger()
            && !BindingService.isBound(player.getUUID())
            && !ServantCardActionTimeline.isActive(player);
      }
      return entity instanceof ServantEntity servant && !ServantCombatSystem.cannotAct(servant)
         && !ServantCombatSystem.skillsSuppressed(servant) && !BindingService.isBound(servant.getUUID());
   }

   private static boolean isCardActive(ServerPlayer player) {
      return player != null && player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed;
   }

   private static ServantParams params(LivingEntity entity) {
      if (entity instanceof ServerPlayer player) {
         String id = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_id;
         ServantDefinition definition = ServantDataRegistry.get(id);
         return definition == null ? null : definition.parameters();
      }
      if (entity instanceof ServantEntity servant) {
         ServantDefinition definition = servant.getDefinition();
         return definition == null ? null : definition.parameters();
      }
      return null;
   }

   private static double stamina(LivingEntity entity) {
      CompoundTag data = entity.getPersistentData();
      String tag = entity instanceof ServerPlayer ? "ServantCardCombatStamina" : TAG_STAMINA;
      return Math.max(0.0, data.getDouble(tag));
   }

   private static void setStamina(LivingEntity entity, double value) {
      entity.getPersistentData().putDouble(entity instanceof ServerPlayer ? "ServantCardCombatStamina" : TAG_STAMINA, Math.max(0.0, value));
   }

   private static double poise(LivingEntity entity) {
      String tag = entity instanceof ServerPlayer ? "ServantCardCombatPoise" : TAG_POISE;
      return Math.max(0.0, entity.getPersistentData().getDouble(tag));
   }

   private static boolean hasGuardResource(LivingEntity entity) {
      ServantParams p = params(entity);
      return p != null && stamina(entity) >= ServantCombatFormulas.parryStaminaCost(p);
   }

   private static Vec3 dodgeDirection(LivingEntity entity, float forward, float strafe) {
      Vec3 look = entity.getLookAngle();
      Vec3 forwardVector = new Vec3(look.x, 0.0, look.z);
      if (forwardVector.lengthSqr() < 1.0E-4) forwardVector = new Vec3(0.0, 0.0, 1.0);
      forwardVector = forwardVector.normalize();
      Vec3 right = new Vec3(-forwardVector.z, 0.0, forwardVector.x);
      Vec3 result = forwardVector.scale(forward).add(right.scale(strafe));
      if (result.lengthSqr() < 1.0E-4) result = forwardVector.scale(-1.0);
      return result.normalize();
   }

   private static void setResult(LivingEntity entity, String result, long until) {
      CompoundTag data = entity.getPersistentData();
      data.putString(TAG_LAST_RESULT, result);
      data.putLong(TAG_RESULT_UNTIL, until);
   }

   private static void fx(LivingEntity entity, net.minecraft.core.particles.ParticleOptions particle,
                          net.minecraft.sounds.SoundEvent sound, float pitch) {
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 12, 0.3, 0.35, 0.3, 0.03);
         level.playSound(null, entity.blockPosition(), sound, entity instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 0.75F, pitch);
      }
   }
}
