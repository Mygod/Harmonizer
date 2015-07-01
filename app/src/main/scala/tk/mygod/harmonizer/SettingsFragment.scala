package tk.mygod.harmonizer

import android.app.Activity
import android.os.Bundle
import android.view.{LayoutInflater, ViewGroup}
import tk.mygod.app.CircularRevealFragment

/**
 * @author Mygod
 */
final class SettingsFragment extends CircularRevealFragment {
  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    activity.asInstanceOf[MainActivity].settingsFragment = this
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
    val result = inflater.inflate(R.layout.fragment_settings, container, false)
    configureToolbar(result, R.string.settings, 0)
    result
  }

  override def onDestroyView {
    val activity = getActivity
    if (!activity.isFinishing && !activity.isDestroyed) {
      val manager = getFragmentManager
      val fragment = manager.findFragmentById(android.R.id.content)
      if (fragment != null) manager.beginTransaction.remove(fragment).commitAllowingStateLoss
    }
    super.onDestroyView
  }
}
