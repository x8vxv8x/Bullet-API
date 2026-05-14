package com.smd.bulletapi.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SideOnly(Side.CLIENT)
public final class ModelCache {
    private static final Map<String, IBakedModel> BAKED_MODELS = new ConcurrentHashMap<>();

    private ModelCache() {}

    public static IBakedModel getJsonModel(String model, String variant) {
        if (model == null || model.isEmpty()) {
            return getMissingModel();
        }

        String resolvedVariant = (variant == null || variant.isEmpty()) ? "inventory" : variant;
        String key = "json|" + model + "#" + resolvedVariant;
        return BAKED_MODELS.computeIfAbsent(key, unused -> bakeJson(model, resolvedVariant));
    }

    public static void clear() {
        BAKED_MODELS.clear();
    }

    private static IBakedModel bakeJson(String modelPath, String variant) {
        try {
            ResourceLocation location;
            if (modelPath.contains("#")) {
                location = new ModelResourceLocation(modelPath);
            } else {
                location = new ModelResourceLocation(new ResourceLocation(modelPath), variant);
            }
            return bakeModel(location);
        } catch (Exception ignored) {
            return getMissingModel();
        }
    }

    private static IBakedModel bakeModel(ResourceLocation location) throws Exception {
        IModel model = ModelLoaderRegistry.getModel(location);
        Function<ResourceLocation, TextureAtlasSprite> spriteGetter =
                spriteLocation -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(spriteLocation.toString());
        return model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM, spriteGetter);
    }

    private static IBakedModel getMissingModel() {
        return Minecraft.getMinecraft()
                .getRenderItem()
                .getItemModelMesher()
                .getModelManager()
                .getMissingModel();
    }
}
