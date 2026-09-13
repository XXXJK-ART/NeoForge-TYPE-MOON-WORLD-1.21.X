#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;
uniform float Intensity;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 source = texture(DiffuseSampler, texCoord);
    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    fragColor = vec4(source.rgb + bloom * Intensity, source.a);
}
