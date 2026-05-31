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

    float ringOuter = 1.0 - smoothstep(0.68, 1.0, radius);
    float ringInner = 1.0 - smoothstep(0.44, 0.72, radius);
    float ring = max(ringOuter - ringInner, 0.0);
    float core = 1.0 - smoothstep(0.0, 0.24, radius);
    float flicker = 0.92 + 0.08 * sin(Time * 6.0 + radius * 12.0);
    float alpha = (ring * 0.95 + core * 0.18) * flicker * vertexColor.a;
    fragColor = vec4(vertexColor.rgb, alpha);
}
