package com.example.whatpill.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.whatpill.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final static int TAKE_PICTURE = 1;
    private static final String TAG = "MyActivity";

    Button btnCamera, btnSave;
    ImageView ivPill;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCamera = findViewById(R.id.btnCamera);
        btnSave = findViewById(R.id.btnSave);
        ivPill = findViewById(R.id.ivPill);

        btnCamera.setOnClickListener(this);
        btnSave.setOnClickListener(this);

        // 권한 체크 후 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    // 권한 요청
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            Log.d("로그", "Permission: " + permissions[0] + " was " + grantResults[0]);
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent1;

        switch (v.getId()) {
            // 촬영
            case R.id.btnCamera:
                intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent1, TAKE_PICTURE);
                break;
            // 저장
            case R.id.btnSave:
                clickUpload();
                imageUri = null;

                try{
                    Thread.sleep(1000);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }

                Intent intent2 = new Intent(this, Name.class);
                startActivity(intent2);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 사진 촬영 완료 후 응답
        if (requestCode == TAKE_PICTURE) {
            if (resultCode == RESULT_OK && data.hasExtra("data")) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                if (bitmap != null) ivPill.setImageBitmap(bitmap);

                String imageSaveUri = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "사진 저장", "사진이 저장되었습니다.");
                imageUri = Uri.parse(imageSaveUri);
                Log.d(TAG, "MainActivity - onActivityResult() called" + imageUri);
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        cursor.moveToNext();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
        Uri uri = Uri.fromFile(new File(path));

        cursor.close();
        return path;
    }

    // 파이어베이스 업로드 함수
    public void clickUpload() {

        // Firebase Storage를 관리하는 객체 얻어오기
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();

        // 원래 확장자는 파일의 실제 확장자를 얻어와서 사용해야함. 그러려면 이미지의 절대 주소를 구해야함.
        String filename = "pill.png";

        // 폴더가 없으면 자동 생성
        StorageReference imgRef = firebaseStorage.getReference("uploads/" + filename);


        // 참조 객체를 통해 이미지 파일 업로드
        // imgRef.putFile(imgUri);
        // 업로드 결과를 받고 싶다면 아래와 같이 UploadTask를 사용하면 된다.
        UploadTask uploadTask = imgRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this, "success upload", Toast.LENGTH_SHORT).show();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "MainActivity - onFailure() called");
                    }
                });
    }
}