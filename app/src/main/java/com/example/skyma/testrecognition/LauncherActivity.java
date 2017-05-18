package com.example.skyma.testrecognition;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.skyma.testrecognition.AzureUpload.ImageManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class LauncherActivity extends AppCompatActivity {

    Button buttonGallery, buttonCamera, buttonUpload, buttonOCR;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_IMAGE_REQUEST = 2;
    ImageView imagePreview;

    Uri cameraCapturedUri = null;

    public String uploadedImageName;
    public Uri uri = null;
    String fileUri;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        buttonGallery = (Button) findViewById(R.id.galleryButton);
        buttonCamera = (Button) findViewById(R.id.cameraButton);
        buttonUpload = (Button) findViewById(R.id.uploadButton);
        buttonOCR = (Button) findViewById(R.id.ocrButton);

        imagePreview = (ImageView) findViewById(R.id.imageChosen);

        buttonGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });

        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                verifyStoragePermissions(LauncherActivity.this);

                File file = new File(Environment.getExternalStorageDirectory(), getFileName());
                if(!file.exists()){
                    try{
                        file.createNewFile();
                    } catch(IOException e){
                        Toast.makeText(LauncherActivity.this, "Exception while creating file of type : " + e.toString(),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                uri = Uri.fromFile(file);
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(cameraIntent, CAMERA_IMAGE_REQUEST);
            }
        });

        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UploadDocument();
            }
        });

        buttonOCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent ocrIntent = new Intent(LauncherActivity.this, MainActivity.class);
                ocrIntent.putExtra("FILE_URI", fileUri);
                startActivity(ocrIntent);
            }
        });
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    // Image chosen from the gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // If image was taken from the camera
        if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            /*Bitmap photo = (Bitmap) data.getExtras().get("data");
            imagePreview.setImageBitmap(photo);*/

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), uri);
                Toast.makeText(LauncherActivity.this, "Trying to set the preview here", Toast.LENGTH_SHORT).show();
                imagePreview.setImageBitmap(bitmap);
            } catch(IOException e){
                Toast.makeText(LauncherActivity.this, "Exception while saving bitmap file of type : " +
                        e.toString(), Toast.LENGTH_SHORT).show();
            }

            // enable the upload button
            buttonUpload.setEnabled(true);
        }

        // If image was chosen from the gallery
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imagePreview.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // enable the upload button
            buttonUpload.setEnabled(true);
        }
    }

    // Method to generate a file name
    public String getFileName(){
        StringBuilder sb = new StringBuilder();
        String randomChars = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        for(int i=0; i<10; i++){
            sb.append(randomChars.charAt(random.nextInt(randomChars.length())));
        }
        return sb.toString();
    }

    // Method to upload documents to Azure Storage
    // Method uploads documents
    private void UploadDocument(){
        try {
            Toast.makeText(LauncherActivity.this, "The image path is : " + uri.getPath(), Toast.LENGTH_SHORT).show();
            final InputStream imageStream = getContentResolver().openInputStream(uri);
            final int imageLength = imageStream.available();
            final Handler handler = new Handler();

            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Calls UploadImage method of ImageManager class
                    try {
                        uploadedImageName = ImageManager.UploadImage(imageStream, imageLength, getFileName() + ".jpg");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LauncherActivity.this, "Image Upload Successfully. Name = " + uploadedImageName, Toast.LENGTH_SHORT).show();
                                // Enable the OCR Button
                                buttonOCR.setEnabled(true);
                            }
                        });
                    } catch (Exception e){
                        final String exceptionMessage = e.getMessage();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(LauncherActivity.this, exceptionMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });

            th.start();

            fileUri = "https://mlxstorage.blob.core.windows.net/second-opinion/" + uploadedImageName;

        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
