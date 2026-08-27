package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.List;

public record RuneProgramValidationResult(boolean valid, List<String> errors) {
   public RuneProgramValidationResult {
      errors = errors == null ? List.of() : List.copyOf(errors);
   }
   public static RuneProgramValidationResult ok() { return new RuneProgramValidationResult(true, List.of()); }
   public static RuneProgramValidationResult failure(String error) { return new RuneProgramValidationResult(false, List.of(error)); }
}
