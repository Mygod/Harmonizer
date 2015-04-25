package tk.mygod.harmonizer

import android.os.Bundle
import tk.mygod.app.FragmentStackActivity
import tk.mygod.view.LocationObserver

final class MainActivity extends FragmentStackActivity {
  lazy val mainFragment = new MainFragment
  private lazy val settingsFragment = new SettingsFragment
  private lazy val favoritesFragment = new FavoritesFragment

  protected override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    push(mainFragment)
  }

  def showSettings {
    settingsFragment.setSpawnLocation(LocationObserver.getOnScreen(findViewById(R.id.settings)))
    push(settingsFragment)
  }

  def showFavorites {
    favoritesFragment.setSpawnLocation(LocationObserver.getOnScreen(findViewById(R.id.favorites)))
    push(favoritesFragment)
  }
}
