package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record ServantAnimations(String idle, String walk, Map<String, String> actions) {
   public ServantAnimations {
      idle = normalize(idle);
      walk = normalize(walk);
      actions = actions == null || actions.isEmpty() ? Collections.emptyMap() : Map.copyOf(new LinkedHashMap<>(actions));
   }

   public static ServantAnimations legacy(String animationPath) {
      String base = basePrefix(animationPath);
      return new ServantAnimations(base + ".idle", base + ".walk", Collections.emptyMap());
   }

   public static ServantAnimations empty() {
      return new ServantAnimations("", "", Collections.emptyMap());
   }

   public Optional<String> idleAnimation() {
      return Optional.ofNullable(this.idle).filter(s -> !s.isBlank());
   }

   public Optional<String> walkAnimation() {
      return Optional.ofNullable(this.walk).filter(s -> !s.isBlank());
   }

   public Optional<String> actionAnimation(String key) {
      if (key == null || key.isBlank() || this.actions == null) {
         return Optional.empty();
      }

      String animation = this.actions.get(key);
      return Optional.ofNullable(animation).filter(s -> !s.isBlank());
   }

   public static String basePrefix(String animationPath) {
      if (animationPath == null || animationPath.isBlank()) {
         return "animation.heracles";
      }

      String path = animationPath.trim();
      int slash = path.lastIndexOf('/');
      String filename = slash >= 0 ? path.substring(slash + 1) : path;
      if (filename.endsWith(".animation.json")) {
         filename = filename.substring(0, filename.length() - ".animation.json".length());
      }
      return "animation." + filename.replace('-', '_').replace(' ', '_');
   }

   private static String normalize(String value) {
      return value == null ? "" : value.trim();
   }
}
