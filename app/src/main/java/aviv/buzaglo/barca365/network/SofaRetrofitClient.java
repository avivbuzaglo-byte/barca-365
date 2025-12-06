package aviv.buzaglo.barca365.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SofaRetrofitClient {

    private static final String BASE_URL = "https://api.sofascore.com/api/v1/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {

            // 1. יצירת ה"מדפסת" (Interceptor)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Level.BODY אומר לו להדפיס את הכל כולל התוכן של ה-JSON
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 2. חיבור המדפסת ללקוח תקשורת (OkHttpClient)
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            // 3. חיבור הלקוח ל-Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client) // הוספנו את הלקוח המשודרג
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}