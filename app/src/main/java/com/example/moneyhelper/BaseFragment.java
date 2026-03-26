package com.example.moneyhelper;

import android.content.Context;
import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {
    @Override
    public void onAttach(Context context) {
        super.onAttach(LocaleHelper.onAttach(context));
    }
}