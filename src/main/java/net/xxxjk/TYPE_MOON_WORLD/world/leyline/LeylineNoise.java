package net.xxxjk.TYPE_MOON_WORLD.world.leyline;

public final class LeylineNoise {
   private static final String DIM_NETHER = "minecraft:the_nether";
   private static final String DIM_END = "minecraft:the_end";
   private static final String DIM_UBW = "typemoonworld:unlimited_blade_works";
   private static final long DIMENSION_SALT = -7046029254386353131L;
   private static final long MICRO_NOISE_SALT = -3335678366873096957L;
   private static final long RIVER_WARP_X_SALT = -6752110988234923001L;
   private static final long RIVER_WARP_Z_SALT = -6939452855193903323L;
   private static final long RIVER_CORE_SALT = -4417276706812531889L;
   private static final long RIVER_TRIBUTARY_SALT = -5801624191690821267L;
   private static final long RIVER_CONTINUITY_SALT = -8134700645754772381L;
   private static final long RIVER_INTENSITY_SALT = 6409622746827299377L;
   private static final long RIVER_LAKE_SALT = 4205101777517744611L;
   private static final long RIVER_WIDTH_SALT = 9088099317168493479L;
   private static final long NOISE_X_MULTIPLIER = 341873128712L;
   private static final long NOISE_Z_MULTIPLIER = 132897987541L;
   private static final double MACRO_SCALE = 8.0;
   private static final double MICRO_SCALE = 2.5;
   private static final double MACRO_WEIGHT = 0.82;
   private static final double MICRO_WEIGHT = 0.18;
   private static final double RIVER_WARP_SCALE = 48.0;
   private static final double RIVER_WARP_STRENGTH = 22.0;
   private static final double RIVER_SCALE = 20.0;
   private static final double RIVER_RIDGE_EXPONENT = 4.15;
   private static final double RIVER_CONTINUITY_SCALE = 0.32;
   private static final double RIVER_INTENSITY_SCALE = 1.3;
   private static final double RIVER_MASK_LOW = 0.34;
   private static final double RIVER_MASK_HIGH = 0.76;
   private static final double RIVER_SHOULDER_MIN = 0.82;
   private static final double RIVER_SHOULDER_SPAN = 0.08;
   private static final double RIVER_CORE_BOOST_MIN = 0.06;
   private static final double RIVER_CORE_BOOST_SPAN = 0.07;
   private static final double RIVER_CORE_LOW = 0.76;
   private static final double RIVER_CORE_HIGH = 0.96;
   private static final double RIVER_TRIBUTARY_WEIGHT = 0.62;
   private static final double RIVER_LAKE_SCALE = 0.78;
   private static final double RIVER_LAKE_LOW = 0.86;
   private static final double RIVER_LAKE_HIGH = 0.99;
   private static final double RIVER_LAKE_EXPAND = 0.6;
   private static final double RIVER_WIDTH_SCALE = 0.6;
   private static final double RIVER_WIDTH_MIN = 0.95;
   private static final double RIVER_WIDTH_MAX = 1.3;
   private static final double LONG_TO_UNIT_DOUBLE = 1.1102230246251565E-16;

   private LeylineNoise() {
   }

   public static long dimensionSeed(long worldSeed, String dimensionId) {
      return mix64(worldSeed ^ mix64(hash64(dimensionId) ^ DIMENSION_SALT));
   }

   public static int computeBaseConcentration(long worldSeed, String dimensionId, int chunkX, int chunkZ) {
      if (isUbwDimension(dimensionId)) {
         return 50;
      } else {
         long dimSeed = dimensionSeed(worldSeed, dimensionId);
         double macro = valueNoise(dimSeed, chunkX / MACRO_SCALE, chunkZ / MACRO_SCALE);
         double micro = valueNoise(dimSeed ^ MICRO_NOISE_SALT, chunkX / MICRO_SCALE, chunkZ / MICRO_SCALE);
         double base = clamp01(macro * MACRO_WEIGHT + micro * MICRO_WEIGHT);
         double riverMask = riverMask(dimSeed, chunkX, chunkZ);
         double combined = blendWithRiver(base, riverMask, dimSeed, chunkX, chunkZ);
         return concentrationFromUnit(combined, dimensionId);
      }
   }

   public static int concentrationFromUnit(double p) {
      return concentrationFromUnit(p, "minecraft:overworld");
   }

