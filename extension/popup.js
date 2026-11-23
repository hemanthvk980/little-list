import {extractProduct} from './extractor.js';
const APP_ORIGIN='https://little-list-30e7.onrender.com';
let imageUrl='';
const field=id=>document.getElementById(id),status=field('status');
field('read').addEventListener('click',async()=>{
 field('read').disabled=true;status.textContent='Reading product suggestions…';
 try{
  const [tab]=await chrome.tabs.query({active:true,currentWindow:true});
  if(!tab?.id)throw Error('Open a product page and try again.');
  const results=await chrome.scripting.executeScript({target:{tabId:tab.id},func:extractProduct});
  const d=results[0]?.result;if(!d)throw Error('No product details found. Paste the link in Little List instead.');
  imageUrl=d.imageUrl||'';field('name').value=d.name;field('url').value=d.retailerUrl;field('price').value=d.price;field('variant').value=d.variant;
  field('review').hidden=false;status.textContent='Please check the name, selected variant and INR estimate.';
 }catch(e){status.textContent='Unable to read this page. Open a product page or paste its link in Little List.';}finally{field('read').disabled=false;}
});
field('review').addEventListener('submit',async e=>{
 e.preventDefault();field('continue').disabled=true;
 try{const u=new URL(field('url').value);if(!['http:','https:'].includes(u.protocol)||u.username||u.password)throw Error('Use a valid product URL.');
 const draft={imageUrl,name:field('name').value.slice(0,120),retailerUrl:u.href,price:field('price').value,variant:field('variant').value.slice(0,120)};
 if(u.href.length>2048)throw Error('This link is too long. Use a shorter product link.');
 const origin=field('destination').value==='local'?'http://localhost:3000':APP_ORIGIN;
 await chrome.tabs.create({url:origin+'/capture#draft='+encodeURIComponent(JSON.stringify(draft))});
 status.textContent='Continue in Little List to select a registry and save.';
 }catch(e){status.textContent=e.message;}finally{field('continue').disabled=false;}
});
