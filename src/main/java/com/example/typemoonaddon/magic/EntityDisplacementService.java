package com.example.typemoonaddon.magic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.typemoonaddon.network.EntityDisplacementTargetPayload;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

/** Server-authoritative first-entity ray target and same-dimension position swap. */
public final class EntityDisplacementService {
    public static final double RANGE = 32.0D;
    private static final Map<UUID, Target> TARGETS = new ConcurrentHashMap<>();

    private EntityDisplacementService() {
    }

    public static void tick(ServerPlayer player) {
        if (player == null || !isSelected(player)) {
            clear(player);
            return;
        }
        Target next = findTarget(player);
        Target previous = TARGETS.put(player.getUUID(), next);
        if (!next.equals(previous)) {
            send(player, next);
        }
    }

    public static boolean swap(ServerPlayer player) {
        if (player == null || !isSelected(player)) {
            return false;
        }
        Target target = findTarget(player);
        if (target.entityId < 0) {
            clear(player);
            return false;
        }
        Entity other = player.serverLevel().getEntity(target.entityId);
        if (!isSafeTarget(player, other)) {
            clear(player);
            return false;
        }
        boolean swapped = swapEntities(player, other);
        clear(player);
        return swapped;
    }

    /** NPC fallback: use its combat target as the displacement target. */
    public static boolean swapNpc(MysticMagicianEntity caster, LivingEntity target) {
        if (caster == null || target == null || !isSafeTarget(caster, target)
                || caster.isPassenger() || !caster.getPassengers().isEmpty()
                || target.isAlliedTo(caster) || caster.isAlliedTo(target)) {
            return false;
        }
        return swapEntities(caster, target);
    }

    private static boolean swapEntities(Entity first, Entity second) {
        Vec3 firstPos = first.position();
        Vec3 secondPos = second.position();
        Vec3 firstVelocity = first.getDeltaMovement();
        Vec3 secondVelocity = second.getDeltaMovement();
        if (!isSafeDestination(first, second, secondPos) || !isSafeDestination(second, first, firstPos)) {
            return false;
        }
        float firstYaw = first.getYRot();
        float firstPitch = first.getXRot();
        float secondYaw = second.getYRot();
        float secondPitch = second.getXRot();
        first.teleportTo(secondPos.x, secondPos.y, secondPos.z);
        second.teleportTo(firstPos.x, firstPos.y, firstPos.z);
        first.setYRot(secondYaw);
        first.setXRot(secondPitch);
        second.setYRot(firstYaw);
        second.setXRot(firstPitch);
        first.setDeltaMovement(secondVelocity);
        second.setDeltaMovement(firstVelocity);
        first.hurtMarked = true;
        second.hurtMarked = true;
        first.level().playSound(null, firstPos.x, firstPos.y, firstPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        first.level().playSound(null, secondPos.x, secondPos.y, secondPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean isSafeTarget(Entity source, Entity target) {
        if (source == null || target == null || target == source || target.level() != source.level()
                || !source.isAlive() || source.isRemoved()
                || !target.isAlive() || target.isRemoved()
                || (target instanceof Player p && p.isSpectator())
                || target.isPassenger() || !target.getPassengers().isEmpty()
                || !target.isPickable() || target.getBoundingBox().getSize() <= 0.0D) {
            return false;
        }
        return true;
    }

    private static boolean isSafeDestination(Entity entity, Entity ignored, Vec3 destination) {
        AABB moved = entity.getBoundingBox().move(destination.subtract(entity.position()));
        if (!entity.level().getWorldBorder().isWithinBounds(moved)
                || !entity.level().noBlockCollision(entity, moved)) {
            return false;
        }
        return entity.level().getEntities(entity, moved,
                candidate -> candidate != ignored && candidate.isAlive()
                        && candidate.isPickable()
                        && candidate.getBoundingBox().intersects(moved)).isEmpty();
    }

    public static void clear(ServerPlayer player) {
        if (player != null && TARGETS.remove(player.getUUID()) != null) {
            send(player, Target.NONE);
        }
    }

    private static Target findTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(RANGE));
        HitResult blockHit = player.serverLevel().clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, player));
        double limit = blockHit.getType() == HitResult.Type.MISS ? RANGE : start.distanceTo(blockHit.getLocation());
        Vec3 clippedEnd = start.add(player.getLookAngle().scale(limit));
        AABB query = new AABB(start, clippedEnd).inflate(0.3D);
        Entity selected = null;
        double selectedDistance = Double.MAX_VALUE;
        for (Entity candidate : player.serverLevel().getEntities(player, query,
                entity -> isSafeTarget(player, entity))) {
            java.util.Optional<Vec3> hit = candidate.getBoundingBox().clip(start, clippedEnd);
            if (hit.isPresent()) {
                double distance = start.distanceToSqr(hit.get());
                if (distance < selectedDistance) {
                    selected = candidate;
                    selectedDistance = distance;
                }
            }
        }
        if (selected == null) {
            return Target.NONE;
        }
        return new Target(selected.getId(), selected.getBbWidth(), selected.getBbHeight(), selected.getBbWidth());
    }

    private static boolean isSelected(ServerPlayer player) {
        if (!player.isAlive() || player.isRemoved() || player.isSpectator()) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();
        vars.rebuildSelectedMagicsFromActiveWheel();
        PlayerMagicSelectionService.prepareCurrentSelection(player, vars);
        return vars.is_magus && vars.is_magic_circuit_open
                && PlayerMagicSelectionService.isCurrentSelection(vars, EntityDisplacementMagic.MAGIC_ID);
    }

    private static void send(ServerPlayer player, Target target) {
        PacketDistributor.sendToPlayer(player, new EntityDisplacementTargetPayload(
                target.entityId, target.width, target.height, target.depth));
    }

    public record Target(int entityId, float width, float height, float depth) {
        private static final Target NONE = new Target(-1, 0.0F, 0.0F, 0.0F);
    }
}
