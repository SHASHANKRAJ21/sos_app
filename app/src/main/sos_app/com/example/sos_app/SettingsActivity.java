package com.example.sos_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private EditText contactNameInput;
    private EditText contactNumberInput;
    private ArrayAdapter<String> adapter;
    private List<String> contactList;
    private static final String PREFS_NAME = "contacts_prefs";
    private static final String CONTACTS_KEY = "contacts_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        contactNameInput = findViewById(R.id.contactNameInput);
        contactNumberInput = findViewById(R.id.contactNumberInput);
        ListView contactListView = findViewById(R.id.contactListView);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> savedContacts = prefs.getStringSet(CONTACTS_KEY, new HashSet<>());

        contactList = new ArrayList<>(savedContacts);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactList);
        contactListView.setAdapter(adapter);

        Button addContactButton = findViewById(R.id.addContactButton);
        addContactButton.setOnClickListener(v -> addContact());

        Button updateContactButton = findViewById(R.id.updateContactButton);
        updateContactButton.setOnClickListener(v -> updateContact());

        Button deleteContactButton = findViewById(R.id.deleteContactButton);
        deleteContactButton.setOnClickListener(v -> deleteContact());

        contactListView.setOnItemClickListener((parent, view, position, id) -> {
            String[] contactDetails = contactList.get(position).split(": ");
            contactNameInput.setText(contactDetails[0]);
            contactNumberInput.setText(contactDetails[1]);
        });
    }

    private void addContact() {
        String name = contactNameInput.getText().toString().trim();
        String number = contactNumberInput.getText().toString().trim();

        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(number)) {
            String newContact = name + ": " + number;
            contactList.add(newContact);
            adapter.notifyDataSetChanged();
            saveContacts();
            contactNameInput.setText("");
            contactNumberInput.setText("");
            Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please enter both name and number", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateContact() {
        String name = contactNameInput.getText().toString().trim();
        String number = contactNumberInput.getText().toString().trim();

        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(number)) {
            String updatedContact = name + ": " + number;
            int index = contactList.indexOf(updatedContact);
            if (index != -1) {
                contactList.set(index, updatedContact);
                adapter.notifyDataSetChanged();
                saveContacts();
                Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Contact not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter both name and number", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteContact() {
        String name = contactNameInput.getText().toString().trim();
        String number = contactNumberInput.getText().toString().trim();

        String contactToDelete = name + ": " + number;
        if (contactList.remove(contactToDelete)) {
            adapter.notifyDataSetChanged();
            saveContacts();
            contactNameInput.setText("");
            contactNumberInput.setText("");
            Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Contact not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveContacts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> contactSet = new HashSet<>(contactList);
        editor.putStringSet(CONTACTS_KEY, contactSet);
        editor.apply();
    }
}
