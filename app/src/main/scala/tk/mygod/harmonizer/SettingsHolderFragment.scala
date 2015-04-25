package tk.mygod.harmonizer

import android.os.Bundle
import android.preference.PreferenceFragment
import tk.mygod.app.FragmentPlus
import tk.mygod.preference.{DropDownPreference, NumberPickerPreference}

/**
 * @author Mygod
 */
final class SettingsHolderFragment extends PreferenceFragment with FragmentPlus {
  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    getPreferenceManager.setSharedPreferencesName("settings")
    addPreferencesFromResource(R.xml.settings)
    val config = new AudioConfig(getActivity)
    val samplingRate = findPreference("audio.samplingRate").asInstanceOf[NumberPickerPreference]
    samplingRate.setMin(AudioConfig.minSamplingRate)
    samplingRate.setMax(AudioConfig.maxSamplingRate)
    samplingRate.setValue(config.getSamplingRate)
    findPreference("audio.bitDepth").asInstanceOf[DropDownPreference].setValue(config.getFormat.toString)
  }
}