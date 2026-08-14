package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.List;
import java.util.Locale;
import net.minecraft.world.entity.EquipmentSlot;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;

public final class ServantCardRegistry {
   public static final List<Entry> ENTRIES = List.of(
      new Entry("fanatic_assassin", "Fanatic (Assassin)", "狂信子（Assassin）", true),
      new Entry("arash", "Arash (Archer)", "阿拉什（Archer）", true),
      new Entry("nightingale", "Florence Nightingale (Berserker)", "南丁格尔（Berserker）", true),
      new Entry("shadow_hassan", "Hassan of the Shadow", "幽式之哈桑", false),
      new Entry("gilgamesh", "Gilgamesh", "吉尔伽美什", true),
      new Entry("gilgamesh_caster", "Gilgamesh (Caster)", "吉尔伽美什（Caster）", true),
      new Entry("emiya_archer", "Emiya Archer", "卫宫", true),
      new Entry("artoria_pendragon", "Artoria Pendragon", "阿尔托莉雅", false),
      new Entry("sasaki_kojiro", "Sasaki Kojiro", "佐佐木小次郎", false),
      new Entry("cu_chulainn", "Cu Chulainn", "库·丘林", true),
      new Entry("medea", "Medea", "美狄亚", true),
      new Entry("medusa", "Medusa", "美杜莎", false),
      new Entry("cursed_arm_hassan", "Cursed Arm Hassan", "咒腕哈桑", false),
      new Entry("heracles", "Heracles", "赫拉克勒斯", false),
      new Entry("oda_nobunaga", "Oda Nobunaga", "织田信长", true),
      new Entry("enkidu", "Enkidu", "恩奇都", true),
      new Entry("gawain", "Gawain", "高文", true),
      new Entry("paracelsus", "Paracelsus", "帕拉塞尔苏斯", true),
      new Entry("li_shuwen", "Li Shuwen", "李书文", true),
      new Entry("pale_rider", "Pale Rider", "苍白骑士", false),
      new Entry("ushiwakamaru_rider", "Ushiwakamaru (Rider)", "牛若丸（Rider）", true),
      new Entry("zhao_yun_rider", "Zhao Yun (Rider)", "赵云（Rider）", true),
      new Entry("senko_muramasa", "Senko Muramasa (Saber)", "千子村正", true),
      new Entry("iskandar", "Iskandar (Rider)", "伊斯坎达尔（Rider）", true)
   );

   private ServantCardRegistry() {
   }

   public static Entry byId(String servantId) {
      if (servantId != null) {
         for (Entry entry : ENTRIES) {
            if (entry.servantId().equals(servantId)) {
               return entry;
            }
         }
      }
      if (servantId != null) {
         var definition = ServantDataRegistry.get(servantId);
         if (definition == null && servantId.indexOf(':') >= 0) definition = ServantDataRegistry.get(servantId.substring(servantId.indexOf(':') + 1));
         if (definition != null) return new Entry(servantId, definition.displayName(), definition.displayNameZh(), true);
      }
      return null;
   }

   /** Includes data-defined addon servants in addition to legacy built-in entries. */
   public static java.util.List<Entry> all() {
      java.util.LinkedHashMap<String, Entry> merged = new java.util.LinkedHashMap<>();
      ENTRIES.forEach(entry -> merged.put(entry.servantId(), entry));
      ServantDataRegistry.getAll().forEach((id, definition) -> {
         merged.putIfAbsent(id, new Entry(id, definition.displayName(), definition.displayNameZh(), true));
      });
      return java.util.List.copyOf(merged.values());
   }

   public static String cardItemId(String servantId) {
      return "servant_card_" + servantId;
   }

   public static String armorItemId(String servantId, EquipmentSlot slot) {
      return "servant_card_" + servantId + "_" + slotName(slot);
   }

   public static String slotName(EquipmentSlot slot) {
      return slot == null ? "unknown" : slot.getName().toLowerCase(Locale.ROOT);
   }

   public record Entry(String servantId, String englishName, String zhName, boolean hasRealArmor) {
   }
}
