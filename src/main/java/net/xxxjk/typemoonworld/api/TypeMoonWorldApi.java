package net.xxxjk.typemoonworld.api;

import java.util.ServiceLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

/** Stable entry point for addons. The implementation is supplied by the main mod at runtime. */
public final class TypeMoonWorldApi {
   public static final int API_VERSION = 1;
   public static final int MIN_COMPATIBLE_API_VERSION = 1;
   public static final int MAX_COMPATIBLE_API_VERSION = 1;
   private static final TypeMoonWorldApi INSTANCE = new TypeMoonWorldApi();
   private static volatile ApiProvider provider;

   private TypeMoonWorldApi() {
   }

   /** Singleton façade retained for the documented {@code instance().addon(...)} form. */
   public static TypeMoonWorldApi instance() {
      return INSTANCE;
   }

   public static boolean isCompatible(int requestedVersion) {
      return requestedVersion >= MIN_COMPATIBLE_API_VERSION && requestedVersion <= MAX_COMPATIBLE_API_VERSION;
   }

   public static AddonRegistrar addon(String modId) {
      return provider().addon(modId);
   }

   public static BodyTrainingAccess bodyTraining(LivingEntity entity) {
      return provider().bodyTraining(entity);
   }

   public static MagicAttributeAccess magicAttributes(LivingEntity entity) {
      return provider().magicAttributes(entity);
   }

   public static boolean isMagicAvailable(LivingEntity entity, ResourceLocation magicId) {
      return provider().isMagicAvailable(entity, magicId);
   }

   public static ProjectionEffects projectionEffects() {
      return provider().projectionEffects();
   }

   public static ServantFormAccess servantForm(ServerPlayer player) {
      return provider().servantForm(player);
   }

   public static MasterAccess master(ServerPlayer player) {
      return provider().master(player);
   }

   public static void install(ApiProvider apiProvider) {
      if (apiProvider == null) {
         throw new IllegalArgumentException("apiProvider");
      }
      synchronized (TypeMoonWorldApi.class) {
         if (provider == null) {
            provider = apiProvider;
         }
      }
   }

   private static ApiProvider provider() {
      ApiProvider current = provider;
      if (current != null) {
         return current;
      }
      synchronized (TypeMoonWorldApi.class) {
         current = provider;
         if (current == null) {
            current = ServiceLoader.load(ApiProvider.class).findFirst().orElseThrow(
               () -> new IllegalStateException("Type Moon World API implementation is not loaded")
            );
            provider = current;
         }
         return current;
      }
   }
}
