package com.smd.bulletapi.server;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Bullet {
    private final int id;
    private Vec3d position;
    private Vec3d velocity;
    private int life;
    private final int maxLife;
    private float damage;
    private boolean dead;

    public Bullet(int id, Vec3d position, Vec3d velocity, int maxLife, float damage) {
        this.id = id;
        this.position = position;
        this.velocity = velocity;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.damage = damage;
        this.dead = false;
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
        // 碰撞检测、伤害应用等（省略具体实现）
        // 示例：如果位置下方是空气等，这里可以添加逻辑。
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
}
