/*
Copyright (c) 2024 DEWAS Albert

This license replace the general license of the software
and apply to all file in this directory "rb_loader".

Permission is hereby granted, free of charge, to any person obtaining
a copy of this package and associated documentation files (the "package"),
to deal in the package without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom
the package is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall
be included in all copies or substantial portions of the package.

THE package IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE package.
 */

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