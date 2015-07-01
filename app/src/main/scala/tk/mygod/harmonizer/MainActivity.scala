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
    if (mainFragment == null) {
      mainFragment = new MainFragment()
      push(mainFragment)
    }
  }

  def showSettings {
    if (settingsFragment == null) settingsFragment = new SettingsFragment
    settingsFragment.setSpawnLocation(LocationObserver.getOnScreen(findViewById(R.id.settings)))
    push(settingsFragment)
  }

  def showFavorites {
    if (favoritesFragment == null) favoritesFragment = new FavoritesFragment
    favoritesFragment.setSpawnLocation(LocationObserver.getOnScreen(findViewById(R.id.favorites)))
    push(favoritesFragment)
  }
}
