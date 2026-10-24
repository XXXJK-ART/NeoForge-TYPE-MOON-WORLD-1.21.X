package net.xxxjk.TYPE_MOON_WORLD.servant.artoria;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatDefenseService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;

/** Persistent resources are separate from the temporary state of the current form. */
@EventBusSubscriber(modid = "typemoonworld")
public final class ArtoriaStateService {
   public static final String JET_USED = "ArtoriaJetUsedTicks";
   public static final String CHARGES = "ArtoriaSavedInstinctCharges";
   public static final String RESTORE = "ArtoriaSavedInstinctRestore";
   public static final String NEXT_DRAIN = "ArtoriaNextBurstDrain";
   public static final String NP_READY = "ArtoriaExcaliburReadyAt";
   public static final String NP_SHORT_READY = "ArtoriaExcaliburShortReadyAt";
   public static final String NP_BEAM = "ArtoriaActiveExcalibur";
   public static final String WIND_READY = "ArtoriaWindReadyAt";
   public static final String WIND_BARRIER = "ArtoriaWindBarrierActive";
   public static final String WIND_DEFENSE_UNTIL = "ArtoriaWindDefenseUntil";
   public static final String WIND_DEFENSE_NEXT_PULSE = "ArtoriaWindDefenseNextPulse";
   public static final String WIND_DEFENSE_READY = "ArtoriaWindDefenseReadyAt";
   public static final String WIND_DEFENSE_SWORD = "ArtoriaWindDefenseSword";
   public static final String COMBO_READY = "ArtoriaComboReadyAt";
   private static final Map<Object, Map<UUID, Boolean>> JUDGMENTS = new WeakHashMap<>();
   private ArtoriaStateService() { }

   public static boolean isArtoria(LivingEntity entity) {
      if (entity instanceof ArtoriaPendragonEntity) return true;
      if (!(entity instanceof ServerPlayer player)) return false;
      var card = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState();
      return card.transformed() && ArtoriaRules.ID.equals(card.id());
   }

   /** The default Saber state keeps Excalibur veiled until the player explicitly reveals it. */
   public static boolean isWindBarrierEnabled(LivingEntity owner) {
      return owner != null && (!owner.getPersistentData().contains(WIND_BARRIER)
         || owner.getPersistentData().getBoolean(WIND_BARRIER));
   }

   public static boolean isWindBarrierActive(LivingEntity owner) {
      if (owner == null || !isWindBarrierEnabled(owner) || busy(owner)) return false;
      var data = owner.getPersistentData();
      long now = owner.level().getGameTime();
      return now >= data.getLong(WIND_READY)
         && now >= data.getLong(ArtoriaPendragonCombatHelper.TAG_EXCALIBUR_VISIBLE_UNTIL);
   }

   public static boolean isWindDefenseActive(LivingEntity owner) {
      return owner != null && isArtoria(owner) && owner.getPersistentData().getLong(WIND_DEFENSE_UNTIL)
         > owner.level().getGameTime();
   }

   public static boolean canStartWindDefense(LivingEntity owner) {
      return owner != null && isArtoria(owner) && !isWindDefenseActive(owner) && !busy(owner)
         && !ArtoriaAbilityService.actionLocked(owner) && ArtoriaAbilityService.canWind(owner);
   }

