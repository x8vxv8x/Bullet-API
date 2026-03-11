package com.smd.bulletapi;

import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.proxy.CommonProxy;
import com.smd.bulletapi.server.Bullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
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

    public static BulletBuilder bullet(World world) {
        return new BulletBuilder(world);
    }

    public static class BulletBuilder {
        private final World world;
        private Vec3d position;
        private Vec3d velocity;
        private int life = 100;
        private float damage = 1.0f;
        private String texture;
        private int color = 0xFFFFFF;          // 默认白色
        private float size = 0.5f;               // 默认0.5
        private String rendererType;              // 默认null（自动选择）
        private final NBTTagCompound customData = new NBTTagCompound();
        private ICollisionShape collisionShape;
        private Consumer<CollisionContext> onCollision;
        private Consumer<Bullet> tickCallback;
        private boolean onlyPlayer = false;
        private EntityLivingBase shooter;
        private ItemStack shooterHeldItem;

        private BulletBuilder(World world) {
            this.world = world;
        }

        // 必须设置
        public BulletBuilder position(Vec3d pos) {
            this.position = pos;
            return this;
        }

        public BulletBuilder velocity(Vec3d vel) {
            this.velocity = vel;
            return this;
        }

        // 可选
        public BulletBuilder life(int life) {
            this.life = life;
            return this;
        }

        public BulletBuilder damage(float damage) {
            this.damage = damage;
            return this;
        }

        public BulletBuilder texture(String texture) {
            this.texture = texture;
            return this;
        }

        public BulletBuilder onlyPlayer(boolean onlyPlayer) {
            this.onlyPlayer = onlyPlayer;
            return this;
        }

        // 新增直接设置颜色、尺寸、渲染器类型
        public BulletBuilder color(int rgb) {
            this.color = rgb;
            return this;
        }

        public BulletBuilder size(float size) {
            this.size = size;
            return this;
        }

        public BulletBuilder rendererType(String type) {
            this.rendererType = type;
            return this;
        }

        // 向 customData 中添加额外数据
        public BulletBuilder set(String key, String value) {
            customData.setString(key, value);
            return this;
        }

        public BulletBuilder set(String key, int value) {
            customData.setInteger(key, value);
            return this;
        }

        public BulletBuilder set(String key, float value) {
            customData.setFloat(key, value);
            return this;
        }

        public BulletBuilder set(String key, boolean value) {
            customData.setBoolean(key, value);
            return this;
        }

        public BulletBuilder collisionShape(ICollisionShape shape) {
            this.collisionShape = shape;
            return this;
        }

        public BulletBuilder onCollision(Consumer<CollisionContext> callback) {
            this.onCollision = callback;
            return this;
        }

        public BulletBuilder onTick(Consumer<Bullet> callback) {
            this.tickCallback = callback;
            return this;
        }

        public BulletBuilder shooter(EntityLivingBase shooter) {
            this.shooter = shooter;
            return this;
        }

        public BulletBuilder shooterHeldItem(ItemStack item) {
            this.shooterHeldItem = item == null ? null : item.copy();
            return this;
        }

        public int spawn() {
            if (position == null) throw new IllegalStateException("Position must be set");
            if (velocity == null) throw new IllegalStateException("Velocity must be set");
            if (world.isRemote) throw new IllegalStateException("Cannot spawn bullet on client side");

            return DanmakuManager.getInstance().spawnBullet(
                    world, position, velocity, life, damage,
                    texture, color, size, rendererType, customData,
                    collisionShape, onCollision, tickCallback, onlyPlayer,
                    shooter, shooterHeldItem
            );
        }
    }

    // ========== 其他静态 API ==========
    public static void removeBullet(World world, int id) {
        DanmakuManager.getInstance().removeBullet(world, id);
    }

    public static void updateBulletVelocity(World world, int id, Vec3d newVelocity) {
        DanmakuManager.getInstance().updateBulletVelocity(world, id, newVelocity);
    }
}
