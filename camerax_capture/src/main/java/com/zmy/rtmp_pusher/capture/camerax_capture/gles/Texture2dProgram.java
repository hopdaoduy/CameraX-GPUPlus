/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zmy.rtmp_pusher.capture.camerax_capture.gles;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;


import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
public class Texture2dProgram {
    private static final String TAG = Texture2dProgram.class.getSimpleName();
    private volatile float[] mBeautyLevelParam;
    private int mGLLevelParamLocation;

    // this for beauty face
//    private int height;
//    private int width;

    public enum ProgramType {
        TEXTURE_2D, ORIGIN, TEXTURE_EXT_BW, TEXTURE_EXT_FILT,
        SEPIA, CROSSPROCESS, POSTERIZE, GRAYSCALE, NIGHT,
        CHROMA, SQUEEZE, TWIRL, TUNNEL, BULGE, DENT, FISHEYE
        , STRETCH, MIRROR, BEAUTIFY, NEGATIVE
    }

    public static final List<ProgramType> EFFECTS = new ArrayList<>();
    static {
        EFFECTS.add(ProgramType.ORIGIN);
        EFFECTS.add(ProgramType.BEAUTIFY);
//        EFFECTS.add(ProgramType.TEXTURE_2D);
//        EFFECTS.add(ProgramType.TEXTURE_EXT_BW);
//        EFFECTS.add(ProgramType.TEXTURE_EXT_FILT);
        EFFECTS.add(ProgramType.CROSSPROCESS);
        EFFECTS.add(ProgramType.POSTERIZE);
        EFFECTS.add(ProgramType.SEPIA);
        EFFECTS.add(ProgramType.GRAYSCALE);
        EFFECTS.add(ProgramType.NIGHT);
        EFFECTS.add(ProgramType.CHROMA);
        EFFECTS.add(ProgramType.SQUEEZE);
        EFFECTS.add(ProgramType.TWIRL);
        EFFECTS.add(ProgramType.TUNNEL);
        EFFECTS.add(ProgramType.BULGE);
        EFFECTS.add(ProgramType.DENT);
        EFFECTS.add(ProgramType.FISHEYE);
        EFFECTS.add(ProgramType.STRETCH);
        EFFECTS.add(ProgramType.MIRROR);
//        EFFECTS.add(ProgramType.NEGATIVE);
    }
    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" + // A constant representing the combined model/view/projection matrix.
                    "uniform mat4 uTexMatrix;\n" + // Per-vertex position information we will pass in.
                    "attribute vec4 aPosition;\n" + // Per-vertex color information we will pass in.
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

//    private static final String VERTEX_SHADER =
//            "uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
//
//                    + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
//                    + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.
//
//                    + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
//
//                    + "void main()                    \n"		// The entry point for our vertex shader.
//                    + "{                              \n"
//                    + "   v_Color = a_Color;          \n"		// Pass the color through to the fragment shader.
//                    // It will be interpolated across the triangle.
//                    + "   gl_Position = u_MVPMatrix   \n" 	// gl_Position is a special variable used to store the final position.
//                    + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
//                    + "}                              \n";    // normalized screen coordinates.

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    // Simple fragment shader for use with external 2D textures (e.g. what we get from
    // SurfaceTexture).
    private static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    // Fragment shader that converts color to black & white with a simple transformation.
    private static final String FRAGMENT_SHADER_EXT_BW =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                    "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                    "    gl_FragColor = vec4(color, color, color, 1.0);\n" +
                    "}\n";


    // Fragment shader that converts color to black & white with a simple transformation.
    private static final String FRAGMENT_SHADER_EXT_SEPIA =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                    "    gl_FragColor = vec4(tc.x, tc.y, tc.z, 1.0);\n" +
                    "    gl_FragColor.r = dot(tc, vec4(.393, .769, .189, 0));\n" +
                    "    gl_FragColor.g = dot(tc, vec4(.349, .686, .168, 0));\n" +
                    "    gl_FragColor.b = dot(tc, vec4(.272, .534, .131, 0));\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_EXT_POSTERIZE =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "uniform samplerExternalOES sTexture;\n"
                    + "varying vec2 vTextureCoord;\n"
                    + "void main() {\n"
                    + "  vec3 color = texture2D(sTexture, vTextureCoord).rgb;\n"
                    + "   color = pow(color, vec3(0.6, 0.6, 0.6));\n"
                    + "   color = color * 8.0;\n"
                    + "   color = floor(color);\n"
                    + "   color = color / 8.0;\n"
                    + "   color = pow(color, vec3(1.0/0.6));\n"
                    + "   gl_FragColor = vec4(color, 1.0);\n"
                    + "}\n";

