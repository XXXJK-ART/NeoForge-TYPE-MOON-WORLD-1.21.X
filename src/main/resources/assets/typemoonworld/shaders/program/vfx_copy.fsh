#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    // World framebuffer alpha is not coverage: vanilla sky pixels can have
    // alpha zero. A completed scene must replace the destination in full.
    fragColor = vec4(texture(DiffuseSampler, texCoord).rgb, 1.0);
}
