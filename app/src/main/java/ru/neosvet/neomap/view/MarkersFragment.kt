package ru.neosvet.neomap.view

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import ru.neosvet.neomap.App
import ru.neosvet.neomap.R
import ru.neosvet.neomap.data.NeoMarker
import ru.neosvet.neomap.databinding.FragmentMarkersBinding
import ru.neosvet.neomap.list.MarkersAdapter
import ru.neosvet.neomap.list.MarkersListEvents
import ru.neosvet.neomap.list.SwipeHelper
import ru.neosvet.neomap.presenters.MapPresenter
import ru.neosvet.neomap.presenters.MarkersPresenter
import ru.neosvet.neomap.presenters.MarkersView

class MarkersFragment : Fragment(), MarkersView {
    private var binding: FragmentMarkersBinding? = null
    private val presenter: MarkersPresenter by lazy {
        MarkersPresenter(
            view = this,
            repository = App.repository,
        )
    }
    private lateinit var adMarkers: MarkersAdapter
    private var updateIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentMarkersBinding.inflate(inflater, container, false).also {
        setHasOptionsMenu(true)
        binding = it
    }.root

    override fun onDestroyView() {
        presenter.onDestroy()
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().setTitle(R.string.markers_on_maps)
        initList()
        presenter.loadList()
    }

    private fun initList() = binding?.run {
        adMarkers = MarkersAdapter(
            events = object : MarkersListEvents {
                override fun onClick(marker: NeoMarker) {
                    requireActivity().onBackPressed()
                    MapPresenter.showMarker(marker)
                }
            }
        )
        rvMarkers.adapter = adMarkers

        val radius = resources.getDimension(R.dimen.default_radius)
        val colorDelete = requireActivity().getColor(R.color.colorAccent)
        val colorEdit = requireActivity().getColor(R.color.colorPrimary)
        val imgDelete = BitmapFactory.decodeResource(resources, R.mipmap.delete)
        val imgEdit = BitmapFactory.decodeResource(resources, R.mipmap.edit)
        val swipeHelper: SwipeHelper = object : SwipeHelper(requireContext()) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {
                underlayButtons.add(
                    UnderlayButton(
                        imgDelete, colorDelete, radius, this@MarkersFragment::deleteMarker
                    )
                )
                underlayButtons.add(
                    UnderlayButton(
                        imgEdit, colorEdit, radius, this@MarkersFragment::editMarker
                    )
                )
            }
        }
        swipeHelper.attachToRecyclerView(rvMarkers)
    }

    private fun deleteMarker(index: Int) {
        presenter.deleteMarker(adMarkers.get(index))
        adMarkers.delete(index)
    }

    private fun editMarker(index: Int) {
        val marker = adMarkers.get(index)
        val oldName = marker.name

        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setMessage(getString(R.string.marker_name))
        val input = EditText(requireContext())
        input.setBackgroundResource(R.drawable.border)
        input.setText(oldName)
        alertDialog.setView(input)
        alertDialog.setPositiveButton(android.R.string.ok) { _, _ ->
            val newName = input.text.toString()
            if (newName.isEmpty() || newName == oldName) {
                adMarkers.notifyItemChanged(index)
                return@setPositiveButton
            }
            val newMarker = NeoMarker(newName, marker.lat, marker.lng)
            presenter.editMarker(oldName, newMarker)
            updateIndex = index
        }
        alertDialog.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            adMarkers.notifyItemChanged(index)
        }
        alertDialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.markers_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export ->
                presenter.exportMarkers()
            R.id.menu_import ->
                presenter.importMarkers()
            else ->
                return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun post(function: () -> Unit) = view?.post(function)

    override fun updateList(list: List<NeoMarker>) {
        for (marker in list) {
            adMarkers.add(marker)
        }
    }

    override fun updateMarker(marker: NeoMarker) {
        adMarkers.update(updateIndex, marker)
    }

    override fun showMessage(resource: Int) {
        binding?.run {
            Snackbar.make(
                root, getString(resource),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
}