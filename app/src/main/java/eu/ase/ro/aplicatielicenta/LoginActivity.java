package eu.ase.ro.aplicatielicenta;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import eu.ase.ro.aplicatielicenta.account.User;
import eu.ase.ro.aplicatielicenta.firebase.FirebaseService;
import eu.ase.ro.aplicatielicenta.interfaces.Callback;

public class LoginActivity extends AppCompatActivity {


    FirebaseService firebaseService=FirebaseService.getFirebaseService();

    public static final String REMEMBER_ME_STATE = "REMEMBER_ME_STATE";
    public static final String CURRENT_USER_KEY="CURRENT_USER_KEY";

    public static final String REMEMBERED_EMAIL="REMEMBERED_EMAIL";
    public static final String REMEMBERED_PASSWORD="REMEMBERED_PASSWORD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        SharedPreferences preferences = getSharedPreferences("moveSmartSharedPreferences", MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = preferences.edit();

        TextInputEditText tiet_email = findViewById(R.id.loginPage_tiet_email);
        TextInputEditText tiet_password = findViewById(R.id.loginPage_tiet_password);


        boolean lastRememberMeCheck = preferences.getBoolean(REMEMBER_ME_STATE, false);
        if (lastRememberMeCheck) {
            tiet_email.setText(preferences.getString(REMEMBERED_EMAIL, ""));
            tiet_password.setText(preferences.getString(REMEMBERED_PASSWORD, ""));
            sharedPreferencesEditor.putBoolean(REMEMBER_ME_STATE,false);
            sharedPreferencesEditor.apply();
        }else{
            tiet_email.setText("");
            tiet_password.setText("");
        }

        CheckBox checkBoxRememberMe = findViewById(R.id.loginPage_checkbox_remember_me);

        Button loginPage_btn_login = findViewById(R.id.loginPage_btn_login);



        TextView tvRegister = findViewById(R.id.loginPage_tv_gotoregister);
        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RegisterAccountActivity.class);
                startActivity(intent);
            }
        });

        loginPage_btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseService.checkUserExistence(tiet_email.getText().toString(), tiet_password.getText().toString(), new Callback<Boolean>() {
                    @Override
                    public void runResultOnUiThread(Boolean result) {
                        if(result){
                            sharedPreferencesEditor.putBoolean(REMEMBER_ME_STATE, checkBoxRememberMe.isChecked());
                            if (checkBoxRememberMe.isChecked()) {
                                sharedPreferencesEditor.putString(REMEMBERED_EMAIL, tiet_email.getText().toString());
                                sharedPreferencesEditor.putString(REMEMBERED_PASSWORD, tiet_password.getText().toString());
                                sharedPreferencesEditor.apply();
                            }
                            Intent intent = getIntent();
                            firebaseService.getUserByEmail(tiet_email.getText().toString(), new Callback<User>() {
                                @Override
                                public void runResultOnUiThread(User result) {
                                    if(result!=null){
                                        intent.putExtra(CURRENT_USER_KEY,result);
                                        setResult(RESULT_OK, intent);
                                        finish();
                                    }
                                }
                            });
                        }
                        else{
                            Toast.makeText(LoginActivity.this, getResources().getString(R.string.login_error_message), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}