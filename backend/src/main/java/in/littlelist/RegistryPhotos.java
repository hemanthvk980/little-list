package in.littlelist;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
class RegistryPhotos {
 private final JdbcTemplate db;private final WishlistService service;
 RegistryPhotos(JdbcTemplate db,WishlistService service){this.db=db;this.service=service;}
 static byte[] normalize(byte[] bytes){
  if(bytes.length==0||bytes.length>5*1024*1024)throw bad("Choose a JPEG or PNG up to 5 MB.");
  try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
   var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw bad("Choose a valid JPEG or PNG photo.");
   var reader=readers.next();
   try{
    if(!Set.of("jpeg","jpg","png").contains(reader.getFormatName().toLowerCase(Locale.ROOT)))throw bad("Only JPEG and PNG photos are supported.");
    reader.setInput(stream,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);
    if(w<1||h<1||(long)w*h>16_000_000)throw bad("Choose a photo with at most 16 million pixels.");
    BufferedImage source=reader.read(0);double scale=Math.min(1,512.0/Math.max(w,h));
    BufferedImage result=new BufferedImage(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),BufferedImage.TYPE_INT_RGB);
    var g=result.createGraphics();try{g.setColor(Color.WHITE);g.fillRect(0,0,result.getWidth(),result.getHeight());g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,0,0,result.getWidth(),result.getHeight(),null);}finally{g.dispose();}
    var out=new ByteArrayOutputStream();ImageIO.write(result,"jpeg",out);return out.toByteArray();
   }finally{reader.dispose();}
  }catch(IOException|IllegalArgumentException ex){throw bad("This photo could not be read. Try another JPEG or PNG.");}
 }
 private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 @PostMapping(value="/wishlists/{id}/photo",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 @Transactional
 WishlistService.Wishlist upload(Authentication auth,@PathVariable UUID id,@RequestParam MultipartFile file)throws IOException{
  service.get(Api.owner(auth),id);byte[] photo=normalize(file.getBytes());
  db.update("update wishlist set photo=?,photo_version=? where id=?",photo,UUID.randomUUID(),id);return service.get(Api.owner(auth),id);
 }
 @DeleteMapping("/wishlists/{id}/photo") @Transactional
 WishlistService.Wishlist remove(Authentication auth,@PathVariable UUID id){service.get(Api.owner(auth),id);db.update("update wishlist set photo=null,photo_version=null where id=?",id);return service.get(Api.owner(auth),id);}
 @GetMapping("/wishlists/{id}/photo") @Transactional
 ResponseEntity<byte[]> own(Authentication auth,@PathVariable UUID id){service.get(Api.owner(auth),id);return photo("id",id);}
 @GetMapping("/shared/{id}/photo")
 ResponseEntity<byte[]> shared(@PathVariable UUID id){return photo("share_id",id);}
 private ResponseEntity<byte[]> photo(String column,UUID id){
  var rows=db.queryForList("select photo from wishlist where "+column+"=? and photo is not null",id);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Photo unavailable.");
  return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body((byte[])rows.getFirst().get("photo"));
 }
}
