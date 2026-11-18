/** Real ApiClient logic with controlled HTTP responses; no React, network or PostgreSQL.
 * Checks authorization, refresh rotation, concurrency and safe failure modes. */
import assert from 'node:assert/strict';
import {resolve} from 'node:path';
import {readFileSync} from 'node:fs';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
// Prefer the project-installed TypeScript; fall back to the global CLI for offline auditing.
let ts;
try { ts=require('typescript'); }
catch { const {execFileSync}=await import('node:child_process');
  const globalDir=execFileSync('npm',['root','-g'],{encoding:'utf8'}).trim();
  ts=require(resolve(globalDir,'typescript'));
}
const source=readFileSync(new URL('../src/core/api.ts',import.meta.url),'utf8')
 .replaceAll('import.meta.env.VITE_API_BASE_URL', 'globalThis.__supplierTestBase');
const output=ts.transpileModule(source,{fileName:'api.ts',compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS}}).outputText;
const SESSION=new Map();
globalThis.__supplierTestBase='/api/v1';
globalThis.sessionStorage={getItem:k=>SESSION.get(k)??null,setItem:(k,v)=>SESSION.set(k,String(v)),removeItem:k=>SESSION.delete(k)};
const ok=(data,status=200,extra={})=>new Response(status===204?null:JSON.stringify(data),{status,headers:{'content-type':'application/json',...extra}});
const fail=(status,message)=>ok({message},status);
function setup(handler){SESSION.clear();const module={exports:{}};globalThis.fetch=handler;new Function('module','exports',output)(module,module.exports);return module.exports.api;}
const hits=[];let assertions=0;
{
 const api=setup(async(url,init)=>{hits.push({url,init});
  if(url.endsWith('/auth/login'))return ok({accessToken:'a1',refreshToken:'r1',tokenType:'Bearer',expiresInSeconds:900});
  return ok({id:'record-1'});
 });
 const tokens=await api.login('tester','S3cretPass!');assert.equal(tokens.accessToken,'a1');assertions++;
 const value=await api.get('/suppliers/example');assert.equal(value.id,'record-1');assertions++;
 assert.equal(hits.at(-1).init.headers.Authorization,'Bearer a1');assertions++;
 assert.equal(JSON.parse(hits[0].init.body).password,'S3cretPass!');assertions++;
 assert.equal(SESSION.get('supplierhub_refresh'),'r1');assertions++;
}
{
 let refreshCount=0,getCount=0;
 const api=setup(async(url,init)=>{
  if(url.endsWith('/auth/login'))return ok({accessToken:'stale',refreshToken:'oldRefresh',tokenType:'Bearer',expiresInSeconds:900});
  if(url.endsWith('/auth/refresh')){refreshCount++;assert.equal(JSON.parse(init.body).refreshToken,'oldRefresh');return ok({accessToken:'renewed',refreshToken:'newRefresh',tokenType:'Bearer',expiresInSeconds:900});}
  if(url.endsWith('/suppliers')){getCount++;return init.headers.Authorization==='Bearer stale'?fail(401,'Токен истёк'):ok({items:[],totalPages:0,totalElements:0});}
  throw Error('Unexpected '+url);
 });
 await api.login('tester','password');
 const rows=await Promise.all(Array.from({length:3},()=>api.get('/suppliers')));
 assert.equal(rows.length,3);assert.equal(refreshCount,1);assert.equal(getCount,6);assert.equal(SESSION.get('supplierhub_refresh'),'newRefresh');assertions+=4;
}
{
 let expired=0;
 const api=setup(async(url,init)=>{
  if(url.endsWith('/auth/login'))return ok({accessToken:'expired',refreshToken:'rejected',tokenType:'Bearer',expiresInSeconds:900});
  if(url.endsWith('/auth/refresh'))return fail(401,'Повторное использование токена');
  return fail(401,'Unauthorized');
 });
 api.onExpire(()=>{expired++;});await api.login('tester','password');
 await assert.rejects(()=>api.get('/auth/me'),e=>e.status===401);assertions++;
 assert.equal(expired,1);assert.equal(api.hasRefresh(),false);assertions+=2;
}
{
 let refreshCount=0;
 const api=setup(async(url)=>{
  if(url.endsWith('/auth/login'))return ok({accessToken:'valid',refreshToken:'keep',tokenType:'Bearer',expiresInSeconds:900});
  if(url.endsWith('/auth/refresh')){refreshCount++;return fail(401,'none');}
  if(url.endsWith('/admin/audit'))return fail(403,'Недостаточно прав');
  return new Response(null,{status:204});
 });
 await api.login('tester','password');
 await assert.rejects(()=>api.get('/admin/audit'),e=>e.status===403);assertions++;
 assert.equal(refreshCount,0);assert.equal(api.hasRefresh(),true);assertions+=2;
 const noContent=await api.post('/auth/logout');assert.equal(noContent,undefined);assertions++;
}
{
 const api=setup(async()=>{throw Error('network disconnected');});
 await assert.rejects(()=>api.get('/suppliers'),e=>e.status===0&&e.message.includes('Сервер недоступен'));assertions++;
}
{
 const api=setup(async(url)=>url.endsWith('/auth/login')?ok({accessToken:'x',refreshToken:'y',tokenType:'Bearer',expiresInSeconds:900}):fail(401,'Сессия отозвана'));
 await api.login('tester','password');await assert.rejects(()=>api.logout());assert.equal(api.hasRefresh(),false);assertions+=2;
}
{
 // The client must automatically renew access from the stored refresh token after a page reload.
 const api=setup(async(url,init)=>{
  if(url.endsWith('/auth/refresh'))return ok({accessToken:'restored',refreshToken:'rotated',tokenType:'Bearer',expiresInSeconds:900});
  assert.equal(init.headers.Authorization,'Bearer restored');return ok({user:{id:'x'},roles:[]});
 });
 SESSION.set('supplierhub_refresh','persisted');
 const me=await api.get('/auth/me');assert.equal(me.user.id,'x');assert.equal(SESSION.get('supplierhub_refresh'),'rotated');assertions+=2;
}
console.log(`PASS: ${assertions} HTTP-client assertions: login, JWT bearer, 401 refresh retry, parallel refresh, logout, 403, 204, offline, session restore.`);