    private static final String FRAGMENT_SHADER_EXT_GRAY_SCALE =
            "#extension GL_OES_EGL_image_external : require\n"
            + "precision mediump float;\n"
            + "uniform samplerExternalOES sTexture;\n"
            + "varying vec2 vTextureCoord;\n" + "void main() {\n"
            + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
            + "  float y = dot(color, vec4(0.299, 0.587, 0.114, 0));\n"
            + "  gl_FragColor = vec4(y, y, y, color.a);\n" + "}\n";


    private static final String FRAGMENT_SHADER_EXT_CROSSPROCESS =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "uniform samplerExternalOES sTexture;\n"
                    + "varying vec2 vTextureCoord;\n" +
                    "void main() {\n"
                    + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                    + "  vec3 ncolor = vec3(0.0, 0.0, 0.0);\n" + "  float value;\n"
                    + "  if (color.r < 0.5) {\n" + "    value = color.r;\n"
                    + "  } else {\n" + "    value = 1.0 - color.r;\n" + "  }\n"
                    + "  float red = 4.0 * value * value * value;\n"
                    + "  if (color.r < 0.5) {\n" + "    ncolor.r = red;\n"
                    + "  } else {\n" + "    ncolor.r = 1.0 - red;\n" + "  }\n"
                    + "  if (color.g < 0.5) {\n" + "    value = color.g;\n"
                    + "  } else {\n" + "    value = 1.0 - color.g;\n" + "  }\n"
                    + "  float green = 2.0 * value * value;\n"
                    + "  if (color.g < 0.5) {\n" + "    ncolor.g = green;\n"
                    + "  } else {\n" + "    ncolor.g = 1.0 - green;\n" + "  }\n"
                    + "  ncolor.b = color.b * 0.5 + 0.25;\n"
                    + "  gl_FragColor = vec4(ncolor.rgb, color.a);\n" +
                    "}\n";
    // Fragment shader that attempts to produce a high contrast image
    private static final String FRAGMENT_SHADER_EXT_NIGHT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                    "    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;\n" +
                    "    gl_FragColor = vec4(color, color + 0.15, color, 1.0);\n" +
                    "}\n";

