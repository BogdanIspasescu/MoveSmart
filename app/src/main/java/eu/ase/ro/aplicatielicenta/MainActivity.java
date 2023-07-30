package eu.ase.ro.aplicatielicenta;

import static eu.ase.ro.aplicatielicenta.LoginActivity.REMEMBERED_EMAIL;
import static eu.ase.ro.aplicatielicenta.LoginActivity.REMEMBERED_PASSWORD;
import static eu.ase.ro.aplicatielicenta.LoginActivity.REMEMBER_ME_STATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import eu.ase.ro.aplicatielicenta.account.Review;
import eu.ase.ro.aplicatielicenta.account.User;
import eu.ase.ro.aplicatielicenta.firebase.FirebaseService;
import eu.ase.ro.aplicatielicenta.interfaces.Callback;
import eu.ase.ro.aplicatielicenta.placeautocomplete.PlaceAutoSuggestAdapter;
import eu.ase.ro.aplicatielicenta.transitparse.CustomStepAdapter;
import eu.ase.ro.aplicatielicenta.transitparse.GeneralTransitInfo;
import eu.ase.ro.aplicatielicenta.transitparse.TransitStep;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private ActivityResultLauncher<Intent> loginLauncher;

    private GoogleMap googleMap;
    private LatLng departureCoordinates = new LatLng(0, 0);
    private String departureId;

    private LatLng destinationCoordinates = new LatLng(0, 0);
    private String destionationId;

    private User current_user;
    private FirebaseService firebaseService;

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;

    private AutoCompleteTextView autoCompleteTextViewDeparture;
    private AutoCompleteTextView autoCompleteTextViewDestination;
    private LocationManager locationManager;
    private Button sendGetRequestButton;


    private GeneralTransitInfo generalTransitInfo;
    private List<TransitStep> transitSteps;

    private boolean isNewRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseService = FirebaseService.getFirebaseService();

        loginLauncher = addLoginLauncher();

        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager()
                .beginTransaction().replace(R.id.main_frame_for_maps, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        openLoginScreen();

        AutoCompleteTextView autoCompleteTextView_start_location = findViewById(R.id.main_tv_autocomplete_start);
        autoCompleteTextView_start_location.setAdapter(new PlaceAutoSuggestAdapter(getApplicationContext(), android.R.layout.simple_list_item_1));
        autoCompleteTextView_start_location.setThreshold(1);
        autoCompleteTextViewDeparture = autoCompleteTextView_start_location;

        AutoCompleteTextView autoCompleteTextView_destination = findViewById(R.id.main_tv_autocomplete_destination);
        autoCompleteTextView_destination.setAdapter(new PlaceAutoSuggestAdapter(getApplicationContext(), android.R.layout.simple_list_item_1));
        autoCompleteTextView_destination.setThreshold(1);
        autoCompleteTextViewDestination = autoCompleteTextView_destination;

        Button btn_clear_fields = findViewById(R.id.main_btn_clear_fields);
        btn_clear_fields.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                autoCompleteTextView_start_location.setText("");
                autoCompleteTextView_destination.setText("");
                autoCompleteTextView_start_location.requestFocus();
            }
        });

        sendGetRequestButton = findViewById(R.id.main_generateRouteButton);
        sendGetRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputDataValid(autoCompleteTextView_start_location, autoCompleteTextView_destination)) {
                    getRoutingJson(autoCompleteTextView_start_location, autoCompleteTextView_destination);
                }
            }
        });

        Button showRouteButton = findViewById(R.id.main_btn_show_directions);
        showRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(departureCoordinates.equals(new LatLng(0,0))
                        ||destinationCoordinates.equals(new LatLng(0,0))){
                    Toast.makeText(getApplicationContext(),"Nu poti trasa ruta, datele sunt incomplete!",Toast.LENGTH_SHORT).show();
                }else{
                    View directionsView = View.inflate(getApplicationContext(), R.layout.alert_dialog_directions_custom_view, null);
                    dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    dialogBuilder.setView(directionsView);
                    dialog = dialogBuilder.create();

                    dialog.setIcon(R.drawable.logo);
                    dialog.show();

                    Toast.makeText(getApplicationContext(), getString(R.string.estimated_arrival_time, generalTransitInfo.getArrivalTime()), Toast.LENGTH_SHORT).show();



                    ListView lv_directions = (ListView) dialog.findViewById(R.id.alertdialog_directions_lv_steps);
                    CustomStepAdapter adapter = new CustomStepAdapter(MainActivity.this, R.layout.alert_dialog_transit_item_step_custom_adapter_view, transitSteps, getLayoutInflater());
                    lv_directions.setAdapter(adapter);
                    Button btnClose = (Button) dialog.findViewById(R.id.alertdialog_directions_btn_close);
                    btnClose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                            if (isNewRoute) {
                                createReviewDialog(autoCompleteTextView_start_location, autoCompleteTextView_destination);
                            }
                        }
                    });
                }

            }
        });
    }

    private void getRoutingJson(AutoCompleteTextView autoCompleteTextView_start_location, AutoCompleteTextView autoCompleteTextView_destination) {
        String departure_name = autoCompleteTextView_start_location.getText().toString();
        String formated_departure_name = departure_name;
        formated_departure_name = formated_departure_name.replace(" ", "%20");

        String destination_name = autoCompleteTextView_destination.getText().toString();
        String formated_destination_name = destination_name;
        formated_destination_name = formated_destination_name.replace(" ", "%20");

        googleMap.clear();

        StringBuilder urlBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/directions/json?origin=");
        urlBuilder = urlBuilder.append(formated_departure_name);
        Spinner spn_transit_preference = findViewById(R.id.main_spn_transit_preference);
        if (spn_transit_preference.getSelectedItem().toString().equals("Transferuri mai puține")) {
            urlBuilder = urlBuilder.append("&lanugage=ro&mode=transit&transit_routing_preference=fewer_transfers&destination=");
        } else {
            urlBuilder = urlBuilder.append("&language=ro&mode=transit&transit_routing_preference=less_walking&destination=");
        }
        urlBuilder = urlBuilder.append(formated_destination_name);
        urlBuilder = urlBuilder.append("&key=");
        urlBuilder = urlBuilder.append(getResources().getString(R.string.api_key));
        String url = urlBuilder.toString();


        RequestQueue requestQueue = Volley.newRequestQueue(this);
        RetryPolicy retryPolicy = new DefaultRetryPolicy(3000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

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
                    if (status.equals("OK")) {
                        JSONArray arrayLocations = response.getJSONArray("geocoded_waypoints");
                        JSONObject jsonObjectDeparture = arrayLocations.getJSONObject(0);
                        JSONObject jsonObjectDestination = arrayLocations.getJSONObject(1);
                        departureId = jsonObjectDeparture.getString("place_id");
                        destionationId = jsonObjectDestination.getString("place_id");

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
                                        JSONObject result = response.getJSONObject("result");
                                        JSONObject geometry = result.getJSONObject("geometry");
                                        JSONObject location = geometry.getJSONObject("location");
                                        double lat = location.getDouble("lat");
                                        double lng = location.getDouble("lng");
                                        departureCoordinates = new LatLng(lat, lng);

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
                                                        JSONObject result = response.getJSONObject("result");
                                                        JSONObject geometry = result.getJSONObject("geometry");
                                                        JSONObject location = geometry.getJSONObject("location");
                                                        double lat = location.getDouble("lat");
                                                        double lng = location.getDouble("lng");
                                                        destinationCoordinates = new LatLng(lat, lng);
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
                                                                    if (status.equals("OK")) {
                                                                        if (googleMap == null) {
                                                                            Toast.makeText(getApplicationContext(), "Google maps neinitializat!", Toast.LENGTH_SHORT).show();
                                                                        } else {
                                                                            isNewRoute = true;
                                                                            try {
                                                                                parseDirectionsJSON(response);
                                                                            } catch (Exception e) {
                                                                                Toast.makeText(getApplicationContext(), "Eroare la parsare:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                            }
                                                                            drawMarkersAndZoom();
                                                                            drawPolyline(response);
                                                                        }
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
                                                    } else {
                                                        Toast.makeText(getApplicationContext(), "Code:" + status + "," + "Eroare la parse coordonate destination!", Toast.LENGTH_SHORT).show();
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

                                    } else {
                                        Toast.makeText(getApplicationContext(), "Code:" + status + "," + "Eroare la parse coordonate departure!", Toast.LENGTH_SHORT).show();
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
        jsonObjectRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(jsonObjectRequest);
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
                isNewRoute = false;
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

    private ActivityResultCallback<ActivityResult> getLoginCredentialsActivityResultCallback() {
        return new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    current_user = (User) result.getData().getSerializableExtra(LoginActivity.CURRENT_USER_KEY);
                    firebaseService.getUserLastNameByEmail(current_user.getEmail(), new Callback<String>() {
                        @Override
                        public void runResultOnUiThread(String result) {
                            if (result != null) {
                                getSupportActionBar().setTitle(getString(R.string.actionbar_custom_title, result));
                            } else {
                                Toast.makeText(MainActivity.this, getResources().getString(R.string.user_not_found_error_message), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        };
    }

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
        if (item.getItemId() == R.id.main_classic_set_current_location) {
            if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 100);
            }
            getCurrentLocation();
        }
        if (item.getItemId() == R.id.main_classic_menu_copyright) {
            View copyrightView = View.inflate(getApplicationContext(), R.layout.copyright_page, null);

            dialogBuilder = new AlertDialog.Builder(MainActivity.this);
            dialogBuilder.setView(copyrightView);
            dialogBuilder.setCancelable(true)
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.dismiss();
                        }
                    });
            dialog = dialogBuilder.create();
            dialog.show();

        }
        return true;
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        try {
            locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, MainActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        try {
            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            String address = addresses.get(0).getAddressLine(0);
            autoCompleteTextViewDeparture.setText(address);
            autoCompleteTextViewDestination.requestFocus();

            LatLng currentLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocationLatLng, 15));
            this.googleMap.animateCamera(CameraUpdateFactory.zoomIn());
            this.googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean inputDataValid(AutoCompleteTextView autoCompleteTextView_start_location, AutoCompleteTextView autoCompleteTextView_destination) {
        if (autoCompleteTextView_start_location.getText().toString().isEmpty()||autoCompleteTextView_start_location.getText()==null) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_get_route_empty_departure_error_message), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (autoCompleteTextView_destination.getText().toString().isEmpty()||autoCompleteTextView_destination.getText()==null) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.main_get_route_empty_destination_error_message), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void drawMarkersAndZoom() throws JSONException {

        LatLng coord_departure = departureCoordinates;
        Marker departureMarker = googleMap.addMarker(new MarkerOptions().position(coord_departure).title("Punct de start"));
        LatLng coord_destination = destinationCoordinates;
        googleMap.addMarker(new MarkerOptions().position(coord_destination).title("Punct de sosire"));

        assert departureMarker != null;
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
                    List<LatLng> list = PolyUtil.decode(polyline);

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

    private void parseDirectionsJSON(JSONObject response) throws JSONException {
        transitSteps = new ArrayList<>();

        JSONArray routes = response.getJSONArray("routes");
        JSONObject routeObject = routes.getJSONObject(0);

        String farePrice = routeObject.getJSONObject("fare").getString("text");
        JSONArray legsArray = routeObject.getJSONArray("legs");

        JSONObject leg = legsArray.getJSONObject(0);
        JSONObject departureTimeObject = leg.getJSONObject("departure_time");
        String generalDepartureTimeString = departureTimeObject.getString("text");

        JSONObject arrivalTimeObject = leg.getJSONObject("arrival_time");
        String generalArrivalTimeString = arrivalTimeObject.getString("text");

        String generalDistanceString = leg.getJSONObject("distance").getString("text");

        generalTransitInfo = new GeneralTransitInfo(generalDepartureTimeString, generalArrivalTimeString, generalDistanceString, farePrice);

        JSONArray steps = leg.getJSONArray("steps");
        for (int i = 0; i < steps.length(); i++) {
            String travelMode = steps.getJSONObject(i).getString("travel_mode");
            String htmlInstructionsString = steps.getJSONObject(i).getString("html_instructions");
            String distanceString = steps.getJSONObject(i).getJSONObject("distance").getString("text");

            if (travelMode.equals("TRANSIT")) {
                JSONObject transitDetails = steps.getJSONObject(i).getJSONObject("transit_details");
                JSONObject departureStopObject = transitDetails.getJSONObject("departure_stop");
                String departureStationString = departureStopObject.getString("name");
                String departureTimeString = transitDetails.getJSONObject("departure_time").getString("text");

                JSONObject arrivalStopObject = transitDetails.getJSONObject("arrival_stop");
                String arrivalStationString = arrivalStopObject.getString("name");
                String arrivalTimeString = transitDetails.getJSONObject("arrival_time").getString("text");
                String shortName = transitDetails.getJSONObject("line").getString("short_name");

                TransitStep step = new TransitStep(htmlInstructionsString,
                        distanceString,
                        departureStationString, departureTimeString,
                        arrivalStationString, arrivalTimeString
                        , shortName);
                transitSteps.add(step);
            } else {
                TransitStep step = new TransitStep();
                step.setHtml_instructions(htmlInstructionsString);
                step.setDistance(distanceString);
                transitSteps.add(step);
            }
        }
    }
}
