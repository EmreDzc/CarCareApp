package com.example.carcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button btnLogin, btnGoToRegister;
    private FirebaseAuth auth;  // FirebaseAuth nesnesi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);  // activity_login.xml layout'unu kullanıyoruz

        // Layout'taki view'leri tanımlıyoruz
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoToRegister = findViewById(R.id.btn_go_to_register);

        // FirebaseAuth örneğini alıyoruz
        auth = FirebaseAuth.getInstance();

        // "Giriş Yap" butonuna tıklanınca giriş işlemini gerçekleştiriyoruz
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = editEmail.getText().toString().trim();
                String password = editPassword.getText().toString().trim();

                // Giriş bilgilerini kontrol ediyoruz
                if(email.isEmpty() || password.isEmpty()){
                    Toast.makeText(LoginActivity.this, "Email ve şifre boş olamaz.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Firebase ile e-posta ve şifre üzerinden giriş yapıyoruz
                auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            // Giriş başarılı ise MainActivity'ye yönlendiriyoruz
                            Toast.makeText(LoginActivity.this, "Giriş başarılı!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            // Hata durumunda kullanıcıya hata mesajı gösteriyoruz
                            Toast.makeText(LoginActivity.this, "Giriş başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // "Kayıt Ol" butonuna tıklanınca RegisterActivity'ye geçiş yapıyoruz
        btnGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }
}
