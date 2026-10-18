#version 150

// Procedural Gilgamesh material.
//
// The reference implementation uses several noisy layers and sends a bright
// core to a bloom target.  This project keeps the material compatible with
// Minecraft's single colour target: emissive colour stays on the effect's
// own geometry, without a fullscreen bloom pass. The same material
// works for billboards, decals and trail samples, which keeps the server-side
// effect definitions cheap and deterministic.
uniform sampler2D Sampler0;
uniform float Time;
uniform float EffectProgress;
uniform float BloomStrength;
uniform float DistortionStrength;

in vec2 vertexUv;
in vec4 vertexColor;
out vec4 fragColor;

const float PI = 3.14159265359;

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; ++i) {
        value += noise(p) * amplitude;
        p = p * 2.02 + vec2(17.0, 11.0);
        amplitude *= 0.5;
    }
    return value;
}

float ring(float radius, float center, float width) {
    float d = abs(radius - center);
    return 1.0 - smoothstep(width, width + 0.018, d);
}

void main() {
    vec2 uv = clamp(vertexUv, 0.0, 1.0);
    vec2 p = uv * 2.0 - 1.0;
    float radius = length(p);
    if (radius > 1.04) discard;

    float time = Time * 0.85;
    vec2 drift = DistortionStrength * 0.035 * vec2(
        sin(time * 2.2 + uv.y * 17.0),
        cos(time * 1.7 + uv.x * 13.0));
    vec2 flowUv = uv + drift;
    float turbulence = fbm(flowUv * 5.0 + vec2(time * 0.55, -time * 0.32));
    float fineNoise = noise(flowUv * 18.0 + vec2(-time * 2.0, time * 1.4));

    float angle = atan(p.y, p.x);
    float spiral = 0.5 + 0.5 * sin(angle * 7.0 - time * 6.0 + radius * 27.0 + turbulence * 4.0);
    spiral = smoothstep(0.42, 0.88, spiral);
    float spoke = pow(max(0.0, 0.5 + 0.5 * cos(angle * 12.0 + time * 2.7)), 8.0);
    float rings = ring(radius, 0.56 + 0.035 * sin(time * 2.3), 0.045)
                + ring(radius, 0.83 + 0.025 * cos(time * 1.8), 0.032);

    // Billboard particles use this radial body.  Trails and beam samples use
    // the second mask, producing a clean white-hot line without a hard square.
    float body = (1.0 - smoothstep(0.52, 1.0, radius)) * (0.22 + 0.78 * spiral);
    float cross = abs(uv.y - 0.5) * 2.0;
    float beamEnds = smoothstep(0.015, 0.12, uv.x) * (1.0 - smoothstep(0.88, 0.995, uv.x));
    float beam = exp(-cross * cross * 24.0) * beamEnds;
    float core = exp(-radius * radius * 8.0) + exp(-cross * cross * 70.0) * beamEnds;
    float edge = (1.0 - smoothstep(0.58, 1.0, radius)) * (0.25 + fineNoise * 0.55);
    float mask = max(body * (0.38 + rings * 0.68 + spoke * 0.22), beam * (0.58 + turbulence * 0.7));
    mask += core * 0.34 + edge * 0.12;
    mask *= texture(Sampler0, uv).a * vertexColor.a;
    mask *= 0.88 + 0.12 * sin(Time * 10.0 + turbulence * 8.0);
    // Keep the lifecycle control live in the shader program.  The renderer
    // can use it for a soft fade when an effect is driven by a replayable
    // trigger instead of popping the whole material at once.
    mask *= 0.94 + 0.06 * clamp(EffectProgress, 0.0, 1.0);
    mask *= 1.0 - smoothstep(0.93, 1.03, max(radius, abs(uv.x - 0.5) * 1.8));
    if (mask < 0.004) discard;

    vec3 tint = max(vertexColor.rgb, vec3(0.05));
    vec3 whiteCore = vec3(1.0, 0.98, 0.86);
    vec3 hotEdge = tint * (0.72 + 0.28 * turbulence);
    vec3 color = mix(hotEdge, whiteCore, clamp(core * 1.45 + beam * 0.35, 0.0, 1.0));
    float emissive = 0.92 + BloomStrength * 0.62 + core * 0.75;
    fragColor = vec4(color * emissive * mask, mask);
}
