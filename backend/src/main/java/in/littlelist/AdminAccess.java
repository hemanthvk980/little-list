package in.littlelist;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
class AdminAccess {
 private final String identity;
 private final JdbcTemplate db;
 AdminAccess(@Value("${ADMIN_GOOGLE_IDENTITY:}") String identity, JdbcTemplate db) { this.identity=identity.trim(); this.db=db; }
 boolean allowed(Authentication auth) {
  return !identity.isEmpty() && auth!=null && auth.isAuthenticated()
   && auth.getPrincipal() instanceof OidcUser u
   && "https://accounts.google.com".equals(u.getIssuer().toString())
   && identity.equals(Api.owner(auth));
 }
 void require(Authentication auth) {
  if(!allowed(auth)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Owner access required.");
 }
 void login(Authentication auth) {
  if(auth.getPrincipal() instanceof OidcUser u) {
   db.update("insert into app_user(owner_subject,display_name,email,last_login) values(?,?,?,now()) on conflict(owner_subject) do update set display_name=excluded.display_name,email=excluded.email,last_login=now()",
    Api.owner(auth),trim(u.getFullName(),200),Boolean.TRUE.equals(u.getEmailVerified())?trim(u.getEmail(),254):null);
  }
 }
 private String trim(String s,int max){return s==null?null:s.substring(0,Math.min(s.length(),max));}
 @org.springframework.transaction.annotation.Transactional
 public void audit(Authentication auth,String action,UUID target) {
  require(auth);
  // Database-backed quota applies across replicas. No request parameters or record contents are logged.
  {
   db.queryForList("select pg_advisory_xact_lock(hashtext(?))", "admin-quota|"+Api.owner(auth));
   Long count=db.queryForObject("select count(*) from admin_audit where actor=? and occurred_at>now()-interval '1 minute'",Long.class,Api.owner(auth));
   if(count>=120) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please wait a minute before trying again.");
   db.update("insert into admin_audit(actor,action,target) values(?,?,?)",Api.owner(auth),action,target);
  }
 }
}
