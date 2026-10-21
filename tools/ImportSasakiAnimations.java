import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.awt.AlphaComposite;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Reimports the supplied Blockbench tracks and bakes their parent pivots for PlayerAnimator. */
class ImportSasakiAnimations {
   private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Map<String, String> PARTS = new LinkedHashMap<>();
   private static final Map<String, JsonObject> GROUPS = new LinkedHashMap<>();
   private static final Map<String, String> PARENTS = new LinkedHashMap<>();
   private static final Map<String, String> NAMES = new LinkedHashMap<>();
   private static final Path ASSETS = Path.of("src/main/resources/assets/typemoonworld");

   static {
      PARTS.put("head", "Head");
      PARTS.put("torso", "Body");
      PARTS.put("rightArm", "Right Arm");
      PARTS.put("leftArm", "Left Arm");
      PARTS.put("rightLeg", "Right Leg");
      PARTS.put("leftLeg", "Left Leg");
   }

   public static void main(String[] args) throws Exception {
      Path root = Path.of(args[0]);
      Path clips = root.resolve("小次郎攻击动画");
      JsonObject rig = read(clips.resolve("xiaocilanggongjimoxing.geo.bbmodel"));
      for (JsonElement entry : rig.getAsJsonArray("groups")) {
         JsonObject group = entry.getAsJsonObject();
         String name = group.get("name").getAsString();
         GROUPS.put(name, group);
         NAMES.put(group.get("uuid").getAsString(), name);
      }
      parents(rig.getAsJsonArray("outliner"), null);
      JsonObject sources = new JsonObject();
      Path basic = clips.resolve("小次郎平A攻击");
      Path crouch = clips.resolve("小次郎平A蹲攻击");
      Path stance = clips.resolve("小次郎右键架势攻击");
      importClip(sources, root, "vertical_slash", basic.resolve("平A攻击1下劈/xiaocilanggongjidonghua1.animation.json"), "gongjidonghua1.unknown.new", 17, 15);
      importClip(sources, root, "downward_slash", basic.resolve("平A攻击1下劈/xiaocilanggongjidonghua1.animation.json"), "xiaocilangpingAdonghua1.new", 13, 10);
      importClip(sources, root, "upward_slash", basic.resolve("平A攻击2上挑/xiaocilangpingAdonghua2.animation.json"), null, 12, 10);
      importClip(sources, root, "horizontal_slash", basic.resolve("平A攻击3横斩/xiaocilangpingAgongji3.animation.json"), null, 14, 12);
      importClip(sources, root, "crouch_cutback", crouch.resolve("小次郎平A蹲攻击连贯动画/xiaocilangdungongjilianguandonghua.animation.json"), null, 20, 18);
      importClip(sources, root, "thrust", stance.resolve("小次郎右键平A2/xiaocilangyoujianpingA2.animation.json"), null, 19, 17);
      importClip(sources, root, "spinning_slash", basic.resolve("平A连贯动画/xiaocilangpingAlianguanhebingingdonghua.animation.json"), null, 23, 21);
      importClip(sources, root, "transparency", crouch.resolve("小次郎平A蹲攻击2/xiaocilangdungongji2.animation.json"), null, 18, 16);
      Path guard = stance.resolve("岩流架势/xiaocilangyanliujiashi.animation.json");
      importClip(sources, root, "presence_concealment", guard, null, 10, 8);
      importClip(sources, root, "counter_stance", guard, null, 20, 10);
      importClip(sources, root, "tsubame_windup", guard, null, 10, 10);
      JsonObject archive = new JsonObject();
      archive.addProperty("format_version", "1.8.0");
      archive.add("animations", sources);
      write(ASSETS.resolve("animations/sasaki_kojiro_source.animation.json"), archive);
      var texture = ImageIO.read(root.resolve("燕返/燕返刀光特效/daoguang.png").toFile());
      var stretched = new BufferedImage(512, 96, BufferedImage.TYPE_INT_ARGB);
      var graphics = stretched.createGraphics();
      graphics.setComposite(AlphaComposite.Src);
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      // Use the source model's blade UV rectangle, then stretch the crescent lengthwise.
      graphics.drawImage(texture, 0, 0, 512, 96, 7, 31, 43, 47, null);
      graphics.dispose();
      ImageIO.write(stretched, "png", ASSETS.resolve("textures/particle/sasaki_tsubame_blade_stretched.png").toFile());
   }

