package net.xxxjk.TYPE_MOON_WORLD.magic.rune;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

/** Fixed Elder Futhark catalogue and the authoritative rune effect table. */
public final class RuneRegistry {
   private static final Map<String, RuneDefinition> DEFINITIONS = createDefinitions();
   private static final Map<String, String[]> ACTION_KEYS = createActionKeys();
   private RuneRegistry() { }
   public static RuneDefinition get(String id) {
      if (id == null) return null;
      String path = id.startsWith(TYPE_MOON_WORLD.MOD_ID + ":") ? id.substring(TYPE_MOON_WORLD.MOD_ID.length() + 1) : id;
      return DEFINITIONS.get(path);
   }
   public static boolean isKnown(String id) { return get(id) != null; }
   public static List<RuneDefinition> all() { return List.copyOf(DEFINITIONS.values()); }
   public static Set<String> ids() { return Collections.unmodifiableSet(DEFINITIONS.keySet()); }
   public static String actionKey(String id, RunePosition position) {
      String normalized = id == null ? "" : id;
      if (normalized.startsWith(TYPE_MOON_WORLD.MOD_ID + ":")) normalized = normalized.substring(TYPE_MOON_WORLD.MOD_ID.length() + 1);
      String[] keys = ACTION_KEYS.get(normalized);
      return keys == null || position == null ? "" : keys[position.ordinal()];
   }
   private static RuneEffectSpec s(String n, String d, String v, double p, double q, double r, int t, String u) { return new RuneEffectSpec(n, d, v, p, q, r, t, u); }

   private static Map<String, String[]> createActionKeys() {
      String[][] values = {
         {"materialize","life_drain","split","harvest"},{"charge","strength_amplify","pierce","wild"},{"lightning","lightning_addon","volley","thunderstorm"},{"mind_blast","mind_addon","guidance","revelation"},
         {"teleport","haste","ricochet","portal"},{"fireball","fire_addon","delay","embers"},{"life_link","connection","chain","share"},{"inspire","light_addon","empower","aura"},
         {"ice_spike","frost_addon","delay","ice_blast"},{"shadow_chain","shadow_addon","gravity","curse_residue"},{"freeze_aura","stasis","freeze_behavior","ice_prison"},{"vine_bind","nature_addon","cycle","overgrowth"},
         {"defense_barrier","protection_addon","rebound","rewind"},{"fate_dice","random_addon","random_modifier","unknown"},{"guardian_shield","sanctuary_addon","sanctuary","bulwark"},{"beam","light_addon","acceleration","cleanse"},
         {"sure_strike","sharpness","precision","judgment"},{"healing_wave","healing_addon","renewal","flourishing_field"},{"teleport_15","transfer","synchronize","harmony"},{"mirror_clone","self_addon","mirror","fusion"},
         {"water_impact","water_addon","liquid","tide"},{"nature_enchant","earth_addon","storage","germination"},{"dawn","cleanse_addon","cleanse","dawn_field"},{"ancestor_summon","soul_addon","territory","territory_arrival"}
      };
      String[] ids = {"fehu","uruz","thurisaz","ansuz","raidho","kenaz","gebo","wunjo","hagalaz","nauthiz","isa","jera","eihwaz","perthro","algiz","sowilo","tiwaz","berkano","ehwaz","mannaz","laguz","ingwaz","dagaz","othala"};
      Map<String, String[]> out = new LinkedHashMap<>();
      for (int i = 0; i < ids.length; i++) out.put(ids[i], values[i]);
      return Collections.unmodifiableMap(out);
   }

