# 樱

`樱` 是 NeoForge 1.21.1 上独立运行的 Type Moon World 附属体系。它从 `E:\mc\虚数\间桐樱\typemoonaddon-src` 的功能中抽离而来，但不包含、不修改也不重新打包 Type Moon World 本体。

## 体系内容

- 虚数魔术：虚数收纳、虚数吸收、虚数保护、影子实体化、影之束缚、影之转移与禁术。
- 持久状态：自动分页虚数空间、刻印虫同化、圣杯侵蚀、黑泥污染、精神损伤和 Rule Breaker 封印状态。
- 独立实体与表现：黑影、影之使魔、影带、黑泥、诅咒铠甲、虚数环礼装和附属自有的 Rho Aias 表现实体验。
- Type Moon World 接入：公开 API 注册魔术、通用选项、Master profile、知识、魔力访问、玩家从者形态和生命周期事件。

## 不再执行的本体修改

- 不导入 `net.xxxjk.TYPE_MOON_WORLD` 内部包。
- 不使用 Mixin 注入本体 UI、招式、伤害、AI、令咒或卡片恢复流程。
- 不反射读写 `PlayerVariables`，不修改本体魔力上限、回复字段、轮盘槽位或已学知识列表。
- 不继承本体实体类和物品类，不把本体类或资源打入附属 JAR。

虚数属性和樱进度由附属 attachment 拥有真值；旧知识条目可以留在本体存档中，但附属执行器会按自己的状态拒绝被封印的能力。

## 构建

要求 Java 21、NeoForge 21.1.242、GeckoLib 4.9.2、TerraBlender 4.1.0.8 和 Type Moon World 3.3 API。`gradle.properties` 指向本体导出的只读 API JAR，该 JAR 不会进入构建产物。

```powershell
.\gradlew.bat verifyTypeMoonReference --console=plain
.\gradlew.bat clean build --console=plain
```

产物（唯一 JAR）：

- `build/libs/typemoonaddon-1.21.1-1.0.0.jar`
影术逻辑已经并入主 JAR。运行时仍须安装未经修改的 Type Moon World 本体及其依赖。

工程边界与迁移规则见 [ARCHITECTURE.md](ARCHITECTURE.md)，抽离清单见 [MIGRATION_REPORT.md](MIGRATION_REPORT.md)。
