# API 总览

## 对外公开入口

当前推荐开发者只使用 `com.smd.bulletapi.api` 包下的公开 API。

- `BulletApi`
  - 普通点弹幕入口
- `LaserApi`
  - 激光入口
- `SummonApi`
  - 召唤物入口

对应 Builder：

- `com.smd.bulletapi.api.builder.BulletBuilder`
- `com.smd.bulletapi.api.builder.LaserBuilder`
- `com.smd.bulletapi.api.builder.SummonBuilder`

召唤物蓝图基类：

- `com.smd.bulletapi.api.summon.AbstractSummonBlueprint`
- `com.smd.bulletapi.api.summon.AbstractOrbitingSummonBlueprint`
- `com.smd.bulletapi.api.summon.AbstractContactSummonBlueprint`

## 包结构说明

- `api`
  - 公开开发入口
- `common`
  - 公共逻辑、管理器、定义、行为接口
- `server`
  - 服务端运行时对象
- `client`
  - 客户端缓存与渲染
- `network`
  - 内部同步包

约定：

- `api` 是唯一推荐依赖的开发入口
- `network` 属于内部实现，不建议业务逻辑直接调用
- `client.render` 里的注册行为应只在客户端初始化阶段执行

## 三类核心对象

### 1. 点弹幕

适合一次性投射物、模型弹、跟踪弹、纯碰撞弹。

入口：

```java
BulletApi.builder(world)
```

### 2. 激光

适合瞬时束、持续束、穿透束、跟随视线束。

入口：

```java
LaserApi.builder(world)
```

### 3. 召唤物

适合持续存在、可追踪目标、可带碰撞或攻击模板的实体型弹幕。

入口：

```java
SummonApi.builder(world)
```

## 服务端与客户端职责

BulletAPI 的核心原则是服务端权威。

- 生成、移除、移动、碰撞、伤害判定都在服务端
- 客户端接收快照并负责表现
- 一般开发不需要手动操作内部同步包

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
    .spawn();
```

## 常见开发路径

### 只做新弹幕

看：

- `02-点弹幕开发`

### 只做新激光

看：

- `03-激光开发`

### 做新召唤物

看：

- `04-召唤物开发`

### 做新渲染表现

看：

- `05-渲染扩展`

### 做新碰撞判定

看：

- `06-碰撞扩展`

### 改同步结构

看：

- `07-网络同步与发包`
