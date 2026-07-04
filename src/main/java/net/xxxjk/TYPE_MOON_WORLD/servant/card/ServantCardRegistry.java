package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.List;
import java.util.Locale;
import net.minecraft.world.entity.EquipmentSlot;

public final class ServantCardRegistry {
   public static final List<Entry> ENTRIES = List.of(
      new Entry("emiya_archer", "Emiya Archer", "卫宫", true),
      new Entry("artoria_pendragon", "Artoria Pendragon", "阿尔托莉雅", false),
      new Entry("sasaki_kojiro", "Sasaki Kojiro", "佐佐木小次郎", false),
      new Entry("cu_chulainn", "Cu Chulainn", "库·丘林", false),
      new Entry("medea", "Medea", "美狄亚", false),
      new Entry("medusa", "Medusa", "美杜莎", false),
      new Entry("cursed_arm_hassan", "Cursed Arm Hassan", "咒腕哈桑", false),
      new Entry("heracles", "Heracles", "赫拉克勒斯", false),
      new Entry("oda_nobunaga", "Oda Nobunaga", "织田信长", false),
      new Entry("enkidu", "Enkidu", "恩奇都", false),
      new Entry("gawain", "Gawain", "高文", false),
      new Entry("paracelsus", "Paracelsus", "帕拉塞尔苏斯", false),
      new Entry("li_shuwen", "Li Shuwen", "李书文", false)
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
      return null;
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
