package com.example.carcare;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    // EditText alanları
    private EditText editEmail, editPassword;
    // Buton
    private Button btnLogin;
    // TextView bağlantıları
    private TextView tvCreateAccount, tvForgotPassword;

    // FirebaseAuth nesnesi
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);  // Yeni tasarım XML'inizin adı

        // View'leri tanımla
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);

        tvCreateAccount = findViewById(R.id.tv_create_account);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);

        // FirebaseAuth örneği
        auth = FirebaseAuth.getInstance();

        // "SIGN IN" butonuna tıklayınca giriş işlemi
        btnLogin.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Email ve şifre boş olamaz.", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(LoginActivity.this, "Giriş başarılı!", Toast.LENGTH_SHORT).show();
                        // Başarılı giriş sonrası ana sayfaya (MainActivity) geçiş
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this, "Giriş başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // "Create an Account" metnine tıklayınca RegisterActivity'ye geçiş
        tvCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // "Forgot Password?" metnine tıklayınca şifre sıfırlama diyaloğu
        tvForgotPassword.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });
    }

    // Şifremi Unuttum diyaloğunu göstermek
    private void showForgotPasswordDialog() {
        // dialog_forgot_password.xml dosyasını inflate ediyoruz
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null);
        final EditText editEmailReset = dialogView.findViewById(R.id.edit_email_reset);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Şifremi Unuttum");
        builder.setView(dialogView);
        builder.setPositiveButton("Gönder", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = editEmailReset.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Lütfen email adresinizi girin.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Firebase Auth üzerinden şifre sıfırlama maili gönder
                auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(LoginActivity.this, "Şifre sıfırlama maili gönderildi.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(LoginActivity.this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }
}
