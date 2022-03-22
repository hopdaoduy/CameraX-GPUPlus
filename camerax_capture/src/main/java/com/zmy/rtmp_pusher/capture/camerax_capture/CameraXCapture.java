package com.zmy.rtmp_pusher.capture.camerax_capture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.zmy.rtmp_pusher.lib.encoder.EOFHandle;
import com.zmy.rtmp_pusher.lib.log.RtmpLogManager;
import com.zmy.rtmp_pusher.lib.video_capture.VideoCapture;

import org.wysaid.common.Common;
import org.wysaid.nativePort.CGEFrameRecorder;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage3x3ConvolutionFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageColorInvertFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary;
import jp.co.cyberagent.android.gpuimage.GPUImageSketchFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.GPUImageGammaFilter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SuppressLint("RestrictedApi")
public class CameraXCapture extends VideoCapture implements Preview.SurfaceProvider, Consumer<SurfaceRequest.Result>, Observable.Observer<CameraInternal.State> {
    private static final String TAG = CameraXCapture.class.getSimpleName();
    private final Context context;
    private final int width;
    private final int height;
    protected int mTextureID;
    private int outputWidth;
    private int outputHeight;
    private int rotationDegree = 0;
    protected float[] mTransformMatrix = new float[16];
    private CGEFrameRecorder cgeFrameRecorder;

    private final CameraSelector cameraSelector;
    private Ilistener listener;
    private  Preview preview, secondPreview;
    private final LifecycleOwner lifecycleOwner;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Preview encodeCase;
    private SurfaceRequest request;
    private EOFHandle eofHandle;
    private SurfaceTexture surfaceTexture, surfaceTextureRender;
    private GPUImageView gpuImageView;
    private ImageAnalysis imageAnalysis;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Bitmap bitmap;
    private Camera camera;
    private Surface surfaceRender;
    private ProcessCameraProvider cameraProvider;
    private GLCameraView glCameraView;
    private ImageReader imageReader;


    public CameraXCapture(Context context, LifecycleOwner lifecycleOwner, int width, int height, CameraSelector cameraSelector, Preview preview , GPUImageView gpuImageView, Ilistener listener , GLCameraView glCameraView) {
        super();
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.cameraSelector = cameraSelector;
        this.preview = preview;
        this.width = width;
        this.height = height;
        this.gpuImageView = gpuImageView;
        this.listener = listener;
        this.glCameraView = glCameraView;


        secondPreview = new Preview.Builder()
                .setTargetResolution(getRotatedResolution(this.width,this.height))
                .build();
        secondPreview.setSurfaceProvider(this);
       // this.preview.setSurfaceProvider(this::onSurfaceRequested);

        //itit CEG
      /*cgeFrameRecorder = new CGEFrameRecorder();
        cgeFrameRecorder.init(1080, 1920, 1080, 1920);
        cgeFrameRecorder.setSrcRotation((float) (Math.PI / 2.0));
        cgeFrameRecorder.setSrcFlipScale(1.0f, -1.0f);
        cgeFrameRecorder.setRenderFlipScale(1.0f, -1.0f);*/
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

    public void setFilterWidthConfig(String config){
        cgeFrameRecorder.setFilterWidthConfig(config);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void doInitialize() {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                throw new SecurityException("Permission denied");
            }

            //set up ImageAnalytics

            imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(new Size(1080,1920))
                    .build();
            imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(@NonNull ImageProxy image) {
                    try {
                        byte[] data = ImageUtil.imageToJpegByteArray(image);


                      /*  cgeFrameRecorder.update(mTextureID, mTransformMatrix);
                        cgeFrameRecorder.runProc();
                        cgeFrameRecorder.render(1080,1920,1080,1920);*/

                        //
                    } catch (ImageUtil.CodecFailedException e) {
                        e.printStackTrace();
                    }
                    image.close();
                }
            });

            cameraProviderFuture = ProcessCameraProvider.getInstance(context);
            cameraProviderFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        cameraProvider = cameraProviderFuture.get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    cameraProvider.unbindAll();

                    camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, getEncodeCase(), preview);
                  //  glCameraView.attachPreview(preview);


                }
            },ContextCompat.getMainExecutor(context));

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
            return new Size(width, width);
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void start(Surface surface, EOFHandle handle) {
     /*   request.provideSurface(surface, ContextCompat.getMainExecutor(context), this);
        this.eofHandle = handle;*/

        request.provideSurface(surface, ContextCompat.getMainExecutor(context), this);

    }

    @Override
    public void accept(SurfaceRequest.Result result) {
        Log.d("HopLog", "onAccept");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private UseCase getEncodeCase() {
        if (encodeCase == null) {
            Log.d("HopLog", "getEncodeCase");
            encodeCase = createUseCase(this::onSurfaceRequested);
        }
        return encodeCase;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
        Log.d("HopLog", "onSurfaceRequested");
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

        //glCameraView.doWithSurfaceRequest(request);
    }

    @Override
    public void onNewData(@Nullable CameraInternal.State value) {

    }

    @Override
    public void onError(@NonNull Throwable t) {
        if (callback != null) callback.onVideoCaptureError(new Exception(t));
    }

    private Preview  createUseCase(Preview.SurfaceProvider provider) {
        Preview useCase = new Preview.Builder()
                .setTargetResolution(getRotatedResolution(width, height))
                .build();
        useCase.setSurfaceProvider(provider);
        return useCase;
    }

    public void startRecording(int i, String recordFilename) {
        cgeFrameRecorder.startRecording(i,recordFilename);
    }

    public void endRecording(boolean b) {
        cgeFrameRecorder.endRecording(b);
    }

    public interface Ilistener{
        void sendBitmap(Bitmap bitmap);
    }
}
