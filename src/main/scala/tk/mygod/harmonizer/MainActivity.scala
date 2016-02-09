package tk.mygod.harmonizer

import android.os.Bundle
import tk.mygod.app.FragmentStackActivity
import tk.mygod.view.LocationObserver

final class MainActivity extends FragmentStackActivity {
  var mainFragment: MainFragment = _
  var settingsFragment: SettingsFragment = _
  var favoritesFragment: FavoritesFragment = _

  protected override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    if (mainFragment == null) push(new MainFragment)
  }

  def showSettings {
    val fragment = if (settingsFragment == null) new SettingsFragment else settingsFragment
    fragment.setSpawnLocation(LocationObserver.getOnScreen(findViewById(R.id.settings)))
    push(fragment)
  }

  def showFavorites {
    val fragment = if (favoritesFragment == null) new FavoritesFragment else favoritesFragment
    fragment.setSpawnLocation(LocationObserver.getOnScreen(findViewById(R.id.favorites)))
    push(fragment)
  }
}
