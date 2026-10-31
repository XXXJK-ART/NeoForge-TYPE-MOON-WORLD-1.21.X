package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterDamageRouter;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.entity.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import static net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaDamageSource.Ability.*;

/** Server-authoritative effects. Resource payment is performed by each actor's input adapter. */
@EventBusSubscriber(modid = "typemoonworld")
public final class OdaAbilityService {
   public static final String MANIFEST_UNTIL = "OdaManifestUntil";
   public static final String TAG_FLIGHT_DESTROYED_UNTIL = "OdaFlightDestroyedUntil";
   private static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath("typemoonworld", "oda_manifest");
   private static final ResourceLocation SLOW = ResourceLocation.fromNamespaceAndPath("typemoonworld", "oda_crossfire");
   private static final Map<UUID, State> STATES = new HashMap<>();
   private static final Set<LivingEntity> SLOWED = new HashSet<>();
   private OdaAbilityService() {}
   private record Task(long at, Runnable run) {}
   private static final class Zone {
      final Vec3 center;
      final long start;
      final boolean fire;
      int nextPulse;
      Zone(Vec3 center, long start, boolean fire) { this.center = center; this.start = start; this.fire = fire; }
   }
   private static final class State {
      final LivingEntity owner;
      final ServerLevel level;
      final List<Task> tasks = new ArrayList<>();
      final List<Zone> zones = new ArrayList<>();
      final Map<UUID, Pursuit> pursuits = new HashMap<>();
      final Set<LivingEntity> burns = new HashSet<>();
      final List<ThreeThousandRelay> gunRelays = new ArrayList<>();
      int pendingGunTiers;
      State(LivingEntity owner) { this.owner = owner; this.level = (ServerLevel)owner.level(); }
   }
   private static final class ThreeThousandRelay {
      final LivingEntity target;
      final boolean follow;
      final Vec3 position, direction, origin;
      final int index;
      final boolean[] waveEffects;
      OdaMatchlockGunEntity gun;
      int wave;
      long nextWaveAt = -1;
      ThreeThousandRelay(LivingEntity target, boolean follow, Vec3 position, Vec3 direction, Vec3 origin, int index,
                         OdaMatchlockGunEntity gun, boolean[] waveEffects) {
         this.target = target; this.follow = follow; this.position = position; this.direction = direction;
         this.origin = origin; this.index = index; this.gun = gun; this.waveEffects = waveEffects;
      }
   }
   private static final class Pursuit {
      final LivingEntity target;
      final Boolean[] results = new Boolean[10];
      final long expires;
      int next, streak;
      Pursuit(LivingEntity target, long now) { this.target = target; expires = now + 180; }
   }
   private static State state(LivingEntity owner) {
      prepare(owner);
      State current = STATES.get(owner.getUUID());
      if (current != null && (current.owner != owner || current.level != owner.level())) clear(owner);
      return STATES.computeIfAbsent(owner.getUUID(), id -> new State(owner));
   }
   public static void prepare(LivingEntity owner) {
      var data = owner.getPersistentData();
      data.remove("IndependentActionCritDamageBonus");
      data.remove("OdaStrategyUntil");
      data.remove("OdaTenkaFubuDivineBaseMultiplier");
      data.remove("OdaMaouUntil");
      if (data.getInt("OdaResetVersion") < 1) {
         owner.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
         owner.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
         data.putInt("OdaResetVersion", 1);
      }
   }
   private static void later(LivingEntity owner, int ticks, Runnable run) {
      state(owner).tasks.add(new Task(owner.level().getGameTime() + ticks, run));
   }
   public static boolean enemy(LivingEntity owner, LivingEntity target) {
      return target != owner && target.isAlive() && !target.isAlliedTo(owner)
         && !ServantMasterTargeting.isContractMaster(owner, target) && !EntityUtils.isImmunePlayerTarget(target);
   }
   public static LivingEntity lookTarget(LivingEntity owner, double range) {
      Vec3 start = owner.getEyePosition(), end = start.add(owner.getLookAngle().scale(range));
      HitResult block = owner.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
      double limit = block.getLocation().distanceToSqr(start);
      LivingEntity selected = null;
      for (LivingEntity target : owner.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().expandTowards(owner.getLookAngle().scale(range)).inflate(1), e -> enemy(owner, e))) {
         var hit = target.getBoundingBox().inflate(.4).clip(start, end);
         if (hit.isPresent() && start.distanceToSqr(hit.get()) < limit) {
            limit = start.distanceToSqr(hit.get()); selected = target;
         }
      }
      return selected;
   }
   public static boolean canCast(LivingEntity owner, int slot) {
      return slot == 0 ? orbitCount(owner) < 5 : slot != 2 || lookTarget(owner, 30) != null;
   }
   public static int orbitCount(LivingEntity owner) {
      return owner.level().getEntitiesOfClass(OdaMatchlockGunEntity.class, owner.getBoundingBox().inflate(48), gun -> gun.isOrbitFor(owner.getUUID())).size();
   }
   public static boolean cast(LivingEntity owner, int slot) {
      if (!(owner.level() instanceof ServerLevel level) || !canCast(owner, slot)) return false;
      state(owner);
      switch (slot) {
         case -1 -> dash(owner, 4.5, 15, true);
         case 0 -> {
            int count = orbitCount(owner);
            for (int i = count; i < 5; i++) level.addFreshEntity(OdaMatchlockGunEntity.floating(level, owner, i, 5, 600, 0));
         }
         case 1 -> {
            for (int wave = 0; wave < 3; wave++) {
               int delay = wave * 10;
               later(owner, delay, () -> fan(owner, 10, 1, 4, ROTATION));
            }
         }
         case 2 -> {
            LivingEntity target = lookTarget(owner, 30);
            UUID volley = UUID.randomUUID();
            state(owner).pursuits.put(volley, new Pursuit(target, level.getGameTime()));
            for (int i = 0; i < 10; i++) {
               double angle = i * Math.PI * 2 / 10;
               Vec3 pos = target.position().add(Math.cos(angle) * 2.6, target.getBbHeight() * .65 + .4, Math.sin(angle) * 2.6);
               var gun = OdaMatchlockGunEntity.oneShotTracking(level, owner, target, pos, i * 4)
                  .configure(PURSUIT, 1, 4, 80).pursuit(volley, i);
               level.addFreshEntity(gun);
            }
         }
         case 3 -> {
            VFXServerEffects.spawn(level, "servant_oda_sniper_charge", owner, 64.0);
            later(owner, 40, () -> sniper(owner, 40)); // NPC full charge, player uses press/release.
         }
         case 4 -> {
            LivingEntity target = lookTarget(owner, 30);
            Vec3 center = target == null ? owner.position().add(owner.getLookAngle().scale(14)) : target.position();
            state(owner).zones.add(new Zone(center, level.getGameTime(), false));
            for (int i = 0; i < 14; i++) {
               double angle = i * Math.PI * 2 / 14;
               Vec3 pos = center.add(Math.cos(angle) * 5, 1.2, Math.sin(angle) * 5);
               var gun = OdaMatchlockGunEntity.oneShotTracking(level, owner, target, pos, 0)
                  .configure(CROSSFIRE, 8, 20, 160).field(center);
               level.addFreshEntity(gun);
            }
         }
         case 5 -> {
            LivingEntity target = lookTarget(owner, 30);
            Vec3 center = target == null ? owner.position().add(owner.getLookAngle().multiply(1, 0, 1).normalize().scale(8)) : target.position();
            state(owner).zones.add(new Zone(center, level.getGameTime(), true));
         }
         case 6 -> { dash(owner, 3.5, 22, false); fan(owner, 5, 3, 4, DASH); }
         case 7 -> {
            owner.getPersistentData().putLong(MANIFEST_UNTIL, level.getGameTime() + 300);
            modifier(owner, SPEED, .15, true);
            VFXServerEffects.spawn(level, "servant_oda_maou_skill", owner, 72.0);
         }
         default -> { return false; }
      }
      OdaNobunagaActionAnimations.playSkill(owner, slot);
      return true;
   }
   private static void fan(LivingEntity owner, int count, int shots, int interval, OdaDamageSource.Ability source) {
      ServerLevel level = (ServerLevel)owner.level();
      Vec3 forward = owner.getLookAngle(), side = new Vec3(-forward.z, 0, forward.x).normalize();
      for (int i = 0; i < count; i++) {
         double angle = Math.toRadians(-30 + 60.0 * i / Math.max(1, count - 1));
         Vec3 dir = forward.scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).normalize();
         Vec3 pos = owner.getEyePosition().add(side.scale((i - (count - 1) * .5) * .4));
         var gun = new OdaMatchlockGunEntity(level, owner, pos, dir, 0)
            .configure(source, shots, interval, 80).fixedDirection(dir);
         level.addFreshEntity(gun);
      }
   }
   private static void dash(LivingEntity owner, double length, float damage, boolean firstOnly) {
      Vec3 direction = owner.getLookAngle().multiply(1, 0, 1).normalize();
      Set<UUID> hit = new HashSet<>();
      boolean struck = false;
      for (double moved = 0; moved < length; moved += .15) {
         Vec3 before = owner.position();
         owner.move(MoverType.SELF, direction.scale(Math.min(.15, length - moved)));
         owner.hurtMarked = true;
         if (!struck || !firstOnly) {
            var targets = owner.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(.3), e -> enemy(owner, e) && !hit.contains(e.getUUID()));
            targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(before)));
            try (var carrySegment300 = ServantMasterDamageRouter.attackSegment()) {
            for (var target : targets) {
               hit.add(target.getUUID()); struck = true;
               hurt(owner, target, damage, firstOnly ? THRUST : DASH, false);
               if (firstOnly) {
                  if (OdaCombatEvents.divinity(target) > 0) target.move(MoverType.SELF, direction.scale(5));
                  if (OdaCombatEvents.riding(target)) for (int i = 0; i < 5; i++) {
                     Vec3 pos = owner.getEyePosition().add((i - 2) * .45, .3, 0);
                     ((ServerLevel)owner.level()).addFreshEntity(OdaMatchlockGunEntity.oneShotTracking(owner.level(), owner, target, pos, 0).configure(THRUST, 1, 4, 60));
                  }
                  break;
               }
            }
            }
         }
         if (owner.position().distanceToSqr(before) < .001) break;
      }
   }
   public static void sniper(LivingEntity owner, int held) {
      OdaNobunagaActionAnimations.play(owner, OdaNobunagaActionAnimations.Action.ATTACK_SHOT);
      Vec3 look = owner.getLookAngle();
      Vec3 start = owner.getEyePosition().add(look.scale(.85));
      if (owner.level() instanceof ServerLevel level) {
         VFXServerEffects.spawnOriented(level, "servant_oda_sniper_release", start, look, 64.0);
      }
      shoot(owner, start, look, OdaRules.chargeDamage(held), OdaRules.chargeSpeed(held), SNIPER)
         .knockback(held >= 40 ? 1 : 0);
   }
   public static OdaMatchlockBulletEntity shoot(LivingEntity owner, Vec3 pos, Vec3 direction, float damage, float speed, OdaDamageSource.Ability ability) {
      var bullet = new OdaMatchlockBulletEntity(owner.level(), owner, null, damage).source(ability);
      bullet.setPos(pos); bullet.setDeltaMovement(direction.normalize().scale(speed));
      owner.level().addFreshEntity(bullet);
      return bullet;
   }
   public static boolean hasThreeThousandArray(LivingEntity owner) {
      State current = STATES.get(owner.getUUID());
      return current != null && (current.pendingGunTiers > 0 || !current.gunRelays.isEmpty());
   }
   public static void threeThousand(LivingEntity owner, LivingEntity target, boolean follow, Vec3 origin) {
      OdaNobunagaActionAnimations.play(owner, OdaNobunagaActionAnimations.Action.NP_THREE_THOUSAND_RELEASE);
      State current = state(owner);
      ServerLevel level = current.level;
      Vec3 horizontal = owner.getLookAngle().multiply(1, 0, 1);
      Vec3 forward = horizontal.lengthSqr() < 1e-6 ? new Vec3(0,0,1) : horizontal.normalize();
      Vec3 side = new Vec3(-forward.z, 0, forward.x);
      boolean[] waveEffects = new boolean[OdaRules.THREE_THOUSAND_WAVE_COUNT];
      current.pendingGunTiers += OdaRules.THREE_THOUSAND_TIER_COUNT;
      for (int tier = 0; tier < OdaRules.THREE_THOUSAND_TIER_COUNT; tier++) {
         int currentTier = tier;
         later(owner, OdaRules.threeThousandTierDelay(tier), () -> {
            current.pendingGunTiers--;
            if (!owner.isAlive() || owner.isRemoved() || owner.level() != level || !OdaCombatEvents.isOda(owner)) return;
            Vec3 center = follow ? owner.position().add(0, owner.getBbHeight() * .55, 0) : origin;
            if (currentTier == 0) startThreeThousandWaveEffect(level, owner, follow, origin, forward, 0, waveEffects);
            for (int slot = 0; slot < OdaRules.THREE_THOUSAND_GUNS_PER_TIER; slot++) {
               int index = currentTier * OdaRules.THREE_THOUSAND_GUNS_PER_TIER + slot;
               double radius = OdaRules.threeThousandRadius(currentTier, slot, 0);
               double angle = OdaRules.threeThousandAngle(currentTier, slot, 0);
               Vec3 position = center.add(side.scale(Math.cos(angle) * radius)).add(forward.scale(Math.sin(angle) * radius))
                  .add(0, OdaRules.threeThousandHeight(currentTier, slot, 0), 0);
               var gun = spawnThreeThousandGun(level, owner, target, follow, position, forward, index, 0,
                  OdaRules.threeThousandShotDelay(slot));
               current.gunRelays.add(new ThreeThousandRelay(target, follow, position, forward, origin, index, gun, waveEffects));
            }
            level.playSound(null, center.x, center.y, center.z, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS,
               .24F, .9F + currentTier * .035F);
         });
      }
   }
   private static OdaMatchlockGunEntity spawnThreeThousandGun(ServerLevel level, LivingEntity owner, LivingEntity target,
         boolean follow, Vec3 position, Vec3 direction, int index, int wave, int delay) {
      int formationIndex = wave * OdaRules.THREE_THOUSAND_GUNS_PER_WAVE + index;
      var gun = follow
         ? OdaMatchlockGunEntity.threeThousandWorldsFollow(level, owner, target, formationIndex, OdaRules.THREE_THOUSAND_TOTAL_GUNS, delay)
         : OdaMatchlockGunEntity.threeThousandWorlds(level, owner, target, position, direction, delay);
      gun.formationIndex(formationIndex);
      level.addFreshEntity(gun);
      Vec3 appearance = gun.position();
      level.sendParticles(ParticleTypes.ASH, appearance.x, appearance.y, appearance.z, 1, .05, .08, .05, .01);
      return gun;
   }
   private static void startThreeThousandWaveEffect(ServerLevel level, LivingEntity owner, boolean follow,
         Vec3 origin, Vec3 direction, int wave, boolean[] started) {
      if (started[wave]) return;
      started[wave] = true;
      if (follow) VFXServerEffects.spawn(level, "servant_oda_three_thousand", owner, 160.0);
      else VFXServerEffects.spawnOriented(level, "servant_oda_three_thousand", origin, direction, 160.0);
   }
   private static void tickThreeThousandRelays(State current) {
      long now = current.level.getGameTime();
      var iterator = current.gunRelays.iterator();
      while (iterator.hasNext()) {
         ThreeThousandRelay relay = iterator.next();
         if (!relay.gun.isRemoved()) continue;
         if (relay.wave + 1 >= OdaRules.THREE_THOUSAND_WAVE_COUNT) { iterator.remove(); continue; }
         if (relay.nextWaveAt < 0) relay.nextWaveAt = now + OdaRules.THREE_THOUSAND_WAVE_INTERVAL;
         if (now < relay.nextWaveAt) continue;
         relay.wave++;
         relay.nextWaveAt = -1;
         startThreeThousandWaveEffect(current.level, current.owner, relay.follow,
            relay.origin, relay.direction, relay.wave, relay.waveEffects);
         relay.gun = spawnThreeThousandGun(current.level, current.owner, relay.target, relay.follow, relay.position,
            relay.gun.getLookAngle(), relay.index, relay.wave, 0);
      }
   }
   public static boolean hurt(LivingEntity caster, LivingEntity target, float damage, OdaDamageSource.Ability ability, boolean fire) {
      DamageSource base = fire ? caster.damageSources().inFire() : ability == HAJUN ? caster.damageSources().magic()
         : ability == PURSUIT_EXPLOSION ? caster.damageSources().explosion(caster, caster) : caster.damageSources().mobAttack(caster);
      target.invulnerableTime = 0;
      boolean hit = target.hurt(new OdaDamageSource(base, caster, caster, ability), damage);
      target.invulnerableTime = 0;
      return hit;
   }
   public static void hajunPulse(LivingEntity caster, ServerLevel level, boolean initial) {
      State state = state(caster);
      try (var carrySegment301 = ServantMasterDamageRouter.attackSegment()) {
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(25), e -> enemy(caster, e))) {
         boolean divine = OdaCombatEvents.divinity(target) > 0;
         target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), initial ? 160 : 80));
         target.getPersistentData().putUUID("OdaBurnOwner", caster.getUUID());
         state.burns.add(target);
         target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 45, divine ? 2 : 0, false, true, true));
         if (divine) target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 45, 1, false, true, true));
         hurt(caster, target, initial ? 52 : 12, HAJUN, false);
      }
      }
   }
   public static LivingEntity burnOwner(LivingEntity target) {
      if (!target.getPersistentData().hasUUID("OdaBurnOwner")) return null;
      State state = STATES.get(target.getPersistentData().getUUID("OdaBurnOwner"));
      return state != null && state.owner.isAlive() && state.owner.level() == target.level() ? state.owner : null;
   }
   public static void report(LivingEntity owner, UUID volley, int index, boolean hit) {
      State state = STATES.get(owner.getUUID());
      if (state == null || volley == null || index < 0 || index >= 10) return;
      Pursuit pursuit = state.pursuits.get(volley);
      if (pursuit == null || pursuit.results[index] != null) return;
      if (!pursuit.target.isAlive() || pursuit.target.level() != owner.level()) { state.pursuits.remove(volley); return; }
      pursuit.results[index] = hit;
      while (pursuit.next < 10 && pursuit.results[pursuit.next] != null) {
         pursuit.streak = pursuit.results[pursuit.next++] ? pursuit.streak + 1 : 0;
         if (pursuit.streak == 3) {
            pursuit.streak = 0;
            hurt(owner, pursuit.target, 20, PURSUIT_EXPLOSION, false);
            state.level.sendParticles(ParticleTypes.EXPLOSION, pursuit.target.getX(), pursuit.target.getY() + .8, pursuit.target.getZ(), 1, 0, 0, 0, 0);
         }
      }
      if (pursuit.next == 10) state.pursuits.remove(volley);
   }
   public static float absorb(LivingEntity owner, float damage) {
      var guns = owner.level().getEntitiesOfClass(OdaMatchlockGunEntity.class, owner.getBoundingBox().inflate(6), gun -> gun.isCombatFor(owner.getUUID()) && gun.distanceToSqr(owner) <= 36);
      guns.sort(Comparator.comparingDouble((OdaMatchlockGunEntity gun) -> gun.distanceToSqr(owner)).thenComparingInt(Entity::getId));
      for (var gun : guns) { damage = gun.absorb(damage); if (damage <= 0) break; }
      return damage;
   }
   public static long flightCooldownRemaining(LivingEntity owner) {
      if (owner instanceof ServerPlayer player
         && net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardUnlimitedMode.isCooldownDisabled(player)) return 0;
      return Math.max(0, owner.getPersistentData().getLong(TAG_FLIGHT_DESTROYED_UNTIL) - owner.level().getGameTime());
   }
   public static void flightGunDestroyed(LivingEntity owner) {
      if (owner == null || owner.level().isClientSide()) return;
      owner.getPersistentData().putLong(TAG_FLIGHT_DESTROYED_UNTIL, owner.level().getGameTime() + OdaRules.FLIGHT_DESTROYED_COOLDOWN);
      owner.setNoGravity(false);
      if (owner instanceof ServerPlayer player) {
         var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardFlightController.stop(player, vars, true);
      } else {
         owner.getPersistentData().remove(net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.TAG_FLIGHT_UNTIL);
         owner.getPersistentData().remove(net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.TAG_FOOT_SUPPORT_GUN);
      }
   }
   private static void modifier(LivingEntity owner, ResourceLocation id, double value, boolean enable) {
      var attribute = owner.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attribute == null) return;
      if (!enable) attribute.removeModifier(id);
      else if (!attribute.hasModifier(id)) attribute.addTransientModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
   }
   public static void clear(LivingEntity owner) {
      State state = STATES.remove(owner.getUUID());
      if (state != null) {
         for (var target : state.burns) { target.getPersistentData().remove("OdaBurnOwner"); target.clearFire(); }
      }
      Set<ServerLevel> levels = new HashSet<>();
      if (state != null) levels.add(state.level);
      if (owner.level() instanceof ServerLevel current) levels.add(current);
      for (ServerLevel level : levels) {
         var owned = new ArrayList<Entity>();
         for (Entity e : level.getEntities().getAll()) {
            if (e instanceof OdaMatchlockGunEntity gun && gun.isOwnedBy(owner.getUUID())) owned.add(gun);
            if (e instanceof OdaMatchlockBulletEntity bullet && bullet.getOwner() != null && bullet.getOwner().getUUID().equals(owner.getUUID())) owned.add(bullet);
         }
         for (var entity : owned) entity.discard();
      }
      owner.getPersistentData().remove(MANIFEST_UNTIL);
      modifier(owner, SPEED, 0, false);
   }
   @SubscribeEvent
   public static void tick(ServerTickEvent.Post event) {
      Set<LivingEntity> slowed = new HashSet<>();
      for (State state : new ArrayList<>(STATES.values())) {
         var owner = state.owner;
         if (!owner.isAlive() || owner.isRemoved() || owner.level() != state.level || !OdaCombatEvents.isOda(owner)
            || owner instanceof ServerPlayer player && player.hasDisconnected()) { clear(owner); continue; }
         long now = state.level.getGameTime();
         var due = state.tasks.stream().filter(task -> task.at <= now).toList();
         state.tasks.removeAll(due);
         for (Task task : due) task.run.run();
         tickThreeThousandRelays(state);
         state.pursuits.values().removeIf(p -> now > p.expires || !p.target.isAlive() || p.target.level() != state.level);
         state.burns.removeIf(target -> {
            if (target.isAlive() && target.getRemainingFireTicks() > 0 && target.level() == state.level) return false;
            target.getPersistentData().remove("OdaBurnOwner"); return true;
         });
         boolean manifest = owner.getPersistentData().getLong(MANIFEST_UNTIL) > now;
         modifier(owner, SPEED, .15, manifest);
         if (manifest && now % 20 == 0 && (owner.isOnFire() || owner.isInLava() || state.level.getBlockStates(owner.getBoundingBox()).anyMatch(block -> block.is(BlockTags.FIRE)))) owner.heal(5);
         for (Zone zone : state.zones) {
            long age = now - zone.start;
            double radius = zone.fire ? 7 : 5;
            if (age >= 160) continue;
            var targets = state.level.getEntitiesOfClass(LivingEntity.class, new AABB(zone.center, zone.center).inflate(radius, 2.5, radius), e -> enemy(owner, e)
               && e.position().subtract(zone.center).multiply(1, 0, 1).lengthSqr() <= radius * radius);
            if (!zone.fire) slowed.addAll(targets);
            else if (age >= zone.nextPulse) {
               for (var target : targets) hurt(owner, target, zone.nextPulse <= 40 ? 8 : 5, SCORCHED, true);
               zone.nextPulse += 20;
            }
            if (zone.fire && age % 4 == 0) state.level.sendParticles(ParticleTypes.FLAME, zone.center.x, zone.center.y + .1, zone.center.z, 24, 3.5, .1, 3.5, .02);
         }
         state.zones.removeIf(zone -> now - zone.start >= 160);
      }
      for (var target : SLOWED) if (!slowed.contains(target)) modifier(target, SLOW, 0, false);
      for (var target : slowed) modifier(target, SLOW, -.3, true);
      SLOWED.clear(); SLOWED.addAll(slowed);
   }
   @SubscribeEvent
   public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
      STATES.clear(); SLOWED.clear();
   }
   @SubscribeEvent
   public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && OdaCombatEvents.isOda(player))
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills.clear(player);
   }
   @SubscribeEvent
   public static void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
      if (OdaCombatEvents.isOda(event.getEntity())) {
         if (event.getEntity() instanceof ServerPlayer player) net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills.clear(player);
         else clear(event.getEntity());
      }
   }
   @SubscribeEvent
   public static void travel(net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent event) {
      if (event.getEntity() instanceof LivingEntity owner && OdaCombatEvents.isOda(owner)) clear(owner);
   }
   @SubscribeEvent
   public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && OdaCombatEvents.isOda(player))
         net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills.onDimensionChange(player);
   }
   @SubscribeEvent
   public static void loaded(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
      if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity living)
         net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.restoreLegacyDuelState(living);
   }
   @SubscribeEvent
   public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
      var owners = new ArrayList<LivingEntity>();
      for (ServerLevel level : event.getServer().getAllLevels()) for (Entity entity : level.getEntities().getAll())
         if (entity instanceof LivingEntity living && OdaCombatEvents.isOda(living)) owners.add(living);
      for (var owner : owners) {
         if (owner instanceof ServerPlayer player) net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills.clear(player);
         else if (owner instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity npc)
            net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper.cleanupHajun(npc);
         clear(owner);
      }
   }
}
