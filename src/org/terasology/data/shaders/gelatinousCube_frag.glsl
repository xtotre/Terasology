uniform sampler2D texture;
uniform vec4 colorOffset;
uniform bool carryingTorch;

varying vec3 normal;
varying vec4 vertexWorldPos;

uniform float light = 1.0;

void main(){
    vec4 color = srgbToLinear(texture2D(texture, vec2(gl_TexCoord[0].x , gl_TexCoord[0].y)));

    float torchlight = 0.0;
    float highlight = calcLambLight(normal, -normalize(vertexWorldPos.xyz));

    if (carryingTorch)
        torchlight = calcTorchlight(highlight, vertexWorldPos.xyz);

    color.rgb *= clamp(gl_Color.rgb, 0.0, 1.0) * colorOffset.rgb;

    float lightValue = expLightValue(light);
    color.rgb *= lightValue * 0.85 + 0.15 * highlight * lightValue;
    color.rgb += torchlight;

    color.a = gl_Color.a;

    gl_FragColor = linearToSrgb(color);
}
