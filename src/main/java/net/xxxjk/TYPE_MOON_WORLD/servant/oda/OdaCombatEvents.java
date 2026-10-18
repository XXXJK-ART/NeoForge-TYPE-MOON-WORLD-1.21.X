package net.xxxjk.TYPE_MOON_WORLD.servant.oda;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmDamageClassifier;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantEra;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

@EventBusSubscriber(modid = "typemoonworld")
public final class OdaCombatEvents {
   private OdaCombatEvents() {}
   public static boolean isOda(LivingEntity entity) {
      var definition = ServantIdentityHelper.definitionOf(entity);
      return definition != null && "oda_nobunaga".equals(definition.id());
   }
   public static ServantEra era(LivingEntity entity) {
      var definition = ServantIdentityHelper.definitionOf(entity);
      return definition == null ? null : definition.era();
   }
   public static int divinity(LivingEntity target) {
      int rank = 0;
      boolean divine = ServantIdentityHelper.hasTrait(target, ServantTraitTag.DIVINE);
      for (String skill : ServantIdentityHelper.skillIdsOf(target)) {
         String key = skill.substring(skill.indexOf(':') + 1);
         if (key.startsWith("divinity_")) rank = Math.max(rank, OdaRules.divinity(key, true));
      }
      String explicit = target.getPersistentData().getString("DivinityRank");
      return Math.max(rank, OdaRules.divinity(explicit, divine));
   }
   public static boolean riding(LivingEntity target) {
      return ServantIdentityHelper.hasTrait(target, ServantTraitTag.RIDING)
         || ServantIdentityHelper.hasTrait(target, ServantTraitTag.MOUNTED)
         || ServantIdentityHelper.skillIdsOf(target).stream().anyMatch(id -> id.substring(id.indexOf(':') + 1).startsWith("riding_"));
   }
   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void ownedBurn(LivingIncomingDamageEvent event) {
      if (event.getSource() instanceof OdaDamageSource || !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)
         || !event.getEntity().getPersistentData().hasUUID("OdaBurnOwner")) return;
      var target = event.getEntity();
      LivingEntity caster = OdaAbilityService.burnOwner(target);
      event.setCanceled(true);
      if (caster == null) { target.clearFire(); target.getPersistentData().remove("OdaBurnOwner"); return; }
      target.invulnerableTime = 0;
      target.hurt(new OdaDamageSource(event.getSource(), caster, caster, OdaDamageSource.Ability.HAJUN), event.getAmount());
   }
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void outgoing(LivingIncomingDamageEvent event) {
      if (event.getSource().getEntity() instanceof LivingEntity caster && isOda(caster)) {
         boolean np = event.getSource() instanceof OdaDamageSource oda && oda.ability() == OdaDamageSource.Ability.THREE_THOUSAND;
         double multiplier = OdaRules.special(divinity(event.getEntity()), era(event.getEntity()), np && riding(event.getEntity()));
         if (caster instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity
            && event.getSource() instanceof OdaDamageSource
            && !caster.getPersistentData().getBoolean(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatService.SHARED_HIT_TAG)) {
            multiplier *= net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantDamageCalculator.computeFactionMultiplier(
               ServantIdentityHelper.factionOf(caster), ServantIdentityHelper.factionOf(event.getEntity()));
         }
         if (divinity(event.getEntity()) > 0) OdaNobunagaCombatHelper.applyDivineDefenseBreak(caster, event.getEntity());
         if (caster.getPersistentData().getLong(OdaAbilityService.MANIFEST_UNTIL) > caster.level().getGameTime()) multiplier *= 1.3;
         if (OdaRules.armyOrCastle(NoblePhantasmDamageClassifier.typeOf(event.getSource()))) multiplier *= 1 + OdaRules.strategy("B");
         event.setAmount((float)(event.getAmount() * multiplier));
      }
      if (isOda(event.getEntity()) && OdaRules.armyOrCastle(NoblePhantasmDamageClassifier.typeOf(event.getSource()))) {
         event.setAmount((float)(event.getAmount() * (1 - OdaRules.strategy("B"))));
      }
   }
   // Common defenses have already resolved immunity and dodge; vanilla armor has not run yet.
   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void substitute(LivingIncomingDamageEvent event) {
      if (isOda(event.getEntity()) && event.getAmount() > 0) {
         float remaining = OdaAbilityService.absorb(event.getEntity(), event.getAmount());
         event.setAmount(remaining);
         if (remaining <= 0) event.setCanceled(true);
      }
   }
   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void finalEra(LivingDamageEvent.Pre event) {
      if (event.getSource().getEntity() instanceof LivingEntity caster && isOda(caster)) {
         event.setNewDamage((float)(event.getNewDamage() * OdaRules.eraFinal(era(event.getEntity()))));
      }
   }
}
