package in.littlelist;

import java.io.Serializable;
import java.util.UUID;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

record AccountPrincipal(UUID id, int version) implements Serializable {}

/** Check revocation on every request, including sessions issued before a password reset. */
class AccountSessionFilter extends OncePerRequestFilter {
 private final JdbcTemplate db;
 AccountSessionFilter(JdbcTemplate db){this.db=db;}
 protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null && auth.getPrincipal() instanceof AccountPrincipal p) {
   boolean valid=Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from password_account where id=? and version=? and verified)",Boolean.class,p.id(),p.version()));
   if(!valid){SecurityContextHolder.clearContext();if(request.getSession(false)!=null)request.getSession(false).invalidate();}
  }
  chain.doFilter(request,response);
 }
}

@Component
class AuthRateLimit {
 private final JdbcTemplate db;
 AuthRateLimit(JdbcTemplate db){this.db=db;}
 @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
 public boolean allow(String key,int limit) {
  Integer count=db.queryForObject("insert into auth_attempt(bucket,attempts) values(?,1) on conflict(bucket) do update set attempts=case when auth_attempt.window_start<now()-interval '15 minutes' then 1 else auth_attempt.attempts+1 end,window_start=case when auth_attempt.window_start<now()-interval '15 minutes' then now() else auth_attempt.window_start end returning attempts",Integer.class,WishlistService.hash(key));
  return count<=limit;
 }
 @org.springframework.scheduling.annotation.Scheduled(fixedDelay=3600000)
 public void cleanup(){db.update("delete from auth_attempt where window_start<now()-interval '1 day'");db.update("delete from account_token where expires_at<now()");db.update("delete from password_account where not verified and created_at<now()-interval '7 days'");}
}
