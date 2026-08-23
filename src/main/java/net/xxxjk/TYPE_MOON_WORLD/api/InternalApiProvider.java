package net.xxxjk.TYPE_MOON_WORLD.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.typemoonworld.api.AddonRegistrar;
import net.xxxjk.typemoonworld.api.ApiProvider;
import net.xxxjk.typemoonworld.api.BodyTrainingAccess;
import net.xxxjk.typemoonworld.api.GemAffinity;
import net.xxxjk.typemoonworld.api.GemRegistry;
import net.xxxjk.typemoonworld.api.ProjectionEffects;
import net.xxxjk.typemoonworld.api.ServantFormAccess;
import net.xxxjk.typemoonworld.api.MasterAccess;
import net.xxxjk.typemoonworld.api.MasterProfileData;
import net.xxxjk.typemoonworld.api.MasterProfileInitializer;
import net.xxxjk.typemoonworld.api.MasterProfileRegistry;
import net.xxxjk.typemoonworld.api.CardActionExecutor;
import net.xxxjk.typemoonworld.api.ClientExtensionRegistry;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.LifecycleHook;
import net.xxxjk.typemoonworld.api.LifecycleHookRegistry;
import net.xxxjk.typemoonworld.api.MagicCastContext;
import net.xxxjk.typemoonworld.api.MagicExecutor;
import net.xxxjk.typemoonworld.api.MagicOptionsExtension;
import net.xxxjk.typemoonworld.api.MagicPresetHandler;
import net.xxxjk.typemoonworld.api.MagicRegistry;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;
import net.xxxjk.typemoonworld.api.MagicKnowledge;
import net.xxxjk.typemoonworld.api.ManaAccess;
import net.xxxjk.typemoonworld.api.MagicAttributeAccess;
import net.xxxjk.typemoonworld.api.NoblePhantasmExecutor;
import net.xxxjk.typemoonworld.api.NoblePhantasmRegistry;
import net.xxxjk.typemoonworld.api.ServantActionExecutor;
import net.xxxjk.typemoonworld.api.ServantCardRegistry;
import net.xxxjk.typemoonworld.api.ServantContext;
import net.xxxjk.typemoonworld.api.ServantRegistry;
import net.xxxjk.typemoonworld.api.SkillExecutor;
import net.xxxjk.typemoonworld.api.SkillRegistry;
import net.xxxjk.typemoonworld.api.event.MagicCastEvent;
import net.xxxjk.typemoonworld.api.event.ServantActionEvent;
import net.xxxjk.typemoonworld.api.event.ServantSummonEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.IMagicExecutor;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.registry.MagicModularRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantCombatActionExecutor;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantLifecycleHandler;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantNoblePhantasmExecutor;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantSkillExecutor;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantLifecycleContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantNoblePhantasmContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.registry.ServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantSkillRegistry;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GenericServantSummonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MasterCardItem;
import net.xxxjk.TYPE_MOON_WORLD.martial.BodyTrainingService;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.ProjectionEffectHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager;
import net.minecraft.server.level.ServerPlayer;

/** Main-mod implementation of the stable public API. */
public final class InternalApiProvider implements ApiProvider {
   private static final Map<String, MagicExecutor> PUBLIC_MAGIC_EXECUTORS = new ConcurrentHashMap<>();
   private final Map<String, AddonRegistrar> addons = new ConcurrentHashMap<>();

   @Override
   public AddonRegistrar addon(String modId) {
      String namespace = normalizeNamespace(modId);
      return this.addons.computeIfAbsent(namespace, InternalAddonRegistrar::new);
   }

   @Override
   public BodyTrainingAccess bodyTraining(LivingEntity entity) {
      if (!(entity instanceof ServerPlayer player)) return new BodyTrainingAccess() {
         public int experience() { return 0; }
         public int unspentPoints() { return 0; }
         public int strength() { return 0; }
         public int speed() { return 0; }
         public int resistance() { return 0; }
         public int technique() { return 0; }
         public int totalEarned() { return 0; }
         public int nextPointCost() { return 0; }
         public boolean allocate(String stat) { return false; }
         public void award(int amount) { }
      };
      var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return new BodyTrainingAccess() {
         public int experience() { return Math.max(0, vars.body_training_xp); }
         public int unspentPoints() { return Math.max(0, vars.body_training_points); }
         public int strength() { return Math.max(0, vars.body_strength); }
         public int speed() { return Math.max(0, vars.body_speed); }
         public int resistance() { return Math.max(0, vars.body_resistance); }
         public int technique() { return Math.max(0, vars.body_technique); }
         public int totalEarned() { return BodyTrainingService.totalEarned(vars); }
         public int nextPointCost() { return BodyTrainingService.nextPointCost(vars); }
         public boolean allocate(String stat) { return BodyTrainingService.allocate(player, stat); }
         public void award(int amount) { BodyTrainingService.award(player, amount); }
      };
   }