   private static Map<String, RuneDefinition> createDefinitions() {
      String[] ids = {"fehu","uruz","thurisaz","ansuz","raidho","kenaz","gebo","wunjo","hagalaz","nauthiz","isa","jera","eihwaz","perthro","algiz","sowilo","tiwaz","berkano","ehwaz","mannaz","laguz","ingwaz","dagaz","othala"};
      String[] names = {"Fehu","Uruz","Thurisaz","Ansuz","Raidho","Kenaz","Gebo","Wunjo","Hagalaz","Nauthiz","Isa","Jera","Eihwaz","Perthro","Algiz","Sowilo","Tiwaz","Berkano","Ehwaz","Mannaz","Laguz","Ingwaz","Dagaz","Othala"};
      // Stable semantic keys preserve saved programs and addon compatibility.
      String[][] k = {
         {"wealth","create","amplify","release"},{"strength","reinforce","power","strike"},{"break","impact","shatter","detonate"},{"command","speak","range","seal"},
         {"move","path","swift","return"},{"ignite","fire","heat","burn"},{"bind","share","link","transfer"},{"joy","heal","duration","bless"},
         {"storm","ice","area","disperse"},{"need","drain","cost","sacrifice"},{"freeze","barrier","slow","endure"},{"cycle","harvest","repeat","complete"},
         {"ward","shadow","resist","banish"},{"chance","random","luck","reveal"},{"protect","shield","guard","protect"},{"shine","light","accuracy","purify"},
         {"judge","pierce","critical","execute"},{"grow","nature","restore","renew"},{"travel","speed","mobility","escape"},{"self","mind","focus","awaken"},
         {"flow","water","control","dissolve"},{"charge","energy","store","discharge"},{"transform","change","fuse","renew"},{"inherit","earth","persist","anchor"}
      };
      int[] colors = {0xFFFFC857,0xFFE53935,0xFFB56BFF,0xFFC9A7FF,0xFFEAF4FF,0xFFFF6B20,0xFF55E38E,0xFFFFD84D,0xFFEAF7FF,0xFF32123F,0xFFF5FCFF,0xFF58D68D,0xFFBFE8FF,0xFFEC78D0,0xFFB7D9E8,0xFFFFF1A8,0xFFFFD34E,0xFF58D68D,0xFFEAF4FF,0xFFC9A7FF,0xFF4DB8FF,0xFF68D391,0xFFFFE07A,0xFFFFD84D};
      RuneEffectSpec[][] x = {
         {s("物质具现","朝朝向发射基础魔力弹（伤害20，飞行50格）","金色粒子拖尾，半透明弹体",20,50,0,0,"伤害/格"),s("生命汲取","造成伤害时恢复自身生命，恢复量为伤害的30%","红色粒子从目标飞向施法者",.30,0,0,200,"比例"),s("增殖","投射物分裂为2个，分裂后伤害降低20%","金色闪光一分为二",2,.20,0,0,"数量/减伤"),s("丰收","命中后额外掉落资源，掉落率提高200%","金色爆裂并掉出物品",2,0,0,0,"倍率")},
         {s("野蛮冲撞","向朝向猛冲20格并击飞路径敌人5格","红色残影与扩散冲击波",20,5,0,0,"格"),s("力量增幅","附加物理伤害加成50%","红色光芒包裹投射物",.50,0,0,0,"比例"),s("贯通","投射物穿透敌人继续飞行，最多3个","红色闪光穿过目标",3,0,0,0,"目标"),s("狂野","产生冲击波击飞周围敌人","红色环形冲击波",8,5,8,0,"半径/格")},
         {s("落雷","在朝向30格处召唤落雷（伤害50，范围5格）","紫色闪电从天而降并爆炸",50,0,5,0,"伤害/格"),s("雷电附加","附加30点雷电伤害并麻痹1秒","蓝色电弧缠绕投射物",30,20,0,20,"伤害/刻"),s("连发","连续发动3次，每次间隔0.3秒","三道闪电依次落下",3,6,0,0,"次数/刻"),s("雷暴","范围8格，持续5秒的闪电伤害","紫色云团持续闪电",8,50,8,100,"半径/刻")},
         {s("精神冲击","释放精神波（伤害30，范围20格）","淡紫色波纹扩散",30,0,20,0,"伤害/格"),s("精神附加","附加精神伤害并使目标混乱2秒","紫色波动包裹投射物",15,0,0,40,"伤害/刻"),s("引导","自动追踪最近敌人，追踪范围50格","淡紫光点环绕并修正弹道",50,0,50,0,"格"),s("启示","揭露目标弱点，防御降低30%，持续10秒","金色光晕笼罩目标",.30,0,0,200,"比例/刻")},
         {s("传送","向朝向瞬移20格","银白色旋涡，身体消失再出现",20,0,0,0,"格"),s("加速","投射物速度提高100%（速度×2）","银色拖尾加速",2,1,0,0,"倍率"),s("弹射","命中后跳转至下一个目标，最多5次","银色弧线连接目标",5,0,0,0,"次数"),s("传送门","开启持续30秒的双向传送门","银白色旋涡门",30,0,0,600,"秒/刻")},
         {s("火球","发射火球（伤害40，爆炸范围5格）","橙红火焰弹与燃烧拖尾",40,0,5,0,"伤害/格"),s("火焰附加","附加火焰伤害，灼烧每秒10点，持续5秒","火焰包裹投射物",10,5,0,100,"伤害/秒"),s("延迟","延迟1秒后生效","火球变暗并停滞",1,0,0,20,"秒/刻"),s("余烬","留下范围5格、持续10秒的燃烧区域","地面着火并升腾火焰粒子",5,10,5,200,"格/秒")},
         {s("生命链接","链接自身与目标并分摊50%伤害","绿色锁链连接",.50,0,0,0,"比例"),s("连接","效果可连接最多5个目标","绿色连线延伸",5,0,0,0,"目标"),s("连锁","在目标间连锁传递5次","绿色闪电连锁",5,0,0,0,"次数"),s("共享","效果分摊给所有友方","绿色光环扩散",0,0,0,0,"友方")},
         {s("鼓舞","自身攻击提高20%，持续15秒","金色光芒笼罩",.20,0,0,300,"比例/刻"),s("光明附加","对黑暗或亡灵特攻提高50%","金色光芒包裹投射物",.50,0,0,0,"比例"),s("强效","伤害提高50%","金色闪光强化",.50,0,0,0,"比例"),s("光环","范围8格治疗光环，每秒恢复5点，持续15秒","金色环形光环",5,15,8,300,"生命/秒")},
         {s("冰锥","在朝向20格处降下冰锥（伤害35，范围5格）","冰蓝色冰锥从天而降",35,0,5,0,"伤害/格"),s("霜冻附加","附加冰霜伤害并减速30%，持续3秒","冰霜包裹投射物",.30,0,0,60,"比例/刻"),s("延迟","延时2秒爆炸","冰锥悬停并闪烁",2,0,0,40,"秒/刻"),s("冰爆","范围8格并冻结2秒","冰蓝色冲击波扩散",8,2,8,40,"格/秒")},
         {s("暗影锁链","从面前地面升起锁链束缚敌人3秒，范围8格","紫黑锁链从地面升起",3,0,8,60,"秒/刻"),s("暗影附加","附加暗影伤害并使攻击降低20%，持续5秒","暗影包裹投射物",.20,0,0,100,"比例/刻"),s("重力","投射物受重力影响呈弧形弹道","暗影拖尾下坠",1,0,0,0,"弧度"),s("诅咒残留","留下范围5格、每秒10点伤害的区域","暗影区域与紫色粒子",10,0,5,200,"伤害/秒")},
         {s("冻结光环","以玩家为中心释放范围10格冻结光环，冻结3秒","冰白光环扩散",10,3,10,60,"格/秒"),s("停滞","伤害提高50%，射速降低30%","冰晶包裹投射物",.50,.30,0,0,"比例"),s("冻结行为","命中后产生范围5格冰环，减速50%","冰环扩散",.50,0,5,0,"比例/格"),s("冰牢","将目标冻结在冰块中5秒","冰块包裹目标",5,0,0,100,"秒/刻")},
         {s("藤蔓缠绕","催生藤蔓缠绕敌人3秒，范围5格","绿色藤蔓从地面升起",3,0,5,60,"秒/格"),s("自然附加","附加自然毒素，每秒15点，持续5秒","绿色毒雾包裹投射物",15,5,0,100,"伤害/秒"),s("循环","投射物在路径上往返一次","绿色轨迹往返",1,0,0,0,"次数"),s("繁茂","催生范围8格、减速50%的植物区域","大量藤蔓覆盖地面",.50,0,8,0,"比例/格")},
         {s("防御结界","生成吸收200点伤害的防护屏障","淡蓝半透明屏障",200,0,0,0,"伤害"),s("防护附加","护甲提高10，持续10秒","淡蓝色光晕",10,0,0,200,"护甲/刻"),s("反弹","撞到实体时反弹，最多3次","淡蓝闪光改变方向",3,0,0,0,"次数"),s("轮回","制造短暂时间回溯，恢复50点生命","银色旋涡时间倒流",50,0,0,0,"生命")},
         {s("命运骰子","释放随机属性魔力弹，伤害20至60随机","彩色随机粒子",20,60,0,0,"伤害范围"),s("随机附加","附加随机属性效果","彩色闪烁",0,0,0,0,"随机"),s("随机修正","随机改变一种行为","彩色波动",0,0,0,0,"随机"),s("未知","触发随机终末效果","彩色爆炸",0,0,0,0,"随机")},
         {s("守护之盾","在玩家面前生成吸收150点伤害的屏障","淡银半透明盾牌",150,0,0,0,"伤害"),s("庇护附加","减少所受伤害20%","银色光晕",.20,0,0,0,"比例"),s("庇护","施法者同时获得20%减伤","银色光环环绕施法者",.20,0,0,0,"比例"),s("壁垒","创造范围8格、持续15秒的防御结界","银色环形结界",8,15,8,300,"格/秒")},
         {s("光束","发射伤害60、贯穿50格的光束","亮金光柱贯穿",60,50,0,0,"伤害/格"),s("光附加","附加光属性，对黑暗特攻50%","金色光芒包裹",.50,0,0,0,"比例"),s("加速","投射物速度提高200%（速度×3）","金色拖尾加速",3,2,0,0,"倍率"),s("净化","净化范围10格内负面效果","金色光芒扩散",10,0,10,0,"格")},
         {s("必中斩击","朝向发动必中斩击（伤害50，范围15格）","金色剑光斩出",50,0,15,0,"伤害/格"),s("锋锐","攻击无视50%护甲","银色剑光",.50,0,0,0,"比例"),s("精准","暴击率提高50%","金色瞄准光环",.50,0,0,0,"比例"),s("裁决","生命低于30%的目标即死","金色剑刃从天而降",.30,0,0,0,"生命比例")},
         {s("治愈","朝向释放治疗波，恢复50点生命，范围15格","翠绿光芒扩散",50,0,15,0,"生命/格"),s("治愈附加","附加治疗效果，恢复友方20点生命","绿色光晕",20,0,0,0,"生命"),s("新生","命中后恢复施法者20点生命","绿色粒子流向施法者",20,0,0,0,"生命"),s("繁茂领域","范围8格持续治疗，每秒恢复10点","绿色光环与花朵",10,0,8,0,"生命/秒")},
         {s("传送","向朝向传送15格，可穿墙","银白色旋涡",15,0,0,0,"格"),s("传递","将增益复制给范围10格附近友方","银色连线延伸",10,0,10,0,"格"),s("同步","效果同步作用于施法者","银色光环环绕",1,0,0,0,"开关"),s("调和","平均分配范围10格友方生命值","银色光环扩散",10,0,10,0,"格")},
         {s("镜像分身","创造持续30秒的幻影分身","淡紫透明分身",30,0,0,600,"秒/刻"),s("自我附加","附加真实伤害，无视30%护甲","淡紫光晕",.30,0,0,0,"比例"),s("镜像","复制一份效果","淡紫色分身出现",1,0,0,0,"份"),s("融合","合并多个效果为一个强效","淡紫旋涡融合",1,0,0,0,"份")},
         {s("水流冲击","释放高压水流（伤害30，击退10格）","蓝色水柱冲击",30,10,0,0,"伤害/格"),s("水附加","附加水属性伤害20%","蓝色水波包裹",.20,0,0,0,"比例"),s("液态","可穿过树叶、水和岩浆等非固体方块","蓝色水波",1,0,0,0,"开关"),s("潮汐","推开或拉近范围10格敌人","蓝色潮汐波",10,0,10,0,"格")},
         {s("丰饶附魔","为武器附加20点自然伤害","绿色光芒包裹武器",20,0,0,0,"伤害"),s("大地附加","附加大地属性并击晕目标1秒","棕色光芒",1,0,0,20,"秒/刻"),s("储存","击杀后储存20点魔力","绿色光球吸收",20,0,0,0,"魔力"),s("孕育","留下范围5格、每秒15点伤害的能量球","绿色能量球漂浮",15,0,5,0,"伤害/秒")},
         {s("破晓","释放范围20格、驱散黑暗的光芒","金色光芒爆发",20,0,20,0,"格"),s("净化附加","附加净化效果，驱散目标增益","金色光芒",1,0,0,0,"增益"),s("净化","投射物移除敌方1个增益","金色闪光",1,0,0,0,"增益"),s("黎明","创造范围10格、持续15秒的净化区域","金色光环令黑暗退散",10,15,10,300,"格/秒")},
         {s("先祖召唤","召唤协助战斗、持续60秒的先祖之魂","淡金灵体出现",60,0,0,1200,"秒/刻"),s("灵魂附加","附加灵魂伤害，无视100%物理护甲","淡金光晕",1,0,0,0,"比例"),s("领地","固定范围10格内效果增强50%","金色边界线",.50,0,10,0,"比例/格"),s("领地降临","范围15格、持续30秒的己方领地","金色领域展开",15,30,15,600,"格/秒")}
      };
      Map<String, RuneDefinition> out = new LinkedHashMap<>();
      for (int i = 0; i < ids.length; i++) {
         Map<RunePosition, String> semantics = Map.of(RunePosition.TRIGGER,k[i][0],RunePosition.EFFECT,k[i][1],RunePosition.MODIFIER,k[i][2],RunePosition.TERMINAL,k[i][3]);
         Map<RunePosition, RuneEffectSpec> specs = Map.of(RunePosition.TRIGGER,x[i][0],RunePosition.EFFECT,x[i][1],RunePosition.MODIFIER,x[i][2],RunePosition.TERMINAL,x[i][3]);
         out.put(ids[i], new RuneDefinition(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,ids[i]), names[i], colors[i],
            ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,"textures/gui/runes/"+ids[i]+".png"),50.0D,
            Set.of("air","stone","weapon","armor","tool","body"),Set.of(),Set.of(k[i][1]),semantics,specs));
      }
      return out;
   }
}
