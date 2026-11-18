/** Static traceability test: every Stage 4 HTTP operation must be reachable from a
 * screen/workflow and its source must contain the corresponding request mechanism.
 * It does NOT claim that the server/database executed the operation. */
import {readFileSync,writeFileSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
// Prefer the project-installed TypeScript; fall back to the global CLI for offline auditing.
let ts;
try { ts=require('typescript'); }
catch { const {execFileSync}=await import('node:child_process');
  const globalDir=execFileSync('npm',['root','-g'],{encoding:'utf8'}).trim();
  ts=require(resolve(globalDir,'typescript'));
}
const dir=resolve(dirname(new URL(import.meta.url).pathname),'..');
const openapi=JSON.parse(readFileSync(resolve(dir,'docs/backend-openapi-stage4.json')));
const read=file=>readFileSync(resolve(dir,'src',file),'utf8');
const source={
 'core/api.ts':read('core/api.ts'),'core/auth.tsx':read('core/auth.tsx'),'core/resources.ts':read('core/resources.ts'),
 'core/operationsApi.ts':read('core/operationsApi.ts'),'pages/Forms.tsx':read('pages/Forms.tsx'),
 'pages/Details.tsx':read('pages/Details.tsx'),'pages/Operations.tsx':read('pages/Operations.tsx'),
 'pages/Workflows.tsx':read('pages/Workflows.tsx'),'pages/Registry.tsx':read('pages/Registry.tsx'),
 'pages/Dashboard.tsx':read('pages/Dashboard.tsx'),'pages/Reports.tsx':read('pages/Reports.tsx'),
 'ui/Layout.tsx':read('ui/Layout.tsx'),'App.tsx':read('App.tsx')};
const compiled=ts.transpileModule(source['core/resources.ts'],{compilerOptions:{module:ts.ModuleKind.ESNext}}).outputText;
const {resources}=await import('data:text/javascript;base64,'+Buffer.from(compiled).toString('base64'));
const evidence=new Map();
const add=(method,path,file,marker)=>{
 const key=`${method.toUpperCase()} /api/v1${path}`;
 if(!source[file]?.includes(marker))throw Error(`${key}: маркер вызова ${marker} отсутствует в ${file}`);
 if(!openapi.paths[`/api/v1${path}`]?.[method.toLowerCase()])throw Error(`${key}: операции нет в контракте`);
 evidence.set(key,{method,path:`/api/v1${path}`,component:file,marker});
};
const byPath=(p,m)=>Object.keys(openapi.paths[p]||{}).includes(m.toLowerCase());
// Every table query and every detail view is provided through the shared data-access template.
for(const r of resources){
 add('GET',r.endpoint,'core/resources.ts',`endpoint:'${r.endpoint}'`);
 if(r.detail)add('GET',r.endpoint+'/{id}','pages/Details.tsx','`${resource.endpoint}/${id}`');
}
for(const p of ['/dashboard','/admin/dashboard'])add('GET',p,'pages/Dashboard.tsx',`'${p}'`);
add('GET','/reports/suppliers','pages/Reports.tsx',"'/reports/suppliers'");
add('GET','/reports/suppliers.xlsx','pages/Reports.tsx',"'/reports/suppliers.xlsx'");
for(const [m,p,sourceFile,marker] of [
 ['POST','/auth/login','core/api.ts',"'/auth/login'"],['POST','/auth/refresh','core/api.ts',"'/auth/refresh'"],
 ['GET','/auth/me','core/auth.tsx',"'/auth/me'"],['POST','/auth/logout','core/api.ts',"'/auth/logout'"],
 ['POST','/suppliers','pages/Forms.tsx',"'/suppliers'"],['PUT','/suppliers/{id}','pages/Forms.tsx','`/suppliers/${editId}`'],
 ['POST','/catalog/categories','pages/Forms.tsx','`/catalog/${id}`'],
 ['POST','/catalog/units','pages/Forms.tsx','`/catalog/${id}`'],
 ['POST','/catalog/products','pages/Forms.tsx',"'/catalog/products'"],
 ['POST','/contracts','pages/Forms.tsx',"'/contracts'"],
 ['POST','/orders','pages/Forms.tsx',"'/orders'"],
 ['POST','/relationships/interactions','pages/Forms.tsx',"'/relationships/interactions'"],
 ['POST','/admin/users','pages/Forms.tsx',"'/admin/users'"],
 ['GET','/suppliers/{id}/products','pages/Details.tsx','`/suppliers/${id}/products`'],
 ['POST','/suppliers/{id}/products','pages/Details.tsx','`/suppliers/${id}/products`'],
 ['GET','/ratings/supplier/{id}','pages/Details.tsx','`/ratings/supplier/${id}`'],
 ['GET','/contracts/{id}/versions','pages/Details.tsx','`/contracts/${id}/versions`'],
 ['GET','/orders/{id}/versions','pages/Details.tsx','`/orders/${id}/versions`'],
 ['GET','/orders/versions/{id}/lines','pages/Workflows.tsx','`/orders/versions/${approved.id}/lines`'],
 ['GET','/fulfillment/deliveries/{id}/lines','pages/Details.tsx','`/fulfillment/deliveries/${id}/lines`'],
 ['GET','/fulfillment/acceptances/{id}/lines','pages/Details.tsx','`/fulfillment/acceptances/${id}/lines`'],
 ['GET','/fulfillment/returns/{id}/lines','pages/Details.tsx','`/fulfillment/returns/${id}/lines`'],
 ['GET','/ratings/{id}/inputs','pages/Details.tsx','`/ratings/${id}/inputs`'],
 ['GET','/admin/users/{id}/roles','pages/Details.tsx','`/admin/users/${id}/roles`'],
 ['POST','/suppliers/{id}/status-requests','pages/Details.tsx','`/suppliers/${id}/status-requests`'],
 ['POST','/suppliers/status-requests/{id}/decision','pages/Registry.tsx','`/suppliers/status-requests/${review.id}/decision`'],
 ['POST','/contracts/versions/{id}/submit','pages/Details.tsx','`/contracts/versions/${item.id}/submit`'],
 ['POST','/contracts/versions/{id}/decision','pages/Details.tsx','`/contracts/versions/${item.id}/decision`'],
 ['POST','/contracts/{id}/terminate','pages/Details.tsx','`/contracts/${id}/terminate`'],
 ['POST','/orders/versions/{id}/submit','pages/Details.tsx','`/orders/versions/${item.id}/submit`'],
 ['POST','/orders/versions/{id}/decision','pages/Details.tsx','`/orders/versions/${item.id}/decision`'],
 ['POST','/orders/{id}/close','pages/Details.tsx','`/orders/${id}/close`'],
 ['POST','/orders/{id}/reopen','pages/Details.tsx','`/orders/${id}/reopen`'],
 ['POST','/fulfillment/orders/{orderId}/deliveries','pages/Workflows.tsx','`/fulfillment/orders/${id}/deliveries`'],
 ['POST','/fulfillment/deliveries/{id}/acceptances','pages/Workflows.tsx','`/fulfillment/deliveries/${id}/acceptances`'],
 ['POST','/fulfillment/deliveries/{id}/confirm','pages/Details.tsx','`/fulfillment/deliveries/${id}/confirm`'],
 ['POST','/fulfillment/acceptances/{id}/confirm','pages/Details.tsx','`/fulfillment/acceptances/${id}/confirm`'],
 ['PATCH','/catalog/products/{id}/active','pages/Details.tsx','`/catalog/products/${id}/active`'],
 ['POST','/fulfillment/returns/{id}/submit','pages/Details.tsx','`/fulfillment/returns/${id}/submit`'],
 ['POST','/fulfillment/corrections/{id}/submit','pages/Details.tsx','`/fulfillment/corrections/${id}/submit`'],
 ['POST','/ratings/expert-assessments/{id}/submit','pages/Details.tsx','`/ratings/expert-assessments/${id}/submit`'],
 ['POST','/fulfillment/returns/{id}/decision','pages/Details.tsx','`${prefix}/${id}/decision`'],
 ['POST','/fulfillment/corrections/{id}/decision','pages/Details.tsx','`${prefix}/${id}/decision`'],
 ['POST','/ratings/expert-assessments/{id}/decision','pages/Details.tsx','`${prefix}/${id}/decision`'],
 ['POST','/relationships/claims/{id}/begin-resolution','pages/Details.tsx','`/relationships/claims/${id}/begin-resolution`'],
 ['POST','/admin/users/{id}/block','pages/Details.tsx',"'block':'unblock'"],
 ['POST','/admin/users/{id}/unblock','pages/Details.tsx',"'block':'unblock'"],
 ['DELETE','/admin/users/{id}/roles/{role}','pages/Details.tsx','`/admin/users/${id}/roles/${item.role}`'],
])add(m,p,sourceFile,marker);
const operations={
 'password-change':['POST','/auth/password'],
 'user-reset':['POST','/admin/users/{id}/reset-password'],
 'user-add-role':['POST','/admin/users/{id}/roles'],
 'contract-revision':['POST','/contracts/{id}/versions'],
 'order-draft':['PUT','/orders/{id}/draft'],
 'order-revise':['POST','/orders/{id}/revise-rejected'],
 'order-increase':['POST','/orders/{id}/increase'],
 'order-reschedule':['POST','/orders/{id}/reschedule'],
 'return-create':['POST','/fulfillment/orders/{id}/returns'],
 'delivery-correction':['POST','/fulfillment/deliveries/{id}/correction'],
 'acceptance-correction':['POST','/fulfillment/acceptances/{id}/correction'],
 'claim-send':['POST','/relationships/claims/{id}/send'],
 'claim-resolve':['POST','/relationships/claims/{id}/resolve'],
 'claim-create':['POST','/relationships/claims'],
 'assessment-create':['POST','/ratings/expert-assessments'],
 'rating-calculate':['POST','/ratings/calculate']
};
for(const [kind,[method,path]] of Object.entries(operations)){
 const src=source['core/operationsApi.ts'];
 if(!src.includes(`case '${kind}':`))throw Error(`${kind}: нет request builder`);
 if(!source['pages/Operations.tsx'].includes(`'${kind}':`) && !source['pages/Operations.tsx'].includes(`'${kind}'`))throw Error(`${kind}: нет заголовка формы`);
 const linked=Object.values(source).some(s=>s.includes(`operation/${kind}`));
 if(!linked)throw Error(`${kind}: недостижимый экран`);
 add(method,path,'core/operationsApi.ts',`case '${kind}':`);
}
const expected=Object.entries(openapi.paths).flatMap(([path,ops])=>Object.keys(ops).filter(m=>['get','post','put','patch','delete'].includes(m)).map(method=>`${method.toUpperCase()} ${path}`));
const missing=expected.filter(x=>!evidence.has(x));
const extra=[...evidence.keys()].filter(x=>!expected.includes(x));
if(missing.length||extra.length){console.error({missing,extra});process.exit(1);}
const report={version:openapi.info.version,count:expected.length,verifiedAt:'31 октября 2026',assessment:'static UI reachability and source markers; not HTTP E2E',operations:expected.sort().map(id=>({id,...evidence.get(id)}))};
writeFileSync(resolve(dir,'docs/API_UI_COVERAGE.json'),JSON.stringify(report,null,2)+'\n');
console.log(`PASS: ${expected.length}/${expected.length} маршрутов/методов сопоставлены с UI и исходными маркерами; ${Object.keys(operations).length} интерактивных форм доступны через навигацию.`);
