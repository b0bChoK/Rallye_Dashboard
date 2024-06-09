package com.b0bchok.rallye_dashboard_kt.rd_loader

data class ConverterConfigData(
    // In percent of the size of the page
    val leftMargin : Float,
    val rightMargin : Float,
    val topMargin : Float,
    val bottomMargin : Float,
    val columnAWidth : Float,
    val columnBWidth : Float,
    // row per column
    val lineNumber : Int
) {

}