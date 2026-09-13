#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Threshold;
uniform float Intensity;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = max(max(c.r, c.g), c.b);
    float mask = smoothstep(Threshold, Threshold + 0.18, l);
    fragColor = vec4(max(c - vec3(Threshold), vec3(0.0)) * mask * Intensity, 1.0);
}
