# AutoGenerateWikiData

自动生成 Wiki 所需数据。

## 自动值

此项目可以直接生成 Wiki 所需要的 Java 版[自动值](https://zh.minecraft.wiki/?curid=107468)。

### 提供数据类型

所有数据将生成在`run/output`下。

- 生物群系
    * `biome_base_temperature`：基础温度。
    * `biome_creature_probability`：生物生成概率。
    * `biome_fog_color`：雾颜色。
    * `biome_foliage_color`：树叶颜色。
    * `biome_has_precipitation`：是否有降水。
    * `biome_sky_color`：天空颜色。
    * `biome_spawn_data`：生物生成数据。
    * `biome_water_color`：水颜色。
    * `biome_water_fog_color`：水中雾颜色。
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
    * `block_support_type`：方块支撑属性。
- 附魔 / 魔咒
    * `enchantment_category`：魔咒类别。
    * `enchantment_cost`：附魔等级限制。
    * `enchantment_flag`：出现条件（宝藏等）。
    * `enchantment_incompatible`：不共存魔咒。
    * `enchantment_max_level`：最大等级。
    * `enchantment_rarity`：魔咒稀有度。
- 实体
    * `entity_mob_category`：（暂无译名，控制周期生成）。
- 物品
    * `item_attribute_modifiers`：物品属性修饰符。
    * `item_burn_duration`：烧炼时间（游戏刻计）。
    * `item_creative_mode_tabs`：创造模式标签页。
    * `item_food_properties`：食物属性。
    * `item_max_damage`：最大耐久度。
    * `item_max_stack_size`：最大堆叠数量。
    * `item_rarity`：物品基础稀有度。
- 状态效果
    * `mob_effect_cannot_affect`：免疫此状态效果的生物。
    * `mob_effect_category`：状态效果类别。
    * `mob_effect_color`：状态效果颜色。
    * `mob_effect_instantenous`：瞬时状态效果。
- 其他
    * `postion effect`：药水效果。

### 生成数据

有两种办法生成此数据：

1. 使用 GitHub Action 自动构建（`autovalue`），只需要输入需要生成数据的版本，然后触发工作流即可。
2. 将项目克隆到本地，设置环境变量`MAKE_VERSION`为需要生成数据的版本，之后执行`gradlew run`。

## 方块与生物群系统计

此项目可以生成方块与生物群系的统计数据。

### 数据结构

所有数据将生成在`run/runtime`下，每个文件的名字以`<dimension>_<item>_count.json`命名，其中`<dimension>`为维度，`<item>`
为统计类型（`block`方块或`biome`生物群系）。

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

**注意：此方法不能统计大量区块，因为当前程序具有无法解决的内存泄漏**。

可以使用工作流`statistics`，输入需要生成数据的版本，然后触发工作流即可，获得工件（一个 JAR）。

之后，设置以下环境变量：

* `BATCH_SIZE`：（默认为 4）每个游戏刻提交给区块生成系统的区块数量。
* `CHUNK_TOTAL`：（默认为 25000）统计的区块总数量。
* `BLOCK_SIZE`：（默认为 1089，必须为完全平方数，否则将向上寻找完全平方数）分块大小。分块越大，一段时间内的运行速度会更快，但也会造成更高程度的内存占用。
* `CHUNK_POS_PROVIDER_FACTORY`：（默认为 `continuous`）区块位置提供器工厂。
    - `random`：以分块为单位随机选择区块。
    - `continuous`：连续选择区块，以世界原点为中心。
* `DIMENSIONS`：（默认为空）以逗号分隔的维度列表，不为空时代表仅处理指定的维度。

在同目录下，创建`eula.txt`，并写入`eula=true`。

在同目录下，创建`server.properties`，并写入以下内容：

```properties
max-tick-time=-1
sync-chunk-writes=false
pause-when-empty-seconds=1000000000
```

最后，执行`java -jar <JAR> -nogui`即可。

**使用本地构建**：

将项目克隆到本地，设置环境变量`MAKE_VERSION`为需要生成数据的版本，`WORLD_TOTAL`为总体存档的数量，`ASYNC_COUNT`为同时运行
Minecraft 实例的数量，并将上述 4 个环境变量设置好后，执行`gradlew runStatistics`。

下面为一个简单的环境变量测试用例：

```shell
MAKE_VERSION=1.21.4
BATCH_SIZE=16
CHUNK_POS_PROVIDER_FACTORY=random
WORLD_TOTAL=20
ASYNC_COUNT=4
BLOCK_SIZE=2500
CHUNK_TOTAL=500000
```

## 渲染标准视图

此项目可以使用 Minecraft 原版渲染管线渲染 Wiki 所需的标准视图。

### 运行

运行有两种方式：

**使用 GitHub Action 构建**：

可以使用工作流`iso`，输入需要生成数据的版本，然后触发工作流即可，获得工件（一个 JAR）。将此 JAR 替换掉启动器内`<对应版本>.jar`
即可。

**使用本地构建**：

将项目克隆到本地，设置环境变量`MAKE_VERSION`为需要生成数据的版本，之后执行`gradlew runISO`。项目将自动作为一个简单的启动器运行客户端。

### 使用

使用此项目需要一个虚空预设的超平坦世界（主要是防止其他方块影响渲染，如果要渲染正常的世界也可以）。在进入世界后，使用 F1 隐藏
GUI，以防止渲染时渲染到玩家手臂。

所有命令都在聊天栏输入，**且不需要正斜杠`/`**：

* `persp`：切换为透视投影。
* `ortho`：切换为正交投影。
* `nosave`：阻止自动保存，防止渲染过程中计时因保存出现延迟。
* `save`：允许自动保存。从禁用自动保存到允许自动保存后，游戏会在下一个游戏刻立刻保存。
* `flatlight`：当物品展示实体渲染时，使用扁平物品光照渲染。
* `blocklight`：当物品展示实体渲染时，使用3D物品光照渲染。
* `levellight`：不修改物品展示实体的渲染，使用维度的基础光照。
* `wsize <宽度>x<高度>`：设置渲染窗口大小，控制最终渲染图片大小。
* `osize <宽度>x<高度>`：设置正交投影的近平面和远平面的大小。
* `call <命令>`：调用一个命令，命令不需要以正斜杠开头。
* `sshot [文件名称]`：渲染截图，文件名称为可选参数，不填则使用默认名称`screenshot.png`。所有文件都将保存在`screenshots`文件夹下。
* `run <文件名>`：运行一个执行脚本，执行脚本内的命令会被逐行执行，且可用命令与上文一致，但不包含`run`命令，且新增了
  `sleep <时间>`命令以等待一段时间。文件中空行和以`#`开头的行将被忽略。

举例来说，如果需要渲染一个石头的标准视图，可以使用以下脚本：

```text
CALL tick freeze
CALL fill 24 -61 24 -8 -61 -8 air
CALL tp @s 1.5 -0.3035 1.5 135 30
WSIZE 300x300
OSIZE 1.6x1.6
ORTHO
CALL setblock 0 0 0 stone
SLEEP 200
SSHOT stone.png
```

如果不使用脚本，则`SLEEP 200`不需要执行，所有`CALL`也可以直接调用正常命令而不需要使用这些命令调用。