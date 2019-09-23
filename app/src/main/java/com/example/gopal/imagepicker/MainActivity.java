package com.example.gopal.imagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final int CAMERA_STORAGE_REQUEST_CODE = 611;
    public static final int ONLY_CAMERA_REQUEST_CODE = 612;
    public static final int ONLY_STORAGE_REQUEST_CODE = 613;
    // Make sure it should have same value as in Manifest's  provider's authorities value
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.gopal.imagepicker";
    private String currentPhotoPath = "";
    private Uri photoURI;
    private ImageView capturedImage;
    private Button openCameraButton;
    private Button uploadButton;
    private String imageDataAsBase64 = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        openCameraButton = findViewById(R.id.open_camera_button);
        uploadButton = findViewById(R.id.upload_picture_button);
        capturedImage = findViewById(R.id.captured_image);

        openCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AskPermissionOrLaunchCamera();
            }
        });
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imageDataAsBase64 == null){
                    Toast.makeText(MainActivity.this, "You haven't clicked a photo yet!", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this, " Refer log to see the value", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onClick: " + imageDataAsBase64 );
                }
            }
        });
    }

    public void AskPermissionOrLaunchCamera() {
        // Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_STORAGE_REQUEST_CODE);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, ONLY_CAMERA_REQUEST_CODE);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, ONLY_STORAGE_REQUEST_CODE);
        } else {
            // Launch the camera if the permission exists
            launchCamera();
        }
    }

    // Returns the result of request permission that we have asked/requested for external write storage when camera launches
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case CAMERA_STORAGE_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                    launchCamera();
                else if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "App needs Storage access in order to store your profile picture.", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (grantResults[0] == PackageManager.PERMISSION_DENIED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "App needs Camera access in order to take  picture.", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (grantResults[0] == PackageManager.PERMISSION_DENIED && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "App needs Camera and Storage access in order to take  picture.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case ONLY_CAMERA_REQUEST_CODE:

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    launchCamera();
                else {
                    Toast.makeText(this, "App needs Camera access in order to take  picture.", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case ONLY_STORAGE_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    launchCamera();
                else {
                    Toast.makeText(this, "App needs Storage access in order to store your  picture.", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void launchCamera() {

        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File or Directory where the photo should go
            File photoFile = null;
            try {
                photoFile = createTempImageFile(this);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                currentPhotoPath = "file:" + photoFile.getAbsolutePath();

                // Get the content URI for the image file
                photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                if (photoURI != null)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            /*
             * This can't be used, For more info refer: https://developer.android.com/training/camera/photobasics.html,
             * Save The full Size Photo -> 2nd Note section
             */
            // Uri uri = data.getData();

           /*
           // This is the simplest one, to convert Uri to Bitmap & display it
           try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(currentPhotoPath));
                capturedImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }*/

           // Finally, for your project
            CompressAndShowImage(Uri.parse(currentPhotoPath));
        }
    }

    public  File createTempImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
       // File storageDir = context.getExternalCacheDir();

        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

    }

    public  void CompressAndShowImage(Uri  imageUri) {

        if (imageUri != null) {
            String encodedImage = null;

            try {
                //1 Encoding the image
                encodedImage = setEncodeImage(imageUri);
                imageDataAsBase64 = encodedImage;
            } catch (Exception e) {
                e.printStackTrace();
            }
            //2 Decoding the image
            Bitmap decodedImage = convertIntoDecodeImage(encodedImage);
            // 3 Show the image
            capturedImage.setImageBitmap(decodedImage);

        }

    }

    /**
     *  This method converts  Bitmap ( using imageUri)  to Base64 string
     * In order to convert Bitmap into Base64 string, we convert Bitmap into  byteArray  & then use encodeToString  method
     * @param displayUri
     * @return Base64 string
     * @throws IOException
     *
     */
    private  String setEncodeImage(Uri displayUri) throws IOException {

        byte[] b;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), displayUri);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        b = baos.toByteArray();
        return (Base64.encodeToString(b, Base64.DEFAULT));
    }

    /**
     * This method converts Base64 string to Bitmap
     * @param displayImage
     * @return  bitmap
     *
     */
    private  Bitmap convertIntoDecodeImage(String displayImage) {
        byte[] b;
        b = Base64.decode(displayImage, Base64.DEFAULT);
        return (BitmapFactory.decodeByteArray(b, 0, b.length));
    }

}
