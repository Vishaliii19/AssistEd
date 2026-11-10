package com.assisted.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.assisted.app.adapters.NotesAdapter
import com.assisted.app.databinding.FragmentNotesBinding
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvNotes.layoutManager = LinearLayoutManager(context)
        adapter = NotesAdapter(emptyList())
        binding.rvNotes.adapter = adapter

        val app = requireActivity().application as AssistApp
        
        viewLifecycleOwner.lifecycleScope.launch {
            app.repository.allNotes.collect { notes ->
                adapter.updateNotes(notes)
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                viewLifecycleOwner.lifecycleScope.launch {
                    app.repository.allNotes.collect { notes ->
                        val filtered = notes.filter { 
                            it.text.lowercase().contains(query) || 
                            it.subjectTag.lowercase().contains(query) 
                        }
                        adapter.updateNotes(filtered)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
