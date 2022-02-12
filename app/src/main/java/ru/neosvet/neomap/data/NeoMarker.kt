package ru.neosvet.neomap.data

class NeoMarker() {
    var name: String = ""
        private set
    var lat: Double = 0.0
        private set
    var lng: Double = 0.0
        private set

    constructor(name: String, lat: Double, lng: Double) : this() {
        this.name = name
        this.lat = lat
        this.lng = lng
    }

    constructor(line: String) : this() {
        var i = line.indexOf(",")
        lng = line.substring(1, i).toDouble()
        i++
        lat = line.substring(i, line.indexOf(",", i)).toDouble()
        i = line.indexOf("\"") + 1
        name = line.substring(i, line.indexOf("\"", i))
    }

    fun toLine(): String =
        "[$lng,$lat,\"$name\",\"\"]\n"
}