#version 150

uniform float Time;

in vec2 vertexUv;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float u = fract(vertexUv.x);
    float v = clamp(vertexUv.y, 0.0, 1.0);
    float baseRim = 1.0 - smoothstep(0.02, 0.24, v);
    float crownRim = smoothstep(0.74, 1.0, v);
    float meridianPulse = 0.5 + 0.5 * sin((u * 7.0 - Time * 0.13) * 6.2831853);
    float softPulse = 0.97 + 0.03 * sin(Time * 4.4 + u * 18.0 + v * 7.0);
    float alpha = vertexColor.a * (0.9 + baseRim * 0.18 + crownRim * 0.08 + meridianPulse * 0.05) * softPulse;
    vec3 deepShadow = vec3(0.04, 0.01, 0.07);
    vec3 rimTint = vec3(0.22, 0.08, 0.30);
    vec3 color = mix(vertexColor.rgb, rimTint, baseRim * 0.34 + crownRim * 0.16);
    color = mix(color, deepShadow, (1.0 - v) * 0.16);
    alpha = clamp(alpha, 0.0, 0.95);
    if (alpha <= 0.01) {
        discard;
    }
    fragColor = vec4(color, alpha);
}
