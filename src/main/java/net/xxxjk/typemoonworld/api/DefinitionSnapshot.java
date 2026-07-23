package net.xxxjk.typemoonworld.api;

import java.util.Map;

/** Immutable server-authoritative content snapshot consumed by client extensions. */
public record DefinitionSnapshot(long revision, Map<String, String> sections) {
   public DefinitionSnapshot {
      sections = sections == null ? Map.of() : Map.copyOf(sections);
   }
   public String section(String name) { return sections.getOrDefault(name, "{}"); }
}
