package ai.deepar.photo_edit_example;

import ai.deepar.ar.AREventListener;
import ai.deepar.ar.DeepAR;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;

import static ai.deepar.photo_edit_example.LoadImageHandlerThread.LOAD_DEFAULT_IMAGE_TASK;
import static ai.deepar.photo_edit_example.LoadImageHandlerThread.LOAD_IMAGE_FROM_GALLERY_TASK;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, AREventListener {

    private DeepAR deepAR;

    private int currentMask=0;
    private int currentEffect=0;
    private int currentFilter=0;

    private int RESULT_LOAD_IMG = 123;

    private LoadImageHandlerThread handlerThread;

    ArrayList<String> masks;
    ArrayList<String> effects;
    ArrayList<String> filters;

    private int activeFilterType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO },
                    1);
        } else {
            // Permission has already been granted
            initialize();
        }

        handlerThread = new LoadImageHandlerThread(this);
        handlerThread.start();

    }



    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return; // no permission
                }
                initialize();
            }
        }
    }

    private void initialize() {
        setContentView(R.layout.activity_main);
        initializeDeepAR();
        initializeFilters();
        initalizeViews();
    }

    private void initializeFilters() {
        masks = new ArrayList<>();
        masks.add("none");
        masks.add("aviators");
        masks.add("bigmouth");
        masks.add("dalmatian");
        masks.add("flowers");
        masks.add("koala");
        masks.add("lion");
        masks.add("smallface");
        masks.add("teddycigar");
        masks.add("kanye");
        masks.add("tripleface");
        masks.add("sleepingmask");
        masks.add("fatify");
        masks.add("obama");
        masks.add("mudmask");
        masks.add("pug");
        masks.add("slash");
        masks.add("twistedface");
        masks.add("grumpycat");

        effects = new ArrayList<>();
        effects.add("none");
        effects.add("fire");
        effects.add("rain");
        effects.add("heart");
        effects.add("blizzard");

        filters = new ArrayList<>();
        filters.add("none");
        filters.add("filmcolorperfection");
        filters.add("tv80");
        filters.add("drawingmanga");
        filters.add("sepia");
        filters.add("bleachbypass");
    }

    private void initalizeViews() {
        ImageButton previousMask = findViewById(R.id.previousMask);
        previousMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        ImageButton nextMask = findViewById(R.id.nextMask);
        nextMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        final RadioButton radioMasks = findViewById(R.id.masks);
        final RadioButton radioEffects = findViewById(R.id.effects);
        final RadioButton radioFilters = findViewById(R.id.filters);

        SurfaceView arView = findViewById(R.id.surface);

        arView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deepAR.onClick();
            }
        });

        arView.getHolder().addCallback(this);

        // Surface might already be initialized, so we force the call to onSurfaceChanged
        arView.setVisibility(View.GONE);
        arView.setVisibility(View.VISIBLE);

        ImageButton screenshotBtn = findViewById(R.id.recordButton);
        screenshotBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deepAR.takeScreenshot();
            }
        });



        ImageButton loadFromGallery = findViewById(R.id.loadFromGallery);
        loadFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
            }
        });

        previousMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoPrevious();
            }
        });

        nextMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoNext();
            }
        });

        radioMasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                radioEffects.setChecked(false);
                radioFilters.setChecked(false);
                activeFilterType = 0;
            }
        });
        radioEffects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                radioMasks.setChecked(false);
                radioFilters.setChecked(false);
                activeFilterType = 1;
            }
        });
        radioFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                radioEffects.setChecked(false);
                radioMasks.setChecked(false);
                activeFilterType = 2;
            }
        });
    }


    private void initializeDeepAR() {
        deepAR = new DeepAR(this);
        deepAR.setLicenseKey("your_license_key_goes_here");
        deepAR.initialize(this, this);

    }

    private String getFilterPath(String filterName) {
        if (filterName.equals("none")) {
            return null;
        }
        return "file:///android_asset/" + filterName;
    }

    private void gotoNext() {
        if (activeFilterType == 0) {
            currentMask = (currentMask + 1) % masks.size();
            deepAR.switchEffect("mask", getFilterPath(masks.get(currentMask)));
        } else if (activeFilterType == 1) {
            currentEffect = (currentEffect + 1) % effects.size();
            deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
        } else if (activeFilterType == 2) {
            currentFilter = (currentFilter + 1) % filters.size();
            deepAR.switchEffect("filter", getFilterPath(filters.get(currentFilter)));
        }
    }

    private void gotoPrevious() {
        if (activeFilterType == 0) {
            currentMask = (currentMask - 1) % masks.size();
            deepAR.switchEffect("mask", getFilterPath(masks.get(currentMask)));
        } else if (activeFilterType == 1) {
            currentEffect = (currentEffect - 1) % effects.size();
            deepAR.switchEffect("effect", getFilterPath(effects.get(currentEffect)));
        } else if (activeFilterType == 2) {
            currentFilter = (currentFilter - 1) % filters.size();
            deepAR.switchEffect("filter", getFilterPath(filters.get(currentFilter)));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deepAR == null) {
            return;
        }
        deepAR.setAREventListener(null);
        deepAR.release();
        deepAR = null;
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode == RESULT_OK) {
                // Load selected image
                final Uri imageUri = data.getData();
                Message msg = Message.obtain(handlerThread.getHandler());
                msg.what = LOAD_IMAGE_FROM_GALLERY_TASK;
                msg.obj = imageUri;
                msg.sendToTarget();

        } else {
            Toast.makeText(this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("MainActivity", "Surface created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        deepAR.setRenderSurface(holder.getSurface(), width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (deepAR != null) {
            deepAR.setRenderSurface(null, 0, 0);
        }
    }

    @Override
    public void screenshotTaken(Bitmap bitmap) {
        CharSequence now = DateFormat.format("yyyy_MM_dd_hh_mm_ss", new Date());
        try {
            File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/DeepAR_" + now + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
            MediaScannerConnection.scanFile(MainActivity.this, new String[]{imageFile.toString()}, null, null);
            Toast.makeText(MainActivity.this, "Screenshot saved", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @Override
    public void videoRecordingStarted() {

    }

    @Override
    public void videoRecordingFinished() {

    }

    @Override
    public void videoRecordingFailed() {

    }

    @Override
    public void videoRecordingPrepared() {

    }

    @Override
    public void shutdownFinished() {

    }

    @Override
    public void initialized() {

        if (handlerThread != null && deepAR != null) {
            handlerThread.setImageReceiver(deepAR);

            // Load default image
            Message msg = Message.obtain(handlerThread.getHandler());
            msg.what = LOAD_DEFAULT_IMAGE_TASK;
            msg.sendToTarget();
        }
    }

    @Override
    public void faceVisibilityChanged(boolean b) {

    }

    @Override
    public void imageVisibilityChanged(String s, boolean b) {

    }

    @Override
    public void error(String s) {

    }

    @Override
    public void effectSwitched(String s) {

    }


}
