package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SoulEchoEntity;

public final class PaleRiderDamageTypes {
   public static final ResourceKey<DamageType> INFECTION = key("pale_rider_infection");
   public static final ResourceKey<DamageType> RAT_BITE = key("pale_rider_rat_bite");
   public static final ResourceKey<DamageType> FAMINE = key("pale_rider_famine");
   public static final ResourceKey<DamageType> CONCEPT_SWORD = key("pale_rider_concept_sword");
   public static final ResourceKey<DamageType> CONCEPT_DEATH = key("pale_rider_concept_death");

   private PaleRiderDamageTypes() {
   }

   public static boolean isInfection(DamageSource source) {
      return source != null && source.is(INFECTION);
   }

   public static boolean isPaleRiderDamage(DamageSource source) {
      if (source == null) return false;
      if (source.is(INFECTION) || source.is(RAT_BITE) || source.is(FAMINE)
         || source.is(CONCEPT_SWORD) || source.is(CONCEPT_DEATH)) return true;
      return isPaleRiderSource(source.getEntity()) || isPaleRiderSource(source.getDirectEntity());
   }

   public static boolean bypassesEnkiduNoblePhantasm(DamageSource source) {
      return source != null && (source.is(INFECTION) || source.is(CONCEPT_DEATH)
         || isUndeadSource(source.getEntity()) || isUndeadSource(source.getDirectEntity()));
   }

   private static boolean isUndeadSource(Entity entity) {
      return entity instanceof SoulEchoEntity || entity != null && entity.getType().is(EntityTypeTags.UNDEAD);
   }

   private static boolean isPaleRiderSource(Entity entity) {
      if (entity instanceof PaleRiderEntity) return true;
      if (entity instanceof OwnedPaleRiderMob owned) return owned.getPaleRiderOwnerUuid() != null;
      if (entity instanceof PaleRiderCrowEntity crow) return crow.getPaleRiderOwnerUuid() != null;
      return entity != null && entity.getPersistentData().getBoolean(PaleRiderInfectionService.TAG_CONTROLLED)
         && entity.getPersistentData().hasUUID(PaleRiderInfectionService.TAG_OWNER);
   }

   private static ResourceKey<DamageType> key(String path) {
      return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path));
   }
}
