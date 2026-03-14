# BulletAPI 召唤物开发说明

这份文档说明 BulletAPI 原生召唤系统的组成、运行流程，以及如何新增召唤物模板。

## 核心结构

- `SummonDefinition`
  - 召唤物模板定义。
  - 负责描述槽位、外观、移动、碰撞、攻击方式等静态配置。
- `AbstractSummonBlueprint`
  - 召唤物蓝图基类。
  - 推荐用于正式预设开发，通过继承类定义默认模板。
- `SummonManager`
  - 召唤物运行时管理器。
  - 负责更新目标、移动、本体碰撞、攻击、同步、槽位回收。
- `SummonBullet`
  - 召唤物实例。
  - 继承普通 `Bullet` 的基础数据，并补充 owner、definition、slot、target、state 等召唤字段。
- `SummonSlotManager`
  - 玩家召唤槽位管理。
- `SummonRegistry`
  - 内置召唤物模板注册表。

## SummonDefinition 常用字段

- `slotCost`
  - 占用多少召唤槽。
- `life`
  - 召唤物寿命，单位 tick。
- `texture` / `color` / `size` / `rendererType` / `customData`
  - 外观和渲染配置。
- `followRange`
  - 目标选择和跟随的最大范围。
- `attackRange`
  - 攻击行为的有效距离。
- `leashRange`
  - 与主人最大允许距离，超出后会强制回归。
- `moveSpeed` / `acceleration`
  - 移动速度和转向加速度。
- `idleHeight` / `idleRadius`
  - 待机绕主人的高度和半径。
- `retargetIntervalTicks`
  - 重新选目标的频率。
- `syncIntervalTicks`
  - 服务端向客户端同步召唤物快照的频率。
- `collisionShape`
  - 本体碰撞箱。近战/碰撞类召唤物必须设置。
- `bodyCollisionIntervalTicks`
  - 本体碰撞对同一目标的最小触发间隔。
- `targetSelector`
  - 目标选择器。
- `moveController`
  - 移动控制器。
- `attackPattern`
  - 攻击模板，比如发子弹、发激光。
- `formationStrategy`
  - 无目标时围绕主人的站位策略。

## 当前推荐开发方式

召唤系统现在支持两套入口：

### 1. 蓝图继承类

适合正式预设、长期维护内容。

推荐流程：

1. 继承 `AbstractSummonBlueprint`
2. 在 `configure(...)` 里写默认模板
3. 调 `BulletAPI.registerSummonBlueprint(...)` 或 `SummonRegistry.register(...)`

如果多个召唤物类型有共同结构，可以继续继承更具体的蓝图基类，例如：

- `AbstractOrbitingSummonBlueprint`
- `AbstractContactSummonBlueprint`

这样可以把公共默认行为抽到父类里。

### 2. 链式覆写

适合测试、变体、外部模组临时调整实例参数。

示例：

```java
BulletAPI.summon(world)
    .owner(player)
    .definition(SummonPresetKeys.RAM_WISP)
    .slotCost(2)
    .damage(5.0f)
    .moveSpeed(0.48)
    .spawn();
```

这里的链式调用不是替代蓝图，而是对蓝图默认模板做生成时覆写。

## 运行流程

`SummonManager` 每个服务端 tick 的主要流程：

1. 校验主人和当前目标是否还有效。
2. 在重选目标冷却结束时执行 `targetSelector`。
3. 执行 `moveController.tickMovement(...)`。
4. 更新召唤物位置。
5. 如果配置了 `collisionShape`，执行召唤物本体碰撞。
6. 如果配置了 `attackPattern`，执行攻击模板。
7. 到达 `syncIntervalTicks` 时向客户端同步位置快照。

## 伤害来源标记

所有召唤物相关伤害都会带 `AttackSourceInfo`：

- 召唤物本体碰撞：`SUMMON_BODY`
- 召唤物发射的子弹：`SUMMON_CHILD_BULLET`
- 召唤物发射的激光：`SUMMON_CHILD_LASER`

外部系统可以用它来做增伤、过滤、特殊判定。

## 三类常见召唤物

### 1. 纯碰撞召唤物

适合本体直接撞人的召唤物。

核心配置：

- `targetSelector`
- `moveController`
- `collisionShape`
- `bodyCollisionIntervalTicks`

