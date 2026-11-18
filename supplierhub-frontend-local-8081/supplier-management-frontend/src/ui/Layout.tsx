import { useState } from 'react';
import type { ReactNode, MouseEvent } from 'react';
import { resources } from '../core/resources';
const nestedResourceIds = new Set(['categories','units','acceptances','returns','corrections','assessments','status-requests']);
import { label } from '../core/navigation';
import { useAuth } from '../core/auth';
import { Icon, SecondaryButton } from './Shared';

export function AppLayout({children,path,go}:{children:ReactNode;path:string;go:(path:string)=>void}) {
  const {current,logout,can}=useAuth();
  const [mobileMenu,setMobileMenu]=useState(false);
  const link = (href:string) => (event:MouseEvent<HTMLAnchorElement>) => {
    if(event.ctrlKey||event.metaKey||event.shiftKey||event.altKey||event.button!==0)return;
    event.preventDefault();go(href);setMobileMenu(false);
  };
  const sections=[
    {name:'Обзор',entries:[{id:'dashboard',short:'Главная',icon:'grid',url:'/dashboard'}]},
    ...['Основное','Закупки','Исполнение','Отношения','Аналитика','Решения','Справочники','Администрирование'].map(name=>({name,entries:resources.filter(item=>item.group===name && !nestedResourceIds.has(item.id) && can(...item.access)).map(item=>({id:item.id,short:item.short,icon:item.icon,url:`/${item.id}`}))})),
    ...(can('PROCUREMENT_MANAGER','PROCUREMENT_SPECIALIST')?[{name:'Отчётность',entries:[{id:'reports',short:'Отчёты',icon:'chart',url:'/reports'}]}]:[])
  ].filter(section=>section.entries.length);
  return <div className="app-shell">
    {mobileMenu&&<div className="mobile-backdrop" onClick={()=>setMobileMenu(false)}/>}
    <aside className={`sidebar ${mobileMenu?'sidebar-open':''}`}>
      <div className="brand"><div className="brand-symbol"><Icon name="package" size={22}/></div><div><strong>SupplierHub</strong><small>Управление поставщиками</small></div></div>
      <nav className="sidebar-nav" aria-label="Основное меню">
        {sections.map(section=><div className="nav-group" key={section.name}><div className="group-label">{section.name}</div>{section.entries.map(entry=><a key={entry.id} href={entry.url} className={`nav-item ${(path===entry.url||path.startsWith(entry.url+'/'))?'nav-active':''}`} onClick={link(entry.url)}><Icon name={entry.icon} size={18}/><span>{entry.short}</span></a>)}</div>)}
      </nav>
      <div className="sidebar-footer"><span className="side-avatar">{current?.user.displayName?.[0]?.toUpperCase() || 'П'}</span><div><strong>{current?.user.displayName}</strong><small>{label(current?.roles[0])}</small></div></div>
    </aside>
    <div className="app-main">
      <header className="topbar"><button className="icon-button mobile-toggle" aria-label="Открыть меню" onClick={()=>setMobileMenu(true)}><Icon name="menu"/></button><div className="topbar-location">Система управления поставщиками</div><div className="topbar-right"><div className="current-account"><span className="user-avatar">{current?.user.displayName?.[0]?.toUpperCase()||'П'}</span><div><strong>{current?.user.displayName}</strong><small>{label(current?.roles[0])}</small></div></div><SecondaryButton onClick={()=>go('/operation/password-change')}>Сменить пароль</SecondaryButton><button className="icon-button exit" onClick={()=>void logout().catch(()=>undefined)} aria-label="Выйти из системы" title="Выйти"><Icon name="logout"/></button></div></header>
      <main className="page-container">{children}</main>
      <footer className="app-footer">Система управления поставщиками · BYN · Без расчёта НДС</footer>
    </div>
  </div>;
}
