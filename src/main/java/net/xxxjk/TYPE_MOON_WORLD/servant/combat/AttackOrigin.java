package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaDamageSource;

/** No UUID, display name or enchantment: changing a wielder cannot reset adaptation. */
public record AttackOrigin(String servant, String item, String skill) {
   public String key() { return servant + "|" + item + "|" + skill; }
   public static AttackOrigin skill(String servant, String skill) { return new AttackOrigin(servant, "", skill); }
   public static AttackOrigin of(DamageSource source) {
      if (source instanceof CombatDamageSource named) return named.origin();
      if (source instanceof net.xxxjk.typemoonworld.api.WrappedDamageSource wrapped) return of(wrapped.baseSource());
      if (source instanceof OdaDamageSource oda) return skill("oda_nobunaga", oda.ability().name().toLowerCase(java.util.Locale.ROOT));
      String type = source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse(source.getMsgId());
      if (source.getEntity() instanceof LivingEntity attacker) {
         var definition = ServantIdentityHelper.definitionOf(attacker);
         String servant = definition == null ? "" : definition.id();
         String item = BuiltInRegistries.ITEM.getKey(attacker.getMainHandItem().getItem()).toString();
         if (!type.startsWith("minecraft:") && !type.equals("neoforge:poison")) return skill(servant,type);
         if (source.getDirectEntity() == attacker) return new AttackOrigin(servant, item, "basic");
         String projectile = source.getDirectEntity() == null ? type : BuiltInRegistries.ENTITY_TYPE.getKey(source.getDirectEntity().getType()).toString();
         return skill(servant, projectile);
      }
      return skill("environment", source.is(DamageTypeTags.IS_FIRE) ? "fire" : type);
   }
}
