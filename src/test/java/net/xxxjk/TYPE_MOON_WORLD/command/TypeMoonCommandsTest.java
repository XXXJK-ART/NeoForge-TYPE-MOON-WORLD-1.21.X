package net.xxxjk.TYPE_MOON_WORLD.command;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.xxxjk.TYPE_MOON_WORLD.passive.PassiveService;
import net.xxxjk.TYPE_MOON_WORLD.talent.TalentService;
import org.junit.jupiter.api.Test;

class TypeMoonCommandsTest {
   @Test
   void churchMagicsAreIncludedInLearnAllAndPlayerMaxCatalog() throws ReflectiveOperationException {
      Field field = TypeMoonCommands.class.getDeclaredField("ALL_MAGICS");
      field.setAccessible(true);
      Set<String> ids = Set.copyOf(Arrays.asList((String[])field.get(null)));

      assertTrue(ids.contains("black_key_fire_engraving"));
      assertTrue(ids.contains("stigma"));
      assertTrue(ids.contains("ubw_sword_control"));
      assertTrue(ids.contains("entity_displacement"));
      assertFalse(ids.contains(TalentService.MONSTROUS_STRENGTH));
      assertFalse(ids.contains(TalentService.CLAIRVOYANCE));
   }

   @Test
   void administratorCommandCatalogsExposeOnlySupportedTalentAndPassiveIds() {
      assertTrue(TalentService.IDS.containsAll(Set.of("monstrous_strength", "clairvoyance")));
      assertTrue(PassiveService.IDS.containsAll(Set.of(
         "divinity", "clairvoyance", "mind_eye_true", "mind_eye_false", "instinct")));
   }

   @Test
   void talentAndPassiveCommandsExposeSuggestionsSelfAndTargetExecutionPaths() {
      CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
      TypeMoonCommands.register(dispatcher);
      CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild("typemoon");
      assertNotNull(root);

      CommandNode<CommandSourceStack> talent = root.getChild("talent");
      CommandNode<CommandSourceStack> talentGrantId = talent.getChild("grant").getChild("talent_id");
      CommandNode<CommandSourceStack> proficiency = talentGrantId.getChild("proficiency");
      assertNotNull(((ArgumentCommandNode<CommandSourceStack, ?>)talentGrantId).getCustomSuggestions());
      assertNotNull(proficiency.getCommand());
      assertNotNull(proficiency.getChild("target").getCommand());
      assertNotNull(talent.getChild("revoke").getChild("talent_id").getCommand());

      CommandNode<CommandSourceStack> passive = root.getChild("passive");
      CommandNode<CommandSourceStack> passiveGrantId = passive.getChild("grant").getChild("passive_id");
      CommandNode<CommandSourceStack> rank = passiveGrantId.getChild("rank");
      assertNotNull(((ArgumentCommandNode<CommandSourceStack, ?>)passiveGrantId).getCustomSuggestions());
      assertNotNull(((ArgumentCommandNode<CommandSourceStack, ?>)rank).getCustomSuggestions());
      assertNotNull(rank.getCommand());
      assertNotNull(rank.getChild("target").getCommand());
      assertNotNull(passive.getChild("revoke").getChild("passive_id").getCommand());
      assertNotNull(passive.getChild("list").getCommand());
      assertNotNull(passive.getChild("list").getChild("target").getCommand());
   }
}
