package ru.neosvet.neomap.presenters

interface DialogView {
    fun postName(name: String)
    fun postResult(nameIsExists: Boolean)
}