package eu.ase.ro.aplicatielicenta.firebase;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import eu.ase.ro.aplicatielicenta.account.Review;
import eu.ase.ro.aplicatielicenta.account.User;
import eu.ase.ro.aplicatielicenta.interfaces.Callback;

public class FirebaseService {

    private static final String USER_REFERENCE = "users";

    private final DatabaseReference reference;

    private static FirebaseService firebaseService;

    private FirebaseService() {
        reference = FirebaseDatabase.getInstance().getReference(USER_REFERENCE);
    }

    public static FirebaseService getFirebaseService() {
        if (firebaseService == null) {
            synchronized (FirebaseService.class) {
                if (firebaseService == null) {
                    firebaseService = new FirebaseService();
                }
            }
        }
        return firebaseService;
    }


    public void insertUser(User user) {
        if (user == null || (user.getId() != null && !user.getId().trim().isEmpty())) {
            return;
        }
        String id = reference.push().getKey(); //generam cheia de acces ptr user
        user.setId(id); //o setam
        reference.child(user.getId()).setValue(user); //scriem userul
    }

public void insertReview(Review review, String userId){
        DatabaseReference reviewsReference=reference.child(userId).child("reviews");
        String id=reviewsReference.push().getKey();
        review.setId(id);
        reviewsReference.child(review.getId()).setValue(review);
}

    public void getUserByEmail(String email, Callback<User> callback) {
        Query query = reference.orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        callback.runResultOnUiThread(user);
                        return;
                    }
                }
                callback.runResultOnUiThread(null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.runResultOnUiThread(null);
            }
        });
    }


    public void checkUserExistence(String email, String password, Callback<Boolean> callback) {
        Query query = reference.orderByChild("email").equalTo(email);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        if (user.getPassword().equals(password)) {
                            callback.runResultOnUiThread(true);
                            return;
                        }
                    }
                }
                callback.runResultOnUiThread(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.runResultOnUiThread(false);
            }
        });
    }

    public void getUserLastNameByEmail(String email, Callback<String> callback) {
        Query query = reference.orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        User user = userSnapshot.getValue(User.class);
                        String lastName = user.getLastName();
                        callback.runResultOnUiThread(lastName);
                        return;
                    }
                }
                callback.runResultOnUiThread(null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.runResultOnUiThread(null);
            }
        });
    }


}
