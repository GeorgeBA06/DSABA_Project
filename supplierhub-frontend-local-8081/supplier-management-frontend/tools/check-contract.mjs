import {readFileSync} from 'node:fs';
const spec = JSON.parse(readFileSync(new URL('../docs/backend-openapi-stage4.json',import.meta.url),'utf8'));
const expected={
 '/api/v1/auth/login':'post','/api/v1/auth/refresh':'post','/api/v1/auth/me':'get','/api/v1/auth/logout':'post',
 '/api/v1/dashboard':'get','/api/v1/admin/dashboard':'get','/api/v1/reports/suppliers':'get','/api/v1/reports/suppliers.xlsx':'get',
 '/api/v1/suppliers':'get,post','/api/v1/suppliers/{id}':'get,put','/api/v1/suppliers/{id}/status-requests':'post',
 '/api/v1/suppliers/status-requests':'get','/api/v1/suppliers/status-requests/{id}/decision':'post',
 '/api/v1/catalog/products':'get,post','/api/v1/catalog/categories':'get,post','/api/v1/catalog/units':'get,post',
 '/api/v1/contracts':'get,post','/api/v1/contracts/{id}':'get','/api/v1/contracts/{id}/versions':'get',
 '/api/v1/contracts/versions/{id}/submit':'post','/api/v1/contracts/versions/{id}/decision':'post',
 '/api/v1/orders':'get,post','/api/v1/orders/{id}':'get','/api/v1/orders/{id}/versions':'get',
 '/api/v1/orders/versions/{id}/lines':'get','/api/v1/orders/versions/{id}/submit':'post','/api/v1/orders/versions/{id}/decision':'post',
 '/api/v1/fulfillment/orders/{orderId}/deliveries':'post','/api/v1/fulfillment/deliveries/{id}/confirm':'post',
 '/api/v1/fulfillment/deliveries/{id}/acceptances':'post','/api/v1/fulfillment/acceptances/{id}/confirm':'post',
 '/api/v1/relationships/interactions':'get,post','/api/v1/relationships/claims':'get',
 '/api/v1/ratings':'get','/api/v1/ratings/{id}':'get','/api/v1/ratings/{id}/inputs':'get',
 '/api/v1/admin/users':'get,post','/api/v1/admin/users/{id}':'get','/api/v1/admin/users/{id}/roles':'get',
 '/api/v1/admin/users/{id}/block':'post','/api/v1/admin/users/{id}/unblock':'post'
};
const missing=[];
for(const [path,methods] of Object.entries(expected))for(const method of methods.split(','))if(!spec.paths[path]?.[method])missing.push(`${method.toUpperCase()} ${path}`);
if(missing.length){console.error('Отсутствуют операции:',missing.join('\n'));process.exit(1);}
console.log(`Контракт: ${Object.keys(expected).length} маршрутов, ${Object.values(expected).reduce((n,s)=>n+s.split(',').length,0)} проверенных операций — OK`);
