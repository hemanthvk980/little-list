export const stores=[{name:'Amazon India',url:'https://www.amazon.in/',letter:'a',hint:'Everyday finds, big and small'},{name:'Flipkart',url:'https://www.flipkart.com/',letter:'f',hint:'More choices for your little one'},{name:'FirstCry',url:'https://www.firstcry.com/',letter:'fc',hint:'Baby and parenting essentials'}];
export function productLink(value:string){
 if(!value.trim())return {url:'',merchant:'Local shop / no link',identity:''};
 const u=new URL(value.trim());if(!['http:','https:'].includes(u.protocol)||u.username||u.password)throw Error('Use an http or https product link without embedded credentials.');
 const h=u.hostname.toLowerCase();let merchant='Other store';
 if(h==='amazon.in'||h.endsWith('.amazon.in')||h==='amzn.in')merchant='Amazon India';
 if(h==='flipkart.com'||h.endsWith('.flipkart.com')||h==='fkrt.it')merchant='Flipkart';
 if(h==='firstcry.com'||h.endsWith('.firstcry.com'))merchant='FirstCry';
 for(const key of [...u.searchParams.keys()])if(key.startsWith('utm_'))u.searchParams.delete(key);
 u.hash='';
 // Retain all potentially variant-bearing query parameters. Only use a definite product ID.
 const asin=merchant==='Amazon India'?u.pathname.match(/\/(?:dp|gp\/product)\/([A-Z0-9]{10})(?:\/|$)/i)?.[1]:undefined;
 const pid=merchant==='Flipkart'?u.searchParams.get('pid'):undefined;
 return {url:u.href,merchant,identity:asin?merchant+':'+asin.toUpperCase():pid?merchant+':'+pid:u.href};
}
export type CaptureDraft={name:string;retailerUrl:string;price:string;variant:string};
export function parseCapture(raw:string):CaptureDraft{
 if(raw.length>16000)throw Error('This captured item is too large. Paste the product link instead.');
 const d=JSON.parse(raw);if(!d||typeof d!=='object'||Array.isArray(d))throw Error('Invalid captured item.');
 const field=(key:string,max:number)=>typeof d[key]==='string'?d[key].slice(0,max):'';
 const retailerUrl=productLink(field('retailerUrl',2048)).url;
 if(!retailerUrl)throw Error('The captured item needs a product link.');
 const price=field('price',20);return {name:field('name',120),retailerUrl,variant:field('variant',120),price:/^\d+(\.\d{1,2})?$/.test(price)&&Number(price)<=21474836.47?price:''};
}
