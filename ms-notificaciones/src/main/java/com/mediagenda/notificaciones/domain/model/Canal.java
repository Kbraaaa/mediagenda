package com.mediagenda.notificaciones.domain.model;

/** Por donde sale el aviso: correo (SMTP o Amazon SES) o mensaje de texto (Amazon SNS). */
public enum Canal {
    CORREO, SMS
}
