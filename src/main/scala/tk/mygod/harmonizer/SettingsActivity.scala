package tk.mygod.harmonizer

import android.os.Bundle
import tk.mygod.app.{CircularRevealActivity, ToolbarActivity}

/**
  * @author mygod
  */
class SettingsActivity extends ToolbarActivity with CircularRevealActivity {
  protected override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)
    configureToolbar()
    setNavigationIcon()
  }
}
