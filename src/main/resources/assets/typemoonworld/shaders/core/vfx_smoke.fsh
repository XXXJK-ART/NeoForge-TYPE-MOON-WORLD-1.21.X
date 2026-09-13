#version 150

uniform sampler2D Sampler0;
uniform float Time;
uniform float EffectProgress;
uniform float BloomStrength;
uniform float DistortionStrength;
in vec2 vertexUv;
in vec4 vertexColor;
out vec4 fragColor;

float hash21(vec2 p) { p = fract(p * vec2(127.1, 311.7)); return fract(sin(dot(p, vec2(269.5, 183.3))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x), mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0)), f.x), f.y);
}
void main() {
    vec2 uv = clamp(vertexUv + DistortionStrength * 0.016 * vec2(sin(Time * 0.9 + vertexUv.y * 6.0), cos(Time * 1.2 + vertexUv.x * 5.0)), 0.001, 0.999);
    float n = noise(uv * 4.0 + vec2(Time * 0.025, -Time * 0.04));
    n = mix(n, noise(uv * 9.0 - Time * 0.03), 0.45);
    float mask = 1.0 - smoothstep(0.15, 0.95, length(uv * 2.0 - 1.0));
    vec4 tex = texture(Sampler0, uv);
    float alpha = tex.a * vertexColor.a * mask * smoothstep(0.20, 0.78, n);
    if (alpha <= 0.004) discard;
    fragColor = vec4(vertexColor.rgb * (0.72 + n * 0.28), alpha);
}
