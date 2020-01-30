package com.example.mytrackerapp;

import java.io.IOException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import android.os.AsyncTask;

// param type, progress bar, return value
class PostData extends AsyncTask<String, Integer, Long> {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();
    public MyLocationService delegate = null;
    private String resp;

    protected String doPost(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    protected Long doInBackground(String... msgs){
        try {
            String url = msgs[0];
            String json = msgs[1];
            resp = this.doPost(url, json);
            return 200L;
        } catch( Exception e ) {
            String foo = e.getMessage();
        }
        return 499L;
    }

    protected void onPostExecute(Long result) {
        delegate.onPostComplete(result, resp);
    }
}
