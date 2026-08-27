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
      Map<String, RuneDefinition> result = new LinkedHashMap<>();
      for (int i = 0; i < ids.length; i++) {
         EnumMap<RunePosition, String> semantics = new EnumMap<>(RunePosition.class);
         semantics.put(RunePosition.TRIGGER, triggers[i]);
         semantics.put(RunePosition.EFFECT, effects[i]);
         semantics.put(RunePosition.MODIFIER, modifiers[i]);
         semantics.put(RunePosition.TERMINAL, terminals[i]);
         String family = effects[i];
         result.put(ids[i], new RuneDefinition(
            ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, ids[i]), names[i], 0xFFB23A + (i * 0x030303),
            ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/gui/runes/" + ids[i] + ".png"),
            50.0D, Set.of("air", "stone", "weapon", "armor", "tool", "body"), Set.of(), Set.of(family), semantics));
      }
      return result;
   }
}
