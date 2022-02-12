package ru.neosvet.neomap.view

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import ru.neosvet.neomap.R
import ru.neosvet.neomap.data.DataBase
import ru.neosvet.neomap.data.DataBaseRepository
import ru.neosvet.neomap.data.NeoMarker
import ru.neosvet.neomap.databinding.FragmentMarkersBinding
import ru.neosvet.neomap.presenters.MarkersPresenter
import ru.neosvet.neomap.presenters.MarkersView

class MarkersFragment : Fragment(), MarkersView {
    private var binding: FragmentMarkersBinding? = null
    private val presenter: MarkersPresenter by lazy {
        MarkersPresenter(
            view = this,
            repository = DataBaseRepository(DataBase(requireContext())),
        )
    }
    private lateinit var adMarkers: ArrayAdapter<String>

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().setTitle(R.string.markers_on_maps)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initList()
        presenter.loadList()
    }

    private fun initList() = binding?.run {
        adMarkers = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            ArrayList<String>()
        )
        lvMarkers.adapter = adMarkers
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
            adMarkers.add(marker.name)
        }
        adMarkers.notifyDataSetChanged()
    }

    override fun showMessage(resource: Int) {
        binding?.run {
            Snackbar.make(
                fabEdit, getString(resource),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
}