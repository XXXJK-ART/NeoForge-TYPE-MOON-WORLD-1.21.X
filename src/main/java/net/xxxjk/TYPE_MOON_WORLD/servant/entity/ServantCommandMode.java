package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

public enum ServantCommandMode {
   FOLLOW,
   GUARD,
   STAY;

   public ServantCommandMode next() {
      return values()[(ordinal() + 1) % values().length];
   }

   public static ServantCommandMode byName(String value) {
      if (value == null) return FOLLOW;
      try { return valueOf(value); } catch (IllegalArgumentException ignored) { return FOLLOW; }
   }
}
