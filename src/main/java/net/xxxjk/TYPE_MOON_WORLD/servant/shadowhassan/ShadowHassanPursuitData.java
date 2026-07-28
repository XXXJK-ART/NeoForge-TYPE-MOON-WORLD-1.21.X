package net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanDeathShadowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanEntity;

public final class ShadowHassanPursuitData extends SavedData {
   private static final String DATA_NAME = "typemoonworld_shadow_hassan";
   private static final Factory<ShadowHassanPursuitData> FACTORY = new Factory<>(ShadowHassanPursuitData::new, ShadowHassanPursuitData::load);
   private final Map<UUID, Contract> contractsByMaster = new HashMap<>();
   private final Map<UUID, Pursuit> pursuits = new HashMap<>();

   public static ShadowHassanPursuitData get(MinecraftServer server) {
      return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
   }

   public boolean bind(ShadowHassanEntity servant, ServerPlayer master) {
      Contract existing = this.contractsByMaster.get(master.getUUID());
      if (existing != null && !existing.servantUuid.equals(servant.getUUID()) && !existing.consumed) return false;
      boolean servantAlreadyBound = this.contractsByMaster.entrySet().stream().anyMatch(entry ->
         !entry.getKey().equals(master.getUUID()) && entry.getValue().servantUuid.equals(servant.getUUID()) && !entry.getValue().consumed);
      if (servantAlreadyBound) return false;
      this.contractsByMaster.put(master.getUUID(), new Contract(servant.getUUID(), servant.level().dimension(), servant.position(), false));
      this.setDirty();
      return true;
   }

   public void updateServant(ShadowHassanEntity servant) {
      UUID masterUuid = servant.getMasterUuid();
      if (masterUuid == null) return;
      Contract contract = this.contractsByMaster.get(masterUuid);
      if (contract == null || !contract.servantUuid.equals(servant.getUUID())) return;
      contract.dimension = servant.level().dimension();
      contract.position = servant.position();
      this.setDirty();
   }

   public boolean isConsumed(UUID servantUuid) {
      return this.contractsByMaster.values().stream().anyMatch(contract -> contract.servantUuid.equals(servantUuid) && contract.consumed);
   }

   public void consumeServant(UUID servantUuid) {
      this.contractsByMaster.values().stream().filter(contract -> contract.servantUuid.equals(servantUuid)).forEach(contract -> contract.consumed = true);
      this.setDirty();
   }

   public void onMasterDeath(ServerPlayer master, LivingEntity killer) {
      Contract contract = this.contractsByMaster.get(master.getUUID());
      if (contract == null || contract.consumed) return;
      contract.consumed = true;
      if (killer != null && killer.isAlive() && !isPaleRider(killer)) {
         this.addPursuit(contract.dimension, contract.position, killer.getUUID());
      }
      for (ServerLevel level : master.getServer().getAllLevels()) {
         Entity loaded = level.getEntity(contract.servantUuid);
         if (loaded instanceof ShadowHassanEntity servant) servant.consumeForMasterDeath();
      }
      this.setDirty();
   }

   public void addPursuit(ServerLevel level, Vec3 origin, LivingEntity target) {
      if (level == null || target == null || !target.isAlive() || isPaleRider(target)) return;
      this.addPursuit(level.dimension(), origin, target.getUUID());
   }

   public void removePursuit(UUID pursuitUuid) {
      if (pursuitUuid != null && this.pursuits.remove(pursuitUuid) != null) this.setDirty();
   }

   /** Ends every pursuit whose target has died, including its client-visible proxy. */
   public void removePursuitsForTarget(MinecraftServer server, UUID targetUuid) {
      if (server == null || targetUuid == null) return;
      boolean changed = false;
      for (Pursuit pursuit : List.copyOf(this.pursuits.values())) {
         if (!pursuit.targetUuid.equals(targetUuid)) continue;
         ServerLevel level = server.getLevel(pursuit.dimension);
         if (level != null) discardProxy(level, pursuit);
         changed |= this.pursuits.remove(pursuit.id, pursuit);
      }
      if (changed) this.setDirty();
   }

