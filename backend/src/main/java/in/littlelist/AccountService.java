package in.littlelist;

import java.util.*;
import java.security.SecureRandom;
import java.text.Normalizer;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
class AccountService {
 private final JdbcTemplate db; private final PasswordScreen screen;
 // OWASP's PBKDF2-HMAC-SHA256 work factor; random 128-bit salt per password.
 private final Pbkdf2PasswordEncoder encoder=new Pbkdf2PasswordEncoder("",16,600000,Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
 private final String dummy=encoder.encode("not a real account password");
 AccountService(JdbcTemplate db,PasswordScreen screen){this.db=db;this.screen=screen;}
 record Mail(String email,String token,String purpose){@Override public String toString(){return "Mail[redacted]";}}
 static String normal(String value){return value.trim().toLowerCase(Locale.ROOT);}
 static String password(String value){
  String p=Normalizer.normalize(value,Normalizer.Form.NFC);
  int length=p.codePointCount(0,p.length());
  if(length<15||length>128)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a passphrase of 15 to 128 characters.");
  String lower=p.toLowerCase(Locale.ROOT);
  if(Set.of("passwordpassword","password123456789","123456789012345","1234567890123456","qwertyuiopasdfgh","iloveyouiloveyou","letmeinletmeinletmein","littlelistpassword","littlelist123456").contains(lower) || p.codePoints().distinct().count()<4)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a less predictable passphrase.");
  return p;
 }
 private Mail token(UUID id,String email,String purpose){
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=HexFormat.of().formatHex(bytes);
  db.update("delete from account_token where account_id=? and purpose=?",id,purpose);
  db.update("insert into account_token values(?,?,?,now()+interval '30 minutes')",WishlistService.hash(token),id,purpose);
  return new Mail(email,token,purpose);
 }
 @Transactional public Mail signup(AccountApi.Signup input){
  String pass=password(input.password()), username=normal(input.username()),email=normal(input.email());
  // Hash even rejected registrations. Never overwrite credentials of an existing account.
  screen.check(pass);
  String hash=encoder.encode(pass);
  UUID id=UUID.randomUUID();
  int inserted=db.update("insert into password_account(id,username,email,display_name,password_hash) values(?,?,?,?,?) on conflict do nothing",id,username,email,input.name().trim(),hash);
  return inserted==0?null:token(id,email,"verify");
 }
 @Transactional public Mail request(String email,String purpose){
  var rows=db.queryForList("select * from password_account where email=? for update",normal(email));
  if(rows.isEmpty())return null;
  var row=rows.getFirst(); boolean verified=(Boolean)row.get("verified");
  if(verified!=purpose.equals("reset"))return null;
  return token((UUID)row.get("id"),(String)row.get("email"),purpose);
 }
 @Transactional public void consume(String token,String purpose,String password){
  if(purpose.equals("reset"))screen.check(password(password));
  // Lock account first so competing tokens and resets serialize consistently.
  var ids=db.queryForList("select account_id from account_token where token_hash=? and purpose=?",WishlistService.hash(token),purpose);
  if(ids.isEmpty())throw invalidToken();
  UUID id=(UUID)ids.getFirst().get("account_id");
  db.queryForList("select id from password_account where id=? for update",id);
  int consumed=db.update("delete from account_token where token_hash=? and purpose=? and expires_at>now()",WishlistService.hash(token),purpose);
  if(consumed==0)throw invalidToken();
  if(purpose.equals("verify")) {
   db.update("update password_account set verified=true where id=?",id);
   db.update("insert into app_user(owner_subject,display_name,email) select 'password|'||id,display_name,email from password_account where id=? on conflict do nothing",id);
  } else {
   db.update("update password_account set password_hash=?,version=version+1 where id=?",encoder.encode(password(password)),id);
   db.update("delete from account_token where account_id=?",id);
  }
 }
 private ResponseStatusException invalidToken(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"This link is invalid or expired. Request a new email.");}
 @Transactional public AccountPrincipal login(String username,String password){
  var rows=db.queryForList("select * from password_account where username=?",normal(username));
  var row=rows.isEmpty()?null:rows.getFirst();
  boolean matches=encoder.matches(Normalizer.normalize(password,Normalizer.Form.NFC),row==null?dummy:(String)row.get("password_hash"));
  if(!matches||row==null||!(Boolean)row.get("verified"))return null;
  UUID id=(UUID)row.get("id");
  db.update("update app_user set last_login=now() where owner_subject=?","password|"+id);
  return new AccountPrincipal(id,(Integer)row.get("version"));
 }
}
