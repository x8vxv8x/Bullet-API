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

    // ========== 对外 API ==========
    public static void removeBullet(World world, int id) {
        DanmakuManager.getInstance().removeBullet(world, id);
    }

    public static void updateBulletVelocity(World world, int id, Vec3d newVelocity) {
        DanmakuManager.getInstance().updateBulletVelocity(world, id, newVelocity);
    }

    public static void spawnBullet(World world, Vec3d position, Vec3d velocity, int life, float damage) {
        DanmakuManager.getInstance().spawnBullet(world, position, velocity, life, damage);
    }

    public static void spawnBullet(World world, Vec3d position, Vec3d velocity, int life, float damage,
                                   String texture, NBTTagCompound customData,
                                   ICollisionShape collisionShape, Consumer<CollisionContext> onCollision) {
        DanmakuManager.getInstance().spawnBullet(world, position, velocity, life, damage,
                texture, customData, collisionShape, onCollision);
    }
}
