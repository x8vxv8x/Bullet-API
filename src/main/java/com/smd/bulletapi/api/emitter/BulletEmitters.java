package com.smd.bulletapi.api.emitter;

import com.smd.bulletapi.api.BulletApi;
import com.smd.bulletapi.api.annotation.PublicApi;
import com.smd.bulletapi.api.handle.BulletHandle;
import com.smd.bulletapi.api.builder.BulletBuilder;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

@PublicApi
public final class BulletEmitters {
    private static final Random RANDOM = new Random();

    private BulletEmitters() {}

    public static List<BulletHandle> ring(World world, Vec3d center, int count, double speed,
                                          Consumer<BulletBuilder> configure) {
        List<BulletHandle> handles = new ArrayList<>();
        if (world == null || center == null || count <= 0) {
            return handles;
        }

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D) * i / count;
            Vec3d velocity = new Vec3d(Math.cos(angle), 0.0D, Math.sin(angle)).scale(speed);
            handles.add(spawn(world, center, velocity, configure));
        }
        return handles;
    }

    public static List<BulletHandle> fan(World world, Vec3d origin, Vec3d direction, int count,
                                         double spreadRadians, double speed,
                                         Consumer<BulletBuilder> configure) {
        List<BulletHandle> handles = new ArrayList<>();
        if (world == null || origin == null || direction == null || count <= 0) {
            return handles;
        }

        Vec3d forward = normalize(direction);
        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 1.0E-6) {
            right = new Vec3d(1, 0, 0);
        }
        right = right.normalize();

        for (int i = 0; i < count; i++) {
            double factor = count == 1 ? 0.0D : ((double) i / (count - 1)) - 0.5D;
            Vec3d velocity = forward.add(right.scale(spreadRadians * factor)).normalize().scale(speed);
            handles.add(spawn(world, origin, velocity, configure));
        }
        return handles;
    }

    public static List<BulletHandle> line(World world, Vec3d start, Vec3d step, Vec3d velocity, int count,
                                          Consumer<BulletBuilder> configure) {
        List<BulletHandle> handles = new ArrayList<>();
        if (world == null || start == null || step == null || velocity == null || count <= 0) {
            return handles;
        }

        for (int i = 0; i < count; i++) {
            handles.add(spawn(world, start.add(step.scale(i)), velocity, configure));
        }
        return handles;
    }

    public static List<BulletHandle> burst(World world, Vec3d origin, int count, double minSpeed, double maxSpeed,
                                           Consumer<BulletBuilder> configure) {
        List<BulletHandle> handles = new ArrayList<>();
        if (world == null || origin == null || count <= 0) {
            return handles;
        }

        for (int i = 0; i < count; i++) {
            Vec3d dir = new Vec3d(RANDOM.nextDouble() * 2.0D - 1.0D,
                    RANDOM.nextDouble() * 2.0D - 1.0D,
                    RANDOM.nextDouble() * 2.0D - 1.0D);
            dir = normalize(dir);
            double speed = minSpeed + (maxSpeed - minSpeed) * RANDOM.nextDouble();
            handles.add(spawn(world, origin, dir.scale(speed), configure));
        }
        return handles;
    }

    private static BulletHandle spawn(World world, Vec3d position, Vec3d velocity,
                                      Consumer<BulletBuilder> configure) {
        BulletBuilder builder = BulletApi.builder(world)
                .position(position)
                .velocity(velocity);
        if (configure != null) {
            configure.accept(builder);
        }
        return builder.spawnHandle();
    }

    private static Vec3d normalize(Vec3d vec) {
        if (vec.lengthSquared() < 1.0E-6) {
            return new Vec3d(0, 0, 1);
        }
        return vec.normalize();
    }
}
