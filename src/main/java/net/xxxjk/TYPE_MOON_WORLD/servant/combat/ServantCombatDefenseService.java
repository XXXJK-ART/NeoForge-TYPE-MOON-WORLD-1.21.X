package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardDefenseStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardGuardPoseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantDefenseAnimationMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardActionTimeline;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanPersonaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreat;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.CombatThreatService;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ProjectileThreatClassifier;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.DefenseRules.NpcReaction;

/** Server-authoritative active dodge and guard state shared by cards and NPCs. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "typemoonworld")
public final class ServantCombatDefenseService {
   private static final String VERSION = "TypeMoonDefenseVersion";
   public static final String LAST_HIT = "TypeMoonDefenseLastHit";
   public static final String AUTO_CHARGES = "TypeMoonDefenseAutoCharges";
   public static final String AUTO_RESTORE = "TypeMoonDefenseAutoRestore";
   private static final String NPC_PARRY_USED = "TypeMoonDefenseNpcParryUsed";
   private static final String NPC_GUARD_UNTIL = "TypeMoonDefenseNpcGuardUntil";
   private static final int NPC_PARRY_WINDOW_TICKS = 10;
   private static final int NPC_REACTION_INTERVAL_TICKS = 16;
   private static final java.util.Map<ServantEntity, NpcDefenseMemory> NPC_REACTIONS = new java.util.WeakHashMap<>();
   private static final class NpcDefenseMemory {
      long nextDecision;
      final java.util.Map<Object, NpcDecision> decisions = new java.util.LinkedHashMap<>();
   }
   private static final class NpcDecision {
      final NpcReaction reaction;
      final boolean fallbackGuard;
      final long expires;
      boolean attempted;
      NpcDecision(NpcReaction reaction, boolean fallbackGuard, long expires) {
         this.reaction = reaction; this.fallbackGuard = fallbackGuard; this.expires = expires;
      }
   }
   private record ThreatKey(java.util.UUID source, net.minecraft.resources.ResourceLocation action, long start, long impact) { }
   private record HitKey(java.util.UUID source, java.util.UUID direct, long tick) { }
   private static final java.util.Map<net.neoforged.neoforge.common.damagesource.DamageContainer, Boolean> GUARDED = new java.util.WeakHashMap<>();
   private static final java.util.Map<net.neoforged.neoforge.common.damagesource.DamageContainer,
      java.util.function.Predicate<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent>> AFTER_ARMOR = new java.util.WeakHashMap<>();
   private record PendingDamage(boolean exhausted, boolean guarded, float postDefenseDamage) { }
   private static final java.util.Map<LivingEntity, java.util.Deque<PendingDamage>> PENDING = new java.util.WeakHashMap<>();

   public static void initialize(LivingEntity entity) {
      if (isPersona(entity)) {
         clearPersonaDefenseState(entity);
         return;
      }
      ServantParams p = params(entity);
      if (p == null) return;
      CompoundTag d = entity.getPersistentData();
      String st = entity instanceof ServerPlayer ? "ServantCardCombatStamina" : TAG_STAMINA;
      String po = entity instanceof ServerPlayer ? "ServantCardCombatPoise" : TAG_POISE;
      double maxSt = ServantCombatFormulas.staminaMax(p);
      double maxPo = ServantCombatFormulas.poiseMax(p) * poiseMultiplier(entity);
      if (d.getInt(VERSION) < 2) {
         d.remove("ServantCardCombatGuardBrokenUntil");
         d.remove("TypeMoonCombatGuardBroken");
         d.putInt(VERSION, 2);
      }
      if (d.getInt(VERSION) < 3) {
         if (d.contains(st)) d.putDouble(st, d.getDouble(st) * 0.75);
         if (d.contains(po)) d.putDouble(po, d.getDouble(po) * 0.75);
         d.putInt(VERSION, 3);
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

   /** Shared undodgeable conditions for counted servant skill evasion. */
   public static boolean trySkillAutoDodge(LivingEntity entity, DamageSource source, float amount) {
      if (isPersona(entity)) return false;
      if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(FanaticDamageTypes.GUARANTEED_HITS)
         || source.is(FanaticDamageTypes.BYPASSES_DEFENSES)
         || net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService.poison(source)
         || net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.isInfection(source)
         || net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper.isOriginBulletDamage(source)
         || NoblePhantasmDamageClassifier.isNoblePhantasmDamage(source,amount)
         || entity.getPersistentData().getLong(net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.TAG_DIVINE_BREAK_UNTIL)>entity.level().getGameTime()) return false;
      return trySkillAutoDodge(entity);
   }

   public static boolean trySkillAutoDodge(LivingEntity entity) {
      if (isPersona(entity)) return false;
      net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService.syncInstinct(entity);
      initialize(entity);
      CompoundTag d = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (now < d.getLong(ServantCombatSystem.TAG_INVULN_UNTIL) || now < d.getLong("ServantCardCombatInvulnUntil")) return true;
      if (BindingService.isBound(entity.getUUID())) return false;
      int max = ServantDefenseTraits.resolve(entity).autoDodges();
      if (max == 0 || d.getInt(AUTO_CHARGES) <= 0) return false;
      int remaining = Math.min(max, d.getInt(AUTO_CHARGES)) - 1;
      d.putInt(AUTO_CHARGES, remaining);
      if (remaining == 0) d.putLong(AUTO_RESTORE, now + (net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService.isArtoria(entity) ? 300 : 400));
      net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService.recordDodge(entity, remaining);
      setResult(entity, RESULT_DODGE, now + 12);
      if (entity instanceof ServerPlayer player) sync(player, true);
      return true;
   }

   /** Called once for the incoming damage container, preserving multi-hit semantics. */
   public static void markGuarded(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
      GUARDED.put(event.getContainer(), true);
   }

   /** Keep each hit's eligibility, but resolve its reaction only after vanilla base defenses. */
   public static void afterArmor(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event,
      java.util.function.Predicate<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> defense) {
      AFTER_ARMOR.put(event.getContainer(), defense);
   }

   @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
   public static void resolveDefense(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
      var defense = AFTER_ARMOR.remove(event.getContainer());
      if (defense == null || event.getNewDamage() <= 0.0F) return;
      // Reuse the defense handlers with this hit's real, already reduced container.
      // This is the same synchronous hurt call, not a later tick or another hurt().
      var hit = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(event.getEntity(), event.getContainer());
      if (defense.test(hit) || hit.isCanceled()) event.setNewDamage(0.0F);
   }

   @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
   public static void beforeDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
      LivingEntity e = event.getEntity();
      if (isPersona(e)) return;
      if (!(e instanceof ServantEntity) && !(e instanceof ServerPlayer p && isCardActive(p))) return;
      initialize(e);
      boolean guarded = GUARDED.remove(event.getContainer()) != null;
      PENDING.computeIfAbsent(e, unused -> new java.util.ArrayDeque<>()).push(
         new PendingDamage(!guarded && stamina(e) <= 0, guarded, event.getNewDamage()));
   }

   @net.neoforged.bus.api.SubscribeEvent
   public static void afterDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
      LivingEntity e = event.getEntity();
      java.util.Deque<PendingDamage> pending = PENDING.get(e);
      if (pending == null || pending.isEmpty()) return;
      PendingDamage hit = pending.pop();
      if (pending.isEmpty()) PENDING.remove(e);
      if (hit.exhausted() && hit.postDefenseDamage() > 0) {
         e.getPersistentData().putLong(LAST_HIT, e.level().getGameTime());
         losePoise(e, DefenseRules.poiseLoss(hit.postDefenseDamage()));
      }
      if (event.getNewDamage() <= 0) return;
      e.getPersistentData().putLong(LAST_HIT, e.level().getGameTime());
      if (event.getSource().getEntity() instanceof ServantEntity attacker) {
         ServantCombatExchangeService.recordContact(attacker, e, e.level().getGameTime());
         if (!hit.guarded() && event.getSource().getDirectEntity() == attacker
            && attacker.distanceTo(e) <= 5) ServantCombatExchangeService.meleeHit(attacker, e);
      }
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
         d.putBoolean("TypeMoonCombatGuardBroken", true);
         ServantParams brokenParams = params(e);
         d.putLong(ServantCombatSystem.TAG_STUN_UNTIL,
            e.level().getGameTime() + ServantCombatFormulas.guardBreakTicks(brokenParams));
         d.remove(TAG_GUARDING);
         if (e instanceof ServantEntity servant) servant.setCombatPose(0, 0);
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
   public static final int GUARD_RELEASE_COOLDOWN_TICKS = 20;
   public static final String TAG_GUARD_COOLDOWN = "TypeMoonDefenseGuardCooldown";
   private static final String TAG_GUARD_PARRY_RELEASE = "TypeMoonDefenseGuardParryRelease";
   public static final String TAG_GUARDING = "TypeMoonDefenseGuarding";
   public static final String TAG_GUARD_START = "TypeMoonDefenseGuardStart";
   public static final String TAG_LAST_RESULT = "TypeMoonDefenseLastResult";
   public static final String TAG_RESULT_UNTIL = "TypeMoonDefenseResultUntil";
   public static final String TAG_DODGE_COOLDOWN = "TypeMoonDefenseDodgeCooldown";
   public static final String TAG_DODGE_UNTIL = "TypeMoonDefenseDodgeUntil";
   private static final String TAG_DODGE_X = "TypeMoonDefenseDodgeX";
   private static final String TAG_DODGE_Z = "TypeMoonDefenseDodgeZ";
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
      net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService.syncInstinct(entity);
      continueNpcDodge(entity, data, now);
      if (entity instanceof ServantEntity servant) ServantDefenseTraits.tickNpcActiveSkills(servant);
      initialize(entity);
      ServantParams p = params(entity);
      if (p != null && !isPersona(entity)) {
         var traits = ServantDefenseTraits.resolve(entity);
         int max = traits.autoDodges();
         if (data.getInt(AUTO_CHARGES) <= 0 && data.getLong(AUTO_RESTORE) > 0 && now >= data.getLong(AUTO_RESTORE)) {
            data.putInt(AUTO_CHARGES, max);
            data.remove(AUTO_RESTORE);
         }
         // Activating or switching a skill never refills a depleted group.
         if (data.getInt(AUTO_CHARGES) == 0 && max > 0 && !data.contains(AUTO_RESTORE)) data.putLong(AUTO_RESTORE, now + (net.xxxjk.TYPE_MOON_WORLD.servant.artoria.ArtoriaStateService.isArtoria(entity) ? 300 : 400));
         if (!data.contains(LAST_HIT) || now - data.getLong(LAST_HIT) >= 60) {
            String st = entity instanceof ServerPlayer ? "ServantCardCombatStamina" : TAG_STAMINA;
            String po = entity instanceof ServerPlayer ? "ServantCardCombatPoise" : TAG_POISE;
            data.putDouble(st, Math.min(data.getDouble(st + "Max"), stamina(entity) + ServantCombatFormulas.staminaRegenPerSecond(p) * recoveryMultiplier(entity, false) / 20));
            data.putDouble(po, Math.min(data.getDouble(po + "Max"), poise(entity) + ServantCombatFormulas.poiseRegenPerSecond(p) * recoveryMultiplier(entity, true) / 20));
            if (poise(entity) >= data.getDouble(po + "Max")) {
               data.remove(TAG_GUARD_BROKEN);
               data.remove("TypeMoonCombatGuardBroken");
            }
         }
      }
      if (entity instanceof ServantEntity servant && entity.level() instanceof ServerLevel level) {
         // A telegraph cannot keep renewing a guard and suppress attacks forever.
         if (isGuarding(servant) && (now >= data.getLong(NPC_GUARD_UNTIL) || !canAct(servant))) setGuarding(servant, false);
         CombatThreat threat = CombatThreatService.incoming(level, servant, now, 10,
            incoming -> incoming.activeAt(now) && level.getEntity(incoming.sourceUuid()) instanceof LivingEntity source
               && !servant.isAlliedTo(source));
         if (threat != null && !isGuarding(servant)) reactToThreat(servant, threat);
      }
      if (!entity.isAlive() || entity instanceof ServerPlayer player && !isCardActive(player)) {
         data.remove(TAG_GUARDING);
      }
      if (entity instanceof ServerPlayer && data.getBoolean(TAG_GUARDING)
         && now - data.getLong(TAG_LAST_GUARD_PACKET) > PLAYER_GUARD_PACKET_TIMEOUT_TICKS) {
         setGuarding(entity, false);
      }
      if (entity instanceof ServerPlayer player && isCardActive(player)
         && (entity.tickCount % 5 == 0 || data.getLong(TAG_LAST_SYNC) != now)) {
         sync(player, false);
      }
   }

   public static boolean requestDodge(LivingEntity entity, float forward, float strafe) {
      if (entity != null && net.xxxjk.TYPE_MOON_WORLD.servant.heracles.HeraclesAbilityService.motionLocked(entity)) return false;
      if (entity == null || entity.level().isClientSide() || !entity.isAlive() || !canAct(entity)) return false;
      CompoundTag data = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (now < data.getLong(TAG_DODGE_COOLDOWN) || BindingService.isBound(entity.getUUID())) {
         setResult(entity, RESULT_FAIL, now + 8L);
         return false;
      }
      ServantParams params = params(entity);
      if (params == null) return false;
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         double cost = ServantCombatFormulas.dodgeMpCost(params);
         if (!isCardActive(player) || !ServantCardManaService.consumeSilently(player, vars, cost)) return false;
      } else if (entity instanceof ServantEntity servant && !isPersona(entity)) {
         double cost = ServantCombatFormulas.dodgeMpCost(params);
         if (servant.getCurrentMp() < cost) return false;
         servant.setCurrentMp(servant.getCurrentMp() - cost);
      }
      int cooldown = DefenseRules.safeDodgeCooldown((int)Math.ceil(ServantCombatFormulas.dodgeCooldownTicks(params)
         * (1 - ServantDefenseTraits.resolve(entity).dodgeReduction())),
         Math.max(ServantCombatFormulas.dodgeInvulnerabilityTicks(params) + 7, ServantCombatFormulas.perfectDodgeInvulnerabilityTicks(params)));
      int invulnerability = ServantCombatFormulas.dodgeInvulnerabilityTicks(params)
         + (entity instanceof ServerPlayer ? 2 : 0);
      data.putLong(TAG_DODGE_COOLDOWN, now + cooldown);
      data.putLong(TAG_LAST_DODGE, now);
      ServantCombatExchangeService.activity(entity, now);
      data.putLong("TypeMoonCombatLastDodgeTick", now);
      // Keep the legacy NPC reaction path from immediately spending a second
      // dodge on the same incoming hit after a tactical dodge was committed.
      if (entity instanceof ServantEntity) {
         data.putLong("TypeMoonCombatLastDodgeTick", now);
      }
      data.putLong(ServantCombatSystem.TAG_INVULN_UNTIL, now + invulnerability);
      if (entity instanceof ServerPlayer) data.putLong("ServantCardCombatInvulnUntil", now + invulnerability);
      data.putBoolean(TAG_GUARDING, false);
      if (entity instanceof ServantEntity servant) servant.setCombatPose(1, 6);
      Vec3 direction = dodgeDirection(entity, forward, strafe);
      double distance = 1.35 + ServantCombatFormulas.agilityStep(params) * 0.12;
      beginDodgeMotion(entity, data, direction, distance, now);
      setResult(entity, RESULT_DODGE, now + 12L);
      fx(entity, ParticleTypes.CLOUD, SoundEvents.PLAYER_ATTACK_SWEEP, 1.35F);
      if (entity instanceof ServerPlayer player && player.connection != null) sync(player, true);
      return true;
   }

   public static boolean setGuarding(LivingEntity entity, boolean active) {
      if (isPersona(entity)) return false;
      if (active && entity != null && net.xxxjk.TYPE_MOON_WORLD.servant.heracles.HeraclesAbilityService.motionLocked(entity)) return false;
      if (entity == null || entity.level().isClientSide()) return false;
      CompoundTag data = entity.getPersistentData();
      long now = entity.level().getGameTime();
      if (!active) {
         if (data.getBoolean(TAG_GUARDING)) {
            if (!data.getBoolean(TAG_GUARD_PARRY_RELEASE)) data.putLong(TAG_GUARD_COOLDOWN, now + GUARD_RELEASE_COOLDOWN_TICKS);
            else data.remove(TAG_GUARD_COOLDOWN);
         }
         data.remove(TAG_GUARD_PARRY_RELEASE);
         data.remove(TAG_GUARDING);
         data.remove(NPC_GUARD_UNTIL);
         broadcastDefenseAnimation(entity, 0);
         if (entity instanceof ServantEntity servant && servant.getCombatPose() == 2) servant.setCombatPose(0, 0);
         if (entity instanceof ServerPlayer player) sync(player, true);
         return true;
      }
      if (!canAct(entity) || broken(entity) || !hasGuardResource(entity)
         || !data.getBoolean(TAG_GUARDING) && now < data.getLong(TAG_GUARD_COOLDOWN)) {
         setResult(entity, RESULT_FAIL, now + 8L);
         return false;
      }
      if (!data.getBoolean(TAG_GUARDING)) {
         data.putLong(TAG_GUARD_START, now);
         data.remove(TAG_GUARD_PARRY_RELEASE);
         if (entity instanceof ServantEntity) {
            data.putBoolean(NPC_PARRY_USED, false);
         }
      }
      data.putBoolean(TAG_GUARDING, true);
      if (entity instanceof ServantEntity) data.putLong(NPC_GUARD_UNTIL, now + NPC_PARRY_WINDOW_TICKS);
      if (entity instanceof ServantEntity servant) servant.setCombatPose(2, 6);
      ServantCombatExchangeService.activity(entity, now);
      data.putLong(TAG_LAST_GUARD_PACKET, now);
      if (entity instanceof ServerPlayer player) sync(player, true);
      return true;
   }

   private static void broadcastDefenseAnimation(LivingEntity entity, int animation) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      ServantDefenseAnimationMessage payload = new ServantDefenseAnimationMessage(entity.getUUID(), animation);
      for (ServerPlayer viewer : level.players()) {
         if (viewer.distanceToSqr(entity) <= 128.0 * 128.0) ModNetwork.sendToPlayer(viewer, payload);
      }
   }

   /** Also covers native techniques and hitscan shots that publish no shared telegraph. */
   public static boolean prepareNpcGuard(ServantEntity defender, DamageSource source, float amount) {
      if (defender == null) return false;
      return reactToIncomingAttack(defender, source, amount, true, true, true) == NpcReaction.GUARD;
   }

   public static NpcReaction reactToIncomingAttack(ServantEntity defender, DamageSource source, float amount,
                                                  boolean allowGuard, boolean allowDodge) {
      return reactToIncomingAttack(defender, source, amount, allowGuard, allowDodge, false);
   }

   private static NpcReaction reactToIncomingAttack(ServantEntity defender, DamageSource source, float amount,
                                                   boolean allowGuard, boolean allowDodge, boolean guardOnly) {
      if (defender == null || source == null || defender.level().isClientSide() || !canAct(defender)
         || !Float.isFinite(amount) || amount <= 0) return NpcReaction.NONE;
      if (isPersona(defender)) allowGuard = false;
      if (allowGuard && isGuarding(defender) && canGuardHit(defender, source, amount)) return NpcReaction.GUARD;
      boolean melee = source.getEntity() instanceof LivingEntity attacker
         && source.getDirectEntity() == attacker && defender.distanceTo(attacker) <= 6.0;
      boolean bypass = net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService.poison(source)
         || net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.isInfection(source)
         || net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper.isOriginBulletDamage(source)
         || source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypeTags.IS_PROJECTILE);
      boolean guard = allowGuard && canGuardHit(defender, source, amount) && DefenseRules.npcGuardCandidate(amount, melee, bypass,
         NoblePhantasmDamageClassifier.typeOf(source) != null
            || NoblePhantasmDamageClassifier.isNoblePhantasmDamage(source, amount),
         source.is(DamageTypeTags.IS_EXPLOSION));
      boolean dodge = allowDodge && !bypass && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         && !source.is(FanaticDamageTypes.GUARANTEED_HITS) && !source.is(FanaticDamageTypes.BYPASSES_DEFENSES)
         && !net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService.isTsubameGaeshi(source)
         && source.getEntity() != defender && source.getDirectEntity() != defender
         && (source.getEntity() != null || source.getDirectEntity() != null);
      if (!guard && !dodge) return NpcReaction.NONE;
      long now = defender.level().getGameTime();
      Entity direct = source.getDirectEntity();
      CombatThreat threat = CombatThreatService.incoming((ServerLevel)defender.level(), defender, now, 10,
         incoming -> source.getEntity() != null && incoming.sourceUuid().equals(source.getEntity().getUUID()));
      Object key = threat != null ? threatKey(threat) : ProjectileThreatClassifier.isProjectileLike(direct) ? direct.getUUID()
         : new HitKey(source.getEntity() == null ? null : source.getEntity().getUUID(),
            direct == null ? null : direct.getUUID(), now);
      int danger = threat != null ? threat.danger() : source.is(DamageTypeTags.IS_EXPLOSION)
         || NoblePhantasmDamageClassifier.isNoblePhantasmDamage(source, amount) ? 5 : 1;
      NpcDecision decision = npcDecision(defender, key, threat == null ? now + 16 : threat.endTick() + 2,
         amount, danger, guard, dodge);
      if (guardOnly && decision.reaction != NpcReaction.GUARD) return NpcReaction.NONE;
      return executeNpcDecision(defender, decision, guard, dodge);
   }

   public static NpcReaction reactToThreat(ServantEntity defender, CombatThreat threat) {
      if (!canAct(defender) || isGuarding(defender)) return NpcReaction.NONE;
      boolean noblePhantasm = threat.actionId().getPath().startsWith("noble_phantasm/");
      NpcDecision decision = npcDecision(defender, threatKey(threat), threat.endTick() + 2,
         0, threat.danger(), !isPersona(defender) && threat.blockable() && !noblePhantasm, threat.dodgeable());
      // Choose once, but time the movement so its existing invulnerability
      // window actually covers impact rather than expiring during the windup.
      long lead = decision.reaction == NpcReaction.DODGE
         ? Math.max(1, ServantCombatFormulas.dodgeInvulnerabilityTicks(params(defender)) - 1) : 8;
      if (threat.ticksToImpact(defender.level().getGameTime()) > lead) return NpcReaction.NONE;
      return executeNpcDecision(defender, decision);
   }

   /** Called only after the projectile sensor has verified an incoming hostile trajectory. */
   public static boolean reactToProjectile(ServantEntity defender, Entity projectile, double damage,
                                            java.util.Set<net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactBypass> bypasses) {
      if (!canAct(defender) || isGuarding(defender)) return false;
      boolean special = ProjectileThreatClassifier.bypassesProjectileNegation(bypasses);
      boolean sureHit = bypasses.contains(net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactBypass.SURE_HIT);
      return executeNpcDecision(defender, npcDecision(defender, projectile.getUUID(), defender.level().getGameTime() + 40,
         damage, special ? 5 : 1, !isPersona(defender) && !special && damage <= DefenseRules.NPC_LIGHT_RANGED_DAMAGE_LIMIT, !sureHit)) != NpcReaction.NONE;
   }

   public static double estimatedProjectileDamage(Entity projectile) {
      if (projectile instanceof AbstractArrow arrow) return arrow.getBaseDamage() * arrow.getDeltaMovement().length();
      if (projectile instanceof net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity gate) return gate.getDamage();
      return 30.0;
   }

   private static ThreatKey threatKey(CombatThreat threat) {
      return new ThreatKey(threat.sourceUuid(), threat.actionId(), threat.startTick(), threat.impactTick());
   }

   private static NpcDecision npcDecision(ServantEntity defender, Object key, long expires, double damage,
                                          int danger, boolean guard, boolean dodge) {
      long now = defender.level().getGameTime();
      NpcDefenseMemory memory = NPC_REACTIONS.computeIfAbsent(defender, ignored -> new NpcDefenseMemory());
      memory.decisions.values().removeIf(decision -> now > decision.expires);
      NpcDecision previous = memory.decisions.get(key);
      if (previous != null) return previous;
      if (now < memory.nextDecision) {
         NpcDecision skipped = new NpcDecision(NpcReaction.NONE, false, expires);
         if (memory.decisions.size() >= 32) memory.decisions.remove(memory.decisions.keySet().iterator().next());
         memory.decisions.put(key, skipped);
         return skipped;
      }
      initialize(defender);
      ServantParams p = params(defender);
      var chances = DefenseRules.npcReactionChances(ServantCombatFormulas.defenseStep(p),
         ServantCombatFormulas.agilityStep(p), damage, defender.getMaxHealth(), danger, guard, dodge);
      if (isPersona(defender)) {
         chances = new DefenseRules.NpcReactionChances(chances.guard(),
            chances.dodge() * net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules.PERSONA_DODGE_CHANCE_MULTIPLIER);
      }
      double dodgeRoll = defender.getRandom().nextDouble();
      double guardRoll = defender.getRandom().nextDouble();
      NpcDecision decision = new NpcDecision(chances.choose(dodgeRoll, guardRoll), guardRoll < chances.guard(), expires);
      memory.nextDecision = now + NPC_REACTION_INTERVAL_TICKS;
      if (memory.decisions.size() >= 32) memory.decisions.remove(memory.decisions.keySet().iterator().next());
      memory.decisions.put(key, decision);
      return decision;
   }

   private static NpcReaction executeNpcDecision(ServantEntity defender, NpcDecision decision) {
      return executeNpcDecision(defender, decision, true, true);
   }

   private static NpcReaction executeNpcDecision(ServantEntity defender, NpcDecision decision,
                                                  boolean allowGuard, boolean allowDodge) {
      if (decision.attempted) return NpcReaction.NONE;
      decision.attempted = true;
      NpcReaction executed = NpcReaction.NONE;
      if (decision.reaction == NpcReaction.DODGE && allowDodge && requestDodge(defender, 0.0F, 1.0F)) {
         executed = NpcReaction.DODGE;
      } else if (decision.fallbackGuard && allowGuard && setGuarding(defender, true)) {
         // The same cached guard roll also covers an evasion rejected by cooldown or MP.
         executed = NpcReaction.GUARD;
      }
      if (executed != NpcReaction.NONE) {
         defender.getNavigation().stop();
         net.xxxjk.TYPE_MOON_WORLD.combat.ai.ServantPlannedActionExecutor.interrupt(defender, 5, defender.level().getGameTime());
      }
      return executed;
   }

   private static boolean canGuardHit(LivingEntity defender, DamageSource source, float amount) {
      boolean guardable = defender != null && source != null && Float.isFinite(amount) && amount > 0
         && source.getEntity() != defender && source.getDirectEntity() != defender
         && (source.getEntity() != null || source.getDirectEntity() != null)
         && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         && !source.is(FanaticDamageTypes.GUARANTEED_HITS) && !source.is(FanaticDamageTypes.BYPASSES_DEFENSES)
         && !net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService.isTsubameGaeshi(source);
      if (!guardable) return false;
      // Ordinary low-damage magic bolts bypass armor, not an active weapon guard.
      return !source.is(DamageTypeTags.BYPASSES_ARMOR)
         || source.is(DamageTypes.INDIRECT_MAGIC) && amount <= DefenseRules.NPC_LIGHT_RANGED_DAMAGE_LIMIT
            && !NoblePhantasmDamageClassifier.isNoblePhantasmDamage(source, amount)
            && NoblePhantasmDamageClassifier.typeOf(source) == null;
   }

   /** Active, automatic and perfect guards share the same resource transaction. */
   public static Float applyGuardHit(LivingEntity defender, DamageSource source, float amount) {
      if (isPersona(defender)) return null;
      if (!canGuardHit(defender, source, amount)) return null;
      initialize(defender);
      CompoundTag data = defender.getPersistentData();
      long now = defender.level().getGameTime();
      data.putLong(LAST_HIT, now);
      if (broken(defender) || stamina(defender) <= 0) return null;
      ServantParams p = params(defender);
      if (p == null) return null;
      boolean active = isGuarding(defender) && canAct(defender);
      // Both guard modes spend the damage remaining after base defense / 5.
      int window = defender instanceof ServerPlayer ? ServantCombatFormulas.parryWindowTicks(p) : NPC_PARRY_WINDOW_TICKS;
      boolean perfect = active && now - data.getLong(TAG_GUARD_START) <= window
         && (!(defender instanceof ServantEntity) || !data.getBoolean(NPC_PARRY_USED));
      double cost = DefenseRules.guardCost(amount, ServantDefenseTraits.resolve(defender).guardBonus(),
         active, perfect, net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService.souwaBonus(source.getEntity() instanceof LivingEntity attacker ? attacker : null));
      setStamina(defender, Math.max(0, stamina(defender) - cost));
      data.putInt("TypeMoonDefenseGuardCount", data.getInt("TypeMoonDefenseGuardCount") + 1);
      if (perfect) {
         data.putBoolean(TAG_GUARD_PARRY_RELEASE, true);
         data.putInt("TypeMoonDefenseParryCount", data.getInt("TypeMoonDefenseParryCount") + 1);
         data.putLong(TAG_GUARD_START, now - 100);
         data.putBoolean(NPC_PARRY_USED, true);
         setResult(defender, RESULT_PARRY, now + 16);
      } else {
         data.remove(TAG_GUARD_PARRY_RELEASE);
         setResult(defender, RESULT_GUARD, now + 12);
      }
      broadcastDefenseAnimation(defender, 2);
      net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroCombatService.onSuccessfulGuard(defender, source.getEntity());
      net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidAbilityService.onSuccessfulGuard(defender, source.getEntity());
      if (source.getEntity() instanceof ServantEntity attacker) {
         ServantCombatExchangeService.recordContact(attacker, defender, now);
         if (!perfect) ServantCombatSpectacleService.exchangeFx(attacker, defender, false, true);
         if (ServantCombatExchangeService.ordinary(attacker, defender)) {
            Vec3 away = defender.position().subtract(attacker.position()).multiply(1, 0, 1).normalize();
            if (perfect) {
               attacker.push(-away.x * 0.28, 0, -away.z * 0.28);
               attacker.hurtMarked = true;
            } else {
               int pressure = data.getInt("TypeMoonExchangeGuardPressure") + 1;
               data.putInt("TypeMoonExchangeGuardPressure", pressure);
               if (pressure % 3 == 0 && !ServantCombatMotionService.isLaunched(defender)) {
                  ServantCombatMotionService.launch(attacker, defender, away, 1.05, 0.14,
                     net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile.Tier.SMALL, 24);
               } else {
                  defender.push(away.x * 0.42, 0, away.z * 0.42);
                  defender.hurtMarked = true;
               }
            }
         }
      }
      if (perfect) perfectGuardFx(defender, source);
      else fx(defender, ParticleTypes.ENCHANT, SoundEvents.SHIELD_BLOCK, 1.1F);
      if (defender instanceof ServerPlayer player) sync(player, true);
      // The hit which empties defense still receives its reduction.
      return DefenseRules.guardedDamage(amount, active, perfect);
   }

   private static void perfectGuardFx(LivingEntity defender, DamageSource source) {
      if (!(defender.level() instanceof ServerLevel level)) return;
      Vec3 direction = source.getSourcePosition() == null ? defender.getLookAngle()
         : source.getSourcePosition().subtract(defender.getEyePosition());
      if (direction.lengthSqr() < 1.0E-4) direction = defender.getLookAngle();
      Vec3 impact = defender.position().add(0, defender.getBbHeight() * 0.65, 0)
         .add(direction.normalize().scale(0.45));
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z, 5, 0.12, 0.12, 0.12, 0.025);
      level.sendParticles(ParticleTypes.END_ROD, impact.x, impact.y, impact.z, 2, 0.06, 0.06, 0.06, 0.01);
      level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.AMETHYST_BLOCK_HIT,
         defender instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 0.28F, 1.35F);
   }

   public static boolean isGuarding(LivingEntity entity) {
      return entity != null && entity.getPersistentData().getBoolean(TAG_GUARDING);
   }

   public static boolean isActiveGuard(LivingEntity entity) {
      return isGuarding(entity) && canAct(entity) && !broken(entity) && hasGuardResource(entity);
   }

   public static boolean isDodgeActive(LivingEntity entity, long now) {
      return entity != null && now < entity.getPersistentData().getLong(TAG_DODGE_UNTIL);
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
      boolean guarding = isCardActive(player) && isGuarding(player);
      if (force || guarding || data.getBoolean("TypeMoonDefenseGuardPoseSent")) {
         var pose = new ServantCardGuardPoseMessage(player.getUUID(), guarding);
         // Use connection-aware sends so embedded/test players need no client channel.
         for (ServerPlayer viewer : ((ServerLevel)player.level()).players()) {
            if (viewer == player || viewer.distanceToSqr(player) <= 128.0 * 128.0) ModNetwork.sendToPlayer(viewer, pose);
         }
         data.putBoolean("TypeMoonDefenseGuardPoseSent", guarding);
      }
   }

   public static void clear(LivingEntity entity) {
      if (entity == null) return;
      if (isGuarding(entity)) setGuarding(entity, false);
      CompoundTag data = entity.getPersistentData();
      data.remove(TAG_GUARDING); data.remove(TAG_GUARD_START); data.remove(TAG_LAST_GUARD_PACKET);
      data.remove(NPC_GUARD_UNTIL);
      if (entity instanceof ServantEntity servant) NPC_REACTIONS.remove(servant);
      data.remove(TAG_LAST_RESULT); data.remove(TAG_RESULT_UNTIL);
      data.remove(TAG_DODGE_UNTIL); data.remove(TAG_DODGE_X); data.remove(TAG_DODGE_Z);
      data.remove("ServantCardCombatGuardBrokenUntil");
      if (entity instanceof ServerPlayer player) sync(player, true);
   }

   public static boolean canAct(LivingEntity entity) {
      if (!entity.isAlive() || net.xxxjk.TYPE_MOON_WORLD.servant.heracles.HeraclesAbilityService.motionLocked(entity)) return false;
      if (entity.level().getGameTime() < entity.getPersistentData().getLong(ServantCombatSystem.TAG_STUN_UNTIL)
         || entity.hasEffect(net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects.PETRIFIED)) return false;
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
         if ("hundred_faces_hassan".equals(id)) {
            return net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules.mainCombatParamsForSplitCount(
               net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardHundredFacesHassanSkills.totalSplitCount(player),
               net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules.MAIN_FULL_PARAMS);
         }
         return definition == null ? null : definition.parameters();
      }
      if (entity instanceof ServantEntity servant) {
         if (servant instanceof HundredFacesHassanPersonaEntity) {
            return net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules.PERSONA_COMBAT_PARAMS;
         }
         ServantDefinition definition = servant.getDefinition();
         if (servant instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.HundredFacesHassanEntity hassan) {
            return net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules.mainCombatParamsForSplitCount(
               hassan.getTotalSplitCount(), definition == null
                  ? net.xxxjk.TYPE_MOON_WORLD.servant.hundredfaces.HundredFacesHassanRules.MAIN_FULL_PARAMS
                  : definition.parameters());
         }
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
      if (isPersona(entity)) return false;
      ServantParams p = params(entity);
      return p != null && stamina(entity) > 0 && !broken(entity);
   }

   private static boolean isPersona(LivingEntity entity) {
      return entity instanceof HundredFacesHassanPersonaEntity;
   }

   private static void clearPersonaDefenseState(LivingEntity entity) {
      CompoundTag data = entity.getPersistentData();
      for (String key : new String[]{TAG_STAMINA, TAG_STAMINA + "Max", TAG_POISE, TAG_POISE + "Max",
         TAG_GUARDING, TAG_GUARD_BROKEN, TAG_GUARD_COOLDOWN, TAG_GUARD_START, NPC_GUARD_UNTIL,
         NPC_PARRY_USED, "TypeMoonCombatGuardBroken"}) {
         data.remove(key);
      }
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

   private static void beginDodgeMotion(LivingEntity entity, CompoundTag data, Vec3 direction,
                                        double speed, long now) {
      data.putDouble(TAG_DODGE_X, direction.x * speed);
      data.putDouble(TAG_DODGE_Z, direction.z * speed);
      data.putLong(TAG_DODGE_UNTIL, now + 4L);
      // The dodge is a short hop, making projectile and skill evasion read in
      // the vertical space as well as the side step.
      entity.setDeltaMovement(direction.x * speed, Math.max(entity.getDeltaMovement().y,
         entity instanceof ServantEntity ? 0.42 : 0.26), direction.z * speed);
      entity.hurtMarked = true;
   }

   /** Preserve the burst for a few frames so a dodge reads as a jump/backstep. */
   private static void continueNpcDodge(LivingEntity entity, CompoundTag data, long now) {
      if (!(entity instanceof ServantEntity) || now >= data.getLong(TAG_DODGE_UNTIL)) return;
      Vec3 wanted = new Vec3(data.getDouble(TAG_DODGE_X), entity.getDeltaMovement().y,
         data.getDouble(TAG_DODGE_Z));
      if (entity.horizontalCollision) {
         data.remove(TAG_DODGE_UNTIL);
         return;
      }
      entity.setDeltaMovement(entity.getDeltaMovement().lerp(wanted, 0.78));
      entity.hasImpulse = true;
      if (entity.level() instanceof ServerLevel level && now % 2L == 0L) {
         level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.22, entity.getZ(),
            5, 0.18, 0.08, 0.18, 0.035);
      }
   }

   private static void setResult(LivingEntity entity, String result, long until) {
      CompoundTag data = entity.getPersistentData();
      data.putString(TAG_LAST_RESULT, result);
      data.putLong(TAG_RESULT_UNTIL, until);
   }

   private static void fx(LivingEntity entity, net.minecraft.core.particles.ParticleOptions particle,
                          net.minecraft.sounds.SoundEvent sound, float pitch) {
      if (entity.level() instanceof ServerLevel level) {
         level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.55, entity.getZ(), 8, 0.3, 0.35, 0.3, 0.03);
         level.playSound(null, entity.blockPosition(), sound, entity instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 0.75F, pitch);
      }
   }
}
