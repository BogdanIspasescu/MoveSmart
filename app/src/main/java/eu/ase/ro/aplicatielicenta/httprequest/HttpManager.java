package eu.ase.ro.aplicatielicenta.httprequest;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

public class HttpManager implements Callable<String> {

    private HttpURLConnection connection;
    private InputStream inputStream;
    private InputStreamReader inputStreamReader;
    private BufferedReader reader;
    private final String url;

    public HttpManager(String url) {
        this.url = url;
    }

    @Override
    public String call() {
        try {
            return readResultFromHttp();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
        return null;
    }

    @NonNull
    private String readResultFromHttp() throws IOException {
        connection = (HttpURLConnection) new URL(url).openConnection();
        inputStream = connection.getInputStream();
        inputStreamReader = new InputStreamReader(inputStream);
        reader = new BufferedReader(inputStreamReader);

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    private void closeConnections() {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            inputStreamReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        connection.disconnect();
    }
}
