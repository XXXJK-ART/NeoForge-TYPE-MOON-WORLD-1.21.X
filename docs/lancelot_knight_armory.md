# 兰斯洛特：骑士不死于徒手武库

变身兰斯洛特后，蹲下使用技能 8 打开武库，选择召唤物。站立使用技能 8 继续沿用夺取／同化功能。无毁的湖光开启时封印武库。

未安装卓越前线时，仅提供本模组铁棍；安装兼容的卓越前线后，默认提供全部已注册联动物品及 A-10，不需要先持有枪械或使用解锁命令。原有解锁／清除命令仍可显式调整玩家的武库。卓越前线保持 `optional` 依赖，开发编译使用 `compileOnly`，不打包进主模组。

| 召唤物 | MP | 冷却 | 存续 |
| --- | ---: | ---: | --- |
| 铁棍 | 10 | 5 秒 | 沿用生成物／变身清理规则 |
| MP5 | 50 | 60 秒 | 30 秒 |
| 加特林 | 80 | 180 秒 | 30 秒 |
| AWM／巴雷特 | 50 | 60 秒 | 30 秒、最多 15 发，仍受弹药限制 |
| A-10 | 450 | 180 秒 | 最多 15 秒，到时强制坠机爆炸 |

NPC 的加特林与 A-10 仅在生命值不高于最大生命值一半时可使用；玩家不受血量门槛限制。玩家和 NPC 的加特林与 A-10 成功使用后各自独立冷却 180 秒。铁棍与普通转化不受此限制。玩家可继续选用已开放的其他联动物品；NPC 仅会召唤加特林和 A-10，近战时继续使用铁棍。玩家和 NPC 共享召唤耗魔、枪械时限及飞机寿命。加特林开火时显式瞄准目标，普通子弹不依赖延迟更新的朝向；射击检查视线与合法敌人。飞机禁用玩家及 NPC 的全部机载武器，NPC 直接驾机冲向敌人，不再绕圈射击。伤害保护主人、盟友及受保护的御主。

A-10 使用独立召唤冷却，不占用技能 7。每名玩家同时最多一架，载具计入骑士不死于徒手的 30 件控制上限。飞机在主人上方 15 格生成并自动骑乘、启动；位置碰撞、世界边界／高度限制、控制容量、MP、冷却和骑乘失败都会阻止召唤，失败不消耗 MP 或进入冷却。

起飞前 20 ticks 为额外碰撞判定的宽限期。之后触墙、触地、垂直碰撞、撞到合法敌人或载具进入撞毁状态会引爆；从召唤或同化完成开始计时，300 ticks（正常 TPS 下 15 秒）到期也会强制引爆。使用卓越前线原生载具爆炸，伤害及地形破坏遵循该模组配置。引爆前解除乘坐，给予 5 秒坠落保护；召唤飞机直接清理，不通过载具拆解产生可回收残骸。主人死亡、离线、跨维度、解除变身、开启湖光或进入受限灵体状态时，召唤飞机清理。寿命以保存的世界游戏时间到期值判定，区块重新加载时继续检查。

开发验证：

```powershell
.\gradlew.bat --offline test --tests com.captain.servant.lancelot.KnightArmoryRulesTest
.\gradlew.bat --offline runGameTestServer -I tools/fatesupport-compat.init.gradle -I tools/lancelot-armory-validation.init.gradle -PfateSupportCompat=warfare -PgameTestNamespaces=typemoonworld_lancelot_armory
.\gradlew.bat --offline runGameTestServer -I tools/fatesupport-compat.init.gradle -I tools/lancelot-armory-validation.init.gradle -PfateSupportCompat=none -PgameTestNamespaces=typemoonworld_lancelot_armory
```

联动回归覆盖武库可用项、耗魔与重复请求、独立飞机冷却、15 秒飞机期限、召唤失败退款、碰撞与到期爆炸、失效主人清理、NPC 加特林瞄准及命中、NPC 驾机撞击，以及三种机载开火入口全部禁用。不带联动时验证主模组加载、铁棍召唤以及禁用联动选择。