   public static int concentrationFromUnit(double p, String dimensionId) {
      double unit = clamp01(p);
      if (isNetherDimension(dimensionId)) {
         return concentrationFromWeightedBands(unit, 50.0, 50.0, 30.0, 10.0);
      } else if (isEndDimension(dimensionId)) {
         return concentrationFromWeightedBands(unit, 40.0, 40.0, 40.0, 20.0);
      } else if (unit < 0.7) {
         double t = unit / 0.7;
         int c = (int)Math.floor(30.0 * Math.pow(t, 3.0));
         return clampInt(c, 0, 29);
      } else if (unit < 0.75) {
         double t = (unit - 0.7) / 0.05;
         int c = 30 + (int)Math.floor(20.0 * t);
         return clampInt(c, 30, 49);
      } else if (unit < 0.9) {
         double t = (unit - 0.75) / 0.15;
         int c = 50 + (int)Math.floor(30.0 * t);
         return clampInt(c, 50, 79);
      } else {
         double t = (unit - 0.9) / 0.1;
         int c = 80 + (int)Math.floor(21.0 * t);
         return clampInt(c, 80, 100);
      }
   }

   public static boolean isUbwDimension(String dimensionId) {
      return DIM_UBW.equals(dimensionId);
   }

   private static boolean isNetherDimension(String dimensionId) {
      return DIM_NETHER.equals(dimensionId);
   }

   private static boolean isEndDimension(String dimensionId) {
      return DIM_END.equals(dimensionId);
   }

   private static int concentrationFromWeightedBands(double unit, double veryLowWeight, double lowWeight, double highWeight, double veryHighWeight) {
      double vl = Math.max(0.0, veryLowWeight);
      double l = Math.max(0.0, lowWeight);
      double h = Math.max(0.0, highWeight);
      double vh = Math.max(0.0, veryHighWeight);
      double sum = vl + l + h + vh;
      if (sum <= 1.0E-9) {
         return concentrationFromUnit(unit);
      } else {
         double cutVl = vl / sum;
         double cutL = cutVl + l / sum;
         double cutH = cutL + h / sum;
         if (unit < cutVl) {
            double t = safeSegmentT(unit, 0.0, cutVl);
            int c = (int)Math.floor(30.0 * Math.pow(t, 2.6));
            return clampInt(c, 0, 29);
         } else if (unit < cutL) {
            double t = safeSegmentT(unit, cutVl, cutL);
            int c = 30 + (int)Math.floor(20.0 * t);
            return clampInt(c, 30, 49);
         } else if (unit < cutH) {
            double t = safeSegmentT(unit, cutL, cutH);
            int c = 50 + (int)Math.floor(30.0 * t);
            return clampInt(c, 50, 79);
         } else {
            double t = safeSegmentT(unit, cutH, 1.0);
            int c = 80 + (int)Math.floor(21.0 * t);
            return clampInt(c, 80, 100);
         }
      }
   }

   private static double safeSegmentT(double value, double low, double high) {
      return high <= low + 1.0E-9 ? 1.0 : clamp01((value - low) / (high - low));
   }

   public static double regenMultiplier(int concentration) {
      int c = clampInt(concentration, 0, 100);
      double unit = c / 100.0;
      return 1.0 + 9.0 * Math.pow(unit, 2.0);
   }

   private static double blendWithRiver(double base, double riverMask, long seed, int chunkX, int chunkZ) {
      if (riverMask <= 0.0) {
         return base;
      } else {
         double rx = warpedRiverX(seed, chunkX, chunkZ);
         double rz = warpedRiverZ(seed, chunkX, chunkZ);
         double intensity = valueNoise(seed ^ RIVER_INTENSITY_SALT, rx * RIVER_INTENSITY_SCALE, rz * RIVER_INTENSITY_SCALE);
         double shoulderTarget = RIVER_SHOULDER_MIN + RIVER_SHOULDER_SPAN * intensity;
         double coreFactor = smoothRange(riverMask, RIVER_CORE_LOW, RIVER_CORE_HIGH);
         double coreBoost = coreFactor * (RIVER_CORE_BOOST_MIN + RIVER_CORE_BOOST_SPAN * intensity);
         double riverTarget = clamp01(shoulderTarget + coreBoost);
         return lerp(base, Math.max(base, riverTarget), riverMask);
      }
   }

