package com.example.https;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;


public class CitasAdapter extends RecyclerView.Adapter<CitasAdapter.ViewHolder> {

    private List<String> citas;
    private OnDeleteClick listener;

    public interface OnDeleteClick {
        void onDelete(int position);
    }

    public CitasAdapter(List<String> citas, OnDeleteClick listener) {
        this.citas = citas;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cita, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        holder.txtCita.setText(citas.get(position));

        holder.btnBorrar.setOnClickListener(v -> {
            listener.onDelete(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return citas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtCita;
        Button btnBorrar;

        public ViewHolder(View itemView) {
            super(itemView);

            txtCita = itemView.findViewById(R.id.txtCita);
            btnBorrar = itemView.findViewById(R.id.btnBorrar);
        }
    }
}


