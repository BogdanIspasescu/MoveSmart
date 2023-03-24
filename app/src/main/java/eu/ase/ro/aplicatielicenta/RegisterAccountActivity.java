package eu.ase.ro.aplicatielicenta;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.ase.ro.aplicatielicenta.classes.DateConverter;
import eu.ase.ro.aplicatielicenta.classes.Gender;
import eu.ase.ro.aplicatielicenta.classes.User;
import eu.ase.ro.aplicatielicenta.firebase.FirebaseService;
import eu.ase.ro.aplicatielicenta.interfaces.Callback;

public class RegisterAccountActivity extends Activity {

    private final String USER_TO_ADD = "USER_TO_ADD";
    private FirebaseService firebaseService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        firebaseService=FirebaseService.getFirebaseService();

        //cumva nu afisa asta corect
        ImageView logoImageView = findViewById(R.id.registerPage_companylogo);
        logoImageView.setImageResource(R.drawable.logo);

        TextInputEditText tiet_first_name = findViewById(R.id.registerPage_tiet_first_name);
        TextInputEditText tiet_last_name = findViewById(R.id.registerPage_tiet_last_name);
        TextInputEditText tiet_email = findViewById(R.id.registerPage_tiet_email);
        tiet_email.setSelectAllOnFocus(true);
        TextInputEditText tiet_password = findViewById(R.id.registerPage_tiet_password);
        Spinner spn_gender = findViewById(R.id.registerPage_spn_gender);
        TextInputEditText tiet_repeat_password = findViewById(R.id.registerPage_tiet_repeat_password);


        Button birthdateEditor = findViewById(R.id.registerPage_btn_edit_dateOfBirth);

        birthdateEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(RegisterAccountActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int month, int day) {
                                birthdateEditor.setText(getResources().getString(R.string.birthdate_text, day, month + 1, year));
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });


        FloatingActionButton createAccountFab = findViewById(R.id.registerPage_fab_createAccount);
        createAccountFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRegistrationDataValid(tiet_first_name, tiet_last_name, tiet_email, tiet_password, tiet_repeat_password, birthdateEditor)) {
                    Intent intent = getIntent();
                    User user = new User();
                    user.setFirstName(tiet_first_name.getText().toString().trim());
                    user.setLastName(tiet_last_name.getText().toString().trim());
                    user.setEmail(tiet_email.getText().toString().trim());
                    user.setPassword(tiet_password.getText().toString().trim());
                    if (spn_gender.getSelectedItem().toString().equals(getResources().getStringArray(R.array.genders)[0])) {
                        user.setGender(Gender.MASCULIN);
                    } else {
                        user.setGender(Gender.FEMININ);
                    }
                    user.setBirthdate(DateConverter.fromString(birthdateEditor.getText().toString()));
                    user.setBirthdateString(DateConverter.fromDate(user.getBirthdate()));

                    //verificam daca userul vrea sa faca un cont cu email unic
                    checkEmailAvailability(user.getEmail(), new Callback<Boolean>() {
                        @Override
                        public void runResultOnUiThread(Boolean emailAvailable) {
                            if (emailAvailable) {
                                firebaseService.insertUser(user);
                                intent.putExtra(USER_TO_ADD,user);
                                setResult(RESULT_OK, intent);
                                finish();
                            } else {
                                Toast.makeText(RegisterAccountActivity.this, getResources().getString(R.string.email_already_exists_error_message), Toast.LENGTH_SHORT).show();
                                tiet_email.requestFocus();
                            }
                        }
                    });
                }
            }
        });

        FloatingActionButton goBackFab = findViewById(R.id.registerPage_fab_back);
        goBackFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RegisterAccountActivity.this, R.style.CustomAlertDialogTheme)
                        .setTitle(getString(R.string.dialog_go_back_to_login_title))
                        .setMessage(getString(R.string.dialog_go_back_to_login_message))
                        .setPositiveButton(getString(R.string.dialog_go_back_to_login_positive_button_message), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_go_back_to_login_negative_button_message), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                builder.setIcon(R.drawable.ic_baseline_exit_to_app_24);
                builder.create().show();
            }
        });
    }


    public void checkEmailAvailability(String email, Callback<Boolean> callback) {
        firebaseService.getUserByEmail(email, new Callback<User>() {
            @Override
            public void runResultOnUiThread(User user) {
                boolean emailAvailable = user == null;
                callback.runResultOnUiThread(emailAvailable);
            }
        });
    }

    private boolean isRegistrationDataValid(TextInputEditText tiet_first_name, TextInputEditText tiet_last_name, TextInputEditText tiet_email, TextInputEditText tiet_password, TextInputEditText tiet_repeat_password, Button birthdateEditor) {

        if (validateFirstName(tiet_last_name)) return false;

        if (validateLastName(tiet_first_name)) return false;

        if (validateEmail(tiet_email)) return false;

        if (validatePassword(tiet_password, tiet_repeat_password)) return false;

        if (validateBirthdate(birthdateEditor)) return false;

        return true;
    }

    private boolean validateBirthdate(Button birthdateEditor) {
        if (birthdateEditor.getText().toString().trim().equals(getResources().getString(R.string.register_page_default_birthdate_text))) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_birthdate_empty_error), Toast.LENGTH_SHORT).show();
            return true;
        }

        if (DateConverter.fromString(birthdateEditor.getText().toString()).after(new Date()) ||
                DateConverter.fromString(birthdateEditor.getText().toString()).equals(new Date())) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_birthdate_invalid_errror), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private boolean validatePassword(TextInputEditText tiet_password, TextInputEditText tiet_repeat_password) {
        if (!isPasswordValid(tiet_password, tiet_repeat_password)) {
            return true;
        }
        return false;
    }

    private boolean validateEmail(TextInputEditText tiet_email) {
        if (tiet_email.getText() == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_empty_email_error), Toast.LENGTH_SHORT).show();
        }

        if (!isEmailValid(tiet_email.getText().toString())) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_email_wrong_format_error), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private boolean validateLastName(TextInputEditText tiet_last_name) {
        if (tiet_last_name.getText() == null) {
            Toast.makeText(getApplicationContext(),getString(R.string.register_last_name_empty_error), Toast.LENGTH_SHORT).show();
            return true;
        }
        if (tiet_last_name.getText().toString().trim().length() < 3) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_last_name_too_short_error), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private boolean validateFirstName(TextInputEditText tiet_first_name) {
        if (tiet_first_name.getText() == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_first_name_empty_error), Toast.LENGTH_SHORT).show();
            return true;
        }
        if (tiet_first_name.getText().toString().trim().length() < 3) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_first_name_too_short_error), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private boolean isPasswordValid(TextInputEditText tiet_password, TextInputEditText tiet_repeat_password) {
        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{4,}$";

        Matcher matcher = Pattern.compile(PASSWORD_PATTERN).matcher(tiet_password.getText().toString());

        if (!matcher.matches()) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_password_format_not_respected_error), Toast.LENGTH_LONG).show();
            return false;
        }

        if (tiet_password.getText().toString().trim().length() < 6) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_password_too_short_error), Toast.LENGTH_SHORT).show();
        }

        if (!(tiet_password.getText().toString().trim().equals(tiet_repeat_password.getText().toString().trim()))) {
            Toast.makeText(getApplicationContext(), getString(R.string.register_passwords_dont_match_error), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