   private static double riverMask(long seed, int chunkX, int chunkZ) {
      double rx = warpedRiverX(seed, chunkX, chunkZ);
      double rz = warpedRiverZ(seed, chunkX, chunkZ);
      double trunk = valueNoise(seed ^ RIVER_CORE_SALT, rx, rz * 0.56);
      double trunkRidge = Math.pow(clamp01(1.0 - Math.abs(trunk * 2.0 - 1.0)), RIVER_RIDGE_EXPONENT);
      double tributary = valueNoise(seed ^ RIVER_TRIBUTARY_SALT, rx * 0.56, rz);
      double tributaryRidge = Math.pow(clamp01(1.0 - Math.abs(tributary * 2.0 - 1.0)), 4.95);
      double widthNoise = valueNoise(seed ^ RIVER_WIDTH_SALT, rx * RIVER_WIDTH_SCALE, rz * RIVER_WIDTH_SCALE);
      double widthScale = lerp(RIVER_WIDTH_MIN, RIVER_WIDTH_MAX, widthNoise);
      double riverPath = Math.max(trunkRidge, tributaryRidge * RIVER_TRIBUTARY_WEIGHT) * widthScale;
      double continuity = valueNoise(seed ^ RIVER_CONTINUITY_SALT, rx * RIVER_CONTINUITY_SCALE, rz * RIVER_CONTINUITY_SCALE);
      double continuityMask = smoothRange(continuity, 0.3, 0.96);
      double baseMask = smoothRange(riverPath * continuityMask, RIVER_MASK_LOW, RIVER_MASK_HIGH);
      double lakeNoise = valueNoise(seed ^ RIVER_LAKE_SALT, rx * RIVER_LAKE_SCALE, rz * RIVER_LAKE_SCALE);
      double lakeHotspot = smoothRange(lakeNoise, RIVER_LAKE_LOW, RIVER_LAKE_HIGH);
      double lakeEligibility = smoothRange(baseMask, 0.26, 0.9);
      double lakeBoost = lakeHotspot * lakeEligibility * RIVER_LAKE_EXPAND;
      return clamp01(Math.max(baseMask, baseMask + lakeBoost));
   }

   private static double warpedRiverX(long seed, int chunkX, int chunkZ) {
      double wx = valueNoise(seed ^ RIVER_WARP_X_SALT, chunkX / RIVER_WARP_SCALE, chunkZ / RIVER_WARP_SCALE);
      double offset = (wx * 2.0 - 1.0) * RIVER_WARP_STRENGTH;
      return (chunkX + offset) / RIVER_SCALE;
   }

   private static double warpedRiverZ(long seed, int chunkX, int chunkZ) {
      double wz = valueNoise(seed ^ RIVER_WARP_Z_SALT, chunkX / RIVER_WARP_SCALE, chunkZ / RIVER_WARP_SCALE);
      double offset = (wz * 2.0 - 1.0) * RIVER_WARP_STRENGTH;
      return (chunkZ + offset) / RIVER_SCALE;
   }

   private static double valueNoise(long seed, double x, double z) {
      int x0 = (int)Math.floor(x);
      int z0 = (int)Math.floor(z);
      int x1 = x0 + 1;
      int z1 = z0 + 1;
      double tx = x - x0;
      double tz = z - z0;
      double v00 = cornerValue(seed, x0, z0);
      double v10 = cornerValue(seed, x1, z0);
      double v01 = cornerValue(seed, x0, z1);
      double v11 = cornerValue(seed, x1, z1);
      double sx = smoothStep(tx);
      double sz = smoothStep(tz);
      double ix0 = lerp(v00, v10, sx);
      double ix1 = lerp(v01, v11, sx);
      return lerp(ix0, ix1, sz);
   }

   private static double cornerValue(long seed, int x, int z) {
      long hashed = mix64(seed ^ x * NOISE_X_MULTIPLIER ^ z * NOISE_Z_MULTIPLIER);
      return toUnitDouble(hashed);
   }

   private static long hash64(String s) {
      long h = -3750763034362895579L;

      for (int i = 0; i < s.length(); i++) {
         h ^= s.charAt(i);
         h *= 1099511628211L;
      }

      return h;
   }

   private static long mix64(long z) {
      z = (z ^ z >>> 33) * -49064778989728563L;
      z = (z ^ z >>> 33) * -4265267296055464877L;
      return z ^ z >>> 33;
   }

   private static double toUnitDouble(long bits) {
      return (bits >>> 11 & 9007199254740991L) * LONG_TO_UNIT_DOUBLE;
   }

   private static double smoothStep(double t) {
      double c = clamp01(t);
      return c * c * (3.0 - 2.0 * c);
   }

   private static double lerp(double a, double b, double t) {
      return a + (b - a) * clamp01(t);
   }

   private static double clamp01(double v) {
      return Math.max(0.0, Math.min(1.0, v));
   }

   private static double smoothRange(double value, double low, double high) {
      if (high <= low) {
         return value >= high ? 1.0 : 0.0;
      } else {
         double t = (value - low) / (high - low);
         return smoothStep(clamp01(t));
      }
   }

   private static int clampInt(int v, int min, int max) {
      return Math.max(min, Math.min(max, v));
   }
}
