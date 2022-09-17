package com.github.andreyasadchy.xtra.ui.player

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.game.Game
import com.github.andreyasadchy.xtra.ui.common.ExpandingBottomSheetDialogFragment
import com.github.andreyasadchy.xtra.ui.view.GridRecyclerView
import com.github.andreyasadchy.xtra.util.C


class PlayerGamesDialog : ExpandingBottomSheetDialogFragment() {

    interface PlayerSeekListener {
        fun seek(position: Long)
    }

    companion object {

        fun newInstance(gamesList: List<Game>): PlayerGamesDialog {
            return PlayerGamesDialog().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(C.GAMES_LIST, ArrayList(gamesList))
                }
            }
        }
    }

    lateinit var listener: PlayerSeekListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as PlayerSeekListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val recycleView = GridRecyclerView(context).apply {
            id = R.id.recyclerView
            setLayoutParams(layoutParams)
            adapter = PlayerGamesDialogAdapter(this@PlayerGamesDialog).also {
                it.submitList(arguments?.getParcelableArrayList<Game>(C.GAMES_LIST)?.toList())
            }
        }
        return NestedScrollView(context).apply { addView(recycleView) }
    }
}
