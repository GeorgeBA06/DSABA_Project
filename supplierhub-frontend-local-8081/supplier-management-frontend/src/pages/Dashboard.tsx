import { useEffect, useState } from 'react';
import { api } from '../core/api';
import { BUSINESS_ROLES, useAuth } from '../core/auth';
import { ErrorBanner, Icon, Loading, PageHeading, SecondaryButton } from '../ui/Shared';

interface BusinessDashboard {suppliers:number;products:number;contracts:number;orders:number;deliveries:number;claims:number;ratings:number;}
interface AdminDashboard {users:number;}
export function Dashboard({go}:{go:(route:string)=>void}) {
 const {can,current}=useAuth(); const isBusiness=can(...BUSINESS_ROLES);const isAdmin=can('ADMINISTRATOR');
 const [data,setData]=useState<BusinessDashboard | null>(null);const [admin,setAdmin]=useState<AdminDashboard | null>(null);
 const [loading,setLoading]=useState(true);const [error,setError]=useState<unknown>(null);const [version,setVersion]=useState(0);
 useEffect(()=>{let alive=true;setLoading(true);setError(null);
  Promise.all([isBusiness?api.get<BusinessDashboard>('/dashboard'):Promise.resolve(null),isAdmin?api.get<AdminDashboard>('/admin/dashboard'):Promise.resolve(null)]).then(([business,admins])=>{if(alive){setData(business);setAdmin(admins);}}).catch(e=>{if(alive)setError(e);}).finally(()=>{if(alive)setLoading(false);});return()=>{alive=false;};},[isAdmin,isBusiness,version]);
 const cards = [{label:'Поставщики',value:data?.suppliers,icon:'building',url:'/suppliers'},{label:'Товары',value:data?.products,icon:'package',url:'/products'},{label:'Договоры',value:data?.contracts,icon:'file',url:'/contracts'},{label:'Заказы',value:data?.orders,icon:'basket',url:'/orders'},{label:'Поступления',value:data?.deliveries,icon:'truck',url:'/deliveries'},{label:'Претензии',value:data?.claims,icon:'alert',url:'/claims'},{label:'Рейтинги',value:data?.ratings,icon:'chart',url:'/ratings'}];
 return <><PageHeading title="Главная" subtitle={`Здравствуйте, ${current?.user.displayName || 'сотрудник'}. Краткий обзор доступных данных.`} action={<SecondaryButton onClick={()=>setVersion(v=>v+1)}><Icon name="refresh" size={16}/> Обновить</SecondaryButton>}/>
 <ErrorBanner error={error} onRetry={()=>setVersion(v=>v+1)}/>
 {loading?<Loading/>:<>
 {isBusiness&&<><div className="section-title"><h2>Рабочие показатели</h2><span className="muted">По данным сервера</span></div><div className="kpi-grid">{cards.map(c=><button key={c.url} className="kpi-card" onClick={()=>go(c.url)}><div className="kpi-symbol"><Icon name={c.icon}/></div><strong>{c.value??'—'}</strong><span>{c.label}</span></button>)}</div><section className="card quick-actions"><h2>Быстрые действия</h2><div className="row wrap"><SecondaryButton onClick={()=>go('/suppliers/new')}>Новый поставщик</SecondaryButton><SecondaryButton onClick={()=>go('/orders/new')}>Создать заказ</SecondaryButton><SecondaryButton onClick={()=>go('/reports')}>Отчёт поставщиков</SecondaryButton></div></section></>}
 {isAdmin&&<section className="card admin-summary"><div className="section-title"><h2>Администрирование</h2></div><div className="kpi-inline"><Icon name="users" size={26}/><div><strong>{admin?.users??'—'}</strong><span className="muted">Учётных записей</span></div></div><SecondaryButton onClick={()=>go('/users')}>Управление пользователями</SecondaryButton></section>}
 </>}
 </>;
}
