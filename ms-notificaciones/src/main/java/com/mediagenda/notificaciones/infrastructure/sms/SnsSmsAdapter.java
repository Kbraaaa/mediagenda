package com.mediagenda.notificaciones.infrastructure.sms;

import com.mediagenda.notificaciones.domain.model.exception.EnvioSmsException;
import com.mediagenda.notificaciones.domain.model.gateway.SmsGateway;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/**
 * En AWS: envia el SMS con Amazon SNS (SDK v2), directo al numero, sin tema. Tipo "Transactional"
 * para que el operador lo trate como aviso importante. Permisos por el rol de IAM, sin claves.
 */
public class SnsSmsAdapter implements SmsGateway {

    private final SnsClient sns;

    public SnsSmsAdapter(SnsClient sns) {
        this.sns = sns;
    }

    @Override
    public void enviar(String telefonoE164, String mensaje) {
        try {
            sns.publish(PublishRequest.builder()
                    .phoneNumber(telefonoE164)
                    .message(mensaje)
                    .messageAttributes(Map.of("AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
                            .dataType("String").stringValue("Transactional").build()))
                    .build());
        } catch (SdkException e) {
            throw new EnvioSmsException("Amazon SNS rechazo el SMS: " + e.getMessage(), e);
        }
    }
}
