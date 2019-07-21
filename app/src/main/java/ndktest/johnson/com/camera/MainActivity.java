package ndktest.johnson.com.camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Button.OnClickListener {
    private static final int requestCode = 1;
    private Size previewSize;
    private CameraDevice mCameraDevice;
    private String CameraID;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession previewSession;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private TextureView cameraview;
    private Button capture;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private static final int RC_HANDLE_CAMERA_PERM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getCameraPermission();
        init();

    }

    private void init() {
        cameraview = (TextureView)findViewById(R.id.cameraView);
        capture = (Button)findViewById(R.id.capture);
        cameraview.setSurfaceTextureListener(this);
        capture.setOnClickListener(this);
    }

    private void getCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    requestCode);
            /*new AlertDialog.Builder(this)
                    .setMessage("R string request permission")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestCode);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();

                                }
                            })
                    .create();*/
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        setupCamera(width,height);
        openCamera();
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //檢查權限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                //請求權限
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                        RC_HANDLE_CAMERA_PERM);
                return;
            }
            //根據mCameraId來決定要開啟的鏡頭
            manager.openCamera(CameraID, mStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            //預覽畫面
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.e("MainActivity", "onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e("MainActivity", "onError: " + error);
        }
    };

    private void startPreview() {
        SurfaceTexture mSurfaceTexture = cameraview.getSurfaceTexture();
        //設置TextureView的緩衝區大小
        mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(),
                previewSize.getHeight());
        //獲取Surface顯示預覽數據
        Surface mSurface = new Surface(mSurfaceTexture);
        try {
            //創建預覽請求CaptureRequestBuilder，TEMPLATE_PREVIEW
            mCaptureRequestBuilder = mCameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //設置Surface作為預覽顯示界面
            mCaptureRequestBuilder.addTarget(mSurface);
            //創建捕捉畫面，並設置callback
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface),
                    mCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private final CameraCaptureSession.StateCallback mCaptureCallback
            = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                //創建捕獲請求
                mCaptureRequest = mCaptureRequestBuilder.build();
                previewSession = session;
                //設置反覆捕獲數據，持續畫面更新
                previewSession.setRepeatingRequest(mCaptureRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };
    private void setupCamera(int width, int height) {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            String[] cameraList = manager.getCameraIdList();
            for(String cameraid : cameraList){
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraid);
                if(characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                previewSize = getOptialSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                CameraID = cameraid;
                break;
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getOptialSize(Size[] outputSizes, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for(Size option : outputSizes){
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight()
                            - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return outputSizes[0];
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }


    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onClick(View v) {

    }
}
