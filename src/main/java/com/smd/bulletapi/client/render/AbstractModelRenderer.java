package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

@SideOnly(Side.CLIENT)
public abstract class AbstractModelRenderer implements IBulletRenderer {
    @Override
    public void beginRender() {
        // 模型渲染需要写入深度并开启背面剔除，否则会出现“透视穿模”效果。
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }

    @Override
    public void endRender() {
        // 恢复为弹幕渲染主循环的通用状态。
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
    }

    @Override
    public boolean canBatch() {
        return false;
    }

    @Override
    public void render(ClientBullet bullet, float partialTicks, double viewX, double viewY, double viewZ) {
        IBakedModel model = resolveModel(bullet);
        if (model == null) return;

        double x = bullet.getRenderX(partialTicks) - viewX;
        double y = bullet.getRenderY(partialTicks) - viewY;
        double z = bullet.getRenderZ(partialTicks) - viewZ;

        NBTTagCompound data = bullet.getCustomData();
        float scale = data.hasKey("scale") ? data.getFloat("scale") : bullet.getSize();
        String rotMode = data.hasKey("rot_mode") ? data.getString("rot_mode") : "velocity";
        int tint = data.hasKey("tint") ? data.getInteger("tint") : bullet.getColor();
        int argb = 0xFF000000 | (tint & 0x00FFFFFF);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        applyRotation(bullet, rotMode, data);
        GlStateManager.scale(scale, scale, scale);

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
        renderAllQuads(model, buf, argb, bullet.getId());
        tess.draw();

        GlStateManager.popMatrix();
    }

    protected abstract IBakedModel resolveModel(ClientBullet bullet);

    private static void renderAllQuads(IBakedModel model, BufferBuilder buf, int color, long seed) {
        renderQuadList(model.getQuads(null, null, seed), buf, color);
        for (EnumFacing facing : EnumFacing.VALUES) {
            renderQuadList(model.getQuads(null, facing, seed), buf, color);
        }
    }

    private static void renderQuadList(List<BakedQuad> quads, BufferBuilder buf, int color) {
        if (quads == null || quads.isEmpty()) return;
        for (BakedQuad quad : quads) {
            LightUtil.renderQuadColor(buf, quad, color);
        }
    }

    private static void applyRotation(ClientBullet bullet, String mode, NBTTagCompound data) {
        if ("face_camera".equalsIgnoreCase(mode)) {
            GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0F, 1F, 0F);
            GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1F, 0F, 0F);
            return;
        }

        if ("fixed".equalsIgnoreCase(mode)) {
            float yaw = data.hasKey("yaw") ? data.getFloat("yaw") : 0F;
            float pitch = data.hasKey("pitch") ? data.getFloat("pitch") : 0F;
            float roll = data.hasKey("roll") ? data.getFloat("roll") : 0F;
            GlStateManager.rotate(yaw, 0F, 1F, 0F);
            GlStateManager.rotate(pitch, 1F, 0F, 0F);
            GlStateManager.rotate(roll, 0F, 0F, 1F);
            return;
        }

        Vec3d velocity = bullet.getVelocity();
        double vx = velocity.x;
        double vy = velocity.y;
        double vz = velocity.z;
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        if (horizontal < 1.0E-6 && Math.abs(vy) < 1.0E-6) return;

        float yaw = (float) (Math.atan2(vx, vz) * 180.0D / Math.PI);
        float pitch = (float) (-Math.atan2(vy, horizontal) * 180.0D / Math.PI);
        GlStateManager.rotate(yaw, 0F, 1F, 0F);
        GlStateManager.rotate(pitch, 1F, 0F, 0F);
    }
}
