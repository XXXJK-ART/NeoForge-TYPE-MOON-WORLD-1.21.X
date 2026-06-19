package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.vfx.Easing;
import net.xxxjk.TYPE_MOON_WORLD.vfx.IVFXComponent;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXBlendMode;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.BicircleStarCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.CircleCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.ConvexPolygonCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.EllipseCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.FractalTreeCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.FractalLightningCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.HelixCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.LineCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.ParabolaCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.ParametricCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.SchlafliStarCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.curve.TCBSplineCurve;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.field.ScalarNoiseField;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry.ConvexPolyhedronGeometry;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry.CuboidGeometry;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry.EllipsoidGeometry;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.geometry.SphereGeometry;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface.BicircleStarTorusSurface;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface.EllipticTorusSurface;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface.ParametricSurface;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface.ImageMaskSurface;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface.PolygonFaceRingSurface;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface.RuledSurface;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface.SpriteSheetSurface;
import net.xxxjk.TYPE_MOON_WORLD.vfx.component.surface.TorusSurface;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.ColorKeyFrame;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.SizeKeyFrame;
import net.xxxjk.TYPE_MOON_WORLD.vfx.keyframe.TransformKeyFrame;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EffectLibrary extends SimpleJsonResourceReloadListener {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public static final EffectLibrary INSTANCE = new EffectLibrary();
   private final Map<ResourceLocation, VFXEffectDefinition> effects = new HashMap<>();

   private EffectLibrary() {
      super(GSON, "effects");
   }

   public VFXEffectDefinition get(String id) {
      ResourceLocation resourceLocation = id.contains(":")
         ? ResourceLocation.parse(id)
         : ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, id);
      return this.effects.get(resourceLocation);
   }

   public boolean reloadNow() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft == null) {
         return false;
      }
      this.loadFromManager(minecraft.getResourceManager());
      return true;
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
      this.effects.clear();
      for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
         try {
            this.effects.put(entry.getKey(), parseEffect(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(), "effect")));
         } catch (Exception ex) {
            TYPE_MOON_WORLD.LOGGER.error("Invalid VFX effect JSON {}: {}", entry.getKey(), ex.getMessage());
         }
      }
      TYPE_MOON_WORLD.LOGGER.info("Loaded {} VFX effect definitions", this.effects.size());
   }

   private void loadFromManager(ResourceManager resourceManager) {
      this.effects.clear();
      Map<ResourceLocation, JsonElement> loaded = new HashMap<>();
      resourceManager.listResources("effects", location -> location.getPath().endsWith(".json"))
         .forEach((location, resource) -> {
            try (var reader = resource.openAsReader()) {
               String path = location.getPath();
               ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), path.substring("effects/".length(), path.length() - ".json".length()));
               loaded.put(id, GsonHelper.fromJson(GSON, reader, JsonElement.class));
            } catch (Exception ex) {
               TYPE_MOON_WORLD.LOGGER.error("Failed to read VFX effect {}", location, ex);
            }
         });
      apply(loaded, resourceManager, null);
   }

   private static VFXEffectDefinition parseEffect(ResourceLocation id, JsonObject json) {
      requireOnly(json, "duration", "emitters", "environment", "notes");
      float duration = requiredFloat(json, "duration");
      JsonArray emittersJson = requiredArray(json, "emitters");
      if (emittersJson.isEmpty()) {
         throw new JsonParseException(id + " must contain at least one emitter");
      }
      List<VFXEffectDefinition.EmitterDefinition> emitters = new ArrayList<>();
      for (JsonElement element : emittersJson) {
         emitters.add(parseEmitter(GsonHelper.convertToJsonObject(element, "emitter")));
      }
      return new VFXEffectDefinition(duration, emitters, parseEnvironment(optionalArray(json, "environment"), duration));
   }

   private static VFXEffectDefinition.EmitterDefinition parseEmitter(JsonObject json) {
      requireOnly(
         json,
         "rate",
         "particle_lifetime",
         "size_variance",
         "lifetime_variance",
         "position_variance",
         "velocity",
         "velocity_variance",
         "start_time",
         "end_time",
         "max_vanilla_particles_per_tick",
         "binding",
         "component",
         "transform_over_life",
         "color_over_life",
         "size_over_life",
         "visibility_condition",
         "blend",
         "particle_role",
         "vanilla_particles",
         "enable_trail",
         "onStart",
         "onTick",
         "onEnd",
         "onComplete"
      );
      float rate = requiredFloat(json, "rate");
      float particleLifetime = requiredFloat(json, "particle_lifetime");
      float sizeVariance = GsonHelper.getAsFloat(json, "size_variance", 0.0F);
      float lifetimeVariance = GsonHelper.getAsFloat(json, "lifetime_variance", 0.0F);
      float positionVariance = GsonHelper.getAsFloat(json, "position_variance", 0.0F);
      Vector3f velocity = vec(json, "velocity", new Vector3f());
      float velocityVariance = GsonHelper.getAsFloat(json, "velocity_variance", 0.0F);
      float startTime = GsonHelper.getAsFloat(json, "start_time", 0.0F);
      float endTime = GsonHelper.getAsFloat(json, "end_time", 0.0F);
      int maxVanillaParticleSpawnsPerTick = GsonHelper.getAsInt(json, "max_vanilla_particles_per_tick", 12);
      VFXEffectDefinition.BindingDefinition binding = parseBinding(json.has("binding") ? requiredObject(json, "binding") : null);
      IVFXComponent component = parseComponent(requiredObject(json, "component"));
      List<TransformKeyFrame> transforms = parseTransformKeyFrames(optionalArray(json, "transform_over_life"));
      List<ColorKeyFrame> colors = parseColorKeyFrames(optionalArray(json, "color_over_life"));
      List<SizeKeyFrame> sizes = parseSizeKeyFrames(optionalArray(json, "size_over_life"));
      String condition = GsonHelper.getAsString(json, "visibility_condition", "");
      VFXBlendMode blend = VFXBlendMode.valueOf(GsonHelper.getAsString(json, "blend", "ADDITIVE").toUpperCase(Locale.ROOT));
      String particleRole = GsonHelper.getAsString(json, "particle_role", "");
      List<VFXVanillaParticleDefinition> vanillaParticles = parseVanillaParticles(optionalArray(json, "vanilla_particles"));
      boolean enableTrail = GsonHelper.getAsBoolean(json, "enable_trail", false);
      List<String> onEnd = parseStringList(optionalArray(json, "onEnd"));
      String onComplete = GsonHelper.getAsString(json, "onComplete", "");
      if (!onComplete.isBlank()) {
         onEnd.add(onComplete);
      }
      return new VFXEffectDefinition.EmitterDefinition(
         rate,
         particleLifetime,
         sizeVariance,
         lifetimeVariance,
         positionVariance,
         velocity,
         velocityVariance,
         startTime,
         endTime,
         maxVanillaParticleSpawnsPerTick,
         binding,
         component,
         transforms,
         colors,
         sizes,
         condition,
         blend,
         particleRole,
         vanillaParticles,
         enableTrail,
         parseStringList(optionalArray(json, "onStart")),
         parseStringList(optionalArray(json, "onTick")),
         onEnd
      );
   }

   private static VFXEffectDefinition.BindingDefinition parseBinding(JsonObject json) {
      if (json == null) {
         return VFXEffectDefinition.BindingDefinition.NONE;
      }
      requireOnly(json, "mode", "bone", "offset", "rotate_with_entity");
      return new VFXEffectDefinition.BindingDefinition(
         GsonHelper.getAsString(json, "mode", ""),
         GsonHelper.getAsString(json, "bone", ""),
         vec(json, "offset", new Vector3f()),
         GsonHelper.getAsBoolean(json, "rotate_with_entity", false)
      );
   }

   private static List<VFXVanillaParticleDefinition> parseVanillaParticles(JsonArray array) {
      List<VFXVanillaParticleDefinition> result = new ArrayList<>();
      if (array == null) {
         return result;
      }
      for (JsonElement element : array) {
         JsonObject json = GsonHelper.convertToJsonObject(element, "vanilla particle");
         requireOnly(json, "type", "count", "spread", "offset", "speed", "chance");
         result.add(
            new VFXVanillaParticleDefinition(
               VFXVanillaParticleDefinition.particleByName(GsonHelper.getAsString(json, "type", "cloud")),
               GsonHelper.getAsInt(json, "count", 1),
               vec(json, "spread", new Vector3f()),
               vec(json, "offset", new Vector3f()),
               GsonHelper.getAsFloat(json, "speed", 0.0F),
               GsonHelper.getAsFloat(json, "chance", 1.0F)
            )
         );
      }
      return result;
   }

   private static List<VFXEnvironmentDefinition> parseEnvironment(JsonArray array, float effectDuration) {
      List<VFXEnvironmentDefinition> result = new ArrayList<>();
      if (array == null) {
         return result;
      }
      for (JsonElement element : array) {
         JsonObject json = GsonHelper.convertToJsonObject(element, "environment");
         requireOnly(json, "type", "color", "start", "end", "fade_in", "fade_out", "intensity");
         String type = GsonHelper.getAsString(json, "type", "screen_tint");
         float start = Mth.clamp(GsonHelper.getAsFloat(json, "start", 0.0F), 0.0F, effectDuration);
         float end = Mth.clamp(GsonHelper.getAsFloat(json, "end", effectDuration), start, effectDuration);
         result.add(
            new VFXEnvironmentDefinition(
               type,
               parseColor(GsonHelper.getAsString(json, "color", "#00000000")),
               start,
               end,
               Math.max(0.0F, GsonHelper.getAsFloat(json, "fade_in", 0.15F)),
               Math.max(0.0F, GsonHelper.getAsFloat(json, "fade_out", 0.25F)),
               Mth.clamp(GsonHelper.getAsFloat(json, "intensity", 1.0F), 0.0F, 1.0F)
            )
         );
      }
      return result;
   }

   private static IVFXComponent parseComponent(JsonObject json) {
      String type = requiredString(json, "type").toLowerCase(Locale.ROOT);
      validateComponentFields(type, json);
      return switch (type) {
         case "line" -> new LineCurve(vec(json, "start", new Vector3f()), vec(json, "end", new Vector3f(0.0F, 1.0F, 0.0F)), resolution(json, 16));
         case "parabola" -> new ParabolaCurve(floatValue(json, "width", 2.0F), floatValue(json, "height", 1.0F), resolution(json, 32));
         case "helix" -> new HelixCurve(floatValue(json, "radius", 0.5F), floatValue(json, "turns", 3.0F), floatValue(json, "height", 2.0F), resolution(json, 64));
         case "convex_polygon" -> new ConvexPolygonCurve(floatValue(json, "radius", 1.0F), intValue(json, "sides", 6), resolution(json, 48));
         case "fractal_tree" -> new FractalTreeCurve(floatValue(json, "length", 1.0F), floatValue(json, "angle", 0.55F), intValue(json, "depth", 4));
         case "fractal_lightning" -> new FractalLightningCurve(
            vec(json, "start", new Vector3f()),
            vec(json, "end", new Vector3f(0.0F, 1.0F, 0.0F)),
            intValue(json, "iterations", 6),
            floatValue(json, "roughness", 0.5F),
            floatValue(json, "period", 5.0F),
            floatValue(json, "density", 8.0F),
            GsonHelper.getAsBoolean(json, "randomize_start", false),
            GsonHelper.getAsBoolean(json, "randomize_end", false),
            floatValue(json, "random_radius_min", 0.0F),
            floatValue(json, "random_radius_max", 0.0F),
            floatValue(json, "random_height_min", 0.0F),
            floatValue(json, "random_height_max", 0.0F),
            floatValue(json, "seed1", 0.0F),
            floatValue(json, "seed2", 0.0F),
            floatValue(json, "seed3", 0.0F),
            floatValue(json, "seed4", 0.0F),
            floatValue(json, "seed5", 0.0F),
            resolution(json, 128)
         );
         case "tcb_spline" -> new TCBSplineCurve(vecArray(requiredArray(json, "points")), floatValue(json, "tension", 0.0F), floatValue(json, "continuity", 0.0F), floatValue(json, "bias", 0.0F), resolution(json, 64));
         case "parametric", "parametric_curve" -> new ParametricCurve(
            floatValue(json, "amplitude", 1.0F),
            floatValue(json, "frequency", 2.0F),
            floatValue(json, "zigzag_frequency", 0.0F),
            floatValue(json, "zigzag_amplitude", 0.0F),
            intValue(json, "min_nodes", 0),
            intValue(json, "max_nodes", 0),
            floatValue(json, "jitter", 0.0F),
            ParametricCurve.Mode.valueOf(GsonHelper.getAsString(json, "mode", "DEFAULT").toUpperCase(Locale.ROOT)),
            stringValue(json, "expr_x", null),
            stringValue(json, "expr_y", null),
            stringValue(json, "expr_z", null),
            floatValue(json, "t_start", 0.0F),
            floatValue(json, "t_end", 1.0F),
            resolution(json, 64)
         );
         case "bicircle_star", "double_circle_star" -> new BicircleStarCurve(floatValue(json, "radius", 1.0F), floatValue(json, "inner_radius", 0.35F), resolution(json, 64));
         case "schlafli_star" -> new SchlafliStarCurve(floatValue(json, "radius", 1.0F), intValue(json, "points", 5), intValue(json, "step", 2), resolution(json, 64));
         case "circle" -> new CircleCurve(floatValue(json, "radius", 1.0F), resolution(json, 48));
         case "ellipse" -> new EllipseCurve(floatValue(json, "radius_x", 1.0F), floatValue(json, "radius_z", 0.5F), resolution(json, 48));
         case "torus" -> new TorusSurface(floatValue(json, "radius", 1.0F), floatValue(json, "tube_radius", 0.2F), intValue(json, "u_segments", 32), intValue(json, "v_segments", 12));
         case "elliptic_torus" -> new EllipticTorusSurface(floatValue(json, "radius_x", 1.0F), floatValue(json, "radius_z", 0.6F), floatValue(json, "tube_radius", 0.2F), intValue(json, "u_segments", 32), intValue(json, "v_segments", 12));
         case "polygon_face_ring" -> new PolygonFaceRingSurface(floatValue(json, "outer_radius", 1.0F), floatValue(json, "inner_radius", 0.5F), intValue(json, "sides", 6), intValue(json, "u_segments", 36), intValue(json, "v_segments", 4));
         case "ruled_surface" -> new RuledSurface(vec(json, "start_a", new Vector3f(-1.0F, 0.0F, -1.0F)), vec(json, "end_a", new Vector3f(1.0F, 0.0F, -1.0F)), vec(json, "start_b", new Vector3f(-1.0F, 1.0F, 1.0F)), vec(json, "end_b", new Vector3f(1.0F, 1.0F, 1.0F)), intValue(json, "u_segments", 16), intValue(json, "v_segments", 8));
         case "bicircle_star_torus" -> new BicircleStarTorusSurface(floatValue(json, "radius", 1.0F), floatValue(json, "inner_radius", 0.35F), floatValue(json, "tube_radius", 0.2F), intValue(json, "u_segments", 32), intValue(json, "v_segments", 12));
         case "parametric_surface" -> new ParametricSurface(floatValue(json, "amplitude", 1.0F), floatValue(json, "frequency", 2.0F), intValue(json, "u_segments", 24), intValue(json, "v_segments", 24));
         case "image", "image_mask" -> new ImageMaskSurface(
            GsonHelper.getAsString(json, "texture", "typemoonworld:textures/particle/ea_red2.png"),
            floatValue(json, "width", 4.0F),
            floatValue(json, "height", 6.0F),
            intValue(json, "u_segments", 32),
            intValue(json, "v_segments", 48),
            floatValue(json, "threshold", 0.4F),
            ImageMaskSurface.Reveal.valueOf(GsonHelper.getAsString(json, "reveal", "NONE").toUpperCase(Locale.ROOT))
         );
         case "spritesheet" -> new SpriteSheetSurface(intValue(json, "frames", 1), floatValue(json, "frame_rate", 12.0F), floatValue(json, "width", 1.0F), floatValue(json, "height", 1.0F), intValue(json, "u_segments", 2), intValue(json, "v_segments", 2));
         case "cuboid" -> new CuboidGeometry(floatValue(json, "width", 1.0F), floatValue(json, "height", 1.0F), floatValue(json, "depth", 1.0F), CuboidGeometry.Mode.valueOf(GsonHelper.getAsString(json, "mode", "EDGES").toUpperCase(Locale.ROOT)), resolution(json, 64));
         case "sphere" -> new SphereGeometry(floatValue(json, "radius", 1.0F), resolution(json, 96));
         case "ellipsoid" -> new EllipsoidGeometry(floatValue(json, "radius_x", 1.0F), floatValue(json, "radius_y", 0.7F), floatValue(json, "radius_z", 0.5F), resolution(json, 96));
         case "convex_polyhedron" -> new ConvexPolyhedronGeometry(vecArray(requiredArray(json, "vertices")), resolution(json, 96));
         case "scalar_field" -> new ScalarNoiseField(floatValue(json, "scale", 1.0F), floatValue(json, "amplitude", 1.0F), resolution(json, 64));
         default -> throw new JsonParseException("Unknown VFX component type: " + type);
      };
   }

   private static void validateComponentFields(String type, JsonObject json) {
      switch (type) {
         case "line" -> requireOnly(json, "type", "start", "end", "segments", "sample_count", "detail", "density");
         case "parabola" -> requireOnly(json, "type", "width", "height", "segments", "sample_count", "detail", "density");
         case "helix" -> requireOnly(json, "type", "radius", "turns", "height", "segments", "sample_count", "detail", "density");
         case "convex_polygon" -> requireOnly(json, "type", "radius", "sides", "segments", "sample_count", "detail", "density");
         case "fractal_tree" -> requireOnly(json, "type", "length", "angle", "depth");
         case "fractal_lightning" -> requireOnly(json, "type", "start", "end", "iterations", "roughness", "period", "density", "randomize_start", "randomize_end", "random_radius_min", "random_radius_max", "random_height_min", "random_height_max", "seed1", "seed2", "seed3", "seed4", "seed5", "segments", "sample_count", "detail");
         case "tcb_spline" -> requireOnly(json, "type", "points", "tension", "continuity", "bias", "segments", "sample_count", "detail", "density");
         case "parametric", "parametric_curve" -> requireOnly(json, "type", "amplitude", "frequency", "zigzag_frequency", "zigzag_amplitude", "min_nodes", "max_nodes", "jitter", "mode", "expr_x", "expr_y", "expr_z", "t_start", "t_end", "segments", "sample_count", "detail", "density");
         case "bicircle_star", "double_circle_star" -> requireOnly(json, "type", "radius", "inner_radius", "segments", "sample_count", "detail", "density");
         case "schlafli_star" -> requireOnly(json, "type", "radius", "points", "step", "segments", "sample_count", "detail", "density");
         case "circle" -> requireOnly(json, "type", "radius", "segments", "sample_count", "detail", "density");
         case "ellipse" -> requireOnly(json, "type", "radius_x", "radius_z", "segments", "sample_count", "detail", "density");
         case "torus" -> requireOnly(json, "type", "radius", "tube_radius", "u_segments", "v_segments");
         case "elliptic_torus" -> requireOnly(json, "type", "radius_x", "radius_z", "tube_radius", "u_segments", "v_segments");
         case "polygon_face_ring" -> requireOnly(json, "type", "outer_radius", "inner_radius", "sides", "u_segments", "v_segments");
         case "ruled_surface" -> requireOnly(json, "type", "start_a", "end_a", "start_b", "end_b", "u_segments", "v_segments");
         case "bicircle_star_torus" -> requireOnly(json, "type", "radius", "inner_radius", "tube_radius", "u_segments", "v_segments");
         case "parametric_surface" -> requireOnly(json, "type", "amplitude", "frequency", "u_segments", "v_segments");
         case "image", "image_mask" -> requireOnly(json, "type", "texture", "width", "height", "u_segments", "v_segments", "threshold", "reveal");
         case "spritesheet" -> requireOnly(json, "type", "frames", "frame_rate", "width", "height", "u_segments", "v_segments");
         case "cuboid" -> requireOnly(json, "type", "width", "height", "depth", "mode", "sample_count", "segments", "detail", "density");
         case "sphere" -> requireOnly(json, "type", "radius", "sample_count", "segments", "detail", "density");
         case "ellipsoid" -> requireOnly(json, "type", "radius_x", "radius_y", "radius_z", "sample_count", "segments", "detail", "density");
         case "convex_polyhedron" -> requireOnly(json, "type", "vertices", "sample_count", "segments", "detail", "density");
         case "scalar_field" -> requireOnly(json, "type", "scale", "amplitude", "sample_count", "segments", "detail", "density");
         default -> {
         }
      }
   }

   private static List<TransformKeyFrame> parseTransformKeyFrames(JsonArray array) {
      List<TransformKeyFrame> result = new ArrayList<>();
      if (array != null) {
         for (JsonElement element : array) {
            JsonObject json = GsonHelper.convertToJsonObject(element, "transform keyframe");
            result.add(
               new TransformKeyFrame(
                  requiredFloat(json, "t"),
                  vec(json, "pos", new Vector3f()),
                  quat(json, "rot", new Quaternionf()),
                  vec(json, "scale", new Vector3f(1.0F)),
                  Easing.byName(GsonHelper.getAsString(json, "easing", "linear"))
               )
            );
         }
      }
      if (result.isEmpty()) {
         result.add(new TransformKeyFrame(0.0F, new Vector3f(), new Quaternionf(), new Vector3f(1.0F), Easing.LINEAR));
      }
      return result;
   }

   private static List<ColorKeyFrame> parseColorKeyFrames(JsonArray array) {
      List<ColorKeyFrame> result = new ArrayList<>();
      if (array != null) {
         for (JsonElement element : array) {
            JsonObject json = GsonHelper.convertToJsonObject(element, "color keyframe");
            result.add(new ColorKeyFrame(requiredFloat(json, "t"), parseColor(requiredString(json, "color")), Easing.byName(GsonHelper.getAsString(json, "easing", "linear"))));
         }
      }
      if (result.isEmpty()) {
         result.add(new ColorKeyFrame(0.0F, 0xFFFFFFFF, Easing.LINEAR));
      }
      return result;
   }

   private static List<SizeKeyFrame> parseSizeKeyFrames(JsonArray array) {
      List<SizeKeyFrame> result = new ArrayList<>();
      if (array != null) {
         for (JsonElement element : array) {
            JsonObject json = GsonHelper.convertToJsonObject(element, "size keyframe");
            result.add(new SizeKeyFrame(requiredFloat(json, "t"), requiredFloat(json, "size"), Easing.byName(GsonHelper.getAsString(json, "easing", "linear"))));
         }
      }
      if (result.isEmpty()) {
         result.add(new SizeKeyFrame(0.0F, 0.08F, Easing.LINEAR));
      }
      return result;
   }

   private static List<String> parseStringList(JsonArray array) {
      List<String> result = new ArrayList<>();
      if (array != null) {
         for (JsonElement element : array) {
            result.add(GsonHelper.convertToString(element, "sub effect id"));
         }
      }
      return result;
   }

   private static Vector3f vec(JsonObject json, String key, Vector3f fallback) {
      if (!json.has(key)) {
         return new Vector3f(fallback);
      }
      JsonArray array = requiredArray(json, key);
      if (array.size() != 3) {
         throw new JsonParseException(key + " must be a 3-number array");
      }
      return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
   }

   private static Quaternionf quat(JsonObject json, String key, Quaternionf fallback) {
      if (!json.has(key)) {
         return new Quaternionf(fallback);
      }
      JsonArray array = requiredArray(json, key);
      if (array.size() != 4) {
         throw new JsonParseException(key + " must be a 4-number quaternion array");
      }
      return new Quaternionf(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat()).normalize();
   }

   private static Vector3f[] vecArray(JsonArray array) {
      Vector3f[] result = new Vector3f[array.size()];
      for (int i = 0; i < array.size(); i++) {
         JsonArray vec = GsonHelper.convertToJsonArray(array.get(i), "vector");
         if (vec.size() != 3) {
            throw new JsonParseException("vector must be a 3-number array");
         }
         result[i] = new Vector3f(vec.get(0).getAsFloat(), vec.get(1).getAsFloat(), vec.get(2).getAsFloat());
      }
      return result;
   }

   private static int parseColor(String text) {
      String value = text.startsWith("#") ? text.substring(1) : text;
      if (value.length() == 6) {
         value = "FF" + value;
      }
      if (value.length() != 8) {
         throw new JsonParseException("color must be #RRGGBB or #AARRGGBB");
      }
      return (int)Long.parseLong(value, 16);
   }

   private static void requireOnly(JsonObject json, String... allowed) {
      List<String> allowedList = List.of(allowed);
      for (String key : json.keySet()) {
         if (!allowedList.contains(key)) {
            throw new JsonParseException("Unknown field: " + key);
         }
      }
   }

   private static JsonObject requiredObject(JsonObject json, String key) {
      if (!json.has(key)) {
         throw new JsonParseException("Missing required object: " + key);
      }
      return GsonHelper.getAsJsonObject(json, key);
   }

   private static JsonArray requiredArray(JsonObject json, String key) {
      if (!json.has(key)) {
         throw new JsonParseException("Missing required array: " + key);
      }
      return GsonHelper.getAsJsonArray(json, key);
   }

   private static JsonArray optionalArray(JsonObject json, String key) {
      return json.has(key) ? GsonHelper.getAsJsonArray(json, key) : null;
   }

   private static String requiredString(JsonObject json, String key) {
      if (!json.has(key)) {
         throw new JsonParseException("Missing required string: " + key);
      }
      return GsonHelper.getAsString(json, key);
   }

   private static float requiredFloat(JsonObject json, String key) {
      if (!json.has(key)) {
         throw new JsonParseException("Missing required number: " + key);
      }
      return GsonHelper.getAsFloat(json, key);
   }

   private static float floatValue(JsonObject json, String key, float fallback) {
      return GsonHelper.getAsFloat(json, key, fallback);
   }

   private static int intValue(JsonObject json, String key, int fallback) {
      return GsonHelper.getAsInt(json, key, fallback);
   }

   private static String stringValue(JsonObject json, String key, String fallback) {
      return GsonHelper.getAsString(json, key, fallback);
   }

   private static int resolution(JsonObject json, int fallback) {
      if (json.has("segments")) {
         return GsonHelper.getAsInt(json, "segments");
      }
      if (json.has("sample_count")) {
         return GsonHelper.getAsInt(json, "sample_count");
      }
      if (json.has("detail")) {
         return GsonHelper.getAsInt(json, "detail");
      }
      if (json.has("density")) {
         return Math.max(2, Math.round(GsonHelper.getAsFloat(json, "density")));
      }
      return fallback;
   }
}
