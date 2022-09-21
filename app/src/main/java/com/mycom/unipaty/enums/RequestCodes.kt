package com.mycom.unipaty.enums

enum class RequestCodes(val value: Int) {
    start(3),
    camera(1),
    permission(2);

    companion object {
        fun fromInt(value: Int) = RequestCodes.values().first { it.value == value }
    }
}