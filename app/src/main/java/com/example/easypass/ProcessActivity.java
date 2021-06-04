package com.example.easypass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;

public class ProcessActivity extends AppCompatActivity implements View.OnClickListener {
    TextView welcome_mess;
    Button btnPassport, btnID, btnBirthdate, btnPoliceCertificate, btnFamilyTree,btnDownLoad;
    FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference myRef;
    static final int PERMISSION_CAMERA = 888;
    static final int CAPTURE_IMAGE = 1024;
    ImageView PassPortPic, IdPic, BirthdatePic, PolicePic, FamilyTreePic;
    StorageReference storageReference;
    String imageName = "";
    Uri uri;


    @Override
    public void onBackPressed() {
        logoutUser();
    }

    private void logoutUser() {
        new AlertDialog.Builder(ProcessActivity.this).
                setTitle("התנתקות").
                setMessage("אתה בטוח שאתה רוצה להתנתק ?").
                setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mAuth.signOut();
                        startActivity(new Intent(ProcessActivity.this, MainActivity.class));
                    }
                })
                .setNegativeButton(android.R.string.no, null).
                setIcon(android.R.drawable.ic_dialog_info).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);
        storageReference = FirebaseStorage.getInstance().getReference().child("userDocuments");
        database = FirebaseDatabase.getInstance("https://easypass-dcff0-default-rtdb.europe-west1.firebasedatabase.app/");
        initViews();
        initButtons();
        readFromDB();
    }

    private void readFromDB() {
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String user = snapshot.child("userName").getValue(String.class);
                welcome_mess.setText("ברוך הבא " + user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initButtons() {
        btnPassport.setOnClickListener(this);
        btnID.setOnClickListener(this);
        btnBirthdate.setOnClickListener(this);
        btnPoliceCertificate.setOnClickListener(this);
        btnFamilyTree.setOnClickListener(this);
        btnDownLoad.setOnClickListener(this);
    }

    private void initViews() {
        mAuth = FirebaseAuth.getInstance();
        welcome_mess = findViewById(R.id.welcome_message);
        myRef = database.getReference("Users").child(mAuth.getUid());
        btnPassport = findViewById(R.id.btn_upload_pass);
        btnID = findViewById(R.id.btn_upload_ID);
        btnDownLoad = findViewById(R.id.downloadBtn);
        btnBirthdate = findViewById(R.id.btn_upload_birthdate);
        btnPoliceCertificate = findViewById(R.id.btn_upload_police_crteif);
        btnFamilyTree = findViewById(R.id.btn_upload_family_tree);
        PassPortPic = findViewById(R.id.passport_view);
        IdPic = findViewById(R.id.id_view);
        BirthdatePic = findViewById(R.id.birthdate_view);
        PolicePic = findViewById(R.id.police_view);
        FamilyTreePic = findViewById(R.id.tree_view);
    }

    private void takeImage() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        } else {
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(i, CAPTURE_IMAGE);
        }
    }

    private String getFileExtension() {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadTofirestorage() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading...");
        progressDialog.show();
        storageReference = FirebaseStorage.getInstance().getReference();
        storageReference = storageReference.child(mAuth.getUid()).child("userDocuments").child("photo_" + System.currentTimeMillis() + "." + getFileExtension());
        if (uri != null) {
            storageReference
                    .putFile(uri)
                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            storageReference.getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            DocumentUser docs = new DocumentUser(uri.toString(),uri.toString(),uri.toString(),uri.toString());
                                            myRef.child("userDocuments").setValue(docs);
                                            progressDialog.dismiss();
                                        }
                                    });
                        }
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTURE_IMAGE) {
            if (resultCode == RESULT_OK) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                uri = getImageUri(ProcessActivity.this, bitmap);
                uploadTofirestorage();


                DocumentUser pi = new DocumentUser();
                myRef.child(mAuth.getUid()).child("UserDoc").setValue(pi);
                if (imageName.equals("passport")) {
                    PassPortPic.setImageBitmap(bitmap);
                } else if (imageName.equals("id")) {
                    IdPic.setImageBitmap(bitmap);
                } else if (imageName.equals("birthdate")) {
                    BirthdatePic.setImageBitmap(bitmap);
                } else if (imageName.equals("police")) {
                    PolicePic.setImageBitmap(bitmap);
                } else if (imageName.equals("family")) {
                    FamilyTreePic.setImageBitmap(bitmap);
                } else {
                    Toast.makeText(getApplicationContext(), "לא הצלחת לעלות תמונה!", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "permission granted!", Toast.LENGTH_LONG).show();
                takeImage();
            } else {
                Toast.makeText(getApplicationContext(), "permission declined!", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_upload_pass:
                imageName = "passport";
                takeImage();
                break;
            case R.id.btn_upload_ID:
                imageName = "id";
                takeImage();
                break;
            case R.id.btn_upload_birthdate:
                imageName = "birthdate";
                takeImage();
                break;
            case R.id.btn_upload_police_crteif:
                imageName = "police";
                takeImage();
                break;
            case R.id.btn_upload_family_tree:
                imageName = "family";
                takeImage();
                break;
            case R.id.downloadBtn:
                Toast.makeText(getApplicationContext(), "כפתור הורדה ושליחה!", Toast.LENGTH_SHORT).show();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + v.getId());
        }
    }
}