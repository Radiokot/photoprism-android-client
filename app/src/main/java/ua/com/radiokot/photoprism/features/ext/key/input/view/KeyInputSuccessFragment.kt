package ua.com.radiokot.photoprism.features.ext.key.input.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ua.com.radiokot.photoprism.databinding.FragmentKeyInputSuccessBinding

class KeyInputSuccessFragment : Fragment() {
    private lateinit var view: FragmentKeyInputSuccessBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = FragmentKeyInputSuccessBinding.inflate(inflater)
        return view.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
