#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 InSize;
uniform float Time;
uniform float WhiteFlash;
uniform float GlassBreak;
uniform float TimeStop;
uniform float Chromatic;
uniform float RadialBlur;
uniform float Vignette;
in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) { p = fract(p * vec2(127.1, 311.7)); return fract(sin(dot(p, vec2(269.5, 183.3))) * 43758.5453); }
float voronoiEdge(vec2 p) {
    vec2 cell = floor(p), f = fract(p);
    float nearest = 1.0, second = 1.0;
    for (int y = -1; y <= 1; ++y) for (int x = -1; x <= 1; ++x) {
        vec2 offset = vec2(float(x), float(y));
        vec2 point = offset + vec2(hash21(cell + offset), hash21(cell + offset + 19.7));
        float distanceToPoint = length(offset + point - f);
        if (distanceToPoint < nearest) { second = nearest; nearest = distanceToPoint; }
        else if (distanceToPoint < second) second = distanceToPoint;
    }
    return smoothstep(0.015, 0.09, second - nearest);
}
void main() {
    vec2 uv = texCoord;
    vec2 centered = uv * 2.0 - 1.0;
    float radius = length(centered);
    float crack = GlassBreak * voronoiEdge(uv * 9.0 + vec2(Time * 0.05));
    vec2 offset = centered * RadialBlur * 0.012 * smoothstep(0.0, 1.2, radius);
    vec3 source;
    if (Chromatic > 0.001) {
        float shift = Chromatic * 0.003 * (0.35 + radius);
        source.r = texture(DiffuseSampler, clamp(uv + offset + vec2(shift, 0.0), 0.001, 0.999)).r;
        source.g = texture(DiffuseSampler, clamp(uv + offset, 0.001, 0.999)).g;
        source.b = texture(DiffuseSampler, clamp(uv + offset - vec2(shift, 0.0), 0.001, 0.999)).b;
    } else {
        source = texture(DiffuseSampler, clamp(uv + offset, 0.001, 0.999)).rgb;
    }
    float depth = texture(DepthSampler, uv).r;
    float flash = WhiteFlash * mix(0.68, 1.0, smoothstep(0.0, 1.0, depth)) * (0.78 + 0.22 * hash21(uv * 80.0 + Time));
    source = mix(source, vec3(1.0), clamp(flash, 0.0, 1.0));
    source = mix(source, source * vec3(0.58, 0.70, 0.95), clamp(TimeStop * 0.32, 0.0, 0.4));
    source += vec3(0.12, 0.18, 0.34) * TimeStop * smoothstep(0.62, 0.96, radius);
    source = mix(source, source * (0.64 + 0.36 * smoothstep(0.15, 0.9, abs(sin(uv.x * 31.0 + uv.y * 12.0)))), clamp(crack, 0.0, 0.68));
    source *= 1.0 - clamp(Vignette, 0.0, 1.0) * smoothstep(0.48, 1.2, radius);
    fragColor = vec4(source, texture(DiffuseSampler, uv).a);
}
