#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 ColorModulator;

out vec2 vertexUv;
out vec4 vertexColor;

void main() {
    vertexUv = UV0;
    vertexColor = Color * ColorModulator;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
