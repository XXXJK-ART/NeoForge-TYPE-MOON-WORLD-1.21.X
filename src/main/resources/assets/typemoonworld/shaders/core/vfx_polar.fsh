#version 150

uniform sampler2D Sampler0;
uniform float Time;
uniform float EffectProgress;
uniform float BloomStrength;
uniform float DistortionStrength;
in vec2 vertexUv;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec2 uv = clamp(vertexUv + DistortionStrength * 0.012 * vec2(cos(Time * 1.5 + vertexUv.y * 8.0), sin(Time * 1.1 + vertexUv.x * 7.0)), 0.001, 0.999);
    vec2 p = uv * 2.0 - 1.0;
    float radius = length(p);
    if (radius > 1.0) discard;
    float angle = atan(p.y, p.x);
    float center = 0.5 + 0.5 * sin(Time * 2.7);
    float band = 1.0 - smoothstep(0.02, 0.18, abs(radius - mix(0.36, 0.88, center)));
    float inner = 1.0 - smoothstep(0.0, 0.30, radius);
    float spokes = pow(max(0.0, cos(angle * 12.0 + Time * 1.7)), 20.0);
    float dissolve = smoothstep(0.04, 0.24, 1.0 - fract(radius * 7.0 + Time * 0.18));
    float alpha = vertexColor.a * (band * 0.92 + spokes * 0.20 + inner * 0.08) * dissolve;
    vec3 color = vertexColor.rgb * (1.0 + BloomStrength * 0.10);
    if (alpha <= 0.004) discard;
    fragColor = vec4(color, alpha);
}
