package tk.mygod.harmonizer

import android.content.SharedPreferences
import android.os.Bundle
import tk.mygod.preference.{DropDownPreference, PreferenceFragmentPlus}

/**
 * @author Mygod
 */
final class SettingsFragment extends PreferenceFragmentPlus with SharedPreferences.OnSharedPreferenceChangeListener {
  def onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
    val pm = getPreferenceManager
    pm.setSharedPreferencesName("settings")
    pm.getSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    addPreferencesFromResource(R.xml.settings)
    findPreference(AudioConfig.BIT_DEPTH).asInstanceOf[DropDownPreference].setValue(AudioConfig.format.toString)
  }

  def onSharedPreferenceChanged(pref: SharedPreferences, key: String) = AudioConfig.formatChanged = true
}
