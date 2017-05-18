package com.example.skyma.testrecognition;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.IntegerRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skyma.testrecognition.Models.Line;
import com.example.skyma.testrecognition.Models.Region;
import com.example.skyma.testrecognition.Models.VisionFile;
import com.example.skyma.testrecognition.Models.Word;
import com.example.skyma.testrecognition.Retrofit.RetrofitInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.skyma.testrecognition.Retrofit.RetrofitBuilder.createService;


public class MainActivity extends AppCompatActivity {

    RetrofitInterface retrofitInterface;
    List<VisionFile> visionFiles;
    TextView textView;
    Button button;
    HashMap<String, String> hashWords;
    List<Word> allWords;
    Call<VisionFile> visionFileCall;

    // List of words representing the eye
    String[] leftEyeWords = {"left", "os", "o.s", "o.s.", "left eye", "O.S.", "OS", "O.S", "Left"};
    String[] rightEyeWords = {"right", "od", "o.d", "o.d.", "right eye", "O.D.", "OD", "O.D", "Right"};

    // List of words representing the parameters
    String[] eyeCylinder = {"Cylinder", "CYL", "cyl", "Cylindrical"};
    String[] eyeSphere = {"Sphere", "Sph", "sph", "Spherical"};
    String[] eyeAxis = {"Axis", "axis"};
    // This is usually not mentioned on the prescriptions
    String[] eyeBCVA = {"BCVA", "bcva", "B.C.V.A", "b.c.v.a", "b.c.v.a.", "B.C.V.A."};
    // This is usually not mentioned on the prescriptions
    String[] eyeUCVA = {"UCVA", "ucva", "U.C.V.A", "u.c.v.a", "u.c.v.a.", "U.C.V.A."};

    EditText leftCylinder, leftSphere, leftAxis, leftBCVA, leftUCVA,
        rightCylinder, rightSphere, rightAxis, rightBCVA, rightUCVA;

    // Table containing the extracted eye param values
    double[][] eyeParams;

    HashMap<String, Integer> eyeIndices = new HashMap<>();
    HashMap<String, Integer> params = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().build();
        StrictMode.setThreadPolicy(policy);

        leftCylinder = (EditText) findViewById(R.id.left_cylinder);
        leftSphere = (EditText) findViewById(R.id.left_sphere);
        leftAxis = (EditText) findViewById(R.id.left_axis);
        leftBCVA = (EditText) findViewById(R.id.left_bcva);
        leftUCVA = (EditText) findViewById(R.id.left_ucva);

        rightCylinder = (EditText) findViewById(R.id.right_cylinder);
        rightSphere = (EditText) findViewById(R.id.right_sphere);
        rightAxis = (EditText) findViewById(R.id.right_axis);
        rightBCVA = (EditText) findViewById(R.id.right_bcva);
        rightUCVA = (EditText) findViewById(R.id.right_ucva);

        allWords = new ArrayList<>();
        hashWords = new HashMap<>();

        eyeIndices.put("Left", 0);
        eyeIndices.put("Right", 1);

        params.put("Axis", 0);
        params.put("Cylinder", 1);
        params.put("Sphere", 2);
        params.put("UCVA", 3);
        params.put("BCVA", 4);

        eyeParams = new double[2][5];
        for(int i=0; i<eyeParams.length; i++)
            for(int j=0; j<eyeParams[i].length; j++)
                eyeParams[i][j] = Integer.MIN_VALUE;

        textView = (TextView) findViewById(R.id.text);
        button = (Button) findViewById(R.id.button);

