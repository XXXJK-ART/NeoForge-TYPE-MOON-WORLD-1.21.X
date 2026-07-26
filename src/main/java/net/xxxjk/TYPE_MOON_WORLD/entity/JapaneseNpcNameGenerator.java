package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.util.RandomSource;

/** Historical Japanese names used by the generated kendo NPCs. */
public final class JapaneseNpcNameGenerator {
   private static final String[] SURNAMES = {
      "织田", "真田", "武田", "上杉", "伊达", "毛利", "岛津", "井伊", "本多", "加藤",
      "斋藤", "冲田", "近藤", "土方", "山南", "永仓", "会津", "坂本", "桂", "西乡",
      "佐藤", "铃木", "高桥", "田中", "渡边", "伊藤", "山本", "中村", "小林", "藤井",
      "吉田", "山田", "佐佐木", "山口", "松本", "井上", "木村", "林", "清水", "山崎",
      "森", "阿部", "池田", "桥本", "山下", "石川", "中岛", "前田", "藤田", "小川",
      "武内", "奈须", "成田", "虚渊", "东出", "樱井", "卫宫", "远坂", "间桐", "言峰",
      "两仪", "苍崎", "黑桐", "藤村"
   };
   private static final String[] MALE = {
      "信长", "幸村", "谦信", "政宗", "信玄", "忠胜", "总司", "勇", "一", "岁三",
      "龙马", "小五郎", "隆盛", "家康", "秀吉", "秀赖", "义元", "义辉", "义昭", "光秀",
      "长政", "兼续", "直江", "半藏", "左近", "重成", "清正", "正成", "利家", "庆次",
      "又兵卫", "勘兵卫", "官兵卫", "如水", "宗矩", "柳生", "严胜", "十兵卫", "弥助", "平八",
      "俊郎", "启介", "拓海", "直人", "健一", "健太", "大辅", "雄太", "和也", "翔太",
      "悠真", "悠斗", "阳一", "阳介", "诚", "诚司", "聪", "圭介", "达也", "亮",
      "直树", "弘树", "优斗", "一郎", "次郎", "三郎", "太郎", "春树", "夏树", "秋人",
      "崇", "国广", "良悟", "玄", "祐一郎", "光", "叶月", "准", "钢",
      "士郎", "切嗣", "时臣", "慎二", "脏砚", "绮礼","干也"
   };
   private static final String[] FEMALE = {
      "宁宁", "千代", "松", "甲斐", "八重", "武子", "玉", "菊", "冬", "花",
      "铃", "葵", "小春", "巴", "咲", "阿市", "茶茶", "初", "江", "稻姬",
      "麻里", "阿松", "小督", "濑名", "志乃", "阿梅", "阿竹", "阿夏", "阿雪", "阿琴",
      "美咲", "樱", "阳子", "优子", "京子", "直子", "裕子", "真由美", "沙织", "千寻",
      "青子", "橙子", "鲜花", "大河", "凛", "樱", "葵"
   };

   private JapaneseNpcNameGenerator() {}

   public static String apprentice(RandomSource random, boolean female) { return pick(random, SURNAMES) + " " + pick(random, female ? FEMALE : MALE); }
   public static String ronin(RandomSource random) { return pick(random, SURNAMES) + " " + pick(random, MALE); }
   public static String shinsengumi(RandomSource random) { return pick(random, SURNAMES) + " " + pick(random, MALE); }
   public static String master(RandomSource random) { return pick(random, SURNAMES) + " " + pick(random, MALE); }
   private static String pick(RandomSource random, String[] values) { return values[random.nextInt(values.length)]; }
}
