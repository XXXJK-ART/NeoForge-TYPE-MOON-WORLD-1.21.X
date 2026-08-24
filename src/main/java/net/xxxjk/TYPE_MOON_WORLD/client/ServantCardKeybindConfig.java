package net.xxxjk.TYPE_MOON_WORLD.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import org.lwjgl.glfw.GLFW;

/** Client-local direct skill bindings, stored separately for each servant card. */
public final class ServantCardKeybindConfig {
   public static final int SLOT_COUNT = 10;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Map<String, Profile> PROFILES = new HashMap<>();

   private ServantCardKeybindConfig() {
   }

   public static Key keyFor(String servantId, int slot) {
      if (!validSlot(slot)) {
         return InputConstants.UNKNOWN;
      }
      Profile profile = profile(servantId);
      return profile.custom ? profile.slotKeys[slot] : TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_KEYS[slot].getKey();
   }

   public static void setKey(String servantId, int slot, Key key) {
      if (!validSlot(slot)) {
         return;
      }
      if (key == null) {
         key = InputConstants.UNKNOWN;
      }
      if (!key.equals(InputConstants.UNKNOWN) && isReservedKey(key)) {
         return;
      }
      Profile profile = profile(servantId);
      profile.custom = true;
      if (!key.equals(InputConstants.UNKNOWN)) {
         for (int other = 0; other < SLOT_COUNT; other++) {
            if (other != slot && profile.slotKeys[other].equals(key)) {
               profile.slotKeys[other] = InputConstants.UNKNOWN;
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
      String id = normalizeId(servantId);
      PROFILES.remove(id);
      try {
         Files.deleteIfExists(profilePath(servantId));
      } catch (IOException ignored) {
      }
   }

   public static boolean isKeyDown(long window, Key key) {
      if (key == null || key.equals(InputConstants.UNKNOWN)) {
         return false;
      }
      if (key.getType() == Type.MOUSE) {
         return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
      }
      if (key.getType() == Type.SCANCODE) {
         return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
      }
      return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
   }

   public static boolean isReservedKey(Key key) {
      if (key == null || key.getType() != Type.KEYSYM) {
         return false;
      }
      return key.getValue() == GLFW.GLFW_KEY_R
         || key.getValue() == GLFW.GLFW_KEY_O
         || key.getValue() == GLFW.GLFW_KEY_LEFT_ALT
         || key.getValue() == GLFW.GLFW_KEY_RIGHT_ALT;
   }

   public static String keyName(Key key) {
      if (key == null || key.equals(InputConstants.UNKNOWN)) {
         return "-";
      }
      return key.getDisplayName().getString();
   }

   private static Profile profile(String servantId) {
      String id = normalizeId(servantId);
      return PROFILES.computeIfAbsent(id, ignored -> load(servantId));
   }

   private static Profile load(String servantId) {
      Path path = profilePath(servantId);
      Profile profile = defaultProfile(false);
      if (!Files.isRegularFile(path)) {
         return profile;
      }
      try {
         JsonElement root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
         if (!root.isJsonObject()) {
            return profile;
         }
         JsonObject json = root.getAsJsonObject();
         JsonArray rawKeys = json.getAsJsonArray("slot_keys");
         boolean legacyDefaults = readKeys(rawKeys, profile.slotKeys);
         readBooleans(json.getAsJsonArray("hold_slots"), profile.holdSlots);
         for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (isReservedKey(profile.slotKeys[slot])) {
               profile.slotKeys[slot] = InputConstants.UNKNOWN;
            }
         }
         profile.custom = !legacyDefaults;
      } catch (IOException | RuntimeException ignored) {
      }
      return profile;
   }

   private static boolean readKeys(JsonArray values, Key[] destination) {
      if (values == null) return false;
      boolean legacy = true;
      for (int i = 0; i < Math.min(values.size(), destination.length); i++) {
         try {
            JsonElement value = values.get(i);
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
               int keyCode = value.getAsInt();
               destination[i] = Type.KEYSYM.getOrCreate(keyCode);
               legacy &= keyCode == GLFW.GLFW_KEY_KP_0 + i;
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
               destination[i] = InputConstants.getKey(value.getAsString());
               legacy = false;
            } else if (value.isJsonObject()) {
               JsonObject object = value.getAsJsonObject();
               String type = object.has("type") ? object.get("type").getAsString() : "keyboard";
               int code = object.has("value") ? object.get("value").getAsInt() : -1;
               destination[i] = keyFromType(type, code);
               legacy = false;
            }
         } catch (RuntimeException ignored) {
            destination[i] = InputConstants.UNKNOWN;
            legacy = false;
         }
      }
      return legacy;
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

   private static Profile defaultProfile(boolean custom) {
      Key[] slotKeys = new Key[SLOT_COUNT];
      for (int slot = 0; slot < SLOT_COUNT; slot++) {
         slotKeys[slot] = TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_KEYS[slot].getKey();
      }
      return new Profile(slotKeys, new boolean[SLOT_COUNT], custom);
   }

   private static void save(String servantId, Profile profile) {
      JsonObject json = new JsonObject();
      json.addProperty("servant_id", normalizeId(servantId));
      JsonArray slotKeys = new JsonArray();
      for (Key key : profile.slotKeys) {
         slotKeys.add(key.getName());
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

   private static Key keyFromType(String type, int value) {
      if ("mouse".equalsIgnoreCase(type)) return Type.MOUSE.getOrCreate(value);
      if ("scancode".equalsIgnoreCase(type)) return Type.SCANCODE.getOrCreate(value);
      return Type.KEYSYM.getOrCreate(value);
   }

   private static final class Profile {
      private final Key[] slotKeys;
      private final boolean[] holdSlots;
      private boolean custom;

      private Profile(Key[] slotKeys, boolean[] holdSlots, boolean custom) {
         this.slotKeys = Arrays.copyOf(slotKeys, SLOT_COUNT);
         this.holdSlots = Arrays.copyOf(holdSlots, SLOT_COUNT);
         this.custom = custom;
      }
   }
}
