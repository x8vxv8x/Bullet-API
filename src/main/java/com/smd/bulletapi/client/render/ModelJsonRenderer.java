package com.smd.bulletapi.client.render;

import com.smd.bulletapi.client.ClientBullet;
import com.smd.bulletapi.common.RenderStateData;
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
        String renderState = RenderStateData.getRenderState(data);
        String modelKey = RenderStateData.scopedKey("model", renderState);
        String variantKey = RenderStateData.scopedKey("variant", renderState);
        String model = data.hasKey(modelKey) ? data.getString(modelKey) : (data.hasKey("model") ? data.getString("model") : "");
        String variant = data.hasKey(variantKey) ? data.getString(variantKey) : (data.hasKey("variant") ? data.getString("variant") : "inventory");
        return ModelCache.getJsonModel(model, variant);
    }
}
