package com.maghfoormalkana.fungai;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.maghfoormalkana.fungai.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button camera, gallery, sampleButton;
    ImageView imageView;
    TextView result;
    int imageSize = 64;

    //permission constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;
    //arrays of permission to be requested
    String[] cameraPermissions;
    String[] storagePermissions;


    //FIREBASE
    FirebaseAuth mAuth;
    FirebaseUser mUser;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        camera = findViewById(R.id.takeimgbtn);
        gallery = findViewById(R.id.chooseimgbtn);
        sampleButton = findViewById(R.id.samplebtn);
        imageView = findViewById(R.id.imgshow);
        result = findViewById(R.id.result);

        //init arrays of permission
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        mAuth = FirebaseAuth.getInstance();

        // Set OnClickListener to the camera button
        camera.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                requestCameraPermission();
            }
            else {

                //intent to start camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent,3);
            }


//            if (checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED){
//                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(cameraIntent,3);
//            } else {
//                requestPermissions(new String[]{Manifest.permission.CAMERA},100);
//            }
        });

        // Set OnClickListener to the gallery button
        gallery.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                requestStoragePermission();
            }
            else {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent,1);
            }


//            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//                // Storage permission granted, proceed
//                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(galleryIntent, 1);
//            } else {
//                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101); // Use a different request code
//            }
        });

        // opening login page
        Button funguyButton = findViewById(R.id.funguy);
        funguyButton.setOnClickListener(v -> {
            // Check if user is signed in (non-null) and update UI accordingly.
            mUser = mAuth.getCurrentUser();
            if(mUser != null){
                startActivity(new Intent(MainActivity.this, Community.class));
                //currentUser.reload();
            }else {
                // Start the login activity
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
            }

        });


        // Set OnClickListener to the sample button
        sampleButton.setOnClickListener(v -> {
            // Load the sample image from drawable
            Bitmap sampleImage = BitmapFactory.decodeResource(getResources(), R.drawable.sample);

            // Resize the image to the model's input size
            sampleImage = Bitmap.createScaledBitmap(sampleImage, imageSize, imageSize, false);

            // Set the resized image to the ImageView
            imageView.setImageBitmap(sampleImage);

            // Process and classify the sample image
            //classifyImage(sampleImage);
            // Displaying the Result
            result.setText("Galerina_marginata");

            //Add Wikipedia link for more information
            TextView wikiLinkTextView = findViewById(R.id.wiki_link);
            String result = "Galerina_marginata";
            String wikipediaUrl = "https://en.wikipedia.org/wiki/" + result.replace(" ", "_"); // Replace spaces with underscores
            //wikiLinkTextView.setText("More information on Wikipedia");
            wikiLinkTextView.setOnClickListener(g -> {
                //Open Wikipedia page in browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(wikipediaUrl));
                startActivity(intent);
            });

            // Make the wiki_link TextView visible
            wikiLinkTextView.setVisibility(View.VISIBLE);

        });

    }

    // Get class names from JSON

    private boolean checkStoragePermission() {
        // Check if the fragment is attached to an activity
        if (this != null) {
            // Check if camera permission and storage permission are granted
            boolean storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;

            // Return true if permissions are granted
            return storagePermission;
        } else {
            // If activity is null, display a Toast message to the user
            Toast.makeText(this, "Error: Null Activity", Toast.LENGTH_SHORT).show();
            return false;
        }

    }

    private void requestStoragePermission() {
        // Check if the fragment is attached to an activity
        if (this != null) {
            // Fragment is attached, request storage permission
            requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
        } else {
            // Fragment is not attached, show a Toast message
            Toast.makeText(this, "Error: Null Activity", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkCameraPermission() {
        // Check if the activity is not null
        if (this != null) {
            // Check if camera permission and storage permission are granted
            boolean cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
            boolean storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;

            // Return true if both permissions are granted
            return cameraPermission && storagePermission;
        } else {
            // If activity is null, display a Toast message to the user
            Toast.makeText(this, "Error: Null Activity", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void requestCameraPermission() {
        // Check if the activity is not null
        if (this != null) {
            // Request runtime camera permission
            requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
        } else {
            // If activity is null, display a Toast message to the user
            Toast.makeText(this, "Error: Null Activity", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    public void classifyImage(Bitmap image){
        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Classifying Image...");
        progressDialog.setCancelable(false);
        progressDialog.show();


        new android.os.Handler().postDelayed(() -> {
            // Process and classify the sample image
            try {
                // Process the input in Bitmap Image
                Model model = Model.newInstance(getApplicationContext());

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 64, 64, 3}, DataType.FLOAT32);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
                byteBuffer.order(ByteOrder.nativeOrder());

                int[] intValues = new int[imageSize * imageSize];
                image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

                int pixel = 0;
                //Iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer
                for (int i = 0; i < imageSize; i++){
                    for (int j = 0; j < imageSize; j++){
                        int val = intValues[pixel++]; // This Contains RGB value.
                        byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                        byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                        byteBuffer.putFloat((val & 0xFF) * (1.f / 255));
                    }
                }

                // Putting in byteBuffer
                inputFeature0.loadBuffer(byteBuffer);

                // Runs model inference and gets result.
                Model.Outputs outputs = model.process(inputFeature0);
                int maxPos = getMaxPos(outputs);

                // Load Class Names from "class_names.txt"
                String[] classes = readClassNames();
                // Load Names from "edible_list.txt"
                String[] edible = ediblelist();
                // Load Names from "cond_edible_list.txt"
                String[] nonEdible = nonEdiblelist();
                // Load Names from "poisonous_list.txt"
                String[] poisonous = poisonouslist();

                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                // Get the position with the highest confidence
                float[] confidences = outputFeature0.getFloatArray();
                runOnUiThread(() -> result.setText(classes[maxPos]));
                //result.setText(classes[maxPos] + " (" + String.format("%.2f", confidences[maxPos]) + ")");
                //result.setText(classes[maxPos] + " (" + confidences[maxPos] + " )");

                // Add Wikipedia link for more information
                TextView wikiLinkTextView = findViewById(R.id.wiki_link);
                TextView edibility = findViewById(R.id.edibility); //EDIBILITY TEXT VIEW

                String result = classes[maxPos];

                // Handling Edibility
                runOnUiThread(() -> {
                    GradientDrawable drawable = (GradientDrawable) edibility.getBackground();
                    if (Arrays.asList(edible).contains(result)) {
                        drawable.setColor(getResources().getColor(R.color.edibleColor));
                    } else if (Arrays.asList(nonEdible).contains(result)) {
                        drawable.setColor(getResources().getColor(R.color.conditionallyEdibleColor));
                    } else if (Arrays.asList(poisonous).contains(result)) {
                        drawable.setColor(getResources().getColor(R.color.poisonousColor));
                    }
                });

                String wikipediaUrl = "https://en.wikipedia.org/wiki/" + result.replace(" ", "_"); // Replace spaces with underscores
                //wikiLinkTextView.setText("More information on Wikipedia");
                wikiLinkTextView.setOnClickListener(v -> {
                    // Open Wikipedia page in browser
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(wikipediaUrl));
                    startActivity(intent);
                });

                // Make the wiki_link TextView visible
                wikiLinkTextView.setVisibility(View.VISIBLE);
                progressDialog.dismiss();
                // Releases model resources if no longer used.
                model.close();
            } catch (IOException e) {
                // Handle IOException here (e.g., log an error message)
            }

            // Dismiss the progress dialog after classification is completed
            progressDialog.dismiss();
        }, 2000); // 2-second delay

    }

    // This method is defined outside the classifyImage function
    private String[] readClassNames() {
        List<String> classNamesList = new ArrayList<>();
        try {
            // Open the InputStream for class_names.txt file
            InputStream inputStream = getAssets().open("all_class_names.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // Read each line from the file and add it to the list
            String line;
            while ((line = reader.readLine()) != null) {
                // Remove any leading/trailing whitespace and add to the list
                classNamesList.add(line.trim());
            }

            // Close the reader
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert the list to an array and return
        return classNamesList.toArray(new String[0]);
    }

    private String[] ediblelist() {
        List<String> edibleList = new ArrayList<>();
        try {
            // Open the InputStream for class_names.txt file
            InputStream inputStream = getAssets().open("edible_list.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // Read each line from the file and add it to the list
            String line;
            while ((line = reader.readLine()) != null) {
                // Remove any leading/trailing whitespace and add to the list
                edibleList.add(line.trim());
            }

            // Close the reader
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Convert the list to an array and return
        return edibleList.toArray(new String[0]);
    }

    private String[] nonEdiblelist() {
        List<String> condEdiblelist = new ArrayList<>();
        try {
            // Open the InputStream for class_names.txt file
            InputStream inputStream = getAssets().open("non_edible.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // Read each line from the file and add it to the list
            String line;
            while ((line = reader.readLine()) != null) {
                // Remove any leading/trailing whitespace and add to the list
                condEdiblelist.add(line.trim());
            }

            // Close the reader
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Convert the list to an array and return
        return condEdiblelist.toArray(new String[0]);
    }

    private String[] poisonouslist() {
        List<String> poisonouslist = new ArrayList<>();
        try {
            // Open the InputStream for class_names.txt file
            InputStream inputStream = getAssets().open("toxic.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // Read each line from the file and add it to the list
            String line;
            while ((line = reader.readLine()) != null) {
                // Remove any leading/trailing whitespace and add to the list
                poisonouslist.add(line.trim());
            }

            // Close the reader
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Convert the list to an array and return
        return poisonouslist.toArray(new String[0]);
    }

    private static int getMaxPos(Model.Outputs outputs) {
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        // Get the position with the highest confidence
        float[] confidences = outputFeature0.getFloatArray();
        // Find the index of the class with the biggest confidence
        int maxPos = 0;
        float maxConfidence = 0;
        for (int i = 0; i < confidences.length; i++){
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i];
                maxPos = i;
            }
        }
        return maxPos;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //  Taking the picture using the CAMERA
        if(resultCode == RESULT_OK){
            if(requestCode == 3) {
                Bitmap image = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);

                // Resizing the Image
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }else if (requestCode == 1) { // Gallery
                // Handle gallery-selected image
                Uri dat = data.getData();
                Bitmap imageg = null;
                try {
                    imageg = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Resize the image to the model's input size
                imageg = Bitmap.createScaledBitmap(imageg, imageSize, imageSize, false);

                // Set the resized image to the ImageView
                imageView.setImageBitmap(imageg);

                // Process and classify the sample image
                classifyImage(imageg);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}