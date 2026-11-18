package com.example.ui.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.model.GlassQualityMode

object LiquidGlassCapabilities {
    fun supportsAgslRuntimeShader(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    fun supportsRenderEffectBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun resolveTier(mode: GlassQualityMode, reducedMotion: Boolean): GlassRenderTier {
        return when (mode) {
            GlassQualityMode.FALLBACK -> GlassRenderTier.FALLBACK
            GlassQualityMode.STANDARD -> {
                if (supportsRenderEffectBlur()) GlassRenderTier.STANDARD else GlassRenderTier.FALLBACK
            }
            GlassQualityMode.HIGH -> {
                when {
                    supportsAgslRuntimeShader() -> GlassRenderTier.HIGH
                    supportsRenderEffectBlur() -> GlassRenderTier.STANDARD
                    else -> GlassRenderTier.FALLBACK
                }
            }
            GlassQualityMode.AUTO -> {
                when {
                    reducedMotion -> GlassRenderTier.STANDARD.takeIf { supportsRenderEffectBlur() }
                        ?: GlassRenderTier.FALLBACK
                    supportsAgslRuntimeShader() -> GlassRenderTier.HIGH
                    supportsRenderEffectBlur() -> GlassRenderTier.STANDARD
                    else -> GlassRenderTier.FALLBACK
                }
            }
        }
    }
}

/**
 * AGSL shader simulating physical Liquid Glass bezel refraction, RGB chromatic dispersion,
 * and grazing-angle specular rim highlights over an arbitrary backdrop shader.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object LiquidGlassAgsl {
    private const val SHADER_SOURCE = """
        uniform shader backdrop;
        uniform float2 iResolution;
        uniform float iCornerRadius;
        uniform float iRefraction;
        uniform float iChromatic;
        uniform float iSpecular;
        uniform float4 iTint;

        float sdRoundedBox(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + float2(r);
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        half4 main(float2 fragCoord) {
            float2 halfSize = iResolution * 0.5;
            float2 p = fragCoord - halfSize;
            float r = min(iCornerRadius, min(halfSize.x, halfSize.y));
            float dist = sdRoundedBox(p, halfSize, r);

            if (dist > 0.5) {
                return half4(0.0);
            }

            // Compute inward optical normal near the curved glass bezel
            float bezelWidth = max(min(halfSize.x, halfSize.y) * 0.65, 18.0);
            float edgeFactor = clamp(1.0 + dist / bezelWidth, 0.0, 1.0);
            float lensCurve = edgeFactor * edgeFactor * (3.0 - 2.0 * edgeFactor);

            float2 gradDir = normalize(p + float2(0.0001));
            // Lens curvature bends background rays inward toward the center
            float2 disp = -gradDir * (lensCurve * iRefraction);
            float2 uvBase = clamp(fragCoord + disp, float2(0.0), iResolution - float2(1.0));

            // Subtle chromatic dispersion along the refracted perimeter
            float2 chromOffset = gradDir * (lensCurve * iChromatic);
            float2 uvR = clamp(uvBase + chromOffset, float2(0.0), iResolution - float2(1.0));
            float2 uvB = clamp(uvBase - chromOffset, float2(0.0), iResolution - float2(1.0));

            half4 colR = backdrop.eval(uvR);
            half4 colG = backdrop.eval(uvBase);
            half4 colB = backdrop.eval(uvB);
            half3 refracted = half3(colR.r, colG.g, colB.b);

            // Mix refracted background with context-aware glass tint
            half3 tinted = mix(refracted, half3(iTint.rgb), half(iTint.a));

            // Grazing-angle Fresnel rim highlight (top-left primary + bottom-right bounce)
            float2 lightDir = normalize(float2(-0.65, -0.76));
            float fresnel = pow(edgeFactor, 3.0);
            float directional = clamp(dot(gradDir, lightDir) * 0.5 + 0.5, 0.15, 1.0);
            float bounce = clamp(dot(gradDir, -lightDir), 0.0, 1.0) * 0.45;
            float highlight = fresnel * (directional + bounce) * iSpecular;

            half3 finalRgb = clamp(tinted + half3(highlight), half3(0.0), half3(1.0));
            float alpha = clamp(1.0 - smoothstep(-1.0, 0.5, dist), 0.0, 1.0);
            return half4(finalRgb * alpha, alpha);
        }
    """

    fun createGlassRenderEffect(
        widthPx: Float,
        heightPx: Float,
        cornerRadiusPx: Float,
        blurRadiusPx: Float,
        refractionStrength: Float,
        chromaticAberration: Float,
        specularIntensity: Float,
        tintRed: Float,
        tintGreen: Float,
        tintBlue: Float,
        tintAlpha: Float
    ): RenderEffect {
        val shader = RuntimeShader(SHADER_SOURCE).apply {
            setFloatUniform("iResolution", widthPx.coerceAtLeast(2f), heightPx.coerceAtLeast(2f))
            setFloatUniform("iCornerRadius", cornerRadiusPx.coerceAtLeast(0f))
            setFloatUniform("iRefraction", refractionStrength)
            setFloatUniform("iChromatic", chromaticAberration)
            setFloatUniform("iSpecular", specularIntensity)
            setFloatUniform("iTint", tintRed, tintGreen, tintBlue, tintAlpha)
        }
        val refractionEffect = RenderEffect.createRuntimeShaderEffect(shader, "backdrop")
        return if (blurRadiusPx > 1f) {
            val blurEffect = RenderEffect.createBlurEffect(
                blurRadiusPx,
                blurRadiusPx,
                Shader.TileMode.CLAMP
            )
            RenderEffect.createChainEffect(refractionEffect, blurEffect)
        } else {
            refractionEffect
        }
    }
}
