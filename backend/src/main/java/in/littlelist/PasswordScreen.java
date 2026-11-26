package in.littlelist;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Only a five-character SHA-1 prefix leaves the server; never a password or full hash. */
@Component
class PasswordScreen {
 private final boolean enabled;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
 PasswordScreen(@Value("${app.password-breach-check:true}")boolean enabled){this.enabled=enabled;}
 void check(String password){
  if(!enabled)return;
  try{
   String hash=HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-1").digest(password.getBytes(StandardCharsets.UTF_8)));
   var request=HttpRequest.newBuilder(URI.create("https://api.pwnedpasswords.com/range/"+hash.substring(0,5))).timeout(Duration.ofSeconds(5)).header("Add-Padding","true").header("User-Agent","LittleList-PasswordScreen").GET().build();
   var response=client.send(request,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=200||response.body().length()>1000000)throw new IllegalStateException();
   if(compromised(hash.substring(5),response.body()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This password appears in known breaches. Choose a different, unique passphrase.");
  }catch(ResponseStatusException e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Password safety checking is temporarily unavailable. Please try again shortly.");}
 }
 static boolean compromised(String suffix,String response){return response.lines().anyMatch(line->{String[] pair=line.trim().split(":");return pair.length==2&&pair[0].equalsIgnoreCase(suffix)&&pair[1].matches("[0-9]+")&&!pair[1].matches("0+");});}
}
