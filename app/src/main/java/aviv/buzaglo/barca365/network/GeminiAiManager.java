package aviv.buzaglo.barca365.network;
import aviv.buzaglo.barca365.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiAiManager {
    public interface GeminiCallback {
        void onSuccess(String analysis);
        void onError(Throwable t);
    }

    public static void analyzeMatch(String prompt, GeminiCallback callback) {
        // הגדרת המודל (gemini-1.5-flash הוא המהיר והזול ביותר)
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder().addText(prompt).build();

        // יצירת Thread נפרד כדי לא לתקוע את האפליקציה
        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                callback.onSuccess(text);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(t);
            }
        }, executor); // מריץ את התשובה חזרה ב-Executor
    }
}