   @Override
   public MagicAttributeAccess magicAttributes(LivingEntity entity) {
      return MagicAttributeService.access(entity);
   }

   @Override
   public ProjectionEffects projectionEffects() {
      return new ProjectionEffects() {
         public void structureStart(ServerLevel level, BlockPos anchor) { ProjectionEffectHelper.spawnStructureStart(level, anchor); }
         public void blockPlace(ServerLevel level, BlockPos position) { ProjectionEffectHelper.spawnBlockPlace(level, position); }
         public void blockBreak(ServerLevel level, BlockPos position) { ProjectionEffectHelper.spawnBlockBreak(level, position); }
      };
   }

   @Override
   public ServantFormAccess servantForm(ServerPlayer player) {
      if (player == null) throw new IllegalArgumentException("player");
      var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return new ServantFormAccess() {
         public boolean transformed() { return vars.servant_card_transformed; }
         public ResourceLocation servantId() {
            if (!vars.servant_card_transformed || vars.servant_card_id == null || vars.servant_card_id.isBlank()) return null;
            if (vars.servant_card_id.indexOf(':') >= 0) return ResourceLocation.tryParse(vars.servant_card_id);
            return ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, vars.servant_card_id);
         }
         public double mana() { return Math.max(0.0, vars.servant_card_mana); }
         public double maximumMana() { return Math.max(0.0, vars.servant_card_max_mana); }
         public boolean transform(ResourceLocation id) {
            if (id == null) return false;
            String storedId = net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID.equals(id.getNamespace()) ? id.getPath() : id.toString();
            return ServantCardTransformManager.transform(player, storedId);
         }
         public boolean release() { return ServantCardTransformManager.release(player, false); }
         public boolean triggerAction(int slot) { return ServantCardTransformManager.triggerAction(player, slot); }
      };
   }

   @Override
   public MasterAccess master(ServerPlayer player) {
      if (player == null) throw new IllegalArgumentException("player");
      var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return new MasterAccess() {
         public boolean active() { return vars.master_active; }
         public int commandSpells() { return Math.max(0, vars.master_command_spells); }
         public String commandSpellStyle() { return vars.master_command_spell_style == null ? "default" : vars.master_command_spell_style; }
         public java.util.UUID boundServant() {
            try { return vars.master_servant_uuid == null || vars.master_servant_uuid.isBlank() ? null : java.util.UUID.fromString(vars.master_servant_uuid); }
            catch (IllegalArgumentException ignored) { return null; }
         }
         public boolean activate() { return MasterStateManager.activate(player); }
         public boolean release() { return MasterStateManager.release(player); }
         public boolean bind(ServerPlayer servant) { return servant != null && MasterStateManager.bind(player, servant); }
      };
   }

   /** Runtime bridge used by the NPC service; the public executor is deliberately kept
    * separate from the legacy context adapter so NPCs can receive target and preset data. */
   public static ExecutionResult executeNpc(LivingEntity caster, LivingEntity target, String magicId,
                                            net.minecraft.nbt.CompoundTag preset, double proficiency, long gameTick) {
      MagicExecutor executor = resolvePublicMagicExecutor(magicId);
      ResourceLocation id = resolveMagicId(magicId);
      if (executor == null || id == null) return ExecutionResult.NOT_HANDLED;
      if (caster == null || !MagicDefinitionRegistry.meetsAttributeRequirements(
            caster.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES), magicId)) {
         return ExecutionResult.FAILED;
      }
      var context = new net.xxxjk.typemoonworld.api.MagicCastContext(
         caster, target, caster == null ? null : caster.level(), magicId,
         preset == null ? new net.minecraft.nbt.CompoundTag() : preset.copy(), false, proficiency);
      MagicCastEvent.Pre pre = NeoForge.EVENT_BUS.post(new MagicCastEvent.Pre(id, context));
      if (pre.isCanceled()) return ExecutionResult.FAILED;
      try {
         ExecutionResult result = executor.execute(context);
         result = result == null ? ExecutionResult.FAILED : result;
         NeoForge.EVENT_BUS.post(new MagicCastEvent.Post(id, context, result));
         return result;
      } catch (RuntimeException ex) {
         net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.error("Addon NPC magic executor failed: {}", magicId, ex);
         return ExecutionResult.FAILED;
      }
   }

   private static MagicExecutor resolvePublicMagicExecutor(String magicId) {
      if (magicId == null || magicId.isBlank()) return null;
      MagicExecutor executor = PUBLIC_MAGIC_EXECUTORS.get(magicId);
      if (executor != null) return executor;
      ResourceLocation parsed = ResourceLocation.tryParse(magicId);
      if (parsed != null) {
         executor = PUBLIC_MAGIC_EXECUTORS.get(parsed.toString());
         if (executor != null) return executor;
      } else {
         executor = PUBLIC_MAGIC_EXECUTORS.get(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID + ":" + magicId);
         if (executor != null) return executor;
      }
      String path = parsed == null ? magicId : parsed.getPath();
      MagicExecutor match = null;
      for (Map.Entry<String, MagicExecutor> entry : PUBLIC_MAGIC_EXECUTORS.entrySet()) {
         ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
         if (id != null && id.getPath().equals(path)) {
            if (match != null) return null;
            match = entry.getValue();
         }
      }
      return match;
   }

   private static ResourceLocation resolveMagicId(String magicId) {
      if (magicId == null || magicId.isBlank()) return null;
      ResourceLocation parsed = ResourceLocation.tryParse(magicId);
      if (parsed != null) return parsed;
      return ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, magicId);
   }

   private static String normalizeNamespace(String modId) {
      if (modId == null || !modId.matches("[a-z][a-z0-9_]{1,63}")) {
         throw new IllegalArgumentException("Invalid addon mod id: " + modId);
      }
      return modId;
   }

   private static boolean valid(ResourceLocation id, String namespace) {
      return id != null && namespace.equals(id.getNamespace());
   }

   private static final class InternalAddonRegistrar implements AddonRegistrar {
      private final String modId;
      private final ServantRegistry servants;
      private final ServantCardRegistry cards;
      private final MagicRegistry magics;
      private final SkillRegistry skills;
      private final NoblePhantasmRegistry noblePhantasms;
      private final LifecycleHookRegistry lifecycle;
      private final ClientExtensionRegistry client;
      private final GemRegistry gems;
      private final MasterProfileRegistry masters;
      private final net.xxxjk.typemoonworld.api.ProjectionRegistry projections;
      private final net.xxxjk.typemoonworld.api.CommandSpellRegistry commandSpells;
      private final net.xxxjk.typemoonworld.api.ProjectileRegistry projectiles;
      private final net.xxxjk.typemoonworld.api.EffectsRegistry effects;
      private final net.xxxjk.typemoonworld.api.DamageTypeRegistry damageTypes;
      private final net.xxxjk.typemoonworld.api.AiTacticsRegistry ai;

      private InternalAddonRegistrar(String modId) {
         this.modId = modId;
         this.servants = new Servants(modId);
         this.cards = new Cards(modId);
         this.magics = new Magics(modId);
         this.skills = new Skills(modId);
         this.noblePhantasms = new NoblePhantasms(modId);
         this.lifecycle = new Lifecycles(modId);
         this.client = new Clients(modId);
         this.gems = new Gems(modId);
         this.masters = new Masters(modId);
         this.projections = new Projections(modId);
         this.commandSpells = new CommandSpells(modId);
         this.projectiles = new Projectiles(modId);
         this.effects = new Effects(modId);
         this.damageTypes = new DamageTypes(modId);
         this.ai = new AiTactics(modId);
      }

      @Override public String modId() { return this.modId; }
      @Override public ServantRegistry servants() { return this.servants; }
      @Override public ServantCardRegistry cards() { return this.cards; }
      @Override public MagicRegistry magics() { return this.magics; }
      @Override public SkillRegistry skills() { return this.skills; }
      @Override public NoblePhantasmRegistry noblePhantasms() { return this.noblePhantasms; }
      @Override public LifecycleHookRegistry lifecycle() { return this.lifecycle; }
      @Override public ClientExtensionRegistry client() { return this.client; }
      @Override public GemRegistry gems() { return this.gems; }
      @Override public MasterProfileRegistry masters() { return this.masters; }
      @Override public net.xxxjk.typemoonworld.api.ProjectionRegistry projections() { return this.projections; }
      @Override public net.xxxjk.typemoonworld.api.CommandSpellRegistry commandSpells() { return this.commandSpells; }
      @Override public net.xxxjk.typemoonworld.api.ProjectileRegistry projectiles() { return this.projectiles; }
      @Override public net.xxxjk.typemoonworld.api.EffectsRegistry effects() { return this.effects; }
      @Override public net.xxxjk.typemoonworld.api.DamageTypeRegistry damageTypes() { return this.damageTypes; }
      @Override public net.xxxjk.typemoonworld.api.AiTacticsRegistry ai() { return this.ai; }
   }

   private static final class Servants implements ServantRegistry {
      private final String namespace;
      private Servants(String namespace) { this.namespace = namespace; }

      @Override
      public boolean registerAction(ResourceLocation id, ServantActionExecutor executor) {
         if (!valid(id, this.namespace) || executor == null) return false;
         IServantCombatActionExecutor adapter = context -> toLegacy(executor.execute(new ServantContext(
            context.caster(), context.target(), context.caster().getServantId(), context.caster().level(),
            context.distance(), context.hasLineOfSight(), context.gameTick()
         )));
         return ServantAddonRegistry.registerExternalCombatAction(id.toString(), adapter, this.namespace);
      }

      @Override
      public boolean registerDefinition(ResourceLocation id) {
         return valid(id, this.namespace) && ServantAddonRegistry.declareExternalDefinition(id.toString(), this.namespace);
      }

      @Override
      public ItemStack createCard(ResourceLocation servantId) {
         if (!valid(servantId, this.namespace) || "ushiwakamaru_rider".equals(servantId.getPath())) return ItemStack.EMPTY;
         return ServantCardItem.create(ModItems.SERVANT_CARD_GENERIC.get(), servantId.toString());
      }

      @Override
      public ItemStack createSummonItem(ResourceLocation servantId) {
         if (!valid(servantId, this.namespace)) return ItemStack.EMPTY;
         return GenericServantSummonItem.create(ModItems.SERVANT_SUMMON_GENERIC.get(), servantId.toString());
      }

      @Override
      public LivingEntity summon(ServerLevel level, ResourceLocation servantId, BlockPos pos) {
         if (level == null || pos == null || !valid(servantId, this.namespace)) return null;
         ServantSummonEvent.Pre pre = NeoForge.EVENT_BUS.post(new ServantSummonEvent.Pre(level, servantId, pos));
         if (pre.isCanceled()) return null;
         var entity = net.xxxjk.TYPE_MOON_WORLD.servant.entity.BuiltinServantEntityFactory.create(level, servantId);
         if (entity == null) return null;
         entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
         entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
         if (!level.addFreshEntity(entity)) return null;
         NeoForge.EVENT_BUS.post(new ServantSummonEvent.Post(level, servantId, pos, entity));
         return entity;
      }
      @Override public boolean hasDefinition(ResourceLocation servantId) {
         if (servantId == null) return false;
         String storedId = net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace()) ? servantId.getPath() : servantId.toString();
         return net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry.contains(storedId);
      }
      @Override public java.util.Set<ResourceLocation> definitions() {
         java.util.Set<ResourceLocation> result = new java.util.LinkedHashSet<>();
         for (String id : net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry.getAll().keySet()) {
            ResourceLocation parsed = id.indexOf(':') >= 0 ? ResourceLocation.tryParse(id)
               : ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, id);
            if (parsed != null && valid(parsed, this.namespace)) result.add(parsed);
         }
         return java.util.Collections.unmodifiableSet(result);
      }
   }

   private static final class Cards implements ServantCardRegistry {
      private final String namespace;
      private Cards(String namespace) { this.namespace = namespace; }
      @Override public boolean registerAction(ResourceLocation id, CardActionExecutor executor) {
         return valid(id, this.namespace) && executor != null && CardActionRegistry.register(id, executor, this.namespace);
      }
      @Override public boolean bindSlot(ResourceLocation servantId, int slot, ResourceLocation actionId) {
         return valid(servantId, this.namespace) && actionId != null && CardActionRegistry.bindSlot(servantId, slot, actionId.toString(), this.namespace);
      }
      @Override public ResourceLocation actionForSlot(ResourceLocation servantId, int slot) {
         if (!valid(servantId, this.namespace)) return null;
         return ResourceLocation.tryParse(CardActionRegistry.actionIdForSlot(servantId.toString(), slot));
      }
   }

   private static final class Magics implements MagicRegistry {
      private final String namespace;
      private Magics(String namespace) { this.namespace = namespace; }
      @Override public boolean registerExecutor(ResourceLocation id, MagicExecutor executor) {
         if (!valid(id, this.namespace) || executor == null) return false;
         IMagicExecutor adapter = context -> {
            LivingEntity caster = context.asPlayer();
            if (caster == null && context.entity() instanceof LivingEntity living) caster = living;
            double proficiency = context.vars() == null ? 0.0D
               : net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.get(context.vars(), context.magicId());
            var result = executor.execute(new MagicCastContext(
               caster,
               null,
               context.entity().level(),
               context.magicId(),
               context.payload() == null ? new net.minecraft.nbt.CompoundTag() : context.payload().copy(),
               context.crestCast(),
               proficiency
            ));
            if (result == null || !result.handled()) return net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult.NOT_HANDLED;
            return new net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult(result.handled(), result.success(), result.resourceCost(), result.cooldownTicks());
         };
         boolean registered = MagicModularRegistry.registerExternal(id.toString(), adapter, this.namespace);
         if (registered) PUBLIC_MAGIC_EXECUTORS.put(id.toString(), executor);
         return registered;
      }
      @Override public boolean registerDefinition(MagicDefinitionData definition) {
         return definition != null && valid(definition.id(), this.namespace) && MagicDefinitionRegistry.register(definition, this.namespace);
      }
      @Override public boolean registerPreset(ResourceLocation id, MagicPresetHandler handler) {
         return valid(id, this.namespace) && handler != null && MagicPresetRegistry.register(id, handler, this.namespace);
      }
      @Override public MagicKnowledge knowledge(LivingEntity entity) {
         if (entity == null) throw new IllegalArgumentException("entity");
         var vars = entity.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return new MagicKnowledge() {
             @Override public boolean isLearned(ResourceLocation magicId) {
                if (magicId == null) return false;
                return vars.hasLearnedSelfMagic(magicId.toString())
                   || vars.hasLearnedSelfMagic(magicId.getPath());
             }
             @Override public boolean learn(ResourceLocation magicId) {
                if (magicId == null
                   || net.xxxjk.TYPE_MOON_WORLD.talent.TalentService.isTalent(magicId.toString())
                   || !MagicDefinitionRegistry.contains(magicId.toString())
                   || vars.learned_magics.contains(magicId.toString())
                   || vars.learned_magics.contains(magicId.getPath())) return false;
                vars.learned_magics.add(magicId.toString());
                vars.syncPlayerVariables(entity);
                return true;
             }
             @Override public double proficiency(ResourceLocation magicId) {
                if (magicId == null) return 0.0;
                if (net.xxxjk.TYPE_MOON_WORLD.talent.TalentService.isTalent(magicId.toString())) {
                   return net.xxxjk.TYPE_MOON_WORLD.talent.TalentService.proficiency(vars, magicId.toString());
                }
                return net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.get(vars, magicId.toString());
             }
             @Override public void setProficiency(ResourceLocation magicId, double value) {
                if (magicId == null || net.xxxjk.TYPE_MOON_WORLD.talent.TalentService.isTalent(magicId.toString())) return;
                net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.set(vars, magicId.toString(), value);
               vars.syncPlayerVariables(entity);
            }
         };
      }
      @Override public ManaAccess mana(LivingEntity entity) {
         if (entity == null) throw new IllegalArgumentException("entity");
         var vars = entity.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return new ManaAccess() {
            @Override public double current() { return vars.player_mana; }
            @Override public double maximum() { return vars.player_max_mana; }
            @Override public boolean tryConsume(double amount) {
               if (amount < 0.0 || vars.player_mana < amount) return false;
               vars.player_mana -= amount;
               vars.syncMana(entity);
               return true;
            }
            @Override public void add(double amount) {
               vars.player_mana = Math.min(vars.player_max_mana, Math.max(0.0, vars.player_mana + amount));
               vars.syncMana(entity);
            }
         };
      }
      @Override public java.util.Optional<MagicDefinitionData> definition(ResourceLocation id) {
         return java.util.Optional.ofNullable(id == null ? null : MagicDefinitionRegistry.get(id.toString()));
      }
      @Override public java.util.Set<ResourceLocation> definitions() {
         java.util.Set<ResourceLocation> result = new java.util.LinkedHashSet<>();
         for (String id : MagicDefinitionRegistry.ids()) {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null && valid(parsed, this.namespace)) result.add(parsed);
         }
         return java.util.Collections.unmodifiableSet(result);
      }
   }

   private static final class Skills implements SkillRegistry {
      private final String namespace;
      private Skills(String namespace) { this.namespace = namespace; }
      @Override public boolean register(ResourceLocation id, SkillExecutor executor) {
         if (!valid(id, this.namespace) || executor == null) return false;
         IServantSkillExecutor adapter = context -> {
            LivingEntity caster = context.caster();
            var publicContext = new ServantContext(caster, context.target(), caster != null ? caster.getType().toString() : "", caster != null ? caster.level() : null, 0.0, true, 0L);
            var pre = NeoForge.EVENT_BUS.post(new ServantActionEvent.Pre(ServantActionEvent.Kind.SKILL, id, publicContext));
            if (pre.isCanceled()) return ServantExecutionResult.FAILED;
            var result = executor.execute(publicContext);
            NeoForge.EVENT_BUS.post(new ServantActionEvent.Post(ServantActionEvent.Kind.SKILL, id, publicContext,
               result == null ? ExecutionResult.FAILED : result));
            return toLegacy(result);
         };
         return ServantSkillRegistry.registerExternal(id.toString(), adapter, this.namespace);
      }
   }

   private static final class NoblePhantasms implements NoblePhantasmRegistry {
      private final String namespace;
      private NoblePhantasms(String namespace) { this.namespace = namespace; }
      @Override public boolean register(ResourceLocation id, NoblePhantasmExecutor executor) {
         if (!valid(id, this.namespace) || executor == null) return false;
         IServantNoblePhantasmExecutor adapter = context -> {
            var publicContext = new ServantContext(context.caster(), context.target(), context.caster().getServantId(), context.caster().level(), 0.0, true, 0L);
            var pre = NeoForge.EVENT_BUS.post(new ServantActionEvent.Pre(ServantActionEvent.Kind.NOBLE_PHANTASM, id, publicContext));
            if (pre.isCanceled()) return ServantExecutionResult.FAILED;
            var result = executor.execute(publicContext);
            NeoForge.EVENT_BUS.post(new ServantActionEvent.Post(ServantActionEvent.Kind.NOBLE_PHANTASM, id, publicContext,
               result == null ? ExecutionResult.FAILED : result));
            return toLegacy(result);
         };
         return ServantAddonRegistry.registerExternalNoblePhantasm(id.toString(), adapter, this.namespace);
      }
   }

   private static final class Lifecycles implements LifecycleHookRegistry {
      private final String namespace;
      private Lifecycles(String namespace) { this.namespace = namespace; }
      @Override public boolean register(ResourceLocation id, LifecycleHook hook) {
         if (!valid(id, this.namespace) || hook == null) return false;
         IServantLifecycleHandler adapter = context -> {
            var result = hook.tick(new ServantContext(context.entity(), context.target(), context.entity().getServantId(), context.entity().level(), 0.0, true, context.gameTick()));
            return toLegacy(result);
         };
         return ServantAddonRegistry.registerExternalLifecycle(id.toString(), adapter, this.namespace);
      }
   }

   private static final class Clients implements ClientExtensionRegistry {
      private final String namespace;
      private Clients(String namespace) { this.namespace = namespace; }
      @Override public boolean registerMagicOptions(ResourceLocation id, MagicOptionsExtension extension) {
         return valid(id, this.namespace) && extension != null && ClientExtensionRegistryImpl.register(id, extension, this.namespace);
      }
      @Override public boolean registerControl(ResourceLocation id, net.xxxjk.typemoonworld.api.MagicOption option) {
         return valid(id, this.namespace) && option != null && ExtensionApiRegistry.registerControl(id, option);
      }
   }

   private static final class Gems implements GemRegistry {
      private final String namespace;
      private Gems(String namespace) { this.namespace = namespace; }
      @Override public boolean registerMagic(ResourceLocation id, GemAffinity affinity) {
         return valid(id, this.namespace) && affinity != null && GemApiRegistry.register(id, affinity);
      }
      @Override public boolean isCompatible(ResourceLocation id) { return GemApiRegistry.contains(id); }
      @Override public int calculateSuccess(ResourceLocation id, net.xxxjk.typemoonworld.api.GemType type,
                                             net.xxxjk.typemoonworld.api.GemQuality quality, double proficiency) {
         return GemApiRegistry.calculate(id, type, quality, proficiency);
      }
      @Override public java.util.Set<ResourceLocation> registeredMagics() { return GemApiRegistry.ids(); }
   }

   private static final class Masters implements MasterProfileRegistry {
      private final String namespace;
      private Masters(String namespace) { this.namespace = namespace; }
      @Override public boolean register(MasterProfileData profile, MasterProfileInitializer initializer) {
         return profile != null && valid(profile.id(), this.namespace) && initializer != null
            && MasterProfileApiRegistry.register(profile, initializer);
      }
      @Override public java.util.Optional<MasterProfileData> profile(ResourceLocation id) {
         if (!valid(id, this.namespace)) return java.util.Optional.empty();
         MasterProfileApiRegistry.Entry entry = MasterProfileApiRegistry.get(id);
         return java.util.Optional.ofNullable(entry == null ? null : entry.data());
      }
      @Override public java.util.Set<ResourceLocation> profiles() {
         java.util.Set<ResourceLocation> result = new java.util.LinkedHashSet<>();
         for (ResourceLocation id : MasterProfileApiRegistry.ids()) if (valid(id, this.namespace)) result.add(id);
         return java.util.Collections.unmodifiableSet(result);
      }
      @Override public ItemStack createCard(ResourceLocation id) {
         return valid(id, this.namespace) && MasterProfileApiRegistry.get(id) != null
            ? MasterCardItem.create(ModItems.MASTER_CARD_GENERIC.get(), id.toString()) : ItemStack.EMPTY;
      }
   }

   private static ResourceLocation scoped(String namespace, String id) {
      if (id == null || !id.matches("[a-z0-9_./-]{1,128}")) return null;
      return ResourceLocation.fromNamespaceAndPath(namespace, id);
   }

   private static final class Projections implements net.xxxjk.typemoonworld.api.ProjectionRegistry {
      private final String namespace;
      private Projections(String namespace) { this.namespace = namespace; }
      public boolean registerItem(String id, net.xxxjk.typemoonworld.api.ProjectionItemExecutor executor) {
         ResourceLocation key = scoped(namespace, id); return key != null && ExtensionApiRegistry.registerProjectionItem(key, executor);
      }
      public boolean registerStructure(String id, net.xxxjk.typemoonworld.api.ProjectionStructureExecutor executor) {
         ResourceLocation key = scoped(namespace, id); return key != null && ExtensionApiRegistry.registerProjectionStructure(key, executor);
      }
      public boolean executeItem(String id, net.xxxjk.typemoonworld.api.ProjectionItemContext context) {
         ResourceLocation key = scoped(namespace, id); return key != null && context != null && ExtensionApiRegistry.projectionItem(key, context);
      }
      public boolean executeStructure(String id, net.xxxjk.typemoonworld.api.ProjectionStructureContext context) {
         ResourceLocation key = scoped(namespace, id); return key != null && context != null && ExtensionApiRegistry.projectionStructure(key, context);
      }
   }

   private static final class CommandSpells implements net.xxxjk.typemoonworld.api.CommandSpellRegistry {
      private final String namespace;
      private CommandSpells(String namespace) { this.namespace = namespace; }
      public boolean register(ResourceLocation id, net.xxxjk.typemoonworld.api.CommandSpellExecutor executor) {
         return valid(id, namespace) && ExtensionApiRegistry.registerCommand(id, executor);
      }
      public ExecutionResult execute(ResourceLocation id, net.xxxjk.typemoonworld.api.CommandSpellContext context) {
         if (!valid(id, namespace) || context == null || context.master() == null || !context.master().isAlive()) return ExecutionResult.FAILED;
         var vars = context.master().getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.master_active || vars.master_command_spells <= 0) return ExecutionResult.FAILED;
         var pre = NeoForge.EVENT_BUS.post(new net.xxxjk.typemoonworld.api.event.CommandSpellEvent.Pre(id, context));
         if (pre.isCanceled()) return ExecutionResult.FAILED;
         ExecutionResult result = ExtensionApiRegistry.command(id, context);
         if (result.handled() && result.success()) {
            int cost = Math.max(1, (int)Math.ceil(result.resourceCost()));
            if (vars.master_command_spells < cost) return ExecutionResult.FAILED;
            vars.master_command_spells -= cost;
            vars.syncPlayerVariables(context.master());
         }
         NeoForge.EVENT_BUS.post(new net.xxxjk.typemoonworld.api.event.CommandSpellEvent.Post(id, context, result));
         return result;
      }
   }

   private static final class Projectiles implements net.xxxjk.typemoonworld.api.ProjectileRegistry {
      private final String namespace;
      private Projectiles(String namespace) { this.namespace = namespace; }
      public boolean register(ResourceLocation id, net.xxxjk.typemoonworld.api.NoblePhantasmProjectileExecutor executor) {
         return valid(id, namespace) && ExtensionApiRegistry.registerProjectile(id, executor);
      }
      public boolean fire(ResourceLocation id, net.xxxjk.typemoonworld.api.NoblePhantasmProjectileContext context) {
         return valid(id, namespace) && context != null && ExtensionApiRegistry.projectile(id, context);
      }
   }

   private static final class Effects implements net.xxxjk.typemoonworld.api.EffectsRegistry {
      private final String namespace;
      private Effects(String namespace) { this.namespace = namespace; }
      public boolean registerParticle(String id, net.minecraft.core.particles.ParticleOptions value) {
         ResourceLocation key = scoped(namespace, id); return key != null && EffectsApiRegistry.particle(key, value);
      }
      public boolean registerSound(String id, net.minecraft.sounds.SoundEvent value) {
         ResourceLocation key = scoped(namespace, id); return key != null && EffectsApiRegistry.sound(key, value);
      }
      public boolean particle(ServerLevel level, String id, net.minecraft.world.phys.Vec3 pos, int count, double spread, double speed) {
         ResourceLocation key = scoped(namespace, id); var value = EffectsApiRegistry.particle(key);
         if (level == null || pos == null || value == null || count < 0 || count > 4096) return false;
         level.sendParticles(value, pos.x, pos.y, pos.z, count, Math.max(0, spread), Math.max(0, spread), Math.max(0, spread), Math.max(0, speed)); return true;
      }
      public boolean sound(ServerLevel level, String id, net.minecraft.world.phys.Vec3 pos, float volume, float pitch) {
         ResourceLocation key = scoped(namespace, id); var value = EffectsApiRegistry.sound(key);
         if (level == null || pos == null || value == null) return false;
         level.playSound(null, pos.x, pos.y, pos.z, value, net.minecraft.sounds.SoundSource.PLAYERS,
            Math.max(0, Math.min(16, volume)), Math.max(0.01F, Math.min(4, pitch))); return true;
      }
   }

   private static final class DamageTypes implements net.xxxjk.typemoonworld.api.DamageTypeRegistry {
      private final String namespace;
      private DamageTypes(String namespace) { this.namespace = namespace; }
      public net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> key(String id) {
         ResourceLocation value = scoped(namespace, id);
         if (value == null) throw new IllegalArgumentException("Invalid damage type id: " + id);
         return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, value);
      }
      public net.minecraft.world.damagesource.DamageSource source(net.minecraft.world.level.Level level, String id,
            net.minecraft.world.entity.Entity direct, net.minecraft.world.entity.Entity owner) {
         if (level == null) throw new IllegalArgumentException("level");
         var holder = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key(id));
         return direct == null ? new net.minecraft.world.damagesource.DamageSource(holder)
            : owner == null ? new net.minecraft.world.damagesource.DamageSource(holder, direct)
            : new net.minecraft.world.damagesource.DamageSource(holder, direct, owner);
      }
   }

   private static final class AiTactics implements net.xxxjk.typemoonworld.api.AiTacticsRegistry {
      private final String namespace;
      private AiTactics(String namespace) { this.namespace = namespace; }
      public boolean register(ResourceLocation id, net.xxxjk.typemoonworld.api.AiTacticProfile profile) {
         return valid(id, namespace) && ExtensionApiRegistry.registerAi(id, profile);
      }
      public boolean registerAdvanced(ResourceLocation id, net.xxxjk.typemoonworld.api.AdvancedAiTacticProfile profile) {
         return valid(id, namespace) && ExtensionApiRegistry.registerAdvancedAi(id, profile);
      }
      public net.xxxjk.typemoonworld.api.AiTacticProfile profile(ResourceLocation id) { return valid(id, namespace) ? ExtensionApiRegistry.ai(id) : null; }
      public net.xxxjk.typemoonworld.api.AdvancedAiTacticProfile advancedProfile(ResourceLocation id) {
         if (!valid(id, namespace)) return null;
         var advanced = ExtensionApiRegistry.advancedAi(id);
         if (advanced != null) return advanced;
         var base = ExtensionApiRegistry.ai(id);
         return base == null ? null : net.xxxjk.typemoonworld.api.AdvancedAiTacticProfile.compatible(base);
      }
      public java.util.List<ResourceLocation> profiles() { return ExtensionApiRegistry.aiIds().stream().filter(id -> valid(id, namespace)).toList(); }
      public net.xxxjk.typemoonworld.api.AiTactic choose(ResourceLocation id, double distance, double healthRatio, java.util.Random random) {
         var profile = profile(id); if (profile == null) return null;
         var candidates = profile.tactics().stream().filter(t -> distance >= t.minDistance() && distance <= t.maxDistance() && healthRatio >= t.minHealthRatio() && t.weight() > 0).toList();
         if (candidates.isEmpty()) return null;
         int total = candidates.stream().mapToInt(net.xxxjk.typemoonworld.api.AiTactic::weight).sum(); int roll = (random == null ? new java.util.Random() : random).nextInt(Math.max(1, total));
         for (var tactic : candidates) { roll -= tactic.weight(); if (roll < 0) return tactic; }
         return candidates.get(candidates.size() - 1);
      }
   }

   private static ServantExecutionResult toLegacy(ExecutionResult result) {
      if (result == null) return ServantExecutionResult.FAILED;
      if (!result.handled()) return ServantExecutionResult.NOT_HANDLED;
      return (result.success() ? ServantExecutionResult.SUCCESS : ServantExecutionResult.FAILED).withMpCost(result.resourceCost());
   }
}
