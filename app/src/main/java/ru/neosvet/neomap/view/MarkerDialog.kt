package ru.neosvet.neomap.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import ru.neosvet.neomap.App
import ru.neosvet.neomap.R
import ru.neosvet.neomap.data.NeoMarker
import ru.neosvet.neomap.databinding.DialogMarkerBinding
import ru.neosvet.neomap.presenters.DialogPresenter
import ru.neosvet.neomap.presenters.DialogView

class MarkerDialog(
    private val bottomSheet: BottomSheetBehavior<FrameLayout>
) : Fragment(), DialogView {
    private val presenter: DialogPresenter by lazy {
        DialogPresenter(
            view = this,
            repository = App.repository
        )
    }
    private var binding: DialogMarkerBinding? = null
    private lateinit var callback: (String, String) -> Unit
    val isHide: Boolean
        get() = bottomSheet.state == BottomSheetBehavior.STATE_HIDDEN
    private var oldName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = DialogMarkerBinding.inflate(inflater, container, false).also {
        binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTextWatcher()
        initButtons()
    }

    private fun initTextWatcher() = binding?.run {
        etName.doAfterTextChanged {
            if (it.isNullOrEmpty()) {
                btnOk.isEnabled = false
                tilName.error = getString(R.string.name_is_empty)
            } else {
                btnOk.isEnabled = true
                tilName.error = null
            }
        }
    }

    private fun initButtons() = binding?.run {
        btnCancel.setOnClickListener {
            hide()
        }
        btnOk.setOnClickListener {
            binding?.run {
                presenter.checkName(oldName, etName.text.toString())
            }
        }
    }

    fun show(marker: NeoMarker?, callback: (String, String) -> Unit) {
        this.callback = callback
        binding?.run {
            if (marker == null) {
                oldName = null
                etName.setText("")
                presenter.requestName(requireContext())
                etDes.setText("")
            } else {
                oldName = marker.name
                etName.setText(marker.name)
                etDes.setText(marker.description)
            }
        }
        bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hide(): Boolean {
        val state = bottomSheet.state != BottomSheetBehavior.STATE_HIDDEN
        if (state)
            bottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
        return state
    }

    override fun postName(name: String) {
        binding?.run {
            root.post {
                etName.setText(name)
                etName.setSelection(name.length)
            }
        }
    }

    override fun postResult(nameIsExists: Boolean) {
        binding?.run {
            root.post {
                if (nameIsExists) {
                    btnOk.isEnabled = false
                    tilName.error = getString(R.string.name_exist)
                } else {
                    tilName.error = null
                    callback.invoke(etName.text.toString(), etDes.text.toString())
                    hide()
                }
            }
        }
    }
}