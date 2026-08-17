package net.xxxjk.TYPE_MOON_WORLD.chain.compat;

import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.typemoonworld.api.ServantFormAccess;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

public final class TypeMoonBridge {
    private static final ResourceLocation ENKIDU = ResourceLocation.fromNamespaceAndPath("typemoonworld", "enkidu");
    private static final ResourceLocation GILGAMESH = ResourceLocation.fromNamespaceAndPath("typemoonworld", "gilgamesh");
    private static final String ENUMA_GREEN_COLUMN_EFFECT = "typemoonworld:enuma_green_column";
    private static final ReflectionState REFLECTION = ReflectionState.create();

    public static boolean isEnkiduPlayer(ServerPlayer player) {
        return inspectEnkiduPlayer(player).eligible();
    }

    public static boolean isSyncedEnkiduPlayer(LivingEntity player) {
        RawPlayerForm raw = REFLECTION.readPlayerForm(player);
        return raw.transformed() && isServantId(raw.servantId(), ENKIDU.getPath());
    }

    public static PlayerFormState inspectEnkiduPlayer(ServerPlayer player) {
        return inspectPlayerForm(player, ENKIDU);
    }

    public static boolean isGilgameshPlayer(ServerPlayer player) {
        return inspectPlayerForm(player, GILGAMESH).eligible();
    }

    private static PlayerFormState inspectPlayerForm(ServerPlayer player, ResourceLocation expectedId) {
        boolean apiTransformed = false;
        ResourceLocation apiId = null;
        try {
            ServantFormAccess form = TypeMoonWorldApi.servantForm(player);
            apiTransformed = form.transformed();
            apiId = form.servantId();
        } catch (RuntimeException exception) {
            TYPE_MOON_WORLD.LOGGER.debug("Type Moon servant form is not ready for {}", player.getScoreboardName(), exception);
        }
        RawPlayerForm raw = REFLECTION.readPlayerForm(player);
        boolean rawEligible = raw.transformed() && isServantId(raw.servantId(), expectedId.getPath());
        boolean eligible = apiTransformed && isServantId(apiId, expectedId.getPath()) || rawEligible;
        return new PlayerFormState(eligible, apiTransformed, apiId, raw.transformed(), raw.servantId());
    }

