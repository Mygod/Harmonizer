package tk.mygod.harmonizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private double savedFrequency;
    private AudioTrack savedTrack;
    private boolean pressed;

    public MainActivity() {
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        findViewById(R.id.beep_button).setOnTouchListener(new android.view.View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                switch (motionevent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressed = true;
                        (new Thread(new Runnable() {
                            private double frequency;
                            private int i;
                            private short last;

                            private short generate() {
                                return last = (short) Math.round(32767 * Math.sin((3.14159265358979323846264338327950288
                                        * frequency * (double) i++) / 22050));
                            }

                            public void run() {
                                try {
                                    frequency = Double.parseDouble(((EditText) findViewById(R.id.frequencyText))
                                            .getText().toString());
                                } catch (NumberFormatException exc) {
                                    frequency = 1000;
                                }
                                if (savedFrequency != frequency) {
                                    savedFrequency = frequency;
                                    if (savedTrack != null) {
                                        savedTrack.stop();
                                        savedTrack.release();
                                        savedTrack = null;
                                    }
                                    ArrayList arraylist = new ArrayList((int) (44100D / frequency));
                                    int max = (int) (44100D / frequency);
                                    i = 0;
                                    while (i < max) arraylist.add(Short.valueOf(generate()));
                                    while (last != 0) arraylist.add(Short.valueOf(generate()));
                                    short[] samples = new short[--i];
                                    for (int j = 0; j < i; ++j) samples[j] = (Short) arraylist.get(j);
                                    arraylist.clear();
                                    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                                            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, i << 1,
                                            AudioTrack.MODE_STATIC);
                                    track.write(samples, 0, i);
                                    track.setLoopPoints(0, i, -1);
                                    savedTrack = track;
                                }
                                if (pressed) savedTrack.play();
                            }
                        })).start();
                        return true;
                    case MotionEvent.ACTION_UP:
                        pressed = false;
                        if (savedTrack != null) savedTrack.pause();
                        return true;
                }
                return false;
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.action_check_for_updates:
                final ProgressDialog dialog = ProgressDialog.show(this, getString(R.string.please_wait),
                        getString(R.string.checking_for_updates), true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BufferedReader reader = null;
                        String url = null;
                        Exception exc = null;
                        try {
                            reader = new BufferedReader(new InputStreamReader(new URL("http://mygod.tk/product/update/"
                                    + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode + '/')
                                    .openStream()));
                            url = reader.readLine();
                        } catch (Exception e) {
                            exc = e;
                        } finally {
                            try {
                                if (reader != null) reader.close();
                            } catch (Exception e) { }
                        }
                        dialog.dismiss();
                        final String urlCopy = url;
                        final Exception excCopy = exc;
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(excCopy == null
                                                ? urlCopy == null || urlCopy.isEmpty()
                                                ? R.string.no_update_available : R.string.update_available
                                                : R.string.check_for_updates_failed);
                                if (urlCopy != null && !urlCopy.isEmpty()) {
                                    builder.setNegativeButton(R.string.go_to_download_page,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlCopy)));
                                                }
                                            });
                                }
                                builder.setNeutralButton(R.string.go_to_product_page,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                startActivity(new Intent(Intent.ACTION_VIEW,
                                                        Uri.parse("http://mygod.tk/product/mobile-tablet-switcher/")));
                                            }
                                        }).setPositiveButton(R.string.close, null).show();
                            }
                        });
                    }
                }).start();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}