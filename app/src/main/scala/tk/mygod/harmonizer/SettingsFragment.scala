package tk.mygod.harmonizer

import android.os.Bundle
import android.view.{LayoutInflater, ViewGroup}
import tk.mygod.app.CircularRevealFragment

/**
 * @author Mygod
 */
final class SettingsFragment extends CircularRevealFragment {
  override def onCreateSubView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
    val result = inflater.inflate(R.layout.fragment_settings, container, false)
    configureToolbar(result, R.string.settings, 0)
    result
  }
}