这类召唤物通常不需要 `attackPattern`。

典型用途：

- 贴身绕敌的近战召唤物
- 冲刺穿透类召唤物
- 类似泰拉瑞亚召唤物的本体撞击攻击

### 2. 弹幕召唤物

适合会站位并发射子弹的召唤物。

核心配置：

- `targetSelector`
- `moveController`
- `attackPattern`，例如 `ShootBulletPattern`

可选：

- `collisionShape`
  - 如果希望本体也能造成碰撞伤害

### 3. 激光召唤物

适合发射激光的召唤物。

核心配置：

- `targetSelector`
- `moveController`
- `attackPattern`，例如 `ShootLaserPattern`

`ShootLaserPattern` 当前支持两种模式：

- `laserLife > 0`
  - 短命激光
- `laserLife == -1`
  - 持续激光
  - 服务端会持续更新同一条激光
  - 丢失目标或召唤物移除时手动结束

## 本体碰撞链路

召唤物本体碰撞由 `SummonManager` 统一处理，不是攻击模板负责。

碰撞发生时的链路：

1. 发出 `BulletCollisionEvent`
2. 调用 `summon.onCollision(ctx)`
3. 如果没有取消，再应用默认伤害

`CollisionContext` 提供了这些判定方法：

- `isSummonSource()`
- `isSummonBody()`
- `isSummonChildBullet()`
- `isSummonChildLaser()`

所以你可以在外部统一监听事件，然后按伤害来源做区分。

## 碰撞召唤物示例：RAM_WISP

`RAM_WISP` 是当前内置的碰撞型示例召唤物，目标是接近泰拉那种“撞上去、穿过去、掉头再撞”的手感。

特点：

- 小碰撞箱
- 对同一目标每 2 tick 触发一次本体碰撞
- 主动追敌
- 固定方向冲刺
- 命中后继续前冲一小段距离，避免卡在大碰撞箱边缘
- 穿透后重新瞄准，再折返撞击

实现分层：

- `RamStrikeMoveController`
  - 管理接近、冲刺、穿透、折返这些移动状态
- `BulletCollisionEvent`
  - 负责在命中时通知控制器进入穿透段
  - 也可以顺便改伤害、播音效、发粒子

这个分层很重要：

- 移动状态机应该写在 `moveController`
- 命中瞬间的响应应该写在碰撞事件里

不要试图只靠碰撞回调实现完整撞击 AI。碰撞回调只会在命中时触发，不能替代每 tick 的位置和速度决策。

## 激光同步说明

激光相关现在分两层：

- 服务端
  - 支持创建短命激光
  - 支持创建 `life = -1` 的持续激光
  - 支持持续更新同一条激光的起点和方向
- 客户端
  - 收到激光快照后立即应用
  - 渲染仍然使用 `prev -> current -> partialTicks` 插帧
  - 不再额外叠一层追目标平滑

这样做的目的是让可见激光锚点更接近服务端命中判定。

## 新增召唤物模板的推荐步骤

1. 在 `SummonPresetKeys` 里加 key。
2. 新建一个蓝图类，继承 `AbstractSummonBlueprint` 或更具体的蓝图父类。
3. 在蓝图类里配置默认模板。
4. 在 `SummonRegistry.bootstrapDefaults()` 或外部初始化流程中注册蓝图。
5. 选择合适的 `targetSelector`。
6. 选择或新建 `moveController`。
7. 如果本体要撞人，设置 `collisionShape` 和 `bodyCollisionIntervalTicks`。
8. 如果要发弹幕或激光，配置 `attackPattern`。
9. 在 `TestEvent` 里加测试触发。
10. 如有需要，监听 `BulletCollisionEvent` 或 `LaserCollisionEvent` 做特殊效果。

## 各层职责建议

`targetSelector` 适合处理：

- 选最近敌人
- 选最低血敌人
- Boss 优先
- 过滤有效目标

`moveController` 适合处理：

- 绕主人待机
- 追敌
- 保持距离
- 冲刺
- 穿透
- 掉头折返

`attackPattern` 适合处理：

- 发射子弹
- 发射激光
- 爆发型攻击
- 持续束维护

事件层适合处理：

- 修改伤害
- 命中特效
- 来源筛选
- 命中后通知移动控制器切换状态
