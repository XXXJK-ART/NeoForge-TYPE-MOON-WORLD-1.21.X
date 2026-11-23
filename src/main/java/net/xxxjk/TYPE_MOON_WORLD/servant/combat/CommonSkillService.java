package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;

/** Single attribute application for all servant forms; ordinary player divinity uses the same table. */
public final class CommonSkillService {
   private CommonSkillService() {}
   public static float legacyDivinity(LivingEntity e) {
      return CommonSkillRules.highest(ServantIdentityHelper.skillIdsOf(e), "divinity_") > 0 ? 0 : e.getPersistentData().getFloat("DivinityFlatDamage");
   }
   public static void reconcile(LivingEntity entity) {
      var definition = ServantIdentityHelper.definitionOf(entity);
      if (definition != null && "heracles".equals(definition.id())) {
         if (entity.getAttribute(Attributes.ATTACK_DAMAGE) != null) entity.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "frenzy_atk_boost"));
         entity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "frenzy_speed_boost"));
      }
      var skills = ServantIdentityHelper.skillIdsOf(entity);
      var bonus = CommonSkillRules.bonus(CommonSkillRules.highest(skills, "mad_enhancement_"), CommonSkillRules.highest(skills, "divinity_"));
      modifier(entity.getAttribute(Attributes.MAX_HEALTH), "common_skill_health", bonus.health(), AttributeModifier.Operation.ADD_VALUE);
      modifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), "common_skill_attack", bonus.attack(), AttributeModifier.Operation.ADD_VALUE);
      modifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), "common_skill_speed", bonus.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      int avengerRank = Math.max(CommonSkillRules.highest(skills, "avenger_"), CommonSkillRules.highest(skills, "jeanne_alter_avenger_"));
      var avenger = CommonSkillRules.avenger(avengerRank);
      modifier(entity.getAttribute(Attributes.MAX_HEALTH), "common_skill_avenger_health", avenger.health(), AttributeModifier.Operation.ADD_VALUE);
      modifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), "common_skill_avenger_attack", avenger.attack(), AttributeModifier.Operation.ADD_VALUE);
      modifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), "common_skill_avenger_speed", avenger.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      // Old E-A executors may run during reload. Their damage must never be applied again.
      if (CommonSkillRules.highest(skills, "divinity_") > 0) entity.getPersistentData().remove("DivinityFlatDamage");
      if (CommonSkillRules.highest(skills, "mad_enhancement_") > 0) entity.getPersistentData().remove("MadEnhancementDamageBonus");
      if (entity instanceof ServerPlayer player) PassiveService.reconcileAttributes(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
   }
   public static void modifier(AttributeInstance attribute, String name, double value, AttributeModifier.Operation operation) {
      if (attribute == null) return;
      var id = ResourceLocation.fromNamespaceAndPath("typemoonworld", name);
      var old = attribute.getModifier(id);
      if (old != null && old.amount() == value && old.operation() == operation) return;
      attribute.removeModifier(id);
      if (value != 0) attribute.addPermanentModifier(new AttributeModifier(id, value, operation));
   }
}
