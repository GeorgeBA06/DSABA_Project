import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../core/api';
import type { JsonRecord } from '../core/types';
import {allRows,todayMinsk} from '../core/data';
import { ErrorBanner, Field, Loading, PageHeading, PrimaryButton, SecondaryButton, TextInput } from '../ui/Shared';

interface QuantityRow { id:string; title:string; max?:number;quantity:string;accepted:string;qualityRejected:string;otherRejected:string;reason:string; }
interface OrderVersion { id:string;versionNo:number;state:string; }
interface Segment {id:string;productNameSnapshot?:string;quantity:number;executionDueOn:string;}
interface DeliveryLine {id:string;quantity:number;}
export function ReceivingWorkflow({type,id,go}:{type:'delivery'|'inspection';id:string;go:(path:string)=>void}) {
 const [lines,setLines]=useState<QuantityRow[]>([]);const [arrivedOn,setArrivedOn]=useState(todayMinsk());const [externalNumber,setExternalNumber]=useState('');
 const [loading,setLoading]=useState(true);const [busy,setBusy]=useState(false);const [error,setError]=useState<unknown>(null);
 const isDelivery=type==='delivery';const returnTo=isDelivery?`/orders/${id}`:`/deliveries/${id}`;
 useEffect(()=>{let alive=true;setLoading(true);setError(null);
 (async()=>{try{
   if(isDelivery){
     const versions=await api.get<OrderVersion[]>(`/orders/${id}/versions`);
     const approved=versions.filter(x=>x.state==='APPROVED').sort((a,b)=>b.versionNo-a.versionNo)[0];
     if(!approved)throw new Error('Для заказа не найдена согласованная версия.');
     const segments=await api.get<Segment[]>(`/orders/versions/${approved.id}/lines`);
     if(!segments.length)throw new Error('У согласованной версии нет строк.');
     if(alive)setLines(segments.map((s,i)=>({id:s.id,title:`${s.productNameSnapshot||`Строка ${i+1}`} · план ${s.quantity} · срок ${s.executionDueOn}`,quantity:'0',accepted:'0',qualityRejected:'0',otherRejected:'0',reason:''})));
   }else{
     const details=await api.get<JsonRecord>(`/fulfillment/deliveries/${id}`);
     if(details.state!=='CONFIRMED')throw new Error('Для приёмки необходимо сначала подтвердить поступление.');
     const deliveryLines=await api.get<DeliveryLine[]>(`/fulfillment/deliveries/${id}/lines`);
     const docs=(await allRows('/fulfillment/acceptances')).filter(d=>d.deliveryId===id&&d.state==='CONFIRMED');
     const superseded=new Set(docs.map(d=>String(d.supersedesId||'')).filter(Boolean));
     const confirmed=docs.filter(d=>!superseded.has(String(d.id)));
     const confirmedLines=(await Promise.all(confirmed.map(doc=>api.get<JsonRecord[]>(`/fulfillment/acceptances/${doc.id}/lines`)))).flat();
     const inspected=new Map<string,number>();
     for(const part of confirmedLines){const lineId=String(part.deliveryLineId);inspected.set(lineId,(inspected.get(lineId)||0)+Number(part.acceptedQty||0)+Number(part.qualityRejectedQty||0)+Number(part.otherRejectedQty||0));}
     if(!deliveryLines.length)throw new Error('В поступлении отсутствуют строки.');
     if(alive)setLines(deliveryLines.filter(l=>l.quantity-(inspected.get(l.id)||0)>0).map((l,i)=>({id:l.id,title:`Строка поступления ${i+1} · доступно к проверке ${Math.max(0,l.quantity-(inspected.get(l.id)||0))}`,max:Math.max(0,l.quantity-(inspected.get(l.id)||0)),quantity:'0',accepted:'0',qualityRejected:'0',otherRejected:'0',reason:''})));
     if(alive)setArrivedOn(String(details.arrivedOn||todayMinsk()));
   }
  }catch(err){if(alive)setError(err);}finally{if(alive)setLoading(false);}})();return()=>{alive=false;};
 },[isDelivery,id]);
 const change=(index:number,key:keyof QuantityRow,value:string)=>setLines(prev=>prev.map((line,i)=>i===index?{...line,[key]:value}:line));
 const submit=async(e:FormEvent)=>{e.preventDefault();setError(null);setBusy(true);try{
   if(isDelivery){
     const items=lines.filter(line=>Number(line.quantity)>0).map(line=>({segmentId:line.id,quantity:Number(line.quantity)}));
     if(!items.length||lines.some(line=>Number(line.quantity)<0))throw new Error('Укажите положительное количество хотя бы в одной строке.');
     const result=await api.post<{id:string}>(`/fulfillment/orders/${id}/deliveries`,{arrivedOn,externalNumber,items});go(`/deliveries/${result.id}`);
   }else{
     const selected=lines.map(line=>({deliveryLineId:line.id,accepted:Number(line.accepted||0),qualityRejected:Number(line.qualityRejected||0),otherRejected:Number(line.otherRejected||0),reason:line.reason||null}));
     if(selected.some((x,i)=>x.accepted<0||x.qualityRejected<0||x.otherRejected<0||(lines[i].max!==undefined&&x.accepted+x.qualityRejected+x.otherRejected>(lines[i].max||0))||(x.otherRejected>0&&!x.reason)))throw new Error('Проверьте количества и причины отклонений.');
     const items=selected.filter(x=>x.accepted+x.qualityRejected+x.otherRejected>0);
     if(!items.length)throw new Error('Укажите количество для проверки хотя бы в одной строке. Возможно, всё поступление уже проверено.');
     const result=await api.post<{id:string}>(`/fulfillment/deliveries/${id}/acceptances`,{inspectedOn:arrivedOn,items});go(`/acceptances/${result.id}`);
   }
  }catch(err){setError(err);}finally{setBusy(false);}};
 return <><button className="text-back" onClick={()=>go(returnTo)}>← К документу</button><PageHeading title={isDelivery?'Регистрация поступления':'Создание акта приёмки'} subtitle={isDelivery?'Укажите фактически полученное количество по строкам заказа':'Разделите проверенное количество на принятое и отклонённое'}/>{loading?<Loading/>:<section className="card form-card"><form onSubmit={submit} className="form-stack"><div className="form-grid"><Field label={isDelivery?'Дата поступления':'Дата проверки'}><TextInput name="date" type="date" value={arrivedOn} required onChange={setArrivedOn}/></Field>{isDelivery&&<Field label="Номер накладной (необязательно)"><TextInput name="externalNumber" value={externalNumber} onChange={setExternalNumber}/></Field>}</div><h2>Товарные строки</h2>{lines.map((line,i)=><div className="order-line" key={line.id}><strong>{line.title}</strong>{isDelivery?<Field label="Фактически поступило"><TextInput name="quantity" type="number" min={0} step="any" value={line.quantity} onChange={v=>change(i,'quantity',v)}/></Field>:<div className="form-grid"><Field label="Принято"><TextInput name="accepted" type="number" min={0} step="any" value={line.accepted} onChange={v=>change(i,'accepted',v)}/></Field><Field label="Брак"><TextInput name="qualityRejected" type="number" min={0} step="any" value={line.qualityRejected} onChange={v=>change(i,'qualityRejected',v)}/></Field><Field label="Другие отклонения"><TextInput name="otherRejected" type="number" min={0} step="any" value={line.otherRejected} onChange={v=>change(i,'otherRejected',v)}/></Field><Field label="Причина прочих отклонений"><TextInput name="reason" value={line.reason} onChange={v=>change(i,'reason',v)}/></Field></div>}</div>)}{error&&<ErrorBanner error={error}/>}<p className="muted note">Данные сохраняются как черновик. Для учёта исполнения необходимо подтвердить документ в его карточке.</p><div className="form-footer"><SecondaryButton onClick={()=>go(returnTo)}>Отмена</SecondaryButton><PrimaryButton type="submit" busy={busy}>Сохранить черновик</PrimaryButton></div></form></section>}</>;
}
