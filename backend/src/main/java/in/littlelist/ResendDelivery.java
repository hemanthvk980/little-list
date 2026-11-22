package in.littlelist;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

final class ResendDelivery implements EmailDelivery {
    private final String key;
    private final ObjectMapper json;
    private final HttpClient client;
    private final URI endpoint;
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(ResendDelivery.class);
    ResendDelivery(String key,ObjectMapper json){this(key,json,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build(),URI.create("https://api.resend.com/emails"));}
    // Package-private endpoint injection is for local HTTP tests only, not runtime configuration.
    ResendDelivery(String key,ObjectMapper json,HttpClient client,URI endpoint){this.key=key;this.json=json;this.client=client;this.endpoint=endpoint;}
    @Override public void send(SimpleMailMessage message,String idempotencyKey){
        try {
            var body=json.writeValueAsString(Map.of("from",Objects.requireNonNull(message.getFrom()),"to",List.of(Objects.requireNonNull(message.getTo())),"subject",Objects.requireNonNull(message.getSubject()),"text",Objects.requireNonNull(message.getText())));
            var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(8))
                .header("Authorization","Bearer "+key).header("Content-Type","application/json")
                .header("Idempotency-Key",idempotencyKey).POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response=client.send(request,HttpResponse.BodyHandlers.discarding());
            if(response.statusCode()<200||response.statusCode()>=300){
                log.warn("Email provider rejected request with HTTP status {}",response.statusCode());
                throw new MailSendException("Email provider did not accept the message.");
            }
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new MailSendException("Email request interrupted.");}
        catch(MailSendException e){throw e;}
        catch(Exception e){log.warn("Email provider request failed ({})",e.getClass().getSimpleName());throw new MailSendException("Email provider is unavailable.");}
    }
}
