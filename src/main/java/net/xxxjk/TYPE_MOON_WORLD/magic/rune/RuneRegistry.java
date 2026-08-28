package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

/** Fixed Elder Futhark rune catalogue. It is deliberately not a NeoForge registry. */
public final class RuneRegistry {
   private static final Map<String, RuneDefinition> DEFINITIONS = createDefinitions();
   private RuneRegistry() { }

   public static RuneDefinition get(String id) {
      if (id == null) return null;
      String path = id.startsWith(TYPE_MOON_WORLD.MOD_ID + ":") ? id.substring(TYPE_MOON_WORLD.MOD_ID.length() + 1) : id;
      return DEFINITIONS.get(path);
   }
   public static boolean isKnown(String id) { return get(id) != null; }
   public static List<RuneDefinition> all() { return List.copyOf(DEFINITIONS.values()); }
   public static Set<String> ids() { return Collections.unmodifiableSet(DEFINITIONS.keySet()); }

   private static Map<String, RuneDefinition> createDefinitions() {
      String[] ids = {"fehu", "uruz", "thurisaz", "ansuz", "raidho", "kenaz", "gebo", "wunjo", "hagalaz", "nauthiz", "isa", "jera", "eihwaz", "perthro", "algiz", "sowilo", "tiwaz", "berkano", "ehwaz", "mannaz", "laguz", "ingwaz", "dagaz", "othala"};
      String[] names = {"Fehu", "Uruz", "Thurisaz", "Ansuz", "Raidho", "Kenaz", "Gebo", "Wunjo", "Hagalaz", "Nauthiz", "Isa", "Jera", "Eihwaz", "Perthro", "Algiz", "Sowilo", "Tiwaz", "Berkano", "Ehwaz", "Mannaz", "Laguz", "Ingwaz", "Dagaz", "Othala"};
      String[] triggers = {"wealth", "strength", "break", "command", "move", "ignite", "bind", "joy", "storm", "need", "freeze", "cycle", "ward", "chance", "protect", "shine", "judge", "grow", "travel", "self", "flow", "charge", "transform", "inherit"};
      String[] effects = {"create", "reinforce", "impact", "speak", "path", "fire", "share", "heal", "ice", "drain", "barrier", "harvest", "shadow", "random", "shield", "light", "pierce", "nature", "speed", "mind", "water", "energy", "change", "earth"};
      String[] modifiers = {"amplify", "power", "shatter", "range", "swift", "heat", "link", "duration", "area", "cost", "slow", "repeat", "resist", "luck", "guard", "accuracy", "critical", "restore", "mobility", "focus", "control", "store", "fuse", "persist"};
      String[] terminals = {"release", "strike", "detonate", "seal", "return", "burn", "transfer", "bless", "disperse", "sacrifice", "endure", "complete", "banish", "reveal", "protect", "purify", "execute", "renew", "escape", "awaken", "dissolve", "discharge", "renew", "anchor"};
      int[] colors = {
         0xFFFFB84D, 0xFFFFCC66, 0xFFFF6B6B, 0xFF69D2E7, 0xFF7BD389, 0xFFFF8A65,
         0xFFE5A4FF, 0xFFFFD166, 0xFFB8C0FF, 0xFFFF9F9F, 0xFFA8DADC, 0xFFC4E17F,
         0xFFC2B0FF, 0xFFDDA15E, 0xFF8AE1FC, 0xFFFFE66D, 0xFFF28482, 0xFF9BE564,
         0xFF6ECEDA, 0xFFF6BD60, 0xFF5BC0EB, 0xFFC77DFF, 0xFFF7A072, 0xFFB8E0D2
      };
      Map<String, RuneDefinition> result = new LinkedHashMap<>();
      for (int i = 0; i < ids.length; i++) {
         EnumMap<RunePosition, String> semantics = new EnumMap<>(RunePosition.class);
         semantics.put(RunePosition.TRIGGER, triggers[i]);
         semantics.put(RunePosition.EFFECT, effects[i]);
         semantics.put(RunePosition.MODIFIER, modifiers[i]);
         semantics.put(RunePosition.TERMINAL, terminals[i]);
         String family = effects[i];
         result.put(ids[i], new RuneDefinition(
            ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, ids[i]), names[i], colors[i],
            ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/runes/" + ids[i] + ".png"),
            50.0D, Set.of("air", "stone", "weapon", "armor", "tool", "body"), Set.of(), Set.of(family), semantics));
      }
      return result;
   }
}
