package in.littlelist;

import org.springframework.mail.SimpleMailMessage;

interface EmailDelivery {
    void send(SimpleMailMessage message, String idempotencyKey);
}
