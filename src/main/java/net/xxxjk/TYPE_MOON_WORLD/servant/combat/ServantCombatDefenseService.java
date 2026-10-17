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
@net.neoforged.fml.common.EventBusSubscriber(modid = "typemoonworld")
public final class ServantCombatDefenseService {
   private static final String VERSION = "TypeMoonDefenseVersion";
   public static final String LAST_HIT = "TypeMoonDefenseLastHit";
   public static final String AUTO_CHARGES = "TypeMoonDefenseAutoCharges";
   public static final String AUTO_RESTORE = "TypeMoonDefenseAutoRestore";
   private static final String NPC_PARRY_READY = "TypeMoonDefenseNpcParryReady";
   private static final String NPC_PARRY_USED = "TypeMoonDefenseNpcParryUsed";
   private static final java.util.Map<net.neoforged.neoforge.common.damagesource.DamageContainer, Boolean> GUARDED = new java.util.WeakHashMap<>();
   private static final java.util.Map<LivingEntity, java.util.Deque<Boolean>> PENDING = new java.util.WeakHashMap<>();

   public static void initialize(LivingEntity entity) {
      ServantParams p = params(entity);
      if (p == null) return;
      CompoundTag d = entity.getPersistentData();
      String st = entity instanceof ServerPlayer ? "ServantCardCombatStamina" : TAG_STAMINA;
      String po = entity instanceof ServerPlayer ? "ServantCardCombatPoise" : TAG_POISE;
      double maxSt = ServantCombatFormulas.staminaMax(p);
      double maxPo = ServantCombatFormulas.poiseMax(p) * poiseMultiplier(entity);
      if (d.getInt(VERSION) < 2) {
         if (d.contains(st)) d.putDouble(st, d.getDouble(st) * 2);
         if (d.contains(po)) d.putDouble(po, d.getDouble(po) * 2);
         d.remove("ServantCardCombatGuardBrokenUntil");
         d.remove("TypeMoonCombatGuardBroken");
         d.putInt(VERSION, 2);
      }
      rescale(d, st, maxSt);
      rescale(d, po, maxPo);
      int available = ServantDefenseTraits.resolve(entity).autoDodges();
      if (!d.contains(AUTO_CHARGES) && available > 0) d.putInt(AUTO_CHARGES, available);
   }

   private static double poiseMultiplier(LivingEntity e) {
      double bonus = 1 + ServantDefenseTraits.resolve(e).durabilityBonus();
      if (e.getPersistentData().getBoolean("ChineseMartialArtsActive")) bonus = Math.max(bonus, 2);
      return bonus;
   }
   private static double recoveryMultiplier(LivingEntity e, boolean poise) {
      var traits = ServantDefenseTraits.resolve(e);
      double value = 1 + (poise ? traits.durabilityBonus() : traits.guardBonus());
      var definition = ServantDefenseTraits.definition(e);
      String id = definition == null ? "" : definition.id();
      if (id.equals("arash") || id.equals("lancelot_berserker")) value = Math.max(value, 2);
      if (id.equals("gilgamesh")) value = Math.max(value, poise ? 2 : 1.75);
      if (poise && e.getPersistentData().getBoolean("ChineseMartialArtsActive")) value = Math.max(value, 2);
      return value;
   }
   private static void rescale(CompoundTag d, String key, double limit) {
      if (!d.contains(key)) d.putDouble(key, limit);
      else if (d.contains(key + "Max") && d.getDouble(key + "Max") > 0)
         d.putDouble(key, Math.min(limit, d.getDouble(key) * limit / d.getDouble(key + "Max")));
      else d.putDouble(key, Math.min(limit, Math.max(0, d.getDouble(key))));
      d.putDouble(key + "Max", limit);
   }

   public static boolean broken(LivingEntity entity) { return entity.getPersistentData().getLong(TAG_GUARD_BROKEN) > 0; }

