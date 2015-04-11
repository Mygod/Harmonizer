package tk.mygod.harmonizer

import android.os.Bundle
import android.view.View
import tk.mygod.app.FragmentStackActivity
import tk.mygod.view.LocationObserver

final class MainActivity extends FragmentStackActivity {
  private lazy val settingsFragment = new SettingsFragment

  protected override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    push(new MainFragment)
  }

  def showSettings(v: View) {
    settingsFragment.setSpawnLocation(LocationObserver.getOnScreen(v))
    push(settingsFragment)
  }
}
