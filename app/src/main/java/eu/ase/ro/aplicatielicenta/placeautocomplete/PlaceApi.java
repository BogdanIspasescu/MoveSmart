package eu.ase.ro.aplicatielicenta.placeautocomplete;
import android.content.Context;
import android.content.res.Resources;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import eu.ase.ro.aplicatielicenta.R;

public class PlaceApi {

    public ArrayList<String> autoComplete(String input){
        ArrayList<String> arrayList=new ArrayList();
        HttpURLConnection connection=null;
        StringBuilder jsonResult=new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/autocomplete/json?");
            sb.append("input=" + input);
            sb.append("&language=en&key=");
            String key= Resources.getSystem().getString(R.string.api_key);
            //sb.append(key);
            sb.append("AIzaSyAN52Ik37ROuqgIlSVUj2RogdvI6EgTl0E");
            URL url = new URL(sb.toString());
            connection=(HttpURLConnection)url.openConnection();
            InputStreamReader inputStreamReader=new InputStreamReader(connection.getInputStream());

            int read;
            char[] buff=new char[1024];
            while((read= inputStreamReader.read(buff))!=-1){
                jsonResult.append(buff,0,read);
            }
        }
        catch(MalformedURLException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally{
            if(connection!=null){
                connection.disconnect();
            }
        }
        try{
            JSONObject jsonObject=new JSONObject(jsonResult.toString());
            JSONArray prediction=jsonObject.getJSONArray("predictions");
            for(int i=0;i<prediction.length();i++){
                arrayList.add(prediction.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return arrayList;
    }


    public static LatLng getPlaceLatLng(Context context, String placeId){
        LatLng latLng=null;
        HttpURLConnection connection=null;
        StringBuilder jsonResult=new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/details/json?");
            sb.append("placeid=" + placeId);
            sb.append("&key=");
            sb.append(context.getResources().getString(R.string.api_key));
            URL url = new URL(sb.toString());
            connection=(HttpURLConnection)url.openConnection();
            InputStreamReader inputStreamReader=new InputStreamReader(connection.getInputStream());

            int read;
            char[] buff=new char[1024];
            while((read= inputStreamReader.read(buff))!=-1){
                jsonResult.append(buff,0,read);
            }
        }
        catch(MalformedURLException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally{
            if(connection!=null){
                connection.disconnect();
            }
        }
        try{
            JSONObject jsonObject=new JSONObject(jsonResult.toString());
            JSONObject location=jsonObject.getJSONObject("result").getJSONObject("geometry").getJSONObject("location");
            double lat=location.getDouble("lat");
            double lng=location.getDouble("lng");
            latLng=new LatLng(lat,lng);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return latLng;
    }
}
