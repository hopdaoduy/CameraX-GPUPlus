package com.zmy.rtmp_pusher.capture.camerax_capture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Observable;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.zmy.rtmp_pusher.lib.encoder.EOFHandle;
import com.zmy.rtmp_pusher.lib.log.RtmpLogManager;
import com.zmy.rtmp_pusher.lib.video_capture.VideoCapture;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage3x3ConvolutionFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageColorInvertFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary;
import jp.co.cyberagent.android.gpuimage.GPUImageSketchFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.GPUImageGammaFilter;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressLint("RestrictedApi")
public class CameraXCapture extends VideoCapture implements Preview.SurfaceProvider, Consumer<SurfaceRequest.Result>, Observable.Observer<CameraInternal.State> {
    private final Context context;
    private final int width;
    private final int height;
    private int outputWidth;
    private int outputHeight;
    private int rotationDegree = 0;
    private final CameraSelector cameraSelector;
    private final Preview preview;
    private final LifecycleOwner lifecycleOwner;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Preview encodeCase;
    private SurfaceRequest request;
    private EOFHandle eofHandle;

    private GPUImageView gpuImageView;
    private ImageAnalysis imageAnalysis;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Bitmap bitmap;


    public CameraXCapture(Context context, LifecycleOwner lifecycleOwner, int width, int height, CameraSelector cameraSelector, Preview preview , GPUImageView gpuImageView) {
        super();
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.cameraSelector = cameraSelector;
        this.preview = preview;
        this.width = width;
        this.height = height;
        this.gpuImageView = gpuImageView;

        imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                Bitmap bitmap = allocateBitmapIfNecessary(image.getWidth(), image.getHeight());
                Log.d("HopLog", bitmap.toString());


                byte[] bytes = new byte[image.getPlanes()[0].getBuffer().remaining()];
                image.getPlanes()[0].getBuffer().get(bytes);




            }
        });
        cameraProviderFuture = ProcessCameraProvider.getInstance(context);
/*        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {

            }
        }, ContextCompat.getMainExecutor(context));*/
    }

    private void setFilter(GPUImageFilter filter){
        gpuImageView.setFilter(filter);
    }


    private Bitmap allocateBitmapIfNecessary(int width, int height){
        if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        return bitmap;
    }

    @Override
    public void doInitialize() {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                throw new SecurityException("Permission denied");
            }
            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, getEncodeCase(), preview);
        } catch (Exception e) {
            if (callback != null) callback.onVideoCaptureInit(this, e);
        }
    }

    @Override
    public void release() {
        try {
            setReady(false);
            if (request != null) {
                request.getCamera().getCameraState().removeObserver(this);
            }
            if (eofHandle != null) {
                eofHandle.signalEndOfInputStream();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getDeviceOrientation() {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        switch (wm.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_0:
            default:
                return 0;

        }
    }

    private Size getRotatedResolution(int width, int height) {
        if (getDeviceOrientation() == 0 || getDeviceOrientation() == 180) {
            return new Size(height, width);
        } else {
            return new Size(width, height);
        }
    }

    @Override
    public int getWidth() {
        return outputWidth;
    }

    @Override
    public int getHeight() {
        return outputHeight;
    }

    @Override
    public void start(Surface surface, EOFHandle handle) {
        request.provideSurface(surface, ContextCompat.getMainExecutor(context), this);
        this.eofHandle = handle;
    }

    @Override
    public void accept(SurfaceRequest.Result result) {

    }

    private UseCase getEncodeCase() {
        if (encodeCase == null) {
            encodeCase = createUseCase(this);
        }
        return encodeCase;
    }

    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
        outputWidth = request.getResolution().getWidth();
        outputHeight = request.getResolution().getHeight();
        request.getCamera().getCameraState().removeObserver(this);
        RtmpLogManager.d("rtmp", "camera output size = " + outputWidth + "x" + outputHeight);
        request.getCamera().getCameraState().addObserver(ContextCompat.getMainExecutor(context), this);
        this.request = request;
        if (callback != null) {
            setReady(true);
            callback.onVideoCaptureInit(this, null);
        }
    }

    @Override
    public void onNewData(@Nullable CameraInternal.State value) {

    }

    @Override
    public void onError(@NonNull Throwable t) {
        if (callback != null) callback.onVideoCaptureError(new Exception(t));
    }

    private Preview createUseCase(Preview.SurfaceProvider provider) {
        Preview useCase = new Preview.Builder()
                .setTargetResolution(getRotatedResolution(width, height))
                .build();
        useCase.setSurfaceProvider(provider);
        return useCase;
    }

}
