import { useEffect, useMemo, useState } from 'react';
import { api } from '../core/api';
import { useAuth } from '../core/auth';
import { findResource } from '../core/resources';
import { formatDate, formatDecimal, label } from '../core/navigation';
import { Badge, ConfirmForm, DataTable, Empty, ErrorBanner, Field, Icon, Loading, PageHeading, PrimaryButton, SecondaryButton, SelectInput, TextInput } from '../ui/Shared';
import type { JsonRecord } from '../core/types';
import {allRows, supplierTransitions, canDecide} from '../core/data';

const readable: Record<string,string> = {
 id:'Идентификатор', name:'Наименование',contactName:'Контактное лицо',email:'Электронная почта',phone:'Телефон',address:'Адрес',registrationNumber:'Регистрационный номер',status:'Статус',state:'Состояние',sku:'Артикул',active:'Активен',supplierId:'Поставщик',supplierItemCode:'Артикул поставщика',categoryId:'Категория',unitId:'Единица измерения',number:'Номер',contractId:'Договор',orderId:'Заказ',externalNumber:'Накладная',arrivedOn:'Дата поступления',inspectedOn:'Дата приёмки',returnedOn:'Дата возврата',createdAt:'Создано',updatedAt:'Обновлено',createdById:'Создал',currentRegisteredVersionId:'Зарегистрированная версия',currentApprovedVersionId:'Согласованная версия',periodStart:'Начало периода',periodEnd:'Конец периода',totalScore:'Общий балл',timelinessScore:'Соблюдение сроков',completenessScore:'Полнота',qualityScore:'Качество',interactionScore:'Взаимодействие',band:'Категория надёжности',score:'Балл',role:'Роль',displayName:'Имя пользователя',login:'Логин',description:'Описание',requirements:'Требования',resolutionDescription:'Решение',responseDueOn:'Срок ответа',violationType:'Тип нарушения',sendChannel:'Канал отправки',occurredAt:'Дата взаимодействия',subject:'Тема',content:'Содержание',outcome:'Результат',methodVersion:'Версия методики',scope:'Охват',versionNo:'Редакция',validFrom:'Начало действия',validTo:'Окончание действия',terms:'Условия',reason:'Основание',quantity:'Количество',unitPrice:'Цена, BYN',executionDueOn:'Срок исполнения',ratingDueOn:'Срок для рейтинга'
};
// Labels cover the public DTO fields we expose. Unknown technical properties stay hidden.
Object.assign(readable,{
 registeredAt:'Зарегистрировано',registeredById:'Зарегистрировал',approvedById:'Согласовал',approvedAt:'Дата согласования',
 createdById:'Создал',decidedById:'Решение принял',decidedAt:'Дата решения',decisionComment:'Комментарий к решению',
 orderLineId:'Строка заказа',orderVersionId:'Версия заказа',orderVersionLineId:'Строка версии',supplierReturnId:'Возврат',
 acceptanceLineId:'Строка приёмки',deliveryLineId:'Строка поступления',acceptanceDocumentId:'Акт приёмки',
 baseVersionId:'Базовая редакция',contractVersionId:'Редакция договора',sourceVersionLineId:'Исходная строка',
 lineNo:'Номер строки',segmentKey:'Часть обязательства',kind:'Тип редакции',initiator:'Инициатор',changeReason:'Основание новой редакции',
 productId:'Товар',productName:'Наименование товара',productNameSnapshot:'Товар на момент заказа',
 categoryCodeSnapshot:'Код категории на момент заказа',categoryNameSnapshot:'Категория на момент заказа',unitCodeSnapshot:'Единица измерения',
 acceptedQty:'Принято',qualityRejectedQty:'Отклонено по качеству',otherRejectedQty:'Прочие отклонения',otherReason:'Причина прочих отклонений',
 defectDescription:'Описание дефекта',returnedOn:'Дата возврата',supersedesId:'Исправляет запись',correctionId:'Основание исправления',
 registeredVersionId:'Зарегистрированная редакция',applicableFrom:'Применяется с',applicableTo:'Применяется по',
 orderedQty:'Заказано',deliveredOnTimeQty:'Поступило вовремя',fulfilledQty:'Исполнено',qualityCheckedQty:'Проверено',qualityGoodQty:'Соответствует требованиям',
 sourceCutoffAt:'Срез исходных данных',selectionReason:'Причина включения в рейтинг',orderCount:'Заказов в оценке',lineCount:'Строк в оценке',
 asOfAt:'На момент расчёта',scope:'Охват рейтинга',claimId:'Претензия',employeeId:'Сотрудник',
 remedyType:'Способ урегулирования',result:'Результат рассмотрения',resolvedAt:'Завершено',sentAt:'Отправлено',
 reason:'Основание',fromStatus:'Предыдущий статус',toStatus:'Запрошенный статус',requestedById:'Заявитель',requestedAt:'Подана',
 category:'Категория',action:'Действие',entityType:'Тип объекта',entityId:'Объект',correlationId:'Идентификатор запроса',
 methodVersion:'Версия алгоритма',ratingDueOn:'Срок для рейтинга',executionDueOn:'Срок исполнения',
 originalDueOn:'Первоначальный срок',periodStart:'Начало периода',periodEnd:'Окончание периода'
});
const statuses=new Set(['status','state','band','scope','violationType','sendChannel','channel','result']);
const hidden=new Set(['passwordHash','refreshTokenHash','secret','authSubject']);
function prettyValue(key:string,value:unknown) {
 if (value === undefined || value === null || value === '') return '—';
 if (statuses.has(key)) return <Badge value={value}/>;
 if (key==='totalScore'||key.endsWith('Score')||key==='unitPrice'||key==='quantity') return formatDecimal(value,key==='unitPrice'?2:1);
 if (key.endsWith('At')||key.endsWith('On')||key==='periodStart'||key==='periodEnd'||key==='validFrom'||key==='validTo') return formatDate(value);
 if (key==='active') return label(value);
 if(typeof value==='object') return '—';
 return label(value);
}
interface Action { title:string;path:string;method?:'POST'|'PATCH'|'DELETE';body?:unknown;hint?:string;requireComment?:boolean;commentLabel?:string;bodyFromComment?:(comment:string)=>unknown; }
export function Details({resourceId,id,go}:{resourceId:string;id:string;go:(path:string)=>void}) {
 const resource=findResource(resourceId);const {can,current}=useAuth();
 const [record,setRecord]=useState<JsonRecord|null>(null);const [related,setRelated]=useState<JsonRecord[]>([]);const [childType,setChildType]=useState('');const [extra,setExtra]=useState<JsonRecord[]>([]);
 const [busy,setBusy]=useState(true);const [error,setError]=useState<unknown>(null);const [version,setVersion]=useState(0);const [action,setAction]=useState<Action|null>(null);
 const [status,setStatus]=useState('');const [reason,setReason]=useState('');const [requestBusy,setRequestBusy]=useState(false);const [requestError,setRequestError]=useState<unknown>(null);const [offerBusy,setOfferBusy]=useState(false);const [offerError,setOfferError]=useState<unknown>(null);
 const [offers,setOffers]=useState<JsonRecord[]>([]);const [catalog,setCatalog]=useState<JsonRecord[]>([]);const [offerProduct,setOfferProduct]=useState('');const [offerCode,setOfferCode]=useState('');
 useEffect(()=>{ if(!resource||!resource.detail)return;
   let alive=true;setBusy(true);setError(null);setRecord(null);setRelated([]);setExtra([]);setOffers([]);setCatalog([]);
   (async()=>{
    try {
      const main=await api.get<JsonRecord>(`${resource.endpoint}/${id}`);
      let child:JsonRecord[]=[];let supplemental:JsonRecord[]=[];let cType='';
      if(resourceId==='suppliers'){
        const [p,r,products]=await Promise.all([api.get<JsonRecord[]>(`/suppliers/${id}/products`),api.get<JsonRecord[]>(`/ratings/supplier/${id}`),allRows('/catalog/products')]);
        if(alive){setOffers(p);setCatalog(products);}supplemental=r;cType='Оценки надёжности';
      }
      else if(resourceId==='contracts') {child=await api.get<JsonRecord[]>(`/contracts/${id}/versions`);cType='Редакции договора';}
      else if(resourceId==='orders') {child=await api.get<JsonRecord[]>(`/orders/${id}/versions`);cType='Редакции заказа';}
      else if(resourceId==='deliveries') {child=await api.get<JsonRecord[]>(`/fulfillment/deliveries/${id}/lines`);cType='Строки поставки';}
      else if(resourceId==='acceptances') {child=await api.get<JsonRecord[]>(`/fulfillment/acceptances/${id}/lines`);cType='Результаты приёмки';}
      else if(resourceId==='returns') {child=await api.get<JsonRecord[]>(`/fulfillment/returns/${id}/lines`);cType='Строки возврата';}
      else if(resourceId==='ratings') {child=await api.get<JsonRecord[]>(`/ratings/${id}/inputs`);cType='Исходные показатели';}
      else if(resourceId==='users') {const roles=await api.get<string[]>(`/admin/users/${id}/roles`);child=roles.map(role=>({id:role,role}));cType='Назначенные роли';}
      if(alive){setStatus(supplierTransitions(main.status)[0]||'');setRecord(main);setRelated(child);setExtra(supplemental);setChildType(cType);}
    } catch(err){if(alive)setError(err);}finally{if(alive)setBusy(false);}
   })();return()=>{alive=false;};
 },[resource?.endpoint,resourceId,id,version]);
 const isManager=can('PROCUREMENT_MANAGER');
 const actions:Action[]=useMemo(()=>{
  if (!record) return [];
  const result:Action[]=[];
  if(resourceId==='deliveries'&&record.state==='DRAFT')result.push({title:'Подтвердить поступление',path:`/fulfillment/deliveries/${id}/confirm`});
  if(resourceId==='acceptances'&&record.state==='DRAFT')result.push({title:'Подтвердить приёмку',path:`/fulfillment/acceptances/${id}/confirm`});
  if(resourceId==='orders'&&record.state==='COMPLETED'&&isManager)result.push({title:'Возобновить заказ',path:`/orders/${id}/reopen`,requireComment:true,bodyFromComment:comment=>({reason:comment})});
  if(resourceId==='orders'&&['APPROVED','IN_PROGRESS'].includes(String(record.state))&&canDecide(record.createdById,current?.user.id,isManager))result.push({title:'Закрыть заказ',path:`/orders/${id}/close`,requireComment:true,bodyFromComment:comment=>({reason:comment})});
  if(resourceId==='contracts'&&record.state==='REGISTERED'&&canDecide(record.createdById,current?.user.id,isManager))result.push({title:'Прекратить договор',path:`/contracts/${id}/terminate`,requireComment:true,bodyFromComment:comment=>({reason:comment})});
  if(resourceId==='products'&&isManager)result.push({title:record.active?'Деактивировать товар':'Активировать товар',path:`/catalog/products/${id}/active`,method:'PATCH',body:{active:!record.active}});
  if(resourceId==='claims'&&record.state==='SENT')result.push({title:'Начать урегулирование',path:`/relationships/claims/${id}/begin-resolution`});
  if(resourceId==='returns'&&record.state==='DRAFT')result.push({title:'Отправить возврат на согласование',path:`/fulfillment/returns/${id}/submit`});
  if(resourceId==='corrections'&&record.state==='DRAFT')result.push({title:'Отправить исправление на согласование',path:`/fulfillment/corrections/${id}/submit`});
  if(resourceId==='assessments'&&record.state==='DRAFT')result.push({title:'Отправить оценку на согласование',path:`/ratings/expert-assessments/${id}/submit`});
  if(['returns','corrections','assessments'].includes(resourceId)&&record.state==='PENDING'&&canDecide(record.createdById,current?.user.id,isManager)){
   const prefix=resourceId==='returns'?'/fulfillment/returns':resourceId==='corrections'?'/fulfillment/corrections':'/ratings/expert-assessments';
   result.push({title:'Согласовать',path:`${prefix}/${id}/decision`,body:{approve:true,comment:''}});
   result.push({title:'Отклонить',path:`${prefix}/${id}/decision`,requireComment:true,bodyFromComment:comment=>({approve:false,comment})});
  }
  if(resourceId==='users')result.push({title:record.status==='ACTIVE'?'Заблокировать':'Разблокировать',path:`/admin/users/${id}/${record.status==='ACTIVE'?'block':'unblock'}`,requireComment:true,bodyFromComment:comment=>({reason:comment})});
  return result;
 },[record,resourceId,isManager,id,current?.user.id]);
 const doAction=async (selected:Action,comment:string)=>{await api.request(selected.method||'POST',selected.path,selected.bodyFromComment?selected.bodyFromComment(comment):selected.body);setVersion(v=>v+1);};
 if(!resource||!resource.detail||!can(...resource.access))return <ErrorBanner error="Карточка недоступна"/>;
 const openFields=record?Object.entries(record).filter(([key,value])=>Boolean(readable[key])&&!hidden.has(key)&&typeof value!=='object'&&value!==null):[];
 return <><button className="text-back" onClick={()=>go(`/${resourceId}`)}>← Вернуться в реестр</button><PageHeading title={String(record?.name||record?.number||record?.displayName||resource.title)} subtitle={resource.title} action={<div className="row wrap">{resource.canEdit&&record?.status!=='TERMINATED'&&<SecondaryButton onClick={()=>go(`/${resourceId}/${id}/edit`)}><Icon name="edit" size={16}/> Изменить</SecondaryButton>}{resourceId==='orders'&&['APPROVED','IN_PROGRESS'].includes(String(record?.state))&&<PrimaryButton onClick={()=>go(`/orders/${id}/receive`)}>Зарегистрировать поступление</PrimaryButton>}{resourceId==='deliveries'&&record?.state==='CONFIRMED'&&<PrimaryButton onClick={()=>go(`/deliveries/${id}/inspect`)}>Создать приёмку</PrimaryButton>}<SecondaryButton onClick={()=>setVersion(v=>v+1)}><Icon name="refresh" size={16}/> Обновить</SecondaryButton></div>}/>
 {error&&<ErrorBanner error={error} onRetry={()=>setVersion(v=>v+1)}/>}{busy?<Loading/>:record&&<>
 <div className="detail-grid"><section className="card detail-main"><h2>Общие сведения</h2><div className="properties">{openFields.map(([key,value])=><div className="property" key={key}><span>{readable[key]||key}</span><strong>{prettyValue(key,value)}</strong></div>)}</div></section>
 <aside className="detail-aside">
 {(actions.length>0||['suppliers','orders','contracts','deliveries','acceptances','claims','users'].includes(resourceId))&&<section className="card"><h2>Действия</h2><div className="action-stack">{actions.map(item=><SecondaryButton key={item.path} onClick={()=>setAction(item)}>{item.title}</SecondaryButton>)}
 {resourceId==='contracts'&&record.state!=='TERMINATED'&&related.every(v=>v.state!=='DRAFT'&&v.state!=='PENDING')&&<SecondaryButton onClick={()=>go(`/operation/contract-revision/${id}`)}>Создать редакцию договора</SecondaryButton>}
 {resourceId==='orders'&&record.state==='DRAFT'&&related.some(v=>v.kind==='INITIAL'&&v.state==='DRAFT')&&<SecondaryButton onClick={()=>go(`/operation/order-draft/${id}`)}>Редактировать строки черновика</SecondaryButton>}
 {resourceId==='orders'&&record.state==='DRAFT'&&related.length>0&&[...related].sort((a,b)=>Number(b.versionNo)-Number(a.versionNo))[0]?.state==='REJECTED'&&<SecondaryButton onClick={()=>go(`/operation/order-revise/${id}`)}>Переработать отклонённую версию</SecondaryButton>}
 {resourceId==='orders'&&['APPROVED','IN_PROGRESS'].includes(String(record.state))&&related.every(v=>v.state!=='PENDING'&&v.state!=='DRAFT')&&<><SecondaryButton onClick={()=>go(`/operation/order-increase/${id}`)}>Увеличить заказ</SecondaryButton><SecondaryButton onClick={()=>go(`/operation/order-reschedule/${id}`)}>Перенести срок</SecondaryButton></>}
 {resourceId==='orders'&&record.currentApprovedVersionId&&<SecondaryButton onClick={()=>go(`/operation/return-create/${id}`)}>Оформить возврат</SecondaryButton>}
 {resourceId==='deliveries'&&record.state==='CONFIRMED'&&<SecondaryButton onClick={()=>go(`/operation/delivery-correction/${id}`)}>Исправить поступление</SecondaryButton>}
 {resourceId==='acceptances'&&record.state==='CONFIRMED'&&<SecondaryButton onClick={()=>go(`/operation/acceptance-correction/${id}`)}>Исправить приёмку</SecondaryButton>}
 {resourceId==='claims'&&record.state==='DRAFT'&&<SecondaryButton onClick={()=>go(`/operation/claim-send/${id}`)}>Отправить поставщику</SecondaryButton>}
 {resourceId==='claims'&&['SENT','IN_RESOLUTION'].includes(String(record.state))&&<SecondaryButton onClick={()=>go(`/operation/claim-resolve/${id}`)}>Урегулировать претензию</SecondaryButton>}
 {resourceId==='users'&&<><SecondaryButton onClick={()=>go(`/operation/user-reset/${id}`)}>Сбросить пароль</SecondaryButton><SecondaryButton onClick={()=>go(`/operation/user-add-role/${id}`)}>Добавить роль</SecondaryButton></>}
 {resourceId==='suppliers'&&<><div className="divider"/><h3>Изменение статуса</h3><Field label="Новый статус"><SelectInput name="status" value={status} onChange={setStatus} items={supplierTransitions(record.status).map(s=>({value:s,label:label(s)}))}/></Field><Field label="Основание"><TextInput name="reason" value={reason} onChange={setReason}/></Field>{requestError&&<ErrorBanner error={requestError}/>}<PrimaryButton busy={requestBusy} disabled={!reason.trim()||!supplierTransitions(record.status).includes(status)} onClick={async()=>{setRequestBusy(true);setRequestError(null);try{await api.post(`/suppliers/${id}/status-requests`,{status,reason});setReason('');setVersion(v=>v+1);}catch(err){setRequestError(err);}finally{setRequestBusy(false);}}}>Отправить заявку</PrimaryButton></>}
 </div></section>}
 {resourceId==='ratings'&&<section className="card"><h2>Методика</h2><p className="muted">Сроки — 35%, полнота — 25%, качество — 30%, взаимодействие — 10%.</p><p className="muted">Результат определяет сервер.</p></section>}
 </aside></div>
 {resourceId==='suppliers'&&<section className="card section-space"><h2>Предлагаемые товары</h2>{offers.length?<DataTable columns={[{key:'productName',title:'Наименование'},{key:'supplierItemCode',title:'Артикул поставщика'},{key:'active',title:'Доступен'}]} rows={offers}/>:<Empty description="Товары пока не добавлены"/>}<div className="offer-form"><h3>Добавить предлагаемый товар</h3><div className="form-grid"><Field label="Товар из каталога"><SelectInput name="offerProduct" value={offerProduct} onChange={setOfferProduct} items={catalog.filter(product=>product.active&&!offers.some(offer=>offer.productId===product.id)).map(product=>({value:String(product.id),label:String(product.name)}))}/></Field><Field label="Артикул поставщика (необязательно)"><TextInput name="offerCode" value={offerCode} onChange={setOfferCode}/></Field></div>{offerError&&<ErrorBanner error={offerError}/>}<PrimaryButton disabled={!offerProduct} busy={offerBusy} onClick={async()=>{setOfferBusy(true);setOfferError(null);try{await api.post(`/suppliers/${id}/products`,{productId:offerProduct,supplierItemCode:offerCode});setOfferProduct('');setOfferCode('');setVersion(v=>v+1);}catch(err){setOfferError(err);}finally{setOfferBusy(false);}}}>Добавить товар</PrimaryButton></div>{extra.length>0&&<><h2>История рейтингов</h2><DataTable rows={extra} columns={[{key:'periodEnd',title:'Период',format:'date'},{key:'totalScore',title:'Балл',format:'decimal'},{key:'state',title:'Статус',format:'status'}]}/></>}</section>}
 {related.length>0&&<section className="card section-space"><h2>{childType}</h2><div className="related-list">{related.map((item,index)=><div className="related-card" key={String(item.id||index)}><div className="related-header"><strong>{item.number?`№ ${item.number}`:item.versionNo?`Версия ${item.versionNo}`:item.productNameSnapshot||item.role||`Запись ${index+1}`}</strong>{item.state&&<Badge value={item.state}/>}</div><div className="related-fields">{Object.entries(item).filter(([k,v])=>Boolean(readable[k])&&!['id','orderId','contractId','createdById','updatedAt'].includes(k)&&v!==null&&typeof v!=='object').slice(0,9).map(([k,v])=><span key={k}><span className="muted">{readable[k]||k}: </span>{prettyValue(k,v)}</span>)}</div>
 {((resourceId==='orders'||resourceId==='contracts')&&['DRAFT','PENDING'].includes(String(item.state)))&&<div className="row wrap related-actions">{item.state==='DRAFT'&&item.createdById===current?.user.id&&<SecondaryButton onClick={()=>setAction({title:'Отправить на согласование',path:resourceId==='orders'?`/orders/versions/${item.id}/submit`:`/contracts/versions/${item.id}/submit`})}>Отправить на согласование</SecondaryButton>}{item.state==='PENDING'&&isManager&&item.createdById!==current?.user.id&&<><SecondaryButton onClick={()=>setAction({title:'Согласовать версию',path:resourceId==='orders'?`/orders/versions/${item.id}/decision`:`/contracts/versions/${item.id}/decision`,bodyFromComment:comment=>({approve:true,comment})})}>Согласовать</SecondaryButton><SecondaryButton onClick={()=>setAction({title:'Отклонить версию',path:resourceId==='orders'?`/orders/versions/${item.id}/decision`:`/contracts/versions/${item.id}/decision`,requireComment:true,bodyFromComment:comment=>({approve:false,comment})})}>Отклонить</SecondaryButton></>}</div>}
 {resourceId==='users'&&item.role&&can('ADMINISTRATOR')&&<SecondaryButton onClick={()=>setAction({title:`Отозвать роль ${label(item.role)}`,path:`/admin/users/${id}/roles/${item.role}`,method:'DELETE'})}>Отозвать роль</SecondaryButton>}
 </div>)}</div></section>}
 </>}
 {action&&<ConfirmForm title={action.title} description="Операция будет зарегистрирована в системе. Проверьте её корректность." requireComment={action.requireComment} onClose={()=>setAction(null)} onSubmit={comment=>doAction(action,comment)}/>}
 </>;
}
