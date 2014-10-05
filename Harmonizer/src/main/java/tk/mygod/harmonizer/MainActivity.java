package tk.mygod.harmonizer;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private double savedFrequency;
    private AudioTrack savedTrack, muteTrack;
    private boolean pressed;

    public MainActivity() {
    }

    private static short generate(double frequency, int i) {
        return (short) Math.round(32767 * Math.cos(3.14159265358979323846264338327950288 / 24000 * frequency * i));
    }
    private static AudioTrack generateTrack(double frequency)
    {
        int max = frequency <= 0 ? 2 : (int) (48000 / frequency), i = 0;
        short last = 0;
        if (max < 16) max = 16;
        if (max > 2880000) max = 2880000;
        ArrayList<Short> arrayList = new ArrayList<Short>(max);
        while (i < max) arrayList.add(last = generate(frequency, i++));
        while (i < 524288 && last < 32767) arrayList.add(last = generate(frequency, i++));  // 1M
        while (i < 2880000 && last < 32760) arrayList.add(last = generate(frequency, i++)); // NO MORE THAN 60s
        int k = i * 3 / 4, s = 0;
        short[] samples = new short[--i];
        for (int j = k; j < i; ++j) samples[s++] = arrayList.get(j);
        for (int j = 0; j < k; ++j) samples[s++] = arrayList.get(j);
        arrayList.clear();
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_MONO,
                                          AudioFormat.ENCODING_PCM_16BIT, i << 1, AudioTrack.MODE_STATIC);
        track.write(samples, 0, i);
        track.setLoopPoints(0, i, -1);
        return track;
    }

    private void stop() {
        pressed = false;
        if (savedTrack != null) savedTrack.pause();
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        muteTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_MONO,
                                   AudioFormat.ENCODING_PCM_16BIT, 32, AudioTrack.MODE_STATIC);
        muteTrack.write(new short[16], 0, 16);
        muteTrack.setLoopPoints(0, 16, -1);
        findViewById(R.id.beep_button).setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                switch (motionevent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressed = true;
                        (new Thread(new Runnable() {
                            private double frequency;

                            public void run() {
                                try {
                                    frequency = Double.parseDouble(((EditText) findViewById(R.id.frequencyText))
                                            .getText().toString());
                                } catch (Exception exc) {
                                    frequency = 1000;
                                }
                                if (savedFrequency != frequency) {
                                    savedFrequency = frequency;
                                    if (savedTrack != null) {
                                        savedTrack.stop();
                                        savedTrack.release();
                                        savedTrack = null;
                                    }
                                    savedTrack = generateTrack(frequency);
                                }
                                if (savedTrack != null && pressed) savedTrack.play();
                            }
                        })).start();
                        return true;
                    case MotionEvent.ACTION_UP:
                        stop();
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        muteTrack.play();   // prevent start_output_stream delay
    }

    @Override
    protected void onPause() {
        super.onPause();
        muteTrack.pause();
        stop();
    }
}