package com.smd.bulletapi;

import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.proxy.CommonProxy;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.function.Consumer;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class BulletAPI {

    @Mod.Instance(Tags.MOD_ID)
    public static BulletAPI instance;

    @SidedProxy(clientSide = "com.smd.bulletapi.proxy.ClientProxy",
            serverSide = "com.smd.bulletapi.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PacketHandler.registerMessages();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(DanmakuManager.getInstance());
        proxy.init(event);
    }

    // ========== 建造者 API ==========

    /**
     * 创建一个弹幕建造者（必须指定世界）
     */
    public static BulletBuilder bullet(World world) {
        return new BulletBuilder(world);
    }

    public static class BulletBuilder {
        private final World world;
        private Vec3d position;
        private Vec3d velocity;
        private int life = 100;                // 默认生命周期（5秒，20 ticks/秒）
        private float damage = 1.0f;            // 默认伤害
        private String texture;                 // 纹理路径（可为null）
        private final NBTTagCompound customData = new NBTTagCompound();
        private ICollisionShape collisionShape;  // 碰撞形状（可为null）
        private Consumer<CollisionContext> onCollision; // 碰撞回调（可为null）

        private BulletBuilder(World world) {
            this.world = world;
        }

        /** 必须设置：弹幕初始位置 */
        public BulletBuilder position(Vec3d pos) {
            this.position = pos;
            return this;
        }

        /** 必须设置：弹幕速度向量 */
        public BulletBuilder velocity(Vec3d vel) {
            this.velocity = vel;
            return this;
        }

        /** 可选：生命周期（单位：tick） */
        public BulletBuilder life(int life) {
            this.life = life;
            return this;
        }

        /** 可选：伤害值 */
        public BulletBuilder damage(float damage) {
            this.damage = damage;
            return this;
        }

        /** 可选：纹理路径（客户端会根据此路径自动加载纹理，若为null则使用点精灵） */
        public BulletBuilder texture(String texture) {
            this.texture = texture;
            return this;
        }

        /** 直接设置整个自定义数据标签（会覆盖之前的设置） */
        public BulletBuilder customData(NBTTagCompound data) {
            this.customData.merge(data); // 合并而不是替换，避免丢失已有键
            return this;
        }

        /** 向自定义数据中添加一个字符串值 */
        public BulletBuilder set(String key, String value) {
            customData.setString(key, value);
            return this;
        }

        /** 向自定义数据中添加一个整数值 */
        public BulletBuilder set(String key, int value) {
            customData.setInteger(key, value);
            return this;
        }

        /** 向自定义数据中添加一个浮点值 */
        public BulletBuilder set(String key, float value) {
            customData.setFloat(key, value);
            return this;
        }

        /** 向自定义数据中添加一个布尔值 */
        public BulletBuilder set(String key, boolean value) {
            customData.setBoolean(key, value);
            return this;
        }

        // ========== 常用渲染属性的快捷方法（自动写入 customData） ==========
        /** 设置弹幕颜色（RGB整数，如 0xFF3333） */
        public BulletBuilder color(int rgb) {
            customData.setInteger("Color", rgb);
            return this;
        }

        /** 设置弹幕渲染尺寸 */
        public BulletBuilder size(float size) {
            customData.setFloat("Size", size);
            return this;
        }

        /** 显式指定客户端渲染器类型（如 "billboard", "point", "entity_model" 等） */
        public BulletBuilder rendererType(String type) {
            customData.setString("RendererType", type);
            return this;
        }

        // ========== 碰撞相关 ==========
        public BulletBuilder collisionShape(ICollisionShape shape) {
            this.collisionShape = shape;
            return this;
        }

        public BulletBuilder onCollision(Consumer<CollisionContext> callback) {
            this.onCollision = callback;
            return this;
        }

        /**
         * 生成弹幕并返回其唯一ID
         * @return 弹幕ID（可用于后续更新或移除）
         * @throws IllegalStateException 如果未设置必要参数（position/velocity）或世界为客户端
         */
        public int spawn() {
            if (position == null) throw new IllegalStateException("Position must be set");
            if (velocity == null) throw new IllegalStateException("Velocity must be set");
            if (world.isRemote) throw new IllegalStateException("Cannot spawn bullet on client side");

            return DanmakuManager.getInstance().spawnBullet(
                    world, position, velocity, life, damage,
                    texture, customData, collisionShape, onCollision
            );
        }
    }

    // ========== 其他静态 API（移除 spawnBullet，保留管理方法） ==========
    public static void removeBullet(World world, int id) {
        DanmakuManager.getInstance().removeBullet(world, id);
    }

    public static void updateBulletVelocity(World world, int id, Vec3d newVelocity) {
        DanmakuManager.getInstance().updateBulletVelocity(world, id, newVelocity);
    }
}