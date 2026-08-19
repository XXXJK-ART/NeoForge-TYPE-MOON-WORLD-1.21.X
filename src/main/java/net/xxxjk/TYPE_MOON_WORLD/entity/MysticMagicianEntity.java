package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatPersonality;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatStyle;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatTemperament;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcMagicCastBridge;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.MysticMagicianCombatController;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.MysticMagicianRank;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicLearningStrategy;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.Nullable;

public class MysticMagicianEntity extends HumanNpcEntity implements Merchant {
   private static final EntityDataAccessor<Integer> SKIN_VARIANT = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> NPC_PERSONALITY = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> NPC_COMBAT_STYLE = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> NPC_TEMPERAMENT = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> CAST_POSE_TICKS = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> MELEE_SKILL_POSE = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> MELEE_SKILL_POSE_TICKS = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> REINFORCEMENT_VISUAL_MASK = SynchedEntityData.defineId(
      MysticMagicianEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Integer> MARTIAL_MASK = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> HOKUSHIN_PROFICIENCY = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> TENNEN_PROFICIENCY = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> BAJIQUAN_PROFICIENCY = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> HAS_THOMPSON = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> RANGED_WEAPON_MODE = SynchedEntityData.defineId(MysticMagicianEntity.class, EntityDataSerializers.BOOLEAN);
   private static final int MARTIAL_HOKUSHIN = 1;
   private static final int MARTIAL_TENNEN = 1 << 1;
   private static final int MARTIAL_BAJIQUAN = 1 << 2;
   private static final String TAG_MARTIAL_INIT = "TypeMoonMagicianMartialInit";
   private static final String TAG_MARTIAL_MASK = "TypeMoonMagicianMartialMask";
   private static final String TAG_HOKUSHIN_PROFICIENCY = "TypeMoonMagicianHokushinProficiency";
   private static final String TAG_TENNEN_PROFICIENCY = "TypeMoonMagicianTennenProficiency";
   private static final String TAG_BAJIQUAN_PROFICIENCY = "TypeMoonMagicianBajiquanProficiency";
   private static final String TAG_HAS_THOMPSON = "TypeMoonMagicianHasThompson";
   private static final String TAG_DUAL_SWORD = "TypeMoonMagicianDualSword";
   private static final String TAG_SWORD_TYPE = "TypeMoonMagicianSwordType";
   private static final String TAG_MAGICIAN_RANK = "TypeMoonMagicianRank";
   private static final String TAG_BRAND_COLOR = "TypeMoonMagicianBrandColor";
   private static final String TAG_TRADE_KEYS = "TypeMoonMagicianTradeKeys";
   private static final String[] TRADE_MAGIC_IDS = new String[]{
      "detection", "aerial_stasis", "aerial_ascent", "magic_bullet", "reinforcement", "projection", "structural_analysis",
      "healing_magic", "gander", "fire_magic", "water_magic", "wind_magic", "earth_magic", "binding_magic", "suggestion_magic",
      "flame_array", "azure_water_array", "gale_wind_array", "rock_earth_array", "contract_magecraft", "touko_travel",
      "spiritual_healing", "magic_analysis", "storage", "time_alter", "jewel_magic_shoot", "jewel_magic_release",
      "jewel_machine_gun", "ruby_throw", "sapphire_throw", "emerald_use", "topaz_throw", "cyan_throw", "ruby_flame_sword",
      "sapphire_winter_frost", "emerald_winter_river", "topaz_reinforcement", "cyan_wind", "spiritron_cannon"
   };
   public static final int SKIN_VARIANT_COUNT = 6;
   public static final int MELEE_POSE_NONE = 0;
   public static final int MELEE_POSE_PUNCH = 1;
   public static final int MELEE_POSE_WHIP_KICK = 2;
   public static final int MELEE_POSE_UPPER_THROW = 3;
   public static final int MELEE_POSE_SLAM = 4;
   public static final int REINFORCEMENT_VISUAL_BODY = 1;
   public static final int REINFORCEMENT_VISUAL_ARM = 2;
   public static final int REINFORCEMENT_VISUAL_LEG = 4;
   public static final int REINFORCEMENT_VISUAL_HEAD = 8;
   private static final String INITIAL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
   private static final String NAME_SEPARATOR = "·";
   private static final int THREAT_SCAN_INTERVAL_TICKS = 5;
   private static final double THREAT_SCAN_RANGE = 24.0;
   private static final int[] FEMALE_GIVEN_NAME_INDICES = new int[]{2, 4, 7, 10, 12, 14, 17, 20, 24, 27, 29, 31, 33, 34, 37, 39, 41, 43, 46, 48};
   private static final int[] MALE_GIVEN_NAME_INDICES = new int[]{
      0, 1, 3, 5, 6, 8, 9, 11, 13, 15, 16, 18, 19, 21, 22, 23, 25, 26, 28, 30, 32, 35, 36, 38, 40, 42, 44, 45, 47, 49
   };
   private MysticMagicianRank magicianRank;
   private MysticMagicianRank.BrandColor brandColor;
   @Nullable
   private Player tradingPlayer;
   @Nullable
   private MerchantOffers offers;
   private final List<String> tradeKeys = new ArrayList<>();
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
      this(type, level, MysticMagicianRank.ADEPT);
   }

