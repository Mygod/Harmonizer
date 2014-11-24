package tk.mygod.harmonizer

/**
 * Project: Harmonizer
 * @author  Mygod
 */
object Utils {
  def betterToString(value: Double): String =
    if (value == Math.floor(value)) "%.0f" format value else value.toString
}