    // Fragment shader that applies a Chroma Key effect, making green pixels transparent
    private static final String FRAGMENT_SHADER_EXT_CHROMA_KEY =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                    "    float color = ((tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11) - 0.5 * 1.5) + 0.8;\n" +
                    "    if(tc.g > 0.6 && tc.b < 0.6 && tc.r < 0.6){ \n" +
                    "        gl_FragColor = vec4(0, 0, 0, 0.0);\n" +
                    "    }else{ \n" +
                    "        gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "    }\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_SQUEEZE =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec2 uPosition;\n" +
                    "void main() {\n" +
                    "    vec2 texCoord = vTextureCoord.xy;\n" +
                    "    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
                    "    float r = length(normCoord); // to polar coords \n" +
                    "    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
                    "    r = pow(r, 1.0/1.8) * 0.8;\n"+  // Squeeze it
                    "    normCoord.x = r * cos(phi); \n" +
                    "    normCoord.y = r * sin(phi); \n" +
                    "    texCoord = normCoord / 2.0 + 0.5;\n"+
                    "    gl_FragColor = texture2D(sTexture, texCoord);\n"+
                    "}\n";

    private static final String FRAGMENT_SHADER_TWIRL =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec2 uPosition;\n" +
                    "void main() {\n" +
                    "    vec2 texCoord = vTextureCoord.xy;\n" +
                    "    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
                    "    float r = length(normCoord); // to polar coords \n" +
                    "    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
                    "    phi = phi + (1.0 - smoothstep(-0.5, 0.5, r)) * 4.0;\n"+ // Twirl it
                    "    normCoord.x = r * cos(phi); \n" +
                    "    normCoord.y = r * sin(phi); \n" +
                    "    texCoord = normCoord / 2.0 + 0.5;\n"+
                    "    gl_FragColor = texture2D(sTexture, texCoord);\n"+
                    "}\n";

    private static final String FRAGMENT_SHADER_TUNNEL =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec2 uPosition;\n" +
                    "void main() {\n" +
                    "    vec2 texCoord = vTextureCoord.xy;\n" +
                    "    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
                    "    float r = length(normCoord); // to polar coords \n" +
                    "    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
                    "    if (r > 0.5) r = 0.5;\n"+ // Tunnel
                    "    normCoord.x = r * cos(phi); \n" +
                    "    normCoord.y = r * sin(phi); \n" +
                    "    texCoord = normCoord / 2.0 + 0.5;\n"+
                    "    gl_FragColor = texture2D(sTexture, texCoord);\n"+
                    "}\n";

    private static final String FRAGMENT_SHADER_BULGE =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec2 uPosition;\n" +
                    "void main() {\n" +
                    "    vec2 texCoord = vTextureCoord.xy;\n" +
                    "    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
                    "    float r = length(normCoord); // to polar coords \n" +
                    "    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
                    "    r = r * smoothstep(-0.1, 0.5, r);\n"+ // Bulge
                    "    normCoord.x = r * cos(phi); \n" +
                    "    normCoord.y = r * sin(phi); \n" +
                    "    texCoord = normCoord / 2.0 + 0.5;\n"+
                    "    gl_FragColor = texture2D(sTexture, texCoord);\n"+
                    "}\n";

    private static final String FRAGMENT_SHADER_DENT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec2 uPosition;\n" +
                    "void main() {\n" +
                    "    vec2 texCoord = vTextureCoord.xy;\n" +
                    "    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
                    "    float r = length(normCoord); // to polar coords \n" +
                    "    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
                    "    r = 2.0 * r - r * smoothstep(0.0, 0.7, r);\n"+ // Dent
                    "    normCoord.x = r * cos(phi); \n" +
                    "    normCoord.y = r * sin(phi); \n" +
                    "    texCoord = normCoord / 2.0 + 0.5;\n"+
                    "    gl_FragColor = texture2D(sTexture, texCoord);\n"+
                    "}\n";

    private static final String FRAGMENT_SHADER_FISHEYE =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec2 uPosition;\n" +
                    "void main() {\n" +
                    "    vec2 texCoord = vTextureCoord.xy;\n" +
                    "    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
                    "    float r = length(normCoord); // to polar coords \n" +
                    "    float phi = atan(normCoord.y + uPosition.y, normCoord.x + uPosition.x); // to polar coords \n"+
                    "    r = r * r / sqrt(2.0);\n"+ // Fisheye
                    "    normCoord.x = r * cos(phi); \n" +
                    "    normCoord.y = r * sin(phi); \n" +
                    "    texCoord = normCoord / 2.0 + 0.5;\n"+
                    "    gl_FragColor = texture2D(sTexture, texCoord);\n"+
                    "}\n";

    private static final String FRAGMENT_SHADER_STRETCH =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec2 uPosition;\n" +
                    "void main() {\n" +
                    "    vec2 texCoord = vTextureCoord.xy;\n" +
                    "    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
                    "    vec2 s = sign(normCoord + uPosition);\n"+
                    "    normCoord = abs(normCoord);\n"+
                    "    normCoord = 0.5 * normCoord + 0.5 * smoothstep(0.25, 0.5, normCoord) * normCoord;\n"+
                    "    normCoord = s * normCoord;\n"+
                    "    texCoord = normCoord / 2.0 + 0.5;\n"+
                    "    gl_FragColor = texture2D(sTexture, texCoord);\n"+
                    "}\n";

    private static final String FRAGMENT_SHADER_MIRROR =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec2 uPosition;\n" +
                    "void main() {\n" +
                    "    vec2 texCoord = vTextureCoord.xy;\n" +
                    "    vec2 normCoord = 2.0 * texCoord - 1.0;\n"+
                    "    normCoord.x = normCoord.x * sign(normCoord.x + uPosition.x);\n"+
                    "    texCoord = normCoord / 2.0 + 0.5;\n"+
                    "    gl_FragColor = texture2D(sTexture, texCoord);\n"+
                    "}\n";

    private static final String FRAGMENT_SHADER_EXT_BEAUTY =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +

                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec4 levelParam;\n" +
                    "varying mediump vec2 vTextureCoord;\n" +

                    "void main() {\n" +
                    "vec3 centralColor;\n" +
                    "float sampleColor;\n" +
                    "vec2 blurCoordinates[20];\n" +
                    "float mul = 2.0;\n" +
                    "float mul_x = mul / 1080.0;\n" +
                    "float mul_y = mul / 1920.0;\n" +

                    "blurCoordinates[0] = vTextureCoord + vec2(0.0 * mul_x,-10.0 * mul_y);\n" +
                    "blurCoordinates[1] = vTextureCoord + vec2(5.0 * mul_x,-8.0 * mul_y);\n" +
                    "blurCoordinates[2] = vTextureCoord + vec2(8.0 * mul_x,-5.0 * mul_y);\n" +
                    "blurCoordinates[3] = vTextureCoord + vec2(10.0 * mul_x,0.0 * mul_y);\n" +
                    "blurCoordinates[4] = vTextureCoord + vec2(8.0 * mul_x,5.0 * mul_y);\n" +
                    "blurCoordinates[5] = vTextureCoord + vec2(5.0 * mul_x,8.0 * mul_y);\n" +
                    "blurCoordinates[6] = vTextureCoord + vec2(0.0 * mul_x,10.0 * mul_y);\n" +
                    "blurCoordinates[7] = vTextureCoord + vec2(-5.0 * mul_x,8.0 * mul_y);\n" +
                    "blurCoordinates[8] = vTextureCoord + vec2(-8.0 * mul_x,5.0 * mul_y);\n" +
                    "blurCoordinates[9] = vTextureCoord + vec2(-10.0 * mul_x,0.0 * mul_y);\n" +
                    "blurCoordinates[10] = vTextureCoord + vec2(-8.0 * mul_x,-5.0 * mul_y);\n" +
                    "blurCoordinates[11] = vTextureCoord + vec2(-5.0 * mul_x,-8.0 * mul_y);\n" +
                    "blurCoordinates[12] = vTextureCoord + vec2(0.0 * mul_x,-6.0 * mul_y);\n" +
                    "blurCoordinates[13] = vTextureCoord + vec2(-4.0 * mul_x,-4.0 * mul_y);\n" +
                    "blurCoordinates[14] = vTextureCoord + vec2(-6.0 * mul_x,0.0 * mul_y);\n" +
                    "blurCoordinates[15] = vTextureCoord + vec2(-4.0 * mul_x,4.0 * mul_y);\n" +
                    "blurCoordinates[16] = vTextureCoord + vec2(0.0 * mul_x,6.0 * mul_y);\n" +
                    "blurCoordinates[17] = vTextureCoord + vec2(4.0 * mul_x,4.0 * mul_y);\n" +
                    "blurCoordinates[18] = vTextureCoord + vec2(6.0 * mul_x,0.0 * mul_y);\n" +
                    "blurCoordinates[19] = vTextureCoord + vec2(4.0 * mul_x,-4.0 * mul_y);\n" +

                    "centralColor = texture2D(sTexture, vTextureCoord).rgb;\n" +
                    "sampleColor = centralColor.g * 22.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[0]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[1]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[2]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[3]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[4]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[5]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[6]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[7]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[8]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[9]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[10]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[11]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[12]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[13]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[14]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[15]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[16]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[17]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[18]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[19]).g * 2.0;\n" +
                    "sampleColor = sampleColor/50.0;\n" +

                    "float dis = centralColor.g - sampleColor + 0.5;\n" +
                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "float aa = 1.03;\n" +
                    "vec3 smoothColor = centralColor*aa - vec3(dis)*(aa-1.0);\n" +

                    "float hue = dot(smoothColor, vec3(0.299,0.587,0.114));\n" +

                    "float huePow = pow(hue, levelParam.x);\n" +
                    "aa = 1.0 + huePow*0.1;\n" +
                    "smoothColor = centralColor*aa - vec3(dis)*(aa-1.0);\n" +

                    "smoothColor.r = clamp(pow(smoothColor.r, levelParam.y),0.0,1.0);\n" +
                    "smoothColor.g = clamp(pow(smoothColor.g, levelParam.y),0.0,1.0);\n" +
                    "smoothColor.b = clamp(pow(smoothColor.b, levelParam.y),0.0,1.0);\n" +

                    "vec3 lvse = vec3(1.0)-(vec3(1.0)-smoothColor)*(vec3(1.0)-centralColor);\n" +
                    "vec3 bianliang = max(smoothColor, centralColor);\n" +
                    "vec3 temp = 2.0*centralColor*smoothColor;\n" +
                    "vec3 rouguang = temp + centralColor*centralColor - temp*centralColor;\n" +

                    "gl_FragColor = vec4(mix(centralColor, lvse, huePow), 1.0);\n" +
                    "gl_FragColor.rgb = mix(gl_FragColor.rgb, bianliang, huePow);\n" +
                    "gl_FragColor.rgb = mix(gl_FragColor.rgb, rouguang, levelParam.z);\n" +

                    "mat3 saturateMatrix = mat3(1.1102, -0.0598, -0.061, -0.0774, 1.0826, -0.1186, -0.0228, -0.0228, 1.1772);\n" +
                    "vec3 satcolor = gl_FragColor.rgb * saturateMatrix;\n" +
                    "gl_FragColor.rgb = mix(gl_FragColor.rgb, satcolor, levelParam.w);\n" +
                    "}\n";

    // Fragment shader with a convolution filter.  The upper-left half will be drawn normally,
    // the lower-swipe_right half will have the filter applied, and a thin red line will be drawn
    // at the border.
    //
    // This is not optimized for performance.  Some things that might make this faster:
    // - Remove the conditionals.  They're used to present a half & half view with a red
    //   stripe across the middle, but that's only useful for a demo.
    // - Unroll the loop.  Ideally the compiler does this for you when it's beneficial.
    // - Bake the filter kernel into the shader, instead of passing it through a uniform
    //   array.  That, combined with loop unrolling, should reduce memory accesses.
    private static final int KERNEL_SIZE = 9;
    private static final String FRAGMENT_SHADER_EXT_FILT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "#define KERNEL_SIZE " + KERNEL_SIZE + "\n" +
                    "precision highp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform float uKernel[KERNEL_SIZE];\n" +
                    "uniform vec2 uTexOffset[KERNEL_SIZE];\n" +
                    "uniform float uColorAdjust;\n" +
                    "void main() {\n" +
                    "    int i = 0;\n" +
                    "    vec4 sum = vec4(0.0);\n" +
                    "    if (vTextureCoord.x < vTextureCoord.y - 0.005) {\n" +
                    "        for (i = 0; i < KERNEL_SIZE; i++) {\n" +
                    "            vec4 texc = texture2D(sTexture, vTextureCoord + uTexOffset[i]);\n" +
                    "            sum += texc * uKernel[i];\n" +
                    "        }\n" +
                    "    sum += uColorAdjust;\n" +
                    "    } else if (vTextureCoord.x > vTextureCoord.y + 0.005) {\n" +
                    "        sum = texture2D(sTexture, vTextureCoord);\n" +
                    "    } else {\n" +
                    "        sum.r = 1.0;\n" +
                    "    }\n" +
                    "    gl_FragColor = sum;\n" +
                    "}\n";

    private ProgramType mProgramType;

    // Handles to the GL program and various components of it.
    private int mProgramHandle;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int muKernelLoc;
    private int muTexOffsetLoc;
    private int muColorAdjustLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;

    // this for beauty face
