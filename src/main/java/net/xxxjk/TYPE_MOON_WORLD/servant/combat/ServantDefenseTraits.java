package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantSkillDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.SkillRank;

public record ServantDefenseTraits(double dodgeReduction, int autoDodges, double guardBonus, double durabilityBonus) {
   public static ServantDefinition definition(LivingEntity entity) {
      return entity instanceof ServantEntity servant ? servant.getDefinition()
         : ServantDataRegistry.get(entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servantCardState().id());
   }

   public static ServantDefenseTraits resolve(LivingEntity entity) {
      ServantDefinition definition = definition(entity);
      double dodge = 0, guard = 0, durability = 0;
      int charges = 0;
      if (definition != null) for (String id : definition.skillIds()) {
         var skill = ServantSkillDataRegistry.get(id);
         if (skill == null || skill.type() != ServantSkillDefinition.SkillType.PASSIVE
            && entity.level().getGameTime() >= entity.getPersistentData().getLong(activeKey(skill.id()))) continue;
         double modifier = SkillRank.modifier(skill.rank());
         if (skill.defense().evasion()) {
            dodge = Math.max(dodge, modifier);
            charges = Math.max(charges, SkillRank.autoDodges(skill.rank()));
         }
         if (skill.defense().guard()) guard = Math.max(guard, modifier);
         if (skill.defense().durability()) durability = Math.max(durability, modifier);
      }
      return new ServantDefenseTraits(dodge, charges, guard, durability);
   }

   public static void activated(LivingEntity entity, String actionId) {
      if (entity == null || actionId == null) return;
      ServantDefinition definition = definition(entity);
      if (definition == null) return;
      for (String id : definition.skillIds()) {
         var skill = ServantSkillDataRegistry.get(id);
         if (skill != null && skill.type() == ServantSkillDefinition.SkillType.ACTIVE
            && (id.equals(actionId) || skill.id().equals(actionId) || skill.defense().activationActions().contains(actionId))) {
            entity.getPersistentData().putLong(activeKey(skill.id()), entity.level().getGameTime() + Math.max(1, skill.durationTicks()));
         }
      }
   }

   public static void tickNpcActiveSkills(ServantEntity servant) {
      if (servant.getTarget() == null || servant.tickCount % 20 != 0) return;
      var definition = servant.getDefinition();
      if (definition == null || ServantCombatSystem.skillsSuppressed(servant)) return;
      long now = servant.level().getGameTime();
      for (String id : definition.skillIds()) {
         var skill = ServantSkillDataRegistry.get(id);
         if (skill == null || skill.type() != ServantSkillDefinition.SkillType.ACTIVE
            || !(skill.defense().evasion() || skill.defense().guard() || skill.defense().durability())) continue;
         String cooldown = "TypeMoonDefenseSkillCooldown:" + skill.id();
         if (now < servant.getPersistentData().getLong(cooldown) || servant.getCurrentMp() < skill.mpCost()) continue;
         var result = net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantSkillRegistry.execute(id,
            new net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext(servant, servant.getTarget(), servant.getCurrentMp(), 0));
         if (result.success()) {
            servant.setCurrentMp(servant.getCurrentMp() - skill.mpCost());
            servant.getPersistentData().putLong(cooldown, now + Math.max(20, skill.cooldownTicks()));
         }
      }
   }
   private static String activeKey(String id) { return "TypeMoonDefenseSkill:" + id; }
}