        retrofitInterface = createService(RetrofitInterface.class, "");
        // Toast.makeText(MainActivity.this, "File URI extracted : " + getIntent().getStringExtra("FILE_URI"), Toast.LENGTH_SHORT).show();
        // final Call<VisionFile> visionFileCall = retrofitInterface.getOCRData(getIntent().getStringExtra("FILE_URI"));
        visionFileCall = retrofitInterface.getOCRData();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Performing OCR, please wait ", Toast.LENGTH_SHORT).show();
                visionFileCall.enqueue(new Callback<VisionFile>() {
                    @Override
                    public void onResponse(Call<VisionFile> call, Response<VisionFile> response) {
                        if(response.isSuccessful()){
                            Toast.makeText(MainActivity.this, "Response was successful", Toast.LENGTH_SHORT).show();
                            textView.setText(response.body().language);

                            List<Region> regions = response.body().regions;
                            for(int i=0; i<regions.size(); i++){
                                List<Line> lines = regions.get(i).lines;
                                for(int j=0; j<lines.size(); j++){
                                    List<Word> words = lines.get(j).words;
                                    for(int k=0; k<words.size(); k++){
                                        allWords.add(words.get(k));
                                    }
                                }
                            }

                            // Fetch the following set of parameters for the left and the right eye
                            // 1. Sphere, 2. Axis, 3. UCVA, 4. BCVA, 5. Sphere
                            // A standard format for the eye prescription document is assumed


                            // Display word and bounding box inside allWords array
                            for(Word word : allWords){
                                Log.i("WORD", word.text + " : " + word.boundingBox);
                                hashWords.put(word.boundingBox, word.text);
                            }
                            extractEyeValues();
                        }
                    }

                    @Override
                    public void onFailure(Call<VisionFile> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Response was unsuccessful", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // Algorithm for finding the value from the text
    public void extractEyeValues(){
        // identify the y coordinate of the word corresponding to the left and the right eye
        int leftEyeY = -1;
        int rightEyeY = -1;

        // determine the x coordinate associated with the various eye parameters
        int axisX = -1;
        int cylinderX = -1;
        int sphereX = -1;
        int UCVAX = -1;
        int BCVAX = -1;

        for(int i=0; i<allWords.size(); i++){
            if(Arrays.asList(leftEyeWords).contains(allWords.get(i).text)){
                leftEyeY = Integer.parseInt(allWords.get(i).boundingBox.split(",")[1]);
            }
            if (Arrays.asList(rightEyeWords).contains(allWords.get(i).text)){
                rightEyeY = Integer.parseInt(allWords.get(i).boundingBox.split(",")[1]);
            }
            if (Arrays.asList(eyeAxis).contains(allWords.get(i).text)){
                axisX = Integer.parseInt(allWords.get(i).boundingBox.split(",")[0]);
            }
            if (Arrays.asList(eyeCylinder).contains(allWords.get(i).text)){
                cylinderX = Integer.parseInt(allWords.get(i).boundingBox.split(",")[0]);
            }
            if (Arrays.asList(eyeSphere).contains(allWords.get(i).text)){
                sphereX = Integer.parseInt(allWords.get(i).boundingBox.split(",")[0]);
            }
            if(Arrays.asList(eyeUCVA).contains(allWords.get(i).text)){
                UCVAX = Integer.parseInt(allWords.get(i).boundingBox.split(",")[0]);
            }
            if (Arrays.asList(eyeBCVA).contains(allWords.get(i).text)) {
                BCVAX = Integer.parseInt(allWords.get(i).boundingBox.split(",")[0]);
            }
        }

        // Print the left and right y coordinates of the eye
        /*Toast.makeText(MainActivity.this, "Left eye y coordinate : "  + leftEyeY + ", Right " +
                "eye y coordinate : " + rightEyeY, Toast.LENGTH_SHORT).show();*/
        textView.setText("Left eye y : " + leftEyeY + ", Right eye y : " + rightEyeY +
                ", Cylinder X : " + cylinderX + ", Sphere X : " + sphereX + ", Axis X : " + axisX +
        ", UCVA X : " + UCVAX + ", BCVA X : " + BCVAX);

        // Find out the value belonging to each of the eye parameters
        for(String key : hashWords.keySet()){
            if(isNumeric(hashWords.get(key))){
                // Toast.makeText(MainActivity.this, "The value is numeric : " + hashWords.get(key), Toast.LENGTH_SHORT).show();
                // check the x,y coordinates of the bounding box

                int xBoundingBox = Integer.parseInt(key.split(",")[0]);
                int yBoundingBox = Integer.parseInt(key.split(",")[1]);

                // check yBoundingBox with the y coordinate of the eyes
                String eye = Math.abs(yBoundingBox - leftEyeY) < Math.abs(yBoundingBox - rightEyeY) ? "Left" : "Right";

                // check xBoundingBox with the x coordinate of eye parameters
                int min = Integer.MAX_VALUE;
                String param = "";

                // Find the parameter
                if(axisX != -1 && Math.abs(xBoundingBox - axisX) < min) {
                    min = Math.abs(xBoundingBox - axisX);
                    param = "Axis";
                }

                if(cylinderX != -1 && Math.abs(xBoundingBox - cylinderX) < min){
                    min = Math.abs(xBoundingBox - cylinderX);
                    param = "Cylinder";
                }

                if (sphereX != -1 && Math.abs(xBoundingBox - sphereX) < min){
                    min = Math.abs(xBoundingBox - sphereX);
                    param = "Sphere";
                }

                if (UCVAX != -1 && Math.abs(xBoundingBox - UCVAX) < min){
                    min = Math.abs(xBoundingBox - UCVAX);
                    param = "UCVA";
                }

                if (BCVAX != -1 && Math.abs(xBoundingBox - BCVAX) < min){
                    min = Math.abs(xBoundingBox - BCVAX);
                    param = "BCVA";
                }

                // if both the eye and the param are successfully found
                if(!eye.equals("") && !params.equals("")) {
                    // tag the value / text with the eye and param
                    eyeParams[eyeIndices.get(eye)][params.get(param)] = Double.parseDouble(hashWords.get(key));
                    // Toast.makeText(MainActivity.this, "Value : " + hashWords.get(key), Toast.LENGTH_SHORT).show();
                    // Toast.makeText(MainActivity.this, "Eye : " + eye + ", Param : " + param, Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Print the values of the table''''''''
        for(int i=0; i<eyeParams.length; i++){
            for(int j=0; j<eyeParams[i].length; j++){
                Log.i("SECOP", Double.toString(eyeParams[i][j]));
            }
        }

        if(eyeParams[eyeIndices.get("Left")][params.get("Cylinder")] != Integer.MIN_VALUE)
            leftCylinder.setText(Double.toString(eyeParams[eyeIndices.get("Left")][params.get("Cylinder")]));
        if(eyeParams[eyeIndices.get("Left")][params.get("Sphere")] != Integer.MIN_VALUE)
            leftSphere.setText(Double.toString(eyeParams[eyeIndices.get("Left")][params.get("Sphere")]));
        if(eyeParams[eyeIndices.get("Left")][params.get("Axis")] != Integer.MIN_VALUE)
            leftAxis.setText(Double.toString(eyeParams[eyeIndices.get("Left")][params.get("Axis")]));
        if(eyeParams[eyeIndices.get("Left")][params.get("BCVA")] != Integer.MIN_VALUE)
            leftBCVA.setText(Double.toString(eyeParams[eyeIndices.get("Left")][params.get("BCVA")]));
        if(eyeParams[eyeIndices.get("Left")][params.get("UCVA")] != Integer.MIN_VALUE)
            leftUCVA.setText(Double.toString(eyeParams[eyeIndices.get("Left")][params.get("UCVA")]));

        if(eyeParams[eyeIndices.get("Right")][params.get("Cylinder")] != Integer.MIN_VALUE)
            rightCylinder.setText(Double.toString(eyeParams[eyeIndices.get("Right")][params.get("Cylinder")]));
        if(eyeParams[eyeIndices.get("Right")][params.get("Sphere")] != Integer.MIN_VALUE)
            rightSphere.setText(Double.toString(eyeParams[eyeIndices.get("Right")][params.get("Sphere")]));
        if(eyeParams[eyeIndices.get("Right")][params.get("Axis")] != Integer.MIN_VALUE)
            rightAxis.setText(Double.toString(eyeParams[eyeIndices.get("Right")][params.get("Axis")]));
        if(eyeParams[eyeIndices.get("Right")][params.get("BCVA")] != Integer.MIN_VALUE)
            rightBCVA.setText(Double.toString(eyeParams[eyeIndices.get("Right")][params.get("BCVA")]));
        if(eyeParams[eyeIndices.get("Right")][params.get("UCVA")] != Integer.MIN_VALUE)
            rightUCVA.setText(Double.toString(eyeParams[eyeIndices.get("Right")][params.get("UCVA")]));

    }

    // Method to check if a given string is a decimal
    public boolean isNumeric(String word){
        return word.matches("-?\\d+(\\.\\d+)?");
    }
}