//    private int muIntensityLoc;
//    private float intensity = 1.0f;
//    private int muImageStepLocation;

    private int mTextureTarget;

    private float[] mKernel = new float[KERNEL_SIZE];
    private float[] mTexOffset;
    private float mColorAdjust;

    public void setLevelParam(int levelParam) {
        mBeautyLevelParam = setLevel(levelParam);
        Log.d(TAG, "mBeautyLevelParam: " + Arrays.toString(mBeautyLevelParam) + " / " + levelParam);
    }

    /**
     * Prepares the program in the current EGL activity.
     */
    public Texture2dProgram(ProgramType programType) {
        mProgramType = programType;

        switch (programType) {
            case TEXTURE_2D:
                mTextureTarget = GLES20.GL_TEXTURE_2D;
                mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case ORIGIN:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case TEXTURE_EXT_BW:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case GRAYSCALE:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_GRAY_SCALE);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case SEPIA:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_SEPIA);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case POSTERIZE:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_POSTERIZE);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case CROSSPROCESS:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_CROSSPROCESS);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case TEXTURE_EXT_FILT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case NIGHT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_NIGHT);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case CHROMA:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_CHROMA_KEY);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case SQUEEZE:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_SQUEEZE);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case TWIRL:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_TWIRL);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case TUNNEL:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_TUNNEL);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case BULGE:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_BULGE);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case DENT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_DENT);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case FISHEYE:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_FISHEYE);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case STRETCH:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_STRETCH);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case MIRROR:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_MIRROR);
