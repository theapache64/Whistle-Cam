package com.shifz.whistlecam.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.shifz.whistlecam.R;
import com.shifz.whistlecam.utils.PrefHelper;
import com.shifz.whistlecam.whistle.DetectorThread;
import com.shifz.whistlecam.whistle.OnSignalsDetectedListener;
import com.shifz.whistlecam.whistle.RecorderThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener, OnSignalsDetectedListener, Camera.PictureCallback {


    private static final int TAG_NO_FLASH = 1;
    private static final String KEY_FLASH_MODE = "flash_mode";
    private static final String X = CameraActivity.class.getSimpleName();
    private SurfaceView svCameraPreview;
    private Camera mCamera;

    private ImageButton ibChangeCamera, ibToggleFlash;
    private RecorderThread recorderThread;
    private DetectorThread detectorThread;
    private static boolean isPhotoBeingTaken = false;
    private PrefHelper prefHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        svCameraPreview = (SurfaceView) findViewById(R.id.svCameraPreview);
        svCameraPreview.setKeepScreenOn(true);

        ibChangeCamera = (ImageButton) findViewById(R.id.ibChangeCamera);
        ibToggleFlash = (ImageButton) findViewById(R.id.ibToggleFlash);

        ibChangeCamera.setOnClickListener(this);
        ibToggleFlash.setOnClickListener(this);

        ibToggleFlash.setTag(TAG_NO_FLASH);

        final SurfaceHolder holder = svCameraPreview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        prefHelper = PrefHelper.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            try {

                final String flashMode = prefHelper.getStringPref(KEY_FLASH_MODE, Camera.Parameters.FLASH_MODE_OFF);


                mCamera = Camera.open(1);
                Camera.Parameters params = mCamera.getParameters();

                //JUST TO LOG
                for (final String sflashMode : params.getSupportedFlashModes()) {
                    Log.d(X, "Supported flash mode :" + sflashMode);
                }
                for (final Camera.Size size : params.getSupportedPictureSizes()) {
                    Log.d(X, String.format("Supported picture size - H:%d W:%d ", size.height, size.width));
                }
                for (final Camera.Size size : params.getSupportedPreviewSizes()) {
                    Log.d(X, String.format("Supported preview size - H:%d W:%d ", size.height, size.width));
                }
                //JUST TO LOG

                params.setFlashMode(flashMode);
                //H:1920 W:2560
                params.setPreviewSize(480,320);
                mCamera.setParameters(params);
                mCamera.setPreviewDisplay(svCameraPreview.getHolder());
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        recorderThread = new RecorderThread();
        recorderThread.start();
        detectorThread = new DetectorThread(recorderThread);
        detectorThread.setOnSignalsDetectedListener(this);
        detectorThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        detectorThread.stopDetection();
        detectorThread = null;
        recorderThread.stopRecording();
        recorderThread = null;*/

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Toast.makeText(CameraActivity.this, "Surface changed", Toast.LENGTH_SHORT).show();
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibToggleFlash:
                break;
        }
    }

    @Override
    public void onWhistleDetected() {
        Log.d("X", "Whistle detected");
        if (!isPhotoBeingTaken) {
            Log.d("X", "Taking photo...");
            isPhotoBeingTaken = true;
            mCamera.takePicture(null, null, this);
        } else {
            Log.d("X", "Photo being taken");
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        Log.d("X", "Photo captured");

        final int rotation = getWindowManager().getDefaultDisplay().getRotation();

        Log.d("X", "Rotation : " + rotation);

        final File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "WhistleCam");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        final File imageFile = new File(imageDir.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg");
        try {
            final FileOutputStream fos = new FileOutputStream(imageFile);
            final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CameraActivity.this, "Image saved", Toast.LENGTH_SHORT).show();
                }
            });

            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isPhotoBeingTaken = false;
    }
}
