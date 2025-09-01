package com.walklight.safety;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Ensure bottom bar sits above nav bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.View actions = findViewById(R.id.settingsActionsBar);
            if (actions != null) {
                android.view.ViewGroup.MarginLayoutParams p = (android.view.ViewGroup.MarginLayoutParams) actions.getLayoutParams();
                p.bottomMargin = Math.max(p.bottomMargin, sys.bottom);
                actions.setLayoutParams(p);
            }
            return insets;
        });

        // Setup Close button
        MaterialButton close = findViewById(R.id.buttonCloseSettings);
        if (close != null) close.setOnClickListener(v -> finish());
        
        // Setup Reset button
        MaterialButton reset = findViewById(R.id.buttonResetSettings);
        if (reset != null) {
            android.util.Log.d("SettingsActivity", "✅ Reset button found successfully");
            reset.setOnClickListener(v -> {
                android.util.Log.d("SettingsActivity", "🔄 Reset button clicked");
                // Get the settings fragment and trigger reset
                SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings_container);
                if (fragment != null) {
                    fragment.triggerResetDialog();
                } else {
                    android.util.Log.w("SettingsActivity", "Could not find SettingsFragment to trigger reset");
                }
            });
        } else {
            android.util.Log.e("SettingsActivity", "❌ Reset button NOT FOUND! Check layout.");
        }
    }
}