   private void addPursuit(ResourceKey<Level> dimension, Vec3 origin, UUID targetUuid) {
      UUID pursuitUuid = UUID.randomUUID();
      this.pursuits.put(pursuitUuid, new Pursuit(pursuitUuid, targetUuid, dimension, origin, null));
      this.setDirty();
   }

   public void tick(MinecraftServer server) {
      for (Pursuit pursuit : List.copyOf(this.pursuits.values())) {
         if (this.pursuits.get(pursuit.id) != pursuit) continue;
         ServerLevel level = server.getLevel(pursuit.dimension);
         if (level == null) continue;
         LivingEntity target = resolveTarget(server, level, pursuit.targetUuid);
         if (target != null && !target.isAlive()) {
            discardProxy(level, pursuit);
            this.pursuits.remove(pursuit.id, pursuit);
            this.setDirty();
            continue;
         }
         if (target != null && target.level().dimension().equals(pursuit.dimension) && target.isAlive()) {
            if (isPaleRider(target)) {
               discardProxy(level, pursuit);
               this.pursuits.remove(pursuit.id, pursuit);
               this.setDirty();
               continue;
            }
            Vec3 destination = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
            Vec3 delta = destination.subtract(pursuit.position);
            double distance = delta.length();
            if (distance <= ShadowHassanRules.DEATH_SHADOW_CONTACT_DISTANCE) {
               discardProxy(level, pursuit);
               this.pursuits.remove(pursuit.id, pursuit);
               this.setDirty();
               killTarget(level, target);
               continue;
            }
            pursuit.position = ShadowHassanRules.advanceDeathShadow(pursuit.position, destination);
            this.setDirty();
         }
         syncProxy(level, pursuit);
      }
   }

   private static LivingEntity resolveTarget(MinecraftServer server, ServerLevel level, UUID uuid) {
      ServerPlayer player = server.getPlayerList().getPlayer(uuid);
      if (player != null) return player;
      Entity entity = level.getEntity(uuid);
      return entity instanceof LivingEntity living ? living : null;
   }

   private static void syncProxy(ServerLevel level, Pursuit pursuit) {
      ShadowHassanDeathShadowEntity proxy = null;
      if (pursuit.proxyUuid != null && level.getEntity(pursuit.proxyUuid) instanceof ShadowHassanDeathShadowEntity loaded) proxy = loaded;
      BlockPos pos = BlockPos.containing(pursuit.position);
      if (proxy == null && level.hasChunkAt(pos)) {
         proxy = ModEntities.SHADOW_HASSAN_DEATH_SHADOW.get().create(level);
         if (proxy != null) {
            proxy.setPursuitUuid(pursuit.id);
            proxy.setPos(pursuit.position);
            level.addFreshEntity(proxy);
            pursuit.proxyUuid = proxy.getUUID();
         }
      }
      if (proxy != null) proxy.setPos(pursuit.position);
   }

   private static void discardProxy(ServerLevel level, Pursuit pursuit) {
      if (pursuit.proxyUuid != null && level.getEntity(pursuit.proxyUuid) instanceof ShadowHassanDeathShadowEntity proxy) proxy.discard();
   }