    public static boolean isEnkiduNpc(LivingEntity entity) {
        return ENKIDU.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public static boolean isGilgameshNpc(LivingEntity entity) {
        return GILGAMESH.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public static boolean isDivine(LivingEntity target) {
        return REFLECTION.hasTrait(target, "DIVINE") || REFLECTION.hasTrait(target, "CELESTIAL");
    }

    public static void spawnGoldenGate(ServerLevel level, Vec3 position, Vec3 direction) {
        spawnGoldenGate(level, position, direction, 96.0D);
    }

    public static void spawnGoldenGate(ServerLevel level, Vec3 position, Vec3 direction, double lifetimeTicks) {
        REFLECTION.spawnGoldenGate(level, position, direction, lifetimeTicks);
    }

    public static void spawnEnumaGreenColumn(ServerLevel level, LivingEntity owner) {
        REFLECTION.spawnEnumaGreenColumn(level, owner);
    }

    public static int divinityLevel(LivingEntity target) {
        int strongest = 0;
        for (String skillId : REFLECTION.skillIds(target)) {
            String id = normalizeSkillId(skillId);
            if (id.startsWith("divinity_a") || "god_hand_a".equals(id) || "god_hand_passive".equals(id)) {
                strongest = Math.max(strongest, 5);
            }
            if (id.startsWith("divinity_b")) {
                strongest = Math.max(strongest, 4);
            }
            if (id.startsWith("divinity_c")) {
                strongest = Math.max(strongest, 3);
            }
            if (id.startsWith("divinity_d")) {
                strongest = Math.max(strongest, 2);
            }
            if (id.startsWith("divinity_e")) {
                strongest = Math.max(strongest, 1);
            }
        }
        return strongest > 0 ? strongest : isDivine(target) ? 1 : 0;
    }

    private static String normalizeSkillId(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    private static boolean isServantId(ResourceLocation id, String expectedPath) {
        return id != null && expectedPath.equals(id.getPath());
    }

    private static boolean isServantId(String id, String expectedPath) {
        if (id == null) {
            return false;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        return expectedPath.equals(separator >= 0 ? normalized.substring(separator + 1) : normalized);
    }

    public record PlayerFormState(
        boolean eligible,
        boolean apiTransformed,
        ResourceLocation apiServantId,
        boolean rawTransformed,
        String rawServantId
    ) {
    }

    private record RawPlayerForm(boolean transformed, String servantId) {
        private static final RawPlayerForm EMPTY = new RawPlayerForm(false, null);
    }

    private record ReflectionState(
        Method hasTrait,
        Object divine,
        Object celestial,
        Method skillIdsOf,
        Method spawnOriented,
        Method spawnAttached,
        Object playerVariablesSupplier,
        Field servantCardTransformed,
        Field servantCardId
    ) {
        private static ReflectionState create() {
            Method hasTrait = null;
            Object divine = null;
            Object celestial = null;
            Method skillIdsOf = null;
            Method spawnOriented = null;
            Method spawnAttached = null;
            Object playerVariablesSupplier = null;
            Field servantCardTransformed = null;
            Field servantCardId = null;
            try {
                Class<?> traitClass = Class.forName("net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag");
                Class<?> utilsClass = Class.forName("net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils");
                hasTrait = utilsClass.getMethod("hasTrait", LivingEntity.class, traitClass);
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object resolvedDivine = Enum.valueOf((Class<? extends Enum>)traitClass.asSubclass(Enum.class), "DIVINE");
                divine = resolvedDivine;
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object resolvedCelestial = Enum.valueOf((Class<? extends Enum>)traitClass.asSubclass(Enum.class), "CELESTIAL");
                celestial = resolvedCelestial;
            } catch (ReflectiveOperationException exception) {
                TYPE_MOON_WORLD.LOGGER.error("Type Moon World 3.3.4 chain compatibility initialization failed", exception);
            }
            try {
                Class<?> identityClass = Class.forName(
                    "net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper"
                );
                skillIdsOf = identityClass.getMethod("skillIdsOf", LivingEntity.class);
                Class<?> vfxClass = Class.forName("net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects");
                spawnOriented = vfxClass.getMethod(
                    "spawnOriented",
                    ServerLevel.class,
                    String.class,
                    Vec3.class,
                    Vec3.class,
                    double.class
                );
                spawnAttached = vfxClass.getMethod(
                    "spawn",
                    ServerLevel.class,
                    String.class,
                    Entity.class,
                    double.class
                );
            } catch (ReflectiveOperationException exception) {
                TYPE_MOON_WORLD.LOGGER.warn("Type Moon divinity or golden-gate bridge is unavailable", exception);
            }
            try {
                Class<?> variablesClass = Class.forName("net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables");
                playerVariablesSupplier = variablesClass.getField("PLAYER_VARIABLES").get(null);
                Class<?> playerVariablesClass = Class.forName(
                    "net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables$PlayerVariables"
                );
                servantCardTransformed = playerVariablesClass.getField("servant_card_transformed");
                servantCardId = playerVariablesClass.getField("servant_card_id");
            } catch (ReflectiveOperationException exception) {
                TYPE_MOON_WORLD.LOGGER.warn("Type Moon servant-card compatibility fallback is unavailable", exception);
            }
            return new ReflectionState(
                hasTrait,
                divine,
                celestial,
                skillIdsOf,
                spawnOriented,
                spawnAttached,
                playerVariablesSupplier,
                servantCardTransformed,
                servantCardId
            );
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private RawPlayerForm readPlayerForm(LivingEntity player) {
            if (!(playerVariablesSupplier instanceof Supplier supplier)
                || servantCardTransformed == null || servantCardId == null) {
                return RawPlayerForm.EMPTY;
            }
            try {
                Object variables = player.getData(supplier);
                return new RawPlayerForm(
                    servantCardTransformed.getBoolean(variables),
                    (String)servantCardId.get(variables)
                );
            } catch (RuntimeException | ReflectiveOperationException exception) {
                TYPE_MOON_WORLD.LOGGER.debug("Unable to read raw Type Moon servant-card state", exception);
                return RawPlayerForm.EMPTY;
            }
        }

        private boolean hasTrait(LivingEntity target, String name) {
            if (hasTrait == null) {
                return false;
            }
            Object trait = "DIVINE".equals(name) ? divine : celestial;
            try {
                return Boolean.TRUE.equals(hasTrait.invoke(null, target, trait));
            } catch (ReflectiveOperationException exception) {
                TYPE_MOON_WORLD.LOGGER.debug("Unable to query Type Moon trait {}", name, exception);
                return false;
            }
        }

        private List<String> skillIds(LivingEntity target) {
            if (skillIdsOf == null) {
                return List.of();
            }
            try {
                Object result = skillIdsOf.invoke(null, target);
                if (result instanceof List<?> values) {
                    return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
                }
            } catch (ReflectiveOperationException exception) {
                TYPE_MOON_WORLD.LOGGER.debug("Unable to inspect Type Moon servant skill ids", exception);
            }
            return List.of();
        }

        private void spawnGoldenGate(ServerLevel level, Vec3 position, Vec3 direction, double lifetimeTicks) {
            if (spawnOriented == null) {
                return;
            }
            try {
                spawnOriented.invoke(
                    null,
                    level,
                    "servant_enkidu_age_of_babylon_gate",
                    position,
                    direction,
                    lifetimeTicks
                );
            } catch (ReflectiveOperationException exception) {
                TYPE_MOON_WORLD.LOGGER.warn("Unable to spawn the Type Moon golden gate VFX", exception);
            }
        }

        private void spawnEnumaGreenColumn(ServerLevel level, LivingEntity owner) {
            if (spawnAttached == null) {
                return;
            }
            try {
                spawnAttached.invoke(null, level, ENUMA_GREEN_COLUMN_EFFECT, owner, 192.0D);
            } catch (ReflectiveOperationException exception) {
                TYPE_MOON_WORLD.LOGGER.warn("Unable to spawn the Enuma Elish green column VFX", exception);
            }
        }
    }

    private TypeMoonBridge() {
    }
}