//                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
//                mBeautyLevelParam = setLevel(0);
                break;
            case BEAUTIFY:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle =  GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BEAUTY);
                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
                mBeautyLevelParam = setLevel(3);
                Log.d(TAG, "mBeautyLevelParam: " + Arrays.toString(mBeautyLevelParam));
                break;

            default:
                throw new RuntimeException("Unhandled type " + programType);
        }
        if (mProgramHandle == 0) {
            Log.e(TAG, "Unable create program");
//            throw new RuntimeException("Unable to create program");
        }
        Log.d(TAG, "Created program " + mProgramHandle + " (" + programType + ")");

        // get locations of attributes and uniforms

        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
         GlUtil.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
         GlUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
         GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
         GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");
        muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel");
        if (muKernelLoc < 0) {
            // no kernel in this one
            muKernelLoc = -1;
            muTexOffsetLoc = -1;
            muColorAdjustLoc = -1;
        } else {
            // has kernel, must also have tex offset and color adj
            muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset");
             GlUtil.checkLocation(muTexOffsetLoc, "uTexOffset");
            muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust");
             GlUtil.checkLocation(muColorAdjustLoc, "uColorAdjust");

            // initialize default values
            setKernel(new float[] {0f, 0f, 0f,  0f, 1f, 0f,  0f, 0f, 0f}, 0f);
            setTexSize(256, 256);
        }
        // this is for beauty face
