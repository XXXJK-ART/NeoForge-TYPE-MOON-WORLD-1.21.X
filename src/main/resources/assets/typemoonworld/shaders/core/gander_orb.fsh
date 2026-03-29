#version 150

uniform float Time;

in vec2 vertexUv;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 centered = vertexUv * 2.0 - 1.0;
    float radius = length(centered);
    if (radius > 1.0) {
        discard;
    }

    float angle = atan(centered.y, centered.x);
    float shellBand = smoothstep(0.70, 0.82, radius) - smoothstep(0.92, 0.985, radius);
    float shellGlow = smoothstep(0.88, 1.0, radius);
    float core = 1.0 - smoothstep(0.0, 0.82, radius);
    float flicker = 0.975 + 0.025 * sin(Time * 6.5 + angle * 4.0 + radius * 8.0);
    bool shellLayer = vertexColor.r > 0.75 && vertexColor.g < 0.12 && vertexColor.b < 0.16;

    vec3 color;
    float alpha;
    if (shellLayer) {
        color = mix(vec3(0.32, 0.01, 0.03), vertexColor.rgb, 0.78 + shellGlow * 0.12);
        alpha = (shellBand * 1.18 + shellGlow * 0.08) * vertexColor.a * flicker;
    } else {
        color = vec3(0.0, 0.0, 0.0);
        alpha = (core * 0.995 + shellBand * 0.03) * vertexColor.a * flicker;
    }

    alpha = clamp(alpha, 0.0, 0.995);
    if (alpha <= 0.01) {
        discard;
    }
    fragColor = vec4(color, alpha);
}
