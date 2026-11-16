import test from 'node:test';
import assert from 'node:assert/strict';
import {productLink,parseCapture} from '../frontend/components/retailers.ts';
test('recognizers check hostname boundaries and retain variants',()=>{
 assert.equal(productLink('https://www.amazon.in/dp/B012345678?size=M&utm_source=test').merchant,'Amazon India');
 assert.equal(productLink('https://amazon.in.attacker.example/dp/B012345678').merchant,'Other store');
 const link=productLink('https://www.flipkart.com/item?pid=ABC&size=M&utm_source=test');assert.match(link.url,/size=M/);assert.doesNotMatch(link.url,/utm_source/);assert.equal(link.identity,'Flipkart:ABC');
 assert.equal(productLink('https://www.firstcry.com/item').merchant,'FirstCry');assert.equal(productLink('https://amzn.in/short').merchant,'Amazon India');
});
test('capture rejects executable URLs and embedded credentials',()=>{
 for(const url of ['javascript:alert(1)','file:///etc/passwd','https://name:secret@example.com/item'])assert.throws(()=>parseCapture(JSON.stringify({retailerUrl:url})));
 assert.throws(()=>parseCapture('x'.repeat(16001)));assert.throws(()=>parseCapture('null'));
});
test('capture bounds untrusted fields and does not coerce foreign price strings',()=>{
 const d=parseCapture(JSON.stringify({name:'x'.repeat(1000),retailerUrl:'https://www.firstcry.com/item',price:'$10',variant:'v'.repeat(200),owner:'attacker'}));
 assert.equal(d.name.length,120);assert.equal(d.variant.length,120);assert.equal(d.price,'');assert.equal(d.owner,undefined);
 assert.equal(parseCapture(JSON.stringify({retailerUrl:'https://example.com',price:'1234.50'})).price,'1234.50');
});
