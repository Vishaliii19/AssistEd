package com.assisted.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.assisted.app.R
import com.assisted.app.data.NoteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(private var notes: List<NoteEntity>) :
    RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNoteText: TextView = view.findViewById(R.id.tvNoteText)
        val tvNoteSubject: TextView = view.findViewById(R.id.tvNoteSubject)
        val tvNoteDate: TextView = view.findViewById(R.id.tvNoteDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.tvNoteText.text = note.text
        holder.tvNoteSubject.text = note.subjectTag
        
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(note.timestamp))
        holder.tvNoteDate.text = dateStr
    }

    override fun getItemCount() = notes.size

    fun updateNotes(newNotes: List<NoteEntity>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}
