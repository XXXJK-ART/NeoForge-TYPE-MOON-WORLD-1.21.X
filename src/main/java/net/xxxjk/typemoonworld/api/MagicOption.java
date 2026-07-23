package net.xxxjk.typemoonworld.api;

import java.util.List;

/** Generic server-described magic GUI control. */
public record MagicOption(String key, Kind kind, String defaultValue, int min, int max, List<String> values) {
   public enum Kind { BOOLEAN, ENUM, INTEGER, TARGET, ITEM }
   public MagicOption {
      key = key == null ? "option" : key.length() > 64 ? key.substring(0, 64) : key;
      kind = kind == null ? Kind.ENUM : kind;
      defaultValue = defaultValue == null ? "" : defaultValue.length() > 128 ? defaultValue.substring(0, 128) : defaultValue;
      min = Math.max(-1_000_000, min); max = Math.min(1_000_000, Math.max(min, max));
      values = values == null ? List.of() : values.stream().limit(64).map(v -> v == null ? "" : v.substring(0, Math.min(128, v.length()))).toList();
   }
}
