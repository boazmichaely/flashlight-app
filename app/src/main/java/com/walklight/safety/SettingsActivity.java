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

        MaterialButton close = findViewById(R.id.buttonCloseSettings);
        if (close != null) close.setOnClickListener(v -> finish());
    }
}