   public MysticMagicianEntity(
      EntityType<? extends PathfinderMob> type, Level level, MysticMagicianRank magicianRank
   ) {
      super(type, level);
      this.magicianRank = magicianRank == null ? MysticMagicianRank.ADEPT : magicianRank;
      this.brandColor = MysticMagicianRank.BrandColor.RED;
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, MysticMagicianCombatController.combatGoal(this));
      this.goalSelector.addGoal(1, new MysticMagicianEntity.NpcAwareMeleeAttackGoal(this, 1.18, true));
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
         .add(Attributes.FOLLOW_RANGE, 16.0)
         .add(Attributes.SCALE, NpcScaleHelper.DEFAULT_RANDOM_SCALE);
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
      builder.define(REINFORCEMENT_VISUAL_MASK, 0);
      builder.define(MARTIAL_MASK, 0);
      builder.define(HOKUSHIN_PROFICIENCY, 0.0F);
      builder.define(TENNEN_PROFICIENCY, 0.0F);
      builder.define(BAJIQUAN_PROFICIENCY, 0.0F);
      builder.define(HAS_THOMPSON, false);
      builder.define(RANGED_WEAPON_MODE, false);
   }

   public int getSkinVariant() {
      return (Integer)this.entityData.get(SKIN_VARIANT);
   }

   public void setSkinVariant(int variant) {
      this.entityData.set(SKIN_VARIANT, Mth.positiveModulo(variant, 6));
   }

   public MysticMagicianRank getMagicianRank() {
      return this.magicianRank == null ? MysticMagicianRank.ADEPT : this.magicianRank;
   }

   public void setMagicianRank(MysticMagicianRank rank) {
      this.magicianRank = rank == null ? MysticMagicianRank.ADEPT : rank;
   }

   public MysticMagicianRank.BrandColor getBrandColor() {
      return this.brandColor == null ? MysticMagicianRank.BrandColor.RED : this.brandColor;
   }

   public void setBrandColor(MysticMagicianRank.BrandColor color) {
      this.brandColor = color == null ? MysticMagicianRank.BrandColor.RED : color;
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

   public int getMartialMask() {
      return this.entityData.get(MARTIAL_MASK);
   }

   public void setMartialMask(int mask) {
      this.entityData.set(MARTIAL_MASK, mask & (MARTIAL_HOKUSHIN | MARTIAL_TENNEN | MARTIAL_BAJIQUAN));
   }

   public boolean hasMartialSchool(KendoSchool school) {
      if (school == KendoSchool.HOKUSHIN) return (getMartialMask() & MARTIAL_HOKUSHIN) != 0;
      if (school == KendoSchool.TENNEN) return (getMartialMask() & MARTIAL_TENNEN) != 0;
      return false;
   }

   public boolean hasSwordSchool() {
      return (getMartialMask() & (MARTIAL_HOKUSHIN | MARTIAL_TENNEN)) != 0;
   }

   public boolean hasBajiquan() {
      return (getMartialMask() & MARTIAL_BAJIQUAN) != 0;
   }

   public boolean hasThompson() {
      return this.entityData.get(HAS_THOMPSON);
   }

   public void setHasThompson(boolean value) {
      this.entityData.set(HAS_THOMPSON, value);
   }

   public boolean isRangedWeaponMode() {
      return this.entityData.get(RANGED_WEAPON_MODE);
   }

   public void setRangedWeaponMode(boolean value) {
      this.entityData.set(RANGED_WEAPON_MODE, value && hasThompson());
   }

   public double getMartialProficiency(KendoSchool school) {
      if (school == KendoSchool.HOKUSHIN) return this.entityData.get(HOKUSHIN_PROFICIENCY);
      if (school == KendoSchool.TENNEN) return this.entityData.get(TENNEN_PROFICIENCY);
      return 0.0;
   }

   public double getBajiquanProficiency() {
      return this.entityData.get(BAJIQUAN_PROFICIENCY);
   }

   public double getMartialProficiency() {
      return Math.max(getBajiquanProficiency(), Math.max(getMartialProficiency(KendoSchool.HOKUSHIN), getMartialProficiency(KendoSchool.TENNEN)));
   }

   public boolean hasPhysicalLoadout() {
      return getMartialMask() != 0 || hasThompson();
   }

   public void initializeMartialLoadout() {
      if (level().isClientSide() || getPersistentData().getBoolean(TAG_MARTIAL_INIT)) return;
      RandomSource random = getRandom();
      int mask = 0;
      if (random.nextInt(100) < 30) {
         int[] schools = new int[]{MARTIAL_HOKUSHIN, MARTIAL_TENNEN, MARTIAL_BAJIQUAN};
         mask |= schools[random.nextInt(schools.length)];
         while (mask != (MARTIAL_HOKUSHIN | MARTIAL_TENNEN | MARTIAL_BAJIQUAN) && random.nextInt(100) < 5) {
            int next = schools[random.nextInt(schools.length)];
            mask |= next;
         }
      }
      setMartialMask(mask);
      this.entityData.set(HOKUSHIN_PROFICIENCY, (mask & MARTIAL_HOKUSHIN) != 0 ? 20.0F + random.nextInt(81) : 0.0F);
      this.entityData.set(TENNEN_PROFICIENCY, (mask & MARTIAL_TENNEN) != 0 ? 20.0F + random.nextInt(81) : 0.0F);
      this.entityData.set(BAJIQUAN_PROFICIENCY, (mask & MARTIAL_BAJIQUAN) != 0 ? 20.0F + random.nextInt(81) : 0.0F);
      setHasThompson(random.nextInt(100) < 1);
      getPersistentData().putBoolean(TAG_DUAL_SWORD, hasSwordSchool() && random.nextInt(100) < 5);
      getPersistentData().putInt(TAG_SWORD_TYPE, random.nextInt(3));
      getPersistentData().putBoolean(TAG_MARTIAL_INIT, true);
      setRangedWeaponMode(false);
      ensurePhysicalEquipment();
   }

   public void ensurePhysicalEquipment() {
      if (level().isClientSide() || !hasPhysicalLoadout()) return;
      ItemStack sword = swordStack();
      ItemStack gun = hasThompson() ? ModItems.THOMPSON_CONTENDER.get().getDefaultInstance() : ItemStack.EMPTY;
      boolean dual = getPersistentData().getBoolean(TAG_DUAL_SWORD) && hasSwordSchool();
      if (isRangedWeaponMode() && hasThompson()) {
         setIfDifferent(EquipmentSlot.MAINHAND, gun);
         setIfDifferent(EquipmentSlot.OFFHAND, hasSwordSchool() ? sword : ItemStack.EMPTY);
      } else {
         setIfDifferent(EquipmentSlot.MAINHAND, hasSwordSchool() ? sword : gun);
         if (dual) setIfDifferent(EquipmentSlot.OFFHAND, sword);
         else if (hasSwordSchool() && hasThompson()) setIfDifferent(EquipmentSlot.OFFHAND, gun);
         else setIfDifferent(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      }
      setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      setDropChance(EquipmentSlot.OFFHAND, 0.0F);
   }

   private void setIfDifferent(EquipmentSlot slot, ItemStack stack) {
      ItemStack current = getItemBySlot(slot);
      if (!ItemStack.isSameItem(current, stack)) setItemSlot(slot, stack.copy());
   }

   private ItemStack swordStack() {
      Item sword = switch (Math.floorMod(getPersistentData().getInt(TAG_SWORD_TYPE), 3)) {
         case 0 -> ModItems.WAKIZASHI.get();
         case 1 -> ModItems.KATANA.get();
         default -> ModItems.NODACHI.get();
      };
      return new ItemStack(sword);
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

   public int getReinforcementVisualMask() {
      return (Integer)this.entityData.get(REINFORCEMENT_VISUAL_MASK);
   }

   public boolean hasReinforcementVisual(int partMask) {
      return (this.getReinforcementVisualMask() & partMask) != 0;
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
      NpcScaleHelper.ensureRandomScale(this);
      int variant = this.random.nextInt(6);
      this.setSkinVariant(variant);
      if (this.getMagicianRank() == MysticMagicianRank.BRAND && this.brandColor == MysticMagicianRank.BrandColor.RED) {
         this.setBrandColor(MysticMagicianRank.BrandColor.random(this.random));
      }
      if (!this.hasCustomName()) {
         MysticMagicianEntity.GeneratedName generated = generateRandomName(this.random, isFemaleVariant(variant));
         this.setCustomName(
            Component.translatable("entity.typemoonworld.mystic_magician.generated_name", generated.english(), generated.chinese())
         );
         this.setCustomNameVisible(true);
      }

      this.setCombatPersonality(spawnType == MobSpawnType.CONVERSION ? NpcCombatPersonality.GOOD : NpcCombatPersonality.random(this.random));
      this.setCombatTemperament(NpcCombatTemperament.random(this.random));
      NpcMagicCastBridge.onSpawnInitialized(this);
      initializeMartialLoadout();
      if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
         this.setPersistenceRequired();
      }
      return data;
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      compound.putInt("SkinVariant", this.getSkinVariant());
      compound.putString(TAG_MAGICIAN_RANK, this.getMagicianRank().id());
      compound.putString(TAG_BRAND_COLOR, this.getBrandColor().id());
      compound.putInt("NpcPersonality", this.getCombatPersonality().id());
      compound.putInt("NpcCombatStyle", this.getCombatStyle().id());
      compound.putInt("NpcTemperament", this.getCombatTemperament().id());
      compound.putBoolean(TAG_MARTIAL_INIT, getPersistentData().getBoolean(TAG_MARTIAL_INIT));
      compound.putInt(TAG_MARTIAL_MASK, getMartialMask());
      compound.putFloat(TAG_HOKUSHIN_PROFICIENCY, (float)getMartialProficiency(KendoSchool.HOKUSHIN));
      compound.putFloat(TAG_TENNEN_PROFICIENCY, (float)getMartialProficiency(KendoSchool.TENNEN));
      compound.putFloat(TAG_BAJIQUAN_PROFICIENCY, (float)getBajiquanProficiency());
      compound.putBoolean(TAG_HAS_THOMPSON, hasThompson());
      compound.putBoolean(TAG_DUAL_SWORD, getPersistentData().getBoolean(TAG_DUAL_SWORD));
      compound.putInt(TAG_SWORD_TYPE, getPersistentData().getInt(TAG_SWORD_TYPE));
      compound.putBoolean("TypeMoonMagicianRangedWeaponMode", isRangedWeaponMode());
      ListTag tradeKeyTags = new ListTag();
      for (String key : this.tradeKeys) {
         tradeKeyTags.add(StringTag.valueOf(key));
      }
      compound.put(TAG_TRADE_KEYS, tradeKeyTags);
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      this.setSkinVariant(compound.getInt("SkinVariant"));
      if (compound.contains(TAG_MAGICIAN_RANK)) {
         this.setMagicianRank(MysticMagicianRank.fromId(compound.getString(TAG_MAGICIAN_RANK)));
      }
      if (compound.contains(TAG_BRAND_COLOR)) {
         this.setBrandColor(MysticMagicianRank.BrandColor.fromId(compound.getString(TAG_BRAND_COLOR)));
      }
      if (compound.contains("NpcPersonality")) {
         this.setCombatPersonality(NpcCombatPersonality.fromId(compound.getInt("NpcPersonality")));
      }

      if (compound.contains("NpcCombatStyle")) {
         this.setCombatStyle(NpcCombatStyle.fromId(compound.getInt("NpcCombatStyle")));
      }

      if (compound.contains("NpcTemperament")) {
         this.setCombatTemperament(NpcCombatTemperament.fromId(compound.getInt("NpcTemperament")));
      }
      if (compound.contains(TAG_MARTIAL_MASK)) {
         setMartialMask(compound.getInt(TAG_MARTIAL_MASK));
         this.entityData.set(HOKUSHIN_PROFICIENCY, compound.getFloat(TAG_HOKUSHIN_PROFICIENCY));
         this.entityData.set(TENNEN_PROFICIENCY, compound.getFloat(TAG_TENNEN_PROFICIENCY));
         this.entityData.set(BAJIQUAN_PROFICIENCY, compound.getFloat(TAG_BAJIQUAN_PROFICIENCY));
         setHasThompson(compound.getBoolean(TAG_HAS_THOMPSON));
         getPersistentData().putBoolean(TAG_MARTIAL_INIT, compound.getBoolean(TAG_MARTIAL_INIT));
         getPersistentData().putBoolean(TAG_DUAL_SWORD, compound.getBoolean(TAG_DUAL_SWORD));
         getPersistentData().putInt(TAG_SWORD_TYPE, compound.getInt(TAG_SWORD_TYPE));
         setRangedWeaponMode(compound.getBoolean("TypeMoonMagicianRangedWeaponMode"));
      }
      this.tradeKeys.clear();
      if (compound.contains(TAG_TRADE_KEYS, Tag.TAG_LIST)) {
         ListTag tradeKeyTags = compound.getList(TAG_TRADE_KEYS, Tag.TAG_STRING);
         for (int i = 0; i < tradeKeyTags.size(); i++) {
            String key = tradeKeyTags.getString(i);
            if (!key.isBlank() && !this.tradeKeys.contains(key)) {
               this.tradeKeys.add(key);
            }
         }
      }
      this.offers = null;
   }

   @Override
   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (player != null && !this.level().isClientSide() && !player.isSpectator() && this.canTradeWithPlayer(player)) {
         this.setTradingPlayer(player);
         MerchantOffers offers = this.getOffers();
         if (!offers.isEmpty()) {
            this.openTradingScreen(player, this.getDisplayName(), this.getMagicianTradeLevel());
            return InteractionResult.CONSUME;
         }
      }
      return super.mobInteract(player, hand);
   }

   @Override
   public void setTradingPlayer(@Nullable Player tradingPlayer) {
      this.tradingPlayer = tradingPlayer;
   }

   @Override
   public Player getTradingPlayer() {
      return this.tradingPlayer;
   }

   @Override
   public MerchantOffers getOffers() {
      if (this.level().isClientSide()) {
         throw new IllegalStateException("Cannot load Mystic Magician offers on the client");
      }
      if (this.offers == null) {
         this.offers = new MerchantOffers();
         rebuildOffers();
      }
      ensureTradePool();
      return this.offers;
   }

   @Override
   public void overrideOffers(MerchantOffers offers) {
      this.offers = offers == null ? new MerchantOffers() : offers;
   }

   @Override
   public void notifyTrade(MerchantOffer offer) {
      if (offer == null) {
         return;
      }
      offer.increaseUses();
      this.ambientSoundTime = -this.getAmbientSoundInterval();
   }

   @Override
   public void notifyTradeUpdated(ItemStack stack) {
      if (!this.level().isClientSide() && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
         this.ambientSoundTime = -this.getAmbientSoundInterval();
         this.playSound(stack.isEmpty() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_YES, 0.8F, 1.0F);
      }
   }

   @Override
   public int getVillagerXp() {
      return 0;
   }

   @Override
   public void overrideXp(int xp) {
   }

   @Override
   public boolean showProgressBar() {
      return false;
   }

   @Override
   public SoundEvent getNotifyTradeSound() {
      return SoundEvents.VILLAGER_YES;
   }

   @Override
   public boolean canRestock() {
      return false;
   }

   @Override
   public boolean isClientSide() {
      return this.level().isClientSide();
   }

   private void rebuildOffers() {
      this.offers.clear();
      for (String key : this.tradeKeys) {
         MerchantOffer offer = this.createOfferFromKey(key);
         if (offer != null) {
            this.offers.add(offer);
         }
      }
   }

   private void ensureTradePool() {
      if (this.offers == null) {
         this.offers = new MerchantOffers();
      }
      int desired = this.desiredTradeCount();
      this.tradeKeys.removeIf(key -> !key.startsWith("page:") || this.createOfferFromKey(key) == null);
      while (this.tradeKeys.size() > desired) {
         this.tradeKeys.remove(this.tradeKeys.size() - 1);
      }
      while (this.tradeKeys.size() < desired) {
         String key = this.createNextTradeKey();
         if (key == null) {
            break;
         }
         this.tradeKeys.add(key);
         MerchantOffer offer = this.createOfferFromKey(key);
         if (offer != null) {
            this.offers.add(offer);
         }
      }
      if (this.offers.size() != this.tradeKeys.size()) {
         rebuildOffers();
      }
   }

   private int desiredTradeCount() {
      return switch (this.getMagicianRank()) {
         case FRAME -> 2;
         case UMNOS -> 3;
         case ADEPT -> 4;
         case FES, PRIDE -> 5;
         case BRAND -> 6;
         case GRAND -> 7;
      };
   }

   private int getMagicianTradeLevel() {
      return this.desiredTradeCount();
   }

   private boolean canTradeWithPlayer(Player player) {
      return player != null && this.getCombatPersonality() == NpcCombatPersonality.GOOD;
   }

   private List<String> getTradeMagicPool() {
      List<String> pool = new ArrayList<>();
      TypeMoonWorldModVariables.PlayerVariables vars = this.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      List<String> learned = vars == null ? List.of() : vars.learned_magics;
      List<String> source = learned.isEmpty() ? List.of(TRADE_MAGIC_IDS) : learned;
      for (String magicId : source) {
         if (this.isTradeMagicAvailable(magicId) && !pool.contains(magicId)) {
            pool.add(magicId);
         }
      }
      return pool;
   }

   private boolean isTradeMagicAvailable(String magicId) {
      if (magicId == null || magicId.isBlank()) {
         return false;
      }
      ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath("typemoonworld", MagicLearningStrategy.pageItemPath(magicId));
      return BuiltInRegistries.ITEM.get(itemId) != Items.AIR;
   }

   private String createNextTradeKey() {
      List<String> candidates = this.buildPageTradeCandidates();
      if (candidates.isEmpty()) {
         return null;
      }
      return candidates.get(this.random.nextInt(candidates.size()));
   }

   private List<String> buildPageTradeCandidates() {
      List<String> candidates = new ArrayList<>();
      for (String magicId : this.getTradeMagicPool()) {
         if (!this.tradeKeys.contains("page:" + magicId)) {
            candidates.add("page:" + magicId);
         }
      }
      return candidates;
   }

   private MerchantOffer createOfferFromKey(String key) {
      if (key == null || key.isBlank()) {
         return null;
      }
      if (key.startsWith("page:")) {
         return this.createPageOffer(key.substring("page:".length()));
      }
      return null;
   }

   private MerchantOffer createPageOffer(String magicId) {
      if (magicId == null || magicId.isBlank()) {
         return null;
      }
      Item pageItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("typemoonworld", MagicLearningStrategy.pageItemPath(magicId)));
      if (pageItem == Items.AIR) {
         return null;
      }
      int emeraldCost = Math.max(3, Math.min(64, 2 + (int)Math.ceil(MagicLearningStrategy.complexity(magicId) / 4.0)));
      return new MerchantOffer(cost(new ItemStack(Items.EMERALD, emeraldCost)), new ItemStack(pageItem), 8, 2, 0.05F);
   }

   private static ItemCost cost(ItemStack stack) {
      return new ItemCost(stack.getItemHolder(), stack.getCount(), DataComponentPredicate.EMPTY, stack);
   }

   protected void customServerAiStep() {
      boolean tactical = net.xxxjk.TYPE_MOON_WORLD.combat.ai.NpcTacticalController.tick(this);
      if (!tactical) super.customServerAiStep();
      NpcScaleHelper.ensureRandomScale(this);
      initializeMartialLoadout();
      ensurePhysicalEquipment();
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

      if (!tactical && !this.level().isClientSide() && this.tickCount % 5 == 0) {
         this.acquireAggressorTarget();
      }

      if (!tactical) NpcMagicCastBridge.tickServer(this);
      this.refreshReinforcementVisualMask();
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

   private static MysticMagicianEntity.GeneratedName generateRandomName(RandomSource random, boolean femaleVariant) {
      return switch (random.nextInt(3)) {
         case 1 -> generateRandomChineseName(random, femaleVariant);
         case 2 -> generateRandomJapaneseName(random, femaleVariant);
         default -> generateRandomEuropeanName(random, femaleVariant);
      };
   }

   public static String generateChurchNameChinese(RandomSource random, boolean femaleVariant) {
      GeneratedName generated = random.nextBoolean()
         ? generateRandomJapaneseName(random, femaleVariant)
         : generateRandomEuropeanName(random, femaleVariant);
      return generated.chinese();
   }

   public static String generateCulturalNameChinese(RandomSource random, int culture, boolean femaleVariant) {
      return switch (Math.floorMod(culture, 3)) {
         case 0 -> generateRandomChineseName(random, femaleVariant).chinese();
         case 1 -> generateRandomJapaneseName(random, femaleVariant).chinese();
         default -> {
            int firstNameIndex = pickGivenNameIndex(random, femaleVariant);
            int surnameIndex = random.nextInt(EUROPEAN_SURNAMES_ZH.length);
            yield EUROPEAN_GIVEN_NAMES_ZH[firstNameIndex] + NAME_SEPARATOR + EUROPEAN_SURNAMES_ZH[surnameIndex];
         }
      };
   }

   private static MysticMagicianEntity.GeneratedName generateRandomChineseName(RandomSource random, boolean femaleVariant) {
      String name = ChineseNpcNameGenerator.apprentice(random, femaleVariant);
      return new MysticMagicianEntity.GeneratedName(name, name);
   }

   private static MysticMagicianEntity.GeneratedName generateRandomJapaneseName(RandomSource random, boolean femaleVariant) {
      String name = JapaneseNpcNameGenerator.apprentice(random, femaleVariant);
      return new MysticMagicianEntity.GeneratedName(name, name);
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

   private void refreshReinforcementVisualMask() {
      if (!this.level().isClientSide()) {
         int mask = 0;
         if (this.hasEffect(ModMobEffects.REINFORCEMENT_SELF_DEFENSE) || this.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_DEFENSE)) {
            mask |= REINFORCEMENT_VISUAL_BODY;
         }

         if (this.hasEffect(ModMobEffects.REINFORCEMENT_SELF_STRENGTH) || this.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_STRENGTH)) {
            mask |= REINFORCEMENT_VISUAL_ARM;
         }

         if (this.hasEffect(ModMobEffects.REINFORCEMENT_SELF_AGILITY) || this.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_AGILITY)) {
            mask |= REINFORCEMENT_VISUAL_LEG;
         }

         if (this.hasEffect(ModMobEffects.REINFORCEMENT_SELF_SIGHT) || this.hasEffect(ModMobEffects.REINFORCEMENT_OTHER_SIGHT)) {
            mask |= REINFORCEMENT_VISUAL_HEAD;
         }

         if (mask != (Integer)this.entityData.get(REINFORCEMENT_VISUAL_MASK)) {
            this.entityData.set(REINFORCEMENT_VISUAL_MASK, mask);
         }
      }
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
         return !this.magician.hasPhysicalLoadout() && NpcMagicCastBridge.shouldUseMeleeGoal(this.magician) && super.canUse();
      }

      public boolean canContinueToUse() {
         return !this.magician.hasPhysicalLoadout() && NpcMagicCastBridge.shouldUseMeleeGoal(this.magician) && super.canContinueToUse();
      }
   }
}
