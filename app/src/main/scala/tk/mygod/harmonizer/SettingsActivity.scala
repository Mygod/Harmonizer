package tk.mygod.harmonizer

import android.os.Bundle
import tk.mygod.app.ToolbarActivity

/**
 * @author Mygod
 */
class SettingsActivity extends ToolbarActivity {
  protected override def onCreate(icicle: Bundle) {
    super.onCreate(icicle)
    setContentView(R.layout.activity_settings)
    configureToolbar(0)
  }
}
