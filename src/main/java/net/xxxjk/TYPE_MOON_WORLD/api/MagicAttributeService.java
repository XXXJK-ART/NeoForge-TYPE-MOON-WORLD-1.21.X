package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.MagicAttributeAccess;
import net.xxxjk.typemoonworld.api.MagicAttributes;
import net.xxxjk.typemoonworld.api.MagicAttributeProvider;

public final class MagicAttributeService {
   private static final MagicAttributeAccess EMPTY = new MagicAttributeAccess() {
      @Override public boolean has(ResourceLocation attribute) { return false; }
      @Override public Set<ResourceLocation> attributes() { return Set.of(); }
   };

   private MagicAttributeService() { }

   public static MagicAttributeAccess access(LivingEntity entity) {
      if (entity == null) return EMPTY;
      TypeMoonWorldModVariables.PlayerVariables vars = entity.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return new MagicAttributeAccess() {
         @Override public boolean has(ResourceLocation attribute) {
            if (MagicAttributeService.has(vars, attribute)) return true;
            return InternalApiProvider.attributeProviders().values().stream().anyMatch(provider -> provider.has(entity, attribute));
         }
         @Override public Set<ResourceLocation> attributes() {
            Set<ResourceLocation> result = new LinkedHashSet<>(MagicAttributeService.attributes(vars));
            for (MagicAttributeProvider provider : InternalApiProvider.attributeProviders().values()) {
               Set<ResourceLocation> provided = provider.attributes(entity);
               if (provided != null) result.addAll(provided);
            }
            return Set.copyOf(result);
         }
      };
   }

   public static boolean meets(TypeMoonWorldModVariables.PlayerVariables vars, List<ResourceLocation> required) {
      return required == null || required.stream().allMatch(attribute -> has(vars, attribute));
   }

   public static boolean has(TypeMoonWorldModVariables.PlayerVariables vars, ResourceLocation attribute) {
      if (vars == null || attribute == null || !"typemoonworld".equals(attribute.getNamespace())) return false;
      return switch (attribute.getPath()) {
         case "earth" -> vars.player_magic_attributes_earth;
         case "water" -> vars.player_magic_attributes_water;
         case "fire" -> vars.player_magic_attributes_fire;
         case "wind" -> vars.player_magic_attributes_wind;
         case "ether" -> vars.player_magic_attributes_ether;
         case "none" -> vars.player_magic_attributes_none;
         case "imaginary_number" -> vars.player_magic_attributes_imaginary_number;
         case "sword" -> vars.player_magic_attributes_sword;
         default -> false;
      };
   }

   private static Set<ResourceLocation> attributes(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) return Set.of();
      Set<ResourceLocation> result = new LinkedHashSet<>();
      if (vars.player_magic_attributes_earth) result.add(MagicAttributes.EARTH);
      if (vars.player_magic_attributes_water) result.add(MagicAttributes.WATER);
      if (vars.player_magic_attributes_fire) result.add(MagicAttributes.FIRE);
      if (vars.player_magic_attributes_wind) result.add(MagicAttributes.WIND);
      if (vars.player_magic_attributes_ether) result.add(MagicAttributes.ETHER);
      if (vars.player_magic_attributes_none) result.add(MagicAttributes.NONE);
      if (vars.player_magic_attributes_imaginary_number) result.add(MagicAttributes.IMAGINARY_NUMBER);
      if (vars.player_magic_attributes_sword) result.add(MagicAttributes.SWORD);
      return Set.copyOf(result);
   }
}
