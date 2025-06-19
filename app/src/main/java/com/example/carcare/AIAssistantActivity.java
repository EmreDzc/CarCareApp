package com.example.carcare; // Kendi paket adınızla değiştirin

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AIAssistantActivity extends AppCompatActivity {

    private static final String TAG = "AIAssistantActivity";

    private TextInputEditText etQuestion;
    private Button btnAsk;
    private TextView tvAnswer;

    private List<QuestionItem> questionList = new ArrayList<>();
    private String defaultAnswer = "Üzgünüm, bu soruyu anlayamadım.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        Toolbar toolbar = findViewById(R.id.toolbar_ai_assistant);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etQuestion = findViewById(R.id.et_question);
        btnAsk = findViewById(R.id.btn_ask);
        tvAnswer = findViewById(R.id.tv_answer);

        loadQuestionsFromJson();

        btnAsk.setOnClickListener(v -> {
            String userQuery = etQuestion.getText().toString().trim();
            if (userQuery.isEmpty()) {
                Toast.makeText(this, "Please type a question.", Toast.LENGTH_SHORT).show();
                return;
            }
            String answer = findAnswerForQuery(userQuery);
            tvAnswer.setText(answer);
        });
    }

    private String findAnswerForQuery(String query) {
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT).trim(); // Kullanıcı girdisini küçük harfe çevir ve boşlukları temizle

        // Erken çıkış: Kullanıcı çok kısa bir şey yazdıysa işlem yapma (isteğe bağlı ama önerilir)
        if (lowerCaseQuery.length() < 3) {
            return null; // Cevap bulunamadı olarak kabul et
        }

        for (QuestionItem item : questionList) {
            for (String keyword : item.getKeywords()) {
                String lowerCaseKeyword = keyword.toLowerCase(Locale.ROOT);

                // =====================================================================
                // ==                 İŞTE ANAHTAR DEĞİŞİKLİK BURADA                  ==
                // =====================================================================
                //
                // İKİ YÖNLÜ KONTROL:
                // 1. Kullanıcının yazdığı uzun cümle, bizim kısa anahtar kelimemizi içeriyor mu?
                //    (Örnek: "what is advanced driver assistance".contains("advanced driver"))
                //
                // VEYA
                //
                // 2. Bizim anahtar kelimemiz, kullanıcının yazdığı kısa sorguyu içeriyor mu?
                //    (Örnek: "advanced driver assistance".contains("advanced driver"))
                //
                if (lowerCaseQuery.contains(lowerCaseKeyword) || lowerCaseKeyword.contains(lowerCaseQuery)) {
                    // Eşleşme bulundu, cevabı döndür
                    return item.getAnswer();
                }
            }
        }

        return defaultAnswer; // Cevap bulunamadı, bu metottan null dönelim
    }

    private void loadQuestionsFromJson() {
        String jsonString;
        try {
            InputStream is = getAssets().open("questions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Log.e(TAG, "JSON dosyası okunurken hata oluştu", ex);
            tvAnswer.setText("Asistan verileri yüklenemedi. Lütfen daha sonra tekrar deneyin.");
            btnAsk.setEnabled(false);
            return;
        }

        try {
            JSONObject rootObject = new JSONObject(jsonString);
            this.defaultAnswer = rootObject.getString("default_answer");
            JSONArray questionsArray = rootObject.getJSONArray("questions");

            for (int i = 0; i < questionsArray.length(); i++) {
                JSONObject questionObject = questionsArray.getJSONObject(i);
                String answer = questionObject.getString("answer");
                JSONArray keywordsArray = questionObject.getJSONArray("keywords");

                List<String> keywords = new ArrayList<>();
                for (int j = 0; j < keywordsArray.length(); j++) {
                    keywords.add(keywordsArray.getString(j));
                }
                questionList.add(new QuestionItem(keywords, answer));
            }
            Log.d(TAG, "JSON'dan " + questionList.size() + " soru başarıyla yüklendi.");
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse edilirken hata oluştu", e);
            tvAnswer.setText("Asistan verileri bozuk. Lütfen geliştirici ile iletişime geçin.");
            btnAsk.setEnabled(false);
        }
    }

    // JSON verisini tutmak için basit bir yardımcı sınıf (POJO)
    private static class QuestionItem {
        private final List<String> keywords;
        private final String answer;

        public QuestionItem(List<String> keywords, String answer) {
            this.keywords = keywords;
            this.answer = answer;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public String getAnswer() {
            return answer;
        }
    }
}