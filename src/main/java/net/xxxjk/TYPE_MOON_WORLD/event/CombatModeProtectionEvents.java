package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class CombatModeProtectionEvents {
   private CombatModeProtectionEvents() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
      LivingEntity target = event.getNewAboutToBeSetTarget();
      if (target == null || !EntityUtils.isImmunePlayerTarget(target)) {
         return;
      }

      LivingEntity attacker = event.getEntity();
      if (isTypeMoonEntity(attacker) && !(attacker instanceof RyougiShikiEntity)) {
         event.setNewAboutToBeSetTarget(null);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onIncomingDamage(LivingIncomingDamageEvent event) {
      if (EntityUtils.isImmunePlayerTarget(event.getEntity()) && !isMysticEyesAttack(event)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onProjectileImpact(ProjectileImpactEvent event) {
      if (event.getRayTraceResult() instanceof EntityHitResult hit
         && EntityUtils.isImmunePlayerTarget(hit.getEntity())) {
         event.setCanceled(true);
      }
   }

   private static boolean isMysticEyesAttack(LivingIncomingDamageEvent event) {
      return event.getSource().getEntity() instanceof RyougiShikiEntity
         || event.getSource().getDirectEntity() instanceof RyougiShikiEntity;
   }

   private static boolean isTypeMoonEntity(Entity entity) {
      return entity != null
         && TYPE_MOON_WORLD.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
   }
}
