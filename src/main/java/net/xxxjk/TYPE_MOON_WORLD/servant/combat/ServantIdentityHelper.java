package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.EntityTypeTags;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantFaction;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;

public final class ServantIdentityHelper {
   private static final String CARD_TRAIT_LIST_TAG = "ServantCardTraitKeys";
   private static final String CARD_FACTION_TAG = "ServantCardFaction";
   private static final String TRAIT_PREFIX = "TypeMoonTrait_";

   private ServantIdentityHelper() {
   }

   @Nullable
   public static ServantDefinition definitionOf(LivingEntity entity) {
      if (entity instanceof ServantEntity servant) {
         return servant.getDefinition();
      }
      if (entity instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.servant_card_transformed && vars.servant_card_id != null && !vars.servant_card_id.isBlank()) {
            return ServantDataRegistry.get(vars.servant_card_id);
         }
      }
      return null;
   }

   @Nullable
   public static ServantFaction factionOf(LivingEntity entity) {
      ServantDefinition definition = definitionOf(entity);
      if (definition != null) {
         return definition.faction();
      }
      String stored = entity == null ? "" : entity.getPersistentData().getString(CARD_FACTION_TAG);
      return stored.isBlank() ? null : ServantFaction.fromKey(stored);
   }

   public static List<ServantTraitTag> traitsOf(LivingEntity entity) {
      ServantDefinition definition = definitionOf(entity);
      if (definition != null) {
         return definition.traits();
      }
      if (entity == null) {
         return Collections.emptyList();
      }
      String raw = entity.getPersistentData().getString(CARD_TRAIT_LIST_TAG);
      if (!raw.isBlank()) {
         List<ServantTraitTag> traits = new ArrayList<>();
         for (String key : raw.split(",")) {
            if (!key.isBlank()) {
               traits.add(ServantTraitTag.fromKey(key.trim()));
            }
         }
         return traits;
      }
      List<ServantTraitTag> traits = new ArrayList<>();
      for (ServantTraitTag trait : ServantTraitTag.values()) {
         if (entity.getPersistentData().getBoolean(TRAIT_PREFIX + trait.key())) {
            traits.add(trait);
         }
      }
      return traits;
   }

   public static List<String> skillIdsOf(LivingEntity entity) {
      ServantDefinition definition = definitionOf(entity);
      return definition == null ? Collections.emptyList() : definition.skillIds();
   }

   public static boolean hasTrait(LivingEntity entity, ServantTraitTag trait) {
      if (trait == ServantTraitTag.DIVINE && entity instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!PassiveService.effectsSuppressed(vars) && PassiveService.has(vars, PassiveService.DIVINITY)) return true;
      }
      if (trait == ServantTraitTag.UNDEAD && entity != null && entity.getType().is(EntityTypeTags.UNDEAD)) {
         return true;
      }
      return trait != null && traitsOf(entity).contains(trait);
   }

   public static boolean isServantLike(LivingEntity entity) {
      return definitionOf(entity) != null;
   }
}
