package com.example.https;

import java.util.Date;
import java.util.Map;

public class GenericItem {

    public String docId;
    public String titulo;
    public String subtitulo;
    public String estado;
    public String userId;
    public Map<String, Object> camposCompletos;

    // Campos extra que tu VisitasActivity YA usa
    public Date timestamp;
    public String asesor;        // asesor visible
    public String clienteNombre; // alias del título

    public GenericItem() {}

    public GenericItem(String docId,
                       String titulo,
                       String subtitulo,
                       String estado,
                       String userId,
                       Map<String, Object> camposCompletos,
                       Date timestamp,
                       String asesor) {

        this.docId = docId;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.estado = estado;
        this.userId = userId;
        this.camposCompletos = camposCompletos;

        this.timestamp = timestamp;
        this.asesor = asesor;
        this.clienteNombre = titulo;
    }
}
