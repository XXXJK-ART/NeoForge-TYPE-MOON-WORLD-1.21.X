# 樱：独立附属体系

本目录是独立的 NeoForge 附属工程。原参考工程和 Type Moon World 本体均不是本工程的源码组成部分。

## 边界

- `typemoonworld` 仅通过公开 `net.xxxjk.typemoonworld.api` 编译接口接入。
- 本工程不导入 `net.xxxjk.TYPE_MOON_WORLD`，不包含本体 JAR，不声明 Mixin，不修改本体类或资源。
- 虚数空间、黑泥、影子、圣杯侵蚀、规则破碎后的封印状态都保存在本附属自己的 NeoForge attachment/entity/data 中。
- 魔术定义、执行器、Master profile、公共事件和通用 `MagicOption` 由 Type Moon World API 注册。
- 无法由公开 API 表达的本体私有行为被移除或降级：固定本体 GUI/轮盘路由、内置 `PlayerVariables` 字段写入、内置实体子类、内置战斗流程 Mixin、内置知识删除和内置 MP 回复字段修改。

## 依赖

构建时通过 `gradle.properties` 的 `type_moon_world_jar` 读取本体导出的只读 API JAR。运行时必须由启动器提供 Type Moon World、NeoForge、GeckoLib 和 TerraBlender；本工程不会把它们打包进输出。

## 迁移规则

旧存档中的本体魔术知识不会被删除。附属执行器依据自己的 attachment 状态判定是否可用，因此 Rule Breaker 的“解封/封印”不会通过私有反射改变本体知识列表。旧的本体轮盘槽位也不会被本工程重写。

## 验证

```powershell
.\gradlew.bat processResources --console=plain
.\gradlew.bat compileJava --console=plain
.\gradlew.bat clean build --console=plain
```

输出 JAR 只包含 `typemoonaddon`（包括 shadow-logic 包）内容，不包含 Type Moon World 类。
