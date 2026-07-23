package com.example.tmwtest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.fml.common.Mod;
import net.xxxjk.typemoonworld.api.AiTacticProfile;
import net.xxxjk.typemoonworld.api.AiTactic;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;
import net.xxxjk.typemoonworld.api.MagicOption;
import net.xxxjk.typemoonworld.api.MagicAttributes;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;
import net.xxxjk.typemoonworld.api.MasterProfileData;

@Mod("tmw_test_addon")
public final class TmwTestAddon {
   private static final ResourceLocation SERVANT = ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "test_servant");
   private static final ResourceLocation MAGIC = ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "test_magic");
   public TmwTestAddon() {
      var addon = TypeMoonWorldApi.addon("tmw_test_addon");
      addon.servants().registerDefinition(SERVANT);
      addon.servants().registerAction(ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "wave"), context -> ExecutionResult.SUCCESS);
      addon.cards().registerAction(ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "wave"), context -> ExecutionResult.SUCCESS);
      addon.cards().bindSlot(SERVANT, 0, ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "wave"));
      addon.skills().register(ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "test_skill"), context -> ExecutionResult.SUCCESS);
      addon.noblePhantasms().register(ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "test_np"), context -> ExecutionResult.SUCCESS);
      addon.magics().registerDefinition(new MagicDefinitionData(MAGIC, "magic.tmw_test_addon.test.name", ResourceLocation.fromNamespaceAndPath("typemoonworld", "basic"), ResourceLocation.fromNamespaceAndPath("typemoonworld", "test"), 2, 10, true, true, true, true, false, 12, 20));
      addon.magics().registerExecutor(MAGIC, context -> TypeMoonWorldApi.magicAttributes(context.caster()).has(MagicAttributes.IMAGINARY_NUMBER)
         ? ExecutionResult.SUCCESS.withCost(2) : ExecutionResult.FAILED);
      addon.ai().register(ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "default"), new AiTacticProfile(24, 12, .2, 10, java.util.List.of(new AiTactic(MAGIC, 10, 0, 24, 0, 20))));
      addon.client().registerControl(MAGIC, new MagicOption("power", MagicOption.Kind.INTEGER, "1", 1, 5, java.util.List.of()));
      addon.projections().registerItem("test_projection", context -> true);
      addon.projections().registerStructure("test_structure", context -> true);
      addon.commandSpells().register(ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "test_command"), context -> ExecutionResult.SUCCESS);
      addon.projectiles().register(ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "test_projectile"), context -> true);
      addon.effects().registerParticle("test_particle", ParticleTypes.ENCHANT);
      addon.effects().registerSound("test_sound", SoundEvents.AMETHYST_BLOCK_CHIME);
      addon.damageTypes().key("test_damage");
      addon.gems().registerMagic(MAGIC, type -> 10);
      addon.masters().register(new MasterProfileData(ResourceLocation.fromNamespaceAndPath("tmw_test_addon", "test_master"),
         "master.tmw_test_addon.test", "default", 100, 5, 5), context -> context.magicKnowledge().learn(MAGIC));
      addon.servants().createCard(SERVANT);
      addon.servants().createSummonItem(SERVANT);
   }
}
