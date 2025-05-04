package com.grippo.validation

public object PasswordValidator {

    public fun isValid(value: String): Boolean {
        return value.length > 6
    }
}
