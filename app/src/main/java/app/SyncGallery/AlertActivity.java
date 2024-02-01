package app.SyncGallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlertActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_notifications);

        TextView hoCapito = findViewById(R.id.hoCapito);
        hoCapito.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Nascondi il bottone
                hoCapito.setVisibility(View.GONE);

                // Passa alla MainActivity
                startActivity(new Intent(AlertActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}