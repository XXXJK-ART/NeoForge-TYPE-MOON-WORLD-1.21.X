package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;

/** Stable IDs for the magic attributes built into Type Moon World. */
public final class MagicAttributes {
   public static final ResourceLocation EARTH = builtin("earth");
   public static final ResourceLocation WATER = builtin("water");
   public static final ResourceLocation FIRE = builtin("fire");
   public static final ResourceLocation WIND = builtin("wind");
   public static final ResourceLocation ETHER = builtin("ether");
   public static final ResourceLocation NONE = builtin("none");
   public static final ResourceLocation IMAGINARY_NUMBER = builtin("imaginary_number");
   public static final ResourceLocation SWORD = builtin("sword");

   private MagicAttributes() { }

   private static ResourceLocation builtin(String path) {
      return ResourceLocation.fromNamespaceAndPath("typemoonworld", path);
   }
}
