package com.example.apktask;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private LinearLayout zoneEnCours;
    private int compteurTaches = 0;
    private ArrayList<Task> listeTaches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        zoneEnCours = findViewById(R.id.zone_en_cours);
        listeTaches = new ArrayList<>();

        ajouterTache();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }

        private void ajouterTache() {
            View nouvelleLigne = LayoutInflater.from(this)
                    .inflate(R.layout.item_task, zoneEnCours, false);

            Button btnAjouter = nouvelleLigne.findViewById(R.id.btn_task_add);
            EditText editText = nouvelleLigne.findViewById(R.id.edittext_task_input);
            Button btnSupprimer = nouvelleLigne.findViewById(R.id.btn_task_delete);

            btnAjouter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String texte = editText.getText().toString();
                    if (!texte.isEmpty()) {
                        System.out.println("Tâche : " + texte);

                        Task nouvelleTache = new Task (
                                listeTaches.size() + 1,
                                texte,
                                false,
                                System.currentTimeMillis()
                        );
                        listeTaches.add(nouvelleTache);

                        nouvelleLigne.setTag(nouvelleTache.id);

                        editText.setEnabled(false);
                        btnAjouter.setText("Modifier");

                        ajouterTache();

                    } else {
                        System.out.println("Champ vide !");
                    }
                }
            });

            btnSupprimer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String texte = editText.getText().toString();

                    // 🔴 SI C'EST VIDE, ON NE FAIT RIEN
                    if (texte.isEmpty()) {
                        System.out.println("Impossible de supprimer une tâche vide !");
                        return;  // ← STOP, on sort de la fonction
                    }

                    // SINON, on supprime normalement
                    if (!editText.isEnabled()) {
                        int taskId = (Integer) nouvelleLigne.getTag();
                        for (int i = 0; i < listeTaches.size(); i++) {
                            Task t = listeTaches.get(i);
                            if (t.id == taskId) {
                                listeTaches.remove(i);
                                break;
                            }
                        }
                    }
                    zoneEnCours.removeView(nouvelleLigne);
                }
            });

            zoneEnCours.addView(nouvelleLigne);
        }
}