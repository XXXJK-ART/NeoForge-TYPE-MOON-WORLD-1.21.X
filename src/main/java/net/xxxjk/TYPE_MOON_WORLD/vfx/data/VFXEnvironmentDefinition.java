package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

public record VFXEnvironmentDefinition(String type, int color, float startTime, float endTime, float fadeIn, float fadeOut, float intensity) {
   public float duration() {
      return Math.max(0.001F, this.endTime);
   }
}
