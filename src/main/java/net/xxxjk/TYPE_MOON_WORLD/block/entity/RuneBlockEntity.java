package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgram;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneProgramExecutor;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneReleaseMode;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.magic.rune.RuneEffectDispatcher;

/** Persistent rune surface inscription. Offline owners pause rather than consuming mana. */
public final class RuneBlockEntity extends BlockEntity {
   private UUID owner;
   private CompoundTag programSnapshot = new CompoundTag();
   private long armedAt;
   public RuneBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.RUNE_BLOCK_ENTITY.get(), pos, state); }

   public void configure(ServerPlayer player, RuneProgram program) {
      owner = player.getUUID(); programSnapshot = program.serializeNBT();
      CompoundTag config = program.releaseConfig(); long now = player.level().getGameTime();
      armedAt = now + Math.max(0, Math.min(1200, config.getInt("delay")));
      setChanged();
   }

   public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, RuneBlockEntity block) {
      if (!(level instanceof ServerLevel serverLevel) || block.owner == null || block.programSnapshot.isEmpty()) return;
      long now = level.getGameTime(); if (now < block.armedAt || now % 5L != 0L) return;
      ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(block.owner); if (owner == null || !owner.isAlive()) return;
      RuneProgram program = RuneProgram.fromNBT(block.programSnapshot);
      double radius = Math.max(1.0, Math.min(12.0, program.releaseConfig().getDouble("radius") == 0 ? 3.0 : program.releaseConfig().getDouble("radius")));
      LivingEntity target = serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(radius), e -> e.isAlive() && e != owner).stream().findFirst().orElse(null);
      if (target == null) return;
      TypeMoonWorldModVariables.PlayerVariables vars = owner.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (RuneProgramExecutor.executeExternal(owner, vars, program, target, RuneReleaseMode.BLOCK_TRAP,
         Vec3.atCenterOf(pos).add(0.0D, 0.2D, 0.0D), null).success()) {
         RuneEffectDispatcher.emitProgramGlyphs(owner, program, Vec3.atCenterOf(pos).add(0.0D, 0.18D, 0.0D));
         serverLevel.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5D, pos.getY() + 0.15D, pos.getZ() + 0.5D, 12, 0.35D, 0.08D, 0.35D, 0.03D);
         serverLevel.removeBlock(pos, false);
      }
   }

   @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); if (owner != null) tag.putUUID("owner", owner); tag.put("program", programSnapshot.copy()); tag.putLong("armed_at", armedAt); }
   @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null; programSnapshot = tag.contains("program", 10) ? tag.getCompound("program") : new CompoundTag(); armedAt = tag.getLong("armed_at"); }
}
