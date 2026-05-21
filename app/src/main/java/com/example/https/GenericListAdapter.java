package com.example.https;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adaptador reutilizable para todas las listas de documentos.
 * Usa item_generic_lista.xml
 *
 * Cada Activity construye la lista de GenericItem con:
 *  - titulo
 *  - subtitulo
 *  - estado (badge opcional)
 *  - userId
 *  - camposCompletos (para rellenar el formulario al editar)
 */
public class GenericListAdapter extends RecyclerView.Adapter<GenericListAdapter.ViewHolder> {

    public interface OnItemListener {
        void onEditar(GenericItem item, int position);
        void onEliminar(GenericItem item, int position);
    }

    private final List<GenericItem> lista;
    private final OnItemListener listener;

    public GenericListAdapter(List<GenericItem> lista, OnItemListener listener) {
        this.lista    = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_generic_lista, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        GenericItem item = lista.get(position);

        // Título
        h.tvTitulo.setText(item.titulo != null && !item.titulo.isEmpty()
                ? item.titulo
                : "—");

        // Subtítulo
        if (item.subtitulo != null && !item.subtitulo.isEmpty()) {
            h.tvSubtitulo.setVisibility(View.VISIBLE);
            h.tvSubtitulo.setText(item.subtitulo);
        } else {
            h.tvSubtitulo.setVisibility(View.GONE);
        }

        // Badge estado
        if (item.estado != null && !item.estado.isEmpty()) {
            h.tvEstado.setVisibility(View.VISIBLE);
            h.tvEstado.setText(item.estado);
        } else {
            h.tvEstado.setVisibility(View.GONE);
        }

        h.btnEditar.setOnClickListener(v ->
                listener.onEditar(item, h.getAdapterPosition()));

        h.btnEliminar.setOnClickListener(v ->
                listener.onEliminar(item, h.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return lista != null ? lista.size() : 0;
    }

    public void remove(int position) {
        if (lista == null) return;
        if (position < 0 || position >= lista.size()) return;

        lista.remove(position);
        notifyItemRemoved(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvTitulo, tvSubtitulo, tvEstado;
        ImageButton btnEditar, btnEliminar;

        ViewHolder(@NonNull View v) {
            super(v);
            tvTitulo    = v.findViewById(R.id.tvItemTitulo);
            tvSubtitulo = v.findViewById(R.id.tvItemSubtitulo);
            tvEstado    = v.findViewById(R.id.tvItemEstado);
            btnEditar   = v.findViewById(R.id.btnItemEditar);
            btnEliminar = v.findViewById(R.id.btnItemEliminar);
        }
    }
}
