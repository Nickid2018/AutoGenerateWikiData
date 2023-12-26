# AutoGenerateWikiData

自动生成 Wiki 所需数据。

## 自动值

此项目可以直接生成 Wiki 所需要的 Java 版[自动值](https://zh.minecraft.wiki/?curid=107468)。

### 提供数据类型

所有数据将生成在`run/output`下。

- 方块：
    * `block_breaking_tools`：合适挖掘工具。
    * `block_burn_odds`：烧毁几率。
    * `block_destroy_time`：破坏时间（硬度）。
    * `block_explosion_resistance`：爆炸抗性。
    * `block_ignite_by_lava`：熔岩可燃性。
    * `block_ignite_odds`：引燃几率。
    * `block_instrument`：乐器。
    * `block_legacy_solid`：固体方块。
    * `block_map_color`：地图颜色基色。
    * `block_push_reaction`：活塞推拉行为。
    * `block_redstone_conductor`：红石导体。
    * `block_replaceable`：可替代方块。
    * `block_suffocating`：窒息方块。
- 附魔 / 魔咒
    * `enchantment_category`：魔咒类别。
    * `enchantment_cost`：附魔等级限制。
    * `enchantment_flag`：魔咒出现条件（宝藏等）。
    * `enchantment_incompatible`：不共存魔咒。
    * `enchantment_max_level`：魔咒最大等级。
    * `enchantment_rarity`：魔咒稀有度。
- 实体
    * `entity_mob_category`：（暂无译名，控制周期生成）。
    * `entity_mob_type`：生物类型。
- 物品
    * `item_burn_duration`：烧炼时间（游戏刻计）。
    * `item_creative_mode_tabs`：创造模式标签页。
    * `item_food_properties`：食物属性。
    * `item_max_damage`：最大耐久度。
    * `item_max_stack_size`：最大堆叠数量。
    * `item_rarity`：物品基础稀有度。

### 生成数据

有两种办法生成此数据：

1. 使用 GitHub Action 自动构建（`autovalue`），只需要输入需要生成数据的版本，然后触发工作流即可。
2. 将项目克隆到本地，设置环境变量`MAKE_VERSION`为需要生成数据的版本，之后执行`gradlew run`。

## 方块与生物群系统计

此项目可以生成方块与生物群系的统计数据。

### 数据结构

所有数据将生成在`run/runtime`下，每个文件的名字以`<dimension>_<item>_count.json`命名，其中`<dimension>`为维度，`<item>`为统计类型（`block`方块或`biome`生物群系）。

每个文件包含下列几项数据：

* `worldSeed`：世界种子。
* `minHeight`：最低高度。
    - 对于方块，高度为 Y 坐标。对于生物群系，高度为生物群系单元（4×4×4方块大小）的坐标。
* `maxHeight`：最高高度。
* `posProvider`：区块位置提供器。
    - `count`：当前统计的区块数量。
    - `total`：总区块数量。
    - `blockSize`：分块大小。
    - `seed`：（仅随机选择区块）随机种子。
* `block` / `biome`：统计数据。
    - 键值对，键为命名空间 ID，值为每个高度上的数量。

### 生成数据

生成数据有两种办法：

**使用 GitHub Action 构建**：

可以使用工作流`statistics`，输入需要生成数据的版本，然后触发工作流即可，获得工件（一个 JAR）。

之后，设置以下环境变量：

* `BATCH_SIZE`：（默认为 4）每个游戏刻提交给区块生成系统的区块数量。
* `CHUNK_TOTAL`：（默认为 25000）统计的区块总数量。
* `BLOCK_SIZE`：（默认为 1089，必须为完全平方数，否则将向上寻找完全平方数）分块大小。分块越大，一段时间内的运行速度会更快，但也会造成更高程度的内存占用。
* `CHUNK_POS_PROVIDER_FACTORY`：（默认为 `continuous`）区块位置提供器工厂。
    - `random`：以分块为单位随机选择区块。
    - `continuous`：连续选择区块，以世界原点为中心。

在同目录下，创建`eula.txt`，并写入`eula=true`。

在同目录下，创建`server.properties`，并写入以下内容：

```properties
max-tick-time=-1
sync-chunk-writes=false
```

最后，执行`java -jar <JAR> -nogui`即可。

**使用本地构建**：

将项目克隆到本地，设置环境变量`MAKE_VERSION`为需要生成数据的版本，并将上述 4 个环境变量设置好后，执行`gradlew runStatistics`。