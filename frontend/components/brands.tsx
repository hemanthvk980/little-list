"use client";
import {useState} from 'react';
import Link from 'next/link';
const brands=[
 {name:'Chicco',category:'Baby gear',url:'https://www.chicco.in/'},
 {name:'Mee Mee',category:'Everyday essentials',url:'https://meemee.in/'},
 {name:'Pigeon',category:'Feeding',url:'https://pigeon-in.com/'},
 {name:'R for Rabbit',category:'Baby gear',url:'https://rforrabbit.com/'},
 {name:'Babyhug',category:'Clothing',url:'https://www.firstcry.com/search?q=Babyhug'},
 {name:'Pampers',category:'Diapering',url:'https://www.firstcry.com/search?q=Pampers'},
 {name:'Huggies',category:'Diapering',url:'https://www.firstcry.com/search?q=Huggies'},
 {name:'Fisher-Price',category:'Play',url:'https://www.firstcry.com/search?q=Fisher-Price'},
 {name:'Mothercare',category:'Clothing',url:'https://www.firstcry.com/search?q=Mothercare'},
].sort((a,b)=>a.name.localeCompare(b.name));
export function Brands(){const [query,setQuery]=useState(''),[category,setCategory]=useState('All categories'),[letter,setLetter]=useState('All');const visible=brands.filter(b=>b.name.toLowerCase().includes(query.toLowerCase())&&(category==='All categories'||b.category===category)&&(letter==='All'||b.name.startsWith(letter)));
 return <main className="workspace"><p className="eyebrow">LITTLE FINDS, FAMILIAR NAMES</p><h1>Find a favourite.</h1><p>Explore baby brands available in India. Open a store, choose a product, then paste its link into your registry.</p><Link href="/dashboard">Back to my registries</Link><div className="gift-filters"><label>Search brands<input type="search" value={query} onChange={e=>setQuery(e.target.value)} placeholder="Find a brand…"/></label><label>Category<select value={category} onChange={e=>setCategory(e.target.value)}>{['All categories',...new Set(brands.map(b=>b.category))].map(c=><option key={c}>{c}</option>)}</select></label><label>Starts with<select value={letter} onChange={e=>setLetter(e.target.value)}>{['All',...new Set(brands.map(b=>b.name[0]))].map(c=><option key={c}>{c}</option>)}</select></label></div><div className="brand-grid">{visible.map(b=><a key={b.name} href={b.url} target="_blank" rel="noopener noreferrer" className="panel"><span className="badge">{b.category}</span><h2>{b.name}</h2><span>{b.url.includes('firstcry')?'Browse on FirstCry':'Visit brand website'} ↗</span></a>)}</div>{!visible.length&&<p role="status">No brands match. Try another search or category.</p>}<p className="quiet">These are browsing links, not a live product catalogue or partnership endorsement. Product details may need the Chrome button or manual entry.</p></main>;
}
