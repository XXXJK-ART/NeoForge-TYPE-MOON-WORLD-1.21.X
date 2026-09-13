#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Strength;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    float luminance = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));
    float strength = clamp(Strength, 0.0, 1.0);
    vec3 pale = mix(source.rgb, vec3(luminance), strength);
    fragColor = vec4(pale * vec3(0.92, 0.94, 0.97), source.a);
}
