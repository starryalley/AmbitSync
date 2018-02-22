package idv.markkuo.ambitsync;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public class MoveInfoActivity extends Activity {
    private final String TAG = "AmbitMoveInfo";

    // in the gridview
    private TextView moveState, moveDateTime, moveDuration, moveAscent, moveDescent, moveAscentTime,
            moveDescentTime, moveRecoveryTime, moveSpeed, moveSpeedText, moveAltMax,
            moveAltMin, moveHR, moveHRText, movePTE, moveType, moveTemp, moveDistance,
            moveCalories, moveCadenceText, moveCadence;

    // below gridview
    private Button buttonOpen;
    private TextView movePath;
    // others
    private File gpxDir;
    private String filename;

    private void getViewHandles() {
        moveState = (TextView) findViewById(R.id.moveState);
        moveDateTime = (TextView) findViewById(R.id.moveDateTime);
        moveDuration = (TextView) findViewById(R.id.moveDuration);
        moveAscent = (TextView) findViewById(R.id.moveAscent);
        moveDescent = (TextView) findViewById(R.id.moveDescent);
        moveAscentTime = (TextView) findViewById(R.id.moveAscentTime);
        moveDescentTime = (TextView) findViewById(R.id.moveDescentTime);
        moveRecoveryTime = (TextView) findViewById(R.id.moveRecoveryTime);
        moveSpeed = (TextView) findViewById(R.id.moveSpeed);
        moveSpeedText = (TextView) findViewById(R.id.moveSpeedText);
        moveAltMax = (TextView) findViewById(R.id.moveAltMax);
        moveAltMin = (TextView) findViewById(R.id.moveAltMin);
        moveHR = (TextView) findViewById(R.id.moveHR);
        moveHRText = (TextView) findViewById(R.id.moveHRText);
        movePTE = (TextView) findViewById(R.id.movePTE);
        moveType = (TextView) findViewById(R.id.moveType);
        moveTemp = (TextView) findViewById(R.id.moveTemp);
        moveDistance = (TextView) findViewById(R.id.moveDistance);
        moveCalories = (TextView) findViewById(R.id.moveCalories);
        moveCadenceText = (TextView) findViewById(R.id.moveCadenceText);
        moveCadence = (TextView) findViewById(R.id.moveCadence);

        buttonOpen = (Button) findViewById(R.id.buttonOpenGPX);

        movePath = (TextView) findViewById(R.id.moveFilePath);
    }

    private void setupViews() {
        boolean isDownloaded = getIntent().getBooleanExtra("moveDownloaded", false);

        // fill those textviews with intent extra
        if (isDownloaded)
            moveState.setText("(GPX downloaded)");
        else
            moveState.setText("");
        moveDateTime.setText(getIntent().getStringExtra("moveDateTime"));
        moveDuration.setText(getIntent().getStringExtra("moveDuration"));
        moveAscent.setText(getIntent().getStringExtra("moveAscent"));
        moveDescent.setText(getIntent().getStringExtra("moveDescent"));
        moveAscentTime.setText(getIntent().getStringExtra("moveAscentTime"));
        moveDescentTime.setText(getIntent().getStringExtra("moveDescentTime"));
        moveRecoveryTime.setText(getIntent().getStringExtra("moveRecoveryTime"));
        moveSpeed.setText(getIntent().getStringExtra("moveSpeed"));
        moveSpeedText.setText(moveSpeedText.getText() + " " + getIntent().getStringExtra("moveSpeedMax"));
        moveAltMax.setText(getIntent().getStringExtra("moveAltMax"));
        moveAltMin.setText(getIntent().getStringExtra("moveAltMin"));
        moveHR.setText(getIntent().getStringExtra("moveHR"));
        moveHRText.setText(moveHRText.getText() + " " + getIntent().getStringExtra("moveHRRange"));
        movePTE.setText(getIntent().getStringExtra("movePTE"));
        moveType.setText(getIntent().getStringExtra("moveType"));
        moveTemp.setText(getIntent().getStringExtra("moveTemp"));
        moveDistance.setText(getIntent().getStringExtra("moveDistance"));
        moveCalories.setText(getIntent().getStringExtra("moveCalories"));
        moveCadenceText.setText(moveCadenceText.getText() + " " + getIntent().getStringExtra("moveCadenceMax"));
        moveCadence.setText(getIntent().getStringExtra("moveCadence"));

        gpxDir = (File) getIntent().getExtras().get("gpxDir");
        filename = getIntent().getStringExtra("moveFileName");

        if (isDownloaded) {
            movePath.setText(filename);
            buttonOpen.setVisibility(View.VISIBLE);
        } else {
            movePath.setText("");
            buttonOpen.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_move);

        getViewHandles();
        setupViews();

        // set button handlers
        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "open gpx at:" + gpxDir + " file:" + filename);
                if (gpxDir != null) {
                    File file = new File(gpxDir, filename);
                    if (file.exists()) {
                        // open the gpx file by other app
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = FileProvider.getUriForFile(MoveInfoActivity.this,
                                BuildConfig.APPLICATION_ID + ".provider", file);
                        intent.setDataAndType(uri,
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension("gpx"));
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException ex) {
                            Toast.makeText(getApplicationContext(), "No app to open GPX file",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };
        });

    }
}
