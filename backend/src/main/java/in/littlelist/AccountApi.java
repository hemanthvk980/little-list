package in.littlelist;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1/auth")
class AccountApi {
 private final AccountService accounts;private final AuthRateLimit limits;private final EmailDelivery mail;private final String web,from;
 AccountApi(AccountService accounts,AuthRateLimit limits,EmailDelivery mail,@Value("${app.web-url}")String web,@Value("${app.mail-from}")String from){this.accounts=accounts;this.limits=limits;this.mail=mail;this.web=web;this.from=from;}
 record Signup(@NotBlank @Size(max=100)String name,@NotBlank @Pattern(regexp="[a-zA-Z0-9_]{3,32}")String username,@NotBlank @Email @Size(max=254)String email,@NotBlank @Size(max=512)String password){@Override public String toString(){return "Signup[redacted]";}}
 record Login(@NotBlank @Size(max=32)String username,@NotBlank @Size(max=512)String password){@Override public String toString(){return "Login[redacted]";}}
 record EmailInput(@NotBlank @Email @Size(max=254)String email){}
 record Token(@NotBlank @Pattern(regexp="[a-f0-9]{64}")String token,@Size(max=512)String password){@Override public String toString(){return "Token[redacted]";}}
 private void limit(HttpServletRequest req,String key,int max){
  // Do not trust arbitrary X-Forwarded-For values. Account quotas remain shared across replicas.
  if(!limits.allow("ip|"+req.getRemoteAddr(),300)||!limits.allow(key,max))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please wait before trying again.");
 }
 private void send(AccountService.Mail delivery){
  if(delivery==null)return;
  var m=new SimpleMailMessage();m.setFrom(from);m.setTo(delivery.email());m.setSubject(delivery.purpose().equals("verify")?"Verify your Little List account":"Reset your Little List password");
  m.setText("Open this link, then confirm on the page. It expires in 30 minutes.\n\n"+web+"/account/"+delivery.purpose()+"#token="+delivery.token()+"\n\nIf you did not request this, ignore this email.");
  try{mail.send(m,"account-"+WishlistService.hash(delivery.token()));}catch(RuntimeException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email is unavailable. Please use resend verification or password recovery later.");}
 }
 @PostMapping("/signup") @ResponseStatus(HttpStatus.ACCEPTED)
 void signup(@Valid @RequestBody Signup input,HttpServletRequest req){limit(req,"signup|"+AccountService.normal(input.email()),3);send(accounts.signup(input));}
 @PostMapping("/{purpose:verify-email|forgot-password}") @ResponseStatus(HttpStatus.ACCEPTED)
 void request(@PathVariable String purpose,@Valid @RequestBody EmailInput input,HttpServletRequest req){limit(req,"mail|"+AccountService.normal(input.email()),3);send(accounts.request(input.email(),purpose.equals("verify-email")?"verify":"reset"));}
 @PostMapping("/{purpose:verify|reset}") @ResponseStatus(HttpStatus.NO_CONTENT)
 void consume(@PathVariable String purpose,@Valid @RequestBody Token input,HttpServletRequest req){limit(req,"token|"+WishlistService.hash(input.token()),8);if(purpose.equals("reset")&&input.password()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a new passphrase.");accounts.consume(input.token(),purpose,input.password());}
 @PostMapping("/login") @ResponseStatus(HttpStatus.NO_CONTENT)
 void login(@Valid @RequestBody Login input,HttpServletRequest req,HttpServletResponse res){
  limit(req,"login|"+AccountService.normal(input.username()),10);
  var principal=accounts.login(input.username(),input.password());
  if(principal==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Unable to sign in. Check your credentials and verify your email.");
  req.getSession();req.changeSessionId();
  var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,List.of(new SimpleGrantedAuthority("ROLE_HOST"))));
  SecurityContextHolder.setContext(context);new HttpSessionSecurityContextRepository().saveContext(context,req,res);new HttpSessionCsrfTokenRepository().saveToken(null,req,res);
 }
}