//        muIntensityLoc = GLES20.glGetUniformLocation(mProgramHandle,"intensity");
//        if (muIntensityLoc >= 0) {
//            Log.d(TAG, "Texture2dProgram: intensity exist");
//        }
//        else {
//            Log.d(TAG, "Texture2dProgram: intensity not exist");
//        }
//
//        muImageStepLocation = GLES20.glGetUniformLocation(mProgramHandle, "imageStep");
//
//        if (muImageStepLocation >= 0) {
//            Log.d(TAG, "Texture2dProgram: imageStep exists");
//        }
//        else {
//            Log.d(TAG, "Texture2dProgram: ImageStep does not exist");
//        }
    }

    private float[] setLevel(int _beautyLevel) {
        float hue_,smoothColor_,rouguang_,saturate_;
        switch (_beautyLevel) {

            case 5:
            {
                hue_ = 0.33f;
                smoothColor_ = 0.63f;
                rouguang_ = 0.4f;
                saturate_ = 0.35f;
                break;
            }
            case 4:
            {
                hue_ = 0.4f;
                smoothColor_ = 0.7f;
                rouguang_ = 0.38f;
                saturate_ = 0.3f;
                break;
            }
            case 3:
            {
                hue_ = 0.6f;
                smoothColor_ = 0.8f;
                rouguang_ = 0.25f;
                saturate_ = 0.25f;
                break;
            }
            case 2:
            {
                hue_ = 0.8f;
                smoothColor_ = 0.9f;
                rouguang_ = 0.2f;
                saturate_ = 0.2f;
                break;
            }
            default:
            {
                hue_ = 1.0f;
                smoothColor_ = 1.0f;
                rouguang_ = 0.15f;
                saturate_ = 0.15f;
                break;
            }
        }
        float levelParam[] = {hue_,smoothColor_,rouguang_,saturate_};
        return levelParam;
    }

    /**
     * Releases the program.
     * <p>
     * The appropriate EGL activity must be current (i.e. the one that was used to create
     * the program).
     */
    public void release() {
        Log.d(TAG, "deleting program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    /**
     * Returns the program type.
     */
    public ProgramType getProgramType() {
        return mProgramType;
    }

    /**
     * Creates a texture object suitable for use with this program.
     * <p>
     * On exit, the texture will be bound.
     */
    public int createTextureObject() {
        long origThreadID = Thread.currentThread().getId();
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
         GlUtil.checkGlError("glGenTextures");
        int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
         GlUtil.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(mTextureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(mTextureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mTextureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
         GlUtil.checkGlError("glTexParameter");

        // origin
//        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
//        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//        GlUtil.checkGlError("glTexParameter");

        return texId;
    }

    /**
     * Configures the convolution filter values.
     *
     * @param values Normalized filter values; must be KERNEL_SIZE elements.
     */
    private void setKernel(float[] values, float colorAdj) {
        if (values.length != KERNEL_SIZE) {
            throw new IllegalArgumentException("Kernel size is " + values.length +
                    " vs. " + KERNEL_SIZE);
        }
        System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE);
        mColorAdjust = colorAdj;
        //Log.d(TAG, "filt kernel: " + Arrays.toString(mKernel) + ", adj=" + colorAdj);
    }

    public List<ProgramType> getEffects() {
        if (EFFECTS.size() != 0) {
            return EFFECTS;
        } else return new ArrayList<>();
    }

    /**
     * Sets the size of the texture.  This is used to find adjacent texels when filtering.
     */
    public void setTexSize(int width, int height) {
        // this for beauty face
        Log.d(TAG, "setTexSize width:" + width + " height: " + height);
//        this.width = width;
//        this.height = height;

        float rw = 1.0f / width;
        float rh = 1.0f / height;

        // Don't need to create a new array here, but it's syntactically convenient.
        mTexOffset = new float[] {
                -rw, -rh,   0f, -rh,    rw, -rh,
                -rw, 0f,    0f, 0f,     rw, 0f,
                -rw, rh,    0f, rh,     rw, rh
        };
        //Log.d(TAG, "filt size: " + width + "x" + height + ": " + Arrays.toString(mTexOffset));
    }

    /**
     * Draw images to send to server
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix The 4x4 projection matrix.
     * @param vertexBuffer Buffer with vertex position data.
     * @param firstVertex Index of first vertex to use in vertexBuffer.
     * @param vertexCount Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride Width, in bytes, of the position data for each vertex (often
     *        vertexCount * sizeof(float)).
     * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
     *        for use with SurfaceTexture.)
     * @param texBuffer Buffer with vertex texture data.
     * @param texStride Width, in bytes, of the texture data for each vertex.
     */
    void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
              int vertexCount, int coordsPerVertex, int vertexStride,
              float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {

//        GlUtil.checkGlError("draw start");
        // Select the program.
        GLES20.glUseProgram(mProgramHandle);

//        GlUtil.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GlUtil.checkGlError("glActiveTexture");

        GLES20.glBindTexture(mTextureTarget, textureId);
//        GlUtil.checkGlError("glBindTexture");

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);

//        GlUtil.checkGlError("glUniformMatrix4fv");


        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);

//        GlUtil.checkGlError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);

//        GlUtil.checkGlError("glEnableVertexAttribArray");


        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

//        GlUtil.checkGlError("glVertexAttribPointer");


        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
//        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, texStride, texBuffer);
//        GlUtil.checkGlError("glVertexAttribPointer");


        // Populate the convolution kernel, if present.
        if (muKernelLoc >= 0) {
            GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0);
            GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0);
            GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
        }

        // this for beauty face
        if (mProgramType == ProgramType.BEAUTIFY) {
            GLES20.glUniform4fv(mGLLevelParamLocation, 1, FloatBuffer.wrap(mBeautyLevelParam));
        }

        // draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
