package com.example.aichat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.aichat.model.LocaleManager;

public class LanguageFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_language, container, false);

        // Обработчики для кнопок выбора языка
        view.findViewById(R.id.btn_english).setOnClickListener(v -> changeLanguage("en"));
        view.findViewById(R.id.btn_russian).setOnClickListener(v -> changeLanguage("ru"));
        view.findViewById(R.id.btn_spanish).setOnClickListener(v -> changeLanguage("es"));

        // Обработчик для кнопки "Назад"
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> navigateBack());

        return view;
    }

    private void changeLanguage(String language) {
        LocaleManager.setLocale(requireContext(), language);
        requireActivity().recreate();
    }

    private void navigateBack() {
        // Вариант 1: Просто закрыть фрагмент
        requireActivity().getSupportFragmentManager().popBackStack();

        // Вариант 2: С анимацией (если нужно)
        // requireActivity().getSupportFragmentManager()
        //     .beginTransaction()
        //     .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        //     .remove(this)
        //     .commit();
    }
}