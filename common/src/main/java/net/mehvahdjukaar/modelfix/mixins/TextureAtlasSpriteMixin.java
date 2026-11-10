package net.mehvahdjukaar.modelfix.mixins;

import net.mehvahdjukaar.modelfix.ModelFixGeom;
import net.mehvahdjukaar.modelfix.PlatStuff;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.11 port: vanilla removed TextureAtlasSprite#uvShrinkRatio and now computes the
 * sprite UV bounds (with the anti-bleeding padding inset) directly in the constructor:
 * u0 = (x + padding) / atlasWidth, u1 = (x + padding + width) / atlasWidth, same for v.
 * We re-run that computation with a scaled-down padding for the block atlas, which is
 * the 1.21.11 equivalent of the old "cancel uv shrink ratio" behaviour.
 */
@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin {

    @Mutable
    @Shadow
    @Final
    private float u0;

    @Mutable
    @Shadow
    @Final
    private float u1;

    @Mutable
    @Shadow
    @Final
    private float v0;

    @Mutable
    @Shadow
    @Final
    private float v1;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void modelfix$cancelAtlasShrink(Identifier atlasLocation, SpriteContents contents,
                                            int atlasWidth, int atlasHeight, int x, int y, int padding,
                                            CallbackInfo ci) {
        if (PlatStuff.isModStateValid()) {
            float mult = ModelFixGeom.getAtlasPaddingMultiplier(atlasLocation);
            if (mult >= 0) {
                float p = padding * mult;
                this.u0 = (x + p) / (float) atlasWidth;
                this.u1 = (x + p + contents.width()) / (float) atlasWidth;
                this.v0 = (y + p) / (float) atlasHeight;
                this.v1 = (y + p + contents.height()) / (float) atlasHeight;
            }
        }
    }
}
