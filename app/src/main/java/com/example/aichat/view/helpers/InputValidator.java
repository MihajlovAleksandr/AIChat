package com.example.aichat.view.helpers;

import android.text.TextUtils;

public class InputValidator {

    private InputValidator() {
    }

    public static boolean isEmailValid(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,6}$";
        return !TextUtils.isEmpty(email) && email.matches(emailPattern);
    }

    public static boolean isPasswordValid(String password) {
        if (password == null || password.length() < 8)
            return false;

        String passwordPattern = "^(?=.*[0-9])(?=.*[a-zа-я])(?=.*[A-ZА-Я])(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }
}
