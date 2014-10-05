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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.util.ArrayList;
import java.util.LinkedList;

public class MainActivity extends ActionBarActivity
        implements ListView.OnItemClickListener, ListView.OnItemLongClickListener {
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
    private static class FavoritesAdapter extends ArrayAdapter<FavoriteItem> {
        public LinkedList<FavoriteItem> Favorites;
        private SharedPreferences pref;

        private FavoritesAdapter(Context context, LinkedList<FavoriteItem> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
            Favorites = objects;
        }

        public static FavoritesAdapter createAdapter(Context context) {
            LinkedList<FavoriteItem> favorites = new LinkedList<FavoriteItem>();
            FavoritesAdapter result = new FavoritesAdapter(context, favorites);
            int size = (result.pref = context.getSharedPreferences("favorites", MODE_PRIVATE)).getInt("size", 0);
            for (int i = 0; i < size; ++i) favorites.add(new FavoriteItem(result.pref.getString(i + "_name", ""),
                    Double.longBitsToDouble(result.pref.getLong(i + "_freq", 0))));
            return result;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = LayoutInflater.from(getContext())
                                                        .inflate(android.R.layout.simple_list_item_1, parent, false);
            FavoriteItem item = getItem(position);
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(item.getFullName());
            convertView.setTag(item.Frequency);
            return convertView;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            int oldSize = pref.getInt("size", 0), size = 0;
            SharedPreferences.Editor editor = pref.edit();
            for (FavoriteItem favorite : Favorites) {
                editor.putString(size + "_name", favorite.Name);
                editor.putLong(size++ + "_freq", Double.doubleToLongBits(favorite.Frequency));
            }
            editor.putInt("size", size);
            while (size < oldSize) {
                editor.remove(size + "_name");
                editor.remove(size++ + "_freq");
            }
            if (Build.VERSION.SDK_INT >= 9) editor.apply();
            else editor.commit();
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

    private static String betterToString(double value) {
        return value == Math.floor(value) ? String.format("%.0f", value) : Double.toString(value);
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
        ArrayList<Short> arrayList = new ArrayList<Short>(max);
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
        (drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout))
                .setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        drawerLayout.setDrawerListener(drawerToggle = new ActionBarDrawerToggle
                (this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(R.string.app_name);
                addFavoriteMenu.setVisible(false);
            }

            @Override
            public void onDrawerOpened(View view) {
                super.onDrawerOpened(view);
                getSupportActionBar().setTitle(R.string.favorites);
                addFavoriteMenu.setVisible(true);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        ListView favoriteList = (ListView) findViewById(R.id.favorite);
        favoriteList.setEmptyView(findViewById(android.R.id.empty));
        favoriteList.setAdapter(favoritesAdapter = FavoritesAdapter.createAdapter(this));
        favoriteList.setOnItemClickListener(this);
        favoriteList.setOnItemLongClickListener(this);
        registerForContextMenu(favoriteList);
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
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.favorites, menu);
        (addFavoriteMenu = menu.findItem(R.id.add_to_favorites))
                .setVisible(drawerLayout.isDrawerOpen(GravityCompat.START));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) return true;
        if (item.getItemId() != R.id.add_to_favorites) return super.onOptionsItemSelected(item);
        final EditText text = new EditText(this);
        new AlertDialog.Builder(this).setTitle(R.string.add_favorite_dialog_title).setView(text)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        favoritesAdapter.add(new FavoriteItem(text.getText().toString(), getFrequency()));
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
        text.requestFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        return true;
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object tag = view.getTag();
        if (tag instanceof Double) frequencyText.setText(betterToString((Double) tag));
        drawerLayout.closeDrawers();
    }

    /**
     * Callback method to be invoked when an item in this view has been
     * clicked and held.
     * <p/>
     * Implementers can call getItemAtPosition(position) if they need to access
     * the data associated with the selected item.
     *
     * @param parent   The AbsListView where the click happened
     * @param view     The view within the AbsListView that was clicked
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     * @return true if the callback consumed the long click, false otherwise
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.favorite_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        FavoriteItem favorite = favoritesAdapter
                .getItem(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.share:
                startActivity(Intent.createChooser(new Intent().setAction(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.share_content),
                                favorite.getFullName())), getString(R.string.share_title)));
                return true;
            case R.id.remove:
                favoritesAdapter.remove(favorite);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}