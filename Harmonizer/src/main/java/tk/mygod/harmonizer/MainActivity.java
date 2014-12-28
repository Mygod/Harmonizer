package tk.mygod.harmonizer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity {
    private static String betterToString(double value) {
        return value == Math.floor(value) ? String.format("%.0f", value) : Double.toString(value);
    }

    private static class FavoriteItem {
        public FavoriteItem(String name, double frequency) {
            Name = name;
            Frequency = frequency;
        }

        public String Name;
        public double Frequency;

        public String getFullName() {
            return String.format("%s (%s Hz)", Name, betterToString(Frequency));
        }
    }
    private class FavoriteItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private View view;
        private TextView text;
        private FavoriteItem item;

        public FavoriteItemViewHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(this);
            registerForContextMenu(view = itemView);
        }

        public void bind(FavoriteItem item) {
            text.setText((this.item = item).getFullName());
            view.setTag(item);
        }

        @Override
        public void onClick(View v) {
            frequencyText.setText(betterToString(item.Frequency));
            if (drawerLayout != null) drawerLayout.closeDrawers();
        }
    }
    private class FavoritesAdapter extends RecyclerView.Adapter<FavoriteItemViewHolder> {
        private ArrayList<FavoriteItem> favorites;
        private SharedPreferences pref;
        private View empty;

        public FavoritesAdapter() {
            favorites = new ArrayList<>();
            empty = findViewById(android.R.id.empty);
            int size = (pref = getSharedPreferences("favorites", MODE_PRIVATE)).getInt("size", 0);
            if (size == 0) return;
            empty.setVisibility(View.GONE);
            for (int i = 0; i < size; ++i) favorites.add(new FavoriteItem(pref.getString(i + "_name", ""),
                    Double.longBitsToDouble(pref.getLong(i + "_freq", 0))));
        }

        @Override
        public FavoriteItemViewHolder onCreateViewHolder(ViewGroup vg, int i) {
            return new FavoriteItemViewHolder(LayoutInflater.from(vg.getContext())
                    .inflate(android.R.layout.simple_list_item_1, vg, false));
        }

        @Override
        public void onBindViewHolder(FavoriteItemViewHolder vh, int i) {
            vh.bind(favorites.get(i));
        }

        @Override
        public int getItemCount() {
            return favorites.size();
        }

        public void add(FavoriteItem item) {
            int pos = favorites.size();
            favorites.add(item);
            update();
            empty.setVisibility(View.GONE);
            notifyItemInserted(pos);
        }

        public void remove(FavoriteItem item) {
            int pos = favorites.indexOf(item);
            favorites.remove(pos);
            update();
            notifyItemRemoved(pos);
            if (favorites.size() == 0) empty.setVisibility(View.VISIBLE);
        }

        public void update() {
            int oldSize = pref.getInt("size", 0), size = 0;
            SharedPreferences.Editor editor = pref.edit();
            for (FavoriteItem favorite : favorites) {
                editor.putString(size + "_name", favorite.Name);
                editor.putLong(size++ + "_freq", Double.doubleToLongBits(favorite.Frequency));
            }
            editor.putInt("size", size);
            while (size < oldSize) {
                editor.remove(size + "_name");
                editor.remove(size++ + "_freq");
            }
            editor.apply();
        }
    }

    private double savedFrequency;
    private AudioTrack savedTrack, muteTrack;
    private boolean pressed;
    private EditText frequencyText;
    private FavoritesAdapter favoritesAdapter;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private MenuItem addFavoriteMenu;
    private FavoriteItem selectedItem;

    private void hideInput(TextView text) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(text.getWindowToken(), 0);
    }

    private double getFrequency() {
        try {
            return Double.parseDouble(frequencyText.getText().toString());
        } catch (Exception exc) {
            return 1000;
        }
    }

    private static AudioTrack generateTrack(double frequency)
    {
        int max = frequency <= 0 ? 2 : (int) (48000 / frequency), i = 0;
        short last = 0;
        if (max < 16) max = 16;
        if (max > 2880000) max = 2880000;
        ArrayList<Short> arrayList = new ArrayList<>(max);
        double k = 3.14159265358979323846264338327950288 / 24000 * frequency;
        while (i < max) arrayList.add((short) Math.round(32767 * Math.cos(k * i++)));
        while (i < 524288 && last < 32767) arrayList.add(last = (short) Math.round(32767 * Math.cos(k * i++))); // 1M
        // NO MORE THAN 60s
        while (i < 2880000 && last < 32760) arrayList.add(last = (short) Math.round(32767 * Math.cos(k * i++)));
        int delta = i * 3 / 4, s = 0;
        short[] samples = new short[--i];
        for (int j = delta; j < i; ++j) samples[s++] = arrayList.get(j);
        for (int j = 0; j < delta; ++j) samples[s++] = arrayList.get(j);
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

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        muteTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_MONO,
                                   AudioFormat.ENCODING_PCM_16BIT, 32, AudioTrack.MODE_STATIC);
        muteTrack.write(new short[16], 0, 16);
        muteTrack.setLoopPoints(0, 16, -1);
        frequencyText = (EditText) findViewById(R.id.frequency_text);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (drawerLayout != null) {
            drawerLayout.setDrawerListener(drawerToggle = new ActionBarDrawerToggle
                    (this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
                @Override
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    toolbar.setTitle(getText(R.string.app_name));
                    addFavoriteMenu.setVisible(false);
                }

                @Override
                public void onDrawerOpened(View view) {
                    super.onDrawerOpened(view);
                    toolbar.setTitle(getText(R.string.favorites));
                    addFavoriteMenu.setVisible(true);
                    hideInput(frequencyText);
                }
            });
        }
        RecyclerView favoriteList = (RecyclerView) findViewById(R.id.favorite);
        favoriteList.setLayoutManager(new LinearLayoutManager(this));
        favoriteList.setItemAnimator(new DefaultItemAnimator());
        favoriteList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        favoriteList.setAdapter(favoritesAdapter = new FavoritesAdapter());
        findViewById(R.id.beep_button).setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                switch (motionevent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressed = true;
                        (new Thread() {
                            public void run() {
                                double frequency = getFrequency();
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
                        }).start();
                        break;
                    case MotionEvent.ACTION_UP:
                        stop();
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (drawerToggle != null) drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (drawerToggle != null) drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.favorites, menu);
        (addFavoriteMenu = menu.findItem(R.id.add_to_favorites))
                .setVisible(drawerLayout == null || drawerLayout.isDrawerOpen(GravityCompat.START));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) return true;
        if (item.getItemId() != R.id.add_to_favorites) return super.onOptionsItemSelected(item);
        final EditText text = new EditText(this);
        new AlertDialog.Builder(this).setTitle(R.string.add_favorite_dialog_title).setView(text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        favoritesAdapter.add(new FavoriteItem(text.getText().toString(), getFrequency()));
                        hideInput(text);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
        text.requestFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.favorite_context_menu, menu);
        selectedItem = (FavoriteItem) v.getTag();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                startActivity(Intent.createChooser(new Intent().setAction(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.share_content),
                                selectedItem.getFullName())), getString(R.string.share_title)));
                return true;
            case R.id.remove:
                favoritesAdapter.remove(selectedItem);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}