// Self-contained: Chrome serializes this function into an isolated page world.
export function extractProduct() {
  const clean=(v,n)=>typeof v==='string'?v.replace(/\s+/g,' ').trim().slice(0,n):'';
  const text=selector=>clean(document.querySelector(selector)?.textContent||'',300);
  const meta=name=>clean(document.querySelector('meta[property="'+name+'"]')?.getAttribute('content')||document.querySelector('meta[name="'+name+'"]')?.getAttribute('content')||'',300);
  const number=value=>{const s=String(value??'').replace(/,/g,'').trim();return /^\d+(\.\d{1,2})?$/.test(s)&&Number(s)<=21474836.47?s:'';};
  const u=new URL(location.href);
  if(!['http:','https:'].includes(u.protocol)||u.username||u.password)throw Error('Open a public product page first.');
  u.hash='';
  if(u.href.length>2048)throw Error('This product link is too long. Use a shorter link.');
  let product;
  const walk=(value,depth=0)=>{if(depth>5||!value||product)return;if(Array.isArray(value)){value.slice(0,100).forEach(v=>walk(v,depth+1));return;}if(typeof value!=='object')return;const types=Array.isArray(value['@type'])?value['@type']:[value['@type']];if(types.includes('Product')){product=value;return;}if(value['@graph'])walk(value['@graph'],depth+1);};
  for(const node of [...document.querySelectorAll('script[type="application/ld+json"]')].slice(0,20)){
    const raw=node.textContent||'';if(raw.length>200000)continue;try{walk(JSON.parse(raw));}catch{}if(product)break;
  }
  let name=clean(product?.name,120)||clean(meta('og:title'),120)||clean(document.title,120);
  let price='',variant='';
  // Multiple offers and aggregate lowPrice are ambiguous; leave the price editable/empty.
  const offer=product?.offers;
  if(offer&&!Array.isArray(offer)&&offer.priceCurrency==='INR')price=number(offer.price);
  if(!price&&meta('product:price:currency')==='INR')price=number(meta('product:price:amount'));
  const host=u.hostname.toLowerCase();let displayed='';
  if(host==='amazon.in'||host.endsWith('.amazon.in')){
    name=clean(text('#productTitle'),120)||name;
    displayed=text('#corePrice_feature_div .a-price:not(.a-text-price) .a-offscreen')||text('#corePriceDisplay_desktop_feature_div .a-price .a-offscreen');
    variant=clean([text('#variation_size_name .selection'),text('#variation_color_name .selection')].filter(Boolean).join(' · '),120);
  } else if(host==='flipkart.com'||host.endsWith('.flipkart.com')){
    const visible=clean(text('h1'),120);
    name=/\.\.\.more$/i.test(visible)?(clean(product?.name,120)||visible):visible||name;
    displayed=text('.Nx9bqj.CxhGGd');
  } else if(host==='firstcry.com'||host.endsWith('.firstcry.com')){
    name=clean(text('h1'),120)||name;
    displayed=text('#prod_price');
    if(!price)price=number(text('#seoprice'));
  }
  if(!price){const match=displayed.match(/(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)(?![\d.])/i);if(match)price=number(match[1]);}
  if(!variant)variant=clean([product?.size,product?.color].filter(v=>typeof v==='string').join(' · '),120);
  let rawImage=product?.image;if(Array.isArray(rawImage))rawImage=rawImage[0];if(rawImage&&typeof rawImage==='object')rawImage=rawImage.url;
  if(!rawImage&&(host==='amazon.in'||host.endsWith('.amazon.in'))){const landing=document.querySelector('#landingImage');rawImage=landing?.getAttribute('data-old-hires')||landing?.getAttribute('src');}
  let imageUrl='';try{const candidate=new URL(typeof rawImage==='string'?rawImage:meta('og:image'),u);if(candidate.protocol==='https:'&&!candidate.username&&!candidate.password&&candidate.href.length<=2048)imageUrl=candidate.href;}catch{}
  return {name,retailerUrl:u.href.slice(0,2048),price,variant,imageUrl};
}