   /** Starts the held defensive field. Card actions pay through their normal transaction. */
   public static boolean startWindDefense(LivingEntity owner, boolean alreadyPaid) {
      if (!canStartWindDefense(owner) || !(owner.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
      long now = level.getGameTime();
      if (owner instanceof ArtoriaPendragonEntity && now < owner.getPersistentData().getLong(WIND_DEFENSE_READY)) return false;
      if (!alreadyPaid && !consume(owner, ArtoriaRules.WIND_DEFENSE_MP)) return false;
      var data = owner.getPersistentData();
      data.putLong(WIND_DEFENSE_UNTIL, now + ArtoriaRules.WIND_DEFENSE_MAX_TICKS);
      data.putLong(WIND_DEFENSE_NEXT_PULSE, now);
      data.putLong(ArtoriaAbilityService.ACTION_LOCK_UNTIL, now + ArtoriaRules.WIND_DEFENSE_MAX_TICKS);
      if (owner instanceof ArtoriaPendragonEntity)
         data.putLong(WIND_DEFENSE_READY, now + ArtoriaRules.WIND_DEFENSE_COOLDOWN);
      spawnPlantedSword(owner, level);
      syncSwordVisibility(owner);
      level.playSound(null, owner.blockPosition(), net.minecraft.sounds.SoundEvents.TRIDENT_RETURN,
         owner instanceof ServerPlayer ? net.minecraft.sounds.SoundSource.PLAYERS : net.minecraft.sounds.SoundSource.HOSTILE,
         0.8F, 1.25F);
      if (owner instanceof ArtoriaPendragonEntity npc) {
         ServantVoiceHelper.tryPlayArtoriaSkill(npc);
      }
      return true;
   }

   public static void stopWindDefense(LivingEntity owner) {
      if (owner == null) return;
      var data = owner.getPersistentData();
      data.remove(WIND_DEFENSE_UNTIL);
      data.remove(WIND_DEFENSE_NEXT_PULSE);
      if (data.getLong(ArtoriaAbilityService.ACTION_LOCK_UNTIL) <= owner.level().getGameTime() + ArtoriaRules.WIND_DEFENSE_MAX_TICKS)
         data.remove(ArtoriaAbilityService.ACTION_LOCK_UNTIL);
      if (data.hasUUID(WIND_DEFENSE_SWORD) && owner.level() instanceof net.minecraft.server.level.ServerLevel level) {
         var sword = level.getEntity(data.getUUID(WIND_DEFENSE_SWORD));
         if (sword != null) sword.discard();
      }
      data.remove(WIND_DEFENSE_SWORD);
      syncSwordVisibility(owner);
   }

   public static boolean tryStartNpcWindDefense(ArtoriaPendragonEntity owner, net.minecraft.server.level.ServerLevel level) {
      long now = level.getGameTime();
      var threats = level.getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(5.5), projectile -> {
         if (!hostileProjectile(owner, projectile)) return false;
         Vec3 relative = projectile.position().subtract(owner.position());
         return relative.horizontalDistanceSqr() <= 5.5 * 5.5
            && projectile.getDeltaMovement().dot(relative.multiply(-1, 0, -1)) > 0;
      });
      if (now < owner.getPersistentData().getLong(WIND_DEFENSE_READY)
         || owner.getCurrentMp() < ArtoriaRules.WIND_DEFENSE_MP
         || threats.isEmpty()) return false;
      return startWindDefense(owner, false);
   }

   private static void spawnPlantedSword(LivingEntity owner, net.minecraft.server.level.ServerLevel level) {
      var oldData = owner.getPersistentData();
      if (oldData.hasUUID(WIND_DEFENSE_SWORD)) {
         var oldSword = level.getEntity(oldData.getUUID(WIND_DEFENSE_SWORD));
         if (oldSword != null) oldSword.discard();
      }
      Vec3 forward = ArtoriaAbilityService.horizontal(owner);
      Vec3 position = owner.position().add(forward.scale(0.78));
      var sword = net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.ARTORIA_WIND_DEFENSE_SWORD.get().create(level);
      if (sword == null) return;
      sword.setPos(position.x, owner.getY(), position.z);
      sword.setYRot(owner.getYRot());
      level.addFreshEntity(sword);
      oldData.putUUID(WIND_DEFENSE_SWORD, sword.getUUID());
   }

   /** Toggle the veil without charging mana. Wind skills remain unavailable while regathering. */
   public static boolean toggleWindBarrier(LivingEntity owner) {
      if (owner == null || !isArtoria(owner) || busy(owner) || ArtoriaAbilityService.actionLocked(owner)) return false;
      boolean active = !isWindBarrierEnabled(owner);
      owner.getPersistentData().putBoolean(WIND_BARRIER, active);
      if (active) {
         owner.getPersistentData().remove(ArtoriaPendragonCombatHelper.TAG_EXCALIBUR_VISIBLE_UNTIL);
      } else {
         owner.getPersistentData().remove(ArtoriaPendragonCombatHelper.TAG_WIND_REGATHER_UNTIL);
      }
      syncSwordVisibility(owner);
      return true;
   }

   /** NPC Excalibur chants strip Invisible Air only after the decisive phase begins. */
   public static void autoReleaseWindBarrier(LivingEntity owner) {
      if (owner == null || !isArtoria(owner)) return;
      if (owner instanceof ArtoriaPendragonEntity npc
         && net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.getPhase(npc)
            != net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase.DECISIVE) return;
      owner.getPersistentData().putBoolean(WIND_BARRIER, false);
      owner.getPersistentData().remove(ArtoriaPendragonCombatHelper.TAG_WIND_REGATHER_UNTIL);
      syncSwordVisibility(owner);
   }

   /** Synchronizes the item custom data used by the client renderer for player-held Excalibur. */
   public static void syncSwordVisibility(LivingEntity owner) {
      if (!(owner instanceof ServerPlayer player)) return;
      long now = player.level().getGameTime();
      boolean visible = !isWindBarrierActive(owner) && !isWindDefenseActive(owner);
      long until = visible && isWindBarrierEnabled(owner)
         ? Math.max(now + 2, owner.getPersistentData().getLong(ArtoriaPendragonCombatHelper.TAG_EXCALIBUR_VISIBLE_UNTIL)) : 0;
      boolean burst = ArtoriaPendragonCombatHelper.isManaBurstActive(owner);
      boolean planted = isWindDefenseActive(owner);
      syncSword(player.getMainHandItem(), visible, until, burst, planted);
      syncSword(player.getOffhandItem(), visible, until, burst, planted);
      player.getInventory().setChanged();
   }

   public static boolean consume(LivingEntity owner, double amount) {
      if (owner instanceof ArtoriaPendragonEntity npc) {
         if (npc.getCurrentMp() + 1.0E-6 < amount) return false;
         npc.setCurrentMp(npc.getCurrentMp() - amount);
         return true;
      }
      return owner instanceof ServerPlayer player && ServantCardManaService.consume(player,
         player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES), amount);
   }

