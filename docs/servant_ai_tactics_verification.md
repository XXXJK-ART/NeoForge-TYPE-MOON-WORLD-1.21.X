# 从者战斗阶段、施法时机与飞行

适用版本：Minecraft 1.21.1、NeoForge 21.1.242、64 位 Java 21。未改变注册 ID、附属模组接口或依赖版本。

## 行为

- 统一战术 AI、旧角色战斗助手和角色独立阶段所依据的战斗形势。持续锁定目标时，远距离交战也会累计时间。
- 试探最多 600 tick（30 秒）。己方受伤、敌人濒死或存在实力劣势时，可以在这个节点直接进入决胜阶段；满血僵持 1200 tick（60 秒）也会进入决胜阶段。
- 实力评估综合最大生命、攻击、魔力、护甲与敏捷。对手评估值达到己方的 1.65 倍时，立即进入决胜阶段。战斗途中恢复生命不会降回试探。
- 技能选择利用敌方受控、蓄力、自身危急、即将命中的高危攻击和施法距离。仲裁保留规划器的实际排序；技能决策窗口从单 tick 扩为 8 tick，降低角色固定 tick 判定错过施法机会的问题。
- 吉尔伽美什维持空中拉扯和原生王之财宝齐射，不再被通用单发攻击、无接触超时落地或对卫宫的强制落地规则覆盖。Ea 蓄力及发射保留自身的运动控制权。
- 远程飞行从者保持空中高度；缺少近战接触不再触发强制近战飞行。击飞和受控运动仍优先。
- 王之财宝记录并同步每次齐射的数量。少于 10 发时完整保留开门和拖尾，并绕过模型省略；大批齐射继续使用原有抽样。玩家从者卡和贤王的齐射也填写相同信息。

## 验证（2026-10-18）

所有命令在项目根目录使用仓库 Gradle wrapper。验证目录与 IDE 构建、现有开发世界隔离。

```powershell
.\gradlew.bat build --offline -I tools/servant-ai-verification.init.gradle --project-cache-dir build/servant-ai-gradle-cache
.\gradlew.bat runGameTestServer --offline -I tools/servant-ai-verification.init.gradle --project-cache-dir build/servant-ai-gradle-cache -PgameTestNamespaces=typemoonworld_tactics
.\gradlew.bat runClient --offline -I tools/servant-ai-verification.init.gradle --project-cache-dir build/servant-ai-gradle-cache -PservantAiClientSmoke=true
```

- 完整构建通过，508 项 JUnit 测试全部通过；附属模组示例编译通过。
- 专用 GameTest 服务器的 5 项测试全部通过：30 秒远程阶段切换与治疗后阶段保持、同生命但攻击悬殊时提前决胜、远程飞行超时后保持后撤、实际飞行与原生齐射、5 发齐射数量和完整效果标记。
- 客户端冒烟测试检查正常召唤后持续飞行、原生齐射，以及客户端收到 5 发完整效果标记。通过标志为 `SERVANT_AI_CLIENT_SMOKE_PASS`；截图位于 `build/servant-ai-client-run/qa/screenshots/`。
- 已检查构建 JAR 包含战斗评估类与测试结构，客户端冒烟模组未打入发布 JAR。

测试日志仍有仓库现有的全局战利品修饰器与旧物品配方解析警告。此项验证覆盖平地与固定对手，复杂地形、大规模混战和长时间多人战斗仍需游戏内观察。

构建产物：`build/servant-ai-verification/libs/Type_Moon_World-neoforge-4.0-mc1.21.1.jar`。