   private static void killTarget(ServerLevel level, LivingEntity target) {
      target.getPersistentData().putBoolean("CausalSevered", true);
      target.getPersistentData().putInt("GodHandLives", 0);
      target.getPersistentData().remove("GodHandActive");
      target.getPersistentData().remove("BattleContinuationActive");
      target.getPersistentData().remove("BattleContinuationRecoveryActive");
      target.getPersistentData().remove("EmiyaStyleBattleContinuationActive");
      target.getPersistentData().remove("GawainGutsReady");
      target.getPersistentData().remove("ServantCardGawainBeltReady");
      if (target instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.master_revive_available = false;
         vars.servant_card_death_release = false;
         vars.syncPlayerVariables(player);
      }
      DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
         .getHolderOrThrow(ShadowHassanDamageTypes.MEDITATIVE_SENSITIVITY));
      target.invulnerableTime = 0;
      target.hurt(source, Float.MAX_VALUE);
      if (target.isAlive()) {
         target.setHealth(0.0F);
         target.die(source);
      }
   }

   public static boolean isPaleRider(LivingEntity entity) {
      if (entity instanceof PaleRiderEntity) return true;
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "pale_rider".equals(vars.servant_card_id);
      }
      return false;
   }

   private static ShadowHassanPursuitData load(CompoundTag tag, HolderLookup.Provider provider) {
      ShadowHassanPursuitData data = new ShadowHassanPursuitData();
      ListTag contracts = tag.getList("Contracts", CompoundTag.TAG_COMPOUND);
      for (int i = 0; i < contracts.size(); i++) {
         CompoundTag entry = contracts.getCompound(i);
         if (!entry.hasUUID("Master") || !entry.hasUUID("Servant")) continue;
         data.contractsByMaster.put(entry.getUUID("Master"), Contract.load(entry));
      }
      ListTag pursuits = tag.getList("Pursuits", CompoundTag.TAG_COMPOUND);
      for (int i = 0; i < pursuits.size(); i++) {
         Pursuit pursuit = Pursuit.load(pursuits.getCompound(i));
         if (pursuit != null) data.pursuits.put(pursuit.id, pursuit);
      }
      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
      ListTag contracts = new ListTag();
      this.contractsByMaster.forEach((master, contract) -> contracts.add(contract.save(master)));
      tag.put("Contracts", contracts);
      ListTag pursuits = new ListTag();
      this.pursuits.values().forEach(pursuit -> pursuits.add(pursuit.save()));
      tag.put("Pursuits", pursuits);
      return tag;
   }

   private static ResourceKey<Level> dimension(String value) {
      return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(value));
   }

   private static final class Contract {
      private final UUID servantUuid;
      private ResourceKey<Level> dimension;
      private Vec3 position;
      private boolean consumed;

      private Contract(UUID servantUuid, ResourceKey<Level> dimension, Vec3 position, boolean consumed) {
         this.servantUuid = servantUuid;
         this.dimension = dimension;
         this.position = position;
         this.consumed = consumed;
      }

      private CompoundTag save(UUID master) {
         CompoundTag tag = new CompoundTag();
         tag.putUUID("Master", master);
         tag.putUUID("Servant", this.servantUuid);
         tag.putString("Dimension", this.dimension.location().toString());
         tag.putDouble("X", this.position.x);
         tag.putDouble("Y", this.position.y);
         tag.putDouble("Z", this.position.z);
         tag.putBoolean("Consumed", this.consumed);
         return tag;
      }

      private static Contract load(CompoundTag tag) {
         return new Contract(tag.getUUID("Servant"), dimension(tag.getString("Dimension")),
            new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")), tag.getBoolean("Consumed"));
      }
   }

   private static final class Pursuit {
      private final UUID id;
      private final UUID targetUuid;
      private final ResourceKey<Level> dimension;
      private Vec3 position;
      private UUID proxyUuid;

      private Pursuit(UUID id, UUID targetUuid, ResourceKey<Level> dimension, Vec3 position, UUID proxyUuid) {
         this.id = id;
         this.targetUuid = targetUuid;
         this.dimension = dimension;
         this.position = position;
         this.proxyUuid = proxyUuid;
      }

      private CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putUUID("Id", this.id);
         tag.putUUID("Target", this.targetUuid);
         tag.putString("Dimension", this.dimension.location().toString());
         tag.putDouble("X", this.position.x);
         tag.putDouble("Y", this.position.y);
         tag.putDouble("Z", this.position.z);
         if (this.proxyUuid != null) tag.putUUID("Proxy", this.proxyUuid);
         return tag;
      }

      private static Pursuit load(CompoundTag tag) {
         if (!tag.hasUUID("Id") || !tag.hasUUID("Target") || tag.getString("Dimension").isBlank()) return null;
         return new Pursuit(tag.getUUID("Id"), tag.getUUID("Target"), dimension(tag.getString("Dimension")),
            new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")), tag.hasUUID("Proxy") ? tag.getUUID("Proxy") : null);
      }
   }
}
