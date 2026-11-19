import type {JsonRecord} from './types';
import {requireNumber,toOrderItems} from './data.ts';
const required=(v:unknown,n:string)=>{const t=String(v??'').trim();if(!t)throw new Error(`Заполните поле «${n}».`);return t;};
const positive=(v:unknown,n:string)=>requireNumber(v,n);
export interface OperationRequest {method:'POST'|'PUT'; endpoint:string;payload:unknown;}
/** Pure request builder. Validated independently against Stage 4 OpenAPI. */
export function buildOperationRequest(kind:string,id:string|undefined,value:JsonRecord,lines:JsonRecord[]):OperationRequest{
  const v=value;let endpoint='';let payload:unknown;let method:'POST'|'PUT'='POST';
  switch(kind){
   case 'password-change':endpoint='/auth/password';payload={currentPassword:required(v.currentPassword,'Текущий пароль'),newPassword:required(v.newPassword,'Новый пароль')};break;
   case 'user-reset':endpoint=`/admin/users/${id}/reset-password`;payload={newPassword:required(v.newPassword,'Новый пароль')};break;
   case 'user-add-role':endpoint=`/admin/users/${id}/roles`;payload={role:required(v.role,'Роль')};break;
   case 'contract-revision':endpoint=`/contracts/${id}/versions`;payload={terms:{number:required(v.number,'Номер договора'),signedOn:v.signedOn||null,validFrom:required(v.validFrom,'Дата начала'),validTo:required(v.validTo,'Дата окончания'),terms:required(v.terms,'Условия')},reason:required(v.reason,'Основание')};break;
   case 'order-draft':endpoint=`/orders/${id}/draft`;method='PUT';payload={items:toOrderItems(lines)};break;
   case 'order-revise':endpoint=`/orders/${id}/revise-rejected`;payload={items:toOrderItems(lines),reason:required(v.reason,'Основание')};break;
   case 'order-increase':endpoint=`/orders/${id}/increase`;payload={orderLineId:required(v.orderLineId,'Строка заказа'),additional:positive(v.additional,'Дополнительное количество'),unitPrice:requireNumber(v.unitPrice,'Цена',true),dueOn:required(v.dueOn,'Срок'),reason:required(v.reason,'Основание')};break;
   case 'order-reschedule':endpoint=`/orders/${id}/reschedule`;payload={segmentKey:required(v.segmentKey,'Часть обязательства'),newDueOn:required(v.newDueOn,'Новый срок'),reason:required(v.reason,'Основание')};break;
   case 'return-create':endpoint=`/fulfillment/orders/${id}/returns`;payload={returnedOn:required(v.returnedOn,'Дата возврата'),reason:required(v.reason,'Причина'),items:lines.filter(x=>Number(x.quantity)>0).map(x=>({acceptanceLineId:x.acceptanceLineId,quantity:positive(x.quantity,'Возвращаемое количество'),defectDescription:required(x.defectDescription,'Описание дефекта')}))};break;
   case 'delivery-correction':endpoint=`/fulfillment/deliveries/${id}/correction`;payload={arrivedOn:required(v.arrivedOn,'Дата поступления'),reason:required(v.reason,'Причина'),items:lines.map(x=>({approvedSegmentId:x.approvedSegmentId,quantity:positive(x.quantity,'Количество')}))};break;
   case 'acceptance-correction':endpoint=`/fulfillment/acceptances/${id}/correction`;payload={inspectedOn:required(v.inspectedOn,'Дата проверки'),reason:required(v.reason,'Причина'),items:lines.map(x=>({deliveryLineId:x.deliveryLineId,accepted:requireNumber(x.accepted,'Принято',true),qualityRejected:requireNumber(x.qualityRejected,'Брак',true),otherRejected:requireNumber(x.otherRejected,'Прочее',true),reason:x.reason||null}))};break;
   case 'claim-send':endpoint=`/relationships/claims/${id}/send`;payload={channel:required(v.channel,'Канал')};break;
   case 'claim-resolve':endpoint=`/relationships/claims/${id}/resolve`;payload={result:required(v.result,'Результат'),remedy:required(v.remedy,'Способ'),description:required(v.description,'Описание')};break;
   case 'claim-create':endpoint='/relationships/claims';payload={number:required(v.number,'Номер'),supplierId:required(v.supplierId,'Поставщик'),orderLineId:required(v.orderLineId,'Строка заказа'),deliveryLineId:v.deliveryLineId||null,type:required(v.type,'Нарушение'),description:required(v.description,'Описание'),requirements:required(v.requirements,'Требования'),responseDueOn:v.responseDueOn||null};break;
   case 'assessment-create':endpoint='/ratings/expert-assessments';payload={supplierId:required(v.supplierId,'Поставщик'),periodStart:required(v.periodStart,'Период с'),periodEnd:required(v.periodEnd,'Период по'),applicableFrom:required(v.applicableFrom,'Применять с'),applicableTo:required(v.applicableTo,'Применять по'),score:Number(required(v.score,'Балл')),justification:required(v.justification,'Обоснование')};break;
   case 'rating-calculate':endpoint='/ratings/calculate';payload={supplierId:required(v.supplierId,'Поставщик'),scope:v.scope,categoryId:v.scope==='CATEGORY'?required(v.categoryId,'Категория'):null};break;
   default:throw new Error('Неизвестная операция');
  }
  if(kind==='return-create'&&!(payload as {items:unknown[]}).items.length)throw new Error('Выберите строки и количество возврата.');
  return {method,endpoint,payload};
}
