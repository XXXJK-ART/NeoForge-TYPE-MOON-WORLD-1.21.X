package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatPersonality;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatStyle;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatTemperament;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcMagicCastBridge;
import org.jetbrains.annotations.Nullable;

public class MysticMagicianEntity extends PathfinderMob {
   private static final EntityDataAccessor<Integer> SKIN_VARIANT = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> NPC_PERSONALITY = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> NPC_COMBAT_STYLE = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> NPC_TEMPERAMENT = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> CAST_POSE_TICKS = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> MELEE_SKILL_POSE = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> MELEE_SKILL_POSE_TICKS = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   public static final int SKIN_VARIANT_COUNT = 6;
   public static final int MELEE_POSE_NONE = 0;
   public static final int MELEE_POSE_PUNCH = 1;
   public static final int MELEE_POSE_WHIP_KICK = 2;
   public static final int MELEE_POSE_UPPER_THROW = 3;
   public static final int MELEE_POSE_SLAM = 4;
   private static final String INITIAL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
   private static final String NAME_SEPARATOR = "·";
   private static final int THREAT_SCAN_INTERVAL_TICKS = 5;
   private static final double THREAT_SCAN_RANGE = 24.0;
   private static final int[] FEMALE_GIVEN_NAME_INDICES = new int[]{2, 4, 7, 10, 12, 14, 17, 20, 24, 27, 29, 31, 33, 34, 37, 39, 41, 43, 46, 48};
   private static final int[] MALE_GIVEN_NAME_INDICES = new int[]{
      0, 1, 3, 5, 6, 8, 9, 11, 13, 15, 16, 18, 19, 21, 22, 23, 25, 26, 28, 30, 32, 35, 36, 38, 40, 42, 44, 45, 47, 49
   };
   private static final String[] EUROPEAN_GIVEN_NAMES = new String[]{
      "Alexander",
      "Benjamin",
      "Charlotte",
      "Daniel",
      "Eleanor",
      "Felix",
      "Gabriel",
      "Helena",
      "Isaac",
      "Julian",
      "Katherine",
      "Lucas",
      "Madeleine",
      "Nathan",
      "Olivia",
      "Patrick",
      "Quentin",
      "Rebecca",
      "Sebastian",
      "Theodore",
      "Ursula",
      "Victor",
      "William",
      "Xavier",
      "Yvonne",
      "Zachary",
      "Adrian",
      "Bianca",
      "Cedric",
      "Daphne",
      "Edgar",
      "Florence",
      "Gregory",
      "Hazel",
      "Ingrid",
      "Jasper",
      "Leon",
      "Monica",
      "Nicolas",
      "Ophelia",
      "Peter",
      "Rosalie",
      "Stefan",
      "Theresa",
      "Vincent",
      "Walter",
      "Cecilia",
      "Dominic",
      "Emilia",
      "Frederik"
   };
   private static final String[] EUROPEAN_GIVEN_NAMES_ZH = new String[]{
      "亚历山大",
      "本杰明",
      "夏洛特",
      "丹尼尔",
      "埃莉诺",
      "菲利克斯",
      "加布里埃尔",
      "海伦娜",
      "艾萨克",
      "朱利安",
      "凯瑟琳",
      "卢卡斯",
      "玛德琳",
      "内森",
      "奥利维亚",
      "帕特里克",
      "昆廷",
      "丽贝卡",
      "塞巴斯蒂安",
      "西奥多",
      "乌苏拉",
      "维克托",
      "威廉",
      "泽维尔",
      "伊冯",
      "扎卡里",
      "阿德里安",
      "比安卡",
      "塞德里克",
      "达芙妮",
      "埃德加",
      "弗洛伦丝",
      "格里高利",
      "海泽尔",
      "英格丽德",
      "贾斯珀",
      "利昂",
      "莫妮卡",
      "尼古拉斯",
      "奥菲莉娅",
      "彼得",
      "罗莎莉",
      "斯特凡",
      "特蕾莎",
      "文森特",
      "沃尔特",
      "塞西莉亚",
      "多米尼克",
      "艾米莉亚",
      "弗雷德里克"
   };
   private static final String[] EUROPEAN_SURNAMES = new String[]{
      "Anderson",
      "Bennett",
      "Clarke",
      "Donovan",
      "Ellis",
      "Fischer",
      "Grayson",
      "Hartmann",
      "Iverson",
      "Jensen",
      "Keller",
      "Laurent",
      "Muller",
      "Novak",
      "OConnell",
      "Petrov",
      "Quintana",
      "Rossi",
      "Schneider",
      "Turner",
      "Ulrich",
      "Varga",
      "Whitaker",
      "Xander",
      "Young",
      "Zimmerman",
      "Albrecht",
      "Bouchard",
      "Caruso",
      "Dubois",
      "Eklund",
      "Fontaine",
      "Gallo",
      "Hawthorne",
      "Ivanov",
      "Kovacs",
      "Lindberg",
      "Moretti",
      "Nielsen",
      "Ortega",
      "Pereira",
      "Rinaldi",
      "Sokolov",
      "Thompson",
      "Urbanski",
      "Valentin",
      "Weiss",
      "Yilmaz",
      "Zamora",
      "Conti"
   };
   private static final String[] EUROPEAN_SURNAMES_ZH = new String[]{
      "安德森",
      "贝内特",
      "克拉克",
      "多诺万",
      "埃利斯",
      "菲舍尔",
      "格雷森",
      "哈特曼",
      "艾弗森",
      "延森",
      "凯勒",
      "洛朗",
      "穆勒",
      "诺瓦克",
      "奥康奈尔",
      "彼得罗夫",
      "金塔纳",
      "罗西",
      "施耐德",
      "特纳",
      "乌尔里希",
      "瓦尔加",
      "惠特克",
      "赞德",
      "杨",
      "齐默尔曼",
      "阿尔布雷希特",
      "布沙尔",
      "卡鲁索",
      "杜布瓦",
      "埃克隆德",
      "丰泰纳",
      "加洛",
      "霍桑",
      "伊万诺夫",
      "科瓦奇",
      "林德伯格",
      "莫雷蒂",
      "尼尔森",
      "奥尔特加",
      "佩雷拉",
      "里纳尔迪",
      "索科洛夫",
      "汤普森",
      "乌尔班斯基",
      "瓦伦丁",
      "魏斯",
      "伊尔马兹",
      "萨莫拉",
      "孔蒂"
   };

