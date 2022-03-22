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

/**
 * This class essentially represents a viewport-sized sprite that will be rendered with
 * a texture, usually from an external source like the camera or video decoder.
 */
public class FullFrameRect {

    private Object drawSynchronized = new Object();

    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
    private Texture2dProgram mProgram;

    private boolean isDrawing = false;


    /**
     * Prepares the object.
     *
     * @param program The program to use.  FullFrameRect takes ownership, and will release
     *     the program when no longer needed.
     */
    public FullFrameRect(Texture2dProgram program) {
        mProgram = program;
    }

    /**
     * Releases resources.
     * <p>
     * This must be called with the appropriate EGL activity current (i.e. the one that was
     * current when the constructor was called).  If we're about to destroy the EGL activity,
     * there's no value in having the caller make it current just to do this cleanup, so you
     * can pass a flag that will tell this function to skip any EGL-activity-specific cleanup.
     */
    public void release(boolean doEglCleanup) {
        if (mProgram != null) {
            if (doEglCleanup) {
                mProgram.release();
            }
            mProgram = null;
        }
    }

    /**
     * Returns the program currently in use.
     */
    public Texture2dProgram getProgram() {
        return mProgram;
    }

    /**
     * Changes the program.  The previous program will be released.
     * <p>
     * The appropriate EGL activity must be current.
     */
    public void changeProgram(Texture2dProgram program) {
        mProgram.release();
        mProgram = program;
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    public int createTextureObject() {
        return mProgram.createTextureObject();
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     * Draws a cropped camera image along with device screen to preview
     */
    public void drawFrame(int textureId, float[] texMatrix, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        mProgram.draw(GlUtil.IDENTITY_MATRIX, mRectDrawable.getVertexArray(), 0,
                mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix, mRectDrawable.getTexCoordArray(), textureId,
                mRectDrawable.getTexCoordStride(), sourceWidth, sourceHeight, targetWidth, targetHeight);
    }
    /**
     * Draw camera image to server
     */
    public void drawFrame(int textureId, float[] texMatrix) {
        mProgram.draw(GlUtil.IDENTITY_MATRIX, mRectDrawable.getVertexArray(), 0,
                mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix, mRectDrawable.getTexCoordArray(), textureId,
                mRectDrawable.getTexCoordStride());
    }

    /**
     * Draw text overlay to server
     * @param overlayProgram basically is an effect like other effect
     */
    public void drawFrameWithOverlay(Texture2dProgram overlayProgram, float[] mvpMatrix, float[] texMatrix, int overlayTextureId, Bitmap overlayText, Drawable2d overlayDrawable) {
        overlayProgram.setBitmap(overlayText, overlayTextureId);
        overlayProgram.draw(mvpMatrix, overlayDrawable.getVertexArray(), 0,
                overlayDrawable.getVertexCount(), overlayDrawable.getCoordsPerVertex(),
                overlayDrawable.getVertexStride(),
                texMatrix, overlayDrawable.getTexCoordArray(), overlayTextureId,
                overlayDrawable.getTexCoordStride());
    }

    public void drawFrameWithOverlay(Texture2dProgram overlayProgram, float[] mvpMatrix, float[] texMatrix, int overlayTextureId, Bitmap overlayText, Drawable2d overlayDrawable, int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        overlayProgram.setBitmap(overlayText, overlayTextureId);
        overlayProgram.draw(mvpMatrix, overlayDrawable.getVertexArray(), 0,
                overlayDrawable.getVertexCount(), overlayDrawable.getCoordsPerVertex(),
                overlayDrawable.getVertexStride(),
                texMatrix, overlayDrawable.getTexCoordArray(), overlayTextureId,
                overlayDrawable.getTexCoordStride(), sourceWidth, sourceHeight, targetWidth, targetHeight);
    }
}
