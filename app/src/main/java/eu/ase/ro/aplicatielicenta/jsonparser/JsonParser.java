package eu.ase.ro.aplicatielicenta.jsonparser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonParser {
    public static String parseLocationIdFromJson(String json) throws JSONException{
            JSONObject jsonObject = new JSONObject(json);
            JSONArray predictionsArray = jsonObject.getJSONArray("predictions");
            JSONObject predictionObject = predictionsArray.getJSONObject(0);
            return predictionObject.getString("place_id");
        }
    public static String convertStringForRequest(String stringToConvert) {
        return stringToConvert.replace(" ", "+");
    }
}
