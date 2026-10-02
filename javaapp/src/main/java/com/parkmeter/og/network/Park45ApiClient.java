package com.parkmeter.og.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.parkmeter.og.model.Zone;

import java.lang.reflect.Type;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Park45ApiClient {
    private static final String BASE_URL = ApiConfig.BASE_URL;
    private static Park45ApiClient instance;
    private Park45ApiService apiService;

    private Park45ApiClient() {
        // Create logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Create OkHttp client with SSL bypass for development
        OkHttpClient client = createOkHttpClient(loggingInterceptor);

        // Create Gson instance with custom deserializers for Zone nested types.
        // The getZonesById API returns "org" and "city_id" as plain string IDs,
        // while the getZones API returns them as full objects — both forms are handled here.
        Gson gson = new GsonBuilder()
                .setLenient()
                .registerTypeAdapter(Zone.Organization.class,
                        (JsonDeserializer<Zone.Organization>) (json, typeOfT, context) -> {
                            Zone.Organization org = new Zone.Organization();
                            if (json.isJsonPrimitive()) {
                                org.setId(json.getAsString());
                            } else if (json.isJsonObject()) {
                                JsonObject obj = json.getAsJsonObject();
                                if (obj.has("_id") && !obj.get("_id").isJsonNull())
                                    org.setId(obj.get("_id").getAsString());
                                if (obj.has("org_name") && !obj.get("org_name").isJsonNull())
                                    org.setOrgName(obj.get("org_name").getAsString());
                                if (obj.has("color") && !obj.get("color").isJsonNull())
                                    org.setColor(obj.get("color").getAsString());
                                if (obj.has("logo") && !obj.get("logo").isJsonNull())
                                    org.setLogo(obj.get("logo").getAsString());
                                if (obj.has("sub_domain") && !obj.get("sub_domain").isJsonNull())
                                    org.setSubDomain(obj.get("sub_domain").getAsString());
                                if (obj.has("service_fee") && !obj.get("service_fee").isJsonNull())
                                    org.setServiceFee(obj.get("service_fee").getAsInt());
                                if (obj.has("payment_gateway") && !obj.get("payment_gateway").isJsonNull())
                                    org.setPaymentGateway(obj.get("payment_gateway").getAsString());
                                if (obj.has("stripe_publishable_key") && !obj.get("stripe_publishable_key").isJsonNull())
                                    org.setStripePublishableKey(obj.get("stripe_publishable_key").getAsString());
                                if (obj.has("stripe_secret_key") && !obj.get("stripe_secret_key").isJsonNull())
                                    org.setStripeSecretKey(obj.get("stripe_secret_key").getAsString());
                                if (obj.has("ssl_installed") && !obj.get("ssl_installed").isJsonNull())
                                    org.setSslInstalled(obj.get("ssl_installed").getAsBoolean());
                            }
                            return org;
                        })
                .registerTypeAdapter(Zone.City.class,
                        (JsonDeserializer<Zone.City>) (json, typeOfT, context) -> {
                            Zone.City city = new Zone.City();
                            if (json.isJsonPrimitive()) {
                                city.setId(json.getAsString());
                            } else if (json.isJsonObject()) {
                                JsonObject obj = json.getAsJsonObject();
                                if (obj.has("_id") && !obj.get("_id").isJsonNull())
                                    city.setId(obj.get("_id").getAsString());
                                if (obj.has("city_name") && !obj.get("city_name").isJsonNull())
                                    city.setCityName(obj.get("city_name").getAsString());
                                if (obj.has("time_zone") && !obj.get("time_zone").isJsonNull())
                                    city.setTimeZone(obj.get("time_zone").getAsString());
                            }
                            return city;
                        })
                .create();

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // Create API service
        apiService = retrofit.create(Park45ApiService.class);
    }
    
    private OkHttpClient createOkHttpClient(HttpLoggingInterceptor loggingInterceptor) {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            // Fallback to default client if SSL setup fails
            return new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build();
        }
    }

    public static synchronized Park45ApiClient getInstance() {
        if (instance == null) {
            instance = new Park45ApiClient();
        }
        return instance;
    }

    public Park45ApiService getApiService() {
        return apiService;
    }
} 
