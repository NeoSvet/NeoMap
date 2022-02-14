package ru.neosvet.neomap.list

import ru.neosvet.neomap.data.NeoMarker

interface MarkersListEvents {
    fun onClick(marker: NeoMarker)
}