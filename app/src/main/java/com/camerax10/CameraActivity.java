package com.camerax10;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.database.Cursor;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Handler;
import android.provider.MediaStore;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraActivity extends Activity {

    static class RotatableButton extends Button {
        private int rotationDeg = 0;

        public RotatableButton(Context context) {
            super(context);
        }

        public void setCustomRotation(int degrees) {
            if (degrees != rotationDeg) {
                rotationDeg = degrees;
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (rotationDeg != 0) {
                canvas.save();
                canvas.rotate(rotationDeg, getWidth() / 2f, getHeight() / 2f);
                super.onDraw(canvas);
                canvas.restore();
            } else {
                super.onDraw(canvas);
            }
        }
    }

    static class GridOverlay extends View {
        private String gridType = "none";
        private final android.graphics.Paint paint = new android.graphics.Paint();

        public GridOverlay(Context context) {
            super(context);
            paint.setColor(0x80FFFFFF);
            paint.setStrokeWidth(1f);
            paint.setAntiAlias(true);
        }

        public void setGridType(String type) {
            gridType = type;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            if ("thirds".equals(gridType)) {
                float x1 = w / 3f, x2 = 2 * w / 3f;
                float y1 = h / 3f, y2 = 2 * h / 3f;
                canvas.drawLine(x1, 0, x1, h, paint);
                canvas.drawLine(x2, 0, x2, h, paint);
                canvas.drawLine(0, y1, w, y1, paint);
                canvas.drawLine(0, y2, w, y2, paint);
            } else if ("grid4x4".equals(gridType)) {
                for (int i = 1; i < 4; i++) {
                    float x = w * i / 4f;
                    float y = h * i / 4f;
                    canvas.drawLine(x, 0, x, h, paint);
                    canvas.drawLine(0, y, w, y, paint);
                }
            } else if ("crosshair".equals(gridType)) {
                float cx = w / 2f, cy = h / 2f;
                canvas.drawLine(cx, 0, cx, h, paint);
                canvas.drawLine(0, cy, w, cy, paint);
            } else if ("golden".equals(gridType)) {
                float r = 0.618f;
                float x1 = w * (1 - r), x2 = w * r;
                float y1 = h * (1 - r), y2 = h * r;
                canvas.drawLine(x1, 0, x1, h, paint);
                canvas.drawLine(x2, 0, x2, h, paint);
                canvas.drawLine(0, y1, w, y1, paint);
                canvas.drawLine(0, y2, w, y2, paint);
            }
        }
    }

    static class FocusCrosshair extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint();
        private int color = 0xFFFFCC00;

        public FocusCrosshair(Context context) {
            super(context);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setAntiAlias(true);
        }

        public void setColor(int c) {
            color = c;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(color);
            int w = getWidth();
            int h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float arm = w * 0.35f;
            float gap = w * 0.1f;
            // top
            canvas.drawLine(cx, cy - arm, cx, cy - gap, paint);
            // bottom
            canvas.drawLine(cx, cy + gap, cx, cy + arm, paint);
            // left
            canvas.drawLine(cx - arm, cy, cx - gap, cy, paint);
            // right
            canvas.drawLine(cx + gap, cy, cx + arm, cy, paint);
            // corner ticks
            float r = arm * 0.85f;
            float tick = arm * 0.25f;
            // top-left
            canvas.drawLine(cx - r, cy - r, cx - r + tick, cy - r, paint);
            canvas.drawLine(cx - r, cy - r, cx - r, cy - r + tick, paint);
            // top-right
            canvas.drawLine(cx + r, cy - r, cx + r - tick, cy - r, paint);
            canvas.drawLine(cx + r, cy - r, cx + r, cy - r + tick, paint);
            // bottom-left
            canvas.drawLine(cx - r, cy + r, cx - r + tick, cy + r, paint);
            canvas.drawLine(cx - r, cy + r, cx - r, cy + r - tick, paint);
            // bottom-right
            canvas.drawLine(cx + r, cy + r, cx + r - tick, cy + r, paint);
            canvas.drawLine(cx + r, cy + r, cx + r, cy + r - tick, paint);
        }
    }

    private Camera camera;
    private CameraPreview preview;
    private FrameLayout previewFrame;
    private GridOverlay gridOverlay;
    private RotatableButton flashBtn;
    private RotatableButton focusBtn;
    private View shutterBtn;
    private RotatableButton settingsBtn;
    private RotatableButton picSizeBtn;
    private RotatableButton sceneModeBtn;
    private RotatableButton modeToggleBtn;
    private RotatableButton videoSizeBtn;
    private FocusCrosshair focusIndicator;
    private View recordingIndicator;
    private RotatableButton gpsIndicator;
    private ImageView thumbnailView;
    private File lastMediaFile;
    private boolean lastMediaIsVideo = false;

    private List<String> supportedFlashModes;
    private List<String> supportedFocusModes;
    private int currentFlashIndex = 0;
    private int currentFocusIndex = 0;
    private int currentPicSizeIndex = 0;
    private int currentSceneModeIndex = 0;
    private boolean isTakingPicture = false;
    private boolean isFocusing = false;
    private boolean hardwareFocusDone = false;
    private boolean captureAfterFocus = false;

    private boolean isVideoMode = false;
    private boolean isRecording = false;
    private MediaRecorder mediaRecorder;
    private File currentVideoFile;
    private Handler blinkHandler = new Handler();
    private String[] videoSizes;
    private int currentVideoSizeIndex = 0;
    private boolean previewOnly = false;
    private LocationManager locationManager;
    private Location lastLocation;
    private int currentZoom = 0;
    private int maxZoom = 0;
    private boolean zoomSupported = false;

    private SoundPool soundPool;
    private int shutterSoundId = -1;
    private int focusSoundId = -1;
    private int videoStartSoundId = -1;
    private int videoStopSoundId = -1;

    private OrientationEventListener orientationListener;
    private int deviceOrientation = 0;
    private int currentUIRotation = 0;
    private int landscapeBase = -1;

    private String[] pictureSizes;
    private String[] whiteBalances;
    private String[] sceneModes;
    private String[] colorEffects;
    private String[] focusModes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        buildUI();

        shutterBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isVideoMode) {
                    if (isRecording) stopRecording();
                    else startRecording();
                } else {
                    takePicture();
                }
            }
        });

        flashBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cycleFlashMode();
            }
        });

        focusBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cycleFocusMode();
            }
        });

        settingsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openSettings();
            }
        });

        picSizeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cyclePicSize();
            }
        });

        sceneModeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cycleSceneMode();
            }
        });

        modeToggleBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleMode();
            }
        });

        videoSizeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cycleVideoSize();
            }
        });

        previewFrame.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && camera != null && !isFocusing && !isRecording) {
                    try {
                        String mode = camera.getParameters().getFocusMode();
                        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(mode)
                                || Camera.Parameters.FOCUS_MODE_MACRO.equals(mode)) {
                            isFocusing = true;
                            showFocusing();
                            camera.autoFocus(new Camera.AutoFocusCallback() {
                                public void onAutoFocus(boolean success, Camera cam) {
                                    isFocusing = false;
                                    showFocusResult(success);
                                    reapplyTorch();
                                    if (captureAfterFocus) {
                                        captureAfterFocus = false;
                                        captureImage(cam);
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        isFocusing = false;
                    }
                    return true;
                }
                return false;
            }
        });

        thumbnailView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (lastMediaFile != null && lastMediaFile.exists()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    String mime = lastMediaIsVideo ? "video/3gpp" : "image/jpeg";
                    intent.setDataAndType(Uri.fromFile(lastMediaFile), mime);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        // No viewer available
                    }
                }
            }
        });
    }

    private void buildUI() {
        FrameLayout root = new FrameLayout(this);

        // Camera preview container
        previewFrame = new FrameLayout(this);
        root.addView(previewFrame, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));

        // Grid overlay
        gridOverlay = new GridOverlay(this);
        previewFrame.addView(gridOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));

        // Focus indicator (centered crosshair on preview)
        focusIndicator = new FocusCrosshair(this);
        focusIndicator.setVisibility(View.GONE);
        FrameLayout.LayoutParams focusParams = new FrameLayout.LayoutParams(dp(60), dp(60));
        focusParams.gravity = Gravity.CENTER;
        previewFrame.addView(focusIndicator, focusParams);

        // Controls sidebar
        LinearLayout sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setGravity(Gravity.CENTER_HORIZONTAL);
        sidebar.setBackgroundColor(0x80000000);
        int pad = dp(4);
        sidebar.setPadding(pad, pad, pad, pad);

        FrameLayout.LayoutParams sidebarParams = new FrameLayout.LayoutParams(
                dp(64), ViewGroup.LayoutParams.FILL_PARENT);
        sidebarParams.gravity = Gravity.RIGHT;
        root.addView(sidebar, sidebarParams);

        // Flash button (square to avoid overlap when rotated)
        flashBtn = makeButton("OFF", 0x40FFCC00);
        LinearLayout.LayoutParams flashParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        flashParams.topMargin = dp(2);
        sidebar.addView(flashBtn, flashParams);

        // Focus button (square)
        focusBtn = makeButton("AF", 0x4000CCFF);
        LinearLayout.LayoutParams focusP = new LinearLayout.LayoutParams(dp(48), dp(48));
        focusP.topMargin = dp(4);
        sidebar.addView(focusBtn, focusP);

        // Spacer
        sidebar.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1.0f));

        // Shutter button
        shutterBtn = new View(this);
        shutterBtn.setBackgroundDrawable(makeShutterDrawable());
        shutterBtn.setClickable(true);
        shutterBtn.setFocusable(true);
        sidebar.addView(shutterBtn, new LinearLayout.LayoutParams(dp(56), dp(56)));

        // Spacer
        sidebar.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1.0f));

        // Mode toggle button (photo/video)
        modeToggleBtn = makeButton("CAM", 0x4000CC00);
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        modeParams.topMargin = dp(4);
        sidebar.addView(modeToggleBtn, modeParams);

        // Settings button (square)
        settingsBtn = makeButton("SET", 0x40AAAAAA);
        LinearLayout.LayoutParams setParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        setParams.bottomMargin = dp(2);
        sidebar.addView(settingsBtn, setParams);

        // Picture size button (top-left corner, square)
        picSizeBtn = makeButton("", 0x80000000);
        picSizeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        FrameLayout.LayoutParams picSizeParams = new FrameLayout.LayoutParams(dp(44), dp(44));
        picSizeParams.gravity = Gravity.TOP | Gravity.LEFT;
        picSizeParams.leftMargin = dp(4);
        picSizeParams.topMargin = dp(4);
        root.addView(picSizeBtn, picSizeParams);

        // Scene mode button (top-left, below pic size, square)
        sceneModeBtn = makeButton("", 0x80000000);
        sceneModeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        FrameLayout.LayoutParams sceneParams = new FrameLayout.LayoutParams(dp(44), dp(44));
        sceneParams.gravity = Gravity.TOP | Gravity.LEFT;
        sceneParams.leftMargin = dp(4);
        sceneParams.topMargin = dp(52);
        root.addView(sceneModeBtn, sceneParams);

        // Video size button (top-left corner, same position as pic size, hidden by default)
        videoSizeBtn = makeButton("", 0x80000000);
        videoSizeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        videoSizeBtn.setVisibility(View.GONE);
        FrameLayout.LayoutParams vidSizeParams = new FrameLayout.LayoutParams(dp(44), dp(44));
        vidSizeParams.gravity = Gravity.TOP | Gravity.LEFT;
        vidSizeParams.leftMargin = dp(4);
        vidSizeParams.topMargin = dp(4);
        root.addView(videoSizeBtn, vidSizeParams);

        // Last-photo thumbnail (bottom-left corner)
        thumbnailView = new ImageView(this);
        thumbnailView.setBackgroundColor(0xCC000000);
        thumbnailView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnailView.setVisibility(View.GONE);
        int thumbPad = dp(2);
        thumbnailView.setPadding(thumbPad, thumbPad, thumbPad, thumbPad);

        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(dp(56), dp(56));
        thumbParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        thumbParams.leftMargin = dp(4);
        thumbParams.bottomMargin = dp(4);
        root.addView(thumbnailView, thumbParams);

        // GPS indicator (bottom-left, above thumbnail, square)
        gpsIndicator = new RotatableButton(this);
        gpsIndicator.setText("GPS");
        gpsIndicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        gpsIndicator.setTextColor(0xFFFFFFFF);
        gpsIndicator.setTypeface(gpsIndicator.getTypeface(), Typeface.BOLD);
        gpsIndicator.setBackgroundColor(0x80666666);
        gpsIndicator.setPadding(0, 0, 0, 0);
        FrameLayout.LayoutParams gpsParams = new FrameLayout.LayoutParams(dp(28), dp(28));
        gpsParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
        gpsParams.leftMargin = dp(4);
        gpsParams.bottomMargin = dp(64);
        root.addView(gpsIndicator, gpsParams);

        // Recording indicator (red dot, top center)
        recordingIndicator = new View(this);
        GradientDrawable recDot = new GradientDrawable();
        recDot.setShape(GradientDrawable.OVAL);
        recDot.setColor(0xFFFF0000);
        recordingIndicator.setBackgroundDrawable(recDot);
        recordingIndicator.setVisibility(View.GONE);
        FrameLayout.LayoutParams recParams = new FrameLayout.LayoutParams(dp(12), dp(12));
        recParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        recParams.topMargin = dp(8);
        root.addView(recordingIndicator, recParams);

        setContentView(root);
    }

    private RotatableButton makeButton(String text, int bgColor) {
        RotatableButton btn = new RotatableButton(this);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTypeface(btn.getTypeface(), Typeface.BOLD);
        btn.setBackgroundColor(bgColor);
        btn.setPadding(0, 0, 0, 0);
        return btn;
    }

    private Drawable makeShutterDrawable() {
        StateListDrawable states = new StateListDrawable();

        GradientDrawable pressed = new GradientDrawable();
        pressed.setShape(GradientDrawable.OVAL);
        pressed.setColor(0xFFCCCCCC);
        pressed.setStroke(dp(3), 0xFF888888);

        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.OVAL);
        normal.setColor(0xFFFFFFFF);
        normal.setStroke(dp(3), 0xFF888888);

        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{}, normal);
        return states;
    }

    private void reapplyTorch() {
        if (camera == null || supportedFlashModes == null || supportedFlashModes.isEmpty()) return;
        String mode = supportedFlashModes.get(currentFlashIndex);
        if (Camera.Parameters.FLASH_MODE_TORCH.equals(mode)) {
            try {
                Camera.Parameters params = camera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(params);
            } catch (Exception e) { }
        }
    }

    private void showFocusing() {
        focusIndicator.setColor(0xFFFFCC00);
        focusIndicator.setVisibility(View.VISIBLE);
        focusIndicator.removeCallbacks(hideFocusRunnable);
    }

    private void showFocusResult(boolean success) {
        if (success) playFocusSound();
        focusIndicator.setColor(success ? 0xFF00CC00 : 0xFFCC0000);
        focusIndicator.removeCallbacks(hideFocusRunnable);
        focusIndicator.postDelayed(hideFocusRunnable, 800);
    }

    private final Runnable hideFocusRunnable = new Runnable() {
        public void run() {
            focusIndicator.setVisibility(View.GONE);
        }
    };

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }

    private void updateUIRotation(int newUIRotation) {
        if (newUIRotation == currentUIRotation) return;
        currentUIRotation = newUIRotation;
        RotatableButton[] btns = {flashBtn, focusBtn, settingsBtn, picSizeBtn, sceneModeBtn, modeToggleBtn, videoSizeBtn, gpsIndicator};
        for (RotatableButton b : btns) {
            if (b != null) b.setCustomRotation(newUIRotation);
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            lastLocation = location;
            runOnUiThread(new Runnable() {
                public void run() { updateGpsIndicator(); }
            });
        }
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        public void onProviderEnabled(String provider) { }
        public void onProviderDisabled(String provider) { }
    };

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
        checkStorage();
        startLocationUpdates();
        loadLastMediaThumbnail();
        initSounds();
        gridOverlay.setGridType(PreferenceManager.getDefaultSharedPreferences(this)
                .getString("grid_lines", "none"));
        orientationListener = new OrientationEventListener(this) {
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;
                deviceOrientation = ((orientation + 45) / 90 * 90) % 360;
                if (landscapeBase < 0) {
                    if (deviceOrientation == 90 || deviceOrientation == 270) {
                        landscapeBase = deviceOrientation;
                    }
                    return;
                }
                final int uiRot = (landscapeBase - deviceOrientation + 360) % 360;
                runOnUiThread(new Runnable() {
                    public void run() {
                        updateUIRotation(uiRot);
                    }
                });
            }
        };
        orientationListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            stopRecording();
        }
        stopLocationUpdates();
        releaseSounds();
        if (orientationListener != null) {
            orientationListener.disable();
            orientationListener = null;
        }
        releaseCamera();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (isVideoMode) {
                if (isRecording) stopRecording();
                else startRecording();
            } else {
                takePicture();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_FOCUS) {
            if (camera != null && !isFocusing) {
                try {
                    isFocusing = true;
                    hardwareFocusDone = false;
                    showFocusing();
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        public void onAutoFocus(boolean success, Camera cam) {
                            isFocusing = false;
                            hardwareFocusDone = true;
                            showFocusResult(success);
                            reapplyTorch();
                            if (captureAfterFocus) {
                                captureAfterFocus = false;
                                captureImage(cam);
                            }
                        }
                    });
                } catch (Exception e) {
                    isFocusing = false;
                }
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            String action = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("volume_key_action", "none");
            if ("capture".equals(action)) {
                if (isVideoMode) {
                    if (isRecording) stopRecording();
                    else startRecording();
                } else {
                    takePicture();
                }
                return true;
            } else if ("zoom".equals(action)) {
                handleZoom(keyCode == KeyEvent.KEYCODE_VOLUME_UP);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void loadLastMediaThumbnail() {
        if (lastMediaFile != null) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    String imagePath = null;
                    long imageDate = 0;
                    String videoPath = null;
                    long videoDate = 0;

                    Cursor ic = getContentResolver().query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED},
                            null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
                    if (ic != null) {
                        if (ic.moveToFirst()) {
                            imagePath = ic.getString(0);
                            imageDate = ic.getLong(1);
                        }
                        ic.close();
                    }

                    Cursor vc = getContentResolver().query(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            new String[]{MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED},
                            null, null, MediaStore.Video.Media.DATE_ADDED + " DESC");
                    if (vc != null) {
                        if (vc.moveToFirst()) {
                            videoPath = vc.getString(0);
                            videoDate = vc.getLong(1);
                        }
                        vc.close();
                    }

                    if (imagePath == null && videoPath == null) return;

                    final boolean isVideo = (videoPath != null && videoDate >= imageDate);
                    final File file = new File(isVideo ? videoPath : imagePath);
                    if (!file.exists()) return;

                    final Bitmap thumb;
                    if (isVideo) {
                        thumb = makeVideoThumbnail();
                    } else {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 16;
                        thumb = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                    }
                    if (thumb == null) return;

                    runOnUiThread(new Runnable() {
                        public void run() {
                            lastMediaFile = file;
                            lastMediaIsVideo = isVideo;
                            thumbnailView.setImageBitmap(thumb);
                            thumbnailView.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (Exception e) {
                    // MediaStore query failed
                }
            }
        }).start();
    }

    private void handleZoom(boolean zoomIn) {
        if (camera == null || !zoomSupported) {
            Toast.makeText(this, "Zoom not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Camera.Parameters params = camera.getParameters();
            if (zoomIn && currentZoom < maxZoom) {
                currentZoom++;
            } else if (!zoomIn && currentZoom > 0) {
                currentZoom--;
            }
            java.lang.reflect.Method setZoomMethod = Camera.Parameters.class.getMethod("setZoom", int.class);
            setZoomMethod.invoke(params, currentZoom);
            camera.setParameters(params);
        } catch (Exception e) {
            Toast.makeText(this, "Zoom failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkStorage() {
        String state = Environment.getExternalStorageState();
        previewOnly = !Environment.MEDIA_MOUNTED.equals(state);
        if (previewOnly) {
            shutterBtn.setEnabled(false);
            shutterBtn.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "No SD card — preview only", Toast.LENGTH_LONG).show();
        } else {
            shutterBtn.setEnabled(true);
            shutterBtn.setVisibility(View.VISIBLE);
        }
    }

    private void startLocationUpdates() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 5000, 0, locationListener);
                if (lastLocation == null) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
        } catch (Exception e) {
            // Location not available
        }
        updateGpsIndicator();
    }

    private void updateGpsIndicator() {
        if (gpsIndicator == null) return;
        if (lastLocation != null) {
            gpsIndicator.setBackgroundColor(0x8000AA00);
        } else {
            gpsIndicator.setBackgroundColor(0x80666666);
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception e) { }
            locationManager = null;
        }
    }

    private void openCamera() {
        try {
            camera = Camera.open();
            Camera.Parameters params = camera.getParameters();

            cacheCapabilities(params);
            try {
                java.lang.reflect.Method isZoomMethod = Camera.Parameters.class.getMethod("isZoomSupported");
                Boolean supported = (Boolean) isZoomMethod.invoke(params);
                zoomSupported = supported != null && supported;
                if (zoomSupported) {
                    java.lang.reflect.Method getMaxMethod = Camera.Parameters.class.getMethod("getMaxZoom");
                    maxZoom = (Integer) getMaxMethod.invoke(params);
                }
            } catch (Exception e) {
                zoomSupported = false;
            }
            restoreFlashMode(params);
            restoreFocusMode(params);
            restorePicSize(params);
            restoreSceneMode(params);
            restoreVideoSize();
            applySettings(params);

            camera.setParameters(params);

            preview = new CameraPreview(this, camera);
            previewFrame.addView(preview, 0);

        } catch (Exception e) {
            String msg = "Cannot open camera: " + e.getMessage();
            android.util.Log.e("CameraX10", msg, e);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void releaseCamera() {
        isTakingPicture = false;
        isFocusing = false;
        hardwareFocusDone = false;
        captureAfterFocus = false;
        currentZoom = 0;
        releaseMediaRecorder();
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (Exception e) {
                // Ignore
            }
            camera.release();
            camera = null;
        }
        if (preview != null) {
            previewFrame.removeView(preview);
            preview = null;
        }
    }

    private void cacheCapabilities(Camera.Parameters params) {
        supportedFlashModes = params.getSupportedFlashModes();
        if (supportedFlashModes == null) {
            supportedFlashModes = new ArrayList<String>();
            flashBtn.setVisibility(View.GONE);
        }

        supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes == null) {
            supportedFocusModes = new ArrayList<String>();
        }

        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        if (sizes != null) {
            pictureSizes = new String[sizes.size()];
            for (int i = 0; i < sizes.size(); i++) {
                pictureSizes[i] = sizes.get(i).width + "x" + sizes.get(i).height;
            }
        }

        List<String> wb = params.getSupportedWhiteBalance();
        whiteBalances = wb != null ? wb.toArray(new String[0]) : null;

        List<String> scenes = params.getSupportedSceneModes();
        sceneModes = scenes != null ? scenes.toArray(new String[0]) : null;

        List<String> effects = params.getSupportedColorEffects();
        colorEffects = effects != null ? effects.toArray(new String[0]) : null;

        focusModes = supportedFocusModes.toArray(new String[0]);

        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        if (previewSizes != null && !previewSizes.isEmpty()) {
            videoSizes = new String[previewSizes.size()];
            for (int i = 0; i < previewSizes.size(); i++) {
                videoSizes[i] = previewSizes.get(i).width + "x" + previewSizes.get(i).height;
            }
        }
    }

    private void restoreFlashMode(Camera.Parameters params) {
        if (supportedFlashModes.isEmpty()) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String saved = prefs.getString("flash_mode", Camera.Parameters.FLASH_MODE_OFF);
        int idx = supportedFlashModes.indexOf(saved);
        currentFlashIndex = idx >= 0 ? idx : 0;
        params.setFlashMode(supportedFlashModes.get(currentFlashIndex));
        updateFlashButton();
    }

    private void restoreFocusMode(Camera.Parameters params) {
        if (supportedFocusModes.isEmpty()) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String saved = prefs.getString("focus_mode", Camera.Parameters.FOCUS_MODE_AUTO);
        int idx = supportedFocusModes.indexOf(saved);
        currentFocusIndex = idx >= 0 ? idx : 0;
        params.setFocusMode(supportedFocusModes.get(currentFocusIndex));
        updateFocusButton();
    }

    private void restorePicSize(Camera.Parameters params) {
        if (pictureSizes == null || pictureSizes.length == 0) {
            picSizeBtn.setVisibility(View.GONE);
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String saved = prefs.getString("picture_size", pictureSizes[0]);
        currentPicSizeIndex = 0;
        for (int i = 0; i < pictureSizes.length; i++) {
            if (pictureSizes[i].equals(saved)) {
                currentPicSizeIndex = i;
                break;
            }
        }
        String[] parts = pictureSizes[currentPicSizeIndex].split("x");
        params.setPictureSize(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        updatePicSizeButton();
    }

    private void restoreSceneMode(Camera.Parameters params) {
        if (sceneModes == null || sceneModes.length == 0) {
            sceneModeBtn.setVisibility(View.GONE);
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String saved = prefs.getString("scene_mode", Camera.Parameters.SCENE_MODE_AUTO);
        currentSceneModeIndex = 0;
        for (int i = 0; i < sceneModes.length; i++) {
            if (sceneModes[i].equals(saved)) {
                currentSceneModeIndex = i;
                break;
            }
        }
        params.setSceneMode(sceneModes[currentSceneModeIndex]);
        updateSceneModeButton();
    }

    private void applySettings(Camera.Parameters params) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int quality = Integer.parseInt(prefs.getString("jpeg_quality", "90"));
        params.setJpegQuality(quality);

        String wb = prefs.getString("white_balance", "");
        if (wb.length() > 0) params.setWhiteBalance(wb);

        String effect = prefs.getString("color_effect", "");
        if (effect.length() > 0) params.setColorEffect(effect);
    }

    private void cycleFlashMode() {
        if (camera == null || supportedFlashModes.isEmpty()) return;

        currentFlashIndex = (currentFlashIndex + 1) % supportedFlashModes.size();
        String mode = supportedFlashModes.get(currentFlashIndex);

        try {
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(mode);
            camera.setParameters(params);
        } catch (Exception e) {
            // Ignore
        }

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("flash_mode", mode).commit();
        updateFlashButton();
    }

    private void cycleFocusMode() {
        if (camera == null || supportedFocusModes.isEmpty()) return;

        currentFocusIndex = (currentFocusIndex + 1) % supportedFocusModes.size();
        String mode = supportedFocusModes.get(currentFocusIndex);

        try {
            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(mode);
            camera.setParameters(params);
        } catch (Exception e) {
            // Ignore
        }

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("focus_mode", mode).commit();
        updateFocusButton();
    }

    private void cyclePicSize() {
        if (camera == null || pictureSizes == null || pictureSizes.length == 0) return;

        currentPicSizeIndex = (currentPicSizeIndex + 1) % pictureSizes.length;
        String size = pictureSizes[currentPicSizeIndex];

        try {
            String[] parts = size.split("x");
            Camera.Parameters params = camera.getParameters();
            params.setPictureSize(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            camera.setParameters(params);
        } catch (Exception e) {
            // Ignore
        }

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("picture_size", size).commit();
        updatePicSizeButton();
    }

    private void cycleSceneMode() {
        if (camera == null || sceneModes == null || sceneModes.length == 0) return;

        currentSceneModeIndex = (currentSceneModeIndex + 1) % sceneModes.length;
        String mode = sceneModes[currentSceneModeIndex];

        try {
            Camera.Parameters params = camera.getParameters();
            params.setSceneMode(mode);
            camera.setParameters(params);
        } catch (Exception e) {
            // Ignore
        }

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("scene_mode", mode).commit();
        updateSceneModeButton();
    }

    private void updateFlashButton() {
        if (supportedFlashModes.isEmpty()) return;
        String mode = supportedFlashModes.get(currentFlashIndex);
        String label;
        if (Camera.Parameters.FLASH_MODE_OFF.equals(mode)) {
            label = "OFF";
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(mode)) {
            label = "AUTO";
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(mode)) {
            label = "ON";
        } else if (Camera.Parameters.FLASH_MODE_TORCH.equals(mode)) {
            label = "TRCH";
        } else if (Camera.Parameters.FLASH_MODE_RED_EYE.equals(mode)) {
            label = "REYE";
        } else {
            label = mode.substring(0, Math.min(4, mode.length())).toUpperCase();
        }
        flashBtn.setText(label);
    }

    private void updateFocusButton() {
        if (supportedFocusModes.isEmpty()) return;
        String mode = supportedFocusModes.get(currentFocusIndex);
        String label;
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(mode)) {
            label = "AF";
        } else if (Camera.Parameters.FOCUS_MODE_INFINITY.equals(mode)) {
            label = "INF";
        } else if (Camera.Parameters.FOCUS_MODE_MACRO.equals(mode)) {
            label = "MAC";
        } else if (Camera.Parameters.FOCUS_MODE_FIXED.equals(mode)) {
            label = "FIX";
        } else {
            label = mode.substring(0, Math.min(3, mode.length())).toUpperCase();
        }
        focusBtn.setText(label);
    }

    private void updatePicSizeButton() {
        if (pictureSizes == null || pictureSizes.length == 0) return;
        String size = pictureSizes[currentPicSizeIndex];
        String[] parts = size.split("x");
        long pixels = Long.parseLong(parts[0]) * Long.parseLong(parts[1]);
        int mpTenths = (int) (pixels / 100000);
        picSizeBtn.setText(mpTenths / 10 + "." + mpTenths % 10 + "M");
    }

    private void updateSceneModeButton() {
        if (sceneModes == null || sceneModes.length == 0) return;
        String mode = sceneModes[currentSceneModeIndex];
        String label;
        if ("auto".equals(mode)) {
            label = "AUTO";
        } else if ("night".equals(mode)) {
            label = "NGHT";
        } else if ("portrait".equals(mode)) {
            label = "PORT";
        } else if ("landscape".equals(mode)) {
            label = "LAND";
        } else if ("sports".equals(mode) || "action".equals(mode)) {
            label = "SPRT";
        } else if ("party".equals(mode)) {
            label = "PRTY";
        } else if ("sunset".equals(mode)) {
            label = "SNST";
        } else {
            label = mode.substring(0, Math.min(4, mode.length())).toUpperCase();
        }
        sceneModeBtn.setText(label);
    }

    private void takePicture() {
        if (camera == null || isTakingPicture || previewOnly) return;
        isTakingPicture = true;

        if (hardwareFocusDone) {
            hardwareFocusDone = false;
            captureImage(camera);
            return;
        }

        if (isFocusing) {
            captureAfterFocus = true;
            return;
        }

        String focusMode = camera.getParameters().getFocusMode();
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(focusMode)
                || Camera.Parameters.FOCUS_MODE_MACRO.equals(focusMode)) {
            isFocusing = true;
            showFocusing();
            camera.autoFocus(new Camera.AutoFocusCallback() {
                public void onAutoFocus(boolean success, Camera cam) {
                    isFocusing = false;
                    showFocusResult(success);
                    reapplyTorch();
                    captureImage(cam);
                }
            });
        } else {
            captureImage(camera);
        }
    }

    private void captureImage(final Camera cam) {
        try {
            Camera.Parameters params = cam.getParameters();
            int rotation = (90 + deviceOrientation) % 360;
            params.setRotation(rotation);
            if (lastLocation != null) {
                params.setGpsLatitude(lastLocation.getLatitude());
                params.setGpsLongitude(lastLocation.getLongitude());
                params.setGpsAltitude(lastLocation.getAltitude());
                params.setGpsTimestamp(lastLocation.getTime() / 1000);
            } else {
                params.removeGpsData();
            }
            cam.setParameters(params);

            playShutterSound();
            cam.takePicture(null, null, new Camera.PictureCallback() {
                public void onPictureTaken(final byte[] data, Camera c) {
                    isTakingPicture = false;
                    try {
                        c.startPreview();
                    } catch (Exception e) {
                        restartPreview(c);
                    }

                    new Thread(new Runnable() {
                        public void run() {
                            savePhotoInBackground(data);
                        }
                    }).start();
                }
            });
        } catch (Exception e) {
            isTakingPicture = false;
            Toast.makeText(CameraActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void restartPreview(Camera cam) {
        if (cam == null) return;
        try {
            cam.stopPreview();
        } catch (Exception e) {
            // Already stopped
        }
        try {
            if (preview != null) {
                cam.setPreviewDisplay(preview.getHolder());
            }
            cam.startPreview();
        } catch (Exception e) {
            android.util.Log.e("CameraX10", "Failed to restart preview", e);
        }
    }

    private void savePhotoInBackground(final byte[] data) {
        final File file = getOutputFile();
        boolean saved = false;

        if (file != null) {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
                saved = true;
            } catch (IOException e) {
                // Failed
            }
        }

        Bitmap thumb = null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 8;
            thumb = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (Exception e) {
            // Decode failed
        }

        showSaveResult(saved, file, thumb);
    }

    private void showSaveResult(final boolean saved, final File file, final Bitmap thumb) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (saved && file != null) {
                    lastMediaFile = file;
                    lastMediaIsVideo = false;
                    sendBroadcast(new Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                    Toast.makeText(CameraActivity.this,
                            "Saved: " + file.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CameraActivity.this,
                            "Error saving photo", Toast.LENGTH_SHORT).show();
                }
                if (thumb != null) {
                    thumbnailView.setImageBitmap(thumb);
                    thumbnailView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File getOutputFile() {
        File dir = new File(Environment.getExternalStorageDirectory(), "DCIM");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(dir, "IMG_" + timestamp + ".jpg");
    }

    // --- Video recording ---

    private void toggleMode() {
        if (isRecording) return;
        isVideoMode = !isVideoMode;
        modeToggleBtn.setText(isVideoMode ? "VID" : "CAM");
        updateShutterButton();
        picSizeBtn.setVisibility(isVideoMode ? View.GONE : View.VISIBLE);
        sceneModeBtn.setVisibility(isVideoMode ? View.GONE : View.VISIBLE);
        boolean hasVideoSizes = videoSizes != null && videoSizes.length > 0;
        videoSizeBtn.setVisibility(isVideoMode && hasVideoSizes ? View.VISIBLE : View.GONE);
        gpsIndicator.setVisibility(isVideoMode ? View.GONE : View.VISIBLE);
    }

    private void updateShutterButton() {
        shutterBtn.setBackgroundDrawable(
                isVideoMode ? makeVideoShutterDrawable() : makeShutterDrawable());
    }

    private Drawable makeVideoShutterDrawable() {
        StateListDrawable states = new StateListDrawable();
        if (isRecording) {
            GradientDrawable pressed = new GradientDrawable();
            pressed.setShape(GradientDrawable.RECTANGLE);
            pressed.setColor(0xFFAAAAAA);
            pressed.setCornerRadius(dp(4));
            GradientDrawable normal = new GradientDrawable();
            normal.setShape(GradientDrawable.RECTANGLE);
            normal.setColor(0xFFCCCCCC);
            normal.setCornerRadius(dp(4));
            states.addState(new int[]{android.R.attr.state_pressed}, pressed);
            states.addState(new int[]{}, normal);
        } else {
            GradientDrawable pressed = new GradientDrawable();
            pressed.setShape(GradientDrawable.OVAL);
            pressed.setColor(0xFFCC0000);
            pressed.setStroke(dp(3), 0xFF880000);
            GradientDrawable normal = new GradientDrawable();
            normal.setShape(GradientDrawable.OVAL);
            normal.setColor(0xFFFF0000);
            normal.setStroke(dp(3), 0xFFCC0000);
            states.addState(new int[]{android.R.attr.state_pressed}, pressed);
            states.addState(new int[]{}, normal);
        }
        return states;
    }

    private void startRecording() {
        if (camera == null || isRecording || previewOnly) return;

        int selW = 320, selH = 240;
        if (videoSizes != null && videoSizes.length > 0) {
            String[] parts = videoSizes[currentVideoSizeIndex].split("x");
            selW = Integer.parseInt(parts[0]);
            selH = Integer.parseInt(parts[1]);
        }
        int[][] fallbacks = {
                {selW, selH, 30},
                {320, 240, 30},
                {176, 144, 30}
        };
        for (int[] s : fallbacks) {
            if (tryStartRecording(s[0], s[1], s[2])) return;
        }
        Toast.makeText(this, "Video recording not supported", Toast.LENGTH_LONG).show();
    }

    private boolean tryStartRecording(int width, int height, int fps) {
        currentVideoFile = getVideoOutputFile();
        if (currentVideoFile == null) return false;

        mediaRecorder = new MediaRecorder();
        try {
            camera.unlock();
        } catch (Exception e) {
            return false;
        }

        try {
            mediaRecorder.setCamera(camera);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
            mediaRecorder.setVideoSize(width, height);
            mediaRecorder.setVideoFrameRate(fps);
            mediaRecorder.setOutputFile(currentVideoFile.getAbsolutePath());
            mediaRecorder.setPreviewDisplay(preview.getHolder().getSurface());
            mediaRecorder.prepare();
            mediaRecorder.start();
            playVideoStartSound();

            isRecording = true;
            updateShutterButton();
            showRecordingIndicator(true);
            setButtonsEnabled(false);
            return true;
        } catch (Exception e) {
            android.util.Log.e("CameraX10", "Recording failed at " + width + "x" + height, e);
            releaseMediaRecorder();
            try { camera.lock(); } catch (Exception ex) { }
            if (currentVideoFile.exists()) currentVideoFile.delete();
            return false;
        }
    }

    private void stopRecording() {
        if (!isRecording || mediaRecorder == null) return;

        try {
            mediaRecorder.stop();
        } catch (Exception e) {
            android.util.Log.e("CameraX10", "Stop recording failed", e);
        }

        playVideoStopSound();
        releaseMediaRecorder();
        isRecording = false;
        updateShutterButton();
        showRecordingIndicator(false);
        setButtonsEnabled(true);

        try {
            camera.lock();
            camera.startPreview();
        } catch (Exception e) {
            android.util.Log.e("CameraX10", "Failed to restart preview after recording", e);
        }

        if (currentVideoFile != null && currentVideoFile.exists() && currentVideoFile.length() > 0) {
            lastMediaFile = currentVideoFile;
            lastMediaIsVideo = true;
            sendBroadcast(new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(currentVideoFile)));
            Toast.makeText(this, "Saved: " + currentVideoFile.getName(), Toast.LENGTH_SHORT).show();
            thumbnailView.setImageBitmap(makeVideoThumbnail());
            thumbnailView.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try { mediaRecorder.reset(); } catch (Exception e) { }
            try { mediaRecorder.release(); } catch (Exception e) { }
            mediaRecorder = null;
        }
    }

    private final Runnable blinkRunnable = new Runnable() {
        public void run() {
            if (isRecording && recordingIndicator != null) {
                recordingIndicator.setVisibility(
                        recordingIndicator.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                blinkHandler.postDelayed(this, 500);
            }
        }
    };

    private void showRecordingIndicator(boolean show) {
        blinkHandler.removeCallbacks(blinkRunnable);
        if (show) {
            recordingIndicator.setVisibility(View.VISIBLE);
            blinkHandler.postDelayed(blinkRunnable, 500);
        } else {
            recordingIndicator.setVisibility(View.GONE);
        }
    }

    private Bitmap makeVideoThumbnail() {
        int w = dp(56);
        int h = dp(56);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(0xFF222222);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(0xFFFFFFFF);
        paint.setAntiAlias(true);
        android.graphics.Path triangle = new android.graphics.Path();
        float cx = w / 2f;
        float cy = h / 2f;
        float size = h * 0.35f;
        triangle.moveTo(cx - size * 0.5f, cy - size);
        triangle.lineTo(cx - size * 0.5f, cy + size);
        triangle.lineTo(cx + size, cy);
        triangle.close();
        c.drawPath(triangle, paint);
        return bmp;
    }

    private void setButtonsEnabled(boolean enabled) {
        flashBtn.setEnabled(enabled);
        focusBtn.setEnabled(enabled);
        settingsBtn.setEnabled(enabled);
        modeToggleBtn.setEnabled(enabled);
    }

    private void cycleVideoSize() {
        if (isRecording || videoSizes == null || videoSizes.length == 0) return;
        currentVideoSizeIndex = (currentVideoSizeIndex + 1) % videoSizes.length;
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("video_size", videoSizes[currentVideoSizeIndex]).commit();
        updateVideoSizeButton();
    }

    private void updateVideoSizeButton() {
        if (videoSizes == null || videoSizes.length == 0) return;
        String size = videoSizes[currentVideoSizeIndex];
        String[] parts = size.split("x");
        videoSizeBtn.setText(parts[0] + "x" + parts[1]);
    }

    private void restoreVideoSize() {
        if (videoSizes == null || videoSizes.length == 0) {
            videoSizeBtn.setVisibility(View.GONE);
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String saved = prefs.getString("video_size", videoSizes[0]);
        currentVideoSizeIndex = 0;
        for (int i = 0; i < videoSizes.length; i++) {
            if (videoSizes[i].equals(saved)) {
                currentVideoSizeIndex = i;
                break;
            }
        }
        updateVideoSizeButton();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File getVideoOutputFile() {
        File dir = new File(Environment.getExternalStorageDirectory(), "DCIM");
        if (!dir.exists() && !dir.mkdirs()) return null;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(dir, "VID_" + timestamp + ".3gp");
    }

    private void initSounds() {
        soundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

        String soundSource = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("camera_sound", "app");
        boolean useAppSounds = "app".equals(soundSource);

        if (useAppSounds) {
            File cacheDir = getCacheDir();

            File f = new File(cacheDir, "shutter.wav");
            if (!f.exists()) generateShutterWav(f);
            if (f.exists()) shutterSoundId = soundPool.load(f.getAbsolutePath(), 1);

            f = new File(cacheDir, "focus_beep.wav");
            if (!f.exists()) generateFocusWav(f);
            if (f.exists()) focusSoundId = soundPool.load(f.getAbsolutePath(), 1);

            f = new File(cacheDir, "rec_start.wav");
            if (!f.exists()) generateVideoStartWav(f);
            if (f.exists()) videoStartSoundId = soundPool.load(f.getAbsolutePath(), 1);

            f = new File(cacheDir, "rec_stop.wav");
            if (!f.exists()) generateVideoStopWav(f);
            if (f.exists()) videoStopSoundId = soundPool.load(f.getAbsolutePath(), 1);
        }

        if (shutterSoundId < 0) shutterSoundId = loadFirstExisting(new String[]{
                "/system/media/audio/ui/camera_click.ogg",
                "/system/media/audio/ui/camera_shutter.ogg",
                "/system/media/audio/ui/shutter.ogg"});

        if (focusSoundId < 0) focusSoundId = loadFirstExisting(new String[]{
                "/system/media/audio/ui/camera_focus.ogg",
                "/system/media/audio/ui/auto_focus.ogg",
                "/system/media/audio/ui/Focus.ogg"});

        if (videoStartSoundId < 0) videoStartSoundId = loadFirstExisting(new String[]{
                "/system/media/audio/ui/VideoRecord.ogg",
                "/system/media/audio/ui/VideoStart.ogg",
                "/system/media/audio/ui/video_record.ogg"});

        if (videoStopSoundId < 0) videoStopSoundId = loadFirstExisting(new String[]{
                "/system/media/audio/ui/VideoStop.ogg",
                "/system/media/audio/ui/video_stop.ogg"});

        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        } catch (Exception e) {
            // Ignore
        }
    }

    private int loadFirstExisting(String[] paths) {
        for (String path : paths) {
            if (new File(path).exists()) {
                return soundPool.load(path, 1);
            }
        }
        return -1;
    }

    private void releaseSounds() {
        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        } catch (Exception e) {
            // Ignore
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        shutterSoundId = -1;
        focusSoundId = -1;
        videoStartSoundId = -1;
        videoStopSoundId = -1;
    }

    private void playShutterSound() {
        if (soundPool != null && shutterSoundId > 0) {
            soundPool.play(shutterSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    private void playFocusSound() {
        if (soundPool != null && focusSoundId > 0) {
            soundPool.play(focusSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    private void playVideoStartSound() {
        if (soundPool != null && videoStartSoundId > 0) {
            soundPool.play(videoStartSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    private void playVideoStopSound() {
        if (soundPool != null && videoStopSoundId > 0) {
            soundPool.play(videoStopSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    // --- Synthetic Sony Alpha-style sound generation ---

    private static final int SOUND_SAMPLE_RATE = 16000;

    private short[] makeTone(int freq, int durationMs, boolean decay) {
        int samples = SOUND_SAMPLE_RATE * durationMs / 1000;
        short[] pcm = new short[samples];
        int fade = Math.min(40, samples / 4);
        for (int i = 0; i < samples; i++) {
            double angle = 2.0 * Math.PI * freq * i / SOUND_SAMPLE_RATE;
            double env = 1.0;
            if (decay) env = Math.exp(-4.0 * i / samples);
            if (i < fade) env *= (double) i / fade;
            else if (i > samples - fade) env *= (double) (samples - i) / fade;
            pcm[i] = (short) (Math.sin(angle) * 24000 * env);
        }
        return pcm;
    }

    private short[] joinPcm(short[][] parts) {
        int total = 0;
        for (short[] p : parts) total += p.length;
        short[] out = new short[total];
        int pos = 0;
        for (short[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    private void writeWav(File output, short[] pcm) {
        try {
            int dataSize = pcm.length * 2;
            byte[] wav = new byte[44 + dataSize];
            wav[0] = 'R'; wav[1] = 'I'; wav[2] = 'F'; wav[3] = 'F';
            wavPutInt(wav, 4, 36 + dataSize);
            wav[8] = 'W'; wav[9] = 'A'; wav[10] = 'V'; wav[11] = 'E';
            wav[12] = 'f'; wav[13] = 'm'; wav[14] = 't'; wav[15] = ' ';
            wavPutInt(wav, 16, 16);
            wavPutShort(wav, 20, (short) 1);
            wavPutShort(wav, 22, (short) 1);
            wavPutInt(wav, 24, SOUND_SAMPLE_RATE);
            wavPutInt(wav, 28, SOUND_SAMPLE_RATE * 2);
            wavPutShort(wav, 32, (short) 2);
            wavPutShort(wav, 34, (short) 16);
            wav[36] = 'd'; wav[37] = 'a'; wav[38] = 't'; wav[39] = 'a';
            wavPutInt(wav, 40, dataSize);
            for (int i = 0; i < pcm.length; i++) {
                wav[44 + i * 2] = (byte) (pcm[i] & 0xFF);
                wav[44 + i * 2 + 1] = (byte) ((pcm[i] >> 8) & 0xFF);
            }
            FileOutputStream fos = new FileOutputStream(output);
            fos.write(wav);
            fos.close();
        } catch (Exception e) {
            // Failed
        }
    }

    private void wavPutInt(byte[] b, int p, int v) {
        b[p] = (byte) (v & 0xFF);
        b[p + 1] = (byte) ((v >> 8) & 0xFF);
        b[p + 2] = (byte) ((v >> 16) & 0xFF);
        b[p + 3] = (byte) ((v >> 24) & 0xFF);
    }

    private void wavPutShort(byte[] b, int p, short v) {
        b[p] = (byte) (v & 0xFF);
        b[p + 1] = (byte) ((v >> 8) & 0xFF);
    }

    private void generateShutterWav(File output) {
        // Single short electronic click with fast decay
        writeWav(output, makeTone(2500, 100, true));
    }

    private void generateFocusWav(File output) {
        // Double-beep: two identical short tones with a gap
        short[] beep1 = makeTone(2700, 60, false);
        short[] gap = new short[SOUND_SAMPLE_RATE * 40 / 1000];
        short[] beep2 = makeTone(2700, 60, false);
        writeWav(output, joinPcm(new short[][]{beep1, gap, beep2}));
    }

    private void generateVideoStartWav(File output) {
        // Ascending two-note chime
        short[] note1 = makeTone(2000, 100, false);
        short[] gap = new short[SOUND_SAMPLE_RATE * 30 / 1000];
        short[] note2 = makeTone(2700, 120, false);
        writeWav(output, joinPcm(new short[][]{note1, gap, note2}));
    }

    private void generateVideoStopWav(File output) {
        // Descending two-note chime
        short[] note1 = makeTone(2700, 100, false);
        short[] gap = new short[SOUND_SAMPLE_RATE * 30 / 1000];
        short[] note2 = makeTone(2000, 120, false);
        writeWav(output, joinPcm(new short[][]{note1, gap, note2}));
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        if (whiteBalances != null) intent.putExtra("white_balances", whiteBalances);
        if (colorEffects != null) intent.putExtra("color_effects", colorEffects);
        startActivity(intent);
    }
}
