import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {readFileSync} from 'node:fs';
import {extractProduct} from '../extension/extractor.js';
function extract({url='https://www.amazon.in/dp/B012345678',json=[],nodes={},title='A product'}={}){
 const document={title,querySelector:s=>nodes[s]?{textContent:nodes[s],getAttribute:()=>nodes[s]}:null,querySelectorAll:()=>json.map(j=>({textContent:typeof j==='string'?j:JSON.stringify(j)}))};
 return JSON.parse(JSON.stringify(vm.runInNewContext('('+extractProduct.toString()+')()',{document,location:{href:url},URL})));
}
test('Amazon selected variant and explicit rupee price',()=>{
 const p=extract({nodes:{'#productTitle':'Cotton outfit','#corePriceDisplay_desktop_feature_div .a-price .a-offscreen':'₹1,299.50','#variation_size_name .selection':'0–3 months','#variation_color_name .selection':'Sage'}});
 assert.equal(p.price,'1299.50');assert.equal(p.variant,'0–3 months · Sage');assert.equal(p.name,'Cotton outfit');
});
test('Flipkart JSON-LD INR price and visible title',()=>{
 const p=extract({url:'https://www.flipkart.com/item/p/itm123?pid=ABC',json:[{'@graph':[{'@type':'Product',name:'Metadata name',offers:{priceCurrency:'INR',price:'1,25,000.00'}}]}],nodes:{h1:'Visible selected product'}});
 assert.equal(p.price,'125000.00');assert.equal(p.name,'Visible selected product');assert.match(p.retailerUrl,/pid=ABC/);
});
test('FirstCry malformed metadata falls back safely',()=>{
 const p=extract({url:'https://www.firstcry.com/item/123/product-detail',json:['{broken'],nodes:{h1:'Bath towel','#prod_price':'Rs. 599.00'}});assert.equal(p.name,'Bath towel');assert.equal(p.price,'599.00');
});
test('foreign currencies and ambiguous multi-offer prices are not relabelled INR',()=>{
 assert.equal(extract({json:[{'@type':'Product',offers:{priceCurrency:'USD',price:20}}]}).price,'');
 assert.equal(extract({json:[{'@type':'Product',offers:[{priceCurrency:'INR',price:20},{priceCurrency:'INR',price:40}]}]}).price,'');
});
test('untrusted strings are bounded and do not execute',()=>{
 const p=extract({json:[{'@type':'Product',name:'<script>alert(1)</script>'+'x'.repeat(500),offers:{priceCurrency:'INR',price:'10;alert(1)'}}]});assert.equal(p.name.length,120);assert.equal(p.price,'');
 assert.throws(()=>extract({url:'chrome://extensions'}));assert.throws(()=>extract({url:'https://user:pass@example.com/'}));
});
test('manifest has no broad or persistent permissions',()=>{
 const manifest=JSON.parse(readFileSync(new URL('../extension/manifest.json',import.meta.url)));
 assert.equal(manifest.manifest_version,3);assert.deepEqual(manifest.permissions,['activeTab','scripting']);assert.equal(manifest.host_permissions,undefined);assert.equal(manifest.content_scripts,undefined);
});
test('popup reads only on click and hands reviewed data to the fixed app origin',async()=>{
 const elements={};let reads=0,opened='';
 const element=id=>elements[id]??=( {value:'',hidden:true,disabled:false,textContent:'',listeners:{},addEventListener(event,fn){this.listeners[event]=fn;}} );
 const source=readFileSync(new URL('../extension/popup.js',import.meta.url),'utf8').replace("import {extractProduct} from './extractor.js';",'');
 const chrome={tabs:{query:async()=>[{id:42}],create:async({url})=>{opened=url;}},scripting:{executeScript:async()=>{reads++;return [{result:{name:'Towel',retailerUrl:'https://www.firstcry.com/towel',price:'599',variant:'Cream'}}];}}};
 vm.runInNewContext(source,{document:{getElementById:element},chrome,extractProduct,URL,encodeURIComponent});
 assert.equal(reads,0);await elements.read.listeners.click();assert.equal(reads,1);assert.equal(elements.review.hidden,false);
 elements.name.value='Reviewed towel';await elements.review.listeners.submit({preventDefault(){}});
 const url=new URL(opened);assert.equal(url.origin,'https://little-list-30e7.onrender.com');assert.equal(url.pathname,'/capture');assert.equal(url.search,'');
 const draft=JSON.parse(new URLSearchParams(url.hash.slice(1)).get('draft'));assert.equal(draft.name,'Reviewed towel');assert.equal(draft.price,'599');
 elements.destination.value='local';await elements.review.listeners.submit({preventDefault(){}});assert.equal(new URL(opened).origin,'http://localhost:3000');
});

test('extension captures structured product image without downloading it',()=>{
 assert.equal(extract({json:[{'@type':'Product',name:'Book',image:['https://m.media-amazon.com/images/I/book.jpg']}]}).imageUrl,'https://m.media-amazon.com/images/I/book.jpg');
});

test('Amazon alternate core price and landing image are captured',()=>{
 const p=extract({url:'https://www.amazon.in/First-Kick-Blankets-Printed-Blanket/dp/B0FH79G2MW/',nodes:{'#productTitle':'First Kick Kids Blanket','#corePrice_feature_div .a-price:not(.a-text-price) .a-offscreen':'₹499.00','#landingImage':'https://m.media-amazon.com/images/I/61zQfj6RxDL._SL1024_.jpg'}});
 assert.equal(p.price,'499.00');assert.equal(p.imageUrl,'https://m.media-amazon.com/images/I/61zQfj6RxDL._SL1024_.jpg');
});

test('FirstCry decimal price excludes adjacent membership price',()=>{
 const p=extract({url:'https://www.firstcry.com/brand/item/41848/product-detail',nodes:{h1:'Toothpaste','#seoprice':'138.33','.clubpriceimg':'13515'}});
 assert.equal(p.price,'138.33');
});
test('Flipkart truncated heading uses complete structured name',()=>{
 const p=extract({url:'https://www.flipkart.com/item/p/itm123',json:[{'@type':'Product',name:'Complete blanket name'}],nodes:{h1:'Blanket...more'}});
 assert.equal(p.name,'Complete blanket name');
});