   /** Checks a hit cost without mutating mana; the actual debit happens after damage lands. */
   public static boolean canConsume(LivingEntity owner, double amount) {
      if (owner instanceof ArtoriaPendragonEntity npc) {
         return npc.getCurrentMp() + 1.0E-6 >= amount;
      }
      return owner instanceof ServerPlayer player && ServantCardManaService.availableForConsume(player,
         player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)) + 1.0E-6 >= amount;
   }

   public static double baseSwordDamage() {
      return ModItems.EXCALIBUR.get().getDefaultInstance().getAttributeModifiers().modifiers().stream()
         .filter(entry -> entry.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
            && entry.modifier().operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE)
         .mapToDouble(entry -> entry.modifier().amount()).sum();
   }

   /** Activation cost belongs to the caller's accepted skill transaction. */
   public static void startBurst(LivingEntity owner) {
      var d = owner.getPersistentData();
      d.putBoolean(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_ACTIVE, true);
      d.remove(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_UNTIL);
      d.putLong(NEXT_DRAIN, owner.level().getGameTime() + 20);
      ArtoriaPendragonCombatHelper.syncManaBurstModifiers(owner);
      syncSwordVisibility(owner);
   }

   public static void stopBurst(LivingEntity owner) {
      var d = owner.getPersistentData();
      if (ArtoriaPendragonCombatHelper.isManaBurstActive(owner)) d.putLong(
         ArtoriaPendragonCombatHelper.TAG_MANA_BURST_REOPEN_AT,
         owner.level().getGameTime() + ArtoriaRules.BURST_REOPEN_TICKS);
      d.remove(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_ACTIVE);
      d.remove(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_UNTIL);
      d.remove(NEXT_DRAIN);
      ArtoriaPendragonCombatHelper.syncManaBurstModifiers(owner);
      syncSwordVisibility(owner);
   }

   public static boolean busy(LivingEntity owner) {
      return NoblePhantasmCastService.chanting(owner) || owner.getPersistentData().hasUUID(NP_BEAM);
   }

   public static void tick(LivingEntity owner) {
      var d = owner.getPersistentData();
      long now = owner.level().getGameTime();
      // onGround alone can be set by water walking, climbing and mounted movement.
      if (trulyGrounded(owner)) d.putInt(JET_USED, 0);
      if (!isArtoria(owner)) return;
      if (d.hasUUID(NP_BEAM) && owner.level() instanceof net.minecraft.server.level.ServerLevel level
         && level.getEntity(d.getUUID(NP_BEAM)) == null) {
         d.remove(NP_BEAM);
         d.remove(ArtoriaPendragonCombatHelper.TAG_EXCALIBUR_RELEASE_UNTIL);
      }
      if (owner instanceof ServerPlayer player && now < d.getLong(NP_READY)
         && !player.getCooldowns().isOnCooldown(ModItems.EXCALIBUR.get()))
         player.getCooldowns().addCooldown(ModItems.EXCALIBUR.get(), (int)Math.min(Integer.MAX_VALUE, d.getLong(NP_READY) - now));
      ArtoriaPendragonCombatHelper.syncRidingModifiers(owner, owner.isPassenger());
      syncInstinct(owner);
      if (busy(owner) && owner instanceof ServerPlayer player)
         net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper.applyNoblePhantasmChargeSlow(player);
      if (busy(owner) && owner instanceof ServerPlayer player && now % 20 == 0)
         net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper.revealArtoriaWindVeiledExcaliburForNp(player, 40);
      if (ArtoriaPendragonCombatHelper.isManaBurstActive(owner)) {
         if (!d.contains(NEXT_DRAIN)) d.putLong(NEXT_DRAIN, now + 20);
         if (now >= d.getLong(NEXT_DRAIN)) {
            if (!consume(owner, ArtoriaRules.BURST_DRAIN_MP)) stopBurst(owner);
            else d.putLong(NEXT_DRAIN, now + 20);
         }
         if (owner.level() instanceof net.minecraft.server.level.ServerLevel level && now % 24 == 0) {
            net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects.spawn(level, "artoria_mana_burst_aura", owner, 96);
         }
      }
      if (d.contains(WIND_DEFENSE_UNTIL)) {
         if (now >= d.getLong(WIND_DEFENSE_UNTIL)) {
            stopWindDefense(owner);
         } else if (owner.level() instanceof net.minecraft.server.level.ServerLevel level) {
            if (owner instanceof ArtoriaPendragonEntity npc) npc.getNavigation().stop();
            owner.setDeltaMovement(0.0, owner.getDeltaMovement().y, 0.0);
            owner.hurtMarked = true;
            if (now % 8 == 0) net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects.spawn(level, "artoria_wind_defense", owner, 96);
            repelIncomingProjectiles(owner, level);
            if (now >= d.getLong(WIND_DEFENSE_NEXT_PULSE)) {
               d.putLong(WIND_DEFENSE_NEXT_PULSE, now + 10);
               pushEnemies(owner, level);
            }
         }
      }
      syncSwordVisibility(owner);
      if (isWindBarrierEnabled(owner) && !busy(owner) && now < d.getLong(WIND_READY)
         && owner.level() instanceof net.minecraft.server.level.ServerLevel level && now % 4 == 0) {
         net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects.spawn(level, "artoria_wind_regather", owner, 96);
      }
      ArtoriaPendragonCombatHelper.syncManaBurstModifiers(owner);
   }

   public static boolean trulyGrounded(LivingEntity owner) {
      return owner.onGround() && !owner.isPassenger() && !owner.onClimbable()
         && owner.getDeltaMovement().y <= 0 && !owner.level().noCollision(owner,
            owner.getBoundingBox().move(0, -0.06, 0));
   }

   /** Shared propulsion uses normal entity motion/collision and never grants flight. */
   public static boolean jet(LivingEntity owner, Vec3 desired, boolean jump, boolean descend) {
      if (!isArtoria(owner) || !ArtoriaPendragonCombatHelper.isManaBurstActive(owner)
         || busy(owner) || owner.isPassenger() || owner.onClimbable()) return false;
      boolean ground = trulyGrounded(owner);
      var data = owner.getPersistentData();
      if (ground) data.putInt(JET_USED, 0);
      int used = data.getInt(JET_USED);
      if (!ground && used >= ArtoriaRules.JET_TICKS) return false;
      Vec3 motion = owner.getDeltaMovement(), next = motion;
      if (desired.lengthSqr() > 1.0E-4) {
         next = next.add(desired.normalize().scale(ground ? 0.135 : 0.095));
         double speed = next.horizontalDistance();
         if (speed > 0.72) next = new Vec3(next.x * 0.72 / speed, next.y, next.z * 0.72 / speed);
      }
      if (descend) next = next.add(0, -0.17, 0);
      else if (jump) next = new Vec3(next.x, Math.min(0.55,
         Math.max(ground ? 0.38 : next.y, next.y + (ground ? 0.38 : 0.065))), next.z);
      else if (!ground && next.y < -0.28) next = new Vec3(next.x, next.y * 0.78, next.z);
      if (next.equals(motion)) return false;
      if (!ground) data.putInt(JET_USED, used + 1);
      owner.setDeltaMovement(next);
      owner.hurtMarked = true;
      return true;
   }

   public static void syncInstinct(LivingEntity owner) {
      if (!isArtoria(owner)) return;
      var d = owner.getPersistentData();
      long now = owner.level().getGameTime();
      if (!d.contains(CHARGES)) {
         int previous = d.contains(ServantCombatDefenseService.AUTO_CHARGES)
            ? Math.min(3, d.getInt(ServantCombatDefenseService.AUTO_CHARGES)) : 3;
         d.putInt(CHARGES, previous);
         if (previous == 0) d.putLong(RESTORE, now + ArtoriaRules.INSTINCT_RECOVERY_TICKS);
      }
      if (d.getInt(CHARGES) == 0 && now >= d.getLong(RESTORE)) {
         d.putInt(CHARGES, 3); d.remove(RESTORE);
      }
      d.putInt(ServantCombatDefenseService.AUTO_CHARGES, d.getInt(CHARGES));
      if (d.contains(RESTORE)) d.putLong(ServantCombatDefenseService.AUTO_RESTORE, d.getLong(RESTORE));
      else d.remove(ServantCombatDefenseService.AUTO_RESTORE);
   }

   public static void recordDodge(LivingEntity owner, int remaining) {
      if (!isArtoria(owner)) return;
      var d = owner.getPersistentData();
      d.putInt(CHARGES, remaining);
      if (remaining == 0) d.putLong(RESTORE, owner.level().getGameTime() + ArtoriaRules.INSTINCT_RECOVERY_TICKS);
   }

   /** One attack token, one target, one coin flip for both certain-hit and death. */
   public static boolean negateSpecial(LivingEntity target, Object attackToken) {
      if (ArtoriaPendragonCombatHelper.tryProtectWithAvalon(target)) return true;
      if (!isArtoria(target)) return false;
      return JUDGMENTS.computeIfAbsent(attackToken, unused -> new java.util.HashMap<>())
         .computeIfAbsent(target.getUUID(), unused -> target.getRandom().nextFloat() < 0.5F);
   }

   public static void syncSword(ItemStack stack, boolean visible, long until, boolean manaBurst) {
      syncSword(stack, visible, until, manaBurst, false);
   }

   public static void syncSword(ItemStack stack, boolean visible, long until, boolean manaBurst, boolean planted) {
      if (!stack.is(ModItems.EXCALIBUR.get())) return;
      var old = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      boolean veiled = !visible;
      if (old.getBoolean("ServantCardArtoriaWindVeiled") == veiled
         && old.getLong("ServantCardArtoriaWindRevealUntil") == until
         && old.getBoolean("ArtoriaManaBurstActive") == manaBurst
         && old.getBoolean("ArtoriaWindDefenseActive") == planted) return;
      old.putBoolean("ServantCardArtoriaWindVeiled", veiled);
      old.putLong("ServantCardArtoriaWindRevealUntil", veiled ? until : 0);
      old.putBoolean("ArtoriaManaBurstActive", manaBurst);
      old.putBoolean("ArtoriaWindDefenseActive", planted);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(old));
   }

   private static void pushEnemies(LivingEntity owner, net.minecraft.server.level.ServerLevel level) {
      double radius = ArtoriaRules.WIND_DEFENSE_RADIUS - 0.5;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
         owner.getBoundingBox().inflate(radius, 2.5, radius), target -> ArtoriaAbilityService.enemy(owner, target))) {
         Vec3 away = target.position().subtract(owner.position()).multiply(1, 0, 1);
         if (away.horizontalDistanceSqr() > radius * radius) continue;
         if (away.lengthSqr() < 1.0E-5) away = ArtoriaAbilityService.horizontal(owner).scale(-1);
         else away = away.normalize();
         target.setDeltaMovement(target.getDeltaMovement().add(away.scale(0.62)).add(0, 0.18, 0));
         target.hurtMarked = true;
      }
   }

   private static void repelIncomingProjectiles(LivingEntity owner, net.minecraft.server.level.ServerLevel level) {
      double radius = ArtoriaRules.WIND_DEFENSE_RADIUS;
      for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
         owner.getBoundingBox().inflate(radius, 2.5, radius))) {
         if (!hostileProjectile(owner, projectile)) continue;
         Vec3 away = projectile.position().subtract(owner.position()).multiply(1, 0, 1);
         if (away.horizontalDistanceSqr() > radius * radius || away.lengthSqr() < 1.0E-5) continue;
         Vec3 outward = away.normalize();
         if (projectile.getDeltaMovement().multiply(1, 0, 1).dot(outward) >= 0) continue;
         reflectProjectile(owner, projectile, outward, radius);
      }
   }

   private static boolean hostileProjectile(LivingEntity owner, Projectile projectile) {
      if (projectile == null || !projectile.isAlive()) return false;
      if (!(projectile.getOwner() instanceof LivingEntity shooter)) return true;
      return shooter != owner && !owner.isAlliedTo(shooter) && !shooter.isAlliedTo(owner);
   }

   private static void reflectProjectile(LivingEntity owner, Projectile projectile, Vec3 outward, double radius) {
      double speed = Math.max(0.8, projectile.getDeltaMovement().length());
      projectile.setDeltaMovement(outward.scale(speed).add(0, 0.12, 0));
      projectile.setPos(owner.getX() + outward.x * (radius + 0.25),
         owner.getY() + Math.min(owner.getBbHeight() * 0.5, 0.9),
         owner.getZ() + outward.z * (radius + 0.25));
      projectile.hurtMarked = true;
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void blockProjectileImpact(ProjectileImpactEvent event) {
      Projectile projectile = event.getProjectile();
      if (!(projectile.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
      Vec3 impact = event.getRayTraceResult().getLocation();
      for (LivingEntity guard : level.getEntitiesOfClass(LivingEntity.class,
         new net.minecraft.world.phys.AABB(impact, impact).inflate(ArtoriaRules.WIND_DEFENSE_RADIUS),
         ArtoriaStateService::isWindDefenseActive)) {
         if (!hostileProjectile(guard, projectile)
            || impact.distanceToSqr(guard.position()) > ArtoriaRules.WIND_DEFENSE_RADIUS * ArtoriaRules.WIND_DEFENSE_RADIUS) continue;
         Vec3 outward = impact.subtract(guard.position()).multiply(1, 0, 1);
         if (outward.lengthSqr() < 1.0E-5) outward = projectile.getDeltaMovement().multiply(-1, 0, -1);
         if (outward.lengthSqr() < 1.0E-5) outward = ArtoriaAbilityService.horizontal(guard).scale(-1);
         reflectProjectile(guard, projectile, outward.normalize(), ArtoriaRules.WIND_DEFENSE_RADIUS);
         event.setCanceled(true);
         return;
      }
   }

   public static void clearTemporary(LivingEntity owner) {
      NoblePhantasmCastService.cancel(owner, false);
      stopBurst(owner);
      stopWindDefense(owner);
      ArtoriaPendragonCombatHelper.syncRidingModifiers(owner, false);
      var d = owner.getPersistentData();
      if (d.hasUUID(NP_BEAM) && owner.level() instanceof net.minecraft.server.level.ServerLevel level) {
         var beam = level.getEntity(d.getUUID(NP_BEAM));
         if (beam instanceof net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity) beam.discard();
      }
      d.remove(NP_BEAM);
      d.remove(ArtoriaPendragonCombatHelper.TAG_EXCALIBUR_WINDUP_UNTIL);
      d.remove(ArtoriaPendragonCombatHelper.TAG_EXCALIBUR_RELEASE_UNTIL);
      d.remove(ArtoriaPendragonCombatHelper.TAG_EXCALIBUR_VISIBLE_UNTIL);
      d.remove(ArtoriaPendragonCombatHelper.TAG_WIND_REGATHER_UNTIL);
      d.remove(WIND_BARRIER);
      d.remove("ServantCardArtoriaWindRevealUntil");
      d.remove("ServantCardArtoriaExcaliburWindLockUntil");
      d.remove(ArtoriaAbilityService.ACTION_LOCK_UNTIL);
      d.remove(ArtoriaAbilityService.COUNTER_UNTIL);
      d.remove(ArtoriaAbilityService.COUNTER_FACING_X);
      d.remove(ArtoriaAbilityService.COUNTER_FACING_Z);
      syncSwordVisibility(owner);
   }

   @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e) {
      if (e.getEntity() instanceof LivingEntity living && !living.level().isClientSide()
         && (isArtoria(living) || living.getPersistentData().contains(JET_USED))) tick(living);
   }
   @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
      clearTemporary(e.getEntity());
   }
   @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) {
      if (isArtoria(e.getEntity()) || e.getEntity().getPersistentData().contains(CHARGES)) clearTemporary(e.getEntity());
   }
   @SubscribeEvent public static void loaded(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
      if (!e.getLevel().isClientSide() && e.loadedFromDisk() && e.getEntity() instanceof ArtoriaPendragonEntity npc)
         clearTemporary(npc);
   }
   @SubscribeEvent public static void reload(net.neoforged.neoforge.event.OnDatapackSyncEvent e) {
      if (e.getPlayer() != null) return;
      for (var level : e.getPlayerList().getServer().getAllLevels()) for (var entity : level.getAllEntities())
         if (entity instanceof LivingEntity living && isArtoria(living)) clearTemporary(living);
   }
   @SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) {
      clearTemporary(e.getEntity());
   }
   @SubscribeEvent public static void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) {
      if (isArtoria(e.getEntity())) clearTemporary(e.getEntity());
   }
   @SubscribeEvent public static void clone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone e) {
      CompoundTag from = e.getOriginal().getPersistentData(), to = e.getEntity().getPersistentData();
      for (String key : new String[]{JET_USED, CHARGES, RESTORE, NP_READY, NP_SHORT_READY, WIND_READY, COMBO_READY, ArtoriaPendragonCombatHelper.TAG_MANA_BURST_REOPEN_AT})
         if (from.contains(key)) to.put(key, from.get(key).copy());
   }
   @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e) { JUDGMENTS.clear(); }
}
