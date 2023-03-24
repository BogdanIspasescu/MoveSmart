package eu.ase.ro.aplicatielicenta;

import static eu.ase.ro.aplicatielicenta.LoginActivity.REMEMBERED_EMAIL;
import static eu.ase.ro.aplicatielicenta.LoginActivity.REMEMBERED_PASSWORD;
import static eu.ase.ro.aplicatielicenta.LoginActivity.REMEMBER_ME_STATE;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Toast;


import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.ase.ro.aplicatielicenta.httprequest.AsyncTaskRunner;
import eu.ase.ro.aplicatielicenta.httprequest.HttpManager;
import eu.ase.ro.aplicatielicenta.interfaces.Callback;
import eu.ase.ro.aplicatielicenta.placeautocomplete.PlaceAutoSuggestAdapter;
import eu.ase.ro.aplicatielicenta.classes.Review;
import eu.ase.ro.aplicatielicenta.classes.User;
import eu.ase.ro.aplicatielicenta.firebase.FirebaseService;
import eu.ase.ro.aplicatielicenta.utilrouteparser.PolylineDecoder;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityResultLauncher<Intent> loginLauncher;



    private GoogleMap googleMap;
    private LatLng departureCoordinates = new LatLng(0, 0);
    private String departureId;

    private LatLng destinationCoordinates = new LatLng(0, 0);
    private String destionationId;


    private AsyncTaskRunner asyncTaskRunner=new AsyncTaskRunner();


    private Polyline currentPolyline;

    private User current_user;
    private FirebaseService firebaseService;


    //ptr zona de lasat review
    private Button reviewButton;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;

    private Button sendGetRequestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseService = FirebaseService.getFirebaseService();

        loginLauncher = addLoginLauncher();

