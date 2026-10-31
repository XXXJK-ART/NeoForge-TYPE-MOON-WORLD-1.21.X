package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.AttackSegmentAccumulator;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatService;

/** Routes before ServerPlayer.hurt: the original master never enters armor, shields or damage events. */
public final class ServantMasterDamageRouter {
   private static final ThreadLocal<Segment> SEGMENT = new ThreadLocal<>();
   private static final ThreadLocal<Delivery> DELIVERY = new ThreadLocal<>();
   private static final ThreadLocal<LivingEntity> SPECIAL_TARGET = new ThreadLocal<>();
   private record Delivery(ServerPlayer target, DamageSource source, float amount, boolean resetFrames, boolean shared,
      Consumer<LivingEntity> operation) {}
   private ServantMasterDamageRouter() {}

   /** Call once around a target loop, separately for every projectile, wave and damage tick. */
   public static Segment attackSegment() {
      Segment segment = new Segment(SEGMENT.get());
      SEGMENT.set(segment);
      return segment;
   }

   /** Deferred target callbacks retain their originating attack segment and individual pulse delay. */
   public static Runnable deferred(int delay, Runnable action) {
      Segment origin = SEGMENT.get();
      if (origin == null) return action;
      var callbacks = origin.deferred.computeIfAbsent(delay, unused -> new java.util.ArrayList<>());
      callbacks.add(action);
      if (callbacks.size() > 1) return null;
      return () -> {
         try (var pulse = attackSegment()) {
            for (Runnable callback : callbacks) {
               try { callback.run(); }
               catch (RuntimeException error) { net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Error in deferred attack segment", error); }
            }
         }
      };
   }

   public static boolean hurt(ServerPlayer original, DamageSource source, float amount, BooleanSupplier vanilla) {
      if (SPECIAL_TARGET.get() == original) return vanilla.getAsBoolean();
      if (original.isCreative() || original.isSpectator()) return vanilla.getAsBoolean();
      Delivery delivering = DELIVERY.get();
      if (delivering != null && delivering.target() == original && delivering.source() == source) return vanilla.getAsBoolean();
      boolean carried = ServantMasterCarryService.isCarriedMaster(original);
      if (carried && (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL))) return false;
      Segment segment = SEGMENT.get();
      ServerPlayer recipient;
      if (segment != null) {
         segment.snapshot(original.level());
         recipient = segment.recipients.get(original.getUUID());
      } else recipient = ServantMasterCarryService.protector(original);
      if (recipient == null) return vanilla.getAsBoolean();
      boolean shared = source.getEntity() instanceof LivingEntity attacker
         && attacker.getPersistentData().getBoolean(ServantCombatService.SHARED_HIT_TAG);
      Delivery hit = new Delivery(recipient, source, amount, original.invulnerableTime == 0, shared, null);
      if (segment != null) {
         segment.hits.add(recipient.getUUID(), original.getUUID(), amount, hit);
         return true;
      }
      return deliver(hit);
   }

   /** Encloses forced-health settlement too: a legacy fallback must not kill the original master. */
   public static boolean special(LivingEntity original, float amount, Consumer<LivingEntity> operation) {
      if (!(original instanceof ServerPlayer player) || SPECIAL_TARGET.get() == original) return false;
      Segment segment = SEGMENT.get();
      ServerPlayer protector;
      if (segment != null) { segment.snapshot(player.level()); protector = segment.recipients.get(player.getUUID()); }
      else protector = ServantMasterCarryService.protector(player);
      if (protector == null) return false;
      Delivery hit = new Delivery(protector, null, amount, true, false, operation);
      if (segment != null) segment.hits.add(protector.getUUID(), original.getUUID(), amount, hit);
      else deliver(hit);
      return true;
   }

   public static LivingEntity recipient(LivingEntity original) {
      if (!(original instanceof ServerPlayer player)) return original;
      Segment segment = SEGMENT.get();
      ServerPlayer protector;
      if (segment != null) { segment.snapshot(player.level()); protector = segment.recipients.get(player.getUUID()); }
      else protector = ServantMasterCarryService.protector(player);
      return protector == null ? original : protector;
   }

   public static boolean isSharedDelivery(LivingEntity target) {
      Delivery hit = DELIVERY.get();
      return hit != null && hit.target() == target && hit.shared();
   }

   private static boolean deliver(Delivery hit) {
      // A dead protector consumes this segment without spilling it onto the original master.
      if (!hit.target().isAlive() || hit.target().isRemoved()) return false;
      Delivery previous = DELIVERY.get();
      LivingEntity previousSpecial = SPECIAL_TARGET.get();
      DELIVERY.set(hit);
      try {
         if (hit.resetFrames()) hit.target().invulnerableTime = 0;
         if (hit.operation() != null) {
            SPECIAL_TARGET.set(hit.target());
            hit.operation().accept(hit.target());
            return true;
         }
         return hit.target().hurt(hit.source(), hit.amount());
      } finally {
         if (previousSpecial == null) SPECIAL_TARGET.remove(); else SPECIAL_TARGET.set(previousSpecial);
         if (previous == null) DELIVERY.remove(); else DELIVERY.set(previous);
         if (!hit.target().isAlive()) ServantMasterCarryService.dismount(hit.target());
      }
   }

   public static final class Segment implements AutoCloseable {
      private final Segment parent;
      private final Map<UUID, ServerPlayer> recipients = new HashMap<>();
      private final AttackSegmentAccumulator<UUID, Delivery> hits = new AttackSegmentAccumulator<>();
      private final Map<Integer, java.util.List<Runnable>> deferred = new HashMap<>();
      private Level level;
      private boolean closed;
      private Segment(Segment parent) { this.parent = parent; }
      private void snapshot(Level world) {
         if (level != null) return;
         level = world;
         for (var player : world.players()) if (player instanceof ServerPlayer master) {
            ServerPlayer protector = ServantMasterCarryService.protector(master);
            if (protector != null) {
               recipients.put(master.getUUID(), protector);
               recipients.put(protector.getUUID(), protector);
            }
         }
      }
      @Override public void close() {
         if (closed) return;
         closed = true;
         if (parent == null) SEGMENT.remove(); else SEGMENT.set(parent);
         for (Delivery hit : hits.values()) deliver(hit);
      }
   }
}