   private static void parents(JsonArray children, String parent) {
      for (JsonElement entry : children) {
         if (!entry.isJsonObject()) continue;
         JsonObject node = entry.getAsJsonObject();
         String name = NAMES.get(node.get("uuid").getAsString());
         if (name == null) throw new IllegalArgumentException("Unknown rig group: " + node);
         PARENTS.put(name, parent);
         if (node.has("children")) parents(node.getAsJsonArray("children"), name);
      }
   }

   private static void importClip(JsonObject archive, Path root, String action, Path file,
                                  String clipName, int duration, int motionTicks) throws Exception {
      JsonObject animations = read(file).getAsJsonObject("animations");
      if (clipName == null) {
         if (animations.size() != 1) throw new IllegalArgumentException("Choose a source clip: " + file);
         clipName = animations.keySet().iterator().next();
      }
      JsonObject clip = animations.getAsJsonObject(clipName);
      archive.add(action, clip.deepCopy());
      double seconds = clip.get("animation_length").getAsDouble();
      JsonArray moves = new JsonArray();
      Map<String, Vector3f> previousAngles = new LinkedHashMap<>();
      for (int tick = 0; tick <= duration; tick++) {
         float time = (float)(seconds * Math.min(tick, motionTicks) / motionTicks);
         float fade = tick <= motionTicks ? 1.0F : (duration - tick) / (float)(duration - motionTicks);
         JsonObject move = new JsonObject();
         move.addProperty("tick", tick);
         // Explicit identity prevents locomotion and vanilla head look from leaking into the source motion.
         move.add("body", pose(new Vector3f(), new Vector3f()));
         for (var part : PARTS.entrySet()) {
            String bone = part.getValue();
            Matrix4f matrix = world(clip.getAsJsonObject("bones"), bone, time);
            Vector3f bind = pivot(bone);
            Vector3f position = matrix.transformPosition(new Vector3f(bind));
            Vector3f rotation = matrix.getEulerAnglesZYX(new Vector3f()).mul((float)(180.0 / Math.PI));
            Vector3f previous = previousAngles.get(part.getKey());
            continuous(rotation, previous);
            previousAngles.put(part.getKey(), new Vector3f(rotation));
            recover(rotation, fade);
            position.sub(bind).mul(fade).add(bind);
            move.add(part.getKey(), pose(position, rotation));
         }
         Matrix4f grip = new Matrix4f().translation(pivot("Right Arm"))
            .rotateX(-(float)Math.PI / 2.0F).rotateY((float)Math.PI).translate(1.0F, 2.0F, -10.0F);
         JsonObject bones = clip.getAsJsonObject("bones");
         Matrix4f swordDelta = world(bones, "jian", time).mul(world(new JsonObject(), "jian", 0.0F).invert());
         // Remove the animated arm/grip before applying the independent sword track.
         Matrix4f item = world(bones, "Right Arm", time).mul(new Matrix4f(grip)).invert()
            .mul(swordDelta).mul(grip);
         Vector3f position = item.getTranslation(new Vector3f()).mul(fade);
         Vector3f rotation = item.getEulerAnglesZYX(new Vector3f()).mul((float)(180.0 / Math.PI));
         continuous(rotation, previousAngles.get("rightItem"));
         previousAngles.put("rightItem", new Vector3f(rotation));
         recover(rotation, fade);
         move.add("rightItem", pose(position, rotation));
         move.add("leftItem", pose(new Vector3f(), new Vector3f()));
         moves.add(move);
      }
      JsonObject emote = new JsonObject();
      emote.addProperty("beginTick", 0);
      emote.addProperty("endTick", duration);
      emote.addProperty("stopTick", duration);
      emote.addProperty("degrees", true);
      emote.add("moves", moves);
      JsonObject output = new JsonObject();
      output.addProperty("name", "sasaki_kojiro_" + action);
      output.addProperty("version", 3);
      output.addProperty("description", "Full source tracks; parent rotations and pivots baked for PlayerAnimator.");
      output.addProperty("source_file", root.relativize(file).toString().replace('\\', '/'));
      output.addProperty("source_animation", clipName);
      output.addProperty("source_length", seconds);
      output.addProperty("motion_ticks", motionTicks);
      output.add("emote", emote);
      write(ASSETS.resolve("player_animations/sasaki_kojiro_" + action + ".json"), output);
      System.out.println(action + ": " + clipName + " -> " + moves.size() + " frames, " + duration + " ticks");
   }

