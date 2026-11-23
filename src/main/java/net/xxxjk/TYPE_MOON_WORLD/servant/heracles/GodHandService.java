package net.xxxjk.TYPE_MOON_WORLD.servant.heracles;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;

public final class GodHandService {
   private static final String STATE = "GodHandStateV2";
   private GodHandService() {}
   public static void initialize(LivingEntity entity) {
      var data = entity.getPersistentData();
      data.putBoolean("GodHandActive", true);
      data.putFloat("GodHandThreshold", 15);
      data.remove("CritDamageBoostActive");data.remove("CritDamageMultiplier");data.remove("CritDamageTicksRemaining");
      if (!data.contains(STATE)) write(entity, new GodHandState(data.contains("GodHandLives") ? data.getInt("GodHandLives") : 11, Set.of(), "", 0, Set.of()));
      for (String key : new String[]{"GodHandReviveLockUntil", "GodHandHighDamageReviveUntil", "GodHandCurrentResistance", "GodHandAdaptiveReduction", "GodHandAdaptiveMax", "GodHandStrongCost", "GodHandExtraStrongCost"}) data.remove(key);
   }
   public static GodHandState read(LivingEntity entity) {
      CompoundTag data = entity.getPersistentData(), state = data.getCompound(STATE);
      return new GodHandState(data.getInt("GodHandLives"), strings(state, "adapted"), state.getString("pending"), state.getInt("ticks"), strings(state, "special"));
   }
   private static Set<String> strings(CompoundTag data, String key) {
      Set<String> result = new HashSet<>();
      for (Tag value : data.getList(key, Tag.TAG_STRING)) result.add(value.getAsString());
      return result;
   }
   private static ListTag list(Set<String> values) { ListTag list = new ListTag(); values.stream().sorted().forEach(v -> list.add(StringTag.valueOf(v))); return list; }
   public static void write(LivingEntity entity, GodHandState state) {
      CompoundTag tag = new CompoundTag();
      tag.put("adapted", list(state.adapted())); tag.putString("pending", state.pending()); tag.putInt("ticks", state.remainingTicks()); tag.put("special", list(state.specialImmunities()));
      entity.getPersistentData().put(STATE, tag); entity.getPersistentData().putInt("GodHandLives", state.lives());
   }
   public static void clear(LivingEntity entity) {
      HeraclesPresentationService.clear(entity);
      for (String key : new java.util.ArrayList<>(entity.getPersistentData().getAllKeys())) if (key.startsWith("GodHand")) entity.getPersistentData().remove(key);
   }
   public static void tick(LivingEntity entity) {
      if (!HeraclesGodHandHelper.hasGodHand(entity) || !entity.isAlive()) return;
      initialize(entity);
      var state = read(entity);
      if (state.remainingTicks() > 0) {
         var next = state.tick();
         write(entity, next);
         if (state.remainingTicks() == 1 && !state.pending().isBlank()
            && !state.adapted().contains(state.pending()) && next.adapted().contains(state.pending()))
            HeraclesPresentationService.adapted(entity);
      }
   }
   public static boolean beforeDefense(LivingIncomingDamageEvent event) {
      var entity = event.getEntity();
      if (!HeraclesGodHandHelper.hasGodHand(entity) || poison(event.getSource())) return false;
      initialize(entity);
      var hit = DamageContexts.capture(event);
      float amount = hit.parts().godHand(read(entity).adapted().contains(hit.origin().key()));
      event.setAmount(amount);
      if (amount <= 0) { event.setCanceled(true); return true; }
      return false;
   }
   public static boolean poison(DamageSource source) { return source.is(NeoForgeMod.POISON_DAMAGE) || source.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("typemoonworld","poison"))); }
   public static boolean revive(LivingEntity entity, DamageSource source, AttackOrigin origin) {
      if (!HeraclesGodHandHelper.hasGodHand(entity) || entity.getPersistentData().getBoolean("CausalSevered") || read(entity).lives() <= 0) return false;
      write(entity, read(entity).revive(origin.key(), poison(source)));
      restore(entity);
      return true;
   }
   public static boolean immune(LivingEntity entity, String skill) { return HeraclesGodHandHelper.hasGodHand(entity) && read(entity).specialImmunities().contains(skill); }
   /** One impact may carry both ordinary damage and a concept; it can spend at most one revival. */
   public static boolean conceptHit(LivingEntity entity, DamageSource source, float ordinaryDamage, String skill, boolean conceptSucceeded) {
      if (!HeraclesGodHandHelper.hasGodHand(entity)) return false;
      initialize(entity);
      int before=read(entity).lives();long continuationBefore=entity.getPersistentData().getLong("CommonContinuationReady");
      if (ordinaryDamage>0) {
         entity.invulnerableTime=0;
         entity.hurt(new CombatDamageSource(source,AttackOrigin.skill(skill.substring(0,skill.indexOf(':')),skill.substring(skill.indexOf(':')+1))),ordinaryDamage);
      }
      if (!conceptSucceeded || !entity.isAlive() || immune(entity,skill)) return true;
      if (read(entity).lives()<before) {
         var state=read(entity);var immunities=new HashSet<>(state.specialImmunities());immunities.add(skill);
         write(entity,new GodHandState(state.lives(),state.adapted(),state.pending(),state.remainingTicks(),immunities));
         return true;
      }
      if (entity.getPersistentData().getLong("CommonContinuationReady")!=continuationBefore || special(entity,skill)) return true;
      entity.setHealth(0);entity.die(new CombatDamageSource(source,AttackOrigin.skill(skill.substring(0,skill.indexOf(':')),skill.substring(skill.indexOf(':')+1))));
      return true;
   }
   /** True means the concept is handled, including already immune. Causal severance always wins. */
   public static boolean special(LivingEntity entity, String skill) {
      if (!HeraclesGodHandHelper.hasGodHand(entity) || entity.getPersistentData().getBoolean("CausalSevered")) return false;
      initialize(entity);
      var state = read(entity);
      if (state.specialImmunities().contains(skill)) return true;
      if (state.lives() == 0) return false;
      write(entity, state.special(skill)); restore(entity); return true;
   }
   private static void restore(LivingEntity entity) {
      entity.setHealth(entity.getMaxHealth());
      entity.invulnerableTime = 0; entity.hurtTime = 0; entity.hurtDuration = 0;
      HeraclesPresentationService.revive(entity);
   }
   /** Explicit poison is health damage, never armor/absorption/dodge damage. */
   public static void poisonHit(LivingIncomingDamageEvent event) {
      LivingEntity entity = event.getEntity(); float damage = Math.max(0, event.getAmount());
      DamageSource source = event.getSource();
      event.setCanceled(true); event.setAmount(0);
      if (source.getEntity() instanceof LivingEntity attacker) entity.setLastHurtByMob(attacker);
      if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) entity.setLastHurtByPlayer(player);
      entity.getCombatTracker().recordDamage(source,damage);
      if (entity.getHealth() <= damage && (revive(entity, source, AttackOrigin.of(source)) || BattleContinuationService.trigger(entity))) return;
      entity.setHealth(Math.max(0, entity.getHealth() - damage));
      if (entity.getHealth() <= 0) entity.die(source);
   }
}
