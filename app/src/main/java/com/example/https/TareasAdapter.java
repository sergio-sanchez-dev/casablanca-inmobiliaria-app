package com.example.https;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.TareaViewHolder> {

    private List<String> listaTareas;

    public TareasAdapter(List<String> listaTareas) {
        this.listaTareas = listaTareas;
    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarea, parent, false);
        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int position) {
        String item = listaTareas.get(position);

        // El formato que guardamos es "titulo|estado"
        String[] partes = item.split("\\|", 2);
        String titulo = partes[0];
        String estado = partes.length > 1 ? partes[1] : "";

        holder.tvTareaNombre.setText(titulo);

        switch (estado) {
            case "hoy":
                holder.tvTareaMeta.setText("Para hoy");
                holder.tvTareaBadge.setText("Hoy");
                holder.tvTareaBadge.setBackgroundResource(R.drawable.bg_badge_hoy);
                holder.tvTareaBadge.setTextColor(0xFF1565C0);
                holder.viewDot.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF2196F3));
                break;
            case "pendiente":
                holder.tvTareaMeta.setText("Pendiente");
                holder.tvTareaBadge.setText("Pendiente");
                holder.tvTareaBadge.setBackgroundResource(R.drawable.bg_badge_pendiente);
                holder.tvTareaBadge.setTextColor(0xFFE65100);
                holder.viewDot.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFFF9800));
                break;
            case "completada":
                holder.tvTareaMeta.setText("Completada");
                holder.tvTareaBadge.setText("Hecha");
                holder.tvTareaBadge.setBackgroundResource(R.drawable.bg_badge_completada);
                holder.tvTareaBadge.setTextColor(0xFF2E7D32);
                holder.viewDot.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF4CAF50));
                break;
            default:
                holder.tvTareaMeta.setText("");
                holder.tvTareaBadge.setText("");
                holder.tvTareaBadge.setBackgroundResource(0);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return listaTareas.size();
    }

    static class TareaViewHolder extends RecyclerView.ViewHolder {

        TextView tvTareaNombre;
        TextView tvTareaMeta;
        TextView tvTareaBadge;
        View viewDot;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTareaNombre = itemView.findViewById(R.id.tvTareaNombre);
            tvTareaMeta   = itemView.findViewById(R.id.tvTareaMeta);
            tvTareaBadge  = itemView.findViewById(R.id.tvTareaBadge);
            viewDot       = itemView.findViewById(R.id.viewDot);
        }
    }
}