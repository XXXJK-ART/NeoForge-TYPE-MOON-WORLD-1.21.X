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
    float ring = max((1.0 - smoothstep(0.62, 1.0, radius)) - (1.0 - smoothstep(0.46, 0.74, radius)), 0.0);
    float radialLine = pow(max(0.0, cos(angle * 12.0 + Time * 1.8)), 18.0);
    float scan = 1.0 - smoothstep(0.0, 0.85, abs(sin(Time * 2.3 + radius * 18.0)));
    float alpha = (ring * 0.72 + radialLine * 0.18 + scan * 0.14) * vertexColor.a;
    fragColor = vec4(vertexColor.rgb, alpha);
}
