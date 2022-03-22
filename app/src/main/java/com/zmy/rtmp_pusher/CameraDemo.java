package com.zmy.rtmp_pusher;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zmy.rtmp_pusher.capture.camerax_capture.CameraXCapture;
import com.zmy.rtmp_pusher.capture.camerax_capture.GLCameraView;
import com.zmy.rtmp_pusher.lib.RtmpCallback;
import com.zmy.rtmp_pusher.lib.RtmpPusher;
import com.zmy.rtmp_pusher.lib.audio_capture.MicAudioCapture;
import com.zmy.rtmp_pusher.lib.pusher.PusherException;

import org.wysaid.myUtils.FileUtil;
import org.wysaid.myUtils.ImageUtil;
import org.wysaid.nativePort.CGEFrameRecorder;

import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImageView;

public class CameraDemo extends AppCompatActivity implements RtmpCallback , View.OnClickListener , CameraXCapture.Ilistener {
    private static final int PERMISSIONS_REQUEST = 8954;
    private final String TAG = "CameraDemo";
    //rtmp://192.168.50.125:19350/live/livestream

    private ImageView imageView;
    private String lastVideoPathFileName = FileUtil.getPath() + "/lastVideoPath.txt";
    private GLCameraView glCameraView;
    private GPUImageView gpuImageView;
    private Button btnRecording;
    private LinearLayout linearLayout;
    private CameraXCapture videoCapture;
    private String mCurrentConfig;
    private PreviewView previewView;

    private MicAudioCapture audioCapture;
    private RtmpPusher rtmpPusher;
    private AlertDialog mAlertDialog;

    @Override
    public void sendBitmap(Bitmap bitmap) {
        if (imageView!= null) {
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageBitmap(bitmap);
                }
            });
        }
    }

    public static class MyButtons extends androidx.appcompat.widget.AppCompatButton {

        public String filterConfig;

        public MyButtons(Context context, String config) {
            super(context);
            filterConfig = config;
        }
    }

    private View.OnClickListener mFilterSwitchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MyButtons btn = (MyButtons) v;
            videoCapture.setFilterWidthConfig(btn.filterConfig);
            mCurrentConfig = btn.filterConfig;
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_camera);

        initView();
        setupFilterMenu();

        if (!isPermissionGranted()){
            requestPermission();
        } else {
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            videoCapture = new CameraXCapture(this.getApplicationContext(), this , 1920 ,1080, CameraSelector.DEFAULT_FRONT_CAMERA, preview , gpuImageView,this, glCameraView);
            audioCapture = new MicAudioCapture(AudioFormat.ENCODING_PCM_16BIT, 44100, AudioFormat.CHANNEL_IN_STEREO);


            //rtmp://192.168.50.125:19350/live/livestream
            // rtmp://live-rtmp.sohatv.vn/ywdacow15xwowa0p7jpdg0w470lws2zr/e405fcf9-4824-4b11-9f87-1b74756d096a
            rtmpPusher = new RtmpPusher.Builder()
                    .url("rtmp://live-rtmp.sohatv.vn/ywdacow15xwowa0p7jpdg0w470lws2zr/e405fcf9-4824-4b11-9f87-1b74756d096a")
                    .audioCapture(audioCapture)
                    .videoCapture(videoCapture)
                    .cacheSize(100)
                    .callback(this)
                    .build();
            try {
                rtmpPusher.start();
            } catch (PusherException e) {
                e.printStackTrace();
            }

        }

        /*Preview previewProvider = new Preview.Builder().build();
        previewProvider.setSurfaceProvider(preview.getSurfaceProvider());*/

    }

    private void setupFilterMenu(){
        for (int i = 0; i != Filter.EFFECT_CONFIGS.length; ++i) {
            MyButtons button = new MyButtons(this, Filter.EFFECT_CONFIGS[i]);
            button.setAllCaps(false);
            if (i == 0)
                button.setText("None");
            else
                button.setText("Filter" + i);
            button.setOnClickListener(mFilterSwitchListener);
            linearLayout.addView(button);
        }
    }

    private void initView(){
        previewView =  findViewById(R.id.preview_test);
        imageView = findViewById(R.id.preview_image);
        glCameraView = findViewById(R.id.glCameraView);
        linearLayout = findViewById(R.id.layout_menu_filter);
        btnRecording = findViewById(R.id.btn_recording);
        gpuImageView = findViewById(R.id.gpuImageView);
        btnRecording.setOnClickListener(this::onClick);

    }

    @Override
    public void onAudioCaptureError(Exception e) {

    }

    @Override
    public void onVideoCaptureError(Exception e) {

    }

    @Override
    public void onVideoEncoderError(Exception e) {

    }

    @Override
    public void onAudioEncoderError(Exception e) {

    }

    @Override
    public void onPusherError(Exception e) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_recording:{
                recording();
            }
        }
    }

    private void recording() {
        if (btnRecording.getText().equals("Recording")){
            btnRecording.setText("Stop");
            String recordFilename = ImageUtil.getPath() + "/rec_" + System.currentTimeMillis() + ".mp4";
            videoCapture.startRecording(30,recordFilename);
            FileUtil.saveTextContent(recordFilename, lastVideoPathFileName);
        } else{
            btnRecording.setText("Recording");
            videoCapture.endRecording(true);
        }
    }

    public boolean isPermissionGranted() {
        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        boolean microPhonePermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        boolean writeStoragePermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return cameraPermissionGranted && microPhonePermissionGranted && writeStoragePermissionGranted;
    }

    private boolean isReadStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermission() {

        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        boolean microPhonePermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;

        boolean writeStoragePermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;


        final List<String> permissionList = new ArrayList();
        if (!cameraPermissionGranted) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (!microPhonePermissionGranted) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!writeStoragePermissionGranted) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissionList.size() > 0) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                mAlertDialog = new AlertDialog.Builder(this)
                        .setTitle("Permission")
                        .setMessage("Camera permission is required to run app")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String[] permissionArray = permissionList.toArray(new String[permissionList.size()]);
                                ActivityCompat.requestPermissions(CameraDemo.this,
                                        permissionArray,
                                        PERMISSIONS_REQUEST);
                            }
                        })
                        .show();
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                mAlertDialog = new AlertDialog.Builder(this)
                        .setTitle("Permission")
                        .setMessage("Write storage is required to run app")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String[] permissionArray = permissionList.toArray(new String[permissionList.size()]);
                                ActivityCompat.requestPermissions(CameraDemo.this,
                                        permissionArray,
                                        PERMISSIONS_REQUEST);
                            }
                        })
                        .show();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                mAlertDialog = new AlertDialog.Builder(this)
                        .setMessage("Microphone permission is required to run this app")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String[] permissionArray = permissionList.toArray(new String[permissionList.size()]);
                                ActivityCompat.requestPermissions(CameraDemo.this,
                                        permissionArray,
                                        PERMISSIONS_REQUEST);
                            }
                        })
                        .show();
            } else {
                String[] permissionArray = permissionList.toArray(new String[permissionList.size()]);
                ActivityCompat.requestPermissions(this, permissionArray,
                        PERMISSIONS_REQUEST);
            }
        }
    }

}
