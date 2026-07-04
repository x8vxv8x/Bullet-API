# API 总览

## 先看什么

当前版本推荐附属 mod 作者按下面的顺序理解系统：

1. `Public API`
   - 直接调用，创建对象、查询状态、操作句柄
2. `SPI`
   - 自己实现接口，接入运行时行为
3. `Event`
   - 监听生命周期和碰撞事件，做跨系统联动
4. `Internal`
   - 只用于理解原理，不应依赖

## Public API

主入口类：

- `BulletApi`
  - 点弹幕创建与句柄入口
- `LaserApi`
  - 激光创建与句柄入口
- `SummonApi`
  - 召唤物创建、注册定义/蓝图、槽位操作
- `Battlefield`
  - 当前世界中的弹幕、激光、召唤物查询入口

常用 Builder：

- `BulletBuilder`
- `LaserBuilder`
- `SummonBuilder`

常用控制对象：

- `BulletHandle`
- `LaserHandle`
- `SummonHandle`

常用只读对象：

- `BulletSnapshot`
- `LaserSnapshot`
- `SummonSnapshot`

## 四层结构

当前版本推荐按下面四层理解公开 API：

### Builder

- 负责生成对象
- 例如 `BulletApi.builder(world)`、`LaserApi.builder(world)`、`SummonApi.builder(world)`

### Handle

- 负责控制单个对象
- 例如移除、改位置、改速度、改寿命、改目标

### Battlefield

- 负责查询当前世界中的对象集合
- 例如数量、id 列表、快照列表、按 id 取 `Handle` 或 `Snapshot`

### Snapshot

- 负责提供精简只读状态
- 适合日志、调试、事件透传、跨系统读取

## SPI

下面这些是附属 mod 可以主动实现的稳定接口：

### 点弹幕

- `IBulletHitBehavior`
  - 单颗弹幕命中时的本地命中逻辑
- `IBulletMotionController`
  - 单颗弹幕每 tick 运动控制
- `IBulletCollisionFilter`
  - 单颗弹幕命中前的过滤规则

### 激光

- `ILaserHitBehavior`
  - 激光命中逻辑
- `ILaserCollisionFilter`
  - 激光命中过滤规则

### 召唤物行为

- `ISummonTargetSelector`
- `ISummonMoveController`
- `ISummonAttackPattern`
- `IFormationStrategy`

## 参数载荷补充

当前版本里对象自定义参数统一走轻量 payload：

- 外部仍然用 `set(key, value)` 写参数
- 渲染器和运行时行为仍然按 key 读取参数
- 但内部不再把这些高频参数建立在 `NBTTagCompound` 之上

### 其他扩展

- `ICombatRelationResolver`
  - 跨系统阵营/友伤关系判断
- `IBulletRenderer`
- `ILaserRenderer`
  - 客户端自定义渲染器
- `BulletPreset`
- `LaserPreset`
- `AbstractSummonBlueprint`
  - 定义可复用预设和蓝图

## Event

公开事件主要分三类：

### 生命周期事件

- `BulletSpawnEvent`
- `BulletRemoveEvent`
- `LaserSpawnEvent`
- `LaserRemoveEvent`
- `SummonSpawnEvent`
- `SummonRemoveEvent`
- `SummonStateChangedEvent`
- `SummonTargetChangedEvent`
- `SummonSlotChangedEvent`
- `BulletPresetRegisteredEvent`
- `LaserPresetRegisteredEvent`
- `SummonDefinitionRegisteredEvent`

### 碰撞事件

- `BulletCollisionEvent`
- `LaserCollisionEvent`

### 什么时候该用事件

- 你要做全局联动
- 你要观察或拦截别的附属 mod 创建出来的对象
- 你不想把逻辑绑死在某一个 builder 或某一个蓝图上

## Internal

以下内容即使可见，也不应该作为附属 mod 的稳定依赖：

- `DanmakuManager`
- `SummonManager`
- `common.runtime.*`
- `SummonRegistry`
- `server.*`
- `network.*`
- `ClientDanmakuCache`
- `ClientLaserCache`
- `ClientSummonCache`
- `SPacket*`
- `PacketHandler`

## 三类核心对象

### 点弹幕

适合一次性投射物、模型弹、追踪弹、纯判定弹。

入口：

```java
BulletApi.builder(world)
```

### 激光

适合瞬发束、持续束、穿透束、跟随束。

入口：

```java
LaserApi.builder(world)
```

### 召唤物

适合持续存在、会选目标、会移动、会执行攻击模板的运行时对象。

入口：

```java
SummonApi.builder(world)
```

查询：

```java
Battlefield.of(world).summons()
```

## 服务端权威

当前版本默认是服务端权威：

- 生成、移除、命中、伤害都在服务端
- 客户端负责缓存、插值和表现
- 附属 mod 一般不需要也不应该直接操作内部同步包

## 最小示例

### 创建点弹幕

```java
BulletApi.builder(world)
    .position(pos)
    .velocity(vel)
    .life(60)
    .damage(2.0f)
    .spawn();
```

### 创建激光

```java
LaserApi.builder(world)
    .start(start)
    .direction(dir)
    .maxLength(24.0)
    .life(20)
    .spawn();
```

### 创建召唤物

```java
SummonApi.builder(world)
    .owner(player)
    .definition("bulletapi:ram_wisp")
    .position(pos)
    .spawn();
```

### 查询世界中的对象

```java
Battlefield battlefield = Battlefield.of(world);

int bulletCount = battlefield.bullets().count();
SummonSnapshot summon = battlefield.summons().get(id);
LaserHandle laser = battlefield.lasers().handle(id);
```

## 阅读路径

- 只做点弹幕：看 `02-点弹幕开发`
- 只做激光：看 `03-激光开发`
- 只做召唤物：看 `04-召唤物开发`
- 做客户端渲染：看 `05-渲染扩展`
- 做判定和阵营规则：看 `06-碰撞扩展`
- 想理解同步原理和内部边界：看 `07-网络同步与发包`
