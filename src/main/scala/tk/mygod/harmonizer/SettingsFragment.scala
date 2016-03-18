package tk.mygod.harmonizer

import android.app.Activity
import android.os.Bundle
import android.support.v7.preference.Preference
import android.view.View
import tk.mygod.app.ToolbarTypedFindView
import tk.mygod.preference.{DropDownPreference, NumberPickerPreference, NumberPickerPreferenceDialogFragment, ToolbarPreferenceFragment}

/**
 * @author Mygod
 */
final class SettingsFragment extends ToolbarPreferenceFragment {
  override def layout = R.layout.fragment_settings

  override def onViewCreated(view: View, savedInstanceState: Bundle) {
    super.onViewCreated(view, savedInstanceState)
    configureToolbar(R.string.settings)
    setNavigationIcon(ToolbarTypedFindView.BACK)
  }

  def onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
    getPreferenceManager.setSharedPreferencesName("settings")
    addPreferencesFromResource(R.xml.settings)
    val config = new AudioConfig(getActivity)
    val samplingRate = findPreference("audio.samplingRate").asInstanceOf[NumberPickerPreference]
    samplingRate.setMin(AudioConfig.minSamplingRate)
    samplingRate.setMax(AudioConfig.maxSamplingRate)
    samplingRate.setValue(config.getSamplingRate)
    findPreference("audio.bitDepth").asInstanceOf[DropDownPreference].setValue(config.getFormat.toString)
  }

  override def onAttach(activity: Activity) {
    //noinspection ScalaDeprecation
    super.onAttach(activity)
    activity.asInstanceOf[MainActivity].settingsFragment = this
  }

  override def onDisplayPreferenceDialog(preference: Preference) =
    if (preference.isInstanceOf[NumberPickerPreference])
      displayPreferenceDialog(new NumberPickerPreferenceDialogFragment(preference.getKey))
    else super.onDisplayPreferenceDialog(preference)
}
