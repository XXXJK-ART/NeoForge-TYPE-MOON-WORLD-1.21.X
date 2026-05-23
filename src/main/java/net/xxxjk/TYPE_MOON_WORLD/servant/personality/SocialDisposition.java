package net.xxxjk.TYPE_MOON_WORLD.servant.personality;

public enum SocialDisposition {
   HERMIT(0, "hermit"),
   NORMAL(1, "normal"),
   ENTHUSIASTIC(2, "enthusiastic");

   private final int id;
   private final String key;

   SocialDisposition(int id, String key) {
      this.id = id;
      this.key = key;
   }

   public int id() {
      return this.id;
   }

   public String key() {
      return this.key;
   }

   public static SocialDisposition fromKey(String key) {
      if (key == null) {
         return NORMAL;
      }

      for (SocialDisposition disposition : values()) {
         if (disposition.key.equals(key.toLowerCase())) {
            return disposition;
         }
      }

      return NORMAL;
   }

   public static SocialDisposition fromId(int id) {
      for (SocialDisposition disposition : values()) {
         if (disposition.id == id) {
            return disposition;
         }
      }

      return NORMAL;
   }
}
