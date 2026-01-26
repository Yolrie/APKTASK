package com.example.apktask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class MidnightReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // À minuit, on vide les tâches
        SharedPreferences prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();  // Supprime tout
        editor.apply();

        System.out.println("Tâches supprimées à minuit !");
    }
}
