#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float ClipMinY;
uniform float ClipMaxY;
uniform float ClipThreshold;
uniform float ClipMode;
uniform float ClipSoftness;
uniform float EdgeWidth;
uniform vec4 EdgeColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;
in float localY;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }

    float normalized = clamp((localY - ClipMinY) / max(0.001, ClipMaxY - ClipMinY), 0.0, 1.0);
    float heightFromFeet = 1.0 - normalized;
    float threshold = clamp(ClipThreshold, 0.0, 1.0);
    float signedDistance = ClipMode < 0.5 ? threshold - heightFromFeet : heightFromFeet - threshold;
    if (signedDistance < -ClipSoftness) {
        discard;
    }

    float maskAlpha = smoothstep(-ClipSoftness, ClipSoftness, signedDistance);
    float edge = 1.0 - smoothstep(0.0, EdgeWidth, abs(heightFromFeet - threshold));

    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;
    color.a *= maskAlpha;
    color.rgb += EdgeColor.rgb * edge * EdgeColor.a;

    if (color.a <= 0.01) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
