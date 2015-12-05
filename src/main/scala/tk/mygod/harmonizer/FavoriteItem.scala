package tk.mygod.harmonizer

/**
 * Project: Harmonizer
 * @author  Mygod
 */
class FavoriteItem(val name: String, val frequency: Double) {
  def getFullName: String = String.format("%s (%s Hz)", name, Utils.betterToString(frequency))
}
