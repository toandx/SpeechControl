package uet.fit.toandx.speechcontrol;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class MainActivity extends AppCompatActivity implements RecognitionListener {
    private static final String LOG_TAG = "toandz";
    private static final String WAKEWORD_SEARCH = "WAKEWORD_SEARCH";
    private static final int PERMISSIONS_REQUEST_CODE = 5;
    private static int sensibility = 50;
    private static String[] X=new String[14];
    private static int[] y=new int[14];
    private SpeechRecognizer mRecognizer;
    private Vibrator mVibrator;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    public EditText editText1;
    public Intent intent;
    public ImageButton action;
    private void initVariable()
    {
        X[0]="mạng xã hội facebook";
        X[1]="tin nhắn messenger";
        X[2]="video youtube";
        X[3]="trình duyệt google chrome";
        X[4]="nghe nhạc zingmp3 mp3";
        X[5]="check kiểm tra mail gmail hộp thư";
        X[6]="bản đồ google maps";
        X[7]="cuộc gọi số";
        X[8]="đánh đặt báo thức lúc";
        X[9]="tìm chỉ đường tới";
        X[10]="truy cập trang web website địa chỉ";
        X[11]="tìm vị trí";
        X[12]="chụp ảnh";
        X[13]="thời tiết";
        y[0]=0;
        y[1]=1;
        y[2]=2;
        y[3]=3;
        y[4]=4;
        y[5]=5;
        y[6]=6;
        y[7]=7;
        y[8]=8;
        y[9]=9;
        y[10]=10;
        y[11]=11;
        y[12]=12;
        y[13]=13;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG,"init");
        initVariable();
        editText1= (EditText) findViewById(R.id.editText1);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        action=(ImageButton) findViewById(R.id.btnSpeak);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startVoiceInput();
            }
        });
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, VIBRATE}, PERMISSIONS_REQUEST_CODE);
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
            final Assets assets = new Assets(MainActivity.this);
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
        Log.d(LOG_TAG,"Open Messenger app");
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
    public void suftWeb(String urlString)
    {
        urlString="https://"+urlString;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.chrome");
        try {
            this.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            // Chrome browser presumably not installed so allow user to choose instead
            intent.setPackage(null);
            this.startActivity(intent);
        }
    }
    public void navigation(String pos)
    {
        Uri gmmIntentUri = Uri.parse("google.navigation:q="+pos);
        //Uri gmmIntentUri=Uri.parse("geo:0,0?q="+location);
        Intent mapIntent=new Intent(Intent.ACTION_VIEW,gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        this.startActivity(mapIntent);
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
    public void takeCam()
    {
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        this.startActivity(intent);
    }
    public void startTimer(String message, String time) {
        String[] list=time.split(":");
        int seconds=0;
        for(int i=0;i<list.length;++i) seconds=seconds*60+Integer.parseInt(list[i]);
        Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_MESSAGE, message)
                .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
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
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Xin chào, tôi có thể giúp gì bạn?");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {

        }
    }
    public void LCS(String s1)
    {
        Log.d(LOG_TAG,"Use LCS");
        String[] xau1=s1.split(" ");
        String[] xau2;
        int[][] f=new int[101][101];
        int maxScore=0;
        int res=-1;
        int lastWord,temp;
        temp=0;
        lastWord=0;
        for(int t=0;t<X.length;++t)
        {
            xau2=X[t].split(" ");
            f[0][0]=0;f[0][1]=0;f[1][0]=0;
            for(int i=1;i<=xau1.length;++i)
                for(int j=1;j<=xau2.length;++j)
                {
                    if (xau1[i-1].equals(xau2[j-1]))
                    {f[i][j]=f[i-1][j-1]+1; temp=i;} else
                        f[i][j]=Math.max(f[i-1][j],f[i][j-1]);
                }
            Log.d(LOG_TAG,Integer.toString(t)+" "+Integer.toString(f[xau1.length][xau2.length]));
            if (maxScore<f[xau1.length][xau2.length])
            {
                maxScore=f[xau1.length][xau2.length];
                res=y[t];
                lastWord=temp;
            }
        }
        String intent;
        if (lastWord<xau1.length) intent=xau1[lastWord]; else intent="";
        for(int i=lastWord+1;i<xau1.length;++i) intent=intent+" "+xau1[i];
        switch (res)
        {
            case -1:
                GGsearch(s1);
                break;
            case 0:openFacebookApp();break;
            case 1:openMessengerApp();break;
            case 2:openYoutubeApp(); break;
            case 3:openChromeApp(); break;
            case 4:openMusicApp(); break;
            case 5:checkMail(); break;
            case 6:openMap(); break;
            case 7:
                try{
                temp=Integer.parseInt(intent);
                makeCall(intent);
                } catch (Exception e)
                {
                    makeCallFor(intent);
                }
                break;
            case 8:setAlarm(intent); break;
            case 9:navigation(intent);break;
            case 10:suftWeb(intent);break;
            case 11:searchMap(intent); break;
            case 12:takeCam();break;
            case 13:GGsearch("thời tiết"); break;
            default: GGsearch(s1); break;
        }
        return;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    //textProcess(result.get(0).toLowerCase());
                    LCS(result.get(0).toLowerCase());
                }
                break;
            }

        }
    }
}