#version 150

uniform float AmbientLightFactor;
uniform float SkyFactor;
uniform float BlockFactor;
uniform int UseBrightLightmap;
uniform vec3 SkyLightColor;
uniform float NightVisionFactor;
uniform float DarknessScale;
uniform float DarkenWorldFactor;
uniform float BrightnessFactor;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    // Keep uniform not to be removed by the optimizer
    fragColor = vec4(1.0, 1.0, 1.0, min(1.0, (
        AmbientLightFactor + SkyFactor + BlockFactor +
        UseBrightLightmap * 1.0 + NightVisionFactor +
        DarknessScale + DarkenWorldFactor + BrightnessFactor + SkyLightColor.x
    ) * 100.0));
}