//        GlUtil.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
//        GlUtil.checkGlError("glDisableVertextAttribArray");
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
//        GlUtil.checkGlError("glDisableVertextAttribArray");
        GLES20.glBindTexture(mTextureTarget, 0);
        // this below line was added with beauty face
        GLES20.glUseProgram(0);
    }

    public void setBitmap(Bitmap bitmap, int textureId) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLUtils.texImage2D(mTextureTarget, 0, GLES20.GL_RGBA, bitmap, 0);
    }

    /**
     * Draw images to send to preview
     * @param mvpMatrix
     * @param vertexBuffer
     * @param firstVertex
     * @param vertexCount
     * @param coordsPerVertex
     * @param vertexStride
     * @param texMatrix
     * @param texBuffer
     * @param textureId
     * @param texStride
     * @param sourceWidth
     * @param sourceHeight
     * @param targetWidth
     * @param targetHeight
     */
    void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
              int vertexCount, int coordsPerVertex, int vertexStride,
              float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {

        int offsetX = 0, offsetY = 0;
        float scaleX = 1, scaleY = 1;

        float sourceAspectRatio = (float) sourceWidth / sourceHeight;
        float targetAspectRatio = (float) targetWidth / targetHeight;

        //Pull the image off the screen if needed, based on the aspect ratio diff.
        if (sourceAspectRatio > targetAspectRatio) {
            int scaledTargetWidth = (int) (targetHeight * sourceAspectRatio);
            offsetX = (scaledTargetWidth - targetWidth) / 2;
            offsetX = -offsetX;
            targetWidth = scaledTargetWidth;
        } else if (sourceAspectRatio < targetAspectRatio) {
            int scaledTargetHeight = (int) (targetWidth / sourceAspectRatio);
            offsetY = (scaledTargetHeight - targetHeight) / 2;
            offsetY = -offsetY;
            targetHeight = scaledTargetHeight;
        }
        GLES20.glViewport(offsetX,offsetY,targetWidth,targetHeight);

//        GlUtil.checkGlError("draw start");
        // Select the program.
        GLES20.glUseProgram(mProgramHandle);

//        GlUtil.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GlUtil.checkGlError("glActiveTexture");

        GLES20.glBindTexture(mTextureTarget, textureId);
//        GlUtil.checkGlError("glBindTexture");


        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);

