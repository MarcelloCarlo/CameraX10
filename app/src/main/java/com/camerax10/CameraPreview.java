package com.camerax10;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder holder;
    private Camera camera;

    @SuppressWarnings("deprecation")
    public CameraPreview(Context context, Camera camera) {
        super(context);
        this.camera = camera;
        holder = getHolder();
        holder.addCallback(this);
        // Required for API < 11
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            // Preview failed
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Camera release is handled by CameraActivity
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (this.holder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            // Ignore — preview was already stopped
        }

        Camera.Parameters params = camera.getParameters();
        Camera.Size bestSize = getBestPreviewSize(w, h, params);
        if (bestSize != null) {
            params.setPreviewSize(bestSize.width, bestSize.height);
            camera.setParameters(params);
        }

        try {
            camera.setPreviewDisplay(this.holder);
            camera.startPreview();
        } catch (Exception e) {
            // Preview restart failed
        }
    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters params) {
        Camera.Size best = null;
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        if (sizes == null) {
            return null;
        }
        for (Camera.Size size : sizes) {
            if (size.width <= width && size.height <= height) {
                if (best == null || (size.width * size.height > best.width * best.height)) {
                    best = size;
                }
            }
        }
        // If no size fits, use the smallest available
        if (best == null) {
            best = sizes.get(0);
            for (Camera.Size size : sizes) {
                if (size.width * size.height < best.width * best.height) {
                    best = size;
                }
            }
        }
        return best;
    }
}
