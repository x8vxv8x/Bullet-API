package com.smd.bulletapi;

import com.smd.bulletapi.common.AttackSourceInfo;
import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.DanmakuManager;
import com.smd.bulletapi.common.LaserCollisionContext;
import com.smd.bulletapi.common.collision.ICollisionShape;
import com.smd.bulletapi.common.summon.SummonDefinition;
import com.smd.bulletapi.common.summon.SummonManager;
import com.smd.bulletapi.common.summon.SummonRegistry;
import com.smd.bulletapi.network.PacketHandler;
import com.smd.bulletapi.proxy.CommonProxy;
import com.smd.bulletapi.server.Bullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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
        MinecraftForge.EVENT_BUS.register(SummonManager.getInstance());
        MinecraftForge.EVENT_BUS.register(SummonManager.getInstance().getSlotManager());
        SummonRegistry.bootstrapDefaults();
        proxy.init(event);
    }

    // ========== 建造者 API ==========

    public static BulletBuilder bullet(World world) {
        return new BulletBuilder(world);
    }

    public static LaserBuilder laser(World world) {
        return new LaserBuilder(world);
    }

    public static SummonBuilder summon(World world) {
        return new SummonBuilder(world);
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
        private AttackSourceInfo attackSourceInfo;

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

        public BulletBuilder attackSourceInfo(AttackSourceInfo attackSourceInfo) {
            this.attackSourceInfo = attackSourceInfo;
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
                    shooter, shooterHeldItem, attackSourceInfo
            );
        }
    }

    public static class LaserBuilder {
        private final World world;
        private Vec3d start;
        private Vec3d direction;
        private double maxLength = 20.0;
        private float thickness = 0.4f;
        private int life = -1;
        private float damage = 1.0f;
        private int color = 0xFF3333;
        private String rendererType = "laser_beam";
        private final NBTTagCompound customData = new NBTTagCompound();
        private boolean penetrate = false;
        private boolean followShooter = true;
        private boolean onlyPlayer = false;
        private boolean blockStops = true;
        private Vec3d startOffset = new Vec3d(0, 0, 0);
        private Vec3d startOffsetLocal = new Vec3d(0, 0, 0);
        private int eventIntervalTicks = 0;
        private Consumer<LaserCollisionContext> onCollision;
        private EntityLivingBase shooter;
        private ItemStack shooterHeldItem;
        private AttackSourceInfo attackSourceInfo;

        private LaserBuilder(World world) {
            this.world = world;
        }

        public LaserBuilder start(Vec3d start) {
            this.start = start;
            return this;
        }

        public LaserBuilder direction(Vec3d direction) {
            this.direction = direction;
            return this;
        }

        public LaserBuilder startOffset(Vec3d offset) {
            this.startOffset = offset == null ? new Vec3d(0, 0, 0) : offset;
            return this;
        }

        /**
         * 本地空间偏移：x=右，y=上，z=前（沿视线方向）
         */
        public LaserBuilder startOffsetLocal(Vec3d offset) {
            this.startOffsetLocal = offset == null ? new Vec3d(0, 0, 0) : offset;
            return this;
        }

        /**
         * 控制碰撞事件触发间隔（tick）。0 表示每次碰撞都触发。
         */
        public LaserBuilder eventIntervalTicks(int ticks) {
            this.eventIntervalTicks = Math.max(0, ticks);
            return this;
        }

        public LaserBuilder maxLength(double maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public LaserBuilder thickness(float thickness) {
            this.thickness = thickness;
            return this;
        }

        public LaserBuilder life(int life) {
            this.life = life;
            return this;
        }

        public LaserBuilder damage(float damage) {
            this.damage = damage;
            return this;
        }

        public LaserBuilder color(int rgb) {
            this.color = rgb;
            return this;
        }

        public LaserBuilder rendererType(String type) {
            this.rendererType = type;
            return this;
        }

        public LaserBuilder set(String key, String value) {
            customData.setString(key, value);
            return this;
        }

        public LaserBuilder set(String key, int value) {
            customData.setInteger(key, value);
            return this;
        }

        public LaserBuilder set(String key, float value) {
            customData.setFloat(key, value);
            return this;
        }

        public LaserBuilder set(String key, boolean value) {
            customData.setBoolean(key, value);
            return this;
        }

        public LaserBuilder penetrate(boolean penetrate) {
            this.penetrate = penetrate;
            return this;
        }

        public LaserBuilder followShooter(boolean followShooter) {
            this.followShooter = followShooter;
            return this;
        }

        public LaserBuilder onlyPlayer(boolean onlyPlayer) {
            this.onlyPlayer = onlyPlayer;
            return this;
        }

        public LaserBuilder blockStops(boolean blockStops) {
            this.blockStops = blockStops;
            return this;
        }

        public LaserBuilder onCollision(Consumer<LaserCollisionContext> callback) {
            this.onCollision = callback;
            return this;
        }

        public LaserBuilder shooter(EntityLivingBase shooter) {
            this.shooter = shooter;
            return this;
        }

        public LaserBuilder shooterHeldItem(ItemStack item) {
            this.shooterHeldItem = item == null ? null : item.copy();
            return this;
        }

        public LaserBuilder attackSourceInfo(AttackSourceInfo attackSourceInfo) {
            this.attackSourceInfo = attackSourceInfo;
            return this;
        }

        public int spawn() {
            if (world.isRemote) throw new IllegalStateException("Cannot spawn laser on client side");
            if (start == null && !followShooter) throw new IllegalStateException("Start must be set or followShooter enabled");
            if (direction == null && !followShooter) throw new IllegalStateException("Direction must be set or followShooter enabled");

            return DanmakuManager.getInstance().spawnLaser(
                    world,
                    start,
                    direction,
                    maxLength,
                    thickness,
                    life,
                    damage,
                    color,
                    rendererType,
                    customData,
                    penetrate,
                    followShooter,
                    onlyPlayer,
                    blockStops,
                    startOffset,
                    startOffsetLocal,
                    eventIntervalTicks,
                    onCollision,
                    shooter,
                    shooterHeldItem,
                    attackSourceInfo
            );
        }
    }

    public static class SummonBuilder {
        private final World world;
        private EntityLivingBase owner;
        private String definitionId;
        private SummonDefinition definition;

        private SummonBuilder(World world) {
            this.world = world;
        }

        public SummonBuilder owner(EntityLivingBase owner) {
            this.owner = owner;
            return this;
        }

        public SummonBuilder definition(String definitionId) {
            this.definitionId = definitionId;
            this.definition = null;
            return this;
        }

        public SummonBuilder definition(SummonDefinition definition) {
            this.definition = definition == null ? null : definition.copy();
            this.definitionId = definition == null ? null : definition.getId();
            return this;
        }

        private SummonDefinition resolveDefinition() {
            SummonDefinition resolved = definition == null
                    ? (definitionId == null ? null : SummonRegistry.get(definitionId))
                    : definition.copy();
            if (resolved == null) {
                throw new IllegalStateException("Summon definition must be set");
            }
            return resolved;
        }

        public SummonBuilder slotCost(int slotCost) {
            resolveDefinitionForMutation().slotCost(slotCost);
            return this;
        }

        public SummonBuilder life(int life) {
            resolveDefinitionForMutation().life(life);
            return this;
        }

        public SummonBuilder damage(float damage) {
            resolveDefinitionForMutation().damage(damage);
            return this;
        }

        public SummonBuilder texture(String texture) {
            resolveDefinitionForMutation().texture(texture);
            return this;
        }

        public SummonBuilder color(int color) {
            resolveDefinitionForMutation().color(color);
            return this;
        }

        public SummonBuilder size(float size) {
            resolveDefinitionForMutation().size(size);
            return this;
        }

        public SummonBuilder rendererType(String rendererType) {
            resolveDefinitionForMutation().rendererType(rendererType);
            return this;
        }

        public SummonBuilder collisionShape(ICollisionShape collisionShape) {
            resolveDefinitionForMutation().collisionShape(collisionShape);
            return this;
        }

        public SummonBuilder followRange(double range) {
            resolveDefinitionForMutation().followRange(range);
            return this;
        }

        public SummonBuilder attackRange(double range) {
            resolveDefinitionForMutation().attackRange(range);
            return this;
        }

        public SummonBuilder leashRange(double range) {
            resolveDefinitionForMutation().leashRange(range);
            return this;
        }

        public SummonBuilder moveSpeed(double speed) {
            resolveDefinitionForMutation().moveSpeed(speed);
            return this;
        }

        public SummonBuilder acceleration(double acceleration) {
            resolveDefinitionForMutation().acceleration(acceleration);
            return this;
        }

        public SummonBuilder idleHeight(double idleHeight) {
            resolveDefinitionForMutation().idleHeight(idleHeight);
            return this;
        }

        public SummonBuilder idleRadius(double idleRadius) {
            resolveDefinitionForMutation().idleRadius(idleRadius);
            return this;
        }

        public SummonBuilder retargetIntervalTicks(int ticks) {
            resolveDefinitionForMutation().retargetIntervalTicks(ticks);
            return this;
        }

        public SummonBuilder syncIntervalTicks(int ticks) {
            resolveDefinitionForMutation().syncIntervalTicks(ticks);
            return this;
        }

        public SummonBuilder bodyCollisionIntervalTicks(int ticks) {
            resolveDefinitionForMutation().bodyCollisionIntervalTicks(ticks);
            return this;
        }

        public SummonBuilder targetSelector(com.smd.bulletapi.common.summon.behavior.ISummonTargetSelector selector) {
            resolveDefinitionForMutation().targetSelector(selector);
            return this;
        }

        public SummonBuilder moveController(com.smd.bulletapi.common.summon.behavior.ISummonMoveController controller) {
            resolveDefinitionForMutation().moveController(controller);
            return this;
        }

        public SummonBuilder attackPattern(com.smd.bulletapi.common.summon.behavior.ISummonAttackPattern pattern) {
            resolveDefinitionForMutation().attackPattern(pattern);
            return this;
        }

        public SummonBuilder formationStrategy(com.smd.bulletapi.common.summon.behavior.IFormationStrategy strategy) {
            resolveDefinitionForMutation().formationStrategy(strategy);
            return this;
        }

        public SummonBuilder set(String key, String value) {
            resolveDefinitionForMutation().set(key, value);
            return this;
        }

        public SummonBuilder set(String key, int value) {
            resolveDefinitionForMutation().set(key, value);
            return this;
        }

        public SummonBuilder set(String key, float value) {
            resolveDefinitionForMutation().set(key, value);
            return this;
        }

        public SummonBuilder set(String key, boolean value) {
            resolveDefinitionForMutation().set(key, value);
            return this;
        }

        public int spawn() {
            if (world.isRemote) throw new IllegalStateException("Cannot spawn summon on client side");
            if (owner == null) throw new IllegalStateException("Summon owner must be set");
            return SummonManager.getInstance().spawnSummon(world, owner, resolveDefinition());
        }

        private SummonDefinition resolveDefinitionForMutation() {
            if (definition == null) {
                definition = definitionId == null ? null : SummonRegistry.get(definitionId);
            }
            if (definition == null) {
                throw new IllegalStateException("Summon definition must be set before overriding properties");
            }
            return definition;
        }
    }

    // ========== 其他静态 API ==========
    public static void removeBullet(World world, int id) {
        DanmakuManager.getInstance().removeBullet(world, id);
    }

    public static void updateBulletVelocity(World world, int id, Vec3d newVelocity) {
        DanmakuManager.getInstance().updateBulletVelocity(world, id, newVelocity);
    }

    public static void removeLaser(World world, int id) {
        DanmakuManager.getInstance().removeLaser(world, id);
    }

    public static void removeSummon(World world, int id) {
        SummonManager.getInstance().removeSummon(world, id);
    }

    public static int getSummonCount(World world) {
        return SummonManager.getInstance().getSummonCount(world);
    }

    public static int getPlayerMaxSummonSlots(EntityPlayer player) {
        return SummonManager.getInstance().getSlotManager().getMaxSlots(player);
    }

    public static int getPlayerUsedSummonSlots(EntityPlayer player) {
        return SummonManager.getInstance().getSlotManager().getUsedSlots(player);
    }

    public static void setPlayerMaxSummonSlots(EntityPlayer player, int slots) {
        SummonManager.getInstance().setPlayerMaxSlots(player, slots);
    }
}
