package com.smd.bulletapi.server;

import com.smd.bulletapi.common.CollisionContext;
import com.smd.bulletapi.common.collision.ICollisionShape;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class Bullet {
    private final int id;
    private Vec3d position;
    private Vec3d velocity;
    private int life;
    private final int maxLife;
    private float damage;
    private boolean dead;
    private String texture;           // 新增：纹理路径
    private NBTTagCompound customData;
    private ICollisionShape collisionShape;   // 可为null，表示无碰撞
    private Consumer<CollisionContext> onCollision;// 新增：任意NBT数据

    public Bullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage,
                  String texture, NBTTagCompound customData,
                  ICollisionShape collisionShape, Consumer<CollisionContext> onCollision) {
        this.id = id;
        this.position = position;
        this.velocity = velocity;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.damage = damage;
        this.dead = false;
        this.texture = texture;
        this.customData = customData == null ? new NBTTagCompound() : customData;
        this.collisionShape = collisionShape;
        this.onCollision = onCollision;
    }

    public void update(World world) {
        if (dead) return;
        // 更新位置
        position = position.add(velocity);
        life--;
        if (life <= 0) {
            dead = true;
            return;
        }
    }

    public boolean isDead() {
        return dead; }
    public int getId() {
        return id; }
    public Vec3d getPosition() {
        return position; }
    public Vec3d getVelocity() {
        return velocity; }
    public void setVelocity(Vec3d velocity) {
        this.velocity = velocity; }
    public int getLife() {
        return life; }
    public float getDamage() {
        return damage; }
    public String getTexture() {
        return texture; }
    public NBTTagCompound getCustomData() {
        return customData; }
    public void setCustomData(NBTTagCompound customData) {
        this.customData = customData; }
    public ICollisionShape getCollisionShape() {
        return collisionShape; }
    public boolean hasCollision() {
        return collisionShape != null; }

    public void onCollision(World world, Entity hitEntity) {
        if (onCollision != null) {
            onCollision.accept(new CollisionContext(this, world, hitEntity));
        }
    }
}
