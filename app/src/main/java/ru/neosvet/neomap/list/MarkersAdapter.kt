package ru.neosvet.neomap.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.neosvet.neomap.data.NeoMarker
import ru.neosvet.neomap.databinding.ItemMarkerBinding

class MarkersAdapter(
    private val events: MarkersListEvents
) : RecyclerView.Adapter<MarkerHolder>() {
    private val list = arrayListOf<NeoMarker>()

    fun add(marker: NeoMarker) {
        list.add(marker)
        notifyItemInserted(list.size - 1)
    }

    fun get(index: Int) = list[index]

    fun delete(index: Int) {
        list.removeAt(index)
        notifyItemRemoved(index)
    }

    fun update(index: Int, marker: NeoMarker) {
        list[index] = marker
        notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MarkerHolder(
            ItemMarkerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            events
        )

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: MarkerHolder, position: Int) =
        holder.setMarker(list[position])
}