//        GlUtil.checkGlError("glUniformMatrix4fv");


        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);

//        GlUtil.checkGlError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);

//        GlUtil.checkGlError("glEnableVertexAttribArray");


        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

//        GlUtil.checkGlError("glVertexAttribPointer");


        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
//        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, texStride, texBuffer);
//        GlUtil.checkGlError("glVertexAttribPointer");


        // Populate the convolution kernel, if present.
        if (muKernelLoc >= 0) {
            GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0);
            GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0);
            GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
        }

        // this for beauty face

        if (mProgramType == ProgramType.BEAUTIFY) {
//        Log.d(TAG, "mBeautyLevelParam " + mBeautyLevelParam);
            GLES20.glUniform4fv(mGLLevelParamLocation, 1, FloatBuffer.wrap(mBeautyLevelParam));
        }
//        Log.d(TAG, "Check beauty, width height: "+ muImageStepLocation + "-" + width + "x" + height);
//        if (muImageStepLocation >= 0 && width != 0 && height != 0) {
//            Log.d(TAG, "draw: setting image step width: " + width + " height: " + height);
//            GLES20.glUniform2f(muImageStepLocation, width, height);
//        }
//        if (muImageStepLocation >= 0 && width != 0 && height != 0) {
//            Log.d(TAG, "draw: setting image step width: " + width + " height: " + height);
//            GLES20.glUniform2f(muImageStepLocation, width, height);
//        }


        // draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
//        GlUtil.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
//        GlUtil.checkGlError("glDisableVertextAttribArray");
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
//        GlUtil.checkGlError("glDisableVertextAttribArray");
        GLES20.glBindTexture(mTextureTarget, 0);
        // this below line was added with beauty face
        GLES20.glUseProgram(0);
    }
}
