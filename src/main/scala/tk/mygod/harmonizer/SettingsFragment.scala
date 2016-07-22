package tk.mygod.harmonizer

import android.os.Bundle
import tk.mygod.preference.{DropDownPreference, PreferenceFragmentPlus}

/**
 * @author Mygod
 */
final class SettingsFragment extends PreferenceFragmentPlus {
  def onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
    getPreferenceManager.setSharedPreferencesName("settings")
    addPreferencesFromResource(R.xml.settings)
    findPreference("audio.bitDepth").asInstanceOf[DropDownPreference].setValue(AudioConfig.format.toString)
  }
}
