package com.mediagenda.notificaciones.application;

import com.mediagenda.notificaciones.domain.model.gateway.CorreoGateway;
import com.mediagenda.notificaciones.domain.model.gateway.NotificacionGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SmsGateway;
import com.mediagenda.notificaciones.domain.model.gateway.SolicitudEnvioGateway;
import com.mediagenda.notificaciones.domain.usecase.ConsultarNotificacionUseCase;
import com.mediagenda.notificaciones.domain.usecase.CorregirDestinatarioUseCase;
import com.mediagenda.notificaciones.domain.usecase.CrearNotificacionManualUseCase;
import com.mediagenda.notificaciones.domain.usecase.Despachador;
import com.mediagenda.notificaciones.domain.usecase.EnviarPendienteUseCase;
import com.mediagenda.notificaciones.domain.usecase.ListarNotificacionesUseCase;
import com.mediagenda.notificaciones.domain.usecase.MarcarLeidaUseCase;
import com.mediagenda.notificaciones.domain.usecase.ProcesarEventoUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReenviarNotificacionUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReintentarFallidasUseCase;
import com.mediagenda.notificaciones.domain.usecase.ReintentarNotificacionUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCasesConfig {

    @Bean
    public Despachador despachador(CorreoGateway correo, SmsGateway sms, NotificacionGateway n, Clock reloj) {
        return new Despachador(correo, sms, n, reloj);
    }

    @Bean
    public ProcesarEventoUseCase procesarEventoUseCase(NotificacionGateway n, Despachador d,
                                                       @Value("${notificaciones.sms:simulado}") String sms) {
        return new ProcesarEventoUseCase(n, d, !"ninguno".equalsIgnoreCase(sms));
    }

    @Bean
    public CrearNotificacionManualUseCase crearNotificacionManualUseCase(NotificacionGateway n,
                                                                         SolicitudEnvioGateway s, Despachador d) {
        return new CrearNotificacionManualUseCase(n, s, d);
    }

    @Bean
    public ConsultarNotificacionUseCase consultarNotificacionUseCase(NotificacionGateway n) {
        return new ConsultarNotificacionUseCase(n);
    }

    @Bean
    public ListarNotificacionesUseCase listarNotificacionesUseCase(NotificacionGateway n) {
        return new ListarNotificacionesUseCase(n);
    }

    @Bean
    public ReintentarNotificacionUseCase reintentarNotificacionUseCase(NotificacionGateway n, SolicitudEnvioGateway s) {
        return new ReintentarNotificacionUseCase(n, s);
    }

    @Bean
    public MarcarLeidaUseCase marcarLeidaUseCase(NotificacionGateway n) {
        return new MarcarLeidaUseCase(n);
    }

    @Bean
    public EnviarPendienteUseCase enviarPendienteUseCase(NotificacionGateway n, Despachador d) {
        return new EnviarPendienteUseCase(n, d);
    }

    @Bean
    public ReintentarFallidasUseCase reintentarFallidasUseCase(NotificacionGateway n, Despachador d,
                                                               @Value("${notificaciones.intentos-maximos:3}") int max) {
        return new ReintentarFallidasUseCase(n, d, max);
    }

    @Bean
    public ReenviarNotificacionUseCase reenviarNotificacionUseCase(NotificacionGateway n, SolicitudEnvioGateway s,
                                                                   Despachador d) {
        return new ReenviarNotificacionUseCase(n, s, d);
    }

    @Bean
    public CorregirDestinatarioUseCase corregirDestinatarioUseCase(NotificacionGateway n, SolicitudEnvioGateway s) {
        return new CorregirDestinatarioUseCase(n, s);
    }
}
