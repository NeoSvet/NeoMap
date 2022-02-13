package ru.neosvet.neomap.list

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import ru.neosvet.neomap.R
import ru.neosvet.neomap.data.NeoMarker
import ru.neosvet.neomap.databinding.ItemMarkerBinding

class MarkerHolder(
    private val binding: ItemMarkerBinding,
    private val events: MarkersListEvents
) : RecyclerView.ViewHolder(binding.root) {
    fun setMarker(marker: NeoMarker) = binding.run {
        root.setOnClickListener {
            events.onClick(marker)
        }
        title.text = marker.name
        location.text = String.format(
            root.context.getString(R.string.format_location),
            marker.lat, marker.lng
        )
        if (marker.description.isEmpty()) {
            description.visibility = View.GONE
        } else {
            description.visibility = View.VISIBLE
            description.text = marker.description
        }
    }
}