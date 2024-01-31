package com.cdac.rrrlf.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cdac.rrrlf.Data.Library
import com.cdac.rrrlf.R
import com.cdac.rrrlf.Activities.Schemes

class LibraryAdapter(private val context: Context, private val library: List<Library>) :
    RecyclerView.Adapter<LibraryAdapter.BookViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_libraries, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val library = library[position]
        holder.emailTextView.text = library.beneficiary_contact_person_email_id
        holder.libraryTextView.text = library.beneficiary_name
        holder.mobileTextView.text = library.beneficiary_contact_person_mobile_no
        holder.pincodeTextView.text = library.beneficiary_pin_code.toString()
        holder.nameTextView.text = library.beneficiary_contact_person

        holder.itemView.setOnClickListener {
            val intent = Intent(context, Schemes::class.java)
            intent.putExtra("LIBRARY_TITLE", library.beneficiary_name)
            intent.putExtra("LIBRARY_ID", library.beneficiary_id)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return library.size
    }

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mobileTextView: TextView = itemView.findViewById(R.id.mobile)
        val pincodeTextView: TextView = itemView.findViewById(R.id.pincode)
        val emailTextView: TextView = itemView.findViewById(R.id.email)
        val libraryTextView: TextView = itemView.findViewById(R.id.library)
        val nameTextView: TextView = itemView.findViewById(R.id.name)
    }
}