   public static boolean trySkillAutoDodge(LivingEntity entity) {
      initialize(entity);
      CompoundTag d = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (now < d.getLong(ServantCombatSystem.TAG_INVULN_UNTIL) || now < d.getLong("ServantCardCombatInvulnUntil")) return true;
      if (BindingService.isBound(entity.getUUID())) return false;
      int max = ServantDefenseTraits.resolve(entity).autoDodges();
      if (max == 0 || d.getInt(AUTO_CHARGES) <= 0) return false;
      int remaining = Math.min(max, d.getInt(AUTO_CHARGES)) - 1;
      d.putInt(AUTO_CHARGES, remaining);
      if (remaining == 0) d.putLong(AUTO_RESTORE, now + 400);
      setResult(entity, RESULT_DODGE, now + 12);
      if (entity instanceof ServerPlayer player) sync(player, true);
      return true;
   }

   /** Called once for the incoming damage container, preserving multi-hit semantics. */
   public static void markGuarded(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
      GUARDED.put(event.getContainer(), true);
   }

   @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
   public static void beforeDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
      LivingEntity e = event.getEntity();
      if (!(e instanceof ServantEntity) && !(e instanceof ServerPlayer p && isCardActive(p))) return;
      initialize(e);
      boolean guarded = GUARDED.remove(event.getContainer()) != null;
      PENDING.computeIfAbsent(e, unused -> new java.util.ArrayDeque<>()).push(!guarded && stamina(e) <= 0);
   }

   @net.neoforged.bus.api.SubscribeEvent
   public static void afterDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
      LivingEntity e = event.getEntity();
      java.util.Deque<Boolean> pending = PENDING.get(e);
      if (pending == null || pending.isEmpty()) return;
      boolean exhausted = pending.pop();
      if (pending.isEmpty()) PENDING.remove(e);
      if (event.getNewDamage() <= 0) return;
      e.getPersistentData().putLong(LAST_HIT, e.level().getGameTime());
      if (exhausted) losePoise(e, DefenseRules.poiseLoss(event.getNewDamage()));
   }

   public static void losePoise(LivingEntity e, double amount) {
      if (stamina(e) > 0 || broken(e) || amount <= 0) return;
      CompoundTag d = e.getPersistentData();
      String key = e instanceof ServerPlayer ? "ServantCardCombatPoise" : TAG_POISE;
      double value = Math.max(0, d.getDouble(key) - amount);
      d.putDouble(key, value);
      if (value <= 0) {
         d.putInt("TypeMoonDefenseBreakCount", d.getInt("TypeMoonDefenseBreakCount") + 1);
         d.putLong(TAG_GUARD_BROKEN, Long.MAX_VALUE);
         d.remove(TAG_GUARDING);
         setResult(e, RESULT_BREAK, e.level().getGameTime() + 20);
         fx(e, ParticleTypes.CRIT, SoundEvents.SHIELD_BREAK, 0.9F);
      }
   }

   @net.neoforged.bus.api.SubscribeEvent
   public static void cloneDefense(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
      var from = event.getOriginal().getPersistentData();
      var to = event.getEntity().getPersistentData();
      for (String key : from.getAllKeys()) if (key.startsWith("TypeMoonDefense") || key.startsWith("ServantCardCombat"))
         to.put(key, from.get(key).copy());
   }

   private static final long PLAYER_GUARD_PACKET_TIMEOUT_TICKS = 15L;
   private static final int PLAYER_PARRY_GRACE_TICKS = 3;
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
      if (entity instanceof ServantEntity servant) ServantDefenseTraits.tickNpcActiveSkills(servant);
      initialize(entity);
      ServantParams p = params(entity);
      if (p != null) {
         var traits = ServantDefenseTraits.resolve(entity);
         int max = traits.autoDodges();
         if (data.getInt(AUTO_CHARGES) <= 0 && data.getLong(AUTO_RESTORE) > 0 && now >= data.getLong(AUTO_RESTORE)) {
            data.putInt(AUTO_CHARGES, max);
            data.remove(AUTO_RESTORE);
         }
         // Activating or switching a skill never refills a depleted group.
         if (data.getInt(AUTO_CHARGES) == 0 && max > 0 && !data.contains(AUTO_RESTORE)) data.putLong(AUTO_RESTORE, now + 400);
         if (!data.contains(LAST_HIT) || now - data.getLong(LAST_HIT) >= 60) {
            String st = entity instanceof ServerPlayer ? "ServantCardCombatStamina" : TAG_STAMINA;
            String po = entity instanceof ServerPlayer ? "ServantCardCombatPoise" : TAG_POISE;
            data.putDouble(st, Math.min(data.getDouble(st + "Max"), stamina(entity) + ServantCombatFormulas.staminaRegenPerSecond(p) * recoveryMultiplier(entity, false) / 20));
            data.putDouble(po, Math.min(data.getDouble(po + "Max"), poise(entity) + ServantCombatFormulas.poiseRegenPerSecond(p) * recoveryMultiplier(entity, true) / 20));
            if (poise(entity) >= data.getDouble(po + "Max")) data.remove(TAG_GUARD_BROKEN);
         }
      }
      if (entity instanceof ServantEntity servant && entity.level() instanceof ServerLevel level) {
         boolean imminent = net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService.nearby(level, servant.position(), 24, now).stream()
            .anyMatch(threat -> threat.ticksToImpact(now) <= 10 && threat.ticksToImpact(now) >= 0
               && !threat.sourceUuid().equals(servant.getUUID()) && threat.blockable());
         if (imminent && !isGuarding(servant)) setGuarding(servant, true);
         else if (!imminent && now - data.getLong(TAG_GUARD_START) > 12) setGuarding(servant, false);
      }
      if (!entity.isAlive() || entity instanceof ServerPlayer player && !isCardActive(player)) {
         data.remove(TAG_GUARDING);
      }
      if (entity instanceof ServerPlayer && data.getBoolean(TAG_GUARDING)
         && now - data.getLong(TAG_LAST_GUARD_PACKET) > PLAYER_GUARD_PACKET_TIMEOUT_TICKS) {
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
      int cooldown = DefenseRules.safeDodgeCooldown((int)Math.ceil(ServantCombatFormulas.dodgeCooldownTicks(params)
         * (1 - ServantDefenseTraits.resolve(entity).dodgeReduction())),
         Math.max(ServantCombatFormulas.dodgeInvulnerabilityTicks(params) + 7, ServantCombatFormulas.perfectDodgeInvulnerabilityTicks(params)));
      int invulnerability = ServantCombatFormulas.dodgeInvulnerabilityTicks(params)
         + (entity instanceof ServerPlayer ? 2 : 0);
      data.putLong(TAG_DODGE_COOLDOWN, now + cooldown);
      data.putLong(TAG_LAST_DODGE, now);
      data.putLong("TypeMoonCombatLastDodgeTick", now);
      // Keep the legacy NPC reaction path from immediately spending a second
      // dodge on the same incoming hit after a tactical dodge was committed.
      if (entity instanceof ServantEntity) {
         data.putLong("TypeMoonCombatLastDodgeTick", now);
      }
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
      if (!canAct(entity) || broken(entity) || !hasGuardResource(entity)) {
         setResult(entity, RESULT_FAIL, now + 8L);
         return false;
      }
      if (!data.getBoolean(TAG_GUARDING)) {
         data.putLong(TAG_GUARD_START, now);
         if (entity instanceof ServantEntity) {
            boolean ready = now >= data.getLong(NPC_PARRY_READY);
            data.putBoolean(NPC_PARRY_USED, !ready);
            if (ready) data.putLong(NPC_PARRY_READY, now + 40);
         }
      }
      data.putBoolean(TAG_GUARDING, true);
      data.putLong(TAG_LAST_GUARD_PACKET, now);
      if (entity instanceof ServerPlayer player) sync(player, true);
      return true;
   }

   /** Active, automatic and perfect guards share the same resource transaction. */
   public static Float applyGuardHit(LivingEntity defender, DamageSource source, float amount) {
      if (defender == null || source == null || amount <= 0 || source.getEntity() == defender
         || source.getEntity() == null && source.getDirectEntity() == null
         || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.BYPASSES_ARMOR)
         || source.is(FanaticDamageTypes.GUARANTEED_HITS) || source.is(FanaticDamageTypes.BYPASSES_DEFENSES)) return null;
      initialize(defender);
      CompoundTag data = defender.getPersistentData();
      long now = defender.level().getGameTime();
      data.putLong(LAST_HIT, now);
      if (broken(defender) || stamina(defender) <= 0) return null;
      ServantParams p = params(defender);
      if (p == null) return null;
      boolean active = isGuarding(defender);
      int window = defender instanceof ServerPlayer ? ServantCombatFormulas.parryWindowTicks(p) + PLAYER_PARRY_GRACE_TICKS : 10;
      boolean perfect = active && now - data.getLong(TAG_GUARD_START) <= window
         && (!(defender instanceof ServantEntity) || !data.getBoolean(NPC_PARRY_USED));
      double cost = DefenseRules.guardCost(ServantCombatFormulas.blockStaminaCost(p), ServantDefenseTraits.resolve(defender).guardBonus(), active, perfect);
      setStamina(defender, Math.max(0, stamina(defender) - cost));
      data.putInt("TypeMoonDefenseGuardCount", data.getInt("TypeMoonDefenseGuardCount") + 1);
      if (perfect) {
         data.putInt("TypeMoonDefenseParryCount", data.getInt("TypeMoonDefenseParryCount") + 1);
         data.putLong(TAG_GUARD_START, now - 100);
         data.putBoolean(NPC_PARRY_USED, true);
         setResult(defender, RESULT_PARRY, now + 16);
      } else setResult(defender, RESULT_GUARD, now + 12);
      fx(defender, perfect ? ParticleTypes.CRIT : ParticleTypes.ENCHANT, SoundEvents.SHIELD_BLOCK, perfect ? 1.65F : 1.1F);
      if (defender instanceof ServerPlayer player) sync(player, true);
      // The hit which empties defense still receives its reduction.
      return DefenseRules.guardedDamage(amount, active, perfect);
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
      double maxPoise = p == null ? 0.0 : ServantCombatFormulas.poiseMax(p) * poiseMultiplier(player);
      boolean gilgamesh = "gilgamesh".equals(vars.servant_card_id) || "gilgamesh_caster".equals(vars.servant_card_id);
      double shieldHp = gilgamesh ? Math.max(0.0, data.getFloat(GilgameshDivineShield.TAG_HP)) : 0.0;
      double shieldMax = gilgamesh ? GilgameshDivineShield.MAX_HP : 0.0;
      boolean shieldActive = gilgamesh && GilgameshDivineShield.isActive(player);
      ModNetwork.sendToPlayer(player, new ServantCardDefenseStateMessage(stamina(player), maxStamina, poise(player), maxPoise,
         isGuarding(player), data.getLong(TAG_DODGE_COOLDOWN), data.getLong(TAG_GUARD_BROKEN),
         data.getString(TAG_LAST_RESULT), data.getLong(TAG_RESULT_UNTIL), shieldHp, shieldMax, shieldActive, data.getInt(AUTO_CHARGES), ServantDefenseTraits.resolve(player).autoDodges(), data.getLong(AUTO_RESTORE), broken(player)));
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
            && !BindingService.isBound(player.getUUID());
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
      return p != null && stamina(entity) > 0 && !broken(entity);
   }

   private static Vec3 dodgeDirection(LivingEntity entity, float forward, float strafe) {
      Vec3 look = entity.getLookAngle();
      Vec3 forwardVector = new Vec3(look.x, 0.0, look.z);
      if (forwardVector.lengthSqr() < 1.0E-4) forwardVector = new Vec3(0.0, 0.0, 1.0);
      forwardVector = forwardVector.normalize();
      // Minecraft's positive strafe impulse is the left key (A).  The old
      // basis treated it as right, so A/D dodges were visibly reversed.
      Vec3 left = new Vec3(forwardVector.z, 0.0, -forwardVector.x);
      Vec3 result = forwardVector.scale(forward).add(left.scale(strafe));
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
