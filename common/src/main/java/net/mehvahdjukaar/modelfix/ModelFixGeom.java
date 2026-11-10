package net.mehvahdjukaar.modelfix;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

import static net.mehvahdjukaar.modelfix.ModelFix.*;

public class ModelFixGeom {

    private static final Identifier BLOCK_ATLAS = Identifier.parse("textures/atlas/blocks.png");

    /**
     * 1.21.11: vanilla no longer has TextureAtlasSprite#uvShrinkRatio. The same atlas
     * bleeding protection now lives in the TextureAtlasSprite constructor, which insets
     * the sprite UV bounds (u0/u1/v0/v1) by {@code padding} texels. This returns the
     * multiplier that should be applied to that padding for the given atlas:
     * 0 on the block atlas with default (non-mac) config to remove item texture gaps,
     * the configured ratio on mac, or -1 when nothing should be done.
     */
    public static float getAtlasPaddingMultiplier(Identifier atlasLocation) {
        if (atlasLocation.equals(BLOCK_ATLAS)) {
            return shrinkMult.get().floatValue();
        }
        return -1;
    }

    /**
     * 1.21.11: ItemModelGenerator#createSideElements bakes a 0.1 texel UV shrink into every
     * generated side face (u goes from x+0.1 to x+0.9, scaled by 16/textureSize). We undo the
     * (1 - shrinkMult) portion of that shrink. A single texel face has UV span 0.8 * scale,
     * so the baked-in shrink is exactly span / 8. Each endpoint is pushed back outwards,
     * which also handles the inverted V axis of vertical (left/right) faces.
     */
    public static void unshrinkSideFaceUVs(List<BlockElement> elements) {
        double mult = 1 - shrinkMult.get();
        if (mult <= 0) return;
        for (int i = 0; i < elements.size(); i++) {
            BlockElement e = elements.get(i);
            Map<Direction, BlockElementFace> faces = e.faces();
            if (faces.size() != 1) continue;
            Map.Entry<Direction, BlockElementFace> entry = faces.entrySet().iterator().next();
            BlockElementFace face = entry.getValue();
            BlockElementFace.UVs uv = face.uvs();
            float du = Math.abs(uv.maxU() - uv.minU()) / 8f;
            float dv = Math.abs(uv.maxV() - uv.minV()) / 8f;
            du *= mult;
            dv *= mult;
            boolean uNormal = uv.minU() <= uv.maxU();
            boolean vNormal = uv.minV() <= uv.maxV();
            float nMinU = uNormal ? uv.minU() - du : uv.minU() + du;
            float nMaxU = uNormal ? uv.maxU() + du : uv.maxU() - du;
            float nMinV = vNormal ? uv.minV() - dv : uv.minV() + dv;
            float nMaxV = vNormal ? uv.maxV() + dv : uv.maxV() - dv;
            BlockElementFace.UVs newUvs = new BlockElementFace.UVs(nMinU, nMinV, nMaxU, nMaxV);
            BlockElementFace newFace = new BlockElementFace(face.cullForDirection(), face.tintIndex(),
                    face.texture(), newUvs, face.rotation());
            elements.set(i, new BlockElement(e.from(), e.to(), Map.of(entry.getKey(), newFace)));
        }
    }

    //who needs anti atlas bleeding when it doesn't occur even on mipmap 4 high render distance lol
    //apparently on mac os it does waaa
    public static void enlargeFaces(List<BlockElement> original) {
        double inc = indent.get();
        double inc2 = expansion.get();
        for (var e : original) {
            Vector3f from = (Vector3f) e.from();
            Vector3f to = (Vector3f) e.to();

            var set = e.faces().keySet();
            if (set.size() == 1) {
                var dir = set.stream().findAny().get();
                switch (dir) {
                    case UP -> {
                        from.set(from.x() - inc2, from.y() - inc, from.z() - inc2);
                        to.set(to.x() + inc2, to.y() - inc, to.z() + inc2);
                    }
                    case DOWN -> {
                        from.set(from.x() - inc2, from.y() + inc, from.z() - inc2);
                        to.set(to.x() + inc2, to.y() + inc, to.z() + inc2);
                    }
                    case WEST -> {
                        from.set(from.x() - inc, from.y() + inc2, from.z() - inc2);
                        to.set(to.x() - inc, to.y() - inc2, to.z() + inc2);
                    }
                    case EAST -> {
                        from.set(from.x() + inc, from.y() + inc2, from.z() - inc2);
                        to.set(to.x() + inc, to.y() - inc2, to.z() + inc2);
                    }
                }
            }
        }
    }
}
