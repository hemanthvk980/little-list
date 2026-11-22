package in.littlelist;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(MailProperties.class)
class EmailConfiguration {
    @Bean
    @ConditionalOnProperty(name="app.mail-provider",havingValue="smtp",matchIfMissing=true)
    JavaMailSenderImpl smtpSender(MailProperties properties) {
        if(properties.getHost()==null || properties.getHost().isBlank())
            throw new IllegalStateException("MAIL_HOST is required for SMTP delivery.");
        var sender=new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        if(properties.getPort()!=null)sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername());sender.setPassword(properties.getPassword());
        sender.setProtocol(properties.getProtocol());sender.setDefaultEncoding(properties.getDefaultEncoding().name());
        var options=new Properties();options.putAll(properties.getProperties());sender.setJavaMailProperties(options);
        return sender;
    }
    @Bean
    @ConditionalOnProperty(name="app.mail-provider",havingValue="smtp",matchIfMissing=true)
    EmailDelivery smtpDelivery(JavaMailSender sender,@Value("${app.mail-from:}") String from){requireFrom(from);return (message,key)->sender.send(message);}

    @Bean
    @ConditionalOnProperty(name="app.mail-provider",havingValue="resend")
    EmailDelivery resendDelivery(@Value("${RESEND_API_KEY:}") String key,com.fasterxml.jackson.databind.ObjectMapper json,@Value("${app.mail-from:}") String from){
        requireFrom(from);
        if(key.isBlank())throw new IllegalStateException("RESEND_API_KEY is required for Resend delivery.");
        return new ResendDelivery(key,json);
    }
    private static void requireFrom(String from){if(from.isBlank())throw new IllegalStateException("MAIL_FROM is required for email delivery.");}
    @Bean
    @ConditionalOnProperty(name="app.mail-provider",havingValue="disabled")
    EmailDelivery disabledDelivery(){return (message,key)->{throw new MailSendException("Email delivery is disabled.");};}
}
