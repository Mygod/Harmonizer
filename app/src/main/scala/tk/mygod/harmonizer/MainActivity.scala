package tk.mygod.harmonizer

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import tk.mygod.app.FragmentStackActivity
import tk.mygod.view.LocationObserver

final class MainActivity extends FragmentStackActivity {
  lazy val mainFragment = new MainFragment
  private lazy val settingsFragment = new SettingsFragment
  private lazy val favoritesFragment = new FavoritesFragment

  private def hideInput = getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
    .hideSoftInputFromWindow(getCurrentFocus.getWindowToken, 0)

  protected override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    push(mainFragment)
  }

  def showSettings {
    hideInput
    settingsFragment.setSpawnLocation(LocationObserver.getOnScreen(findViewById(R.id.settings)))
    push(settingsFragment)
  }

  def showFavorites {
    hideInput
    favoritesFragment.setSpawnLocation(LocationObserver.getOnScreen(findViewById(R.id.favorites)))
    push(favoritesFragment)
  }
}
