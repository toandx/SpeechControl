package com.wolfpaulus.android.hotword;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.VIBRATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * The ListeningActivity implements the Pocket-Sphinx's RecognitionListener and moves on the
 * MainActivity, only after the wake word has been recognized.
 * <p>
 * While the Wake Word get read from a resource file, to change it, a new wake word would also need
 * to be added the ./assets/sync/models/lm/words.dic
 * Don't forget to generate a new MD5 hash for dictionary after you modified it, and store it in
 * ./assets/sync/models/lm/words.dic.md5 (E.g. use http://passwordsgenerator.net/md5-hash-generator/
 *
 * @author <a mailto="wolf@wolfpaulus.com">Wolf Paulus</a>
 */
public class ListeningActivity extends AppCompatActivity implements RecognitionListener {
    private static final String LOG_TAG = ListeningActivity.class.getName();
    private static final String WAKEWORD_SEARCH = "WAKEWORD_SEARCH";
    private static final int PERMISSIONS_REQUEST_CODE = 5;
    private static int sensibility = 25;
    private SpeechRecognizer mRecognizer;
    private Vibrator mVibrator;
    public EditText editText1;
    public ImageButton action;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    public Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listening);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        final TextView threshold = (TextView) findViewById(R.id.threshold);
        editText1= (EditText) findViewById(R.id.editText1);
        action=(ImageButton) findViewById(R.id.btnSpeak);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVoiceInput();
            }
        });
        threshold.setText(String.valueOf(sensibility));
        final SeekBar seekbar = (SeekBar) findViewById(R.id.seekbar);
        seekbar.setProgress(sensibility);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                threshold.setText(String.valueOf(progress));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // intentionally empty
            }

            public void onStopTrackingTouch(final SeekBar seekBar) {
                sensibility = seekBar.getProgress();
                Log.i(LOG_TAG, "Changing Recognition Threshold to " + sensibility);
                threshold.setText(String.valueOf(sensibility));
                mRecognizer.removeListener(ListeningActivity.this);
                mRecognizer.stop();
                mRecognizer.shutdown();
                setup();
            }
        });

        ActivityCompat.requestPermissions(ListeningActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, VIBRATE}, PERMISSIONS_REQUEST_CODE);
    }

    public void openFacebookApp() {
        intent=this.getPackageManager().getLaunchIntentForPackage("com.facebook.katana");
        if (intent==null)
            intent=this.getPackageManager().getLaunchIntentForPackage("com.facebook.lite");
        if (intent!=null)
            startActivity(intent); else
            Toast.makeText(this,"Không tìm thấy app",Toast.LENGTH_LONG).show();
    }
    public void openMessengerApp()
    {
        intent=this.getPackageManager().getLaunchIntentForPackage("com.facebook.orca");
        if (intent==null)
            intent=this.getPackageManager().getLaunchIntentForPackage("com.facebook.mlite");
        if (intent!=null)
            startActivity(intent); else
            Toast.makeText(this,"Không tìm thấy app",Toast.LENGTH_LONG).show();
    }
    public void openYoutubeApp()
    {
        intent=this.getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
        if (intent!=null)
            startActivity(intent); else
            Toast.makeText(this,"Không tìm thấy app",Toast.LENGTH_LONG).show();

    }
    public void openChromeApp()
    {
        intent=this.getPackageManager().getLaunchIntentForPackage("com.android.chrome");
        if (intent!=null)
            startActivity(intent); else
            Toast.makeText(this,"Không tìm thấy app",Toast.LENGTH_LONG).show();
    }
    public void openMusicApp()
    {
        intent=this.getPackageManager().getLaunchIntentForPackage("com.zing.mp3");
        if (intent!=null)
            startActivity(intent); else
            Toast.makeText(this,"Không tìm thấy app Zing mp3",Toast.LENGTH_LONG).show();
    }
    public void checkMail()
    {
        intent=this.getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
        if (intent!=null)
            startActivity(intent); else
            Toast.makeText(this,"Không tìm thấy app",Toast.LENGTH_LONG).show();
    }
    public void openMap()
    {
        intent=this.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
        if (intent!=null)
            startActivity(intent); else
            Toast.makeText(this,"Không tìm thấy app",Toast.LENGTH_LONG).show();
    }

    public void makeCallFor(String contact) {
        contact=contact.trim();
        String num;
        Cursor phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, "lower("+ContactsContract.Contacts.DISPLAY_NAME + ")='" + contact + "'",
                null, null);
        if (phone.moveToFirst()) {
            num = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));
            makeCall(num);
        } else
            Toast.makeText(this,"Không tìm thấy liên hệ",Toast.LENGTH_LONG).show();
    }
    public void makeCall(String tel)
    {
        tel=tel.replaceAll(" ","");
        editText1.setText(tel);
        //intent=new Intent(Intent.ACTION_DIAL);
        intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:"+tel));
        startActivity(intent);
    }
    public void GGsearch(String term)
    {
        try {
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, term);
            startActivity(intent);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    public void setAlarm(String text)
    {
        String h=text.split(":")[0];
        String m=text.split(":")[1];
        int hour=Integer.parseInt(h);
        int minutes=Integer.parseInt(m);
        intent=new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_MESSAGE,"Alarm");
        intent.putExtra(AlarmClock.EXTRA_HOUR,hour);
        intent.putExtra(AlarmClock.EXTRA_MINUTES,minutes);
        startActivity(intent);
    }
    public void test(View view)
    {
        Uri gmmIntentUri=Uri.parse("geo:0,0?q=Ha Noi");
        Intent mapIntent=new Intent(Intent.ACTION_VIEW,gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        this.startActivity(mapIntent);
    }
    public void searchMap(String location)
    {
        Uri gmmIntentUri=Uri.parse("geo:0,0?q="+location);
        Intent mapIntent=new Intent(Intent.ACTION_VIEW,gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        this.startActivity(mapIntent);
    }
    public void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hello, How can I help you?");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {

        }
    }

    public void textProcess(String text)
    {
        editText1.setText(text+" "+Integer.toString(text.length()));
        if (text.equals("mở facebook")) openFacebookApp(); else
        if (text.equals("mở tin nhắn")) openMessengerApp(); else
        if (text.equals("mở video")) openYoutubeApp(); else
        if (text.equals("mở trình duyệt")) openChromeApp(); else
        if (text.equals("mở nhạc")) openMusicApp(); else
        if (text.equals("check mail")) checkMail(); else
        if (text.equals("mở bản đồ")) openMap(); else
        if (text.length()>=8){
            if (text.substring(0, 6).equals("gọi số")) makeCall(text.substring(6));
            else if (text.substring(0, 7).equals("gọi cho")) makeCallFor(text.substring(7)); else
            if (text.length()>=17 && text.substring(0,16).equals("đặt báo thức lúc")) setAlarm(text.substring(17)); else
            if (text.length()>=12 && text.substring(0,13).equals("tìm đường tới")) searchMap(text.substring(13)); else

                GGsearch(text);
        } else GGsearch(text);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    textProcess(result.get(0).toLowerCase());
                }
                break;
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode,
            @NonNull final String[] permissions,
            @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (0 < grantResults.length && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio recording permissions denied.", Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setup();
    }

    /**
     * Stop the recognizer.
     * Since cancel() does trigger an onResult() call,
     * we cancel the recognizer rather then stopping it.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mRecognizer != null) {
            mRecognizer.removeListener(this);
            mRecognizer.cancel();
            mRecognizer.shutdown();
            Log.d(LOG_TAG, "PocketSphinx Recognizer was shutdown");
        }
    }

    /**
     * Setup the Recognizer with a sensitivity value in the range [1..100]
     * Where 1 means no false alarms but many true matches might be missed.
     * and 100 most of the words will be correctly detected, but you will have many false alarms.
     */
    private void setup() {
        try {
            final Assets assets = new Assets(ListeningActivity.this);
            final File assetDir = assets.syncAssets();
            mRecognizer = SpeechRecognizerSetup.defaultSetup()
                    .setAcousticModel(new File(assetDir, "models/en-us-ptm"))
                    .setDictionary(new File(assetDir, "models/lm/words.dic"))
                    .setKeywordThreshold(Float.valueOf("1.e-" + 2 * sensibility))
                    .getRecognizer();
            mRecognizer.addKeyphraseSearch(WAKEWORD_SEARCH, getString(R.string.wake_word));
            mRecognizer.addListener(this);
            mRecognizer.startListening(WAKEWORD_SEARCH);
            Log.d(LOG_TAG, "... listening");
        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    //
    // RecognitionListener Implementation
    //

    @Override
    public void onBeginningOfSpeech() {
        Log.d(LOG_TAG, "Beginning Of Speech");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle("~ ~ ~");
        }
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(LOG_TAG, "End Of Speech");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle("");
        }
    }

    @Override
    public void onPartialResult(final Hypothesis hypothesis) {
        if (hypothesis != null) {
            final String text = hypothesis.getHypstr();
            Log.d(LOG_TAG, "on partial: " + text);
            if (text.equals(getString(R.string.wake_word))) {
                mVibrator.vibrate(100);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle("");
                }
                startVoiceInput();
                //startActivity(new Intent(this, MainActivity.class));
            }
        }
    }

    @Override
    public void onResult(final Hypothesis hypothesis) {
        if (hypothesis != null) {
            Log.d(LOG_TAG, "on Result: " + hypothesis.getHypstr() + " : " + hypothesis.getBestScore());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle("");
            }
        }
    }

    @Override
    public void onError(final Exception e) {
        Log.e(LOG_TAG, "on Error: " + e);
    }

    @Override
    public void onTimeout() {
        Log.d(LOG_TAG, "on Timeout");
    }
}