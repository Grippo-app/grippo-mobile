package com.grippo.validation

public object NameValidator {

    public fun isValid(value: String): Boolean {
        return value.length > 3
    }
}
