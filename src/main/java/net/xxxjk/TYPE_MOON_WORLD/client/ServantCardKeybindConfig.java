package net.xxxjk.TYPE_MOON_WORLD.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

/** Client-local direct skill bindings, stored separately for each servant card. */
public final class ServantCardKeybindConfig {
   public static final int SLOT_COUNT = 10;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Map<String, Profile> PROFILES = new HashMap<>();

   private ServantCardKeybindConfig() {
   }

   public static int keyFor(String servantId, int slot) {
      return profile(servantId).slotKeys[validSlot(slot) ? slot : 0];
   }

   public static void setKey(String servantId, int slot, int key) {
      if (!validSlot(slot)) {
         return;
      }
      if (key != GLFW.GLFW_KEY_UNKNOWN && isReservedKey(key)) {
         return;
      }
      Profile profile = profile(servantId);
      if (key != GLFW.GLFW_KEY_UNKNOWN) {
         for (int other = 0; other < SLOT_COUNT; other++) {
            if (other != slot && profile.slotKeys[other] == key) {
               profile.slotKeys[other] = GLFW.GLFW_KEY_UNKNOWN;
            }
         }
      }
      profile.slotKeys[slot] = key;
      save(servantId, profile);
   }

   public static boolean isHoldSlot(String servantId, int slot) {
      return validSlot(slot) && profile(servantId).holdSlots[slot];
   }

   public static void setHoldSlot(String servantId, int slot, boolean hold) {
      if (!validSlot(slot)) {
         return;
      }
      Profile profile = profile(servantId);
      profile.holdSlots[slot] = hold;
      save(servantId, profile);
   }

   public static void reset(String servantId) {
      Profile profile = defaultProfile();
      PROFILES.put(normalizeId(servantId), profile);
      save(servantId, profile);
   }

   public static boolean isKeyDown(long window, int key) {
      return key != GLFW.GLFW_KEY_UNKNOWN && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
   }

   public static boolean isReservedKey(int key) {
      return key == GLFW.GLFW_KEY_R
         || key == GLFW.GLFW_KEY_O
         || key == GLFW.GLFW_KEY_LEFT_ALT
         || key == GLFW.GLFW_KEY_RIGHT_ALT;
   }

   public static String keyName(int key) {
      if (key == GLFW.GLFW_KEY_UNKNOWN) {
         return "-";
      }
      return InputConstants.getKey(key, GLFW.GLFW_KEY_UNKNOWN).getDisplayName().getString();
   }

   private static Profile profile(String servantId) {
      String id = normalizeId(servantId);
      return PROFILES.computeIfAbsent(id, ignored -> load(servantId));
   }

   private static Profile load(String servantId) {
      Path path = profilePath(servantId);
      Profile profile = defaultProfile();
      if (!Files.isRegularFile(path)) {
         save(servantId, profile);
         return profile;
      }
      try {
         JsonElement root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
         if (!root.isJsonObject()) {
            return profile;
         }
         JsonObject json = root.getAsJsonObject();
         readInts(json.getAsJsonArray("slot_keys"), profile.slotKeys);
         readBooleans(json.getAsJsonArray("hold_slots"), profile.holdSlots);
         for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (isReservedKey(profile.slotKeys[slot])) {
               profile.slotKeys[slot] = GLFW.GLFW_KEY_UNKNOWN;
            }
         }
      } catch (IOException | RuntimeException ignored) {
      }
      return profile;
   }

   private static void readInts(JsonArray values, int[] destination) {
      if (values == null) {
         return;
      }
      for (int i = 0; i < Math.min(values.size(), destination.length); i++) {
         try {
            destination[i] = values.get(i).getAsInt();
         } catch (RuntimeException ignored) {
         }
      }
   }

   private static void readBooleans(JsonArray values, boolean[] destination) {
      if (values == null) {
         return;
      }
      for (int i = 0; i < Math.min(values.size(), destination.length); i++) {
         try {
            destination[i] = values.get(i).getAsBoolean();
         } catch (RuntimeException ignored) {
         }
      }
   }

   private static Profile defaultProfile() {
      int[] slotKeys = new int[SLOT_COUNT];
      for (int slot = 0; slot < SLOT_COUNT; slot++) {
         slotKeys[slot] = GLFW.GLFW_KEY_KP_0 + slot;
      }
      return new Profile(slotKeys, new boolean[SLOT_COUNT]);
   }

   private static void save(String servantId, Profile profile) {
      JsonObject json = new JsonObject();
      json.addProperty("servant_id", normalizeId(servantId));
      JsonArray slotKeys = new JsonArray();
      for (int key : profile.slotKeys) {
         slotKeys.add(key);
      }
      json.add("slot_keys", slotKeys);
      JsonArray holdSlots = new JsonArray();
      for (boolean hold : profile.holdSlots) {
         holdSlots.add(hold);
      }
      json.add("hold_slots", holdSlots);
      try {
         Path path = profilePath(servantId);
         Files.createDirectories(path.getParent());
         Files.writeString(path, GSON.toJson(json), StandardCharsets.UTF_8);
      } catch (IOException ignored) {
      }
   }

   private static Path profilePath(String servantId) {
      return FMLPaths.CONFIGDIR.get().resolve("typemoonworld").resolve("servant_card_keybinds").resolve(normalizeId(servantId) + ".json");
   }

   private static String normalizeId(String servantId) {
      String normalized = servantId == null ? "" : servantId.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
      return normalized.isBlank() ? "unknown" : normalized;
   }

   private static boolean validSlot(int slot) {
      return slot >= 0 && slot < SLOT_COUNT;
   }

   private static final class Profile {
      private final int[] slotKeys;
      private final boolean[] holdSlots;

      private Profile(int[] slotKeys, boolean[] holdSlots) {
         this.slotKeys = Arrays.copyOf(slotKeys, SLOT_COUNT);
         this.holdSlots = Arrays.copyOf(holdSlots, SLOT_COUNT);
      }
   }
}
