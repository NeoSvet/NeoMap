package ru.neosvet.neomap.presenters

interface MarkersView {
    fun post(function: () -> Unit): Boolean?
    fun updateList(list: ArrayList<String>)
    fun showMessage(resource: Int)
}