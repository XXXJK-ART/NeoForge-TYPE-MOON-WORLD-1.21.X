#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 px = 1.0 / max(InSize, vec2(1.0));
    vec3 sum = texture(DiffuseSampler, texCoord).rgb * 0.227027;
    sum += texture(DiffuseSampler, texCoord + vec2(px.x * 1.3846, 0.0)).rgb * 0.316216;
    sum += texture(DiffuseSampler, texCoord - vec2(px.x * 1.3846, 0.0)).rgb * 0.316216;
    sum += texture(DiffuseSampler, texCoord + vec2(0.0, px.y * 1.3846)).rgb * 0.070270;
    sum += texture(DiffuseSampler, texCoord - vec2(0.0, px.y * 1.3846)).rgb * 0.070270;
    fragColor = vec4(sum, 1.0);
}
