package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.typemoonworld.api.AddonRegistrar;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Registers Jeanne's data-driven skills and card actions in the core namespace. */
public final class JeanneAlterContent {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "jeanne_alter");
   private JeanneAlterContent() {}
   public static void register() {
      AddonRegistrar addon = TypeMoonWorldApi.addon(TYPE_MOON_WORLD.MOD_ID);
      addon.skills().register(id("jeanne_alter_avenger_b"), JeanneAlterSkills::avengerB);
      addon.skills().register(id("jeanne_alter_oblivion_correction_a"), JeanneAlterSkills::oblivionCorrectionA);
      addon.skills().register(id("jeanne_alter_self_replenishment_a_plus"), JeanneAlterSkills::selfReplenishmentAPlus);
      addon.cards().registerAction(id("jeanne_alter_fire_pillar"), JeanneAlterSkills::firePillar);
      addon.cards().registerAction(id("jeanne_alter_cursed_lance"), JeanneAlterSkills::cursedLance);
      addon.cards().registerAction(id("jeanne_alter_self_modification_ex"), JeanneAlterSkills::selfModification);
      addon.cards().registerAction(id("jeanne_alter_dragon_witch_ex"), JeanneAlterSkills::dragonWitch);
      addon.cards().registerAction(id("jeanne_alter_inferno_body"), JeanneAlterSkills::infernoBody);
      addon.cards().registerAction(id("jeanne_alter_ephemeral_dream_a"), JeanneAlterSkills::ephemeralDream);
      addon.cards().registerAction(id("jeanne_alter_le_grondement_du_haine"), JeanneAlterSkills::noblePhantasm);
      addon.servants().registerAction(id("jeanne_alter_fire_pillar"), JeanneAlterSkills::combatFirePillar);
      addon.servants().registerAction(id("jeanne_alter_cursed_lance"), JeanneAlterSkills::combatCursedLance);
      addon.servants().registerAction(id("jeanne_alter_self_modification_ex"), JeanneAlterSkills::combatSelfModification);
      addon.servants().registerAction(id("jeanne_alter_dragon_witch_ex"), JeanneAlterSkills::combatDragonWitch);
      addon.servants().registerAction(id("jeanne_alter_inferno_body"), JeanneAlterSkills::combatInfernoBody);
      addon.servants().registerAction(id("jeanne_alter_ephemeral_dream_a"), JeanneAlterSkills::combatEphemeralDream);
      addon.servants().registerAction(id("jeanne_alter_le_grondement_du_haine"), JeanneAlterSkills::combatNoblePhantasm);
   }
   private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path); }
}
