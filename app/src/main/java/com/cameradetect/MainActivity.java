package com.cameradetect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView imageView;
    Button process;
    TextView alpha_view, result_view, message;
    EditText result, alpha;
    Bitmap bitmap;
    View parentLayout;

    private int RESULT_LOAD_IMAGE = 2;
    final int CAMERA_PIC_REQUEST = 1;
    final int CROP_PIC = 6;
    final int CONTACT = 5;

    ProgressDialog p;
    Uri selectImage;
    private String pictureImagePath = "";

    String num, alphabet;
    private static final String TAG = "MainActivity";

    private final int PERMISSIONS_ALL=1;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        parentLayout = findViewById(android.R.id.content);

        String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_CONTACTS,};

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_ALL);
        }

        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setVisibility(View.GONE);

        process = (Button) findViewById(R.id.process);
        process.setOnClickListener(this);
        process.setVisibility(View.GONE);

        result = (EditText) findViewById(R.id.result);
        alpha = (EditText) findViewById(R.id.alpha);
        alpha_view = (TextView) findViewById(R.id.alpha_view);
        result_view = (TextView) findViewById(R.id.result_view);
        message = (TextView) findViewById(R.id.message);
        result.setVisibility(View.GONE);
        alpha.setVisibility(View.GONE);
        alpha_view.setVisibility(View.GONE);
        result_view.setVisibility(View.GONE);
        this.invalidateOptionsMenu();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        this.invalidateOptionsMenu();
        if (requestCode == CAMERA_PIC_REQUEST) {
            if (resultCode == RESULT_OK) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                imageView.setVisibility(View.VISIBLE);
                process.setVisibility(View.VISIBLE);
                message.setVisibility(View.GONE);
                /*Bundle b = data.getExtras();
                bitmap = b.getParcelable("data");
                imageView.setImageBitmap(bitmap);*/
                File imgFile = new File(pictureImagePath);
                if (imgFile.exists()) {
                    bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                    imageView.setImageBitmap(bitmap);
                    selectImage = getImageUri(MainActivity.this, bitmap);
                }
                //file_path = getRealPathFromURI(selectImage);
            }
        }
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            imageView.setVisibility(View.VISIBLE);
            process.setVisibility(View.VISIBLE);
            message.setVisibility(View.GONE);
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Log.d("File path: ", "Arr" + Arrays.toString(filePathColumn));
            Cursor cursor = getContentResolver().query(data.getData(), filePathColumn, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                Log.d("Col index 1: ", Integer.toString(columnIndex));
                String picturePath = cursor.getString(columnIndex);
                Log.d("Path: ", picturePath);
                cursor.close();
                bitmap = BitmapFactory.decodeFile(picturePath);
                imageView.setImageBitmap(bitmap);
                selectImage = getImageUri(MainActivity.this, bitmap);
                //file_path = getRealPathFromURI(selectImage);
            } else {
                Snackbar.make(parentLayout, "Error selecting image", Snackbar.LENGTH_LONG).show();
            }
        }
        if (requestCode == CROP_PIC && data != null) {
            imageView.setVisibility(View.VISIBLE);
            process.setVisibility(View.VISIBLE);
            message.setVisibility(View.GONE);
            /*imageView.setMinimumHeight(crop_bitmap.getHeight());
            imageView.setMinimumWidth(crop_bitmap.getWidth());*/
            Bundle b = data.getExtras();
            bitmap = b.getParcelable("data");
            if(bitmap!=null) {
                imageView.setImageBitmap(bitmap);
            }
            selectImage = getImageUri(MainActivity.this, bitmap);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void cropImage() {
        try {
            Intent crop = new Intent("com.android.camera.action.CROP");
            crop.setDataAndType(selectImage, "image/*");
            crop.putExtra("crop", true);
            crop.putExtra("outputX", 500);
            crop.putExtra("outputY", 420);
            /*crop.putExtra("aspectX", imageView.getHeight());
            crop.putExtra("aspectY", imageView.getWidth());*/
            /*crop.putExtra("scale", true);
            crop.putExtra("scaleUpIfNeeded",true);*/
            crop.putExtra("return-data", true);
            startActivityForResult(crop, CROP_PIC);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error your phone does not support cropping!", Toast.LENGTH_LONG).show();
        } catch (NullPointerException f) {
            f.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_ALL:
                if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    alertDialog.setTitle("Unable to click a picture!");
                    alertDialog.setMessage("Please allow the app to access your camera");
                    alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent in = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                            startActivity(in);
                        }
                    });
                    return;
                }
                if(grantResults[1] != PackageManager.PERMISSION_GRANTED){
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    alertDialog.setTitle("Unable to access storage");
                    alertDialog.setMessage("Please allow the app to access your storage");
                    alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent in = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                            startActivity(in);
                        }
                    });
                    return;
                }
                if(grantResults[2] != PackageManager.PERMISSION_GRANTED){
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    alertDialog.setTitle("Unable to access storage");
                    alertDialog.setMessage("Please allow the app to access your storage");
                    alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent in = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                            startActivity(in);
                        }
                    });
                    return;
                }
                if(grantResults[3] != PackageManager.PERMISSION_GRANTED){
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    alertDialog.setTitle("Unable to access contacts");
                    alertDialog.setMessage("Please allow the app to access your contacts");
                    alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent in = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                            startActivity(in);
                        }
                    });
                    return;
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.action_bar2, menu);
        if (imageView.getDrawable() == null) {
            menu.findItem(R.id.crop).setVisible(false);
            menu.findItem(R.id.contacts).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
            Log.d("Hiding", "item");
        } else {
            menu.findItem(R.id.crop).setVisible(true);
            menu.findItem(R.id.contacts).setVisible(true);
            menu.findItem(R.id.delete).setVisible(true);
            Log.d("Showing", "item");
        }
        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.upload_image:
                startGal();
                return true;

            case R.id.camera:
                startCam();
                return true;

            case R.id.contacts:
                if (imageView.getDrawable() != null || !result.getText().toString().isEmpty() || !alpha.getText().toString().isEmpty()) {
                    Intent contact = new Intent(ContactsContract.Intents.Insert.ACTION);
                    contact.setType(ContactsContract.RawContacts.CONTENT_TYPE);

                    if (!alpha.getText().toString().equals("No text found")) {
                        contact.putExtra(ContactsContract.Intents.Insert.NAME, alpha.getText().toString());
                    } else if (alpha.getText().toString().equals("No text found")) {
                        Snackbar.make(parentLayout, "No name found! Try clicking/uploading the picture again", Snackbar.LENGTH_LONG).show();
                        return false;
                    }

                    if (!result.getText().toString().equals("No number found")) {
                        contact.putExtra(ContactsContract.Intents.Insert.PHONE, result.getText().toString());
                    } else if (alpha.getText().toString().equals("No text found")) {
                        Snackbar.make(parentLayout, "No number found! Try clicking/uploading the picture again", Snackbar.LENGTH_LONG).show();
                        return false;
                    }

                    startActivityForResult(contact, CONTACT);
                    return true;
                } else if (imageView.getDrawable() == null) {
                    Snackbar.make(parentLayout, "No Image selected!", Snackbar.LENGTH_LONG).show();
                    return false;
                } else if (result.getText().toString().isEmpty() || alpha.getText().toString().isEmpty()) {
                    Snackbar.make(parentLayout, "No information found", Snackbar.LENGTH_LONG).show();
                    return false;
                }

            case R.id.crop:
                cropImage();
                return true;

            case R.id.delete:
                removeImage();
                return true;

            case R.id.livescan:
                liveScan();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void removeImage() {
        AlertDialog.Builder al = new AlertDialog.Builder(MainActivity.this);
        al.setTitle("Remove image");
        al.setMessage("Are you sure you would like to remove this image?");
        al.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (imageView.getDrawable() != null) {
                    imageView.setImageDrawable(null);
                    bitmap = null;
                    imageView.setVisibility(View.GONE);
                    alpha.setVisibility(View.GONE);
                    result.setVisibility(View.GONE);
                    process.setVisibility(View.GONE);
                    alpha_view.setVisibility(View.GONE);
                    result_view.setVisibility(View.GONE);
                    message.setVisibility(View.VISIBLE);
                    MainActivity.this.invalidateOptionsMenu();
                } else if (imageView.getDrawable() == null) {
                    Snackbar.make(parentLayout, "No image selected!", Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(parentLayout, "Random lilz", Snackbar.LENGTH_LONG).show();
                }
            }
        });
        al.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        al.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.process:
                if (imageView.getDrawable() == null) {
                    Snackbar.make(parentLayout, "No image selected!", Snackbar.LENGTH_LONG).show();
                } else {
                    new Process().execute();
                    result.setVisibility(View.VISIBLE);
                    alpha.setVisibility(View.VISIBLE);
                    alpha_view.setVisibility(View.VISIBLE);
                    result_view.setVisibility(View.VISIBLE);
                }
                break;

        }
    }

    private void liveScan(){
        Intent in = new Intent(this, LiveScan.class);
        startActivity(in);
    }

    private void startGal(){
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    private void startCam(){
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + ".jpg";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName;
        File file = new File(pictureImagePath);
        Uri outputFileUri = Uri.fromFile(file);
        Intent camera_intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        startActivityForResult(camera_intent, CAMERA_PIC_REQUEST);
    }

    private class Process extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(MainActivity.this);
            p.setMessage("Processing");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected String doInBackground(String... params) {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
            try {
                if (!textRecognizer.isOperational()) {
                    Log.d(TAG, "Not working");
                } else {
                    try {
                        Frame frame;
                        frame = new Frame.Builder().setBitmap(bitmap).build();
                        //Log.d("Bitmap: ", bitmap.toString());
                        SparseArray<TextBlock> items = textRecognizer.detect(frame);
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int j = 0; j < items.size(); j++) {
                            TextBlock tb = items.valueAt(j);
                            stringBuilder.append(tb.getValue());
                            stringBuilder.append("\n");
                        }
                        p.dismiss();
                        String res = stringBuilder.toString();
                        num = res.replaceAll("\\D+", "");
                        Log.d("Original: ", res);
                        Log.d("Numbers", num);
                        if (num.isEmpty()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String no_num = "No number found";
                                    result.setText(no_num);
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    result.setText(num);
                                }
                            });
                        }
                        String res_a = stringBuilder.toString();
                        alphabet = res_a.replaceAll("[0-9]", "");
                        Log.d("Original1: ", res_a);
                        Log.d("Alpha", alphabet);
                        if (alphabet.isEmpty()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String none = "No text found";
                                    alpha.setText(none);
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    alpha.setText(alphabet);
                                }
                            });
                        }
                    } catch (NullPointerException f) {
                        f.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                p.dismiss();
                                Snackbar.make(parentLayout, "No image selected!", Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        p.dismiss();
                        Snackbar.make(parentLayout, "Error selecting image", Snackbar.LENGTH_LONG).show();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            result.setVisibility(View.VISIBLE);
            alpha.setVisibility(View.VISIBLE);
            alpha_view.setVisibility(View.VISIBLE);
            result_view.setVisibility(View.VISIBLE);
            p.dismiss();
        }
    }
}