   public MysticMagicianEntity(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MysticMagicianEntity.NpcAwareMeleeAttackGoal(this, 1.05, false));
      this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.targetSelector
         .addGoal(
            2,
            new NearestAttackableTargetGoal<>(
               this, LivingEntity.class, 8, true, false, this::canTargetLiving
            )
         );
   }

   public static Builder createAttributes() {
      return createMobAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.MOVEMENT_SPEED, 0.25)
         .add(Attributes.ATTACK_DAMAGE, 2.0)
         .add(Attributes.FOLLOW_RANGE, 16.0);
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SKIN_VARIANT, 0);
      builder.define(NPC_PERSONALITY, NpcCombatPersonality.NEUTRAL.id());
      builder.define(NPC_COMBAT_STYLE, NpcCombatStyle.BALANCED.id());
      builder.define(NPC_TEMPERAMENT, NpcCombatTemperament.STEADY.id());
      builder.define(CAST_POSE_TICKS, 0);
      builder.define(MELEE_SKILL_POSE, MELEE_POSE_NONE);
      builder.define(MELEE_SKILL_POSE_TICKS, 0);
   }

   public int getSkinVariant() {
      return (Integer)this.entityData.get(SKIN_VARIANT);
   }

   public void setSkinVariant(int variant) {
      this.entityData.set(SKIN_VARIANT, Mth.positiveModulo(variant, 6));
   }

   public NpcCombatPersonality getCombatPersonality() {
      return NpcCombatPersonality.fromId((Integer)this.entityData.get(NPC_PERSONALITY));
   }

   public void setCombatPersonality(NpcCombatPersonality personality) {
      NpcCombatPersonality resolved = personality == null ? NpcCombatPersonality.NEUTRAL : personality;
      this.entityData.set(NPC_PERSONALITY, resolved.id());
   }

   public NpcCombatStyle getCombatStyle() {
      return NpcCombatStyle.fromId((Integer)this.entityData.get(NPC_COMBAT_STYLE));
   }

   public void setCombatStyle(NpcCombatStyle style) {
      NpcCombatStyle resolved = style == null ? NpcCombatStyle.BALANCED : style;
      this.entityData.set(NPC_COMBAT_STYLE, resolved.id());
   }

   public NpcCombatTemperament getCombatTemperament() {
      return NpcCombatTemperament.fromId((Integer)this.entityData.get(NPC_TEMPERAMENT));
   }

   public void setCombatTemperament(NpcCombatTemperament temperament) {
      NpcCombatTemperament resolved = temperament == null ? NpcCombatTemperament.STEADY : temperament;
      this.entityData.set(NPC_TEMPERAMENT, resolved.id());
   }

   public boolean isCastingPoseActive() {
      return (Integer)this.entityData.get(CAST_POSE_TICKS) > 0;
   }

   public void triggerCastingPose(int ticks) {
      if (!this.level().isClientSide()) {
         int clamped = Mth.clamp(ticks, 0, 40);
         if (clamped > 0) {
            int current = (Integer)this.entityData.get(CAST_POSE_TICKS);
            if (clamped > current) {
               this.entityData.set(CAST_POSE_TICKS, clamped);
            }
         }
      }
   }

   public boolean isMeleeSkillPoseActive() {
      return (Integer)this.entityData.get(MELEE_SKILL_POSE_TICKS) > 0 && (Integer)this.entityData.get(MELEE_SKILL_POSE) != MELEE_POSE_NONE;
   }

   public int getMeleeSkillPose() {
      return (Integer)this.entityData.get(MELEE_SKILL_POSE);
   }

   public void triggerMeleeSkillPose(int pose, int ticks) {
      if (!this.level().isClientSide()) {
         int clampedPose = Mth.clamp(pose, MELEE_POSE_NONE, MELEE_POSE_SLAM);
         int clampedTicks = Mth.clamp(ticks, 0, 30);
         if (clampedPose == MELEE_POSE_NONE || clampedTicks <= 0) {
            this.entityData.set(MELEE_SKILL_POSE, MELEE_POSE_NONE);
            this.entityData.set(MELEE_SKILL_POSE_TICKS, 0);
         } else {
            this.entityData.set(MELEE_SKILL_POSE, clampedPose);
            this.entityData.set(MELEE_SKILL_POSE_TICKS, clampedTicks);
         }
      }
   }

   public static boolean isFemaleVariant(int variant) {
      int normalized = Mth.positiveModulo(variant, 6);
      return normalized == 1 || normalized == 3 || normalized == 5;
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData
   ) {
      SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
      int variant = this.random.nextInt(6);
      this.setSkinVariant(variant);
      if (!this.hasCustomName()) {
         MysticMagicianEntity.GeneratedName generated = generateRandomEuropeanName(this.random, isFemaleVariant(variant));
         this.setCustomName(
            Component.translatable("entity.typemoonworld.mystic_magician.generated_name", generated.english(), generated.chinese())
         );
         this.setCustomNameVisible(true);
      }

      this.setCombatPersonality(NpcCombatPersonality.random(this.random));
      this.setCombatTemperament(NpcCombatTemperament.random(this.random));
      NpcMagicCastBridge.onSpawnInitialized(this);
      this.setPersistenceRequired();
      return data;
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putInt("SkinVariant", this.getSkinVariant());
      compound.putInt("NpcPersonality", this.getCombatPersonality().id());
      compound.putInt("NpcCombatStyle", this.getCombatStyle().id());
      compound.putInt("NpcTemperament", this.getCombatTemperament().id());
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      this.setSkinVariant(compound.getInt("SkinVariant"));
      if (compound.contains("NpcPersonality")) {
         this.setCombatPersonality(NpcCombatPersonality.fromId(compound.getInt("NpcPersonality")));
      }

      if (compound.contains("NpcCombatStyle")) {
         this.setCombatStyle(NpcCombatStyle.fromId(compound.getInt("NpcCombatStyle")));
      }

      if (compound.contains("NpcTemperament")) {
         this.setCombatTemperament(NpcCombatTemperament.fromId(compound.getInt("NpcTemperament")));
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      if ((Integer)this.entityData.get(CAST_POSE_TICKS) > 0) {
         this.entityData.set(CAST_POSE_TICKS, (Integer)this.entityData.get(CAST_POSE_TICKS) - 1);
      }

      if ((Integer)this.entityData.get(MELEE_SKILL_POSE_TICKS) > 0) {
         int remaining = (Integer)this.entityData.get(MELEE_SKILL_POSE_TICKS) - 1;
         if (remaining <= 0) {
            this.entityData.set(MELEE_SKILL_POSE_TICKS, 0);
            this.entityData.set(MELEE_SKILL_POSE, MELEE_POSE_NONE);
         } else {
            this.entityData.set(MELEE_SKILL_POSE_TICKS, remaining);
         }
      }

      if (!this.level().isClientSide() && this.tickCount % 5 == 0) {
         this.acquireAggressorTarget();
      }

      NpcMagicCastBridge.tickServer(this);
   }

   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide()) {
         NpcMagicCastBridge.cleanup(this);
      }

      super.remove(reason);
   }

   private boolean canTargetMonster(Monster monster) {
      if (monster != null && monster.isAlive()) {
         NpcCombatPersonality personality = this.getCombatPersonality();
         return personality == NpcCombatPersonality.GOOD || personality == NpcCombatPersonality.EVIL;
      } else {
         return false;
      }
   }

   private boolean canTargetPlayer(Player player) {
      return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
         ? this.getCombatPersonality() == NpcCombatPersonality.EVIL
         : false;
   }

   private boolean canTargetAnimal(Animal animal) {
      return animal != null && animal.isAlive() ? this.getCombatPersonality() == NpcCombatPersonality.EVIL : false;
   }

   private boolean canTargetLiving(LivingEntity living) {
      if (!NpcMagicCastBridge.isNpcTargetCandidate(this, living)) {
         return false;
      } else if (living instanceof Player player) {
         if (player.isCreative() || player.isSpectator()) {
            return false;
         }

         return this.getCombatPersonality() == NpcCombatPersonality.EVIL;
      } else if (this.getCombatPersonality() == NpcCombatPersonality.EVIL) {
         // Evil personality proactively attacks all living entities (not only players/monsters/animals).
         return true;
      } else if (living instanceof Monster monster) {
         return this.canTargetMonster(monster);
      } else {
         return living instanceof Animal animal && this.canTargetAnimal(animal);
      }
   }

   private void acquireAggressorTarget() {
      LivingEntity current = this.getTarget();
      if (current == null || !current.isAlive()) {
         double range = Math.max(18.0, this.getAttributeValue(Attributes.FOLLOW_RANGE) + 2.0);
         Monster nearestThreat = null;
         double bestDist = Double.MAX_VALUE;

         for (Monster monster : this.level()
            .getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(range), m -> m.isAlive() && m.getTarget() == this && NpcMagicCastBridge.isNpcTargetCandidate(this, m))) {
            double dist = this.distanceToSqr(monster);
            if (dist < bestDist) {
               bestDist = dist;
               nearestThreat = monster;
            }
         }

         if (nearestThreat != null) {
            this.setTarget(nearestThreat);
         }
      }
   }

   private static MysticMagicianEntity.GeneratedName generateRandomEuropeanName(RandomSource random, boolean femaleVariant) {
      int firstNameIndex = pickGivenNameIndex(random, femaleVariant);
      int surnameIndex = random.nextInt(EUROPEAN_SURNAMES.length);
      String firstName = EUROPEAN_GIVEN_NAMES[firstNameIndex];
      String surname = EUROPEAN_SURNAMES[surnameIndex];
      String firstNameZh = EUROPEAN_GIVEN_NAMES_ZH[firstNameIndex];
      String surnameZh = EUROPEAN_SURNAMES_ZH[surnameIndex];
      boolean useThreePartName = random.nextBoolean();
      if (!useThreePartName) {
         return new MysticMagicianEntity.GeneratedName(firstName + "·" + surname, firstNameZh + "·" + surnameZh);
      } else {
         String initials = generateMiddleInitial(random);
         return new MysticMagicianEntity.GeneratedName(firstName + "·" + initials + "·" + surname, firstNameZh + "·" + initials + "·" + surnameZh);
      }
   }

   private static int pickGivenNameIndex(RandomSource random, boolean femaleVariant) {
      int[] pool = femaleVariant ? FEMALE_GIVEN_NAME_INDICES : MALE_GIVEN_NAME_INDICES;
      return pool[random.nextInt(pool.length)];
   }

   private static String generateMiddleInitial(RandomSource random) {
      char initial = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZ".length()));
      return String.valueOf(initial);
   }

   private record GeneratedName(String english, String chinese) {
   }

   private static final class NpcAwareMeleeAttackGoal extends MeleeAttackGoal {
      private final MysticMagicianEntity magician;

      private NpcAwareMeleeAttackGoal(MysticMagicianEntity magician, double speedModifier, boolean followingTargetEvenIfNotSeen) {
         super(magician, speedModifier, followingTargetEvenIfNotSeen);
         this.magician = magician;
      }

      public boolean canUse() {
         return NpcMagicCastBridge.shouldUseMeleeGoal(this.magician) && super.canUse();
      }

      public boolean canContinueToUse() {
         return NpcMagicCastBridge.shouldUseMeleeGoal(this.magician) && super.canContinueToUse();
      }
   }
}
