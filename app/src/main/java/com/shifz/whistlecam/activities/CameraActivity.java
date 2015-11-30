package com.shifz.whistlecam.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.shifz.whistlecam.R;
import com.shifz.whistlecam.utils.PrefHelper;
import com.shifz.whistlecam.whistle.DetectorThread;
import com.shifz.whistlecam.whistle.OnSignalsDetectedListener;
import com.shifz.whistlecam.whistle.RecorderThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener, OnSignalsDetectedListener, Camera.PictureCallback {


    private static final int TAG_NO_FLASH = 1;
    private static final String KEY_FLASH_MODE = "flash_mode";
    private static final String X = CameraActivity.class.getSimpleName();
    private static final String KEY_CAMERA_INDEX = "camera_index";
    private static final int CAMERA_REAR_INDEX = 0;
    private static final String IMAGE_SIZE_WIDTH = "image_size_width";
    private static final String IMAGE_SIZE_HEIGHT = "image_size_height";
    private static final String IMAGE_SIZE_RADIO_ID = "image_size_radio_id";
    private static final int CAMERA_FRONT_INDEX = 1;
    private SurfaceView svCameraPreview;
    private Camera mCamera;

    private ImageButton ibChangeCamera, ibChangeImageSize, ibToggleFlash;
    private RecorderThread recorderThread;
    private DetectorThread detectorThread;
    private static boolean isPhotoBeingTaken = false;
    private PrefHelper prefHelper;
    private int defScrWidth, defScrHeight;
    private AlertDialog alertDialog;
    private int cameraIndex;
    private List<Camera.Size> supPictureSizes;
    private Vibrator vibrator;
    private String flashMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        defScrWidth = metrics.widthPixels;
        defScrHeight = metrics.heightPixels;

        svCameraPreview = (SurfaceView) findViewById(R.id.svCameraPreview);
        svCameraPreview.setKeepScreenOn(true);

        ibChangeCamera = (ImageButton) findViewById(R.id.ibChangeCamera);
        ibToggleFlash = (ImageButton) findViewById(R.id.ibToggleFlash);
        ibChangeImageSize = (ImageButton) findViewById(R.id.ibChangeImageSize);

        ibChangeCamera.setOnClickListener(this);
        ibToggleFlash.setOnClickListener(this);
        ibChangeImageSize.setOnClickListener(this);

        ibToggleFlash.setTag(TAG_NO_FLASH);

        final SurfaceHolder holder = svCameraPreview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        prefHelper = PrefHelper.getInstance(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            try {

                cameraIndex = prefHelper.getIntPref(KEY_CAMERA_INDEX, CAMERA_REAR_INDEX);
                flashMode = prefHelper.getStringPref(KEY_FLASH_MODE, Camera.Parameters.FLASH_MODE_OFF);

                if (flashMode.equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    ibToggleFlash.setImageResource(R.drawable.ic_flash_off_white_36dp);
                } else {
                    ibToggleFlash.setImageResource(R.drawable.ic_flash_on_white_36dp);
                }

                mCamera = Camera.open(cameraIndex);
                Camera.Parameters params = mCamera.getParameters();

                supPictureSizes = params.getSupportedPictureSizes();

                final int imageSizeWidth = prefHelper.getIntPref(IMAGE_SIZE_WIDTH, supPictureSizes.get(0).width);
                final int imageSizeHeight = prefHelper.getIntPref(IMAGE_SIZE_HEIGHT, supPictureSizes.get(0).height);

                params.setFlashMode(flashMode);
                params.setPreviewSize(defScrWidth, defScrHeight);
                params.setPictureSize(imageSizeWidth, imageSizeHeight);

                //JUST TO LOG
                /*for (final String sflashMode : params.getSupportedFlashModes()) {
                    Log.d(X, "Supported flash mode :" + sflashMode);
                }
                for (final Camera.Size size : params.getSupportedPictureSizes()) {
                    Log.d(X, String.format("Supported picture size - H:%d W:%d ", size.height, size.width));
                }
                for (final Camera.Size size : params.getSupportedPreviewSizes()) {
                    Log.d(X, String.format("Supported preview size - H:%d W:%d ", size.height, size.width));
                }*/
                //JUST TO LOG;
                //H:1920 W:2560
                mCamera.setParameters(params);
                mCamera.setPreviewDisplay(svCameraPreview.getHolder());
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (recorderThread == null && detectorThread == null) {
            recorderThread = new RecorderThread();
            recorderThread.start();
            detectorThread = new DetectorThread(recorderThread);
            detectorThread.setOnSignalsDetectedListener(this);
            detectorThread.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }

        detectorThread.stopDetection();
        detectorThread = null;
        recorderThread.stopRecording();
        recorderThread = null;
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


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ibToggleFlash:
                switch (flashMode) {
                    case Camera.Parameters.FLASH_MODE_OFF:
                        flashMode = Camera.Parameters.FLASH_MODE_ON;
                        ibToggleFlash.setImageResource(R.drawable.ic_flash_on_white_36dp);
                        break;
                    case Camera.Parameters.FLASH_MODE_ON:
                        flashMode = Camera.Parameters.FLASH_MODE_OFF;
                        ibToggleFlash.setImageResource(R.drawable.ic_flash_on_white_36dp);
                        break;
                }

                if (mCamera != null) {
                    final Camera.Parameters params = mCamera.getParameters();
                    params.setFlashMode(flashMode);
                    mCamera.setParameters(params);
                }
                break;
            case R.id.ibChangeImageSize:
                showImageSizeDialog();
                break;

            case R.id.ibChangeCamera:
                final int cameraCount = Camera.getNumberOfCameras();
                if (cameraCount > 1) {
                    if (cameraIndex == CAMERA_REAR_INDEX) {
                        cameraIndex = CAMERA_FRONT_INDEX;
                    } else {
                        cameraIndex = CAMERA_REAR_INDEX;
                    }

                    if (mCamera != null) {
                        mCamera.release();
                        mCamera = null;
                    }

                    mCamera = Camera.open(cameraIndex);
                    prefHelper.savePref(KEY_CAMERA_INDEX, cameraIndex);
                    try {
                        mCamera.setPreviewDisplay(svCameraPreview.getHolder());
                        mCamera.startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(CameraActivity.this, R.string.Youve_only_one_cam, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onWhistleDetected() {
        Log.d(X, "Whistle detected");

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
            vibrator.vibrate(100);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isPhotoBeingTaken = false;
    }

    //Showing dialog to change image size
    public void showImageSizeDialog() {

        if (alertDialog == null) {

            final View imageSizeDialogLayout = LayoutInflater.from(this).inflate(R.layout.image_size_dialog_layout, null);

            if (mCamera == null) {
                mCamera = Camera.open(cameraIndex);
            }

            final Camera.Parameters cameraParams = mCamera.getParameters();


            if (supPictureSizes.size() >= 3) {

                final int currentImageSizeRbId = prefHelper.getIntPref(IMAGE_SIZE_RADIO_ID, R.id.rbImageSizeFine);

                final RadioGroup rgImageSizes = (RadioGroup) imageSizeDialogLayout.findViewById(R.id.rgImageSizes);
                rgImageSizes.check(currentImageSizeRbId);

                final RadioButton rbImageSizeFine = (RadioButton) imageSizeDialogLayout.findViewById(R.id.rbImageSizeFine);
                final RadioButton rbImageSizeExtraFine = (RadioButton) imageSizeDialogLayout.findViewById(R.id.rbImageSizeExtraFine);
                final RadioButton rbImageSizeAwesome = (RadioButton) imageSizeDialogLayout.findViewById(R.id.rbImageSizeAwesome);

                final String sizeFineText = String.format("%s (%d x %d)", getString(R.string.Fine), supPictureSizes.get(0).width, supPictureSizes.get(0).height);
                final String sizeExtaFineText = String.format("%s (%d x %d)", getString(R.string.Extra_Fine), supPictureSizes.get(1).width, supPictureSizes.get(1).height);
                final String sizeAwesome = String.format("%s (%d x %d)", getString(R.string.Awesome), supPictureSizes.get(2).width, supPictureSizes.get(2).height);

                rbImageSizeFine.setText(sizeFineText);
                rbImageSizeExtraFine.setText(sizeExtaFineText);
                rbImageSizeAwesome.setText(sizeAwesome);

                alertDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.Choose_Image_Size)
                        .setView(imageSizeDialogLayout)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                int imageSizeIndex = -1;
                                final int rbId = rgImageSizes.getCheckedRadioButtonId();
                                switch (rbId) {
                                    case R.id.rbImageSizeFine:
                                        imageSizeIndex = 0;
                                        break;
                                    case R.id.rbImageSizeExtraFine:
                                        imageSizeIndex = 1;
                                        break;
                                    case R.id.rbImageSizeAwesome:
                                        imageSizeIndex = 2;
                                        break;
                                }

                                final int width = supPictureSizes.get(imageSizeIndex).width;
                                final int height = supPictureSizes.get(imageSizeIndex).height;

                                prefHelper.savePref(IMAGE_SIZE_WIDTH, width);
                                prefHelper.savePref(IMAGE_SIZE_HEIGHT, height);
                                prefHelper.savePref(IMAGE_SIZE_RADIO_ID, rbId);

                                if (mCamera == null) {
                                    mCamera = Camera.open(cameraIndex);
                                }

                                mCamera.stopPreview();
                                cameraParams.setPictureSize(width, height);
                                mCamera.setParameters(cameraParams);
                                try {
                                    mCamera.setPreviewDisplay(svCameraPreview.getHolder());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                mCamera.startPreview();

                                dialog.dismiss();
                            }
                        })
                        .create();
            }
        }

        if (alertDialog != null) {
            alertDialog.show();
        } else {
            Toast.makeText(CameraActivity.this, R.string.Custom_image_size_not_supported, Toast.LENGTH_SHORT).show();
        }

    }
}
