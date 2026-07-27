package com.camerax10;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(12, 8, 12, 12);

        addSection(layout, "CameraX10 Help Guide", null, true);

        addSection(layout, "Taking Photos",
                "Tap the shutter button to capture a photo. "
                + "The camera will auto-focus before capturing. "
                + "Tap on the preview to manually focus on a specific area.");

        addSection(layout, "Recording Video",
                "Switch to video mode using the mode toggle button. "
                + "Tap the record button to start/stop recording. "
                + "Video is saved as 3GP format.");

        addSection(layout, "Flash Modes",
                "Tap the flash button to cycle through modes:\n"
                + "- Auto: flash fires when needed\n"
                + "- On: flash always fires\n"
                + "- Off: flash disabled\n"
                + "- Torch: continuous light");

        addSection(layout, "Focus Modes",
                "Tap the focus button to cycle through:\n"
                + "- Auto: focuses when capturing\n"
                + "- Macro: for close-up shots\n"
                + "- Infinity: for distant subjects\n"
                + "- Fixed: no auto-focus");

        addSection(layout, "Zoom",
                "Use the zoom button or volume keys (if configured) "
                + "to zoom in and out.");

        addSection(layout, "Grid Lines",
                "Enable grid overlays in Settings to help "
                + "with composition: Rule of Thirds, 4x4 Grid, "
                + "Crosshair, or Golden Ratio.");

        addSection(layout, "Volume Keys",
                "Configure volume key behavior in Settings:\n"
                + "- None: normal volume control\n"
                + "- Capture: press to take photo\n"
                + "- Zoom: press to zoom in/out");

        addSection(layout, "Camera Sound",
                "Choose between App Sounds (synthesized tones) "
                + "or System Sounds (device default sounds) "
                + "in Settings.");

        addSection(layout, "Picture Size & Quality",
                "Tap the picture size button to cycle through "
                + "available resolutions. JPEG quality can be "
                + "adjusted in Settings (70-100%).");

        addSection(layout, "Scene Modes",
                "Tap the scene mode button to switch between "
                + "modes like Auto, Night, Portrait, Landscape, "
                + "and more (varies by device).");

        addSection(layout, "White Balance & Effects",
                "Adjust white balance and color effects in "
                + "Settings for different lighting conditions "
                + "and creative filters.");

        addSection(layout, "GPS Location",
                "Photos are geotagged with your location "
                + "when GPS is available. A GPS indicator "
                + "appears on the screen.");

        Button backBtn = new Button(this);
        backBtn.setText("Back");
        backBtn.setTextColor(Color.WHITE);
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        backBtn.setBackgroundColor(Color.DKGRAY);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = 8;
        backBtn.setLayoutParams(btnParams);
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        layout.addView(backBtn);

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addSection(LinearLayout parent, String title, String body) {
        addSection(parent, title, body, false);
    }

    private void addSection(LinearLayout parent, String title, String body, boolean isHeader) {
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        if (isHeader) {
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            titleView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            p.bottomMargin = 8;
            titleView.setLayoutParams(p);
        } else {
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            p.topMargin = 6;
            titleView.setLayoutParams(p);
        }
        parent.addView(titleView);

        if (body != null) {
            TextView bodyView = new TextView(this);
            bodyView.setText(body);
            bodyView.setTextColor(Color.LTGRAY);
            bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.topMargin = 2;
            bp.bottomMargin = 4;
            bodyView.setLayoutParams(bp);
            parent.addView(bodyView);
        }
    }
}