   private static Matrix4f world(JsonObject bones, String bone, float time) {
      JsonObject data = bones.has(bone) ? bones.getAsJsonObject(bone) : new JsonObject();
      Vector3f origin = pivot(bone);
      Vector3f rotation = vector(GROUPS.get(bone).get("rotation"));
      rotation.add(sample(data.get("rotation"), time)).mul((float)(Math.PI / 180.0));
      Vector3f offset = sample(data.get("position"), time);
      offset.y = -offset.y;
      Matrix4f local = new Matrix4f().translation(new Vector3f(origin).add(offset))
         .rotateZYX(rotation.z, rotation.y, rotation.x).translate(new Vector3f(origin).negate());
      String parent = PARENTS.get(bone);
      return parent == null ? local : world(bones, parent, time).mul(local);
   }

   private static Vector3f pivot(String bone) {
      Vector3f p = vector(GROUPS.get(bone).get("origin"));
      return new Vector3f(-p.x, 24.0F - p.y, p.z);
   }

   private static JsonObject pose(Vector3f position, Vector3f rotation) {
      JsonObject result = new JsonObject();
      result.addProperty("x", clean(position.x));
      result.addProperty("y", clean(position.y));
      result.addProperty("z", clean(position.z));
      result.addProperty("pitch", clean(rotation.x));
      result.addProperty("yaw", clean(rotation.y));
      result.addProperty("roll", clean(rotation.z));
      return result;
   }

   private static float clean(float value) { return Math.round(value * 10000.0F) / 10000.0F; }
   private static void continuous(Vector3f rotation, Vector3f previous) {
      if (previous == null) return;
      Vector3f alternate = new Vector3f(rotation.x + 180.0F, 180.0F - rotation.y, rotation.z + 180.0F);
      for (Vector3f candidate : new Vector3f[]{rotation, alternate}) {
         candidate.x = unwrap(candidate.x, previous.x);
         candidate.y = unwrap(candidate.y, previous.y);
         candidate.z = unwrap(candidate.z, previous.z);
      }
      if (alternate.distanceSquared(previous) < rotation.distanceSquared(previous)) rotation.set(alternate);
   }

   private static void recover(Vector3f rotation, float fade) {
      rotation.x = unwrap(0.0F, rotation.x) * (1.0F - fade) + rotation.x * fade;
      rotation.y = unwrap(0.0F, rotation.y) * (1.0F - fade) + rotation.y * fade;
      rotation.z = unwrap(0.0F, rotation.z) * (1.0F - fade) + rotation.z * fade;
   }
   private static float unwrap(float value, float previous) {
      while (value - previous > 180.0F) value -= 360.0F;
      while (value - previous < -180.0F) value += 360.0F;
      return value;
   }

   private static Vector3f sample(JsonElement channel, float time) {
      if (channel == null) return new Vector3f();
      if (!channel.isJsonObject()) return vector(channel);
      JsonObject keys = channel.getAsJsonObject();
      if (keys.has("vector") || keys.has("post")) return vector(keys);
      float before = -Float.MAX_VALUE, after = Float.MAX_VALUE;
      JsonElement left = null, right = null;
      for (var entry : keys.entrySet()) {
         float key = Float.parseFloat(entry.getKey());
         if (key <= time && key > before) { before = key; left = entry.getValue(); }
         if (key >= time && key < after) { after = key; right = entry.getValue(); }
      }
      if (left == null) return vector(right);
      if (right == null || after == before) return vector(left);
      return vector(left).lerp(vector(right), (time - before) / (after - before));
   }

   private static Vector3f vector(JsonElement value) {
      if (value == null) return new Vector3f();
      if (value.isJsonPrimitive()) return new Vector3f(value.getAsFloat());
      if (value.isJsonObject()) {
         JsonObject key = value.getAsJsonObject();
         return vector(key.has("post") ? key.get("post") : key.get("vector"));
      }
      JsonArray a = value.getAsJsonArray();
      return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
   }

   private static JsonObject read(Path file) throws Exception {
      try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      }
   }

   private static void write(Path file, JsonObject data) throws Exception {
      Files.createDirectories(file.getParent());
      Files.writeString(file, JSON.toJson(data) + "\n", StandardCharsets.UTF_8);
   }
}
