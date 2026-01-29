package com.example.apktask;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private LinearLayout zoneEnCours;
    private LinearLayout zoneTerminees;
    private LinearLayout zoneAnnulees;
    private Button btnValider;
    private Button btnReset;
    private ArrayList<Task> listeTaches;
    private boolean isEnregistree = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        zoneEnCours = findViewById(R.id.zone_en_cours);
        zoneTerminees = findViewById(R.id.zone_terminees);
        zoneAnnulees = findViewById(R.id.zone_annulees);
        btnValider = findViewById(R.id.button_valider);
        btnReset = findViewById(R.id.button_reset);
        listeTaches = new ArrayList<>();

        chargerTaches();
        chargerEtat();
        afficherTaches();
        ajouterTache();

        btnValider.setOnClickListener(v -> enregistrerTaches());
        btnReset.setOnClickListener(v -> reinitialiserTout());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void ajouterTache() {
        if (isEnregistree) {
            btnValider.setEnabled(false);
            return;
        }

        View nouvelleLigne = LayoutInflater.from(this)
                .inflate(R.layout.item_task, zoneEnCours, false);

        Button btnAjouter = nouvelleLigne.findViewById(R.id.btn_task_add);
        EditText editText = nouvelleLigne.findViewById(R.id.edittext_task_input);
        Button btnSupprimer = nouvelleLigne.findViewById(R.id.btn_task_delete);

        btnAjouter.setOnClickListener(v -> {
            String texte = editText.getText().toString();
            String boutonTexte = btnAjouter.getText().toString();

            if (boutonTexte.equals("Ajouter")) {
                if (!texte.isEmpty()) {
                    Task nouvelleTache = new Task(
                            listeTaches.size() + 1,
                            texte,
                            false,
                            System.currentTimeMillis()
                    );
                    listeTaches.add(nouvelleTache);
                    sauvegarderTaches();
                    nouvelleLigne.setTag(nouvelleTache.id);

                    editText.setEnabled(false);
                    btnAjouter.setText("Modifier");
                    ajouterTache();
                }
            }
            else if (boutonTexte.equals("Modifier")) {
                editText.setEnabled(true);
                btnAjouter.setText("Valider");
            }
            else if (boutonTexte.equals("Valider")) {
                if (!texte.isEmpty()) {
                    int taskId = (Integer) nouvelleLigne.getTag();
                    for (Task t : listeTaches) {
                        if (t.id == taskId) {
                            t.setTitle(texte);
                            break;
                        }
                    }
                    sauvegarderTaches();
                    editText.setEnabled(false);
                    btnAjouter.setText("Modifier");
                }
            }
        });

        btnSupprimer.setOnClickListener(v -> {
            String texte = editText.getText().toString();

            if (texte.isEmpty()) {
                System.out.println("Impossible de supprimer une tâche vide !");
                return;
            }

            if (!editText.isEnabled()) {
                int taskId = (Integer) nouvelleLigne.getTag();
                for (int i = 0; i < listeTaches.size(); i++) {
                    Task t = listeTaches.get(i);
                    if (t.id == taskId) {
                        listeTaches.remove(i);
                        sauvegarderTaches();
                        break;
                    }
                }
                zoneEnCours.removeView(nouvelleLigne);
            }
        });

        zoneEnCours.addView(nouvelleLigne);
    }

    private void afficherTaches() {
        zoneEnCours.removeAllViews();
        zoneTerminees.removeAllViews();
        zoneAnnulees.removeAllViews();

        for (Task t : listeTaches) {
            View nouvelleLigne = LayoutInflater.from(this)
                    .inflate(R.layout.item_task, null, false);

            EditText editText = nouvelleLigne.findViewById(R.id.edittext_task_input);
            Button btnAjouter = nouvelleLigne.findViewById(R.id.btn_task_add);
            Button btnSupprimer = nouvelleLigne.findViewById(R.id.btn_task_delete);

            editText.setTextColor(getColor(android.R.color.system_primary_dark));

            editText.setText(t.getTitle());
            editText.setEnabled(false);
            nouvelleLigne.setTag(t.id);

            if (t.status == 0) {
                btnAjouter.setText("Modifier");
                btnSupprimer.setText("Supprimer");
                editText.setTextColor(Color.BLACK);

                btnAjouter.setOnClickListener(v -> {
                    String texte = editText.getText().toString();
                    String boutonTexte = btnAjouter.getText().toString();

                    if (boutonTexte.equals("Modifier")) {
                        editText.setEnabled(true);
                        btnAjouter.setText("Valider");
                    }
                    else if (boutonTexte.equals("Valider")) {
                        if (!texte.isEmpty()) {
                            int taskId = (Integer) nouvelleLigne.getTag();
                            for (Task task : listeTaches) {
                                if (task.id == taskId) {
                                    task.setTitle(texte);
                                    break;
                                }
                            }
                            sauvegarderTaches();
                            editText.setEnabled(false);
                            btnAjouter.setText("Modifier");
                        }
                    }
                });

                btnSupprimer.setOnClickListener(v -> {
                    int taskId = (Integer) nouvelleLigne.getTag();
                    for (int i = 0; i < listeTaches.size(); i++) {
                        if (listeTaches.get(i).id == taskId) {
                            listeTaches.remove(i);
                            sauvegarderTaches();
                            break;
                        }
                    }
                    zoneEnCours.removeView(nouvelleLigne);
                });

                zoneEnCours.addView(nouvelleLigne);
            }
            else if (t.status == 1) {
                // ENREGISTRÉE (pas encore terminée ni annulée)
                btnAjouter.setText("Terminé");
                btnSupprimer.setText("Annuler");  // ← Le bouton "Annuler" réapparaît

                editText.setEnabled(false);

                btnAjouter.setOnClickListener(v -> {
                    int taskId = (Integer) nouvelleLigne.getTag();
                    for (Task task : listeTaches) {
                        if (task.id == taskId) {
                            task.setStatus(2);  // Passer en "Terminée" (2)
                            break;
                        }
                    }
                    sauvegarderTaches();
                    afficherTaches();
                });

                btnSupprimer.setOnClickListener(v -> {
                    int taskId = (Integer) nouvelleLigne.getTag();
                    for (Task task : listeTaches) {
                        if (task.id == taskId) {
                            task.setStatus(3);  // Passer en "Annulée" (3)
                            break;
                        }
                    }
                    sauvegarderTaches();
                    afficherTaches();
                });

                zoneEnCours.addView(nouvelleLigne);
            }

            else if (t.status == 2) {
                btnAjouter.setVisibility(View.GONE);
                btnSupprimer.setVisibility(View.GONE);
                editText.setTextColor(Color.GREEN);
                editText.setEnabled(false);

                zoneTerminees.addView(nouvelleLigne);
            }
            else if (t.status == 3) {
                btnAjouter.setVisibility(View.GONE);
                btnSupprimer.setVisibility(View.GONE);
                editText.setTextColor(Color.RED);
                editText.setEnabled(false);

                zoneAnnulees.addView(nouvelleLigne);
            }
        }
    }

    private void enregistrerTaches() {
        for (Task t : listeTaches) {
            if (t.status == 0) {
                t.setStatus(1);
            }
        }
        sauvegarderTaches();
        sauvegarderEtat();
        isEnregistree = true;
        btnValider.setVisibility(View.GONE);
        btnReset.setVisibility(View.VISIBLE);
        afficherTaches();
    }

    private void reinitialiserTout() {
        listeTaches.clear();
        sauvegarderTaches();

        isEnregistree = false;
        sauvegarderEtat();

        btnValider.setVisibility(View.VISIBLE);
        btnReset.setVisibility(View.GONE);

        afficherTaches();
        ajouterTache();

        System.out.println("Tâches réinitialisées !");
    }

    private void sauvegarderTaches() {
        SharedPreferences prefs = getSharedPreferences("tasks", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < listeTaches.size(); i++) {
            Task t = listeTaches.get(i);
            json.append("{\"id\":").append(t.getId())
                    .append(",\"title\":\"").append(t.getTitle())
                    .append("\",\"isDone\":").append(t.getIsDone())
                    .append(",\"createAt\":").append(t.getCreateAt())
                    .append(",\"status\":").append(t.getStatus()).append("}");

            if (i < listeTaches.size() - 1) json.append(",");
        }
        json.append("]");

        editor.putString("taches", json.toString());
        editor.apply();
        System.out.println("Tâches sauvegardées !");
    }

    private void chargerTaches() {
        SharedPreferences prefs = getSharedPreferences("tasks", MODE_PRIVATE);
        String json = prefs.getString("taches", "[]");

        listeTaches.clear();

        if (json == null || json.equals("[]") || json.isEmpty()) {
            System.out.println("Pas de tâches sauvegardées");
            return;
        }

        try {
            json = json.substring(1, json.length() - 1);
            String[] tasks = json.split("\\},\\{");

            for (String taskJson : tasks) {
                taskJson = taskJson.replaceAll("[\\{\\}]", "");

                int idStart = taskJson.indexOf("\"id\":") + 5;
                int idEnd = taskJson.indexOf(",", idStart);
                int id = Integer.parseInt(taskJson.substring(idStart, idEnd).trim());

                int titleStart = taskJson.indexOf("\"title\":\"") + 9;
                int titleEnd = taskJson.indexOf("\"", titleStart);
                String title = taskJson.substring(titleStart, titleEnd);

                int isDoneStart = taskJson.indexOf("\"isDone\":") + 9;
                int isDoneEnd = taskJson.indexOf(",", isDoneStart);
                boolean isDone = Boolean.parseBoolean(taskJson.substring(isDoneStart, isDoneEnd).trim());

                int createAtStart = taskJson.indexOf("\"createAt\":") + 11;
                int createAtEnd = taskJson.indexOf(",", createAtStart);
                long createAt = Long.parseLong(taskJson.substring(createAtStart, createAtEnd).trim());

                int statusStart = taskJson.indexOf("\"status\":") + 9;
                int statusEnd = taskJson.length();
                int status = Integer.parseInt(taskJson.substring(statusStart, statusEnd).trim());

                Task t = new Task(id, title, isDone, createAt);
                t.setStatus(status);
                listeTaches.add(t);

                System.out.println("Tâche chargée : " + title + " (status=" + status + ")");
            }
        } catch (Exception e) {
            System.out.println("Erreur parsing JSON : " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Total tâches chargées : " + listeTaches.size());
    }

    private void sauvegarderEtat() {
        SharedPreferences prefs = getSharedPreferences("tasks", MODE_PRIVATE);
        prefs.edit().putBoolean("isEnregistree", isEnregistree).apply();
    }

    private void chargerEtat() {
        SharedPreferences prefs = getSharedPreferences("tasks", MODE_PRIVATE);
        isEnregistree = prefs.getBoolean("isEnregistree", false);
    }
}
