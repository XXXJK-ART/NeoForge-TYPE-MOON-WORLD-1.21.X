#version 150

uniform sampler2D Sampler0;
uniform float Time;
uniform float EffectProgress;
uniform float BloomStrength;
in vec2 vertexUv;
in vec4 vertexColor;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0)), f.x), f.y);
}
float fbm(vec2 p) {
    float value = 0.0, amp = 0.5;
    for (int i = 0; i < 4; ++i) { value += amp * noise(p); p = p * 2.03 + 17.1; amp *= 0.5; }
    return value;
}
void main() {
    vec2 uv = vertexUv;
    vec2 flow = vec2(Time * 0.035, -Time * 0.021);
    float n = fbm(uv * 5.5 + flow);
    float edge = 1.0 - smoothstep(0.34, 0.70, length(uv * 2.0 - 1.0));
    vec4 tex = texture(Sampler0, uv);
    float alpha = tex.a * vertexColor.a * clamp(0.48 + n * 0.72 + edge * 0.25, 0.0, 1.0);
    vec3 color = vertexColor.rgb * (0.82 + n * 0.38 + BloomStrength * 0.06);
    if (alpha <= 0.004) discard;
    fragColor = vec4(color, alpha);
}
