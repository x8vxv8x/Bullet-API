package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientBullet;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModelJsonRenderer extends AbstractModelRenderer {
    public static final ModelJsonRenderer INSTANCE = new ModelJsonRenderer();

    private ModelJsonRenderer() {}

    @Override
    protected IBakedModel resolveModel(ClientBullet bullet) {
        NBTTagCompound data = bullet.getCustomData();
        String model = data.hasKey("model") ? data.getString("model") : "";
        String variant = data.hasKey("variant") ? data.getString("variant") : "inventory";
        return ModelCache.getJsonModel(model, variant);
    }
}
