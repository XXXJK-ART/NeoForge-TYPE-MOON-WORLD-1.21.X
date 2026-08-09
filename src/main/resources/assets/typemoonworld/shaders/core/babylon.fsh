#version 150

uniform float Time;

in vec2 vertexUv;
in vec4 vertexColor;

out vec4 fragColor;

float band(float value, float center, float width) {
    float halfWidth = width * 0.5;
    return smoothstep(center - halfWidth, center, value) - smoothstep(center, center + halfWidth, value);
}

void main() {
    vec2 p = vertexUv * 2.0 - 1.0;
    float radius = length(p);
    if (radius > 1.0) {
        discard;
    }

    float angle = atan(p.y, p.x);
    float swirl = angle + radius * 7.0 - Time * 2.15;
    float spokes = pow(0.5 + 0.5 * sin(swirl * 12.0), 5.0);
    float fine = 0.5 + 0.5 * sin(angle * 28.0 - radius * 22.0 + Time * 4.4);
    float outer = band(radius, 0.86, 0.13);
    float inner = band(radius, 0.54, 0.08);
    float core = (1.0 - smoothstep(0.08, 0.62, radius)) * (0.32 + 0.68 * spokes);
    float rimGlow = 1.0 - smoothstep(0.72, 1.0, radius);
    float cutout = smoothstep(0.02, 0.18, radius);

    vec3 deepGold = vec3(0.95, 0.55, 0.08);
    vec3 hotGold = vec3(1.0, 0.92, 0.42);
    vec3 whiteGold = vec3(1.0, 0.98, 0.78);
    vec3 color = mix(deepGold, hotGold, outer + inner * 0.65);
    color = mix(color, whiteGold, clamp(core * 0.5 + fine * outer * 0.28, 0.0, 1.0));

    float alpha = (outer * 0.95 + inner * 0.42 + core * 0.36 + spokes * rimGlow * 0.16) * cutout;
    alpha *= vertexColor.a * (0.9 + 0.1 * sin(Time * 8.0 + radius * 10.0));
    alpha = clamp(alpha, 0.0, 0.98);
    if (alpha <= 0.01) {
        discard;
    }

    fragColor = vec4(color * vertexColor.rgb, alpha);
}
