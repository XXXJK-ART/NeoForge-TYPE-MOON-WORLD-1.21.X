#version 150

uniform sampler2D Sampler0;
uniform float Time;
uniform float EffectProgress;
uniform float BloomStrength;
in vec2 vertexUv;
in vec4 vertexColor;
out vec4 fragColor;

float sdCircle(vec2 p, float r) { return length(p) - r; }
float sdStar(vec2 p, float r, float n) {
    float a = atan(p.y, p.x) + 3.14159265 / n;
    float sector = 6.2831853 / n;
    float d = cos(floor(0.5 + a / sector) * sector - a) * length(p);
    return d - r;
}
void main() {
    vec2 p = vertexUv * 2.0 - 1.0;
    float ring = 1.0 - smoothstep(0.01, 0.045, abs(sdCircle(p, 0.72)));
    float star = 1.0 - smoothstep(0.01, 0.06, abs(sdStar(p, 0.62, 5.0)));
    float inner = 1.0 - smoothstep(0.0, 0.04, abs(sdCircle(p, 0.30)));
    float sweep = 0.5 + 0.5 * sin(atan(p.y, p.x) * 8.0 - Time * 2.4);
    float alpha = vertexColor.a * (ring * 0.72 + star * 0.32 + inner * 0.20) * (0.78 + sweep * 0.22);
    if (alpha <= 0.004) discard;
    fragColor = vec4(vertexColor.rgb * (1.0 + BloomStrength * 0.12), alpha);
}
