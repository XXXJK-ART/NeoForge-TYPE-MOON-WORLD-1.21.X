package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

import java.util.List;
import java.util.Random;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;

public record VFXVanillaParticleDefinition(ParticleOptions options, int count, Vector3f spread, Vector3f offset, float speed, float chance) {
   public VFXVanillaParticleDefinition {
      count = Math.max(1, count);
      spread = spread == null ? new Vector3f() : new Vector3f(spread);
      offset = offset == null ? new Vector3f() : new Vector3f(offset);
      speed = Math.max(0.0F, speed);
      chance = Math.max(0.0F, Math.min(1.0F, chance));
   }

   public void emit(Random random, Vector3f basePosition, List<VFXVanillaParticleSpawn> out) {
      if (this.options == null || random.nextFloat() > this.chance) {
         return;
      }
      for (int i = 0; i < this.count; i++) {
         Vector3f position = new Vector3f(basePosition).add(this.offset);
         Vector3f velocity = new Vector3f(
            randomSigned(random) * this.spread.x * this.speed,
            randomSigned(random) * this.spread.y * this.speed,
            randomSigned(random) * this.spread.z * this.speed
         );
         out.add(new VFXVanillaParticleSpawn(this.options, position, velocity));
      }
   }

   private static float randomSigned(Random random) {
      return random.nextFloat() * 2.0F - 1.0F;
   }

   public static ParticleOptions particleByName(String name) {
      String key = name == null ? "" : name.toLowerCase();
      return switch (key) {
         case "cloud" -> ParticleTypes.CLOUD;
         case "smoke" -> ParticleTypes.SMOKE;
         case "large_smoke" -> ParticleTypes.LARGE_SMOKE;
         case "campfire_cosy_smoke" -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
         case "ash" -> ParticleTypes.ASH;
         case "crit" -> ParticleTypes.CRIT;
         case "enchanted_hit" -> ParticleTypes.ENCHANTED_HIT;
         case "sweep_attack" -> ParticleTypes.SWEEP_ATTACK;
         case "gust" -> ParticleTypes.GUST;
         case "end_rod" -> ParticleTypes.END_ROD;
         case "poof" -> ParticleTypes.POOF;
         case "flame" -> ParticleTypes.FLAME;
         case "lava" -> ParticleTypes.LAVA;
         case "snowflake" -> ParticleTypes.SNOWFLAKE;
         case "soul" -> ParticleTypes.SOUL;
         case "sculk_soul" -> ParticleTypes.SCULK_SOUL;
         case "soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
         case "wax_on" -> ParticleTypes.WAX_ON;
         case "wax_off" -> ParticleTypes.WAX_OFF;
         case "flash" -> ParticleTypes.FLASH;
         case "explosion" -> ParticleTypes.EXPLOSION;
         case "explosion_emitter" -> ParticleTypes.EXPLOSION_EMITTER;
         case "heart" -> ParticleTypes.HEART;
         case "happy_villager" -> ParticleTypes.HAPPY_VILLAGER;
         case "witch" -> ParticleTypes.WITCH;
         case "portal" -> ParticleTypes.PORTAL;
         case "reverse_portal" -> ParticleTypes.REVERSE_PORTAL;
         case "electric_spark" -> ParticleTypes.ELECTRIC_SPARK;
         case "totem_of_undying" -> ParticleTypes.TOTEM_OF_UNDYING;
         case "enchanted_hit_small" -> ParticleTypes.ENCHANTED_HIT;
         default -> throw new IllegalArgumentException("Unknown vanilla particle type: " + name);
      };
   }
}
