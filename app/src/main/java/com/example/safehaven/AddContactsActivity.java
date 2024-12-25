package com.example.safehaven;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AddContactsActivity extends AppCompatActivity {

    private EditText contact1, contact2, contact3, contact4;
    private Button editContactButton;
    private Button saveContactButton;
    private FirebaseAuth auth;
    private String userID;
    private DatabaseReference contactsReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contacts);

        // Initialize UI components
        contact1 = findViewById(R.id.EtContact1);
        contact2 = findViewById(R.id.EtContact2);
        contact3 = findViewById(R.id.EtContact3);
        contact4 = findViewById(R.id.EtContact4);
        editContactButton = findViewById(R.id.BtnEditContacts);
        saveContactButton = findViewById(R.id.BtnSaveContacts);

        auth = FirebaseAuth.getInstance();
        userID = auth.getCurrentUser().getUid();
        contactsReference = FirebaseDatabase.getInstance().getReference("Users")
                .child(userID).child("Contacts");

        // Load contacts from Firebase
        loadContacts();

        editContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableEditing(true);
            }
        });

        saveContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContacts();
            }
        });
    }

    private void enableEditing(boolean enable) {
        contact1.setEnabled(enable);
        contact2.setEnabled(enable);
        contact3.setEnabled(enable);
        contact4.setEnabled(enable);
        saveContactButton.setVisibility(enable
                ? View.VISIBLE
                : View.GONE);
    }
    private void loadContacts() {
        contactsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Contacts contacts = snapshot.getValue(Contacts.class);
                if (contacts != null) {
                    contact1.setText(contacts.contact1);
                    contact2.setText(contacts.contact2);
                    contact3.setText(contacts.contact3);
                    contact4.setText(contacts.contact4);
                }
                enableEditing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddContactsActivity.this,
                        "Failed to load contacts",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveContacts() {
        String contact1Text = contact1.getText().toString().trim();
        String contact2Text = contact2.getText().toString().trim();
        String contact3Text = contact3.getText().toString().trim();
        String contact4Text = contact4.getText().toString().trim();

        // Validate input
        if (contact1Text.isEmpty() && contact2Text.isEmpty()
                && contact3Text.isEmpty() && contact4Text.isEmpty()) {
            Toast.makeText(this,
                    "Please enter at least one contact",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Contacts updatedContacts = new Contacts(contact1Text, contact2Text, contact3Text, contact4Text);
        contactsReference.setValue(updatedContacts)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        Toast.makeText(AddContactsActivity.this,
                                "Contacts updated successfully",
                                Toast.LENGTH_SHORT).show();
                        enableEditing(false);
                    } else {
                        Toast.makeText(AddContactsActivity.this,
                                "Failed to update contacts!",
                                Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(AddContactsActivity.this,
                            "An error occurred: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });

    }
}