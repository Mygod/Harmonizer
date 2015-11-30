package tk.mygod.harmonizer

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView.ViewHolder
import android.support.v7.widget.helper.ItemTouchHelper
import android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback
import android.support.v7.widget.{DefaultItemAnimator, LinearLayoutManager, RecyclerView}
import android.view.View.{OnAttachStateChangeListener, OnClickListener}
import android.view._
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import tk.mygod.app.CircularRevealFragment
import tk.mygod.harmonizer.TypedResource._
import tk.mygod.view.LocationObserver

import scala.collection.mutable.ArrayBuffer

/**
 * @author Mygod
 */
class FavoritesFragment extends CircularRevealFragment {
  private lazy val mainActivity = getActivity.asInstanceOf[MainActivity]
  private lazy val mainFragment = mainActivity.mainFragment

  private class FavoriteItemViewHolder(private val view: View) extends RecyclerView.ViewHolder(view)
    with View.OnClickListener {
    var item: FavoriteItem = _
    private val text = itemView.findViewById(android.R.id.text1).asInstanceOf[TextView]
    itemView.setOnTouchListener(LocationObserver)
    itemView.setOnClickListener(this)

    {
      val share = itemView.findViewById(R.id.action_share)
      share.setOnClickListener(_ => mainActivity.share(getString(R.string.share_content, item.getFullName), item.name))
      share.setOnLongClickListener(_ => {
        showToast(R.string.action_share)
        true
      })
    }

    def bind(item: FavoriteItem) {
      this.item = item
      text.setText(item.getFullName)
      view.setTag(item)
    }

    def onClick(v: View) {
      mainFragment.frequencyText.setText(Utils.betterToString(item.frequency))
      exit(v)
    }
  }

  private class FavoritesAdapter(private val empty: View) extends RecyclerView.Adapter[FavoriteItemViewHolder] {
    private val favorites = new ArrayBuffer[FavoriteItem]
    private val pref = mainActivity.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    {
      val size = pref.getInt("size", 0)
      if (size > 0) {
        empty.setVisibility(View.GONE)
        for (i <- 0 until size) favorites += new FavoriteItem(pref.getString(i + "_name", ""),
          java.lang.Double.longBitsToDouble(pref.getLong(i + "_freq", 0)))
      }
    }

    def onCreateViewHolder(vg: ViewGroup, i: Int) = new FavoriteItemViewHolder(LayoutInflater.from(vg.getContext)
      .inflate(R.layout.favorite_list_item, vg, false))
    def onBindViewHolder(vh: FavoriteItemViewHolder, i: Int) = vh.bind(favorites(i))
    def getItemCount = favorites.size

    def add(item: FavoriteItem) {
      val pos = favorites.size
      favorites += item
      update
      empty.setVisibility(View.GONE)
      notifyItemInserted(pos)
    }

    def remove(pos: Int) {
      favorites.remove(pos)
      update
      notifyItemRemoved(pos)
      if (favorites.isEmpty) empty.setVisibility(View.VISIBLE)
    }
    def remove(item: FavoriteItem): Unit = remove(favorites.indexOf(item))

    def move(from: Int, to: Int) {
      if (from == to) return
      val item = favorites(from)
      val order = if (from > to) -1 else 1
      var i = from
      var j = from + order
      while ((j <= from || j <= to) && (j >= from || j >= to)) {
        favorites(i) = favorites(j)
        i = j
        j += order
      }
      favorites(to) = item
      update
      notifyItemMoved(from, to)
    }

    def undo(actions: ArrayBuffer[(Int, FavoriteItem)]) = {
      for ((index, item) <- actions.reverseIterator) {
        favorites.insert(index, item)
        notifyItemInserted(index)
      }
      actions.clear
      update
      empty.setVisibility(View.GONE)
    }

    def update {
      val oldSize = pref.getInt("size", 0)
      var size = 0
      val editor = pref.edit
      for (favorite <- favorites) {
        editor.putString(size + "_name", favorite.name)
        editor.putLong(size + "_freq", java.lang.Double.doubleToLongBits(favorite.frequency))
        size += 1
      }
      editor.putInt("size", size)
      for (size <- size until oldSize) {
        editor.remove(size + "_name")
        editor.remove(size + "_freq")
      }
      editor.apply
    }
  }

  private var favoritesAdapter: FavoritesAdapter = _
  private var removedSnackbar: Snackbar = _
  private val recycleBin = new ArrayBuffer[(Int, FavoriteItem)]

  override def isFullscreen = true

  override def onAttach(activity: Activity) {
    //noinspection ScalaDeprecation
    super.onAttach(activity)
    activity.asInstanceOf[MainActivity].favoritesFragment = this
  }

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
    removedSnackbar = Snackbar.make(getView, R.string.removed, Snackbar.LENGTH_LONG)
      .setAction(R.string.undo, ((v: View) => favoritesAdapter.undo(recycleBin)): OnClickListener)
    removedSnackbar.getView.addOnAttachStateChangeListener(new OnAttachStateChangeListener {
      def onViewDetachedFromWindow(v: View) {
        recycleBin.clear
      }
      def onViewAttachedToWindow(v: View) = ()
    })
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) = {
    val result = inflater.inflate(R.layout.fragment_favorites, container, false)
    configureToolbar(result, R.string.favorites, 0)
    result.findView(TR.favorite_name_text).setOnEditorActionListener(
      (v: TextView, actionId: Int, event: KeyEvent) => if (actionId == EditorInfo.IME_ACTION_SEND) {
        favoritesAdapter.add(new FavoriteItem(v.getText.toString, mainFragment.getFrequency))
        v.setText(null)
        true
      } else false)
    val favoriteList = result.findView(TR.favorite)
    favoriteList.setLayoutManager(new LinearLayoutManager(mainActivity))
    favoriteList.setItemAnimator(new DefaultItemAnimator)
    favoritesAdapter = new FavoritesAdapter(result.findViewById(android.R.id.empty))
    favoriteList.setAdapter(favoritesAdapter)
    new ItemTouchHelper(new SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
      ItemTouchHelper.START | ItemTouchHelper.END) {
      def onSwiped(viewHolder: ViewHolder, direction: Int) = {
        val index = viewHolder.getAdapterPosition
        favoritesAdapter.remove(index)
        recycleBin.append((index, viewHolder.asInstanceOf[FavoriteItemViewHolder].item))
        removedSnackbar.show
      }
      def onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean = {
        favoritesAdapter.move(viewHolder.getAdapterPosition, target.getAdapterPosition)
        true
      }
    }).attachToRecyclerView(favoriteList)
    result
  }
}