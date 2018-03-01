package idv.markkuo.ambitsync;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sweetzpot.stravazpot.authenticaton.api.AccessScope;
import com.sweetzpot.stravazpot.authenticaton.api.ApprovalPrompt;
import com.sweetzpot.stravazpot.authenticaton.api.AuthenticationAPI;
import com.sweetzpot.stravazpot.authenticaton.api.StravaLogin;
import com.sweetzpot.stravazpot.authenticaton.model.AppCredentials;
import com.sweetzpot.stravazpot.authenticaton.model.LoginResult;
import com.sweetzpot.stravazpot.authenticaton.model.Token;
import com.sweetzpot.stravazpot.authenticaton.ui.StravaLoginActivity;
import com.sweetzpot.stravazpot.authenticaton.ui.StravaLoginButton;
import com.sweetzpot.stravazpot.common.api.AuthenticationConfig;
import com.sweetzpot.stravazpot.common.api.StravaConfig;
import com.sweetzpot.stravazpot.upload.api.UploadAPI;
import com.sweetzpot.stravazpot.upload.model.DataType;
import com.sweetzpot.stravazpot.upload.model.UploadActivityType;
import com.sweetzpot.stravazpot.upload.model.UploadStatus;

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
    private int moveTypeInt;

    private static final int RQ_LOGIN = 1001;
    private StravaLoginButton stravaButton;
    private AuthenticationConfig stravaConfig;
    private static final int STRAVA_CLIENT_ID = 23675;
    private static final String STRAVA_CLIENT_SECRET = "b832b7743701776d2873265881f2b9a8c9313181";
    private Token stravaToken = null;

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

        stravaButton = (StravaLoginButton) findViewById(R.id.login_button);
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
        moveTypeInt = getIntent().getIntExtra("moveTypeInt", 0);

        if (isDownloaded) {
            movePath.setText(filename);
            buttonOpen.setVisibility(View.VISIBLE);
            stravaButton.setVisibility(View.VISIBLE);
        } else {
            movePath.setText("");
            buttonOpen.setVisibility(View.INVISIBLE);
            stravaButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_move);

        getViewHandles();
        setupViews();

        // strava
        stravaConfig = AuthenticationConfig.create().debug().build();

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

        stravaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (stravaToken == null) {
                    Log.d(TAG, "asking user to login to strava");
                    Intent intent = StravaLogin.withContext(MoveInfoActivity.this)
                            .withClientID(STRAVA_CLIENT_ID)
                            .withRedirectURI("http://localhost")
                            .withApprovalPrompt(ApprovalPrompt.AUTO)
                            .withAccessScope(AccessScope.VIEW_PRIVATE_WRITE)
                            .makeIntent();
                    startActivityForResult(intent, RQ_LOGIN);
                } else {
                    Log.d(TAG, "already has token. Try uploading");

                    new StravaUploadAsyncTask().execute();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RQ_LOGIN && resultCode == RESULT_OK && data != null) {
            final String code = data.getStringExtra(StravaLoginActivity.RESULT_CODE);
            Log.d(TAG, "strava code:" + code);
            // Use code to obtain token

            new Thread() {
                @Override
                public void run() {
                    AuthenticationAPI api = new AuthenticationAPI(stravaConfig);
                    LoginResult result = api.getTokenForApp(AppCredentials.with(STRAVA_CLIENT_ID, STRAVA_CLIENT_SECRET))
                            .withCode(code)
                            .execute();
                    stravaToken = result.getToken();
                    Log.d(TAG, "strava token:" + stravaToken);

                    //TODO: need to store the token in app storage so that it can be used for any future request.
                    //also need to remove it when the StravaUnauthorizedException is thrown because
                    // it means the token is no longer valid and the user must authenticate again.

                    //TODO: if we have the token, show "upload to strava". else we show "connect to strava"
                }
            }.start();

        }
    }

    private class StravaUploadAsyncTask extends AsyncTask<Void, Integer, Integer> {

        UploadActivityType type;

        @Override
        protected void onPreExecute() {
            switch(moveTypeInt) {
                case 0x13: //alpine skiing
                    type = UploadActivityType.ALPINE_SKI;
                    break;
                case 0x15: //crosscountry skiing
                case 0x4d: //ski touring
                    type = UploadActivityType.BACKCOUNTRY_SKI;
                    break;
                case 0x0a: //trekking
                case 0x49: //mountaineering
                    type = UploadActivityType.HIKE;
                    break;
                case 0x45: //ice skating
                    type = UploadActivityType.ICE_SKATE;
                    break;
                case 0x07: //skating
                    type = UploadActivityType.INLINE_SKATE;
                    break;
                case 0x04: //cycling
                case 0x05: //mountain biking
                    type = UploadActivityType.RIDE;
                    break;
                case 0x03: //run
                case 0x51: //trail running
                    type = UploadActivityType.RUN;
                    break;
                case 0x14: //snowboarding
                    type = UploadActivityType.SNOWBOARD;
                    break;
                case 0x52: //openwater swimming
                    type = UploadActivityType.SWIM;
                    break;
                case 0x0b: //walking
                    type = UploadActivityType.WALK;
                    break;
                default:
                    type = UploadActivityType.WORKOUT;
                    break;
            }
        }

        @Override
        protected Integer doInBackground(Void... v) {
            int activity = -1;
            StravaConfig config = StravaConfig.withToken(stravaToken)
                    .debug()
                    .build();

            UploadAPI uploadAPI = new UploadAPI(config);

            try {
                Log.d(TAG, "uploading file:" + new File(gpxDir, filename).getAbsolutePath());
                Log.d(TAG, "set activity type to:" + type.toString());
                UploadStatus uploadStatus = uploadAPI.uploadFile(new File(gpxDir, filename))
                        .withDataType(DataType.GPX)
                        .withActivityType(type)
                        .withName("")
                        .withDescription("")
                        .isPrivate(false)
                        .hasTrainer(false)
                        .isCommute(false)
                        .withExternalID(filename)
                        .execute();

                publishProgress(0);
                Log.d(TAG, "activity id:" + uploadStatus.getActivityID());
                Log.d(TAG, "upload status:" + uploadStatus.getStatus());

                // wait 3 second and check status
                Thread.sleep(3 * 1000);
                publishProgress(1);
                uploadStatus = uploadAPI.checkUploadStatus(uploadStatus.getId())
                        .execute();

                Log.d(TAG, "uploads status:" + uploadStatus.getStatus() + " activity ID:" + uploadStatus.getActivityID());
                activity = uploadStatus.getActivityID();
                // TODO: add a button to open strava activity with link:
                // https://www.strava.com/activities/[activityID]
            } catch (Exception e) {
                Log.w(TAG, "upload failed with exception:" + e);
            }
            return activity;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (progress[0] == 0)
                Toast.makeText(getApplicationContext(), "Upload completed!",
                        Toast.LENGTH_SHORT).show();
            else if (progress[0] == 1)
                Toast.makeText(getApplicationContext(), "Checking upload status...",
                        Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Integer act_id) {

            if (act_id == null || act_id == -1)
                Toast.makeText(getApplicationContext(), "Failed to upload to Strava",
                        Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getApplicationContext(), "Upload successful, Strava activity ID:" + act_id,
                        Toast.LENGTH_LONG).show();
        }
    }

    // for use with StravaZpot API when targeting API < 21
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
