package com.example.carcare; // Kendi paket adınızla değiştirin

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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

    // UI Elemanları
    private RecyclerView recyclerViewChat;
    private ChatAdapter chatAdapter;
    private TextInputEditText etQuestion;
    private Button btnAsk;
    private ChipGroup chipGroupSuggestions; // YENİ

    // Veri Yapıları
    private List<Message> messageList = new ArrayList<>();
    private List<QuestionItem> questionList = new ArrayList<>();
    private List<String> suggestionList = new ArrayList<>(); // YENİ
    private String defaultAnswer = "I'm sorry, I could not find an answer for that.";


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

        // UI elemanlarını bağla
        recyclerViewChat = findViewById(R.id.recycler_view_chat);
        etQuestion = findViewById(R.id.et_question);
        btnAsk = findViewById(R.id.btn_ask);
        chipGroupSuggestions = findViewById(R.id.chip_group_suggestions); // YENİ

        // RecyclerView'ı ayarla
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);

        loadDataFromJson();
        populateSuggestionChips(); // YENİ: Chipleri oluştur

        btnAsk.setOnClickListener(v -> {
            String userQuery = etQuestion.getText().toString().trim();
            if (!userQuery.isEmpty()) {
                sendMessage(userQuery);
            }
        });

        addBotMessage("Hello! I am your AI Car Assistant. You can ask me a question or tap a suggestion below.");
    }

    private void sendMessage(String text) {
        addUserMessage(text);
        etQuestion.setText("");

        String botResponse = findAnswerForQuery(text);
        if (botResponse == null) {
            botResponse = defaultAnswer;
        }

        String finalBotResponse = botResponse;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            addBotMessage(finalBotResponse);
        }, 500);
    }

    private void addUserMessage(String text) {
        messageList.add(new Message(text, Message.Sender.USER));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerViewChat.scrollToPosition(messageList.size() - 1);
    }

    private void addBotMessage(String text) {
        messageList.add(new Message(text, Message.Sender.BOT));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerViewChat.scrollToPosition(messageList.size() - 1);
    }

    // YENİ: Öneri chiplerini oluşturan metod
    private void populateSuggestionChips() {
        chipGroupSuggestions.removeAllViews();
        for (String suggestion : suggestionList) {
            Chip chip = new Chip(this);
            chip.setText(suggestion);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> {
                // Bir çipe tıklandığında, metnini sendMessage'e gönder
                sendMessage(suggestion);
            });
            chipGroupSuggestions.addView(chip);
        }
    }

    private String findAnswerForQuery(String query) {
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT).trim();
        if (lowerCaseQuery.length() < 3) return null;

        for (QuestionItem item : questionList) {
            for (String keyword : item.getKeywords()) {
                String lowerCaseKeyword = keyword.toLowerCase(Locale.ROOT);
                if (lowerCaseQuery.contains(lowerCaseKeyword) || lowerCaseKeyword.contains(lowerCaseQuery)) {
                    return item.getAnswer();
                }
            }
        }
        return null;
    }

    private void loadDataFromJson() {
        String jsonString;
        try {
            InputStream is = getAssets().open("questions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonString = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Log.e(TAG, "Error reading JSON file", ex);
            return;
        }

        try {
            JSONObject rootObject = new JSONObject(jsonString);

            // YENİ: Önerileri yükle
            if (rootObject.has("suggestions")) {
                JSONArray suggestionsArray = rootObject.getJSONArray("suggestions");
                for (int i = 0; i < suggestionsArray.length(); i++) {
                    suggestionList.add(suggestionsArray.getString(i));
                }
            }

            // Mevcut: Soruları ve varsayılan cevabı yükle
            this.defaultAnswer = rootObject.getString("default_answer");
            JSONArray questionsArray = rootObject.getJSONArray("questions");
            for (int i = 0; i < questionsArray.length(); i++) {
                JSONObject q = questionsArray.getJSONObject(i);
                JSONArray keywordsArray = q.getJSONArray("keywords");
                List<String> keywords = new ArrayList<>();
                for (int j = 0; j < keywordsArray.length(); j++) {
                    keywords.add(keywordsArray.getString(j));
                }
                questionList.add(new QuestionItem(keywords, q.getString("answer")));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
    }

    // Yardımcı sınıf - Değişiklik yok
    private static class QuestionItem {
        private final List<String> keywords;
        private final String answer;
        public QuestionItem(List<String> keywords, String answer) { this.keywords = keywords; this.answer = answer; }
        public List<String> getKeywords() { return keywords; }
        public String getAnswer() { return answer; }
    }
}