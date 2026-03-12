# 激光渲染

本 Mod 内置三种激光渲染器类型：

- `laser_beam`：简单可见的光束，默认回退渲染。
- `laser_blast`：分段、双层（核心+外壳）并带脉动效果的示例渲染。
- `laser_none`：不渲染，完全由开发者接管渲染。

## 选择内置渲染器

```java
BulletAPI.laser(world)
    .rendererType("laser_beam")   // 或 "laser_blast", "laser_none"
    .spawn();
```

## 自定义渲染器

在客户端注册渲染类型：

```java
LaserRendererRegistry.register("my_laser", data -> new MyLaserRenderer());
```

使用自定义类型：

```java
BulletAPI.laser(world)
    .rendererType("my_laser")
    .spawn();
```

## laser_blast 的 customData 参数

所有参数均可选：

- `alpha` (float)：基础透明度，默认 0.85
- `segment_len` (float)：分段长度，默认 1.0
- `core_scale` (float)：核心厚度比例，默认 0.55
- `shell_scale` (float)：外壳厚度比例，默认 1.0
- `pulse_amp` (float)：脉动幅度，默认 0.2
- `pulse_speed` (float)：脉动速度，默认 0.35
- `core_color` (int RGB)：核心颜色，默认白色
- `shell_color` (int RGB)：外壳颜色，默认激光颜色
- `shell_color_end` (int RGB)：外壳末端颜色，默认外壳颜色
```
