package com.github.andreyasadchy.xtra.ui.streams.common

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.stream.Sort
import com.github.andreyasadchy.xtra.ui.common.ExpandingBottomSheetDialogFragment
import com.github.andreyasadchy.xtra.ui.main.MainActivity
import com.github.andreyasadchy.xtra.util.C
import kotlinx.android.synthetic.main.dialog_streams_sort.*

class StreamsSortDialog : ExpandingBottomSheetDialogFragment() {

    interface OnFilter {
        fun onChange(sort: Sort)
    }

    companion object {

        private const val SORT = "sort"

        fun newInstance(sort: Sort? = Sort.VIEWERS_HIGH): StreamsSortDialog {
            return StreamsSortDialog().apply {
                arguments = bundleOf(SORT to sort)
            }
        }
    }

    private lateinit var listener: OnFilter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as OnFilter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_streams_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        val args = requireArguments()
        val originalSortId = if (args.getSerializable(SORT) as Sort == Sort.VIEWERS_HIGH) R.id.viewers_high else R.id.viewers_low
        sort.check(originalSortId)
        apply.setOnClickListener {
            val checkedSortId = sort.checkedRadioButtonId
            if (checkedSortId != originalSortId) {
                listener.onChange(
                    if (checkedSortId == R.id.viewers_high) Sort.VIEWERS_HIGH else Sort.VIEWERS_LOW
                )
            }
            dismiss()
        }
        selectTags.setOnClickListener {
            activity.openTagSearch(gameId = parentFragment?.arguments?.getString(C.GAME_ID), gameName = parentFragment?.arguments?.getString(C.GAME_NAME))
            dismiss()
        }
    }
}