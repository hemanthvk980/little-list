package in.littlelist;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("local") @AutoConfigureMockMvc @Testcontainers
class AccountApiTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired MockMvc mvc;@Autowired AccountService accounts;@Autowired JdbcTemplate db;
 String username;final String pass="Sunlit gardens welcome little stars";
 @BeforeEach void setup(){username="u"+UUID.randomUUID().toString().replace("-","").substring(0,15);}
 AccountService.Mail signup(){return accounts.signup(new AccountApi.Signup("Parent",username,username+"@example.test",pass));}
 MockHttpSession login()throws Exception{return (MockHttpSession)mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content("{\"username\":\""+username+"\",\"password\":\""+pass+"\"}")).andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);}
 @Test void verificationRequiredAndTokenSingleUse()throws Exception{
  var mail=signup();assertNull(accounts.login(username,pass));assertFalse(db.queryForObject("select password_hash from password_account where username=?",String.class,username).contains(pass));
  mvc.perform(get("/api/v1/auth/verify?token="+mail.token())).andExpect(status().isMethodNotAllowed());assertNull(accounts.login(username,pass));
  mvc.perform(post("/api/v1/auth/verify").with(csrf()).contentType("application/json").content("{\"token\":\""+mail.token()+"\"}")).andExpect(status().isNoContent());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.consume(mail.token(),"verify",null));
  var session=login();mvc.perform(get("/api/v1/identity").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.identity").value(org.hamcrest.Matchers.startsWith("password|")));
  mvc.perform(get("/api/v1/admin/users").session(session)).andExpect(status().isForbidden());
 }
 @Test void resetRevokesExistingSessionsAndOldPassword()throws Exception{
  var m=signup();accounts.consume(m.token(),"verify",null);var session=login();var reset=accounts.request(m.email(),"reset");accounts.consume(reset.token(),"reset","A different sunset over the hills");
  assertNull(accounts.login(username,pass));assertNotNull(accounts.login(username,"A different sunset over the hills"));mvc.perform(get("/api/v1/identity").session(session)).andExpect(status().isUnauthorized());
 }
 @Test void expiredAndWrongPurposeTokensCannotActivateAccount(){var m=signup();assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.consume(m.token(),"reset",pass));db.update("update account_token set expires_at=now()-interval '1 minute' where token_hash=?",WishlistService.hash(m.token()));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->accounts.consume(m.token(),"verify",null));assertNull(accounts.login(username,pass));}
 @Test void duplicateCannotOverwriteCredentials(){var m=signup();assertNull(accounts.signup(new AccountApi.Signup("Attacker",username,m.email(),"Different credentials from attacker")));accounts.consume(m.token(),"verify",null);assertNotNull(accounts.login(username,pass));assertNull(accounts.login(username,"Different credentials from attacker"));}
 @Test void csrfRequiredAndFailuresThrottled()throws Exception{
  String body="{\"username\":\""+username+"\",\"password\":\"wrong password here\"}";
  mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(body)).andExpect(status().isForbidden());
  for(int i=0;i<10;i++)mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content(body)).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content(body)).andExpect(status().isTooManyRequests());
 }
 @Test void passphrasePolicyAllowsSpacesAndRejectsWeakValues(){assertThrows(org.springframework.web.server.ResponseStatusException.class,()->AccountService.password("Short123!"));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->AccountService.password("passwordpassword"));assertEquals(pass,AccountService.password(pass));assertEquals("é"+" long unique phrase",AccountService.password("e\u0301 long unique phrase"));}
 @Test void privateJourneyRequiresOwnershipAndIsAbsentFromGuestResponse()throws Exception{
  UUID id=UUID.randomUUID();db.update("insert into wishlist(id,owner_subject,title,share_id) values(?,?,?,?)",id,"local|journey-owner","Welcome",id);
  String body="{\"chapter\":\"Adoption\",\"arrivalDate\":\"2027-05-10\",\"multiple\":false,\"experience\":\"first\",\"feeling\":\"guided\",\"countdownEnabled\":true}";
  mvc.perform(put("/api/v1/wishlists/"+id+"/journey").with(user("other")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
  mvc.perform(put("/api/v1/wishlists/"+id+"/journey").with(user("journey-owner")).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/wishlists/"+id+"/journey").with(user("journey-owner"))).andExpect(jsonPath("$.arrivalDate").value("2027-05-10"));
  mvc.perform(get("/api/v1/shared/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.arrivalDate").doesNotExist()).andExpect(jsonPath("$.chapter").doesNotExist());
 }
}
