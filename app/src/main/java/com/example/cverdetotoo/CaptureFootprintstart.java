package com.example.cverdetotoo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CaptureFootprintstart extends AppCompatActivity {

    // Optional: If you want multiple facts, store them in an array
    private String[] facts = {
            "If just 10% more students walked or biked to school...",
            "Riding a bike burns about 600 calories per hour...",
            "Walking 30 minutes a day improves cardiovascular health..."
    };
    private int currentFactIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure activity_battle_eco_start.xml is in res/layout
        setContentView(R.layout.activity_capture_footprintstart);

        // Find views by ID
        View parentLayout = findViewById(R.id.parentLayout1);
        final TextView tvFact = findViewById(R.id.tvFact1);

        // (Optional) Show the first fact immediately
        // tvFact.setText(facts[currentFactIndex]);

        // Set an OnClickListener on the root layout
        parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Optionally show a Toast message
                Toast.makeText(CaptureFootprintstart.this, "Screen tapped!", Toast.LENGTH_SHORT).show();

                // Navigate to NextActivity when the screen is tapped
                Intent intent = new Intent(CaptureFootprintstart.this, Bfast1Fragment.class);
                startActivity(intent);
            }
        });
    }
}
