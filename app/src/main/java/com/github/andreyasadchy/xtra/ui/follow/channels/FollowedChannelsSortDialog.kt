package com.github.andreyasadchy.xtra.ui.follow.channels

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.os.bundleOf
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.helix.follows.Order
import com.github.andreyasadchy.xtra.model.helix.follows.Order.ASC
import com.github.andreyasadchy.xtra.model.helix.follows.Order.DESC
import com.github.andreyasadchy.xtra.model.helix.follows.Sort
import com.github.andreyasadchy.xtra.model.helix.follows.Sort.*
import com.github.andreyasadchy.xtra.ui.common.ExpandingBottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_followed_channels_sort.*

class FollowedChannelsSortDialog : ExpandingBottomSheetDialogFragment() {

    interface OnFilter {
        fun onChange(sort: Sort, sortText: CharSequence, order: Order, orderText: CharSequence, saveDefault: Boolean)
    }

    companion object {

        private const val SORT = "sort"
        private const val ORDER = "order"
        private const val SAVE_DEFAULT = "save_default"

        fun newInstance(sort: Sort, order: Order, saveDefault: Boolean = false): FollowedChannelsSortDialog {
            return FollowedChannelsSortDialog().apply {
                arguments = bundleOf(SORT to sort, ORDER to order, SAVE_DEFAULT to saveDefault)
            }
        }
    }

    private lateinit var listener: OnFilter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as OnFilter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_followed_channels_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val originalSortId = when (args.getSerializable(SORT) as Sort) {
            FOLLOWED_AT -> R.id.time_followed
            ALPHABETICALLY -> R.id.alphabetically
            LAST_BROADCAST -> R.id.last_broadcast
        }
        val originalOrderId = if (args.getSerializable(ORDER) as Order == DESC) R.id.newest_first else R.id.oldest_first
        val originalSaveDefault = args.getBoolean(SAVE_DEFAULT)
        sort.check(originalSortId)
        order.check(originalOrderId)
        saveDefault.isChecked = originalSaveDefault
        apply.setOnClickListener {
            val checkedSortId = sort.checkedRadioButtonId
            val checkedOrderId = order.checkedRadioButtonId
            val checkedSaveDefault = saveDefault.isChecked
            if (checkedSortId != originalSortId || checkedOrderId != originalOrderId || checkedSaveDefault != originalSaveDefault) {
                val sortBtn = view.findViewById<RadioButton>(checkedSortId)
                val orderBtn = view.findViewById<RadioButton>(checkedOrderId)
                listener.onChange(
                    when (checkedSortId) {
                        R.id.time_followed -> FOLLOWED_AT
                        R.id.alphabetically -> ALPHABETICALLY
                        else -> LAST_BROADCAST
                    },
                    sortBtn.text,
                    if (checkedOrderId == R.id.newest_first) DESC else ASC,
                    orderBtn.text,
                    checkedSaveDefault
                )
            }
            dismiss()
        }
    }
}