//        MapFragment mapFragment = new MapFragment();

        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager()
                .beginTransaction().replace(R.id.main_frame_for_maps, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);


        //incepem aplicatia prin a deschide pagina de login, asteptam sa aflam care e user-ul curent
        openLoginScreen();


        //zona textview-uri cu autocomplete pentru locatia de start si cea de finish
        AutoCompleteTextView autoCompleteTextView_start_location = findViewById(R.id.main_tv_autocomplete_start);
        autoCompleteTextView_start_location.setAdapter(new PlaceAutoSuggestAdapter(getApplicationContext(), android.R.layout.simple_list_item_1));

        AutoCompleteTextView autoCompleteTextView_destination = findViewById(R.id.main_tv_autocomplete_destination);
        autoCompleteTextView_destination.setAdapter(new PlaceAutoSuggestAdapter(getApplicationContext(), android.R.layout.simple_list_item_1));

        sendGetRequestButton = findViewById(R.id.main_generateRouteButton);
        sendGetRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //facem url-ul ca sa putem trimite request-ul dupa, pentru a obtine json-ul
                if (inputDataValid(autoCompleteTextView_start_location, autoCompleteTextView_destination) == true) {
                    getRoutingJson(autoCompleteTextView_start_location, autoCompleteTextView_destination);
                }

            }

        });

        reviewButton = findViewById(R.id.main_reviewButton);
        reviewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createReviewDialog(autoCompleteTextView_start_location, autoCompleteTextView_destination);
            }
        });

        //TODO: zona de about aplicatie maybe (cu copyright and all)
    }

    private void getRoutingJson(AutoCompleteTextView autoCompleteTextView_start_location, AutoCompleteTextView autoCompleteTextView_destination) {
        String departure_name = autoCompleteTextView_start_location.getText().toString();
        String formated_departure_name = departure_name;
        formated_departure_name = formated_departure_name.replace(" ", "%20");

        String destination_name = autoCompleteTextView_destination.getText().toString();
        String formated_destination_name = destination_name;
        formated_destination_name = formated_destination_name.replace(" ", "%20");

        StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?origin=");
        urlBuilder = urlBuilder.append(formated_departure_name);
        urlBuilder = urlBuilder.append("&mode=transit&transit_routing_preference=less_walking&destination=");
        urlBuilder = urlBuilder.append(formated_destination_name);
        urlBuilder = urlBuilder.append("&key=");
        urlBuilder = urlBuilder.append(getResources().getString(R.string.api_key));
        String url = urlBuilder.toString();


        RequestQueue requestQueue = Volley.newRequestQueue(this);

        //acest request este ca sa obtin place id_urile
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener() {
            @Override
            public void onResponse(Object object) {
                Object json = null;
                JSONObject response = null;
                try {
                    json = new JSONTokener(object.toString()).nextValue();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (json instanceof JSONObject) {
                    response = (JSONObject) json;
                }
                try {
                    assert response != null;
                    String status = response.getString("status");
                    if (status.equals("OK")) { //AICI DA EROARE (la polyline)
                        //parsam coordonatele aici
                        JSONArray arrayLocations = response.getJSONArray("geocoded_waypoints");
                        JSONObject jsonObjectDeparture = arrayLocations.getJSONObject(0);
                        JSONObject jsonObjectDestination = arrayLocations.getJSONObject(1);
                        departureId = jsonObjectDeparture.getString("place_id");
                        destionationId = jsonObjectDestination.getString("place_id");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        RetryPolicy retryPolicy = new DefaultRetryPolicy(3000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(jsonObjectRequest);


        String apiKey = getResources().getString(R.string.api_key);
        String secondUrl = "https://maps.googleapis.com/maps/api/place/details/json?placeid=" + departureId + "&fields=geometry&key=" + apiKey;

        JsonObjectRequest secondJsonObjectRequest = new JsonObjectRequest(Request.Method.GET, secondUrl, null, new Response.Listener() {
            @Override
            public void onResponse(Object object) {
                Object json = null;
                JSONObject response = null;
                try {
                    json = new JSONTokener(object.toString()).nextValue();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (json instanceof JSONObject) {
                    response = (JSONObject) json;
                }
                try {
                    assert response != null;
                    String status = response.getString("status");
                    if (status.equals("OK")) {
//                            Toast.makeText(getApplicationContext(),status,Toast.LENGTH_SHORT).show();
                        JSONObject result = response.getJSONObject("result");
                        JSONObject geometry = result.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");
                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");
                        departureCoordinates = new LatLng(lat, lng);

//                        Toast.makeText(getApplicationContext(), departureCoordinates.toString(), Toast.LENGTH_SHORT).show();
                    }
                    else{
//                        Toast.makeText(getApplicationContext(),"Code:"+status+","+"Eroare la parse coordonate departure!",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        secondJsonObjectRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(secondJsonObjectRequest);


        String thirdUrl = "https://maps.googleapis.com/maps/api/place/details/json?placeid=" + destionationId + "&fields=geometry&key=" + apiKey;
        JsonObjectRequest thirdJsonObjectRequest = new JsonObjectRequest(Request.Method.GET, thirdUrl, null, new Response.Listener() {
            @Override
            public void onResponse(Object object) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Object json = null;
                JSONObject response = null;
                try {
                    json = new JSONTokener(object.toString()).nextValue();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (json instanceof JSONObject) {
                    response = (JSONObject) json;
                }
                try {
                    assert response != null;
                    String status = response.getString("status");
                    if (status.equals("OK")) {
//                            Toast.makeText(getApplicationContext(),status,Toast.LENGTH_SHORT).show();
                        JSONObject result = response.getJSONObject("result");
                        JSONObject geometry = result.getJSONObject("geometry");
                        JSONObject location = geometry.getJSONObject("location");
                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");
                        destinationCoordinates = new LatLng(lat, lng);
                        Toast.makeText(getApplicationContext(),departureCoordinates.toString(),Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"Code:"+status+","+"Eroare la parse coordonate destination!",Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        thirdJsonObjectRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(thirdJsonObjectRequest);


        //aici o sa parsam polyline-ul
        JsonObjectRequest fourthJsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener() {
            @Override
            public void onResponse(Object object) {
                Object json = null;
                JSONObject response = null;
                try {
                    json = new JSONTokener(object.toString()).nextValue();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (json instanceof JSONObject) {
                    response = (JSONObject) json;
                }
                try {
                    String status = response.getString("status");
                    if (status.equals("OK")) { //AICI DA EROARE (la polyline)
                        //TODO: De reparat drawPolyline(response)
                        if (googleMap == null) {
//                            Toast.makeText(getApplicationContext(),"Nu s-a instantiat obiectul de tip google maps",Toast.LENGTH_SHORT).show();
                        } else {
//                            Toast.makeText(getApplicationContext(),"Google maps instantiat!",Toast.LENGTH_SHORT).show();
                            //drawPolyline(response);
                            drawMarkersAndZoom();
                        }

                        //TODO: de reparat drawMarkersAndZoom();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        fourthJsonObjectRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(fourthJsonObjectRequest);
    }

    private void createReviewDialog(AutoCompleteTextView autoCompleteTextView_start_location, AutoCompleteTextView autoCompleteTextView_destination) {
        View ratingView = View.inflate(getApplicationContext(), R.layout.review_page, null);

        dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setView(ratingView);
        dialog = dialogBuilder.create();

        dialog.setIcon(R.drawable.logo);
        dialog.show();

        RatingBar ratingBar = (RatingBar) ratingView.findViewById(R.id.reviewPage_ratingBar_rating);
        Button btnSendReview = (Button) dialog.findViewById(R.id.reviewPage_btn_sendRating);
        Button btnCancelReview = (Button) dialog.findViewById(R.id.reviewPage_btn_cancelRating);


        assert btnSendReview != null;
        btnSendReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeReviewToDatabase(firebaseService, ratingBar, autoCompleteTextView_start_location, autoCompleteTextView_destination);
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.dialog_thank_you_message), Toast.LENGTH_SHORT).show();
            }
        });

        assert btnCancelReview != null;
        btnCancelReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        loginLauncher.launch(intent);
    }


    private ActivityResultLauncher<Intent> addLoginLauncher() {
        ActivityResultCallback<ActivityResult> callback = getLoginCredentialsActivityResultCallback();
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), callback);
    }


    //callback pentru obtinut credentialele in activitatea LoginActivity sa vedem daca ne putem loga
    private ActivityResultCallback<ActivityResult> getLoginCredentialsActivityResultCallback() {
        return new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    current_user = (User) result.getData().getSerializableExtra(LoginActivity.CURRENT_USER_KEY);
                }
            }
        };
    }


    //FUNCTIA DE SCRIS CE AM PRELUAT DIN INPUTURILE USER-ULUI
    private void writeReviewToDatabase(FirebaseService firebaseService, RatingBar ratingBar, AutoCompleteTextView actv_start, AutoCompleteTextView actv_destination) {
        Review review = new Review();
        review.setRating(ratingBar.getRating());
        review.setStartPoint(actv_start.getText().toString());
        review.setDestinationPoint(actv_destination.getText().toString());

        String userId = current_user.getId();
        firebaseService.insertReview(review, userId);
    }

    @SuppressLint("ResourceType")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_classic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.main_classic_menu_logout) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this, R.style.CustomAlertDialogTheme)
                    .setTitle(getString(R.string.main_logout_dialog_title))
                    .setMessage(getString(R.string.main_logout_dialog_message))
                    .setPositiveButton(getString(R.string.main_logout_positive_button_text), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            current_user = null;
                            SharedPreferences preferences = getSharedPreferences("moveSmartSharedPreferences", MODE_PRIVATE);
                            SharedPreferences.Editor sharedPreferencesEditor = preferences.edit();
                            sharedPreferencesEditor.putBoolean(REMEMBER_ME_STATE, false);
                            sharedPreferencesEditor.putString(REMEMBERED_EMAIL, "");
                            sharedPreferencesEditor.putString(REMEMBERED_PASSWORD, "");
                            sharedPreferencesEditor.apply();
                            dialogInterface.dismiss();
                            openLoginScreen();
                        }
                    })
                    .setNegativeButton(getString(R.string.main_logout_negative_button_text), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
            builder.setIcon(R.drawable.ic_baseline_exit_to_app_24);
            builder.create().show();
        }
        return true;
    }

    private boolean inputDataValid(AutoCompleteTextView autoCompleteTextView_start_location, AutoCompleteTextView autoCompleteTextView_destination) {
        if (autoCompleteTextView_start_location.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_get_route_empty_departure_error_message), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (autoCompleteTextView_destination.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_get_route_empty_destination_error_message), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    //functie gasit locatie dupa adresa data din autocomplete

    private void drawMarkersAndZoom() throws JSONException {

        LatLng coord_departure = departureCoordinates;
        Marker departureMarker=googleMap.addMarker(new MarkerOptions().position(coord_departure).title("Punct de start"));
        LatLng coord_destination = destinationCoordinates;
        googleMap.addMarker(new MarkerOptions().position(coord_destination).title("Punct de sosire"));

//        LatLngBounds bounds = new LatLngBounds.Builder()
//                .include(coord_departure)
//                .include(coord_destination).build();
//        Point point = new Point();
//        getWindowManager().getDefaultDisplay().getSize(point);
//        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, point.x, 50, 30));

        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(departureMarker.getPosition(), 15));
        this.googleMap.animateCamera(CameraUpdateFactory.zoomIn());
        this.googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
    }

    private void drawPolyline(JSONObject response) throws JSONException {
        JSONArray routes = response.getJSONArray("routes");
        ArrayList<LatLng> points;
        PolylineOptions polylineOptions = null;
        for (int i = 0; i < routes.length(); i++) {
            points = new ArrayList<>();
            polylineOptions = new PolylineOptions();
            JSONArray legs = routes.getJSONObject(i).getJSONArray("legs");

            for (int j = 0; j < legs.length(); j++) {
                JSONArray steps = legs.getJSONObject(j).getJSONArray("steps");
                for (int k = 0; k < steps.length(); k++) {
                    String polyline = steps.getJSONObject(k).getJSONObject("polyline").getString("points");
                    List<LatLng> list = PolylineDecoder.decodePoly(polyline);

                    for (int l = 0; l < list.size(); l++) {
                        LatLng position = new LatLng((list.get(l)).latitude, (list.get(l)).longitude);
                        points.add(position);
                    }
                }
            }
            polylineOptions.addAll(points);
            polylineOptions.width(10);
            polylineOptions.color(Color.RED);
            polylineOptions.geodesic(true);
        }
        googleMap.addPolyline(polylineOptions);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        LatLng bucharestLatLng = new LatLng(44.439663, 26.096306);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bucharestLatLng, 15));
        this.googleMap.animateCamera(CameraUpdateFactory.zoomIn());
        this.googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
    }
}
