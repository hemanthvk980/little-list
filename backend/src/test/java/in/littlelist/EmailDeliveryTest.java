package in.littlelist;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class EmailDeliveryTest {
 private final ApplicationContextRunner runner=new ApplicationContextRunner()
  .withConfiguration(AutoConfigurations.of(MailHealthContributorAutoConfiguration.class))
  .withUserConfiguration(EmailConfiguration.class).withBean(ObjectMapper.class,ObjectMapper::new)
  .withPropertyValues("app.mail-from=test@example.test");
 @Test void smtpRetainsConnectionHealthAndTimeouts(){runner.withPropertyValues("spring.mail.host=127.0.0.1","spring.mail.port=1025","spring.mail.properties.mail.smtp.connectiontimeout=3000").run(ctx->{assertNull(ctx.getStartupFailure());assertNotNull(ctx.getBean(EmailDelivery.class));assertNotNull(ctx.getBean(JavaMailSender.class));assertTrue(ctx.containsBean("mailHealthContributor"));});}
 @Test void resendDoesNotInstantiateSmtpEvenIfOldGmailSettingsRemain(){runner.withPropertyValues("app.mail-provider=resend","RESEND_API_KEY=test-only","spring.mail.host=smtp.gmail.com").run(ctx->{assertNull(ctx.getStartupFailure());assertInstanceOf(ResendDelivery.class,ctx.getBean(EmailDelivery.class));assertTrue(ctx.getBeansOfType(JavaMailSender.class).isEmpty());assertFalse(ctx.containsBean("mailHealthContributor"));});}
 @Test void missingApiKeyFailsClearly(){runner.withPropertyValues("app.mail-provider=resend").run(ctx->assertNotNull(ctx.getStartupFailure()));}
 @Test void disabledDeliveryNeverReportsSuccess(){runner.withPropertyValues("app.mail-provider=disabled").run(ctx->{assertNull(ctx.getStartupFailure());assertTrue(ctx.getBeansOfType(JavaMailSender.class).isEmpty());assertThrows(MailSendException.class,()->ctx.getBean(EmailDelivery.class).send(new SimpleMailMessage(),"test"));});}
 @Test void httpAdapterSendsExpectedPayloadAndMapsFailuresWithoutSecrets() throws Exception {
  var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
  var status=new java.util.concurrent.atomic.AtomicInteger(200);
  var body=new java.util.concurrent.atomic.AtomicReference<String>();var key=new java.util.concurrent.atomic.AtomicReference<String>();
  server.createContext("/emails",exchange->{body.set(new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));key.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));exchange.sendResponseHeaders(status.get(),-1);exchange.close();});server.start();
  try{
   var sender=new ResendDelivery("test-key",new ObjectMapper(),java.net.http.HttpClient.newHttpClient(),java.net.URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/emails"));
   var message=new SimpleMailMessage();message.setFrom("sender@example.test");message.setTo("guest@example.test");message.setSubject("Confirm");message.setText("private-verification-link");
   sender.send(message,"claim-hash");assertEquals("claim-hash",key.get());assertEquals("guest@example.test",new ObjectMapper().readTree(body.get()).at("/to/0").asText());
   for(int code:new int[]{401,429,500,302}){status.set(code);var error=assertThrows(MailSendException.class,()->sender.send(message,"claim-hash"));assertFalse(error.toString().contains("test-key"));assertFalse(error.toString().contains("private-verification-link"));}
  }finally{server.stop(0);}
 }
}
