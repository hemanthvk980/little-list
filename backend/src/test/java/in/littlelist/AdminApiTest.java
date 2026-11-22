package in.littlelist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="ADMIN_GOOGLE_IDENTITY=https://accounts.google.com|owner-test")
@ActiveProfiles("local") @AutoConfigureMockMvc @Testcontainers
class AdminApiTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired MockMvc mvc; @Autowired JdbcTemplate db; @Autowired AdminAccess access;
 private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor owner(){return oidcLogin().idToken(t->t.subject("owner-test").issuer("https://accounts.google.com"));}
 @Test void allAdminRoutesRejectAnonymousAndOrdinaryUsers() throws Exception {
  String id="00000000-0000-0000-0000-000000000001";
  for(String path:new String[]{"overview","users","users/"+id,"users/"+id+"/registries","registries/"+id,"registries/"+id+"/items","registries/"+id+"/checklist"}){
   mvc.perform(get("/api/v1/admin/"+path)).andExpect(status().isUnauthorized());
   mvc.perform(get("/api/v1/admin/"+path).with(user("owner-test"))).andExpect(status().isForbidden());
   mvc.perform(get("/api/v1/admin/"+path).with(oidcLogin().idToken(t->t.subject("other").issuer("https://accounts.google.com")))).andExpect(status().isForbidden());
  }
 }
 @Test void productPreviewRequiresLoginAndCsrf() throws Exception {
  var post=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/products/preview").contentType("application/json").content("{\"url\":\"https://unsupported.example/product\"}");
  mvc.perform(post.with(csrf())).andExpect(status().isUnauthorized());
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/products/preview").with(user("demo")).contentType("application/json").content("{\"url\":\"https://unsupported.example/product\"}")).andExpect(status().isForbidden());
  mvc.perform(post.with(user("demo")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("")).andExpect(header().string("Cache-Control","no-store"));
 }
 @Test void issuerCannotBeSpoofed() throws Exception {mvc.perform(get("/api/v1/admin/users").with(oidcLogin().idToken(t->t.subject("owner-test").issuer("https://other.example")))).andExpect(status().isForbidden());}
 @Test void pagesAreBoundedAndSensitiveFieldsExcluded() throws Exception {
  db.update("insert into app_user(owner_subject,display_name) values('test-a','Alice'),('test-b','Bob') on conflict do nothing");
  mvc.perform(get("/api/v1/admin/users?hasRegistries=false&size=1&page=0").with(owner())).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].owner_subject").doesNotExist()).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")));
  mvc.perform(get("/api/v1/admin/users?size=101").with(owner())).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/admin/users?page=-1").with(owner())).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/admin/users?q=Alice&hasRegistries=false").with(owner())).andExpect(jsonPath("$.total").value(1));
  assertTrue(db.queryForObject("select count(*) from admin_audit",Long.class)>0);
 }
 @Test void registryDrilldownIsPaginatedWithoutTokens() throws Exception {
  var uid=java.util.UUID.randomUUID(); var wid=java.util.UUID.randomUUID();
  db.update("insert into app_user(id,owner_subject) values(?,?)",uid,"drilldown");
  db.update("insert into wishlist(id,owner_subject,title,share_id) values(?,?,?,?)",wid,"drilldown","Private family",java.util.UUID.randomUUID());
  for(int i=0;i<3;i++)db.update("insert into gift(id,wishlist_id,name,category,quantity) values(?,?,?,?,1)",java.util.UUID.randomUUID(),wid,"Gift "+i,"Clothing");
  db.update("insert into checklist_entry values(?,?,?)",wid,"test-item","have");
  mvc.perform(get("/api/v1/admin/users/"+uid+"/registries").with(owner())).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].share_id").doesNotExist());
  mvc.perform(get("/api/v1/admin/registries/"+wid).with(owner())).andExpect(status().isOk()).andExpect(jsonPath("$.user_id").value(uid.toString())).andExpect(jsonPath("$.share_id").doesNotExist());
  var a=mvc.perform(get("/api/v1/admin/registries/"+wid+"/items?size=1&page=0").with(owner())).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.items.length()").value(1)).andReturn().getResponse().getContentAsString();
  var b=mvc.perform(get("/api/v1/admin/registries/"+wid+"/items?size=1&page=1").with(owner())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertNotEquals(new com.fasterxml.jackson.databind.ObjectMapper().readTree(a).at("/items/0/id"),new com.fasterxml.jackson.databind.ObjectMapper().readTree(b).at("/items/0/id"));
  mvc.perform(get("/api/v1/admin/registries/"+wid+"/checklist").with(owner())).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].state").value("have"));
  mvc.perform(get("/api/v1/admin/registries/"+java.util.UUID.randomUUID()).with(owner())).andExpect(status().isNotFound());
 }
 @Test void readsAreRateLimited() throws Exception {
  db.update("insert into admin_audit(actor,action) select 'https://accounts.google.com|owner-test','quota-test' from generate_series(1,120)");
  try{mvc.perform(get("/api/v1/admin/overview").with(owner())).andExpect(status().isTooManyRequests());}
  finally{db.update("delete from admin_audit where action='quota-test'");}
 }
 @Test void emptyAllowlistDisablesAdmin() {
  var principal=new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(java.util.List.of(),org.springframework.security.oauth2.core.oidc.OidcIdToken.withTokenValue("test").subject("owner-test").issuer("https://accounts.google.com").build());
  var auth=new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(principal,java.util.List.of(),"google");
  assertFalse(new AdminAccess("",db).allowed(auth));
  access.login(auth);access.login(auth);
  assertEquals(1,db.queryForObject("select count(*) from app_user where owner_subject='https://accounts.google.com|owner-test'",Integer.class));
 }
}
