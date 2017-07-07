package com.cameradetect;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView imageView;
    Button process;
    EditText result;
    Bitmap bitmap;
    View parentLayout;
    private int RESULT_LOAD_IMAGE = 2;
    final int REQUEST_STORAGE_PERM = 1001;
    ProgressDialog p;
    //Camera camera;

    private static final String TAG = "MainActivity";

    final int REQUEST_CAM_PERM = 1002;
    final int CAMERA_PIC_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        parentLayout = findViewById(android.R.id.content);

        imageView = (ImageView) findViewById(R.id.imageView);

        process = (Button) findViewById(R.id.process);
        process.setOnClickListener(this);

        result = (EditText) findViewById(R.id.result);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CAMERA_PIC_REQUEST){
            if(resultCode == RESULT_OK && data != null){
                bitmap = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(bitmap);
            }
        }
        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null){
            Uri selectImage = data.getData();
            Log.d("Image: ", selectImage.toString());
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Log.d("File path: ", "Arr" + Arrays.toString(filePathColumn));
            Cursor cursor = getContentResolver().query(selectImage, filePathColumn, null, null, null);
            if(cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                Log.d("Col index 1: ", Integer.toString(columnIndex));
                String picturePath = cursor.getString(columnIndex);
                Log.d("Path: ", picturePath);
                cursor.close();
                bitmap = BitmapFactory.decodeFile(picturePath);
                imageView.setImageBitmap(bitmap);
            }
            else {
                Snackbar.make(parentLayout, "Error selecting image", Snackbar.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case REQUEST_CAM_PERM:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                break;

            case REQUEST_STORAGE_PERM:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        return;
                    }
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.action_bar2, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.upload_image:
                if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERM);
                    return true;
                }
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
                return true;

            case R.id.camera:
                if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAM_PERM);
                    return true;
                }
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(camera_intent, CAMERA_PIC_REQUEST);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.process:
                String temp = "";
                result.setText(temp);
                if(imageView.getDrawable() == null){
                    Snackbar.make(parentLayout, "No image selected!", Snackbar.LENGTH_LONG).show();
                }
                else {
                    TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                    try {
                        if (!textRecognizer.isOperational()) {
                            Log.d(TAG, "Not working");
                        } else {
                            try {
                                p = new ProgressDialog(MainActivity.this);
                                p.setMessage("Processing");
                                p.setIndeterminate(false);
                                p.setCancelable(false);
                                p.show();
                                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                                SparseArray<TextBlock> items = textRecognizer.detect(frame);
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int j = 0; j < items.size(); j++) {
                                    TextBlock tb = items.valueAt(j);
                                    stringBuilder.append(tb.getValue());
                                    stringBuilder.append("\n");
                                }
                                p.dismiss();
                                if (stringBuilder.toString().isEmpty()) {
                                    String none = "No text found";
                                    result.setText(none);
                                } else {
                                    result.setText(stringBuilder.toString());
                                }
                            } catch (NullPointerException f) {
                                f.printStackTrace();
                                Snackbar.make(parentLayout, "No image selected!", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        Snackbar.make(parentLayout, "Error selecting image", Snackbar.LENGTH_LONG).show();
                    }
                }
                break;

        }
    }
}
