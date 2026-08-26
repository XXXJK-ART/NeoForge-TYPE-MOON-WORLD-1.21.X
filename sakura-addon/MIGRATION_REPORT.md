# 樱系统抽离报告

## 来源与目标

- 只读来源：`E:\mc\虚数\间桐樱\typemoonaddon-src`
- 新体系：`E:\mc\虚数\樱`
- 本体参考：来源工程 `libs` 下的 Type Moon World 3.3.2 JAR

本工程迁移源码、资源、Blockbench 源文件、工具和 Gradle wrapper。构建依赖只使用本体导出的 API JAR，不迁移本体 JAR、运行目录或缓存。

## 已移除的侵入点

- 删除 17 个 Mixin 及 Mixin 元数据。
- 删除对本体 GUI、按键、网络轮盘、`PlayerVariables`、内置从者实体、Master 服务、内置卡片物品、内置声音和 Rho Aias 实体的直接依赖。
- 删除反射写本体属性、魔力、知识、熟练度和轮盘槽位的路径。
- Master Card 改为公开 `MasterProfileRegistry#createCard`。
- 魔术模式改为公开 `MagicOption` 与服务端 preset 校验。
- 从者识别改为公开玩家从者形态和 `typemoonaddon:servants` 实体标签；契约改为公开 `MasterAccess`。
- Gate 投射物处理改为原版 `Projectile#getOwner`；Rho Aias 改为附属自有实体。

## 所有权

附属拥有虚数空间、黑泥、影术、污染、侵蚀、铠甲、网络 payload、实体、渲染和持久化。Type Moon World 只拥有它公开 API 明确暴露的魔术目录、知识、魔力、Master 和玩家从者形态。

## 有意降级

公开 API 无法修改本体固定 GUI、内置招式细节、NPC 契约内部字段、魔力回复字段或删除已学知识。因此这些行为不再通过私有实现强行保持；封印与迁移由附属状态和执行器校验完成。
