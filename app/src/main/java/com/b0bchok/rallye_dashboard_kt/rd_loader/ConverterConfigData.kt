package com.b0bchok.rallye_dashboard_kt.rd_loader

data class ConverterConfigData(
    // In percent of the size of the page
    var leftMargin : Float,
    var rightMargin : Float,
    var topMargin : Float,
    var bottomMargin : Float,
    var columnAWidth : Float,
    var columnBWidth : Float,
    // row per column
    var lineNumber : Int
) {

}