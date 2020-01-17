package com.example.tareacloudvision;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.TextAnnotation;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
public class MainActivity extends AppCompatActivity {

    ImageView imagen;
    TextView texto;
    Vision vision;
    TextAnnotation text;
    List<FaceAnnotation> faces;
    String path;
    private final String CARPETA_RAIZ="DCIM/";
    private final String RUTA_IMAGEN=CARPETA_RAIZ+"Camara";
    final int COD_SELECCIONA=10;
    final int COD_FOTO=20;
    public static final String FILE_NAME = "temp.jpg";
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imagen = (ImageView)findViewById(R.id.imageView2);
        texto = (TextView)findViewById(R.id.textView2);

        //*******CONFIGURANDO Google API Client for Cloud Vision*********//
        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(), new AndroidJsonFactory(), null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer(""));
        vision = visionBuilder.build();
    }

    public void onCBBuscarImagen(View view){
        startGalleryChooser();
    }

    public void onCBOpenCamera(View view){
        startCamera();
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        File fileImagen=new File(Environment.getExternalStorageDirectory(),RUTA_IMAGEN);
        boolean isCreada=fileImagen.exists();
        String nombreImagen="";
        if(isCreada==false){
            isCreada=fileImagen.mkdirs();
        }

        if(isCreada==true){
            nombreImagen=(System.currentTimeMillis()/1000)+".jpg";
        }


        path=Environment.getExternalStorageDirectory()+
                File.separator+RUTA_IMAGEN+File.separator+nombreImagen;

        File imagen=new File(path);

        Intent intent=null;
        intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        ////
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        {
            String authorities=getApplicationContext().getPackageName()+".provider";
            Uri imageUri= FileProvider.getUriForFile(this,authorities,imagen);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        }else
        {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imagen));
        }
        startActivityForResult(intent,COD_FOTO);
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            MediaScannerConnection.scanFile(this, new String[]{path}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("Ruta de almacenamiento","Path: "+path);
                        }
                    });

            Bitmap bitmap= BitmapFactory.decodeFile(path);
            imagen.setImageBitmap(bitmap);
        }
    }

    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                MAX_DIMENSION);

                //callCloudVision(bitmap);
                imagen.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                //Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            //Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }


     String res ="";
    public void onCBProcesarTextDetection(View view){
        texto.setText("");
        //Codificar la Imagen como “Base64string
        //Convertir la imagen a un Vector de Bytes
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,90,stream);
                byte[]imagenInByte = stream.toByteArray();

                //IMAGEN GOOGLE CLOUD VISION
                Image inputImage = new Image();
                inputImage.encodeContent(imagenInByte);

                //REALIZAR LA SOLICITUD
                Feature desiredFeature = new Feature();
                desiredFeature.setType("TEXT_DETECTION");

                //**Crear la anotacion de la solicitud
                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(inputImage);
                request.setFeatures(Arrays.asList(desiredFeature));

                BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));

                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);

                    //Ejecuta la solicitud
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse batchResponse = annotateRequest.execute();

                    //Tratar la Respuesta - Text
                    text = batchResponse.getResponses().get(0).getFullTextAnnotation();

                    if(text!=null)
                    {
                        res ="";
                        res = text.getText();
                        //Fin Tratar la Respuesta - Text
                    }
                    else{
                        message +="No se reconoce ningun texto en la imagen"+ "\n";
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView) findViewById (R.id.textView2);
                            imageDetail.setText(res.toString());
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

     String message="";
    public void onCBProcesarFaceDetection(View view){
        texto.setText("");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,90,stream);
                byte[]imagenInByte = stream.toByteArray();

                //IMAGEN GOOGLE CLOUD VISION
                Image inputImage = new Image();
                inputImage.encodeContent(imagenInByte);

                //REALIZAR LA SOLICITUD
                Feature desiredFeature = new Feature();
                desiredFeature.setType("FACE_DETECTION");

                //**Crear la anotacion de la solicitud
                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(inputImage);
                request.setFeatures(Arrays.asList(desiredFeature));
                BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));

                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);

                    //Ejecuta la solicitud
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse batchResponse = annotateRequest.execute();

                    //Tratar la Respuesta – Identificación de Imágenes.
                    faces = batchResponse.getResponses().get(0).getFaceAnnotations();
                    int numberOfFaces;
                    if (faces!= null)
                    {
                        message="";
                                 numberOfFaces = faces.size();

                            String likelihoods = "";
                            for(int i=0; i<numberOfFaces; i++) {
                                likelihoods += "\n It is " + faces.get(i).getJoyLikelihood() + " that face " + i + " is happy";
                            }
                            message = "This photo has " + numberOfFaces + " faces" + likelihoods;
                            //Fin Tratar la Respuesta – Identificación de Imágenes.
                    }else{

                        message = "No se reconoce ningun rostro en la imagen"+ "\n";

                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView) findViewById (R.id.textView2);
                            imageDetail.setText(message.toString());
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    public void onCBProcesarLabelDetection(View view){
        texto.setText("");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
                Bitmap bitmap = drawable.getBitmap();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,90,stream);
                byte[]imagenInByte = stream.toByteArray();

                //IMAGEN GOOGLE CLOUD VISION
                Image inputImage = new Image();
                inputImage.encodeContent(imagenInByte);

                //REALIZAR LA SOLICITUD
                Feature desiredFeature = new Feature();
                desiredFeature.setType("LABEL_DETECTION");

                //**Crear la anotacion de la solicitud
                AnnotateImageRequest request = new AnnotateImageRequest();
                request.setImage(inputImage);
                request.setFeatures(Arrays.asList(desiredFeature));

                BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
                batchRequest.setRequests(Arrays.asList(request));

                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    //Ejecuta la solicitud
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse batchResponse = annotateRequest.execute();

                    //Tratar la Respuesta – Detección de Objetos
                    final StringBuilder smessage = new StringBuilder("I found these things:\n\n");
                    List<EntityAnnotation> labels = batchResponse.getResponses().get(0).getLabelAnnotations();
                    if (labels != null) {
                        for (EntityAnnotation label : labels) {
                            smessage.append(String.format(Locale.US, "%.3f: %s",label.getScore(), label.getDescription()));
                            smessage.append("\n");
                        }
                    } else {
                        smessage.append("nothing");
                    }
                    //Fin Tratar la Respuesta – Detección de Objetos

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView) findViewById (R.id.textView2);
                            imageDetail.setText(smessage.toString());
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
