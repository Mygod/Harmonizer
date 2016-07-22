package tk.mygod.harmonizer

import android.os.Bundle
import android.support.v7.preference.Preference
import tk.mygod.preference.{DropDownPreference, NumberPickerPreference, NumberPickerPreferenceDialogFragment, PreferenceFragmentPlus}

/**
 * @author Mygod
 */
final class SettingsFragment extends PreferenceFragmentPlus {
  def onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
    getPreferenceManager.setSharedPreferencesName("settings")
    addPreferencesFromResource(R.xml.settings)
    val samplingRate = findPreference("audio.samplingRate").asInstanceOf[NumberPickerPreference]
    samplingRate.setMin(AudioConfig.minSamplingRate)
    samplingRate.setMax(AudioConfig.maxSamplingRate)
    samplingRate.setValue(AudioConfig.samplingRate)
    findPreference("audio.bitDepth").asInstanceOf[DropDownPreference].setValue(AudioConfig.format.toString)
  }

  override def onDisplayPreferenceDialog(preference: Preference) =
    if (preference.isInstanceOf[NumberPickerPreference])
      displayPreferenceDialog(preference.getKey, new NumberPickerPreferenceDialogFragment())
    else super.onDisplayPreferenceDialog(preference)
}
