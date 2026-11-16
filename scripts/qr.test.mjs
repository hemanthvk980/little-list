import {createRequire} from 'node:module';
import test from 'node:test';
import assert from 'node:assert/strict';
const require=createRequire(new URL('../frontend/package.json',import.meta.url));
const QRCode=require('qrcode'),jsQR=require('jsqr'),{PNG}=require('pngjs');
test('downloadable QR decodes to the exact tracked short link',async()=>{
 const target='https://little-list.example/r/AbCdEf0123456789_-xyZA?via=qr';
 const data=await QRCode.toDataURL(target,{width:768,margin:4,errorCorrectionLevel:'M',color:{dark:'#000000',light:'#ffffff'}});
 const png=PNG.sync.read(Buffer.from(data.split(',')[1],'base64'));
 const decoded=jsQR(new Uint8ClampedArray(png.data),png.width,png.height);
 assert.equal(decoded?.data,target);
 assert.equal(png.width,768);
});
